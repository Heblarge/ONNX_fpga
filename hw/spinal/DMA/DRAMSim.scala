package DMA

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._

// =============================================================================
// DRAMSim 接口：用于连接 ramulator2 DPI-C 仿真模型
//
// 与 SpAttenFull 版本的区别：
//   1. 支持完整 AXI4（读+写），不仅限于 Axi4ReadOnly
//   2. 包名从 spatten 改为 DMA
// =============================================================================

case class DRAMSimConfig(
  addressWidth: Int,
  dataWidth: Int
)

case class DRAMSimTransaction(config: DRAMSimConfig) extends Bundle {
  val addr     = UInt(config.addressWidth bits)
  val data     = Bits(config.dataWidth bits)
  val is_write = Bool
}

// 用于 outstanding 读追踪：记录每个 AR 突发的总拍数和 ID
case class ArBurstInfo(idWidth: Int) extends Bundle {
  val totalBeats = UInt(9 bits)
  val id         = UInt(idWidth bits)
}

case class DRAMSim(config: DRAMSimConfig) extends Bundle with IMasterSlave {
  val req  = Stream(DRAMSimTransaction(config))
  val resp = Stream(DRAMSimTransaction(config))

  override def asMaster(): Unit = {
    master(req)
    slave(resp)
  }

  /** 连接 AXI4 完整接口（读+写）
    *
    * ramulator2 DPI 按单拍操作，不理解 AXI4 突发（burst）。
    * 此处将 AXI4 burst 拆解为逐拍的 DRAMSim 事务：
    *   - AW+W burst: 每个 W beat 生成一个 DRAMSim 写请求（地址自增）
    *   - AR burst:   生成 len+1 个 DRAMSim 读请求，收集同等数量的响应
    */
  def fromAxi4(bus: Axi4): Unit = {
    val bytesPerBeat = config.dataWidth / 8

    // =================================================================
    // 写通道：AW → captured, then per W-beat → DRAMSim write request
    // =================================================================
    val awAddr    = Reg(UInt(config.addressWidth bits)) init 0
    val awLen     = Reg(UInt(8 bits)) init 0
    val awId      = Reg(UInt(bus.config.idWidth bits)) init 0
    val awPending = Reg(Bool()) init False  // AW 已接受但 W 未完成
    val wBeatCnt  = Reg(UInt(8 bits)) init 0

    // AW 通道握手
    bus.aw.ready := !awPending
    when(bus.aw.fire) {
      awAddr    := bus.aw.addr
      awLen     := bus.aw.len
      awId      := bus.aw.id
      awPending := True
      wBeatCnt  := 0
    }

    // W 通道 → DRAMSim 写请求
    val wReq = Stream(DRAMSimTransaction(config))
    wReq.valid    := awPending && bus.w.valid
    wReq.addr     := awAddr
    wReq.data     := bus.w.data
    wReq.is_write := True
    bus.w.ready   := awPending && wReq.ready

    when(bus.w.fire) {
      awAddr := awAddr + bytesPerBeat
      wBeatCnt := wBeatCnt + 1
      when(bus.w.last || wBeatCnt === awLen) {
        awPending := False
      }
    }

    // B 通道：写完成（W last fired 后立即响应）
    val bPending = Reg(Bool()) init False
    val bId      = Reg(UInt(bus.config.idWidth bits)) init 0
    when(bus.w.fire && (bus.w.last || wBeatCnt === awLen)) {
      bPending := True
      bId      := awId
    }
    bus.b.valid := bPending
    bus.b.id    := bId
    bus.b.setOKAY()
    when(bus.b.fire) {
      bPending := False
    }

    // =================================================================
    // 读通道：AR → 生成 len+1 个 DRAMSim 读请求（支持 outstanding）
    // =================================================================
    // 用 FIFO 记录每个 AR 突发的 (totalBeats, id)，支持多个 outstanding 读事务
    val arInfoFifo = StreamFifo(ArBurstInfo(bus.config.idWidth), 8)

    val arReqLeft = Reg(UInt(9 bits)) init 0
    val arReqAddr = Reg(UInt(config.addressWidth bits)) init 0

    // AR 接受条件：FIFO 有空间 且 上一个 AR 的请求已全部发出
    bus.ar.ready := arInfoFifo.io.push.ready && (arReqLeft === 0)

    arInfoFifo.io.push.valid := bus.ar.fire
    arInfoFifo.io.push.payload.totalBeats := (bus.ar.len +^ 1).resized
    arInfoFifo.io.push.payload.id := bus.ar.id

    when(bus.ar.fire) {
      arReqLeft := (bus.ar.len +^ 1).resized
      arReqAddr := bus.ar.addr
    }

    // AR → DRAMSim 读请求
    val rReq = Stream(DRAMSimTransaction(config))
    rReq.valid    := (arReqLeft =/= 0)
    rReq.addr     := arReqAddr
    rReq.data.assignDontCare()
    rReq.is_write := False

    when(rReq.fire) {
      arReqAddr := arReqAddr + bytesPerBeat
      arReqLeft := arReqLeft - 1
    }

    // =================================================================
    // 仲裁 req（写优先，逐拍仲裁）
    // =================================================================
    val arbiter = StreamArbiterFactory.lowerFirst.noLock.build(DRAMSimTransaction(config), 2)
    arbiter.io.inputs(0) << wReq
    arbiter.io.inputs(1) << rReq
    req << arbiter.io.output

    // =================================================================
    // 响应分发（支持 outstanding 读）
    // =================================================================
    val rRespCnt = Reg(UInt(9 bits)) init 0

    arInfoFifo.io.pop.ready := False  // 默认不弹出

    bus.r.valid  := resp.valid && !resp.is_write && arInfoFifo.io.pop.valid
    bus.r.data   := resp.data
    bus.r.id     := arInfoFifo.io.pop.payload.id
    bus.r.last   := (rRespCnt === (arInfoFifo.io.pop.payload.totalBeats - 1))
    bus.r.setOKAY()

    // 写响应直接丢弃，读响应需要 FIFO 中有对应突发信息
    resp.ready := Mux(resp.is_write, True, bus.r.ready && arInfoFifo.io.pop.valid)

    when(bus.r.fire) {
      rRespCnt := rRespCnt + 1
      when(bus.r.last) {
        rRespCnt := 0
        arInfoFifo.io.pop.ready := True  // 弹出已完成的突发信息
      }
    }
  }

  /** 连接 AXI4 只读接口（兼容 SpAttenFull） */
  def fromAxi4ReadOnly(bus: Axi4ReadOnly): Unit = {
    val fifoId = StreamFifo(cloneOf(bus.ar.id), 64)
    val arForked = StreamFork2(bus.ar)

    arForked._1.translateInto(fifoId.io.push) { (to, from) => to := from.id }
    arForked._2.translateInto(req) { (to, from) =>
      to.addr     := from.addr
      to.data.assignDontCare()
      to.is_write := False
    }

    StreamJoin.arg(fifoId.io.pop, resp).translateInto(bus.r) { (to, from) =>
      to.data := resp.data
      to.last := True
      to.id   := fifoId.io.pop.payload
    }
  }
}

// =============================================================================
// DRAMSimDPIDriver BlackBox：映射到 DRAMSimDPIDriver.sv
// =============================================================================
class DRAMSimDPIDriver(val config: DRAMSimConfig) extends BlackBox {
  val io = new Bundle {
    val clk  = in Bool()
    val rst  = in Bool()
    val dram = slave(DRAMSim(config))
  }
  addGeneric("addressWidth", config.addressWidth)
  addGeneric("dataWidth", config.dataWidth)
  noIoPrefix()
  mapCurrentClockDomain(clock = io.clk, reset = io.rst)
}

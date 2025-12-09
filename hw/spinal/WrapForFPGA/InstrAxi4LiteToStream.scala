package WrapForFPGA

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.amba4.axilite._

// =============================================================
// 1. 配置类
// =============================================================
case class Axi4LiteToStreamConfig(
                                   axiDataWidth: Int = 32,
                                   axiAddrWidth: Int = 12,
                                   fifoDepth: Int = 4,
                                   ctrlRegAddr: Int = 0x10
                                 ) {
  // [修改 2] 返回 AxiLite4Config，它非常简单，不需要配置 useBurst 等
  def getAxiConfig = AxiLite4Config(
    addressWidth = axiAddrWidth,
    dataWidth    = axiDataWidth
  )
}


// =============================================================
// 2. 通用核心模块 (Generic Core)
// =============================================================
class Axi4LiteToStream[T <: Data](
                                   config: Axi4LiteToStreamConfig,
                                   dataType: HardType[T],
                                   // [修改 3] 回调函数类型改为 AxiLite4SlaveFactory
                                   mappingFunc: (AxiLite4SlaveFactory, T) => Unit
                                 ) extends Component {

  val io = new Bundle {
    // [修改 4] 接口类型改为 AxiLite4
    val s_axi = slave(AxiLite4(config.getAxiConfig))
    val m_stream = master(Stream(dataType()))
    val busy = out Bool()
  }

  val shadowReg = RegInit(dataType().getZero)

  val busCtrl = new AxiLite4SlaveFactory(io.s_axi)

  mappingFunc(busCtrl, shadowReg)

  val fireTrigger = False
  busCtrl.onWrite(config.ctrlRegAddr) {
    fireTrigger := True
  }

  val fifo = StreamFifo(dataType(), config.fifoDepth)
  fifo.io.push.valid    := fireTrigger
  fifo.io.push.payload  := shadowReg

  busCtrl.read(!fifo.io.push.ready, address = config.ctrlRegAddr, bitOffset = 0)

  io.m_stream <> fifo.io.pop
  io.busy     := !fifo.io.push.ready
}

// =============================================================
// 3. 128-bit 指令包装器
// =============================================================
case class Instruction128() extends Bundle {
  val SLICE0 = UInt(32 bits)
  val SLICE1 = UInt(32 bits)
  val SLICE2 = UInt(32 bits)
  val SLICE3 = UInt(32 bits)
}

class Inst128_Wrapper(use64BitBus: Boolean = false) extends Component {

  val axiWidth = if (use64BitBus) 64 else 32

  val cfg = Axi4LiteToStreamConfig(
    axiDataWidth = axiWidth,
    axiAddrWidth = 12,
    fifoDepth    = 4,
    ctrlRegAddr  = 0x10
  )

  // [修改 6] 回调函数签名更新
  // 在 Inst128_Wrapper 类中

  // 定义映射策略
  def myMapping(factory: AxiLite4SlaveFactory, reg: Instruction128): Unit = {
    val base = 0x00 // 数据寄存器从 0x00 开始

    if (use64BitBus) {
      // ==========================================
      // 64-bit 模式映射策略
      // ==========================================
      // 必须映射到 8字节对齐的地址，并利用 bitOffset 区分高低位

      // Word 0 (Address 0x10)
      factory.write(reg.SLICE0, address = base + 0x00, bitOffset = 0)
      factory.write(reg.SLICE1, address = base + 0x00, bitOffset = 32)

      // Word 1 (Address 0x18) —— 注意这里是 +0x08，而不是 +0x08 和 +0x0C
      factory.write(reg.SLICE2, address = base + 0x08, bitOffset = 0)
      factory.write(reg.SLICE3, address = base + 0x08, bitOffset = 32)

    } else {
      // ==========================================
      // 32-bit 模式映射策略 (原逻辑)
      // ==========================================
      factory.write(reg.SLICE0, address = base + 0x00)
      factory.write(reg.SLICE1, address = base + 0x04)
      factory.write(reg.SLICE2, address = base + 0x08)
      factory.write(reg.SLICE3, address = base + 0x0C)
    }
  }

  val core = new Axi4LiteToStream(
    config      = cfg,
    dataType    = Instruction128(),
    mappingFunc = myMapping
  )

  val io = new Bundle {
    // [修改 7] 顶层接口也需要是 AxiLite4
    val s_axi    = slave(AxiLite4(cfg.getAxiConfig))
    val m_stream = master(Stream(Bits(Instruction128().getBitsWidth bits)))
    val busy     = out Bool()  // 添加 busy 输出
  }
  //io.s_axi.b.valid := io.s_axi.aw.valid && io.s_axi.w.valid
  //Sio.s_axi.b.payload.resp := 0
  io.busy     := core.io.busy  // 连接 busy 信号
  io.s_axi    <> core.io.s_axi
  io.m_stream.valid    := core.io.m_stream.valid
  io.m_stream.payload  := core.io.m_stream.payload.asBits
  core.io.m_stream.ready := io.m_stream.ready
}


object Inst128_Wrapper_Verilog extends App {

  SpinalConfig(
    targetDirectory = "rtl/Inst128_Wrapper",
    oneFilePerComponent = false,
    defaultConfigForClockDomains = ClockDomainConfig(
      resetActiveLevel = LOW
    )
  ).generateVerilog(new Inst128_Wrapper(use64BitBus = true))
}
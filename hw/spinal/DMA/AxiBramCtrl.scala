package DMA

import DataPump._

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import spinal.lib.fsm._

// =============================================================================
// AXI BRAM Controller
//
// 功能类似 Xilinx axi_bram_ctrl，将 AXI4 Slave 接口映射到
// MemoryReadPort_TypeDef / MemoryWritePort_TypeDef 存储端口。
//
// 特性：
//   - 支持 INCR 突发（burst），单拍和多拍
//   - 写字节选通（strobe → Wen）
//   - 顺序处理：一次只处理一个读或写事务
//   - 1 拍 BRAM 读延迟（同步 RAM）
//   - 写优先（当读写同时到达时）
// =============================================================================

case class AxiBramCtrlConfig(
  axiConfig: Axi4Config,
  memAddrWidth: Int     // 存储端口地址位宽（字节地址）
)

case class AxiBramCtrl(cfg: AxiBramCtrlConfig) extends Component {
  val dataWidth = cfg.axiConfig.dataWidth

  val io = new Bundle {
    val axi      = slave(Axi4(cfg.axiConfig))
    val memRead  = master(MemoryReadPort_TypeDef(cfg.memAddrWidth, dataWidth))
    val memWrite = master(MemoryWritePort_TypeDef(cfg.memAddrWidth, dataWidth))
  }

  // 时钟/复位连接
  io.memRead.clk  := ClockDomain.current.readClockWire
  io.memRead.rst  := ClockDomain.current.readResetWire
  io.memWrite.clk := ClockDomain.current.readClockWire
  io.memWrite.rst := ClockDomain.current.readResetWire

  // 默认输出
  io.memRead.Valid   := False
  io.memRead.Address := 0
  io.memWrite.Valid   := False
  io.memWrite.Address := 0
  io.memWrite.Data    := 0
  io.memWrite.Wen     := 0

  io.axi.ar.ready := False
  io.axi.aw.ready := False
  io.axi.w.ready  := False
  io.axi.r.valid  := False
  io.axi.r.data   := 0
  io.axi.r.last   := False
  if (cfg.axiConfig.useId) io.axi.r.id := 0
  io.axi.r.setOKAY()
  io.axi.b.valid  := False
  if (cfg.axiConfig.useId) io.axi.b.id := 0
  io.axi.b.setOKAY()

  val bytePerBeat = dataWidth / 8

  // 事务寄存器
  val addrReg  = Reg(UInt(cfg.axiConfig.addressWidth bits)) init 0
  val lenReg   = Reg(UInt(8 bits)) init 0
  val beatCnt  = Reg(UInt(8 bits)) init 0
  val idReg    = cfg.axiConfig.useId generate Reg(UInt(cfg.axiConfig.idWidth bits))

  // 读数据流水：BRAM 同步读有 1 拍延迟
  val rdDataValid = RegNext(False) init False
  val rdDataLast  = RegNext(False) init False

  val fsm = new StateMachine {
    val IDLE     = new State with EntryPoint
    val WR_DATA  = new State
    val WR_RESP  = new State
    val RD_CMD   = new State
    val RD_DATA  = new State

    IDLE.whenIsActive {
      // 写优先：只需 aw.valid 即可接受写地址
      when(io.axi.aw.valid) {
        io.axi.aw.ready := True
        addrReg := io.axi.aw.addr.resized
        lenReg  := io.axi.aw.len
        beatCnt := 0
        if (cfg.axiConfig.useId) idReg := io.axi.aw.id
        goto(WR_DATA)
      }.elsewhen(io.axi.ar.valid) {
        io.axi.ar.ready := True
        addrReg := io.axi.ar.addr.resized
        lenReg  := io.axi.ar.len
        beatCnt := 0
        if (cfg.axiConfig.useId) idReg := io.axi.ar.id
        goto(RD_CMD)
      }
    }

    // --- 写通道 ---
    WR_DATA.whenIsActive {
      io.axi.w.ready := True
      when(io.axi.w.valid) {
        io.memWrite.Valid   := True
        io.memWrite.Address := addrReg.resized
        io.memWrite.Data    := io.axi.w.data
        io.memWrite.Wen     := io.axi.w.strb

        addrReg := addrReg + bytePerBeat
        beatCnt := beatCnt + 1

        when(beatCnt === lenReg) {
          goto(WR_RESP)
        }
      }
    }

    WR_RESP.whenIsActive {
      io.axi.b.valid := True
      if (cfg.axiConfig.useId) io.axi.b.id := idReg
      when(io.axi.b.ready) {
        goto(IDLE)
      }
    }

    // --- 读通道 ---
    RD_CMD.whenIsActive {
      // 发出第一个读命令
      io.memRead.Valid   := True
      io.memRead.Address := addrReg.resized
      addrReg := addrReg + bytePerBeat
      rdDataValid := True
      rdDataLast  := (lenReg === 0)
      when(lenReg === 0) {
        goto(RD_DATA)
      }.otherwise {
        beatCnt := 1
        goto(RD_DATA)
      }
    }

    RD_DATA.whenIsActive {
      // 当前拍有读数据返回（上一拍发出的命令）
      when(rdDataValid) {
        io.axi.r.valid := True
        io.axi.r.data  := io.memRead.Data
        io.axi.r.last  := rdDataLast
        if (cfg.axiConfig.useId) io.axi.r.id := idReg

        when(io.axi.r.ready) {
          when(rdDataLast) {
            rdDataValid := False
            goto(IDLE)
          }.otherwise {
            // 发出下一拍读命令
            io.memRead.Valid   := True
            io.memRead.Address := addrReg.resized
            addrReg := addrReg + bytePerBeat
            beatCnt := beatCnt + 1
            rdDataValid := True
            rdDataLast  := (beatCnt === lenReg)
          }
        }.otherwise {
          // R 通道反压，暂停流水
          rdDataValid := True  // 保持
        }
      }
    }
  }
}

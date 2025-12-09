package playGround

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._

case class InstAxi4ToStream128_Config(
    axiCfg    : Axi4Config = Axi4Config(
      addressWidth = 32,
      dataWidth    = 128,
      idWidth      = 16,
      useId        = true,
      useBurst     = true,
      useLock      = false,
      useRegion    = false,
      useQos       = false,
      useStrb      = false
    ),
    fifoDepth : Int = 32
)

case class InstAxi4ToStream128(cfg: InstAxi4ToStream128_Config) extends Component {

  val io = new Bundle {
    val axi = slave(Axi4(cfg.axiCfg))
    val out = master(Stream(UInt(128 bits)))
  }

  val bridge = new Axi4CompatBridge128(cfg.axiCfg)
  bridge.io.s <> io.axi
  val bus = bridge.io.m

  private val push = Stream(UInt(128 bits))
  private val outStream =
    if (cfg.fifoDepth > 1) {
      val fifo = StreamFifo(UInt(128 bits), cfg.fifoDepth)
      fifo.io.push << push
      fifo.io.pop
    } else {
      push
    }
  io.out << outStream

  val axif = Axi4SlaveFactory(bus)

  axif.driveStream(
    that      = push,
    address   = 0x000,
    bitOffset = 0
  )
}


/** AXI4 写地址 + 写数据 合并桥 */
class Axi4CompatBridge128(cfg: Axi4Config) extends Component {
  val io = new Bundle {
    val s = slave(Axi4(cfg))
    val m = master(Axi4(cfg))
  }

  // 写地址缓存
  val awHold = RegInit(False)
  val awReg  = Reg(Axi4Aw(cfg))
  io.s.aw.ready := !awHold
  when(io.s.aw.fire){ awHold := True; awReg := io.s.aw.payload }

  // 写数据缓存
  val wHold = RegInit(False)
  val wReg  = Reg(Axi4W(cfg))
  io.s.w.ready := !wHold
  when(io.s.w.fire){ wHold := True; wReg := io.s.w.payload }

  // 同拍输出
  io.m.aw.valid   := awHold
  io.m.aw.payload := awReg
  io.m.w.valid    := wHold
  io.m.w.payload  := wReg

  when(io.m.aw.fire && io.m.w.fire){
    awHold := False
    wHold  := False
  }

  // 写响应透传
  io.m.b.ready   := io.s.b.ready
  io.s.b.valid   := io.m.b.valid
  io.s.b.payload := io.m.b.payload

  // 读通道透传
  io.m.ar << io.s.ar
  io.s.r  << io.m.r
}



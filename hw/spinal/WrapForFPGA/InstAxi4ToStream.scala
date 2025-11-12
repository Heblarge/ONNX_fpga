package WrapForFPGA

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._

/** InstAxi4ToStream_Config
 * 外部配置结构体：载荷类型 / AXI 配置 / 输出FIFO深度
 */
case class InstAxi4ToStream_Config[T <: Data](
                                               payloadType : HardType[T],
                                               axiCfg      : Axi4Config = Axi4Config(
                                                 addressWidth = 32,
                                                 dataWidth = 256,
                                                 idWidth = 0,
                                                 useId = false,
                                                 useBurst = true,
                                                 useLock = false,
                                                 useRegion = false,
                                                 useQos = false,
                                                 useStrb = false
                                               ),
                                               fifoDepth   : Int = 2
                                             )

case class InstAxi4ToStream[T <: Data](cfg: InstAxi4ToStream_Config[T]) extends Component {

  /** 顶层接口定义 */
  val io = new Bundle {
    val axi = slave(Axi4(cfg.axiCfg))
    val out = master(Stream(cloneOf(cfg.payloadType())))
  }

  /** 兼容桥：上游(s)可异步，桥内缓存；下游(m)同拍输出给 SlaveFactory */
  val bridge = new Axi4CompatBridge(cfg.axiCfg)
  bridge.io.s <> io.axi
  val bus = bridge.io.m

  /** 宽度约束：单拍写，payload 宽度必须 ≤ AXI 数据宽度 */
  private val DW = cfg.axiCfg.dataWidth
  private val W  = widthOf(cfg.payloadType())
  require(W <= DW, s"payload width ($W) must be <= AXI dataWidth ($DW) for single-beat write")

  private val DATA_ADDR = 0x000

  val push = Stream(cloneOf(cfg.payloadType()))
  val outStream =
    if (cfg.fifoDepth > 1) {
      val fifo = StreamFifo(cloneOf(cfg.payloadType()), cfg.fifoDepth)
      fifo.io.push << push
      fifo.io.pop
    } else {
      push
    }
  io.out << outStream

  val axif = Axi4SlaveFactory(bus)

  axif.driveStream(
    that      = push,
    address   = DATA_ADDR,
    bitOffset = 0          // 低位对齐；T 宽度不足 DW 时仅取低 W 位
  )
}

/** Axi4CompatBridge
 * 目的：缓存 AW/W，直到两者齐，再向下游同拍输出；读通道透传，写响应透传。
 * 这样可兼容“AW先来或W先来”的异步写入风格，同时满足 Axi4SlaveFactory 的 join 语义。
 */
class Axi4CompatBridge(cfg: Axi4Config) extends Component {
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

  // 下游同拍输出（两路齐后可各自 ready 决定 fire，但桥仅在两路都 fire 才清空缓存）
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


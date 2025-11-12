package WrapForFPGA

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axilite._

/** AXI4-Lite → Stream[T] 桥接
 * - payload 任意位宽，拆成 AXI dataWidth(32/64) 多次写入
 * - 最后写入或软件 commit 后一次性发出 Stream
 * - 自动地址对齐与布局，避免地址重叠
 */
class InstAxiLiteToStream[T <: Data](
                                      payloadType: HardType[T],
                                      axiCfg: AxiLite4Config = AxiLite4Config(addressWidth = 12, dataWidth = 64) // 可设32或64
                                    ) extends Component {

  // ---------------- IO ----------------
  val io = new Bundle {
    val axi = slave(AxiLite4(axiCfg))
    val out = master(Stream(cloneOf(payloadType())))
  }

  // ---------------- 参数 ----------------
  val W          = widthOf(payloadType())
  val WORD_W     = axiCfg.dataWidth                 // 32 or 64
  val WORD_BYTES = WORD_W/8
  val WORD_NUM   = (W + WORD_W - 1) / WORD_W

  // ---------------- 地址布局（自动对齐，无重叠） ----------------
  @inline def alignUp(x: Int, a: Int) = ((x + a - 1) / a) * a
  val DATA_BASE  = 0x000
  val DATA_END   = DATA_BASE + WORD_NUM * WORD_BYTES
  val MIN_CTRL   = 0x100                             // 至少从 0x100 开始，便于人读
  val CTRL_ADDR  = Math.max(MIN_CTRL, alignUp(DATA_END, WORD_BYTES))
  val STAT_ADDR  = CTRL_ADDR + WORD_BYTES            // 紧跟其后，且按总线宽度对齐

  // 保护：避免空间溢出/重叠
  require(CTRL_ADDR % WORD_BYTES == 0, "CTRL_ADDR 未按 dataWidth 对齐")
  require(STAT_ADDR % WORD_BYTES == 0, "STAT_ADDR 未按 dataWidth 对齐")
  require(DATA_END <= CTRL_ADDR, "DATA 区与 CTRL 区发生重叠，请加大 addressWidth 或减少 payload 宽度")

  // ---------------- 寄存器 ----------------
  val words    = Vec(Reg(Bits(WORD_W bits)) init(0), WORD_NUM)
  val hasWords = RegInit(False)   // 已写满一次 payload
  val bufValid = RegInit(False)   // 输出缓冲占用
  val busy     = RegInit(False)   // 对外可见 busy
  val commit   = Bool()
  val clear    = Bool()
  commit := False; clear := False

  val axif = AxiLite4SlaveFactory(io.axi)

  // ---------------- WORD[i] 数据窗口 ----------------
  for(i <- 0 until WORD_NUM){
    val addr = DATA_BASE + i * WORD_BYTES
    axif.readAndWrite(words(i), address = addr)
    // 最后一个分片写入后，置位 hasWords（Scala层展开，无额外比较器）
    if(i == WORD_NUM-1){
      axif.onWrite(addr){ hasWords := True }
    }
  }

  // ---------------- CTRL 寄存器（对齐地址） ----------------
  axif.onWrite(CTRL_ADDR){
    val wdata = io.axi.writeData.payload.data  // 注意：Stream[AxiLite4W] 的 payload.data
    when(wdata(0)){ commit := True }   // bit0=commit
    when(wdata(1)){ clear  := True }   // bit1=clear
  }

  // ---------------- STAT 寄存器（对齐地址） ----------------
  // bit0=busy, bit1=ready, bit2=hasWords
  val stat = Bits(WORD_W bits)
  stat := (B(busy ## io.out.ready ## hasWords) ## B(0, (WORD_W - 3) bits))
  axif.read(stat, address = STAT_ADDR)

  // ---------------- 聚合与发送 ----------------
  val payloadBits = words.asBits.resize(W)
  val payloadReg  = Reg(cloneOf(payloadType()))
  payloadReg.assignFromBits(payloadBits)

  when(clear){
    hasWords := False
    bufValid := False
    busy     := False
  }

  when(commit && hasWords && !bufValid){
    bufValid := True
    busy     := True
    hasWords := False
  }

  io.out.valid   := bufValid
  io.out.payload := payloadReg
  when(io.out.fire){
    bufValid := False
    busy     := False
  }
}


object InstAxiLiteToStream_Verilog extends App {
  case class MyInst() extends Bundle {
    val opcode = Bits(8 bits)
    val addr   = UInt(16 bits)
    val data   = Bits(64 bits)
    val flag   = Bool()
  }

  SpinalConfig(
    targetDirectory = "rtl/InstAxiLiteToStream",
    oneFilePerComponent = true
  ).generateVerilog(new InstAxiLiteToStream(HardType(MyInst())))
}

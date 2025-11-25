package StreamProcessor

import Util._
import Interface._

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._

case class AxiStreamConfig(
  dataWidth : Int,
  useLast   : Boolean = true
)

case class AxiStream(cfg: AxiStreamConfig) extends Bundle with IMasterSlave {

  val tdata = Bits(cfg.dataWidth bits)
  val tvalid = Bool()
  val tready = Bool()
  val tlast = if(cfg.useLast) Bool() else null

  override def asMaster() = {
    out(tdata, tvalid)
    in(tready)
    if(cfg.useLast) out(tlast)
  }
}

case class Axi4StreamShifter_Config[T <: Data](
  payloadType : HardType[T],
  axisCfg     : AxiStreamConfig,
  fifoDepth   : Int = 8,
  UIDWidth    : Int,
  ShiftWidth  : Int,
  AddressWidth: Int,
  ShapeWidth  : Int
)

case class Axi4StreamShifter[T <: Data](cfg: Axi4StreamShifter_Config[T]) extends Component {

  val io = new Bundle {
    val sAxis = slave(AxiStream(cfg.axisCfg))
    val mAxis = master(AxiStream(cfg.axisCfg))

    val instr = slave(Stream(ComputeInstruction_TypeDef(
      cfg.UIDWidth, cfg.ShiftWidth, cfg.AddressWidth, cfg.ShapeWidth
    )))
  }

  // -------------------------------------
  // 1) 锁存指令的 shiftLeft_AfterMatrixOperation
  // -------------------------------------
  val shiftReg = Reg(SInt(cfg.ShiftWidth bits)) init(0)
  io.instr.ready := True
  when(io.instr.fire) {
    shiftReg := io.instr.shiftLeft_AfterMatrixOperation
  }

  // -------------------------------------
  // 2) 将 AXIS 转成 Stream(payloadType)
  //    tdata => payloadType() 的宽度需要 <= axis.dataWidth
  // -------------------------------------
  private val DW = cfg.axisCfg.dataWidth
  private val PW = widthOf(cfg.payloadType())

  require(PW <= DW, s"payload width ($PW) must be <= AXIS dataWidth ($DW)")

  val inStream = Stream(cfg.payloadType())

  inStream.valid := io.sAxis.tvalid
  io.sAxis.tready := inStream.ready
  inStream.payload.assignFromBits(io.sAxis.tdata(PW-1 downto 0))

  // -------------------------------------
  // 3) FIFO 缓冲
  // -------------------------------------
  val fifo = StreamFifo(cfg.payloadType(), cfg.fifoDepth)
  fifo.io.push << inStream
  val fromFifo = fifo.io.pop

    // -------------------------------------
    // 4) 移位逻辑（translateWith 版本，避免重复吐数）
    // -------------------------------------
    val shifted = fromFifo.translateWith {
    val bits = fromFifo.payload.asBits               // PW bits
    val s    = bits.asSInt

    // absShiftU : UInt（abs 已经是 UInt）
    val absShiftU = shiftReg.abs.resized

    // clip absShiftU to [0, PW-1]
    val absShiftClipped = UInt(log2Up(PW) bits)
    absShiftClipped := absShiftU
    when(absShiftU > U(PW-1)) {
        absShiftClipped := U(PW-1)
    }

    val resBits = Bits(PW bits)
    resBits := bits

    when(shiftReg > 0) {
        resBits := (s |<< absShiftClipped).asBits
    } elsewhen(shiftReg < 0) {
        resBits := (s >> absShiftClipped).asBits // 算术右移
    }

    val out = cfg.payloadType()
    out.assignFromBits(resBits)
    out
    }

    // -------------------------------------
    // 5) Stream => AXI-Stream
    // -------------------------------------
    io.mAxis.tvalid := shifted.valid
    shifted.ready   := io.mAxis.tready

    io.mAxis.tdata(PW-1 downto 0) := shifted.payload.asBits
    if(PW < DW) io.mAxis.tdata(DW-1 downto PW) := 0
    if(cfg.axisCfg.useLast) io.mAxis.tlast := False

}

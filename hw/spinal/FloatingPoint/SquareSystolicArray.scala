package FloatingPoint

import Interface.MatrixOperation_TypeDef
import spinal.core._
import spinal.lib._

case class SquareSystolicArray_Config(
    in_Length_Max: Int,
    in_Length_Min: Int = 1,
    in_MatA_row_num: Int = 4,
    in_MatB_col_num: Int = 4,
    fpConfig: FpxxConfig = FpxxConfig.float16(),
    accIntBits: BitCount = 16 bits,
    accFracBits: BitCount = 16 bits,
    mulStages: Int = 1,
    f2iStages: Int = 1,
    Enable_Transpose_logic: Boolean = true,
    Enable_ElementWise_logic: Boolean = true
) {
  require(in_MatA_row_num == in_MatB_col_num, "SquareSystolicArray requires square array")
  require(in_Length_Min > 0 && in_Length_Min <= in_Length_Max, "invalid in_Length_Min/in_Length_Max")

  val out_MatZ_row_num: Int = in_MatA_row_num
  val out_MatZ_col_num: Int = in_MatB_col_num
  val out_MatZ_element_Width: Int = accIntBits.value + accFracBits.value
}

case class Fpxx_withFinalMark(c: FpxxConfig) extends Bundle {
  val data = Fpxx(c)
  val Final = Bool()
}

case class SquareSystolicArray_OpMode(cfg: SquareSystolicArray_Config) extends Bundle {
  val post_Shift = SInt(log2Up(cfg.out_MatZ_element_Width + 1) + 1 bits)
  val do_PostTranspose = cfg.Enable_Transpose_logic generate Bool()
  val MatrixOperation = cfg.Enable_ElementWise_logic generate MatrixOperation_TypeDef()

  def do_MatMul: Bool = cfg.Enable_ElementWise_logic match {
    case true  => MatrixOperation === MatrixOperation_TypeDef.MatMul
    case false => True
  }
}

object init_SquareSystolicArray_OpMode {
  def apply(cfg: SquareSystolicArray_Config): SquareSystolicArray_OpMode = {
    val mode = SquareSystolicArray_OpMode(cfg)
    mode.post_Shift := 0
    if (cfg.Enable_Transpose_logic) {
      mode.do_PostTranspose := False
    }
    if (cfg.Enable_ElementWise_logic) {
      mode.MatrixOperation := MatrixOperation_TypeDef.MatMul
    }
    mode
  }
}

case class SquareSystolicArray(cfg: SquareSystolicArray_Config) extends Component {
  class in_Mats_TypeDef(cfg: SquareSystolicArray_Config) extends Bundle {
    val A = Vec.fill(cfg.in_MatA_row_num)(Fpxx_withFinalMark(cfg.fpConfig))
    val B = Vec.fill(cfg.in_MatB_col_num)(Fpxx_withFinalMark(cfg.fpConfig))
    val OpMode = SquareSystolicArray_OpMode(cfg)
  }

  class out_Mats_TypeDef(cfg: SquareSystolicArray_Config) extends Bundle {
    val Z = Vec.fill(cfg.out_MatZ_row_num)(Vec.fill(cfg.out_MatZ_col_num)(Fpxx(cfg.fpConfig)))
  }

  def in_Mats_Bundle(): in_Mats_TypeDef = new in_Mats_TypeDef(cfg)
  def out_Mats_Bundle(): out_Mats_TypeDef = new out_Mats_TypeDef(cfg)

  val io = new Bundle {
    val in_Mats = slave(Stream(in_Mats_Bundle()))
    val out_Mats = master(Stream(out_Mats_Bundle()))
  }

  val mode_reg = Reg(SquareSystolicArray_OpMode(cfg)) init init_SquareSystolicArray_OpMode(cfg)
  val latched = Reg(Bool()) init False
  when(io.in_Mats.fire && !latched) {
    mode_reg := io.in_Mats.payload.OpMode
    latched := True
  }
  when(io.in_Mats.fire && io.in_Mats.payload.A(0).Final) {
    latched := False
  }
  val latched_mode = SquareSystolicArray_OpMode(cfg)
  latched_mode := Mux(!latched, io.in_Mats.payload.OpMode, mode_reg)

  val frameActive = Reg(Bool()) init False
  val waitingResult = Reg(Bool()) init False
  val waitingSawPeActivity = Reg(Bool()) init False
  val pendingResult = Reg(Bool()) init False
  val outPayloadValid = Bool()
  val anyPeOutValid = Bool()
  val frameDoTranspose = cfg.Enable_Transpose_logic generate Reg(Bool()) init False

  io.in_Mats.ready := !waitingResult && !pendingResult
  io.out_Mats.valid := outPayloadValid

  val inputFire = io.in_Mats.fire
  val clearPulse = inputFire && !frameActive

  when(inputFire && !frameActive) {
    frameActive := True
    if (cfg.Enable_Transpose_logic) {
      frameDoTranspose := io.in_Mats.payload.OpMode.do_PostTranspose
    }
  }

  when(inputFire && io.in_Mats.payload.A(0).Final) {
    frameActive := False
    waitingResult := True
    waitingSawPeActivity := False
  }

  when(waitingResult) {
    when(anyPeOutValid) {
      waitingSawPeActivity := True
    } elsewhen(waitingSawPeActivity) {
      // Once all PE accumulators stop updating, outputs are stable for this frame.
      waitingResult := False
      pendingResult := True
    }
  }

  when(io.out_Mats.fire) {
    pendingResult := False
  }

  val zeroFp = Fpxx(cfg.fpConfig)
  zeroFp.sign := False
  zeroFp.exp := 0
  zeroFp.mant := 0

  val peMatrix = Array.tabulate(cfg.in_MatA_row_num, cfg.in_MatB_col_num) { (r, c) =>
    val pe = new FpxxPE(
      fpxxCfg = cfg.fpConfig,
      accIntBits = cfg.accIntBits,
      accFracBits = cfg.accFracBits,
      mulStages = cfg.mulStages,
      f2iStages = cfg.f2iStages
    )
    pe.setName(s"PE_${r}_${c}")
    pe
  }

  val aIngress = Array.tabulate(cfg.in_MatA_row_num) { r =>
    var a = Mux(inputFire, io.in_Mats.payload.A(r).data, zeroFp)
    for (_ <- 0 until r) {
      a = RegNext(a)
    }
    a
  }

  val bIngress = Array.tabulate(cfg.in_MatB_col_num) { c =>
    var b = Mux(inputFire, io.in_Mats.payload.B(c).data, zeroFp)
    for (_ <- 0 until c) {
      b = RegNext(b)
    }
    b
  }

  val validIngress = Array.tabulate(cfg.in_MatA_row_num) { r =>
    var v = inputFire
    for (_ <- 0 until r) {
      v = RegNext(v) init False
    }
    v
  }

  for (r <- 0 until cfg.in_MatA_row_num) {
    for (c <- 0 until cfg.in_MatB_col_num) {
      val pe = peMatrix(r)(c)
      pe.io.clear := clearPulse

      if (c == 0) {
        pe.io.inSig.valid := validIngress(r)
        pe.io.inSig.payload.a := aIngress(r)
      } else {
        pe.io.inSig.valid := RegNext(peMatrix(r)(c - 1).io.inSig.valid) init False
        pe.io.inSig.payload.a := RegNext(peMatrix(r)(c - 1).io.inSig.payload.a)
      }

      if (r == 0) {
        pe.io.inSig.payload.b := bIngress(c)
      } else {
        pe.io.inSig.payload.b := RegNext(peMatrix(r - 1)(c).io.inSig.payload.b)
      }
    }
  }

  val selectedFix = Array.tabulate(cfg.out_MatZ_row_num, cfg.out_MatZ_col_num) { (r, c) =>
    val zFix = AFix.SQ(cfg.accIntBits, cfg.accFracBits)
    zFix := peMatrix(r)(c).io.out.payload
    if (cfg.Enable_Transpose_logic) {
      // Use per-frame latched transpose bit to avoid any cross-frame control skew.
      when(frameDoTranspose) {
        zFix := peMatrix(c)(r).io.out.payload
      }
    }
    zFix
  }

  val outConverters = Array.tabulate(cfg.out_MatZ_row_num, cfg.out_MatZ_col_num) { (r, c) =>
    val conv = new AFix2Fpxx(
      intNrBits = cfg.accIntBits,
      fracNrBits = cfg.accFracBits,
      c = cfg.fpConfig,
      pipeStages = 0
    )
    conv.io.op.valid := pendingResult
    conv.io.op.payload.number := selectedFix(r)(c)
    conv
  }

  outPayloadValid := pendingResult && outConverters(0)(0).io.result.valid
  anyPeOutValid := peMatrix.flatten.map(_.io.out.valid).reduce(_ || _)

  for (r <- 0 until cfg.out_MatZ_row_num; c <- 0 until cfg.out_MatZ_col_num) {
    io.out_Mats.payload.Z(r)(c) := outConverters(r)(c).io.result.payload
  }
}

object SquareSystolicArrayApp extends App {
  SpinalVerilog(
    SquareSystolicArray(
      SquareSystolicArray_Config(
        in_Length_Max = 4,
        in_MatA_row_num = 4,
        in_MatB_col_num = 4,
        fpConfig = FpxxConfig.float16(),
        accIntBits = 16 bits,
        accFracBits = 16 bits
      )
    )
  )
}

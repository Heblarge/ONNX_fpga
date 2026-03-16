package FloatingPoint

import Interface.MatrixOperation_TypeDef
import spinal.core._
import spinal.lib._

/**
  * GEMM = A * B + bias (bias is fixed-point, added per column).
  * Based on SquareSystolicArray, but with an extra Bias vector input.
  */
case class GEMM(cfg: SquareSystolicArray_Config) extends Component {
  class in_Mats_TypeDef(cfg: SquareSystolicArray_Config) extends Bundle {
    val A = Vec.fill(cfg.in_MatA_row_num)(Fpxx_withFinalMark(cfg.fpConfig))
    val B = Vec.fill(cfg.in_MatB_col_num)(Fpxx_withFinalMark(cfg.fpConfig))
    // bias is fixed-point, one value per output column
    val Bias = Vec.fill(cfg.out_MatZ_col_num)(AFix.SQ(cfg.accIntBits, cfg.accFracBits))
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
  val pendingResult = Reg(Bool()) init False
  val outPayloadValid = Bool()
  val frameDoTranspose = cfg.Enable_Transpose_logic generate Reg(Bool()) init False

  // latch bias at frame start to align with the whole frame
  val bias_reg = Reg(Vec.fill(cfg.out_MatZ_col_num)(AFix.SQ(cfg.accIntBits, cfg.accFracBits)))
  bias_reg.foreach(_.init(0))

  io.in_Mats.ready := !waitingResult && !pendingResult
  io.out_Mats.valid := outPayloadValid

  val inputFire = io.in_Mats.fire
  val clearPulse = inputFire && !frameActive

  when(inputFire && !frameActive) {
    frameActive := True
    if (cfg.Enable_Transpose_logic) {
      frameDoTranspose := io.in_Mats.payload.OpMode.do_PostTranspose
    }
    bias_reg := io.in_Mats.payload.Bias
  }

  when(inputFire && io.in_Mats.payload.A(0).Final) {
    frameActive := False
    waitingResult := True
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

  val peCount = cfg.in_MatA_row_num * cfg.in_MatB_col_num
  val pendingMacWidth = log2Up(cfg.in_Length_Max * peCount + 1)
  val pendingMacs = Reg(UInt(pendingMacWidth bits)) init 0

  val injectedMacs = UInt(pendingMacWidth bits)
  injectedMacs := 0
  when(inputFire) {
    injectedMacs := peCount
  }

  val retiredMacs = UInt(pendingMacWidth bits)
  retiredMacs := peMatrix.flatten
    .map(_.io.out.valid.asUInt.resize(pendingMacWidth))
    .reduce(_ + _)

  val pendingMacsNext = UInt(pendingMacWidth bits)
  pendingMacsNext := (pendingMacs + injectedMacs - retiredMacs).resized
  pendingMacs := pendingMacsNext

  when(waitingResult && pendingMacsNext === 0) {
    waitingResult := False
    pendingResult := True
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
      when(frameDoTranspose) {
        zFix := peMatrix(c)(r).io.out.payload
      }
    }
    val zWithBias = AFix.SQ(cfg.accIntBits, cfg.accFracBits)
    zWithBias := zFix
    when(latched_mode.do_MatMul) {
      zWithBias := (zFix + bias_reg(c)).truncated
    }
    zWithBias
  }

  val outConverters = Array.tabulate(cfg.out_MatZ_row_num, cfg.out_MatZ_col_num) { (r, c) =>
    val conv = new AFix2Fpxx(
      intNrBits = cfg.accIntBits,
      fracNrBits = cfg.accFracBits,
      c = cfg.fpConfig,
      pipeStages = cfg.af2fStages
    )
    conv.io.op.valid := pendingResult
    conv.io.op.payload.number := selectedFix(r)(c)
    conv
  }

  outPayloadValid := pendingResult && outConverters(0)(0).io.result.valid

  for (r <- 0 until cfg.out_MatZ_row_num; c <- 0 until cfg.out_MatZ_col_num) {
    io.out_Mats.payload.Z(r)(c) := outConverters(r)(c).io.result.payload
  }
}


package FloatingPoint

import spinal.core._
import spinal.lib._
import spinal.lib.misc.pipeline._

class FpxxPE(
    fpxxCfg     : FpxxConfig,
    accIntBits  : BitCount,
    accFracBits : BitCount,
    mulStages   : StageMask = 1,
    f2iStages   : Int = 1
) extends Component {

  // ------------------------------------------------------------
  // IO
  // ------------------------------------------------------------
  val io = new Bundle {
    val inSig = slave Stream(new Bundle {
      val a = Fpxx(fpxxCfg)
      val b = Fpxx(fpxxCfg)
    })

    val clear = in Bool()

    val out = master Stream(AFix.SQ(accIntBits, accFracBits))
  }

  // ------------------------------------------------------------
  // FP MUL
  // ------------------------------------------------------------
  val mul = new FpxxMulCompatible(
    FpxxMul.Options(
      cIn        = fpxxCfg,
      cOut       = None,
      pipeStages = mulStages
    )
  )

  mul.io.input.valid := io.inSig.valid
  mul.io.input.payload.a := io.inSig.payload.a
  mul.io.input.payload.b := io.inSig.payload.b
  io.inSig.ready := True

  // ------------------------------------------------------------
  // FP → FIX
  // ------------------------------------------------------------
  val f2i = new Fpxx2AFixCompatible(
    intNrBits  = accIntBits,
    fracNrBits = accFracBits,
    c          = fpxxCfg,
    pipeStages = f2iStages,
    generateFlags = false
  )

  f2i.io.op.valid   := mul.io.result.valid
  f2i.io.op.payload := mul.io.result.payload
  

  // ------------------------------------------------------------
  // Local Accumulator (FIXED)
  // ------------------------------------------------------------
  val acc = Reg(AFix.SQ(accIntBits, accFracBits))
  acc.init(0)

  when(io.clear) {
    acc.clearAll()
  }

  val accFire = f2i.io.result.valid

  when(accFire) {
    acc := (acc + f2i.io.result.number).truncated
  }

  // ------------------------------------------------------------
  // Output
  // ------------------------------------------------------------
  io.out.valid   := accFire
  io.out.payload := acc
}

package FloatingPoint

import spinal.core._
import spinal.lib._
import spinal.lib.misc.pipeline._

//如果需要更改浮点格式的话，改动：mul的cin写为对应浮点格式（fp8_xxx/fp16）,cout写为对应浮点乘法结果格式(fp8_xxxmul/fp16mul)；f2i的c写为对应浮点乘法结果格式。

class FpxxPE(
    fpxxCfg     : FpxxConfig,
    accIntBits  : BitCount,
    accFracBits : BitCount,
    mulStages   : StageMask = 1,
    f2iStages   : Int = 1
) extends Component {
  private val mulCfg =
    if (fpxxCfg == FpxxConfig.float16()) {
      FpxxConfig.float16_mul()
    } else if (fpxxCfg == FpxxConfig.float8_e5m2fnuz()) {
      FpxxConfig.float8_e5m2mul()
    } else if (fpxxCfg == FpxxConfig.float8_e4m3fnuz()) {
      FpxxConfig.float8_e4m3mul()
    } else {
      throw new IllegalArgumentException(s"Unsupported FpxxPE input format: $fpxxCfg")
    }

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

    val mulresult = master Stream(AFix.SQ(accIntBits, accFracBits))
  }

  // ------------------------------------------------------------
  // FP MUL
  // ------------------------------------------------------------
  val mul = new FpxxMulCompatible(
    FpxxMul.Options(
      cIn        = fpxxCfg,
      cOut       = Some(mulCfg),
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
    c          = mulCfg,
    pipeStages = f2iStages,
    generateFlags = false
  )

  f2i.io.op.valid   := mul.io.result.valid
  f2i.io.op.payload := mul.io.result.payload
  
  // ------------------------------------------------------------
  // 单次乘法结果输出
  // ------------------------------------------------------------
  io.mulresult.valid   := f2i.io.result.valid
  io.mulresult.payload := f2i.io.result.number

  // ------------------------------------------------------------
  // Local Accumulator (FIXED)
  // ------------------------------------------------------------
  val acc = Reg(AFix.SQ(accIntBits, accFracBits))
  acc.init(0)

  val accFire = f2i.io.result.valid

  when(io.clear) {
    // On frame boundary clear, keep same-cycle first product when low-latency pipelines are used.
    when(accFire) {
      acc := f2i.io.result.number.truncated
    } otherwise {
      acc.clearAll()
    }
  } elsewhen(accFire) {
    acc := (acc + f2i.io.result.number).truncated
  }

  // ------------------------------------------------------------
  // Output
  // ------------------------------------------------------------
  io.out.valid   := accFire
  io.out.payload := acc
}

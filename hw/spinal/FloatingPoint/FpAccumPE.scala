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
      //更改cIn和cOut来决定浮点数的格式
      cIn        = FpxxConfig.float16(),
      cOut       = Some(FpxxConfig.float16_mul()),
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
    c          = FpxxConfig.float16_mul(),//这个也要同步更改
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

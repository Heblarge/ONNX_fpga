package FloatingPoint

import spinal.core._
import spinal.lib._
import spinal.lib.misc.pipeline._

object FpxxMul {
    import caseapp._

    case class Options(
        @HelpMessage(FloatingPoint.Cli.fpxxConfigHelpMsg)
        cIn: FpxxConfig,
        @HelpMessage(FloatingPoint.Cli.fpxxConfigHelpMsg)
        cOut: Option[FpxxConfig] = None,
        @HelpMessage(FloatingPoint.Cli.stageMaskHelpMsg(3))
        pipeStages: StageMask = 1,
        @HelpMessage(FloatingPoint.Cli.roundTypeHelpMsg)
        rounding: RoundType = RoundType.ROUNDTOEVEN
    )
}

case class FpxxMul(o: FpxxMul.Options) extends Component {

    val cOutU      = o.cOut getOrElse o.cIn
    def pipeStages = o.pipeStages

    assert(o.cIn.ieee_like && cOutU.ieee_like, "Can only handle IEEE compliant floats")
    assert(o.cIn.exp_size == cOutU.exp_size, "Can only handle equal input and output exponents")

    val io = new Bundle {
        val input = slave Flow (new Bundle {
            val a = Fpxx(o.cIn)
            val b = Fpxx(o.cIn)
        })
        val result = master Flow (Fpxx(cOutU))
    }

    val n0 = new Node {
        arbitrateFrom(io.input)

        val a = insert(io.input.payload.a)
        val b = insert(io.input.payload.b)
        val is_nan = insert(
          a.is_nan() || b.is_nan() || a.is_zero() && b.is_infinite() || b.is_zero() && a.is_infinite()
        )
        val is_inf    = insert(a.is_infinite() || b.is_infinite())
        val a_is_zero = insert(a.is_zero() || a.is_subnormal())
        val b_is_zero = insert(b.is_zero() || b.is_subnormal())
        val is_zero   = insert(a_is_zero || b_is_zero)

        val mant_a   = insert(U(1, 1 bits) @@ a.mant)
        val mant_b   = insert(U(1, 1 bits) @@ b.mant)
        val sign_mul = insert(a.sign ^ b.sign)
    }

    val n1 = new Node {
        val exp_mul  = insert((n0.a.exp +^ n0.b.exp).intoSInt - o.cIn.bias)
        val mant_mul = insert(n0.mant_a * n0.mant_b)
    }

    val n2 = new Node {
        arbitrateTo(io.result)

        val mant_mul_adj =
            ((n1.mant_mul @@ U(0, 1 bit)) |>> n1.mant_mul.msb.asUInt)(0, n1.mant_mul.getBitsWidth + 1 - 2 bits)

        val mant_mul_rounded =
            mant_mul_adj.fixTo(mant_mul_adj.getWidth downto mant_mul_adj.getWidth - cOutU.mant_size, o.rounding)

        val exp_mul_adj = n1.exp_mul + n1.mant_mul.msb.asUInt.intoSInt + mant_mul_rounded.msb.asUInt.intoSInt

        val result = io.result.payload

        result.sign := n0.sign_mul
        when(n0.is_nan) {
            result.set_nan()
        }.elsewhen(n0.is_inf) {
            result.set_inf()
        }.elsewhen(n0.is_zero || exp_mul_adj <= 0) {
            result.set_zero()
        }.elsewhen(exp_mul_adj >= result.exp.maxValue) {
            result.set_inf()
        }.otherwise {
            result.exp  := exp_mul_adj.asUInt.resized
            result.mant := mant_mul_rounded.resized
        }
    }

    implicit val maskConfig = StageMask.Config(2, List(0, 1))
    Builder(o.pipeStages(Seq(n0, n1, n2)))
}


case class FpxxMulCompatible(o: FpxxMul.Options) extends Component {

  val cOutU      = o.cOut getOrElse o.cIn
  def pipeStages = o.pipeStages

  val io = new Bundle {
    val input = slave Flow (new Bundle {
      val a = Fpxx(o.cIn)
      val b = Fpxx(o.cIn)
    })
    val result = master Flow (Fpxx(cOutU))
  }

  val n0 = new Node {
    arbitrateFrom(io.input)
    val a = insert(io.input.payload.a)
    val b = insert(io.input.payload.b)

    val is_nan = insert(
      a.is_nan() || b.is_nan() || (a.is_zero() && b.is_infinite()) || (b.is_zero() && a.is_infinite())
    )
    val is_inf    = insert(a.is_infinite() || b.is_infinite())
    val a_is_zero = insert(a.is_zero() || a.is_subnormal())
    val b_is_zero = insert(b.is_zero() || b.is_subnormal())
    val is_zero   = insert(a_is_zero || b_is_zero)

    val mant_a    = insert(U(1, 1 bits) @@ a.mant)
    val mant_b    = insert(U(1, 1 bits) @@ b.mant)
    val sign_mul  = insert(a.sign ^ b.sign)
  }

  val n1 = new Node {
    // 1. 尾数乘法保持不变
    val mant_mul = insert(n0.mant_a * n0.mant_b)

    // 2. 指数真值预计算
    // exp_sum_true = (a.exp - Bias_in) + (b.exp - Bias_in)
    // 扩展位宽以安全处理负数
    val expExtWidth = (o.cIn.exp_size max cOutU.exp_size) + 4
    val exp_a_true = n0.a.exp.resize(expExtWidth).asSInt - o.cIn.bias
    val exp_b_true = n0.b.exp.resize(expExtWidth).asSInt - o.cIn.bias
    
    val exp_sum_true = insert(exp_a_true + exp_b_true)
  }

  val n2 = new Node {
    arbitrateTo(io.result)

    // 尾数对齐逻辑
    val mant_mul_adj =
      ((n1.mant_mul @@ U(0, 1 bit)) |>> n1.mant_mul.msb.asUInt)(0, n1.mant_mul.getBitsWidth + 1 - 2 bits)

    val mant_mul_rounded =
      mant_mul_adj.fixTo(mant_mul_adj.getWidth downto mant_mul_adj.getWidth - cOutU.mant_size, o.rounding)

    // --- 关键修改：计算最终指数真值 ---
    // final_true = exp_sum_true + 乘法进位 + 舍入进位
    val final_exp_true = n1.exp_sum_true + n1.mant_mul.msb.asUInt.intoSInt + mant_mul_rounded.msb.asUInt.intoSInt

    // 转换为输出偏置格式
    val exp_biased = final_exp_true + cOutU.bias

    val final_exp = UInt(cOutU.exp_size bits)
    val final_mant = UInt(cOutU.mant_size bits)
    val final_sign = n0.sign_mul

    // 默认赋值
    final_exp  := exp_biased.asUInt.resized
    final_mant := mant_mul_rounded.resized

    // 判定边界
    val maxExpBiased = (1 << cOutU.exp_size) - 1
    
    val is_overflow = Bool()
    cOutU.inf_encoding match {
      case IEEEInfinity() => is_overflow := exp_biased >= maxExpBiased
      case NoInfinity(_)  => is_overflow := exp_biased >  maxExpBiased
    }
    
    // 下溢判定：真值小于输出格式所能表示的最小真值 (1 - Bias)
    val is_underflow = final_exp_true <= -cOutU.bias

    // 结果选择逻辑
    when(n0.is_nan) {
      cOutU.nan_encoding match {
        case IEEENan() =>
          io.result.sign := False
          final_exp.setAll
          final_mant := (cOutU.mant_size - 1 -> True, default -> False)
        case SpecialNan(encoding) =>
          val nanBits = U(encoding, cOutU.full_size bits)
          io.result.sign := nanBits.msb
          final_exp  := nanBits(cOutU.mant_size, cOutU.exp_size bits)
          final_mant := nanBits(0, cOutU.mant_size bits)
      }
    }.elsewhen(n0.is_inf) {
      io.result.sign := final_sign
      cOutU.inf_encoding match {
        case IEEEInfinity() => final_exp.setAll; final_mant.clearAll
        case NoInfinity(_)  => final_exp.setAll; final_mant.setAll
      }
    }.elsewhen(n0.is_zero || is_underflow) {
      io.result.sign := (if (cOutU.signed_zero) final_sign else False)
      final_exp := 0
      final_mant := 0
    }.elsewhen(is_overflow) {
      io.result.sign := final_sign
      cOutU.inf_encoding match {
        case IEEEInfinity() => final_exp.setAll; final_mant.clearAll
        case NoInfinity(_)  => final_exp.setAll; final_mant.setAll
      }
    }.otherwise {
      io.result.sign := final_sign
    }

    // 修复无符号 0
    if (!cOutU.signed_zero) {
      when(final_exp === 0 && final_mant === 0) {
        io.result.sign := False
      }
    }

    io.result.exp  := final_exp
    io.result.mant := final_mant
  }

  implicit val maskConfig = StageMask.Config(2, List(0, 1))
  Builder(o.pipeStages(Seq(n0, n1, n2)))
}


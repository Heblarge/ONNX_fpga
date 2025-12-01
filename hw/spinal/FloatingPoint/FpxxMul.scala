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

  // 移除 IEEE 断言以支持 E4M3/E5M2
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

    // FNUZ 模式下 a.is_infinite 恒为 false，逻辑兼容
    val is_nan = insert(
      a.is_nan() || b.is_nan() || a.is_zero() && b.is_infinite() || b.is_zero() && a.is_infinite()
    )

    val is_inf    = insert(a.is_infinite() || b.is_infinite())

    // Input Flush-to-Zero: 硬件不支持 Subnormal，将其视为 0
    val a_is_zero = insert(a.is_zero() || a.is_subnormal())
    val b_is_zero = insert(b.is_zero() || b.is_subnormal())
    val is_zero   = insert(a_is_zero || b_is_zero)

    // 构造尾数 (隐含 1)
    val mant_a    = insert(U(1, 1 bits) @@ a.mant)
    val mant_b    = insert(U(1, 1 bits) @@ b.mant)
    val sign_mul  = insert(a.sign ^ b.sign)
  }

  val n1 = new Node {
    // 指数预计算 (ExpA + ExpB - Bias)
    val exp_mul  = insert((n0.a.exp +^ n0.b.exp).intoSInt - o.cIn.bias)
    // 尾数乘法
    val mant_mul = insert(n0.mant_a * n0.mant_b)
  }

  val n2 = new Node {
    arbitrateTo(io.result)

    // 尾数对齐与舍入逻辑
    // 如果乘积 >= 2.0 (bit width overflow)，需要右移
    val mant_mul_adj =
      ((n1.mant_mul @@ U(0, 1 bit)) |>> n1.mant_mul.msb.asUInt)(0, n1.mant_mul.getBitsWidth + 1 - 2 bits)

    val mant_mul_rounded =
      mant_mul_adj.fixTo(mant_mul_adj.getWidth downto mant_mul_adj.getWidth - cOutU.mant_size, o.rounding)

    // 最终指数计算：基础指数 + 乘积进位 + 舍入进位
    // 这里的逻辑非常关键：它允许 rounding 产生的进位把指数从 0 推到 1，从而挽救下溢
    val exp_mul_adj = n1.exp_mul + n1.mant_mul.msb.asUInt.intoSInt + mant_mul_rounded.msb.asUInt.intoSInt

    val result = io.result.payload
    val final_exp = UInt(cOutU.exp_size bits)
    val final_mant = UInt(cOutU.mant_size bits)
    val final_sign = Bool()

    final_sign := n0.sign_mul
    final_exp  := exp_mul_adj.asUInt.resized
    final_mant := mant_mul_rounded.resized

    // 溢出判定
    val is_overflow = Bool()
    cOutU.inf_encoding match {
      case IEEEInfinity() =>
        val maxIEEEExp = (1 << cOutU.exp_size) - 1
        is_overflow := exp_mul_adj >= maxIEEEExp
      case NoInfinity(_) =>
        // FNUZ: > MaxExp 才是溢出
        val maxFNUZExp = (1 << cOutU.exp_size) - 1
        is_overflow := exp_mul_adj > maxFNUZExp
    }

    // 结果选择逻辑
    when(n0.is_nan) {
      // NaN
      cOutU.nan_encoding match {
        case IEEENan() =>
          final_sign := False
          final_exp.setAll
          final_mant := (cOutU.mant_size - 1 -> True, default -> False)
        case SpecialNan(encoding) =>
          val nanBits = U(encoding, cOutU.full_size bits)
          final_sign := nanBits.msb
          final_exp  := nanBits(cOutU.mant_size, cOutU.exp_size bits)
          final_mant := nanBits(0, cOutU.mant_size bits)
      }
    }.elsewhen(n0.is_inf) {
      // Inf (FNUZ 不会进入)
      cOutU.inf_encoding match {
        case IEEEInfinity() =>
          final_exp.setAll
          final_mant.clearAll
        case NoInfinity(_) =>
          final_exp.setAll
          final_mant.setAll
      }
    }.elsewhen(n0.is_zero || exp_mul_adj <= 0) {
      // Output Flush-to-Zero
      // 只要最终指数 <= 0，一律归零
      final_exp := 0
      final_mant := 0
    }.elsewhen(is_overflow) {
      // Overflow
      cOutU.inf_encoding match {
        case IEEEInfinity() =>
          final_exp.setAll
          final_mant.clearAll
        case NoInfinity(_) =>
          final_exp.setAll
          final_mant.setAll
      }
    }

    // Unsigned Zero 修复: 只有在非 NaN 且结果确实为 0 时才拉低符号
    io.result.sign := final_sign
    if (!cOutU.signed_zero) {
      when(!n0.is_nan && final_exp === 0 && final_mant === 0) {
        io.result.sign := False
      }
    }

    io.result.exp  := final_exp
    io.result.mant := final_mant
  }

  implicit val maskConfig = StageMask.Config(2, List(0, 1))
  Builder(o.pipeStages(Seq(n0, n1, n2)))
}

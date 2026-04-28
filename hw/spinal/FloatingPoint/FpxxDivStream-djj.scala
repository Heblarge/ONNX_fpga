package FloatingPoint

import spinal.core._
import spinal.lib._

/**
 * FpxxDivStreams: 带 Stream 接口的浮点除法器（支持反压）
 *
 * 与 FpxxDiv 使用相同的 LUT + 两次乘法 的近似除法逻辑，
 * 但使用 Stream 接口实现流水线，支持下游反压。
 *
 * 支持奇数和偶数 mantissa：
 * 偶数时内部以 mant_size + 1 的精度计算，最后舍入到 mant_size 位输出。
 */
class FpxxDivStreams(c: FpxxConfig, divConfig: FpxxDivConfig = null) extends Component {

    assert(c.ieee_like, "Can only handle IEEE compliant floats")

    val isEvenMant = (c.mant_size & 1) == 0
    def effectiveMantSize = if (isEvenMant) c.mant_size + 1 else c.mant_size

    def pipeStages    = if (divConfig == null) 0 else divConfig.pipeStages
    def halfBits      = (effectiveMantSize + 1) / 2
    def lutMantBits   = if (divConfig == null || divConfig.lutMantBits < 0) 2 * halfBits + 2 else divConfig.lutMantBits
    def tableSizeBits = if (divConfig == null || divConfig.tableSizeBits < 0) halfBits else divConfig.tableSizeBits
    def tableSize     = 1 << tableSizeBits

    def divTableContents = for (i <- 0 until tableSize) yield {
        import _root_.FloatingPoint.FpxxHost._

        val fin  = 1.0 + i.toDouble / tableSize
        val fout = 1.0 / (fin * fin)

        val round     = (fout.mant >> (fout.c.mant_size - lutMantBits + 1)) & 1
        val fout_mant = (fout.mant >> (fout.c.mant_size - lutMantBits)) + round

        U(fout_mant, lutMantBits bits)
    }

    val div_table = Mem(UInt(lutMantBits bits), initialContent = divTableContents)

    val io = new Bundle {
        val input = slave Stream (new Bundle {
            val a = Fpxx(c)
            val b = Fpxx(c)
        })
        val output = master Stream (Fpxx(c))
    }

    // ============================================================
    // Stage payload bundles
    // ============================================================

    case class P0Bundle() extends Bundle {
        val yh_m_yl   = UInt(2 * halfBits bits)
        val div_addr  = UInt(tableSizeBits bits)
        val mant_a    = UInt(effectiveMantSize bits)
        val exp       = SInt((c.exp_size + 1) bits)
        val sign      = Bool()
        val op_a_zero = Bool()
        val op_b_zero = Bool()
        val op_nan    = Bool()
        val recip_exp = UInt(2 bits)
    }

    case class P1Bundle() extends Bundle {
        val yh_m_yl   = UInt(2 * halfBits bits)
        val mant_a    = UInt(effectiveMantSize bits)
        val sign      = Bool()
        val recip_yh2 = UInt((lutMantBits + 1) bits)
        val exp_full  = SInt((c.exp_size + 2) bits)
        val op_a_zero = Bool()
        val op_b_zero = Bool()
        val op_nan    = Bool()
    }

    case class P2Bundle() extends Bundle {
        val sign       = Bool()
        val x_mul_yhyl = UInt((2 * halfBits + 3) bits)
        val recip_yh2  = UInt((lutMantBits + 1) bits)
        val exp_full   = SInt((c.exp_size + 2) bits)
        val op_a_zero  = Bool()
        val op_b_zero  = Bool()
        val op_nan     = Bool()
    }

    case class P3Bundle() extends Bundle {
        val sign       = Bool()
        val x_mul_yhyl = UInt((2 * halfBits + 3) bits)
        val recip_yh2  = UInt((lutMantBits + 1) bits)
        val exp_full   = SInt((c.exp_size + 2) bits)
        val op_a_zero  = Bool()
        val op_b_zero  = Bool()
        val op_nan     = Bool()
    }

    case class P4Bundle() extends Bundle {
        val sign      = Bool()
        val div       = UInt((2 * halfBits + 3) bits)
        val exp_full  = SInt((c.exp_size + 2) bits)
        val op_a_zero = Bool()
        val op_b_zero = Bool()
        val op_nan    = Bool()
    }

    case class P5Bundle() extends Bundle {
        val sign      = Bool()
        val div_adj   = UInt(effectiveMantSize bits)
        val exp_adj   = SInt((c.exp_size + 2) bits)
        val op_a_zero = Bool()
        val op_b_zero = Bool()
        val op_nan    = Bool()
    }

    // ============================================================
    // Optional pipeline helper
    // ============================================================

    def optPipe[T <: Data](stream: Stream[T], enable: Boolean): Stream[T] = {
        if (enable) stream.m2sPipe() else stream
    }

    // ============================================================
    // P0: input decode / preprocessing
    // ============================================================

    val op_a_p0 = io.input.payload.a
    val op_b_p0 = io.input.payload.b

    val op_a_mant_eff = if (isEvenMant) (op_a_p0.mant @@ U(0, 1 bits)) else op_a_p0.mant
    val op_b_mant_eff = if (isEvenMant) (op_b_p0.mant @@ U(0, 1 bits)) else op_b_p0.mant

    val yh_p0       = (U(1, 1 bits) @@ op_b_mant_eff)(halfBits - 1, halfBits + 1 bits) << (halfBits - 1)
    val yl_p0       = op_b_mant_eff(0, halfBits - 1 bits).resize(2 * halfBits)
    val yh_m_yl_p0  = yh_p0 - yl_p0
    val div_addr_p0 = op_b_mant_eff >> (effectiveMantSize - tableSizeBits)

    val exp_p0  = op_a_p0.exp.resize(c.exp_size + 1).asSInt - op_b_p0.exp.resize(c.exp_size + 1).asSInt
    val sign_p0 = op_a_p0.sign ^ op_b_p0.sign

    val op_a_zero_p0 = op_a_p0.is_zero() || op_a_p0.is_subnormal()
    val op_b_zero_p0 = op_b_p0.is_zero() || op_b_p0.is_subnormal()
    val op_a_inf_p0  = op_a_p0.is_infinite()
    val op_b_inf_p0  = op_b_p0.is_infinite()
    val op_nan_p0    = op_a_p0.is_nan() || op_b_p0.is_nan() || (op_a_inf_p0 && op_b_inf_p0)

    val expBoundary = U(((scala.math.sqrt(2.0) - 1.0) * tableSize + 1).toInt, tableSizeBits bits)
    val recip_exp_p0 = (div_addr_p0 === 0) ? U(0, 2 bits) |
        ((div_addr_p0 < expBoundary) ? U(1, 2 bits) | U(2, 2 bits))

    val p0_stream = io.input.translateWith {
        val bundle = P0Bundle()
        bundle.yh_m_yl   := yh_m_yl_p0
        bundle.div_addr  := div_addr_p0
        bundle.mant_a    := op_a_mant_eff
        bundle.exp       := exp_p0
        bundle.sign      := sign_p0
        bundle.op_a_zero := op_a_zero_p0
        bundle.op_b_zero := op_b_zero_p0
        bundle.op_nan    := op_nan_p0
        bundle.recip_exp := recip_exp_p0
        bundle
    }

    // ============================================================
    // P0 -> P1: ROM stage (forced pipeline)
    // ============================================================

    val p0_piped = p0_stream.m2sPipe()

    val div_val_p1 = div_table.readSync(p0_stream.payload.div_addr, p0_stream.fire)

    val p1_stream = p0_piped.translateWith {
        val bundle = P1Bundle()
        val recip_yh2 = U(1, 1 bits) @@ div_val_p1
        val exp_full  = p0_piped.payload.exp.resize(c.exp_size + 2) -
                        p0_piped.payload.recip_exp.resize(c.exp_size + 2).asSInt +
                        S(c.bias + 1, c.exp_size + 2 bits)

        bundle.yh_m_yl   := p0_piped.payload.yh_m_yl
        bundle.mant_a    := p0_piped.payload.mant_a
        bundle.sign      := p0_piped.payload.sign
        bundle.recip_yh2 := recip_yh2
        bundle.exp_full  := exp_full
        bundle.op_a_zero := p0_piped.payload.op_a_zero
        bundle.op_b_zero := p0_piped.payload.op_b_zero
        bundle.op_nan    := p0_piped.payload.op_nan
        bundle
    }

    // ============================================================
    // P1 -> P2: first multiply
    // ============================================================

    val p1_piped = optPipe(p1_stream, pipeStages >= 1)

    val mant_a_full_p2     = U(1, 1 bits) @@ p1_piped.payload.mant_a
    val x_mul_yhyl_full_p2 = mant_a_full_p2 * p1_piped.payload.yh_m_yl
    val xMulYhYlShift      = x_mul_yhyl_full_p2.getWidth - (2 * halfBits + 3)
    val x_mul_yhyl_p2      = x_mul_yhyl_full_p2(xMulYhYlShift, (2 * halfBits + 3) bits)

    val p2_stream = p1_piped.translateWith {
        val bundle = P2Bundle()
        bundle.sign       := p1_piped.payload.sign
        bundle.x_mul_yhyl := x_mul_yhyl_p2
        bundle.recip_yh2  := p1_piped.payload.recip_yh2
        bundle.exp_full   := p1_piped.payload.exp_full
        bundle.op_a_zero  := p1_piped.payload.op_a_zero
        bundle.op_b_zero  := p1_piped.payload.op_b_zero
        bundle.op_nan     := p1_piped.payload.op_nan
        bundle
    }

    // ============================================================
    // P2 -> P3: optional route stage
    // ============================================================

    val p2_piped = optPipe(p2_stream, pipeStages >= 1)

    val p3_stream = p2_piped.translateWith {
        val bundle = P3Bundle()
        bundle.sign       := p2_piped.payload.sign
        bundle.x_mul_yhyl := p2_piped.payload.x_mul_yhyl
        bundle.recip_yh2  := p2_piped.payload.recip_yh2
        bundle.exp_full   := p2_piped.payload.exp_full
        bundle.op_a_zero  := p2_piped.payload.op_a_zero
        bundle.op_b_zero  := p2_piped.payload.op_b_zero
        bundle.op_nan     := p2_piped.payload.op_nan
        bundle
    }

    // ============================================================
    // P3 -> P4: second multiply
    // ============================================================

    val p3_piped = optPipe(p3_stream, pipeStages >= 2)

    val div_full_p4 = p3_piped.payload.x_mul_yhyl * p3_piped.payload.recip_yh2
    val divShift    = div_full_p4.getWidth - (2 * halfBits + 3)
    val div_p4      = div_full_p4(divShift, 2 * halfBits + 3 bits)

    val p4_stream = p3_piped.translateWith {
        val bundle = P4Bundle()
        bundle.sign      := p3_piped.payload.sign
        bundle.div       := div_p4
        bundle.exp_full  := p3_piped.payload.exp_full
        bundle.op_a_zero := p3_piped.payload.op_a_zero
        bundle.op_b_zero := p3_piped.payload.op_b_zero
        bundle.op_nan    := p3_piped.payload.op_nan
        bundle
    }

    // ============================================================
    // P4 -> P5: normalize shift decision
    // ============================================================

    val p4_piped = optPipe(p4_stream, pipeStages >= 2)

    val div_p5       = p4_piped.payload.div
    val shift_adj_p5 = UInt(2 bits)
    val exp_delta_p5 = SInt(3 bits)

    when(div_p5(div_p5.getWidth - 1, 1 bits) === U"1") {
        shift_adj_p5 := 3
        exp_delta_p5 := 1
    }.elsewhen(div_p5(div_p5.getWidth - 2, 2 bits) === U"01") {
        shift_adj_p5 := 2
        exp_delta_p5 := 0
    }.elsewhen(div_p5(div_p5.getWidth - 3, 3 bits) === U"001") {
        shift_adj_p5 := 1
        exp_delta_p5 := -1
    }.otherwise {
        shift_adj_p5 := 0
        exp_delta_p5 := -2
    }

    val div_adj_p5 = (div_p5 >> shift_adj_p5).resize(effectiveMantSize)
    val exp_adj_p5 = p4_piped.payload.exp_full + exp_delta_p5

    val p5_stream = p4_piped.translateWith {
        val bundle = P5Bundle()
        bundle.sign      := p4_piped.payload.sign
        bundle.div_adj   := div_adj_p5
        bundle.exp_adj   := exp_adj_p5
        bundle.op_a_zero := p4_piped.payload.op_a_zero
        bundle.op_b_zero := p4_piped.payload.op_b_zero
        bundle.op_nan    := p4_piped.payload.op_nan
        bundle
    }

    // ============================================================
    // P5 -> P6: optional output pipeline
    // ============================================================

    val p5_piped = optPipe(p5_stream, pipeStages >= 3)

    // Even mantissa rounding: effectiveMantSize -> c.mant_size
    val div_rounded_p5 = UInt(c.mant_size bits)
    val exp_rounded_p5 = SInt((c.exp_size + 2) bits)

    if (isEvenMant) {
        val extraBits = effectiveMantSize - c.mant_size
        val truncated = p5_piped.payload.div_adj(extraBits, c.mant_size bits)
        val roundBit  = p5_piped.payload.div_adj(extraBits - 1)
        val sum       = truncated +^ (roundBit ? U(1, 1 bits) | U(0, 1 bits))
        val overflow  = sum(c.mant_size)

        when(overflow) {
            div_rounded_p5 := U(0, c.mant_size bits)
            exp_rounded_p5 := p5_piped.payload.exp_adj + 1
        } otherwise {
            div_rounded_p5 := sum.resize(c.mant_size)
            exp_rounded_p5 := p5_piped.payload.exp_adj
        }
    } else {
        div_rounded_p5 := p5_piped.payload.div_adj
        exp_rounded_p5 := p5_piped.payload.exp_adj
    }

    // ============================================================
    // Final pack / special cases
    // ============================================================

    val sign_final = Bool()
    val exp_final  = UInt(c.exp_size bits)
    val div_final  = UInt(c.mant_size bits)

    when((p5_piped.payload.op_a_zero && p5_piped.payload.op_b_zero) || p5_piped.payload.op_nan) {
        sign_final := p5_piped.payload.sign
        exp_final.setAll
        div_final := (c.mant_size - 1 -> True, default -> False)
    }.elsewhen(exp_rounded_p5 >= ((1 << c.exp_size) - 1) || p5_piped.payload.op_b_zero) {
        sign_final := p5_piped.payload.sign
        exp_final.setAll
        div_final.clearAll
    }.elsewhen(exp_rounded_p5 <= 0) {
        sign_final := p5_piped.payload.sign
        exp_final.clearAll
        div_final.clearAll
    }.otherwise {
        sign_final := p5_piped.payload.sign
        exp_final  := exp_rounded_p5(0, c.exp_size bits).asUInt
        div_final  := div_rounded_p5
    }

    io.output << p5_piped.translateWith {
        val result = Fpxx(c)
        result.sign := sign_final
        result.exp  := exp_final
        result.mant := div_final
        result
    }
}

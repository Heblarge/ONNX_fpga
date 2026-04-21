package FloatingPoint

import spinal.core._
import spinal.lib._

/**
 * FpxxDivEvenStreams: 带 Stream 接口的浮点除法器（支持偶数mantissa）
 *
 * 与 FpxxDivEven 使用相同的 Goldschmidt 风格除法算法，
 * 但使用 Stream 接口实现流水线，支持下游反压。
 *
 * 流水线使用 m2sPipe() 实现，当 ready = 0 时自动暂停。
 *
 * @param c FpxxConfig 浮点格式配置
 * @param divConfig FpxxDivEvenConfig 除法器配置
 */
class FpxxDivEvenStreams(c: FpxxConfig, divConfig: FpxxDivEvenConfig = FpxxDivEvenConfig()) extends Component {

    def halfBits      = (c.mant_size + 2) / 2
    def pipeStages    = divConfig.pipeStages
    def lutMantBits   = if (divConfig.lutMantBits < 0) 2 * halfBits + 2 else divConfig.lutMantBits
    def tableSizeBits = if (divConfig.tableSizeBits < 0) halfBits else divConfig.tableSizeBits
    def tableSize     = 1 << tableSizeBits

    // 查找表：存储 1/y^2 的近似值
    def divTableContents = for (i <- 0 until tableSize) yield {
        import FloatingPoint.FpxxHost._
        val fin  = 1.0 + i.toDouble / tableSize
        val fout = 1.0 / (fin * fin)
        val round     = (fout.mant >> (fout.c.mant_size - lutMantBits + 1)) & 1
        val fout_mant = (fout.mant >> (fout.c.mant_size - lutMantBits)) + round
        U(fout_mant, lutMantBits bits)
    }

    val div_table = Mem(UInt(lutMantBits bits), initialContent = divTableContents)

    // ==================== Stream 接口 ====================
    val io = new Bundle {
        val input = slave Stream (new Bundle {
            val a = Fpxx(c)
            val b = Fpxx(c)
        })
        val result = master Stream (Fpxx(c))
    }

    // ==================== 流水线 Payload Bundle 定义 ====================
    case class P1Payload() extends Bundle {
        val yh_m_yl   = UInt((2 * halfBits) bits)
        val mant_a    = UInt(c.mant_size bits)
        val exp       = SInt((c.exp_size + 1) bits)
        val sign      = Bool()
        val op_a_zero = Bool()
        val op_b_zero = Bool()
        val op_nan    = Bool()
        val recip_exp = UInt(2 bits)
    }

    case class P2Payload() extends Bundle {
        val yh_m_yl   = UInt((2 * halfBits) bits)
        val mant_a    = UInt(c.mant_size bits)
        val sign      = Bool()
        val recip_yh2 = UInt((lutMantBits + 1) bits)
        val exp_full  = SInt((c.exp_size + 2) bits)
        val op_a_zero = Bool()
        val op_b_zero = Bool()
        val op_nan    = Bool()
    }

    case class P3Payload() extends Bundle {
        val sign       = Bool()
        val x_mul_yhyl = UInt((2 * halfBits + 3) bits)
        val recip_yh2  = UInt((lutMantBits + 1) bits)
        val exp_full   = SInt((c.exp_size + 2) bits)
        val op_a_zero  = Bool()
        val op_b_zero  = Bool()
        val op_nan     = Bool()
    }

    case class P4Payload() extends Bundle {
        val sign      = Bool()
        val div       = UInt((2 * halfBits + 3) bits)
        val exp_full  = SInt((c.exp_size + 2) bits)
        val op_a_zero = Bool()
        val op_b_zero = Bool()
        val op_nan    = Bool()
    }

    case class P5Payload() extends Bundle {
        val sign      = Bool()
        val div_adj   = UInt(c.mant_size bits)
        val exp_adj   = SInt((c.exp_size + 2) bits)
        val op_a_zero = Bool()
        val op_b_zero = Bool()
        val op_nan    = Bool()
    }

    // ==================== 条件流水线工具 ====================
    def optPipe[T <: Data](stream: Stream[T], enable: Boolean): Stream[T] = {
        if (enable) stream.m2sPipe() else stream
    }

    // ==================== P0 -> P1: 输入处理 ====================
    val op_a_p0 = io.input.payload.a
    val op_b_p0 = io.input.payload.b

    val y_full_p0 = U(1, 1 bits) @@ op_b_p0.mant
    val yhStartBit = c.mant_size + 1 - halfBits
    val yh_p0 = y_full_p0(yhStartBit, halfBits bits) << (halfBits - 1)
    val ylBits = c.mant_size + 1 - halfBits
    val yl_p0 = y_full_p0(0, ylBits bits).resize(2 * halfBits)

    val yh_m_yl_p0  = yh_p0 - yl_p0
    val div_addr_p0 = op_b_p0.mant >> (c.mant_size - tableSizeBits)

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

    val p0_to_p1_payload = P1Payload()
    p0_to_p1_payload.yh_m_yl   := yh_m_yl_p0
    p0_to_p1_payload.mant_a    := op_a_p0.mant
    p0_to_p1_payload.exp       := exp_p0
    p0_to_p1_payload.sign      := sign_p0
    p0_to_p1_payload.op_a_zero := op_a_zero_p0
    p0_to_p1_payload.op_b_zero := op_b_zero_p0
    p0_to_p1_payload.op_nan    := op_nan_p0
    p0_to_p1_payload.recip_exp := recip_exp_p0

    val p0_to_p1_stream = io.input.translateWith(p0_to_p1_payload)
    val p1_stream = optPipe(p0_to_p1_stream, true)  // ROM 需要一个周期

    // ==================== P1 -> P2: 查表 ====================
    val div_val_p1 = div_table.readSync(div_addr_p0.resize(tableSizeBits), io.input.fire)

    val recip_yh2_p1 = U(1, 1 bits) @@ div_val_p1
    val exp_full_p1  = p1_stream.payload.exp.resize(c.exp_size + 2) - 
                       p1_stream.payload.recip_exp.resize(c.exp_size + 2).asSInt + 
                       S(c.bias + 1, c.exp_size + 2 bits)

    val p1_to_p2_payload = P2Payload()
    p1_to_p2_payload.yh_m_yl   := p1_stream.payload.yh_m_yl
    p1_to_p2_payload.mant_a    := p1_stream.payload.mant_a
    p1_to_p2_payload.sign      := p1_stream.payload.sign
    p1_to_p2_payload.recip_yh2 := recip_yh2_p1
    p1_to_p2_payload.exp_full  := exp_full_p1
    p1_to_p2_payload.op_a_zero := p1_stream.payload.op_a_zero
    p1_to_p2_payload.op_b_zero := p1_stream.payload.op_b_zero
    p1_to_p2_payload.op_nan    := p1_stream.payload.op_nan

    val p1_to_p2_stream = p1_stream.translateWith(p1_to_p2_payload)
    val p2_stream = optPipe(p1_to_p2_stream, pipeStages >= 1)

    // ==================== P2 -> P3: 第一次乘法 ====================
    val mant_a_full_p2     = U(1, 1 bits) @@ p2_stream.payload.mant_a
    val x_mul_yhyl_full_p2 = mant_a_full_p2 * p2_stream.payload.yh_m_yl
    val xMulYhYlShift      = x_mul_yhyl_full_p2.getWidth - (2 * halfBits + 3)
    val x_mul_yhyl_p2      = x_mul_yhyl_full_p2(xMulYhYlShift, (2 * halfBits + 3) bits)

    val p2_to_p3_payload = P3Payload()
    p2_to_p3_payload.sign       := p2_stream.payload.sign
    p2_to_p3_payload.x_mul_yhyl := x_mul_yhyl_p2
    p2_to_p3_payload.recip_yh2  := p2_stream.payload.recip_yh2
    p2_to_p3_payload.exp_full   := p2_stream.payload.exp_full
    p2_to_p3_payload.op_a_zero  := p2_stream.payload.op_a_zero
    p2_to_p3_payload.op_b_zero  := p2_stream.payload.op_b_zero
    p2_to_p3_payload.op_nan     := p2_stream.payload.op_nan

    val p2_to_p3_stream = p2_stream.translateWith(p2_to_p3_payload)
    val p3_stream = optPipe(p2_to_p3_stream, pipeStages >= 1)

    // ==================== P3 -> P4: 第二次乘法 ====================
    val div_full_p3 = p3_stream.payload.x_mul_yhyl * p3_stream.payload.recip_yh2
    val divShift    = div_full_p3.getWidth - (2 * halfBits + 3)
    val div_p3      = div_full_p3(divShift, (2 * halfBits + 3) bits)

    val p3_to_p4_payload = P4Payload()
    p3_to_p4_payload.sign      := p3_stream.payload.sign
    p3_to_p4_payload.div       := div_p3
    p3_to_p4_payload.exp_full  := p3_stream.payload.exp_full
    p3_to_p4_payload.op_a_zero := p3_stream.payload.op_a_zero
    p3_to_p4_payload.op_b_zero := p3_stream.payload.op_b_zero
    p3_to_p4_payload.op_nan    := p3_stream.payload.op_nan

    val p3_to_p4_stream = p3_stream.translateWith(p3_to_p4_payload)
    val p4_stream = optPipe(p3_to_p4_stream, pipeStages >= 2)

    // ==================== P4 -> P5: 规范化 ====================
    val div_p4 = p4_stream.payload.div

    val shift_adj_p4 = UInt(2 bits)
    val exp_delta_p4 = SInt(3 bits)

    when(div_p4(div_p4.getWidth - 1)) {
        shift_adj_p4 := 3
        exp_delta_p4 := 1
    } elsewhen(div_p4(div_p4.getWidth - 2)) {
        shift_adj_p4 := 2
        exp_delta_p4 := 0
    } elsewhen(div_p4(div_p4.getWidth - 3)) {
        shift_adj_p4 := 1
        exp_delta_p4 := -1
    } otherwise {
        shift_adj_p4 := 0
        exp_delta_p4 := -2
    }

    val div_shifted_p4 = div_p4 >> shift_adj_p4
    val div_adj_p4 = div_shifted_p4.resize(c.mant_size)
    val exp_adj_p4 = p4_stream.payload.exp_full + exp_delta_p4

    val p4_to_p5_payload = P5Payload()
    p4_to_p5_payload.sign      := p4_stream.payload.sign
    p4_to_p5_payload.div_adj   := div_adj_p4
    p4_to_p5_payload.exp_adj   := exp_adj_p4
    p4_to_p5_payload.op_a_zero := p4_stream.payload.op_a_zero
    p4_to_p5_payload.op_b_zero := p4_stream.payload.op_b_zero
    p4_to_p5_payload.op_nan    := p4_stream.payload.op_nan

    val p4_to_p5_stream = p4_stream.translateWith(p4_to_p5_payload)
    val p5_stream = optPipe(p4_to_p5_stream, pipeStages >= 3)

    // ==================== P5 -> Output: 最终输出 ====================
    val result_payload = Fpxx(c)
    result_payload.sign := p5_stream.payload.sign
    result_payload.exp := p5_stream.payload.exp_adj(0, c.exp_size bits).asUInt
    result_payload.mant := p5_stream.payload.div_adj

    when((p5_stream.payload.op_a_zero && p5_stream.payload.op_b_zero) || p5_stream.payload.op_nan) {
        result_payload.sign := p5_stream.payload.sign
        result_payload.set_nan()
    } elsewhen(p5_stream.payload.exp_adj >= ((1 << c.exp_size) - 1) || p5_stream.payload.op_b_zero) {
        result_payload.sign := p5_stream.payload.sign
        result_payload.set_inf()
    } elsewhen(p5_stream.payload.exp_adj <= 0 || p5_stream.payload.op_a_zero) {
        result_payload.sign := p5_stream.payload.sign
        result_payload.set_zero()
    }

    io.result << p5_stream.translateWith(result_payload)
}

// Companion object for generation
object FpxxDivEvenStreamsMain extends App {
    import spinal.core.sim._

    val float16Config = FpxxConfig.float16()
    val divConfig = FpxxDivEvenConfig(pipeStages = 2)

    SpinalConfig(
        defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC),
        targetDirectory = "hw/rtl/math"
    ).generateVerilog(new FpxxDivEvenStreams(float16Config, divConfig))
}

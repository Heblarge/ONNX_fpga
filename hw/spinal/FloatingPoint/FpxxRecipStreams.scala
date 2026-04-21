package FloatingPoint

import spinal.core._
import spinal.lib._

/**
 * FpxxRecipStreams: 带 Stream 接口的浮点倒数计算器 (1/x)
 *
 * 使用查找表直接提供倒数近似值，支持任意 mantissa 大小（包括偶数）。
 * 带有 Stream 接口，支持下游反压（backpressure）。
 *
 * 流水线使用 m2sPipe() 实现，当 ready = 0 时自动暂停。
 *
 * 对于 float16 (mant=10)，使用 10-bit 查找表地址可提供完整精度。
 * 查找表大小: 2^10 * 11 bits = 11Kbits = 1.375KB
 *
 * @param c FpxxConfig 浮点格式配置
 * @param recipConfig FpxxRecipConfig 倒数计算器配置
 */
class FpxxRecipStreams(c: FpxxConfig, recipConfig: FpxxRecipConfig = FpxxRecipConfig()) extends Component {

    assert(c.ieee_like, "Can only handle IEEE compliant floats")

    // 配置参数
    def pipeStages    = recipConfig.pipeStages
    def tableSizeBits = if (recipConfig.tableSizeBits < 0) c.mant_size else recipConfig.tableSizeBits
    def tableSize     = 1 << tableSizeBits

    // ==================== 查找表生成 ====================
    def recipTableContents = for (i <- 0 until tableSize) yield {
        val y_in = 1.0 + i.toDouble / tableSize.toDouble
        val recip = 1.0 / y_in
        
        val (normalized, exp_adj) = if (recip >= 1.0) {
            (recip / 2.0, 1)
        } else {
            (recip * 2.0, 0)
        }
        
        val mant_val = ((normalized - 1.0) * (1L << c.mant_size).toDouble).toLong.max(0L)
        val packed = (exp_adj.toLong << c.mant_size) | mant_val
        U(packed, (c.mant_size + 1) bits)
    }

    val recip_table = Mem(UInt((c.mant_size + 1) bits), initialContent = recipTableContents)

    // ==================== Stream 接口 ====================
    val io = new Bundle {
        val input  = slave Stream (Fpxx(c))
        val result = master Stream (Fpxx(c))
    }

    // ==================== 流水线 Payload Bundle 定义 ====================
    case class P1Payload() extends Bundle {
        val sign     = Bool()
        val exp      = UInt(c.exp_size bits)
        val op_zero  = Bool()
        val op_inf   = Bool()
        val op_nan   = Bool()
    }

    case class P2Payload() extends Bundle {
        val sign      = Bool()
        val exp       = UInt(c.exp_size bits)
        val op_zero   = Bool()
        val op_inf    = Bool()
        val op_nan    = Bool()
        val table_val = UInt((c.mant_size + 1) bits)
    }

    case class P3Payload() extends Bundle {
        val sign       = Bool()
        val op_zero    = Bool()
        val op_inf     = Bool()
        val op_nan     = Bool()
        val mant       = UInt(c.mant_size bits)
        val exp_result = SInt((c.exp_size + 2) bits)
    }

    // ==================== 条件流水线工具 ====================
    def optPipe[T <: Data](stream: Stream[T], enable: Boolean): Stream[T] = {
        if (enable) stream.m2sPipe() else stream
    }

    // ==================== P0 -> P1: 输入处理 ====================
    val op_p0 = io.input.payload

    val op_zero_p0 = op_p0.is_zero() || op_p0.is_subnormal()
    val op_inf_p0  = op_p0.is_infinite()
    val op_nan_p0  = op_p0.is_nan()

    val table_addr_p0 = if (tableSizeBits <= c.mant_size) {
        op_p0.mant >> (c.mant_size - tableSizeBits)
    } else {
        op_p0.mant << (tableSizeBits - c.mant_size)
    }

    // P0 -> P1 Stream
    val p0_to_p1_payload = P1Payload()
    p0_to_p1_payload.sign    := op_p0.sign
    p0_to_p1_payload.exp     := op_p0.exp
    p0_to_p1_payload.op_zero := op_zero_p0
    p0_to_p1_payload.op_inf  := op_inf_p0
    p0_to_p1_payload.op_nan  := op_nan_p0

    val p0_to_p1_stream = io.input.translateWith(p0_to_p1_payload)
    val p1_stream = optPipe(p0_to_p1_stream, pipeStages >= 1)

    // ==================== P1 -> P2: 查表 ====================
    // 同步读取查找表
    val table_addr_reg = RegNextWhen(table_addr_p0.resize(tableSizeBits), io.input.fire)
    val table_val_p1 = recip_table.readSync(table_addr_p0.resize(tableSizeBits), io.input.fire)

    val p1_to_p2_payload = P2Payload()
    p1_to_p2_payload.sign      := p1_stream.payload.sign
    p1_to_p2_payload.exp       := p1_stream.payload.exp
    p1_to_p2_payload.op_zero   := p1_stream.payload.op_zero
    p1_to_p2_payload.op_inf    := p1_stream.payload.op_inf
    p1_to_p2_payload.op_nan    := p1_stream.payload.op_nan
    p1_to_p2_payload.table_val := table_val_p1

    val p1_to_p2_stream = p1_stream.translateWith(p1_to_p2_payload)
    val p2_stream = optPipe(p1_to_p2_stream, pipeStages >= 2)

    // ==================== P2 -> P3: 计算指数和提取尾数 ====================
    val exp_adj_p2  = p2_stream.payload.table_val(c.mant_size)
    val mant_lut_p2 = p2_stream.payload.table_val(0, c.mant_size bits)

    val exp_in_p2 = p2_stream.payload.exp.resize(c.exp_size + 2).asSInt
    val exp_result_p2 = S(2 * c.bias - 1, c.exp_size + 2 bits) - exp_in_p2 + exp_adj_p2.asUInt.resize(c.exp_size + 2).asSInt

    val p2_to_p3_payload = P3Payload()
    p2_to_p3_payload.sign       := p2_stream.payload.sign
    p2_to_p3_payload.op_zero    := p2_stream.payload.op_zero
    p2_to_p3_payload.op_inf     := p2_stream.payload.op_inf
    p2_to_p3_payload.op_nan     := p2_stream.payload.op_nan
    p2_to_p3_payload.mant       := mant_lut_p2
    p2_to_p3_payload.exp_result := exp_result_p2

    val p2_to_p3_stream = p2_stream.translateWith(p2_to_p3_payload)
    val p3_stream = optPipe(p2_to_p3_stream, pipeStages >= 3)

    // ==================== P3 -> Output: 最终输出处理 ====================
    val result_payload = Fpxx(c)

    when(p3_stream.payload.op_nan) {
        result_payload.sign := p3_stream.payload.sign
        result_payload.exp.setAll()
        result_payload.mant := (c.mant_size - 1 -> True, default -> False)
    } elsewhen(p3_stream.payload.op_zero) {
        result_payload.sign := p3_stream.payload.sign
        result_payload.exp.setAll()
        result_payload.mant.clearAll()
    } elsewhen(p3_stream.payload.op_inf) {
        result_payload.sign := p3_stream.payload.sign
        result_payload.exp.clearAll()
        result_payload.mant.clearAll()
    } elsewhen(p3_stream.payload.exp_result >= ((1 << c.exp_size) - 1)) {
        result_payload.sign := p3_stream.payload.sign
        result_payload.exp.setAll()
        result_payload.mant.clearAll()
    } elsewhen(p3_stream.payload.exp_result <= 0) {
        result_payload.sign := p3_stream.payload.sign
        result_payload.exp.clearAll()
        result_payload.mant.clearAll()
    } otherwise {
        result_payload.sign := p3_stream.payload.sign
        result_payload.exp  := p3_stream.payload.exp_result(0, c.exp_size bits).asUInt
        result_payload.mant := p3_stream.payload.mant
    }

    io.result << p3_stream.translateWith(result_payload)
}

// Companion object for generation
object FpxxRecipStreamsMain extends App {
    import spinal.core.sim._

    val float16Config = FpxxConfig.float16()
    val recipConfig = FpxxRecipConfig(pipeStages = 2)

    SpinalConfig(
        defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC),
        targetDirectory = "hw/rtl/math"
    ).generateVerilog(new FpxxRecipStreams(float16Config, recipConfig))
}

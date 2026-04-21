package FloatingPoint

import spinal.core._
import spinal.lib._

/**
 * FpxxRecipConfig: 倒数计算器配置
 *
 * @param pipeStages 流水线级数 (0-3)
 * @param tableSizeBits 查找表地址位数 (默认 = mant_size)
 */
case class FpxxRecipConfig(
    pipeStages    : Int = 2,
    tableSizeBits : Int = -1
)

/**
 * FpxxRecip: 浮点倒数计算器 (1/x)
 *
 * 使用查找表直接提供倒数近似值，支持任意 mantissa 大小（包括偶数）。
 * 精度由查找表大小决定。
 *
 * 对于 float16 (mant=10)，使用 10-bit 查找表地址可提供完整精度。
 *
 * 算法：
 * 1. 规范化输入到 [1, 2) 范围
 * 2. 使用 mantissa 高位作为查找表地址
 * 3. 查找表直接返回 1/x 的 mantissa
 * 4. 计算结果指数
 *
 * @param c FpxxConfig 浮点格式配置
 * @param recipConfig FpxxRecipConfig 倒数计算器配置
 */
class FpxxRecip(c: FpxxConfig, recipConfig: FpxxRecipConfig = FpxxRecipConfig()) extends Component {

    assert(c.ieee_like, "Can only handle IEEE compliant floats")

    // 配置参数
    def pipeStages    = recipConfig.pipeStages
    def tableSizeBits = if (recipConfig.tableSizeBits < 0) c.mant_size else recipConfig.tableSizeBits
    def tableSize     = 1 << tableSizeBits

    // ==================== 查找表生成 ====================
    // 表存储 1/(1+x) 的近似值，其中 x = i/tableSize，范围 [0, 1)
    // 输出为规范化浮点的 mantissa 部分
    def recipTableContents = for (i <- 0 until tableSize) yield {
        // 输入值 y = 1.0 + i/(tableSize)，范围 [1.0, 2.0)
        val y_in = 1.0 + i.toDouble / tableSize.toDouble

        // 计算 1/y，范围 (0.5, 1.0]
        val recip = 1.0 / y_in

        // 结果需要规范化：
        // - 当 y = 1.0 时，recip = 1.0，需要表示为 1.0 * 2^0
        // - 当 y > 1.0 时，recip < 1.0，需要表示为 1.xxx * 2^{-1}
        // 
        // 统一规范化到 [1.0, 2.0) 范围，记录额外的指数调整
        val (normalized, exp_adj) = if (recip >= 1.0) {
            (recip / 2.0, 1)  // 1.0 变成 0.5 * 2^1
        } else {
            (recip * 2.0, 0)  // (0.5, 1.0) 变成 (1.0, 2.0) * 2^{-1}
        }
        
        // normalized 现在在 [1.0, 2.0) 范围，提取 mantissa (去掉隐含的 1)
        val mant_val = ((normalized - 1.0) * (1L << c.mant_size).toDouble).toLong.max(0L)

        // 打包: [exp_adj(1bit) | mantissa(mant_size bits)]
        val packed = (exp_adj.toLong << c.mant_size) | mant_val
        U(packed, (c.mant_size + 1) bits)
    }

    val recip_table = Mem(UInt((c.mant_size + 1) bits), initialContent = recipTableContents)

    // ==================== IO 接口 ====================
    val io = new Bundle {
        val op_vld    = in Bool()
        val op        = in(Fpxx(c))

        val result_vld = out Bool()
        val result     = out(Fpxx(c))
    }

    // ==================== P0 阶段：输入预处理 ====================
    val p0_vld = io.op_vld
    val op_p0  = io.op

    // 特殊值检测
    val op_zero_p0 = op_p0.is_zero() || op_p0.is_subnormal()
    val op_inf_p0  = op_p0.is_infinite()
    val op_nan_p0  = op_p0.is_nan()

    // 符号直接传递（1/x 的符号与 x 相同）
    val sign_p0 = op_p0.sign

    // 保存指数用于后续计算
    val exp_p0 = op_p0.exp

    // 查找表地址：使用 mantissa 的高 tableSizeBits 位
    val table_addr_p0 = if (tableSizeBits <= c.mant_size) {
        op_p0.mant >> (c.mant_size - tableSizeBits)
    } else {
        op_p0.mant << (tableSizeBits - c.mant_size)
    }

    // ==================== P1 阶段：查表 ====================
    val p1_pipe_ena = pipeStages >= 1
    val p1_vld      = OptPipeInit(p0_vld, False, p1_pipe_ena)
    val sign_p1     = OptPipe(sign_p0, p0_vld, p1_pipe_ena)
    val exp_p1      = OptPipe(exp_p0, p0_vld, p1_pipe_ena)
    val op_zero_p1  = OptPipe(op_zero_p0, p0_vld, p1_pipe_ena)
    val op_inf_p1   = OptPipe(op_inf_p0, p0_vld, p1_pipe_ena)
    val op_nan_p1   = OptPipe(op_nan_p0, p0_vld, p1_pipe_ena)

    // 读取查找表（同步读取）
    val table_val_p1 = recip_table.readSync(table_addr_p0.resize(tableSizeBits), p0_vld)

    // ==================== P2 阶段：解析查表结果并计算指数 ====================
    val p2_pipe_ena = pipeStages >= 2
    val p2_vld      = OptPipeInit(p1_vld, False, p2_pipe_ena)
    val sign_p2     = OptPipe(sign_p1, p1_vld, p2_pipe_ena)
    val exp_p2      = OptPipe(exp_p1, p1_vld, p2_pipe_ena)
    val op_zero_p2  = OptPipe(op_zero_p1, p1_vld, p2_pipe_ena)
    val op_inf_p2   = OptPipe(op_inf_p1, p1_vld, p2_pipe_ena)
    val op_nan_p2   = OptPipe(op_nan_p1, p1_vld, p2_pipe_ena)
    val table_val_p2 = OptPipe(table_val_p1, p1_vld, p2_pipe_ena)

    // 解析查找表值
    val exp_adj_p2  = table_val_p2(c.mant_size)        // 指数调整标志 (1 bit)
    val mant_lut_p2 = table_val_p2(0, c.mant_size bits) // 查表得到的 mantissa

    // 计算结果指数
    // 输入: x = 1.mant * 2^(exp-bias)
    // 输出: 1/x = 1.mant' * 2^(exp'-bias)
    // 其中 exp' = 2*bias - exp - 1 + exp_adj
    //
    // exp_adj = 1 当输入 mant=0 时（即 x=1.0），1/x = 1.0 需要额外 +1
    // exp_adj = 0 其他情况
    val exp_in_p2 = exp_p2.resize(c.exp_size + 2).asSInt
    val exp_result_p2 = S(2 * c.bias - 1, c.exp_size + 2 bits) - exp_in_p2 + exp_adj_p2.asUInt.resize(c.exp_size + 2).asSInt

    // ==================== P3 阶段：最终输出 ====================
    val p3_pipe_ena = pipeStages >= 3
    val p3_vld      = OptPipeInit(p2_vld, False, p3_pipe_ena)
    val sign_p3     = OptPipe(sign_p2, p2_vld, p3_pipe_ena)
    val op_zero_p3  = OptPipe(op_zero_p2, p2_vld, p3_pipe_ena)
    val op_inf_p3   = OptPipe(op_inf_p2, p2_vld, p3_pipe_ena)
    val op_nan_p3   = OptPipe(op_nan_p2, p2_vld, p3_pipe_ena)
    val mant_p3     = OptPipe(mant_lut_p2, p2_vld, p3_pipe_ena)
    val exp_result_p3 = OptPipe(exp_result_p2, p2_vld, p3_pipe_ena)

    // ==================== 最终输出处理 ====================
    val sign_out = Bool()
    val exp_out  = UInt(c.exp_size bits)
    val mant_out = UInt(c.mant_size bits)

    when(op_nan_p3) {
        // NaN -> NaN
        sign_out := sign_p3
        exp_out.setAll()
        mant_out := (c.mant_size - 1 -> True, default -> False)
    } elsewhen(op_zero_p3) {
        // 1/0 -> Infinity
        sign_out := sign_p3
        exp_out.setAll()
        mant_out.clearAll()
    } elsewhen(op_inf_p3) {
        // 1/Inf -> 0
        sign_out := sign_p3
        exp_out.clearAll()
        mant_out.clearAll()
    } elsewhen(exp_result_p3 >= ((1 << c.exp_size) - 1)) {
        // 溢出 -> Infinity
        sign_out := sign_p3
        exp_out.setAll()
        mant_out.clearAll()
    } elsewhen(exp_result_p3 <= 0) {
        // 下溢 -> 0
        sign_out := sign_p3
        exp_out.clearAll()
        mant_out.clearAll()
    } otherwise {
        sign_out := sign_p3
        exp_out  := exp_result_p3(0, c.exp_size bits).asUInt
        mant_out := mant_p3
    }

    io.result_vld  := p3_vld
    io.result.sign := sign_out
    io.result.exp  := exp_out
    io.result.mant := mant_out
}

// Companion object for generation
object FpxxRecipMain extends App {
    import spinal.core.sim._

    val float16Config = FpxxConfig.float16()
    val recipConfig = FpxxRecipConfig(pipeStages = 2)

    SpinalConfig(
        defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC),
        targetDirectory = "hw/rtl/math"
    ).generateVerilog(new FpxxRecip(float16Config, recipConfig))
}

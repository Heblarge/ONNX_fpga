package FloatingPoint

import spinal.core._

/**
 * FpxxDivEvenConfig: 支持偶数mantissa的除法器配置
 *
 * @param pipeStages 流水线级数 (0-3)
 * @param tableSizeBits 查找表地址位数
 * @param lutMantBits 查找表输出位数
 */
case class FpxxDivEvenConfig(
    pipeStages    : Int = 2,
    tableSizeBits : Int = -1,
    lutMantBits   : Int = -1
)

/**
 * FpxxDivEven: 支持偶数mantissa的浮点除法器
 *
 * 修改自 FpxxDiv，移除了对奇数 mantissa 的要求。
 * 使用查找表 + 两次乘法实现除法，精度由 lutMantBits 决定。
 *
 * 算法（Goldschmidt 风格）：
 * 1. 查表获取 1/y^2 的近似值
 * 2. 计算 x * (yh - yl) 其中 y = yh + yl
 * 3. 计算 (x * (yh - yl)) * (1/y^2) ≈ x/y
 *
 * @param c FpxxConfig 浮点格式配置
 * @param divConfig FpxxDivEvenConfig 除法器配置
 */
class FpxxDivEven(c: FpxxConfig, divConfig: FpxxDivEvenConfig = FpxxDivEvenConfig()) extends Component {

    // 对于偶数 mant_size，使用 halfBits = mant_size/2 + 1
    // 对于奇数 mant_size，使用 halfBits = (mant_size+1)/2
    def halfBits        = (c.mant_size + 2) / 2  // 向上取整
    def pipeStages      = divConfig.pipeStages
    def lutMantBits     = if (divConfig.lutMantBits < 0) 2 * halfBits + 2 else divConfig.lutMantBits
    def tableSizeBits   = if (divConfig.tableSizeBits < 0) halfBits else divConfig.tableSizeBits
    def tableSize       = 1 << tableSizeBits

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

    val io = new Bundle {
        val op_vld  = in Bool()
        val op_a    = in(Fpxx(c))
        val op_b    = in(Fpxx(c))

        val result_vld = out Bool()
        val result     = out(Fpxx(c))
    }

    // ==================== P0 阶段：输入预处理 ====================
    val p0_vld  = io.op_vld
    val op_a_p0 = io.op_a
    val op_b_p0 = io.op_b

    // 分割 y = 1.mant 为 yh (高位) 和 yl (低位)
    // 完整的 y = 1.mant 有 (mant_size + 1) 位，索引从 0 到 mant_size
    // 对于 float16: y_full 是 11 位 [10:0]，其中 bit[10]=1 是隐含位
    // yh 取高 halfBits 位：从 bit[mant_size] 开始取 halfBits 位
    // yl 取低位：bit[mant_size - halfBits] 到 bit[0]
    val y_full_p0 = U(1, 1 bits) @@ op_b_p0.mant  // mant_size + 1 bits, 索引 [mant_size:0]
    
    // yh: 取最高的 halfBits 位，左移使其对齐到 2*halfBits 的高位
    val yhStartBit = c.mant_size + 1 - halfBits  // float16: 11-6=5, 即从 bit[10] 取到 bit[5]
    val yh_p0 = y_full_p0(yhStartBit, halfBits bits) << (halfBits - 1)
    
    // yl: 取低位，扩展到 2*halfBits 宽度
    val ylBits = c.mant_size + 1 - halfBits  // float16: 11-6=5
    val yl_p0 = y_full_p0(0, ylBits bits).resize(2 * halfBits)

    val yh_m_yl_p0  = yh_p0 - yl_p0
    val div_addr_p0 = op_b_p0.mant >> (c.mant_size - tableSizeBits)

    // 指数计算
    val exp_p0  = op_a_p0.exp.resize(c.exp_size + 1).asSInt - op_b_p0.exp.resize(c.exp_size + 1).asSInt
    val sign_p0 = op_a_p0.sign ^ op_b_p0.sign

    // 特殊值检测
    val op_a_zero_p0 = op_a_p0.is_zero() || op_a_p0.is_subnormal()
    val op_b_zero_p0 = op_b_p0.is_zero() || op_b_p0.is_subnormal()
    val op_a_inf_p0  = op_a_p0.is_infinite()
    val op_b_inf_p0  = op_b_p0.is_infinite()
    val op_nan_p0    = op_a_p0.is_nan() || op_b_p0.is_nan() || (op_a_inf_p0 && op_b_inf_p0)

    // 1/y^2 的指数调整边界
    val expBoundary = U(((scala.math.sqrt(2.0) - 1.0) * tableSize + 1).toInt, tableSizeBits bits)
    val recip_exp_p0 = (div_addr_p0 === 0) ? U(0, 2 bits) |
                         ((div_addr_p0 < expBoundary) ? U(1, 2 bits) | U(2, 2 bits))

    // ==================== P1 阶段：查表 ====================
    val p1_pipe_ena  = true  // ROM 同步读需要一个周期
    val p1_vld       = OptPipeInit(p0_vld, False, p1_pipe_ena)
    val yh_m_yl_p1   = OptPipe(yh_m_yl_p0, p0_vld, p1_pipe_ena)
    val mant_a_p1    = OptPipe(op_a_p0.mant, p0_vld, p1_pipe_ena)
    val exp_p1       = OptPipe(exp_p0, p0_vld, p1_pipe_ena)
    val sign_p1      = OptPipe(sign_p0, p0_vld, p1_pipe_ena)
    val op_a_zero_p1 = OptPipe(op_a_zero_p0, p0_vld, p1_pipe_ena)
    val op_b_zero_p1 = OptPipe(op_b_zero_p0, p0_vld, p1_pipe_ena)
    val op_nan_p1    = OptPipe(op_nan_p0, p0_vld, p1_pipe_ena)
    val recip_exp_p1 = OptPipe(recip_exp_p0, p0_vld, p1_pipe_ena)

    val div_val_p1 = div_table.readSync(div_addr_p0.resize(tableSizeBits), p0_vld)

    // ==================== P2 阶段：第一次乘法 ====================
    val recip_yh2_p1 = U(1, 1 bits) @@ div_val_p1
    val exp_full_p1  = exp_p1.resize(c.exp_size + 2) - recip_exp_p1.resize(c.exp_size + 2).asSInt + S(c.bias + 1, c.exp_size + 2 bits)

    val p2_pipe_ena  = pipeStages >= 1
    val p2_vld       = OptPipeInit(p1_vld, False, p2_pipe_ena)
    val yh_m_yl_p2   = OptPipe(yh_m_yl_p1, p1_vld, p2_pipe_ena)
    val mant_a_p2    = OptPipe(mant_a_p1, p1_vld, p2_pipe_ena)
    val sign_p2      = OptPipe(sign_p1, p1_vld, p2_pipe_ena)
    val recip_yh2_p2 = OptPipe(recip_yh2_p1, p1_vld, p2_pipe_ena)
    val exp_full_p2  = OptPipe(exp_full_p1, p1_vld, p2_pipe_ena)
    val op_a_zero_p2 = OptPipe(op_a_zero_p1, p1_vld, p2_pipe_ena)
    val op_b_zero_p2 = OptPipe(op_b_zero_p1, p1_vld, p2_pipe_ena)
    val op_nan_p2    = OptPipe(op_nan_p1, p1_vld, p2_pipe_ena)

    // x * (yh - yl) 乘法
    val mant_a_full_p2     = U(1, 1 bits) @@ mant_a_p2
    val x_mul_yhyl_full_p2 = mant_a_full_p2 * yh_m_yl_p2
    val xMulYhYlShift      = x_mul_yhyl_full_p2.getWidth - (2 * halfBits + 3)
    val x_mul_yhyl_p2      = x_mul_yhyl_full_p2(xMulYhYlShift, (2 * halfBits + 3) bits)

    // ==================== P3 阶段：可选流水线 ====================
    val p3_pipe_ena  = pipeStages >= 1
    val p3_vld       = OptPipeInit(p2_vld, False, p3_pipe_ena)
    val sign_p3      = OptPipe(sign_p2, p2_vld, p3_pipe_ena)
    val x_mul_yhyl_p3 = OptPipe(x_mul_yhyl_p2, p2_vld, p3_pipe_ena)
    val recip_yh2_p3 = OptPipe(recip_yh2_p2, p2_vld, p3_pipe_ena)
    val exp_full_p3  = OptPipe(exp_full_p2, p2_vld, p3_pipe_ena)
    val op_a_zero_p3 = OptPipe(op_a_zero_p2, p2_vld, p3_pipe_ena)
    val op_b_zero_p3 = OptPipe(op_b_zero_p2, p2_vld, p3_pipe_ena)
    val op_nan_p3    = OptPipe(op_nan_p2, p2_vld, p3_pipe_ena)

    // ==================== P4 阶段：第二次乘法 ====================
    val p4_pipe_ena  = pipeStages >= 2
    val p4_vld       = OptPipeInit(p3_vld, False, p4_pipe_ena)
    val sign_p4      = OptPipe(sign_p3, p3_vld, p4_pipe_ena)
    val x_mul_yhyl_p4 = OptPipe(x_mul_yhyl_p3, p3_vld, p4_pipe_ena)
    val recip_yh2_p4 = OptPipe(recip_yh2_p3, p3_vld, p4_pipe_ena)
    val exp_full_p4  = OptPipe(exp_full_p3, p3_vld, p4_pipe_ena)
    val op_a_zero_p4 = OptPipe(op_a_zero_p3, p3_vld, p4_pipe_ena)
    val op_b_zero_p4 = OptPipe(op_b_zero_p3, p3_vld, p4_pipe_ena)
    val op_nan_p4    = OptPipe(op_nan_p3, p3_vld, p4_pipe_ena)

    // (x * (yh - yl)) * (1/y^2) 乘法
    val div_full_p4 = x_mul_yhyl_p4 * recip_yh2_p4
    val divShift    = div_full_p4.getWidth - (2 * halfBits + 3)
    val div_p4      = div_full_p4(divShift, (2 * halfBits + 3) bits)

    // ==================== P5 阶段：规范化 ====================
    val p5_pipe_ena  = pipeStages >= 2
    val p5_vld       = OptPipeInit(p4_vld, False, p5_pipe_ena)
    val sign_p5      = OptPipe(sign_p4, p4_vld, p5_pipe_ena)
    val div_p5       = OptPipe(div_p4, p4_vld, p5_pipe_ena)
    val exp_full_p5  = OptPipe(exp_full_p4, p4_vld, p5_pipe_ena)
    val op_a_zero_p5 = OptPipe(op_a_zero_p4, p4_vld, p5_pipe_ena)
    val op_b_zero_p5 = OptPipe(op_b_zero_p4, p4_vld, p5_pipe_ena)
    val op_nan_p5    = OptPipe(op_nan_p4, p4_vld, p5_pipe_ena)

    // 规范化调整
    val div_adj_p5 = UInt(c.mant_size bits)
    val exp_adj_p5 = SInt((c.exp_size + 2) bits)

    val shift_adj_p5 = UInt(2 bits)
    val exp_delta_p5 = SInt(3 bits)

    when(div_p5(div_p5.getWidth - 1)) {
        shift_adj_p5 := 3
        exp_delta_p5 := 1
    } elsewhen(div_p5(div_p5.getWidth - 2)) {
        shift_adj_p5 := 2
        exp_delta_p5 := 0
    } elsewhen(div_p5(div_p5.getWidth - 3)) {
        shift_adj_p5 := 1
        exp_delta_p5 := -1
    } otherwise {
        shift_adj_p5 := 0
        exp_delta_p5 := -2
    }

    // 提取最终 mantissa（截取 mant_size 位）
    val div_shifted_p5 = div_p5 >> shift_adj_p5
    div_adj_p5 := div_shifted_p5.resize(c.mant_size)
    exp_adj_p5 := exp_full_p5 + exp_delta_p5

    // ==================== P6 阶段：最终输出 ====================
    val p6_pipe_ena  = pipeStages >= 3
    val p6_vld       = OptPipeInit(p5_vld, False, p6_pipe_ena)
    val sign_p6      = OptPipe(sign_p5, p5_vld, p6_pipe_ena)
    val div_adj_p6   = OptPipe(div_adj_p5, p5_vld, p6_pipe_ena)
    val exp_adj_p6   = OptPipe(exp_adj_p5, p5_vld, p6_pipe_ena)
    val op_a_zero_p6 = OptPipe(op_a_zero_p5, p5_vld, p6_pipe_ena)
    val op_b_zero_p6 = OptPipe(op_b_zero_p5, p5_vld, p6_pipe_ena)
    val op_nan_p6    = OptPipe(op_nan_p5, p5_vld, p6_pipe_ena)

    // 最终结果
    val resultFinal = Fpxx(c)
    resultFinal.sign := sign_p6
    resultFinal.exp := exp_adj_p6(0, c.exp_size bits).asUInt
    resultFinal.mant := div_adj_p6

    when((op_a_zero_p6 && op_b_zero_p6) || op_nan_p6) {
        resultFinal.sign := sign_p6
        resultFinal.set_nan()
    } elsewhen(exp_adj_p6 >= ((1 << c.exp_size) - 1) || op_b_zero_p6) {
        resultFinal.sign := sign_p6
        resultFinal.set_inf()
    } elsewhen(exp_adj_p6 <= 0 || op_a_zero_p6) {
        resultFinal.sign := sign_p6
        resultFinal.set_zero()
    }

    io.result_vld := p6_vld
    io.result := resultFinal
}

// Companion object for generation
object FpxxDivEvenMain extends App {
    import spinal.core.sim._

    val float16Config = FpxxConfig.float16()
    val divConfig = FpxxDivEvenConfig(pipeStages = 2)

    SpinalConfig(
        defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC),
        targetDirectory = "hw/rtl/math"
    ).generateVerilog(new FpxxDivEven(float16Config, divConfig))
}

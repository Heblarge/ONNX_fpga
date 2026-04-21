package FloatingPoint

import spinal.core._
import spinal.lib._

/**
 * FpxxSqrtStream: 带 Stream 接口的浮点平方根模块
 * 
 * 基于 FpxxSqrt 实现，添加 Stream 接口支持反压。
 * 使用查找表实现快速平方根近似。
 * 
 * 算法:
 *   对于输入 x，计算 sqrt(x)
 *   - 特殊情况处理: NaN, Inf, Zero, 负数
 *   - 使用 LUT 加速尾数计算
 * 
 * 接口:
 *   - io.op: slave Stream(Fpxx)   输入浮点数
 *   - io.result: master Stream(Fpxx) 输出平方根结果
 * 
 * 反压支持:
 *   - 使用 m2sPipe 实现流水线阶段
 *   - 当下游 ready=false 时，流水线暂停
 */

object FpxxSqrtStream {
    import caseapp._

    val fpxxConfigHelpMsg =
        "Floating point format. May be one of (float64, float32, float16, bfloat16, e5m2fnuz, e4m2fnuz, eXmX), where X is a whole number and stands for the exponent and mantissa sizes."

    def stageMaskHelpMsg(n: Int) =
        f"How many pipeline stages to use (${n} max). Can be an integer or a mask of the form: " +
            f"m${List.tabulate(n)(_ => 0).mkString}. 0 = disable, 1 = enable. Leftmost digit is the earliest stage."

    case class Options(
        @HelpMessage(fpxxConfigHelpMsg)
        c: FpxxConfig,
        @HelpMessage(stageMaskHelpMsg(1))
        pipeStages: StageMask = 0,
        @HelpMessage("Log2 number of words in the lookup table. Default = no. mantissa bits / 2")
        tableSizeBits: Int = -1,
        @HelpMessage("Number of mantissa bits provided by lookup table. Default = no. mantissa bits")
        lutMantBits: Int = -1
    )
}

case class FpxxSqrtStream(o: FpxxSqrtStream.Options) extends Component {

    assert(o.c.ieee_like, "Can only handle IEEE compliant floats")
    
    def lutMantBits   = if (o.lutMantBits < 0) o.c.mant_size else o.lutMantBits
    def tableSizeBits = if (o.tableSizeBits < 0) o.c.mant_size / 2 else o.tableSizeBits
    def tableSize     = (1 << tableSizeBits) - (1 << (tableSizeBits - 2))
    
    // Convert StageMask to pipeStages count
    implicit val maskConfig = StageMask.Config(1, List(0))
    def pipeStages = o.pipeStages.mask()(maskConfig).count(_ == true)

    val io = new Bundle {
        val op     = slave Stream (Fpxx(o.c))
        val result = master Stream (Fpxx(o.c))
    }

    // 平方根查找表
    def sqrtTableContents = for (i <- 0 until tableSize) yield {
        import FpxxHost._
        
        // Values in range (0.5, 2.0)
        val fin  = ((1L << (tableSizeBits - 2)) + i).toDouble / (1L << (tableSizeBits - 1)).toDouble
        val fout = scala.math.sqrt(fin)
        
        val shift = fin.exp - fout.exp
        
        val round     = (fout.mant >> (fout.c.mant_size - lutMantBits + 1)) & 1
        val fout_mant = (fout.mant >> (fout.c.mant_size - lutMantBits)) + round
        
        U((fout_mant << 2) | (shift & 0x3), (lutMantBits + 2) bits)
    }

    val sqrt_table = Mem(UInt((lutMantBits + 2) bits), initialContent = sqrtTableContents)

    //============================================================
    // Stage 0: 输入处理和表地址计算 (组合逻辑)
    //============================================================
    // 定义 Stage0 输出 Bundle (LUT 读取前的数据)
    case class Stage0PreBundle() extends Bundle {
        val op_zero = Bool()
        val op_nan = Bool()
        val op_inf = Bool()
        val exp = SInt(o.c.exp_size + 1 bits)
    }
    
    // 定义 Stage1 输出 Bundle (包含 LUT 结果)
    case class Stage1Bundle() extends Bundle {
        val op_zero = Bool()
        val op_nan = Bool()
        val op_inf = Bool()
        val exp = SInt(o.c.exp_size + 1 bits)
        val sqrt_val = UInt((lutMantBits + 2) bits)  // LUT 输出
    }
    
    val stage0_op = io.op.payload

    val stage0_zero = stage0_op.is_zero() || stage0_op.is_subnormal()
    val stage0_nan  = stage0_op.is_nan() || stage0_op.sign  // 负数 -> NaN
    val stage0_inf  = stage0_op.is_infinite() && !stage0_op.sign

    val stage0_exp = SInt(o.c.exp_size + 1 bits)
    stage0_exp := stage0_op.exp.resize(o.c.exp_size + 1).asSInt - o.c.bias

    val stage0_gt_1 = !(stage0_exp).lsb

    val stage0_sqrt_addr = UInt(tableSizeBits bits)
    stage0_sqrt_addr := (((U(1, 1 bits) @@ stage0_op.mant) << stage0_gt_1.asUInt) >> (o.c.mant_size + 2 - tableSizeBits)) - (1 << (tableSizeBits - 2))

    // LUT 同步读取 - 输出在下一个周期有效
    val stage0_lut_out = sqrt_table.readSync(stage0_sqrt_addr, io.op.fire)
    
    // 构建 Stage0 输出 (不包含 LUT 值)
    val stage0_pre_bundle = Stage0PreBundle()
    stage0_pre_bundle.op_zero := stage0_zero
    stage0_pre_bundle.op_nan := stage0_nan
    stage0_pre_bundle.op_inf := stage0_inf
    stage0_pre_bundle.exp := stage0_exp
    
    // Stage 0 -> Stage 0.5: 将 pre_bundle 延迟 1 周期，与 LUT 输出对齐
    val stage0_pre_stream = Stream(Stage0PreBundle())
    stage0_pre_stream.valid := io.op.valid
    stage0_pre_stream.payload := stage0_pre_bundle
    io.op.ready := stage0_pre_stream.ready
    
    // 延迟 1 周期（对应 LUT 的同步读取延迟）
    val stage0_post_stream = stage0_pre_stream.m2sPipe()
    
    // 组装完整的 Stage1 Bundle（此时 LUT 输出已与其他信号对齐）
    val stage1_bundle = Stage1Bundle()
    stage1_bundle.op_zero := stage0_post_stream.payload.op_zero
    stage1_bundle.op_nan := stage0_post_stream.payload.op_nan
    stage1_bundle.op_inf := stage0_post_stream.payload.op_inf
    stage1_bundle.exp := stage0_post_stream.payload.exp
    stage1_bundle.sqrt_val := stage0_lut_out  // LUT 输出现在与其他信号对齐
    
    val stage1_pre_stream = Stream(Stage1Bundle())
    stage1_pre_stream.valid := stage0_post_stream.valid
    stage1_pre_stream.payload := stage1_bundle
    stage0_post_stream.ready := stage1_pre_stream.ready
    
    // 添加额外流水线寄存器 (pipeStages 控制)
    val stage1_stream = if (pipeStages >= 1) stage1_pre_stream.m2sPipe() else stage1_pre_stream

    //============================================================
    // Stage 1: 结果计算 (组合逻辑)
    //============================================================
    val stage1_payload = stage1_stream.payload
    
    val sqrt_shift_p1 = stage1_payload.sqrt_val(0, 2 bits)
    val sqrt_mant_p1  = stage1_payload.sqrt_val(2, lutMantBits bits)

    // 指数调整: sqrt(2^e * m) = 2^(e/2) * sqrt(m)
    val exp_adj_p1 = SInt(o.c.exp_size + 1 bits)
    exp_adj_p1 := (stage1_payload.exp |>> 1) - sqrt_shift_p1.resize(3).asSInt + o.c.bias

    val sign_final_p1 = Bool()
    val exp_final_p1  = UInt(o.c.exp_size bits)
    val mant_final_p1 = UInt(o.c.mant_size bits)

    when(stage1_payload.op_nan) {
        // 负数 -> NaN
        sign_final_p1 := False
        exp_final_p1.setAll
        mant_final_p1 := (o.c.mant_size - 1 -> True, default -> False)
    }
    .elsewhen(stage1_payload.op_inf) {
        // Infinite -> Infinite (sqrt(inf) = inf)
        sign_final_p1 := False
        exp_final_p1.setAll
        mant_final_p1.clearAll
    }
    .elsewhen(exp_adj_p1 <= 0 || stage1_payload.op_zero) {
        // Underflow or Zero -> Zero
        sign_final_p1 := False
        exp_final_p1.clearAll
        mant_final_p1.clearAll
    }
    .otherwise {
        sign_final_p1 := False
        exp_final_p1  := exp_adj_p1.asUInt.resize(o.c.exp_size)
        mant_final_p1 := sqrt_mant_p1 << (o.c.mant_size - lutMantBits)
    }

    // 输出 Stream 接口
    io.result.valid := stage1_stream.valid
    io.result.payload.sign := sign_final_p1
    io.result.payload.exp  := exp_final_p1
    io.result.payload.mant := mant_final_p1
    stage1_stream.ready := io.result.ready
}

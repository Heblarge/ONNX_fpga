package FloatingPoint

import spinal.core._
import spinal.lib._

/**
 * FpxxShift2N: 浮点数乘以/除以 2^N (通过指数移位实现)
 * 
 * 功能:
 *   - 计算 x × 2^N (N 可以为正、负或零)
 *   - 通过直接修改指数实现，无需乘法器
 *   - 纯组合逻辑，可直接作用于 payload
 * 
 * 原理:
 *   IEEE 754 浮点数: x = (-1)^sign × 2^(exp-bias) × 1.mant
 *   x × 2^N = (-1)^sign × 2^(exp-bias+N) × 1.mant
 *   即: exp_new = exp + N, mant 保持不变
 * 
 * 边界情况处理:
 *   - NaN: 透传 NaN
 *   - Inf: 透传 Inf
 *   - Zero/Subnormal: 透传 Zero
 *   - 上溢 (exp + N >= exp_max): 输出 ±Inf
 *   - 下溢 (exp + N <= 0): 输出 Zero
 * 
 * 使用方式:
 *   val result = FpxxShift2N.shift(input, shiftAmount, config)
 *   val result = FpxxMul2(input, config)   // ×2
 *   val result = FpxxDiv2(input, config)   // ÷2
 */

object FpxxShift2N {
    /**
     * 浮点数移位运算 (纯组合逻辑)
     * @param op 输入浮点数
     * @param shiftN 移位量 (正数×2^N, 负数÷2^N)
     * @param c 浮点配置
     * @return 移位结果
     */
    def shift(op: Fpxx, shiftN: SInt, c: FpxxConfig): Fpxx = {
        assert(c.ieee_like, "Can only handle IEEE compliant floats")
        
        // 特殊值检测
        val is_zero = op.is_zero() || op.is_subnormal()
        val is_nan = op.is_nan()
        val is_inf = op.is_infinite()
        
        // 指数计算 (扩展位数防止溢出/下溢)
        val exp_extended = op.exp.resize(c.exp_size + 2).asSInt
        val exp_shifted = exp_extended + shiftN.resize(c.exp_size + 2)
        
        // 溢出/下溢检测
        val exp_max = (1 << c.exp_size) - 1  // 全 1，用于 Inf/NaN
        val exp_overflow = exp_shifted >= exp_max
        val exp_underflow = exp_shifted <= 0
        
        // 结果计算
        val result = Fpxx(c)
        result.sign := op.sign
        
        when(is_nan) {
            // NaN 透传
            result.exp.setAll
            result.mant := (c.mant_size - 1 -> True, default -> False)
        }.elsewhen(is_inf) {
            // Inf 透传
            result.exp.setAll
            result.mant.clearAll
        }.elsewhen(is_zero) {
            // Zero 透传
            result.exp.clearAll
            result.mant.clearAll
        }.elsewhen(exp_overflow) {
            // 溢出 -> Inf
            result.exp.setAll
            result.mant.clearAll
        }.elsewhen(exp_underflow) {
            // 下溢 -> Zero (简化处理，不支持 subnormal)
            result.exp.clearAll
            result.mant.clearAll
        }.otherwise {
            // 正常情况
            result.exp := exp_shifted.asUInt.resized
            result.mant := op.mant
        }
        
        result
    }
    
    /**
     * 静态移位量版本 (编译时常量)
     */
    def shift(op: Fpxx, shiftN: Int, c: FpxxConfig): Fpxx = {
        shift(op, S(shiftN, c.exp_size + 2 bits), c)
    }
}

/**
 * 便捷函数: ×2
 */
object FpxxMul2 {
    def apply(op: Fpxx, c: FpxxConfig): Fpxx = FpxxShift2N.shift(op, 1, c)
}

/**
 * 便捷函数: ×4
 */
object FpxxMul4 {
    def apply(op: Fpxx, c: FpxxConfig): Fpxx = FpxxShift2N.shift(op, 2, c)
}

/**
 * 便捷函数: ÷2
 */
object FpxxDiv2 {
    def apply(op: Fpxx, c: FpxxConfig): Fpxx = FpxxShift2N.shift(op, -1, c)
}

/**
 * 便捷函数: ×0.5 (与 ÷2 相同)
 */
object FpxxMul0_5 {
    def apply(op: Fpxx, c: FpxxConfig): Fpxx = FpxxShift2N.shift(op, -1, c)
}

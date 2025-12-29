package ExponentialFunction

import spinal.core._
import spinal.lib._
import scala.math._
import FloatingPoint._ // 假设这里包含了 Fpxx 和 FpxxConfig 定义

// 辅助对象：用于 FP8 数值的编解码 (直接复用 LN 函数中的逻辑)
object FP8Utils {
  // 将整数位模式解码为 Double
  def decode(bits: Int, c: FpxxConfig): Double = {
    val mantMask = (1 << c.mant_size) - 1
    val expMask = (1 << c.exp_size) - 1
    val mant = bits & mantMask
    val exp = (bits >> c.mant_size) & expMask
    val sign = (bits >> (c.mant_size + c.exp_size)) & 1
    val bias = c.bias

    // 1. NaN
    if (c.nan_encoding.isInstanceOf[SpecialNan]) {
      val nanEnc = c.nan_encoding.asInstanceOf[SpecialNan].encoding.toInt
      if (bits == nanEnc) return Double.NaN
    } else if (c.nan_encoding.isInstanceOf[IEEENan]) {
      if (exp == expMask && mant != 0) return Double.NaN
    }

    // 2. Infinity
    if (c.inf_encoding.isInstanceOf[IEEEInfinity]) {
      if (exp == expMask && mant == 0) return if (sign == 1) Double.NegativeInfinity else Double.PositiveInfinity
    }

    // 3. Zero (FNUZ 只有一个零)
    if (exp == 0 && mant == 0) {
      if (!c.signed_zero) return 0.0
      return if (sign == 1) -0.0 else 0.0
    }

    // 4. Denormal / Normal numbers
    val s = if (sign == 1) -1.0 else 1.0
    if (exp == 0) {
      s * Math.pow(2, 1 - bias) * (mant.toDouble / Math.pow(2, c.mant_size))
    } else {
      s * Math.pow(2, exp - bias) * (1.0 + mant.toDouble / Math.pow(2, c.mant_size))
    }
  }

  // 暴力搜索：找到最接近 targetValue 的 FP8 位模式
  def encodeNearest(targetValue: Double, c: FpxxConfig): Int = {
    // NaN
    if (targetValue.isNaN) {
      c.nan_encoding match {
        case SpecialNan(enc) => return enc.toInt
        case IEEENan() => return ((1 << (c.exp_size + c.mant_size)) - 1)
      }
    }
    
    // +Inf
    if (targetValue.isPosInfinity) {
       c.inf_encoding match {
         case IEEEInfinity() => return ((1 << c.exp_size) - 1) << c.mant_size
         case NoInfinity(_) => return findMaxFinite(c, positive = true)
       }
    }
    
    // -Inf
    if (targetValue.isNegInfinity) {
       c.inf_encoding match {
         case IEEEInfinity() => return (1 << (c.exp_size + c.mant_size)) | (((1 << c.exp_size) - 1) << c.mant_size)
         case NoInfinity(_) => return findMaxFinite(c, positive = false)
       }
    }

    // 暴力搜索最小差值
    var minDiff = Double.MaxValue
    var bestBits = 0
    val maxCode = (1 << c.full_size) - 1

    for (i <- 0 to maxCode) {
      val valDecoded = decode(i, c)
      if (!valDecoded.isNaN) {
        val diff = Math.abs(valDecoded - targetValue)
        if (diff < minDiff) {
          minDiff = diff
          bestBits = i
        }
      }
    }
    bestBits
  }

  def findMaxFinite(c: FpxxConfig, positive: Boolean): Int = {
      var maxVal = -1.0
      var bestBits = 0
      val maxCode = (1 << c.full_size) - 1
      for (i <- 0 to maxCode) {
          val v = decode(i, c)
          if (!v.isNaN && !v.isInfinite) {
             if (positive && v > maxVal) { maxVal = v; bestBits = i }
             if (!positive && v < 0 && (maxVal == -1.0 || v < maxVal)) { maxVal = v; bestBits = i }
          }
      }
      bestBits
  }
}

// ===========================================================================
// 主模块：参数化 FP8 指数函数 (EXP) 查表
// ===========================================================================
case class EXP_function_LUT(c: FpxxConfig) extends Component {
  require(c.full_size == 8, "EXP_function_LUT is designed for 8-bit Floating Point (FP8) only.")

  val io = new Bundle {
    val x      = slave Flow Fpxx(c)    // 输入流
    val expx   = master Flow Fpxx(c)   // 输出流 (原命名为 result, 这里改为 expx 以贴合原意)
  }

  // 1. 预计算 LUT 内容 (Scala 阶段执行)
  // 内容为：encode( exp( decode(index) ) )
  val lutSize = 1 << c.full_size
  val lutContent = for (i <- 0 until lutSize) yield {
    // a. 解码输入
    val x_val = FP8Utils.decode(i, c)

    // b. 计算 exp(x)
    // 规则：
    // exp(NaN) = NaN
    // exp(+Inf) = +Inf
    // exp(-Inf) = 0
    // exp(0) = 1
    // exp(value) = Math.exp(value)
    val exp_val = if (x_val.isNaN) Double.NaN
                  else if (x_val.isPosInfinity) Double.PositiveInfinity
                  else if (x_val.isNegInfinity) 0.0
                  else Math.exp(x_val)
    
    // --- 【新增修复逻辑】 ---
    // 获取当前 FP8 格式的最大有限值 (Max Finite)
    // 对于 E4M3 FNUZ，最大值通常对应位模式 0x7F (正)
    val max_finite_val = FP8Utils.decode(FP8Utils.findMaxFinite(c, true), c)
    
    val res_bits = if (exp_val > max_finite_val) {
      // 如果真值超过了 FP8 最大值
      // 1. 如果格式支持 Infinity，返回 Infinity
      // 2. 否则，饱和到最大值 (Saturate to Max)
      // 这里根据 E4M3 FNUZ 特性，通常选择饱和到最大值
      FP8Utils.findMaxFinite(c, true) 
    } else {
      // 正常编码
      FP8Utils.encodeNearest(exp_val, c)
    }

    // c. 编码回 FP8
    // 注意：FP8 E4M3 的最大值约为 448，exp(6.1) ≈ 445，exp(6.2) ≈ 492
    // 因此当 x > 6.1 时，结果会饱和到最大值 (Saturate) 或 Inf
//    val res_bits = FP8Utils.encodeNearest(exp_val, c)

    B(res_bits, c.full_size bits)
  }

  // 2. 硬件逻辑实现 (完全复用 LN 的流水线结构)
  
  // 将输入 Fpxx 转换为地址
  val address = io.x.payload.asBits.asUInt
  
  // ROM 定义
  val lutMem = Vec(lutContent)
  
  // Stage 1
  val addr_reg = RegNext(address)
  val valid_reg = RegNext(io.x.valid) init(False)

  // Stage 2
  val data_comb = lutMem(addr_reg)
  val data_reg = RegNext(data_comb)
  val valid_reg2 = RegNext(valid_reg) init(False)

  // 输出
  io.expx.valid := valid_reg2
  io.expx.payload.assignFromBits(data_reg)
}

// 生成模块
object EXP_function_Gen_FP8_LUT {
  def main(args: Array[String]): Unit = {
    // 使用 FP8 E4M3 FNUZ 格式
    val cfg = FpxxConfig.float8_e4m3fnuz()

    SpinalConfig(
      targetDirectory = "rtl/FP8_look_up",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(EXP_function_LUT(cfg))
  }
}

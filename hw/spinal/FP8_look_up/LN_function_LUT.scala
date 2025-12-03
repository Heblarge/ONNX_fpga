package LogarithmFunction

import spinal.core._
import spinal.lib._
import scala.math._
import FloatingPoint._


// 辅助对象：用于在生成 LUT 时处理 FP8 数值的编解码
object FP8Utils {
  // 将整数位模式解码为 Double
  def decode(bits: Int, c: FpxxConfig): Double = {
    val mantMask = (1 << c.mant_size) - 1
    val expMask = (1 << c.exp_size) - 1

    val mant = bits & mantMask
    val exp = (bits >> c.mant_size) & expMask
    val sign = (bits >> (c.mant_size + c.exp_size)) & 1

    val bias = c.bias

    // 处理特殊情况
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

    // 3. Zero
    if (exp == 0 && mant == 0) {
      // FNUZ (Signed Zero = false) 只有一个零，通常视为正零
      if (!c.signed_zero) return 0.0
      return if (sign == 1) -0.0 else 0.0
    }

    // 4. Denormal / Normal numbers
    val s = if (sign == 1) -1.0 else 1.0
    
    if (exp == 0) {
      // Denormal (Subnormal)
      // Value = (-1)^s * 2^(1 - bias) * (mant / 2^mant_bits)
      s * Math.pow(2, 1 - bias) * (mant.toDouble / Math.pow(2, c.mant_size))
    } else {
      // Normal
      // Value = (-1)^s * 2^(exp - bias) * (1 + mant / 2^mant_bits)
      s * Math.pow(2, exp - bias) * (1.0 + mant.toDouble / Math.pow(2, c.mant_size))
    }
  }

  // 暴力搜索：找到最接近 targetValue 的 FP8 位模式
  def encodeNearest(targetValue: Double, c: FpxxConfig): Int = {
    // 处理特殊输出结果
    if (targetValue.isNaN) {
      c.nan_encoding match {
        case SpecialNan(enc) => return enc.toInt
        case IEEENan() => return ((1 << (c.exp_size + c.mant_size)) - 1) // 设置为全1或者特定的NaN
      }
    }
    
    if (targetValue.isPosInfinity) {
       c.inf_encoding match {
         case IEEEInfinity() => return ((1 << c.exp_size) - 1) << c.mant_size // Exp全1, Mant 0
         case NoInfinity(_) => // 如果不支持Inf，返回最大正数 (Saturate)
             return findMaxFinite(c, positive = true)
       }
    }
    
    if (targetValue.isNegInfinity) {
       c.inf_encoding match {
         case IEEEInfinity() => return (1 << (c.exp_size + c.mant_size)) | (((1 << c.exp_size) - 1) << c.mant_size)
         case NoInfinity(_) => // 如果不支持Inf，返回最大负数
             return findMaxFinite(c, positive = false)
       }
    }

    // 遍历所有可能的 256 个值，寻找差值最小的
    var minDiff = Double.MaxValue
    var bestBits = 0
    val maxCode = (1 << c.full_size) - 1

    for (i <- 0 to maxCode) {
      val valDecoded = decode(i, c)
      // 跳过 NaN 用于比较
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
      // 简单查找逻辑：寻找非NaN非Inf的最大值
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

// 主模块：参数化 FP8 对数函数查表
case class LN_function_LUT(c: FpxxConfig) extends Component {
  // 强制要求 FP8，因为这是针对 8-bit 全映射设计的
  require(c.full_size == 8, "LN_function_LUT is designed for 8-bit Floating Point (FP8) only.")

  val io = new Bundle {
    val op = slave Flow Fpxx(c)     // 输入流 (FP8)
    val result = master Flow Fpxx(c) // 输出流 (FP8)
  }

  // 1. 预计算 LUT 内容 (Scala 阶段执行)
  // 这是一个 256 深度，宽度为 8 bits 的 ROM
  // 内容为：encode( ln( decode(index) ) )
  val lutSize = 1 << c.full_size
  val lutContent = for (i <- 0 until lutSize) yield {
    // a. 将索引 i 解码为浮点数 x
    val x = FP8Utils.decode(i, c)

    // b. 计算 ln(x)
    // 规则：
    // ln(NaN) = NaN
    // ln(x < 0) = NaN
    // ln(0) = -Inf
    // ln(+Inf) = +Inf
    val ln_val = if (x.isNaN) Double.NaN
                 else if (x < 0) Double.NaN // 复数域在硬件实数计算中通常返回 NaN
                 else if (x == 0.0) Double.NegativeInfinity
                 else Math.log(x)

    // c. 将结果编码回 FP8 格式的 bits
    val res_bits = FP8Utils.encodeNearest(ln_val, c)

    // d. 返回 Bits 类型
    B(res_bits, c.full_size bits)
  }

  // 2. 硬件逻辑实现
  
  // 将输入 Fpxx 视为纯粹的地址索引 (UInt)
  val address = io.op.payload.asBits.asUInt
  
  // 定义 ROM (Vec)
  val lutMem = Vec(lutContent)
  
  // 流水线 Stage 1: 寄存输入地址和 Valid 信号
  val addr_reg = RegNext(address)
  val valid_reg = RegNext(io.op.valid) init(False)

  // 流水线 Stage 2: 查表并寄存输出
  // 读取 LUT (同步读取，或者组合逻辑读取后寄存，这里采用组合读取后寄存以匹配时序)
  val data_comb = lutMem(addr_reg)
  
  val data_reg = RegNext(data_comb)
  val valid_reg2 = RegNext(valid_reg) init(False)

  // 输出赋值
  io.result.valid := valid_reg2
  // 将查表得到的 Bits 转换回 Fpxx 结构
  io.result.payload.assignFromBits(data_reg)
}

// 测试/生成模块
object LN_function_Gen_FP8_LUT {
  def main(args: Array[String]): Unit = {
    // 使用 FP8 E4M3 FNUZ 格式 (常用 AI 推理格式)
    val cfg = FpxxConfig.float8_e4m3fnuz()

    SpinalConfig(
      targetDirectory = "rtl/FP8_look_up",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(LN_function_LUT(cfg))
  }
}
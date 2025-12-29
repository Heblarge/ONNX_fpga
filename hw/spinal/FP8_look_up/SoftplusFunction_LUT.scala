package SoftplusFunction

import spinal.core._
import spinal.lib._
import scala.math._
import FloatingPoint._ 

// ===========================================================================
// 辅助对象：用于 FP8 数值的编解码 (直接复用 LN 函数中的逻辑)
// ===========================================================================
object Softplus_FP8Utils {
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

    // 3. Zero
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

    // 遍历搜索
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
// 主模块：参数化 FP8 Softplus 函数查表
// ===========================================================================
case class Softplus_function_LUT(c: FpxxConfig) extends Component {
  // 强制要求 FP8
  require(c.full_size == 8, "Softplus_function_LUT is designed for 8-bit Floating Point (FP8) only.")

  val io = new Bundle {
    val x         = slave Flow Fpxx(c)    // 输入流
    val softplusx = master Flow Fpxx(c)   // 输出流
  }

  // 1. 预计算 LUT 内容 (Scala 阶段执行)
  // 内容为：encode( Softplus( decode(index) ) )
  val lutSize = 1 << c.full_size
  val lutContent = for (i <- 0 until lutSize) yield {
    // a. 将索引 i 解码为浮点数 x
    val x = Softplus_FP8Utils.decode(i, c)

    // b. 计算 Softplus(x) = ln(1 + e^x)
    // 逻辑：
    // 1. NaN -> NaN
    // 2. +Inf -> +Inf (因为 ln(1+inf) = inf)
    // 3. -Inf -> 0    (因为 ln(1+0) = 0)
    // 4. x > 20 -> x  (数值稳定性：当 x 很大时，ln(1+e^x) ≈ ln(e^x) = x)
    // 5. 其他 -> Math.log(1 + Math.exp(x))
    val sp_val = if (x.isNaN) Double.NaN
                 else if (x.isPosInfinity) Double.PositiveInfinity
                 else if (x.isNegInfinity) 0.0
                 else if (x > 20.0) x // 防止 exp(x) 溢出 Double 范围，且此时 Softplus 近似线性
                 else Math.log(1.0 + Math.exp(x))

    // c. 将结果编码回 FP8 格式
    val res_bits = Softplus_FP8Utils.encodeNearest(sp_val, c)

    // d. 返回 Bits 类型
    B(res_bits, c.full_size bits)
  }

  // 2. 硬件逻辑实现 (标准的 2 级流水线查表)
  
  // 将输入 Fpxx 视为地址索引
  val address = io.x.payload.asBits.asUInt
  
  // 定义 ROM
  val lutMem = Vec(lutContent)
  
  // 流水线 Stage 1: 锁存地址
  val addr_reg = RegNext(address)
  val valid_reg = RegNext(io.x.valid) init(False)

  // 流水线 Stage 2: 读表并锁存数据
  val data_comb = lutMem(addr_reg)
  val data_reg = RegNext(data_comb)
  val valid_reg2 = RegNext(valid_reg) init(False)

  // 输出赋值
  io.softplusx.valid := valid_reg2
  io.softplusx.payload.assignFromBits(data_reg)
}

// 测试/生成模块
object Softplus_function_Gen_FP8_LUT {
  def main(args: Array[String]): Unit = {
    // 使用 FP8 E4M3 FNUZ 格式
    val cfg = FpxxConfig.float8_e4m3fnuz()

    SpinalConfig(
      targetDirectory = "rtl/FP8_look_up",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(Softplus_function_LUT(cfg))
  }
}

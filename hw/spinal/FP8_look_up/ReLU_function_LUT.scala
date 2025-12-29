package ReLUFunction

import spinal.core._
import spinal.lib._
import FloatingPoint._ 
import scala.math._

// 辅助对象：复用之前的逻辑，用于在生成 LUT 时处理 FP8 数值的编解码
// 如果你在同一个工程下，可以直接复用 LogarithmFunction 中的 FP8Utils，
// 这里为了代码的独立完整性，我将其包含在内并简化为 ReLU 需要的部分。
object ReLU_FP8Utils {
  // 将整数位模式解码为 Double
  def decode(bits: Int, c: FpxxConfig): Double = {
    val mantMask = (1 << c.mant_size) - 1
    val expMask = (1 << c.exp_size) - 1
    val mant = bits & mantMask
    val exp = (bits >> c.mant_size) & expMask
    val sign = (bits >> (c.mant_size + c.exp_size)) & 1
    val bias = c.bias

    // 1. NaN (支持 IEEE 和 SpecialNan)
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
    // ReLU 的特殊处理：如果目标是 0，直接返回正零的编码
    if (targetValue == 0.0) return 0

    // NaN 处理
    if (targetValue.isNaN) {
      c.nan_encoding match {
        case SpecialNan(enc) => return enc.toInt
        case IEEENan() => return ((1 << (c.exp_size + c.mant_size)) - 1)
      }
    }
    
    // Inf 处理
    if (targetValue.isPosInfinity) {
       c.inf_encoding match {
         case IEEEInfinity() => return ((1 << c.exp_size) - 1) << c.mant_size
         case NoInfinity(_) => return findMaxFinite(c, positive = true)
       }
    }

    // 遍历搜索最接近值
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

// ReLU 核心模块
case class ReLU_function_LUT(c: FpxxConfig) extends Component {
  // 强制 FP8
  require(c.full_size == 8, "ReLU_function_LUT is designed for 8-bit Floating Point (FP8) only.")

  val io = new Bundle {
    val x       = slave Flow Fpxx(c)    // 输入 Flow
    val relux   = master Flow Fpxx(c)   // 输出 Flow
  }
  noIoPrefix()

  // 1. 预计算 LUT 内容 (Software / Generation time)
  val lutSize = 1 << c.full_size
  val lutContent = for (i <- 0 until lutSize) yield {
    // a. 解码输入
    val x_val = ReLU_FP8Utils.decode(i, c)

    // b. 计算 ReLU: f(x) = max(0, x)
    // 逻辑：
    // 1. NaN -> NaN
    // 2. x < 0 -> 0
    // 3. x >= 0 -> x (直通)
    val relu_val = if (x_val.isNaN) Double.NaN
                   else if (x_val < 0.0) 0.0
                   else x_val

    // c. 编码回 FP8
    val res_bits = ReLU_FP8Utils.encodeNearest(relu_val, c)

    B(res_bits, c.full_size bits)
  }

  // 2. 硬件逻辑实现 (Hardware)
  
  // 将输入 Fpxx 转换为无符号整数作为地址
  val address = io.x.payload.asBits.asUInt
  
  // 定义 ROM
  val lutMem = Vec(lutContent)

  // 采用两级流水线结构 (同 LN_function 保持一致)
  // Stage 1: 锁存地址
  val addr_reg  = RegNext(address)
  val valid_reg = RegNext(io.x.valid) init(False)

  // Stage 2: 读表并锁存数据
  val data_comb = lutMem(addr_reg)
  val data_reg  = RegNext(data_comb)
  val valid_reg2 = RegNext(valid_reg) init(False)

  // 输出
  io.relux.valid   := valid_reg2
  io.relux.payload.assignFromBits(data_reg)
}

// 生成与测试对象
object ReLU_function_Gen {
  def main(args: Array[String]): Unit = {
    // 使用 FP8 E4M3 FNUZ 格式
    val cfg = FpxxConfig.float8_e4m3fnuz()

    SpinalConfig(
      targetDirectory = "rtl/FP8_look_up",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(ReLU_function_LUT(cfg))
  }
}

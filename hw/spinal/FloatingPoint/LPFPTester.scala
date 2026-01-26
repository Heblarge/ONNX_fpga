package FloatingPoint

import spinal.core._
import spinal.core.sim._
import scala.util.Random

// --- RTL GEN ---


// --- 增强型软件浮点逻辑 ---
object SoftFloat {
  /**
   * 通用浮点解码：支持 FP16 和 FP8
   */
  def decodeFpxx(bits: Long, cfg: FpxxConfig): Double = {
    val exp_size = cfg.exp_size
    val mant_size = cfg.mant_size
    val bias = (1 << (exp_size - 1)) - 1
    
    val signMask = 1L << (exp_size + mant_size)
    val expMask = (1L << exp_size) - 1
    val mantMask = (1L << mant_size) - 1

    val sign = if ((bits & signMask) != 0) -1.0 else 1.0
    val exp = (bits >> mant_size) & expMask
    val mant = bits & mantMask

    if (exp == expMask) {
      Double.NaN // 简化处理，实际 FP8 E4M3 的 NaN 处理略有不同
    } else if (exp == 0) {
      // 次正规数 (Subnormal)
      sign * math.pow(2, 1 - bias) * (mant.toDouble / math.pow(2, mant_size))
    } else {
      // 正规数 (Normal)
      sign * math.pow(2, exp - bias) * (1.0 + mant.toDouble / math.pow(2, mant_size))
    }
  }

  def softwareMultiply(aBits: Long, bBits: Long, cfg: FpxxConfig): Double = {
    decodeFpxx(aBits, cfg) * decodeFpxx(bBits, cfg)
  }
}

object FpxxPETest extends App {

  /**
   * 将 Float 转换为指定配置的位模式 (支持 FP16, FP8 等)
   */
  def floatToRawBits(f: Float, cfg: FpxxConfig): Long = {
    import java.lang.Float._
    val fbits = floatToIntBits(f)
    val fSign = (fbits >>> 31) & 0x1
    val fExp = ((fbits >>> 23) & 0xFF) - 127
    val fMant = fbits & 0x7FFFFF

    val targetExpWidth = cfg.exp_size
    val targetMantWidth = cfg.mant_size
    val targetBias = (1 << (targetExpWidth - 1)) - 1
    
    var resExp = fExp + targetBias
    var resMant = fMant >> (23 - targetMantWidth)

    // 边界检查与饱和处理 (简单的截断逻辑)
    if (f == 0.0f) return 0L
    if (resExp >= (1 << targetExpWidth) - 1) {
       resExp = (1 << targetExpWidth) - 1
       resMant = (1 << targetMantWidth) - 1 // 饱和到最大值
    } else if (resExp <= 0) {
       resExp = 0
       resMant = 0 // 简化处理为 0
    }

    (fSign.toLong << (targetExpWidth + targetMantWidth)) | (resExp.toLong << targetMantWidth) | resMant.toLong
  }

  // --- 这里的配置可以灵活切换 ---
  // val fpxxCfg = FpxxConfig.float16() 
  val fpxxCfg = FpxxConfig.float16() // FP8 E4M3 示例
  
  val accIntBits  = 32 bits
  val accFracBits = 16 bits

  val compiled = SimConfig.withVcdWave.compile {
    new FpxxPE(fpxxCfg, accIntBits, accFracBits)
  }

  compiled.doSim { dut =>
    dut.clockDomain.forkStimulus(10)
    
    dut.io.inSig.valid #= false
    dut.io.clear       #= true
    dut.clockDomain.waitSampling(5)
    dut.io.clear       #= false

    var hwValue       = BigDecimal(0)
    var softwareGolden = BigDecimal(0)
    var validCount    = 0
    val testCount     = 20
    val frac          = accFracBits.value

    fork {
      while(true) {
        dut.clockDomain.waitSampling()
        if(dut.io.out.valid.toBoolean) {
          hwValue = dut.io.out.payload.toBigDecimal
          validCount += 1
        }
      }
    }

    val rnd = new Random(42)
    println(f"Testing Config: Exp=${fpxxCfg.exp_size}, Mant=${fpxxCfg.mant_size}")
    println(f"${"Index"}%-5s | ${"Input A"}%-10s | ${"Input B"}%-10s | ${"Software Golden"}%-20s | ${"Hardware LPFP"}")
    println("-" * 100)

    for (i <- 0 until testCount) {
      // 为了适应 FP8 较小的动态范围，减小随机数范围
      val range = if(fpxxCfg.exp_size < 5) 2.0f else 10.0f
      val fA = (rnd.nextFloat() * 2.0f - 1.0f) * range
      val fB = (rnd.nextFloat() * 2.0f - 1.0f) * range

      // 1. 转换为目标位宽的原始位
      val bitsA = floatToRawBits(fA, fpxxCfg)
      val bitsB = floatToRawBits(fB, fpxxCfg)

      // 2. 软件计算
      val softProd = SoftFloat.softwareMultiply(bitsA, bitsB, fpxxCfg)
      val softProdFixed = BigDecimal(softProd).setScale(frac, BigDecimal.RoundingMode.FLOOR)
      softwareGolden += softProdFixed

      // 3. 驱动硬件 (注意：FpxxHost 内部需要能处理不同位宽)
      // 如果你的 FpxxHost 只接受 Long 或 BigInt，这里直接传入 bits 即可
      dut.io.inSig.payload.a #= FpxxHost(bitsA,fpxxCfg)
      dut.io.inSig.payload.b #= FpxxHost(bitsB,fpxxCfg)
      dut.io.inSig.valid #= true
      
      dut.clockDomain.waitSampling()
      dut.io.inSig.valid #= false
      
      dut.clockDomain.waitSampling(1) 
      println(f"$i%-5d | $fA%10.4f | $fB%10.4f | $softwareGolden%20.10f | $hwValue%20.10f")
    }

    waitUntil(validCount == testCount)
    // ... 打印结果和判断逻辑 ...
    simSuccess()
  }
}
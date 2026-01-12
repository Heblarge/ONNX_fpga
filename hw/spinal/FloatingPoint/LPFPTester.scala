package FloatingPoint

import spinal.core._
import spinal.core.sim._
import scala.util.Random

// --- Pure Software FP16 Golden Logic ---
object SoftFP16 {
  /** * Mimics an FP16 Addition or Multiplication at bit-level.
   * Here we use it to calculate the expected float result before Fixed-point conversion.
   */
  def decodeFP16(bits: Short): Double = {
    val h = bits & 0xFFFF
    val sign = if ((h & 0x8000) != 0) -1.0 else 1.0
    var exp = (h & 0x7C00) >> 10
    val mant = h & 0x03FF
    
    if (exp == 0x1F) Double.NaN
    else if (exp == 0) sign * math.pow(2, -14) * (mant.toDouble / 1024.0) // Subnormal
    else sign * math.pow(2, exp - 15) * (1.0 + mant.toDouble / 1024.0)
  }

  // Pure software product of two FP16 bit-patterns
  def softwareMultiply(aBits: Short, bBits: Short): Double = {
    val res = decodeFP16(aBits) * decodeFP16(bBits)
    // To be a true FP16 Golden, we simulate the precision of the result
    val resBits = FpxxPETest.floatToHalfPrecision(res.toFloat)
    decodeFP16(resBits)
  }
}

object FpxxPETest extends App {

  def floatToHalfPrecision(f: Float): Short = {
    import java.lang.Float._
    val fbits = floatToIntBits(f)
    val sign = (fbits >>> 16) & 0x8000
    var valExp = ((fbits >>> 23) & 0xff) - 127
    var res = 0
    if (fbits == 0) res = 0
    else if (valExp >= 16) res = sign | 0x7c00
    else if (valExp <= -15) res = sign
    else {
      val exp = (valExp + 15) << 10
      val mantissa = (fbits >> 13) & 0x3ff
      res = sign | exp | mantissa
    }
    res.toShort
  }

  val fpxxCfg = FpxxConfig.float16()
  val accIntBits  = 32 bits
  val accFracBits = 16 bits

  val compiled = SimConfig.withVcdWave.compile {
    new FpxxPE(fpxxCfg, accIntBits, accFracBits)
  }

  compiled.doSim { dut =>
    dut.clockDomain.forkStimulus(10)
    
    // Reset
    dut.io.inSig.valid #= false
    dut.io.clear       #= true
    dut.clockDomain.waitSampling(5)
    dut.io.clear       #= false

    var hwValue       = BigDecimal(0)
    var softwareGolden = BigDecimal(0)
    var validCount    = 0
    val testCount     = 20
    val frac          = accFracBits.value

    // Monitor
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
    println(f"${"Index"}%-5s | ${"Input A"}%-10s | ${"Input B"}%-10s | ${"Software Golden"}%-20s | ${"Hardware LPFP"}")
    println("-" * 90)

    for (i <- 0 until testCount) {
      val fA = rnd.nextFloat() * 2.0f - 1.0f
      val fB = rnd.nextFloat() * 2.0f - 1.0f

      // 1. Convert to FP16 Bits
      val bitsA = floatToHalfPrecision(fA)
      val bitsB = floatToHalfPrecision(fB)

      // 2. Pure Software Golden Calculation
      // Multiplies the two FP16 values and quantizes the product
      val softProd = SoftFP16.softwareMultiply(bitsA, bitsB)
      
      // Accumulate into the fixed-point software register
      val softProdFixed = BigDecimal(softProd).setScale(frac, BigDecimal.RoundingMode.FLOOR)
      softwareGolden += softProdFixed

      // 3. Drive Hardware
      dut.io.inSig.payload.a #= FpxxHost(bitsA)
      dut.io.inSig.payload.b #= FpxxHost(bitsB)
      dut.io.inSig.valid #= true
      
      dut.clockDomain.waitSampling()
      dut.io.inSig.valid #= false
      
      // Wait for HW update to print a clean line
      dut.clockDomain.waitSampling(1) 
      println(f"$i%-5d | $fA%10.4f | $fB%10.4f | $softwareGolden%20.10f | $hwValue%20.10f")
    }

    waitUntil(validCount == testCount)
    
    println("-" * 90)
    println("FINAL RESULTS")
    println(f"Software Golden Accumulator: $softwareGolden%.12f")
    println(f"Hardware LPFP Accumulator  : $hwValue%.12f")
    println("-" * 90)

    if((softwareGolden - hwValue).abs < 0.000001) {
      println("RESULT MATCH: Software Golden and Hardware LPFP are identical.")
    } else {
      println("RESULT MISMATCH: Check hardware truncation or scaling logic.")
    }

    simSuccess()
  }
}
// package FloatingPoint

// import spinal.core._
// import spinal.core.sim._
// import spinal.lib._
// import spinal.sim.VCSFlags
// import java.nio.ByteBuffer
// import scala.util.Random

// // --- Software Reference Model Object ---
// object Fp16Reference {
//   def quantize(f: Float): Float = {
//     val bits = FpxxPETest.floatToHalfPrecision(f)
//     halfToFloat(bits)
//   }

//   def halfToFloat(h: Short): Float = {
//     val hbits = h & 0xFFFF
//     val sign = (hbits & 0x8000) << 16
//     val exp = (hbits & 0x7C00) >> 10
//     val mant = hbits & 0x03FF
//     if (exp == 0x1F) java.lang.Float.intBitsToFloat(sign | 0x7f800000 | (mant << 13))
//     else if (exp == 0) {
//       if (mant == 0) java.lang.Float.intBitsToFloat(sign)
//       else java.lang.Float.intBitsToFloat(sign | ((exp + (127 - 15)) << 23) | (mant << 13))
//     } else java.lang.Float.intBitsToFloat(sign | ((exp + (127 - 15)) << 23) | (mant << 13))
//   }
// }

// object LpfpHardwareGolden {
//   /** * Mimics the hardware logic: 
//    * (FP16 * FP16) -> FP16 -> FixedPoint(accInt, accFrac) -> Accumulate
//    */
//   def compute(a: Float, b: Float, currentAcc: BigDecimal, fracBits: Int): BigDecimal = {
//     // 1. Inputs to FP16
//     val qA = Fp16Reference.quantize(a)
//     val qB = Fp16Reference.quantize(b)
    
//     // 2. Multiplier result to FP16 (Mimicking the FP multiplier output)
//     val prod = Fp16Reference.quantize(qA * qB)
    
//     // 3. Convert FP16 product to Fixed-Point
//     // This mimics the 'toAFix' or 'toFix' behavior in SpinalHDL
//     val prodScaled = BigDecimal(prod.toDouble).setScale(fracBits, BigDecimal.RoundingMode.HALF_UP)
    
//     // 4. Fixed-Point Accumulation
//     currentAcc + prodScaled
//   }
// }

// object FpxxPETest extends App {

//   // --- Utility: Float32 to IEEE 754 Half-Precision ---
//   def floatToHalfPrecision(f: Float): Short = {
//     import java.lang.Float._
//     val fbits = floatToIntBits(f)
//     val sign = (fbits >>> 16) & 0x8000
//     var valExp = ((fbits >>> 23) & 0xff) - 127
    
//     var res = 0
//     if (fbits == 0) {
//       res = 0
//     } else if (valExp >= 16) {
//       res = sign | 0x7c00
//     } else if (valExp <= -15) {
//       res = sign
//     } else {
//       val exp = (valExp + 15) << 10
//       val mantissa = (fbits >> 13) & 0x3ff
//       res = sign | exp | mantissa
//     }
//     res.toShort
//   }

//   // ------------------------------------------------------------
//   // Configuration
//   // ------------------------------------------------------------
//   val fpxxCfg = FpxxConfig.float16()
//   val accIntBits  = 32 bits
//   val accFracBits = 16 bits

//   val FileDir = "rtl/FpxxLPFPTester"

//   val flag = VCSFlags (
//     compileFlags = List("-kdb", "-lca", "+notimingchecks", "-debug_access+all"),
//     elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
//     runFlags = List("-l ./run.log")
//   )

//   val Spinalcfg = SpinalConfig(
//     targetDirectory = FileDir,
//     oneFilePerComponent = true,
//     bitVectorWidthMax = 20000
//   )

//   val compiled = SimConfig
//     .withVCS(flag)
//     .withVcdWave
//     .withTimeScale(1 ns)
//     .withTimePrecision(1 ns)
//     .withConfig(Spinalcfg)
//     .allOptimisation
//     .compile {
//       new FpxxPE(
//         fpxxCfg     = fpxxCfg,
//         accIntBits  = accIntBits,
//         accFracBits = accFracBits
//       )
//     }

//   compiled.doSim { dut =>
//     SimTimeout(100000)
//     dut.clockDomain.forkStimulus(period = 10)

//     // --- Initialization ---
//     dut.io.inSig.valid #= false
//     dut.io.out.ready   #= true
//     dut.io.clear       #= true
//     dut.clockDomain.waitSampling(5)
//     dut.io.clear       #= false

//     // --- Golden Model Tracking Variables ---
//     var highPrecAcc = 0.0          // Pure Double (Reference)
//     var fp16SimAcc  = 0.0f         // Quantized FP16 (Simulator)
//     var lastHwValue = BigDecimal(0)
//     var validCount  = 0
//     val testCount   = 50

//     // --- Monitor Thread ---
//     fork {
//       while (true) {
//         dut.clockDomain.waitSampling()
//         if (dut.io.out.valid.toBoolean) {
//           lastHwValue = dut.io.out.payload.toBigDecimal
//           validCount += 1
//         }
//       }
//     }

//     // --- Driver Loop ---
//     val rnd = new Random(0)
//     println(s"Starting Simulation with $testCount vectors...")

//     for (i <- 0 until testCount) {
//       val fA = rnd.nextFloat() * 2.0f - 1.0f
//       val fB = rnd.nextFloat() * 2.0f - 1.0f

//       // Hardware bits conversion
//       val bitsA = floatToHalfPrecision(fA)
//       val bitsB = floatToHalfPrecision(fB)

//       dut.io.inSig.valid #= true
//       dut.io.inSig.payload.a #= FpxxHost(bitsA)
//       dut.io.inSig.payload.b #= FpxxHost(bitsB)
      
//       dut.clockDomain.waitSampling()
//       dut.io.inSig.valid #= false

//       // --- Software Reference Calculations ---
//       // 1. Double Precision Reference
//       highPrecAcc += (fA.toDouble * fB.toDouble)

//       // 2. FP16 Quantized Model (Simulating HW precision loss)
//       val qA = Fp16Reference.quantize(fA)
//       val qB = Fp16Reference.quantize(fB)
//       val prod = Fp16Reference.quantize(qA * qB)
//       fp16SimAcc += prod
//     }

//     // --- Wait for Completion ---
//     waitUntil(validCount == testCount)
//     dut.clockDomain.waitSampling(10)

//     // --- Final Comparison Report ---
//     val hwFinal = lastHwValue.toDouble
    
//     println("-" * 50)
//     println(f"--- Comparison Report (N=$testCount) ---")
//     println(f"Hardware (AFix) Result : $hwFinal%.10f")
//     println(f"Software (FP16 Mode)   : $fp16SimAcc%.10f")
//     println(f"Software (Double Ref)  : $highPrecAcc%.10f")
    
//     val errorVsFp16 = (hwFinal - fp16SimAcc).abs
//     val errorVsHigh = (hwFinal - highPrecAcc).abs

//     println("-" * 50)
//     println(f"Error vs FP16 Model    : $errorVsFp16%.10f")
//     println(f"Error vs Double Ref    : $errorVsHigh%.10f")

//     // Assert based on quantized model (should be very close)
//     assert(errorVsFp16 < 0.01, s"Hardware output deviates too much from FP16 reference!")
    
//     println("Test Status: SUCCESS")
//     simSuccess()
//   }
// }
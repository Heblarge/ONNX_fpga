package FloatingPoint

import spinal.core._
import spinal.core.sim._
import FpxxTesterSupport._
import spinal.sim.VCSFlags
import java.io.File
import scala.collection.mutable.Queue

case class FpxxDivStreamsDut(config: FpxxConfig) extends Component {
  val io = new Bundle {
    val in_valid  = in(Bool)
    val in_ready  = out(Bool)
    val op_a      = in(Bits(config.full_size bits))
    val op_b      = in(Bits(config.full_size bits))

    val out_valid = out(Bool)
    val out_ready = in(Bool)
    val result    = out(Bits(config.full_size bits))
  }

  val fp_op = new FpxxDivStreams_djj(config, FpxxDivConfig(pipeStages = 5))

  fp_op.io.input.valid := io.in_valid
  fp_op.io.input.payload.a.assignFromBits(io.op_a)
  fp_op.io.input.payload.b.assignFromBits(io.op_b)
  io.in_ready := fp_op.io.input.ready

  fp_op.io.output.ready := io.out_ready
  io.out_valid := fp_op.io.output.valid
  io.result := fp_op.io.output.payload.asBits
}

object FpxxDivStreamsTester extends App {

  val FileDir = "rtl/FpxxDivStreamsTester"
  new File(FileDir).mkdirs()

  val flag = VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )

  val Spinalcfg = SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    bitVectorWidthMax = 20000
  )

  case class Txn(a: Float, b: Float, expected: Float)

  case class CompareInfo(
      matches: Boolean,
      isBitExact: Boolean,
      mantDiff: Long,
      relError: Double,
      bothNaN: Boolean,
      bothInf: Boolean,
      expectedDenormActualZero: Boolean,
      expectedZeroActualZero: Boolean,
      expectedNormalActualNormal: Boolean
  )

  def safeRelError(expected: Float, actual: Float): Double = {
    if (expected.isNaN && actual.isNaN) 0.0
    else if (
      expected.isInfinite && actual.isInfinite &&
      java.lang.Float.floatToIntBits(expected) == java.lang.Float.floatToIntBits(actual)
    ) 0.0
    else if (expected == 0.0f && actual == 0.0f) 0.0
    else {
      val denom = math.max(math.abs(expected.toDouble), 1e-45)
      math.abs(actual.toDouble - expected.toDouble) / denom
    }
  }

  def analyzeResult(opA: Float, opB: Float, expected: Float, actual: Float, verbose: Boolean = false): CompareInfo = {
    val expectedBits = Fp32.asBits(expected)
    val actualBits   = Fp32.asBits(actual)

    val bothNaN = Fp32.isNaN(expected) && Fp32.isNaN(actual)
    val bothInf = Fp32.isInfinite(expected) && Fp32.isInfinite(actual)
    val expectedDenormActualZero = Fp32.isDenormal(expected) && Fp32.isZero(actual)
    val expectedZeroActualZero   = Fp32.isZero(expected) && Fp32.isZero(actual)

    val expectedNormalActualNormal =
      !Fp32.isNaN(expected) && !Fp32.isNaN(actual) &&
      !Fp32.isInfinite(expected) && !Fp32.isInfinite(actual) &&
      !Fp32.isZero(expected) && !Fp32.isZero(actual) &&
      !Fp32.isDenormal(expected) && !Fp32.isDenormal(actual)

    val mantDiff =
      if ((Fp32.exp(expected) == Fp32.exp(actual)) && (Fp32.sign(expected) == Fp32.sign(actual)))
        math.abs(Fp32.mant(expected) - Fp32.mant(actual)).toLong
      else Long.MaxValue

    val relError = safeRelError(expected, actual)

    var matches = false
    matches |= expectedDenormActualZero
    matches |= bothInf
    matches |= bothNaN
    matches |= (Fp32.exp(expected) == Fp32.exp(actual)) &&
      (Fp32.sign(expected) == Fp32.sign(actual)) &&
      (math.abs(Fp32.mant(expected) - Fp32.mant(actual)) < 3)

    matches |= (relError < (2.0 / (1 << 24)))

    val isBitExact = expectedBits == actualBits

    if (!matches) {
      printf("\n")
      printf("ERROR!\n")
      printAll(opA, opB, expected, actual)
      printf("Relative error: %e\n", relError)
    } else if (verbose) {
      printf("Match!\n")
      printAll(opA, opB, expected, actual)
      printf("Relative error: %e\n", relError)
    }

    CompareInfo(
      matches = matches,
      isBitExact = isBitExact,
      mantDiff = mantDiff,
      relError = relError,
      bothNaN = bothNaN,
      bothInf = bothInf,
      expectedDenormActualZero = expectedDenormActualZero,
      expectedZeroActualZero = expectedZeroActualZero,
      expectedNormalActualNormal = expectedNormalActualNormal
    )
  }

  val config = FpxxConfig(8, 23)

  val compiled = SimConfig
    .withVCS(flag)
    .withVcdWave
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .withConfig(Spinalcfg)
    .allOptimisation
    .compile(new FpxxDivStreamsDut(config))

  compiled.doSim { dut =>
    dut.clockDomain.forkStimulus(period = 10)
    dut.clockDomain.forkSimSpeedPrinter(0.2)

    dut.io.in_valid  #= false
    dut.io.op_a      #= 0
    dut.io.op_b      #= 0
    dut.io.out_ready #= false

    dut.clockDomain.waitSampling()

    val stimuli = FpxxTesterSupport.directedStimuli
    val rand = new scala.util.Random(0)
    val scoreboard = Queue[Txn]()

    var sent = 0
    var recv = 0
    var pass = 0
    var fail = 0
    @volatile var finished = false

    // ========= 精度统计 =========
    var bitExactCount = 0L

    var bothNaNCount = 0L
    var bothInfCount = 0L
    var expectedDenormActualZeroCount = 0L
    var expectedZeroActualZeroCount = 0L

    var expectedNaNCount = 0L
    var expectedInfCount = 0L
    var expectedZeroCount = 0L
    var expectedDenormCount = 0L
    var expectedNormalCount = 0L

    var actualNaNCount = 0L
    var actualInfCount = 0L
    var actualZeroCount = 0L
    var actualDenormCount = 0L
    var actualNormalCount = 0L

    var mantDiffEq0 = 0L
    var mantDiffEq1 = 0L
    var mantDiffEq2 = 0L
    var mantDiffGt2 = 0L
    var mantDiffUnavailable = 0L

    var maxRelError = 0.0
    var maxRelErrorA = 0.0f
    var maxRelErrorB = 0.0f
    var maxRelErrorExpected = 0.0f
    var maxRelErrorActual = 0.0f

    var maxMantDiff = Long.MinValue
    var maxMantDiffA = 0.0f
    var maxMantDiffB = 0.0f
    var maxMantDiffExpected = 0.0f
    var maxMantDiffActual = 0.0f

    // ========= 反压 / 吞吐统计 =========
    var readyHighCycles = 0L
    var readyLowCycles = 0L
    var outValidCycles = 0L
    var outputFireCycles = 0L
    var stallCyclesWhenValid = 0L
    var backpressureCycles = 0L
    var inputFireCycles = 0L

    // 小样本调试时可改为 20 / 200 / 5000
    val totalTests = scala.math.max(stimuli.size, 1000000)

    // 发送端
    fork {
      var i = 0
      while (i < totalTests) {
        val inputs =
          if (i < stimuli.size) stimuli(i)
          else (Fp32.randomRegular(rand), Fp32.randomRegular(rand))

        val op_a = inputs._1
        val op_b = inputs._2
        val result_exp = op_a / op_b

        val op_a_long: Long = Fp32.asBits(op_a)
        val op_b_long: Long = Fp32.asBits(op_b)

        dut.io.in_valid #= true
        dut.io.op_a #= op_a_long
        dut.io.op_b #= op_b_long

        var fired = false
        var timeout = 0
        while (!fired && timeout < 1000) {
          dut.clockDomain.waitSampling()
          fired = dut.io.in_valid.toBoolean && dut.io.in_ready.toBoolean
          if (fired) inputFireCycles += 1
          timeout += 1
        }

        if (!fired) {
          simFailure(s"Input handshake timeout at transaction $i")
        }

        scoreboard.enqueue(Txn(op_a, op_b, result_exp))
        sent += 1

        if (i % 1000 == 0) printf(".")

        dut.io.in_valid #= false
        dut.clockDomain.waitSampling()

        i += 1
      }
    }

    // 接收端 + 随机反压
    fork {
      var idleCycles = 0
      while (!finished) {
        // 75% ready, 25% backpressure
        val readyThisCycle = rand.nextInt(4) != 0
        dut.io.out_ready #= readyThisCycle

        dut.clockDomain.waitSampling()

        val ov = dut.io.out_valid.toBoolean
        val or = dut.io.out_ready.toBoolean
        val fire = ov && or

        if (or) readyHighCycles += 1 else readyLowCycles += 1
        if (ov) outValidCycles += 1
        if (!or) backpressureCycles += 1
        if (ov && !or) stallCyclesWhenValid += 1
        if (fire) outputFireCycles += 1

        if (fire) {
          if (scoreboard.isEmpty) {
            simFailure("Output fired but scoreboard is empty!")
          }

          val got = scoreboard.dequeue()
          val result_act = Fp32.asFloat(dut.io.result.toLong.toInt)

          // -------- expected 分类统计 --------
          if (Fp32.isNaN(got.expected)) expectedNaNCount += 1
          else if (Fp32.isInfinite(got.expected)) expectedInfCount += 1
          else if (Fp32.isZero(got.expected)) expectedZeroCount += 1
          else if (Fp32.isDenormal(got.expected)) expectedDenormCount += 1
          else expectedNormalCount += 1

          // -------- actual 分类统计 --------
          if (Fp32.isNaN(result_act)) actualNaNCount += 1
          else if (Fp32.isInfinite(result_act)) actualInfCount += 1
          else if (Fp32.isZero(result_act)) actualZeroCount += 1
          else if (Fp32.isDenormal(result_act)) actualDenormCount += 1
          else actualNormalCount += 1

          val info = analyzeResult(got.a, got.b, got.expected, result_act, verbose = false)

          if (info.isBitExact) bitExactCount += 1
          if (info.bothNaN) bothNaNCount += 1
          if (info.bothInf) bothInfCount += 1
          if (info.expectedDenormActualZero) expectedDenormActualZeroCount += 1
          if (info.expectedZeroActualZero) expectedZeroActualZeroCount += 1

          if (info.mantDiff == Long.MaxValue) {
            mantDiffUnavailable += 1
          } else if (info.mantDiff == 0) {
            mantDiffEq0 += 1
          } else if (info.mantDiff == 1) {
            mantDiffEq1 += 1
          } else if (info.mantDiff == 2) {
            mantDiffEq2 += 1
          } else {
            mantDiffGt2 += 1
          }

          if (!java.lang.Double.isNaN(info.relError) && !java.lang.Double.isInfinite(info.relError) && info.relError > maxRelError) {
            maxRelError = info.relError
            maxRelErrorA = got.a
            maxRelErrorB = got.b
            maxRelErrorExpected = got.expected
            maxRelErrorActual = result_act
          }

          if (info.mantDiff != Long.MaxValue && info.mantDiff > maxMantDiff) {
            maxMantDiff = info.mantDiff
            maxMantDiffA = got.a
            maxMantDiffB = got.b
            maxMantDiffExpected = got.expected
            maxMantDiffActual = result_act
          }

          if (info.matches) {
            pass += 1
          } else {
            fail += 1
            printf("%6d: %10e, %10e\n", recv, got.a, got.b)
            printf("Expected: %10e, Actual: %10e\n", got.expected, result_act)
            printf("--------\n")
            simFailure("ABORTING!")
          }

          recv += 1
          idleCycles = 0

          if (recv == totalTests) {
            finished = true

            val passRatio = pass.toDouble / totalTests.toDouble
            val bitExactRatio = bitExactCount.toDouble / totalTests.toDouble
            val readyHighRatio = readyHighCycles.toDouble / (readyHighCycles + readyLowCycles).toDouble
            val firePerValidRatio =
              if (outValidCycles == 0) 0.0 else outputFireCycles.toDouble / outValidCycles.toDouble

            printf("\n")
            printf("========== FpxxDivStreams FP32 Statistics (Random Backpressure) ==========\n")
            printf("PASS=%d FAIL=%d sent=%d recv=%d\n", pass, fail, sent, recv)
            printf("Pass ratio           : %.8f\n", passRatio)
            printf("Bit-exact count      : %d\n", bitExactCount)
            printf("Bit-exact ratio      : %.8f\n", bitExactRatio)
            printf("\n")

            printf("---- Mantissa diff distribution (same sign+exp only) ----\n")
            printf("mantDiff == 0        : %d\n", mantDiffEq0)
            printf("mantDiff == 1        : %d\n", mantDiffEq1)
            printf("mantDiff == 2        : %d\n", mantDiffEq2)
            printf("mantDiff  > 2        : %d\n", mantDiffGt2)
            printf("mantDiff unavailable : %d\n", mantDiffUnavailable)
            printf("\n")

            printf("---- Special / matching categories ----\n")
            printf("both NaN                     : %d\n", bothNaNCount)
            printf("both Inf                     : %d\n", bothInfCount)
            printf("expected denorm -> actual 0  : %d\n", expectedDenormActualZeroCount)
            printf("expected 0 -> actual 0       : %d\n", expectedZeroActualZeroCount)
            printf("\n")

            printf("---- Expected classification ----\n")
            printf("expected NaN       : %d\n", expectedNaNCount)
            printf("expected Inf       : %d\n", expectedInfCount)
            printf("expected Zero      : %d\n", expectedZeroCount)
            printf("expected Denorm    : %d\n", expectedDenormCount)
            printf("expected Normal    : %d\n", expectedNormalCount)
            printf("\n")

            printf("---- Actual classification ----\n")
            printf("actual NaN         : %d\n", actualNaNCount)
            printf("actual Inf         : %d\n", actualInfCount)
            printf("actual Zero        : %d\n", actualZeroCount)
            printf("actual Denorm      : %d\n", actualDenormCount)
            printf("actual Normal      : %d\n", actualNormalCount)
            printf("\n")

            printf("---- Worst relative error ----\n")
            printf("maxRelError        : %.12e\n", maxRelError)
            printf("at a=%e b=%e expected=%e actual=%e\n",
              maxRelErrorA, maxRelErrorB, maxRelErrorExpected, maxRelErrorActual)
            printf("\n")

            printf("---- Worst mantissa diff ----\n")
            if (maxMantDiff == Long.MinValue) {
              printf("No comparable normal case recorded.\n")
            } else {
              printf("maxMantDiff        : %d\n", maxMantDiff)
              printf("at a=%e b=%e expected=%e actual=%e\n",
                maxMantDiffA, maxMantDiffB, maxMantDiffExpected, maxMantDiffActual)
            }
            printf("\n")

            printf("---- Stream / backpressure statistics ----\n")
            printf("inputFireCycles     : %d\n", inputFireCycles)
            printf("outputFireCycles    : %d\n", outputFireCycles)
            printf("outValidCycles      : %d\n", outValidCycles)
            printf("readyHighCycles     : %d\n", readyHighCycles)
            printf("readyLowCycles      : %d\n", readyLowCycles)
            printf("backpressureCycles  : %d\n", backpressureCycles)
            printf("stallWhenValid      : %d\n", stallCyclesWhenValid)
            printf("readyHighRatio      : %.8f\n", readyHighRatio)
            printf("firePerValidRatio   : %.8f\n", firePerValidRatio)
            printf("==========================================================================\n")
          }
        } else {
          idleCycles += 1
          if (idleCycles > 1000000) {
            simFailure(s"Output timeout: sent=$sent recv=$recv queueSize=${scoreboard.size}")
          }
        }
      }
    }

    var guardCycles = 0
    while (!finished && guardCycles < 20000000) {
      dut.clockDomain.waitSampling()
      guardCycles += 1
    }

    if (!finished) {
      simFailure(s"Global timeout: sent=$sent recv=$recv queueSize=${scoreboard.size}")
    } else {
      simSuccess()
    }
  }
}

object FpxxDivStreamsFp16Tester extends App {

  val FileDir = "rtl/FpxxDivStreamsFp16Tester"
  new File(FileDir).mkdirs()

  val flag = VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )

  val Spinalcfg = SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    bitVectorWidthMax = 20000
  )

  val config = FpxxConfig.float16()

  object Fp16 {
    val EXP_BITS  = 5
    val MANT_BITS = 10
    val EXP_MASK  = 0x1F
    val MANT_MASK = 0x3FF

    def sign(bits: Int): Int = (bits >>> 15) & 1
    def exp(bits: Int): Int = (bits >>> 10) & EXP_MASK
    def mant(bits: Int): Int = bits & MANT_MASK

    def isNaN(bits: Int): Boolean = exp(bits) == 0x1F && mant(bits) != 0
    def isInfinite(bits: Int): Boolean = exp(bits) == 0x1F && mant(bits) == 0
    def isZero(bits: Int): Boolean = exp(bits) == 0 && mant(bits) == 0
    def isDenormal(bits: Int): Boolean = exp(bits) == 0 && mant(bits) != 0
    def isNormal(bits: Int): Boolean = exp(bits) != 0 && exp(bits) != 0x1F

    def halfToFloat(h: Int): Float = {
      val s = (h >>> 15) & 0x1
      val e = (h >>> 10) & 0x1F
      val m = h & 0x3FF

      val fbits =
        if (e == 0) {
          if (m == 0) {
            s << 31
          } else {
            var mm = m
            var ee = -14
            while ((mm & 0x400) == 0) {
              mm <<= 1
              ee -= 1
            }
            mm &= 0x3FF
            (s << 31) | ((ee + 127) << 23) | (mm << 13)
          }
        } else if (e == 0x1F) {
          (s << 31) | 0x7F800000 | (m << 13)
        } else {
          val ee = e - 15 + 127
          (s << 31) | (ee << 23) | (m << 13)
        }

      java.lang.Float.intBitsToFloat(fbits)
    }

    def floatToHalfBits(f: Float): Int = {
      val x = java.lang.Float.floatToRawIntBits(f)
      val sign = (x >>> 16) & 0x8000
      val exp32 = (x >>> 23) & 0xFF
      val mant32 = x & 0x7FFFFF

      if (exp32 == 0xFF) {
        if (mant32 == 0) sign | 0x7C00
        else sign | 0x7E00
      } else {
        val exp16 = exp32 - 127 + 15

        if (exp16 >= 0x1F) {
          sign | 0x7C00
        } else if (exp16 <= 0) {
          if (exp16 < -10) {
            sign
          } else {
            val mant = mant32 | 0x800000
            val shift = 14 - exp16
            var halfMant = mant >> shift
            val roundBit = (mant >> (shift - 1)) & 1
            val sticky = mant & ((1 << (shift - 1)) - 1)
            if (roundBit == 1 && (sticky != 0 || (halfMant & 1) == 1)) {
              halfMant += 1
            }
            sign | halfMant
          }
        } else {
          var halfExp = exp16
          var halfMant = mant32 >> 13
          val roundBits = mant32 & 0x1FFF
          if (roundBits > 0x1000 || (roundBits == 0x1000 && (halfMant & 1) == 1)) {
            halfMant += 1
            if (halfMant == 0x400) {
              halfMant = 0
              halfExp += 1
              if (halfExp >= 0x1F) return sign | 0x7C00
            }
          }
          sign | (halfExp << 10) | halfMant
        }
      }
    }

    def randomRegular(rand: scala.util.Random): Int = {
      val s = rand.nextInt(2)
      val e = 1 + rand.nextInt(30) // 1..30, avoid zero/subnormal and inf/nan
      val m = rand.nextInt(1 << MANT_BITS)
      (s << 15) | (e << 10) | m
    }

    def directed: Seq[(Int, Int)] = Seq(
      (0x3C00, 0x3C00), // 1 / 1
      (0x4000, 0x3C00), // 2 / 1
      (0x3C00, 0x4000), // 1 / 2
      (0xBC00, 0x3C00), // -1 / 1
      (0x3C00, 0xBC00), // 1 / -1
      (0x0000, 0x3C00), // 0 / 1
      (0x3C00, 0x0000), // 1 / 0 -> inf
      (0x0000, 0x0000), // 0 / 0 -> nan
      (0x7C00, 0x3C00), // inf / 1
      (0x3C00, 0x7C00), // 1 / inf -> 0
      (0x7C00, 0x7C00), // inf / inf -> nan
      (0x7E00, 0x3C00), // nan / 1
      (0x7BFF, 0x3C00), // max / 1
      (0x0400, 0x3C00)  // min normal / 1
    )
  }

  case class Txn(aBits: Int, bBits: Int, expectedBits: Int)

  def safeRelError(expected: Float, actual: Float): Double = {
    if (expected.isNaN && actual.isNaN) 0.0
    else if (expected.isInfinite && actual.isInfinite && expected.signum == actual.signum) 0.0
    else if (expected == 0.0f && actual == 0.0f) 0.0
    else {
      val denom = math.max(math.abs(expected.toDouble), 1e-30)
      math.abs(actual.toDouble - expected.toDouble) / denom
    }
  }

  def matches(expectedBits: Int, actualBits: Int): Boolean = {
    val expected = Fp16.halfToFloat(expectedBits)
    val actual   = Fp16.halfToFloat(actualBits)

    val bothNaN = Fp16.isNaN(expectedBits) && Fp16.isNaN(actualBits)
    val bothInf = Fp16.isInfinite(expectedBits) && Fp16.isInfinite(actualBits) &&
      Fp16.sign(expectedBits) == Fp16.sign(actualBits)

    val expectedDenormActualZero =
      Fp16.isDenormal(expectedBits) && Fp16.isZero(actualBits)

    val bothZero = Fp16.isZero(expectedBits) && Fp16.isZero(actualBits)

    val sameSignExp =
      Fp16.sign(expectedBits) == Fp16.sign(actualBits) &&
      Fp16.exp(expectedBits) == Fp16.exp(actualBits)

    val mantDiff =
      if (sameSignExp) math.abs(Fp16.mant(expectedBits) - Fp16.mant(actualBits))
      else Int.MaxValue

    val relError = safeRelError(expected, actual)

    bothNaN ||
    bothInf ||
    expectedDenormActualZero ||
    bothZero ||
    (sameSignExp && mantDiff < 3) ||
    (relError < (2.0 / (1 << 11)))
  }

  val compiled = SimConfig
    .withVCS(flag)
    .withVcdWave
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .withConfig(Spinalcfg)
    .allOptimisation
    .compile(new FpxxDivStreamsDut(config))

  compiled.doSim { dut =>
    dut.clockDomain.forkStimulus(period = 10)
    dut.clockDomain.forkSimSpeedPrinter(0.2)

    dut.io.in_valid  #= false
    dut.io.op_a      #= 0
    dut.io.op_b      #= 0
    dut.io.out_ready #= false
    dut.clockDomain.waitSampling()

    val rand = new scala.util.Random(1)
    val scoreboard = Queue[Txn]()

    var sent = 0
    var recv = 0
    var pass = 0
    var fail = 0
    var bitExact = 0L

    var inputFireCycles = 0L
    var outputFireCycles = 0L
    var outValidCycles = 0L
    var readyHighCycles = 0L
    var readyLowCycles = 0L
    var stallWhenValid = 0L
    var backpressureCycles = 0L

    @volatile var finished = false

    val totalTests = 200000

    fork {
      var i = 0
      while (i < totalTests) {
        val (aBits, bBits) =
          if (i < Fp16.directed.size) Fp16.directed(i)
          else (Fp16.randomRegular(rand), Fp16.randomRegular(rand))

        val a = Fp16.halfToFloat(aBits)
        val b = Fp16.halfToFloat(bBits)

        val expectedBits = Fp16.floatToHalfBits(a / b)

        dut.io.in_valid #= true
        dut.io.op_a #= aBits
        dut.io.op_b #= bBits

        var fired = false
        var timeout = 0
        while (!fired && timeout < 1000) {
          dut.clockDomain.waitSampling()
          fired = dut.io.in_valid.toBoolean && dut.io.in_ready.toBoolean
          if (fired) inputFireCycles += 1
          timeout += 1
        }

        if (!fired) simFailure(s"Input handshake timeout at transaction $i")

        scoreboard.enqueue(Txn(aBits, bBits, expectedBits))
        sent += 1

        if (i % 1000 == 0) printf(".")
        dut.io.in_valid #= false
        dut.clockDomain.waitSampling()

        i += 1
      }
    }

    fork {
      var idleCycles = 0

      while (!finished) {
        val readyThisCycle = rand.nextInt(4) != 0
        dut.io.out_ready #= readyThisCycle

        dut.clockDomain.waitSampling()

        val ov = dut.io.out_valid.toBoolean
        val or = dut.io.out_ready.toBoolean
        val fire = ov && or

        if (or) readyHighCycles += 1 else readyLowCycles += 1
        if (ov) outValidCycles += 1
        if (!or) backpressureCycles += 1
        if (ov && !or) stallWhenValid += 1
        if (fire) outputFireCycles += 1

        if (fire) {
          if (scoreboard.isEmpty) simFailure("Output fired but scoreboard is empty!")

          val got = scoreboard.dequeue()
          val actualBits = dut.io.result.toLong.toInt & 0xFFFF

          if (actualBits == got.expectedBits) bitExact += 1

          if (matches(got.expectedBits, actualBits)) {
            pass += 1
          } else {
            fail += 1

            val a = Fp16.halfToFloat(got.aBits)
            val b = Fp16.halfToFloat(got.bBits)
            val expected = Fp16.halfToFloat(got.expectedBits)
            val actual = Fp16.halfToFloat(actualBits)

            printf("\nERROR!\n")
            printf("aBits=0x%04x bBits=0x%04x\n", got.aBits, got.bBits)
            printf("a=%e b=%e\n", a, b)
            printf("expectedBits=0x%04x actualBits=0x%04x\n", got.expectedBits, actualBits)
            printf("expected=%e actual=%e\n", expected, actual)
            printf("relError=%e\n", safeRelError(expected, actual))

            simFailure("ABORTING!")
          }

          recv += 1
          idleCycles = 0

          if (recv == totalTests) {
            finished = true

            val readyHighRatio =
              readyHighCycles.toDouble / (readyHighCycles + readyLowCycles).toDouble
            val firePerValidRatio =
              if (outValidCycles == 0) 0.0 else outputFireCycles.toDouble / outValidCycles.toDouble

            printf("\n")
            printf("========== FpxxDivStreams FP16 Statistics (Random Backpressure) ==========\n")
            printf("PASS=%d FAIL=%d sent=%d recv=%d\n", pass, fail, sent, recv)
            printf("Pass ratio          : %.8f\n", pass.toDouble / totalTests.toDouble)
            printf("Bit-exact count     : %d\n", bitExact)
            printf("Bit-exact ratio     : %.8f\n", bitExact.toDouble / totalTests.toDouble)
            printf("\n")
            printf("---- Stream / backpressure statistics ----\n")
            printf("inputFireCycles     : %d\n", inputFireCycles)
            printf("outputFireCycles    : %d\n", outputFireCycles)
            printf("outValidCycles      : %d\n", outValidCycles)
            printf("readyHighCycles     : %d\n", readyHighCycles)
            printf("readyLowCycles      : %d\n", readyLowCycles)
            printf("backpressureCycles  : %d\n", backpressureCycles)
            printf("stallWhenValid      : %d\n", stallWhenValid)
            printf("readyHighRatio      : %.8f\n", readyHighRatio)
            printf("firePerValidRatio   : %.8f\n", firePerValidRatio)
            printf("==========================================================================\n")
          }
        } else {
          idleCycles += 1
          if (idleCycles > 1000000) {
            simFailure(s"Output timeout: sent=$sent recv=$recv queueSize=${scoreboard.size}")
          }
        }
      }
    }

    var guardCycles = 0
    while (!finished && guardCycles < 10000000) {
      dut.clockDomain.waitSampling()
      guardCycles += 1
    }

    if (!finished) {
      simFailure(s"Global timeout: sent=$sent recv=$recv queueSize=${scoreboard.size}")
    } else {
      simSuccess()
    }
  }
}


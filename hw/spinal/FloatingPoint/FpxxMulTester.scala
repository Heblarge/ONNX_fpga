package FloatingPoint

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import FpxxTesterSupport._
import spinal.sim.VCSFlags
import spinal.lib.sim.ScoreboardInOrder
import java.io.File
import spinal.lib.sim._
import scala.math
import scala.util.Random


object FpxxAMul_Gen {
  def main(args: Array[String]): Unit = {

    val config = FpxxConfig.float8_e4m3fnuz()

    SpinalConfig(
      targetDirectory = "rtl/FpxxMul",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(new FpxxMulCompatible(FpxxMul.Options(config)))
  }
}

case class FpxxMulDut(config: FpxxConfig) extends Component {
  val dut = FpxxMul(FpxxMul.Options(cIn = config, pipeStages = 1))

  val op = slave(Flow(Vec(cloneOf(dut.io.input.payload.a), 2)))
  dut.io.input << op.map { payload =>
    val bundle = cloneOf(dut.io.input.payload)
    bundle.a := payload(0)
    bundle.b := payload(1)

    bundle
  }

  val res = master(cloneOf(dut.io.result))
  res << dut.io.result
}

object FpxxMulTester extends App {

  val FileDir = "rtl/FpxxMulTester"
  new File(FileDir).mkdirs()

  def mulTest(
      config: FpxxConfig,
      testLines: Iterator[String]
  ) {
    val flag = VCSFlags(
      compileFlags = List("-kdb", "-lca", "+notimingchecks"),
      elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
      runFlags = List("-l ./run.log")
    )
    val Spinalcfg = SpinalConfig(
      targetDirectory = FileDir,
      oneFilePerComponent = true,
      // defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
      bitVectorWidthMax = 20000 // disable internal bigvector limitation"Way too big signal Bits"
    )
    val compiled = SimConfig
      .withVCS(flag)
      .withVcdWave
      .withTimeScale(1 ns)
      .withTimePrecision(1 ns)
      .withConfig(Spinalcfg)
      .allOptimisation
      .compile(FpxxMulDut(config))
      
      
      compiled.doSim { dut =>
        SimTimeout(100000)
        val stimuli = parseHexCases(testLines, 2, config, config, false)
          // Avoid smallest value since it may involve subnormal rounding
          .filter { a => !(a._2.mant == 0 && a._2.exp == 1) }
          // No denormals
          .filter { a => !a._2.isDenormal && !a._1.map(_.isDenormal).reduce(_ || _) }

        testOperation(stimuli, dut.op, dut.res, dut.clockDomain)
      }
  }


    mulTest(
      FpxxConfig.float16(),
      testfloatGen(Seq("f16_mul"))
    )
  


    mulTest(
      FpxxConfig.float32(),
      testfloatGen(Seq("f32_mul"))
    )
  
    val inConfig = FpxxConfig.float8_e5m2fnuz()
    val outConfig = FpxxConfig.bfloat16()

    // SimConfig.withVcdWave
    //   .compile(BundleDebug.fpxxDebugBits(new Module {
    //     val input = slave Flow (Vec(Fpxx(inConfig), 2))
    //     val result = master Flow (Fpxx(outConfig))

    //     val aConv = FpxxConverter(FpxxConverter.Options(inConfig, FpxxConfig(8, 2)))
    //     val bConv = FpxxConverter(FpxxConverter.Options(inConfig, FpxxConfig(8, 2)))
    //     aConv.io.a.payload := input.payload(0)
    //     aConv.io.a.valid := True
    //     bConv.io.a.payload := input.payload(1)
    //     bConv.io.a.valid := True

    //     val mult = FpxxMul(
    //       FpxxMul.Options(FpxxConfig(8, 2), Some(outConfig), pipeStages = 2, rounding = RoundType.FLOOR)
    //     )
    //     mult.io.input.payload.a := aConv.io.r
    //     mult.io.input.payload.b := bConv.io.r
    //     mult.io.input.valid := input.valid

    //     mult.io.result >> result
    //   }))
    //   .doSim { dut =>
    //     SimTimeout(10000000)
    //     import scala.sys.process._
    //     val lines =
    //       Process(Seq("python", "testgen.py", "float8_e5m2fnuz", "bfloat16", "mul")).lineStream.iterator

    //     testOperation(
    //       parseHexCases(lines, 2, FpxxConfig.float8_e5m2fnuz(), FpxxConfig.bfloat16()),
    //       dut.input,
    //       dut.result,
    //       dut.clockDomain
    //     )
    //   }
  }

case class FpxxMulCompatibleDut(config: FpxxConfig) extends Component {
  // 实例化兼容版乘法器
  val dut = new FpxxMulCompatible(FpxxMul.Options(cIn = config, pipeStages = 1))

  // 适配 Flow 接口
  val op = slave(Flow(Vec(cloneOf(dut.io.input.payload.a), 2)))
  dut.io.input.valid := op.valid
  dut.io.input.payload.a := op.payload(0)
  dut.io.input.payload.b := op.payload(1)

  // 输出结果
  val res = master(cloneOf(dut.io.result))
  res << dut.io.result

  // --- 辅助观测区域 (Debug Probes) ---
  // 将浮点数转为定点数，方便在 Waveform 中直接看数值验证乘法结果
  // 例如 E4M3 Max=240, 使用 Q16.16 足够覆盖
  val debugFix = new Area {
    val intBits  = 16 bits
    val fracBits = 16 bits

    // A, B, Result 的定点转换器
    val toFixA = new Fpxx2AFixCompatible(intBits, fracBits, config)
    toFixA.io.op.valid   := op.valid
    toFixA.io.op.payload := op.payload(0)

    val toFixB = new Fpxx2AFixCompatible(intBits, fracBits, config)
    toFixB.io.op.valid   := op.valid
    toFixB.io.op.payload := op.payload(1)

    val toFixRes = new Fpxx2AFixCompatible(intBits, fracBits, config)
    toFixRes.io.op.valid   := res.valid
    toFixRes.io.op.payload := res.payload

    // 暴露给波形的输出端口
    // 注意：使用 out() 暴露内部信号必须在顶层完成赋值
    val a_fix    = out(AFix.SQ(intBits, fracBits))
    val b_fix    = out(AFix.SQ(intBits, fracBits))
    val res_fix  = out(AFix.SQ(intBits, fracBits))
    val res_ovfl = out(Bool())

    a_fix    := toFixA.io.result.number
    b_fix    := toFixB.io.result.number
    res_fix  := toFixRes.io.result.number
    res_ovfl := toFixRes.io.result.overflow
  }
}

/**
 * 通用浮点乘法验证平台
 * 支持: E4M3FNUZ, E5M2FNUZ, IEEE Float16/32
 * 策略: 8-bit 格式进行 100% 全穷举覆盖 (65536 cases); >8-bit 格式进行大规模随机覆盖
 */
object FpxxMulCompatibleTester extends App {

  // ==========================================
  // 1. 配置切换
  // ==========================================
  // val activeConfig = FpxxConfig.float8_e4m3fnuz()
  //val activeConfig = FpxxConfig.float8_e5m2fnuz()
  val activeConfig = FpxxConfig.float16()

  // ==========================================
  // 2. 通用浮点算法工具
  // ==========================================
  class FloatAlgo(c: FpxxConfig) {
    val expBits  = c.exp_size
    val mantBits = c.mant_size
    val bias     = c.bias
    val isFNUZ   = c.inf_encoding.isInstanceOf[NoInfinity]
    val hasInf   = !isFNUZ

    val maxExpVal = (1 << expBits) - 1

    // 【补全定义】
    // FNUZ 的 MaxExp 是普通的数值; IEEE 的 MaxExp 是 Inf/NaN
    val maxLegalExp = if (isFNUZ) maxExpVal else maxExpVal - 1

    // 计算最大有限值 (Max Finite Value)
    val maxFiniteValue = {
      val mVal = (1 << mantBits) - 1
      math.pow(2, maxLegalExp - bias) * (1.0 + mVal.toDouble / (1 << mantBits).toDouble)
    }

    // 计算最小正规数 (Min Normal)
    val minNormalValue = math.pow(2, 1 - bias)

    // IEEE 溢出阈值
    val ieeeOverflowThreshold = if (hasInf) {
      val ulpAtMax = math.pow(2, maxLegalExp - bias - mantBits)
      maxFiniteValue + 0.5 * ulpAtMax
    } else Double.PositiveInfinity

    // LUT 结构
    case class Info(bits: Int, value: Double, isSubnormal: Boolean, isNaN: Boolean)

    val fullSize = 1 << c.full_size
    val lut: Array[Info] = Array.tabulate(fullSize) { i =>
      val sign = (i >> (expBits + mantBits)) & 0x1
      val exp  = (i >> mantBits) & ((1 << expBits) - 1)
      val mant = i & ((1 << mantBits) - 1)
      val s = if (sign == 1) -1.0 else 1.0

      if (isFNUZ) {
        if (sign == 1 && exp == 0 && mant == 0) Info(i, Double.NaN, false, true)
        else if (exp == 0 && mant == 0) Info(i, 0.0, false, false)
        else if (exp == 0) {
          val valSub = s * math.pow(2, 1 - bias) * (mant.toDouble / (1 << mantBits).toDouble)
          Info(i, valSub, true, false)
        } else {
          val valNorm = s * math.pow(2, exp - bias) * (1.0 + mant.toDouble / (1 << mantBits).toDouble)
          Info(i, valNorm, false, false)
        }
      } else {
        if (exp == maxExpVal && mant != 0) Info(i, Double.NaN, false, true)
        else if (exp == maxExpVal) Info(i, if(sign==1) Double.NegativeInfinity else Double.PositiveInfinity, false, false)
        else if (exp == 0 && mant == 0) Info(i, 0.0, false, false)
        else if (exp == 0) {
          val valSub = s * math.pow(2, 1 - bias) * (mant.toDouble / (1 << mantBits).toDouble)
          Info(i, valSub, true, false)
        } else {
          val valNorm = s * math.pow(2, exp - bias) * (1.0 + mant.toDouble / (1 << mantBits).toDouble)
          Info(i, valNorm, false, false)
        }
      }
    }

    // Double -> Bits
    def toBits(d: Double): Int = {
      if (d.isNaN) return if (isFNUZ) 1 << (expBits + mantBits) else ((1 << expBits) - 1) << mantBits | 1
      if (hasInf) {
        if (d.isPosInfinity || d >= ieeeOverflowThreshold) return ((1 << expBits) - 1) << mantBits
        if (d.isNegInfinity || d <= -ieeeOverflowThreshold) return 1 << (expBits + mantBits) | (((1 << expBits) - 1) << mantBits)
      }
      val validCands = lut.filter(x => !x.isNaN && !x.value.isInfinite)
      val minDst = validCands.map(c => math.abs(c.value - d)).min
      val ties = validCands.filter(c => math.abs(math.abs(c.value - d) - minDst) < 1e-12)
      if (ties.size == 1) ties.head.bits else ties.find(c => (c.bits & 1) == 0).getOrElse(ties.head).bits
    }

    def toHost(d: Double): FpxxHost = FpxxHost(BigInt(toBits(d)), c)
    def fromBits(b: Int): Double = lut(b & (fullSize - 1)).value

    def unpack(bits: Int): (Int, Int, Int) = {
      val sign = (bits >> (expBits + mantBits)) & 0x1
      val exp  = (bits >> mantBits) & ((1 << expBits) - 1)
      val mant = bits & ((1 << mantBits) - 1)
      (sign, exp, mant)
    }

    // =========================================================
    // 位级精确硬件模拟 (Bit-Exact Hardware Simulation)
    // =========================================================
    def hardwareSimMul(valA: Double, valB: Double): Double = {
      val bitsA = toBits(valA)
      val bitsB = toBits(valB)
      val (sA, eA, mA) = unpack(bitsA)
      val (sB, eB, mB) = unpack(bitsB)

      // 1. NaN / Inf / Zero Detection
      val a_is_nan = if (isFNUZ) (bitsA == (1 << (expBits+mantBits))) else (eA == maxExpVal && mA != 0)
      val b_is_nan = if (isFNUZ) (bitsB == (1 << (expBits+mantBits))) else (eB == maxExpVal && mB != 0)
      val a_is_inf = if (isFNUZ) false else (eA == maxExpVal && mA == 0)
      val b_is_inf = if (isFNUZ) false else (eB == maxExpVal && mB == 0)
      val a_is_zero_or_sub = (eA == 0)
      val b_is_zero_or_sub = (eB == 0)

      val is_nan_op = a_is_nan || b_is_nan || (a_is_zero_or_sub && b_is_inf) || (b_is_zero_or_sub && a_is_inf)
      if (is_nan_op) return if (isFNUZ) fromBits(1 << (expBits + mantBits)) else Double.NaN

      if (a_is_inf || b_is_inf) {
        val sign = sA ^ sB
        if (sign == 1) return Double.NegativeInfinity else return Double.PositiveInfinity
      }

      if (a_is_zero_or_sub || b_is_zero_or_sub) {
        val sign = sA ^ sB
        if (isFNUZ) return 0.0
        return if (sign == 1) -0.0 else 0.0
      }

      // 2. Core Math
      // n1.exp_mul = (a.exp + b.exp) - bias
      val exp_mul_temp = eA + eB - bias

      // n1.mant_mul
      val mantA_full = (1 << mantBits) | mA
      val mantB_full = (1 << mantBits) | mB
      val mant_mul = mantA_full.toLong * mantB_full.toLong

      // 3. Rounding Logic
      val mul_width = (mantBits + 1) * 2
      val msb_idx = mul_width - 1
      // Check normalization (product >= 2.0 ?)
      val msb = (mant_mul >> (mul_width - 1)) & 1

      val shift_amount = mantBits + msb.toInt
      val round_pos = shift_amount - 1
      val round_bit = (mant_mul >> round_pos) & 1
      val sticky_bit = if ((mant_mul & ((1L << round_pos) - 1)) != 0) 1 else 0

      val mant_truncated = (mant_mul >> shift_amount)
      val lsb_bit = mant_truncated & 1

      // Round to Nearest Even
      var round_carry = 0L
      if (round_bit == 1 && (sticky_bit == 1 || lsb_bit == 1)) {
        round_carry = 1
      }

      val mant_rounded_full = mant_truncated + round_carry

      // Check if rounding caused overflow (e.g. 1.11...1 -> 10.00...0)
      val mant_rounded_msb = (mant_rounded_full >> (mantBits + 1)) & 1

      // 4. Final Exponent
      val exp_adj = exp_mul_temp + msb + mant_rounded_msb.toInt

      // 5. Output FTZ (Hardware Behavior)
      if (exp_adj <= 0) {
        return if (isFNUZ) 0.0 else (if ((sA^sB)==1) -0.0 else 0.0)
      }

      // 6. Overflow / Saturation
      val is_overflow = if (isFNUZ) (exp_adj > maxExpVal) else (exp_adj >= maxExpVal)

      if (is_overflow) {
        if (isFNUZ) {
          val bits = ((sA^sB) << (expBits+mantBits)) | (maxExpVal << mantBits) | ((1<<mantBits)-1)
          return fromBits(bits)
        } else {
          return if ((sA^sB)==1) Double.NegativeInfinity else Double.PositiveInfinity
        }
      }

      // 7. Assemble
      val final_mant = mant_rounded_full & ((1 << mantBits) - 1)
      val final_exp = exp_adj

      val final_bits = ((sA^sB) << (expBits+mantBits)) | (final_exp.toInt << mantBits) | final_mant.toInt
      fromBits(final_bits)
    }
  }

  val algo = new FloatAlgo(activeConfig)
  println(s"Testing Config: ${activeConfig.exp_size}E${activeConfig.mant_size}M, Bias=${activeConfig.bias}, FNUZ=${algo.isFNUZ}")

  // Wrapper
  def goldenMul(a: Double, b: Double): Double = algo.hardwareSimMul(a, b)

  // ==========================================
  // 4. 测试生成
  // ==========================================
  var testCases = scala.collection.mutable.ArrayBuffer[(Double, Double)]()
  val totalBits = activeConfig.full_size

  if (totalBits <= 8) {
    println(s"Mode: EXHAUSTIVE (${1 << totalBits} * ${1 << totalBits} cases)")
    val maxVal = 1 << totalBits
    for (i <- 0 until maxVal; j <- 0 until maxVal) {
      testCases += ((algo.fromBits(i), algo.fromBits(j)))
    }
  } else {
    println(s"Mode: RANDOMIZED + CORNERS")
    val corners = Seq(0.0, -0.0, 1.0, -1.0, algo.maxFiniteValue, -algo.maxFiniteValue, Double.NaN)
    for (c1 <- corners; c2 <- corners) testCases += ((c1, c2))
    for (_ <- 0 until 200000) {
      val range = if (Random.nextBoolean()) algo.maxFiniteValue else 1.0
      val a = (Random.nextDouble() - 0.5) * 2 * range
      val b = (Random.nextDouble() - 0.5) * 2 * range
      testCases += ((a, b))
    }
  }

  val FileDir = "rtl/FpxxMulCompatibleTester"
  new File(FileDir).mkdirs()
  val flag = VCSFlags(compileFlags = List("-kdb","-lca", "+notimingchecks"), elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"), runFlags = List("-l ./run.log"))
  val Spinalcfg = SpinalConfig(targetDirectory = FileDir, oneFilePerComponent = true, bitVectorWidthMax = 20000)
  val compiled = SimConfig.withVCS(flag).withFsdbWave.withTimeScale(1 ns).withTimePrecision(1 ns).withConfig(Spinalcfg).allOptimisation.compile(FpxxMulCompatibleDut(activeConfig))

  compiled.doSim { dut =>
    dut.clockDomain.forkStimulus(period = 10)
    SimTimeout(testCases.length * 20L + 5000)
    val scoreboard = ScoreboardInOrder[Double]
    val caseIter = testCases.iterator
    var processedCount = 0

    FlowDriver(dut.op, dut.clockDomain) { payload =>
      if (caseIter.hasNext) {
        val (rawA, rawB) = caseIter.next()
        val hostA = algo.toHost(rawA)
        val hostB = algo.toHost(rawB)
        payload(0) #= hostA
        payload(1) #= hostB

        val qa = algo.fromBits(hostA.value.toInt)
        val qb = algo.fromBits(hostB.value.toInt)
        val ref = goldenMul(qa, qb)

        if (ref.isNaN) scoreboard.pushRef(Double.NaN)
        else scoreboard.pushRef(ref)

        processedCount += 1
        if (processedCount % 50000 == 0) println(s"Processed $processedCount / ${testCases.length}")
        true
      } else false
    }

    FlowMonitor(dut.res, dut.clockDomain) { payload =>
      val sign = payload.sign.toBigInt
      val exp  = payload.exp.toBigInt
      val mant = payload.mant.toBigInt
      val hwBits = ((sign << (activeConfig.exp_size + activeConfig.mant_size)) | (exp << activeConfig.mant_size) | mant).toInt
      val hwVal = algo.fromBits(hwBits)

      if (scoreboard.ref.nonEmpty) {
        val refVal = scoreboard.ref.dequeue()
        var pass = false

        if (refVal.isNaN) {
          if (hwVal.isNaN) pass = true
          else println(s"[FAIL] Expected NaN, got $hwVal (Bits: 0x${hwBits.toHexString})")
        } else if (refVal.isInfinite) {
          if (hwVal.isInfinite && refVal.signum == hwVal.signum) pass = true
          else println(s"[FAIL] Expected ${refVal}, got $hwVal")
        } else {
          if (hwVal == refVal) pass = true
          else {
            if (refVal == 0.0 && hwVal == 0.0) pass = true
            else {
              println(s"\n[FAIL] Mismatch detected!")
              println(s"  Ref (Golden) : $refVal (0x${algo.toBits(refVal).toHexString})")
              println(s"  HW  (DUT)    : $hwVal (0x${hwBits.toHexString})")
              println(s"  Raw HW Parts : S=$sign E=$exp M=$mant")
            }
          }
        }
        if (!pass) simFailure("Mismatch detected!")
      }
    }

    dut.clockDomain.forkStimulus(2)
    dut.clockDomain.waitActiveEdgeWhere(!caseIter.hasNext && scoreboard.ref.isEmpty)
    dut.clockDomain.waitActiveEdge(20)
    println(s"SUCCESS! Passed all ${testCases.length} vectors.")
  }
}
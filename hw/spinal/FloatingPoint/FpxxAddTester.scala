package FloatingPoint

import spinal.core._
import spinal.core.formal._
import spinal.core.sim._
import spinal.lib._
import FpxxTesterSupport._
import spinal.sim.VCSFlags
import spinal.lib.sim.ScoreboardInOrder
import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import scala.math
import scala.util.Random


object FpxxAdd_Gen {
  def main(args: Array[String]): Unit = {

    val config = FpxxConfig.float8_e5m2fnuz()

    SpinalConfig(
      targetDirectory = "rtl/FpxxAdd",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(new FpxxAddCompatible(FpxxAdd.Options(config)))
  }
}

case class FpxxAddDut(config: FpxxConfig) extends Component {
  val op     = slave Flow (Vec(Fpxx(config), 2))
  val result = master Flow (Fpxx(config))

  val inner = new FpxxAdd(FpxxAdd.Options(config))
  inner.io.op.valid := op.valid
  inner.io.op.a     := op.payload(0)
  inner.io.op.b     := op.payload(1)

  val addDelay = LatencyAnalysis(inner.io.op.valid, inner.io.result.valid)

  val equiv = new Area {
    val toFix = List.tabulate(2) { i =>
      val conv = new Fpxx2AFix(
        (result.payload.exp.maxValue - config.bias + 3) bits,
        config.bias + config.mant_size bits,
        config,
        if (addDelay > 2) 1 else 0,
        true
      )
      conv.io.op << op.map(_(i))
      conv.io.result
    }

    val sum = toFix.reduce { (a, b) =>
      val next = cloneOf(a)
      next.valid      := a.valid && b.valid
      next.number     := (a.number + b.number).truncated
      next.flags.inf  := a.flags.inf || b.flags.inf
      next.flags.nan  := a.flags.nan || b.flags.nan
      next.flags.sign := a.flags.sign || b.flags.sign
      next
    }

    val toFpxx = new AFix2Fpxx(
      result.payload.exp.maxValue - config.bias + 3 bits,
      config.bias + config.mant_size bits,
      config,
      scala.math.min(addDelay, 2),
      true
    )

    toFpxx.io.op.assignAllByName(sum)

    val fixedDelay = LatencyAnalysis(op.valid, toFpxx.io.result.valid)

    val fixRes = Delay(toFpxx.io.result, addDelay - fixedDelay)

    when(pastValidAfterReset()) {
      assert(fixRes.valid === inner.io.result.valid, "Valid should be equal")
      when(fixRes.valid) {
        assert(
          fixRes.payload === inner.io.result.payload ||
            (fixRes.is_nan() || fixRes.is_infinite()) && (inner.io.result.is_nan() || inner.io.result
              .is_infinite()) || fixRes.is_zero() && inner.io.result.is_zero(),
          "Fixed and adder outputs should match"
        )
      }
    }
  }

  result << inner.io.result
}


object FpxxAddTester extends App {

  val config = FpxxConfig.float16() //If you want float32, change this.
  val FileDir = "rtl/FpxxAddTester"
  new File(FileDir).mkdirs()
  //    val op     = slave Flow (Vec(Fpxx(config), 2))
  //    val result = master Flow (Fpxx(config))

  val flag = VCSFlags (
    compileFlags = List("-kdb","-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )

  val Spinalcfg=SpinalConfig (
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    //defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
    bitVectorWidthMax = 20000, //disable internal bigvector limitation"Way too big signal Bits"
  )

  val compiled = SimConfig
    .withVCS(flag)
    .withVcdWave
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .withConfig(Spinalcfg)
    .allOptimisation
    .compile(FpxxAddDut(config))

  compiled.doSim { dut =>
    SimTimeout(100000)
    val stimuli = parseHexCases(testfloatGen(Seq("f16_add")), 2, config, config, false)
      // No denormals
      .filter { a => !a._2.isDenormal && !a._1.map(_.isDenormal).reduce(_ || _) }
    testOperation(stimuli, dut.op, dut.result, dut.clockDomain)
  }
}

//     test("add float32") {
//         val config = FpxxConfig.float32()

//         SimConfig.withIVerilog.withWave.noOptimisation
//             .compile(BundleDebug.fpxxDebugBits(FpxxAddTester.FpxxAddDut(config)))
//             .doSim { dut =>
//                 SimTimeout(100000)
//                 val stimuli = parseHexCases(testfloatGen(Seq("f32_add")), 2, config, config, false)
//                     // No denormals
//                     .filter { a => !a._2.isDenormal && !a._1.map(_.isDenormal).reduce(_ || _) }
//                 testOperation(stimuli, dut.op, dut.result, dut.clockDomain)
//     }

// }

case class FpxxAddCompatibleDut(config: FpxxConfig) extends Component {
  val op     = slave Flow (Vec(Fpxx(config), 2))
  val result = master Flow (Fpxx(config))

  // 1. 实例化兼容版加法器
  // 注意：假设您已经把原来的 FpxxAdd 重命名为了 FpxxAddCompatible，如果没有改名，请改回 FpxxAdd
  val inner = new FpxxAddCompatible(FpxxAdd.Options(config))
  inner.io.op.valid := op.valid
  inner.io.op.a     := op.payload(0)
  inner.io.op.b     := op.payload(1)

  result << inner.io.result

  // 2. 辅助观测区域
  val debugFix = new Area {
    val intBits  = 16 bits
    val fracBits = 16 bits

    // --- 子模块实例化 ---
    val toFixA = new Fpxx2AFixCompatible(intBits, fracBits, config)
    toFixA.io.op.valid   := op.valid
    toFixA.io.op.payload := op.payload(0)

    val toFixB = new Fpxx2AFixCompatible(intBits, fracBits, config)
    toFixB.io.op.valid   := op.valid
    toFixB.io.op.payload := op.payload(1)

    val toFixRes = new Fpxx2AFixCompatible(intBits, fracBits, config)
    toFixRes.io.op.valid   := result.valid
    toFixRes.io.op.payload := result.payload

    // --- 【修复点】正确暴露信号 ---
    // 1. 先定义顶层输出端口
    val a_fix    = out(AFix.SQ(intBits, fracBits))
    val b_fix    = out(AFix.SQ(intBits, fracBits))
    val res_fix  = out(AFix.SQ(intBits, fracBits))
    val a_ovfl   = out(Bool())
    val res_ovfl = out(Bool())

    // 2. 再进行赋值连接
    a_fix    := toFixA.io.result.number
    b_fix    := toFixB.io.result.number
    res_fix  := toFixRes.io.result.number
    a_ovfl   := toFixA.io.result.overflow
    res_ovfl := toFixRes.io.result.overflow
  }
}

object FpxxAddCompatibleTester extends App {
  val vpiInclude = scala.sys.env.getOrElse("VCS_HOME", "")

  // ==========================================
  // 1. 配置：Float8 E4M3FNUZ (Bias=8, Max=240)
  // ==========================================
  val config = FpxxConfig.float8_e4m3fnuz()

  // ==========================================
  // 2. 辅助工具：Float8 转换器
  // ==========================================
  object Float8Util {
    case class F8Info(bits: Int, value: Double, isSubnormal: Boolean)

    val lut: Seq[F8Info] = (0 until 256).map { i =>
      val sign = (i >> 7) & 0x1
      val exp  = (i >> 3) & 0xF
      val mant = i & 0x7

      if (i == 0x80) {
        F8Info(i, Double.NaN, false)
      } else {
        val s = if (sign == 1) -1.0 else 1.0
        if (exp == 0) {
          val valSub = s * math.pow(2, -7) * (mant.toDouble / 8.0)
          F8Info(i, valSub, true)
        } else {
          val valNorm = s * math.pow(2, exp - 8) * (1.0 + mant.toDouble / 8.0)
          F8Info(i, valNorm, false)
        }
      }
    }

    // 【关键修复 1】实现严格的 Round to Nearest Even (Ties to Even)
    def toBits(d: Double): Int = {
      if (d.isNaN) return 0x80

      val validCands = lut.filter(_.bits != 0x80)

      // 1. 找到最小距离
      val minDst = validCands.map(c => math.abs(c.value - d)).min

      // 2. 找出所有距离等于最小距离的候选项 (处理 Tie)
      // 使用 epsilon 避免浮点比较误差
      val ties = validCands.filter(c => math.abs(math.abs(c.value - d) - minDst) < 1e-12)

      if (ties.size == 1) {
        ties.head.bits
      } else {
        // Tie-breaking: 选择偶数 (Mantissa LSB == 0)
        // E4M3 的 Mantissa 是低 3 位，所以 bit 0 为 0 即为偶数
        ties.find(c => (c.bits & 1) == 0).getOrElse(ties.head).bits
      }
    }

    def toHost(d: Double): FpxxHost = FpxxHost(BigInt(toBits(d)), config)
    def fromBits(b: BigInt): Double = lut.find(_.bits == b.toInt).get.value
    def isSubnormal(d: Double): Boolean = {
      if (d == 0.0 || d == -0.0 || d.isNaN) false
      else {
        val bits = toBits(d)
        lut.find(_.bits == bits).exists(_.isSubnormal)
      }
    }
  }

  // ==========================================
  // 3. 软件参考模型 (Golden Model)
  // ==========================================
  def goldenAdd(a: Double, b: Double): Double = {
    if (a.isNaN || b.isNaN) return Double.NaN

    // 模拟硬件 Flush-to-Zero
    val a_flushed = if (Float8Util.isSubnormal(a)) 0.0 else a
    val b_flushed = if (Float8Util.isSubnormal(b)) 0.0 else b

    var sum = a_flushed + b_flushed

    // 饱和处理 (Max = 240.0)
    val maxVal = 240.0
    if (sum > maxVal) sum = maxVal
    else if (sum < -maxVal) sum = -maxVal

    // 输出量化
    val bits = Float8Util.toBits(sum)
    Float8Util.fromBits(BigInt(bits))
  }

  // ==========================================
  // 4. 测试激励
  // ==========================================
  var testCases = scala.collection.mutable.ArrayBuffer[ (Double, Double) ](
    (0.0, 0.0), (1.0, 1.0), (1.0, -1.0),
    (240.0, 0.0), (240.0, 10.0), (-240.0, -20.0),
    (Double.NaN, 5.0), (0.5, 0.5), (0.0078125, 0.0078125)
  )
  for(_ <- 0 until 2000) {
    val a = (Random.nextDouble() - 0.5) * 600.0
    val b = (Random.nextDouble() - 0.5) * 600.0
    testCases += ((a, b))
  }

  // ==========================================
  // 5. 仿真配置
  // ==========================================
  val FileDir = "rtl/FpxxAddCompatibleTester"
  new File(FileDir).mkdirs()

  val flag = VCSFlags (
    compileFlags = List("-kdb","-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )

  val Spinalcfg = SpinalConfig (
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    bitVectorWidthMax = 20000,
  )

  val compiled = SimConfig
    .withVCS(flag)
    .withFsdbWave
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .withConfig(Spinalcfg)
    .allOptimisation
    .compile(FpxxAddCompatibleDut(config))

  // ==========================================
  // 6. 执行仿真
  // ==========================================
  compiled.doSim { dut =>
    dut.clockDomain.forkStimulus(period = 10)
    SimTimeout(1000000)

    val scoreboard = ScoreboardInOrder[Double]
    val caseIter = testCases.iterator

    FlowDriver(dut.op, dut.clockDomain) { payload =>
      if (caseIter.hasNext) {
        val (rawA, rawB) = caseIter.next()

        // 1. 先转成 Host 对象 (包含量化过程)
        val hostA = Float8Util.toHost(rawA)
        val hostB = Float8Util.toHost(rawB)

        // 2. 驱动硬件 (硬件看到的是量化后的值)
        payload(0) #= hostA
        payload(1) #= hostB

        // 3. 【关键修复 2】Golden Model 也必须使用量化后的值作为输入！
        // 这样才能保证 "Input -> Golden" 和 "Input -> HW" 的路径一致
        val quantA = Float8Util.fromBits(hostA.value)
        val quantB = Float8Util.fromBits(hostB.value)

        val ref = goldenAdd(quantA, quantB)

        if (ref.isNaN) scoreboard.pushRef(Double.NaN)
        else scoreboard.pushRef(ref)

        true
      } else false
    }

    FlowMonitor(dut.result, dut.clockDomain) { payload =>
      val sign = payload.sign.toBigInt
      val exp  = payload.exp.toBigInt
      val mant = payload.mant.toBigInt
      val hwBits = (sign << 7) | (exp << 3) | mant

      val hwVal  = Float8Util.fromBits(hwBits)

      if (scoreboard.ref.nonEmpty) {
        val refVal = scoreboard.ref.dequeue()
        var pass = false

        if (refVal.isNaN) {
          if (hwVal.isNaN) pass = true
          else println(s"[FAIL] Expected NaN, got $hwVal (Bits: 0x${hwBits.toString(16)})")
        } else {
          if (hwVal == refVal) pass = true
          else {
            println(s"\n[FAIL] Mismatch detected!")
            println(s"  Ref (Golden) : $refVal")
            println(s"  HW  (DUT)    : $hwVal")
            println(s"  HW Raw Bits  : S=$sign E=$exp M=$mant (0x${hwBits.toString(16)})")
          }
        }

        if (!pass) {
          simFailure("Mismatch detected!")
        }
      }
    }

    dut.clockDomain.forkStimulus(2)
    dut.clockDomain.waitActiveEdgeWhere(!caseIter.hasNext && scoreboard.ref.isEmpty)
    dut.clockDomain.waitActiveEdge(20)
    println("Simulation Passed!")
  }
}

object FpxxAddCompatibleCoveringTester extends App {

  // ==========================================
  // 1. 切换配置 (您可以随意切换这三行)
  // ==========================================
   val activeConfig = FpxxConfig.float8_e4m3fnuz()
  //val activeConfig = FpxxConfig.float8_e5m2fnuz()
  //val activeConfig = FpxxConfig.float16()

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

    // FNUZ 的 MaxExp 是普通的数值; IEEE 的 MaxExp 是 Inf/NaN
    val maxLegalExp = if (isFNUZ) maxExpVal else maxExpVal - 1

    // 计算最大有限值
    val maxFiniteValue = {
      val mVal = (1 << mantBits) - 1
      math.pow(2, maxLegalExp - bias) * (1.0 + mVal.toDouble / (1 << mantBits).toDouble)
    }

    // 计算最小正规数 (Min Normal)
    val minNormalValue = math.pow(2, 1 - bias)

    // IEEE 溢出阈值
    // 当数值超过这个界限时，IEEE 应该舍入为 Infinity，而不是 MaxFinite
    // Threshold = MaxFinite + 0.5 * ULP_at_Max
    // ULP_at_Max = 2^(maxLegalExp - bias - mantBits)
    val ieeeOverflowThreshold = if (hasInf) {
      val ulpAtMax = math.pow(2, maxLegalExp - bias - mantBits)
      maxFiniteValue + 0.5 * ulpAtMax
    } else Double.PositiveInfinity

    case class Info(bits: Int, value: Double, isSubnormal: Boolean, isNaN: Boolean, isInf: Boolean)

    val fullSize = 1 << c.full_size
    val lut: Array[Info] = Array.tabulate(fullSize) { i =>
      val sign = (i >> (expBits + mantBits)) & 0x1
      val exp  = (i >> mantBits) & ((1 << expBits) - 1)
      val mant = i & ((1 << mantBits) - 1)
      val s = if (sign == 1) -1.0 else 1.0

      if (isFNUZ) {
        // FNUZ Logic
        if (sign == 1 && exp == 0 && mant == 0) Info(i, Double.NaN, false, true, false)
        else if (exp == 0 && mant == 0) Info(i, 0.0, false, false, false)
        else if (exp == 0) {
          val valSub = s * math.pow(2, 1 - bias) * (mant.toDouble / (1 << mantBits).toDouble)
          Info(i, valSub, true, false, false)
        } else {
          val valNorm = s * math.pow(2, exp - bias) * (1.0 + mant.toDouble / (1 << mantBits).toDouble)
          Info(i, valNorm, false, false, false)
        }
      } else {
        // IEEE Logic
        if (exp == maxExpVal) {
          if (mant != 0) Info(i, Double.NaN, false, true, false)
          else Info(i, if(sign==1) Double.NegativeInfinity else Double.PositiveInfinity, false, false, true)
        } else if (exp == 0) {
          if (mant == 0) Info(i, 0.0, false, false, false)
          else {
            val valSub = s * math.pow(2, 1 - bias) * (mant.toDouble / (1 << mantBits).toDouble)
            Info(i, valSub, true, false, false)
          }
        } else {
          val valNorm = s * math.pow(2, exp - bias) * (1.0 + mant.toDouble / (1 << mantBits).toDouble)
          Info(i, valNorm, false, false, false)
        }
      }
    }

    def toBits(d: Double): Int = {
      if (d.isNaN) return if (isFNUZ) 1 << (expBits + mantBits) else ((1 << expBits) - 1) << mantBits | 1

      // IEEE 溢出判断
      // 如果是 IEEE 格式，且绝对值超过了阈值，直接返回 Infinity
      if (hasInf) {
        if (d.isPosInfinity || d >= ieeeOverflowThreshold) return ((1 << expBits) - 1) << mantBits
        if (d.isNegInfinity || d <= -ieeeOverflowThreshold) return 1 << (expBits + mantBits) | (((1 << expBits) - 1) << mantBits)
      }

      // 标准 Nearest Lookup (只会查找 Finite Values)
      val validCands = lut.filter(x => !x.isNaN && !x.isInf)
      val minDst = validCands.map(c => math.abs(c.value - d)).min
      val ties = validCands.filter(c => math.abs(math.abs(c.value - d) - minDst) < 1e-12)

      if (ties.size == 1) ties.head.bits
      else ties.find(c => (c.bits & 1) == 0).getOrElse(ties.head).bits
    }

    def toHost(d: Double): FpxxHost = FpxxHost(BigInt(toBits(d)), c)
    def fromBits(b: Int): Double = lut(b).value

    def isSubnormalOrZero(d: Double): Boolean = {
      if (d == 0.0 || d == -0.0) true
      else if (d.isNaN || d.isInfinite) false
      else lut(toBits(d)).isSubnormal
    }
  }

  val algo = new FloatAlgo(activeConfig)
  println(s"Testing Config: ${activeConfig.exp_size}E${activeConfig.mant_size}M, Bias=${activeConfig.bias}")
  println(s"Min Normal: ${algo.minNormalValue}, Max Finite: ${algo.maxFiniteValue}")
  if (!algo.isFNUZ) println(s"IEEE Overflow Threshold: ${algo.ieeeOverflowThreshold}")

  // ==========================================
  // 3. Golden Model
  // ==========================================
  def goldenAdd(a: Double, b: Double): Double = {
    if (a.isNaN || b.isNaN) return Double.NaN

    val a_in = if (algo.isSubnormalOrZero(a)) 0.0 else a
    val b_in = if (algo.isSubnormalOrZero(b)) 0.0 else b

    var sum = a_in + b_in

    // Output FTZ
    if (math.abs(sum) < algo.minNormalValue) {
      sum = 0.0
    }

    // FNUZ 饱和处理 (IEEE 不处理，交给 toBits 的溢出逻辑)
    if (algo.isFNUZ) {
      if (sum > algo.maxFiniteValue) sum = algo.maxFiniteValue
      else if (sum < -algo.maxFiniteValue) sum = -algo.maxFiniteValue
    }

    val bits = algo.toBits(sum)
    algo.fromBits(bits)
  }

  // ==========================================
  // 4. 测试用例生成
  // ==========================================
  var testCases = scala.collection.mutable.ArrayBuffer[(Double, Double)]()
  val totalBits = activeConfig.full_size

  if (totalBits <= 8) {
    println(s"Running EXHAUSTIVE verification (65536 cases).")
    val maxVal = 1 << totalBits
    for (i <- 0 until maxVal; j <- 0 until maxVal) {
      testCases += ((algo.fromBits(i), algo.fromBits(j)))
    }
  } else {
    println(s"Running RANDOMIZED verification.")
    val corners = Seq(0.0, -0.0, 1.0, -1.0, algo.maxFiniteValue, -algo.maxFiniteValue, Double.NaN)
    if (!algo.isFNUZ) {
      // Add Infinity cases for IEEE
      testCases += ((Double.PositiveInfinity, 1.0))
      testCases += ((Double.NegativeInfinity, -1.0))
      testCases += ((algo.maxFiniteValue, algo.maxFiniteValue)) // Trigger Overflow
    }
    for (c1 <- corners; c2 <- corners) testCases += ((c1, c2))
    for (_ <- 0 until 200000) {
      val range = if (Random.nextBoolean()) algo.maxFiniteValue else 1.0
      // 增加一些大数测试以触发溢出
      val mult = if (Random.nextInt(10) == 0) 2.0 else 1.0
      val a = (Random.nextDouble() - 0.5) * 2 * range * mult
      val b = (Random.nextDouble() - 0.5) * 2 * range * mult
      testCases += ((a, b))
    }
  }

  // ==========================================
  // 5. 仿真执行
  // ==========================================
  val FileDir = "rtl/FpxxAddCompatibleTester"
  new File(FileDir).mkdirs()
  val flag = VCSFlags(compileFlags = List("-kdb","-lca", "+notimingchecks"), elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"), runFlags = List("-l ./run.log"))
  val Spinalcfg = SpinalConfig(targetDirectory = FileDir, oneFilePerComponent = true, bitVectorWidthMax = 20000)
  val compiled = SimConfig.withVCS(flag).withFsdbWave.withTimeScale(1 ns).withTimePrecision(1 ns).withConfig(Spinalcfg).allOptimisation.compile(FpxxAddCompatibleDut(activeConfig))

  compiled.doSim { dut =>
    dut.clockDomain.forkStimulus(period = 10)
    SimTimeout(testCases.length * 20L + 2000)

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
        val ref = goldenAdd(qa, qb)

        if (ref.isNaN) scoreboard.pushRef(Double.NaN)
        else scoreboard.pushRef(ref)

        processedCount += 1
        if (processedCount % 10000 == 0) println(s"Processed $processedCount / ${testCases.length}")
        true
      } else false
    }

    FlowMonitor(dut.result, dut.clockDomain) { payload =>
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
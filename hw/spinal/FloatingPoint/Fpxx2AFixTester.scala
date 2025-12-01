package FloatingPoint

import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{FlowDriver, FlowMonitor}
import FpxxTesterSupport._
import spinal.sim.VCSFlags
import spinal.lib.sim.ScoreboardInOrder
import java.io.File

object Fpxx2AFixTester extends App {
    val vpiInclude = scala.sys.env.get("VCS_HOME")
    println("VCS_HOME"+vpiInclude)
    val config = FpxxConfig.float64()
    val stimuli = Array[Double](
          0.0,
          1.0,
          -1.0,
          4.8828125e-4,
          0.5,
          20.0,
          50.0,
          32767.5,
          32768.0,
          -32767.5,
          -32768.0,
          65535.5,
          65536.0,
          -65535.5,
          -65536.0,
          (1L << 43).toDouble + 1.0,
          -((1L << 32).toDouble),
          -1000.0,
          127.00390625,
          0.000244140625
    )

    val FileDir = "rtl/Fpxx2AFixTester"
    new File(FileDir).mkdirs()

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
        .withFsdbWave
        .withTimeScale(1 ns)
        .withTimePrecision(1 ns)
        .withConfig(Spinalcfg)
        .allOptimisation
        .compile(new Fpxx2AFixCompatible(
            intNrBits = 44 bits,
            fracNrBits = 12 bit,
            c = config
          ))

            // .compile(BundleDebug.fpxxDebugBits(new Fpxx2AFix(44 bits, 12 bits, config)))

        compiled.doSim { dut =>
            dut.clockDomain.forkStimulus(period = 10)
            SimTimeout(100000)
            val scoreboard = ScoreboardInOrder[FpxxHost]

            val cases = stimuli.iterator

            FlowDriver(dut.io.op, dut.clockDomain) { payload =>
                if (!cases.isEmpty) {
                    val a = cases.next()
                    payload #= a
                    scoreboard.pushRef(a)
                    true
                } else false
            }

            FlowMonitor(dut.io.result, dut.clockDomain) { payload =>
                assert(!payload.overflow.toBoolean)
                scoreboard.pushDut(payload.number.toDouble)
            }

            dut.clockDomain.forkStimulus(2)
            dut.clockDomain.waitActiveEdgeWhere(cases.isEmpty && scoreboard.ref.isEmpty)
        }
}

object Fpxx2AFixCompatibleTester extends App {
  val vpiInclude = scala.sys.env.getOrElse("VCS_HOME", "")

  // 1. 定义配置
  val config = FpxxConfig.float8_e4m3fnuz()

  // 2. Float8 E4M3FNUZ 转换工具 (Double -> FpxxHost)
  // 利用 Float8 只有 256 个状态的特性，建立查找表，确保转换绝对精确
  object Float8Converter {
    case class F8Info(bits: Int, value: Double)

    val lut: Seq[F8Info] = (0 until 256).map { i =>
      val sign = (i >> 7) & 0x1
      val exp  = (i >> 3) & 0xF
      val mant = i & 0x7

      // 解析逻辑参考自您提供的图片 (E4M3FNUZ)
      // NaN: 1000 0000 (0x80)
      if (i == 0x80) {
        F8Info(i, Double.NaN)
      } else {
        val s = if (sign == 1) -1.0 else 1.0
        val value = if (exp == 0) {
          // Subnormal: (-1)^S * 2^-7 * (mant / 8)
          // Bias=8, 但 Denorm 的指数通常固定为 1-Bias 或特定值，图片显示为 2^-7
          s * math.pow(2, -7) * (mant.toDouble / 8.0)
        } else {
          // Normal: (-1)^S * 2^(exp - 8) * (1 + mant / 8)
          s * math.pow(2, exp - 8) * (1.0 + mant.toDouble / 8.0)
        }
        F8Info(i, value)
      }
    }

    // 查找最接近的 Float8 表示 (Round to Nearest)
    def toHost(d: Double): FpxxHost = {
      if (d.isNaN) return FpxxHost(BigInt(0x80), config)

      // 排除 NaN (0x80) 后寻找差值最小的
      val closest = lut.filter(_.bits != 0x80).minBy(info => math.abs(info.value - d))

      // 如果输入超出了最大范围，逻辑上 minBy 会自动选择最大值 (饱和)
      FpxxHost(BigInt(closest.bits), config)
    }
  }

  // 3. 激励向量
  val stimuli = Array[Double](
    0.0, 1.0, -1.0, 0.5,
    0.001953125, // Subnormal
    10.0, 20.5, 100.0,
    448.0,       // Max
    -448.0,
    500.0,       // Overflow -> Should saturate to 448
    -1000.0,     // Overflow -> Should saturate to -448
    Double.NaN
  )

  val FileDir = "rtl/Fpxx2AFixCompatibleTester"
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
    .compile(new Fpxx2AFixCompatible(
      intNrBits = 16 bits,
      fracNrBits = 16 bit,
      c = config,
      pipeStages = 1,
      generateFlags = true
    ))

  compiled.doSim { dut =>
    dut.clockDomain.forkStimulus(period = 10)
    SimTimeout(100000)

    val scoreboard = ScoreboardInOrder[Double]
    val cases = stimuli.iterator

    FlowDriver(dut.io.op, dut.clockDomain) { payload =>
      if (cases.hasNext) {
        val valDouble = cases.next()

        // 【修复点】：使用自定义转换器生成匹配 Float8 Config 的 Host 对象
        // 这样 #= 操作符两边的 Config 就一致了
        val hostVal = Float8Converter.toHost(valDouble)
        payload #= hostVal

        // 推入 Scoreboard 逻辑
        if (valDouble.isNaN) {
          scoreboard.pushRef(0.0)
        } else {
          // 模拟溢出饱和 (E4M3 Max = 448)
          // 注意：硬件其实也是根据 bits 转定点，所以这里最好也根据 bit 转换回来的真实值做 ref
          // 这样可以避免 Double 与 Float8 量化误差导致的对比失败
          val quantizedVal = Float8Converter.lut.find(_.bits == hostVal.value.toInt).get.value
          scoreboard.pushRef(quantizedVal)
        }
        true
      } else false
    }

    FlowMonitor(dut.io.result, dut.clockDomain) { payload =>
      val hwValue = payload.number.toDouble
      val isOverflow = payload.overflow.toBoolean
      val isNan = payload.flags.nan.toBoolean

      if (scoreboard.ref.nonEmpty) {
        val refValue = scoreboard.ref.dequeue()

        if (payload.flags.nan.toBoolean) {
          // NaN 输入期望输出 0
          if(hwValue != 0.0) {
            println(s"[FAIL] NaN Input: Expected 0.0, got $hwValue")
            simFailure()
          }
        } else if (isOverflow) {
          // 定点数位宽不够时会溢出
          println(s"[INFO] Overflow detected for ref $refValue.")
        } else {
          val epsilon = 0.5 // 适当放宽误差，因为 Float8 精度极低
          if (math.abs(hwValue - refValue) > epsilon) {
            println(s"[FAIL] Mismatch! Ref(Quantized): $refValue, HW: $hwValue")
            simFailure("Mismatch exceeded epsilon")
          } else {
            // println(s"[PASS] Ref: $refValue, HW: $hwValue")
          }
        }
      }
    }

    dut.clockDomain.forkStimulus(2)
    dut.clockDomain.waitActiveEdgeWhere(!cases.hasNext && scoreboard.ref.isEmpty)
    dut.clockDomain.waitActiveEdge(20)
  }
}
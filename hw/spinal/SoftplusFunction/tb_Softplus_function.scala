package SoftplusFunction

import breeze.linalg.{DenseVector, linspace}

import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.sim.{FlowDriver, FlowMonitor, ScoreboardInOrder}

import scala.collection.mutable
import breeze.plot._
import scala.collection.mutable.ArrayBuffer

// Softplus表结构
case class SoftplusTables(P: Array[Int], N: Array[Int])

class Softplus_function_sw(cfg: Softplus_function_cfg) {
  import cfg._

  // 预计算P表和N表
  private val tables: SoftplusTables = generatePNTablesInts(cfg)

  // 生成P和N表的内部方法
  private def generatePNTablesInts(cfg: Softplus_function_cfg): SoftplusTables = {
    val K1 = cfg.K1; val K2 = cfg.K2; val K3 = cfg.K3
    val bit_frac = cfg.bit_frac
    val totalBits = cfg.total_bits
    val tMin = cfg.t_range._1
    val tMax = cfg.t_range._2
    val fullIdxMax = (1 << totalBits) - 1

    def alphamid(xh: Int, xm: Int, xl: Int): Double = {
      val idx = (xh << (K2 + K3)) | (xm << K3) | xl
      val t_mid = tMin + idx.toDouble / fullIdxMax.toDouble * (tMax - tMin)
      math.log1p(math.exp(t_mid))
    }

    val pLen = 1 << (K1 + K2)
    val P_table = Array.ofDim[Int](pLen)
    for (idx <- 0 until pLen) {
      val xh = idx >> K2
      val xm = idx & ((1 << K2) - 1)
      val spread = alphamid(xh, xm, 0) - alphamid(xh, xm, (1 << K3) - 1)
      val first = alphamid(xh, 0, 0) - alphamid(xh, 0, (1 << K3) - 1)
      val last  = alphamid(xh, (1 << K2) - 1, 0) - alphamid(xh, (1 << K2) - 1, (1 << K3) - 1)
      val avg_spread = (first + last) / 2.0
      val adjust = (avg_spread - spread) / 2.0
      val value = (alphamid(xh, xm, 0) + adjust) * (1 << bit_frac)
      P_table(idx) = value.toInt
    }

    val nLen = 1 << (K1 + K3)
    val N_table = Array.ofDim[Int](nLen)
    for (idx <- 0 until nLen) {
      val xh = idx >> K3
      val xl = idx & ((1 << K3) - 1)
      val diff0 = alphamid(xh, 0, xl) - alphamid(xh, 0, 0)
      val diff1 = alphamid(xh, (1 << K2) - 1, xl) - alphamid(xh, (1 << K2) - 1, 0)
      val avgDiff = (diff0 + diff1) / 2.0
      val value = avgDiff * (1 << bit_frac)
      N_table(idx) = value.toInt
    }

    SoftplusTables(P_table, N_table)
  }

  // 核心计算函数（返回定点数结果）
  def compute(payloadInt: Int): Int = {
    val K1 = cfg.K1; val K2 = cfg.K2; val K3 = cfg.K3
    val totalBits = cfg.total_bits
    val bit_frac = cfg.bit_frac
    val tMin = cfg.t_range._1
    val tMax = cfg.t_range._2

    // 输入范围检查断言
    val scale_factor = 1 << bit_frac
    val min_fixed = Math.round(x_in_Min * scale_factor).toInt
    val max_fixed = Math.round(x_in_Max * scale_factor).toInt
    val x_float = payloadInt.toDouble / scale_factor
    assert(payloadInt >= min_fixed && payloadInt <= max_fixed,
      s"Softplus_function_sw.compute(payloadInt:Int):\n(x>=cfg.x_in_Min)&&(x<=cfg.x_in_Max) assert failed. " +
      s"Input: fixed-point x=$payloadInt (float: $x_float), " +
      s"float range: [$x_in_Min, $x_in_Max], " +
<<<<<<< HEAD
      s"fixed-point range: [$min_fixed, $max_fixed] (bit_frac=$bit_frac).")
=======
      s"fixed-point range: [$min_fixed, $max_fixed] (bit_frac=$bit_frac)."
    )
    val lowerBoundFixed = tMin << bit_frac
    val upperBoundFixed = tMax << bit_frac

    if (payloadInt < lowerBoundFixed) {
      return 0
    } else if (payloadInt > upperBoundFixed) {
      return payloadInt // 直接返回输入，模拟 ln(1+exp(x)) ≈ x
    }
>>>>>>> precise

    // scale_inv 与硬件 cfg.scale_inv 的整数计算 (以 Long 避免溢出)
    val scaleInvLong = (((1L << totalBits) - 1L) / (tMax - tMin)).toLong

    val tMinFixed = (tMin.toLong << bit_frac)        // t_min << bit_frac (Long)
    val numerator = (payloadInt.toLong - tMinFixed) * scaleInvLong // Long
    val idxLong = (numerator >> bit_frac)              // 与硬件的 >> bit_frac 保持一致 (算术右移)
    var idx = idxLong.toInt

    // clip 防止越界（硬件未必需要，但保证安全）
    val maxIdx = (1 << totalBits) - 1
    if (idx < 0) idx = 0
    else if (idx > maxIdx) idx = maxIdx

    val xh = idx >> (K2 + K3)
    val rem = idx - (xh << (K2 + K3))
    val xm = rem >> K3
    val xl = rem - (xm << K3)

    val pIdx = (xh << K2) | xm
    val nIdx = (xh << K3) | xl

    val P_raw = tables.P(pIdx)
    val N_raw = tables.N(nIdx)
    val sum = P_raw + N_raw
    sum // 整数形式，与硬件 payload (fixed-point) 对齐
  }

    // 提供浮点输入版本（可选）
  def compute(x: Double): Long = {
    val x_fixed = Math.round(x * (1 << bit_frac)).toInt
    val result_fixed = compute(x_fixed)
    result_fixed
  }
  // 提供浮点输出版本（可选）
  def computeFloat(x: Int): Double = {
    val result_fixed = compute(x)
    result_fixed.toDouble / (1 << bit_frac)
  }
  // 提供浮点输入版本（可选）
  def computeFloat(x: Double): Double = {
    val result_fixed = compute(x)
    result_fixed.toDouble / (1 << bit_frac)
  }

  // 获取表内容（用于调试和验证）
  def getPTable: Array[Int] = tables.P.clone()
  def getNTable: Array[Int] = tables.N.clone()
}

object SoftplusFunctionTest extends App {
  new File("rtl/Softplus_function/tb_softplus_function_report").mkdir()
  val cfg=Softplus_function_cfg(
    bit_int = 8,
    bit_frac = 12
  )
  val report = SpinalConfig(
    targetDirectory = "rtl/Softplus_function/tb_softplus_function_report",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(Softplus_function(cfg))
    .printPruned()
  val flags = VCSFlags(
    compileFlags = List(
      "-kdb","-lca", "+notimingchecks"
      //"-kdb -work xil_defaultlib",
      //"/home/cotr/Workspace/Xilinx_IP_lib/glbl.v",
    ),
    elaborateFlags = List(
      "-fgp", "-kdb", "-lca","+rad", "+notimingchecks"
      //      "-LDFLAGS -Wl,--no-as-needed",
      //      "-fgp",
      //      "-kdb",
      //      "-lca",
      //      "+rad",
      //      "+notimingchecks",
      //      "xil_defaultlib.glbl"
    ),
    runFlags = List("-l ./run.log")
  )
  val simCompiled = SimConfig
    .withVCS(flags)
    .withFsdbWave
    //.withVCSSimSetup(setupFile = "./synopsys_sim.setup", beforeAnalysis = null)
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .allOptimisation
    .withConfig(
      SpinalConfig(
        targetDirectory = "rtl",
        anonymSignalPrefix = "temp",
        oneFilePerComponent = false,
        defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
        nameWhenByFile = false,
        genLineComments = true
      )
    ).compile(report)


  simCompiled.doSim("softplus_tb"){dut =>

    dut.clockDomain.forkStimulus(2)
    dut.io.x.payload #= 0
    dut.io.x.valid #= false
    dut.clockDomain.waitSampling(5)


    val start = cfg.t_range._1*1024*4
    val end = cfg.t_range._2*1024*4 - 1
    val step = 32

    var x_iter = Stream.iterate(start)(_ + step).takeWhile(_ <= end).iterator

  val softplus_sw = new Softplus_function_sw(cfg)
  // 用于精确断言：记录输入 (double) 与硬件输出的整数值
  val inputData = ArrayBuffer[Double]()
  val outputDataDouble = ArrayBuffer[Double]()
  val outputDataInt = ArrayBuffer[Int]()

    val PostionThread = fork {
      while (x_iter.hasNext) {
        val x_value = x_iter.next()
        dut.io.x.valid #= true
        dut.io.x.payload #= x_value
        dut.clockDomain.waitSampling()

        // 输入以浮点形式保存（对应你原来的做法）
    inputData.append(x_value.toDouble / (1 << cfg.bit_frac))
      }
      dut.clockDomain.waitSampling(10)
    }
    // 捕获线程：同时保存整数输出和浮点显示用的值
val CaptureThread = fork {
  while (true) {
    dut.clockDomain.waitSampling()
    if (dut.io.softplusx.valid.toBoolean) {
      val yInt = dut.io.softplusx.payload.toInt
      outputDataInt.append(yInt)
      outputDataDouble.append(yInt.toDouble / (1 << cfg.bit_frac))
    }
  }
}

    PostionThread.join()
    CaptureThread.terminate()
    // 确保输入输出数据对齐
    //assert(inputData.length == outputData.length, "Input/Output data mismatch due to timing issues")
// 绝对/相对误差数组
val absErrors = Array.ofDim[Double](inputData.length)
val relErrors = Array.ofDim[Double](inputData.length)

    // 精确断言：对每个 input 计算硬件等价的整数结果并逐项比较
for (i <- 0 until inputData.length) {
  val x = inputData(i)
  val payloadInt = math.round(x * (1 << cfg.bit_frac)).toInt // 恢复到硬件使用的 payload int
  val expectedInt = softplus_sw.compute(payloadInt)
  val hwInt = outputDataInt(i)
  assert(hwInt == expectedInt,
    s"Mismatch @ idx=$i, x=$x, hwInt=$hwInt, expectedInt=$expectedInt, hwFloat=${hwInt.toDouble/(1<<cfg.bit_frac)}, expectedFloat=${expectedInt.toDouble/(1<<cfg.bit_frac)}")
// 误差计算（浮点值对比）
  val hwFloat = hwInt.toDouble / (1 << cfg.bit_frac)
  val expFloat = expectedInt.toDouble / (1 << cfg.bit_frac)

  absErrors(i) = (hwFloat - expFloat).abs
  relErrors(i) = if (expFloat.abs < 1e-9) 0.0 else (hwFloat - expFloat) / expFloat * 100.0
}


    // 绘制对比图
    val f = Figure()
    val p = f.subplot(0)
    val x_real = linspace(-16.0, 16.0, 100)
    val y_real = x_real.map(x => Math.log(1.0 + Math.exp(x)))
    p += plot(DenseVector(inputData.toArray), DenseVector(outputDataDouble.toArray.take(inputData.toArray.length)), style = '.')
    p += plot(x_real, y_real, name="Reference", colorcode="r")
    p.title = "Hardware vs Reference (softplus(x))"

    f.saveas("tb_Softplus_comparison.png")

    // 新增误差曲线绘制
    // 绝对误差
    val fAbs = Figure()
    val pAbs = fAbs.subplot(0)
    pAbs += plot(DenseVector(inputData.toArray), DenseVector(absErrors.map(_ *1e4).toArray), '.')
    pAbs.title = "Absolute Error(x 1e-4) (softplus(x))"
    pAbs.xlabel = "Input x"
    pAbs.ylabel = "|Reference - Hardware|(x 1e-4)"

    fAbs.saveas("tb_Softplus_absolute_error.png")

    // 相对误差（对数坐标）
    val fRel = Figure()
    val pRel = fRel.subplot(0)
    pRel += plot(DenseVector(inputData.toArray), DenseVector(relErrors.toArray), '.')
    pRel.title = "Relative Error (%) (softplus(x))"
    pRel.xlabel = "Input x"
    pRel.ylabel = "Error (%)"

    //pRel.yscale = breeze.plot.LogScale // 对数坐标显示小误差
    fRel.saveas("tb_Softplus_relative_error_log.png")


  }

}

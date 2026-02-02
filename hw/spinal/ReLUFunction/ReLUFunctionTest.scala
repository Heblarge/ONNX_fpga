package ReLUFunction

import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.sim._
import spinal.lib.sim.{FlowDriver, FlowMonitor, ScoreboardInOrder}
import scala.collection.mutable.ListBuffer
import breeze.plot._
import breeze.linalg._
import scala.collection.mutable


class ReLU_function_sw(cfg: ReLU_function_cfg) {
  import cfg._

  // ReLU函数的定点数实现
  def compute(x: Int): Int = {
    // ReLU函数：max(0, x)
    if (x < 0) 0 else x
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

}
object ReLUFunctionTest extends App {

  new File("rtl/Relu/tb_ReluFunction_report").mkdir()
  val cfg = ReLU_function_cfg(bit_int = 8, bit_frac = 12)
  val report = SpinalConfig(
    targetDirectory = "rtl/Relu/tb_ReluFunction_report",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(ReLU_function(cfg))
    .printPruned()
  val flags = VCSFlags(
    compileFlags = List(
      "-kdb", "-lca", "+notimingchecks"
    ),
    elaborateFlags = List(
      "-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"
    ),
    runFlags = List("-l ./run.log")
  )
  val simCompiled = SimConfig
    .withVCS(flags)
    .withFsdbWave
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

  // 生成测试数据
  def generateTestValues(start: Int, end: Int, step: Int): Seq[Int] = {
    Stream.iterate(start)(_ + step).takeWhile(_ <= end).toSeq
  }

  // 初始化队列用于存储输入、参考输出和结果
  val x_Queue = mutable.Queue[Int]()
  val relux_Queue = mutable.Queue[Double]()
  val display_x_Queue = mutable.Queue[Int]()
  val display_ref_Queue = mutable.Queue[Double]()
  val display_out_Queue = mutable.Queue[Int]()
  val display_abserror_Queue = mutable.Queue[Double]()
  val display_relerror_Queue = mutable.Queue[Double]()

  val relu_sw = new ReLU_function_sw(cfg)

  // 生成测试数据
  val start = -11 * 1024 * 4
  val end = 6 * 1024 * 4 - 1
  val step = 32
  val x_iter = generateTestValues(start, end, step).iterator

  // 填充测试数据到队列
  while (x_iter.hasNext) {
    val x = x_iter.next()
    x_Queue.enqueue(x)
    display_x_Queue.enqueue(x)
    val ref = relu_sw.computeFloat(x)
    relux_Queue.enqueue(ref)
    display_ref_Queue.enqueue(ref)
  }

  simCompiled.doSim("relu_tb") { dut =>
    SimTimeout(6000000)

    dut.clockDomain.forkStimulus(10)
    dut.io.x.payload #= 0
    dut.io.x.valid #= false
    dut.clockDomain.waitSampling(5)

    // 配置输入驱动
    val driverThread = fork {
      while (x_Queue.nonEmpty) {
        dut.io.x.valid #= true
        dut.io.x.payload #= x_Queue.dequeue()
        dut.clockDomain.waitSampling()
      }
      // 发送结束信号
      dut.io.x.valid #= false
      dut.clockDomain.waitSampling(10)
    }

    // 配置输出监控
    FlowMonitor(dut.io.relux, dut.clockDomain) { payload =>
      val received = payload.toInt
      val float_received = received.toDouble / (1024.0 * 4) // 转换回浮点数范围

      display_out_Queue.enqueue(received)

      if (relux_Queue.nonEmpty) {
        val ref = relux_Queue.dequeue()
        val float_ref = ref

        val absolute_error = Math.abs(float_received - float_ref)
        val relative_error = if (float_ref != 0) absolute_error / float_ref * 100 else 0.0

        display_abserror_Queue.enqueue(absolute_error)
        display_relerror_Queue.enqueue(relative_error)

        println(s"Input: ${display_x_Queue.head.toDouble/(1024.0*4)} -> HW: $float_received, SW: $float_ref")
        println(s"\tAbsolute error: $absolute_error")
        println(s"\tRelative error: ${relative_error}%")
      }
    }

    driverThread.join()
    dut.clockDomain.waitSampling(100) // 等待所有输出完成

    println("Simulation completed successfully!")
    simSuccess()
  }

  // ========== 结果可视化和分析 ==========

  // 确保数据长度一致
  assert(display_x_Queue.size == display_out_Queue.size,
    s"Input (${display_x_Queue.size}) and output (${display_out_Queue.size}) data size mismatch!")
  assert(display_x_Queue.size == display_ref_Queue.size,
    s"Input (${display_x_Queue.size}) and reference (${display_ref_Queue.size}) data size mismatch!")

  // 转换数据格式
  val display_x_array = display_x_Queue.toArray.map(_.toDouble / (1024.0 * 4))
  val display_ref_array = display_ref_Queue.toArray
  val display_out_array = display_out_Queue.toArray.map(_.toDouble / (1024.0 * 4))
  val display_abserror_array = display_abserror_Queue.toArray
  val display_relerror_array = display_relerror_Queue.toArray

  // 按 x 排序以获得更平滑的曲线
  val sortedPairs = display_x_array.zip(display_ref_array).zip(display_out_array)
    .zip(display_abserror_array).zip(display_relerror_array)
    .map { case ((((x, ref), out), abserr), relerr) => (x, ref, out, abserr, relerr) }
    .sortBy(_._1)

  // 提取排序后的数据
  val sorted_x_values = sortedPairs.map(_._1)
  val sorted_ref_values = sortedPairs.map(_._2)
  val sorted_out_values = sortedPairs.map(_._3)
  val sorted_abserror_values = sortedPairs.map(_._4)
  val sorted_relerror_values = sortedPairs.map(_._5)

  // 统计信息
  val maxAbsError = sorted_abserror_values.max
  val avgAbsError = sorted_abserror_values.sum / sorted_abserror_values.length
  val maxRelError = sorted_relerror_values.max
  val avgRelError = sorted_relerror_values.sum / sorted_relerror_values.length

  println("=" * 50)
  println("ReLU Hardware Test Results")
  println("=" * 50)
  println(s"Number of test points: ${sorted_x_values.length}")
  println(s"Input range: [${sorted_x_values.min}, ${sorted_x_values.max}]")
  println(s"Max Absolute Error: $maxAbsError")
  println(s"Average Absolute Error: $avgAbsError")
  println(s"Max Relative Error: ${maxRelError}%")
  println(s"Average Relative Error: ${avgRelError}%")

  // 断言检查
  val absErrorTolerance = 0.001
  val relErrorTolerance = 0.1

  assert(maxAbsError <= absErrorTolerance,
    s"Max absolute error $maxAbsError exceeds tolerance $absErrorTolerance")
  assert(maxRelError <= relErrorTolerance,
    s"Max relative error ${maxRelError}% exceeds tolerance ${relErrorTolerance}%")

  println("All assertions passed! ✓")

  // —— 绘制对比图 ——
  val fCompare = Figure()
  val pCompare = fCompare.subplot(0)
  pCompare += plot(sorted_x_values.toArray, sorted_ref_values.toArray, style = '.', name = "Software Reference")
  pCompare += plot(sorted_x_values.toArray, sorted_out_values.toArray, style = '.', name = "Hardware Output")
  pCompare.title = "Hardware vs Software ReLU Implementation"
  pCompare.xlabel = "Input x"
  pCompare.ylabel = "ReLU(x)"
  pCompare.legend = true
  fCompare.saveas("relu_comparison.png")

  // —— 绝对误差 ——
  val fAbs = Figure()
  val pAbs = fAbs.subplot(0)
  pAbs += plot(sorted_x_values.toArray, sorted_abserror_values.toArray, style = '.')
  pAbs.title = s"Absolute Error (Max: ${maxAbsError.formatted("%.6f")}, Avg: ${avgAbsError.formatted("%.6f")})"
  pAbs.xlabel = "Input x"
  pAbs.ylabel = "Absolute Error"
  fAbs.saveas("relu_absolute_error.png")

  // —— 相对误差（百分比） ——
  val fRel = Figure()
  val pRel = fRel.subplot(0)
  pRel += plot(sorted_x_values.toArray, sorted_relerror_values.toArray, style = '.')
  pRel.title = s"Relative Error (%) (Max: ${maxRelError.formatted("%.2f")}%, Avg: ${avgRelError.formatted("%.2f")}%)"
  pRel.xlabel = "Input x"
  pRel.ylabel = "Relative Error (%)"
  fRel.saveas("relu_relative_error.png")

  println("Visualization completed!")
  println("Generated files:")
  println("  - relu_comparison.png")
  println("  - relu_absolute_error.png")
  println("  - relu_relative_error.png")
}
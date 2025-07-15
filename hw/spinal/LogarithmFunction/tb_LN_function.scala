package LogarithmFunction// 包声明，指定当前代码所属的包
// 导入必要的库和模块
import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.sim.{FlowDriver, FlowMonitor, ScoreboardInOrder}
import scala.collection.mutable
import breeze.plot._

object sim_LN_function_test extends App {

  new File("rtl/LogFunction/sim_LN_function_test_report").mkdir()
  val flags = VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )
// 定义配置参数，设置整数位和小数位数
  val cfg = LN_function_cfg(
    bit_int = 8,
    bit_frac = 12
  )
// 生成Verilog代码并配置仿真参数
  val report = SpinalConfig(
    targetDirectory = "rtl/LogFunction/sim_LN_function_test_report",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(LN_function(cfg))
    .printPruned()

  val module_compiled = SimConfig
    .withVCS(flags)
    .withTimePrecision(1 ps)
    .withFSDBWave
    .withConfig(SpinalConfig(bitVectorWidthMax = 20000))
    .compile(report)
// 生成Q12格式的测试输入值
  def generateQ12Values(intRange: (Int, Int) = (0, 100), fracBits: Int = 12, skipZero: Boolean = false): Seq[Int] = {
    val fracVals = (0 until (1 << fracBits)).map(_ / Math.pow(2, fracBits))
    val intVals = (intRange._1 to intRange._2)
    val q12Vals = for {
      i <- intVals
      f <- fracVals
    } yield ((i + f) * Math.pow(2, fracBits)).toInt
    if (skipZero) {
      q12Vals.filter(_ > 0)
    } else {
      q12Vals
    }
  }
// 初始化随机数生成器
  val random = new scala.util.Random
  random.setSeed(1233)
  // 使用生成的Q12值作为输入数据源
  val x_iter = generateQ12Values(intRange = (1, 5), fracBits = cfg.bit_frac, skipZero = true).iterator
  // 固定点输入的浮点输出计算函数
  def lnx_fixIn_fix_out(x: Int): Int = {
    Math.floor(Math.log(x.toDouble / Math.pow(2, cfg.bit_frac)) * Math.pow(2, cfg.bit_frac)).toInt
  }


  def lnx_fixIn_fpOut(x: Int): Double = {
    val x_float = x.toDouble / Math.pow(2, cfg.bit_frac)
    if (x_float > 0) {
      val ln_x_float = Math.log(x_float)
      ln_x_float
    } else {
      Double.NaN
    }
  }
// 初始化队列用于存储输入、参考输出和结果
  val x_Queue = mutable.Queue[Int]()
  val lnx_Queue = mutable.Queue[Double]()
  val display_x_Queue = mutable.Queue[Int]()
  val display_ref_Queue = mutable.Queue[Double]()
  val display_out_Queue = mutable.Queue[Int]()
  val display_abserror_Queue = mutable.Queue[Double]()
  val display_relerror_Queue = mutable.Queue[Double]()
  // 填充测试数据到队列
  while (x_iter.hasNext) {
    val x = x_iter.next()
    x_Queue.enqueue(x)
    display_x_Queue.enqueue(x)
    val ref = lnx_fixIn_fpOut(x)
    lnx_Queue.enqueue(ref)
    display_ref_Queue.enqueue(ref)
  }
// 执行仿真
  module_compiled.doSim("ln_tb", seed = 1233) { dut =>
    SimTimeout(6000000)
    val scoreboard = ScoreboardInOrder[Int]()
    // 配置输入驱动
    FlowDriver(dut.io.x, dut.clockDomain) { payload =>
      if (x_Queue.nonEmpty) {
        payload #= x_Queue.dequeue()
        true
      } else {
        payload #= 0
        true
      }
    }
    // 配置输出监控
    FlowMonitor(dut.io.lnx, dut.clockDomain) { payload =>
      val received = payload.toInt
      val float_received = received.toDouble / Math.pow(2, cfg.bit_frac)
      println(s"out: ${received}\t:${float_received}")

      display_out_Queue.enqueue(received)

      if (lnx_Queue.nonEmpty) {
        val ref = lnx_Queue.dequeue()
        val float_ref = ref

        val absolute_error = Math.abs(float_received - float_ref)
        val relative_error = absolute_error / float_ref * 100
        display_abserror_Queue.enqueue(absolute_error)
        display_relerror_Queue.enqueue(relative_error)
        println(s"ref: ${float_ref}\n\tabsolute error: ${absolute_error}\n\trelative error: ${relative_error}%")
      } else {
        println("end")
        simSuccess()
      }
    }

    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.waitActiveEdgeWhere(false)
  }

  // 结果可视化
  val display_x_array = display_x_Queue.toArray.map(_.toDouble / Math.pow(2, cfg.bit_frac))
  val display_ref_array = display_ref_Queue.toArray//.map(_.toDouble / Math.pow(2, cfg.bit_frac))
  val display_out_array = display_out_Queue.toArray.map(_.toDouble / Math.pow(2, cfg.bit_frac))
  val display_abserror_array = display_abserror_Queue.toArray
  val display_relerror_array = display_relerror_Queue.toArray

  // 按 x 排序以获得更平滑的曲线
  val sortedPairs = display_x_array.zip(display_ref_array).zip(display_out_array).zip(display_abserror_array).zip(display_relerror_array)
    .map { case ((((x, ref), out), abserr), relerr) => (x, ref, out, abserr, relerr) }
    .sortBy(_._1)
  // 提取排序后的数据
  val sorted_x_values = sortedPairs.map(_._1)
  val sorted_ref_values = sortedPairs.map(_._2)
  val sorted_out_values = sortedPairs.map(_._3)
  val sorted_abserror_values = sortedPairs.map(_._4)
  val sorted_relerror_values = sortedPairs.map(_._5)

  // 确保输入数据类型一致，转换为 Array[Double]
  val sorted_x_values_double = sorted_x_values.map(_.toDouble)
  val sorted_ref_values_double = sorted_ref_values.map(_.toDouble)
  val sorted_out_values_double = sorted_out_values.map(_.toDouble)
  val sorted_abserror_values_double = sorted_abserror_values.map(_.toDouble)
  val sorted_relerror_values_double = sorted_relerror_values.map(_.toDouble)


  // —— 绘制对比图 ——
  val fCompare = Figure()
  val pCompare = fCompare.subplot(0)
  pCompare += plot(sorted_x_values_double, sorted_ref_values_double, style = '.', name = "Reference")
  pCompare += plot(sorted_x_values_double, sorted_out_values_double, style = '.', name = "Output")
  pCompare.title = "Hardware vs Reference (ln(x))"
  pCompare.xlabel = "x"
  pCompare.ylabel = "ln(x)"
  pCompare.legend = true
  fCompare.saveas("tb_LN_function_comparison.png")

  // —— 绝对误差 ——
  val fAbs = Figure()
  val pAbs = fAbs.subplot(0)
  pAbs += plot(sorted_x_values_double, sorted_abserror_values_double.map(_ *1e4), style = '.')
  pAbs.title = "Absolute Error(x 1e-4) (ln(x))"
  pAbs.xlabel = "x"
  pAbs.ylabel = "|Reference - Output|(x 1e-4)"
//  val minYA = sorted_abserror_values_double.min
//  val maxYA = sorted_abserror_values_double.max
//  pAbs.ylim(minYA, maxYA)
  fAbs.saveas("tb_LN_function_absolute_error.png")

  // —— 相对误差（百分比） ——
  val fRel = Figure()
  val pRel = fRel.subplot(0)
  val scaled_relerror_values_double = sorted_relerror_values_double.map(_ * 1e4)
  pRel += plot(sorted_x_values_double, sorted_relerror_values_double, style = '.')
  pRel.title = "Relative Error (%) (ln(x))"
  pRel.xlabel = "x"
  pRel.ylabel = "Error (%)"
//  val minYR = sorted_relerror_values_double.min
//  val maxYR = sorted_relerror_values_double.max
//  pRel.ylim(minYR, maxYR)
  fRel.saveas("tb_LN_function_relative_error.png")

}

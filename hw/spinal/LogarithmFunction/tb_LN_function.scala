package LogarithmFunction// 包声明，指定当前代码所属的包
// 导入必要的库和模块
import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.sim.{FlowDriver, FlowMonitor, ScoreboardInOrder}
import scala.collection.mutable
import breeze.plot._
import scala.math._

object sim_LN_function_test extends App {
def cordic_ln_ref(x: Int, cfg: LN_function_cfg): Double = {
    val scale_factor = 1 << cfg.bit_frac
    val log_2_int = math.round(log(2) * scale_factor).toInt

    if (x <= 0) return 0 // Assuming hardware handles invalid input gracefully

    // 规范化步骤：将x调整到[1, 2)范围
    var x_scaled: Int = x
    var k = 0
    while (x_scaled >= 2 * scale_factor) {
      x_scaled >>= 1
      k += 1
    }
    while (x_scaled < scale_factor) {
      x_scaled <<= 1
      k -= 1
    }

    // CORDIC atanh(2^-j) 值的定点表示 (Q12格式)
    val atanh_vals_fix = Array[Int](
      0, // Dummy
      (atanh(pow(2, -1)) * scale_factor).round.toInt, // j=1
      (atanh(pow(2, -2)) * scale_factor).round.toInt, // j=2
      (atanh(pow(2, -3)) * scale_factor).round.toInt, // j=3
      (atanh(pow(2, -4)) * scale_factor).round.toInt, // j=4
      (atanh(pow(2, -5)) * scale_factor).round.toInt, // j=5
      (atanh(pow(2, -6)) * scale_factor).round.toInt, // j=6
      (atanh(pow(2, -7)) * scale_factor).round.toInt, // j=7
      (atanh(pow(2, -8)) * scale_factor).round.toInt, // j=8
      (atanh(pow(2, -9)) * scale_factor).round.toInt, // j=9
      (atanh(pow(2, -10)) * scale_factor).round.toInt, // j=10
      (atanh(pow(2, -11)) * scale_factor).round.toInt, // j=11
      (atanh(pow(2, -12)) * scale_factor).round.toInt, // j=12
      (atanh(pow(2, -13)) * scale_factor).round.toInt, // j=13
      (atanh(pow(2, -14)) * scale_factor).round.toInt // j=14
    )
    
    // 初始化CORDIC变量（使用定点数）
    var x_n: Long = x_scaled.toLong + scale_factor.toLong
    var y_n: Long = x_scaled.toLong - scale_factor.toLong
    var z_n: Long = 0

    // CORDIC迭代
    for (j <- 1 to cfg.rotate) {
      val sign_y = if (y_n > 0) 1 else -1
      val atanh_val = atanh_vals_fix(j)
      
      val x_temp = x_n
      x_n = x_n - (sign_y * (y_n >> j))
      y_n = y_n - (sign_y * (x_temp >> j))
      z_n = z_n + sign_y * atanh_val

      // 补偿迭代
      if (cfg.using_compensation_iters && (j == 4 || j == 13)) {
        val sign_y_comp = if (y_n > 0) 1 else -1
        val x_temp_comp = x_n
        x_n = x_n - (sign_y_comp * (y_n >> j))
        y_n = y_n - (sign_y_comp * (x_temp_comp >> j))
        z_n = z_n + sign_y_comp * atanh_val
      }
    }
    
    // 最终结果计算（定点数）
    val result = (2 * z_n + k * log_2_int).toDouble/scale_factor
    result
  }
   
  // 辅助函数：计算atanh(x)
  def atanh(x: Double): Double = 0.5 * log((1 + x) / (1 - x))
  
  // 替换原来的简单实现
  def lnx_fixIn_fpOut(x: Int, cfg: LN_function_cfg): Double = {
    cordic_ln_ref(x, cfg)
  }

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


  // def lnx_fixIn_fpOut(x: Int): Double = {
  //   val x_float = x.toDouble / Math.pow(2, cfg.bit_frac)
  //   if (x_float > 0) {
  //     val ln_x_float = Math.log(x_float)
  //     ln_x_float
  //   } else {
  //     Double.NaN
  //   }
  // }
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
    val ref = lnx_fixIn_fpOut(x,cfg)
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

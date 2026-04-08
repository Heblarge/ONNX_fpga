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
import spire.std.int

// 定点数数学工具类
object FixedPointMath {
  def pow2(x: Int): Double = Math.pow(2, x)

  def floor(value: Long, bit_frac: Int): Long = {
    value / pow2(bit_frac).toLong
  }

  def sat(value: Long, max_int_bits: Int, bit_frac: Int): Long = {
    val max_val = pow2(max_int_bits + bit_frac).toLong - 1
    if (value > max_val) max_val else value
  }

  def clz(x: Int, width: Int): Int = {
    // 将x限制在指定位宽下，避免负数扩展影响
    val mask = if (width >= 31) -1 else ((1 << width) - 1)
    val v = x & mask
    if (v == 0) width
    else {
      // 使用内置bitLength近似：前导零 = width - bitLength
      val bl = 32 - Integer.numberOfLeadingZeros(v) // 有效位数
      width - bl
    }
  }

  // 辅助函数：计算atanh(x)
  //def atanh(x: Double): Double = 0.5 * log((1 + x) / (1 - x))

}

class LN_function_sw(cfg: LN_function_cfg) {
  import cfg._
  import FixedPointMath._
  import LN_function_cfg._


  // 使用泰勒展开近似的atanh
  val atanh_vals_fix: Array[Int] = {
    val scale_factor = 1 << bit_frac
    Array.tabulate(rotate + 1) { j =>
      if (j == 0) 0
      else {
        val v = atanh(Math.pow(2.0, -j)) * scale_factor
        v.toInt // 截断
      }
    }
  }
  val log2_fix: Int = {
    val scale_factor = 1 << bit_frac
    math.round(log(2) * scale_factor).toInt
  }
  private def normalize(x: Int): (Int, Int) = {
    val width     = bit_int + bit_frac
    val threshold = bit_int - 1
    val lz        = FixedPointMath.clz(x, width)
    if (lz < threshold) {
      val sh = (threshold - lz)
      // 逻辑右移（无符号）：与硬件右移UInt一致
      val x_shift = (x >>> sh)
      (x_shift, sh)
    } else if (lz > threshold) {
      val sh = (lz - threshold)
      // 左移（可能溢出上位被截断，软件侧与硬件位宽一致时等价）
      val mask = if (width >= 31) -1 else ((1 << width) - 1)
      val x_shift = (x << sh) & mask
      (x_shift, -sh)
    } else {
      (x, 0)
    }
  }

  // CORDIC核心计算
  def compute(x: Int): Long = {
    val scale_factor = 1 << bit_frac

    // Asymptotic handling: matches hardware behavior
    // Hardware uses UInt input, so x=0 → normalizer produces very negative output
    // For x <= 0 (signed), return most negative representable value
    if (x <= 0) {
      val lnBit = bit_int + bit_frac  // ln output bit width (roughly)
      return -(1L << (lnBit - 1))     // most negative SInt value
    }

    // 规范化输入（与硬件一致）
    val (x_norm, k) = normalize(x)

    // 初始化CORDIC变量（使用Long以避免溢出）
    var x_n: Long = x_norm.toLong + scale_factor.toLong  // x + 1
    var y_n: Long = x_norm.toLong - scale_factor.toLong  // x - 1
    var z_n: Long = 0

    // CORDIC迭代
    for (j <- 1 to rotate) {
      // [FIX] 与硬件一致：y>=0 走“正号”分支（!msb）
      val sign_y = if (y_n >= 0L) 1L else -1L
      val atanh_val = atanh_vals_fix(j).toLong

      val x_temp = x_n
      x_n = x_n - (sign_y * (y_n >> j))
      y_n = y_n - (sign_y * (x_temp >> j))
      z_n = z_n + sign_y * atanh_val

      // 补偿迭代（如果启用），索引保持你的原逻辑 j==4 || j==13
      if (using_compensation_iters && (j == 4 || j == 13)) {
        val sign_y_comp = if (y_n >= 0L) 1L else -1L
        val x_temp_comp = x_n
        x_n = x_n - (sign_y_comp * (y_n >> j))
        y_n = y_n - (sign_y_comp * (x_temp_comp >> j))
        z_n = z_n + sign_y_comp * atanh_val
      }
    }

    // 最终结果计算（考虑规范化因子k和log(2)）
    val result = 2L * z_n + k.toLong * log2_fix.toLong
    result
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

object tb_LN_function extends App {

  new File("rtl/LogFunction/tb_LN_function_report").mkdir()
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
    targetDirectory = "rtl/LogFunction/tb_LN_function_report",
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
// 生成Q12格式的测试输入值，完全覆盖配置定义的浮点范围
  def generateQ12Values(cfg: LN_function_cfg, skipZero: Boolean = false): Seq[Int] = {
    val scale_factor = 1 << cfg.bit_frac
    // 使用ceil确保最小值不小于x_in_Min，使用round处理最大值
    val min_fixed = Math.ceil(cfg.x_in_Min * scale_factor).toInt
    val max_fixed = Math.round(cfg.x_in_Max * scale_factor).toInt

    // 直接在定点数范围内生成所有可能的值
    (min_fixed to max_fixed).toSeq
  }
// 初始化随机数生成器
  val random = new scala.util.Random
  random.setSeed(1233)
  // 使用生成的Q12值作为输入数据源，完全覆盖配置定义的范围
  val x_iter = generateQ12Values(cfg, skipZero = true).iterator
  // 固定点输入的浮点输出计算函数
  def lnx_fixIn_fix_out(x: Int): Int = {
    Math.floor(Math.log(x.toDouble / Math.pow(2, cfg.bit_frac)) * Math.pow(2, cfg.bit_frac)).toInt
  }

// 初始化队列用于存储输入、参考输出和结果
  val x_Queue = mutable.Queue[Int]()
  val lnx_Queue = mutable.Queue[Double]()
  val display_x_Queue = mutable.Queue[Int]()
  val display_ref_Queue = mutable.Queue[Double]()
  val display_out_Queue = mutable.Queue[Int]()
  val display_abserror_Queue = mutable.Queue[Double]()
  val display_relerror_Queue = mutable.Queue[Double]()
  val lnx_sw = new LN_function_sw(cfg)
  // 填充测试数据到队列
  while (x_iter.hasNext) {
    val x = x_iter.next()
    x_Queue.enqueue(x)
    display_x_Queue.enqueue(x)
    val ref = lnx_sw.computeFloat(x)
    lnx_Queue.enqueue(ref)
    display_ref_Queue.enqueue(ref)
  }
// 执行仿真
  module_compiled.doSim("ln_tb", seed = 1233) { dut =>
    SimTimeout(60000000)
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

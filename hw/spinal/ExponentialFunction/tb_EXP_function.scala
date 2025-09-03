package ExponentialFunction
import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.sim.{FlowDriver, FlowMonitor, ScoreboardInOrder}
import scala.collection.mutable
import breeze.plot._
import spire.std.double
import scala.annotation.varargs


// Helper functions to mirror the fixed-point arithmetic and bit-level operations
// of the hardware design.
object FixedPointMath {
  def pow2(x: Double): Double = Math.pow(2, x)

  // This function simulates the floor division of the fixed-point number,
  // effectively shifting the result to the right by `bit_frac` bits.
  def floor(value: Long, bit_frac: Int): Long = {
    value / pow2(bit_frac).toLong
  }

  // This function simulates the saturation logic, ensuring the value doesn't
  // exceed the max integer part of the result.
  def sat(value: Long, max_int_bits: Int, bit_frac: Int): Long = {
    val max_val = pow2(max_int_bits + bit_frac).toLong - 1
    if (value > max_val) max_val else value
  }
}

class EXP_function_sw(cfg: EXP_function_cfg) {
  import cfg._
  import FixedPointMath._

  val exp_frac_table = (1 to rotate).map(i =>
    Math.round(Math.exp(1.0 / pow2(i)) * pow2(bit_frac)).toLong
  )

  val exp_int_pos_table = (0 until log2Up(x_max)).map(i =>
    Math.round(Math.exp(pow2(i)) * pow2(bit_frac)).toLong
  )

  val exp_int_neg_table = (0 until bit_int).map(i =>
    Math.round(Math.exp(-pow2(i)) * pow2(bit_frac)).toLong
  )

  private def getBit(n: Long, k: Int): Boolean = (n & (1L << k)) != 0

  def compute(x: Int): Long = {
    // ------------------------------------
    // 初始值设置 (与硬件一致)
    // ------------------------------------
    val int_x = x >> bit_frac
    val abs_int_x = Math.abs(int_x).toLong
    val neg = int_x < 0
    val frac_x = (x & ((1 << bit_frac) - 1)).toLong
    
    // 初始化寄存器数组 (模拟硬件流水线)
    val frac_x_regs = Array.fill(rotate + 1)(0L)
    val expx_frac_regs = Array.fill(rotate + 1)(0L)
    val abs_int_x_regs = Array.fill(bit_int + 1)(0L)
    val expx_int_regs = Array.fill(bit_int + 1)(0L)
    val neg_regs = Array.fill(bit_int + 1)(false)
    
    // 设置初始值
    frac_x_regs(0) = frac_x
    abs_int_x_regs(0) = abs_int_x
    neg_regs(0) = neg
    expx_int_regs(0) = pow2(bit_frac).toLong // 1.0 的定点表示
    expx_frac_regs(0) = pow2(bit_frac).toLong // 1.0 的定点表示
    
    // 预计算 poweroftwo 值 (与硬件一致)
    val poweroftwo_values = (1 to rotate).map(i => 
      pow2(bit_frac - i).toLong
    )
    
    // ------------------------------------
    // 分数部分计算 (模拟硬件流水线)
    // ------------------------------------
    for (i <- 0 until rotate) {
      // 默认传递当前值到下一级
      frac_x_regs(i + 1) = frac_x_regs(i)
      expx_frac_regs(i + 1) = expx_frac_regs(i)
      
      // 与硬件相同的条件判断
      if (frac_x_regs(i) > poweroftwo_values(i)) {
        frac_x_regs(i + 1) = frac_x_regs(i) - poweroftwo_values(i)
        val product = expx_frac_regs(i) * exp_frac_table(i)
        expx_frac_regs(i + 1) = sat(floor(product, bit_frac), expx_int_bit, bit_frac)
      }
    }
    
    // ------------------------------------
    // 整数部分计算 (模拟硬件流水线)
    // ------------------------------------
    for (i <- 0 until bit_int) {
      // 默认传递当前值到下一级
      abs_int_x_regs(i + 1) = abs_int_x_regs(i)
      neg_regs(i + 1) = neg_regs(i)
      expx_int_regs(i + 1) = expx_int_regs(i)
      
      // 检查当前位是否为1
      if (getBit(abs_int_x_regs(i), i)) {
        if (neg_regs(i)) {
          val product = expx_int_regs(i) * exp_int_neg_table(i)
          expx_int_regs(i + 1) = sat(floor(product, bit_frac), expx_int_bit, bit_frac)
        } else {
          if (i < log2Up(x_max)) {
            val product = expx_int_regs(i) * exp_int_pos_table(i)
            expx_int_regs(i + 1) = sat(floor(product, bit_frac), expx_int_bit, bit_frac)
          }
          // 如果 i >= log2Up(x_max)，保持原值不变 (与硬件一致)
        }
      }
    }

    // ------------------------------------
    // 最终结果计算
    // ------------------------------------
    val final_result_raw = expx_frac_regs(rotate) * expx_int_regs(bit_int)
    val final_result_floor = floor(final_result_raw, bit_frac)
    sat(final_result_floor, expx_int_bit, bit_frac)
  }
}


object sim_EXP_function_test extends App {

  new File("rtl/ExponentialFunction/sim_EXP_function_test_report").mkdir()
  val flags = VCSFlags(
    compileFlags = List("-kdb","-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )
//  val flags = VCSFlags(
//    compileFlags = List(
//      "-kdb -work xil_defaultlib",
//      "/home/cotr/Workspace/Xilinx_IP_lib/glbl.v",
//    ),
//    elaborateFlags = List(
//      "-LDFLAGS -Wl,--no-as-needed",
////      "-fgp",
////      "-kdb",
////      "-lca",
////      "+rad",
////      "+notimingchecks",
//      "xil_defaultlib.glbl"
//    ),
//    runFlags = List("-l ./run.log")
//  )
  val cfg = EXP_function_cfg(
    bit_int = 16,
    bit_frac = 16,
    x_max = 3
  )
  val report = SpinalConfig(
      targetDirectory = "rtl/ExponentialFunction/sim_EXP_function_test_report",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(EXP_function(cfg))
      .printPruned()


  val module_compiled =SimConfig
    .withVCS(flags)
    //.withVCSSimSetup(setupFile = "./synopsys_sim.setup", beforeAnalysis = null)
    .withTimePrecision(1 ps)
    .withFSDBWave
    .withConfig(SpinalConfig(bitVectorWidthMax = 20000)).compile(report)

  val exp_ref = new EXP_function_sw(cfg)


  val random = new scala.util.Random
  val start = -3 * Math.pow(2, cfg.bit_frac).toInt
  val end =  2 * Math.pow(2, cfg.bit_frac).toInt
  val step = (end - start) / 499 // 199 steps to get 200 points

  val x_iter = (start to end by step).map(_.toInt).iterator
  def expx(x: Int): Int = {Math.round(Math.exp(x.toDouble / Math.pow(2,cfg.bit_frac))*Math.pow(2,cfg.bit_frac)).toInt}
  val x_Queue=mutable.Queue[Int]()
  val expx_Queue=mutable.Queue[Int]()
  val display_x_Queue=mutable.Queue[Double]()
  val display_ref_Queue = mutable.Queue[Double]()
  val display_out_Queue=mutable.Queue[Double]()
  val display_Absolute_error_Queue=mutable.Queue[Double]()
  val display_Relative_error_Queue=mutable.Queue[Double]()
  while (x_iter.hasNext) {
    val x = x_iter.next()
    x_Queue.enqueue(x)
    display_x_Queue.enqueue(x/Math.pow(2, cfg.bit_frac))

    val ref = exp_ref.compute(x).toInt
    //val ref = expx(x)

    expx_Queue.enqueue(ref)
    display_ref_Queue.enqueue(ref)
  }

  module_compiled.doSim("exp_tb"){dut =>
    //    println(log2Up(524288))
    SimTimeout(60000)
    val scoreboard = ScoreboardInOrder[Int]()
    FlowDriver(dut.io.x, dut.clockDomain) { payload =>
      if(x_Queue.length>0)
      {
      payload#=x_Queue.dequeue()
      true
    }
      else{
        payload#=0
        true
      }
    }

    FlowMonitor(dut.io.expx, dut.clockDomain) { payload =>
      val received = payload.toInt
      val float_received = received.toDouble/Math.pow(2,cfg.bit_frac)
      display_out_Queue.enqueue(float_received)
      //println(s"out: ${received}\t:${float_received}")
      if(expx_Queue.length>0)
      {
        val ref = expx_Queue.dequeue()
        val float_ref = ref.toDouble/Math.pow(2,cfg.bit_frac)
        val abserror = Math.abs(float_received - float_ref)
        val relerror = abserror / float_ref * 100
        display_Absolute_error_Queue.enqueue(abserror)
        display_Relative_error_Queue.enqueue(relerror)
        //println(s"ref: ${ref}\t:${float_ref}\n\terror: ${error}")
        println(s"ref: ${float_ref}\n\tabsolute error: ${abserror}\n\trelative error: ${relerror}%")

        assert(ref==received)
      }
      else{
        println("end")
        simSuccess()
      }
    }
    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.waitActiveEdgeWhere(false)
  }
  //结果可视化
  val display_x_array = display_x_Queue.toArray
  val display_ref_array = display_ref_Queue.toArray.map(_.toDouble / Math.pow(2, cfg.bit_frac))
  val display_out_array = display_out_Queue.toArray
  val display_Absolute_error_array = display_Absolute_error_Queue.toArray
  val display_Relative_error_array = display_Relative_error_Queue.toArray


  val sortedPairs = display_x_array.zip(display_ref_array).zip(display_out_array).zip(display_Absolute_error_array).zip(display_Relative_error_array)
    .map { case ((((x, ref), out), abserr), relerr) => (x, ref, out, abserr, relerr) }
    .sortBy(_._1)

  val sorted_x_values = sortedPairs.map(_._1)
  val sorted_ref_values = sortedPairs.map(_._2)
  val sorted_out_values = sortedPairs.map(_._3)
  val sorted_abserror_values = sortedPairs.map(_._4)
  val sorted_relerror_values = sortedPairs.map(_._5)

  val sorted_x_values_double = sorted_x_values.map(_.toDouble)
  val sorted_ref_values_double = sorted_ref_values.map(_.toDouble)
  val sorted_out_values_double = sorted_out_values.map(_.toDouble)
  val sorted_abserror_values_double = sorted_abserror_values.map(_.toDouble)
  val sorted_relerror_values_double = sorted_relerror_values.map(_.toDouble)


//var f = Figure()
//var p = f.subplot(0)
//p += plot(sorted_x_values, sorted_y_values, '-')
//p.title = "input vs error"
//p.xlabel = "input"
//p.ylabel = "error"
//  // 设置 y 轴的范围
//var minY = sorted_y_values.min
//var maxY = sorted_y_values.max
//println("max error:"+maxY)
////如果有需要可以保存
//f.saveas("tb_EXP_function_input_vs_error.png")
//
//sortedPairs = display_x_array.zip(display_output_array).sortBy(_._1)
//sorted_x_values = sortedPairs.map(_._1.toDouble)
//sorted_y_values = sortedPairs.map(_._2)
//f = Figure()
//p = f.subplot(0)
//p += plot(sorted_x_values, sorted_y_values, '-')
//p.title = "input vs output"
//p.xlabel = "input"
//p.ylabel = "output"
//minY = sorted_y_values.min
//maxY = sorted_y_values.max
////p.ylim = (minY, maxY*2)
////yTicks = (minY to maxY by (maxY - minY) / 10).toArray // 10 ticks
//f.saveas("tb_EXP_function_input_vs_output.png")
//
//sortedPairs = display_x_array.zip(display_Relative_error_array).sortBy(_._1)
//sorted_x_values = sortedPairs.map(_._1.toDouble)
//sorted_y_values = sortedPairs.map(_._2)
//f = Figure()
//p = f.subplot(0)
//p += plot(sorted_x_values, sorted_y_values, '-')
//p.title = "input vs Relative error"
//p.xlabel = "input"
//p.ylabel = "Relative error"
//minY = sorted_y_values.min
//maxY = sorted_y_values.max
//println("max error:"+maxY*100+"%")
////p.ylim = (minY, maxY*2)
////yTicks = (minY to maxY by (maxY - minY) / 10).toArray // 10 ticks
//f.saveas("tb_EXP_function_input_vs_Relative_error.png")

// —— 绘制对比图 ——
  val fCompare = Figure()
  val pCompare = fCompare.subplot(0)
  pCompare += plot(sorted_x_values_double, sorted_ref_values_double, style = '.', name = "Reference")
  pCompare += plot(sorted_x_values_double, sorted_out_values_double, style = '.', name = "Output")
  pCompare.title = "Hardware vs Reference (exp(x))"
  pCompare.xlabel = "x"
  pCompare.ylabel = "exp(x)"
  pCompare.legend = true
  fCompare.saveas("tb_EXP_function_comparison.png")

  // —— 绝对误差 ——
  val fAbs = Figure()
  val pAbs = fAbs.subplot(0)
  pAbs += plot(sorted_x_values_double, sorted_abserror_values_double.map(_ *1e4), style = '.')
  pAbs.title = "Absolute Error(x 1e-4) (exp(x))"
  pAbs.xlabel = "x"
  pAbs.ylabel = "|Reference - Output|(x 1e-4)"
//  val minYA = sorted_abserror_values_double.min
//  val maxYA = sorted_abserror_values_double.max
//  pAbs.ylim(minYA, maxYA)
  fAbs.saveas("tb_EXP_function_absolute_error.png")

  // —— 相对误差（百分比） ——
  val fRel = Figure()
  val pRel = fRel.subplot(0)
  pRel += plot(sorted_x_values_double, sorted_relerror_values_double.map(_ *1e2), style = '.')
  pRel.title = "Relative Error(x 1e-2) (%) (exp(x))"
  pRel.xlabel = "x"
  pRel.ylabel = "Error(x 1e-2) (%)"
//  val minYR = sorted_relerror_values_double.min
//  val maxYR = sorted_relerror_values_double.max
//  pRel.ylim(minYR, maxYR)
  fRel.saveas("tb_EXP_function_relative_error.png")

}

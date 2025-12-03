package LogarithmFunction

import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.sim.{FlowDriver, FlowMonitor, ScoreboardInOrder}
import scala.collection.mutable
import breeze.plot._
import scala.math._
import FloatingPoint._ // 导入 Fpxx 相关定义

object sim_LN_function_LUT_test extends App {

  // 创建输出目录
  new File("rtl/FP8_look_up/sim_LN_function_test_report").mkdir()
  
  // 仿真器标志
  val flags = VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )

  // ===========================================================================
  // 1. 配置：使用与 DUT 一致的 FP8 E4M3 FNUZ 格式
  // ===========================================================================
  val cfg = FpxxConfig.float8_e4m3fnuz()

  // ===========================================================================
  // 2. 编译 DUT
  // ===========================================================================
  val report = SpinalConfig(
    targetDir ectory = "rtl/FP8_look_up/sim_LN_function_test_report",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(LN_function_LUT(cfg))
    .printPruned()

  val module_compiled = SimConfig
    .withVCS(flags)
    .withTimePrecision(1 ps)
    .withFSDBWave
    .compile(report)

  // ===========================================================================
  // 3. 准备测试数据
  // ===========================================================================
  
  // 遍历所有可能的 8-bit 输入 (0 ~ 255)
  val all_patterns = (0 until 256)
  val x_iter = all_patterns.iterator

  // Driver 用的队列
  val x_Queue = mutable.Queue[Int]()
  
  // Monitor 用的队列 (用于比对和打印)
  val monitor_x_Queue = mutable.Queue[Int]()     // 新增：保存输入Bit模式供Monitor打印
  val lnx_ref_Queue   = mutable.Queue[Double]()  // 保存期望的参考值
  
  // 预计算参考值
  while (x_iter.hasNext) {
    val x_bits = x_iter.next()
    
    // 解码输入用于计算真值
    val x_val = FP8Utils.decode(x_bits, cfg)
    
    // 计算参考真值 (Double精度)
    val ref = if (x_val.isNaN) Double.NaN
              else if (x_val < 0) Double.NaN
              else if (x_val == 0.0) Double.NegativeInfinity
              else Math.log(x_val)

    // 存入队列
    x_Queue.enqueue(x_bits)
    monitor_x_Queue.enqueue(x_bits) // 同时存入 Monitor 队列
    lnx_ref_Queue.enqueue(ref)
  }

  // ===========================================================================
  // 4. 执行仿真
  // ===========================================================================
  module_compiled.doSim("ln_lut_tb", seed = 1233) { dut =>
    SimTimeout(1000000)
    
    // -------------------------------------------------------------------------
    // Driver: 负责将数据送入 DUT
    // -------------------------------------------------------------------------
    FlowDriver(dut.io.op, dut.clockDomain) { payload =>
      if (x_Queue.nonEmpty) {
        val bits = x_Queue.dequeue()
        // 使用 FpxxHost 辅助拆解 bits 并驱动子信号
        val hostVal = FpxxHost(BigInt(bits), cfg)
        payload.sign #= (hostVal.sign == 1)
        payload.exp  #= hostVal.exp
        payload.mant #= hostVal.mant
        true
      } else {
        payload.sign #= false
        payload.exp  #= 0
        payload.mant #= 0
        false
      }
    }

    // -------------------------------------------------------------------------
    // Monitor: 负责接收结果、计算误差并打印
    // -------------------------------------------------------------------------
    FlowMonitor(dut.io.result, dut.clockDomain) { payload =>
      // 1. 获取硬件输出
      val received_bits = payload.toHost().value.toInt
      val float_received = FP8Utils.decode(received_bits, cfg)
      
      if (lnx_ref_Queue.nonEmpty) {
        val float_ref = lnx_ref_Queue.dequeue()
        val input_bits = monitor_x_Queue.dequeue() // 获取对应的输入用于打印
        val float_input = FP8Utils.decode(input_bits, cfg)

        // 2. 误差计算逻辑
        val is_match = (float_received.isNaN && float_ref.isNaN) || 
                       (float_received.isPosInfinity && float_ref.isPosInfinity) ||
                       (float_received.isNegInfinity && float_ref.isNegInfinity)

        var absolute_error = 0.0
        var relative_error = 0.0

        if (is_match) {
          absolute_error = 0.0
          relative_error = 0.0
        } else if (float_received.isNaN || float_ref.isNaN || float_received.isInfinite || float_ref.isInfinite) {
          absolute_error = 999.9 // 标记异常值
          relative_error = 999.9
        } else {
          absolute_error = Math.abs(float_received - float_ref)
          relative_error = if (float_ref != 0) absolute_error / Math.abs(float_ref) * 100 else 0.0
        }
        
        // 3. 打印详细信息 (仿照原版风格)
        // 格式说明:
        // Input: 输入的Hex和解码后的Float
        // Out  : 硬件输出的Hex和解码后的Float
        // Ref  : 理论参考值
        // Err  : 绝对误差和相对误差
        println(f"Input: 0x$input_bits%02X ($float_input%8.4f)  =>  " +
                f"Out: 0x$received_bits%02X ($float_received%8.4f)  |  " +
                f"Ref: $float_ref%8.4f  |  " +
                f"AbsErr: $absolute_error%8.5f  RelErr: $relative_error%6.2f%%")
        
      } else {
        println("Simulation Finished.")
        simSuccess()
      }
    }

    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.waitActiveEdgeWhere(x_Queue.isEmpty && lnx_ref_Queue.isEmpty)
    dut.clockDomain.waitActiveEdge(20)
  }

  // ===========================================================================
  // 5. 结果可视化
  // ===========================================================================
  
  val plot_x = mutable.ArrayBuffer[Double]()
  val plot_ref = mutable.ArrayBuffer[Double]()
  val plot_out = mutable.ArrayBuffer[Double]()
  val plot_abs_err = mutable.ArrayBuffer[Double]()
  val plot_rel_err = mutable.ArrayBuffer[Double]()

  for (i <- 0 until 256) {
    val x_val = FP8Utils.decode(i, cfg)
    
    // 只绘制有效的正数输入
    if (!x_val.isNaN && x_val > 0 && !x_val.isInfinite) {
       val ref = Math.log(x_val)
       val hw_bits = FP8Utils.encodeNearest(ref, cfg)
       val hw_val = FP8Utils.decode(hw_bits, cfg)
       
       if (!hw_val.isNaN && !hw_val.isInfinite) {
         val abs_err = Math.abs(hw_val - ref)
         val rel_err = if (ref != 0) abs_err / Math.abs(ref) * 100 else 0.0
         
         plot_x += x_val
         plot_ref += ref
         plot_out += hw_val
         plot_abs_err += abs_err
         plot_rel_err += rel_err
       }
    }
  }
  
  val sortedIndices = plot_x.zipWithIndex.sortBy(_._1).map(_._2)
  val sorted_x = sortedIndices.map(plot_x)
  val sorted_ref = sortedIndices.map(plot_ref)
  val sorted_out = sortedIndices.map(plot_out)
  val sorted_abs = sortedIndices.map(plot_abs_err)
  val sorted_rel = sortedIndices.map(plot_rel_err)

  // 绘图部分保持不变...
  val fCompare = Figure()
  val pCompare = fCompare.subplot(0)
  pCompare += plot(sorted_x, sorted_ref, style = '-', name = "Reference")
  pCompare += plot(sorted_x, sorted_out, style = '.', name = "Output")
  pCompare.title = "FP8 Hardware vs Reference (ln(x))"
  pCompare.xlabel = "Input x"
  pCompare.ylabel = "Output ln(x)"
  pCompare.legend = true
  fCompare.saveas("tb_LN_function_LUT_comparison.png")

  val fAbs = Figure()
  val pAbs = fAbs.subplot(0)
  pAbs += plot(sorted_x, sorted_abs, style = '.')
  pAbs.title = "Absolute Error"
  pAbs.xlabel = "Input x"
  pAbs.ylabel = "Error"
  fAbs.saveas("tb_LN_function_LUT_absolute_error.png")

  val fRel = Figure()
  val pRel = fRel.subplot(0)
  pRel += plot(sorted_x, sorted_rel, style = '.')
  pRel.title = "Relative Error (%)"
  pRel.xlabel = "Input x"
  pRel.ylabel = "Error (%)"
  fRel.saveas("tb_LN_function_LUT_relative_error.png")
  
  println("Done. Check png files for results.")
}

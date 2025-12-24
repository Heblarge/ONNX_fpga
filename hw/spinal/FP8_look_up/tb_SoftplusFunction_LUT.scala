package SoftplusFunction

import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.sim.{FlowDriver, FlowMonitor}
import scala.collection.mutable
import breeze.plot._
import scala.math._
import FloatingPoint._ // 导入 Fpxx 相关定义

object sim_Softplus_function_LUT_test extends App {

  // ===========================================================================
  // 0. 环境准备
  // ===========================================================================
  val simReportDir = "rtl/FP8_look_up/sim_Softplus_function_test_report"
  new File(simReportDir).mkdirs()
  
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
  // 2. 编译 DUT (Softplus_function_LUT)
  // ===========================================================================
  val report = SpinalConfig(
    targetDirectory = simReportDir,
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(Softplus_function_LUT(cfg))
    .printPruned()

  val module_compiled = SimConfig
    .withVCS(flags)
    .withTimePrecision(1 ps)
    .withFSDBWave
    .compile(report)

  // ===========================================================================
  // 3. 准备测试数据 (穷举 0 ~ 255)
  // ===========================================================================
  
  val all_patterns = (0 until 256)
  val x_iter = all_patterns.iterator

  // Driver 用的队列
  val x_Queue = mutable.Queue[Int]()
  
  // Monitor 用的队列
  val monitor_x_Queue = mutable.Queue[Int]()     
  val sp_ref_Queue    = mutable.Queue[Double]()  // 保存期望的参考值
  
  // --- 预计算参考值 (Golden Model) ---
  while (x_iter.hasNext) {
    val x_bits = x_iter.next()
    
    // 1. 解码输入
    // 复用 Softplus_FP8Utils (注意：确保你的代码中能访问到这个对象)
    val x_val = Softplus_FP8Utils.decode(x_bits, cfg)
    
    // 2. 计算 Softplus 参考真值
    // 逻辑必须与硬件生成器完全一致，包括数值稳定性保护
    val ref = if (x_val.isNaN) Double.NaN
              else if (x_val.isPosInfinity) Double.PositiveInfinity
              else if (x_val.isNegInfinity) 0.0
              else if (x_val > 20.0) x_val // 数值稳定处理
              else Math.log(1.0 + Math.exp(x_val))

    // 3. 存入队列
    x_Queue.enqueue(x_bits)
    monitor_x_Queue.enqueue(x_bits)
    sp_ref_Queue.enqueue(ref)
  }

  // ===========================================================================
  // 4. 执行仿真
  // ===========================================================================
  module_compiled.doSim("softplus_lut_tb", seed = 2024) { dut =>
    SimTimeout(1000000)
    
    // -------------------------------------------------------------------------
    // Driver: 负责将数据送入 DUT
    // -------------------------------------------------------------------------
    FlowDriver(dut.io.x, dut.clockDomain) { payload =>
      if (x_Queue.nonEmpty) {
        val bits = x_Queue.dequeue()
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
    FlowMonitor(dut.io.softplusx, dut.clockDomain) { payload =>
      // 1. 获取硬件输出
      val received_bits = payload.toHost().value.toInt
      val float_received = Softplus_FP8Utils.decode(received_bits, cfg)
      
      if (sp_ref_Queue.nonEmpty) {
        val float_ref = sp_ref_Queue.dequeue()
        val input_bits = monitor_x_Queue.dequeue()
        val float_input = Softplus_FP8Utils.decode(input_bits, cfg)

        // 2. 误差计算逻辑
        // 匹配 NaN, Inf, 以及 0.0 == -0.0 的情况
        val is_match = (float_received.isNaN && float_ref.isNaN) || 
                       (float_received.isPosInfinity && float_ref.isPosInfinity) ||
                       (float_received.isNegInfinity && float_ref.isNegInfinity) ||
                       (float_received == 0.0 && float_ref == 0.0)

        var absolute_error = 0.0
        var relative_error = 0.0

        if (is_match) {
          absolute_error = 0.0
          relative_error = 0.0
        } else if (float_received.isNaN || float_ref.isNaN || float_received.isInfinite || float_ref.isInfinite) {
          absolute_error = 9999.9 
          relative_error = 9999.9
        } else {
          absolute_error = Math.abs(float_received - float_ref)
          // 避免除以0 (Softplus 输出总是 >= 0)
          relative_error = if (float_ref > 1e-9) absolute_error / Math.abs(float_ref) * 100 else 0.0
        }
        
        // 3. 打印详细信息
        println(f"In: 0x$input_bits%02X ($float_input%8.4f) => " +
                f"Out: 0x$received_bits%02X ($float_received%8.4f) | " +
                f"Ref: $float_ref%8.4f | " +
                f"AbsErr: $absolute_error%8.5f | RelErr: $relative_error%6.2f%%")
        
      } else {
        println("Simulation Finished.")
        simSuccess()
      }
    }

    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.waitActiveEdgeWhere(x_Queue.isEmpty && sp_ref_Queue.isEmpty)
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
    val x_val = Softplus_FP8Utils.decode(i, cfg)
    
    // 过滤 NaN，Softplus 对负数和正数都有定义
    if (!x_val.isNaN) {
       val ref = if (x_val.isPosInfinity) Double.PositiveInfinity
                 else if (x_val.isNegInfinity) 0.0
                 else if (x_val > 20.0) x_val
                 else Math.log(1.0 + Math.exp(x_val))
       
       // 模拟硬件的编解码过程
       val hw_bits = Softplus_FP8Utils.encodeNearest(ref, cfg)
       val hw_val = Softplus_FP8Utils.decode(hw_bits, cfg)
       
       // 只有当硬件输出也是有效数值时才绘图
       if (!hw_val.isNaN && !hw_val.isInfinite && !ref.isInfinite) {
         val abs_err = Math.abs(hw_val - ref)
         val rel_err = if (ref > 1e-9) abs_err / Math.abs(ref) * 100 else 0.0
         
         plot_x += x_val
         plot_ref += ref
         plot_out += hw_val
         plot_abs_err += abs_err
         plot_rel_err += rel_err
       }
    }
  }
  
  // 排序方便绘图
  val sortedIndices = plot_x.zipWithIndex.sortBy(_._1).map(_._2)
  val sorted_x = sortedIndices.map(plot_x)
  val sorted_ref = sortedIndices.map(plot_ref)
  val sorted_out = sortedIndices.map(plot_out)
  val sorted_abs = sortedIndices.map(plot_abs_err)
  val sorted_rel = sortedIndices.map(plot_rel_err)

  // 图1: 函数曲线对比
  val fCompare = Figure()
  val pCompare = fCompare.subplot(0)
  pCompare += plot(sorted_x, sorted_ref, style = '-', name = "Reference")
  pCompare += plot(sorted_x, sorted_out, style = '.', name = "Output")
  pCompare.title = "FP8 Hardware vs Reference (Softplus(x))"
  pCompare.xlabel = "Input x"
  pCompare.ylabel = "Output Softplus(x)"
  pCompare.legend = true
  fCompare.saveas(simReportDir + "/tb_Softplus_comparison.png")

  // 图2: 绝对误差
  val fAbs = Figure()
  val pAbs = fAbs.subplot(0)
  pAbs += plot(sorted_x, sorted_abs, style = '.')
  pAbs.title = "Absolute Error"
  pAbs.xlabel = "Input x"
  pAbs.ylabel = "Error"
  fAbs.saveas(simReportDir + "/tb_Softplus_absolute_error.png")

  // 图3: 相对误差
  val fRel = Figure()
  val pRel = fRel.subplot(0)
  pRel += plot(sorted_x, sorted_rel, style = '.')
  pRel.title = "Relative Error (%)"
  pRel.xlabel = "Input x"
  pRel.ylabel = "Error (%)"
  fRel.saveas("tb_Softplus_relative_error.png")
  
  println("Done. Check png files for results.")
}

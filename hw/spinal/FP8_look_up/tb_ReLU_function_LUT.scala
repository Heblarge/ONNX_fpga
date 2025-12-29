package ReLUFunction

import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.sim.{FlowDriver, FlowMonitor}
import scala.collection.mutable
import breeze.plot._
import scala.math._
import FloatingPoint._ // 导入 Fpxx 相关定义

object sim_ReLU_function_LUT_test extends App {

  // ===========================================================================
  // 0. 环境准备
  // ===========================================================================
  val simReportDir = "rtl/ReLU_FP8/sim_ReLU_report"
  new File(simReportDir).mkdir()

  // VCS 仿真器标志 (如果没有安装 VCS，可以删除 .withVCS(flags) 使用默认 Verilator)
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
  // 2. 编译 DUT (ReLU_function_LUT)
  // ===========================================================================
  val report = SpinalConfig(
    targetDirectory = simReportDir,
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(ReLU_function_LUT(cfg)) // 这里实例化 ReLU 模块
    .printPruned()

  val module_compiled = SimConfig
    .withVCS(flags) // 如果没有 VCS，请注释掉这一行
    .withTimePrecision(1 ps)
    .withFSDBWave
    .compile(report)

  // ===========================================================================
  // 3. 准备测试数据 (穷举 0 ~ 255)
  // ===========================================================================
  
  // 遍历所有可能的 8-bit 输入
  val all_patterns = (0 until 256)
  val x_iter = all_patterns.iterator

  // Driver 用的队列
  val x_Queue = mutable.Queue[Int]()
  
  // Monitor 用的队列
  val monitor_x_Queue = mutable.Queue[Int]()     
  val relu_ref_Queue  = mutable.Queue[Double]()  // 保存期望的参考值
  
  // --- 预计算参考值 (Golden Model) ---
  while (x_iter.hasNext) {
    val x_bits = x_iter.next()
    
    // 1. 解码输入
    // 注意：这里复用 ReLU_FP8Utils 进行软件解码
    val x_val = ReLU_FP8Utils.decode(x_bits, cfg)
    
    // 2. 计算 ReLU 参考真值
    // 逻辑：max(0, x)，同时保持 NaN 传递
    val ref = if (x_val.isNaN) Double.NaN
              else if (x_val < 0.0) 0.0
              else x_val

    // 3. 存入队列
    x_Queue.enqueue(x_bits)
    monitor_x_Queue.enqueue(x_bits)
    relu_ref_Queue.enqueue(ref)
  }

  // ===========================================================================
  // 4. 执行仿真
  // ===========================================================================
  module_compiled.doSim("relu_lut_tb", seed = 9999) { dut =>
    SimTimeout(1000000)
    
    // -------------------------------------------------------------------------
    // Driver: 负责将数据送入 DUT
    // -------------------------------------------------------------------------
    FlowDriver(dut.io.x, dut.clockDomain) { payload =>
      if (x_Queue.nonEmpty) {
        val bits = x_Queue.dequeue()
        // 使用 FpxxHost 辅助拆解 bits 并驱动子信号 (Sign, Exp, Mant)
        val hostVal = FpxxHost(BigInt(bits), cfg)
        payload.sign #= (hostVal.sign == 1)
        payload.exp  #= hostVal.exp
        payload.mant #= hostVal.mant
        true
      } else {
        // 队列空了，停止发送 Valid
        payload.sign #= false
        payload.exp  #= 0
        payload.mant #= 0
        false
      }
    }

    // -------------------------------------------------------------------------
    // Monitor: 负责接收结果、计算误差并打印
    // -------------------------------------------------------------------------
    FlowMonitor(dut.io.relux, dut.clockDomain) { payload =>
      // 1. 获取硬件输出
      val received_bits = payload.toHost().value.toInt
      val float_received = ReLU_FP8Utils.decode(received_bits, cfg)
      
      if (relu_ref_Queue.nonEmpty) {
        val float_ref = relu_ref_Queue.dequeue()
        val input_bits = monitor_x_Queue.dequeue()
        val float_input = ReLU_FP8Utils.decode(input_bits, cfg)

        // 2. 误差计算逻辑
        // 对于 ReLU，只要不是 NaN，理论上应该是位精确(Bit-exact)匹配，误差应为0
        val is_match = (float_received.isNaN && float_ref.isNaN) || 
                       (float_received.isPosInfinity && float_ref.isPosInfinity) ||
                       (float_received.isNegInfinity && float_ref.isNegInfinity)

        var absolute_error = 0.0
        var relative_error = 0.0

        if (is_match) {
          absolute_error = 0.0
          relative_error = 0.0
        } else if (float_received.isNaN || float_ref.isNaN || float_received.isInfinite || float_ref.isInfinite) {
          absolute_error = 999.9 // 标记异常
          relative_error = 999.9
        } else {
          absolute_error = Math.abs(float_received - float_ref)
          relative_error = if (float_ref != 0) absolute_error / Math.abs(float_ref) * 100 else 0.0
        }
        
        // 3. 打印详细信息
        // 格式: Input Hex(Float) => Output Hex(Float) | Ref | Err
        println(f"In: 0x$input_bits%02X ($float_input%8.4f) => " +
                f"Out: 0x$received_bits%02X ($float_received%8.4f) | " +
                f"Ref: $float_ref%8.4f | " +
                f"AbsErr: $absolute_error%8.5f")
        
      } else {
        println("Simulation Finished.")
        simSuccess()
      }
    }

    // 启动仿真时钟
    dut.clockDomain.forkStimulus(10)
    // 等待所有数据处理完毕
    dut.clockDomain.waitActiveEdgeWhere(x_Queue.isEmpty && relu_ref_Queue.isEmpty)
    dut.clockDomain.waitActiveEdge(20)
  }

  // ===========================================================================
  // 5. 结果可视化 (Breeze Plot)
  // ===========================================================================
  
  val plot_x = mutable.ArrayBuffer[Double]()
  val plot_ref = mutable.ArrayBuffer[Double]()
  val plot_out = mutable.ArrayBuffer[Double]()
  val plot_abs_err = mutable.ArrayBuffer[Double]()
  val plot_rel_err = mutable.ArrayBuffer[Double]()

  // 再次遍历以收集绘图数据 (只画正常的数字，不画NaN)
  for (i <- 0 until 256) {
    val x_val = ReLU_FP8Utils.decode(i, cfg)
    
    if (!x_val.isNaN && !x_val.isInfinite) {
       // 计算参考值
       val ref = if (x_val < 0) 0.0 else x_val
       
       // 模拟硬件行为 (Encode -> Decode)
       val hw_bits = ReLU_FP8Utils.encodeNearest(ref, cfg)
       val hw_val = ReLU_FP8Utils.decode(hw_bits, cfg)
       
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
  
  // 排序数据以便绘图线条连贯
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
  pCompare.title = "FP8 Hardware vs Reference (ReLU(x))"
  pCompare.xlabel = "Input x"
  pCompare.ylabel = "Output ReLU(x)"
  pCompare.legend = true
  fCompare.saveas(simReportDir + "/tb_ReLU_comparison.png")

  // 图2: 绝对误差 (理论上应全为0，除非有舍入误差，但在ReLU中不应存在)
  val fAbs = Figure()
  val pAbs = fAbs.subplot(0)
  pAbs += plot(sorted_x, sorted_abs, style = '.')
  pAbs.title = "Absolute Error"
  pAbs.xlabel = "Input x"
  pAbs.ylabel = "Error"
  fAbs.saveas("tb_ReLU_function_LUT_absolute_error.png")

  val fRel = Figure()
  val pRel = fRel.subplot(0)
  pRel += plot(sorted_x, sorted_rel, style = '.')
  pRel.title = "Relative Error (%)"
  pRel.xlabel = "Input x"
  pRel.ylabel = "Error (%)"
  fRel.saveas("tb_ReLU_function_LUT_relative_error.png")
  
  println(s"Done. Check png files in $simReportDir for results.")
}

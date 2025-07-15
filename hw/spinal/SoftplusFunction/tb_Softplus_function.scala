package SoftplusFunction

import breeze.linalg.{DenseVector, linspace}

import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.sim.{FlowDriver, FlowMonitor, ScoreboardInOrder}

import scala.collection.mutable
import breeze.plot._
object SoftplusFunctionTest extends App {
  new File("rtl/Softplus_function/sim_softplus_function_test_report").mkdir()
  val cfg=Softplus_function_cfg(
    bit_int = 8,
    bit_frac = 12
  )
  val report = SpinalConfig(
    targetDirectory = "rtl/Softplus_function/sim_softplus_function_test_report",
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

    // 记录输入输出
    val inputData = scala.collection.mutable.ArrayBuffer[Double]()
    val outputData = scala.collection.mutable.ArrayBuffer[Double]()

    val start = -16*1024*4
    val end = 16*1024*4
    val step = 32

    var x_iter = Stream.iterate(start)(_ + step).takeWhile(_ <= end).iterator

    //    val PostionThread = fork {
    //      while (x_iter.hasNext) {
    //        dut.io.inputX.valid #= true
    //        dut.io.inputX.payload #= x_iter.next()
    //        dut.clockDomain.waitSampling()
    //        dut.io.inputX.payload #= x_iter.next()
    //        dut.clockDomain.waitSampling()
    //        //dut.io.inputX.valid #= false
    //        //dut.clockDomain.waitSampling()
    //      }
    //      dut.clockDomain.waitSampling(10)
    //    }
    val PostionThread = fork {
      while (x_iter.hasNext) {
        val x_value = x_iter.next()
        dut.io.x.valid #= true
        dut.io.x.payload #= x_value
        dut.clockDomain.waitSampling()

        inputData.append(x_value.toDouble / (1024.0 * 4)) // 转换回浮点数范围
      }
      dut.clockDomain.waitSampling(10)
    }
    val CaptureThread = fork {
      while (true) {
        dut.clockDomain.waitSampling()
        if (dut.io.softplusx.valid.toBoolean) {
          val y_value = dut.io.softplusx.payload.toInt
          outputData.append(y_value.toDouble / (1024.0 * 4)) // 转换回浮点数范围
        }
      }
    }

    PostionThread.join()
    CaptureThread.terminate()
    // 确保输入输出数据对齐
    //assert(inputData.length == outputData.length, "Input/Output data mismatch due to timing issues")

    // 计算理论值和误差
    val refData = inputData.map(x => math.log1p(math.exp(x))) // 高精度计算理论值
    val absErrors = outputData.zip(refData).map { case (hw, ref) => (ref - hw).abs }
    val relErrors = outputData.zip(refData).map {
      case (hw, ref) if ref.abs < 1e-9 => 0.0 // 避免除以零
      case (hw, ref) => (ref - hw) / ref * 100 // 百分比相对误差
    }

    // 绘制对比图
    val f = Figure()
    val p = f.subplot(0)
    val x_real = linspace(-16.0, 16.0, 100)
    val y_real = x_real.map(x => Math.log(1.0 + Math.exp(x)))
    p += plot(DenseVector(inputData.toArray), DenseVector(outputData.toArray.take(inputData.toArray.length)), style = '.')
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

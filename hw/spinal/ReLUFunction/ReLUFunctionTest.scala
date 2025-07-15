package ReLUFunction

import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.sim._

import scala.collection.mutable.ListBuffer
import breeze.plot._
import breeze.linalg._

object ReLUFunctionTest extends App {

  new File("rtl/Relu/tb_ReluFunction_report").mkdir()
  val cfg=ReLU_function_cfg(bit_int = 8, bit_frac = 12)
  val report = SpinalConfig(
    targetDirectory = "rtl/Relu/tb_ReluFunction_report",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(ReLU_function(cfg))
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


  simCompiled.doSim("relu_tb"){dut =>


    dut.clockDomain.forkStimulus(2)
    dut.io.x.payload #= 0
    dut.io.x.valid #= false
    dut.clockDomain.waitSampling(5)

    // 记录输入输出
    val inputData = scala.collection.mutable.ArrayBuffer[Double]()
    val outputData = scala.collection.mutable.ArrayBuffer[Double]()

    val start = 1*1024*4
      //-5*1024*4
    val end = 6*1024*4
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
        if (dut.io.relux.valid.toBoolean) {
          val y_value = dut.io.relux.payload.toInt
          outputData.append(y_value.toDouble / (1024.0 * 4)) // 转换回浮点数范围
        }
      }
    }

    PostionThread.join()
    CaptureThread.terminate()


    // Breeze 绘图
    val f = Figure()
    val p = f.subplot(0)

    // 真实 Sigmoid 计算
    val x_real = linspace(-6.0, 6.0, 100)
    val y_real = x_real.map(x => if (x > 0) x else 0)

    p += plot(DenseVector(inputData.toArray), DenseVector(outputData.toArray.take(inputData.toArray.length)), style = '.')
    p += plot(x_real, y_real, name = "True Relu", colorcode = "r")

    p.xlabel = "Input x"
    p.ylabel = "Relu(x)"
    p.title = "Hardware Rule Approximation vs True Relu"

    f.saveas("Relu_comparison.png") // 保存图像
  }
}

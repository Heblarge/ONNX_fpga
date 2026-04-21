package LayerNormalization

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import FloatingPoint._
import FloatingPoint.AttentionOps._
import java.io.File
import spinal.sim.VCSFlags

object LayerNormTester extends App {
  val opts = LayerNormOpts(
    c = FpxxConfig.float32(),
    dim = 4
  )


  val vcsFlag = VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )


  val simConfig = SimConfig
    .withVCS(vcsFlag)
    .withFsdbWave
    .withConfig(SpinalConfig(targetDirectory = "rtl/ReduceMean"))

  simConfig.compile(new LayerNorm(opts)).doSim { dut =>

    dut.clockDomain.forkStimulus(period = 10)


    val testData = Array(10.0f, 20.0f, 30.0f, 40.0f)
    val expectedMean = testData.sum / opts.dim


    FlowDriver(dut.io.input, dut.clockDomain) { payload =>
      for (i <- 0 until opts.dim) {
        // 使用 FpxxHost 的 apply 方法将 Float 转为 FpxxHost
        payload(i) #= FpxxHost(testData(i))
      }
      true
    }


    FlowMonitor(dut.io.output, dut.clockDomain) { (payload: Fpxx) =>

      val s = payload.sign.toBigInt
      val e = payload.exp.toBigInt
      val m = payload.mant.toBigInt

      // 2. 根据 float32 的位宽手动拼接 (1 + 8 + 23)
      val rawBits = (s << 31) | (e << 23) | m

      // 3. 转为 Float 进行比对
      val hwFloat = java.lang.Float.intBitsToFloat(rawBits.toInt)

      println(s"[@${simTime()}] 硬件原始位: S=$s, E=$e, M=$m (0x${rawBits.toString(16)})")
      println(s"[@${simTime()}] 硬件计算结果: $hwFloat")
      println(s"[@${simTime()}] 软件预期结果: $expectedMean")

      val error = Math.abs(hwFloat - expectedMean)
      if (error < 0.01) {
        println(">>> SUCCESS <<<")
      } else {
        println(">>> FAIL: Mismatch! <<<")
      }
    }

    dut.clockDomain.waitSampling(100)
    println("Simulation Finished")
  }
}
package FloatingPoint

import org.scalatest.funsuite.AnyFunSuite
import spinal.core._
import spinal.core.formal._
import spinal.core.sim._
import spinal.lib._
import FpxxTesterSupport._
import spinal.sim.VCSFlags
import spinal.lib.sim.ScoreboardInOrder
import java.io.File

import scala.sys.process._

object FpxxConverterTester extends App {

  val FileDir = "rtl/AFix2FpxxTester"
  new File(FileDir).mkdirs()

  def conversionTest(
      inConfig: FpxxConfig,
      outConfig: FpxxConfig,
      testLines: Iterator[String]
  ) {
    val flag = VCSFlags(
      compileFlags = List("-kdb", "-lca", "+notimingchecks"),
      elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
      runFlags = List("-l ./run.log")
    )
    val Spinalcfg = SpinalConfig(
      targetDirectory = FileDir,
      oneFilePerComponent = true,
      // defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
      bitVectorWidthMax = 20000 // disable internal bigvector limitation"Way too big signal Bits"
    )

    val compiled = SimConfig
      .withVCS(flag)
      .withVcdWave
      .withTimeScale(1 ns)
      .withTimePrecision(1 ns)
      .withConfig(Spinalcfg)
      .allOptimisation
      .compile(
        new FpxxConverter(FpxxConverter.Options(inConfig, outConfig, pipeStages = List(true, true, true, true)))
      )
      

      compiled.doSim { dut =>
        SimTimeout(1000000)
        val stimuli = FpxxTesterSupport.parseHexCases(testLines, 1, inConfig, outConfig)

        FpxxTesterSupport.testOperation(stimuli, dut.io.a, dut.io.r, dut.clockDomain)
      }
  }

  conversionTest(
    FpxxConfig.float16(),
    FpxxConfig.float32(),
    FpxxTesterSupport.testfloatGen(Seq("f16_to_f32"))
  )

  // conversionTest(
  //   FpxxConfig.float8_e5m2fnuz(),
  //   FpxxConfig.bfloat16(),
  //   Process(Seq("python3", "testgen.py", "float8_e5m2fnuz", "bfloat16", "conv")).lineStream.iterator
  // )

  conversionTest(
    FpxxConfig.float32(),
    FpxxConfig.float16(),
    FpxxTesterSupport.testfloatGen(Seq("f32_to_f16"))
  )

  // conversionTest(
  //   FpxxConfig.bfloat16(),
  //   FpxxConfig.float8_e5m2fnuz(),
  //   Process(Seq("python3", "testgen.py", "bfloat16", "float8_e5m2fnuz", "conv")).lineStream.iterator
  // )

}

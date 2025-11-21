package FloatingPoint

import spinal.core._
import spinal.core.sim._
import FpxxTesterSupport._
import spinal.sim.VCSFlags
import spinal.lib.sim.ScoreboardInOrder
import java.io.File

object FpxxRSqrtTester extends App {
  val config = FpxxConfig.float32()
  val FileDir = "rtl/FpxxRsqrtTester"
  new File(FileDir).mkdirs()

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
    .compile(FpxxRSqrt(FpxxRSqrt.Options(config, pipeStages = 1)))

  compiled
    .doSim { dut =>
      val lines = testfloatGen(Seq("-n", "100000", "f32")).map(s => {
        val input = java.lang.Float.intBitsToFloat(Integer.parseUnsignedInt(s, 16))
        val invsqrt = 1.0 / scala.math.sqrt(input)
        f"$s ${FpxxHost(invsqrt.floatValue()).value}%x"
      })

      SimTimeout(1000000)
      val stimuli = parseHexCases(lines, 1, config, config, false, maxUlpDist = 1 << (24 / 2 + 2))
        // No denormals
        .filter { a => !a._2.isDenormal && !a._1.map(_.isDenormal).reduce(_ || _) }
      testOperation(stimuli, dut.io.op, dut.io.result, dut.clockDomain)
    }
}

package FloatingPoint

import org.scalatest.funsuite.AnyFunSuite
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import FpxxTesterSupport._
import spinal.sim.VCSFlags
import spinal.lib.sim.ScoreboardInOrder
import java.io.File

case class FpxxMulDut(config: FpxxConfig) extends Component {
  val dut = FpxxMul(FpxxMul.Options(cIn = config, pipeStages = 2))

  val op = slave(Flow(Vec(cloneOf(dut.io.input.payload.a), 2)))
  dut.io.input << op.map { payload =>
    val bundle = cloneOf(dut.io.input.payload)
    bundle.a := payload(0)
    bundle.b := payload(1)

    bundle
  }

  val res = master(cloneOf(dut.io.result))
  res << dut.io.result
}

object FpxxMulTester extends App {

  val FileDir = "rtl/FpxxMulTester"
  new File(FileDir).mkdirs()

  def mulTest(
      config: FpxxConfig,
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
      .compile(FpxxMulDut(config))
      
      
      compiled.doSim { dut =>
        SimTimeout(100000)
        val stimuli = parseHexCases(testLines, 2, config, config, false)
          // Avoid smallest value since it may involve subnormal rounding
          .filter { a => !(a._2.mant == 0 && a._2.exp == 1) }
          // No denormals
          .filter { a => !a._2.isDenormal && !a._1.map(_.isDenormal).reduce(_ || _) }

        testOperation(stimuli, dut.op, dut.res, dut.clockDomain)
      }
  }


    mulTest(
      FpxxConfig.float16(),
      testfloatGen(Seq("f16_mul"))
    )
  


    mulTest(
      FpxxConfig.float32(),
      testfloatGen(Seq("f32_mul"))
    )
  
    val inConfig = FpxxConfig.float8_e5m2fnuz()
    val outConfig = FpxxConfig.bfloat16()

    // SimConfig.withVcdWave
    //   .compile(BundleDebug.fpxxDebugBits(new Module {
    //     val input = slave Flow (Vec(Fpxx(inConfig), 2))
    //     val result = master Flow (Fpxx(outConfig))

    //     val aConv = FpxxConverter(FpxxConverter.Options(inConfig, FpxxConfig(8, 2)))
    //     val bConv = FpxxConverter(FpxxConverter.Options(inConfig, FpxxConfig(8, 2)))
    //     aConv.io.a.payload := input.payload(0)
    //     aConv.io.a.valid := True
    //     bConv.io.a.payload := input.payload(1)
    //     bConv.io.a.valid := True

    //     val mult = FpxxMul(
    //       FpxxMul.Options(FpxxConfig(8, 2), Some(outConfig), pipeStages = 2, rounding = RoundType.FLOOR)
    //     )
    //     mult.io.input.payload.a := aConv.io.r
    //     mult.io.input.payload.b := bConv.io.r
    //     mult.io.input.valid := input.valid

    //     mult.io.result >> result
    //   }))
    //   .doSim { dut =>
    //     SimTimeout(10000000)
    //     import scala.sys.process._
    //     val lines =
    //       Process(Seq("python", "testgen.py", "float8_e5m2fnuz", "bfloat16", "mul")).lineStream.iterator

    //     testOperation(
    //       parseHexCases(lines, 2, FpxxConfig.float8_e5m2fnuz(), FpxxConfig.bfloat16()),
    //       dut.input,
    //       dut.result,
    //       dut.clockDomain
    //     )
    //   }
  }



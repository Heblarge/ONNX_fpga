package FloatingPoint

import org.scalatest.funsuite.AnyFunSuite
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{FlowDriver, FlowMonitor}
import FpxxTesterSupport._
import spinal.sim.VCSFlags
import spinal.lib.sim.ScoreboardInOrder
import java.io.File

object Fpxx2AFixTester extends App {
    val vpiInclude = scala.sys.env.get("VCS_HOME")
    println("VCS_HOME"+vpiInclude)
    val config = FpxxConfig.float64()
    val stimuli = Array[Double](
          0.0,
          1.0,
          -1.0,
          4.8828125e-4,
          0.5,
          20.0,
          50.0,
          32767.5,
          32768.0,
          -32767.5,
          -32768.0,
          65535.5,
          65536.0,
          -65535.5,
          -65536.0,
          (1L << 43).toDouble + 1.0,
          -((1L << 32).toDouble),
          -1000.0,
          127.00390625,
          0.000244140625
    )

    val FileDir = "rtl/Fpxx2AFixTester"
    new File(FileDir).mkdirs()

    val flag = VCSFlags (
    compileFlags = List("-kdb","-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
    )

    val Spinalcfg=SpinalConfig (
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    //defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
    bitVectorWidthMax = 20000, //disable internal bigvector limitation"Way too big signal Bits"
   )

    val compiled = SimConfig
        .withVCS(flag)
        .withFsdbWave
        .withTimeScale(1 ns)
        .withTimePrecision(1 ns)
        .withConfig(Spinalcfg)
        .allOptimisation
        .compile(new Fpxx2AFix(
            intNrBits = 44 bits,
            fracNrBits = 12 bit,
            c = config
          ))

            // .compile(BundleDebug.fpxxDebugBits(new Fpxx2AFix(44 bits, 12 bits, config)))

        compiled.doSim { dut =>
            dut.clockDomain.forkStimulus(period = 10)
            SimTimeout(100000)
            val scoreboard = ScoreboardInOrder[FpxxHost]

            val cases = stimuli.iterator

            FlowDriver(dut.io.op, dut.clockDomain) { payload =>
                if (!cases.isEmpty) {
                    val a = cases.next()
                    payload #= a
                    scoreboard.pushRef(a)
                    true
                } else false
            }

            FlowMonitor(dut.io.result, dut.clockDomain) { payload =>
                assert(!payload.overflow.toBoolean)
                scoreboard.pushDut(payload.number.toDouble)
            }

            dut.clockDomain.forkStimulus(2)
            dut.clockDomain.waitActiveEdgeWhere(cases.isEmpty && scoreboard.ref.isEmpty)
        }


}

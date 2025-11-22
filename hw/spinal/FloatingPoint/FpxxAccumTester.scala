package FloatingPoint

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim.{StreamDriver, StreamMonitor, StreamReadyRandomizer}
import FpxxTesterSupport._
import spinal.sim.VCSFlags
import spinal.lib.sim.ScoreboardInOrder
import java.io.File


object FpxxAccumTester extends App {
        val flag = VCSFlags (
        compileFlags = List("-kdb","-lca", "+notimingchecks"),
        elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"),
        runFlags = List("-l ./run.log")
        )
        val FileDir = "rtl/FpxxAccumTester"
        new File(FileDir).mkdirs()

        val Spinalcfg=SpinalConfig(
            targetDirectory = FileDir,
            oneFilePerComponent = true,
    //defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
            bitVectorWidthMax = 20000, //disable internal bigvector limitation"Way too big signal Bits"
        )

        val compiled = SimConfig
            .withVCS(flag)
            .withVcdWave
            .withTimeScale(1 ns)
            .withTimePrecision(1 ns)
            .withConfig(Spinalcfg)
            .allOptimisation
            .compile(new FpxxAccum(
            FpxxAdd.Options(FpxxConfig.float32(), 1)
            )
            )

            compiled.doSim { dut =>
                SimTimeout(1000000)
                dut.clockDomain.forkStimulus(2)

                dut.clockDomain.waitActiveEdge(2)

                val scoreboard = ScoreboardInOrder[FpxxHost]

                val samples = (1 to 30).map { i =>
                    val nums     = (1 to i).map(_.toFloat).toList
                    val sum      = nums.sum
                    val boolList = List.fill(nums.length - 1)(false) :+ true
                    scoreboard.pushRef(FpxxHost(sum))
                    (nums.zip(boolList).toList, sum)
                }.toList

                val (driver, queue) = StreamDriver.queue(dut.io.op, dut.clockDomain)
                driver.setFactor(0.95f)
                queue ++= samples
                    .map(_._1)
                    .flatten
                    .map(x =>
                        (payload: Fragment[Fpxx]) => {
                            payload.fragment #= FpxxHost(x._1)
                            payload.last #= x._2
                        }
                    )
                    .toList

                StreamMonitor(dut.io.result, dut.clockDomain) { payload =>
                    scoreboard.pushDut(payload.toHost())
                }

                StreamReadyRandomizer(dut.io.result, dut.clockDomain).setFactor(0.95f)

                dut.clockDomain.waitActiveEdgeWhere(queue.isEmpty && scoreboard.ref.isEmpty)
        
    }

}

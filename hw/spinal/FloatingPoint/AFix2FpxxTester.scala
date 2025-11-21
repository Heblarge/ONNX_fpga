package FloatingPoint

import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{FlowDriver, FlowMonitor}
import FpxxTesterSupport._
import spinal.sim.VCSFlags
import spinal.lib.sim.ScoreboardInOrder


object AFix2FpxxTester extends App {
  val vpiInclude = scala.sys.env.get("VCS_HOME")
  println("VCS_HOME"+vpiInclude)
  val stimuli = Array[Float](0, 0.5f, 1, -1, 45, -45, 255, -255, 256, -256, 1000.5f, -100)
  val config  = FpxxConfig.float32()
  val FileDir = "rtl/AFix2FpxxTester"
  import java.io.File
new File(FileDir).mkdirs()
  val flag = VCSFlags(
    compileFlags = List("-kdb","-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )
  val Spinalcfg=SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    //defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
    bitVectorWidthMax = 20000, //disable internal bigvector limitation"Way too big signal Bits"
  )


val Sim_compiled=SimConfig
  .withVCS(flag)
  .withVcdWave
  .withTimeScale(1 ns)
  .withTimePrecision(1 ns)
  .withConfig(Spinalcfg)
  .allOptimisation
  .compile(new AFix2Fpxx(
            intNrBits = 24 bits,
            fracNrBits = 1 bit,
            c = config
          ))

  Sim_compiled.doSim("MatMul test"){ dut =>
      SimTimeout(100000)

      val scoreboard = ScoreboardInOrder[FpxxHost]

      dut.clockDomain.forkStimulus(10)
      dut.clockDomain.forkSimSpeedPrinter(0.2)

      val cases = stimuli.iterator

      // ---- 输入驱动 Flow(op) ----
      FlowDriver(dut.io.op, dut.clockDomain) { payload =>
        if (cases.nonEmpty) {
          val a = cases.next()
          payload.number #= a      // 自动转换 AFix.SQ
          scoreboard.pushRef(a)    // golden push
          true
        } else {
          false
        }
      }

      // ---- 输出监控 Flow(result) ----
      FlowMonitor(dut.io.result, dut.clockDomain) { payload =>
        scoreboard.pushDut(payload.toHost())   // 转回 Host float
      }

      // 和原 TB 行为一致
      dut.clockDomain.waitActiveEdgeWhere(cases.isEmpty && scoreboard.ref.isEmpty)
    }

}

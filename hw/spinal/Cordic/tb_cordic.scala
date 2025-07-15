package Cordic
import Cordic._
import spinal.lib._
import spinal.lib.tools
import spinal.core
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{StreamMonitor, StreamDriver, StreamReadyRandomizer, ScoreboardInOrder}
import spinal.sim.VCSFlags
import scala.util.Random

//TODO: 这个肉眼验证属实有点简陋，写一下自动化验证吧
object Cordic_Sim {

  def main(args: Array[String]): Unit = {
    val FileDir = "rtl/MatMultUnit/verilog"
    val flags = VCSFlags(
      compileFlags = List(
        "-kdb -work xil_defaultlib",
        "/home/cotr/Workspace/Xilinx_IP_lib/glbl.v"
      ),
      elaborateFlags = List(
        "-LDFLAGS -Wl,--no-as-needed",
////    "-fgp",
////    "-kdb",
////    "-lca",
////     "+rad",
////    "+notimingchecks",
        "xil_defaultlib.glbl"
      ),
      runFlags = List("-l ./run.log")
    )
    val cfg = cordicConfig(xinFrac = 20, xoutFrac = 20, rotate = 20)
    var compiled = SimConfig
      .withVCS(flags)
      .withVCSSimSetup(
        setupFile = "./synopsys_sim.setup",
        beforeAnalysis = null
      )
      .withTimePrecision(1 ps)
      .withFSDBWave
      .withConfig(
        SpinalConfig(
          targetDirectory = FileDir,
          bitVectorWidthMax = 20000,
          removePruned = true
        )
      )
      .compile(
        rtl = cordic(cfg)
      )
    compiled.doSim("test") { dut =>
      dut.clockDomain.assertReset()
      dut.io.xin.valid #= false
      dut.io.xin.payload #= 0

      sleep(70)
      dut.clockDomain.deassertReset()
      dut.clockDomain.forkStimulus(10)

      for (i <- 0 until 30) {
        dut.clockDomain.waitSampling()
        dut.io.xin.valid #= true
        dut.io.xin.payload #= 500000
      }
      for (i <- 0 until 30) {
        dut.clockDomain.waitSampling()
        dut.io.xin.valid #= true
        dut.io.xin.payload #= 555555
      }
      for (i <- 0 until 30) {
        dut.clockDomain.waitSampling()
        dut.io.xin.valid #= true
        dut.io.xin.payload #= 1111111
      }
      for (i <- 0 until 30) {
        dut.clockDomain.waitSampling()
        dut.io.xin.valid #= true
        dut.io.xin.payload #= 1600000
      }
      for (i <- 0 until 30) {
        dut.clockDomain.waitSampling()
        dut.io.xin.valid #= true
        dut.io.xin.payload #= 1049000
      }

      sleep(100)

      simSuccess()

    }
//    val compiled = SimConfig
//      .withWave
//      .allOptimisation
//      .compile(
//        rtl = cordicBsc(20, 20, 20)
//      )
//
//    compiled.doSimUntilVoid{dut =>
//      dut.clockDomain.assertReset()
//      dut.io.xin.valid #= false
//      dut.io.xin.payload #= 0
//
//      sleep(70)
//      dut.clockDomain.deassertReset()
//      dut.clockDomain.forkStimulus(10)
//
//      for (i <- 0 until 30){
//        dut.clockDomain.waitSampling()
//        dut.io.xin.valid #= true
//        dut.io.xin.payload #= 1049000
//
//      }
//
//      sleep(100)
//
//      simSuccess()
//
//
//    }

  }

}

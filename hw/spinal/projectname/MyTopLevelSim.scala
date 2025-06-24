package projectname

import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags

import scala.util.Random

object MyTopLevelSim extends App {
    val flags = VCSFlags(
      compileFlags = List("-kdb", "-lca"),
      elaborateFlags = List("-kdb", "-lca")
    )
    SimConfig
      .withVCS(flags)
      .withFSDBWave.compile(new MyTopLevel)
      .doSim{dut =>
        dut.clockDomain.forkStimulus(period = 10)
        var modelState = 0
        for (idx <- 0 to 99) {
          // Drive the dut inputs with random values
          dut.io.cond0 #= Random.nextBoolean()
          dut.io.cond1 #= Random.nextBoolean()
          // Wait a rising edge on the clock
          dut.clockDomain.waitRisingEdge()
          // Check that the dut values match with the reference model ones
          val modelFlag = modelState == 0 || dut.io.cond1.toBoolean
          assert(dut.io.state.toInt == modelState)
          assert(dut.io.flag.toBoolean == modelFlag)
          // Update the reference model value
          if (dut.io.cond0.toBoolean) {
            modelState = (modelState + 1) & 0xff
          }
        }
      }

  // 新增：兼容Java调用的重载方法
  def runSim(cond0Seq: scala.collection.Seq[java.lang.Boolean], cond1Seq: scala.collection.Seq[java.lang.Boolean]): scala.collection.Seq[(Int, Boolean)] = {
    require(cond0Seq.length == cond1Seq.length, "输入序列长度必须一致")
    val flags = VCSFlags(
      compileFlags = List("-kdb", "-lca"),
      elaborateFlags = List("-kdb", "-lca")
    )
    var result = Seq.empty[(Int, Boolean)]
    SimConfig
      .withVCS(flags)
      .withFSDBWave.compile(new MyTopLevel)
      .doSim{dut =>
        dut.clockDomain.forkStimulus(period = 10)
        var modelState = 0
        for (idx <- cond0Seq.indices) {
          val c0 = cond0Seq(idx).booleanValue()
          val c1 = cond1Seq(idx).booleanValue()
          dut.io.cond0 #= c0
          dut.io.cond1 #= c1
          dut.clockDomain.waitRisingEdge()
          val modelFlag = modelState == 0 || dut.io.cond1.toBoolean
          result :+= (dut.io.state.toInt, dut.io.flag.toBoolean)
          assert(dut.io.state.toInt == modelState)
          assert(dut.io.flag.toBoolean == modelFlag)
          if (dut.io.cond0.toBoolean) {
            modelState = (modelState + 1) & 0xff
          }
        }
      }
    result
  }
}


//object MyTopLevelSim extends App {
//  Config.sim.compile(MyTopLevel()).doSim { dut =>
//    // Fork a process to generate the reset and the clock on the dut
//    dut.clockDomain.forkStimulus(period = 10)
//
//    var modelState = 0
//    for (idx <- 0 to 99) {
//      // Drive the dut inputs with random values
//      dut.io.cond0.randomize()
//      dut.io.cond1.randomize()
//
//      // Wait a rising edge on the clock
//      dut.clockDomain.waitRisingEdge()
//
//      // Check that the dut values match with the reference model ones
//      val modelFlag = modelState == 0 || dut.io.cond1.toBoolean
//      assert(dut.io.state.toInt == modelState)
//      assert(dut.io.flag.toBoolean == modelFlag)
//
//      // Update the reference model value
//      if (dut.io.cond0.toBoolean) {
//        modelState = (modelState + 1) & 0xff
//      }
//    }
//  }
//}

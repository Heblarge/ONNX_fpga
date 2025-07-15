package playGround

import spinal.core._
import spinal.core.sim._

object MyTopLevelSim extends App {
  Config.sim.compile(MyTopLevel()).doSim { dut =>
    // Fork a process to generate the reset and the clock on the dut
    dut.clockDomain.forkStimulus(period = 10)

    var modelState = 0
    val thread1=fork{
      // Drive the dut inputs with random values
      dut.io.cond0.randomize()
      dut.io.cond1.randomize()

      // Wait a rising edge on the clock
      dut.clockDomain.waitRisingEdge()
      println(s"thread1:${dut.io.cond0.toBoolean} ${dut.io.cond1.toBoolean}")
      // Check that the dut values match with the reference model ones
      val modelFlag = modelState == 0 || dut.io.cond1.toBoolean
      assert(dut.io.state.toInt == modelState)
      assert(dut.io.flag.toBoolean == modelFlag)

      // Update the reference model value
      if (dut.io.cond0.toBoolean) {
        modelState = (modelState + 1) & 0xff
      }
      if(modelState==100)
      {simSuccess()}else{}
      
    }
    val thread2=forkSensitive{

      println(s"thread2")

    }
    simThread.suspend()
  }
}

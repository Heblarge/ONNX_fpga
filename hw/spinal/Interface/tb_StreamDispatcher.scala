package Util

import spinal.core._
import spinal.core.sim._
import spinal.lib.{Stream, Fragment, master, slave}
import spinal.lib.sim.{StreamDriver, StreamReadyRandomizer, StreamMonitor}
import spinal.sim.VCSFlags

import scala.util.Random
import scala.collection.mutable.ArrayBuffer

case class StreamDispatcherTest(portCount: Int) extends Component {
  val io = new Bundle {
    val input = slave Stream Fragment(Bits(32 bits))
    val outputs = Vec.fill(portCount)(master Stream Fragment(Bits(32 bits)))
  }
  io.outputs <> StreamDispatcher(io.input, portCount)
}

object StreamDispatcherTb extends App {
  val period = 10
  val driveSpeed = 0.5f
  val receiveSpeed = 1f
  val fragmentMaxLength = 10
  val seed = 114514
  val random = new Random(seed)
  val testNum = 10000
  val portCount = 4
  val compiled = SimConfig.withFsdbWave
    .withConfig(
      SpinalConfig(
        bitVectorWidthMax = 100000
      )
    )
    .withVCS(
      VCSFlags(
        compileFlags = List("-kdb", "-lca", "+notimingchecks"),
        elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
        runFlags = List("-l ./run.log")
      )
    )
    .compile(StreamDispatcherTest(portCount))

  val testInputs = ArrayBuffer[(Int, Boolean)]()
  var fragmentLength = random.between(1, fragmentMaxLength + 1)
  for (m <- 0 until testNum) {
    val last =
      if (m == testNum - 1) { true }
      else if (fragmentLength == 1) {
        fragmentLength = random.between(1, fragmentMaxLength + 1)
        true
      } else {
        fragmentLength -= 1
        false
      }
    testInputs += Tuple2(random.nextInt(100), last)
  }

  compiled.doSimUntilVoid { dut =>
    SimTimeout(1000000 * period)
    dut.clockDomain.forkStimulus(period)

    var m = 0
    StreamDriver(dut.io.input, dut.clockDomain) { payload =>
      if (m < testNum) {
        dut.io.input.fragment #= testInputs(m)._1
        dut.io.input.last #= testInputs(m)._2
        m += 1
        true
      } else {
        false
      }
    }.setFactor(driveSpeed)

    var i = 0
    var lock = false
    var portLock = 0
    // StreamReadyRandomizer(dut.io.outputs(0), dut.clockDomain).setFactor(1)
    // StreamReadyRandomizer(dut.io.outputs(1), dut.clockDomain).setFactor(0)
    // StreamReadyRandomizer(dut.io.outputs(2), dut.clockDomain).setFactor(1)
    // StreamReadyRandomizer(dut.io.outputs(3), dut.clockDomain).setFactor(0)
    dut.io.outputs.foreach(StreamReadyRandomizer(_, dut.clockDomain).setFactor(receiveSpeed))
    dut.io.outputs.zipWithIndex.foreach { case (output, port) =>
      StreamMonitor(output, dut.clockDomain) { payload =>
        val last = payload.last.toBoolean
        assert(payload.fragment.toInt == testInputs(i)._1, "data dispatch error")
        assert(last == testInputs(i)._2, "last signal dispatch error")
        if (!lock && last) {} else if (!lock && !last) {
          lock = true
          portLock = port
        } else {
          assert(port == portLock, "dispatch lock error")
          if (last) lock = false
        }
        if (i == testNum - 1) {
          println("TEST PASS".green)
          simSuccess()
        } else {
          i += 1
        }
      }
    }
  }
}

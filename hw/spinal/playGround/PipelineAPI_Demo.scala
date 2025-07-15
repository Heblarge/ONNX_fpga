package playGround

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.tools
import spinal.lib.misc.pipeline._
import spinal.lib.sim.{StreamMonitor, StreamDriver, StreamReadyRandomizer, ScoreboardInOrder}

class PipelineAPI_BasicDemo extends Component {
  val io = new Bundle {
    val up = slave Stream (UInt(16 bits))
    val down = master Stream (UInt(16 bits))
  }

  // Let's define 3 Nodes for our pipeline
  val n0, n1, n2 = Node()

  // Let's connect those nodes by using simples registers
  val s01 = StageLink(n0, n1)
  val s12 = StageLink(n1, n2)

  // Let's define a few Payload things that can go through the pipeline
  val VALUE = Payload(UInt(16 bits))
  val RESULT = Payload(UInt(16 bits))

  // Let's bind io.up to n0
  io.up.ready := n0.ready
  n0.valid := io.up.valid
  n0(VALUE) := io.up.payload

  // Let's do some processing on n1
  n1(RESULT) := n1(VALUE)/3 

  // Let's bind n2 to io.down
  n2.ready := io.down.ready
  io.down.valid := n2.valid
  io.down.payload := n2(RESULT)

  // Let's ask the builder to generate all the required hardware
  Builder(s01, s12)

}
object PipelineAPI_BasicDemo_Sim extends App {
    ////tools.HDElkDiagramGen(SpinalVerilog(new PipelineAPI_BasicDemo()))
  Config.sim.compile(new PipelineAPI_BasicDemo()).doSim { dut =>
    // Fork a process to generate the reset and the clock on the dut
    
    val scoreboard = ScoreboardInOrder[Int]()
    // drive random data and add pushed data to scoreboard
    StreamDriver(dut.io.up, dut.clockDomain) { payload =>
      payload.randomize()
      true
    }
    StreamMonitor(dut.io.up, dut.clockDomain) { payload =>
      scoreboard.pushRef(payload.toInt/3)
    }
    // randmize ready on the output and add popped data to scoreboard
    StreamReadyRandomizer(dut.io.down, dut.clockDomain)
    StreamMonitor(dut.io.down, dut.clockDomain) { payload =>
      scoreboard.pushDut(payload.toInt)
    }
    dut.clockDomain.forkStimulus(10)

    dut.clockDomain.waitActiveEdgeWhere(scoreboard.matches == 100)

    
  }
}

class PipelineAPI_AdvancedDemo extends Component {
  val VALUE = Payload(UInt(16 bits))

  val io = new Bundle {
    val up = slave Stream(VALUE)  // VALUE can also be used as a HardType
    val down = master Stream(VALUE)
  }

  // NodesBuilder will be used to register all the nodes created, connect them via stages and generate the hardware
  val builder = new NodesBuilder()

  // Let's define a Node which connect from io.up
  val n0 = new builder.Node {
    arbitrateFrom(io.up)
    VALUE := io.up.payload
  }

  // Let's define a Node which do some processing
  val n1 = new builder.Node {
    val RESULT = insert(VALUE /3 )
  }

  //  Let's define a Node which connect to io.down
  val n2 = new builder.Node {
    arbitrateTo(io.down)
    io.down.payload := n1.RESULT
  }

  // Let's connect those nodes by using registers stages and generate the related hardware
  builder.genStagedPipeline()
}

object PipelineAPI_AdvencedDemo_Sim extends App {
    ////tools.HDElkDiagramGen(SpinalVerilog(new PipelineAPI_AdvancedDemo()))
  Config.sim.compile(new PipelineAPI_AdvancedDemo()).doSim { dut =>
    // Fork a process to generate the reset and the clock on the dut
    
    val scoreboard = ScoreboardInOrder[Int]()
    // drive random data and add pushed data to scoreboard
    StreamDriver(dut.io.up, dut.clockDomain) { payload =>
      payload.randomize()
      true
    }
    StreamMonitor(dut.io.up, dut.clockDomain) { payload =>
      scoreboard.pushRef(payload.toInt/3)
    }
    // randmize ready on the output and add popped data to scoreboard
    StreamReadyRandomizer(dut.io.down, dut.clockDomain)
    StreamMonitor(dut.io.down, dut.clockDomain) { payload =>
      scoreboard.pushDut(payload.toInt)
    }
    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.waitActiveEdgeWhere(scoreboard.matches == 100)
  }
}


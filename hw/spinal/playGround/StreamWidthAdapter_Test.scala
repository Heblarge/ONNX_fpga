import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.tools
import spinal.lib.sim.{StreamMonitor, StreamDriver, StreamReadyRandomizer, ScoreboardInOrder}


case class StreamWidthAdapterTop() extends Component {
val io=new Bundle{
  val inStream = slave(Stream(Vec.fill(4)(Vec.fill(4)(SInt(16 bits)))))
val outStream = master(Stream(Vec.fill(4)(Vec.fill(4)(SInt(16 bits)))))
}
val midStream_in = Stream(Vec.fill(4)(SInt(16 bits).asBits).asBits)
val fifo=StreamFifo(Vec.fill(4)(SInt(16 bits).asBits).asBits, 2)
val midStream_out = Stream(Vec.fill(4)(SInt(16 bits).asBits).asBits)

val in2mid = StreamWidthAdapter(io.inStream, midStream_in)
val mid2out = StreamWidthAdapter(midStream_out, io.outStream)
midStream_in>/->fifo.io.push
fifo.io.pop>/->midStream_out
}
object StreamWidthAdapterTop_Verilog extends App 
{
  SpinalConfig().generateVerilog(StreamWidthAdapterTop())
  ////tools.HDElkDiagramGen(SpinalVerilog(StreamWidthAdapterTop()))
}
object StreamWidthAdapterTop_Sim extends App {
  val FileDir = "rtl/StreamWidthAdapterTop/verilog"
  import java.io.File
  new File(FileDir).mkdirs()
import spinal.sim.VCSFlags
      val flag = VCSFlags(
        compileFlags = List("-kdb","-lca", "+notimingchecks"),
        elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"),
        //    runFlags = List("-fgp=num_threads:11,allow_less_cores", "-l ./run.log")
        //    elaborateFlags = List("-fgp", "+notimingchecks"),
        runFlags = List("-l ./run.log")
      )
val Sim_compiled=SimConfig
      .withVCS(flag)
      .withVcdWave
      .withTimeScale(1 ns)
      .withTimePrecision(1 ns)
      .withConfig(SpinalConfig(
                targetDirectory = FileDir,
        oneFilePerComponent = true,
        defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
        ))
      .allOptimisation
      .compile(new StreamWidthAdapterTop())
  Sim_compiled.doSim("simple test") { dut =>
    SimTimeout(10000)

    val scoreboard = ScoreboardInOrder[BigInt]()

    // drive random data and add pushed data to scoreboard
    StreamDriver(dut.io.inStream, dut.clockDomain) { payload =>
      payload.randomize()
      true
    }
    StreamMonitor(dut.io.inStream, dut.clockDomain) { payload =>
      
        scoreboard.pushRef(payload(0)(0).toBigInt)
        scoreboard.pushRef(payload(0)(1).toBigInt)
    }
      
    // randmize ready on the output and add popped data to scoreboard
    StreamReadyRandomizer(dut.io.outStream, dut.clockDomain)
    StreamMonitor(dut.io.outStream, dut.clockDomain) { payload =>

        scoreboard.pushDut(payload(0)(0).toBigInt)
        scoreboard.pushDut(payload(0)(1).toBigInt)

    }

    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.waitActiveEdgeWhere(scoreboard.matches == 10)
  }
}
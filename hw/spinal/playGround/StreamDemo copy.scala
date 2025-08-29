import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.tools
import spinal.lib.sim.{StreamMonitor, StreamDriver, StreamReadyRandomizer, ScoreboardInOrder}
import GeMM.SystolicArray2D.sim_SIntShifter.inputQueue



case class SteamDemoTop() extends Component {
  def streamHalfPipeN[T <: Data](input: Stream[T], n: Int): Stream[T] = {
    require(n >= 0)
    (0 until n).foldLeft(input)((stream, _) => stream.pipelined(true,true,false))
  }
val io=new Bundle{
  val inStream = slave(Stream(Vec.fill(4)(SInt(16 bits))))
  val outStream = master(Stream(Vec.fill(4)(SInt(16 bits))))
}
val dispachedStreams=StreamFork(io.inStream,4)
val midStream_in = Vec.fill(4)(Stream((SInt(16 bits))))
//val midStream_out = Vec.fill(4)(Stream((SInt(16 bits))))

for(index <- 0 until 4){
  
  var substream=dispachedStreams(index).map{payload=>
    val new_payload = (SInt(16 bits))
    new_payload := payload(index)
    new_payload
    }
  
  midStream_in(index)<<streamHalfPipeN(substream,index)
}

io.outStream<<StreamJoin.vec(midStream_in)





}
object SteamDemoTop_Verilog extends App 
{
  SpinalConfig().generateVerilog(SteamDemoTop())
  ////tools.HDElkDiagramGen(SpinalVerilog(SteamDemoTop()))
} 
object SteamDemoTop_Sim extends App {
  val FileDir = "rtl/SteamDemoTop/verilog"
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
      .compile(new SteamDemoTop())
  Sim_compiled.doSim("simple test") { dut =>
    SimTimeout(1000)

    val scoreboard = ScoreboardInOrder[BigInt]()

    // drive random data and add pushed data to scoreboard
    StreamDriver(dut.io.inStream, dut.clockDomain) { payload =>
      payload.randomize()
      true
    }
    StreamMonitor(dut.io.inStream, dut.clockDomain) { payload =>
      
        scoreboard.pushRef(payload(0).toBigInt)
        scoreboard.pushRef(payload(1).toBigInt)
    }
      
    // randmize ready on the output and add popped data to scoreboard
    StreamReadyRandomizer(dut.io.outStream, dut.clockDomain)
    StreamMonitor(dut.io.outStream, dut.clockDomain) { payload =>



    }

    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.waitActiveEdgeWhere(scoreboard.matches == 10)
  }
}
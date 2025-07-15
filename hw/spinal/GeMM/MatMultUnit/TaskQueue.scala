package GeMM.MatMult
import spinal.lib._
import spinal.core
import spinal.core._
import spinal.lib.tools
import breeze.numerics.log2

case class TaskQueue_Config(
  depth: Int,
  num_readport: Int,
  allowExtraMsb: Boolean = true,
  forFMax: Boolean = true)

case class TaskQueue[T <: Data](
    cfg: TaskQueue_Config,
    Task_Type: HardType[T]
) extends Component{
  require(cfg.depth > 1) // bypass is not allowed,one stage is not allowed
  val io = new Bundle {
    val push = slave Stream (Task_Type)
    val pop = master Stream (Task_Type)
    val flush = in Bool () default (False)
    val occupancy = out UInt (log2Up(cfg.depth + 1) bits) // 占用率输出信号，最大值为depth
    val availability = out UInt (log2Up(cfg.depth + 1) bits)
    def pushOccupancy = occupancy
    def popOccupancy = occupancy
    val readPorts = Vec.fill(cfg.num_readport)(master(Stream(Task_Type))) // 逻辑是每次fire就读取下一个任务
  }

  // TaskQueueFifo类继承自StreamFifo
  // 与原本的StreamFifo相比：
  // depth         至少为2
  // withAsyncRead 为真，并添加了若干个读端口
  // withBypass    为假
  // allowExtraMsb 留作参数
  // forFMax       留作参数
  // useVec        为真
  class TaskQueueFifo[T <: Data](dataType: HardType[T], depth: Int, num_readport: Int)
      extends StreamFifo[T](
        dataType = dataType,
        depth = depth,
        withAsyncRead = true, // 固定值
        withBypass = false, // 固定值
        allowExtraMsb = cfg.allowExtraMsb,
        forFMax = cfg.forFMax,
        useVec = true // 固定值
      ) {
    // 新增的读端口
    val readPorts = Vec.fill(num_readport)(master(Stream(dataType)))
    // 逻辑是每次fire就读取下一个任务，如果不是valid_Flag(logic.ptr.pop.resized)，那stream的valid也会被置0
    val valid_data_judge_logic = new Area {
      val valid_Flag = Vec.fill(depth)(Reg(Bool()) init False)
      when(logic.ptr.doPush) {
        valid_Flag(logic.ptr.push.resized) := True
      }
      when(logic.ptr.doPop) {
        valid_Flag(logic.ptr.pop.resized) := False
      }
    }
    // 连接读端口至内部逻辑
    val read = new Area {
      val pop_ptr = logic.pop.addressGen.payload
      for (readPort_Idx <- 0 until num_readport) {
        val Address_to_Read = Counter(stateCount = depth, inc = readPorts(readPort_Idx).fire)
        if (!isPow2(depth)) when(Address_to_Read.value > depth - 1) { Address_to_Read.clear() }
        readPorts(readPort_Idx).payload := logic.vec.read(Address_to_Read.resize(log2Up(depth)))
        readPorts(readPort_Idx).valid := valid_data_judge_logic.valid_Flag(Address_to_Read)
      }
    }

  }
  val Fifo = new TaskQueueFifo(
    dataType = Task_Type,
    depth = cfg.depth,
    num_readport = cfg.num_readport
  )
  io.push <> Fifo.io.push
  io.pop <> Fifo.io.pop
  Fifo.io.flush <> io.flush
  io.occupancy <> Fifo.io.occupancy
  io.availability <> Fifo.io.availability
  for (i <- 0 until cfg.num_readport) {
    io.readPorts(i) <> Fifo.readPorts(i)
  }

}
//套这层的主要目的在于防止spinal检查的时候说我在testbench里面写asBits
class test_Task() extends Bundle {
  val payload = UInt(16 bits)
}
case class TaskQueue_testTop() extends Component {

  val cfg = TaskQueue_Config(
    depth = 16,
    num_readport = 2
  )
  val io = new Bundle {
    val push = slave Stream (new test_Task().asBits)
    val pop = master Stream (new test_Task().asBits)
    val flush = in Bool () default (False)
    val occupancy = out UInt (log2Up(cfg.depth + 1) bits) // 占用率输出信号，最大值为depth
    val availability = out UInt (log2Up(cfg.depth + 1) bits)
    def pushOccupancy = occupancy
    def popOccupancy = occupancy
    val readPorts = Vec.fill(cfg.num_readport)(master(Stream(new test_Task().asBits)))

  }
  val DUT_top = TaskQueue(cfg, new test_Task())
  io.push.ready <> DUT_top.io.push.ready
  DUT_top.io.push.payload.assignFromBits(io.push.payload)
  io.push.valid <> DUT_top.io.push.valid
  DUT_top.io.pop.ready <> io.pop.ready
  DUT_top.io.pop.payload.asBits <> io.pop.payload
  io.pop.valid <> DUT_top.io.pop.valid
  io.flush <> DUT_top.io.flush
  io.occupancy <> DUT_top.io.occupancy
  io.availability <> DUT_top.io.availability
  for (i <- 0 until cfg.num_readport) {
    io.readPorts(i).ready <> DUT_top.io.readPorts(i).ready
    io.readPorts(i).payload <> DUT_top.io.readPorts(i).payload.asBits
    io.readPorts(i).valid <> DUT_top.io.readPorts(i).valid
  }

}

import spinal.sim.VCSFlags
import spinal.core.sim._
import spinal.lib.sim.{StreamMonitor, StreamDriver, StreamReadyRandomizer, ScoreboardInOrder}
import scala.util.Random
object TaskQueue_Sim extends App {
  val FileDir = "rtl/SteamDemoTop/verilog"
  import java.io.File
  new File(FileDir).mkdirs()

  val flag = VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
    //    runFlags = List("-fgp=num_threads:11,allow_less_cores", "-l ./run.log")
    //    elaborateFlags = List("-fgp", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )

  val Sim_compiled = SimConfig
    .withVCS(flag)
    .withFsdbWave
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .withConfig(
      SpinalConfig(
        targetDirectory = FileDir,
        oneFilePerComponent = true,
        defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
      )
    )
    .allOptimisation
    .compile(new TaskQueue_testTop())
//!验证流程
  Sim_compiled.doSim("test as Fifo") { dut =>
    StreamDriver(dut.io.push, dut.clockDomain) { payload =>
      payload.randomize()
      true
    }
    val scoreboard_pop = ScoreboardInOrder[Int]()
    val scoreboard_read1 = ScoreboardInOrder[Int]()
    val scoreboard_read2 = ScoreboardInOrder[Int]()
    StreamMonitor(dut.io.push, dut.clockDomain) { payload =>
      scoreboard_pop.pushRef(payload.toInt)
      scoreboard_read1.pushRef(payload.toInt)
      scoreboard_read2.pushRef(payload.toInt)
    }
    // randmize ready on the output and add popped data to scoreboard
    StreamReadyRandomizer(dut.io.pop, dut.clockDomain)
    StreamMonitor(dut.io.pop, dut.clockDomain) { payload =>
      scoreboard_pop.pushDut(payload.toInt)
    }
    StreamMonitor(dut.io.readPorts(0), dut.clockDomain) { payload =>
      scoreboard_read1.pushDut(payload.toInt)
    }
    StreamMonitor(dut.io.readPorts(1), dut.clockDomain) { payload =>
      scoreboard_read2.pushDut(payload.toInt)
    }

    dut.clockDomain.forkStimulus(period = 10) // 时钟周期为10
    dut.io.readPorts(0).ready #= true
    dut.io.readPorts(1).ready #= true
    dut.clockDomain.waitActiveEdgeWhere(scoreboard_pop.matches == 50)

    println("Simulation finished successfully.")
  }
}

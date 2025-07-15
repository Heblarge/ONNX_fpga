package DataPump
import spinal.lib._
import spinal.lib.tools
import spinal.core
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{StreamMonitor, StreamDriver, StreamReadyRandomizer, ScoreboardInOrder}
import spinal.sim.VCSFlags
import scala.util.Random

case class DataPump_mm2mm_test_Top(cfg: DataPump_mm2mm_Config) extends Component {
  // 实例化 sdpram 和 DataPump_mm2s
  val sdpram_in = DataPump_mm2s_sdpram_for_test(cfg.Pump_in_Cfg)
  val sdpram_out = DataPump_s2mm_sdpram_for_test(cfg.Pump_out_Cfg)
  val data_pump = DataPump_mm2mm(cfg)
  val io = new Bundle {
    val sdpram_in_write_port = slave(sdpram_in.MemoryWritePort_Type())
    val sdpram_out_read_port = slave(sdpram_out.MemoryReadPort_Type())
    val Task_in = slave(Stream (data_pump.Task_Type()))
    val Task_out = master(Stream(data_pump.Task_Type()))
  }

  // 连接 DataPump 的 MemoryReadPort 到 sdpram
  data_pump.io.MemoryReadPort <> sdpram_in.io.read
  data_pump.io.Task_in <> io.Task_in
  data_pump.io.Task_out <> io.Task_out
  sdpram_out.io.write <> data_pump.io.MemoryWritePort


  // 连接 sdpram 的写接口最外层
  sdpram_in.io.write <> io.sdpram_in_write_port
  sdpram_out.io.read <> io.sdpram_out_read_port


}

//!仿真程序主体
object DataPump_mm2mm_Sim extends App {
  val flags = VCSFlags(
    compileFlags = List(
      "-kdb -work xil_defaultlib",
      "/home/cotr/Workspace/Xilinx_IP_lib/glbl.v"
    ),
    elaborateFlags = List(
      "-LDFLAGS -Wl,--no-as-needed",
      "xil_defaultlib.glbl"
    ),
    runFlags = List("-l ./run.log")
  )

  val cfg = DataPump_mm2mm_Config()
  var compiled=SimConfig
    .withVCS(flags)
    .withVCSSimSetup(
      setupFile = "./synopsys_sim.setup",
      beforeAnalysis = null
    )
    .withTimePrecision(1 ps)
    .withFSDBWave
    .withConfig(
      SpinalConfig(
        bitVectorWidthMax = 20000,
        removePruned = true
      )
    )
    .compile(new DataPump_mm2mm_test_Top(cfg))
    // !多任务测试
  compiled.doSim("multiple tasks") { dut =>
    SimTimeout(60000)
    dut.clockDomain.forkStimulus(period = 10)

    // Initialize signals
    dut.io.sdpram_in_write_port.Valid #= false
    dut.io.sdpram_in_write_port.Address #= 0
    sleep(200)

    // Randomize input data for memory write
    val random = new Random()
    val addressDataMap = (0 until 512).map { i =>
      val data = random.nextInt(1000) // Random data generation
      dut.io.sdpram_in_write_port.Valid #= true
      dut.io.sdpram_in_write_port.Address #= i
      dut.io.sdpram_in_write_port.Data #= data
      sleep(10)
      (i, data)
    }.toMap
    dut.io.sdpram_in_write_port.Valid #= false

    sleep(10)

    // Task generation and data check
    val numTasks = 20 // Number of random tasks to test
    var currentTask = 0
    val tasks = Array.fill(numTasks) {
        val repeatNum = random.nextInt(10) + 1 // ranging from 1 to 10
        
        val in_startAddr = random.nextInt(256) // ranging from 0 to 255
        val in_validNum = random.nextInt(repeatNum)
        val in_data_to_Pad=random.nextInt(10)
        val in_offset = random.nextInt(5) + 1 // ranging from 1 to 5

        val out_startAddr = random.nextInt(256)
        val out_validNum = random.nextInt(repeatNum)
        val out_offset = random.nextInt(5) + 1


      (repeatNum, in_startAddr, in_validNum,in_data_to_Pad,in_offset,out_startAddr,out_validNum,out_offset)
    }

    // Send random tasks to TaskStream
    StreamDriver(dut.io.Task_in, dut.clockDomain) { payload =>
      if (currentTask < numTasks) {
        val (repeatNum, in_startAddr, in_validNum,in_data_to_Pad,in_offset,out_startAddr,out_validNum,out_offset) = tasks(currentTask)
        payload.RepeatNum #= repeatNum
        payload.Task_Pump_in.StartAddr #= in_startAddr
        payload.Task_Pump_in.ValidNum #= in_validNum
        payload.Task_Pump_in.Offset #= in_offset
        payload.Task_Pump_in.data_to_Pad #= in_data_to_Pad
        payload.Task_Pump_out.StartAddr #= out_startAddr
        payload.Task_Pump_out.ValidNum #= out_validNum
        payload.Task_Pump_out.Offset #= out_offset
        println(s"Sending task $currentTask:\nin: StartAddr=$in_startAddr, RepeatNum=$repeatNum,ValidNum=$in_validNum, Offset=$in_offset\nout: StartAddr=$out_startAddr, RepeatNum=$repeatNum,ValidNum=$out_validNum, Offset=$out_offset\n")
        currentTask += 1
        true
      } else {
        false // No more tasks to send
      }
    }
    StreamReadyRandomizer(dut.io.Task_out, dut.clockDomain)
    // Monitor and verify output for each task
    var readCounter = 0
    var currentTaskIndex = 0
    simThread.suspend()
  }
}


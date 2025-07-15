package DataPump
import spinal.lib._
import spinal.lib.tools
import spinal.core
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{StreamMonitor, StreamDriver, StreamReadyRandomizer, ScoreboardInOrder}
import spinal.sim.VCSFlags
import scala.util.Random

case class DataPump_mm2s_sdpram_for_test(cfg: DataPump_mm2s_Config) extends Component {
  def MemoryWritePort_Type():MemoryWritePort_TypeDef={new MemoryWritePort_TypeDef(cfg.mem_addr_width, cfg.mem_data_width)}
  def MemoryReadPort_Type():MemoryReadPort_TypeDef={new MemoryReadPort_TypeDef(cfg.mem_addr_width, cfg.mem_data_width)}
  val io = new Bundle {
    val write = slave(MemoryWritePort_Type)
    val read = slave(MemoryReadPort_Type)
  }
  
  // 创建具有同步读写的 RAM
  val mem = Mem(Bits(cfg.mem_data_width bits), wordCount = 1 << cfg.mem_addr_width)

  // 写入操作
  mem.write(
    enable = io.write.Valid,
    address = io.write.Address,
    data = io.write.Data
  )

  // 读取操作
  io.read.Data := mem.readSync(
    enable = io.read.Valid,
    address = io.read.Address
  )
}
case class DataPump_mm2s_test_Top(cfg: DataPump_mm2s_Config) extends Component {
  // 实例化 sdpram 和 DataPump_mm2s
  val sdpram = DataPump_mm2s_sdpram_for_test(cfg)
  val data_pump = DataPump_mm2s(cfg)
  val io = new Bundle {
    val mm_write_port = slave(MemoryWritePort_TypeDef(cfg.mem_addr_width, cfg.mem_data_width))
    val TaskStream = slave(Stream (data_pump.Task_mm2s_Type()))
    val DataStream = master(Stream(data_pump.Data_mm2s_Type()))
  }

  // 连接 DataPump 的 MemoryReadPort 到 sdpram
  data_pump.io.MemoryReadPort <> sdpram.io.read
  data_pump.io.TaskStream <> io.TaskStream
  data_pump.io.DataStream <> io.DataStream

  // 连接 sdpram 的写接口最外层
  sdpram.io.write <> io.mm_write_port
}

//!仿真程序主体
object DataPump_mm2s_Sim extends App {
//  val flags = VCSFlags(
//    compileFlags = List(
//      "-kdb -work xil_defaultlib",
//      "/home/cotr/Workspace/Xilinx_IP_lib/glbl.v"
//    ),
//    elaborateFlags = List(
//      "-LDFLAGS -Wl,--no-as-needed",
//////    "-fgp",
//////    "-kdb",
//////    "-lca",
//////     "+rad",
//////    "+notimingchecks",
//      "xil_defaultlib.glbl"
//    ),
//    runFlags = List("-l ./run.log")
//  )
val flags = VCSFlags(
  compileFlags = List("-kdb","-lca", "+notimingchecks"),
  elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"),
  runFlags = List("-l ./run.log")
)
  val cfg_noPad = DataPump_mm2s_Config(Enable_Padding_logic = false)
  val cfg_Pad = DataPump_mm2s_Config(Enable_Padding_logic = true)
  var compiled = SimConfig
    .withVCS(flags)
//    .withVCSSimSetup(
//      setupFile = "./synopsys_sim.setup",
//      beforeAnalysis = null
//    )
    .withTimePrecision(1 ps)
    .withFSDBWave
    .withConfig(
      SpinalConfig(
        bitVectorWidthMax = 20000,
        removePruned = true
      )
    )
    .compile(new DataPump_mm2s_test_Top(cfg_noPad))
  // !单任务测试
  compiled.doSim("single task") { dut =>
    SimTimeout(6000)
    // 定义写入任务
    val startAddr = 0
    val repeatNum = 13
    val offset = 1
    dut.clockDomain.forkStimulus(period = 10)
    // 初始化信号
    dut.io.mm_write_port.Valid #= false
    dut.io.mm_write_port.Address #= 0
    sleep(200)
    // 随机化输入写入
    val random = new Random()
    val addressDataMap = (0 until 256).map { i =>
      val data = random.nextInt(1000) // 随机生成数据
      dut.io.mm_write_port.Valid #= true
      dut.io.mm_write_port.Address #= i
      dut.io.mm_write_port.Data #= data
      sleep(10)
      (i, data)
    }.toMap

    dut.io.mm_write_port.Valid #= false
    dut.io.DataStream.ready #= true
    sleep(10)

    // 发送写入任务,send once
    var TaskSend = false
    StreamDriver(dut.io.TaskStream, dut.clockDomain) { payload =>
      if (!TaskSend) {
        payload.StartAddr #= startAddr
        payload.RepeatNum #= repeatNum
        payload.Offset #= offset
        TaskSend = true
        true
      } else {
        // do nothing
        false
      }

    }
    StreamReadyRandomizer(dut.io.DataStream, dut.clockDomain)

    // 检查输出并计算读出地址
    var readCounter = 0
    StreamMonitor(dut.io.DataStream, dut.clockDomain) { payload =>
      val readData = payload.data.toInt
      if (readCounter < repeatNum) {
        val readAddr = startAddr + readCounter * offset.toInt
        val expectedData = addressDataMap.getOrElse(readAddr, -1)
        // 断言检测
        assert(
          readData == expectedData,
          s"Data mismatch: Address=$readAddr Read data is $readData, expected data is $expectedData"
        )
        println(s"Read data: Address=$readAddr Data=$readData")

        // 更新计数器以计算下一个地址
        if (payload.Final.toBoolean) { readCounter = 0 }
        else { readCounter += 1 }
      } else { SpinalError(s"unexpected data: $readData") }

    }
    sleep(1000)
    simSuccess()
  }
  // !多任务测试
  compiled.doSim("multiple tasks") { dut =>
    SimTimeout(60000)
    dut.clockDomain.forkStimulus(period = 10)

    // Initialize signals
    dut.io.mm_write_port.Valid #= false
    dut.io.mm_write_port.Address #= 0
    sleep(200)

    // Randomize input data for memory write
    val random = new Random()
    val addressDataMap = (0 until 512).map { i =>
      val data = random.nextInt(1000) // Random data generation
      dut.io.mm_write_port.Valid #= true
      dut.io.mm_write_port.Address #= i
      dut.io.mm_write_port.Data #= data
      sleep(10)
      (i, data)
    }.toMap
    dut.io.mm_write_port.Valid #= false
    dut.io.DataStream.ready #= true
    sleep(10)

    // Task generation and data check
    val numTasks = 20 // Number of random tasks to test
    var currentTask = 0
    val tasks = Array.fill(numTasks) {
      val startAddr = random.nextInt(256) // ranging from 0 to 255
      val repeatNum = random.nextInt(10) + 1 // ranging from 1 to 10
      val offset = random.nextInt(5) + 1 // ranging from 1 to 5
      (startAddr, repeatNum, offset)
    }

    // Send random tasks to TaskStream
    StreamDriver(dut.io.TaskStream, dut.clockDomain) { payload =>
      if (currentTask < numTasks) {
        val (startAddr, repeatNum, offset) = tasks(currentTask)
        payload.StartAddr #= startAddr
        payload.RepeatNum #= repeatNum
        payload.Offset #= offset
        println(s"Sending task $currentTask: StartAddr=$startAddr, RepeatNum=$repeatNum, Offset=$offset")
        currentTask += 1
        true
      } else {
        false // No more tasks to send
      }
    }

    // Randomize DataStream readiness
    StreamReadyRandomizer(dut.io.DataStream, dut.clockDomain)

    // Monitor and verify output for each task
    var readCounter = 0
    var currentTaskIndex = 0

    StreamMonitor(dut.io.DataStream, dut.clockDomain) { payload =>
      val readData = payload.data.toInt
      if (currentTaskIndex < numTasks) {
        val (startAddr, repeatNum, offset) = tasks(currentTaskIndex)
        if (readCounter < repeatNum) {
          val readAddr = startAddr + readCounter * offset
          val expectedData = addressDataMap.getOrElse(readAddr, -1)

          // Assert data validity
          assert(
            readData == expectedData,
            s"Data mismatch in Task $currentTaskIndex: Address=$readAddr Read data=$readData, expected data=$expectedData"
          )
          println(s"Task $currentTaskIndex Read data: Address=$readAddr Data=$readData")

          // Update counter for the current task
          if (payload.Final.toBoolean) {
            // Move to the next task after finishing current one
            readCounter = 0
            currentTaskIndex += 1
            if (currentTaskIndex >= numTasks) {
              println("All tasks verified.")
              simSuccess()
            }
          } else { readCounter += 1 }
        } else {
          SpinalError(s"unexpected data: $readData")
        }
      }
    }
    simThread.suspend()
  }
  compiled = SimConfig
    .withVCS(flags)
//    .withVCSSimSetup(
//      setupFile = "./synopsys_sim.setup",
//      beforeAnalysis = null
//    )
    .withTimePrecision(1 ps)
    .withFSDBWave
    .withConfig(
      SpinalConfig(
        bitVectorWidthMax = 20000,
        removePruned = true
      )
    )
    .compile(new DataPump_mm2s_test_Top(cfg_Pad))
  // !Padding单任务测试
  compiled.doSim("single task Padding") { dut =>
    SimTimeout(6000)
    // 定义写入任务
    val startAddr = 0
    val validNum = 9
    val data_to_Pad= 1024
    val repeatNum = 13
    val offset = 1
    dut.clockDomain.forkStimulus(period = 10)
    // 初始化信号
    dut.io.mm_write_port.Valid #= false
    dut.io.mm_write_port.Address #= 0
    sleep(200)
    // 随机化输入写入
    val random = new Random()
    val addressDataMap = (0 until 256).map { i =>
      val data = random.nextInt(1000) // 随机生成数据
      dut.io.mm_write_port.Valid #= true
      dut.io.mm_write_port.Address #= i
      dut.io.mm_write_port.Data #= data
      sleep(10)
      (i, data)
    }.toMap

    dut.io.mm_write_port.Valid #= false
    dut.io.DataStream.ready #= true
    sleep(10)

    // 发送写入任务,send once
    var TaskSend = false
    StreamDriver(dut.io.TaskStream, dut.clockDomain) { payload =>
      if (!TaskSend) {
        payload.StartAddr #= startAddr
        payload.ValidNum #= validNum
        payload.data_to_Pad#=data_to_Pad
        payload.RepeatNum #= repeatNum
        payload.Offset #= offset
        TaskSend = true
        true
      } else {
        // do nothing
        false
      }

    }
    StreamReadyRandomizer(dut.io.DataStream, dut.clockDomain)

    // 检查输出并计算读出地址
    var readCounter = 0
    StreamMonitor(dut.io.DataStream, dut.clockDomain) { payload =>
      val readData = payload.data.toInt
      if (readCounter < validNum) {
        val readAddr = startAddr + readCounter * offset.toInt
        val expectedData = addressDataMap.getOrElse(readAddr, -1)

        // 断言检测
        assert(
          readData == expectedData,
          s"Data mismatch: Address=$readAddr Read data is $readData, expected data is $expectedData"
        )
        println(s"Read data: Address=$readAddr Data=$readData")

        // 更新计数器以计算下一个地址
        readCounter += 1
      } else if (readCounter < repeatNum) {
        assert(
          readData == data_to_Pad,
          s"Data mismatch: Padding stage Read data is $readData, expected data is $data_to_Pad"
        )
        println(s"Padded Data=$readData")
      } else {
        readCounter = 0
        SpinalError("unexpected data")
      }

    }
    sleep(1000)
    simSuccess()
  }
}

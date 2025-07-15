package DataPump
import spinal.lib._
import spinal.lib.tools
import spinal.core
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{StreamMonitor, StreamDriver, StreamReadyRandomizer, ScoreboardInOrder}
import spinal.sim.VCSFlags
import scala.util.Random


case class DataPump_s2mm_sdpram_for_test(cfg: DataPump_s2mm_Config) extends Component {
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

case class DataPump_s2mm_test_Top(cfg: DataPump_s2mm_Config) extends Component {
  // 实例化 sdpram 和 DataPump_s2mm
  val sdpram = DataPump_s2mm_sdpram_for_test(cfg)
  val data_pump = DataPump_s2mm(cfg)
  val io = new Bundle {
    val mm_read_port = slave(MemoryReadPort_TypeDef(cfg.mem_addr_width,cfg.mem_data_width))
    val TaskStream = slave Stream (data_pump.Task_s2mm_Type)
    val DataStream = slave Stream (data_pump.Data_s2mm_Type)
    val ErrorPort = cfg.Enable_Error_Port_logic generate (out(data_pump.Error_Type))
  }

  // 连接 DataPump 的 MemoryWritePort 到 sdpram 的写接口
  sdpram.io.write <> data_pump.io.MemoryWritePort

  // 连接 TaskStream 和 DataStream 接口
  data_pump.io.TaskStream <> io.TaskStream
  data_pump.io.DataStream <> io.DataStream
  data_pump.io.ErrorPort <> io.ErrorPort
  // 连接 sdpram 的读接口最外层
  sdpram.io.read <> io.mm_read_port
}
//!仿真程序主体
object DataPump_s2mm_Sim extends App {
  val flags = VCSFlags(
    compileFlags = List(
      // "-kdb -work xil_defaultlib"
      // "/home/cotr/Workspace/Xilinx_IP_lib/glbl.v"
    ),
    elaborateFlags = List(
      "-LDFLAGS -Wl,--no-as-needed",
      // "xil_defaultlib.glbl"
    ),
    runFlags = List("-l ./run.log")
  )

  val cfg_noUnPad = DataPump_s2mm_Config(Enable_UnPadding_logic=false)
  var compiled=SimConfig
    .withVCS(flags)
    // .withVCSSimSetup(
    //   setupFile = "./synopsys_sim.setup",
    //   beforeAnalysis = null
    // )
    .withTimePrecision(1 ps)
    .withFSDBWave
    .withConfig(
      SpinalConfig(
        bitVectorWidthMax = 20000,
        removePruned = true
      )
    )
    .compile(new DataPump_s2mm_test_Top(cfg_noUnPad))
    //!单任务测试
    compiled.doSim("single task") { dut =>
      SimTimeout(6000)
      dut.clockDomain.forkStimulus(period = 10)
      // 定义写入任务
      val startAddr = 0
      val repeatNum = 9
      val offset = 1
      // 初始化信号
      dut.io.mm_read_port.Valid #= false
      dut.io.mm_read_port.Address #= 0
      sleep(200)
      // 随机化生成地址和数据的映射表
      val random = new Random()
      val addressDataMap = (0 until 256).map { i =>
        val data = random.nextInt(1000) // 随机生成数据
        (i, data)
      }.toMap

      dut.io.mm_read_port.Valid #= false
      dut.io.DataStream.valid #= false
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
      // 写入数据流
      var writeCounter = 0
      StreamDriver(dut.io.DataStream, dut.clockDomain) { payload =>
        if (writeCounter < repeatNum) {
          val writeAddr = startAddr + writeCounter * offset
          val data_to_write = addressDataMap.getOrElse(writeAddr, 0)
          payload.data #= data_to_write
          writeCounter += 1
          if(writeCounter==repeatNum){
            payload.Final #= true
          }else{
            payload.Final #= false
          }
          println(s"Write data to stream: Data=${data_to_write}")
        } else {}
        true
      }
      sleep(1000)
      // 检查读出的数据
      for (readCounter <- startAddr until repeatNum) {
        dut.io.mm_read_port.Valid #= true
        val readAddr = startAddr + readCounter * offset // 从地址 i 读取
        dut.io.mm_read_port.Address #= readAddr // 从地址 i 读取
        sleep(10)
        val result = dut.io.mm_read_port.Data.toInt
        val ref = addressDataMap.getOrElse(readAddr, -1)
        assert(
          result == ref,
          s"\nData mismatch: Read value from address $readAddr is $result did not match written value $ref!"
        )
        println(s"Received data: Address=$readAddr Data=$result")
      }
      sleep(1000)
      simSuccess()
    }
    //!多任务测试
    compiled.doSim("multiple tasks") { dut =>
      SimTimeout(60000)
      dut.clockDomain.forkStimulus(period = 10)

      // Initialize signals
      dut.io.mm_read_port.Valid #= false
      dut.io.mm_read_port.Address #= 0
      sleep(200)

      // Create a map of randomized address data
      val random = new Random()
      val addressDataMap = (0 until 512).map { i =>
        val data = random.nextInt(1000) // Randomly generated data
        (i, data)
      }.toMap

      dut.io.mm_read_port.Valid #= false
      dut.io.DataStream.valid #= false
      sleep(10)

      // Generate multiple randomized tasks
      val numTasks = 20
      var currentTask = 0
      val tasks = Array.fill(numTasks) {
        val startAddr = random.nextInt(256)
        val repeatNum = random.nextInt(10) + 1
        val offset = random.nextInt(5) + 1
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


      // Write data to DataStream based on each task
      var writeCounter = 0
      var currentTaskIndex = 0
      var data_writing = true
      // Verification phase
      var readCounter = 0
      var taskVerificationIndex = 0
      var request_for_data=true
      var wait_for_data=false
      


      // Data writing phase
      StreamDriver(dut.io.DataStream, dut.clockDomain) { payload =>
        assert(
                dut.io.ErrorPort.Wrong_DataStream_Final.toBoolean == false,
                s"\nError Detected: Wrong_DataStream_Final !"
              )
        if (currentTaskIndex < numTasks) {
          // Write data for the current task
          if(data_writing){
            //write data
            val (startAddr, repeatNum, offset) = tasks(currentTaskIndex)
            if (writeCounter < repeatNum) {
              val writeAddr = startAddr + writeCounter * offset
              val dataToWrite = addressDataMap.getOrElse(writeAddr, 0)
              payload.data #= dataToWrite
              writeCounter += 1
              if(writeCounter==repeatNum){
                payload.Final #= true
              }else{
                payload.Final #= false
              }
              println(s"Write data to stream: Address=$writeAddr Data=$dataToWrite")
              true
            } else {
              // Move to the next task after finishing the current one
              writeCounter = 0
              currentTaskIndex += 1
              data_writing = false
              if (currentTaskIndex >= numTasks) {
                println("data of this task has been sent.Starting verification phase")
              }
              false
            }
          }else{
            ////println("DataStream is stopped.")
            // Verification phase
      if(taskVerificationIndex < numTasks)
      {if(!data_writing)
        {
            //verify data
            
            val (startAddr, repeatNum, offset) = tasks(taskVerificationIndex)
            if (readCounter < repeatNum) {
              val readAddr = startAddr + readCounter * offset
              if(request_for_data){
              dut.io.mm_read_port.Valid #= true
              dut.io.mm_read_port.Address #= readAddr
              request_for_data=false
              wait_for_data=true
            }else if(wait_for_data)
              {
                ////println(s"wait a period")
                request_for_data=false
              wait_for_data=false
              }
              else{
              
              val result = dut.io.mm_read_port.Data.toInt
              val expectedData = addressDataMap.getOrElse(readAddr, -1)
              request_for_data=true
              wait_for_data=false
              // Assertion to check data validity
              assert(
                result == expectedData,
                s"Data mismatch: Address=$readAddr, Read data=$result, Expected data=$expectedData"
              )
              
              println(s"Read data from address $readAddr: Data=$result")

              // Increment counter
              readCounter += 1}
            }else
            {// Move to next task
            readCounter = 0
            taskVerificationIndex += 1
            data_writing = true
            println(s"Verification completed for task $taskVerificationIndex,Restarting data writing.")}
            
        }
      }
            false
          }
        } else {
          println("All tasks have been sent.")
          simSuccess()
          false // No more data to write
        }
        
      }
      simThread.suspend()
    }
    val cfg_UnPad = DataPump_s2mm_Config(Enable_UnPadding_logic=true,Enable_Error_Port_logic=true)
    compiled=SimConfig
    .withVCS(flags)
    // .withVCSSimSetup(
      // setupFile = "./synopsys_sim.setup",
      // beforeAnalysis = null
    // )
    .withTimePrecision(1 ps)
    .withFSDBWave
    .withConfig(
      SpinalConfig(
        bitVectorWidthMax = 20000,
        removePruned = true
      )
    )
    .compile(new DataPump_s2mm_test_Top(cfg_UnPad))
    //!UnPadding单任务测试
    compiled.doSim("single task UnPadding") { dut =>
      SimTimeout(6000)
      dut.clockDomain.forkStimulus(period = 10)
      // 定义写入任务
      val startAddr = 0
      val validNum = 9
      val repeatNum = 13
      val offset = 1
      // 初始化信号
      dut.io.mm_read_port.Valid #= false
      dut.io.mm_read_port.Address #= 0
      sleep(200)
      // 随机化生成地址和数据的映射表
      val random = new Random()
      val addressDataMap = (0 until 256).map { i =>
        val data = random.nextInt(1000) // 随机生成数据
        (i, data)
      }.toMap

      dut.io.mm_read_port.Valid #= false
      dut.io.DataStream.valid #= false
      sleep(10)

      // 发送写入任务,send once
      var TaskSend = false
      StreamDriver(dut.io.TaskStream, dut.clockDomain) { payload =>
        if (!TaskSend) {
          payload.StartAddr #= startAddr
          payload.ValidNum #= validNum
          payload.RepeatNum #= repeatNum
          payload.Offset #= offset
          TaskSend = true
          true
        } else {
          // do nothing
          false
        }

      }
      // 写入数据流
      var writeCounter = 0
      StreamDriver(dut.io.DataStream, dut.clockDomain) { payload =>
        if (writeCounter < validNum) {
          val writeAddr = startAddr + writeCounter * offset
          val data_to_write = addressDataMap.getOrElse(writeAddr, 0)
          payload.data #= data_to_write
          writeCounter += 1
          if(writeCounter==repeatNum){
            payload.Final #= true
          }else{
            payload.Final #= false
          }
          println(s"Write data to stream: Data=${data_to_write}")
          true
        } else if(writeCounter<repeatNum){
          val data_to_write = 0
          payload.data #= data_to_write
          writeCounter += 1
          if(writeCounter==repeatNum){
            payload.Final #= true
          }else{
            payload.Final #= false
          }
          println(s"Write 0 to stream")
          true
        }else{
          writeCounter = 0
          false
        }
      }
      assert(
          dut.io.ErrorPort.Not_Valid_and_Not_Zero.toBoolean == false,
          s"\nError Detected: Not_Valid_and_Not_Zero !"
        )
        assert(
          dut.io.ErrorPort.Wrong_DataStream_Final.toBoolean == false,
          s"\nError Detected: Wrong_DataStream_Final !"
        )
      sleep(1000)
      // 检查读出的数据
      for (readCounter <- startAddr until repeatNum) {
        dut.io.mm_read_port.Valid #= true
        val readAddr = startAddr + readCounter * offset // 从地址 i 读取
        dut.io.mm_read_port.Address #= readAddr // 从地址 i 读取
        sleep(10)
        val result = dut.io.mm_read_port.Data.toInt
        val ref = addressDataMap.getOrElse(readAddr, -1)
        if(readCounter<validNum){
        assert(
          result == ref,
          s"\nData mismatch: Read value from address $readAddr is $result did not match written value $ref!"
        )}
        
        println(s"Received data: Address=$readAddr Data=$result")
      }
      sleep(1000)
      simSuccess()
    }

}

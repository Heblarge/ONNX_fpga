package DataPump
import spinal.lib._
import spinal.lib.tools
import spinal.lib.fsm._
import spinal.core
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{StreamMonitor, StreamDriver, StreamReadyRandomizer, ScoreboardInOrder}
import spinal.sim.VCSFlags
import scala.util.Random

import Interface._

case class DataPump_s2mm_Config(
    // memory port parameters
    mem_data_width: Int = 32 * 16,
    mem_addr_width: Int = 16,
    // Task parameters
    RepeatNum_Max: Int = 32,
    Enable_UnPadding_logic: Boolean = false, // 使能除去无效位的逻辑
    //这种模式下可以配置RepeatNum>ValidNum,那么这个任务会读取RepeatNum个stream数据包，只保留ValidNum包个有效数据生成memory写入请求
    Enable_Error_Port_logic:Boolean = true // 使能错误输出
) {
  val RepeatNum_width = log2Up(RepeatNum_Max) + 1
}

case class DataPump_s2mm(cfg: DataPump_s2mm_Config) extends Component {
	// 便于直接使用对象的Task方法创建新的信号
  def Task_s2mm_Type(): Task_s2mm_TypeDef = {
    new Task_s2mm_TypeDef(cfg.mem_addr_width, cfg.mem_data_width, cfg.RepeatNum_width, cfg.Enable_UnPadding_logic)
  }
	// 便于直接使用对象的Data方法创建新的信号
  def Data_s2mm_Type(): Data_s2mm_TypeDef = {
    new Data_s2mm_TypeDef(cfg.mem_data_width)
  }
  def MemoryWritePort_Type(): MemoryWritePort_TypeDef = {
    new MemoryWritePort_TypeDef(cfg.mem_addr_width, cfg.mem_data_width)
  }
  case class Error_TypeDef(cfg: DataPump_s2mm_Config) extends Bundle {
    val Not_Valid_and_Not_Zero = cfg.Enable_UnPadding_logic generate Bool()
    val Wrong_DataStream_Final = Bool()
  }
  def Error_Type(): Error_TypeDef = {
    new Error_TypeDef(cfg)
  }
  val io = new Bundle {
    val MemoryWritePort = master(MemoryWritePort_Type())
    val TaskStream = slave Stream (Task_s2mm_Type())// Assuming that TaskStream.valid will keep until TaskStream.ready is Given
    val DataStream = slave Stream (Data_s2mm_Type())
    val ErrorPort = cfg.Enable_Error_Port_logic generate out(Error_TypeDef(cfg))
  }
	// 内部缓存任务寄存器初始化定义，其实就是全部填0
  val Task_Reg_init = Task_s2mm_Type().init_to_zero()
  val Task_Reg = Reg(Task_s2mm_Type()) init (Task_Reg_init)

	val TaskStream_ready = Reg(Bool())
	val MemoryWritePort_Valid = (Reg(Bool()) init False)
  val DataStream_ready = Reg(Bool())
  
  io.DataStream.ready := DataStream_ready
  io.TaskStream.ready := TaskStream_ready
  io.MemoryWritePort.Valid := MemoryWritePort_Valid
  val MemoryWritePort_Data = Reg(Bits(cfg.mem_data_width bits)) init 0
  io.MemoryWritePort.Data := MemoryWritePort_Data
	val data_is_valid = Reg(Bool()) init True

  val Address_now = Reg(UInt(cfg.mem_addr_width bits)) init (0)
  val Address_next = Reg(UInt(cfg.mem_addr_width bits)) init (0)
  io.MemoryWritePort.Address := Address_now

  val fsm = new StateMachine {
    val counter = Reg(UInt(cfg.RepeatNum_width bits)) init (0)
    val Idle = new State with EntryPoint
    val LoadingTask = new State
    val Pumping = new State
		val UnPadding = cfg.Enable_UnPadding_logic generate new State
    val TaskDone = new State

    Idle.whenIsActive {
      TaskStream_ready := True
      DataStream_ready := False
      MemoryWritePort_Valid := False
      MemoryWritePort_Data := 0
      when(io.TaskStream.valid) {
        goto(LoadingTask)
      }.otherwise {
        counter := 0
        Address_now := 0
        Address_next := 0
      }
    }

    LoadingTask.onEntry {
      Task_Reg := io.TaskStream.payload
      TaskStream_ready := False
    }

    LoadingTask.whenIsActive {
      counter := Task_Reg.RepeatNum // Load repeat counter
      Address_now := Task_Reg.StartAddr // Load start address
      Address_next := Task_Reg.StartAddr
      DataStream_ready := False
      if (cfg.Enable_UnPadding_logic) {
				when(Task_Reg.ValidNum === 0) {
					goto(UnPadding)
				} otherwise {
					goto(Pumping)
				}
			} else {
				goto(Pumping)
			}
    }

    Pumping.whenIsActive {
      TaskStream_ready := False
      DataStream_ready := True
      MemoryWritePort_Valid := io.DataStream.fire // Write data if DataStream is valid

      when(counter === 0) {
        goto(TaskDone)
      }.otherwise {
				if (cfg.Enable_UnPadding_logic) {
					when(counter === Task_Reg.RepeatNum - Task_Reg.ValidNum + 1) {
						when(io.DataStream.fire) {
						goto(UnPadding)
						}
					} otherwise {
						
					}
				}
        when(io.DataStream.fire) {
          // Write data to memory
          MemoryWritePort_Data := io.DataStream.payload.data
          counter := counter - 1
          Address_now := Address_next
          Address_next := Address_next + Task_Reg.Offset
        }
      }
    }
		if (cfg.Enable_UnPadding_logic) {
      UnPadding.onEntry {}
      UnPadding.whenIsActive {

        TaskStream_ready := False
        DataStream_ready := True
        MemoryWritePort_Valid := False
        when(counter === 0) {
          // send data and wait ack
          when(io.DataStream.fire) {
            goto(TaskDone)
          }
        }.otherwise {
          // !differ from Pumping,UnPadding do not generate MM write request
          // send data and wait ack
          when(io.DataStream.fire) {
            // read next data
            data_is_valid := False
            counter := counter - 1
            Address_now := 0
            Address_next := 0
          }
        }
      }
    }

    TaskDone.onEntry {
      DataStream_ready := False
      TaskStream_ready := True
      MemoryWritePort_Valid := False
    }

    TaskDone.whenIsActive {
      counter := 0
      Address_now := 0
      Address_next := 0
      when(io.TaskStream.valid) {
        goto(LoadingTask)
        Task_Reg := io.TaskStream.payload
      } otherwise {
        goto(Idle)
      }
    }
  }
  //error reporting logic
  if(cfg.Enable_Error_Port_logic)
  {
    if(cfg.Enable_UnPadding_logic)
    {
      io.ErrorPort.Not_Valid_and_Not_Zero:=
        (fsm.isActive(fsm.UnPadding))&&
        (io.DataStream.fire)&&
        (io.DataStream.payload.data=/=0)
    }
    io.ErrorPort.Wrong_DataStream_Final:=
      (((fsm.counter === 1)&&(io.DataStream.fire)&&(io.DataStream.payload.Final===False))
    ||((fsm.counter =/= 1)&&(io.DataStream.fire)&&(io.DataStream.payload.Final===True)))

  }
}

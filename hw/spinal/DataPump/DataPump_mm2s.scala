package DataPump
import spinal.lib._
import spinal.lib.fsm._
import spinal.core
import spinal.core._

import Interface._

case class DataPump_mm2s_Config(
    // memory port param
    mem_data_width: Int = 32 * 16,
    mem_addr_width: Int = 16,
    // Task param
    RepeatNum_Max: Int = 32,
    Enable_Padding_logic: Boolean = false // 使能对非有效位进行填充的逻辑
) {
  val RepeatNum_width = log2Up(RepeatNum_Max) + 1
}

case class DataPump_mm2s(cfg: DataPump_mm2s_Config) extends Component {
  // 便于直接使用对象的Task方法创建新的信号
  def Task_mm2s_Type(): Task_mm2s_TypeDef = {
    new Task_mm2s_TypeDef(cfg.mem_addr_width, cfg.mem_data_width, cfg.RepeatNum_width, cfg.Enable_Padding_logic)
  }
  // 便于直接使用对象的Data方法创建新的信号
  def Data_mm2s_Type(): Data_mm2s_TypeDef = {
    new Data_mm2s_TypeDef(cfg.mem_data_width)
  }
  def MemoryReadPort_Type(): MemoryReadPort_TypeDef = {
    new MemoryReadPort_TypeDef(cfg.mem_addr_width, cfg.mem_data_width)
  }
  val io = new Bundle {
    val MemoryReadPort = master(MemoryReadPort_TypeDef(cfg.mem_addr_width, cfg.mem_data_width))
    val TaskStream = slave Stream (Task_mm2s_Type()) // Assuming that TaskStream.valid will keep until TaskStream.ready is Given
    val DataStream = master(Stream(Data_mm2s_Type()))
  }

  // 内部缓存任务寄存器初始化定义，其实就是全部填0
  val Task_Reg_init = Task_mm2s_Type().init_to_zero()
  val Task_Reg = Reg(Task_mm2s_Type()) init (Task_Reg_init)

  val TaskStream_ready = Reg(Bool())
  val MemoryReadPort_Valid = Reg(Bool()) init False
  val DataStream_valid = Reg(Bool()) init False
  io.DataStream.valid := DataStream_valid
  io.MemoryReadPort.Valid := MemoryReadPort_Valid

  val data_is_valid = Reg(Bool()) init True
  if(cfg.Enable_Padding_logic)
  {
    io.DataStream.payload.data := data_is_valid ? io.MemoryReadPort.Data | Task_Reg.data_to_Pad
    // 如果填充逻辑被使能data_is_valid生成内存请求。无效但依然Repeat的位会被自定义值(data_to_Pad)填充
  }
  else 
  {
    io.DataStream.payload.data := io.MemoryReadPort.Data
  }

  val next_address_castNext = io.DataStream.fire
  val Address_now = Reg(UInt(cfg.mem_addr_width bits)) init (0)
  val Address_next = Reg(UInt(cfg.mem_addr_width bits)) init (0)
  io.MemoryReadPort.Address := next_address_castNext ? Address_next | Address_now

  io.TaskStream.ready := TaskStream_ready
  val fsm = new StateMachine {
    val counter = Reg(UInt(cfg.RepeatNum_width bits)) init (0)
    val Idle = new State with EntryPoint
    val LoadingTask = new State
    val Pumping = new State
    val Padding = cfg.Enable_Padding_logic generate new State
    val TaskDone = new State

    Idle.whenIsActive {
      TaskStream_ready := True
      DataStream_valid := False
      MemoryReadPort_Valid := False
      when(io.TaskStream.valid) {
        goto(LoadingTask)
      }.otherwise {
        counter := 0
        MemoryReadPort_Valid := False
        Address_now := 0
        Address_next := 0

      }
    }
    LoadingTask.onEntry {
      Task_Reg := io.TaskStream.payload
      TaskStream_ready := False
      data_is_valid := True
      counter := io.TaskStream.payload.RepeatNum
    }
    LoadingTask.whenIsActive {
      when(counter === 0) {
        goto(TaskDone)
      } otherwise {
        counter := counter - 1
        Address_now := Task_Reg.StartAddr // load StartAddr
        Address_next := Task_Reg.StartAddr + Task_Reg.Offset
        if (cfg.Enable_Padding_logic) {
          when(Task_Reg.ValidNum === 0) {
            goto(Padding)
          } otherwise {
            goto(Pumping)
          }
        } else {
          goto(Pumping)
        }
      }
    }
    Pumping.onEntry {
      MemoryReadPort_Valid := True
    }
    Pumping.whenIsActive {
      TaskStream_ready := False
      DataStream_valid := True
      when(counter === 0) {
        // send data and wait ack
        when(io.DataStream.fire) {
          goto(TaskDone)
        }
      }otherwise {
				if (cfg.Enable_Padding_logic) {
					when(counter === Task_Reg.RepeatNum - Task_Reg.ValidNum + 1) {
						when(io.DataStream.fire) {
						goto(Padding)
						}
					} otherwise {
						MemoryReadPort_Valid := True
					}
				}
				// send data and wait ack
				when(io.DataStream.fire) {
				// read next data
				counter := counter - 1
				Address_now := Address_next
				Address_next := Address_next + Task_Reg.Offset
				}
			}
    }
    Pumping.onExit {
      MemoryReadPort_Valid := False
    }
    if (cfg.Enable_Padding_logic) {
      Padding.onEntry {}
      Padding.whenIsActive {

        TaskStream_ready := False
        DataStream_valid := True
        MemoryReadPort_Valid := False
        when(counter === 0) {
          // send data and wait ack
          when(io.DataStream.fire) {
            goto(TaskDone)
          }
        }.otherwise {
          // !differ from Pumping,Padding do not generate MM read request
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
      DataStream_valid := False
      TaskStream_ready := True
      MemoryReadPort_Valid := False
      data_is_valid := False
    }
    TaskDone.whenIsActive {
      counter := 0
      Address_now := 0
      Address_next := 0
      when(io.TaskStream.valid) {
        goto(LoadingTask)
        Task_Reg := io.TaskStream.payload
      } otherwise { goto(Idle) }
    }
  }
  io.DataStream.payload.Final := (fsm.counter === 0)

}

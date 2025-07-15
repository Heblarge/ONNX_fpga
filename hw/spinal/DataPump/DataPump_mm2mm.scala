package DataPump
import spinal.lib._
import spinal.core
import spinal.core._
import DataPump._

import Interface._

case class DataPump_mm2mm_Config(
    // in/out data width
    data_width: Int = 32,
    // in memory-mapped(mm) ports params
    in_memory_addr_width: Int = 16,
    // out memory-mapped(mm) ports params
    out_memory_addr_width: Int = 16,
    RepeatNum_Max: Int = 1024

){
  val Pump_in_Cfg=DataPump_mm2s_Config(
    // memory port param
    mem_data_width= data_width,
    mem_addr_width= in_memory_addr_width,
    // Task param
    RepeatNum_Max= RepeatNum_Max,
    Enable_Padding_logic= true // 使能对非有效位进行填充的逻辑
  )
  val Pump_out_Cfg=DataPump_s2mm_Config(
    // memory port param
    mem_data_width= data_width,
    mem_addr_width= in_memory_addr_width,
    // Task param
    RepeatNum_Max= RepeatNum_Max,
    Enable_UnPadding_logic= true // 使能对非有效位进行填充的逻辑
  )
}
case class DataPump_mm2mm(cfg:DataPump_mm2mm_Config) extends Component{
  val Pump_in=DataPump_mm2s(cfg.Pump_in_Cfg)
  val Pump_out=DataPump_s2mm(cfg.Pump_out_Cfg)
  def MemoryReadPort_Type():MemoryReadPort_TypeDef={Pump_in.MemoryReadPort_Type()}
  def MemoryWritePort_Type():MemoryWritePort_TypeDef={Pump_out.MemoryWritePort_Type()}

  class Task_Pump_in_TypeDef(DataPump_mm2s_Cfg:DataPump_mm2s_Config) extends Bundle{
    val StartAddr = UInt(DataPump_mm2s_Cfg.mem_addr_width bits)
    val ValidNum = DataPump_mm2s_Cfg.Enable_Padding_logic generate UInt(DataPump_mm2s_Cfg.RepeatNum_width bits)
    val data_to_Pad = DataPump_mm2s_Cfg.Enable_Padding_logic generate Bits(DataPump_mm2s_Cfg.mem_data_width bits)
    val Offset = UInt(DataPump_mm2s_Cfg.mem_addr_width bits) // will add offset to adder per repeat
  }
  class Task_Pump_out_TypeDef(DataPump_s2mm_Cfg:DataPump_s2mm_Config) extends Bundle{
    val StartAddr = UInt(DataPump_s2mm_Cfg.mem_addr_width bits)
    val ValidNum = DataPump_s2mm_Cfg.Enable_UnPadding_logic generate UInt(DataPump_s2mm_Cfg.RepeatNum_width bits)
    val Offset = UInt(DataPump_s2mm_Cfg.mem_addr_width bits) 
  }
  class Task_TypeDef(cfg:DataPump_mm2mm_Config) extends Bundle{
    val Task_Pump_in=new Task_Pump_in_TypeDef(cfg.Pump_in_Cfg)
    val Task_Pump_out=new Task_Pump_out_TypeDef(cfg.Pump_out_Cfg)
    val RepeatNum=UInt(log2Up(cfg.RepeatNum_Max) bits)
    def to_Pump_in_Task():Task_mm2s_TypeDef={
      val task=Pump_in.Task_mm2s_Type()
      task.RepeatNum:=RepeatNum
      task.StartAddr:=Task_Pump_in.StartAddr
      task.ValidNum:=Task_Pump_in.ValidNum
      task.Offset:=Task_Pump_in.Offset
      if(cfg.Pump_in_Cfg.Enable_Padding_logic)
      {task.data_to_Pad:=Task_Pump_in.data_to_Pad}
      task
    }
    def to_Pump_out_Task():Task_s2mm_TypeDef={
      val task=Pump_out.Task_s2mm_Type()
      task.RepeatNum:=RepeatNum
      task.StartAddr:=Task_Pump_out.StartAddr
      task.ValidNum:=Task_Pump_out.ValidNum
      task.Offset:=Task_Pump_out.Offset
      task
    }
  }
  def Task_Type():Task_TypeDef={new Task_TypeDef(cfg)}

  val io=new Bundle
  {
    val MemoryReadPort=master(MemoryReadPort_Type())
    val MemoryWritePort=master(MemoryWritePort_Type())
    val Task_in=slave(Stream(Task_Type()))
    val Task_out=master(Stream(Task_Type()))
  }
  //connect Pump_in/Pump_out to io
  Pump_in.io.MemoryReadPort<>io.MemoryReadPort
  Pump_out.io.MemoryWritePort<>io.MemoryWritePort
  //connect Pump_in to Pump_out
  val convert_to_Pump_out=Pump_in.io.DataStream.map(payload=>{
    val turn_to=Pump_out.Data_s2mm_Type()
    turn_to.data:=payload.data
    turn_to.Final:=payload.Final
    turn_to
  })
  Pump_out.io.DataStream<>convert_to_Pump_out
  //instance Task_Queue
  val Task_Queue=StreamFifo(dataType=Task_Type(),depth=16)
  //connect io.Task_in to Task_Queue,Pump_in
  val Fork_Task_in= StreamFork2(io.Task_in, synchronous=true)
  val convert_to_Pump_in_Task=Fork_Task_in._1.map(in=>{
    val to_Pump_in_Task=in.to_Pump_in_Task()
    to_Pump_in_Task
  })
  Pump_in.io.TaskStream<>convert_to_Pump_in_Task
  Fork_Task_in._2>>Task_Queue.io.push
  //connect Task_Queue to io.Task_out,Pump_out
  val Fork_Task_out = StreamFork2(Task_Queue.io.pop, synchronous=true)
  val convert_to_Pump_out_Task=Fork_Task_out._1.map(out=>{
    val to_Pump_out_Task=out.to_Pump_out_Task()
    to_Pump_out_Task
  })
  Pump_out.io.TaskStream<>convert_to_Pump_out_Task
  Fork_Task_out._2>>io.Task_out
}
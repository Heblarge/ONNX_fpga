package Slicer

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import spinal.sim._
import scala.util._
import scala.math._
import scala.collection.mutable._

import Interface._

case class Collector_Config(
    UIDWidth: Int = 32,
    AddressWidth: Int = 16,
    ShapeWidth: Int = 16,
    SlicecntWidth: Int = 16,
    in_MatA_row_num: Int = 2, // 一次性从A侧输入的数的数量，也就是输入的A矩阵的行数
    in_MatB_col_num: Int = 2, // 一次性从B侧输入的数的数量，也就是输入的B矩阵的列数
    MatX_Width: Int = 2,
    Activation_x_Width: Int = 8,
    numCores: Int = 4
) {
  require(
    in_MatA_row_num == in_MatB_col_num,
    "Enable_ElementWise_logic: in_MatA_row_num must equal to in_MatB_col_num"
  )
  val MatAsub_row_num = in_MatA_row_num
  val MatBsub_col_num = in_MatB_col_num
  val Matin_col_num = MatX_Width
  val Matin_row_num = MatAsub_row_num * MatBsub_col_num / MatX_Width
  require(MatAsub_row_num * MatBsub_col_num % MatX_Width == 0, "Activation_num mismatch matrix shape")
  val mem_data_widthZ = MatBsub_col_num * Activation_x_Width
  val CoreSelectWidth = log2Up(numCores + 1)
}

case class Collector(cfg: Collector_Config) extends Component {
  def Sliced_ComputeInstruction_Type(): Sliced_ComputeInstruction_TypeDef = {
    new Sliced_ComputeInstruction_TypeDef(cfg.UIDWidth, cfg.AddressWidth, cfg.ShapeWidth)
  }
  def out_Mats_AfterActivation_Type(): out_Mats_AfterActivation_TypeDef = {
    new out_Mats_AfterActivation_TypeDef(cfg.MatX_Width, cfg.Activation_x_Width, cfg.UIDWidth, cfg.SlicecntWidth)
  }
  def Task_s2mm_Type(): Task_s2mm_TypeDef = {
    new Task_s2mm_TypeDef(cfg.AddressWidth, 0, 1, false)
  }
  def Data_s2mm_Type(): Data_s2mm_TypeDef = {
    new Data_s2mm_TypeDef(cfg.mem_data_widthZ)
  }

  val io = new Bundle {
    val Sliced_ComputeInstruction_Stream = slave Stream (Sliced_ComputeInstruction_Type())
    val Task_Stream = master Stream (Task_s2mm_Type())
    val Data_Stream = master Stream (Data_s2mm_Type())
    val Mats_from_Cores_Streams = Vec.fill(cfg.numCores)(slave Stream (out_Mats_AfterActivation_Type()))
  }

  val Sliced_ComputeInstruction_Reg = Reg(Sliced_ComputeInstruction_Type())
  val Sliced_ComputeInstruction_Stream_ready = Reg(Bool) init (True)
  io.Sliced_ComputeInstruction_Stream.ready := Sliced_ComputeInstruction_Stream_ready
  val MatZsub_write_finish = Bool()
  val inst_finish = Bool()
  when(io.Sliced_ComputeInstruction_Stream.fire) {
    Sliced_ComputeInstruction_Reg := io.Sliced_ComputeInstruction_Stream.payload
    Sliced_ComputeInstruction_Stream_ready := False
  } elsewhen (inst_finish) {
    Sliced_ComputeInstruction_Stream_ready := True
  }

  val Mats_Stream = Stream(out_Mats_AfterActivation_Type())
  val lock = Reg(Bool(), init = False)
  when(Mats_Stream.fire && !Mats_Stream.payload.Final) {
    lock := True
  } elsewhen (Mats_Stream.fire && Mats_Stream.payload.Final) {
    lock := False
  }

  val request = UInt(cfg.numCores bits)
  for (i <- 0 until cfg.numCores) {
    request(i) := io.Mats_from_Cores_Streams(i).valid &&
      io.Mats_from_Cores_Streams(i)
        .payload
        .CoreInstruction_AfterActivation
        .Collector_Instruction
        .UID === Sliced_ComputeInstruction_Reg.UID
  }
  val priority = Reg(UInt(cfg.numCores bits), init = U(1))
  when(!lock && request.orR) {
    priority := priority.rotateLeft(1)
  }
  val double_request = Cat(request, request).asUInt
  val request_sub_priority = double_request - priority
  val double_grant = double_request & ~request_sub_priority
  val grant = double_grant(0, cfg.numCores bits) | double_grant(cfg.numCores, cfg.numCores bits)

  var select_nolock = UInt(cfg.CoreSelectWidth bits)
  select_nolock := cfg.numCores
  for (i <- 0 until cfg.numCores) {
    when(grant(i)) {
      select_nolock \= U(i, cfg.CoreSelectWidth bits)
    }
  }
  val select_lock = RegNextWhen(select_nolock, !lock)
  val select = lock ? select_lock | select_nolock
  val Mats_from_Cores_Streams = Vec.fill(cfg.numCores + 1)(Stream(out_Mats_AfterActivation_Type()))
  for (i <- 0 until cfg.numCores) Mats_from_Cores_Streams(i) <> io.Mats_from_Cores_Streams(i)
  Mats_from_Cores_Streams(cfg.numCores).setIdle()
  Mats_Stream <> StreamMux(select, Mats_from_Cores_Streams)

  val Matoutsub_row_cnt = Reg(UInt(cfg.ShapeWidth bits))
  when(io.Sliced_ComputeInstruction_Stream.fire) {
    Matoutsub_row_cnt := 0
  } elsewhen (Mats_Stream.fire) {
    when(Matoutsub_row_cnt === cfg.Matin_row_num - 1) {
      Matoutsub_row_cnt := 0
    } otherwise {
      Matoutsub_row_cnt := Matoutsub_row_cnt + 1
    }
  }
  val Matoutsub_receive_finish = RegNext(Mats_Stream.fire && Matoutsub_row_cnt === cfg.Matin_row_num - 1, init = False)

  val Mats_Reg = Reg(out_Mats_AfterActivation_Type())
  when(Mats_Stream.fire) {
    Mats_Reg := Mats_Stream.payload
  }
  val Mats_Stream_ready = Reg(Bool()) init (False)
  Mats_Stream.ready := Mats_Stream_ready
  when(io.Sliced_ComputeInstruction_Stream.fire) {
    Mats_Stream_ready := True
  } elsewhen (MatZsub_write_finish && !inst_finish) {
    Mats_Stream_ready := True
  } elsewhen (Mats_Stream.fire && Matoutsub_row_cnt === cfg.Matin_row_num - 1) {
    Mats_Stream_ready := False
  }

  val MatA_row_slice_cnt = Reg(UInt(cfg.SlicecntWidth bits))
  val MatB_col_slice_cnt = Reg(UInt(cfg.SlicecntWidth bits))
  val Matoutsub_buffer = Reg(
    Vec.fill(cfg.Matin_row_num)(Vec.fill(cfg.Matin_col_num)(SInt(cfg.Activation_x_Width bits)))
  )
  when(RegNext(Mats_Stream.fire, init = False)) {
    when(RegNext(Matoutsub_row_cnt) === 0) {
      MatA_row_slice_cnt := Mats_Reg.CoreInstruction_AfterActivation.Collector_Instruction.MatA_row_slice_cnt
      MatB_col_slice_cnt := Mats_Reg.CoreInstruction_AfterActivation.Collector_Instruction.MatB_col_slice_cnt
    }
    Matoutsub_buffer(RegNext(Matoutsub_row_cnt).resized) := Mats_Reg.Activation_x
  }

  val MatZsub_row_cnt = Reg(UInt(cfg.ShapeWidth bits))
  when(io.Sliced_ComputeInstruction_Stream.fire) {
    MatZsub_row_cnt := 0
  } elsewhen (io.Data_Stream.fire) {
    when(MatZsub_row_cnt === cfg.MatAsub_row_num - 1) {
      MatZsub_row_cnt := 0
    } otherwise {
      MatZsub_row_cnt := MatZsub_row_cnt + 1
    }
  }
  MatZsub_write_finish := io.Data_Stream.fire && MatZsub_row_cnt === cfg.MatAsub_row_num - 1

  val Task_Reg = Reg(Task_s2mm_Type())
  io.Task_Stream.payload := Task_Reg
  Task_Reg.StartAddr := !Sliced_ComputeInstruction_Reg.doTranspose ?
    (Sliced_ComputeInstruction_Reg.outputAddress + ((MatA_row_slice_cnt * cfg.MatAsub_row_num + MatZsub_row_cnt) * Sliced_ComputeInstruction_Reg
      .outputShape(1) + (MatB_col_slice_cnt * cfg.MatBsub_col_num)) / cfg.MatBsub_col_num).resize(cfg.AddressWidth) |
    (Sliced_ComputeInstruction_Reg.outputAddress + ((MatB_col_slice_cnt * cfg.MatBsub_col_num + MatZsub_row_cnt) * Sliced_ComputeInstruction_Reg
      .outputShape(1) + (MatA_row_slice_cnt * cfg.MatAsub_row_num)) / cfg.MatAsub_row_num).resize(cfg.AddressWidth)
  Task_Reg.RepeatNum := 1
  Task_Reg.Offset := 0
  val Task_Stream_valid = Reg(Bool()) init (False)
  io.Task_Stream.valid := Task_Stream_valid
  when(io.Task_Stream.fire) {
    Task_Stream_valid := False
  } elsewhen (Matoutsub_receive_finish) {
    Task_Stream_valid := True
  } elsewhen (RegNext(io.Data_Stream.fire, init = False) && RegNext(MatZsub_row_cnt) =/= cfg.MatAsub_row_num - 1) {
    Task_Stream_valid := True
  }

  val MatZsub_buffer = Matoutsub_buffer.asBits.subdivideIn(cfg.mem_data_widthZ bits)
  val Data_Reg = Reg(Data_s2mm_Type())
  io.Data_Stream.payload := Data_Reg
  Data_Reg.data := MatZsub_buffer(MatZsub_row_cnt.resized)
  Data_Reg.Final := True
  val Data_Stream_valid = Reg(Bool()) init (False)
  io.Data_Stream.valid := Data_Stream_valid
  when(io.Data_Stream.fire) {
    Data_Stream_valid := False
  } elsewhen (io.Task_Stream.fire) {
    Data_Stream_valid := True
  }

  val MatZ_slice_num =
    Sliced_ComputeInstruction_Reg.outputShape(0) / cfg.MatAsub_row_num *
      Sliced_ComputeInstruction_Reg.outputShape(1) / cfg.MatBsub_col_num
  val MatZ_slice_cnt = Reg(UInt(2 * cfg.SlicecntWidth bits))
  when(io.Sliced_ComputeInstruction_Stream.fire) {
    MatZ_slice_cnt := 0
  } elsewhen (MatZsub_write_finish) {
    when(MatZ_slice_cnt === MatZ_slice_num - 1) {
      MatZ_slice_cnt := 0
    } otherwise {
      MatZ_slice_cnt := MatZ_slice_cnt + 1
    }
  }
  inst_finish := MatZsub_write_finish && MatZ_slice_cnt === MatZ_slice_num - 1

}

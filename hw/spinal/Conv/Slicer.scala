package Slicer

import Interface._

import spinal.core._
import spinal.lib._

case class SlicerCfg(
    UIDWidth: Int,
    ShiftWidth: Int,
    AddressWidth: Int,
    ShapeWidth: Int,
    SlicecntWidth: Int,
    matSubRowNum: Int,
    elementWidthA: Int,
    elementWidthB: Int,
    numCores: Int
) {
  val dataWidthA = matSubRowNum * elementWidthA
  val dataWidthB = matSubRowNum * elementWidthB
  val CoreSelectWidth = log2Up(numCores)
}

case class Slicer(slicerCfg: SlicerCfg) extends Component {
  def InstType =
    ComputeInstruction_TypeDef(
      UIDWidth = slicerCfg.UIDWidth,
      ShiftWidth = slicerCfg.ShiftWidth,
      AddressWidth = slicerCfg.AddressWidth,
      ShapeWidth = slicerCfg.ShapeWidth
    )

  def SlicedInstType =
    Sliced_ComputeInstruction_TypeDef(
      UIDWidth = slicerCfg.UIDWidth,
      AddressWidth = slicerCfg.AddressWidth,
      ShapeWidth = slicerCfg.ShapeWidth
    )

  def CoreInstType =
    CoreInstruction_TypeDef(
      ShiftWidth = slicerCfg.ShiftWidth,
      UIDWidth = slicerCfg.UIDWidth,
      SlicecntWidth = slicerCfg.SlicecntWidth
    )

  def InMatsType =
    in_Mats_TypeDef(
      in_MatA_row_num = slicerCfg.matSubRowNum,
      in_MatA_element_Width = slicerCfg.elementWidthA,
      in_MatB_col_num = slicerCfg.matSubRowNum,
      in_MatB_element_Width = slicerCfg.elementWidthB,
      ShiftWidth = slicerCfg.ShiftWidth,
      UIDWidth = slicerCfg.UIDWidth,
      SlicecntWidth = slicerCfg.SlicecntWidth
    )

  def ReadAddrType =
    Task_mm2s_TypeDef(
      mem_addr_width = slicerCfg.AddressWidth,
      mem_data_width = 0,
      RepeatNum_width = 1,
      Enable_Padding_logic = false
    )

  def ReadDataTypeA = Data_mm2s_TypeDef(slicerCfg.dataWidthA)

  def ReadDataTypeB = Data_mm2s_TypeDef(slicerCfg.dataWidthB)

  val io = new Bundle {
    val ComputeInstruction_Stream = slave Stream (InstType)
    val Sliced_ComputeInstruction_Stream = master Stream (SlicedInstType)
    val TaskA_Stream = master Stream (ReadAddrType)
    val DataA_Stream = slave Stream (ReadDataTypeA)
    val TaskB_Stream = master Stream (ReadAddrType)
    val DataB_Stream = slave Stream (ReadDataTypeB)
    val Mats_to_Cores_Streams = Vec.fill(slicerCfg.numCores)(master Stream (InMatsType))
  }

  val ComputeInstruction_Reg = Reg(InstType)
  val ComputeInstruction_Stream_ready = Reg(Bool()) init (True)
  io.ComputeInstruction_Stream.ready := ComputeInstruction_Stream_ready
  val is_MatMul = ComputeInstruction_Reg.matrixOperation === MatrixOperation_TypeDef.MatMul
  val inst_finish = Bool()
  val inst_finish_Reg = Reg(Bool, init = False)
  when(io.ComputeInstruction_Stream.fire) {
    inst_finish_Reg := False
  } elsewhen (inst_finish) {
    inst_finish_Reg := True
  }
  val Sliced_ComputeInstruction_Stream_valid = Reg(Bool()) init (False)
  when(io.ComputeInstruction_Stream.fire) {
    ComputeInstruction_Reg := io.ComputeInstruction_Stream.payload
    ComputeInstruction_Stream_ready := False
  } elsewhen (inst_finish_Reg && !Sliced_ComputeInstruction_Stream_valid) {
    ComputeInstruction_Stream_ready := True
  }

  val Sliced_ComputeInstruction_Reg = Reg(SlicedInstType)
  io.Sliced_ComputeInstruction_Stream.payload := Sliced_ComputeInstruction_Reg
  Sliced_ComputeInstruction_Reg.UID := ComputeInstruction_Reg.UID
  Sliced_ComputeInstruction_Reg.doTranspose := ComputeInstruction_Reg.doTranspose
  Sliced_ComputeInstruction_Reg.outputAddress := ComputeInstruction_Reg.outputAddress
  Sliced_ComputeInstruction_Reg.outputShape := ComputeInstruction_Reg.outputShape
  io.Sliced_ComputeInstruction_Stream.valid := Sliced_ComputeInstruction_Stream_valid
  when(io.Sliced_ComputeInstruction_Stream.fire) {
    Sliced_ComputeInstruction_Stream_valid := False
  } elsewhen (RegNext(io.ComputeInstruction_Stream.fire, init = False)) {
    Sliced_ComputeInstruction_Stream_valid := True
  }

  val MatA_row_slice_num = ComputeInstruction_Reg.input0Shape(0) / slicerCfg.matSubRowNum
  val MatB_col_slice_num = ComputeInstruction_Reg.input1Shape(1) / slicerCfg.matSubRowNum
  val MatA_col_slice_num = is_MatMul ? (ComputeInstruction_Reg.input0Shape(1) / slicerCfg.matSubRowNum) | 1
  val MatA_row_slice_cnt = Reg(UInt(slicerCfg.SlicecntWidth bits))
  val MatB_col_slice_cnt = Reg(UInt(slicerCfg.SlicecntWidth bits))
  val MatA_col_slice_cnt = Reg(UInt(slicerCfg.SlicecntWidth bits))
  val Matinsub_send_finish = Bool()
  val Matoutsub_send_finish = Bool()
  when(io.ComputeInstruction_Stream.fire) {
    MatA_row_slice_cnt := 0
    MatB_col_slice_cnt := 0
  } elsewhen (is_MatMul ? Matoutsub_send_finish | Matinsub_send_finish) {
    when(MatA_row_slice_cnt === MatA_row_slice_num - 1 && MatB_col_slice_cnt === MatB_col_slice_num - 1) {
      MatA_row_slice_cnt := 0
      MatB_col_slice_cnt := 0
    } elsewhen (MatB_col_slice_cnt === MatB_col_slice_num - 1) {
      MatA_row_slice_cnt := MatA_row_slice_cnt + 1
      MatB_col_slice_cnt := 0
    } otherwise {
      MatB_col_slice_cnt := MatB_col_slice_cnt + 1
    }
  }

  val MatAsub_row_cnt = Reg(UInt(slicerCfg.ShapeWidth bits))
  when(io.ComputeInstruction_Stream.fire) {
    MatAsub_row_cnt := 0
  } elsewhen (io.DataA_Stream.fire) {
    when(MatAsub_row_cnt === slicerCfg.matSubRowNum - 1) {
      MatAsub_row_cnt := 0
    } otherwise {
      MatAsub_row_cnt := MatAsub_row_cnt + 1
    }
  }

  val TaskA_Reg = Reg(ReadAddrType)
  io.TaskA_Stream.payload := TaskA_Reg
  TaskA_Reg.StartAddr := is_MatMul ?
    (ComputeInstruction_Reg.input0Address + ((MatA_row_slice_cnt * slicerCfg.matSubRowNum + MatAsub_row_cnt) * ComputeInstruction_Reg
      .input0Shape(1) + (MatA_col_slice_cnt * slicerCfg.matSubRowNum)) / slicerCfg.matSubRowNum)
      .resize(slicerCfg.AddressWidth) |
    (ComputeInstruction_Reg.input0Address + ((MatA_row_slice_cnt * slicerCfg.matSubRowNum + MatAsub_row_cnt) * ComputeInstruction_Reg
      .input0Shape(1) + (MatB_col_slice_cnt * slicerCfg.matSubRowNum)) / slicerCfg.matSubRowNum)
      .resize(slicerCfg.AddressWidth)
  TaskA_Reg.RepeatNum := 1
  TaskA_Reg.Offset := 0
  val TaskA_Stream_valid = Reg(Bool()) init (False)
  io.TaskA_Stream.valid := TaskA_Stream_valid
  when(io.TaskA_Stream.fire) {
    TaskA_Stream_valid := False
  } elsewhen (RegNext(io.ComputeInstruction_Stream.fire, init = False)) {
    TaskA_Stream_valid := True
  } elsewhen (RegNext(Matinsub_send_finish, init = False) && !RegNext(inst_finish, init = False)) {
    TaskA_Stream_valid := True
  } elsewhen (RegNext(io.DataA_Stream.fire, init = False) && RegNext(MatAsub_row_cnt) =/= slicerCfg.matSubRowNum - 1) {
    TaskA_Stream_valid := True
  }

  val DataA_Reg = Reg(ReadDataTypeA)
  val DataA_Stream_ready = Reg(Bool()) init (False)
  io.DataA_Stream.ready := DataA_Stream_ready
  when(io.DataA_Stream.fire) {
    DataA_Reg := io.DataA_Stream.payload
    DataA_Stream_ready := False
  } elsewhen (io.TaskA_Stream.fire) {
    DataA_Stream_ready := True
  }

  val MatAsub_buffer =
    Vec.fill(slicerCfg.matSubRowNum)(Vec.fill(slicerCfg.matSubRowNum)(Reg(Bits(slicerCfg.elementWidthA bits))))
  when(RegNext(io.DataA_Stream.fire, init = False)) {
    MatAsub_buffer(RegNext(MatAsub_row_cnt).resized) := DataA_Reg.data.subdivideIn(slicerCfg.elementWidthA bits)
  }

  val MatBsub_row_cnt = Reg(UInt(slicerCfg.ShapeWidth bits))
  when(io.ComputeInstruction_Stream.fire) {
    MatBsub_row_cnt := 0
  } elsewhen (io.DataB_Stream.fire) {
    when(MatBsub_row_cnt === slicerCfg.matSubRowNum - 1) {
      MatBsub_row_cnt := 0
    } otherwise {
      MatBsub_row_cnt := MatBsub_row_cnt + 1
    }
  }

  val TaskB_Reg = Reg(ReadAddrType)
  io.TaskB_Stream.payload := TaskB_Reg
  TaskB_Reg.StartAddr := is_MatMul ?
    (ComputeInstruction_Reg.input1Address + ((MatA_col_slice_cnt * slicerCfg.matSubRowNum + MatBsub_row_cnt) * ComputeInstruction_Reg
      .input1Shape(1) + (MatB_col_slice_cnt * slicerCfg.matSubRowNum)) / slicerCfg.matSubRowNum)
      .resize(slicerCfg.AddressWidth) |
    (ComputeInstruction_Reg.input1Address + ((MatA_row_slice_cnt * slicerCfg.matSubRowNum + MatBsub_row_cnt) * ComputeInstruction_Reg
      .input1Shape(1) + (MatB_col_slice_cnt * slicerCfg.matSubRowNum)) / slicerCfg.matSubRowNum)
      .resize(slicerCfg.AddressWidth)
  TaskB_Reg.RepeatNum := 1
  TaskB_Reg.Offset := 0
  val TaskB_Stream_valid = Reg(Bool()) init (False)
  io.TaskB_Stream.valid := TaskB_Stream_valid
  when(io.TaskB_Stream.fire) {
    TaskB_Stream_valid := False
  } elsewhen (RegNext(io.ComputeInstruction_Stream.fire, init = False)) {
    TaskB_Stream_valid := True
  } elsewhen (RegNext(Matinsub_send_finish, init = False) && !RegNext(inst_finish, init = False)) {
    TaskB_Stream_valid := True
  } elsewhen (RegNext(io.DataB_Stream.fire, init = False) && RegNext(MatBsub_row_cnt) =/= slicerCfg.matSubRowNum - 1) {
    TaskB_Stream_valid := True
  }

  val DataB_Reg = Reg(ReadDataTypeB)
  val DataB_Stream_ready = Reg(Bool()) init (False)
  io.DataB_Stream.ready := DataB_Stream_ready
  when(io.DataB_Stream.fire) {
    DataB_Reg := io.DataB_Stream.payload
    DataB_Stream_ready := False
  } elsewhen (io.TaskB_Stream.fire) {
    DataB_Stream_ready := True
  }

  val MatBsub_buffer =
    Vec.fill(slicerCfg.matSubRowNum)(Vec.fill(slicerCfg.matSubRowNum)(Reg(Bits(slicerCfg.elementWidthB bits))))
  when(RegNext(io.DataB_Stream.fire, init = False)) {
    MatBsub_buffer(RegNext(MatBsub_row_cnt).resized) := DataB_Reg.data.subdivideIn(slicerCfg.elementWidthB bits)
  }

  val MatAsub_read_finish = Reg(Bool()) init (False)
  val MatBsub_read_finish = Reg(Bool()) init (False)
  when(MatAsub_read_finish && MatBsub_read_finish) {
    MatAsub_read_finish := False
    MatBsub_read_finish := False
  } otherwise {
    when(RegNext(io.DataA_Stream.fire, init = False) && RegNext(MatAsub_row_cnt) === slicerCfg.matSubRowNum - 1) {
      MatAsub_read_finish := True
    }
    when(RegNext(io.DataB_Stream.fire, init = False) && RegNext(MatBsub_row_cnt) === slicerCfg.matSubRowNum - 1) {
      MatBsub_read_finish := True
    }
  }
  val Matinsub_read_finish = MatAsub_read_finish && MatBsub_read_finish

  val Mats_Stream = Stream(InMatsType)
  val Matinsub_row_cnt = Reg(UInt(slicerCfg.ShapeWidth bits))
  when(io.ComputeInstruction_Stream.fire) {
    Matinsub_row_cnt := 0
  } elsewhen (Mats_Stream.fire) {
    when(Matinsub_row_cnt === slicerCfg.matSubRowNum - 1) {
      Matinsub_row_cnt := 0
    } otherwise {
      Matinsub_row_cnt := Matinsub_row_cnt + 1
    }
  }
  Matinsub_send_finish := Mats_Stream.fire && Matinsub_row_cnt === slicerCfg.matSubRowNum - 1

  val Mats_Reg = Reg(InMatsType)
  Mats_Stream.payload := Mats_Reg
  for (i <- 0 until slicerCfg.matSubRowNum) {
    Mats_Reg.A(i) := MatAsub_buffer(i)(Matinsub_row_cnt.resized).asSInt
  }
  for (i <- 0 until slicerCfg.matSubRowNum) {
    when(is_MatMul) {
      Mats_Reg.B(i) := MatBsub_buffer(Matinsub_row_cnt.resized)(i).asSInt
    } otherwise {
      Mats_Reg.B(i) := MatBsub_buffer(slicerCfg.matSubRowNum - 1 - i)(Matinsub_row_cnt.resized).asSInt
    }
  }
  Mats_Reg.Final := is_MatMul ?
    (Matinsub_row_cnt === slicerCfg.matSubRowNum - 1 && MatA_col_slice_cnt === MatA_col_slice_num - 1) |
    Matinsub_row_cnt === slicerCfg.matSubRowNum - 1
  Mats_Reg.CoreInstruction.SystolicArray2D_CC_Instruction.matrixOperation := ComputeInstruction_Reg.matrixOperation
  Mats_Reg.CoreInstruction.SystolicArray2D_CC_Instruction.shiftLeft_AfterMatrixOperation := ComputeInstruction_Reg.shiftLeft_AfterMatrixOperation
  Mats_Reg.CoreInstruction.SystolicArray2D_CC_Instruction.doTranspose := ComputeInstruction_Reg.doTranspose
  Mats_Reg.CoreInstruction.Activation_Instruction.activationFunction := ComputeInstruction_Reg.activationFunction
  Mats_Reg.CoreInstruction.Activation_Instruction.shiftLeft_AfterActivation := ComputeInstruction_Reg.shiftLeft_AfterActivation
  Mats_Reg.CoreInstruction.Collector_Instruction.UID := ComputeInstruction_Reg.UID
  Mats_Reg.CoreInstruction.Collector_Instruction.MatA_row_slice_cnt := MatA_row_slice_cnt
  Mats_Reg.CoreInstruction.Collector_Instruction.MatB_col_slice_cnt := MatB_col_slice_cnt
  val Mats_Stream_valid = Reg(Bool()) init (False)
  Mats_Stream.valid := Mats_Stream_valid
  when(Mats_Stream.fire) {
    Mats_Stream_valid := False
  } elsewhen (Matinsub_read_finish) {
    Mats_Stream_valid := True
  } elsewhen (RegNext(Mats_Stream.fire, init = False) && RegNext(Matinsub_row_cnt) =/= slicerCfg.matSubRowNum - 1) {
    Mats_Stream_valid := True
  }

  when(io.ComputeInstruction_Stream.fire) {
    MatA_col_slice_cnt := 0
  } elsewhen (Matinsub_send_finish) {
    when(MatA_col_slice_cnt === MatA_col_slice_num - 1) {
      MatA_col_slice_cnt := 0
    } otherwise {
      MatA_col_slice_cnt := MatA_col_slice_cnt + 1
    }
  }
  Matoutsub_send_finish :=
    Mats_Stream.fire && Matinsub_row_cnt === slicerCfg.matSubRowNum - 1 && MatA_col_slice_cnt === MatA_col_slice_num - 1
  inst_finish := is_MatMul ?
    (Matoutsub_send_finish && MatB_col_slice_cnt === MatB_col_slice_num - 1 && MatA_row_slice_cnt === MatA_row_slice_num - 1) |
    (Matinsub_send_finish && MatB_col_slice_cnt === MatB_col_slice_num - 1 && MatA_row_slice_cnt === MatA_row_slice_num - 1)

  val lock = Reg(Bool(), init = False)
  when(Mats_Stream.fire && !Mats_Stream.payload.Final) {
    lock := True
  } elsewhen (Mats_Stream.fire && Mats_Stream.payload.Final) {
    lock := False
  }

  val request = UInt(slicerCfg.numCores bits)
  for (i <- 0 until slicerCfg.numCores) {
    request(i) := io.Mats_to_Cores_Streams(i).ready
  }
  val priority = Reg(UInt(slicerCfg.numCores bits), init = U(1))
  when(!lock && request.orR) {
    priority := priority.rotateLeft(1)
  }
  val double_request = Cat(request, request).asUInt
  val request_sub_priority = double_request - priority
  val double_grant = double_request & ~request_sub_priority
  val grant = double_grant(0, slicerCfg.numCores bits) | double_grant(slicerCfg.numCores, slicerCfg.numCores bits)

  var select_nolock = UInt(slicerCfg.CoreSelectWidth bits)
  select_nolock := 0
  for (i <- 0 until slicerCfg.numCores) {
    when(grant(i)) {
      select_nolock \= U(i, slicerCfg.CoreSelectWidth bits)
    }
  }
  val select_lock = RegNextWhen(select_nolock, !lock)
  val select = lock ? select_lock | select_nolock
  io.Mats_to_Cores_Streams <> StreamDemux(Mats_Stream, select, slicerCfg.numCores)
}

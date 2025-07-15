package Accelerator

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import spinal.sim._
import scala.util._
import scala.math._
import scala.collection.mutable._

import Slicer._
import DataPump._
import GeMM.SystolicArray2D._
import Activation._
import ExponentialFunction._
import LogarithmFunction._
import ReLUFunction._
import SoftplusFunction._

case class Accelerator_Config(
    UIDWidth: Int,
    ShiftWidth: Int,
    AddressWidth: Int,
    ShapeWidth: Int,
    matSubRowNum: Int,
    activationRowNum: Int,
    elementWidth: Int,
    intWidth: Int,
    in_Length_Max: Int,
    systolicArrayInFifoDepth: Int,
    systolicArrayOutFifoDepth: Int,
    systolicArrayInstFifoDepth: Int,
    activationOutFifoDepth: Int,
    slicedInstFifoDepth: Int,
    numCores: Int
) {
  require(systolicArrayInFifoDepth == 16, "This is magic")
  require(systolicArrayOutFifoDepth == 8, "This is magic")
  val SlicecntWidth = log2Up(round(ceil((pow(2, ShapeWidth) - 1) / matSubRowNum)))
  val dataWidth = matSubRowNum * elementWidth
  val fracWidth = elementWidth - intWidth
  val slicerCfg = SlicerCfg(
    UIDWidth = UIDWidth,
    ShiftWidth = ShiftWidth,
    AddressWidth = AddressWidth,
    ShapeWidth = ShapeWidth,
    SlicecntWidth = SlicecntWidth,
    matSubRowNum = matSubRowNum,
    elementWidthA = elementWidth,
    elementWidthB = elementWidth,
    numCores = numCores
  )
  val dataPumpMm2sCfg = DataPump_mm2s_Config(
    mem_data_width = dataWidth,
    mem_addr_width = AddressWidth,
    RepeatNum_Max = 1,
    Enable_Padding_logic = false
  )
  val systolicArray2DWrapCfg = SystolicArray2D_Wrap_Config(
    in_Length_Max = in_Length_Max,
    in_Length_Min = matSubRowNum * matSubRowNum / activationRowNum,
    in_MatA_row_num = matSubRowNum,
    in_MatB_col_num = matSubRowNum,
    in_MatA_element_Width = elementWidth,
    in_MatB_element_Width = elementWidth,
    out_MatZ_element_Width = elementWidth,
    Enable_Transpose_logic = true,
    Enable_ElementWise_logic = true,
    in_FIFO_Depth = systolicArrayInFifoDepth,
    out_FIFO_Depth = systolicArrayOutFifoDepth,
    instruction_FIFO_Depth = systolicArrayInstFifoDepth,
    UIDWidth = UIDWidth,
    ShiftWidth = ShiftWidth,
    SlicecntWidth = SlicecntWidth
  )
  val activationCfg = Activation_Config(
    Matx_Width = activationRowNum,
    MatX_Width = activationRowNum,
    element_in_Width = elementWidth,
    element_out_Width = elementWidth,
    max_indepth = activationOutFifoDepth,
    expCfg = EXP_function_cfg(bit_int = intWidth - 1, bit_frac = fracWidth, x_max = 9),
    lnCfg = LN_function_cfg(bit_int = intWidth - 1, bit_frac = fracWidth),
    reluCfg = ReLU_function_cfg(bit_int = intWidth - 1, bit_frac = fracWidth),
    softplusCfg = Softplus_function_cfg(bit_int = intWidth - 1, bit_frac = fracWidth),
    UIDWidth = UIDWidth,
    ShiftWidth = ShiftWidth,
    SlicecntWidth = SlicecntWidth
  )
  val collectorCfg = Collector_Config(
    UIDWidth = UIDWidth,
    AddressWidth = AddressWidth,
    ShapeWidth = ShapeWidth,
    SlicecntWidth = SlicecntWidth,
    in_MatA_row_num = matSubRowNum,
    in_MatB_col_num = matSubRowNum,
    MatX_Width = activationRowNum,
    Activation_x_Width = elementWidth,
    numCores = numCores
  )
  val dataPumpS2mmCfg = DataPump_s2mm_Config(
    mem_data_width = dataWidth,
    mem_addr_width = AddressWidth,
    RepeatNum_Max = 1,
    Enable_UnPadding_logic = false,
    Enable_Error_Port_logic = false
  )
}

case class Sdpram(addrWidth: Int, dataWidth: Int) extends Component {
  def MemoryReadPortType = MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth)
  def MemoryWritePortType = MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth)
  def noRead = {
    io.read.Valid := False
    io.read.Address := 0
  }
  def noWrite = {
    io.write.Valid := False
    io.write.Address := 0
    io.write.Data := 0
  }
  val io = new Bundle {
    val read = slave(MemoryReadPortType)
    val write = slave(MemoryWritePortType)
  }
  val mem = Mem(Bits(dataWidth bits), wordCount = 1 << addrWidth)
  io.read.Data := mem.readSync(
    enable = io.read.Valid,
    address = io.read.Address
  )
  mem.write(
    enable = io.write.Valid,
    address = io.write.Address,
    data = io.write.Data
  )
}

case class Accelerator(acceleratorCfg: Accelerator_Config) extends Component {
  val slicer = Slicer(acceleratorCfg.slicerCfg)
  val dataPumpA = DataPump_mm2s(acceleratorCfg.dataPumpMm2sCfg)
  val dataPumpB = DataPump_mm2s(acceleratorCfg.dataPumpMm2sCfg)
  val sdpramA = Sdpram(addrWidth = acceleratorCfg.AddressWidth, dataWidth = acceleratorCfg.dataWidth)
  val sdpramB = Sdpram(addrWidth = acceleratorCfg.AddressWidth, dataWidth = acceleratorCfg.dataWidth)
  val collector = Collector(acceleratorCfg.collectorCfg)
  val datapumpZ = DataPump_s2mm(acceleratorCfg.dataPumpS2mmCfg)
  val sdpramZ = Sdpram(acceleratorCfg.AddressWidth, acceleratorCfg.dataWidth)

  val io = new Bundle {
    val ComputeInstruction_Stream = slave Stream (slicer.InstType)
  }

  slicer.io.ComputeInstruction_Stream <> io.ComputeInstruction_Stream
  slicer.io.Sliced_ComputeInstruction_Stream.queue(
    acceleratorCfg.slicedInstFifoDepth
  ) <> collector.io.Sliced_ComputeInstruction_Stream
  slicer.io.TaskA_Stream <> dataPumpA.io.TaskStream
  slicer.io.DataA_Stream <> dataPumpA.io.DataStream
  slicer.io.TaskB_Stream <> dataPumpB.io.TaskStream
  slicer.io.DataB_Stream <> dataPumpB.io.DataStream
  sdpramA.io.read <> dataPumpA.io.MemoryReadPort
  sdpramA.noWrite
  sdpramB.io.read <> dataPumpB.io.MemoryReadPort
  sdpramB.noWrite

  val clkCore = ClockDomain.external("SystolicArray2D_CC_core")
  for (i <- 0 until acceleratorCfg.numCores) {
    val systolicArray2DWrapper =
      SystolicArray2D_Wrapper(
        cfg = acceleratorCfg.systolicArray2DWrapCfg,
        clk_in = ClockDomain.current,
        clk_out = ClockDomain.current,
        clk_core = clkCore
      )
    val activation = Activation(acceleratorCfg.activationCfg)
    slicer.io.Mats_to_Cores_Streams(i) <> systolicArray2DWrapper.io.in_Mats_with_Core_Instruction
    systolicArray2DWrapper.io.out_Mats_with_Core_Instruction <> activation.io.in_Mats
    collector.io.Mats_from_Cores_Streams(i) <> activation.io.out_Mats
  }

  collector.io.Task_Stream <> datapumpZ.io.TaskStream
  collector.io.Data_Stream <> datapumpZ.io.DataStream
  sdpramZ.io.write <> datapumpZ.io.MemoryWritePort
  sdpramZ.noRead
}

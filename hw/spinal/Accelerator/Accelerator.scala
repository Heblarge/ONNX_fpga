package Accelerator

import Slicer._
import DataPump._
import GeMM.SystolicArray2D._
import Activation._
import ExponentialFunction._
import LogarithmFunction._
import ReLUFunction._
import SoftplusFunction._

import spinal.core._
import spinal.lib.{Stream, slave}

import scala.math._

case class AcceleratorCfg(
    UIDWidth: Int,
    AddressWidth: Int,
    ShapeWidth: Int,
    systolicArraySideNum: Int,
    elementWidth: Int,
    intWidth: Int,
    systolicArrayInFifoDepth: Int,
    systolicArrayOutFifoDepth: Int,
    systolicArrayInstFifoDepth: Int,
    activationOutFifoDepth: Int,
    slicedInstFifoDepth: Int,
    numCores: Int
) {
  //require(systolicArrayInFifoDepth == 32, "This is magic")
  //require(systolicArrayOutFifoDepth == 32, "This is magic")
  val SlicecntWidth = log2Up(round(ceil((pow(2, ShapeWidth) - 1) / systolicArraySideNum)))
  val ShiftWidth = log2Up(elementWidth + 1) + 1
  val dataWidth = systolicArraySideNum * elementWidth
  val fracWidth = elementWidth - intWidth
  val slicerCfg = SlicerCfg(
    UIDWidth = UIDWidth,
    ShiftWidth = ShiftWidth,
    AddressWidth = AddressWidth,
    ShapeWidth = ShapeWidth,
    SlicecntWidth = SlicecntWidth,
    systolicArraySideNum = systolicArraySideNum,
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
    in_Length_Max = systolicArraySideNum,
    in_Length_Min = systolicArraySideNum,
    in_MatA_row_num = systolicArraySideNum,
    in_MatB_col_num = systolicArraySideNum,
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
    Matx_Width = systolicArraySideNum,
    MatX_Width = systolicArraySideNum,
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
  val collectorCfg = CollectorCfg(
    UIDWidth = UIDWidth,
    AddressWidth = AddressWidth,
    ShapeWidth = ShapeWidth,
    SlicecntWidth = SlicecntWidth,
    slicedInstFifoDepth = slicedInstFifoDepth,
    systolicArraySideNum = systolicArraySideNum,
    activationUnitNum = systolicArraySideNum,
    elementWidthZ = elementWidth,
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

case class Accelerator(acceleratorCfg: AcceleratorCfg) extends Component {
  val slicer = Slicer(acceleratorCfg.slicerCfg)
  val dataPumpA = DataPump_mm2s(acceleratorCfg.dataPumpMm2sCfg)
  val dataPumpB = DataPump_mm2s(acceleratorCfg.dataPumpMm2sCfg)
  val sdpramA = Sdpram(addrWidth = acceleratorCfg.AddressWidth, dataWidth = acceleratorCfg.dataWidth)
  val sdpramB = Sdpram(addrWidth = acceleratorCfg.AddressWidth, dataWidth = acceleratorCfg.dataWidth)
  val collector = Collector(acceleratorCfg.collectorCfg)
  val datapumpZ = DataPump_s2mm(acceleratorCfg.dataPumpS2mmCfg)
  val sdpramZ = Sdpram(acceleratorCfg.AddressWidth, acceleratorCfg.dataWidth)

  val io = new Bundle {
    val inst = slave Stream slicer.InstType
  }

  slicer.io.inst <> io.inst
  slicer.io.slicedInst <> collector.io.slicedInst
  slicer.io.readAddrA <> dataPumpA.io.TaskStream
  slicer.io.readDataA <> dataPumpA.io.DataStream
  slicer.io.readAddrB <> dataPumpB.io.TaskStream
  slicer.io.readDataB <> dataPumpB.io.DataStream
  sdpramA.io.read <> dataPumpA.io.MemoryReadPort
  sdpramA.noWrite()
  sdpramB.io.read <> dataPumpB.io.MemoryReadPort
  sdpramB.noWrite()

  val clkCore = ClockDomain.external("SystolicArray2D_CC_core")
  slicer.io.matAfterSlicers.zip(collector.io.matAfterActivations).foreach { case (matAfterSlicer, matAfterActivation) =>
    val systolicArray2DWrapper =
      SystolicArray2D_Wrapper(
        cfg = acceleratorCfg.systolicArray2DWrapCfg,
        clk_in = ClockDomain.current,
        clk_out = ClockDomain.current,
        clk_core = clkCore
      )
    val activation = Activation(acceleratorCfg.activationCfg)
    systolicArray2DWrapper.io.in_Mats_with_Core_Instruction <> matAfterSlicer
    systolicArray2DWrapper.io.out_Mats_with_Core_Instruction <> activation.io.in_Mats
    matAfterActivation <> activation.io.out_Mats
  }

  collector.io.writeAddr <> datapumpZ.io.TaskStream
  collector.io.writeData <> datapumpZ.io.DataStream
  sdpramZ.io.write <> datapumpZ.io.MemoryWritePort
  sdpramZ.noRead()
}

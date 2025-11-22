package WrapForFPGA

import Tiling._
import DataPump._
import GeMM.SystolicArray2D._
import Activation._
import ExponentialFunction._
import LogarithmFunction._
import ReLUFunction._
import SoftplusFunction._

import spinal.core._
import spinal.lib._
import scala.math._

import spinal.lib.bus.amba4.axi._
import Tiling.{Slicer, SlicerCfg, Collector, CollectorCfg}

case class FPGACfg(
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
                           numCores: Int,
                         ) {
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
  // Instruction sAxi4-Full Port Config
  val sAxi4FullInstCfg = Axi4Config(
    addressWidth = 32, dataWidth = 256, idWidth = 0, useId = false,
    useBurst = true, useLock = false, useRegion = false, useQos = false , useStrb = false
  )
}


case class WrapForFPGA (FPGACfg: FPGACfg) extends Component{
  val slicer = Slicer(FPGACfg.slicerCfg)
  val dataPumpA = DataPump_mm2s(FPGACfg.dataPumpMm2sCfg)
  val dataPumpB = DataPump_mm2s(FPGACfg.dataPumpMm2sCfg)
  val collector = Collector(FPGACfg.collectorCfg)
  val datapumpZ = DataPump_s2mm(FPGACfg.dataPumpS2mmCfg)

  val instAxi4ToStreamCfg =  InstAxi4ToStream_Config(
    payloadType = slicer.InstType,
    axiCfg = FPGACfg.sAxi4FullInstCfg,
    fifoDepth = 2
  )

  val instAxi4ToStream = InstAxi4ToStream(instAxi4ToStreamCfg)

  def MemoryReadPort_Type(): MemoryReadPort_TypeDef = {
    new MemoryReadPort_TypeDef(FPGACfg.dataPumpMm2sCfg.mem_addr_width, FPGACfg.dataPumpMm2sCfg.mem_data_width)
  }

  def MemoryWritePort_Type(): MemoryWritePort_TypeDef = {
    new MemoryWritePort_TypeDef(FPGACfg.dataPumpS2mmCfg.mem_addr_width, FPGACfg.dataPumpS2mmCfg.mem_data_width)
  }

  val io = new Bundle {
    // Stream Instruction Port
//    val inst = slave Stream slicer.InstType
//    println(s"Total compute instruction width is ${widthOf(slicer.InstType)} bits")
    // AXI4-Full Instruction Port
    val sAxi4FullInst = slave(Axi4(instAxi4ToStreamCfg.axiCfg))
    val memPortA = master (MemoryReadPort_Type()) // Read Request Only
    val memPortB = master (MemoryReadPort_Type()) // Read Request Only
    val memPortZ = master (MemoryWritePort_Type()) // Write Request Only
  }

  io.sAxi4FullInst <> instAxi4ToStream.io.axi

  slicer.io.inst <> instAxi4ToStream.io.out//io.inst
  slicer.io.slicedInst <> collector.io.slicedInst
  io.memPortA <> slicer.io.memoryReadPortA
  io.memPortB <> slicer.io.memoryReadPortB

  val clkCore = ClockDomain.external("SystolicArray2D_CC_core")
  slicer.io.matAfterSlicers.zip(collector.io.matAfterActivations).foreach { case (matAfterSlicer, matAfterActivation) =>
    val systolicArray2DWrapper =
      SystolicArray2D_Wrapper(
        cfg = FPGACfg.systolicArray2DWrapCfg,
        clk_in = ClockDomain.current,
        clk_out = ClockDomain.current,
        clk_core = clkCore
      )
    val activation = Activation(FPGACfg.activationCfg)
    systolicArray2DWrapper.io.in_Mats_with_Core_Instruction <> matAfterSlicer
    systolicArray2DWrapper.io.out_Mats_with_Core_Instruction <> activation.io.in_Mats
    matAfterActivation <> activation.io.out_Mats
  }

  io.memPortZ <> collector.io.memoryWritePort
}
object WrapForFPGA_Verilog extends App {
  val FileDir = "rtl/WrapForFPGA/verilog"
  import java.io.File
  new File(FileDir).mkdirs()
  val WrapForFPGACfg = FPGACfg(
    UIDWidth = 19,
    AddressWidth = 20,
    ShapeWidth = 16,
    systolicArraySideNum = 32,
    elementWidth = 24,
    intWidth = 12,
    systolicArrayInFifoDepth = 2,
    systolicArrayOutFifoDepth = 2,
    systolicArrayInstFifoDepth = 16,
    activationOutFifoDepth = 32,
    slicedInstFifoDepth = 16,
    numCores = 1,

  )

  SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    removePruned = true,
    bitVectorWidthMax = 100000
  ).generateVerilog(new WrapForFPGA(WrapForFPGACfg))//.printPruned()
}
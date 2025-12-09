package WrapForFPGA

import Tiling._
import DataPump._
import MatrixComputeUnit.SystolicArray2D._
import Activation._
import ExponentialFunction._
import LogarithmFunction._
import ReLUFunction._
import SoftplusFunction._
import Interface._

import spinal.core._
import spinal.lib._
import scala.math._

import spinal.lib.bus.amba4.axi._
import Tiling.{Slicer, SlicerCfg, Collector, CollectorCfg}
import MatrixComputeUnit.SystolicArray2D.{SystolicArray2D_Wrap_Config, SystolicArray2D_Wrapper}

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
                           // Axi4-Lite config
                           axiDataWidth: Int = 32,
                           axiAddressWidth: Int = 32,
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
    // shiftLeft_A = 0,
    // shiftLeft_B = 0,
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
    addressWidth = axiAddressWidth, dataWidth = axiDataWidth, idWidth = 0, useId = false,
    useBurst = true, useLock = false, useRegion = false, useQos = false , useStrb = true
  )
}

case class WrapForFPGA(FPGACfg: FPGACfg) extends Component{
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



// DUMMY LOGIC FOR "Axi4ToStreamConfigurable" (ONLY FOR INSTRUCTION INTERFACE TEST)
class InstAxi4ToStreamWrapper(FPGACfg: FPGACfg) extends Component {
  val slicer = Slicer(FPGACfg.slicerCfg)

  // 确保使用 32bit 位宽的配置
  val instAxi4ToStreamCfg = Axi4ToStreamConfigurable_Config(
    axiCfg = FPGACfg.sAxi4FullInstCfg, // 确保 FPGACfg 里这是 32位宽
    fifoDepth = 2
  )

  // 映射函数
  // 2. 定义映射策略 (Mapping Strategy)
  // 每一个字段都独占一个 4字节 (32bit) 地址
  def myMapping(factory: Axi4SlaveFactory, reg: ComputeInstruction_TypeDef, baseAddr: Int): Unit = {
    // 0x00: UID (19 bits)
    factory.write(reg.UID,                            address = baseAddr + 0x00)
    // 0x04: matrixOperation (2 bits)
    factory.write(reg.matrixOperation,                address = baseAddr + 0x04)
    // 0x08: shiftLeft_AfterMatrixOperation (6 bits)
    factory.write(reg.shiftLeft_AfterMatrixOperation, address = baseAddr + 0x08)
    // 0x0C: doTranspose (1 bit)
    factory.write(reg.doTranspose,                    address = baseAddr + 0x0C)
    // 0x10: activationFunction (3 bits)
    factory.write(reg.activationFunction,             address = baseAddr + 0x10)
    // 0x14: shiftLeft_AfterActivation (6 bits)
    factory.write(reg.shiftLeft_AfterActivation,      address = baseAddr + 0x14)
    // 0x18: Input0 Address
    factory.write(reg.input0Address,                  address = baseAddr + 0x18)
    // 0x1C: Input0 Shape[0] (16 bits)
    factory.write(reg.input0Shape(0),                 address = baseAddr + 0x1C)
    // 0x20: Input0 Shape[1] (16 bits)
    factory.write(reg.input0Shape(1),                 address = baseAddr + 0x20)
    // 0x24: Input1 Address
    factory.write(reg.input1Address,                  address = baseAddr + 0x24)
    // 0x28: Input1 Shape[0]
    factory.write(reg.input1Shape(0),                 address = baseAddr + 0x28)
    // 0x2C: Input1 Shape[1]
    factory.write(reg.input1Shape(1),                 address = baseAddr + 0x2C)
    // 0x30: Output Address
    factory.write(reg.outputAddress,                  address = baseAddr + 0x30)
    // 0x34: Output Shape[0]
    factory.write(reg.outputShape(0),                 address = baseAddr + 0x34)
    // 0x38: Output Shape[1]
    factory.write(reg.outputShape(1),                 address = baseAddr + 0x38)
  }

  // 实例化子模块
  val instAxi4ToStream = new Axi4ToStreamConfigurable(
    instAxi4ToStreamCfg,
    slicer.InstType,
    myMapping
  )

  val io = new Bundle {
    val sAxi4FullInst = slave(Axi4(instAxi4ToStreamCfg.axiCfg))

    // ILA Debugging
    val regInstr = out Bits(256 bits)
    val regValid = out Bool()
  }

  val InstrReg = RegInit(slicer.InstType.getZero)

  // 连接 AXI
  // 如果 <> 依然报错，请尝试手动连接 (一般不需要)
  io.sAxi4FullInst <> instAxi4ToStream.io.axi

  // 下游逻辑
  when(instAxi4ToStream.io.out.valid) {
    InstrReg := instAxi4ToStream.io.out.payload
  }
  instAxi4ToStream.io.out.ready := True

  // ILA 输出
  io.regInstr := InstrReg.asBits.resized
  io.regValid := RegNext(instAxi4ToStream.io.out.valid)
}

case class InstAxi4ToStreamNarrowWeaverWrapper (FPGACfg: FPGACfg) extends Component{
  val slicer = Slicer(FPGACfg.slicerCfg)
  val instAxi4ToStreamCfg =  InstAxi4ToStream_Config(
    payloadType = slicer.InstType,
    axiCfg = FPGACfg.sAxi4FullInstCfg,
    fifoDepth = 2
  )
  //val instAxi4ToStream = new InstAxi4ToStreamNarrowWeaver(instAxi4ToStreamCfg)
  val io = new Bundle {
    val sAxi4FullInst = slave(Axi4(instAxi4ToStreamCfg.axiCfg))
    // FOR ILA DEBUGGING
    val regInstr = out Bits(256 bits)
    val regValid = out Bool()
  }
//  val InstrReg = RegInit(slicer.InstType.getZero)
//  io.sAxi4FullInst <> instAxi4ToStream.io.axi
//  // 只有当 valid 有效时才更新寄存器，防止读到空 FIFO 的垃圾数据
//  when(instAxi4ToStream.io.out.valid) {
//    InstrReg := instAxi4ToStream.io.out.payload
//  }
//  instAxi4ToStream.io.out.ready := True
//  // --- ILA 调试信号连接 ---
//  // 注意：regInstr 只有 32bit，而 InstrReg 有 193bit
//  io.regInstr := InstrReg.asBits.resized
//  io.regValid := RegNext(instAxi4ToStream.io.out.valid)
}

object InstAxi4ToStream_verilog {
  import java.io.File
  new File("rtl/InstAxi4ToStream").mkdir()

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
  def main(arg:Array[String]): Unit ={
    SpinalConfig(
      targetDirectory = "rtl/InstAxi4ToStream",
      oneFilePerComponent = false,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(new InstAxi4ToStreamWrapper(WrapForFPGACfg))
      .printPruned()
  }
}

object InstAxi4ToStreamNarrowWeaverWrapper_verilog {
  import java.io.File
  new File("rtl/InstAxi4ToStreamNarrowWeaverWrapper").mkdir()

  val WrapForFPGACfg = FPGACfg(
    UIDWidth = 19,
    AddressWidth = 20,
    ShapeWidth = 16,
    systolicArraySideNum = 16,
    elementWidth = 24,
    intWidth = 12,
    systolicArrayInFifoDepth = 2,
    systolicArrayOutFifoDepth = 2,
    systolicArrayInstFifoDepth = 16,
    activationOutFifoDepth = 32,
    slicedInstFifoDepth = 16,
    numCores = 1,
    axiDataWidth = 256,
    axiAddressWidth = 32
  )
  def main(arg:Array[String]): Unit ={
    SpinalConfig(
      targetDirectory = "rtl/InstAxi4ToStreamNarrowWeaverWrapper",
      oneFilePerComponent = false,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(new InstAxi4ToStreamNarrowWeaverWrapper(WrapForFPGACfg))
      .printPruned()
  }
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


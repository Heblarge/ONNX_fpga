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
import spinal.lib.bus.amba4.axilite._
import scala.math._

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
    numCores: Int
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
  
  // AXI4-Lite 配置 - 固定使用 32 位总线，对应 128 位指令
  val axi4LiteInstCfg = Axi4LiteToStreamConfig(
    axiDataWidth = 32,      // 对外：32 位 AXI 总线
    axiAddrWidth = 12,
    fifoDepth = 2,
    ctrlRegAddr = 0x20
  )
}

case class WrapForFPGA(fpgaCfg: FPGACfg) extends Component {
  val slicer = Slicer(fpgaCfg.slicerCfg)
  val collector = Collector(fpgaCfg.collectorCfg)

  // 创建 128 位指令的 AXI4-Lite to Stream 桥接器
  // use64BitBus = false 表示使用 32 位 AXI 总线
  val instBridge = new Inst128_Wrapper(use64BitBus = false)

  val io = new Bundle {
    // AXI4-Lite 指令接口（32 位总线）
    val sAxi4LiteInst = slave(AxiLite4(fpgaCfg.axi4LiteInstCfg.getAxiConfig))
    
    // Memory Ports - 直接暴露存储接口
    val memPortA = master(MemoryReadPort_TypeDef(fpgaCfg.AddressWidth, fpgaCfg.dataWidth))
    val memPortB = master(MemoryReadPort_TypeDef(fpgaCfg.AddressWidth, fpgaCfg.dataWidth))
    val memPortZ = master(MemoryWritePort_TypeDef(fpgaCfg.AddressWidth, fpgaCfg.dataWidth))
    
    // 指令完成信号
    val instFinish = out Bool()
  }

  // 连接 AXI4-Lite 桥接器
  io.sAxi4LiteInst <> instBridge.io.s_axi
  
  // 将桥接器的 128 位输出连接到 slicer
  val instStream = Stream(slicer.InstType)
  instStream.valid := instBridge.io.m_stream.valid
  instStream.payload.assignFromBits(instBridge.io.m_stream.payload.resized)
  instBridge.io.m_stream.ready := instStream.ready
  
  // 连接 slicer
  slicer.io.inst <> instStream
  slicer.io.slicedInst <> collector.io.slicedInst
  
  // 暴露指令完成信号
  io.instFinish := slicer.io.instFinish
  
  // 直接暴露 memory port
  io.memPortA <> slicer.io.memoryReadPortA
  io.memPortB <> slicer.io.memoryReadPortB
  io.memPortZ <> collector.io.memoryWritePort
  
  // 定义外部核心时钟域
  val clkCore = ClockDomain.external("SystolicArray2D_CC_core")
  
  // 连接 Systolic Array 和 Activation 模块
  slicer.io.matAfterSlicers.zip(collector.io.matAfterActivations).foreach { 
    case (matAfterSlicer, matAfterActivation) =>
      val systolicArray2DWrapper = SystolicArray2D_Wrapper(
        cfg = fpgaCfg.systolicArray2DWrapCfg,
        clk_in = ClockDomain.current,
        clk_out = ClockDomain.current,
        clk_core = clkCore
      )
      val activation = Activation(fpgaCfg.activationCfg)
      
      systolicArray2DWrapper.io.in_Mats_with_Core_Instruction <> matAfterSlicer
      systolicArray2DWrapper.io.out_Mats_with_Core_Instruction <> activation.io.in_Mats
      matAfterActivation <> activation.io.out_Mats
  }
}

object WrapForFPGA_Verilog extends App {
  val FileDir = "rtl/WrapForFPGA/verilog"
  import java.io.File
  new File(FileDir).mkdirs()
  
  val fpgaCfg = FPGACfg(
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
    numCores = 1
  )
  
  SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    removePruned = true,
    bitVectorWidthMax = 100000
  ).generateVerilog(new WrapForFPGA(fpgaCfg))
}
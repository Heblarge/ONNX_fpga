package Accelerator

import Tiling._
import DataPump._
import MatrixComputeUnit.SystolicArray2D._
import Activation._
import ExponentialFunction._
import LogarithmFunction._
import ReLUFunction._
import SoftplusFunction._

import spinal.core._
import spinal.lib.{Stream, slave}

import scala.math._
import Tiling.{SlicerCfg, Collector, Slicer, CollectorCfg, Sdpram}
import MatrixComputeUnit.SystolicArray2D.{SystolicArray2D_Wrap_Config, SystolicArray2D_Wrapper}

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
    numCores: Int,
    memElementWidth: Int = 32
) {
  require(memElementWidth >= elementWidth, s"memElementWidth($memElementWidth) must >= elementWidth($elementWidth)")
  require(memElementWidth % 8 == 0, s"memElementWidth($memElementWidth) must be multiple of 8")
  val SlicecntWidth = log2Up(round(ceil((pow(2, ShapeWidth) - 1) / systolicArraySideNum)))
  val ShiftWidth = log2Up(elementWidth + 1) + 1
  val dataWidth = systolicArraySideNum * elementWidth
  val memElementBytes = memElementWidth / 8
  val memWidth  = systolicArraySideNum * memElementWidth
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
    in_Length_Max = 2048,
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
    numCores = numCores,
    memElementWidth = memElementWidth
  )
}

case class Accelerator(acceleratorCfg: AcceleratorCfg) extends Component {
  val slicer = Slicer(acceleratorCfg.slicerCfg)
  val sdpramA = Sdpram(addrWidth = acceleratorCfg.AddressWidth, dataWidth = acceleratorCfg.memWidth)
  val sdpramB = Sdpram(addrWidth = acceleratorCfg.AddressWidth, dataWidth = acceleratorCfg.memWidth)
  val collector = Collector(acceleratorCfg.collectorCfg)
  val sdpramZ = Sdpram(acceleratorCfg.AddressWidth, acceleratorCfg.memWidth)

  val io = new Bundle {
    val inst = slave Stream slicer.InstType
  }

  slicer.io.inst <> io.inst
  slicer.io.slicedInst <> collector.io.slicedInst

  // 桥接mem读端口：Slicer 字索引 → 字节地址 → Sdpram，数据 memElementWidth 对齐 → elementWidth 紧凑格式
  val byteOffset = log2Up(acceleratorCfg.memWidth / 8)
  Seq(
    (slicer.io.memoryReadPortA, sdpramA.io.read),
    (slicer.io.memoryReadPortB, sdpramB.io.read)
  ).foreach { case (slicerPort, sdpramPort) =>
    sdpramPort.clk := slicerPort.clk
    sdpramPort.rst := slicerPort.rst
    sdpramPort.Valid := slicerPort.Valid
    // Slicer 输出字索引，左移转换为字节地址
    sdpramPort.Address := (slicerPort.Address << byteOffset).resized
    // Cat(Seq) 中第一个元素在 LSB，所以用 0 until N 遍历
    slicerPort.Data := Cat(
      (0 until acceleratorCfg.systolicArraySideNum).map(i =>
        sdpramPort.Data(i * acceleratorCfg.memElementWidth, acceleratorCfg.elementWidth bits)
      )
    )
  }

  sdpramA.noWrite()
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

  collector.io.memoryWritePort <> sdpramZ.io.write
  sdpramZ.noRead()
  // 防止这些模块被剪枝
  sdpramA.io.dontSimplifyIt()
  sdpramB.io.dontSimplifyIt()
  sdpramZ.io.dontSimplifyIt()
}

object Accelerator_Verilog extends App {
  val FileDir = "rtl/Accelerator/verilog"
  import java.io.File
  new File(FileDir).mkdirs()
  val acceleratorCfg = AcceleratorCfg(
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
  // val acceleratorCfg = AcceleratorCfg(
  //   UIDWidth = 16,
  //   AddressWidth = 16,
  //   ShapeWidth = 16,
  //   systolicArraySideNum = 2,
  //   elementWidth = 24,
  //   intWidth = 12,
  //   systolicArrayInFifoDepth = 32,
  //   systolicArrayOutFifoDepth = 32,
  //   systolicArrayInstFifoDepth = 32,
  //   activationOutFifoDepth = 32,
  //   slicedInstFifoDepth = 32,
  //   numCores = 2
  // )
  SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    removePruned = true,
    bitVectorWidthMax = 100000
  ).generateVerilog(new Accelerator(acceleratorCfg)) // .printPruned()
}

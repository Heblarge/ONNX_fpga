package Tiling

import Util._
import Interface._
import DataPump._

import spinal.core._
import spinal.lib._

/**
 * Slicer Configuration Parameters
 * 切片器配置参数
 * 
 * @param UIDWidth Unique Instruction ID width - 指令唯一标识符宽度
 * @param ShiftWidth Shift operation width - 移位操作宽度  
 * @param AddressWidth Memory address width - 内存地址宽度
 * @param ShapeWidth Matrix shape dimension width - 矩阵形状维度宽度
 * @param SlicecntWidth Slice counter width - 切片计数器宽度
 * @param systolicArraySideNum Systolic array side dimension - 脉动阵列边长
 * @param elementWidthA Matrix A element width - 矩阵A元素宽度
 * @param elementWidthB Matrix B element width - 矩阵B元素宽度
 * @param numCores Number of processing cores - 处理核心数量
 */
case class SlicerCfg(
    UIDWidth: Int,
    ShiftWidth: Int,
    AddressWidth: Int,
    ShapeWidth: Int,
    SlicecntWidth: Int,
    systolicArraySideNum: Int,
    elementWidthA: Int,
    elementWidthB: Int,
    numCores: Int
) {
  val dataWidthA = systolicArraySideNum * elementWidthA
  val dataWidthB = systolicArraySideNum * elementWidthB
  val CoreSelectWidth = log2Up(numCores)
}

/**
 * Matrix Slicer Component - 矩阵切片器组件
 * 
 * Main function: Split large matrix operations into smaller sub-matrices suitable for hardware processing
 * 主要功能：将大型矩阵运算分解为适合硬件处理的小型子矩阵
 */
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

  def MatAfterSlicerType =
    in_Mats_TypeDef(
      in_MatA_row_num = slicerCfg.systolicArraySideNum,
      in_MatA_element_Width = slicerCfg.elementWidthA,
      in_MatB_col_num = slicerCfg.systolicArraySideNum,
      in_MatB_element_Width = slicerCfg.elementWidthB,
      ShiftWidth = slicerCfg.ShiftWidth,
      UIDWidth = slicerCfg.UIDWidth,
      SlicecntWidth = slicerCfg.SlicecntWidth
    )

  def MemoryReadPortTypeA =
    MemoryReadPort_TypeDef(AddressWidth = slicerCfg.AddressWidth, DataWidth = slicerCfg.dataWidthA)
  def MemoryReadPortTypeB =
    MemoryReadPort_TypeDef(AddressWidth = slicerCfg.AddressWidth, DataWidth = slicerCfg.dataWidthB)

  val io = new Bundle {
    val inst = slave Stream InstType
    val slicedInst = master Stream SlicedInstType
    val memoryReadPortA = master(MemoryReadPortTypeA)
    val memoryReadPortB = master(MemoryReadPortTypeB)
    val matAfterSlicers = Vec.fill(slicerCfg.numCores)(master Stream MatAfterSlicerType)
  }

  io.inst.ready.setAsReg().init(True)
  val instReg = Reg(InstType)
  val isMatMul = instReg.matrixOperation === MatrixOperation_TypeDef.MatMul
  val instFinish = Bool()
  when(io.inst.fire) {
    io.inst.ready := False
    instReg := io.inst.payload
  } elsewhen (instFinish) {
    io.inst.ready := True
  }

  io.slicedInst.valid.setAsReg().init(False)
  io.slicedInst.assignFromInst(instReg)
  when(io.slicedInst.fire) {
    io.slicedInst.valid := False
  } elsewhen (io.inst.fire) {
    io.slicedInst.valid := True
  }

  val matABSubSendFinish = Bool()
  val matAColSliceCnt = Cnt(
    Mux(isMatMul, instReg.input0Shape(1) / slicerCfg.systolicArraySideNum, U(1)) - 1,
    io.inst.fire,
    matABSubSendFinish
  )
  val matBColSliceCnt =
    Cnt(instReg.input1Shape(1) / slicerCfg.systolicArraySideNum - 1, io.inst.fire, matAColSliceCnt.willOverflow)
  val matARowSliceCnt =
    Cnt(instReg.input0Shape(0) / slicerCfg.systolicArraySideNum - 1, io.inst.fire, matBColSliceCnt.willOverflow)

  val matASubReadRowCnt = Cnt(slicerCfg.systolicArraySideNum - 1, io.inst.fire, io.memoryReadPortA.Valid)
  io.memoryReadPortA.Valid := Mux(
    matASubReadRowCnt === 0,
    io.slicedInst.fire || RegNext(matABSubSendFinish && !instFinish,False),
    True
  )
  io.memoryReadPortA.Address := instReg.input0Address + ((matARowSliceCnt * slicerCfg.systolicArraySideNum +
    matASubReadRowCnt) * instReg.input0Shape(1) / slicerCfg.systolicArraySideNum).resized +
    Mux(isMatMul, matAColSliceCnt, matBColSliceCnt)

  val matASub =
    Vec.fill(slicerCfg.systolicArraySideNum, slicerCfg.systolicArraySideNum)(Reg(Bits(slicerCfg.elementWidthA bits)))
  when(RegNext(io.memoryReadPortA.Valid, False)) {
    matASub(RegNext(matASubReadRowCnt.implicitValue)) := io.memoryReadPortA.Data.subdivideIn(slicerCfg.elementWidthA bits)
  }

  val matBSubReadRowCnt = Cnt(slicerCfg.systolicArraySideNum - 1, io.inst.fire, io.memoryReadPortB.Valid)
  io.memoryReadPortB.Valid := Mux(
    matBSubReadRowCnt === 0,
    io.slicedInst.fire || RegNext(matABSubSendFinish && !instFinish,False),
    True
  )
  io.memoryReadPortB.Address := instReg.input1Address +
    ((Mux[UInt](isMatMul, matAColSliceCnt, matARowSliceCnt) * slicerCfg.systolicArraySideNum +
      matBSubReadRowCnt) * instReg.input1Shape(1) / slicerCfg.systolicArraySideNum).resized +
    matBColSliceCnt

  val matBSub =
    Vec.fill(slicerCfg.systolicArraySideNum, slicerCfg.systolicArraySideNum)(Reg(Bits(slicerCfg.elementWidthB bits)))
  when(RegNext(io.memoryReadPortB.Valid, False)) {
    matBSub(RegNext(matASubReadRowCnt.implicitValue)) := io.memoryReadPortB.Data.subdivideIn(slicerCfg.elementWidthB bits)
  } 

  val matASubReadFinish = Reg(Bool(), False)
  val matBSubReadFinish = Reg(Bool(), False)
  val matInSubReadFinish = matASubReadFinish && matBSubReadFinish
  when(matInSubReadFinish) {
    matASubReadFinish := False
    matBSubReadFinish := False
  } otherwise {
    when(matASubReadRowCnt.willOverflow) {
      matASubReadFinish := True
    }
    when(matBSubReadRowCnt.willOverflow) {
      matBSubReadFinish := True
    }
  }

  val matAfterSlicer = Stream(MatAfterSlicerType)
  val matABSubSendRowCnt = Cnt(slicerCfg.systolicArraySideNum - 1, io.inst.fire, matAfterSlicer.fire)
  matABSubSendFinish := matABSubSendRowCnt.willOverflow
  instFinish := matABSubSendFinish && matAColSliceCnt.willOverflowIfInc && matBColSliceCnt.willOverflowIfInc && matARowSliceCnt.willOverflowIfInc

  matAfterSlicer.A := matASub.mapVec(_(matABSubSendRowCnt).asSInt)
  matAfterSlicer.B := Mux(
    isMatMul,
    matBSub(matABSubSendRowCnt).mapVec(_.asSInt),
    matBSub.shuffle(slicerCfg.systolicArraySideNum - 1 - _).mapVec(_(matABSubSendRowCnt).asSInt)
  )
  matAfterSlicer.CoreInstruction.assignFromInst(instReg, matARowSliceCnt.resized, matBColSliceCnt.resized)
  matAfterSlicer.Final := matABSubSendRowCnt.willOverflowIfInc && matAColSliceCnt.willOverflowIfInc
  matAfterSlicer.valid.setAsReg().init(False)
  when(matInSubReadFinish) {
    matAfterSlicer.valid := True
  } elsewhen (matABSubSendFinish) {
    matAfterSlicer.valid := False
  }

  val matAfterSlicers = StreamDispatcher(in_Mats_Converter.withFragment(matAfterSlicer), slicerCfg.numCores)
  io.matAfterSlicers <> matAfterSlicers.mapVec(in_Mats_Converter.withoutFragment(_))
}

package Slicer

import Util._
import Interface._

import spinal.core._
import spinal.lib._

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
    val inst = slave Stream InstType
    val slicedInst = master Stream SlicedInstType
    val readAddrA = master Stream ReadAddrType
    val readDataA = slave Stream ReadDataTypeA
    val readAddrB = master Stream ReadAddrType
    val readDataB = slave Stream ReadDataTypeB
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

  val matASubReadRowCnt = Cnt(slicerCfg.systolicArraySideNum - 1, io.inst.fire, io.readDataA.fire)
  io.readAddrA.valid.setAsReg().init(False)
  io.readAddrA.StartAddr := instReg.input0Address + ((matARowSliceCnt * slicerCfg.systolicArraySideNum +
    matASubReadRowCnt) * instReg.input0Shape(1) / slicerCfg.systolicArraySideNum).resized +
    Mux(isMatMul, matAColSliceCnt, matBColSliceCnt)
  io.readAddrA.RepeatNum := 1
  io.readAddrA.Offset := 0
  when(io.readAddrA.fire) {
    io.readAddrA.valid := False
  } elsewhen (io.slicedInst.fire || matABSubSendFinish && !instFinish || io.readDataA.fire && !matASubReadRowCnt.willOverflowIfInc) {
    io.readAddrA.valid := True
  }

  val matASub =
    Vec.fill(slicerCfg.systolicArraySideNum, slicerCfg.systolicArraySideNum)(Reg(Bits(slicerCfg.elementWidthA bits)))
  io.readDataA.ready.setAsReg().init(False)
  when(io.readDataA.fire) {
    io.readDataA.ready := False
    matASub(matASubReadRowCnt) := io.readDataA.data.subdivideIn(slicerCfg.elementWidthA bits)
  } elsewhen (io.readAddrA.fire) {
    io.readDataA.ready := True
  }

  val matBSubReadRowCnt = Cnt(slicerCfg.systolicArraySideNum - 1, io.inst.fire, io.readDataB.fire)
  io.readAddrB.valid.setAsReg().init(False)
  io.readAddrB.StartAddr := instReg.input1Address +
    ((Mux[UInt](isMatMul, matAColSliceCnt, matARowSliceCnt) * slicerCfg.systolicArraySideNum +
      matBSubReadRowCnt) * instReg.input1Shape(1) / slicerCfg.systolicArraySideNum).resized +
    matBColSliceCnt
  io.readAddrB.RepeatNum := 1
  io.readAddrB.Offset := 0
  when(io.readAddrB.fire) {
    io.readAddrB.valid := False
  } elsewhen (io.slicedInst.fire || matABSubSendFinish && !instFinish || io.readDataB.fire && !matBSubReadRowCnt.willOverflowIfInc) {
    io.readAddrB.valid := True
  }

  val matBSub =
    Vec.fill(slicerCfg.systolicArraySideNum, slicerCfg.systolicArraySideNum)(Reg(Bits(slicerCfg.elementWidthB bits)))
  io.readDataB.ready.setAsReg().init(False)
  when(io.readDataB.fire) {
    io.readDataB.ready := False
    matBSub(matBSubReadRowCnt) := io.readDataB.data.subdivideIn(slicerCfg.elementWidthB bits)
  } elsewhen (io.readAddrB.fire) {
    io.readDataB.ready := True
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

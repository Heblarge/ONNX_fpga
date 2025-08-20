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

  val matInSubReadFinish = Bool()
  case class SlicerReader(ReadDataType: Data_mm2s_TypeDef, elementWidth: Int) extends Area {
    val readData = Stream(ReadDataType)
    val matABSubReadRowCnt = Cnt(slicerCfg.systolicArraySideNum - 1, io.inst.fire, readData.fire)

    val readAddr = Stream(ReadAddrType)
    readAddr.valid.setAsReg().init(False)
    readAddr.StartAddr := 0
    readAddr.RepeatNum := 1
    readAddr.Offset := 0
    when(readAddr.fire) {
      readAddr.valid := False
    } elsewhen (io.slicedInst.fire || matABSubSendFinish && !instFinish || readData.fire && !matABSubReadRowCnt.willOverflowIfInc) {
      readAddr.valid := True
    }

    val matABSub =
      Vec.fill(slicerCfg.systolicArraySideNum, slicerCfg.systolicArraySideNum)(Reg(Bits(elementWidth bits)))
    readData.ready.setAsReg().init(False)
    when(readData.fire) {
      readData.ready := False
      matABSub(matABSubReadRowCnt) := readData.data.subdivideIn(elementWidth bits)
    } elsewhen (readAddr.fire) {
      readData.ready := True
    }

    val matABSubReadFinish = Reg(Bool(), False)
    when(matInSubReadFinish) {
      matABSubReadFinish := False
    } elsewhen (matABSubReadRowCnt.willOverflow) {
      matABSubReadFinish := True
    }
  }

  val slicerReaderA = SlicerReader(ReadDataTypeA, slicerCfg.elementWidthA)
  val slicerReaderB = SlicerReader(ReadDataTypeB, slicerCfg.elementWidthB)
  matInSubReadFinish := slicerReaderA.matABSubReadFinish && slicerReaderB.matABSubReadFinish

  slicerReaderA.readAddr <> io.readAddrA
  slicerReaderA.readData <> io.readDataA
  io.readAddrA.StartAddr.allowOverride := instReg.input0Address + ((matARowSliceCnt * slicerCfg.systolicArraySideNum +
    slicerReaderA.matABSubReadRowCnt) * instReg.input0Shape(1) / slicerCfg.systolicArraySideNum).resized +
    Mux(isMatMul, matAColSliceCnt, matBColSliceCnt)

  slicerReaderB.readAddr <> io.readAddrB
  slicerReaderB.readData <> io.readDataB
  io.readAddrB.StartAddr.allowOverride := instReg.input1Address +
    ((Mux[UInt](isMatMul, matAColSliceCnt, matARowSliceCnt) * slicerCfg.systolicArraySideNum +
      slicerReaderB.matABSubReadRowCnt) * instReg.input1Shape(1) / slicerCfg.systolicArraySideNum).resized +
    matBColSliceCnt

  val matAfterSlicer = Stream(MatAfterSlicerType)
  val matABSubSendRowCnt = Cnt(slicerCfg.systolicArraySideNum - 1, io.inst.fire, matAfterSlicer.fire)
  matABSubSendFinish := matABSubSendRowCnt.willOverflow
  instFinish := matABSubSendFinish && matAColSliceCnt.willOverflowIfInc && matBColSliceCnt.willOverflowIfInc && matARowSliceCnt.willOverflowIfInc

  matAfterSlicer.A := slicerReaderA.matABSub.mapVec(_(matABSubSendRowCnt).asSInt)
  matAfterSlicer.B := Mux(
    isMatMul,
    slicerReaderB.matABSub(matABSubSendRowCnt).mapVec(_.asSInt),
    slicerReaderB.matABSub.shuffle(slicerCfg.systolicArraySideNum - 1 - _).mapVec(_(matABSubSendRowCnt).asSInt)
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

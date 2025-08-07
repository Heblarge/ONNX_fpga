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
  val matABSubReadRowCntWidth = log2Up(systolicArraySideNum)
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

  val matARowSliceNum = instReg.input0Shape(0) / slicerCfg.systolicArraySideNum
  val matBColSliceNum = instReg.input1Shape(1) / slicerCfg.systolicArraySideNum
  val matAColSliceNum = Mux(isMatMul, instReg.input0Shape(1) / slicerCfg.systolicArraySideNum, U(1))
  val matARowSliceCnt = Reg(UInt(slicerCfg.SlicecntWidth bits))
  val matBColSliceCnt = Reg(UInt(slicerCfg.SlicecntWidth bits))
  val matAColSliceCnt = Reg(UInt(slicerCfg.SlicecntWidth bits))
  val matABSubSendFinish = Bool()
  when(io.inst.fire) {
    matARowSliceCnt := 0
    matBColSliceCnt := 0
    matAColSliceCnt := 0
  } elsewhen (matABSubSendFinish) {
    when(matAColSliceCnt =/= matAColSliceNum - 1) {
      matAColSliceCnt := matAColSliceCnt + 1
    } otherwise {
      matAColSliceCnt := 0
      when(matBColSliceCnt =/= matBColSliceNum - 1) {
        matBColSliceCnt := matBColSliceCnt + 1
      } otherwise {
        matBColSliceCnt := 0
        when(matARowSliceCnt =/= matARowSliceNum - 1) {
          matARowSliceCnt := matARowSliceCnt + 1
        } otherwise {
          matARowSliceCnt := 0
        }
      }
    }
  }

  case class SlicerReader(ReadDataType: Data_mm2s_TypeDef, elementWidth: Int) extends Component {
    val io = new Bundle {
      val instFire = in port Bool()
      val matABSubReadRowCnt = out(Reg(UInt((slicerCfg.matABSubReadRowCntWidth bits))))
      val readStart = in port Bool()
      val matABSub =
        out(Vec.fill(slicerCfg.systolicArraySideNum, slicerCfg.systolicArraySideNum)(Reg(Bits(elementWidth bits))))
      val matInSubReadFinish = in port Bool()
      val matABSubReadFinish = out port Bool()
      val readAddr = master Stream ReadAddrType
      val readData = slave Stream ReadDataType
    }

    when(io.instFire) {
      io.matABSubReadRowCnt := 0
    } elsewhen (io.readData.fire) {
      io.matABSubReadRowCnt :=
        Mux(io.matABSubReadRowCnt === slicerCfg.systolicArraySideNum - 1, U(0), io.matABSubReadRowCnt + 1)
    }

    io.readAddr.valid.setAsReg().init(False)
    io.readAddr.StartAddr := 0
    io.readAddr.RepeatNum := 1
    io.readAddr.Offset := 0
    when(io.readAddr.fire) {
      io.readAddr.valid := False
    } elsewhen (io.readStart || io.readData.fire && io.matABSubReadRowCnt =/= slicerCfg.systolicArraySideNum - 1) {
      io.readAddr.valid := True
    }

    io.readData.ready.setAsReg().init(False)
    when(io.readData.fire) {
      io.readData.ready := False
      io.matABSub(io.matABSubReadRowCnt) := io.readData.data.subdivideIn(elementWidth bits)
    } elsewhen (io.readAddr.fire) {
      io.readData.ready := True
    }

    io.matABSubReadFinish.setAsReg().init(False)
    when(io.matInSubReadFinish) {
      io.matABSubReadFinish := False
    } otherwise {
      when(io.readData.fire && io.matABSubReadRowCnt === slicerCfg.systolicArraySideNum - 1) {
        io.matABSubReadFinish := True
      }
    }
  }

  val slicerReaderA = SlicerReader(ReadDataTypeA, slicerCfg.elementWidthA)
  val slicerReaderB = SlicerReader(ReadDataTypeB, slicerCfg.elementWidthB)
  val matInSubReadFinish = slicerReaderA.io.matABSubReadFinish && slicerReaderB.io.matABSubReadFinish

  slicerReaderA.io.instFire := io.inst.fire
  slicerReaderA.io.readStart := io.slicedInst.fire || matABSubSendFinish && !instFinish
  slicerReaderA.io.matInSubReadFinish := matInSubReadFinish
  slicerReaderA.io.readAddr <> io.readAddrA
  slicerReaderA.io.readData <> io.readDataA
  io.readAddrA.StartAddr.allowOverride()
  io.readAddrA.StartAddr := instReg.input0Address + ((matARowSliceCnt * slicerCfg.systolicArraySideNum +
    slicerReaderA.io.matABSubReadRowCnt) * instReg.input0Shape(1) / slicerCfg.systolicArraySideNum).resized +
    Mux(isMatMul, matAColSliceCnt, matBColSliceCnt)

  slicerReaderB.io.instFire := io.inst.fire
  slicerReaderB.io.readStart := io.slicedInst.fire || matABSubSendFinish && !instFinish
  slicerReaderB.io.matInSubReadFinish := matInSubReadFinish
  slicerReaderB.io.readAddr <> io.readAddrB
  slicerReaderB.io.readData <> io.readDataB
  io.readAddrB.StartAddr.allowOverride()
  io.readAddrB.StartAddr := instReg.input1Address + ((Mux(isMatMul, matAColSliceCnt, matARowSliceCnt) *
    slicerCfg.systolicArraySideNum + slicerReaderB.io.matABSubReadRowCnt) * instReg.input1Shape(1) /
    slicerCfg.systolicArraySideNum).resized + matBColSliceCnt

  val matAfterSlicer = Stream(MatAfterSlicerType)
  val matABSubSendRowCnt = Reg(UInt(slicerCfg.matABSubReadRowCntWidth bits))
  matABSubSendFinish := matAfterSlicer.fire && matABSubSendRowCnt === slicerCfg.systolicArraySideNum - 1
  instFinish := matABSubSendFinish && matAColSliceCnt === matAColSliceNum - 1 && matBColSliceCnt === matBColSliceNum - 1 && matARowSliceCnt === matARowSliceNum - 1
  when(io.inst.fire) {
    matABSubSendRowCnt := 0
  } elsewhen (matAfterSlicer.fire) {
    matABSubSendRowCnt := Mux(matABSubSendRowCnt === slicerCfg.systolicArraySideNum - 1, U(0), matABSubSendRowCnt + 1)
  }

  matAfterSlicer.A := slicerReaderA.io.matABSub.mapVec(_(matABSubSendRowCnt).asSInt)
  matAfterSlicer.B := Mux(
    isMatMul,
    slicerReaderB.io.matABSub(matABSubSendRowCnt).mapVec(_.asSInt),
    slicerReaderB.io.matABSub.shuffle(slicerCfg.systolicArraySideNum - 1 - _).mapVec(_(matABSubSendRowCnt).asSInt)
  )
  matAfterSlicer.CoreInstruction.assignFromInst(instReg, matARowSliceCnt, matBColSliceCnt)
  matAfterSlicer.Final := matABSubSendRowCnt === slicerCfg.systolicArraySideNum - 1 && matAColSliceCnt === matAColSliceNum - 1
  matAfterSlicer.valid.setAsReg().init(False)
  when(matInSubReadFinish) {
    matAfterSlicer.valid := True
  } elsewhen (matAfterSlicer.fire && matABSubSendRowCnt === slicerCfg.systolicArraySideNum - 1) {
    matAfterSlicer.valid := False
  }

  val matAfterSlicers = StreamDispatcher(in_Mats_Converter.withFragment(matAfterSlicer), slicerCfg.numCores)
  io.matAfterSlicers <> matAfterSlicers.mapVec(in_Mats_Converter.withoutFragment(_))
}

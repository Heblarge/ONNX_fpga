package Slicer

import Util._
import Interface._

import spinal.core._
import spinal.lib.{Stream, master, slave, StreamArbiterFactory}

case class CollectorCfg(
    UIDWidth: Int,
    AddressWidth: Int,
    ShapeWidth: Int,
    SlicecntWidth: Int,
    slicedInstFifoDepth: Int,
    systolicArraySideNum: Int,
    activationUnitNum: Int,
    elementWidthZ: Int,
    numCores: Int
) {
  require(
    systolicArraySideNum * systolicArraySideNum % activationUnitNum == 0,
    "activationUnitNum mismatch systolicArraySideNum"
  )
  val activationRowNum = systolicArraySideNum * systolicArraySideNum / activationUnitNum
  val dataWidthZ = systolicArraySideNum * elementWidthZ
  val matZSubReceiveRowCntWidth = log2Up(activationRowNum)
  val matZSubWriteRowCntWidth = log2Up(systolicArraySideNum)
  val CoreSelectWidth = log2Up(numCores)
}

case class Collector(collectorCfg: CollectorCfg) extends Component {
  def SlicedInstType =
    Sliced_ComputeInstruction_TypeDef(
      UIDWidth = collectorCfg.UIDWidth,
      AddressWidth = collectorCfg.AddressWidth,
      ShapeWidth = collectorCfg.ShapeWidth
    )

  def MatAfterActivationType =
    out_Mats_AfterActivation_TypeDef(
      MatX_Width = collectorCfg.activationUnitNum,
      Activation_x_Width = collectorCfg.elementWidthZ,
      UIDWidth = collectorCfg.UIDWidth,
      SlicecntWidth = collectorCfg.SlicecntWidth
    )

  def WriteAddrType =
    Task_s2mm_TypeDef(
      mem_addr_width = collectorCfg.AddressWidth,
      mem_data_width = 0,
      RepeatNum_width = 1,
      Enable_UnPadding_logic = false
    )

  def WriteDataType = Data_s2mm_TypeDef(collectorCfg.dataWidthZ)

  val io = new Bundle {
    val slicedInst = slave Stream SlicedInstType
    val writeAddr = master Stream WriteAddrType
    val writeData = master Stream WriteDataType
    val matAfterActivations = Vec.fill(collectorCfg.numCores)(slave Stream MatAfterActivationType)
  }

  val slicedInst = Stream(SlicedInstType)
  io.slicedInst.queue(collectorCfg.slicedInstFifoDepth) <> slicedInst

  slicedInst.ready.setAsReg().init(True)
  val slicedInstReg = Reg(SlicedInstType)
  val instFinish = Bool()
  when(slicedInst.fire) {
    slicedInstReg := slicedInst.payload
    slicedInst.ready := False
  } elsewhen (instFinish) {
    slicedInst.ready := True
  }

  val matAfterActivation = Stream(MatAfterActivationType)
  val lock = RegNextWhen(!matAfterActivation.Final, matAfterActivation.fire, False)
  val matAfterActivations =
    io.matAfterActivations.mapVec(out_Mats_AfterActivation_Converter.withFragment(_, slicedInstReg.UID, lock))
  matAfterActivation << out_Mats_AfterActivation_Converter.withoutFragment(
    StreamArbiterFactory.roundRobin.fragmentLock.on(matAfterActivations)
  )

  val matZSubReceiveRowCnt = Reg(UInt(collectorCfg.matZSubReceiveRowCntWidth bits))
  val matZsubReceiveFinish = matAfterActivation.fire && matZSubReceiveRowCnt === collectorCfg.activationRowNum - 1
  when(slicedInst.fire) {
    matZSubReceiveRowCnt := 0
  } elsewhen (matAfterActivation.fire) {
    matZSubReceiveRowCnt :=
      Mux(matZSubReceiveRowCnt === collectorCfg.activationRowNum - 1, U(0), matZSubReceiveRowCnt + 1)
  }

  matAfterActivation.ready.setAsReg().init(False)
  val matZsubWriteFinish = Bool()
  when(slicedInst.fire || matZsubWriteFinish && !instFinish) {
    matAfterActivation.ready := True
  } elsewhen (matAfterActivation.fire && matZSubReceiveRowCnt === collectorCfg.activationRowNum - 1) {
    matAfterActivation.ready := False
  }

  val matARowSliceCnt = RegNextWhen(
    matAfterActivation.CoreInstruction_AfterActivation.Collector_Instruction.MatA_row_slice_cnt,
    matAfterActivation.fire && matZSubReceiveRowCnt === 0
  )
  val matBColSliceCnt = RegNextWhen(
    matAfterActivation.CoreInstruction_AfterActivation.Collector_Instruction.MatB_col_slice_cnt,
    matAfterActivation.fire && matZSubReceiveRowCnt === 0
  )
  val matZsub =
    Vec.fill(collectorCfg.activationRowNum, collectorCfg.activationUnitNum)(Reg(SInt(collectorCfg.elementWidthZ bits)))
  when(matAfterActivation.fire) {
    matZsub(matZSubReceiveRowCnt) := matAfterActivation.Activation_x
  }

  val matZSubWriteRowCnt = Reg(UInt(collectorCfg.matZSubWriteRowCntWidth bits))
  matZsubWriteFinish := io.writeData.fire && matZSubWriteRowCnt === collectorCfg.systolicArraySideNum - 1
  when(slicedInst.fire) {
    matZSubWriteRowCnt := 0
  } elsewhen (io.writeData.fire) {
    matZSubWriteRowCnt :=
      Mux(matZSubWriteRowCnt === collectorCfg.systolicArraySideNum - 1, U(0), matZSubWriteRowCnt + 1)
  }

  io.writeAddr.StartAddr :=
    slicedInstReg.outputAddress + ((Mux(slicedInstReg.doTranspose, matBColSliceCnt, matARowSliceCnt) *
      collectorCfg.systolicArraySideNum + matZSubWriteRowCnt) * slicedInstReg.outputShape(1) /
      collectorCfg.systolicArraySideNum).resized + Mux(slicedInstReg.doTranspose, matARowSliceCnt, matBColSliceCnt)
  io.writeAddr.RepeatNum := 1
  io.writeAddr.Offset := 0
  io.writeAddr.valid.setAsReg().init(False)
  when(io.writeAddr.fire) {
    io.writeAddr.valid := False
  } elsewhen (matZsubReceiveFinish || io.writeData.fire && matZSubWriteRowCnt =/= collectorCfg.systolicArraySideNum - 1) {
    io.writeAddr.valid := True
  }

  io.writeData.data := matZsub.asBits.subdivideIn(collectorCfg.dataWidthZ bits)(matZSubWriteRowCnt)
  io.writeData.Final := True
  io.writeData.valid.setAsReg.init(False)
  when(io.writeData.fire) {
    io.writeData.valid := False
  } elsewhen (io.writeAddr.fire) {
    io.writeData.valid := True
  }

  val matZSliceNum = slicedInstReg.outputShape(0) / collectorCfg.systolicArraySideNum *
    slicedInstReg.outputShape(1) / collectorCfg.systolicArraySideNum
  val matZSliceCnt = Reg(UInt(2 * collectorCfg.SlicecntWidth bits))
  instFinish := matZsubWriteFinish && matZSliceCnt === matZSliceNum - 1
  when(slicedInst.fire) {
    matZSliceCnt := 0
  } elsewhen (matZsubWriteFinish) {
    matZSliceCnt := Mux(matZSliceCnt === matZSliceNum - 1, U(0), matZSliceCnt + 1)
  }
}

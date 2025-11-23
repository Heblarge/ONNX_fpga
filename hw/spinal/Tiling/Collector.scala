package Tiling

import Util._
import Interface._
import DataPump._

import spinal.core._
import spinal.lib.{Stream, master, slave, StreamArbiterFactory}

/**
 * Collector Configuration Parameters
 * 收集器配置参数
 * 
 * @param UIDWidth Unique Instruction ID width - 指令唯一标识符宽度
 * @param AddressWidth Memory address width - 内存地址宽度
 * @param ShapeWidth Matrix shape dimension width - 矩阵形状维度宽度
 * @param SlicecntWidth Slice counter width - 切片计数器宽度
 * @param slicedInstFifoDepth Sliced instruction FIFO depth - 切片指令FIFO深度
 * @param systolicArraySideNum Systolic array side dimension - 脉动阵列边长
 * @param activationUnitNum Number of activation units - 激活单元数量
 * @param elementWidthZ Output matrix element width - 输出矩阵元素宽度
 * @param numCores Number of processing cores - 处理核心数量
 */
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
  val CoreSelectWidth = log2Up(numCores)
}

/**
 * Matrix Collector Component - 矩阵收集器组件
 * 
 * Main function: Collect and reassemble computation results from multiple cores into complete matrices
 * 主要功能：从多个核心收集并重新组装计算结果为完整矩阵
 */
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

  def MemoryWritePortType =
    MemoryWritePort_TypeDef(AddressWidth = collectorCfg.AddressWidth, DataWidth = collectorCfg.dataWidthZ)

  val io = new Bundle {
    val slicedInst = slave Stream SlicedInstType
    val memoryWritePort = master(MemoryWritePortType)
    val matAfterActivations = Vec.fill(collectorCfg.numCores)(slave Stream MatAfterActivationType)
    val instFinish = out Bool ()
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

  val matZSubReceiveRowCnt = Cnt(collectorCfg.activationRowNum - 1, slicedInst.fire, matAfterActivation.fire)
  val matZsubReceiveFinish = matZSubReceiveRowCnt.willOverflow

  matAfterActivation.ready.setAsReg().init(False)
  val matZsubWriteFinish = Bool()
  when(slicedInst.fire || matZsubWriteFinish && !instFinish) {
    matAfterActivation.ready := True
  } elsewhen (matZsubReceiveFinish) {
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

  val matZSubWriteRowCnt = Cnt(collectorCfg.systolicArraySideNum - 1, slicedInst.fire, io.memoryWritePort.Valid)
  matZsubWriteFinish := matZSubWriteRowCnt.willOverflow
  io.memoryWritePort.Valid := Mux(matZSubWriteRowCnt === 0, matZsubReceiveFinish, True)
  io.memoryWritePort.Address :=
    slicedInstReg.outputAddress + ((Mux(slicedInstReg.doTranspose, matBColSliceCnt, matARowSliceCnt) *
      collectorCfg.systolicArraySideNum + matZSubWriteRowCnt) * slicedInstReg.outputShape(1) /
      collectorCfg.systolicArraySideNum).resized + Mux(slicedInstReg.doTranspose, matARowSliceCnt, matBColSliceCnt)

  io.memoryWritePort.Data := matZsub.asBits.subdivideIn(collectorCfg.dataWidthZ bits)(matZSubWriteRowCnt)

  val matZSliceCnt = Cnt(
    slicedInstReg.outputShape(0) / collectorCfg.systolicArraySideNum *
      slicedInstReg.outputShape(1) / collectorCfg.systolicArraySideNum - 1,
    slicedInst.fire,
    matZsubWriteFinish
  )
  instFinish := matZSliceCnt.willOverflow
  io.instFinish := RegNext(instFinish, False)
}

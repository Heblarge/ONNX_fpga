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

  // 假设外部存储每个数据占用 32位
  val bytesPerVector = collectorCfg.systolicArraySideNum * 4
  // 输出端口数据位宽 (32-bit 对齐)
  val outputDataWidth = collectorCfg.systolicArraySideNum * 32
  // 字节掩码位宽
  val byteMaskWidth = outputDataWidth / 8

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
    MemoryWritePort_TypeDef(AddressWidth = collectorCfg.AddressWidth, DataWidth = outputDataWidth)

//  def MemoryWritePortType =
//    MemoryWritePort_TypeDef(AddressWidth = collectorCfg.AddressWidth, DataWidth = collectorCfg.dataWidthZ)

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

  // 修改为字节寻址
//  io.memoryWritePort.Address :=
//    slicedInstReg.outputAddress + ((Mux(slicedInstReg.doTranspose, matBColSliceCnt, matARowSliceCnt) *
//      collectorCfg.systolicArraySideNum + matZSubWriteRowCnt) * slicedInstReg.outputShape(1) /
//      collectorCfg.systolicArraySideNum).resized + Mux(slicedInstReg.doTranspose, matARowSliceCnt, matBColSliceCnt)

//  val rowOffset = Mux(slicedInstReg.doTranspose, matBColSliceCnt, matARowSliceCnt) * U(collectorCfg.systolicArraySideNum)
//  val colOffset = Mux(slicedInstReg.doTranspose, matARowSliceCnt, matBColSliceCnt)
//  val offsetIdxZ = ((rowOffset + matZSubWriteRowCnt) * slicedInstReg.outputShape(1) / U(collectorCfg.systolicArraySideNum)).resized + colOffset
//  io.memoryWritePort.Address := slicedInstReg.outputAddress + (offsetIdxZ * U(bytesPerVector)).resized

//  io.memoryWritePort.Data := matZsub.asBits.subdivideIn(collectorCfg.dataWidthZ bits)(matZSubWriteRowCnt)

  val rowOffset = Mux(slicedInstReg.doTranspose, matBColSliceCnt, matARowSliceCnt) * U(collectorCfg.systolicArraySideNum)
  val colOffset = Mux(slicedInstReg.doTranspose, matARowSliceCnt, matBColSliceCnt)
  val offsetIdxZ = ((rowOffset + matZSubWriteRowCnt) * slicedInstReg.outputShape(1) / U(collectorCfg.systolicArraySideNum)).resized + colOffset
  io.memoryWritePort.Address := slicedInstReg.outputAddress + (offsetIdxZ * U(bytesPerVector)).resized

  val matZsub2D = Vec(Vec(SInt(collectorCfg.elementWidthZ bits), collectorCfg.systolicArraySideNum), collectorCfg.systolicArraySideNum)
  for (r <- 0 until collectorCfg.systolicArraySideNum) {
    for (c <- 0 until collectorCfg.systolicArraySideNum) {
      // 计算 flatten 索引
      val totalIdx = r * collectorCfg.systolicArraySideNum + c
      val actRow = totalIdx / collectorCfg.activationUnitNum
      val actCol = totalIdx % collectorCfg.activationUnitNum
      matZsub2D(r)(c) := matZsub(actRow)(actCol)
    }
  }
  val currentRowData = matZsub2D(matZSubWriteRowCnt)
  val outputDataVec = Vec(Bits(32 bits), collectorCfg.systolicArraySideNum)
  for (i <- 0 until collectorCfg.systolicArraySideNum) {
    // 符号扩展到 32位
    outputDataVec(i) := currentRowData(i).resize(32 bits).asBits
  }
  io.memoryWritePort.Data := outputDataVec.asBits
  io.memoryWritePort.Wen := B(byteMaskWidth bits, default -> io.memoryWritePort.Valid)


  val matZSliceCnt = Cnt(
    slicedInstReg.outputShape(0) / collectorCfg.systolicArraySideNum *
      slicedInstReg.outputShape(1) / collectorCfg.systolicArraySideNum - 1,
    slicedInst.fire,
    matZsubWriteFinish
  )
  instFinish := matZSliceCnt.willOverflow
  io.instFinish := RegNext(instFinish, False)
}

//case class CollectorWrap(collectorCfg: CollectorCfg) extends Component {
//  val collector=Collector(collectorCfg)
//  def MemoryWritePortType =
//    MemoryWritePort_TypeDef(AddressWidth = collectorCfg.AddressWidth, DataWidth = collectorCfg.systolicArraySideNum*32)
//
//  val io = new Bundle {
//    val slicedInst = slave Stream collector.SlicedInstType
//    val memoryWritePort = master(MemoryWritePortType)
//    val matAfterActivations = Vec.fill(collectorCfg.numCores)(slave Stream collector.MatAfterActivationType)
//    val instFinish = out Bool ()
//  }
//
//  val LanesN = collector.io.memoryWritePort.Data.subdivideIn(collectorCfg.elementWidthZ bits)
//  val Lanes32 = Vec(Bits(32 bits), collectorCfg.systolicArraySideNum)
//
//  for(i <- 0 until collectorCfg.systolicArraySideNum){
//    Lanes32(i) := LanesN(i).asSInt.resize(32 bits).asBits
//  }
//  io.memoryWritePort.Data := Lanes32.asBits
//  io.memoryWritePort.Valid := collector.io.memoryWritePort.Valid
//  io.memoryWritePort.Address := collector.io.memoryWritePort.Address
//  io.memoryWritePort.clk := ClockDomain.current.readClockWire
//  io.memoryWritePort.Wen := collector.io.memoryWritePort.Wen
//
//  collector.io.slicedInst <> io.slicedInst
//  collector.io.matAfterActivations <> io.matAfterActivations
//  io.instFinish <> collector.io.instFinish
//}

case class CollectorWrap(collectorCfg: CollectorCfg) extends Component {
  val collector = Collector(collectorCfg)

  // 定义与 Collector 内部一致的端口类型
  def MemoryWritePortType =
    MemoryWritePort_TypeDef(AddressWidth = collectorCfg.AddressWidth, DataWidth = collectorCfg.systolicArraySideNum * 32)

  val io = new Bundle {
    val slicedInst = slave Stream collector.SlicedInstType
    val memoryWritePort = master(MemoryWritePortType)
    val matAfterActivations = Vec.fill(collectorCfg.numCores)(slave Stream collector.MatAfterActivationType)
    val instFinish = out Bool ()
  }

  // 直接连接信号，移除旧的 LanesN/Lanes32 扩展逻辑
  io.memoryWritePort.Valid   := collector.io.memoryWritePort.Valid
  io.memoryWritePort.Address := collector.io.memoryWritePort.Address
  io.memoryWritePort.Data    := collector.io.memoryWritePort.Data
  io.memoryWritePort.Wen     := collector.io.memoryWritePort.Wen
  // 显式驱动时钟信号到外部接口
  io.memoryWritePort.clk     := ClockDomain.current.readClockWire

  collector.io.slicedInst <> io.slicedInst
  collector.io.matAfterActivations <> io.matAfterActivations
  io.instFinish <> collector.io.instFinish
}

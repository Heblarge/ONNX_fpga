package Tiling

import Util._
import Interface._
import DataPump._

import spinal.core._
import spinal.lib._
import MatrixComputeUnit.SystolicArray2D.SIntShifter

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
 * @param shiftLeft_A
 * @param shiftLeft_B
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
    val instFinish = out Bool ()
  }

  val N = slicerCfg.systolicArraySideNum

  // =====================================================================
  // 指令接收
  // =====================================================================
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

  // =====================================================================
  // 双缓冲 ping-pong: 2 个 bank，读侧和发侧各自独立推进
  // =====================================================================
  // bankFull(b) = true 表示 bank b 已填满，发侧可读；读侧只能写 !bankFull 的 bank
  val bankFull = Vec(Reg(Bool()) init(False), 2)
  val rdBank = Reg(UInt(1 bits)) init(0)
  val sndBank = Reg(UInt(1 bits)) init(0)
  when(io.inst.fire) {
    bankFull(0) := False
    bankFull(1) := False
    rdBank := 0
    sndBank := 0
  }

  // =====================================================================
  // 读侧：独立的切片计数器（由 rdSubDone 驱动递增）
  // =====================================================================
  val rdSubDone = Bool()

  val rd_matAColSliceCnt = Cnt(
    Mux(isMatMul, instReg.input0Shape(1) / N, U(1)) - 1,
    io.inst.fire,
    rdSubDone
  )
  val rd_matBColSliceCnt =
    Cnt(instReg.input1Shape(1) / N - 1, io.inst.fire, rd_matAColSliceCnt.willOverflow)
  val rd_matARowSliceCnt =
    Cnt(instReg.input0Shape(0) / N - 1, io.inst.fire, rd_matBColSliceCnt.willOverflow)

  val rdAllSlicesDone = Reg(Bool()) init(False)
  when(io.inst.fire) { rdAllSlicesDone := False }
  when(rdSubDone && rd_matAColSliceCnt.willOverflowIfInc &&
       rd_matBColSliceCnt.willOverflowIfInc && rd_matARowSliceCnt.willOverflowIfInc) {
    rdAllSlicesDone := True
  }

  // 读侧启动条件: 目标 bank 空 && 还有子块要读
  // 首次启动: slicedInst.fire
  // 后续启动: 上一个子块读完后(rdSubDone)，如果 bank 空就立即启动，否则等待
  val rdWaitForBank = Reg(Bool()) init(False)  // 读完一个子块后 bank 满，等待 bank 释放
  when(io.inst.fire) { rdWaitForBank := False }

  val rdStartCond = Bool()
  rdStartCond := False
  when(io.slicedInst.fire && !bankFull(rdBank)) {
    rdStartCond := True
  }
  // rdSubDone 后如果还有子块且 bank 空，下一拍启动
  when(RegNext(rdSubDone && !rdAllSlicesDone, False) && !bankFull(rdBank) && !rdAllSlicesDone) {
    rdStartCond := True
  }
  // bank 释放后，如果有等待中的读请求
  when(rdWaitForBank && !bankFull(rdBank) && !rdAllSlicesDone) {
    rdStartCond := True
    rdWaitForBank := False
  }

  val matASubReadRowCntIncCond = Bool()
  val matASubReadRowCnt = Cnt(N - 1, io.inst.fire, matASubReadRowCntIncCond)
  matASubReadRowCntIncCond := Mux(
    matASubReadRowCnt === 0,
    rdStartCond,
    True
  )
  io.memoryReadPortA.Valid.setAsReg().init(False)
  io.memoryReadPortA.Valid := matASubReadRowCntIncCond
  io.memoryReadPortA.Address.setAsReg()
  io.memoryReadPortA.Address := instReg.input0Address + ((rd_matARowSliceCnt * N +
    matASubReadRowCnt) * instReg.input0Shape(1) / N).resized +
    Mux(isMatMul, rd_matAColSliceCnt, rd_matBColSliceCnt)

  val matBSubReadRowCntIncCond = Bool()
  val matBSubReadRowCnt = Cnt(N - 1, io.inst.fire, matBSubReadRowCntIncCond)
  matBSubReadRowCntIncCond := Mux(
    matBSubReadRowCnt === 0,
    rdStartCond,
    True
  )
  io.memoryReadPortB.Valid.setAsReg().init(False)
  io.memoryReadPortB.Valid := matBSubReadRowCntIncCond
  io.memoryReadPortB.Address.setAsReg()
  io.memoryReadPortB.Address := instReg.input1Address +
    ((Mux[UInt](isMatMul, rd_matAColSliceCnt, rd_matARowSliceCnt) * N +
      matBSubReadRowCnt) * instReg.input1Shape(1) / N).resized +
    rd_matBColSliceCnt

  // =====================================================================
  // 双缓冲寄存器堆
  // =====================================================================
  val matASub = Vec.fill(2, N, N)(Reg(Bits(slicerCfg.elementWidthA bits)))
  val matBSub = Vec.fill(2, N, N)(Reg(Bits(slicerCfg.elementWidthB bits)))

  // 数据从 memory 返回比 Valid 晚 1 拍（RegNext），行号晚 2 拍（RegNext×2）
  // 但 rdBank 也可能在此期间翻转，所以必须用延迟版本
  val rdBank_d1 = RegNext(rdBank, U(0, 1 bits))

  when(RegNext(io.memoryReadPortA.Valid, False)) {
    matASub(rdBank_d1)(RegNext(RegNext(matASubReadRowCnt.implicitValue))) := io.memoryReadPortA.Data.subdivideIn(
      slicerCfg.elementWidthA bits
    )
  }
  when(RegNext(io.memoryReadPortB.Valid, False)) {
    matBSub(rdBank_d1)(RegNext(RegNext(matBSubReadRowCnt.implicitValue))) := io.memoryReadPortB.Data.subdivideIn(
      slicerCfg.elementWidthB bits
    )
  }

  // 读完成检测
  val matASubReadFinish = Reg(Bool(), False)
  val matBSubReadFinish = Reg(Bool(), False)
  val matInSubReadFinishRaw = RegNext(matASubReadFinish, False) && RegNext(matBSubReadFinish, False)
  val matInSubReadFinish = matInSubReadFinishRaw && !RegNext(matInSubReadFinishRaw, False)
  rdSubDone := matInSubReadFinish

  when(matInSubReadFinish) {
    matASubReadFinish := False
    matBSubReadFinish := False
    // 标记当前 bank 为满，翻转到下一个 bank
    bankFull(rdBank) := True
    rdBank := ~rdBank
    // 如果下一个 bank 也满，需要等待
    when(bankFull(~rdBank)) {
      rdWaitForBank := True
    }
  } otherwise {
    when(matASubReadRowCnt.willOverflow) {
      matASubReadFinish := True
    }
    when(matBSubReadRowCnt.willOverflow) {
      matBSubReadFinish := True
    }
  }

  // 保存读侧切片坐标到 bank 元数据，在 matInSubReadFinish 时捕获
  val bankMeta_ARowSlice = Vec(Reg(UInt(rd_matARowSliceCnt.cnt.getWidth bits)) init(0), 2)
  val bankMeta_BColSlice = Vec(Reg(UInt(rd_matBColSliceCnt.cnt.getWidth bits)) init(0), 2)
  val bankMeta_AColSlice = Vec(Reg(UInt(rd_matAColSliceCnt.cnt.getWidth bits)) init(0), 2)
  when(matInSubReadFinish) {
    bankMeta_ARowSlice(rdBank) := rd_matARowSliceCnt.value
    bankMeta_BColSlice(rdBank) := rd_matBColSliceCnt.value
    bankMeta_AColSlice(rdBank) := rd_matAColSliceCnt.value
  }

  // =====================================================================
  // 发侧
  // =====================================================================
  val matAfterSlicer = Stream(MatAfterSlicerType)
  val matABSubSendRowCnt = Cnt(N - 1, io.inst.fire, matAfterSlicer.fire)
  val matABSubSendFinish = matABSubSendRowCnt.willOverflow

  // 从 bank 元数据获取当前发送子块的切片坐标
  val snd_ARowSlice = bankMeta_ARowSlice(sndBank)
  val snd_BColSlice = bankMeta_BColSlice(sndBank)
  val snd_AColSlice = bankMeta_AColSlice(sndBank)

  val snd_AColMax = Mux(isMatMul, instReg.input0Shape(1) / N, U(1)) - 1
  val snd_BColMax = instReg.input1Shape(1) / N - 1
  val snd_ARowMax = instReg.input0Shape(0) / N - 1

  val sndIsLast = (snd_AColSlice === snd_AColMax.resized) &&
                  (snd_BColSlice === snd_BColMax.resized) &&
                  (snd_ARowSlice === snd_ARowMax.resized)

  instFinish := matABSubSendFinish && sndIsLast
  io.instFinish := RegNext(instFinish, False)

  // 发侧 valid 控制：bank 满时启动，发完时停止并释放 bank
  val sndSending = Reg(Bool()) init(False)
  when(!sndSending && bankFull(sndBank) && !io.inst.fire) {
    sndSending := True
  }
  when(matABSubSendFinish || io.inst.fire) {
    sndSending := False
  }
  when(matABSubSendFinish) {
    bankFull(sndBank) := False
    sndBank := ~sndBank
  }

  // ================== A / B shifter ==================
  val shiftersA = Seq.fill(N) {
    SIntShifter(slicerCfg.elementWidthA, slicerCfg.elementWidthA)
  }
  val shiftersB = Seq.fill(N) {
    SIntShifter(slicerCfg.elementWidthB, slicerCfg.elementWidthB)
  }

  val matASubShifted = Vec(SInt(slicerCfg.elementWidthA bits), N)
  val matBSubShifted = Vec(SInt(slicerCfg.elementWidthB bits), N)

  for(i <- 0 until N) {
    val aElemBits = matASub(sndBank)(i)(matABSubSendRowCnt)
    shiftersA(i).io.input := aElemBits.asSInt
    shiftersA(i).io.shiftAmount := -instReg.shiftLeft_A.resize(log2Up(slicerCfg.elementWidthA + 1) + 1 bits)
    matASubShifted(i) := shiftersA(i).io.output

    val bElemBits = Mux(
      isMatMul,
      matBSub(sndBank)(matABSubSendRowCnt)(i),
      matBSub(sndBank)(N - 1 - i)(matABSubSendRowCnt)
    )
    shiftersB(i).io.input := bElemBits.asSInt
    shiftersB(i).io.shiftAmount := -instReg.shiftLeft_B.resize(log2Up(slicerCfg.elementWidthB + 1) + 1 bits)
    matBSubShifted(i) := shiftersB(i).io.output
  }

  matAfterSlicer.A := matASubShifted
  matAfterSlicer.B := matBSubShifted

  matAfterSlicer.CoreInstruction.assignFromInst(instReg, snd_ARowSlice.resized, snd_BColSlice.resized)
  matAfterSlicer.Final := matABSubSendRowCnt.willOverflowIfInc && (snd_AColSlice === snd_AColMax.resized)
  matAfterSlicer.valid := sndSending


  val matAfterSlicers = StreamDispatcher(in_Mats_Converter.withFragment(matAfterSlicer), slicerCfg.numCores)
  io.matAfterSlicers <> matAfterSlicers.mapVec(in_Mats_Converter.withoutFragment(_))
}

case class SlicerWrap(slicerCfg: SlicerCfg) extends Component {
  val slicer = Slicer(slicerCfg)
  def InstType =
    ComputeInstruction_Simplified_TypeDef(
      UIDWidth = slicerCfg.UIDWidth,
      ShiftWidth = slicerCfg.ShiftWidth,
      AddressWidth = slicerCfg.AddressWidth,
      ShapeWidth = slicerCfg.ShapeWidth
    )
  def MemoryReadPortTypeA =
    MemoryReadPort_TypeDef(AddressWidth = slicerCfg.AddressWidth, DataWidth = slicerCfg.systolicArraySideNum*32)
  def MemoryReadPortTypeB =
    MemoryReadPort_TypeDef(AddressWidth = slicerCfg.AddressWidth, DataWidth = slicerCfg.systolicArraySideNum*32)

  val io = new Bundle {
    val inst = slave Stream InstType
    val slicedInst = master Stream slicer.SlicedInstType
    val memoryReadPortA = master(MemoryReadPortTypeA)
    val memoryReadPortB = master(MemoryReadPortTypeB)
    val matAfterSlicers = Vec.fill(slicerCfg.numCores)(master Stream slicer.MatAfterSlicerType)
    val instFinish = out Bool ()
  }

  val aLanes32 = io.memoryReadPortA.Data.subdivideIn(32 bits)
  val aLanesN = Vec(Bits(slicerCfg.elementWidthA bits), slicerCfg.systolicArraySideNum)

  for (i <- 0 until slicerCfg.systolicArraySideNum) {
    aLanesN(i) := aLanes32(i).asSInt.resize(slicerCfg.elementWidthA).asBits
  }
  slicer.io.memoryReadPortA.Data := aLanesN.asBits
  io.memoryReadPortA.Valid := slicer.io.memoryReadPortA.Valid
  io.memoryReadPortA.Address := slicer.io.memoryReadPortA.Address
  io.memoryReadPortA.clk := ClockDomain.current.readClockWire
  io.memoryReadPortA.rst := ClockDomain.current.readResetWire

  val bLanes32 = io.memoryReadPortB.Data.subdivideIn(32 bits)
  val bLanesN = Vec(Bits(slicerCfg.elementWidthB bits), slicerCfg.systolicArraySideNum)

  for (i <- 0 until slicerCfg.systolicArraySideNum) {
    bLanesN(i) := bLanes32(i).asSInt.resize(slicerCfg.elementWidthB).asBits
  }
  slicer.io.memoryReadPortB.Data := bLanesN.asBits
  io.memoryReadPortB.Valid := slicer.io.memoryReadPortB.Valid
  io.memoryReadPortB.Address := slicer.io.memoryReadPortB.Address
  io.memoryReadPortB.clk := ClockDomain.current.readClockWire
  io.memoryReadPortB.rst := ClockDomain.current.readResetWire

  slicer.io.inst.payload.assignFromInst(io.inst.payload)
  slicer.io.inst.valid <> io.inst.valid
  slicer.io.inst.ready <> io.inst.ready
  slicer.io.slicedInst <> io.slicedInst
  slicer.io.matAfterSlicers <> io.matAfterSlicers
  io.instFinish <> slicer.io.instFinish
}

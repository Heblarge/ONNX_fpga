package runtime.dispatch.system

import DMA._
import Accelerator._
import Interface._

// =============================================================================
// HwDrivers — 硬件驱动层
//
// 封装 DMA 控制、Cache 生命周期管理、指令发送等硬件操作，
// 通过 SimContext 的线程安全接口与仿真交互。
//
// 三个驱动类：
//   - DmaDriver:         DMA 传输控制（配置、启动、等待、清除中断）
//   - CacheDriver:       Cache 生命周期配置和全局中断管理
//   - InstructionDriver: 计算指令编码和发送
// =============================================================================

// ========================= DMA 驱动 =========================

object DmaChannel extends Enumeration {
  type DmaChannel = Value
  val A, B, Z = Value

  def toPort(ch: DmaChannel): String = ch match {
    case A => "A"
    case B => "B"
    case Z => "Z"
  }
}

case class DmaTransferParams(
  matBase: Long,      // DDR 矩阵基地址
  localBase: Long,    // Cache 本地基地址（通常为 0）
  matCols: Int,       // 矩阵总列数（以 beat 为单位）
  matRows: Int,       // 矩阵总行数
  tileCols: Int,      // Tile 列数（以 beat 为单位）
  tileRows: Int,      // Tile 行数
  tileColPos: Int,    // Tile 列偏移（以 beat 为单位）
  tileRowPos: Int,    // Tile 行偏移
  direction: Int      // 0 = DDR→Cache (读), 1 = Cache→DDR (写)
)

class DmaDriver(simCtx: SimContext) {

  /** 启动 DMA 传输 */
  def startTransfer(channel: DmaChannel.DmaChannel, params: DmaTransferParams): Unit = {
    simCtx.submitChecked(StartDmaCmd(
      port = DmaChannel.toPort(channel),
      matBase = params.matBase,
      localBase = params.localBase,
      matCols = params.matCols,
      matRows = params.matRows,
      tileCols = params.tileCols,
      tileRows = params.tileRows,
      tileColPos = params.tileColPos,
      tileRowPos = params.tileRowPos,
      direction = params.direction
    ))
  }

  /** 等待 DMA 完成中断 */
  def waitComplete(channel: DmaChannel.DmaChannel, maxCycles: Int = 50000000): Unit = {
    simCtx.submitChecked(WaitDmaIntrCmd(
      port = DmaChannel.toPort(channel),
      maxCycles = maxCycles
    ))
  }

  /** 清除 DMA 中断 */
  def clearInterrupt(channel: DmaChannel.DmaChannel): Unit = {
    simCtx.submitChecked(ClearDmaIntrCmd(DmaChannel.toPort(channel)))
  }

  /** 执行完整 DMA 传输：启动 + 等待 + 清中断 */
  def transfer(channel: DmaChannel.DmaChannel, params: DmaTransferParams,
               maxCycles: Int = 50000000): Unit = {
    startTransfer(channel, params)
    waitComplete(channel, maxCycles)
    clearInterrupt(channel)
  }

  /** 构建 DDR→Cache 的 DMA 参数（矩阵加载） */
  def makeLoadParams(
    ddrBase: Long,
    matTotalCols: Int,   // 矩阵总列数（元素数）
    matTotalRows: Int,   // 矩阵总行数
    tileCols: Int,       // Tile 列数（元素数）
    tileRows: Int,       // Tile 行数
    tileColPos: Int,     // Tile 列偏移（元素数）
    tileRowPos: Int      // Tile 行偏移
  ): DmaTransferParams = {
    val sideNum = simCtx.sideNum
    DmaTransferParams(
      matBase = ddrBase,
      localBase = 0,
      matCols = matTotalCols / sideNum,
      matRows = matTotalRows,
      tileCols = tileCols / sideNum,
      tileRows = tileRows,
      tileColPos = tileColPos / sideNum,
      tileRowPos = tileRowPos,
      direction = 0
    )
  }

  /** 构建 Cache→DDR 的 DMA 参数（结果写回） */
  def makeStoreParams(
    ddrBase: Long,
    matTotalCols: Int,
    matTotalRows: Int,
    tileCols: Int,
    tileRows: Int,
    tileColPos: Int,
    tileRowPos: Int
  ): DmaTransferParams = {
    val sideNum = simCtx.sideNum
    DmaTransferParams(
      matBase = ddrBase,
      localBase = 0,
      matCols = matTotalCols / sideNum,
      matRows = matTotalRows,
      tileCols = tileCols / sideNum,
      tileRows = tileRows,
      tileColPos = tileColPos / sideNum,
      tileRowPos = tileRowPos,
      direction = 1
    )
  }
}

// ========================= Cache 驱动 =========================

class CacheDriver(simCtx: SimContext) {

  /** 设置 Cache A 的生命周期计数 */
  def setLifeCfgA(value: Int): Unit = {
    simCtx.submitChecked(SetCacheLifeCfgCmd("A", value))
  }

  /** 设置 Cache B 的生命周期计数 */
  def setLifeCfgB(value: Int): Unit = {
    simCtx.submitChecked(SetCacheLifeCfgCmd("B", value))
  }

  /** 清除全局计算完成中断 */
  def clearGlobalIntr(): Unit = {
    simCtx.submitChecked(ClearCacheGlobalIntrCmd)
  }
}

// ========================= 指令驱动 =========================

class InstructionDriver(simCtx: SimContext) {

  private var _uidCounter = 0

  /** 分配唯一 UID */
  def nextUID(): Int = {
    val uid = _uidCounter
    _uidCounter += 1
    uid
  }

  /** 重置 UID 计数器 */
  def resetUID(): Unit = { _uidCounter = 0 }

  /** 发送计算指令 */
  def sendInstruction(inst: InstSim): Unit = {
    simCtx.submitChecked(SendInstructionCmd(inst))
  }

  /** 等待计算完成中断 */
  def waitComputeDone(maxCycles: Int = 50000000): Unit = {
    simCtx.submitChecked(WaitGlobalIntrCmd(maxCycles))
  }

  /** 创建并发送 MatMul 指令 */
  def sendMatMul(
    m: Int, k: Int, n: Int,
    shiftAfterOp: Int = 0, shiftAfterAct: Int = 0,
    activation: Activation_TypeDef.E = Activation_TypeDef.None,
    doTranspose: Boolean = false,
    shiftA: Int = 0, shiftB: Int = 0
  ): InstSim = {
    val uid = nextUID()
    val inst = new InstSim(
      UID = uid,
      matrixOperation = MatrixOperation_TypeDef.MatMul,
      shiftLeft_AfterMatrixOperation = shiftAfterOp,
      doTranspose = doTranspose,
      activationFunction = activation,
      shiftLeft_AfterActivation = shiftAfterAct,
      input0Address = 0, input1Address = 0, outputAddress = 0,
      input0Shape0 = m, input0Shape1 = k, input1Shape1 = n,
      shiftLeft_A = shiftA, shiftLeft_B = shiftB
    )
    inst.computeShape()
    sendInstruction(inst)
    inst
  }

  /** 创建并发送逐元素运算指令 */
  def sendElementOp(
    opType: MatrixOperation_TypeDef.E,
    m: Int, k: Int, n: Int,
    shiftAfterOp: Int = 0, shiftAfterAct: Int = 0,
    activation: Activation_TypeDef.E = Activation_TypeDef.None,
    shiftA: Int = 0, shiftB: Int = 0
  ): InstSim = {
    val uid = nextUID()
    val inst = new InstSim(
      UID = uid,
      matrixOperation = opType,
      shiftLeft_AfterMatrixOperation = shiftAfterOp,
      doTranspose = false,
      activationFunction = activation,
      shiftLeft_AfterActivation = shiftAfterAct,
      input0Address = 0, input1Address = 0, outputAddress = 0,
      input0Shape0 = m, input0Shape1 = k, input1Shape1 = n,
      shiftLeft_A = shiftA, shiftLeft_B = shiftB
    )
    inst.computeShape()
    sendInstruction(inst)
    inst
  }
}

// ========================= DDR 数据驱动 =========================

class DdrDataDriver(simCtx: SimContext) {

  /** 将 Int 矩阵写入 DDR */
  def writeMatrix(ddrBase: Long, matrix: Array[Array[Int]], rows: Int, cols: Int): Unit = {
    simCtx.submitChecked(WriteMatrixToDdr(ddrBase, matrix, rows, cols))
  }

  /** 从 DDR 读取 Int 矩阵 */
  def readMatrix(ddrBase: Long, rows: Int, cols: Int): Array[Array[Int]] = {
    simCtx.submitChecked(ReadMatrixFromDdr(ddrBase, rows, cols)) match {
      case MatrixResult(m) => m
      case other => throw new RuntimeException(s"Unexpected result: $other")
    }
  }

  /** 将 Long 矩阵写入 DDR（截断到 Int） */
  def writeLongMatrix(ddrBase: Long, matrix: Array[Array[Long]], rows: Int, cols: Int): Unit = {
    val intMat = matrix.map(_.map(_.toInt))
    writeMatrix(ddrBase, intMat, rows, cols)
  }

  /** 从 DDR 读取并扩展为 Long 矩阵 */
  def readLongMatrix(ddrBase: Long, rows: Int, cols: Int): Array[Array[Long]] = {
    readMatrix(ddrBase, rows, cols).map(_.map(_.toLong))
  }

  /** 计算矩阵所需的 DDR 字节数 */
  def matrixSizeBytes(rows: Int, cols: Int): Long = {
    val beatsPerRow = cols / simCtx.sideNum
    rows.toLong * beatsPerRow * simCtx.bytesPerBeat
  }
}

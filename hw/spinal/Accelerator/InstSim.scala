package Accelerator

import Interface._
import Util._

import spinal.core._
import spinal.core.sim._

import scala.util.Random
import scala.collection.mutable.Seq

object InstSim {
  var lastUID = 0
  var lastInput0Address = 0
  var lastInput1Address = 0
  var lastOutputAddress = 0
  var memElementBytes = 4  // 默认 32/8=4，由外部设置

  def apply(random: Random, matSubRowNum: Int) = new InstSim(random, matSubRowNum)

//  def memSetInstSims[T <: Data](
//      mem: Mem[T],
//      instSims: Seq[InstSim],
//      isinput0: Boolean,
//      mats: Seq[Array[Array[Int]]],
//      memColNum: Int,
//      elementWidth: Int
//  ) = instSims.zip(mats).foreach { case (instSim, mat) =>
//    memSetMat(mem, if (isinput0) instSim.input0Address else instSim.input1Address, mat, memColNum, elementWidth)
//  }
def memSetInstSims(
                    mem: Mem[Bits],
                    instSims: Seq[InstSim],
                    isA: Boolean,
                    mats: Seq[Array[Array[Int]]],
                    sideNum: Int,
                    elementWidth: Int,
                    memElementWidth: Int = 32
                  ): Unit = {
  val memElementBytes = memElementWidth / 8
  val elemMask = (BigInt(1) << memElementWidth) - 1
  val bytesPerVector = sideNum * memElementBytes

  for (m <- instSims.indices) {
    val inst = instSims(m)
    val mat = mats(m)
    val baseAddr = if (isA) inst.input0Address else inst.input1Address

    // 获取矩阵的宽度 (列数)
    // 注意：matB 的列数对应 shape1，matA 的列数也对应 shape1 (在 Slicer 逻辑中 Input0Shape1 是宽度)
    // Slicer 逻辑：GlobalStride = Shape1 / SideNum

    // 数据按 memElementWidth 对齐紧密堆放在内存中，
    // 内存总线位宽是 sideNum * memElementBytes Bytes，用一个 bigint 模拟
    // mem 的索引是字索引（与 Slicer 硬件地址一致）
    val shape1 = if(isA) inst.input0Shape1 else inst.input1Shape1
    val numBlocksX = shape1 / sideNum

    for (row <- mat.indices) {
      // 遍历这一行的每一个 Block
      for (blkCol <- 0 until numBlocksX) {
        // 1. 计算 Slicer 预期的地址索引（字索引，与 Slicer 硬件地址一致）
        // Logic: (GlobalRow * NumBlocksPerRow) + BlockCol
        val addrIndex = row * numBlocksX + blkCol
        val physAddr = baseAddr + addrIndex

        // 2. 提取当前 Block 的数据 (sideNum 个元素)
        var rowBits = BigInt(0)
        for (colOffset <- 0 until sideNum) {
          val globalCol = blkCol * sideNum + colOffset
          // 防止越界（虽然通常矩阵大小是 sideNum 倍数）
          val elementVal = if (globalCol < mat(row).length) mat(row)(globalCol) else 0
          val element = BigInt(elementVal)

          // 3. 数据打包 (memElementWidth 对齐)
          rowBits |= (element & elemMask) << (colOffset * memElementWidth)
        }

        // 4. 写入内存
        mem.setBigInt(physAddr.toLong, rowBits)
      }
    }
  }
}

}

class InstSim(
    val UID: Int,
    var matrixOperation: MatrixOperation_TypeDef.E,
    var shiftLeft_AfterMatrixOperation: Int,
    val doTranspose: Boolean,
    var activationFunction: Activation_TypeDef.E,
    var shiftLeft_AfterActivation: Int,
    val input0Address: Int,
    val input1Address: Int,
    val outputAddress: Int,
    val input0Shape0: Int,
    var input0Shape1: Int,
    val input1Shape1: Int,
    val shiftLeft_A: Int,
    val shiftLeft_B: Int
) {
  var input1Shape0, outputShape0, outputShape1 = 0
  computeShape()

  def this(random: Random, matSubRowNum: Int) = {
    this(
      InstSim.lastUID + random.nextInt(10),
      random.nextSpinalEnum(MatrixOperation_TypeDef),
      random.between(-2, 3),
      random.nextBoolean(),
      random.nextSpinalEnum(Activation_TypeDef),
      random.between(-2, 3),
      InstSim.lastInput0Address + random.nextInt(10),
      InstSim.lastInput1Address + random.nextInt(10),
      InstSim.lastOutputAddress + random.nextInt(10),

      random.between(1, 10) * matSubRowNum,
      random.between(1, 10) * matSubRowNum,
      random.between(1, 10) * matSubRowNum,
      random.between(-2, 3),
      random.between(-2, 3)
    )
    // this(
    //   InstSim.lastUID,
    //   MatrixOperation_TypeDef.MatMul,
    //   0,
    //   false,
    //   Activation_TypeDef.None,
    //   0,
    //   InstSim.lastInput0Address,
    //   InstSim.lastInput1Address,
    //   InstSim.lastOutputAddress,
    //   4 * matSubRowNum,
    //   4 * matSubRowNum,
    //   4 * matSubRowNum,
    //   0,
    //   0
    // )

    if (activationFunction == Activation_TypeDef.Log) {
      // activationFunction = Activation_TypeDef.Softplus
      matrixOperation = MatrixOperation_TypeDef.ElementAdd
      shiftLeft_AfterMatrixOperation = 0
      computeShape()
    }
    InstSim.lastUID = UID + 1
    InstSim.lastInput0Address = input0Address + input0Shape0 * input0Shape1 / matSubRowNum
    InstSim.lastInput1Address = input1Address + input1Shape0 * input1Shape1 / matSubRowNum
    // 指令中的地址是字索引，按字索引步进
    InstSim.lastOutputAddress = outputAddress + outputShape0 * outputShape1 / matSubRowNum
  }

  def this(instJava: InstJavaTODO) = {
    this(
      instJava.UID,
      instJava.matrixOperation.toLowerCase match {
        case "matmul"     => MatrixOperation_TypeDef.MatMul
        case "elementadd" => MatrixOperation_TypeDef.ElementAdd
        case "elementmul" => MatrixOperation_TypeDef.ElementMul
        case "elementmax" => MatrixOperation_TypeDef.ElementMax
        case other        => throw new IllegalArgumentException(s"wrong matrix operation: $other")
      },
      instJava.shiftLeft_AfterMatrixOperation,
      instJava.doTranspose,
      instJava.activationFunction.toLowerCase match {
        case "exp"      => Activation_TypeDef.Exp
        case "log"      => Activation_TypeDef.Log
        case "relu"     => Activation_TypeDef.Relu
        case "softplus" => Activation_TypeDef.Softplus
        case "none"     => Activation_TypeDef.None
        case other      => throw new IllegalArgumentException(s"wrong activation function: $other")
      },
      instJava.shiftLeft_AfterActivation,
      instJava.input0Address,
      instJava.input1Address,
      instJava.outputAddress,
      instJava.input0Shape0,
      instJava.input0Shape1,
      instJava.input1Shape1,
      instJava.shiftLeft_A,
      instJava.shiftLeft_B
    )
  }

  def computeShape() = {
    input1Shape0 = if (matrixOperation == MatrixOperation_TypeDef.MatMul) {
      input0Shape1
    } else {
      input0Shape1 = input1Shape1
      input0Shape0
    }
    if (!doTranspose) {
      outputShape0 = input0Shape0
      outputShape1 = input1Shape1
    } else {
      outputShape0 = input1Shape1
      outputShape1 = input0Shape0
    }
  }

  override def toString =
    s"""UID: ${UID}
       |matrixOperation: ${matrixOperation}
       |shiftLeft_AfterMatrixOperation: ${shiftLeft_AfterMatrixOperation}
       |doTranspose: ${doTranspose}
       |activationFunction: ${activationFunction}
       |shiftLeft_AfterActivation: ${shiftLeft_AfterActivation}
       |input0Address: ${input0Address}
       |input0Shape: (${input0Shape0}, ${input0Shape1})
       |input1Address: ${input1Address}
       |input1Shape: (${input1Shape0}, ${input1Shape1})
       |outputAddress: ${outputAddress}
       |outputShape: (${outputShape0}, ${outputShape1})
       |shiftLeft_A: ${shiftLeft_A}
       |shiftLeft_B: ${shiftLeft_B}""".stripMargin

  def driveSim(payload: ComputeInstruction_TypeDef) = {
    payload.UID #= UID
    payload.matrixOperation #= matrixOperation
    payload.shiftLeft_AfterMatrixOperation #= shiftLeft_AfterMatrixOperation
    payload.doTranspose #= doTranspose
    payload.activationFunction #= activationFunction
    payload.shiftLeft_AfterActivation #= shiftLeft_AfterActivation
    payload.input0Address #= input0Address
    payload.input0Shape(0) #= input0Shape0
    payload.input0Shape(1) #= input0Shape1
    payload.input1Address #= input1Address
    payload.input1Shape(0) #= input1Shape0
    payload.input1Shape(1) #= input1Shape1
    payload.outputAddress #= outputAddress
    payload.shiftLeft_A #= shiftLeft_A
    payload.shiftLeft_B #= shiftLeft_B
  }

  def driveSim(payload: Sliced_ComputeInstruction_TypeDef) = {
    payload.UID #= UID
    payload.doTranspose #= doTranspose
    payload.outputAddress #= outputAddress
    payload.outputShape(0) #= outputShape0
    payload.outputShape(1) #= outputShape1
  }

  def transposeSim(mat: Array[Array[BigInt]]) = if (doTranspose) mat.transpose else mat
  def transposeSim(mat: Array[Array[Int]]) = if (doTranspose) mat.transpose else mat

  def systolicArraySim(matA: Array[Array[BigInt]], matB: Array[Array[BigInt]], elementWidth: Int) = {
    // Simulate Slicer: hardware negates shiftLeft_A/B before SIntShifter,
    // so positive field = left shift. matElementwiseShift positive = right shift,
    // hence negate the field value.
    val slicedA = if (shiftLeft_A != 0) matElementwiseShift(matA, -shiftLeft_A, elementWidth) else matA
    val slicedB = if (shiftLeft_B != 0) matElementwiseShift(matB, -shiftLeft_B, elementWidth) else matB
    val matZ = matrixOperation match {
      case MatrixOperation_TypeDef.MatMul     => matMul(slicedA, slicedB)
      case MatrixOperation_TypeDef.ElementAdd => matElementwiseAdd(slicedA, slicedB)
      case MatrixOperation_TypeDef.ElementMul => matElementwiseMul(slicedA, slicedB)
      case MatrixOperation_TypeDef.ElementMax => matElementwiseMax(slicedA, slicedB)
    }
    // Saturate to elementWidth bits, matching hardware SIntShifter(outWidth=elementWidth)
    transposeSim(matElementwiseShift(matZ, shiftLeft_AfterMatrixOperation, elementWidth))
  }

  def activationSim(matZ: Array[Array[BigInt]], cfg: AcceleratorCfg) = {
    val matZ2 = activationFunction match {
      case Activation_TypeDef.Exp      => matElementwiseExp(matZ, cfg)
      case Activation_TypeDef.Log      => matElementwiseLog(matZ, cfg)
      case Activation_TypeDef.Relu     => matElementwiseRelu(matZ, cfg)
      case Activation_TypeDef.Softplus => matElementwiseSoftplus(matZ, cfg)
      case Activation_TypeDef.None     => matZ
    }
    // Saturate to elementWidth bits, matching hardware SIntShifter(outWidth=elementWidth)
    matElementwiseShift(matZ2, shiftLeft_AfterActivation, cfg.elementWidth)
  }

  def acceleratorSim(matA: Array[Array[Int]], matB: Array[Array[Int]], cfg: AcceleratorCfg) = {
    val matZ = systolicArraySim(matA.map(_.map(BigInt(_))), matB.map(_.map(BigInt(_))), cfg.elementWidth)
    activationSim(matZ, cfg).map(_.map(_.toInt))
  }

  def acceleratorSim(matA: Array[Array[BigInt]], matB: Array[Array[BigInt]], cfg: AcceleratorCfg) = {
    val matZ = systolicArraySim(matA, matB, cfg.elementWidth)
    activationSim(matZ, cfg)
  }
}

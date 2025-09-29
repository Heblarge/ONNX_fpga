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

  def apply(random: Random, matSubRowNum: Int) = new InstSim(random, matSubRowNum)

  def memSetInstSims[T <: Data](
      mem: Mem[T],
      instSims: Seq[InstSim],
      isinput0: Boolean,
      mats: Seq[Array[Array[Int]]],
      memColNum: Int,
      elementWidth: Int
  ) = instSims.zip(mats).foreach { case (instSim, mat) =>
    memSetMat(mem, if (isinput0) instSim.input0Address else instSim.input1Address, mat, memColNum, elementWidth)
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
    val input1Shape1: Int
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
      random.between(1, 10) * matSubRowNum
    )
    // this(
    //   InstSim.lastUID,
    //   MatrixOperation_TypeDef.ElementAdd,
    //   0,
    //   false,
    //   Activation_TypeDef.None,
    //   0,
    //   InstSim.lastInput0Address,
    //   InstSim.lastInput1Address,
    //   InstSim.lastOutputAddress,
    //   1 * matSubRowNum,
    //   1 * matSubRowNum,
    //   1 * matSubRowNum
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
      instJava.input1Shape1
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
       |outputShape: (${outputShape0}, ${outputShape1})""".stripMargin

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
    payload.outputShape(0) #= outputShape0
    payload.outputShape(1) #= outputShape1
  }

  def driveSim(payload: Sliced_ComputeInstruction_TypeDef) = {
    payload.UID #= UID
    payload.doTranspose #= doTranspose
    payload.outputAddress #= outputAddress
    payload.outputShape(0) #= outputShape0
    payload.outputShape(1) #= outputShape1
  }

  def transposeSim(mat: Array[Array[Int]]) = if (doTranspose) mat.transpose else mat

  def systolicArraySim(matA: Array[Array[Int]], matB: Array[Array[Int]], elementWidth: Int) = {
    if (matrixOperation == MatrixOperation_TypeDef.MatMul) {
      transposeSim(matElementwiseShift(matMul(matA, matB), shiftLeft_AfterMatrixOperation, elementWidth))
    } else {
      val matZ = matrixOperation match {
        case MatrixOperation_TypeDef.ElementAdd => matElementwiseAdd(matA, matB)
        case MatrixOperation_TypeDef.ElementMul => matElementwiseMul(matA, matB)
        case MatrixOperation_TypeDef.ElementMax => matElementwiseMax(matA, matB)
      }
      transposeSim(matElementwiseShift(matZ, shiftLeft_AfterMatrixOperation, elementWidth))
    }
  }

  def activationSim(matZ: Array[Array[Int]], cfg:AcceleratorCfg) = {
    val matZ2 = activationFunction match {
      case Activation_TypeDef.Exp      => matElementwiseExp(matZ, cfg)
      case Activation_TypeDef.Log      => matElementwiseLog(matZ, cfg)
      case Activation_TypeDef.Relu     => matElementwiseRelu(matZ, cfg)
      case Activation_TypeDef.Softplus => matElementwiseSoftplus(matZ, cfg)
      case Activation_TypeDef.None     => matZ
    }
    matElementwiseShift(matZ2, shiftLeft_AfterActivation, cfg.elementWidth)
  }

  def acceleratorSim(matA: Array[Array[Int]], matB: Array[Array[Int]], cfg:AcceleratorCfg) = {
    val matZ = systolicArraySim(matA, matB, cfg.elementWidth)
    activationSim(matZ, cfg)
  }
}

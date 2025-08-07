import spinal.core._
import spinal.core.sim._

import scala.math._
import scala.util.Random

package object Util {
  def printlnMat(mat: Array[Array[Int]]) = println(matToString(mat))

  def matToString(mat: Array[Array[Int]]) = mat.map(vecToString(_)).mkString("\n")

  def vecToString(vec: Array[Int]) = vec.mkString("\t")

  def printlnMatWithHex(mat: Array[Array[Int]]) = println(matToStringWithHex(mat))

  def matToStringWithHex(mat: Array[Array[Int]]) = mat.map(vecToStringWithHex(_)).mkString("\n")

  def vecToStringWithHex(vec: Array[Int]) = vec.map(x => s"$x(${x.toHexString})").mkString("\t")

  def matElementwiseOp(matA: Array[Array[Int]], matB: Array[Array[Int]], op: (Int, Int) => Int) = {
    require(matA.length == matB.length && matA(0).length == matB(0).length, "mat shape mismatch".red)
    matA.zip(matB).map { case (matARow, matBRow) =>
      matARow.zip(matBRow).map { case (a, b) =>
        op(a, b)
      }
    }
  }

  def matElementwiseAdd(matA: Array[Array[Int]], matB: Array[Array[Int]]) = matElementwiseOp(matA, matB, _ + _)

  def matElementwiseMul(matA: Array[Array[Int]], matB: Array[Array[Int]]) = matElementwiseOp(matA, matB, _ * _)

  def matElementwiseMax(matA: Array[Array[Int]], matB: Array[Array[Int]]) = matElementwiseOp(matA, matB, _ max _)

  def matElementwiseOp(mat: Array[Array[Int]], op: Int => Int) = mat.map(_.map(op))

  def matElementwiseShift(mat: Array[Array[Int]], shiftAmount: Int, satBits: Int) = matElementwiseOp(
    mat,
    x => {
      require(satBits > 0 && satBits <= 32, "satBits unsupported".red)
      val y =
        if (shiftAmount > 0) x >> shiftAmount
        else x << -shiftAmount
      val min = -(1 << (satBits - 1))
      val max = (1 << (satBits - 1)) - 1
      y match {
        case y if y > max => max
        case y if y < min => min
        case _            => y
      }
    }
  )

  def fixOp(fracWidth: Int, op: Double => Double) = (x: Int) =>
    (op(x.toDouble / pow(2, fracWidth)) * pow(2, fracWidth)).toInt

  def matElementwiseExp(mat: Array[Array[Int]], fracWidth: Int) = matElementwiseOp(mat, fixOp(fracWidth, exp))

  def matElementwiseLog(mat: Array[Array[Int]], fracWidth: Int) = matElementwiseOp(mat, fixOp(fracWidth, log))

  def matElementwiseRelu(mat: Array[Array[Int]], fracWidth: Int) =
    matElementwiseOp(mat, fixOp(fracWidth, x => if (x > 0) x else 0))

  def matElementwiseSoftplus(mat: Array[Array[Int]], fracWidth: Int) =
    matElementwiseOp(mat, fixOp(fracWidth, x => log(1 + exp(x))))

  def matMul(matA: Array[Array[Int]], matB: Array[Array[Int]]) = {
    require(matA(0).length == matB.length, "mat shape mismatch".red)
    matA.map(matARow => matB.transpose.map(matBCol => matARow.zip(matBCol).map { case (a, b) => a * b }.sum))
  }

  def matGetSub(mat: Array[Array[Int]], rowStart: Int, colStart: Int, rowNum: Int, colNum: Int) =
    Array.tabulate(rowNum, colNum)((i, j) => mat(rowStart + i)(colStart + j))

  def matRotateCw(mat: Array[Array[Int]]) =
    Array.tabulate(mat(0).length, mat.length)((i, j) => mat(mat.length - 1 - j)(i))

  def memGetMat[T <: Data](mem: Mem[T], addr: Int, memColNum: Int, elementWidth: Int, rowNum: Int, colNum: Int) =
    memToMat(
      Array.tabulate(rowNum * colNum / memColNum)(i => mem.getBigInt(addr + i)),
      memColNum,
      elementWidth,
      rowNum,
      colNum
    )

  def memToMat(mem: Array[BigInt], memColNum: Int, elementWidth: Int, rowNum: Int, colNum: Int) = {
    val mat = Array.tabulate(rowNum * colNum)(i =>
      mem(i / memColNum).extractBitsWidth(i % memColNum * elementWidth, elementWidth).signExtend(elementWidth).toInt
    )
    Array.tabulate(rowNum, colNum)((i, j) => mat(i * colNum + j))
  }

  def memSetMat[T <: Data](mem: Mem[T], addr: Int, mat: Array[Array[Int]], memColNum: Int, elementWidth: Int) =
    matToMem(mat, memColNum, elementWidth).zipWithIndex.foreach { case (matInMemRow, i) =>
      mem.setBigInt(addr + i, matInMemRow)
    }

  def matToMem(mat: Array[Array[Int]], memColNum: Int, elementWidth: Int) = {
    val mem = Array.fill(mat.length * mat(0).length / memColNum)(BigInt(0))
    mat.flatten.zipWithIndex.foreach { case (x, i) =>
      mem(i / memColNum) += BigInt(x).extractBitsWidth(0, elementWidth) << i % memColNum * elementWidth
    }
    mem
  }

  def vecZipForeach[A, B](vecA: Array[A], vecB: Array[B])(f: (A, B, Int) => Unit) =
    vecA.zip(vecB).zipWithIndex.foreach { case ((a, b), i) =>
      f(a, b, i)
    }

  def matZipForeach[A, B](matA: Array[Array[A]], matB: Array[Array[B]])(f: (A, B, Int, Int) => Unit) =
    vecZipForeach(matA, matB)((matARow, matBRow, i) => vecZipForeach(matARow, matBRow)((a, b, j) => f(a, b, i, j)))

  implicit class BigIntExtend(val x: BigInt) {
    def extractBits(msb: Int, lsb: Int) = (x >> lsb) & ((BigInt(1) << (msb - lsb + 1)) - 1)

    def extractBitsWidth(lsb: Int, width: Int) = x.extractBits(lsb + width - 1, lsb)

    def signExtend(width: Int) =
      if (x.testBit(width - 1)) x | ~((BigInt(1) << width) - 1)
      else x
  }

  implicit class StringExtend(val str: String) {
    def green = s"\u001B[0;32m$str\u001B[0m"

    def red = s"\u001B[0;31m$str\u001B[0m"

    def blue = s"\u001B[0;34m$str\u001B[0m"
  }

  implicit class ClockDomainExtend(val cd: ClockDomain) {
    def forkStimulusRandomClk(random: Random, period: Int, rangeRate: Int = 2, resetCycles: Int = 16) = {
      cd.config.clockEdge match {
        case RISING  => cd.fallingEdge()
        case FALLING => cd.risingEdge()
      }
      if (cd.hasResetSignalSim) cd.deassertReset()
      if (cd.hasSoftResetSignalSim) cd.deassertSoftReset()
      if (cd.hasClockEnableSignalSim) cd.deassertClockEnable()
      fork {
        assert(period >= 2)
        if (cd.hasClockEnableSignalSim) cd.assertClockEnable()
        if (cd.hasSoftResetSignalSim) cd.deassertSoftReset()
        cd.config.clockEdge match {
          case RISING  => cd.fallingEdge()
          case FALLING => cd.risingEdge()
        }
        if (cd.config.resetKind == ASYNC) {
          val dummy = if (cd.hasResetSignalSim) {
            cd.resetSim #= (cd.config.resetActiveLevel match {
              case HIGH => false
              case LOW  => true
            })
            sleep(0)
            DoReset(cd.resetSim, period * resetCycles, cd.config.resetActiveLevel)
          }
          sleep(period)
          while (true) {
            cd.clockToggle()
            sleep(random.between(period / 2 / rangeRate, period / 2 * rangeRate))
          }
        } else { TODO() }
      }
    }
  }
  implicit class ClockDomainHandleExtend(cd: spinal.core.fiber.Handle[ClockDomain]) extends ClockDomainExtend(cd.get)

  implicit class RandomExtend(val random: Random) {
    def nextSpinalEnum(spinalEnum: SpinalEnum) = spinalEnum.elements(random.nextInt(spinalEnum.elements.length))

    def validNumWhen(validNum: Int, cond: Boolean, min: Int = 0, max: Int = 10) =
      if (cond) validNum else random.between(min, max)

    def nextMat(rowNum: Int, colNum: Int, min: Int = 0, max: Int = 10) =
      Array.fill(rowNum, colNum)(random.between(min, max))
    // Array.tabulate(rowNum, colNum)((i, j) => i * colNum + j)
  }

  def TODO() = assert(false, "This code is not finished yet".red)

  def debugCheck() = println("If see this message, the bug may have been fixed, please let me know".green)

  implicit class VecExtend[T <: Data](val vec: Vec[T]) {
    def mapVec[B <: Data](f: T => B) = Vec(vec.map(f))
  }

  implicit class VecSIntExtend(val vec: Vec[SInt]) {
    def #=(arr: Array[Int]) = vec.zip(arr).foreach(p => p._1 #= p._2)
    def toArrayInt = vec.map(_.toInt).toArray
  }
}

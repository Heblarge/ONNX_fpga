package GeMM.SystolicArray2D
import spinal.core._
import spinal.lib._

/**
 * SIntShifter模块:
 *
 *   本模块实现一个支持SInt类型的带饱和处理的移位器，其中移位量为正表示右移，负表示左移。
 *
 * @param inWidth  输入信号的位宽
 * @param outWidth 输出信号的位宽，同时限定了最大移位量
 */

case class SIntShifter(inWidth: Int, outWidth: Int) extends Component {
  val io = new Bundle {
    val input = in SInt(inWidth bits)
    val shiftAmount = in SInt(log2Up(outWidth + 1) + 1 bits)
    val output = out SInt(outWidth bits)
  }

  val maxShift = outWidth

  val effShift = Mux(io.shiftAmount > S(maxShift),
    S(maxShift),
    Mux(io.shiftAmount < S(-maxShift), S(-maxShift), io.shiftAmount))


  val extWidth = inWidth + outWidth
  val extInput = io.input.resize(extWidth)

  val rawResult = Mux(effShift > 0, extInput >> effShift.asUInt, extInput << (-effShift).asUInt)

  // 定义目标位宽对应的饱和边界：
  val maxVal = (BigInt(1) << (outWidth - 1)) - 1
  val minVal = -(BigInt(1) << (outWidth - 1))
  // 将边界扩展到 extWidth 位以便比较
  val maxS = S(maxVal, extWidth bits)
  val minS = S(minVal, extWidth bits)

  // 若计算结果超出目标表示范围，则限定为最大或最小值
  val satResult = Mux(rawResult > maxS, maxS, Mux(rawResult < minS, minS, rawResult))

  // 最终输出结果，将饱和后的结果缩减至目标位宽
  io.output := satResult.resized
}

import spinal.core._
import spinal.lib._
import scala.util.Random
import scala.collection.mutable.Queue
import java.io.File
import spinal.sim.VCSFlags
import spinal.lib.sim._
import spinal.core.sim._

object sim_SIntShifter extends App {
  // 创建仿真报告目录
  new File("rtl/SIntShifter/sim_SIntShifter_report").mkdir()

  // 定义VCS仿真相关的参数
  val flags = VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )

  val inWidth = 8
  val outWidth = 8

  // 根据SIntShifter模块生成Verilog代码，并存放于指定目录
  val report = SpinalConfig(
    targetDirectory = "rtl/SIntShifter/sim_SIntShifter_report",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(SIntShifter(inWidth, outWidth))
    .printPruned()

  // 生成仿真模块
  val module_compiled = SimConfig
    .withVCS(flags)
    .withTimePrecision(1 ps)
    .withFSDBWave
    .withConfig(SpinalConfig(bitVectorWidthMax = 20000))
    .compile(report)

  // 生成随机输入数据
  def generateRandomInput(inWidth: Int): BigInt = {
    val rand = new Random()
    // 此处生成范围在[0, 2^(inWidth-1)-1]之间的随机正数
    BigInt(rand.nextInt(1 << (inWidth - 1)))
  }

  // 生成随机移位量，范围为 [-outWidth, outWidth]
  def generateRandomShift(maxShift: Int): BigInt = {
    val rand = new Random()
    // 生成0到(2*maxShift)之间的随机数，再减去maxShift，实现移位量的正负分布
    BigInt(rand.nextInt(2 * maxShift + 1) - maxShift)
  }

  // 构造输入及移位量的队列
  val inputQueue = Queue[BigInt]()
  val shiftQueue = Queue[BigInt]()

  // 填充100个测试用例
  for (_ <- 0 until 100) {
    inputQueue.enqueue(generateRandomInput(inWidth))
    shiftQueue.enqueue(generateRandomShift(outWidth))
  }

  // 仿真过程
  module_compiled.doSim("SIntShifter_TB", seed = 1233) { dut =>
    // 产生时钟周期，周期为10个时间单位
    //dut.clockDomain.forkStimulus(10)
    SimTimeout(6000000)

    // 初始赋值
    dut.io.input #= 0
    dut.io.shiftAmount #= 0
    sleep(10)

    // 依次读取队列中的测试数据，驱动输入信号和移位量，并等待足够时钟周期以捕捉响应
    for (_ <- 0 until 100) {
      val inVal = inputQueue.dequeue()
      val shiftVal = shiftQueue.dequeue()
      dut.io.input #= inVal
      dut.io.shiftAmount #= shiftVal
      // 值得注意的是，此处引入sleep的核心意义在于确保信号稳定后进行后续采样
      sleep(10)
    }
    simSuccess()
  }
}


object MatrixMultiplySaturationTest {

  // 带移位与饱和保护的矩阵乘法函数
  def multiplyMatricesWithShiftSaturation(
                                           matrixA: Array[Array[Int]],
                                           matrixB: Array[Array[Int]],
                                           shiftAmount: Int,
                                           satBits: Int
                                         ): Array[Array[Int]] = {

    val rowsA = matrixA.length
    val colsA = matrixA(0).length
    val rowsB = matrixB.length
    val colsB = matrixB(0).length

    require(colsA == rowsB, "矩阵A的列数必须等于矩阵B的行数")
    require(satBits > 0 && satBits <= 32, "位宽必须为 1 到 32 之间的正整数")

    val minVal = -(1 << (satBits - 1))
    val maxVal =  (1 << (satBits - 1)) - 1

    val result = Array.ofDim[Int](rowsA, colsB)

    for (i <- 0 until rowsA) {
      for (j <- 0 until colsB) {
        val rawSum = (0 until colsA).map(k => matrixA(i)(k) * matrixB(k)(j)).sum
        val shifted = if (shiftAmount > 0) {
          rawSum >> shiftAmount
        } else if (shiftAmount < 0) {
          rawSum << (-shiftAmount)
        } else {
          rawSum
        }
        result(i)(j) =
          if (shifted > maxVal) maxVal
          else if (shifted < minVal) minVal
          else shifted
      }
    }

    result
  }

  def elementWiseBinaryOpWithShiftAndSaturation(
                                                 A: Array[Array[Int]],
                                                 B: Array[Array[Int]],
                                                 op: (Int, Int) => Int,    // 运算函数：乘或加
                                                 shiftAmount: Int,
                                                 satBits: Int
                                               ): Array[Array[Int]] = {
    require(A.length == B.length && A(0).length == B(0).length, "矩阵维度不一致")
    require(satBits > 0 && satBits <= 32, "位宽必须为 1 到 32 之间")

    val rows = A.length
    val cols = A(0).length
    val minVal = -(1 << (satBits - 1))
    val maxVal =  (1 << (satBits - 1)) - 1

    val result = Array.ofDim[Int](rows, cols)

    for (i <- 0 until rows; j <- 0 until cols) {
      val raw = op(A(i)(j), B(i)(j))
      val shifted = if (shiftAmount > 0) {
        raw >> shiftAmount
      } else if (shiftAmount < 0) {
        raw << (-shiftAmount)
      } else raw

      result(i)(j) =
        if (shifted > maxVal) maxVal
        else if (shifted < minVal) minVal
        else shifted
    }

    result
  }
  def elementWiseMultiplyMatrix(
                                 A: Array[Array[Int]],
                                 B: Array[Array[Int]],
                                 shiftAmount: Int,
                                 satBits: Int
                               ): Array[Array[Int]] = {
    elementWiseBinaryOpWithShiftAndSaturation(A, B, _ * _, shiftAmount, satBits)
  }
  def elementWiseAdditionMatrix(
                                 A: Array[Array[Int]],
                                 B: Array[Array[Int]],
                                 shiftAmount: Int,
                                 satBits: Int
                               ): Array[Array[Int]] = {
    elementWiseBinaryOpWithShiftAndSaturation(A, B, _ + _, shiftAmount, satBits)
  }

  // 打印矩阵
  def printMatrix(matrix: Array[Array[Int]], name: String): Unit = {
    println(s"$name:")
    matrix.foreach(row => println(row.map("%8d".format(_)).mkString(" ")))
    println()
  }

  def main(args: Array[String]): Unit = {
    // 输入矩阵
    val A = Array(
      Array(1000, 2000),
      Array(3000, 4000)
    )

    val B = Array(
      Array(5, 6),
      Array(7, 8)
    )

    val shiftAmount = -2  // 左移 1 位，相当于乘以 2
    val satBits = 8      // 20 位 SInt，可表示范围为 [-524288, 524287]

    // 打印输入
    printMatrix(A, "矩阵 A")
    printMatrix(B, "矩阵 B")
    println(s"移位量：$shiftAmount")
    println(s"饱和位宽：$satBits 位，范围为 [${-(1 << (satBits - 1))}, ${(1 << (satBits - 1)) - 1}]\n")

    // 计算结果
    val result = multiplyMatricesWithShiftSaturation(A, B, shiftAmount, satBits)

    // 打印结果
    printMatrix(result, "结果矩阵 (移位+饱和)")

    //测试按元素操作
    val ewMul = elementWiseMultiplyMatrix(A, B, shiftAmount, satBits)
    val ewAdd = elementWiseAdditionMatrix(A, B, shiftAmount, satBits)

    println("按元素乘（移位+饱和）：")
    ewMul.foreach(row => println(row.mkString(", ")))

    println("\n按元素加（移位+饱和）：")
    ewAdd.foreach(row => println(row.mkString(", ")))
  }
}


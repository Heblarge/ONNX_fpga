package FloatingPoint

import Interface.MatrixOperation_TypeDef
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{StreamDriver, StreamMonitor, StreamReadyRandomizer, ScoreboardInOrder}
import spinal.sim.VCSFlags

import scala.collection.mutable
import scala.util.Random

/**
 * 浮点数精确算法工具类
 * 参考 FpxxAddTester 和 FpxxMulTester 的实现
 * 用于处理浮点数的量化、反量化和精确运算
 */
class FloatAlgo(c: FpxxConfig) {
  val expBits = c.exp_size
  val mantBits = c.mant_size
  val bias = c.bias
  val isFNUZ = c.inf_encoding.isInstanceOf[NoInfinity]
  val hasInf = !isFNUZ

  val maxExpVal = (1 << expBits) - 1
  val maxLegalExp = if (isFNUZ) maxExpVal else maxExpVal - 1

  // 计算最大有限值
  val maxFiniteValue = {
    val mVal = (1 << mantBits) - 1
    math.pow(2, maxLegalExp - bias) * (1.0 + mVal.toDouble / (1 << mantBits).toDouble)
  }

  // 计算最小正规数
  val minNormalValue = math.pow(2, 1 - bias)

  // IEEE 溢出阈值
  val ieeeOverflowThreshold = if (hasInf) {
    val ulpAtMax = math.pow(2, maxLegalExp - bias - mantBits)
    maxFiniteValue + 0.5 * ulpAtMax
  } else Double.PositiveInfinity

  // LUT 信息结构
  case class Info(bits: Int, value: Double, isSubnormal: Boolean, isNaN: Boolean, isInf: Boolean)

  val fullSize = 1 << c.full_size
  val lut: Array[Info] = Array.tabulate(fullSize) { i =>
    val sign = (i >> (expBits + mantBits)) & 0x1
    val exp = (i >> mantBits) & ((1 << expBits) - 1)
    val mant = i & ((1 << mantBits) - 1)
    val s = if (sign == 1) -1.0 else 1.0

    if (isFNUZ) {
      // FNUZ 逻辑
      if (sign == 1 && exp == 0 && mant == 0) Info(i, Double.NaN, false, true, false)
      else if (exp == 0 && mant == 0) Info(i, 0.0, false, false, false)
      else if (exp == 0) {
        val valSub = s * math.pow(2, 1 - bias) * (mant.toDouble / (1 << mantBits).toDouble)
        Info(i, valSub, true, false, false)
      } else {
        val valNorm = s * math.pow(2, exp - bias) * (1.0 + mant.toDouble / (1 << mantBits).toDouble)
        Info(i, valNorm, false, false, false)
      }
    } else {
      // IEEE 逻辑
      if (exp == maxExpVal) {
        if (mant != 0) Info(i, Double.NaN, false, true, false)
        else Info(i, if (sign == 1) Double.NegativeInfinity else Double.PositiveInfinity, false, false, true)
      } else if (exp == 0) {
        if (mant == 0) Info(i, 0.0, false, false, false)
        else {
          val valSub = s * math.pow(2, 1 - bias) * (mant.toDouble / (1 << mantBits).toDouble)
          Info(i, valSub, true, false, false)
        }
      } else {
        val valNorm = s * math.pow(2, exp - bias) * (1.0 + mant.toDouble / (1 << mantBits).toDouble)
        Info(i, valNorm, false, false, false)
      }
    }
  }

  // Double -> Bits (带舍入到最近偶数)
  def toBits(d: Double): Int = {
    if (d.isNaN) return if (isFNUZ) 1 << (expBits + mantBits) else ((1 << expBits) - 1) << mantBits | 1

    // IEEE 溢出判断
    if (hasInf) {
      if (d.isPosInfinity || d >= ieeeOverflowThreshold) return ((1 << expBits) - 1) << mantBits
      if (d.isNegInfinity || d <= -ieeeOverflowThreshold) return 1 << (expBits + mantBits) | (((1 << expBits) - 1) << mantBits)
    }

    // 标准 Nearest Lookup
    val validCands = lut.filter(x => !x.isNaN && !x.isInf)
    val minDst = validCands.map(c => math.abs(c.value - d)).min
    val ties = validCands.filter(c => math.abs(math.abs(c.value - d) - minDst) < 1e-12)

    if (ties.size == 1) ties.head.bits
    else ties.find(c => (c.bits & 1) == 0).getOrElse(ties.head).bits
  }

  def toHost(d: Double): FpxxHost = FpxxHost(BigInt(toBits(d)), c)
  def fromBits(b: Int): Double = lut(b & (fullSize - 1)).value

  def isSubnormalOrZero(d: Double): Boolean = {
    if (d == 0.0 || d == -0.0) true
    else if (d.isNaN || d.isInfinite) false
    else lut(toBits(d)).isSubnormal
  }

  /**
   * 打印浮点矩阵
   */
  def printMatrix(matrix: Array[Array[Double]]): Unit = {
    matrix.foreach(row => println(row.map(v => f"$v%.4f").mkString("\t")))
    println()
  }
}

/**
 * 浮点矩阵记分板
 * 参考 ScoreboardInOrder_matrix 实现，但针对浮点数使用容差比较
 */
class ScoreboardInOrder_floatMatrix(tolerance: Double = 2.5) extends ScoreboardInOrder[Array[Array[Double]]] {
  override def compare(ref: Array[Array[Double]], dut: Array[Array[Double]]): Boolean = {
    ref.zip(dut).forall { case (refRow, dutRow) =>
      refRow.zip(dutRow).forall { case (r, d) =>
        // 处理 NaN 和 Inf
        if (r.isNaN && d.isNaN) true
        else if (r.isInfinite && d.isInfinite && r.signum == d.signum) true
        else math.abs(r - d) <= tolerance
      }
    }
  }

  def printMatrix(matrix: Array[Array[Double]]): Unit = {
    matrix.foreach(row => println(row.map(v => f"$v%.4f").mkString("\t")))
    println()
  }

  override def check(): Unit = {
    if (ref.nonEmpty && dut.nonEmpty) {
      val dutHead = dut.dequeue()
      val refHead = ref.dequeue()

      if (!compare(refHead, dutHead)) {
        println("Transaction mismatch :")
        println("REF :")
        printMatrix(refHead)
        println("DUT :")
        printMatrix(dutHead)
        simFailure()
      }
      matches += 1
    }
  }
}

object ScoreboardInOrder_floatMatrix {
  def apply(tolerance: Double = 2.5): ScoreboardInOrder_floatMatrix = new ScoreboardInOrder_floatMatrix(tolerance)
}

/**
 * 抽象基类 - 包含公共配置和工具函数
 * 参考 SystolicArray2D_Sim 的结构
 */
abstract class SquareSystolicArray_Sim_Abstract extends App {
  val matrix_num = 30
  val size = 4

  // 配置将在子类中提供
  val cfg: SquareSystolicArray_Config

  // FloatAlgo 实例 - 延迟初始化，等待 cfg 被子类赋值
  lazy val algo = new FloatAlgo(cfg.fpConfig)

  // 统计变量
  var AllTestCaseSent_Count = 0
  var AllTestCaseSentReported = false

  // 队列定义
  val matrixA_queue = mutable.Queue[Array[Array[Float]]]()
  val matrixB_queue = mutable.Queue[Array[Array[Float]]]()
  val transposeQueue = mutable.Queue[Boolean]()
  val refQueue = mutable.Queue[Array[Array[Double]]]()

  val random = new Random()

  /**
   * 生成随机浮点矩阵
   */
  def generateRandomMatrix(rows: Int, cols: Int, seed: Int): Array[Array[Float]] = {
    val rand = new Random()
    rand.setSeed(seed)
    Array.fill(rows, cols)((rand.nextFloat() - 0.5f) * 2.0f)
  }

  /**
   * 矩阵乘法 - 使用 Double 进行计算（参考模型）
   */
  def multiplyMatrices(matrixA: Array[Array[Double]], matrixB: Array[Array[Double]]): Array[Array[Double]] = {
    val rowsA = matrixA.length
    val colsA = matrixA(0).length
    val rowsB = matrixB.length
    val colsB = matrixB(0).length

    require(colsA == rowsB, "矩阵A的列数必须等于矩阵B的行数")

    val result = Array.ofDim[Double](rowsA, colsB)
    for (i <- 0 until rowsA) {
      for (j <- 0 until colsB) {
        result(i)(j) = (0 until colsA).map(k => matrixA(i)(k) * matrixB(k)(j)).sum
      }
    }
    result
  }

  /**
   * 转置矩阵
   */
  def transpose(matrix: Array[Array[Double]]): Array[Array[Double]] = {
    val rows = matrix.length
    val cols = matrix(0).length
    val result = Array.ofDim[Double](cols, rows)
    for (i <- 0 until rows) {
      for (j <- 0 until cols) {
        result(j)(i) = matrix(i)(j)
      }
    }
    result
  }

  /**
   * 打印浮点矩阵（使用 algo.printMatrix）
   */
  def printMatrix(matrix: Array[Array[Double]]): Unit = {
    algo.printMatrix(matrix)
  }
}

/**
 * 浮点矩阵乘法单元测试主入口
 * 参考 SystolicArray2D_Sim_matmul 的测试逻辑
 */
object SquareSystolicArray_Sim extends SquareSystolicArray_Sim_Abstract {
  /**
   * 辅助函数：将 FpxxHost 解包为 (sign, exp, mant)
   */
  private def unpackFloat(host: FpxxHost): (Boolean, Int, Int) = {
    val bits = host.value.toLong
    val sign = ((bits >> (host.c.exp_size + host.c.mant_size)) & 0x1) != 0
    val exp = ((bits >> host.c.mant_size) & ((1 << host.c.exp_size) - 1)).toInt
    val mant = (bits & ((1 << host.c.mant_size) - 1)).toInt
    (sign, exp, mant)
  }

  // 提供配置
  val cfg = SquareSystolicArray_Config(
    in_Length_Max = size,
    in_Length_Min = size,
    in_MatA_row_num = size,
    in_MatB_col_num = size,
    fpConfig = FpxxConfig.float16(),
    accIntBits = 16 bits,
    accFracBits = 16 bits,
    mulStages = 0,
    f2iStages = 0,
    Enable_Transpose_logic = true,
    Enable_ElementWise_logic = true
  )

  // 文件目录和仿真配置
  val FileDir = "rtl/SquareSystolicArray/verilog"
  import java.io.File
  new File(FileDir).mkdirs()

  val flag = VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )

  val Spinalcfg = SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
    bitVectorWidthMax = 20000
  )

  val Sim_compiled = SimConfig
    .withVCS(flag)
    .withVcdWave
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .withConfig(Spinalcfg)
    .allOptimisation
    .compile(new SquareSystolicArray(cfg))

  // 准备测试数据
  for (matrix_idx <- 0 until matrix_num) {
    var m_A = generateRandomMatrix(size, size, random.nextInt(20))
    var m_B = generateRandomMatrix(size, size, random.nextInt(20))
    var doTranspose = random.nextBoolean()

    // 生成两个随机矩阵
    matrixA_queue.enqueue(m_A)
    matrixB_queue.enqueue(m_B)
    transposeQueue.enqueue(doTranspose)

    // 使用 Float 转换为 Double 进行参考计算
    val m_A_Double = m_A.map(_.map(_.toDouble))
    val m_B_Double = m_B.map(_.map(_.toDouble))

    // 计算参考结果
    val ref = multiplyMatrices(m_A_Double, m_B_Double)

    // 根据转置标志决定是否转置
    if (doTranspose) {
      refQueue.enqueue(transpose(ref))
    } else {
      refQueue.enqueue(ref)
    }
  }

  println(s"case generated:")
  println(s"matrixA_queue size: ${matrixA_queue.size}")
  println(s"matrixB_queue size: ${matrixB_queue.size}")
  println(s"transposeQueue size: ${transposeQueue.size}")
  println(s"refQueue size: ${refQueue.size}")

  // 开始仿真
  Sim_compiled.doSim("matmul") { dut =>
    SimTimeout(190000) // 设置仿真超时时间

    val scoreboard = new ScoreboardInOrder_floatMatrix(tolerance = 2.5) // 创建自动对比参考结果的记分板
    var StreamDriver_sending_period = 0 // 记录当前数据流发送周期

    var matrixA_sending = matrixA_queue.dequeue() // 当前正在发送的矩阵A
    var matrixB_sending = matrixB_queue.dequeue() // 当前正在发送的矩阵B
    var doTranspose_sending = transposeQueue.dequeue() // 当前转置标志

    // 启动输入流驱动器，向DUT输入数据
    dut.io.in_Mats.valid #= true
    var case_sent_num = 0

    StreamDriver(dut.io.in_Mats, dut.clockDomain) { payload =>
      if (AllTestCaseSentReported) {
        false
      } else {
        // 设置工作模式
        payload.OpMode.MatrixOperation #= MatrixOperation_TypeDef.MatMul
        payload.OpMode.do_PostTranspose #= doTranspose_sending
        payload.OpMode.post_Shift #= 0

        // A矩阵输入 - 使用 FloatAlgo 量化
        for (row_index <- 0 until cfg.in_MatA_row_num) {
          if (StreamDriver_sending_period < size - 1) {
            val (s, e, m) = unpackFloat(algo.toHost(matrixA_sending(row_index)(StreamDriver_sending_period).toDouble))
            payload.A(row_index).data.sign #= s
            payload.A(row_index).data.exp #= e
            payload.A(row_index).data.mant #= m
            payload.A(row_index).Final #= false
          } else if (StreamDriver_sending_period == size - 1) {
            val (s, e, m) = unpackFloat(algo.toHost(matrixA_sending(row_index)(StreamDriver_sending_period).toDouble))
            payload.A(row_index).data.sign #= s
            payload.A(row_index).data.exp #= e
            payload.A(row_index).data.mant #= m
            payload.A(row_index).Final #= true
          } else {
            payload.A(row_index).data.sign #= false
            payload.A(row_index).data.exp #= 0
            payload.A(row_index).data.mant #= 0
            payload.A(row_index).Final #= false
          }
        }

        // B矩阵输入 - 使用 FloatAlgo 量化
        for (col_index <- 0 until cfg.in_MatB_col_num) {
          if (StreamDriver_sending_period < size - 1) {
            val (s, e, m) = unpackFloat(algo.toHost(matrixB_sending(StreamDriver_sending_period)(col_index).toDouble))
            payload.B(col_index).data.sign #= s
            payload.B(col_index).data.exp #= e
            payload.B(col_index).data.mant #= m
            payload.B(col_index).Final #= false
          } else if (StreamDriver_sending_period == size - 1) {
            val (s, e, m) = unpackFloat(algo.toHost(matrixB_sending(StreamDriver_sending_period)(col_index).toDouble))
            payload.B(col_index).data.sign #= s
            payload.B(col_index).data.exp #= e
            payload.B(col_index).data.mant #= m
            payload.B(col_index).Final #= true
          } else {
            payload.B(col_index).data.sign #= false
            payload.B(col_index).data.exp #= 0
            payload.B(col_index).data.mant #= 0
            payload.B(col_index).Final #= false
          }
        }

        // 打印当前周期信息
        println(s"StreamDriver called: ${StreamDriver_sending_period}")

        // 判断是否切换到下一组输入
        if (StreamDriver_sending_period == size - 1) {
          StreamDriver_sending_period = 0
          println(s"case${case_sent_num}")
          println("A:")
          printMatrix(matrixA_sending.map(_.map(_.toDouble)))
          println("B:")
          printMatrix(matrixB_sending.map(_.map(_.toDouble)))

          if (matrixA_queue.isEmpty || matrixB_queue.isEmpty || transposeQueue.isEmpty) {
            if (AllTestCaseSentReported != true) {
              println("All input queues are empty, stopping StreamDriver.")
              AllTestCaseSentReported = true
            }
            AllTestCaseSent_Count += 1
          } else {
            matrixA_sending = matrixA_queue.dequeue()
            matrixB_sending = matrixB_queue.dequeue()
            doTranspose_sending = transposeQueue.dequeue()
            println(s"matrixA_queue size: ${matrixA_queue.size}")
            println(s"matrixB_queue size: ${matrixB_queue.size}")
            println(s"transposeQueue size: ${transposeQueue.size}")
            println(s"refQueue size: ${refQueue.size}")
            case_sent_num += 1
          }
        } else {
          StreamDriver_sending_period = StreamDriver_sending_period + 1
        }
        true
      }
    }

    // 输出流随机化，模拟ready信号
    StreamReadyRandomizer(dut.io.out_Mats, dut.clockDomain)
    dut.io.out_Mats.ready #= true

    var Dut_push_counter = 0 // DUT输出计数
    var Ref_push_counter = 0 // 参考输出计数

    // 监控DUT输出并与参考结果对比
    StreamMonitor(dut.io.out_Mats, dut.clockDomain) { payload =>
      var dut_result = Array.fill(cfg.in_MatA_row_num) { Array.fill(cfg.in_MatB_col_num) { 0.0 } }
      if (dut.io.out_Mats.valid.toBoolean) {
        for (row_index <- 0 until cfg.in_MatA_row_num) {
          for (col_index <- 0 until cfg.in_MatB_col_num) {
            val sign = payload.Z(row_index)(col_index).sign.toBigInt
            val exp = payload.Z(row_index)(col_index).exp.toBigInt
            val mant = payload.Z(row_index)(col_index).mant.toBigInt
            val bits = ((sign << (cfg.fpConfig.exp_size + cfg.fpConfig.mant_size)) |
              (exp << cfg.fpConfig.mant_size) |
              mant).toInt
            dut_result(row_index)(col_index) = algo.fromBits(bits)
          }
        }

        println(s"dut_result${Dut_push_counter}:")
        printMatrix(dut_result)
        scoreboard.pushDut(dut_result)
        Dut_push_counter = Dut_push_counter + 1

        println(s"ref_result${Ref_push_counter}:")
        var resultMatrix = refQueue.dequeue()
        printMatrix(resultMatrix)
        scoreboard.pushRef(resultMatrix)
        Ref_push_counter = Ref_push_counter + 1
      }
    }

    // 启动时钟激励
    dut.clockDomain.forkStimulus(10)

    // 等待所有结果比对完成
    dut.clockDomain.waitActiveEdgeWhere(scoreboard.matches == matrix_num)

    // 仿真结束时输出统计信息
    println("TEST PASS")
    simSuccess() // 仿真成功退出
  }

  if (AllTestCaseSent_Count > 0) {
    println(s"All test cases sent count: $AllTestCaseSent_Count")
  }
}

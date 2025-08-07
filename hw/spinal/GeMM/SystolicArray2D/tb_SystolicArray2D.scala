package GeMM.SystolicArray2D
import spinal.core._
import spinal.core.sim.SimConfig
import spinal.core.sim._
import spinal.lib.sim.{StreamMonitor, StreamDriver, StreamReadyRandomizer, ScoreboardInOrder_matrix}
import scala.util.Random
import scala.collection.mutable
import Interface.MatrixOperation_TypeDef

object SystolicArray2D_Sim extends App {
  val FileDir = "rtl/SystolicArray2D/verilog"
  import java.io.File
  new File(FileDir).mkdirs()
  val matrix_num = 10
  val matmul_mult_dim = 8


  // 矩阵A的行数和列数
  val rowsA = 4
  val colsA = matmul_mult_dim

  // 矩阵B的行数（必须等于矩阵A的列数），列数
  val rowsB = colsA
  val colsB = 4

  val elementWise_dimA = rowsA
  val elementWise_dimB = colsB
  val elementWise_mult_dim = rowsA

  var mult_dim = matmul_mult_dim
  // 统计无效模式出现次数和提示标志
  var AllTestCaseSent_Count = 0 // 统计无效模式出现次数
  var AllTestCaseSentReported = false // 是否已打印过提示

  val cfg = SystolicArray2D_Config(
    in_Length_Max = matmul_mult_dim,
    in_Length_Min = rowsA,
    in_MatA_row_num = rowsA,
    in_MatB_col_num = colsB,
    in_MatA_element_Width = 8,
    in_MatB_element_Width = 8,
    out_MatZ_element_Width = 32,
    Enable_Transpose_logic = true
  )
  //verilator
  /* val Sim_compiled = SimConfig.
  withFstWave.
  compile(new SystolicArray2D(cfg)) 
  */
  //VCS
  
  import spinal.sim.VCSFlags
  val flag = VCSFlags(
    compileFlags = List("-kdb","-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"),
    //    runFlags = List("-fgp=num_threads:11,allow_less_cores", "-l ./run.log")
    //    elaborateFlags = List("-fgp", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )
  val Spinalcfg=SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
    bitVectorWidthMax = 20000, //disable internal bigvector limitation"Way too big signal Bits"
    )
val Sim_compiled=SimConfig
      .withVCS(flag)
      .withVcdWave
      .withTimeScale(1 ns)
      .withTimePrecision(1 ns)
      .withConfig(Spinalcfg)
      .allOptimisation
      .compile(new SystolicArray2D(cfg))
    // 生成随机矩阵
  def generateRandomMatrix(rows: Int, cols: Int,seed: Int): Array[Array[Int]] = {
    val rand = new Random()
    rand.setSeed(seed)
    Array.fill(rows, cols)(rand.nextInt(16)) // 随机生成0到15之间的整数
  }
  // 随机生成移位量
  def generateRandomShiftAmount(shiftAmount: Int, seed: Int): Int = {
    require(shiftAmount >= 0, "移位量必须为非负整数")
    val rand = new Random(seed)
    //rand.nextInt(shiftAmount * 2 + 1) - shiftAmount  // 注意 +1 才能包含上界
    0
  }
  //生成工作模式
  def generateRandomMode(bool: Boolean, do_transpose:Boolean): Array[Boolean] = {
    //    val rand = new Random()
    //    rand.setSeed(seed)
        val result = Array.fill(5)(false) // 随机生成0到1之间的整数
        result(0) = do_transpose//do_PostTranspose
        result(1) = false//bool//do_MatMul
        result(2) = false//(!bool)//do_ElementWiseMul
        result(3) = false//do_ElementWiseAdd
        result(4) = true//(!bool)//do_ElementWiseMax
        result
      }

  /**
  * 逆时针旋转矩阵 90 度
  */
  def rotateMatrixCounterClockwise(matrix: Array[Array[Int]]): Array[Array[Int]] = {
    val rows = matrix.length
    val cols = matrix(0).length
    val rotated = Array.ofDim[Int](cols, rows) // 新的矩阵是 cols x rows
    for (i <- 0 until rows) {
      for (j <- 0 until cols) {
        rotated(cols - j - 1)(i) = matrix(i)(j)
      }
    }
    rotated
  }

  /**
  * 按元素乘矩阵ref
  */
//  def elementWiseMultiplyMatrix(A: Array[Array[Int]], B: Array[Array[Int]]): Array[Array[Int]] = {
//    val n = A.length
//    val result = Array.ofDim[Int](n, n)
//    for (i <- 0 until n) {
//      for (j <- 0 until n) {
//        result(i)(j) = A(i)(j) * B(i)(j)
//      }
//    }
//    result
//  }

  /**
  * 按元素加矩阵ref
  */
//  def elementWiseAdditionMatrix(A: Array[Array[Int]], B: Array[Array[Int]]): Array[Array[Int]] = {
//    val n = A.length
//    val result = Array.ofDim[Int](n, n)
//    for (i <- 0 until n) {
//      for (j <- 0 until n) {
//        result(i)(j) = A(i)(j) * B(i)(j)
//      }
//    }
//    result
//  }

  /**
   * 带移位与饱和保护的整数矩阵按元素操作ref
   * @param A 矩阵A（m × n）
   * @param B 矩阵B（m × n）
   * @param op 操作符（×, +）
   * @param shiftAmount 位移量（正：右移，负：左移）
   * @param satBits 饱和位宽，即输出矩阵元素在SInt格式下的位宽
   * @return m × n 结果矩阵
   */
  def elementWiseBinaryOpWithShiftAndSaturation(
                                                 A: Array[Array[Int]],
                                                 B: Array[Array[Int]],
                                                 op: (Int, Int) => Int,    // 运算函数：加法或乘法
                                                 shiftAmount: Int,
                                                 satBits: Int
                                               ): Array[Array[Int]] = {
    require(satBits > 0 && satBits <= 32, "位宽必须为 1 到 32 之间")

    val n = Math.min(A.length, B.length)
    val minVal = -(1 << (satBits - 1))
    val maxVal =  (1 << (satBits - 1)) - 1

    val result = Array.ofDim[Int](n, n)

    for (i <- 0 until n; j <- 0 until n) {
      val aVal = A(i)(j)
      val bVal = B(i)(j)
      val raw = op(aVal, bVal)
      val shifted =
        if (shiftAmount > 0) raw >> shiftAmount
        else if (shiftAmount < 0) raw << (-shiftAmount)
        else raw

      result(i)(j) =
        if (shifted > maxVal) maxVal
        else if (shifted < minVal) minVal
        else shifted
    }

    result
  }

//  def elementWiseBinaryOpWithShiftAndSaturation(
//                                                 A: Array[Array[Int]],
//                                                 B: Array[Array[Int]],
//                                                 op: (Int, Int) => Int,    // 运算函数：乘或加
//                                                 shiftAmount: Int,
//                                                 satBits: Int
//                                               ): Array[Array[Int]] = {
//    require(A.length == B.length && A(0).length == B(0).length, "矩阵维度不一致")
//    require(satBits > 0 && satBits <= 32, "位宽必须为 1 到 32 之间")
//
//    val rows = A.length
//    val cols = A(0).length
//    val minVal = -(1 << (satBits - 1))
//    val maxVal =  (1 << (satBits - 1)) - 1
//
//    val result = Array.ofDim[Int](rows, cols)
//
//    for (i <- 0 until rows; j <- 0 until cols) {
//      val raw = op(A(i)(j), B(i)(j))
//      val shifted = if (shiftAmount > 0) {
//        raw >> shiftAmount
//      } else if (shiftAmount < 0) {
//        raw << (-shiftAmount)
//      } else raw
//
//      result(i)(j) =
//        if (shifted > maxVal) maxVal
//        else if (shifted < minVal) minVal
//        else shifted
//    }
//
//    result
//  }
  def elementWiseMultiplyMatrixWithShiftAndSaturation(
                                 A: Array[Array[Int]],
                                 B: Array[Array[Int]],
                                 shiftAmount: Int,
                                 satBits: Int
                               ): Array[Array[Int]] = {
    elementWiseBinaryOpWithShiftAndSaturation(A, B, _ * _, shiftAmount, satBits)
  }
  def elementWiseAdditionMatrixWithShiftAndSaturation(
                                 A: Array[Array[Int]],
                                 B: Array[Array[Int]],
                                 shiftAmount: Int,
                                 satBits: Int
                               ): Array[Array[Int]] = {
    elementWiseBinaryOpWithShiftAndSaturation(A, B, _ + _, shiftAmount, satBits)
  }

  def elementWiseMaximumMatrixWithShiftAndSaturation(
                                                       A: Array[Array[Int]],
                                                       B: Array[Array[Int]],
                                                       shiftAmount: Int,
                                                       satBits: Int
                                                     ): Array[Array[Int]] = {
    elementWiseBinaryOpWithShiftAndSaturation(A, B, _ max _, shiftAmount, satBits)
  }

  /**
  * 矩阵乘ref
  */
  def multiplyMatrices(matrixA: Array[Array[Int]], matrixB: Array[Array[Int]]): Array[Array[Int]] = {
    val rowsA = matrixA.length
    val colsA = matrixA(0).length
    val rowsB = matrixB.length
    val colsB = matrixB(0).length

    require(colsA == rowsB, "矩阵A的列数必须等于矩阵B的行数")

    val result = Array.ofDim[Int](rowsA, colsB)
    for (i <- 0 until rowsA) {
      for (j <- 0 until colsB) {
        result(i)(j) = (0 until colsA).map(k => matrixA(i)(k) * matrixB(k)(j)).sum
      }
    }
    result
  }

  /**
   * 带移位与饱和保护的整数矩阵乘法ref
   * @param matrixA 左矩阵（m × k）
   * @param matrixB 右矩阵（k × n）
   * @param shiftAmount 位移量（正：右移，负：左移）
   * @param satBits 饱和位宽，即输出矩阵元素在SInt格式下的位宽
   * @return m × n 结果矩阵（Int表示，已经做移位与饱和）
   */
  def multiplyMatricesWithShiftSaturation(matrixA: Array[Array[Int]], matrixB: Array[Array[Int]],
                                           shiftAmount: Int, satBits: Int): Array[Array[Int]] = {
    val rowsA = matrixA.length
    val colsA = matrixA(0).length
    val rowsB = matrixB.length
    val colsB = matrixB(0).length
    require(colsA == rowsB, "矩阵A的列数必须等于矩阵B的行数")
    require(satBits > 0 && satBits <= 32, "位宽必须为 1 到 32 之间的正整数")
    val minVal = -(1 << (satBits - 1))           // 饱和最小值
    val maxVal =  (1 << (satBits - 1)) - 1       // 饱和最大值
    val result = Array.ofDim[Int](rowsA, colsB)
    for (i <- 0 until rowsA) {
      for (j <- 0 until colsB) {
        val rawSum = (0 until colsA).map(k => matrixA(i)(k) * matrixB(k)(j)).sum
        // 移位处理
        val shifted = if (shiftAmount > 0) {
          rawSum >> shiftAmount
        } else if (shiftAmount < 0) {
          rawSum << (-shiftAmount)
        } else {
          rawSum
        }
        // 饱和截断处理
        result(i)(j) =
          if (shifted > maxVal) maxVal
          else if (shifted < minVal) minVal
          else shifted
      }
    }
    result
  }

  /**
  * 打印矩阵
  */
  def printMatrix(matrix: Array[Array[Int]]): Unit = {
    matrix.foreach(row => println(row.mkString("\t")))
    println()
  }

  val matrixA_queue = mutable.Queue[Array[Array[Int]]]()
  val matrixB_queue = mutable.Queue[Array[Array[Int]]]()
  val mode_queue = mutable.Queue[Array[Boolean]]()
  val shift_queue = mutable.Queue[Int]()
  val ref_queue = mutable.Queue[Array[Array[Int]]]()

  val random = new Random()
  val global_seed = 1234//1334
  random.setSeed(global_seed)

  //准备数据
  for (matrix_idx <- 0 until matrix_num) {
    var m_A = generateRandomMatrix(rowsA, colsA,random.nextInt(20))
    var m_B = generateRandomMatrix(rowsB, colsB,random.nextInt(20))
    var mode = generateRandomMode(random.nextBoolean(),random.nextBoolean())//true:matmul false:elementwise false
    var shift = generateRandomShiftAmount(shiftAmount = 4, seed = random.nextInt(20))
    // 生成两个随机矩阵
    matrixA_queue.enqueue(m_A)
    matrixB_queue.enqueue(m_B)
    mode_queue.enqueue(mode)
    shift_queue.enqueue(shift)
    if(mode(0) ==true){
      if(mode(1)==true) {
        //ref_queue.enqueue(multiplyMatrices(m_A, m_B).transpose)
        //考虑输出移位与饱和处理
        ref_queue.enqueue(multiplyMatricesWithShiftSaturation(m_A, m_B, shiftAmount=shift, satBits = cfg.out_MatZ_element_Width).transpose)
      } else if(mode(2)==true) {
        m_B = rotateMatrixCounterClockwise(m_B)
        //ref_queue.enqueue(elementWiseMultiplyMatrix(m_A, m_B).transpose)
        //考虑输出移位与饱和处理
        ref_queue.enqueue(elementWiseMultiplyMatrixWithShiftAndSaturation(m_A, m_B, shiftAmount=shift, satBits = cfg.out_MatZ_element_Width).transpose)
      } else if(mode(3)==true) {
        m_B = rotateMatrixCounterClockwise(m_B)
        //ref_queue.enqueue(elementWiseAdditionMatrix(m_A, m_B).transpose)
        //考虑输出移位与饱和处理
        ref_queue.enqueue(elementWiseAdditionMatrixWithShiftAndSaturation(m_A, m_B, shiftAmount=shift, satBits = cfg.out_MatZ_element_Width).transpose)
      } else if(mode(4)==true) {
        m_B = rotateMatrixCounterClockwise(m_B)
        ref_queue.enqueue(elementWiseMaximumMatrixWithShiftAndSaturation(m_A, m_B, shiftAmount=shift, satBits = cfg.out_MatZ_element_Width).transpose)
      } else {
        println("Invalid mode.")
        assert(false)
      }
    } else {
      if(mode(1)==true) {
        //ref_queue.enqueue(multiplyMatrices(m_A, m_B))
        ref_queue.enqueue(multiplyMatricesWithShiftSaturation(m_A, m_B, shiftAmount=shift, satBits = cfg.out_MatZ_element_Width))
      } else if(mode(2)==true) {
        m_B = rotateMatrixCounterClockwise(m_B)
        //ref_queue.enqueue(elementWiseMultiplyMatrix(m_A, m_B))
        ref_queue.enqueue(elementWiseMultiplyMatrixWithShiftAndSaturation(m_A, m_B, shiftAmount=shift, satBits = cfg.out_MatZ_element_Width))
      } else if(mode(3)==true) {
        m_B = rotateMatrixCounterClockwise(m_B)
        //ref_queue.enqueue(elementWiseAdditionMatrix(m_A, m_B))
        ref_queue.enqueue(elementWiseAdditionMatrixWithShiftAndSaturation(m_A, m_B, shiftAmount=shift, satBits = cfg.out_MatZ_element_Width))
      } else if (mode(4)==true){
        m_B = rotateMatrixCounterClockwise(m_B)
        //ref_queue.enqueue(elementWiseAdditionMatrix(m_A, m_B))
        ref_queue.enqueue(elementWiseMaximumMatrixWithShiftAndSaturation(m_A, m_B, shiftAmount=shift, satBits = cfg.out_MatZ_element_Width))
      } else {
        println("Invalid mode.")
        assert(false)
      }
    }
  }

  Sim_compiled.doSim("simple test") { dut =>
    SimTimeout(19000) // 设置仿真超时时间，单位为仿真时钟周期

    val scoreboard = new ScoreboardInOrder_matrix() // 创建自动对比参考结果的记分板
    var StreamDriver_sending_period = 0 // 记录当前数据流发送周期
    var reset_done=false
    var do_random_clk = false

    var matrixA_sending = matrixA_queue.dequeue() // 当前正在发送的矩阵A
    var matrixB_sending = matrixB_queue.dequeue() // 当前正在发送的矩阵B
    var mode_sending = mode_queue.dequeue() // 当前工作模式
    var shift_sending = shift_queue.dequeue()
    // 启动输入流驱动器，向DUT输入数据
    dut.io.in_Mats.valid #= true
    StreamDriver(dut.io.in_Mats, dut.clockDomain) { payload =>
      if (matrixA_queue.isEmpty || matrixB_queue.isEmpty || mode_queue.isEmpty || shift_queue.isEmpty) {
        if(AllTestCaseSentReported!=true)
        {
          println("All input queues are empty, stopping StreamDriver.")
          AllTestCaseSentReported = true
        }
        AllTestCaseSent_Count += 1
        false
      } else {
      // 设置工作模式
      payload.OpMode.do_PostTranspose #= mode_sending(0)
      if((mode_sending(1)==true) && (mode_sending(2)==false) && (mode_sending(3)==false) && (mode_sending(4)==false))
        payload.OpMode.MatrixOperation #= MatrixOperation_TypeDef.MatMul // 矩阵乘法
      else if(mode_sending(1)==false && mode_sending(2)==true && mode_sending(3)==false && mode_sending(4)==false)
        payload.OpMode.MatrixOperation #= MatrixOperation_TypeDef.ElementMul // 按元素乘
      else if(mode_sending(1)==false && mode_sending(2)==false && mode_sending(3)==true&& mode_sending(4)==false)
        payload.OpMode.MatrixOperation #= MatrixOperation_TypeDef.ElementAdd // 按元素加
      else if(mode_sending(1)==false && mode_sending(2)==false && mode_sending(3)==false&& mode_sending(4)==true)
        payload.OpMode.MatrixOperation #= MatrixOperation_TypeDef.ElementMax // 按元素最大值
      else {
        println("Invalid mode.")
        assert(false)
      }

      payload.OpMode.post_Shift #= shift_sending

      //确保按元素操作的矩阵均为正方形（由于输出buffer的尺寸限制）
      if (mode_sending(1)==false){
        mult_dim = rowsA
      } else {
        mult_dim = matmul_mult_dim
      }
      // 依次为每一行/列赋值
      // A矩阵输入
      for (row_index <- 0 until cfg.in_MatA_row_num) {
        if (StreamDriver_sending_period < mult_dim - 1) {
          payload.A(row_index).data #= matrixA_sending(row_index)(StreamDriver_sending_period)
          payload.A(row_index).Final #= false
        } else if (StreamDriver_sending_period == mult_dim - 1) {
          payload.A(row_index).data #= matrixA_sending(row_index)(StreamDriver_sending_period)
          payload.A(row_index).Final #= true // 最后一个数据打Final信号
        } else {
          payload.A(row_index).data #= 0
          payload.A(row_index).Final #= false
        }
      }
      // B矩阵输入
      for (col_index <- 0 until cfg.in_MatB_col_num) {
        if (StreamDriver_sending_period < mult_dim - 1) {
          payload.B(col_index).data #= matrixB_sending(StreamDriver_sending_period)(col_index)
          payload.B(col_index).Final #= false
        } else if (StreamDriver_sending_period == mult_dim - 1) {
          payload.B(col_index).data #= matrixB_sending(StreamDriver_sending_period)(col_index)
          payload.B(col_index).Final #= true
        } else {
          payload.B(col_index).data #= 0
          payload.B(col_index).Final #= false
        }
      }
      // 打印当前周期信息
      println(s"StreamDriver called:${StreamDriver_sending_period}")
      // 判断是否切换到下一组输入
      if (StreamDriver_sending_period == mult_dim - 1) {
        StreamDriver_sending_period = 0
        println(s"new input loaded")
        println("A:")
        printMatrix(matrixA_sending)
        println("B:")
        printMatrix(matrixB_sending)
        println("Z:")
        if (mode_sending(1)==true)
          //printMatrix(multiplyMatrices(matrixA_sending, matrixB_sending))
          printMatrix(multiplyMatricesWithShiftSaturation(matrixA_sending, matrixB_sending,shiftAmount= shift_sending, satBits = cfg.out_MatZ_element_Width))
        else if (mode_sending(2)==true)
          //printMatrix(elementWiseMultiplyMatrix(matrixA_sending, matrixB_sending))
          printMatrix(elementWiseMultiplyMatrixWithShiftAndSaturation(matrixA_sending, matrixB_sending,shiftAmount= shift_sending, satBits = cfg.out_MatZ_element_Width))
        else if (mode_sending(3)==true)
          //printMatrix(elementWiseAdditionMatrix(matrixA_sending, matrixB_sending))
          printMatrix(elementWiseAdditionMatrixWithShiftAndSaturation(matrixA_sending, matrixB_sending,shiftAmount= shift_sending, satBits = cfg.out_MatZ_element_Width))
        else if (mode_sending(4)==true)
          printMatrix(elementWiseMaximumMatrixWithShiftAndSaturation(matrixA_sending, matrixB_sending,shiftAmount= shift_sending, satBits = cfg.out_MatZ_element_Width))
        else{
          println("Invalid mode.")
          assert(false)
        }
        matrixA_sending = matrixA_queue.dequeue()
        matrixB_sending = matrixB_queue.dequeue()
        mode_sending = mode_queue.dequeue()
        shift_sending = shift_queue.dequeue()
        println(s"matrixA_queue size: ${matrixA_queue.size}")
        println(s"matrixB_queue size: ${matrixB_queue.size}")
        println(s"mode_queue size: ${mode_queue.size}")
        println(s"ref_queue size: ${ref_queue.size}")
      } else { StreamDriver_sending_period = StreamDriver_sending_period + 1}
      true}
    }

    // 输出流随机化，模拟ready信号
    StreamReadyRandomizer(dut.io.out_Mats, dut.clockDomain)
    dut.io.out_Mats.ready #= true
    var Dut_push_counter = 0 // DUT输出计数
    var Ref_push_counter = 0 // 参考输出计数
    // 监控DUT输出并与参考结果对比
    StreamMonitor(dut.io.out_Mats, dut.clockDomain) { payload =>
      var dut_result = Array.fill(cfg.in_MatA_row_num) { Array.fill(cfg.in_MatB_col_num) { 0 } }
      if (dut.io.out_Mats.valid.toBoolean) {
        for (row_index <- 0 until cfg.in_MatA_row_num) {
          for (col_index <- 0 until cfg.in_MatB_col_num) {
            dut_result(row_index)(col_index) = payload.Z(row_index)(col_index).toInt
          }
        }
        println(s"dut_result${Dut_push_counter}:")
        printMatrix(dut_result)
        scoreboard.pushDut(dut_result)
        Dut_push_counter = Dut_push_counter+1
        println(s"ref_result${Ref_push_counter}:")
        var resultMatrix =ref_queue.dequeue()
        printMatrix(resultMatrix)
        scoreboard.pushRef(resultMatrix)
        Ref_push_counter=Ref_push_counter+1
      }
    }
    // 监控输入流（可选）
    StreamMonitor(dut.io.in_Mats, dut.clockDomain) { payload =>
      {}
    }
    // 启动时钟激励
    dut.clockDomain.forkStimulus(10)
    // 等待所有结果比对完成
    dut.clockDomain.waitActiveEdgeWhere(scoreboard.matches == matrix_num)
    // 仿真结束时输出无效模式统计
    
    simSuccess() // 仿真成功退出
  }
  if (AllTestCaseSent_Count > 0) {
    println(s"Invalid mode occurred $AllTestCaseSent_Count times.")
  }
}

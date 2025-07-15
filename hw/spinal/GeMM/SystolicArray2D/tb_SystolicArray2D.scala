package GeMM.SystolicArray2D
import spinal.core._
import spinal.core.sim.SimConfig
import spinal.core.sim._
import spinal.lib.sim.{StreamMonitor, StreamDriver, StreamReadyRandomizer, ScoreboardInOrder_matrix}
import scala.util.Random
import breeze.linalg.min

object SystolicArray2D_Sim extends App {
  val FileDir = "rtl/SystolicArray2D/verilog"
  import java.io.File
  new File(FileDir).mkdirs()
  val matrix_num=100
  val matmul_mult_dim = 4


  // 矩阵A的行数和列数
  val rowsA = 4
  val colsA = matmul_mult_dim

  // 矩阵B的行数（必须等于矩阵A的列数），列数
  val rowsB = colsA
  val colsB = 4
  val elementWise_dimA = rowsA
  val elementWise_dimB = colsB
  val elementWise_mult_dim = rowsA


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
val Sim_compiled=SimConfig
      .withVCS(flag)
      .withFsdbWave
      .withTimeScale(1 ns)
      .withTimePrecision(1 ns)
      .withConfig(SpinalConfig(
        
        targetDirectory = FileDir,
        oneFilePerComponent = true,
        defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
        ))
      .allOptimisation
      .compile(new SystolicArray2D(cfg))
    // 生成随机矩阵
  def generateRandomMatrix(rows: Int, cols: Int,seed: Int): Array[Array[Int]] = {
    val rand = new Random()
    rand.setSeed(seed)
    Array.fill(rows, cols)(rand.nextInt(16)) // 随机生成0到9之间的整数
  }
  //随机生成工作模式
  def generateRandomMode(bool: Boolean, do_transpose:Boolean): Array[Boolean] = {
//    val rand = new Random()
//    rand.setSeed(seed)
    val result = Array.fill(4)(false) // 随机生成0到1之间的整数
    //val bool = Random.nextBoolean()
    //val bool = rand.nextBoolean()
    result(0) = do_transpose//do_PostTranspose
    result(1) = bool//bool//do_MatMul
    result(2) = !bool//(!bool)//do_ElementWiseMul
    result(3) = false//do_ElementWiseAdd
    result
  }
  // 逆时针旋转矩阵 90 度
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
  //按元素乘矩阵ref
  def elementWiseMultiplyMatrix(A: Array[Array[Int]], B: Array[Array[Int]]): Array[Array[Int]] = {
    val n = A.length
    val result = Array.ofDim[Int](n, n)
    for (i <- 0 until n) {
      for (j <- 0 until n) {
        result(i)(j) = A(i)(j) * B(i)(j)
      }
    }
    result
  }
  //按元素加矩阵ref
  def elementWiseAdditionMatrix(A: Array[Array[Int]], B: Array[Array[Int]]): Array[Array[Int]] = {
    val n = A.length
    val result = Array.ofDim[Int](n, n)
    for (i <- 0 until n) {
      for (j <- 0 until n) {
        result(i)(j) = A(i)(j) * B(i)(j)
      }
    }
    result
  }
  // 打印矩阵
  def printMatrix(matrix: Array[Array[Int]]): Unit = {
    matrix.foreach(row => println(row.mkString("\t")))
    println()
  }

  //矩阵乘ref
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

  val random = new Random()
  import scala.collection.mutable
  val matrixA_queue=mutable.Queue[Array[Array[Int]]]()
  val matrixB_queue=mutable.Queue[Array[Array[Int]]]()
  val mode_queue = mutable.Queue[Array[Boolean]]()
  val ref_queue=mutable.Queue[Array[Array[Int]]]()
  for(matrix_idx<-0 until matrix_num)
  // 生成两个随机矩阵
{    var m_A=generateRandomMatrix(rowsA, colsA,random.nextInt(20))
  var m_B=generateRandomMatrix(rowsB, colsB,random.nextInt(20))
  var mode = generateRandomMode(random.nextBoolean(),random.nextBoolean())//true:matmul false:elementwise false
  matrixA_queue.enqueue(m_A)
    matrixB_queue.enqueue(m_B)
    mode_queue.enqueue(mode)
        if(mode(0) ==true){
      if(mode(1)==true) {
        ref_queue.enqueue(multiplyMatrices(m_A, m_B).transpose)
      } else if(mode(2)==true) {
        m_B = rotateMatrixCounterClockwise(m_B)
        ref_queue.enqueue(elementWiseMultiplyMatrix(m_A, m_B).transpose)
      } else if(mode(3)==true) {
        m_B = rotateMatrixCounterClockwise(m_B)
        ref_queue.enqueue(elementWiseAdditionMatrix(m_A, m_B).transpose)
      }else{
        println("Invalid mode.")
        assert(false)
      }
    } else {
      if(mode(1)==true) {
        ref_queue.enqueue(multiplyMatrices(m_A, m_B))
      } else if(mode(2)==true) {
        m_B = rotateMatrixCounterClockwise(m_B)
        ref_queue.enqueue(elementWiseMultiplyMatrix(m_A, m_B))
      } else if(mode(3)==true) {
        m_B = rotateMatrixCounterClockwise(m_B)
        ref_queue.enqueue(elementWiseAdditionMatrix(m_A, m_B))
      }else{
        println("Invalid mode.")
        assert(false)
      }
    }
}

  Sim_compiled.doSim("simple test") { dut =>
    SimTimeout(120000)

    val scoreboard = new ScoreboardInOrder_matrix()

    var StreamDriver_sending_period = 0

    var matrixA_sending=matrixA_queue.dequeue()
    var matrixB_sending=matrixB_queue.dequeue()
    var mode_sending = mode_queue.dequeue()
    // drive random data and add pushed data to scoreboard
    dut.io.in_Mats.valid #= true
    dut.io.in_Mats.payload.mode.do_MatMul #= true
    dut.io.in_Mats.payload.mode.do_PostTranspose #= false
    dut.io.in_Mats.payload.mode.do_ElementWiseAdd #= false
    dut.io.in_Mats.payload.mode.do_ElementWiseMul #= false

    var mult_dim = matmul_mult_dim
    StreamDriver(dut.io.in_Mats, dut.clockDomain) { payload =>
      payload.mode.do_PostTranspose #= mode_sending(0)
      payload.mode.do_MatMul #= mode_sending(1)
      payload.mode.do_ElementWiseMul #= mode_sending(2)
      payload.mode.do_ElementWiseAdd #= mode_sending(3)
      if (mode_sending(1)==false){
        mult_dim = rowsA
      } else {
        mult_dim = matmul_mult_dim
      }
      // A
      for (row_index <- 0 until cfg.in_MatA_row_num) {
        if (StreamDriver_sending_period < mult_dim - 1) {
          payload.A(row_index).data #= matrixA_sending(row_index)(StreamDriver_sending_period)
          payload.A(row_index).Final #= false
        } else if (StreamDriver_sending_period == mult_dim - 1) {
          payload.A(row_index).data #= matrixA_sending(row_index)(StreamDriver_sending_period)
          payload.A(row_index).Final #= true
        } else {
          payload.A(row_index).data #= 0
          payload.A(row_index).Final #= false
        }
      }
      // B
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
      println(s"StreamDriver called:${StreamDriver_sending_period}")
      if (StreamDriver_sending_period == mult_dim - 1) { 
        StreamDriver_sending_period = 0 

        println(s"new input loaded")
        println("A:")
        printMatrix(matrixA_sending)
        println("B:")
        printMatrix(matrixB_sending)
        println("Z:")
        if (mode_sending(1)==true)
          printMatrix(multiplyMatrices(matrixA_sending, matrixB_sending))
        else if (mode_sending(2)==true)
          printMatrix(elementWiseMultiplyMatrix(matrixA_sending, matrixB_sending))
        else if (mode_sending(3)==true)
          printMatrix(elementWiseAdditionMatrix(matrixA_sending, matrixB_sending))
        else{
          println("Invalid mode.")
          assert(false)
        }
        matrixA_sending = matrixA_queue.dequeue()
        matrixB_sending = matrixB_queue.dequeue()
        mode_sending = mode_queue.dequeue()
      }
      else { StreamDriver_sending_period = StreamDriver_sending_period + 1 }
      true
    }

    // add popped data to scoreboard
StreamReadyRandomizer(dut.io.out_Mats, dut.clockDomain)
    dut.io.out_Mats.ready #= true
    var Dut_push_counter = 0
    var Ref_push_counter = 0
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
    
    StreamMonitor(dut.io.in_Mats, dut.clockDomain) { payload =>
      //if (dut.io.out_Mats.valid.toBoolean) 
      {
      }
    }
    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.waitActiveEdgeWhere(scoreboard.matches == 80)
    simSuccess()
  }
}

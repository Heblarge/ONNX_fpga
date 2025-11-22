package MatrixComputeUnit.ElementWise
import spinal.core._
import spinal.core.sim.SimConfig
import spinal.core.sim._
import spinal.lib.sim.{StreamMonitor, StreamDriver, StreamReadyRandomizer, ScoreboardInOrder_matrix}
import scala.util.Random

case object tb_ElementWise extends App{
  val FileDir = "rtl/SystolicArray2D/verilog"
  import java.io.File
  new File(FileDir).mkdirs()
  //一些参数
  val matrix_num=20
  val rowsA = 4
  val colsA = 4
  val mult_dim = colsA
  val rowsB = rowsA
  val colsB = colsA
  val cfg = ElementWise_Config(
    in_Length_Max = 8,
    in_Length_Min = 1,
    in_Mat_row_num = rowsA,
    in_MatA_element_Width = 8,
    in_MatB_element_Width = 8,
    out_MatZ_element_Width = 16
  )
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
    .compile(new ElementWise(cfg))

  def generateRandomMatrix(rows: Int, cols: Int): Array[Array[Int]] = {
    val rand = new Random()
    Array.fill(rows, cols)(rand.nextInt(16))
  }
  def generateFinals(cols: Int): Array[Boolean] = {
    require(cols > 0, "数组长度必须大于0")
    val result = Array.fill(cols)(false)
    result(cols - 1) = true
    result
  }

  def printMatrix(matrix: Array[Array[Int]]): Unit = {
    matrix.foreach(row => println(row.mkString("\t")))
    println()
  }
  def hadamardProduct(matrixA: Array[Array[Int]], matrixB: Array[Array[Int]]): Array[Array[Int]] = {
    val rows = matrixA.length
    val cols = matrixA(0).length
    require(matrixB.length == rows && matrixB(0).length == cols, "两个矩阵的维度必须相同")
    val result = Array.ofDim[Int](rows, cols)
    // 逐元素相乘
    for (i <- 0 until rows) {
      for (j <- 0 until cols) {
        result(i)(j) = matrixA(i)(j) * matrixB(i)(j)
      }
    }
    result
  }
  import scala.collection.mutable
  val matrixA_queue=mutable.Queue[Array[Array[Int]]]()
  val matrixB_queue=mutable.Queue[Array[Array[Int]]]()
  val final_queue=mutable.Queue[Array[Boolean]]()
  val ref_queue=mutable.Queue[Array[Array[Int]]]()
  for(matrix_idx<-0 until matrix_num)
    // 生成两个随机矩阵
  {
    var m_A=generateRandomMatrix(rowsA, colsA)
    var m_B=generateRandomMatrix(rowsB, colsB)
    var m_final = generateFinals(colsA)
    matrixA_queue.enqueue(m_A)
    matrixB_queue.enqueue(m_B)
    final_queue.enqueue(m_final)
    ref_queue.enqueue(hadamardProduct(m_A, m_B))
  }
  Sim_compiled.doSim("simple test") { dut =>
    SimTimeout(60000)
    val scoreboard = new ScoreboardInOrder_matrix()
    var StreamDriver_sending_period = 0
    var matrixA_sending=matrixA_queue.dequeue()
    var matrixB_sending=matrixB_queue.dequeue()
    var final_sending=final_queue.dequeue()
    dut.io.in_Mats.valid #= true
    dut.io.in_Mats.Final #= false
    StreamDriver(dut.io.in_Mats, dut.clockDomain) { payload =>
      for (row_index <- 0 until cfg.in_Mat_row_num) {
        if (StreamDriver_sending_period < mult_dim - 1) {
          payload.A(row_index) #= matrixA_sending(row_index)(StreamDriver_sending_period)
          payload.B(row_index) #= matrixB_sending(row_index)(StreamDriver_sending_period)
          payload.Final #= final_sending(StreamDriver_sending_period)
        } else if (StreamDriver_sending_period == mult_dim - 1) {
          payload.A(row_index) #= matrixA_sending(row_index)(StreamDriver_sending_period)
          payload.B(row_index) #= matrixB_sending(row_index)(StreamDriver_sending_period)
          payload.Final #= final_sending(StreamDriver_sending_period)
        } else {
          payload.A(row_index) #= 0
          payload.B(row_index) #= 0
          payload.Final #= false
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
        printMatrix(hadamardProduct(matrixA_sending, matrixB_sending))
        matrixA_sending=matrixA_queue.dequeue()
        matrixB_sending=matrixB_queue.dequeue()
      }
      else { StreamDriver_sending_period = StreamDriver_sending_period + 1 }

      true
    }
    //StreamReadyRandomizer(dut.io.out_Mats, dut.clockDomain)
    dut.io.out_Mats.ready #= true
    var Dut_push_counter = 0
    var Ref_push_counter = 0

    StreamMonitor(dut.io.out_Mats, dut.clockDomain) { payload =>
      var dut_result = Array.fill(cfg.in_Mat_row_num) { Array.fill(cfg.in_Length_Max) { 0 } }

      if (dut.io.out_Mats.valid.toBoolean) {
        for (row_index <- 0 until cfg.in_Mat_row_num) {
          for (col_index <- 0 until cfg.in_Length_Max) {
            dut_result(row_index)(col_index) = payload.Z(row_index)(col_index).toInt
          }
        }
        println(s"dut_result${Dut_push_counter}:")
        val submatrix = dut_result.map(row => row.slice(0, 4))
        printMatrix(submatrix)
        scoreboard.pushDut(submatrix)
        Dut_push_counter = Dut_push_counter+1
        println(s"ref_result${Ref_push_counter}:")
        var resultMatrix =ref_queue.dequeue()
        printMatrix(resultMatrix)
        scoreboard.pushRef(resultMatrix)
        Ref_push_counter=Ref_push_counter+1
      }
    }

    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.waitActiveEdgeWhere(scoreboard.matches == 5)
  }
}

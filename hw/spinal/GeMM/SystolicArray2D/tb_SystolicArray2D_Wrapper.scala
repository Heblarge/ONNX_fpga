package GeMM.SystolicArray2D

import GeMM.SystolicArray2D.SystolicArray2D_CC_Sim.FileDir
import spinal.core._
import spinal.lib._
import spinal.core.ClockDomain
import spinal.core.sim._
import spinal.core.sim.SimConfig
import spinal.lib.sim._
import scala.util.Random
import scala.collection.mutable
import java.io.File
//import Slicer._
import Interface._

import java.lang

case class SystolicArray2D_Wrap_depress_for_Sim(cfg: SystolicArray2D_Wrap_Config,
                                                clk_in: ClockDomain,
                                                clk_out: ClockDomain,
                                                clk_core: ClockDomain
                                               ) extends Component {
  val DUT=new SystolicArray2D_Wrapper(cfg,clk_in,clk_out,clk_core)

  val io = new Bundle {
    val in_Mats = slave(Stream(DUT.in_Mats_Type()))//.addAttribute("DONT_TOUCH = \"TRUE\"")
    val out_MatZ = master(
      Stream(Vec.fill(cfg.to_SystolicArray2D_CC_Config().out_MatZ_row_num)(Vec.fill(cfg.to_SystolicArray2D_CC_Config().out_MatZ_col_num)(SInt(cfg.out_MatZ_element_Width bits))))
    )//.addAttribute("DONT_TOUCH = \"TRUE\"")
    val out_coreInstruction_AfterMatrixOperation = master(Stream((DUT.CoreInstruction_AfterMatrixOperation_Type())))//.addAttribute("DONT_TOUCH = \"TRUE\"")
  }
  io.in_Mats>>DUT.io.in_Mats_with_Core_Instruction
//  val to_decompres = DUT.io.out_Mats_with_Core_Instruction.map(payload=>{
//    val turnto = Vec.fill(cfg.to_SystolicArray2D_CC_Config().out_MatZ_Width)(SInt(cfg.out_MatZ_element_Width bits))
//    turnto := payload.Z
//    turnto
//  })
//  val decompresser = clk_out(StreamWidthAdapter(to_decompres, io.out_MatZ))
  val outputStreams = StreamFork(DUT.io.out_Mats_with_Core_Instruction, portCount=2, synchronous=true)
  val to_decompres = outputStreams(0).map(payload=>{
    val turnto = Vec.fill(cfg.to_SystolicArray2D_CC_Config().out_MatZ_Width)(SInt(cfg.out_MatZ_element_Width bits))
    turnto := payload.Z
    turnto
  })
  val decompresser = clk_out(StreamWidthAdapter(to_decompres, io.out_MatZ))
//  val to_decompres_instru = outputStreams(1).map(payload=>{
//    val turnto = DUT.CoreInstruction_Type().matrixOperation
//    turnto := payload.CoreInstruction.matrixOperation
//    turnto
//  })
  //val decompresser_instru = clk_out(StreamWidthAdapter(to_decompres_instru, io.out_coreInstruction))
  io.out_coreInstruction_AfterMatrixOperation.payload.assignFrom(outputStreams(1).CoreInstruction_AfterMatrixOperation)
  io.out_coreInstruction_AfterMatrixOperation.valid := outputStreams(1).valid
  outputStreams(1).ready := io.out_coreInstruction_AfterMatrixOperation.ready
}

object tb_SystolicArray2d_Wrapper extends App {
  val FileDir = "rtl/SystolicArray2D_Wrapper/verilog"
  new File(FileDir).mkdirs()

  val matrix_num = 100
  val matmul_mult_dim = 4

  // 矩阵A的行数和列数
  val rowsA = 4
  val colsA = matmul_mult_dim

  // 矩阵B的行数（必须等于矩阵A的列数），列数
  val rowsB = colsA
  val colsB = 4

  var mult_dim = matmul_mult_dim

  val cfg = SystolicArray2D_Wrap_Config(
    in_Length_Max = mult_dim,
    in_Length_Min = rowsA,
    in_MatA_row_num = rowsA,
    in_MatB_col_num = colsB,
    in_MatA_element_Width = 8,
    in_MatB_element_Width = 8,
    out_MatZ_element_Width = 22,
    in_FIFO_Depth = 2,
    out_FIFO_Depth = 2,
    instruction_FIFO_Depth = 16,
    UIDWidth = 8,
    ShiftWidth = 20,
    SlicecntWidth = 16
  )
  val defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)

  // 配置仿真
  import spinal.sim.VCSFlags
  val flag = VCSFlags(
    compileFlags = List("-kdb","-lca", "+notimingchecks"), elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"), runFlags = List("-l ./run.log"))
  val Spinalcfg=SpinalConfig(targetDirectory = FileDir,
    oneFilePerComponent = true, defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW), bitVectorWidthMax = 20000) //disable internal bigvector limitation"Way too big signal Bits"
  val Sim_compiled=SimConfig.withVCS(flag).withFsdbWave.withTimeScale(1 ns).withTimePrecision(1 ns).withConfig(Spinalcfg).allOptimisation
    .compile(new SystolicArray2D_Wrap_depress_for_Sim(cfg=cfg, clk_in = ClockDomain.external("in"), clk_out = ClockDomain.external("out"), clk_core = ClockDomain.external("core")))

  // 软件函数
  def generateRandomMatrix(rows: Int, cols: Int,seed: Int): Array[Array[Int]] = {
    val rand = new Random()
    rand.setSeed(seed)
    Array.fill(rows, cols)(rand.nextInt(16)) // 随机生成0到9之间的整数
  }
  def generateRandomShiftAmount(shiftAmount: Int, seed: Int): Int = {
    require(shiftAmount >= 0, "移位量必须为非负整数")
    val rand = new Random(seed)
    //rand.nextInt(shiftAmount * 2 + 1) - shiftAmount  // 注意 +1 才能包含上界
    0
  }
  // 生成 Core Instruction

  case class CoreInstruction(val UID: Int,
                             val matrixOperation: MatrixOperation_TypeDef.E,
                             val shiftLeft_AfterMatrixOperation: Int,
                             val doTranspose: Boolean,
                             val activationFunction: Activation_TypeDef.E,
                             val shiftLeft_AfterActivation: Int,
                             val MatA_row_slice_cnt: Int,
                             val MatB_col_slice_cnt: Int)

  def generateRandomCoreInstruction_CC2Wrapper(
                                                UID: Int = 0,
                                                matrixOperation: MatrixOperation_TypeDef.E = MatrixOperation_TypeDef.MatMul, // 枚举
                                                shiftLeft_AfterMatrixOperation : Int = 0,
                                                doTranspose: Boolean = false,
                                                activationFunction: Activation_TypeDef.E = Activation_TypeDef.None,  // 可默认0
                                                shiftLeft_AfterActivation: Int = 0, // 可默认
                                                MatA_row_slice_cnt: Int = 0, // 视需求
                                                MatB_col_slice_cnt: Int = 0
                                              ): CoreInstruction = {
    CoreInstruction(
      UID, // 枚举
      matrixOperation,
      shiftLeft_AfterMatrixOperation,
      doTranspose,
      activationFunction,  // 可默认0
      shiftLeft_AfterActivation, // 可默认0
      MatA_row_slice_cnt, // 视需求
      MatB_col_slice_cnt
    )
  }
  def rotateMatrixCounterClockwise(matrix: Array[Array[Int]]): Array[Array[Int]] = {
    val rows = matrix.length
    val cols = matrix(0).length
    val rotated = Array.ofDim[Int](cols, rows) // 新的矩阵是 cols x rows
    for (i <- 0 until rows) {for (j <- 0 until cols) {rotated(cols - j - 1)(i) = matrix(i)(j)}}
    rotated}

  def elementWiseBinaryOpWithShiftAndSaturation(A: Array[Array[Int]], B: Array[Array[Int]], op: (Int, Int) => Int, shiftAmount: Int, satBits: Int): Array[Array[Int]] = {
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
  def elementWiseMultiplyMatrixWithShiftAndSaturation(A: Array[Array[Int]], B: Array[Array[Int]], shiftAmount: Int, satBits: Int): Array[Array[Int]] = {
    elementWiseBinaryOpWithShiftAndSaturation(A, B, _ * _, shiftAmount, satBits)}
  def elementWiseAdditionMatrixWithShiftAndSaturation(A: Array[Array[Int]], B: Array[Array[Int]], shiftAmount: Int, satBits: Int): Array[Array[Int]] = {
    elementWiseBinaryOpWithShiftAndSaturation(A, B, _ + _, shiftAmount, satBits)}
  def elementWiseMaximumMatrixWithShiftAndSaturation(A: Array[Array[Int]], B: Array[Array[Int]], shiftAmount: Int, satBits: Int): Array[Array[Int]] = {
    elementWiseBinaryOpWithShiftAndSaturation(A, B, _ max _, shiftAmount, satBits)}
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
  def multiplyMatricesWithShiftSaturation(matrixA: Array[Array[Int]], matrixB: Array[Array[Int]], shiftAmount: Int, satBits: Int): Array[Array[Int]] = {
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
  def printMatrix(matrix: Array[Array[Int]]): Unit = {
    matrix.foreach(row => println(row.mkString("\t")))
    println()
  }

  // 仿真 Case 队列
  val matrixA_queue = mutable.Queue[Array[Array[Int]]]()
  val matrixB_queue = mutable.Queue[Array[Array[Int]]]()
  val coreInstruction_queue = mutable.Queue[CoreInstruction]()
  val ref_queue = mutable.Queue[Array[Array[Int]]]()
  val ref_coreInstruction_queue = mutable.Queue[CoreInstruction]()
  val matrixOperations = Array(MatrixOperation_TypeDef.MatMul, MatrixOperation_TypeDef.ElementAdd, MatrixOperation_TypeDef.ElementMul, MatrixOperation_TypeDef.ElementMax)
  val activationFunctions = Array(Activation_TypeDef.None, Activation_TypeDef.Relu, Activation_TypeDef.Exp, Activation_TypeDef.Log, Activation_TypeDef.Softplus)

  // 准备仿真 Case
  val random = new Random()
  val global_seed = 1234//1334
  random.setSeed(global_seed)
  for (matrix_idx <- 0 until matrix_num) {
    // -------------------------------
    // 生成测试数据
    // -------------------------------
    var m_A = generateRandomMatrix(rowsA, colsA,random.nextInt(20))
    var m_B = generateRandomMatrix(rowsB, colsB,random.nextInt(20))
    var matrixOperation = matrixOperations(random.nextInt(matrixOperations.length))
    var activationFunction =activationFunctions(random.nextInt(activationFunctions.length))
    var shiftLeft_AfterMatrixOperation = generateRandomShiftAmount(shiftAmount = 4, seed = random.nextInt(20))
    var shiftLeft_AfterActivation = generateRandomShiftAmount(shiftAmount = 4, seed = random.nextInt(20))
    var doTranspose = random.nextBoolean()
    var UID = random.nextInt(matrix_num)

    var coreInstruction = generateRandomCoreInstruction_CC2Wrapper(
      UID = UID,
      matrixOperation = matrixOperation,//matrixOperation,
      shiftLeft_AfterMatrixOperation = shiftLeft_AfterMatrixOperation,//shiftLeft_AfterMatrixOperation,
      doTranspose = doTranspose,//doTranspose,
      activationFunction = activationFunction,
      shiftLeft_AfterActivation = shiftLeft_AfterActivation,//shiftLeft_AfterActivation,
      MatA_row_slice_cnt = 0,
      MatB_col_slice_cnt = 0
    )
    // -------------------------------
    // 准备测试激励与参考数据队列
    // -------------------------------
    matrixA_queue.enqueue(m_A)
    matrixB_queue.enqueue(m_B)
    coreInstruction_queue.enqueue(coreInstruction)
    ref_coreInstruction_queue.enqueue(coreInstruction)

    val refResult = matrixOperation match {
      case MatrixOperation_TypeDef.MatMul =>
        val matmulRes = multiplyMatricesWithShiftSaturation(m_A, m_B, shiftLeft_AfterMatrixOperation, cfg.out_MatZ_element_Width)
        if(doTranspose) matmulRes.transpose else matmulRes
      case MatrixOperation_TypeDef.ElementMul =>
        m_B = rotateMatrixCounterClockwise(m_B)
        val ewRes = elementWiseMultiplyMatrixWithShiftAndSaturation(m_A, m_B, shiftLeft_AfterMatrixOperation, cfg.out_MatZ_element_Width)
        if(doTranspose) ewRes.transpose else ewRes
      case MatrixOperation_TypeDef.ElementAdd =>
        m_B = rotateMatrixCounterClockwise(m_B)
        val ewRes = elementWiseAdditionMatrixWithShiftAndSaturation(m_A, m_B, shiftLeft_AfterMatrixOperation, cfg.out_MatZ_element_Width)
        if(doTranspose) ewRes.transpose else ewRes
      case MatrixOperation_TypeDef.ElementMax =>
        m_B = rotateMatrixCounterClockwise(m_B)
        val ewRes = elementWiseMaximumMatrixWithShiftAndSaturation(m_A, m_B, shiftLeft_AfterMatrixOperation, cfg.out_MatZ_element_Width)
        if(doTranspose) ewRes.transpose else ewRes
    }
    ref_queue.enqueue(refResult)
  }

  // 开始仿真
  Sim_compiled.doSim("simple test",seed = 2344) { dut =>
    val scoreboard = new ScoreboardInOrder_matrix()
    val scoreboard1 = new ScoreboardInOrder_Bigint()

    var StreamDriver_sending_period = 0
    var reset_done=false

    // -------------------------------
    // 生成时钟激励
    // -------------------------------
    var do_random_clk = false
    if(do_random_clk){
      val clocksThread = fork {
        dut.io.out_MatZ.ready #= false
        dut.clk_in.fallingEdge()
        dut.clk_out.fallingEdge()
        dut.clk_core.fallingEdge()
        dut.clk_in.deassertReset()
        dut.clk_out.deassertReset()
        dut.clk_core.deassertReset()
        println("de clk reset")
        sleep(0)
        dut.clk_in.assertReset()
        dut.clk_out.assertReset()
        dut.clk_core.assertReset()
        println("clk reset")
        sleep(10)
        dut.clk_in.deassertReset()
        dut.clk_out.deassertReset()
        dut.clk_core.deassertReset()
        sleep(1)
        reset_done=true

        while (true) {
          var rand = new Random().nextInt(11) // 假设我们想要生成0到10之间的随机数
          val direction = (rand%10) match {
            case x if (x  == 0) => dut.clk_in.clockToggle()
            case x if (x  == 1) => dut.clk_in.clockToggle()
            case x if (x  == 2) => dut.clk_in.clockToggle()
            case x if (x  == 3) => dut.clk_out.clockToggle()
            case x if (x  == 4) => dut.clk_out.clockToggle()
            case x if (x  == 5) => dut.clk_out.clockToggle()
            case x if (x  == 6) => dut.clk_core.clockToggle()
            case _                 =>
          }
          sleep(1)
        }
      }
    } else {
      val clocksThreadWithFixedFreq = fork {
        // 设置各个时钟周期（以仿真时间为单位，例如：10ns 表示 100MHz）
        val clk_in_period = 5  // 200MHz
        val clk_out_period = 5 // 200MHz//至少为core频率的四倍
        val clk_core_period = 14 // 50MHz

        dut.io.out_MatZ.ready #= false
        dut.clk_in.fallingEdge()
        dut.clk_out.fallingEdge()
        dut.clk_core.fallingEdge()
        dut.clk_in.deassertReset()
        dut.clk_out.deassertReset()
        dut.clk_core.deassertReset()
        sleep(0)
        // Do the resets.
        dut.clk_in.assertReset()
        dut.clk_out.assertReset()
        dut.clk_core.assertReset()
        sleep(10)
        dut.clk_in.deassertReset()
        dut.clk_out.deassertReset()
        dut.clk_core.deassertReset()
        sleep(1)
        reset_done=true

        // 启动三个时钟
        dut.clk_in.forkStimulus(clk_in_period) // 启动 clk_in，周期为 10ns
        dut.clk_out.forkStimulus(clk_out_period)
        dut.clk_core.forkStimulus(clk_core_period)
      }
    }

    // -------------------------------
    // 输入数据初始化
    // -------------------------------
    fork{
      dut.io.in_Mats.payload.CoreInstruction.Collector_Instruction.UID #= 0
      dut.io.in_Mats.payload.CoreInstruction.SystolicArray2D_CC_Instruction.matrixOperation #= MatrixOperation_TypeDef.MatMul
      dut.io.in_Mats.payload.CoreInstruction.SystolicArray2D_CC_Instruction.shiftLeft_AfterMatrixOperation #= 0
      dut.io.in_Mats.payload.CoreInstruction.SystolicArray2D_CC_Instruction.doTranspose #= false
      dut.io.in_Mats.payload.CoreInstruction.Activation_Instruction.activationFunction #= Activation_TypeDef.None
      dut.io.in_Mats.payload.CoreInstruction.Collector_Instruction.MatA_row_slice_cnt #= 0
      dut.io.in_Mats.payload.CoreInstruction.Collector_Instruction.MatB_col_slice_cnt #= 0
    }

    // -------------------------------
    // 开始仿真
    // -------------------------------
    var matrixA_sending = matrixA_queue.dequeue()
    var matrixB_sending = matrixB_queue.dequeue()
    var coreInstruction_sending = coreInstruction_queue.dequeue()
    dut.io.in_Mats.valid #= true
    StreamDriver(dut.io.in_Mats, dut.DUT.clk_in) { payload =>
      payload.CoreInstruction.Collector_Instruction.UID #= coreInstruction_sending.UID
      payload.CoreInstruction.SystolicArray2D_CC_Instruction.matrixOperation #= coreInstruction_sending.matrixOperation
      payload.CoreInstruction.SystolicArray2D_CC_Instruction.shiftLeft_AfterMatrixOperation #= coreInstruction_sending.shiftLeft_AfterActivation
      payload.CoreInstruction.SystolicArray2D_CC_Instruction.doTranspose #= coreInstruction_sending.doTranspose
      payload.CoreInstruction.Activation_Instruction.activationFunction #= coreInstruction_sending.activationFunction
      payload.CoreInstruction.Activation_Instruction.shiftLeft_AfterActivation #= coreInstruction_sending.shiftLeft_AfterActivation
      payload.CoreInstruction.Collector_Instruction.MatA_row_slice_cnt #= coreInstruction_sending.MatA_row_slice_cnt
      payload.CoreInstruction.Collector_Instruction.MatB_col_slice_cnt #= coreInstruction_sending.MatB_col_slice_cnt
      if (coreInstruction_sending.matrixOperation != MatrixOperation_TypeDef.MatMul){
        mult_dim = rowsA
      } else {
        mult_dim = matmul_mult_dim
      }
      //传输矩阵A
      for (row_index <- 0 until cfg.in_MatA_row_num) {
        if (StreamDriver_sending_period <= mult_dim - 1) {
          payload.A(row_index) #= matrixA_sending(row_index)(StreamDriver_sending_period)
        } else {
          payload.A(row_index) #= 0
        }
      }
      //传输矩阵B
      for (col_index <- 0 until cfg.in_MatB_col_num) {
        if (StreamDriver_sending_period <= mult_dim - 1) {
          payload.B(col_index) #= matrixB_sending(StreamDriver_sending_period)(col_index)
        } else {
          payload.B(col_index) #= 0
        }
      }
      //传输Final信号
      if (StreamDriver_sending_period == mult_dim - 1) {
        payload.Final #= true
      } else {
        payload.Final #= false
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
        if (coreInstruction_sending.matrixOperation == MatrixOperation_TypeDef.MatMul)
          //printMatrix(multiplyMatrices(matrixA_sending, matrixB_sending))
          printMatrix(multiplyMatricesWithShiftSaturation(matrixA_sending, matrixB_sending,shiftAmount= coreInstruction_sending.shiftLeft_AfterActivation, satBits = cfg.out_MatZ_element_Width))
        else if (coreInstruction_sending.matrixOperation == MatrixOperation_TypeDef.ElementMul)
          //printMatrix(elementWiseMultiplyMatrix(matrixA_sending, matrixB_sending))
          printMatrix(elementWiseMultiplyMatrixWithShiftAndSaturation(matrixA_sending, matrixB_sending,shiftAmount= coreInstruction_sending.shiftLeft_AfterActivation, satBits = cfg.out_MatZ_element_Width))
        else if (coreInstruction_sending.matrixOperation == MatrixOperation_TypeDef.ElementAdd)
          //printMatrix(elementWiseAdditionMatrix(matrixA_sending, matrixB_sending))
          printMatrix(elementWiseAdditionMatrixWithShiftAndSaturation(matrixA_sending, matrixB_sending,shiftAmount= coreInstruction_sending.shiftLeft_AfterActivation, satBits = cfg.out_MatZ_element_Width))
        else if (coreInstruction_sending.matrixOperation == MatrixOperation_TypeDef.ElementMax)
          printMatrix(elementWiseMaximumMatrixWithShiftAndSaturation(matrixA_sending, matrixB_sending,shiftAmount= coreInstruction_sending.shiftLeft_AfterActivation, satBits = cfg.out_MatZ_element_Width))
        else{
          println("Invalid mode.")
          assert(false)
        }
        matrixA_sending = matrixA_queue.dequeue()
        matrixB_sending = matrixB_queue.dequeue()
        coreInstruction_sending = coreInstruction_queue.dequeue()
      } else { StreamDriver_sending_period = StreamDriver_sending_period + 1}
      true
    }
    StreamReadyRandomizer(dut.io.out_MatZ, dut.clk_out)
    StreamReadyRandomizer(dut.io.out_coreInstruction_AfterMatrixOperation, dut.clk_out)
    var Dut_push_counter = 0
    var Ref_push_counter = 0

    StreamMonitor(dut.io.out_MatZ, dut.clk_out) { payload =>
      var dut_result = Array.fill(cfg.in_MatA_row_num) { Array.fill(cfg.in_MatB_col_num) { 0 } }
      if(reset_done) {
        if (dut.io.out_MatZ.valid.toBoolean && dut.io.out_MatZ.ready.toBoolean) {
          // 获取输出矩阵
          for (row_index <- 0 until cfg.in_MatA_row_num) {
            for (col_index <- 0 until cfg.in_MatB_col_num) {
              dut_result(row_index)(col_index) = payload(row_index)(col_index).toInt
            }
          }
          println(s"dut_result${Dut_push_counter}:")
          printMatrix(dut_result)
          scoreboard.pushDut(dut_result)
          Dut_push_counter = Dut_push_counter + 1
          println(s"ref_result${Ref_push_counter}:")
          var resultMatrix = ref_queue.dequeue()
          printMatrix(resultMatrix)
          scoreboard.pushRef(resultMatrix)
          Ref_push_counter = Ref_push_counter + 1
        }
      }
    }
    StreamMonitor(dut.io.out_coreInstruction_AfterMatrixOperation, dut.clk_in) { payload =>
      var matrixOperation: String = "None"
      var dut_UID = 0
      if(reset_done) {
        if (dut.io.out_coreInstruction_AfterMatrixOperation.valid.toBoolean && dut.io.out_coreInstruction_AfterMatrixOperation.ready.toBoolean
          && dut.io.out_MatZ.ready.toBoolean && dut.io.out_MatZ.valid.toBoolean) {
          dut_UID = payload.Collector_Instruction.UID.toInt
          println(s"dut_UID: ${dut_UID}")
          var ref_Instr = ref_coreInstruction_queue.dequeue()
          var ref_UID = ref_Instr.UID
          println(s"ref_UID: ${ref_UID}")
          assert(ref_UID == dut_UID)
        }
      }
    }



    StreamMonitor(dut.io.in_Mats, dut.clk_in) { payload =>
    {
    }
    }

    dut.clk_out.waitActiveEdgeWhere(scoreboard.matches == 80)
    //sleep(100000)
    simSuccess()
  }
}

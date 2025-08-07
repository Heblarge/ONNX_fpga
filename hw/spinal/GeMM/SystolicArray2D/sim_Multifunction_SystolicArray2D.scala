package GeMM.SystolicArray2D
import scala.util.Random
import scala.collection.mutable

/**
 * 这个模块模拟SystolicArray2D计算行为
 */

case class opmode_sim(
                   do_PostTranspose: Boolean,
                   do_MatMul: Boolean,
                   do_ElementWiseMul: Boolean,
                   do_ElementWiseAdd: Boolean
                 )


object SystolicArray2D_SoftwareReference {

  def rotateMatrixCounterClockwise(matrix: Array[Array[Int]]): Array[Array[Int]] = {
    val rows = matrix.length
    val cols = matrix(0).length
    val rotated = Array.ofDim[Int](cols, rows)
    for (i <- 0 until rows) {
      for (j <- 0 until cols) {
        rotated(cols - j - 1)(i) = matrix(i)(j)
      }
    }
    rotated
  }

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

  def elementWiseAdditionMatrix(A: Array[Array[Int]], B: Array[Array[Int]]): Array[Array[Int]] = {
    val n = A.length
    val result = Array.ofDim[Int](n, n)
    for (i <- 0 until n) {
      for (j <- 0 until n) {
        result(i)(j) = A(i)(j) + B(i)(j)
      }
    }
    result
  }

  def multiplyMatrices(matrixA: Array[Array[Int]], matrixB: Array[Array[Int]]): Array[Array[Int]] = {
    val rowsA = matrixA.length
    val colsA = matrixA(0).length
    val rowsB = matrixB.length
    val colsB = matrixB(0).length
    require(colsA == rowsB, "Matrix A's columns must match Matrix B's rows")

    val result = Array.ofDim[Int](rowsA, colsB)
    for (i <- 0 until rowsA) {
      for (j <- 0 until colsB) {
        result(i)(j) = (0 until colsA).map(k => matrixA(i)(k) * matrixB(k)(j)).sum
      }
    }
    result
  }

  def compute(matrixA: Array[Array[Int]], matrixB: Array[Array[Int]], mode: opmode_sim): Array[Array[Int]] = {
    //Input Validation Check
    if(matrixA.length != matrixB(0).length){
      throw new IllegalArgumentException(s"Matrix dimensions NOT match: rowsA (${matrixA.length}) must be == colsB (${matrixB(0).length})")
    }
    if(matrixA(0).length != matrixB.length){
      throw new IllegalArgumentException(s"Matrix dimensions NOT match: colsA (${matrixA(0).length}) must be == rowsB (${matrixB.length})")
    }
    if (matrixA.length > matrixA(0).length) {
      throw new IllegalArgumentException(s"Invalid matrix dimensions: rowsA (${matrixA.length}) must be <= colsA (${matrixA(0).length})")
    }
    if(matrixB(0).length > matrixB.length) {
      throw new IllegalArgumentException(s"Invalid matrix dimensions: colsB (${matrixB(0).length}) must be <= rowsB (${matrixB.length})")
    }
    //Computation
    var result: Array[Array[Int]] = Array.ofDim[Int](matrixA.length, matrixB(0).length)
    if (mode.do_MatMul) {
      result = multiplyMatrices(matrixA, matrixB)
    } else if (mode.do_ElementWiseMul) {
      result = elementWiseMultiplyMatrix(matrixA, matrixB) // 这里B已经在外部旋转
    } else if (mode.do_ElementWiseAdd) {
      result = elementWiseAdditionMatrix(matrixA, matrixB) // 这里B已经在外部旋转
    } else {
      throw new IllegalStateException("Invalid mode: At least one operation must be selected.")
    }
    if (mode.do_PostTranspose) {
      result = result.transpose
    }
    result
  }
}

object SystolicArraySoftwareSimulator {
  val matrix_num = 10
  val matmul_mult_dim = 8

  val rowsA = 4
  val colsA = matmul_mult_dim
  val rowsB = colsA
  val colsB = 4

  def generateRandomMatrix(rows: Int, cols: Int, seed: Int): Array[Array[Int]] = {
    val rand = new Random()
    rand.setSeed(seed)
    Array.fill(rows, cols)(rand.nextInt(16))
  }

  def generateRandomMode(): opmode_sim = {
    val rand = new Random()
    val do_PostTranspose = rand.nextBoolean()
    val do_MatMul = rand.nextBoolean()
    val do_ElementWiseMul = if (!do_MatMul) rand.nextBoolean() else false
    val do_ElementWiseAdd = if (!do_MatMul && !do_ElementWiseMul) true else false
    opmode_sim(
      do_PostTranspose = do_PostTranspose,
      do_MatMul = do_MatMul,
      do_ElementWiseMul = do_ElementWiseMul,
      do_ElementWiseAdd = do_ElementWiseAdd
    )
  }

  def rotateMatrixCounterClockwise(matrix: Array[Array[Int]]): Array[Array[Int]] = {
    val rows = matrix.length
    val cols = matrix(0).length
    val rotated = Array.ofDim[Int](cols, rows)
    for (i <- 0 until rows) {
      for (j <- 0 until cols) {
        rotated(cols - j - 1)(i) = matrix(i)(j)
      }
    }
    rotated
  }

  def printMatrix(matrix: Array[Array[Int]]): Unit = {
    matrix.foreach(row => println(row.mkString("\t")))
    println()
  }

  def main(args: Array[String]): Unit = {
    val matrixA_queue = mutable.Queue[Array[Array[Int]]]()
    val matrixB_queue = mutable.Queue[Array[Array[Int]]]()
    val mode_queue = mutable.Queue[opmode_sim]()
    val ref_queue = mutable.Queue[Array[Array[Int]]]()

    val random = new Random()
    val global_seed = 1234
    random.setSeed(global_seed)

    for (_ <- 0 until matrix_num) {
      val mode = generateRandomMode()

      // **根据 mode 选择矩阵尺寸**
      val (m_A, m_B) = if (mode.do_ElementWiseMul || mode.do_ElementWiseAdd) {
        // 逐元素运算：生成方阵
        val square_size = Math.min(rowsA, Math.min(colsA, Math.min(rowsB, colsB)))
        val A = generateRandomMatrix(square_size, square_size, random.nextInt(20))
        var B = generateRandomMatrix(square_size, square_size, random.nextInt(20))
        B = rotateMatrixCounterClockwise(B) // **逐元素操作需要旋转 B**
        (A, B)
      } else {
        // 矩阵乘法：使用完整尺寸
        val A = generateRandomMatrix(rowsA, colsA, random.nextInt(20))
        val B = generateRandomMatrix(rowsB, colsB, random.nextInt(20))
        (A, B)
      }

      matrixA_queue.enqueue(m_A)
      matrixB_queue.enqueue(m_B)
      mode_queue.enqueue(mode)

      // **调用 SystolicArray2D_SoftwareReference 计算**
      val result = SystolicArray2D_SoftwareReference.compute(m_A, m_B, mode)

      ref_queue.enqueue(result)
    }

    while (matrixA_queue.nonEmpty) {
      val matrixA = matrixA_queue.dequeue()
      val matrixB = matrixB_queue.dequeue()
      val mode = mode_queue.dequeue()
      val ref_result = ref_queue.dequeue()

      println("Matrix A:")
      printMatrix(matrixA)
      println("Matrix B:")
      printMatrix(matrixB)
      println(s"Mode: $mode")
      println("Expected Result:")
      printMatrix(ref_result)
    }
  }
}
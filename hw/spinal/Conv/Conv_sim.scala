package Conv
import scala.util.Random

object ConvSoftwaveSimulator {
  val random = new Random
  val seed = 114514
  random.setSeed(seed)

  def generateRandomVector(w: Int): Array[Int] = {
    Array.fill(w)(random.nextInt(10))
  }

  def generateRandomMatrix(rows: Int, cols: Int): Array[Array[Int]] = {
    Array.fill(rows, cols)(random.nextInt(10))
  }

  def generateRandomTensor(d1: Int, d2: Int, d3: Int): Array[Array[Array[Int]]] = {
    Array.fill(d1, d2, d3)(random.nextInt(10))
  }

  def printVector(vector: Array[Int]): Unit = {
    println(vector.mkString("\t"))
    println()
  }

  def printMatrix(matrix: Array[Array[Int]]): Unit = {
    matrix.foreach(row => println(row.mkString("\t")))
    println()
  }

  def printTensor(tensor: Array[Array[Array[Int]]]): Unit = {
    tensor.foreach(matrix => printMatrix(matrix))
    println()
  }

  def compareMatrix(matrixA: Array[Array[Int]], matrixB: Array[Array[Int]]): Boolean = {
    matrixA.zip(matrixB).forall { case (a, b) => a.sameElements(b) }
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

  def multiplyMatricesSlice(matrixA: Array[Array[Int]], matrixB: Array[Array[Int]]): Array[Array[Int]] = {
    val in_MatA_row_num = 2
    val in_Length_Max = 3
    val in_MatB_col_num = 4
    val rowsA = matrixA.length
    val colsA = matrixA(0).length
    val rowsB = matrixB.length
    val colsB = matrixB(0).length
    val rowsA_slice = in_MatA_row_num
    val colsA_slice = in_Length_Max
    val rowsB_slice = in_Length_Max
    val colsB_slice = in_MatB_col_num
    val rowsA_slice_num = rowsA / rowsA_slice
    val colsA_slice_num = colsA / colsA_slice
    val rowsB_slice_num = rowsB / rowsB_slice
    val colsB_slice_num = colsB / colsB_slice
    require(colsA_slice_num == rowsB_slice_num, "Matrix A's columns must match Matrix B's rows")
    require(rowsA % rowsA_slice == 0, "Matrix slice error")
    require(colsA % colsA_slice == 0, "Matrix slice error")
    require(rowsB % rowsB_slice == 0, "Matrix slice error")
    require(colsB % colsB_slice == 0, "Matrix slice error")

    val result = Array.fill(rowsA, colsB)(0)
    for (i <- 0 until rowsA_slice_num) {
      for (j <- 0 until colsB_slice_num) {
        for (k <- 0 until colsA_slice_num) {
          val matrixA_slice = Array.ofDim[Int](rowsA_slice, colsA_slice)
          for (m <- 0 until rowsA_slice) {
            for (n <- 0 until colsA_slice) {
              matrixA_slice(m)(n) = matrixA(i * rowsA_slice + m)(k * colsA_slice + n)
            }
          }
          val matrixB_slice = Array.ofDim[Int](rowsB_slice, colsB_slice)
          for (m <- 0 until rowsB_slice) {
            for (n <- 0 until colsB_slice) {
              matrixB_slice(m)(n) = matrixB(k * rowsB_slice + m)(j * colsB_slice + n)
            }
          }
          val result_slice = multiplyMatrices(matrixA_slice, matrixB_slice)
          for (m <- 0 until rowsA_slice) {
            for (n <- 0 until colsB_slice) {
              result(i * rowsA_slice + m)(j * colsB_slice + n) += result_slice(m)(n)
            }
          }
        }
      }
    }
    result
  }

  def multiplyMatricesSliceCheck() = {
    val rowsA = 10
    val colsA = 18
    val rowsB = 18
    val colsB = 28
    val matrixA = generateRandomMatrix(rowsA, colsA)
    val matrixB = generateRandomMatrix(rowsB, colsB)
    val ref = multiplyMatrices(matrixA, matrixB)
    val result = multiplyMatricesSlice(matrixA, matrixB)
    println("matrixA:")
    printMatrix(matrixA)
    println("matrixB:")
    printMatrix(matrixB)
    println("ref:")
    printMatrix(ref)
    println("result:")
    printMatrix(result)
    require(compareMatrix(ref, result),"multiplyMatricesSliceCheck error")
  }

  def convNative(
      tensorX: Array[Array[Int]],
      tensorW: Array[Array[Array[Int]]],
      vectorB: Array[Int]
  ): Array[Array[Int]] = {
    val c = tensorX.length
    val w = tensorX(0).length
    val m = tensorW.length
    require(c == tensorW(0).length, "tensor channel error")
    val kw = tensorW(0)(0).length
    require(kw % 2 == 1, "kernel shape error")
    require(m == vectorB.length, "bias shape error")

    val padw = kw / 2
    val tensorpad = Array.fill(c, w + 2 * padw)(0)
    for (i <- 0 until c) {
      for (j <- 0 until w) {
        tensorpad(i)(j + padw) = tensorX(i)(j)
      }
    }
    val result = Array.ofDim[Int](m, w)
    for (i <- 0 until m) {
      for (j <- 0 until w) {
        var sum = 0
        for (k <- 0 until c) {
          for (l <- 0 until kw) {
            sum += tensorpad(k)(j + l) * tensorW(i)(k)(l)
          }
        }
        result(i)(j) = sum + vectorB(i)
      }
    }
    result
  }

  def convFast(
      tensorX: Array[Array[Int]],
      tensorW: Array[Array[Array[Int]]],
      vectorB: Array[Int]
  ): Array[Array[Int]] = {
    val c = tensorX.length
    val w = tensorX(0).length
    val m = tensorW.length
    require(c == tensorW(0).length, "tensor channel error")
    val kw = 1
    require(kw == tensorW(0)(0).length, "kernel shape error")
    require(m == vectorB.length, "bias shape error")

    val matrixX_img2col = Array.ofDim[Int](w, c + 1)
    for (i <- 0 until w) {
      for (j <- 0 until c) {
        matrixX_img2col(i)(j) = tensorX(j)(i)
      }
      matrixX_img2col(i)(c) = 1;
    }
    val matrixW_img2col = Array.ofDim[Int](c + 1, m)
    for (i <- 0 until c) {
      for (j <- 0 until m) {
        matrixW_img2col(i)(j) = tensorW(j)(i)(0)
      }
    }
    for (j <- 0 until m) {
      matrixW_img2col(c)(j) = vectorB(j)
    }

    val result = multiplyMatricesSlice(matrixX_img2col, matrixW_img2col)
    result.transpose
  }

  def convFastCheck() = {
    val c = 17
    val w = 10
    val m = 28
    val kw = 1
    val tensorX = generateRandomMatrix(c, w)
    val tensorW = generateRandomTensor(m, c, kw)
    val vectorB = generateRandomVector(m)
    val ref = convNative(tensorX, tensorW, vectorB)
    val result = convFast(tensorX, tensorW, vectorB)
    println("tensorX")
    printMatrix(tensorX)
    println("tensorW")
    printTensor(tensorW)
    println("vectorB")
    printVector(vectorB)
    println("ref")
    printMatrix(ref)
    println("result:")
    printMatrix(result)
    require(compareMatrix(ref, result),"convFastCheck error")
  }

  def main(args: Array[String]): Unit = {
    multiplyMatricesSliceCheck()
    convFastCheck()
  }
}

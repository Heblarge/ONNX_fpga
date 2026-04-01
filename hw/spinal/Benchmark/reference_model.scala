package Benchmark

object BenchmarkReferenceModel {
  def exactMatmul(matrixA: Array[Array[Int]], matrixB: Array[Array[Int]]): Array[Array[BigInt]] = {
    val rowsA = matrixA.length
    val colsA = matrixA.head.length
    val rowsB = matrixB.length
    val colsB = matrixB.head.length
    require(colsA == rowsB, "Matrix A columns must match Matrix B rows")

    Array.tabulate(rowsA, colsB) { (row, col) =>
      var acc = BigInt(0)
      var idx = 0
      while (idx < colsA) {
        acc += BigInt(matrixA(row)(idx)) * BigInt(matrixB(idx)(col))
        idx += 1
      }
      acc
    }
  }

  def bigIntMatrixToDouble(matrix: Array[Array[BigInt]]): Array[Array[Double]] =
    Array.tabulate(matrix.length, matrix.head.length) { (row, col) =>
      matrix(row)(col).toDouble
    }

  def rescaleFixedOutput(matrix: Array[Array[Int]], postShift: Int): Array[Array[Double]] =
    Array.tabulate(matrix.length, matrix.head.length) { (row, col) =>
      rescaleFixedValue(matrix(row)(col), postShift)
    }

  def rescaleFixedValue(value: Int, postShift: Int): Double = {
    val raw = value.toDouble
    if (postShift > 0) raw * math.pow(2.0, postShift.toDouble)
    else if (postShift < 0) raw / math.pow(2.0, (-postShift).toDouble)
    else raw
  }
}

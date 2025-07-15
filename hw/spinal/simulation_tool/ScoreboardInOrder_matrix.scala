package spinal.lib.sim
import spinal.core.Data
import spinal.core.sim._
import scala.collection.mutable

/**
 * 该类扩展了ScoreboardInOrder类，用于比较两个二维整数数组序列的顺序和内容。
 * 它重写了比较逻辑，并添加了打印二维数组的方法。
 */
class ScoreboardInOrder_matrix extends ScoreboardInOrder[Array[Array[Int]]] {
  /**
   * 重写compare方法，用于比较两个二维整数数组是否相等。
   * 通过深度比较来判断两个数组是否完全一样。
   *
   * @param ref  参考的二维整数数组
   * @param dut  被测试的二维整数数组
   * @return     如果两个数组相等，则返回true；否则返回false。
   */
  //override def compare(ref: Array[Array[Int]], dut: Array[Array[Int]]): Boolean = !(ref.deep != dut.deep)

  override def compare(ref: Array[Array[Int]], dut: Array[Array[Int]]): Boolean =
    java.util.Arrays.deepEquals(ref.asInstanceOf[Array[AnyRef]], dut.asInstanceOf[Array[AnyRef]])

  /**
   * 打印二维整数数组，格式化输出为表格形式。
   *
   * @param matrix  需要打印的二维整数数组
   */
  def printMatrix(matrix: Array[Array[Int]]): Unit = {
    matrix.foreach(row => println(row.mkString("\t")))
    println()
  }
  
  /**
   * 重写check方法，用于检查参考序列和被测试序列的头部元素是否相等。
   * 如果不相等，则输出错误信息并标记模拟失败。
   */
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

object ScoreboardInOrder_matrix{
  def apply() : ScoreboardInOrder_matrix = new ScoreboardInOrder_matrix()
}
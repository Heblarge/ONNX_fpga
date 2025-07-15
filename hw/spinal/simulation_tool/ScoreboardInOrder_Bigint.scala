package spinal.lib.sim

import spinal.core.sim._
/**
 * 该类扩展了ScoreboardInOrder类，用于比较两个BigInt数组序列的顺序和内容。
 * 它重写了比较逻辑，并添加了打印BigInt数组的方法。
 */
class ScoreboardInOrder_Bigint extends ScoreboardInOrder[Array[BigInt]] {
  /**
   * 重写compare方法，用于比较两个BigInt数组是否相等。
   * 通过深度比较来判断两个数组是否完全一样。
   *
   * @param ref  参考的BigInt数组
   * @param dut  被测试的BigInt数组
   * @return     如果两个数组相等，则返回true；否则返回false。
   */
  override def compare(ref: Array[BigInt], dut: Array[BigInt]): Boolean = {
    // 使用BigInt的deepEquals来比较数组内容
    java.util.Arrays.deepEquals(ref.asInstanceOf[Array[AnyRef]], dut.asInstanceOf[Array[AnyRef]])
  }

  /**
   * 打印BigInt数组，格式化输出为表格形式。
   *
   * @param array  需要打印的BigInt数组
   */
  def printArray(array: Array[BigInt]): Unit = {
    array.foreach { elem =>
      println(elem.toString())  // 打印BigInt值
    }
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
        printArray(refHead)
        println("DUT :")
        printArray(dutHead)
        simFailure()  // 标记模拟失败
      }
      matches += 1  // 增加匹配计数
    }
  }
}

object ScoreboardInOrder_Bigint {
  def apply(): ScoreboardInOrder_Bigint = new ScoreboardInOrder_Bigint()
}

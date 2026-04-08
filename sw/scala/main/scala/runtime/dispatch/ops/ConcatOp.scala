package runtime.dispatch.ops

import runtime.bridge.Tensor
import runtime.engine._

/**
 * ConcatOp — 拼接硬件算子。
 *
 * 将多个输入张量沿指定轴拼接，自动对齐各输入的移位精度到统一的输出移位。
 */
class ConcatOp extends HWOp {
  override def opTypes: Set[String] = Set("Concat")

  override def execute(opType: String, node: GraphNode, inputIds: Seq[String],
                       graph: OnnxGraph, ctx: DispatchContext): Seq[String] = {
    val tos = node.fpgaOutShift.headOption.getOrElse(0L)
    val tis = node.fpgaInShift
    val axis = node.getInt("axis", 0L).toInt

    val rescaledInputs = inputIds.zipWithIndex.map { case (tid, idx) =>
      val tisI = if (idx < tis.length) tis(idx) else 0L
      val inputName = if (idx < node.inputs.length) node.inputs(idx) else ""
      val ss = ctx.getProducerOutputShift(graph, inputName, tisI)
      val rescaleAmount = (ss - tos).toInt

      if (rescaleAmount == 0) {
        ctx.mkRef(tid)
      } else {
        val shape = ctx.getShape(tid)
        val cols = shape.last.toInt
        val rows = (shape.product / cols).toInt
        val data = ctx.pullToLong2D(tid, rows, cols)
        for (i <- 0 until rows; j <- 0 until cols) {
          data(i)(j) = if (rescaleAmount > 0) data(i)(j) / (1L << rescaleAmount)
                       else data(i)(j) * (1L << (-rescaleAmount))
        }
        val rid = ctx.pushFromLong2D(data, rows, cols)
        val shaped = Tensor.op("Reshape", Seq(ctx.mkRef(rid)), Map("shape" -> shape.toSeq)).head
        shaped
      }
    }

    val result = Tensor.op("Concat", rescaledInputs, Map("axis" -> axis))
    Seq(result.head.id)
  }
}

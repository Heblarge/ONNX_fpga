package runtime.dispatch.ops

import Util._
import runtime.bridge.Tensor
import runtime.engine._

/**
 * NegOp — 取反硬件算子。
 *
 * 纯软件实现：取反 + 移位对齐，不需要 HW tiling。
 */
class NegOp extends HWOp {
  override def opTypes: Set[String] = Set("Neg")

  override def execute(opType: String, node: GraphNode, inputIds: Seq[String],
                       graph: OnnxGraph, ctx: DispatchContext): Seq[String] = {
    val xShape = ctx.getShape(inputIds(0))
    val cols = xShape.last.toInt
    val rows = (xShape.product / cols).toInt
    val xData = ctx.pullToLong2D(inputIds(0), rows, cols)

    val tis = node.fpgaInShift
    val tisX = if (tis.nonEmpty) tis(0) else 0L
    val ssX = ctx.getProducerOutputShift(graph, node.inputs(0), tisX)
    val tos = node.fpgaOutShift.headOption.getOrElse(ssX)
    val shiftAmount = (ssX - tos).toInt

    for (i <- 0 until rows; j <- 0 until cols) {
      xData(i)(j) = roundShiftRight(-xData(i)(j), shiftAmount)
    }
    val resultId = ctx.pushFromLong2D(xData, rows, cols)
    val shaped = Tensor.op("Reshape", Seq(ctx.mkRef(resultId)), Map("shape" -> xShape.toSeq)).head
    Seq(shaped.id)
  }
}

package runtime.dispatch.ops

import runtime.bridge.Tensor
import runtime.engine._

/**
 * GemmOp — 通用矩阵乘法硬件算子。
 *
 * 处理 Gemm（= MatMul + bias）和 GemmRelu（= Gemm + Relu）。
 * 内部通过 ctx.dispatchToOp("MatMul", ...) 复用 MatMulOp 的实现。
 */
class GemmOp extends HWOp {
  override def opTypes: Set[String] = Set("Gemm", "GemmRelu")

  override def execute(opType: String, node: GraphNode, inputIds: Seq[String],
                       graph: OnnxGraph, ctx: DispatchContext): Seq[String] = {
    val gemmResult = gemm(node, inputIds, graph, ctx)
    if (opType == "GemmRelu") {
      val result = Tensor.op("Relu", Seq(ctx.mkRef(gemmResult.head)), Map.empty).head
      Seq(result.id)
    } else gemmResult
  }

  private def gemm(node: GraphNode, inputIds: Seq[String],
                   graph: OnnxGraph, ctx: DispatchContext): Seq[String] = {
    val transA = node.getInt("transA", 0L) != 0L
    val transB = node.getInt("transB", 0L) != 0L

    var aId = inputIds(0)
    var bId = inputIds(1)
    if (transA) aId = Tensor.op("Transpose", Seq(ctx.mkRef(aId)), Map.empty).head.id
    if (transB) bId = Tensor.op("Transpose", Seq(ctx.mkRef(bId)), Map.empty).head.id

    // Delegate to MatMulOp
    val matMulResult = ctx.dispatchToOp("MatMul",
      node.copy(inputs = Array(node.inputs(0), node.inputs(1))),
      Seq(aId, bId), graph)

    if (inputIds.size > 2 && inputIds(2) != null) {
      val beta = node.getFloat("beta", 1.0f)
      val biasScaled = if (beta != 1.0f) {
        Tensor.op("Mul", Seq(ctx.mkRef(inputIds(2)),
          Tensor.scalar(beta.toDouble)), Map.empty).head
      } else ctx.mkRef(inputIds(2))
      val result = Tensor.op("Add", Seq(ctx.mkRef(matMulResult.head), biasScaled), Map.empty).head
      Seq(result.id)
    } else matMulResult
  }
}

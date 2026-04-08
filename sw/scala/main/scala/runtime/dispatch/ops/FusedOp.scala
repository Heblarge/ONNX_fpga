package runtime.dispatch.ops

import runtime.bridge.Tensor
import runtime.engine._

/**
 * FusedOp — 融合算子。
 *
 * 处理由多个基础算子组合而成的融合操作：
 *   - NegSoftplus = Softplus + Neg
 *   - AddExp = Add + Exp
 *   - AddLog = Add + Log
 *   - SubExp = Sub + Exp
 *
 * 内部通过 ctx.dispatchToOp 复用 ElementWiseOp / ActivationOp 的实现。
 */
class FusedOp extends HWOp {
  override def opTypes: Set[String] = Set("NegSoftplus", "AddExp", "AddLog", "SubExp")

  override def execute(opType: String, node: GraphNode, inputIds: Seq[String],
                       graph: OnnxGraph, ctx: DispatchContext): Seq[String] = {
    opType match {
      case "NegSoftplus" =>
        val ids = ctx.dispatchToOp("Softplus", node, inputIds, graph)
        val negResult = Tensor.op("Neg", Seq(ctx.mkRef(ids.head)), Map.empty)
        Seq(negResult.head.id)

      case "AddExp" =>
        addWithActivation(node, inputIds, graph, "exp", ctx)
      case "AddLog" =>
        addWithActivation(node, inputIds, graph, "log", ctx)
      case "SubExp" =>
        subWithActivation(node, inputIds, graph, "exp", ctx)
    }
  }

  private def addWithActivation(node: GraphNode, inputIds: Seq[String],
                                 graph: OnnxGraph, actFn: String,
                                 ctx: DispatchContext): Seq[String] = {
    val addResult = ctx.dispatchToOp("Add", node, inputIds, graph)
    val fakeActNode = node.copy(opType = actFn.capitalize)
    ctx.dispatchToOp(actFn.capitalize, fakeActNode, addResult, graph)
  }

  private def subWithActivation(node: GraphNode, inputIds: Seq[String],
                                 graph: OnnxGraph, actFn: String,
                                 ctx: DispatchContext): Seq[String] = {
    val subResult = ctx.dispatchToOp("Sub", node, inputIds, graph)
    val fakeActNode = node.copy(opType = actFn.capitalize)
    ctx.dispatchToOp(actFn.capitalize, fakeActNode, subResult, graph)
  }
}

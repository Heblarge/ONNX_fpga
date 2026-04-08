package runtime.dispatch.ops

import Util._
import runtime.bridge.{TorchBridge, Tensor}
import runtime.engine._

/**
 * ElementWiseOp — 逐元素运算硬件算子。
 *
 * 处理 Add、Sub、Max 三种逐元素操作。
 * 自动处理 broadcast、移位对齐、padding、分块 tiling。
 */
class ElementWiseOp extends HWOp {
  override def opTypes: Set[String] = Set("Add", "Sub", "Max")

  override def execute(opType: String, node: GraphNode, inputIds: Seq[String],
                       graph: OnnxGraph, ctx: DispatchContext): Seq[String] = {
    val opName = opType match {
      case "Add" => "elementadd"
      case "Sub" => "elementsub"
      case "Max" => "elementmax"
    }
    elementWise(node, inputIds, graph, opName, ctx)
  }

  private[ops] def elementWise(node: GraphNode, inputIds: Seq[String],
                                graph: OnnxGraph, opName: String,
                                ctx: DispatchContext): Seq[String] = {
    val aShape = ctx.getShape(inputIds(0))
    val bShape = ctx.getShape(inputIds(1))

    val tis = node.fpgaInShift
    val tisA = if (tis.nonEmpty) tis(0) else 0L
    val tisB = if (tis.size > 1) tis(1) else 0L
    val ssA = ctx.getProducerOutputShift(graph, node.inputs(0), tisA)
    val ssB = ctx.getProducerOutputShift(graph, node.inputs(1), tisB)
    val tos = node.fpgaOutShift.headOption.getOrElse(0L)

    val (aId, bId, outShape) = if (aShape.sameElements(bShape)) {
      (inputIds(0), inputIds(1), aShape)
    } else {
      val broadcastResult = TorchBridge.call("broadcast_tensors",
        Map("ids" -> Seq(inputIds(0), inputIds(1))))
        .asInstanceOf[List[Map[String, Any]]]
      val broadcastedA = broadcastResult(0)("id").toString
      val broadcastedB = broadcastResult(1)("id").toString
      val bcastShape = ctx.getShape(broadcastedA)
      (broadcastedA, broadcastedB, bcastShape)
    }

    val totalElements = outShape.product.toInt
    val cols = outShape.last.toInt
    val rows = totalElements / cols

    val aData = ctx.pullToLong2D(aId, rows, cols)
    val bData = ctx.pullToLong2D(bId, rows, cols)

    val isSub = opName == "elementsub"
    val hwOp = if (isSub) "elementadd" else opName

    val isMax = opName == "elementmax"
    val comparisonShift = if (isMax) math.max(tisA, tisB) else tisA
    val rsA = (ssA - comparisonShift).toInt
    val rsB = (ssB - (if (isMax) comparisonShift else tisB)).toInt

    val pr = ctx.ceilToMultiple(rows, ctx.HW_DIM_MULTIPLE)
    val pc = ctx.ceilToMultiple(cols, ctx.HW_DIM_MULTIPLE)
    val padA = Array.ofDim[Long](pr, pc)
    val padB = Array.ofDim[Long](pr, pc)
    for (i <- 0 until rows; j <- 0 until cols) {
      padA(i)(j) = roundShiftRight(aData(i)(j), rsA)
      val bRaw = if (isSub) -bData(i)(j) else bData(i)(j)
      padB(i)(j) = roundShiftRight(bRaw, rsB)
    }

    val shiftAfterOp = (comparisonShift - tos).toInt
    if (ctx.debugLimit > 0 && ctx.hwOpCount <= ctx.debugLimit) {
      println(s"  [elemWise] op=$opName isSub=$isSub isMax=$isMax ssA=$ssA ssB=$ssB tisA=$tisA tisB=$tisB tos=$tos compShift=$comparisonShift rsA=$rsA rsB=$rsB shiftAfterOp=$shiftAfterOp")
      println(s"  [elemWise] padA[0]=${padA(0).take(3).mkString(",")} padB[0]=${padB(0).take(3).mkString(",")}")
    }
    val hwResult = ctx.tiledElementOp(padA, padB, pr, pc, hwOp, shiftAfterOp, "none", 0, node.name)

    val unpadded = Array.ofDim[Long](rows, cols)
    for (i <- 0 until rows; j <- 0 until cols) unpadded(i)(j) = hwResult(i)(j)

    val resultId = ctx.pushFromLong2D(unpadded, rows, cols)
    val shaped = Tensor.op("Reshape", Seq(ctx.mkRef(resultId)), Map("shape" -> outShape.toSeq)).head
    Seq(shaped.id)
  }
}

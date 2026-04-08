package runtime.dispatch.ops

import Util._
import runtime.bridge.Tensor
import runtime.engine._

/**
 * MatMulOp — 矩阵乘法硬件算子。
 *
 * 支持 2D×2D、3D×2D、3D×3D（batch）矩阵乘法。
 * 自动进行移位对齐、padding 到 HW_DIM_MULTIPLE、分块 tiling。
 */
class MatMulOp extends HWOp {
  override def opTypes: Set[String] = Set("MatMul")

  override def execute(opType: String, node: GraphNode, inputIds: Seq[String],
                       graph: OnnxGraph, ctx: DispatchContext): Seq[String] = {
    val aShape = ctx.getShape(inputIds(0))
    val bShape = ctx.getShape(inputIds(1))

    val tis = node.fpgaInShift
    val tisA = if (tis.nonEmpty) tis(0) else 0L
    val tisB = if (tis.size > 1) tis(1) else 0L
    val ssA = ctx.getProducerOutputShift(graph, node.inputs(0), tisA)
    val ssB = ctx.getProducerOutputShift(graph, node.inputs(1), tisB)
    val tos = node.fpgaOutShift.headOption.getOrElse(0L)

    if (aShape.length == 2 && bShape.length == 2) {
      val resultId = matMul2D(inputIds(0), inputIds(1),
        aShape(0).toInt, aShape(1).toInt, bShape(1).toInt,
        ssA, ssB, tisA, tisB, tos, node.name, ctx)
      Seq(resultId)
    } else if (aShape.length == 3 && bShape.length == 2) {
      val batch = aShape(0)
      val m = aShape(1).toInt
      val k = aShape(2).toInt
      val n = bShape(1).toInt
      val aFlat = Tensor.op("Reshape", Seq(ctx.mkRef(inputIds(0))), Map("shape" -> Seq(batch * m, k))).head
      val r2d = matMul2D(aFlat.id, inputIds(1), (batch * m).toInt, k, n,
        ssA, ssB, tisA, tisB, tos, node.name, ctx)
      val result = Tensor.op("Reshape", Seq(ctx.mkRef(r2d)), Map("shape" -> Seq(batch, m, n))).head
      Seq(result.id)
    } else if (aShape.length == 3 && bShape.length == 3) {
      val batch = aShape(0)
      val m = aShape(1).toInt
      val k = aShape(2).toInt
      val n = bShape(2).toInt
      val aFlat = Tensor.op("Reshape", Seq(ctx.mkRef(inputIds(0))), Map("shape" -> Seq(batch * m, k))).head
      val bFlat = Tensor.op("Reshape", Seq(ctx.mkRef(inputIds(1))), Map("shape" -> Seq(batch * k, n))).head
      val batchSize = batch.toInt
      val sliceResults = (0 until batchSize).map { b =>
        val aSlice = Tensor.op("Slice", Seq(ctx.mkRef(aFlat.id)),
          Map("starts" -> Seq(b * m), "ends" -> Seq((b + 1) * m), "axes" -> Seq(0))).head
        val bSlice = Tensor.op("Slice", Seq(ctx.mkRef(bFlat.id)),
          Map("starts" -> Seq(b * k), "ends" -> Seq((b + 1) * k), "axes" -> Seq(0))).head
        matMul2D(aSlice.id, bSlice.id, m, k, n,
          ssA, ssB, tisA, tisB, tos, node.name, ctx)
      }
      val catInputs = sliceResults.map(id => ctx.mkRef(id))
      val catResult = Tensor.op("Concat", catInputs, Map("axis" -> 0)).head
      val result = Tensor.op("Reshape", Seq(catResult), Map("shape" -> Seq(batch, m, n))).head
      Seq(result.id)
    } else {
      throw new IllegalArgumentException(s"Unsupported MatMul ranks: A=${aShape.length}, B=${bShape.length}")
    }
  }

  private def matMul2D(aId: String, bId: String,
                       rowsA: Int, colsA: Int, colsB: Int,
                       ssA: Long, ssB: Long, tisA: Long, tisB: Long,
                       tos: Long, nodeName: String, ctx: DispatchContext): String = {
    val pr = ctx.ceilToMultiple(rowsA, ctx.HW_DIM_MULTIPLE)
    val pk = ctx.ceilToMultiple(colsA, ctx.HW_DIM_MULTIPLE)
    val pn = ctx.ceilToMultiple(colsB, ctx.HW_DIM_MULTIPLE)

    val aData = ctx.pullToLong2D(aId, rowsA, colsA)
    val bData = ctx.pullToLong2D(bId, colsA, colsB)

    val padA = Array.ofDim[Long](pr, pk)
    val padB = Array.ofDim[Long](pk, pn)
    for (i <- 0 until rowsA; j <- 0 until colsA) {
      val rsa = (ssA - tisA).toInt
      padA(i)(j) = roundShiftRight(aData(i)(j), rsa)
    }
    for (i <- 0 until colsA; j <- 0 until colsB) {
      val rsb = (ssB - tisB).toInt
      padB(i)(j) = roundShiftRight(bData(i)(j), rsb)
    }

    val shiftAmount = (tisA + tisB - tos).toInt
    val hwResult = ctx.tiledHWOp(padA, padB, pr, pk, pn, "matmul", shiftAmount, "none", 0, nodeName)

    val unpadded = Array.ofDim[Long](rowsA, colsB)
    for (i <- 0 until rowsA; j <- 0 until colsB) unpadded(i)(j) = hwResult(i)(j)

    ctx.pushFromLong2D(unpadded, rowsA, colsB)
  }
}

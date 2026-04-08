package runtime.dispatch.ops

import Accelerator.AcceleratorSimInterface
import runtime.bridge.Tensor
import runtime.engine._

/**
 * ActivationOp — 激活函数硬件算子。
 *
 * 处理 Relu、Exp、Log、Softplus 四种激活函数。
 * 输入先对齐到 HW fracWidth，经 HW 查表激活后再移位到输出精度。
 * 支持浮点旁路模式（_useFloatActivations）用于诊断。
 */
class ActivationOp extends HWOp {
  override def opTypes: Set[String] = Set("Relu", "Exp", "Log", "Softplus")

  override def execute(opType: String, node: GraphNode, inputIds: Seq[String],
                       graph: OnnxGraph, ctx: DispatchContext): Seq[String] = {
    activation(node, inputIds, graph, opType.toLowerCase, ctx)
  }

  private[ops] def activation(node: GraphNode, inputIds: Seq[String],
                               graph: OnnxGraph, actFn: String,
                               ctx: DispatchContext): Seq[String] = {
    val xShape = ctx.getShape(inputIds(0))
    val tis = node.fpgaInShift
    val tisX = if (tis.nonEmpty) tis(0) else 0L
    val ssX = ctx.getProducerOutputShift(graph, node.inputs(0), tisX)
    val tos = node.fpgaOutShift.headOption.getOrElse(0L)
    val sHW = ctx.hwFracWidth

    val cols = xShape.last.toInt
    val rows = (xShape.product / cols).toInt

    val xData = ctx.pullToLong2D(inputIds(0), rows, cols)
    val preShift = (ssX - sHW).toInt
    val postShift = (sHW - tos).toInt

    if (ctx.debugNodeNames.contains(node.name)) {
      val producerName = graph.producerMap.get(node.inputs(0)).map(_.name).getOrElse("N/A")
      println(s"  [activation] fn=$actFn ssX=$ssX tisX=$tisX tos=$tos sHW=$sHW preShift=$preShift postShift=$postShift")
      println(s"  [activation] producer='$producerName' producerOutShift=${graph.producerMap.get(node.inputs(0)).map(_.fpgaOutShift).getOrElse("N/A")}")
      var minRaw = Long.MaxValue; var maxRaw = Long.MinValue
      for (i <- 0 until rows; j <- 0 until cols) { if (xData(i)(j) < minRaw) minRaw = xData(i)(j); if (xData(i)(j) > maxRaw) maxRaw = xData(i)(j) }
      println(s"  [activation] raw input range: [$minRaw, $maxRaw]")
      var minShifted = Long.MaxValue; var maxShifted = Long.MinValue
      for (i <- 0 until rows; j <- 0 until cols) {
        val v = if (preShift < 0) xData(i)(j) << (-preShift) else xData(i)(j) >> preShift
        if (v < minShifted) minShifted = v; if (v > maxShifted) maxShifted = v
      }
      println(s"  [activation] shifted input range: [$minShifted, $maxShifted]")
    }

    val clampRange: Option[(Long, Long)] = None

    val pr = ctx.ceilToMultiple(rows, ctx.HW_DIM_MULTIPLE)
    val pc = ctx.ceilToMultiple(cols, ctx.HW_DIM_MULTIPLE)
    val padX = Array.ofDim[Long](pr, pc)
    val padDefault: Long = actFn match {
      case "log" => math.ceil(0.2 * (1L << sHW.toInt)).toLong
      case _     => 0L
    }
    var clampCount = 0
    for (i <- 0 until pr; j <- 0 until pc) {
      if (i < rows && j < cols) {
        var v = if (preShift < 0) xData(i)(j) << (-preShift) else xData(i)(j) >> preShift
        clampRange.foreach { case (lo, hi) =>
          if (v < lo || v > hi) clampCount += 1
          v = math.max(lo, math.min(hi, v))
        }
        padX(i)(j) = v
      } else {
        padX(i)(j) = padDefault
      }
    }
    if (clampCount > 0) {
      println(s"[CLAMP] HW#${ctx.hwOpCount} ${node.name} ($actFn): $clampCount/${rows*cols} elements clamped")
    }

    // Float activation bypass for diagnostics
    if (ctx.useFloatActivations && Set("exp", "log", "softplus").contains(actFn)) {
      val scale = math.pow(2.0, sHW.toDouble)
      val outScale = math.pow(2.0, tos.toDouble)
      val unpadded = Array.ofDim[Long](rows, cols)
      for (i <- 0 until rows; j <- 0 until cols) {
        val intVal = padX(i)(j)
        val floatVal = intVal.toDouble / scale
        val result = actFn match {
          case "exp"      => math.exp(floatVal)
          case "log"      => if (floatVal > 0) math.log(floatVal) else math.log(1e-30)
          case "softplus"  => math.log(1.0 + math.exp(floatVal))
        }
        unpadded(i)(j) = math.round(result * outScale)
        val maxVal = (1L << (AcceleratorSimInterface.acceleratorCfg.elementWidth - 1)) - 1
        val minVal = -(1L << (AcceleratorSimInterface.acceleratorCfg.elementWidth - 1))
        if (unpadded(i)(j) > maxVal) unpadded(i)(j) = maxVal
        if (unpadded(i)(j) < minVal) unpadded(i)(j) = minVal
      }
      val resultId = ctx.pushFromLong2D(unpadded, rows, cols)
      val shaped = Tensor.op("Reshape", Seq(ctx.mkRef(resultId)), Map("shape" -> xShape.toSeq)).head
      return Seq(shaped.id)
    }

    val padZ = Array.ofDim[Long](pr, pc)

    val hwResult = ctx.tiledElementOp(padX, padZ, pr, pc, "elementadd", 0, actFn, postShift, node.name)

    val unpadded = Array.ofDim[Long](rows, cols)
    for (i <- 0 until rows; j <- 0 until cols) unpadded(i)(j) = hwResult(i)(j)

    val resultId = ctx.pushFromLong2D(unpadded, rows, cols)
    val shaped = Tensor.op("Reshape", Seq(ctx.mkRef(resultId)), Map("shape" -> xShape.toSeq)).head
    Seq(shaped.id)
  }
}

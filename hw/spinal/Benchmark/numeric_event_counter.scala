package Benchmark

import FloatingPoint.FloatAlgo

object NumericEventCounter {
  case class NumericEventMetrics(
      shiftCount: Long,
      truncateCount: Long,
      saturationCount: Long,
      overflowCount: Long,
      roundingCount: Long
  ) {
    def +(that: NumericEventMetrics): NumericEventMetrics =
      NumericEventMetrics(
        shiftCount = this.shiftCount + that.shiftCount,
        truncateCount = this.truncateCount + that.truncateCount,
        saturationCount = this.saturationCount + that.saturationCount,
        overflowCount = this.overflowCount + that.overflowCount,
        roundingCount = this.roundingCount + that.roundingCount
      )
  }

  val empty: NumericEventMetrics = NumericEventMetrics(0, 0, 0, 0, 0)

  def fixed(rawAccumulator: Array[Array[BigInt]], shift: Int, outBits: Int): NumericEventMetrics = {
    val minVal = signedMin(outBits)
    val maxVal = signedMax(outBits)
    val boundedShift = shift.max(-outBits).min(outBits)

    rawAccumulator.iterator.flatMap(_.iterator).foldLeft(empty) { (acc, value) =>
      val shifted =
        if (boundedShift > 0) value >> boundedShift
        else if (boundedShift < 0) value << (-boundedShift)
        else value

      val shiftedBack =
        if (boundedShift > 0) shifted << boundedShift
        else if (boundedShift < 0) shifted >> (-boundedShift)
        else shifted

      acc + NumericEventMetrics(
        shiftCount = if (boundedShift != 0) 1 else 0,
        truncateCount = if (boundedShift > 0 && shiftedBack != value) 1 else 0,
        saturationCount = if (shifted > maxVal || shifted < minVal) 1 else 0,
        overflowCount = if (shifted > maxVal || shifted < minVal) 1 else 0,
        roundingCount = 0
      )
    }
  }

  def floating(
      sourceInputs: Seq[Double],
      quantizedInputs: Seq[Double],
      referenceOutputs: Seq[Double],
      quantizedReferenceOutputs: Seq[Double],
      algo: FloatAlgo
  ): NumericEventMetrics = {
    val inputMetrics = sourceInputs.zip(quantizedInputs).foldLeft(empty) { case (acc, (source, quantized)) =>
      acc + analyzeFloatValue(source, quantized, algo)
    }

    val outputMetrics = referenceOutputs.zip(quantizedReferenceOutputs).foldLeft(empty) { case (acc, (source, quantized)) =>
      acc + analyzeFloatValue(source, quantized, algo)
    }

    inputMetrics + outputMetrics
  }

  private def analyzeFloatValue(source: Double, quantized: Double, algo: FloatAlgo): NumericEventMetrics = {
    if (source.isNaN && quantized.isNaN) {
      empty
    } else if (source == quantized) {
      empty
    } else {
      val overflow = source.isInfinite || (!source.isNaN && math.abs(source) > algo.maxFiniteValue)
      NumericEventMetrics(
        shiftCount = 0,
        truncateCount = if (!overflow && !source.isNaN) 1 else 0,
        saturationCount = if (overflow) 1 else 0,
        overflowCount = if (overflow) 1 else 0,
        roundingCount = if (!overflow && !source.isNaN) 1 else 0
      )
    }
  }

  private def signedMin(bits: Int): BigInt = -(BigInt(1) << (bits - 1))

  private def signedMax(bits: Int): BigInt = (BigInt(1) << (bits - 1)) - 1
}

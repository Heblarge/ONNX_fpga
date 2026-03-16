package Benchmark

object PrecisionAnalyzer {
  case class PrecisionMetrics(
      sampleCount: Int,
      mae: Double,
      mse: Double,
      maxAbsError: Double,
      relativeError: Double,
      cosineSimilarity: Double
  )

  def analyze(reference: Seq[Double], dut: Seq[Double]): PrecisionMetrics = {
    require(reference.length == dut.length, "reference and dut must have the same length")

    if (reference.isEmpty) {
      PrecisionMetrics(0, 0.0, 0.0, 0.0, 0.0, 1.0)
    } else {
      val absErrors = reference.zip(dut).map { case (ref, got) => safeAbsError(ref, got) }
      val squaredErrors = absErrors.map(err => err * err)
      val mae = safeAverage(absErrors)
      val mse = safeAverage(squaredErrors)
      val maxAbsError = absErrors.max

      val relativeTerms = reference.zip(dut).map { case (ref, got) =>
        if (!ref.isFinite || !got.isFinite) {
          if (safeAbsError(ref, got) == 0.0) 0.0 else Double.PositiveInfinity
        } else if (ref == 0.0) {
          math.abs(got - ref)
        } else {
          math.abs(got - ref) / math.abs(ref)
        }
      }
      val relativeError = safeAverage(relativeTerms)

      val finitePairs = reference.zip(dut).filter { case (ref, got) => ref.isFinite && got.isFinite }
      val dot = finitePairs.map { case (ref, got) => ref * got }.sum
      val refNorm = math.sqrt(finitePairs.map { case (ref, _) => ref * ref }.sum)
      val dutNorm = math.sqrt(finitePairs.map { case (_, got) => got * got }.sum)
      val cosineSimilarity =
        if (absErrors.forall(_ == 0.0)) 1.0
        else if (finitePairs.isEmpty) 0.0
        else if (refNorm == 0.0 && dutNorm == 0.0) 1.0
        else if (refNorm == 0.0 || dutNorm == 0.0) 0.0
        else (dot / (refNorm * dutNorm)).max(-1.0).min(1.0)

      PrecisionMetrics(
        sampleCount = reference.length,
        mae = mae,
        mse = mse,
        maxAbsError = maxAbsError,
        relativeError = relativeError,
        cosineSimilarity = cosineSimilarity
      )
    }
  }

  private def safeAbsError(reference: Double, dut: Double): Double = {
    if (reference.isNaN && dut.isNaN) {
      0.0
    } else if (reference.isInfinite && dut.isInfinite && reference.sign == dut.sign) {
      0.0
    } else if (!reference.isFinite || !dut.isFinite) {
      Double.PositiveInfinity
    } else {
      math.abs(reference - dut)
    }
  }

  private def safeAverage(values: Seq[Double]): Double = {
    if (values.isEmpty) {
      0.0
    } else if (values.exists(_.isNaN)) {
      Double.NaN
    } else if (values.exists(_.isPosInfinity)) {
      Double.PositiveInfinity
    } else if (values.exists(_.isNegInfinity)) {
      Double.NegativeInfinity
    } else {
      values.sum / values.length.toDouble
    }
  }
}

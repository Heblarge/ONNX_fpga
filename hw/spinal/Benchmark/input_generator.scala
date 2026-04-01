package Benchmark

import scala.util.Random

object BenchmarkInputGenerator {
  sealed trait InputMode {
    def cliName: String
  }

  object InputMode {
    case object NormalDistribution extends InputMode {
      override val cliName: String = "normal"
    }

    case object WideDynamicRange extends InputMode {
      override val cliName: String = "wide"
    }

    case object MixedExtremeValues extends InputMode {
      override val cliName: String = "extreme"
    }

    val all: Seq[InputMode] = Seq(NormalDistribution, WideDynamicRange, MixedExtremeValues)

    def parse(text: String): InputMode = {
      text.trim.toLowerCase match {
        case "normal" | "normal_distribution" | "gaussian" =>
          NormalDistribution
        case "wide" | "wide_dynamic_range" | "dynamic" =>
          WideDynamicRange
        case "extreme" | "mixed_extreme_values" | "mixed" =>
          MixedExtremeValues
        case other =>
          throw new IllegalArgumentException(
            s"Unknown input mode '$other'. Expected one of: ${all.map(_.cliName).mkString(", ")}"
          )
      }
    }
  }

  case class TestCase(index: Int, matrixA: Array[Array[Int]], matrixB: Array[Array[Int]])

  def generateCases(
      caseCount: Int,
      rowsA: Int,
      colsA: Int,
      rowsB: Int,
      colsB: Int,
      mode: InputMode,
      bits: Int,
      seed: Int
  ): Seq[TestCase] = {
    require(caseCount > 0, "caseCount must be > 0")
    val random = new Random(seed)
    Seq.tabulate(caseCount) { index =>
      TestCase(
        index = index,
        matrixA = generateMatrix(rowsA, colsA, mode, bits, random),
        matrixB = generateMatrix(rowsB, colsB, mode, bits, random)
      )
    }
  }

  def generateMatrix(
      rows: Int,
      cols: Int,
      mode: InputMode,
      bits: Int,
      random: Random
  ): Array[Array[Int]] = {
    Array.tabulate(rows, cols) { (_, _) =>
      mode match {
        case InputMode.NormalDistribution =>
          generateNormalValue(bits, random)
        case InputMode.WideDynamicRange =>
          generateWideDynamicRangeValue(bits, random)
        case InputMode.MixedExtremeValues =>
          generateMixedExtremeValue(bits, random)
      }
    }
  }

  private def generateNormalValue(bits: Int, random: Random): Int = {
    val sigma = signedMax(bits).toDouble / 6.0
    clampToSignedRange(math.round(random.nextGaussian() * sigma).toInt, bits)
  }

  private def generateWideDynamicRangeValue(bits: Int, random: Random): Int = {
    val maxAbs = signedMax(bits).toInt
    if (maxAbs == 0) {
      0
    } else {
      val maxExponent = math.max(0, (math.log(maxAbs.toDouble) / math.log(2.0)).toInt)
      val exponent = random.nextInt(maxExponent + 1)
      val baseMagnitude = 1 << exponent
      val jitter = if (baseMagnitude == 1) 0 else random.nextInt(baseMagnitude)
      val magnitude = math.min(maxAbs, baseMagnitude + jitter)
      val signed = if (random.nextBoolean()) magnitude else -magnitude
      clampToSignedRange(signed, bits)
    }
  }

  private def generateMixedExtremeValue(bits: Int, random: Random): Int = {
    val minVal = signedMin(bits).toInt
    val maxVal = signedMax(bits).toInt
    val candidates = Array(
      minVal,
      maxVal,
      0,
      1,
      -1,
      maxVal / 2,
      minVal / 2,
      generateNormalValue(bits, random)
    )

    if (random.nextDouble() < 0.65) {
      candidates(random.nextInt(candidates.length))
    } else {
      clampToSignedRange(random.nextInt(maxVal.saturatingAdd(1)) - random.nextInt(maxVal.saturatingAdd(1)), bits)
    }
  }

  def signedMin(bits: Int): BigInt = -(BigInt(1) << (bits - 1))

  def signedMax(bits: Int): BigInt = (BigInt(1) << (bits - 1)) - 1

  private def clampToSignedRange(value: Int, bits: Int): Int = {
    val clamped = BigInt(value).max(signedMin(bits)).min(signedMax(bits))
    clamped.toInt
  }

  private implicit class RichInt(private val value: Int) extends AnyVal {
    def saturatingAdd(that: Int): Int = {
      val sum = value.toLong + that.toLong
      sum.max(Int.MinValue.toLong).min(Int.MaxValue.toLong).toInt
    }
  }
}

package Benchmark

object TrafficCounter {
  case class TrafficMetrics(
      totalCycles: Long,
      inputValidCycles: Long,
      inputFireCount: Long,
      inputStallCycles: Long,
      outputValidCycles: Long,
      outputFireCount: Long,
      outputStallCycles: Long,
      firstInputCycle: Option[Long],
      firstOutputCycle: Option[Long],
      lastOutputCycle: Option[Long],
      totalOps: Long,
      inputTensorReadBytes: Long,
      weightReadBytes: Long,
      outputWriteBytes: Long,
      arrayInputBytes: Long,
      arrayOutputBytes: Long,
      intermediateBufferWriteBytes: Long,
      intermediateBufferReadBytes: Long,
      totalExternalBytes: Long,
      totalOnchipBytes: Long,
      maxIntermediateBitwidth: Int,
      intermediateWriteCount: Long,
      intermediateReadCount: Long
  ) {
    def firstOutputLatency: Option[Long] =
      for {
        in <- firstInputCycle
        out <- firstOutputCycle
      } yield out - in

    def opsPerCycle: Double =
      if (totalCycles <= 0) 0.0 else totalOps.toDouble / totalCycles.toDouble
  }

  case class HandshakeStats(
      totalCycles: Long,
      inputValidCycles: Long,
      inputFireCount: Long,
      inputStallCycles: Long,
      outputValidCycles: Long,
      outputFireCount: Long,
      outputStallCycles: Long,
      firstInputCycle: Option[Long],
      firstOutputCycle: Option[Long],
      lastOutputCycle: Option[Long],
      totalOps: Long
  )

  def bytesForBits(bitWidth: Int, elementCount: Long): Long =
    ceilDiv(bitWidth, 8).toLong * elementCount

  def fixed(
      handshake: HandshakeStats,
      m: Int,
      n: Int,
      k: Int,
      caseCount: Int,
      inputBits: Int,
      outputBits: Int,
      intermediateBits: Int
  ): TrafficMetrics = {
    val inputCount = caseCount.toLong * m.toLong * k.toLong
    val weightCount = caseCount.toLong * k.toLong * n.toLong
    val outputCount = caseCount.toLong * m.toLong * n.toLong
    val macCount = caseCount.toLong * m.toLong * n.toLong * k.toLong

    val inputTensorReadBytes = bytesForBits(inputBits, inputCount)
    val weightReadBytes = bytesForBits(inputBits, weightCount)
    val outputWriteBytes = bytesForBits(outputBits, outputCount)
    val arrayInputBytes = inputTensorReadBytes + weightReadBytes
    val arrayOutputBytes = outputWriteBytes
    val intermediateBufferWriteBytes = bytesForBits(intermediateBits, macCount)
    val intermediateBufferReadBytes = bytesForBits(intermediateBits, macCount)

    TrafficMetrics(
      totalCycles = handshake.totalCycles,
      inputValidCycles = handshake.inputValidCycles,
      inputFireCount = handshake.inputFireCount,
      inputStallCycles = handshake.inputStallCycles,
      outputValidCycles = handshake.outputValidCycles,
      outputFireCount = handshake.outputFireCount,
      outputStallCycles = handshake.outputStallCycles,
      firstInputCycle = handshake.firstInputCycle,
      firstOutputCycle = handshake.firstOutputCycle,
      lastOutputCycle = handshake.lastOutputCycle,
      totalOps = handshake.totalOps,
      inputTensorReadBytes = inputTensorReadBytes,
      weightReadBytes = weightReadBytes,
      outputWriteBytes = outputWriteBytes,
      arrayInputBytes = arrayInputBytes,
      arrayOutputBytes = arrayOutputBytes,
      intermediateBufferWriteBytes = intermediateBufferWriteBytes,
      intermediateBufferReadBytes = intermediateBufferReadBytes,
      totalExternalBytes = inputTensorReadBytes + weightReadBytes + outputWriteBytes,
      totalOnchipBytes = arrayInputBytes + arrayOutputBytes + intermediateBufferWriteBytes + intermediateBufferReadBytes,
      maxIntermediateBitwidth = intermediateBits,
      intermediateWriteCount = macCount,
      intermediateReadCount = macCount
    )
  }

  def floating(
      handshake: HandshakeStats,
      m: Int,
      n: Int,
      k: Int,
      caseCount: Int,
      floatBits: Int,
      intermediateBits: Int
  ): TrafficMetrics = {
    val inputCount = caseCount.toLong * m.toLong * k.toLong
    val weightCount = caseCount.toLong * k.toLong * n.toLong
    val outputCount = caseCount.toLong * m.toLong * n.toLong
    val macCount = caseCount.toLong * m.toLong * n.toLong * k.toLong

    val inputTensorReadBytes = bytesForBits(floatBits, inputCount)
    val weightReadBytes = bytesForBits(floatBits, weightCount)
    val outputWriteBytes = bytesForBits(floatBits, outputCount)
    val arrayInputBytes = inputTensorReadBytes + weightReadBytes
    val arrayOutputBytes = outputWriteBytes
    val intermediateBufferWriteBytes = bytesForBits(intermediateBits, macCount)
    val intermediateBufferReadBytes = bytesForBits(intermediateBits, macCount)

    TrafficMetrics(
      totalCycles = handshake.totalCycles,
      inputValidCycles = handshake.inputValidCycles,
      inputFireCount = handshake.inputFireCount,
      inputStallCycles = handshake.inputStallCycles,
      outputValidCycles = handshake.outputValidCycles,
      outputFireCount = handshake.outputFireCount,
      outputStallCycles = handshake.outputStallCycles,
      firstInputCycle = handshake.firstInputCycle,
      firstOutputCycle = handshake.firstOutputCycle,
      lastOutputCycle = handshake.lastOutputCycle,
      totalOps = handshake.totalOps,
      inputTensorReadBytes = inputTensorReadBytes,
      weightReadBytes = weightReadBytes,
      outputWriteBytes = outputWriteBytes,
      arrayInputBytes = arrayInputBytes,
      arrayOutputBytes = arrayOutputBytes,
      intermediateBufferWriteBytes = intermediateBufferWriteBytes,
      intermediateBufferReadBytes = intermediateBufferReadBytes,
      totalExternalBytes = inputTensorReadBytes + weightReadBytes + outputWriteBytes,
      totalOnchipBytes = arrayInputBytes + arrayOutputBytes + intermediateBufferWriteBytes + intermediateBufferReadBytes,
      maxIntermediateBitwidth = intermediateBits,
      intermediateWriteCount = macCount,
      intermediateReadCount = macCount
    )
  }

  private def ceilDiv(x: Int, y: Int): Int = (x + y - 1) / y
}

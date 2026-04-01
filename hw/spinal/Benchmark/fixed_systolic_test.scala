package Benchmark

import BenchmarkInputGenerator.InputMode
import MatrixComputeUnit.SystolicArray2D._
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{StreamDriver, StreamMonitor}
import spinal.sim.VCSFlags

object FixedSystolicTest extends App {
  case class Options(
      m: Int = 4,
      n: Int = 4,
      k: Int = 4,
      caseCount: Int = 1,
      seed: Int = 20260316,
      mode: InputMode = InputMode.NormalDistribution,
      workloadBits: Int = 8,
      inputBits: Int = 16,
      outputBits: Int = 32,
      postShift: Int = 0,
      periodNs: Int = 10,
      useVcs: Boolean = false,
      dumpWave: Boolean = false,
      strict: Boolean = false
  ) {
    require(m > 0, "m must be > 0")
    require(n > 0, "n must be > 0")
    require(k > 0, "k must be > 0")
    require(caseCount > 0, "caseCount must be > 0")
    require(workloadBits > 0 && workloadBits <= inputBits, "workloadBits must be in 1..inputBits")
    require(inputBits > 0 && inputBits <= 32, "inputBits must be in 1..32")
    require(outputBits > 0 && outputBits <= 32, "outputBits must be in 1..32")
    require(periodNs > 0, "periodNs must be > 0")
  }

  case class NumericStats(
      count: Int,
      min: BigInt,
      max: BigInt,
      mean: Double,
      absMax: BigInt,
      zeroCount: Int,
      positiveCount: Int,
      negativeCount: Int,
      saturatedCount: Int
  )

  case class CaseIntermediateStats(
      inputA: NumericStats,
      inputB: NumericStats,
      rawAccumulator: NumericStats,
      referenceOutput: NumericStats,
      dutOutput: NumericStats
  )

  case class CaseResult(
      index: Int,
      matrixA: Array[Array[Int]],
      matrixB: Array[Array[Int]],
      exactReferenceOutput: Array[Array[Double]],
      expectedOutput: Array[Array[Int]],
      dutOutput: Array[Array[Int]],
      normalizedDutOutput: Array[Array[Double]],
      rawAccumulator: Array[Array[BigInt]],
      mismatchCount: Int,
      maxAbsError: BigInt,
      stats: CaseIntermediateStats,
      numericEvents: NumericEventCounter.NumericEventMetrics,
      precision: PrecisionAnalyzer.PrecisionMetrics
  )

  case class ExperimentResult(
      options: Options,
      cases: Seq[CaseResult],
      traffic: TrafficCounter.TrafficMetrics,
      numericEvents: NumericEventCounter.NumericEventMetrics,
      precision: PrecisionAnalyzer.PrecisionMetrics,
      totalMismatchCount: Int,
      artifacts: ReportGenerator.ReportArtifacts
  )

  private val options = parseArgs(args.toList)
  private val result = runExperiment(options)
  printReport(result)
  if (options.strict) {
    require(result.totalMismatchCount == 0, s"Detected ${result.totalMismatchCount} mismatched output elements")
  }

  private def parseArgs(argv: List[String]): Options = {
    def usage(): String =
      s"""Usage: sbt "runMain Benchmark.FixedSystolicTest [options]"
         |
         |Options:
         |  --m <int>             Rows of matrix A / output rows (default: 4)
         |  --n <int>             Columns of matrix B / output cols (default: 4)
         |  --k <int>             Shared inner dimension (default: 4)
         |  --cases <int>         Number of matrix pairs to simulate (default: 1)
         |  --seed <int>          Random seed (default: 20260316)
         |  --mode <name>         Input mode: normal, wide, extreme (default: normal)
         |  --workload-bits <int> Logical shared input range in bits (default: 8)
         |  --input-bits <int>    Input element width in bits (default: 16)
         |  --output-bits <int>   Output element width in bits (default: 32)
         |  --post-shift <int>    Arithmetic shift after matmul (default: 0)
         |  --period-ns <int>     Simulation clock period in ns (default: 10)
         |  --use-vcs             Use VCS backend instead of default simulator
         |  --dump-wave           Dump waves (FST on default backend, VCD on VCS)
         |  --strict              Fail the run if DUT output mismatches reference
         |  --help                Print this message
         |""".stripMargin

    def nextValue(rest: List[String], flag: String): (String, List[String]) = {
      rest match {
        case value :: tail => (value, tail)
        case Nil           => throw new IllegalArgumentException(s"Missing value for $flag")
      }
    }

    def loop(remaining: List[String], current: Options): Options = {
      remaining match {
        case Nil => current
        case "--help" :: _ =>
          println(usage())
          sys.exit(0)
        case "--m" :: tail =>
          val (value, next) = nextValue(tail, "--m")
          loop(next, current.copy(m = value.toInt))
        case "--n" :: tail =>
          val (value, next) = nextValue(tail, "--n")
          loop(next, current.copy(n = value.toInt))
        case "--k" :: tail =>
          val (value, next) = nextValue(tail, "--k")
          loop(next, current.copy(k = value.toInt))
        case "--cases" :: tail =>
          val (value, next) = nextValue(tail, "--cases")
          loop(next, current.copy(caseCount = value.toInt))
        case "--seed" :: tail =>
          val (value, next) = nextValue(tail, "--seed")
          loop(next, current.copy(seed = value.toInt))
        case "--mode" :: tail =>
          val (value, next) = nextValue(tail, "--mode")
          loop(next, current.copy(mode = InputMode.parse(value)))
        case "--workload-bits" :: tail =>
          val (value, next) = nextValue(tail, "--workload-bits")
          loop(next, current.copy(workloadBits = value.toInt))
        case "--input-bits" :: tail =>
          val (value, next) = nextValue(tail, "--input-bits")
          loop(next, current.copy(inputBits = value.toInt))
        case "--output-bits" :: tail =>
          val (value, next) = nextValue(tail, "--output-bits")
          loop(next, current.copy(outputBits = value.toInt))
        case "--post-shift" :: tail =>
          val (value, next) = nextValue(tail, "--post-shift")
          loop(next, current.copy(postShift = value.toInt))
        case "--period-ns" :: tail =>
          val (value, next) = nextValue(tail, "--period-ns")
          loop(next, current.copy(periodNs = value.toInt))
        case "--use-vcs" :: tail =>
          loop(tail, current.copy(useVcs = true))
        case "--dump-wave" :: tail =>
          loop(tail, current.copy(dumpWave = true))
        case "--strict" :: tail =>
          loop(tail, current.copy(strict = true))
        case unknown :: _ =>
          throw new IllegalArgumentException(s"Unknown option '$unknown'\n${usage()}")
      }
    }

    loop(argv, Options())
  }

  private def runExperiment(opts: Options): ExperimentResult = {
    val cases = BenchmarkInputGenerator.generateCases(
      caseCount = opts.caseCount,
      rowsA = opts.m,
      colsA = opts.k,
      rowsB = opts.k,
      colsB = opts.n,
      mode = opts.mode,
      bits = opts.workloadBits,
      seed = opts.seed
    )

    val workspace = s"simWorkspace/fixed_systolic_${opts.mode.cliName}_${opts.m}x${opts.k}x${opts.n}_seed${opts.seed}"
    val rtlTarget = s"rtl/Benchmark/fixed_systolic_${opts.mode.cliName}_${opts.m}x${opts.k}x${opts.n}"
    val spinalCfg = SpinalConfig(
      targetDirectory = rtlTarget,
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
      bitVectorWidthMax = 20000
    )

    val vcsFlag = VCSFlags(
      compileFlags = List("-kdb", "-lca", "+notimingchecks"),
      elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
      runFlags = List("-l ./run.log")
    )

    val baseSimConfig = SimConfig
      .workspacePath(workspace)
      .withConfig(spinalCfg)
      .withTimeScale(1 ns)
      .withTimePrecision(1 ns)
      .allOptimisation

    val simConfigWithBackend =
      if (opts.useVcs) baseSimConfig.withVCS(vcsFlag) else baseSimConfig

    val simConfig =
      if (!opts.dumpWave) {
        simConfigWithBackend
      } else if (opts.useVcs) {
        simConfigWithBackend.withVcdWave
      } else {
        simConfigWithBackend.withFstWave
      }

    val dutCfg = SystolicArray2D_Config(
      in_Length_Max = opts.k,
      in_Length_Min = 1,
      in_MatA_row_num = opts.m,
      in_MatB_col_num = opts.n,
      in_MatA_element_Width = opts.inputBits,
      in_MatB_element_Width = opts.inputBits,
      out_MatZ_element_Width = opts.outputBits,
      Enable_Transpose_logic = false,
      Enable_ElementWise_logic = false
    )

    val compiled = simConfig.compile(new SystolicArray2D(dutCfg))
    val dutOutputs = Array.fill(opts.caseCount)(Array.ofDim[Int](opts.m, opts.n))
    var handshakeStats: Option[TrafficCounter.HandshakeStats] = None

    compiled.doSim("fixed_systolic_test") { dut =>
      val timeoutCycles = math.max(5000, opts.caseCount * (opts.k + opts.m + opts.n + 64))
      SimTimeout(timeoutCycles)
      dut.clockDomain.forkStimulus(opts.periodNs)
      dut.io.out_Mats.ready #= true

      var cycle = 0L
      var inputFireCount = 0L
      var inputStallCycles = 0L
      var outputFireCount = 0L
      var outputStallCycles = 0L
      var firstInputCycle: Option[Long] = None
      var firstOutputCycle: Option[Long] = None
      var lastOutputCycle: Option[Long] = None

      fork {
        while (true) {
          dut.clockDomain.waitSampling()
          cycle += 1

          val inValid = dut.io.in_Mats.valid.toBoolean
          val inReady = dut.io.in_Mats.ready.toBoolean
          val outValid = dut.io.out_Mats.valid.toBoolean
          val outReady = dut.io.out_Mats.ready.toBoolean

          if (inValid && !inReady) {
            inputStallCycles += 1
          }

          if (outValid && !outReady) {
            outputStallCycles += 1
          }
        }
      }

      var sendingCase = 0
      var sendingK = 0
      StreamDriver(dut.io.in_Mats, dut.clockDomain) { payload =>
        if (sendingCase >= cases.length) {
          false
        } else {
          val current = cases(sendingCase)
          val isFinal = sendingK == opts.k - 1
          val eventCycle = cycle + 1

          inputFireCount += 1
          if (firstInputCycle.isEmpty) {
            firstInputCycle = Some(eventCycle)
          }

          payload.OpMode.post_Shift #= opts.postShift

          for (row <- 0 until opts.m) {
            payload.A(row).data #= current.matrixA(row)(sendingK)
            payload.A(row).Final #= isFinal
          }
          for (col <- 0 until opts.n) {
            payload.B(col).data #= current.matrixB(sendingK)(col)
            payload.B(col).Final #= isFinal
          }

          if (isFinal) {
            sendingCase += 1
            sendingK = 0
          } else {
            sendingK += 1
          }
          true
        }
      }

      var receivedCases = 0
      var done = false
      StreamMonitor(dut.io.out_Mats, dut.clockDomain) { payload =>
        val eventCycle = cycle + 1
        outputFireCount += 1
        if (firstOutputCycle.isEmpty) {
          firstOutputCycle = Some(eventCycle)
        }
        lastOutputCycle = Some(eventCycle)
        val outMatrix = Array.tabulate(opts.m, opts.n) { (row, col) =>
          payload.Z(row)(col).toInt
        }
        dutOutputs(receivedCases) = outMatrix
        receivedCases += 1
        if (receivedCases == opts.caseCount) {
          done = true
        }
      }

      dut.clockDomain.waitSamplingWhere(done)

      val totalOps = 2L * opts.m.toLong * opts.n.toLong * opts.k.toLong * opts.caseCount.toLong
      val inputValidCycles = inputFireCount + inputStallCycles
      val outputValidCycles = outputFireCount + outputStallCycles
      val totalCycles = (for {
        start <- firstInputCycle
        end <- lastOutputCycle
      } yield end - start + 1).getOrElse(cycle)

      handshakeStats = Some(
        TrafficCounter.HandshakeStats(
          totalCycles = totalCycles,
          inputValidCycles = inputValidCycles,
          inputFireCount = inputFireCount,
          inputStallCycles = inputStallCycles,
          outputValidCycles = outputValidCycles,
          outputFireCount = outputFireCount,
          outputStallCycles = outputStallCycles,
          firstInputCycle = firstInputCycle,
          firstOutputCycle = firstOutputCycle,
          lastOutputCycle = lastOutputCycle,
          totalOps = totalOps
        )
      )

      simSuccess()
    }

    val caseResults = cases.zipWithIndex.map { case (testCase, index) =>
      val rawAccumulator = BenchmarkReferenceModel.exactMatmul(testCase.matrixA, testCase.matrixB)
      val exactReferenceOutput = BenchmarkReferenceModel.bigIntMatrixToDouble(rawAccumulator)
      val expectedOutput = applyShiftAndSaturate(rawAccumulator, opts.postShift, opts.outputBits)
      val dutOutput = dutOutputs(index)
      val normalizedDutOutput = BenchmarkReferenceModel.rescaleFixedOutput(dutOutput, opts.postShift)
      val mismatchCount = countMismatches(expectedOutput, dutOutput)
      val maxAbsError = computeMaxAbsError(expectedOutput, dutOutput)

      val numericEvents = NumericEventCounter.fixed(rawAccumulator, opts.postShift, opts.outputBits)
      val precision = PrecisionAnalyzer.analyze(
        flattenDoubleMatrix(exactReferenceOutput),
        flattenDoubleMatrix(normalizedDutOutput)
      )

      CaseResult(
        index = testCase.index,
        matrixA = testCase.matrixA,
        matrixB = testCase.matrixB,
        exactReferenceOutput = exactReferenceOutput,
        expectedOutput = expectedOutput,
        dutOutput = dutOutput,
        normalizedDutOutput = normalizedDutOutput,
        rawAccumulator = rawAccumulator,
        mismatchCount = mismatchCount,
        maxAbsError = maxAbsError,
        stats = CaseIntermediateStats(
          inputA = computeStats(flattenInts(testCase.matrixA)),
          inputB = computeStats(flattenInts(testCase.matrixB)),
          rawAccumulator = computeStats(flattenBigInts(rawAccumulator)),
          referenceOutput = computeStats(flattenInts(expectedOutput), Some(opts.outputBits)),
          dutOutput = computeStats(flattenInts(dutOutput), Some(opts.outputBits))
        ),
        numericEvents = numericEvents,
        precision = precision
      )
    }

    val traffic = TrafficCounter.fixed(
      handshake = handshakeStats.getOrElse(throw new IllegalStateException("Traffic statistics were not captured")),
      m = opts.m,
      n = opts.n,
      k = opts.k,
      caseCount = opts.caseCount,
      inputBits = opts.inputBits,
      outputBits = opts.outputBits,
      intermediateBits = dutCfg.SystolicArray2DUnit_Cfg.ProductSum_Width
    )
    val precision = PrecisionAnalyzer.analyze(
      caseResults.flatMap(caseResult => flattenDoubleMatrix(caseResult.exactReferenceOutput)),
      caseResults.flatMap(caseResult => flattenDoubleMatrix(caseResult.normalizedDutOutput))
    )
    val numericEvents = caseResults.map(_.numericEvents).foldLeft(NumericEventCounter.empty)(_ + _)
    val artifacts = writeReports(opts, traffic, numericEvents, precision, caseResults)

    ExperimentResult(
      options = opts,
      cases = caseResults,
      traffic = traffic,
      numericEvents = numericEvents,
      precision = precision,
      totalMismatchCount = caseResults.map(_.mismatchCount).sum,
      artifacts = artifacts
    )
  }

  private def applyShiftAndSaturate(raw: Array[Array[BigInt]], shift: Int, outBits: Int): Array[Array[Int]] = {
    Array.tabulate(raw.length, raw.head.length) { (row, col) =>
      shiftAndSaturate(raw(row)(col), shift, outBits).toInt
    }
  }

  private def shiftAndSaturate(value: BigInt, shift: Int, outBits: Int): BigInt = {
    val boundedShift = shift.max(-outBits).min(outBits)
    val shifted =
      if (boundedShift > 0) value >> boundedShift
      else if (boundedShift < 0) value << (-boundedShift)
      else value

    shifted.max(signedMin(outBits)).min(signedMax(outBits))
  }

  private def computeStats(values: Seq[BigInt], satBits: Option[Int] = None): NumericStats = {
    require(values.nonEmpty, "cannot compute statistics of an empty sequence")

    val minValue = values.min
    val maxValue = values.max
    val absMaxValue = values.map(_.abs).max
    val meanValue = values.map(_.toDouble).sum / values.size.toDouble
    val zeroCount = values.count(_ == 0)
    val positiveCount = values.count(_ > 0)
    val negativeCount = values.count(_ < 0)
    val saturatedCount = satBits match {
      case Some(bits) =>
        val minSat = signedMin(bits)
        val maxSat = signedMax(bits)
        values.count(v => v == minSat || v == maxSat)
      case None => 0
    }

    NumericStats(
      count = values.size,
      min = minValue,
      max = maxValue,
      mean = meanValue,
      absMax = absMaxValue,
      zeroCount = zeroCount,
      positiveCount = positiveCount,
      negativeCount = negativeCount,
      saturatedCount = saturatedCount
    )
  }

  private def countMismatches(reference: Array[Array[Int]], dut: Array[Array[Int]]): Int = {
    reference.indices.iterator.map { row =>
      reference(row).indices.count { col =>
        reference(row)(col) != dut(row)(col)
      }
    }.sum
  }

  private def computeMaxAbsError(reference: Array[Array[Int]], dut: Array[Array[Int]]): BigInt = {
    reference.indices.iterator.flatMap { row =>
      reference(row).indices.iterator.map { col =>
        (BigInt(reference(row)(col)) - BigInt(dut(row)(col))).abs
      }
    }.maxOption.getOrElse(BigInt(0))
  }

  private def flattenInts(matrix: Array[Array[Int]]): Seq[BigInt] =
    matrix.iterator.flatMap(_.iterator).map(BigInt(_)).toSeq

  private def flattenBigInts(matrix: Array[Array[BigInt]]): Seq[BigInt] =
    matrix.iterator.flatMap(_.iterator).toSeq

  private def flattenIntMatrixToDoubles(matrix: Array[Array[Int]]): Seq[Double] =
    matrix.iterator.flatMap(_.iterator).map(_.toDouble).toSeq

  private def flattenDoubleMatrix(matrix: Array[Array[Double]]): Seq[Double] =
    matrix.iterator.flatMap(_.iterator).toSeq

  private def signedMin(bits: Int): BigInt = -(BigInt(1) << (bits - 1))

  private def signedMax(bits: Int): BigInt = (BigInt(1) << (bits - 1)) - 1

  private def formatMatrix[T](matrix: Array[Array[T]]): String =
    matrix.map(row => row.mkString("[", ", ", "]")).mkString("\n")

  private def writeReports(
      opts: Options,
      traffic: TrafficCounter.TrafficMetrics,
      numericEvents: NumericEventCounter.NumericEventMetrics,
      precision: PrecisionAnalyzer.PrecisionMetrics,
      caseResults: Seq[CaseResult]
  ): ReportGenerator.ReportArtifacts = {
    val timestamp = ReportGenerator.currentTimestampTag()
    val matrix = ReportGenerator.matrixTag(opts.m, opts.n, opts.k)
    val prefix = ReportGenerator.buildPrefix("fixed", opts.mode.cliName, matrix, timestamp)

    val csvHeaders = Seq(
      "experiment",
      "mode",
      "m",
      "n",
      "k",
      "case_count",
      "seed",
      "workload_bits",
      "input_bits",
      "output_bits",
      "post_shift",
      "input_tensor_read_bytes",
      "weight_read_bytes",
      "output_write_bytes",
      "array_input_bytes",
      "array_output_bytes",
      "intermediate_buffer_write_bytes",
      "intermediate_buffer_read_bytes",
      "total_external_bytes",
      "total_onchip_bytes",
      "max_intermediate_bitwidth",
      "intermediate_write_count",
      "intermediate_read_count",
      "shift_count",
      "truncate_count",
      "saturation_count",
      "overflow_count",
      "rounding_count",
      "mae",
      "mse",
      "max_abs_error",
      "relative_error",
      "cosine_similarity",
      "total_mismatches",
      "total_cycles",
      "ops_per_cycle"
    )

    val csvValues = Seq(
      "fixed_systolic",
      opts.mode.cliName,
      opts.m.toString,
      opts.n.toString,
      opts.k.toString,
      opts.caseCount.toString,
      opts.seed.toString,
      opts.workloadBits.toString,
      opts.inputBits.toString,
      opts.outputBits.toString,
      opts.postShift.toString,
      traffic.inputTensorReadBytes.toString,
      traffic.weightReadBytes.toString,
      traffic.outputWriteBytes.toString,
      traffic.arrayInputBytes.toString,
      traffic.arrayOutputBytes.toString,
      traffic.intermediateBufferWriteBytes.toString,
      traffic.intermediateBufferReadBytes.toString,
      traffic.totalExternalBytes.toString,
      traffic.totalOnchipBytes.toString,
      traffic.maxIntermediateBitwidth.toString,
      traffic.intermediateWriteCount.toString,
      traffic.intermediateReadCount.toString,
      numericEvents.shiftCount.toString,
      numericEvents.truncateCount.toString,
      numericEvents.saturationCount.toString,
      numericEvents.overflowCount.toString,
      numericEvents.roundingCount.toString,
      precision.mae.toString,
      precision.mse.toString,
      precision.maxAbsError.toString,
      precision.relativeError.toString,
      precision.cosineSimilarity.toString,
      caseResults.map(_.mismatchCount).sum.toString,
      traffic.totalCycles.toString,
      traffic.opsPerCycle.toString
    )

    val jsonBody = ReportGenerator.jsonObject(
      Seq(
        "experiment" -> ReportGenerator.jsonString("fixed_systolic"),
        "options" -> ReportGenerator.jsonObject(
          Seq(
            "mode" -> ReportGenerator.jsonString(opts.mode.cliName),
            "m" -> ReportGenerator.jsonNumber(opts.m),
            "n" -> ReportGenerator.jsonNumber(opts.n),
            "k" -> ReportGenerator.jsonNumber(opts.k),
            "caseCount" -> ReportGenerator.jsonNumber(opts.caseCount),
            "seed" -> ReportGenerator.jsonNumber(opts.seed),
            "workloadBits" -> ReportGenerator.jsonNumber(opts.workloadBits),
            "inputBits" -> ReportGenerator.jsonNumber(opts.inputBits),
            "outputBits" -> ReportGenerator.jsonNumber(opts.outputBits),
            "postShift" -> ReportGenerator.jsonNumber(opts.postShift),
            "precisionReference" -> ReportGenerator.jsonString("exact_integer_gemm_rescaled_to_raw_domain")
          )
        ),
        "traffic" -> ReportGenerator.jsonObject(
          Seq(
            "inputTensorReadBytes" -> ReportGenerator.jsonNumber(traffic.inputTensorReadBytes),
            "weightReadBytes" -> ReportGenerator.jsonNumber(traffic.weightReadBytes),
            "outputWriteBytes" -> ReportGenerator.jsonNumber(traffic.outputWriteBytes),
            "arrayInputBytes" -> ReportGenerator.jsonNumber(traffic.arrayInputBytes),
            "arrayOutputBytes" -> ReportGenerator.jsonNumber(traffic.arrayOutputBytes),
            "intermediateBufferWriteBytes" -> ReportGenerator.jsonNumber(traffic.intermediateBufferWriteBytes),
            "intermediateBufferReadBytes" -> ReportGenerator.jsonNumber(traffic.intermediateBufferReadBytes),
            "totalExternalBytes" -> ReportGenerator.jsonNumber(traffic.totalExternalBytes),
            "totalOnchipBytes" -> ReportGenerator.jsonNumber(traffic.totalOnchipBytes),
            "maxIntermediateBitwidth" -> ReportGenerator.jsonNumber(traffic.maxIntermediateBitwidth),
            "intermediateWriteCount" -> ReportGenerator.jsonNumber(traffic.intermediateWriteCount),
            "intermediateReadCount" -> ReportGenerator.jsonNumber(traffic.intermediateReadCount),
            "totalCycles" -> ReportGenerator.jsonNumber(traffic.totalCycles),
            "firstOutputLatency" -> ReportGenerator.jsonOptional(traffic.firstOutputLatency)
          )
        ),
        "numericEvents" -> ReportGenerator.jsonObject(
          Seq(
            "shiftCount" -> ReportGenerator.jsonNumber(numericEvents.shiftCount),
            "truncateCount" -> ReportGenerator.jsonNumber(numericEvents.truncateCount),
            "saturationCount" -> ReportGenerator.jsonNumber(numericEvents.saturationCount),
            "overflowCount" -> ReportGenerator.jsonNumber(numericEvents.overflowCount),
            "roundingCount" -> ReportGenerator.jsonNumber(numericEvents.roundingCount)
          )
        ),
        "precision" -> ReportGenerator.jsonObject(
          Seq(
            "mae" -> ReportGenerator.jsonNumber(precision.mae),
            "mse" -> ReportGenerator.jsonNumber(precision.mse),
            "maxAbsError" -> ReportGenerator.jsonNumber(precision.maxAbsError),
            "relativeError" -> ReportGenerator.jsonNumber(precision.relativeError),
            "cosineSimilarity" -> ReportGenerator.jsonNumber(precision.cosineSimilarity)
          )
        ),
        "cases" -> ReportGenerator.jsonArray(
          caseResults.map { caseResult =>
            ReportGenerator.jsonObject(
              Seq(
                "index" -> ReportGenerator.jsonNumber(caseResult.index),
                "matrixA" -> ReportGenerator.jsonMatrixInt(caseResult.matrixA),
                "matrixB" -> ReportGenerator.jsonMatrixInt(caseResult.matrixB),
                "rawAccumulator" -> ReportGenerator.jsonMatrixBigInt(caseResult.rawAccumulator),
                "exactReferenceOutput" -> ReportGenerator.jsonMatrixDouble(caseResult.exactReferenceOutput),
                "expectedOutput" -> ReportGenerator.jsonMatrixInt(caseResult.expectedOutput),
                "dutOutput" -> ReportGenerator.jsonMatrixInt(caseResult.dutOutput),
                "normalizedDutOutput" -> ReportGenerator.jsonMatrixDouble(caseResult.normalizedDutOutput),
                "mismatchCount" -> ReportGenerator.jsonNumber(caseResult.mismatchCount),
                "maxAbsError" -> ReportGenerator.jsonNumber(caseResult.maxAbsError),
                "numericEvents" -> ReportGenerator.jsonObject(
                  Seq(
                    "shiftCount" -> ReportGenerator.jsonNumber(caseResult.numericEvents.shiftCount),
                    "truncateCount" -> ReportGenerator.jsonNumber(caseResult.numericEvents.truncateCount),
                    "saturationCount" -> ReportGenerator.jsonNumber(caseResult.numericEvents.saturationCount),
                    "overflowCount" -> ReportGenerator.jsonNumber(caseResult.numericEvents.overflowCount),
                    "roundingCount" -> ReportGenerator.jsonNumber(caseResult.numericEvents.roundingCount)
                  )
                ),
                "precision" -> ReportGenerator.jsonObject(
                  Seq(
                    "mae" -> ReportGenerator.jsonNumber(caseResult.precision.mae),
                    "mse" -> ReportGenerator.jsonNumber(caseResult.precision.mse),
                    "maxAbsError" -> ReportGenerator.jsonNumber(caseResult.precision.maxAbsError),
                    "relativeError" -> ReportGenerator.jsonNumber(caseResult.precision.relativeError),
                    "cosineSimilarity" -> ReportGenerator.jsonNumber(caseResult.precision.cosineSimilarity)
                  )
                )
              )
            )
          }
        )
      )
    )

    val markdownBody =
      s"""# Fixed Experiment Report
         |
         |## Experiment Configuration
         |- Design: `fixed`
         |- Matrix size: `${opts.m} x ${opts.n} x ${opts.k}`
         |- Float mode: `n/a`
         |- Input mode: `${opts.mode.cliName}`
         |- Workload bits: `${opts.workloadBits}`
         |- Fixed input bits: `${opts.inputBits}`
         |- Fixed output bits: `${opts.outputBits}`
         |- Precision reference: `exact integer GEMM, fixed output rescaled back to raw domain`
         |- Timestamp: `$timestamp`
         |
         |## Traffic Summary
         |- Input tensor read bytes: `${traffic.inputTensorReadBytes}`
         |- Weight read bytes: `${traffic.weightReadBytes}`
         |- Output write bytes: `${traffic.outputWriteBytes}`
         |- Array input bytes: `${traffic.arrayInputBytes}`
         |- Array output bytes: `${traffic.arrayOutputBytes}`
         |- Intermediate buffer writes: `${traffic.intermediateBufferWriteBytes}`
         |- Intermediate buffer reads: `${traffic.intermediateBufferReadBytes}`
         |- Total external bytes: `${traffic.totalExternalBytes}`
         |- Total on-chip bytes: `${traffic.totalOnchipBytes}`
         |- Max intermediate bitwidth: `${traffic.maxIntermediateBitwidth}`
         |- Intermediate write count: `${traffic.intermediateWriteCount}`
         |- Intermediate read count: `${traffic.intermediateReadCount}`
         |
         |## Precision Summary
         |- Compared against: `common high-precision reference`
         |- MAE: `${precision.mae}`
         |- MSE: `${precision.mse}`
         |- Max abs error: `${precision.maxAbsError}`
         |- Relative error: `${precision.relativeError}`
         |- Cosine similarity: `${precision.cosineSimilarity}`
         |
         |## Numeric Event Summary
         |- Shift count: `${numericEvents.shiftCount}`
         |- Truncate count: `${numericEvents.truncateCount}`
         |- Saturation count: `${numericEvents.saturationCount}`
         |- Overflow count: `${numericEvents.overflowCount}`
         |- Rounding count: `${numericEvents.roundingCount}`
         |""".stripMargin

    ReportGenerator.writeReport(
      prefix = prefix,
      csvHeaders = csvHeaders,
      csvValues = csvValues,
      jsonBody = jsonBody,
      markdownBody = markdownBody,
      summaryInputMode = opts.mode.cliName,
      summaryMatrix = matrix,
      summaryTimestamp = timestamp
    )
  }

  private def printNumericStats(name: String, stats: NumericStats): Unit = {
    println(
      f"$name%-18s count=${stats.count}%4d min=${stats.min}%8s max=${stats.max}%8s " +
        f"mean=${stats.mean}%12.4f absMax=${stats.absMax}%8s zeros=${stats.zeroCount}%4d " +
        f"pos=${stats.positiveCount}%4d neg=${stats.negativeCount}%4d sat=${stats.saturatedCount}%4d"
    )
  }

  private def printReport(result: ExperimentResult): Unit = {
    val opts = result.options
    println("===== Fixed Systolic Experiment =====")
    println(s"Mode              : ${opts.mode.cliName}")
    println(s"Matrix size (M,N,K): (${opts.m}, ${opts.n}, ${opts.k})")
    println(s"Case count        : ${opts.caseCount}")
    println(s"Seed              : ${opts.seed}")
    println(s"Workload bits     : ${opts.workloadBits}")
    println(s"Input bits        : ${opts.inputBits}")
    println(s"Output bits       : ${opts.outputBits}")
    println(s"Post shift        : ${opts.postShift}")
    println(s"Simulator backend : ${if (opts.useVcs) "VCS" else "default"}")
    println(s"CSV report        : ${result.artifacts.csvPath}")
    println(s"JSON report       : ${result.artifacts.jsonPath}")
    println(s"Markdown report   : ${result.artifacts.markdownPath}")
    println(s"Summary report    : ${result.artifacts.summaryMarkdownPath}")
    println()

    result.cases.foreach { caseResult =>
      println(s"--- Case ${caseResult.index} ---")
      println("Matrix A:")
      println(formatMatrix(caseResult.matrixA))
      println("Matrix B:")
      println(formatMatrix(caseResult.matrixB))
      println("Raw accumulator tensor:")
      println(formatMatrix(caseResult.rawAccumulator))
      println("Exact reference tensor:")
      println(formatMatrix(caseResult.exactReferenceOutput))
      println("Expected fixed output tensor:")
      println(formatMatrix(caseResult.expectedOutput))
      println("DUT fixed output tensor:")
      println(formatMatrix(caseResult.dutOutput))
      println("DUT output rescaled to raw domain:")
      println(formatMatrix(caseResult.normalizedDutOutput))
      println(s"Mismatch count    : ${caseResult.mismatchCount}")
      println(s"Max abs error     : ${caseResult.maxAbsError}")
      printNumericStats("inputA", caseResult.stats.inputA)
      printNumericStats("inputB", caseResult.stats.inputB)
      printNumericStats("rawAccumulator", caseResult.stats.rawAccumulator)
      printNumericStats("referenceOutput", caseResult.stats.referenceOutput)
      printNumericStats("dutOutput", caseResult.stats.dutOutput)
      println()
    }

    println("===== Traffic Statistics =====")
    println(s"Total cycles      : ${result.traffic.totalCycles}")
    println(s"Input valid cycles: ${result.traffic.inputValidCycles}")
    println(s"Input fire count  : ${result.traffic.inputFireCount}")
    println(s"Input stall cycles: ${result.traffic.inputStallCycles}")
    println(s"Output valid cycles: ${result.traffic.outputValidCycles}")
    println(s"Output fire count : ${result.traffic.outputFireCount}")
    println(s"Output stall cycles: ${result.traffic.outputStallCycles}")
    println(s"First input cycle : ${result.traffic.firstInputCycle.getOrElse(-1L)}")
    println(s"First output cycle: ${result.traffic.firstOutputCycle.getOrElse(-1L)}")
    println(s"Last output cycle : ${result.traffic.lastOutputCycle.getOrElse(-1L)}")
    println(s"First output latency: ${result.traffic.firstOutputLatency.getOrElse(-1L)}")
    println(f"Ops per cycle     : ${result.traffic.opsPerCycle}%.4f")
    println(s"Total external bytes: ${result.traffic.totalExternalBytes}")
    println(s"Total onchip bytes: ${result.traffic.totalOnchipBytes}")
    println(s"Intermediate write count: ${result.traffic.intermediateWriteCount}")
    println(s"Intermediate read count : ${result.traffic.intermediateReadCount}")
    println(s"Shift count       : ${result.numericEvents.shiftCount}")
    println(s"Truncate count    : ${result.numericEvents.truncateCount}")
    println(s"Saturation count  : ${result.numericEvents.saturationCount}")
    println(s"Overflow count    : ${result.numericEvents.overflowCount}")
    println(s"Rounding count    : ${result.numericEvents.roundingCount}")
    println(f"MAE               : ${result.precision.mae}%.6f")
    println(f"MSE               : ${result.precision.mse}%.6f")
    println(f"Relative error    : ${result.precision.relativeError}%.6f")
    println(f"Cosine similarity : ${result.precision.cosineSimilarity}%.6f")
    println(s"Total mismatches  : ${result.totalMismatchCount}")
  }
}

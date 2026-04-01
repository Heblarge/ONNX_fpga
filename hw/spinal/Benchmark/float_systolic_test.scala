package Benchmark

import BenchmarkInputGenerator.InputMode
import FloatingPoint._
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{StreamDriver, StreamMonitor}
import spinal.sim.VCSFlags

object FloatSystolicTest extends App {
  sealed trait FloatMode {
    def cliName: String
    def config: FpxxConfig
    def resolvedName: String
  }

  object FloatMode {
    case object Float16 extends FloatMode {
      override val cliName: String = "float16"
      override val config: FpxxConfig = FpxxConfig.float16()
      override val resolvedName: String = "float16"
    }

    case object Float8 extends FloatMode {
      override val cliName: String = "float8"
      override val config: FpxxConfig = FpxxConfig.float8_e4m3fnuz()
      override val resolvedName: String = "float8_e4m3fnuz"
    }

    case object Float8E5M2Fnuz extends FloatMode {
      override val cliName: String = "float8_e5m2fnuz"
      override val config: FpxxConfig = FpxxConfig.float8_e5m2fnuz()
      override val resolvedName: String = "float8_e5m2fnuz"
    }

    case object Float8E4M3Fnuz extends FloatMode {
      override val cliName: String = "float8_e4m3fnuz"
      override val config: FpxxConfig = FpxxConfig.float8_e4m3fnuz()
      override val resolvedName: String = "float8_e4m3fnuz"
    }

    val all: Seq[FloatMode] = Seq(Float16, Float8, Float8E5M2Fnuz, Float8E4M3Fnuz)

    def parse(text: String): FloatMode = {
      text.trim.toLowerCase match {
        case "float16" | "fp16" =>
          Float16
        case "float8" | "fp8" =>
          Float8
        case "float8_e5m2fnuz" | "e5m2fnuz" =>
          Float8E5M2Fnuz
        case "float8_e4m3fnuz" | "e4m3fnuz" =>
          Float8E4M3Fnuz
        case other =>
          throw new IllegalArgumentException(
            s"Unknown float mode '$other'. Expected one of: ${all.map(_.cliName).mkString(", ")}"
          )
      }
    }
  }

  case class Options(
      m: Int = 4,
      n: Int = 4,
      k: Int = 4,
      caseCount: Int = 1,
      seed: Int = 20260316,
      mode: InputMode = InputMode.NormalDistribution,
      workloadBits: Int = 8,
      floatMode: FloatMode = FloatMode.Float16,
      periodNs: Int = 10,
      useVcs: Boolean = false,
      dumpWave: Boolean = false
  ) {
    require(m > 0, "m must be > 0")
    require(n > 0, "n must be > 0")
    require(k > 0, "k must be > 0")
    require(caseCount > 0, "caseCount must be > 0")
    require(workloadBits > 0 && workloadBits <= 32, "workloadBits must be in 1..32")
    require(periodNs > 0, "periodNs must be > 0")
  }

  case class CaseResult(
      index: Int,
      sourceA: Array[Array[Int]],
      sourceB: Array[Array[Int]],
      quantizedA: Array[Array[Double]],
      quantizedB: Array[Array[Double]],
      exactReferenceOutput: Array[Array[Double]],
      quantizedReferenceOutput: Array[Array[Double]],
      dutOutput: Array[Array[Double]],
      numericEvents: NumericEventCounter.NumericEventMetrics,
      precision: PrecisionAnalyzer.PrecisionMetrics
  )

  case class ExperimentResult(
      options: Options,
      cases: Seq[CaseResult],
      traffic: TrafficCounter.TrafficMetrics,
      numericEvents: NumericEventCounter.NumericEventMetrics,
      precision: PrecisionAnalyzer.PrecisionMetrics,
      artifacts: ReportGenerator.ReportArtifacts
  )

  private val options = parseArgs(args.toList)
  private val result = runExperiment(options)
  printReport(result)

  private def parseArgs(argv: List[String]): Options = {
    def usage(): String =
      s"""Usage: sbt "runMain Benchmark.FloatSystolicTest [options]"
         |
         |Options:
         |  --m <int>             Rows of matrix A / output rows (default: 4)
         |  --n <int>             Columns of matrix B / output cols (default: 4, must equal m)
         |  --k <int>             Shared inner dimension (default: 4)
         |  --cases <int>         Number of matrix pairs to simulate (default: 1)
         |  --seed <int>          Random seed (default: 20260316)
         |  --mode <name>         Input mode: normal, wide, extreme (default: normal)
         |  --workload-bits <int> Shared logical input range in bits (default: 8)
         |  --input-bits <int>    Alias of --workload-bits for backward compatibility
         |  --float-mode <name>   float16, float8, float8_e5m2fnuz, float8_e4m3fnuz (default: float16)
         |  --period-ns <int>     Simulation clock period in ns (default: 10)
         |  --use-vcs             Use VCS backend instead of default simulator
         |  --dump-wave           Dump waves (FST on default backend, VCD on VCS)
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
          loop(next, current.copy(workloadBits = value.toInt))
        case "--float-mode" :: tail =>
          val (value, next) = nextValue(tail, "--float-mode")
          loop(next, current.copy(floatMode = FloatMode.parse(value)))
        case "--period-ns" :: tail =>
          val (value, next) = nextValue(tail, "--period-ns")
          loop(next, current.copy(periodNs = value.toInt))
        case "--use-vcs" :: tail =>
          loop(tail, current.copy(useVcs = true))
        case "--dump-wave" :: tail =>
          loop(tail, current.copy(dumpWave = true))
        case unknown :: _ =>
          throw new IllegalArgumentException(s"Unknown option '$unknown'\n${usage()}")
      }
    }

    val parsed = loop(argv, Options())
    require(parsed.m == parsed.n, "SquareSystolicArray requires m == n")
    parsed
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

    val algo = new FloatAlgo(opts.floatMode.config)
    val quantizedA = cases.map(testCase => quantizeMatrix(testCase.matrixA, algo))
    val quantizedB = cases.map(testCase => quantizeMatrix(testCase.matrixB, algo))
    val exactReferenceOutputs = cases.map(testCase =>
      BenchmarkReferenceModel.bigIntMatrixToDouble(BenchmarkReferenceModel.exactMatmul(testCase.matrixA, testCase.matrixB))
    )
    val quantizedReferenceOutputs = exactReferenceOutputs.map(matrix => quantizeDoubleMatrix(matrix, algo))

    val workspace =
      s"simWorkspace/float_systolic_${opts.floatMode.cliName}_${opts.mode.cliName}_${opts.m}x${opts.k}x${opts.n}_seed${opts.seed}"
    val rtlTarget =
      s"rtl/Benchmark/float_systolic_${opts.floatMode.cliName}_${opts.mode.cliName}_${opts.m}x${opts.k}x${opts.n}"

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

    val accIntWidth = math.max(24, opts.workloadBits * 2 + log2Up(opts.k + 1) + 2)
    val dutCfg = SquareSystolicArray_Config(
      in_Length_Max = opts.k,
      in_Length_Min = opts.k,
      in_MatA_row_num = opts.m,
      in_MatB_col_num = opts.n,
      fpConfig = opts.floatMode.config,
      accIntBits = accIntWidth bits,
      accFracBits = 0 bits,
      mulStages = SquareSystolicArray_Config.MaxMulStages,
      f2iStages = SquareSystolicArray_Config.MaxF2iStages,
      af2fStages = SquareSystolicArray_Config.MaxAf2fStages,
      Enable_Transpose_logic = false,
      Enable_ElementWise_logic = false
    )

    val compiled = simConfig.compile(new SquareSystolicArray(dutCfg))
    val dutOutputs = Array.fill(opts.caseCount)(Array.ofDim[Double](opts.m, opts.n))
    var handshakeStats: Option[TrafficCounter.HandshakeStats] = None

    compiled.doSim("float_systolic_test") { dut =>
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

          payload.OpMode.post_Shift #= 0

          for (row <- 0 until opts.m) {
            val host = algo.toHost(current.matrixA(row)(sendingK).toDouble)
            val (sign, exp, mant) = unpackFloat(host)
            payload.A(row).data.sign #= sign
            payload.A(row).data.exp #= exp
            payload.A(row).data.mant #= mant
            payload.A(row).Final #= isFinal
          }

          for (col <- 0 until opts.n) {
            val host = algo.toHost(current.matrixB(sendingK)(col).toDouble)
            val (sign, exp, mant) = unpackFloat(host)
            payload.B(col).data.sign #= sign
            payload.B(col).data.exp #= exp
            payload.B(col).data.mant #= mant
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
          decodeFloat(
            sign = payload.Z(row)(col).sign.toBoolean,
            exp = payload.Z(row)(col).exp.toInt,
            mant = payload.Z(row)(col).mant.toInt,
            cfg = dutCfg.fpConfig,
            algo = algo
          )
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

    val caseResults = cases.indices.map { index =>
        val testCase = cases(index)
        CaseResult(
          index = testCase.index,
          sourceA = testCase.matrixA,
          sourceB = testCase.matrixB,
          quantizedA = quantizedA(index),
          quantizedB = quantizedB(index),
          exactReferenceOutput = exactReferenceOutputs(index),
          quantizedReferenceOutput = quantizedReferenceOutputs(index),
          dutOutput = dutOutputs(index),
          numericEvents = NumericEventCounter.floating(
            sourceInputs = flattenIntMatrixToDoubles(testCase.matrixA) ++ flattenIntMatrixToDoubles(testCase.matrixB),
            quantizedInputs = flattenDoubleMatrix(quantizedA(index)) ++ flattenDoubleMatrix(quantizedB(index)),
            referenceOutputs = flattenDoubleMatrix(exactReferenceOutputs(index)),
            quantizedReferenceOutputs = flattenDoubleMatrix(quantizedReferenceOutputs(index)),
            algo = algo
          ),
          precision = PrecisionAnalyzer.analyze(
            flattenDoubleMatrix(exactReferenceOutputs(index)),
            flattenDoubleMatrix(dutOutputs(index))
          )
        )
      }
    val traffic = TrafficCounter.floating(
      handshake = handshakeStats.getOrElse(throw new IllegalStateException("Traffic statistics were not captured")),
      m = opts.m,
      n = opts.n,
      k = opts.k,
      caseCount = opts.caseCount,
      floatBits = opts.floatMode.config.full_size,
      intermediateBits = dutCfg.accIntBits.value + dutCfg.accFracBits.value
    )
    val numericEvents = caseResults.map(_.numericEvents).foldLeft(NumericEventCounter.empty)(_ + _)
    val precision = PrecisionAnalyzer.analyze(
      caseResults.flatMap(caseResult => flattenDoubleMatrix(caseResult.exactReferenceOutput)),
      caseResults.flatMap(caseResult => flattenDoubleMatrix(caseResult.dutOutput))
    )
    val artifacts = writeReports(opts, traffic, numericEvents, precision, caseResults)

    ExperimentResult(
      options = opts,
      cases = caseResults,
      traffic = traffic,
      numericEvents = numericEvents,
      precision = precision,
      artifacts = artifacts
    )
  }

  private def quantizeMatrix(matrix: Array[Array[Int]], algo: FloatAlgo): Array[Array[Double]] = {
    Array.tabulate(matrix.length, matrix.head.length) { (row, col) =>
      val host = algo.toHost(matrix(row)(col).toDouble)
      algo.fromBits(host.value.toInt)
    }
  }

  private def quantizeDoubleMatrix(matrix: Array[Array[Double]], algo: FloatAlgo): Array[Array[Double]] = {
    Array.tabulate(matrix.length, matrix.head.length) { (row, col) =>
      val host = algo.toHost(matrix(row)(col))
      algo.fromBits(host.value.toInt)
    }
  }

  private def unpackFloat(host: FpxxHost): (Boolean, Int, Int) = {
    val bits = host.value.toLong
    val sign = ((bits >> (host.c.exp_size + host.c.mant_size)) & 0x1L) != 0
    val exp = ((bits >> host.c.mant_size) & ((1L << host.c.exp_size) - 1L)).toInt
    val mant = (bits & ((1L << host.c.mant_size) - 1L)).toInt
    (sign, exp, mant)
  }

  private def decodeFloat(sign: Boolean, exp: Int, mant: Int, cfg: FpxxConfig, algo: FloatAlgo): Double = {
    val bits =
      ((if (sign) 1 else 0) << (cfg.exp_size + cfg.mant_size)) |
        (exp << cfg.mant_size) |
        mant
    algo.fromBits(bits)
  }

  private def formatIntMatrix(matrix: Array[Array[Int]]): String =
    matrix.map(row => row.mkString("[", ", ", "]")).mkString("\n")

  private def formatDoubleMatrix(matrix: Array[Array[Double]]): String = {
    matrix
      .map { row =>
        row
          .map {
            case value if value.isNaN      => "NaN"
            case value if value.isPosInfinity => "Inf"
            case value if value.isNegInfinity => "-Inf"
            case value                    => f"$value%.8g"
          }
          .mkString("[", ", ", "]")
      }
      .mkString("\n")
  }

  private def flattenIntMatrixToDoubles(matrix: Array[Array[Int]]): Seq[Double] =
    matrix.iterator.flatMap(_.iterator).map(_.toDouble).toSeq

  private def flattenDoubleMatrix(matrix: Array[Array[Double]]): Seq[Double] =
    matrix.iterator.flatMap(_.iterator).toSeq

  private def writeReports(
      opts: Options,
      traffic: TrafficCounter.TrafficMetrics,
      numericEvents: NumericEventCounter.NumericEventMetrics,
      precision: PrecisionAnalyzer.PrecisionMetrics,
      caseResults: Seq[CaseResult]
  ): ReportGenerator.ReportArtifacts = {
    val timestamp = ReportGenerator.currentTimestampTag()
    val matrix = ReportGenerator.matrixTag(opts.m, opts.n, opts.k)
    val prefix = ReportGenerator.buildPrefix(opts.floatMode.cliName, opts.mode.cliName, matrix, timestamp)

    val csvHeaders = Seq(
      "experiment",
      "float_mode",
      "resolved_format",
      "mode",
      "m",
      "n",
      "k",
      "case_count",
      "seed",
      "workload_bits",
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
      "total_cycles",
      "ops_per_cycle"
    )

    val csvValues = Seq(
      "float_systolic",
      opts.floatMode.cliName,
      opts.floatMode.resolvedName,
      opts.mode.cliName,
      opts.m.toString,
      opts.n.toString,
      opts.k.toString,
      opts.caseCount.toString,
      opts.seed.toString,
      opts.workloadBits.toString,
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
      traffic.totalCycles.toString,
      traffic.opsPerCycle.toString
    )

    val jsonBody = ReportGenerator.jsonObject(
      Seq(
        "experiment" -> ReportGenerator.jsonString("float_systolic"),
        "options" -> ReportGenerator.jsonObject(
          Seq(
            "floatMode" -> ReportGenerator.jsonString(opts.floatMode.cliName),
            "resolvedFormat" -> ReportGenerator.jsonString(opts.floatMode.resolvedName),
            "mode" -> ReportGenerator.jsonString(opts.mode.cliName),
            "m" -> ReportGenerator.jsonNumber(opts.m),
            "n" -> ReportGenerator.jsonNumber(opts.n),
            "k" -> ReportGenerator.jsonNumber(opts.k),
            "caseCount" -> ReportGenerator.jsonNumber(opts.caseCount),
            "seed" -> ReportGenerator.jsonNumber(opts.seed),
            "workloadBits" -> ReportGenerator.jsonNumber(opts.workloadBits),
            "precisionReference" -> ReportGenerator.jsonString("exact_integer_gemm")
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
                "sourceA" -> ReportGenerator.jsonMatrixInt(caseResult.sourceA),
                "sourceB" -> ReportGenerator.jsonMatrixInt(caseResult.sourceB),
                "quantizedA" -> ReportGenerator.jsonMatrixDouble(caseResult.quantizedA),
                "quantizedB" -> ReportGenerator.jsonMatrixDouble(caseResult.quantizedB),
                "exactReferenceOutput" -> ReportGenerator.jsonMatrixDouble(caseResult.exactReferenceOutput),
                "quantizedReferenceOutput" -> ReportGenerator.jsonMatrixDouble(caseResult.quantizedReferenceOutput),
                "dutOutput" -> ReportGenerator.jsonMatrixDouble(caseResult.dutOutput),
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
      s"""# Floating Experiment Report
         |
         |## Experiment Configuration
         |- Design: `${opts.floatMode.cliName}`
         |- Matrix size: `${opts.m} x ${opts.n} x ${opts.k}`
         |- Float mode: `${opts.floatMode.resolvedName}`
         |- Input mode: `${opts.mode.cliName}`
         |- Workload bits: `${opts.workloadBits}`
         |- Precision reference: `exact integer GEMM`
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

  private def printReport(result: ExperimentResult): Unit = {
    val opts = result.options
    println("===== Floating Systolic Experiment =====")
    println(s"Float mode        : ${opts.floatMode.cliName}")
    println(s"Resolved format   : ${opts.floatMode.resolvedName}")
    println(s"Matrix size (M,N,K): (${opts.m}, ${opts.n}, ${opts.k})")
    println(s"Case count        : ${opts.caseCount}")
    println(s"Seed              : ${opts.seed}")
    println(s"Input mode        : ${opts.mode.cliName}")
    println(s"Workload bits     : ${opts.workloadBits}")
    println(s"Simulator backend : ${if (opts.useVcs) "VCS" else "default"}")
    println(s"CSV report        : ${result.artifacts.csvPath}")
    println(s"JSON report       : ${result.artifacts.jsonPath}")
    println(s"Markdown report   : ${result.artifacts.markdownPath}")
    println(s"Summary report    : ${result.artifacts.summaryMarkdownPath}")
    println(f"MAE               : ${result.precision.mae}%.6f")
    println(f"MSE               : ${result.precision.mse}%.6f")
    println(f"Relative error    : ${result.precision.relativeError}%.6f")
    println(f"Cosine similarity : ${result.precision.cosineSimilarity}%.6f")
    println(s"Total external bytes: ${result.traffic.totalExternalBytes}")
    println(s"Total onchip bytes: ${result.traffic.totalOnchipBytes}")
    println()

    result.cases.foreach { caseResult =>
      println(s"--- Case ${caseResult.index} ---")
      println("Source matrix A:")
      println(formatIntMatrix(caseResult.sourceA))
      println("Source matrix B:")
      println(formatIntMatrix(caseResult.sourceB))
      println("Quantized matrix A:")
      println(formatDoubleMatrix(caseResult.quantizedA))
      println("Quantized matrix B:")
      println(formatDoubleMatrix(caseResult.quantizedB))
      println("Exact reference output tensor:")
      println(formatDoubleMatrix(caseResult.exactReferenceOutput))
      println("Format-quantized reference output tensor:")
      println(formatDoubleMatrix(caseResult.quantizedReferenceOutput))
      println("DUT output tensor:")
      println(formatDoubleMatrix(caseResult.dutOutput))
      println()
    }
  }
}

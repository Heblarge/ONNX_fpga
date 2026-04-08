package runtime.dispatch

import org.scalatest.funsuite.AnyFunSuite
import runtime.bridge.{TorchBridge, Tensor}
import runtime.dispatch.system.SystemDispatcher
import runtime.engine._

/**
 * HWBackendTestSuite — validates HW dispatch against two model variants.
 *
 * 1. OnnxRuntime model (float32, QDQ strategy) — pure PyTorch, no HW dispatch
 * 2. FPGA model (int32, fixed-point) — AcceleratorDispatcher (ref model) HW dispatch
 *
 * To test with real VCS simulation, change HW_BACKEND below.
 *
 * Run via:
 *   sbt "testOnly runtime.dispatch.HWBackendTestSuite"
 */

// mkdir -p sw/scala/test/scala/runtime/dispatch/log && \
// sbt "testOnly runtime.dispatch.HWBackendTestSuite" 2>&1 | tee "sw/scala/test/scala/runtime/dispatch/log/hwtest_$(date +%Y%m%d_%H%M%S).log"

class HWBackendTestSuite extends AnyFunSuite {

  // ==================== OnnxRuntime (float32) Model ====================

  val ORT_MODEL = "sw/scala/test/resources/" +
    "NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_OnnxRuntime.onnx"
  val ORT_DATA = "sw/scala/test/resources/data_ort"

  val ortInputMapping: Map[String, String] = Map(
    "input_seq_pc.pb"  -> "seq_pc",
    "input_seq_pos.pb" -> "seq_pos_fp"
  )
  val ortOutputMapping: Map[String, String] = Map(
    "output_pre_trans.pb" -> "pre_trans_fp",
    "output_rot.pb"       -> "rot",
    "output_trj.pb"       -> "trj"
  )

  // ==================== FPGA (int32) Model ====================

  val FPGA_MODEL = "sw/scala/test/resources/" +
    "NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_FPGA.onnx"
  val FPGA_DATA = "sw/scala/test/resources/data_fpga"

  val fpgaInputMapping: Map[String, String] = Map(
    "input_seq_pc.pb"  -> "seq_pc",
    "input_seq_pos.pb" -> "seq_pos"
  )
  val fpgaOutputMapping: Map[String, String] = Map(
    "output_pre_trans.pb" -> "pre_trans",
    "output_rot.pb"       -> "rot",
    "output_trj.pb"       -> "trj"
  )

  // ==================== Shared Setup ====================

  private def ensureBridge(): Unit = {
    if (!TorchBridge.isInitialized) TorchBridge.init()
  }

  private def runModel(
      modelPath: String, dataDir: String,
      inputMap: Map[String, String],
      hwDispatcher: Option[HWDispatcher] = None
  ): (OnnxGraph, Session, Map[String, Any]) = {
    ensureBridge()
    val workDir = sys.props.getOrElse("user.dir", ".")
    val absModel = s"$workDir/$modelPath"
    val absData = s"$workDir/$dataDir"

    val graph = OnnxLoader.load(absModel)
    val testData = TorchBridge.call("load_test_data", Map("dir" -> absData))
      .asInstanceOf[Map[String, Any]]

    val session = new Session(graph, debug = false)
    session.progressInterval = if (hwDispatcher.isDefined) 100 else 0

    // Feed inputs
    for ((file, inputName) <- inputMap) {
      val fileInfo = testData(file).asInstanceOf[Map[String, Any]]
      val tid = fileInfo("id").toString
      val info = TorchBridge.call("get_info", Map("id" -> tid)).asInstanceOf[Map[String, Any]]
      session.feed(inputName, Tensor.fromInfo(info))
    }

    // Forward
    session.forward(hwDispatcher)

    (graph, session, testData)
  }

  private def assertOutputFloat(
      session: Session, testData: Map[String, Any],
      pbFile: String, outputName: String,
      expectedShape: Array[Int], tolerance: Double = 1e-2
  ): Unit = {
    val computed = session.getTensor(outputName)
    val computedData = computed.toFloatArray()

    val fileInfo = testData(pbFile).asInstanceOf[Map[String, Any]]
    val refTid = fileInfo("id").toString
    val refInfo = TorchBridge.call("get_info", Map("id" -> refTid)).asInstanceOf[Map[String, Any]]
    val refData = Tensor.fromInfo(refInfo).toFloatArray()

    assert(computed.shape.map(_.toInt).sameElements(expectedShape),
      s"Shape mismatch for $outputName: [${computed.shape.mkString(",")}] vs [${expectedShape.mkString(",")}]")

    val n = math.min(computedData.length, refData.length)
    var sumAbsErr = 0.0
    var maxAbsErr = 0.0
    for (i <- 0 until n) {
      val err = math.abs(computedData(i).toDouble - refData(i).toDouble)
      sumAbsErr += err
      if (err > maxAbsErr) maxAbsErr = err
    }
    val meanAbsErr = sumAbsErr / n
    info(f"  $outputName: Mean Abs Error=$meanAbsErr%.6e, Max=$maxAbsErr%.6e")
    assert(meanAbsErr < tolerance, f"Mean abs error $meanAbsErr%.6e exceeds tolerance $tolerance%.1e")
  }

  /** Save a tensor as int32 .pb reference for regression testing. */
  private def saveRefPb(tensor: Tensor, name: String, filename: String): Unit = {
    val workDir = sys.props.getOrElse("user.dir", ".")
    val outDir = s"$workDir/$FPGA_DATA"
    val path = s"$outDir/$filename"
    TorchBridge.call("save_tensor_pb", Map(
      "id" -> tensor.id, "path" -> path, "name" -> name
    ))
    info(s"  Saved reference: $filename")
  }

  private def assertOutputInt(
      session: Session, testData: Map[String, Any],
      pbFile: String, outputName: String,
      expectedShape: Array[Int], tolerance: Double = 5.0
  ): Unit = {
    val computed = session.getTensor(outputName)
    val computedShape = computed.shape.map(_.toInt)

    val fileInfo = testData(pbFile).asInstanceOf[Map[String, Any]]
    val refTid = fileInfo("id").toString
    val refInfo = TorchBridge.call("get_info", Map("id" -> refTid)).asInstanceOf[Map[String, Any]]
    val refTensor = Tensor.fromInfo(refInfo)

    assert(computedShape.sameElements(expectedShape),
      s"Shape mismatch for $outputName: [${computedShape.mkString(",")}] vs [${expectedShape.mkString(",")}]")

    // Compare as Long arrays (fixed-point integer values)
    val computedLong = TorchBridge.call("to_long_array_2d", Map(
      "id" -> computed.id,
      "rows" -> computedShape.init.product,
      "cols" -> computedShape.last
    )).asInstanceOf[Map[String, Any]]

    val refLong = TorchBridge.call("to_long_array_2d", Map(
      "id" -> refTid,
      "rows" -> expectedShape.init.product,
      "cols" -> expectedShape.last
    )).asInstanceOf[Map[String, Any]]

    // Decode the base64 long arrays
    import java.nio.{ByteBuffer, ByteOrder}
    import java.util.Base64
    val totalElements = expectedShape.product
    def decodeLongs(b64: String): Array[Long] = {
      val raw = Base64.getDecoder.decode(b64)
      val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
      val arr = new Array[Long](totalElements)
      buf.asLongBuffer().get(arr)
      arr
    }

    val computedArr = decodeLongs(computedLong("data_b64").toString)
    val refArr = decodeLongs(refLong("data_b64").toString)

    var sumAbsErr = 0.0
    var maxAbsErr = 0.0
    for (i <- 0 until totalElements) {
      val err = math.abs((computedArr(i) - refArr(i)).toDouble)
      sumAbsErr += err
      if (err > maxAbsErr) maxAbsErr = err
    }
    val meanAbsErr = sumAbsErr / totalElements
    info(f"  $outputName: Mean Abs Error=$meanAbsErr%.1f, Max=$maxAbsErr%.1f")
    // Debug: print first 10 values side by side
    if (totalElements <= 100) {
      for (i <- 0 until math.min(totalElements, 30)) {
        info(f"    [$i] computed=${computedArr(i)}%12d  ref=${refArr(i)}%12d  diff=${computedArr(i)-refArr(i)}%12d")
      }
    }
    assert(meanAbsErr < tolerance, f"Mean abs error $meanAbsErr%.1f exceeds tolerance $tolerance%.1f")
  }

  // ==================== ORT Tests (pure PyTorch) ====================

  private lazy val (ortGraph, ortSession, ortData) = {
    runModel(ORT_MODEL, ORT_DATA, ortInputMapping, None)
  }

  test("ORT model loads: 4519 nodes, 2 inputs, 3 outputs") {
    assert(ortGraph.nodes.size == 4519, s"Expected 4519 nodes, got ${ortGraph.nodes.size}")
    assert(ortGraph.userInputs.size == 2)
    assert(ortGraph.outputs.size == 3)
  }

  test("ORT output pre_trans_fp: shape=[1,32,3], mean abs error < 0.01") {
    assertOutputFloat(ortSession, ortData, "output_pre_trans.pb", "pre_trans_fp",
      Array(1, 32, 3))
  }

  test("ORT output rot: shape=[1,32,144], mean abs error < 0.01") {
    assertOutputFloat(ortSession, ortData, "output_rot.pb", "rot",
      Array(1, 32, 144))
  }

  test("ORT output trj: shape=[1,31,72], mean abs error < 0.01") {
    assertOutputFloat(ortSession, ortData, "output_trj.pb", "trj",
      Array(1, 31, 72))
  }

  // ==================== FPGA Tests (HW dispatch) ====================
  //
  // Backend selection — change HW_BACKEND to switch:
  //   "sw"        — AcceleratorDispatcher SW reference model (fast, ~4min)
  //   "verilator" — Verilator RTL simulation
  //   "vcs"       — Synopsys VCS RTL simulation
  //   "system"    — SystemDispatcher full system-level Verilator simulation
  //
  private val HW_BACKEND = "system"

  private lazy val hwBackend = sys.props.getOrElse("hw.backend", HW_BACKEND)

  /**
   * Online node-level ORT cross-comparison callback.
   * After each HW node completes, dequantize its output and compare against ORT float.
   * Prints [ONLINE] tag for real-time monitoring. Threshold alert with [ALERT].
   */
  private def setupOnlineOrtComparison(dispatcher: AcceleratorDispatcher): Unit = {
    // Ensure ORT model is loaded first
    val _ = ortSession
    val alertThreshold = 0.1 // mean abs error threshold for alert
    dispatcher.onNodeComplete = Some { (hwOpCount, node, outputIds, graph) =>
      for ((outName, outId) <- node.outputs.zip(outputIds)) {
        // Try "_fp" suffix first (QDQ-quantized ops), then fall back to original name
        // (non-quantized ops like Gemm/MatMul→Activation chains without QDQ in between)
        val (ortName, ortTidOpt) = {
          val fpName = outName + "_fp"
          ortSession.getTensorId(fpName) match {
            case some @ Some(_) => (fpName, some)
            case None => (outName, ortSession.getTensorId(outName))
          }
        }
        ortTidOpt match {
          case Some(ortTid) =>
            try {
              val ortInfo = TorchBridge.call("get_info", Map("id" -> ortTid)).asInstanceOf[Map[String, Any]]
              val ortTensor = Tensor.fromInfo(ortInfo)
              val ortFloat = ortTensor.toFloatArray().map(_.toDouble)
              // Get FPGA output shift for dequantization
              val shift = graph.producerMap.get(outName).flatMap { pn =>
                val s = pn.fpgaOutShift; if (s.nonEmpty && s.head != 0L) Some(s.head) else None
              }.getOrElse(0L)
              val scale = math.pow(2.0, shift.toDouble)
              // Get FPGA tensor data
              val fpgaInfo = TorchBridge.call("get_info", Map("id" -> outId)).asInstanceOf[Map[String, Any]]
              val fpgaTensor = Tensor.fromInfo(fpgaInfo)
              val fpgaFloat = fpgaTensor.toFloatArray().map(_.toDouble / scale)
              val n = math.min(fpgaFloat.length, ortFloat.length)
              var sumAbsErr = 0.0; var maxAbsErr = 0.0
              var sumFpga = 0.0; var sumOrt = 0.0
              for (i <- 0 until n) {
                val e = math.abs(fpgaFloat(i) - ortFloat(i))
                sumAbsErr += e; if (e > maxAbsErr) maxAbsErr = e
                sumFpga += math.abs(fpgaFloat(i)); sumOrt += math.abs(ortFloat(i))
              }
              val meanAbsErr = sumAbsErr / n
              val ratio = if (sumOrt > 1e-12) (sumFpga / sumOrt) else 0.0
              val tag = if (meanAbsErr > alertThreshold) "ALERT" else "OK"
              val src = if (ortName.endsWith("_fp")) "" else " (raw)"
              println(f"[ONLINE] HW#$hwOpCount%4d ${node.opType}%-10s [$tag] meanErr=$meanAbsErr%.6f maxErr=$maxAbsErr%.6f ratio=$ratio%.4f  '${node.name}'$src")
              if (meanAbsErr > alertThreshold) {
                println(f"  ^^^^ DIVERGENCE DETECTED at '${node.name}' ($outName) — consider stopping")
              }
            } catch {
              case e: Exception =>
                println(f"[ONLINE] HW#$hwOpCount%4d ${node.opType}%-10s [ERR] ${e.getMessage}")
            }
          case None =>
            println(f"[ONLINE] HW#$hwOpCount%4d ${node.opType}%-10s [SKIP] no ORT counterpart '$outName'")
        }
      }
    }
  }

  private def makeRtlDispatcher(backendName: String): AcceleratorDispatcher = backendName match {
    case "vcs" =>
      println("[INFO] Using VCS RTL simulation backend (hw.backend=vcs)")
      val d = new VCSDispatcher("int32")
      d.crossCheck = true
      d.crossCheckMaxErr = 0
      d.crossCheckAbortAfter = 0
      d._progressInterval = 1
      setupOnlineOrtComparison(d)
      d
    case "verilator" =>
      println("[INFO] Using Verilator RTL simulation backend (hw.backend=verilator)")
      val d = new VerilatorDispatcher("int32")
      d.crossCheck = true
      d.crossCheckMaxErr = 0
      d.crossCheckAbortAfter = 0
      d._progressInterval = 1
      setupOnlineOrtComparison(d)
      d
    case "system" =>
      println("[INFO] Using SystemDispatcher full system-level simulation (hw.backend=system)")
      val acfg = Accelerator.AcceleratorSimInterfaceVerilator.acceleratorCfg
      val sysCfg = WrapForFPGA.SystemWrapperConfig(
        fpgaCfg = WrapForFPGA.FPGACfg(
          UIDWidth = acfg.UIDWidth,
          AddressWidth = acfg.AddressWidth,
          ShapeWidth = acfg.ShapeWidth,
          systolicArraySideNum = acfg.systolicArraySideNum,
          elementWidth = acfg.elementWidth,
          intWidth = acfg.intWidth,
          systolicArrayInFifoDepth = acfg.systolicArrayInFifoDepth,
          systolicArrayOutFifoDepth = acfg.systolicArrayOutFifoDepth,
          systolicArrayInstFifoDepth = acfg.systolicArrayInstFifoDepth,
          activationOutFifoDepth = acfg.activationOutFifoDepth,
          slicedInstFifoDepth = acfg.slicedInstFifoDepth,
          numCores = acfg.numCores
        ),
        cacheAddrWidth = 14,  // 2^14 * 16 = 262144 elements = 512×512
        ddrAddrWidth = 64,
        dmaMaxBurstLen = 256
      )
      val d = new SystemDispatcher(sysCfg, "int32")
      d.crossCheck = true
      d.crossCheckMaxErr = 0
      d.crossCheckAbortAfter = 0
      // d.dmaLogLevel = 2  // 详细 DMA 日志
      d._progressInterval = 1
      setupOnlineOrtComparison(d)
      d
    case _ =>
      println("[INFO] Using SW reference model backend (default)")
      new AcceleratorDispatcher("int32")
  }

  private var _systemDispatcher: Option[SystemDispatcher] = None

  private lazy val (fpgaGraph, fpgaSession, fpgaData) = {
    val dispatcher = makeRtlDispatcher(hwBackend)
    dispatcher match {
      case sd: SystemDispatcher => _systemDispatcher = Some(sd)
      case _ =>
    }
    val result = runModel(FPGA_MODEL, FPGA_DATA, fpgaInputMapping, Some(dispatcher))
    // 全图执行完毕后立即 shutdown SystemDispatcher（打印性能统计）
    _systemDispatcher.foreach(_.shutdown())
    result
  }

  test("FPGA model loads: 1321 nodes, 2 inputs, 3 outputs") {
    assert(fpgaGraph.nodes.size == 1321, s"Expected 1321 nodes, got ${fpgaGraph.nodes.size}")
    assert(fpgaGraph.userInputs.size == 2)
    assert(fpgaGraph.outputs.size == 3)
  }

  test("FPGA output pre_trans: shape=[1,32,3]") {
    val computed = fpgaSession.getTensor("pre_trans")
    val computedShape = computed.shape.map(_.toInt)
    assert(computedShape.sameElements(Array(1, 32, 3)),
      s"Shape mismatch: [${computedShape.mkString(",")}] vs [1,32,3]")

    // Validate output range using dynamic shift from graph
    val shift = getOutputShift(fpgaGraph, "pre_trans")
    val scale = math.pow(2.0, shift.toDouble)
    val floatVals = computed.toFloatArray()
    val maxAbs = floatVals.map(v => math.abs(v.toDouble / scale)).max
    info(f"  pre_trans: shift=$shift, max abs dequantized value = $maxAbs%.4f")
    assert(maxAbs < 10.0, f"Output values unreasonably large: maxAbs=$maxAbs%.1f")

    // Save as new reference for regression testing
    saveRefPb(computed, "pre_trans", "output_pre_trans.pb")
  }

  test("FPGA output rot: shape=[1,32,144]") {
    val computed = fpgaSession.getTensor("rot")
    val computedShape = computed.shape.map(_.toInt)
    assert(computedShape.sameElements(Array(1, 32, 144)),
      s"Shape mismatch: [${computedShape.mkString(",")}] vs [1,32,144]")
    val shift = getOutputShift(fpgaGraph, "rot")
    val scale = math.pow(2.0, shift.toDouble)
    val maxAbs = computed.toFloatArray().map(v => math.abs(v.toDouble / scale)).max
    info(f"  rot: shift=$shift, max abs dequantized value = $maxAbs%.4f")
    assert(maxAbs < 10.0, f"Output values unreasonably large: maxAbs=$maxAbs%.1f")
    saveRefPb(computed, "rot", "output_rot.pb")
  }

  test("FPGA output trj: shape=[1,31,72]") {
    val computed = fpgaSession.getTensor("trj")
    val computedShape = computed.shape.map(_.toInt)
    assert(computedShape.sameElements(Array(1, 31, 72)),
      s"Shape mismatch: [${computedShape.mkString(",")}] vs [1,31,72]")
    val shift = getOutputShift(fpgaGraph, "trj")
    val scale = math.pow(2.0, shift.toDouble)
    val maxAbs = computed.toFloatArray().map(v => math.abs(v.toDouble / scale)).max
    info(f"  trj: shift=$shift, max abs dequantized value = $maxAbs%.4f")
    assert(maxAbs < 10.0, f"Output values unreasonably large: maxAbs=$maxAbs%.1f")
    saveRefPb(computed, "trj", "output_trj.pb")
  }

  // ==================== Cross-Comparison: FPGA (dequantized) vs ORT (float) ====================

  /** Get the output shift for a tensor by looking up its producer node in the graph. */
  private def getOutputShift(graph: OnnxGraph, tensorName: String): Long = {
    graph.producerMap.get(tensorName) match {
      case Some(pn) =>
        val shifts = pn.fpgaOutShift
        if (shifts.nonEmpty && shifts.head != 0L) shifts.head
        else throw new RuntimeException(s"No fpga_out_shift on producer of '$tensorName' (node '${pn.name}')")
      case None =>
        throw new RuntimeException(s"No producer node found for '$tensorName'")
    }
  }

  /**
   * Compare FPGA int32 output (dequantized via shift) against ORT float output.
   * Prints per-element comparison for small tensors and summary statistics.
   */
  private def assertCrossComparison(
      fpgaOutputName: String, ortOutputName: String,
      expectedShape: Array[Int], toleranceMean: Double = 0.05, toleranceMax: Double = 0.5
  ): Unit = {
    // Get FPGA output and its shift from graph
    val fpgaTensor = fpgaSession.getTensor(fpgaOutputName)
    val shift = getOutputShift(fpgaGraph, fpgaOutputName)
    val scale = math.pow(2.0, shift.toDouble)
    info(f"  $fpgaOutputName: fpga_out_shift=$shift, scale=2^$shift = $scale%.0f")

    val fpgaShape = fpgaTensor.shape.map(_.toInt)
    assert(fpgaShape.sameElements(expectedShape),
      s"FPGA shape mismatch: [${fpgaShape.mkString(",")}] vs [${expectedShape.mkString(",")}]")

    // Get ORT output
    val ortTensor = ortSession.getTensor(ortOutputName)
    val ortShape = ortTensor.shape.map(_.toInt)
    assert(ortShape.sameElements(expectedShape),
      s"ORT shape mismatch: [${ortShape.mkString(",")}] vs [${expectedShape.mkString(",")}]")

    // Dequantize FPGA: float_value = int_value / 2^shift
    val fpgaFloat = fpgaTensor.toFloatArray().map(_.toDouble / scale)
    val ortFloat = ortTensor.toFloatArray().map(_.toDouble)

    val n = fpgaFloat.length
    var sumAbsErr = 0.0
    var maxAbsErr = 0.0
    var maxAbsErrIdx = 0
    var sumRelErr = 0.0
    var relErrCount = 0
    for (i <- 0 until n) {
      val err = math.abs(fpgaFloat(i) - ortFloat(i))
      sumAbsErr += err
      if (err > maxAbsErr) { maxAbsErr = err; maxAbsErrIdx = i }
      if (math.abs(ortFloat(i)) > 1e-6) {
        sumRelErr += err / math.abs(ortFloat(i))
        relErrCount += 1
      }
    }
    val meanAbsErr = sumAbsErr / n
    val meanRelErr = if (relErrCount > 0) sumRelErr / relErrCount else 0.0

    info(f"  $fpgaOutputName vs $ortOutputName ($n elements):")
    info(f"    Mean Abs Error = $meanAbsErr%.6e")
    info(f"    Max  Abs Error = $maxAbsErr%.6e (at index $maxAbsErrIdx)")
    info(f"    Mean Rel Error = ${meanRelErr * 100}%.2f%%")

    // Print first few and worst elements for inspection
    val printCount = math.min(10, n)
    info(s"    First $printCount values:")
    for (i <- 0 until printCount) {
      val err = fpgaFloat(i) - ortFloat(i)
      info(f"      [$i] fpga=${fpgaFloat(i)}%12.6f  ort=${ortFloat(i)}%12.6f  diff=$err%+.6e")
    }
    if (maxAbsErrIdx >= printCount) {
      val err = fpgaFloat(maxAbsErrIdx) - ortFloat(maxAbsErrIdx)
      info(f"      [$maxAbsErrIdx] fpga=${fpgaFloat(maxAbsErrIdx)}%12.6f  ort=${ortFloat(maxAbsErrIdx)}%12.6f  diff=$err%+.6e  <-- max error")
    }

    assert(meanAbsErr < toleranceMean,
      f"Cross-comparison $fpgaOutputName: mean abs error $meanAbsErr%.6e exceeds $toleranceMean%.1e")
    assert(maxAbsErr < toleranceMax,
      f"Cross-comparison $fpgaOutputName: max abs error $maxAbsErr%.6e exceeds $toleranceMax%.1e")
  }

  test("Cross: FPGA pre_trans vs ORT pre_trans_fp") {
    assertCrossComparison("pre_trans", "pre_trans_fp", Array(1, 32, 3),
      toleranceMean = 0.05, toleranceMax = 0.2)
  }

  // model2 的 rot/trj 经 MinGRU 递归层计算，INT 定点量化误差在时间步间累积放大。
  // SW 基线: rot meanErr=3.35e-3, maxErr=1.69e-2; trj meanErr=1.34e-4, maxErr=1.54e-3
  // 给 5 倍余量用于 HW 截断 vs SW 四舍五入的微小差异。
  test("Cross: FPGA rot vs ORT rot") {
    assertCrossComparison("rot", "rot", Array(1, 32, 144),
      toleranceMean = 0.02, toleranceMax = 0.1)
  }

  test("Cross: FPGA trj vs ORT trj") {
    assertCrossComparison("trj", "trj", Array(1, 31, 72),
      toleranceMean = 0.001, toleranceMax = 0.01)
  }
}

package runtime.dispatch

import org.scalatest.funsuite.AnyFunSuite
import runtime.bridge.{TorchBridge, Tensor}
import runtime.dispatch.system.SystemDispatcher
import runtime.engine._

/**
 * SingleOpReplayTestSuite — 混合 SW→HW 硬件仿真验证。
 *
 * 两种模式（通过 -Dreplay.mode 切换）：
 *
 * === "hybrid" 模式（默认） ===
 *   从指定算子开始用硬件仿真跑剩余全图，观察误差累积：
 *   1. 目标算子之前的节点 → AcceleratorDispatcher（SW 快速参考模型, ~5min）
 *   2. 目标算子及之后的节点 → SystemDispatcher（HW Verilator RTL 仿真）
 *   3. 每个 HW 节点执行后，与 ORT float32 参考做 online 对比
 *
 * === "single" 模式 ===
 *   仅跑单个目标算子通过硬件，用于精确定位单算子问题：
 *   1. 整图 SW 前向 → 提取目标算子输入/输出张量
 *   2. 仅跑目标算子通过 HW → 逐元素比对
 *
 * 用法：
 *   # hybrid 模式（默认）：从 PPQ_Operation_51 开始 HW 仿真
 *   sbt -Dreplay.startFrom=PPQ_Operation_51 "testOnly runtime.dispatch.SingleOpReplayTestSuite"
 *
 *   # single 模式：仅跑单个算子
 *   sbt -Dreplay.mode=single -Dreplay.target=PPQ_Operation_115 "testOnly runtime.dispatch.SingleOpReplayTestSuite"
 */
class SingleOpReplayTestSuite extends AnyFunSuite {

  // ==================== 模型路径 ====================

  val FPGA_MODEL = "sw/scala/test/resources/" +
    "NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_FPGA.onnx"
  val FPGA_DATA = "sw/scala/test/resources/data_fpga"

  val fpgaInputMapping: Map[String, String] = Map(
    "input_seq_pc.pb"  -> "seq_pc",
    "input_seq_pos.pb" -> "seq_pos"
  )

  // ORT 参考模型（用于 online 对比）
  val ORT_MODEL = "sw/scala/test/resources/" +
    "NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_OnnxRuntime.onnx"
  val ORT_DATA = "sw/scala/test/resources/data_ort"

  val ortInputMapping: Map[String, String] = Map(
    "input_seq_pc.pb"  -> "seq_pc",
    "input_seq_pos.pb" -> "seq_pos_fp"
  )

  // ==================== 配置 ====================

  /** 运行模式: "hybrid"（从某算子开始 HW）或 "single"（仅单算子） */
  private val MODE = sys.props.getOrElse("replay.mode", "hybrid")

  /** hybrid 模式: 从此算子开始用 HW。可通过 -Dreplay.startFrom=xxx 覆盖 */
  private val START_FROM = sys.props.getOrElse("replay.startFrom", "PPQ_Operation_114")

  /** single 模式: 目标算子名。可通过 -Dreplay.target=xxx 覆盖 */
  private val TARGET_NODE = sys.props.getOrElse("replay.target", "PPQ_Operation_114")

  // ==================== 共用 ====================

  private def ensureBridge(): Unit = {
    if (!TorchBridge.isInitialized) TorchBridge.init()
  }

  private def makeSystemDispatcher(): SystemDispatcher = {
    import Accelerator.AcceleratorSimInterfaceVerilator
    import WrapForFPGA._

    val acfg = AcceleratorSimInterfaceVerilator.acceleratorCfg
    val sysCfg = SystemWrapperConfig(
      fpgaCfg = FPGACfg(
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
      cacheAddrWidth = 14,
      ddrAddrWidth = 64,
      dmaMaxBurstLen = 256
    )
    val d = new SystemDispatcher(sysCfg, "int32")
    d.crossCheck = true
    d.crossCheckMaxErr = 0
    d.crossCheckAbortAfter = 0
    d.dmaLogLevel = 2
    d._progressInterval = 1
    d
  }

  // ==================== ORT 参考 Session（lazy，hybrid 模式用到） ====================

  private lazy val ortSession: Session = {
    ensureBridge()
    val workDir = sys.props.getOrElse("user.dir", ".")
    val absModel = s"$workDir/$ORT_MODEL"
    val absData  = s"$workDir/$ORT_DATA"

    val graph = OnnxLoader.load(absModel)
    val testData = TorchBridge.call("load_test_data", Map("dir" -> absData))
      .asInstanceOf[Map[String, Any]]

    println(s"[ORT] Loading ORT reference model...")
    val session = new Session(graph, debug = false)

    for ((file, inputName) <- ortInputMapping) {
      val fileInfo = testData(file).asInstanceOf[Map[String, Any]]
      val tid = fileInfo("id").toString
      val info = TorchBridge.call("get_info", Map("id" -> tid)).asInstanceOf[Map[String, Any]]
      session.feed(inputName, Tensor.fromInfo(info))
    }

    println(s"[ORT] Running ORT forward pass...")
    val t0 = System.currentTimeMillis()
    session.forward(None) // 纯 PyTorch SW
    val elapsed = (System.currentTimeMillis() - t0) / 1000.0
    println(f"[ORT] Done in $elapsed%.1fs")
    session
  }

  /**
   * Online ORT 对比回调。每个 HW 节点执行后与 ORT float 参考对比。
   * ORT session 在第一次回调时 lazy 加载，避免阻塞 SW 阶段。
   */
  private def setupOnlineOrtComparison(dispatcher: AcceleratorDispatcher, graph: OnnxGraph): Unit = {
    var ortLoaded = false
    val alertThreshold = 0.1
    dispatcher.onNodeComplete = Some { (hwOpCount, node, outputIds, _graph) =>
      if (!ortLoaded) {
        val _ = ortSession // lazy load on first HW callback
        ortLoaded = true
      }
      for ((outName, outId) <- node.outputs.zip(outputIds)) {
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
              val shift = graph.producerMap.get(outName).flatMap { pn =>
                val s = pn.fpgaOutShift; if (s.nonEmpty && s.head != 0L) Some(s.head) else None
              }.getOrElse(0L)
              val scale = math.pow(2.0, shift.toDouble)
              val fpgaInfo = TorchBridge.call("get_info", Map("id" -> outId)).asInstanceOf[Map[String, Any]]
              val fpgaTensor = Tensor.fromInfo(fpgaInfo)
              val fpgaFloat = fpgaTensor.toFloatArray().map(_.toDouble / scale)
              val n = math.min(fpgaFloat.length, ortFloat.length)
              var sumAbsErr = 0.0; var maxAbsErr = 0.0
              for (i <- 0 until n) {
                val e = math.abs(fpgaFloat(i) - ortFloat(i))
                sumAbsErr += e; if (e > maxAbsErr) maxAbsErr = e
              }
              val meanAbsErr = sumAbsErr / n
              val tag = if (meanAbsErr > alertThreshold) "ALERT" else "OK"
              println(f"[ONLINE] HW#$hwOpCount%4d ${node.opType}%-10s [$tag] meanErr=$meanAbsErr%.6f maxErr=$maxAbsErr%.6f  '${node.name}'")
              if (meanAbsErr > alertThreshold)
                println(f"  ^^^^ DIVERGENCE at '${node.name}' ($outName)")
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

  // ==================== hybrid 模式：混合 Dispatcher ====================

  /**
   * HybridDispatcher — 在目标节点之前用 swDisp（快速 SW），之后用 hwDisp（HW RTL 仿真）。
   * 非 FPGA 量化节点（Reshape/Transpose 等）由 Session 自动走 PyTorch，不经过此处。
   */
  private class HybridDispatcher(
    swDisp: AcceleratorDispatcher,
    hwDisp: SystemDispatcher,
    startNodeName: String,
    expectedSwOps: Int = 0
  ) extends HWDispatcher {
    private var _hwMode = false
    private var _swOps = 0
    private var _hwOps = 0
    private var _switchedAt = ""
    private val _swStartTime = System.currentTimeMillis()
    /** SW 阶段打印间隔 */
    var swProgressInterval: Int = 10

    def hwMode: Boolean = _hwMode
    def swOps: Int = _swOps
    def hwOps: Int = _hwOps
    def switchedAt: String = _switchedAt

    override def shouldDispatch(node: GraphNode): Boolean = {
      if (!_hwMode && node.name == startNodeName) {
        _hwMode = true
        _switchedAt = node.name
        val swElapsed = (System.currentTimeMillis() - _swStartTime) / 1000.0
        println(s"\n${"=" * 80}")
        println(f"  [SWITCH] SW → HW 切换！ at '${node.name}' (SW 阶段: ${_swOps} ops in $swElapsed%.1fs)")
        println(s"  [SWITCH] 从这里开始使用 Verilator RTL 硬件仿真...")
        println(s"${"=" * 80}\n")
      }
      if (_hwMode) hwDisp.shouldDispatch(node)
      else swDisp.shouldDispatch(node)
    }

    override def execute(node: GraphNode, inputIds: Seq[String], graph: OnnxGraph): Seq[String] = {
      if (_hwMode) {
        _hwOps += 1
        hwDisp.execute(node, inputIds, graph)
      } else {
        _swOps += 1
        if (swProgressInterval > 0 && (_swOps % swProgressInterval == 0 || _swOps == 1)) {
          val elapsed = (System.currentTimeMillis() - _swStartTime) / 1000.0
          val pct = if (expectedSwOps > 0) _swOps * 100 / expectedSwOps else 0
          println(f"  [SW-FAST] ${_swOps}%d/$expectedSwOps ($pct%d%%) $elapsed%.1fs  ${node.opType} '${node.name}'")
        }
        swDisp.execute(node, inputIds, graph)
      }
    }
  }

  // ==================== Test: hybrid 模式 ====================

  test(s"Hybrid SW→HW from $START_FROM") {
    assume(MODE == "hybrid", s"Skipped: replay.mode=$MODE (need 'hybrid')")

    ensureBridge()
    val workDir = sys.props.getOrElse("user.dir", ".")
    val absModel = s"$workDir/$FPGA_MODEL"
    val absData  = s"$workDir/$FPGA_DATA"

    val graph = OnnxLoader.load(absModel)
    val testData = TorchBridge.call("load_test_data", Map("dir" -> absData))
      .asInstanceOf[Map[String, Any]]

    // 验证目标节点存在
    require(graph.nodes.exists(_.name == START_FROM),
      s"Node '$START_FROM' not found. Available: ${graph.nodes.map(_.name).take(20).mkString(", ")}...")

    // 找到目标节点在拓扑序中的位置
    val topoIdx = graph.topologicalOrder.indexWhere(_.name == START_FROM)
    val totalNodes = graph.topologicalOrder.size
    val hwEligibleBefore = graph.topologicalOrder.take(topoIdx).count { n =>
      val hasInShift = n.getInts("fpga_in_shift").exists(_ != 0L)
      val hasOutShift = n.getInts("fpga_out_shift").exists(_ != 0L)
      hasInShift || hasOutShift
    }
    val hwEligibleAfter = graph.topologicalOrder.drop(topoIdx).count { n =>
      val hasInShift = n.getInts("fpga_in_shift").exists(_ != 0L)
      val hasOutShift = n.getInts("fpga_out_shift").exists(_ != 0L)
      hasInShift || hasOutShift
    }

    println(s"\n${"=" * 80}")
    println(s"  [HYBRID] SW→HW split at '${START_FROM}'")
    println(s"  [HYBRID] Total nodes: $totalNodes, split point: #$topoIdx")
    println(s"  [HYBRID] Before (SW): $topoIdx nodes ($hwEligibleBefore HW-eligible)")
    println(s"  [HYBRID] After  (HW): ${totalNodes - topoIdx} nodes ($hwEligibleAfter HW-eligible)")
    println(s"${"=" * 80}\n")

    // 创建两个 Dispatcher
    val swDisp = new AcceleratorDispatcher("int32")
    swDisp._progressInterval = 0  // 禁用 AccDisp 内部进度（避免显示误导性的 [HW #N]）

    val hwDisp = makeSystemDispatcher()
    setupOnlineOrtComparison(hwDisp, graph)  // ORT 不会立即加载，等到第一个 HW 回调时才 lazy load

    val hybrid = new HybridDispatcher(swDisp, hwDisp, START_FROM, expectedSwOps = hwEligibleBefore)

    // 创建 Session 并喂入输入
    // 注意：禁用 Session 自带进度条，因为它会把 SW 阶段的节点标记为 [HW]（误导）
    // 改用 HybridDispatcher 内部的 [SW-FAST] 进度
    val session = new Session(graph, debug = false)
    session.progressInterval = 0

    for ((file, inputName) <- fpgaInputMapping) {
      val fileInfo = testData(file).asInstanceOf[Map[String, Any]]
      val tid = fileInfo("id").toString
      val info = TorchBridge.call("get_info", Map("id" -> tid)).asInstanceOf[Map[String, Any]]
      session.feed(inputName, Tensor.fromInfo(info))
    }

    // 执行！
    println(s"\n${'▸'} [SW-FAST 阶段] 使用 AcceleratorDispatcher (纯软件) 快速跑前 $hwEligibleBefore 个量化算子...")
    println(s"${'▸'} 到达 '$START_FROM' 后自动切换为 Verilator RTL 硬件仿真")
    println()
    val t0 = System.currentTimeMillis()
    try {
      session.forward(Some(hybrid))
    } finally {
      val elapsed = (System.currentTimeMillis() - t0) / 1000.0
      println(f"\n${"=" * 80}")
      println(f"  [HYBRID] Forward completed in $elapsed%.1fs")
      println(f"  [HYBRID] SW ops: ${hybrid.swOps}, HW ops: ${hybrid.hwOps}")
      println(f"  [HYBRID] Switched at: '${hybrid.switchedAt}'")
      println(f"${"=" * 80}\n")

      hwDisp.shutdown()
    }
  }

  // ==================== Test: single 模式 ====================

  /** SW 全图前向（single 模式用） */
  private lazy val (swGraph, swSession) = {
    ensureBridge()
    val workDir = sys.props.getOrElse("user.dir", ".")
    val absModel = s"$workDir/$FPGA_MODEL"
    val absData  = s"$workDir/$FPGA_DATA"

    val graph = OnnxLoader.load(absModel)
    val testData = TorchBridge.call("load_test_data", Map("dir" -> absData))
      .asInstanceOf[Map[String, Any]]

    println(s"[SingleOpReplay] Loading model: $FPGA_MODEL")
    println(s"[SingleOpReplay] Graph: ${graph.nodes.size} nodes")

    val swDispatcher = new AcceleratorDispatcher("int32")
    swDispatcher._progressInterval = 200

    val session = new Session(graph, debug = false)
    session.progressInterval = 200

    for ((file, inputName) <- fpgaInputMapping) {
      val fileInfo = testData(file).asInstanceOf[Map[String, Any]]
      val tid = fileInfo("id").toString
      val info = TorchBridge.call("get_info", Map("id" -> tid)).asInstanceOf[Map[String, Any]]
      session.feed(inputName, Tensor.fromInfo(info))
    }

    println(s"[SingleOpReplay] Running SW forward pass...")
    val t0 = System.currentTimeMillis()
    session.forward(Some(swDispatcher))
    val elapsed = (System.currentTimeMillis() - t0) / 1000.0
    println(f"[SingleOpReplay] SW forward done in $elapsed%.1fs")

    (graph, session)
  }

  private def extractLong2D(tensorName: String): (Array[Array[Long]], Array[Long]) = {
    val tid = swSession.getTensorId(tensorName).getOrElse(
      throw new NoSuchElementException(s"Tensor '$tensorName' not found in SW session"))

    val info = TorchBridge.call("get_info", Map("id" -> tid)).asInstanceOf[Map[String, Any]]
    val shape = info("shape").asInstanceOf[List[Any]].map(OnnxLoader.toLong).toArray

    val lastDim = shape.last.toInt
    val leadingDims = if (shape.length > 1) shape.dropRight(1).product.toInt else 1
    val rows = leadingDims
    val cols = lastDim

    val tmpDisp = new AcceleratorDispatcher("int32")
    val data = tmpDisp.pullToLong2D(tid, rows, cols)
    (data, shape)
  }

  test(s"Single-op HW replay: $TARGET_NODE") {
    assume(MODE == "single", s"Skipped: replay.mode=$MODE (need 'single')")

    val graph = swGraph
    val session = swSession

    val node = graph.nodes.find(_.name == TARGET_NODE).getOrElse(
      fail(s"Node '$TARGET_NODE' not found in graph. Available: ${graph.nodes.map(_.name).take(20).mkString(", ")}..."))

    println(s"\n${"=" * 80}")
    println(s"  [REPLAY] Target: ${node.opType} '${node.name}'")
    println(s"  [REPLAY] Inputs:  ${node.inputs.toSeq}")
    println(s"  [REPLAY] Outputs: ${node.outputs.toSeq}")
    println(s"  [REPLAY] fpga_in_shift:  ${node.fpgaInShift}")
    println(s"  [REPLAY] fpga_out_shift: ${node.fpgaOutShift}")
    println(s"${"=" * 80}\n")

    println("[REPLAY] Extracting SW reference tensors...")
    val inputData = node.inputs.zipWithIndex.map { case (inName, idx) =>
      if (inName.isEmpty) {
        println(s"  input[$idx]: (empty)")
        (null, null)
      } else {
        val (data, shape) = extractLong2D(inName)
        println(f"  input[$idx] '$inName': shape=[${shape.mkString(",")}] → ${data.length}×${data(0).length}")
        val flatVals = data.flatten.take(10)
        println(s"    first10: [${flatVals.mkString(", ")}]")
        val allVals = data.flatten
        println(f"    stats: mean=${allVals.map(_.toDouble).sum / allVals.length}%.2f absMax=${allVals.map(math.abs).max} count=${allVals.length}")
        (data, shape)
      }
    }

    val (swOutput, swOutShape) = extractLong2D(node.outputs(0))
    val swOutRows = swOutput.length
    val swOutCols = swOutput(0).length
    println(f"  SW output '${node.outputs(0)}': shape=[${swOutShape.mkString(",")}] → ${swOutRows}×${swOutCols}")
    val swStats = swOutput.flatten
    println(f"    stats: mean=${swStats.map(_.toDouble).sum / swStats.length}%.2f absMax=${swStats.map(math.abs).max}")

    println("\n[REPLAY] Creating SystemDispatcher for HW replay...")
    val hwDisp = makeSystemDispatcher()

    try {
      val hwInputIds = inputData.zipWithIndex.map { case ((data, _), _) =>
        if (data == null) null
        else hwDisp.pushFromLong2D(data, data.length, data(0).length, "int32")
      }

      println(s"[REPLAY] Executing single node through SystemDispatcher...")
      val t0 = System.currentTimeMillis()
      val hwOutputIds = hwDisp.execute(node, hwInputIds.toSeq, graph)
      val elapsed = (System.currentTimeMillis() - t0) / 1000.0
      println(f"[REPLAY] HW execution done in $elapsed%.1fs")

      println(s"\n${"=" * 80}")
      println(s"  [COMPARE] HW vs SW element-by-element comparison")
      println(s"${"=" * 80}")

      val tmpDisp2 = new AcceleratorDispatcher("int32")
      val hwOutput = tmpDisp2.pullToLong2D(hwOutputIds.head, swOutRows, swOutCols)

      var totalDiff = 0L; var maxDiff = 0L; var maxDiffPos = (0, 0)
      var diffCount = 0; var zeroDiffCount = 0

      for (i <- 0 until swOutRows; j <- 0 until swOutCols) {
        val diff = math.abs(hwOutput(i)(j) - swOutput(i)(j))
        totalDiff += diff
        if (diff > maxDiff) { maxDiff = diff; maxDiffPos = (i, j) }
        if (diff > 0) diffCount += 1 else zeroDiffCount += 1
      }

      val totalElems = swOutRows * swOutCols
      val meanDiff = totalDiff.toDouble / totalElems
      val tos = node.fpgaOutShift.headOption.getOrElse(0L)
      val scale = math.pow(2.0, tos.toDouble)

      println(f"\n  Total elements:    $totalElems")
      println(f"  Exact matches:     $zeroDiffCount (${"%.1f".format(zeroDiffCount * 100.0 / totalElems)}%%)")
      println(f"  Elements with diff: $diffCount")
      println(f"  Max  int diff:     $maxDiff at (${maxDiffPos._1}, ${maxDiffPos._2})")
      println(f"  Mean float diff:   ${meanDiff / scale}%.6e (/ 2^$tos)")
      println(f"  Max  float diff:   ${maxDiff.toDouble / scale}%.6e (/ 2^$tos)")

      val (wi, wj) = maxDiffPos
      println(f"\n  Worst diff region (around [$wi,$wj]):")
      for (i <- math.max(0, wi - 2) until math.min(swOutRows, wi + 3)) {
        for (j <- math.max(0, wj - 2) until math.min(swOutCols, wj + 3)) {
          val diff = hwOutput(i)(j) - swOutput(i)(j)
          val marker = if (i == wi && j == wj) " <--MAX" else ""
          println(f"    [$i,$j] hw=${hwOutput(i)(j)}%12d  sw=${swOutput(i)(j)}%12d  diff=$diff%+d$marker")
        }
      }

      if (maxDiff == 0) println("\n  [DIAGNOSIS] HW and SW identical (integer-exact).")
      else if (maxDiff <= 1) println("\n  [DIAGNOSIS] Max 1 LSB diff (rounding). Expected.")
      else println(f"\n  [DIAGNOSIS] Max diff=$maxDiff LSBs. Check Slicer/PE/DMA.")

      println(s"\n${"=" * 80}\n")
    } finally {
      hwDisp.shutdown()
    }
  }
}

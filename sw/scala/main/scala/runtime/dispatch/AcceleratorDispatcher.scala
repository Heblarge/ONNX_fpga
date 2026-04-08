package runtime.dispatch

import Accelerator.{AcceleratorSimInterface, InstJavaTODO}
import Util._
import runtime.bridge.{TorchBridge, Tensor}
import runtime.engine._
import runtime.dispatch.ops._

import java.nio.{ByteBuffer, ByteOrder}
import java.util.Base64
import scala.collection.mutable

/**
 * AcceleratorDispatcher — Routes eligible ONNX nodes to the FPGA accelerator.
 *
 * A node is dispatched to HW if it has non-zero fpga_in_shift attributes,
 * indicating quantized fixed-point computation. Otherwise, PyTorch handles it.
 *
 * This is the SW reference model backend by default. Subclass and override
 * `runOneInst` for RTL simulation (VCS, Verilator).
 *
 * 算子实现位于 runtime.dispatch.ops 包中，每个算子一个文件。
 * 可通过 registerOp / unregisterOp 动态注册/替换算子实现。
 */
class AcceleratorDispatcher(val pushDtype: String = "float32") extends HWDispatcher with DispatchContext {

  val HW_DIM_MULTIPLE = 16
  val MAX_HW_ELEMENTS: Int = 512 * 512

  // ====================== 算子注册表 ======================

  /** opType → HWOp 映射。可通过 registerOp / unregisterOp 修改。 */
  private val opRegistry = mutable.Map.empty[String, HWOp]

  // 注册默认算子
  registerOp(new MatMulOp)
  registerOp(new ElementWiseOp)
  registerOp(new ActivationOp)
  registerOp(new NegOp)
  registerOp(new ConcatOp)
  registerOp(new GemmOp)
  registerOp(new FusedOp)

  /** 注册一个算子实现。会覆盖同 opType 的旧实现。 */
  def registerOp(op: HWOp): Unit = {
    op.opTypes.foreach(t => opRegistry(t) = op)
  }

  /** 注销一个 opType 的算子实现。 */
  def unregisterOp(opType: String): Unit = {
    opRegistry.remove(opType)
  }

  /** 获取已注册的所有 opType。 */
  def registeredOpTypes: Set[String] = opRegistry.keySet.toSet

  /** Which op types to actually dispatch. Override in tests for diagnostics. */
  var dispatchOps: Option[Set[String]] = None

  var _maxHwOps = 0  // >0: dispatch only first N HW ops, rest fall through to SW

  private var _shouldDispatchCount = 0

  override def shouldDispatch(node: GraphNode): Boolean = {
    if (!opRegistry.contains(node.opType)) return false
    dispatchOps.foreach(set => if (!set.contains(node.opType)) return false)
    val hasInShift = node.getInts("fpga_in_shift").exists(_ != 0L)
    val hasOutShift = node.getInts("fpga_out_shift").exists(_ != 0L)
    val eligible = hasInShift || hasOutShift
    if (eligible && _maxHwOps > 0) {
      _shouldDispatchCount += 1
      if (_shouldDispatchCount > _maxHwOps) return false
    }
    eligible
  }

  // Debug controls
  var _hwOpCount = 0
  var _debugLimit = 0
  var _debugNodeNames: Set[String] = Set.empty
  var _validateRoundTrip = false
  var _digestMode = false
  var _progressInterval = 50
  var _useFloatActivations = false

  /** Callback after each HW node: (hwOpCount, node, outputTensorIds, graph) => Unit */
  var onNodeComplete: Option[(Int, GraphNode, Seq[String], OnnxGraph) => Unit] = None

  override def execute(node: GraphNode, inputIds: Seq[String], graph: OnnxGraph): Seq[String] = {
    _hwOpCount += 1
    val doDebug = (_debugLimit > 0 && _hwOpCount <= _debugLimit) || _debugNodeNames.contains(node.name)

    if (_progressInterval > 0 && (_hwOpCount % _progressInterval == 0 || _hwOpCount == 1)) {
      println(s"[HW#${_hwOpCount}] ${node.opType} '${node.name}' shifts_in=${node.fpgaInShift} shifts_out=${node.fpgaOutShift}")
    }
    if (doDebug) {
      val tis = node.fpgaInShift
      val tos = node.fpgaOutShift
      println(s"[HW#${_hwOpCount}] ${node.opType} '${node.name}' tis=$tis tos=$tos inputs=${node.inputs.toSeq}")
      val printCount = if (_debugNodeNames.contains(node.name)) 20 else 5
      inputIds.zipWithIndex.foreach { case (tid, idx) =>
        if (tid != null) {
          try {
            val info = TorchBridge.call("get_info", Map("id" -> tid)).asInstanceOf[Map[String, Any]]
            val shape = info("shape").asInstanceOf[List[Any]]
            val numel = shape.map(OnnxLoader.toLong).product
            val n = math.min(printCount, numel).toInt
            val vals = TorchBridge.call("get_values", Map("id" -> tid, "n" -> n))
            println(s"  input[$idx] shape=$shape first$n=$vals")
          } catch { case _: Throwable => }
        }
      }
    }

    // 查找并执行算子
    val op = opRegistry.getOrElse(node.opType,
      throw new UnsupportedOperationException(s"HW dispatch not implemented for ${node.opType}"))
    val result = op.execute(node.opType, node, inputIds, graph, this)

    if (doDebug) {
      result.foreach { rid =>
        try {
          val n = 5
          val vals = TorchBridge.call("get_values", Map("id" -> rid, "n" -> n))
          println(s"  output first$n=$vals")
        } catch { case _: Throwable => }
      }
    }
    if (_digestMode) {
      result.foreach { rid =>
        try {
          val stats = TorchBridge.call("get_stats", Map("id" -> rid)).asInstanceOf[Map[String, Any]]
          val absSum = stats("abs_sum").asInstanceOf[Double]
          val maxAbs = stats("max_abs").asInstanceOf[Double]
          val numel = stats("numel").asInstanceOf[Number].intValue()
          val tis = node.fpgaInShift
          val tos = node.fpgaOutShift
          println(f"[DIGEST] HW#${_hwOpCount}%4d ${node.opType}%-10s tis=$tis tos=$tos numel=$numel meanAbs=${absSum/numel}%.0f maxAbs=$maxAbs%.0f  '${node.name}'")
        } catch { case e: Throwable => println(s"[DIGEST] HW#${_hwOpCount} ERROR: ${e.getMessage}")}
      }
    }
    onNodeComplete.foreach(cb => cb(_hwOpCount, node, result, graph))
    result
  }

  // ====================== DispatchContext 实现 ======================

  override def hwOpCount: Int = _hwOpCount
  override def debugLimit: Int = _debugLimit
  override def debugNodeNames: Set[String] = _debugNodeNames
  override def useFloatActivations: Boolean = _useFloatActivations

  /** Override in subclass to change the backend (e.g. VCS simulation). */
  protected def runOneInst(matA: Array[Array[Long]], matB: Array[Array[Long]],
                           inst: InstJavaTODO): Array[Array[Long]] = {
    AcceleratorSimInterface.runRefOneInst(matA, matB, inst)
  }

  override def hwFracWidth: Long = AcceleratorSimInterface.acceleratorCfg.fracWidth

  override def getProducerOutputShift(graph: OnnxGraph, tensorName: String, default: Long): Long = {
    graph.producerMap.get(tensorName) match {
      case Some(pn) =>
        val shifts = pn.fpgaOutShift
        if (shifts.nonEmpty) shifts.head else default
      case None => default
    }
  }

  override def pullToLong2D(tensorId: String, rows: Int, cols: Int): Array[Array[Long]] = {
    val resp = TorchBridge.call("to_long_array_2d",
      Map("id" -> tensorId, "rows" -> rows, "cols" -> cols)).asInstanceOf[Map[String, Any]]
    val b64 = resp("data_b64").toString
    val raw = Base64.getDecoder.decode(b64)
    val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
    val flat = new Array[Long](rows * cols)
    buf.asLongBuffer().get(flat)
    Array.tabulate(rows)(r => flat.slice(r * cols, (r + 1) * cols))
  }

  override def pushFromLong2D(data: Array[Array[Long]], rows: Int, cols: Int,
                              dtype: String): String = {
    val flat = new Array[Long](rows * cols)
    for (r <- 0 until rows; c <- 0 until cols) {
      val v = data(r)(c)
      if (v > Int.MaxValue || v < Int.MinValue) {
        println(s"[OVERFLOW] value=$v at ($r,$c) in HW#${_hwOpCount}")
      }
      flat(r * cols + c) = v
    }
    val buf = ByteBuffer.allocate(flat.length * 8).order(ByteOrder.LITTLE_ENDIAN)
    flat.foreach(buf.putLong)
    val b64 = Base64.getEncoder.encodeToString(buf.array())
    val info = TorchBridge.call("from_long_array_2d", Map(
      "data_b64" -> b64, "rows" -> rows, "cols" -> cols, "dtype" -> dtype
    )).asInstanceOf[Map[String, Any]]
    val resultId = info("id").toString

    if (_validateRoundTrip && _hwOpCount <= 20) {
      val readBack = pullToLong2D(resultId, rows, cols)
      var mismatchCount = 0
      for (r <- 0 until rows; c <- 0 until cols) {
        if (readBack(r)(c) != data(r)(c)) {
          mismatchCount += 1
          if (mismatchCount <= 3) {
            println(s"[ROUNDTRIP MISMATCH] HW#${_hwOpCount} ($r,$c): wrote=${data(r)(c)} read=${readBack(r)(c)}")
          }
        }
      }
      if (mismatchCount > 0) println(s"[ROUNDTRIP] Total mismatches: $mismatchCount / ${rows * cols}")
      else println(s"[ROUNDTRIP] OK: ${rows * cols} elements match")
    }

    resultId
  }

  override def getShape(tid: String): Array[Long] = {
    val info = TorchBridge.call("get_info", Map("id" -> tid)).asInstanceOf[Map[String, Any]]
    info("shape").asInstanceOf[List[Any]].map(OnnxLoader.toLong).toArray
  }

  override def mkRef(id: String): Tensor = {
    val info = TorchBridge.call("get_info", Map("id" -> id)).asInstanceOf[Map[String, Any]]
    Tensor.fromInfo(info)
  }

  override def ceilToMultiple(v: Int, m: Int): Int = {
    val rem = v % m; if (rem == 0) v else v + (m - rem)
  }

  override def tiledHWOp(
      matA: Array[Array[Long]], matB: Array[Array[Long]],
      rows: Int, colsA: Int, colsB: Int,
      opName: String, shiftAfterOp: Int,
      activationFn: String, shiftAfterAct: Int,
      nodeName: String
  ): Array[Array[Long]] = {
    val k = colsA
    val maxTileM = MAX_HW_ELEMENTS / math.max(k, 1)
    require(maxTileM >= HW_DIM_MULTIPLE,
      s"Node $nodeName: Cannot tile. K=$k too large")

    val hwTileCapM = (maxTileM / HW_DIM_MULTIPLE) * HW_DIM_MULTIPLE
    val hwTileCapN = (MAX_HW_ELEMENTS / math.max(k, 1) / HW_DIM_MULTIPLE) * HW_DIM_MULTIPLE
    val result = Array.ofDim[Long](rows, colsB)

    val maxTileN = math.min(colsB, hwTileCapN)
    val hwTileCapMForOutput = math.max(HW_DIM_MULTIPLE,
      (MAX_HW_ELEMENTS / math.max(maxTileN, 1) / HW_DIM_MULTIPLE) * HW_DIM_MULTIPLE)
    val effectiveTileCapM = math.min(hwTileCapM, hwTileCapMForOutput)

    var mOff = 0
    while (mOff < rows) {
      val tM = math.min(rows - mOff, effectiveTileCapM)
      var nOff = 0
      while (nOff < colsB) {
        val tN = math.min(colsB - nOff, hwTileCapN)
        val tileA = Array.ofDim[Long](tM, k)
        for (i <- 0 until tM; j <- 0 until k) tileA(i)(j) = matA(mOff + i)(j)
        val tileB = Array.ofDim[Long](k, tN)
        for (i <- 0 until k; j <- 0 until tN) tileB(i)(j) = matB(i)(nOff + j)

        val inst = new InstJavaTODO(0, opName, shiftAfterOp, false, activationFn,
          shiftAfterAct, 0, 0, 0, tM, k, tN, 0, 0)
        val tileResult = runOneInst(tileA, tileB, inst)
        for (i <- 0 until tM; j <- 0 until tN) result(mOff + i)(nOff + j) = tileResult(i)(j)
        nOff += tN
      }
      mOff += tM
    }
    result
  }

  override def tiledElementOp(
      matA: Array[Array[Long]], matB: Array[Array[Long]],
      rows: Int, cols: Int,
      opName: String, shiftAfterOp: Int,
      activationFn: String, shiftAfterAct: Int,
      nodeName: String
  ): Array[Array[Long]] = {
    val maxRows = MAX_HW_ELEMENTS / cols
    require(maxRows >= HW_DIM_MULTIPLE, s"Node $nodeName: Cannot tile element op")
    val hwTileCap = (maxRows / HW_DIM_MULTIPLE) * HW_DIM_MULTIPLE
    val result = Array.ofDim[Long](rows, cols)

    var rOff = 0
    while (rOff < rows) {
      val tR = math.min(rows - rOff, hwTileCap)
      val tileA = Array.ofDim[Long](tR, cols)
      val tileB = Array.ofDim[Long](tR, cols)
      for (i <- 0 until tR; j <- 0 until cols) {
        tileA(i)(j) = matA(rOff + i)(j)
        tileB(i)(j) = matB(rOff + i)(j)
      }
      val inst = new InstJavaTODO(0, opName, shiftAfterOp, false, activationFn,
        shiftAfterAct, 0, 0, 0, tR, cols, cols, 0, 0)
      if (_debugNodeNames.nonEmpty && _debugNodeNames.exists(n => nodeName.contains(n.split('/').last))) {
        println(s"  [inst] op=$opName shiftOp=$shiftAfterOp act=$activationFn shiftAct=$shiftAfterAct shape=($tR,$cols,$cols)")
      }
      val tileResult = runOneInst(tileA, tileB, inst)
      for (i <- 0 until tR; j <- 0 until cols) result(rOff + i)(j) = tileResult(i)(j)
      rOff += tR
    }
    result
  }

  override def dispatchToOp(opType: String, node: GraphNode, inputIds: Seq[String],
                            graph: OnnxGraph): Seq[String] = {
    val op = opRegistry.getOrElse(opType,
      throw new UnsupportedOperationException(s"No HWOp registered for $opType"))
    op.execute(opType, node, inputIds, graph, this)
  }
}

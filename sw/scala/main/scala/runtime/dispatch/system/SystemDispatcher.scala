package runtime.dispatch.system

import Accelerator._
import WrapForFPGA._
import DMA._
import Interface._
import runtime.dispatch.AcceleratorDispatcher
import runtime.engine._
import runtime.bridge.Tensor

import scala.collection.mutable

// =============================================================================
// SystemDispatcher — 系统级仿真调度器（全图 DDR 驻留执行）
//
// 执行模型（精确模拟真实 FPGA 部署）：
//   Phase 1 - 预加载：Host 通过 hostAxi 将全部 ONNX 常量写入 DDR
//   Phase 2 - 推理：  纯指令触发，所有数据驻留 DDR
//             算子输入通过 DMA 从 DDR 加载到 Cache → Core 计算 → DMA 写回 DDR
//             中间结果始终驻留 DDR，无 hostAxi 数据搬运
//             移位对齐通过硬件指令 shiftLeft_A/B 字段在 Slicer 中完成
//   Phase 3 - 回读：  Host 从 DDR 读取最终输出（仅用于结果验证）
//
// DDR 数据流：
//   DDR → [DMA_A] → CacheA → Slicer(shiftA) → SystolicArray → Collector → CacheC
//   DDR → [DMA_B] → CacheB → Slicer(shiftB) ↗
//   CacheC → [DMA_Z] → DDR（中间结果驻留，供后续算子直接读取）
//
// 支持的 DDR 原生算子：MatMul, Add/Sub/Max, Relu/Exp/Log/Softplus
// 回退算子（通过算子注册表）：Neg, Concat, Gemm, Fused 等
// =============================================================================

class SystemDispatcher(
  val sysCfg: SystemWrapperConfig,
  dtype: String = "int32"
) extends AcceleratorDispatcher(dtype) {

  // ====================== 硬件参数覆盖 ======================

  override val HW_DIM_MULTIPLE: Int = sysCfg.fpgaCfg.systolicArraySideNum
  override val MAX_HW_ELEMENTS: Int = (1 << sysCfg.cacheAddrWidth) * sysCfg.fpgaCfg.systolicArraySideNum

  private val _acceleratorCfg = AcceleratorCfg(
    UIDWidth = sysCfg.fpgaCfg.UIDWidth,
    AddressWidth = sysCfg.fpgaCfg.AddressWidth,
    ShapeWidth = sysCfg.fpgaCfg.ShapeWidth,
    elementWidth = sysCfg.fpgaCfg.elementWidth,
    intWidth = sysCfg.fpgaCfg.intWidth,
    systolicArraySideNum = sysCfg.fpgaCfg.systolicArraySideNum,
    systolicArrayInFifoDepth = sysCfg.fpgaCfg.systolicArrayInFifoDepth,
    systolicArrayOutFifoDepth = sysCfg.fpgaCfg.systolicArrayOutFifoDepth,
    systolicArrayInstFifoDepth = sysCfg.fpgaCfg.systolicArrayInstFifoDepth,
    activationOutFifoDepth = sysCfg.fpgaCfg.activationOutFifoDepth,
    slicedInstFifoDepth = sysCfg.fpgaCfg.slicedInstFifoDepth,
    numCores = sysCfg.fpgaCfg.numCores,
    memElementWidth = 32
  )

  override def hwFracWidth: Long = _acceleratorCfg.fracWidth

  /**
   * 强制所有 pushFromLong2D 使用 int32 dtype，避免 float32 round-trip 丢失 >24bit 精度。
   * AcceleratorDispatcher 全用 float32（一致性无损），但 SystemDispatcher 混合 DDR-native（int32）
   * 和 FB（float32）会导致精度不一致。统一为 int32 消除此问题。
   */
  override def pushFromLong2D(data: Array[Array[Long]], rows: Int, cols: Int,
                              dtype: String = "int32"): String = {
    super.pushFromLong2D(data, rows, cols, "int32")
  }

  // ====================== 仿真上下文和驱动 ======================

  private lazy val simCtx    = new SimContext(sysCfg)
  private lazy val dmaDriver = new DmaDriver(simCtx)
  private lazy val cacheDriver = new CacheDriver(simCtx)
  private lazy val instDriver  = new InstructionDriver(simCtx)
  private lazy val ddrDriver   = new DdrDataDriver(simCtx)

  private lazy val memPool = new MemoryPool(
    baseAddr  = 0x1_0000_0000L,
    totalSize = 1L << 30,
    alignment = simCtx.bytesPerBeat
  )

  // ====================== 交叉校验 ======================

  var crossCheck: Boolean = false
  var crossCheckMaxErr: Long = 0L
  var crossCheckAbortAfter: Int = 5

  /** 控制哪些算子走 DDR 原生路径。null = 全部允许（默认）；Set() = 全部回退 FB */
  var ddrNativeFilter: Set[String] = null

  private var _tileCount: Int = 0
  private var _failedTiles: Int = 0

  // ====================== DDR 张量注册表 ======================
  // 核心数据结构：跟踪所有驻留 DDR 的张量位置和元信息

  /** DDR 中张量的完整描述 */
  case class DdrTensorInfo(
    name: String,           // ONNX 张量名
    ddrAddr: Long,          // DDR 基地址
    origRows: Int,          // 原始行数（未填充）
    origCols: Int,          // 原始列数（未填充）
    padRows: Int,           // 填充后行数（sideNum 的倍数）
    padCols: Int,           // 填充后列数（sideNum 的倍数）
    originalShape: Array[Long], // 原始 N 维形状
    isPreloaded: Boolean    // 是否为预加载常量
  )

  /** DDR 张量注册表：ONNX 张量名 → DDR 信息 */
  private val _tensorDdrMap = mutable.Map.empty[String, DdrTensorInfo]

  /** Python 张量 ID → DDR 信息（用于 dispatchToOp 场景） */
  private val _tidToDdr = mutable.Map.empty[String, DdrTensorInfo]

  /** 零矩阵 DDR 地址（用于 Activation 算子的 B 输入） */
  private var _zeroDdrAddr: Long = 0L
  private var _zeroDdrMaxElems: Int = 0

  // ====================== 指纹缓存（回退路径用） ======================

  case class DdrCacheEntry(
    tensorName: String, ddrAddr: Long, rows: Int, cols: Int,
    fingerprint: Long, isPreloaded: Boolean
  )
  private val _ddrWriteCache = mutable.Map.empty[Long, DdrCacheEntry]

  // ====================== 预加载/推理统计 ======================

  private var _preloadCycles: Long = 0L
  private var _preloadBytes: Long = 0L
  private var _preloaded: Boolean = false
  private var _inferenceStartCycle: Long = 0L
  private var _readbackOverheadCycles: Long = 0L
  private var _ddrNativeOps: Int = 0
  private var _fallbackOps: Int = 0

  /** 自动预加载 */
  var autoPreload: Boolean = true
  /** DMA 日志: 0=关闭, 1=每算子摘要, 2=每次 DMA */
  var dmaLogLevel: Int = 1

  // ====================== 性能统计 ======================

  case class OpPerfRecord(
    hwOpIdx: Int, nodeName: String, opType: String,
    startCycle: Long, endCycle: Long, computeEndCycle: Long,
    tiles: Int, shapeDesc: String, isDdrNative: Boolean
  ) {
    def cycles: Long = endCycle - startCycle
    def computeCycles: Long = computeEndCycle - startCycle
    def readbackCycles: Long = endCycle - computeEndCycle
  }

  private val _perfRecords = mutable.ArrayBuffer.empty[OpPerfRecord]
  private var _currentOpTiles: Int = 0
  private var _currentOpShape: String = ""
  private var _currentComputeEndCycle: Long = 0L

  var collectPerfStats: Boolean = true

  def currentCycle: Long = {
    if (_started) {
      simCtx.submitChecked(GetCycleCountCmd) match {
        case LongResult(v) => v
        case _ => 0L
      }
    } else 0L
  }

  def perfRecords: Seq[OpPerfRecord] = _perfRecords.toSeq

  // ====================== 性能摘要 ======================

  def printPerfSummary(): Unit = {
    if (_perfRecords.isEmpty && !_preloaded) return

    val pureComputeCycles = _perfRecords.map(_.computeCycles).sum
    val totalCyclesWithReadback = _perfRecords.map(_.cycles).sum
    val readbackCycles = _perfRecords.map(_.readbackCycles).sum
    val byOpType = _perfRecords.groupBy(_.opType)

    println(f"\n${"=" * 80}")
    println(f"  System-Level Performance Summary (DDR-Resident Full-Graph)")
    println(f"${"=" * 80}")

    if (_preloaded) {
      println(f"\n  [Phase 1] DDR Constant Preloading:")
      println(f"    Tensors loaded:     ${_tensorDdrMap.count(_._2.isPreloaded)}%d")
      println(f"    Data transferred:   ${_preloadBytes / 1024}%d KB")
      println(f"    Preload cycles:     ${_preloadCycles}%d")
    }

    if (_perfRecords.nonEmpty) {
      println(f"\n  [Phase 2] Inference (Pure DMA+Compute):")
      println(f"    Total HW ops:       ${_perfRecords.size}%d  (DDR-native: ${_ddrNativeOps}%d, fallback: ${_fallbackOps}%d)")
      println(f"    Pure compute cycles: ${pureComputeCycles}%d  ← 真实推理周期数")
      if (_preloaded) {
        println(f"    Total (preload+inf): ${_preloadCycles + pureComputeCycles}%d")
      }

      println(f"\n  [Phase 3] Readback Overhead (不计入推理):")
      println(f"    Readback cycles:    ${readbackCycles}%d  (仅用于结果验证/兼容)")

      println(f"\n  Per-OpType Breakdown (pure compute):")
      println("  " + "─" * 76)
      println(f"  ${"OpType"}%-15s ${"Count"}%6s ${"ComputeCyc"}%14s ${"Avg"}%12s ${"Pct"}%6s ${"Mode"}%8s")
      println("  " + "─" * 76)
      for ((opType, records) <- byOpType.toSeq.sortBy(-_._2.map(_.computeCycles).sum)) {
        val total = records.map(_.computeCycles).sum
        val avg = if (records.nonEmpty) total / records.size else 0L
        val pct = if (pureComputeCycles > 0) total * 100.0 / pureComputeCycles else 0.0
        val nativeCount = records.count(_.isDdrNative)
        val mode = if (nativeCount == records.size) "DDR" else if (nativeCount == 0) "FB" else "MIX"
        println(f"  $opType%-15s ${records.size}%6d $total%14d $avg%12d $pct%5.1f%% $mode%8s")
      }
      println("  " + "─" * 76)

      println(f"\n  Top 10 Most Expensive Operators (pure compute):")
      for (rec <- _perfRecords.sortBy(-_.computeCycles).take(10)) {
        val mode = if (rec.isDdrNative) "DDR" else "FB"
        println(f"    HW#${rec.hwOpIdx}%-5d ${rec.opType}%-12s ${rec.computeCycles}%12d cycles  ${rec.shapeDesc}  [$mode]  '${rec.nodeName}'")
      }
    }

    println(f"\n${"=" * 80}\n")
  }

  // ====================== 预加载接口 ======================

  def preloadInitializers(graph: OnnxGraph): Unit = {
    ensureStarted()

    val startCycle = currentCycle
    val startTime = System.currentTimeMillis()
    val sideNum = HW_DIM_MULTIPLE
    var count = 0
    var totalBytes = 0L

    println(s"\n${"=" * 80}")
    println(s"  [PRELOAD] Phase 1: Loading ONNX Initializers to DDR")
    println(s"  [PRELOAD] Total initializers: ${graph.initializers.size}")
    println(s"${"=" * 80}")

    for ((name, tensorId) <- graph.initializers) {
      val shape = getShape(tensorId)
      if (shape.length < 1 || shape.product == 0) {
        if (dmaLogLevel >= 2) println(f"  [PRELOAD] SKIP '$name' shape=[${shape.mkString(",")}] (empty)")
      } else if (shape.product < sideNum) {
        // 跳过过小的张量（通常是量化参数/缩放因子，非矩阵操作数）
        // 这些参数在软件侧用于合成指令的移位参数，不应加载到 DDR
        if (dmaLogLevel >= 2) println(f"  [PRELOAD] SKIP '$name' shape=[${shape.mkString(",")}] (${shape.product} elems < sideNum=$sideNum, param not tensor)")
      } else {
        val lastDim = shape.last.toInt
        val leadingDims = if (shape.length > 1) shape.dropRight(1).product.toInt else 1
        val rows = leadingDims
        val cols = lastDim
        val pRows = ceilToMultiple(rows, sideNum)
        val pCols = ceilToMultiple(cols, sideNum)

        val data = pullToLong2D(tensorId, rows, cols)
        val padded = Array.ofDim[Long](pRows, pCols)
        for (i <- 0 until rows; j <- 0 until cols) padded(i)(j) = data(i)(j)

        val sizeBytes = ddrDriver.matrixSizeBytes(pRows, pCols)
        val addr = memPool.allocate(s"preload_$name", sizeBytes)
        ddrDriver.writeLongMatrix(addr, padded, pRows, pCols)

        // 注册到 DDR 张量注册表
        val info = DdrTensorInfo(name, addr, rows, cols, pRows, pCols, shape, isPreloaded = true)
        _tensorDdrMap(name) = info
        // 也注册到 tensorId 映射（供 dispatchToOp 场景使用）
        _tidToDdr(tensorId) = info

        // 指纹缓存（回退路径用）
        val fp = dataFingerprint(padded, pRows, pCols)
        _ddrWriteCache(fp) = DdrCacheEntry(name, addr, pRows, pCols, fp, isPreloaded = true)

        count += 1
        totalBytes += sizeBytes
        if (dmaLogLevel >= 1) {
          println(f"  [PRELOAD] #$count%-4d '$name%-40s' shape=[${shape.mkString(",")}]" +
            f" → padded(${pRows}×${pCols}) DDR=0x${addr}%X  ${sizeBytes / 1024}%d KB")
        }
      }
    }

    val endCycle = currentCycle
    _preloadCycles = endCycle - startCycle
    _preloadBytes = totalBytes
    _preloaded = true
    _inferenceStartCycle = endCycle

    // 预分配零矩阵 DDR（用于 Activation 算子的 B 输入）
    allocateZeroMatrix()

    val elapsed = System.currentTimeMillis() - startTime
    println(s"${"=" * 80}")
    println(f"  [PRELOAD] Done: $count tensors (${totalBytes / 1024}%d KB) → DDR")
    println(f"  [PRELOAD] Sim cycles: ${_preloadCycles}%d  |  Wall time: ${elapsed}ms")
    println(f"  [PRELOAD] MemPool used: ${memPool.usedBytes / 1024}%d KB / ${memPool.totalSize / 1024 / 1024}%d MB")
    println(s"${"=" * 80}\n")
  }

  /** 预分配零矩阵 DDR 区域（Activation 算子的 B 输入固定为零） */
  private def allocateZeroMatrix(): Unit = {
    val maxElems = MAX_HW_ELEMENTS
    val side = math.sqrt(maxElems.toDouble).toInt
    val pSide = ceilToMultiple(side, HW_DIM_MULTIPLE)
    val sizeBytes = ddrDriver.matrixSizeBytes(pSide, pSide)
    _zeroDdrAddr = memPool.allocate("_zero_matrix", sizeBytes)
    _zeroDdrMaxElems = pSide * pSide
    // 显式写入零矩阵（不依赖 ramulator 初始化行为）
    val zeroMat = Array.ofDim[Long](pSide, pSide)
    ddrDriver.writeLongMatrix(_zeroDdrAddr, zeroMat, pSide, pSide)
    if (dmaLogLevel >= 1) {
      println(f"  [PRELOAD] Zero matrix allocated + written: DDR 0x${_zeroDdrAddr}%X (${pSide}×${pSide})")
    }
  }

  // ====================== DDR 张量管理 ======================

  /**
   * 确保张量在 DDR 中可用。
   * 优先从 _tensorDdrMap 查找，未命中则从 Python 拉取、填充、写入 DDR。
   */
  private def ensureInDdr(tensorName: String, tensorId: String): DdrTensorInfo = {
    // 首先按 ONNX 名查找，然后按 Python ID 查找
    _tensorDdrMap.get(tensorName) match {
      case Some(info) =>
        if (dmaLogLevel >= 2) println(f"    [DDR-HIT] '$tensorName' via _tensorDdrMap → DDR=0x${info.ddrAddr}%X (${info.padRows}×${info.padCols})")
        return info
      case None =>
    }
    _tidToDdr.get(tensorId) match {
      case Some(info) =>
        if (dmaLogLevel >= 2) println(f"    [DDR-HIT] '$tensorName' via _tidToDdr(id=$tensorId) → DDR=0x${info.ddrAddr}%X (${info.padRows}×${info.padCols})")
        return info
      case None =>
    }
    {
      // 未在 DDR 中 → 从 Python 拉取并写入
      val shape = getShape(tensorId)
      val lastDim = shape.last.toInt
      val leadingDims = if (shape.length > 1) shape.dropRight(1).product.toInt else 1
      val rows = leadingDims
      val cols = lastDim
      val pRows = ceilToMultiple(rows, HW_DIM_MULTIPLE)
      val pCols = ceilToMultiple(cols, HW_DIM_MULTIPLE)

      val data = pullToLong2D(tensorId, rows, cols)
      val padded = Array.ofDim[Long](pRows, pCols)
      for (i <- 0 until rows; j <- 0 until cols) padded(i)(j) = data(i)(j)

      val sizeBytes = ddrDriver.matrixSizeBytes(pRows, pCols)
      val addr = memPool.allocate(s"ddr_$tensorName", sizeBytes)
      ddrDriver.writeLongMatrix(addr, padded, pRows, pCols)

      val info = DdrTensorInfo(tensorName, addr, rows, cols, pRows, pCols, shape, isPreloaded = false)
      _tensorDdrMap(tensorName) = info
      _tidToDdr(tensorId) = info
      if (dmaLogLevel >= 1) {
        println(f"    [DDR-IN] WRITE '$tensorName' (${rows}×${cols}) → padded(${pRows}×${pCols}) DDR=0x${addr}%X")
      }
      info
    }
  }

  /**
   * 将 DDR 中的计算结果回读到 Python（Phase 3 回读，独立于推理周期计数）。
   * 返回 Python 张量 ID。
   */
  private def materializeToHost(
    addrZ: Long, origRows: Int, origCols: Int, padRows: Int, padCols: Int,
    originalShape: Array[Long], outputName: String
  ): String = {
    val padded = ddrDriver.readLongMatrix(addrZ, padRows, padCols)
    val unpadded = Array.ofDim[Long](origRows, origCols)
    for (i <- 0 until origRows; j <- 0 until origCols) unpadded(i)(j) = padded(i)(j)
    // 必须用 "int32" dtype，否则 Long→float32→Long round-trip 会丢失 >24 bit 精度
    val rawId = pushFromLong2D(unpadded, origRows, origCols, dtype = "int32")
    // 如果原始形状不是 2D，进行 Reshape
    if (originalShape.length > 2 || (originalShape.length == 2 &&
        (originalShape(0) != origRows || originalShape(1) != origCols))) {
      val shaped = Tensor.op("Reshape", Seq(mkRef(rawId)), Map("shape" -> originalShape.toSeq)).head
      shaped.id
    } else rawId
  }

  // ====================== 指纹工具（回退路径用） ======================

  private def dataFingerprint(mat: Array[Array[Long]], rows: Int, cols: Int): Long = {
    var h = rows.toLong * 0x9E3779B97F4A7C15L
    h ^= cols.toLong * 0x517CC1B727220A95L
    if (rows > 0 && cols > 0) {
      h = h * 31 + mat(0)(0); h = h * 31 + mat(0)(cols - 1)
      h = h * 31 + mat(rows - 1)(0); h = h * 31 + mat(rows - 1)(cols - 1)
      h = h * 31 + mat(rows / 2)(cols / 2)
      val step = math.max(1, math.min(rows, cols) / 8)
      for (k <- 0 until math.min(rows, cols) by step) h = h * 31 + mat(k)(k)
      h = h * 31 + mat(0)(cols / 2); h = h * 31 + mat(rows - 1)(cols / 2)
      h = h * 31 + mat(rows / 2)(0); h = h * 31 + mat(rows / 2)(cols - 1)
    }
    h
  }

  private def acquireDdr(mat: Array[Array[Long]], rows: Int, cols: Int,
                         tag: String, label: String, opDesc: String): (Long, Boolean) = {
    // 不使用指纹缓存：FB 路径的动态数据每次可能不同，稀疏指纹无法可靠区分
    // （指纹碰撞会导致 HW 使用旧的 DDR 数据，产生静默计算错误）
    val sizeBytes = ddrDriver.matrixSizeBytes(rows, cols)
    val addr = memPool.allocate(tag, sizeBytes)
    ddrDriver.writeLongMatrix(addr, mat, rows, cols)
    if (dmaLogLevel >= 1) println(f"    [DDR-$label] WRITE (${rows}×${cols}) → DDR 0x${addr}%X  [$opDesc]")
    (addr, true)
  }

  // ====================== execute 重写（DDR 原生路由） ======================

  /** DDR 原生支持的算子类型 */
  private val _ddrNativeOps2D = Set("MatMul", "Gemm", "Add", "Max", "Relu", "Exp", "Log", "Softplus")

  /** 从 DDR 注册表或 Python 获取张量原始形状 */
  private def getInputShape(tensorName: String, tensorId: String): Array[Long] = {
    _tensorDdrMap.get(tensorName).map(_.originalShape)
      .orElse(if (tensorId != null) _tidToDdr.get(tensorId).map(_.originalShape) else None)
      .getOrElse {
        if (tensorId != null) getShape(tensorId) else null
      }
  }

  /**
   * 判断是否可以走 DDR 原生路径。
   * - MatMul: 支持 2D×2D 和 3D×2D；3D×3D 回退
   * - Add/Max: 仅支持形状完全匹配（无 broadcast）；形状不同回退
   * - Activation: 单输入，始终支持
   */
  private def canDdrNative(node: GraphNode, inputIds: Seq[String]): Boolean = {
    if (!_ddrNativeOps2D.contains(node.opType)) return false
    if (ddrNativeFilter != null && !ddrNativeFilter.contains(node.opType)) return false
    node.opType match {
      case "MatMul" | "Gemm" =>
        if (inputIds.size < 2 || inputIds(0) == null || inputIds(1) == null) return false
        // Gemm：仅无 bias 且无转置时等价于 MatMul
        if (node.opType == "Gemm") {
          if (inputIds.size > 2 && inputIds(2) != null) return false  // 有 bias，回退
          if (node.getInt("transA", 0L) != 0L || node.getInt("transB", 0L) != 0L) return false
        }
        val aShape = getInputShape(node.inputs(0), inputIds(0))
        val bShape = getInputShape(node.inputs(1), inputIds(1))
        if (aShape == null || bShape == null) return false
        // 2D×2D 或 3D×2D（3D 自动展平为 2D，与 MatMulOp 一致）
        bShape.length == 2 && aShape.length >= 2 && aShape.length <= 3
      case "Add" | "Max" =>
        if (inputIds.size < 2 || inputIds(0) == null || inputIds(1) == null) return false
        val aShape = getInputShape(node.inputs(0), inputIds(0))
        val bShape = getInputShape(node.inputs(1), inputIds(1))
        if (aShape == null || bShape == null) return false
        // 仅形状完全匹配时走 DDR 原生；broadcast 回退到算子注册表
        aShape.sameElements(bShape)
      case "Relu" | "Exp" | "Log" | "Softplus" =>
        inputIds.nonEmpty && inputIds(0) != null
      case _ => false
    }
  }

  override def execute(node: GraphNode, inputIds: Seq[String], graph: OnnxGraph): Seq[String] = {
    ensureStarted()
    if (autoPreload && !_preloaded) preloadInitializers(graph)

    val isDdrNative = canDdrNative(node, inputIds)
    val startCycle = if (collectPerfStats) currentCycle else 0L
    _currentOpTiles = 0
    _currentOpShape = s"inputs=${node.inputs.length}"
    _currentComputeEndCycle = 0L

    if (dmaLogLevel >= 1) {
      val opIdx = if (isDdrNative) _hwOpCount + 1 else _hwOpCount + 1
      val mode = if (isDdrNative) "DDR" else "FB"
      println(f"\n  ┌─ HW#$opIdx ${node.opType} '${node.name}'  [$mode]")
    }

    val result = if (isDdrNative) {
      // DDR 原生路径：手动管理 _hwOpCount，跳过父类
      _hwOpCount += 1
      _ddrNativeOps += 1
      val ddrResult = node.opType match {
        case "MatMul" | "Gemm" => executeDdrMatMul(node, inputIds, graph)
        case "Add" | "Max" => executeDdrElementOp(node, inputIds, graph)
        case "Relu" | "Exp" | "Log" | "Softplus" => executeDdrActivation(node, inputIds, graph)
        case _ => super.execute(node, inputIds, graph)
      }
      // 触发 onNodeComplete 回调（与父类 execute 行为一致，支持 online ORT 对比）
      onNodeComplete.foreach(cb => cb(_hwOpCount, node, ddrResult, graph))
      ddrResult
    } else {
      // 回退路径：调用父类 execute（父类管理 _hwOpCount、debug、opRegistry）
      _fallbackOps += 1
      super.execute(node, inputIds, graph)
    }

    if (collectPerfStats) {
      val endCycle = currentCycle
      val computeEnd = if (_currentComputeEndCycle > 0) _currentComputeEndCycle else endCycle
      val opCycles = computeEnd - startCycle
      _perfRecords += OpPerfRecord(
        hwOpIdx = _hwOpCount, nodeName = node.name, opType = node.opType,
        startCycle = startCycle, endCycle = endCycle, computeEndCycle = computeEnd,
        tiles = _currentOpTiles, shapeDesc = _currentOpShape, isDdrNative = isDdrNative
      )
      _readbackOverheadCycles += (endCycle - computeEnd)
      if (dmaLogLevel >= 1) {
        val mode = if (isDdrNative) "DDR" else "FB"
        println(f"  └─ HW#${_hwOpCount} done: compute=$opCycles%d cycles, ${_currentOpTiles} tiles  [$mode]  ${_currentOpShape}")
      }
    }

    result
  }

  // ====================== DDR 原生算子实现 ======================

  /**
   * DDR 原生矩阵乘法。
   * 数据全程驻留 DDR，移位由硬件指令 shiftLeft_A/B 完成。
   */
  private def executeDdrMatMul(node: GraphNode, inputIds: Seq[String], graph: OnnxGraph): Seq[String] = {
    val infoA = ensureInDdr(node.inputs(0), inputIds(0))
    val infoB = ensureInDdr(node.inputs(1), inputIds(1))

    // 移位参数
    val tis = node.fpgaInShift
    val tisA = if (tis.nonEmpty) tis(0) else 0L
    val tisB = if (tis.size > 1) tis(1) else 0L
    val ssA = getProducerOutputShift(graph, node.inputs(0), tisA)
    val ssB = getProducerOutputShift(graph, node.inputs(1), tisB)
    val tos = node.fpgaOutShift.headOption.getOrElse(0L)

    // shiftLeft_A: 硬件 Slicer 中对 A 的左移量（负值=右移）
    // 等效于 SW 的 roundShiftRight(data, ssA - tisA) → shiftLeft_A = tisA - ssA
    val shiftA = (tisA - ssA).toInt
    val shiftB = (tisB - ssB).toInt
    val shiftAfterOp = (tisA + tisB - tos).toInt

    // 3D×2D 支持：ensureInDdr 已将 [batch,m,k] 展平为 (batch*m, k)
    // infoA.origRows = batch*m, infoA.origCols = k（与 MatMulOp 一致）
    val outRows = infoA.origRows
    val outCols = infoB.origCols
    val pOutRows = infoA.padRows
    val pK = infoA.padCols   // = infoB.padRows
    val pOutCols = infoB.padCols

    val outName = node.outputs(0)
    val addrZ = memPool.allocate(s"ddr_$outName", ddrDriver.matrixSizeBytes(pOutRows, pOutCols))

    // 纯 DMA + Compute（无 hostAxi 数据搬运）
    tiledHWOpDdr(
      infoA.ddrAddr, pOutRows, pK,
      infoB.ddrAddr, pK, pOutCols,
      addrZ, "matmul", shiftAfterOp, shiftA, shiftB, "none", 0, node.name
    )
    _currentComputeEndCycle = currentCycle

    // 输出形状：3D×2D 时保留 batch 维度 [batch, m, n]，与 MatMulOp 一致
    val aOrigShape = infoA.originalShape
    val outShape = if (aOrigShape.length == 3) {
      Array(aOrigShape(0), aOrigShape(1), infoB.originalShape(1))
    } else {
      Array(outRows.toLong, outCols.toLong)
    }

    // 注册输出
    val outInfo = DdrTensorInfo(outName, addrZ, outRows, outCols, pOutRows, pOutCols, outShape, isPreloaded = false)
    _tensorDdrMap(outName) = outInfo
    _currentOpShape = s"matmul(${pOutRows}×${pK}×${pOutCols}) DDR-native"

    // Phase 3: 回读到 Python（不计入推理周期）
    val resultId = materializeToHost(addrZ, outRows, outCols, pOutRows, pOutCols, outShape, outName)
    _tidToDdr(resultId) = outInfo
    Seq(resultId)
  }

  /**
   * DDR 原生逐元素运算 (Add/Sub/Max)。
   */
  private def executeDdrElementOp(node: GraphNode, inputIds: Seq[String], graph: OnnxGraph): Seq[String] = {
    val infoA = ensureInDdr(node.inputs(0), inputIds(0))
    val infoB = ensureInDdr(node.inputs(1), inputIds(1))

    val tis = node.fpgaInShift
    val tisA = if (tis.nonEmpty) tis(0) else 0L
    val tisB = if (tis.size > 1) tis(1) else 0L
    val ssA = getProducerOutputShift(graph, node.inputs(0), tisA)
    val ssB = getProducerOutputShift(graph, node.inputs(1), tisB)
    val tos = node.fpgaOutShift.headOption.getOrElse(0L)

    val isMax = node.opType == "Max"
    val hwOp = node.opType.toLowerCase match {
      case "add" => "elementadd"; case "max" => "elementmax"; case other => s"element$other"
    }

    // ElementWise 移位对齐：与 ElementWiseOp.scala 保持一致
    // Add: A 对齐到 tisA，B 对齐到 tisB（各自的目标精度），shiftAfterOp = tisA - tos
    // Max: 两路都对齐到 max(tisA, tisB)，shiftAfterOp = max(tisA, tisB) - tos
    val comparisonShift = if (isMax) math.max(tisA, tisB) else tisA
    val shiftA = (comparisonShift - ssA).toInt
    val shiftB = ((if (isMax) comparisonShift else tisB) - ssB).toInt
    val shiftAfterOp = (comparisonShift - tos).toInt

    // 确保 A 和 B 形状兼容（广播已由上游处理）
    val rows = math.max(infoA.padRows, infoB.padRows)
    val cols = math.max(infoA.padCols, infoB.padCols)
    val origRows = math.max(infoA.origRows, infoB.origRows)
    val origCols = math.max(infoA.origCols, infoB.origCols)

    // ---- 右移对齐时使用软件 roundShiftRight，与 FB 路径 ElementWiseOp 一致 ----
    // 硬件 Slicer 做纯截断(向负无穷) vs SW roundShiftRight(加半 LSB 后截断)
    // 差异在逐元素运算中逐步累积，经后续 MatMul 放大导致偏差
    // 修复：正右移时在软件中完成 roundShiftRight，硬件移位清零
    val (addrAForHW, hwShiftA) = if (shiftA > 0) {
      val mat = ddrDriver.readLongMatrix(infoA.ddrAddr, rows, cols)
      for (i <- 0 until rows; j <- 0 until cols)
        mat(i)(j) = Util.roundShiftRight(mat(i)(j), shiftA)
      val a = memPool.allocate(s"roundA_${node.name}", ddrDriver.matrixSizeBytes(rows, cols))
      ddrDriver.writeLongMatrix(a, mat, rows, cols)
      (a, 0)
    } else (infoA.ddrAddr, shiftA)

    val (addrBForHW, hwShiftB) = if (shiftB > 0) {
      val mat = ddrDriver.readLongMatrix(infoB.ddrAddr, rows, cols)
      for (i <- 0 until rows; j <- 0 until cols)
        mat(i)(j) = Util.roundShiftRight(mat(i)(j), shiftB)
      val a = memPool.allocate(s"roundB_${node.name}", ddrDriver.matrixSizeBytes(rows, cols))
      ddrDriver.writeLongMatrix(a, mat, rows, cols)
      (a, 0)
    } else (infoB.ddrAddr, shiftB)

    val outName = node.outputs(0)
    val addrZ = memPool.allocate(s"ddr_$outName", ddrDriver.matrixSizeBytes(rows, cols))

    tiledElementOpDdr(
      addrAForHW, rows, cols, infoA.padRows, infoA.padCols,
      addrBForHW, infoB.padRows, infoB.padCols,
      addrZ, hwOp, shiftAfterOp, hwShiftA, hwShiftB, "none", 0, node.name
    )
    _currentComputeEndCycle = currentCycle

    val outShape = infoA.originalShape   // 保留原始高维形状
    val outInfo = DdrTensorInfo(outName, addrZ, origRows, origCols, rows, cols, outShape, isPreloaded = false)
    _tensorDdrMap(outName) = outInfo
    _currentOpShape = s"elem:${hwOp}(${rows}×${cols}) DDR-native"

    val resultId = materializeToHost(addrZ, origRows, origCols, rows, cols, outShape, outName)
    _tidToDdr(resultId) = outInfo
    Seq(resultId)
  }

  /**
   * DDR 原生激活函数 (Relu/Exp/Log/Softplus)。
   * B 输入为零矩阵（预分配），opName=elementadd，激活由硬件查表完成。
   */
  private def executeDdrActivation(node: GraphNode, inputIds: Seq[String], graph: OnnxGraph): Seq[String] = {
    val infoA = ensureInDdr(node.inputs(0), inputIds(0))
    val actFn = node.opType.toLowerCase

    val tis = node.fpgaInShift
    val tisX = if (tis.nonEmpty) tis(0) else 0L
    val ssX = getProducerOutputShift(graph, node.inputs(0), tisX)
    val tos = node.fpgaOutShift.headOption.getOrElse(0L)
    val sHW = hwFracWidth

    // 激活函数输入需要对齐到 hwFracWidth
    // 当前 SW 做: preShift = (ssX - sHW).toInt, postShift = (sHW - tos).toInt
    // DDR 原生: shiftLeft_A = sHW - ssX（将输入对齐到 hwFracWidth）
    val shiftA = (sHW - ssX).toInt
    val shiftAfterAct = (sHW - tos).toInt

    val rows = infoA.padRows
    val cols = infoA.padCols

    // Log 激活: padding 区域需要填充非零值避免 log(0) 硬件溢出
    // 与 ActivationOp 一致: padding 填充 ceil(0.2 * 2^precision) 使 log 输入非零
    // DDR 数据在 ssX 精度，Slicer 会移到 sHW，所以 padding 用 ssX 精度表示 0.2
    val needsLogPad = actFn == "log" && (infoA.origRows < rows || infoA.origCols < cols)
    val addrA = if (needsLogPad) {
      val padDefault = math.ceil(0.2 * (1L << ssX.toInt)).toLong
      val mat = ddrDriver.readLongMatrix(infoA.ddrAddr, rows, cols)
      for (i <- 0 until rows; j <- 0 until cols) {
        if (i >= infoA.origRows || j >= infoA.origCols) mat(i)(j) = padDefault
      }
      val tmpAddr = memPool.allocate(s"logpad_${node.name}", ddrDriver.matrixSizeBytes(rows, cols))
      ddrDriver.writeLongMatrix(tmpAddr, mat, rows, cols)
      tmpAddr
    } else {
      infoA.ddrAddr
    }

    val outName = node.outputs(0)
    val addrZ = memPool.allocate(s"ddr_$outName", ddrDriver.matrixSizeBytes(rows, cols))

    // B = 零矩阵（预分配），zeroBMode=true 确保每个 tile 从 offset=0 加载零
    tiledElementOpDdr(
      addrA, rows, cols, rows, cols,
      _zeroDdrAddr, rows, cols,
      addrZ, "elementadd", 0, shiftA, 0, actFn, shiftAfterAct, node.name,
      zeroBMode = true
    )
    _currentComputeEndCycle = currentCycle

    val outShape = infoA.originalShape
    val outInfo = DdrTensorInfo(outName, addrZ, infoA.origRows, infoA.origCols, rows, cols, outShape, isPreloaded = false)
    _tensorDdrMap(outName) = outInfo
    _currentOpShape = s"act:${actFn}(${rows}×${cols}) DDR-native"

    val resultId = materializeToHost(addrZ, infoA.origRows, infoA.origCols, rows, cols, outShape, outName)
    _tidToDdr(resultId) = outInfo
    Seq(resultId)
  }

  // ====================== 生命周期 ======================

  private var _started = false

  def start(): Unit = {
    if (!_started) { simCtx.start(); _started = true }
  }

  def shutdown(): Unit = {
    if (_started) {
      printPerfSummary()
      memPool.printSummary()
      simCtx.shutdown()
      _started = false
    }
  }

  private def ensureStarted(): Unit = {
    if (!_started) start()
  }

  // ====================== DDR 原生 Tiling 引擎 ======================

  /**
   * DDR 原生矩阵乘法 Tiling：纯 DMA + Compute 指令。
   * 数据全程在 DDR，移位由 shiftLeft_A/B 在 Slicer 中完成。
   * 无 hostAxi 数据搬运。
   */
  private def tiledHWOpDdr(
    addrA: Long, rowsA: Int, colsA: Int,
    addrB: Long, rowsB: Int, colsB: Int,
    addrZ: Long,
    opName: String, shiftAfterOp: Int,
    shiftA: Int, shiftB: Int,
    activationFn: String, shiftAfterAct: Int,
    nodeName: String
  ): Unit = {
    val k = colsA
    val sideNum = HW_DIM_MULTIPLE
    val maxTileM = MAX_HW_ELEMENTS / math.max(k, 1)
    require(maxTileM >= sideNum, s"$nodeName: K=$k too large for cache")

    val hwTileCapM = (maxTileM / sideNum) * sideNum
    val hwTileCapN = (MAX_HW_ELEMENTS / math.max(k, 1) / sideNum) * sideNum
    val maxTileN   = math.min(colsB, hwTileCapN)
    val hwTileCapMForOutput = math.max(sideNum,
      (MAX_HW_ELEMENTS / math.max(maxTileN, 1) / sideNum) * sideNum)
    val effectiveTileCapM = math.min(hwTileCapM, hwTileCapMForOutput)
    val numTilesM = (rowsA + effectiveTileCapM - 1) / effectiveTileCapM
    val numTilesN = (colsB + hwTileCapN - 1) / hwTileCapN

    val matOp = resolveMatrixOp(opName)
    val actFn = resolveActivation(activationFn)
    _tileCount = 0

    for (mIdx <- 0 until numTilesM) {
      val mOff = mIdx * effectiveTileCapM
      val tM = math.min(rowsA - mOff, effectiveTileCapM)
      for (nIdx <- 0 until numTilesN) {
        val nOff = nIdx * hwTileCapN
        val tN = math.min(colsB - nOff, hwTileCapN)
        _tileCount += 1

        val loadA = (nIdx == 0)

        if (loadA) {
          cacheDriver.setLifeCfgA(numTilesN)
          val p = dmaDriver.makeLoadParams(addrA, k, rowsA, k, tM, 0, mOff)
          if (dmaLogLevel >= 2) println(f"    [DMA-A] LOAD 0x${addrA}%X tile(${tM}×${k}) offset($mOff,0) [#${_tileCount}]")
          dmaDriver.transfer(DmaChannel.A, p)
        }
        // B 每个 tile 都重新加载（lifeCfg=1）。
        // CacheB 是 2-bank FIFO：rdPtr 指向最早写入的 bank，消费完 lifeCnt 次后才轮转。
        // 若 mIdx=0 时连续加载多个 B tile（lifeCfg=numTilesM），FIFO 消费顺序与期望的
        // (B[n=0],B[n=1],...) 不符——rdPtr 会重复读 B[n=0] 直到其 lifeCnt 耗尽。
        cacheDriver.setLifeCfgB(1)
        val pB = dmaDriver.makeLoadParams(addrB, colsB, k, tN, k, nOff, 0)
        if (dmaLogLevel >= 2) println(f"    [DMA-B] LOAD 0x${addrB}%X tile(${k}×${tN}) offset(0,$nOff) [#${_tileCount}]")
        dmaDriver.transfer(DmaChannel.B, pB)

        val inst = new InstSim(
          UID = instDriver.nextUID(), matrixOperation = matOp,
          shiftLeft_AfterMatrixOperation = shiftAfterOp, doTranspose = false,
          activationFunction = actFn, shiftLeft_AfterActivation = shiftAfterAct,
          input0Address = 0, input1Address = 0, outputAddress = 0,
          input0Shape0 = tM, input0Shape1 = k, input1Shape1 = tN,
          shiftLeft_A = shiftA, shiftLeft_B = shiftB
        )
        inst.computeShape()
        val tileStartCyc = if (dmaLogLevel >= 1) currentCycle else 0L
        instDriver.sendInstruction(inst)
        instDriver.waitComputeDone()

        val sp = dmaDriver.makeStoreParams(addrZ, colsB, rowsA, tN, tM, nOff, mOff)
        if (dmaLogLevel >= 2) println(f"    [DMA-Z] STORE → 0x${addrZ}%X tile(${tM}×${tN}) offset($mOff,$nOff) [#${_tileCount}]")
        dmaDriver.transfer(DmaChannel.Z, sp)
        cacheDriver.clearGlobalIntr()
        if (dmaLogLevel >= 1) {
          val tileCyc = currentCycle - tileStartCyc
          println(f"    [TILE ${_tileCount}%d/${numTilesM * numTilesN}%d] matmul ${tM}×${k}×${tN} offset($mOff,$nOff) $tileCyc%,d cyc")
        }
        // DDR-native crossCheck: 读 DDR tile 数据 → SW sim → 比对 HW 结果
        if (crossCheck) {
          val fullA = ddrDriver.readLongMatrix(addrA, rowsA, k)
          val fullB = ddrDriver.readLongMatrix(addrB, k, colsB)
          val tileA = Array.tabulate(tM, k)((i, j) => fullA(mOff + i)(j))
          val tileB = Array.tabulate(k, tN)((i, j) => fullB(i)(nOff + j))
          val refResult = inst.acceleratorSim(
            tileA.map(_.map(BigInt(_))), tileB.map(_.map(BigInt(_))), _acceleratorCfg
          ).map(_.map(_.toLong))
          val fullZ = ddrDriver.readLongMatrix(addrZ, rowsA, colsB)
          var maxErr = 0L; var maxErrPos = (0, 0)
          for (i <- 0 until tM; j <- 0 until tN) {
            val err = math.abs(fullZ(mOff + i)(nOff + j) - refResult(i)(j))
            if (err > maxErr) { maxErr = err; maxErrPos = (i, j) }
          }
          if (maxErr > crossCheckMaxErr) {
            _failedTiles += 1
            val (ei, ej) = maxErrPos
            println(f"  [XCHK-DDR] MATMUL MISMATCH #${_tileCount} ($tM,$k,$tN) offset($mOff,$nOff) maxErr=$maxErr at($ei,$ej) '$nodeName'")
            println(f"    HW=${fullZ(mOff+ei)(nOff+ej)} SW=${refResult(ei)(ej)} shiftA=$shiftA shiftB=$shiftB shiftAfterOp=$shiftAfterOp")
            if (crossCheckAbortAfter > 0 && _failedTiles >= crossCheckAbortAfter)
              throw new RuntimeException(s"[XCHK-DDR] Aborting: ${_failedTiles} tiles exceeded threshold")
          }
        }
      }
    }
    _currentOpTiles = _tileCount
  }

  /**
   * DDR 原生逐元素 Tiling：纯 DMA + Compute 指令。
   */
  /**
   * DDR 原生逐元素 Tiling：纯 DMA + Compute 指令。
   * @param zeroBMode 当 true 时，B 为零矩阵，每个 tile 始终从 addrB+offset=0 加载
   *                  （避免读取超出零矩阵分配范围的 DDR 脏数据）
   */
  private def tiledElementOpDdr(
    addrA: Long, totalRows: Int, totalCols: Int,
    padRowsA: Int, padColsA: Int,
    addrB: Long, padRowsB: Int, padColsB: Int,
    addrZ: Long,
    opName: String, shiftAfterOp: Int,
    shiftA: Int, shiftB: Int,
    activationFn: String, shiftAfterAct: Int,
    nodeName: String,
    zeroBMode: Boolean = false
  ): Unit = {
    val cols = totalCols
    val rows = totalRows
    val sideNum = HW_DIM_MULTIPLE
    val maxRows = MAX_HW_ELEMENTS / cols
    require(maxRows >= sideNum, s"$nodeName: cols=$cols too large")
    val hwTileCap = (maxRows / sideNum) * sideNum
    val numTilesR = (rows + hwTileCap - 1) / hwTileCap

    val matOp = resolveMatrixOp(opName)
    val actFn = resolveActivation(activationFn)
    _tileCount = 0

    for (rIdx <- 0 until numTilesR) {
      val rOff = rIdx * hwTileCap
      val tR = math.min(rows - rOff, hwTileCap)
      _tileCount += 1

      cacheDriver.setLifeCfgA(1)
      val pA = dmaDriver.makeLoadParams(addrA, cols, rows, cols, tR, 0, rOff)
      if (dmaLogLevel >= 2) println(f"    [DMA-A] LOAD 0x${addrA}%X tile(${tR}×${cols}) offset($rOff,0) [#${_tileCount}]")
      dmaDriver.transfer(DmaChannel.A, pA)

      cacheDriver.setLifeCfgB(1)
      // zeroBMode: B 是零矩阵，每个 tile 从 offset=0 加载同一块零区域
      // (避免 offset 超出零矩阵分配范围导致读取脏数据)
      val pB = if (zeroBMode) {
        dmaDriver.makeLoadParams(addrB, cols, tR, cols, tR, 0, 0)
      } else {
        dmaDriver.makeLoadParams(addrB, cols, rows, cols, tR, 0, rOff)
      }
      if (dmaLogLevel >= 2) println(f"    [DMA-B] LOAD 0x${addrB}%X tile(${tR}×${cols}) offset(${if (zeroBMode) 0 else rOff},0) [#${_tileCount}]${if (zeroBMode) " [ZERO]" else ""}")
      dmaDriver.transfer(DmaChannel.B, pB)

      val inst = new InstSim(
        UID = instDriver.nextUID(), matrixOperation = matOp,
        shiftLeft_AfterMatrixOperation = shiftAfterOp, doTranspose = false,
        activationFunction = actFn, shiftLeft_AfterActivation = shiftAfterAct,
        input0Address = 0, input1Address = 0, outputAddress = 0,
        input0Shape0 = tR, input0Shape1 = cols, input1Shape1 = cols,
        shiftLeft_A = shiftA, shiftLeft_B = shiftB
      )
      inst.computeShape()
      val tileStartCyc = if (dmaLogLevel >= 1) currentCycle else 0L
      instDriver.sendInstruction(inst)
      instDriver.waitComputeDone()

      val pZ = dmaDriver.makeStoreParams(addrZ, cols, rows, cols, tR, 0, rOff)
      if (dmaLogLevel >= 2) println(f"    [DMA-Z] STORE → 0x${addrZ}%X tile(${tR}×${cols}) offset($rOff,0) [#${_tileCount}]")
      dmaDriver.transfer(DmaChannel.Z, pZ)
      cacheDriver.clearGlobalIntr()
      if (dmaLogLevel >= 1) {
        val tileCyc = currentCycle - tileStartCyc
        val actStr = if (activationFn != "none") s" act:$activationFn" else ""
        println(f"    [TILE ${_tileCount}%d/${numTilesR}%d] elem ${tR}×${cols} offset($rOff)$actStr $tileCyc%,d cyc")
      }
      // DDR-native crossCheck: 读 DDR tile 数据 → SW sim → 比对 HW 结果
      if (crossCheck) {
        val fullA = ddrDriver.readLongMatrix(addrA, totalRows, cols)
        val bTotalRows = if (zeroBMode) tR else totalRows
        val fullB = ddrDriver.readLongMatrix(addrB, bTotalRows, cols)
        val bRoff = if (zeroBMode) 0 else rOff
        val tileA = Array.tabulate(tR, cols)((i, j) => fullA(rOff + i)(j))
        val tileB = Array.tabulate(tR, cols)((i, j) => fullB(bRoff + i)(j))
        val refResult = inst.acceleratorSim(
          tileA.map(_.map(BigInt(_))), tileB.map(_.map(BigInt(_))), _acceleratorCfg
        ).map(_.map(_.toLong))
        val fullZ = ddrDriver.readLongMatrix(addrZ, totalRows, cols)
        var maxErr = 0L
        for (i <- 0 until tR; j <- 0 until cols) {
          val e = math.abs(fullZ(rOff + i)(j) - refResult(i)(j))
          if (e > maxErr) maxErr = e
        }
        if (maxErr > crossCheckMaxErr) {
          _failedTiles += 1
          println(f"  [XCHK-DDR] ELEM MISMATCH #${_tileCount} ($tR,$cols) offset=$rOff maxErr=$maxErr '$nodeName' sA=$shiftA sB=$shiftB sOp=$shiftAfterOp act=$activationFn sAct=$shiftAfterAct")
          if (crossCheckAbortAfter > 0 && _failedTiles >= crossCheckAbortAfter)
            throw new RuntimeException(s"[XCHK-DDR] Aborting: ${_failedTiles} tiles exceeded threshold")
        }
      }
    }
    _currentOpTiles = _tileCount
  }

  // ====================== 回退路径用 tiledHWOp / tiledElementOp ======================

  override def tiledHWOp(
      matA: Array[Array[Long]], matB: Array[Array[Long]],
      rows: Int, colsA: Int, colsB: Int,
      opName: String, shiftAfterOp: Int,
      activationFn: String, shiftAfterAct: Int,
      nodeName: String
  ): Array[Array[Long]] = {
    ensureStarted()
    val k = colsA
    val sideNum = HW_DIM_MULTIPLE
    val maxTileM = MAX_HW_ELEMENTS / math.max(k, 1)
    require(maxTileM >= sideNum, s"$nodeName: K=$k too large")
    val hwTileCapM = (maxTileM / sideNum) * sideNum
    val hwTileCapN = (MAX_HW_ELEMENTS / math.max(k, 1) / sideNum) * sideNum
    val maxTileN = math.min(colsB, hwTileCapN)
    val hwTileCapMForOutput = math.max(sideNum, (MAX_HW_ELEMENTS / math.max(maxTileN, 1) / sideNum) * sideNum)
    val effectiveTileCapM = math.min(hwTileCapM, hwTileCapMForOutput)
    val numTilesM = (rows + effectiveTileCapM - 1) / effectiveTileCapM
    val numTilesN = (colsB + hwTileCapN - 1) / hwTileCapN

    _currentOpShape = s"matmul(${rows}×${k}×${colsB}) tiles=${numTilesM}×${numTilesN} [FB]"
    val (addrA, allocA) = acquireDdr(matA, rows, k, s"_fb_A_$nodeName", "A", s"matmul ${rows}×${k}×${colsB}")
    val (addrB, allocB) = acquireDdr(matB, k, colsB, s"_fb_B_$nodeName", "B", s"matmul ${rows}×${k}×${colsB}")
    val addrZ = memPool.allocate(s"_fb_Z_$nodeName", ddrDriver.matrixSizeBytes(rows, colsB))
    try {
      val matOp = resolveMatrixOp(opName)
      val actFn = resolveActivation(activationFn)
      _tileCount = 0; _failedTiles = 0
      for (mIdx <- 0 until numTilesM) {
        val mOff = mIdx * effectiveTileCapM; val tM = math.min(rows - mOff, effectiveTileCapM)
        for (nIdx <- 0 until numTilesN) {
          val nOff = nIdx * hwTileCapN; val tN = math.min(colsB - nOff, hwTileCapN)
          _tileCount += 1
          if (nIdx == 0) { cacheDriver.setLifeCfgA(numTilesN); dmaDriver.transfer(DmaChannel.A, dmaDriver.makeLoadParams(addrA, k, rows, k, tM, 0, mOff)) }
          // B 每个 tile 重新加载（lifeCfg=1），原因同 tiledHWOpDdr：CacheB FIFO 消费顺序限制
          cacheDriver.setLifeCfgB(1); dmaDriver.transfer(DmaChannel.B, dmaDriver.makeLoadParams(addrB, colsB, k, tN, k, nOff, 0))
          val inst = new InstSim(instDriver.nextUID(), matOp, shiftAfterOp, false, actFn, shiftAfterAct, 0, 0, 0, tM, k, tN, 0, 0)
          inst.computeShape()
          val tileStartCyc = if (dmaLogLevel >= 1) currentCycle else 0L
          instDriver.sendInstruction(inst); instDriver.waitComputeDone()
          dmaDriver.transfer(DmaChannel.Z, dmaDriver.makeStoreParams(addrZ, colsB, rows, tN, tM, nOff, mOff))
          cacheDriver.clearGlobalIntr()
          if (dmaLogLevel >= 1) {
            val tileCyc = currentCycle - tileStartCyc
            println(f"    [TILE ${_tileCount}%d/${numTilesM * numTilesN}%d] matmul ${tM}×${k}×${tN} offset($mOff,$nOff) $tileCyc%,d cyc [FB]")
          }
          if (crossCheck) crossCheckTile(matA, matB, inst, mOff, tM, nOff, tN, k, nodeName)
        }
      }
      _currentOpTiles = _tileCount
      ddrDriver.readLongMatrix(addrZ, rows, colsB)
    } finally {
      if (allocA) memPool.release(s"_fb_A_$nodeName")
      if (allocB) memPool.release(s"_fb_B_$nodeName")
      memPool.release(s"_fb_Z_$nodeName")
    }
  }

  override def tiledElementOp(
      matA: Array[Array[Long]], matB: Array[Array[Long]],
      rows: Int, cols: Int,
      opName: String, shiftAfterOp: Int,
      activationFn: String, shiftAfterAct: Int,
      nodeName: String
  ): Array[Array[Long]] = {
    ensureStarted()
    val sideNum = HW_DIM_MULTIPLE
    val maxRows = MAX_HW_ELEMENTS / cols
    require(maxRows >= sideNum, s"$nodeName: cols=$cols too large")
    val hwTileCap = (maxRows / sideNum) * sideNum
    val numTilesR = (rows + hwTileCap - 1) / hwTileCap
    _currentOpShape = s"elem(${rows}×${cols}) tiles=${numTilesR} [FB]"
    val (addrA, allocA) = acquireDdr(matA, rows, cols, s"_fb_eA_$nodeName", "A", s"elem ${rows}×${cols}")
    val (addrB, allocB) = acquireDdr(matB, rows, cols, s"_fb_eB_$nodeName", "B", s"elem ${rows}×${cols}")
    val addrZ = memPool.allocate(s"_fb_eZ_$nodeName", ddrDriver.matrixSizeBytes(rows, cols))
    try {
      val matOp = resolveMatrixOp(opName)
      val actFn = resolveActivation(activationFn)
      _tileCount = 0; _failedTiles = 0
      for (rIdx <- 0 until numTilesR) {
        val rOff = rIdx * hwTileCap; val tR = math.min(rows - rOff, hwTileCap)
        _tileCount += 1
        cacheDriver.setLifeCfgA(1); dmaDriver.transfer(DmaChannel.A, dmaDriver.makeLoadParams(addrA, cols, rows, cols, tR, 0, rOff))
        cacheDriver.setLifeCfgB(1); dmaDriver.transfer(DmaChannel.B, dmaDriver.makeLoadParams(addrB, cols, rows, cols, tR, 0, rOff))
        val inst = new InstSim(instDriver.nextUID(), matOp, shiftAfterOp, false, actFn, shiftAfterAct, 0, 0, 0, tR, cols, cols, 0, 0)
        inst.computeShape()
        val tileStartCyc = if (dmaLogLevel >= 1) currentCycle else 0L
        instDriver.sendInstruction(inst); instDriver.waitComputeDone()
        dmaDriver.transfer(DmaChannel.Z, dmaDriver.makeStoreParams(addrZ, cols, rows, cols, tR, 0, rOff))
        cacheDriver.clearGlobalIntr()
        if (dmaLogLevel >= 1) {
          val tileCyc = currentCycle - tileStartCyc
          println(f"    [TILE ${_tileCount}%d/${numTilesR}%d] elem ${tR}×${cols} offset($rOff) $tileCyc%,d cyc [FB]")
        }
        if (crossCheck) crossCheckElementTile(matA, matB, inst, rOff, tR, cols, nodeName)
      }
      _currentOpTiles = _tileCount
      ddrDriver.readLongMatrix(addrZ, rows, cols)
    } finally {
      if (allocA) memPool.release(s"_fb_eA_$nodeName")
      if (allocB) memPool.release(s"_fb_eB_$nodeName")
      memPool.release(s"_fb_eZ_$nodeName")
    }
  }

  // ====================== 辅助方法 ======================

  private def resolveMatrixOp(opName: String): MatrixOperation_TypeDef.E = opName.toLowerCase match {
    case "matmul"     => MatrixOperation_TypeDef.MatMul
    case "elementadd" => MatrixOperation_TypeDef.ElementAdd
    case "elementmul" => MatrixOperation_TypeDef.ElementMul
    case "elementmax" => MatrixOperation_TypeDef.ElementMax
    case other        => throw new IllegalArgumentException(s"Unknown matrix operation: $other")
  }

  private def resolveActivation(actFn: String): Activation_TypeDef.E = actFn.toLowerCase match {
    case "none"     => Activation_TypeDef.None
    case "relu"     => Activation_TypeDef.Relu
    case "exp"      => Activation_TypeDef.Exp
    case "log"      => Activation_TypeDef.Log
    case "softplus" => Activation_TypeDef.Softplus
    case other      => throw new IllegalArgumentException(s"Unknown activation: $other")
  }

  private def crossCheckTile(
    matA: Array[Array[Long]], matB: Array[Array[Long]], inst: InstSim,
    mOff: Int, tM: Int, nOff: Int, tN: Int, k: Int, nodeName: String
  ): Unit = {
    val tileA = Array.tabulate(tM, k)((i, j) => matA(mOff + i)(j))
    val tileB = Array.tabulate(k, tN)((i, j) => matB(i)(nOff + j))
    val refResult = inst.acceleratorSim(tileA.map(_.map(BigInt(_))), tileB.map(_.map(BigInt(_))), _acceleratorCfg).map(_.map(_.toLong))
    val addrZ = memPool.getAddress(s"_fb_Z_$nodeName").get
    val fullResult = ddrDriver.readLongMatrix(addrZ, inst.outputShape0 + mOff, nOff + tN)
    var maxErr = 0L; var maxErrPos = (0, 0)
    for (i <- 0 until tM; j <- 0 until tN) {
      val err = math.abs(fullResult(mOff + i)(nOff + j) - refResult(i)(j))
      if (err > maxErr) { maxErr = err; maxErrPos = (i, j) }
    }
    if (maxErr > crossCheckMaxErr) {
      _failedTiles += 1
      val (ei, ej) = maxErrPos
      println(f"  [XCHK] MISMATCH #${_tileCount} ($tM,$k,$tN) offset($mOff,$nOff) maxErr=$maxErr at($ei,$ej) '$nodeName'")
      if (crossCheckAbortAfter > 0 && _failedTiles >= crossCheckAbortAfter)
        throw new RuntimeException(s"[XCHK] Aborting: ${_failedTiles} tiles exceeded threshold")
    }
  }

  private def crossCheckElementTile(
    matA: Array[Array[Long]], matB: Array[Array[Long]], inst: InstSim,
    rOff: Int, tR: Int, cols: Int, nodeName: String
  ): Unit = {
    val tileA = Array.tabulate(tR, cols)((i, j) => matA(rOff + i)(j))
    val tileB = Array.tabulate(tR, cols)((i, j) => matB(rOff + i)(j))
    val refResult = inst.acceleratorSim(tileA.map(_.map(BigInt(_))), tileB.map(_.map(BigInt(_))), _acceleratorCfg).map(_.map(_.toLong))
    val addrZ = memPool.getAddress(s"_fb_eZ_$nodeName").get
    val fullResult = ddrDriver.readLongMatrix(addrZ, rOff + tR, cols)
    var maxErr = 0L
    for (i <- 0 until tR; j <- 0 until cols) { val e = math.abs(fullResult(rOff + i)(j) - refResult(i)(j)); if (e > maxErr) maxErr = e }
    if (maxErr > crossCheckMaxErr) { _failedTiles += 1; println(f"  [XCHK] ELEM MISMATCH #${_tileCount} ($tR,$cols) offset=$rOff maxErr=$maxErr '$nodeName'") }
  }

  // ====================== 访问器 ======================

  def getMemoryPool: MemoryPool = { ensureStarted(); memPool }
  def getSimContext: SimContext = { ensureStarted(); simCtx }
  def getAcceleratorCfg: AcceleratorCfg = _acceleratorCfg
  def preloadedTensorCount: Int = _tensorDdrMap.count(_._2.isPreloaded)
  def preloadCycles: Long = _preloadCycles
  def ddrNativeOpCount: Int = _ddrNativeOps
  def fallbackOpCount: Int = _fallbackOps
}

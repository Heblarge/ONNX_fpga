package runtime.dispatch.ops

import Accelerator.{AcceleratorSimInterface, InstJavaTODO}
import runtime.bridge.{TorchBridge, Tensor}
import runtime.engine._

/**
 * HWOp — 硬件算子接口。
 *
 * 每个 HWOp 实现一种或多种 ONNX 算子的硬件执行逻辑。
 * 通过 DispatchContext 访问共享基础设施（tiling、数据搬运、移位等）。
 */
trait HWOp {
  /** 该算子支持的 ONNX opType 集合。 */
  def opTypes: Set[String]

  /**
   * 执行硬件算子。
   * @param opType  具体触发的 ONNX opType（对于支持多种 opType 的算子有用）
   * @param node    当前图节点
   * @param inputIds  输入张量 ID 列表
   * @param graph   完整计算图
   * @param ctx     分发上下文
   * @return 输出张量 ID 列表
   */
  def execute(opType: String, node: GraphNode, inputIds: Seq[String],
              graph: OnnxGraph, ctx: DispatchContext): Seq[String]
}

/**
 * DispatchContext — 算子可使用的共享基础设施。
 *
 * 由 AcceleratorDispatcher 实现，将 tiling、数据传输、移位计算等
 * 以受控方式暴露给各个算子实现。
 */
trait DispatchContext {

  // ==================== 常量 ====================
  def HW_DIM_MULTIPLE: Int
  def MAX_HW_ELEMENTS: Int
  def hwFracWidth: Long
  def pushDtype: String

  // ==================== 调试状态 ====================
  def hwOpCount: Int
  def debugLimit: Int
  def debugNodeNames: Set[String]
  def useFloatActivations: Boolean

  // ==================== 数据传输 ====================
  /** 从 Python 拉取张量数据为 2D Long 数组 */
  def pullToLong2D(tensorId: String, rows: Int, cols: Int): Array[Array[Long]]
  /** 将 2D Long 数组推送回 Python 成为张量 */
  def pushFromLong2D(data: Array[Array[Long]], rows: Int, cols: Int,
                     dtype: String = "float32"): String
  /** 获取张量形状 */
  def getShape(tid: String): Array[Long]
  /** 从张量 ID 创建 Tensor 引用 */
  def mkRef(id: String): Tensor

  // ==================== 图查询 ====================
  /** 获取张量的生产者节点的 fpga_out_shift */
  def getProducerOutputShift(graph: OnnxGraph, tensorName: String, default: Long): Long

  // ==================== 工具 ====================
  def ceilToMultiple(v: Int, m: Int): Int

  // ==================== Tiling ====================
  /** 分块矩阵运算（matmul 等）*/
  def tiledHWOp(matA: Array[Array[Long]], matB: Array[Array[Long]],
                rows: Int, colsA: Int, colsB: Int,
                opName: String, shiftAfterOp: Int,
                activationFn: String = "none", shiftAfterAct: Int = 0,
                nodeName: String = ""): Array[Array[Long]]

  /** 分块逐元素运算（elementadd 等）*/
  def tiledElementOp(matA: Array[Array[Long]], matB: Array[Array[Long]],
                     rows: Int, cols: Int,
                     opName: String, shiftAfterOp: Int,
                     activationFn: String = "none", shiftAfterAct: Int = 0,
                     nodeName: String = ""): Array[Array[Long]]

  // ==================== 跨算子调用 ====================
  /** 分发到另一个算子（用于组合算子如 Gemm = MatMul + bias） */
  def dispatchToOp(opType: String, node: GraphNode, inputIds: Seq[String],
                   graph: OnnxGraph): Seq[String]
}

package runtime.engine

import runtime.bridge.{TorchBridge, Tensor}
import scala.collection.mutable

/**
 * Session — Executes an OnnxGraph via the Python PyTorch bridge.
 *
 * All tensor computation happens in Python/PyTorch. When a node should run
 * on hardware, data is transferred to Scala, executed via the HWDispatcher,
 * and results are sent back.
 *
 * Intermediate tensors for every node are retained, allowing extraction at any point.
 */
class Session(val graph: OnnxGraph, val debug: Boolean = false) extends AutoCloseable {

  /** Map from tensor name to Python-side tensor ID. */
  private val tensorMap = mutable.Map[String, String]()

  /** Tracks which tensor IDs we own (vs initializers). */
  private val ownedTensors = mutable.Set[String]()

  /** Register a tensor by name. */
  def setTensor(name: String, tensorId: String): Unit = {
    tensorMap(name) = tensorId
  }

  /** Get tensor ID by name. */
  def getTensorId(name: String): Option[String] = tensorMap.get(name)

  /** Feed input data. */
  def feed(name: String, tensor: Tensor): Unit = {
    tensorMap(name) = tensor.id
    ownedTensors += tensor.id
  }

  /** Get an intermediate or output tensor. */
  def getTensor(name: String): Tensor = {
    val tid = tensorMap.getOrElse(name,
      throw new NoSuchElementException(s"Tensor '$name' not found"))
    val info = TorchBridge.call("get_info", Map("id" -> tid)).asInstanceOf[Map[String, Any]]
    Tensor.fromInfo(info)
  }

  /** Progress printing interval (0 = disabled). Prints every N nodes. */
  var progressInterval: Int = 0

  /** Execute the full graph in topological order. */
  def forward(hwDispatcher: Option[HWDispatcher] = None): Unit = {
    // Load initializers
    graph.initializers.foreach { case (name, tid) =>
      if (!tensorMap.contains(name)) tensorMap(name) = tid
    }

    val totalNodes = graph.topologicalOrder.size
    var nodeIdx = 0
    var hwCount = 0
    var swCount = 0
    val startTime = System.currentTimeMillis()

    if (progressInterval > 0) {
      println(s"[Forward] Starting: $totalNodes nodes, ${graph.userInputs.size} inputs, ${graph.outputs.size} outputs")
    }

    for (node <- graph.topologicalOrder) {
      nodeIdx += 1
      val inputIds: Seq[String] = node.inputs.toSeq.map { inName =>
        if (inName.isEmpty) null
        else tensorMap.getOrElse(inName, null)
      }

      if (debug) {
        val shapes = inputIds.map { tid =>
          if (tid == null) "None"
          else {
            val info = TorchBridge.call("get_info", Map("id" -> tid)).asInstanceOf[Map[String, Any]]
            info("shape").toString
          }
        }
        println(s"[Torch] ${node.opType} '${node.name}' inputs=${shapes.mkString(",")}")
      }

      // Check if this node should be dispatched to HW
      val isHw = hwDispatcher.exists(_.shouldDispatch(node))
      val outputIds: Seq[String] = if (isHw) {
        hwCount += 1
        hwDispatcher.get.execute(node, inputIds, graph)
      } else {
        swCount += 1
        executeOnPyTorch(node, inputIds)
      }

      // Progress printing
      if (progressInterval > 0 && (nodeIdx % progressInterval == 0 || nodeIdx == totalNodes)) {
        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
        val pct = nodeIdx * 100 / totalNodes
        val marker = if (isHw) "HW" else "SW"
        println(f"[Forward] $nodeIdx%d/$totalNodes ($pct%d%%) elapsed=${elapsed}%.1fs hw=$hwCount sw=$swCount | [$marker] ${node.opType} '${node.name}'")
      }

      // Store outputs
      node.outputs.zip(outputIds).foreach { case (outName, outId) =>
        tensorMap(outName) = outId
        ownedTensors += outId
      }
    }

    if (progressInterval > 0) {
      val totalTime = (System.currentTimeMillis() - startTime) / 1000.0
      println(f"[Forward] Done: $totalNodes nodes ($hwCount HW + $swCount SW) in $totalTime%.1fs")
    }
  }

  /** Execute a node on PyTorch via the bridge. */
  private def executeOnPyTorch(node: GraphNode, inputIds: Seq[String]): Seq[String] = {
    val attrs = node.attrsForBridge

    val result = TorchBridge.call("op", Map(
      "op" -> node.opType,
      "inputs" -> inputIds,
      "attrs" -> attrs
    ))

    result.asInstanceOf[List[Map[String, Any]]].map { info =>
      info("id").toString
    }
  }

  /** Get all output tensor names and their Tensor refs. */
  def getOutputs: Map[String, Tensor] = {
    graph.outputs.flatMap { out =>
      tensorMap.get(out.name).map { tid =>
        val info = TorchBridge.call("get_info", Map("id" -> tid)).asInstanceOf[Map[String, Any]]
        out.name -> Tensor.fromInfo(info)
      }
    }.toMap
  }

  override def close(): Unit = {
    tensorMap.clear()
    ownedTensors.clear()
  }
}

/**
 * Trait for hardware dispatch decisions and execution.
 */
trait HWDispatcher {
  /** Should this node be dispatched to hardware? */
  def shouldDispatch(node: GraphNode): Boolean

  /** Execute a node on hardware, returning output tensor IDs. */
  def execute(node: GraphNode, inputIds: Seq[String], graph: OnnxGraph): Seq[String]
}

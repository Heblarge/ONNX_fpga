package runtime.engine

import scala.collection.mutable

// ============ Attribute Types ============
sealed trait AttrValue
case class IntAttr(value: Long) extends AttrValue
case class IntsAttr(values: Seq[Long]) extends AttrValue
case class FloatAttr(value: Float) extends AttrValue
case class FloatsAttr(values: Seq[Float]) extends AttrValue
case class StringAttr(value: String) extends AttrValue
case class StringsAttr(values: Seq[String]) extends AttrValue
case class TensorRefAttr(tensorId: String) extends AttrValue

// ============ Graph Node ============
case class GraphNode(
    name: String,
    opType: String,
    domain: String,
    inputs: Array[String],
    outputs: Array[String],
    attrs: Map[String, AttrValue]
) {
  def getInt(key: String, default: Long = 0L): Long =
    attrs.get(key).collect { case IntAttr(v) => v }.getOrElse(default)

  def getInts(key: String, default: Seq[Long] = Nil): Seq[Long] =
    attrs.get(key).collect { case IntsAttr(v) => v }.getOrElse(default)

  def getFloat(key: String, default: Float = 0f): Float =
    attrs.get(key).collect { case FloatAttr(v) => v }.getOrElse(default)

  def getFloats(key: String, default: Seq[Float] = Nil): Seq[Float] =
    attrs.get(key).collect { case FloatsAttr(v) => v }.getOrElse(default)

  def getString(key: String, default: String = ""): String =
    attrs.get(key).collect { case StringAttr(v) => v }.getOrElse(default)

  def getTensorRef(key: String): Option[String] =
    attrs.get(key).collect { case TensorRefAttr(id) => id }

  def fpgaInShift: Seq[Long] = getInts("fpga_in_shift", Seq(0L))
  def fpgaOutShift: Seq[Long] = getInts("fpga_out_shift", Seq(0L))

  /** Convert attrs to a Map[String,Any] for sending to Python bridge. */
  def attrsForBridge: Map[String, Any] = attrs.flatMap {
    case (k, IntAttr(v))      => Some(k -> v)
    case (k, IntsAttr(v))     => Some(k -> v)
    case (k, FloatAttr(v))    => Some(k -> v.toDouble)
    case (k, FloatsAttr(v))   => Some(k -> v.map(_.toDouble))
    case (k, StringAttr(v))   => Some(k -> v)
    case (k, TensorRefAttr(id)) => Some(k -> id)
    case _ => None
  }
}

// ============ IO Info ============
case class IOInfo(name: String, dtypeProto: Int, shape: Array[Long]) {
  def dtypeString: String = dtypeProto match {
    case 1  => "float32"
    case 11 => "float64"
    case 6  => "int32"
    case 7  => "int64"
    case 5  => "int16"
    case 3  => "int8"
    case 2  => "uint8"
    case 9  => "bool"
    case 10 => "float16"
    case 16 => "bfloat16"
    case _  => "float32"
  }
}

// ============ Graph ============
case class OnnxGraph(
    nodes: Seq[GraphNode],
    initializers: Map[String, String],  // name -> python tensor ID
    inputs: Seq[IOInfo],
    outputs: Seq[IOInfo],
    opsetVersion: Long
) {
  lazy val producerMap: Map[String, GraphNode] = {
    val m = mutable.Map[String, GraphNode]()
    nodes.foreach(n => n.outputs.foreach(o => m(o) = n))
    m.toMap
  }

  lazy val topologicalOrder: Seq[GraphNode] = {
    val nodeByOutput = mutable.Map[String, GraphNode]()
    nodes.foreach(n => n.outputs.foreach(o => nodeByOutput(o) = n))

    val visited = mutable.LinkedHashSet[String]()
    val visiting = mutable.Set[String]()

    def dfs(n: GraphNode): Unit = {
      val key = n.name
      if (visited.contains(key) || visiting.contains(key)) return
      visiting += key
      n.inputs.foreach(in => nodeByOutput.get(in).foreach(dfs))
      visiting -= key
      visited += key
    }

    outputs.foreach(out => nodeByOutput.get(out.name).foreach(dfs))
    nodes.foreach(dfs)

    val keyToNode = nodes.map(n => n.name -> n).toMap
    visited.toSeq.map(keyToNode)
  }

  lazy val userInputs: Seq[IOInfo] =
    inputs.filterNot(vi => initializers.contains(vi.name))
}

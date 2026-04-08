package runtime.engine

import runtime.bridge.{TorchBridge, Tensor}

/**
 * OnnxLoader — Loads an ONNX model via the Python bridge into an OnnxGraph.
 *
 * The model file is parsed by the Python process (using the onnx library),
 * and the graph structure, initializers, and metadata are transferred to Scala.
 */
object OnnxLoader {
  def load(path: String): OnnxGraph = {
    val result = TorchBridge.call("load_model", Map("path" -> path))
      .asInstanceOf[Map[String, Any]]

    val opsetVersion = toLong(result("opsetVersion"))
    val irVersion = toLong(result("irVersion"))
    println(s"[OnnxLoader] Loaded model: IR=$irVersion, opset=$opsetVersion")

    val nodesRaw = result("nodes").asInstanceOf[List[Map[String, Any]]]
    val nodes = nodesRaw.map(parseNode)

    val initsRaw = result("initializers").asInstanceOf[Map[String, Any]]
    val initializers = initsRaw.map { case (name, tidAny) =>
      name -> tidAny.toString
    }

    val inputs = result("inputs").asInstanceOf[List[Map[String, Any]]].map(parseValueInfo)
    val outputs = result("outputs").asInstanceOf[List[Map[String, Any]]].map(parseValueInfo)

    println(s"[OnnxLoader] ${nodes.size} nodes, ${initializers.size} initializers, " +
      s"${inputs.size} inputs, ${outputs.size} outputs")

    OnnxGraph(nodes, initializers, inputs, outputs, opsetVersion)
  }

  private def parseNode(raw: Map[String, Any]): GraphNode = {
    val attrsRaw = raw.getOrElse("attrs", Map.empty).asInstanceOf[Map[String, Any]]
    val attrs = attrsRaw.map { case (name, vRaw) =>
      val v = vRaw.asInstanceOf[Map[String, Any]]
      val attrType = v("type").toString
      val value: AttrValue = attrType match {
        case "int"     => IntAttr(toLong(v("value")))
        case "ints"    => IntsAttr(v("value").asInstanceOf[List[Any]].map(toLong))
        case "float"   => FloatAttr(toFloat(v("value")))
        case "floats"  => FloatsAttr(v("value").asInstanceOf[List[Any]].map(toFloat))
        case "string"  => StringAttr(v("value").toString)
        case "strings" => StringsAttr(v("value").asInstanceOf[List[Any]].map(_.toString))
        case "tensor"  => TensorRefAttr(v("value").toString)
        case _ => StringAttr(v.getOrElse("value", "").toString)
      }
      name -> value
    }

    GraphNode(
      name = raw("name").toString,
      opType = raw("opType").toString,
      domain = raw.getOrElse("domain", "").toString,
      inputs = raw("inputs").asInstanceOf[List[Any]].map(_.toString).toArray,
      outputs = raw("outputs").asInstanceOf[List[Any]].map(_.toString).toArray,
      attrs = attrs
    )
  }

  private def parseValueInfo(raw: Map[String, Any]): IOInfo = {
    IOInfo(
      name = raw("name").toString,
      dtypeProto = toLong(raw("dtype")).toInt,
      shape = raw("shape").asInstanceOf[List[Any]].map(toLong).toArray
    )
  }

  private[runtime] def toLong(v: Any): Long = v match {
    case d: Double => d.toLong
    case l: Long => l
    case i: Int => i.toLong
    case s => s.toString.toDouble.toLong
  }

  private[runtime] def toFloat(v: Any): Float = v match {
    case d: Double => d.toFloat
    case f: Float => f
    case i: Int => i.toFloat
    case s => s.toString.toFloat
  }
}

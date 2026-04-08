package runtime.bridge

import java.io._
import java.nio.{ByteBuffer, ByteOrder}
import java.util.Base64

import scala.collection.mutable

/**
 * TorchBridge — Scala client communicating with the Python torch_bridge.py server.
 *
 * Manages a subprocess running Python/PyTorch, exchanging JSON-line messages
 * over stdin/stdout. All tensor storage lives in the Python process.
 */
object TorchBridge {
  private var process: Process = _
  private var writer: BufferedWriter = _
  private var reader: BufferedReader = _
  private var _initialized = false
  private var _torchVersion: String = ""
  private var _cudaAvailable = false

  private val condaEnv: String = sys.env.getOrElse("TORCH_BRIDGE_CONDA",
    "/home/zhou/miniconda3/envs/spatten")
  private val bridgeScript: String = sys.env.getOrElse("TORCH_BRIDGE_SCRIPT", {
    // Find the bridge script: check resources first, then relative paths
    val candidates = Seq(
      "sw/scala/main/resources/torch_bridge.py",
      "sw/scala/runtime/bridge/torch_bridge.py",
      "torch_bridge.py"
    )
    val workDir = sys.props.getOrElse("user.dir", ".")
    candidates.map(c => new File(workDir, c)).find(_.exists()).map(_.getAbsolutePath).getOrElse(
      throw new FileNotFoundException("Cannot find torch_bridge.py. Set TORCH_BRIDGE_SCRIPT env var.")
    )
  })

  def isInitialized: Boolean = _initialized
  def torchVersion: String = _torchVersion
  def cudaAvailable: Boolean = _cudaAvailable

  /** Start the Python bridge process. */
  def init(): Unit = synchronized {
    if (_initialized) return

    val pythonExe = condaEnv + "/bin/python3"
    val pb = new ProcessBuilder(pythonExe, bridgeScript)
    pb.redirectErrorStream(false)
    pb.environment().put("PYTHONUNBUFFERED", "1")

    process = pb.start()
    writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream, "UTF-8"))
    reader = new BufferedReader(new InputStreamReader(process.getInputStream, "UTF-8"))

    // Start a thread to consume stderr and print to System.err
    val stderr = process.getErrorStream
    val errThread = new Thread(() => {
      val errReader = new BufferedReader(new InputStreamReader(stderr))
      var line: String = null
      while ({ line = errReader.readLine(); line != null }) {
        System.err.println(s"[PyBridge] $line")
      }
    }, "torch-bridge-stderr")
    errThread.setDaemon(true)
    errThread.start()

    // Read the "ready" signal
    val readyLine = reader.readLine()
    if (readyLine == null) {
      throw new RuntimeException("Python bridge process exited immediately")
    }
    val readyResp = parseJson(readyLine)
    if (readyResp.get("status") != Some("ready")) {
      throw new RuntimeException(s"Expected 'ready' from bridge, got: $readyLine")
    }
    _torchVersion = readyResp.getOrElse("torch", "unknown").toString
    _cudaAvailable = readyResp.get("cuda").contains(true)
    _initialized = true

    println(s"[TorchBridge] Connected. PyTorch=${_torchVersion}, CUDA=${_cudaAvailable}")
  }

  /** Shutdown the Python bridge. */
  def shutdown(): Unit = synchronized {
    if (!_initialized) return
    try {
      sendRaw("""{"cmd":"shutdown"}""")
      writer.close()
      reader.close()
      process.waitFor()
    } catch {
      case _: Exception => process.destroyForcibly()
    }
    _initialized = false
  }

  private def sendRaw(json: String): Unit = {
    writer.write(json)
    writer.newLine()
    writer.flush()
  }

  private def readResponse(): Map[String, Any] = {
    val line = reader.readLine()
    if (line == null) throw new RuntimeException("Python bridge closed unexpectedly")
    val resp = parseJson(line)
    if (resp.contains("error")) {
      val tb = resp.getOrElse("traceback", "").toString
      throw new RuntimeException(s"Python bridge error: ${resp("error")}\n$tb")
    }
    resp
  }

  /** Send a command and get the result. Thread-safe. */
  def call(cmd: String, args: Map[String, Any] = Map.empty): Any = synchronized {
    require(_initialized, "TorchBridge not initialized. Call TorchBridge.init() first.")
    val jsonStr = toJson(Map("cmd" -> cmd, "args" -> args))
    sendRaw(jsonStr)
    val resp = readResponse()
    resp("result")
  }

  // ============ JSON helpers (no external dependency) ============

  private def parseJson(s: String): Map[String, Any] = {
    SimpleJson.parse(s) match {
      case m: Map[_, _] => m.asInstanceOf[Map[String, Any]]
      case _ => throw new RuntimeException(s"Failed to parse JSON: ${s.take(200)}")
    }
  }

  def toJson(v: Any): String = v match {
    case null => "null"
    case b: Boolean => if (b) "true" else "false"
    case n: Int => n.toString
    case n: Long => n.toString
    case n: Double =>
      if (n.isInfinite || n.isNaN) s""""$n""""
      else n.toString
    case n: Float =>
      if (n.isInfinite || n.isNaN) s""""$n""""
      else n.toDouble.toString
    case s: String =>
      val escaped = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
      s""""$escaped""""
    case seq: Seq[_] => seq.map(toJson).mkString("[", ",", "]")
    case arr: Array[_] => arr.map(toJson).mkString("[", ",", "]")
    case m: Map[_, _] =>
      m.map { case (k, v) => s"${toJson(k.toString)}:${toJson(v)}" }.mkString("{", ",", "}")
    case _ => s""""${v.toString}""""
  }
}

/**
 * Tensor — A reference to a tensor stored in the Python bridge process.
 * Operations are lazy; they send commands to Python for execution.
 */
class Tensor private[runtime](val id: String, private var _shape: Array[Long], private var _dtype: String) {
  def shape: Array[Long] = _shape
  def dtype: String = _dtype
  def rank: Int = _shape.length
  def length: Long = if (_shape.isEmpty) 1L else _shape.product
  def size(dim: Int): Long = _shape(if (dim < 0) _shape.length + dim else dim)

  /** Execute an op that produces a single output tensor. */
  private def unaryOp(op: String, attrs: Map[String, Any] = Map.empty): Tensor = {
    Tensor.op(op, Seq(this), attrs).head
  }

  private def binaryOp(op: String, other: Tensor, attrs: Map[String, Any] = Map.empty): Tensor = {
    Tensor.op(op, Seq(this, other), attrs).head
  }

  def add(other: Tensor): Tensor = binaryOp("Add", other)
  def sub(other: Tensor): Tensor = binaryOp("Sub", other)
  def mul(other: Tensor): Tensor = binaryOp("Mul", other)
  def div(other: Tensor): Tensor = binaryOp("Div", other)
  def matmul(other: Tensor): Tensor = binaryOp("MatMul", other)
  def neg(): Tensor = unaryOp("Neg")
  def abs(): Tensor = unaryOp("Abs")
  def exp(): Tensor = unaryOp("Exp")
  def log(): Tensor = unaryOp("Log")
  def relu(): Tensor = unaryOp("Relu")
  def sigmoid(): Tensor = unaryOp("Sigmoid")

  /** Get raw float data from the Python side. */
  def toFloatArray(): Array[Float] = {
    val resp = TorchBridge.call("get_tensor", Map("id" -> id)).asInstanceOf[Map[String, Any]]
    val b64 = resp("data_b64").toString
    val raw = Base64.getDecoder.decode(b64)
    val dt = resp.getOrElse("dtype", _dtype).toString
    dt match {
      case "float32" =>
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val arr = new Array[Float](raw.length / 4)
        buf.asFloatBuffer().get(arr)
        arr
      case "float64" =>
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val arr = new Array[Double](raw.length / 8)
        buf.asDoubleBuffer().get(arr)
        arr.map(_.toFloat)
      case "int64" =>
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val arr = new Array[Long](raw.length / 8)
        buf.asLongBuffer().get(arr)
        arr.map(_.toFloat)
      case "int32" =>
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val arr = new Array[Int](raw.length / 4)
        buf.asIntBuffer().get(arr)
        arr.map(_.toFloat)
      case _ =>
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val arr = new Array[Float](raw.length / 4)
        buf.asFloatBuffer().get(arr)
        arr
    }
  }

  /** Get raw long data from the Python side. */
  def toLongArray(): Array[Long] = {
    val resp = TorchBridge.call("get_tensor", Map("id" -> id)).asInstanceOf[Map[String, Any]]
    val b64 = resp("data_b64").toString
    val raw = Base64.getDecoder.decode(b64)
    val dt = resp.getOrElse("dtype", _dtype).toString
    dt match {
      case "int64" =>
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val arr = new Array[Long](raw.length / 8)
        buf.asLongBuffer().get(arr)
        arr
      case "float32" =>
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val arr = new Array[Float](raw.length / 4)
        buf.asFloatBuffer().get(arr)
        arr.map(_.toLong)
      case _ =>
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val arr = new Array[Float](raw.length / 4)
        buf.asFloatBuffer().get(arr)
        arr.map(_.toLong)
    }
  }

  /** Get raw double data. */
  def toDoubleArray(): Array[Double] = toFloatArray().map(_.toDouble)

  /** Get as 2D long array for HW interface. */
  def toLongArray2D(rows: Int, cols: Int): Array[Array[Long]] = {
    val resp = TorchBridge.call("to_long_array_2d",
      Map("id" -> id, "rows" -> rows, "cols" -> cols)).asInstanceOf[Map[String, Any]]
    val b64 = resp("data_b64").toString
    val raw = Base64.getDecoder.decode(b64)
    val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
    val flat = new Array[Long](rows * cols)
    buf.asLongBuffer().get(flat)
    Array.tabulate(rows)(r => flat.slice(r * cols, (r + 1) * cols))
  }

  def getDouble(idx: Long): Double = {
    val arr = toFloatArray()
    arr(idx.toInt).toDouble
  }

  def free(): Unit = {
    TorchBridge.call("delete_tensor", Map("id" -> id))
  }

  override def toString: String = s"Tensor(id=$id, shape=[${_shape.mkString(",")}], dtype=${_dtype})"
}

object Tensor {

  /** Create from Python bridge response info. */
  def fromInfo(info: Map[String, Any]): Tensor = {
    val id = info("id").toString
    val shape = info("shape").asInstanceOf[List[Any]].map {
      case d: Double => d.toLong
      case d: Long => d
      case d: Int => d.toLong
      case d => d.toString.toDouble.toLong
    }.toArray
    val dtype = info.getOrElse("dtype", "float32").toString
    new Tensor(id, shape, dtype)
  }

  /** Execute an operation returning one or more tensors. */
  def op(opName: String, inputs: Seq[Tensor], attrs: Map[String, Any] = Map.empty): Seq[Tensor] = {
    val inputIds = inputs.map(t => if (t != null) t.id else null)
    val result = TorchBridge.call("op", Map(
      "op" -> opName,
      "inputs" -> inputIds,
      "attrs" -> attrs
    ))
    result.asInstanceOf[List[Map[String, Any]]].map(fromInfo)
  }

  /** Create a tensor filled with random values in [-1, 1]. */
  def rand(shape: Array[Long], dtype: String = "float32"): Tensor = {
    val info = TorchBridge.call("create_tensor", Map(
      "shape" -> shape.toSeq,
      "dtype" -> dtype,
      "rand" -> "uniform"
    )).asInstanceOf[Map[String, Any]]
    fromInfo(info)
  }

  /** Create a tensor filled with zeros. */
  def zeros(shape: Array[Long], dtype: String = "float32"): Tensor = {
    val info = TorchBridge.call("create_tensor", Map(
      "shape" -> shape.toSeq,
      "dtype" -> dtype
    )).asInstanceOf[Map[String, Any]]
    fromInfo(info)
  }

  /** Create a scalar tensor. */
  def scalar(value: Double, dtype: String = "float32"): Tensor = {
    val info = TorchBridge.call("create_tensor", Map(
      "shape" -> Seq.empty[Int],
      "dtype" -> dtype,
      "fill" -> value
    )).asInstanceOf[Map[String, Any]]
    fromInfo(info)
  }

  /** Create tensor from float array. */
  def fromFloats(data: Array[Float], shape: Array[Long]): Tensor = {
    val buf = ByteBuffer.allocate(data.length * 4).order(ByteOrder.LITTLE_ENDIAN)
    data.foreach(buf.putFloat)
    val b64 = Base64.getEncoder.encodeToString(buf.array())
    val info = TorchBridge.call("create_tensor", Map(
      "shape" -> shape.toSeq,
      "dtype" -> "float32",
      "data_b64" -> b64
    )).asInstanceOf[Map[String, Any]]
    fromInfo(info)
  }

  /** Create tensor from long array. */
  def fromLongs(data: Array[Long], shape: Array[Long]): Tensor = {
    val buf = ByteBuffer.allocate(data.length * 8).order(ByteOrder.LITTLE_ENDIAN)
    data.foreach(buf.putLong)
    val b64 = Base64.getEncoder.encodeToString(buf.array())
    val info = TorchBridge.call("create_tensor", Map(
      "shape" -> shape.toSeq,
      "dtype" -> "int64",
      "data_b64" -> b64
    )).asInstanceOf[Map[String, Any]]
    fromInfo(info)
  }

  /** Create tensor from a 2D long array (for HW interface). */
  def fromLongArray2D(data: Array[Array[Long]], rows: Int, cols: Int, dtype: String = "int64"): Tensor = {
    val flat = new Array[Long](rows * cols)
    for (r <- 0 until rows; c <- 0 until cols) {
      flat(r * cols + c) = data(r)(c)
    }
    val buf = ByteBuffer.allocate(flat.length * 8).order(ByteOrder.LITTLE_ENDIAN)
    flat.foreach(buf.putLong)
    val b64 = Base64.getEncoder.encodeToString(buf.array())
    val info = TorchBridge.call("from_long_array_2d", Map(
      "data_b64" -> b64,
      "rows" -> rows,
      "cols" -> cols,
      "dtype" -> dtype
    )).asInstanceOf[Map[String, Any]]
    fromInfo(info)
  }
}

/**
 * Minimal JSON parser — no external dependency.
 * Handles well-formed JSON (objects, arrays, strings, numbers, booleans, null).
 */
private[bridge] object SimpleJson {
  def parse(s: String): Any = {
    val (result, _) = parseValue(s.trim, 0)
    result
  }

  private def parseValue(s: String, pos: Int): (Any, Int) = {
    if (pos >= s.length) throw new RuntimeException("Unexpected end of JSON")
    s.charAt(pos) match {
      case '{' => parseObject(s, pos)
      case '[' => parseArray(s, pos)
      case '"' => parseString(s, pos)
      case 't' => (true, pos + 4)   // true
      case 'f' => (false, pos + 5)  // false
      case 'n' => (null, pos + 4)   // null
      case c if c == '-' || c.isDigit => parseNumber(s, pos)
      case c => throw new RuntimeException(s"Unexpected char '$c' at pos $pos")
    }
  }

  private def parseObject(s: String, pos: Int): (Map[String, Any], Int) = {
    val m = mutable.LinkedHashMap[String, Any]()
    var p = skipWs(s, pos + 1) // skip '{'
    if (s.charAt(p) == '}') return (m.toMap, p + 1)
    while (true) {
      p = skipWs(s, p)
      val (key, p2) = parseString(s, p)
      p = skipWs(s, p2)
      if (s.charAt(p) != ':') throw new RuntimeException(s"Expected ':' at $p")
      p = skipWs(s, p + 1)
      val (value, p3) = parseValue(s, p)
      m(key.asInstanceOf[String]) = value
      p = skipWs(s, p3)
      if (s.charAt(p) == '}') return (m.toMap, p + 1)
      if (s.charAt(p) != ',') throw new RuntimeException(s"Expected ',' or '}' at $p")
      p += 1
    }
    (m.toMap, p) // unreachable
  }

  private def parseArray(s: String, pos: Int): (List[Any], Int) = {
    val arr = mutable.ListBuffer[Any]()
    var p = skipWs(s, pos + 1) // skip '['
    if (s.charAt(p) == ']') return (arr.toList, p + 1)
    while (true) {
      p = skipWs(s, p)
      val (value, p2) = parseValue(s, p)
      arr += value
      p = skipWs(s, p2)
      if (s.charAt(p) == ']') return (arr.toList, p + 1)
      if (s.charAt(p) != ',') throw new RuntimeException(s"Expected ',' or ']' at $p")
      p += 1
    }
    (arr.toList, p) // unreachable
  }

  private def parseString(s: String, pos: Int): (String, Int) = {
    if (s.charAt(pos) != '"') throw new RuntimeException(s"Expected '\"' at $pos")
    val sb = new StringBuilder
    var p = pos + 1
    while (p < s.length) {
      val c = s.charAt(p)
      if (c == '"') return (sb.toString, p + 1)
      if (c == '\\') {
        p += 1
        s.charAt(p) match {
          case '"'  => sb += '"'
          case '\\' => sb += '\\'
          case '/'  => sb += '/'
          case 'n'  => sb += '\n'
          case 'r'  => sb += '\r'
          case 't'  => sb += '\t'
          case 'b'  => sb += '\b'
          case 'f'  => sb += '\f'
          case 'u'  =>
            val hex = s.substring(p + 1, p + 5)
            sb += Integer.parseInt(hex, 16).toChar
            p += 4
          case ch => sb += ch
        }
      } else {
        sb += c
      }
      p += 1
    }
    throw new RuntimeException("Unterminated string")
  }

  private def parseNumber(s: String, pos: Int): (Any, Int) = {
    var p = pos
    if (p < s.length && s.charAt(p) == '-') p += 1
    while (p < s.length && s.charAt(p).isDigit) p += 1
    var isFloat = false
    if (p < s.length && s.charAt(p) == '.') { isFloat = true; p += 1; while (p < s.length && s.charAt(p).isDigit) p += 1 }
    if (p < s.length && (s.charAt(p) == 'e' || s.charAt(p) == 'E')) {
      isFloat = true; p += 1
      if (p < s.length && (s.charAt(p) == '+' || s.charAt(p) == '-')) p += 1
      while (p < s.length && s.charAt(p).isDigit) p += 1
    }
    val numStr = s.substring(pos, p)
    val value: Any = if (isFloat) numStr.toDouble else {
      val l = numStr.toLong
      if (l >= Int.MinValue && l <= Int.MaxValue) l.toDouble else l.toDouble
    }
    (value, p)
  }

  private def skipWs(s: String, pos: Int): Int = {
    var p = pos
    while (p < s.length && s.charAt(p).isWhitespace) p += 1
    p
  }
}

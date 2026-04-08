package runtime.engine

import org.scalatest.funsuite.AnyFunSuite
import runtime.bridge.{TorchBridge, Tensor}

/**
 * RuntimeTestSuite — ScalaTest suite for full graph validation.
 *
 * Loads the MNIST ONNX model and reference .pb data, runs inference via
 * the PyTorch bridge, then compares outputs against references.
 *
 * Run via:
 *   sbt "testOnly runtime.engine.RuntimeTestSuite"
 */
class RuntimeTestSuite extends AnyFunSuite {

  val MODEL_PATH = "sw/java/test/resources/mnist/opset_v13/" +
    "NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_OnnxRuntime_ir_version5.onnx"
  val DATA_DIR = "sw/java/test/resources/mnist/opset_v13/data_0"

  val inputMapping: Map[String, String] = Map(
    "input_seq_pc.pb" -> "seq_pc",
    "input_seq_pos.pb" -> "seq_pos"
  )
  val outputMapping: Map[String, String] = Map(
    "output_pre_tran.pb" -> "pre_trans",
    "output_rot.pb" -> "rot",
    "output_trj.pb" -> "trj"
  )

  /** Shared state: bridge + session + test data, initialized once. */
  private lazy val (graph, session, testData) = {
    val workDir = sys.props.getOrElse("user.dir", ".")
    val modelPath = s"$workDir/$MODEL_PATH"
    val dataDir = s"$workDir/$DATA_DIR"

    TorchBridge.init()

    val g = OnnxLoader.load(modelPath)
    val td = TorchBridge.call("load_test_data", Map("dir" -> dataDir))
      .asInstanceOf[Map[String, Any]]

    val sess = new Session(g, debug = false)

    // Feed inputs
    for ((file, inputName) <- inputMapping) {
      val fileInfo = td(file).asInstanceOf[Map[String, Any]]
      val tid = fileInfo("id").toString
      val info = TorchBridge.call("get_info", Map("id" -> tid)).asInstanceOf[Map[String, Any]]
      val tensor = Tensor.fromInfo(info)
      sess.feed(inputName, tensor)
    }

    // Forward
    sess.forward()

    (g, sess, td)
  }

  test("Model loads correctly: 3732 nodes, 2 inputs, 3 outputs") {
    assert(graph.nodes.size == 3732, s"Expected 3732 nodes, got ${graph.nodes.size}")
    assert(graph.userInputs.size == 2, s"Expected 2 user inputs, got ${graph.userInputs.size}")
    assert(graph.outputs.size == 3, s"Expected 3 outputs, got ${graph.outputs.size}")
  }

  test("Output pre_trans: shape=[1,32,3], mean abs error < 0.01") {
    assertOutputMatch("output_pre_tran.pb", "pre_trans", Array(1, 32, 3))
  }

  test("Output rot: shape=[1,32,144], mean abs error < 0.01") {
    assertOutputMatch("output_rot.pb", "rot", Array(1, 32, 144))
  }

  test("Output trj: shape=[1,31,72], mean abs error < 0.01") {
    assertOutputMatch("output_trj.pb", "trj", Array(1, 31, 72))
  }

  private def assertOutputMatch(pbFile: String, outputName: String, expectedShape: Array[Int]): Unit = {
    val computed = session.getTensor(outputName)
    val computedData = computed.toFloatArray()

    val fileInfo = testData(pbFile).asInstanceOf[Map[String, Any]]
    val refTid = fileInfo("id").toString
    val refInfo = TorchBridge.call("get_info", Map("id" -> refTid)).asInstanceOf[Map[String, Any]]
    val refTensor = Tensor.fromInfo(refInfo)
    val refData = refTensor.toFloatArray()

    // Check shape
    assert(computed.shape.map(_.toInt).sameElements(expectedShape),
      s"Shape mismatch: computed=[${computed.shape.mkString(",")}], expected=[${expectedShape.mkString(",")}]")

    // Compare values
    val n = math.min(computedData.length, refData.length)
    var sumAbsErr = 0.0
    var maxAbsErr = 0.0
    for (i <- 0 until n) {
      val absErr = math.abs(computedData(i).toDouble - refData(i).toDouble)
      sumAbsErr += absErr
      if (absErr > maxAbsErr) maxAbsErr = absErr
    }
    val meanAbsErr = sumAbsErr / n

    info(f"  Mean Abs Error: $meanAbsErr%.6e, Max Abs Error: $maxAbsErr%.6e")
    assert(meanAbsErr < 1e-2,
      f"Mean abs error $meanAbsErr%.6e exceeds tolerance 1e-2")
  }
}

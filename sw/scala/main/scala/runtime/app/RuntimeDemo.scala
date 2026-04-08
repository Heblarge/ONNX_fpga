package runtime.app

import runtime.bridge.{TorchBridge, Tensor}
import runtime.engine._
import runtime.dispatch.AcceleratorDispatcher

/**
 * RuntimeDemo — Pure Scala ONNX runtime using PyTorch via bridge.
 *
 * No Java library dependencies (ND4J, DL4J, OnnxProto3).
 * All tensor computation happens in Python/PyTorch.
 * HW dispatch goes through AcceleratorSimInterface (Scala).
 *
 * Usage:
 *   sbt "runMain runtime.app.RuntimeDemo <onnx_model> [--debug] [--hw]"
 */
object RuntimeDemo {

  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      println("Usage: RuntimeDemo <onnx_model_path> [--debug] [--hw]")
      return
    }

    val modelPath = args(0)
    val debug = args.contains("--debug")
    val useHW = args.contains("--hw")

    TorchBridge.init()
    try {
      println(s"=== Runtime Demo ===")
      println(s"Model: $modelPath")
      println(s"Debug: $debug, HW dispatch: $useHW")

      // Load model
      val graph = OnnxLoader.load(modelPath)

      // Create session
      val session = new Session(graph, debug)
      try {
        // Feed random inputs
        for (vi <- graph.userInputs) {
          val shape = vi.shape.map(d => if (d <= 0) 1L else d)
          val input = Tensor.rand(shape, vi.dtypeString)
          println(s"  Input '${vi.name}': shape=[${shape.mkString(",")}] dtype=${vi.dtypeString}")
          session.feed(vi.name, input)
        }

        // Forward
        val hwDispatcher = if (useHW) Some(new AcceleratorDispatcher()) else None
        println(s"\nForwarding (${graph.topologicalOrder.size} nodes)...")
        val t0 = System.currentTimeMillis()
        session.forward(hwDispatcher)
        val elapsed = System.currentTimeMillis() - t0
        println(s"Done in ${elapsed}ms")

        // Print outputs
        val outputs = session.getOutputs
        for ((name, tensor) <- outputs) {
          println(s"\nOutput '$name': shape=[${tensor.shape.mkString(",")}] dtype=${tensor.dtype}")
          val data = tensor.toFloatArray()
          val n = math.min(10, data.length)
          println(s"  First $n values: [${data.take(n).map(v => f"$v%.6f").mkString(", ")}]")
          println(s"  Min: ${data.min}, Max: ${data.max}")
        }
      } finally {
        session.close()
      }
    } finally {
      TorchBridge.shutdown()
    }
  }
}

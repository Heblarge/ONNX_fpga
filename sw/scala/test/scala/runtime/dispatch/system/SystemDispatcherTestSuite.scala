package runtime.dispatch.system

import Accelerator._
import WrapForFPGA._
import DMA._
import Interface._

import scala.math.abs
import scala.util.Random

// =============================================================================
// SystemDispatcherTestSuite — 系统级调度器独立测试
//
// 直接通过 SystemDispatcher 的 tiledHWOp / tiledElementOp 接口测试，
// 不依赖 ONNX 模型或 TorchBridge。使用 SW golden model 验证。
//
// 运行方式：
//   sbt 'runMain runtime.dispatch.system.SystemDispatcherTestSuite'
// =============================================================================
object SystemDispatcherTestSuite extends App {

  val seed   = 42
  val random = new Random(seed)

  // ====================== 硬件配置 ======================
  val sideNum      = 8
  val elementWidth = 24
  val intWidth     = 12
  val cacheAddrWidth = 15  // 32768 words/bank → 512×512 元素

  val fpgaCfg = FPGACfg(
    UIDWidth = 19,
    AddressWidth = 20,
    ShapeWidth = 16,
    systolicArraySideNum = sideNum,
    elementWidth = elementWidth,
    intWidth = intWidth,
    systolicArrayInFifoDepth = 64,
    systolicArrayOutFifoDepth = 16,
    systolicArrayInstFifoDepth = 32,
    activationOutFifoDepth = 32,
    slicedInstFifoDepth = 2,
    numCores = 1
  )

  val sysCfg = SystemWrapperConfig(
    fpgaCfg        = fpgaCfg,
    cacheAddrWidth = cacheAddrWidth,
    ddrAddrWidth   = 64,
    dmaMaxBurstLen = 256
  )

  val acceleratorCfg = AcceleratorCfg(
    UIDWidth = 19,
    AddressWidth = 20,
    ShapeWidth = 16,
    elementWidth = elementWidth,
    intWidth = intWidth,
    systolicArraySideNum = sideNum,
    systolicArrayInFifoDepth = 64,
    systolicArrayOutFifoDepth = 16,
    systolicArrayInstFifoDepth = 32,
    activationOutFifoDepth = 32,
    slicedInstFifoDepth = 2,
    numCores = 1,
    memElementWidth = 32
  )

  // ====================== 测试用例定义 ======================

  case class TestCase(
    name: String,
    opName: String,
    activation: String,
    shiftAfterOp: Int,
    shiftAfterAct: Int,
    rows: Int,     // A 的行数
    colsA: Int,    // A 的列数 = B 的行数 (matmul) 或 B 的列数 (element)
    colsB: Int,    // B 的列数
    valueRange: (Int, Int) = (-3, 4)
  )

  val testCases = Seq(
    // --- 基础矩阵乘法 ---
    TestCase("MatMul_16x16x16", "matmul", "none", 0, 0, 16, 16, 16),
    TestCase("MatMul_32x32x32", "matmul", "none", 0, 0, 32, 32, 32),
    TestCase("MatMul_64x32x64", "matmul", "none", 0, 0, 64, 32, 64),

    // --- 大矩阵（触发 tiling）---
    TestCase("MatMul_512x512x512", "matmul", "none", 0, 0, 512, 512, 512),

    // --- 带激活函数 ---
    TestCase("MatMul_32x32_Relu", "matmul", "relu", 0, 0, 32, 32, 32),

    // --- 逐元素运算 ---
    TestCase("ElemAdd_32x32", "elementadd", "none", 0, 0, 32, 32, 32),
    TestCase("ElemAdd_512x512", "elementadd", "none", 0, 0, 512, 512, 512),
    TestCase("ElemMul_32x32", "elementmul", "none", 0, 0, 32, 32, 32),
    TestCase("ElemMax_32x32", "elementmax", "none", 0, 0, 32, 32, 32),

    // --- 连续多任务 ---
    // (这些测试在同一个持久化仿真中连续执行，验证 DDR 和缓存状态正确回收)
  )

  // ====================== 验证辅助 ======================

  val errRateLimit = 0.01
  val zeroLimit    = 10

  def verifyMatrix(golden: Array[Array[Long]], hwResult: Array[Array[Long]],
                   name: String): Boolean = {
    val rows = golden.length
    val cols = golden(0).length
    var mismatch = 0
    for (i <- 0 until rows; j <- 0 until cols) {
      val g = golden(i)(j)
      val h = hwResult(i)(j)
      val err = abs(g - h)
      val errRate = abs((g - h).toDouble / (if (g != 0) g.toDouble else 1.0))
      val pass = if (errRate < errRateLimit) true
                 else abs(g) < zeroLimit && abs(h) < zeroLimit
      if (!pass) {
        if (mismatch < 5) {
          println(s"  [$name] MISMATCH ($i,$j): golden=$g hw=$h err=$err rate=$errRate")
        }
        mismatch += 1
      }
    }
    if (mismatch > 0) {
      println(s"  [$name] FAIL: $mismatch / ${rows * cols} mismatches")
      false
    } else true
  }

  def goldenModel(matA: Array[Array[Long]], matB: Array[Array[Long]],
                  opName: String, activation: String,
                  shiftAfterOp: Int, shiftAfterAct: Int,
                  m: Int, k: Int, n: Int): Array[Array[Long]] = {
    val matOp = opName.toLowerCase match {
      case "matmul"     => MatrixOperation_TypeDef.MatMul
      case "elementadd" => MatrixOperation_TypeDef.ElementAdd
      case "elementmul" => MatrixOperation_TypeDef.ElementMul
      case "elementmax" => MatrixOperation_TypeDef.ElementMax
    }
    val actFn = activation.toLowerCase match {
      case "none"     => Activation_TypeDef.None
      case "relu"     => Activation_TypeDef.Relu
      case "exp"      => Activation_TypeDef.Exp
      case "log"      => Activation_TypeDef.Log
      case "softplus" => Activation_TypeDef.Softplus
    }
    val inst = new InstSim(0, matOp, shiftAfterOp, false, actFn, shiftAfterAct,
      0, 0, 0, m, k, n, 0, 0)
    inst.computeShape()
    inst.acceleratorSim(
      matA.map(_.map(BigInt(_))),
      matB.map(_.map(BigInt(_))),
      acceleratorCfg
    ).map(_.map(_.toLong))
  }

  // ====================== 主测试流程 ======================

  println(s"${"=" * 70}")
  println(s"SystemDispatcher Standalone Test Suite")
  println(s"sideNum=$sideNum, elementWidth=$elementWidth, intWidth=$intWidth")
  println(s"cacheAddrWidth=$cacheAddrWidth (${1 << cacheAddrWidth} words/bank)")
  println(s"MAX_HW_ELEMENTS = ${(1 << cacheAddrWidth) * sideNum}")
  println(s"Total tests: ${testCases.length}")
  println(s"${"=" * 70}")

  val dispatcher = new SystemDispatcher(sysCfg)
  dispatcher._progressInterval = 1

  var totalPassed = 0
  var totalFailed = 0
  val wallStart = System.currentTimeMillis()

  def wallElapsed(): String = {
    val sec = (System.currentTimeMillis() - wallStart) / 1000
    f"${sec / 3600}%d:${(sec % 3600) / 60}%02d:${sec % 60}%02d"
  }

  try {
    dispatcher.start()

    for ((tc, idx) <- testCases.zipWithIndex) {
      println(s"\n--- [Test $idx] ${tc.name} ---")
      println(s"    op=${tc.opName} act=${tc.activation} shape=(${tc.rows},${tc.colsA},${tc.colsB})")

      val isMatMul = tc.opName.toLowerCase == "matmul"

      // 生成随机矩阵（已 padding 到 sideNum 倍数）
      val pRows = ceilToMultiple(tc.rows, sideNum)
      val pColsA = ceilToMultiple(tc.colsA, sideNum)
      val pColsB = ceilToMultiple(tc.colsB, sideNum)

      val matA = Array.ofDim[Long](pRows, pColsA)
      for (i <- 0 until tc.rows; j <- 0 until tc.colsA) {
        matA(i)(j) = random.between(tc.valueRange._1, tc.valueRange._2).toLong
      }

      val bRows = if (isMatMul) pColsA else pRows
      val bCols = pColsB
      val matB = Array.ofDim[Long](bRows, bCols)
      for (i <- 0 until (if (isMatMul) tc.colsA else tc.rows);
           j <- 0 until tc.colsB) {
        matB(i)(j) = random.between(tc.valueRange._1, tc.valueRange._2).toLong
      }

      // Golden model（仅使用实际维度区域，但传入 padded 数组）
      println(s"    Computing golden model...")
      val goldenStart = System.currentTimeMillis()
      val golden = goldenModel(matA, matB, tc.opName, tc.activation,
        tc.shiftAfterOp, tc.shiftAfterAct, pRows, pColsA, pColsB)
      val goldenMs = System.currentTimeMillis() - goldenStart
      println(s"    Golden done (${goldenMs}ms)")

      // 系统级仿真
      println(s"    Running system-level simulation...")
      val hwStart = System.currentTimeMillis()
      val hwResult = if (isMatMul) {
        dispatcher.tiledHWOp(matA, matB, pRows, pColsA, pColsB,
          tc.opName, tc.shiftAfterOp, tc.activation, tc.shiftAfterAct, tc.name)
      } else {
        dispatcher.tiledElementOp(matA, matB, pRows, pColsA,
          tc.opName, tc.shiftAfterOp, tc.activation, tc.shiftAfterAct, tc.name)
      }
      val hwMs = System.currentTimeMillis() - hwStart
      println(s"    System sim done (${hwMs}ms) [wall: ${wallElapsed()}]")

      // 验证
      val pass = verifyMatrix(golden, hwResult, tc.name)
      if (pass) {
        totalPassed += 1
        println(s"    [Test $idx] ${tc.name}: PASS")
      } else {
        totalFailed += 1
        println(s"    [Test $idx] ${tc.name}: FAIL")
      }
    }

    // ====================== 连续多任务边界测试 ======================
    println(s"\n${"=" * 70}")
    println(s"Consecutive Multi-Task Test (3 tasks)")
    println(s"${"=" * 70}")

    val consecutiveTasks = Seq(
      TestCase("Seq_MatMul_64", "matmul", "none", 0, 0, 64, 64, 64),
      TestCase("Seq_ElemAdd_64", "elementadd", "none", 0, 0, 64, 64, 64),
      TestCase("Seq_MatMul_32", "matmul", "relu", 0, 0, 32, 32, 32),
    )

    for ((tc, idx) <- consecutiveTasks.zipWithIndex) {
      println(s"\n    [ConsecTask $idx] ${tc.name}")
      val isMatMul = tc.opName.toLowerCase == "matmul"
      val pRows = ceilToMultiple(tc.rows, sideNum)
      val pColsA = ceilToMultiple(tc.colsA, sideNum)
      val pColsB = ceilToMultiple(tc.colsB, sideNum)

      val matA = Array.tabulate(pRows, pColsA)((i, j) =>
        if (i < tc.rows && j < tc.colsA) random.between(-3, 4).toLong else 0L)
      val bRows = if (isMatMul) pColsA else pRows
      val matB = Array.tabulate(bRows, pColsB)((i, j) => {
        val rBound = if (isMatMul) tc.colsA else tc.rows
        if (i < rBound && j < tc.colsB) random.between(-3, 4).toLong else 0L
      })

      val golden = goldenModel(matA, matB, tc.opName, tc.activation,
        tc.shiftAfterOp, tc.shiftAfterAct, pRows, pColsA, pColsB)

      val hwResult = if (isMatMul) {
        dispatcher.tiledHWOp(matA, matB, pRows, pColsA, pColsB,
          tc.opName, tc.shiftAfterOp, tc.activation, tc.shiftAfterAct, tc.name)
      } else {
        dispatcher.tiledElementOp(matA, matB, pRows, pColsA,
          tc.opName, tc.shiftAfterOp, tc.activation, tc.shiftAfterAct, tc.name)
      }

      val pass = verifyMatrix(golden, hwResult, tc.name)
      if (pass) { totalPassed += 1; println(s"    → PASS") }
      else      { totalFailed += 1; println(s"    → FAIL") }
    }

  } finally {
    dispatcher.shutdown()
  }

  // ====================== 结果汇总 ======================
  val totalTests = testCases.length + 3
  println(s"\n${"=" * 70}")
  println(s"RESULTS: $totalPassed passed, $totalFailed failed (of $totalTests)")
  println(s"Wall time: ${wallElapsed()}")
  println(s"${"=" * 70}")

  if (totalFailed > 0) {
    System.exit(1)
  } else {
    println("ALL TESTS PASSED!")
    System.exit(0)
  }

  // ====================== 工具函数 ======================

  def ceilToMultiple(v: Int, m: Int): Int = {
    val rem = v % m; if (rem == 0) v else v + (m - rem)
  }
}

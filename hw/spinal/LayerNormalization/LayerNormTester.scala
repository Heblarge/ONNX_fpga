package LayerNormalization

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import FloatingPoint._
import FloatingPoint.AttentionOps._
import spinal.sim.VCSFlags
import scala.math._

object LayerNormTester extends App {
  val opts = LayerNormOpts(c = FpxxConfig.float32(), dim = 100, epsilon = 1e-5)
  val rng = new scala.util.Random(2026)

  // 辅助函数：Float转换与判定
  def toFloat(f: Fpxx): Float = {
    val bits = ((f.sign.toBigInt << 31) | (f.exp.toBigInt << 23) | f.mant.toBigInt).toInt
    java.lang.Float.intBitsToFloat(bits)
  }


  def almostEqual(hw: Float, ref: Double): Boolean = {
    val diff = java.lang.Math.abs(hw.toDouble - ref)
    // 允许 10% 的相对误差或 0.1 的绝对误差
    diff < 0.01 || diff < java.lang.Math.abs(ref) * 0.01
  }

  // 将函数改为这种写法，显式调用 .toLong (通常浮点数 Bits 用 toLong 更稳)
  def getFloatFromPort(f: Fpxx): Float = {
    // 使用 .toLong 或 .toBigInt 都可以，前提是导入了 spinal.core.sim._
    val rawBits: Long = f.sign.toBigInt.toLong << 31 |
      f.exp.toBigInt.toLong << 23 |
      f.mant.toBigInt.toLong
    java.lang.Float.intBitsToFloat(rawBits.toInt)
  }

  val simConfig = SimConfig.withVCS(VCSFlags(compileFlags = List("-kdb", "-lca"))).withFsdbWave

  simConfig.compile(new LayerNorm(opts)).doSim { dut =>
    dut.clockDomain.forkStimulus(10)

    // 1. 生成 100 组随机测试向量

    val testCases = Array.fill(100)(Array.fill(opts.dim)(rng.nextFloat() * 200.0f - 100.0f))
    val gammaArr = Array.fill(opts.dim)(1.0f + rng.nextFloat())
    val betaArr  = Array.fill(opts.dim)(rng.nextFloat() - 0.5f)

    // 初始化参数
    for (i <- 0 until opts.dim) {
      dut.io.gamma(i) #= fpxxHostFromDouble(gammaArr(i).toDouble, opts.c)
      dut.io.beta(i)  #= fpxxHostFromDouble(betaArr(i).toDouble, opts.c)
    }

    var sendIdx = 0
    var recvIdx = 0

    // 2. 异步监控器：输出详细的对比结果
    FlowMonitor(dut.io.output, dut.clockDomain) { (payload: Vec[Fpxx]) =>
      val inputs: Array[Float] = testCases(recvIdx)
      val mean: Double = inputs.sum.toDouble / opts.dim

      val variance: Double = inputs.map { x =>
        val diff = x.toDouble - mean
        diff * diff
      }.sum / opts.dim

      val invStd: Double = 1.0 / java.lang.Math.sqrt(variance + opts.epsilon)

      println(s"\n" + "="*60)
      println(s"检查案例 [$recvIdx]")
      println(f"${"Index"}%-10s | ${"Input"}%-12s | ${"HW Out"}%-12s | ${"REF Out"}%-12s | ${"Diff"}%-12s")
      println("-" * 60)

      for (i <- 0 until opts.dim) {
        val hw = getFloatFromPort(payload(i))
        val ref = (inputs(i).toDouble - mean) * invStd * gammaArr(i).toDouble + betaArr(i).toDouble
        val diff = Math.abs(hw - ref)

        // 打印每一维度的详细对比
        println(f"$i%-10d | ${inputs(i)}%-12.6f | $hw%-12.6f | $ref%-12.6f | $diff%-12.6f")

        assert(almostEqual(hw, ref), s"数据偏差过大 @Case $recvIdx, Dim $i")
      }
      println("="*60)
      recvIdx += 1
    }

    // 3. 连续驱动：模拟流水线满载
    while (recvIdx < testCases.length) {
      if (sendIdx < testCases.length) {
        dut.io.input.valid #= true
        for (i <- 0 until opts.dim) {
          dut.io.input.payload(i) #= fpxxHostFromDouble(testCases(sendIdx)(i).toDouble, opts.c)
        }
        sendIdx += 1
      } else {
        dut.io.input.valid #= false
      }
      dut.clockDomain.waitSampling()
    }

    println(s"测试圆满完成！共校验 ${testCases.length} 组向量。")
    simSuccess()
  }
}
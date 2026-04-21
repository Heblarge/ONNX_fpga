package FloatingPoint

import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import java.io.File

object FpxxOnlineSoftmaxTester extends App {
    case class SoftmaxCase(scores: Seq[Float], prevMax: Float, prevSum: Float, init: Boolean)

    val tileSize = 4
    val fpCfg = FpxxConfig.float32()
    val fxCfg = AttentionExpConfig()
    val fileDir = "rtl/FpxxOnlineSoftmaxTester"
    new File(fileDir).mkdirs()

    def toFloat(sign: BigInt, exp: BigInt, mant: BigInt): Float = {
        val bits = ((sign << 31) | (exp << 23) | mant).toInt
        java.lang.Float.intBitsToFloat(bits)
    }

    def quantizedExp(x: Double): Double = {
        val clipped = math.min(0.0, math.max(fxCfg.expLutMin, x))
        val idx = math.floor((-clipped) * fxCfg.expLutStepsPerUnit.toDouble + 1e-9).toInt
        math.exp(-idx.toDouble / fxCfg.expLutStepsPerUnit.toDouble)
    }

    def almostEqual(a: Double, b: Double, tol: Double = 8e-2): Boolean = {
        if (a.isNaN && b.isNaN) true
        else if (a.isInfinite || b.isInfinite) a == b
        else math.abs(a - b) <= tol.max(math.abs(b) * 0.1)
    }

    def golden(tc: SoftmaxCase): (Seq[Double], Seq[Double], Double, Double, Double) = {
        val blockMax = tc.scores.max.toDouble
        val newMax = if (tc.init) blockMax else math.max(blockMax, tc.prevMax.toDouble)
        val expScores = tc.scores.map(s => quantizedExp(s.toDouble - newMax))
        val prevScale = if (tc.init) 0.0 else quantizedExp(tc.prevMax.toDouble - newMax)
        val newSum = (if (tc.init) 0.0 else tc.prevSum.toDouble * prevScale) + expScores.sum
        val normScores = expScores.map(v => if (newSum == 0.0) 0.0 else v / newSum)
        (expScores, normScores, newMax, prevScale, newSum)
    }

    val cases = Seq(
      SoftmaxCase(Seq(1.0f, 0.0f, -1.0f, 0.5f), 0.0f, 0.0f, init = true),
      SoftmaxCase(Seq(-0.5f, -1.0f, -0.25f, -2.0f), 0.25f, 1.7f, init = false),
      SoftmaxCase(Seq(3.0f, 2.5f, 2.0f, 1.5f), 2.75f, 0.8f, init = false),
      SoftmaxCase(Seq(-3.0f, -3.5f, -2.75f, -4.0f), -2.5f, 2.2f, init = false)
    )

    val flag = VCSFlags(
      compileFlags = List("-kdb", "-lca", "+notimingchecks"),
      elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
      runFlags = List("-l ./run.log")
    )

    val spinalCfg = SpinalConfig(
      targetDirectory = fileDir,
      oneFilePerComponent = true,
      bitVectorWidthMax = 20000
    )

    val compiled = SimConfig
      .withVCS(flag)
      .withVcdWave
      .withTimeScale(1 ns)
      .withTimePrecision(1 ns)
      .withConfig(spinalCfg)
      .allOptimisation
      .compile(new FpxxOnlineSoftmax(tileSize, fpCfg, fxCfg))

    compiled.doSim { dut =>
        SimTimeout(100000)
        dut.clockDomain.forkStimulus(10)
        dut.io.input.valid #= false
        dut.clockDomain.waitSampling(5)

        for ((tc, caseId) <- cases.zipWithIndex) {
            val (expScoresRef, normScoresRef, newMaxRef, prevScaleRef, newSumRef) = golden(tc)

            dut.io.input.valid #= true
            for (i <- 0 until tileSize) {
                dut.io.input.payload.scores(i) #= FpxxHost(tc.scores(i))
            }
            dut.io.input.payload.prevMax #= FpxxHost(tc.prevMax)
            dut.io.input.payload.prevSum #= FpxxHost(tc.prevSum)
            dut.io.input.payload.init #= tc.init
            dut.clockDomain.waitSampling()
            dut.io.input.valid #= false

            var timeout = 0
            while (!dut.io.output.valid.toBoolean && timeout < 200) {
                dut.clockDomain.waitSampling()
                timeout += 1
            }
            assert(dut.io.output.valid.toBoolean, s"softmax case $caseId timed out")

            val hwNewMax = toFloat(
              dut.io.output.payload.newMax.sign.toBigInt,
              dut.io.output.payload.newMax.exp.toBigInt,
              dut.io.output.payload.newMax.mant.toBigInt
            ).toDouble
            val hwPrevScale = toFloat(
              dut.io.output.payload.prevScale.sign.toBigInt,
              dut.io.output.payload.prevScale.exp.toBigInt,
              dut.io.output.payload.prevScale.mant.toBigInt
            ).toDouble
            val hwNewSum = toFloat(
              dut.io.output.payload.newSum.sign.toBigInt,
              dut.io.output.payload.newSum.exp.toBigInt,
              dut.io.output.payload.newSum.mant.toBigInt
            ).toDouble
            val hwExpScores = (0 until tileSize).map { i =>
                toFloat(
                  dut.io.output.payload.expScores(i).sign.toBigInt,
                  dut.io.output.payload.expScores(i).exp.toBigInt,
                  dut.io.output.payload.expScores(i).mant.toBigInt
                ).toDouble
            }
            val hwNormScores = (0 until tileSize).map { i =>
                toFloat(
                  dut.io.output.payload.normScores(i).sign.toBigInt,
                  dut.io.output.payload.normScores(i).exp.toBigInt,
                  dut.io.output.payload.normScores(i).mant.toBigInt
                ).toDouble
            }

            assert(almostEqual(hwNewMax, newMaxRef), s"case $caseId newMax mismatch: hw=$hwNewMax ref=$newMaxRef")
            assert(almostEqual(hwPrevScale, prevScaleRef), s"case $caseId prevScale mismatch: hw=$hwPrevScale ref=$prevScaleRef")
            assert(almostEqual(hwNewSum, newSumRef), s"case $caseId newSum mismatch: hw=$hwNewSum ref=$newSumRef")
            hwExpScores.zip(expScoresRef).zipWithIndex.foreach { case ((hw, ref), i) =>
                assert(almostEqual(hw, ref), s"case $caseId exp[$i] mismatch: hw=$hw ref=$ref")
            }
            hwNormScores.zip(normScoresRef).zipWithIndex.foreach { case ((hw, ref), i) =>
                assert(almostEqual(hw, ref), s"case $caseId norm[$i] mismatch: hw=$hw ref=$ref")
            }
        }

        simSuccess()
    }
}

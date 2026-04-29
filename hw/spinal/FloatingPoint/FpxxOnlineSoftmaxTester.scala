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

    val seed = 20260429
    val rng = new scala.util.Random(seed)
    def randIn(min: Float, max: Float): Float = min + rng.nextFloat() * (max - min)

    val cornerCases = Seq(
      SoftmaxCase(Seq(1.0f, 0.0f, -1.0f, 0.5f), 0.0f, 0.0f, init = true),
      SoftmaxCase(Seq(-0.5f, -1.0f, -0.25f, -2.0f), 0.25f, 1.7f, init = false),
      SoftmaxCase(Seq(3.0f, 2.5f, 2.0f, 1.5f), 2.75f, 0.8f, init = false),
      SoftmaxCase(Seq(-3.0f, -3.5f, -2.75f, -4.0f), -2.5f, 2.2f, init = false)
    )
    val randomCases = (0 until 96).map { _ =>
        val init = rng.nextInt(4) == 0
        SoftmaxCase(
          scores = Seq.fill(tileSize)(randIn(-12.0f, 12.0f)),
          prevMax = randIn(-8.0f, 8.0f),
          prevSum = randIn(0.01f, 8.0f),
          init = init
        )
    }
    val cases = cornerCases ++ randomCases

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
        SimTimeout(2000000)
        dut.clockDomain.forkStimulus(10)
        dut.io.input.valid #= false
        dut.io.output.ready #= false
        dut.clockDomain.waitSampling(5)

        val refs = cases.map(golden)
        var sendIdx = 0
        var recvIdx = 0
        var cycles = 0
        val maxCycles = 80000

        var holdReadyVal = false
        var holdReadyCycles = 0
        def nextReady(): Boolean = {
            if (holdReadyCycles == 0) {
                if (rng.nextInt(100) < 60) {
                    holdReadyVal = false
                    holdReadyCycles = 8 + rng.nextInt(33) // long backpressure
                } else {
                    holdReadyVal = true
                    holdReadyCycles = 1 + rng.nextInt(6) // short drain burst
                }
            }
            holdReadyCycles -= 1
            holdReadyVal
        }

        while ((sendIdx < cases.length || recvIdx < cases.length) && cycles < maxCycles) {
            val outReady = nextReady()
            dut.io.output.ready #= outReady

            if (sendIdx < cases.length) {
                dut.io.input.valid #= true
                val tc = cases(sendIdx)
                for (i <- 0 until tileSize) {
                    dut.io.input.payload.scores(i) #= FpxxHost(tc.scores(i))
                }
                dut.io.input.payload.prevMax #= FpxxHost(tc.prevMax)
                dut.io.input.payload.prevSum #= FpxxHost(tc.prevSum)
                dut.io.input.payload.init #= tc.init
            } else {
                dut.io.input.valid #= false
            }

            val inFire = dut.io.input.valid.toBoolean && dut.io.input.ready.toBoolean
            val outFire = dut.io.output.valid.toBoolean && dut.io.output.ready.toBoolean

            if (outFire) {
                val (expScoresRef, normScoresRef, newMaxRef, prevScaleRef, newSumRef) = refs(recvIdx)
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

                assert(almostEqual(hwNewMax, newMaxRef), s"case $recvIdx newMax mismatch: hw=$hwNewMax ref=$newMaxRef")
                assert(almostEqual(hwPrevScale, prevScaleRef), s"case $recvIdx prevScale mismatch: hw=$hwPrevScale ref=$prevScaleRef")
                assert(almostEqual(hwNewSum, newSumRef), s"case $recvIdx newSum mismatch: hw=$hwNewSum ref=$newSumRef")
                hwExpScores.zip(expScoresRef).zipWithIndex.foreach { case ((hw, ref), i) =>
                    assert(almostEqual(hw, ref), s"case $recvIdx exp[$i] mismatch: hw=$hw ref=$ref")
                }
                hwNormScores.zip(normScoresRef).zipWithIndex.foreach { case ((hw, ref), i) =>
                    assert(almostEqual(hw, ref), s"case $recvIdx norm[$i] mismatch: hw=$hw ref=$ref")
                }
                recvIdx += 1
            }

            dut.clockDomain.waitSampling()
            if (inFire) sendIdx += 1
            cycles += 1
        }
        assert(sendIdx == cases.length, s"not all inputs were accepted: $sendIdx/${cases.length}")
        assert(recvIdx == cases.length, s"not all outputs were drained: $recvIdx/${cases.length}")

        simSuccess()
    }
}

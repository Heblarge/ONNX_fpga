package FloatingPoint

import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import java.io.File

object FpxxQKVTester extends App {
    case class QKVCase(
        q: Seq[Float],
        k: Seq[Seq[Float]],
        v: Seq[Seq[Float]],
        prevMax: Float,
        prevSum: Float,
        prevAcc: Seq[Float],
        init: Boolean
    )

    val tileSize = 2
    val headDim = 4
    val fpCfg = FpxxConfig.float32()
    val fxCfg = AttentionExpConfig()
    val fileDir = "rtl/FpxxQKVTester"
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

    def almostEqual(a: Double, b: Double, tol: Double = 1.2e-1): Boolean = {
        if (a.isNaN && b.isNaN) true
        else math.abs(a - b) <= tol.max(math.abs(b) * 0.15)
    }

    def golden(tc: QKVCase): (Seq[Double], Seq[Double], Seq[Double], Double, Double, Seq[Double], Seq[Double]) = {
        val scores = tc.k.map(row => row.zip(tc.q).map { case (a, b) => a * b }.sum.toDouble)
        val blockMax = scores.max
        val newMax = if (tc.init) blockMax else math.max(blockMax, tc.prevMax.toDouble)
        val expScores = scores.map(s => quantizedExp(s - newMax))
        val prevScale = if (tc.init) 0.0 else quantizedExp(tc.prevMax.toDouble - newMax)
        val newSum = (if (tc.init) 0.0 else tc.prevSum.toDouble * prevScale) + expScores.sum
        val normScores = expScores.map(v => if (newSum == 0.0) 0.0 else v / newSum)
        val newAcc = (0 until headDim).map { d =>
            val prevTerm = if (tc.init) 0.0 else tc.prevAcc(d).toDouble * prevScale
            prevTerm + tc.v.indices.map(i => expScores(i) * tc.v(i)(d)).sum
        }
        val newAccNorm = newAcc.map(v => if (newSum == 0.0) 0.0 else v / newSum)
        (scores, expScores, normScores, newMax, newSum, newAcc, newAccNorm)
    }

    val cases = Seq(
      QKVCase(
        q = Seq(0.25f, -0.5f, 0.75f, 1.0f),
        k = Seq(
          Seq(0.5f, 0.25f, -0.5f, 1.0f),
          Seq(-0.25f, 1.0f, 0.5f, -0.5f)
        ),
        v = Seq(
          Seq(0.5f, 0.25f, 1.0f, -0.25f),
          Seq(1.0f, -0.5f, 0.25f, 0.75f)
        ),
        prevMax = 0.0f,
        prevSum = 0.0f,
        prevAcc = Seq.fill(headDim)(0.0f),
        init = true
      ),
      QKVCase(
        q = Seq(1.0f, -0.25f, 0.5f, -0.75f),
        k = Seq(
          Seq(0.25f, -0.5f, 0.75f, 0.5f),
          Seq(-0.75f, 0.5f, 0.25f, -0.25f)
        ),
        v = Seq(
          Seq(0.25f, 0.75f, -0.5f, 0.5f),
          Seq(-0.25f, 0.5f, 1.0f, -1.0f)
        ),
        prevMax = 0.2f,
        prevSum = 1.3f,
        prevAcc = Seq(0.6f, -0.2f, 0.4f, 0.1f),
        init = false
      )
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
      .compile(new FpxxQKV(tileSize, headDim, fpCfg, fxCfg))

    compiled.doSim { dut =>
        SimTimeout(200000)
        dut.clockDomain.forkStimulus(10)
        dut.io.input.valid #= false
        dut.clockDomain.waitSampling(5)

        for ((tc, caseId) <- cases.zipWithIndex) {
            val (scoreRef, expRef, normRef, maxRef, sumRef, accRef, accNormRef) = golden(tc)

            dut.io.input.valid #= true
            for (d <- 0 until headDim) {
                dut.io.input.payload.q(d) #= FpxxHost(tc.q(d))
                dut.io.input.payload.prevAcc(d) #= FpxxHost(tc.prevAcc(d))
            }
            for (i <- 0 until tileSize; d <- 0 until headDim) {
                dut.io.input.payload.k(i)(d) #= FpxxHost(tc.k(i)(d))
                dut.io.input.payload.v(i)(d) #= FpxxHost(tc.v(i)(d))
            }
            dut.io.input.payload.prevMax #= FpxxHost(tc.prevMax)
            dut.io.input.payload.prevSum #= FpxxHost(tc.prevSum)
            dut.io.input.payload.init #= tc.init
            dut.clockDomain.waitSampling()
            dut.io.input.valid #= false

            var timeout = 0
            while (!dut.io.output.valid.toBoolean && timeout < 400) {
                dut.clockDomain.waitSampling()
                timeout += 1
            }
            assert(dut.io.output.valid.toBoolean, s"QKV case $caseId timed out")

            val hwScores = (0 until tileSize).map { i =>
                toFloat(
                  dut.io.output.payload.scores(i).sign.toBigInt,
                  dut.io.output.payload.scores(i).exp.toBigInt,
                  dut.io.output.payload.scores(i).mant.toBigInt
                ).toDouble
            }
            val hwExp = (0 until tileSize).map { i =>
                toFloat(
                  dut.io.output.payload.expScores(i).sign.toBigInt,
                  dut.io.output.payload.expScores(i).exp.toBigInt,
                  dut.io.output.payload.expScores(i).mant.toBigInt
                ).toDouble
            }
            val hwNorm = (0 until tileSize).map { i =>
                toFloat(
                  dut.io.output.payload.normScores(i).sign.toBigInt,
                  dut.io.output.payload.normScores(i).exp.toBigInt,
                  dut.io.output.payload.normScores(i).mant.toBigInt
                ).toDouble
            }
            val hwMax = toFloat(
              dut.io.output.payload.newMax.sign.toBigInt,
              dut.io.output.payload.newMax.exp.toBigInt,
              dut.io.output.payload.newMax.mant.toBigInt
            ).toDouble
            val hwSum = toFloat(
              dut.io.output.payload.newSum.sign.toBigInt,
              dut.io.output.payload.newSum.exp.toBigInt,
              dut.io.output.payload.newSum.mant.toBigInt
            ).toDouble
            val hwAcc = (0 until headDim).map { d =>
                toFloat(
                  dut.io.output.payload.newAcc(d).sign.toBigInt,
                  dut.io.output.payload.newAcc(d).exp.toBigInt,
                  dut.io.output.payload.newAcc(d).mant.toBigInt
                ).toDouble
            }
            val hwAccNorm = (0 until headDim).map { d =>
                toFloat(
                  dut.io.output.payload.newAccNorm(d).sign.toBigInt,
                  dut.io.output.payload.newAccNorm(d).exp.toBigInt,
                  dut.io.output.payload.newAccNorm(d).mant.toBigInt
                ).toDouble
            }

            hwScores.zip(scoreRef).zipWithIndex.foreach { case ((hw, ref), i) =>
                assert(almostEqual(hw, ref), s"case $caseId score[$i] mismatch: hw=$hw ref=$ref")
            }
            hwExp.zip(expRef).zipWithIndex.foreach { case ((hw, ref), i) =>
                assert(almostEqual(hw, ref), s"case $caseId exp[$i] mismatch: hw=$hw ref=$ref")
            }
            hwNorm.zip(normRef).zipWithIndex.foreach { case ((hw, ref), i) =>
                assert(almostEqual(hw, ref), s"case $caseId norm[$i] mismatch: hw=$hw ref=$ref")
            }
            assert(almostEqual(hwMax, maxRef), s"case $caseId newMax mismatch: hw=$hwMax ref=$maxRef")
            assert(almostEqual(hwSum, sumRef), s"case $caseId newSum mismatch: hw=$hwSum ref=$sumRef")
            hwAcc.zip(accRef).zipWithIndex.foreach { case ((hw, ref), d) =>
                assert(almostEqual(hw, ref), s"case $caseId acc[$d] mismatch: hw=$hw ref=$ref")
            }
            hwAccNorm.zip(accNormRef).zipWithIndex.foreach { case ((hw, ref), d) =>
                assert(almostEqual(hw, ref), s"case $caseId accNorm[$d] mismatch: hw=$hw ref=$ref")
            }
        }

        simSuccess()
    }
}

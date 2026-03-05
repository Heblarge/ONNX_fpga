package FloatingPoint

import Interface.MatrixOperation_TypeDef
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{StreamDriver, StreamMonitor, StreamReadyRandomizer}

import java.lang.Float.floatToRawIntBits
import scala.collection.mutable
import scala.util.Random

object SquareSystolicArraySim extends App {
  val matrixNum = 80
  val size = 4
  val cfg = SquareSystolicArray_Config(
    in_Length_Max = size,
    in_Length_Min = size,
    in_MatA_row_num = size,
    in_MatB_col_num = size,
    fpConfig = FpxxConfig.float16(),
    accIntBits = 16 bits,
    accFracBits = 16 bits,
    mulStages = 1,
    f2iStages = 1,
    Enable_Transpose_logic = true,
    Enable_ElementWise_logic = true
  )

  def floatToFpComponents(v: Float, fpCfg: FpxxConfig): (Boolean, Int, Int) = {
    if (v == 0.0f) {
      (false, 0, 0)
    } else {
      val bits = floatToRawIntBits(v)
      val sign = ((bits >>> 31) & 0x1) != 0
      val exp = (bits >>> 23) & 0xFF
      val mant = bits & 0x7FFFFF

      val rebias = (exp - 127 + fpCfg.bias).max(0).min((1 << fpCfg.exp_size) - 1)
      val newMant = mant >> (23 - fpCfg.mant_size)
      (sign, rebias, newMant)
    }
  }

  def matMul(a: Array[Array[Double]], b: Array[Array[Double]]): Array[Array[Double]] = {
    val n = a.length
    val z = Array.ofDim[Double](n, n)
    for (r <- 0 until n; c <- 0 until n) {
      z(r)(c) = (0 until n).map(k => a(r)(k) * b(k)(c)).sum
    }
    z
  }

  def transpose(m: Array[Array[Double]]): Array[Array[Double]] = {
    val n = m.length
    val z = Array.ofDim[Double](n, n)
    for (r <- 0 until n; c <- 0 until n) {
      z(c)(r) = m(r)(c)
    }
    z
  }

  val rand = new Random(42)
  val inAQueue = mutable.Queue[Array[Array[Float]]]()
  val inBQueue = mutable.Queue[Array[Array[Float]]]()
  val transposeQueue = mutable.Queue[Boolean]()
  val refQueue = mutable.Queue[Array[Array[Double]]]()

  for (_ <- 0 until matrixNum) {
    val a = Array.fill(size, size)((rand.nextFloat() - 0.5f) * 2.0f)
    val b = Array.fill(size, size)((rand.nextFloat() - 0.5f) * 2.0f)
    val doTranspose = rand.nextBoolean()

    val ref = matMul(a.map(_.map(_.toDouble)), b.map(_.map(_.toDouble)))
    inAQueue.enqueue(a)
    inBQueue.enqueue(b)
    transposeQueue.enqueue(doTranspose)
    refQueue.enqueue(if (doTranspose) transpose(ref) else ref)
  }

  SimConfig.withWave.compile(SquareSystolicArray(cfg)).doSim("square_systolic_stream") { dut =>
    SimTimeout(300000)
    dut.clockDomain.forkStimulus(10)

    var sendingK = 0
    var sendingA = inAQueue.dequeue()
    var sendingB = inBQueue.dequeue()
    var sendingTranspose = transposeQueue.dequeue()
    var sentCases = 0

    StreamDriver(dut.io.in_Mats, dut.clockDomain) { payload =>
      if (sentCases >= matrixNum) {
        false
      } else {
        payload.OpMode.post_Shift #= 0
        payload.OpMode.MatrixOperation #= MatrixOperation_TypeDef.MatMul
        payload.OpMode.do_PostTranspose #= sendingTranspose

        for (r <- 0 until size) {
          val (s, e, m) = floatToFpComponents(sendingA(r)(sendingK), cfg.fpConfig)
          payload.A(r).data.sign #= s
          payload.A(r).data.exp #= e
          payload.A(r).data.mant #= m
          payload.A(r).Final #= (sendingK == size - 1)
        }

        for (c <- 0 until size) {
          val (s, e, m) = floatToFpComponents(sendingB(sendingK)(c), cfg.fpConfig)
          payload.B(c).data.sign #= s
          payload.B(c).data.exp #= e
          payload.B(c).data.mant #= m
          payload.B(c).Final #= (sendingK == size - 1)
        }

        if (sendingK == size - 1) {
          sendingK = 0
          sentCases += 1
          if (sentCases < matrixNum) {
            sendingA = inAQueue.dequeue()
            sendingB = inBQueue.dequeue()
            sendingTranspose = transposeQueue.dequeue()
          }
        } else {
          sendingK += 1
        }
        true
      }
    }

    StreamReadyRandomizer(dut.io.out_Mats, dut.clockDomain)
    dut.io.out_Mats.ready #= true

    var checked = 0
    var maxAbsErr = 0.0
    val tolerance = 2.5

    StreamMonitor(dut.io.out_Mats, dut.clockDomain) { payload =>
      val ref = refQueue.dequeue()
      for (r <- 0 until size; c <- 0 until size) {
        val hw = payload.Z(r)(c).toDouble
        val sw = ref(r)(c)
        val err = math.abs(hw - sw)
        if (err > maxAbsErr) maxAbsErr = err
        assert(err <= tolerance, f"Mismatch at ($r,$c): hw=$hw%.4f sw=$sw%.4f err=$err%.4f")
      }
      checked += 1
    }

    dut.clockDomain.waitSamplingWhere(checked == matrixNum)
    println(f"TEST PASS, checked=$checked, maxAbsErr=$maxAbsErr%.4f")
    simSuccess()
  }
}

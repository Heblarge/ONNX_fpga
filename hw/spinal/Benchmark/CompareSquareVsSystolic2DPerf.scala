package Benchmark

import FloatingPoint._
import Interface.MatrixOperation_TypeDef
import MatrixComputeUnit.SystolicArray2D._
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{StreamDriver, StreamMonitor}
import spinal.sim.VCSFlags

import scala.util.Random

case class PerfResult(name: String, totalCycles: Long, totalOps: Long) {
  def opsPerCycle: Double = if (totalCycles == 0) 0.0 else totalOps.toDouble / totalCycles.toDouble
}

object CompareSquareVsSystolic2DPerf extends App {
  val size = if (args.length > 0) args(0).toInt else 4
  val caseNum = if (args.length > 1) args(1).toInt else 30
  val seed = if (args.length > 2) args(2).toInt else 20260306
  val periodNs = if (args.length > 3) args(3).toInt else 10

  require(size > 0, "size must be > 0")
  require(caseNum > 0, "caseNum must be > 0")

  val random = new Random(seed)
  val matricesA = Array.fill(caseNum, size, size)((random.nextFloat() - 0.5f) * 2.0f)
  val matricesB = Array.fill(caseNum, size, size)((random.nextFloat() - 0.5f) * 2.0f)

  val vcsFlag = VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )

  val baseCfg = SpinalConfig(
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
    bitVectorWidthMax = 20000
  )

  val fpCfg = SquareSystolicArray_Config(
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

  val fxCfg = SystolicArray2D_Config(
    in_Length_Max = size,
    in_Length_Min = size,
    in_MatA_row_num = size,
    in_MatB_col_num = size,
    in_MatA_element_Width = 16,
    in_MatB_element_Width = 16,
    out_MatZ_element_Width = 32,
    Enable_Transpose_logic = true,
    Enable_ElementWise_logic = true
  )

  val fpCompiled = SimConfig
    .workspacePath(s"simWorkspace/PerfCompare_Square_fp16_${size}x$size")
    .withVCS(vcsFlag)
    .withConfig(baseCfg.copy(targetDirectory = s"rtl/PerfCompare/Square_fp16_${size}x$size"))
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .allOptimisation
    .compile(new SquareSystolicArray(fpCfg))

  val fxCompiled = SimConfig
    .workspacePath(s"simWorkspace/PerfCompare_Systolic2D_fix_${size}x$size")
    .withVCS(vcsFlag)
    .withConfig(baseCfg.copy(targetDirectory = s"rtl/PerfCompare/Systolic2D_fix_${size}x$size"))
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .allOptimisation
    .compile(new SystolicArray2D(fxCfg))

  def saturateSigned(v: Int, bits: Int): Int = {
    val minVal = -(1 << (bits - 1))
    val maxVal = (1 << (bits - 1)) - 1
    v.max(minVal).min(maxVal)
  }

  def floatToFix(v: Float, fracBits: Int, bits: Int): Int = {
    val scaled = math.round(v * (1 << fracBits)).toInt
    saturateSigned(scaled, bits)
  }

  def unpackFloat(host: FpxxHost): (Boolean, Int, Int) = {
    val bits = host.value.toLong
    val sign = ((bits >> (host.c.exp_size + host.c.mant_size)) & 0x1) != 0
    val exp = ((bits >> host.c.mant_size) & ((1 << host.c.exp_size) - 1)).toInt
    val mant = (bits & ((1 << host.c.mant_size) - 1)).toInt
    (sign, exp, mant)
  }

  def runSquareFpPerf(): PerfResult = {
    val algo = new FloatAlgo(fpCfg.fpConfig)
    val totalOps = 2L * size * size * size * caseNum
    var totalCycles = -1L

    fpCompiled.doSim("perf_square_fp") { dut =>
      SimTimeout(2000000)
      dut.clockDomain.forkStimulus(periodNs)

      dut.io.out_Mats.ready #= true
      dut.io.in_Mats.valid #= true

      var cycle = 0L
      fork {
        while (true) {
          dut.clockDomain.waitSampling()
          cycle += 1
        }
      }

      var startCycle = -1L
      var endCycle = -1L
      var sendingCase = 0
      var sendingK = 0
      var outCount = 0
      var done = false

      StreamDriver(dut.io.in_Mats, dut.clockDomain) { payload =>
        if (sendingCase >= caseNum) {
          false
        } else {
          if (startCycle < 0) startCycle = cycle

          payload.OpMode.post_Shift #= 0
          payload.OpMode.MatrixOperation #= MatrixOperation_TypeDef.MatMul
          payload.OpMode.do_PostTranspose #= false

          val isFinal = sendingK == size - 1
          for (r <- 0 until size) {
            val aVal = matricesA(sendingCase)(r)(sendingK).toDouble
            val (as, ae, am) = unpackFloat(algo.toHost(aVal))
            payload.A(r).data.sign #= as
            payload.A(r).data.exp #= ae
            payload.A(r).data.mant #= am
            payload.A(r).Final #= isFinal
          }
          for (c <- 0 until size) {
            val bVal = matricesB(sendingCase)(sendingK)(c).toDouble
            val (bs, be, bm) = unpackFloat(algo.toHost(bVal))
            payload.B(c).data.sign #= bs
            payload.B(c).data.exp #= be
            payload.B(c).data.mant #= bm
            payload.B(c).Final #= isFinal
          }

          if (isFinal) {
            sendingK = 0
            sendingCase += 1
          } else {
            sendingK += 1
          }
          true
        }
      }

      StreamMonitor(dut.io.out_Mats, dut.clockDomain) { _ =>
        outCount += 1
        if (outCount == caseNum) {
          endCycle = cycle
          totalCycles = endCycle - startCycle + 1
          done = true
        }
      }

      dut.clockDomain.waitSamplingWhere(done)
      simSuccess()
    }

    PerfResult("SquareSystolicArray(fp16)", totalCycles, totalOps)
  }

  def runSystolic2DFixPerf(): PerfResult = {
    val fracBits = 8
    val inputBits = fxCfg.in_MatA_element_Width
    val totalOps = 2L * size * size * size * caseNum
    var totalCycles = -1L

    fxCompiled.doSim("perf_systolic2d_fix") { dut =>
      SimTimeout(2000000)
      dut.clockDomain.forkStimulus(periodNs)

      dut.io.out_Mats.ready #= true
      dut.io.in_Mats.valid #= true

      var cycle = 0L
      fork {
        while (true) {
          dut.clockDomain.waitSampling()
          cycle += 1
        }
      }

      var startCycle = -1L
      var endCycle = -1L
      var sendingCase = 0
      var sendingK = 0
      var outCount = 0
      var done = false

      StreamDriver(dut.io.in_Mats, dut.clockDomain) { payload =>
        if (sendingCase >= caseNum) {
          false
        } else {
          if (startCycle < 0) startCycle = cycle

          payload.OpMode.post_Shift #= 0
          payload.OpMode.MatrixOperation #= MatrixOperation_TypeDef.MatMul
          payload.OpMode.do_PostTranspose #= false

          val isFinal = sendingK == size - 1
          for (r <- 0 until size) {
            payload.A(r).data #= floatToFix(matricesA(sendingCase)(r)(sendingK), fracBits, inputBits)
            payload.A(r).Final #= isFinal
          }
          for (c <- 0 until size) {
            payload.B(c).data #= floatToFix(matricesB(sendingCase)(sendingK)(c), fracBits, inputBits)
            payload.B(c).Final #= isFinal
          }

          if (isFinal) {
            sendingK = 0
            sendingCase += 1
          } else {
            sendingK += 1
          }
          true
        }
      }

      StreamMonitor(dut.io.out_Mats, dut.clockDomain) { _ =>
        outCount += 1
        if (outCount == caseNum) {
          endCycle = cycle
          totalCycles = endCycle - startCycle + 1
          done = true
        }
      }

      dut.clockDomain.waitSamplingWhere(done)
      simSuccess()
    }

    PerfResult("SystolicArray2D(fix16,q8)", totalCycles, totalOps)
  }

  val fpRes = runSquareFpPerf()
  val fxRes = runSystolic2DFixPerf()

  println("===== Square Systolic Performance Compare =====")
  println(s"Size          : ${size}x$size")
  println(s"Case Num      : $caseNum")
  println(s"Seed          : $seed")
  println(s"${fpRes.name} -> cycles=${fpRes.totalCycles}, ops=${fpRes.totalOps}, ops/cycle=${fpRes.opsPerCycle}")
  println(s"${fxRes.name} -> cycles=${fxRes.totalCycles}, ops=${fxRes.totalOps}, ops/cycle=${fxRes.opsPerCycle}")
  val speedup = if (fpRes.opsPerCycle == 0.0) Double.NaN else fxRes.opsPerCycle / fpRes.opsPerCycle
  println(s"Speedup(fix/fp ops/cycle): $speedup")
}

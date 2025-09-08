package Accelerator

import Util._
import Interface._

import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.StreamDriver
import spinal.sim.VCSFlags

import scala.math._
import scala.util.Random
import scala.collection.mutable.ArrayBuffer

object AcceleratorTb extends App {
  val period = 10
  val instDriveSpeed = 10f
  val errRateLimit = 0.01
  val zeroLimit = 10
  val seed = 114514+1000
  val random = new Random(seed)
  var testNum = 50
  // var testNum = 1
  val acceleratorCfg = AcceleratorCfg(
    UIDWidth = 19,
    AddressWidth = 20,
    ShapeWidth = 16,
    elementWidth = 24,
    intWidth = 12,
    systolicArraySideNum = 8,//越高越快
    systolicArrayInFifoDepth = 16,//越高越好
    systolicArrayOutFifoDepth = 16,//越高越好,但帮助不大
    systolicArrayInstFifoDepth = 16,//越高越好,但帮助不大
    activationOutFifoDepth = 32,//能过reqirements就行
    slicedInstFifoDepth = 2,//能过reqirements就行
    numCores = 2//越高越快，但超过2之后帮助不大
  )
  val path = s"simWorkspace/AcceleratorTb"
  import java.io.File
  new File(path).mkdirs()
  val compiled = SimConfig.workspacePath(path).withFsdbWave
    .withConfig(
      SpinalConfig(
        //oneFilePerComponent = true,
        //removePruned = true,
        bitVectorWidthMax = 100000,
        //defaultClockDomainFrequency=FixedFrequency(1 GHz),
      )
    )
    //.withTimeScale(1 ns)
    //.withTimePrecision(1 ns)
    .allOptimisation
    .withVCS(
      VCSFlags(
        compileFlags = List("-kdb", "-lca", "+notimingchecks"),
        elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
        runFlags = List("-l ./run.log")
      )
    )
    .compile {
      val dut = Accelerator(acceleratorCfg)
      dut.sdpramA.mem.simPublic()
      dut.sdpramB.mem.simPublic()
      dut.sdpramZ.mem.simPublic()
      dut.collector.instFinish.simPublic()
      dut
    }

  def genMat(instSim: InstSim) = {
    if (instSim.activationFunction == Activation_TypeDef.Log) {
      val one = pow(2, acceleratorCfg.fracWidth).toInt
      val matA =
        if (random.nextBoolean) random.nextMat(instSim.input0Shape0, instSim.input0Shape1, one * 2, one * 3)
        else random.nextMat(instSim.input0Shape0, instSim.input0Shape1, one / 3, one / 2)
      val matB = random.nextMat(instSim.input1Shape0, instSim.input1Shape1, 0, 1)
      (matA, matB)
    } else {
      val matA = random.nextMat(instSim.input0Shape0, instSim.input0Shape1, -9, 10)
      val matB = random.nextMat(instSim.input1Shape0, instSim.input1Shape1, -9, 10)
      (matA, matB)
    }
  }

  val instSims = ArrayBuffer[InstSim]()
  val matAs = ArrayBuffer[Array[Array[Int]]]()
  val matBs = ArrayBuffer[Array[Array[Int]]]()
  val matZs = ArrayBuffer[Array[Array[Int]]]()
  for (i <- 0 until testNum) {
    val instSim = InstSim(random, acceleratorCfg.systolicArraySideNum)
    instSims += instSim
    val (matA, matB) = genMat(instSim)
    matAs += matA
    matBs += matB
    matZs += instSim.acceleratorSim(matA, matB, acceleratorCfg.elementWidth, acceleratorCfg.fracWidth)
  }

  var totalCycles: Long   = -1
  var cyclesPerTest: Double = -1
  var totalOps: Long = -1 // 总浮点运算次数
  var flopsPerCycle: Double = -1 // 每周期浮点运算次数 (FLOPS/cycle)

  compiled.doSimUntilVoid { dut =>
    SimTimeout(10000000 * 4*period)
    dut.clockDomain.forkStimulus(period)
    dut.clkCore.forkStimulus(4*period)
    // dut.clockDomain.forkStimulus(period)
    // dut.clkCore.forkStimulus(period)
    var startTime: Long = -1
    var endTime: Long   = -1
    var cycleCount: Long = 0
    // 手动计数
    fork {
      while (true) {
        dut.clockDomain.waitSampling()
        cycleCount += 1
      }
    }
    InstSim.memSetInstSims(
      dut.sdpramA.mem,
      instSims,
      true,
      matAs,
      acceleratorCfg.systolicArraySideNum,
      acceleratorCfg.elementWidth
    )
    InstSim.memSetInstSims(
      dut.sdpramB.mem,
      instSims,
      false,
      matBs,
      acceleratorCfg.systolicArraySideNum,
      acceleratorCfg.elementWidth
    )

    var m = 0
    StreamDriver(dut.io.inst, dut.clockDomain) { payload =>
      if (m < testNum) {
        if (m == 0) startTime = cycleCount
        instSims(m).driveSim(payload)
        m += 1
        true
      } else {
        false
      }
    }.setFactor(instDriveSpeed)

    var n = 0
    fork {
      while (true) {
        dut.clockDomain.waitSamplingWhere(dut.collector.instFinish.toBoolean == true)
        dut.clockDomain.waitSampling()
        dut.clockDomain.waitSampling()
        val instSim = instSims(n)
        val matA = matAs(n)
        val matB = matBs(n)
        val matZRef = matZs(n)
        val matZResult = memGetMat(
          dut.sdpramZ.mem,
          instSim.outputAddress,
          acceleratorCfg.systolicArraySideNum,
          acceleratorCfg.elementWidth,
          instSim.outputShape0,
          instSim.outputShape1
        )
        matZipForeach(matZRef, matZResult) { (zRef, zResult, i, j) =>
          val err = abs(zRef - zResult)
          val errRate = abs((zRef - zResult).toDouble / zRef.toDouble)
          assert(
            if (errRate < errRateLimit) true else abs(zRef) < zeroLimit && abs(zResult) < zeroLimit,
            s"matZRef compute error, test $n, position ($i, $j), zRef: $zRef(${zRef.toHexString}), ".red +
              s"zResult: $zResult(${zResult.toHexString}), err:$err, errRate: $errRate\n".red +
              s"$instSim\nmatA:\n${matToStringWithHex(matA)}\nmatB:\n${matToStringWithHex(matB)}\n" +
              s"matZRef:\n${matToStringWithHex(matZRef)}\nmatZResult:\n${matToStringWithHex(matZResult)}"
          )
        }
        println(s"test $n pass")
        n += 1
        if (n == testNum ) {
          println("TEST PASS".green)

          endTime = cycleCount
          totalCycles   = endTime - startTime
          cyclesPerTest = totalCycles.toDouble / testNum
          
          totalOps = instSims.map { inst =>
            // M*K*N*2，其中 K=inst.input0Shape1，也是 inst.input1Shape0
            2L * inst.input0Shape0 * inst.input0Shape1 * inst.input1Shape1
          }.sum
          flopsPerCycle = totalOps.toDouble / totalCycles

          println(s"Total cycles: $totalCycles, Cycles/test: $cyclesPerTest")
          println(s"Total operations: $totalOps, FLOPS/cycle: $flopsPerCycle")
          
          simSuccess()
        }
      }
    }
  }
}

package Accelerator

import Interface._
import Util._

import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.sim.StreamDriver

import scala.math._
import scala.util.Random
import scala.collection.mutable.ArrayBuffer

object AcceleratorTb extends App {
  val period = 10
  val instDriveSpeed = 0.5f
  val errRateLimit = 0.01
  val zeroLimit = 10
  val seed = 114514
  val random = new Random(seed)
  var testNum = 100
  // var testNum = 1
  val acceleratorCfg = Accelerator_Config(
    UIDWidth = 32,
    ShiftWidth = 6,
    AddressWidth = 20,
    ShapeWidth = 16,
    matSubRowNum = 32,
    activationRowNum = 32,
    elementWidth = 24,
    intWidth = 12,
    in_Length_Max = 32,
    systolicArrayInFifoDepth = 16,
    systolicArrayOutFifoDepth = 8,
    systolicArrayInstFifoDepth = 32,
    activationOutFifoDepth = 32,
    slicedInstFifoDepth = 32,
    numCores = 4
  )
  // val acceleratorCfg = Accelerator_Config(
  //   UIDWidth = 32,
  //   ShiftWidth = 6,
  //   AddressWidth = 20,
  //   ShapeWidth = 16,
  //   matSubRowNum = 2,
  //   activationRowNum = 2,
  //   elementWidth = 24,
  //   intWidth = 12,
  //   in_Length_Max = 32,
  //   systolicArrayInFifoDepth = 16,
  //   systolicArrayOutFifoDepth = 8,
  //   systolicArrayInstFifoDepth = 32,
  //   activationOutFifoDepth = 32,
  //   slicedInstFifoDepth = 32,
  //   numCores = 1
  // )
  testNum += acceleratorCfg.numCores // This is magic
  val compiled = SimConfig.withFsdbWave
    .withConfig(
      SpinalConfig(
        bitVectorWidthMax = 100000
      )
    )
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
      dut.collector.inst_finish.simPublic()
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
    val instSim = InstSim(random, acceleratorCfg.matSubRowNum)
    instSims += instSim
    val (matA, matB) = genMat(instSim)
    matAs += matA
    matBs += matB
    matZs += instSim.acceleratorSim(matA, matB, acceleratorCfg.elementWidth, acceleratorCfg.fracWidth)
  }

  compiled.doSimUntilVoid { dut =>
    SimTimeout(10000000 * period)
    dut.clockDomain.forkStimulusRandomClk(random, period)
    dut.clkCore.forkStimulusRandomClk(random, period)
    // dut.clockDomain.forkStimulus(period)
    // dut.clkCore.forkStimulus(period)
    InstSim.memSetInstSims(
      dut.sdpramA.mem,
      instSims,
      true,
      matAs,
      acceleratorCfg.matSubRowNum,
      acceleratorCfg.elementWidth
    )
    InstSim.memSetInstSims(
      dut.sdpramB.mem,
      instSims,
      false,
      matBs,
      acceleratorCfg.matSubRowNum,
      acceleratorCfg.elementWidth
    )

    var m = 0
    StreamDriver(dut.io.ComputeInstruction_Stream, dut.clockDomain) { payload =>
      if (m < testNum) {
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
        dut.clockDomain.waitSamplingWhere(dut.collector.inst_finish.toBoolean == true)
        dut.clockDomain.waitSampling()
        dut.clockDomain.waitSampling()
        val instSim = instSims(n)
        val matA = matAs(n)
        val matB = matBs(n)
        val matZRef = matZs(n)
        val matZResult = memGetMat(
          dut.sdpramZ.mem,
          instSim.outputAddress,
          acceleratorCfg.matSubRowNum,
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
        if (n == testNum - acceleratorCfg.numCores) {
          println("TEST PASS".green)
          simSuccess()
        }
      }
    }
  }
}

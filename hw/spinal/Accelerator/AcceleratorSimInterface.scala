package Accelerator

import Interface._
import Util._

import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.sim.StreamDriver

import scala.util.Random

object AcceleratorSimInterface {
  val period = 10
  val instDriveSpeed = 0.5f
  val seed = 114514
  val random = new Random(seed)
  val acceleratorCfg = AcceleratorCfg(
    UIDWidth = 16,
    AddressWidth = 20,
    ShapeWidth = 16,
    systolicArraySideNum = 32,
    elementWidth = 32,
    intWidth = 10,
    systolicArrayInFifoDepth = 8,
    systolicArrayOutFifoDepth = 8,
    systolicArrayInstFifoDepth = 32,
    activationOutFifoDepth = 32,
    slicedInstFifoDepth = 32,
    numCores = 2
  )
  lazy val compiled = SimConfig.withFsdbWave // This is magic
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
      dut.collector.instFinish.simPublic()
      dut
    }

  def runSimOneInst(matA: Array[Array[Int]], matB: Array[Array[Int]], instJava: InstJavaTODO) = {
    val instSim = new InstSim(instJava)
    var matZ = Array[Array[Int]]()
    compiled.doSimUntilVoid { dut =>
      SimTimeout(10000000 * period)
      dut.clockDomain.forkStimulusRandomClk(random, period)
      dut.clkCore.forkStimulusRandomClk(random, period)
      memSetMat(
        dut.sdpramA.mem,
        instSim.input0Address,
        matA,
        acceleratorCfg.systolicArraySideNum,
        acceleratorCfg.elementWidth
      )
      memSetMat(
        dut.sdpramB.mem,
        instSim.input1Address,
        matB,
        acceleratorCfg.systolicArraySideNum,
        acceleratorCfg.elementWidth
      )

      var m = 0
      StreamDriver(dut.io.inst, dut.clockDomain) { payload =>
        if (m < 1 + 2 * acceleratorCfg.numCores) { // This is magic
          instSim.driveSim(payload)
          m += 1
          true
        } else {
          false
        }
      }.setFactor(instDriveSpeed)

      fork {
        while (true) {
          dut.clockDomain.waitSamplingWhere(dut.collector.instFinish.toBoolean)
          dut.clockDomain.waitSampling()
          matZ = memGetMat(
            dut.sdpramZ.mem,
            instSim.outputAddress,
            acceleratorCfg.systolicArraySideNum,
            acceleratorCfg.elementWidth,
            instSim.outputShape0,
            instSim.outputShape1
          )
          simSuccess()
        }
      }
    }
    matZ
  }
  def runRefOneInst(matA: Array[Array[Int]], matB: Array[Array[Int]], instJava: InstJavaTODO) = {
    val instSim = new InstSim(instJava)
    var matZ = Array[Array[Int]]()
    matZ=instSim.acceleratorSim(matA, matB, acceleratorCfg)
    matZ
  }
  def runRefOneInst(matA: Array[Array[Long]], matB: Array[Array[Long]], instJava: InstJavaTODO) = {
    val instSim = new InstSim(instJava)
    var matZ = Array[Array[BigInt]]()
    matZ=instSim.acceleratorSim(matA.map(_.map(BigInt(_))), matB.map(_.map(BigInt(_))), acceleratorCfg)
    matZ.map(_.map(_.toLong))
  }
}

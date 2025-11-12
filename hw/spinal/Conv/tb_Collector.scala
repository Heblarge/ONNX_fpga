package Slicer

import Util._
import Accelerator._
import DataPump._

import spinal.core._
import spinal.core.sim._
import spinal.lib.{Stream, slave}
import spinal.lib.sim.{StreamDriver, StreamMonitor}
import spinal.sim.VCSFlags

import scala.util.Random
import scala.collection.mutable.ArrayBuffer

case class CollectorTest(collectorCfg: CollectorCfg) extends Component {
  val sdpramZ = Sdpram(addrWidth = collectorCfg.AddressWidth, dataWidth = collectorCfg.dataWidthZ)
  val collector = Collector(collectorCfg)
  val io = new Bundle {
    val slicedInst = slave Stream collector.SlicedInstType
    val matAfterActivations =
      Vec.fill(collectorCfg.numCores)(slave Stream collector.MatAfterActivationType)
  }

  collector.io.slicedInst <> io.slicedInst
  collector.io.matAfterActivations <> io.matAfterActivations
  collector.io.memoryWritePort <> sdpramZ.io.write
  sdpramZ.noRead()
}

object CollectorTb extends App {
  val period = 10
  val instDriveSpeed = 0.5f
  val dataDriveSpeedInSlice = 0.5f
  val dataDriveSpeedBetweenSlice = 0.01f
  val seed = 114514
  val random = new Random(seed)
  val testNum = 100
  // val testNum = 1
  val collectorCfg = CollectorCfg(
    UIDWidth = 19,
    AddressWidth = 17,
    ShapeWidth = 15,
    SlicecntWidth = 13,
    slicedInstFifoDepth = 33,
    systolicArraySideNum = 6,
    activationUnitNum = 4,
    elementWidthZ = 9,
    numCores = 3
  )
  // val collectorCfg = CollectorCfg(
  //   UIDWidth = 16,
  //   AddressWidth = 16,
  //   ShapeWidth = 16,
  //   SlicecntWidth = 16,
  //   slicedInstFifoDepth = 32,
  //   systolicArraySideNum = 2,
  //   activationUnitNum = 2,
  //   elementWidthZ = 8,
  //   numCores = 2
  // )
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
      val dut = CollectorTest(collectorCfg)
      dut.sdpramZ.mem.simPublic()
      dut.collector.instFinish.simPublic()
      dut
    }

  val instSims = ArrayBuffer[InstSim]()
  val matZs = ArrayBuffer[Array[Array[Int]]]()
  var n = random.nextInt(collectorCfg.numCores);
  val testInputs =
    Array.fill(collectorCfg.numCores)(ArrayBuffer[ArrayBuffer[ArrayBuffer[(Array[Int], Int, Int, Int)]]]())
  for (m <- 0 until testNum) {
    val instSim = InstSim(random, collectorCfg.systolicArraySideNum)
    instSims += instSim
    val matZ = random.nextMat(instSim.input0Shape0, instSim.input1Shape1)
    matZs += matZ
    val testInputs2 = Array.fill(collectorCfg.numCores)(ArrayBuffer[ArrayBuffer[(Array[Int], Int, Int, Int)]]())
    for (
      i <- 0 until instSim.input0Shape0 / collectorCfg.systolicArraySideNum;
      j <- 0 until instSim.input1Shape1 / collectorCfg.systolicArraySideNum
    ) {
      val MatZsub = instSim
        .transposeSim(
          matGetSub(
            matZ,
            i * collectorCfg.systolicArraySideNum,
            j * collectorCfg.systolicArraySideNum,
            collectorCfg.systolicArraySideNum,
            collectorCfg.systolicArraySideNum
          )
        )
        .flatten
      val testInputs3 = ArrayBuffer.tabulate(collectorCfg.activationRowNum)(k =>
        (
          Array.tabulate(collectorCfg.activationUnitNum)(l => MatZsub(k * collectorCfg.activationUnitNum + l)),
          random.validNumWhen(instSim.UID, k == 0),
          random.validNumWhen(i, k == 0),
          random.validNumWhen(j, k == 0)
        )
      )
      testInputs2(n) += testInputs3
      n = random.nextInt(collectorCfg.numCores);
    }
    testInputs.zip(testInputs2).foreach { case (x, y) => x += y }
  }

  compiled.doSimUntilVoid { dut =>
    SimTimeout(1000000 * period)
    dut.clockDomain.forkStimulus(period)

    var m = 0
    StreamDriver(dut.io.slicedInst, dut.clockDomain) { payload =>
      if (m < testNum) {
        instSims(m).driveSim(payload)
        m += 1
        true
      } else {
        false
      }
    }.setFactor(instDriveSpeed)

    dut.io.matAfterActivations.zipWithIndex.foreach { case (matAfterActivation, l) =>
      var i, j, k = 0
      while (if (i < testNum) testInputs(l)(i).length == 0 else false) i += 1
      val streamDriver = StreamDriver(matAfterActivation, dut.clockDomain) { payload =>
        if (i < testNum) {
          val testInput = testInputs(l)(i)(j)(k)
          payload.Activation_x #= testInput._1
          payload.CoreInstruction_AfterActivation.Collector_Instruction.UID #= testInput._2
          payload.CoreInstruction_AfterActivation.Collector_Instruction.MatA_row_slice_cnt #= testInput._3
          payload.CoreInstruction_AfterActivation.Collector_Instruction.MatB_col_slice_cnt #= testInput._4
          val isFinal = k == testInputs(l)(i)(j).length - 1
          payload.Final #= isFinal
          k += 1
          if (isFinal) {
            k = 0
            j += 1
            if (j == testInputs(l)(i).length) {
              j = 0
              i += 1
              while (if (i < testNum) testInputs(l)(i).length == 0 else false) i += 1
            }
          }
          true
        } else {
          false
        }
      }
      StreamMonitor(matAfterActivation, dut.clockDomain) { payload =>
        if (payload.Final.toBoolean) {
          streamDriver.setFactor(dataDriveSpeedBetweenSlice)
        } else {
          streamDriver.setFactor(dataDriveSpeedInSlice)
        }
      }
    }

    var n = 0
    fork {
      while (true) {
        dut.clockDomain.waitSamplingWhere(dut.collector.instFinish.toBoolean)
        dut.clockDomain.waitSampling()
        val instSim = instSims(n)
        val matZRef = instSim.transposeSim(matZs(n))
        val matZResult = memGetMat(
          dut.sdpramZ.mem,
          instSim.outputAddress,
          collectorCfg.systolicArraySideNum,
          collectorCfg.elementWidthZ,
          instSim.outputShape0,
          instSim.outputShape1
        )
        matZipForeach(matZRef, matZResult) { (zRef, zResult, i, j) =>
          assert(
            zRef == zResult,
            s"matZ collect error, test $n, position: ($i, $j), zRef: $zRef(${zRef.toHexString}), ".red +
              s"zResult: $zResult(${zResult.toHexString})\n".red +
              s"$instSim\nmatZRef:\n${matToStringWithHex(matZRef)}\nmatZResult:\n${matToStringWithHex(matZResult)}"
          )
        }
        println(s"test $n pass")
        n += 1
        if (n == testNum) {
          println("TEST PASS".green)
          simSuccess()
        }
      }
    }
  }
}

package Slicer

import Util._
import Interface._
import Accelerator._
import DataPump._

import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.{Stream, master, slave}
import spinal.lib.sim.{StreamDriver, StreamReadyRandomizer, StreamMonitor}

import scala.util.Random
import scala.collection.mutable.ArrayBuffer

case class SlicerTest(slicerCfg: SlicerCfg) extends Component {
  val dataPumpA = DataPump_mm2s(
    DataPump_mm2s_Config(
      mem_data_width = slicerCfg.dataWidthA,
      mem_addr_width = slicerCfg.AddressWidth,
      RepeatNum_Max = 1,
      Enable_Padding_logic = false
    )
  )
  val dataPumpB = DataPump_mm2s(
    DataPump_mm2s_Config(
      mem_data_width = slicerCfg.dataWidthB,
      mem_addr_width = slicerCfg.AddressWidth,
      RepeatNum_Max = 1,
      Enable_Padding_logic = false
    )
  )
  val sdpramA = Sdpram(addrWidth = slicerCfg.AddressWidth, dataWidth = slicerCfg.dataWidthA)
  val sdpramB = Sdpram(addrWidth = slicerCfg.AddressWidth, dataWidth = slicerCfg.dataWidthB)
  val slicer = Slicer(slicerCfg)
  val io = new Bundle {
    val ComputeInstruction_Stream = slave Stream (slicer.InstType)
    val Sliced_ComputeInstruction_Stream = master Stream (slicer.SlicedInstType)
    val Mats_to_Cores_Streams = Vec.fill(slicerCfg.numCores)(master Stream (slicer.InMatsType))
  }

  slicer.io.ComputeInstruction_Stream <> io.ComputeInstruction_Stream
  slicer.io.Sliced_ComputeInstruction_Stream <> io.Sliced_ComputeInstruction_Stream
  slicer.io.Mats_to_Cores_Streams <> io.Mats_to_Cores_Streams
  slicer.io.TaskA_Stream <> dataPumpA.io.TaskStream
  slicer.io.DataA_Stream <> dataPumpA.io.DataStream
  slicer.io.TaskB_Stream <> dataPumpB.io.TaskStream
  slicer.io.DataB_Stream <> dataPumpB.io.DataStream
  sdpramA.io.read <> dataPumpA.io.MemoryReadPort
  sdpramA.noWrite
  sdpramB.io.read <> dataPumpB.io.MemoryReadPort
  sdpramB.noWrite
}

object SlicerTb extends App {
  val period = 10
  val instDriveSpeed = 0.5f
  val slicedInstReceiveSpeed = 0.5f
  val dataReceiveSpeed = 0.5f
  val seed = 114514
  val random = new Random(seed)
  val testNum = 100
  // val testNum = 1
  val slicerCfg = SlicerCfg(
    UIDWidth = 33,
    ShiftWidth = 7,
    AddressWidth = 17,
    ShapeWidth = 15,
    SlicecntWidth = 13,
    matSubRowNum = 3,
    elementWidthA = 11,
    elementWidthB = 9,
    numCores = 4
  )
  // val slicerCfg = SlicerCfg(
  //   UIDWidth = 32,
  //   ShiftWidth = 6,
  //   AddressWidth = 16,
  //   ShapeWidth = 16,
  //   SlicecntWidth = 16,
  //   matSubRowNum = 2,
  //   elementWidthA = 8,
  //   elementWidthB = 8,
  //   numCores = 1
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
      val dut = SlicerTest(slicerCfg)
      dut.sdpramA.mem.simPublic()
      dut.sdpramB.mem.simPublic()
      dut
    }

  val instSims = ArrayBuffer[InstSim]()
  val matAs = ArrayBuffer[Array[Array[Int]]]()
  val matBs = ArrayBuffer[Array[Array[Int]]]()
  val testRefs = ArrayBuffer[ArrayBuffer[ArrayBuffer[(Array[Int], Array[Int])]]]()
  for (m <- 0 until testNum) {
    val instSim = InstSim(random, slicerCfg.matSubRowNum)
    instSims += instSim
    val matA = random.nextMat(instSim.input0Shape0, instSim.input0Shape1)
    val matB = random.nextMat(instSim.input1Shape0, instSim.input1Shape1)
    matAs += matA
    matBs += matB
    val testRefs2 = ArrayBuffer[ArrayBuffer[(Array[Int], Array[Int])]]()
    for (
      i <- 0 until instSim.input0Shape0 / slicerCfg.matSubRowNum;
      j <- 0 until instSim.input1Shape1 / slicerCfg.matSubRowNum
    ) {
      val testRefs3 = ArrayBuffer[(Array[Int], Array[Int])]()
      val isMatMul = instSim.matrixOperation == MatrixOperation_TypeDef.MatMul
      for (k <- 0 until (if (isMatMul) instSim.input0Shape1 / slicerCfg.matSubRowNum else 1)) {
        val matASub = matGetSub(
          matA,
          i * slicerCfg.matSubRowNum,
          if (isMatMul) k * slicerCfg.matSubRowNum else j * slicerCfg.matSubRowNum,
          slicerCfg.matSubRowNum,
          slicerCfg.matSubRowNum
        ).transpose
        var matBSub = matGetSub(
          matB,
          if (isMatMul) k * slicerCfg.matSubRowNum else i * slicerCfg.matSubRowNum,
          j * slicerCfg.matSubRowNum,
          slicerCfg.matSubRowNum,
          slicerCfg.matSubRowNum
        )
        if (!isMatMul) matBSub = matRotateCw(matBSub)
        matASub.zip(matBSub).foreach(testRefs3 += _)
      }
      testRefs2 += testRefs3
    }
    testRefs += testRefs2
  }

  compiled.doSimUntilVoid { dut =>
    SimTimeout(1000000 * period)
    dut.clockDomain.forkStimulus(period)
    InstSim.memSetInstSims(dut.sdpramA.mem, instSims, true, matAs, slicerCfg.matSubRowNum, slicerCfg.elementWidthA)
    InstSim.memSetInstSims(dut.sdpramB.mem, instSims, false, matBs, slicerCfg.matSubRowNum, slicerCfg.elementWidthB)

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

    var i, j, k = 0
    StreamReadyRandomizer(dut.io.Sliced_ComputeInstruction_Stream, dut.clockDomain).setFactor(slicedInstReceiveSpeed)
    dut.io.Mats_to_Cores_Streams.foreach(StreamReadyRandomizer(_, dut.clockDomain).setFactor(dataReceiveSpeed))
    dut.io.Mats_to_Cores_Streams.foreach(StreamMonitor(_, dut.clockDomain) { payload =>
      val instSim = instSims(i)
      val matColSliceNum = instSim.input1Shape1 * slicerCfg.matSubRowNum
      val matA = matAs(i)
      val matB = matBs(i)
      val testRef = testRefs(i)(j)(k)
      val finalRef = k == testRefs(i)(j).length - 1
      val outputAResult = payload.A.map(_.toInt).toArray
      val outputBResult = payload.B.map(_.toInt).toArray
      val finalResult = payload.Final.toBoolean
      vecZipForeach(testRef._1, outputAResult)((aRef, aResult, l) =>
        assert(
          aRef == aResult,
          s"matA slice error, test $i, slice position (${j / matColSliceNum}, ${j % matColSliceNum}), ".red +
            s"output order $k, output row $l, aRef: $aRef(${aRef.toHexString}), ".red +
            s"aResult: $aResult(${aResult.toHexString})\n".red +
            s"$instSim\nmatA:\n${matToStringWithHex(matA)}\n" +
            s"outputARef:\n${vecToStringWithHex(testRef._1)}\noutputAResult:\n${vecToStringWithHex(outputAResult)}"
        )
      )
      vecZipForeach(testRef._2, outputBResult)((bRef, bResult, l) =>
        assert(
          bRef == bResult,
          s"matB slice error, test $i, slice position (${j / matColSliceNum}, ${j % matColSliceNum}), ".red +
            s"output order $k, output row $l, bRef: $bRef(${bRef.toHexString}), ".red +
            s"bResult: $bResult(${bResult.toHexString})\n".red +
            s"$instSim\nmatB:\n${matToStringWithHex(matB)}\n" +
            s"outputBRef:\n${vecToStringWithHex(testRef._2)}\noutputBResult:\n${vecToStringWithHex(outputBResult)}"
        )
      )
      assert(
        finalRef == finalResult,
        s"mat slice Final error, test $i, slice position (${j / matColSliceNum}, ${j % matColSliceNum}), ".red +
          s"output order $k, finalRef: $finalRef, finalResult: $finalResult\n".red +
          s"$instSim\nmatA:\n${matToStringWithHex(matA)}\nmatB:\n${matToStringWithHex(matB)}\n" +
          s"outputARef:\n${vecToStringWithHex(testRef._1)}\noutputAResult:\n${vecToStringWithHex(outputAResult)}\n" +
          s"outputBRef:\n${vecToStringWithHex(testRef._2)}\noutputBResult:\n${vecToStringWithHex(outputBResult)}"
      )
      k += 1
      if (finalRef) {
        k = 0
        j += 1
        if (j == testRefs(i).length) {
          j = 0
          println(s"test $i pass")
          i += 1
          if (i == testNum) {
            println("TEST PASS".green)
            simSuccess()
          }
        }
      }
    })
  }
}

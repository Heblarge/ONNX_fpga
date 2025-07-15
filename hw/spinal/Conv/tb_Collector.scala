package Slicer

import Util._
import Accelerator._
import DataPump._

import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.{Stream, slave}
import spinal.lib.sim.{StreamDriver, StreamMonitor}

import scala.util.Random
import scala.collection.mutable.ArrayBuffer

case class CollectorTest(collectorCfg: Collector_Config) extends Component {
  val dataPumpZ = DataPump_s2mm(
    DataPump_s2mm_Config(
      mem_data_width = collectorCfg.mem_data_widthZ,
      mem_addr_width = collectorCfg.AddressWidth,
      RepeatNum_Max = 1,
      Enable_UnPadding_logic = false,
      Enable_Error_Port_logic = false
    )
  )
  val sdpramZ = Sdpram(addrWidth = collectorCfg.AddressWidth, dataWidth = collectorCfg.mem_data_widthZ)
  val collector = Collector(collectorCfg)
  val io = new Bundle {
    val Sliced_ComputeInstruction_Stream = slave Stream (collector.Sliced_ComputeInstruction_Type())
    val Mats_from_Cores_Streams =
      Vec.fill(collectorCfg.numCores)(slave Stream (collector.out_Mats_AfterActivation_Type()))
  }

  collector.io.Sliced_ComputeInstruction_Stream <> io.Sliced_ComputeInstruction_Stream
  collector.io.Mats_from_Cores_Streams <> io.Mats_from_Cores_Streams
  collector.io.Task_Stream <> dataPumpZ.io.TaskStream
  collector.io.Data_Stream <> dataPumpZ.io.DataStream
  sdpramZ.io.write <> dataPumpZ.io.MemoryWritePort
  sdpramZ.noRead
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
  val collectorCfg = Collector_Config(
    UIDWidth = 33,
    AddressWidth = 17,
    ShapeWidth = 15,
    SlicecntWidth = 13,
    in_MatA_row_num = 6,
    in_MatB_col_num = 6,
    MatX_Width = 4,
    Activation_x_Width = 9,
    numCores = 4
  )
  // val collectorCfg = Collector_Config(
  //   UIDWidth = 32,
  //   AddressWidth = 16,
  //   ShapeWidth = 16,
  //   SlicecntWidth = 16,
  //   in_MatA_row_num = 2,
  //   in_MatB_col_num = 2,
  //   MatX_Width = 2,
  //   Activation_x_Width = 8,
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
      val dut = CollectorTest(collectorCfg)
      dut.sdpramZ.mem.simPublic()
      dut.collector.inst_finish.simPublic()
      dut
    }

  val instSims = ArrayBuffer[InstSim]()
  val matZs = ArrayBuffer[Array[Array[Int]]]()
  var n = random.nextInt(collectorCfg.numCores);
  val testInputs =
    Array.fill(collectorCfg.numCores)(ArrayBuffer[ArrayBuffer[ArrayBuffer[(Array[Int], Int, Int, Int)]]]())
  for (m <- 0 until testNum) {
    val instSim = InstSim(random, collectorCfg.MatAsub_row_num)
    instSims += instSim
    val matZ = random.nextMat(instSim.input0Shape0, instSim.input1Shape1)
    matZs += matZ
    val testInputs2 = Array.fill(collectorCfg.numCores)(ArrayBuffer[ArrayBuffer[(Array[Int], Int, Int, Int)]]())
    for (
      i <- 0 until instSim.input0Shape0 / collectorCfg.MatAsub_row_num;
      j <- 0 until instSim.input1Shape1 / collectorCfg.MatBsub_col_num
    ) {
      val MatZsub = instSim
        .transposeSim(
          matGetSub(
            matZ,
            i * collectorCfg.MatAsub_row_num,
            j * collectorCfg.MatBsub_col_num,
            collectorCfg.MatAsub_row_num,
            collectorCfg.MatBsub_col_num
          )
        )
        .flatten
      val testInputs3 = ArrayBuffer.tabulate(collectorCfg.Matin_row_num)(k =>
        (
          Array.tabulate(collectorCfg.Matin_col_num)(l => MatZsub(k * collectorCfg.Matin_col_num + l)),
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
    StreamDriver(dut.io.Sliced_ComputeInstruction_Stream, dut.clockDomain) { payload =>
      if (m < testNum) {
        instSims(m).driveSim(payload)
        m += 1
        true
      } else {
        false
      }
    }.setFactor(instDriveSpeed)

    dut.io.Mats_from_Cores_Streams.zipWithIndex.foreach { Mats_from_Core_StreamWithIndex =>
      var i, j, k = 0
      var l = Mats_from_Core_StreamWithIndex._2
      while (if (i < testNum) testInputs(l)(i).length == 0 else false) i += 1
      val streamDriver = StreamDriver(Mats_from_Core_StreamWithIndex._1, dut.clockDomain) { payload =>
        if (i < testNum) {
          val testInput = testInputs(l)(i)(j)(k)
          payload.Activation_x.zip(testInput._1).foreach { case (x, y) => x #= y }
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
      StreamMonitor(Mats_from_Core_StreamWithIndex._1, dut.clockDomain) { payload =>
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
        dut.clockDomain.waitSamplingWhere(dut.collector.inst_finish.toBoolean)
        dut.clockDomain.waitSampling()
        dut.clockDomain.waitSampling()
        val instSim = instSims(n)
        val matZRef = instSim.transposeSim(matZs(n))
        val matZResult = memGetMat(
          dut.sdpramZ.mem,
          instSim.outputAddress,
          collectorCfg.MatBsub_col_num,
          collectorCfg.Activation_x_Width,
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

package Tiling

import Util._
import Interface._
import Accelerator._
import DataPump._

import spinal.core._
import spinal.core.sim._
import spinal.lib.{Stream, master, slave}
import spinal.lib.sim.{StreamDriver, StreamReadyRandomizer, StreamMonitor}
import spinal.sim.VCSFlags

import scala.util.Random
import scala.collection.mutable.ArrayBuffer

case class SlicerTest(slicerCfg: SlicerCfg) extends Component {

  // FIX: 仿真内存必须是 32位 * SideNum 宽度，以匹配 InstSim 中的 (col * 32) 写入逻辑
  val ramDataWidth = slicerCfg.systolicArraySideNum * 32

  val sdpramA = Sdpram(addrWidth = slicerCfg.AddressWidth, dataWidth = ramDataWidth)
  val sdpramB = Sdpram(addrWidth = slicerCfg.AddressWidth, dataWidth = ramDataWidth)
//  val sdpramA = Sdpram(addrWidth = slicerCfg.AddressWidth, dataWidth = slicerCfg.dataWidthA)
//  val sdpramB = Sdpram(addrWidth = slicerCfg.AddressWidth, dataWidth = slicerCfg.dataWidthB)
  val slicer = Slicer(slicerCfg)
  val io = new Bundle {
    val inst = slave Stream slicer.InstType
    val slicedInst = master Stream slicer.SlicedInstType
    val Mats_to_Cores_Streams = Vec.fill(slicerCfg.numCores)(master Stream slicer.MatAfterSlicerType)
  }

  slicer.io.inst <> io.inst
  slicer.io.slicedInst <> io.slicedInst
  slicer.io.matAfterSlicers <> io.Mats_to_Cores_Streams

  // ================== A Port Glue Logic ==================
  sdpramA.io.read.Valid := slicer.io.memoryReadPortA.Valid
  sdpramA.io.read.Address := slicer.io.memoryReadPortA.Address
  // 如果 Sdpram 是同步读，可能需要时钟连接，具体取决于 Util.Sdpram 实现
  // sdpramA.io.read.clk := ClockDomain.current.readClockWire

  // 数据适配：从 RAM 读出 32位宽数据 -> 截取 -> 拼成 Slicer 需要的紧凑位宽
  val rawDataA = sdpramA.io.read.Data // 160 bits (5 * 32)
  val slicedDataA = Vec(Bits(slicerCfg.elementWidthA bits), slicerCfg.systolicArraySideNum)
  for(i <- 0 until slicerCfg.systolicArraySideNum) {
    // 取出第 i 个 32位数据，resize 到 elementWidthA (9 bits)
    slicedDataA(i) := rawDataA.subdivideIn(32 bits)(i).resize(slicerCfg.elementWidthA)
  }
  slicer.io.memoryReadPortA.Data := slicedDataA.asBits

  // ================== B Port Glue Logic ==================
  sdpramB.io.read.Valid := slicer.io.memoryReadPortB.Valid
  sdpramB.io.read.Address := slicer.io.memoryReadPortB.Address

  val rawDataB = sdpramB.io.read.Data // 160 bits (5 * 32)
  val slicedDataB = Vec(Bits(slicerCfg.elementWidthB bits), slicerCfg.systolicArraySideNum)
  for(i <- 0 until slicerCfg.systolicArraySideNum) {
    // 取出第 i 个 32位数据，resize 到 elementWidthB (7 bits)
    slicedDataB(i) := rawDataB.subdivideIn(32 bits)(i).resize(slicerCfg.elementWidthB)
  }
  slicer.io.memoryReadPortB.Data := slicedDataB.asBits

  sdpramA.noWrite()
  sdpramB.noWrite()

//  slicer.io.inst <> io.inst
//  slicer.io.slicedInst <> io.slicedInst
//  slicer.io.matAfterSlicers <> io.Mats_to_Cores_Streams
//  slicer.io.memoryReadPortA <> sdpramA.io.read
//  slicer.io.memoryReadPortB <> sdpramB.io.read
//  sdpramA.noWrite()
//  sdpramB.noWrite()
}

object SlicerTb extends App {
  val period = 10
  val instDriveSpeed = 0.5f
  val slicedInstReceiveSpeed = 0.5f
  val dataReceiveSpeed = 0.5f
  val seed = 114
  val random = new Random(seed)
  val testNum = 100
  // val testNum = 1
  val slicerCfg = SlicerCfg(
    UIDWidth = 19,
    ShiftWidth = 6,
    AddressWidth = 20,
    ShapeWidth = 15,
    SlicecntWidth = 13,
    systolicArraySideNum = 5,
    elementWidthA = 9,
    elementWidthB = 7,
    numCores = 3
  )

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
    val instSim = InstSim(random, slicerCfg.systolicArraySideNum)
    instSims += instSim
    val matA = random.nextMat(instSim.input0Shape0, instSim.input0Shape1)
    val matB = random.nextMat(instSim.input1Shape0, instSim.input1Shape1)
    matAs += matA
    matBs += matB
    val testRefs2 = ArrayBuffer[ArrayBuffer[(Array[Int], Array[Int])]]()
    for (
      i <- 0 until instSim.input0Shape0 / slicerCfg.systolicArraySideNum;
      j <- 0 until instSim.input1Shape1 / slicerCfg.systolicArraySideNum
    ) {
      val testRefs3 = ArrayBuffer[(Array[Int], Array[Int])]()
      val isMatMul = instSim.matrixOperation == MatrixOperation_TypeDef.MatMul
      for (k <- 0 until (if (isMatMul) instSim.input0Shape1 / slicerCfg.systolicArraySideNum else 1)) {
        var matASub = matGetSub(
          matA,
          i * slicerCfg.systolicArraySideNum,
          if (isMatMul) k * slicerCfg.systolicArraySideNum else j * slicerCfg.systolicArraySideNum,
          slicerCfg.systolicArraySideNum,
          slicerCfg.systolicArraySideNum
        ).transpose
        matASub=matElementwiseShift(matASub.map(_.map(BigInt(_))), -instSim.shiftLeft_A, slicerCfg.elementWidthA).map(_.map(_.toInt))
        // matASub = matASub.map(row => row.map(elem => elem << instSim.shiftLeft_A))
        var matBSub = matGetSub(
          matB,
          if (isMatMul) k * slicerCfg.systolicArraySideNum else i * slicerCfg.systolicArraySideNum,
          j * slicerCfg.systolicArraySideNum,
          slicerCfg.systolicArraySideNum,
          slicerCfg.systolicArraySideNum
        )
        if (!isMatMul) matBSub = matRotateCw(matBSub)
        matBSub=matElementwiseShift(matBSub.map(_.map(BigInt(_))), -instSim.shiftLeft_B, slicerCfg.elementWidthB).map(_.map(_.toInt))
        // matBSub = matBSub.map(row =>row.map(elem => elem << instSim.shiftLeft_B))
        matASub.zip(matBSub).foreach(testRefs3 += _)
      }
      testRefs2 += testRefs3
    }
    testRefs += testRefs2
  }

  compiled.doSimUntilVoid { dut =>
    SimTimeout(1000000 * period)
    dut.clockDomain.forkStimulus(period)
    InstSim.memSetInstSims(
      dut.sdpramA.mem,
      instSims,
      true,
      matAs,
      slicerCfg.systolicArraySideNum,
      slicerCfg.elementWidthA
    )
    InstSim.memSetInstSims(
      dut.sdpramB.mem,
      instSims,
      false,
      matBs,
      slicerCfg.systolicArraySideNum,
      slicerCfg.elementWidthB
    )

    var m = 0
    StreamDriver(dut.io.inst, dut.clockDomain) { payload =>
      if (m < testNum) {
        instSims(m).driveSim(payload)
        m += 1
        true
      } else {
        false
      }
    }.setFactor(instDriveSpeed)

    var i, j, k = 0
    StreamReadyRandomizer(dut.io.slicedInst, dut.clockDomain).setFactor(slicedInstReceiveSpeed)
    dut.io.Mats_to_Cores_Streams.foreach(StreamReadyRandomizer(_, dut.clockDomain).setFactor(dataReceiveSpeed))
    dut.io.Mats_to_Cores_Streams.foreach(StreamMonitor(_, dut.clockDomain) { payload =>
      val instSim = instSims(i)
      val matColSliceNum = instSim.input1Shape1 * slicerCfg.systolicArraySideNum
      val matA = matAs(i)
      val matB = matBs(i)
      val testRef = testRefs(i)(j)(k)
      val finalRef = k == testRefs(i)(j).length - 1
      val outputAResult = payload.A.toArrayInt
      val outputBResult = payload.B.toArrayInt
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

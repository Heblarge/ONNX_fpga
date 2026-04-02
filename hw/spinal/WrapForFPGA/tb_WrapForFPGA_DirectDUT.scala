package WrapForFPGA

import Accelerator._
import Tiling._
import Interface._
import DataPump._
import Util._

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axilite._
import spinal.lib.sim._
import spinal.sim.VCSFlags
import java.io.File

import scala.math._
import scala.util.Random
import scala.collection.mutable.ArrayBuffer

// =============================================================================
// WrapForFPGA 直接作为顶层 DUT 的测试
// 内存行为通过软件仿真（同步读延迟+写捕获）
// 解决了嵌套组件中 ClockDomain.external 无法正确 forkStimulus 的问题
// =============================================================================
object WrapForFPGA_DirectDUTTb extends App {
  val period = 10
  val errRateLimit = 0.01
  val zeroLimit = 10
  val seed = 114514 + 1000
  val random = new Random(seed)
  val testNum = 50
  val sideNum = 8
  val elementWidth = 24
  val intWidth = 12
  val fracWidth = elementWidth - intWidth
  val memElementWidth = 32
  val memDataWidth = sideNum * memElementWidth // 256 bits

  val fpgaCfg = FPGACfg(
    UIDWidth = 19,
    AddressWidth = 20,
    ShapeWidth = 16,
    systolicArraySideNum = sideNum,
    elementWidth = elementWidth,
    intWidth = intWidth,
    systolicArrayInFifoDepth = 64,
    systolicArrayOutFifoDepth = 16,
    systolicArrayInstFifoDepth = 32,
    activationOutFifoDepth = 32,
    slicedInstFifoDepth = 2,
    numCores = 1
  )

  val acceleratorCfg = AcceleratorCfg(
    UIDWidth = 19,
    AddressWidth = 20,
    ShapeWidth = 16,
    elementWidth = elementWidth,
    intWidth = intWidth,
    systolicArraySideNum = sideNum,
    systolicArrayInFifoDepth = 64,
    systolicArrayOutFifoDepth = 16,
    systolicArrayInstFifoDepth = 32,
    activationOutFifoDepth = 32,
    slicedInstFifoDepth = 2,
    numCores = 1,
    memElementWidth = memElementWidth
  )

  val path = s"simWorkspace/WrapForFPGA_DirectDUTTb"
  new File(path).mkdirs()

  val compiled = SimConfig.workspacePath(path).withFsdbWave
    .withConfig(SpinalConfig(bitVectorWidthMax = 100000))
    .allOptimisation
    .withVCS(VCSFlags(
        compileFlags = List("-kdb", "-lca", "+notimingchecks"),
        elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
        runFlags = List("-l ./run.log")
    ))
    .compile {
      val dut = WrapForFPGA(fpgaCfg)
      dut.io.memPortA.Data.simPublic()
      dut.io.memPortB.Data.simPublic()
      dut
    }

  // =========================================================================
  // 生成测试数据
  // =========================================================================
  def genInstSim(random: Random): InstSim = {
    val matSubRowNum = sideNum
    val uid = random.nextInt(1000)
    var matOp = random.nextSpinalEnum(MatrixOperation_TypeDef)
    var activationFunc = random.nextSpinalEnum(Activation_TypeDef)
    val shiftAfterMat = random.between(-2, 3)
    val doTranspose = random.nextBoolean()
    val shiftAfterAct = random.between(-2, 3)
    val shiftA = 0
    val shiftB = 0

    val shape0_0 = random.between(1, 10) * matSubRowNum
    var shape0_1 = random.between(1, 10) * matSubRowNum
    val shape1_1 = random.between(1, 10) * matSubRowNum

    if (activationFunc == Activation_TypeDef.Log) {
      matOp = MatrixOperation_TypeDef.ElementAdd
    }

    val inst = new InstSim(
      UID = uid,
      matrixOperation = matOp,
      shiftLeft_AfterMatrixOperation = if (activationFunc == Activation_TypeDef.Log) 0 else shiftAfterMat,
      doTranspose = doTranspose,
      activationFunction = activationFunc,
      shiftLeft_AfterActivation = shiftAfterAct,
      input0Address = 0,
      input1Address = 0,
      outputAddress = 0,
      input0Shape0 = shape0_0,
      input0Shape1 = shape0_1,
      input1Shape1 = shape1_1,
      shiftLeft_A = shiftA,
      shiftLeft_B = shiftB
    )
    inst.computeShape()
    inst
  }

  def genMat(instSim: InstSim): (Array[Array[Int]], Array[Array[Int]]) = {
    if (instSim.activationFunction == Activation_TypeDef.Log) {
      val one = pow(2, fracWidth).toInt
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

  // 预生成所有测试数据
  val instSims = ArrayBuffer[InstSim]()
  val matAs = ArrayBuffer[Array[Array[Int]]]()
  val matBs = ArrayBuffer[Array[Array[Int]]]()
  val matZs = ArrayBuffer[Array[Array[Int]]]()

  for (_ <- 0 until testNum) {
    val instSim = genInstSim(random)
    instSims += instSim
    val (matA, matB) = genMat(instSim)
    matAs += matA
    matBs += matB
    matZs += instSim.acceleratorSim(matA, matB, acceleratorCfg)
  }

  // =========================================================================
  // 仿真主体
  // =========================================================================
  compiled.doSimUntilVoid { dut =>
    SimTimeout(10000000L * period)
    dut.clockDomain.forkStimulus(period)
    dut.clkCore.forkStimulus(4 * period)

    // -----------------------------------------------------------------------
    // 软件模拟内存（每个测试前重新填充）
    // -----------------------------------------------------------------------
    val memSize = 1 << 16 // 65536 words，足够存放最大矩阵
    val memA = Array.fill(memSize)(BigInt(0))
    val memB = Array.fill(memSize)(BigInt(0))
    val memZ = Array.fill(memSize)(BigInt(0))

    // 将矩阵打包到内存数组（8 个 32-bit 元素/word）
    def packMatToMem(mem: Array[BigInt], mat: Array[Array[Int]], shape0: Int, shape1: Int): Unit = {
      val numBlocksX = shape1 / sideNum
      val elemMask = (BigInt(1) << memElementWidth) - 1
      for (row <- 0 until shape0) {
        for (blkCol <- 0 until numBlocksX) {
          val addr = row * numBlocksX + blkCol
          var word = BigInt(0)
          for (colOff <- 0 until sideNum) {
            val globalCol = blkCol * sideNum + colOff
            val elemVal = if (globalCol < mat(row).length) mat(row)(globalCol) else 0
            word |= (BigInt(elemVal) & elemMask) << (colOff * memElementWidth)
          }
          mem(addr) = word
        }
      }
    }

    // 从内存数组解包矩阵
    def unpackMatFromMem(mem: Array[BigInt], shape0: Int, shape1: Int): Array[Array[Int]] = {
      val numBlocksX = shape1 / sideNum
      val elemMask = (BigInt(1) << memElementWidth) - 1
      val signBit = BigInt(1) << (memElementWidth - 1)
      Array.tabulate(shape0, shape1) { (i, j) =>
        val vectorIdx = i * numBlocksX + (j / sideNum)
        val elemIdx = j % sideNum
        val elemBits = (mem(vectorIdx) >> (elemIdx * memElementWidth)) & elemMask
        if ((elemBits & signBit) != 0) {
          (elemBits | ((BigInt(-1) >> memElementWidth) << memElementWidth)).toInt
        } else {
          elemBits.toInt
        }
      }
    }

    // -----------------------------------------------------------------------
    // 内存读端口仿真线程
    // WrapForFPGA 已将 Slicer 的字地址转为字节地址暴露
    // 软件 Mem 数组仍按字索引 → 需右移 byteOffset 还原字索引
    // 模拟同步 SDPRAM 1 拍延迟：Address 在 edge N 有效,
    // 数据在 edge N+1 被 slicer 的寄存器捕获
    // -----------------------------------------------------------------------
    val memByteOffset = log2Up(memDataWidth / 8) // = 5 for 256-bit

    fork {
      while (true) {
        dut.clockDomain.waitSampling()
        val addr = (dut.io.memPortA.Address.toInt >> memByteOffset) & (memSize - 1)
        dut.io.memPortA.Data #= memA(addr)
      }
    }

    fork {
      while (true) {
        dut.clockDomain.waitSampling()
        val addr = (dut.io.memPortB.Address.toInt >> memByteOffset) & (memSize - 1)
        dut.io.memPortB.Data #= memB(addr)
      }
    }

    // -----------------------------------------------------------------------
    // 内存写端口捕获线程
    // Collector 输出字节地址，WrapForFPGA 直接传递，需右移还原字索引
    // -----------------------------------------------------------------------
    fork {
      while (true) {
        dut.clockDomain.waitSampling()
        if (dut.io.memPortZ.Valid.toBoolean) {
          val byteAddr = dut.io.memPortZ.Address.toInt
          val addr = (byteAddr >> memByteOffset) & (memSize - 1)
          val data = dut.io.memPortZ.Data.toBigInt
          memZ(addr) = data
        }
      }
    }

    // -----------------------------------------------------------------------
    // 初始化 AXI 信号
    // -----------------------------------------------------------------------
    val axiInst = dut.io.sAxi4LiteInst
    axiInst.aw.valid #= false
    axiInst.aw.addr  #= 0
    axiInst.w.valid  #= false
    axiInst.w.data   #= 0
    axiInst.w.strb   #= 0
    axiInst.b.ready  #= true
    axiInst.ar.valid #= false
    axiInst.ar.addr  #= 0
    axiInst.r.ready  #= true

    dut.clockDomain.waitSampling(20)

    // -----------------------------------------------------------------------
    // AXI4-Lite 写操作
    // -----------------------------------------------------------------------
    def axiLiteWrite(axi: AxiLite4, addr: Long, data: Long): Unit = {
      axi.aw.valid #= true
      axi.aw.addr  #= addr
      axi.w.valid  #= true
      axi.w.data   #= data
      axi.w.strb   #= 0xF

      var awDone = false
      var wDone  = false

      while (!awDone || !wDone) {
        dut.clockDomain.waitSampling()
        if (!awDone && axi.aw.ready.toBoolean) {
          awDone = true
          axi.aw.valid #= false
        }
        if (!wDone && axi.w.ready.toBoolean) {
          wDone = true
          axi.w.valid #= false
        }
      }
      while (!axi.b.valid.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
    }

    // -----------------------------------------------------------------------
    // 发送指令
    // -----------------------------------------------------------------------
    def sendInstruction(instSim: InstSim): Unit = {
      val ShiftWidth = fpgaCfg.slicerCfg.ShiftWidth
      val ShapeWidth = fpgaCfg.ShapeWidth
      val UIDWidth = fpgaCfg.UIDWidth

      var bits = BigInt(0)
      var offset = 0

      def appendField(value: BigInt, width: Int): Unit = {
        val mask = (BigInt(1) << width) - 1
        bits |= (value & mask) << offset
        offset += width
      }

      // SpinalHDL Bundle asBits: 第一个声明的字段在 LSB
      appendField(BigInt(instSim.UID), UIDWidth)
      appendField(BigInt(instSim.matrixOperation.position), 2)
      appendField(BigInt(instSim.shiftLeft_AfterMatrixOperation) & ((BigInt(1) << ShiftWidth) - 1), ShiftWidth)
      appendField(if (instSim.doTranspose) BigInt(1) else BigInt(0), 1)
      appendField(BigInt(instSim.activationFunction.position), 3)
      appendField(BigInt(instSim.shiftLeft_AfterActivation) & ((BigInt(1) << ShiftWidth) - 1), ShiftWidth)
      appendField(BigInt(instSim.input0Shape0), ShapeWidth)
      appendField(BigInt(instSim.input0Shape1), ShapeWidth)
      appendField(BigInt(instSim.input1Shape0), ShapeWidth)
      appendField(BigInt(instSim.input1Shape1), ShapeWidth)
      appendField(BigInt(instSim.shiftLeft_A) & ((BigInt(1) << ShiftWidth) - 1), ShiftWidth)
      appendField(BigInt(instSim.shiftLeft_B) & ((BigInt(1) << ShiftWidth) - 1), ShiftWidth)

      val mask32 = (BigInt(1) << 32) - 1
      val dword0 = (bits >>  0) & mask32
      val dword1 = (bits >> 32) & mask32
      val dword2 = (bits >> 64) & mask32
      val dword3 = (bits >> 96) & mask32

      // SLICE0(first declared) at LSB → 0x00, SLICE3(last) at MSB → 0x0C
      axiLiteWrite(axiInst, 0x00, dword0.toLong)
      axiLiteWrite(axiInst, 0x04, dword1.toLong)
      axiLiteWrite(axiInst, 0x08, dword2.toLong)
      axiLiteWrite(axiInst, 0x0C, dword3.toLong)
      axiLiteWrite(axiInst, 0x10, 0x1) // fire
    }

    // -----------------------------------------------------------------------
    // 主测试循环
    // -----------------------------------------------------------------------
    println(s"[Progress] Start WrapForFPGA DirectDUT test with seed $seed")

    var startCycle = 0L
    var cycleCount = 0L

    fork {
      while (true) {
        dut.clockDomain.waitSampling()
        cycleCount += 1
      }
    }

    for (testIdx <- 0 until testNum) {
      val instSim = instSims(testIdx)
      val matA = matAs(testIdx)
      val matB = matBs(testIdx)
      val matZRef = matZs(testIdx)

      if (testIdx == 0) startCycle = cycleCount

      // Step 1: 填充内存
      packMatToMem(memA, matA, instSim.input0Shape0, instSim.input0Shape1)
      packMatToMem(memB, matB, instSim.input1Shape0, instSim.input1Shape1)
      // 清空输出内存
      for (i <- memZ.indices) memZ(i) = BigInt(0)

      // Step 2: 发送指令
      println(s"[Test $testIdx] ${instSim.matrixOperation}, shapes: (${instSim.input0Shape0}x${instSim.input0Shape1}) x (${instSim.input1Shape0}x${instSim.input1Shape1}) -> (${instSim.outputShape0}x${instSim.outputShape1})")
      sendInstruction(instSim)

      // Step 3: 等待 writeSwitch（collector 完成）
      var waitCycles = 0
      val maxWait = 100000
      while (!dut.io.writeSwitch.toBoolean && waitCycles < maxWait) {
        dut.clockDomain.waitSampling()
        waitCycles += 1
        if (waitCycles % 10000 == 0) {
          println(s"[Test $testIdx] Waiting... cycle=$waitCycles")
        }
      }
      assert(waitCycles < maxWait, s"Test $testIdx: Timeout waiting for writeSwitch")

      // 等待写操作完成
      dut.clockDomain.waitSampling(10)

      // Step 4: 验证结果
      val matZResult = unpackMatFromMem(memZ, instSim.outputShape0, instSim.outputShape1)

      matZipForeach(matZRef, matZResult) { (zRef, zResult, i, j) =>
        val err = abs(zRef - zResult)
        val errRate = abs((zRef - zResult).toDouble / (if (zRef != 0) zRef.toDouble else 1.0))
        assert(
          if (errRate < errRateLimit) true else abs(zRef) < zeroLimit && abs(zResult) < zeroLimit,
          s"test $testIdx: output mismatch at ($i, $j), zRef=$zRef (0x${zRef.toHexString}), " +
            s"zResult=$zResult (0x${zResult.toHexString}), err=$err, errRate=$errRate\n$instSim"
        )
      }

      println(s"test $testIdx pass")
    }

    val totalCycles = cycleCount - startCycle
    val cyclesPerTest = totalCycles.toDouble / testNum
    println("TEST PASS".green)
    println(s"Total cycles: $totalCycles, Cycles/test: $cyclesPerTest")

    simSuccess()
  }
}

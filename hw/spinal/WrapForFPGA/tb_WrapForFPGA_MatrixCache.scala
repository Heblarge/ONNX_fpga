package WrapForFPGA

import Accelerator._
import Tiling._
import Interface._
import DataPump._
import MatrixCacheInterface._
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
// 顶层测试包装：WrapForFPGA + MatrixCache（含内部 SdpramModel）
// =============================================================================
case class WrapForFPGA_MatrixCacheTest(fpgaCfg: FPGACfg, cacheAddrWidth: Int) extends Component {
  val memDataWidth = fpgaCfg.systolicArraySideNum * 32 // 256 bits

  // 不传 externalClkCore，让 WrapForFPGA 内部自己创建 ClockDomain.external
  // 这和 DirectDUT TB 的工作模式一致
  val core = WrapForFPGA(fpgaCfg)
  val matrixCache = MatrixCache(addrWidth = cacheAddrWidth, dataWidth = memDataWidth)

  val io = new Bundle {
    // AXI4-Lite 指令接口
    val sAxi4LiteInst = slave(AxiLite4(fpgaCfg.axi4LiteInstCfg.getAxiConfig))
    // AXI4-Lite 缓存配置接口
    val sAxi4LiteCache = slave(AxiLite4(AxiLite4Config(addressWidth = 8, dataWidth = 32)))

    // DMA 完成信号（TB 驱动）
    val dmaDoneA = in Bool()
    val dmaDoneB = in Bool()
    val dmaDoneC = in Bool()

    // 状态输出
    val globalIntr = out Bool()
  }

  // ===== 指令接口 =====
  io.sAxi4LiteInst <> core.io.sAxi4LiteInst

  // ===== 缓存配置接口 =====
  io.sAxi4LiteCache <> matrixCache.io.axi

  // ===== 核心 → 缓存 读端口（WrapForFPGA 已将 Slicer 字地址转为字节地址） =====
  // memPortA: master(clk,rst,Valid,Address out; Data in)
  // readA: slave(clk,rst,Valid,Address in; Data out)
  matrixCache.io.readA.clk     := core.io.memPortA.clk
  matrixCache.io.readA.rst     := core.io.memPortA.rst
  matrixCache.io.readA.Valid   := core.io.memPortA.Valid
  matrixCache.io.readA.Address := core.io.memPortA.Address.resized
  core.io.memPortA.Data        := matrixCache.io.readA.Data

  matrixCache.io.readB.clk     := core.io.memPortB.clk
  matrixCache.io.readB.rst     := core.io.memPortB.rst
  matrixCache.io.readB.Valid   := core.io.memPortB.Valid
  matrixCache.io.readB.Address := core.io.memPortB.Address.resized
  core.io.memPortB.Data        := matrixCache.io.readB.Data

  // ===== 核心 → 缓存 写端口（Collector 已输出字节地址，WrapForFPGA 直接传递） =====
  matrixCache.io.writeC.clk     := core.io.memPortZ.clk
  matrixCache.io.writeC.rst     := core.io.memPortZ.rst
  matrixCache.io.writeC.Valid   := core.io.memPortZ.Valid
  matrixCache.io.writeC.Address := core.io.memPortZ.Address.resized
  matrixCache.io.writeC.Data    := core.io.memPortZ.Data
  matrixCache.io.writeC.Wen     := core.io.memPortZ.Wen

  // ===== 缓存切换信号 =====
  matrixCache.io.switchA := core.io.readSwitch
  matrixCache.io.switchB := core.io.readSwitch
  matrixCache.io.switchC := core.io.writeSwitch

  // ===== DMA 完成信号 =====
  matrixCache.io.dmaDoneA := io.dmaDoneA
  matrixCache.io.dmaDoneB := io.dmaDoneB
  matrixCache.io.dmaDoneC := io.dmaDoneC

  // ===== 中断输出 =====
  io.globalIntr := matrixCache.io.globalIntr

  // ===== DMA 写入端口默认不使用（TB 直接访问底层 mem） =====
  matrixCache.io.writeA.Valid   := False
  matrixCache.io.writeA.Address := 0
  matrixCache.io.writeA.Data    := 0
  matrixCache.io.writeA.Wen     := 0
  matrixCache.io.writeA.clk     := ClockDomain.current.readClockWire
  matrixCache.io.writeA.rst     := ClockDomain.current.readResetWire

  matrixCache.io.writeB.Valid   := False
  matrixCache.io.writeB.Address := 0
  matrixCache.io.writeB.Data    := 0
  matrixCache.io.writeB.Wen     := 0
  matrixCache.io.writeB.clk     := ClockDomain.current.readClockWire
  matrixCache.io.writeB.rst     := ClockDomain.current.readResetWire

  // ===== DMA 读出端口默认不使用（TB 直接访问底层 mem） =====
  matrixCache.io.readC.Valid   := False
  matrixCache.io.readC.Address := 0
  matrixCache.io.readC.clk     := ClockDomain.current.readClockWire
  matrixCache.io.readC.rst     := ClockDomain.current.readResetWire
}


// =============================================================================
// 主测试入口
// =============================================================================
object WrapForFPGA_MatrixCacheTb extends App {
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

  // 缓存地址宽度：需要足够放下单个测试的矩阵（最大约 80x80 / 8 = 800 words）
  // 使用 12 位地址 = 4096 words，足够
  val cacheAddrWidth = 12

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

  // AcceleratorCfg 用于 golden model 计算
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

  val path = s"simWorkspace/WrapForFPGA_MatrixCacheTb"
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
      val dut = WrapForFPGA_MatrixCacheTest(fpgaCfg, cacheAddrWidth)
      // 暴露内部存储和控制信号用于仿真
      dut.matrixCache.cacheA.sdpramModel.mem.simPublic()
      dut.matrixCache.cacheB.sdpramModel.mem.simPublic()
      dut.matrixCache.cacheC.sdpramModel.mem.simPublic()
      dut.matrixCache.cacheA.inputMatrixCacheInterface.wrPtr.simPublic()
      dut.matrixCache.cacheA.inputMatrixCacheInterface.rdPtr.simPublic()
      dut.matrixCache.cacheA.inputMatrixCacheInterface.bankValid.simPublic()
      dut.matrixCache.cacheB.inputMatrixCacheInterface.wrPtr.simPublic()
      dut.matrixCache.cacheB.inputMatrixCacheInterface.rdPtr.simPublic()
      dut.matrixCache.cacheB.inputMatrixCacheInterface.bankValid.simPublic()
      dut.matrixCache.cacheC.matrixCacheInterface.wrPtr.simPublic()
      dut.matrixCache.cacheC.matrixCacheInterface.rdPtr.simPublic()
      dut.matrixCache.cacheC.matrixCacheInterface.bankValid.simPublic()
      // 暴露核心调试信号
      dut.core.slicer.slicer.io.matAfterSlicers(0).valid.simPublic()
      dut.core.slicer.slicer.io.matAfterSlicers(0).ready.simPublic()
      dut.core.slicer.slicer.io.inst.ready.simPublic()
      dut.core.instBridge.io.m_stream.valid.simPublic()
      dut.core.instBridge.io.m_stream.ready.simPublic()
      dut.core.collector.io.instFinish.simPublic()
      dut.core.io.readSwitch.simPublic()
      dut.core.io.writeSwitch.simPublic()
      val wrapper0 = dut.core.systolicArrayWrappers(0)
      wrapper0.SystolicArray2D_CC_inst.io.out_Mats.valid.simPublic()
      wrapper0.SystolicArray2D_CC_inst.io.out_Mats.ready.simPublic()
      wrapper0.SystolicArray2D_CC_inst.in_Mats_CC_logic.Fifo.io.pushOccupancy.simPublic()
      wrapper0.SystolicArray2D_CC_inst.in_Mats_CC_logic.Fifo.io.popOccupancy.simPublic()
      val act0 = dut.core.activations(0)
      act0.busyOut.simPublic()
      act0.io.in_Mats.ready.simPublic()
      act0.io.out_Mats.valid.simPublic()
      act0.io.out_Mats.ready.simPublic()
      dut
    }

  // =========================================================================
  // 生成测试数据
  // =========================================================================
  // 为 WrapForFPGA 场景生成指令：地址始终从 0 开始（cache 内部空间）
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
    // 确保形状一致性（ElementAdd/Mul/Max 强制 input0Shape1 = input1Shape1）
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
    // 和 DirectDUT TB 一致：让 WrapForFPGA 内部的 ClockDomain.external 被驱动
    dut.core.clkCore.forkStimulus(4 * period)

    // -----------------------------------------------------------------------
    // 初始化所有输入信号
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

    val axiCache = dut.io.sAxi4LiteCache
    axiCache.aw.valid #= false
    axiCache.aw.addr  #= 0
    axiCache.w.valid  #= false
    axiCache.w.data   #= 0
    axiCache.w.strb   #= 0
    axiCache.b.ready  #= true
    axiCache.ar.valid #= false
    axiCache.ar.addr  #= 0
    axiCache.r.ready  #= true

    dut.io.dmaDoneA #= false
    dut.io.dmaDoneB #= false
    dut.io.dmaDoneC #= false

    dut.clockDomain.waitSampling(20)

    // -----------------------------------------------------------------------
    // AXI4-Lite 写操作辅助函数
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
      // 等待写响应
      while (!axi.b.valid.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
    }

    def axiLiteRead(axi: AxiLite4, addr: Long): Long = {
      axi.ar.valid #= true
      axi.ar.addr  #= addr

      while (!axi.ar.ready.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
      axi.ar.valid #= false

      while (!axi.r.valid.toBoolean) dut.clockDomain.waitSampling()
      val data = axi.r.data.toLong
      dut.clockDomain.waitSampling()
      data
    }

    // -----------------------------------------------------------------------
    // DMA 模拟：直接写入缓存底层 SdpramModel 的 Mem
    // 写入到 wrPtr 指向的 bank
    // -----------------------------------------------------------------------
    def dmaWriteToCache(
        sdpramMem: Mem[Bits],
        wrPtr: UInt,
        mat: Array[Array[Int]],
        shape0: Int,
        shape1: Int
    ): Unit = {
      val numBlocksX = shape1 / sideNum
      val elemMask = (BigInt(1) << memElementWidth) - 1
      val bankOffset = wrPtr.toInt * (1 << cacheAddrWidth)

      for (row <- 0 until shape0) {
        for (blkCol <- 0 until numBlocksX) {
          val addr = row * numBlocksX + blkCol
          var rowBits = BigInt(0)
          for (colOffset <- 0 until sideNum) {
            val globalCol = blkCol * sideNum + colOffset
            val elementVal = if (globalCol < mat(row).length) mat(row)(globalCol) else 0
            rowBits |= (BigInt(elementVal) & elemMask) << (colOffset * memElementWidth)
          }
          sdpramMem.setBigInt(bankOffset + addr, rowBits)
        }
      }
    }

    // -----------------------------------------------------------------------
    // DMA 模拟：直接从缓存底层 SdpramModel 的 Mem 读取结果
    // 从 rdPtr 指向的 bank 读取
    // -----------------------------------------------------------------------
    def dmaReadFromCache(
        sdpramMem: Mem[Bits],
        rdPtr: UInt,
        shape0: Int,
        shape1: Int
    ): Array[Array[Int]] = {
      val numBlocksX = shape1 / sideNum
      val elemMask = (BigInt(1) << memElementWidth) - 1
      val signBit = BigInt(1) << (memElementWidth - 1)
      val bankOffset = rdPtr.toInt * (1 << cacheAddrWidth)
      val totalVectors = shape0 * numBlocksX

      val rawMem = Array.tabulate(totalVectors) { i =>
        sdpramMem.getBigInt(bankOffset + i)
      }

      Array.tabulate(shape0, shape1) { (i, j) =>
        val vectorIdx = i * numBlocksX + (j / sideNum)
        val elemIdx = j % sideNum
        val vectorVal = rawMem(vectorIdx)
        val elemBits = (vectorVal >> (elemIdx * memElementWidth)) & elemMask
        if ((elemBits & signBit) != 0) {
          (elemBits | ((BigInt(-1) >> memElementWidth) << memElementWidth)).toInt
        } else {
          elemBits.toInt
        }
      }
    }

    // -----------------------------------------------------------------------
    // 发送指令：将 InstSim 编码为 128-bit 并通过 AXI-Lite 写入
    // -----------------------------------------------------------------------
    def sendInstruction(instSim: InstSim): Unit = {
      // 构造 ComputeInstruction_Simplified_TypeDef 的 bit 表示
      // SpinalHDL Bundle asBits / assignFromBits: 第一个声明的字段在 LSB!
      // (Cat(Seq(a,b,c)) 把 a 放 LSB)
      // 字段顺序（LSB→MSB，按声明顺序）:
      //   UID, matrixOperation, shiftLeft_AfterMatrixOperation, doTranspose,
      //   activationFunction, shiftLeft_AfterActivation,
      //   input0Shape(0,1), input1Shape(0,1), shiftLeft_A, shiftLeft_B
      // Vec(2) 也是 element(0) 在低位、element(1) 在高位

      val ShiftWidth = fpgaCfg.slicerCfg.ShiftWidth
      val AddressWidth = fpgaCfg.AddressWidth
      val ShapeWidth = fpgaCfg.ShapeWidth
      val UIDWidth = fpgaCfg.UIDWidth

      var bits = BigInt(0)
      var offset = 0

      def appendField(value: BigInt, width: Int): Unit = {
        val mask = (BigInt(1) << width) - 1
        bits |= (value & mask) << offset
        offset += width
      }

      // 按 Bundle 声明顺序 — 第一个到最后一个，第一个在 LSB
      appendField(BigInt(instSim.UID), UIDWidth)
      appendField(BigInt(instSim.matrixOperation.position), 2)
      appendField(BigInt(instSim.shiftLeft_AfterMatrixOperation) & ((BigInt(1) << ShiftWidth) - 1), ShiftWidth)
      appendField(if (instSim.doTranspose) BigInt(1) else BigInt(0), 1)
      appendField(BigInt(instSim.activationFunction.position), 3)
      appendField(BigInt(instSim.shiftLeft_AfterActivation) & ((BigInt(1) << ShiftWidth) - 1), ShiftWidth)
      // input0Shape: Vec(2) - element(0) 在低位
      appendField(BigInt(instSim.input0Shape0), ShapeWidth)
      appendField(BigInt(instSim.input0Shape1), ShapeWidth)
      // input1Shape: Vec(2)
      appendField(BigInt(instSim.input1Shape0), ShapeWidth)
      appendField(BigInt(instSim.input1Shape1), ShapeWidth)
      appendField(BigInt(instSim.shiftLeft_A) & ((BigInt(1) << ShiftWidth) - 1), ShiftWidth)
      appendField(BigInt(instSim.shiftLeft_B) & ((BigInt(1) << ShiftWidth) - 1), ShiftWidth)

      println(s"[DEBUG] Encoded bits ($offset bits) = 0x${bits.toString(16)}")

      // 拆分为 4 个 32-bit DWORD
      val mask32 = (BigInt(1) << 32) - 1
      val dword0 = (bits >>  0) & mask32
      val dword1 = (bits >> 32) & mask32
      val dword2 = (bits >> 64) & mask32
      val dword3 = (bits >> 96) & mask32

      println(s"[DEBUG] dwords: 0x${dword3.toString(16)}_${dword2.toString(16)}_${dword1.toString(16)}_${dword0.toString(16)}")

      // 写入 AXI-Lite 指令寄存器
      // Instruction128 Bundle 的 asBits: SLICE0(first declared, LSB), SLICE3(last declared, MSB)
      // AXI 映射: 0x00→SLICE0, 0x0C→SLICE3
      // 所以低位 dword 写到低地址
      axiLiteWrite(axiInst, 0x00, dword0.toLong)  // bits[31:0]  → SLICE0
      axiLiteWrite(axiInst, 0x04, dword1.toLong)  // bits[63:32] → SLICE1
      axiLiteWrite(axiInst, 0x08, dword2.toLong)  // bits[95:64] → SLICE2
      axiLiteWrite(axiInst, 0x0C, dword3.toLong)  // bits[127:96]→ SLICE3
      // 触发 fire
      axiLiteWrite(axiInst, 0x10, 0x1)
    }

    // -----------------------------------------------------------------------
    // 脉冲信号辅助
    // -----------------------------------------------------------------------
    def pulseDmaDone(signal: Bool): Unit = {
      signal #= true
      dut.clockDomain.waitSampling(2)
      signal #= false
      dut.clockDomain.waitSampling()
    }

    // -----------------------------------------------------------------------
    // 配置缓存生命周期
    // -----------------------------------------------------------------------
    // 每个输入矩阵只使用一次（单条指令）
    axiLiteWrite(axiCache, 0x00, 1L) // Cache A lifecycle = 1
    axiLiteWrite(axiCache, 0x04, 1L) // Cache B lifecycle = 1

    // -----------------------------------------------------------------------
    // 主测试循环
    // -----------------------------------------------------------------------
    println(s"[Progress] Start WrapForFPGA + MatrixCache test simulation with seed $seed")

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

      // --- Step 1: 写入矩阵 A 到 cacheA 的当前写 bank ---
      dmaWriteToCache(
        dut.matrixCache.cacheA.sdpramModel.mem,
        dut.matrixCache.cacheA.inputMatrixCacheInterface.wrPtr,
        matA, instSim.input0Shape0, instSim.input0Shape1
      )
      pulseDmaDone(dut.io.dmaDoneA)

      // --- Step 2: 写入矩阵 B 到 cacheB 的当前写 bank ---
      dmaWriteToCache(
        dut.matrixCache.cacheB.sdpramModel.mem,
        dut.matrixCache.cacheB.inputMatrixCacheInterface.wrPtr,
        matB, instSim.input1Shape0, instSim.input1Shape1
      )
      pulseDmaDone(dut.io.dmaDoneB)

      // --- Step 3: 发送指令 ---
      println(s"[Test $testIdx] Sending instruction: ${instSim.matrixOperation}, shapes: (${instSim.input0Shape0}x${instSim.input0Shape1}) x (${instSim.input1Shape0}x${instSim.input1Shape1}) -> (${instSim.outputShape0}x${instSim.outputShape1})")
      sendInstruction(instSim)

      // --- Step 4: 等待输出完成（globalIntr 拉高） ---
      var waitCycles = 0
      val maxWait = 100000
      while (!dut.io.globalIntr.toBoolean && waitCycles < maxWait) {
        dut.clockDomain.waitSampling()
        waitCycles += 1
        if (waitCycles % 2000 == 0) {
          val w0 = dut.core.systolicArrayWrappers(0)
          val a0 = dut.core.activations(0)
          println(s"[Test $testIdx] cycle=$waitCycles")
          println(s"  slicer: inst_ready=${dut.core.slicer.slicer.io.inst.ready.toBoolean}, matOut valid=${dut.core.slicer.slicer.io.matAfterSlicers(0).valid.toBoolean}, ready=${dut.core.slicer.slicer.io.matAfterSlicers(0).ready.toBoolean}")
          println(s"  CC: out_valid=${w0.SystolicArray2D_CC_inst.io.out_Mats.valid.toBoolean}, out_ready=${w0.SystolicArray2D_CC_inst.io.out_Mats.ready.toBoolean}, inFIFO push/pop=${w0.SystolicArray2D_CC_inst.in_Mats_CC_logic.Fifo.io.pushOccupancy.toInt}/${w0.SystolicArray2D_CC_inst.in_Mats_CC_logic.Fifo.io.popOccupancy.toInt}")
          println(s"  activation: busyOut=${a0.busyOut.toBoolean}, in_ready=${a0.io.in_Mats.ready.toBoolean}, out_valid=${a0.io.out_Mats.valid.toBoolean}, out_ready=${a0.io.out_Mats.ready.toBoolean}")
          println(s"  instBridge: valid=${dut.core.instBridge.io.m_stream.valid.toBoolean}, ready=${dut.core.instBridge.io.m_stream.ready.toBoolean}")
          println(s"  cacheA bankValid=[${dut.matrixCache.cacheA.inputMatrixCacheInterface.bankValid(0).toBoolean},${dut.matrixCache.cacheA.inputMatrixCacheInterface.bankValid(1).toBoolean}] rdPtr=${dut.matrixCache.cacheA.inputMatrixCacheInterface.rdPtr.toInt}")
          println(s"  cacheB bankValid=[${dut.matrixCache.cacheB.inputMatrixCacheInterface.bankValid(0).toBoolean},${dut.matrixCache.cacheB.inputMatrixCacheInterface.bankValid(1).toBoolean}] rdPtr=${dut.matrixCache.cacheB.inputMatrixCacheInterface.rdPtr.toInt}")
          println(s"  cacheC bankValid=[${dut.matrixCache.cacheC.matrixCacheInterface.bankValid(0).toBoolean},${dut.matrixCache.cacheC.matrixCacheInterface.bankValid(1).toBoolean}] wrPtr=${dut.matrixCache.cacheC.matrixCacheInterface.wrPtr.toInt}")
          println(s"  readSwitch=${dut.core.io.readSwitch.toBoolean}, writeSwitch=${dut.core.io.writeSwitch.toBoolean}")
        }
      }
      assert(waitCycles < maxWait, s"Test $testIdx: Timeout waiting for output interrupt")

      // 等几拍确保数据写入完成
      dut.clockDomain.waitSampling(5)

      // --- Step 5: 读取并验证结果 ---
      val matZResult = dmaReadFromCache(
        dut.matrixCache.cacheC.sdpramModel.mem,
        dut.matrixCache.cacheC.matrixCacheInterface.rdPtr,
        instSim.outputShape0, instSim.outputShape1
      )

      matZipForeach(matZRef, matZResult) { (zRef, zResult, i, j) =>
        val err = abs(zRef - zResult)
        val errRate = abs((zRef - zResult).toDouble / (if (zRef != 0) zRef.toDouble else 1.0))
        assert(
          if (errRate < errRateLimit) true else abs(zRef) < zeroLimit && abs(zResult) < zeroLimit,
          s"test $testIdx: output mismatch at ($i, $j), zRef=$zRef (0x${zRef.toHexString}), " +
            s"zResult=$zResult (0x${zResult.toHexString}), err=$err, errRate=$errRate\n$instSim"
        )
      }

      // --- Step 6: DMA 读完，清除中断并释放输出 bank ---
      // 清除中断 (写 0x0C bit 0)
      axiLiteWrite(axiCache, 0x0C, 0x1)
      // DMA 已取走数据
      pulseDmaDone(dut.io.dmaDoneC)

      println(s"test $testIdx pass")
    }

    val totalCycles = cycleCount - startCycle
    val cyclesPerTest = totalCycles.toDouble / testNum
    println("TEST PASS".green)
    println(s"Total cycles: $totalCycles, Cycles/test: $cyclesPerTest")

    simSuccess()
  }
}

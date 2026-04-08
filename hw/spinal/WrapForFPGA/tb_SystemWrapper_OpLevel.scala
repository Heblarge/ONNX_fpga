package WrapForFPGA

import Accelerator._
import Tiling._
import Interface._
import DataPump._
import MatrixCacheInterface._
import DMA._
import Util._

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.amba4.axilite._

import java.io.File
import scala.math._
import scala.util.Random

// =============================================================================
// 系统级算子测试平台（Verilator + ramulator2 DPI）
//
// 完整数据通路：Host→DDR→DMA_A/B→Cache→Core→Cache→DMA_Z→DDR→Host
// 逐算子/逐激活函数测试，用于：
//   1. 验证 SystemWrapper 整合正确性
//   2. 测量每种算子的硬件执行周期
//   3. 确定节点级调度硬件的软件策略
// =============================================================================
object SystemWrapper_OpLevelTb extends App {

  // =========================================================================
  // 全局参数
  // =========================================================================
  val period     = 10
  val seed       = 42
  val random     = new Random(seed)
  val sideNum    = 8
  val elementWidth = 24
  val intWidth   = 12
  val fracWidth  = elementWidth - intWidth
  val memElementWidth = 32
  val bytesPerBeat = sideNum * memElementWidth / 8  // 32 bytes
  val byteOffset   = log2Up(bytesPerBeat)           // 5

  val cacheAddrWidth = 10  // 1024 words/bank，足够容纳测试矩阵

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

  val ddrAddrWidth = 32   // 可设为 64 以支持 >4GB DDR 地址空间

  val sysCfg = SystemWrapperConfig(
    fpgaCfg        = fpgaCfg,
    cacheAddrWidth = cacheAddrWidth,
    ddrAddrWidth   = ddrAddrWidth,
    dmaMaxBurstLen = 256
  )

  // DMA 寄存器偏移（自动适配地址位宽）
  val dmaCfg = StrideDmaConfig(ddrAddrWidth = ddrAddrWidth)

  // DDR 内布局
  val DDR_BASE_A = 0x00000000L
  val DDR_BASE_B = 0x00100000L
  val DDR_BASE_Z = 0x00200000L

  // =========================================================================
  // 测试用例定义
  // =========================================================================
  case class OpTestCase(
    name: String,
    matOp: MatrixOperation_TypeDef.E,
    activation: Activation_TypeDef.E,
    shiftAfterOp: Int,
    shiftAfterAct: Int,
    doTranspose: Boolean,
    M: Int, K: Int, N: Int,  // A(M×K), B(K×N) for MatMul; A(M×N), B(M×N) for Elementwise
    shiftA: Int, shiftB: Int
  )

  val testCases = Seq(
    // --- 矩阵乘法 ---
    OpTestCase("MatMul_8x8",           MatrixOperation_TypeDef.MatMul, Activation_TypeDef.None, 0, 0, false, 8,  8,  8,  0, 0),
    OpTestCase("MatMul_16x24x16",      MatrixOperation_TypeDef.MatMul, Activation_TypeDef.None, 0, 0, false, 16, 24, 16, 0, 0),
    OpTestCase("MatMul_Transpose",     MatrixOperation_TypeDef.MatMul, Activation_TypeDef.None, 0, 0, true,  8,  8,  8,  0, 0),
    OpTestCase("MatMul_Shift",         MatrixOperation_TypeDef.MatMul, Activation_TypeDef.None, 2, 0, false, 8,  8,  8,  0, 0),
    // --- 矩阵乘法 + 激活函数 ---
    OpTestCase("MatMul_Relu",          MatrixOperation_TypeDef.MatMul, Activation_TypeDef.Relu, 0, 0, false, 8,  8,  8,  0, 0),
    OpTestCase("MatMul_Exp",           MatrixOperation_TypeDef.MatMul, Activation_TypeDef.Exp,  0, 0, false, 8,  8,  8,  0, 0),
    OpTestCase("MatMul_Softplus",      MatrixOperation_TypeDef.MatMul, Activation_TypeDef.Softplus, 0, 0, false, 8, 8, 8, 0, 0),
    // --- 逐元素运算 ---
    OpTestCase("ElementAdd_8x8",       MatrixOperation_TypeDef.ElementAdd, Activation_TypeDef.None, 0, 0, false, 8,  8,  8,  0, 0),
    OpTestCase("ElementMul_8x8",       MatrixOperation_TypeDef.ElementMul, Activation_TypeDef.None, 0, 0, false, 8,  8,  8,  0, 0),
    OpTestCase("ElementMax_8x8",       MatrixOperation_TypeDef.ElementMax, Activation_TypeDef.None, 0, 0, false, 8,  8,  8,  0, 0),
    // --- 逐元素 + 激活 ---
    OpTestCase("ElementAdd_Relu",      MatrixOperation_TypeDef.ElementAdd, Activation_TypeDef.Relu, 0, 0, false, 8,  8,  8,  0, 0),
    OpTestCase("ElementAdd_Exp",       MatrixOperation_TypeDef.ElementAdd, Activation_TypeDef.Exp,  0, 0, false, 8,  8,  8,  0, 0),
    // --- 较大矩阵（需 Slicer 内部分块）---
    OpTestCase("MatMul_24x16x24",      MatrixOperation_TypeDef.MatMul, Activation_TypeDef.None, 0, 0, false, 24, 16, 24, 0, 0),
    OpTestCase("MatMul_24x16x24_Relu", MatrixOperation_TypeDef.MatMul, Activation_TypeDef.Relu, 0, 0, false, 24, 16, 24, 0, 0),
  )

  // =========================================================================
  // 编译 DUT
  // =========================================================================
  val path = s"simWorkspace/SystemWrapper_OpLevelTb"
  new File(path).mkdirs()

  val thirdPartyDir = new File("hw/third_party").getAbsolutePath
  val dpiDir        = new File("hw/third_party/dpi").getAbsolutePath
  val ramulator2Dir = new File("hw/third_party/ramulator2").getAbsolutePath

  val compiled = SimConfig.workspacePath(path)
    .withConfig(SpinalConfig(bitVectorWidthMax = 100000))
    .allOptimisation
    .addRtl(s"$dpiDir/DRAMSimDPIDriver.sv")
    .withVerilator
    .addSimulatorFlag(s"-LDFLAGS -L$dpiDir")
    .addSimulatorFlag(s"-LDFLAGS -L$ramulator2Dir")
    .addSimulatorFlag(s"-LDFLAGS -l:DRAMSimDPIDriverRamulator2.so")
    .addSimulatorFlag(s"-LDFLAGS -lramulator")
    .addSimulatorFlag(s"-LDFLAGS -Wl,-rpath,$dpiDir")
    .addSimulatorFlag(s"-LDFLAGS -Wl,-rpath,$ramulator2Dir")
    .compile {
      val dut = SystemWrapper(sysCfg)
      dut
    }

  // 复制 ramulator_config.yaml 到仿真运行目录 + 项目根目录
  // Verilator 通过 JNI 在 JVM 进程内执行仿真，DPI-C 代码的 CWD = 项目根目录，
  // 因此需要在项目根目录也放置 ramulator_config.yaml（VCS 则在 workspace 目录运行）。
  {
    val ramCfgSrc = new File(s"$thirdPartyDir/ramulator_config.yaml").toPath

    // 1. 仿真 workspace 目录（VCS 兼容）
    val targetDir = new File(s"$path/SystemWrapper")
    targetDir.mkdirs()
    val ramCfgDst = new File(targetDir, "ramulator_config.yaml")
    if (!ramCfgDst.exists()) {
      java.nio.file.Files.copy(ramCfgSrc, ramCfgDst.toPath)
    }
    new File(targetDir, "trace").mkdirs()

    // 2. 项目根目录（Verilator JNI 模式 CWD）
    val rootCfgDst = new File("ramulator_config.yaml")
    if (!rootCfgDst.exists()) {
      java.nio.file.Files.copy(ramCfgSrc, rootCfgDst.toPath)
    }
    new File("trace").mkdirs()
  }

  // =========================================================================
  // 仿真主体
  // =========================================================================
  compiled.doSimUntilVoid { dut =>
    SimTimeout(100000000L * period)
    dut.clockDomain.forkStimulus(period)
    dut.core.clkCore.forkStimulus(4 * period)

    val elemMask = (BigInt(1) << memElementWidth) - 1
    val signBit  = BigInt(1) << (memElementWidth - 1)

    // -----------------------------------------------------------------
    // 初始化所有 AXI 接口
    // -----------------------------------------------------------------
    def initAxiLite(axi: AxiLite4): Unit = {
      axi.aw.valid #= false; axi.aw.addr #= 0
      axi.w.valid #= false; axi.w.data #= 0; axi.w.strb #= 0
      axi.b.ready #= true
      axi.ar.valid #= false; axi.ar.addr #= 0
      axi.r.ready #= true
    }
    initAxiLite(dut.io.sAxi4LiteInst)
    initAxiLite(dut.io.sAxi4LiteCache)
    initAxiLite(dut.io.ctrlDmaA)
    initAxiLite(dut.io.ctrlDmaB)
    initAxiLite(dut.io.ctrlDmaZ)

    // Host AXI4 初始化
    val hostAxi = dut.io.hostAxi
    hostAxi.aw.valid #= false; hostAxi.aw.addr #= 0
    hostAxi.aw.len #= 0; hostAxi.aw.size #= byteOffset
    hostAxi.aw.burst #= 1; hostAxi.aw.id #= 0
    if (hostAxi.aw.config.useLock) hostAxi.aw.lock #= 0
    hostAxi.w.valid #= false; hostAxi.w.data #= 0
    hostAxi.w.strb #= (BigInt(1) << bytesPerBeat) - 1
    hostAxi.w.last #= false
    hostAxi.b.ready #= true
    hostAxi.ar.valid #= false; hostAxi.ar.addr #= 0
    hostAxi.ar.len #= 0; hostAxi.ar.size #= byteOffset
    hostAxi.ar.burst #= 1; hostAxi.ar.id #= 0
    if (hostAxi.ar.config.useLock) hostAxi.ar.lock #= 0
    hostAxi.r.ready #= true

    dut.clockDomain.waitSampling(50)

    // -----------------------------------------------------------------
    // AXI4-Lite 辅助函数
    // -----------------------------------------------------------------
    def axiLiteWrite(axi: AxiLite4, addr: Long, data: Long): Unit = {
      axi.aw.valid #= true; axi.aw.addr #= addr
      axi.w.valid #= true; axi.w.data #= data; axi.w.strb #= 0xF
      var awDone = false; var wDone = false
      while (!awDone || !wDone) {
        dut.clockDomain.waitSampling()
        if (!awDone && axi.aw.ready.toBoolean) { awDone = true; axi.aw.valid #= false }
        if (!wDone && axi.w.ready.toBoolean)   { wDone = true; axi.w.valid #= false }
      }
      while (!axi.b.valid.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
    }

    def axiLiteRead(axi: AxiLite4, addr: Long): Long = {
      axi.ar.valid #= true; axi.ar.addr #= addr
      while (!axi.ar.ready.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
      axi.ar.valid #= false
      while (!axi.r.valid.toBoolean) dut.clockDomain.waitSampling()
      val data = axi.r.data.toLong
      dut.clockDomain.waitSampling()
      data
    }

    // -----------------------------------------------------------------
    // Host AXI4 DDR 读写
    // -----------------------------------------------------------------
    def hostWriteBeat(addr: Long, data: BigInt): Unit = {
      hostAxi.aw.valid #= true; hostAxi.aw.addr #= addr
      hostAxi.aw.len #= 0; hostAxi.aw.size #= byteOffset
      hostAxi.aw.burst #= 1; hostAxi.aw.id #= 0
      hostAxi.w.valid #= true; hostAxi.w.data #= data
      hostAxi.w.strb #= (BigInt(1) << bytesPerBeat) - 1
      hostAxi.w.last #= true

      var awDone = false; var wDone = false
      while (!awDone || !wDone) {
        dut.clockDomain.waitSampling()
        if (!awDone && hostAxi.aw.ready.toBoolean) { awDone = true; hostAxi.aw.valid #= false }
        if (!wDone && hostAxi.w.ready.toBoolean)   { wDone = true; hostAxi.w.valid #= false }
      }
      while (!hostAxi.b.valid.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
    }

    def hostReadBeat(addr: Long): BigInt = {
      hostAxi.ar.valid #= true; hostAxi.ar.addr #= addr
      hostAxi.ar.len #= 0; hostAxi.ar.size #= byteOffset
      hostAxi.ar.burst #= 1; hostAxi.ar.id #= 0
      var arDone = false
      while (!arDone) {
        dut.clockDomain.waitSampling()
        if (hostAxi.ar.ready.toBoolean) { arDone = true; hostAxi.ar.valid #= false }
      }
      while (!hostAxi.r.valid.toBoolean) dut.clockDomain.waitSampling()
      val data = hostAxi.r.data.toBigInt
      dut.clockDomain.waitSampling()
      data
    }

    // -----------------------------------------------------------------
    // 矩阵 ↔ DDR 打包/解包
    // -----------------------------------------------------------------
    def packMatrixRow(mat: Array[Array[Int]], row: Int, beatCol: Int): BigInt = {
      var data = BigInt(0)
      for (elem <- 0 until sideNum) {
        val col = beatCol * sideNum + elem
        val value = if (col < mat(row).length) mat(row)(col) else 0
        data |= (BigInt(value) & elemMask) << (elem * memElementWidth)
      }
      data
    }

    def writeMatrixToDDR(mat: Array[Array[Int]], ddrBase: Long, shape0: Int, shape1: Int): Unit = {
      val beatsPerRow = shape1 / sideNum
      for (row <- 0 until shape0; bc <- 0 until beatsPerRow) {
        val addr = ddrBase + (row * beatsPerRow + bc).toLong * bytesPerBeat
        hostWriteBeat(addr, packMatrixRow(mat, row, bc))
      }
    }

    def readMatrixFromDDR(ddrBase: Long, shape0: Int, shape1: Int): Array[Array[Int]] = {
      val beatsPerRow = shape1 / sideNum
      // 按 beat 读取，每个 beat 包含 sideNum 个元素，避免重复读取同一地址
      val result = Array.ofDim[Int](shape0, shape1)
      for (row <- 0 until shape0; bc <- 0 until beatsPerRow) {
        val addr = ddrBase + (row * beatsPerRow + bc).toLong * bytesPerBeat
        val beat = hostReadBeat(addr)
        for (elem <- 0 until sideNum) {
          val col = bc * sideNum + elem
          if (col < shape1) {
            val bits = (beat >> (elem * memElementWidth)) & elemMask
            result(row)(col) = if ((bits & signBit) != 0)
              (bits | ((BigInt(-1) >> memElementWidth) << memElementWidth)).toInt
            else bits.toInt
          }
        }
      }
      result
    }

    // -----------------------------------------------------------------
    // DMA 控制
    // -----------------------------------------------------------------
    def startDma(axiCtrl: AxiLite4, matBase: Long, localBase: Long,
                 matCols: Int, matRows: Int, tileCols: Int, tileRows: Int,
                 tileColPos: Int, tileRowPos: Int, direction: Int): Unit = {
      // matBase 可能为 64 位，在 32 位 AXI-Lite 总线上需拆为多次写入
      axiLiteWrite(axiCtrl, dmaCfg.REG_MAT_BASE, matBase & 0xFFFFFFFFL)
      if (ddrAddrWidth > 32) {
        axiLiteWrite(axiCtrl, dmaCfg.REG_MAT_BASE + 4, (matBase >>> 32) & 0xFFFFFFFFL)
      }
      axiLiteWrite(axiCtrl, dmaCfg.REG_LOCAL_BASE, localBase)
      axiLiteWrite(axiCtrl, dmaCfg.REG_MAT_COLS, matCols.toLong)
      axiLiteWrite(axiCtrl, dmaCfg.REG_MAT_ROWS, matRows.toLong)
      axiLiteWrite(axiCtrl, dmaCfg.REG_TILE_COLS, tileCols.toLong)
      axiLiteWrite(axiCtrl, dmaCfg.REG_TILE_ROWS, tileRows.toLong)
      axiLiteWrite(axiCtrl, dmaCfg.REG_TILE_COL_POS, tileColPos.toLong)
      axiLiteWrite(axiCtrl, dmaCfg.REG_TILE_ROW_POS, tileRowPos.toLong)
      // direction bit 1, start bit 0
      axiLiteWrite(axiCtrl, dmaCfg.REG_CTRL, ((direction << 1) | 0x1).toLong)
    }

    def waitDmaIntr(intrSignal: Bool, name: String, maxWait: Int = 500000): Unit = {
      var cnt = 0
      while (!intrSignal.toBoolean && cnt < maxWait) {
        dut.clockDomain.waitSampling()
        cnt += 1
      }
      assert(cnt < maxWait, s"DMA $name: Timeout after $cnt cycles!")
    }

    def clearDmaIntr(axiCtrl: AxiLite4): Unit = {
      axiLiteWrite(axiCtrl, dmaCfg.REG_INTR_CLR, 0x1)
    }

    // -----------------------------------------------------------------
    // 指令发送（128 位打包，4 × 32 位 AXI 写）
    // -----------------------------------------------------------------
    def sendInstruction(instSim: InstSim): Unit = {
      val axiInst = dut.io.sAxi4LiteInst
      val ShiftWidth = fpgaCfg.slicerCfg.ShiftWidth
      val ShapeWidth = fpgaCfg.ShapeWidth
      val UIDWidth   = fpgaCfg.UIDWidth
      var bits = BigInt(0); var offset = 0
      def appendField(value: BigInt, width: Int): Unit = {
        bits |= (value & ((BigInt(1) << width) - 1)) << offset; offset += width
      }
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
      axiLiteWrite(axiInst, 0x00, ((bits >>  0) & mask32).toLong)
      axiLiteWrite(axiInst, 0x04, ((bits >> 32) & mask32).toLong)
      axiLiteWrite(axiInst, 0x08, ((bits >> 64) & mask32).toLong)
      axiLiteWrite(axiInst, 0x0C, ((bits >> 96) & mask32).toLong)
      // Trigger
      axiLiteWrite(axiInst, 0x10, 0x1)
    }

    // -----------------------------------------------------------------
    // 验证辅助
    // -----------------------------------------------------------------
    val errRateLimit = 0.01
    val zeroLimit    = 10

    def verifyMatrix(golden: Array[Array[Int]], hwResult: Array[Array[Int]],
                     caseName: String): Boolean = {
      var mismatch = 0
      for (i <- golden.indices; j <- golden(i).indices) {
        val zRef = golden(i)(j)
        val zHw  = hwResult(i)(j)
        val err  = abs(zRef - zHw)
        val errRate = abs((zRef - zHw).toDouble / (if (zRef != 0) zRef.toDouble else 1.0))
        val pass = if (errRate < errRateLimit) true
                   else abs(zRef) < zeroLimit && abs(zHw) < zeroLimit
        if (!pass) {
          if (mismatch < 5) {
            println(s"  [$caseName] MISMATCH at ($i,$j): ref=$zRef (0x${zRef.toHexString}), " +
              s"hw=$zHw (0x${zHw.toHexString}), err=$err, rate=$errRate")
          }
          mismatch += 1
        }
      }
      if (mismatch > 0) {
        println(s"  [$caseName] FAIL: $mismatch mismatches out of ${golden.length * golden(0).length} elements")
        false
      } else true
    }

    // =================================================================
    // 主测试循环
    // =================================================================
    var totalPassed = 0
    var totalFailed = 0
    var globalCycleCount = 0L
    fork { while (true) { dut.clockDomain.waitSampling(); globalCycleCount += 1 } }

    println(s"\n${"=" * 70}")
    println(s"SystemWrapper Operator-Level Test")
    println(s"sideNum=$sideNum, elementWidth=$elementWidth, cacheAddrWidth=$cacheAddrWidth")
    println(s"Total test cases: ${testCases.length}")
    println(s"${"=" * 70}\n")

    for ((tc, idx) <- testCases.zipWithIndex) {
      println(s"--- [Test $idx] ${tc.name} ---")
      println(s"    matOp=${tc.matOp}, act=${tc.activation}, transpose=${tc.doTranspose}")
      println(s"    dims: A(${tc.M}x${tc.K}) * B(${tc.K}x${tc.N})")

      // --- 生成随机测试矩阵（值域较小避免溢出）---
      val valueRange = tc.activation match {
        case Activation_TypeDef.Exp | Activation_TypeDef.Softplus => (-3, 4) // 避免 Exp 溢出
        case _ => (-5, 6)
      }
      val matA = Array.tabulate(tc.M, tc.K)((_, _) =>
        random.between(valueRange._1, valueRange._2))
      val matB = if (tc.matOp == MatrixOperation_TypeDef.MatMul) {
        Array.tabulate(tc.K, tc.N)((_, _) =>
          random.between(valueRange._1, valueRange._2))
      } else {
        // 逐元素运算：B 的 shape 与 A 相同
        Array.tabulate(tc.M, tc.N)((_, _) =>
          random.between(valueRange._1, valueRange._2))
      }

      // --- Golden model ---
      val inputShape1ForB = if (tc.matOp == MatrixOperation_TypeDef.MatMul) tc.N else tc.N
      val instSim = new InstSim(
        UID = idx,
        matrixOperation = tc.matOp,
        shiftLeft_AfterMatrixOperation = tc.shiftAfterOp,
        doTranspose = tc.doTranspose,
        activationFunction = tc.activation,
        shiftLeft_AfterActivation = tc.shiftAfterAct,
        input0Address = 0,
        input1Address = 0,
        outputAddress = 0,
        input0Shape0 = tc.M,
        input0Shape1 = tc.K,
        input1Shape1 = tc.N,
        shiftLeft_A = tc.shiftA,
        shiftLeft_B = tc.shiftB
      )
      instSim.computeShape()
      val goldenResult = instSim.acceleratorSim(matA, matB, acceleratorCfg)
      println(s"    output shape: (${instSim.outputShape0}x${instSim.outputShape1})")

      // --- DMA 参数：全矩阵无 tiling ---
      val matABeatsPerRow = tc.K / sideNum
      val matBCols = if (tc.matOp == MatrixOperation_TypeDef.MatMul) tc.N else tc.N
      val matBRows = if (tc.matOp == MatrixOperation_TypeDef.MatMul) tc.K else tc.M
      val matBBeatsPerRow = matBCols / sideNum
      val outBeatsPerRow = instSim.outputShape1 / sideNum

      // --- 1. 将矩阵写入 DDR ---
      writeMatrixToDDR(matA, DDR_BASE_A, tc.M, tc.K)
      writeMatrixToDDR(matB, DDR_BASE_B, matBRows, matBCols)
      println(s"    DDR loaded: A at 0x${DDR_BASE_A.toHexString}, B at 0x${DDR_BASE_B.toHexString}")

      // --- 2. 配置 Cache 生命周期 ---
      // 单次计算：A 和 B 各使用一次
      axiLiteWrite(dut.io.sAxi4LiteCache, 0x00, 1L) // LIFE_CFG_A = 1
      axiLiteWrite(dut.io.sAxi4LiteCache, 0x04, 1L) // LIFE_CFG_B = 1

      // --- 3. DMA_A: DDR → Cache A ---
      val cycleStart = globalCycleCount
      startDma(dut.io.ctrlDmaA,
        matBase = DDR_BASE_A, localBase = 0,
        matCols = matABeatsPerRow, matRows = tc.M,
        tileCols = matABeatsPerRow, tileRows = tc.M,
        tileColPos = 0, tileRowPos = 0, direction = 0)
      waitDmaIntr(dut.io.intrDmaA, "A")
      clearDmaIntr(dut.io.ctrlDmaA)
      val cycleDmaA = globalCycleCount - cycleStart
      println(s"    DMA_A done ($cycleDmaA cycles)")

      // --- 4. DMA_B: DDR → Cache B ---
      val cycleBStart = globalCycleCount
      startDma(dut.io.ctrlDmaB,
        matBase = DDR_BASE_B, localBase = 0,
        matCols = matBBeatsPerRow, matRows = matBRows,
        tileCols = matBBeatsPerRow, tileRows = matBRows,
        tileColPos = 0, tileRowPos = 0, direction = 0)
      waitDmaIntr(dut.io.intrDmaB, "B")
      clearDmaIntr(dut.io.ctrlDmaB)
      val cycleDmaB = globalCycleCount - cycleBStart
      println(s"    DMA_B done ($cycleDmaB cycles)")

      // --- 5. 发送计算指令 ---
      val cycleComputeStart = globalCycleCount
      sendInstruction(instSim)

      // --- 6. 等待计算完成中断 ---
      var waitCycles = 0
      val maxWait = 500000
      while (!dut.io.globalIntr.toBoolean && waitCycles < maxWait) {
        dut.clockDomain.waitSampling()
        waitCycles += 1
      }
      assert(waitCycles < maxWait, s"[${tc.name}] Timeout waiting for globalIntr!")
      val cycleCompute = globalCycleCount - cycleComputeStart
      println(s"    Compute done ($cycleCompute cycles)")

      // --- 7. DMA_Z: Cache C → DDR ---
      val cycleZStart = globalCycleCount
      startDma(dut.io.ctrlDmaZ,
        matBase = DDR_BASE_Z, localBase = 0,
        matCols = outBeatsPerRow, matRows = instSim.outputShape0,
        tileCols = outBeatsPerRow, tileRows = instSim.outputShape0,
        tileColPos = 0, tileRowPos = 0, direction = 1)
      waitDmaIntr(dut.io.intrDmaZ, "Z")
      clearDmaIntr(dut.io.ctrlDmaZ)
      val cycleDmaZ = globalCycleCount - cycleZStart
      println(s"    DMA_Z done ($cycleDmaZ cycles)")

      // --- 8. 清除 Cache C 中断 ---
      axiLiteWrite(dut.io.sAxi4LiteCache, 0x0C, 0x1)

      // --- 9. 从 DDR 读取结果并验证 ---
      val hwResult = readMatrixFromDDR(DDR_BASE_Z, instSim.outputShape0, instSim.outputShape1)
      val pass = verifyMatrix(goldenResult, hwResult, tc.name)

      val totalCycles = globalCycleCount - cycleStart
      if (pass) {
        totalPassed += 1
        println(s"    [Test $idx] ${tc.name}: PASS (total=$totalCycles cycles, " +
          s"DMA_A=$cycleDmaA, DMA_B=$cycleDmaB, compute=$cycleCompute, DMA_Z=$cycleDmaZ)")
      } else {
        totalFailed += 1
        println(s"    [Test $idx] ${tc.name}: FAIL (total=$totalCycles cycles)")
      }

      // 充分等待 DRAM 响应管线排空，避免残留事务影响下一个测试
      dut.clockDomain.waitSampling(1000)
    }

    // =================================================================
    // 结果汇总
    // =================================================================
    println(s"\n${"=" * 70}")
    println(s"RESULTS: $totalPassed passed, $totalFailed failed (out of ${testCases.length})")
    println(s"${"=" * 70}")

    if (totalFailed == 0) {
      println("ALL TESTS PASSED!")
      simSuccess()
    } else {
      simFailure(s"$totalFailed test(s) failed")
    }
  }
}

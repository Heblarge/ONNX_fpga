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
// 系统级大矩阵 64 位地址测试平台（Verilator + ramulator2 DPI）
//
// 目标：
//   1. 64 位 DDR 地址，矩阵基地址跨越 4GB 边界
//   2. 大矩阵尽可能用满 BRAM 的 512×512 元素空间
//   3. 完整数据通路：Host→DDR→DMA_A/B→Cache→Core→Cache→DMA_Z→DDR→Host
//
// BRAM 空间分析（sideNum=8, memElementWidth=32, dataWidth=256）：
//   每 beat = 8 个元素 = 32 字节
//   512×512 元素 = 512 行 × 64 beats/行 = 32768 words
//   cacheAddrWidth = 15 → 2^15 = 32768 words/bank
// =============================================================================
object SystemWrapper_LargeMatrix64Tb extends App {

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

  // ★ cacheAddrWidth = 15 → 32768 words/bank → 可容纳 512×64=32768 words
  // 即 512 行 × 512 列（64 beats/行 × 8 元素/beat）的完整矩阵
  val cacheAddrWidth = 15

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

  // ★ 64 位 DDR 地址
  val ddrAddrWidth = 64

  val sysCfg = SystemWrapperConfig(
    fpgaCfg        = fpgaCfg,
    cacheAddrWidth = cacheAddrWidth,
    ddrAddrWidth   = ddrAddrWidth,
    dmaMaxBurstLen = 256
  )

  val dmaCfg = StrideDmaConfig(ddrAddrWidth = ddrAddrWidth)

  // ★ DDR 布局：跨 4GB 边界
  //   A 矩阵: 3.5 GB (0x0_E000_0000)
  //   B 矩阵: 4.5 GB (0x1_1000_0000) — 跨越 4GB 边界
  //   Z 结果: 5.0 GB (0x1_4000_0000) — 完全在 >4GB 空间
  val DDR_BASE_A = 0x0E0000000L  // 3.5 GB
  val DDR_BASE_B = 0x110000000L  // 4.25 GB
  val DDR_BASE_Z = 0x140000000L  // 5.0 GB

  // =========================================================================
  // 测试用例：大矩阵用满 512×512 BRAM 空间
  // =========================================================================
  case class OpTestCase(
    name: String,
    matOp: MatrixOperation_TypeDef.E,
    activation: Activation_TypeDef.E,
    shiftAfterOp: Int, shiftAfterAct: Int,
    doTranspose: Boolean,
    M: Int, K: Int, N: Int,
    shiftA: Int, shiftB: Int
  )

  val testCases = Seq(
    // --- 大矩阵乘法（尽可能填满 512×512 BRAM）---
    // A(512×512) × B(512×512) → C(512×512)：完全填满 BRAM
    OpTestCase("MatMul_512x512x512", MatrixOperation_TypeDef.MatMul, Activation_TypeDef.None,
      0, 0, false, 512, 512, 512, 0, 0),

    // --- 非方阵：512×256×512，A 填满 512 行，B 填满 512 列 ---
    OpTestCase("MatMul_512x256x512", MatrixOperation_TypeDef.MatMul, Activation_TypeDef.None,
      0, 0, false, 512, 256, 512, 0, 0),

    // --- 大矩阵 + 激活函数 ---
    OpTestCase("MatMul_512x512x512_Relu", MatrixOperation_TypeDef.MatMul, Activation_TypeDef.Relu,
      0, 0, false, 512, 512, 512, 0, 0),

    // --- 逐元素运算填满 512×512 ---
    OpTestCase("ElementAdd_512x512", MatrixOperation_TypeDef.ElementAdd, Activation_TypeDef.None,
      0, 0, false, 512, 512, 512, 0, 0),
  )

  // =========================================================================
  // 编译 DUT
  // =========================================================================
  val path = s"simWorkspace/SystemWrapper_LargeMatrix64Tb"
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

  // 复制 ramulator_config.yaml
  {
    val ramCfgSrc = new File(s"$thirdPartyDir/ramulator_config.yaml").toPath
    val targetDir = new File(s"$path/SystemWrapper")
    targetDir.mkdirs()
    val ramCfgDst = new File(targetDir, "ramulator_config.yaml")
    if (!ramCfgDst.exists()) java.nio.file.Files.copy(ramCfgSrc, ramCfgDst.toPath)
    new File(targetDir, "trace").mkdirs()
    val rootCfgDst = new File("ramulator_config.yaml")
    if (!rootCfgDst.exists()) java.nio.file.Files.copy(ramCfgSrc, rootCfgDst.toPath)
    new File("trace").mkdirs()
  }

  // =========================================================================
  // 仿真主体
  // =========================================================================
  compiled.doSimUntilVoid { dut =>
    SimTimeout(5000000000L * period)  // 50 亿拍超时
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
    // Host AXI4 DDR 读写（支持 64 位地址）
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
    // DMA 控制（64 位地址版）
    // -----------------------------------------------------------------
    def startDma(axiCtrl: AxiLite4, matBase: Long, localBase: Long,
                 matCols: Int, matRows: Int, tileCols: Int, tileRows: Int,
                 tileColPos: Int, tileRowPos: Int, direction: Int): Unit = {
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
      axiLiteWrite(axiCtrl, dmaCfg.REG_CTRL, ((direction << 1) | 0x1).toLong)
    }

    def waitDmaIntr(intrSignal: Bool, name: String, maxWait: Int = 5000000): Unit = {
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
    println(s"SystemWrapper Large-Matrix 64-bit Address Test")
    println(s"sideNum=$sideNum, elementWidth=$elementWidth, cacheAddrWidth=$cacheAddrWidth")
    println(s"BRAM capacity: ${1 << cacheAddrWidth} words/bank = " +
      s"${(1 << cacheAddrWidth) / (512 / sideNum)} rows × 512 elements")
    println(s"DDR address width: $ddrAddrWidth bits")
    println(f"DDR_BASE_A = 0x${DDR_BASE_A}%X (3.5 GB)")
    println(f"DDR_BASE_B = 0x${DDR_BASE_B}%X (4.25 GB, above 4GB)")
    println(f"DDR_BASE_Z = 0x${DDR_BASE_Z}%X (5.0 GB, above 4GB)")
    println(s"Total test cases: ${testCases.length}")
    println(s"${"=" * 70}\n")

    val wallStart = System.currentTimeMillis()
    def wallElapsed(): String = {
      val sec = (System.currentTimeMillis() - wallStart) / 1000
      f"${sec / 3600}%d:${(sec % 3600) / 60}%02d:${sec % 60}%02d"
    }

    for ((tc, idx) <- testCases.zipWithIndex) {
      println(s"--- [Test $idx] ${tc.name} ---")
      println(s"    matOp=${tc.matOp}, act=${tc.activation}, transpose=${tc.doTranspose}")
      println(s"    dims: A(${tc.M}x${tc.K}) * B(${tc.K}x${tc.N})")
      val bramUtil = tc.M * (tc.K / sideNum) * 100.0 / (1 << cacheAddrWidth)
      println(s"    BRAM utilization: A=${tc.M * (tc.K / sideNum)}/${1 << cacheAddrWidth} words " +
        f"($bramUtil%.1f%%)")

      // --- 生成随机测试矩阵（值域较小避免定点溢出）---
      val valueRange = tc.activation match {
        case Activation_TypeDef.Exp | Activation_TypeDef.Softplus => (-2, 3)
        case _ => (-3, 4)  // 大矩阵累加，值域更小避免溢出
      }
      val matA = Array.tabulate(tc.M, tc.K)((_, _) =>
        random.between(valueRange._1, valueRange._2))
      val matB = if (tc.matOp == MatrixOperation_TypeDef.MatMul) {
        Array.tabulate(tc.K, tc.N)((_, _) =>
          random.between(valueRange._1, valueRange._2))
      } else {
        Array.tabulate(tc.M, tc.N)((_, _) =>
          random.between(valueRange._1, valueRange._2))
      }

      // --- Golden model ---
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
      println(s"    Computing golden model (${tc.M}x${tc.K} op ${tc.K}x${tc.N})...")
      val goldenStart = System.currentTimeMillis()
      val goldenResult = instSim.acceleratorSim(matA, matB, acceleratorCfg)
      val goldenMs = System.currentTimeMillis() - goldenStart
      println(s"    Golden model done in ${goldenMs}ms, output shape: (${instSim.outputShape0}x${instSim.outputShape1})")

      // --- DMA 参数 ---
      val matABeatsPerRow = tc.K / sideNum
      val matBCols = if (tc.matOp == MatrixOperation_TypeDef.MatMul) tc.N else tc.N
      val matBRows = if (tc.matOp == MatrixOperation_TypeDef.MatMul) tc.K else tc.M
      val matBBeatsPerRow = matBCols / sideNum
      val outBeatsPerRow = instSim.outputShape1 / sideNum

      // --- 1. 将矩阵写入 DDR（64 位地址）---
      println(s"    Writing A to DDR at 0x${DDR_BASE_A.toHexString} (${tc.M * matABeatsPerRow} beats)...")
      writeMatrixToDDR(matA, DDR_BASE_A, tc.M, tc.K)
      println(s"    Writing B to DDR at 0x${DDR_BASE_B.toHexString} (${matBRows * matBBeatsPerRow} beats)...")
      writeMatrixToDDR(matB, DDR_BASE_B, matBRows, matBCols)
      println(s"    DDR loaded [wall: ${wallElapsed()}]")

      // --- 2. 配置 Cache 生命周期 ---
      axiLiteWrite(dut.io.sAxi4LiteCache, 0x00, 1L)
      axiLiteWrite(dut.io.sAxi4LiteCache, 0x04, 1L)

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
      println(s"    DMA_A done ($cycleDmaA cycles) [wall: ${wallElapsed()}]")

      // --- 4. DMA_B: DDR → Cache B（B 矩阵在 >4GB 空间！）---
      val cycleBStart = globalCycleCount
      startDma(dut.io.ctrlDmaB,
        matBase = DDR_BASE_B, localBase = 0,
        matCols = matBBeatsPerRow, matRows = matBRows,
        tileCols = matBBeatsPerRow, tileRows = matBRows,
        tileColPos = 0, tileRowPos = 0, direction = 0)
      waitDmaIntr(dut.io.intrDmaB, "B")
      clearDmaIntr(dut.io.ctrlDmaB)
      val cycleDmaB = globalCycleCount - cycleBStart
      println(s"    DMA_B done ($cycleDmaB cycles) [wall: ${wallElapsed()}]")

      // --- 5. 发送计算指令 ---
      val cycleComputeStart = globalCycleCount
      sendInstruction(instSim)

      // --- 6. 等待计算完成中断 ---
      var waitCycles = 0
      val maxWait = 50000000  // 5000万拍（大矩阵计算需要更多时间）
      while (!dut.io.globalIntr.toBoolean && waitCycles < maxWait) {
        dut.clockDomain.waitSampling()
        waitCycles += 1
      }
      assert(waitCycles < maxWait, s"[${tc.name}] Timeout waiting for globalIntr after $waitCycles cycles!")
      val cycleCompute = globalCycleCount - cycleComputeStart
      println(s"    Compute done ($cycleCompute cycles) [wall: ${wallElapsed()}]")

      // --- 7. DMA_Z: Cache C → DDR（结果写到 >4GB 空间）---
      val cycleZStart = globalCycleCount
      startDma(dut.io.ctrlDmaZ,
        matBase = DDR_BASE_Z, localBase = 0,
        matCols = outBeatsPerRow, matRows = instSim.outputShape0,
        tileCols = outBeatsPerRow, tileRows = instSim.outputShape0,
        tileColPos = 0, tileRowPos = 0, direction = 1)
      waitDmaIntr(dut.io.intrDmaZ, "Z")
      clearDmaIntr(dut.io.ctrlDmaZ)
      val cycleDmaZ = globalCycleCount - cycleZStart
      println(s"    DMA_Z done ($cycleDmaZ cycles) [wall: ${wallElapsed()}]")

      // --- 8. 清除 Cache C 中断 ---
      axiLiteWrite(dut.io.sAxi4LiteCache, 0x0C, 0x1)

      // --- 9. 从 DDR 读取结果并验证 ---
      println(s"    Reading result from DDR at 0x${DDR_BASE_Z.toHexString}...")
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
      println(s"    [wall: ${wallElapsed()}]")

      dut.clockDomain.waitSampling(1000)
    }

    // =================================================================
    // Part 2: 缓存复用测试（lifeCfg > 1, DMA 级分块）
    //
    // 当矩阵超出 BRAM 容量时，需将其拆分为多个 tile 分批搬运。
    // 被复用的矩阵只搬一次，通过 lifeCfg 使其在 cache 中驻留多轮计算。
    // =================================================================
    println(s"\n${"=" * 70}")
    println(s"Part 2: Tiled MatMul with Cache Reuse (lifeCfg > 1)")
    println(s"${"=" * 70}\n")

    // 执行单次分块计算并返回 PASS/FAIL
    // direction: 0=DMA read(DDR→cache), 1=DMA write(cache→DDR)
    def doTileCompute(tileIdx: Int, numTiles: Int,
                      matOp: MatrixOperation_TypeDef.E,
                      tileM: Int, tileK: Int, tileN: Int,
                      matA_ddr: Long, matA_totalCols: Int, matA_totalRows: Int,
                      matA_tileColPos: Int, matA_tileRowPos: Int,
                      loadA: Boolean, lifeCfgA: Int,
                      matB_ddr: Long, matB_totalCols: Int, matB_totalRows: Int,
                      matB_tileColPos: Int, matB_tileRowPos: Int,
                      loadB: Boolean, lifeCfgB: Int,
                      matZ_ddr: Long, matZ_totalCols: Int, matZ_totalRows: Int,
                      matZ_tileColPos: Int, matZ_tileRowPos: Int,
                      uid: Int): Unit = {
      val aBeats = tileK / sideNum
      val bBeats = tileN / sideNum
      val zBeats = tileN / sideNum

      // DMA A（如需加载）
      if (loadA) {
        axiLiteWrite(dut.io.sAxi4LiteCache, 0x00, lifeCfgA.toLong)
        startDma(dut.io.ctrlDmaA,
          matBase = matA_ddr, localBase = 0,
          matCols = matA_totalCols / sideNum, matRows = matA_totalRows,
          tileCols = aBeats, tileRows = tileM,
          tileColPos = matA_tileColPos / sideNum, tileRowPos = matA_tileRowPos,
          direction = 0)
        waitDmaIntr(dut.io.intrDmaA, s"A[tile$tileIdx]")
        clearDmaIntr(dut.io.ctrlDmaA)
      }

      // DMA B（如需加载）
      if (loadB) {
        axiLiteWrite(dut.io.sAxi4LiteCache, 0x04, lifeCfgB.toLong)
        startDma(dut.io.ctrlDmaB,
          matBase = matB_ddr, localBase = 0,
          matCols = matB_totalCols / sideNum, matRows = matB_totalRows,
          tileCols = bBeats, tileRows = tileK,
          tileColPos = matB_tileColPos / sideNum, tileRowPos = matB_tileRowPos,
          direction = 0)
        waitDmaIntr(dut.io.intrDmaB, s"B[tile$tileIdx]")
        clearDmaIntr(dut.io.ctrlDmaB)
      }

      // 发送计算指令
      val instSim = new InstSim(
        UID = uid, matrixOperation = matOp,
        shiftLeft_AfterMatrixOperation = 0, doTranspose = false,
        activationFunction = Activation_TypeDef.None,
        shiftLeft_AfterActivation = 0,
        input0Address = 0, input1Address = 0, outputAddress = 0,
        input0Shape0 = tileM, input0Shape1 = tileK, input1Shape1 = tileN,
        shiftLeft_A = 0, shiftLeft_B = 0
      )
      instSim.computeShape()
      sendInstruction(instSim)

      // 等待计算
      var w = 0; val mw = 50000000
      while (!dut.io.globalIntr.toBoolean && w < mw) { dut.clockDomain.waitSampling(); w += 1 }
      assert(w < mw, s"Tile $tileIdx: compute timeout")

      // DMA Z: cache → DDR
      startDma(dut.io.ctrlDmaZ,
        matBase = matZ_ddr, localBase = 0,
        matCols = matZ_totalCols / sideNum, matRows = matZ_totalRows,
        tileCols = zBeats, tileRows = tileM,
        tileColPos = matZ_tileColPos / sideNum, tileRowPos = matZ_tileRowPos,
        direction = 1)
      waitDmaIntr(dut.io.intrDmaZ, s"Z[tile$tileIdx]")
      clearDmaIntr(dut.io.ctrlDmaZ)
      axiLiteWrite(dut.io.sAxi4LiteCache, 0x0C, 0x1)
    }

    // -----------------------------------------------------------------
    // Test T0: A(512×512) × B(512×1024) → C(512×1024)
    //   B 超出 BRAM，分 2 个列 tile；A 复用 2 次（lifeCfgA=2）
    //
    //   Tile 0: A(512×512) × B[:,0:512]   → C[:,0:512]
    //   Tile 1: A(512×512) × B[:,512:1024] → C[:,512:1024]  （A 仍在 cache）
    // -----------------------------------------------------------------
    {
      val testName = "TiledMatMul_AReuse_512x512x1024"
      println(s"--- $testName ---")
      val M = 512; val K = 512; val N = 1024
      val tileN = 512
      val numBTiles = N / tileN  // 2

      // 生成矩阵
      val matA = Array.tabulate(M, K)((_, _) => random.between(-2, 3))
      val matB = Array.tabulate(K, N)((_, _) => random.between(-2, 3))

      println(s"    A($M×$K) × B($K×$N), B split into $numBTiles col-tiles of $tileN")
      println(s"    lifeCfg_A = $numBTiles (A reused $numBTiles times)")

      // 写入 DDR
      writeMatrixToDDR(matA, DDR_BASE_A, M, K)
      writeMatrixToDDR(matB, DDR_BASE_B, K, N)
      println(s"    DDR loaded [wall: ${wallElapsed()}]")

      val cycleStart = globalCycleCount
      for (bt <- 0 until numBTiles) {
        val bColStart = bt * tileN
        println(s"    [Tile $bt/$numBTiles] B cols [$bColStart, ${bColStart + tileN}), " +
          s"loadA=${bt == 0} [wall: ${wallElapsed()}]")

        doTileCompute(
          tileIdx = bt, numTiles = numBTiles,
          matOp = MatrixOperation_TypeDef.MatMul,
          tileM = M, tileK = K, tileN = tileN,
          // A: 只在第一轮加载
          matA_ddr = DDR_BASE_A, matA_totalCols = K, matA_totalRows = M,
          matA_tileColPos = 0, matA_tileRowPos = 0,
          loadA = (bt == 0), lifeCfgA = numBTiles,
          // B: 每轮加载不同 tile
          matB_ddr = DDR_BASE_B, matB_totalCols = N, matB_totalRows = K,
          matB_tileColPos = bColStart, matB_tileRowPos = 0,
          loadB = true, lifeCfgB = 1,
          // Z: 写入对应列位置
          matZ_ddr = DDR_BASE_Z, matZ_totalCols = N, matZ_totalRows = M,
          matZ_tileColPos = bColStart, matZ_tileRowPos = 0,
          uid = 100 + bt
        )
        println(s"    [Tile $bt] done [wall: ${wallElapsed()}]")
      }
      val totalCyc = globalCycleCount - cycleStart

      // 计算完整 golden 并验证
      val goldenFull = Array.ofDim[Int](M, N)
      for (bt <- 0 until numBTiles) {
        val bColStart = bt * tileN
        val bTile = Array.tabulate(K, tileN)((r, c) => matB(r)(bColStart + c))
        val inst = new InstSim(100 + bt, MatrixOperation_TypeDef.MatMul,
          0, false, Activation_TypeDef.None, 0, 0, 0, 0,
          M, K, tileN, 0, 0)
        inst.computeShape()
        val cTile = inst.acceleratorSim(matA, bTile, acceleratorCfg)
        for (r <- 0 until M; c <- 0 until tileN) goldenFull(r)(bColStart + c) = cTile(r)(c)
      }
      val hwResult = readMatrixFromDDR(DDR_BASE_Z, M, N)
      val pass = verifyMatrix(goldenFull, hwResult, testName)
      if (pass) { totalPassed += 1; println(s"    $testName: PASS ($totalCyc cycles)") }
      else      { totalFailed += 1; println(s"    $testName: FAIL ($totalCyc cycles)") }
      println(s"    [wall: ${wallElapsed()}]")
      dut.clockDomain.waitSampling(1000)
    }

    // -----------------------------------------------------------------
    // Test T1: A(1024×512) × B(512×512) → C(1024×512)
    //   A 超出 BRAM，分 2 个行 tile；B 复用 2 次（lifeCfgB=2）
    //
    //   Tile 0: A[0:512,:]   × B → C[0:512,:]
    //   Tile 1: A[512:1024,:] × B → C[512:1024,:]  （B 仍在 cache）
    // -----------------------------------------------------------------
    {
      val testName = "TiledMatMul_BReuse_1024x512x512"
      println(s"\n--- $testName ---")
      val M = 1024; val K = 512; val N = 512
      val tileM = 512
      val numATiles = M / tileM  // 2

      val matA = Array.tabulate(M, K)((_, _) => random.between(-2, 3))
      val matB = Array.tabulate(K, N)((_, _) => random.between(-2, 3))

      println(s"    A($M×$K) × B($K×$N), A split into $numATiles row-tiles of $tileM")
      println(s"    lifeCfg_B = $numATiles (B reused $numATiles times)")

      writeMatrixToDDR(matA, DDR_BASE_A, M, K)
      writeMatrixToDDR(matB, DDR_BASE_B, K, N)
      println(s"    DDR loaded [wall: ${wallElapsed()}]")

      val cycleStart = globalCycleCount
      for (at <- 0 until numATiles) {
        val aRowStart = at * tileM
        println(s"    [Tile $at/$numATiles] A rows [$aRowStart, ${aRowStart + tileM}), " +
          s"loadB=${at == 0} [wall: ${wallElapsed()}]")

        doTileCompute(
          tileIdx = at, numTiles = numATiles,
          matOp = MatrixOperation_TypeDef.MatMul,
          tileM = tileM, tileK = K, tileN = N,
          // A: 每轮加载不同行 tile
          matA_ddr = DDR_BASE_A, matA_totalCols = K, matA_totalRows = M,
          matA_tileColPos = 0, matA_tileRowPos = aRowStart,
          loadA = true, lifeCfgA = 1,
          // B: 只在第一轮加载
          matB_ddr = DDR_BASE_B, matB_totalCols = N, matB_totalRows = K,
          matB_tileColPos = 0, matB_tileRowPos = 0,
          loadB = (at == 0), lifeCfgB = numATiles,
          // Z: 写入对应行位置
          matZ_ddr = DDR_BASE_Z, matZ_totalCols = N, matZ_totalRows = M,
          matZ_tileColPos = 0, matZ_tileRowPos = aRowStart,
          uid = 200 + at
        )
        println(s"    [Tile $at] done [wall: ${wallElapsed()}]")
      }
      val totalCyc = globalCycleCount - cycleStart

      val goldenFull = Array.ofDim[Int](M, N)
      for (at <- 0 until numATiles) {
        val aRowStart = at * tileM
        val aTile = Array.tabulate(tileM, K)((r, c) => matA(aRowStart + r)(c))
        val inst = new InstSim(200 + at, MatrixOperation_TypeDef.MatMul,
          0, false, Activation_TypeDef.None, 0, 0, 0, 0,
          tileM, K, N, 0, 0)
        inst.computeShape()
        val cTile = inst.acceleratorSim(aTile, matB, acceleratorCfg)
        for (r <- 0 until tileM; c <- 0 until N) goldenFull(aRowStart + r)(c) = cTile(r)(c)
      }
      val hwResult = readMatrixFromDDR(DDR_BASE_Z, M, N)
      val pass = verifyMatrix(goldenFull, hwResult, testName)
      if (pass) { totalPassed += 1; println(s"    $testName: PASS ($totalCyc cycles)") }
      else      { totalFailed += 1; println(s"    $testName: FAIL ($totalCyc cycles)") }
      println(s"    [wall: ${wallElapsed()}]")
      dut.clockDomain.waitSampling(1000)
    }

    // =================================================================
    // Part 3: 连续多任务边界测试
    //
    // 背靠背执行 5 个不同类型的任务（混合 tiled / non-tiled），
    // 验证任务交界处缓存状态正确回收、不残留脏数据。
    //
    // 序列:
    //   Task 0: MatMul 512×512×512          (lifeCfg=1, 无 tiling)
    //   Task 1: ElementAdd 512×512          (lifeCfg=1, 无 tiling)
    //   Task 2: Tiled A×B(512×512×1024)     (lifeCfgA=2, 有 tiling)
    //   Task 3: MatMul+Relu 256×256×256     (lifeCfg=1, 缩小矩阵)
    //   Task 4: ElementMul 512×512          (lifeCfg=1, 重新填满 BRAM)
    //
    // 关键验证点:
    //   - Task 1→2: 从非 tiled 到 tiled 的 lifeCfg 切换
    //   - Task 2→3: tiled 完成后（lifeCfgA=2→0 自然释放）到新任务
    //   - Task 3→4: 矩阵尺寸从 256 变回 512，缓存地址空间的高区域
    //     是否因上一任务的残留数据而出错
    // =================================================================
    println(s"\n${"=" * 70}")
    println(s"Part 3: Consecutive Multi-Task Boundary Test (5 tasks back-to-back)")
    println(s"${"=" * 70}\n")

    // 辅助：执行单个非 tiled 任务
    def doSimpleTask(taskName: String, taskIdx: Int,
                     matOp: MatrixOperation_TypeDef.E,
                     act: Activation_TypeDef.E,
                     M: Int, K: Int, N: Int): Boolean = {
      println(s"    [Task $taskIdx] $taskName: $matOp + $act, A(${M}x${K}) op B(${K}x${N})")

      val vr = if (act == Activation_TypeDef.Exp || act == Activation_TypeDef.Softplus) (-2, 3) else (-3, 4)
      val matA = Array.tabulate(M, K)((_, _) => random.between(vr._1, vr._2))
      val matB = if (matOp == MatrixOperation_TypeDef.MatMul) {
        Array.tabulate(K, N)((_, _) => random.between(vr._1, vr._2))
      } else {
        Array.tabulate(M, N)((_, _) => random.between(vr._1, vr._2))
      }

      val inst = new InstSim(300 + taskIdx, matOp, 0, false, act, 0,
        0, 0, 0, M, K, N, 0, 0)
      inst.computeShape()
      val golden = inst.acceleratorSim(matA, matB, acceleratorCfg)

      val bRows = if (matOp == MatrixOperation_TypeDef.MatMul) K else M
      val bCols = N
      writeMatrixToDDR(matA, DDR_BASE_A, M, K)
      writeMatrixToDDR(matB, DDR_BASE_B, bRows, bCols)

      axiLiteWrite(dut.io.sAxi4LiteCache, 0x00, 1L)
      axiLiteWrite(dut.io.sAxi4LiteCache, 0x04, 1L)

      startDma(dut.io.ctrlDmaA, DDR_BASE_A, 0,
        K / sideNum, M, K / sideNum, M, 0, 0, 0)
      waitDmaIntr(dut.io.intrDmaA, "A"); clearDmaIntr(dut.io.ctrlDmaA)

      startDma(dut.io.ctrlDmaB, DDR_BASE_B, 0,
        bCols / sideNum, bRows, bCols / sideNum, bRows, 0, 0, 0)
      waitDmaIntr(dut.io.intrDmaB, "B"); clearDmaIntr(dut.io.ctrlDmaB)

      sendInstruction(inst)
      var w = 0; val mw = 50000000
      while (!dut.io.globalIntr.toBoolean && w < mw) { dut.clockDomain.waitSampling(); w += 1 }
      assert(w < mw, s"[$taskName] compute timeout")

      val outBpr = inst.outputShape1 / sideNum
      startDma(dut.io.ctrlDmaZ, DDR_BASE_Z, 0,
        outBpr, inst.outputShape0, outBpr, inst.outputShape0, 0, 0, 1)
      waitDmaIntr(dut.io.intrDmaZ, "Z"); clearDmaIntr(dut.io.ctrlDmaZ)
      axiLiteWrite(dut.io.sAxi4LiteCache, 0x0C, 0x1)

      val hw = readMatrixFromDDR(DDR_BASE_Z, inst.outputShape0, inst.outputShape1)
      verifyMatrix(golden, hw, taskName)
    }

    // 辅助：执行 tiled A-reuse 任务（与 Part 2 T0 相同逻辑）
    def doTiledAReuse(taskName: String, taskIdx: Int,
                      M: Int, K: Int, N: Int, tileN: Int): Boolean = {
      val numBTiles = N / tileN
      println(s"    [Task $taskIdx] $taskName: A($M×$K) × B($K×$N), $numBTiles B-tiles, lifeCfgA=$numBTiles")

      val matA = Array.tabulate(M, K)((_, _) => random.between(-2, 3))
      val matB = Array.tabulate(K, N)((_, _) => random.between(-2, 3))

      writeMatrixToDDR(matA, DDR_BASE_A, M, K)
      writeMatrixToDDR(matB, DDR_BASE_B, K, N)

      for (bt <- 0 until numBTiles) {
        doTileCompute(bt, numBTiles, MatrixOperation_TypeDef.MatMul,
          M, K, tileN,
          DDR_BASE_A, K, M, 0, 0, bt == 0, numBTiles,
          DDR_BASE_B, N, K, bt * tileN, 0, true, 1,
          DDR_BASE_Z, N, M, bt * tileN, 0,
          400 + taskIdx * 10 + bt)
      }

      val goldenFull = Array.ofDim[Int](M, N)
      for (bt <- 0 until numBTiles) {
        val bcs = bt * tileN
        val bTile = Array.tabulate(K, tileN)((r, c) => matB(r)(bcs + c))
        val inst = new InstSim(400 + taskIdx * 10 + bt, MatrixOperation_TypeDef.MatMul,
          0, false, Activation_TypeDef.None, 0, 0, 0, 0, M, K, tileN, 0, 0)
        inst.computeShape()
        val cTile = inst.acceleratorSim(matA, bTile, acceleratorCfg)
        for (r <- 0 until M; c <- 0 until tileN) goldenFull(r)(bcs + c) = cTile(r)(c)
      }
      val hw = readMatrixFromDDR(DDR_BASE_Z, M, N)
      verifyMatrix(goldenFull, hw, taskName)
    }

    val seqCycleStart = globalCycleCount

    // Task 0: MatMul 512×512 (填满 BRAM, lifeCfg=1)
    val t0 = doSimpleTask("Seq_MatMul_512", 0,
      MatrixOperation_TypeDef.MatMul, Activation_TypeDef.None, 512, 512, 512)
    if (t0) totalPassed += 1 else totalFailed += 1
    println(s"    → ${if (t0) "PASS" else "FAIL"} [wall: ${wallElapsed()}]")
    dut.clockDomain.waitSampling(100)  // 极短间隔，测试快速切换

    // Task 1: ElementAdd 512×512 (不同运算类型)
    val t1 = doSimpleTask("Seq_ElemAdd_512", 1,
      MatrixOperation_TypeDef.ElementAdd, Activation_TypeDef.None, 512, 512, 512)
    if (t1) totalPassed += 1 else totalFailed += 1
    println(s"    → ${if (t1) "PASS" else "FAIL"} [wall: ${wallElapsed()}]")
    dut.clockDomain.waitSampling(100)

    // Task 2: Tiled A-reuse (从 lifeCfg=1 切换到 lifeCfgA=2, 关键边界)
    val t2 = doTiledAReuse("Seq_TiledAReuse_512x1024", 2, 512, 512, 1024, 512)
    if (t2) totalPassed += 1 else totalFailed += 1
    println(s"    → ${if (t2) "PASS" else "FAIL"} [wall: ${wallElapsed()}]")
    dut.clockDomain.waitSampling(100)

    // Task 3: MatMul+Relu 256×256 (tiled→non-tiled, 缩小矩阵, 激活函数变化)
    val t3 = doSimpleTask("Seq_MatMulRelu_256", 3,
      MatrixOperation_TypeDef.MatMul, Activation_TypeDef.Relu, 256, 256, 256)
    if (t3) totalPassed += 1 else totalFailed += 1
    println(s"    → ${if (t3) "PASS" else "FAIL"} [wall: ${wallElapsed()}]")
    dut.clockDomain.waitSampling(100)

    // Task 4: ElementMul 512×512 (重新填满 BRAM, 验证高地址区无残留数据影响)
    val t4 = doSimpleTask("Seq_ElemMul_512", 4,
      MatrixOperation_TypeDef.ElementMul, Activation_TypeDef.None, 512, 512, 512)
    if (t4) totalPassed += 1 else totalFailed += 1
    println(s"    → ${if (t4) "PASS" else "FAIL"} [wall: ${wallElapsed()}]")

    val seqCycles = globalCycleCount - seqCycleStart
    val seqAll = t0 && t1 && t2 && t3 && t4
    println(s"\n    Consecutive sequence: ${if (seqAll) "ALL PASS" else "HAS FAILURES"} " +
      f"($seqCycles cycles) [wall: ${wallElapsed()}]")

    dut.clockDomain.waitSampling(1000)

    // =================================================================
    // 结果汇总
    // =================================================================
    val totalTests = testCases.length + 2 + 5  // simple + 2 tiled + 5 consecutive
    println(s"\n${"=" * 70}")
    println(s"RESULTS: $totalPassed passed, $totalFailed failed (out of $totalTests)")
    println(s"  Part 1 (simple large matrix):     ${testCases.length} tests")
    println(s"  Part 2 (tiled cache reuse):        2 tests")
    println(s"  Part 3 (consecutive multi-task):   5 tests")
    println(s"Wall time: ${wallElapsed()}")
    println(s"${"=" * 70}")

    if (totalFailed == 0) {
      println("ALL TESTS PASSED!")
      simSuccess()
    } else {
      simFailure(s"$totalFailed test(s) failed")
    }
  }
}

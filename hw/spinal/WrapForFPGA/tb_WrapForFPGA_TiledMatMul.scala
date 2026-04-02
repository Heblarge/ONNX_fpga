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

// =============================================================================
// 大矩阵切片计算测试：WrapForFPGA + MatrixCache 带软件层 Tiling
//
// 测试场景：
//   大矩阵 A(M×K) * B(K×N) = Z(M×N)，其中 M、N 超过单个 cache bank 容量。
//   软件层将 A 横向切条（按行分组），B 纵向切条（按列分组）：
//     Z[i][j] = A_strip[i] * B_strip[j]
//   其中 A_strip[i] 可在所有 B_strip[j] 之间复用（通过 lifecycle 配置）。
//
// 切片策略：
//   给定共享维度 K，计算最大 mStrip、nStrip 使得 A_strip、B_strip、Z_block
//   均能放入各自的 cache bank（容量 = 2^cacheAddrWidth words）。
//   mStrip、nStrip 必须为 sideNum 的整数倍。
//
// 生命周期管理：
//   Cache A lifecycle = numBStrips（A strip 被所有 B strip 复用后释放）
//   Cache B lifecycle = 1（每个 B strip 用完即释放）
// =============================================================================
object WrapForFPGA_TiledMatMulTb extends App {
  val period = 10
  val errRateLimit = 0.01
  val zeroLimit = 10
  val seed = 42
  val random = new Random(seed)
  val sideNum = 8
  val elementWidth = 24
  val intWidth = 12
  val fracWidth = elementWidth - intWidth
  val memElementWidth = 32

  // 使用较小的缓存强制软件层进行 tiling
  val cacheAddrWidth = 8  // 256 words per bank
  val cacheWords = 1 << cacheAddrWidth

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

  // =========================================================================
  // 计算最优切片尺寸
  // =========================================================================
  // 给定共享维度 K，找到最大 mStrip × nStrip 使得：
  //   A strip (mStrip × K) 放入 cacheA: mStrip × ceil(K/s) <= C
  //   B strip (K × nStrip) 放入 cacheB: K × ceil(nStrip/s) <= C
  //   Z block (mStrip × nStrip) 放入 cacheC: mStrip × ceil(nStrip/s) <= C
  def computeStripSizes(K: Int): (Int, Int) = {
    require(K % sideNum == 0, s"K=$K must be a multiple of sideNum=$sideNum")
    val kBlocks = K / sideNum
    val mUpperA = (cacheWords / kBlocks / sideNum) * sideNum
    val nBlocksUpperB = cacheWords / K

    var bestM = 0
    var bestN = 0
    var bestProduct = 0L

    for (m <- sideNum to mUpperA by sideNum) {
      if (m * kBlocks <= cacheWords) {
        val nBlocksMaxZ = cacheWords / m
        val nBlocksMax = min(nBlocksUpperB, nBlocksMaxZ)
        val n = nBlocksMax * sideNum
        if (n >= sideNum) {
          val nBlocks = n / sideNum
          if (K * nBlocks <= cacheWords && m * nBlocks <= cacheWords) {
            val product = m.toLong * n
            if (product > bestProduct) {
              bestM = m
              bestN = n
              bestProduct = product
            }
          }
        }
      }
    }

    assert(bestM >= sideNum && bestN >= sideNum,
      s"Cache too small for K=$K with cacheWords=$cacheWords")
    println(s"  computeStripSizes(K=$K): mStrip=$bestM, nStrip=$bestN, " +
      s"A=${bestM * kBlocks}/$cacheWords words, " +
      s"B=${K * bestN / sideNum}/$cacheWords words, " +
      s"Z=${bestM * bestN / sideNum}/$cacheWords words")
    (bestM, bestN)
  }

  // =========================================================================
  // 测试用例定义
  // =========================================================================
  case class TiledTestCase(
    M: Int, K: Int, N: Int,
    mStrip: Int, nStrip: Int,
    numAStrips: Int, numBStrips: Int
  ) {
    val totalTiles: Int = numAStrips * numBStrips
    override def toString: String =
      s"${M}x${K} * ${K}x${N}, strip=(${mStrip}x${K})*(${K}x${nStrip}), " +
        s"tiles=${numAStrips}x${numBStrips}=${totalTiles}, A_life=${numBStrips}"
  }

  val testCases = {
    val rawCases = Seq(
      (96, 24, 192),   // K=24, 较小共享维度
      (96, 40, 120),   // K=40, 较大共享维度
      (128, 32, 128),  // K=32, 方形输出
    )
    rawCases.map { case (rawM, k, rawN) =>
      val (mStrip, nStrip) = computeStripSizes(k)
      // 调整 M、N 为 strip 尺寸的整数倍
      val m = ((rawM + mStrip - 1) / mStrip) * mStrip
      val n = ((rawN + nStrip - 1) / nStrip) * nStrip
      TiledTestCase(m, k, n, mStrip, nStrip, m / mStrip, n / nStrip)
    }
  }

  println("=== Tiled MatMul Test Configuration ===")
  println(s"Cache: ${cacheWords} words/bank (cacheAddrWidth=$cacheAddrWidth)")
  println(s"sideNum=$sideNum, elementWidth=$elementWidth, memElementWidth=$memElementWidth")
  testCases.zipWithIndex.foreach { case (tc, i) => println(s"  Case $i: $tc") }
  println()

  // =========================================================================
  // 子矩阵提取
  // =========================================================================
  def subMatrix(mat: Array[Array[Int]], rowStart: Int, rowEnd: Int,
                colStart: Int, colEnd: Int): Array[Array[Int]] = {
    Array.tabulate(rowEnd - rowStart, colEnd - colStart) { (i, j) =>
      mat(rowStart + i)(colStart + j)
    }
  }

  // =========================================================================
  // 编译 DUT（复用 WrapForFPGA_MatrixCacheTest 包装器）
  // =========================================================================
  val path = s"simWorkspace/WrapForFPGA_TiledMatMulTb"
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
  // 仿真主体
  // =========================================================================
  compiled.doSimUntilVoid { dut =>
    SimTimeout(50000000L * period)
    dut.clockDomain.forkStimulus(period)
    dut.core.clkCore.forkStimulus(4 * period)

    // --- 初始化信号 ---
    val axiInst = dut.io.sAxi4LiteInst
    axiInst.aw.valid #= false; axiInst.aw.addr #= 0
    axiInst.w.valid #= false; axiInst.w.data #= 0; axiInst.w.strb #= 0
    axiInst.b.ready #= true
    axiInst.ar.valid #= false; axiInst.ar.addr #= 0
    axiInst.r.ready #= true

    val axiCache = dut.io.sAxi4LiteCache
    axiCache.aw.valid #= false; axiCache.aw.addr #= 0
    axiCache.w.valid #= false; axiCache.w.data #= 0; axiCache.w.strb #= 0
    axiCache.b.ready #= true
    axiCache.ar.valid #= false; axiCache.ar.addr #= 0
    axiCache.r.ready #= true

    dut.io.dmaDoneA #= false
    dut.io.dmaDoneB #= false
    dut.io.dmaDoneC #= false

    dut.clockDomain.waitSampling(20)

    // --- AXI4-Lite 写操作 ---
    def axiLiteWrite(axi: AxiLite4, addr: Long, data: Long): Unit = {
      axi.aw.valid #= true; axi.aw.addr #= addr
      axi.w.valid #= true; axi.w.data #= data; axi.w.strb #= 0xF
      var awDone = false; var wDone = false
      while (!awDone || !wDone) {
        dut.clockDomain.waitSampling()
        if (!awDone && axi.aw.ready.toBoolean) { awDone = true; axi.aw.valid #= false }
        if (!wDone && axi.w.ready.toBoolean) { wDone = true; axi.w.valid #= false }
      }
      while (!axi.b.valid.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
    }

    // --- DMA 脉冲 ---
    def pulseDmaDone(signal: Bool): Unit = {
      signal #= true
      dut.clockDomain.waitSampling(2)
      signal #= false
      dut.clockDomain.waitSampling()
    }

    // --- DMA 写入缓存底层 SdpramModel.Mem（字索引） ---
    def dmaWriteToCache(sdpramMem: Mem[Bits], wrPtr: UInt,
                        mat: Array[Array[Int]], shape0: Int, shape1: Int): Unit = {
      val numBlocksX = shape1 / sideNum
      val elemMask = (BigInt(1) << memElementWidth) - 1
      val bankOffset = wrPtr.toInt * (1 << cacheAddrWidth)
      for (row <- 0 until shape0; blkCol <- 0 until numBlocksX) {
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

    // --- DMA 从缓存底层 SdpramModel.Mem 读取（字索引） ---
    def dmaReadFromCache(sdpramMem: Mem[Bits], rdPtr: UInt,
                         shape0: Int, shape1: Int): Array[Array[Int]] = {
      val numBlocksX = shape1 / sideNum
      val elemMask = (BigInt(1) << memElementWidth) - 1
      val signBit = BigInt(1) << (memElementWidth - 1)
      val bankOffset = rdPtr.toInt * (1 << cacheAddrWidth)
      val totalVectors = shape0 * numBlocksX
      val rawMem = Array.tabulate(totalVectors)(i => sdpramMem.getBigInt(bankOffset + i))
      Array.tabulate(shape0, shape1) { (i, j) =>
        val vectorIdx = i * numBlocksX + (j / sideNum)
        val elemIdx = j % sideNum
        val elemBits = (rawMem(vectorIdx) >> (elemIdx * memElementWidth)) & elemMask
        if ((elemBits & signBit) != 0)
          (elemBits | ((BigInt(-1) >> memElementWidth) << memElementWidth)).toInt
        else elemBits.toInt
      }
    }

    // --- 发送指令 ---
    def sendInstruction(instSim: InstSim): Unit = {
      val ShiftWidth = fpgaCfg.slicerCfg.ShiftWidth
      val ShapeWidth = fpgaCfg.ShapeWidth
      val UIDWidth = fpgaCfg.UIDWidth
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

    // =====================================================================
    // 主测试循环
    // =====================================================================
    var totalTiles = 0
    var cycleCount = 0L
    fork { while (true) { dut.clockDomain.waitSampling(); cycleCount += 1 } }
    val startCycle = cycleCount

    for ((tc, tcIdx) <- testCases.zipWithIndex) {
      println(s"\n${"=" * 70}")
      println(s"[TestCase $tcIdx] $tc")
      println(s"${"=" * 70}")

      // 生成大矩阵（值域较小以避免定点溢出）
      val matA = random.nextMat(tc.M, tc.K, -5, 6)
      val matB = random.nextMat(tc.K, tc.N, -5, 6)

      // 用于拼装完整结果
      val fullZ = Array.ofDim[Int](tc.M, tc.N)

      // 配置 Cache A lifecycle = 此 A strip 被复用的次数（B strip 个数）
      axiLiteWrite(axiCache, 0x00, tc.numBStrips.toLong)
      // Cache B lifecycle = 1（每个 B strip 用完即释放）
      axiLiteWrite(axiCache, 0x04, 1L)

      for (iStrip <- 0 until tc.numAStrips) {
        val mStart = iStrip * tc.mStrip
        val aStrip = subMatrix(matA, mStart, mStart + tc.mStrip, 0, tc.K)

        println(s"  [A strip $iStrip] rows [$mStart, ${mStart + tc.mStrip}), " +
          s"shape ${tc.mStrip}x${tc.K}, lifecycle=${tc.numBStrips}")

        // DMA 写入 A strip 到 cacheA
        dmaWriteToCache(
          dut.matrixCache.cacheA.sdpramModel.mem,
          dut.matrixCache.cacheA.inputMatrixCacheInterface.wrPtr,
          aStrip, tc.mStrip, tc.K
        )
        pulseDmaDone(dut.io.dmaDoneA)

        for (jStrip <- 0 until tc.numBStrips) {
          val nStart = jStrip * tc.nStrip
          val bStrip = subMatrix(matB, 0, tc.K, nStart, nStart + tc.nStrip)

          val tileIdx = iStrip * tc.numBStrips + jStrip
          totalTiles += 1

          // DMA 写入 B strip 到 cacheB
          dmaWriteToCache(
            dut.matrixCache.cacheB.sdpramModel.mem,
            dut.matrixCache.cacheB.inputMatrixCacheInterface.wrPtr,
            bStrip, tc.K, tc.nStrip
          )
          pulseDmaDone(dut.io.dmaDoneB)

          // 构造指令：MatMul, 无转置、无激活、无移位
          val instSim = new InstSim(
            UID = tileIdx % ((1 << fpgaCfg.UIDWidth) - 1),
            matrixOperation = MatrixOperation_TypeDef.MatMul,
            shiftLeft_AfterMatrixOperation = 0,
            doTranspose = false,
            activationFunction = Activation_TypeDef.None,
            shiftLeft_AfterActivation = 0,
            input0Address = 0,
            input1Address = 0,
            outputAddress = 0,
            input0Shape0 = tc.mStrip,
            input0Shape1 = tc.K,
            input1Shape1 = tc.nStrip,
            shiftLeft_A = 0,
            shiftLeft_B = 0
          )
          instSim.computeShape()

          // 计算此 tile 的 golden model
          val goldenTile = instSim.acceleratorSim(aStrip, bStrip, acceleratorCfg)

          println(s"    [Tile $tileIdx] A[$iStrip]*B[$jStrip]: " +
            s"(${tc.mStrip}x${tc.K}) * (${tc.K}x${tc.nStrip}) -> " +
            s"(${instSim.outputShape0}x${instSim.outputShape1})")

          sendInstruction(instSim)

          // 等待输出完成
          var waitCycles = 0
          val maxWait = 100000
          while (!dut.io.globalIntr.toBoolean && waitCycles < maxWait) {
            dut.clockDomain.waitSampling()
            waitCycles += 1
            if (waitCycles % 5000 == 0) {
              println(s"    [Tile $tileIdx] waiting... cycle=$waitCycles")
              println(s"      cacheA: bankValid=[" +
                s"${dut.matrixCache.cacheA.inputMatrixCacheInterface.bankValid(0).toBoolean}," +
                s"${dut.matrixCache.cacheA.inputMatrixCacheInterface.bankValid(1).toBoolean}] " +
                s"wrPtr=${dut.matrixCache.cacheA.inputMatrixCacheInterface.wrPtr.toInt} " +
                s"rdPtr=${dut.matrixCache.cacheA.inputMatrixCacheInterface.rdPtr.toInt}")
              println(s"      cacheB: bankValid=[" +
                s"${dut.matrixCache.cacheB.inputMatrixCacheInterface.bankValid(0).toBoolean}," +
                s"${dut.matrixCache.cacheB.inputMatrixCacheInterface.bankValid(1).toBoolean}] " +
                s"wrPtr=${dut.matrixCache.cacheB.inputMatrixCacheInterface.wrPtr.toInt} " +
                s"rdPtr=${dut.matrixCache.cacheB.inputMatrixCacheInterface.rdPtr.toInt}")
              println(s"      cacheC: bankValid=[" +
                s"${dut.matrixCache.cacheC.matrixCacheInterface.bankValid(0).toBoolean}," +
                s"${dut.matrixCache.cacheC.matrixCacheInterface.bankValid(1).toBoolean}] " +
                s"wrPtr=${dut.matrixCache.cacheC.matrixCacheInterface.wrPtr.toInt} " +
                s"rdPtr=${dut.matrixCache.cacheC.matrixCacheInterface.rdPtr.toInt}")
            }
          }
          assert(waitCycles < maxWait, s"Tile $tileIdx: Timeout waiting for output interrupt!")

          dut.clockDomain.waitSampling(5)

          // 读取结果
          val hwTile = dmaReadFromCache(
            dut.matrixCache.cacheC.sdpramModel.mem,
            dut.matrixCache.cacheC.matrixCacheInterface.rdPtr,
            instSim.outputShape0, instSim.outputShape1
          )

          // 逐元素验证
          matZipForeach(goldenTile, hwTile) { (zRef, zResult, i, j) =>
            val err = abs(zRef - zResult)
            val errRate = abs((zRef - zResult).toDouble / (if (zRef != 0) zRef.toDouble else 1.0))
            assert(
              if (errRate < errRateLimit) true else abs(zRef) < zeroLimit && abs(zResult) < zeroLimit,
              s"Tile $tileIdx (A[$iStrip]*B[$jStrip]): mismatch at ($i,$j), " +
                s"ref=$zRef (0x${zRef.toHexString}), hw=$zResult (0x${zResult.toHexString}), " +
                s"err=$err, rate=$errRate\n$instSim"
            )
          }

          // 将 tile 结果拼入全矩阵
          for (i <- 0 until instSim.outputShape0; j <- 0 until instSim.outputShape1) {
            fullZ(mStart + i)(nStart + j) = hwTile(i)(j)
          }

          // 清除中断，释放 C bank
          axiLiteWrite(axiCache, 0x0C, 0x1)
          pulseDmaDone(dut.io.dmaDoneC)

          println(s"    [Tile $tileIdx] PASS ($waitCycles cycles)")
        }
        // A strip[iStrip] 的 lifecycle 此时应已耗尽，bank 自动释放
      }

      println(s"[TestCase $tcIdx] All ${tc.totalTiles} tiles PASSED!")
    }

    val elapsed = cycleCount - startCycle
    println(s"\n${"=" * 70}")
    println(s"ALL ${testCases.length} TEST CASES PASSED ($totalTiles tiles total)")
    println(s"Total cycles: $elapsed, Cycles/tile: ${elapsed.toDouble / totalTiles}")
    println(s"${"=" * 70}")

    simSuccess()
  }
}

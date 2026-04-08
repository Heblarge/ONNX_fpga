package DMA

import DataPump._
import Tiling._

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.amba4.axilite._
import spinal.core.sim._

import java.io.File
import scala.util.Random

// =============================================================================
// DMA 64 位地址测试平台
//
// 验证目标：
//   1. ddrAddrWidth=64 时寄存器偏移自动适配
//   2. 矩阵基地址跨越 4GB 边界（高 32 位非零）
//   3. 源矩阵位于 0x0_C000_0000（3 GB），目标矩阵位于 0x1_4000_0000（5 GB）
//      跨越 4GB 边界验证 64 位地址寻址正确性
//   4. 完整的 tile 分块搬运 + 边缘裁剪功能
//
// 连接拓扑（同 DmaTestTb，但 ddrAddrWidth=64）：
//   ramulator2 DPI ←→ DRAMSim ←→ mDdr (StrideDma) mLocal ←→ AxiBramCtrl ←→ Sdpram
// =============================================================================

case class DmaTestTop64(dmaCfg: StrideDmaConfig) extends Component {
  val dataWidth = dmaCfg.dataWidth

  val dma       = StrideDma(dmaCfg)
  val bramCtrl  = AxiBramCtrl(AxiBramCtrlConfig(
    axiConfig    = dma.localAxiCfg,
    memAddrWidth = dmaCfg.localAddrWidth
  ))
  val sdpram    = Sdpram(addrWidth = dmaCfg.localAddrWidth, dataWidth = dataWidth)

  val dramSimCfg = DRAMSimConfig(
    addressWidth = dmaCfg.ddrAddrWidth,
    dataWidth    = dataWidth
  )
  val dramDpi    = new DRAMSimDPIDriver(dramSimCfg)
  val dramSim    = DRAMSim(dramSimCfg)

  // AXI 连接
  dramSim.fromAxi4(dma.io.mDdr)
  dramDpi.io.dram <> dramSim

  bramCtrl.io.axi <> dma.io.mLocal
  sdpram.io.read.clk     := ClockDomain.current.readClockWire
  sdpram.io.read.rst     := ClockDomain.current.readResetWire
  sdpram.io.read.Valid   := bramCtrl.io.memRead.Valid
  sdpram.io.read.Address := bramCtrl.io.memRead.Address.resized
  bramCtrl.io.memRead.Data := sdpram.io.read.Data

  sdpram.io.write.clk     := ClockDomain.current.readClockWire
  sdpram.io.write.rst     := ClockDomain.current.readResetWire
  sdpram.io.write.Valid   := bramCtrl.io.memWrite.Valid
  sdpram.io.write.Address := bramCtrl.io.memWrite.Address.resized
  sdpram.io.write.Data    := bramCtrl.io.memWrite.Data
  sdpram.io.write.Wen     := bramCtrl.io.memWrite.Wen

  val io = new Bundle {
    val ctrl = slave(AxiLite4(AxiLite4Config(addressWidth = 8, dataWidth = 32)))
    val intr = out Bool()
  }

  io.ctrl <> dma.io.ctrl
  io.intr := dma.io.intr
}

// =============================================================================
// 64 位地址 DMA 测试（Verilator, 跨 4GB 边界）
// =============================================================================
object DmaTest64Tb extends App {
  val period     = 10
  val seed       = 42
  val random     = new Random(seed)
  val dataWidth  = 256
  val bytesPerBeat = dataWidth / 8  // 32 bytes
  val byteOffset = log2Up(bytesPerBeat)

  // =========================================================================
  // 配置参数
  // =========================================================================
  val N = 100             // 矩阵维度 (beats): NxN（适中大小，快速验证）
  val T = 64              // Tile 标称维度 (beats)

  val tilesPerDim = (N + T - 1) / T
  val totalTiles  = tilesPerDim * tilesPerDim

  def tileActualSize(tilePos: Int, tileNominal: Int, matDim: Int): Int =
    scala.math.min(tileNominal, matDim - tilePos)

  // localAddrWidth 需要容纳一个完整 tile
  val localAddrWidth = log2Up(T * T * bytesPerBeat) + 1

  val dmaCfg = StrideDmaConfig(
    ddrAddrWidth   = 64,          // ★ 64 位地址
    localAddrWidth = localAddrWidth,
    dataWidth      = dataWidth
  )

  // 打印寄存器偏移验证
  println(s"=== 64-bit Address DMA Register Map ===")
  println(f"REG_CTRL         = 0x${dmaCfg.REG_CTRL}%02X")
  println(f"REG_STATUS       = 0x${dmaCfg.REG_STATUS}%02X")
  println(f"REG_INTR_CLR     = 0x${dmaCfg.REG_INTR_CLR}%02X")
  println(f"REG_MAT_BASE     = 0x${dmaCfg.REG_MAT_BASE}%02X  (64-bit, spans 2 words)")
  println(f"REG_LOCAL_BASE   = 0x${dmaCfg.REG_LOCAL_BASE}%02X")
  println(f"REG_MAT_COLS     = 0x${dmaCfg.REG_MAT_COLS}%02X")
  println(f"REG_MAT_ROWS     = 0x${dmaCfg.REG_MAT_ROWS}%02X")
  println(f"REG_TILE_COLS    = 0x${dmaCfg.REG_TILE_COLS}%02X")
  println(f"REG_TILE_ROWS    = 0x${dmaCfg.REG_TILE_ROWS}%02X")
  println(f"REG_TILE_COL_POS = 0x${dmaCfg.REG_TILE_COL_POS}%02X")
  println(f"REG_TILE_ROW_POS = 0x${dmaCfg.REG_TILE_ROW_POS}%02X")
  println(f"REG_ACTUAL_COLS  = 0x${dmaCfg.REG_ACTUAL_COLS}%02X")
  println(f"REG_ACTUAL_ROWS  = 0x${dmaCfg.REG_ACTUAL_ROWS}%02X")
  println()

  val path = s"simWorkspace/DmaTest64Tb"
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
      val dut = DmaTestTop64(dmaCfg)
      dut.sdpram.mem.simPublic()
      dut
    }

  // 复制 ramulator_config.yaml
  {
    val ramCfgSrc = new File(s"$thirdPartyDir/ramulator_config.yaml").toPath
    val targetDir = new File(s"$path/DmaTestTop64")
    targetDir.mkdirs()
    val ramCfgDst = new File(targetDir, "ramulator_config.yaml")
    if (!ramCfgDst.exists()) java.nio.file.Files.copy(ramCfgSrc, ramCfgDst.toPath)
    new File(targetDir, "trace").mkdirs()
    // Verilator JNI 模式需要项目根目录也有配置
    val rootCfgDst = new File("ramulator_config.yaml")
    if (!rootCfgDst.exists()) java.nio.file.Files.copy(ramCfgSrc, rootCfgDst.toPath)
    new File("trace").mkdirs()
  }

  // =========================================================================
  // 辅助函数
  // =========================================================================
  def axiLiteWrite(dut: DmaTestTop64, addr: Long, data: Long): Unit = {
    val axi = dut.io.ctrl
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

  def axiLiteRead(dut: DmaTestTop64, addr: Long): Long = {
    val axi = dut.io.ctrl
    axi.ar.valid #= true; axi.ar.addr #= addr
    while (!axi.ar.ready.toBoolean) dut.clockDomain.waitSampling()
    dut.clockDomain.waitSampling()
    axi.ar.valid #= false
    while (!axi.r.valid.toBoolean) dut.clockDomain.waitSampling()
    val data = axi.r.data.toLong
    dut.clockDomain.waitSampling()
    data
  }

  /** 写 64 位 matBase：拆成低 32 位 + 高 32 位两次 AXI-Lite 写 */
  def writeMatBase64(dut: DmaTestTop64, addr: Long): Unit = {
    axiLiteWrite(dut, dmaCfg.REG_MAT_BASE, addr & 0xFFFFFFFFL)
    axiLiteWrite(dut, dmaCfg.REG_MAT_BASE + 4, (addr >>> 32) & 0xFFFFFFFFL)
  }

  /** 配置 DMA 并启动（64 位地址版） */
  def startDma64(dut: DmaTestTop64,
                 matBase: Long, localBase: Long,
                 matCols: Int, matRows: Int,
                 tileCols: Int, tileRows: Int,
                 tileColPos: Int, tileRowPos: Int,
                 direction: Int): Unit = {
    writeMatBase64(dut, matBase)
    axiLiteWrite(dut, dmaCfg.REG_LOCAL_BASE, localBase)
    axiLiteWrite(dut, dmaCfg.REG_MAT_COLS, matCols.toLong)
    axiLiteWrite(dut, dmaCfg.REG_MAT_ROWS, matRows.toLong)
    axiLiteWrite(dut, dmaCfg.REG_TILE_COLS, tileCols.toLong)
    axiLiteWrite(dut, dmaCfg.REG_TILE_ROWS, tileRows.toLong)
    axiLiteWrite(dut, dmaCfg.REG_TILE_COL_POS, tileColPos.toLong)
    axiLiteWrite(dut, dmaCfg.REG_TILE_ROW_POS, tileRowPos.toLong)
    axiLiteWrite(dut, dmaCfg.REG_CTRL, ((direction << 1) | 0x1).toLong)
  }

  /** 一次性配置矩阵参数（64位地址） */
  def setupDmaMatrix64(dut: DmaTestTop64,
                       matBase: Long, localBase: Long,
                       matCols: Int, matRows: Int,
                       tileCols: Int, tileRows: Int): Unit = {
    writeMatBase64(dut, matBase)
    axiLiteWrite(dut, dmaCfg.REG_LOCAL_BASE, localBase)
    axiLiteWrite(dut, dmaCfg.REG_MAT_COLS, matCols.toLong)
    axiLiteWrite(dut, dmaCfg.REG_MAT_ROWS, matRows.toLong)
    axiLiteWrite(dut, dmaCfg.REG_TILE_COLS, tileCols.toLong)
    axiLiteWrite(dut, dmaCfg.REG_TILE_ROWS, tileRows.toLong)
  }

  /** 快速启动 tile 传输（仅更新位置 + 方向） */
  def startTileDma(dut: DmaTestTop64, tileColPos: Int, tileRowPos: Int, direction: Int): Unit = {
    axiLiteWrite(dut, dmaCfg.REG_TILE_COL_POS, tileColPos.toLong)
    axiLiteWrite(dut, dmaCfg.REG_TILE_ROW_POS, tileRowPos.toLong)
    axiLiteWrite(dut, dmaCfg.REG_CTRL, ((direction << 1) | 0x1).toLong)
  }

  /** 等待 DMA 完成 */
  def waitDmaDone(dut: DmaTestTop64, maxCycles: Int = 2000000): Int = {
    var cycles = 0
    while (cycles < maxCycles) {
      dut.clockDomain.waitSampling()
      cycles += 1
      if (dut.io.intr.toBoolean) {
        axiLiteWrite(dut, dmaCfg.REG_INTR_CLR, 0x1)
        return cycles
      }
    }
    assert(false, s"DMA timeout after $maxCycles cycles!")
    cycles
  }

  def readSdpramWord(dut: DmaTestTop64, wordIdx: Int): BigInt = dut.sdpram.mem.getBigInt(wordIdx)
  def writeSdpramWord(dut: DmaTestTop64, wordIdx: Int, value: BigInt): Unit = dut.sdpram.mem.setBigInt(wordIdx, value)

  /** 确定性数据生成 */
  def genWord(globalRow: Int, globalCol: Int): BigInt = {
    val s = globalRow.toLong * N + globalCol + 42L
    val r = new java.util.Random(s)
    val bytes = new Array[Byte](32)
    r.nextBytes(bytes)
    BigInt(new java.math.BigInteger(1, bytes))
  }

  // =========================================================================
  // 测试：64 位地址跨 4GB 边界搬运
  // =========================================================================
  // ★ 源矩阵在 3 GB （0x0_C000_0000），目标矩阵在 5 GB（0x1_4000_0000）
  //    源矩阵尾部超过 4GB 边界（如果 N 足够大），目标矩阵完全在 >4GB 空间
  val srcBase: Long = 0x0C0000000L  // 3 GB
  val dstBase: Long = 0x140000000L  // 5 GB

  val matSizeBytes = N.toLong * N * bytesPerBeat
  val matSizeMB    = matSizeBytes / 1024 / 1024
  val edgeCols = N % T
  val edgeRows = N % T

  println(s"=== DMA 64-bit Address Test: ${N}x${N} Full Tile Transfer ===")
  println(s"Matrix: ${N}x${N} words ($matSizeMB MB per matrix)")
  println(s"Tile: ${T}x${T} nominal, $totalTiles tiles ($tilesPerDim x $tilesPerDim)")
  println(s"srcBase = 0x${srcBase.toHexString} (3 GB), dstBase = 0x${dstBase.toHexString} (5 GB)")
  println(s"src range: 0x${srcBase.toHexString} ~ 0x${(srcBase + matSizeBytes - 1).toHexString}")
  println(s"dst range: 0x${dstBase.toHexString} ~ 0x${(dstBase + matSizeBytes - 1).toHexString}")
  println(s"  ★ Destination is entirely above 4GB boundary (tests 64-bit addressing)")
  if (edgeCols != 0 || edgeRows != 0) {
    println(s"  ★ Non-divisible: edge tiles have cols=${if (edgeCols == 0) T else edgeCols}, rows=${if (edgeRows == 0) T else edgeRows}")
  }
  println(s"localAddrWidth=$localAddrWidth, ddrAddrWidth=${dmaCfg.ddrAddrWidth}")
  println()

  compiled.doSimUntilVoid(s"DMA64_${N}x${N}_CrossBoundary") { dut =>
    SimTimeout(2000000000L * period)
    dut.clockDomain.forkStimulus(period)

    dut.io.ctrl.aw.valid #= false
    dut.io.ctrl.w.valid  #= false
    dut.io.ctrl.ar.valid #= false
    dut.io.ctrl.r.ready  #= true
    dut.io.ctrl.b.ready  #= true

    dut.clockDomain.waitSampling(20)

    val startTime = System.currentTimeMillis()
    def elapsed(): String = {
      val sec = (System.currentTimeMillis() - startTime) / 1000
      f"${sec / 3600}%d:${(sec % 3600) / 60}%02d:${sec % 60}%02d"
    }

    // =================================================================
    // Phase 0: 验证 64 位寄存器读写
    // =================================================================
    println("[Phase 0] 64-bit register read/write verification..."); {
      // 写一个跨 4GB 的地址
      val testAddr: Long = 0x1_DEAD_BEEFL
      writeMatBase64(dut, testAddr)
      dut.clockDomain.waitSampling(5)

      // 读回低 32 位
      val lo = axiLiteRead(dut, dmaCfg.REG_MAT_BASE)
      // 读回高 32 位
      val hi = axiLiteRead(dut, dmaCfg.REG_MAT_BASE + 4)
      val readBack = (hi << 32) | (lo & 0xFFFFFFFFL)
      assert(readBack == testAddr,
        f"64-bit MAT_BASE readback mismatch: wrote 0x${testAddr}%X, read 0x${readBack}%X (lo=0x${lo}%08X, hi=0x${hi}%08X)")
      println(f"  [OK] MAT_BASE=0x${testAddr}%X → readback lo=0x${lo & 0xFFFFFFFFL}%08X, hi=0x${hi}%08X → 0x${readBack}%X")
      println("[Phase 0] PASSED\n")
    }

    // =================================================================
    // Phase 1: 预填充源矩阵到 DDR（基地址 3 GB）
    // =================================================================
    println(s"[Phase 1] Pre-filling source DDR at 0x${srcBase.toHexString} ($totalTiles tiles)...")
    var phase1Cycles = 0L
    setupDmaMatrix64(dut, matBase = srcBase, localBase = 0,
      matCols = N, matRows = N, tileCols = T, tileRows = T)
    for (tileIdx <- 0 until totalTiles) {
      val tr = tileIdx / tilesPerDim; val tc = tileIdx % tilesPerDim
      val colPos = tc * T; val rowPos = tr * T
      val aCols = tileActualSize(colPos, T, N)
      val aRows = tileActualSize(rowPos, T, N)

      for (r <- 0 until aRows; c <- 0 until aCols)
        writeSdpramWord(dut, r * aCols + c, genWord(rowPos + r, colPos + c))
      dut.clockDomain.waitSampling(5)

      startTileDma(dut, tileColPos = colPos, tileRowPos = rowPos, direction = 1)
      phase1Cycles += waitDmaDone(dut)

      if (tileIdx % 64 == 0 || tileIdx == totalTiles - 1)
        println(f"  Tile $tileIdx%4d/$totalTiles ($tr%2d,$tc%2d) ${aCols}x${aRows}  [${elapsed()}]")
    }
    println(s"[Phase 1] Done. Total cycles: $phase1Cycles  [${elapsed()}]\n")

    // =================================================================
    // Phase 2: 分块搬运 src(3GB) → BRAM → dst(5GB)
    // =================================================================
    println(s"[Phase 2] Copying src→dst across 4GB boundary ($totalTiles tiles x 2 DMAs)...")
    var phase2Cycles = 0L
    for (tileIdx <- 0 until totalTiles) {
      val tr = tileIdx / tilesPerDim; val tc = tileIdx % tilesPerDim
      val colPos = tc * T; val rowPos = tr * T

      // DDR src (3GB) → BRAM
      writeMatBase64(dut, srcBase)
      startTileDma(dut, tileColPos = colPos, tileRowPos = rowPos, direction = 0)
      phase2Cycles += waitDmaDone(dut)

      // BRAM → DDR dst (5GB, above 4GB boundary)
      writeMatBase64(dut, dstBase)
      startTileDma(dut, tileColPos = colPos, tileRowPos = rowPos, direction = 1)
      phase2Cycles += waitDmaDone(dut)

      if (tileIdx % 64 == 0 || tileIdx == totalTiles - 1)
        println(f"  Tile $tileIdx%4d/$totalTiles  [${elapsed()}]")
    }
    println(s"[Phase 2] Done. Total cycles: $phase2Cycles  [${elapsed()}]\n")

    // =================================================================
    // Phase 3: 验证源矩阵 (3GB)
    // =================================================================
    println(s"[Phase 3] Verifying source DDR at 0x${srcBase.toHexString}...")
    var srcErrors = 0; var phase3Cycles = 0L
    setupDmaMatrix64(dut, matBase = srcBase, localBase = 0,
      matCols = N, matRows = N, tileCols = T, tileRows = T)
    for (tileIdx <- 0 until totalTiles) {
      val tr = tileIdx / tilesPerDim; val tc = tileIdx % tilesPerDim
      val colPos = tc * T; val rowPos = tr * T
      val aCols = tileActualSize(colPos, T, N); val aRows = tileActualSize(rowPos, T, N)

      startTileDma(dut, tileColPos = colPos, tileRowPos = rowPos, direction = 0)
      phase3Cycles += waitDmaDone(dut)
      dut.clockDomain.waitSampling(10)

      for (r <- 0 until aRows; c <- 0 until aCols) {
        val actual = readSdpramWord(dut, r * aCols + c)
        val expected = genWord(rowPos + r, colPos + c)
        if (actual != expected) {
          if (srcErrors < 10)
            println(s"  SRC MISMATCH tile($tr,$tc)[$r][$c]: got 0x${actual.toString(16).take(16)}..., " +
              s"expected 0x${expected.toString(16).take(16)}...")
          srcErrors += 1
        }
      }
    }
    if (srcErrors == 0) println(s"  [PASS] Source matrix (3GB): all ${N.toLong * N} words intact!")
    else println(s"  [FAIL] Source matrix: $srcErrors mismatches")
    println(s"[Phase 3] Done. Cycles: $phase3Cycles  [${elapsed()}]\n")

    // =================================================================
    // Phase 4: 验证目标矩阵 (5GB, above 4GB boundary)
    // =================================================================
    println(s"[Phase 4] Verifying destination DDR at 0x${dstBase.toHexString} (above 4GB)...")
    var dstErrors = 0; var phase4Cycles = 0L
    setupDmaMatrix64(dut, matBase = dstBase, localBase = 0,
      matCols = N, matRows = N, tileCols = T, tileRows = T)
    for (tileIdx <- 0 until totalTiles) {
      val tr = tileIdx / tilesPerDim; val tc = tileIdx % tilesPerDim
      val colPos = tc * T; val rowPos = tr * T
      val aCols = tileActualSize(colPos, T, N); val aRows = tileActualSize(rowPos, T, N)

      startTileDma(dut, tileColPos = colPos, tileRowPos = rowPos, direction = 0)
      phase4Cycles += waitDmaDone(dut)
      dut.clockDomain.waitSampling(10)

      for (r <- 0 until aRows; c <- 0 until aCols) {
        val actual = readSdpramWord(dut, r * aCols + c)
        val expected = genWord(rowPos + r, colPos + c)
        if (actual != expected) {
          if (dstErrors < 10)
            println(s"  DST MISMATCH tile($tr,$tc)[$r][$c]: got 0x${actual.toString(16).take(16)}..., " +
              s"expected 0x${expected.toString(16).take(16)}...")
          dstErrors += 1
        }
      }
    }
    if (dstErrors == 0) println(s"  [PASS] Destination matrix (5GB): all ${N.toLong * N} words match source!")
    else println(s"  [FAIL] Destination matrix: $dstErrors mismatches")
    println(s"[Phase 4] Done. Cycles: $phase4Cycles  [${elapsed()}]\n")

    // =================================================================
    // 总结
    // =================================================================
    val totalErrors = srcErrors + dstErrors
    val totalCycles = phase1Cycles + phase2Cycles + phase3Cycles + phase4Cycles
    println("=" * 70)
    println(s"64-bit Address DMA Test: ${N}x${N} matrix, ${T}x${T} tiles")
    println(s"Source:      0x${srcBase.toHexString} (3 GB)")
    println(s"Destination: 0x${dstBase.toHexString} (5 GB, above 4GB boundary)")
    println(s"Total tiles: $totalTiles, Total cycles: $totalCycles")
    println(s"Phase 1 (fill src):   $phase1Cycles cycles")
    println(s"Phase 2 (copy 3G→5G): $phase2Cycles cycles")
    println(s"Phase 3 (verify src): $phase3Cycles cycles ($srcErrors errors)")
    println(s"Phase 4 (verify dst): $phase4Cycles cycles ($dstErrors errors)")
    println(s"Elapsed: ${elapsed()}")
    println("=" * 70)
    if (totalErrors == 0) {
      println("[ALL PASS] 64-bit address DMA test passed — cross-4GB boundary verified!")
    } else {
      println(s"[FAIL] Total $totalErrors errors")
    }
    simSuccess()
  }
}

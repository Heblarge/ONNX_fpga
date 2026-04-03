package DMA

import DataPump._
import Tiling._

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.amba4.axilite._
import spinal.core.sim._
import spinal.sim.VCSFlags

import java.io.File
import scala.util.Random

// =============================================================================
// DMA 测试顶层：DDR (ramulator2 DPI) ← DMA → SDPRAM (via AXI BRAM Ctrl)
//
// 测试流程：
//   1. 预初始化 DDR 中一个大矩阵（通过 DMA 写入 DDR）
//   2. DDR → Local：DMA 将子矩阵块搬入 SDPRAM
//   3. 通过 AXI BRAM Ctrl 的读端口验证 SDPRAM 内容
//   4. Local → DDR：修改 SDPRAM 数据，DMA 搬回 DDR 的另一位置
//   5. 从 DDR 读回并验证
//
// 连接拓扑：
//   ramulator2 DPI ←→ DRAMSim ←→ mDdr (StrideDma) mLocal ←→ AxiBramCtrl ←→ Sdpram
// =============================================================================

case class DmaTestTop(dmaCfg: StrideDmaConfig) extends Component {
  val dataWidth = dmaCfg.dataWidth

  val dma       = StrideDma(dmaCfg)
  val bramCtrl  = AxiBramCtrl(AxiBramCtrlConfig(
    axiConfig    = dma.localAxiCfg,
    memAddrWidth = dmaCfg.localAddrWidth
  ))
  val sdpram    = Sdpram(addrWidth = dmaCfg.localAddrWidth, dataWidth = dataWidth)

  // DRAMSim DPI BlackBox
  val dramSimCfg = DRAMSimConfig(
    addressWidth = dmaCfg.ddrAddrWidth,
    dataWidth    = dataWidth
  )
  val dramDpi    = new DRAMSimDPIDriver(dramSimCfg)
  val dramSim    = DRAMSim(dramSimCfg)

  // AXI 连接：DMA.mDdr → DRAMSim → DPI
  dramSim.fromAxi4(dma.io.mDdr)
  dramDpi.io.dram <> dramSim

  // AXI 连接：DMA.mLocal → AXI BRAM Ctrl → Sdpram
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
// 仿真主体
// =============================================================================
object DmaTestTb extends App {
  val period     = 10
  val seed       = 42
  val random     = new Random(seed)
  val dataWidth  = 256
  val bytesPerBeat = dataWidth / 8  // 32 bytes
  val byteOffset = log2Up(bytesPerBeat) // 5

  // =========================================================================
  // ★★★ 用户可配置参数 ★★★
  // =========================================================================
  val N = 500             // 大矩阵维度 (beats): NxN 矩阵（允许不整除）
  val T = 256             // Tile 标称维度 (beats): TxT 分块
  // =========================================================================

  // ★ 不要求 N % T == 0：边缘 tile 由硬件自动裁剪
  val tilesPerDim = (N + T - 1) / T   // 向上取整
  val totalTiles  = tilesPerDim * tilesPerDim

  /** 计算 tile 在指定维度上的实际大小（边缘裁剪） */
  def tileActualSize(tilePos: Int, tileNominal: Int, matDim: Int): Int =
    scala.math.min(tileNominal, matDim - tilePos)

  // localAddrWidth 需要容纳一个完整 tile: T*T*bytesPerBeat 字节
  val localAddrWidth = log2Up(T * T * bytesPerBeat) + 1  // +1 留余量

  val dmaCfg = StrideDmaConfig(
    ddrAddrWidth   = 32,
    localAddrWidth = localAddrWidth,
    dataWidth      = dataWidth
  )

  val path = s"simWorkspace/DmaTestTb"
  new File(path).mkdirs()

  // 需要将 ramulator_config.yaml 和 DPI .so 链接到仿真工作目录
  val thirdPartyDir = new File("hw/third_party").getAbsolutePath
  val dpiDir        = new File("hw/third_party/dpi").getAbsolutePath
  val ramulator2Dir = new File("hw/third_party/ramulator2").getAbsolutePath

  val compiled = SimConfig.workspacePath(path)
    .withFsdbWave
    // 注意：不启用 withFsdbWave，超大矩阵仿真会产生巨大的波形文件
    .withConfig(SpinalConfig(bitVectorWidthMax = 100000))
    .allOptimisation
    .addRtl(s"$dpiDir/DRAMSimDPIDriver.sv")
    .withVCS(VCSFlags(
      compileFlags = List(
        "-kdb", "-lca", "+notimingchecks"
      ),
      elaborateFlags = List(
        "-fgp", "-kdb", "-lca", "+rad", "+notimingchecks",
        s"-LDFLAGS -L$dpiDir",
        s"-LDFLAGS -L$ramulator2Dir",
        s"-LDFLAGS -l:DRAMSimDPIDriverRamulator2.so",
        s"-LDFLAGS -lramulator",
        s"-LDFLAGS -Wl,-rpath,$dpiDir",
        s"-LDFLAGS -Wl,-rpath,$ramulator2Dir"
      ),
      runFlags = List("-l ./run.log")
    ))
    .compile {
      val dut = DmaTestTop(dmaCfg)
      dut.sdpram.mem.simPublic()
      dut
    }

  // 复制 ramulator_config.yaml 到 VCS 仿真运行目录
  // 避免修改仿真目录中的文件时影响到源文件
  {
    val vcsRunDir = new File(s"$path/DmaTestTop")
    vcsRunDir.mkdirs()
    val ramCfgDst = new File(vcsRunDir, "ramulator_config.yaml")
    if (!ramCfgDst.exists()) {
      val ramCfgSrc = new File(s"$thirdPartyDir/ramulator_config.yaml").toPath
      java.nio.file.Files.copy(ramCfgSrc, ramCfgDst.toPath)
    }
  }

  // =========================================================================
  // 辅助函数
  // =========================================================================
  def axiLiteWrite(dut: DmaTestTop, addr: Long, data: Long): Unit = {
    val axi = dut.io.ctrl
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

  def axiLiteRead(dut: DmaTestTop, addr: Long): Long = {
    val axi = dut.io.ctrl
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

  /** 配置 DMA 寄存器并启动传输（矩阵分块语义接口）
    *
    * @param matBase    大矩阵 DDR 基地址
    * @param localBase  BRAM 基地址
    * @param matCols    矩阵总列数 (beats)
    * @param matRows    矩阵总行数
    * @param tileCols   分块标称宽度 (beats)
    * @param tileRows   分块标称高度
    * @param tileColPos 当前分块列起始位置 (beats)
    * @param tileRowPos 当前分块行起始位置
    * @param direction  0=DDR→Local (提取), 1=Local→DDR (拼合)
    */
  def startDma(dut: DmaTestTop,
               matBase: Long, localBase: Long,
               matCols: Int, matRows: Int,
               tileCols: Int, tileRows: Int,
               tileColPos: Int, tileRowPos: Int,
               direction: Int): Unit = {
    axiLiteWrite(dut, 0x0C, matBase)
    axiLiteWrite(dut, 0x10, localBase)
    axiLiteWrite(dut, 0x14, matCols.toLong)
    axiLiteWrite(dut, 0x18, matRows.toLong)
    axiLiteWrite(dut, 0x1C, tileCols.toLong)
    axiLiteWrite(dut, 0x20, tileRows.toLong)
    axiLiteWrite(dut, 0x24, tileColPos.toLong)
    axiLiteWrite(dut, 0x28, tileRowPos.toLong)
    // direction 在 bit 1，start 在 bit 0
    val ctrlVal = (direction << 1) | 0x1
    axiLiteWrite(dut, 0x00, ctrlVal.toLong)
  }

  /** 一次性配置矩阵和分块尺寸（每次切换矩阵时调用一次） */
  def setupDmaMatrix(dut: DmaTestTop,
                     matBase: Long, localBase: Long,
                     matCols: Int, matRows: Int,
                     tileCols: Int, tileRows: Int): Unit = {
    axiLiteWrite(dut, 0x0C, matBase)
    axiLiteWrite(dut, 0x10, localBase)
    axiLiteWrite(dut, 0x14, matCols.toLong)
    axiLiteWrite(dut, 0x18, matRows.toLong)
    axiLiteWrite(dut, 0x1C, tileCols.toLong)
    axiLiteWrite(dut, 0x20, tileRows.toLong)
  }

  /** 快速启动 tile 传输：仅更新 tile 位置和方向（每 tile 仅 3 次 AXI-Lite 写）*/
  def startTileDma(dut: DmaTestTop,
                   tileColPos: Int, tileRowPos: Int,
                   direction: Int): Unit = {
    axiLiteWrite(dut, 0x24, tileColPos.toLong)
    axiLiteWrite(dut, 0x28, tileRowPos.toLong)
    val ctrlVal = (direction << 1) | 0x1
    axiLiteWrite(dut, 0x00, ctrlVal.toLong)
  }

  /** 等待 DMA 完成（通过轮询中断位） */
  def waitDmaDone(dut: DmaTestTop, maxCycles: Int = 2000000): Int = {
    var cycles = 0
    while (cycles < maxCycles) {
      dut.clockDomain.waitSampling()
      cycles += 1
      if (dut.io.intr.toBoolean) {
        axiLiteWrite(dut, 0x08, 0x1)
        return cycles
      }
    }
    assert(false, s"DMA timeout after $maxCycles cycles!")
    cycles
  }

  /** 从 SDPRAM mem (simPublic) 读取一个 word */
  def readSdpramWord(dut: DmaTestTop, wordIdx: Int): BigInt = {
    dut.sdpram.mem.getBigInt(wordIdx)
  }

  /** 向 SDPRAM mem (simPublic) 写入一个 word */
  def writeSdpramWord(dut: DmaTestTop, wordIdx: Int, value: BigInt): Unit = {
    dut.sdpram.mem.setBigInt(wordIdx, value)
  }

  // =========================================================================
  // 测试常量（由用户配置参数推导）
  // =========================================================================

  /** 确定性数据生成：给定全局 (row, col) 坐标生成唯一的 256-bit word
    * 不依赖全局状态，可随时重新计算 */
  def genWord(globalRow: Int, globalCol: Int): BigInt = {
    val s = globalRow.toLong * N + globalCol + 42L
    val r = new java.util.Random(s)
    val bytes = new Array[Byte](32)
    r.nextBytes(bytes)
    BigInt(new java.math.BigInteger(1, bytes))
  }

  // =========================================================================
  // 测试用例：NxN 全矩阵分块传输
  // =========================================================================
  val matSizeMB = N.toLong * N * bytesPerBeat / 1024 / 1024
  val edgeCols = N % T
  val edgeRows = N % T
  println(s"=== DMA ${N}x${N} Full Tile Transfer Test (Edge-Aware) ===")
  println(s"Matrix: ${N}x${N} words ($matSizeMB MB per matrix)")
  println(s"Tile: ${T}x${T} nominal, $totalTiles tiles ($tilesPerDim x $tilesPerDim)")
  if (edgeCols != 0 || edgeRows != 0) {
    println(s"  ★ Non-divisible: edge tiles have cols=${if (edgeCols == 0) T else edgeCols}, rows=${if (edgeRows == 0) T else edgeRows}")
  }
  println(s"localAddrWidth=$localAddrWidth (${1 << localAddrWidth} bytes = ${(1 << localAddrWidth) / 1024} KB)")
  println(s"dataWidth=$dataWidth, bytesPerBeat=$bytesPerBeat")
  println(s"maxBeatsPerBurst=${dmaCfg.maxBeatsPerBurst} (4KB/${bytesPerBeat}B)")

  compiled.doSimUntilVoid(s"DMA_${N}x${N}_FullTile_Test") { dut =>
    SimTimeout(5000000000L * period)  // 50 亿拍超时
    dut.clockDomain.forkStimulus(period)

    // 初始化 AXI-Lite 信号
    dut.io.ctrl.aw.valid #= false
    dut.io.ctrl.w.valid  #= false
    dut.io.ctrl.ar.valid #= false
    dut.io.ctrl.r.ready  #= true
    dut.io.ctrl.b.ready  #= true

    dut.clockDomain.waitSampling(20)

    // 矩阵在 DDR 中的布局
    //   源矩阵：从 srcBase 开始，NxN row-major
    //   目标矩阵：从 dstBase 开始，相同布局
    //   地址空间：
    //     src: 0x00000000 ~ srcBase + N*N*32 - 1
    //     dst: 0x80000000 ~ dstBase + N*N*32 - 1
    val srcBase: Long    = 0x00000000L
    val dstBase: Long    = 0x80000000L  // 2 GB 偏移

    val startTime = System.currentTimeMillis()
    def elapsed(): String = {
      val sec = (System.currentTimeMillis() - startTime) / 1000
      f"${sec / 3600}%d:${(sec % 3600) / 60}%02d:${sec % 60}%02d"
    }

    // =================================================================
    // Phase 0: 中断与状态寄存器行为验证
    //   使用一个小 tile 传输，显式检查 BUSY/DONE/INTR 时序
    // =================================================================
    println("\n[Phase 0] Interrupt & status register behavior test..."); {
      // 初始状态：非 busy，无中断
      val status0 = axiLiteRead(dut, 0x04)
      assert((status0 & 0x1) == 0, s"Initial BUSY should be 0, got $status0")
      assert((status0 & 0x2) == 0, s"Initial DONE should be 0, got $status0")
      assert(!dut.io.intr.toBoolean, "Initial intr should be low")
      println("  [OK] Initial: BUSY=0, DONE=0, intr=low")

      // 写入一个小 tile 数据到 SDPRAM（1x1 tile 用于快速测试）
      writeSdpramWord(dut, 0, genWord(0, 0))
      dut.clockDomain.waitSampling(5)

      // 启动 DMA 传输（1x1 tile，Local → DDR）
      startDma(dut,
        matBase = srcBase, localBase = 0,
        matCols = N, matRows = N,
        tileCols = 1, tileRows = 1,
        tileColPos = 0, tileRowPos = 0,
        direction = 1
      )

      // 启动后应该立即 busy（可能需要 1 拍延迟）
      dut.clockDomain.waitSampling(2)
      val status1 = axiLiteRead(dut, 0x04)
      assert((status1 & 0x1) == 1, s"After start: BUSY should be 1, got $status1")
      assert(!dut.io.intr.toBoolean, "During transfer: intr should still be low")
      println("  [OK] After start: BUSY=1, intr=low")

      // 等待完成
      val cycles = waitDmaDone(dut)
      // waitDmaDone 已经清除了中断

      // 中断清除后：检查 DONE=0, BUSY=0, intr=low
      dut.clockDomain.waitSampling(2)
      val status2 = axiLiteRead(dut, 0x04)
      assert((status2 & 0x1) == 0, s"After done+clear: BUSY should be 0, got $status2")
      assert((status2 & 0x2) == 0, s"After done+clear: DONE should be 0, got $status2")
      assert(!dut.io.intr.toBoolean, "After clear: intr should be low")
      println(s"  [OK] After done+clear: BUSY=0, DONE=0, intr=low (took $cycles cycles)")

      // 再做一次传输，但这次不立即清除中断，验证中断持续挂起
      writeSdpramWord(dut, 0, genWord(0, 1))
      dut.clockDomain.waitSampling(5)
      startDma(dut,
        matBase = srcBase, localBase = 0,
        matCols = N, matRows = N,
        tileCols = 1, tileRows = 1,
        tileColPos = 1, tileRowPos = 0,
        direction = 1
      )
      // 等待 intr 变高（不用 waitDmaDone，手动等）
      var waitCycles = 0
      while (!dut.io.intr.toBoolean && waitCycles < 200000) {
        dut.clockDomain.waitSampling()
        waitCycles += 1
      }
      assert(dut.io.intr.toBoolean, "Interrupt should be asserted after DMA done")

      // 验证中断持续挂起（电平触发）
      dut.clockDomain.waitSampling(10)
      assert(dut.io.intr.toBoolean, "Interrupt should remain asserted until cleared")
      val status3 = axiLiteRead(dut, 0x04)
      assert((status3 & 0x2) != 0, "DONE bit should be 1 while interrupt pending")
      assert((status3 & 0x1) == 0, "BUSY should be 0 after completion")
      println("  [OK] Interrupt stays asserted until explicitly cleared")

      // 清除中断
      axiLiteWrite(dut, 0x08, 0x1)
      dut.clockDomain.waitSampling(2)
      assert(!dut.io.intr.toBoolean, "Interrupt should be low after clear")
      val status4 = axiLiteRead(dut, 0x04)
      assert((status4 & 0x2) == 0, "DONE bit should be 0 after clear")
      println("  [OK] After explicit clear: DONE=0, intr=low")

      println("[Phase 0] Interrupt behavior test PASSED!\n")
    }

    // =================================================================
    // Phase 1: 预填充源矩阵到 DDR
    //   对每个 tile：生成数据写入 SDPRAM，DMA 搬到 DDR 源位置
    //   边缘 tile 自动由硬件裁剪
    //   ★ 优化：矩阵参数只配一次，每 tile 仅 3 次 AXI-Lite 写
    // =================================================================
    println(s"\n[Phase 1] Pre-filling source DDR matrix ($totalTiles tiles)...")
    var phase1Cycles = 0L
    // 一次性配置源矩阵参数（6 次 AXI-Lite 写）
    setupDmaMatrix(dut,
      matBase  = srcBase, localBase = 0,
      matCols  = N, matRows = N,
      tileCols = T, tileRows = T
    )
    for (tileIdx <- 0 until totalTiles) {
      val tr = tileIdx / tilesPerDim
      val tc = tileIdx % tilesPerDim
      val colPos = tc * T
      val rowPos = tr * T
      val aCols = tileActualSize(colPos, T, N)
      val aRows = tileActualSize(rowPos, T, N)

      // 生成 tile 数据并写入 SDPRAM（紧凑布局：actualCols × actualRows）
      for (r <- 0 until aRows; c <- 0 until aCols) {
        writeSdpramWord(dut, r * aCols + c, genWord(rowPos + r, colPos + c))
      }
      dut.clockDomain.waitSampling(5)

      // DMA: BRAM → DDR src — 每 tile 仅 3 次写（tileColPos, tileRowPos, CTRL）
      startTileDma(dut, tileColPos = colPos, tileRowPos = rowPos, direction = 1)
      val c2 = waitDmaDone(dut)
      phase1Cycles += c2

      // 可选：读回 ACTUAL_COLS/ACTUAL_ROWS 验证硬件裁剪
      if (tileIdx == 0 || (aCols != T || aRows != T)) {
        val hwActualCols = axiLiteRead(dut, 0x2C).toInt
        val hwActualRows = axiLiteRead(dut, 0x30).toInt
        assert(hwActualCols == aCols, s"ACTUAL_COLS mismatch: hw=$hwActualCols sw=$aCols")
        assert(hwActualRows == aRows, s"ACTUAL_ROWS mismatch: hw=$hwActualRows sw=$aRows")
        if (aCols != T || aRows != T) {
          println(f"  Edge tile ($tr%d,$tc%d) actual=${aCols}x${aRows} (nominal ${T}x$T)")
        }
      }

      if (tileIdx % 128 == 0 || tileIdx == totalTiles - 1) {
        println(f"  Tile $tileIdx%4d/$totalTiles ($tr%2d,$tc%2d) ${aCols}x${aRows}  [${elapsed()}]")
      }
    }
    println(s"[Phase 1] Done. $totalTiles tiles written to source DDR. Total cycles: $phase1Cycles  [${elapsed()}]\n")

    // =================================================================
    // Phase 2: 分块搬运 src → BRAM → dst
    //   DMA1: DDR src → BRAM（提取 tile）
    //   DMA2: BRAM → DDR dst（拼合 tile）
    //   ★ 优化：切换 src/dst 矩阵时才重配 matBase，同矩阵内仅 3 次写/tile
    // =================================================================
    println(s"[Phase 2] Copying source to destination via BRAM ($totalTiles tiles x 2 DMAs)...")
    var phase2Cycles = 0L
    for (tileIdx <- 0 until totalTiles) {
      val tr = tileIdx / tilesPerDim
      val tc = tileIdx % tilesPerDim
      val colPos = tc * T
      val rowPos = tr * T
      val aCols = tileActualSize(colPos, T, N)
      val aRows = tileActualSize(rowPos, T, N)

      // DMA1: DDR src → BRAM（提取 tile）
      // 切换到 src 矩阵：只需更新 matBase（其他参数不变）
      axiLiteWrite(dut, 0x0C, srcBase)
      startTileDma(dut, tileColPos = colPos, tileRowPos = rowPos, direction = 0)
      phase2Cycles += waitDmaDone(dut)

      // DMA2: BRAM → DDR dst（拼合 tile）
      // 切换到 dst 矩阵：只需更新 matBase
      axiLiteWrite(dut, 0x0C, dstBase)
      startTileDma(dut, tileColPos = colPos, tileRowPos = rowPos, direction = 1)
      phase2Cycles += waitDmaDone(dut)

      if (tileIdx % 128 == 0 || tileIdx == totalTiles - 1) {
        println(f"  Tile $tileIdx%4d/$totalTiles ($tr%2d,$tc%2d) ${aCols}x${aRows}  [${elapsed()}]")
      }
    }
    println(s"[Phase 2] Done. $totalTiles tiles copied. Total cycles: $phase2Cycles  [${elapsed()}]\n")

    // =================================================================
    // Phase 3: 验证源矩阵 DDR 数据完整性
    // =================================================================
    println(s"[Phase 3] Verifying source DDR matrix integrity ($totalTiles tiles)...")
    var srcErrors = 0
    var phase3Cycles = 0L
    // 配置 src 矩阵参数，direction=0 (DDR→Local)
    setupDmaMatrix(dut,
      matBase  = srcBase, localBase = 0,
      matCols  = N, matRows = N,
      tileCols = T, tileRows = T
    )
    for (tileIdx <- 0 until totalTiles) {
      val tr = tileIdx / tilesPerDim
      val tc = tileIdx % tilesPerDim
      val colPos = tc * T
      val rowPos = tr * T
      val aCols = tileActualSize(colPos, T, N)
      val aRows = tileActualSize(rowPos, T, N)

      startTileDma(dut, tileColPos = colPos, tileRowPos = rowPos, direction = 0)
      phase3Cycles += waitDmaDone(dut)
      dut.clockDomain.waitSampling(10)

      // 验证 BRAM 中的数据与预期一致（紧凑布局）
      for (r <- 0 until aRows; c <- 0 until aCols) {
        val actual   = readSdpramWord(dut, r * aCols + c)
        val expected = genWord(rowPos + r, colPos + c)
        if (actual != expected) {
          if (srcErrors < 20) {
            println(s"  SRC MISMATCH tile($tr,$tc)[$r][$c]: " +
              s"got 0x${actual.toString(16).take(16)}..., expected 0x${expected.toString(16).take(16)}...")
          }
          srcErrors += 1
        }
      }

      if (tileIdx % 128 == 0 || tileIdx == totalTiles - 1) {
        println(f"  Verified tile $tileIdx%4d/$totalTiles  errors so far: $srcErrors  [${elapsed()}]")
      }
    }
    if (srcErrors == 0) {
      println(s"  [PASS] Source matrix: all ${N.toLong * N} words intact!")
    } else {
      println(s"  [FAIL] Source matrix: $srcErrors mismatches out of ${N.toLong * N}")
    }
    println(s"[Phase 3] Done. Cycles: $phase3Cycles  [${elapsed()}]\n")

    // =================================================================
    // Phase 4: 验证目标矩阵 DDR 数据
    // =================================================================
    println(s"[Phase 4] Verifying destination DDR matrix ($totalTiles tiles)...")
    var dstErrors = 0
    var phase4Cycles = 0L
    // 配置 dst 矩阵参数
    setupDmaMatrix(dut,
      matBase  = dstBase, localBase = 0,
      matCols  = N, matRows = N,
      tileCols = T, tileRows = T
    )
    for (tileIdx <- 0 until totalTiles) {
      val tr = tileIdx / tilesPerDim
      val tc = tileIdx % tilesPerDim
      val colPos = tc * T
      val rowPos = tr * T
      val aCols = tileActualSize(colPos, T, N)
      val aRows = tileActualSize(rowPos, T, N)

      startTileDma(dut, tileColPos = colPos, tileRowPos = rowPos, direction = 0)
      phase4Cycles += waitDmaDone(dut)
      dut.clockDomain.waitSampling(10)

      // 验证 BRAM 中的数据与源矩阵预期一致（紧凑布局）
      for (r <- 0 until aRows; c <- 0 until aCols) {
        val actual   = readSdpramWord(dut, r * aCols + c)
        val expected = genWord(rowPos + r, colPos + c)
        if (actual != expected) {
          if (dstErrors < 20) {
            println(s"  DST MISMATCH tile($tr,$tc)[$r][$c]: " +
              s"got 0x${actual.toString(16).take(16)}..., expected 0x${expected.toString(16).take(16)}...")
          }
          dstErrors += 1
        }
      }

      if (tileIdx % 128 == 0 || tileIdx == totalTiles - 1) {
        println(f"  Verified tile $tileIdx%4d/$totalTiles  errors so far: $dstErrors  [${elapsed()}]")
      }
    }
    if (dstErrors == 0) {
      println(s"  [PASS] Destination matrix: all ${N.toLong * N} words match source!")
    } else {
      println(s"  [FAIL] Destination matrix: $dstErrors mismatches out of ${N.toLong * N}")
    }
    println(s"[Phase 4] Done. Cycles: $phase4Cycles  [${elapsed()}]\n")

    // =================================================================
    // 总结
    // =================================================================
    val totalErrors = srcErrors + dstErrors
    val totalCycles = phase1Cycles + phase2Cycles + phase3Cycles + phase4Cycles
    println("=" * 60)
    println(s"Matrix size:  ${N}x${N} = ${N.toLong * N} words ($matSizeMB MB)")
    println(s"Tile size:    ${T}x${T} nominal")
    if (edgeCols != 0 || edgeRows != 0)
      println(s"Edge tile:    ${if (edgeCols == 0) T else edgeCols} cols, ${if (edgeRows == 0) T else edgeRows} rows")
    println(s"Total tiles:  $totalTiles ($tilesPerDim x $tilesPerDim)")
    println(s"Total cycles: $totalCycles")
    println(s"Phase 1 (fill src):   $phase1Cycles cycles")
    println(s"Phase 2 (copy):       $phase2Cycles cycles")
    println(s"Phase 3 (verify src): $phase3Cycles cycles ($srcErrors errors)")
    println(s"Phase 4 (verify dst): $phase4Cycles cycles ($dstErrors errors)")
    println(s"Elapsed wall time:    ${elapsed()}")
    println("=" * 60)
    if (totalErrors == 0) {
      println(s"[ALL PASS] ${N}x${N} full tile transfer test passed (edge-aware)!")
    } else {
      println(s"[FAIL] Total $totalErrors errors")
    }

    simSuccess()
  }
}

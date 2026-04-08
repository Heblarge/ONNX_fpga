package runtime.dispatch.system

import WrapForFPGA._
import DMA._
import Accelerator._
import Interface._

import spinal.core._
import spinal.core.sim._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.amba4.axilite._

import java.io.File
import java.util.concurrent.{ConcurrentLinkedQueue, CountDownLatch, LinkedBlockingQueue}
import java.util.concurrent.atomic.AtomicBoolean

// =============================================================================
// SimContext — 持久化 Verilator 仿真上下文
//
// 在后台线程中运行 SystemWrapper 的 Verilator 仿真，通过线程安全的命令/响应
// 队列与主线程通信。仿真状态（DDR 内容、缓存、核心流水线）在整个计算图
// 执行期间持续保持。
//
// 使用方式：
//   val ctx = new SimContext(sysCfg)
//   ctx.start()
//   ctx.submit(WriteMatrixToDdr(...))
//   ctx.submit(ConfigureDma(...))
//   ...
//   ctx.shutdown()
// =============================================================================

// ========================= 命令定义 =========================

sealed trait SimCommand
sealed trait SimResult

case class WriteMatrixToDdr(
  ddrBase: Long, matrix: Array[Array[Int]],
  rows: Int, cols: Int
) extends SimCommand

case class ReadMatrixFromDdr(
  ddrBase: Long, rows: Int, cols: Int
) extends SimCommand

case class AxiLiteWriteCmd(
  port: String, addr: Long, data: Long
) extends SimCommand

case class AxiLiteReadCmd(
  port: String, addr: Long
) extends SimCommand

case class StartDmaCmd(
  port: String,
  matBase: Long, localBase: Long,
  matCols: Int, matRows: Int,
  tileCols: Int, tileRows: Int,
  tileColPos: Int, tileRowPos: Int,
  direction: Int
) extends SimCommand

case class WaitDmaIntrCmd(
  port: String, maxCycles: Int = 50000000
) extends SimCommand

case class ClearDmaIntrCmd(port: String) extends SimCommand

case class SetCacheLifeCfgCmd(
  channel: String, value: Int
) extends SimCommand

case object ClearCacheGlobalIntrCmd extends SimCommand

case class SendInstructionCmd(inst: InstSim) extends SimCommand

case class WaitGlobalIntrCmd(maxCycles: Int = 50000000) extends SimCommand

case class WaitCyclesCmd(n: Int) extends SimCommand

case object GetCycleCountCmd extends SimCommand

case object ShutdownCmd extends SimCommand

// ========================= 响应定义 =========================

case object OkResult extends SimResult
case class MatrixResult(matrix: Array[Array[Int]]) extends SimResult
case class LongResult(value: Long) extends SimResult
case class ErrorResult(msg: String) extends SimResult

// =============================================================================
// SimContext 实现
// =============================================================================

class SimContext(
  val sysCfg: SystemWrapperConfig,
  simWorkspacePath: String = "simWorkspace/SystemDispatcher",
  val period: Int = 10
) {
  val sideNum: Int         = sysCfg.fpgaCfg.systolicArraySideNum
  val memElementWidth: Int = 32
  val bytesPerBeat: Int    = sideNum * memElementWidth / 8
  val byteOffset: Int      = (math.log(bytesPerBeat.toDouble) / math.log(2.0)).toInt
  val ddrAddrWidth: Int    = sysCfg.ddrAddrWidth
  val dmaCfg: StrideDmaConfig = StrideDmaConfig(ddrAddrWidth = ddrAddrWidth)

  private val elemMask = (BigInt(1) << memElementWidth) - 1
  private val signBit  = BigInt(1) << (memElementWidth - 1)

  // 事件队列
  private val cmdQueue = new LinkedBlockingQueue[SimCommand](1)
  private val resQueue = new LinkedBlockingQueue[SimResult](1)
  private val running  = new AtomicBoolean(true)
  private val ready    = new CountDownLatch(1)

  @volatile var globalCycleCount: Long = 0L

  // ====================== 编译 DUT ======================

  private val thirdPartyDir = new File("hw/third_party").getAbsolutePath
  private val dpiDir        = new File("hw/third_party/dpi").getAbsolutePath
  private val ramulator2Dir = new File("hw/third_party/ramulator2").getAbsolutePath

  lazy val compiled: SimCompiled[SystemWrapper] = {
    new File(simWorkspacePath).mkdirs()
    SimConfig.workspacePath(simWorkspacePath)
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
        SystemWrapper(sysCfg)
      }
  }

  // ====================== 仿真线程 ======================

  private var simThread: Thread = _

  def start(): Unit = {
    // 确保 ramulator 配置
    setupRamulatorConfig()
    // 触发编译
    val _ = compiled

    simThread = new Thread(() => {
      try {
        compiled.doSimUntilVoid { dut =>
          SimTimeout(Long.MaxValue / 2)
          dut.clockDomain.forkStimulus(period)
          dut.core.clkCore.forkStimulus(4 * period)

          // 初始化所有接口
          initAxiLite(dut, dut.io.sAxi4LiteInst)
          initAxiLite(dut, dut.io.sAxi4LiteCache)
          initAxiLite(dut, dut.io.ctrlDmaA)
          initAxiLite(dut, dut.io.ctrlDmaB)
          initAxiLite(dut, dut.io.ctrlDmaZ)
          initHostAxi(dut, dut.io.hostAxi)

          dut.clockDomain.waitSampling(50)
          ready.countDown()

          // 周期计数器
          fork {
            while (true) {
              dut.clockDomain.waitSampling()
              globalCycleCount += 1
            }
          }

          // 命令处理循环
          // 注意：不使用 running 标志控制循环退出，避免与 shutdown() 产生竞态
          // 仅当收到 ShutdownCmd 时才退出循环
          fork {
            var alive = true
            while (alive) {
              val cmd = cmdQueue.poll()
              if (cmd != null) {
                val result = try {
                  executeCommand(dut, cmd)
                } catch {
                  case e: Throwable =>
                    ErrorResult(s"SimContext error: ${e.getMessage}")
                }
                resQueue.put(result)
                if (cmd == ShutdownCmd) {
                  alive = false
                }
              } else {
                dut.clockDomain.waitSampling()
              }
            }
            simSuccess()
          }
        }
      } catch {
        case _: Throwable =>
          if (ready.getCount > 0) ready.countDown()
      }
    }, "SimContext-Verilator")
    simThread.setDaemon(true)
    simThread.start()
    ready.await()
    println(s"[SimContext] Verilator simulation ready " +
      s"(sideNum=$sideNum, ddrAddr=${ddrAddrWidth}b, cache=${sysCfg.cacheAddrWidth}b)")
  }

  /** 提交命令并等待结果 */
  def submit(cmd: SimCommand): SimResult = {
    cmdQueue.put(cmd)
    resQueue.take()
  }

  /** 提交命令，成功返回 true，错误抛异常 */
  def submitChecked(cmd: SimCommand): SimResult = {
    val r = submit(cmd)
    r match {
      case ErrorResult(msg) =>
        throw new RuntimeException(s"[SimContext] Command failed: $msg")
      case other => other
    }
  }

  def shutdown(): Unit = {
    if (running.compareAndSet(true, false)) {
      try {
        submit(ShutdownCmd)  // sim 线程仍在循环中，会处理此命令
      } catch {
        case _: Throwable => // 忽略 submit 异常（sim 线程可能已退出）
      }
      simThread.join(10000)
      println("[SimContext] Simulation shutdown")
    }
  }

  // ====================== 初始化辅助 ======================

  private def initAxiLite(dut: SystemWrapper, axi: AxiLite4): Unit = {
    axi.aw.valid #= false; axi.aw.addr #= 0
    axi.w.valid #= false; axi.w.data #= 0; axi.w.strb #= 0
    axi.b.ready #= true
    axi.ar.valid #= false; axi.ar.addr #= 0
    axi.r.ready #= true
  }

  private def initHostAxi(dut: SystemWrapper, hostAxi: Axi4): Unit = {
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
  }

  // ====================== ramulator 配置 ======================

  private def setupRamulatorConfig(): Unit = {
    val thirdParty = new File("hw/third_party")
    val ramCfgSrc  = new File(thirdParty, "ramulator_config.yaml").toPath
    val targetDir  = new File(s"$simWorkspacePath/SystemWrapper")
    targetDir.mkdirs()
    val ramCfgDst = new File(targetDir, "ramulator_config.yaml")
    if (!ramCfgDst.exists()) java.nio.file.Files.copy(ramCfgSrc, ramCfgDst.toPath)
    new File(targetDir, "trace").mkdirs()
    val rootCfgDst = new File("ramulator_config.yaml")
    if (!rootCfgDst.exists()) {
      try { java.nio.file.Files.copy(ramCfgSrc, rootCfgDst.toPath) }
      catch { case _: java.nio.file.FileAlreadyExistsException => }
    }
    new File("trace").mkdirs()
  }

  // ====================== 命令执行 ======================

  private def executeCommand(dut: SystemWrapper, cmd: SimCommand): SimResult = cmd match {

    case WriteMatrixToDdr(ddrBase, matrix, rows, cols) =>
      val beatsPerRow = cols / sideNum
      for (row <- 0 until rows; bc <- 0 until beatsPerRow) {
        val addr = ddrBase + (row.toLong * beatsPerRow + bc) * bytesPerBeat
        val data = packMatrixBeat(matrix, row, bc)
        hostWriteBeat(dut, addr, data)
      }
      OkResult

    case ReadMatrixFromDdr(ddrBase, rows, cols) =>
      val beatsPerRow = cols / sideNum
      val result = Array.ofDim[Int](rows, cols)
      for (row <- 0 until rows; bc <- 0 until beatsPerRow) {
        val addr = ddrBase + (row.toLong * beatsPerRow + bc) * bytesPerBeat
        val beat = hostReadBeat(dut, addr)
        for (elem <- 0 until sideNum) {
          val col = bc * sideNum + elem
          if (col < cols) {
            val bits = (beat >> (elem * memElementWidth)) & elemMask
            result(row)(col) = if ((bits & signBit) != 0)
              (bits | ((BigInt(-1) >> memElementWidth) << memElementWidth)).toInt
            else bits.toInt
          }
        }
      }
      MatrixResult(result)

    case AxiLiteWriteCmd(port, addr, data) =>
      val axi = resolveAxiLitePort(dut, port)
      axiLiteWrite(dut, axi, addr, data)
      OkResult

    case AxiLiteReadCmd(port, addr) =>
      val axi = resolveAxiLitePort(dut, port)
      val data = axiLiteRead(dut, axi, addr)
      LongResult(data)

    case StartDmaCmd(port, matBase, localBase, matCols, matRows,
                     tileCols, tileRows, tileColPos, tileRowPos, direction) =>
      val axi = resolveDmaCtrlPort(dut, port)
      axiLiteWrite(dut, axi, dmaCfg.REG_MAT_BASE, matBase & 0xFFFFFFFFL)
      if (ddrAddrWidth > 32) {
        axiLiteWrite(dut, axi, dmaCfg.REG_MAT_BASE + 4, (matBase >>> 32) & 0xFFFFFFFFL)
      }
      axiLiteWrite(dut, axi, dmaCfg.REG_LOCAL_BASE, localBase)
      axiLiteWrite(dut, axi, dmaCfg.REG_MAT_COLS, matCols.toLong)
      axiLiteWrite(dut, axi, dmaCfg.REG_MAT_ROWS, matRows.toLong)
      axiLiteWrite(dut, axi, dmaCfg.REG_TILE_COLS, tileCols.toLong)
      axiLiteWrite(dut, axi, dmaCfg.REG_TILE_ROWS, tileRows.toLong)
      axiLiteWrite(dut, axi, dmaCfg.REG_TILE_COL_POS, tileColPos.toLong)
      axiLiteWrite(dut, axi, dmaCfg.REG_TILE_ROW_POS, tileRowPos.toLong)
      axiLiteWrite(dut, axi, dmaCfg.REG_CTRL, ((direction << 1) | 0x1).toLong)
      OkResult

    case WaitDmaIntrCmd(port, maxCycles) =>
      val intrSig = resolveDmaIntr(dut, port)
      var cnt = 0
      while (!intrSig.toBoolean && cnt < maxCycles) {
        dut.clockDomain.waitSampling()
        cnt += 1
      }
      if (cnt >= maxCycles) ErrorResult(s"DMA $port: Timeout after $cnt cycles")
      else OkResult

    case ClearDmaIntrCmd(port) =>
      val axi = resolveDmaCtrlPort(dut, port)
      axiLiteWrite(dut, axi, dmaCfg.REG_INTR_CLR, 0x1)
      OkResult

    case SetCacheLifeCfgCmd(channel, value) =>
      val addr = channel match {
        case "A" => 0x00L
        case "B" => 0x04L
        case _   => throw new IllegalArgumentException(s"Unknown cache channel: $channel")
      }
      axiLiteWrite(dut, dut.io.sAxi4LiteCache, addr, value.toLong)
      OkResult

    case ClearCacheGlobalIntrCmd =>
      axiLiteWrite(dut, dut.io.sAxi4LiteCache, 0x0C, 0x1)
      OkResult

    case SendInstructionCmd(inst) =>
      sendInstruction(dut, inst)
      OkResult

    case WaitGlobalIntrCmd(maxCycles) =>
      var cnt = 0
      while (!dut.io.globalIntr.toBoolean && cnt < maxCycles) {
        dut.clockDomain.waitSampling()
        cnt += 1
      }
      if (cnt >= maxCycles) ErrorResult(s"Global interrupt timeout after $cnt cycles")
      else OkResult

    case WaitCyclesCmd(n) =>
      dut.clockDomain.waitSampling(n)
      OkResult

    case GetCycleCountCmd =>
      LongResult(globalCycleCount)

    case ShutdownCmd =>
      OkResult
  }

  // ====================== AXI 低层操作 ======================

  private def axiLiteWrite(dut: SystemWrapper, axi: AxiLite4, addr: Long, data: Long): Unit = {
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

  private def axiLiteRead(dut: SystemWrapper, axi: AxiLite4, addr: Long): Long = {
    axi.ar.valid #= true; axi.ar.addr #= addr
    while (!axi.ar.ready.toBoolean) dut.clockDomain.waitSampling()
    dut.clockDomain.waitSampling()
    axi.ar.valid #= false
    while (!axi.r.valid.toBoolean) dut.clockDomain.waitSampling()
    val data = axi.r.data.toLong
    dut.clockDomain.waitSampling()
    data
  }

  private def hostWriteBeat(dut: SystemWrapper, addr: Long, data: BigInt): Unit = {
    val hostAxi = dut.io.hostAxi
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

  private def hostReadBeat(dut: SystemWrapper, addr: Long): BigInt = {
    val hostAxi = dut.io.hostAxi
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

  // ====================== 指令编码 ======================

  private def sendInstruction(dut: SystemWrapper, inst: InstSim): Unit = {
    val fpgaCfg   = sysCfg.fpgaCfg
    val ShiftWidth = fpgaCfg.slicerCfg.ShiftWidth
    val ShapeWidth = fpgaCfg.ShapeWidth
    val UIDWidth   = fpgaCfg.UIDWidth

    var bits = BigInt(0); var offset = 0
    def appendField(value: BigInt, width: Int): Unit = {
      bits |= (value & ((BigInt(1) << width) - 1)) << offset
      offset += width
    }
    appendField(BigInt(inst.UID), UIDWidth)
    appendField(BigInt(inst.matrixOperation.position), 2)
    appendField(BigInt(inst.shiftLeft_AfterMatrixOperation) & ((BigInt(1) << ShiftWidth) - 1), ShiftWidth)
    appendField(if (inst.doTranspose) BigInt(1) else BigInt(0), 1)
    appendField(BigInt(inst.activationFunction.position), 3)
    appendField(BigInt(inst.shiftLeft_AfterActivation) & ((BigInt(1) << ShiftWidth) - 1), ShiftWidth)
    appendField(BigInt(inst.input0Shape0), ShapeWidth)
    appendField(BigInt(inst.input0Shape1), ShapeWidth)
    appendField(BigInt(inst.input1Shape0), ShapeWidth)
    appendField(BigInt(inst.input1Shape1), ShapeWidth)
    appendField(BigInt(inst.shiftLeft_A) & ((BigInt(1) << ShiftWidth) - 1), ShiftWidth)
    appendField(BigInt(inst.shiftLeft_B) & ((BigInt(1) << ShiftWidth) - 1), ShiftWidth)

    val axiInst = dut.io.sAxi4LiteInst
    val mask32 = (BigInt(1) << 32) - 1
    axiLiteWrite(dut, axiInst, 0x00, ((bits >>  0) & mask32).toLong)
    axiLiteWrite(dut, axiInst, 0x04, ((bits >> 32) & mask32).toLong)
    axiLiteWrite(dut, axiInst, 0x08, ((bits >> 64) & mask32).toLong)
    axiLiteWrite(dut, axiInst, 0x0C, ((bits >> 96) & mask32).toLong)
    axiLiteWrite(dut, axiInst, 0x10, 0x1)
  }

  // ====================== 矩阵打包/解包 ======================

  private def packMatrixBeat(mat: Array[Array[Int]], row: Int, beatCol: Int): BigInt = {
    var data = BigInt(0)
    for (elem <- 0 until sideNum) {
      val col = beatCol * sideNum + elem
      val value = if (col < mat(row).length) mat(row)(col) else 0
      data |= (BigInt(value) & elemMask) << (elem * memElementWidth)
    }
    data
  }

  // ====================== 端口解析 ======================

  private def resolveAxiLitePort(dut: SystemWrapper, port: String): AxiLite4 = port match {
    case "inst"  => dut.io.sAxi4LiteInst
    case "cache" => dut.io.sAxi4LiteCache
    case "dmaA"  => dut.io.ctrlDmaA
    case "dmaB"  => dut.io.ctrlDmaB
    case "dmaZ"  => dut.io.ctrlDmaZ
    case _ => throw new IllegalArgumentException(s"Unknown AXI-Lite port: $port")
  }

  private def resolveDmaCtrlPort(dut: SystemWrapper, port: String): AxiLite4 = port match {
    case "A" => dut.io.ctrlDmaA
    case "B" => dut.io.ctrlDmaB
    case "Z" => dut.io.ctrlDmaZ
    case _ => throw new IllegalArgumentException(s"Unknown DMA port: $port")
  }

  private def resolveDmaIntr(dut: SystemWrapper, port: String): Bool = port match {
    case "A" => dut.io.intrDmaA
    case "B" => dut.io.intrDmaB
    case "Z" => dut.io.intrDmaZ
    case _ => throw new IllegalArgumentException(s"Unknown DMA interrupt: $port")
  }
}

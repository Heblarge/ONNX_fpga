package WrapForFPGA

import Accelerator._
import Tiling._
import Interface._
import DataPump._
import MatrixCacheInterface._
import DMA._

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.amba4.axilite._
import spinal.lib.bus.misc.SizeMapping

// =============================================================================
// SystemWrapper — 完整系统级顶层（含 DRAM + 3 DMA + MatrixCache + 加速器核心）
//
// 架构：
//   DRAM (ramulator2 DPI)
//       ↕ AXI4 (经仲裁合并 3 个 DMA 主端口)
//   DMA_A ──mLocal──▶ AxiBramCtrl_A ──memWrite──▶ CacheCtrl.writeA ──▶ SdpramA ──readA──▶ Core(Slicer)
//   DMA_B ──mLocal──▶ AxiBramCtrl_B ──memWrite──▶ CacheCtrl.writeB ──▶ SdpramB ──readB──▶ Core(Slicer)
//   DMA_Z ──mLocal──▶ AxiBramCtrl_Z ──memRead───▶ CacheCtrl.readC  ──▶ SdpramC ◀──writeC── Core(Collector)
//
// 对外接口（全部 AXI4-Lite 控制平面）：
//   - sAxi4LiteInst   : 指令输入（128 位打包指令）
//   - sAxi4LiteCache  : 缓存配置（lifecycle, status, intr clear）
//   - ctrlDmaA/B/Z    : 3 个 DMA 控制（矩阵分块语义寄存器）
//   - intrDmaA/B/Z    : 3 个 DMA 完成中断
//   - globalIntr       : 加速器输出完成中断
//
// 寄存器映射见 StrideDma.scala 和 MatrixCache.scala。
// =============================================================================

case class SystemWrapperConfig(
  fpgaCfg: FPGACfg,
  cacheAddrWidth: Int,           // MatrixCache 每 bank 字地址位宽
  ddrAddrWidth: Int = 32,        // DDR 地址位宽
  dmaMaxBurstLen: Int = 256      // DMA 最大 AXI4 突发长度
)

case class SystemWrapper(cfg: SystemWrapperConfig) extends Component {
  val fpgaCfg        = cfg.fpgaCfg
  val sideNum        = fpgaCfg.systolicArraySideNum
  val memElementWidth = 32
  val memDataWidth   = sideNum * memElementWidth  // 256 bits (8 × 32)
  val bytesPerBeat   = memDataWidth / 8           // 32 bytes
  val byteOffset     = log2Up(bytesPerBeat)       // 5

  // MatrixCacheController 内部为字地址 + bank bit：
  //   byteAddrWidth = cacheAddrWidth + byteOffset
  //   memPort 地址 = byteAddrWidth + 1 (bank bit)
  val cacheByteAddrWidth = cfg.cacheAddrWidth + byteOffset

  // DMA 配置：local 端地址位宽匹配 cache bank 的字节地址（不含 bank bit）
  // CacheCtrl 内部管理 bank 切换，DMA 只需要寻址单个 bank 的地址空间
  val localAddrWidth = cacheByteAddrWidth

  val dmaCfg = StrideDmaConfig(
    ddrAddrWidth   = cfg.ddrAddrWidth,
    localAddrWidth = localAddrWidth,
    dataWidth      = memDataWidth,
    maxBurstLen    = cfg.dmaMaxBurstLen
  )

  // =========================================================================
  // 子模块实例化
  // =========================================================================

  // 加速器核心（Slicer + SystolicArray + Activation + Collector）
  val core = WrapForFPGA(fpgaCfg)

  // 缓存控制器（双 bank ping-pong，不含内部 RAM）
  val cacheCtrl = MatrixCacheController(
    addrWidth = cfg.cacheAddrWidth,
    dataWidth = memDataWidth
  )

  // 物理 BRAM（每个 cache 的深度 = 2 × bank_depth，因为有 2 个 bank）
  val bramDepth = (1 << cfg.cacheAddrWidth) * 2
  val wordAddrWidth = cfg.cacheAddrWidth + 1  // +1 for bank bit
  val bramByteAddrWidth = wordAddrWidth + byteOffset

  val sdpramA = Sdpram(addrWidth = bramByteAddrWidth, dataWidth = memDataWidth)
  val sdpramB = Sdpram(addrWidth = bramByteAddrWidth, dataWidth = memDataWidth)
  val sdpramC = Sdpram(addrWidth = bramByteAddrWidth, dataWidth = memDataWidth)

  // 3 个 DMA 引擎
  val dmaA = StrideDma(dmaCfg)
  val dmaB = StrideDma(dmaCfg)
  val dmaZ = StrideDma(dmaCfg)

  // 3 个 AXI BRAM 控制器（AXI4 slave → MemoryPort 桥接）
  val bramCtrlA = AxiBramCtrl(AxiBramCtrlConfig(axiConfig = dmaA.localAxiCfg, memAddrWidth = localAddrWidth))
  val bramCtrlB = AxiBramCtrl(AxiBramCtrlConfig(axiConfig = dmaB.localAxiCfg, memAddrWidth = localAddrWidth))
  val bramCtrlZ = AxiBramCtrl(AxiBramCtrlConfig(axiConfig = dmaZ.localAxiCfg, memAddrWidth = localAddrWidth))

  // DRAM 仿真接口
  val dramSimCfg = DRAMSimConfig(
    addressWidth = cfg.ddrAddrWidth,
    dataWidth    = memDataWidth
  )
  val dramDpi = new DRAMSimDPIDriver(dramSimCfg)
  val dramSim = DRAMSim(dramSimCfg)

  // =========================================================================
  // IO
  // =========================================================================
  // Host AXI4 配置（仿真/SoC 宿主端口，用于 DDR 读写）
  val hostAxiCfg = Axi4Config(
    addressWidth = cfg.ddrAddrWidth,
    dataWidth    = memDataWidth,
    idWidth      = 4,
    useId        = true,
    useBurst     = true,
    useLock      = false,
    useRegion    = false,
    useQos       = false,
    useStrb      = true
  )

  val io = new Bundle {
    // 指令接口
    val sAxi4LiteInst  = slave(AxiLite4(fpgaCfg.axi4LiteInstCfg.getAxiConfig))
    // 缓存配置接口
    val sAxi4LiteCache = slave(AxiLite4(AxiLite4Config(addressWidth = 8, dataWidth = 32)))
    // 3 个 DMA 控制接口
    val ctrlDmaA = slave(AxiLite4(AxiLite4Config(addressWidth = 8, dataWidth = 32)))
    val ctrlDmaB = slave(AxiLite4(AxiLite4Config(addressWidth = 8, dataWidth = 32)))
    val ctrlDmaZ = slave(AxiLite4(AxiLite4Config(addressWidth = 8, dataWidth = 32)))
    // Host AXI4 端口（用于仿真时预加载 DDR 和读取结果）
    val hostAxi = slave(Axi4(hostAxiCfg))
    // 中断输出
    val intrDmaA   = out Bool()
    val intrDmaB   = out Bool()
    val intrDmaZ   = out Bool()
    val globalIntr = out Bool()
  }

  // =========================================================================
  // 指令接口连接
  // =========================================================================
  io.sAxi4LiteInst <> core.io.sAxi4LiteInst

  // =========================================================================
  // 缓存配置接口连接
  // =========================================================================
  io.sAxi4LiteCache <> cacheCtrl.io.axi

  // =========================================================================
  // DMA 控制接口与中断
  // =========================================================================
  io.ctrlDmaA <> dmaA.io.ctrl
  io.ctrlDmaB <> dmaB.io.ctrl
  io.ctrlDmaZ <> dmaZ.io.ctrl
  io.intrDmaA := dmaA.io.intr
  io.intrDmaB := dmaB.io.intr
  io.intrDmaZ := dmaZ.io.intr
  io.globalIntr := cacheCtrl.io.globalIntr

  // =========================================================================
  // DMA.mLocal → AxiBramCtrl → MatrixCacheController 外部 BRAM 端口
  // =========================================================================

  // DMA_A: DDR→Local（写入 cache A）
  bramCtrlA.io.axi <> dmaA.io.mLocal
  cacheCtrl.io.writeA.clk     := bramCtrlA.io.memWrite.clk
  cacheCtrl.io.writeA.rst     := bramCtrlA.io.memWrite.rst
  cacheCtrl.io.writeA.Valid   := bramCtrlA.io.memWrite.Valid
  cacheCtrl.io.writeA.Address := bramCtrlA.io.memWrite.Address
  cacheCtrl.io.writeA.Data    := bramCtrlA.io.memWrite.Data
  cacheCtrl.io.writeA.Wen     := bramCtrlA.io.memWrite.Wen
  // AxiBramCtrl_A 的 memRead 不使用（DMA_A 只写）
  bramCtrlA.io.memRead.Data   := B(0, memDataWidth bits)

  // DMA_B: DDR→Local（写入 cache B）
  bramCtrlB.io.axi <> dmaB.io.mLocal
  cacheCtrl.io.writeB.clk     := bramCtrlB.io.memWrite.clk
  cacheCtrl.io.writeB.rst     := bramCtrlB.io.memWrite.rst
  cacheCtrl.io.writeB.Valid   := bramCtrlB.io.memWrite.Valid
  cacheCtrl.io.writeB.Address := bramCtrlB.io.memWrite.Address
  cacheCtrl.io.writeB.Data    := bramCtrlB.io.memWrite.Data
  cacheCtrl.io.writeB.Wen     := bramCtrlB.io.memWrite.Wen
  // AxiBramCtrl_B 的 memRead 不使用
  bramCtrlB.io.memRead.Data   := B(0, memDataWidth bits)

  // DMA_Z: Local→DDR（从 cache C 读出）
  bramCtrlZ.io.axi <> dmaZ.io.mLocal
  cacheCtrl.io.readC.clk      := bramCtrlZ.io.memRead.clk
  cacheCtrl.io.readC.rst      := bramCtrlZ.io.memRead.rst
  cacheCtrl.io.readC.Valid     := bramCtrlZ.io.memRead.Valid
  cacheCtrl.io.readC.Address   := bramCtrlZ.io.memRead.Address
  bramCtrlZ.io.memRead.Data    := cacheCtrl.io.readC.Data
  // AxiBramCtrl_Z 的 memWrite 不使用（DMA_Z 只读）
  // 但 cacheCtrl.io.writeC 由核心写入，不由 DMA_Z 写

  // =========================================================================
  // 加速器核心 ↔ MatrixCacheController 数据路径
  // =========================================================================

  // 核心读端口 A
  cacheCtrl.io.readA.clk     := core.io.memPortA.clk
  cacheCtrl.io.readA.rst     := core.io.memPortA.rst
  cacheCtrl.io.readA.Valid   := core.io.memPortA.Valid
  cacheCtrl.io.readA.Address := core.io.memPortA.Address.resized
  core.io.memPortA.Data      := cacheCtrl.io.readA.Data

  // 核心读端口 B
  cacheCtrl.io.readB.clk     := core.io.memPortB.clk
  cacheCtrl.io.readB.rst     := core.io.memPortB.rst
  cacheCtrl.io.readB.Valid   := core.io.memPortB.Valid
  cacheCtrl.io.readB.Address := core.io.memPortB.Address.resized
  core.io.memPortB.Data      := cacheCtrl.io.readB.Data

  // 核心写端口 C（Collector 输出 → cache C）
  cacheCtrl.io.writeC.clk     := core.io.memPortZ.clk
  cacheCtrl.io.writeC.rst     := core.io.memPortZ.rst
  cacheCtrl.io.writeC.Valid   := core.io.memPortZ.Valid
  cacheCtrl.io.writeC.Address := core.io.memPortZ.Address.resized
  cacheCtrl.io.writeC.Data    := core.io.memPortZ.Data
  cacheCtrl.io.writeC.Wen     := core.io.memPortZ.Wen

  // =========================================================================
  // 缓存切换信号（核心完成一次计算后触发）
  // =========================================================================
  cacheCtrl.io.switchA := core.io.readSwitch
  cacheCtrl.io.switchB := core.io.readSwitch
  cacheCtrl.io.switchC := core.io.writeSwitch

  // =========================================================================
  // DMA 完成 → MatrixCacheController dmaDone（需边沿检测）
  // =========================================================================
  cacheCtrl.io.dmaDoneA := dmaA.io.intr
  cacheCtrl.io.dmaDoneB := dmaB.io.intr
  cacheCtrl.io.dmaDoneC := dmaZ.io.intr

  // =========================================================================
  // MatrixCacheController ↔ 物理 BRAM
  // =========================================================================
  sdpramA.io.read.clk     := cacheCtrl.io.memA_read.clk
  sdpramA.io.read.rst     := cacheCtrl.io.memA_read.rst
  sdpramA.io.read.Valid   := cacheCtrl.io.memA_read.Valid
  sdpramA.io.read.Address := cacheCtrl.io.memA_read.Address
  cacheCtrl.io.memA_read.Data := sdpramA.io.read.Data

  sdpramA.io.write.clk     := cacheCtrl.io.memA_write.clk
  sdpramA.io.write.rst     := cacheCtrl.io.memA_write.rst
  sdpramA.io.write.Valid   := cacheCtrl.io.memA_write.Valid
  sdpramA.io.write.Address := cacheCtrl.io.memA_write.Address
  sdpramA.io.write.Data    := cacheCtrl.io.memA_write.Data
  sdpramA.io.write.Wen     := cacheCtrl.io.memA_write.Wen

  sdpramB.io.read.clk     := cacheCtrl.io.memB_read.clk
  sdpramB.io.read.rst     := cacheCtrl.io.memB_read.rst
  sdpramB.io.read.Valid   := cacheCtrl.io.memB_read.Valid
  sdpramB.io.read.Address := cacheCtrl.io.memB_read.Address
  cacheCtrl.io.memB_read.Data := sdpramB.io.read.Data

  sdpramB.io.write.clk     := cacheCtrl.io.memB_write.clk
  sdpramB.io.write.rst     := cacheCtrl.io.memB_write.rst
  sdpramB.io.write.Valid   := cacheCtrl.io.memB_write.Valid
  sdpramB.io.write.Address := cacheCtrl.io.memB_write.Address
  sdpramB.io.write.Data    := cacheCtrl.io.memB_write.Data
  sdpramB.io.write.Wen     := cacheCtrl.io.memB_write.Wen

  sdpramC.io.read.clk     := cacheCtrl.io.memC_read.clk
  sdpramC.io.read.rst     := cacheCtrl.io.memC_read.rst
  sdpramC.io.read.Valid   := cacheCtrl.io.memC_read.Valid
  sdpramC.io.read.Address := cacheCtrl.io.memC_read.Address
  cacheCtrl.io.memC_read.Data := sdpramC.io.read.Data

  sdpramC.io.write.clk     := cacheCtrl.io.memC_write.clk
  sdpramC.io.write.rst     := cacheCtrl.io.memC_write.rst
  sdpramC.io.write.Valid   := cacheCtrl.io.memC_write.Valid
  sdpramC.io.write.Address := cacheCtrl.io.memC_write.Address
  sdpramC.io.write.Data    := cacheCtrl.io.memC_write.Data
  sdpramC.io.write.Wen     := cacheCtrl.io.memC_write.Wen

  // =========================================================================
  // DMA.mDdr → AXI4 合并 → 单一 DRAM
  //
  // 使用 SpinalHDL 的 Axi4CrossbarFactory 将 3 个 DMA 主端口合并。
  // 所有 3 个 DMA 映射到同一 DRAM 地址空间。
  // =========================================================================
  val ddrAxi = Axi4(dmaA.ddrAxiCfg.copy(idWidth = dmaA.ddrAxiCfg.idWidth + 2))

  val crossbar = Axi4CrossbarFactory()
  crossbar.lowLatency = true

  crossbar.addSlave(ddrAxi, SizeMapping(0, BigInt(1) << cfg.ddrAddrWidth))

  crossbar.addConnection(dmaA.io.mDdr, Seq(ddrAxi))
  crossbar.addConnection(dmaB.io.mDdr, Seq(ddrAxi))
  crossbar.addConnection(dmaZ.io.mDdr, Seq(ddrAxi))
  crossbar.addConnection(io.hostAxi, Seq(ddrAxi))

  crossbar.build()

  // DRAM 仿真接口连接
  dramSim.fromAxi4(ddrAxi)
  dramDpi.io.dram <> dramSim
}

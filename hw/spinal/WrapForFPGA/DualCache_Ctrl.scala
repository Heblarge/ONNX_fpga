package WrapForFPGA

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axilite._
import DataPump._

/** ****************************************************************************
 * MatrixCache_Ctrl Top Level (Memory Controller Version)
 *
 * 功能：
 *   - 集成 InputCache_Ctrl (A, B) 和 OutputCache_Ctrl (C)
 *   - 不实例化内部 memory，暴露 mem_read/mem_write 接口供外部连接
 *   - 保持与原 MatrixCache 相同的 AXI4-Lite 寄存器映射
 *   - 外部需要为每个 cache 实例化对应的 memory（深度为 2^(addrWidth+1)）
 *
 * =============================================================================
 * AXI4-Lite Register Map (32-bit) - 与原 MatrixCache 完全一致
 * =============================================================================
 *
 * +--------+--------------+------+--------+--------------------------------------------------------+
 * | Offset | Name         | Type | Range  | Description
 * +--------+--------------+------+--------+--------------------------------------------------------+
 * | 0x00   | LIFE_CFG_A   | RW   | [15:0] | Input Cache A 生命周期配置
 * |        |              |      |        | 设置 A 矩阵块可以被核心重复读取的次数 (N)。
 * +--------+--------------+------+--------+--------------------------------------------------------+
 * | 0x04   | LIFE_CFG_B   | RW   | [15:0] | Input Cache B 生命周期配置
 * |        |              |      |        | 设置 B 矩阵块可以被核心重复读取的次数 (N)。
 * +--------+--------------+------+--------+--------------------------------------------------------+
 * | 0x08   | STATUS       | RO   | 0      | [Cache A Available] 1 = A 有空闲 Bank，DMA 可写入; 0 = 忙
 * |        |              |      | 1      | [Cache B Available] 1 = B 有空闲 Bank，DMA 可写入; 0 = 忙
 * +--------+--------------+------+--------+--------------------------------------------------------+
 * **************************************************************************** */

case class DualCache_Ctrl(addrWidth: Int, dataWidth: Int, lifeWidth: Int = 16) extends Component {
  val io = new Bundle {
    val axi = slave(AxiLite4(addressWidth = 8, dataWidth = 32))

    // Cache A 接口 (Slave)
    val readA   = slave(MemoryReadPort_TypeDef(addrWidth, dataWidth))
    val writeA  = slave(MemoryWritePort_TypeDef(addrWidth, dataWidth))
    val switchA, dmaDoneA = in Bool()

    // Cache B 接口 (Slave)
    val readB   = slave(MemoryReadPort_TypeDef(addrWidth, dataWidth))
    val writeB  = slave(MemoryWritePort_TypeDef(addrWidth, dataWidth))
    val switchB, dmaDoneB = in Bool()

    // 外部 Memory 接口 (Master)
    // 这些接口将连接到顶层的 SdpramXilinx
    val memA_read  = master(MemoryReadPort_TypeDef(addrWidth + 1, dataWidth))
    val memA_write = master(MemoryWritePort_TypeDef(addrWidth + 1, dataWidth))
    val memB_read  = master(MemoryReadPort_TypeDef(addrWidth + 1, dataWidth))
    val memB_write = master(MemoryWritePort_TypeDef(addrWidth + 1, dataWidth))
  }

  val cacheA = InputCache_Ctrl(addrWidth, dataWidth, lifeWidth)
  val cacheB = InputCache_Ctrl(addrWidth, dataWidth, lifeWidth)

  // ============================================================
  // 1. 核心修改：将内存端口时钟同步到对应的读写端口时钟上
  // ============================================================
  // 这样 RAM 的读端口将运行在用户提供的读时钟下，确保 1-cycle latency
  io.memA_read.clk  := io.readA.clk
  io.memA_write.clk := io.writeA.clk
  io.memB_read.clk  := io.readB.clk
  io.memB_write.clk := io.writeB.clk

  // ============================================================
  // 2. 零延迟同步助手 (利用 addTag 绕过 CDC 检查)
  // ============================================================
  def syncReadSlave(ext: MemoryReadPort_TypeDef, int: MemoryReadPort_TypeDef): Unit = {
    // 地址和使能：从外部时钟域直接透传到内部逻辑接口，再由内部逻辑输出到 Master 端口
    int.Valid   := ext.Valid.addTag(crossClockDomain)
    int.Address := ext.Address.addTag(crossClockDomain)

    // 数据返回：直接从内部 Master 返回到外部 Slave 接口
    ext.Data    := int.Data.addTag(crossClockDomain)

    // 内部接口的时钟线不再由这里驱动，由 InputCache_Ctrl 内部逻辑或外层逻辑处理
    int.clk     := False
  }

  def syncWriteSlave(ext: MemoryWritePort_TypeDef, int: MemoryWritePort_TypeDef): Unit = {
    int.Valid   := ext.Valid.addTag(crossClockDomain)
    int.Address := ext.Address.addTag(crossClockDomain)
    int.Data    := ext.Data.addTag(crossClockDomain)
    int.Wen     := ext.Wen.addTag(crossClockDomain)
    int.clk     := False
  }

  // 3. 执行同步连接
  syncReadSlave(io.readA, cacheA.io.read)
  syncWriteSlave(io.writeA, cacheA.io.write)
  syncReadSlave(io.readB, cacheB.io.read)
  syncWriteSlave(io.writeB, cacheB.io.write)

  // 单 bit 控制信号（切换、DMA完成）仍通过同步器，确保控制器状态机稳定
  cacheA.io.switch  := BufferCC(io.switchA)
  cacheA.io.dmaIntr := BufferCC(io.dmaDoneA)
  cacheB.io.switch  := BufferCC(io.switchB)
  cacheB.io.dmaIntr := BufferCC(io.dmaDoneB)

  // ============================================================
  // 4. 数据通路透传
  // ============================================================
  // Cache A 通路
  io.memA_read.Valid   := cacheA.io.mem_read.Valid
  io.memA_read.Address := cacheA.io.mem_read.Address
  cacheA.io.mem_read.Data := io.memA_read.Data

  io.memA_write.Valid   := cacheA.io.mem_write.Valid
  io.memA_write.Address := cacheA.io.mem_write.Address
  io.memA_write.Data    := cacheA.io.mem_write.Data
  io.memA_write.Wen     := cacheA.io.mem_write.Wen

  // Cache B 通路
  io.memB_read.Valid   := cacheB.io.mem_read.Valid
  io.memB_read.Address := cacheB.io.mem_read.Address
  cacheB.io.mem_read.Data := io.memB_read.Data

  io.memB_write.Valid   := cacheB.io.mem_write.Valid
  io.memB_write.Address := cacheB.io.mem_write.Address
  io.memB_write.Data    := cacheB.io.mem_write.Data
  io.memB_write.Wen     := cacheB.io.mem_write.Wen

  // ============================================================
  // 5. AXI 控制与状态 (使用寄存器防止锁存器)
  // ============================================================
  val busCtrl = new AxiLite4SlaveFactory(io.axi)
  cacheA.io.lifeCfg := busCtrl.createReadAndWrite(UInt(lifeWidth bits), 0x00) init(0)
  cacheB.io.lifeCfg := busCtrl.createReadAndWrite(UInt(lifeWidth bits), 0x04) init(0)

  busCtrl.read(cacheA.io.status, 0x08, 0)
  busCtrl.read(cacheB.io.status, 0x08, 1)
}



object DualCache_Ctrl_verilog {
  import java.io.File

  def main(args: Array[String]): Unit = {
    new File("rtl/DualCache_Ctrl").mkdir() // 创建输出目录

    SpinalConfig(
      targetDirectory = "rtl/DualCache_Ctrl",
      oneFilePerComponent = false,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(DualCache_Ctrl(addrWidth = 15, dataWidth = 256, lifeWidth = 16))
      .printPruned()
      .printUnused()
  }
}
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
 * |        |              |      | 2      | [Cache C Data Valid] 1 = C 有数据可读 (Output Ready); 0 = 空
 * |        |              |      | 3      | [IRQ Pending]   1 = 输出中断挂起 (ISR 应查询此位确认)
 * |        |              |      | 4      | [Cache C Full]  1 = C 输出全满 (反压核心); 0 = 正常
 * +--------+--------------+------+--------+--------------------------------------------------------+
 * | 0x0C   | INTR_CLR     | WO   | 0      | [Interrupt Clear]
 * |        |              |      |        | 向此位写 '1' 以清除 Output Cache C 的中断信号。
 * |        |              |      |        | (Write-1-to-Clear / Pulse Generation)
 * +--------+--------------+------+--------+--------------------------------------------------------+
 *
 * **************************************************************************** */

case class MatrixCache_Ctrl(addrWidth: Int, dataWidth: Int, lifeWidth: Int = 16) extends Component {
  val io = new Bundle {
    // === 1. AXI4-Lite Config & Status Interface ===
    val axi = slave(AxiLite4(addressWidth = 8, dataWidth = 32))

    // === 2. Global Interrupt Output ===
    val globalIntr = out Bool()

    // === 3. Cache A (Input) - 用户数据接口 ===
    val readA     = slave(MemoryReadPort_TypeDef(addrWidth, dataWidth))
    val writeA    = slave(MemoryWritePort_TypeDef(addrWidth, dataWidth))
    val switchA   = in Bool()
    val dmaDoneA  = in Bool()

    // === 4. Cache B (Input) - 用户数据接口 ===
    val readB     = slave(MemoryReadPort_TypeDef(addrWidth, dataWidth))
    val writeB    = slave(MemoryWritePort_TypeDef(addrWidth, dataWidth))
    val switchB   = in Bool()
    val dmaDoneB  = in Bool()

    // === 5. Cache C (Output) - 用户数据接口 ===
    val readC     = slave(MemoryReadPort_TypeDef(addrWidth, dataWidth))
    val writeC    = slave(MemoryWritePort_TypeDef(addrWidth, dataWidth))
    val switchC   = in Bool()
    val dmaDoneC  = in Bool()

    // === 6. Memory 接口 (连接到外部 Memory) ===
    // Cache A Memory 接口
    val memA_read  = master(MemoryReadPort_TypeDef(addrWidth + 1, dataWidth))
    val memA_write = master(MemoryWritePort_TypeDef(addrWidth + 1, dataWidth))

    // Cache B Memory 接口
    val memB_read  = master(MemoryReadPort_TypeDef(addrWidth + 1, dataWidth))
    val memB_write = master(MemoryWritePort_TypeDef(addrWidth + 1, dataWidth))

    // Cache C Memory 接口
    val memC_read  = master(MemoryReadPort_TypeDef(addrWidth + 1, dataWidth))
    val memC_write = master(MemoryWritePort_TypeDef(addrWidth + 1, dataWidth))
  }

  // =============================
  // 1. 实例化子模块（纯控制器版本）
  // =============================
  val cacheA = InputCache_Ctrl(addrWidth, dataWidth, lifeWidth)
  val cacheB = InputCache_Ctrl(addrWidth, dataWidth, lifeWidth)
  val cacheC = OutputCache_Ctrl(addrWidth, dataWidth)

  // =============================
  // 2. 用户数据通路连接
  // =============================
  // Cache A 用户接口
  cacheA.io.read    <> io.readA
  cacheA.io.write   <> io.writeA
  cacheA.io.switch  <> io.switchA
  cacheA.io.dmaIntr <> io.dmaDoneA

  // Cache B 用户接口
  cacheB.io.read    <> io.readB
  cacheB.io.write   <> io.writeB
  cacheB.io.switch  <> io.switchB
  cacheB.io.dmaIntr <> io.dmaDoneB

  // Cache C 用户接口
  cacheC.io.read    <> io.readC
  cacheC.io.write   <> io.writeC
  cacheC.io.switch  <> io.switchC
  cacheC.io.dmaIntr <> io.dmaDoneC

  // =============================
  // 3. Memory 接口连接（暴露给外部）
  // =============================
  // Cache A Memory 接口
  cacheA.io.mem_read  <> io.memA_read
  cacheA.io.mem_write <> io.memA_write

  // Cache B Memory 接口
  cacheB.io.mem_read  <> io.memB_read
  cacheB.io.mem_write <> io.memB_write

  // Cache C Memory 接口
  cacheC.io.mem_read  <> io.memC_read
  cacheC.io.mem_write <> io.memC_write

  // =============================
  // 4. 硬件中断输出
  // =============================
  io.globalIntr := cacheC.io.intr

  // =============================
  // 5. AXI4-Lite 寄存器映射（保持与原 MatrixCache 一致）
  // =============================
  val busCtrl = new AxiLite4SlaveFactory(io.axi)

  // --- 寄存器 0x00: Cache A 生命周期配置 (RW) ---
  val lifeCfgRegA = Reg(UInt(lifeWidth bits)) init(0)
  busCtrl.readAndWrite(lifeCfgRegA, address = 0x00)
  cacheA.io.lifeCfg := lifeCfgRegA

  // --- 寄存器 0x04: Cache B 生命周期配置 (RW) ---
  val lifeCfgRegB = Reg(UInt(lifeWidth bits)) init(0)
  busCtrl.readAndWrite(lifeCfgRegB, address = 0x04)
  cacheB.io.lifeCfg := lifeCfgRegB

  // --- 寄存器 0x08: 状态寄存器 (Read Only) ---
  // Bit 0: Cache A Available (有空闲 Bank 可写)
  busCtrl.read(cacheA.io.status, address = 0x08, bitOffset = 0)
  // Bit 1: Cache B Available (有空闲 Bank 可写)
  busCtrl.read(cacheB.io.status, address = 0x08, bitOffset = 1)
  // Bit 2: Cache C Data Valid (有数据可读)
  busCtrl.read(cacheC.io.status, address = 0x08, bitOffset = 2)
  // Bit 3: IRQ Pending (输出中断挂起)
  busCtrl.read(cacheC.io.intr,   address = 0x08, bitOffset = 3)
  // Bit 4: Cache C Full (输出全满)
  busCtrl.read(cacheC.io.full,   address = 0x08, bitOffset = 4)

  // --- 寄存器 0x0C: 中断清除控制 (Write 1 to Clear) ---
  cacheC.io.intrClear := False
  busCtrl.setOnSet(cacheC.io.intrClear, address = 0x0C, bitOffset = 0)

  // =============================
  // 6. 仿真可见性设置
  // =============================
  // 标记此信号为 simPublic，确保 VCS/Verilator 仿真后端保留此信号
  /* 
  cacheC.io.full.simPublic()
  cacheA.io.status.simPublic()
  cacheB.io.status.simPublic()
  cacheC.io.status.simPublic()
  */
}

object MatrixCache_Ctrl_verilog {
  import java.io.File

  def main(args: Array[String]): Unit = {
    new File("rtl/MatrixCache_Ctrl").mkdir() // 创建输出目录

    SpinalConfig(
      targetDirectory = "rtl/MatrixCache_Ctrl",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(MatrixCache_Ctrl(addrWidth = 13, dataWidth = 1024, lifeWidth = 16))
      .printPruned()
      .printUnused()
  }
}
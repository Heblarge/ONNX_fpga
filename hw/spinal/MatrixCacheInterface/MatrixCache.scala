package MatrixCacheInterface
import java.io.File
import Tiling._
import DataPump._
import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axilite._
import spinal.core._
import spinal.core.sim.SimDataPimper
import spinal.lib._
import spinal.lib.bus.amba4.axilite._

/** ****************************************************************************
 * MatrixCache Top Level
 *
 * =============================================================================
 * AXI4-Lite Register Map (32-bit)
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

case class MatrixCache(addrWidth: Int, dataWidth: Int, lifeWidth: Int = 16) extends Component {
  val io = new Bundle {
    // === 1. AXI4-Lite Config & Status Interface ===
    val axi = slave(AxiLite4(addressWidth = 8, dataWidth = 32))

    // === 2. Global Interrupt Output ===
    val globalIntr = out Bool()

    // === 3. Cache A (Input) ===
    val readA     = slave(MemoryReadPort_TypeDef(addrWidth, dataWidth))
    val writeA    = slave(MemoryWritePort_TypeDef(addrWidth, dataWidth))
    val switchA   = in Bool()
    val dmaDoneA  = in Bool()

    // === 4. Cache B (Input) ===
    val readB     = slave(MemoryReadPort_TypeDef(addrWidth, dataWidth))
    val writeB    = slave(MemoryWritePort_TypeDef(addrWidth, dataWidth))
    val switchB   = in Bool()
    val dmaDoneB  = in Bool()

    // === 5. Cache C (Output) ===
    val readC     = slave(MemoryReadPort_TypeDef(addrWidth, dataWidth))
    val writeC    = slave(MemoryWritePort_TypeDef(addrWidth, dataWidth))
    val switchC   = in Bool()
    val dmaDoneC  = in Bool()
  }

  // =============================
  // 1. 实例化子模块
  // =============================
  val cacheA = InputMatrixCache(addrWidth, dataWidth, lifeWidth)
  val cacheB = InputMatrixCache(addrWidth, dataWidth, lifeWidth)
  val cacheC = OutputMatrixCache(addrWidth, dataWidth)

  // =============================
  // 2. 数据通路连接
  // =============================
  cacheA.io.read    <> io.readA
  cacheA.io.write   <> io.writeA
  cacheA.io.switch  <> io.switchA
  cacheA.io.dmaIntr <> io.dmaDoneA

  cacheB.io.read    <> io.readB
  cacheB.io.write   <> io.writeB
  cacheB.io.switch  <> io.switchB
  cacheB.io.dmaIntr <> io.dmaDoneB

  cacheC.io.read    <> io.readC
  cacheC.io.write   <> io.writeC
  cacheC.io.switch  <> io.switchC
  cacheC.io.dmaIntr <> io.dmaDoneC

  // =============================
  // 3. 硬件中断输出
  // =============================
  io.globalIntr := cacheC.io.intr

  // =============================
  // 4. AXI4-Lite 寄存器映射
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
  busCtrl.read(cacheA.io.status, address = 0x08, bitOffset = 0)
  busCtrl.read(cacheB.io.status, address = 0x08, bitOffset = 1)
  busCtrl.read(cacheC.io.status, address = 0x08, bitOffset = 2)
  busCtrl.read(cacheC.io.intr,   address = 0x08, bitOffset = 3)
  busCtrl.read(cacheC.io.full,   address = 0x08, bitOffset = 4)

  // --- 寄存器 0x0C: 中断清除控制 (Write 1 to Clear) ---
  cacheC.io.intrClear := False
  busCtrl.setOnSet(cacheC.io.intrClear, address = 0x0C, bitOffset = 0)

  // =============================
  // [关键修复] 仿真可见性设置
  // =============================
  // 标记此信号为 simPublic，确保 VCS/Verilator 仿真后端保留此信号，
  // 从而允许 Testbench 通过 dut.cacheC.io.full 访问它。
  cacheC.io.full.simPublic()
}

object MatrixCache_verilog {

  new File("rtl/MatrixCache").mkdir() // 创建输出目录

  def main(arg:Array[String]): Unit ={
    SpinalConfig(
      targetDirectory = "rtl/MatrixCache",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(MatrixCache(addrWidth=4,dataWidth=8))
      .printPruned()
  }
}

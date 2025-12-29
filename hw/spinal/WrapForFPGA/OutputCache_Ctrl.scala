package WrapForFPGA

import spinal.core._
import spinal.lib._
import DataPump._

/** ****************************************************************************
 * OutputCache_Ctrl
 * 功能：
 *   - OutputMatrixCache 的纯控制器版本，不实例化 memory
 *   - 维护两个 bank（0/1）的占用状态（valid），实现 ping-pong 双缓冲
 *   - 写端总是映射到 wrPtr 指向的 bank；读端总是映射到 rdPtr 指向的 bank
 *   - 当 switch=1（单拍）且非全满时：当前写 bank 置 valid，wrPtr 翻转
 *   - 当 dmaIntr=1 且读 bank 有效：清除读 bank valid，rdPtr 翻转
 *   - full = 两个 bank 均有效；status = 存在任意有效 bank
 *   - intr = 保持型中断，通过 intrClear 清除
 *   - memory 由外部实例化和管理
 * **************************************************************************** */
case class OutputCache_Ctrl(addrWidth: Int, dataWidth: Int) extends Component {
  val io = new Bundle {
    // 外部读写接口（连接到外部使用者）
    val read   = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val write  = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    
    // 控制信号
    val switch     = in Bool()      // 写主机完成当前 bank 写入后触发（切换到新 bank）
    val dmaIntr    = in Bool()      // 读端 DMA 完成当前 bank 读取后触发（释放读 bank）
    val intrClear  = in Bool()      // 清除中断信号
    
    // 状态输出
    val status     = out Bool()     // 存在任意有效 bank（有可读数据）
    val intr       = out Bool()     // 保持型中断（新 bank 就绪）
    val full       = out Bool()     // 两个 bank 均有效（无可写空间）
    
    // memory 接口（连接到外部 memory）
    val mem_read   = master(MemoryReadPort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
    val mem_write  = master(MemoryWritePort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
  }

  // 当前写 / 读 bank 指针
  val wrPtr = Reg(UInt(1 bits)) init(0)
  val rdPtr = Reg(UInt(1 bits)) init(0)
  
  // 中断寄存器（保持型）
  val intrReg = Reg(Bool()) init(False)
  
  // bank valid 标志：该 bank 是否存有可读数据
  val bankValid = Vec(Reg(Bool()), 2)
  bankValid.foreach(_ init(False))

  // ========================================================================
  // 状态输出
  // ========================================================================
  io.status := bankValid(0) || bankValid(1)  // 存在任意有效 bank
  io.full   := bankValid(0) && bankValid(1)  // 两个 bank 均有效
  io.intr   := intrReg                        // 保持型中断

  // ========================================================================
  // 中断逻辑（Critical Fix）
  // ========================================================================
  // 1. 清除中断（Reset）
  //    AXI setOnSet 产生一个单周期高脉冲，直接清除寄存器
  when(io.intrClear) {
    intrReg := False
  }

  // 2. 产生中断（Set）- 优先级更高！
  //    如果 Switch 和 Clear 在同一拍发生，说明刚刚清除了旧中断，但马上新数据又来了
  //    此时必须保持中断为高，否则新数据会被漏处理
  val acceptSwitch = io.switch && !io.full
  when(acceptSwitch) {
    bankValid(wrPtr) := True
    wrPtr := wrPtr + 1
    intrReg := True  // Set overrides Reset
  }

  // ========================================================================
  // 释放读 bank 逻辑：dmaIntr 表示读端 DMA 完成当前 bank 读取
  // ========================================================================
  val acceptRelease = io.dmaIntr.rise() && bankValid(rdPtr)
  when(acceptRelease) {
    bankValid(rdPtr) := False
    rdPtr := rdPtr + 1
  }

  // ========================================================================
  // 地址映射：扩展地址高位为 bank 号，保持对外地址空间不变
  // ========================================================================
  // 写地址映射
  val wrAddrInt = (wrPtr.asBits ## io.write.Address.asBits).asUInt
  io.mem_write.Valid   := io.write.Valid
  io.mem_write.Address := wrAddrInt
  io.mem_write.Data    := io.write.Data

  // 读地址映射
  val rdAddrInt = (rdPtr.asBits ## io.read.Address.asBits).asUInt
  io.mem_read.Valid   := io.read.Valid
  io.mem_read.Address := rdAddrInt
  io.read.Data := io.mem_read.Data

  /* 仿真调试接口
  wrPtr.simPublic()
  rdPtr.simPublic()
  bankValid.simPublic()
  intrReg.simPublic()*/
}
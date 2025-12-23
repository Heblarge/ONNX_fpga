package WrapForFPGA

import spinal.core._
import spinal.lib._
import DataPump._

/** ****************************************************************************
 * InputCache_Ctrl
 * 功能：
 *   - InputMatrixCache 的纯控制器版本，不实例化 memory
 *   - 双 bank ping-pong 输入缓存控制器
 *   - 写入由 DMA 通过 dmaIntr 通知完成（释放写 bank）
 *   - 读取由上游通过 switch 释放读 bank
 *   - 每次写入前通过 lifeCfg 配置该矩阵的生命周期（可被读取次数）
 *   - 内部通过生命周期计数器控制 bank 的释放
 *   - memory 由外部实例化和管理
 * **************************************************************************** */

case class InputCache_Ctrl(addrWidth: Int, dataWidth: Int, lifeWidth: Int = 16) extends Component {
  val io = new Bundle {
    // 1. 外部用户接口 (Slave)
    val read  = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val write = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))

    // 2. 控制信号
    val switch   = in Bool()      // 读完成信号
    val dmaIntr  = in Bool()      // 写完成信号
    val lifeCfg  = in UInt(lifeWidth bits) // 初始生命周期配置

    // 3. 状态输出
    val status   = out Bool()     // 是否有空闲 Bank 可写
    val empty    = out Bool()     // 是否全空

    // 4. Memory 接口 (Master) - 地址位宽 +1 用于 Bank 选择
    val mem_read  = master(MemoryReadPort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
    val mem_write = master(MemoryWritePort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
  }

  // --- 状态寄存器 ---
  val wrPtr = Reg(UInt(1 bits)) init(0)
  val rdPtr = Reg(UInt(1 bits)) init(0)

  // Bank 有效标志
  val bankValid = Vec(Reg(Bool()), 2)
  bankValid.foreach(_ init(False))

  // 每个 bank 对应的生命周期计数器
  // 修复 WIDTH MISMATCH：初始化时显式指定位宽
  val lifeCnt = Vec(Reg(UInt(lifeWidth bits)), 2)
  lifeCnt.foreach(_ init(U(0, lifeWidth bits)))

  // --- 状态逻辑 ---
  io.status := !bankValid(0) || !bankValid(1) // 任意一个 Bank 为空即表示 Ready
  io.empty  := !bankValid(0) && !bankValid(1)

  // --- 写入管理逻辑 ---
  // 使用 .rise() 检测 DMA 完成信号的上升沿
  when(io.dmaIntr.rise() && !bankValid(wrPtr)) {
    bankValid(wrPtr) := True
    lifeCnt(wrPtr)   := io.lifeCfg
    wrPtr := wrPtr + 1
  }

  // --- 读取管理逻辑 ---
  when(io.switch && bankValid(rdPtr)) {
    when(lifeCnt(rdPtr) > 1) {
      lifeCnt(rdPtr) := lifeCnt(rdPtr) - 1
    } otherwise {
      // 修复 WIDTH MISMATCH：赋值时显式指定位宽，避免被推导为 0 bits
      lifeCnt(rdPtr)  := U(0, lifeWidth bits)
      bankValid(rdPtr) := False
      rdPtr := rdPtr + 1
    }
  }

  // --- 地址映射与信号透传 ---

  // 拼接 Bank 指针作为地址最高位
  val wrAddrFull = (wrPtr ## io.write.Address).asUInt
  val rdAddrFull = (rdPtr ## io.read.Address).asUInt

  // 映射写端口
  io.mem_write.Valid   := io.write.Valid
  io.mem_write.Address := wrAddrFull
  io.mem_write.Data    := io.write.Data
  io.mem_write.Wen     := io.write.Wen    // 必须透传 Wen 信号

  // 映射读端口
  io.mem_read.Valid   := io.read.Valid
  io.mem_read.Address := rdAddrFull
  io.read.Data        := io.mem_read.Data

  // 驱动 Master 侧时钟由父模块处理，但在子模块内需确保接口完整
}


//case class InputCache_Ctrl(addrWidth: Int, dataWidth: Int, lifeWidth: Int = 16) extends Component {
//  val io = new Bundle {
//    // 外部读写接口（连接到外部使用者）
//    val read       = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
//    val write      = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
//
//    // 控制信号
//    val switch     = in Bool()      // 读主机完成当前 bank 读取后触发（一次"消费"）
//    val dmaIntr    = in Bool()      // 写端 DMA 完成当前 bank 写入后触发（写 bank 填满）
//    val lifeCfg    = in UInt(lifeWidth bits) // 配置"下一块写入矩阵"的生命周期（可被读的次数）
//
//    // 状态输出
//    val status     = out Bool()     // 是否存在可写 bank（任意 bank 空闲）
//    val empty      = out Bool()     // 两个 bank 均为空（无可读数据）
//
//    // memory 接口（连接到外部 memory）
//    val mem_read   = master(MemoryReadPort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
//    val mem_write  = master(MemoryWritePort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
//  }
//
//  // 当前写 / 读 bank 指针
//  val wrPtr = Reg(UInt(1 bits)) init(0)
//  val rdPtr = Reg(UInt(1 bits)) init(0)
//
//  // bank valid 标志：该 bank 是否存有可读矩阵
//  val bankValid = Vec(Reg(Bool()), 2)
//  bankValid.foreach(_ init(False))
//
//  // 每个 bank 对应的生命周期计数器（可被读的次数）
//  val lifeCnt = Vec(Reg(UInt(lifeWidth bits)), 2)
//  lifeCnt.foreach(_ init(0))
//
//  // 是否有空闲 bank（可供 DMA 写入）
//  val anyFree  = !bankValid(0) || !bankValid(1)
//  // 是否两个 bank 都为空（不可读）
//  val allEmpty = !bankValid(0) && !bankValid(1)
//
//  io.status := anyFree
//  io.empty  := allEmpty
//
//  // ========================================================================
//  // 写入完成逻辑：dmaIntr 拉高时，将 lifeCfg 绑定到当前 wrPtr 指向的 bank
//  // ========================================================================
//  when(io.dmaIntr.rise() && !bankValid(wrPtr)) {
//    bankValid(wrPtr) := True
//    lifeCnt(wrPtr)   := io.lifeCfg          // 捕获生命周期配置
//    wrPtr := wrPtr + 1
//  }
//
//  // ========================================================================
//  // 读取完成逻辑：switch 拉高表示消费当前 rdPtr bank 一次
//  // ========================================================================
//  when(io.switch && bankValid(rdPtr)) {
//    // 只在生命周期大于 0 时才做操作（正常情况 lifeCnt>0）
//    when(lifeCnt(rdPtr) > 1) {
//      // 还有剩余生命周期，不释放 bank，只自减计数器
//      lifeCnt(rdPtr) := lifeCnt(rdPtr) - 1
//    } otherwise {
//      // lifeCnt == 1：本次消费后生命周期归零，释放 bank
//      lifeCnt(rdPtr)   := U(0).resized
//      bankValid(rdPtr) := False
//      rdPtr := rdPtr + 1
//    }
//  }
//
//  // ========================================================================
//  // 地址映射：扩展地址高位为 bank 号，保持对外地址空间不变
//  // ========================================================================
//  val wrAddrInt = (wrPtr(0).asBits ## io.write.Address.asBits).asUInt
//  val rdAddrInt = (rdPtr(0).asBits ## io.read.Address.asBits).asUInt
//
//  // 写端口映射到 memory
//  io.mem_write.Valid   := io.write.Valid
//  io.mem_write.Address := wrAddrInt
//  io.mem_write.Data    := io.write.Data
//
//  // 读端口映射到 memory
//  io.mem_read.Valid   := io.read.Valid
//  io.mem_read.Address := rdAddrInt
//  io.read.Data := io.mem_read.Data
//
//}
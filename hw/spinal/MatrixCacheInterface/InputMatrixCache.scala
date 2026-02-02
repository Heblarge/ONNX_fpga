package MatrixCacheInterface

import spinal.core._
import spinal.lib._
import DataPump._
import MatrixCacheInterface._
import spinal.core.sim.SimDataPimper

/** ****************************************************************************
 * InputMatrixCache
 * 功能：
 *   - 双 bank ping-pong 输入缓存
 *   - 写入由 DMA 通过 dmaIntr 通知完成（释放写 bank）
 *   - 读取由上游通过 switch 释放读 bank
 *   - 每次写入前通过 lifeCfg 配置该矩阵的生命周期（可被读取次数）
 *   - 内部通过生命周期计数器控制 bank 的释放
 * **************************************************************************** */
case class InputMatrixCache(addrWidth: Int, dataWidth: Int, lifeWidth: Int = 16) extends Component {
  val io = new Bundle {
    val read       = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val write      = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val switch     = in Bool()      // 读主机完成当前 bank 读取后触发（一次“消费”）
    val dmaIntr    = in Bool()      // 写端 DMA 完成当前 bank 写入后触发（写 bank 填满）
    val lifeCfg    = in UInt(lifeWidth bits) // 配置“下一块写入矩阵”的生命周期（可被读的次数）
    val status     = out Bool()     // 是否存在可写 bank（任意 bank 空闲）
    val empty      = out Bool()     // 两个 bank 均为空（无可读数据）
  }

  // 实例化接口核心
  val inputMatrixCacheInterface = InputMatrixCacheInterface(addrWidth, dataWidth, lifeWidth)

  // 物理双端口存储模型（深度×2）
  val visibleDepth  = 1 << addrWidth
  val internalDepth = visibleDepth * 2
  val sdpramModel = SdpramModel(dataWidth = dataWidth, depth = internalDepth)

  // 连接外部端口
  io.read    <> inputMatrixCacheInterface.io.read
  io.write   <> inputMatrixCacheInterface.io.write
  io.switch  <> inputMatrixCacheInterface.io.switch
  io.dmaIntr <> inputMatrixCacheInterface.io.dmaIntr
  io.lifeCfg <> inputMatrixCacheInterface.io.lifeCfg
  io.status  <> inputMatrixCacheInterface.io.status
  io.empty   <> inputMatrixCacheInterface.io.empty

  // 内部存储连接
  inputMatrixCacheInterface.io.read_sdpram  <> sdpramModel.io.read
  inputMatrixCacheInterface.io.write_sdpram <> sdpramModel.io.write
}


case class InputMatrixCacheController(addrWidth: Int, dataWidth: Int, lifeWidth: Int = 16) extends Component {
  val io = new Bundle {
    val read       = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val write      = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val switch     = in Bool()      // 读主机完成当前 bank 读取后触发（一次“消费”）
    val dmaIntr    = in Bool()      // 写端 DMA 完成当前 bank 写入后触发（写 bank 填满）
    val lifeCfg    = in UInt(lifeWidth bits) // 配置“下一块写入矩阵”的生命周期（可被读的次数）
    val status     = out Bool()     // 是否存在可写 bank（任意 bank 空闲）
    val empty      = out Bool()     // 两个 bank 均为空（无可读数据）

    val memRead       = master(MemoryReadPort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
    val memWrite      = master(MemoryWritePort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
  }

  // 实例化接口核心
  val inputMatrixCacheInterface = InputMatrixCacheInterface(addrWidth, dataWidth, lifeWidth)

  // 物理双端口存储模型（深度×2）
  val visibleDepth  = 1 << addrWidth
  val internalDepth = visibleDepth * 2
  //val sdpramModel = SdpramModel(dataWidth = dataWidth, depth = internalDepth)

  // 连接外部端口
  io.read    <> inputMatrixCacheInterface.io.read
  io.write   <> inputMatrixCacheInterface.io.write
  io.switch  <> inputMatrixCacheInterface.io.switch
  io.dmaIntr <> inputMatrixCacheInterface.io.dmaIntr
  io.lifeCfg <> inputMatrixCacheInterface.io.lifeCfg
  io.status  <> inputMatrixCacheInterface.io.status
  io.empty   <> inputMatrixCacheInterface.io.empty

  // 内部存储连接
  inputMatrixCacheInterface.io.read_sdpram  <> io.memRead
  inputMatrixCacheInterface.io.write_sdpram <> io.memWrite
}



/** ****************************************************************************
 * InputMatrixCacheInterface
 * 功能：
 *   - 维护两个 bank（0/1）的使用状态，实现 ping-pong 双缓冲
 *   - 每个 bank 绑定一个生命周期计数器 lifeCnt(i)，表示该矩阵可被读取的次数
 *   - 写端：dmaIntr 时，如果 wrPtr 指向 bank 空闲，则：
 *       bankValid(wrPtr) := True
 *       lifeCnt(wrPtr)   := lifeCfg   // 捕获当前配置的生命周期
 *       wrPtr := wrPtr + 1
 *   - 读端：switch 时，如果 rdPtr 指向 bank 有效，则：
 *       lifeCnt(rdPtr) := lifeCnt(rdPtr) - 1
 *       当 lifeCnt 自减到 0 时才释放该 bank，并翻转 rdPtr
 *   - status = 存在空闲 bank；empty = 两个 bank 均为空；
 * **************************************************************************** */
case class InputMatrixCacheInterface(addrWidth: Int, dataWidth: Int, lifeWidth: Int = 16) extends Component {
  val io = new Bundle {
    val read   = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val write  = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val switch = in  Bool()   // 读主机一次“消费”动作（等价于读完当前矩阵一次）
    val dmaIntr= in  Bool()   // DMA 完成一次写入
    val lifeCfg= in  UInt(lifeWidth bits) // 配置下一次写入矩阵的生命周期
    val status = out Bool()   // 有空闲 bank 可写
    val empty  = out Bool()   // 无可读 bank
    val read_sdpram  = master(MemoryReadPort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
    val write_sdpram = master(MemoryWritePort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
  }

  // 当前写 / 读 bank 指针
  val wrPtr = Reg(UInt(1 bits)) init(0)
  val rdPtr = Reg(UInt(1 bits)) init(0)

  // bank valid 标志：该 bank 是否存有可读矩阵
  val bankValid = Vec(Reg(Bool()), 2)
  bankValid.foreach(_ init(False))

  // 每个 bank 对应的生命周期计数器（可被读的次数）
  val lifeCnt = Vec(Reg(UInt(lifeWidth bits)), 2)
  lifeCnt.foreach(_ init(0))

  // 是否有空闲 bank（可供 DMA 写入）
  val anyFree  = !bankValid(0) || !bankValid(1)
  // 是否两个 bank 都为空（不可读）
  val allEmpty = !bankValid(0) && !bankValid(1)

  io.status := anyFree
  io.empty  := allEmpty

  // 写入完成：dmaIntr 拉高时，将 lifeCfg 绑定到当前 wrPtr 指向的 bank
  when(io.dmaIntr.rise() && !bankValid(wrPtr)) {
    bankValid(wrPtr) := True
    lifeCnt(wrPtr)   := io.lifeCfg          // 捕获生命周期配置
    wrPtr := wrPtr + 1
  }

  // 读取完成一次：switch 拉高表示消费当前 rdPtr bank 一次
  when(io.switch && bankValid(rdPtr)) {
    // 只在生命周期大于 0 时才做操作（正常情况 lifeCnt>0）
    when(lifeCnt(rdPtr) > 1) {
      // 还有剩余生命周期，不释放 bank，只自减计数器
      lifeCnt(rdPtr) := lifeCnt(rdPtr) - 1
    } otherwise {
      // lifeCnt == 1：本次消费后生命周期归零，释放 bank
      lifeCnt(rdPtr)   := U(0).resized
      bankValid(rdPtr) := False
      rdPtr := rdPtr + 1
    }
  }

  // 地址映射：扩展地址高位为 bank 号，保持对外地址空间不变
  val wrAddrInt = (wrPtr(0).asBits ## io.write.Address.asBits).asUInt
  val rdAddrInt = (rdPtr(0).asBits ## io.read.Address.asBits).asUInt

  io.write_sdpram.Valid   := io.write.Valid
  io.write_sdpram.Address := wrAddrInt
  io.write_sdpram.Data    := io.write.Data
  io.write_sdpram.clk     := io.write.clk
  io.write_sdpram.Wen     := io.write.Wen

  io.read_sdpram.Valid   := io.read.Valid
  io.read_sdpram.clk     := io.read.clk

  io.read_sdpram.Address := rdAddrInt
  io.read.Data := io.read_sdpram.Data

  wrPtr.simPublic()
  rdPtr.simPublic()
  bankValid.simPublic()
  lifeCnt.simPublic()
}



//package MatrixCacheInterface
//
//import spinal.core._
//import spinal.lib._
//import DataPump._
//import MatrixCacheInterface._
//
///** ****************************************************************************
// * InputMatrixCache
// * - 功能：与 OutputMatrixCache 相反
// * - 由读主机控制读取（switch触发切换），写入由DMA通过dmaIntr通知完成
// * - 维护双bank ping-pong 状态，保证读写交替进行
// * - 增加 intrClear 输入信号，intr 改为保持型（高电平直至清除）
// * **************************************************************************** */
//case class InputMatrixCache(addrWidth: Int, dataWidth: Int) extends Component {
//  val io = new Bundle {
//    val read       = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
//    val write      = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
//    val switch     = in Bool()      // 读主机完成当前 bank 读取后触发
//    val dmaIntr    = in Bool()      // 写端 DMA 完成当前 bank 写入后触发（释放写 bank）
//    val status     = out Bool()     // 是否存在可写 bank（任意 bank 空闲）
//    val empty      = out Bool()     // 两个 bank 均为空（无可读数据）
//  }
//
//  // 实例化接口核心
//  val inputMatrixCacheInterface = InputMatrixCacheInterface(addrWidth, dataWidth)
//
//  // 物理双端口存储模型（深度×2）
//  val visibleDepth  = 1 << addrWidth
//  val internalDepth = visibleDepth * 2
//  val sdpramModel = SdpramModel(dataWidth = dataWidth, depth = internalDepth)
//
//  // 连接外部端口
//  io.read       <> inputMatrixCacheInterface.io.read
//  io.write      <> inputMatrixCacheInterface.io.write
//  io.switch     <> inputMatrixCacheInterface.io.switch
//  io.dmaIntr    <> inputMatrixCacheInterface.io.dmaIntr
//  io.status     <> inputMatrixCacheInterface.io.status
//  io.empty      <> inputMatrixCacheInterface.io.empty
//
//
//  // 内部存储连接
//  inputMatrixCacheInterface.io.read_sdpram  <> sdpramModel.io.read
//  inputMatrixCacheInterface.io.write_sdpram <> sdpramModel.io.write
//}
//
///** ****************************************************************************
// * InputMatrixCacheInterface
// * 功能：
// *   - 维护两个 bank（0/1）的使用状态，实现 ping-pong 双缓冲
// *   - 读端由 rdPtr 指向的 bank 读取；写端由 wrPtr 指向的 bank 写入
// *   - 当 io.switch=1 且 bankValid(rdPtr)=True：释放读 bank，rdPtr 翻转
// *   - 当 io.dmaIntr=1 且 bank 空闲：置 valid，wrPtr 翻转
// *   - status=存在空闲 bank；empty=两个 bank 均空；
// *   - intr=保持型中断，高电平直到 io.intrClear 清除
// * **************************************************************************** */
//case class InputMatrixCacheInterface(addrWidth: Int, dataWidth: Int) extends Component {
//  val io = new Bundle {
//    val read   = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
//    val write  = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
//    val switch = in  Bool()   // block 被读完释放
//    val dmaIntr= in  Bool()   // block 写入完成
//    val status = out Bool()   // 有空闲 bank 可写
//    val empty  = out Bool()   // 无可读 bank
//    val read_sdpram  = master(MemoryReadPort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
//    val write_sdpram = master(MemoryWritePort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
//  }
//
//  val wrPtr = Reg(UInt(1 bits)) init(0)
//  val rdPtr = Reg(UInt(1 bits)) init(0)
//  val bankValid = Vec(Reg(Bool()), 2)
//  bankValid.foreach(_ init(False))
//
//  val anyFree  = !bankValid(0) || !bankValid(1)
//  val allEmpty = !bankValid(0) && !bankValid(1)
//  io.status := anyFree
//  io.empty  := allEmpty
//
//  when(io.dmaIntr && !bankValid(wrPtr)) {
//    bankValid(wrPtr) := True
//    wrPtr := wrPtr + 1
//  }
//
//  when(io.switch && bankValid(rdPtr)) {
//    bankValid(rdPtr) := False
//    rdPtr := rdPtr + 1
//  }
//
//  // 地址映射保持不变
//  val wrAddrInt = (wrPtr(0).asBits ## io.write.Address.asBits).asUInt
//  val rdAddrInt = (rdPtr(0).asBits ## io.read.Address.asBits).asUInt
//
//  io.write_sdpram.Valid   := io.write.Valid
//  io.write_sdpram.Address := wrAddrInt
//  io.write_sdpram.Data    := io.write.Data
//
//  io.read_sdpram.Valid   := io.read.Valid
//  io.read_sdpram.Address := rdAddrInt
//  io.read.Data := io.read_sdpram.Data
//}
//

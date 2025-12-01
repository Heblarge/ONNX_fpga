package MatrixCacheInterface

import Slicer._
import DataPump._
import spinal.core._
import spinal.lib._

/** ****************************************************************************
 * - 封装 OutputMatrixCacheInterface 与底层 SDPRAM 模型
 * - 对外暴露与原内存端口一致的读/写接口（单倍深度视图）
 * - 内部以双倍深度的 SDPRAM 实现 ping-pong 映射
 * **************************************************************************** */
case class OutputMatrixCache(addrWidth: Int, dataWidth: Int) extends Component {
  val io = new Bundle {
    val read   = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val write  = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val switch = in Bool()     // 单周期有效的切换脉冲：写完当前 bank 后触发
    val dmaIntr= in Bool()     // 读端 DMA 完成当前 bank 后触发（释放读 bank）
    val status = out Bool()    // 有可读 bank（任一 bank valid）时为 1（可被轮询）
    val intr   = out Bool()    // 新的可读 bank 就绪的脉冲中断（来自 switch && !full）
    val intrClear = in Bool()
    val full   = out Bool()    // 两个 bank 均占用（写满未释放），上游需暂停切换
  }

  // 内部实际深度为 2×可见深度
  val visibleDepth  = 1 << addrWidth
  val internalDepth = visibleDepth * 2

  val matrixCacheInterface = OutputMatrixCacheInterface(addrWidth, dataWidth)

  val sdpramModel = SdpramModel(dataWidth = dataWidth, depth = internalDepth)

  // 对外端口直连核心接口
  io.read   <> matrixCacheInterface.io.read
  io.write  <> matrixCacheInterface.io.write
  io.switch <> matrixCacheInterface.io.switch
  io.dmaIntr<> matrixCacheInterface.io.dmaIntr
  io.status <> matrixCacheInterface.io.status
  io.intrClear <> matrixCacheInterface.io.intrClear
  io.intr   <> matrixCacheInterface.io.intr
  io.full   <> matrixCacheInterface.io.full

  // 核心接口与物理存储模型的连接
  matrixCacheInterface.io.read_sdpram  <> sdpramModel.io.read
  matrixCacheInterface.io.write_sdpram <> sdpramModel.io.write
}

/** ****************************************************************************
 * OutputMatrixCacheInterface
 * 功能：
 *   - 维护两个 bank（0/1）的占用状态（valid），实现 ping-pong 双缓冲
 *   - 写端总是映射到 wrPtr 指向的 bank；读端总是映射到 rdPtr 指向的 bank
 *   - 当 io.switch=1（单拍）且非全满时：当前写 bank 置 valid，wrPtr 翻转
 *   - 当 io.dmaIntr=1 且读 bank 有效：清除读 bank valid，rdPtr 翻转
 *   - full = 两个 bank 均有效；status = 存在任意有效 bank；intr = 新 bank 就绪脉冲
 * 接口：
 *   - 外部：MemoryRead/Write（单倍地址空间）
 *   - 内部：read_sdpram / write_sdpram（地址位宽 +1，MSB 为 bank 位）
 * **************************************************************************** */
case class OutputMatrixCacheInterface(addrWidth: Int, dataWidth: Int) extends Component {

  val io = new Bundle {
    val read   = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val write  = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    // 控制/状态
    val switch = in  Bool()   // 单拍：写完当前 bank 后触发切换
    val dmaIntr= in  Bool()   // 单拍：读端完成当前 bank 后释放
    val status = out Bool()   // 是否存在已写好且尚未被读完的 bank
    val intr   = out Bool()   // 新 bank 可读的脉冲（switch 被接受）
    val intrClear = in Bool() // 用于清除中断的寄存器
    val full   = out Bool()   // 两个 bank 均有效（无空闲 bank）
    // 对物理 SDPRAM 的主端口（地址 +1 位以承载 bank）
    val read_sdpram  = master(MemoryReadPort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
    val write_sdpram = master(MemoryWritePort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
  }

  // 写/读指针（指向 bank 0 或 1），仅 1 bit
  val wrPtr = Reg(UInt(1 bits)) init(0)  // 当前写 bank
  val rdPtr = Reg(UInt(1 bits)) init(0)  // 当前读 bank

  val intrReg = Reg(Bool()) init(False)
  val intrClearReg = Reg(Bool()) init(False)
  val statusReg = Reg(Bool()) init(False)



  when(io.intrClear.rise()){
    intrClearReg := io.intrClear
  }.otherwise{
    intrClearReg := False
  }

  // 两个 bank 的有效标志（“写满待读”）
  val bankValid = Vec(Reg(Bool()), 2)
  bankValid.foreach(_ init(False))

  // 便捷别名
  val wrIdx = wrPtr
  val rdIdx = rdPtr

  // 满/空/状态计算
  val anyValid = bankValid(0) || bankValid(1)
  val fullCond = bankValid(0) && bankValid(1)
  //io.status := anyValid
  statusReg := anyValid
  io.status := statusReg

  io.full   := fullCond

  val acceptSwitch = io.switch && !fullCond
  when(acceptSwitch) {
    bankValid(wrIdx) := True
    wrPtr := wrPtr + 1
    intrReg := True
  }.otherwise{
    intrReg := intrReg
  }

  when(intrClearReg){
    intrReg := False
  }


  val acceptRelease = io.dmaIntr.rise() && bankValid(rdIdx)
  when(acceptRelease) {
    bankValid(rdIdx) := False
    rdPtr := rdPtr + 1
  }

  io.intr := intrReg
//  io.intr := acceptSwitch

  // -----------------------
  // 地址映射到物理 SDPRAM
  // -----------------------
  // 写端：映射到 wrPtr 指向的 bank
  val wrBankBit = wrPtr(0).asBits
  val wrAddrInt = (wrBankBit ## io.write.Address.asBits).asUInt

  io.write_sdpram.Valid   := io.write.Valid
  io.write_sdpram.Address := wrAddrInt
  io.write_sdpram.Data    := io.write.Data

  // 读端：映射到 rdPtr 指向的 bank
  val rdBankBit = rdPtr(0).asBits
  val rdAddrInt = (rdBankBit ## io.read.Address.asBits).asUInt

  io.read_sdpram.Valid   := io.read.Valid
  io.read_sdpram.Address := rdAddrInt
  // 下层 SDPRAM 为同步读：数据一拍后返回
  io.read.Data := io.read_sdpram.Data
}

/** ****************************************************************************
 * SdpramModel模拟Xilinx的简单双端口BRAM行为
 * 功能：
 *   - 单时钟、双端口、同步读的简化 RAM 模型
 *   - wordCount = depth；地址宽度自动推导或外部指定
 * 约束：
 *   - 写在 io.write.Valid 时生效
 *   - 读在 io.read.Valid 时触发，同步读（1 拍延迟）
 * **************************************************************************** */
case class SdpramModel(dataWidth: Int, depth: Int, addrWidth: Int = -1) extends Component {
  // 地址宽度在 elaboration 阶段计算（软件层面）
  val effAddrWidth = if (addrWidth < 0) log2Up(depth) else addrWidth
  if ((1 << effAddrWidth) < depth) {
    SpinalError(s"[SdpramModel] Not enough address width: depth=$depth, addrWidth=$effAddrWidth, 2^addrWidth=${1 << effAddrWidth}")
  }

  def MemoryReadPortType  = MemoryReadPort_TypeDef(AddressWidth = effAddrWidth, DataWidth = dataWidth)
  def MemoryWritePortType = MemoryWritePort_TypeDef(AddressWidth = effAddrWidth, DataWidth = dataWidth)

  val io = new Bundle {
    val read  = slave(MemoryReadPortType)
    val write = slave(MemoryWritePortType)
  }

  val mem = Mem(Bits(dataWidth bits), wordCount = depth)

  mem.write(
    address = io.write.Address,
    data    = io.write.Data,
    enable  = io.write.Valid
  )

  io.read.Data := mem.readSync(
    address = io.read.Address,
    enable  = io.read.Valid
  )

  def noRead(): Unit = {
    io.read.Valid   := False
    io.read.Address := 0
  }
  def noWrite(): Unit = {
    io.write.Valid   := False
    io.write.Address := 0
    io.write.Data    := 0
  }
}

package MatrixCacheInterface

import Tiling._
import DataPump._
import spinal.core._
import spinal.core.sim.SimDataPimper
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
    val switch = in Bool()
    val dmaIntr= in Bool()
    val status = out Bool()
    val intr   = out Bool()
    val intrClear = in Bool()
    val full   = out Bool()
  }

  val visibleDepth  = 1 << addrWidth
  val internalDepth = visibleDepth * 2

  val matrixCacheInterface = OutputMatrixCacheInterface(addrWidth, dataWidth)
  val sdpramModel = SdpramModel(dataWidth = dataWidth, depth = internalDepth)

  io.read   <> matrixCacheInterface.io.read
  io.write  <> matrixCacheInterface.io.write
  io.switch <> matrixCacheInterface.io.switch
  io.dmaIntr<> matrixCacheInterface.io.dmaIntr
  io.status <> matrixCacheInterface.io.status
  io.intrClear <> matrixCacheInterface.io.intrClear
  io.intr   <> matrixCacheInterface.io.intr
  io.full   <> matrixCacheInterface.io.full

  matrixCacheInterface.io.read_sdpram  <> sdpramModel.io.read
  matrixCacheInterface.io.write_sdpram <> sdpramModel.io.write
}


/** ****************************************************************************
 * - 封装 OutputMatrixCacheInterface 与底层 SDPRAM 模型
 * - 对外暴露与原内存端口一致的读/写接口（单倍深度视图）
 * - 内部以双倍深度的 SDPRAM 实现 ping-pong 映射
 * **************************************************************************** */
case class OutputMatrixCacheController(addrWidth: Int, dataWidth: Int) extends Component {
  val io = new Bundle {
    val read   = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val write  = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val switch = in Bool()
    val dmaIntr= in Bool()
    val status = out Bool()
    val intr   = out Bool()
    val intrClear = in Bool()
    val full   = out Bool()
    val memRead   = master(MemoryReadPort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
    val memWrite  = master(MemoryWritePort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
  }

  val visibleDepth  = 1 << addrWidth
  val internalDepth = visibleDepth * 2

  val matrixCacheInterface = OutputMatrixCacheInterface(addrWidth, dataWidth)

  io.read   <> matrixCacheInterface.io.read
  io.write  <> matrixCacheInterface.io.write
  io.switch <> matrixCacheInterface.io.switch
  io.dmaIntr<> matrixCacheInterface.io.dmaIntr
  io.status <> matrixCacheInterface.io.status
  io.intrClear <> matrixCacheInterface.io.intrClear
  io.intr   <> matrixCacheInterface.io.intr
  io.full   <> matrixCacheInterface.io.full

  matrixCacheInterface.io.read_sdpram  <> io.memRead
  matrixCacheInterface.io.write_sdpram <> io.memWrite
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
    val switch = in  Bool()
    val dmaIntr= in  Bool()
    val status = out Bool()
    val intr   = out Bool()
    val intrClear = in Bool()
    val full   = out Bool()
    val read_sdpram  = master(MemoryReadPort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
    val write_sdpram = master(MemoryWritePort_TypeDef(AddressWidth = addrWidth + 1, DataWidth = dataWidth))
  }

  val wrPtr = Reg(UInt(1 bits)) init(0)
  val rdPtr = Reg(UInt(1 bits)) init(0)
  val intrReg = Reg(Bool()) init(False)
  val bankValid = Vec(Reg(Bool()), 2)
  bankValid.foreach(_ init(False))

  // 状态输出
  io.status := bankValid(0) || bankValid(1)
  io.full   := bankValid(0) && bankValid(1)
  io.intr   := intrReg

  // =========================
  // 中断逻辑 (Critical Fix)
  // =========================
  // 1. 默认保持原值

  // 2. 清除中断 (Reset)
  // AXI setOnSet 产生一个单周期高脉冲，直接清除寄存器
  when(io.intrClear) {
    intrReg := False
  }

  // 3. 产生中断 (Set) - 优先级更高！
  // 如果 Switch 和 Clear 在同一拍发生，说明刚刚清除了旧中断，但马上新数据又来了
  // 此时必须保持中断为高，否则新数据会被漏处理
  val acceptSwitch = io.switch && !io.full
  when(acceptSwitch) {
    bankValid(wrPtr) := True
    wrPtr := wrPtr + 1
    intrReg := True  // Set overrides Reset
  }

  // 释放逻辑
  val acceptRelease = io.dmaIntr.rise() && bankValid(rdPtr)
  when(acceptRelease) {
    bankValid(rdPtr) := False
    rdPtr := rdPtr + 1
  }

  // 地址映射
  val wrAddrInt = (wrPtr.asBits ## io.write.Address.asBits).asUInt
  io.write_sdpram.Valid   := io.write.Valid
  io.write_sdpram.Address := wrAddrInt
  io.write_sdpram.Data    := io.write.Data
  io.write_sdpram.clk     := io.write.clk
  io.write_sdpram.rst     := io.write.rst
  io.write_sdpram.Wen     := io.write.Wen


  val rdAddrInt = (rdPtr.asBits ## io.read.Address.asBits).asUInt
  io.read_sdpram.Valid   := io.read.Valid
  io.read_sdpram.clk     := io.read.clk
  io.read_sdpram.rst     := io.read.rst
  io.read_sdpram.Address := rdAddrInt
  io.read.Data := io.read_sdpram.Data

  // Debug 信号暴露
  wrPtr.simPublic()
  rdPtr.simPublic()
  bankValid.simPublic()
  intrReg.simPublic()
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

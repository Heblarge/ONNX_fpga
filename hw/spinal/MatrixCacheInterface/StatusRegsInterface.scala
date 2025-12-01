package MatrixCacheInterface

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axilite._

/** ****************************************************************************
 * StatusRegsInterface
 * - 通过 AXI4-Lite 访问一组“外部寄存器”：写 cfg，总线读 status
 * - 地址空间：
 *   0x00 : cfg (gpio_out)
 *   0x04 : status (gpio_in)
 *   0x08 : dir (gpio_dir，可选)
 *   0x0C : cfg_set（按位置位）
 *   0x10 : cfg_clr（按位清零）
 **************************************************************************** */
case class StatusRegsInterface(bitCount: Int = 8) extends Component {
  val io = new Bundle {
    val axi      = slave(AxiLite4(addressWidth = 5, dataWidth = 32))
    val gpio_in  = in  Bits(bitCount bits)   // 外部状态寄存器输入
    val gpio_out = out Bits(bitCount bits)   // 外部配置寄存器输出
  }

  // 内部寄存器（其实就是“那一排配置寄存器”，对外通过 gpio_out 输出）
  val reg_out = Reg(Bits(bitCount bits)) init(0)

  val axiCtrl = AxiLite4SlaveFactory(io.axi)

  val GPIO_OUT_ADDR = 0x00
  val GPIO_IN_ADDR  = 0x04
  val GPIO_SET_ADDR = 0x08
  val GPIO_CLR_ADDR = 0x0C

  // 读寄存器映射
  axiCtrl.read(reg_out.asUInt,     GPIO_OUT_ADDR)
  axiCtrl.read(io.gpio_in.asUInt,  GPIO_IN_ADDR)

  // 写寄存器映射（整字写）
  axiCtrl.write(reg_out, GPIO_OUT_ADDR)

  // 按位 SET/CLR：获取写数据总线
  val writeBits = axiCtrl.nonStopWrite(Bits(bitCount bits))

  when(axiCtrl.isWriting(GPIO_SET_ADDR)) {
    reg_out := reg_out | writeBits
  }
  when(axiCtrl.isWriting(GPIO_CLR_ADDR)) {
    reg_out := reg_out & ~writeBits
  }

  // 输出到外部模块
  io.gpio_out := reg_out
}

/** ****************************************************************************
 * DummyUserLogic
 * - 示例业务模块
 * - cfg_from_axi : 来自 StatusRegsInterface 的配置总线
 * - status_to_axi: 导出给 StatusRegsInterface 的状态总线
 **************************************************************************** */
case class DummyUserLogic(bitCount: Int = 8) extends Component {
  val io = new Bundle {
    val config  = in  Bits(bitCount bits)
    val status = out Bits(bitCount bits)
  }

  val cfgReg    = Reg(Bits(bitCount bits)) init(0)
  val statusReg = Reg(Bits(bitCount bits)) init(0)

  // 配置寄存器：每拍采样 AXI 传来的配置值
  cfgReg := io.config

  // 状态寄存器：这里简单用“按位取反”模拟某种状态函数
  statusReg := ~cfgReg

  io.status := statusReg
}


/** ****************************************************************************
 * TopWithStatusRegs
 * - 顶层封装：
 *   AXI <-> StatusRegsInterface <-> UserLogic
 **************************************************************************** */
case class TopWithStatusRegs(bitCount: Int = 8) extends Component {
  val io = new Bundle {
    val axi = slave(AxiLite4(addressWidth = 5, dataWidth = 32))
  }

  val regs  = StatusRegsInterface(bitCount)
  val user  = DummyUserLogic(bitCount)

  // AXI 接口直通
  regs.io.axi <> io.axi

  // 配置与状态连接
  user.io.config  := regs.io.gpio_out
  regs.io.gpio_in := user.io.status

}
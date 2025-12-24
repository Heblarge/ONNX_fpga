package DataPump
import spinal.lib._
import spinal.core
import spinal.core._

/**
 * 定义内存读取端口的类型
 * @param AddressWidth 地址总线宽度
 * @param DataWidth 数据总线宽度
 * 这个类继承了Bundle和IMasterSlave，用于定义内存读取端口的接口
 */
case class MemoryReadPort_TypeDef(AddressWidth:Int,DataWidth:Int) extends Bundle with IMasterSlave{
  val clk = Bool()
  val Valid=Bool()  // 标识读取操作是否有效的信号
  val Address=UInt(AddressWidth bits)  // 内存地址信号，宽度由AddressWidth参数决定
  val Data = Bits(DataWidth bits)  // 读取的数据信号，宽度由DataWidth参数决定
  
  /**
   * 配置端口为Master角色
   * 这里将Valid和Address设置为输出，Data设置为输入
   */
  override def asMaster(): Unit ={
    out(clk,Valid,Address)
    in(Data)
  }
  
  /**
   * 配置端口为Slave角色
   * 这里将Valid和Address设置为输入，Data设置为输出
   */
  override def asSlave(): Unit = {

    in(clk, Valid, Address)
    out(Data)
  }
}

/**
 * 定义内存写入端口的类型
 * @param AddressWidth 地址总线宽度
 * @param DataWidth 数据总线宽度
 * 这个类继承了Bundle和IMasterSlave，用于定义内存写入端口的接口
 */
case class MemoryWritePort_TypeDef(AddressWidth:Int,DataWidth:Int) extends Bundle with IMasterSlave{
  val clk = Bool()
  val Wen = Bits(DataWidth / 8 bits)
  val Valid= Bool()  // 标识写入操作是否有效的信号，作为输入
  val Address= UInt(AddressWidth bits)  // 内存地址信号，宽度由AddressWidth参数决定，作为输入
  val Data = (Bits(DataWidth bits))  // 写入的数据信号，宽度由DataWidth参数决定，作为输入
  
  /**
   * 配置端口为Master角色
   * 这里将Valid、Address和Data都设置为输出
   */
  override def asMaster(): Unit ={
    out(clk, Wen, Valid, Address, Data)
  }
  
  /**
   * 配置端口为Slave角色
   * 这里将Valid、Address和Data都设置为输入
   */
  override def asSlave(): Unit = {
    in(clk, Wen, Valid, Address, Data)
  }
}

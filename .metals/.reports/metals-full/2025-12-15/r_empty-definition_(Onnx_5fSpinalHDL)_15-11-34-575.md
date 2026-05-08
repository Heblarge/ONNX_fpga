error id: file://<WORKSPACE>/hw/spinal/DataPump/BRAMPORT.scala:
file://<WORKSPACE>/hw/spinal/DataPump/BRAMPORT.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -spinal/lib/dataWidth.
	 -spinal/lib/dataWidth#
	 -spinal/lib/dataWidth().
	 -spinal/core/dataWidth.
	 -spinal/core/dataWidth#
	 -spinal/core/dataWidth().
	 -dataWidth.
	 -dataWidth#
	 -dataWidth().
	 -scala/Predef.dataWidth.
	 -scala/Predef.dataWidth#
	 -scala/Predef.dataWidth().
offset: 326
uri: file://<WORKSPACE>/hw/spinal/DataPump/BRAMPORT.scala
text:
```scala
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
case class BRAMReadPort_TypeDef(AddressWidth:Int,DataWidth:Int) extends Bundle with IMasterSlave{

  val Valid=Bits(dataWidth@@/8 bits)  // 标识读取操作是否有效的信号
  val Address=UInt(AddressWidth bits)  // 内存地址信号，宽度由AddressWidth参数决定
  val Data = Bits(DataWidth bits)  // 读取的数据信号，宽度由DataWidth参数决定
  
  /**
   * 配置端口为Master角色
   * 这里将Valid和Address设置为输出，Data设置为输入
   */
  override def asMaster(): Unit ={
    out(Valid,Address)
    in(Data)
  }
  
  /**
   * 配置端口为Slave角色
   * 这里将Valid和Address设置为输入，Data设置为输出
   */
  override def asSlave(): Unit = {
    in(Valid,Address)
    out(Data)
  }
}

/**
 * 定义内存写入端口的类型
 * @param AddressWidth 地址总线宽度
 * @param DataWidth 数据总线宽度
 * 这个类继承了Bundle和IMasterSlave，用于定义内存写入端口的接口
 */
case class BRAMWritePort_TypeDef(AddressWidth:Int,DataWidth:Int) extends Bundle with IMasterSlave{

  val Valid=in Bool()  // 标识写入操作是否有效的信号，作为输入
  val Address=in UInt(AddressWidth bits)  // 内存地址信号，宽度由AddressWidth参数决定，作为输入
  val Data = in(Bits(DataWidth bits))  // 写入的数据信号，宽度由DataWidth参数决定，作为输入
  
  /**
   * 配置端口为Master角色
   * 这里将Valid、Address和Data都设置为输出
   */
  override def asMaster(): Unit ={
    out(Valid,Address,Data)
  }
  
  /**
   * 配置端口为Slave角色
   * 这里将Valid、Address和Data都设置为输入
   */
  override def asSlave(): Unit = {
    in(Valid,Address,Data)
  }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 
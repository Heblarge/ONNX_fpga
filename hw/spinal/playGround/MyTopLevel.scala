package playGround

import spinal.core._
import spinal.lib._
import spinal.lib.tools
import spinal.lib.dsptool.FixData
import spinal.lib.bus.misc._
import java.{util => ju}
import spinal.lib.bus.amba3._
import spinal.lib.bus.amba3.apb.Apb3SlaveFactory
import scala.util.matching.Regex


// 定义一个顶级组件，用于演示条件计数和状态输出的功能
// Hardware definition
case class MyTopLevel() extends Component {
  // 定义输入输出接口
  val io = new Bundle {
    // 输入条件0，当为真时触发计数器加一
    val cond0 = in Bool()
    // 输入条件1，当为真时影响标志位
    val cond1 = in Bool()
    // 输出标志位，当计数器为0或cond1为真时为真
    val flag = out Bool()
    // 输出当前计数器的状态
    val state = out UInt(8 bits)
  }
  val counter_wire = UInt(8 bits)
  // 定义一个8位的计数器，初始值为0
  val counter = Reg(UInt(8 bits)) init 0
  //define the counter as UINT8 with initial value 0
  val counter_next = Reg(UInt(8 bits)) init 0

  val target_bits = B(31 bits,7->true,default->false)

  val demo_uint1= UInt(10 bits)
  val demo_uint2= UInt(10 bits)

  val demo_uint_sum= demo_uint1 + demo_uint2//add
  val demo_uint_sum_fix= demo_uint1 +^ demo_uint2//add and add 1bit
  val demo_uint_sum_all1_if_overflow= demo_uint1 +| demo_uint2//add, if overflow, result will be 10b'1111111111

  val product_uint12=demo_uint1*demo_uint2
  val product_uint12_fixed=product_uint12.fixTo(15 downto 6)
  val demo_int1= SInt(10 bits)
  val demo_int2= SInt(10 bits)
  val demo_int_sum= demo_int1 + demo_int2

  // 当cond0为真时，计数器加一
  when(io.cond0) {
    counter := counter + 1
  }//when cond0 is true, increment the counter
  .otherwise {
    // 否则，计数器保持不变
    counter := counter
  }//otherwise, keep the counter as it is
  //describe the behavior of the counter

  // 将计数器的值输出到状态接口
  io.state := counter
  // 将计数器为0或cond1为真的条件输出到标志位
  io.flag := (counter === 0) | io.cond1
  counter_next:=counter
}

//生成Verilog代码运行这个
object MyTopLevelVerilog extends App {
  ////tools.HDElkDiagramGen(SpinalConfig(globalPrefix="",anonymSignalPrefix="Pre").generateVerilog(MyTopLevel()))
}



//生成VHDL代码运行这个
object MyTopLevelVhdl extends App {
  Config.spinal.generateVhdl(MyTopLevel())
}



object fixDataTest {
  def main(args: Array[String]): Unit = {
    import spinal.core.RoundType._
    val roundList = List(CEIL,FLOOR,FLOORTOZERO,CEILTOINF,ROUNDUP,ROUNDDOWN,ROUNDTOZERO,ROUNDTOINF)
    for(rd <- roundList){
      val a = FixData(29.29715926,SQ(16,8),rd)
      println(rd)
      println(a.value)
      println(a.hex)

    }
  }
}

case class FilterConfig(iqWidth: Int,
                        tapNumbers: Int = 33,
                        hwFreq: HertzNumber = 200 MHz,
                        sampleFreq: HertzNumber = 1.92 MHz)

class Filter(fc: FilterConfig) extends Component{
  val din   = slave Flow(Bits(32 bits))
  val dout  = master Flow(Bits(32 bits))
  val flush = in Bool()

  val clockSMP = ClockDomain.external("smp")
  val clockHW = ClockDomain.external("hw")

  val u_fifo_in = StreamFifoCC(
    dataType = Bits(32 bits),
    depth = 8,
    pushClock = clockSMP,
    popClock = clockDomain
  )

  u_fifo_in.io.push << din.toStream
  dout << u_fifo_in.io.pop.toFlow

}

object Filter {
  def main(args: Array[String]): Unit = {
    val fc = FilterConfig(8)
    ////tools.HDElkDiagramGen(SpinalVerilog(new Filter(fc)))
  }
}



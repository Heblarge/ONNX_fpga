package playGround
import spinal.core
import spinal.core._
import spinal.core.ClockDomain
import spinal.core.sim._
import spinal.core.sim.SimConfig
import spinal.lib._
import spinal.lib.tools
import spinal.lib.sim.{StreamMonitor, StreamDriver, StreamReadyRandomizer, ScoreboardInOrder_matrix}
import scala.util.Random

case class ExternalClockExample(myClockDomain:ClockDomain) extends Component {
  val io = new Bundle {
    val result = out UInt (4 bits)
  }

  // On the top level you have two signals  :
  //     myClockName_clk and myClockName_reset

  val myArea = new ClockingArea(myClockDomain) {
    val myReg = Reg(UInt(4 bits)) init(7)
    myReg := myReg + 1

    io.result := myReg
  }
}

case class module_with_many_clk(clkdomain:ClockDomain) extends Component{
    val io = new Bundle{
      val IN =in UInt(8 bits)
      val OUT=out(UInt(8 bits))
    }
    
    val myArea = new ClockingArea(clkdomain) {
        val OUT_reg = RegNext(io.IN)
        io.OUT := OUT_reg
    }
}
case class shell1(clkdomain:ClockDomain) extends Component{
    val io = new Bundle{
      val IN =in UInt(8 bits)
      val OUT=out(UInt(8 bits))
    }
    val module_with_many_clk_instance = module_with_many_clk(clkdomain)
    module_with_many_clk_instance.io.IN := io.IN
    io.OUT := module_with_many_clk_instance.io.OUT
}
case class shell2(clkdomain:ClockDomain) extends Component{
    val io = new Bundle{
      val IN =in UInt(8 bits)
      val OUT=out(UInt(8 bits))
    }
    val shell1_instance = shell1(clkdomain)
    shell1_instance.io.IN:=io.IN
    io.OUT:=shell1_instance.io.OUT
}
case class top() extends Component{
    val io = new Bundle{
      val IN =in UInt(8 bits)
      val OUT=out(UInt(8 bits))
      val myclk=in Bool()
      val myrst=in Bool()
    }
    
    val clkA_ClockDomain=ClockDomain.internal("clkB")

    
    clkA_ClockDomain.clock := io.myclk
    clkA_ClockDomain.reset := io.myrst
    val shell2_instance = shell2(clkA_ClockDomain)
    shell2_instance.io.IN:=io.IN
    io.OUT:=shell2_instance.io.OUT
}

object topVerilog extends App {
  SpinalConfig(
    globalPrefix="",
    anonymSignalPrefix="Pre").generateVerilog(
      top()
      )
}
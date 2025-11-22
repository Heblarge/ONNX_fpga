package MatrixComputeUnit.AdderTree
import spinal.core._
import spinal.lib._
import spinal.lib.tools
/** **************************************************************
 *
 *    adderTreeOri
 *
 *    adderTreeOri is the traditional adder module.
 *
 *    Update history:
 *      * V1.1.0  2021/12/04
 *        Create file   // raochl
 *
 * *************************************************************/
case class addConfig
(
  nof_Input   : Int,  // number of input data
  xin_Width   : Int,
//  block_Size  : Int,  // max input for one block
  reg_Bf_Add  : Boolean=true
)
{
  val xout_Width = xin_Width + log2Up(nof_Input)
}

// 定义一个加法树组件的案例类，用于硬件描述
case class adderTreeOri(cfg: addConfig) extends Component {

  // 定义组件的输入和输出接口
  val io = new Bundle {
    // 定义输入端口xin，是一个宽度为xin_Width的有符号整数向量
    val xin = in Vec(SInt(cfg.xin_Width bits), cfg.nof_Input)
    // 定义输出端口xout，是一个宽度为xout_Width的有符号整数寄存器
    val xout = out(Reg(SInt(cfg.xout_Width bits)))
  }

  // 定义第一级中间结果寄存器xstg1，宽度为xin_Width+2，数量为输入数量的1/4
  val xstg1 = Reg(Vec(SInt(cfg.xin_Width+2 bits), cfg.nof_Input/4))

  // 更新xstg1的值，通过将连续4个输入相加得到
  xstg1 := Vec((0 until cfg.nof_Input/4).map( i => io.xin.slice(4*i , 4*i+4).map(_.resize(cfg.xin_Width+2 bits)).reduce(_+_)))

  // 定义第二级中间结果寄存器xstg2，宽度为xin_Width+4，数量为输入数量的1/16
  val xstg2 = Reg(Vec(SInt(cfg.xin_Width+4 bits), cfg.nof_Input/16))

  // 更新xstg2的值，通过将连续4个xstg1的值相加得到
  xstg2 := Vec((0 until cfg.nof_Input/16).map( i => xstg1.slice(4*i, 4*i+4).map(_.resize(cfg.xin_Width+4)).reduce(_+_)))

  // 最终输出值，通过将所有xstg2的值相加得到
  io.xout := xstg2.map(_.resize(cfg.xout_Width)).reduce(_+_)
  
  // 定义组件的延迟周期数
  val Latency = 3

}



object adderTreeOriMain {
  val cfg = addConfig(64, 20)
  def main(args: Array[String]): Unit = {
    val FileDir = "rtl/adderTreeOri/verilog"
    import java.io.File
    new File(FileDir).mkdirs()
    
    SpinalConfig(
      targetDirectory = FileDir,
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(new adderTreeOri(cfg)).printPruned()
    
  }
}

object adderTreeOriSim extends App {
  val FileDir = "rtl/adderTreeOri/verilog"
  import java.io.File
  new File(FileDir).mkdirs()
  import spinal.core.sim._
  val cfg = addConfig(32, 16)
  
  SimConfig.withConfig(SpinalConfig(
      targetDirectory = FileDir,
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    )).withFstWave.allOptimisation.compile(new adderTreeOri(cfg)).doSim { dut =>
    // Fork a process to generate the reset and the clock on the dut
    dut.clockDomain.forkStimulus(period = 10)

    var modelState = 0
    
    for (idx <- 0 to 99) {
      // Drive the dut inputs with random values
      dut.io.xin.randomize()

      // Wait a rising edge on the clock
      dut.clockDomain.waitRisingEdge()
      var xout_ref=0
      for(input<-dut.io.xin)
      {
          xout_ref=xout_ref+input.toInt
          //println(input.toInt)
      }
      
      sleep(30) // Sleep 1 simulation timestep
      

      //println(dut.io.xout.toInt)
      println(idx)
      assert(dut.io.xout.toInt == xout_ref)
      
    }
  }
  //tools.HDElkDiagramGen(SpinalVerilog(new adderTreeOri(cfg)))
}

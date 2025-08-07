package GeMM.SystolicArray2D
import spinal.core._
import spinal.lib._
import spinal.lib.tools
import spinal.core
import scala.math
import Interface.MatrixOperation_TypeDef
/** **************************************************************
 *
 *    SystolicArray2DUnit
 *
 *    SystolicArray2DUnit is one Unit of systolic array 2D
 *                                                                                                                                                                
 * *************************************************************/
abstract class SystolicArray2DUnit_io(cfg: SystolicArray2DUnit_Config) extends Component {
  val io = new Bundle {
    val inA = in SInt(cfg.inA_Width bits)
    val inA_Final = in Bool()
    val inB = in SInt(cfg.inB_Width bits)
    val inB_Final = in Bool()
    val Go = in Bool()

    val outA = out(Reg(SInt(cfg.inA_Width bits))) init 0
    val outA_Final = out(Reg(Bool())) init False
    val outB = out(Reg(SInt(cfg.inB_Width bits))) init 0
    val outB_Final = out(Reg(Bool())) init False

    val outZ = out(Reg(SInt(cfg.outZ_Width bits))) init 0
    val inMode = in(MatrixOperation_TypeDef()) // 使用枚举类型替换Bits(2 bits)
    val outMode = out(Reg(MatrixOperation_TypeDef())) init(MatrixOperation_TypeDef.MatMul)

    val inTranspose = in Bool()
    val outTranspose = out(Reg(Bool()) init False)

    val inShift = in (SInt(log2Up(cfg.outZ_Width + 1) + 1 bits))
    val outShift = out (Reg(SInt(log2Up(cfg.outZ_Width + 1) + 1 bits)) init 0)
  }
}


case class SystolicArray2DUnit_Config
(
  in_Length   : Int,  // number of input data
  inA_Width   : Int=16,
  inB_Width   : Int=16,
  outZ_Width  : Int=16
)
{
    val ABProduct_Width = inA_Width+inB_Width
    val ProductSum_Width = ABProduct_Width + log2Up(in_Length)
    val theroretical_outZ_Width = math.max(ABProduct_Width,ProductSum_Width)+1
    if(outZ_Width<theroretical_outZ_Width)
    {SpinalWarning("SystolicArray2DUnit:\n\toutZ_Width is set to"+outZ_Width+"\n\tBut theroretical maximum is ABProduct_Width+ProductSum_Width="+(theroretical_outZ_Width))}
    val Latency = 1//得到全部输入后一周期就能出结果
    val IterationInterval = 1
    //对于每个元素，理想情况下每一个时钟周期都可以输入一个数据，除非下游的buffer阻塞，这种情况下本单元会传递该阻塞信号
}

case class SystolicArray2DUnit(cfg: SystolicArray2DUnit_Config) extends SystolicArray2DUnit_io(cfg) {
  // io定义从SystolicArray2DUnit_io继承
  val ABProduct = SInt(cfg.ABProduct_Width bits)
  val ProductSum = Reg(SInt(cfg.ProductSum_Width bits)) init 0
  val ProductSum_Next = SInt(cfg.ProductSum_Width bits)

  when(io.inMode === MatrixOperation_TypeDef.MatMul) {
    ABProduct := io.inA * io.inB
    ProductSum_Next := ProductSum + ABProduct
  }.otherwise {
    ABProduct := S(0)
    ProductSum_Next := S(0)
  }
  when(io.Go === True) {
    io.outA_Final := io.inA_Final
    io.outB_Final := io.inB_Final
  }
  when(io.Go === True) {
    io.outA := io.inA
    io.outB := io.inB
    io.outMode := io.inMode
    io.outTranspose := io.inTranspose
    io.outShift := io.inShift
    // 计算部分
    when(io.inMode === MatrixOperation_TypeDef.MatMul) {
      ProductSum := ProductSum_Next
      when((io.inA_Final === True) && (io.inB_Final === True)) {
        val instSIntShifter = new SIntShifter(inWidth = cfg.ProductSum_Width, outWidth = cfg.outZ_Width)
        instSIntShifter.io.input := ProductSum_Next
        instSIntShifter.io.shiftAmount := io.inShift
        io.outZ := (instSIntShifter.io.output).resize(cfg.outZ_Width)

        //io.outZ := (ProductSum_Next).resize(cfg.outZ_Width)
        ProductSum:=0
      }
    }.otherwise{
      ProductSum := S(0)
      io.outZ := S(0)
    }
  }
}

object SystolicArray2DUnit_Verilog extends App{
    val cfg = SystolicArray2DUnit_Config(32,16,16)

    val FileDir = "rtl/SystolicArray2DUnit/verilog"
    import java.io.File
    new File(FileDir).mkdirs()
    
    SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(new SystolicArray2DUnit(cfg)).printPruned()
    
    //tools.HDElkDiagramGen(SpinalVerilog(new SystolicArray2DUnit(cfg)))
}

object SystolicArray2DUnit_Sim extends App {
    val FileDir = "rtl/SystolicArray2DUnit/verilog"
    import java.io.File
    new File(FileDir).mkdirs()
    import spinal.core.sim._
    val testLength=3
    val cfg = SystolicArray2DUnit_Config(testLength,
    inA_Width = 8,
    inB_Width = 8,
    outZ_Width = 16
    )
    
//verilator
    /*     val Sim_compiled=SimConfig.withConfig(SpinalConfig(
        targetDirectory = FileDir,
        oneFilePerComponent = true,
        defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
        )).
        withFstWave.
        allOptimisation.
        compile(new SystolicArray2DUnit(cfg)) */
        
//VCS
import scala.sys.process._
import scala.util.{Try, Success, Failure}
    // 设置自定义的 g++ 路径
    val customGppPath = "/usr/local/bin"

    // 设置 PATH 环境变量，将自定义 g++ 放在最前面
    val newPath = s"$customGppPath:" + scala.sys.env("PATH")

    // 执行 `which g++`，查看当前环境下使用的 g++
    val whichGpp = Try(Process(Seq("which", "g++"), None, "PATH" -> newPath).!!.trim)

    whichGpp match {
      case Success(path) => println(s"Custom g++ path is: $path")
      case Failure(exception) => println(s"Error finding g++: ${exception.getMessage}")
    }
    var result = scala.sys.env.get("CPLUS_INCLUDE_PATH")
    println(s"The CPLUS_INCLUDE_PATH is: $result")
    result = scala.sys.env.get("LIBRARY_PATH")
    println(s"The LIBRARY_PATH is: $result")

    val custom_CPLUS_INCLUDE_PATH = "/tools/opensource/boost_1_78_0"
    val custom_LIBRARY_PATH = "/tools/opensource/boost_1_78_0/stage/lib"
    val new_CPLUS_INCLUDE_PATH = s"$custom_CPLUS_INCLUDE_PATH"
    val new_LIBRARY_PATH = s"$custom_LIBRARY_PATH"
    val envVars = Map("CPLUS_INCLUDE_PATH" -> new_CPLUS_INCLUDE_PATH, "LIBRARY_PATH" -> new_LIBRARY_PATH)
    val result_envVars = Process(Seq("/bin/bash", "-c", "echo $CPLUS_INCLUDE_PATH && echo $LIBRARY_PATH"), None, envVars.toSeq: _*).!!
    println(result_envVars)
 
import spinal.sim.VCSFlags
      val flag = VCSFlags(
        compileFlags = List("-kdb","-lca", "+notimingchecks"),
        elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"),
        //    runFlags = List("-fgp=num_threads:11,allow_less_cores", "-l ./run.log")
        //    elaborateFlags = List("-fgp", "+notimingchecks"),
        runFlags = List("-l ./run.log")
      )
      val Sim_compiled=SimConfig
      .withVCS(flag)
      .withVcdWave
      .withTimeScale(1 ns)
      .withTimePrecision(1 ns)
      .withConfig(SpinalConfig(
        
        targetDirectory = FileDir,
        oneFilePerComponent = true,
        defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
        ))
      .allOptimisation
      .compile(new SystolicArray2DUnit(cfg))



    Sim_compiled.doSim{ dut =>
      // Fork a process to generate the reset and the clock on the dut
      dut.clockDomain.forkStimulus(period = 10)
      var xout_ref=0
      var product:Int=0
      var sumproduct:Int=0
      var inA:Int=0
      var inB:Int=0
      var Go:Boolean = true
      
      dut.io.inA#=0
      dut.io.inB#=0
      dut.io.inA_Final#=false
      dut.io.inB_Final#=false
      dut.io.inMode #= MatrixOperation_TypeDef.MatMul
      dut.io.inTranspose#= false
      dut.io.inShift#=0
      dut.io.Go#=true
      var idx=0
      while(idx<(testLength*10)) 
      {
        //Final 发送机制
        if(idx % testLength == (testLength-1)){
            dut.io.inA_Final#=true
            dut.io.inB_Final#=true
        }
        else{
            dut.io.inA_Final#=false
            dut.io.inB_Final#=false
        }

        dut.clockDomain.waitRisingEdge()
        // Drive the dut inputs with random values
        dut.io.Go.randomize()
        dut.io.inA.randomize()
        dut.io.inB.randomize()
        //将输入值转换回scala变量用于计算参考值
        inA=dut.io.inA.toInt
        inB=dut.io.inB.toInt
        Go=dut.io.Go.toBoolean
        if(Go)
        {

          product=(inA * inB)
          sumproduct=sumproduct+product
    
          if(idx % testLength == (testLength-1)){

              xout_ref=sumproduct
              sumproduct=0
          }
        }

        println(
          s"${idx}:inA:${dut.io.inA.toInt};" +
          s"inB:${dut.io.inB.toInt};" +
          s"sumproduct=${sumproduct};" +
          s"xout=${dut.io.outZ.toInt};xout_ref=${xout_ref}")
        // Wait a rising edge on the clock
        
  
        if((idx % testLength == 0)&&(idx != 0)){
            
            assert(dut.io.outZ.toInt == xout_ref)
        }
        

        //println(dut.io.xout.toInt)
          
        if(Go)
        {
          idx=idx+1
        }
      }
    }

}

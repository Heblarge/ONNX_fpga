package GeMM.SystolicArray2D
import spinal.core._
import spinal.lib._
import spinal.lib.tools
import spinal.core
import Interface.MatrixOperation_TypeDef
/** **************************************************************
 *
 *    SystolicArray2DUnitSpecial
 *
 *    SystolicArray2DUnit is a special unit of systolic array 2D that can be reconfigured to element-wise multiply or addition.
 *
 * *************************************************************/

case class SystolicArray2DUnitSpecial(cfg: SystolicArray2DUnit_Config) extends SystolicArray2DUnit_io(cfg) {

  //io定义从SystolicArray2DUnit_io继承

  val ABProduct=SInt(cfg.ABProduct_Width bits)
  val ProductSum=Reg(SInt(cfg.ProductSum_Width bits)) init 0
  val ProductSum_Next=  SInt(cfg.ProductSum_Width bits)

  val addend_1 = SInt(cfg.ProductSum_Width bits)
  val addend_2 = SInt(cfg.ABProduct_Width bits)
  val sum = SInt(cfg.ProductSum_Width bits)
  val factor_1 = SInt(cfg.inA_Width bits)
  val factor_2 = SInt(cfg.inA_Width bits)
  val product = SInt(cfg.ABProduct_Width bits)

  val comp_1 = SInt(cfg.inA_Width bits)
  val comp_2 = SInt(cfg.inB_Width bits)
  val greater = SInt(cfg.ProductSum_Width bits)
  //信号切换
  when(io.inMode === MatrixOperation_TypeDef.MatMul){//matmul
    comp_1 := S(0)
    comp_2 := S(0)
    addend_1:=ProductSum
    addend_2:=product
    factor_1:=io.inA
    factor_2:=io.inB
    ProductSum_Next := sum
  }.elsewhen(io.inMode === MatrixOperation_TypeDef.ElementMax){//element-wise max
    comp_1 := io.inA
    comp_2 := io.inB
    factor_1:=S(0)
    factor_2:=S(0)
    addend_1 := S(0)
    addend_2 := S(0)
    ProductSum_Next := greater
  }.elsewhen(io.inMode === MatrixOperation_TypeDef.ElementAdd) {//element-wise add
    comp_1 := S(0)
    comp_2 := S(0)
    factor_1:=S(0)
    factor_2:=S(0)
    addend_1:=io.inA.resized
    addend_2:=io.inB.resized
    ProductSum_Next := sum
  }.elsewhen(io.inMode === MatrixOperation_TypeDef.ElementMul) { //element-wise multiply
    comp_1 := S(0)
    comp_2 := S(0)
    addend_1 := S(0)
    addend_2 := S(0)
    factor_1 := io.inA
    factor_2 := io.inB
    ProductSum_Next := product.resized
  }.otherwise{
    addend_1 := S(0)
    addend_2 := S(0)
    factor_1 := S(0)
    factor_2 := S(0)
    comp_1 := S(0)
    comp_2 := S(0)
    ProductSum_Next := S(0)
  }
  //例化加法器与分发器
  sum := addend_1 + addend_2
  product := factor_1 * factor_2
  greater := (comp_1 max comp_2).resized
  when(io.Go===True){
    io.outA_Final:=io.inA_Final
    io.outB_Final:=io.inB_Final
  }
  when(io.Go===True){
    io.outA := io.inA
    io.outB := io.inB
    io.outMode := io.inMode
    io.outTranspose := io.inTranspose
    io.outShift := io.inShift
    //输出逻辑
    val instSIntShifter = new SIntShifter(inWidth = cfg.ProductSum_Width, outWidth = cfg.outZ_Width)
    instSIntShifter.io.input := ProductSum_Next
    instSIntShifter.io.shiftAmount := io.inShift
    val ShiftedResult = instSIntShifter.io.output

    when(io.inMode === MatrixOperation_TypeDef.MatMul){
      ProductSum := ProductSum_Next
      when((io.inA_Final===True)&&(io.inB_Final===True)){
        io.outZ := ShiftedResult.resize(cfg.outZ_Width)
        ProductSum:=0
      }
    }.elsewhen(io.inMode === MatrixOperation_TypeDef.ElementMax){
      ProductSum := S(0)
      io.outZ:= ShiftedResult.resize(cfg.outZ_Width)
    }.elsewhen(io.inMode === MatrixOperation_TypeDef.ElementAdd){//element-wise add
      ProductSum := S(0)
      io.outZ:= ShiftedResult.resize(cfg.outZ_Width)
    }.elsewhen(io.inMode === MatrixOperation_TypeDef.ElementMul){//element-wise multiply
      ProductSum := S(0)
      io.outZ:= ShiftedResult.resize(cfg.outZ_Width)
    }.otherwise{
      ProductSum := S(0)
      io.outZ:= S(0)
    }
  }
}

object SystolicArray2DUnitSpecial_Verilog extends App{
  val cfg = SystolicArray2DUnit_Config(in_Length = 32, inA_Width = 16, inB_Width = 16)

  val FileDir = "rtl/SystolicArray2DUnitSpecial/verilog"
  import java.io.File
  new File(FileDir).mkdirs()

  SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(new SystolicArray2DUnitSpecial(cfg)).printPruned()

  //tools.HDElkDiagramGen(SpinalVerilog(new SystolicArray2DUnitSpecial(cfg)))
}

object SystolicArray2DUnitSpecial_Sim extends App {
  val FileDir = "rtl/SystolicArray2DUnitSpecial/verilog"
  import java.io.File
  new File(FileDir).mkdirs()
  import spinal.core.sim._
  val testLength=3
  val cfg = SystolicArray2DUnit_Config(testLength,8,8)

  //VCS
  import scala.sys.process._
  import scala.util.{Try, Success, Failure}
  import scala.util.Random
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
    .withFsdbWave
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .withConfig(SpinalConfig(
      targetDirectory = FileDir,
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ))
    .allOptimisation
    .compile(new SystolicArray2DUnitSpecial(cfg))

  Sim_compiled.doSim{ dut =>
    // Fork a process to generate the reset and the clock on the dut
    dut.clockDomain.forkStimulus(period = 10)
    var xout_ref=0
    var product:Int=0
    var sumproduct:Int=0
    var inA:Int=0
    var inB:Int=0
    val ref_array = Array.ofDim[Int](testLength*10)
    val dut_array = Array.ofDim[Int](testLength*10)
    dut.io.inA #= 0
    dut.io.inB #= 0
    dut.io.inMode #= MatrixOperation_TypeDef.ElementAdd
    dut.io.inA_Final #= false
    dut.io.inB_Final #= false
    dut.io.inTranspose#= false
    dut.io.inShift#=0
    dut.io.Go #= true
    var idx=0
    val rand = new Random()
    rand.setSeed(1234)
    while(idx<(testLength*10))
    {
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
      dut.io.inA#=rand.nextInt(16)//.randomize()
      dut.io.inB#=rand.nextInt(16)//.randomize()
      inA=dut.io.inA.toInt
      inB=dut.io.inB.toInt
      if(dut.io.Go.toBoolean)
      {
        if(dut.io.inMode==MatrixOperation_TypeDef.MatMul){
          product=(inA * inB)
          sumproduct=sumproduct+product
          if(idx % testLength == (testLength-1)){
            xout_ref=sumproduct
            sumproduct=0
          }
        }else if(dut.io.inMode==MatrixOperation_TypeDef.ElementMax){
          ref_array(idx)= if(inA > inB) inA else inB
        }
        else if(dut.io.inMode == MatrixOperation_TypeDef.ElementAdd){
          ref_array(idx)=inA + inB
        }else if(dut.io.inMode == MatrixOperation_TypeDef.ElementMul){
          ref_array(idx)=inA * inB
        }
        if((dut.io.inMode == MatrixOperation_TypeDef.ElementAdd|| dut.io.inMode == MatrixOperation_TypeDef.ElementMul) && idx > 0){
          dut_array(idx-1) = dut.io.outZ.toInt//-1因为计算输出有一级寄存器
        }
      }

      if(dut.io.inMode==MatrixOperation_TypeDef.MatMul)
      {
        println(s"${idx}:inA:${dut.io.inA.toInt};inB:${dut.io.inB.toInt};sumproduct=${sumproduct};xout_ref=${xout_ref}")
        // Wait a rising edge on the clock
        println(s"${idx}dut_out:${dut.io.outZ.toInt}")
        if((idx % testLength == 0)&&(idx != 0)){
          assert(dut.io.outZ.toInt == xout_ref)
        }
      }

      if(dut.io.Go.toBoolean)
      {
        idx=idx+1
      }
    }
    if(dut.io.inMode == MatrixOperation_TypeDef.ElementAdd){
      for(idx <- 0 until (testLength*10)-1){
        println(s"${idx}:ref_array:${ref_array(idx)};dut_array:${dut_array(idx)}")
        assert(dut_array(idx)==ref_array(idx))
      }
    }
  }
}

class SyatolicArray2DUnitSpecial {

}

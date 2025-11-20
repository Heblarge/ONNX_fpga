import spinal.core._
import spinal.lib._
import spinal.lib.experimental.math._
import spinal.core._

case class PriorityEncoder(width: Int) extends Component {
  val io = new Bundle {
    val input = in Bits(width bits)    // 输入信号，任意位宽
    val output = out UInt (log2Up(width) bits) // 输出优先编码，输出位宽根据输入位宽确定
  }
  val ctx=WhenBuilder()//使用whenBuilder语法构造判定链条时，事实上会生成一个if... elseif ...elseif ...else...的verilog语句。
  //因此如果高位判定成功就不会再进行低位的判定了
  for (i <- width-1 downto 0 )//对于输入的width位数据,从高位至低位依次判定是否为1
  {
    ctx.when(io.input(i))
    {    io.output := U(i)    }
  }
  ctx.otherwise{io.output := U(0)}
}
//对于floating类型
//sign:1位即符号位，0:正;1:负
//exponent:exponentSize位指数，在8位exponentSize的情况下需要减去一个127的Offset使得其可以实际上表示2^(-127~128)
//mantissa:23位尾数,默认最高位为1，实际尾数24位，尾数=尾数真值-1(减去最高位1=在规格化形式下-1) 真值：±（1+尾数）*2^(阶码-127)
class FloatingPointAdder(exponentSize: Int, mantissaSize: Int) extends Component {
  val io = new Bundle {
    val a = in(Floating(exponentSize, mantissaSize))
    val b = in(Floating(exponentSize, mantissaSize))
    val result = out((Floating(exponentSize, mantissaSize)) )
  }

  val higher_exp_floating=Mux(io.a.exponent.asUInt > io.b.exponent.asUInt,io.a,io.b)
  val lower_exp_floating=Mux(io.a.exponent.asUInt > io.b.exponent.asUInt,io.b,io.a)

  // 对齐两个操作数的指数
  val expDiff = higher_exp_floating.exponent.asUInt - lower_exp_floating.exponent.asUInt//应该总是为正
  
  val aligned_mantissa_lower_exp_floating = ((U(1, 2 bits) @@ lower_exp_floating.mantissa.asUInt) )|>>(expDiff)
  val aligned_mantissa_higher_exp_floating = (((U(1, 2 bits) @@ higher_exp_floating.mantissa.asUInt) ))  // 扩展尾数以考虑隐式1
  
  //先判定符号是否异号
  val Sign_is_equal=(higher_exp_floating.sign===lower_exp_floating.sign)
  val higher_exp_floating_mantissa_is_bigger=(aligned_mantissa_higher_exp_floating>aligned_mantissa_lower_exp_floating)

  // 根据符号决定尾数是加法还是减法
  val result_mantissa = Mux(Sign_is_equal,
  aligned_mantissa_higher_exp_floating + aligned_mantissa_lower_exp_floating, 
  aligned_mantissa_higher_exp_floating - aligned_mantissa_lower_exp_floating)(mantissaSize+1 downto 0)
  
  // 根据尾数的大小决定结果的符号位
  val resultSign=Bool()
  switch(Sign_is_equal){
    is( True)  {resultSign:=higher_exp_floating.sign}
    is(False)  {resultSign:=Mux(higher_exp_floating_mantissa_is_bigger,
                      higher_exp_floating.sign,
                      lower_exp_floating.sign)}
  }

  // 计算新的指数
  val resultExponent = higher_exp_floating.exponent.asUInt
  
  // 规格化结果,保证尾数的[23]位（最高位）为1，并省略
  val shift_round_bits:Int=1
  val result_mantissa_PriorityEncoder=PriorityEncoder(mantissaSize+2)
  result_mantissa_PriorityEncoder.io.input := result_mantissa.asBits
  val result_mantissa_highest_one=result_mantissa_PriorityEncoder.io.output
  val shift_right=result_mantissa_highest_one>mantissaSize
  val shift_left=result_mantissa_highest_one<mantissaSize
  val num_to_shift=UInt(log2Up(mantissaSize) bits)
  val normalizedMantissa=UInt(mantissaSize bits)
  val normalizedExponent=UInt(exponentSize bits)
  when(shift_right)
  {
    num_to_shift:=result_mantissa_highest_one-mantissaSize
    normalizedMantissa:=((result_mantissa @@ U(0, shift_round_bits bits))|>>(num_to_shift)).roundToEven(shift_round_bits,align=true)(mantissaSize-1 downto 0)
    normalizedExponent:=(resultExponent+num_to_shift)(exponentSize-1 downto 0)
  }.elsewhen(shift_left)
  {
    num_to_shift:=mantissaSize-result_mantissa_highest_one
    normalizedMantissa:=(result_mantissa<<(num_to_shift))(mantissaSize-1 downto 0)
    normalizedExponent:=(resultExponent-num_to_shift)(exponentSize-1 downto 0)
  }.otherwise
  {
    num_to_shift:=0
    normalizedMantissa:=result_mantissa(mantissaSize-1 downto 0)
    normalizedExponent:=resultExponent(exponentSize-1 downto 0)
  }
  
  // 输出结果
  io.result.mantissa := normalizedMantissa.asBits
  io.result.exponent := normalizedExponent.asBits
  io.result.sign := resultSign
}

class FloatingPointMultiplier(exponentSize: Int, mantissaSize: Int) extends Component {
  val io = new Bundle {
    val a = in(Floating(exponentSize, mantissaSize))
    val b = in(Floating(exponentSize, mantissaSize))
    val result = out((Floating(exponentSize, mantissaSize)))
  }

  // 尾数相乘
  val mantissaA = (U(1, 1 bits) @@ io.a.mantissa.asUInt)  // 尾数扩展隐式位
  val mantissaB = (U(1, 1 bits) @@ io.b.mantissa.asUInt) 
  val result_mantissa_unround=(mantissaA * mantissaB)(2*mantissaSize+1 downto 0)

  // 指数相加
  val exponentA = io.a.exponent.asUInt
  val exponentB = io.b.exponent.asUInt
  val resultExponent = (exponentA + exponentB - U(127, exponentSize bits))(exponentSize-1 downto 0) // 去掉127偏移量

  // 符号处理
  val resultSign = io.a.sign ^ io.b.sign

  // 规格化结果,保证尾数的[23]位（最高位）为1，并省略
  //这边的做法和加法器不同，是保留了2*mantissaSize+2位的尾数计算结果，在移位结束之后再进行舍入
  val result_mantissa_PriorityEncoder=PriorityEncoder(2*mantissaSize+2)
  result_mantissa_PriorityEncoder.io.input := result_mantissa_unround.asBits
  val result_mantissa_highest_one=result_mantissa_PriorityEncoder.io.output
  val shift_right=result_mantissa_highest_one>2*mantissaSize
  val shift_left=result_mantissa_highest_one<2*mantissaSize
  val num_to_shift=UInt(log2Up(2*mantissaSize+2) bits)
  val normalizedMantissa=UInt(mantissaSize bits)
  val normalizedExponent=UInt(exponentSize bits)
  when(shift_right)
  {
    num_to_shift:=result_mantissa_highest_one-(2*mantissaSize)
    val normalizedMantissa_unround=((result_mantissa_unround)|>>(num_to_shift))
    normalizedMantissa:=normalizedMantissa_unround.roundToEven(mantissaSize,align=true)(mantissaSize-1 downto 0)
    normalizedExponent:=(resultExponent+num_to_shift)(exponentSize-1 downto 0)
  }.elsewhen(shift_left)
  {
    num_to_shift:=(2*mantissaSize)-result_mantissa_highest_one
    normalizedMantissa:=(result_mantissa_unround<<(num_to_shift)).roundToEven(mantissaSize,align=true)(mantissaSize-1 downto 0)
    normalizedExponent:=(resultExponent-num_to_shift)(exponentSize-1 downto 0)
  }.otherwise
  {
    num_to_shift:=0
    normalizedMantissa:=result_mantissa_unround.roundToEven(mantissaSize,align=true)(mantissaSize-1 downto 0)
    normalizedExponent:=resultExponent(exponentSize-1 downto 0)
  }
  // 输出结果
  io.result.mantissa := normalizedMantissa.asBits
  io.result.exponent := normalizedExponent.asBits
  io.result.sign := resultSign
}


case class  FloatingPointExample() extends Component {
      val io = new Bundle {
        val a = in(Floating32())
        val b = in(Floating32())
        val adder_result = out(Floating32())
        val Multiplier_result = out(Floating32())
      }
      val adder=new FloatingPointAdder(8,23)
      adder.io.a := io.a
      adder.io.b := io.b
      io.adder_result := adder.io.result
      val Multiplier=new FloatingPointMultiplier(8,23)
      Multiplier.io.a := io.a
      Multiplier.io.b := io.b
      io.Multiplier_result := Multiplier.io.result
      
}

import spinal.lib.tools
object FloatingPointAdder_Verilog extends App{
  val testLength=32

    val FileDir = "rtl/FloatingPointAdder/verilog"
    import java.io.File
    new File(FileDir).mkdirs()
    
    SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = false,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(new FloatingPointAdder(8,23)).printPruned()
    
    //tools.HDElkDiagramGen(SpinalVerilog(new FloatingPointAdder(8,23)))
}
object FloatingPointMultiplier_Verilog extends App{
  val testLength=32

    val FileDir = "rtl/FloatingPointMultiplier/verilog"
    import java.io.File
    new File(FileDir).mkdirs()
    
    SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = false,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(new FloatingPointMultiplier(8,23)).printPruned()
    
    //tools.HDElkDiagramGen(SpinalVerilog(new FloatingPointMultiplier(8,23)))
}


import spinal.core.sim._
import spinal.lib.tools
import scala.util.Random
object FloatingPointExample_Sim extends App {
  val FileDir = "rtl/FloatingPointExample_Sim/verilog"
  import java.io.File
  new File(FileDir).mkdirs()

  val defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  //verilator
  /*
  val Sim_compiled = SimConfig.withFstWave.compile(new SystolicArray2D_CC(cfg=cfg,clk_in = ClockDomain.external("clk_in"),
    clk_out = ClockDomain.external("clk_out",withReset=false),
    clk_core = ClockDomain.external("clk_core")))
    */
  //VCS
  import spinal.sim.VCSFlags
  val flag = VCSFlags(
        compileFlags = List("-kdb","-lca", "+notimingchecks"),
        elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"),
        //    runFlags = List("-fgp=num_threads:11,allow_less_cores", "-l ./run.log")
        //    elaborateFlags = List("-fgp", "+notimingchecks"),
        runFlags = List("-l ./run.log")
      )
      val Spinalcfg=SpinalConfig(
        targetDirectory = FileDir,
        oneFilePerComponent = true,
        defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
        bitVectorWidthMax = 20000, //disable internal bigvector limitation"Way too big signal Bits"
        )
      val rtl=Spinalcfg.generateVerilog(new FloatingPointExample()).printPruned()
      //tools.HDElkDiagramGen(rtl)
     // 将 Float 转换为 IEEE 754 格式 (32-bit)，返回符号位、指数部分和尾数部分
  def floatToIEEE754(value: Float): (Int, Int, Int) = {
    val intBits = java.lang.Float.floatToIntBits(value)
    
    // 提取符号位、指数部分和尾数部分
    var sign = (intBits >>> 31) & 0x1           // 符号位 (最高位)
    var exponent = (intBits >>> 23) & 0xFF      // 指数部分 (8 位)
    var mantissa = intBits & 0x7FFFFF           // 尾数部分 (23 位)
    
    // 返回符号位、指数部分和尾数部分
    (sign, exponent, mantissa)
  }

      
  val Sim_compiled=SimConfig
      .withVCS(flag)
      .withFsdbWave
      .withTimeScale(1 ns)
      .withTimePrecision(1 ns)
      .withConfig(Spinalcfg)
      .allOptimisation
      .compile(new FloatingPointExample()) 

    Sim_compiled.doSim("simple test") { dut =>
    SimTimeout(60000)
    dut.clockDomain.forkStimulus(period = 10)
    for (idx <- 0 to 99) {
// Drive the dut inputs with random values
// 生成随机浮点数
    val a = Random.nextFloat()

     
      dut.io.a.sign#=floatToIEEE754(a)._1.toBoolean
      dut.io.a.exponent#=floatToIEEE754(a)._2.toInt
      dut.io.a.mantissa#=floatToIEEE754(a)._3.toInt
   val b = Random.nextFloat()

    dut.io.b.sign#=floatToIEEE754(b)._1.toBoolean
      dut.io.b.exponent#=floatToIEEE754(b)._2.toInt
      dut.io.b.mantissa#=floatToIEEE754(b)._3.toInt
      
      val  ref_adder_result=a+b
      val ref_Multiplier_result=a*b

      val ARS=floatToIEEE754(ref_adder_result)._1
      val ARE=floatToIEEE754(ref_adder_result)._2
      val ARM=floatToIEEE754(ref_adder_result)._3
      val MRS=floatToIEEE754(ref_Multiplier_result)._1
      val MRE=floatToIEEE754(ref_Multiplier_result)._2
      val MRM=floatToIEEE754(ref_Multiplier_result)._3
      
      // Wait a rising edge on the clock
      dut.clockDomain.waitRisingEdge()

      assert(dut.io.adder_result.sign.toBoolean==ARS.toBoolean,s"adder_result.sign should be ${ARS.toBoolean} but is ${dut.io.adder_result.sign.toBoolean}")
      assert(dut.io.adder_result.exponent.toInt==ARE.toInt,s"adder_result.exponent should be ${ARE.toInt} but is ${dut.io.adder_result.exponent.toInt}")
      assert(dut.io.adder_result.mantissa.toInt==ARM.toInt,s"adder_result.mantissa should be ${ARM.toInt} but is ${dut.io.adder_result.mantissa.toInt}")
      
      assert(dut.io.Multiplier_result.sign.toBoolean==MRS.toBoolean,s"Multiplier_result.sign should be ${MRS.toBoolean} but is ${dut.io.Multiplier_result.sign.toBoolean}")
      assert(dut.io.Multiplier_result.exponent.toInt==MRE.toInt,s"Multiplier_result.exponent should be ${MRE.toInt} but is ${dut.io.Multiplier_result.exponent.toInt}")
      assert(dut.io.Multiplier_result.mantissa.toInt==MRM.toInt,s"Multiplier_result.mantissa should be ${MRM.toInt} but is ${dut.io.Multiplier_result.mantissa.toInt}")
 
      // Check that the dut values match with the reference model ones
      println(s"pass: ${idx}/100")
      }

    /*val zeroCases = Seq(
    (0.0f, 1.2345f),
    (-0.0f, 3.21f),
    (1.2345f, 0.0f),
    (3.21f, -0.0f),
    (0.0f, -0.0f)
    )

    for ((a, b) <- zeroCases) {
 // 将浮点数拆为 sign/exponent/mantissa 并驱动 DUT
  val (as, ae, am) = floatToIEEE754(a)
  val (bs, be, bm) = floatToIEEE754(b)

  dut.io.a.sign    #= as.toBoolean
  dut.io.a.exponent#= ae.toInt
  dut.io.a.mantissa#= am.toInt

  dut.io.b.sign    #= bs.toBoolean
  dut.io.b.exponent#= be.toInt
  dut.io.b.mantissa#= bm.toInt

// 等待一个上升沿让模块计算输出
  dut.clockDomain.waitRisingEdge()

//  检查乘法结果是否为 IEEE754 的 0：exponent == 0 && mantissa == 0
  assert(dut.io.Multiplier_result.exponent.toInt == 0,
    s"Expected exponent==0 for product $a * $b, got ${dut.io.Multiplier_result.exponent.toInt}")
  assert(dut.io.Multiplier_result.mantissa.toInt == 0,
    s"Expected mantissa==0 for product $a * $b, got ${dut.io.Multiplier_result.mantissa.toInt}")

//  对于零，符号位应为 sign(a) xor sign(b)
  val expectedSignBool = ((as ^ bs) != 0)
  assert(dut.io.Multiplier_result.sign.toBoolean == expectedSignBool,
    s"Expected sign ${expectedSignBool} for product $a * $b, got ${dut.io.Multiplier_result.sign.toBoolean}")

  println(s" zero-case passed: $a * $b -> +0/-0 sign=${expectedSignBool}")
      }*/
    

    } 
       
    
}
    
package ExponentialFunction

import java.io.File

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._


/**
 * exp_function
 *    basic algorithm: https://ieeexplore.ieee.org/abstract/document/7571158
 *    1. use CORDIC for fraction part.
 *    2. use exp(a+b) = exp(a) * exp(b) for integer part.
 *    3. turn integer part into s-m for better coding
 *
 *                      int_x | frac_x
 * compute direction:     <-  |  ->
 *
 *    input: x (2's complement) [1 + bit_int + bit_frac bits] SInt
 *          !!!  -145 < x < 9  !!!
 *    output: expx [log2Up(Math.exp(x_max).ceil.toInt) = 13 if x_max = 9 (int part) + bit_frac bits] UInt
 *
 *    computation flow: (continuous)
 *    # initial values
 *      frac_x = x[bit_frac-1 : 0]
 *      int_x = x[bit_int+bit_frac : bit_frac] (bit_int+1 bits)
 *      neg = int_x < 0
 *      abs_int_x = int_x.abs
 *      poweroftwo = 0.5
 *      expx_int = 1
 *      expx_frac = 1
 *      exp_frac = round(exp(1/(pow2[1,rotate+1]))*pow2(bit_frac))
 *      exp_int_pos = round(exp(pow2[0,bit_int])*pow2(bit_frac))
 *      exp_int_neg = round(exp(-pow2[0,bit_int])*pow2(bit_frac))
 *
 *      fork
 *      # here we set rotate == bit_int to compute two parts concurrently
 *    # compute for fraction part
 *      for i in range(rotate):
 *        if (poweroftwo < frac_x):
 *          frac_x = frac_x - poweroftwo
 *          expx_frac = expx_frac * exp_frac[i] (floor)
 *       poweroftwo = poweroftwo  /2
 *
 *    # compute for integer part
 *      for i in range(bit_int):
 *          if abs_int_x[i] == 1:
 *              if neg:
 *                 expx_int = expx_int * exp_int_neg[i] (floor)
 *              else:
 *                 expx_int = expx_int * exp_int_pos[i] (floor)
 *      join
 *
 *      return expx_int * expx_frac (floor)
 *
 */
case class EXP_function_cfg(
                             bit_int : Int,
                             bit_frac : Int,
                             x_max : Int
                          ){
  require(bit_int >= 8)
  val rotate = bit_int// nof loop for CORDIC computation
  //如果rotate = bit_int则整数和小数部分的计算会同时完成
  def x_type = SInt(1+bit_int + bit_frac bits)
  def expx_int_bit = log2Up(Math.exp(x_max).ceil.toInt)
  def expx_bit = expx_int_bit + bit_frac
  def expx_type = UInt(expx_bit bits)
  println(s"expx_int_bits: ${expx_int_bit}\t;frac_bits:${bit_frac}")

  private def log2Up(x: Int): Int = {
    var result = 0
    var temp = 1
    while (temp < x) {
      temp *= 2
      result += 1
    }
    result
  }

}

case class EXP_function(cfg : EXP_function_cfg) extends Component {

  import cfg._
  val io = new Bundle {
    val x = slave Flow x_type
    val expx = master Flow expx_type
    val expx_valid_p1 = out Bool() // one tap before expx valid
    val expx_valid_p2 = out Bool() // two taps before expx valid
    val expx_valid_p3 = out Bool() // 3 taps before expx valid
  }

  def pow2(x: Double): Double = Math.pow(2,x)
 // initial values
  val frac_x = Vec(Reg(UInt(bit_frac bits)) init 0,size = rotate+1) //last not used //小数部分
  val int_x = SInt(bit_int+1 bits)//整数部分
  val neg = Vec(Reg(Bool()) init False,size =  rotate)//符号位
  val abs_int_x = Vec(Reg(UInt(bit_int+1 bits)) init 0,size = bit_int)
  val poweroftwo = (1 until( rotate+1)).map(i => U(((1/pow2(i))*pow2(bit_frac)).toInt,bit_frac bits))
  val expx_int = Vec(Reg(expx_type) init 0,size =bit_int+1)//结果整数部分
  val expx_frac = Vec(Reg(expx_type) init 0,size = rotate+1)//结果小数部分
  val exp_frac = (1 until(rotate+1)).map(i => U(Math.round(Math.exp(1/pow2(i))*pow2(bit_frac)).toInt,expx_bit bits))
  val exp_int_pos = (0 until log2Up(x_max)).map(i => U(Math.round(Math.exp(pow2(i))*pow2(bit_frac)).toInt,expx_bit bits))
  val exp_int_neg = (0 until bit_int).map(i => U(Math.round(Math.exp(-pow2(i))*pow2(bit_frac)).toInt,expx_bit bits))
  val expx_final  = Reg(expx_type) init 0

  //give control signals and first x
  val validVec = Vec(Reg(Bool()) init False, rotate+2)
  validVec.reduceLeft((a, b) => {b := a;b})
  validVec(0) := io.x.valid
  abs_int_x.reduceLeft((a,b) => {b := a;b})
  int_x := io.x.payload(bit_int+bit_frac downto(bit_frac))
  abs_int_x(0) := int_x.abs
  neg(0) := io.x.payload < 0
  neg.reduceLeft((a,b) => {b:= a;b})
  frac_x(0) := io.x.payload(bit_frac-1 downto(0)).asUInt
  frac_x.reduceLeft((a,b) => {b:= a;b})
  expx_int(0) := 1*pow2(bit_frac).toInt
  expx_int.reduceLeft((a,b) => {b:= a;b})
  expx_frac(0) := 1*pow2(bit_frac).toInt
  expx_frac.reduceLeft((a,b) => {b:= a;b})

  // fraction part computation
// 遍历rotate次，处理分数部分的转换
for (i <- 0 until rotate){
  // 当当前位的poweroftwo值小于frac_x值时，进行转换计算
  when (poweroftwo(i) < frac_x(i)){
    // 更新下一位的frac_x值为当前位的frac_x值减去poweroftwo值
    frac_x(i+1) := frac_x(i) - poweroftwo(i)
    // 更新下一位的expx_frac值为当前位的expx_frac值与exp_frac值相乘的结果，
    // 并对结果进行向下取整及饱和处理
    expx_frac(i+1) := (expx_frac(i) * exp_frac(i)).sat(expx_int_bit).floor(bit_frac)
  }
}
  // 整数部分计算
  // 迭代计算指数值的整数部分
  for (i <- 0 until bit_int){
    //检查当前位是否为1，如果是1则需要更新结果的指数值，不然就不需要更新
    when(abs_int_x(i)(i)){
      // 当输入的当前位为负时，expx_int会越来越小
      when(neg(i)){
        // 通过将当前指数值乘以预计算的负指数因子来更新指数值
        // 并进行取整和饱和操作以确保结果不超过表示范围
        expx_int(i+1) := (expx_int(i) * exp_int_neg(i)).sat(expx_int_bit).floor(bit_frac)
      }otherwise{
        // 当输入的当前位为正时，expx_int会越来越大，因此需要检查当前位位置是否在预定义的最大范围内
        if (i < log2Up(x_max)){
          // 通过将当前指数值乘以预计算的正指数因子来更新指数值
          // 并进行取整和饱和操作以确保结果不超过表示范围
          expx_int(i+1) := (expx_int(i) * exp_int_pos(i)).sat(expx_int_bit).floor(bit_frac)
        }
      }
    }
  }

  expx_final := (expx_frac.last * expx_int.last).floor(bit_frac).sat(expx_int_bit)

  io.expx_valid_p1 := validVec(rotate)
  io.expx_valid_p2 := validVec(rotate-1)
  io.expx_valid_p3 := validVec(rotate-2)
  io.expx.valid := validVec.last
  io.expx.payload := expx_final

}

object exp_function_001 {

  new File("rtl/EXP_function").mkdir()
  val cfg = EXP_function_cfg(
    bit_int = 8,
    bit_frac = 12,
    x_max = 9
 )

  def main(arg:Array[String]): Unit ={
    SpinalConfig(
      targetDirectory = "rtl/EXP_function",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(EXP_function(cfg))
      .printPruned()
  }
}



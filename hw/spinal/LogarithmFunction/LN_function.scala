package LogarithmFunction

import spinal.core._
import spinal.lib._
import scala.math._


case class LN_function_cfg(
                            bit_int: Int,// 整数部分的位数
                            bit_frac: Int   // 小数部分的位数
                          ) {
  require(bit_int >= 8)  // 要求整数部分至少为8位，确保足够的动态范围

  val rotate = bit_frac // CORDIC 算法的迭代次数等于小数部分的位数
  val using_compensation_iters = true // 启用补偿迭代，针对特定迭代点（j=4和j=13）减少误差
  val x_in_Min=0.0001
  val x_in_Max=256 -(1* Math.pow(2, -bit_frac))
    //6.0-(1* Math.pow(2, -bit_frac))

  // 定义输入数据类型：无符号整数，总位数为整数部分 + 小数部分
  def x_type = UInt(bit_int + bit_frac bits)

  // 计算 ln(2) 的整数部分所需的位数（向上取整的对数）
  def ln_int_bit = log2Up(Math.log(2).ceil.toInt)

  // 输出 ln(x) 的总位数：整数部分 + 小数部分 + 额外的整数位
  def ln_bit = ln_int_bit + bit_frac + bit_int

  // 定义输出数据类型：有符号整数，位数为 ln_bit
  def ln_type = SInt(ln_bit bits)

  // 使用泰勒级数计算 atanh(x) 的近似值
  def atanh_taylor(x: Double, terms: Int = 10): Double = {
    var sum = 0.0
    for (i <- 0 until terms) {
      val term = Math.pow(x, 2 * i + 1) / (2 * i + 1)
      sum += term
    }
    sum
  }

  def atanh(x: Double): Double = 0.5 * log((1 + x) / (1 - x))

}


// 计算前导零个数的模块
// Counts the number of consecutive zero bits starting from the MSB.
object CountLeadingZeroes {
  def apply(in: Bits): UInt = {
    val padLen = (1 << log2Up(in.getWidth)) - in.getWidth

    if (in.getWidth == 0) U(0)
    else if (in.getWidth == 1) ~in.asUInt
    else if (padLen != 0) {
      // 如果输入位数不是2的幂，则填充零后递归计算，并减去填充的位数
      return CountLeadingZeroes(B(0, padLen bits) ## in) - padLen
    } else {
      val w = in.getWidth // input width
      assert(w % 2 == 0 && w > 0, s"cannot do clz for width $w")

      val ow = log2Up(w) + 1 // output width 输出宽度
      val oLrw = ow - 1 // output width of halves 半部分输出的宽度
      // 递归计算左半部分和右半部分的前导零个数
      val clzL = CountLeadingZeroes(in(w / 2, w / 2 bits))
      val clzR = CountLeadingZeroes(in(0, w / 2 bits))
      // 组合结果：如果左半部分全零，则结果为右半部分结果加上左半部分的宽度
      val first = clzL(oLrw - 1) & clzR(oLrw - 1)
      val mux = Mux(~clzL(oLrw - 1),
        (U("0") ## clzL(0, oLrw - 1 bits)),
        (~clzR(oLrw - 1)) ## clzR(0, oLrw - 1 bits)
      )
      (first ## mux).asUInt
    }
  }
}
// 规范化模块，将输入调整到接近1的范围，并记录移位次数k
case class Normalizer(cfg: LN_function_cfg) extends Component {
  import cfg._

  val io = new Bundle {
    val x_in = in(x_type)          // 输入值
    val valid_in = in(Bool())      // 输入有效信号
    val x_out = out(x_type)        // 规范化后的输出值
    val k_out = out(SInt(bit_int bits)) // 移位次数k（有符号）
    val valid_out = out Bool()     // 输出有效信号
  }

  val shift_amount = UInt(log2Up(bit_int + bit_frac) + 1 bits) // 移位量
  val k = Reg(SInt(bit_int bits)) init(0) // 移位次数寄存器

  // 计算前导零的个数
  val lz = CountLeadingZeroes(io.x_in.asBits)

  // 延迟输入和有效信号
  val x_in_d = RegNext(io.x_in)
  val valid_in_d = RegNext(io.valid_in) init(False)

  val threshold = S(bit_int - 1) // 阈值，用于判断移位方向
  when(lz < threshold.asUInt) {
    // 如果前导零少于阈值，需要右移
    shift_amount := threshold.asUInt - lz
    k := (threshold - lz.asSInt).resized
  }.elsewhen(lz > threshold.asUInt) {
    // 如果前导零多于阈值，需要左移
    shift_amount := lz - threshold.asUInt
    k := (threshold - lz.asSInt).resized
  }.otherwise {
    // 无需移位
    shift_amount := 0
    k := S(0)
  }

  // 多级寄存器延迟以同步流水线
  val x_in_dd = RegNext(x_in_d)
  val valid_in_dd = RegNext(valid_in_d) init(False)
  val shift_amount_d = RegNext(shift_amount) init(0)
  val shift_amount_dd = RegNext(shift_amount_d) init(0)

  // 预计算所有可能的右移结果
  val right_shift_results = Vec.fill(bit_int)(UInt(2*bit_int + bit_frac bits))
  for (i <- 0 until bit_int) {
    right_shift_results(i) := (x_in_dd >> i).resized
  }

  // 预计算所有可能的左移结果
  val left_shift_results = Vec.fill(bit_frac)(UInt(2*bit_int + bit_frac bits))
  for (i <- 0 until bit_frac) {
    left_shift_results(i) := (x_in_dd << i).resized
  }

  // 根据k的符号选择移位方向（左移或右移）
  val shifted_x_comb = Mux(valid_in_dd,
    Mux(!RegNext(k).msb,  // 如果k为正（之前是右移）
      right_shift_results(shift_amount_dd.resized),
      left_shift_results(shift_amount_dd.resized)
    ),
    U(0)
  )

  // 输出寄存器
  val valid_reg = valid_in_dd
  val x_out_reg = shifted_x_comb
  val k_out_reg = k

  io.x_out := x_out_reg.resized
  io.k_out := k_out_reg.resized
  io.valid_out := valid_reg
}

// 主计算模块：使用CORDIC算法计算自然对数
case class LN_function(cfg: LN_function_cfg) extends Component {
  import cfg._
  val io = new Bundle {
    val x = slave Flow x_type    // 输入流，包含数据和有效信号
    val lnx = master Flow ln_type // 输出流，包含结果和有效信号
  }

  // 预计算atanh(2^(-i-1))的查找表，转换为定点数
  val atanh_lut = Vec.fill(rotate)(SInt(ln_bit + 1 bits))
  for (i <- 0 until rotate) {
    atanh_lut(i) := S((atanh(Math.pow(2, -i - 1)) * Math.pow(2, bit_frac)).toInt, ln_bit + 1 bits).resized
  }

  // 实例化规范化模块
  val normalizer = Normalizer(cfg)
  normalizer.io.x_in := io.x.payload
  normalizer.io.valid_in := io.x.valid

  // 移位次数k的流水线寄存器
  val iteration_k = Vec.fill(rotate + 2)(Reg(SInt(bit_int bits)) init(0))
  iteration_k(0) := normalizer.io.k_out
  iteration_k.reduceLeft((a, b) => {b := a;b})

  // CORDIC计算中的变量寄存器：x、y、z和有效信号
  val x_n = Vec.fill(rotate + 1)(Reg(SInt(bit_int + bit_frac bits)) init(0))
  val y_n = Vec.fill(rotate + 1)(Reg(SInt(bit_int + bit_frac bits)) init(0))
  val z_n = Vec.fill(rotate + 1)(Reg(SInt(ln_bit + 1 bits)) init(0))
  val iteration_valid = Vec.fill(rotate + 1)(Reg(Bool()) init(False))

  // 初始化CORDIC变量
  when(normalizer.io.valid_out) {
    // 根据公式初始化x和y：x = (normalized_x + 1), y = (normalized_x - 1)
    x_n(0) := (normalizer.io.x_out + (1 << bit_frac)).asSInt.resized
    y_n(0) := (normalizer.io.x_out - (1 << bit_frac)).asSInt.resized
    z_n(0) := 0
  }
  iteration_valid(0) := normalizer.io.valid_out
  iteration_valid.reduceLeft((a, b) => {b := a;b})

  // CORDIC迭代循环
  for (i <- 0 until rotate) {
    when(iteration_valid(i)) {
      val sign_y = !y_n(i).msb  // 判断y的符号
      val shift = y_n(i) >> (i + 1) // 算术右移

      val x_next = SInt(bit_int + bit_frac bits)
      val y_next = SInt(bit_int + bit_frac bits)
      val z_next = SInt(ln_bit + 1 bits)

      // 在特定迭代点（i=4-1和i=13-1）启用补偿迭代，进行两次迭代以减少误差
      if(i == 4-1 || i == 13-1 && cfg.using_compensation_iters){
        // 第一次迭代
        val sign_y_1 = sign_y
        val shift_1 = shift
        val x_1 = SInt(bit_int + bit_frac bits)
        val y_1 = SInt(bit_int + bit_frac bits)
        val z_1 = SInt(ln_bit + 1 bits)
        when(sign_y_1) {
          x_1 := x_n(i) - shift_1
          y_1 := y_n(i) - (x_n(i) >> (i + 1))
          z_1 := z_n(i) + atanh_lut(i)
        } otherwise {
          x_1 := x_n(i) + shift_1
          y_1 := y_n(i) + (x_n(i) >> (i + 1))
          z_1 := z_n(i) - atanh_lut(i)
        }
        // 第二次迭代
        val sign_y_2 = !y_1.msb
        val shift_2 = y_1 >> (i + 1)
        when(sign_y_2) {
          x_next := x_1 - shift_2
          y_next := y_1 - (x_1 >> (i + 1))
          z_next := z_1 + atanh_lut(i)
        } otherwise {
          x_next := x_1 + shift_2
          y_next := y_1 + (x_1 >> (i + 1))
          z_next := z_1 - atanh_lut(i)
        }
        x_n(i+1) := x_next
        y_n(i+1) := y_next
        z_n(i+1) := z_next
      } else {
        // 正常迭代
        when(sign_y) {
          x_next := x_n(i) - shift
          y_next := y_n(i) - (x_n(i) >> (i + 1))
          z_next := z_n(i) + atanh_lut(i)
        } otherwise {
          x_next := x_n(i) + shift
          y_next := y_n(i) + (x_n(i) >> (i + 1))
          z_next := z_n(i) - atanh_lut(i)
        }
        x_n(i+1) := x_next
        y_n(i+1) := y_next
        z_n(i+1) := z_next
      }
    }
  }

  // 最终结果计算：z_n * 2 + k * ln(2)（定点数缩放）
  val ln_x = ((z_n.last << 1) + iteration_k.last * ((Math.log(2) * Math.pow(2, bit_frac)).toInt)).resize(ln_bit bits)

  // 输出结果和有效信号
  io.lnx.valid := RegNext(iteration_valid.last)
  io.lnx.payload := RegNext(ln_x)
}


object LN_function_Gen {
  def main(args: Array[String]): Unit = {
    val cfg = LN_function_cfg(
      bit_frac = 12,
      bit_int = 8
    )

    SpinalConfig(
      targetDirectory = "rtl/LN_function",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(LN_function(cfg))
  }
}

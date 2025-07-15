package LogarithmFunction

import spinal.core._
import spinal.lib._


case class LN_function_cfg(
                            bit_int: Int,
                            bit_frac: Int
                          ) {
  require(bit_int >= 8)

  val rotate = bit_frac // CORDIC 计算的迭代次数
  val using_compensation_iters = true //由于atanh(2^-j)的值在j=4和j=13处相对较大，累积误差明显，启用将在这些点处使用额外迭代
  def x_type = UInt(bit_int + bit_frac bits)
  def ln_int_bit = log2Up(Math.log(2).ceil.toInt)
  def ln_bit = ln_int_bit + bit_frac + bit_int
  def ln_type = SInt(ln_bit bits)

  //atanh LUT
  def atanh_taylor(x: Double, terms: Int = 10): Double = {
    var sum = 0.0
    for (i <- 0 until terms) {
      val term = Math.pow(x, 2 * i + 1) / (2 * i + 1)
      sum += term
    }
    sum
  }
}

//case class PriorityEncoder(width: Int) extends Component {
//  val io = new Bundle {
//    val input = in Bits(width bits)
//    val output = out UInt (log2Up(width) bits)
//  }
//  val ctx = WhenBuilder()
//  for (i <- width-1 downto 0 ) {
//    ctx.when(io.input(i)) {
//      io.output := U(i)
//    }
//  }
//  ctx.otherwise{io.output := U(0)}
//}
//
//case class LeadingZeros(width: Int) extends Component {
//  val io = new Bundle {
//    val input = in Bits(width bits)
//    val output = out UInt (log2Up(width) bits)
//  }
//  val priorityEncoder = PriorityEncoder(width)
//  priorityEncoder.io.input := io.input
//  val leadingZeroCount = UInt(log2Up(width) bits)
//  when(priorityEncoder.io.output === U(0)) {
//    leadingZeroCount := U(width)
//  } otherwise {
//    leadingZeroCount := width - priorityEncoder.io.output - 1
//  }
//  io.output := leadingZeroCount
//}


// Counts the number of consecutive zero bits starting from the MSB.
object CountLeadingZeroes {
  def apply(in: Bits): UInt = {
    val padLen = (1 << log2Up(in.getWidth)) - in.getWidth

    if (in.getWidth == 0) U(0)
    else if (in.getWidth == 1) ~in.asUInt
    else if (padLen != 0) {
      return CountLeadingZeroes(B(0, padLen bits) ## in) - padLen
    } else {
      val w = in.getWidth // input width
      assert(w % 2 == 0 && w > 0, s"cannot do clz for width $w")

      val ow = log2Up(w) + 1 // output width
      val oLrw = ow - 1 // output width of halves

      val clzL = CountLeadingZeroes(in(w / 2, w / 2 bits))
      val clzR = CountLeadingZeroes(in(0, w / 2 bits))

      val first = clzL(oLrw - 1) & clzR(oLrw - 1)
      val mux = Mux(~clzL(oLrw - 1),
        (U("0") ## clzL(0, oLrw - 1 bits)),
        (~clzR(oLrw - 1)) ## clzR(0, oLrw - 1 bits)
      )
      (first ## mux).asUInt
    }
  }
}

case class Normalizer(cfg: LN_function_cfg) extends Component {
  import cfg._

  val io = new Bundle {
    val x_in = in(x_type)
    val valid_in = in(Bool())
    val x_out = out(x_type)
    val k_out = out(SInt(bit_int bits))
    val valid_out = out Bool()
  }

  val shift_amount = UInt(log2Up(bit_int + bit_frac) + 1 bits)
  val k = Reg(SInt(bit_int bits)) init(0)
  //val lz_old = Reg(UInt(log2Up(bit_int + bit_frac) + 1 bits)) init(0)

  //val LeadingZeros_init = LeadingZeros(bit_int + bit_frac)
  //LeadingZeros_init.io.input := io.x_in.asBits
  //lz_old := LeadingZeros_init.io.output.resized
  val lz = CountLeadingZeroes(io.x_in.asBits)

  val x_in_d = RegNext(io.x_in)
  val valid_in_d = RegNext(io.valid_in) init(False)

  val threshold = S(bit_int - 1)
  when(lz < threshold.asUInt) {//右移
    shift_amount := threshold.asUInt - lz
    k := (threshold - lz.asSInt).resized//>0
  }.elsewhen(lz > threshold.asUInt) {//左移
    shift_amount := lz - threshold.asUInt
    k := (threshold - lz.asSInt).resized//<0
  }.otherwise {//不动
    shift_amount := 0
    k := S(0)
  }
  val x_in_dd = RegNext(x_in_d)
  //val x_in_ddd = RegNext(x_in_d)
  val valid_in_dd = RegNext(valid_in_d) init(False)
  val shift_amount_d = RegNext(shift_amount) init(0)
  val shift_amount_dd = RegNext(shift_amount_d) init(0)

  val right_shift_results = Vec.fill(bit_int)(UInt(2*bit_int + bit_frac bits))
  val left_shift_results = Vec.fill(bit_frac)(UInt(2*bit_int + bit_frac bits))
  for (i <- 0 until bit_int) {
    right_shift_results(i) := (x_in_dd >> i).resized  // 右移
  }
  for (i <- 0 until bit_frac) {
    left_shift_results(i) := (x_in_dd << i).resized   // 左移
  }
  //val valid_in_ddd = RegNext(valid_in_d) init(False)

  val shifted_x_comb = Mux(valid_in_dd,
    Mux(!RegNext(k).msb,
      right_shift_results(shift_amount_dd.resized),
      left_shift_results(shift_amount_dd.resized) // 当 k === 0 时，不移位
      ),
    U(0)
  )

  val valid_reg = (valid_in_dd)
  val x_out_reg = (shifted_x_comb)
  val k_out_reg = (k)

  io.x_out := x_out_reg.resized//x_out_reg.resized
  io.k_out := k_out_reg.resized
  io.valid_out := valid_reg
}


case class LN_function(cfg: LN_function_cfg) extends Component {
  import cfg._
  val io = new Bundle {
    val x = slave Flow x_type
    val lnx = master Flow ln_type
  }

  def pow2(x: Double): Double = Math.pow(2, x)
  //val k = Reg(SInt(bit_int bits)) init 0
  val iteration_k = Vec.fill(rotate + 2)(Reg(SInt(bit_int bits)) init(0))

  val atanh_lut = Vec.fill(rotate)(SInt(ln_bit + 1 bits))
  for (i <- 0 until rotate) {
    atanh_lut(i) := S((atanh_taylor(Math.pow(2, -i - 1)) * Math.pow(2, bit_frac)).toInt, ln_bit + 1 bits).resized
  }

  val normalizer = Normalizer(cfg)
  normalizer.io.x_in := io.x.payload
  normalizer.io.valid_in := io.x.valid
  iteration_k(0) := normalizer.io.k_out
  iteration_k.reduceLeft((a, b) => {b := a;b})

  // CORDIC 计算 ln(x) = 2 * atanh((x-1)/(x+1))
  val x_n = Vec.fill(rotate + 1)(Reg(SInt(bit_int + bit_frac bits)) init(0))
  val y_n = Vec.fill(rotate + 1)(Reg(SInt(bit_int + bit_frac bits)) init(0))
  val z_n = Vec.fill(rotate + 1)(Reg(SInt(ln_bit + 1 bits)) init(0))
  val iteration_valid = Vec.fill(rotate + 1)(Reg(Bool()) init(False))


  when(normalizer.io.valid_out) {
    x_n(0) := (normalizer.io.x_out + (1 << bit_frac)).asSInt.resized
    y_n(0) := (normalizer.io.x_out - (1 << bit_frac)).asSInt.resized
    z_n(0) := 0
    //iteration_valid(0) := True
  }
  iteration_valid(0) := normalizer.io.valid_out
  iteration_valid.reduceLeft((a, b) => {b := a;b})

  for (i <- 0 until rotate) {
    when(iteration_valid(i)) {
      val sign_y = !y_n(i).msb
      val shift = y_n(i) >> (i + 1)
      val x_next = SInt(bit_int + bit_frac bits)
      val y_next = SInt(bit_int + bit_frac bits)
      val z_next = SInt(ln_bit + 1 bits)
//      if(true){
//        when(sign_y) {
//          x_next := x_n(i) - shift
//          y_next := y_n(i) - (x_n(i) >> (i + 1))
//          z_next := z_n(i) + atanh_lut(i)
//        } otherwise {
//          x_next := x_n(i) + shift
//          y_next := y_n(i) + (x_n(i) >> (i + 1))
//          z_next := z_n(i) - atanh_lut(i)
//        }
//        x_n(i+1) := x_next
//        y_n(i+1) := y_next
//        z_n(i+1) := z_next
//      }

      if(i == 4-1 || i == 13-1 && cfg.using_compensation_iters){//一个周期迭代两次
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
        // 最终结果：两次迭代后的结果写入寄存器
        x_n(i+1) := x_next
        y_n(i+1) := y_next
        z_n(i+1) := z_next
      }else{
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

//      if (cfg.using_compensation_iters) {
//        if(i == 4 || i == 13) {
//          val sign_y_comp = !y_n(i+1).msb
//          val shift_comp = y_n(i+1) >> (i + 1)
//          when(sign_y_comp) {
//            x_n(i+1) := x_n(i+1) - shift_comp
//            y_n(i+1) := y_n(i+1) - (x_n(i+1) >> (i + 1))
//            z_n(i+1) := z_n(i+1) + atanh_lut(i)
//          } otherwise {
//            x_n(i+1) := x_n(i+1) + shift_comp
//            y_n(i+1) := y_n(i+1) + (x_n(i+1) >> (i + 1))
//            z_n(i+1) := z_n(i+1) - atanh_lut(i)
//          }
//        }
//      }

      //iteration_valid(i + 1) := True
    }
  }
  val ln_x = ((z_n.last << 1) + iteration_k.last * ((Math.log(2) * Math.pow(2, bit_frac)).toInt)).resize(ln_bit bits)
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

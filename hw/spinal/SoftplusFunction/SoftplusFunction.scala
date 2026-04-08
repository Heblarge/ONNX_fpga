package SoftplusFunction

import spinal.core._
import spinal.lib._

case class Softplus_function_cfg(
                                  bit_int: Int,
                                  bit_frac: Int,
                                  K1: Int = 7,
                                  K2: Int = 6,
                                  K3: Int = 6,
                                  t_range: (Int, Int) = (-16, 16))
{
  def total_bits = K1 + K2 + K3
  def scale_inv = ((1 << total_bits) - 1) / (t_range._2 - t_range._1)
  def bit_all = 1 + bit_int + bit_frac
  def x_Type = SInt(bit_all bits)
  def softplusx_Type = SInt(bit_all bits) // 最终输出位宽

  // 添加输入范围定义
  val x_in_Min = -256                                // (-256.0, 239.722412109375)
  val x_in_Max = 256 - (1 * Math.pow(2, -bit_frac))  // 动态根据bit_frac计算
}

case class Softplus_function(cfg: Softplus_function_cfg) extends Component {
  import cfg._
  val io = new Bundle {
    val x = slave Flow x_Type
    val softplusx = master Flow softplusx_Type
  }

  private def generatePTable():Vec[SInt] = {
    def alphamid(xh: Int, xm: Int, xl: Int): Double = {
      val idx = (xh << (K2 + K3)) | (xm << K3) | xl
      val t_mid = t_range._1 + idx.toDouble / ((1 << total_bits) - 1) * (t_range._2 - t_range._1)
      Math.log(1 + Math.exp(t_mid))
    }

    Vec.tabulate(1 << (K1 + K2)) { case idx =>
      val xh = idx >> K2
      val xm = idx & ((1 << K2) - 1)

      val spread = alphamid(xh, xm, 0) - alphamid(xh, xm, (1 << K3)-1)
      val avg_spread = {
        val first = alphamid(xh, 0, 0) - alphamid(xh, 0, (1 << K3)-1)
        val last = alphamid(xh, (1 << K2)-1, 0) - alphamid(xh, (1 << K2)-1, (1 << K3)-1)
        (first + last) / 2
      }
      val adjust = (avg_spread - spread) / 2
      val value = (alphamid(xh, xm, 0) + adjust) * (1 << bit_frac)
      S(value.toInt, bit_all bits)
    }
  }

  private def generateNTable():Vec[SInt] = {
    def alphamid(xh: Int, xm: Int, xl: Int): Double = {
      val idx = (xh << (K2 + K3)) | (xm << K3) | xl
      val t_mid = t_range._1 + idx.toDouble / ((1 << total_bits) - 1) * (t_range._2 - t_range._1)
      Math.log(1 + Math.exp(t_mid))
    }

    Vec.tabulate(1 << (K1 + K3)) { case idx =>
      val xh = idx >> K3
      val xl = idx & ((1 << K3) - 1)

      val avgDiff = {
        val diff0 = alphamid(xh, 0, xl) - alphamid(xh, 0, 0)
        val diff1 = alphamid(xh, (1 << K2)-1, xl) - alphamid(xh, (1 << K2)-1, 0)
        (diff0 + diff1) / 2
      }
      val value = avgDiff * (1 << bit_frac)
      S(value.toInt, bit_all bits)
    }
  }

  // 每个实例独立生成表
  val P_table = generatePTable()
  val N_table = generateNTable()

  // 渐近边界定点常量: t_max << bit_frac 和 t_min << bit_frac
  val tMaxFixed = S(t_range._2 << bit_frac, bit_all bits)
  val tMinFixed = S(t_range._1 << bit_frac, bit_all bits)

  // 渐近判断: softplus(x) ≈ x when x >> 0, softplus(x) ≈ 0 when x << 0
  val isAboveRange = io.x.payload >= tMaxFixed
  val isBelowRange = io.x.payload <= tMinFixed

  // 计算索引 (LUT path)
  val idx = (((io.x.payload) - S(t_range._1 << bit_frac, bit_all bits)) * scale_inv) >> bit_frac
  val xh  = idx >> (K2 + K3)
  val rem = idx - (xh << (K2 + K3))
  val xm  = rem >> K3
  val xl  = rem - (xm << K3)

  val xh_reg = RegNext(xh)
  val xm_reg = RegNext(xm)
  val xl_reg = RegNext(xl)

  // 查表阶段
  val P_raw = RegNext(P_table((xh_reg.resize(K1 bits) ## xm_reg.resize(K2 bits)).asUInt))
  val N_raw = RegNext(N_table((xh_reg.resize(K1 bits) ## xl_reg.resize(K3 bits)).asUInt))

  val sum = RegNext(P_raw + N_raw)

  val validVec = Vec(Reg(Bool()) init False, 5)
  validVec.reduceLeft((a, b) => {b := a;b})
  validVec(0) := io.x.valid
  val xVec = Vec(Reg(x_Type) init 0, 3)
  xVec.reduceLeft((a, b) => {b := a;b})
  xVec(0) := io.x.payload

  val lowerBound = S(t_range._1 << bit_frac, bit_all bits)
  val upperBound = S(t_range._2 << bit_frac, bit_all bits)
  val final_val = SInt(bit_all bits)

  when(xVec(2) < lowerBound) {
    final_val := 0
  } elsewhen (xVec(2) > upperBound) {
    final_val := xVec(2)
  } otherwise {
    final_val := sum
  }

  io.softplusx.payload := RegNext(final_val) init 0
  io.softplusx.valid := validVec(3) init False

  
  // 输出选择: 超上界→y=x, 超下界→y=0, 范围内→LUT结果
  // val result = SInt(bit_all bits)
  // when(isAboveRange) {
  //   result := io.x.payload
  // }.elsewhen(isBelowRange) {
  //   result := S(0, bit_all bits)
  // }.otherwise {
  //   result := sum
  // }

  // io.softplusx.payload := RegNext(result) init 0
  // io.softplusx.valid := RegNext(io.x.valid) init False
}

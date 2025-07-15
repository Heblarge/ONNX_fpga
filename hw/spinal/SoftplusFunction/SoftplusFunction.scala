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

  // 计算索引
  val idx = (((io.x.payload) - S(t_range._1 << bit_frac, bit_all bits)) * scale_inv) >> bit_frac
  val xh  = idx >> (K2 + K3)
  val rem = idx - (xh << (K2 + K3))
  val xm  = rem >> K3
  val xl  = rem - (xm << K3)

  // 查表阶段
  val P_raw = P_table((xh.resize(K1 bits) ## xm.resize(K2 bits)).asUInt)
  val N_raw = N_table((xh.resize(K1 bits) ## xl.resize(K3 bits)).asUInt)

  val sum = P_raw + N_raw

  io.softplusx.payload := RegNext(sum) init 0
  io.softplusx.valid := RegNext(io.x.valid) init False
}
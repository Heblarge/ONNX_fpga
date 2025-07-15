package SoftplusFunction

import spinal.core._
import spinal.lib._

case class SoftplusFunction_cfg(
                                 bit_int : Int,
                                 bit_frac : Int,
                               ) {
  // 参数配置部分保持不变
  val K1 = 7
  val K2 = 6
  val K3 = 6
  val g1 = 10
  val g2 = 15
  val t_range = (-16, 16)
  val total_bits = K1 + K2 + K3

  val scale_inv = ((1 << total_bits) - 1)/ (t_range._2 - t_range._1)

  def bit_all = 1 + bit_int + bit_frac
  def x_Type = SInt(bit_all bits)

  def softplusx_Type = UInt(bit_all bits) // 最终输出位宽

  def alphamid(xh: Int, xm: Int, xl: Int): Double = {
    val idx = (xh << (K2 + K3)) | (xm << K3) | xl
    val t_mid = t_range._1 + (idx.toDouble / ((1 << total_bits)-1)) * (t_range._2 - t_range._1)
    Math.log(1 + Math.exp(t_mid))
  }

  // 预计算所有表项
  lazy val P_table: Vec[UInt] = {
    val entries = for {
      xh <- 0 until (1 << K1)
      xm <- 0 until (1 << K2)
    } yield {
      val spread = alphamid(xh, xm, 0) - alphamid(xh, xm, (1 << K3)-1)
      val avg_spread = {
        val firstspread = alphamid(xh, 0, 0) - alphamid(xh, 0, (1 << K3)-1)
        val lastspread  = alphamid(xh, (1 << K2)-1, 0) - alphamid(xh, (1 << K2)-1, (1 << K3)-1)
        (firstspread + lastspread) / 2
      }
      val adjust = (avg_spread - spread) / 2
      U((( alphamid(xh, xm, 0) + adjust) * (1 << bit_frac)).toInt, bit_all bits)
    }
    Vec(entries)
  }

  lazy val N_table: Vec[UInt] = {
    val entries = for {
      xh <- 0 until (1 << K1)
      xl <- 0 until (1 << K3)
    } yield {
      // 计算调整后的N值
      val avgDiff = {
        val diff0 = alphamid(xh, 0, xl) - alphamid(xh, 0, 0)
        val diff1 = alphamid(xh, (1 << K2)-1, xl) - alphamid(xh, (1 << K2)-1, 0)
        (diff0 + diff1) / 2
      }
      U((avgDiff * (1 << bit_frac)).toInt, bit_all bits)
    }
    Vec(entries)
  }
}

case class SoftplusFunction(cfg: SoftplusFunction_cfg) extends Component {
  import cfg._
  val io = new Bundle {
    val x = slave Flow x_Type
    val softplusx = master Flow softplusx_Type
  }
  val valid_ShiftRegister = Vec(RegInit(False), 3)
  valid_ShiftRegister.reduceLeft((a, b) => { b := a; b })
  valid_ShiftRegister(0) := io.x.valid

  val scaledIdx = RegNext((io.x.payload) - S(t_range._1 << bit_frac, bit_all bits))

  val idx = (scaledIdx.asUInt * scale_inv)>>bit_frac
  //val idx = (scaledIdx.asUInt * U((1 << total_bits) - 1, bit_all bits)) / U(t_range._2 - t_range._1, bit_all bits)
  val xh = (idx >> (K2 + K3)).resize(K1 bits)
  val rem = idx - (xh << (K2 + K3))
  val xm = (rem >> K3).resize(K2 bits)
  val xl = (rem - (xm << K3)).resize(K3 bits)

  val P_raw = P_table((xh ## xm).asUInt)
  val N_raw = N_table((xh ## xl).asUInt)
  val P = RegNext(P_raw)
  val N = RegNext(N_raw)

  val sum = RegNext(P + N)
  io.softplusx.payload := sum.resized

  io.softplusx.valid := valid_ShiftRegister.last // 延迟3周期
}
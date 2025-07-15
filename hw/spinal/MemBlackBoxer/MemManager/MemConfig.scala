package MemBlackBoxer.MemManager

import spinal.core._
import scala.language.postfixOps

case class MemConfig
(
  dataWidth: Int,  // word bit width
  depth: Int, // sram depth
  vendor: MemVendor,
  maskBitWidth: Int = 0, // bit width of write mask
  withBist: Boolean = true,
  withScan: Boolean = false,
  withPowerGate: Boolean = false,
  needBwe: Boolean = false
) {
  val dw = dataWidth
  val addrWidth = log2Up(depth)
  val bytePerWord = (dw+7)/8
  val size = bytePerWord * (1 << addrWidth)

  def bitPerMask = if (maskBitWidth == 0) 0 else math.ceil(dataWidth.toDouble / maskBitWidth).toInt
  val noMask : Boolean = maskBitWidth == 0
  val nofMask = if(maskBitWidth == 0) 0 else math.ceil(dataWidth.toDouble / bitPerMask).toInt

  var name = vendor.prefixName + "w" + depth.toString + "d" + dataWidth.toString + (if(noMask) "" else "MW")

  def maskName: String = bitPerMask match {
    case 16 => "wordMask"
    case  8 => "ByteMask"
    case  1 => "BitMask"
    case  0 => "noMask"
    case  _ => SpinalError("Undefined write mask type")
  }

  def genMask: Bits = Bits(maskBitWidth bit)
}

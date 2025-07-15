package MemBlackBoxer.MemManager

import spinal.core._
import spinal.lib._

class MemPorts extends Bundle {

}

case class AddrCtrlPorts(mc: MemConfig) extends MemPorts with IMasterSlave {
  val memoryEnable = Bool()
  val mask = if(!mc.noMask) mc.genMask else null
  val address = UInt(mc.addrWidth bit)

  override def asMaster(): Unit = {
    in(memoryEnable, mask, address)
  }
}

case class DataPorts(mc: MemConfig) extends MemPorts with IMasterSlave {
  val writeData = Bits(mc.dw bit)
  val readData = Bits(mc.dw bit)
  val writeEnable = Bool

  override def asMaster(): Unit = {
    in(writeEnable, writeData)
    out(readData)
  }
}

case class BistPorts(mc: MemConfig) extends MemPorts with IMasterSlave {
  val bist_en = Bool
  val ap = AddrCtrlPorts(mc)
  val dp = DataPorts(mc)

  override def asMaster(): Unit = {
    in(ap, bist_en)
    master(dp)
  }
}

case class ScanPorts(mc: MemConfig) extends MemPorts with IMasterSlave {

  override def asMaster(): Unit = ???
}

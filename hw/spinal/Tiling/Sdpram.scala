package Tiling

import DataPump._

import spinal.core._
import spinal.lib.slave

case class Sdpram(addrWidth: Int, dataWidth: Int) extends Component {
  def MemoryReadPortType = MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth)

  def MemoryWritePortType = MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth)

  val io = new Bundle {
    val read = slave(MemoryReadPortType)
    val write = slave(MemoryWritePortType)
  }

  def noRead() = {
    io.read.Valid := False
    io.read.Address := 0
  }

  def noWrite() = {
    io.write.Valid := False
    io.write.Address := 0
    io.write.Data := 0
  }

  val mem = Mem(Bits(dataWidth bits), 1 << addrWidth)
  io.read.Data := mem.readSync(io.read.Address, io.read.Valid)
  mem.write(io.write.Address, io.write.Data, io.write.Valid)
}

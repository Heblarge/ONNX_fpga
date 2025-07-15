package MemBlackBoxer.Vendor

import MemBlackBoxer.MemManager._
import spinal.core._
import scala.language._

object UMC40 extends MemVendor {

  def memPrefix = "memUMC"
  def technology = "40"
  def process = "lp"
  def prefixName = memPrefix + technology + process

  override def build(mw:MemWrapper) = mw match {
    case mem: Ram1rw => new mbb1rw(mem).connectPort()
    case mem: Ram1r1w => new mbb1r1w(mem).connectPort()
    case mem: Ram2rw => new mbb2rw(mem).connectPort()
    case mem: Rom => new mbbrom(mem).connectPort()
  }

  class mbb1r1w(wrap: Ram1r1w) extends TwoPortBB(wrap.mc) {
    // NOTICE: this is FSH0L_B_SZ type of sram (Two-port register file)
    val io = new Bundle {
      val CKA, CKB = in Bool()
      val A, B = in UInt(wrap.mc.addrWidth bit) // Address
      val DI = in Bits(wrap.mc.dataWidth bit)
      val DO = out Bits(wrap.mc.dataWidth bit)
      val WEB, CSAN, CSBN, NAP = in Bool()
      val DVSE = in Bool()
      val DVS = in Bits(4 bits)
    }

    // Notice: UMC use Port A to read and Port B to write, which is different from our commonly use.
    val cda = ClockDomain(io.CKB)
    val cdb = ClockDomain(io.CKA)

    def connectPort(): MemBlackBox = {
      wrap.cda.setSynchronousWith(cda)
      wrap.cdb.setSynchronousWith(cdb)

      this.io.CKA <> wrap.cda.readClockWire
      this.io.CKB <> wrap.cdb.readClockWire
      this.io.A <> wrap.io.apa.address
      this.io.B <> wrap.io.apb.address
      this.io.DI <> wrap.io.dp.writeData
      this.io.DO <> wrap.io.dp.readData
      this.io.WEB <> !wrap.io.dp.writeEnable
      this.io.CSAN <> !wrap.io.apa.memoryEnable
      this.io.CSBN <> !wrap.io.apb.memoryEnable

      this.io.NAP <> False      // NAP -> False -> Normal mode
      this.io.DVSE <> False     // DVSE -> false -> Delay option set default
      this.io.DVS <> B"4'b0110" // DVSE is False -> DVS never mind

      this
    }

    noIoPrefix()
  }

  class mbb1rw(wrap: Ram1rw) extends SinglePortBB(wrap.mc) {
    // Notice: This is FSH0L_B_SH type of sram. (high-density single-port SRAM)
    val io = new Bundle {
      val CK = in Bool() 
      val A = in UInt(wrap.mc.addrWidth bit)
      val DI = in Bits(wrap.mc.dataWidth bit)
      val DO = out Bits(wrap.mc.dataWidth bit)
      val WEB, CSB, NAP = in Bool() 
      val DVSE = in Bool() 
      val DVS = in Bits(4 bit)
    }

    val cd = ClockDomain(io.CK)

    def connectPort(): MemBlackBox = {
      wrap.clockDomain.setSynchronousWith(cd)
      this.io.CK <> wrap.clockDomain.readClockWire
      this.io.A <> wrap.io.ap.address
      this.io.DI <> wrap.io.dp.writeData 
      this.io.DO <> wrap.io.dp.readData
      this.io.CSB <> !wrap.io.ap.memoryEnable 
      this.io.WEB <> !wrap.io.dp.writeEnable 

      this.io.NAP <> False 
      this.io.DVSE <> False
      this.io.DVS <> B"4'b0110"

      this
    }

    noIoPrefix()
  }

  class mbb2rw(wrap: Ram2rw) extends DualPortBB(wrap.mc) {
    // Notice: This is FSH0L_C_SJ type of sram. (High-density Dual-port SRAM)
    val io = new Bundle{
      val CKA, CKB = in Bool() 
      val A, B = in UInt(wrap.mc.addrWidth bit)
      val DIA, DIB = in Bits(wrap.mc.dataWidth bit)
      val DOA, DOB = out Bits(wrap.mc.dataWidth bit)
      val WEAN, WEBN, CSAN, CSBN, NAP = in Bool() 
      val DVSE = in Bool() 
      val DVS = in Bits(4 bit)
    }

    val cda = ClockDomain(io.CKA)
    val cdb = ClockDomain(io.CKB)

    def connectPort(): MemBlackBox = {
      wrap.cda.setSynchronousWith(cda)
      wrap.cdb.setSynchronousWith(cdb)

      this.io.CKA <> wrap.cda.readClockEnableWire
      this.io.CKB <> wrap.cdb.readClockEnableWire 
      this.io.A <> wrap.io.apa.address 
      this.io.B <> wrap.io.apb.address 
      this.io.DIA <> wrap.io.dpa.writeData 
      this.io.DOA <> wrap.io.dpa.readData 
      this.io.DIB <> wrap.io.dpb.writeData
      this.io.DOB <> wrap.io.dpb.readData 

      this.io.WEAN := !wrap.io.dpa.writeEnable 
      this.io.CSAN := !wrap.io.apa.memoryEnable 
      this.io.WEBN := !wrap.io.dpb.writeEnable
      this.io.CSAN := !wrap.io.apb.memoryEnable

      this.io.NAP  := False 
      this.io.DVSE := False 
      this.io.DVS  :=  B"4'b0110"

      this 
    }
    noIoPrefix()

  }

  class mbbrom(wrap: Rom) extends RomBB(wrap.mc){
    // Notice: This is FSH0L_B_SP type of rom. Via1 Programmable ROM.
    val io = new Bundle{
      val CK = in Bool() 
      val A = in UInt(wrap.mc.addrWidth bit)
      val DO = out Bits(wrap.mc.dataWidth bit)
      val CS = in Bool() 
      val DVSE = in Bool() 
      val DVS = in Bits(4 bit)
    }

    val cd = ClockDomain(io.CK)

    def connectPort(): MemBlackBox = {
      wrap.clockDomain.setSynchronousWith(cd)

      this.io.CK <> wrap.clockDomain.readClockEnableWire
      this.io.A <> wrap.io.addr 
      this.io.DO <> wrap.io.rdata 
      this.io.CS <> wrap.io.cs 
      this.io.DVSE := False 
      this.io.DVS  := B"4'b0000"

      this
    }

    noIoPrefix()
  }

}

package Tiling

import DataPump._

import spinal.core._
import spinal.lib.slave
import spinal.sim.VCSFlags
import spinal.core.sim._


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

//class Sdpram_BlackBox(addrWidth: Int, dataWidth: Int) extends BlackBox {
//  // 设置泛型/参数
//  addGeneric("ADDR_WIDTH", addrWidth)
//  addGeneric("DATA_WIDTH", dataWidth)
//
//  val io = new Bundle {
//    // 写端口 (Port A)
//    val clkW  = in Bool()
//    val we    = in Bits(dataWidth / 8 bits) // 字节使能作为写使能
//    val addrW = in UInt(addrWidth bits)
//    val dataW = in Bits(dataWidth bits)
//
//    // 读端口 (Port B)
//    val clkR  = in Bool()
//    val enR   = in Bool()
//    val addrR = in UInt(addrWidth bits)
//    val dataR = out Bits(dataWidth bits)
//  }
//
//  // 映射端口名称
//  noIoPrefix()
//
//  // 使用 setInlineVerilog 直接将实现嵌入 BlackBox
//  setInlineVerilog(
//    s"""
//       |module Sdpram_BlackBox #(
//       |    parameter ADDR_WIDTH = $addrWidth,
//       |    parameter DATA_WIDTH = $dataWidth
//       |)(
//       |    input  wire                     clkW,
//       |    input  wire [(DATA_WIDTH/8)-1:0] we,
//       |    input  wire [ADDR_WIDTH-1:0]    addrW,
//       |    input  wire [DATA_WIDTH-1:0]    dataW,
//       |    input  wire                     clkR,
//       |    input  wire                     enR,
//       |    input  wire [ADDR_WIDTH-1:0]    addrR,
//       |    output reg  [DATA_WIDTH-1:0]    dataR
//       |);
//       |    reg [DATA_WIDTH-1:0] mem [0:(1<<ADDR_WIDTH)-1];
//       |    always @(posedge clkW) begin
//       |        integer i;
//       |        for (i = 0; i < DATA_WIDTH/8; i = i + 1) begin
//       |            if (we[i]) mem[addrW][i*8 +: 8] <= dataW[i*8 +: 8];
//       |        end
//       |    end
//       |    always @(posedge clkR) begin
//       |        if (enR) dataR <= mem[addrR];
//       |    end
//       |endmodule
//       |""".stripMargin
//  )
//}
//
//case class SdpramXilinx(addrWidth: Int, dataWidth: Int) extends Component {
//  val io = new Bundle {
//    val read  = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
//    val write = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
//  }
//
//  // 辅助函数
//  def noRead() = {
//    io.read.Valid := False
//    io.read.Address := 0
//  }
//
//  def noWrite() = {
//    io.write.Valid := False
//    io.write.Address := 0
//    io.write.Data := 0
//    io.write.Wen := 0
//  }
//
//  // 实例化黑盒
//  val bb = new Sdpram_BlackBox(addrWidth, dataWidth)
//
//  // --- 连接写端口 ---
//  bb.io.clkW  := io.write.clk
//  // 只有在 Valid 为高时才传递 Wen，否则为 0
//  bb.io.we    := io.write.Valid ? io.write.Wen | B(0, dataWidth / 8 bits)
//  bb.io.addrW := io.write.Address
//  bb.io.dataW := io.write.Data
//
//  // --- 连接读端口 ---
//  bb.io.clkR  := io.read.clk
//  bb.io.enR   := io.read.Valid
//  bb.io.addrR := io.read.Address
//  io.read.Data := bb.io.dataR
//
//}

class Sdpram_ByteAddr_BlackBox(addrWidth: Int, dataWidth: Int) extends BlackBox {
  // addrWidth 是总字节地址位宽。
  // 对于 256bit (32 bytes) 数据，低 5 位是字节偏移。
  val byteOffset = log2Up(dataWidth / 8)
  val wordAddrWidth = addrWidth - byteOffset

  addGeneric("ADDR_WIDTH", addrWidth)
  addGeneric("DATA_WIDTH", dataWidth)
  addGeneric("WORD_ADDR_WIDTH", wordAddrWidth)

  val io = new Bundle {
    val clkW  = in Bool()
    val we    = in Bits(dataWidth / 8 bits)
    val addrW = in UInt(addrWidth bits)   // 字节地址
    val dataW = in Bits(dataWidth bits)

    val clkR  = in Bool()
    val enR   = in Bool()
    val addrR = in UInt(addrWidth bits)   // 字节地址
    val dataR = out Bits(dataWidth bits)
  }

  noIoPrefix()

  setInlineVerilog(
    s"""
       |module Sdpram_ByteAddr_BlackBox #(
       |    parameter ADDR_WIDTH = $addrWidth,
       |    parameter DATA_WIDTH = $dataWidth,
       |    parameter WORD_ADDR_WIDTH = $wordAddrWidth
       |)(
       |    input  wire                     clkW,
       |    input  wire [(DATA_WIDTH/8)-1:0] we,
       |    input  wire [ADDR_WIDTH-1:0]    addrW,
       |    input  wire [DATA_WIDTH-1:0]    dataW,
       |    input  wire                     clkR,
       |    input  wire                     enR,
       |    input  wire [ADDR_WIDTH-1:0]    addrR,
       |    output reg  [DATA_WIDTH-1:0]    dataR
       |);
       |    localparam OFFSET = $byteOffset;
       |    reg [DATA_WIDTH-1:0] mem [0:(1 << WORD_ADDR_WIDTH)-1];
       |
       |    always @(posedge clkW) begin
       |        integer i;
       |        for (i = 0; i < DATA_WIDTH/8; i = i + 1) begin
       |            if (we[i]) mem[addrW >> OFFSET][i*8 +: 8] <= dataW[i*8 +: 8];
       |        end
       |    end
       |
       |    always @(posedge clkR) begin
       |        if (enR) dataR <= mem[addrR >> OFFSET];
       |    end
       |endmodule
       |""".stripMargin
  )
}

case class SdpramXilinx(addrWidth: Int, dataWidth: Int) extends Component {
  val io = new Bundle {
    val read  = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val write = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
  }

  val bb = new Sdpram_ByteAddr_BlackBox(addrWidth, dataWidth)
  bb.io.clkW  := io.write.clk
  bb.io.we    := io.write.Valid ? io.write.Wen | B(0, dataWidth / 8 bits)
  bb.io.addrW := io.write.Address
  bb.io.dataW := io.write.Data

  bb.io.clkR  := io.read.clk
  bb.io.enR   := io.read.Valid
  bb.io.addrR := io.read.Address
  io.read.Data := bb.io.dataR
}

object SdpramXilinxSim extends App {
  val addrWidth = 10
  val dataWidth = 32

  val compiled = SimConfig
    .withFsdbWave
    .withConfig(SpinalConfig(bitVectorWidthMax = 100000))
    .withVCS(VCSFlags(
      compileFlags   = List("-kdb", "-lca", "+notimingchecks"),
      elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
      runFlags       = List("-l ./run.log")
    ))
    .compile {
      val dut = SdpramXilinx(addrWidth, dataWidth)
      dut
    }

  compiled.doSim { dut =>
    // 助手函数：使用仿真赋值 (#=)
    def simNoRead(): Unit = {
      dut.io.read.Valid #= false
      dut.io.read.Address #= 0
    }
    def simNoWrite(): Unit = {
      dut.io.write.Valid #= false
      dut.io.write.Address #= 0
      dut.io.write.Data #= 0
      dut.io.write.Wen #= 0
    }

    // 1. 初始化信号
    dut.io.write.clk #= false
    dut.io.read.clk  #= false
    simNoRead()
    simNoWrite()

    // 2. 创建 ClockDomain 对象并启动时钟
    // 使用 forkStimulus 启动时钟激励
    val writeCd = ClockDomain(dut.io.write.clk)
    val readCd  = ClockDomain(dut.io.read.clk)

    writeCd.forkStimulus(10) // 100MHz
    readCd.forkStimulus(14)  // ~71.4MHz

    // 等待仿真稳定
    sleep(100)

    // --- 场景 1: 同步写入测试 ---
    writeCd.waitSampling()
    dut.io.write.Valid   #= true
    dut.io.write.Address #= 0xAA
    dut.io.write.Data    #= 0x12345678L
    dut.io.write.Wen     #= 0xF

    writeCd.waitSampling() // 数据在此时钟沿被采样进入存储阵列
    simNoWrite()

    // --- 场景 2: 同步读取测试 (1-Cycle Latency) ---
    readCd.waitSampling()
    dut.io.read.Valid   #= true
    dut.io.read.Address #= 0xAA

    // 拍 1：RAM 在上升沿采样地址 0xAA
    readCd.waitSampling()
    simNoRead()

    // 拍 2：同步读取的数据出现在 Data 总线上
    readCd.waitSampling()

    val result = dut.io.read.Data.toLong
    println(s"Read Result at 0xAA (Simulated): 0x${result.toHexString}")
    assert(result == 0x12345678L, s"Data Mismatch! Expected 0x12345678, got 0x${result.toHexString}")

    sleep(100)
    simSuccess()
  }
}


object SdpramXilinx_verilog {
  import java.io.File

  def main(args: Array[String]): Unit = {
    new File("rtl/SdpramXilinx").mkdir() // 创建输出目录

    SpinalConfig(
      targetDirectory = "rtl/SdpramXilinx",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(SdpramXilinx(addrWidth = 15, dataWidth = 256))
      .printPruned()
      .printUnused()
  }
}
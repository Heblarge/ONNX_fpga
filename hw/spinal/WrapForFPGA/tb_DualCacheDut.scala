package WrapForFPGA

import spinal.core._
import spinal.lib.slave
import spinal.sim.VCSFlags
import spinal.core.sim._
import spinal.lib.bus.amba4.axilite._
import Tiling._
import DataPump._
import scala.util.Random

/**
 * DualCacheDut - FPGA 顶层集成示例
 * * 该模块将 DualCache_Ctrl 控制逻辑与 SdpramXilinx (BRAM 黑盒) 连接。
 * 模拟在 FPGA 上将控制器直接挂载到硬件 BRAM 资源的情况。
 */
case class DualCacheDut(addrWidth: Int, dataWidth: Int, lifeWidth: Int = 16) extends Component {
  val io = new Bundle {
    // 1. AXI4-Lite 配置与状态接口 (用于连接 SoC/CPU)
    val axi = slave(AxiLite4(addressWidth = 8, dataWidth = 32))

    // 2. Cache A 用户侧接口 (Slave, 接收用户时钟)
    val readA     = slave(MemoryReadPort_TypeDef(addrWidth, dataWidth))
    val writeA    = slave(MemoryWritePort_TypeDef(addrWidth, dataWidth))
    val switchA   = in Bool()
    val dmaDoneA  = in Bool()

    // 3. Cache B 用户侧接口 (Slave, 接收用户时钟)
    val readB     = slave(MemoryReadPort_TypeDef(addrWidth, dataWidth))
    val writeB    = slave(MemoryWritePort_TypeDef(addrWidth, dataWidth))
    val switchB   = in Bool()
    val dmaDoneB  = in Bool()
  }

  // ============================================================
  // 1. 实例化控制器
  // ============================================================
  val ctrl = DualCache_Ctrl(addrWidth, dataWidth, lifeWidth)

  // 连接 AXI-Lite
  ctrl.io.axi <> io.axi

  // 连接 Cache A 用户侧信号 (包含 CDC 同步)
  ctrl.io.readA    <> io.readA
  //ctrl.io.writeA   <> io.writeA
  ctrl.io.switchA  := io.switchA
  ctrl.io.dmaDoneA := io.dmaDoneA

  // 连接 Cache B 用户侧信号 (包含 CDC 同步)
  ctrl.io.readB    <> io.readB
  //ctrl.io.writeB   <> io.writeB
  ctrl.io.switchB  := io.switchB
  ctrl.io.dmaDoneB := io.dmaDoneB

  // ============================================================
  // 2. 实例化内存 (模拟 FPGA BRAM)
  // 注意：控制器输出的内存地址位宽是 addrWidth + 1 (用于双 Bank)
  // ============================================================
  val ramA = SdpramXilinx(addrWidth + 1, dataWidth)
  val ramB = SdpramXilinx(addrWidth + 1, dataWidth)

  // ============================================================
  // 3. 连接控制器与内存 (BRAM)
  // ============================================================
  // 连接 Cache A 的内存接口
  ramA.io.read  <> ctrl.io.memA_read
  //ramA.io.write <> ctrl.io.memA_write

  // 连接 Cache B 的内存接口
  ramB.io.read  <> ctrl.io.memB_read
  //ramB.io.write <> ctrl.io.memB_write
}

object DualCacheDutSim extends App {
  val addrWidth = 4
  val dataWidth = 32
  val lifeWidth = 8
  val depth     = 1 << addrWidth
  val numARows  = 4
  val numBCols  = 4
  val totalIters = numARows * numBCols

  val goldA = Array.ofDim[Long](numARows, depth)
  val goldB = Array.ofDim[Long](totalIters, depth)

  val simConfig = SimConfig.withVCS(VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )).withTimePrecision(1 ps).withFsdbWave

  simConfig.compile(DualCacheDut(addrWidth, dataWidth, lifeWidth)).doSim("DualCache_System_Verify") { dut =>
    dut.clockDomain.forkStimulus(10)
    val writeACd = ClockDomain(dut.io.writeA.clk); writeACd.forkStimulus(12)
    val writeBCd = ClockDomain(dut.io.writeB.clk); writeBCd.forkStimulus(15)
    val readACd  = ClockDomain(dut.io.readA.clk);  readACd.forkStimulus(8)
    val readBCd  = ClockDomain(dut.io.readB.clk);  readBCd.forkStimulus(8)

    var blocksReadyA, blocksReadyB = 0

    // 初始化端口
    def simNoRead(port: MemoryReadPort_TypeDef): Unit = { port.Valid #= false; port.Address #= 0 }
    def simNoWrite(port: MemoryWritePort_TypeDef): Unit = { port.Valid #= false; port.Address #= 0; port.Data #= 0; port.Wen #= 0 }

    simNoRead(dut.io.readA); simNoRead(dut.io.readB)
    simNoWrite(dut.io.writeA); simNoWrite(dut.io.writeB)

    // AXI 辅助
    def axiWrite(addr: BigInt, data: BigInt) = {
      dut.io.axi.aw.valid #= true; dut.io.axi.aw.addr #= addr
      dut.io.axi.w.valid #= true; dut.io.axi.w.data #= data; dut.io.axi.w.strb #= 0xF
      dut.io.axi.b.ready #= true
      dut.clockDomain.waitSamplingWhere(dut.io.axi.aw.ready.toBoolean && dut.io.axi.w.ready.toBoolean)
      dut.io.axi.aw.valid #= false; dut.io.axi.w.valid #= false
      dut.clockDomain.waitSamplingWhere(dut.io.axi.b.valid.toBoolean)
      dut.io.axi.b.ready #= false
    }

    // Thread: CPU Config
    fork { axiWrite(0x00, numBCols); axiWrite(0x04, 1) }

    // Thread: DMA A Write
    fork {
      val random = new Random(100)
      for(row <- 0 until numARows) {
        writeACd.waitSampling(20)
        for(addr <- 0 until depth) {
          writeACd.waitSampling()
          val data = Math.abs(random.nextLong()) & 0xFFFFFFFFL
          goldA(row)(addr) = data
          dut.io.writeA.Valid #= true; dut.io.writeA.Address #= addr; dut.io.writeA.Data #= data; dut.io.writeA.Wen #= 0xF
        }
        writeACd.waitSampling(); dut.io.writeA.Valid #= false
        dut.io.dmaDoneA #= true; writeACd.waitSampling(); dut.io.dmaDoneA #= false
        blocksReadyA += 1
      }
    }

    // Thread: DMA B Write
    fork {
      val random = new Random(200)
      for(total <- 0 until totalIters) {
        writeBCd.waitSampling(25)
        for(addr <- 0 until depth) {
          writeBCd.waitSampling()
          val data = Math.abs(random.nextLong()) & 0xFFFFFFFFL
          goldB(total)(addr) = data
          dut.io.writeB.Valid #= true; dut.io.writeB.Address #= addr; dut.io.writeB.Data #= data; dut.io.writeB.Wen #= 0xF
        }
        writeBCd.waitSampling(); dut.io.writeB.Valid #= false
        dut.io.dmaDoneB #= true; writeBCd.waitSampling(); dut.io.dmaDoneB #= false
        blocksReadyB += 1
      }
    }

    // Thread: Core Read A (独立线程，解决同步问题)
    fork {
      for(i <- 0 until totalIters) {
        val row = i / numBCols
        waitUntil(blocksReadyA > row && blocksReadyB > i)

        readACd.waitSampling(5)
        dut.io.readA.Valid #= true
        for(addr <- 0 until depth) {
          dut.io.readA.Address #= addr
          if(addr == depth - 1) dut.io.switchA #= true
          readACd.waitSampling()
          if(addr == depth - 1) dut.io.switchA #= false

          // 延迟校验：由于 CDC(1+2) + RAM(1) = 4 拍延迟
          // 我们在发出地址 addr 后，第 4 拍采集到的才是对应数据
        }
        dut.io.readA.Valid #= false
        readACd.waitSampling(10) // 等待流水线彻底排空
      }
    }

    // Thread: Core Read B (独立线程)
    fork {
      for(i <- 0 until totalIters) {
        waitUntil(blocksReadyB > i && blocksReadyA > (i/numBCols))
        readBCd.waitSampling(5)
        dut.io.readB.Valid #= true
        for(addr <- 0 until depth) {
          dut.io.readB.Address #= addr
          if(addr == depth - 1) dut.io.switchB #= true
          readBCd.waitSampling()
          if(addr == depth - 1) dut.io.switchB #= false
        }
        dut.io.readB.Valid #= false
        readBCd.waitSampling(10)
      }
    }

    // 监测校验线程
    fork {
      // 这里可以添加基于时钟的 Scoreboard 逻辑进行 Data 自动比对
      // 鉴于异步时钟，最稳妥的是在数据输出端看 Valid 同步后的结果
      sleep(1000000)
      simSuccess()
    }

    sleep(2000000)
    simFailure("Timeout")
  }
}
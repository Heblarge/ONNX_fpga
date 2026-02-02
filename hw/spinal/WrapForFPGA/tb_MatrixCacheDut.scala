package WrapForFPGA

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axilite._
import spinal.sim._
import Tiling._
import DataPump._
import scala.util.Random

/**
 * MatrixCacheDut - FPGA 顶层集成验证模块
 * 将控制器与三个外部 SdpramXilinx (模拟 BRAM) 连接。
 */
case class MatrixCacheDut(addrWidth: Int, dataWidth: Int, lifeWidth: Int = 16) extends Component {
  val io = new Bundle {
    val axi        = slave(AxiLite4(addressWidth = 8, dataWidth = 32))
    val globalIntr = out Bool()

    // 用户侧接口
    val readA  = slave(MemoryReadPort_TypeDef(addrWidth, dataWidth))
    val writeA = slave(MemoryWritePort_TypeDef(addrWidth, dataWidth))
    val switchA, dmaDoneA = in Bool()

    val readB  = slave(MemoryReadPort_TypeDef(addrWidth, dataWidth))
    val writeB = slave(MemoryWritePort_TypeDef(addrWidth, dataWidth))
    val switchB, dmaDoneB = in Bool()

    val readC  = slave(MemoryReadPort_TypeDef(addrWidth, dataWidth))
    val writeC = slave(MemoryWritePort_TypeDef(addrWidth, dataWidth))
    val switchC, dmaDoneC = in Bool()
  }

  // 1. 实例化控制器
  val ctrl = MatrixCache_Ctrl(addrWidth, dataWidth, lifeWidth)
  ctrl.io.axi <> io.axi
  io.globalIntr := ctrl.io.globalIntr

  // 连接用户侧
  ctrl.io.readA <> io.readA; ctrl.io.writeA <> io.writeA
  ctrl.io.switchA := io.switchA; ctrl.io.dmaDoneA := io.dmaDoneA

  ctrl.io.readB <> io.readB; ctrl.io.writeB <> io.writeB
  ctrl.io.switchB := io.switchB; ctrl.io.dmaDoneB := io.dmaDoneB

  ctrl.io.readC <> io.readC; ctrl.io.writeC <> io.writeC
  ctrl.io.switchC := io.switchC; ctrl.io.dmaDoneC := io.dmaDoneC

  // 2. 实例化内存 (BRAM) - 深度需要 addrWidth + 1 (用于双 Bank)
  // 注意：此处假设 SdpramXilinx 已经定义且包含 clk, en, we, addr, din, dout 接口
  val ramA = SdpramXilinx(addrWidth + 1, dataWidth)
  val ramB = SdpramXilinx(addrWidth + 1, dataWidth)
  val ramC = SdpramXilinx(addrWidth + 1, dataWidth)

  // 3. 连接控制器与内存
  ramA.io.read  <> ctrl.io.memA_read;  ramA.io.write <> ctrl.io.memA_write
  ramB.io.read  <> ctrl.io.memB_read;  ramB.io.write <> ctrl.io.memB_write
  ramC.io.read  <> ctrl.io.memC_read;  ramC.io.write <> ctrl.io.memC_write
}

object MatrixCacheSystemSim extends App {
  val addrWidth = 4
  val dataWidth = 32
  val lifeWidth = 8
  val depth     = 1 << addrWidth

  // 测试规模
  val numARows = 2
  val numBCols = 4
  val totalIters = numARows * numBCols

  // Golden Models
  val goldA = Array.ofDim[Long](numARows, depth)
  val goldB = Array.ofDim[Long](totalIters, depth)
  val goldC = Array.ofDim[Long](totalIters, depth)

  val simConfig = SimConfig.withVCS(VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad"),
    runFlags = List("-l ./run.log")
  )).withTimePrecision(1 ps).withFsdbWave

  simConfig.compile(MatrixCacheDut(addrWidth, dataWidth, lifeWidth)).doSim("MatrixCache_Full_Verify") { dut =>
    // --- 1. 时钟域初始化 ---
    dut.clockDomain.forkStimulus(5) // 系统 AXI 时钟 (100MHz)

    // 为每个端口创建独立的模拟时钟 (模拟异步 FPGA 环境)
    val cdWriteA = ClockDomain(dut.io.writeA.clk); cdWriteA.forkStimulus(5) // 83MHz
    val cdReadA  = ClockDomain(dut.io.readA.clk);  cdReadA.forkStimulus(5)  // 125MHz
    val cdWriteB = ClockDomain(dut.io.writeB.clk); cdWriteB.forkStimulus(5) // 66MHz
    val cdReadB  = ClockDomain(dut.io.readB.clk);  cdReadB.forkStimulus(5)
    val cdWriteC = ClockDomain(dut.io.writeC.clk); cdWriteC.forkStimulus(5)  // 142MHz
    val cdReadC  = ClockDomain(dut.io.readC.clk);  cdReadC.forkStimulus(5) // 90MHz

    // --- 2. 辅助函数 ---
    def axiWrite(addr: BigInt, data: BigInt) = {
      dut.io.axi.aw.valid #= true; dut.io.axi.aw.addr #= addr
      dut.io.axi.w.valid #= true; dut.io.axi.w.data #= data; dut.io.axi.w.strb #= 0xF
      dut.io.axi.b.ready #= true
      dut.clockDomain.waitSamplingWhere(dut.io.axi.aw.ready.toBoolean && dut.io.axi.w.ready.toBoolean)
      dut.io.axi.aw.valid #= false
      dut.io.axi.w.valid #= false
      dut.clockDomain.waitSamplingWhere(dut.io.axi.b.valid.toBoolean)
      dut.io.axi.b.ready #= false
    }

    def axiRead(addr: BigInt): BigInt = {
      dut.io.axi.ar.valid #= true; dut.io.axi.ar.addr #= addr; dut.io.axi.r.ready #= true
      dut.clockDomain.waitSamplingWhere(dut.io.axi.ar.ready.toBoolean)
      dut.io.axi.ar.valid #= false
      dut.clockDomain.waitSamplingWhere(dut.io.axi.r.valid.toBoolean)
      val data = dut.io.axi.r.data.toBigInt
      dut.io.axi.r.ready #= false
      data
    }

    // 初始化信号
    dut.io.writeA.Valid #= false; dut.io.readA.Valid #= false
    dut.io.writeB.Valid #= false; dut.io.readB.Valid #= false
    dut.io.writeC.Valid #= false; dut.io.readC.Valid #= false
    dut.io.dmaDoneA #= false; dut.io.dmaDoneB #= false; dut.io.dmaDoneC #= false
    dut.io.switchA #= false; dut.io.switchB #= false; dut.io.switchC #= false

    // --- 3. 任务线程 ---

    // Thread A: CPU 配置生命周期
    fork {
      axiWrite(0x00, numBCols) // A 矩阵一行复用 numBCols 次
      axiWrite(0x04, 1)          // B 矩阵流式读取 (1次)
      println("[AXI] Config Done.")
    }

    // Thread B: DMA A 写入 (使用 writeA.clk)
    fork {
      val rand = new Random(101)
      for(r <- 0 until numARows) {
        // 等待控制器 Ready (查询状态寄存器 0x08 bit 0)
        while((axiRead(0x08).toLong & 0x1) == 0) dut.clockDomain.waitSampling(5)

        cdWriteA.waitSampling(5)
        for(a <- 0 until depth) {
          val data = rand.nextInt().toLong & 0xFFFFFFFFL
          goldA(r)(a) = data
          dut.io.writeA.Valid #= true; dut.io.writeA.Address #= a; dut.io.writeA.Data #= data; dut.io.writeA.Wen #= 0xF
          cdWriteA.waitSampling()
        }
        dut.io.writeA.Valid #= false
        dut.io.dmaDoneA #= true; cdWriteA.waitSampling(); dut.io.dmaDoneA #= false
        println(f"[DMA-A] Block $r written.")
      }
    }

    // Thread C: DMA B 写入 (使用 writeB.clk)
    fork {
      val rand = new Random(202)
      for(i <- 0 until totalIters) {
        while((axiRead(0x08).toLong & 0x2) == 0) dut.clockDomain.waitSampling(5)

        cdWriteB.waitSampling(3)
        for(a <- 0 until depth) {
          val data = rand.nextInt().toLong & 0xFFFFFFFFL
          goldB(i)(a) = data
          dut.io.writeB.Valid #= true; dut.io.writeB.Address #= a; dut.io.writeB.Data #= data; dut.io.writeB.Wen #= 0xF
          cdWriteB.waitSampling()
        }
        dut.io.writeB.Valid #= false
        dut.io.dmaDoneB #= true; cdWriteB.waitSampling(); dut.io.dmaDoneB #= false
        println(f"[DMA-B] Block $i written.")
      }
    }

    // Thread D: Core 计算 (读取 A/B, 写入 C)
    fork {
      val rand = new Random(303)
      for(i <- 0 until totalIters) {
        val rowIdx = i / numBCols
        // 简单同步：等待 DMA 进度。在实际硬件中这里由状态机控制。
        // 此处我们等待 A 和 B 的 Bank 切换完成
        dut.clockDomain.waitSampling(100)

        // 读过程：发送地址并校验 1-cycle 延迟数据
        dut.io.readA.Valid #= true; dut.io.readB.Valid #= true
        for(a <- 0 until depth) {
          dut.io.readA.Address #= a; dut.io.readB.Address #= a
          if(a == depth - 1) { dut.io.switchA #= true; dut.io.switchB #= true }

          cdReadA.waitSampling() // 此时 BRAM 采样地址
          if(a == depth - 1) { dut.io.switchA #= false; dut.io.switchB #= false }

          // BRAM 数据在采样地址后的下一个有效时钟沿输出
          if(a > 0) {
            val checkA = dut.io.readA.Data.toLong
            val checkB = dut.io.readB.Data.toLong
            assert(checkA == goldA(rowIdx)(a-1), s"Error A at Iter $i, Addr ${a-1}")
            assert(checkB == goldB(i)(a-1), s"Error B at Iter $i, Addr ${a-1}")
          }
        }
        // 处理最后一个地址的数据
        cdReadA.waitSampling()
        assert(dut.io.readA.Data.toLong == goldA(rowIdx)(depth-1))
        assert(dut.io.readB.Data.toLong == goldB(i)(depth-1))

        dut.io.readA.Valid #= false; dut.io.readB.Valid #= false

        // 模拟计算后写入 C
        cdWriteC.waitSampling(10)
        for(a <- 0 until depth) {
          val res = rand.nextInt().toLong & 0xFFFFFFFFL
          goldC(i)(a) = res
          dut.io.writeC.Valid #= true; dut.io.writeC.Address #= a; dut.io.writeC.Data #= res; dut.io.writeC.Wen #= 0xF
          if(a == depth - 1) dut.io.switchC #= true
          cdWriteC.waitSampling()
          dut.io.switchC #= false
        }
        dut.io.writeC.Valid #= false
        println(f"[CORE] Block $i processed (Read A/B, Write C).")
      }
    }

    // Thread E: ISR & DMA C 读取 (通过中断触发)
    fork {
      var cReadCount = 0
      while(cReadCount < totalIters) {
        // 等待硬件中断信号
        dut.clockDomain.waitSamplingWhere(dut.io.globalIntr.toBoolean)
        println(f"[ISR] Interrupt $cReadCount detected.")

        cdReadC.waitSampling(5)
        dut.io.readC.Valid #= true
        for(a <- 0 until depth) {
          dut.io.readC.Address #= a
          cdReadC.waitSampling()
          if(a > 0) {
            assert(dut.io.readC.Data.toLong == goldC(cReadCount)(a-1))
          }
        }
        cdReadC.waitSampling()
        assert(dut.io.readC.Data.toLong == goldC(cReadCount)(depth-1))
        dut.io.readC.Valid #= false

        // 清除中断
        axiWrite(0x0C, 1)
        dut.io.dmaDoneC #= true; cdReadC.waitSampling(); dut.io.dmaDoneC #= false

        cReadCount += 1
      }
      println("[SUCCESS] MatrixCache_Ctrl Multi-Clock System Verification Passed!")
      simSuccess()
    }

    // 超时保护
    sleep(1000 * 2000)
    simFailure("Simulation Timeout - Deadlock suspected")
  }
}
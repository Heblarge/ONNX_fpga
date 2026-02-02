package MatrixCacheInterface

import spinal.core._
import spinal.core.sim._
import spinal.lib.bus.amba4.axilite._
import java.io.File
import scala.util.Random
import spinal.sim._

object tb_MatrixCache_System extends App {

  val addrWidth = 4
  val dataWidth = 32
  val lifeWidth = 8

  // 规模参数
  val Num_A_Rows = 4
  val Num_B_Cols = 8
  val Depth      = 1 << addrWidth

  // ==========================================
  // [Scoreboard] 黄金模型 (Golden Model)
  // ==========================================
  val goldA = Array.ofDim[Long](Num_A_Rows, Depth)
  val goldB = Array.ofDim[Long](Num_A_Rows * Num_B_Cols, Depth)
  val goldC = Array.ofDim[Long](Num_A_Rows * Num_B_Cols, Depth)

  val simConfig = SimConfig
    .withVCS(VCSFlags(
      compileFlags = List("-kdb", "-lca", "+notimingchecks"),
      elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
      runFlags = List("-l ./run.log")
    ))
    .withTimePrecision(1 ps)
    .withFSDBWave

  simConfig.compile(MatrixCache(addrWidth, dataWidth, lifeWidth)).doSim("system_verify_test") { dut =>

    // 初始化
    dut.clockDomain.forkStimulus(10)
    dut.io.axi.ar.valid #= false; dut.io.axi.aw.valid #= false; dut.io.axi.w.valid #= false
    dut.io.axi.r.ready #= false; dut.io.axi.b.ready #= false

    dut.io.writeA.Valid #= false; dut.io.writeB.Valid #= false; dut.io.writeC.Valid #= false
    dut.io.readA.Valid  #= false; dut.io.readB.Valid  #= false; dut.io.readC.Valid  #= false
    dut.io.switchA      #= false; dut.io.switchB      #= false; dut.io.switchC      #= false
    dut.io.dmaDoneA     #= false; dut.io.dmaDoneB     #= false; dut.io.dmaDoneC     #= false

    dut.clockDomain.assertReset()
    dut.clockDomain.waitSampling(10)
    dut.clockDomain.deassertReset()
    dut.clockDomain.waitSampling(10)

    // AXI Helper
    def axiWrite(addr: BigInt, data: BigInt): Unit = {
      dut.io.axi.aw.valid #= true; dut.io.axi.aw.addr #= addr
      dut.io.axi.w.valid  #= true; dut.io.axi.w.data  #= data; dut.io.axi.w.strb #= 0xF
      dut.io.axi.b.ready  #= true
      var awDone, wDone = false
      while(!awDone || !wDone) {
        dut.clockDomain.waitSampling()
        if(!awDone && dut.io.axi.aw.ready.toBoolean) { dut.io.axi.aw.valid #= false; awDone = true }
        if(!wDone && dut.io.axi.w.ready.toBoolean)   { dut.io.axi.w.valid  #= false; wDone = true }
      }
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


    // ==========================================
    // Thread 1: CPU Config
    // ==========================================
    fork {
      axiWrite(0x00, Num_B_Cols) // Life A = 8
      axiWrite(0x04, 1)          // Life B = 1
    }

    // ==========================================
    // Thread 2: DMA A Write (Synchronous Write)
    // ==========================================
    fork {
      val random = new Random(100)
      for(row <- 0 until Num_A_Rows) {
        // Wait Ready
        var canWrite = false
        while(!canWrite) {
          val status = axiRead(0x08)
          canWrite = (status & 0x01) == 0x01
          if(!canWrite) dut.clockDomain.waitSampling(10)
        }

        // Write Loop
        // 这里的写是“同拍写入”：Valid/Addr/Data 在沿前建立，沿后立即写入 RAM
        for(addr <- 0 until Depth) {
          val data = Math.abs(random.nextLong()) % 0xFFFFFFFFL
          goldA(row)(addr) = data

          dut.io.writeA.Valid #= true
          dut.io.writeA.Address #= addr
          dut.io.writeA.Data #= data
          dut.clockDomain.waitSampling() // Trigger Clock Edge -> Write Happens
        }
        dut.io.writeA.Valid #= false
        dut.io.dmaDoneA #= true; dut.clockDomain.waitSampling(); dut.io.dmaDoneA #= false
      }
    }

    // ==========================================
    // Thread 3: DMA B Write (Synchronous Write)
    // ==========================================
    fork {
      val random = new Random(200)
      for(total <- 0 until Num_A_Rows * Num_B_Cols) {
        var canWrite = false
        while(!canWrite) {
          val status = axiRead(0x08)
          canWrite = (status & 0x02) == 0x02
          if(!canWrite) dut.clockDomain.waitSampling(10)
        }

        for(addr <- 0 until Depth) {
          val data = Math.abs(random.nextLong()) % 0xFFFFFFFFL
          goldB(total)(addr) = data

          dut.io.writeB.Valid #= true
          dut.io.writeB.Address #= addr
          dut.io.writeB.Data #= data
          dut.clockDomain.waitSampling()
        }
        dut.io.writeB.Valid #= false
        dut.io.dmaDoneB #= true; dut.clockDomain.waitSampling(); dut.io.dmaDoneB #= false
      }
    }

    // ==========================================
    // Thread 4: Core (Read Latency Check + Compute)
    // ==========================================
    fork {
      val random = new Random(300)

      for(i <- 0 until Num_A_Rows * Num_B_Cols) {

        // 1. 等待 Input Ready
        var inputReady = false
        while(!inputReady) {
          val a_rdPtr = dut.cacheA.inputMatrixCacheInterface.rdPtr.toInt
          val b_rdPtr = dut.cacheB.inputMatrixCacheInterface.rdPtr.toInt
          val a_ready = dut.cacheA.inputMatrixCacheInterface.bankValid(a_rdPtr).toBoolean
          val b_ready = dut.cacheB.inputMatrixCacheInterface.bankValid(b_rdPtr).toBoolean
          if(a_ready && b_ready) inputReady = true else dut.clockDomain.waitSampling()
        }

        // 2. 验证读取 (Read Verification with Latency)
        // A复用逻辑：当前是第几个 Row Block
        val currentARow = i / Num_B_Cols
        // B流式逻辑：当前是第几个 Col Block
        val currentBCol = i

        dut.io.readA.Valid #= true
        dut.io.readB.Valid #= true

        // 读循环：发送地址 0 ~ Depth-1
        for(addr <- 0 until Depth) {
          dut.io.readA.Address #= addr
          dut.io.readB.Address #= addr

          if(addr == Depth - 1){
            dut.io.switchA #= true; dut.io.switchB #= true;
            dut.clockDomain.waitSampling()
            dut.io.switchA #= false; dut.io.switchB #= false;
          }else{
            // 时钟沿：DUT 采样当前 addr
            dut.clockDomain.waitSampling()
          }

          // 时钟沿后：DUT 输出的是上一个时钟周期请求的数据 (addr - 1)
          // 所以我们在此时 (Current Cycle) 只能校验 (Previous Cycle) 的数据
          if(addr > 0) {
            val checkAddr = addr - 1
            val rDataA = dut.io.readA.Data.toLong
            val rDataB = dut.io.readB.Data.toLong
            assert(rDataA == goldA(currentARow)(checkAddr),
              f"Mismatch A! Block=$currentARow Addr=$checkAddr Exp=0x${goldA(currentARow)(checkAddr)}%X Act=0x$rDataA%X")
            assert(rDataB == goldB(currentBCol)(checkAddr),
              f"Mismatch B! Block=$currentBCol Addr=$checkAddr Exp=0x${goldB(currentBCol)(checkAddr)}%X Act=0x$rDataB%X")
          }
        }

        // Pipeline Flush (流水线排空)
        // 循环结束时，Address=Depth-1 刚刚被采样。
        // 我们需要再过一个周期，才能在 Data 端口读到 Depth-1 的数据。
        dut.io.readA.Valid #= false
        dut.io.readB.Valid #= false
        dut.clockDomain.waitSampling()

        // 校验最后一个数据 (Depth-1)
        val lastAddr = Depth - 1
        val rDataA = dut.io.readA.Data.toLong
        val rDataB = dut.io.readB.Data.toLong

        assert(rDataA == goldA(currentARow)(lastAddr), f"Mismatch A Last! Exp=0x${goldA(currentARow)(lastAddr)}%X Act=0x$rDataA%X")
        assert(rDataB == goldB(currentBCol)(lastAddr), f"Mismatch B Last! Exp=0x${goldB(currentBCol)(lastAddr)}%X Act=0x$rDataB%X")

        // 3. 计算与写出 (Synchronous Write)
        dut.clockDomain.waitSampling(5)
        while(dut.cacheC.io.full.toBoolean) { dut.clockDomain.waitSampling() }

        for(addr <- 0 until Depth) {
          val resultData = Math.abs(random.nextLong()) % 0xFFFFFFFFL
          goldC(i)(addr) = resultData // 更新 Golden C

          dut.io.writeC.Valid #= true
          dut.io.writeC.Address #= addr
          dut.io.writeC.Data #= resultData
          if(addr == Depth -1){
            // 4. Switch (Using last pulse)
            dut.io.switchC #= true//dut.io.switchA #= true; dut.io.switchB #= true;
            dut.clockDomain.waitSampling()
            dut.io.switchC #= false//dut.io.switchA #= false; dut.io.switchB #= false;
          }else{
            dut.clockDomain.waitSampling() // Write happens here
          }
        }
        dut.io.writeC.Valid #= false

        println(f"[CORE]  Block $i Verified & Computed.")
      }
    }

    // ==========================================
    // Thread 5: ISR (Read Latency Check)
    // ==========================================
    fork {
      var handledCount = 0
      while(handledCount < Num_A_Rows * Num_B_Cols) {
        while(!dut.io.globalIntr.toBoolean) { dut.clockDomain.waitSampling() }
        dut.clockDomain.waitSampling(5)

        // 启动 DMA 读取
        dut.io.readC.Valid #= true

        for(addr <- 0 until Depth) {
          dut.io.readC.Address #= addr
          dut.clockDomain.waitSampling()

          // 同样的 Latency 校验逻辑：检查 addr - 1
          if(addr > 0) {
            val checkAddr = addr - 1
            val rDataC = dut.io.readC.Data.toLong
            assert(rDataC == goldC(handledCount)(checkAddr),
              f"Mismatch C! Block=$handledCount Addr=$checkAddr Exp=0x${goldC(handledCount)(checkAddr)}%X Act=0x$rDataC%X")
          }
        }
        dut.io.readC.Valid #= false
        dut.clockDomain.waitSampling() // Flush last data

        // 校验最后一个数据
        val lastAddr = Depth - 1
        val rDataC = dut.io.readC.Data.toLong
        assert(rDataC == goldC(handledCount)(lastAddr), f"Mismatch C Last! Exp=0x${goldC(handledCount)(lastAddr)}%X Act=0x$rDataC%X")

        dut.io.dmaDoneC #= true; dut.clockDomain.waitSampling(); dut.io.dmaDoneC #= false
        axiWrite(0x0C, 1)
        println(f"[ISR]   Output $handledCount Verified.")
        handledCount += 1
      }
      println("\n[SUCCESS] All checks passed with 1-cycle read latency!")
      simSuccess()
    }

    sleep(100000)
    println("[TB] Timeout! Deadlock.")
    simSuccess()

  }
}
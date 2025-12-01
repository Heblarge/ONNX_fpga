package MatrixCacheInterface

import MatrixCacheInterface._
import spinal.core._
import spinal.core.sim._
import spinal.sim._
import scala.collection.mutable
import java.io.File

object sim_OutputMatrixCache_test extends App {

  new File("rtl/OutputMatrixCache/sim_OutputMatrixCache_test_report").mkdirs()

  val flags = VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )

  val addrWidth = 4
  val dataWidth = 16

  val report = SpinalConfig(
    targetDirectory = "rtl/OutputMatrixCache/sim_OutputMatrixCache_test_report",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(OutputMatrixCache(addrWidth, dataWidth))
    .printPruned()

  val dutCompiled = SimConfig
    .withVCS(flags)
    .withTimePrecision(1 ps)
    .withFSDBWave
    .withConfig(SpinalConfig(bitVectorWidthMax = 20000))
    .compile(report)

  dutCompiled.doSim("output_matrix_cache_tb") { dut =>
    dut.clockDomain.forkStimulus(period = 10)
    dut.clockDomain.assertReset()
    dut.clockDomain.waitSampling(10)
    dut.clockDomain.deassertReset()
    dut.clockDomain.waitSampling(5)

    dut.io.intrClear #= false

    dut.io.switch #= false
    dut.io.dmaIntr #= false
    dut.io.write.Valid #= false
    dut.io.read.Valid #= false
    dut.io.write.Address #= 0
    dut.io.read.Address #= 0
    dut.io.write.Data #= 0

    val visibleDepth = 1 << addrWidth
    val random = new scala.util.Random
    var cycleCount = 0
    var frameCount = 0

    // =======================
    // 写端：持续写入 + 触发 switch
    // =======================
    fork {
      while (true) {
        if(frameCount < 10 && !dut.io.full.toBoolean){

          dut.clockDomain.waitSampling(10 + random.nextInt(10))

          for (addr <- 0 until visibleDepth) {
            dut.io.write.Valid #= true
            dut.io.write.Address #= addr
            dut.io.write.Data #= random.nextInt(1 << dataWidth)
            if (addr == visibleDepth - 1) {
              dut.io.switch #= true
              dut.clockDomain.waitSampling()
              dut.io.switch #= false
            } else {
              dut.clockDomain.waitSampling()
            }
          }
          dut.io.write.Valid #= false
          frameCount += 1
          println(s"[TB] >>> Frame $frameCount written")
          dut.clockDomain.waitSampling()
        }else{
          dut.clockDomain.waitSampling()
        }
      }
    }

    // =======================
    // 读端：仅在前 5 帧期间工作
    // =======================
    fork {
      var readFrame = 0
      while (readFrame < 5) {          // ← 改为只读前 5 帧
        while (dut.io.intr.toBoolean == false) {//frameCount <= readFrame &&
          dut.clockDomain.waitSampling()
        }
        println(s"[TB] >>> Start reading frame $readFrame")

        //if(dut.io.intr.toBoolean){
        // 软件清中断
        dut.clockDomain.waitSampling(random.nextInt(4)+1)
        dut.io.intrClear #= true
        dut.clockDomain.waitSampling()
        dut.io.intrClear #= false
        // 发起DMA读
        for (addr <- 0 until visibleDepth) {
          dut.io.read.Valid #= true
          dut.io.read.Address #= addr
          dut.clockDomain.waitSampling()
        }
        dut.io.read.Valid #= false

        // 模拟 DMA 完成中断
        dut.io.dmaIntr #= true
        dut.clockDomain.waitSampling()
        dut.io.dmaIntr #= false
        readFrame += 1
        //}

        dut.clockDomain.waitSampling(random.nextInt(10) + 5)
      }
      // 读完前 5 帧后彻底停下
      println("[TB] >>> Read phase finished (5 frames). Continue write only.")
    }



    //    fork {
//      while (frameCount < 10 && !dut.io.full.toBoolean) {
//        for (addr <- 0 until visibleDepth) {
//          dut.io.write.Valid #= true
//          dut.io.write.Address #= addr
//          dut.io.write.Data #= random.nextInt(1 << dataWidth)
//          if (addr == visibleDepth - 1) {
//            dut.io.switch #= true
//            dut.clockDomain.waitSampling()
//            dut.io.switch #= false
//          } else {
//            dut.clockDomain.waitSampling()
//          }
//        }
//        dut.io.write.Valid #= false
//        frameCount += 1
//        println(s"frame count: ${frameCount}")
//        dut.clockDomain.waitSampling(random.nextInt(5) + 3)
//      }
//    }
//
//    fork {
//      var readFrame = 0
//      while (readFrame < 4) {
//        // 等待写入至少一帧后开始读取
//        while (frameCount <= readFrame) {
//          dut.clockDomain.waitSampling()
//        }
//
//        //println(s"[TB] >>> Start reading frame $readFrame")
//        for (addr <- 0 until visibleDepth) {
//          dut.io.read.Valid #= true
//          dut.io.read.Address #= addr
//          dut.clockDomain.waitSampling()
//        }
//        dut.io.read.Valid #= false
//
//        // 模拟 DMA 完成中断
//        dut.io.dmaIntr #= true
//        dut.clockDomain.waitSampling()
//        dut.io.dmaIntr #= false
//
//        readFrame += 1
//        dut.clockDomain.waitSampling(random.nextInt(10) + 5)
//      }
//    }

    // =======================
    // 状态监控线程
    // =======================
//    fork {
//      while (cycleCount < 2000) {
//        dut.clockDomain.waitSampling()
//        cycleCount += 1
//        val full = dut.io.full.toBoolean
//        val status = dut.io.status.toBoolean
//        val intr = dut.io.intr.toBoolean
//        if (intr)
//          println(f"[TB @${cycleCount}%4d] >>> New bank ready (intr pulse)")
//        if (full)
//          println(f"[TB @${cycleCount}%4d] [Warning] both banks full (write paused)")
//        if (status)
//          println(f"[TB @${cycleCount}%4d] status=1 (bank available)")
//      }
//    }

    sleep(20000)
    simSuccess()
  }
}


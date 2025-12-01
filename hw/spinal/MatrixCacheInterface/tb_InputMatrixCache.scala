package MatrixCacheInterface

import MatrixCacheInterface._
import spinal.core._
import spinal.core.sim._
import spinal.sim._
import java.io.File
import scala.util.Random

/** ****************************************************************************
 * sim_InputMatrixCache_test
 * - 验证包含生命周期的 InputMatrixCache 行为
 * - 前5帧：写入并读取，每帧 lifeCfg = 3
 * - 后5帧：仅写入，每帧 lifeCfg = 1
 * **************************************************************************** */
object sim_InputMatrixCache_test extends App {

  // 仿真输出目录
  new File("rtl/InputMatrixCache/sim_InputMatrixCache_test_report").mkdirs()

  val flags = VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )

  val addrWidth = 4
  val dataWidth = 16
  val lifeWidth = 8

  // 生成 Verilog
  val report = SpinalConfig(
    targetDirectory = "rtl/InputMatrixCache/sim_InputMatrixCache_test_report",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(InputMatrixCache(addrWidth, dataWidth, lifeWidth))
    .printPruned()

  // 编译
  val dutCompiled = SimConfig
    .withVCS(flags)
    .withTimePrecision(1 ps)
    .withFSDBWave
    .withConfig(SpinalConfig(bitVectorWidthMax = 20000))
    .compile(report)

  // ======================
  // 仿真主体
  // ======================
  dutCompiled.doSim("input_matrix_cache_life_tb") { dut =>

    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.assertReset()
    dut.clockDomain.waitSampling(10)
    dut.clockDomain.deassertReset()
    dut.clockDomain.waitSampling(5)

    // 初始化
    dut.io.switch #= false
    dut.io.dmaIntr #= false
    dut.io.lifeCfg #= 0
    dut.io.write.Valid #= false
    dut.io.read.Valid #= false
    dut.io.write.Address #= 0
    dut.io.read.Address #= 0
    dut.io.write.Data #= 0

    val visibleDepth = 1 << addrWidth
    val random = new Random
    var frameCount = 0

    // =====================================
    // 写端线程：DMA 写入 + 生命周期配置
    // =====================================
    fork {
      while (true) {

        // 有可用 bank 才能写入
        if (dut.io.status.toBoolean && frameCount < 10) {

          // 设置下一帧的生命周期
          val lifeValue =
            if (frameCount < 5) random.nextInt(5)  // 前5帧可读 3 次
            else                1  // 后5帧可读 1 次

          dut.io.lifeCfg #= lifeValue
          if(random.nextBoolean()){
            dut.clockDomain.waitSampling(random.nextInt(5))
          }

          println(s"[TB] >>> Frame $frameCount lifeCfg = $lifeValue")

          // 写满一帧
          for (addr <- 0 until visibleDepth) {
            dut.io.write.Valid #= true
            dut.io.write.Address #= addr
            dut.io.write.Data #= random.nextInt(1 << dataWidth)

            // 最后一个地址写完触发 dmaIntr
            if (addr == visibleDepth - 1) {
              dut.clockDomain.waitSampling()
              dut.io.dmaIntr #= true
              dut.clockDomain.waitSampling(random.nextInt(5)+15)//模拟处理器在若干周期后清除中断
              dut.io.dmaIntr #= false
            } else {
              dut.clockDomain.waitSampling()
            }
          }

          dut.io.write.Valid #= false
          println(s"[TB] >>> DMA write frame $frameCount done")
          frameCount += 1
          dut.clockDomain.waitSampling()

        } else {
          dut.clockDomain.waitSampling()
        }
      }
    }

    // =====================================
    // 读端线程：按 lifeCfg 次数读取
    // =====================================
    fork {
      var readFrame = 0

      while (readFrame < 5) {  // 前5帧读取
        // 等待至少有一个可读 bank
        if (!dut.io.empty.toBoolean) {

          // 每帧可读 lifeCfg 次（这里用3次）
          println(s"[TB] >>> Start reading frame $readFrame")

          for (readTimes <- 0 until 3) {

            // 等随机时间开始读
            dut.clockDomain.waitSampling(random.nextInt(8) + 3)

            for (addr <- 0 until visibleDepth) {
              dut.io.read.Valid #= true
              dut.io.read.Address #= addr
              dut.clockDomain.waitSampling()
            }
            dut.io.read.Valid #= false

            // 读完一次触发 switch
            dut.io.switch #= true
            dut.clockDomain.waitSampling()
            dut.io.switch #= false

            println(s"[TB] >>> frame $readFrame consumed once (life--)")
          }

          readFrame += 1
        }

        dut.clockDomain.waitSampling()
      }

      println("[TB] >>> Finish reading first 5 frames.")
    }

    // =====================================
    // 监控线程（可打印生命周期、bankValid 等）
    // =====================================
//    fork {
//      for (i <- 0 until 5000) {
//        dut.clockDomain.waitSampling()
//
//        if (i % 20 == 0) {
//          val status = dut.io.status.toBoolean
//          val empty  = dut.io.empty.toBoolean
//          val wrPtr  = dut.inputMatrixCacheInterface.wrPtr.toInt
//          val rdPtr  = dut.inputMatrixCacheInterface.rdPtr.toInt
//          val valid0 = dut.inputMatrixCacheInterface.bankValid(0).toBoolean
//          val valid1 = dut.inputMatrixCacheInterface.bankValid(1).toBoolean
//          val life0  = dut.inputMatrixCacheInterface.lifeCnt(0).toInt
//          val life1  = dut.inputMatrixCacheInterface.lifeCnt(1).toInt
//
//          println(f"[MON] wrPtr=$wrPtr rdPtr=$rdPtr  " +
//            f"V0=$valid0 L0=$life0   V1=$valid1 L1=$life1   status=$status empty=$empty")
//        }
//      }
//    }

    // 结束仿真
    sleep(30000)
    simSuccess()
  }
}


//package MatrixCacheInterface
//
//import MatrixCacheInterface._
//import spinal.core._
//import spinal.core.sim._
//import spinal.sim._
//import java.io.File
//import scala.util.Random
//
///** ****************************************************************************
// * object sim_InputMatrixCache_test
// * VCS 仿真模板，用于验证 InputMatrixCache 的 ping-pong 双缓存行为
// * - 模拟 DMA 写入并触发 dmaIntr
// * - 模拟读主机读取并触发 switch
// * - 前5帧读写交替，后5帧仅写入
// * **************************************************************************** */
//object sim_InputMatrixCache_test extends App {
//
//  // 仿真输出路径
//  new File("rtl/InputMatrixCache/sim_InputMatrixCache_test_report").mkdirs()
//
//  // VCS 仿真编译参数
//  val flags = VCSFlags(
//    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
//    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
//    runFlags = List("-l ./run.log")
//  )
//
//  // 参数配置
//  val addrWidth = 4
//  val dataWidth = 16
//
//  // 生成 Verilog
//  val report = SpinalConfig(
//    targetDirectory = "rtl/InputMatrixCache/sim_InputMatrixCache_test_report",
//    oneFilePerComponent = true,
//    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
//  ).generateVerilog(InputMatrixCache(addrWidth, dataWidth))
//    .printPruned()
//
//  // 编译模块
//  val dutCompiled = SimConfig
//    .withVCS(flags)
//    .withTimePrecision(1 ps)
//    .withFSDBWave
//    .withConfig(SpinalConfig(bitVectorWidthMax = 20000))
//    .compile(report)
//
//  // ======================
//  // 仿真主体
//  // ======================
//  dutCompiled.doSim("input_matrix_cache_tb") { dut =>
//    dut.clockDomain.forkStimulus(period = 10)
//    dut.clockDomain.assertReset()
//    dut.clockDomain.waitSampling(10)
//    dut.clockDomain.deassertReset()
//    dut.clockDomain.waitSampling(5)
//
//    // 初始化信号
//    dut.io.switch #= false
//    dut.io.dmaIntr #= false
//    dut.io.write.Valid #= false
//    dut.io.read.Valid #= false
//    dut.io.write.Address #= 0
//    dut.io.read.Address #= 0
//    dut.io.write.Data #= 0
//
//    val visibleDepth = 1 << addrWidth
//    val random = new Random
//    var frameCount = 0
//    var cycleCount = 0
//
//    // =======================
//    // 写端：模拟 DMA 写入行为
//    // =======================
//    fork {
//      while (true) {
//        if(frameCount < 10 && dut.io.status.toBoolean){
//          // 模拟写满一个 bank
//          for (addr <- 0 until visibleDepth) {
//            dut.io.write.Valid #= true
//            dut.io.write.Address #= addr
//            dut.io.write.Data #= random.nextInt(1 << dataWidth)
//
//            if(addr == visibleDepth-1){
//              dut.io.dmaIntr #= true
//              dut.clockDomain.waitSampling()
//              dut.io.dmaIntr #= false
//            }else{
//              dut.clockDomain.waitSampling()
//            }
//
//          }
//          dut.io.write.Valid #= false
//
//          // DMA 写完成，通知模块
//
//          frameCount += 1
//          println(s"[TB] >>> DMA write frame $frameCount done")
//          dut.clockDomain.waitSampling()
//        }else{
//          dut.clockDomain.waitSampling()
//        }
//      }
//    }
//
//    // =======================
//    // 读端：模拟读主机行为
//    // =======================
//    fork {
//      var readFrame = 0
//      while (true) { // 仅前5帧执行读取
//        if(readFrame < 5 && !dut.io.empty.toBoolean){
//
//          dut.clockDomain.waitSampling(random.nextInt(10) + 5)
//
//          println(s"[TB] >>> Start reading frame $readFrame")
//          for (addr <- 0 until visibleDepth) {
//            dut.io.read.Valid #= true
//            dut.io.read.Address #= addr
//            dut.clockDomain.waitSampling()
//          }
//          dut.io.read.Valid #= false
//
//          // 读完一帧发送 switch 释放 bank
//          dut.io.switch #= true
//          dut.clockDomain.waitSampling()
//          dut.io.switch #= false
//          readFrame += 1
//        }else{
//          dut.clockDomain.waitSampling()
//        }
//
//      }
//      println("[TB] >>> Read phase finished (5 frames). Continue write only.")
//    }
//
//    // =======================
//    // 状态监控
//    // =======================
////    fork {
////      while (cycleCount < 2000) {
////        dut.clockDomain.waitSampling()
////        cycleCount += 1
////        val empty = dut.io.empty.toBoolean
////        val status = dut.io.status.toBoolean
////        if (empty)
////          println(f"[TB @${cycleCount}%4d] [Warning] all banks empty (no readable data)")
////        if (status)
////          println(f"[TB @${cycleCount}%4d] status=1 (bank available for write)")
////      }
////    }
//
//    // 仿真结束
//    sleep(20000)
//    simSuccess()
//  }
//}

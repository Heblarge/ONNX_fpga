package playGround

import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.sim._
import spinal.lib.bus.amba4.axi._
import scala.util.Random
// Refer to InstAxi4ToStream128 and InstAxi4ToStream128_Config directly (same package),
// the explicit import was causing a "not a member of package playGround" error.

// Make sure InstAxi4ToStream128_Config is defined or available in this package

object InstAxi4ToStream128_tb extends App {
  new File("rtl/InstAxi4ToStream128/tb_report_128bits").mkdir()
  
  val instCfg128 = InstAxi4ToStream128_Config()

  // =======================================
  // 1. 生成 RTL
  // =======================================
  val rtl = SpinalConfig(
    targetDirectory = "rtl/InstAxi4ToStream128",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(
      resetActiveLevel = LOW
    )
  ).generateVerilog(new InstAxi4ToStream128(instCfg128))
    .printPruned()

  // =======================================
  // 2. VCS 仿真配置
  // =======================================
  val flags = VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )

  val simCompiled = SimConfig
    .withVCS(flags)
    .withFsdbWave
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .allOptimisation
    .withConfig(
      SpinalConfig(
        targetDirectory = "rtl/InstAxi4ToStream128",
        anonymSignalPrefix = "temp",
        oneFilePerComponent = false,
        defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
        nameWhenByFile = false,
        genLineComments = true
      )
    )
    .compile(new InstAxi4ToStream128(instCfg128))

  // =======================================
// 3. 仿真行为
  simCompiled.doSim("axi4tostream_tb") { dut =>
    // 初始化时钟
    dut.clockDomain.forkStimulus(10)

    // 初始化所有信号
    dut.io.axi.aw.valid #= false
    dut.io.axi.aw.addr  #= 0
    dut.io.axi.aw.len   #= 0
    dut.io.axi.aw.size  #= 4  // 128 bits = 16 bytes = 2^4
    dut.io.axi.aw.burst #= 1  // INCR
    
    dut.io.axi.w.valid  #= false
    dut.io.axi.w.data   #= 0
    dut.io.axi.w.last   #= false
    
    dut.io.axi.b.ready  #= true
    
    dut.io.axi.ar.valid #= false
    dut.io.axi.r.ready  #= false
    
    dut.io.out.ready    #= true

    // 复位
    dut.clockDomain.assertReset()
    dut.clockDomain.waitSampling(5)
    dut.clockDomain.deassertReset()
    dut.clockDomain.waitSampling(2)

    println("=== Starting AXI4 to Stream Test ===")

    // AXI 写事务函数（带超时保护）
    def axiWrite(addr: Int, data: BigInt, timeout: Int = 100): Unit = {
      var cycleCount = 0
      var awDone = false
      var wDone = false
      var bDone = false

      println(f"Writing 0x$data%032x to address 0x$addr%08x")

      // 同时发起 AW 和 W
      dut.io.axi.aw.valid #= true
      dut.io.axi.aw.addr  #= addr
      dut.io.axi.aw.len   #= 0
      dut.io.axi.aw.size  #= 4
      dut.io.axi.aw.burst #= 1
      
      dut.io.axi.w.valid  #= true
      dut.io.axi.w.data   #= data
      dut.io.axi.w.last   #= true

      // 等待 AW 和 W 握手完成
      while((!awDone || !wDone) && cycleCount < timeout) {
        dut.clockDomain.waitSampling()
        
        if(dut.io.axi.aw.ready.toBoolean && dut.io.axi.aw.valid.toBoolean) {
          awDone = true
          dut.io.axi.aw.valid #= false
          println(s"  AW handshake done at cycle $cycleCount")
        }
        
        if(dut.io.axi.w.ready.toBoolean && dut.io.axi.w.valid.toBoolean) {
          wDone = true
          dut.io.axi.w.valid #= false
          println(s"  W handshake done at cycle $cycleCount")
        }
        
        cycleCount += 1
      }

      if(cycleCount >= timeout) {
        println(s"ERROR: AW/W timeout after $timeout cycles!")
        println(s"  awDone=$awDone, wDone=$wDone")
        simFailure("AXI write address/data phase timeout")
      }

      // 等待写响应
      cycleCount = 0
      while(!bDone && cycleCount < timeout) {
        dut.clockDomain.waitSampling()
        
        if(dut.io.axi.b.valid.toBoolean && dut.io.axi.b.ready.toBoolean) {
          bDone = true
          val resp = dut.io.axi.b.resp.toInt
          println(s"  B response received: resp=$resp")
          if(resp != 0) {
            println(s"WARNING: Non-OKAY response: $resp")
          }
        }
        
        cycleCount += 1
      }

      if(cycleCount >= timeout) {
        println(s"ERROR: B response timeout after $timeout cycles!")
        simFailure("AXI write response timeout")
      }

      println("Write transaction completed\n")
    }

    // 读取输出流数据（带超时）
    def readOutputStream(expectedData: BigInt, timeout: Int = 100): Unit = {
      var cycleCount = 0
      var gotData = false

      println(f"Waiting for output stream data (expecting 0x$expectedData%032x)")

      while(!gotData && cycleCount < timeout) {
        dut.clockDomain.waitSampling()
        
        if(dut.io.out.valid.toBoolean && dut.io.out.ready.toBoolean) {
          val outData = dut.io.out.payload.toBigInt
          println(f"  Output stream data: 0x$outData%032x")
          
          if(outData == expectedData) {
            println("  ✓ Data matched!")
          } else {
            println(f"  ✗ Data mismatch! Expected 0x$expectedData%032x")
          }
          
          gotData = true
        }
        
        cycleCount += 1
      }

      if(cycleCount >= timeout) {
        println(s"ERROR: Output stream timeout after $timeout cycles!")
        simFailure("Output stream timeout")
      }
      
      println()
    }

    // 测试用例 1
    val testData1 = BigInt("0123456789abcdef0123456789abcdef", 16)
    axiWrite(0x000, testData1)
    readOutputStream(testData1)

    // 测试用例 2
    val testData2 = BigInt("FFFFFFFFAAAAAAAA0000000012345678", 16)
    axiWrite(0x000, testData2)
    readOutputStream(testData2)

    // 测试用例 3: 随机数据
    val testData3 = BigInt(128, Random)
    axiWrite(0x000, testData3)
    readOutputStream(testData3)

    // 额外等待
    dut.clockDomain.waitSampling(20)
    
    println("=== All tests passed! ===")
    println("Simulation completed successfully!")
  }
}
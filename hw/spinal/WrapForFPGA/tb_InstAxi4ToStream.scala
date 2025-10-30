// SPDX-License-Identifier: MIT
package WrapForFPGA

import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.sim._
import spinal.lib.bus.amba4.axi._
import scala.util.Random

object tb_InstAxi4ToStream extends App {

  // 创建仿真输出目录
  new File("rtl/InstAxi4ToStream/tb_report").mkdirs()

  // =======================================
  // 1. 构造配置对象
  // =======================================
  val instCfg = InstAxi4ToStream_Config(
    payloadType = HardType(Bits(193 bits)),
    axiCfg = Axi4Config(
      addressWidth = 12,
      dataWidth = 256,
      idWidth = 0,
      useId = false,
      useBurst = true,
      useLock = false,
      useRegion = false,
      useQos = false
    ),
    fifoDepth = 4
  )

  // =======================================
  // 2. 生成 Verilog（仅一次）
  // =======================================
  val report = SpinalConfig(
    targetDirectory = "rtl/InstAxi4ToStream/tb_report",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(new InstAxi4ToStream(instCfg))
    .printPruned()

  // =======================================
  // 3. VCS 仿真配置
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
        targetDirectory = "rtl/InstAxi4ToStream/tb_report",
        anonymSignalPrefix = "temp",
        oneFilePerComponent = false,
        defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
        nameWhenByFile = false,
        genLineComments = true
      )
    ).compile(report)

  // =======================================
  // 4. 运行仿真
  // =======================================
  simCompiled.doSim("tb") { dut =>
    // 建立时钟与复位
    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.assertReset()
    dut.clockDomain.waitSampling(5)
    dut.clockDomain.deassertReset()
    dut.clockDomain.waitSampling(10)

    val axi = dut.io.axi
    val out = dut.io.out

    // 初始化
    axi.aw.valid #= false
    axi.w.valid  #= false
    axi.b.ready  #= true
    axi.ar.valid #= false
    axi.r.ready  #= true

    // ---------------------------------------
    // AXI4 单拍写：先握AW，再握W，补齐必要字段，WLAST=1
    // ---------------------------------------
    def axiWrite(addr: BigInt, data: BigInt): Unit = {
      val beatBytes = instCfg.axiCfg.dataWidth / 8
      val sizeVal   = log2Up(beatBytes)
      val fullStrb  = (BigInt(1) << beatBytes) - 1

      // 预载
      axi.aw.payload.addr   #= addr
      axi.aw.payload.len    #= 0
      axi.aw.payload.size   #= sizeVal
      axi.aw.payload.burst  #= 1
      axi.aw.payload.cache  #= 0
      axi.aw.payload.prot   #= 0
      axi.w.payload.data    #= data
      axi.w.payload.strb    #= fullStrb
      axi.w.payload.last    #= true

      // 1) 拉起 valid
      axi.aw.valid #= true
      axi.w.valid  #= true

      // 2) 至少等一个采样沿（保证这一拍 valid 真正出现在时序上）
      dut.clockDomain.waitSampling()

      // 3) 等待直到 ready 同时为 1（这期间 valid 始终保持为 1）
      while(!(axi.aw.ready.toBoolean && axi.w.ready.toBoolean)){
        dut.clockDomain.waitSampling()
      }

      // 4) 握手完成后的下一拍再拉低 valid
      dut.clockDomain.waitSampling()
      axi.aw.valid #= false
      axi.w.valid  #= false

      // 5) 等 B
      while(!axi.b.valid.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
    }
    // -------------------------------------------------------------
    // 异步写例 1：AW 先到，W 后到
    // -------------------------------------------------------------
    def axiWrite_AWFirst(addr: BigInt, data: BigInt): Unit = {
      val beatBytes = instCfg.axiCfg.dataWidth / 8
      val sizeVal   = log2Up(beatBytes)
      val fullStrb  = (BigInt(1) << 8) - 1

      // 预载
      axi.aw.payload.addr   #= addr
      axi.aw.payload.len    #= 0
      axi.aw.payload.size   #= sizeVal
      axi.aw.payload.burst  #= 1
      axi.aw.payload.cache  #= 0
      axi.aw.payload.prot   #= 0

      axi.w.payload.data    #= data
      axi.w.payload.strb    #= fullStrb
      axi.w.payload.last    #= true

      // ---- AW 先到：拉高并等待自身 fire，然后马上降 ----
      axi.aw.valid #= true
      dut.clockDomain.waitSampling()                      // 保证出现一拍
      while(!axi.aw.ready.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()                      // fire 的下一拍
      axi.aw.valid #= false                               // 立刻降，避免二次握手

      // ---- W 后到：同理 ----
      axi.w.valid  #= true
      dut.clockDomain.waitSampling()
      while(!axi.w.ready.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
      axi.w.valid  #= false                               // 立刻降

      // ---- 等 B ----
      while(!axi.b.valid.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
    }


    // -------------------------------------------------------------
    // 异步写例 2：W 先到，AW 后到
    // -------------------------------------------------------------
    def axiWrite_WFirst(addr: BigInt, data: BigInt): Unit = {
      val beatBytes = instCfg.axiCfg.dataWidth / 8
      val sizeVal   = log2Up(beatBytes)
      val fullStrb  = (BigInt(1) << beatBytes) - 1

      // 预载
      axi.aw.payload.addr   #= addr
      axi.aw.payload.len    #= 0
      axi.aw.payload.size   #= sizeVal
      axi.aw.payload.burst  #= 1
      axi.aw.payload.cache  #= 0
      axi.aw.payload.prot   #= 0
      axi.w.payload.data    #= data
      axi.w.payload.strb    #= fullStrb
      axi.w.payload.last    #= true

      println(s"[${simTime()} ns] === 异步写例 2：W first ===")

      // 1) 数据先拉起
      axi.w.valid  #= true
      dut.clockDomain.waitSampling(2) // 提前几拍
      axi.aw.valid #= true

      // 2) 保持直到各自 ready
      while(!axi.aw.ready.toBoolean) dut.clockDomain.waitSampling()
      while(!axi.w.ready.toBoolean) dut.clockDomain.waitSampling()

      dut.clockDomain.waitSampling()
      axi.aw.valid #= false
      axi.w.valid  #= false

      // 3) 等 B 响应
      while(!axi.b.valid.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
      println(s"[${simTime()} ns] W-first 写完成")
    }

    fork {
      while (true) {
        out.ready #= Random.nextBoolean()
        dut.clockDomain.waitSampling()
      }
    }

    println("===== AXI 写入指令 =====")
    val instData1 = BigInt("111111111111111111111111111111111111111111111", 16)
    val instData2 = BigInt("222222222222222222222222222222222222222222222", 16)
    axiWrite(0x000, instData1)   // 写 DATA 寄存器
    //axiWrite(0x100, 0x1)        // 写 CTRL(commit)

    println("===== 异步写例 1：AW 先到 =====")
    axiWrite_AWFirst(0x000, instData2)
    //axiWrite_AWFirst(0x100, 0x1)


    // 等待 push.fire
    for(_ <- 0 until 100){
      if(out.valid.toBoolean && out.ready.toBoolean){
        println(f"[${simTime()} ns] Out fire! payload = 0x${out.payload.toBigInt.toString(16)}")
      }
      dut.clockDomain.waitSampling()
    }

    // 发出 clear
    //axiWrite(0x100, 0x2)
    println("Simulation finished.")
    dut.clockDomain.waitSampling(10)

    sleep(200)
    simSuccess()
  }
}

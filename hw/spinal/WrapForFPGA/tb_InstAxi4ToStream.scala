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

object tb_InstAxi4ToStreamWrapper extends App {

  // 创建仿真输出目录
  new File("rtl/InstAxi4ToStreamWrapper/tb_report").mkdirs()

  // =======================================
  // 1. 构造配置
  // =======================================
  val simCfg = FPGACfg(
    UIDWidth = 19,
    AddressWidth = 20,
    ShapeWidth = 16,
    systolicArraySideNum = 32,
    elementWidth = 24,
    intWidth = 12,
    systolicArrayInFifoDepth = 2,
    systolicArrayOutFifoDepth = 2,
    systolicArrayInstFifoDepth = 16,
    activationOutFifoDepth = 32,
    slicedInstFifoDepth = 16,
    numCores = 1
  )

  // =======================================
  // 2. 编译仿真模型
  // =======================================
  val simCompiled = SimConfig
    .withVCS(VCSFlags(
      compileFlags = List("-kdb", "-lca", "+notimingchecks"),
      elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
      runFlags = List("-l ./run.log")
    ))
    .withFsdbWave
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .compile(new InstAxi4ToStreamWrapper(simCfg))

  // =======================================
  // 3. 运行仿真
  // =======================================
  simCompiled.doSim("tb") { dut =>
    // 初始化时钟和复位
    dut.clockDomain.forkStimulus(10)

    val axi = dut.io.sAxi4FullInst

    // 初始化 AXI 信号
    axi.aw.valid #= false
    axi.w.valid  #= false
    axi.b.ready  #= true
    axi.ar.valid #= false
    axi.r.ready  #= true

    dut.clockDomain.waitSampling(10)

    // =============================================================
    // 辅助函数：AXI Lite/Full 32-bit 写操作 (带空指针检查修复)
    // =============================================================
    def axiWrite(addr: Long, data: Long): Unit = {
      // 1. 启动传输
      axi.aw.valid #= true
      axi.aw.addr  #= addr
      axi.w.valid  #= true
      axi.w.data   #= data
      if (axi.w.strb != null) axi.w.strb #= 0xF
      if (axi.w.last != null) axi.w.last #= true

      var awDone = false
      var wDone  = false

      // 2. 循环等待握手
      while(!awDone || !wDone) {
        // --- 核心修改：先等待时钟沿发生 ---
        dut.clockDomain.waitSampling()

        // --- 时钟沿之后，检查刚才那一拍是否 Ready ---
        // 并且：一旦握手成功，**立即**拉低 Valid，防止下一拍重复握手

        if(!awDone && axi.aw.ready.toBoolean) {
          awDone = true
          axi.aw.valid #= false // 立即撤销 AW 请求
        }

        if(!wDone && axi.w.ready.toBoolean) {
          wDone = true
          axi.w.valid #= false // 立即撤销 W 请求
        }
      }

      // 3. 等待 B 响应
      while(!axi.b.valid.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
    }
    // =============================================================
    // 测试流程
    // =============================================================
    println("\n[Test] 开始配置指令寄存器 (32-bit 对齐模式)...")

    // 寄存器地址映射 (BaseAddr = 0x04)
    // 注意：所有的偏移量都变了
    val REG_TRIGGER      = 0x00

    val REG_UID          = 0x04 + 0x00 // 0x04
    val REG_MAT_OP       = 0x04 + 0x04 // 0x08
    val REG_SHIFT_MAT    = 0x04 + 0x08 // 0x0C
    val REG_TRANSPOSE    = 0x04 + 0x0C // 0x10
    val REG_ACT_FUNC     = 0x04 + 0x10 // 0x14
    val REG_SHIFT_ACT    = 0x04 + 0x14 // 0x18

    val REG_IN0_ADDR     = 0x04 + 0x18 // 0x1C
    val REG_IN0_SHAPE_0  = 0x04 + 0x1C // 0x20
    val REG_IN0_SHAPE_1  = 0x04 + 0x20 // 0x24

    val REG_IN1_ADDR     = 0x04 + 0x24 // 0x28
    val REG_IN1_SHAPE_0  = 0x04 + 0x28 // 0x2C
    val REG_IN1_SHAPE_1  = 0x04 + 0x2C // 0x30

    val REG_OUT_ADDR     = 0x04 + 0x30 // 0x34
    val REG_OUT_SHAPE_0  = 0x04 + 0x34 // 0x38
    val REG_OUT_SHAPE_1  = 0x04 + 0x38 // 0x3C

    // 1. 配置参数 (模拟软件填表，完全不需要位拼接了)
    axiWrite(REG_UID,         0x40000)

    axiWrite(REG_MAT_OP,      0x2) // MatrixOp
    axiWrite(REG_SHIFT_MAT,   0x20) // Shift
    axiWrite(REG_TRANSPOSE,   0x1) // True
    axiWrite(REG_ACT_FUNC,    0x4) // ReLU etc.
    axiWrite(REG_SHIFT_ACT,   0x20)

    axiWrite(REG_IN0_ADDR,    0x80000)
    axiWrite(REG_IN0_SHAPE_0, 0x8000) // H
    axiWrite(REG_IN0_SHAPE_1, 0x8000) // W

    axiWrite(REG_IN1_ADDR,    0x80000)
    axiWrite(REG_IN1_SHAPE_0, 0x8000) // H
    axiWrite(REG_IN1_SHAPE_1, 0x8000) // W

    axiWrite(REG_OUT_ADDR,    0x80000)
    axiWrite(REG_OUT_SHAPE_0, 0x8000) // H
    axiWrite(REG_OUT_SHAPE_1, 0x8000) // W

    println("[Test] 参数配置完成，准备 Trigger...")

    // 检查此时 Valid 应该为低 (还没 Commit)
    assert(!dut.io.regValid.toBoolean, "Error: Valid should be low before trigger")

    // 2. 触发提交 (Commit)
    // 往 0x00 写 1
    axiWrite(REG_TRIGGER, 0x1)

    println("[Test] Trigger 信号已发送")


    // 3. 验证输出
    // 此时 Valid 应该变高
    while(!dut.io.regValid.toBoolean){
      dut.clockDomain.waitSampling()
    }

    //if (dut.io.regValid.toBoolean) {
    println("[Pass] 检测到 Valid 信号变高！")

    // 读取输出数据 (注意：io.regInstr 被截断为 32bit)
    // 在 ComputeInstruction_TypeDef 中，UID 通常在最低位
    val outputBits = dut.io.regInstr.toBigInt
    println(f"[Info] ILA RegInstr (Low 32bits): 0x$outputBits%x")

    // 验证 UID 是否正确 (0x12345)
    val receivedUID = outputBits & 0x7FFFF // 19 bits mask

    if (receivedUID == 0x40000) {
      println(f"[Pass] UID 匹配成功: 0x$receivedUID%x")
    } else {
      println(f"[Fail] UID 不匹配! 期望: 0x12345, 实际: 0x$receivedUID%x")
    }

    //}

    // 4. 修改参数再次触发 (验证 Shadow Register 特性)
    println("\n[Test] 修改 UID 并再次触发...")
    axiWrite(REG_UID, 0x54321) // 只改 UID，其他参数保留
    axiWrite(REG_TRIGGER, 0x1) // 再次 Commit

    while(!dut.io.regValid.toBoolean){
      dut.clockDomain.waitSampling()
    }

    val newOutputBits = dut.io.regInstr.toBigInt
    val newUID = newOutputBits & 0x7FFFF
    if (newUID == 0x54321) {
      println(f"[Pass] 第二次 UID 匹配成功: 0x$newUID%x")
    } else {
      println(f"[Fail] 第二次 UID 不匹配: 0x$newUID%x")
    }

    println("\nsimulation finished.")
    dut.clockDomain.waitSampling(10)
    simSuccess()
  }
}

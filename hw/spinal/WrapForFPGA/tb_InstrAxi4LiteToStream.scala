// SPDX-License-Identifier: MIT
package WrapForFPGA

import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.sim._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.amba4.axilite._

object tb_Inst128_Wrapper extends App {

  // 创建仿真输出目录
  new File("rtl/tb_Inst128_Wrapper_report").mkdirs()

  // =======================================
  // 1. 编译仿真模型
  // =======================================
  // 我们测试默认的 32-bit 接口版本
  val simCompiled = SimConfig
    .withVCS(VCSFlags(
      compileFlags = List("-kdb", "-lca", "+notimingchecks"),
      elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
      runFlags = List("-l ./run.log")
    ))
    .withFsdbWave
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .compile(new Inst128_Wrapper(use64BitBus = false))

  // =======================================
  // 2. 运行仿真
  // =======================================
  simCompiled.doSim("tb") { dut =>
    // 初始化时钟
    dut.clockDomain.forkStimulus(10)

    val axi = dut.io.s_axi
    val stream = dut.io.m_stream

    // 初始化信号
    axi.aw.valid #= false
    axi.w.valid  #= false
    axi.b.ready  #= true
    axi.ar.valid #= false
    axi.r.ready  #= true
    stream.ready #= false

    dut.clockDomain.waitSampling(10)

    // =============================================================
    // 辅助函数：AXI4-Lite 写操作
    // =============================================================
    def axiLiteWrite(addr: Long, data: Long): Unit = {
      axi.aw.valid #= true
      axi.aw.addr  #= addr
      // 注意：AxiLite4 没有 aw.len, aw.burst, aw.size 等

      axi.w.valid  #= true
      axi.w.data   #= data
      axi.w.strb   #= 0xF
      // 注意：AxiLite4 没有 w.last

      var awDone = false
      var wDone  = false

      while(!awDone || !wDone) {
        if(!awDone && axi.aw.ready.toBoolean) awDone = true
        if(!wDone && axi.w.ready.toBoolean)   wDone = true
        dut.clockDomain.waitSampling()

        if(awDone) axi.aw.valid #= false
        if(wDone)  axi.w.valid  #= false
      }

      while(!axi.b.valid.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
    }

    // =============================================================
    // 辅助函数：AXI4-Lite 读操作 (用于检查 Busy 状态)
    // =============================================================
    def axiLiteRead(addr: Long): BigInt = {
      axi.ar.valid #= true
      axi.ar.addr  #= addr
      // 注意：AxiLite4 没有 ar.len 等

      while(!axi.ar.ready.toBoolean) dut.clockDomain.waitSampling()

      axi.ar.valid #= false

      while(!axi.r.valid.toBoolean) dut.clockDomain.waitSampling()

      val data = axi.r.data.toBigInt
      dut.clockDomain.waitSampling()
      data
    }

    // =============================================================
    // 测试流程
    // =============================================================
    println("\n[Test] 开始测试 Inst128_Wrapper...")

    // 地址映射 (参考 Inst192_Wrapper 中的 myMapping)
    val REG_CTRL = 0x00
    val SLICE0  = 0x10
    val SLICE1  = 0x14
    val SLICE2  = 0x18
    val SLICE3  = 0x1C

    // 1. 配置影子寄存器
    println("[Step 1] 配置参数...")
    axiLiteWrite(SLICE0, 0x11111111L)
    axiLiteWrite(SLICE1, 0xAAAAAAAAL)
    axiLiteWrite(SLICE2, 0xBBBBBBBBL)
    axiLiteWrite(SLICE3, 0xCCCCCCCCL)

    // 验证此时 FIFO 应该是空的
    assert(!stream.valid.toBoolean, "Error: Trigger 前 Stream 不应有数据")

    // 2. 触发提交 (Commit)
    println("[Step 2] 发送 Trigger...")
    axiLiteWrite(REG_CTRL, 0x1)

    dut.clockDomain.waitSampling()
    if(stream.valid.toBoolean){
      stream.ready #= true
      dut.clockDomain.waitSampling()
    }else{
      assert(false, "Error: Trigger 后无数据输出")
    }
    stream.ready #= false

    dut.clockDomain.waitSampling(5)

    // 开始第二次测试
    println("[Step 1] 配置参数...")
    axiLiteWrite(SLICE0, 0x22222222L)
    axiLiteWrite(SLICE1, 0xBBBBBBBBL)
    axiLiteWrite(SLICE2, 0xCCCCCCCCL)
    axiLiteWrite(SLICE3, 0xDDDDDDDDL)

    // 验证此时 FIFO 应该是空的

    dut.clockDomain.waitSampling(10)
    // 2. 触发提交 (Commit)
    println("[Step 2] 发送 Trigger...")
    axiLiteWrite(REG_CTRL, 0x1)

    dut.clockDomain.waitSampling()
    if(stream.valid.toBoolean){
      stream.ready #= true
      dut.clockDomain.waitSampling()
    }else{
      assert(false, "Error: Trigger 后无数据输出")
    }
    stream.ready #= false

    // 3. 验证输出
//    if(stream.valid.toBoolean) {
//      println("[Check] 检测到 Valid 信号！")
//
//      // 获取 payload 各个字段 (SpinalHDL 生成的 bundle 信号名称可能带层级)
//      // 这里我们直接读取整个 payload 的大整数来验证
//      val payload = dut.io.m_stream.payload.toBigInt
//      println(f"[Check] Payload Hex: 0x$payload%x")
//
//      // 手动拼装期望值 (注意 Bundle 的字段顺序，通常最后定义的在高位，或者取决于生成逻辑)
//      // Instruction192 定义顺序: header, addrA, addrB, addrC, config1, config2
//      // SpinalHDL Bundle 转 Bits 通常是按照定义顺序从低位到高位排列 (Low to High)
//      // 即: header 在 [31:0], addrA 在 [63:32] ...
//
//      val expHeader = BigInt("11223344", 16)
//      val expAddrA  = BigInt("A0000000", 16)
//      val expAddrB  = BigInt("B0000000", 16)
//      val expAddrC  = BigInt("C0000000", 16)
//      val expCfg1   = BigInt("00000001", 16)
//      val expCfg2   = BigInt("0000000F", 16)
//
//      // 计算期望的大整数
//      val expected = (expHeader << 0) |
//        (expAddrA  << 32) |
//        (expAddrB  << 64) |
//        (expAddrC  << 96) |
//        (expCfg1   << 128)|
//        (expCfg2   << 160)
//
//      if(payload == expected) {
//        println("[Pass] 数据校验成功！")
//      } else {
//        println(f"[Fail] 数据不匹配。\n期望: 0x$expected%x\n实际: 0x$payload%x")
//        simFailure()
//      }
//
//      // 消耗掉这个数据
//      stream.ready #= true
//      dut.clockDomain.waitSampling()
//      stream.ready #= false
//
//    } else {
//      println("[Fail] Trigger 后没有产生 Valid 输出！")
//      simFailure()
//    }

//    // 4. 验证 Busy 状态 (FIFO 填充测试)
//    println("\n[Step 3] FIFO 满状态测试...")
//    // 我们的 FIFO 深度是 4。再发 4 次指令填满它
//    for(i <- 1 to 4) {
//      axiLiteWrite(REG_CTRL, 0x1) // 这里的参数不变，只是反复 Trigger
//    }
//
//    dut.clockDomain.waitSampling(5)
//
//    // 读取状态寄存器 (0x00)
//    // Bit 0 是 Busy/Full 标志
//    val status = axiLiteRead(REG_CTRL)
//    if ((status & 0x1) == 1) {
//      println(f"[Pass] 状态寄存器正确返回 Busy (0x$status%x)")
//    } else {
//      println(f"[Fail] FIFO 应满但状态未 Busy (0x$status%x)")
//    }

    println("\nSimulation Finished.")
    dut.clockDomain.waitSampling(20)
    simSuccess()
  }
}

object tb_Inst128_Wrapper_64bit extends App {

  // 创建仿真输出目录
  new File("rtl/tb_Inst128_Wrapper_64bit_report").mkdirs()

  // =======================================
  // 1. 编译仿真模型 (开启 64-bit)
  // =======================================
  val simCompiled = SimConfig
    .withVCS(VCSFlags(
      compileFlags = List("-kdb", "-lca", "+notimingchecks"),
      elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
      runFlags = List("-l ./run_64.log")
    ))
    .withFsdbWave
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    // [重点] 这里参数设为 true
    .compile(new Inst128_Wrapper(use64BitBus = true))

  // =======================================
  // 2. 运行仿真
  // =======================================
  simCompiled.doSim("tb_64bit") { dut =>
    dut.clockDomain.forkStimulus(10)

    val axi = dut.io.s_axi
    val stream = dut.io.m_stream

    // 初始化信号
    axi.aw.valid #= false
    axi.w.valid  #= false
    axi.b.ready  #= true
    axi.ar.valid #= false
    axi.r.ready  #= true
    stream.ready #= false

    dut.clockDomain.waitSampling(10)

    // =============================================================
    // 辅助函数：64-bit AXI Lite 写
    // =============================================================
    def axiLiteWrite64(addr: Long, data: BigInt): Unit = {
      axi.aw.valid #= true
      axi.aw.addr  #= addr

      axi.w.valid  #= true
      axi.w.data   #= data
      // [重点] 64位总线，全写开启是 0xFF (8 bytes)
      axi.w.strb   #= 0xFF

      var awDone = false
      var wDone  = false

      while(!awDone || !wDone) {
        if(!awDone && axi.aw.ready.toBoolean) awDone = true
        if(!wDone && axi.w.ready.toBoolean)   wDone = true
        dut.clockDomain.waitSampling()

        if(awDone) axi.aw.valid #= false
        if(wDone)  axi.w.valid  #= false
      }

      while(!axi.b.valid.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
    }

    // =============================================================
    // 测试流程
    // =============================================================
    println("\n[Test] 开始 64-bit 总线测试...")

    val REG_CTRL = 0x00
    // 虽然寄存器映射地址没变(0x10, 0x14, 0x18, 0x1C)，
    // 但我们可以通过 64位写 0x10 一次性写入 (Slice1 << 32 | Slice0)
    val BASE_DATA = 0x10

    // 准备数据
    val slice0 = BigInt("11111111", 16)
    val slice1 = BigInt("22222222", 16)
    val slice2 = BigInt("33333333", 16)
    val slice3 = BigInt("44444444", 16)

    // 拼装成 64-bit 数据
    // 注意：高位是 SLICE1 (地址 0x14)，低位是 SLICE0 (地址 0x10)
    val dataWord0 = (slice1 << 32) | slice0
    // 高位是 SLICE3 (地址 0x1C)，低位是 SLICE2 (地址 0x18)
    val dataWord1 = (slice3 << 32) | slice2

    // 1. 写入数据 (利用 64-bit 带宽)
    println(f"[Step 1] 写入数据 (64-bit Access)...")
    println(f"  Write 0x$BASE_DATA%x : 0x$dataWord0%x")
    axiLiteWrite64(BASE_DATA, dataWord0)

    println(f"  Write 0x${BASE_DATA + 8}%x : 0x$dataWord1%x")
    axiLiteWrite64(BASE_DATA + 8, dataWord1)

    // 2. 触发
    println("[Step 2] 发送 Trigger...")
    // 尽管控制寄存器只有 1 bit 有效，用 64-bit 写 0x00 也没问题
    axiLiteWrite64(REG_CTRL, 1)

    dut.clockDomain.waitSampling(5)

    // 3. 验证输出
//    if(stream.valid.toBoolean) {
//      val payload = dut.io.m_stream.payload.toBigInt
//      println(f"[Check] 收到 Payload: 0x$payload%x")
//
//      // 验证拼接逻辑：SpinalHDL Bundle 转 Bits 也是低位在前
//      // Payload = SLICE3 ## SLICE2 ## SLICE1 ## SLICE0
//      val expected = (slice3 << 96) | (slice2 << 64) | (slice1 << 32) | slice0
//
//      if(payload == expected) {
//        println("[Pass] 数据完整性校验成功！")
//      } else {
//        println(f"[Fail] 数据错误！\nExpect: 0x$expected%x\nActual: 0x$payload%x")
//        simFailure()
//      }
//
//      stream.ready #= true
//      dut.clockDomain.waitSampling()
//      stream.ready #= false
//    } else {
//      println("[Fail] 未检测到 Valid 输出")
//      simFailure()
//    }

    println("Simulation Finished.")
    dut.clockDomain.waitSampling(10)
    simSuccess()
  }
}
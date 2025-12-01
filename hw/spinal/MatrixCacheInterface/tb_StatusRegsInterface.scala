package MatrixCacheInterface
import spinal.core._
import spinal.core.sim._
import java.io.File
import spinal.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axilite._

object sim_StatusRegsInterface_test extends App {

  // ======================
  // 仿真输出路径
  // ======================
  new File("rtl/StatusRegsInterface/sim_StatusRegsInterface_test_report").mkdirs()

  // VCS 仿真编译参数
  val flags = VCSFlags(
    compileFlags = List("-kdb", "-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )

  // ======================
  // 生成 Verilog
  // ======================
  val report = SpinalConfig(
    targetDirectory = "rtl/StatusRegsInterface/sim_StatusRegsInterface_test_report",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(TopWithStatusRegs())
    .printPruned()

  // ======================
  // 编译模块
  // ======================
  val dutCompiled = SimConfig
    .withVCS(flags)
    .withTimePrecision(1 ps)
    .withFSDBWave
    .withConfig(SpinalConfig(bitVectorWidthMax = 20000))
    .compile(report)

  // ======================
  // 仿真主体
  // ======================
  dutCompiled.doSim("status_regs_interface_tb") { dut =>

    def write(address: BigInt, data: BigInt): Unit = {
      // 地址通道
      dut.io.axi.aw.valid #= true
      dut.io.axi.aw.addr  #= address
      dut.io.axi.w.valid  #= true
      dut.io.axi.w.data   #= data
      dut.io.axi.w.strb   #= (BigInt(1) << (dut.io.axi.config.dataWidth / 8)) - 1
      dut.io.axi.b.ready  #= true
      // 等待握手
      dut.clockDomain.waitSamplingWhere(dut.io.axi.aw.ready.toBoolean && dut.io.axi.w.ready.toBoolean)
      //waitUntil(axi.aw.ready.toBoolean && axi.w.ready.toBoolean)
      dut.io.axi.aw.valid #= false
      dut.io.axi.w.valid  #= false
      // 等待应答
      dut.clockDomain.waitSamplingWhere(dut.io.axi.b.valid.toBoolean)
      //waitUntil(axi.b.valid.toBoolean)
      dut.io.axi.b.ready  #= false
    }
    def read(address: BigInt): BigInt = {
      var rdata = BigInt(0)
      dut.io.axi.ar.valid #= true
      dut.io.axi.ar.addr  #= address
      dut.io.axi.r.ready  #= true
      dut.clockDomain.waitSamplingWhere(dut.io.axi.ar.ready.toBoolean)
      //waitUntil(axi.ar.ready.toBoolean)
      dut.io.axi.ar.valid #= false
      // 等待数据返回
      dut.clockDomain.waitSamplingWhere(dut.io.axi.r.valid.toBoolean)
      //waitUntil(axi.r.valid.toBoolean)
      rdata = dut.io.axi.r.data.toBigInt
      dut.io.axi.r.ready #= false
      rdata
    }




    // 建立时钟与复位
    dut.clockDomain.forkStimulus(period = 10)
    dut.clockDomain.assertReset()
    sleep(100)
    dut.clockDomain.deassertReset()
    sleep(100)

    dut.io.axi.ar.valid #= false

    // ======================
    // AXI-Lite Master 驱动
    // ======================
    //val axi = AxiLite4Driver(dut.io.axi, dut.clockDomain)

    // ======================
    // 测试流程
    // ======================
    println("[SIM] Start test sequence...")

    // 写入 GPIO 输出寄存器 0x00
    write(0x00, 0xA5)
    sleep(20)

    // 读回 GPIO 输出寄存器
    val outValue = read(0x00)
    println(f"[SIM] GPIO_OUT readback = 0x$outValue%X")

    // 写 SET 寄存器 (置位 bit1, bit3)
    write(0x08, 0x0A)
    sleep(20)
    val setAfter = read(0x00)
    println(f"[SIM] After SET, GPIO_OUT = 0x$setAfter%X")

    // 写 CLR 寄存器 (清零 bit3)
    write(0x0C, 0x08)
    sleep(20)
    val clrAfter = read(0x00)
    println(f"[SIM] After CLR, GPIO_OUT = 0x$clrAfter%X")

    // 写 DIR 寄存器 (bit[3:0] 设为输出)
//    write(0x08, 0x0F)
//    sleep(20)
//    val dirVal = read(0x08)
//    println(f"[SIM] GPIO_DIR = 0x$dirVal%X")

    // 模拟输入变化后读出
    sleep(10)
    val inVal = read(0x04)
    println(f"[SIM] GPIO_IN = 0x$inVal%X")

    println("[SIM] Test completed.")
    sleep(500)
    simSuccess()
  }
}

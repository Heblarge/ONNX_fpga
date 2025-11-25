package StreamProcessor

import Util._
import Interface._

import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.sim._
import scala.util.Random



object tb_Axi4StreamShifter extends App {

  // 创建仿真输出目录
  new File("rtl/Axi4StreamShifter/tb_report").mkdirs()

  // =======================================
  // 1. 构造配置对象
  // =======================================
  val PW = 32  // payload 宽度（<= dataWidth）
  val DW = 256  // AXIS tdata 宽度

  val axisCfg = AxiStreamConfig(
    dataWidth = DW,
    useLast   = true
  )

  val shifterCfg = Axi4StreamShifter_Config(
    payloadType  = HardType(Bits(PW bits)),
    axisCfg      = axisCfg,
    fifoDepth    = 4,
    UIDWidth     = 8,
    ShiftWidth   = 8,
    AddressWidth = 32,
    ShapeWidth   = 16
  )

  // =======================================
  // 2. 生成 Verilog（仅一次）
  // =======================================
  val report = SpinalConfig(
    targetDirectory = "rtl/Axi4StreamShifter/tb_report",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(new Axi4StreamShifter(shifterCfg))
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
        targetDirectory = "rtl/Axi4StreamShifter/tb_report",
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
    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.assertReset()
    dut.clockDomain.waitSampling(5)
    dut.clockDomain.deassertReset()
    dut.clockDomain.waitSampling(10)

    val sAxis = dut.io.sAxis
    val mAxis = dut.io.mAxis
    val instr = dut.io.instr

    // 初始化
    sAxis.tvalid #= false
    sAxis.tdata  #= 0
    sAxis.tlast  #= false

    mAxis.tready #= false

    instr.valid #= false

    def clearInstr(): Unit = {
    instr.payload.UID  #= 0
    instr.payload.shiftLeft_AfterMatrixOperation #= 0
    instr.payload.doTranspose #= false
    instr.payload.shiftLeft_AfterActivation #= 0
    instr.payload.input0Address #= 0
    instr.payload.input1Address #= 0
    instr.payload.outputAddress #= 0
    instr.payload.input0Shape(0) #= 0
    instr.payload.input0Shape(1) #= 0
    instr.payload.input1Shape(0) #= 0
    instr.payload.input1Shape(1) #= 0
    instr.payload.outputShape(0) #= 0
    instr.payload.outputShape(1) #= 0
    }

    clearInstr()


    // ---------------------------------------
    // 随机 backpressure
    // ---------------------------------------
    fork {
      while(true){
        mAxis.tready #= Random.nextBoolean()
        dut.clockDomain.waitSampling()
      }
    }

    // ---------------------------------------
    // 发送一条 ComputeInstruction（只关心 shiftLeft_AfterMatrixOperation）
    // ---------------------------------------
    def sendInstr(shift: Int): Unit = {
      instr.payload.UID  #= 0
      instr.payload.shiftLeft_AfterMatrixOperation #= shift
      instr.payload.doTranspose #= false
      instr.payload.shiftLeft_AfterActivation #= 0
      instr.payload.input0Address #= 0
      instr.payload.input1Address #= 0
      instr.payload.outputAddress #= 0
      instr.payload.input0Shape(0) #= 0
      instr.payload.input0Shape(1) #= 0
      instr.payload.input1Shape(0) #= 0
      instr.payload.input1Shape(1) #= 0
      instr.payload.outputShape(0) #= 0
      instr.payload.outputShape(1) #= 0

      instr.valid #= true
      dut.clockDomain.waitSampling()
      while(!instr.ready.toBoolean) dut.clockDomain.waitSampling()
      dut.clockDomain.waitSampling()
      instr.valid #= false

      println(s"[${simTime()} ns] Instr fire, shift = $shift")
    }

    // ---------------------------------------
    // AXIS 单拍发送：保持 valid 直到 ready
    // ---------------------------------------
    def axisSend(data: BigInt, last: Boolean = false): Unit = {
        val maskPW = (BigInt(1) << PW) - 1
        val d = data & maskPW

        // 驱动信号
        sAxis.tdata  #= d
        sAxis.tlast  #= last
        sAxis.tvalid #= true

        // 等待真正 fire（valid && ready）
        do {
            dut.clockDomain.waitSampling()
        } while(!(sAxis.tvalid.toBoolean && sAxis.tready.toBoolean))

        // fire 已发生：下一拍撤 valid，避免重复发送
        sAxis.tvalid #= false
        sAxis.tlast  #= false
    }



    // ---------------------------------------
    // 计算期望移位（和 DUT 语义一致：SInt 左/算术右移）
    // ---------------------------------------
    def expectShift(data: BigInt, shift: Int): BigInt = {
      val maskPW = (BigInt(1) << PW) - 1
      val x = data & maskPW
      val signed =
        if(((x >> (PW-1)) & 1) == 1) x - (BigInt(1) << PW) else x

      val absS = math.min(math.abs(shift), PW-1)

      val shiftedSigned =
        if(shift > 0) signed << absS
        else if(shift < 0) signed >> absS
        else signed

      shiftedSigned & maskPW
    }

    // 期望队列
    val expQueue = scala.collection.mutable.Queue[BigInt]()

    // ---------------------------------------
    // 监控输出并校验
    // ---------------------------------------
    var curShift = 0
    fork {
      while(true){
        dut.clockDomain.waitSampling()
        if(mAxis.tvalid.toBoolean && mAxis.tready.toBoolean){
          val maskPW = (BigInt(1) << PW) - 1
          val outPW = mAxis.tdata.toBigInt & maskPW
          println(f"[${simTime()} ns] Out fire! payload=0x${outPW.toString(16)} shift=$curShift")

          // 取出期望队列头做比对（下面主线程会 push 进去）
          val exp = expQueue.dequeue()
          assert(outPW == exp,
            s"Mismatch! got=0x${outPW.toString(16)} exp=0x${exp.toString(16)} shift=$curShift")
        }
      }
    }


    // =======================================
    // 5. 测试序列
    // =======================================
    println("===== Test 1: shift = +3 (left) =====")
    curShift = 3
    sendInstr(curShift)
    dut.clockDomain.waitSampling(1)  // assume 指令和数据不能同一拍来

    val d1 = BigInt("00001234", 16)
    val d2 = BigInt("89abcdef", 16)
    expQueue.enqueue(expectShift(d1, curShift))
    expQueue.enqueue(expectShift(d2, curShift))

    axisSend(d1)
    axisSend(d2, last = true)

    dut.clockDomain.waitSampling(30)

    println("===== Test 2: shift = -4 (arith right) =====")
    curShift = -4
    sendInstr(curShift)
    dut.clockDomain.waitSampling(1)

    val d3 = BigInt("f0000001", 16)
    val d4 = BigInt("0aaaaaaa", 16)
    expQueue.enqueue(expectShift(d3, curShift))
    expQueue.enqueue(expectShift(d4, curShift))

    axisSend(d3)
    axisSend(d4, last = true)


    // 多跑一会，把 FIFO 都吐完
    dut.clockDomain.waitSampling(100)

    println("Simulation finished.")
    dut.clockDomain.waitSampling(10)
    simSuccess()
  }
}

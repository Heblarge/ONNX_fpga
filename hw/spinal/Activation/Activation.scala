package Activation

import java.io.File
import spinal.core._
import spinal.lib._
import ExponentialFunction._
import LogarithmFunction._
import ReLUFunction._
import SoftplusFunction._
import Interface._
import GeMM.SystolicArray2D.SIntShifter

// 定义激活函数配置类
case class Activation_Config(
                              Matx_Width   : Int,                   // 输入矩阵宽度
                              MatX_Width   : Int,                   // 输出矩阵宽度
                              element_in_Width: Int,                // 输入元素位宽
                              element_out_Width: Int,               // 输出元素位宽
                              max_indepth  :Int,                    // 最大缓存深度
                              expCfg       : EXP_function_cfg,      // 指数函数配置
                              lnCfg        : LN_function_cfg,       // 对数函数配置
                              reluCfg      : ReLU_function_cfg,     // ReLU函数配置
                              softplusCfg  : Softplus_function_cfg, // Softplus函数配置
                              UIDWidth     : Int,                   // UID位宽
                              ShiftWidth   : Int,                   // 移位位宽
                              SlicecntWidth: Int                    // Slice计数位宽
                            ) {
  require((max_indepth >= MatX_Width) && (max_indepth >= Matx_Width))
  def in_type = SInt(element_in_Width bits)   // 定义输入类型
  def out_type = SInt(element_out_Width bits) // 定义输出类型
}

// 定义激活函数模块
case class Activation(cfg: Activation_Config) extends Component {

  // 定义输入流类型的外部调用接口
  def in_Mats_Type(): out_Mats_AfterMatrixOperation_TypeDef = {
    new out_Mats_AfterMatrixOperation_TypeDef(
      cfg.Matx_Width,
      cfg.element_in_Width,
      cfg.ShiftWidth,
      cfg.UIDWidth,
      cfg.SlicecntWidth
    )
  }
  // 定义输出流类型的外部调用接口
  def out_Mats_Type(): out_Mats_AfterActivation_TypeDef = {
    new out_Mats_AfterActivation_TypeDef(
      cfg.MatX_Width,
      cfg.element_out_Width,
      cfg.UIDWidth,
      cfg.SlicecntWidth
    )
  }

  val io = new Bundle {// 定义接口端口
    val in_Mats = slave(Stream(in_Mats_Type()))   // 定义输入流接口
    val out_Mats = master(Stream(out_Mats_Type()))// 定义输出流接口
  }

  // 批量实例化Matx_Width个激活函数硬件模块
  val expI       = Seq.fill(cfg.Matx_Width)(EXP_function(cfg.expCfg))          // 实例化指数函数
  val lnI        = Seq.fill(cfg.Matx_Width)(LN_function(cfg.lnCfg))            // 实例化对数函数
  val reluI      = Seq.fill(cfg.Matx_Width)(ReLU_function(cfg.reluCfg))        // 实例化ReLU函数
  val softplusI  = Seq.fill(cfg.Matx_Width)(Softplus_function(cfg.softplusCfg))// 实例化Softplus函数

  val inRegData  = Vec(Reg(SInt(cfg.element_in_Width bits)) init(0), cfg.Matx_Width) // 定义输入数据寄存器
  val inRegValid = RegInit(False)                                                    // 定义输入有效信号寄存器

  val busyIn  = RegInit(False)   // 定义输入忙信号寄存器
  val busyOut = RegInit(False)   // 定义输出忙信号寄存器
  val instrReg = Reg(io.in_Mats.payload.CoreInstruction_AfterMatrixOperation) // 定义指令寄存器

  val activation_dataReg = Reg(Vec.fill(cfg.Matx_Width)(cfg.out_type)) // 定义激活函数输出寄存器

  val Outvalid = RegInit(False) // 定义输出有效信号寄存器
  val ValidVec = Vec(Reg(Bool()) init False, 1) // 定义有效信号向量
  ValidVec.reduceLeft((a, b) => {b := a;b}) // 级联赋值
  ValidVec(0) := Outvalid

  val in_final = Reg(Bool()) init False   // 定义输入final信号寄存器
  val out_final = Reg(Bool()) init False  // 定义输出final信号寄存器

  val DEPTH   = cfg.max_indepth          // 定义缓存深度
  val PTRW    = log2Up(DEPTH)            // 定义指针位宽
  val out_databuffer  = Vec.fill(DEPTH)(Vec(Reg(SInt(cfg.element_out_Width bits)) init(0), cfg.MatX_Width)) // 定义输出数据缓存
  val finalbuffer  = Vec.fill(DEPTH)(Reg(Bool()) init(False)) // 定义final信号缓存

  val writePtr   = Reg(UInt(PTRW bits)) init(0) // 定义写指针寄存器
  val readPtr    = Reg(UInt(PTRW bits)) init(0) // 定义读指针寄存器
  val count      = Reg(UInt((PTRW+1) bits)) init(0) // 定义缓存计数寄存器
  val sawFinal   = RegInit(False) // 定义是否遇到final信号寄存器

  val notFull = count < DEPTH     // 判断缓存是否未满
  val notEmpty = count =/= 0      // 判断缓存是否非空
  val doWrite = ValidVec.last && !sawFinal && notFull // 判断是否写入缓存

  // 处理输入valid信号
  when(io.in_Mats.fire) {
    // fire时更新输入数据
    for(i <- 0 until cfg.Matx_Width) inRegData(i) := io.in_Mats.payload.Z(i)
    inRegValid := True
  }.otherwise{
    inRegValid := False
  }

  // 控制ready信号和状态切换
  when(io.in_Mats.fire && !busyIn && !busyOut) {
    busyIn   := True
    instrReg := io.in_Mats.payload.CoreInstruction_AfterMatrixOperation
    sawFinal   := False
    writePtr   := 0
    readPtr    := 0
    count      := 0
    for(i <- 0 until DEPTH) {
      finalbuffer(i) := False
    }
  }
  when(io.in_Mats.fire && busyIn && io.in_Mats.payload.Final) {
    busyIn  := False
    busyOut := True
  }
  when(io.out_Mats.fire && io.out_Mats.payload.Final) {
    busyOut := False
  }

  // 激活函数计算
  for (i <- 0 until cfg.Matx_Width) {
    expI(i).io.x.payload      := 0
    lnI(i).io.x.payload       := 0
    reluI(i).io.x.payload     := 0
    softplusI(i).io.x.payload := 0

    expI(i).io.x.valid      := False
    lnI(i).io.x.valid       := False
    reluI(i).io.x.valid     := False
    softplusI(i).io.x.valid := False

    switch(instrReg.Activation_Instruction.activationFunction) {
      is(Activation_TypeDef.Exp) {
        expI(i).io.x.payload  := inRegData(i)
        expI(i).io.x.valid    := inRegValid
        activation_dataReg(i) := expI(i).io.expx.payload.resize(cfg.element_out_Width).asSInt
      }

      is(Activation_TypeDef.Log) {
        lnI(i).io.x.payload   := inRegData(i).resize(cfg.element_out_Width - 1 bits).asUInt
        lnI(i).io.x.valid     := inRegValid
        activation_dataReg(i) := lnI(i).io.lnx.payload.resize(cfg.element_out_Width).asBits.asSInt
      }

      is(Activation_TypeDef.Relu) {
        reluI(i).io.x.payload := inRegData(i)
        reluI(i).io.x.valid   := inRegValid
        activation_dataReg(i) := reluI(i).io.relux.payload.resize(cfg.element_out_Width).asSInt
      }

      is(Activation_TypeDef.Softplus) {
        softplusI(i).io.x.payload := inRegData(i)
        softplusI(i).io.x.valid   := inRegValid
        activation_dataReg(i)     := softplusI(i).io.softplusx.payload.resize(cfg.element_out_Width)
      }

      is(Activation_TypeDef.None) {
        activation_dataReg(i) := inRegData(i).resize(cfg.element_out_Width)
      }
    }
  }

  // 处理输出valid信号
  Outvalid := instrReg.Activation_Instruction.activationFunction.mux(
    Activation_TypeDef.Exp      -> expI(0).io.expx.valid, // 任选一个通道的valid信号
    Activation_TypeDef.Log      -> lnI(0).io.lnx.valid,
    Activation_TypeDef.Relu     -> reluI(0).io.relux.valid,
    Activation_TypeDef.Softplus -> softplusI(0).io.softplusx.valid,
    default                     -> inRegValid
  )

  // 计算结果移位
  val shifters  = Seq.fill(cfg.Matx_Width) {SIntShifter(cfg.element_out_Width, cfg.element_out_Width)} // 实例化移位器
  val shift_data = Vec(Reg(SInt(cfg.element_out_Width bits)) init(0), cfg.MatX_Width)                  // 定义移位后数据寄存器
  for(i <- 0 until cfg.Matx_Width) {
    shifters(i).io.input       := activation_dataReg(i)
    shifters(i).io.shiftAmount := instrReg.Activation_Instruction.shiftLeft_AfterActivation.resize(log2Up(cfg.element_out_Width + 1) + 1 bits)
    shift_data(i)              := shifters(i).io.output
  }

  // 处理final信号延迟
  val FinalVec = Vec(Reg(Bool()) init False, Math.max(cfg.lnCfg.bit_frac + 3 + 2 + 1,cfg.expCfg.bit_int + 3 + 1))
  FinalVec.reduceLeft((a, b) => {b := a;b})
  FinalVec(0) := io.in_Mats.payload.Final && io.in_Mats.fire
  out_final := instrReg.Activation_Instruction.activationFunction.mux(
    Activation_TypeDef.Exp      -> FinalVec(cfg.expCfg.bit_int + 1 + 2),  // exp延迟 11 = 1 + bit_int + 2
    Activation_TypeDef.Log      -> FinalVec(cfg.lnCfg.bit_frac + 3 + 2), // ln延迟
    Activation_TypeDef.Relu     -> FinalVec(4),   // relu延迟
    Activation_TypeDef.Softplus -> FinalVec(2),   // softplus延迟
    Activation_TypeDef.None     -> FinalVec(1)    // 无函数延迟
  )

  // 输出结果和final存入缓存
  when(doWrite) {
    out_databuffer(writePtr) := shift_data
    finalbuffer(writePtr) := out_final
    writePtr := writePtr + 1
    when(io.out_Mats.fire) {
      count := count
    } elsewhen(!io.out_Mats.fire) {
      count    := count + 1
    }
    when(out_final) {  // 遇到final停止写入
      sawFinal := True
    }
  }

  // 读指针和计数管理
  when(io.out_Mats.fire) {
    when(!doWrite) {
      readPtr := readPtr + 1
      count := count - 1
    } elsewhen(doWrite) {
      readPtr := readPtr + 1
    }
  }

  // 输出流赋值
  io.out_Mats.payload.Activation_x := out_databuffer(readPtr)
  io.out_Mats.payload.Final := finalbuffer(readPtr)
  io.out_Mats.payload.CoreInstruction_AfterActivation.Collector_Instruction := instrReg.Collector_Instruction
  io.out_Mats.valid := notEmpty

  io.in_Mats.ready := !busyOut // 输入ready信号

}

// 定义生成Verilog的主对象
object Activation_01 {

  new File("rtl/Activation").mkdir() // 创建输出目录
  val cfg = Activation_Config(
    Matx_Width  = 32,
    MatX_Width  = 32,
    element_in_Width = 21,
    element_out_Width = 21,
    max_indepth = 32,
    expCfg      = EXP_function_cfg(bit_int = 8, bit_frac = 12, x_max = 9),
    lnCfg       = LN_function_cfg(bit_int = 8, bit_frac = 12),
    reluCfg     = ReLU_function_cfg(bit_int = 8, bit_frac = 12),
    softplusCfg = Softplus_function_cfg(bit_int = 8, bit_frac = 12),
    UIDWidth      = 32,
    ShiftWidth   = 6,
    SlicecntWidth = 16
  )

  def main(arg:Array[String]): Unit ={
    SpinalConfig(
      targetDirectory = "rtl/Activation",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(Activation(cfg))
      .printPruned()
  }
}


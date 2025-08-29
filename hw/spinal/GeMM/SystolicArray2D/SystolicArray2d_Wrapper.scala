package GeMM.SystolicArray2D

import Slicer._
import spinal.core._
import spinal.core.ClockDomain
import spinal.core.sim._
import spinal.lib._
import Interface._

import scala.math.{ceil, min, pow, round}

/**
  * SystolicArray2D_Wrap_Config
  * 配置类，用于定义SystolicArray2D_Wrapper模块的参数。
  * Configuration class for defining parameters of the SystolicArray2D_Wrapper module.
  */
case class SystolicArray2D_Wrap_Config(
                                        in_Length_Max: Int,
                                        in_Length_Min: Int = 1,
                                        in_MatA_row_num: Int = 12,
                                        in_MatB_col_num: Int = 24,
                                        in_MatA_element_Width: Int = 8,
                                        in_MatB_element_Width: Int = 8,
                                        out_MatZ_element_Width: Int = 8,
                                        Enable_Transpose_logic: Boolean = true,
                                        Enable_ElementWise_logic: Boolean = true,
                                        in_FIFO_Depth: Int = 16,
                                        out_FIFO_Depth: Int = 8,
                                        instruction_FIFO_Depth: Int = 32,

                                        //需要与用来配置slicer模块的参数一致
                                        UIDWidth: Int = 32,
                                        ShiftWidth: Int = 6,
                                        SlicecntWidth: Int = 16,
                                      ) {
  def to_SystolicArray2D_CC_Config(): SystolicArray2D_CC_Config = {
    SystolicArray2D_CC_Config(
      in_Length_Max = in_Length_Max,
      in_Length_Min = in_Length_Min,
      in_MatA_row_num = in_MatA_row_num,
      in_MatB_col_num = in_MatB_col_num,
      in_MatA_element_Width = in_MatA_element_Width,
      in_MatB_element_Width = in_MatB_element_Width,
      out_MatZ_element_Width = out_MatZ_element_Width,
      Enable_Transpose_logic = Enable_Transpose_logic,
      Enable_ElementWise_logic = Enable_ElementWise_logic,
      in_FIFO_Depth = in_FIFO_Depth,
      out_FIFO_Depth = out_FIFO_Depth
    )
  }
  def to_SystolicArray_Config(): SystolicArray2D_Config = {
    SystolicArray2D_Config(
      in_Length_Max = in_Length_Max,
      in_Length_Min = in_Length_Min,
      in_MatA_row_num = in_MatA_row_num,
      in_MatB_col_num = in_MatB_col_num,
      in_MatA_element_Width = in_MatA_element_Width,
      in_MatB_element_Width = in_MatB_element_Width,
      out_MatZ_element_Width = out_MatZ_element_Width,
    )
  }
}

/**
  * SystolicArray2D_Wrapper
  * 顶层模块，封装了SystolicArray2D_CC模块并添加了指令FIFO和锁存逻辑。
  * Top-level module that encapsulates the SystolicArray2D_CC module and adds instruction FIFO and latch logic.
  */
case class SystolicArray2D_Wrapper(
                                    cfg: SystolicArray2D_Wrap_Config,
                                    clk_in: ClockDomain,
                                    clk_out: ClockDomain,
                                    clk_core: ClockDomain
                                  ) extends Component {

  // 用于例化完整的 core instruction 包含 SystolicArray2D 、Activation 、Collector 三部分指令。
  /**
    * CoreInstruction_Type
    * 定义完整的核心指令类型，包括UID、移位宽度和切片计数宽度。
    * Defines the complete core instruction type, including UID, shift width, and slice count width.
    */
  def CoreInstruction_Type(): CoreInstruction_TypeDef = {
    new CoreInstruction_TypeDef(
      UIDWidth = cfg.UIDWidth,
      ShiftWidth = cfg.ShiftWidth,
      SlicecntWidth = cfg.SlicecntWidth
    )
  }
  def CoreInstruction_AfterMatrixOperation_Type(): CoreInstruction_AfterMatrixOperation_TypeDef = {
    new CoreInstruction_AfterMatrixOperation_TypeDef(
      UIDWidth = cfg.UIDWidth,
      ShiftWidth = cfg.ShiftWidth,
      SlicecntWidth = cfg.SlicecntWidth
    )
  }
//  case class in_Mats_TypeDef(cfg: SystolicArray2D_Wrap_Config) extends Bundle {
//    val A = Vec.fill(cfg.in_MatA_row_num)(SInt(cfg.in_MatA_element_Width bits))
//    val B = Vec.fill(cfg.in_MatB_col_num)(SInt(cfg.in_MatB_element_Width bits))
//    val CoreInstruction = CoreInstruction_Type()
//    val Final = Bool()
//  }
  /**
    * in_Mats_Type
    * 定义输入矩阵类型，包括矩阵A/B和核心指令。
    * Defines the input matrix type, including matrices A/B and core instructions.
    */
  def in_Mats_Type(): in_Mats_TypeDef = {
    new in_Mats_TypeDef(in_MatA_row_num = cfg.in_MatA_row_num,
      in_MatA_element_Width = cfg.in_MatA_element_Width,
      in_MatB_col_num = cfg.in_MatB_col_num,
      in_MatB_element_Width = cfg.in_MatB_element_Width,
      ShiftWidth = cfg.ShiftWidth,
      UIDWidth = cfg.UIDWidth,
      SlicecntWidth = cfg.SlicecntWidth)
  }

//  case class out_Mats_TypeDef(cfg: SystolicArray2D_Wrap_Config) extends Bundle {
//    val Z = Vec.fill(cfg.to_SystolicArray2D_CC_Config().out_MatZ_Width)(SInt(cfg.out_MatZ_element_Width bits))
//    val CoreInstruction = CoreInstruction_Type()
//    val Final = Bool()
//  }
//  def out_Mats_Type(): out_Mats_TypeDef = {
//    new out_Mats_TypeDef(cfg)
//  }
  /**
    * out_Mats_Type
    * 定义输出矩阵类型，包括矩阵Z和核心指令。
    * Defines the output matrix type, including matrix Z and core instructions.
    */
  def out_Mats_Type(): out_Mats_AfterMatrixOperation_TypeDef = {
    new out_Mats_AfterMatrixOperation_TypeDef(
      out_MatZ_Width = cfg.to_SystolicArray2D_CC_Config().out_MatZ_Width,
      out_MatZ_element_Width = cfg.out_MatZ_element_Width,
      ShiftWidth = cfg.ShiftWidth,
      UIDWidth = cfg.UIDWidth,
      SlicecntWidth = cfg.SlicecntWidth)
  }

  val io = new Bundle {
    val in_Mats_with_Core_Instruction = slave Stream(in_Mats_Type())
    val out_Mats_with_Core_Instruction = master Stream(out_Mats_Type())
  }

  //  // 三个时钟域定义（你可按需接管）

  // 实例化 CC 模块
  /**
    * SystolicArray2D_CC_inst
    * 实例化的SystolicArray2D_CC模块，用于执行核心计算。
    * Instantiated SystolicArray2D_CC module for core computation.
    */
  val SystolicArray2D_CC_inst = SystolicArray2D_CC(cfg.to_SystolicArray2D_CC_Config(), clk_in = clk_in, clk_core = clk_core, clk_out = clk_out)
  // 创建 OpMode_TypeDef 类型
  val OpMode_wire = OpMode_TypeDef(cfg.to_SystolicArray_Config())
  // 提取 Stream 中的指令
  val instruction = io.in_Mats_with_Core_Instruction.payload.CoreInstruction
  // 将指令转换为OpMode（逻辑字段设置）
  OpMode_wire.do_PostTranspose     := instruction.SystolicArray2D_CC_Instruction.doTranspose
  OpMode_wire.MatrixOperation      := instruction.SystolicArray2D_CC_Instruction.matrixOperation 
  OpMode_wire.post_Shift           := instruction.SystolicArray2D_CC_Instruction.shiftLeft_AfterMatrixOperation.resized

  val cc_in_payload = SystolicArray2D_CC.in_Mats_TypeDef(cfg.to_SystolicArray2D_CC_Config())
  val inst_fifo_ready = Bool()

  // A/B 数据直接转发
  for (i <- 0 until cfg.in_MatA_row_num) {
    cc_in_payload.A(i) := io.in_Mats_with_Core_Instruction.payload.A(i)
  }
  for (j <- 0 until cfg.in_MatB_col_num) {
    cc_in_payload.B(j) := io.in_Mats_with_Core_Instruction.payload.B(j)
  }
  cc_in_payload.mode  := OpMode_wire
  cc_in_payload.Final := io.in_Mats_with_Core_Instruction.payload.Final

  SystolicArray2D_CC_inst.io.in_Mats.valid := io.in_Mats_with_Core_Instruction.valid
  io.in_Mats_with_Core_Instruction.ready := SystolicArray2D_CC_inst.io.in_Mats.ready & inst_fifo_ready
  SystolicArray2D_CC_inst.io.in_Mats.payload := cc_in_payload

//   Core Instruction FIFO
  /**
    * CoreInstFifo
    * 指令FIFO，用于存储和传递核心指令。
    * Instruction FIFO for storing and passing core instructions.
    */
  val CoreInstFifo = StreamFifoCC(
    dataType = CoreInstruction_AfterMatrixOperation_Type(),
    depth = cfg.instruction_FIFO_Depth,
    pushClock = clk_in,
    popClock = clk_out
  )

  // 将 in_Mats.payload.CoreInstruction 写入 FIFO
  inst_fifo_ready := CoreInstFifo.io.push.ready
  val inst_pushed = clk_in(Reg(Bool()) init(False))
  CoreInstFifo.io.push.payload.Activation_Instruction := io.in_Mats_with_Core_Instruction.payload.CoreInstruction.Activation_Instruction
  CoreInstFifo.io.push.payload.Collector_Instruction := io.in_Mats_with_Core_Instruction.payload.CoreInstruction.Collector_Instruction
  when(io.in_Mats_with_Core_Instruction.fire && !inst_pushed) {
    // 仅在第一次fire时push
    CoreInstFifo.io.push.valid := True
    inst_pushed := True  // 设置标志，防止重复push
  } otherwise {
    CoreInstFifo.io.push.valid := False  // 当没有fire时，保持push信号为无效
  }
  when(io.in_Mats_with_Core_Instruction.fire) {
    when(io.in_Mats_with_Core_Instruction.payload.Final) {
      inst_pushed := False
    }
  }
  

  // 将 FIFO输出的core instruction 对齐到输出的cc_out(SystolicArray2D_CC_inst.io.out_Mats)

  // 由于输出cc_out将会分批进行，也就是一个指令的计算结果会fire多次分批输出。直到其中的Final信号拉高表示最后一个传输数据
  // 因此需要在第一次fire时从指令fifo中读出一个指令存到一个寄存器并保持住，直到final拉高释放锁定。之后再fire在缓存，循环往复。

  // 指令锁存
  /**
    * inst_locked
    * 指令锁存标志，用于控制指令的锁存和释放。
    * Instruction latch flag for controlling instruction locking and releasing.
    */
  val inst_locked = clk_out(Reg(Bool()) init(False))
  val inst_buffer = clk_out(Reg(CoreInstruction_AfterMatrixOperation_Type()))
  CoreInstFifo.io.pop.ready := False
  // 输出流
  /**
    * out_stream
    * 输出流，包含锁存的指令和矩阵数据。
    * Output stream containing latched instructions and matrix data.
    */
  val cc_out = SystolicArray2D_CC_inst.io.out_Mats
  val out_stream = clk_out(Stream(out_Mats_Type()))
  // 锁存逻辑

  val inst_comb = Mux(cc_out.fire && !inst_locked, CoreInstFifo.io.pop.payload, inst_buffer)

  when(cc_out.fire) {
    when(!inst_locked) {
      // 第一次fire，锁存指令
      CoreInstFifo.io.pop.ready := True
      inst_buffer := CoreInstFifo.io.pop.payload
      inst_locked := True
    }
    // 输出时携带锁存的指令
    out_stream.payload.CoreInstruction_AfterMatrixOperation := inst_comb
    out_stream.payload.Final := cc_out.payload.Final

    // Final拉高时，解锁
    when(cc_out.payload.Final) {
      inst_locked := False
    }
  }
    .otherwise {
    out_stream.payload.CoreInstruction_AfterMatrixOperation.assignDontCare()
    out_stream.payload.Final := False
  }
  out_stream.valid := cc_out.valid
  for (i <- 0 until cfg.to_SystolicArray2D_CC_Config().out_MatZ_Width) {
    out_stream.payload.Z(i) := cc_out.payload.Z(i)
  }
  cc_out.ready := out_stream.ready
  io.out_Mats_with_Core_Instruction <> out_stream
}

/**
  * SystolicArray2D_Wrap_Verilog
  * 生成SystolicArray2D_Wrapper模块的Verilog代码。
  * Generates Verilog code for the SystolicArray2D_Wrapper module.
  */
object SystolicArray2D_Wrap_Verilog extends App {
  val testLength = 4
  val clk_domain_defaultConfig=ClockDomainConfig(clockEdge = RISING, resetKind = ASYNC, resetActiveLevel = HIGH, softResetActiveLevel = HIGH, clockEnableActiveLevel = HIGH)

  val cfg = SystolicArray2D_Wrap_Config(
    in_Length_Max = testLength,
    in_Length_Min = 4,
    in_MatA_row_num = 4,
    in_MatB_col_num = 4,
    in_MatA_element_Width = 8, // 输入的A矩阵的每个数的位宽
    in_MatB_element_Width = 8, // 输入的B矩阵的每个数的位宽
    out_MatZ_element_Width = 8,// 输出的Z矩阵的每个数的位宽
    UIDWidth = 4,
    ShiftWidth = 20,
    SlicecntWidth = 16
  )
  val vendor = MemBlackBoxer.Vendor.UMC40
  val FileDir = "rtl/SystolicArray2D_Wrapper/verilog"
  import java.io.File
  new File(FileDir).mkdirs()

  val rtl=SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
    removePruned=false,
    bitVectorWidthMax = 20000, //disable internal bigvector limitation"Way too big signal Bits"
  ).generateVerilog(new SystolicArray2D_Wrapper(cfg,ClockDomain.external("SystolicArray2D_Wrapper_in"),
    ClockDomain.external("SystolicArray2D_Wrapper_out"),
    ClockDomain.external("SystolicArray2D_Wrapper_core")))
  rtl.printPruned()
}


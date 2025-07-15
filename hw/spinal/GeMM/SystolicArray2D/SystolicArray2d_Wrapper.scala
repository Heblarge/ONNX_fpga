package GeMM.SystolicArray2D

import Slicer._
import spinal.core._
import spinal.core.ClockDomain
import spinal.core.sim._
import spinal.lib._
import Interface._

import scala.math.{ceil, min, pow, round}

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

case class SystolicArray2D_Wrapper(
                                    cfg: SystolicArray2D_Wrap_Config,
                                    clk_in: ClockDomain,
                                    clk_out: ClockDomain,
                                    clk_core: ClockDomain
                                  ) extends Component {

  // 用于例化完整的 core instruction 包含 SystolicArray2D 、Activation 、Collector 三部分指令。
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
  val SystolicArray2D_CC_inst = SystolicArray2D_CC(cfg.to_SystolicArray2D_CC_Config(), clk_in = clk_in, clk_core = clk_core, clk_out = clk_out)
  // 创建 opmode 类型
  val opmode_wire = opmode(cfg.to_SystolicArray_Config())
  // 提取 Stream 中的指令
  val instruction = io.in_Mats_with_Core_Instruction.payload.CoreInstruction
  // 将指令转换为opmode（逻辑字段设置）
  opmode_wire.do_PostTranspose     := instruction.SystolicArray2D_CC_Instruction.doTranspose
  opmode_wire.do_MatMul            := instruction.SystolicArray2D_CC_Instruction.matrixOperation === MatrixOperation_TypeDef.MatMul
  opmode_wire.do_ElementWiseAdd    := instruction.SystolicArray2D_CC_Instruction.matrixOperation === MatrixOperation_TypeDef.ElementAdd
  opmode_wire.do_ElementWiseMul    := instruction.SystolicArray2D_CC_Instruction.matrixOperation === MatrixOperation_TypeDef.ElementMul
  opmode_wire.do_ElementWiseMax    := instruction.SystolicArray2D_CC_Instruction.matrixOperation === MatrixOperation_TypeDef.ElementMax
  opmode_wire.post_Shift           := instruction.SystolicArray2D_CC_Instruction.shiftLeft_AfterMatrixOperation.resized

  val cc_in_payload = SystolicArray2D_CC.in_Mats_TypeDef(cfg.to_SystolicArray2D_CC_Config())
  val inst_fifo_ready = Bool()

  // A/B 数据直接转发
  for (i <- 0 until cfg.in_MatA_row_num) {
    cc_in_payload.A(i) := io.in_Mats_with_Core_Instruction.payload.A(i)
  }
  for (j <- 0 until cfg.in_MatB_col_num) {
    cc_in_payload.B(j) := io.in_Mats_with_Core_Instruction.payload.B(j)
  }
  cc_in_payload.mode  := opmode_wire
  cc_in_payload.Final := io.in_Mats_with_Core_Instruction.payload.Final

  SystolicArray2D_CC_inst.io.in_Mats.valid := io.in_Mats_with_Core_Instruction.valid
  io.in_Mats_with_Core_Instruction.ready := SystolicArray2D_CC_inst.io.in_Mats.ready & inst_fifo_ready
  SystolicArray2D_CC_inst.io.in_Mats.payload := cc_in_payload

//   Core Instruction FIFO
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


  // 由于输出cc_out将会分批进行，也就是一个指令的计算结果会fire多次分批输出。直到其中的Final信号拉高表示最后一个传输数据
  // 因此需要在第一次fire时从指令fifo中读出一个指令存到一个寄存器并保持住，直到final拉高释放锁定。之后再fire在缓存，循环往复。

  // 指令锁存
  val inst_locked = clk_out(Reg(Bool()) init(False))
  val inst_buffer = clk_out(Reg(CoreInstruction_AfterMatrixOperation_Type()))
  CoreInstFifo.io.pop.ready := False
  // 输出流
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

object SystolicArray2D_Wrap_Verilog extends App {
  val testLength = 32
  val clk_domain_defaultConfig=ClockDomainConfig(clockEdge = RISING, resetKind = ASYNC, resetActiveLevel = HIGH, softResetActiveLevel = HIGH, clockEnableActiveLevel = HIGH)

  val cfg = SystolicArray2D_Wrap_Config(
    in_Length_Max = testLength,
    in_Length_Min = 16,
    in_MatA_row_num = 16,
    in_MatB_col_num = 16,
    in_MatA_element_Width = 8, // 输入的A矩阵的每个数的位宽
    in_MatB_element_Width = 8, // 输入的B矩阵的每个数的位宽
    out_MatZ_element_Width = 22,// 输出的Z矩阵的每个数的位宽
    UIDWidth = 32,
    ShiftWidth = 20,
    SlicecntWidth = 16
  )
  val vendor = MemBlackBoxer.Vendor.UMC40
  val FileDir = "rtl/SystolicArray2D_Wrapper/verilog"
  import java.io.File
  new File(FileDir).mkdirs()

  val rtl=SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = false,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
    removePruned=true,
    bitVectorWidthMax = 20000, //disable internal bigvector limitation"Way too big signal Bits"
  ).generateVerilog(new SystolicArray2D_Wrapper(cfg,ClockDomain.external("SystolicArray2D_Wrapper_in"),
    ClockDomain.external("SystolicArray2D_Wrapper_out"),
    ClockDomain.external("SystolicArray2D_Wrapper_core")))
  rtl.printPruned()
}


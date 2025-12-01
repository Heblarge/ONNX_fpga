package Interface

import spinal.core._
import spinal.lib.{Stream, Fragment}

import scala.util.Random

case class Task_mm2s_TypeDef(
    mem_addr_width: Int,
    mem_data_width: Int,
    RepeatNum_width: Int,
    Enable_Padding_logic: Boolean
) extends Bundle {
  val StartAddr = UInt(mem_addr_width bits) // 起始地址
  val ValidNum = Enable_Padding_logic generate UInt(RepeatNum_width bits) // 有效位数
  val data_to_Pad = Enable_Padding_logic generate Bits(mem_data_width bits) // 填充值
  // 如果填充逻辑被使能则这个ValidNum是代表该任务的有效位数，生成ValidNum个内存请求。无效但依然Repeat的位会被自定义值(data_to_Pad)填充
  val RepeatNum = UInt(RepeatNum_width bits) // 不填充0的情况下，所有的repeat位默认都是有效的//传输元素个数
  val Offset = UInt(mem_addr_width bits) // will add offset to adder per repeat//每一个元素的地址偏移

  def init_to_zero(): this.type = {
    StartAddr := 0
    if (Enable_Padding_logic) {
      ValidNum := 0
      data_to_Pad := 0
    }
    RepeatNum := 0
    Offset := 0
    return this
  }
}

case class Data_mm2s_TypeDef(mem_data_width: Int) extends Bundle {
  val data = Bits(mem_data_width bits) // 定义数据位宽
  val Final = Bool() // 定义是否为最后一个数据包的标志
}

case class Task_s2mm_TypeDef(
    mem_addr_width: Int,
    mem_data_width: Int,
    RepeatNum_width: Int,
    Enable_UnPadding_logic: Boolean
) extends Bundle {
  val StartAddr = UInt(mem_addr_width bits) // 起始地址
  val ValidNum = Enable_UnPadding_logic generate UInt(RepeatNum_width bits) // 有效数据包数目
  // 如果使能除去无效位的逻辑,则这个ValidNum代表该任务的有效数据包数目，生成ValidNum个内存请求。无效但依然Repeat的位会被舍弃
  val RepeatNum = UInt(RepeatNum_width bits) // 重复次数
  val Offset = UInt(mem_addr_width bits) // 不填充0的情况下，所有的repeat位默认都是有效的

  def init_to_zero(): this.type = {
    StartAddr := 0
    if (Enable_UnPadding_logic) {
      ValidNum := 0
    }
    RepeatNum := 0
    Offset := 0
    return this
  }
}

case class Data_s2mm_TypeDef(mem_data_width: Int) extends Bundle {
  val data = Bits(mem_data_width bits) // 定义数据位宽
  val Final = Bool() // 定义是否为最后一个数据包的标志
}

object MatrixOperation_TypeDef extends SpinalEnum(defaultEncoding = binarySequential) {
  val MatMul, ElementAdd, ElementMul, ElementMax = newElement() // 定义矩阵操作类型：矩阵乘法、元素加法、元素乘法
}

object Activation_TypeDef extends SpinalEnum(defaultEncoding = binarySequential) {
  val Exp, Log, Softplus, Relu, None = newElement() // 定义激活函数类型：指数、对数、Softplus、ReLU、无激活

  def randomSpinalEnum(random: Random): Activation_TypeDef.E = { // 别用这个函数了，试试这个random.nextSpinalEnum
    val ret = random.nextInt(5) match {
      case 0 => Exp
      case 1 => Log
      case 2 => Softplus
      case 3 => Relu
      case 4 => None
    }
    ret
  }
}

case class ComputeInstruction_TypeDef(UIDWidth: Int, ShiftWidth: Int, AddressWidth: Int, ShapeWidth: Int)
    extends Bundle {
  val UID = UInt(UIDWidth bits) // 唯一标识符
  val matrixOperation = MatrixOperation_TypeDef() // 矩阵操作类型
  val shiftLeft_AfterMatrixOperation = SInt(ShiftWidth bits) // 矩阵操作后的移位量
  val doTranspose = Bool() // 是否进行转置操作
  val activationFunction = Activation_TypeDef() // 激活函数类型
  val shiftLeft_AfterActivation = SInt(ShiftWidth bits) // 激活函数后的移位量
  val input0Address = UInt(AddressWidth bits) // 输入0的地址
  val input0Shape = Vec(UInt(ShapeWidth bits), 2) // 输入0的形状
  val input1Address = UInt(AddressWidth bits) // 输入1的地址
  val input1Shape = Vec(UInt(ShapeWidth bits), 2) // 输入1的形状
  val outputAddress = UInt(AddressWidth bits) // 输出的地址
  val outputShape = Vec(UInt(ShapeWidth bits), 2) // 输出的形状
}

case class Sliced_ComputeInstruction_TypeDef(UIDWidth: Int, AddressWidth: Int, ShapeWidth: Int) extends Bundle {
  val UID = UInt(UIDWidth bits) // 唯一标识符
  val doTranspose = Bool() // 是否进行转置操作
  val outputAddress = UInt(AddressWidth bits) // 输出的地址
  val outputShape = Vec(UInt(ShapeWidth bits), 2) // 输出的形状

  def assignFromInst(inst: ComputeInstruction_TypeDef) = {
    UID := inst.UID
    doTranspose := inst.doTranspose
    outputAddress := inst.outputAddress
    outputShape := inst.outputShape
  }
}

case class SystolicArray2D_CC_Instruction_TypeDef(ShiftWidth: Int) extends Bundle {
  val matrixOperation = MatrixOperation_TypeDef() // 矩阵操作类型
  val shiftLeft_AfterMatrixOperation = SInt(ShiftWidth bits) // 矩阵操作后的移位量
  val doTranspose = Bool() // 是否进行转置操作
}

case class Activation_Instruction_TypeDef(ShiftWidth: Int) extends Bundle {
  val activationFunction = Activation_TypeDef() // 激活函数类型
  val shiftLeft_AfterActivation = SInt(ShiftWidth bits) // 激活函数后的移位量
}

case class Collector_Instruction_TypeDef(UIDWidth: Int, SlicecntWidth: Int) extends Bundle {
  val UID = UInt(UIDWidth bits) // 唯一标识符
  val MatA_row_slice_cnt = UInt(SlicecntWidth bits) // 矩阵A的行切片计数
  val MatB_col_slice_cnt = UInt(SlicecntWidth bits) // 矩阵B的列切片计数
}

case class CoreInstruction_TypeDef(ShiftWidth: Int, UIDWidth: Int, SlicecntWidth: Int) extends Bundle {
  val SystolicArray2D_CC_Instruction = SystolicArray2D_CC_Instruction_TypeDef(ShiftWidth) // 矩阵操作指令
  val Activation_Instruction = Activation_Instruction_TypeDef(ShiftWidth) // 激活函数指令
  val Collector_Instruction = Collector_Instruction_TypeDef(UIDWidth, SlicecntWidth) // 数据收集指令

  def assignFromInst(inst: ComputeInstruction_TypeDef, matARowSliceCnt: UInt, matBColSliceCnt: UInt) = {
    SystolicArray2D_CC_Instruction.matrixOperation := inst.matrixOperation
    SystolicArray2D_CC_Instruction.shiftLeft_AfterMatrixOperation := inst.shiftLeft_AfterMatrixOperation
    SystolicArray2D_CC_Instruction.doTranspose := inst.doTranspose
    Activation_Instruction.activationFunction := inst.activationFunction
    Activation_Instruction.shiftLeft_AfterActivation := inst.shiftLeft_AfterActivation
    Collector_Instruction.UID := inst.UID
    Collector_Instruction.MatA_row_slice_cnt := matARowSliceCnt
    Collector_Instruction.MatB_col_slice_cnt := matBColSliceCnt
  }
}

case class CoreInstruction_AfterMatrixOperation_TypeDef(ShiftWidth: Int, UIDWidth: Int, SlicecntWidth: Int)
    extends Bundle {
  val Activation_Instruction = Activation_Instruction_TypeDef(ShiftWidth) // 激活函数指令
  val Collector_Instruction = Collector_Instruction_TypeDef(UIDWidth, SlicecntWidth) // 数据收集指令
}

case class CoreInstruction_AfterActivation_TypeDef(UIDWidth: Int, SlicecntWidth: Int) extends Bundle {
  val Collector_Instruction = Collector_Instruction_TypeDef(UIDWidth, SlicecntWidth) // 数据收集指令
}

case class in_Mats_TypeDef(
    in_MatA_row_num: Int,
    in_MatA_element_Width: Int,
    in_MatB_col_num: Int,
    in_MatB_element_Width: Int,
    ShiftWidth: Int,
    UIDWidth: Int,
    SlicecntWidth: Int
) extends Bundle {
  val A = Vec.fill(in_MatA_row_num)(SInt(in_MatA_element_Width bits)) // 矩阵A的行数据
  val B = Vec.fill(in_MatB_col_num)(SInt(in_MatB_element_Width bits)) // 矩阵B的列数据
  val CoreInstruction = CoreInstruction_TypeDef(ShiftWidth, UIDWidth, SlicecntWidth) // 核心指令
  val Final = Bool() // 是否为最后一个数据包的标志
}

case class in_Mats_ForFragment(
    in_MatA_row_num: Int,
    in_MatA_element_Width: Int,
    in_MatB_col_num: Int,
    in_MatB_element_Width: Int,
    ShiftWidth: Int,
    UIDWidth: Int,
    SlicecntWidth: Int
) extends Bundle {
  val A = Vec.fill(in_MatA_row_num)(SInt(in_MatA_element_Width bits)) // 矩阵A的行数据
  val B = Vec.fill(in_MatB_col_num)(SInt(in_MatB_element_Width bits)) // 矩阵B的列数据
  val CoreInstruction = CoreInstruction_TypeDef(ShiftWidth, UIDWidth, SlicecntWidth) // 核心指令
}

object in_Mats_Converter {
  def withFragment(input: Stream[in_Mats_TypeDef]) = {
    val output =
      Stream(
        Fragment(
          in_Mats_ForFragment(
            in_MatA_row_num = input.in_MatA_row_num,
            in_MatA_element_Width = input.in_MatA_element_Width,
            in_MatB_col_num = input.in_MatB_col_num,
            in_MatB_element_Width = input.in_MatB_element_Width,
            ShiftWidth = input.ShiftWidth,
            UIDWidth = input.UIDWidth,
            SlicecntWidth = input.SlicecntWidth
          )
        )
      )
    output.A := input.A
    output.B := input.B
    output.CoreInstruction := input.CoreInstruction
    output.last := input.Final
    output.valid := input.valid
    input.ready := output.ready
    output
  }

  def withoutFragment(input: Stream[Fragment[in_Mats_ForFragment]]) = {
    val output =
      Stream(
        in_Mats_TypeDef(
          in_MatA_row_num = input.in_MatA_row_num,
          in_MatA_element_Width = input.in_MatA_element_Width,
          in_MatB_col_num = input.in_MatB_col_num,
          in_MatB_element_Width = input.in_MatB_element_Width,
          ShiftWidth = input.ShiftWidth,
          UIDWidth = input.UIDWidth,
          SlicecntWidth = input.SlicecntWidth
        )
      )
    output.A := input.A
    output.B := input.B
    output.CoreInstruction := input.CoreInstruction
    output.Final := input.last
    output.valid := input.valid
    input.ready := output.ready
    output
  }
}

case class out_Mats_AfterMatrixOperation_TypeDef(
    out_MatZ_Width: Int,
    out_MatZ_element_Width: Int,
    ShiftWidth: Int,
    UIDWidth: Int,
    SlicecntWidth: Int
) extends Bundle {
  val Z = Vec.fill(out_MatZ_Width)(SInt(out_MatZ_element_Width bits)) // 矩阵Z的输出数据
  val CoreInstruction_AfterMatrixOperation =
    CoreInstruction_AfterMatrixOperation_TypeDef(ShiftWidth, UIDWidth, SlicecntWidth) // 矩阵操作后的核心指令
  val Final = Bool() // 是否为最后一个数据包的标志
}

case class out_Mats_AfterActivation_TypeDef(MatX_Width: Int, Activation_x_Width: Int, UIDWidth: Int, SlicecntWidth: Int)
    extends Bundle {
  val Activation_x = Vec.fill(MatX_Width)(SInt(Activation_x_Width bits)) // 激活函数的输出数据
  val CoreInstruction_AfterActivation = CoreInstruction_AfterActivation_TypeDef(UIDWidth, SlicecntWidth) // 激活后的核心指令
  val Final = Bool() // 是否为最后一个数据包的标志
}

case class out_Mats_AfterActivation_ForFragment(
    MatX_Width: Int,
    Activation_x_Width: Int,
    UIDWidth: Int,
    SlicecntWidth: Int
) extends Bundle {
  val Activation_x = Vec.fill(MatX_Width)(SInt(Activation_x_Width bits)) // 激活函数的输出数据
  val CoreInstruction_AfterActivation = CoreInstruction_AfterActivation_TypeDef(UIDWidth, SlicecntWidth) // 激活后的核心指令
}

object out_Mats_AfterActivation_Converter {
  def withFragment(input: Stream[out_Mats_AfterActivation_TypeDef], UID: UInt, lock: Bool) = {
    val output =
      Stream(
        Fragment(
          out_Mats_AfterActivation_ForFragment(
            MatX_Width = input.MatX_Width,
            Activation_x_Width = input.Activation_x_Width,
            UIDWidth = input.UIDWidth,
            SlicecntWidth = input.SlicecntWidth
          )
        )
      )
    output.Activation_x := input.Activation_x
    output.CoreInstruction_AfterActivation := input.CoreInstruction_AfterActivation
    output.last := input.Final
    output.valid := input.valid && (input.CoreInstruction_AfterActivation.Collector_Instruction.UID === UID || lock)
    input.ready := output.ready && (input.CoreInstruction_AfterActivation.Collector_Instruction.UID === UID || lock)
    output
  }

  def withoutFragment(input: Stream[Fragment[out_Mats_AfterActivation_ForFragment]]) = {
    val output =
      Stream(
        out_Mats_AfterActivation_TypeDef(
          MatX_Width = input.MatX_Width,
          Activation_x_Width = input.Activation_x_Width,
          UIDWidth = input.UIDWidth,
          SlicecntWidth = input.SlicecntWidth
        )
      )
    output.Activation_x := input.Activation_x
    output.CoreInstruction_AfterActivation := input.CoreInstruction_AfterActivation
    output.Final := input.last
    output.valid := input.valid
    input.ready := output.ready
    output
  }
}



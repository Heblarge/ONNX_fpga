package GeMM.SystolicArray2D
import spinal.core._
import spinal.lib._
import spinal.lib.tools
import spinal.core
import scala.math
import Interface.MatrixOperation_TypeDef

case class SystolicArray2D_Config(
                                   in_Length_Max: Int, // 最大的乘加次数，譬如要计算A(32,16)*B(16,64),此时乘加的次数就是16
                                   in_Length_Min: Int = 1,// 最少的乘加次数,用于计算出缓存阵列结果所需的缓存数量

                                   in_MatA_row_num: Int = 24, // 一个周期从A侧输入的数的数量，也就是输入的A矩阵的行数
                                   in_MatB_col_num: Int = 24, // 一个周期从B侧输入的数的数量，也就是输入的B矩阵的列数
                                   // 这两个值也决定了整个脉动整列的尺寸
                                   in_MatA_element_Width: Int = 20, // 输入的A矩阵的每个数的位宽
                                   in_MatB_element_Width: Int = 20, // 输入的B矩阵的每个数的位宽
                                   Enable_Transpose_logic: Boolean = true,
                                   Enable_ElementWise_logic: Boolean = true,
                                   out_MatZ_element_Width: Int = 20

                                 )
{
  val SystolicArray2DUnit_Cfg = SystolicArray2DUnit_Config(
    in_Length = in_Length_Max,
    inA_Width = in_MatA_element_Width,
    inB_Width = in_MatB_element_Width,
    outZ_Width = out_MatZ_element_Width
  )

  // Z=A*B
  // 根据矩阵乘法，输出的行数就是A的行数，输出的列数就是B的列数
  val out_MatZ_row_num = in_MatA_row_num // 输出的Z矩阵的行数
  val out_MatZ_col_num = in_MatB_col_num // 输出的Z矩阵的列数

  require(
    in_Length_Min <= math.min(in_MatA_row_num, in_MatB_col_num),
    "in_Length_Min must be less or equal to the width (height) of Array."
  )

  // 一次矩阵完整输出的元素数是cfg.out_MatZ_row_num * cfg.out_MatZ_col_num
  val out_MatZ_Width = out_MatZ_row_num * out_MatZ_col_num

  /**
   * 计算并返回 x 除以 y 的上限
   *
   * 对于给定的两个整数 x 和 y，此方法计算 x 除以 y 的结果，并返回能够被 y 整除的最小整数。
   * 如果 x 是 y 的倍数，结果就是 x 除以 y 的值；如果 x 不是 y 的倍数，结果比 x 除以 y 的值大 1。
   *
   * @param x 被除数
   * @param y 除数
   * @return 能够被 y 整除的最小整数
   */
  val ceilDiv: (Int, Int) => Int = { case (x, y) => (x + y - 1) / y }

  // 理论上需要做 out_MatZ_buffer_num 个 buffer，
  // 最坏的情况下每一个周期都需要开始一个新的矩阵的计算，即 Final 线会恒为 1。
  // 此时，就需要 out_MatZ_buffer_num 个 buffer 缓存所有的中间结果
  val diag_num: Int = in_MatB_col_num + in_MatA_row_num
  val out_MatZ_buffer_num: Int = ceilDiv(diag_num, in_Length_Min) + 3

  // 打印系统配置信息到控制台
  SpinalInfo("SystolicArray2D_Config:\n")
  // 打印输出矩阵缓冲区数量到控制台
  SpinalInfo("out_MatZ_buffer_num=" + out_MatZ_buffer_num)

  // 如果要求启用转置逻辑，需要检查脉动阵列是否被配置为方阵
  
  if (Enable_Transpose_logic) {
    //换而言之，也就是检查矩阵A的行数是否等于矩阵B的列数
    if (in_MatA_row_num != in_MatB_col_num) {
      // 如果不相等，打印错误信息到控制台并报错
      SpinalError("Enable_Transpose_logic: in_MatA_row_num must equal to in_MatB_col_num")
    } else {
      // 如果相等，打印允许转置的结果到控制台
      SpinalInfo("in_MatA_row_num equals to in_MatB_col_num, allow result being transposed")
    }
  }

  // 如果要求启用按元素运算逻辑，需要检查脉动阵列是否被配置为方阵
  if (Enable_ElementWise_logic) {
    //换而言之，也就是检查矩阵A的行数是否等于矩阵B的列数
    if (in_MatA_row_num != in_MatB_col_num) {
      // 如果不相等，打印错误信息到控制台并报错
      SpinalError("Enable_ElementWise_logic: in_MatA_row_num must equal to in_MatB_col_num")
    } else {
      // 如果相等，打印允许按元素运算的结果到控制台
      SpinalInfo("in_MatA_row_num equals to in_MatB_col_num, allow performing element-wise operation")
    }
  }

  // 计算迭代间隔和总的延迟时间
  val IterationInterval = SystolicArray2DUnit_Cfg.IterationInterval
  val Latency = (in_MatA_row_num + in_MatB_col_num) * IterationInterval + SystolicArray2DUnit_Cfg.Latency
}
/**
  * SInt_withFinalMark
  * 包含数据和Final标志的有符号整数打包类型。
  * Signed integer bundle with data and Final flag.
  */
case class SInt_withFinalMark(element_Width: Int) extends Bundle {
  val data = SInt(element_Width bits)
  val Final = Bool()
}
//// /**
////    SInt_withFinalMarkandMode
////    扩展型数据包，包含数据、操作模式、转置、Final标志和移位量。
////    Extended bundle with data, MatrixOperation, transpose, Final flag, and shift amount.*/

/**
  * OpMode_TypeDef
  * 操作模式控制信号集合，支持转置、矩阵乘法、元素级乘法/加法/最大值及结果移位。
  * Operation OpMode_TypeDef bundle, supports transpose, matmul, element-wise mul/add/max, and result shift.
  */
case class OpMode_TypeDef(cfg: SystolicArray2D_Config) extends Bundle {
  
  
  val post_Shift = SInt(log2Up(cfg.out_MatZ_element_Width + 1) + 1 bits)//结果的截断位置
  //另外
  //如果Enable_Transpose_logic，则生成以下信号：
  val do_PostTranspose = cfg.Enable_Transpose_logic generate new Bool()
  //如果Enable_ElementWise_logic，则生成以下信号：
  val MatrixOperation = cfg.Enable_ElementWise_logic generate MatrixOperation_TypeDef()
  //如果!Enable_ElementWise_logic,默认do_MatMul为True
  //方便后续判断，并兼容旧代码
  def do_MatMul: Bool = cfg.Enable_ElementWise_logic match {
    case true => MatrixOperation === MatrixOperation_TypeDef.MatMul
    case false => True //如果不启用元素级运算逻辑，则默认是矩阵乘法
  }
  def do_ElementWiseMul: Bool = cfg.Enable_ElementWise_logic match {
    case true => MatrixOperation === MatrixOperation_TypeDef.ElementMul
    case false => False //如果不启用元素级运算逻辑，则默认不启用元素级乘法
  }
  def do_ElementWiseAdd: Bool = cfg.Enable_ElementWise_logic match {
    case true => MatrixOperation === MatrixOperation_TypeDef.ElementAdd
    case false => False //如果不启用元素级运算逻辑，则默认不启用元素级乘法
  }
  def do_ElementWiseMax: Bool = cfg.Enable_ElementWise_logic match {
    case true => MatrixOperation === MatrixOperation_TypeDef.ElementMax
    case false => False //如果不启用元素级运算逻辑，则默认不启用元素级乘法
  }


  //// val do_MatMul = Bool()
  //// val do_ElementWiseMul = cfg.Enable_ElementWise_logic generate Bool(OpMode==MatrixOperation_TypeDef.ElementWiseMul)
  //// val do_ElementWiseAdd = cfg.Enable_ElementWise_logic generate Bool()
  //// val do_ElementWiseMax = cfg.Enable_ElementWise_logic generate Bool()
}

/**
  * init_OpMode
  * 初始化操作模式为全False和移位为0。
  * Initialize OpMode_TypeDef with all flags False and shift 0.
  */
object init_OpMode {
  def apply(cfg: SystolicArray2D_Config): OpMode_TypeDef = {
    val OpMode = OpMode_TypeDef(cfg)
    OpMode.post_Shift := S(0)
    //如果Enable_Transpose_logic，则初始化
    if(cfg.Enable_Transpose_logic)
    {OpMode.do_PostTranspose := False}
    //如果Enable_ElementWise_logic，则初始化
    if(cfg.Enable_ElementWise_logic) 
    {
    OpMode.MatrixOperation:= MatrixOperation_TypeDef.MatMul
  }
  OpMode
}
}


case class SystolicArray2D(cfg: SystolicArray2D_Config) extends Component {
  /**
    * 功能控制标志
    * Function control flags
    */
  val enableTranspose = cfg.Enable_Transpose_logic // 转置功能使能 | Enable transpose
  val enableElementWise = cfg.Enable_ElementWise_logic // 元素级运算使能 | Enable element-wise operations
  
  /**
    * Bundle defining the input matrices and operation OpMode for the systolic array.
    *
    * @param cfg Configuration parameters for the systolic array.
    */
  case class in_Mats_TypeDef(cfg: SystolicArray2D_Config) extends Bundle {
    /**
      * Input matrix A/B:
      * Vector of signed integers with final mark signals, representing rows of matrix A/B.
      * The vector size is determined by cfg.in_MatA_row_num.
      */
    val A = Vec.fill(cfg.in_MatA_row_num)(SInt_withFinalMark(cfg.in_MatA_element_Width))
    val B = Vec.fill(cfg.in_MatB_col_num)(SInt_withFinalMark(cfg.in_MatB_element_Width))

    /**
      * Operation OpMode:
      * Control bundle specifying the systolic array's operation OpMode
      * (matrix multiplication, element-wise operations, etc.).
      */
    val OpMode = OpMode_TypeDef(cfg)//操作模式
  }
  def in_Mats_Bundle():in_Mats_TypeDef={new in_Mats_TypeDef(cfg)}
  case class out_Mats_TypeDef(cfg: SystolicArray2D_Config) extends Bundle {
    val Z = Vec.fill(cfg.out_MatZ_row_num)(Vec.fill(cfg.out_MatZ_col_num)(SInt(cfg.out_MatZ_element_Width bits)))
  }
  def out_Mats_Bundle():out_Mats_TypeDef={new out_Mats_TypeDef(cfg)}

  val io = new Bundle {
    val in_Mats = slave(Stream(in_Mats_Bundle())).addAttribute("DONT_TOUCH = \"TRUE\"")
    val out_Mats = master(Stream(out_Mats_Bundle())).addAttribute("DONT_TOUCH = \"TRUE\"")
  }

  //延迟逻辑
  val in_MatA_AfterDelay = Vec.fill(cfg.in_MatA_row_num) {SInt_withFinalMark(cfg.in_MatA_element_Width)}
  val in_MatB_AfterDelay = Vec.fill(cfg.in_MatB_col_num) {SInt_withFinalMark(cfg.in_MatB_element_Width)}
  val in_OpMode_AfterDelay = Vec.fill(cfg.in_MatB_col_num)(new OpMode_TypeDef(cfg))


  val in_MatA_BeforeDelay = Vec.fill(cfg.in_MatA_row_num) {SInt_withFinalMark(cfg.in_MatA_element_Width)}
  val in_MatB_BeforeDelay = Vec.fill(cfg.in_MatB_col_num) {SInt_withFinalMark(cfg.in_MatB_element_Width)}
  val in_OpMode_BeforeDelay = Vec.fill(cfg.in_MatB_col_num)(new OpMode_TypeDef(cfg))

  for (row_index <- 0 until cfg.in_MatA_row_num){
    in_MatA_BeforeDelay(row_index).data := io.in_Mats.payload.A(row_index).data
    in_MatA_BeforeDelay(row_index).Final := io.in_Mats.payload.A(row_index).Final
  }
  for (col_index <- 0 until cfg.in_MatB_col_num){
    in_MatB_BeforeDelay(col_index).data := io.in_Mats.payload.B(col_index).data
    in_MatB_BeforeDelay(col_index).Final := io.in_Mats.payload.B(col_index).Final
  }

  //锁存io.in_Mats.payload.mode，以实现只有第一个有效数据的OpMode是有效的
  val mode_reg = Reg(OpMode_TypeDef(cfg)) init init_OpMode(cfg)
  val latched = Reg(Bool()) init False
  when(io.in_Mats.fire && !latched){
    //当输入有效且未锁存时，锁存io.in_Mats.payload.OpMode
    mode_reg := io.in_Mats.OpMode
    latched := True
  }
  when(io.in_Mats.fire && io.in_Mats.payload.A(0).Final){
    //当其中一个矩阵的第一行数据的Final为True时，表示输入结束，复位latched。下一个周期会重新锁存一个OpMode
    latched := False
  }
  val latched_mode = OpMode_TypeDef(cfg)//定义连到后续模块的OpMode连线
  latched_mode := Mux(!latched,io.in_Mats.OpMode,mode_reg)

  for (col_index <- 0 until cfg.in_MatB_col_num){
    in_OpMode_BeforeDelay(col_index).post_Shift := io.in_Mats.payload.OpMode.post_Shift
    if(enableElementWise){in_OpMode_BeforeDelay(col_index).MatrixOperation := latched_mode.MatrixOperation}
    if(enableTranspose){in_OpMode_BeforeDelay(col_index).do_PostTranspose := io.in_Mats.payload.OpMode.do_PostTranspose}
  }
  //设置SInt_withFinalMark类型寄存器的初始值
  val init_SInt_withFinalMark_MatA = SInt_withFinalMark(cfg.in_MatA_element_Width)
  init_SInt_withFinalMark_MatA.data := 0
  init_SInt_withFinalMark_MatA.Final := False
  //设置SInt_withFinalMarkandMode类型寄存器的初始值
  val init_SInt_withFinalMark_MatB = SInt_withFinalMark(cfg.in_MatB_element_Width)
  init_SInt_withFinalMark_MatB.data := 0
  init_SInt_withFinalMark_MatB.Final := False

  //只有当输入有效时延迟流水线会工作否则暂停
  for (row_index <- 0 until cfg.in_MatA_row_num) {
    in_MatA_AfterDelay(row_index) := 
      Delay(
        in_MatA_BeforeDelay(row_index), 
        cycleCount = row_index, 
        when = io.in_Mats.fire, 
        init = init_SInt_withFinalMark_MatA
        )
  }
  for (col_index <- 0 until cfg.in_MatB_col_num) {
    in_MatB_AfterDelay(col_index) := 
      Delay(
        in_MatB_BeforeDelay(col_index), 
        cycleCount = col_index, 
        when = io.in_Mats.fire, 
        init = init_SInt_withFinalMark_MatB
        )
  }
  for (col_index <- 0 until cfg.in_MatB_col_num) {
    in_OpMode_AfterDelay(col_index) := 
      Delay(
        in_OpMode_BeforeDelay(col_index), 
        cycleCount = col_index, 
        when = io.in_Mats.fire, 
        init = init_OpMode(cfg)
        )
  }


  //延迟后数据与计算单元的连接
  val interconnect_outA_with_inA = Vec.fill(cfg.in_MatA_row_num)(Vec.fill(cfg.in_MatB_col_num)(new Bundle {
    val data = SInt(cfg.SystolicArray2DUnit_Cfg.inA_Width bits)
    val Final = Bool()
  }))
  val interconnect_outB_with_inB = Vec.fill(cfg.in_MatA_row_num)(Vec.fill(cfg.in_MatB_col_num)(new Bundle {
    val data = SInt(cfg.SystolicArray2DUnit_Cfg.inB_Width bits)
    val Final = Bool()
  }))
  val interconnect_outOpMode_with_inOpMode = Vec.fill(cfg.in_MatA_row_num)(Vec.fill(cfg.in_MatB_col_num)(
    new OpMode_TypeDef(cfg)
    ))
  val out_Mats_TypeDef_wire = Vec.fill(cfg.in_MatA_row_num)(
    Vec.fill(cfg.in_MatB_col_num)(SInt_withFinalMark(cfg.out_MatZ_element_Width))
  )
  val units = Array.tabulate(cfg.in_MatA_row_num, cfg.in_MatB_col_num)((row_index, col_index) => {
    if (row_index + col_index == cfg.in_MatA_row_num - 1) {// 反对角线位置例化可配置Unit
      new SystolicArray2DUnitSpecial(cfg.SystolicArray2DUnit_Cfg).asInstanceOf[SystolicArray2DUnit_io]
    } else {// 非反对角线位置例化普通Unit
      new SystolicArray2DUnit(cfg.SystolicArray2DUnit_Cfg).asInstanceOf[SystolicArray2DUnit_io]
    }
  })
  for (row_index <- 0 until cfg.in_MatA_row_num) {// 纵向连接线
    for (col_index <- 0 until cfg.in_MatB_col_num) {// 横向连接线
      //为所有单元的inA安排对应相连的输入信号
      if (col_index == 0) {// 第一层，和已经完成延迟对齐的输入相连
        units(row_index)(col_index).io.inA <> in_MatA_AfterDelay(row_index).data
        units(row_index)(col_index).io.inA_Final <> in_MatA_AfterDelay(row_index).Final
      } else {// 不是第一层，和上一层的输出相连
        units(row_index)(col_index).io.inA <> interconnect_outA_with_inA(row_index)(col_index - 1).data
        units(row_index)(col_index).io.inA_Final <> interconnect_outA_with_inA(row_index)(col_index - 1).Final
      }
      // 所有的输出先连接到interconnect_outA_with_inA线束
      interconnect_outA_with_inA(row_index)(col_index).data <> units(row_index)(col_index).io.outA
      interconnect_outA_with_inA(row_index)(col_index).Final <> units(row_index)(col_index).io.outA_Final

      //为所有单元的inB安排对应相连的输入信号
      if (row_index == 0) {// 第一层，和已经完成延迟对齐的输入相连
        units(row_index)(col_index).io.inB <> in_MatB_AfterDelay(col_index).data
        units(row_index)(col_index).io.inB_Final <> in_MatB_AfterDelay(col_index).Final
        // units(row_index)(col_index).io.inMode <> in_MatB_AfterDelay(col_index).OpMode
        // units(row_index)(col_index).io.inTranspose <> in_MatB_AfterDelay(col_index).Transpose
        // units(row_index)(col_index).io.inShift <> in_MatB_AfterDelay(col_index).Shift
      } else {// 不是第一层，和上一层的输出相连
        units(row_index)(col_index).io.inB <> interconnect_outB_with_inB(row_index - 1)(col_index).data
        units(row_index)(col_index).io.inB_Final <> interconnect_outB_with_inB(row_index - 1)(col_index).Final
        // units(row_index)(col_index).io.inMode <> interconnect_outB_with_inB(row_index - 1)(col_index).OpMode
        // units(row_index)(col_index).io.inTranspose <> interconnect_outB_with_inB(row_index - 1)(col_index).Transpose        
        // units(row_index)(col_index).io.inShift <> interconnect_outB_with_inB(row_index - 1)(col_index).Shift
      }
      // 所有的输出先连接到interconnect_outB_with_inB线束
      interconnect_outB_with_inB(row_index)(col_index).data <> units(row_index)(col_index).io.outB
      interconnect_outB_with_inB(row_index)(col_index).Final <> units(row_index)(col_index).io.outB_Final
      // interconnect_outB_with_inB(row_index)(col_index).OpMode <> units(row_index)(col_index).io.outMode
      // interconnect_outB_with_inB(row_index)(col_index).Transpose <> units(row_index)(col_index).io.outTranspose
      // interconnect_outB_with_inB(row_index)(col_index).Shift <> units(row_index)(col_index).io.outShift

      //为所有单元的inB安排对应相连的输入信号
      if (row_index == 0) {// 第一层，和已经完成延迟对齐的输入相连
        units(row_index)(col_index).io.inMode <> in_OpMode_AfterDelay(col_index).MatrixOperation
        units(row_index)(col_index).io.inTranspose <> in_OpMode_AfterDelay(col_index).do_PostTranspose
        units(row_index)(col_index).io.inShift <> in_OpMode_AfterDelay(col_index).post_Shift
      } else {// 不是第一层，和上一层的输出相连
        units(row_index)(col_index).io.inMode <> interconnect_outOpMode_with_inOpMode(row_index - 1)(col_index).MatrixOperation
        units(row_index)(col_index).io.inTranspose <> interconnect_outOpMode_with_inOpMode(row_index - 1)(col_index).do_PostTranspose
        units(row_index)(col_index).io.inShift <> interconnect_outOpMode_with_inOpMode(row_index - 1)(col_index).post_Shift
      } 
      // 所有的输出先连接到interconnect_outOpMode_with_inOpMode线束
      interconnect_outOpMode_with_inOpMode(row_index)(col_index).MatrixOperation <> units(row_index)(col_index).io.outMode
      interconnect_outOpMode_with_inOpMode(row_index)(col_index).do_PostTranspose <> units(row_index)(col_index).io.outTranspose
      interconnect_outOpMode_with_inOpMode(row_index)(col_index).post_Shift <> units(row_index)(col_index).io.outShift
      
      //units(row_index)(col_index).io.Go <> shift_wire_in_fire(row_index + col_index)//一旦数据无效，单元不会计算继续向后传递
      units(row_index)(col_index).io.Go <> (io.in_Mats.fire)

      units(row_index)(col_index).io.outZ <> out_Mats_TypeDef_wire(row_index)(col_index).data
      out_Mats_TypeDef_wire(row_index)(col_index).Final := units(row_index)(col_index).io.outA_Final && units(row_index)(col_index).io.outB_Final
    }
  }
  //输出缓存模块
  case class out_MatZ_buffer(cfg: SystolicArray2D_Config) extends Bundle {
    val data = Vec.fill(cfg.in_MatA_row_num)(Vec.fill(cfg.in_MatB_col_num)(Reg(SInt(cfg.out_MatZ_element_Width bits)) init 0))
    val Valid = Reg(Bool()) init False // 数据就绪标志 | Data ready flag
    val OpMode = Reg(OpMode_TypeDef(cfg)) init init_OpMode(cfg) // 操作模式 | Operation OpMode
  }
  val buffer_array = Vec.fill(cfg.out_MatZ_buffer_num)(out_MatZ_buffer(cfg))
  val Unit2buffer_ptr = Vec.fill(cfg.diag_num - 1)(Reg(UInt(log2Up(cfg.out_MatZ_buffer_num) bits)) init 0)
  val buffer_array_output_ptr = Reg(UInt(log2Up(cfg.out_MatZ_buffer_num) bits)) init 1
  val sub_ptr = Reg(UInt(log2Up(cfg.in_MatA_row_num) bits)) init 0//在element-wise模式下，表示一块buffer中的子列

  //开始往一个buffer中写入，它将会被写入，直到读出后释放
  val buffer_lock_status = Vec.fill(cfg.out_MatZ_buffer_num)(Reg(Bool()) init False)//未被读出的buffer会标记
  val buffer_array_full = Bool()
  buffer_array_full := {buffer_lock_status.fold(True){(acc, status) => acc && status}}//指示buffer是否全部被锁定，用于停止计算与接收输入

  //val delay_transpose = Delay(io.in_Mats.payload.OpMode.do_PostTranspose, cycleCount = cfg.in_MatA_row_num + cfg.diag_num - 1 ,when = io.in_Mats.fire,init = False)
  when(io.in_Mats.fire){
    when(units(0)(0).io.inA_Final){
      when(Unit2buffer_ptr(0) + 1 < cfg.out_MatZ_buffer_num){
        Unit2buffer_ptr(0) := Unit2buffer_ptr(0) + 1
      }.otherwise{
        Unit2buffer_ptr(0) := 0
      }
    }
    for (i <- (1 until cfg.diag_num-1).reverse){// i<-{cfg.diag_num-1,cfg.diag_num-2,...,1}
      Unit2buffer_ptr(i) := Unit2buffer_ptr(i - 1)
    }
    when((units(0)(cfg.in_MatB_col_num-1).io.outMode =/= MatrixOperation_TypeDef.MatMul).asBits.asBool ){//&& RegNext(units(0)(cfg.in_MatB_col_num-1).io.Go)
      sub_ptr := sub_ptr + U(1)
    }.elsewhen(sub_ptr === cfg.in_MatA_row_num-1){//in_MatB_AfterDelay(cfg.in_MatB_col_num).Final，要始终填满一个buffer块，填不满需要补零
      sub_ptr := 0
    }

    //mat_mul缓存逻辑
    for (row_index <- 0 until cfg.in_MatA_row_num) {
      for (col_index <- 0 until cfg.in_MatB_col_num) {
        when(units(row_index)(col_index).io.outMode === MatrixOperation_TypeDef.MatMul){//mat_mul
          when(out_Mats_TypeDef_wire(row_index)(col_index).Final){//final触发意味着需要输出&& RegNext(units(row_index)(col_index).io.Go)
            buffer_array(Unit2buffer_ptr(row_index+col_index)).data(row_index)(col_index) := out_Mats_TypeDef_wire(row_index)(col_index).data
            buffer_array(Unit2buffer_ptr(row_index+col_index)).OpMode.MatrixOperation := units(row_index)(col_index).io.outMode
            buffer_array(Unit2buffer_ptr(row_index+col_index)).OpMode.do_PostTranspose := units(row_index)(col_index).io.outTranspose//Delay(in_MatB_AfterDelay(row_index).Transpose,cfg.diag_num)
            buffer_array(Unit2buffer_ptr(row_index+col_index)).OpMode.post_Shift := units(row_index)(col_index).io.outShift

            buffer_lock_status(Unit2buffer_ptr(row_index+col_index)) := True
          }
        }
      }
    }
    when(units(cfg.in_MatA_row_num-1)(cfg.in_MatB_col_num-1).io.outMode === MatrixOperation_TypeDef.MatMul){
      when(units(cfg.in_MatA_row_num-1)(cfg.in_MatB_col_num-1).io.outB_Final){
        buffer_array(Unit2buffer_ptr(cfg.diag_num-2)).Valid := True
      }
    }

    //elementwise缓存逻辑
    for (row_index <- 0 until cfg.in_MatA_row_num) {
      for (col_index <- 0 until cfg.in_MatB_col_num) {
        when((units(row_index)(col_index).io.outMode =/= MatrixOperation_TypeDef.MatMul)) {//elementwise
          when(sub_ptr <= cfg.in_MatA_row_num-1) {//&& RegNext(units(0)(cfg.in_MatB_col_num-1).io.Go)
            if(row_index + col_index == cfg.in_MatA_row_num - 1){//在反对角线上
              buffer_array(Unit2buffer_ptr(sub_ptr.resized)).data(row_index)(sub_ptr) := out_Mats_TypeDef_wire(row_index)(col_index).data
              buffer_array(Unit2buffer_ptr(sub_ptr.resized)).OpMode.MatrixOperation := units(row_index)(col_index).io.outMode
              if(enableTranspose) {
                buffer_array(Unit2buffer_ptr(sub_ptr.resized)).OpMode.do_PostTranspose := units(row_index)(col_index).io.outTranspose
              }
              buffer_array(Unit2buffer_ptr(sub_ptr.resized)).OpMode.post_Shift := units(row_index)(col_index).io.outShift
            }
          }
        }
      }
    }
    when((units(0)(cfg.in_MatB_col_num-1).io.outMode =/= MatrixOperation_TypeDef.MatMul)){
      when(units(0)(cfg.in_MatB_col_num-1).io.outB_Final) {//elementwise,拉了final表示输出完成了
        buffer_array(Unit2buffer_ptr(cfg.in_MatB_col_num-1)).Valid := True
      }
      buffer_lock_status(Unit2buffer_ptr(0)) := True
    }

  }
  //输出逻辑
  when(io.out_Mats.fire) {
    // 当索引为(in_MatA_row_num-1,in_MatB_col_num-1)的单元输出结果时，说明已经完成了一个矩阵的计算，此时，对应的buffer的Valid应该被置位
    when(buffer_array_output_ptr===cfg.out_MatZ_buffer_num-1){
      buffer_array_output_ptr := 0
    }.otherwise{
      buffer_array_output_ptr := buffer_array_output_ptr + 1//Unit2buffer_ptr(cfg.diag_num - 1)
    }
  }

  when(io.out_Mats.fire) {
    buffer_array(buffer_array_output_ptr).Valid := False
    buffer_lock_status(buffer_array_output_ptr) := False
  }
  //io.out_Mats.payload.Z <> buffer_array(buffer_array_output_ptr).data
  if(cfg.Enable_Transpose_logic){
    when(buffer_array(buffer_array_output_ptr).OpMode.do_PostTranspose){
      for (row_index <- 0 until cfg.in_MatA_row_num) {
        for (col_index <- 0 until cfg.in_MatB_col_num) {
          io.out_Mats.payload.Z(row_index)(col_index) := buffer_array(buffer_array_output_ptr).data(col_index)(row_index)
        }
      }
    }.otherwise{
      io.out_Mats.payload.Z <> buffer_array(buffer_array_output_ptr).data
    }
  }
  io.out_Mats.valid := buffer_array(buffer_array_output_ptr).Valid //&& (io.in_Mats.valid)
  io.in_Mats.ready := !buffer_array_full
}


object SystolicArray2D_Verilog extends App {
  val testLength = 16
  val cfg = SystolicArray2D_Config(
    in_Length_Max = testLength,
    in_Length_Min = 4,
    in_MatA_row_num = 4,
    in_MatB_col_num = 4
  )

  val FileDir = "rtl/SystolicArray2D/verilog"
  import java.io.File
  new File(FileDir).mkdirs()

  SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
    removePruned=true
  ).generateVerilog(new SystolicArray2D(cfg)).printPruned()

  //tools.HDElkDiagramGen(SpinalVerilog(new SystolicArray2D(cfg)))//生成电路图
}
package MatrixComputeUnit.SystolicArray2D
import spinal.core._
import spinal.lib._
import spinal.lib.tools
import spinal.core
import scala.math
import MatrixComputeUnit.SystolicArray2D._
import Interface.MatrixOperation_TypeDef
import scala.collection.mutable.ArrayBuffer
import Util._
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
  val ID_Width:Int = log2Up(out_MatZ_buffer_num)+1

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
  val SystolicArray2DUnit_Cfg = SystolicArray2DUnit_Config(
    in_Length = in_Length_Max,
    inA_Width = in_MatA_element_Width,
    inB_Width = in_MatB_element_Width,
    outZ_Width = out_MatZ_element_Width,
    ID_Width = ID_Width
  )

}
/**
  * SInt_withFinalMark
  * 包含数据和Final标志的有符号整数打包类型。
  * Signed integer bundle with data and Final flag.
  */
case class SInt_withFinalMark(element_Width: Int) extends Bundle {
  val data = SInt(element_Width bits)
  val Final = Bool()
  implicit def toFragment: Fragment[SInt] = {
    val f = Fragment(SInt(element_Width bits))
    f.fragment := this.data
    f.last := this.Final
    f
  }
  // 隐式转换：Fragment[SInt] => SInt_withFinalMark
  implicit def fromFragment(f: Fragment[SInt]): SInt_withFinalMark = {
    this.data := f.fragment
    this.Final := f.last
    this
  }
}
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
  if (cfg.Enable_ElementWise_logic)
  {
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
  }}
  def to_Ctrl:SystolicArray2DUnit_Control_TypeDef = {
    val Ctrl=SystolicArray2DUnit_Control_TypeDef(this.cfg.SystolicArray2DUnit_Cfg.outZ_Width)
    if (cfg.Enable_ElementWise_logic){Ctrl.Mode:=this.MatrixOperation}
    Ctrl.Shift:=this.post_Shift
    if(cfg.Enable_Transpose_logic){Ctrl.Transpose:=this.do_PostTranspose}
    Ctrl
  }
  def :=(that: SystolicArray2DUnit_Control_TypeDef): Unit ={
    if (cfg.Enable_ElementWise_logic){this.MatrixOperation:=that.Mode}
    this.post_Shift:=that.Shift
    if(cfg.Enable_Transpose_logic){this.do_PostTranspose:=that.Transpose}
  }
  
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
  class in_Mats_TypeDef(cfg: SystolicArray2D_Config) extends Bundle {
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
    def Ctrl:SystolicArray2DUnit_Control_TypeDef = this.OpMode.to_Ctrl
    
  }

  def in_Mats_Bundle():in_Mats_TypeDef={new in_Mats_TypeDef(cfg)}
  class out_Mats_TypeDef(cfg: SystolicArray2D_Config) extends Bundle {
    val Z = Vec.fill(cfg.out_MatZ_row_num)(Vec.fill(cfg.out_MatZ_col_num)(SInt(cfg.out_MatZ_element_Width bits)))
  }
  def out_Mats_Bundle():out_Mats_TypeDef={new out_Mats_TypeDef(cfg)}

  val io = new Bundle {
    val in_Mats = slave(Stream(in_Mats_Bundle()))//.addAttribute("DONT_TOUCH = \"TRUE\"")
    val out_Mats = master(Stream(out_Mats_Bundle()))//.addAttribute("DONT_TOUCH = \"TRUE\"")
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


  // 存储每一级的interconnect流
  case class interconnect_TypeDef(cfg:SystolicArray2D_Config) extends in_Mats_TypeDef(cfg) {
    val ID=UInt(cfg.ID_Width bits)
  }
  val generate_next_ID=Reg(Bool()) init True
  val gray_counter = GrayCounter(cfg.ID_Width,
  enable=io.in_Mats.fire&&io.in_Mats.payload.A(0).Final)
  
  val interconnects = Vec.fill(cfg.in_MatA_row_num + cfg.in_MatB_col_num)(Stream(interconnect_TypeDef(cfg)))
  // 第0级：输入直接连接到第一个interconnect
  interconnects(0) << io.in_Mats.map{payload=>
    val new_payload=interconnect_TypeDef(cfg)
    new_payload.A:=payload.A
    new_payload.B:=payload.B
    new_payload.OpMode:=latched_mode
    new_payload.ID:=gray_counter
    new_payload
  }
  // 函数：计算某个stage中的计算单元坐标
  def getComputeUnitIndices(stage: Int): Array[(Int, Int)] = {
    val rowStart = math.max(0, stage - cfg.in_MatB_col_num + 1)
    val rowEnd = math.min(cfg.in_MatA_row_num - 1, stage)
  
    (rowStart to rowEnd)
      .map(row => (row, stage - row))
      .filter { case (_, col) => col >= 0 && col < cfg.in_MatB_col_num }
      .toArray
  }
  
  def getPassthroughIndices(stage: Int): (Array[Int], Array[Int]) = {
    // 获取当前stage使用的计算单元
    val computeUnits = getComputeUnitIndices(stage)
    
    // 提取使用的行和列
    val usedRows = computeUnits.map(_._1).toSet
    val usedCols = computeUnits.map(_._2).toSet
    
    // 计算未使用的行和列
    val unusedRows = (0 until cfg.in_MatA_row_num).filterNot(usedRows.contains)
    val unusedCols = (0 until cfg.in_MatB_col_num).filterNot(usedCols.contains)
    
    (unusedRows.toArray, unusedCols.toArray)
  }
  // 主循环：为每一级流水线创建计算逻辑和interconnect
  // 总的流水线级数
  val totalStages = cfg.in_MatA_row_num + cfg.in_MatB_col_num - 1
  val ResultStreams = Vec.fill(cfg.out_MatZ_row_num)(Vec.fill(cfg.out_MatZ_col_num)(Stream(SystolicArray2DUnit_ResultBundle_TypeDef(cfg.SystolicArray2DUnit_Cfg))))
  // 提前生成所有计算单元
  val computeUnits = Array.tabulate(cfg.out_MatZ_row_num, cfg.out_MatZ_col_num) { (row, col) =>
  val unit =
    if (enableElementWise && (row + col == cfg.in_MatA_row_num - 1))
      new SystolicArray2DUnitSpecial(cfg.SystolicArray2DUnit_Cfg)
    else
      new SystolicArray2DUnit(cfg.SystolicArray2DUnit_Cfg)
  unit.io
}
  for (stage <- 0 to totalStages) {
    println("--------------------------------------------------")
    println(s"Generating Stage: ${stage}/${totalStages}")
    val computeUnitIndices = getComputeUnitIndices(stage)
    println(s"  Compute Units: ${computeUnitIndices.map(p => s"(${p._1}, ${p._2})").mkString(", ")}")
    val (passthroughAIndices, passthroughBIndices) = getPassthroughIndices(stage)//index of A and B
    // 打印直通索引
  println(s"  Passthrough A: ${passthroughAIndices.mkString(", ")}")
  println(s"  Passthrough B: ${passthroughBIndices.mkString(", ")}")
    val validPairs_Count = computeUnitIndices.length
    val passthroughA_Count = passthroughAIndices.length 
    val passthroughB_Count= passthroughBIndices.length
    // 当前级的总信号数量
    val totalSignals = validPairs_Count+passthroughA_Count+passthroughB_Count
    if (validPairs_Count > 0) {
      // 将当前interconnect分叉成多个流
      val forkedStreams = StreamFork(interconnects(stage), totalSignals, synchronous = false)
      // 存储计算单元的输入和输出
      val upStream = Vec.fill(validPairs_Count)(
        Stream(SystolicArray2DUnit_StreamingBundle_TypeDef(cfg.SystolicArray2DUnit_Cfg)))
      val downStream = Vec.fill(validPairs_Count)(
        Stream(SystolicArray2DUnit_StreamingBundle_TypeDef(cfg.SystolicArray2DUnit_Cfg)))
      val passthroughA_Stream = Vec.fill(passthroughA_Count)(
        Stream(Fragment(SInt(cfg.in_MatA_element_Width bits))))
      val passthroughB_Stream = Vec.fill(passthroughB_Count)(
          Stream(Fragment(SInt(cfg.in_MatB_element_Width bits))))
      // 为有效的计算对分配输入流
      for (i <- 0 until validPairs_Count) {
        val (row, col) = computeUnitIndices(i)
        upStream(i) << forkedStreams(i).map{payload=>
          val new_payload=SystolicArray2DUnit_StreamingBundle_TypeDef(cfg.SystolicArray2DUnit_Cfg)
          new_payload.A:=payload.A(row).toFragment
          new_payload.B:=payload.B(col).toFragment
          new_payload.Ctrl:=payload.Ctrl
          new_payload.ID:=payload.ID
          new_payload
        }
        computeUnits(row)(col).upStream<<upStream(i)
        computeUnits(row)(col).downStream>>downStream(i)
        computeUnits(row)(col).result>>ResultStreams(row)(col)
      }
      val joined_downstreams=if(validPairs_Count>1)StreamJoin(downStream)else downStream(0)

      // 处理直通的行数据
      val joined_passthroughA_Stream = if(passthroughA_Count > 0) {
        for (i <- 0 until passthroughA_Count) {
          val row = passthroughAIndices(i)
          passthroughA_Stream(i)<-/<forkedStreams(validPairs_Count + i).map{payload =>
            val new_payload=Fragment(SInt(cfg.in_MatA_element_Width bits))
            new_payload:=payload.A(row).toFragment
            new_payload
          }
        }
        if(passthroughA_Count > 1) StreamJoin(passthroughA_Stream) else passthroughA_Stream(0).toEvent()
      } else null
      // 处理直通的列数据  
      val joined_passthroughB_Stream = if(passthroughB_Count > 0) {
        for (i <- 0 until passthroughB_Count) {
          val col = passthroughBIndices(i)
          passthroughB_Stream(i)<-/<forkedStreams(validPairs_Count + passthroughA_Count + i).map{payload =>
            val new_payload=Fragment(SInt(cfg.in_MatB_element_Width bits))
            new_payload:=payload.B(col).toFragment
            new_payload
          }
        }
        if(passthroughB_Count > 1) StreamJoin(passthroughB_Stream) else passthroughB_Stream(0).toEvent()
      } else null
      val activeStreams = Seq(
        joined_downstreams,
        if (passthroughA_Count > 0) joined_passthroughA_Stream else null,
        if (passthroughB_Count > 0) joined_passthroughB_Stream else null
      ).filter(_ != null)
      val joined_All=Stream(interconnect_TypeDef(cfg))
      joined_All.arbitrationFrom(
        if (activeStreams.size > 1) StreamJoin(activeStreams) 
        else activeStreams.head)
      for (i <- 0 until validPairs_Count) {
        val (row, col) = computeUnitIndices(i)
        joined_All.payload.A(row).fromFragment(downStream(i).payload.A)
        joined_All.payload.B(col).fromFragment(downStream(i).payload.B)
      }
      for (i <- 0 until passthroughA_Count) {
        val row = passthroughAIndices(i)
        joined_All.payload.A(row).fromFragment(passthroughA_Stream(i).payload)
      }
      for (i <- 0 until passthroughB_Count) {
        val col = passthroughBIndices(i)
        joined_All.payload.B(col).fromFragment(passthroughB_Stream(i).payload)
      }
      //唯一信号直接取0号位的数值
      joined_All.payload.OpMode:=downStream(0).payload.Ctrl
      joined_All.payload.ID:=downStream(0).payload.ID
      if(stage+1<totalStages){
        interconnects(stage+1)<<joined_All
      }else{
        joined_All.ready:=True
      }
    }
  }
  // 定义输出缓存状态
  object out_MatZ_buffer_Status extends SpinalEnum(defaultEncoding = binarySequential) {
    val  Idle,Matmul_Collecting,Element_Collecting,Ready_to_Output= newElement() // 定义当前输出包的out_MatZ_buffer的状态
  }
  //输出缓存模块
  case class out_MatZ_buffer(cfg: SystolicArray2D_Config) extends Bundle {
    val data = Vec.fill(cfg.in_MatA_row_num)(Vec.fill(cfg.in_MatB_col_num)(Reg(Flow(SInt(cfg.out_MatZ_element_Width bits)))))
    for(i <- 0 until cfg.in_MatA_row_num){
      for(j <- 0 until cfg.in_MatB_col_num){
        data(i)(j).payload init (S(0))
        data(i)(j).valid init (False)
      }
    }
    val diag_valid=Vec.fill(cfg.diag_num)(Bool())//和为i的对角线全部valid的标记，用于判断是否开始接受下个对角线的数据
    for(i <- 0 until cfg.diag_num){
      diag_valid(i):=True
      for (row_index <- 0 until cfg.in_MatA_row_num) {
        for (col_index <- 0 until cfg.in_MatB_col_num) {
          if(row_index + col_index == i){
            diag_valid(i).clearWhen(!this.data(row_index)(col_index).valid)
          }
        }
      }
    }
    val all_valid=Bool()
    all_valid:=True
    for(i<- 0 until cfg.in_MatA_row_num){
      for(j<-0 until cfg.in_MatB_col_num){
        all_valid.clearWhen(!this.data(i)(j).valid)
      }
    }
    val OpMode = Reg(OpMode_TypeDef(cfg)) init init_OpMode(cfg) // 操作模式 | Operation OpMode
    val ID = Reg(UInt(cfg.ID_Width bits))
    val Status = Reg(out_MatZ_buffer_Status()) init out_MatZ_buffer_Status.Idle
    when(this.Status===out_MatZ_buffer_Status.Matmul_Collecting
      ||this.Status===out_MatZ_buffer_Status.Element_Collecting){
    when(all_valid){
      this.Status:=out_MatZ_buffer_Status.Ready_to_Output
    }}
    def is_Valid:Bool={this.Status===out_MatZ_buffer_Status.Ready_to_Output}
    def is_Busy:Bool={this.Status=/=out_MatZ_buffer_Status.Idle}
  }
  val buffer_array = Vec.fill(cfg.out_MatZ_buffer_num)(out_MatZ_buffer(cfg))

  val Matmul_Unit2buffer_ptr = Vec.fill(cfg.diag_num - 1)((Reg(Flow(UInt(log2Up(cfg.out_MatZ_buffer_num) bits)))))//在Matmul模式下，Matmul_Unit2buffer_ptr(i)代表行号+列号=i所对应的计算单元分配到的缓冲区指针
  for (i <- 0 until cfg.diag_num-1 ) {
    Matmul_Unit2buffer_ptr(i).payload init (U(0))
    Matmul_Unit2buffer_ptr(i).valid init (False)
  }
  val Element_Unit2buffer_ptr = enableElementWise generate 
                                (Reg(Flow(UInt(log2Up(cfg.out_MatZ_buffer_num) bits)))
                                )//在element-wise模式，代表了正在输出的反对角线上的计算单元输出到的缓冲区指针
  if(enableElementWise){
    Element_Unit2buffer_ptr.payload init (U(0))
    Element_Unit2buffer_ptr.valid init (False)
  }
  val Element_col_ptr = enableElementWise generate                        
                          Reg(UInt(log2Up(cfg.in_MatB_col_num) bits)) init 0
                        //在element-wise模式下，代表了正在输出的反对角线的计算单元输出到缓冲区中的第几列
  

    // --- 指针管理逻辑 ---
    //新buffer片分配机制
    // 跟踪上一次分配的缓冲区指针
    val last_allocated_ptr = Reg(UInt(log2Up(cfg.out_MatZ_buffer_num) bits)) init 0
    // 组合逻辑，用于从上一次分配的指针之后开始环形查找
    val next_idle_buffer_ptr = UInt(log2Up(cfg.out_MatZ_buffer_num) bits)
    val idle_buffer_found = Bool()
    next_idle_buffer_ptr:=U(0)
    idle_buffer_found:=False
    // 环形查找：从 last_allocated_ptr 的下一个位置开始
    val start_search_ptr = (last_allocated_ptr + U(1)) % cfg.out_MatZ_buffer_num
    // 构建优先编码器
    for (i <- 0 until cfg.out_MatZ_buffer_num) {
        when((buffer_array((start_search_ptr + U(i)) % cfg.out_MatZ_buffer_num).Status === out_MatZ_buffer_Status.Idle)) {
            next_idle_buffer_ptr := (start_search_ptr + U(i)) % cfg.out_MatZ_buffer_num
            idle_buffer_found := True
        }
    }
    //matmul模式下输入指针系统
    val request_Matmul_allocation = 
      ((ResultStreams(0)(0).valid&& 
        (ResultStreams(0)(0).payload.Ctrl.Mode === MatrixOperation_TypeDef.MatMul)))
    val element_allocation_grant = enableElementWise generate RegInit(True) init (True)//当buffer片已经分配给Element_Unit2buffer_ptr，阻止新的request_Element_allocation生成
    val request_Element_allocation = enableElementWise generate (
      (element_allocation_grant && // 只有在获得“许可”时才申请
      (ResultStreams(0)(cfg.in_MatB_col_num - 1).payload.Ctrl.Mode =/= MatrixOperation_TypeDef.MatMul)) && 
      (ResultStreams(0)(cfg.in_MatB_col_num - 1).valid)
      )

    // 分配逻辑
    when(request_Matmul_allocation) {
      when(idle_buffer_found&&Matmul_Unit2buffer_ptr(0).valid===False) 
      {// 找到空闲缓冲区，进行分配
            Matmul_Unit2buffer_ptr(0).payload := next_idle_buffer_ptr
            Matmul_Unit2buffer_ptr(0).valid := True
            buffer_array(next_idle_buffer_ptr).Status := out_MatZ_buffer_Status.Matmul_Collecting
            last_allocated_ptr := next_idle_buffer_ptr
      }
    } 
    
    // Matmul_Unit2buffer_ptr对角线指针传递：将前一级指针值向后传递（从高位索引向低位索引传递）
    for (i <- (0 until cfg.diag_num-1).reverse){// i<-{cfg.diag_num-1,cfg.diag_num-2,...,1}
      
        when(Matmul_Unit2buffer_ptr(i).valid&&buffer_array(Matmul_Unit2buffer_ptr(i).payload).diag_valid(i)
          &&(buffer_array(Matmul_Unit2buffer_ptr(i).payload).Status === out_MatZ_buffer_Status.Matmul_Collecting)){
            if(i+1<cfg.diag_num-1)
            {Matmul_Unit2buffer_ptr(i+1) := Matmul_Unit2buffer_ptr(i)}
            Matmul_Unit2buffer_ptr(i).valid:=False
        }
        
    }

    //mat_mul缓存逻辑
    //对于每个计算单元，当其输出结果且工作状态是mat_mul时，更新buffer的对应位置
    for (row_index <- 0 until cfg.in_MatA_row_num) {
      for (col_index <- 0 until cfg.in_MatB_col_num) {
        ResultStreams(row_index)(col_index).ready:=False//默认状态
        when(ResultStreams(row_index)(col_index).valid&&
        ResultStreams(row_index)(col_index).payload.Ctrl.Mode === MatrixOperation_TypeDef.MatMul&&
        Matmul_Unit2buffer_ptr(row_index+col_index).valid&&buffer_array(Matmul_Unit2buffer_ptr(row_index+col_index).payload).all_valid===False)
        {//判定工作模式为mat_mul
          ResultStreams(row_index)(col_index).ready:=Matmul_Unit2buffer_ptr(row_index+col_index).valid
          when(Matmul_Unit2buffer_ptr(row_index+col_index).valid)
          {
            when(ResultStreams(row_index)(col_index).fire)
            {
              buffer_array(Matmul_Unit2buffer_ptr(row_index+col_index).payload).data(row_index)(col_index).payload := ResultStreams(row_index)(col_index).payload.Z
              buffer_array(Matmul_Unit2buffer_ptr(row_index+col_index).payload).data(row_index)(col_index).valid:=True
              buffer_array(Matmul_Unit2buffer_ptr(row_index+col_index).payload).OpMode := ResultStreams(row_index)(col_index).payload.Ctrl
              buffer_array(Matmul_Unit2buffer_ptr(row_index+col_index).payload).ID := ResultStreams(row_index)(col_index).payload.ID
            }
          }
        }
      }
    }

    if(enableElementWise){
      //分配逻辑
      when(request_Element_allocation&&request_Matmul_allocation===False) {
        when(idle_buffer_found) {
          Element_Unit2buffer_ptr.payload := next_idle_buffer_ptr
          Element_Unit2buffer_ptr.valid := True
          buffer_array(next_idle_buffer_ptr).Status := out_MatZ_buffer_Status.Element_Collecting  
          last_allocated_ptr := next_idle_buffer_ptr
          element_allocation_grant := False
        } otherwise {
          // 没有找到空闲缓冲区，
          Element_Unit2buffer_ptr.payload:=0
          Element_Unit2buffer_ptr.valid := False
          
        }
      }
      //elementwise缓存逻辑
      //对于反对角线上的计算单元
      for (row_index <- 0 until cfg.in_MatA_row_num) {
        for (col_index <- 0 until cfg.in_MatB_col_num) {
          if(row_index + col_index == cfg.in_MatA_row_num - 1){//在反对角线上
          //输出结果且工作状态不是mat_mul时（也就是说是按元素逻辑），按照Element_col_ptr更新buffer的对应位置
          when((ResultStreams(row_index)(col_index).valid&&
          ResultStreams(row_index)(col_index).payload.Ctrl.Mode =/= MatrixOperation_TypeDef.MatMul&&
          buffer_array(Element_Unit2buffer_ptr.payload.resized).all_valid===False)) {
            ResultStreams(row_index)(col_index).ready:=Element_Unit2buffer_ptr.valid
            when(Element_Unit2buffer_ptr.valid){
              when(ResultStreams(row_index)(col_index).fire){
                //当反对角线上的计算单元输出结果且工作模式不为matmul时更新buffer的行指针。
                when(Element_col_ptr < cfg.in_MatA_row_num-1){
                  Element_col_ptr := Element_col_ptr + U(1)
                }
                when(Element_col_ptr <= cfg.in_MatA_row_num-1) {//&& RegNext(units(0)(cfg.in_MatB_col_num-1).io.Go)
                    buffer_array(Element_Unit2buffer_ptr.payload.resized).data(row_index)(Element_col_ptr).payload := ResultStreams(row_index)(col_index).payload.Z
                    buffer_array(Element_Unit2buffer_ptr.payload.resized).data(row_index)(Element_col_ptr).valid := True
                    buffer_array(Element_Unit2buffer_ptr.payload.resized).OpMode := ResultStreams(row_index)(col_index).payload.Ctrl
                    buffer_array(Element_Unit2buffer_ptr.payload.resized).ID := ResultStreams(row_index)(col_index).payload.ID
                  }
                }
              }
            }
          }
        }
      }
    }
  //buffer转stream输出逻辑
  // 跟踪上一次输出的缓冲区指针
  val last_output_ptr = Reg(UInt(log2Up(cfg.out_MatZ_buffer_num) bits)) init 0
  // 组合逻辑，用于从上一次输出的指针之后开始环形查找
  val next_valid_buffer_ptr = Reg(UInt(log2Up(cfg.out_MatZ_buffer_num) bits)) init 0
  val valid_buffer_found = Reg(Bool()) init False
  // 环形查找：从 last_output_ptr 的下一个位置开始
  val start_search_ptr_output = (last_output_ptr + U(1)) % cfg.out_MatZ_buffer_num
  // 寄存器用于在找到有效缓冲区后锁存其指针
  val current_output_ptr = (UInt(log2Up(cfg.out_MatZ_buffer_num) bits))
  val current_output_ID = GrayCounter(cfg.ID_Width,
  enable=io.out_Mats.fire)
  // 构建优先编码器
  for (i <- 0 until cfg.out_MatZ_buffer_num) {

    when(buffer_array((start_search_ptr_output + U(i)) % cfg.out_MatZ_buffer_num).Status === out_MatZ_buffer_Status.Ready_to_Output
    &&(buffer_array((start_search_ptr_output + U(i)) % cfg.out_MatZ_buffer_num).ID ===current_output_ID)) {
        next_valid_buffer_ptr := (start_search_ptr_output + U(i)) % cfg.out_MatZ_buffer_num
        valid_buffer_found := True
    }
  }

  
  current_output_ptr := next_valid_buffer_ptr
  
  // 输出流的控制逻辑
  io.out_Mats.valid := valid_buffer_found
  if(cfg.Enable_Transpose_logic){
    when(buffer_array(current_output_ptr).OpMode.do_PostTranspose){
      for (row_index <- 0 until cfg.in_MatA_row_num) {
        for (col_index <- 0 until cfg.in_MatB_col_num) {
          io.out_Mats.payload.Z(row_index)(col_index) := buffer_array(current_output_ptr).data(col_index)(row_index).payload
        }
      }
    }.otherwise{
      for (row_index <- 0 until cfg.in_MatA_row_num) {
        for (col_index <- 0 until cfg.in_MatB_col_num) {
          io.out_Mats.payload.Z(row_index)(col_index) := buffer_array(current_output_ptr).data(row_index)(col_index).payload
        }
      }
    }
  }else{
    for (row_index <- 0 until cfg.in_MatA_row_num) {
      for (col_index <- 0 until cfg.in_MatB_col_num) {
        io.out_Mats.payload.Z(row_index)(col_index) := buffer_array(current_output_ptr).data(row_index)(col_index).payload
      }
    }
  }
  // 实际输出操作：当io.out_Mats.fire时
  when(io.out_Mats.fire) {
    // 将已输出的缓冲区状态重置为空闲
    buffer_array(current_output_ptr).Status := out_MatZ_buffer_Status.Idle
    valid_buffer_found:=False
    // 更新last_output_ptr以指向刚刚输出的缓冲区，为下一次查找提供起点
    last_output_ptr := current_output_ptr
    
    
    // 释放指针的valid标志，以允许新的分配
    // Matmul_Unit2buffer_ptr释放，需要找到哪个指针指向了current_output_ptr
    for(i <- 0 until cfg.diag_num - 1){
      when(Matmul_Unit2buffer_ptr(i).valid && Matmul_Unit2buffer_ptr(i).payload === current_output_ptr){
        Matmul_Unit2buffer_ptr(i).valid := False
      }
    }
    // Element_Unit2buffer_ptr释放
    if(enableElementWise){
      when(Element_Unit2buffer_ptr.valid && Element_Unit2buffer_ptr.payload === current_output_ptr){
        Element_Unit2buffer_ptr.valid := False
        element_allocation_grant := True
        Element_col_ptr := 0
      }
    }
    for(row_index <- 0 until cfg.in_MatA_row_num){
      for(col_index <- 0 until cfg.in_MatB_col_num){
        buffer_array(current_output_ptr).data(row_index)(col_index).valid:=False
      }
    }
  }
}


object SystolicArray2D_Verilog extends App {
  val testLength = 4
  val cfg = SystolicArray2D_Config(
    in_Length_Max = testLength,
    in_Length_Min = 3,
    in_MatA_row_num = 3,
    in_MatB_col_num = 3
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
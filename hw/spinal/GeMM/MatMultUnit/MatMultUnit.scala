package GeMM.MatMult
import GeMM.SystolicArray2D._
import spinal.lib._
import spinal.core
import spinal.core._

/**
 * 这个类是MatMultUnit所需要的生成参数定义
 * 
 * @param task_Queue_depth 最大的乘加次数，譬如要计算A(32,16)*B(16,64),此时乘加的次数就是16
 * @param in_Length_Max 最大的乘加次数，譬如要计算A(32,16)*B(16,64),此时乘加的次数就是16
 * @param in_Length_Min 最少的乘加次数,用于计算出缓存阵列结果所需的缓存数量
 * @param in_MatA_row_num 一次性从A侧输入的数的数量，也就是输入的A矩阵的行数,决定了整个脉动整列的尺寸
 * @param in_MatB_col_num 一次性从B侧输入的数的数量，也就是输入的B矩阵的列数,决定了整个脉动整列的尺寸
 * @param in_MatA_element_Width 输入的A矩阵的每个数的位宽
 * @param in_MatB_element_Width 输入的B矩阵的每个数的位宽
 * @param out_MatZ_element_Width 输出的Z矩阵的每个数的位宽
 * @param in_memory_addr_width 输入memory的地址宽度
 * @param out_memory_addr_width 输出的Z矩阵的地址宽度
 * @param in_FIFO_Depth  输入侧跨时钟域fifo深度
 * @param out_FIFO_Depth 输出侧跨时钟域fifo深度（等效）
 */
case class MatMultUnit_Config
(
   // task_Queue params
    task_Queue_depth: Int = 16,
    // SystolicArray2D_CC_Config params
    in_Length_Max: Int=32, // 最大的乘加次数，譬如要计算A(32,16)*B(16,64),此时乘加的次数就是16
    in_Length_Min: Int = 4, // 最少的乘加次数,用于计算出缓存阵列结果所需的缓存数量
    in_MatA_row_num: Int = 32, // 一次性从A侧输入的数的数量，也就是输入的A矩阵的行数,决定了整个脉动整列的尺寸
    in_MatB_col_num: Int = 32, // 一次性从B侧输入的数的数量，也就是输入的B矩阵的列数,决定了整个脉动整列的尺寸
    in_MatA_element_Width: Int = 8, // 输入的A矩阵的每个数的位宽
    in_MatB_element_Width: Int = 8, // 输入的B矩阵的每个数的位宽
    out_MatZ_element_Width: Int = 8,// 输出的Z矩阵的每个数的位宽
    // in memory-mapped(mm) ports params
    in_memory_addr_width: Int = 16,
    // out memory-mapped(mm) ports params
    out_memory_addr_width: Int = 16,
    //! FIFO params(ratio of these two params(in_FIFO_Depth/out_FIFO_Depth=2) are not recommended to be changed)
    in_FIFO_Depth: Int = 16,
    out_FIFO_Depth: Int = 8
)
{
  val SystolicArray2D_CC_Cfg = new SystolicArray2D_CC_Config(
    in_Length_Max=in_Length_Max,
    in_Length_Min=in_Length_Min,
    in_MatA_row_num=in_MatA_row_num,
    in_MatB_col_num=in_MatB_col_num,
    in_MatA_element_Width=in_MatA_element_Width,
    in_MatB_element_Width=in_MatB_element_Width,
    out_MatZ_element_Width=out_MatZ_element_Width,
    in_FIFO_Depth=in_FIFO_Depth,
    out_FIFO_Depth=out_FIFO_Depth
    )//根据获取到的参数生成SystolicArray2D_CC的配置
  val in_MatA_Bus_Width=SystolicArray2D_CC_Cfg.in_MatA_thoughput//输入的A矩阵的Bus宽度(bits)
  val in_MatB_Bus_Width=SystolicArray2D_CC_Cfg.in_MatB_thoughput//输入的B矩阵的Bus宽度(bits)
  val out_MatZ_row_num=SystolicArray2D_CC_Cfg.out_MatZ_row_num//输出的Z矩阵的行数
  val out_MatZ_col_num=SystolicArray2D_CC_Cfg.out_MatZ_col_num//输出的Z矩阵的列数
  val out_MatZ_Bus_Width=SystolicArray2D_CC_Cfg.out_MatZ_thoughput//输出的Z矩阵的Bus宽度(bits)

  val TaskQueue_Cfg=new TaskQueue_Config(
    depth=task_Queue_depth,
    num_readport=2
  )
}

/** **************************************************************
 *该模块的主要作用是根据输入的task指令从输入ram的指定位置取数据，然后等待SystolicArray2D_CC计算完成之后将其存到输出ram
 *
 * *************************************************************/
case class MatMultUnit(cfg: MatMultUnit_Config,
// 时钟域
    clk_memory: ClockDomain=ClockDomain.external("MatMultUnit_memory"),//和所有MemoryReadPort以及其datapump以及MemoryWritePort以及其datapump相连
    clk_core: ClockDomain=ClockDomain.external("MatMultUnit_core")//驱动脉动阵列SystolicArray2D
    ) extends Component {
  /**
   * 定义矩阵乘法任务的案例类，用于配置矩阵乘法单元的任务参数
   * 这个类主要封装了执行矩阵乘法所需的各种地址和数值参数
   * 
   * @param cfg 矩阵乘法单元的配置对象，包含了矩阵乘法单元的各种配置参数
   */
  class Task_TypeDef(cfg: MatMultUnit_Config) extends Bundle{
    val MatA_Start_Addr=UInt(cfg.in_memory_addr_width bits)//A矩阵的起始地址
    val MatB_Start_Addr=UInt(cfg.in_memory_addr_width bits)//B矩阵的起始地址
    val multiple_num=UInt(log2Up(cfg.in_Length_Max) bits)//这个任务的乘加数
    //如果能能将要部署的矩阵乘法的规模凑到脉动阵列的整数倍，那以下两个任务参数可以省略（主要是相应的逻辑太麻烦了，而且对整体的利用率有弊无利）
    //val valid_row_num=UInt(log2Up(cfg.out_MatZ_row_num) bits)//这个任务的有效行数，并非有效的部分会在输入脉动阵列前填充0
    //val valid_col_num=UInt(log2Up(cfg.out_MatZ_col_num) bits)//这个任务的有效列数，并非有效的部分会在输入脉动阵列前填充0
    val MatZ_Start_Addr=UInt(cfg.out_memory_addr_width bits)//Z矩阵的起始地址
  }
  def Task_Type():Task_TypeDef={new Task_TypeDef(cfg)}

  // 定义组件的输入和输出接口
  val io = new Bundle {
    val Task_in = slave Stream(Task_Type())//输入的任务流，包含A矩阵和B矩阵的起始地址，以及乘加的次数，还有输出矩阵的起始地址，还有输出矩阵的行数和列数
    val Task_out = master Stream(Task_Type())//输出的任务流，包含A矩阵和B矩阵的起始地址，以及乘加的次数，还有输出矩阵的起始地址，还有输出矩阵的行数和列数
    val occupancy=  out UInt (log2Up(cfg.task_Queue_depth + 1) bits)
    val flush=in Bool() default(False)
  }
  //例化任务队列
  val TaskQueue=clk_memory(new TaskQueue(cfg.TaskQueue_Cfg,Task_Type()))

  //任务队列相关的逻辑
  val TaskQueue_logic=new ClockingArea(clk_memory){
    TaskQueue.io.flush := io.flush
    TaskQueue.io.push <> io.Task_in
    io.occupancy <> TaskQueue.io.occupancy

  }

  //例化脉动阵列
  val SystolicArray2D_CC=new SystolicArray2D_CC(cfg.SystolicArray2D_CC_Cfg,clk_in=clk_memory,clk_out=clk_memory,clk_core=clk_core)



}
import spinal.lib.tools
object MatMultUnit_Verilog extends App{
  val testLength=4
  val cfg = MatMultUnit_Config(
       // task_Queue params
    task_Queue_depth= 16,
    // SystolicArray2D_CC_Config params
    in_Length_Max=testLength, // 最大的乘加次数，譬如要计算A(32,16)*B(16,64),此时乘加的次数就是16
    in_Length_Min= 4, // 最少的乘加次数,用于计算出缓存阵列结果所需的缓存数量
    in_MatA_row_num= 4, // 一次性从A侧输入的数的数量，也就是输入的A矩阵的行数,决定了整个脉动整列的尺寸
    in_MatB_col_num= 4, // 一次性从B侧输入的数的数量，也就是输入的B矩阵的列数,决定了整个脉动整列的尺寸
    )

    val FileDir = "rtl/MatMultUnit/verilog"
    import java.io.File
    new File(FileDir).mkdirs()
    
    val rtl=SpinalConfig(
      targetDirectory = FileDir,
      oneFilePerComponent = false,
      //memBlackBoxers = mutable.ArrayBuffer(new PhaseSramConverter(vendor)),
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
      removePruned=false,//如果允许剪枝可能会导致画出来的diagram少东西
      bitVectorWidthMax = 20000, //disable internal bigvector limitation"Way too big signal Bits"
      ).generateVerilog(new MatMultUnit(cfg))
    //rtl.printPruned()
    //tools.HDElkDiagramGen(rtl)
}
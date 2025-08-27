package GeMM.SystolicArray2D
import spinal.core
import spinal.core._
import spinal.core.ClockDomain
import spinal.lib._
import spinal.lib.tools
import GeMM.SystolicArray2D._
import scala.math

/** **************************************************************
  *
  *    SystolicArray2D_CC
  *
  *    SystolicArray2D_CC is just SystolicArray2D with cross clock FIFO before input and behind output
  *
  * ************************************************************
  */
//该模块就是在SystolicArray2D的头尾加上了跨时钟域的FIFO,并且调整了输入输出：
//输入：每个数据线都配套一根的Final数据线缩减为一根
//输出：按照SystolicArray2D输出结果按照in_Length_Min压缩输出位宽（代价则是将原本一个时钟周期输出的结果降低到了in_Length_Min个时钟周期输出完毕）

case class SystolicArray2D_CC_Config(
    // SystolicArray2D_Config
    in_Length_Max: Int, // 最大的乘加次数，譬如要计算A(32,16)*B(16,64),此时乘加的次数就是16
    in_Length_Min: Int = 1, // 最少的乘加次数,用于计算出缓存阵列结果所需的缓存数量

    in_MatA_row_num: Int = 12, // 一次性从A侧输入的数的数量，也就是输入的A矩阵的行数
    in_MatB_col_num: Int = 24, // 一次性从B侧输入的数的数量，也就是输入的B矩阵的列数
    // 这两个值也决定了整个脉动整列的尺寸
    in_MatA_element_Width: Int = 8, // 输入的A矩阵的每个数的位宽
    in_MatB_element_Width: Int = 8, // 输入的B矩阵的每个数的位宽
    out_MatZ_element_Width: Int = 8,// 输出的Z矩阵的每个数的位宽

    Enable_Transpose_logic: Boolean = true,
    Enable_ElementWise_logic: Boolean = true,

    // FIFO的配置
    in_FIFO_Depth: Int = 16,
    out_FIFO_Depth: Int = 8,//指缓存多少位结果，实际的内存深度会再乘以in_Length_Min，而内存宽度会除以in_Length_Min。
){
  SpinalInfo("SystolicArray2D_CC_Config:")
  /**
 * 计算并返回x除以y的上限
 * 
 * 对于给定的两个整数x和y，此方法计算x除以y的结果，并返回能够被y整除的最小整数
 * 如果x是y的倍数，结果就是x除以y的值；如果x不是y的倍数，结果比x除以y的值大1
 * 
 * @param x 被除数
 * @param y 除数
 * @return 能够被y整除的最小整数
 */
  def ceilDiv(x: Int, y: Int): Int = {
    (x + y - 1) / y
  }
  val in_MatA_thoughput = in_MatA_row_num*in_MatA_element_Width
  val in_MatB_thoughput = in_MatB_col_num*in_MatB_element_Width
  SpinalInfo("in_MatA_thoughput="+in_MatA_thoughput)
  SpinalInfo("in_MatB_thoughput="+in_MatB_thoughput)
  
  val SystolicArray2D_Cfg = SystolicArray2D_Config(
    in_Length_Max = in_Length_Max,
    in_Length_Min = in_Length_Min,
    in_MatA_row_num = in_MatA_row_num,
    in_MatB_col_num = in_MatB_col_num,
    in_MatA_element_Width = in_MatA_element_Width,
    in_MatB_element_Width = in_MatB_element_Width,
    out_MatZ_element_Width = out_MatZ_element_Width,
  )
  val out_MatZ_row_num = SystolicArray2D_Cfg.in_MatA_row_num
  val out_MatZ_col_num = SystolicArray2D_Cfg.in_MatB_col_num
  val out_MatZ_Width = ceilDiv(SystolicArray2D_Cfg.out_MatZ_Width,in_Length_Min)
  require(in_Length_Min<=math.min(in_MatA_row_num,in_MatB_col_num))
  //一次矩阵完整输出的元素数是cfg.out_MatZ_row_num*cfg.out_MatZ_col_num
  //而脉动阵列可以输出一个结果的速度取决于最小乘加次数，因此输出线的宽度定义为out_MatZ_row_num*out_MatZ_col_num/in_Length_Min
  if(SystolicArray2D_Cfg.out_MatZ_Width%in_Length_Min!=0)
  {//如果除不尽就会报错
    SpinalWarning("please check SystolicArray2D_Cfg.out_MatZ_Width and in_Length_Min\n" +
    "make sure that SystolicArray2D_Cfg.out_MatZ_Width/in_Length_Min is an integer")
  }
  val out_MatZ_buffer_num = SystolicArray2D_Cfg.out_MatZ_buffer_num
  val SystolicArray2DUnit_Cfg = SystolicArray2D_Cfg.SystolicArray2DUnit_Cfg
  val out_MatZ_thoughput=out_MatZ_Width*out_MatZ_element_Width
  //相比SystolicArray2D,这个模块将输出的宽度压缩到了SystolicArray2D_Cfg.out_MatZ_Width/in_Length_Min
  //代价是原本一个周期能传完的数据包，现在会分成in_Length_Min个时钟周期完成传输
  val out_MatZ_package_length=in_Length_Min
  if(out_MatZ_thoughput>=4096)
   { SpinalWarning("out_MatZ_thoughput="+out_MatZ_thoughput+"seems too big")}
   else {
    SpinalInfo("out_MatZ_thoughput="+out_MatZ_thoughput)
   }
}

object SystolicArray2D_CC {
  case class in_Mats_TypeDef(cfg: SystolicArray2D_CC_Config) extends Bundle {
    val A = Vec.fill(cfg.in_MatA_row_num)(SInt(cfg.in_MatA_element_Width bits))
    val B = Vec.fill(cfg.in_MatB_col_num)(SInt(cfg.in_MatB_element_Width bits))
    val mode = OpMode_TypeDef(cfg.SystolicArray2D_Cfg)
    val Final = Bool()
  }

  case class out_Mats_TypeDef(cfg: SystolicArray2D_CC_Config) extends Bundle {
    val Z = Vec.fill(cfg.out_MatZ_Width)(SInt(cfg.out_MatZ_element_Width bits))
    val Final = Bool()
  }
}

import SystolicArray2D_CC._
case class SystolicArray2D_CC(
    cfg: SystolicArray2D_CC_Config,
    // 时钟域
    clk_in: ClockDomain,//和输入数据的Stream接口和fifo相连
    clk_out: ClockDomain,//和输出数据的fifo和输出数据的Stream接口相连
    clk_core: ClockDomain//驱动脉动阵列SystolicArray2D
    ) extends Component {
  def in_Mats_Type():in_Mats_TypeDef={new in_Mats_TypeDef(cfg)}
  def out_Mats_Type():out_Mats_TypeDef={new out_Mats_TypeDef(cfg)}
//  case class in_Mats_TypeDef(cfg: SystolicArray2D_CC_Config) extends Bundle {
//    val A = Vec.fill(cfg.in_MatA_row_num)(SInt(cfg.in_MatA_element_Width bits))
//    val B = Vec.fill(cfg.in_MatB_col_num)(SInt(cfg.in_MatB_element_Width bits))
//    val mode = OpMode_TypeDef(cfg.SystolicArray2D_Cfg)
//    val Final = Bool()
//  }
//  def in_Mats_Type():in_Mats_TypeDef={new in_Mats_TypeDef(cfg)}
//  case class out_Mats_TypeDef(cfg: SystolicArray2D_CC_Config) extends Bundle {
//    val Z = Vec.fill(cfg.out_MatZ_Width)(SInt(cfg.out_MatZ_element_Width bits))
//    val Final = Bool()
//  }
//  def out_Mats_Type():out_Mats_TypeDef={new out_Mats_TypeDef(cfg)}
  val io = new Bundle {
    val in_Mats = slave(Stream(in_Mats_TypeDef(cfg))).addAttribute("DONT_TOUCH = \"TRUE\"")
    val out_Mats = master(Stream(out_Mats_TypeDef(cfg))).addAttribute("DONT_TOUCH = \"TRUE\"")
  }

  val SystolicArray2D_Instance = clk_core(SystolicArray2D(cfg.SystolicArray2D_Cfg))
  val in_Mats_CC_logic= new Area {
    val Fifo = StreamFifoCC(
      dataType = in_Mats_TypeDef(cfg),
      depth = cfg.in_FIFO_Depth,
      pushClock = clk_in,
      popClock = clk_core,
    )
    io.in_Mats >> Fifo.io.push
    Fifo.io.pop.ready <> SystolicArray2D_Instance.io.in_Mats.ready
    Fifo.io.pop.valid <> SystolicArray2D_Instance.io.in_Mats.valid

    //一组输入矩阵只有一个对应的OpMode
    Fifo.io.pop.payload.mode <> SystolicArray2D_Instance.io.in_Mats.OpMode
    for (row_index <- 0 until cfg.in_MatA_row_num) {
      Fifo.io.pop.payload.A(row_index) <> SystolicArray2D_Instance.io.in_Mats.A(row_index).data
      // 所有的数据线原样连接到SystolicArray2D
      Fifo.io.pop.payload.Final <> SystolicArray2D_Instance.io.in_Mats.A(row_index).Final
      // 由于在FIFO中Final只有一根，所以要将Final拓展到A和B的每根数据线
    }
    for (col_index <- 0 until cfg.in_MatB_col_num) {
      Fifo.io.pop.payload.B(col_index) <> SystolicArray2D_Instance.io.in_Mats.B(col_index).data
      // 所有的数据线原样连接到SystolicArray2D
      Fifo.io.pop.payload.Final <> SystolicArray2D_Instance.io.in_Mats.B(col_index).Final
      // 由于在FIFO中Final只有一根，所以要将Final拓展到A和B的每根数据线
    }
  }
  val out_MatZ_CC_logic= new Area {
    val MatZ_type=Vec.fill(cfg.in_MatA_row_num)(Vec.fill(cfg.in_MatB_col_num)(SInt(cfg.out_MatZ_element_Width bits)))
    val compressed_MatZ_data=Stream(out_Mats_TypeDef(cfg).Z)
    // 定义一个压缩器，用于将SystolicArray2D实例的输出矩阵Z进行宽度适配压缩,这里使用了时钟域clk_core，表明压缩操作在clk_core时钟域内进行
    val compresser=clk_core(StreamWidthAdapter(SystolicArray2D_Instance.io.out_Mats, compressed_MatZ_data))
    
    val compressed_MatZ_beforeFIFO=compressed_MatZ_data.map(payload=>{
      val turn_to=new out_Mats_TypeDef(cfg)
      turn_to.Z:=payload
      turn_to.Final:=SystolicArray2D_Instance.io.out_Mats.ready
      turn_to
    })
    val compressed_MatZ_afterFIFO=Stream(out_Mats_TypeDef(cfg))
    
    val Fifo = StreamFifoCC(
      dataType = out_Mats_TypeDef(cfg),
      depth = cfg.out_FIFO_Depth*cfg.in_Length_Min,
      pushClock = clk_core,
      popClock = clk_out
    )
    compressed_MatZ_beforeFIFO <> Fifo.io.push
    Fifo.io.pop<>compressed_MatZ_afterFIFO
    compressed_MatZ_afterFIFO<>io.out_Mats
  }
}
import MemBlackBoxer.PhaseMemBlackBoxer._
import scala.collection.mutable
object SystolicArray2D_CC_Verilog extends App {
 val testLength = 32
 val clk_domain_defaultConfig=ClockDomainConfig(clockEdge = RISING, resetKind = ASYNC, resetActiveLevel = HIGH, softResetActiveLevel = HIGH, clockEnableActiveLevel = HIGH)

 val cfg = SystolicArray2D_CC_Config(
    in_Length_Max = 32,
    in_Length_Min = 16,
    in_MatA_row_num = 32,
    in_MatB_col_num = 32,
    in_MatA_element_Width = 8,
    in_MatB_element_Width = 8,
    out_MatZ_element_Width = 16,
    Enable_Transpose_logic = true,
    in_FIFO_Depth = 8,
    out_FIFO_Depth = 8
  )
 val vendor = MemBlackBoxer.Vendor.UMC40
 val FileDir = "rtl/SystolicArray2D_CC/verilog"
 import java.io.File
 new File(FileDir).mkdirs()

 val rtl=SpinalConfig(
   targetDirectory = FileDir,
   oneFilePerComponent = false,
   //memBlackBoxers = mutable.ArrayBuffer(new PhaseSramConverter(vendor)),
   defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
   removePruned=true,
   bitVectorWidthMax = 20000, //disable internal bigvector limitation"Way too big signal Bits"
 ).generateVerilog(new SystolicArray2D_CC_depress_for_Sim(cfg=cfg,
 clk_in = ClockDomain.external("clk_in"),
clk_out = ClockDomain.external("clk_out"),
clk_core = ClockDomain.external("clk_core")))
 rtl.printPruned()

 ////tools.HDElkDiagramGen(rtl)
}

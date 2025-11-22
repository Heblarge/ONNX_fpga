package MatrixComputeUnit.ElementWise
import spinal.core._
import spinal.lib._
import MatrixComputeUnit.SystolicArray2D._
import spinal.lib.tools
import spinal.core

/** **************************************************************
 *
 *    ElementWise
 *
 *    ElementWise is a systolic array to calculate the ElementWise Production/Addition of two matrices
 *
 * ************************************************************
 */
//支持等宽矩阵进行对饮元素乘积
case class ElementWise_Config(
                                   in_Length_Max: Int, // 最大的乘加次数，譬如要计算A(32,16)*B(16,64),此时乘加的次数就是16
                                   in_Length_Min: Int = 1,// 最少的乘加次数,用于计算出缓存阵列结果所需的缓存数量
                                   in_Mat_row_num: Int = 12, // 一个周期矩阵输入宽度，对应乘法器个数
                                   in_MatA_element_Width: Int = 8, // 输入的A矩阵的每个数的位宽
                                   in_MatB_element_Width: Int = 8, // 输入的B矩阵的每个数的位宽
                                   out_MatZ_element_Width:Int = 16
                                 )


case class ElementWise(cfg: ElementWise_Config) extends Component{
  case class in_Mats_TypeDef(cfg: ElementWise_Config) extends Bundle {
    val A = Vec.fill(cfg.in_Mat_row_num)(SInt(cfg.in_MatA_element_Width bits))
    val B = Vec.fill(cfg.in_Mat_row_num)(SInt(cfg.in_MatB_element_Width bits))
    val Final = Bool()
    val do_multiply = Bool()
  }
  def in_Mats_Bundle():in_Mats_TypeDef={new in_Mats_TypeDef(cfg)}
  case class out_Mats_TypeDef(cfg: ElementWise_Config) extends Bundle {
    val Z = Vec.fill(cfg.in_Mat_row_num)(Vec.fill(cfg.in_Length_Max)(SInt(cfg.out_MatZ_element_Width bits)))
  }
  def out_Mats_Bundle():out_Mats_TypeDef={new out_Mats_TypeDef(cfg)}
  val io = new Bundle {
    val in_Mats = slave(Stream(in_Mats_Bundle()))//.addAttribute("DONT_TOUCH = \"TRUE\"")
    val out_Mats = master(Stream(out_Mats_Bundle()))//.addAttribute("DONT_TOUCH = \"TRUE\"")
  }
  //计数器统计乘次数
  val counter = Reg(UInt(log2Up(cfg.in_Length_Max) bits)) init(0)
  when(io.in_Mats.fire && !io.in_Mats.Final){
    counter := counter + 1
  }.elsewhen(io.in_Mats.Final && io.in_Mats.fire){
    counter := 0
  }

  //stream的控制
  io.in_Mats.ready := !io.out_Mats.valid
  val out_valid = Reg(Bool()) init(False)
  when(io.in_Mats.Final && io.in_Mats.fire){
    out_valid := True
  }.elsewhen(io.out_Mats.fire){
    out_valid := False
  }
  io.out_Mats.valid := out_valid


  val out_Mats_TypeDef_wire = Vec.fill(cfg.in_Mat_row_num)(
    Vec.fill(cfg.in_Length_Max)(SInt_withFinalMark(cfg.out_MatZ_element_Width))
  ) // 阵列的输出线
  val out_MatZ_buffer = Vec.fill(cfg.in_Mat_row_num)(Vec.fill(cfg.in_Length_Max)(Reg(SInt(cfg.out_MatZ_element_Width bits)) init 0))
  when(io.in_Mats.do_multiply){
    for(row_index <- 0 until cfg.in_Mat_row_num){
      out_MatZ_buffer(row_index)(counter) := io.in_Mats.A(row_index) * io.in_Mats.B(row_index)
    }
  }.otherwise {
    for(row_index <- 0 until cfg.in_Mat_row_num){
      out_MatZ_buffer(row_index)(counter) := io.in_Mats.A(row_index) + io.in_Mats.B(row_index)
    }
  }
  io.out_Mats.payload.Z := out_MatZ_buffer
}

object ElementWise_Verilog extends App {
  val testLength = 8
  val cfg = ElementWise_Config(
    in_Length_Max = testLength,
    in_Length_Min = testLength,
    in_Mat_row_num = 4
  )

  val FileDir = "rtl/ElementWise/verilog"
  import java.io.File
  new File(FileDir).mkdirs()

  SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
    removePruned=true
  ).generateVerilog(new ElementWise(cfg)).printPruned()

  //tools.HDElkDiagramGen(SpinalVerilog(new ElementWise(cfg)))
}

package GeMM.SystolicArray2D
import spinal.core._
import spinal.lib._
import spinal.lib.tools
import spinal.core
import scala.math

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

                                 ) {
  val SystolicArray2DUnit_Cfg = SystolicArray2DUnit_Config(
    in_Length = in_Length_Max,
    inA_Width = in_MatA_element_Width,
    inB_Width = in_MatB_element_Width,
    outZ_Width= out_MatZ_element_Width
  )
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
  // Z=A*B
  // 根据矩阵乘法，输出的行数就是A的行数，输出的列数就是B的列数
  val out_MatZ_row_num = in_MatA_row_num // 输出的Z矩阵的行数
  val out_MatZ_col_num = in_MatB_col_num // 输出的Z矩阵的列数

  require(
    in_Length_Min <= math.min(in_MatA_row_num,in_MatB_col_num),
    "in_Length_Min must less or equal to Array width (height)."
  )

  // 一次矩阵完整输出的元素数是cfg.out_MatZ_row_num*cfg.out_MatZ_col_num
  val out_MatZ_Width = out_MatZ_row_num * out_MatZ_col_num
  // 理论上需要做out_MatZ_buffer_num个buffer，
  //最坏的情况下每一个周期都需要开始一个新的矩阵的计算，即Final线会恒为1。
  // 此时，就需要out_MatZ_buffer_num个buffer缓存所有的中间结果
  val diag_num: Int = in_MatB_col_num + in_MatA_row_num
  val out_MatZ_buffer_num: Int = ceilDiv(diag_num,in_Length_Min)+3
  SpinalInfo("SystolicArray2D_Config:\n")
  SpinalInfo("SystolicArray2D_Config:\nout_MatZ_buffer_num=" + out_MatZ_buffer_num)
  if(Enable_Transpose_logic) {
    if(in_MatA_row_num!=in_MatB_col_num) {
      SpinalError("Enable_Transpose_logic :in_MatA_row_num must equal to in_MatB_col_num")
    }
    SpinalInfo("in_MatA_row_num equals to in_MatB_col_num, allow result being transposed")
  }
  if(Enable_ElementWise_logic) {
    if(in_MatA_row_num!=in_MatB_col_num) {
      SpinalError("Enable_ElementWise_logic :in_MatA_row_num must equal to in_MatB_col_num")
    }
    SpinalInfo("in_MatA_row_num equals to in_MatB_col_num, allow performing element-wise operation")
  }
  val IterationInterval = SystolicArray2DUnit_Cfg.IterationInterval
  val Latency = (in_MatA_row_num+in_MatB_col_num)*IterationInterval+SystolicArray2DUnit_Cfg.Latency
}

case class SInt_withFinalMark(element_Width: Int) extends Bundle {
  val data = SInt(element_Width bits)
  val Final = Bool()
}

case class SInt_withFinalMarkandMode(element_Width: Int, result_Element_Width: Int) extends Bundle {
  val data = SInt(element_Width bits)
  val mode = Bits(2 bit)
  val Transpose = Bool()
  val Final = Bool()
  val Shift = SInt(log2Up(result_Element_Width + 1) + 1 bits)
}

case class opmode(cfg: SystolicArray2D_Config) extends Bundle {
  val do_PostTranspose = Bool()
  val do_MatMul = Bool()
  val do_ElementWiseMul = Bool()
  val do_ElementWiseAdd = Bool()

  val do_ElementWiseMax = Bool()

  val post_Shift = SInt(log2Up(cfg.out_MatZ_element_Width + 1) + 1 bits)//结果的截断位置
}

object init_opmode {
  def apply(cfg: SystolicArray2D_Config): opmode = {
    val mode = opmode(cfg)
    mode.do_PostTranspose := False
    mode.do_MatMul := False
    mode.do_ElementWiseMul := False
    mode.do_ElementWiseAdd := False
    mode.do_ElementWiseMax := False
    mode.post_Shift := S(0)
    mode
  }
}


case class SystolicArray2D(cfg: SystolicArray2D_Config) extends Component {
  case class in_Mats_TypeDef(cfg: SystolicArray2D_Config) extends Bundle {
    val A = Vec.fill(cfg.in_MatA_row_num)(SInt_withFinalMark(cfg.in_MatA_element_Width))
    val B = Vec.fill(cfg.in_MatB_col_num)(SInt_withFinalMark(cfg.in_MatB_element_Width))
    val mode = opmode(cfg)//操作模式
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
  val in_MatB_AfterDelay = Vec.fill(cfg.in_MatB_col_num) {SInt_withFinalMarkandMode(cfg.in_MatB_element_Width,cfg.out_MatZ_element_Width)}

  //初始化寄存器
  val init_SInt_withFinalMark_MatA = SInt_withFinalMark(cfg.in_MatA_element_Width)
  init_SInt_withFinalMark_MatA.data := 0
  init_SInt_withFinalMark_MatA.Final := False
  val init_SInt_withFinalMark_MatB = SInt_withFinalMarkandMode(cfg.in_MatB_element_Width, cfg.out_MatZ_element_Width)
  init_SInt_withFinalMark_MatB.data := 0
  init_SInt_withFinalMark_MatB.mode := 0
  init_SInt_withFinalMark_MatB.Transpose := False
  init_SInt_withFinalMark_MatB.Final := False
  init_SInt_withFinalMark_MatB.Shift := S(0)

  val in_MatA_BeforeDelay = Vec.fill(cfg.in_MatA_row_num) {SInt_withFinalMark(cfg.in_MatA_element_Width)}
  val in_MatB_BeforeDelay = Vec.fill(cfg.in_MatB_col_num) {SInt_withFinalMarkandMode(cfg.in_MatB_element_Width, cfg.out_MatZ_element_Width)}
  for (row_index <- 0 until cfg.in_MatA_row_num){
    in_MatA_BeforeDelay(row_index).data := io.in_Mats.payload.A(row_index).data
    in_MatA_BeforeDelay(row_index).Final := io.in_Mats.payload.A(row_index).Final
  }

  //锁存io.in_Mats.payload.mode
  val mode_reg = Reg(opmode(cfg)) init init_opmode(cfg)
  val latched = Reg(Bool()) init False
  val latched_mode = opmode(cfg)
  when(io.in_Mats.fire && !latched){
    mode_reg := io.in_Mats.mode
    latched := True
  }.otherwise{
    mode_reg := mode_reg
  }
  when(io.in_Mats.fire && io.in_Mats.payload.A(0).Final){
    latched := False
  }
  latched_mode := Mux(!latched,io.in_Mats.mode,mode_reg)

  for (col_index <- 0 until cfg.in_MatB_col_num){
    in_MatB_BeforeDelay(col_index).data := io.in_Mats.payload.B(col_index).data
    //解析one-hot opmode为 00or01:MatMul, 10:EleAdd, 11:EleMul
    //in_MatB_BeforeDelay(col_index).mode := (!latched_mode.do_MatMul) ## (latched_mode.do_ElementWiseMul && !latched_mode.do_ElementWiseAdd)
    //解析one-hot opmode为 00:MatMul, 01:EleMax, 10:EleAdd, 11:EleMul
    val bit1 = !(latched_mode.do_MatMul || latched_mode.do_ElementWiseMax)
    val bit0 = latched_mode.do_ElementWiseMax || (latched_mode.do_ElementWiseMul && !latched_mode.do_ElementWiseAdd)
    in_MatB_BeforeDelay(col_index).mode := bit1 ## bit0

    in_MatB_BeforeDelay(col_index).Shift := io.in_Mats.payload.mode.post_Shift
    in_MatB_BeforeDelay(col_index).Transpose := io.in_Mats.payload.mode.do_PostTranspose
    in_MatB_BeforeDelay(col_index).Final := io.in_Mats.payload.B(col_index).Final
  }
  //只有当输入有效时延迟流水线会工作否则暂停
  for (row_index <- 0 until cfg.in_MatA_row_num) {
    in_MatA_AfterDelay(row_index) := Delay(in_MatA_BeforeDelay(row_index), row_index, when = io.in_Mats.fire, init = init_SInt_withFinalMark_MatA)
  }
  for (col_index <- 0 until cfg.in_MatB_col_num) {
    in_MatB_AfterDelay(col_index) := Delay(in_MatB_BeforeDelay(col_index), col_index, when = io.in_Mats.fire, init = init_SInt_withFinalMark_MatB)
  }

  //延迟后数据与计算单元的连接
  val interconnect_outA_with_inA = Vec.fill(cfg.in_MatA_row_num)(Vec.fill(cfg.in_MatB_col_num)(new Bundle {
    val data = SInt(cfg.SystolicArray2DUnit_Cfg.inA_Width bits)
    //val mode = Bits(2 bits)
    val Final = Bool()
    //val Transpose = Bool()
    //val Go = Bool()
  }))
  val interconnect_outB_with_inB = Vec.fill(cfg.in_MatA_row_num)(Vec.fill(cfg.in_MatB_col_num)(new Bundle {
    val data = SInt(cfg.SystolicArray2DUnit_Cfg.inB_Width bits)
    val mode = Bits(2 bits)
    val Final = Bool()
    val Transpose = Bool()
    val Shift = SInt(log2Up(cfg.out_MatZ_element_Width + 1) + 1 bits)
    //val Go = Bool()
  }))
  val out_Mats_TypeDef_wire = Vec.fill(cfg.in_MatA_row_num)(
    Vec.fill(cfg.in_MatB_col_num)(SInt_withFinalMark(cfg.out_MatZ_element_Width))
  )
  val units = Array.tabulate(cfg.in_MatA_row_num, cfg.in_MatB_col_num)((row_index, col_index) => {
    if (row_index + col_index == cfg.in_MatA_row_num - 1) {// 反对角线位置例化可配置Unit
      new SystolicArray2DUnitSpecial(cfg.SystolicArray2DUnit_Cfg).asInstanceOf[BaseSystolicArray2DUnit]
    } else {// 非反对角线位置例化普通Unit
      new SystolicArray2DUnit(cfg.SystolicArray2DUnit_Cfg).asInstanceOf[BaseSystolicArray2DUnit]
    }
  })
  for (row_index <- 0 until cfg.in_MatA_row_num) {// 纵向连接线
    for (col_index <- 0 until cfg.in_MatB_col_num) {// 横向连接线
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
      if (row_index == 0) {// 第一层，和已经完成延迟对齐的输入相连
        units(row_index)(col_index).io.inB <> in_MatB_AfterDelay(col_index).data
        units(row_index)(col_index).io.in_mode <> in_MatB_AfterDelay(col_index).mode
        units(row_index)(col_index).io.in_Transpose <> in_MatB_AfterDelay(col_index).Transpose
        units(row_index)(col_index).io.inB_Final <> in_MatB_AfterDelay(col_index).Final
        units(row_index)(col_index).io.in_Shift <> in_MatB_AfterDelay(col_index).Shift
      } else {// 不是第一层，和上一层的输出相连
        units(row_index)(col_index).io.inB <> interconnect_outB_with_inB(row_index - 1)(col_index).data
        units(row_index)(col_index).io.in_mode <> interconnect_outB_with_inB(row_index - 1)(col_index).mode
        units(row_index)(col_index).io.in_Transpose <> interconnect_outB_with_inB(row_index - 1)(col_index).Transpose
        units(row_index)(col_index).io.inB_Final <> interconnect_outB_with_inB(row_index - 1)(col_index).Final
        units(row_index)(col_index).io.in_Shift <> interconnect_outB_with_inB(row_index - 1)(col_index).Shift
      }
      // 所有的输出先连接到interconnect_outB_with_inB线束
      interconnect_outB_with_inB(row_index)(col_index).data <> units(row_index)(col_index).io.outB
      interconnect_outB_with_inB(row_index)(col_index).mode <> units(row_index)(col_index).io.out_mode
      interconnect_outB_with_inB(row_index)(col_index).Transpose <> units(row_index)(col_index).io.out_Transpose
      interconnect_outB_with_inB(row_index)(col_index).Final <> units(row_index)(col_index).io.outB_Final
      interconnect_outB_with_inB(row_index)(col_index).Shift <> units(row_index)(col_index).io.out_Shift

      //units(row_index)(col_index).io.Go <> shift_wire_in_fire(row_index + col_index)//一旦数据无效，单元不会计算继续向后传递
      units(row_index)(col_index).io.Go <> (io.in_Mats.fire)

      units(row_index)(col_index).io.outZ <> out_Mats_TypeDef_wire(row_index)(col_index).data
      out_Mats_TypeDef_wire(row_index)(col_index).Final := units(row_index)(col_index).io.outA_Final && units(row_index)(col_index).io.outB_Final
    }
  }
  //输出缓存模块
  case class out_MatZ_buffer(cfg: SystolicArray2D_Config) extends Bundle {
    val data = Vec.fill(cfg.in_MatA_row_num)(Vec.fill(cfg.in_MatB_col_num)(Reg(SInt(cfg.out_MatZ_element_Width bits)) init 0))
    val Valid = Reg(Bool()) init False // 说明所有的数据都已经就绪，可以输出了
    val Transpose = cfg.Enable_Transpose_logic generate Reg(Bool()) init False//result transpose？
    val mode = Reg(Bits(2 bits)) init 0//do mat mul or elementwise operation
    val Shift = Reg(SInt(log2Up(cfg.out_MatZ_element_Width + 1) + 1 bits)) init S(0)//存储当前输出经过的移位量
  }
  val buffer_array = Vec.fill(cfg.out_MatZ_buffer_num)(out_MatZ_buffer(cfg))
  val Unit2buffer_ptr = Vec.fill(cfg.diag_num - 1)(Reg(UInt(log2Up(cfg.out_MatZ_buffer_num) bits)) init 0)
  val buffer_array_output_ptr = Reg(UInt(log2Up(cfg.out_MatZ_buffer_num) bits)) init 1
  val sub_ptr = Reg(UInt(log2Up(cfg.in_MatA_row_num) bits)) init 0//在element-wise模式下，表示一块buffer中的子列

  //开始往一个buffer中写入，它将会被写入，直到读出后释放
  val buffer_lock_status = Vec.fill(cfg.out_MatZ_buffer_num)(Reg(Bool()) init False)//未被读出的buffer会标记
  val buffer_array_full = Bool()
  buffer_array_full := {buffer_lock_status.fold(True){(acc, status) => acc && status}}//指示buffer是否全部被锁定，用于停止计算与接收输入

  //val delay_transpose = Delay(io.in_Mats.payload.mode.do_PostTranspose, cycleCount = cfg.in_MatA_row_num + cfg.diag_num - 1 ,when = io.in_Mats.fire,init = False)
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
    when((units(0)(cfg.in_MatB_col_num-1).io.out_mode =/= 0).asBits.asBool ){//&& RegNext(units(0)(cfg.in_MatB_col_num-1).io.Go)
      sub_ptr := sub_ptr + U(1)
    }.elsewhen(sub_ptr === cfg.in_MatA_row_num-1){//in_MatB_AfterDelay(cfg.in_MatB_col_num).Final，要始终填满一个buffer块，填不满需要补零
      sub_ptr := 0
    }

    //mat_mul缓存逻辑
    for (row_index <- 0 until cfg.in_MatA_row_num) {
      for (col_index <- 0 until cfg.in_MatB_col_num) {
        when(units(row_index)(col_index).io.out_mode === 0){//mat_mul
          when(out_Mats_TypeDef_wire(row_index)(col_index).Final){//final触发意味着需要输出&& RegNext(units(row_index)(col_index).io.Go)
            buffer_array(Unit2buffer_ptr(row_index+col_index)).data(row_index)(col_index) := out_Mats_TypeDef_wire(row_index)(col_index).data
            buffer_array(Unit2buffer_ptr(row_index+col_index)).mode := units(row_index)(col_index).io.out_mode
            buffer_array(Unit2buffer_ptr(row_index+col_index)).Transpose := units(row_index)(col_index).io.out_Transpose//Delay(in_MatB_AfterDelay(row_index).Transpose,cfg.diag_num)
            buffer_array(Unit2buffer_ptr(row_index+col_index)).Shift := units(row_index)(col_index).io.out_Shift

            buffer_lock_status(Unit2buffer_ptr(row_index+col_index)) := True
          }
        }
      }
    }
    when(units(cfg.in_MatA_row_num-1)(cfg.in_MatB_col_num-1).io.out_mode === 0){
      when(units(cfg.in_MatA_row_num-1)(cfg.in_MatB_col_num-1).io.outB_Final){
        buffer_array(Unit2buffer_ptr(cfg.diag_num-2)).Valid := True
      }
    }

    //elementwise缓存逻辑
    for (row_index <- 0 until cfg.in_MatA_row_num) {
      for (col_index <- 0 until cfg.in_MatB_col_num) {
        when((units(row_index)(col_index).io.out_mode.asUInt =/= 0).asBits.asBool) {//elementwise
          when(sub_ptr <= cfg.in_MatA_row_num-1) {//&& RegNext(units(0)(cfg.in_MatB_col_num-1).io.Go)
            if(row_index + col_index == cfg.in_MatA_row_num - 1){//在反对角线上
              buffer_array(Unit2buffer_ptr(sub_ptr.resized)).data(row_index)(sub_ptr) := out_Mats_TypeDef_wire(row_index)(col_index).data
              buffer_array(Unit2buffer_ptr(sub_ptr.resized)).mode := units(row_index)(col_index).io.out_mode
              buffer_array(Unit2buffer_ptr(sub_ptr.resized)).Transpose := units(row_index)(col_index).io.out_Transpose
              buffer_array(Unit2buffer_ptr(sub_ptr.resized)).Shift := units(row_index)(col_index).io.out_Shift
            }
          }
        }
      }
    }
    when((units(0)(cfg.in_MatB_col_num-1).io.out_mode =/= 0).asBits.asBool){
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
    when(buffer_array(buffer_array_output_ptr).Transpose){
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
package ReLUFunction

import java.io.File
import spinal.core._
import spinal.lib._

case class ReLU_function_cfg(
                                bit_int : Int,
                                bit_frac: Int
                                 ){
  val in_width = 1 + bit_int + bit_frac
  val out_width = 1 + bit_int + bit_frac
  def x_Type   = SInt(in_width    bits) //输入是一个16bit有符号数
  def xAbs_Type     = UInt(in_width    bits) //取绝对值后，转为16bit无符号数
  def relux_Type    = UInt(out_width   bits) //最终结果13bit ?
}

case class ReLU_function(cfg:ReLU_function_cfg) extends Component {
  import cfg._
  val io = new Bundle{
    val x       = slave Flow x_Type
    val relux        = master Flow relux_Type
  }
  noIoPrefix() //去掉io前缀

  // initial and regs
  val Negative_Flag = Reg(Bool()) //记录输入数据是否为负数
  //Negitave_Flags.reduceLeft((a, b) => {b := a;b})
  val xReg = io.x.toReg(0)   //读取输入数据 存入寄存器 初始化为0
  Negative_Flag := xReg.msb       //最高位表示符号位 1为负数

  //valid 移位寄存器
  val valid_ShiftRegister = Vec(RegInit(False),3)      //三级移位寄存器 初始化为false
  //这一句描述了移位行为
  valid_ShiftRegister.reduceLeft((a, b) => {b := a;b}) //级联移位
  //第一位的输入来自输入的valid信号
  valid_ShiftRegister(0) := io.x.valid

  val xAbs = Reg(xAbs_Type) init 0
  xAbs := xReg.abs   //计算输入值的绝对值

  val resultFinal = Reg(relux_Type) init 0

  // 判断是否为负数
  resultFinal := Mux(Negative_Flag, U(0, cfg.out_width bits), xAbs)

  io.relux.valid := valid_ShiftRegister.last
  io.relux.payload := resultFinal
}

package Sigmoid

import java.io.File
import spinal.core._
import spinal.lib._

case class SigmoidFunction_Config(
    val in_width: Int = 16,
    val out_width: Int = 12
){}

case class SigmoidFunction(cfg:SigmoidFunction_Config) extends Component{

  def inputX_Type = SInt(cfg.in_width bits)
  def xAbs_Type = UInt(cfg.in_width bits)
  def sigmoidX_Type = UInt(1+cfg.out_width bits)
  val io = new Bundle{
    val inputX = slave Flow(inputX_Type)
    val sigmoidX = master Flow sigmoidX_Type
    val sigmoidXValidP1 = out Bool() // one tap before sigmoid valid
    val sigmoidXValidP2 = out Bool() // two taps before sigmoid valid
  }
  noIoPrefix()
  // initial and regs
  val Negative_Flag = Reg(Bool())
  //Negitave_Flags.reduceLeft((a, b) => {b := a;b})
  val xReg = io.inputX.toReg(0)
  Negative_Flag := xReg.msb
  //valid 移位寄存器
  val valid_ShiftRegister = Vec(RegInit(False),3)
  //这一句描述了移位行为
  valid_ShiftRegister.reduceLeft((a, b) => {b := a;b})
  //第一位的输入来自输入的valid信号
  valid_ShiftRegister(0) := io.inputX.valid
  val xAbs = xAbs_Type
  xAbs := xReg.abs
  //判断输入的绝对值的区间范围：
  val xBigger5 = Bool()
  xBigger5 := xAbs >= 5120*4//当x的绝对值大于5，(缩放值1024*4)该位被置位
  val xBigger2375 = Bool()
  xBigger2375 := (xAbs < 5120*4) & (xAbs>=2432*4)//当x属于[2.375,5)，(2.375*1024=2432,缩放值1024*4)该位被置位
  val xBigger1 = Bool()
  xBigger1 := (xAbs < 2432*4) & (xAbs >= 1024*4)//当x属于[1,2.375)，(1*1024=1024,缩放值1024*4)该位被置位
  val xSmaller1 = Bool()
  xSmaller1 := xAbs<1024*4//当x小于1，该位被置位
  val case_onehot = xBigger5 ## xBigger2375 ## xBigger1 ## xSmaller1//将上述flags拼接，生成一个独热码
  val resultPos = Reg(sigmoidX_Type) init 0
  val resultFinal = Reg(sigmoidX_Type) init 0
  //根据输入值的绝对值的范围case_onehot生成resultPos
  resultPos := MuxOH(case_onehot.asBools,
    Seq(((xAbs >> 2) +^ 512*4).resized,
      ((xAbs >> 3) +^ 640*4).resized,
      ((xAbs >> 5) +^ 864*4).resized,
      U(1024*4))
  )

//  switch(case_onehot){
//    is (B"1000"){
//      resultPos := 1024*4
//    }
//    is(B"0100"){
//      resultPos := ((xAbs >> 5) +^ 864*4).resized
//    }
//    is(B"0010"){
//      resultPos := ((xAbs >> 3) +^ 640*4).resized
//    }
//    is(B"0001"){
//      resultPos := ((xAbs >> 2) +^ 512*4).resized
//    }
//    default{
//      resultPos := 0
//    }
//  }
  // 判断是否为负数
  resultFinal := Mux(Negative_Flag, 1024*4 - resultPos, resultPos)

//  when(Negative_Flag){
//    resultFinal := 1024*4 - resultPos
//  }otherwise{
//    resultFinal := resultPos
//  }

  io.sigmoidX.valid := valid_ShiftRegister.last
  io.sigmoidX.payload := resultFinal
  io.sigmoidXValidP1 := valid_ShiftRegister(1)
  io.sigmoidXValidP2 := valid_ShiftRegister(0)
}

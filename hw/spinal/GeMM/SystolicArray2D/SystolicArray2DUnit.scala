package GeMM.SystolicArray2D
import spinal.core._
import spinal.lib._
import spinal.lib.tools
import spinal.core
import scala.math
import Interface.MatrixOperation_TypeDef
import spire.std.boolean
import spinal.lib.misc.pipeline._
import Util._
import spinal.lib.sim._
import spinal.lib.sim.SimStreamAssert
import spinal.lib.sim.ScoreboardInOrder_Bigint
import spinal.core.sim.SimConfig
import spire.syntax.bool

/** **************************************************************
 *
 *    SystolicArray2DUnit
 *
 *    SystolicArray2DUnit is one Unit of systolic array 2D
 *                                                                                                                                                                
 * *************************************************************/
/** Configuration parameters for the SystolicArray2DUnit
  *
  * @param in_Length Number of elements to process
  * @param inA_Width Bit width for matrix A elements (default=16)
  * @param inB_Width Bit width for matrix B elements (default=16)
  * @param outZ_Width Bit width for output Z elements (default=16)
  */
case class SystolicArray2DUnit_Config
(
  in_Length   : Int,  // number of input data
  inA_Width   : Int=16,
  inB_Width   : Int=16,
  outZ_Width  : Int=16,
  ID_Width    : Int=4, 
)
{
    // Calculates bit width needed for A*B product
    val ABProduct_Width = inA_Width+inB_Width
    val ProductSum_Width = ABProduct_Width + log2Up(in_Length)
    val theroretical_outZ_Width = math.max(ABProduct_Width,ProductSum_Width)+1
    if(outZ_Width<theroretical_outZ_Width)
    {SpinalWarning("SystolicArray2DUnit:\n\toutZ_Width is set to"+outZ_Width+"\n\tBut theroretical maximum is ABProduct_Width+ProductSum_Width="+(theroretical_outZ_Width))}
}

case class Fragment_Sim(){
  var fragment:BigInt = 0
  var last:Boolean = false
  def ==(that: Fragment_Sim): Boolean = {
    this.fragment == that.fragment && this.last == that.last
  }
}

/** Control signal definitions for the SystolicArray2DUnit
  *
  * @param out_Width Bit width of the output Z signal
  */
case class SystolicArray2DUnit_Control_TypeDef(out_Width:Int) extends Bundle {
  
  val Mode = MatrixOperation_TypeDef()
  val Transpose = Bool()
  val Shift = SInt(log2Up(out_Width + 1) + 1 bits)
}
case class SystolicArray2DUnit_Control_Sim_TypeDef(){
  var Mode:String = MatrixOperation_TypeDef.MatMul.toString
  var Transpose:Boolean = false
  var Shift:BigInt = 0
  def ==(that: SystolicArray2DUnit_Control_Sim_TypeDef): Boolean = {
    this.Mode == that.Mode && 
    this.Transpose == that.Transpose &&
    this.Shift == that.Shift
  }
}

case class SystolicArray2DUnit_StreamingBundle_TypeDef(cfg: SystolicArray2DUnit_Config) extends Bundle {
  val A = Fragment(SInt(cfg.inA_Width bits))
  val B = Fragment(SInt(cfg.inB_Width bits))
  val Ctrl = SystolicArray2DUnit_Control_TypeDef(
        cfg.outZ_Width)
  val ID = UInt(cfg.ID_Width bits)
  def initized_instance():SystolicArray2DUnit_StreamingBundle_TypeDef = {
    val initialized = new SystolicArray2DUnit_StreamingBundle_TypeDef(cfg)
    initialized.A.fragment := 0
    initialized.A.last := False
    initialized.B.fragment := 0
    initialized.B.last := False
    initialized.Ctrl.Mode := MatrixOperation_TypeDef.MatMul
    initialized.Ctrl.Transpose := False
    initialized.Ctrl.Shift := 0
    initialized.ID:=0
    initialized
  }
}
case class SystolicArray2DUnit_StreamingBundle_Sim_TypeDef(){
  var A = Fragment_Sim()
  var B = Fragment_Sim()
  var Ctrl = SystolicArray2DUnit_Control_Sim_TypeDef()
  var ID:BigInt = 0
  def ==(that: SystolicArray2DUnit_StreamingBundle_Sim_TypeDef): Boolean = {
    this.A == that.A && this.B == that.B && this.Ctrl == that.Ctrl
  }
}
case class SystolicArray2DUnit_ResultBundle_TypeDef(cfg: SystolicArray2DUnit_Config) extends Bundle {
  val Z = Fragment(SInt(cfg.outZ_Width bits))
  val Ctrl = SystolicArray2DUnit_Control_TypeDef(cfg.outZ_Width)
  val ID = UInt(cfg.ID_Width bits)
  def initized_instance():SystolicArray2DUnit_ResultBundle_TypeDef = {
    val initialized = new SystolicArray2DUnit_ResultBundle_TypeDef(cfg)
    initialized.Z.fragment := 0
    initialized.Z.last := False
    initialized.Ctrl.Mode := MatrixOperation_TypeDef.MatMul
    initialized.Ctrl.Transpose := False
    initialized.Ctrl.Shift := 0
    initialized.ID := 0
    initialized
  }
}
case class SystolicArray2DUnit_ResultBundle_Sim_TypeDef(){
  var Z = Fragment_Sim()
  var Ctrl = SystolicArray2DUnit_Control_Sim_TypeDef()
  var ID:BigInt = 0
  def ==(that: SystolicArray2DUnit_ResultBundle_Sim_TypeDef): Boolean = {
    this.Z == that.Z && this.Ctrl == that.Ctrl
  }
}


abstract class SystolicArray2DUnit_io(cfg: SystolicArray2DUnit_Config) extends Component {
  // io定义
  /*
  *   * io.upStream:    输入流，接收SystolicArray2DUnit_StreamingBundle_TypeDef类型的数据。
  *   * io.downStream:  输出流，发送SystolicArray2DUnit_StreamingBundle_TypeDef类型的数据。
  *   * io.result:      结果流，发送SystolicArray2DUnit_ResultBundle_TypeDef类型的数据。 
   */
  val io = new Bundle {
    val upStream = slave Stream(SystolicArray2DUnit_StreamingBundle_TypeDef(cfg))
    val downStream = master Stream(SystolicArray2DUnit_StreamingBundle_TypeDef(cfg))
    val result = master Stream(SystolicArray2DUnit_ResultBundle_TypeDef(cfg))
  }
  //先把upStream fork成两个流
  //一个用于传递到下游，一个用于传递到结果流
  val (upStream_for_downStream, upStream_for_result) = StreamFork2(io.upStream, synchronous=false)
  // 将upStream_for_downStream连接到downStream
  upStream_for_downStream >/-> io.downStream


}

case class SystolicArray2DUnit(cfg: SystolicArray2DUnit_Config) extends SystolicArray2DUnit_io(cfg) {
  // io定义从SystolicArray2DUnit_io继承
  /*  在SystolicArray2DUnit中，我们一共需要实现逻辑
    1. 矩阵乘法（MatMul）
      实现链路是 * -> + -> Shift -> result
      其中*是A和B的乘积，+是累加的结果，
      Shift是对累加结果的移位，result是最终输出。
    为了让+和*调用同样的计算资源，先实现一个输入是A和B的* 阶段，
    再实现一个+模块（matmul:ProductSum and product），
    最后实现一个移位模块和输出模块。
   */
  val Mul_Node,Add_Node,Filter_Node,Shift_Node=Node()
  val SL_MulMax2Add=StageLink(Mul_Node,Add_Node)
  val CL_Add2Filter=CtrlLink(Add_Node,Filter_Node)
  val SL_Filter2Shift=StageLink(Filter_Node,Shift_Node)
  
  /* Mul_Node */
  val PAYLOAD_A = Payload(Fragment(SInt(cfg.inA_Width bits)))
  val PAYLOAD_B = Payload(Fragment(SInt(cfg.inB_Width bits)))
  val PAYLOAD_Ctrl = Payload(SystolicArray2DUnit_Control_TypeDef(
        cfg.outZ_Width))
  val PAYLOAD_ID = Payload((UInt(cfg.ID_Width bits)))
  //这一级的共用乘法器
  val PAYLOAD_factor_1 = Payload(Fragment(SInt(cfg.inA_Width bits)))
  val PAYLOAD_factor_2 = Payload(Fragment(SInt(cfg.inB_Width bits)))
  val PAYLOAD_product = Payload(Fragment(SInt(cfg.ABProduct_Width bits)))

  //这一级的输入，从上游流中获取
  
  Mul_Node.driveFrom(upStream_for_result.throwWhen(upStream_for_result.payload.Ctrl.Mode=/=MatrixOperation_TypeDef.MatMul))((self,payload)=> 
    {
      self(PAYLOAD_A):= payload.A
      self(PAYLOAD_B):= payload.B
      self(PAYLOAD_Ctrl):= payload.Ctrl
      self(PAYLOAD_ID):= payload.ID
    })
  val Mul_Node_logic = new Mul_Node.Area{
    //这一级的共用乘法器
    (PAYLOAD_product).fragment:=
      (PAYLOAD_factor_1).fragment * 
      (PAYLOAD_factor_2).fragment
    
    (PAYLOAD_product).last:=
      (PAYLOAD_factor_1).last &&
      (PAYLOAD_factor_2).last

      PAYLOAD_factor_1 := PAYLOAD_A
      PAYLOAD_factor_2 := PAYLOAD_B
  }
  /*Add_Node*/
  val PAYLOAD_addend_1 = Payload(Fragment(SInt(cfg.ProductSum_Width bits)))
  val PAYLOAD_addend_2 = Payload(Fragment(SInt(cfg.ABProduct_Width bits)))
  val PAYLOAD_sum = Payload(Fragment(SInt(cfg.ProductSum_Width bits)))
  val reg_ProductSum=Reg(SInt(cfg.ProductSum_Width bits)) init 0
  val Add_Node_logic = new Add_Node.Area{
    
    //这一级共用的加法器
    PAYLOAD_sum.fragment:=PAYLOAD_addend_1.fragment+PAYLOAD_addend_2.fragment
    PAYLOAD_Ctrl:=PAYLOAD_Ctrl
    PAYLOAD_sum.last:=PAYLOAD_product.last
    //根据mode选择对应的逻辑
    PAYLOAD_addend_1.fragment:=reg_ProductSum
    PAYLOAD_addend_1.last:=PAYLOAD_addend_2.last
    PAYLOAD_addend_2:=PAYLOAD_product
    when(isFiring){
      when(PAYLOAD_sum.last===True){
        //结算
        reg_ProductSum := 0
      }.otherwise{
        //累加
        reg_ProductSum:=PAYLOAD_sum.fragment
        CL_Add2Filter.terminateIt()
      }
    }
  }
  /* Filter_Node */
  
  /* Shift_Node */
  val PAYLOAD_result = Payload(SInt(cfg.outZ_Width bits))
  val Shift_Node_logic = new Shift_Node.Area{
    //这一级用的移位器
    val instSIntShifter = new SIntShifter(inWidth = cfg.ProductSum_Width, outWidth = cfg.outZ_Width)
    instSIntShifter.io.shiftAmount:=PAYLOAD_Ctrl.Shift
    instSIntShifter.io.input:=(PAYLOAD_sum).fragment
    PAYLOAD_result:=instSIntShifter.io.output
  }
  Shift_Node.driveTo(io.result){(payload, self) =>
    payload.Z.fragment := self(PAYLOAD_result)
    payload.Z.last:=self(PAYLOAD_sum).last
    payload.Ctrl :=self(PAYLOAD_Ctrl)
    payload.ID:=self(PAYLOAD_ID)
  }
  Builder(
    SL_MulMax2Add, 
    CL_Add2Filter,
    SL_Filter2Shift)
}

object SystolicArray2DUnit_Verilog extends App{
    val cfg = SystolicArray2DUnit_Config(32,16,16)

    val FileDir = "rtl/SystolicArray2DUnit/verilog"
    import java.io.File
    new File(FileDir).mkdirs()
    
    SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(new SystolicArray2DUnit(cfg)).printPruned()
    
    //tools.HDElkDiagramGen(SpinalVerilog(new SystolicArray2DUnit(cfg)))
}


import spinal.core.sim.SimConfig
import spinal.core.sim._
import spinal.sim.VCSFlags
object SystolicArray2DUnit_Sim extends App {
    val FileDir = "rtl/SystolicArray2DUnit/verilog"
    import java.io.File
    new File(FileDir).mkdirs()
    val testLength=3
    val cfg = SystolicArray2DUnit_Config(testLength,
    inA_Width = 8,
    inB_Width = 8,
    outZ_Width = 32
    )
    
//VCS
    val flag = VCSFlags(
      compileFlags = List("-kdb","-lca", "+notimingchecks"),
      elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"),
      //    runFlags = List("-fgp=num_threads:11,allow_less_cores", "-l ./run.log")
      //    elaborateFlags = List("-fgp", "+notimingchecks"),
      runFlags = List("-l ./run.log")
    )
    val Spinalcfg=SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
    bitVectorWidthMax = 20000, //disable internal bigvector limitation"Way too big signal Bits"
    )
      val Sim_compiled=SimConfig
      .withVCS(flag)
      .withVcdWave
      .withTimeScale(1 ns)
      .withTimePrecision(1 ns)
      .withConfig(Spinalcfg)
      .allOptimisation
      .compile(new SystolicArray2DUnit(cfg))
    
      Sim_compiled.doSim("MatMul test"){ dut =>
        SimTimeout(10000)
  
        val testLength = 8
        val scoreboard_result = ScoreboardInOrder[BigInt]
        val scoreboard_downStream = ScoreboardInOrder[SystolicArray2DUnit_StreamingBundle_Sim_TypeDef]
  
        // 增加移位范围参数
        val maxShiftBits = 4  // 最大移位位数
        var sumProduct = BigInt(0)
        var currentShift = 0  // 当前事务的移位值
        var isNewMatrix = true  // 标记新矩阵开始
  
        // ===== Stream driver: 自动驱动输入流 =====
        StreamDriver(dut.io.upStream, dut.clockDomain) { payload =>
          var downStream_Ref = new SystolicArray2DUnit_StreamingBundle_Sim_TypeDef()
  
          // 新矩阵开始时随机化shift值
          if (isNewMatrix) {
            currentShift = scala.util.Random.nextInt(maxShiftBits + 1)  // 0到maxShiftBits之间的随机值
            println(s"New matrix started with shift = $currentShift bits")
            isNewMatrix = false
          }
          
          // Generate all random values first
          var afragment = payload.A.fragment.randomizedBigInt()
          var aFinal = payload.A.last.randomize()
          var bfragment = payload.B.fragment.randomizedBigInt()
          var bFinal = payload.B.last.randomize()
          var transposeValue = payload.Ctrl.Transpose.randomize()
          var id = payload.ID.randomize()
  
          // Assign generated values to DUT and reference
          downStream_Ref.A.fragment = afragment
          downStream_Ref.A.last = aFinal
          downStream_Ref.B.fragment = bfragment
          downStream_Ref.B.last = bFinal
          payload.A.fragment #= afragment
          payload.A.last #= aFinal
          payload.B.fragment #= bfragment
          payload.B.last #= bFinal
          payload.Ctrl.Mode #= MatrixOperation_TypeDef.MatMul
          downStream_Ref.Ctrl.Mode = MatrixOperation_TypeDef.MatMul.toString()
          payload.Ctrl.Shift #= currentShift
          downStream_Ref.Ctrl.Shift = currentShift
          payload.Ctrl.Transpose #= transposeValue
          downStream_Ref.Ctrl.Transpose = transposeValue
          payload.ID #= id
          downStream_Ref.ID = id
  
          // Print all generated values after assignment
          println(s"A.fragment: $afragment")
          println(s"A.last: $aFinal")
          println(s"B.fragment: $bfragment")
          println(s"B.last: $bFinal")
          println(s"Set Ctrl.Mode: ${MatrixOperation_TypeDef.MatMul.toString()}")
          println(s"Ctrl.Shift: $currentShift")
          println(s"Ctrl.Transpose: $transposeValue")
          println(s"ID: $id")
          
          scoreboard_downStream.pushRef(downStream_Ref)
          var product = downStream_Ref.A.fragment * downStream_Ref.B.fragment
          sumProduct += product
          if (downStream_Ref.A.last && downStream_Ref.B.last) {
                // 应用移位操作到最终结果
            val shiftedResult = sumProduct >> currentShift
  
            println(s"Matrix complete | Raw sum: $sumProduct | " +
            s"Shift: $currentShift | Result: $shiftedResult")
            scoreboard_result.pushRef(shiftedResult)
            sumProduct = BigInt(0)
            isNewMatrix = true  // 标记下一个矩阵开始
          }
        
          true  // 驱动 valid
        }
  
        // ===== 随机 ready 模拟流控（可选）=====
        StreamReadyRandomizer(dut.io.downStream, dut.clockDomain)
        StreamReadyRandomizer(dut.io.result, dut.clockDomain)
        //dut.io.result.ready #= true // 直接使能结果流的ready
        var downStream_DUT= new SystolicArray2DUnit_StreamingBundle_Sim_TypeDef()
        // ===== Stream monitor: 中间传递监控 =====
        StreamMonitor(dut.io.downStream, dut.clockDomain) { payload =>
          downStream_DUT.A.fragment = payload.A.fragment.toBigInt
          downStream_DUT.A.last = payload.A.last.toBoolean
          downStream_DUT.B.fragment = payload.B.fragment.toBigInt
          downStream_DUT.B.last = payload.B.last.toBoolean
          downStream_DUT.Ctrl.Mode = payload.Ctrl.Mode.toString
          downStream_DUT.Ctrl.Transpose = payload.Ctrl.Transpose.toBoolean
          downStream_DUT.Ctrl.Shift = payload.Ctrl.Shift.toBigInt
          downStream_DUT.ID = payload.ID.toBigInt
          scoreboard_downStream.pushDut(downStream_DUT)
        }
  
        // ===== 输出结果流监控与比对 =====
        StreamMonitor(dut.io.result, dut.clockDomain) { payload =>
          scoreboard_result.pushDut(payload.Z.fragment.toBigInt)
        }
        // 启动时钟激励
        dut.clockDomain.forkStimulus(10)
        // ===== 等待仿真结束 =====
        dut.clockDomain.waitActiveEdgeWhere(
          scoreboard_result.matches == testLength 
        )
        println("TEST PASS".green)
        simSuccess()
    }
}

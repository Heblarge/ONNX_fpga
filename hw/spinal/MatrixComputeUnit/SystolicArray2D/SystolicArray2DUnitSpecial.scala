package MatrixComputeUnit.SystolicArray2D
import spinal.core._
import spinal.lib._
import spinal.lib.tools
import spinal.core
import Interface.MatrixOperation_TypeDef
import spinal.lib.misc.pipeline._
import Util._
import spinal.core.sim.SimConfig
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.sim._
import spinal.lib.sim.SimStreamAssert
import spinal.lib.sim.ScoreboardInOrder_Bigint

/** **************************************************************
 *
 *    SystolicArray2DUnitSpecial
 *
 *    SystolicArray2DUnit is a special unit of systolic array 2D that can be reconfigured to element-wise multiply or addition.
 *
 * *************************************************************/

case class SystolicArray2DUnitSpecial(cfg: SystolicArray2DUnit_Config) extends SystolicArray2DUnit_io(cfg) {

  // io定义从SystolicArray2DUnit_io继承
  /*  在Special中，我们一共需要实现四种逻辑
    1. 矩阵乘法（MatMul）
      实现链路是 * -> + -> Shift -> result
      其中*是A和B的乘积，+是累加的结果，
      Shift是对累加结果的移位，result是最终输出。
    2. 元素最大值（ElementMax）
      实现链路是 max -> Shift -> result
      其中max是A和B的最大值，Shift是对最大值的移位，result是最终输出。
    3. 元素加法（ElementAdd）
      实现链路是 + -> Shift -> result
      其中+是A和B的加法，Shift是对加法结果的移位，result是最终输出。
    4. 元素乘法（ElementMul）
      实现链路是 * -> Shift -> result
      其中*是A和B的乘积，Shift是对乘积结果的移位，result是最终输出。
    为了让+和*调用同样的计算资源，先实现一个输入是A和B但可以跳过的* or Max阶段，
    再实现一个输入可选的+模块（matmul:ProductSum and product;ElementAdd: A and B），
    最后实现一个输入可选的移位模块和输出模块。
   */
  val MulMax_Node,Add_Node,Filter_Node,Shift_Node=Node()
  val SL_MulMax2Add=StageLink(MulMax_Node,Add_Node)
  val CL_Add2Filter=CtrlLink(Add_Node,Filter_Node)
  val SL_Filter2Shift=StageLink(Filter_Node,Shift_Node)

  /* MulMax_Node */
  val PAYLOAD_A = Payload(Fragment(SInt(cfg.inA_Width bits)))
  val PAYLOAD_B = Payload(Fragment(SInt(cfg.inB_Width bits)))
  val PAYLOAD_Ctrl = Payload(SystolicArray2DUnit_Control_TypeDef(
        cfg.outZ_Width))
  val PAYLOAD_ID = Payload((UInt(cfg.ID_Width bits)))
  //这一级的共用乘法器
  val PAYLOAD_factor_1 = Payload(Fragment(SInt(cfg.inA_Width bits)))
  val PAYLOAD_factor_2 = Payload(Fragment(SInt(cfg.inB_Width bits)))
  val PAYLOAD_product = Payload(Fragment(SInt(cfg.ABProduct_Width bits)))

  //这一级的共用比较器
  val PAYLOAD_compare_1 = Payload((SInt(cfg.inA_Width bits)))
  val PAYLOAD_compare_2 = Payload((SInt(cfg.inB_Width bits)))
  val PAYLOAD_greater = Payload((SInt(cfg.ABProduct_Width bits)))

  //这一级的输入，从上游流中获取

  MulMax_Node.driveFrom(upStream_for_result.pipelined(true,true,false))((self,payload)=>
    {
      self(PAYLOAD_A):= payload.A
      self(PAYLOAD_B):= payload.B
      self(PAYLOAD_Ctrl):= payload.Ctrl
      self(PAYLOAD_ID):= payload.ID
    })
  val MulMax_Node_logic = new MulMax_Node.Area{
    //这一级的共用乘法器
    (PAYLOAD_product).fragment:=
      (PAYLOAD_factor_1).fragment *
      (PAYLOAD_factor_2).fragment

    (PAYLOAD_product).last:=
      (PAYLOAD_factor_1).last &&
      (PAYLOAD_factor_2).last
    //这一级的共用比较器
      PAYLOAD_greater:=
        (PAYLOAD_compare_1 max
        PAYLOAD_compare_2).resized

    //根据mode选择对应的逻辑
    when((PAYLOAD_Ctrl).Mode === MatrixOperation_TypeDef.MatMul){
      PAYLOAD_factor_1 := PAYLOAD_A
      PAYLOAD_factor_2 := PAYLOAD_B
      PAYLOAD_compare_1:= S(0)
      PAYLOAD_compare_2:= S(0)
    }.elsewhen((PAYLOAD_Ctrl).Mode === MatrixOperation_TypeDef.ElementMax){
      PAYLOAD_factor_1.fragment := S(0)
      PAYLOAD_factor_1.last:=PAYLOAD_A.last
      PAYLOAD_factor_2.fragment := S(0)
      PAYLOAD_factor_2.last:=PAYLOAD_B.last
      PAYLOAD_compare_1:= PAYLOAD_A.fragment
      PAYLOAD_compare_2:= PAYLOAD_B.fragment
    }.elsewhen((PAYLOAD_Ctrl).Mode === MatrixOperation_TypeDef.ElementMul){
      PAYLOAD_factor_1 := PAYLOAD_A
      PAYLOAD_factor_2 := PAYLOAD_B
      PAYLOAD_compare_1:= S(0)
      PAYLOAD_compare_2:= S(0)
    }.otherwise{
      PAYLOAD_factor_1.fragment := S(0)
      PAYLOAD_factor_1.last:=PAYLOAD_A.last
      PAYLOAD_factor_2.fragment := S(0)
      PAYLOAD_factor_2.last:=PAYLOAD_B.last
      PAYLOAD_compare_1:= S(0)
      PAYLOAD_compare_2:= S(0)
    }
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
    when(PAYLOAD_Ctrl.Mode === MatrixOperation_TypeDef.MatMul){
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
    }.elsewhen((PAYLOAD_Ctrl).Mode === MatrixOperation_TypeDef.ElementAdd){
      PAYLOAD_addend_1 := PAYLOAD_A.resized
      PAYLOAD_addend_2 := PAYLOAD_B.resized
    }.otherwise{
      PAYLOAD_addend_1.fragment:=S(0)
      PAYLOAD_addend_2.fragment:=S(0)
      PAYLOAD_addend_1.last:=False
      PAYLOAD_addend_2.last:=False
    }
  }
  /* Filter_Node */

  /* Shift_Node */
  val PAYLOAD_result = Payload(SInt(cfg.outZ_Width bits))
  val Shift_Node_logic = new Shift_Node.Area{
    //这一级用的移位器
    val instSIntShifter = new SIntShifter(inWidth = cfg.ProductSum_Width, outWidth = cfg.outZ_Width)
    instSIntShifter.io.shiftAmount:=PAYLOAD_Ctrl.Shift
    //根据mode选择对应的逻辑
    when((PAYLOAD_Ctrl).Mode === MatrixOperation_TypeDef.MatMul){
      instSIntShifter.io.input:=(PAYLOAD_sum).fragment
    }.elsewhen((PAYLOAD_Ctrl).Mode === MatrixOperation_TypeDef.ElementMax){
      instSIntShifter.io.input:=(PAYLOAD_greater).resized
    }.elsewhen((PAYLOAD_Ctrl).Mode === MatrixOperation_TypeDef.ElementAdd){
      instSIntShifter.io.input:=(PAYLOAD_sum).fragment
    }.elsewhen((PAYLOAD_Ctrl).Mode === MatrixOperation_TypeDef.ElementMul){
      instSIntShifter.io.input:=(PAYLOAD_product).resized
    }.otherwise{
      instSIntShifter.io.input := S(0)
    }
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

object SystolicArray2DUnitSpecial_Verilog extends App{
  val cfg = SystolicArray2DUnit_Config(in_Length = 32, inA_Width = 16, inB_Width = 16)

  val FileDir = "rtl/SystolicArray2DUnitSpecial/verilog"
  import java.io.File
  new File(FileDir).mkdirs()

  SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(new SystolicArray2DUnitSpecial(cfg)).printPruned()

  //tools.HDElkDiagramGen(SpinalVerilog(new SystolicArray2DUnitSpecial(cfg)))
}


abstract class SystolicArray2DUnitSpecial_Sim extends App {
  val FileDir = "rtl/SystolicArray2DUnitSpecial/verilog"
  import java.io.File
  new File(FileDir).mkdirs()
  import spinal.core.sim._
  val testLength=3
  val cfg = SystolicArray2DUnit_Config(testLength,8,8)

  //VCS
  import scala.sys.process._
  import scala.util.{Try, Success, Failure}
  import scala.util.Random
  import spinal.sim.VCSFlags
  val flag = VCSFlags(
    compileFlags = List("-kdb","-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"),
    //    runFlags = List("-fgp=num_threads:11,allow_less_cores", "-l ./run.log")
    //    elaborateFlags = List("-fgp", "+notimingchecks"),
    runFlags = List("-l ./run.log")
  )
  val Sim_compiled=SimConfig
    .withVCS(flag)
    .withVcdWave
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .withConfig(SpinalConfig(
      targetDirectory = FileDir,
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ))
    .allOptimisation
    .compile(new SystolicArray2DUnitSpecial(cfg))
}
object SystolicArray2DUnitSpecial_Sim_Matmul extends SystolicArray2DUnitSpecial_Sim
{
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
object SystolicArray2DUnitSpecial_Sim_ElementMax extends SystolicArray2DUnitSpecial_Sim
{
  // ================== 元素最大值测试 ==================
  Sim_compiled.doSim("ElementMax test"){ dut =>
    SimTimeout(20000)
    val testLength = 10
    val scoreboard_result = ScoreboardInOrder[BigInt]
    val scoreboard_downStream = ScoreboardInOrder[SystolicArray2DUnit_StreamingBundle_Sim_TypeDef]

    val maxShiftBits = 4
    var currentShift = 0
    var refCounter = 0 // 用于跟踪生成的参考数据数量

    StreamDriver(dut.io.upStream, dut.clockDomain) { payload =>
      val downStream_Ref = new SystolicArray2DUnit_StreamingBundle_Sim_TypeDef()

      // 使用局部计数器代替scoreboard_downStream.refCount
    refCounter += 1

    // 每10个元素重新随机化shift值
    if (refCounter % 10 == 0) {
      currentShift = scala.util.Random.nextInt(maxShiftBits + 1)
    }

      // 生成随机值（包含负数和零）
      val aVal:BigInt = if (scala.util.Random.nextDouble() < 0.1) 0
                else if (scala.util.Random.nextDouble() < 0.2) -payload.A.fragment.randomizedBigInt()
                else payload.A.fragment.randomizedBigInt()

      val bVal:BigInt = if (scala.util.Random.nextDouble() < 0.1) 0
                else if (scala.util.Random.nextDouble() < 0.2) -payload.B.fragment.randomizedBigInt()
                else payload.B.fragment.randomizedBigInt()

      val aFinal = payload.A.last.randomize()
      val bFinal = payload.B.last.randomize()
      val transposeValue = payload.Ctrl.Transpose.randomize()

      // 赋值给DUT和参考模型
      downStream_Ref.A.fragment = aVal
      downStream_Ref.A.last = aFinal
      downStream_Ref.B.fragment = bVal
      downStream_Ref.B.last = bFinal
      payload.A.fragment #= aVal
      payload.A.last #= aFinal
      payload.B.fragment #= bVal
      payload.B.last #= bFinal
      payload.Ctrl.Mode #= MatrixOperation_TypeDef.ElementMax
      downStream_Ref.Ctrl.Mode = MatrixOperation_TypeDef.ElementMax.toString()
      payload.Ctrl.Shift #= currentShift
      downStream_Ref.Ctrl.Shift = currentShift
      payload.Ctrl.Transpose #= transposeValue
      downStream_Ref.Ctrl.Transpose = transposeValue

      // 计算参考结果（最大值）
      val maxVal = if (aVal > bVal) aVal else bVal
      val shiftedResult = maxVal >> currentShift

      // 每个元素都产生结果
      scoreboard_result.pushRef(shiftedResult)
      scoreboard_downStream.pushRef(downStream_Ref)

      true
    }

    // 流控制
    StreamReadyRandomizer(dut.io.downStream, dut.clockDomain)
    StreamReadyRandomizer(dut.io.result, dut.clockDomain)

    // 下游监控
    val downStream_DUT = new SystolicArray2DUnit_StreamingBundle_Sim_TypeDef()
    StreamMonitor(dut.io.downStream, dut.clockDomain) { payload =>
      downStream_DUT.A.fragment = payload.A.fragment.toBigInt
      downStream_DUT.A.last = payload.A.last.toBoolean
      downStream_DUT.B.fragment = payload.B.fragment.toBigInt
      downStream_DUT.B.last = payload.B.last.toBoolean
      downStream_DUT.Ctrl.Mode = payload.Ctrl.Mode.toString
      downStream_DUT.Ctrl.Transpose = payload.Ctrl.Transpose.toBoolean
      downStream_DUT.Ctrl.Shift = payload.Ctrl.Shift.toBigInt
      scoreboard_downStream.pushDut(downStream_DUT)
    }

    // 结果监控
    StreamMonitor(dut.io.result, dut.clockDomain) { payload =>
      scoreboard_result.pushDut(payload.Z.fragment.toBigInt)
    }

    dut.clockDomain.forkStimulus(10)

    dut.clockDomain.waitActiveEdgeWhere(
      scoreboard_result.matches >= testLength
    )
    println("TEST PASS".green)
    simSuccess()

  }
}
object SystolicArray2DUnitSpecial_Sim_ElementAdd extends SystolicArray2DUnitSpecial_Sim
{
  Sim_compiled.doSim("ElementAdd test"){ dut =>
    SimTimeout(20000)
    val testLength = 10
    val scoreboard_result = ScoreboardInOrder[BigInt]
    val scoreboard_downStream = ScoreboardInOrder[SystolicArray2DUnit_StreamingBundle_Sim_TypeDef]

    val maxShiftBits = 4
    var currentShift = 0
    var refCounter = 0 // 用于跟踪生成的参考数据数量

    StreamDriver(dut.io.upStream, dut.clockDomain) { payload =>
      val downStream_Ref = new SystolicArray2DUnit_StreamingBundle_Sim_TypeDef()

      // 使用局部计数器代替scoreboard_downStream.refCount
    refCounter += 1

    // 每10个元素重新随机化shift值
    if (refCounter % 10 == 0) {
      currentShift = scala.util.Random.nextInt(maxShiftBits + 1)
    }

      // 生成随机值（包含负数和零）
      val aVal:BigInt = if (scala.util.Random.nextDouble() < 0.1) 0
                else if (scala.util.Random.nextDouble() < 0.2) -payload.A.fragment.randomizedBigInt()
                else payload.A.fragment.randomizedBigInt()

      val bVal:BigInt = if (scala.util.Random.nextDouble() < 0.1) 0
                else if (scala.util.Random.nextDouble() < 0.2) -payload.B.fragment.randomizedBigInt()
                else payload.B.fragment.randomizedBigInt()

      val aFinal = payload.A.last.randomize()
      val bFinal = payload.B.last.randomize()
      val transposeValue = payload.Ctrl.Transpose.randomize()

      // 赋值给DUT和参考模型
      downStream_Ref.A.fragment = aVal
      downStream_Ref.A.last = aFinal
      downStream_Ref.B.fragment = bVal
      downStream_Ref.B.last = bFinal
      payload.A.fragment #= aVal
      payload.A.last #= aFinal
      payload.B.fragment #= bVal
      payload.B.last #= bFinal
      payload.Ctrl.Mode #= MatrixOperation_TypeDef.ElementAdd
      downStream_Ref.Ctrl.Mode = MatrixOperation_TypeDef.ElementAdd.toString()
      payload.Ctrl.Shift #= currentShift
      downStream_Ref.Ctrl.Shift = currentShift
      payload.Ctrl.Transpose #= transposeValue
      downStream_Ref.Ctrl.Transpose = transposeValue

      // 计算参考结果
      val SumVal = aVal + bVal
      val shiftedResult = SumVal >> currentShift

      // 每个元素都产生结果
      scoreboard_result.pushRef(shiftedResult)
      scoreboard_downStream.pushRef(downStream_Ref)

      true
    }

    // 流控制
    StreamReadyRandomizer(dut.io.downStream, dut.clockDomain)
    StreamReadyRandomizer(dut.io.result, dut.clockDomain)

    // 下游监控
    val downStream_DUT = new SystolicArray2DUnit_StreamingBundle_Sim_TypeDef()
    StreamMonitor(dut.io.downStream, dut.clockDomain) { payload =>
      downStream_DUT.A.fragment = payload.A.fragment.toBigInt
      downStream_DUT.A.last = payload.A.last.toBoolean
      downStream_DUT.B.fragment = payload.B.fragment.toBigInt
      downStream_DUT.B.last = payload.B.last.toBoolean
      downStream_DUT.Ctrl.Mode = payload.Ctrl.Mode.toString
      downStream_DUT.Ctrl.Transpose = payload.Ctrl.Transpose.toBoolean
      downStream_DUT.Ctrl.Shift = payload.Ctrl.Shift.toBigInt
      scoreboard_downStream.pushDut(downStream_DUT)
    }

    // 结果监控
    StreamMonitor(dut.io.result, dut.clockDomain) { payload =>
      scoreboard_result.pushDut(payload.Z.fragment.toBigInt)
    }

    dut.clockDomain.forkStimulus(10)

    dut.clockDomain.waitActiveEdgeWhere(
      scoreboard_result.matches >= testLength
    )
    println("TEST PASS".green)
    simSuccess()
  }
}
object SystolicArray2DUnitSpecial_Sim_ElementMul extends SystolicArray2DUnitSpecial_Sim
{
  Sim_compiled.doSim("ElementMul test"){ dut =>
    SimTimeout(20000)
    val testLength = 10
    val scoreboard_result = ScoreboardInOrder[BigInt]
    val scoreboard_downStream = ScoreboardInOrder[SystolicArray2DUnit_StreamingBundle_Sim_TypeDef]

    val maxShiftBits = 4
    var currentShift = 0
    var refCounter = 0 // 用于跟踪生成的参考数据数量

    StreamDriver(dut.io.upStream, dut.clockDomain) { payload =>
      val downStream_Ref = new SystolicArray2DUnit_StreamingBundle_Sim_TypeDef()

      // 使用局部计数器代替scoreboard_downStream.refCount
    refCounter += 1

    // 每10个元素重新随机化shift值
    if (refCounter % 10 == 0) {
      currentShift = scala.util.Random.nextInt(maxShiftBits + 1)
    }

      // 生成随机值（包含负数和零）
      val aVal:BigInt = if (scala.util.Random.nextDouble() < 0.1) 0
                else if (scala.util.Random.nextDouble() < 0.2) -payload.A.fragment.randomizedBigInt()
                else payload.A.fragment.randomizedBigInt()

      val bVal:BigInt = if (scala.util.Random.nextDouble() < 0.1) 0
                else if (scala.util.Random.nextDouble() < 0.2) -payload.B.fragment.randomizedBigInt()
                else payload.B.fragment.randomizedBigInt()

      val aFinal = payload.A.last.randomize()
      val bFinal = payload.B.last.randomize()
      val transposeValue = payload.Ctrl.Transpose.randomize()

      // 赋值给DUT和参考模型
      downStream_Ref.A.fragment = aVal
      downStream_Ref.A.last = aFinal
      downStream_Ref.B.fragment = bVal
      downStream_Ref.B.last = bFinal
      payload.A.fragment #= aVal
      payload.A.last #= aFinal
      payload.B.fragment #= bVal
      payload.B.last #= bFinal
      payload.Ctrl.Mode #= MatrixOperation_TypeDef.ElementMul
      downStream_Ref.Ctrl.Mode = MatrixOperation_TypeDef.ElementMul.toString()
      payload.Ctrl.Shift #= currentShift
      downStream_Ref.Ctrl.Shift = currentShift
      payload.Ctrl.Transpose #= transposeValue
      downStream_Ref.Ctrl.Transpose = transposeValue

      // 计算参考结果
      val SumVal = aVal * bVal
      val shiftedResult = SumVal >> currentShift

      // 每个元素都产生结果
      scoreboard_result.pushRef(shiftedResult)
      scoreboard_downStream.pushRef(downStream_Ref)

      true
    }

    // 流控制
    StreamReadyRandomizer(dut.io.downStream, dut.clockDomain)
    StreamReadyRandomizer(dut.io.result, dut.clockDomain)

    // 下游监控
    val downStream_DUT = new SystolicArray2DUnit_StreamingBundle_Sim_TypeDef()
    StreamMonitor(dut.io.downStream, dut.clockDomain) { payload =>
      downStream_DUT.A.fragment = payload.A.fragment.toBigInt
      downStream_DUT.A.last = payload.A.last.toBoolean
      downStream_DUT.B.fragment = payload.B.fragment.toBigInt
      downStream_DUT.B.last = payload.B.last.toBoolean
      downStream_DUT.Ctrl.Mode = payload.Ctrl.Mode.toString
      downStream_DUT.Ctrl.Transpose = payload.Ctrl.Transpose.toBoolean
      downStream_DUT.Ctrl.Shift = payload.Ctrl.Shift.toBigInt
      scoreboard_downStream.pushDut(downStream_DUT)
    }

    // 结果监控
    StreamMonitor(dut.io.result, dut.clockDomain) { payload =>
      scoreboard_result.pushDut(payload.Z.fragment.toBigInt)
    }

    dut.clockDomain.forkStimulus(10)

    dut.clockDomain.waitActiveEdgeWhere(
      scoreboard_result.matches >= testLength
    )
    println("TEST PASS".green)
    simSuccess()
  }
}
object SystolicArray2DUnitSpecial_Sim_Mode_switching extends SystolicArray2DUnitSpecial_Sim {

  // ================== 模式切换测试 ==================

  Sim_compiled.doSim("Mode switching test") { dut =>

    SimTimeout(50000)

    val testLength = 20
    val scoreboard_result = ScoreboardInOrder[BigInt]

    val operations = Seq(
      MatrixOperation_TypeDef.MatMul,
      MatrixOperation_TypeDef.ElementMax,
      MatrixOperation_TypeDef.ElementAdd,
      MatrixOperation_TypeDef.ElementMul
    )

    var currentModeIndex = 0
    var currentShift = 0
    var sumProduct = BigInt(0)
    var isNewMatrix = true
    var refCounter = 0

    StreamDriver(dut.io.upStream, dut.clockDomain) { payload =>
      val downStream_Ref = new SystolicArray2DUnit_StreamingBundle_Sim_TypeDef()

      refCounter += 1

      // 即将切换模式：本周期是当前任务的最后一个
      val isLastBeforeSwitch = (refCounter + 1) % 5 == 0

      // 实际切换模式（每5个输入切换一次）
      if (refCounter % 5 == 0) {
        currentModeIndex = (currentModeIndex + 1) % operations.length
        currentShift = scala.util.Random.nextInt(5)

        println(s"Switching mode to ${operations(currentModeIndex)} with shift=$currentShift")

        if (operations(currentModeIndex) == MatrixOperation_TypeDef.MatMul) {
          sumProduct = BigInt(0)
          isNewMatrix = true
        }
      }

      val currentMode = operations(currentModeIndex)

      val aVal: BigInt = if (scala.util.Random.nextDouble() < 0.1) 0
      else if (scala.util.Random.nextDouble() < 0.2) -payload.A.fragment.randomizedBigInt()
      else payload.A.fragment.randomizedBigInt()

      val bVal: BigInt = if (scala.util.Random.nextDouble() < 0.1) 0
      else if (scala.util.Random.nextDouble() < 0.2) -payload.B.fragment.randomizedBigInt()
      else payload.B.fragment.randomizedBigInt()

      val aFinal = isLastBeforeSwitch
      val bFinal = isLastBeforeSwitch
      val transposeValue = payload.Ctrl.Transpose.randomize()

      // 填充 DUT 输入
      payload.A.fragment #= aVal
      payload.A.last #= aFinal
      payload.B.fragment #= bVal
      payload.B.last #= bFinal
      payload.Ctrl.Mode #= currentMode
      payload.Ctrl.Shift #= currentShift
      payload.Ctrl.Transpose #= transposeValue

      // 填充参考数据
      downStream_Ref.A.fragment = aVal
      downStream_Ref.A.last = aFinal
      downStream_Ref.B.fragment = bVal
      downStream_Ref.B.last = bFinal
      downStream_Ref.Ctrl.Mode = currentMode.toString()
      downStream_Ref.Ctrl.Shift = currentShift
      downStream_Ref.Ctrl.Transpose = transposeValue

      // 参考模型计算
      val result = currentMode match {
        case MatrixOperation_TypeDef.MatMul =>
          sumProduct += aVal * bVal
          if (aFinal && bFinal) {
            val res = sumProduct >> currentShift
            sumProduct = BigInt(0)
            isNewMatrix = true
            res
          } else {
            isNewMatrix = false
            null
          }

        case MatrixOperation_TypeDef.ElementMax =>
          val maxVal = if (aVal > bVal) aVal else bVal
          maxVal >> currentShift

        case MatrixOperation_TypeDef.ElementAdd =>
          (aVal + bVal) >> currentShift

        case MatrixOperation_TypeDef.ElementMul =>
          (aVal * bVal) >> currentShift
      }

      if (currentMode != MatrixOperation_TypeDef.MatMul || (aFinal && bFinal)) {
        if (result != null) {
          scoreboard_result.pushRef(result)
        }
      }

      true
    }

    // 流控和监控设置
    StreamReadyRandomizer(dut.io.downStream, dut.clockDomain)
    StreamReadyRandomizer(dut.io.result, dut.clockDomain)

    StreamMonitor(dut.io.result, dut.clockDomain) { payload =>
      scoreboard_result.pushDut(payload.Z.fragment.toBigInt)
    }

    dut.clockDomain.forkStimulus(10)

    dut.clockDomain.waitActiveEdgeWhere(
      scoreboard_result.matches >= testLength
    )

    println("TEST PASS".green)
    simSuccess()
  }
}

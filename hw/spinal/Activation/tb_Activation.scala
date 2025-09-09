package Activation


import java.io.File
import scala.util.Random
import scala.collection.mutable._
import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib._
import spinal.lib.sim._
import spinal.lib.sim.ScoreboardInOrder

import scala.math
import ExponentialFunction.EXP_function_cfg
import LogarithmFunction.LN_function_cfg
import ReLUFunction.ReLU_function_cfg
import SoftplusFunction.Softplus_function_cfg
import _root_.Interface._

import scala.collection.mutable
import scala.collection.mutable.Queue
import ExponentialFunction.EXP_function_sw
import LogarithmFunction.LN_function_sw
import ReLUFunction.ReLU_function_sw
import SoftplusFunction.Softplus_function_sw

class TolerantScoreboard(tolerance: Double) extends ScoreboardInOrder[Double] {
  // 只要 abs(ref - dut) < tolerance 就算匹配
  override def compare(ref: Double, dut: Double): Boolean = {
    math.abs(ref - dut) < tolerance
  }
}


object ActivationTest1 extends App {
  new File("rtl/Activation/sim_Activation_test_report").mkdir()
  val cfg=Activation_Config(
    Matx_Width        = 6,
    MatX_Width        = 6,
    element_in_Width  = 21,
    element_out_Width = 21,
    max_indepth       = 16,
    expCfg            = EXP_function_cfg(bit_int = 8, bit_frac = 12, x_max = 9),
    lnCfg             = LN_function_cfg(bit_int = 8, bit_frac = 12),
    reluCfg           = ReLU_function_cfg(bit_int = 8, bit_frac = 12),
    softplusCfg       = Softplus_function_cfg(bit_int = 8, bit_frac = 12),
    UIDWidth          = 32,
    ShiftWidth        = 6,
    SlicecntWidth     = 16
  )
  val report = SpinalConfig(
    targetDirectory = "rtl/Activation/sim_Activation_test_report",
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
  ).generateVerilog(Activation(cfg))
    .printPruned()
  val flags = VCSFlags(
    compileFlags = List(
      "-kdb","-lca", "+notimingchecks"
      //"-kdb -work xil_defaultlib",
      //"/home/cotr/Workspace/Xilinx_IP_lib/glbl.v",
    ),
    elaborateFlags = List(
      "-fgp", "-kdb", "-lca","+rad", "+notimingchecks"
      //      "-LDFLAGS -Wl,--no-as-needed",
      //      "-fgp",
      //      "-kdb",
      //      "-lca",
      //      "+rad",
      //      "+notimingchecks",
      //      "xil_defaultlib.glbl"
    ),
    runFlags = List("-l ./run.log")
  )

  val simCompiled = SimConfig
    .withVCS(flags)
    .withFsdbWave
    //.withVCSSimSetup(setupFile = "./synopsys_sim.setup", beforeAnalysis = null)
    .withTimeScale(1 ns)
    .withTimePrecision(1 ns)
    .allOptimisation
    .withConfig(
      SpinalConfig(
        targetDirectory = "rtl",
        anonymSignalPrefix = "temp",
        oneFilePerComponent = false,
        defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
        nameWhenByFile = false,
        genLineComments = true
      )
    ).compile(report)

  def genActivationSel(): Activation_TypeDef.E =
    Activation_TypeDef.randomSpinalEnum(new Random())

  def genInput(sel: Activation_TypeDef.E): BigInt = {
    val rnd = new Random()
    sel match {
      case Activation_TypeDef.Exp =>
        val (min, max) = (-2 * 4096, 3 * 4096)
        BigInt(rnd.nextInt(max - min + 1) + min)
      case Activation_TypeDef.Log =>
        val (min, max) = (1, 20 * 4096)
        BigInt(rnd.nextInt(max - min + 1) + min)
      case Activation_TypeDef.Softplus =>
        val (min, max) = (-16 * 4096, 16 * 4096)
        BigInt(rnd.nextInt(max - min + 1) + min)
      case _ =>
        val mag = rnd.nextInt(1 << 12)
        if (rnd.nextBoolean()) BigInt(mag) else BigInt(-mag)
    }
  }

  def genShift(maxShift: Int): Int = {
    val rnd = new Random()
    rnd.nextInt(2 * maxShift + 1) - maxShift
  }
  val EXP_sw=new EXP_function_sw(cfg.expCfg)
  val LN_sw=new LN_function_sw(cfg.lnCfg)
  val RELU_sw= new ReLU_function_sw(cfg.reluCfg)
  val SOFTPLUS_sw=new Softplus_function_sw(cfg.softplusCfg)

  def activationRef(sel: Activation_TypeDef.E, x: Double, shiftAmt: Int): Double = {
    val activated = sel match {
      case Activation_TypeDef.Exp      => EXP_sw.computeFloat(x)
      case Activation_TypeDef.Log      => LN_sw.computeFloat(x)
      case Activation_TypeDef.Relu     => RELU_sw.computeFloat(x)
      case Activation_TypeDef.Softplus => SOFTPLUS_sw.computeFloat(x)
      case Activation_TypeDef.None     => x
    }

    val outWidth = cfg.element_out_Width
    val rawFixed = (activated * 4096.0).round.toLong
    val effShift = if(shiftAmt > outWidth) outWidth else if(shiftAmt < -outWidth) -outWidth else shiftAmt
    val shifted = if(effShift >= 0) rawFixed >> effShift else rawFixed << (-effShift)
    val maxFixed = (math.pow(2, outWidth - 1) - 1)
    val minFixed = -math.pow(2, outWidth - 1)
    val saturated = if(shifted > maxFixed) maxFixed else if(shifted < minFixed) minFixed else shifted
    saturated / 4096

  }

  val sel      = genActivationSel()
  //val sel      = Activation_TypeDef.None
  val shiftAmt = 0
  //val shiftAmt  = genShift(cfg.element_out_Width)

  val tolerance = sel match {
    case Activation_TypeDef.Exp      => 500.0
    case Activation_TypeDef.Log      => 10.0
    case Activation_TypeDef.Softplus => 10.0
    case _                           => 1.0
  }

  val data_total_num = 80
  val data_num = 8
  var StreamDriver_sending_period = 0

  val in_queue = mutable.Queue[Seq[BigInt]]()
  for (i <- 0 until data_total_num){
    var in = Seq.fill(cfg.Matx_Width)(genInput(sel))
    in_queue.enqueue(in)
  }

  val refQueue     = mutable.Queue[Double]()
  var maxError     = 0.0
  var totalError   = 0.0
  var sampleCount  = 0


  // ---- 仿真 ----
  simCompiled.doSim("Activation_tb") { dut =>
    dut.clockDomain.forkStimulus(10)
    SimTimeout(8000)

    val scoreboard = new TolerantScoreboard(tolerance)

    print(in_queue)
    //var in_sending = in_queue.dequeue()


    StreamDriver(dut.io.in_Mats, dut.clockDomain) { payload =>
      if (in_queue.nonEmpty) {
        val current_in = in_queue.dequeue()
        print(in_queue)
        // 取出一个包含6元素的Vector
        payload.CoreInstruction_AfterMatrixOperation.Activation_Instruction.activationFunction        #= sel
        payload.CoreInstruction_AfterMatrixOperation.Activation_Instruction.shiftLeft_AfterActivation #= shiftAmt
        for (idx <- 0 until cfg.Matx_Width) {
          payload.Z(idx) #= current_in(idx) // 填充6个元素

          val realX   = current_in(idx).toDouble / 4096.0
          val realRef = activationRef(sel, realX, shiftAmt)
          refQueue.enqueue(realRef)
        }
        // 控制Final信号，例如每发送data_num个数据后置True
        payload.Final #= (StreamDriver_sending_period == data_num - 1)
        StreamDriver_sending_period = (StreamDriver_sending_period + 1) % data_num
        println(s"Remaining queue items: ${in_queue.size}")
        true
      } else {
        false
      }
    }

    StreamReadyRandomizer(dut.io.out_Mats, dut.clockDomain)
    dut.io.out_Mats.ready #= true
    StreamMonitor(dut.io.out_Mats, dut.clockDomain) { payload =>
      if(dut.io.out_Mats.valid.toBoolean && dut.io.out_Mats.ready.toBoolean) {
        for(i <- 0 until cfg.Matx_Width) {
          val dutOut = payload.Activation_x(i).toInt
          val refOut = (refQueue.dequeue() * 4096).toInt
          val err    = math.abs(dutOut - refOut)
          totalError += err
          maxError = math.max(maxError, err)
          sampleCount += 1
          scoreboard.pushDut(dutOut.toDouble)
          scoreboard.pushRef(refOut.toDouble)
          println(f" channel $i: DUT = $dutOut%.6f, REF = $refOut%.6f, ERR = $err%.6f")
        }
      }
    }
    StreamMonitor(dut.io.in_Mats, dut.clockDomain) { payload =>
    {}
    }


    dut.clockDomain.waitActiveEdgeWhere(scoreboard.matches == data_total_num * cfg.Matx_Width)
    simSuccess()

  }

}

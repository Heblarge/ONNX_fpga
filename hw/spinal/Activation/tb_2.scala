package Activation


import java.io.File
import scala.util.Random
import scala.collection.mutable._
import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib._
import spinal.lib.sim._

import scala.math
import ExponentialFunction.EXP_function_cfg
import LogarithmFunction.LN_function_cfg
import ReLUFunction.ReLU_function_cfg
import SoftplusFunction.Softplus_function_cfg
import _root_.Interface._

import scala.collection.mutable
import scala.collection.mutable.Queue
object ActivationTest2 extends App {
  new File("rtl/Activation/sim_Activation_test_report").mkdir()
  val cfg=Activation_Config(
    Matx_Width        = 6,
    MatX_Width        = 6,
    element_in_Width  = 21,
    element_out_Width = 21,
    max_indepth       = 32,
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
        val (min, max) = (1, 100 * 4096)
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

  def activationRef(sel: Activation_TypeDef.E, x: Double, shiftAmt: Int): Double = {
    val activated = sel match {
      case Activation_TypeDef.Exp      => math.exp(x)
      case Activation_TypeDef.Log      => math.log(x max 1e-6)
      case Activation_TypeDef.Relu     => if (x > 0) x else 0.0
      case Activation_TypeDef.Softplus => math.log(1 + math.exp(x))
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

  //val sel      = genActivationSel()
  val sel      = Activation_TypeDef.Relu
  val shiftAmt = 0
  //val shiftAmt  = genShift(cfg.element_out_Width)

  val data_total_num = 400
  val data_num = 8
  var StreamDriver_sending_period = 0

  val in_queue = mutable.Queue[Seq[BigInt]]()
  for (i <- 0 until data_total_num){
    var in = Seq.fill(cfg.Matx_Width)(genInput(sel))
    in_queue.enqueue(in)
  }


  // ---- 仿真 ----
  simCompiled.doSim("Activation_tb") { dut =>
    dut.clockDomain.forkStimulus(10)
    SimTimeout(50000)

    print(in_queue)
    var in_sending = in_queue.dequeue()


    StreamDriver(dut.io.in_Mats, dut.clockDomain) { payload =>

      payload.CoreInstruction_AfterMatrixOperation.Activation_Instruction.activationFunction        #= sel
      payload.CoreInstruction_AfterMatrixOperation.Activation_Instruction.shiftLeft_AfterActivation #= shiftAmt
      for (idx <- 0 until cfg.Matx_Width) {
        if (StreamDriver_sending_period < data_num - 1) {
          payload.Z(idx) #= in_sending(idx)
          payload.Final #= false
          in_sending = in_queue.dequeue()
        } else if (StreamDriver_sending_period == data_num - 1) {
          payload.Z(idx) #= in_sending(idx)
          payload.Final #= true
          in_sending = in_queue.dequeue()
        } else {
          payload.Z(idx) #= 0
          payload.Final #= false
        }
        println(s"StreamDriver_sending_period")
        println(StreamDriver_sending_period)
      }
      //in_sending = in_queue.dequeue()
      if (StreamDriver_sending_period == data_num - 1) { StreamDriver_sending_period = 0 }
      else { StreamDriver_sending_period = StreamDriver_sending_period + 1 }

      true
    }

    StreamReadyRandomizer(dut.io.out_Mats, dut.clockDomain)
      dut.io.out_Mats.ready #= true
      StreamMonitor(dut.io.out_Mats, dut.clockDomain) { payload =>
        val dutVec = payload.Activation_x.map(_.toInt)

    }
    StreamMonitor(dut.io.in_Mats, dut.clockDomain) { payload =>
      //if (dut.io.out_Mats.valid.toBoolean)
      {}
    }


    // 等够时间再退出
    dut.clockDomain.waitSampling(50)
    simSuccess()

  }

}

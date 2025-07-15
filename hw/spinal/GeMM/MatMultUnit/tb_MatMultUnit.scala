package GeMM.MatMult
import spinal.lib._
import spinal.lib.tools
import spinal.core
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{StreamMonitor, StreamDriver, StreamReadyRandomizer, ScoreboardInOrder}
import spinal.sim.VCSFlags
import scala.util.Random

case class MatMultUnit_test_Top(cfg: MatMultUnit_Config,clk_memory: ClockDomain,clk_core: ClockDomain) extends Component {


}

object MatMultUnit_Sim extends App {
  val FileDir = "rtl/MatMultUnit/verilog"
  val flags = VCSFlags(
    compileFlags = List(
      "-kdb -work xil_defaultlib",
      "/home/cotr/Workspace/Xilinx_IP_lib/glbl.v"
    ),
    elaborateFlags = List(
      "-LDFLAGS -Wl,--no-as-needed",
////    "-fgp",
////    "-kdb",
////    "-lca",
////     "+rad",
////    "+notimingchecks",
      "xil_defaultlib.glbl"
    ),
    runFlags = List("-l ./run.log")
  )
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
  var compiled = SimConfig
    .withVCS(flags)
    .withVCSSimSetup(
      setupFile = "./synopsys_sim.setup",
      beforeAnalysis = null
    )
    .withTimePrecision(1 ps)
    .withFSDBWave
    .withConfig(
      SpinalConfig(
        targetDirectory = FileDir,
        bitVectorWidthMax = 20000,
        removePruned = true
      )
    )
    .compile(new MatMultUnit_test_Top(cfg
    ,clk_memory = ClockDomain.external("clk_memory"),
    clk_core = ClockDomain.external("clk_core")))
  // !单任务测试
  compiled.doSim("single task") { dut =>
    SimTimeout(6000)

    sleep(1000)
    simSuccess()
  }
}

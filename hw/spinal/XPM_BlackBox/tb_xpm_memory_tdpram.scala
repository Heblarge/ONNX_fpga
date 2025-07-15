package XPM_BlackBox
import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import scala.util.Random


object xpm_memory_tdpram_Sim extends App{
val flags = VCSFlags(
    compileFlags = List(
      "-kdb -work xil_defaultlib",
      "/home/cotr/Workspace/Xilinx_IP_lib/glbl.v",
    ),
    elaborateFlags = List(
      "-LDFLAGS -Wl,--no-as-needed",
//      "-fgp",
//      "-kdb",
//      "-lca",
//      "+rad",
//      "+notimingchecks",
      "xil_defaultlib.glbl"
    ),
    runFlags = List("-l ./run.log")
  )
    val cfg=tdpram_IP_Config()
  SimConfig
    .withVCS(flags)
    .withVCSSimSetup(setupFile = "./synopsys_sim.setup", beforeAnalysis = null)
    .withTimePrecision(1 ps)
    .withFSDBWave
    .compile(new tdpram_IP(cfg)).doSimUntilVoid { dut =>
      //SimTimeout(60000)
      fork {
        dut.io.A.clk #= false
        dut.io.B.clk #= false
        while (true) {
          sleep(5)
          dut.io.A.clk #= !dut.io.A.clk.toBoolean
          dut.io.B.clk #= !dut.io.B.clk.toBoolean
        }
      }
      // 初始化信号
      dut.io.A.read.en #= false
      dut.io.A.write.we #= 0
      dut.io.B.read.en #= false
      dut.io.B.addr #= 0
      dut.io.A.addr #= 0
      sleep(25)

      // 写入循环
      for (i <- 0 until 10) {
        dut.io.A.read.en #= true
        dut.io.A.write.we#=1
        dut.io.A.addr #= i    // 写入地址 i
         dut.io.A.write.din #= i * 0x1234 // 写入数据（例如：地址为 i，数据为 i * 0x1234）
        sleep(10)
      }
      dut.io.A.read.en #= false
      dut.io.A.write.we#=1
      sleep(10)
      // 读出循环
      for (i <- 0 until 10) {
        dut.io.B.read.en #= true
        dut.io.B.addr #= i    // 从地址 i 读取
        sleep(10)
        val result=dut.io.B.read.dout.toInt
        val ref =if(i>0){(i-1) * 0x1234}else{0}
        assert(result == ref, s"\nRead value from address $i is $result did not match written value $ref!")
      }
      sleep(100)
      // 初始化信号
      dut.io.A.read.en #= false
      dut.io.A.write.we #= 1
      dut.io.B.read.en #= false
      dut.io.B.addr #= 0
      dut.io.A.addr #= 0
      sleep(20)

      // 写入与读出同一地址的循环
      for (i <- 0 until 10) {
        // 写入操作
        dut.io.A.read.en #= true
        dut.io.A.write.we #=1
        dut.io.A.addr #= i    // 写入地址 i
        dut.io.A.read.dout #= i * 0x1140 // 写入数据（例如：地址为 i，数据为 i * 0x1234）
        // 读出操作
        dut.io.B.read.en #= true
        dut.io.B.addr #= i    // 从地址 i 读取
        sleep(10)
        //val result = dut.io.doutb.toInt
        //val ref = if (i > 0) { (i - 1) * 0x1234 } else { 0 }
        //assert(result == ref, s"\nRead value from address $i is $result did not match written value $ref!")
      }

      sleep(100)
 
      simSuccess()
    }

}
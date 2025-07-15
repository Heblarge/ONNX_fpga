package XPM_BlackBox
import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import scala.util.Random


object xpm_memory_sdpram_Sim extends App{
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
    val cfg=sdpram_IP_Config(
      //WRITE_DATA_WIDTH_A=16384,BYTE_WRITE_WIDTH_A=16384,READ_DATA_WIDTH_B=16384
      //I tested 16384,but failed
      )
  SimConfig
    .withVCS(flags)
    .withVCSSimSetup(setupFile = "./synopsys_sim.setup", beforeAnalysis = null)
    .withTimePrecision(1 ps)
    .withFSDBWave
    .withConfig(SpinalConfig(bitVectorWidthMax = 20000))
    .compile(new sdpram_IP(cfg)).doSimUntilVoid { dut =>
      //SimTimeout(60000)
      fork {
        dut.io.clka #= false
        dut.io.clkb #= false
        while (true) {
          sleep(5)
          dut.io.clka #= !dut.io.clka.toBoolean
          dut.io.clkb #= !dut.io.clkb.toBoolean
        }
      }
      // 初始化信号
      dut.io.ena #= false
      dut.io.wea #= false
      dut.io.enb #= false
      dut.io.addrb #= 0
      dut.io.addra #= 0
      sleep(25)

      // 写入循环
      for (i <- 0 until 10) {
        dut.io.ena #= true
        dut.io.wea #= true
        dut.io.addra #= i    // 写入地址 i
        dut.io.dina #= i * 0x1234 // 写入数据（例如：地址为 i，数据为 i * 0x1234）
        sleep(10)
      }
      dut.io.ena #= false
      dut.io.wea #= false
      sleep(10)
      // 读出循环
      for (i <- 0 until 10) {
        dut.io.enb #= true
        dut.io.addrb #= i    // 从地址 i 读取
        sleep(10)
        val result=dut.io.doutb.toInt
        val ref =if(i>0){(i-1) * 0x1234}else{0}
        assert(result == ref, s"\nRead value from address $i is $result did not match written value $ref!")
      }
      sleep(100)
      // 初始化信号
      dut.io.ena #= false
      dut.io.wea #= false
      dut.io.enb #= false
      dut.io.addrb #= 0
      dut.io.addra #= 0
      sleep(20)

      // 写入与读出同一地址的循环
      for (i <- 0 until 10) {
        // 写入操作
        dut.io.ena #= true
        dut.io.wea #= true
        dut.io.addra #= i    // 写入地址 i
        dut.io.dina #= i * 0x1140 // 写入数据（例如：地址为 i，数据为 i * 0x1234）
        // 读出操作
        dut.io.enb #= true
        dut.io.addrb #= i    // 从地址 i 读取
        sleep(10)
        //val result = dut.io.doutb.toInt
        //val ref = if (i > 0) { (i - 1) * 0x1234 } else { 0 }
        //assert(result == ref, s"\nRead value from address $i is $result did not match written value $ref!")
      }

      sleep(100)
 
      simSuccess()
    }

}

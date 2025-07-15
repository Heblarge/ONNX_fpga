package playGround
import spinal.core._
import spinal.lib._
import spinal.lib.tools
import spinal.core
import spinal.core._
import spinal.core.sim._
import scala.collection.mutable
import scala.collection.mutable.Queue
import scala.util.Random
import spinal.sim.VCSFlags

/**
 * 定义一个 SineRom 类，用于生成正弦波 ROM（只读存储器）组件。
 * 此组件主要提供两个输出信号：sin 和 sinFiltered，均为特定分辨率的正弦波。
 * 其中，sin 信号是从 ROM 直接输出的正弦波，而 sinFiltered 是经过 5 位延迟和滤波后的正弦波。
 *
 * @param resolutionWidth 输出正弦波的宽度，决定正弦波值的精度。
 * @param sampleCount 每个周期内的采样点数，影响正弦波的频率。
 */
case class SineRom(resolutionWidth: Int, sampleCount: Int) extends Component {
  // 定义组件的输出端口
  val io = new Bundle {
    // 定义 sin 和 sinFiltered 输出端口，均为 resolutionWidth 位的有符号整数
    val sin = out SInt(resolutionWidth bits)
    val sinFiltered = out SInt(resolutionWidth bits)
  }

  /**
   * 计算并生成正弦波查找表。
   * 该函数遍历所有采样点，并计算每个采样点对应的正弦波值，
   * 将其转换为适合硬件实现的定点数格式。
   *
   * @return 生成一个序列，每个元素为采样点上的正弦波值，为 resolutionWidth 位的有符号整数。
   */
  def sinTable = for(sampleIndex <- 0 until sampleCount) yield {
    // 计算浮点型正弦波值
    val sinValue = Math.sin(2 * Math.PI * sampleIndex / sampleCount)
    // 将浮点型正弦波值转换为定点数，并返回 resolutionWidth 位的有符号整数
    S((sinValue * ((1<<resolutionWidth)/2-1)).toInt, resolutionWidth bits)
  }

  // 初始化 ROM 内存，使用正弦波查找表作为初始值
  val rom = Mem(SInt(resolutionWidth bits), initialContent = sinTable)
  // 定义一个 phase 寄存器来控制 ROM 地址，初始化为 0
  val phase = Reg(UInt(log2Up(sampleCount) bits)) init 0
  // 更新 phase，在每个时钟周期增加 1，指向下一个正弦波采样点
  phase := phase + 1

  // 从 ROM 中直接输出当前 phase 对应的正弦波值
  io.sin := rom.readSync(phase)

  //平滑处理滤除正弦波信号中的高频噪声
  io.sinFiltered := RegNext(io.sinFiltered - (io.sinFiltered >> 5) + (io.sin >> 5)) init 0
}
object SineRom_Sim extends App {
  SimConfig.withFstWave.compile(SineRom(16,16)).doSim { dut =>
    // Fork a process to generate the reset and the clock on the dut
    dut.clockDomain.forkStimulus(period = 10)

    var modelState = 0
    for (idx <- 0 to 99) {

      // Wait a rising edge on the clock
      dut.clockDomain.waitRisingEdge()

    }
  }
}

case class spram_demo() extends Component{
  val io = new Bundle{
    val writeValid=in Bool()
    val Address=in UInt(log2Up(256) bits)
    val writeData = in(Bits(32 bits))

    val readValid=in Bool()
    val readData =out(Reg(Bits(32 bits)))
  }
  val mem = Mem(Bits(32 bits), wordCount = 256)
mem.write(
  enable  = io.writeValid,
  address = io.Address,
  data    = io.writeData,
  mask    = B"1001_0011"
) 
io.readData := mem.readSync(
  enable  = io.readValid,
  address = io.Address
)
}

object spram_Sim extends App{
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
    
  SimConfig
    .withVCS(flags)
    .withVCSSimSetup(setupFile = "./synopsys_sim.setup", beforeAnalysis = null)
    .withTimePrecision(1 ps)
    .withFSDBWave
    .withConfig(SpinalConfig(bitVectorWidthMax = 20000))
    .compile(new spram_demo()).doSimUntilVoid { dut =>
      SimTimeout(60000)
      dut.clockDomain.forkStimulus(period = 10)
      // 初始化信号
      dut.io.readValid #= false
      dut.io.writeValid #= false
      dut.io.Address #= 0
      sleep(200)

      // 写入循环
      for (i <- 0 until 10) {
        dut.io.readValid #= false
        dut.io.writeValid #= true
        dut.io.Address #= i    // 写入地址 i
        dut.io.writeData #= i * 0x1234 // 写入数据（例如：地址为 i，数据为 i * 0x1234）
        sleep(10)
      }
      dut.io.readValid #= false
      dut.io.writeValid #= false
      sleep(10)
      // 读出循环
      for (i <- 0 until 10) {
        dut.io.readValid #= true
        dut.io.Address #= i    // 从地址 i 读取
        sleep(10)
        val result=dut.io.readData.toInt
        val ref =if(i>0){(i-1) * 0x1234}else{0}
        //assert(result == ref, s"\nRead value from address $i is $result did not match written value $ref!")
      }
        sleep(100)
      // 初始化信号
      dut.io.readValid #= false
      dut.io.writeValid #= false
      dut.io.Address #= 0
      sleep(20)

      // 写入与读出同一地址的循环
      for (i <- 0 until 10) {
        // 写入操作
        dut.io.readValid #= true
        dut.io.writeValid #= true
        dut.io.Address #= i    // 写入地址 i
        dut.io.writeData #= i * 0x1140 // 写入数据（例如：地址为 i，数据为 i * 0x1234）
        // 读出操作
        sleep(10)
        //val result = dut.io.doutb.toInt
        //val ref = if (i > 0) { (i - 1) * 0x1234 } else { 0 }
        //assert(result == ref, s"\nRead value from address $i is $result did not match written value $ref!")
      }

      sleep(100)

      simSuccess()
    }

}

package MatrixCacheInterface
import java.io.File
import Slicer._
import DataPump._
import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axilite._

/** ****************************************************************************
 * - 封装 两个InputMatrixCache与一个OutputMatrixCache
 * - 对外暴露与原内存端口一致的读/写接口
 * - 对外暴露一个可以用来获取status或写入config的sAxi4-Lite接口
 * **************************************************************************** */
case class MatrixCache (addrWidth: Int, dataWidth: Int, lifeWidth: Int = 16) extends Component {
  val io = new Bundle{
    // Status & Config
    val axi      = slave(AxiLite4(addressWidth = 5, dataWidth = 32))
    // Cache A
    val readA    = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val writeA   = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val switchA = in Bool()     // 单周期有效的切换脉冲：写完当前 bank 后触发

    // Cache B
    val readB    = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val writeB   = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val switchB = in Bool()     // 单周期有效的切换脉冲：写完当前 bank 后触发

    // Cache C
    val readC    = slave(MemoryReadPort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val writeC   = slave(MemoryWritePort_TypeDef(AddressWidth = addrWidth, DataWidth = dataWidth))
    val switchC = in Bool()     // 单周期有效的切换脉冲：写完当前 bank 后触发
    // Single DMA
    val dmaIntr = in Bool()

    // Independent triggers for debugging
    val dmaIntrA = out Bool()
    val dmaIntrB = out Bool()
    val dmaIntrC = out Bool()
  }




  // =============================
  // 管理 DMA 中断路由
  // =============================
  object CacheActivity extends SpinalEnum(defaultEncoding = binarySequential) {
    val NONE, A_IN, B_IN, C_OUT = newElement()
  }
  val cacheActivity = CacheActivity
  val lastActivity = Reg(CacheActivity()) init(cacheActivity.NONE)
  val dmaBusy      = Reg(Bool()) init(False)

  when(io.writeA.Valid.rise() && !dmaBusy) {
    lastActivity := cacheActivity.A_IN
    dmaBusy := True
  }

  when(io.writeB.Valid.rise() && !dmaBusy) {
    lastActivity := cacheActivity.B_IN
    dmaBusy := True
  }

  when(io.readC.Valid.rise() && !dmaBusy) {   // OutputCache 是从 SDPRAM 读
    lastActivity := cacheActivity.C_OUT
    dmaBusy := True
  }
  val intrFire = io.dmaIntr.rise()

  val routeToA = intrFire && (lastActivity === cacheActivity.A_IN)
  val routeToB = intrFire && (lastActivity === cacheActivity.B_IN)
  val routeToC = intrFire && (lastActivity === cacheActivity.C_OUT)

  io.dmaIntrA := routeToA
  io.dmaIntrB := routeToB
  io.dmaIntrC := routeToC

  // 清除活动记录
  when(intrFire) {
    lastActivity := cacheActivity.NONE
    dmaBusy := False
  }

  // =============================
  // 例化 Cache 模块
  // =============================
  val inputMatrixCacheA = new InputMatrixCache(addrWidth, dataWidth, lifeWidth)
  val inputMatrixCacheB = new InputMatrixCache(addrWidth, dataWidth, lifeWidth)
  val outputMatrixCacheC = new OutputMatrixCache(addrWidth, dataWidth)
  inputMatrixCacheA.io.read <> io.readA
  inputMatrixCacheA.io.write <> io.writeA
  inputMatrixCacheA.io.switch <> io.switchA

  inputMatrixCacheB.io.read <> io.readB
  inputMatrixCacheB.io.write <> io.writeB
  inputMatrixCacheB.io.switch <> io.switchB

  outputMatrixCacheC.io.read <> io.readC
  outputMatrixCacheC.io.write <> io.writeC
  outputMatrixCacheC.io.switch <> io.switchC

  // =============================
  // 例化 寄存器配置读取 模块
  // =============================
  val statusRegsInterface = new StatusRegsInterface(bitCount = 32)
  statusRegsInterface.io.axi <> io.axi

  val lifeA = Bits(32 bits)
  val lifeB = Bits(32 bits)
  val statusAll = Bits(32 bits)
  val clearIntr = Bits(32 bits)



  statusRegsInterface.io.gpio_out




}


object MatrixCache_verilog {

  new File("rtl/MatrixCache").mkdir() // 创建输出目录

  def main(arg:Array[String]): Unit ={
    SpinalConfig(
      targetDirectory = "rtl/MatrixCache",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(MatrixCache(addrWidth=4,dataWidth=8))
      .printPruned()
  }
}

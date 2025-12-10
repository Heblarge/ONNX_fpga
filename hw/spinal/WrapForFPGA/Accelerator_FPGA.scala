package WrapForFPGA

import Accelerator._
import Interface._
import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axilite._
import scala.math._

// =============================================================
// 配置类
// =============================================================
case class Accelerator_FPGA_Config(
    acceleratorCfg: AcceleratorCfg,
    use64BitBus: Boolean = true,
    axiAddrWidth: Int = 12,
    instFifoDepth: Int = 8
) {
  // 计算指令位宽
  val instBitWidth = {
    val tempInst = ComputeInstruction1_TypeDef(
      UIDWidth = acceleratorCfg.UIDWidth,
      ShiftWidth = acceleratorCfg.slicerCfg.ShiftWidth,
      AddressWidth = acceleratorCfg.AddressWidth,
      ShapeWidth = acceleratorCfg.ShapeWidth
    )
    tempInst.getBitsWidth
  }
  
  // 需要多少个32位寄存器
  val num32BitRegs = (instBitWidth + 31) / 32
  
  require(num32BitRegs <= 5, s"Instruction width $instBitWidth bits requires $num32BitRegs registers, but only support up to 5 (160 bits)")
}

// =============================================================
// 指令桥接器：Bits 转 ComputeInstruction1_TypeDef
// =============================================================
class InstructionBridge(config: Accelerator_FPGA_Config) extends Component {
  val io = new Bundle {
    val inst_bits = slave Stream(Bits(config.instBitWidth bits))
    val inst_typed = master Stream(ComputeInstruction1_TypeDef(
      UIDWidth = config.acceleratorCfg.UIDWidth,
      ShiftWidth = config.acceleratorCfg.slicerCfg.ShiftWidth,
      AddressWidth = config.acceleratorCfg.AddressWidth,
      ShapeWidth = config.acceleratorCfg.ShapeWidth
    ))
  }
  
  // 直接转换 Bits 到类型化指令
  io.inst_typed.valid := io.inst_bits.valid
  io.inst_bits.ready := io.inst_typed.ready
  io.inst_typed.payload.assignFromBits(io.inst_bits.payload)
}

// =============================================================
// FPGA 顶层模块
// =============================================================
class Accelerator_FPGA(config: Accelerator_FPGA_Config) extends Component {
  
  val io = new Bundle {
    // AXI4-Lite 指令接口
    val s_axi_inst = slave(AxiLite4(
      AxiLite4Config(
        addressWidth = config.axiAddrWidth,
        dataWidth = if(config.use64BitBus) 64 else 32
      )
    ))
    
    // 状态输出
    val inst_fifo_full = out Bool()
    val inst_fifo_empty = out Bool()
    val accelerator_busy = out Bool()
    
    // 可选：调试端口
    val debug_inst_count = out UInt(32 bits)
  }
  
  // =============================================================
  // 1. AXI4-Lite 到 Stream 转换（使用动态位宽指令包装器）
  // =============================================================
  val axiToStream = config.num32BitRegs match {
    case n if n <= 4 => 
      // 使用 128-bit wrapper
      new Inst128_Wrapper(config.use64BitBus)
    case 5 => 
      // 使用 160-bit wrapper
      new Inst160_Wrapper(config.use64BitBus)
  }
  
  axiToStream.io.s_axi <> io.s_axi_inst
  
  // =============================================================
  // 2. 位宽适配：将 128/160 bits 转换为实际指令位宽
  // =============================================================
  val instStreamBits = Stream(Bits(config.instBitWidth bits))
  
  // 截取或扩展到正确的位宽
  instStreamBits.valid := axiToStream.io.m_stream.valid
  axiToStream.io.m_stream.ready := instStreamBits.ready
  
  if (axiToStream.io.m_stream.payload.getWidth > config.instBitWidth) {
    // 如果 AXI 转换器输出更宽，截取低位
    instStreamBits.payload := axiToStream.io.m_stream.payload.resized
  } else if (axiToStream.io.m_stream.payload.getWidth < config.instBitWidth) {
    // 如果 AXI 转换器输出更窄，高位补零
    instStreamBits.payload := axiToStream.io.m_stream.payload.resize(config.instBitWidth)
  } else {
    instStreamBits.payload := axiToStream.io.m_stream.payload
  }
  
  // =============================================================
  // 3. 指令 FIFO 缓冲
  // =============================================================
  val instFifo = StreamFifo(
    dataType = Bits(config.instBitWidth bits),
    depth = config.instFifoDepth
  )
  instFifo.io.push <> instStreamBits
  
  io.inst_fifo_full := !instFifo.io.push.ready
  io.inst_fifo_empty := !instFifo.io.pop.valid
  
  // =============================================================
  // 4. Bits 转 ComputeInstruction1_TypeDef
  // =============================================================
  val instBridge = new InstructionBridge(config)
  instBridge.io.inst_bits <> instFifo.io.pop
  
  // =============================================================
  // 5. 实例化加速器核心
  // =============================================================
  val accelerator = Accelerator(config.acceleratorCfg)
  
  // ComputeInstruction1_TypeDef 转 ComputeInstruction_TypeDef
  val instConverter = Stream(ComputeInstruction_TypeDef(
    UIDWidth = config.acceleratorCfg.UIDWidth,
    ShiftWidth = config.acceleratorCfg.slicerCfg.ShiftWidth,
    AddressWidth = config.acceleratorCfg.AddressWidth,
    ShapeWidth = config.acceleratorCfg.ShapeWidth
  ))
  
  instConverter.valid := instBridge.io.inst_typed.valid
  instBridge.io.inst_typed.ready := instConverter.ready
  instConverter.payload.assignFromInst(instBridge.io.inst_typed.payload)
  
  accelerator.io.inst <> instConverter
  
  // =============================================================
  // 6. 状态监控
  // =============================================================
  val instCounter = Counter(32 bits)
  when(accelerator.io.inst.fire) {
    instCounter.increment()
  }
  io.debug_inst_count := instCounter.value
  
  // 简单的忙碌指示（可根据实际情况优化）
  io.accelerator_busy := axiToStream.io.busy || !instFifo.io.push.ready
}

// =============================================================
// Verilog 生成
// =============================================================
object Accelerator_FPGA_Verilog extends App {
  val FileDir = "rtl/Accelerator_FPGA/verilog"
  import java.io.File
  new File(FileDir).mkdirs()
  
  // 创建配置
  val acceleratorCfg = AcceleratorCfg(
    UIDWidth = 19,
    AddressWidth = 20,
    ShapeWidth = 16,
    systolicArraySideNum = 32,
    elementWidth = 24,
    intWidth = 12,
    systolicArrayInFifoDepth = 2,
    systolicArrayOutFifoDepth = 2,
    systolicArrayInstFifoDepth = 16,
    activationOutFifoDepth = 32,
    slicedInstFifoDepth = 16,
    numCores = 1
  )
  
  val fpgaCfg = Accelerator_FPGA_Config(
    acceleratorCfg = acceleratorCfg,
    use64BitBus = true,
    axiAddrWidth = 12,
    instFifoDepth = 16
  )
  
  println(s"Instruction bit width: ${fpgaCfg.instBitWidth} bits")
  println(s"Number of 32-bit registers: ${fpgaCfg.num32BitRegs}")
  println(s"Using ${if(fpgaCfg.use64BitBus) "64-bit" else "32-bit"} AXI bus")
  
  SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    removePruned = true,
    bitVectorWidthMax = 100000,
    defaultConfigForClockDomains = ClockDomainConfig(
      resetActiveLevel = LOW
    )
  ).generateVerilog(new Accelerator_FPGA(fpgaCfg))
  
  println(s"\nVerilog files generated in: $FileDir")
}
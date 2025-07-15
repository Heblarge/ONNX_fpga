package XPM_BlackBox

import spinal.core._
// XPM_MEMORY instantiation template for Dual Port ROM configurations
// Refer to the targeted device family architecture libraries guide for XPM_MEMORY documentation
// =======================================================================================================================

// Parameter usage table, organized as follows:
// +---------------------------------------------------------------------------------------------------------------------+
// | Parameter name       | Data type          | Restrictions, if applicable                                             |
// |---------------------------------------------------------------------------------------------------------------------|
// | Description                                                                                                         |
// +---------------------------------------------------------------------------------------------------------------------+
// +---------------------------------------------------------------------------------------------------------------------+
// | ADDR_WIDTH_A         | Integer            | Range: 1 - 20. Default value = 6.                                       |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify the width of the port A address port addra, in bits.                                                        |
// | Must be large enough to access the entire memory from port A, i.e. &gt;= $clog2(MEMORY_SIZE/READ_DATA_WIDTH_A).     |
// +---------------------------------------------------------------------------------------------------------------------+
// | ADDR_WIDTH_B         | Integer            | Range: 1 - 20. Default value = 6.                                       |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify the width of the port B address port addrb, in bits.                                                        |
// | Must be large enough to access the entire memory from port B, i.e. &gt;= $clog2(MEMORY_SIZE/READ_DATA_WIDTH_B).     |
// +---------------------------------------------------------------------------------------------------------------------+
// | AUTO_SLEEP_TIME      | Integer            | Range: 0 - 15. Default value = 0.                                       |
// |---------------------------------------------------------------------------------------------------------------------|
// | Must be set to 0                                                                                                    |
// | 0 - Disable auto-sleep feature                                                                                      |
// +---------------------------------------------------------------------------------------------------------------------+
// | CASCADE_HEIGHT       | Integer            | Range: 0 - 64. Default value = 0.                                       |
// |---------------------------------------------------------------------------------------------------------------------|
// | 0- No Cascade Height, Allow Vivado Synthesis to choose.                                                             |
// | 1 or more - Vivado Synthesis sets the specified value as Cascade Height.                                            |
// +---------------------------------------------------------------------------------------------------------------------+
// | CLOCKING_MODE        | String             | Allowed values: common_clock, independent_clock. Default value = common_clock.|
// |---------------------------------------------------------------------------------------------------------------------|
// | Designate whether port A and port B are clocked with a common clock or with independent clocks-                     |
// | "common_clock"- Common clocking; clock both port A and port B with clka                                             |
// | "independent_clock"- Independent clocking; clock port A with clka and port B with clkb                              |
// +---------------------------------------------------------------------------------------------------------------------+
// | ECC_BIT_RANGE        | String             | Default value = 7:0.                                                    |
// |---------------------------------------------------------------------------------------------------------------------|
// | This parameter is only used by synthesis. Specify the ECC bit range on the provided data.                           |
// | "7:0" - it specifies lower 8 bits are ECC bits.                                                                     |
// +---------------------------------------------------------------------------------------------------------------------+
// | ECC_MODE             | String             | Allowed values: no_ecc, both_encode_and_decode, decode_only, encode_only. Default value = no_ecc.|
// |---------------------------------------------------------------------------------------------------------------------|
// |                                                                                                                     |
// |   "no_ecc" - Disables ECC                                                                                           |
// |   "encode_only" - Enables ECC Encoder only                                                                          |
// |   "decode_only" - Enables ECC Decoder only                                                                          |
// |   "both_encode_and_decode" - Enables both ECC Encoder and Decoder                                                   |
// +---------------------------------------------------------------------------------------------------------------------+
// | ECC_TYPE             | String             | Allowed values: none, ECCHSIAO32-7, ECCHSIAO64-8, ECCHSIAO128-9, ECCH32-7, ECCH64-8. Default value = none.|
// |---------------------------------------------------------------------------------------------------------------------|
// | This parameter is only used by synthesis. Specify the algorithm used to generate the ecc bits outside the XPM Memory.|
// | XPM Memory does not performs ECC operation with this parameter.                                                     |
// |                                                                                                                     |
// |   "none" - No ECC                                                                                                   |
// |   "ECCH32-7" - 32 bit ECC Hamming algorithm is used                                                                 |
// |   "ECCH64-8" - 64 bit ECC Hamming algorithm is used                                                                 |
// |   "ECCHSIAO32-7" - 32 bit ECC HSIAO algorithm is used                                                               |
// |   "ECCHSIAO64-8" - 64 bit ECC HSIAO algorithm is used                                                               |
// |   "ECCHSIAO128-9" - 128 bit ECC HSIAO algorithm is used                                                             |
// +---------------------------------------------------------------------------------------------------------------------+
// | IGNORE_INIT_SYNTH    | Integer            | Range: 0 - 1. Default value = 0.                                        |
// |---------------------------------------------------------------------------------------------------------------------|
// | 0 - Initiazation file if specified will applies for both simulation and synthesis                                   |
// | 1 - Initiazation file if specified will applies for only simulation and will ignore for synthesis                   |
// +---------------------------------------------------------------------------------------------------------------------+
// | MEMORY_INIT_FILE     | String             | Default value = none.                                                   |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify "none" (including quotes) for no memory initialization, or specify the name of a memory initialization file-|
// | Enter only the name of the file with .mem extension, including quotes but without path (e.g. "my_file.mem").        |
// | File format must be ASCII and consist of only hexadecimal values organized into the specified depth by              |
// | narrowest data width generic value of the memory. Initialization of memory happens through the file name specified only when parameter|
// | MEMORY_INIT_PARAM value is equal to "". |                                                                           |
// | When using XPM_MEMORY in a project, add the specified file to the Vivado project as a design source.                |
// +---------------------------------------------------------------------------------------------------------------------+
// | MEMORY_INIT_PARAM    | String             | Default value = 0.                                                      |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify "" or "0" (including quotes) for no memory initialization through parameter, or specify the string          |
// | containing the hex characters. Enter only hex characters with each location separated by delimiter (,).             |
// | Parameter format must be ASCII and consist of only hexadecimal values organized into the specified depth by         |
// | narrowest data width generic value of the memory.For example, if the narrowest data width is 8, and the depth of    |
// | memory is 8 locations, then the parameter value should be passed as shown below.                                    |
// | parameter MEMORY_INIT_PARAM = "AB,CD,EF,1,2,34,56,78"                                                               |
// | Where "AB" is the 0th location and "78" is the 7th location.                                                        |
// +---------------------------------------------------------------------------------------------------------------------+
// | MEMORY_OPTIMIZATION  | String             | Allowed values: true, false. Default value = true.                      |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify "true" to enable the optimization of unused memory or bits in the memory structure. Specify "false" to      |
// | disable the optimization of unused memory or bits in the memory structure.                                          |
// +---------------------------------------------------------------------------------------------------------------------+
// | MEMORY_PRIMITIVE     | String             | Allowed values: auto, block, distributed, ultra. Default value = auto.  |
// |---------------------------------------------------------------------------------------------------------------------|
// | Designate the memory primitive (resource type) to use-                                                              |
// | "auto"- Allow Vivado Synthesis to choose                                                                            |
// | "distributed"- Distributed memory                                                                                   |
// | "block"- Block memory                                                                                               |
// +---------------------------------------------------------------------------------------------------------------------+
// | MEMORY_SIZE          | Integer            | Range: 2 - 150994944. Default value = 2048.                             |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify the total memory array size, in bits.                                                                       |
// | For example, enter 65536 for a 2kx32 ROM.                                                                           |
// +---------------------------------------------------------------------------------------------------------------------+
// | MESSAGE_CONTROL      | Integer            | Range: 0 - 1. Default value = 0.                                        |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify 1 to enable the dynamic message reporting such as collision warnings, and 0 to disable the message reporting|
// +---------------------------------------------------------------------------------------------------------------------+
// | RAM_DECOMP           | String             | Allowed values: auto, area, power. Default value = auto.                |
// |---------------------------------------------------------------------------------------------------------------------|
// |  Specifies the decomposition of the memory.                                                                         |
// |  "auto" - Synthesis selects default.                                                                                |
// |  "power" - Synthesis selects a strategy to reduce switching activity of RAMs and maps using widest configuration possible.|
// |  "area" - Synthesis selects a strategy to reduce RAM resource count.                                                |
// +---------------------------------------------------------------------------------------------------------------------+
// | READ_DATA_WIDTH_A    | Integer            | Range: 1 - 4608. Default value = 32.                                    |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify the width of the port A read data output port douta, in bits.                                               |
// +---------------------------------------------------------------------------------------------------------------------+
// | READ_DATA_WIDTH_B    | Integer            | Range: 1 - 4608. Default value = 32.                                    |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify the width of the port B read data output port doutb, in bits.                                               |
// +---------------------------------------------------------------------------------------------------------------------+
// | READ_LATENCY_A       | Integer            | Range: 0 - 100. Default value = 2.                                      |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify the number of register stages in the port A read data pipeline. Read data output to port douta takes this   |
// | number of clka cycles.                                                                                              |
// | To target block memory, a value of 1 or larger is required- 1 causes use of memory latch only; 2 causes use of      |
// | output register. To target distributed memory, a value of 0 or larger is required- 0 indicates combinatorial output.|
// | Values larger than 2 synthesize additional flip-flops that are not retimed into memory primitives.                  |
// +---------------------------------------------------------------------------------------------------------------------+
// | READ_LATENCY_B       | Integer            | Range: 0 - 100. Default value = 2.                                      |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify the number of register stages in the port B read data pipeline. Read data output to port doutb takes this   |
// | number of clkb cycles (clka when CLOCKING_MODE is "common_clock").                                                  |
// | To target block memory, a value of 1 or larger is required- 1 causes use of memory latch only; 2 causes use of      |
// | output register. To target distributed memory, a value of 0 or larger is required- 0 indicates combinatorial output.|
// | Values larger than 2 synthesize additional flip-flops that are not retimed into memory primitives.                  |
// +---------------------------------------------------------------------------------------------------------------------+
// | READ_RESET_VALUE_A   | String             | Default value = 0.                                                      |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify the reset value of the port A final output register stage in response to rsta input port is assertion.      |
// | For example, to reset the value of port douta to all 0s when READ_DATA_WIDTH_A is 32, specify 32HHHHh0.             |
// +---------------------------------------------------------------------------------------------------------------------+
// | READ_RESET_VALUE_B   | String             | Default value = 0.                                                      |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify the reset value of the port B final output register stage in response to rstb input port is assertion.      |
// +---------------------------------------------------------------------------------------------------------------------+
// | RST_MODE_A           | String             | Allowed values: SYNC, ASYNC. Default value = SYNC.                      |
// |---------------------------------------------------------------------------------------------------------------------|
// | Describes the behaviour of the reset                                                                                |
// |                                                                                                                     |
// |   "SYNC" - when reset is applied, synchronously resets output port douta to the value specified by parameter READ_RESET_VALUE_A|
// |   "ASYNC" - when reset is applied, asynchronously resets output port douta to zero                                  |
// +---------------------------------------------------------------------------------------------------------------------+
// | RST_MODE_B           | String             | Allowed values: SYNC, ASYNC. Default value = SYNC.                      |
// |---------------------------------------------------------------------------------------------------------------------|
// | Describes the behaviour of the reset                                                                                |
// |                                                                                                                     |
// |   "SYNC" - when reset is applied, synchronously resets output port doutb to the value specified by parameter READ_RESET_VALUE_B|
// |   "ASYNC" - when reset is applied, asynchronously resets output port doutb to zero                                  |
// +---------------------------------------------------------------------------------------------------------------------+
// | SIM_ASSERT_CHK       | Integer            | Range: 0 - 1. Default value = 0.                                        |
// |---------------------------------------------------------------------------------------------------------------------|
// | 0- Disable simulation message reporting. Messages related to potential misuse will not be reported.                 |
// | 1- Enable simulation message reporting. Messages related to potential misuse will be reported.                      |
// +---------------------------------------------------------------------------------------------------------------------+
// | USE_MEM_INIT         | Integer            | Range: 0 - 1. Default value = 1.                                        |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify 1 to enable the generation of below message and 0 to disable generation of the following message completely.|
// | "INFO - MEMORY_INIT_FILE and MEMORY_INIT_PARAM together specifies no memory initialization.                         |
// | Initial memory contents will be all 0s."                                                                            |
// | NOTE: This message gets generated only when there is no Memory Initialization specified either through file or      |
// | Parameter.                                                                                                          |
// +---------------------------------------------------------------------------------------------------------------------+
// | USE_MEM_INIT_MMI     | Integer            | Range: 0 - 1. Default value = 0.                                        |
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify 1 to expose this memory information to be written out in the MMI file.                                      |
// +---------------------------------------------------------------------------------------------------------------------+
// | WAKEUP_TIME          | String             | Allowed values: disable_sleep, use_sleep_pin. Default value = disable_sleep.|
// |---------------------------------------------------------------------------------------------------------------------|
// | Specify "disable_sleep" to disable dynamic power saving option, and specify "use_sleep_pin" to enable the           |
// | dynamic power saving option                                                                                         |
// +---------------------------------------------------------------------------------------------------------------------+

// Port usage table, organized as follows:
// +---------------------------------------------------------------------------------------------------------------------+
// | Port name      | Direction | Size, in bits                         | Domain  | Sense       | Handling if unused     |
// |---------------------------------------------------------------------------------------------------------------------|
// | Description                                                                                                         |
// +---------------------------------------------------------------------------------------------------------------------+
// +---------------------------------------------------------------------------------------------------------------------+
// | addra          | Input     | ADDR_WIDTH_A                          | clka    | NA          | Required               |
// |---------------------------------------------------------------------------------------------------------------------|
// | Address for port A read operations.                                                                                 |
// +---------------------------------------------------------------------------------------------------------------------+
// | addrb          | Input     | ADDR_WIDTH_B                          | clkb    | NA          | Required               |
// |---------------------------------------------------------------------------------------------------------------------|
// | Address for port B read operations.                                                                                 |
// +---------------------------------------------------------------------------------------------------------------------+
// | clka           | Input     | 1                                     | NA      | Rising edge | Required               |
// |---------------------------------------------------------------------------------------------------------------------|
// | Clock signal for port A. Also clocks port B when parameter CLOCKING_MODE is "common_clock".                         |
// +---------------------------------------------------------------------------------------------------------------------+
// | clkb           | Input     | 1                                     | NA      | Rising edge | Required               |
// |---------------------------------------------------------------------------------------------------------------------|
// | Clock signal for port B when parameter CLOCKING_MODE is "independent_clock".                                        |
// | Unused when parameter CLOCKING_MODE is "common_clock".                                                              |
// +---------------------------------------------------------------------------------------------------------------------+
// | dbiterra       | Output    | 1                                     | clka    | Active-high | DoNotCare              |
// |---------------------------------------------------------------------------------------------------------------------|
// | Leave open.                                                                                                         |
// +---------------------------------------------------------------------------------------------------------------------+
// | dbiterrb       | Output    | 1                                     | clkb    | Active-high | DoNotCare              |
// |---------------------------------------------------------------------------------------------------------------------|
// | Leave open.                                                                                                         |
// +---------------------------------------------------------------------------------------------------------------------+
// | douta          | Output    | READ_DATA_WIDTH_A                     | clka    | NA          | Required               |
// |---------------------------------------------------------------------------------------------------------------------|
// | Data output for port A read operations.                                                                             |
// +---------------------------------------------------------------------------------------------------------------------+
// | doutb          | Output    | READ_DATA_WIDTH_B                     | clkb    | NA          | Required               |
// |---------------------------------------------------------------------------------------------------------------------|
// | Data output for port B read operations.                                                                             |
// +---------------------------------------------------------------------------------------------------------------------+
// | ena            | Input     | 1                                     | clka    | Active-high | Required               |
// |---------------------------------------------------------------------------------------------------------------------|
// | Memory enable signal for port A.                                                                                    |
// | Must be high on clock cycles when read operations are initiated. Pipelined internally.                              |
// +---------------------------------------------------------------------------------------------------------------------+
// | enb            | Input     | 1                                     | clkb    | Active-high | Required               |
// |---------------------------------------------------------------------------------------------------------------------|
// | Memory enable signal for port B.                                                                                    |
// | Must be high on clock cycles when read operations are initiated. Pipelined internally.                              |
// +---------------------------------------------------------------------------------------------------------------------+
// | injectdbiterra | Input     | 1                                     | clka    | Active-high | Tie to 1'b0            |
// |---------------------------------------------------------------------------------------------------------------------|
// | Do not change from the provided value.                                                                              |
// +---------------------------------------------------------------------------------------------------------------------+
// | injectdbiterrb | Input     | 1                                     | clkb    | Active-high | Tie to 1'b0            |
// |---------------------------------------------------------------------------------------------------------------------|
// | Do not change from the provided value.                                                                              |
// +---------------------------------------------------------------------------------------------------------------------+
// | injectsbiterra | Input     | 1                                     | clka    | Active-high | Tie to 1'b0            |
// |---------------------------------------------------------------------------------------------------------------------|
// | Do not change from the provided value.                                                                              |
// +---------------------------------------------------------------------------------------------------------------------+
// | injectsbiterrb | Input     | 1                                     | clkb    | Active-high | Tie to 1'b0            |
// |---------------------------------------------------------------------------------------------------------------------|
// | Do not change from the provided value.                                                                              |
// +---------------------------------------------------------------------------------------------------------------------+
// | regcea         | Input     | 1                                     | clka    | Active-high | Tie to 1'b1            |
// |---------------------------------------------------------------------------------------------------------------------|
// | Do not change from the provided value.                                                                              |
// +---------------------------------------------------------------------------------------------------------------------+
// | regceb         | Input     | 1                                     | clkb    | Active-high | Tie to 1'b1            |
// |---------------------------------------------------------------------------------------------------------------------|
// | Do not change from the provided value.                                                                              |
// +---------------------------------------------------------------------------------------------------------------------+
// | rsta           | Input     | 1                                     | clka    | Active-high | Required               |
// |---------------------------------------------------------------------------------------------------------------------|
// | Reset signal for the final port A output register stage.                                                            |
// | Synchronously resets output port douta to the value specified by parameter READ_RESET_VALUE_A.                      |
// +---------------------------------------------------------------------------------------------------------------------+
// | rstb           | Input     | 1                                     | clkb    | Active-high | Required               |
// |---------------------------------------------------------------------------------------------------------------------|
// | Reset signal for the final port B output register stage.                                                            |
// | Synchronously resets output port doutb to the value specified by parameter READ_RESET_VALUE_B.                      |
// +---------------------------------------------------------------------------------------------------------------------+
// | sbiterra       | Output    | 1                                     | clka    | Active-high | DoNotCare              |
// |---------------------------------------------------------------------------------------------------------------------|
// | Leave open.                                                                                                         |
// +---------------------------------------------------------------------------------------------------------------------+
// | sbiterrb       | Output    | 1                                     | clkb    | Active-high | DoNotCare              |
// |---------------------------------------------------------------------------------------------------------------------|
// | Leave open.                                                                                                         |
// +---------------------------------------------------------------------------------------------------------------------+
// | sleep          | Input     | 1                                     | NA      | Active-high | Tie to 1'b0            |
// |---------------------------------------------------------------------------------------------------------------------|
// | sleep signal to enable the dynamic power saving feature.                                                            |
// +---------------------------------------------------------------------------------------------------------------------+

// modified for SpinalHDL 
// NOT a code from Xilinx

case class xpm_memory_dprom_generic(
  ADDR_WIDTH_A: Int = 6,
  ADDR_WIDTH_B: Int = 6,
  AUTO_SLEEP_TIME: Int = 0,//Must be set to 0  
  CASCADE_HEIGHT: Int = 0,
  CLOCKING_MODE: String = "common_clock",
  ECC_BIT_RANGE: String = "7:0",
  ECC_MODE: String = "no_ecc",
  ECC_TYPE: String = "none",
  IGNORE_INIT_SYNTH: Int = 0,
  MEMORY_INIT_FILE: String = "none",
  MEMORY_INIT_PARAM: String = "0",
  MEMORY_OPTIMIZATION: String = "true",
  MEMORY_PRIMITIVE: String = "auto",
  MEMORY_SIZE: Int = 2048,
  MESSAGE_CONTROL: Int = 0,
  RAM_DECOMP: String = "auto",
  READ_DATA_WIDTH_A: Int = 32,
  READ_DATA_WIDTH_B: Int = 32,
  READ_LATENCY_A: Int = 2,
  READ_LATENCY_B: Int = 2,
  READ_RESET_VALUE_A: String = "0",
  READ_RESET_VALUE_B: String = "0",
  RST_MODE_A: String = "SYNC",
  RST_MODE_B: String = "SYNC",
  SIM_ASSERT_CHK: Int = 0,
  USE_MEM_INIT: Int = 1,
  USE_MEM_INIT_MMI: Int = 0,
  WAKEUP_TIME: String = "disable_sleep"
) extends Generic

case class xpm_memory_dprom(generic: xpm_memory_dprom_generic) extends BlackBox {
  val io = new Bundle {
    val dbiterra = out Bool()//Leave open.  
    val dbiterrb = out Bool()//Leave open.  
    val douta = out Bits(generic.READ_DATA_WIDTH_A bits)
    val doutb = out Bits(generic.READ_DATA_WIDTH_B bits)
    val sbiterra = out Bool()//Leave open.  
    val sbiterrb = out Bool()//Leave open.  
    val addra = in UInt(generic.ADDR_WIDTH_A bits)
    val addrb = in UInt(generic.ADDR_WIDTH_B bits)
    val clka = in Bool()
    val clkb = in Bool()
    val ena = in Bool()
    val enb = in Bool()
    val injectdbiterra = in Bool() default False//Do not change from the provided value.
    val injectdbiterrb = in Bool() default False//Do not change from the provided value.
    val injectsbiterra = in Bool() default False//Do not change from the provided value.
    val injectsbiterrb = in Bool() default False//Do not change from the provided value.
    val regcea = in Bool() default True//Do not change from the provided value.
    val regceb = in Bool() default True//Do not change from the provided value.
    val rsta = in Bool()
    val rstb = in Bool()
    val sleep = in Bool() default False//sleep signal to enable the dynamic power saving feature. 
  }
  noIoPrefix() // 去除IO前缀
}

case class dprom_IP_Config(
  READ_DATA_WIDTH_A: Int = 16,
  ADDR_WIDTH_A: Int = 10,
  WRITE_MODE_B: String = "read_first",
  READ_DATA_WIDTH_B: Int = 16,
  ADDR_WIDTH_B: Int = 10
)
case class dprom_IP(cfg: dprom_IP_Config) extends Component {
  val io = new Bundle {
    // Write port A
    val clka = in Bool()
    val rsta = in Bool()
    val ena = in Bool()
    val addra = in UInt(cfg.ADDR_WIDTH_A bits)
    val douta = out UInt(cfg.READ_DATA_WIDTH_A bits)

    // Write port B
    val clkb = in Bool()
    val rstb = in Bool()
    val enb = in Bool()
    val addrb = in UInt(cfg.ADDR_WIDTH_B bits)
    val doutb = out UInt(cfg.READ_DATA_WIDTH_B bits)
  }

  val generic = new xpm_memory_dprom_generic(
    ADDR_WIDTH_A = cfg.ADDR_WIDTH_A,
    ADDR_WIDTH_B = cfg.ADDR_WIDTH_B,
    READ_DATA_WIDTH_A = cfg.READ_DATA_WIDTH_A,
    READ_DATA_WIDTH_B = cfg.READ_DATA_WIDTH_B,
  )

  val instance = xpm_memory_dprom(generic)
  instance.io.ena<>io.ena
  instance.io.addra<>io.addra
  instance.io.clka<>io.clka
  instance.io.rsta<>io.rsta
  instance.io.douta.asUInt<>io.douta
  instance.io.enb<>io.enb
  instance.io.addrb<>io.addrb
  instance.io.clkb<>io.clkb
  instance.io.rstb<>io.rstb
  instance.io.doutb.asUInt<>io.doutb
}

object xpm_memory_dprom_Verilog extends App{
val FileDir = "rtl/xpm_memory_dprom/verilog"
  import java.io.File
  new File(FileDir).mkdirs()
val cfg=new dprom_IP_Config()
  SpinalConfig(
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
    removePruned=false
  ).generateVerilog(new dprom_IP(cfg)).printPruned()
}

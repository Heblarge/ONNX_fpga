# Usage with Vitis IDE:
# In Vitis IDE create a Single Application Debug launch configuration,
# change the debug type to 'Attach to running target' and provide this 
# tcl script in 'Execute Script' option.
# Path of this script: /home/zhou/Desktop/validation_1/vitis/full_system_test_v0_system/_ide/scripts/systemdebugger_full_system_test_v0_system_1_standalone.tcl
# 
# 
# Usage with xsct:
# To debug using xsct, launch xsct and run below command
# source /home/zhou/Desktop/validation_1/vitis/full_system_test_v0_system/_ide/scripts/systemdebugger_full_system_test_v0_system_1_standalone.tcl
# 
connect -url tcp:127.0.0.1:3121
source /tools/Xilinx/Vitis/2024.2/scripts/vitis/util/zynqmp_utils.tcl
targets -set -nocase -filter {name =~"APU*"}
reset_apu
targets -set -nocase -filter {name =~"APU*"}
loadhw -hw /home/zhou/Desktop/validation_1/vitis/full_system_v0/export/full_system_v0/hw/new_system_v1_wrapper.xsa -mem-ranges [list {0x80000000 0xbfffffff} {0x400000000 0x5ffffffff} {0x1000000000 0x7fffffffff}] -regs
configparams force-mem-access 1
targets -set -nocase -filter {name =~"APU*"}
set mode [expr [mrd -value 0xFF5E0200] & 0xf]
targets -set -nocase -filter {name =~ "*A53*#0"}
rst -processor
dow /home/zhou/Desktop/validation_1/vitis/full_system_v0/export/full_system_v0/sw/full_system_v0/boot/fsbl.elf
set bp_23_45_fsbl_bp [bpadd -addr &XFsbl_Exit]
con -block -timeout 60
bpremove $bp_23_45_fsbl_bp
targets -set -nocase -filter {name =~ "*A53*#0"}
rst -processor
dow /home/zhou/Desktop/validation_1/vitis/full_system_test_v0/Debug/full_system_test_v0.elf
configparams force-mem-access 0
targets -set -nocase -filter {name =~ "*A53*#0"}
con

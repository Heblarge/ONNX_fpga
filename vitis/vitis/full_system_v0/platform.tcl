# 
# Usage: To re-create this platform project launch xsct with below options.
# xsct /home/zhou/Desktop/validation_1/vitis/full_system_v0/platform.tcl
# 
# OR launch xsct and run below command.
# source /home/zhou/Desktop/validation_1/vitis/full_system_v0/platform.tcl
# 
# To create the platform in a different location, modify the -out option of "platform create" command.
# -out option specifies the output directory of the platform project.

platform create -name {full_system_v0}\
-hw {/home/zhou/Desktop/validation_1/vitis/new_system_v0_wrapper.xsa}\
-proc {psu_cortexa53_0} -os {standalone} -arch {64-bit} -fsbl-target {psu_cortexa53_0} -out {/home/zhou/Desktop/validation_1/vitis}

platform write
platform generate -domains 
platform generate
platform active {full_system_v0}
platform config -updatehw {/home/zhou/Desktop/validation_1/new_system_v1_wrapper.xsa}
platform active {full_system_v0}
platform generate

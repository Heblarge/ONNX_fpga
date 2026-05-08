simSetSimulator "-vcssv" -exec \
           "/root/onnx_spinal_project_new/simWorkspace/WrapForFPGATest/WrapForFPGATest" \
           -args
debImport "-dbdir" \
          "/root/onnx_spinal_project_new/simWorkspace/WrapForFPGATest/WrapForFPGATest.daidir"
debLoadSimResult \
           /root/onnx_spinal_project_new/simWorkspace/WrapForFPGATest/null_WrapForFPGATest.fsdb
wvCreateWindow
wvGetSignalOpen -win $_nWave2
wvGetSignalSetScope -win $_nWave2 "/WrapForFPGATest"
wvSetPosition -win $_nWave2 {("G1" 4)}
wvSetPosition -win $_nWave2 {("G1" 4)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_valid} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_valid} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 1 2 3 4 )} 
wvSetPosition -win $_nWave2 {("G1" 4)}
wvSetPosition -win $_nWave2 {("G1" 4)}
wvSetPosition -win $_nWave2 {("G1" 4)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_valid} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_valid} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 1 2 3 4 )} 
wvSetPosition -win $_nWave2 {("G1" 4)}
wvGetSignalClose -win $_nWave2
wvZoom -win $_nWave2 130.936487 1032.943398
wvGetSignalOpen -win $_nWave2
wvGetSignalSetScope -win $_nWave2 "/WrapForFPGATest"
wvGetSignalSetScope -win $_nWave2 "/WrapForFPGATest"
wvSetPosition -win $_nWave2 {("G1" 6)}
wvSetPosition -win $_nWave2 {("G1" 6)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_valid} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_valid} \
{/WrapForFPGATest/dut_io_memPortA_Valid} \
{/WrapForFPGATest/dut_io_memPortB_Valid} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 5 6 )} 
wvSetPosition -win $_nWave2 {("G1" 6)}
wvSetPosition -win $_nWave2 {("G1" 6)}
wvSetPosition -win $_nWave2 {("G1" 6)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_valid} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_valid} \
{/WrapForFPGATest/dut_io_memPortA_Valid} \
{/WrapForFPGATest/dut_io_memPortB_Valid} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 5 6 )} 
wvSetPosition -win $_nWave2 {("G1" 6)}
wvGetSignalClose -win $_nWave2
wvGetSignalOpen -win $_nWave2
wvGetSignalSetScope -win $_nWave2 "/WrapForFPGATest"
wvGetSignalSetScope -win $_nWave2 "/WrapForFPGATest"
wvSetPosition -win $_nWave2 {("G1" 7)}
wvSetPosition -win $_nWave2 {("G1" 7)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_valid} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_valid} \
{/WrapForFPGATest/dut_io_memPortA_Valid} \
{/WrapForFPGATest/dut_io_memPortB_Valid} \
{/WrapForFPGATest/SystolicArray2D_CC_core_clk} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 7 )} 
wvSetPosition -win $_nWave2 {("G1" 7)}
wvSetPosition -win $_nWave2 {("G1" 7)}
wvSetPosition -win $_nWave2 {("G1" 7)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_valid} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_valid} \
{/WrapForFPGATest/dut_io_memPortA_Valid} \
{/WrapForFPGATest/dut_io_memPortB_Valid} \
{/WrapForFPGATest/SystolicArray2D_CC_core_clk} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 7 )} 
wvSetPosition -win $_nWave2 {("G1" 7)}
wvGetSignalClose -win $_nWave2
wvSetCursor -win $_nWave2 603.323727 -snap {("G2" 0)}
wvGetSignalOpen -win $_nWave2
wvGetSignalSetScope -win $_nWave2 "/WrapForFPGATest"
wvGetSignalSetScope -win $_nWave2 "/WrapForFPGATest"
wvSetPosition -win $_nWave2 {("G1" 8)}
wvSetPosition -win $_nWave2 {("G1" 8)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_valid} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_valid} \
{/WrapForFPGATest/dut_io_memPortA_Valid} \
{/WrapForFPGATest/dut_io_memPortB_Valid} \
{/WrapForFPGATest/SystolicArray2D_CC_core_clk} \
{/WrapForFPGATest/clk} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 8 )} 
wvSetPosition -win $_nWave2 {("G1" 8)}
wvSetPosition -win $_nWave2 {("G1" 8)}
wvSetPosition -win $_nWave2 {("G1" 8)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_aw_valid} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_ready} \
{/WrapForFPGATest/io_sAxi4LiteInst_w_valid} \
{/WrapForFPGATest/dut_io_memPortA_Valid} \
{/WrapForFPGATest/dut_io_memPortB_Valid} \
{/WrapForFPGATest/SystolicArray2D_CC_core_clk} \
{/WrapForFPGATest/clk} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 8 )} 
wvSetPosition -win $_nWave2 {("G1" 8)}
wvGetSignalClose -win $_nWave2
wvSetCursor -win $_nWave2 487.332896 -snap {("G1" 4)}
wvSetCursor -win $_nWave2 499.644772 -snap {("G1" 8)}
wvSetCursor -win $_nWave2 509.364674 -snap {("G1" 8)}
wvSetCursor -win $_nWave2 479.556974 -snap {("G1" 4)}
wvSetCursor -win $_nWave2 507.420693 -snap {("G1" 5)}
wvSetCursor -win $_nWave2 482.148948 -snap {("G1" 4)}
wvSetCursor -win $_nWave2 489.276876 -snap {("G1" 8)}
wvSetCursor -win $_nWave2 498.348785 -snap {("G1" 8)}
wvSetCursor -win $_nWave2 509.364674 -snap {("G1" 8)}

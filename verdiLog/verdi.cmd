wvConvertFile -o "/root/livehps_1/simWorkspace/FpxxRSqrt/null_FpxxRSqrt.vcd.fsdb" \
           "/root/livehps_1/simWorkspace/FpxxRSqrt/null_FpxxRSqrt.vcd"
debLoadSimResult /root/livehps_1/simWorkspace/FpxxRSqrt/null_FpxxRSqrt.vcd.fsdb
wvSelectGroup -win $_nWave2 {G1}
wvGetSignalOpen -win $_nWave2
wvGetSignalSetScope -win $_nWave2 "/FpxxRSqrt"
wvSetPosition -win $_nWave2 {("G1" 52)}
wvSetPosition -win $_nWave2 {("G1" 52)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/FpxxRSqrt/_zz_n0_exp\[8:0\]} \
{/FpxxRSqrt/_zz_n0_exp_1\[8:0\]} \
{/FpxxRSqrt/_zz_n0_rsqrt_addr\[10:0\]} \
{/FpxxRSqrt/_zz_n0_rsqrt_addr_1\[24:0\]} \
{/FpxxRSqrt/_zz_n1_exp_adj\[8:0\]} \
{/FpxxRSqrt/_zz_n1_exp_adj_1\[8:0\]} \
{/FpxxRSqrt/_zz_n1_exp_adj_2\[8:0\]} \
{/FpxxRSqrt/_zz_n1_exp_adj_3\[8:0\]} \
{/FpxxRSqrt/_zz_n1_exp_adj_4\[8:0\]} \
{/FpxxRSqrt/_zz_n1_exp_final\[8:0\]} \
{/FpxxRSqrt/clk} \
{/FpxxRSqrt/io_op_payload_exp\[7:0\]} \
{/FpxxRSqrt/io_op_payload_mant\[22:0\]} \
{/FpxxRSqrt/io_op_payload_sign} \
{/FpxxRSqrt/io_op_valid} \
{/FpxxRSqrt/io_result_payload_exp\[7:0\]} \
{/FpxxRSqrt/io_result_payload_mant\[22:0\]} \
{/FpxxRSqrt/io_result_payload_sign} \
{/FpxxRSqrt/io_result_valid} \
{/FpxxRSqrt/n0_exp\[8:0\]} \
{/FpxxRSqrt/n0_gt_1} \
{/FpxxRSqrt/n0_isValid} \
{/FpxxRSqrt/n0_op_inf} \
{/FpxxRSqrt/n0_op_nan} \
{/FpxxRSqrt/n0_op_payload_exp\[7:0\]} \
{/FpxxRSqrt/n0_op_payload_mant\[22:0\]} \
{/FpxxRSqrt/n0_op_payload_sign} \
{/FpxxRSqrt/n0_op_valid} \
{/FpxxRSqrt/n0_op_zero} \
{/FpxxRSqrt/n0_rsqrt_addr\[10:0\]} \
{/FpxxRSqrt/n0_valid} \
{/FpxxRSqrt/n0_vld} \
{/FpxxRSqrt/n1_exp_adj\[8:0\]} \
{/FpxxRSqrt/n1_exp_final\[7:0\]} \
{/FpxxRSqrt/n1_isValid} \
{/FpxxRSqrt/n1_mant_final\[22:0\]} \
{/FpxxRSqrt/n1_n0_exp\[8:0\]} \
{/FpxxRSqrt/n1_n0_op_inf} \
{/FpxxRSqrt/n1_n0_op_nan} \
{/FpxxRSqrt/n1_n0_op_payload_exp\[7:0\]} \
{/FpxxRSqrt/n1_n0_op_payload_mant\[22:0\]} \
{/FpxxRSqrt/n1_n0_op_payload_sign} \
{/FpxxRSqrt/n1_n0_op_valid} \
{/FpxxRSqrt/n1_n0_op_zero} \
{/FpxxRSqrt/n1_rsqrt_mant\[22:0\]} \
{/FpxxRSqrt/n1_rsqrt_shift\[1:0\]} \
{/FpxxRSqrt/n1_rsqrt_val\[24:0\]} \
{/FpxxRSqrt/n1_sign_final} \
{/FpxxRSqrt/n1_valid} \
{/FpxxRSqrt/reset} \
{/FpxxRSqrt/rsqrt_table_spinal_port0\[24:0\]} \
{/FpxxRSqrt/when_FpxxRSqrt_l109} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 \
           18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 \
           40 41 42 43 44 45 46 47 48 49 50 51 52 )} 
wvSetPosition -win $_nWave2 {("G1" 52)}
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomIn -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvScrollUp -win $_nWave2 1
wvScrollUp -win $_nWave2 1
wvScrollUp -win $_nWave2 1
wvScrollUp -win $_nWave2 1
wvScrollUp -win $_nWave2 1
wvScrollUp -win $_nWave2 1
wvScrollUp -win $_nWave2 1
wvScrollUp -win $_nWave2 1
wvScrollUp -win $_nWave2 1
debExit

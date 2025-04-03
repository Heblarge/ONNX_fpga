simSetSimulator "-vcssv" -exec \
           "/home/zhou/Workspace/LiveHPS++/SpinalHDL/GeMM/simWorkspace/HadamardProduct/HadamardProduct" \
           -args
debImport "-dbdir" \
          "/home/zhou/Workspace/LiveHPS++/SpinalHDL/GeMM/simWorkspace/HadamardProduct/HadamardProduct.daidir"
debLoadSimResult \
           /home/zhou/Workspace/LiveHPS++/SpinalHDL/GeMM/simWorkspace/HadamardProduct/null_HadamardProduct.fsdb
wvCreateWindow
wvGetSignalOpen -win $_nWave2
wvGetSignalSetScope -win $_nWave2 "/HadamardProduct"
wvSetPosition -win $_nWave2 {("G1" 9)}
wvSetPosition -win $_nWave2 {("G1" 9)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 1 2 3 4 5 6 7 8 9 )} 
wvSetPosition -win $_nWave2 {("G1" 9)}
wvSetPosition -win $_nWave2 {("G1" 9)}
wvSetPosition -win $_nWave2 {("G1" 9)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 1 2 3 4 5 6 7 8 9 )} 
wvSetPosition -win $_nWave2 {("G1" 9)}
wvGetSignalClose -win $_nWave2
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
wvZoomOut -win $_nWave2
wvSetCursor -win $_nWave2 86.250901 -snap {("G2" 0)}
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
wvZoomOut -win $_nWave2
wvSetCursor -win $_nWave2 73.543315 -snap {("G2" 0)}
wvSelectSignal -win $_nWave2 {( "G1" 6 )} 
wvGetSignalOpen -win $_nWave2
wvGetSignalSetScope -win $_nWave2 "/HadamardProduct"
wvGetSignalSetScope -win $_nWave2 "/HadamardProduct"
wvSetCursor -win $_nWave2 20.649311 -snap {("G2" 0)}
wvSelectSignal -win $_nWave2 {( "G1" 9 )} 
wvSetCursor -win $_nWave2 239.373164 -snap {("G1" 8)}
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvSetCursor -win $_nWave2 145.021698 -snap {("G1" 9)}
wvGetSignalOpen -win $_nWave2
wvGetSignalSetScope -win $_nWave2 "/HadamardProduct"
wvGetSignalSetScope -win $_nWave2 "/HadamardProduct"
wvSetPosition -win $_nWave2 {("G1" 10)}
wvSetPosition -win $_nWave2 {("G1" 10)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
{/HadamardProduct/Go} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 10 )} 
wvSetPosition -win $_nWave2 {("G1" 10)}
wvSetPosition -win $_nWave2 {("G1" 10)}
wvSetPosition -win $_nWave2 {("G1" 10)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
{/HadamardProduct/Go} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 10 )} 
wvSetPosition -win $_nWave2 {("G1" 10)}
wvGetSignalClose -win $_nWave2
wvSetCursor -win $_nWave2 245.409117 -snap {("G1" 8)}
wvGetSignalOpen -win $_nWave2
wvGetSignalSetScope -win $_nWave2 "/HadamardProduct"
wvGetSignalSetScope -win $_nWave2 "/HadamardProduct"
wvSetPosition -win $_nWave2 {("G1" 12)}
wvSetPosition -win $_nWave2 {("G1" 12)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
{/HadamardProduct/Go} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/counter\[2:0\]} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 12 )} 
wvSetPosition -win $_nWave2 {("G1" 12)}
wvSetPosition -win $_nWave2 {("G1" 12)}
wvSetPosition -win $_nWave2 {("G1" 12)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
{/HadamardProduct/Go} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/counter\[2:0\]} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 12 )} 
wvSetPosition -win $_nWave2 {("G1" 12)}
wvGetSignalClose -win $_nWave2
wvSetCursor -win $_nWave2 249.856660 -snap {("G1" 11)}
wvSetCursor -win $_nWave2 250.015501 -snap {("G1" 12)}
verdiWindowResize -win $_Verdi_1 "307" "36" "2136" "1393"
verdiWindowResize -win $_Verdi_1 "307" "37" "2136" "1389"
verdiWindowResize -win $_Verdi_1 "307" "37" "2136" "1356"
verdiWindowResize -win $_Verdi_1 "307" "37" "2136" "1340"
verdiWindowResize -win $_Verdi_1 "307" "37" "2136" "1332"
verdiWindowResize -win $_Verdi_1 "307" "37" "2136" "1297"
verdiWindowResize -win $_Verdi_1 "307" "37" "2136" "1299"
verdiWindowResize -win $_Verdi_1 "307" "37" "2136" "1300"
wvGetSignalOpen -win $_nWave2
wvGetSignalSetScope -win $_nWave2 "/HadamardProduct"
wvGetSignalSetScope -win $_nWave2 "/HadamardProduct"
wvSetPosition -win $_nWave2 {("G1" 13)}
wvSetPosition -win $_nWave2 {("G1" 13)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
{/HadamardProduct/Go} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/io_out_Mats_valid} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 13 )} 
wvSetPosition -win $_nWave2 {("G1" 13)}
wvSetPosition -win $_nWave2 {("G1" 13)}
wvSetPosition -win $_nWave2 {("G1" 13)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
{/HadamardProduct/Go} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/io_out_Mats_valid} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
}
wvSelectSignal -win $_nWave2 {( "G1" 13 )} 
wvSetPosition -win $_nWave2 {("G1" 13)}
wvGetSignalClose -win $_nWave2
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvSelectGroup -win $_nWave2 {G2}
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvSetCursor -win $_nWave2 1415.423872 -snap {("G2" 0)}
wvZoom -win $_nWave2 222.112669 897.160977
wvSelectGroup -win $_nWave2 {G2}
wvSetPosition -win $_nWave2 {("G2" 0)}
wvMoveSelected -win $_nWave2
wvSelectSignal -win $_nWave2 {( "G1" 5 6 7 8 9 )} 
wvSetPosition -win $_nWave2 {("G1" 8)}
wvSetPosition -win $_nWave2 {("G1" 9)}
wvSetPosition -win $_nWave2 {("G1" 10)}
wvSetPosition -win $_nWave2 {("G1" 11)}
wvSetPosition -win $_nWave2 {("G1" 12)}
wvSetPosition -win $_nWave2 {("G1" 13)}
wvSetPosition -win $_nWave2 {("G2" 0)}
wvMoveSelected -win $_nWave2
wvSetPosition -win $_nWave2 {("G2" 5)}
wvSetPosition -win $_nWave2 {("G2" 5)}
wvSelectSignal -win $_nWave2 {( "G1" 7 )} 
wvSelectSignal -win $_nWave2 {( "G1" 5 6 7 8 )} 
wvSetPosition -win $_nWave2 {("G1" 7)}
wvSetPosition -win $_nWave2 {("G1" 8)}
wvSetPosition -win $_nWave2 {("G2" 0)}
wvSetPosition -win $_nWave2 {("G2" 1)}
wvSetPosition -win $_nWave2 {("G2" 2)}
wvSetPosition -win $_nWave2 {("G2" 3)}
wvSetPosition -win $_nWave2 {("G2" 4)}
wvSetPosition -win $_nWave2 {("G2" 5)}
wvSetPosition -win $_nWave2 {("G3" 0)}
wvMoveSelected -win $_nWave2
wvSetPosition -win $_nWave2 {("G3" 4)}
wvSetPosition -win $_nWave2 {("G3" 4)}
wvSelectGroup -win $_nWave2 {G4}
wvSelectSignal -win $_nWave2 {( "G3" 1 )} 
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvSetCursor -win $_nWave2 259.333378 -snap {("G2" 5)}
wvSelectSignal -win $_nWave2 {( "G2" 4 )} 
wvSelectSignal -win $_nWave2 {( "G2" 3 )} 
wvSelectSignal -win $_nWave2 {( "G2" 2 )} 
wvSelectSignal -win $_nWave2 {( "G2" 1 )} 
wvSelectSignal -win $_nWave2 {( "G2" 2 )} 
wvSelectSignal -win $_nWave2 {( "G2" 3 )} 
wvSelectSignal -win $_nWave2 {( "G2" 3 )} 
wvSelectSignal -win $_nWave2 {( "G2" 4 )} 
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvSelectSignal -win $_nWave2 {( "G2" 4 )} 
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvGetSignalOpen -win $_nWave2
wvSetPosition -win $_nWave2 {("G3" 5)}
wvSetPosition -win $_nWave2 {("G3" 5)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
}
wvAddSignal -win $_nWave2 -group {"G3" \
{/HadamardProduct/Go} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/io_out_Mats_valid} \
{/HadamardProduct/clk} \
}
wvAddSignal -win $_nWave2 -group {"G4" \
}
wvSelectSignal -win $_nWave2 {( "G3" 5 )} 
wvSetPosition -win $_nWave2 {("G3" 5)}
wvSetPosition -win $_nWave2 {("G3" 5)}
wvSetPosition -win $_nWave2 {("G3" 5)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
}
wvAddSignal -win $_nWave2 -group {"G3" \
{/HadamardProduct/Go} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/io_out_Mats_valid} \
{/HadamardProduct/clk} \
}
wvAddSignal -win $_nWave2 -group {"G4" \
}
wvSelectSignal -win $_nWave2 {( "G3" 5 )} 
wvSetPosition -win $_nWave2 {("G3" 5)}
wvGetSignalClose -win $_nWave2
wvSetPosition -win $_nWave2 {("G3" 4)}
wvSetPosition -win $_nWave2 {("G3" 3)}
wvSetPosition -win $_nWave2 {("G3" 2)}
wvSetPosition -win $_nWave2 {("G3" 1)}
wvSetPosition -win $_nWave2 {("G3" 0)}
wvMoveSelected -win $_nWave2
wvSetPosition -win $_nWave2 {("G3" 0)}
wvSetPosition -win $_nWave2 {("G3" 1)}
wvSelectGroup -win $_nWave2 {G4}
wvGetSignalOpen -win $_nWave2
wvGetSignalSetScope -win $_nWave2 "/HadamardProduct"
wvGetSignalSetScope -win $_nWave2 "/HadamardProduct"
wvSetCursor -win $_nWave2 251.297089 -snap {("G3" 5)}
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvSelectSignal -win $_nWave2 {( "G2" 4 )} 
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvSetCursor -win $_nWave2 252.508672 -snap {("G2" 5)}
wvSelectGroup -win $_nWave2 {G4}
wvGetSignalOpen -win $_nWave2
wvSetPosition -win $_nWave2 {("G3" 3)}
wvSetPosition -win $_nWave2 {("G3" 3)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
}
wvAddSignal -win $_nWave2 -group {"G3" \
{/HadamardProduct/clk} \
{/HadamardProduct/io_in_Mats_ready} \
{/HadamardProduct/io_in_Mats_valid} \
{/HadamardProduct/Go} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/io_out_Mats_valid} \
}
wvAddSignal -win $_nWave2 -group {"G4" \
}
wvSelectSignal -win $_nWave2 {( "G3" 2 3 )} 
wvSetPosition -win $_nWave2 {("G3" 3)}
wvSetPosition -win $_nWave2 {("G3" 3)}
wvSetPosition -win $_nWave2 {("G3" 3)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
}
wvAddSignal -win $_nWave2 -group {"G3" \
{/HadamardProduct/clk} \
{/HadamardProduct/io_in_Mats_ready} \
{/HadamardProduct/io_in_Mats_valid} \
{/HadamardProduct/Go} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/io_out_Mats_valid} \
}
wvAddSignal -win $_nWave2 -group {"G4" \
}
wvSelectSignal -win $_nWave2 {( "G3" 2 3 )} 
wvSetPosition -win $_nWave2 {("G3" 3)}
wvGetSignalClose -win $_nWave2
wvSelectSignal -win $_nWave2 {( "G3" 2 3 )} 
wvSetPosition -win $_nWave2 {("G3" 2)}
wvSetPosition -win $_nWave2 {("G3" 1)}
wvSetPosition -win $_nWave2 {("G3" 0)}
wvSetPosition -win $_nWave2 {("G2" 5)}
wvSetPosition -win $_nWave2 {("G3" 0)}
wvSetPosition -win $_nWave2 {("G3" 1)}
wvSetPosition -win $_nWave2 {("G3" 2)}
wvSetPosition -win $_nWave2 {("G3" 3)}
wvSetPosition -win $_nWave2 {("G3" 4)}
wvSetPosition -win $_nWave2 {("G3" 5)}
wvSetPosition -win $_nWave2 {("G3" 6)}
wvSetPosition -win $_nWave2 {("G3" 7)}
wvSetPosition -win $_nWave2 {("G4" 0)}
wvMoveSelected -win $_nWave2
wvSetPosition -win $_nWave2 {("G4" 2)}
wvSetPosition -win $_nWave2 {("G4" 2)}
wvSelectGroup -win $_nWave2 {G5}
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvReloadFile -win $_nWave2
wvSetCursor -win $_nWave2 287.614567 -snap {("G4" 2)}
wvSetCursor -win $_nWave2 34.682933 -snap {("G2" 2)}
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
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
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvZoom -win $_nWave2 95.427914 598.014925
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvSelectSignal -win $_nWave2 {( "G2" 4 )} 
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvSelectSignal -win $_nWave2 {( "G2" 4 )} 
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvSetCursor -win $_nWave2 294.766222 -snap {("G4" 2)}
wvSetCursor -win $_nWave2 197.217688 -snap {("G4" 2)}
wvSetCursor -win $_nWave2 292.645602 -snap {("G4" 2)}
wvSetCursor -win $_nWave2 434.727162 -snap {("G4" 2)}
wvSetCursor -win $_nWave2 568.326241 -snap {("G4" 2)}
wvSetCursor -win $_nWave2 640.427331 -snap {("G4" 2)}
wvSetCursor -win $_nWave2 729.493383 -snap {("G4" 2)}
wvSetCursor -win $_nWave2 746.458346 -snap {("G4" 2)}
wvZoom -win $_nWave2 116.634117 1310.543346
wvSetCursor -win $_nWave2 193.684650 -snap {("G3" 2)}
wvSetCursor -win $_nWave2 207.897855 -snap {("G3" 2)}
wvSetCursor -win $_nWave2 201.165284 -snap {("G3" 2)}
wvSetCursor -win $_nWave2 210.890108 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 290.932895 -snap {("G4" 1)}
wvSetCursor -win $_nWave2 299.161593 -snap {("G4" 1)}
wvSetCursor -win $_nWave2 207.897855 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 198.173030 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 490.665830 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 502.634845 -snap {("G3" 3)}
wvSetCursor -win $_nWave2 578.937314 -snap {("G3" 3)}
wvSelectSignal -win $_nWave2 {( "G3" 2 )} 
wvSetCursor -win $_nWave2 209.393981 -snap {("G3" 2)}
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvSelectSignal -win $_nWave2 {( "G2" 4 )} 
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvSetCursor -win $_nWave2 210.142045 -snap {("G2" 5)}
wvSelectSignal -win $_nWave2 {( "G2" 3 )} 
wvSelectSignal -win $_nWave2 {( "G2" 4 )} 
wvSelectSignal -win $_nWave2 {( "G2" 1 2 3 4 5 )} 
wvSelectSignal -win $_nWave2 {( "G2" 1 )} 
wvSelectSignal -win $_nWave2 {( "G2" 1 2 3 4 )} 
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvSelectSignal -win $_nWave2 {( "G2" 4 )} 
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvSelectSignal -win $_nWave2 {( "G3" 1 )} 
wvSelectSignal -win $_nWave2 {( "G3" 2 )} 
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
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
wvSetActiveFile -win $_nWave2 -applyAnnotation off \
           {/home/zhou/Workspace/LiveHPS++/SpinalHDL/GeMM/simWorkspace/HadamardProduct/null_HadamardProduct.fsdb}
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoom -win $_nWave2 145.184969 711.644355
wvSetCursor -win $_nWave2 240.304715 -snap {("G2" 4)}
wvSetCursor -win $_nWave2 419.186627 -snap {("G2" 4)}
wvSelectSignal -win $_nWave2 {( "G3" 2 )} 
wvZoomOut -win $_nWave2
wvSetCursor -win $_nWave2 448.624476 -snap {("G2" 4)}
wvSetCursor -win $_nWave2 430.168406 -snap {("G3" 3)}
wvSetCursor -win $_nWave2 439.396441 -snap {("G3" 2)}
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvZoom -win $_nWave2 131.322038 355.634276
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvSetCursor -win $_nWave2 200.138237 -snap {("G2" 5)}
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvSetCursor -win $_nWave2 353.615032 -snap {("G3" 2)}
wvSetCursor -win $_nWave2 366.545311 -snap {("G3" 2)}
wvSetCursor -win $_nWave2 359.799078 -snap {("G3" 3)}
wvSetCursor -win $_nWave2 371.604986 -snap {("G3" 3)}
wvSetCursor -win $_nWave2 368.231869 -snap {("G3" 3)}
wvSetCursor -win $_nWave2 73.646374 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 35.417722 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 1.686558 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 237.804703 -snap {("G2" 5)}
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvSetCursor -win $_nWave2 251.297169 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 340.684752 -snap {("G3" 2)}
wvSetCursor -win $_nWave2 351.928474 -snap {("G3" 2)}
wvSetCursor -win $_nWave2 341.246939 -snap {("G3" 2)}
wvSetCursor -win $_nWave2 348.555357 -snap {("G3" 2)}
wvSetCursor -win $_nWave2 341.809125 -snap {("G3" 2)}
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvSetCursor -win $_nWave2 219.252563 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 211.381959 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 219.252563 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 210.257586 -snap {("G4" 0)}
wvSetCursor -win $_nWave2 208.571028 -snap {("G4" 1)}
wvSetCursor -win $_nWave2 217.566005 -snap {("G3" 4)}
wvSetCursor -win $_nWave2 223.750052 -snap {("G3" 4)}
wvSetCursor -win $_nWave2 235.555959 -snap {("G3" 4)}
wvSetCursor -win $_nWave2 391.281498 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 415.455498 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 390.719312 -snap {("G3" 1)}
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvSetCursor -win $_nWave2 351.366288 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 358.674706 -snap {("G3" 3)}
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvSetCursor -win $_nWave2 291.774565 -snap {("G2" 5)}
wvSelectGroup -win $_nWave2 {G5}
wvGetSignalOpen -win $_nWave2
wvGetSignalSetScope -win $_nWave2 "/HadamardProduct"
wvSetPosition -win $_nWave2 {("G4" 3)}
wvSetPosition -win $_nWave2 {("G4" 3)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
}
wvAddSignal -win $_nWave2 -group {"G3" \
{/HadamardProduct/clk} \
{/HadamardProduct/Go} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/io_out_Mats_valid} \
}
wvAddSignal -win $_nWave2 -group {"G4" \
{/HadamardProduct/io_in_Mats_ready} \
{/HadamardProduct/io_in_Mats_valid} \
{/HadamardProduct/data_not_received} \
}
wvAddSignal -win $_nWave2 -group {"G5" \
}
wvSelectSignal -win $_nWave2 {( "G4" 3 )} 
wvSetPosition -win $_nWave2 {("G4" 3)}
wvSetPosition -win $_nWave2 {("G4" 3)}
wvSetPosition -win $_nWave2 {("G4" 3)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
}
wvAddSignal -win $_nWave2 -group {"G3" \
{/HadamardProduct/clk} \
{/HadamardProduct/Go} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/io_out_Mats_valid} \
}
wvAddSignal -win $_nWave2 -group {"G4" \
{/HadamardProduct/io_in_Mats_ready} \
{/HadamardProduct/io_in_Mats_valid} \
{/HadamardProduct/data_not_received} \
}
wvAddSignal -win $_nWave2 -group {"G5" \
}
wvSelectSignal -win $_nWave2 {( "G4" 3 )} 
wvSetPosition -win $_nWave2 {("G4" 3)}
wvGetSignalClose -win $_nWave2
wvGetSignalOpen -win $_nWave2
wvGetSignalSetScope -win $_nWave2 "/HadamardProduct"
wvGetSignalSetScope -win $_nWave2 "/HadamardProduct"
wvSetPosition -win $_nWave2 {("G4" 5)}
wvSetPosition -win $_nWave2 {("G4" 5)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
}
wvAddSignal -win $_nWave2 -group {"G3" \
{/HadamardProduct/clk} \
{/HadamardProduct/Go} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/io_out_Mats_valid} \
}
wvAddSignal -win $_nWave2 -group {"G4" \
{/HadamardProduct/io_in_Mats_ready} \
{/HadamardProduct/io_in_Mats_valid} \
{/HadamardProduct/data_not_received} \
{/HadamardProduct/io_out_Mats_ready} \
{/HadamardProduct/io_out_Mats_valid} \
}
wvAddSignal -win $_nWave2 -group {"G5" \
}
wvSelectSignal -win $_nWave2 {( "G4" 4 5 )} 
wvSetPosition -win $_nWave2 {("G4" 5)}
wvSetPosition -win $_nWave2 {("G4" 5)}
wvSetPosition -win $_nWave2 {("G4" 5)}
wvAddSignal -win $_nWave2 -clear
wvAddSignal -win $_nWave2 -group {"G1" \
{/HadamardProduct/io_in_Mats_payload_A_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_A_3\[7:0\]} \
}
wvAddSignal -win $_nWave2 -group {"G2" \
{/HadamardProduct/io_in_Mats_payload_B_0\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_1\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_2\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_B_3\[7:0\]} \
{/HadamardProduct/io_in_Mats_payload_Final} \
}
wvAddSignal -win $_nWave2 -group {"G3" \
{/HadamardProduct/clk} \
{/HadamardProduct/Go} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/counter\[2:0\]} \
{/HadamardProduct/io_out_Mats_valid} \
}
wvAddSignal -win $_nWave2 -group {"G4" \
{/HadamardProduct/io_in_Mats_ready} \
{/HadamardProduct/io_in_Mats_valid} \
{/HadamardProduct/data_not_received} \
{/HadamardProduct/io_out_Mats_ready} \
{/HadamardProduct/io_out_Mats_valid} \
}
wvAddSignal -win $_nWave2 -group {"G5" \
}
wvSelectSignal -win $_nWave2 {( "G4" 4 5 )} 
wvSetPosition -win $_nWave2 {("G4" 5)}
wvGetSignalClose -win $_nWave2
wvSelectSignal -win $_nWave2 {( "G4" 4 5 )} 
wvSetPosition -win $_nWave2 {("G5" 0)}
wvMoveSelected -win $_nWave2
wvSetPosition -win $_nWave2 {("G5" 2)}
wvSetPosition -win $_nWave2 {("G5" 2)}
wvSelectGroup -win $_nWave2 {G6}
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvUnknownSaveResult -win $_nWave2 -clear
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
wvZoomOut -win $_nWave2
wvSetCursor -win $_nWave2 45.587957 -snap {("G3" 0)}
wvSetCursor -win $_nWave2 169.408515 -snap {("G3" 1)}
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
verdiWindowResize -win $_Verdi_1 "2744" -21 "2136" "1299"
verdiWindowResize -win $_Verdi_1 "2744" -16 "2136" "1295"
verdiWindowResize -win $_Verdi_1 "2744" -16 "2136" "1294"
verdiWindowResize -win $_Verdi_1 "2744" -16 "2136" "1288"
verdiWindowResize -win $_Verdi_1 "2744" -16 "2136" "1281"
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
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
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvSetCursor -win $_nWave2 219.465533 -snap {("G2" 4)}
wvSetCursor -win $_nWave2 211.687393 -snap {("G2" 4)}
wvSelectSignal -win $_nWave2 {( "G5" 2 )} 
wvSelectSignal -win $_nWave2 {( "G5" 1 )} 
wvSelectSignal -win $_nWave2 {( "G5" 2 )} 
wvSetCursor -win $_nWave2 230.141410 -snap {("G5" 2)}
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvSetCursor -win $_nWave2 170.095771 -snap {("G4" 1)}
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
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
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvSetCursor -win $_nWave2 270.154117 -snap {("G2" 3)}
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
wvGetSignalOpen -win $_nWave2
wvGetSignalClose -win $_nWave2
wvSelectSignal -win $_nWave2 {( "G4" 1 )} 
wvSelectSignal -win $_nWave2 {( "G4" 2 )} 
wvSelectSignal -win $_nWave2 {( "G4" 1 )} 
wvSelectSignal -win $_nWave2 {( "G4" 2 )} 
wvSelectSignal -win $_nWave2 {( "G4" 1 )} 
wvSelectSignal -win $_nWave2 {( "G4" 2 )} 
wvSelectSignal -win $_nWave2 {( "G4" 1 )} 
wvSelectSignal -win $_nWave2 {( "G4" 1 )} 
wvSelectSignal -win $_nWave2 {( "G4" 2 )} 
wvSelectSignal -win $_nWave2 {( "G4" 1 )} 
wvSelectSignal -win $_nWave2 {( "G4" 2 )} 
wvSelectSignal -win $_nWave2 {( "G4" 1 )} 
wvSelectSignal -win $_nWave2 {( "G4" 1 )} 
wvSelectSignal -win $_nWave2 {( "G4" 2 )} 
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
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
wvSetCursor -win $_nWave2 279.876316 -snap {("G4" 2)}
wvSetCursor -win $_nWave2 269.436485 -snap {("G4" 2)}
wvSetCursor -win $_nWave2 280.986936 -snap {("G4" 2)}
wvSetCursor -win $_nWave2 310.085188 -snap {("G4" 2)}
wvSetCursor -win $_nWave2 318.525902 -snap {("G4" 2)}
wvSetCursor -win $_nWave2 330.076353 -snap {("G4" 2)}
wvSelectSignal -win $_nWave2 {( "G5" 1 2 )} 
wvSetCursor -win $_nWave2 78.409793 -snap {("G3" 4)}
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
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
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
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvSetCursor -win $_nWave2 300.524361 -snap {("G3" 4)}
wvSetCursor -win $_nWave2 310.115564 -snap {("G5" 2)}
wvSetCursor -win $_nWave2 303.188584 -snap {("G5" 2)}
wvSelectSignal -win $_nWave2 {( "G2" 5 )} 
wvSetCursor -win $_nWave2 290.933158 -snap {("G2" 5)}
wvSetCursor -win $_nWave2 300.790783 -snap {("G3" 3)}
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
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
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvDisplayGridCount -win $_nWave2 -off
wvGetSignalClose -win $_nWave2
wvReloadFile -win $_nWave2
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
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvZoomOut -win $_nWave2
wvSelectStuckSignals -win $_nWave2
wvSelectGroup -win $_nWave2 {G6}
verdiWindowResize -win $_Verdi_1 "2714" "48" "2136" "1283"
verdiWindowResize -win $_Verdi_1 "2714" "46" "2136" "1285"
verdiWindowResize -win $_Verdi_1 "2714" "44" "2136" "1278"
verdiWindowResize -win $_Verdi_1 "2714" "78" "2136" "1230"
verdiWindowResize -win $_Verdi_1 "2714" "122" "2136" "1163"
verdiWindowResize -win $_Verdi_1 "2714" "122" "2136" "1108"
verdiWindowResize -win $_Verdi_1 "2714" "122" "2136" "1101"
verdiWindowResize -win $_Verdi_1 "2714" "122" "2136" "1103"
verdiWindowResize -win $_Verdi_1 "2714" "122" "2136" "1129"
verdiWindowResize -win $_Verdi_1 "2714" "122" "2136" "1362"
verdiWindowResize -win $_Verdi_1 "2714" "122" "2136" "1354"
debExit

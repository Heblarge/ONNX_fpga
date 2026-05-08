
`timescale 1ns/1ns
module __simulation_def;
initial begin
  $fsdbDumpfile("null_InstAxi4ToStream128.fsdb");
  $fsdbDumpvars(0, InstAxi4ToStream128);
  $fsdbDumpflush;
end
endmodule
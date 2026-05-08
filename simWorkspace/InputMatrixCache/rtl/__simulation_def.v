
`timescale 1ns/1ps
module __simulation_def;
initial begin
  $fsdbDumpfile("null_InputMatrixCache.fsdb");
  $fsdbDumpvars(0, InputMatrixCache);
  $fsdbDumpflush;
end
endmodule
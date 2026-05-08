
`timescale 1ns/1ns
module __simulation_def;
initial begin
  $fsdbDumpfile("null_WrapForFPGATest.fsdb");
  $fsdbDumpvars(0, WrapForFPGATest);
  $fsdbDumpflush;
end
endmodule
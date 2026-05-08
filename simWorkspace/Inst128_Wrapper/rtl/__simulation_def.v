
`timescale 1ns/1ns
module __simulation_def;
initial begin
  $fsdbDumpfile("null_Inst128_Wrapper.fsdb");
  $fsdbDumpvars(0, Inst128_Wrapper);
  $fsdbDumpflush;
end
endmodule
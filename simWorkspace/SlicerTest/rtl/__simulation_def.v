
`timescale 1ns/1ns
module __simulation_def;
initial begin
  $fsdbDumpfile("null_SlicerTest.fsdb");
  $fsdbDumpvars(0, SlicerTest);
  $fsdbDumpflush;
end
endmodule
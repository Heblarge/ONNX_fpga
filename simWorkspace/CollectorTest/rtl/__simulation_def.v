
`timescale 1ns/1ns
module __simulation_def;
initial begin
  $fsdbDumpfile("null_CollectorTest.fsdb");
  $fsdbDumpvars(0, CollectorTest);
  $fsdbDumpflush;
end
endmodule
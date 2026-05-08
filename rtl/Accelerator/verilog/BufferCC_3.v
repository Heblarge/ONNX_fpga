// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : BufferCC_3
// Git hash  : e7984c6fd05794f7617e004bdecebf29e04e3020

`timescale 1ns/1ps 
module BufferCC_3 (
  input  wire [6:0]    io_dataIn,
  output wire [6:0]    io_dataOut,
  input  wire          SystolicArray2D_CC_core_clk,
  input  wire          SystolicArray2D_CC_core_reset
);

  (* async_reg = "true" , altera_attribute = "-name ADV_NETLIST_OPT_ALLOWED NEVER_ALLOW" *) reg        [6:0]    buffers_0;
  (* async_reg = "true" *) reg        [6:0]    buffers_1;

  assign io_dataOut = buffers_1;
  always @(posedge SystolicArray2D_CC_core_clk or posedge SystolicArray2D_CC_core_reset) begin
    if(SystolicArray2D_CC_core_reset) begin
      buffers_0 <= 7'h0;
      buffers_1 <= 7'h0;
    end else begin
      buffers_0 <= io_dataIn;
      buffers_1 <= buffers_0;
    end
  end


endmodule

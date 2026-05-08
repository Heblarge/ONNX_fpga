// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : BufferCC
// Git hash  : e7984c6fd05794f7617e004bdecebf29e04e3020

`timescale 1ns/1ps 
module BufferCC (
  input  wire [1:0]    io_dataIn,
  output wire [1:0]    io_dataOut,
  input  wire          clk,
  input  wire          reset
);

  (* async_reg = "true" , altera_attribute = "-name ADV_NETLIST_OPT_ALLOWED NEVER_ALLOW" *) reg        [1:0]    buffers_0;
  (* async_reg = "true" *) reg        [1:0]    buffers_1;

  assign io_dataOut = buffers_1;
  always @(posedge clk or posedge reset) begin
    if(reset) begin
      buffers_0 <= 2'b00;
      buffers_1 <= 2'b00;
    end else begin
      buffers_0 <= io_dataIn;
      buffers_1 <= buffers_0;
    end
  end


endmodule

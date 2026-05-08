// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : SdpramModel
// Git hash  : 033f13503c7527489599352f8721a18e89b6d190

`timescale 1ns/1ps 
module SdpramModel (
  input  wire          io_read_Valid,
  input  wire [4:0]    io_read_Address,
  output wire [15:0]   io_read_Data,
  input  wire          io_write_Valid,
  input  wire [4:0]    io_write_Address,
  input  wire [15:0]   io_write_Data,
  input  wire          clk,
  input  wire          resetn
);

  reg        [15:0]   mem_spinal_port1;
  reg [15:0] mem [0:31];

  always @(posedge clk) begin
    if(io_write_Valid) begin
      mem[io_write_Address] <= io_write_Data;
    end
  end

  always @(posedge clk) begin
    if(io_read_Valid) begin
      mem_spinal_port1 <= mem[io_read_Address];
    end
  end

  assign io_read_Data = mem_spinal_port1;

endmodule

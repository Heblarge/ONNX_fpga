// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : SdpramModel
// Git hash  : a9cbd96b00c1311cf49010ec546f8110a9a55e92

`timescale 1ns/1ps 
module SdpramModel (
  input  wire          io_read_clk,
  input  wire          io_read_rst,
  input  wire          io_read_Valid,
  input  wire [4:0]    io_read_Address,
  output wire [7:0]    io_read_Data,
  input  wire          io_write_clk,
  input  wire          io_write_rst,
  input  wire [0:0]    io_write_Wen,
  input  wire          io_write_Valid,
  input  wire [4:0]    io_write_Address,
  input  wire [7:0]    io_write_Data,
  input  wire          clk,
  input  wire          resetn
);

  reg        [7:0]    mem_spinal_port1;
  wire       [4:0]    _zz_io_read_Data;
  reg [7:0] mem [0:31];

  always @(posedge clk) begin
    if(io_write_Valid) begin
      mem[io_write_Address] <= io_write_Data;
    end
  end

  always @(posedge clk) begin
    if(io_read_Valid) begin
      mem_spinal_port1 <= mem[_zz_io_read_Data];
    end
  end

  assign _zz_io_read_Data = io_read_Address;
  assign io_read_Data = mem_spinal_port1;

endmodule

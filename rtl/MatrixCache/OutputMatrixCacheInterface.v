// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : OutputMatrixCacheInterface
// Git hash  : a9cbd96b00c1311cf49010ec546f8110a9a55e92

`timescale 1ns/1ps 
module OutputMatrixCacheInterface (
  input  wire          io_read_clk,
  input  wire          io_read_rst,
  input  wire          io_read_Valid,
  input  wire [3:0]    io_read_Address,
  output wire [7:0]    io_read_Data,
  input  wire          io_write_clk,
  input  wire          io_write_rst,
  input  wire [0:0]    io_write_Wen,
  input  wire          io_write_Valid,
  input  wire [3:0]    io_write_Address,
  input  wire [7:0]    io_write_Data,
  input  wire          io_switch,
  input  wire          io_dmaIntr,
  output wire          io_status,
  output wire          io_intr,
  input  wire          io_intrClear,
  output wire          io_full,
  output wire          io_read_sdpram_clk,
  output wire          io_read_sdpram_rst,
  output wire          io_read_sdpram_Valid,
  output wire [4:0]    io_read_sdpram_Address,
  input  wire [7:0]    io_read_sdpram_Data,
  output wire          io_write_sdpram_clk,
  output wire          io_write_sdpram_rst,
  output wire [0:0]    io_write_sdpram_Wen,
  output wire          io_write_sdpram_Valid,
  output wire [4:0]    io_write_sdpram_Address,
  output wire [7:0]    io_write_sdpram_Data,
  input  wire          clk,
  input  wire          resetn
);

  reg                 _zz_acceptRelease;
  reg        [0:0]    wrPtr;
  reg        [0:0]    rdPtr;
  reg                 intrReg;
  reg                 bankValid_0;
  reg                 bankValid_1;
  wire                acceptSwitch;
  wire       [1:0]    _zz_1;
  reg                 io_dmaIntr_regNext;
  wire       [1:0]    _zz_2;
  wire                acceptRelease;
  wire       [4:0]    wrAddrInt;
  wire       [4:0]    rdAddrInt;

  always @(*) begin
    case(rdPtr)
      1'b0 : _zz_acceptRelease = bankValid_0;
      default : _zz_acceptRelease = bankValid_1;
    endcase
  end

  assign io_status = (bankValid_0 || bankValid_1);
  assign io_full = (bankValid_0 && bankValid_1);
  assign io_intr = intrReg;
  assign acceptSwitch = (io_switch && (! io_full));
  assign _zz_1 = ({1'd0,1'b1} <<< wrPtr);
  assign _zz_2 = ({1'd0,1'b1} <<< rdPtr);
  assign acceptRelease = ((io_dmaIntr && (! io_dmaIntr_regNext)) && _zz_acceptRelease);
  assign wrAddrInt = {wrPtr,io_write_Address};
  assign io_write_sdpram_Valid = io_write_Valid;
  assign io_write_sdpram_Address = wrAddrInt;
  assign io_write_sdpram_Data = io_write_Data;
  assign io_write_sdpram_clk = io_write_clk;
  assign io_write_sdpram_rst = io_write_rst;
  assign io_write_sdpram_Wen = io_write_Wen;
  assign rdAddrInt = {rdPtr,io_read_Address};
  assign io_read_sdpram_Valid = io_read_Valid;
  assign io_read_sdpram_clk = io_read_clk;
  assign io_read_sdpram_rst = io_read_rst;
  assign io_read_sdpram_Address = rdAddrInt;
  assign io_read_Data = io_read_sdpram_Data;
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      wrPtr <= 1'b0;
      rdPtr <= 1'b0;
      intrReg <= 1'b0;
      bankValid_0 <= 1'b0;
      bankValid_1 <= 1'b0;
    end else begin
      if(io_intrClear) begin
        intrReg <= 1'b0;
      end
      if(acceptSwitch) begin
        if(_zz_1[0]) begin
          bankValid_0 <= 1'b1;
        end
        if(_zz_1[1]) begin
          bankValid_1 <= 1'b1;
        end
        wrPtr <= (wrPtr + 1'b1);
        intrReg <= 1'b1;
      end
      if(acceptRelease) begin
        if(_zz_2[0]) begin
          bankValid_0 <= 1'b0;
        end
        if(_zz_2[1]) begin
          bankValid_1 <= 1'b0;
        end
        rdPtr <= (rdPtr + 1'b1);
      end
    end
  end

  always @(posedge clk) begin
    io_dmaIntr_regNext <= io_dmaIntr;
  end


endmodule

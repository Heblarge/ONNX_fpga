// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : InputMatrixCacheInterface
// Git hash  : 033f13503c7527489599352f8721a18e89b6d190

`timescale 1ns/1ps 
module InputMatrixCacheInterface (
  input  wire          io_read_Valid,
  input  wire [3:0]    io_read_Address,
  output wire [15:0]   io_read_Data,
  input  wire          io_write_Valid,
  input  wire [3:0]    io_write_Address,
  input  wire [15:0]   io_write_Data,
  input  wire          io_switch,
  input  wire          io_dmaIntr,
  input  wire [7:0]    io_lifeCfg,
  output wire          io_status,
  output wire          io_empty,
  output wire          io_read_sdpram_Valid,
  output wire [4:0]    io_read_sdpram_Address,
  input  wire [15:0]   io_read_sdpram_Data,
  output wire          io_write_sdpram_Valid,
  output wire [4:0]    io_write_sdpram_Address,
  output wire [15:0]   io_write_sdpram_Data,
  input  wire          clk,
  input  wire          resetn
);

  reg                 _zz_when_InputMatrixCache_l97;
  reg                 _zz_when_InputMatrixCache_l104;
  reg        [7:0]    _zz__zz_when_InputMatrixCache_l106;
  reg        [0:0]    wrPtr;
  reg        [0:0]    rdPtr;
  reg                 bankValid_0;
  reg                 bankValid_1;
  reg        [7:0]    lifeCnt_0;
  reg        [7:0]    lifeCnt_1;
  wire                anyFree;
  wire                allEmpty;
  reg                 io_dmaIntr_regNext;
  wire       [1:0]    _zz_1;
  wire                when_InputMatrixCache_l97;
  wire       [1:0]    _zz_2;
  wire       [1:0]    _zz_3;
  wire                when_InputMatrixCache_l104;
  wire       [7:0]    _zz_when_InputMatrixCache_l106;
  wire       [1:0]    _zz_4;
  wire                _zz_5;
  wire                _zz_6;
  wire                when_InputMatrixCache_l106;
  wire       [7:0]    _zz_lifeCnt_0;
  wire       [4:0]    wrAddrInt;
  wire       [4:0]    rdAddrInt;

  always @(*) begin
    case(wrPtr)
      1'b0 : _zz_when_InputMatrixCache_l97 = bankValid_0;
      default : _zz_when_InputMatrixCache_l97 = bankValid_1;
    endcase
  end

  always @(*) begin
    case(rdPtr)
      1'b0 : begin
        _zz_when_InputMatrixCache_l104 = bankValid_0;
        _zz__zz_when_InputMatrixCache_l106 = lifeCnt_0;
      end
      default : begin
        _zz_when_InputMatrixCache_l104 = bankValid_1;
        _zz__zz_when_InputMatrixCache_l106 = lifeCnt_1;
      end
    endcase
  end

  assign anyFree = ((! bankValid_0) || (! bankValid_1));
  assign allEmpty = ((! bankValid_0) && (! bankValid_1));
  assign io_status = anyFree;
  assign io_empty = allEmpty;
  assign _zz_1 = ({1'd0,1'b1} <<< wrPtr);
  assign when_InputMatrixCache_l97 = ((io_dmaIntr && (! io_dmaIntr_regNext)) && (! _zz_when_InputMatrixCache_l97));
  assign _zz_2 = ({1'd0,1'b1} <<< wrPtr);
  assign _zz_3 = ({1'd0,1'b1} <<< rdPtr);
  assign when_InputMatrixCache_l104 = (io_switch && _zz_when_InputMatrixCache_l104);
  assign _zz_when_InputMatrixCache_l106 = _zz__zz_when_InputMatrixCache_l106;
  assign _zz_4 = ({1'd0,1'b1} <<< rdPtr);
  assign _zz_5 = _zz_4[0];
  assign _zz_6 = _zz_4[1];
  assign when_InputMatrixCache_l106 = (8'h01 < _zz_when_InputMatrixCache_l106);
  assign _zz_lifeCnt_0 = (_zz_when_InputMatrixCache_l106 - 8'h01);
  assign wrAddrInt = {wrPtr[0],io_write_Address};
  assign rdAddrInt = {rdPtr[0],io_read_Address};
  assign io_write_sdpram_Valid = io_write_Valid;
  assign io_write_sdpram_Address = wrAddrInt;
  assign io_write_sdpram_Data = io_write_Data;
  assign io_read_sdpram_Valid = io_read_Valid;
  assign io_read_sdpram_Address = rdAddrInt;
  assign io_read_Data = io_read_sdpram_Data;
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      wrPtr <= 1'b0;
      rdPtr <= 1'b0;
      bankValid_0 <= 1'b0;
      bankValid_1 <= 1'b0;
      lifeCnt_0 <= 8'h0;
      lifeCnt_1 <= 8'h0;
    end else begin
      if(when_InputMatrixCache_l97) begin
        if(_zz_1[0]) begin
          bankValid_0 <= 1'b1;
        end
        if(_zz_1[1]) begin
          bankValid_1 <= 1'b1;
        end
        if(_zz_2[0]) begin
          lifeCnt_0 <= io_lifeCfg;
        end
        if(_zz_2[1]) begin
          lifeCnt_1 <= io_lifeCfg;
        end
        wrPtr <= (wrPtr + 1'b1);
      end
      if(when_InputMatrixCache_l104) begin
        if(when_InputMatrixCache_l106) begin
          if(_zz_5) begin
            lifeCnt_0 <= _zz_lifeCnt_0;
          end
          if(_zz_6) begin
            lifeCnt_1 <= _zz_lifeCnt_0;
          end
        end else begin
          if(_zz_5) begin
            lifeCnt_0 <= 8'h0;
          end
          if(_zz_6) begin
            lifeCnt_1 <= 8'h0;
          end
          if(_zz_3[0]) begin
            bankValid_0 <= 1'b0;
          end
          if(_zz_3[1]) begin
            bankValid_1 <= 1'b0;
          end
          rdPtr <= (rdPtr + 1'b1);
        end
      end
    end
  end

  always @(posedge clk) begin
    io_dmaIntr_regNext <= io_dmaIntr;
  end


endmodule

// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : Axi4CompatBridge128
// Git hash  : e7984c6fd05794f7617e004bdecebf29e04e3020

`timescale 1ns/1ps 
module Axi4CompatBridge128 (
  input  wire          io_s_aw_valid,
  output wire          io_s_aw_ready,
  input  wire [31:0]   io_s_aw_payload_addr,
  input  wire [15:0]   io_s_aw_payload_id,
  input  wire [7:0]    io_s_aw_payload_len,
  input  wire [2:0]    io_s_aw_payload_size,
  input  wire [1:0]    io_s_aw_payload_burst,
  input  wire [3:0]    io_s_aw_payload_cache,
  input  wire [2:0]    io_s_aw_payload_prot,
  input  wire          io_s_w_valid,
  output wire          io_s_w_ready,
  input  wire [127:0]  io_s_w_payload_data,
  input  wire          io_s_w_payload_last,
  output wire          io_s_b_valid,
  input  wire          io_s_b_ready,
  output wire [15:0]   io_s_b_payload_id,
  output wire [1:0]    io_s_b_payload_resp,
  input  wire          io_s_ar_valid,
  output wire          io_s_ar_ready,
  input  wire [31:0]   io_s_ar_payload_addr,
  input  wire [15:0]   io_s_ar_payload_id,
  input  wire [7:0]    io_s_ar_payload_len,
  input  wire [2:0]    io_s_ar_payload_size,
  input  wire [1:0]    io_s_ar_payload_burst,
  input  wire [3:0]    io_s_ar_payload_cache,
  input  wire [2:0]    io_s_ar_payload_prot,
  output wire          io_s_r_valid,
  input  wire          io_s_r_ready,
  output wire [127:0]  io_s_r_payload_data,
  output wire [15:0]   io_s_r_payload_id,
  output wire [1:0]    io_s_r_payload_resp,
  output wire          io_s_r_payload_last,
  output wire          io_m_aw_valid,
  input  wire          io_m_aw_ready,
  output wire [31:0]   io_m_aw_payload_addr,
  output wire [15:0]   io_m_aw_payload_id,
  output wire [7:0]    io_m_aw_payload_len,
  output wire [2:0]    io_m_aw_payload_size,
  output wire [1:0]    io_m_aw_payload_burst,
  output wire [3:0]    io_m_aw_payload_cache,
  output wire [2:0]    io_m_aw_payload_prot,
  output wire          io_m_w_valid,
  input  wire          io_m_w_ready,
  output wire [127:0]  io_m_w_payload_data,
  output wire          io_m_w_payload_last,
  input  wire          io_m_b_valid,
  output wire          io_m_b_ready,
  input  wire [15:0]   io_m_b_payload_id,
  input  wire [1:0]    io_m_b_payload_resp,
  output wire          io_m_ar_valid,
  input  wire          io_m_ar_ready,
  output wire [31:0]   io_m_ar_payload_addr,
  output wire [15:0]   io_m_ar_payload_id,
  output wire [7:0]    io_m_ar_payload_len,
  output wire [2:0]    io_m_ar_payload_size,
  output wire [1:0]    io_m_ar_payload_burst,
  output wire [3:0]    io_m_ar_payload_cache,
  output wire [2:0]    io_m_ar_payload_prot,
  input  wire          io_m_r_valid,
  output wire          io_m_r_ready,
  input  wire [127:0]  io_m_r_payload_data,
  input  wire [15:0]   io_m_r_payload_id,
  input  wire [1:0]    io_m_r_payload_resp,
  input  wire          io_m_r_payload_last,
  input  wire          clk,
  input  wire          resetn
);

  reg                 awHold;
  reg        [31:0]   awReg_addr;
  reg        [15:0]   awReg_id;
  reg        [7:0]    awReg_len;
  reg        [2:0]    awReg_size;
  reg        [1:0]    awReg_burst;
  reg        [3:0]    awReg_cache;
  reg        [2:0]    awReg_prot;
  wire                io_s_aw_fire;
  reg                 wHold;
  reg        [127:0]  wReg_data;
  reg                 wReg_last;
  wire                io_s_w_fire;
  wire                io_m_aw_fire;
  wire                io_m_w_fire;
  wire                when_InstAxi4ToStream128_l79;

  assign io_s_aw_ready = (! awHold);
  assign io_s_aw_fire = (io_s_aw_valid && io_s_aw_ready);
  assign io_s_w_ready = (! wHold);
  assign io_s_w_fire = (io_s_w_valid && io_s_w_ready);
  assign io_m_aw_valid = awHold;
  assign io_m_aw_payload_addr = awReg_addr;
  assign io_m_aw_payload_id = awReg_id;
  assign io_m_aw_payload_len = awReg_len;
  assign io_m_aw_payload_size = awReg_size;
  assign io_m_aw_payload_burst = awReg_burst;
  assign io_m_aw_payload_cache = awReg_cache;
  assign io_m_aw_payload_prot = awReg_prot;
  assign io_m_w_valid = wHold;
  assign io_m_w_payload_data = wReg_data;
  assign io_m_w_payload_last = wReg_last;
  assign io_m_aw_fire = (io_m_aw_valid && io_m_aw_ready);
  assign io_m_w_fire = (io_m_w_valid && io_m_w_ready);
  assign when_InstAxi4ToStream128_l79 = (io_m_aw_fire && io_m_w_fire);
  assign io_m_b_ready = io_s_b_ready;
  assign io_s_b_valid = io_m_b_valid;
  assign io_s_b_payload_id = io_m_b_payload_id;
  assign io_s_b_payload_resp = io_m_b_payload_resp;
  assign io_m_ar_valid = io_s_ar_valid;
  assign io_s_ar_ready = io_m_ar_ready;
  assign io_m_ar_payload_addr = io_s_ar_payload_addr;
  assign io_m_ar_payload_id = io_s_ar_payload_id;
  assign io_m_ar_payload_len = io_s_ar_payload_len;
  assign io_m_ar_payload_size = io_s_ar_payload_size;
  assign io_m_ar_payload_burst = io_s_ar_payload_burst;
  assign io_m_ar_payload_cache = io_s_ar_payload_cache;
  assign io_m_ar_payload_prot = io_s_ar_payload_prot;
  assign io_s_r_valid = io_m_r_valid;
  assign io_m_r_ready = io_s_r_ready;
  assign io_s_r_payload_data = io_m_r_payload_data;
  assign io_s_r_payload_id = io_m_r_payload_id;
  assign io_s_r_payload_resp = io_m_r_payload_resp;
  assign io_s_r_payload_last = io_m_r_payload_last;
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      awHold <= 1'b0;
      wHold <= 1'b0;
    end else begin
      if(io_s_aw_fire) begin
        awHold <= 1'b1;
      end
      if(io_s_w_fire) begin
        wHold <= 1'b1;
      end
      if(when_InstAxi4ToStream128_l79) begin
        awHold <= 1'b0;
        wHold <= 1'b0;
      end
    end
  end

  always @(posedge clk) begin
    if(io_s_aw_fire) begin
      awReg_addr <= io_s_aw_payload_addr;
      awReg_id <= io_s_aw_payload_id;
      awReg_len <= io_s_aw_payload_len;
      awReg_size <= io_s_aw_payload_size;
      awReg_burst <= io_s_aw_payload_burst;
      awReg_cache <= io_s_aw_payload_cache;
      awReg_prot <= io_s_aw_payload_prot;
    end
    if(io_s_w_fire) begin
      wReg_data <= io_s_w_payload_data;
      wReg_last <= io_s_w_payload_last;
    end
  end


endmodule

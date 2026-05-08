// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : InstAxi4ToStream128
// Git hash  : e7984c6fd05794f7617e004bdecebf29e04e3020

`timescale 1ns/1ps

module InstAxi4ToStream128 (
  input  wire          io_axi_aw_valid,
  output wire          io_axi_aw_ready,
  input  wire [31:0]   io_axi_aw_payload_addr,
  input  wire [15:0]   io_axi_aw_payload_id,
  input  wire [7:0]    io_axi_aw_payload_len,
  input  wire [2:0]    io_axi_aw_payload_size,
  input  wire [1:0]    io_axi_aw_payload_burst,
  input  wire [3:0]    io_axi_aw_payload_cache,
  input  wire [2:0]    io_axi_aw_payload_prot,
  input  wire          io_axi_w_valid,
  output wire          io_axi_w_ready,
  input  wire [127:0]  io_axi_w_payload_data,
  input  wire          io_axi_w_payload_last,
  output wire          io_axi_b_valid,
  input  wire          io_axi_b_ready,
  output wire [15:0]   io_axi_b_payload_id,
  output wire [1:0]    io_axi_b_payload_resp,
  input  wire          io_axi_ar_valid,
  output wire          io_axi_ar_ready,
  input  wire [31:0]   io_axi_ar_payload_addr,
  input  wire [15:0]   io_axi_ar_payload_id,
  input  wire [7:0]    io_axi_ar_payload_len,
  input  wire [2:0]    io_axi_ar_payload_size,
  input  wire [1:0]    io_axi_ar_payload_burst,
  input  wire [3:0]    io_axi_ar_payload_cache,
  input  wire [2:0]    io_axi_ar_payload_prot,
  output wire          io_axi_r_valid,
  input  wire          io_axi_r_ready,
  output wire [127:0]  io_axi_r_payload_data,
  output wire [15:0]   io_axi_r_payload_id,
  output wire [1:0]    io_axi_r_payload_resp,
  output wire          io_axi_r_payload_last,
  output wire          io_out_valid,
  input  wire          io_out_ready,
  output wire [127:0]  io_out_payload,
  input  wire          clk,
  input  wire          resetn
);

  reg                 bridge_io_m_aw_ready;
  reg                 bridge_io_m_ar_ready;
  wire                bridge_io_s_ar_ready;
  wire                bridge_io_s_aw_ready;
  wire                bridge_io_s_w_ready;
  wire                bridge_io_s_r_valid;
  wire       [127:0]  bridge_io_s_r_payload_data;
  wire       [15:0]   bridge_io_s_r_payload_id;
  wire       [1:0]    bridge_io_s_r_payload_resp;
  wire                bridge_io_s_r_payload_last;
  wire                bridge_io_s_b_valid;
  wire       [15:0]   bridge_io_s_b_payload_id;
  wire       [1:0]    bridge_io_s_b_payload_resp;
  wire                bridge_io_m_ar_valid;
  wire       [31:0]   bridge_io_m_ar_payload_addr;
  wire       [15:0]   bridge_io_m_ar_payload_id;
  wire       [7:0]    bridge_io_m_ar_payload_len;
  wire       [2:0]    bridge_io_m_ar_payload_size;
  wire       [1:0]    bridge_io_m_ar_payload_burst;
  wire       [3:0]    bridge_io_m_ar_payload_cache;
  wire       [2:0]    bridge_io_m_ar_payload_prot;
  wire                bridge_io_m_aw_valid;
  wire       [31:0]   bridge_io_m_aw_payload_addr;
  wire       [15:0]   bridge_io_m_aw_payload_id;
  wire       [7:0]    bridge_io_m_aw_payload_len;
  wire       [2:0]    bridge_io_m_aw_payload_size;
  wire       [1:0]    bridge_io_m_aw_payload_burst;
  wire       [3:0]    bridge_io_m_aw_payload_cache;
  wire       [2:0]    bridge_io_m_aw_payload_prot;
  wire                bridge_io_m_w_valid;
  wire       [127:0]  bridge_io_m_w_payload_data;
  wire                bridge_io_m_w_payload_last;
  wire                bridge_io_m_r_ready;
  wire                bridge_io_m_b_ready;
  wire                streamFifo_1_io_push_ready;
  wire                streamFifo_1_io_pop_valid;
  wire       [127:0]  streamFifo_1_io_pop_payload;
  wire       [5:0]    streamFifo_1_io_occupancy;
  wire       [5:0]    streamFifo_1_io_availability;
  wire       [3:0]    temp_Axi4Incr_alignMask;
  wire       [11:0]   temp_Axi4Incr_baseIncr;
  wire       [2:0]    temp_Axi4Incr_wrapCase_2;
  reg        [11:0]   temp_Axi4Incr_result;
  wire       [10:0]   temp_Axi4Incr_result_1;
  wire       [0:0]    temp_Axi4Incr_result_2;
  wire       [9:0]    temp_Axi4Incr_result_3;
  wire       [1:0]    temp_Axi4Incr_result_4;
  wire       [8:0]    temp_Axi4Incr_result_5;
  wire       [2:0]    temp_Axi4Incr_result_6;
  wire       [7:0]    temp_Axi4Incr_result_7;
  wire       [3:0]    temp_Axi4Incr_result_8;
  wire       [6:0]    temp_Axi4Incr_result_9;
  wire       [4:0]    temp_Axi4Incr_result_10;
  wire       [5:0]    temp_Axi4Incr_result_11;
  wire       [5:0]    temp_Axi4Incr_result_12;
  wire       [4:0]    temp_Axi4Incr_result_13;
  wire       [6:0]    temp_Axi4Incr_result_14;
  wire       [3:0]    temp_Axi4Incr_result_15;
  wire       [7:0]    temp_Axi4Incr_result_16;
  wire                temp_when;
  wire       [3:0]    temp_Axi4Incr_alignMask_1;
  wire       [11:0]   temp_Axi4Incr_baseIncr_1;
  wire       [2:0]    temp_Axi4Incr_wrapCase_1_1;
  reg        [11:0]   temp_Axi4Incr_result_1_1;
  wire       [10:0]   temp_Axi4Incr_result_1_2;
  wire       [0:0]    temp_Axi4Incr_result_1_3;
  wire       [9:0]    temp_Axi4Incr_result_1_4;
  wire       [1:0]    temp_Axi4Incr_result_1_5;
  wire       [8:0]    temp_Axi4Incr_result_1_6;
  wire       [2:0]    temp_Axi4Incr_result_1_7;
  wire       [7:0]    temp_Axi4Incr_result_1_8;
  wire       [3:0]    temp_Axi4Incr_result_1_9;
  wire       [6:0]    temp_Axi4Incr_result_1_10;
  wire       [4:0]    temp_Axi4Incr_result_1_11;
  wire       [5:0]    temp_Axi4Incr_result_1_12;
  wire       [5:0]    temp_Axi4Incr_result_1_13;
  wire       [4:0]    temp_Axi4Incr_result_1_14;
  wire       [6:0]    temp_Axi4Incr_result_1_15;
  wire       [3:0]    temp_Axi4Incr_result_1_16;
  wire       [7:0]    temp_Axi4Incr_result_1_17;
  wire                temp_when_1;
  wire                push_valid;
  wire                push_ready;
  wire       [127:0]  push_payload;
  wire                axif_readErrorFlag;
  wire                axif_writeErrorFlag;
  wire                axif_readHaltRequest;
  reg                 axif_writeHaltRequest;
  reg                 unburstify_result_valid;
  wire                unburstify_result_ready;
  reg                 unburstify_result_payload_last;
  reg        [31:0]   unburstify_result_payload_fragment_addr;
  reg        [15:0]   unburstify_result_payload_fragment_id;
  reg        [2:0]    unburstify_result_payload_fragment_size;
  reg        [1:0]    unburstify_result_payload_fragment_burst;
  reg        [3:0]    unburstify_result_payload_fragment_cache;
  reg        [2:0]    unburstify_result_payload_fragment_prot;
  wire                unburstify_doResult;
  reg                 unburstify_buffer_valid;
  reg        [7:0]    unburstify_buffer_len;
  reg        [7:0]    unburstify_buffer_beat;
  reg        [31:0]   unburstify_buffer_transaction_addr;
  reg        [15:0]   unburstify_buffer_transaction_id;
  reg        [2:0]    unburstify_buffer_transaction_size;
  reg        [1:0]    unburstify_buffer_transaction_burst;
  reg        [3:0]    unburstify_buffer_transaction_cache;
  reg        [2:0]    unburstify_buffer_transaction_prot;
  wire                unburstify_buffer_last;
  wire       [2:0]    Axi4Incr_validSize;
  reg        [31:0]   Axi4Incr_result;
  wire       [19:0]   Axi4Incr_highCat;
  wire       [4:0]    Axi4Incr_sizeValue;
  wire       [11:0]   Axi4Incr_alignMask;
  wire       [11:0]   Axi4Incr_base;
  wire       [11:0]   Axi4Incr_baseIncr;
  reg        [1:0]    temp_Axi4Incr_wrapCase;
  wire       [2:0]    Axi4Incr_wrapCase;
  wire                axif_writeJoinEvent_valid;
  reg                 axif_writeJoinEvent_ready;
  wire                axif_writeOccur;
  reg                 axif_writeRsp_valid;
  reg                 axif_writeRsp_ready;
  wire       [15:0]   axif_writeRsp_payload_id;
  reg        [1:0]    axif_writeRsp_payload_resp;
  wire                axif_writeRsp_stage_valid;
  wire                axif_writeRsp_stage_ready;
  wire       [15:0]   axif_writeRsp_stage_payload_id;
  wire       [1:0]    axif_writeRsp_stage_payload_resp;
  reg                 axif_writeRsp_rValid;
  reg        [15:0]   axif_writeRsp_rData_id;
  reg        [1:0]    axif_writeRsp_rData_resp;
  reg                 unburstify_result_valid_1;
  reg                 unburstify_result_ready_1;
  reg                 unburstify_result_payload_last_1;
  reg        [31:0]   unburstify_result_payload_fragment_addr_1;
  reg        [15:0]   unburstify_result_payload_fragment_id_1;
  reg        [2:0]    unburstify_result_payload_fragment_size_1;
  reg        [1:0]    unburstify_result_payload_fragment_burst_1;
  reg        [3:0]    unburstify_result_payload_fragment_cache_1;
  reg        [2:0]    unburstify_result_payload_fragment_prot_1;
  wire                unburstify_doResult_1;
  reg                 unburstify_buffer_valid_1;
  reg        [7:0]    unburstify_buffer_len_1;
  reg        [7:0]    unburstify_buffer_beat_1;
  reg        [31:0]   unburstify_buffer_transaction_addr_1;
  reg        [15:0]   unburstify_buffer_transaction_id_1;
  reg        [2:0]    unburstify_buffer_transaction_size_1;
  reg        [1:0]    unburstify_buffer_transaction_burst_1;
  reg        [3:0]    unburstify_buffer_transaction_cache_1;
  reg        [2:0]    unburstify_buffer_transaction_prot_1;
  wire                unburstify_buffer_last_1;
  wire       [2:0]    Axi4Incr_validSize_1;
  reg        [31:0]   Axi4Incr_result_1;
  wire       [19:0]   Axi4Incr_highCat_1;
  wire       [4:0]    Axi4Incr_sizeValue_1;
  wire       [11:0]   Axi4Incr_alignMask_1;
  wire       [11:0]   Axi4Incr_base_1;
  wire       [11:0]   Axi4Incr_baseIncr_1;
  reg        [1:0]    temp_Axi4Incr_wrapCase_1;
  wire       [2:0]    Axi4Incr_wrapCase_1;
  wire                axif_readDataStage_valid;
  wire                axif_readDataStage_ready;
  wire                axif_readDataStage_payload_last;
  wire       [31:0]   axif_readDataStage_payload_fragment_addr;
  wire       [15:0]   axif_readDataStage_payload_fragment_id;
  wire       [2:0]    axif_readDataStage_payload_fragment_size;
  wire       [1:0]    axif_readDataStage_payload_fragment_burst;
  wire       [3:0]    axif_readDataStage_payload_fragment_cache;
  wire       [2:0]    axif_readDataStage_payload_fragment_prot;
  reg                 unburstify_result_rValid;
  reg                 unburstify_result_rData_last;
  reg        [31:0]   unburstify_result_rData_fragment_addr;
  reg        [15:0]   unburstify_result_rData_fragment_id;
  reg        [2:0]    unburstify_result_rData_fragment_size;
  reg        [1:0]    unburstify_result_rData_fragment_burst;
  reg        [3:0]    unburstify_result_rData_fragment_cache;
  reg        [2:0]    unburstify_result_rData_fragment_prot;
  wire       [127:0]  axif_readRsp_data;
  wire       [15:0]   axif_readRsp_id;
  reg        [1:0]    axif_readRsp_resp;
  wire                axif_readRsp_last;
  wire                temp_axif_readDataStage_ready;
  wire                axif_readDataStage_haltWhen_valid;
  wire                axif_readDataStage_haltWhen_ready;
  wire                axif_readDataStage_haltWhen_payload_last;
  wire       [31:0]   axif_readDataStage_haltWhen_payload_fragment_addr;
  wire       [15:0]   axif_readDataStage_haltWhen_payload_fragment_id;
  wire       [2:0]    axif_readDataStage_haltWhen_payload_fragment_size;
  wire       [1:0]    axif_readDataStage_haltWhen_payload_fragment_burst;
  wire       [3:0]    axif_readDataStage_haltWhen_payload_fragment_cache;
  wire       [2:0]    axif_readDataStage_haltWhen_payload_fragment_prot;
  wire                axif_readDataStage_haltWhen_translated_valid;
  wire                axif_readDataStage_haltWhen_translated_ready;
  wire       [127:0]  axif_readDataStage_haltWhen_translated_payload_data;
  wire       [15:0]   axif_readDataStage_haltWhen_translated_payload_id;
  wire       [1:0]    axif_readDataStage_haltWhen_translated_payload_resp;
  wire                axif_readDataStage_haltWhen_translated_payload_last;
  wire                axif_readOccur;
  wire       [31:0]   axif_readAddressMasked;
  wire       [31:0]   axif_writeAddressMasked;
  reg                 temp_push_valid;

  assign temp_when = (bridge_io_m_aw_payload_len != 8'h0);
  assign temp_when_1 = (bridge_io_m_ar_payload_len != 8'h0);
  assign temp_Axi4Incr_alignMask = {(3'b011 < Axi4Incr_validSize),{(3'b010 < Axi4Incr_validSize),{(3'b001 < Axi4Incr_validSize),(3'b000 < Axi4Incr_validSize)}}};
  assign temp_Axi4Incr_baseIncr = {7'd0, Axi4Incr_sizeValue};
  assign temp_Axi4Incr_wrapCase_2 = {1'd0, temp_Axi4Incr_wrapCase};
  assign temp_Axi4Incr_alignMask_1 = {(3'b011 < Axi4Incr_validSize_1),{(3'b010 < Axi4Incr_validSize_1),{(3'b001 < Axi4Incr_validSize_1),(3'b000 < Axi4Incr_validSize_1)}}};
  assign temp_Axi4Incr_baseIncr_1 = {7'd0, Axi4Incr_sizeValue_1};
  assign temp_Axi4Incr_wrapCase_1_1 = {1'd0, temp_Axi4Incr_wrapCase_1};
  assign temp_Axi4Incr_result_1 = Axi4Incr_base[11 : 1];
  assign temp_Axi4Incr_result_2 = Axi4Incr_baseIncr[0 : 0];
  assign temp_Axi4Incr_result_3 = Axi4Incr_base[11 : 2];
  assign temp_Axi4Incr_result_4 = Axi4Incr_baseIncr[1 : 0];
  assign temp_Axi4Incr_result_5 = Axi4Incr_base[11 : 3];
  assign temp_Axi4Incr_result_6 = Axi4Incr_baseIncr[2 : 0];
  assign temp_Axi4Incr_result_7 = Axi4Incr_base[11 : 4];
  assign temp_Axi4Incr_result_8 = Axi4Incr_baseIncr[3 : 0];
  assign temp_Axi4Incr_result_9 = Axi4Incr_base[11 : 5];
  assign temp_Axi4Incr_result_10 = Axi4Incr_baseIncr[4 : 0];
  assign temp_Axi4Incr_result_11 = Axi4Incr_base[11 : 6];
  assign temp_Axi4Incr_result_12 = Axi4Incr_baseIncr[5 : 0];
  assign temp_Axi4Incr_result_13 = Axi4Incr_base[11 : 7];
  assign temp_Axi4Incr_result_14 = Axi4Incr_baseIncr[6 : 0];
  assign temp_Axi4Incr_result_15 = Axi4Incr_base[11 : 8];
  assign temp_Axi4Incr_result_16 = Axi4Incr_baseIncr[7 : 0];
  assign temp_Axi4Incr_result_1_2 = Axi4Incr_base_1[11 : 1];
  assign temp_Axi4Incr_result_1_3 = Axi4Incr_baseIncr_1[0 : 0];
  assign temp_Axi4Incr_result_1_4 = Axi4Incr_base_1[11 : 2];
  assign temp_Axi4Incr_result_1_5 = Axi4Incr_baseIncr_1[1 : 0];
  assign temp_Axi4Incr_result_1_6 = Axi4Incr_base_1[11 : 3];
  assign temp_Axi4Incr_result_1_7 = Axi4Incr_baseIncr_1[2 : 0];
  assign temp_Axi4Incr_result_1_8 = Axi4Incr_base_1[11 : 4];
  assign temp_Axi4Incr_result_1_9 = Axi4Incr_baseIncr_1[3 : 0];
  assign temp_Axi4Incr_result_1_10 = Axi4Incr_base_1[11 : 5];
  assign temp_Axi4Incr_result_1_11 = Axi4Incr_baseIncr_1[4 : 0];
  assign temp_Axi4Incr_result_1_12 = Axi4Incr_base_1[11 : 6];
  assign temp_Axi4Incr_result_1_13 = Axi4Incr_baseIncr_1[5 : 0];
  assign temp_Axi4Incr_result_1_14 = Axi4Incr_base_1[11 : 7];
  assign temp_Axi4Incr_result_1_15 = Axi4Incr_baseIncr_1[6 : 0];
  assign temp_Axi4Incr_result_1_16 = Axi4Incr_base_1[11 : 8];
  assign temp_Axi4Incr_result_1_17 = Axi4Incr_baseIncr_1[7 : 0];
  Axi4CompatBridge128 bridge (
    .io_s_aw_valid         (io_axi_aw_valid                                           ), //i
    .io_s_aw_ready         (bridge_io_s_aw_ready                                      ), //o
    .io_s_aw_payload_addr  (io_axi_aw_payload_addr[31:0]                              ), //i
    .io_s_aw_payload_id    (io_axi_aw_payload_id[15:0]                                ), //i
    .io_s_aw_payload_len   (io_axi_aw_payload_len[7:0]                                ), //i
    .io_s_aw_payload_size  (io_axi_aw_payload_size[2:0]                               ), //i
    .io_s_aw_payload_burst (io_axi_aw_payload_burst[1:0]                              ), //i
    .io_s_aw_payload_cache (io_axi_aw_payload_cache[3:0]                              ), //i
    .io_s_aw_payload_prot  (io_axi_aw_payload_prot[2:0]                               ), //i
    .io_s_w_valid          (io_axi_w_valid                                            ), //i
    .io_s_w_ready          (bridge_io_s_w_ready                                       ), //o
    .io_s_w_payload_data   (io_axi_w_payload_data[127:0]                              ), //i
    .io_s_w_payload_last   (io_axi_w_payload_last                                     ), //i
    .io_s_b_valid          (bridge_io_s_b_valid                                       ), //o
    .io_s_b_ready          (io_axi_b_ready                                            ), //i
    .io_s_b_payload_id     (bridge_io_s_b_payload_id[15:0]                            ), //o
    .io_s_b_payload_resp   (bridge_io_s_b_payload_resp[1:0]                           ), //o
    .io_s_ar_valid         (io_axi_ar_valid                                           ), //i
    .io_s_ar_ready         (bridge_io_s_ar_ready                                      ), //o
    .io_s_ar_payload_addr  (io_axi_ar_payload_addr[31:0]                              ), //i
    .io_s_ar_payload_id    (io_axi_ar_payload_id[15:0]                                ), //i
    .io_s_ar_payload_len   (io_axi_ar_payload_len[7:0]                                ), //i
    .io_s_ar_payload_size  (io_axi_ar_payload_size[2:0]                               ), //i
    .io_s_ar_payload_burst (io_axi_ar_payload_burst[1:0]                              ), //i
    .io_s_ar_payload_cache (io_axi_ar_payload_cache[3:0]                              ), //i
    .io_s_ar_payload_prot  (io_axi_ar_payload_prot[2:0]                               ), //i
    .io_s_r_valid          (bridge_io_s_r_valid                                       ), //o
    .io_s_r_ready          (io_axi_r_ready                                            ), //i
    .io_s_r_payload_data   (bridge_io_s_r_payload_data[127:0]                         ), //o
    .io_s_r_payload_id     (bridge_io_s_r_payload_id[15:0]                            ), //o
    .io_s_r_payload_resp   (bridge_io_s_r_payload_resp[1:0]                           ), //o
    .io_s_r_payload_last   (bridge_io_s_r_payload_last                                ), //o
    .io_m_aw_valid         (bridge_io_m_aw_valid                                      ), //o
    .io_m_aw_ready         (bridge_io_m_aw_ready                                      ), //i
    .io_m_aw_payload_addr  (bridge_io_m_aw_payload_addr[31:0]                         ), //o
    .io_m_aw_payload_id    (bridge_io_m_aw_payload_id[15:0]                           ), //o
    .io_m_aw_payload_len   (bridge_io_m_aw_payload_len[7:0]                           ), //o
    .io_m_aw_payload_size  (bridge_io_m_aw_payload_size[2:0]                          ), //o
    .io_m_aw_payload_burst (bridge_io_m_aw_payload_burst[1:0]                         ), //o
    .io_m_aw_payload_cache (bridge_io_m_aw_payload_cache[3:0]                         ), //o
    .io_m_aw_payload_prot  (bridge_io_m_aw_payload_prot[2:0]                          ), //o
    .io_m_w_valid          (bridge_io_m_w_valid                                       ), //o
    .io_m_w_ready          (axif_writeOccur                                           ), //i
    .io_m_w_payload_data   (bridge_io_m_w_payload_data[127:0]                         ), //o
    .io_m_w_payload_last   (bridge_io_m_w_payload_last                                ), //o
    .io_m_b_valid          (axif_writeRsp_stage_valid                                 ), //i
    .io_m_b_ready          (bridge_io_m_b_ready                                       ), //o
    .io_m_b_payload_id     (axif_writeRsp_stage_payload_id[15:0]                      ), //i
    .io_m_b_payload_resp   (axif_writeRsp_stage_payload_resp[1:0]                     ), //i
    .io_m_ar_valid         (bridge_io_m_ar_valid                                      ), //o
    .io_m_ar_ready         (bridge_io_m_ar_ready                                      ), //i
    .io_m_ar_payload_addr  (bridge_io_m_ar_payload_addr[31:0]                         ), //o
    .io_m_ar_payload_id    (bridge_io_m_ar_payload_id[15:0]                           ), //o
    .io_m_ar_payload_len   (bridge_io_m_ar_payload_len[7:0]                           ), //o
    .io_m_ar_payload_size  (bridge_io_m_ar_payload_size[2:0]                          ), //o
    .io_m_ar_payload_burst (bridge_io_m_ar_payload_burst[1:0]                         ), //o
    .io_m_ar_payload_cache (bridge_io_m_ar_payload_cache[3:0]                         ), //o
    .io_m_ar_payload_prot  (bridge_io_m_ar_payload_prot[2:0]                          ), //o
    .io_m_r_valid          (axif_readDataStage_haltWhen_translated_valid              ), //i
    .io_m_r_ready          (bridge_io_m_r_ready                                       ), //o
    .io_m_r_payload_data   (axif_readDataStage_haltWhen_translated_payload_data[127:0]), //i
    .io_m_r_payload_id     (axif_readDataStage_haltWhen_translated_payload_id[15:0]   ), //i
    .io_m_r_payload_resp   (axif_readDataStage_haltWhen_translated_payload_resp[1:0]  ), //i
    .io_m_r_payload_last   (axif_readDataStage_haltWhen_translated_payload_last       ), //i
    .clk                   (clk                                                       ), //i
    .resetn                (resetn                                                    )  //i
  );
  StreamFifo streamFifo_1 (
    .io_push_valid   (push_valid                        ), //i
    .io_push_ready   (streamFifo_1_io_push_ready        ), //o
    .io_push_payload (push_payload[127:0]               ), //i
    .io_pop_valid    (streamFifo_1_io_pop_valid         ), //o
    .io_pop_ready    (io_out_ready                      ), //i
    .io_pop_payload  (streamFifo_1_io_pop_payload[127:0]), //o
    .io_flush        (1'b0                              ), //i
    .io_occupancy    (streamFifo_1_io_occupancy[5:0]    ), //o
    .io_availability (streamFifo_1_io_availability[5:0] ), //o
    .clk             (clk                               ), //i
    .resetn          (resetn                            )  //i
  );
  always @(*) begin
    case(Axi4Incr_wrapCase)
      3'b000 : temp_Axi4Incr_result = {temp_Axi4Incr_result_1,temp_Axi4Incr_result_2};
      3'b001 : temp_Axi4Incr_result = {temp_Axi4Incr_result_3,temp_Axi4Incr_result_4};
      3'b010 : temp_Axi4Incr_result = {temp_Axi4Incr_result_5,temp_Axi4Incr_result_6};
      3'b011 : temp_Axi4Incr_result = {temp_Axi4Incr_result_7,temp_Axi4Incr_result_8};
      3'b100 : temp_Axi4Incr_result = {temp_Axi4Incr_result_9,temp_Axi4Incr_result_10};
      3'b101 : temp_Axi4Incr_result = {temp_Axi4Incr_result_11,temp_Axi4Incr_result_12};
      3'b110 : temp_Axi4Incr_result = {temp_Axi4Incr_result_13,temp_Axi4Incr_result_14};
      default : temp_Axi4Incr_result = {temp_Axi4Incr_result_15,temp_Axi4Incr_result_16};
    endcase
  end

  always @(*) begin
    case(Axi4Incr_wrapCase_1)
      3'b000 : temp_Axi4Incr_result_1_1 = {temp_Axi4Incr_result_1_2,temp_Axi4Incr_result_1_3};
      3'b001 : temp_Axi4Incr_result_1_1 = {temp_Axi4Incr_result_1_4,temp_Axi4Incr_result_1_5};
      3'b010 : temp_Axi4Incr_result_1_1 = {temp_Axi4Incr_result_1_6,temp_Axi4Incr_result_1_7};
      3'b011 : temp_Axi4Incr_result_1_1 = {temp_Axi4Incr_result_1_8,temp_Axi4Incr_result_1_9};
      3'b100 : temp_Axi4Incr_result_1_1 = {temp_Axi4Incr_result_1_10,temp_Axi4Incr_result_1_11};
      3'b101 : temp_Axi4Incr_result_1_1 = {temp_Axi4Incr_result_1_12,temp_Axi4Incr_result_1_13};
      3'b110 : temp_Axi4Incr_result_1_1 = {temp_Axi4Incr_result_1_14,temp_Axi4Incr_result_1_15};
      default : temp_Axi4Incr_result_1_1 = {temp_Axi4Incr_result_1_16,temp_Axi4Incr_result_1_17};
    endcase
  end

  assign io_axi_aw_ready = bridge_io_s_aw_ready; // @ InstAxi4ToStream128.scala l30
  assign io_axi_w_ready = bridge_io_s_w_ready; // @ InstAxi4ToStream128.scala l30
  assign io_axi_b_valid = bridge_io_s_b_valid; // @ InstAxi4ToStream128.scala l30
  assign io_axi_b_payload_id = bridge_io_s_b_payload_id; // @ InstAxi4ToStream128.scala l30
  assign io_axi_b_payload_resp = bridge_io_s_b_payload_resp; // @ InstAxi4ToStream128.scala l30
  assign io_axi_ar_ready = bridge_io_s_ar_ready; // @ InstAxi4ToStream128.scala l30
  assign io_axi_r_valid = bridge_io_s_r_valid; // @ InstAxi4ToStream128.scala l30
  assign io_axi_r_payload_data = bridge_io_s_r_payload_data; // @ InstAxi4ToStream128.scala l30
  assign io_axi_r_payload_id = bridge_io_s_r_payload_id; // @ InstAxi4ToStream128.scala l30
  assign io_axi_r_payload_resp = bridge_io_s_r_payload_resp; // @ InstAxi4ToStream128.scala l30
  assign io_axi_r_payload_last = bridge_io_s_r_payload_last; // @ InstAxi4ToStream128.scala l30
  assign push_ready = streamFifo_1_io_push_ready; // @ Stream.scala l316
  assign io_out_valid = streamFifo_1_io_pop_valid; // @ Stream.scala l315
  assign io_out_payload = streamFifo_1_io_pop_payload; // @ Stream.scala l317
  assign axif_readErrorFlag = 1'b0; // @ BusSlaveFactory.scala l105
  assign axif_writeErrorFlag = 1'b0; // @ BusSlaveFactory.scala l106
  assign axif_readHaltRequest = 1'b0; // @ Axi4SlaveFactory.scala l13
  always @(*) begin
    axif_writeHaltRequest = 1'b0; // @ Axi4SlaveFactory.scala l14
    if(((axif_writeAddressMasked & (~ 32'h0000000f)) == 32'h0)) begin
      if(axif_writeJoinEvent_valid) begin
        if((! push_ready)) begin
          axif_writeHaltRequest = 1'b1; // @ Axi4SlaveFactory.scala l68
        end
      end
    end
  end

  assign unburstify_buffer_last = (unburstify_buffer_beat == 8'h01); // @ BaseType.scala l305
  assign Axi4Incr_validSize = unburstify_buffer_transaction_size[2 : 0]; // @ BaseType.scala l299
  assign Axi4Incr_highCat = unburstify_buffer_transaction_addr[31 : 12]; // @ BaseType.scala l299
  assign Axi4Incr_sizeValue = {(3'b100 == Axi4Incr_validSize),{(3'b011 == Axi4Incr_validSize),{(3'b010 == Axi4Incr_validSize),{(3'b001 == Axi4Incr_validSize),(3'b000 == Axi4Incr_validSize)}}}}; // @ BaseType.scala l318
  assign Axi4Incr_alignMask = {8'd0, temp_Axi4Incr_alignMask}; // @ BaseType.scala l299
  assign Axi4Incr_base = (unburstify_buffer_transaction_addr[11 : 0] & (~ Axi4Incr_alignMask)); // @ BaseType.scala l299
  assign Axi4Incr_baseIncr = (Axi4Incr_base + temp_Axi4Incr_baseIncr); // @ BaseType.scala l299
  always @(*) begin
    casez(unburstify_buffer_len)
      8'b????1??? : begin
        temp_Axi4Incr_wrapCase = 2'b11; // @ Misc.scala l254
      end
      8'b????01?? : begin
        temp_Axi4Incr_wrapCase = 2'b10; // @ Misc.scala l254
      end
      8'b????001? : begin
        temp_Axi4Incr_wrapCase = 2'b01; // @ Misc.scala l254
      end
      default : begin
        temp_Axi4Incr_wrapCase = 2'b00; // @ Misc.scala l250
      end
    endcase
  end

  assign Axi4Incr_wrapCase = (Axi4Incr_validSize + temp_Axi4Incr_wrapCase_2); // @ BaseType.scala l299
  always @(*) begin
    case(unburstify_buffer_transaction_burst)
      2'b00 : begin
        Axi4Incr_result = unburstify_buffer_transaction_addr; // @ Axi4.scala l334
      end
      2'b10 : begin
        Axi4Incr_result = {Axi4Incr_highCat,temp_Axi4Incr_result}; // @ Axi4.scala l338
      end
      default : begin
        Axi4Incr_result = {Axi4Incr_highCat,Axi4Incr_baseIncr}; // @ Axi4.scala l341
      end
    endcase
  end

  always @(*) begin
    bridge_io_m_aw_ready = 1'b0; // @ Axi4Channel.scala l310
    if(!unburstify_buffer_valid) begin
      bridge_io_m_aw_ready = unburstify_result_ready; // @ Axi4Channel.scala l318
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_valid = 1'b1; // @ Axi4Channel.scala l312
    end else begin
      unburstify_result_valid = bridge_io_m_aw_valid; // @ Axi4Channel.scala l319
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_payload_last = unburstify_buffer_last; // @ Axi4Channel.scala l313
    end else begin
      unburstify_result_payload_last = 1'b1; // @ Axi4Channel.scala l321
      if(temp_when) begin
        unburstify_result_payload_last = 1'b0; // @ Axi4Channel.scala l324
      end
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_payload_fragment_id = unburstify_buffer_transaction_id; // @ Axi4Channel.scala l314
    end else begin
      unburstify_result_payload_fragment_id = bridge_io_m_aw_payload_id; // @ Bundle.scala l141
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_payload_fragment_size = unburstify_buffer_transaction_size; // @ Axi4Channel.scala l314
    end else begin
      unburstify_result_payload_fragment_size = bridge_io_m_aw_payload_size; // @ Bundle.scala l141
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_payload_fragment_burst = unburstify_buffer_transaction_burst; // @ Axi4Channel.scala l314
    end else begin
      unburstify_result_payload_fragment_burst = bridge_io_m_aw_payload_burst; // @ Bundle.scala l141
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_payload_fragment_cache = unburstify_buffer_transaction_cache; // @ Axi4Channel.scala l314
    end else begin
      unburstify_result_payload_fragment_cache = bridge_io_m_aw_payload_cache; // @ Bundle.scala l141
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_payload_fragment_prot = unburstify_buffer_transaction_prot; // @ Axi4Channel.scala l314
    end else begin
      unburstify_result_payload_fragment_prot = bridge_io_m_aw_payload_prot; // @ Bundle.scala l141
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_payload_fragment_addr = Axi4Incr_result; // @ Axi4Channel.scala l316
    end else begin
      unburstify_result_payload_fragment_addr = bridge_io_m_aw_payload_addr; // @ Bundle.scala l141
    end
  end

  assign axif_writeOccur = (axif_writeJoinEvent_valid && axif_writeJoinEvent_ready); // @ BaseType.scala l305
  assign axif_writeJoinEvent_valid = (unburstify_result_valid && bridge_io_m_w_valid); // @ Stream.scala l1160
  assign unburstify_result_ready = axif_writeOccur; // @ Stream.scala l1161
  always @(*) begin
    axif_writeRsp_ready = axif_writeRsp_stage_ready; // @ Stream.scala l392
    if((! axif_writeRsp_stage_valid)) begin
      axif_writeRsp_ready = 1'b1; // @ Stream.scala l393
    end
  end

  assign axif_writeRsp_stage_valid = axif_writeRsp_rValid; // @ Stream.scala l395
  assign axif_writeRsp_stage_payload_id = axif_writeRsp_rData_id; // @ Stream.scala l396
  assign axif_writeRsp_stage_payload_resp = axif_writeRsp_rData_resp; // @ Stream.scala l396
  assign axif_writeRsp_stage_ready = bridge_io_m_b_ready; // @ Stream.scala l316
  always @(*) begin
    if(bridge_io_m_w_payload_last) begin
      axif_writeJoinEvent_ready = (axif_writeRsp_ready && (! axif_writeHaltRequest)); // @ Axi4SlaveFactory.scala l22
    end else begin
      axif_writeJoinEvent_ready = (! axif_writeHaltRequest); // @ Axi4SlaveFactory.scala l26
    end
  end

  always @(*) begin
    if(bridge_io_m_w_payload_last) begin
      axif_writeRsp_valid = axif_writeOccur; // @ Axi4SlaveFactory.scala l23
    end else begin
      axif_writeRsp_valid = 1'b0; // @ Axi4SlaveFactory.scala l27
    end
  end

  assign unburstify_buffer_last_1 = (unburstify_buffer_beat_1 == 8'h01); // @ BaseType.scala l305
  assign Axi4Incr_validSize_1 = unburstify_buffer_transaction_size_1[2 : 0]; // @ BaseType.scala l299
  assign Axi4Incr_highCat_1 = unburstify_buffer_transaction_addr_1[31 : 12]; // @ BaseType.scala l299
  assign Axi4Incr_sizeValue_1 = {(3'b100 == Axi4Incr_validSize_1),{(3'b011 == Axi4Incr_validSize_1),{(3'b010 == Axi4Incr_validSize_1),{(3'b001 == Axi4Incr_validSize_1),(3'b000 == Axi4Incr_validSize_1)}}}}; // @ BaseType.scala l318
  assign Axi4Incr_alignMask_1 = {8'd0, temp_Axi4Incr_alignMask_1}; // @ BaseType.scala l299
  assign Axi4Incr_base_1 = (unburstify_buffer_transaction_addr_1[11 : 0] & (~ Axi4Incr_alignMask_1)); // @ BaseType.scala l299
  assign Axi4Incr_baseIncr_1 = (Axi4Incr_base_1 + temp_Axi4Incr_baseIncr_1); // @ BaseType.scala l299
  always @(*) begin
    casez(unburstify_buffer_len_1)
      8'b????1??? : begin
        temp_Axi4Incr_wrapCase_1 = 2'b11; // @ Misc.scala l254
      end
      8'b????01?? : begin
        temp_Axi4Incr_wrapCase_1 = 2'b10; // @ Misc.scala l254
      end
      8'b????001? : begin
        temp_Axi4Incr_wrapCase_1 = 2'b01; // @ Misc.scala l254
      end
      default : begin
        temp_Axi4Incr_wrapCase_1 = 2'b00; // @ Misc.scala l250
      end
    endcase
  end

  assign Axi4Incr_wrapCase_1 = (Axi4Incr_validSize_1 + temp_Axi4Incr_wrapCase_1_1); // @ BaseType.scala l299
  always @(*) begin
    case(unburstify_buffer_transaction_burst_1)
      2'b00 : begin
        Axi4Incr_result_1 = unburstify_buffer_transaction_addr_1; // @ Axi4.scala l334
      end
      2'b10 : begin
        Axi4Incr_result_1 = {Axi4Incr_highCat_1,temp_Axi4Incr_result_1_1}; // @ Axi4.scala l338
      end
      default : begin
        Axi4Incr_result_1 = {Axi4Incr_highCat_1,Axi4Incr_baseIncr_1}; // @ Axi4.scala l341
      end
    endcase
  end

  always @(*) begin
    bridge_io_m_ar_ready = 1'b0; // @ Axi4Channel.scala l310
    if(!unburstify_buffer_valid_1) begin
      bridge_io_m_ar_ready = unburstify_result_ready_1; // @ Axi4Channel.scala l318
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_valid_1 = 1'b1; // @ Axi4Channel.scala l312
    end else begin
      unburstify_result_valid_1 = bridge_io_m_ar_valid; // @ Axi4Channel.scala l319
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_payload_last_1 = unburstify_buffer_last_1; // @ Axi4Channel.scala l313
    end else begin
      unburstify_result_payload_last_1 = 1'b1; // @ Axi4Channel.scala l321
      if(temp_when_1) begin
        unburstify_result_payload_last_1 = 1'b0; // @ Axi4Channel.scala l324
      end
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_payload_fragment_id_1 = unburstify_buffer_transaction_id_1; // @ Axi4Channel.scala l314
    end else begin
      unburstify_result_payload_fragment_id_1 = bridge_io_m_ar_payload_id; // @ Bundle.scala l141
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_payload_fragment_size_1 = unburstify_buffer_transaction_size_1; // @ Axi4Channel.scala l314
    end else begin
      unburstify_result_payload_fragment_size_1 = bridge_io_m_ar_payload_size; // @ Bundle.scala l141
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_payload_fragment_burst_1 = unburstify_buffer_transaction_burst_1; // @ Axi4Channel.scala l314
    end else begin
      unburstify_result_payload_fragment_burst_1 = bridge_io_m_ar_payload_burst; // @ Bundle.scala l141
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_payload_fragment_cache_1 = unburstify_buffer_transaction_cache_1; // @ Axi4Channel.scala l314
    end else begin
      unburstify_result_payload_fragment_cache_1 = bridge_io_m_ar_payload_cache; // @ Bundle.scala l141
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_payload_fragment_prot_1 = unburstify_buffer_transaction_prot_1; // @ Axi4Channel.scala l314
    end else begin
      unburstify_result_payload_fragment_prot_1 = bridge_io_m_ar_payload_prot; // @ Bundle.scala l141
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_payload_fragment_addr_1 = Axi4Incr_result_1; // @ Axi4Channel.scala l316
    end else begin
      unburstify_result_payload_fragment_addr_1 = bridge_io_m_ar_payload_addr; // @ Bundle.scala l141
    end
  end

  always @(*) begin
    unburstify_result_ready_1 = axif_readDataStage_ready; // @ Stream.scala l392
    if((! axif_readDataStage_valid)) begin
      unburstify_result_ready_1 = 1'b1; // @ Stream.scala l393
    end
  end

  assign axif_readDataStage_valid = unburstify_result_rValid; // @ Stream.scala l395
  assign axif_readDataStage_payload_last = unburstify_result_rData_last; // @ Stream.scala l396
  assign axif_readDataStage_payload_fragment_addr = unburstify_result_rData_fragment_addr; // @ Stream.scala l396
  assign axif_readDataStage_payload_fragment_id = unburstify_result_rData_fragment_id; // @ Stream.scala l396
  assign axif_readDataStage_payload_fragment_size = unburstify_result_rData_fragment_size; // @ Stream.scala l396
  assign axif_readDataStage_payload_fragment_burst = unburstify_result_rData_fragment_burst; // @ Stream.scala l396
  assign axif_readDataStage_payload_fragment_cache = unburstify_result_rData_fragment_cache; // @ Stream.scala l396
  assign axif_readDataStage_payload_fragment_prot = unburstify_result_rData_fragment_prot; // @ Stream.scala l396
  assign temp_axif_readDataStage_ready = (! axif_readHaltRequest); // @ BaseType.scala l299
  assign axif_readDataStage_haltWhen_valid = (axif_readDataStage_valid && temp_axif_readDataStage_ready); // @ Stream.scala l454
  assign axif_readDataStage_ready = (axif_readDataStage_haltWhen_ready && temp_axif_readDataStage_ready); // @ Stream.scala l455
  assign axif_readDataStage_haltWhen_payload_last = axif_readDataStage_payload_last; // @ Stream.scala l456
  assign axif_readDataStage_haltWhen_payload_fragment_addr = axif_readDataStage_payload_fragment_addr; // @ Stream.scala l456
  assign axif_readDataStage_haltWhen_payload_fragment_id = axif_readDataStage_payload_fragment_id; // @ Stream.scala l456
  assign axif_readDataStage_haltWhen_payload_fragment_size = axif_readDataStage_payload_fragment_size; // @ Stream.scala l456
  assign axif_readDataStage_haltWhen_payload_fragment_burst = axif_readDataStage_payload_fragment_burst; // @ Stream.scala l456
  assign axif_readDataStage_haltWhen_payload_fragment_cache = axif_readDataStage_payload_fragment_cache; // @ Stream.scala l456
  assign axif_readDataStage_haltWhen_payload_fragment_prot = axif_readDataStage_payload_fragment_prot; // @ Stream.scala l456
  assign axif_readDataStage_haltWhen_translated_valid = axif_readDataStage_haltWhen_valid; // @ Stream.scala l324
  assign axif_readDataStage_haltWhen_ready = axif_readDataStage_haltWhen_translated_ready; // @ Stream.scala l325
  assign axif_readDataStage_haltWhen_translated_payload_data = axif_readRsp_data; // @ Stream.scala l345
  assign axif_readDataStage_haltWhen_translated_payload_id = axif_readRsp_id; // @ Stream.scala l345
  assign axif_readDataStage_haltWhen_translated_payload_resp = axif_readRsp_resp; // @ Stream.scala l345
  assign axif_readDataStage_haltWhen_translated_payload_last = axif_readRsp_last; // @ Stream.scala l345
  assign axif_readDataStage_haltWhen_translated_ready = bridge_io_m_r_ready; // @ Stream.scala l316
  assign axif_writeRsp_payload_id = unburstify_result_payload_fragment_id; // @ Axi4SlaveFactory.scala l36
  always @(*) begin
    if(axif_writeErrorFlag) begin
      axif_writeRsp_payload_resp = 2'b10; // @ Axi4Channel.scala l210
    end else begin
      axif_writeRsp_payload_resp = 2'b00; // @ Axi4Channel.scala l208
    end
  end

  always @(*) begin
    if(axif_readErrorFlag) begin
      axif_readRsp_resp = 2'b10; // @ Axi4Channel.scala l240
    end else begin
      axif_readRsp_resp = 2'b00; // @ Axi4Channel.scala l238
    end
  end

  assign axif_readRsp_data = 128'h0; // @ Axi4SlaveFactory.scala l51
  assign axif_readRsp_last = axif_readDataStage_payload_last; // @ Axi4SlaveFactory.scala l52
  assign axif_readRsp_id = axif_readDataStage_payload_fragment_id; // @ Axi4SlaveFactory.scala l53
  assign axif_readOccur = (axif_readDataStage_haltWhen_translated_valid && bridge_io_m_r_ready); // @ BaseType.scala l305
  assign axif_readAddressMasked = (axif_readDataStage_payload_fragment_addr & (~ 32'h0000000f)); // @ BaseType.scala l299
  assign axif_writeAddressMasked = (unburstify_result_payload_fragment_addr & (~ 32'h0000000f)); // @ BaseType.scala l299
  always @(*) begin
    temp_push_valid = 1'b0; // @ BusSlaveFactory.scala l529
    case(axif_writeAddressMasked)
      32'h0 : begin
        if(axif_writeOccur) begin
          temp_push_valid = 1'b1; // @ BusSlaveFactory.scala l524
        end
      end
      default : begin
      end
    endcase
  end

  assign push_valid = temp_push_valid; // @ Stream.scala l315
  assign push_payload = bridge_io_m_w_payload_data[127 : 0]; // @ Stream.scala l317
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      unburstify_buffer_valid <= 1'b0; // @ Data.scala l410
      axif_writeRsp_rValid <= 1'b0; // @ Data.scala l410
      unburstify_buffer_valid_1 <= 1'b0; // @ Data.scala l410
      unburstify_result_rValid <= 1'b0; // @ Data.scala l410
    end else begin
      if(unburstify_result_ready) begin
        if(unburstify_buffer_last) begin
          unburstify_buffer_valid <= 1'b0; // @ Axi4Channel.scala l305
        end
      end
      if(!unburstify_buffer_valid) begin
        if(temp_when) begin
          if(unburstify_result_ready) begin
            unburstify_buffer_valid <= bridge_io_m_aw_valid; // @ Axi4Channel.scala l326
          end
        end
      end
      if(axif_writeRsp_ready) begin
        axif_writeRsp_rValid <= axif_writeRsp_valid; // @ Stream.scala l382
      end
      if(unburstify_result_ready_1) begin
        if(unburstify_buffer_last_1) begin
          unburstify_buffer_valid_1 <= 1'b0; // @ Axi4Channel.scala l305
        end
      end
      if(!unburstify_buffer_valid_1) begin
        if(temp_when_1) begin
          if(unburstify_result_ready_1) begin
            unburstify_buffer_valid_1 <= bridge_io_m_ar_valid; // @ Axi4Channel.scala l326
          end
        end
      end
      if(unburstify_result_ready_1) begin
        unburstify_result_rValid <= unburstify_result_valid_1; // @ Stream.scala l382
      end
    end
  end

  always @(posedge clk) begin
    if(unburstify_result_ready) begin
      unburstify_buffer_beat <= (unburstify_buffer_beat - 8'h01); // @ Axi4Channel.scala l302
      unburstify_buffer_transaction_addr[11 : 0] <= Axi4Incr_result[11 : 0]; // @ Axi4Channel.scala l303
    end
    if(!unburstify_buffer_valid) begin
      if(temp_when) begin
        if(unburstify_result_ready) begin
          unburstify_buffer_transaction_addr <= bridge_io_m_aw_payload_addr; // @ Bundle.scala l141
          unburstify_buffer_transaction_id <= bridge_io_m_aw_payload_id; // @ Bundle.scala l141
          unburstify_buffer_transaction_size <= bridge_io_m_aw_payload_size; // @ Bundle.scala l141
          unburstify_buffer_transaction_burst <= bridge_io_m_aw_payload_burst; // @ Bundle.scala l141
          unburstify_buffer_transaction_cache <= bridge_io_m_aw_payload_cache; // @ Bundle.scala l141
          unburstify_buffer_transaction_prot <= bridge_io_m_aw_payload_prot; // @ Bundle.scala l141
          unburstify_buffer_beat <= bridge_io_m_aw_payload_len; // @ Axi4Channel.scala l328
          unburstify_buffer_len <= bridge_io_m_aw_payload_len; // @ Axi4Channel.scala l329
        end
      end
    end
    if(axif_writeRsp_ready) begin
      axif_writeRsp_rData_id <= axif_writeRsp_payload_id; // @ Stream.scala l383
      axif_writeRsp_rData_resp <= axif_writeRsp_payload_resp; // @ Stream.scala l383
    end
    if(unburstify_result_ready_1) begin
      unburstify_buffer_beat_1 <= (unburstify_buffer_beat_1 - 8'h01); // @ Axi4Channel.scala l302
      unburstify_buffer_transaction_addr_1[11 : 0] <= Axi4Incr_result_1[11 : 0]; // @ Axi4Channel.scala l303
    end
    if(!unburstify_buffer_valid_1) begin
      if(temp_when_1) begin
        if(unburstify_result_ready_1) begin
          unburstify_buffer_transaction_addr_1 <= bridge_io_m_ar_payload_addr; // @ Bundle.scala l141
          unburstify_buffer_transaction_id_1 <= bridge_io_m_ar_payload_id; // @ Bundle.scala l141
          unburstify_buffer_transaction_size_1 <= bridge_io_m_ar_payload_size; // @ Bundle.scala l141
          unburstify_buffer_transaction_burst_1 <= bridge_io_m_ar_payload_burst; // @ Bundle.scala l141
          unburstify_buffer_transaction_cache_1 <= bridge_io_m_ar_payload_cache; // @ Bundle.scala l141
          unburstify_buffer_transaction_prot_1 <= bridge_io_m_ar_payload_prot; // @ Bundle.scala l141
          unburstify_buffer_beat_1 <= bridge_io_m_ar_payload_len; // @ Axi4Channel.scala l328
          unburstify_buffer_len_1 <= bridge_io_m_ar_payload_len; // @ Axi4Channel.scala l329
        end
      end
    end
    if(unburstify_result_ready_1) begin
      unburstify_result_rData_last <= unburstify_result_payload_last_1; // @ Stream.scala l383
      unburstify_result_rData_fragment_addr <= unburstify_result_payload_fragment_addr_1; // @ Stream.scala l383
      unburstify_result_rData_fragment_id <= unburstify_result_payload_fragment_id_1; // @ Stream.scala l383
      unburstify_result_rData_fragment_size <= unburstify_result_payload_fragment_size_1; // @ Stream.scala l383
      unburstify_result_rData_fragment_burst <= unburstify_result_payload_fragment_burst_1; // @ Stream.scala l383
      unburstify_result_rData_fragment_cache <= unburstify_result_payload_fragment_cache_1; // @ Stream.scala l383
      unburstify_result_rData_fragment_prot <= unburstify_result_payload_fragment_prot_1; // @ Stream.scala l383
    end
  end


endmodule

module StreamFifo (
  input  wire          io_push_valid,
  output wire          io_push_ready,
  input  wire [127:0]  io_push_payload,
  output wire          io_pop_valid,
  input  wire          io_pop_ready,
  output wire [127:0]  io_pop_payload,
  input  wire          io_flush,
  output wire [5:0]    io_occupancy,
  output wire [5:0]    io_availability,
  input  wire          clk,
  input  wire          resetn
);

  reg        [127:0]  logic_ram_spinal_port1;
  wire       [127:0]  temp_logic_ram_port;
  reg                 temp_1;
  wire                logic_ptr_doPush;
  wire                logic_ptr_doPop;
  wire                logic_ptr_full;
  wire                logic_ptr_empty;
  reg        [5:0]    logic_ptr_push;
  reg        [5:0]    logic_ptr_pop;
  wire       [5:0]    logic_ptr_occupancy;
  wire       [5:0]    logic_ptr_popOnIo;
  reg                 logic_ptr_wentUp;
  wire                io_push_fire;
  wire                logic_push_onRam_write_valid;
  wire       [4:0]    logic_push_onRam_write_payload_address;
  wire       [127:0]  logic_push_onRam_write_payload_data;
  wire                logic_pop_addressGen_valid;
  reg                 logic_pop_addressGen_ready;
  wire       [4:0]    logic_pop_addressGen_payload;
  wire                logic_pop_addressGen_fire;
  wire                logic_pop_sync_readArbitation_valid;
  wire                logic_pop_sync_readArbitation_ready;
  wire       [4:0]    logic_pop_sync_readArbitation_payload;
  reg                 logic_pop_addressGen_rValid;
  reg        [4:0]    logic_pop_addressGen_rData;
  wire                logic_pop_sync_readPort_cmd_valid;
  wire       [4:0]    logic_pop_sync_readPort_cmd_payload;
  wire       [127:0]  logic_pop_sync_readPort_rsp;
  wire                logic_pop_addressGen_toFlowFire_valid;
  wire       [4:0]    logic_pop_addressGen_toFlowFire_payload;
  wire                logic_pop_sync_readArbitation_translated_valid;
  wire                logic_pop_sync_readArbitation_translated_ready;
  wire       [127:0]  logic_pop_sync_readArbitation_translated_payload;
  wire                logic_pop_sync_readArbitation_fire;
  reg        [5:0]    logic_pop_sync_popReg;
  reg [127:0] logic_ram [0:31];

  assign temp_logic_ram_port = logic_push_onRam_write_payload_data;
  always @(posedge clk) begin
    if(temp_1) begin
      logic_ram[logic_push_onRam_write_payload_address] <= temp_logic_ram_port;
    end
  end

  always @(posedge clk) begin
    if(logic_pop_sync_readPort_cmd_valid) begin
      logic_ram_spinal_port1 <= logic_ram[logic_pop_sync_readPort_cmd_payload];
    end
  end

  always @(*) begin
    temp_1 = 1'b0; // @ when.scala l47
    if(logic_push_onRam_write_valid) begin
      temp_1 = 1'b1; // @ when.scala l52
    end
  end

  assign logic_ptr_full = (((logic_ptr_push ^ logic_ptr_popOnIo) ^ 6'h20) == 6'h0); // @ Stream.scala l1279
  assign logic_ptr_empty = (logic_ptr_push == logic_ptr_pop); // @ Stream.scala l1280
  assign logic_ptr_occupancy = (logic_ptr_push - logic_ptr_popOnIo); // @ Stream.scala l1322
  assign io_push_ready = (! logic_ptr_full); // @ Stream.scala l1340
  assign io_push_fire = (io_push_valid && io_push_ready); // @ BaseType.scala l305
  assign logic_ptr_doPush = io_push_fire; // @ Stream.scala l1341
  assign logic_push_onRam_write_valid = io_push_fire; // @ Stream.scala l1344
  assign logic_push_onRam_write_payload_address = logic_ptr_push[4:0]; // @ Stream.scala l1345
  assign logic_push_onRam_write_payload_data = io_push_payload; // @ Stream.scala l1346
  assign logic_pop_addressGen_valid = (! logic_ptr_empty); // @ Stream.scala l1357
  assign logic_pop_addressGen_payload = logic_ptr_pop[4:0]; // @ Stream.scala l1358
  assign logic_pop_addressGen_fire = (logic_pop_addressGen_valid && logic_pop_addressGen_ready); // @ BaseType.scala l305
  assign logic_ptr_doPop = logic_pop_addressGen_fire; // @ Stream.scala l1359
  always @(*) begin
    logic_pop_addressGen_ready = logic_pop_sync_readArbitation_ready; // @ Stream.scala l392
    if((! logic_pop_sync_readArbitation_valid)) begin
      logic_pop_addressGen_ready = 1'b1; // @ Stream.scala l393
    end
  end

  assign logic_pop_sync_readArbitation_valid = logic_pop_addressGen_rValid; // @ Stream.scala l395
  assign logic_pop_sync_readArbitation_payload = logic_pop_addressGen_rData; // @ Stream.scala l396
  assign logic_pop_sync_readPort_rsp = logic_ram_spinal_port1; // @ Mem.scala l120
  assign logic_pop_addressGen_toFlowFire_valid = logic_pop_addressGen_fire; // @ Stream.scala l103
  assign logic_pop_addressGen_toFlowFire_payload = logic_pop_addressGen_payload; // @ Stream.scala l104
  assign logic_pop_sync_readPort_cmd_valid = logic_pop_addressGen_toFlowFire_valid; // @ Stream.scala l1365
  assign logic_pop_sync_readPort_cmd_payload = logic_pop_addressGen_toFlowFire_payload; // @ Stream.scala l1365
  assign logic_pop_sync_readArbitation_translated_valid = logic_pop_sync_readArbitation_valid; // @ Stream.scala l324
  assign logic_pop_sync_readArbitation_ready = logic_pop_sync_readArbitation_translated_ready; // @ Stream.scala l325
  assign logic_pop_sync_readArbitation_translated_payload = logic_pop_sync_readPort_rsp; // @ Stream.scala l345
  assign io_pop_valid = logic_pop_sync_readArbitation_translated_valid; // @ Stream.scala l315
  assign logic_pop_sync_readArbitation_translated_ready = io_pop_ready; // @ Stream.scala l316
  assign io_pop_payload = logic_pop_sync_readArbitation_translated_payload; // @ Stream.scala l317
  assign logic_pop_sync_readArbitation_fire = (logic_pop_sync_readArbitation_valid && logic_pop_sync_readArbitation_ready); // @ BaseType.scala l305
  assign logic_ptr_popOnIo = logic_pop_sync_popReg; // @ Stream.scala l1369
  assign io_occupancy = logic_ptr_occupancy; // @ Stream.scala l1391
  assign io_availability = (6'h20 - logic_ptr_occupancy); // @ Stream.scala l1392
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      logic_ptr_push <= 6'h0; // @ Data.scala l410
      logic_ptr_pop <= 6'h0; // @ Data.scala l410
      logic_ptr_wentUp <= 1'b0; // @ Data.scala l410
      logic_pop_addressGen_rValid <= 1'b0; // @ Data.scala l410
      logic_pop_sync_popReg <= 6'h0; // @ Data.scala l410
    end else begin
      if((logic_ptr_doPush != logic_ptr_doPop)) begin
        logic_ptr_wentUp <= logic_ptr_doPush; // @ Stream.scala l1273
      end
      if(io_flush) begin
        logic_ptr_wentUp <= 1'b0; // @ Stream.scala l1273
      end
      if(logic_ptr_doPush) begin
        logic_ptr_push <= (logic_ptr_push + 6'h01); // @ Stream.scala l1307
      end
      if(logic_ptr_doPop) begin
        logic_ptr_pop <= (logic_ptr_pop + 6'h01); // @ Stream.scala l1311
      end
      if(io_flush) begin
        logic_ptr_push <= 6'h0; // @ Stream.scala l1316
        logic_ptr_pop <= 6'h0; // @ Stream.scala l1317
      end
      if(logic_pop_addressGen_ready) begin
        logic_pop_addressGen_rValid <= logic_pop_addressGen_valid; // @ Stream.scala l382
      end
      if(io_flush) begin
        logic_pop_addressGen_rValid <= 1'b0; // @ Stream.scala l390
      end
      if(logic_pop_sync_readArbitation_fire) begin
        logic_pop_sync_popReg <= logic_ptr_pop; // @ Stream.scala l1368
      end
      if(io_flush) begin
        logic_pop_sync_popReg <= 6'h0; // @ Stream.scala l1370
      end
    end
  end

  always @(posedge clk) begin
    if(logic_pop_addressGen_ready) begin
      logic_pop_addressGen_rData <= logic_pop_addressGen_payload; // @ Stream.scala l383
    end
  end


endmodule

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

  assign io_s_aw_ready = (! awHold); // @ InstAxi4ToStream128.scala l64
  assign io_s_aw_fire = (io_s_aw_valid && io_s_aw_ready); // @ BaseType.scala l305
  assign io_s_w_ready = (! wHold); // @ InstAxi4ToStream128.scala l70
  assign io_s_w_fire = (io_s_w_valid && io_s_w_ready); // @ BaseType.scala l305
  assign io_m_aw_valid = awHold; // @ InstAxi4ToStream128.scala l74
  assign io_m_aw_payload_addr = awReg_addr; // @ InstAxi4ToStream128.scala l75
  assign io_m_aw_payload_id = awReg_id; // @ InstAxi4ToStream128.scala l75
  assign io_m_aw_payload_len = awReg_len; // @ InstAxi4ToStream128.scala l75
  assign io_m_aw_payload_size = awReg_size; // @ InstAxi4ToStream128.scala l75
  assign io_m_aw_payload_burst = awReg_burst; // @ InstAxi4ToStream128.scala l75
  assign io_m_aw_payload_cache = awReg_cache; // @ InstAxi4ToStream128.scala l75
  assign io_m_aw_payload_prot = awReg_prot; // @ InstAxi4ToStream128.scala l75
  assign io_m_w_valid = wHold; // @ InstAxi4ToStream128.scala l76
  assign io_m_w_payload_data = wReg_data; // @ InstAxi4ToStream128.scala l77
  assign io_m_w_payload_last = wReg_last; // @ InstAxi4ToStream128.scala l77
  assign io_m_aw_fire = (io_m_aw_valid && io_m_aw_ready); // @ BaseType.scala l305
  assign io_m_w_fire = (io_m_w_valid && io_m_w_ready); // @ BaseType.scala l305
  assign io_m_b_ready = io_s_b_ready; // @ InstAxi4ToStream128.scala l85
  assign io_s_b_valid = io_m_b_valid; // @ InstAxi4ToStream128.scala l86
  assign io_s_b_payload_id = io_m_b_payload_id; // @ InstAxi4ToStream128.scala l87
  assign io_s_b_payload_resp = io_m_b_payload_resp; // @ InstAxi4ToStream128.scala l87
  assign io_m_ar_valid = io_s_ar_valid; // @ Stream.scala l315
  assign io_s_ar_ready = io_m_ar_ready; // @ Stream.scala l316
  assign io_m_ar_payload_addr = io_s_ar_payload_addr; // @ Stream.scala l317
  assign io_m_ar_payload_id = io_s_ar_payload_id; // @ Stream.scala l317
  assign io_m_ar_payload_len = io_s_ar_payload_len; // @ Stream.scala l317
  assign io_m_ar_payload_size = io_s_ar_payload_size; // @ Stream.scala l317
  assign io_m_ar_payload_burst = io_s_ar_payload_burst; // @ Stream.scala l317
  assign io_m_ar_payload_cache = io_s_ar_payload_cache; // @ Stream.scala l317
  assign io_m_ar_payload_prot = io_s_ar_payload_prot; // @ Stream.scala l317
  assign io_s_r_valid = io_m_r_valid; // @ Stream.scala l315
  assign io_m_r_ready = io_s_r_ready; // @ Stream.scala l316
  assign io_s_r_payload_data = io_m_r_payload_data; // @ Stream.scala l317
  assign io_s_r_payload_id = io_m_r_payload_id; // @ Stream.scala l317
  assign io_s_r_payload_resp = io_m_r_payload_resp; // @ Stream.scala l317
  assign io_s_r_payload_last = io_m_r_payload_last; // @ Stream.scala l317
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      awHold <= 1'b0; // @ Data.scala l410
      wHold <= 1'b0; // @ Data.scala l410
    end else begin
      if(io_s_aw_fire) begin
        awHold <= 1'b1; // @ InstAxi4ToStream128.scala l65
      end
      if(io_s_w_fire) begin
        wHold <= 1'b1; // @ InstAxi4ToStream128.scala l71
      end
      if((io_m_aw_fire && io_m_w_fire)) begin
        awHold <= 1'b0; // @ InstAxi4ToStream128.scala l80
        wHold <= 1'b0; // @ InstAxi4ToStream128.scala l81
      end
    end
  end

  always @(posedge clk) begin
    if(io_s_aw_fire) begin
      awReg_addr <= io_s_aw_payload_addr; // @ InstAxi4ToStream128.scala l65
      awReg_id <= io_s_aw_payload_id; // @ InstAxi4ToStream128.scala l65
      awReg_len <= io_s_aw_payload_len; // @ InstAxi4ToStream128.scala l65
      awReg_size <= io_s_aw_payload_size; // @ InstAxi4ToStream128.scala l65
      awReg_burst <= io_s_aw_payload_burst; // @ InstAxi4ToStream128.scala l65
      awReg_cache <= io_s_aw_payload_cache; // @ InstAxi4ToStream128.scala l65
      awReg_prot <= io_s_aw_payload_prot; // @ InstAxi4ToStream128.scala l65
    end
    if(io_s_w_fire) begin
      wReg_data <= io_s_w_payload_data; // @ InstAxi4ToStream128.scala l71
      wReg_last <= io_s_w_payload_last; // @ InstAxi4ToStream128.scala l71
    end
  end


endmodule

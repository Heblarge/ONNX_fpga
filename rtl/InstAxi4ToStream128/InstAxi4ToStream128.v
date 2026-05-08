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
  wire       [3:0]    _zz_Axi4Incr_alignMask;
  wire       [11:0]   _zz_Axi4Incr_baseIncr;
  wire       [2:0]    _zz_Axi4Incr_wrapCase_2;
  reg        [11:0]   _zz_Axi4Incr_result;
  wire       [10:0]   _zz_Axi4Incr_result_1;
  wire       [0:0]    _zz_Axi4Incr_result_2;
  wire       [9:0]    _zz_Axi4Incr_result_3;
  wire       [1:0]    _zz_Axi4Incr_result_4;
  wire       [8:0]    _zz_Axi4Incr_result_5;
  wire       [2:0]    _zz_Axi4Incr_result_6;
  wire       [7:0]    _zz_Axi4Incr_result_7;
  wire       [3:0]    _zz_Axi4Incr_result_8;
  wire       [6:0]    _zz_Axi4Incr_result_9;
  wire       [4:0]    _zz_Axi4Incr_result_10;
  wire       [5:0]    _zz_Axi4Incr_result_11;
  wire       [5:0]    _zz_Axi4Incr_result_12;
  wire       [4:0]    _zz_Axi4Incr_result_13;
  wire       [6:0]    _zz_Axi4Incr_result_14;
  wire       [3:0]    _zz_Axi4Incr_result_15;
  wire       [7:0]    _zz_Axi4Incr_result_16;
  wire       [3:0]    _zz_Axi4Incr_alignMask_1;
  wire       [11:0]   _zz_Axi4Incr_baseIncr_1;
  wire       [2:0]    _zz_Axi4Incr_wrapCase_1_1;
  reg        [11:0]   _zz_Axi4Incr_result_1_1;
  wire       [10:0]   _zz_Axi4Incr_result_1_2;
  wire       [0:0]    _zz_Axi4Incr_result_1_3;
  wire       [9:0]    _zz_Axi4Incr_result_1_4;
  wire       [1:0]    _zz_Axi4Incr_result_1_5;
  wire       [8:0]    _zz_Axi4Incr_result_1_6;
  wire       [2:0]    _zz_Axi4Incr_result_1_7;
  wire       [7:0]    _zz_Axi4Incr_result_1_8;
  wire       [3:0]    _zz_Axi4Incr_result_1_9;
  wire       [6:0]    _zz_Axi4Incr_result_1_10;
  wire       [4:0]    _zz_Axi4Incr_result_1_11;
  wire       [5:0]    _zz_Axi4Incr_result_1_12;
  wire       [5:0]    _zz_Axi4Incr_result_1_13;
  wire       [4:0]    _zz_Axi4Incr_result_1_14;
  wire       [6:0]    _zz_Axi4Incr_result_1_15;
  wire       [3:0]    _zz_Axi4Incr_result_1_16;
  wire       [7:0]    _zz_Axi4Incr_result_1_17;
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
  reg        [1:0]    _zz_Axi4Incr_wrapCase;
  wire       [2:0]    Axi4Incr_wrapCase;
  wire                when_Axi4Channel_l323;
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
  wire                when_Stream_l393;
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
  reg        [1:0]    _zz_Axi4Incr_wrapCase_1;
  wire       [2:0]    Axi4Incr_wrapCase_1;
  wire                when_Axi4Channel_l323_1;
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
  wire                when_Stream_l393_1;
  wire       [127:0]  axif_readRsp_data;
  wire       [15:0]   axif_readRsp_id;
  reg        [1:0]    axif_readRsp_resp;
  wire                axif_readRsp_last;
  wire                _zz_axif_readDataStage_ready;
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
  reg                 _zz_push_valid;
  wire                when_Axi4SlaveFactory_l85;
  wire                when_BusSlaveFactory_l550;

  assign _zz_Axi4Incr_alignMask = {(3'b011 < Axi4Incr_validSize),{(3'b010 < Axi4Incr_validSize),{(3'b001 < Axi4Incr_validSize),(3'b000 < Axi4Incr_validSize)}}};
  assign _zz_Axi4Incr_baseIncr = {7'd0, Axi4Incr_sizeValue};
  assign _zz_Axi4Incr_wrapCase_2 = {1'd0, _zz_Axi4Incr_wrapCase};
  assign _zz_Axi4Incr_alignMask_1 = {(3'b011 < Axi4Incr_validSize_1),{(3'b010 < Axi4Incr_validSize_1),{(3'b001 < Axi4Incr_validSize_1),(3'b000 < Axi4Incr_validSize_1)}}};
  assign _zz_Axi4Incr_baseIncr_1 = {7'd0, Axi4Incr_sizeValue_1};
  assign _zz_Axi4Incr_wrapCase_1_1 = {1'd0, _zz_Axi4Incr_wrapCase_1};
  assign _zz_Axi4Incr_result_1 = Axi4Incr_base[11 : 1];
  assign _zz_Axi4Incr_result_2 = Axi4Incr_baseIncr[0 : 0];
  assign _zz_Axi4Incr_result_3 = Axi4Incr_base[11 : 2];
  assign _zz_Axi4Incr_result_4 = Axi4Incr_baseIncr[1 : 0];
  assign _zz_Axi4Incr_result_5 = Axi4Incr_base[11 : 3];
  assign _zz_Axi4Incr_result_6 = Axi4Incr_baseIncr[2 : 0];
  assign _zz_Axi4Incr_result_7 = Axi4Incr_base[11 : 4];
  assign _zz_Axi4Incr_result_8 = Axi4Incr_baseIncr[3 : 0];
  assign _zz_Axi4Incr_result_9 = Axi4Incr_base[11 : 5];
  assign _zz_Axi4Incr_result_10 = Axi4Incr_baseIncr[4 : 0];
  assign _zz_Axi4Incr_result_11 = Axi4Incr_base[11 : 6];
  assign _zz_Axi4Incr_result_12 = Axi4Incr_baseIncr[5 : 0];
  assign _zz_Axi4Incr_result_13 = Axi4Incr_base[11 : 7];
  assign _zz_Axi4Incr_result_14 = Axi4Incr_baseIncr[6 : 0];
  assign _zz_Axi4Incr_result_15 = Axi4Incr_base[11 : 8];
  assign _zz_Axi4Incr_result_16 = Axi4Incr_baseIncr[7 : 0];
  assign _zz_Axi4Incr_result_1_2 = Axi4Incr_base_1[11 : 1];
  assign _zz_Axi4Incr_result_1_3 = Axi4Incr_baseIncr_1[0 : 0];
  assign _zz_Axi4Incr_result_1_4 = Axi4Incr_base_1[11 : 2];
  assign _zz_Axi4Incr_result_1_5 = Axi4Incr_baseIncr_1[1 : 0];
  assign _zz_Axi4Incr_result_1_6 = Axi4Incr_base_1[11 : 3];
  assign _zz_Axi4Incr_result_1_7 = Axi4Incr_baseIncr_1[2 : 0];
  assign _zz_Axi4Incr_result_1_8 = Axi4Incr_base_1[11 : 4];
  assign _zz_Axi4Incr_result_1_9 = Axi4Incr_baseIncr_1[3 : 0];
  assign _zz_Axi4Incr_result_1_10 = Axi4Incr_base_1[11 : 5];
  assign _zz_Axi4Incr_result_1_11 = Axi4Incr_baseIncr_1[4 : 0];
  assign _zz_Axi4Incr_result_1_12 = Axi4Incr_base_1[11 : 6];
  assign _zz_Axi4Incr_result_1_13 = Axi4Incr_baseIncr_1[5 : 0];
  assign _zz_Axi4Incr_result_1_14 = Axi4Incr_base_1[11 : 7];
  assign _zz_Axi4Incr_result_1_15 = Axi4Incr_baseIncr_1[6 : 0];
  assign _zz_Axi4Incr_result_1_16 = Axi4Incr_base_1[11 : 8];
  assign _zz_Axi4Incr_result_1_17 = Axi4Incr_baseIncr_1[7 : 0];
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
      3'b000 : _zz_Axi4Incr_result = {_zz_Axi4Incr_result_1,_zz_Axi4Incr_result_2};
      3'b001 : _zz_Axi4Incr_result = {_zz_Axi4Incr_result_3,_zz_Axi4Incr_result_4};
      3'b010 : _zz_Axi4Incr_result = {_zz_Axi4Incr_result_5,_zz_Axi4Incr_result_6};
      3'b011 : _zz_Axi4Incr_result = {_zz_Axi4Incr_result_7,_zz_Axi4Incr_result_8};
      3'b100 : _zz_Axi4Incr_result = {_zz_Axi4Incr_result_9,_zz_Axi4Incr_result_10};
      3'b101 : _zz_Axi4Incr_result = {_zz_Axi4Incr_result_11,_zz_Axi4Incr_result_12};
      3'b110 : _zz_Axi4Incr_result = {_zz_Axi4Incr_result_13,_zz_Axi4Incr_result_14};
      default : _zz_Axi4Incr_result = {_zz_Axi4Incr_result_15,_zz_Axi4Incr_result_16};
    endcase
  end

  always @(*) begin
    case(Axi4Incr_wrapCase_1)
      3'b000 : _zz_Axi4Incr_result_1_1 = {_zz_Axi4Incr_result_1_2,_zz_Axi4Incr_result_1_3};
      3'b001 : _zz_Axi4Incr_result_1_1 = {_zz_Axi4Incr_result_1_4,_zz_Axi4Incr_result_1_5};
      3'b010 : _zz_Axi4Incr_result_1_1 = {_zz_Axi4Incr_result_1_6,_zz_Axi4Incr_result_1_7};
      3'b011 : _zz_Axi4Incr_result_1_1 = {_zz_Axi4Incr_result_1_8,_zz_Axi4Incr_result_1_9};
      3'b100 : _zz_Axi4Incr_result_1_1 = {_zz_Axi4Incr_result_1_10,_zz_Axi4Incr_result_1_11};
      3'b101 : _zz_Axi4Incr_result_1_1 = {_zz_Axi4Incr_result_1_12,_zz_Axi4Incr_result_1_13};
      3'b110 : _zz_Axi4Incr_result_1_1 = {_zz_Axi4Incr_result_1_14,_zz_Axi4Incr_result_1_15};
      default : _zz_Axi4Incr_result_1_1 = {_zz_Axi4Incr_result_1_16,_zz_Axi4Incr_result_1_17};
    endcase
  end

  assign io_axi_aw_ready = bridge_io_s_aw_ready;
  assign io_axi_w_ready = bridge_io_s_w_ready;
  assign io_axi_b_valid = bridge_io_s_b_valid;
  assign io_axi_b_payload_id = bridge_io_s_b_payload_id;
  assign io_axi_b_payload_resp = bridge_io_s_b_payload_resp;
  assign io_axi_ar_ready = bridge_io_s_ar_ready;
  assign io_axi_r_valid = bridge_io_s_r_valid;
  assign io_axi_r_payload_data = bridge_io_s_r_payload_data;
  assign io_axi_r_payload_id = bridge_io_s_r_payload_id;
  assign io_axi_r_payload_resp = bridge_io_s_r_payload_resp;
  assign io_axi_r_payload_last = bridge_io_s_r_payload_last;
  assign push_ready = streamFifo_1_io_push_ready;
  assign io_out_valid = streamFifo_1_io_pop_valid;
  assign io_out_payload = streamFifo_1_io_pop_payload;
  assign axif_readErrorFlag = 1'b0;
  assign axif_writeErrorFlag = 1'b0;
  assign axif_readHaltRequest = 1'b0;
  always @(*) begin
    axif_writeHaltRequest = 1'b0;
    if(when_Axi4SlaveFactory_l85) begin
      if(axif_writeJoinEvent_valid) begin
        if(when_BusSlaveFactory_l550) begin
          axif_writeHaltRequest = 1'b1;
        end
      end
    end
  end

  assign unburstify_buffer_last = (unburstify_buffer_beat == 8'h01);
  assign Axi4Incr_validSize = unburstify_buffer_transaction_size[2 : 0];
  assign Axi4Incr_highCat = unburstify_buffer_transaction_addr[31 : 12];
  assign Axi4Incr_sizeValue = {(3'b100 == Axi4Incr_validSize),{(3'b011 == Axi4Incr_validSize),{(3'b010 == Axi4Incr_validSize),{(3'b001 == Axi4Incr_validSize),(3'b000 == Axi4Incr_validSize)}}}};
  assign Axi4Incr_alignMask = {8'd0, _zz_Axi4Incr_alignMask};
  assign Axi4Incr_base = (unburstify_buffer_transaction_addr[11 : 0] & (~ Axi4Incr_alignMask));
  assign Axi4Incr_baseIncr = (Axi4Incr_base + _zz_Axi4Incr_baseIncr);
  always @(*) begin
    casez(unburstify_buffer_len)
      8'b????1??? : begin
        _zz_Axi4Incr_wrapCase = 2'b11;
      end
      8'b????01?? : begin
        _zz_Axi4Incr_wrapCase = 2'b10;
      end
      8'b????001? : begin
        _zz_Axi4Incr_wrapCase = 2'b01;
      end
      default : begin
        _zz_Axi4Incr_wrapCase = 2'b00;
      end
    endcase
  end

  assign Axi4Incr_wrapCase = (Axi4Incr_validSize + _zz_Axi4Incr_wrapCase_2);
  always @(*) begin
    case(unburstify_buffer_transaction_burst)
      2'b00 : begin
        Axi4Incr_result = unburstify_buffer_transaction_addr;
      end
      2'b10 : begin
        Axi4Incr_result = {Axi4Incr_highCat,_zz_Axi4Incr_result};
      end
      default : begin
        Axi4Incr_result = {Axi4Incr_highCat,Axi4Incr_baseIncr};
      end
    endcase
  end

  always @(*) begin
    bridge_io_m_aw_ready = 1'b0;
    if(!unburstify_buffer_valid) begin
      bridge_io_m_aw_ready = unburstify_result_ready;
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_valid = 1'b1;
    end else begin
      unburstify_result_valid = bridge_io_m_aw_valid;
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_payload_last = unburstify_buffer_last;
    end else begin
      unburstify_result_payload_last = 1'b1;
      if(when_Axi4Channel_l323) begin
        unburstify_result_payload_last = 1'b0;
      end
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_payload_fragment_id = unburstify_buffer_transaction_id;
    end else begin
      unburstify_result_payload_fragment_id = bridge_io_m_aw_payload_id;
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_payload_fragment_size = unburstify_buffer_transaction_size;
    end else begin
      unburstify_result_payload_fragment_size = bridge_io_m_aw_payload_size;
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_payload_fragment_burst = unburstify_buffer_transaction_burst;
    end else begin
      unburstify_result_payload_fragment_burst = bridge_io_m_aw_payload_burst;
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_payload_fragment_cache = unburstify_buffer_transaction_cache;
    end else begin
      unburstify_result_payload_fragment_cache = bridge_io_m_aw_payload_cache;
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_payload_fragment_prot = unburstify_buffer_transaction_prot;
    end else begin
      unburstify_result_payload_fragment_prot = bridge_io_m_aw_payload_prot;
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid) begin
      unburstify_result_payload_fragment_addr = Axi4Incr_result;
    end else begin
      unburstify_result_payload_fragment_addr = bridge_io_m_aw_payload_addr;
    end
  end

  assign when_Axi4Channel_l323 = (bridge_io_m_aw_payload_len != 8'h0);
  assign axif_writeOccur = (axif_writeJoinEvent_valid && axif_writeJoinEvent_ready);
  assign axif_writeJoinEvent_valid = (unburstify_result_valid && bridge_io_m_w_valid);
  assign unburstify_result_ready = axif_writeOccur;
  always @(*) begin
    axif_writeRsp_ready = axif_writeRsp_stage_ready;
    if(when_Stream_l393) begin
      axif_writeRsp_ready = 1'b1;
    end
  end

  assign when_Stream_l393 = (! axif_writeRsp_stage_valid);
  assign axif_writeRsp_stage_valid = axif_writeRsp_rValid;
  assign axif_writeRsp_stage_payload_id = axif_writeRsp_rData_id;
  assign axif_writeRsp_stage_payload_resp = axif_writeRsp_rData_resp;
  assign axif_writeRsp_stage_ready = bridge_io_m_b_ready;
  always @(*) begin
    if(bridge_io_m_w_payload_last) begin
      axif_writeJoinEvent_ready = (axif_writeRsp_ready && (! axif_writeHaltRequest));
    end else begin
      axif_writeJoinEvent_ready = (! axif_writeHaltRequest);
    end
  end

  always @(*) begin
    if(bridge_io_m_w_payload_last) begin
      axif_writeRsp_valid = axif_writeOccur;
    end else begin
      axif_writeRsp_valid = 1'b0;
    end
  end

  assign unburstify_buffer_last_1 = (unburstify_buffer_beat_1 == 8'h01);
  assign Axi4Incr_validSize_1 = unburstify_buffer_transaction_size_1[2 : 0];
  assign Axi4Incr_highCat_1 = unburstify_buffer_transaction_addr_1[31 : 12];
  assign Axi4Incr_sizeValue_1 = {(3'b100 == Axi4Incr_validSize_1),{(3'b011 == Axi4Incr_validSize_1),{(3'b010 == Axi4Incr_validSize_1),{(3'b001 == Axi4Incr_validSize_1),(3'b000 == Axi4Incr_validSize_1)}}}};
  assign Axi4Incr_alignMask_1 = {8'd0, _zz_Axi4Incr_alignMask_1};
  assign Axi4Incr_base_1 = (unburstify_buffer_transaction_addr_1[11 : 0] & (~ Axi4Incr_alignMask_1));
  assign Axi4Incr_baseIncr_1 = (Axi4Incr_base_1 + _zz_Axi4Incr_baseIncr_1);
  always @(*) begin
    casez(unburstify_buffer_len_1)
      8'b????1??? : begin
        _zz_Axi4Incr_wrapCase_1 = 2'b11;
      end
      8'b????01?? : begin
        _zz_Axi4Incr_wrapCase_1 = 2'b10;
      end
      8'b????001? : begin
        _zz_Axi4Incr_wrapCase_1 = 2'b01;
      end
      default : begin
        _zz_Axi4Incr_wrapCase_1 = 2'b00;
      end
    endcase
  end

  assign Axi4Incr_wrapCase_1 = (Axi4Incr_validSize_1 + _zz_Axi4Incr_wrapCase_1_1);
  always @(*) begin
    case(unburstify_buffer_transaction_burst_1)
      2'b00 : begin
        Axi4Incr_result_1 = unburstify_buffer_transaction_addr_1;
      end
      2'b10 : begin
        Axi4Incr_result_1 = {Axi4Incr_highCat_1,_zz_Axi4Incr_result_1_1};
      end
      default : begin
        Axi4Incr_result_1 = {Axi4Incr_highCat_1,Axi4Incr_baseIncr_1};
      end
    endcase
  end

  always @(*) begin
    bridge_io_m_ar_ready = 1'b0;
    if(!unburstify_buffer_valid_1) begin
      bridge_io_m_ar_ready = unburstify_result_ready_1;
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_valid_1 = 1'b1;
    end else begin
      unburstify_result_valid_1 = bridge_io_m_ar_valid;
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_payload_last_1 = unburstify_buffer_last_1;
    end else begin
      unburstify_result_payload_last_1 = 1'b1;
      if(when_Axi4Channel_l323_1) begin
        unburstify_result_payload_last_1 = 1'b0;
      end
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_payload_fragment_id_1 = unburstify_buffer_transaction_id_1;
    end else begin
      unburstify_result_payload_fragment_id_1 = bridge_io_m_ar_payload_id;
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_payload_fragment_size_1 = unburstify_buffer_transaction_size_1;
    end else begin
      unburstify_result_payload_fragment_size_1 = bridge_io_m_ar_payload_size;
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_payload_fragment_burst_1 = unburstify_buffer_transaction_burst_1;
    end else begin
      unburstify_result_payload_fragment_burst_1 = bridge_io_m_ar_payload_burst;
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_payload_fragment_cache_1 = unburstify_buffer_transaction_cache_1;
    end else begin
      unburstify_result_payload_fragment_cache_1 = bridge_io_m_ar_payload_cache;
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_payload_fragment_prot_1 = unburstify_buffer_transaction_prot_1;
    end else begin
      unburstify_result_payload_fragment_prot_1 = bridge_io_m_ar_payload_prot;
    end
  end

  always @(*) begin
    if(unburstify_buffer_valid_1) begin
      unburstify_result_payload_fragment_addr_1 = Axi4Incr_result_1;
    end else begin
      unburstify_result_payload_fragment_addr_1 = bridge_io_m_ar_payload_addr;
    end
  end

  assign when_Axi4Channel_l323_1 = (bridge_io_m_ar_payload_len != 8'h0);
  always @(*) begin
    unburstify_result_ready_1 = axif_readDataStage_ready;
    if(when_Stream_l393_1) begin
      unburstify_result_ready_1 = 1'b1;
    end
  end

  assign when_Stream_l393_1 = (! axif_readDataStage_valid);
  assign axif_readDataStage_valid = unburstify_result_rValid;
  assign axif_readDataStage_payload_last = unburstify_result_rData_last;
  assign axif_readDataStage_payload_fragment_addr = unburstify_result_rData_fragment_addr;
  assign axif_readDataStage_payload_fragment_id = unburstify_result_rData_fragment_id;
  assign axif_readDataStage_payload_fragment_size = unburstify_result_rData_fragment_size;
  assign axif_readDataStage_payload_fragment_burst = unburstify_result_rData_fragment_burst;
  assign axif_readDataStage_payload_fragment_cache = unburstify_result_rData_fragment_cache;
  assign axif_readDataStage_payload_fragment_prot = unburstify_result_rData_fragment_prot;
  assign _zz_axif_readDataStage_ready = (! axif_readHaltRequest);
  assign axif_readDataStage_haltWhen_valid = (axif_readDataStage_valid && _zz_axif_readDataStage_ready);
  assign axif_readDataStage_ready = (axif_readDataStage_haltWhen_ready && _zz_axif_readDataStage_ready);
  assign axif_readDataStage_haltWhen_payload_last = axif_readDataStage_payload_last;
  assign axif_readDataStage_haltWhen_payload_fragment_addr = axif_readDataStage_payload_fragment_addr;
  assign axif_readDataStage_haltWhen_payload_fragment_id = axif_readDataStage_payload_fragment_id;
  assign axif_readDataStage_haltWhen_payload_fragment_size = axif_readDataStage_payload_fragment_size;
  assign axif_readDataStage_haltWhen_payload_fragment_burst = axif_readDataStage_payload_fragment_burst;
  assign axif_readDataStage_haltWhen_payload_fragment_cache = axif_readDataStage_payload_fragment_cache;
  assign axif_readDataStage_haltWhen_payload_fragment_prot = axif_readDataStage_payload_fragment_prot;
  assign axif_readDataStage_haltWhen_translated_valid = axif_readDataStage_haltWhen_valid;
  assign axif_readDataStage_haltWhen_ready = axif_readDataStage_haltWhen_translated_ready;
  assign axif_readDataStage_haltWhen_translated_payload_data = axif_readRsp_data;
  assign axif_readDataStage_haltWhen_translated_payload_id = axif_readRsp_id;
  assign axif_readDataStage_haltWhen_translated_payload_resp = axif_readRsp_resp;
  assign axif_readDataStage_haltWhen_translated_payload_last = axif_readRsp_last;
  assign axif_readDataStage_haltWhen_translated_ready = bridge_io_m_r_ready;
  assign axif_writeRsp_payload_id = unburstify_result_payload_fragment_id;
  always @(*) begin
    if(axif_writeErrorFlag) begin
      axif_writeRsp_payload_resp = 2'b10;
    end else begin
      axif_writeRsp_payload_resp = 2'b00;
    end
  end

  always @(*) begin
    if(axif_readErrorFlag) begin
      axif_readRsp_resp = 2'b10;
    end else begin
      axif_readRsp_resp = 2'b00;
    end
  end

  assign axif_readRsp_data = 128'h0;
  assign axif_readRsp_last = axif_readDataStage_payload_last;
  assign axif_readRsp_id = axif_readDataStage_payload_fragment_id;
  assign axif_readOccur = (axif_readDataStage_haltWhen_translated_valid && bridge_io_m_r_ready);
  assign axif_readAddressMasked = (axif_readDataStage_payload_fragment_addr & (~ 32'h0000000f));
  assign axif_writeAddressMasked = (unburstify_result_payload_fragment_addr & (~ 32'h0000000f));
  always @(*) begin
    _zz_push_valid = 1'b0;
    case(axif_writeAddressMasked)
      32'h0 : begin
        if(axif_writeOccur) begin
          _zz_push_valid = 1'b1;
        end
      end
      default : begin
      end
    endcase
  end

  assign push_valid = _zz_push_valid;
  assign push_payload = bridge_io_m_w_payload_data[127 : 0];
  assign when_Axi4SlaveFactory_l85 = ((axif_writeAddressMasked & (~ 32'h0000000f)) == 32'h0);
  assign when_BusSlaveFactory_l550 = (! push_ready);
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      unburstify_buffer_valid <= 1'b0;
      axif_writeRsp_rValid <= 1'b0;
      unburstify_buffer_valid_1 <= 1'b0;
      unburstify_result_rValid <= 1'b0;
    end else begin
      if(unburstify_result_ready) begin
        if(unburstify_buffer_last) begin
          unburstify_buffer_valid <= 1'b0;
        end
      end
      if(!unburstify_buffer_valid) begin
        if(when_Axi4Channel_l323) begin
          if(unburstify_result_ready) begin
            unburstify_buffer_valid <= bridge_io_m_aw_valid;
          end
        end
      end
      if(axif_writeRsp_ready) begin
        axif_writeRsp_rValid <= axif_writeRsp_valid;
      end
      if(unburstify_result_ready_1) begin
        if(unburstify_buffer_last_1) begin
          unburstify_buffer_valid_1 <= 1'b0;
        end
      end
      if(!unburstify_buffer_valid_1) begin
        if(when_Axi4Channel_l323_1) begin
          if(unburstify_result_ready_1) begin
            unburstify_buffer_valid_1 <= bridge_io_m_ar_valid;
          end
        end
      end
      if(unburstify_result_ready_1) begin
        unburstify_result_rValid <= unburstify_result_valid_1;
      end
    end
  end

  always @(posedge clk) begin
    if(unburstify_result_ready) begin
      unburstify_buffer_beat <= (unburstify_buffer_beat - 8'h01);
      unburstify_buffer_transaction_addr[11 : 0] <= Axi4Incr_result[11 : 0];
    end
    if(!unburstify_buffer_valid) begin
      if(when_Axi4Channel_l323) begin
        if(unburstify_result_ready) begin
          unburstify_buffer_transaction_addr <= bridge_io_m_aw_payload_addr;
          unburstify_buffer_transaction_id <= bridge_io_m_aw_payload_id;
          unburstify_buffer_transaction_size <= bridge_io_m_aw_payload_size;
          unburstify_buffer_transaction_burst <= bridge_io_m_aw_payload_burst;
          unburstify_buffer_transaction_cache <= bridge_io_m_aw_payload_cache;
          unburstify_buffer_transaction_prot <= bridge_io_m_aw_payload_prot;
          unburstify_buffer_beat <= bridge_io_m_aw_payload_len;
          unburstify_buffer_len <= bridge_io_m_aw_payload_len;
        end
      end
    end
    if(axif_writeRsp_ready) begin
      axif_writeRsp_rData_id <= axif_writeRsp_payload_id;
      axif_writeRsp_rData_resp <= axif_writeRsp_payload_resp;
    end
    if(unburstify_result_ready_1) begin
      unburstify_buffer_beat_1 <= (unburstify_buffer_beat_1 - 8'h01);
      unburstify_buffer_transaction_addr_1[11 : 0] <= Axi4Incr_result_1[11 : 0];
    end
    if(!unburstify_buffer_valid_1) begin
      if(when_Axi4Channel_l323_1) begin
        if(unburstify_result_ready_1) begin
          unburstify_buffer_transaction_addr_1 <= bridge_io_m_ar_payload_addr;
          unburstify_buffer_transaction_id_1 <= bridge_io_m_ar_payload_id;
          unburstify_buffer_transaction_size_1 <= bridge_io_m_ar_payload_size;
          unburstify_buffer_transaction_burst_1 <= bridge_io_m_ar_payload_burst;
          unburstify_buffer_transaction_cache_1 <= bridge_io_m_ar_payload_cache;
          unburstify_buffer_transaction_prot_1 <= bridge_io_m_ar_payload_prot;
          unburstify_buffer_beat_1 <= bridge_io_m_ar_payload_len;
          unburstify_buffer_len_1 <= bridge_io_m_ar_payload_len;
        end
      end
    end
    if(unburstify_result_ready_1) begin
      unburstify_result_rData_last <= unburstify_result_payload_last_1;
      unburstify_result_rData_fragment_addr <= unburstify_result_payload_fragment_addr_1;
      unburstify_result_rData_fragment_id <= unburstify_result_payload_fragment_id_1;
      unburstify_result_rData_fragment_size <= unburstify_result_payload_fragment_size_1;
      unburstify_result_rData_fragment_burst <= unburstify_result_payload_fragment_burst_1;
      unburstify_result_rData_fragment_cache <= unburstify_result_payload_fragment_cache_1;
      unburstify_result_rData_fragment_prot <= unburstify_result_payload_fragment_prot_1;
    end
  end


endmodule

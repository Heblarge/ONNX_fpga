// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : Inst128_Wrapper
// Git hash  : 2a282c45d9b6a060f56315ddc07c4370a22ef2f2

`timescale 1ns/1ps

module Inst128_Wrapper (
  input  wire          io_s_axi_aw_valid,
  output wire          io_s_axi_aw_ready,
  input  wire [11:0]   io_s_axi_aw_payload_addr,
  input  wire [2:0]    io_s_axi_aw_payload_prot,
  input  wire          io_s_axi_w_valid,
  output wire          io_s_axi_w_ready,
  input  wire [63:0]   io_s_axi_w_payload_data,
  input  wire [7:0]    io_s_axi_w_payload_strb,
  output wire          io_s_axi_b_valid,
  input  wire          io_s_axi_b_ready,
  output wire [1:0]    io_s_axi_b_payload_resp,
  input  wire          io_s_axi_ar_valid,
  output wire          io_s_axi_ar_ready,
  input  wire [11:0]   io_s_axi_ar_payload_addr,
  input  wire [2:0]    io_s_axi_ar_payload_prot,
  output wire          io_s_axi_r_valid,
  input  wire          io_s_axi_r_ready,
  output wire [63:0]   io_s_axi_r_payload_data,
  output wire [1:0]    io_s_axi_r_payload_resp,
  output wire          io_m_stream_valid,
  input  wire          io_m_stream_ready,
  output wire [127:0]  io_m_stream_payload,
  input  wire          clk,
  input  wire          resetn
);

  wire                core_io_s_axi_aw_ready;
  wire                core_io_s_axi_w_ready;
  wire                core_io_s_axi_b_valid;
  wire       [1:0]    core_io_s_axi_b_payload_resp;
  wire                core_io_s_axi_ar_ready;
  wire                core_io_s_axi_r_valid;
  wire       [63:0]   core_io_s_axi_r_payload_data;
  wire       [1:0]    core_io_s_axi_r_payload_resp;
  wire                core_io_m_stream_valid;
  wire       [31:0]   core_io_m_stream_payload_SLICE0;
  wire       [31:0]   core_io_m_stream_payload_SLICE1;
  wire       [31:0]   core_io_m_stream_payload_SLICE2;
  wire       [31:0]   core_io_m_stream_payload_SLICE3;
  wire                core_io_busy;

  Axi4LiteToStream core (
    .io_s_axi_aw_valid          (io_s_axi_aw_valid                    ), //i
    .io_s_axi_aw_ready          (core_io_s_axi_aw_ready               ), //o
    .io_s_axi_aw_payload_addr   (io_s_axi_aw_payload_addr[11:0]       ), //i
    .io_s_axi_aw_payload_prot   (io_s_axi_aw_payload_prot[2:0]        ), //i
    .io_s_axi_w_valid           (io_s_axi_w_valid                     ), //i
    .io_s_axi_w_ready           (core_io_s_axi_w_ready                ), //o
    .io_s_axi_w_payload_data    (io_s_axi_w_payload_data[63:0]        ), //i
    .io_s_axi_w_payload_strb    (io_s_axi_w_payload_strb[7:0]         ), //i
    .io_s_axi_b_valid           (core_io_s_axi_b_valid                ), //o
    .io_s_axi_b_ready           (io_s_axi_b_ready                     ), //i
    .io_s_axi_b_payload_resp    (core_io_s_axi_b_payload_resp[1:0]    ), //o
    .io_s_axi_ar_valid          (io_s_axi_ar_valid                    ), //i
    .io_s_axi_ar_ready          (core_io_s_axi_ar_ready               ), //o
    .io_s_axi_ar_payload_addr   (io_s_axi_ar_payload_addr[11:0]       ), //i
    .io_s_axi_ar_payload_prot   (io_s_axi_ar_payload_prot[2:0]        ), //i
    .io_s_axi_r_valid           (core_io_s_axi_r_valid                ), //o
    .io_s_axi_r_ready           (io_s_axi_r_ready                     ), //i
    .io_s_axi_r_payload_data    (core_io_s_axi_r_payload_data[63:0]   ), //o
    .io_s_axi_r_payload_resp    (core_io_s_axi_r_payload_resp[1:0]    ), //o
    .io_m_stream_valid          (core_io_m_stream_valid               ), //o
    .io_m_stream_ready          (io_m_stream_ready                    ), //i
    .io_m_stream_payload_SLICE0 (core_io_m_stream_payload_SLICE0[31:0]), //o
    .io_m_stream_payload_SLICE1 (core_io_m_stream_payload_SLICE1[31:0]), //o
    .io_m_stream_payload_SLICE2 (core_io_m_stream_payload_SLICE2[31:0]), //o
    .io_m_stream_payload_SLICE3 (core_io_m_stream_payload_SLICE3[31:0]), //o
    .io_busy                    (core_io_busy                         ), //o
    .clk                        (clk                                  ), //i
    .resetn                     (resetn                               )  //i
  );
  assign io_s_axi_aw_ready = core_io_s_axi_aw_ready;
  assign io_s_axi_w_ready = core_io_s_axi_w_ready;
  assign io_s_axi_b_valid = core_io_s_axi_b_valid;
  assign io_s_axi_b_payload_resp = core_io_s_axi_b_payload_resp;
  assign io_s_axi_ar_ready = core_io_s_axi_ar_ready;
  assign io_s_axi_r_valid = core_io_s_axi_r_valid;
  assign io_s_axi_r_payload_data = core_io_s_axi_r_payload_data;
  assign io_s_axi_r_payload_resp = core_io_s_axi_r_payload_resp;
  assign io_m_stream_valid = core_io_m_stream_valid;
  assign io_m_stream_payload = {core_io_m_stream_payload_SLICE3,{core_io_m_stream_payload_SLICE2,{core_io_m_stream_payload_SLICE1,core_io_m_stream_payload_SLICE0}}};

endmodule

module Axi4LiteToStream (
  input  wire          io_s_axi_aw_valid,
  output wire          io_s_axi_aw_ready,
  input  wire [11:0]   io_s_axi_aw_payload_addr,
  input  wire [2:0]    io_s_axi_aw_payload_prot,
  input  wire          io_s_axi_w_valid,
  output wire          io_s_axi_w_ready,
  input  wire [63:0]   io_s_axi_w_payload_data,
  input  wire [7:0]    io_s_axi_w_payload_strb,
  output wire          io_s_axi_b_valid,
  input  wire          io_s_axi_b_ready,
  output wire [1:0]    io_s_axi_b_payload_resp,
  input  wire          io_s_axi_ar_valid,
  output wire          io_s_axi_ar_ready,
  input  wire [11:0]   io_s_axi_ar_payload_addr,
  input  wire [2:0]    io_s_axi_ar_payload_prot,
  output wire          io_s_axi_r_valid,
  input  wire          io_s_axi_r_ready,
  output wire [63:0]   io_s_axi_r_payload_data,
  output wire [1:0]    io_s_axi_r_payload_resp,
  output wire          io_m_stream_valid,
  input  wire          io_m_stream_ready,
  output wire [31:0]   io_m_stream_payload_SLICE0,
  output wire [31:0]   io_m_stream_payload_SLICE1,
  output wire [31:0]   io_m_stream_payload_SLICE2,
  output wire [31:0]   io_m_stream_payload_SLICE3,
  output wire          io_busy,
  input  wire          clk,
  input  wire          resetn
);

  wire                fifo_io_push_ready;
  wire                fifo_io_pop_valid;
  wire       [31:0]   fifo_io_pop_payload_SLICE0;
  wire       [31:0]   fifo_io_pop_payload_SLICE1;
  wire       [31:0]   fifo_io_pop_payload_SLICE2;
  wire       [31:0]   fifo_io_pop_payload_SLICE3;
  wire       [2:0]    fifo_io_occupancy;
  wire       [2:0]    fifo_io_availability;
  reg        [31:0]   shadowReg_SLICE0;
  reg        [31:0]   shadowReg_SLICE1;
  reg        [31:0]   shadowReg_SLICE2;
  reg        [31:0]   shadowReg_SLICE3;
  wire                busCtrl_readErrorFlag;
  wire                busCtrl_writeErrorFlag;
  wire                busCtrl_readHaltRequest;
  wire                busCtrl_writeHaltRequest;
  wire                busCtrl_writeJoinEvent_valid;
  wire                busCtrl_writeJoinEvent_ready;
  wire                busCtrl_writeOccur;
  reg        [1:0]    busCtrl_writeRsp_resp;
  wire                busCtrl_writeJoinEvent_translated_valid;
  wire                busCtrl_writeJoinEvent_translated_ready;
  wire       [1:0]    busCtrl_writeJoinEvent_translated_payload_resp;
  wire                _zz_busCtrl_writeJoinEvent_translated_ready;
  wire                busCtrl_writeJoinEvent_translated_haltWhen_valid;
  wire                busCtrl_writeJoinEvent_translated_haltWhen_ready;
  wire       [1:0]    busCtrl_writeJoinEvent_translated_haltWhen_payload_resp;
  wire                busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_valid;
  wire                busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_ready;
  wire       [1:0]    busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_payload_resp;
  reg                 busCtrl_writeJoinEvent_translated_haltWhen_rValid;
  wire                busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_fire;
  reg        [1:0]    busCtrl_writeJoinEvent_translated_haltWhen_rData_resp;
  wire                busCtrl_readDataStage_valid;
  wire                busCtrl_readDataStage_ready;
  wire       [11:0]   busCtrl_readDataStage_payload_addr;
  wire       [2:0]    busCtrl_readDataStage_payload_prot;
  reg                 io_s_axi_ar_rValid;
  wire                busCtrl_readDataStage_fire;
  reg        [11:0]   io_s_axi_ar_rData_addr;
  reg        [2:0]    io_s_axi_ar_rData_prot;
  reg        [63:0]   busCtrl_readRsp_data;
  reg        [1:0]    busCtrl_readRsp_resp;
  wire                _zz_busCtrl_readDataStage_ready;
  wire                busCtrl_readDataStage_haltWhen_valid;
  wire                busCtrl_readDataStage_haltWhen_ready;
  wire       [11:0]   busCtrl_readDataStage_haltWhen_payload_addr;
  wire       [2:0]    busCtrl_readDataStage_haltWhen_payload_prot;
  wire                busCtrl_readDataStage_haltWhen_translated_valid;
  wire                busCtrl_readDataStage_haltWhen_translated_ready;
  wire       [63:0]   busCtrl_readDataStage_haltWhen_translated_payload_data;
  wire       [1:0]    busCtrl_readDataStage_haltWhen_translated_payload_resp;
  wire       [11:0]   busCtrl_readAddressMasked;
  wire       [11:0]   busCtrl_writeAddressMasked;
  wire                busCtrl_readOccur;
  reg                 fireTrigger;

  StreamFifo fifo (
    .io_push_valid          (fireTrigger                     ), //i
    .io_push_ready          (fifo_io_push_ready              ), //o
    .io_push_payload_SLICE0 (shadowReg_SLICE0[31:0]          ), //i
    .io_push_payload_SLICE1 (shadowReg_SLICE1[31:0]          ), //i
    .io_push_payload_SLICE2 (shadowReg_SLICE2[31:0]          ), //i
    .io_push_payload_SLICE3 (shadowReg_SLICE3[31:0]          ), //i
    .io_pop_valid           (fifo_io_pop_valid               ), //o
    .io_pop_ready           (io_m_stream_ready               ), //i
    .io_pop_payload_SLICE0  (fifo_io_pop_payload_SLICE0[31:0]), //o
    .io_pop_payload_SLICE1  (fifo_io_pop_payload_SLICE1[31:0]), //o
    .io_pop_payload_SLICE2  (fifo_io_pop_payload_SLICE2[31:0]), //o
    .io_pop_payload_SLICE3  (fifo_io_pop_payload_SLICE3[31:0]), //o
    .io_flush               (1'b0                            ), //i
    .io_occupancy           (fifo_io_occupancy[2:0]          ), //o
    .io_availability        (fifo_io_availability[2:0]       ), //o
    .clk                    (clk                             ), //i
    .resetn                 (resetn                          )  //i
  );
  assign busCtrl_readErrorFlag = 1'b0;
  assign busCtrl_writeErrorFlag = 1'b0;
  assign busCtrl_readHaltRequest = 1'b0;
  assign busCtrl_writeHaltRequest = 1'b0;
  assign busCtrl_writeOccur = (busCtrl_writeJoinEvent_valid && busCtrl_writeJoinEvent_ready);
  assign busCtrl_writeJoinEvent_valid = (io_s_axi_aw_valid && io_s_axi_w_valid);
  assign io_s_axi_aw_ready = busCtrl_writeOccur;
  assign io_s_axi_w_ready = busCtrl_writeOccur;
  assign busCtrl_writeJoinEvent_translated_valid = busCtrl_writeJoinEvent_valid;
  assign busCtrl_writeJoinEvent_ready = busCtrl_writeJoinEvent_translated_ready;
  assign busCtrl_writeJoinEvent_translated_payload_resp = busCtrl_writeRsp_resp;
  assign _zz_busCtrl_writeJoinEvent_translated_ready = (! busCtrl_writeHaltRequest);
  assign busCtrl_writeJoinEvent_translated_haltWhen_valid = (busCtrl_writeJoinEvent_translated_valid && _zz_busCtrl_writeJoinEvent_translated_ready);
  assign busCtrl_writeJoinEvent_translated_ready = (busCtrl_writeJoinEvent_translated_haltWhen_ready && _zz_busCtrl_writeJoinEvent_translated_ready);
  assign busCtrl_writeJoinEvent_translated_haltWhen_payload_resp = busCtrl_writeJoinEvent_translated_payload_resp;
  assign busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_fire = (busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_valid && busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_ready);
  assign busCtrl_writeJoinEvent_translated_haltWhen_ready = (! busCtrl_writeJoinEvent_translated_haltWhen_rValid);
  assign busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_valid = busCtrl_writeJoinEvent_translated_haltWhen_rValid;
  assign busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_payload_resp = busCtrl_writeJoinEvent_translated_haltWhen_rData_resp;
  assign io_s_axi_b_valid = busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_valid;
  assign busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_ready = io_s_axi_b_ready;
  assign io_s_axi_b_payload_resp = busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_payload_resp;
  assign busCtrl_readDataStage_fire = (busCtrl_readDataStage_valid && busCtrl_readDataStage_ready);
  assign io_s_axi_ar_ready = (! io_s_axi_ar_rValid);
  assign busCtrl_readDataStage_valid = io_s_axi_ar_rValid;
  assign busCtrl_readDataStage_payload_addr = io_s_axi_ar_rData_addr;
  assign busCtrl_readDataStage_payload_prot = io_s_axi_ar_rData_prot;
  assign _zz_busCtrl_readDataStage_ready = (! busCtrl_readHaltRequest);
  assign busCtrl_readDataStage_haltWhen_valid = (busCtrl_readDataStage_valid && _zz_busCtrl_readDataStage_ready);
  assign busCtrl_readDataStage_ready = (busCtrl_readDataStage_haltWhen_ready && _zz_busCtrl_readDataStage_ready);
  assign busCtrl_readDataStage_haltWhen_payload_addr = busCtrl_readDataStage_payload_addr;
  assign busCtrl_readDataStage_haltWhen_payload_prot = busCtrl_readDataStage_payload_prot;
  assign busCtrl_readDataStage_haltWhen_translated_valid = busCtrl_readDataStage_haltWhen_valid;
  assign busCtrl_readDataStage_haltWhen_ready = busCtrl_readDataStage_haltWhen_translated_ready;
  assign busCtrl_readDataStage_haltWhen_translated_payload_data = busCtrl_readRsp_data;
  assign busCtrl_readDataStage_haltWhen_translated_payload_resp = busCtrl_readRsp_resp;
  assign io_s_axi_r_valid = busCtrl_readDataStage_haltWhen_translated_valid;
  assign busCtrl_readDataStage_haltWhen_translated_ready = io_s_axi_r_ready;
  assign io_s_axi_r_payload_data = busCtrl_readDataStage_haltWhen_translated_payload_data;
  assign io_s_axi_r_payload_resp = busCtrl_readDataStage_haltWhen_translated_payload_resp;
  always @(*) begin
    if(busCtrl_writeErrorFlag) begin
      busCtrl_writeRsp_resp = 2'b10;
    end else begin
      busCtrl_writeRsp_resp = 2'b00;
    end
  end

  always @(*) begin
    if(busCtrl_readErrorFlag) begin
      busCtrl_readRsp_resp = 2'b10;
    end else begin
      busCtrl_readRsp_resp = 2'b00;
    end
  end

  always @(*) begin
    busCtrl_readRsp_data = 64'h0;
    case(busCtrl_readAddressMasked)
      12'h010 : begin
        busCtrl_readRsp_data[0 : 0] = (! fifo_io_push_ready);
      end
      default : begin
      end
    endcase
  end

  assign busCtrl_readAddressMasked = (busCtrl_readDataStage_payload_addr & (~ 12'h007));
  assign busCtrl_writeAddressMasked = (io_s_axi_aw_payload_addr & (~ 12'h007));
  assign busCtrl_readOccur = (io_s_axi_r_valid && io_s_axi_r_ready);
  always @(*) begin
    fireTrigger = 1'b0;
    case(busCtrl_writeAddressMasked)
      12'h010 : begin
        if(busCtrl_writeOccur) begin
          fireTrigger = 1'b1;
        end
      end
      default : begin
      end
    endcase
  end

  assign io_m_stream_valid = fifo_io_pop_valid;
  assign io_m_stream_payload_SLICE0 = fifo_io_pop_payload_SLICE0;
  assign io_m_stream_payload_SLICE1 = fifo_io_pop_payload_SLICE1;
  assign io_m_stream_payload_SLICE2 = fifo_io_pop_payload_SLICE2;
  assign io_m_stream_payload_SLICE3 = fifo_io_pop_payload_SLICE3;
  assign io_busy = (! fifo_io_push_ready);
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      shadowReg_SLICE0 <= 32'h0;
      shadowReg_SLICE1 <= 32'h0;
      shadowReg_SLICE2 <= 32'h0;
      shadowReg_SLICE3 <= 32'h0;
      busCtrl_writeJoinEvent_translated_haltWhen_rValid <= 1'b0;
      io_s_axi_ar_rValid <= 1'b0;
    end else begin
      if(busCtrl_writeJoinEvent_translated_haltWhen_valid) begin
        busCtrl_writeJoinEvent_translated_haltWhen_rValid <= 1'b1;
      end
      if(busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_fire) begin
        busCtrl_writeJoinEvent_translated_haltWhen_rValid <= 1'b0;
      end
      if(io_s_axi_ar_valid) begin
        io_s_axi_ar_rValid <= 1'b1;
      end
      if(busCtrl_readDataStage_fire) begin
        io_s_axi_ar_rValid <= 1'b0;
      end
      case(busCtrl_writeAddressMasked)
        12'h0 : begin
          if(busCtrl_writeOccur) begin
            shadowReg_SLICE0 <= io_s_axi_w_payload_data[31 : 0];
            shadowReg_SLICE1 <= io_s_axi_w_payload_data[63 : 32];
          end
        end
        12'h008 : begin
          if(busCtrl_writeOccur) begin
            shadowReg_SLICE2 <= io_s_axi_w_payload_data[31 : 0];
            shadowReg_SLICE3 <= io_s_axi_w_payload_data[63 : 32];
          end
        end
        default : begin
        end
      endcase
    end
  end

  always @(posedge clk) begin
    if(busCtrl_writeJoinEvent_translated_haltWhen_ready) begin
      busCtrl_writeJoinEvent_translated_haltWhen_rData_resp <= busCtrl_writeJoinEvent_translated_haltWhen_payload_resp;
    end
    if(io_s_axi_ar_ready) begin
      io_s_axi_ar_rData_addr <= io_s_axi_ar_payload_addr;
      io_s_axi_ar_rData_prot <= io_s_axi_ar_payload_prot;
    end
  end


endmodule

module StreamFifo (
  input  wire          io_push_valid,
  output wire          io_push_ready,
  input  wire [31:0]   io_push_payload_SLICE0,
  input  wire [31:0]   io_push_payload_SLICE1,
  input  wire [31:0]   io_push_payload_SLICE2,
  input  wire [31:0]   io_push_payload_SLICE3,
  output wire          io_pop_valid,
  input  wire          io_pop_ready,
  output wire [31:0]   io_pop_payload_SLICE0,
  output wire [31:0]   io_pop_payload_SLICE1,
  output wire [31:0]   io_pop_payload_SLICE2,
  output wire [31:0]   io_pop_payload_SLICE3,
  input  wire          io_flush,
  output wire [2:0]    io_occupancy,
  output wire [2:0]    io_availability,
  input  wire          clk,
  input  wire          resetn
);

  reg        [127:0]  logic_ram_spinal_port1;
  wire       [127:0]  _zz_logic_ram_port;
  reg                 _zz_1;
  wire                logic_ptr_doPush;
  wire                logic_ptr_doPop;
  wire                logic_ptr_full;
  wire                logic_ptr_empty;
  reg        [2:0]    logic_ptr_push;
  reg        [2:0]    logic_ptr_pop;
  wire       [2:0]    logic_ptr_occupancy;
  wire       [2:0]    logic_ptr_popOnIo;
  wire                when_Stream_l1273;
  reg                 logic_ptr_wentUp;
  wire                io_push_fire;
  wire                logic_push_onRam_write_valid;
  wire       [1:0]    logic_push_onRam_write_payload_address;
  wire       [31:0]   logic_push_onRam_write_payload_data_SLICE0;
  wire       [31:0]   logic_push_onRam_write_payload_data_SLICE1;
  wire       [31:0]   logic_push_onRam_write_payload_data_SLICE2;
  wire       [31:0]   logic_push_onRam_write_payload_data_SLICE3;
  wire                logic_pop_addressGen_valid;
  reg                 logic_pop_addressGen_ready;
  wire       [1:0]    logic_pop_addressGen_payload;
  wire                logic_pop_addressGen_fire;
  wire                logic_pop_sync_readArbitation_valid;
  wire                logic_pop_sync_readArbitation_ready;
  wire       [1:0]    logic_pop_sync_readArbitation_payload;
  reg                 logic_pop_addressGen_rValid;
  reg        [1:0]    logic_pop_addressGen_rData;
  wire                when_Stream_l393;
  wire                logic_pop_sync_readPort_cmd_valid;
  wire       [1:0]    logic_pop_sync_readPort_cmd_payload;
  wire       [31:0]   logic_pop_sync_readPort_rsp_SLICE0;
  wire       [31:0]   logic_pop_sync_readPort_rsp_SLICE1;
  wire       [31:0]   logic_pop_sync_readPort_rsp_SLICE2;
  wire       [31:0]   logic_pop_sync_readPort_rsp_SLICE3;
  wire       [127:0]  _zz_logic_pop_sync_readPort_rsp_SLICE0;
  wire                logic_pop_addressGen_toFlowFire_valid;
  wire       [1:0]    logic_pop_addressGen_toFlowFire_payload;
  wire                logic_pop_sync_readArbitation_translated_valid;
  wire                logic_pop_sync_readArbitation_translated_ready;
  wire       [31:0]   logic_pop_sync_readArbitation_translated_payload_SLICE0;
  wire       [31:0]   logic_pop_sync_readArbitation_translated_payload_SLICE1;
  wire       [31:0]   logic_pop_sync_readArbitation_translated_payload_SLICE2;
  wire       [31:0]   logic_pop_sync_readArbitation_translated_payload_SLICE3;
  wire                logic_pop_sync_readArbitation_fire;
  reg        [2:0]    logic_pop_sync_popReg;
  reg [127:0] logic_ram [0:3];

  assign _zz_logic_ram_port = {logic_push_onRam_write_payload_data_SLICE3,{logic_push_onRam_write_payload_data_SLICE2,{logic_push_onRam_write_payload_data_SLICE1,logic_push_onRam_write_payload_data_SLICE0}}};
  always @(posedge clk) begin
    if(_zz_1) begin
      logic_ram[logic_push_onRam_write_payload_address] <= _zz_logic_ram_port;
    end
  end

  always @(posedge clk) begin
    if(logic_pop_sync_readPort_cmd_valid) begin
      logic_ram_spinal_port1 <= logic_ram[logic_pop_sync_readPort_cmd_payload];
    end
  end

  always @(*) begin
    _zz_1 = 1'b0;
    if(logic_push_onRam_write_valid) begin
      _zz_1 = 1'b1;
    end
  end

  assign when_Stream_l1273 = (logic_ptr_doPush != logic_ptr_doPop);
  assign logic_ptr_full = (((logic_ptr_push ^ logic_ptr_popOnIo) ^ 3'b100) == 3'b000);
  assign logic_ptr_empty = (logic_ptr_push == logic_ptr_pop);
  assign logic_ptr_occupancy = (logic_ptr_push - logic_ptr_popOnIo);
  assign io_push_ready = (! logic_ptr_full);
  assign io_push_fire = (io_push_valid && io_push_ready);
  assign logic_ptr_doPush = io_push_fire;
  assign logic_push_onRam_write_valid = io_push_fire;
  assign logic_push_onRam_write_payload_address = logic_ptr_push[1:0];
  assign logic_push_onRam_write_payload_data_SLICE0 = io_push_payload_SLICE0;
  assign logic_push_onRam_write_payload_data_SLICE1 = io_push_payload_SLICE1;
  assign logic_push_onRam_write_payload_data_SLICE2 = io_push_payload_SLICE2;
  assign logic_push_onRam_write_payload_data_SLICE3 = io_push_payload_SLICE3;
  assign logic_pop_addressGen_valid = (! logic_ptr_empty);
  assign logic_pop_addressGen_payload = logic_ptr_pop[1:0];
  assign logic_pop_addressGen_fire = (logic_pop_addressGen_valid && logic_pop_addressGen_ready);
  assign logic_ptr_doPop = logic_pop_addressGen_fire;
  always @(*) begin
    logic_pop_addressGen_ready = logic_pop_sync_readArbitation_ready;
    if(when_Stream_l393) begin
      logic_pop_addressGen_ready = 1'b1;
    end
  end

  assign when_Stream_l393 = (! logic_pop_sync_readArbitation_valid);
  assign logic_pop_sync_readArbitation_valid = logic_pop_addressGen_rValid;
  assign logic_pop_sync_readArbitation_payload = logic_pop_addressGen_rData;
  assign _zz_logic_pop_sync_readPort_rsp_SLICE0 = logic_ram_spinal_port1;
  assign logic_pop_sync_readPort_rsp_SLICE0 = _zz_logic_pop_sync_readPort_rsp_SLICE0[31 : 0];
  assign logic_pop_sync_readPort_rsp_SLICE1 = _zz_logic_pop_sync_readPort_rsp_SLICE0[63 : 32];
  assign logic_pop_sync_readPort_rsp_SLICE2 = _zz_logic_pop_sync_readPort_rsp_SLICE0[95 : 64];
  assign logic_pop_sync_readPort_rsp_SLICE3 = _zz_logic_pop_sync_readPort_rsp_SLICE0[127 : 96];
  assign logic_pop_addressGen_toFlowFire_valid = logic_pop_addressGen_fire;
  assign logic_pop_addressGen_toFlowFire_payload = logic_pop_addressGen_payload;
  assign logic_pop_sync_readPort_cmd_valid = logic_pop_addressGen_toFlowFire_valid;
  assign logic_pop_sync_readPort_cmd_payload = logic_pop_addressGen_toFlowFire_payload;
  assign logic_pop_sync_readArbitation_translated_valid = logic_pop_sync_readArbitation_valid;
  assign logic_pop_sync_readArbitation_ready = logic_pop_sync_readArbitation_translated_ready;
  assign logic_pop_sync_readArbitation_translated_payload_SLICE0 = logic_pop_sync_readPort_rsp_SLICE0;
  assign logic_pop_sync_readArbitation_translated_payload_SLICE1 = logic_pop_sync_readPort_rsp_SLICE1;
  assign logic_pop_sync_readArbitation_translated_payload_SLICE2 = logic_pop_sync_readPort_rsp_SLICE2;
  assign logic_pop_sync_readArbitation_translated_payload_SLICE3 = logic_pop_sync_readPort_rsp_SLICE3;
  assign io_pop_valid = logic_pop_sync_readArbitation_translated_valid;
  assign logic_pop_sync_readArbitation_translated_ready = io_pop_ready;
  assign io_pop_payload_SLICE0 = logic_pop_sync_readArbitation_translated_payload_SLICE0;
  assign io_pop_payload_SLICE1 = logic_pop_sync_readArbitation_translated_payload_SLICE1;
  assign io_pop_payload_SLICE2 = logic_pop_sync_readArbitation_translated_payload_SLICE2;
  assign io_pop_payload_SLICE3 = logic_pop_sync_readArbitation_translated_payload_SLICE3;
  assign logic_pop_sync_readArbitation_fire = (logic_pop_sync_readArbitation_valid && logic_pop_sync_readArbitation_ready);
  assign logic_ptr_popOnIo = logic_pop_sync_popReg;
  assign io_occupancy = logic_ptr_occupancy;
  assign io_availability = (3'b100 - logic_ptr_occupancy);
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      logic_ptr_push <= 3'b000;
      logic_ptr_pop <= 3'b000;
      logic_ptr_wentUp <= 1'b0;
      logic_pop_addressGen_rValid <= 1'b0;
      logic_pop_sync_popReg <= 3'b000;
    end else begin
      if(when_Stream_l1273) begin
        logic_ptr_wentUp <= logic_ptr_doPush;
      end
      if(io_flush) begin
        logic_ptr_wentUp <= 1'b0;
      end
      if(logic_ptr_doPush) begin
        logic_ptr_push <= (logic_ptr_push + 3'b001);
      end
      if(logic_ptr_doPop) begin
        logic_ptr_pop <= (logic_ptr_pop + 3'b001);
      end
      if(io_flush) begin
        logic_ptr_push <= 3'b000;
        logic_ptr_pop <= 3'b000;
      end
      if(logic_pop_addressGen_ready) begin
        logic_pop_addressGen_rValid <= logic_pop_addressGen_valid;
      end
      if(io_flush) begin
        logic_pop_addressGen_rValid <= 1'b0;
      end
      if(logic_pop_sync_readArbitation_fire) begin
        logic_pop_sync_popReg <= logic_ptr_pop;
      end
      if(io_flush) begin
        logic_pop_sync_popReg <= 3'b000;
      end
    end
  end

  always @(posedge clk) begin
    if(logic_pop_addressGen_ready) begin
      logic_pop_addressGen_rData <= logic_pop_addressGen_payload;
    end
  end


endmodule

// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : MatrixCacheController
// Git hash  : a9cbd96b00c1311cf49010ec546f8110a9a55e92

`timescale 1ns/1ps

module MatrixCacheController (
  input  wire          io_axi_aw_valid,
  output wire          io_axi_aw_ready,
  input  wire [7:0]    io_axi_aw_payload_addr,
  input  wire [2:0]    io_axi_aw_payload_prot,
  input  wire          io_axi_w_valid,
  output wire          io_axi_w_ready,
  input  wire [31:0]   io_axi_w_payload_data,
  input  wire [3:0]    io_axi_w_payload_strb,
  output wire          io_axi_b_valid,
  input  wire          io_axi_b_ready,
  output wire [1:0]    io_axi_b_payload_resp,
  input  wire          io_axi_ar_valid,
  output wire          io_axi_ar_ready,
  input  wire [7:0]    io_axi_ar_payload_addr,
  input  wire [2:0]    io_axi_ar_payload_prot,
  output wire          io_axi_r_valid,
  input  wire          io_axi_r_ready,
  output wire [31:0]   io_axi_r_payload_data,
  output wire [1:0]    io_axi_r_payload_resp,
  output wire          io_globalIntr,
  input  wire          io_readA_clk,
  input  wire          io_readA_rst,
  input  wire          io_readA_Valid,
  input  wire [19:0]   io_readA_Address,
  output wire [255:0]  io_readA_Data,
  input  wire          io_writeA_clk,
  input  wire          io_writeA_rst,
  input  wire [31:0]   io_writeA_Wen,
  input  wire          io_writeA_Valid,
  input  wire [19:0]   io_writeA_Address,
  input  wire [255:0]  io_writeA_Data,
  input  wire          io_switchA,
  input  wire          io_dmaDoneA,
  input  wire          io_readB_clk,
  input  wire          io_readB_rst,
  input  wire          io_readB_Valid,
  input  wire [19:0]   io_readB_Address,
  output wire [255:0]  io_readB_Data,
  input  wire          io_writeB_clk,
  input  wire          io_writeB_rst,
  input  wire [31:0]   io_writeB_Wen,
  input  wire          io_writeB_Valid,
  input  wire [19:0]   io_writeB_Address,
  input  wire [255:0]  io_writeB_Data,
  input  wire          io_switchB,
  input  wire          io_dmaDoneB,
  input  wire          io_readC_clk,
  input  wire          io_readC_rst,
  input  wire          io_readC_Valid,
  input  wire [19:0]   io_readC_Address,
  output wire [255:0]  io_readC_Data,
  input  wire          io_writeC_clk,
  input  wire          io_writeC_rst,
  input  wire [31:0]   io_writeC_Wen,
  input  wire          io_writeC_Valid,
  input  wire [19:0]   io_writeC_Address,
  input  wire [255:0]  io_writeC_Data,
  input  wire          io_switchC,
  input  wire          io_dmaDoneC,
  output wire          io_memA_read_clk,
  output wire          io_memA_read_rst,
  output wire          io_memA_read_Valid,
  output wire [20:0]   io_memA_read_Address,
  input  wire [255:0]  io_memA_read_Data,
  output wire          io_memA_write_clk,
  output wire          io_memA_write_rst,
  output wire [31:0]   io_memA_write_Wen,
  output wire          io_memA_write_Valid,
  output wire [20:0]   io_memA_write_Address,
  output wire [255:0]  io_memA_write_Data,
  output wire          io_memB_read_clk,
  output wire          io_memB_read_rst,
  output wire          io_memB_read_Valid,
  output wire [20:0]   io_memB_read_Address,
  input  wire [255:0]  io_memB_read_Data,
  output wire          io_memB_write_clk,
  output wire          io_memB_write_rst,
  output wire [31:0]   io_memB_write_Wen,
  output wire          io_memB_write_Valid,
  output wire [20:0]   io_memB_write_Address,
  output wire [255:0]  io_memB_write_Data,
  output wire          io_memC_read_clk,
  output wire          io_memC_read_rst,
  output wire          io_memC_read_Valid,
  output wire [20:0]   io_memC_read_Address,
  input  wire [255:0]  io_memC_read_Data,
  output wire          io_memC_write_clk,
  output wire          io_memC_write_rst,
  output wire [31:0]   io_memC_write_Wen,
  output wire          io_memC_write_Valid,
  output wire [20:0]   io_memC_write_Address,
  output wire [255:0]  io_memC_write_Data,
  input  wire          clk,
  input  wire          resetn
);

  reg                 cacheC_io_intrClear;
  wire       [255:0]  cacheA_io_read_Data;
  wire                cacheA_io_status;
  wire                cacheA_io_empty;
  wire                cacheA_io_memRead_clk;
  wire                cacheA_io_memRead_rst;
  wire                cacheA_io_memRead_Valid;
  wire       [20:0]   cacheA_io_memRead_Address;
  wire                cacheA_io_memWrite_clk;
  wire                cacheA_io_memWrite_rst;
  wire       [31:0]   cacheA_io_memWrite_Wen;
  wire                cacheA_io_memWrite_Valid;
  wire       [20:0]   cacheA_io_memWrite_Address;
  wire       [255:0]  cacheA_io_memWrite_Data;
  wire       [255:0]  cacheB_io_read_Data;
  wire                cacheB_io_status;
  wire                cacheB_io_empty;
  wire                cacheB_io_memRead_clk;
  wire                cacheB_io_memRead_rst;
  wire                cacheB_io_memRead_Valid;
  wire       [20:0]   cacheB_io_memRead_Address;
  wire                cacheB_io_memWrite_clk;
  wire                cacheB_io_memWrite_rst;
  wire       [31:0]   cacheB_io_memWrite_Wen;
  wire                cacheB_io_memWrite_Valid;
  wire       [20:0]   cacheB_io_memWrite_Address;
  wire       [255:0]  cacheB_io_memWrite_Data;
  wire       [255:0]  cacheC_io_read_Data;
  wire                cacheC_io_status;
  wire                cacheC_io_intr;
  wire                cacheC_io_full;
  wire                cacheC_io_memRead_clk;
  wire                cacheC_io_memRead_rst;
  wire                cacheC_io_memRead_Valid;
  wire       [20:0]   cacheC_io_memRead_Address;
  wire                cacheC_io_memWrite_clk;
  wire                cacheC_io_memWrite_rst;
  wire       [31:0]   cacheC_io_memWrite_Wen;
  wire                cacheC_io_memWrite_Valid;
  wire       [20:0]   cacheC_io_memWrite_Address;
  wire       [255:0]  cacheC_io_memWrite_Data;
  wire       [0:0]    _zz_io_intrClear;
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
  wire       [7:0]    busCtrl_readDataStage_payload_addr;
  wire       [2:0]    busCtrl_readDataStage_payload_prot;
  reg                 io_axi_ar_rValid;
  wire                busCtrl_readDataStage_fire;
  reg        [7:0]    io_axi_ar_rData_addr;
  reg        [2:0]    io_axi_ar_rData_prot;
  reg        [31:0]   busCtrl_readRsp_data;
  reg        [1:0]    busCtrl_readRsp_resp;
  wire                _zz_busCtrl_readDataStage_ready;
  wire                busCtrl_readDataStage_haltWhen_valid;
  wire                busCtrl_readDataStage_haltWhen_ready;
  wire       [7:0]    busCtrl_readDataStage_haltWhen_payload_addr;
  wire       [2:0]    busCtrl_readDataStage_haltWhen_payload_prot;
  wire                busCtrl_readDataStage_haltWhen_translated_valid;
  wire                busCtrl_readDataStage_haltWhen_translated_ready;
  wire       [31:0]   busCtrl_readDataStage_haltWhen_translated_payload_data;
  wire       [1:0]    busCtrl_readDataStage_haltWhen_translated_payload_resp;
  wire       [7:0]    busCtrl_readAddressMasked;
  wire       [7:0]    busCtrl_writeAddressMasked;
  wire                busCtrl_readOccur;
  reg        [15:0]   lifeCfgRegA;
  reg        [15:0]   lifeCfgRegB;
  reg                 when_BusSlaveFactory_l377;
  wire                when_BusSlaveFactory_l379;

  assign _zz_io_intrClear = 1'b1;
  InputMatrixCacheController cacheA (
    .io_read_clk         (io_readA_clk                    ), //i
    .io_read_rst         (io_readA_rst                    ), //i
    .io_read_Valid       (io_readA_Valid                  ), //i
    .io_read_Address     (io_readA_Address[19:0]          ), //i
    .io_read_Data        (cacheA_io_read_Data[255:0]      ), //o
    .io_write_clk        (io_writeA_clk                   ), //i
    .io_write_rst        (io_writeA_rst                   ), //i
    .io_write_Wen        (io_writeA_Wen[31:0]             ), //i
    .io_write_Valid      (io_writeA_Valid                 ), //i
    .io_write_Address    (io_writeA_Address[19:0]         ), //i
    .io_write_Data       (io_writeA_Data[255:0]           ), //i
    .io_switch           (io_switchA                      ), //i
    .io_dmaIntr          (io_dmaDoneA                     ), //i
    .io_lifeCfg          (lifeCfgRegA[15:0]               ), //i
    .io_status           (cacheA_io_status                ), //o
    .io_empty            (cacheA_io_empty                 ), //o
    .io_memRead_clk      (cacheA_io_memRead_clk           ), //o
    .io_memRead_rst      (cacheA_io_memRead_rst           ), //o
    .io_memRead_Valid    (cacheA_io_memRead_Valid         ), //o
    .io_memRead_Address  (cacheA_io_memRead_Address[20:0] ), //o
    .io_memRead_Data     (io_memA_read_Data[255:0]        ), //i
    .io_memWrite_clk     (cacheA_io_memWrite_clk          ), //o
    .io_memWrite_rst     (cacheA_io_memWrite_rst          ), //o
    .io_memWrite_Wen     (cacheA_io_memWrite_Wen[31:0]    ), //o
    .io_memWrite_Valid   (cacheA_io_memWrite_Valid        ), //o
    .io_memWrite_Address (cacheA_io_memWrite_Address[20:0]), //o
    .io_memWrite_Data    (cacheA_io_memWrite_Data[255:0]  ), //o
    .clk                 (clk                             ), //i
    .resetn              (resetn                          )  //i
  );
  InputMatrixCacheController cacheB (
    .io_read_clk         (io_readB_clk                    ), //i
    .io_read_rst         (io_readB_rst                    ), //i
    .io_read_Valid       (io_readB_Valid                  ), //i
    .io_read_Address     (io_readB_Address[19:0]          ), //i
    .io_read_Data        (cacheB_io_read_Data[255:0]      ), //o
    .io_write_clk        (io_writeB_clk                   ), //i
    .io_write_rst        (io_writeB_rst                   ), //i
    .io_write_Wen        (io_writeB_Wen[31:0]             ), //i
    .io_write_Valid      (io_writeB_Valid                 ), //i
    .io_write_Address    (io_writeB_Address[19:0]         ), //i
    .io_write_Data       (io_writeB_Data[255:0]           ), //i
    .io_switch           (io_switchB                      ), //i
    .io_dmaIntr          (io_dmaDoneB                     ), //i
    .io_lifeCfg          (lifeCfgRegB[15:0]               ), //i
    .io_status           (cacheB_io_status                ), //o
    .io_empty            (cacheB_io_empty                 ), //o
    .io_memRead_clk      (cacheB_io_memRead_clk           ), //o
    .io_memRead_rst      (cacheB_io_memRead_rst           ), //o
    .io_memRead_Valid    (cacheB_io_memRead_Valid         ), //o
    .io_memRead_Address  (cacheB_io_memRead_Address[20:0] ), //o
    .io_memRead_Data     (io_memB_read_Data[255:0]        ), //i
    .io_memWrite_clk     (cacheB_io_memWrite_clk          ), //o
    .io_memWrite_rst     (cacheB_io_memWrite_rst          ), //o
    .io_memWrite_Wen     (cacheB_io_memWrite_Wen[31:0]    ), //o
    .io_memWrite_Valid   (cacheB_io_memWrite_Valid        ), //o
    .io_memWrite_Address (cacheB_io_memWrite_Address[20:0]), //o
    .io_memWrite_Data    (cacheB_io_memWrite_Data[255:0]  ), //o
    .clk                 (clk                             ), //i
    .resetn              (resetn                          )  //i
  );
  OutputMatrixCacheController cacheC (
    .io_read_clk         (io_readC_clk                    ), //i
    .io_read_rst         (io_readC_rst                    ), //i
    .io_read_Valid       (io_readC_Valid                  ), //i
    .io_read_Address     (io_readC_Address[19:0]          ), //i
    .io_read_Data        (cacheC_io_read_Data[255:0]      ), //o
    .io_write_clk        (io_writeC_clk                   ), //i
    .io_write_rst        (io_writeC_rst                   ), //i
    .io_write_Wen        (io_writeC_Wen[31:0]             ), //i
    .io_write_Valid      (io_writeC_Valid                 ), //i
    .io_write_Address    (io_writeC_Address[19:0]         ), //i
    .io_write_Data       (io_writeC_Data[255:0]           ), //i
    .io_switch           (io_switchC                      ), //i
    .io_dmaIntr          (io_dmaDoneC                     ), //i
    .io_status           (cacheC_io_status                ), //o
    .io_intr             (cacheC_io_intr                  ), //o
    .io_intrClear        (cacheC_io_intrClear             ), //i
    .io_full             (cacheC_io_full                  ), //o
    .io_memRead_clk      (cacheC_io_memRead_clk           ), //o
    .io_memRead_rst      (cacheC_io_memRead_rst           ), //o
    .io_memRead_Valid    (cacheC_io_memRead_Valid         ), //o
    .io_memRead_Address  (cacheC_io_memRead_Address[20:0] ), //o
    .io_memRead_Data     (io_memC_read_Data[255:0]        ), //i
    .io_memWrite_clk     (cacheC_io_memWrite_clk          ), //o
    .io_memWrite_rst     (cacheC_io_memWrite_rst          ), //o
    .io_memWrite_Wen     (cacheC_io_memWrite_Wen[31:0]    ), //o
    .io_memWrite_Valid   (cacheC_io_memWrite_Valid        ), //o
    .io_memWrite_Address (cacheC_io_memWrite_Address[20:0]), //o
    .io_memWrite_Data    (cacheC_io_memWrite_Data[255:0]  ), //o
    .clk                 (clk                             ), //i
    .resetn              (resetn                          )  //i
  );
  assign io_readA_Data = cacheA_io_read_Data;
  assign io_readB_Data = cacheB_io_read_Data;
  assign io_readC_Data = cacheC_io_read_Data;
  assign io_memA_read_clk = cacheA_io_memRead_clk;
  assign io_memA_read_rst = cacheA_io_memRead_rst;
  assign io_memA_read_Valid = cacheA_io_memRead_Valid;
  assign io_memA_read_Address = cacheA_io_memRead_Address;
  assign io_memA_write_clk = cacheA_io_memWrite_clk;
  assign io_memA_write_rst = cacheA_io_memWrite_rst;
  assign io_memA_write_Wen = cacheA_io_memWrite_Wen;
  assign io_memA_write_Valid = cacheA_io_memWrite_Valid;
  assign io_memA_write_Address = cacheA_io_memWrite_Address;
  assign io_memA_write_Data = cacheA_io_memWrite_Data;
  assign io_memB_read_clk = cacheB_io_memRead_clk;
  assign io_memB_read_rst = cacheB_io_memRead_rst;
  assign io_memB_read_Valid = cacheB_io_memRead_Valid;
  assign io_memB_read_Address = cacheB_io_memRead_Address;
  assign io_memB_write_clk = cacheB_io_memWrite_clk;
  assign io_memB_write_rst = cacheB_io_memWrite_rst;
  assign io_memB_write_Wen = cacheB_io_memWrite_Wen;
  assign io_memB_write_Valid = cacheB_io_memWrite_Valid;
  assign io_memB_write_Address = cacheB_io_memWrite_Address;
  assign io_memB_write_Data = cacheB_io_memWrite_Data;
  assign io_memC_read_clk = cacheC_io_memRead_clk;
  assign io_memC_read_rst = cacheC_io_memRead_rst;
  assign io_memC_read_Valid = cacheC_io_memRead_Valid;
  assign io_memC_read_Address = cacheC_io_memRead_Address;
  assign io_memC_write_clk = cacheC_io_memWrite_clk;
  assign io_memC_write_rst = cacheC_io_memWrite_rst;
  assign io_memC_write_Wen = cacheC_io_memWrite_Wen;
  assign io_memC_write_Valid = cacheC_io_memWrite_Valid;
  assign io_memC_write_Address = cacheC_io_memWrite_Address;
  assign io_memC_write_Data = cacheC_io_memWrite_Data;
  assign io_globalIntr = cacheC_io_intr;
  assign busCtrl_readErrorFlag = 1'b0;
  assign busCtrl_writeErrorFlag = 1'b0;
  assign busCtrl_readHaltRequest = 1'b0;
  assign busCtrl_writeHaltRequest = 1'b0;
  assign busCtrl_writeOccur = (busCtrl_writeJoinEvent_valid && busCtrl_writeJoinEvent_ready);
  assign busCtrl_writeJoinEvent_valid = (io_axi_aw_valid && io_axi_w_valid);
  assign io_axi_aw_ready = busCtrl_writeOccur;
  assign io_axi_w_ready = busCtrl_writeOccur;
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
  assign io_axi_b_valid = busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_valid;
  assign busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_ready = io_axi_b_ready;
  assign io_axi_b_payload_resp = busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_payload_resp;
  assign busCtrl_readDataStage_fire = (busCtrl_readDataStage_valid && busCtrl_readDataStage_ready);
  assign io_axi_ar_ready = (! io_axi_ar_rValid);
  assign busCtrl_readDataStage_valid = io_axi_ar_rValid;
  assign busCtrl_readDataStage_payload_addr = io_axi_ar_rData_addr;
  assign busCtrl_readDataStage_payload_prot = io_axi_ar_rData_prot;
  assign _zz_busCtrl_readDataStage_ready = (! busCtrl_readHaltRequest);
  assign busCtrl_readDataStage_haltWhen_valid = (busCtrl_readDataStage_valid && _zz_busCtrl_readDataStage_ready);
  assign busCtrl_readDataStage_ready = (busCtrl_readDataStage_haltWhen_ready && _zz_busCtrl_readDataStage_ready);
  assign busCtrl_readDataStage_haltWhen_payload_addr = busCtrl_readDataStage_payload_addr;
  assign busCtrl_readDataStage_haltWhen_payload_prot = busCtrl_readDataStage_payload_prot;
  assign busCtrl_readDataStage_haltWhen_translated_valid = busCtrl_readDataStage_haltWhen_valid;
  assign busCtrl_readDataStage_haltWhen_ready = busCtrl_readDataStage_haltWhen_translated_ready;
  assign busCtrl_readDataStage_haltWhen_translated_payload_data = busCtrl_readRsp_data;
  assign busCtrl_readDataStage_haltWhen_translated_payload_resp = busCtrl_readRsp_resp;
  assign io_axi_r_valid = busCtrl_readDataStage_haltWhen_translated_valid;
  assign busCtrl_readDataStage_haltWhen_translated_ready = io_axi_r_ready;
  assign io_axi_r_payload_data = busCtrl_readDataStage_haltWhen_translated_payload_data;
  assign io_axi_r_payload_resp = busCtrl_readDataStage_haltWhen_translated_payload_resp;
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
    busCtrl_readRsp_data = 32'h0;
    case(busCtrl_readAddressMasked)
      8'h0 : begin
        busCtrl_readRsp_data[15 : 0] = lifeCfgRegA;
      end
      8'h04 : begin
        busCtrl_readRsp_data[15 : 0] = lifeCfgRegB;
      end
      8'h08 : begin
        busCtrl_readRsp_data[0 : 0] = cacheA_io_status;
        busCtrl_readRsp_data[1 : 1] = cacheB_io_status;
        busCtrl_readRsp_data[2 : 2] = cacheC_io_status;
        busCtrl_readRsp_data[3 : 3] = cacheC_io_intr;
        busCtrl_readRsp_data[4 : 4] = cacheC_io_full;
      end
      default : begin
      end
    endcase
  end

  assign busCtrl_readAddressMasked = (busCtrl_readDataStage_payload_addr & (~ 8'h03));
  assign busCtrl_writeAddressMasked = (io_axi_aw_payload_addr & (~ 8'h03));
  assign busCtrl_readOccur = (io_axi_r_valid && io_axi_r_ready);
  always @(*) begin
    cacheC_io_intrClear = 1'b0;
    if(when_BusSlaveFactory_l377) begin
      if(when_BusSlaveFactory_l379) begin
        cacheC_io_intrClear = _zz_io_intrClear[0];
      end
    end
  end

  always @(*) begin
    when_BusSlaveFactory_l377 = 1'b0;
    case(busCtrl_writeAddressMasked)
      8'h0c : begin
        if(busCtrl_writeOccur) begin
          when_BusSlaveFactory_l377 = 1'b1;
        end
      end
      default : begin
      end
    endcase
  end

  assign when_BusSlaveFactory_l379 = io_axi_w_payload_data[0];
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      busCtrl_writeJoinEvent_translated_haltWhen_rValid <= 1'b0;
      io_axi_ar_rValid <= 1'b0;
      lifeCfgRegA <= 16'h0;
      lifeCfgRegB <= 16'h0;
    end else begin
      if(busCtrl_writeJoinEvent_translated_haltWhen_valid) begin
        busCtrl_writeJoinEvent_translated_haltWhen_rValid <= 1'b1;
      end
      if(busCtrl_writeJoinEvent_translated_haltWhen_halfPipe_fire) begin
        busCtrl_writeJoinEvent_translated_haltWhen_rValid <= 1'b0;
      end
      if(io_axi_ar_valid) begin
        io_axi_ar_rValid <= 1'b1;
      end
      if(busCtrl_readDataStage_fire) begin
        io_axi_ar_rValid <= 1'b0;
      end
      case(busCtrl_writeAddressMasked)
        8'h0 : begin
          if(busCtrl_writeOccur) begin
            lifeCfgRegA <= io_axi_w_payload_data[15 : 0];
          end
        end
        8'h04 : begin
          if(busCtrl_writeOccur) begin
            lifeCfgRegB <= io_axi_w_payload_data[15 : 0];
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
    if(io_axi_ar_ready) begin
      io_axi_ar_rData_addr <= io_axi_ar_payload_addr;
      io_axi_ar_rData_prot <= io_axi_ar_payload_prot;
    end
  end


endmodule

module OutputMatrixCacheController (
  input  wire          io_read_clk,
  input  wire          io_read_rst,
  input  wire          io_read_Valid,
  input  wire [19:0]   io_read_Address,
  output wire [255:0]  io_read_Data,
  input  wire          io_write_clk,
  input  wire          io_write_rst,
  input  wire [31:0]   io_write_Wen,
  input  wire          io_write_Valid,
  input  wire [19:0]   io_write_Address,
  input  wire [255:0]  io_write_Data,
  input  wire          io_switch,
  input  wire          io_dmaIntr,
  output wire          io_status,
  output wire          io_intr,
  input  wire          io_intrClear,
  output wire          io_full,
  output wire          io_memRead_clk,
  output wire          io_memRead_rst,
  output wire          io_memRead_Valid,
  output wire [20:0]   io_memRead_Address,
  input  wire [255:0]  io_memRead_Data,
  output wire          io_memWrite_clk,
  output wire          io_memWrite_rst,
  output wire [31:0]   io_memWrite_Wen,
  output wire          io_memWrite_Valid,
  output wire [20:0]   io_memWrite_Address,
  output wire [255:0]  io_memWrite_Data,
  input  wire          clk,
  input  wire          resetn
);

  wire       [255:0]  matrixCacheInterface_io_read_Data;
  wire                matrixCacheInterface_io_status;
  wire                matrixCacheInterface_io_intr;
  wire                matrixCacheInterface_io_full;
  wire                matrixCacheInterface_io_read_sdpram_clk;
  wire                matrixCacheInterface_io_read_sdpram_rst;
  wire                matrixCacheInterface_io_read_sdpram_Valid;
  wire       [20:0]   matrixCacheInterface_io_read_sdpram_Address;
  wire                matrixCacheInterface_io_write_sdpram_clk;
  wire                matrixCacheInterface_io_write_sdpram_rst;
  wire       [31:0]   matrixCacheInterface_io_write_sdpram_Wen;
  wire                matrixCacheInterface_io_write_sdpram_Valid;
  wire       [20:0]   matrixCacheInterface_io_write_sdpram_Address;
  wire       [255:0]  matrixCacheInterface_io_write_sdpram_Data;

  OutputMatrixCacheInterface matrixCacheInterface (
    .io_read_clk             (io_read_clk                                       ), //i
    .io_read_rst             (io_read_rst                                       ), //i
    .io_read_Valid           (io_read_Valid                                     ), //i
    .io_read_Address         (io_read_Address[19:0]                             ), //i
    .io_read_Data            (matrixCacheInterface_io_read_Data[255:0]          ), //o
    .io_write_clk            (io_write_clk                                      ), //i
    .io_write_rst            (io_write_rst                                      ), //i
    .io_write_Wen            (io_write_Wen[31:0]                                ), //i
    .io_write_Valid          (io_write_Valid                                    ), //i
    .io_write_Address        (io_write_Address[19:0]                            ), //i
    .io_write_Data           (io_write_Data[255:0]                              ), //i
    .io_switch               (io_switch                                         ), //i
    .io_dmaIntr              (io_dmaIntr                                        ), //i
    .io_status               (matrixCacheInterface_io_status                    ), //o
    .io_intr                 (matrixCacheInterface_io_intr                      ), //o
    .io_intrClear            (io_intrClear                                      ), //i
    .io_full                 (matrixCacheInterface_io_full                      ), //o
    .io_read_sdpram_clk      (matrixCacheInterface_io_read_sdpram_clk           ), //o
    .io_read_sdpram_rst      (matrixCacheInterface_io_read_sdpram_rst           ), //o
    .io_read_sdpram_Valid    (matrixCacheInterface_io_read_sdpram_Valid         ), //o
    .io_read_sdpram_Address  (matrixCacheInterface_io_read_sdpram_Address[20:0] ), //o
    .io_read_sdpram_Data     (io_memRead_Data[255:0]                            ), //i
    .io_write_sdpram_clk     (matrixCacheInterface_io_write_sdpram_clk          ), //o
    .io_write_sdpram_rst     (matrixCacheInterface_io_write_sdpram_rst          ), //o
    .io_write_sdpram_Wen     (matrixCacheInterface_io_write_sdpram_Wen[31:0]    ), //o
    .io_write_sdpram_Valid   (matrixCacheInterface_io_write_sdpram_Valid        ), //o
    .io_write_sdpram_Address (matrixCacheInterface_io_write_sdpram_Address[20:0]), //o
    .io_write_sdpram_Data    (matrixCacheInterface_io_write_sdpram_Data[255:0]  ), //o
    .clk                     (clk                                               ), //i
    .resetn                  (resetn                                            )  //i
  );
  assign io_read_Data = matrixCacheInterface_io_read_Data;
  assign io_status = matrixCacheInterface_io_status;
  assign io_intr = matrixCacheInterface_io_intr;
  assign io_full = matrixCacheInterface_io_full;
  assign io_memRead_clk = matrixCacheInterface_io_read_sdpram_clk;
  assign io_memRead_rst = matrixCacheInterface_io_read_sdpram_rst;
  assign io_memRead_Valid = matrixCacheInterface_io_read_sdpram_Valid;
  assign io_memRead_Address = matrixCacheInterface_io_read_sdpram_Address;
  assign io_memWrite_clk = matrixCacheInterface_io_write_sdpram_clk;
  assign io_memWrite_rst = matrixCacheInterface_io_write_sdpram_rst;
  assign io_memWrite_Wen = matrixCacheInterface_io_write_sdpram_Wen;
  assign io_memWrite_Valid = matrixCacheInterface_io_write_sdpram_Valid;
  assign io_memWrite_Address = matrixCacheInterface_io_write_sdpram_Address;
  assign io_memWrite_Data = matrixCacheInterface_io_write_sdpram_Data;

endmodule

//InputMatrixCacheController_1 replaced by InputMatrixCacheController

module InputMatrixCacheController (
  input  wire          io_read_clk,
  input  wire          io_read_rst,
  input  wire          io_read_Valid,
  input  wire [19:0]   io_read_Address,
  output wire [255:0]  io_read_Data,
  input  wire          io_write_clk,
  input  wire          io_write_rst,
  input  wire [31:0]   io_write_Wen,
  input  wire          io_write_Valid,
  input  wire [19:0]   io_write_Address,
  input  wire [255:0]  io_write_Data,
  input  wire          io_switch,
  input  wire          io_dmaIntr,
  input  wire [15:0]   io_lifeCfg,
  output wire          io_status,
  output wire          io_empty,
  output wire          io_memRead_clk,
  output wire          io_memRead_rst,
  output wire          io_memRead_Valid,
  output wire [20:0]   io_memRead_Address,
  input  wire [255:0]  io_memRead_Data,
  output wire          io_memWrite_clk,
  output wire          io_memWrite_rst,
  output wire [31:0]   io_memWrite_Wen,
  output wire          io_memWrite_Valid,
  output wire [20:0]   io_memWrite_Address,
  output wire [255:0]  io_memWrite_Data,
  input  wire          clk,
  input  wire          resetn
);

  wire       [255:0]  inputMatrixCacheInterface_2_io_read_Data;
  wire                inputMatrixCacheInterface_2_io_status;
  wire                inputMatrixCacheInterface_2_io_empty;
  wire                inputMatrixCacheInterface_2_io_read_sdpram_clk;
  wire                inputMatrixCacheInterface_2_io_read_sdpram_rst;
  wire                inputMatrixCacheInterface_2_io_read_sdpram_Valid;
  wire       [20:0]   inputMatrixCacheInterface_2_io_read_sdpram_Address;
  wire                inputMatrixCacheInterface_2_io_write_sdpram_clk;
  wire                inputMatrixCacheInterface_2_io_write_sdpram_rst;
  wire       [31:0]   inputMatrixCacheInterface_2_io_write_sdpram_Wen;
  wire                inputMatrixCacheInterface_2_io_write_sdpram_Valid;
  wire       [20:0]   inputMatrixCacheInterface_2_io_write_sdpram_Address;
  wire       [255:0]  inputMatrixCacheInterface_2_io_write_sdpram_Data;

  InputMatrixCacheInterface inputMatrixCacheInterface_2 (
    .io_read_clk             (io_read_clk                                              ), //i
    .io_read_rst             (io_read_rst                                              ), //i
    .io_read_Valid           (io_read_Valid                                            ), //i
    .io_read_Address         (io_read_Address[19:0]                                    ), //i
    .io_read_Data            (inputMatrixCacheInterface_2_io_read_Data[255:0]          ), //o
    .io_write_clk            (io_write_clk                                             ), //i
    .io_write_rst            (io_write_rst                                             ), //i
    .io_write_Wen            (io_write_Wen[31:0]                                       ), //i
    .io_write_Valid          (io_write_Valid                                           ), //i
    .io_write_Address        (io_write_Address[19:0]                                   ), //i
    .io_write_Data           (io_write_Data[255:0]                                     ), //i
    .io_switch               (io_switch                                                ), //i
    .io_dmaIntr              (io_dmaIntr                                               ), //i
    .io_lifeCfg              (io_lifeCfg[15:0]                                         ), //i
    .io_status               (inputMatrixCacheInterface_2_io_status                    ), //o
    .io_empty                (inputMatrixCacheInterface_2_io_empty                     ), //o
    .io_read_sdpram_clk      (inputMatrixCacheInterface_2_io_read_sdpram_clk           ), //o
    .io_read_sdpram_rst      (inputMatrixCacheInterface_2_io_read_sdpram_rst           ), //o
    .io_read_sdpram_Valid    (inputMatrixCacheInterface_2_io_read_sdpram_Valid         ), //o
    .io_read_sdpram_Address  (inputMatrixCacheInterface_2_io_read_sdpram_Address[20:0] ), //o
    .io_read_sdpram_Data     (io_memRead_Data[255:0]                                   ), //i
    .io_write_sdpram_clk     (inputMatrixCacheInterface_2_io_write_sdpram_clk          ), //o
    .io_write_sdpram_rst     (inputMatrixCacheInterface_2_io_write_sdpram_rst          ), //o
    .io_write_sdpram_Wen     (inputMatrixCacheInterface_2_io_write_sdpram_Wen[31:0]    ), //o
    .io_write_sdpram_Valid   (inputMatrixCacheInterface_2_io_write_sdpram_Valid        ), //o
    .io_write_sdpram_Address (inputMatrixCacheInterface_2_io_write_sdpram_Address[20:0]), //o
    .io_write_sdpram_Data    (inputMatrixCacheInterface_2_io_write_sdpram_Data[255:0]  ), //o
    .clk                     (clk                                                      ), //i
    .resetn                  (resetn                                                   )  //i
  );
  assign io_read_Data = inputMatrixCacheInterface_2_io_read_Data;
  assign io_status = inputMatrixCacheInterface_2_io_status;
  assign io_empty = inputMatrixCacheInterface_2_io_empty;
  assign io_memRead_clk = inputMatrixCacheInterface_2_io_read_sdpram_clk;
  assign io_memRead_rst = inputMatrixCacheInterface_2_io_read_sdpram_rst;
  assign io_memRead_Valid = inputMatrixCacheInterface_2_io_read_sdpram_Valid;
  assign io_memRead_Address = inputMatrixCacheInterface_2_io_read_sdpram_Address;
  assign io_memWrite_clk = inputMatrixCacheInterface_2_io_write_sdpram_clk;
  assign io_memWrite_rst = inputMatrixCacheInterface_2_io_write_sdpram_rst;
  assign io_memWrite_Wen = inputMatrixCacheInterface_2_io_write_sdpram_Wen;
  assign io_memWrite_Valid = inputMatrixCacheInterface_2_io_write_sdpram_Valid;
  assign io_memWrite_Address = inputMatrixCacheInterface_2_io_write_sdpram_Address;
  assign io_memWrite_Data = inputMatrixCacheInterface_2_io_write_sdpram_Data;

endmodule

module OutputMatrixCacheInterface (
  input  wire          io_read_clk,
  input  wire          io_read_rst,
  input  wire          io_read_Valid,
  input  wire [19:0]   io_read_Address,
  output wire [255:0]  io_read_Data,
  input  wire          io_write_clk,
  input  wire          io_write_rst,
  input  wire [31:0]   io_write_Wen,
  input  wire          io_write_Valid,
  input  wire [19:0]   io_write_Address,
  input  wire [255:0]  io_write_Data,
  input  wire          io_switch,
  input  wire          io_dmaIntr,
  output wire          io_status,
  output wire          io_intr,
  input  wire          io_intrClear,
  output wire          io_full,
  output wire          io_read_sdpram_clk,
  output wire          io_read_sdpram_rst,
  output wire          io_read_sdpram_Valid,
  output wire [20:0]   io_read_sdpram_Address,
  input  wire [255:0]  io_read_sdpram_Data,
  output wire          io_write_sdpram_clk,
  output wire          io_write_sdpram_rst,
  output wire [31:0]   io_write_sdpram_Wen,
  output wire          io_write_sdpram_Valid,
  output wire [20:0]   io_write_sdpram_Address,
  output wire [255:0]  io_write_sdpram_Data,
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
  wire       [20:0]   wrAddrInt;
  wire       [20:0]   rdAddrInt;

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

//InputMatrixCacheInterface_1 replaced by InputMatrixCacheInterface

module InputMatrixCacheInterface (
  input  wire          io_read_clk,
  input  wire          io_read_rst,
  input  wire          io_read_Valid,
  input  wire [19:0]   io_read_Address,
  output wire [255:0]  io_read_Data,
  input  wire          io_write_clk,
  input  wire          io_write_rst,
  input  wire [31:0]   io_write_Wen,
  input  wire          io_write_Valid,
  input  wire [19:0]   io_write_Address,
  input  wire [255:0]  io_write_Data,
  input  wire          io_switch,
  input  wire          io_dmaIntr,
  input  wire [15:0]   io_lifeCfg,
  output wire          io_status,
  output wire          io_empty,
  output wire          io_read_sdpram_clk,
  output wire          io_read_sdpram_rst,
  output wire          io_read_sdpram_Valid,
  output wire [20:0]   io_read_sdpram_Address,
  input  wire [255:0]  io_read_sdpram_Data,
  output wire          io_write_sdpram_clk,
  output wire          io_write_sdpram_rst,
  output wire [31:0]   io_write_sdpram_Wen,
  output wire          io_write_sdpram_Valid,
  output wire [20:0]   io_write_sdpram_Address,
  output wire [255:0]  io_write_sdpram_Data,
  input  wire          clk,
  input  wire          resetn
);

  reg                 _zz_when_InputMatrixCache_l151;
  reg                 _zz_when_InputMatrixCache_l158;
  reg        [15:0]   _zz__zz_when_InputMatrixCache_l160;
  reg        [0:0]    wrPtr;
  reg        [0:0]    rdPtr;
  reg                 bankValid_0;
  reg                 bankValid_1;
  reg        [15:0]   lifeCnt_0;
  reg        [15:0]   lifeCnt_1;
  wire                anyFree;
  wire                allEmpty;
  reg                 io_dmaIntr_regNext;
  wire       [1:0]    _zz_1;
  wire                when_InputMatrixCache_l151;
  wire       [1:0]    _zz_2;
  wire       [1:0]    _zz_3;
  wire                when_InputMatrixCache_l158;
  wire       [15:0]   _zz_when_InputMatrixCache_l160;
  wire       [1:0]    _zz_4;
  wire                _zz_5;
  wire                _zz_6;
  wire                when_InputMatrixCache_l160;
  wire       [15:0]   _zz_lifeCnt_0;
  wire       [20:0]   wrAddrInt;
  wire       [20:0]   rdAddrInt;

  always @(*) begin
    case(wrPtr)
      1'b0 : _zz_when_InputMatrixCache_l151 = bankValid_0;
      default : _zz_when_InputMatrixCache_l151 = bankValid_1;
    endcase
  end

  always @(*) begin
    case(rdPtr)
      1'b0 : begin
        _zz_when_InputMatrixCache_l158 = bankValid_0;
        _zz__zz_when_InputMatrixCache_l160 = lifeCnt_0;
      end
      default : begin
        _zz_when_InputMatrixCache_l158 = bankValid_1;
        _zz__zz_when_InputMatrixCache_l160 = lifeCnt_1;
      end
    endcase
  end

  assign anyFree = ((! bankValid_0) || (! bankValid_1));
  assign allEmpty = ((! bankValid_0) && (! bankValid_1));
  assign io_status = anyFree;
  assign io_empty = allEmpty;
  assign _zz_1 = ({1'd0,1'b1} <<< wrPtr);
  assign when_InputMatrixCache_l151 = ((io_dmaIntr && (! io_dmaIntr_regNext)) && (! _zz_when_InputMatrixCache_l151));
  assign _zz_2 = ({1'd0,1'b1} <<< wrPtr);
  assign _zz_3 = ({1'd0,1'b1} <<< rdPtr);
  assign when_InputMatrixCache_l158 = (io_switch && _zz_when_InputMatrixCache_l158);
  assign _zz_when_InputMatrixCache_l160 = _zz__zz_when_InputMatrixCache_l160;
  assign _zz_4 = ({1'd0,1'b1} <<< rdPtr);
  assign _zz_5 = _zz_4[0];
  assign _zz_6 = _zz_4[1];
  assign when_InputMatrixCache_l160 = (16'h0001 < _zz_when_InputMatrixCache_l160);
  assign _zz_lifeCnt_0 = (_zz_when_InputMatrixCache_l160 - 16'h0001);
  assign wrAddrInt = {wrPtr[0],io_write_Address};
  assign rdAddrInt = {rdPtr[0],io_read_Address};
  assign io_write_sdpram_Valid = io_write_Valid;
  assign io_write_sdpram_Address = wrAddrInt;
  assign io_write_sdpram_Data = io_write_Data;
  assign io_write_sdpram_clk = io_write_clk;
  assign io_write_sdpram_rst = io_write_rst;
  assign io_write_sdpram_Wen = io_write_Wen;
  assign io_read_sdpram_Valid = io_read_Valid;
  assign io_read_sdpram_clk = io_read_clk;
  assign io_read_sdpram_rst = io_read_rst;
  assign io_read_sdpram_Address = rdAddrInt;
  assign io_read_Data = io_read_sdpram_Data;
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      wrPtr <= 1'b0;
      rdPtr <= 1'b0;
      bankValid_0 <= 1'b0;
      bankValid_1 <= 1'b0;
      lifeCnt_0 <= 16'h0;
      lifeCnt_1 <= 16'h0;
    end else begin
      if(when_InputMatrixCache_l151) begin
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
      if(when_InputMatrixCache_l158) begin
        if(when_InputMatrixCache_l160) begin
          if(_zz_5) begin
            lifeCnt_0 <= _zz_lifeCnt_0;
          end
          if(_zz_6) begin
            lifeCnt_1 <= _zz_lifeCnt_0;
          end
        end else begin
          if(_zz_5) begin
            lifeCnt_0 <= 16'h0;
          end
          if(_zz_6) begin
            lifeCnt_1 <= 16'h0;
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

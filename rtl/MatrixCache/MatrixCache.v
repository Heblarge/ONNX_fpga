// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : MatrixCache
// Git hash  : a9cbd96b00c1311cf49010ec546f8110a9a55e92

`timescale 1ns/1ps 
module MatrixCache (
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
  input  wire [3:0]    io_readA_Address,
  output wire [7:0]    io_readA_Data,
  input  wire          io_writeA_clk,
  input  wire          io_writeA_rst,
  input  wire [0:0]    io_writeA_Wen,
  input  wire          io_writeA_Valid,
  input  wire [3:0]    io_writeA_Address,
  input  wire [7:0]    io_writeA_Data,
  input  wire          io_switchA,
  input  wire          io_dmaDoneA,
  input  wire          io_readB_clk,
  input  wire          io_readB_rst,
  input  wire          io_readB_Valid,
  input  wire [3:0]    io_readB_Address,
  output wire [7:0]    io_readB_Data,
  input  wire          io_writeB_clk,
  input  wire          io_writeB_rst,
  input  wire [0:0]    io_writeB_Wen,
  input  wire          io_writeB_Valid,
  input  wire [3:0]    io_writeB_Address,
  input  wire [7:0]    io_writeB_Data,
  input  wire          io_switchB,
  input  wire          io_dmaDoneB,
  input  wire          io_readC_clk,
  input  wire          io_readC_rst,
  input  wire          io_readC_Valid,
  input  wire [3:0]    io_readC_Address,
  output wire [7:0]    io_readC_Data,
  input  wire          io_writeC_clk,
  input  wire          io_writeC_rst,
  input  wire [0:0]    io_writeC_Wen,
  input  wire          io_writeC_Valid,
  input  wire [3:0]    io_writeC_Address,
  input  wire [7:0]    io_writeC_Data,
  input  wire          io_switchC,
  input  wire          io_dmaDoneC,
  input  wire          clk,
  input  wire          resetn
);

  reg                 cacheC_io_intrClear;
  wire       [7:0]    cacheA_io_read_Data;
  wire                cacheA_io_status;
  wire                cacheA_io_empty;
  wire       [7:0]    cacheB_io_read_Data;
  wire                cacheB_io_status;
  wire                cacheB_io_empty;
  wire       [7:0]    cacheC_io_read_Data;
  wire                cacheC_io_status;
  wire                cacheC_io_intr;
  wire                cacheC_io_full;
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
  InputMatrixCache cacheA (
    .io_read_clk      (io_readA_clk            ), //i
    .io_read_rst      (io_readA_rst            ), //i
    .io_read_Valid    (io_readA_Valid          ), //i
    .io_read_Address  (io_readA_Address[3:0]   ), //i
    .io_read_Data     (cacheA_io_read_Data[7:0]), //o
    .io_write_clk     (io_writeA_clk           ), //i
    .io_write_rst     (io_writeA_rst           ), //i
    .io_write_Wen     (io_writeA_Wen           ), //i
    .io_write_Valid   (io_writeA_Valid         ), //i
    .io_write_Address (io_writeA_Address[3:0]  ), //i
    .io_write_Data    (io_writeA_Data[7:0]     ), //i
    .io_switch        (io_switchA              ), //i
    .io_dmaIntr       (io_dmaDoneA             ), //i
    .io_lifeCfg       (lifeCfgRegA[15:0]       ), //i
    .io_status        (cacheA_io_status        ), //o
    .io_empty         (cacheA_io_empty         ), //o
    .clk              (clk                     ), //i
    .resetn           (resetn                  )  //i
  );
  InputMatrixCache cacheB (
    .io_read_clk      (io_readB_clk            ), //i
    .io_read_rst      (io_readB_rst            ), //i
    .io_read_Valid    (io_readB_Valid          ), //i
    .io_read_Address  (io_readB_Address[3:0]   ), //i
    .io_read_Data     (cacheB_io_read_Data[7:0]), //o
    .io_write_clk     (io_writeB_clk           ), //i
    .io_write_rst     (io_writeB_rst           ), //i
    .io_write_Wen     (io_writeB_Wen           ), //i
    .io_write_Valid   (io_writeB_Valid         ), //i
    .io_write_Address (io_writeB_Address[3:0]  ), //i
    .io_write_Data    (io_writeB_Data[7:0]     ), //i
    .io_switch        (io_switchB              ), //i
    .io_dmaIntr       (io_dmaDoneB             ), //i
    .io_lifeCfg       (lifeCfgRegB[15:0]       ), //i
    .io_status        (cacheB_io_status        ), //o
    .io_empty         (cacheB_io_empty         ), //o
    .clk              (clk                     ), //i
    .resetn           (resetn                  )  //i
  );
  OutputMatrixCache cacheC (
    .io_read_clk      (io_readC_clk            ), //i
    .io_read_rst      (io_readC_rst            ), //i
    .io_read_Valid    (io_readC_Valid          ), //i
    .io_read_Address  (io_readC_Address[3:0]   ), //i
    .io_read_Data     (cacheC_io_read_Data[7:0]), //o
    .io_write_clk     (io_writeC_clk           ), //i
    .io_write_rst     (io_writeC_rst           ), //i
    .io_write_Wen     (io_writeC_Wen           ), //i
    .io_write_Valid   (io_writeC_Valid         ), //i
    .io_write_Address (io_writeC_Address[3:0]  ), //i
    .io_write_Data    (io_writeC_Data[7:0]     ), //i
    .io_switch        (io_switchC              ), //i
    .io_dmaIntr       (io_dmaDoneC             ), //i
    .io_status        (cacheC_io_status        ), //o
    .io_intr          (cacheC_io_intr          ), //o
    .io_intrClear     (cacheC_io_intrClear     ), //i
    .io_full          (cacheC_io_full          ), //o
    .clk              (clk                     ), //i
    .resetn           (resetn                  )  //i
  );
  assign io_readA_Data = cacheA_io_read_Data;
  assign io_readB_Data = cacheB_io_read_Data;
  assign io_readC_Data = cacheC_io_read_Data;
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

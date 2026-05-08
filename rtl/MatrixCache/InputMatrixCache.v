// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : InputMatrixCache
// Git hash  : a9cbd96b00c1311cf49010ec546f8110a9a55e92

`timescale 1ns/1ps 
module InputMatrixCache (
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
  input  wire [15:0]   io_lifeCfg,
  output wire          io_status,
  output wire          io_empty,
  input  wire          clk,
  input  wire          resetn
);

  wire       [7:0]    inputMatrixCacheInterface_2_io_read_Data;
  wire                inputMatrixCacheInterface_2_io_status;
  wire                inputMatrixCacheInterface_2_io_empty;
  wire                inputMatrixCacheInterface_2_io_read_sdpram_clk;
  wire                inputMatrixCacheInterface_2_io_read_sdpram_rst;
  wire                inputMatrixCacheInterface_2_io_read_sdpram_Valid;
  wire       [4:0]    inputMatrixCacheInterface_2_io_read_sdpram_Address;
  wire                inputMatrixCacheInterface_2_io_write_sdpram_clk;
  wire                inputMatrixCacheInterface_2_io_write_sdpram_rst;
  wire       [0:0]    inputMatrixCacheInterface_2_io_write_sdpram_Wen;
  wire                inputMatrixCacheInterface_2_io_write_sdpram_Valid;
  wire       [4:0]    inputMatrixCacheInterface_2_io_write_sdpram_Address;
  wire       [7:0]    inputMatrixCacheInterface_2_io_write_sdpram_Data;
  wire       [7:0]    sdpramModel_3_io_read_Data;

  InputMatrixCacheInterface inputMatrixCacheInterface_2 (
    .io_read_clk             (io_read_clk                                             ), //i
    .io_read_rst             (io_read_rst                                             ), //i
    .io_read_Valid           (io_read_Valid                                           ), //i
    .io_read_Address         (io_read_Address[3:0]                                    ), //i
    .io_read_Data            (inputMatrixCacheInterface_2_io_read_Data[7:0]           ), //o
    .io_write_clk            (io_write_clk                                            ), //i
    .io_write_rst            (io_write_rst                                            ), //i
    .io_write_Wen            (io_write_Wen                                            ), //i
    .io_write_Valid          (io_write_Valid                                          ), //i
    .io_write_Address        (io_write_Address[3:0]                                   ), //i
    .io_write_Data           (io_write_Data[7:0]                                      ), //i
    .io_switch               (io_switch                                               ), //i
    .io_dmaIntr              (io_dmaIntr                                              ), //i
    .io_lifeCfg              (io_lifeCfg[15:0]                                        ), //i
    .io_status               (inputMatrixCacheInterface_2_io_status                   ), //o
    .io_empty                (inputMatrixCacheInterface_2_io_empty                    ), //o
    .io_read_sdpram_clk      (inputMatrixCacheInterface_2_io_read_sdpram_clk          ), //o
    .io_read_sdpram_rst      (inputMatrixCacheInterface_2_io_read_sdpram_rst          ), //o
    .io_read_sdpram_Valid    (inputMatrixCacheInterface_2_io_read_sdpram_Valid        ), //o
    .io_read_sdpram_Address  (inputMatrixCacheInterface_2_io_read_sdpram_Address[4:0] ), //o
    .io_read_sdpram_Data     (sdpramModel_3_io_read_Data[7:0]                         ), //i
    .io_write_sdpram_clk     (inputMatrixCacheInterface_2_io_write_sdpram_clk         ), //o
    .io_write_sdpram_rst     (inputMatrixCacheInterface_2_io_write_sdpram_rst         ), //o
    .io_write_sdpram_Wen     (inputMatrixCacheInterface_2_io_write_sdpram_Wen         ), //o
    .io_write_sdpram_Valid   (inputMatrixCacheInterface_2_io_write_sdpram_Valid       ), //o
    .io_write_sdpram_Address (inputMatrixCacheInterface_2_io_write_sdpram_Address[4:0]), //o
    .io_write_sdpram_Data    (inputMatrixCacheInterface_2_io_write_sdpram_Data[7:0]   ), //o
    .clk                     (clk                                                     ), //i
    .resetn                  (resetn                                                  )  //i
  );
  SdpramModel sdpramModel_3 (
    .io_read_clk      (inputMatrixCacheInterface_2_io_read_sdpram_clk          ), //i
    .io_read_rst      (inputMatrixCacheInterface_2_io_read_sdpram_rst          ), //i
    .io_read_Valid    (inputMatrixCacheInterface_2_io_read_sdpram_Valid        ), //i
    .io_read_Address  (inputMatrixCacheInterface_2_io_read_sdpram_Address[4:0] ), //i
    .io_read_Data     (sdpramModel_3_io_read_Data[7:0]                         ), //o
    .io_write_clk     (inputMatrixCacheInterface_2_io_write_sdpram_clk         ), //i
    .io_write_rst     (inputMatrixCacheInterface_2_io_write_sdpram_rst         ), //i
    .io_write_Wen     (inputMatrixCacheInterface_2_io_write_sdpram_Wen         ), //i
    .io_write_Valid   (inputMatrixCacheInterface_2_io_write_sdpram_Valid       ), //i
    .io_write_Address (inputMatrixCacheInterface_2_io_write_sdpram_Address[4:0]), //i
    .io_write_Data    (inputMatrixCacheInterface_2_io_write_sdpram_Data[7:0]   ), //i
    .clk              (clk                                                     ), //i
    .resetn           (resetn                                                  )  //i
  );
  assign io_read_Data = inputMatrixCacheInterface_2_io_read_Data;
  assign io_status = inputMatrixCacheInterface_2_io_status;
  assign io_empty = inputMatrixCacheInterface_2_io_empty;

endmodule

// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : OutputMatrixCache
// Git hash  : a9cbd96b00c1311cf49010ec546f8110a9a55e92

`timescale 1ns/1ps 
module OutputMatrixCache (
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
  input  wire          clk,
  input  wire          resetn
);

  wire       [7:0]    matrixCacheInterface_io_read_Data;
  wire                matrixCacheInterface_io_status;
  wire                matrixCacheInterface_io_intr;
  wire                matrixCacheInterface_io_full;
  wire                matrixCacheInterface_io_read_sdpram_clk;
  wire                matrixCacheInterface_io_read_sdpram_rst;
  wire                matrixCacheInterface_io_read_sdpram_Valid;
  wire       [4:0]    matrixCacheInterface_io_read_sdpram_Address;
  wire                matrixCacheInterface_io_write_sdpram_clk;
  wire                matrixCacheInterface_io_write_sdpram_rst;
  wire       [0:0]    matrixCacheInterface_io_write_sdpram_Wen;
  wire                matrixCacheInterface_io_write_sdpram_Valid;
  wire       [4:0]    matrixCacheInterface_io_write_sdpram_Address;
  wire       [7:0]    matrixCacheInterface_io_write_sdpram_Data;
  wire       [7:0]    sdpramModel_3_io_read_Data;

  OutputMatrixCacheInterface matrixCacheInterface (
    .io_read_clk             (io_read_clk                                      ), //i
    .io_read_rst             (io_read_rst                                      ), //i
    .io_read_Valid           (io_read_Valid                                    ), //i
    .io_read_Address         (io_read_Address[3:0]                             ), //i
    .io_read_Data            (matrixCacheInterface_io_read_Data[7:0]           ), //o
    .io_write_clk            (io_write_clk                                     ), //i
    .io_write_rst            (io_write_rst                                     ), //i
    .io_write_Wen            (io_write_Wen                                     ), //i
    .io_write_Valid          (io_write_Valid                                   ), //i
    .io_write_Address        (io_write_Address[3:0]                            ), //i
    .io_write_Data           (io_write_Data[7:0]                               ), //i
    .io_switch               (io_switch                                        ), //i
    .io_dmaIntr              (io_dmaIntr                                       ), //i
    .io_status               (matrixCacheInterface_io_status                   ), //o
    .io_intr                 (matrixCacheInterface_io_intr                     ), //o
    .io_intrClear            (io_intrClear                                     ), //i
    .io_full                 (matrixCacheInterface_io_full                     ), //o
    .io_read_sdpram_clk      (matrixCacheInterface_io_read_sdpram_clk          ), //o
    .io_read_sdpram_rst      (matrixCacheInterface_io_read_sdpram_rst          ), //o
    .io_read_sdpram_Valid    (matrixCacheInterface_io_read_sdpram_Valid        ), //o
    .io_read_sdpram_Address  (matrixCacheInterface_io_read_sdpram_Address[4:0] ), //o
    .io_read_sdpram_Data     (sdpramModel_3_io_read_Data[7:0]                  ), //i
    .io_write_sdpram_clk     (matrixCacheInterface_io_write_sdpram_clk         ), //o
    .io_write_sdpram_rst     (matrixCacheInterface_io_write_sdpram_rst         ), //o
    .io_write_sdpram_Wen     (matrixCacheInterface_io_write_sdpram_Wen         ), //o
    .io_write_sdpram_Valid   (matrixCacheInterface_io_write_sdpram_Valid       ), //o
    .io_write_sdpram_Address (matrixCacheInterface_io_write_sdpram_Address[4:0]), //o
    .io_write_sdpram_Data    (matrixCacheInterface_io_write_sdpram_Data[7:0]   ), //o
    .clk                     (clk                                              ), //i
    .resetn                  (resetn                                           )  //i
  );
  SdpramModel sdpramModel_3 (
    .io_read_clk      (matrixCacheInterface_io_read_sdpram_clk          ), //i
    .io_read_rst      (matrixCacheInterface_io_read_sdpram_rst          ), //i
    .io_read_Valid    (matrixCacheInterface_io_read_sdpram_Valid        ), //i
    .io_read_Address  (matrixCacheInterface_io_read_sdpram_Address[4:0] ), //i
    .io_read_Data     (sdpramModel_3_io_read_Data[7:0]                  ), //o
    .io_write_clk     (matrixCacheInterface_io_write_sdpram_clk         ), //i
    .io_write_rst     (matrixCacheInterface_io_write_sdpram_rst         ), //i
    .io_write_Wen     (matrixCacheInterface_io_write_sdpram_Wen         ), //i
    .io_write_Valid   (matrixCacheInterface_io_write_sdpram_Valid       ), //i
    .io_write_Address (matrixCacheInterface_io_write_sdpram_Address[4:0]), //i
    .io_write_Data    (matrixCacheInterface_io_write_sdpram_Data[7:0]   ), //i
    .clk              (clk                                              ), //i
    .resetn           (resetn                                           )  //i
  );
  assign io_read_Data = matrixCacheInterface_io_read_Data;
  assign io_status = matrixCacheInterface_io_status;
  assign io_intr = matrixCacheInterface_io_intr;
  assign io_full = matrixCacheInterface_io_full;

endmodule

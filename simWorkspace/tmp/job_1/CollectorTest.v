// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : CollectorTest
// Git hash  : 4e4826c7704249043def433adbdd3ca9bbf3799a

`timescale 1ns/1ps

module CollectorTest (
  input  wire          io_slicedInst_valid,
  output wire          io_slicedInst_ready,
  input  wire [18:0]   io_slicedInst_payload_UID,
  input  wire          io_slicedInst_payload_doTranspose,
  input  wire [16:0]   io_slicedInst_payload_outputAddress,
  input  wire [14:0]   io_slicedInst_payload_outputShape_0,
  input  wire [14:0]   io_slicedInst_payload_outputShape_1,
  input  wire          io_matAfterActivations_0_valid,
  output wire          io_matAfterActivations_0_ready,
  input  wire [8:0]    io_matAfterActivations_0_payload_Activation_x_0,
  input  wire [8:0]    io_matAfterActivations_0_payload_Activation_x_1,
  input  wire [8:0]    io_matAfterActivations_0_payload_Activation_x_2,
  input  wire [8:0]    io_matAfterActivations_0_payload_Activation_x_3,
  input  wire [18:0]   io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID,
  input  wire [12:0]   io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt,
  input  wire [12:0]   io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt,
  input  wire          io_matAfterActivations_0_payload_Final,
  input  wire          io_matAfterActivations_1_valid,
  output wire          io_matAfterActivations_1_ready,
  input  wire [8:0]    io_matAfterActivations_1_payload_Activation_x_0,
  input  wire [8:0]    io_matAfterActivations_1_payload_Activation_x_1,
  input  wire [8:0]    io_matAfterActivations_1_payload_Activation_x_2,
  input  wire [8:0]    io_matAfterActivations_1_payload_Activation_x_3,
  input  wire [18:0]   io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID,
  input  wire [12:0]   io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt,
  input  wire [12:0]   io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt,
  input  wire          io_matAfterActivations_1_payload_Final,
  input  wire          io_matAfterActivations_2_valid,
  output wire          io_matAfterActivations_2_ready,
  input  wire [8:0]    io_matAfterActivations_2_payload_Activation_x_0,
  input  wire [8:0]    io_matAfterActivations_2_payload_Activation_x_1,
  input  wire [8:0]    io_matAfterActivations_2_payload_Activation_x_2,
  input  wire [8:0]    io_matAfterActivations_2_payload_Activation_x_3,
  input  wire [18:0]   io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID,
  input  wire [12:0]   io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt,
  input  wire [12:0]   io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt,
  input  wire          io_matAfterActivations_2_payload_Final,
  input  wire          clk,
  input  wire          reset
);

  wire                sdpramZ_io_read_clk;
  wire       [53:0]   sdpramZ_io_read_Data;
  wire                collector_1_io_slicedInst_ready;
  wire                collector_1_io_memoryWritePort_clk;
  wire       [5:0]    collector_1_io_memoryWritePort_Wen;
  wire                collector_1_io_memoryWritePort_Valid;
  wire       [16:0]   collector_1_io_memoryWritePort_Address;
  wire       [53:0]   collector_1_io_memoryWritePort_Data;
  wire                collector_1_io_matAfterActivations_0_ready;
  wire                collector_1_io_matAfterActivations_1_ready;
  wire                collector_1_io_matAfterActivations_2_ready;
  wire                collector_1_io_instFinish;

  Sdpram sdpramZ (
    .io_read_clk      (sdpramZ_io_read_clk                         ), //i
    .io_read_Valid    (1'b0                                        ), //i
    .io_read_Address  (17'h0                                       ), //i
    .io_read_Data     (sdpramZ_io_read_Data[53:0]                  ), //o
    .io_write_clk     (collector_1_io_memoryWritePort_clk          ), //i
    .io_write_Wen     (collector_1_io_memoryWritePort_Wen[5:0]     ), //i
    .io_write_Valid   (collector_1_io_memoryWritePort_Valid        ), //i
    .io_write_Address (collector_1_io_memoryWritePort_Address[16:0]), //i
    .io_write_Data    (collector_1_io_memoryWritePort_Data[53:0]   ), //i
    .clk              (clk                                         ), //i
    .reset            (reset                                       )  //i
  );
  Collector collector_1 (
    .io_slicedInst_valid                                                                                       (io_slicedInst_valid                                                                                            ), //i
    .io_slicedInst_ready                                                                                       (collector_1_io_slicedInst_ready                                                                                ), //o
    .io_slicedInst_payload_UID                                                                                 (io_slicedInst_payload_UID[18:0]                                                                                ), //i
    .io_slicedInst_payload_doTranspose                                                                         (io_slicedInst_payload_doTranspose                                                                              ), //i
    .io_slicedInst_payload_outputAddress                                                                       (io_slicedInst_payload_outputAddress[16:0]                                                                      ), //i
    .io_slicedInst_payload_outputShape_0                                                                       (io_slicedInst_payload_outputShape_0[14:0]                                                                      ), //i
    .io_slicedInst_payload_outputShape_1                                                                       (io_slicedInst_payload_outputShape_1[14:0]                                                                      ), //i
    .io_memoryWritePort_clk                                                                                    (collector_1_io_memoryWritePort_clk                                                                             ), //o
    .io_memoryWritePort_Wen                                                                                    (collector_1_io_memoryWritePort_Wen[5:0]                                                                        ), //o
    .io_memoryWritePort_Valid                                                                                  (collector_1_io_memoryWritePort_Valid                                                                           ), //o
    .io_memoryWritePort_Address                                                                                (collector_1_io_memoryWritePort_Address[16:0]                                                                   ), //o
    .io_memoryWritePort_Data                                                                                   (collector_1_io_memoryWritePort_Data[53:0]                                                                      ), //o
    .io_matAfterActivations_0_valid                                                                            (io_matAfterActivations_0_valid                                                                                 ), //i
    .io_matAfterActivations_0_ready                                                                            (collector_1_io_matAfterActivations_0_ready                                                                     ), //o
    .io_matAfterActivations_0_payload_Activation_x_0                                                           (io_matAfterActivations_0_payload_Activation_x_0[8:0]                                                           ), //i
    .io_matAfterActivations_0_payload_Activation_x_1                                                           (io_matAfterActivations_0_payload_Activation_x_1[8:0]                                                           ), //i
    .io_matAfterActivations_0_payload_Activation_x_2                                                           (io_matAfterActivations_0_payload_Activation_x_2[8:0]                                                           ), //i
    .io_matAfterActivations_0_payload_Activation_x_3                                                           (io_matAfterActivations_0_payload_Activation_x_3[8:0]                                                           ), //i
    .io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID                (io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID[18:0]               ), //i
    .io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt (io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt[12:0]), //i
    .io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt (io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt[12:0]), //i
    .io_matAfterActivations_0_payload_Final                                                                    (io_matAfterActivations_0_payload_Final                                                                         ), //i
    .io_matAfterActivations_1_valid                                                                            (io_matAfterActivations_1_valid                                                                                 ), //i
    .io_matAfterActivations_1_ready                                                                            (collector_1_io_matAfterActivations_1_ready                                                                     ), //o
    .io_matAfterActivations_1_payload_Activation_x_0                                                           (io_matAfterActivations_1_payload_Activation_x_0[8:0]                                                           ), //i
    .io_matAfterActivations_1_payload_Activation_x_1                                                           (io_matAfterActivations_1_payload_Activation_x_1[8:0]                                                           ), //i
    .io_matAfterActivations_1_payload_Activation_x_2                                                           (io_matAfterActivations_1_payload_Activation_x_2[8:0]                                                           ), //i
    .io_matAfterActivations_1_payload_Activation_x_3                                                           (io_matAfterActivations_1_payload_Activation_x_3[8:0]                                                           ), //i
    .io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID                (io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID[18:0]               ), //i
    .io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt (io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt[12:0]), //i
    .io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt (io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt[12:0]), //i
    .io_matAfterActivations_1_payload_Final                                                                    (io_matAfterActivations_1_payload_Final                                                                         ), //i
    .io_matAfterActivations_2_valid                                                                            (io_matAfterActivations_2_valid                                                                                 ), //i
    .io_matAfterActivations_2_ready                                                                            (collector_1_io_matAfterActivations_2_ready                                                                     ), //o
    .io_matAfterActivations_2_payload_Activation_x_0                                                           (io_matAfterActivations_2_payload_Activation_x_0[8:0]                                                           ), //i
    .io_matAfterActivations_2_payload_Activation_x_1                                                           (io_matAfterActivations_2_payload_Activation_x_1[8:0]                                                           ), //i
    .io_matAfterActivations_2_payload_Activation_x_2                                                           (io_matAfterActivations_2_payload_Activation_x_2[8:0]                                                           ), //i
    .io_matAfterActivations_2_payload_Activation_x_3                                                           (io_matAfterActivations_2_payload_Activation_x_3[8:0]                                                           ), //i
    .io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID                (io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID[18:0]               ), //i
    .io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt (io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt[12:0]), //i
    .io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt (io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt[12:0]), //i
    .io_matAfterActivations_2_payload_Final                                                                    (io_matAfterActivations_2_payload_Final                                                                         ), //i
    .io_instFinish                                                                                             (collector_1_io_instFinish                                                                                      ), //o
    .clk                                                                                                       (clk                                                                                                            ), //i
    .reset                                                                                                     (reset                                                                                                          )  //i
  );
  assign io_slicedInst_ready = collector_1_io_slicedInst_ready;
  assign io_matAfterActivations_0_ready = collector_1_io_matAfterActivations_0_ready;
  assign io_matAfterActivations_1_ready = collector_1_io_matAfterActivations_1_ready;
  assign io_matAfterActivations_2_ready = collector_1_io_matAfterActivations_2_ready;

endmodule

module Collector (
  input  wire          io_slicedInst_valid,
  output wire          io_slicedInst_ready,
  input  wire [18:0]   io_slicedInst_payload_UID,
  input  wire          io_slicedInst_payload_doTranspose,
  input  wire [16:0]   io_slicedInst_payload_outputAddress,
  input  wire [14:0]   io_slicedInst_payload_outputShape_0,
  input  wire [14:0]   io_slicedInst_payload_outputShape_1,
  output wire          io_memoryWritePort_clk,
  output wire [5:0]    io_memoryWritePort_Wen,
  output wire          io_memoryWritePort_Valid,
  output wire [16:0]   io_memoryWritePort_Address,
  output wire [53:0]   io_memoryWritePort_Data,
  input  wire          io_matAfterActivations_0_valid,
  output wire          io_matAfterActivations_0_ready,
  input  wire [8:0]    io_matAfterActivations_0_payload_Activation_x_0,
  input  wire [8:0]    io_matAfterActivations_0_payload_Activation_x_1,
  input  wire [8:0]    io_matAfterActivations_0_payload_Activation_x_2,
  input  wire [8:0]    io_matAfterActivations_0_payload_Activation_x_3,
  input  wire [18:0]   io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID,
  input  wire [12:0]   io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt,
  input  wire [12:0]   io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt,
  input  wire          io_matAfterActivations_0_payload_Final,
  input  wire          io_matAfterActivations_1_valid,
  output wire          io_matAfterActivations_1_ready,
  input  wire [8:0]    io_matAfterActivations_1_payload_Activation_x_0,
  input  wire [8:0]    io_matAfterActivations_1_payload_Activation_x_1,
  input  wire [8:0]    io_matAfterActivations_1_payload_Activation_x_2,
  input  wire [8:0]    io_matAfterActivations_1_payload_Activation_x_3,
  input  wire [18:0]   io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID,
  input  wire [12:0]   io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt,
  input  wire [12:0]   io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt,
  input  wire          io_matAfterActivations_1_payload_Final,
  input  wire          io_matAfterActivations_2_valid,
  output wire          io_matAfterActivations_2_ready,
  input  wire [8:0]    io_matAfterActivations_2_payload_Activation_x_0,
  input  wire [8:0]    io_matAfterActivations_2_payload_Activation_x_1,
  input  wire [8:0]    io_matAfterActivations_2_payload_Activation_x_2,
  input  wire [8:0]    io_matAfterActivations_2_payload_Activation_x_3,
  input  wire [18:0]   io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID,
  input  wire [12:0]   io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt,
  input  wire [12:0]   io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt,
  input  wire          io_matAfterActivations_2_payload_Final,
  output wire          io_instFinish,
  input  wire          clk,
  input  wire          reset
);

  wire                io_slicedInst_fifo_io_push_ready;
  wire                io_slicedInst_fifo_io_pop_valid;
  wire       [18:0]   io_slicedInst_fifo_io_pop_payload_UID;
  wire                io_slicedInst_fifo_io_pop_payload_doTranspose;
  wire       [16:0]   io_slicedInst_fifo_io_pop_payload_outputAddress;
  wire       [14:0]   io_slicedInst_fifo_io_pop_payload_outputShape_0;
  wire       [14:0]   io_slicedInst_fifo_io_pop_payload_outputShape_1;
  wire       [5:0]    io_slicedInst_fifo_io_occupancy;
  wire       [5:0]    io_slicedInst_fifo_io_availability;
  wire                streamArbiter_1_io_inputs_0_ready;
  wire                streamArbiter_1_io_inputs_1_ready;
  wire                streamArbiter_1_io_inputs_2_ready;
  wire                streamArbiter_1_io_output_valid;
  wire                streamArbiter_1_io_output_payload_last;
  wire       [8:0]    streamArbiter_1_io_output_payload_fragment_Activation_x_0;
  wire       [8:0]    streamArbiter_1_io_output_payload_fragment_Activation_x_1;
  wire       [8:0]    streamArbiter_1_io_output_payload_fragment_Activation_x_2;
  wire       [8:0]    streamArbiter_1_io_output_payload_fragment_Activation_x_3;
  wire       [18:0]   streamArbiter_1_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID;
  wire       [12:0]   streamArbiter_1_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   streamArbiter_1_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
  wire       [1:0]    streamArbiter_1_io_chosen;
  wire       [2:0]    streamArbiter_1_io_chosenOH;
  wire       [3:0]    _zz_matZSubReceiveRowCnt_valueNext;
  wire       [2:0]    _zz_matZSubWriteRowCnt_valueNext;
  wire       [16:0]   _zz_io_memoryWritePort_Address;
  wire       [16:0]   _zz_io_memoryWritePort_Address_1;
  wire       [30:0]   _zz_io_memoryWritePort_Address_2;
  wire       [30:0]   _zz_io_memoryWritePort_Address_3;
  wire       [15:0]   _zz_io_memoryWritePort_Address_4;
  wire       [15:0]   _zz_io_memoryWritePort_Address_5;
  wire       [15:0]   _zz_io_memoryWritePort_Address_6;
  wire       [16:0]   _zz_io_memoryWritePort_Address_7;
  wire       [12:0]   _zz_io_memoryWritePort_Address_8;
  wire       [8:0]    _zz__zz_io_memoryWritePort_Data;
  wire       [8:0]    _zz__zz_io_memoryWritePort_Data_1;
  wire       [8:0]    _zz__zz_io_memoryWritePort_Data_2;
  wire       [17:0]   _zz__zz_io_memoryWritePort_Data_3;
  wire       [8:0]    _zz__zz_io_memoryWritePort_Data_4;
  wire       [26:0]   _zz__zz_io_memoryWritePort_Data_5;
  wire       [35:0]   _zz__zz_io_memoryWritePort_Data_6;
  wire       [107:0]  _zz__zz_io_memoryWritePort_Data_7;
  wire       [8:0]    _zz__zz_io_memoryWritePort_Data_8;
  wire       [8:0]    _zz__zz_io_memoryWritePort_Data_9;
  wire       [8:0]    _zz__zz_io_memoryWritePort_Data_10;
  wire       [8:0]    _zz__zz_io_memoryWritePort_Data_11;
  reg        [53:0]   _zz_io_memoryWritePort_Data_1;
  wire       [29:0]   _zz_matZSliceCnt_willOverflowIfInc;
  wire       [29:0]   _zz_matZSliceCnt_willOverflowIfInc_1;
  wire       [29:0]   _zz_matZSliceCnt_willOverflowIfInc_2;
  wire       [14:0]   _zz_matZSliceCnt_willOverflowIfInc_3;
  wire       [29:0]   _zz_matZSliceCnt_valueNext;
  wire                slicedInst_valid;
  reg                 slicedInst_ready;
  wire       [18:0]   slicedInst_payload_UID;
  wire                slicedInst_payload_doTranspose;
  wire       [16:0]   slicedInst_payload_outputAddress;
  wire       [14:0]   slicedInst_payload_outputShape_0;
  wire       [14:0]   slicedInst_payload_outputShape_1;
  reg        [18:0]   slicedInstReg_UID;
  reg                 slicedInstReg_doTranspose;
  reg        [16:0]   slicedInstReg_outputAddress;
  reg        [14:0]   slicedInstReg_outputShape_0;
  reg        [14:0]   slicedInstReg_outputShape_1;
  wire                instFinish;
  wire                slicedInst_fire;
  wire                matAfterActivation_valid;
  reg                 matAfterActivation_ready;
  wire       [8:0]    matAfterActivation_payload_Activation_x_0;
  wire       [8:0]    matAfterActivation_payload_Activation_x_1;
  wire       [8:0]    matAfterActivation_payload_Activation_x_2;
  wire       [8:0]    matAfterActivation_payload_Activation_x_3;
  wire       [18:0]   matAfterActivation_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID;
  wire       [12:0]   matAfterActivation_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   matAfterActivation_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
  wire                matAfterActivation_payload_Final;
  wire                matAfterActivation_fire;
  reg                 lock;
  wire                matAfterActivations_0_valid;
  wire                matAfterActivations_0_ready;
  wire                matAfterActivations_0_payload_last;
  wire       [8:0]    matAfterActivations_0_payload_fragment_Activation_x_0;
  wire       [8:0]    matAfterActivations_0_payload_fragment_Activation_x_1;
  wire       [8:0]    matAfterActivations_0_payload_fragment_Activation_x_2;
  wire       [8:0]    matAfterActivations_0_payload_fragment_Activation_x_3;
  wire       [18:0]   matAfterActivations_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID;
  wire       [12:0]   matAfterActivations_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   matAfterActivations_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
  wire                matAfterActivations_1_valid;
  wire                matAfterActivations_1_ready;
  wire                matAfterActivations_1_payload_last;
  wire       [8:0]    matAfterActivations_1_payload_fragment_Activation_x_0;
  wire       [8:0]    matAfterActivations_1_payload_fragment_Activation_x_1;
  wire       [8:0]    matAfterActivations_1_payload_fragment_Activation_x_2;
  wire       [8:0]    matAfterActivations_1_payload_fragment_Activation_x_3;
  wire       [18:0]   matAfterActivations_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID;
  wire       [12:0]   matAfterActivations_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   matAfterActivations_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
  wire                matAfterActivations_2_valid;
  wire                matAfterActivations_2_ready;
  wire                matAfterActivations_2_payload_last;
  wire       [8:0]    matAfterActivations_2_payload_fragment_Activation_x_0;
  wire       [8:0]    matAfterActivations_2_payload_fragment_Activation_x_1;
  wire       [8:0]    matAfterActivations_2_payload_fragment_Activation_x_2;
  wire       [8:0]    matAfterActivations_2_payload_fragment_Activation_x_3;
  wire       [18:0]   matAfterActivations_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID;
  wire       [12:0]   matAfterActivations_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   matAfterActivations_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
  reg        [3:0]    matZSubReceiveRowCnt_cnt;
  wire                matZSubReceiveRowCnt_willOverflowIfInc;
  wire                matZSubReceiveRowCnt_willOverflow;
  wire       [3:0]    matZSubReceiveRowCnt_valueNext;
  wire                matZsubWriteFinish;
  wire                when_Collector_l102;
  wire                when_Collector_l108;
  reg        [12:0]   matARowSliceCnt;
  wire                when_Collector_l112;
  reg        [12:0]   matBColSliceCnt;
  reg        [8:0]    matZsub_0_0;
  reg        [8:0]    matZsub_0_1;
  reg        [8:0]    matZsub_0_2;
  reg        [8:0]    matZsub_0_3;
  reg        [8:0]    matZsub_1_0;
  reg        [8:0]    matZsub_1_1;
  reg        [8:0]    matZsub_1_2;
  reg        [8:0]    matZsub_1_3;
  reg        [8:0]    matZsub_2_0;
  reg        [8:0]    matZsub_2_1;
  reg        [8:0]    matZsub_2_2;
  reg        [8:0]    matZsub_2_3;
  reg        [8:0]    matZsub_3_0;
  reg        [8:0]    matZsub_3_1;
  reg        [8:0]    matZsub_3_2;
  reg        [8:0]    matZsub_3_3;
  reg        [8:0]    matZsub_4_0;
  reg        [8:0]    matZsub_4_1;
  reg        [8:0]    matZsub_4_2;
  reg        [8:0]    matZsub_4_3;
  reg        [8:0]    matZsub_5_0;
  reg        [8:0]    matZsub_5_1;
  reg        [8:0]    matZsub_5_2;
  reg        [8:0]    matZsub_5_3;
  reg        [8:0]    matZsub_6_0;
  reg        [8:0]    matZsub_6_1;
  reg        [8:0]    matZsub_6_2;
  reg        [8:0]    matZsub_6_3;
  reg        [8:0]    matZsub_7_0;
  reg        [8:0]    matZsub_7_1;
  reg        [8:0]    matZsub_7_2;
  reg        [8:0]    matZsub_7_3;
  reg        [8:0]    matZsub_8_0;
  reg        [8:0]    matZsub_8_1;
  reg        [8:0]    matZsub_8_2;
  reg        [8:0]    matZsub_8_3;
  wire       [15:0]   _zz_1;
  wire                _zz_2;
  wire                _zz_3;
  wire                _zz_4;
  wire                _zz_5;
  wire                _zz_6;
  wire                _zz_7;
  wire                _zz_8;
  wire                _zz_9;
  wire                _zz_10;
  reg        [2:0]    matZSubWriteRowCnt_cnt;
  wire                matZSubWriteRowCnt_willOverflowIfInc;
  wire                matZSubWriteRowCnt_willOverflow;
  wire       [2:0]    matZSubWriteRowCnt_valueNext;
  wire       [323:0]  _zz_io_memoryWritePort_Data;
  reg        [29:0]   matZSliceCnt_cnt;
  wire                matZSliceCnt_willOverflowIfInc;
  wire                matZSliceCnt_willOverflow;
  wire       [29:0]   matZSliceCnt_valueNext;
  reg                 instFinish_regNext;

  assign _zz_matZSubReceiveRowCnt_valueNext = (matZSubReceiveRowCnt_cnt + 4'b0001);
  assign _zz_matZSubWriteRowCnt_valueNext = (matZSubWriteRowCnt_cnt + 3'b001);
  assign _zz_io_memoryWritePort_Address = (slicedInstReg_outputAddress + _zz_io_memoryWritePort_Address_1);
  assign _zz_io_memoryWritePort_Address_2 = (_zz_io_memoryWritePort_Address_3 / 3'b110);
  assign _zz_io_memoryWritePort_Address_1 = _zz_io_memoryWritePort_Address_2[16:0];
  assign _zz_io_memoryWritePort_Address_3 = (_zz_io_memoryWritePort_Address_4 * slicedInstReg_outputShape_1);
  assign _zz_io_memoryWritePort_Address_4 = (_zz_io_memoryWritePort_Address_5 + _zz_io_memoryWritePort_Address_6);
  assign _zz_io_memoryWritePort_Address_5 = ((slicedInstReg_doTranspose ? matBColSliceCnt : matARowSliceCnt) * 3'b110);
  assign _zz_io_memoryWritePort_Address_6 = {13'd0, matZSubWriteRowCnt_cnt};
  assign _zz_io_memoryWritePort_Address_8 = (slicedInstReg_doTranspose ? matARowSliceCnt : matBColSliceCnt);
  assign _zz_io_memoryWritePort_Address_7 = {4'd0, _zz_io_memoryWritePort_Address_8};
  assign _zz_matZSliceCnt_willOverflowIfInc = (_zz_matZSliceCnt_willOverflowIfInc_1 - 30'h00000001);
  assign _zz_matZSliceCnt_willOverflowIfInc_1 = (_zz_matZSliceCnt_willOverflowIfInc_2 / 3'b110);
  assign _zz_matZSliceCnt_willOverflowIfInc_2 = (_zz_matZSliceCnt_willOverflowIfInc_3 * slicedInstReg_outputShape_1);
  assign _zz_matZSliceCnt_willOverflowIfInc_3 = (slicedInstReg_outputShape_0 / 3'b110);
  assign _zz_matZSliceCnt_valueNext = (matZSliceCnt_cnt + 30'h00000001);
  assign _zz__zz_io_memoryWritePort_Data = matZsub_6_1;
  assign _zz__zz_io_memoryWritePort_Data_1 = matZsub_6_0;
  assign _zz__zz_io_memoryWritePort_Data_2 = matZsub_5_2;
  assign _zz__zz_io_memoryWritePort_Data_3 = {matZsub_5_1,matZsub_5_0};
  assign _zz__zz_io_memoryWritePort_Data_4 = matZsub_4_3;
  assign _zz__zz_io_memoryWritePort_Data_5 = {matZsub_4_2,{matZsub_4_1,matZsub_4_0}};
  assign _zz__zz_io_memoryWritePort_Data_6 = {matZsub_3_3,{matZsub_3_2,{matZsub_3_1,matZsub_3_0}}};
  assign _zz__zz_io_memoryWritePort_Data_7 = {{matZsub_2_3,{matZsub_2_2,{matZsub_2_1,matZsub_2_0}}},{{matZsub_1_3,{matZsub_1_2,{_zz__zz_io_memoryWritePort_Data_8,_zz__zz_io_memoryWritePort_Data_9}}},{matZsub_0_3,{matZsub_0_2,{_zz__zz_io_memoryWritePort_Data_10,_zz__zz_io_memoryWritePort_Data_11}}}}};
  assign _zz__zz_io_memoryWritePort_Data_8 = matZsub_1_1;
  assign _zz__zz_io_memoryWritePort_Data_9 = matZsub_1_0;
  assign _zz__zz_io_memoryWritePort_Data_10 = matZsub_0_1;
  assign _zz__zz_io_memoryWritePort_Data_11 = matZsub_0_0;
  StreamFifo io_slicedInst_fifo (
    .io_push_valid                 (io_slicedInst_valid                                  ), //i
    .io_push_ready                 (io_slicedInst_fifo_io_push_ready                     ), //o
    .io_push_payload_UID           (io_slicedInst_payload_UID[18:0]                      ), //i
    .io_push_payload_doTranspose   (io_slicedInst_payload_doTranspose                    ), //i
    .io_push_payload_outputAddress (io_slicedInst_payload_outputAddress[16:0]            ), //i
    .io_push_payload_outputShape_0 (io_slicedInst_payload_outputShape_0[14:0]            ), //i
    .io_push_payload_outputShape_1 (io_slicedInst_payload_outputShape_1[14:0]            ), //i
    .io_pop_valid                  (io_slicedInst_fifo_io_pop_valid                      ), //o
    .io_pop_ready                  (slicedInst_ready                                     ), //i
    .io_pop_payload_UID            (io_slicedInst_fifo_io_pop_payload_UID[18:0]          ), //o
    .io_pop_payload_doTranspose    (io_slicedInst_fifo_io_pop_payload_doTranspose        ), //o
    .io_pop_payload_outputAddress  (io_slicedInst_fifo_io_pop_payload_outputAddress[16:0]), //o
    .io_pop_payload_outputShape_0  (io_slicedInst_fifo_io_pop_payload_outputShape_0[14:0]), //o
    .io_pop_payload_outputShape_1  (io_slicedInst_fifo_io_pop_payload_outputShape_1[14:0]), //o
    .io_flush                      (1'b0                                                 ), //i
    .io_occupancy                  (io_slicedInst_fifo_io_occupancy[5:0]                 ), //o
    .io_availability               (io_slicedInst_fifo_io_availability[5:0]              ), //o
    .clk                           (clk                                                  ), //i
    .reset                         (reset                                                )  //i
  );
  StreamArbiter streamArbiter_1 (
    .io_inputs_0_valid                                                                                     (matAfterActivations_0_valid                                                                                              ), //i
    .io_inputs_0_ready                                                                                     (streamArbiter_1_io_inputs_0_ready                                                                                        ), //o
    .io_inputs_0_payload_last                                                                              (matAfterActivations_0_payload_last                                                                                       ), //i
    .io_inputs_0_payload_fragment_Activation_x_0                                                           (matAfterActivations_0_payload_fragment_Activation_x_0[8:0]                                                               ), //i
    .io_inputs_0_payload_fragment_Activation_x_1                                                           (matAfterActivations_0_payload_fragment_Activation_x_1[8:0]                                                               ), //i
    .io_inputs_0_payload_fragment_Activation_x_2                                                           (matAfterActivations_0_payload_fragment_Activation_x_2[8:0]                                                               ), //i
    .io_inputs_0_payload_fragment_Activation_x_3                                                           (matAfterActivations_0_payload_fragment_Activation_x_3[8:0]                                                               ), //i
    .io_inputs_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID                (matAfterActivations_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID[18:0]                   ), //i
    .io_inputs_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt (matAfterActivations_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt[12:0]    ), //i
    .io_inputs_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt (matAfterActivations_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt[12:0]    ), //i
    .io_inputs_1_valid                                                                                     (matAfterActivations_1_valid                                                                                              ), //i
    .io_inputs_1_ready                                                                                     (streamArbiter_1_io_inputs_1_ready                                                                                        ), //o
    .io_inputs_1_payload_last                                                                              (matAfterActivations_1_payload_last                                                                                       ), //i
    .io_inputs_1_payload_fragment_Activation_x_0                                                           (matAfterActivations_1_payload_fragment_Activation_x_0[8:0]                                                               ), //i
    .io_inputs_1_payload_fragment_Activation_x_1                                                           (matAfterActivations_1_payload_fragment_Activation_x_1[8:0]                                                               ), //i
    .io_inputs_1_payload_fragment_Activation_x_2                                                           (matAfterActivations_1_payload_fragment_Activation_x_2[8:0]                                                               ), //i
    .io_inputs_1_payload_fragment_Activation_x_3                                                           (matAfterActivations_1_payload_fragment_Activation_x_3[8:0]                                                               ), //i
    .io_inputs_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID                (matAfterActivations_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID[18:0]                   ), //i
    .io_inputs_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt (matAfterActivations_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt[12:0]    ), //i
    .io_inputs_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt (matAfterActivations_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt[12:0]    ), //i
    .io_inputs_2_valid                                                                                     (matAfterActivations_2_valid                                                                                              ), //i
    .io_inputs_2_ready                                                                                     (streamArbiter_1_io_inputs_2_ready                                                                                        ), //o
    .io_inputs_2_payload_last                                                                              (matAfterActivations_2_payload_last                                                                                       ), //i
    .io_inputs_2_payload_fragment_Activation_x_0                                                           (matAfterActivations_2_payload_fragment_Activation_x_0[8:0]                                                               ), //i
    .io_inputs_2_payload_fragment_Activation_x_1                                                           (matAfterActivations_2_payload_fragment_Activation_x_1[8:0]                                                               ), //i
    .io_inputs_2_payload_fragment_Activation_x_2                                                           (matAfterActivations_2_payload_fragment_Activation_x_2[8:0]                                                               ), //i
    .io_inputs_2_payload_fragment_Activation_x_3                                                           (matAfterActivations_2_payload_fragment_Activation_x_3[8:0]                                                               ), //i
    .io_inputs_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID                (matAfterActivations_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID[18:0]                   ), //i
    .io_inputs_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt (matAfterActivations_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt[12:0]    ), //i
    .io_inputs_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt (matAfterActivations_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt[12:0]    ), //i
    .io_output_valid                                                                                       (streamArbiter_1_io_output_valid                                                                                          ), //o
    .io_output_ready                                                                                       (matAfterActivation_ready                                                                                                 ), //i
    .io_output_payload_last                                                                                (streamArbiter_1_io_output_payload_last                                                                                   ), //o
    .io_output_payload_fragment_Activation_x_0                                                             (streamArbiter_1_io_output_payload_fragment_Activation_x_0[8:0]                                                           ), //o
    .io_output_payload_fragment_Activation_x_1                                                             (streamArbiter_1_io_output_payload_fragment_Activation_x_1[8:0]                                                           ), //o
    .io_output_payload_fragment_Activation_x_2                                                             (streamArbiter_1_io_output_payload_fragment_Activation_x_2[8:0]                                                           ), //o
    .io_output_payload_fragment_Activation_x_3                                                             (streamArbiter_1_io_output_payload_fragment_Activation_x_3[8:0]                                                           ), //o
    .io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID                  (streamArbiter_1_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID[18:0]               ), //o
    .io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt   (streamArbiter_1_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt[12:0]), //o
    .io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt   (streamArbiter_1_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt[12:0]), //o
    .io_chosen                                                                                             (streamArbiter_1_io_chosen[1:0]                                                                                           ), //o
    .io_chosenOH                                                                                           (streamArbiter_1_io_chosenOH[2:0]                                                                                         ), //o
    .clk                                                                                                   (clk                                                                                                                      ), //i
    .reset                                                                                                 (reset                                                                                                                    )  //i
  );
  always @(*) begin
    case(matZSubWriteRowCnt_cnt)
      3'b000 : _zz_io_memoryWritePort_Data_1 = _zz_io_memoryWritePort_Data[53 : 0];
      3'b001 : _zz_io_memoryWritePort_Data_1 = _zz_io_memoryWritePort_Data[107 : 54];
      3'b010 : _zz_io_memoryWritePort_Data_1 = _zz_io_memoryWritePort_Data[161 : 108];
      3'b011 : _zz_io_memoryWritePort_Data_1 = _zz_io_memoryWritePort_Data[215 : 162];
      3'b100 : _zz_io_memoryWritePort_Data_1 = _zz_io_memoryWritePort_Data[269 : 216];
      default : _zz_io_memoryWritePort_Data_1 = _zz_io_memoryWritePort_Data[323 : 270];
    endcase
  end

  assign io_slicedInst_ready = io_slicedInst_fifo_io_push_ready;
  assign slicedInst_valid = io_slicedInst_fifo_io_pop_valid;
  assign slicedInst_payload_UID = io_slicedInst_fifo_io_pop_payload_UID;
  assign slicedInst_payload_doTranspose = io_slicedInst_fifo_io_pop_payload_doTranspose;
  assign slicedInst_payload_outputAddress = io_slicedInst_fifo_io_pop_payload_outputAddress;
  assign slicedInst_payload_outputShape_0 = io_slicedInst_fifo_io_pop_payload_outputShape_0;
  assign slicedInst_payload_outputShape_1 = io_slicedInst_fifo_io_pop_payload_outputShape_1;
  assign slicedInst_fire = (slicedInst_valid && slicedInst_ready);
  assign matAfterActivation_fire = (matAfterActivation_valid && matAfterActivation_ready);
  assign matAfterActivations_0_payload_fragment_Activation_x_0 = io_matAfterActivations_0_payload_Activation_x_0;
  assign matAfterActivations_0_payload_fragment_Activation_x_1 = io_matAfterActivations_0_payload_Activation_x_1;
  assign matAfterActivations_0_payload_fragment_Activation_x_2 = io_matAfterActivations_0_payload_Activation_x_2;
  assign matAfterActivations_0_payload_fragment_Activation_x_3 = io_matAfterActivations_0_payload_Activation_x_3;
  assign matAfterActivations_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID = io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID;
  assign matAfterActivations_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt = io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
  assign matAfterActivations_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt = io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
  assign matAfterActivations_0_payload_last = io_matAfterActivations_0_payload_Final;
  assign matAfterActivations_0_valid = (io_matAfterActivations_0_valid && ((io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID == slicedInstReg_UID) || lock));
  assign io_matAfterActivations_0_ready = (matAfterActivations_0_ready && ((io_matAfterActivations_0_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID == slicedInstReg_UID) || lock));
  assign matAfterActivations_1_payload_fragment_Activation_x_0 = io_matAfterActivations_1_payload_Activation_x_0;
  assign matAfterActivations_1_payload_fragment_Activation_x_1 = io_matAfterActivations_1_payload_Activation_x_1;
  assign matAfterActivations_1_payload_fragment_Activation_x_2 = io_matAfterActivations_1_payload_Activation_x_2;
  assign matAfterActivations_1_payload_fragment_Activation_x_3 = io_matAfterActivations_1_payload_Activation_x_3;
  assign matAfterActivations_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID = io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID;
  assign matAfterActivations_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt = io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
  assign matAfterActivations_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt = io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
  assign matAfterActivations_1_payload_last = io_matAfterActivations_1_payload_Final;
  assign matAfterActivations_1_valid = (io_matAfterActivations_1_valid && ((io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID == slicedInstReg_UID) || lock));
  assign io_matAfterActivations_1_ready = (matAfterActivations_1_ready && ((io_matAfterActivations_1_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID == slicedInstReg_UID) || lock));
  assign matAfterActivations_2_payload_fragment_Activation_x_0 = io_matAfterActivations_2_payload_Activation_x_0;
  assign matAfterActivations_2_payload_fragment_Activation_x_1 = io_matAfterActivations_2_payload_Activation_x_1;
  assign matAfterActivations_2_payload_fragment_Activation_x_2 = io_matAfterActivations_2_payload_Activation_x_2;
  assign matAfterActivations_2_payload_fragment_Activation_x_3 = io_matAfterActivations_2_payload_Activation_x_3;
  assign matAfterActivations_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID = io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID;
  assign matAfterActivations_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt = io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
  assign matAfterActivations_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt = io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
  assign matAfterActivations_2_payload_last = io_matAfterActivations_2_payload_Final;
  assign matAfterActivations_2_valid = (io_matAfterActivations_2_valid && ((io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID == slicedInstReg_UID) || lock));
  assign io_matAfterActivations_2_ready = (matAfterActivations_2_ready && ((io_matAfterActivations_2_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID == slicedInstReg_UID) || lock));
  assign matAfterActivations_0_ready = streamArbiter_1_io_inputs_0_ready;
  assign matAfterActivations_1_ready = streamArbiter_1_io_inputs_1_ready;
  assign matAfterActivations_2_ready = streamArbiter_1_io_inputs_2_ready;
  assign matAfterActivation_valid = streamArbiter_1_io_output_valid;
  assign matAfterActivation_payload_Activation_x_0 = streamArbiter_1_io_output_payload_fragment_Activation_x_0;
  assign matAfterActivation_payload_Activation_x_1 = streamArbiter_1_io_output_payload_fragment_Activation_x_1;
  assign matAfterActivation_payload_Activation_x_2 = streamArbiter_1_io_output_payload_fragment_Activation_x_2;
  assign matAfterActivation_payload_Activation_x_3 = streamArbiter_1_io_output_payload_fragment_Activation_x_3;
  assign matAfterActivation_payload_CoreInstruction_AfterActivation_Collector_Instruction_UID = streamArbiter_1_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID;
  assign matAfterActivation_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt = streamArbiter_1_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
  assign matAfterActivation_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt = streamArbiter_1_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
  assign matAfterActivation_payload_Final = streamArbiter_1_io_output_payload_last;
  assign matZSubReceiveRowCnt_willOverflowIfInc = (matZSubReceiveRowCnt_cnt == 4'b1000);
  assign matZSubReceiveRowCnt_willOverflow = (matAfterActivation_fire && matZSubReceiveRowCnt_willOverflowIfInc);
  assign matZSubReceiveRowCnt_valueNext = (matZSubReceiveRowCnt_willOverflowIfInc ? 4'b0000 : _zz_matZSubReceiveRowCnt_valueNext);
  assign when_Collector_l102 = (slicedInst_fire || (matZsubWriteFinish && (! instFinish)));
  assign when_Collector_l108 = (matAfterActivation_fire && (matZSubReceiveRowCnt_cnt == 4'b0000));
  assign when_Collector_l112 = (matAfterActivation_fire && (matZSubReceiveRowCnt_cnt == 4'b0000));
  assign _zz_1 = ({15'd0,1'b1} <<< matZSubReceiveRowCnt_cnt);
  assign _zz_2 = _zz_1[0];
  assign _zz_3 = _zz_1[1];
  assign _zz_4 = _zz_1[2];
  assign _zz_5 = _zz_1[3];
  assign _zz_6 = _zz_1[4];
  assign _zz_7 = _zz_1[5];
  assign _zz_8 = _zz_1[6];
  assign _zz_9 = _zz_1[7];
  assign _zz_10 = _zz_1[8];
  assign matZSubWriteRowCnt_willOverflowIfInc = (matZSubWriteRowCnt_cnt == 3'b101);
  assign matZSubWriteRowCnt_willOverflow = (io_memoryWritePort_Valid && matZSubWriteRowCnt_willOverflowIfInc);
  assign matZSubWriteRowCnt_valueNext = (matZSubWriteRowCnt_willOverflowIfInc ? 3'b000 : _zz_matZSubWriteRowCnt_valueNext);
  assign matZsubWriteFinish = matZSubWriteRowCnt_willOverflow;
  assign io_memoryWritePort_Valid = ((matZSubWriteRowCnt_cnt == 3'b000) ? matZSubReceiveRowCnt_willOverflow : 1'b1);
  assign io_memoryWritePort_Address = (_zz_io_memoryWritePort_Address + _zz_io_memoryWritePort_Address_7);
  assign _zz_io_memoryWritePort_Data = {{matZsub_8_3,{matZsub_8_2,{matZsub_8_1,matZsub_8_0}}},{{matZsub_7_3,{matZsub_7_2,{matZsub_7_1,matZsub_7_0}}},{{matZsub_6_3,{matZsub_6_2,{_zz__zz_io_memoryWritePort_Data,_zz__zz_io_memoryWritePort_Data_1}}},{{matZsub_5_3,{_zz__zz_io_memoryWritePort_Data_2,_zz__zz_io_memoryWritePort_Data_3}},{{_zz__zz_io_memoryWritePort_Data_4,_zz__zz_io_memoryWritePort_Data_5},{_zz__zz_io_memoryWritePort_Data_6,_zz__zz_io_memoryWritePort_Data_7}}}}}};
  assign io_memoryWritePort_Data = _zz_io_memoryWritePort_Data_1;
  assign matZSliceCnt_willOverflowIfInc = (matZSliceCnt_cnt == _zz_matZSliceCnt_willOverflowIfInc);
  assign matZSliceCnt_willOverflow = (matZsubWriteFinish && matZSliceCnt_willOverflowIfInc);
  assign matZSliceCnt_valueNext = (matZSliceCnt_willOverflowIfInc ? 30'h0 : _zz_matZSliceCnt_valueNext);
  assign instFinish = matZSliceCnt_willOverflow;
  assign io_instFinish = instFinish_regNext;
  always @(posedge clk or posedge reset) begin
    if(reset) begin
      slicedInst_ready <= 1'b1;
      lock <= 1'b0;
      matAfterActivation_ready <= 1'b0;
      instFinish_regNext <= 1'b0;
    end else begin
      if(slicedInst_fire) begin
        slicedInst_ready <= 1'b0;
      end else begin
        if(instFinish) begin
          slicedInst_ready <= 1'b1;
        end
      end
      if(matAfterActivation_fire) begin
        lock <= (! matAfterActivation_payload_Final);
      end
      if(when_Collector_l102) begin
        matAfterActivation_ready <= 1'b1;
      end else begin
        if(matZSubReceiveRowCnt_willOverflow) begin
          matAfterActivation_ready <= 1'b0;
        end
      end
      instFinish_regNext <= instFinish;
    end
  end

  always @(posedge clk) begin
    if(slicedInst_fire) begin
      slicedInstReg_UID <= slicedInst_payload_UID;
      slicedInstReg_doTranspose <= slicedInst_payload_doTranspose;
      slicedInstReg_outputAddress <= slicedInst_payload_outputAddress;
      slicedInstReg_outputShape_0 <= slicedInst_payload_outputShape_0;
      slicedInstReg_outputShape_1 <= slicedInst_payload_outputShape_1;
    end
    if(slicedInst_fire) begin
      matZSubReceiveRowCnt_cnt <= 4'b0000;
    end else begin
      if(matAfterActivation_fire) begin
        matZSubReceiveRowCnt_cnt <= matZSubReceiveRowCnt_valueNext;
      end
    end
    if(when_Collector_l108) begin
      matARowSliceCnt <= matAfterActivation_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
    end
    if(when_Collector_l112) begin
      matBColSliceCnt <= matAfterActivation_payload_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
    end
    if(matAfterActivation_fire) begin
      if(_zz_2) begin
        matZsub_0_0 <= matAfterActivation_payload_Activation_x_0;
      end
      if(_zz_3) begin
        matZsub_1_0 <= matAfterActivation_payload_Activation_x_0;
      end
      if(_zz_4) begin
        matZsub_2_0 <= matAfterActivation_payload_Activation_x_0;
      end
      if(_zz_5) begin
        matZsub_3_0 <= matAfterActivation_payload_Activation_x_0;
      end
      if(_zz_6) begin
        matZsub_4_0 <= matAfterActivation_payload_Activation_x_0;
      end
      if(_zz_7) begin
        matZsub_5_0 <= matAfterActivation_payload_Activation_x_0;
      end
      if(_zz_8) begin
        matZsub_6_0 <= matAfterActivation_payload_Activation_x_0;
      end
      if(_zz_9) begin
        matZsub_7_0 <= matAfterActivation_payload_Activation_x_0;
      end
      if(_zz_10) begin
        matZsub_8_0 <= matAfterActivation_payload_Activation_x_0;
      end
      if(_zz_2) begin
        matZsub_0_1 <= matAfterActivation_payload_Activation_x_1;
      end
      if(_zz_3) begin
        matZsub_1_1 <= matAfterActivation_payload_Activation_x_1;
      end
      if(_zz_4) begin
        matZsub_2_1 <= matAfterActivation_payload_Activation_x_1;
      end
      if(_zz_5) begin
        matZsub_3_1 <= matAfterActivation_payload_Activation_x_1;
      end
      if(_zz_6) begin
        matZsub_4_1 <= matAfterActivation_payload_Activation_x_1;
      end
      if(_zz_7) begin
        matZsub_5_1 <= matAfterActivation_payload_Activation_x_1;
      end
      if(_zz_8) begin
        matZsub_6_1 <= matAfterActivation_payload_Activation_x_1;
      end
      if(_zz_9) begin
        matZsub_7_1 <= matAfterActivation_payload_Activation_x_1;
      end
      if(_zz_10) begin
        matZsub_8_1 <= matAfterActivation_payload_Activation_x_1;
      end
      if(_zz_2) begin
        matZsub_0_2 <= matAfterActivation_payload_Activation_x_2;
      end
      if(_zz_3) begin
        matZsub_1_2 <= matAfterActivation_payload_Activation_x_2;
      end
      if(_zz_4) begin
        matZsub_2_2 <= matAfterActivation_payload_Activation_x_2;
      end
      if(_zz_5) begin
        matZsub_3_2 <= matAfterActivation_payload_Activation_x_2;
      end
      if(_zz_6) begin
        matZsub_4_2 <= matAfterActivation_payload_Activation_x_2;
      end
      if(_zz_7) begin
        matZsub_5_2 <= matAfterActivation_payload_Activation_x_2;
      end
      if(_zz_8) begin
        matZsub_6_2 <= matAfterActivation_payload_Activation_x_2;
      end
      if(_zz_9) begin
        matZsub_7_2 <= matAfterActivation_payload_Activation_x_2;
      end
      if(_zz_10) begin
        matZsub_8_2 <= matAfterActivation_payload_Activation_x_2;
      end
      if(_zz_2) begin
        matZsub_0_3 <= matAfterActivation_payload_Activation_x_3;
      end
      if(_zz_3) begin
        matZsub_1_3 <= matAfterActivation_payload_Activation_x_3;
      end
      if(_zz_4) begin
        matZsub_2_3 <= matAfterActivation_payload_Activation_x_3;
      end
      if(_zz_5) begin
        matZsub_3_3 <= matAfterActivation_payload_Activation_x_3;
      end
      if(_zz_6) begin
        matZsub_4_3 <= matAfterActivation_payload_Activation_x_3;
      end
      if(_zz_7) begin
        matZsub_5_3 <= matAfterActivation_payload_Activation_x_3;
      end
      if(_zz_8) begin
        matZsub_6_3 <= matAfterActivation_payload_Activation_x_3;
      end
      if(_zz_9) begin
        matZsub_7_3 <= matAfterActivation_payload_Activation_x_3;
      end
      if(_zz_10) begin
        matZsub_8_3 <= matAfterActivation_payload_Activation_x_3;
      end
    end
    if(slicedInst_fire) begin
      matZSubWriteRowCnt_cnt <= 3'b000;
    end else begin
      if(io_memoryWritePort_Valid) begin
        matZSubWriteRowCnt_cnt <= matZSubWriteRowCnt_valueNext;
      end
    end
    if(slicedInst_fire) begin
      matZSliceCnt_cnt <= 30'h0;
    end else begin
      if(matZsubWriteFinish) begin
        matZSliceCnt_cnt <= matZSliceCnt_valueNext;
      end
    end
  end


endmodule

module Sdpram (
  input  wire          io_read_clk,
  input  wire          io_read_Valid,
  input  wire [16:0]   io_read_Address,
  output wire [53:0]   io_read_Data,
  input  wire          io_write_clk,
  input  wire [5:0]    io_write_Wen,
  input  wire          io_write_Valid,
  input  wire [16:0]   io_write_Address,
  input  wire [53:0]   io_write_Data,
  input  wire          clk,
  input  wire          reset
);

  reg        [53:0]   mem_spinal_port0;
  reg [53:0] mem [0:131071];

  always @(posedge clk) begin
    if(io_read_Valid) begin
      mem_spinal_port0 <= mem[io_read_Address];
    end
  end

  always @(posedge clk) begin
    if(io_write_Valid) begin
      mem[io_write_Address] <= io_write_Data;
    end
  end

  assign io_read_Data = mem_spinal_port0;

endmodule

module StreamArbiter (
  input  wire          io_inputs_0_valid,
  output wire          io_inputs_0_ready,
  input  wire          io_inputs_0_payload_last,
  input  wire [8:0]    io_inputs_0_payload_fragment_Activation_x_0,
  input  wire [8:0]    io_inputs_0_payload_fragment_Activation_x_1,
  input  wire [8:0]    io_inputs_0_payload_fragment_Activation_x_2,
  input  wire [8:0]    io_inputs_0_payload_fragment_Activation_x_3,
  input  wire [18:0]   io_inputs_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID,
  input  wire [12:0]   io_inputs_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt,
  input  wire [12:0]   io_inputs_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt,
  input  wire          io_inputs_1_valid,
  output wire          io_inputs_1_ready,
  input  wire          io_inputs_1_payload_last,
  input  wire [8:0]    io_inputs_1_payload_fragment_Activation_x_0,
  input  wire [8:0]    io_inputs_1_payload_fragment_Activation_x_1,
  input  wire [8:0]    io_inputs_1_payload_fragment_Activation_x_2,
  input  wire [8:0]    io_inputs_1_payload_fragment_Activation_x_3,
  input  wire [18:0]   io_inputs_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID,
  input  wire [12:0]   io_inputs_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt,
  input  wire [12:0]   io_inputs_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt,
  input  wire          io_inputs_2_valid,
  output wire          io_inputs_2_ready,
  input  wire          io_inputs_2_payload_last,
  input  wire [8:0]    io_inputs_2_payload_fragment_Activation_x_0,
  input  wire [8:0]    io_inputs_2_payload_fragment_Activation_x_1,
  input  wire [8:0]    io_inputs_2_payload_fragment_Activation_x_2,
  input  wire [8:0]    io_inputs_2_payload_fragment_Activation_x_3,
  input  wire [18:0]   io_inputs_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID,
  input  wire [12:0]   io_inputs_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt,
  input  wire [12:0]   io_inputs_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt,
  output wire          io_output_valid,
  input  wire          io_output_ready,
  output wire          io_output_payload_last,
  output wire [8:0]    io_output_payload_fragment_Activation_x_0,
  output wire [8:0]    io_output_payload_fragment_Activation_x_1,
  output wire [8:0]    io_output_payload_fragment_Activation_x_2,
  output wire [8:0]    io_output_payload_fragment_Activation_x_3,
  output wire [18:0]   io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID,
  output wire [12:0]   io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt,
  output wire [12:0]   io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt,
  output wire [1:0]    io_chosen,
  output wire [2:0]    io_chosenOH,
  input  wire          clk,
  input  wire          reset
);

  wire       [5:0]    _zz__zz_maskProposal_0_2;
  wire       [5:0]    _zz__zz_maskProposal_0_2_1;
  wire       [2:0]    _zz__zz_maskProposal_0_2_2;
  reg                 _zz_io_output_payload_last_1;
  reg        [8:0]    _zz_io_output_payload_fragment_Activation_x_0;
  reg        [8:0]    _zz_io_output_payload_fragment_Activation_x_1;
  reg        [8:0]    _zz_io_output_payload_fragment_Activation_x_2;
  reg        [8:0]    _zz_io_output_payload_fragment_Activation_x_3;
  reg        [18:0]   _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID;
  reg        [12:0]   _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
  reg        [12:0]   _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
  reg                 locked;
  wire                maskProposal_0;
  wire                maskProposal_1;
  wire                maskProposal_2;
  reg                 maskLocked_0;
  reg                 maskLocked_1;
  reg                 maskLocked_2;
  wire                maskRouted_0;
  wire                maskRouted_1;
  wire                maskRouted_2;
  wire       [2:0]    _zz_maskProposal_0;
  wire       [5:0]    _zz_maskProposal_0_1;
  wire       [5:0]    _zz_maskProposal_0_2;
  wire       [2:0]    _zz_maskProposal_0_3;
  wire                io_output_fire;
  wire                when_Stream_l704;
  wire       [1:0]    _zz_io_output_payload_last;
  wire                _zz_io_chosen;
  wire                _zz_io_chosen_1;

  assign _zz__zz_maskProposal_0_2 = (_zz_maskProposal_0_1 - _zz__zz_maskProposal_0_2_1);
  assign _zz__zz_maskProposal_0_2_2 = {maskLocked_1,{maskLocked_0,maskLocked_2}};
  assign _zz__zz_maskProposal_0_2_1 = {3'd0, _zz__zz_maskProposal_0_2_2};
  always @(*) begin
    case(_zz_io_output_payload_last)
      2'b00 : begin
        _zz_io_output_payload_last_1 = io_inputs_0_payload_last;
        _zz_io_output_payload_fragment_Activation_x_0 = io_inputs_0_payload_fragment_Activation_x_0;
        _zz_io_output_payload_fragment_Activation_x_1 = io_inputs_0_payload_fragment_Activation_x_1;
        _zz_io_output_payload_fragment_Activation_x_2 = io_inputs_0_payload_fragment_Activation_x_2;
        _zz_io_output_payload_fragment_Activation_x_3 = io_inputs_0_payload_fragment_Activation_x_3;
        _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID = io_inputs_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID;
        _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt = io_inputs_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
        _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt = io_inputs_0_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
      end
      2'b01 : begin
        _zz_io_output_payload_last_1 = io_inputs_1_payload_last;
        _zz_io_output_payload_fragment_Activation_x_0 = io_inputs_1_payload_fragment_Activation_x_0;
        _zz_io_output_payload_fragment_Activation_x_1 = io_inputs_1_payload_fragment_Activation_x_1;
        _zz_io_output_payload_fragment_Activation_x_2 = io_inputs_1_payload_fragment_Activation_x_2;
        _zz_io_output_payload_fragment_Activation_x_3 = io_inputs_1_payload_fragment_Activation_x_3;
        _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID = io_inputs_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID;
        _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt = io_inputs_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
        _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt = io_inputs_1_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
      end
      default : begin
        _zz_io_output_payload_last_1 = io_inputs_2_payload_last;
        _zz_io_output_payload_fragment_Activation_x_0 = io_inputs_2_payload_fragment_Activation_x_0;
        _zz_io_output_payload_fragment_Activation_x_1 = io_inputs_2_payload_fragment_Activation_x_1;
        _zz_io_output_payload_fragment_Activation_x_2 = io_inputs_2_payload_fragment_Activation_x_2;
        _zz_io_output_payload_fragment_Activation_x_3 = io_inputs_2_payload_fragment_Activation_x_3;
        _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID = io_inputs_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID;
        _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt = io_inputs_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
        _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt = io_inputs_2_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
      end
    endcase
  end

  assign maskRouted_0 = (locked ? maskLocked_0 : maskProposal_0);
  assign maskRouted_1 = (locked ? maskLocked_1 : maskProposal_1);
  assign maskRouted_2 = (locked ? maskLocked_2 : maskProposal_2);
  assign _zz_maskProposal_0 = {io_inputs_2_valid,{io_inputs_1_valid,io_inputs_0_valid}};
  assign _zz_maskProposal_0_1 = {_zz_maskProposal_0,_zz_maskProposal_0};
  assign _zz_maskProposal_0_2 = (_zz_maskProposal_0_1 & (~ _zz__zz_maskProposal_0_2));
  assign _zz_maskProposal_0_3 = (_zz_maskProposal_0_2[5 : 3] | _zz_maskProposal_0_2[2 : 0]);
  assign maskProposal_0 = _zz_maskProposal_0_3[0];
  assign maskProposal_1 = _zz_maskProposal_0_3[1];
  assign maskProposal_2 = _zz_maskProposal_0_3[2];
  assign io_output_fire = (io_output_valid && io_output_ready);
  assign when_Stream_l704 = (io_output_fire && io_output_payload_last);
  assign io_output_valid = (((io_inputs_0_valid && maskRouted_0) || (io_inputs_1_valid && maskRouted_1)) || (io_inputs_2_valid && maskRouted_2));
  assign _zz_io_output_payload_last = {maskRouted_2,maskRouted_1};
  assign io_output_payload_last = _zz_io_output_payload_last_1;
  assign io_output_payload_fragment_Activation_x_0 = _zz_io_output_payload_fragment_Activation_x_0;
  assign io_output_payload_fragment_Activation_x_1 = _zz_io_output_payload_fragment_Activation_x_1;
  assign io_output_payload_fragment_Activation_x_2 = _zz_io_output_payload_fragment_Activation_x_2;
  assign io_output_payload_fragment_Activation_x_3 = _zz_io_output_payload_fragment_Activation_x_3;
  assign io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID = _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_UID;
  assign io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt = _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatA_row_slice_cnt;
  assign io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt = _zz_io_output_payload_fragment_CoreInstruction_AfterActivation_Collector_Instruction_MatB_col_slice_cnt;
  assign io_inputs_0_ready = (maskRouted_0 && io_output_ready);
  assign io_inputs_1_ready = (maskRouted_1 && io_output_ready);
  assign io_inputs_2_ready = (maskRouted_2 && io_output_ready);
  assign io_chosenOH = {maskRouted_2,{maskRouted_1,maskRouted_0}};
  assign _zz_io_chosen = io_chosenOH[1];
  assign _zz_io_chosen_1 = io_chosenOH[2];
  assign io_chosen = {_zz_io_chosen_1,_zz_io_chosen};
  always @(posedge clk or posedge reset) begin
    if(reset) begin
      locked <= 1'b0;
      maskLocked_0 <= 1'b0;
      maskLocked_1 <= 1'b0;
      maskLocked_2 <= 1'b1;
    end else begin
      if(io_output_valid) begin
        maskLocked_0 <= maskRouted_0;
        maskLocked_1 <= maskRouted_1;
        maskLocked_2 <= maskRouted_2;
      end
      if(io_output_valid) begin
        locked <= 1'b1;
      end
      if(when_Stream_l704) begin
        locked <= 1'b0;
      end
    end
  end


endmodule

module StreamFifo (
  input  wire          io_push_valid,
  output wire          io_push_ready,
  input  wire [18:0]   io_push_payload_UID,
  input  wire          io_push_payload_doTranspose,
  input  wire [16:0]   io_push_payload_outputAddress,
  input  wire [14:0]   io_push_payload_outputShape_0,
  input  wire [14:0]   io_push_payload_outputShape_1,
  output wire          io_pop_valid,
  input  wire          io_pop_ready,
  output wire [18:0]   io_pop_payload_UID,
  output wire          io_pop_payload_doTranspose,
  output wire [16:0]   io_pop_payload_outputAddress,
  output wire [14:0]   io_pop_payload_outputShape_0,
  output wire [14:0]   io_pop_payload_outputShape_1,
  input  wire          io_flush,
  output wire [5:0]    io_occupancy,
  output wire [5:0]    io_availability,
  input  wire          clk,
  input  wire          reset
);

  reg        [66:0]   logic_ram_spinal_port1;
  wire       [5:0]    _zz_logic_ptr_notPow2_counter;
  wire       [5:0]    _zz_logic_ptr_notPow2_counter_1;
  wire       [0:0]    _zz_logic_ptr_notPow2_counter_2;
  wire       [5:0]    _zz_logic_ptr_notPow2_counter_3;
  wire       [0:0]    _zz_logic_ptr_notPow2_counter_4;
  wire       [66:0]   _zz_logic_ram_port;
  reg                 _zz_1;
  wire                logic_ptr_doPush;
  wire                logic_ptr_doPop;
  wire                logic_ptr_full;
  wire                logic_ptr_empty;
  reg        [5:0]    logic_ptr_push;
  reg        [5:0]    logic_ptr_pop;
  wire       [5:0]    logic_ptr_occupancy;
  wire       [5:0]    logic_ptr_popOnIo;
  wire                when_Stream_l1273;
  reg                 logic_ptr_wentUp;
  wire                when_Stream_l1308;
  wire                when_Stream_l1312;
  reg        [5:0]    logic_ptr_notPow2_counter;
  wire                io_push_fire;
  wire                io_pop_fire;
  wire                logic_push_onRam_write_valid;
  wire       [5:0]    logic_push_onRam_write_payload_address;
  wire       [18:0]   logic_push_onRam_write_payload_data_UID;
  wire                logic_push_onRam_write_payload_data_doTranspose;
  wire       [16:0]   logic_push_onRam_write_payload_data_outputAddress;
  wire       [14:0]   logic_push_onRam_write_payload_data_outputShape_0;
  wire       [14:0]   logic_push_onRam_write_payload_data_outputShape_1;
  wire                logic_pop_addressGen_valid;
  reg                 logic_pop_addressGen_ready;
  wire       [5:0]    logic_pop_addressGen_payload;
  wire                logic_pop_addressGen_fire;
  wire                logic_pop_sync_readArbitation_valid;
  wire                logic_pop_sync_readArbitation_ready;
  wire       [5:0]    logic_pop_sync_readArbitation_payload;
  reg                 logic_pop_addressGen_rValid;
  reg        [5:0]    logic_pop_addressGen_rData;
  wire                when_Stream_l393;
  wire                logic_pop_sync_readPort_cmd_valid;
  wire       [5:0]    logic_pop_sync_readPort_cmd_payload;
  wire       [18:0]   logic_pop_sync_readPort_rsp_UID;
  wire                logic_pop_sync_readPort_rsp_doTranspose;
  wire       [16:0]   logic_pop_sync_readPort_rsp_outputAddress;
  wire       [14:0]   logic_pop_sync_readPort_rsp_outputShape_0;
  wire       [14:0]   logic_pop_sync_readPort_rsp_outputShape_1;
  wire       [66:0]   _zz_logic_pop_sync_readPort_rsp_UID;
  wire       [29:0]   _zz_logic_pop_sync_readPort_rsp_outputShape_0;
  wire                logic_pop_addressGen_toFlowFire_valid;
  wire       [5:0]    logic_pop_addressGen_toFlowFire_payload;
  wire                logic_pop_sync_readArbitation_translated_valid;
  wire                logic_pop_sync_readArbitation_translated_ready;
  wire       [18:0]   logic_pop_sync_readArbitation_translated_payload_UID;
  wire                logic_pop_sync_readArbitation_translated_payload_doTranspose;
  wire       [16:0]   logic_pop_sync_readArbitation_translated_payload_outputAddress;
  wire       [14:0]   logic_pop_sync_readArbitation_translated_payload_outputShape_0;
  wire       [14:0]   logic_pop_sync_readArbitation_translated_payload_outputShape_1;
  wire                logic_pop_sync_readArbitation_fire;
  reg        [5:0]    logic_pop_sync_popReg;
  reg [66:0] logic_ram [0:32];

  assign _zz_logic_ptr_notPow2_counter = (logic_ptr_notPow2_counter + _zz_logic_ptr_notPow2_counter_1);
  assign _zz_logic_ptr_notPow2_counter_2 = io_push_fire;
  assign _zz_logic_ptr_notPow2_counter_1 = {5'd0, _zz_logic_ptr_notPow2_counter_2};
  assign _zz_logic_ptr_notPow2_counter_4 = io_pop_fire;
  assign _zz_logic_ptr_notPow2_counter_3 = {5'd0, _zz_logic_ptr_notPow2_counter_4};
  assign _zz_logic_ram_port = {{logic_push_onRam_write_payload_data_outputShape_1,logic_push_onRam_write_payload_data_outputShape_0},{logic_push_onRam_write_payload_data_outputAddress,{logic_push_onRam_write_payload_data_doTranspose,logic_push_onRam_write_payload_data_UID}}};
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
  assign logic_ptr_full = ((logic_ptr_push == logic_ptr_popOnIo) && logic_ptr_wentUp);
  assign logic_ptr_empty = ((logic_ptr_push == logic_ptr_pop) && (! logic_ptr_wentUp));
  assign when_Stream_l1308 = (logic_ptr_push == 6'h20);
  assign when_Stream_l1312 = (logic_ptr_pop == 6'h20);
  assign io_push_fire = (io_push_valid && io_push_ready);
  assign io_pop_fire = (io_pop_valid && io_pop_ready);
  assign logic_ptr_occupancy = logic_ptr_notPow2_counter;
  assign io_push_ready = (! logic_ptr_full);
  assign logic_ptr_doPush = io_push_fire;
  assign logic_push_onRam_write_valid = io_push_fire;
  assign logic_push_onRam_write_payload_address = logic_ptr_push;
  assign logic_push_onRam_write_payload_data_UID = io_push_payload_UID;
  assign logic_push_onRam_write_payload_data_doTranspose = io_push_payload_doTranspose;
  assign logic_push_onRam_write_payload_data_outputAddress = io_push_payload_outputAddress;
  assign logic_push_onRam_write_payload_data_outputShape_0 = io_push_payload_outputShape_0;
  assign logic_push_onRam_write_payload_data_outputShape_1 = io_push_payload_outputShape_1;
  assign logic_pop_addressGen_valid = (! logic_ptr_empty);
  assign logic_pop_addressGen_payload = logic_ptr_pop;
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
  assign _zz_logic_pop_sync_readPort_rsp_UID = logic_ram_spinal_port1;
  assign _zz_logic_pop_sync_readPort_rsp_outputShape_0 = _zz_logic_pop_sync_readPort_rsp_UID[66 : 37];
  assign logic_pop_sync_readPort_rsp_UID = _zz_logic_pop_sync_readPort_rsp_UID[18 : 0];
  assign logic_pop_sync_readPort_rsp_doTranspose = _zz_logic_pop_sync_readPort_rsp_UID[19];
  assign logic_pop_sync_readPort_rsp_outputAddress = _zz_logic_pop_sync_readPort_rsp_UID[36 : 20];
  assign logic_pop_sync_readPort_rsp_outputShape_0 = _zz_logic_pop_sync_readPort_rsp_outputShape_0[14 : 0];
  assign logic_pop_sync_readPort_rsp_outputShape_1 = _zz_logic_pop_sync_readPort_rsp_outputShape_0[29 : 15];
  assign logic_pop_addressGen_toFlowFire_valid = logic_pop_addressGen_fire;
  assign logic_pop_addressGen_toFlowFire_payload = logic_pop_addressGen_payload;
  assign logic_pop_sync_readPort_cmd_valid = logic_pop_addressGen_toFlowFire_valid;
  assign logic_pop_sync_readPort_cmd_payload = logic_pop_addressGen_toFlowFire_payload;
  assign logic_pop_sync_readArbitation_translated_valid = logic_pop_sync_readArbitation_valid;
  assign logic_pop_sync_readArbitation_ready = logic_pop_sync_readArbitation_translated_ready;
  assign logic_pop_sync_readArbitation_translated_payload_UID = logic_pop_sync_readPort_rsp_UID;
  assign logic_pop_sync_readArbitation_translated_payload_doTranspose = logic_pop_sync_readPort_rsp_doTranspose;
  assign logic_pop_sync_readArbitation_translated_payload_outputAddress = logic_pop_sync_readPort_rsp_outputAddress;
  assign logic_pop_sync_readArbitation_translated_payload_outputShape_0 = logic_pop_sync_readPort_rsp_outputShape_0;
  assign logic_pop_sync_readArbitation_translated_payload_outputShape_1 = logic_pop_sync_readPort_rsp_outputShape_1;
  assign io_pop_valid = logic_pop_sync_readArbitation_translated_valid;
  assign logic_pop_sync_readArbitation_translated_ready = io_pop_ready;
  assign io_pop_payload_UID = logic_pop_sync_readArbitation_translated_payload_UID;
  assign io_pop_payload_doTranspose = logic_pop_sync_readArbitation_translated_payload_doTranspose;
  assign io_pop_payload_outputAddress = logic_pop_sync_readArbitation_translated_payload_outputAddress;
  assign io_pop_payload_outputShape_0 = logic_pop_sync_readArbitation_translated_payload_outputShape_0;
  assign io_pop_payload_outputShape_1 = logic_pop_sync_readArbitation_translated_payload_outputShape_1;
  assign logic_pop_sync_readArbitation_fire = (logic_pop_sync_readArbitation_valid && logic_pop_sync_readArbitation_ready);
  assign logic_ptr_popOnIo = logic_pop_sync_popReg;
  assign io_occupancy = logic_ptr_occupancy;
  assign io_availability = (6'h21 - logic_ptr_occupancy);
  always @(posedge clk or posedge reset) begin
    if(reset) begin
      logic_ptr_push <= 6'h0;
      logic_ptr_pop <= 6'h0;
      logic_ptr_wentUp <= 1'b0;
      logic_ptr_notPow2_counter <= 6'h0;
      logic_pop_addressGen_rValid <= 1'b0;
      logic_pop_sync_popReg <= 6'h0;
    end else begin
      if(when_Stream_l1273) begin
        logic_ptr_wentUp <= logic_ptr_doPush;
      end
      if(io_flush) begin
        logic_ptr_wentUp <= 1'b0;
      end
      if(logic_ptr_doPush) begin
        logic_ptr_push <= (logic_ptr_push + 6'h01);
        if(when_Stream_l1308) begin
          logic_ptr_push <= 6'h0;
        end
      end
      if(logic_ptr_doPop) begin
        logic_ptr_pop <= (logic_ptr_pop + 6'h01);
        if(when_Stream_l1312) begin
          logic_ptr_pop <= 6'h0;
        end
      end
      if(io_flush) begin
        logic_ptr_push <= 6'h0;
        logic_ptr_pop <= 6'h0;
      end
      logic_ptr_notPow2_counter <= (_zz_logic_ptr_notPow2_counter - _zz_logic_ptr_notPow2_counter_3);
      if(io_flush) begin
        logic_ptr_notPow2_counter <= 6'h0;
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
        logic_pop_sync_popReg <= 6'h0;
      end
    end
  end

  always @(posedge clk) begin
    if(logic_pop_addressGen_ready) begin
      logic_pop_addressGen_rData <= logic_pop_addressGen_payload;
    end
  end


endmodule

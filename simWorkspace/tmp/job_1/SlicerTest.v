// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : SlicerTest
// Git hash  : 033f13503c7527489599352f8721a18e89b6d190

`timescale 1ns/1ps

module SlicerTest (
  input  wire          io_inst_valid,
  output wire          io_inst_ready,
  input  wire [18:0]   io_inst_payload_UID,
  input  wire [1:0]    io_inst_payload_matrixOperation,
  input  wire [5:0]    io_inst_payload_shiftLeft_AfterMatrixOperation,
  input  wire          io_inst_payload_doTranspose,
  input  wire [2:0]    io_inst_payload_activationFunction,
  input  wire [5:0]    io_inst_payload_shiftLeft_AfterActivation,
  input  wire [16:0]   io_inst_payload_input0Address,
  input  wire [14:0]   io_inst_payload_input0Shape_0,
  input  wire [14:0]   io_inst_payload_input0Shape_1,
  input  wire [16:0]   io_inst_payload_input1Address,
  input  wire [14:0]   io_inst_payload_input1Shape_0,
  input  wire [14:0]   io_inst_payload_input1Shape_1,
  input  wire [16:0]   io_inst_payload_outputAddress,
  output wire          io_slicedInst_valid,
  input  wire          io_slicedInst_ready,
  output wire [18:0]   io_slicedInst_payload_UID,
  output wire          io_slicedInst_payload_doTranspose,
  output wire [16:0]   io_slicedInst_payload_outputAddress,
  output wire [14:0]   io_slicedInst_payload_outputShape_0,
  output wire [14:0]   io_slicedInst_payload_outputShape_1,
  output wire          io_Mats_to_Cores_Streams_0_valid,
  input  wire          io_Mats_to_Cores_Streams_0_ready,
  output wire [8:0]    io_Mats_to_Cores_Streams_0_payload_A_0,
  output wire [8:0]    io_Mats_to_Cores_Streams_0_payload_A_1,
  output wire [8:0]    io_Mats_to_Cores_Streams_0_payload_A_2,
  output wire [8:0]    io_Mats_to_Cores_Streams_0_payload_A_3,
  output wire [8:0]    io_Mats_to_Cores_Streams_0_payload_A_4,
  output wire [6:0]    io_Mats_to_Cores_Streams_0_payload_B_0,
  output wire [6:0]    io_Mats_to_Cores_Streams_0_payload_B_1,
  output wire [6:0]    io_Mats_to_Cores_Streams_0_payload_B_2,
  output wire [6:0]    io_Mats_to_Cores_Streams_0_payload_B_3,
  output wire [6:0]    io_Mats_to_Cores_Streams_0_payload_B_4,
  output wire [1:0]    io_Mats_to_Cores_Streams_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  output wire [5:0]    io_Mats_to_Cores_Streams_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  output wire          io_Mats_to_Cores_Streams_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  output wire [2:0]    io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Activation_Instruction_activationFunction,
  output wire [5:0]    io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  output wire [18:0]   io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Collector_Instruction_UID,
  output wire [12:0]   io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  output wire [12:0]   io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt,
  output wire          io_Mats_to_Cores_Streams_0_payload_Final,
  output wire          io_Mats_to_Cores_Streams_1_valid,
  input  wire          io_Mats_to_Cores_Streams_1_ready,
  output wire [8:0]    io_Mats_to_Cores_Streams_1_payload_A_0,
  output wire [8:0]    io_Mats_to_Cores_Streams_1_payload_A_1,
  output wire [8:0]    io_Mats_to_Cores_Streams_1_payload_A_2,
  output wire [8:0]    io_Mats_to_Cores_Streams_1_payload_A_3,
  output wire [8:0]    io_Mats_to_Cores_Streams_1_payload_A_4,
  output wire [6:0]    io_Mats_to_Cores_Streams_1_payload_B_0,
  output wire [6:0]    io_Mats_to_Cores_Streams_1_payload_B_1,
  output wire [6:0]    io_Mats_to_Cores_Streams_1_payload_B_2,
  output wire [6:0]    io_Mats_to_Cores_Streams_1_payload_B_3,
  output wire [6:0]    io_Mats_to_Cores_Streams_1_payload_B_4,
  output wire [1:0]    io_Mats_to_Cores_Streams_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  output wire [5:0]    io_Mats_to_Cores_Streams_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  output wire          io_Mats_to_Cores_Streams_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  output wire [2:0]    io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Activation_Instruction_activationFunction,
  output wire [5:0]    io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  output wire [18:0]   io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Collector_Instruction_UID,
  output wire [12:0]   io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  output wire [12:0]   io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt,
  output wire          io_Mats_to_Cores_Streams_1_payload_Final,
  output wire          io_Mats_to_Cores_Streams_2_valid,
  input  wire          io_Mats_to_Cores_Streams_2_ready,
  output wire [8:0]    io_Mats_to_Cores_Streams_2_payload_A_0,
  output wire [8:0]    io_Mats_to_Cores_Streams_2_payload_A_1,
  output wire [8:0]    io_Mats_to_Cores_Streams_2_payload_A_2,
  output wire [8:0]    io_Mats_to_Cores_Streams_2_payload_A_3,
  output wire [8:0]    io_Mats_to_Cores_Streams_2_payload_A_4,
  output wire [6:0]    io_Mats_to_Cores_Streams_2_payload_B_0,
  output wire [6:0]    io_Mats_to_Cores_Streams_2_payload_B_1,
  output wire [6:0]    io_Mats_to_Cores_Streams_2_payload_B_2,
  output wire [6:0]    io_Mats_to_Cores_Streams_2_payload_B_3,
  output wire [6:0]    io_Mats_to_Cores_Streams_2_payload_B_4,
  output wire [1:0]    io_Mats_to_Cores_Streams_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  output wire [5:0]    io_Mats_to_Cores_Streams_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  output wire          io_Mats_to_Cores_Streams_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  output wire [2:0]    io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Activation_Instruction_activationFunction,
  output wire [5:0]    io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  output wire [18:0]   io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Collector_Instruction_UID,
  output wire [12:0]   io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  output wire [12:0]   io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt,
  output wire          io_Mats_to_Cores_Streams_2_payload_Final,
  input  wire          clk,
  input  wire          reset
);
  localparam MatrixOperation_TypeDef_MatMul = 2'd0;
  localparam MatrixOperation_TypeDef_ElementAdd = 2'd1;
  localparam MatrixOperation_TypeDef_ElementMul = 2'd2;
  localparam MatrixOperation_TypeDef_ElementMax = 2'd3;
  localparam Activation_TypeDef_Exp = 3'd0;
  localparam Activation_TypeDef_Log = 3'd1;
  localparam Activation_TypeDef_Softplus = 3'd2;
  localparam Activation_TypeDef_Relu = 3'd3;
  localparam Activation_TypeDef_None = 3'd4;

  wire       [44:0]   sdpramA_io_read_Data;
  wire       [34:0]   sdpramB_io_read_Data;
  wire                slicer_1_io_inst_ready;
  wire                slicer_1_io_slicedInst_valid;
  wire       [18:0]   slicer_1_io_slicedInst_payload_UID;
  wire                slicer_1_io_slicedInst_payload_doTranspose;
  wire       [16:0]   slicer_1_io_slicedInst_payload_outputAddress;
  wire       [14:0]   slicer_1_io_slicedInst_payload_outputShape_0;
  wire       [14:0]   slicer_1_io_slicedInst_payload_outputShape_1;
  wire                slicer_1_io_memoryReadPortA_Valid;
  wire       [16:0]   slicer_1_io_memoryReadPortA_Address;
  wire                slicer_1_io_memoryReadPortB_Valid;
  wire       [16:0]   slicer_1_io_memoryReadPortB_Address;
  wire                slicer_1_io_matAfterSlicers_0_valid;
  wire       [8:0]    slicer_1_io_matAfterSlicers_0_payload_A_0;
  wire       [8:0]    slicer_1_io_matAfterSlicers_0_payload_A_1;
  wire       [8:0]    slicer_1_io_matAfterSlicers_0_payload_A_2;
  wire       [8:0]    slicer_1_io_matAfterSlicers_0_payload_A_3;
  wire       [8:0]    slicer_1_io_matAfterSlicers_0_payload_A_4;
  wire       [6:0]    slicer_1_io_matAfterSlicers_0_payload_B_0;
  wire       [6:0]    slicer_1_io_matAfterSlicers_0_payload_B_1;
  wire       [6:0]    slicer_1_io_matAfterSlicers_0_payload_B_2;
  wire       [6:0]    slicer_1_io_matAfterSlicers_0_payload_B_3;
  wire       [6:0]    slicer_1_io_matAfterSlicers_0_payload_B_4;
  wire       [1:0]    slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  wire       [5:0]    slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  wire                slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  wire       [2:0]    slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction;
  wire       [5:0]    slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  wire       [18:0]   slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_UID;
  wire       [12:0]   slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  wire                slicer_1_io_matAfterSlicers_0_payload_Final;
  wire                slicer_1_io_matAfterSlicers_1_valid;
  wire       [8:0]    slicer_1_io_matAfterSlicers_1_payload_A_0;
  wire       [8:0]    slicer_1_io_matAfterSlicers_1_payload_A_1;
  wire       [8:0]    slicer_1_io_matAfterSlicers_1_payload_A_2;
  wire       [8:0]    slicer_1_io_matAfterSlicers_1_payload_A_3;
  wire       [8:0]    slicer_1_io_matAfterSlicers_1_payload_A_4;
  wire       [6:0]    slicer_1_io_matAfterSlicers_1_payload_B_0;
  wire       [6:0]    slicer_1_io_matAfterSlicers_1_payload_B_1;
  wire       [6:0]    slicer_1_io_matAfterSlicers_1_payload_B_2;
  wire       [6:0]    slicer_1_io_matAfterSlicers_1_payload_B_3;
  wire       [6:0]    slicer_1_io_matAfterSlicers_1_payload_B_4;
  wire       [1:0]    slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  wire       [5:0]    slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  wire                slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  wire       [2:0]    slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction;
  wire       [5:0]    slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  wire       [18:0]   slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_UID;
  wire       [12:0]   slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  wire                slicer_1_io_matAfterSlicers_1_payload_Final;
  wire                slicer_1_io_matAfterSlicers_2_valid;
  wire       [8:0]    slicer_1_io_matAfterSlicers_2_payload_A_0;
  wire       [8:0]    slicer_1_io_matAfterSlicers_2_payload_A_1;
  wire       [8:0]    slicer_1_io_matAfterSlicers_2_payload_A_2;
  wire       [8:0]    slicer_1_io_matAfterSlicers_2_payload_A_3;
  wire       [8:0]    slicer_1_io_matAfterSlicers_2_payload_A_4;
  wire       [6:0]    slicer_1_io_matAfterSlicers_2_payload_B_0;
  wire       [6:0]    slicer_1_io_matAfterSlicers_2_payload_B_1;
  wire       [6:0]    slicer_1_io_matAfterSlicers_2_payload_B_2;
  wire       [6:0]    slicer_1_io_matAfterSlicers_2_payload_B_3;
  wire       [6:0]    slicer_1_io_matAfterSlicers_2_payload_B_4;
  wire       [1:0]    slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  wire       [5:0]    slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  wire                slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  wire       [2:0]    slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction;
  wire       [5:0]    slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  wire       [18:0]   slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_UID;
  wire       [12:0]   slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  wire                slicer_1_io_matAfterSlicers_2_payload_Final;
  `ifndef SYNTHESIS
  reg [79:0] io_inst_payload_matrixOperation_string;
  reg [63:0] io_inst_payload_activationFunction_string;
  reg [79:0] io_Mats_to_Cores_Streams_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] io_Mats_to_Cores_Streams_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] io_Mats_to_Cores_Streams_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string;
  `endif


  Sdpram sdpramA (
    .io_read_Valid    (slicer_1_io_memoryReadPortA_Valid        ), //i
    .io_read_Address  (slicer_1_io_memoryReadPortA_Address[16:0]), //i
    .io_read_Data     (sdpramA_io_read_Data[44:0]               ), //o
    .io_write_Valid   (1'b0                                     ), //i
    .io_write_Address (17'h0                                    ), //i
    .io_write_Data    (45'h0                                    ), //i
    .clk              (clk                                      ), //i
    .reset            (reset                                    )  //i
  );
  Sdpram_1 sdpramB (
    .io_read_Valid    (slicer_1_io_memoryReadPortB_Valid        ), //i
    .io_read_Address  (slicer_1_io_memoryReadPortB_Address[16:0]), //i
    .io_read_Data     (sdpramB_io_read_Data[34:0]               ), //o
    .io_write_Valid   (1'b0                                     ), //i
    .io_write_Address (17'h0                                    ), //i
    .io_write_Data    (35'h0                                    ), //i
    .clk              (clk                                      ), //i
    .reset            (reset                                    )  //i
  );
  Slicer slicer_1 (
    .io_inst_valid                                                                                              (io_inst_valid                                                                                                           ), //i
    .io_inst_ready                                                                                              (slicer_1_io_inst_ready                                                                                                  ), //o
    .io_inst_payload_UID                                                                                        (io_inst_payload_UID[18:0]                                                                                               ), //i
    .io_inst_payload_matrixOperation                                                                            (io_inst_payload_matrixOperation[1:0]                                                                                    ), //i
    .io_inst_payload_shiftLeft_AfterMatrixOperation                                                             (io_inst_payload_shiftLeft_AfterMatrixOperation[5:0]                                                                     ), //i
    .io_inst_payload_doTranspose                                                                                (io_inst_payload_doTranspose                                                                                             ), //i
    .io_inst_payload_activationFunction                                                                         (io_inst_payload_activationFunction[2:0]                                                                                 ), //i
    .io_inst_payload_shiftLeft_AfterActivation                                                                  (io_inst_payload_shiftLeft_AfterActivation[5:0]                                                                          ), //i
    .io_inst_payload_input0Address                                                                              (io_inst_payload_input0Address[16:0]                                                                                     ), //i
    .io_inst_payload_input0Shape_0                                                                              (io_inst_payload_input0Shape_0[14:0]                                                                                     ), //i
    .io_inst_payload_input0Shape_1                                                                              (io_inst_payload_input0Shape_1[14:0]                                                                                     ), //i
    .io_inst_payload_input1Address                                                                              (io_inst_payload_input1Address[16:0]                                                                                     ), //i
    .io_inst_payload_input1Shape_0                                                                              (io_inst_payload_input1Shape_0[14:0]                                                                                     ), //i
    .io_inst_payload_input1Shape_1                                                                              (io_inst_payload_input1Shape_1[14:0]                                                                                     ), //i
    .io_inst_payload_outputAddress                                                                              (io_inst_payload_outputAddress[16:0]                                                                                     ), //i
    .io_slicedInst_valid                                                                                        (slicer_1_io_slicedInst_valid                                                                                            ), //o
    .io_slicedInst_ready                                                                                        (io_slicedInst_ready                                                                                                     ), //i
    .io_slicedInst_payload_UID                                                                                  (slicer_1_io_slicedInst_payload_UID[18:0]                                                                                ), //o
    .io_slicedInst_payload_doTranspose                                                                          (slicer_1_io_slicedInst_payload_doTranspose                                                                              ), //o
    .io_slicedInst_payload_outputAddress                                                                        (slicer_1_io_slicedInst_payload_outputAddress[16:0]                                                                      ), //o
    .io_slicedInst_payload_outputShape_0                                                                        (slicer_1_io_slicedInst_payload_outputShape_0[14:0]                                                                      ), //o
    .io_slicedInst_payload_outputShape_1                                                                        (slicer_1_io_slicedInst_payload_outputShape_1[14:0]                                                                      ), //o
    .io_memoryReadPortA_Valid                                                                                   (slicer_1_io_memoryReadPortA_Valid                                                                                       ), //o
    .io_memoryReadPortA_Address                                                                                 (slicer_1_io_memoryReadPortA_Address[16:0]                                                                               ), //o
    .io_memoryReadPortA_Data                                                                                    (sdpramA_io_read_Data[44:0]                                                                                              ), //i
    .io_memoryReadPortB_Valid                                                                                   (slicer_1_io_memoryReadPortB_Valid                                                                                       ), //o
    .io_memoryReadPortB_Address                                                                                 (slicer_1_io_memoryReadPortB_Address[16:0]                                                                               ), //o
    .io_memoryReadPortB_Data                                                                                    (sdpramB_io_read_Data[34:0]                                                                                              ), //i
    .io_matAfterSlicers_0_valid                                                                                 (slicer_1_io_matAfterSlicers_0_valid                                                                                     ), //o
    .io_matAfterSlicers_0_ready                                                                                 (io_Mats_to_Cores_Streams_0_ready                                                                                        ), //i
    .io_matAfterSlicers_0_payload_A_0                                                                           (slicer_1_io_matAfterSlicers_0_payload_A_0[8:0]                                                                          ), //o
    .io_matAfterSlicers_0_payload_A_1                                                                           (slicer_1_io_matAfterSlicers_0_payload_A_1[8:0]                                                                          ), //o
    .io_matAfterSlicers_0_payload_A_2                                                                           (slicer_1_io_matAfterSlicers_0_payload_A_2[8:0]                                                                          ), //o
    .io_matAfterSlicers_0_payload_A_3                                                                           (slicer_1_io_matAfterSlicers_0_payload_A_3[8:0]                                                                          ), //o
    .io_matAfterSlicers_0_payload_A_4                                                                           (slicer_1_io_matAfterSlicers_0_payload_A_4[8:0]                                                                          ), //o
    .io_matAfterSlicers_0_payload_B_0                                                                           (slicer_1_io_matAfterSlicers_0_payload_B_0[6:0]                                                                          ), //o
    .io_matAfterSlicers_0_payload_B_1                                                                           (slicer_1_io_matAfterSlicers_0_payload_B_1[6:0]                                                                          ), //o
    .io_matAfterSlicers_0_payload_B_2                                                                           (slicer_1_io_matAfterSlicers_0_payload_B_2[6:0]                                                                          ), //o
    .io_matAfterSlicers_0_payload_B_3                                                                           (slicer_1_io_matAfterSlicers_0_payload_B_3[6:0]                                                                          ), //o
    .io_matAfterSlicers_0_payload_B_4                                                                           (slicer_1_io_matAfterSlicers_0_payload_B_4[6:0]                                                                          ), //o
    .io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation                (slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation[1:0]               ), //o
    .io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation (slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation[5:0]), //o
    .io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                    (slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                        ), //o
    .io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction                     (slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction[2:0]                    ), //o
    .io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation              (slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation[5:0]             ), //o
    .io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_UID                                     (slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_UID[18:0]                                   ), //o
    .io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt                      (slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt[12:0]                    ), //o
    .io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt                      (slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt[12:0]                    ), //o
    .io_matAfterSlicers_0_payload_Final                                                                         (slicer_1_io_matAfterSlicers_0_payload_Final                                                                             ), //o
    .io_matAfterSlicers_1_valid                                                                                 (slicer_1_io_matAfterSlicers_1_valid                                                                                     ), //o
    .io_matAfterSlicers_1_ready                                                                                 (io_Mats_to_Cores_Streams_1_ready                                                                                        ), //i
    .io_matAfterSlicers_1_payload_A_0                                                                           (slicer_1_io_matAfterSlicers_1_payload_A_0[8:0]                                                                          ), //o
    .io_matAfterSlicers_1_payload_A_1                                                                           (slicer_1_io_matAfterSlicers_1_payload_A_1[8:0]                                                                          ), //o
    .io_matAfterSlicers_1_payload_A_2                                                                           (slicer_1_io_matAfterSlicers_1_payload_A_2[8:0]                                                                          ), //o
    .io_matAfterSlicers_1_payload_A_3                                                                           (slicer_1_io_matAfterSlicers_1_payload_A_3[8:0]                                                                          ), //o
    .io_matAfterSlicers_1_payload_A_4                                                                           (slicer_1_io_matAfterSlicers_1_payload_A_4[8:0]                                                                          ), //o
    .io_matAfterSlicers_1_payload_B_0                                                                           (slicer_1_io_matAfterSlicers_1_payload_B_0[6:0]                                                                          ), //o
    .io_matAfterSlicers_1_payload_B_1                                                                           (slicer_1_io_matAfterSlicers_1_payload_B_1[6:0]                                                                          ), //o
    .io_matAfterSlicers_1_payload_B_2                                                                           (slicer_1_io_matAfterSlicers_1_payload_B_2[6:0]                                                                          ), //o
    .io_matAfterSlicers_1_payload_B_3                                                                           (slicer_1_io_matAfterSlicers_1_payload_B_3[6:0]                                                                          ), //o
    .io_matAfterSlicers_1_payload_B_4                                                                           (slicer_1_io_matAfterSlicers_1_payload_B_4[6:0]                                                                          ), //o
    .io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation                (slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation[1:0]               ), //o
    .io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation (slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation[5:0]), //o
    .io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                    (slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                        ), //o
    .io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction                     (slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction[2:0]                    ), //o
    .io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation              (slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation[5:0]             ), //o
    .io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_UID                                     (slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_UID[18:0]                                   ), //o
    .io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt                      (slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt[12:0]                    ), //o
    .io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt                      (slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt[12:0]                    ), //o
    .io_matAfterSlicers_1_payload_Final                                                                         (slicer_1_io_matAfterSlicers_1_payload_Final                                                                             ), //o
    .io_matAfterSlicers_2_valid                                                                                 (slicer_1_io_matAfterSlicers_2_valid                                                                                     ), //o
    .io_matAfterSlicers_2_ready                                                                                 (io_Mats_to_Cores_Streams_2_ready                                                                                        ), //i
    .io_matAfterSlicers_2_payload_A_0                                                                           (slicer_1_io_matAfterSlicers_2_payload_A_0[8:0]                                                                          ), //o
    .io_matAfterSlicers_2_payload_A_1                                                                           (slicer_1_io_matAfterSlicers_2_payload_A_1[8:0]                                                                          ), //o
    .io_matAfterSlicers_2_payload_A_2                                                                           (slicer_1_io_matAfterSlicers_2_payload_A_2[8:0]                                                                          ), //o
    .io_matAfterSlicers_2_payload_A_3                                                                           (slicer_1_io_matAfterSlicers_2_payload_A_3[8:0]                                                                          ), //o
    .io_matAfterSlicers_2_payload_A_4                                                                           (slicer_1_io_matAfterSlicers_2_payload_A_4[8:0]                                                                          ), //o
    .io_matAfterSlicers_2_payload_B_0                                                                           (slicer_1_io_matAfterSlicers_2_payload_B_0[6:0]                                                                          ), //o
    .io_matAfterSlicers_2_payload_B_1                                                                           (slicer_1_io_matAfterSlicers_2_payload_B_1[6:0]                                                                          ), //o
    .io_matAfterSlicers_2_payload_B_2                                                                           (slicer_1_io_matAfterSlicers_2_payload_B_2[6:0]                                                                          ), //o
    .io_matAfterSlicers_2_payload_B_3                                                                           (slicer_1_io_matAfterSlicers_2_payload_B_3[6:0]                                                                          ), //o
    .io_matAfterSlicers_2_payload_B_4                                                                           (slicer_1_io_matAfterSlicers_2_payload_B_4[6:0]                                                                          ), //o
    .io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation                (slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation[1:0]               ), //o
    .io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation (slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation[5:0]), //o
    .io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                    (slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                        ), //o
    .io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction                     (slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction[2:0]                    ), //o
    .io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation              (slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation[5:0]             ), //o
    .io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_UID                                     (slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_UID[18:0]                                   ), //o
    .io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt                      (slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt[12:0]                    ), //o
    .io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt                      (slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt[12:0]                    ), //o
    .io_matAfterSlicers_2_payload_Final                                                                         (slicer_1_io_matAfterSlicers_2_payload_Final                                                                             ), //o
    .clk                                                                                                        (clk                                                                                                                     ), //i
    .reset                                                                                                      (reset                                                                                                                   )  //i
  );
  `ifndef SYNTHESIS
  always @(*) begin
    case(io_inst_payload_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_inst_payload_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_inst_payload_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_inst_payload_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_inst_payload_matrixOperation_string = "ElementMax";
      default : io_inst_payload_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_inst_payload_activationFunction)
      Activation_TypeDef_Exp : io_inst_payload_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_inst_payload_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_inst_payload_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_inst_payload_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_inst_payload_activationFunction_string = "None    ";
      default : io_inst_payload_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(io_Mats_to_Cores_Streams_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_Mats_to_Cores_Streams_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_Mats_to_Cores_Streams_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_Mats_to_Cores_Streams_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_Mats_to_Cores_Streams_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : io_Mats_to_Cores_Streams_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(io_Mats_to_Cores_Streams_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_Mats_to_Cores_Streams_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_Mats_to_Cores_Streams_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_Mats_to_Cores_Streams_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_Mats_to_Cores_Streams_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : io_Mats_to_Cores_Streams_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(io_Mats_to_Cores_Streams_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_Mats_to_Cores_Streams_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_Mats_to_Cores_Streams_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_Mats_to_Cores_Streams_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_Mats_to_Cores_Streams_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : io_Mats_to_Cores_Streams_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  `endif

  assign io_inst_ready = slicer_1_io_inst_ready;
  assign io_slicedInst_valid = slicer_1_io_slicedInst_valid;
  assign io_slicedInst_payload_UID = slicer_1_io_slicedInst_payload_UID;
  assign io_slicedInst_payload_doTranspose = slicer_1_io_slicedInst_payload_doTranspose;
  assign io_slicedInst_payload_outputAddress = slicer_1_io_slicedInst_payload_outputAddress;
  assign io_slicedInst_payload_outputShape_0 = slicer_1_io_slicedInst_payload_outputShape_0;
  assign io_slicedInst_payload_outputShape_1 = slicer_1_io_slicedInst_payload_outputShape_1;
  assign io_Mats_to_Cores_Streams_0_valid = slicer_1_io_matAfterSlicers_0_valid;
  assign io_Mats_to_Cores_Streams_0_payload_A_0 = slicer_1_io_matAfterSlicers_0_payload_A_0;
  assign io_Mats_to_Cores_Streams_0_payload_A_1 = slicer_1_io_matAfterSlicers_0_payload_A_1;
  assign io_Mats_to_Cores_Streams_0_payload_A_2 = slicer_1_io_matAfterSlicers_0_payload_A_2;
  assign io_Mats_to_Cores_Streams_0_payload_A_3 = slicer_1_io_matAfterSlicers_0_payload_A_3;
  assign io_Mats_to_Cores_Streams_0_payload_A_4 = slicer_1_io_matAfterSlicers_0_payload_A_4;
  assign io_Mats_to_Cores_Streams_0_payload_B_0 = slicer_1_io_matAfterSlicers_0_payload_B_0;
  assign io_Mats_to_Cores_Streams_0_payload_B_1 = slicer_1_io_matAfterSlicers_0_payload_B_1;
  assign io_Mats_to_Cores_Streams_0_payload_B_2 = slicer_1_io_matAfterSlicers_0_payload_B_2;
  assign io_Mats_to_Cores_Streams_0_payload_B_3 = slicer_1_io_matAfterSlicers_0_payload_B_3;
  assign io_Mats_to_Cores_Streams_0_payload_B_4 = slicer_1_io_matAfterSlicers_0_payload_B_4;
  assign io_Mats_to_Cores_Streams_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign io_Mats_to_Cores_Streams_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation = slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  assign io_Mats_to_Cores_Streams_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose = slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  assign io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Activation_Instruction_activationFunction = slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction;
  assign io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation = slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  assign io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Collector_Instruction_UID = slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_UID;
  assign io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt = slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  assign io_Mats_to_Cores_Streams_0_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt = slicer_1_io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  assign io_Mats_to_Cores_Streams_0_payload_Final = slicer_1_io_matAfterSlicers_0_payload_Final;
  assign io_Mats_to_Cores_Streams_1_valid = slicer_1_io_matAfterSlicers_1_valid;
  assign io_Mats_to_Cores_Streams_1_payload_A_0 = slicer_1_io_matAfterSlicers_1_payload_A_0;
  assign io_Mats_to_Cores_Streams_1_payload_A_1 = slicer_1_io_matAfterSlicers_1_payload_A_1;
  assign io_Mats_to_Cores_Streams_1_payload_A_2 = slicer_1_io_matAfterSlicers_1_payload_A_2;
  assign io_Mats_to_Cores_Streams_1_payload_A_3 = slicer_1_io_matAfterSlicers_1_payload_A_3;
  assign io_Mats_to_Cores_Streams_1_payload_A_4 = slicer_1_io_matAfterSlicers_1_payload_A_4;
  assign io_Mats_to_Cores_Streams_1_payload_B_0 = slicer_1_io_matAfterSlicers_1_payload_B_0;
  assign io_Mats_to_Cores_Streams_1_payload_B_1 = slicer_1_io_matAfterSlicers_1_payload_B_1;
  assign io_Mats_to_Cores_Streams_1_payload_B_2 = slicer_1_io_matAfterSlicers_1_payload_B_2;
  assign io_Mats_to_Cores_Streams_1_payload_B_3 = slicer_1_io_matAfterSlicers_1_payload_B_3;
  assign io_Mats_to_Cores_Streams_1_payload_B_4 = slicer_1_io_matAfterSlicers_1_payload_B_4;
  assign io_Mats_to_Cores_Streams_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign io_Mats_to_Cores_Streams_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation = slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  assign io_Mats_to_Cores_Streams_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose = slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  assign io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Activation_Instruction_activationFunction = slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction;
  assign io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation = slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  assign io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Collector_Instruction_UID = slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_UID;
  assign io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt = slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  assign io_Mats_to_Cores_Streams_1_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt = slicer_1_io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  assign io_Mats_to_Cores_Streams_1_payload_Final = slicer_1_io_matAfterSlicers_1_payload_Final;
  assign io_Mats_to_Cores_Streams_2_valid = slicer_1_io_matAfterSlicers_2_valid;
  assign io_Mats_to_Cores_Streams_2_payload_A_0 = slicer_1_io_matAfterSlicers_2_payload_A_0;
  assign io_Mats_to_Cores_Streams_2_payload_A_1 = slicer_1_io_matAfterSlicers_2_payload_A_1;
  assign io_Mats_to_Cores_Streams_2_payload_A_2 = slicer_1_io_matAfterSlicers_2_payload_A_2;
  assign io_Mats_to_Cores_Streams_2_payload_A_3 = slicer_1_io_matAfterSlicers_2_payload_A_3;
  assign io_Mats_to_Cores_Streams_2_payload_A_4 = slicer_1_io_matAfterSlicers_2_payload_A_4;
  assign io_Mats_to_Cores_Streams_2_payload_B_0 = slicer_1_io_matAfterSlicers_2_payload_B_0;
  assign io_Mats_to_Cores_Streams_2_payload_B_1 = slicer_1_io_matAfterSlicers_2_payload_B_1;
  assign io_Mats_to_Cores_Streams_2_payload_B_2 = slicer_1_io_matAfterSlicers_2_payload_B_2;
  assign io_Mats_to_Cores_Streams_2_payload_B_3 = slicer_1_io_matAfterSlicers_2_payload_B_3;
  assign io_Mats_to_Cores_Streams_2_payload_B_4 = slicer_1_io_matAfterSlicers_2_payload_B_4;
  assign io_Mats_to_Cores_Streams_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign io_Mats_to_Cores_Streams_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation = slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  assign io_Mats_to_Cores_Streams_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose = slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  assign io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Activation_Instruction_activationFunction = slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction;
  assign io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation = slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  assign io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Collector_Instruction_UID = slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_UID;
  assign io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt = slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  assign io_Mats_to_Cores_Streams_2_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt = slicer_1_io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  assign io_Mats_to_Cores_Streams_2_payload_Final = slicer_1_io_matAfterSlicers_2_payload_Final;

endmodule

module Slicer (
  input  wire          io_inst_valid,
  output reg           io_inst_ready,
  input  wire [18:0]   io_inst_payload_UID,
  input  wire [1:0]    io_inst_payload_matrixOperation,
  input  wire [5:0]    io_inst_payload_shiftLeft_AfterMatrixOperation,
  input  wire          io_inst_payload_doTranspose,
  input  wire [2:0]    io_inst_payload_activationFunction,
  input  wire [5:0]    io_inst_payload_shiftLeft_AfterActivation,
  input  wire [16:0]   io_inst_payload_input0Address,
  input  wire [14:0]   io_inst_payload_input0Shape_0,
  input  wire [14:0]   io_inst_payload_input0Shape_1,
  input  wire [16:0]   io_inst_payload_input1Address,
  input  wire [14:0]   io_inst_payload_input1Shape_0,
  input  wire [14:0]   io_inst_payload_input1Shape_1,
  input  wire [16:0]   io_inst_payload_outputAddress,
  output reg           io_slicedInst_valid,
  input  wire          io_slicedInst_ready,
  output wire [18:0]   io_slicedInst_payload_UID,
  output wire          io_slicedInst_payload_doTranspose,
  output wire [16:0]   io_slicedInst_payload_outputAddress,
  output wire [14:0]   io_slicedInst_payload_outputShape_0,
  output wire [14:0]   io_slicedInst_payload_outputShape_1,
  output wire          io_memoryReadPortA_Valid,
  output wire [16:0]   io_memoryReadPortA_Address,
  input  wire [44:0]   io_memoryReadPortA_Data,
  output wire          io_memoryReadPortB_Valid,
  output wire [16:0]   io_memoryReadPortB_Address,
  input  wire [34:0]   io_memoryReadPortB_Data,
  output wire          io_matAfterSlicers_0_valid,
  input  wire          io_matAfterSlicers_0_ready,
  output wire [8:0]    io_matAfterSlicers_0_payload_A_0,
  output wire [8:0]    io_matAfterSlicers_0_payload_A_1,
  output wire [8:0]    io_matAfterSlicers_0_payload_A_2,
  output wire [8:0]    io_matAfterSlicers_0_payload_A_3,
  output wire [8:0]    io_matAfterSlicers_0_payload_A_4,
  output wire [6:0]    io_matAfterSlicers_0_payload_B_0,
  output wire [6:0]    io_matAfterSlicers_0_payload_B_1,
  output wire [6:0]    io_matAfterSlicers_0_payload_B_2,
  output wire [6:0]    io_matAfterSlicers_0_payload_B_3,
  output wire [6:0]    io_matAfterSlicers_0_payload_B_4,
  output wire [1:0]    io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  output wire [5:0]    io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  output wire          io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  output wire [2:0]    io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction,
  output wire [5:0]    io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  output wire [18:0]   io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_UID,
  output wire [12:0]   io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  output wire [12:0]   io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt,
  output wire          io_matAfterSlicers_0_payload_Final,
  output wire          io_matAfterSlicers_1_valid,
  input  wire          io_matAfterSlicers_1_ready,
  output wire [8:0]    io_matAfterSlicers_1_payload_A_0,
  output wire [8:0]    io_matAfterSlicers_1_payload_A_1,
  output wire [8:0]    io_matAfterSlicers_1_payload_A_2,
  output wire [8:0]    io_matAfterSlicers_1_payload_A_3,
  output wire [8:0]    io_matAfterSlicers_1_payload_A_4,
  output wire [6:0]    io_matAfterSlicers_1_payload_B_0,
  output wire [6:0]    io_matAfterSlicers_1_payload_B_1,
  output wire [6:0]    io_matAfterSlicers_1_payload_B_2,
  output wire [6:0]    io_matAfterSlicers_1_payload_B_3,
  output wire [6:0]    io_matAfterSlicers_1_payload_B_4,
  output wire [1:0]    io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  output wire [5:0]    io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  output wire          io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  output wire [2:0]    io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction,
  output wire [5:0]    io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  output wire [18:0]   io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_UID,
  output wire [12:0]   io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  output wire [12:0]   io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt,
  output wire          io_matAfterSlicers_1_payload_Final,
  output wire          io_matAfterSlicers_2_valid,
  input  wire          io_matAfterSlicers_2_ready,
  output wire [8:0]    io_matAfterSlicers_2_payload_A_0,
  output wire [8:0]    io_matAfterSlicers_2_payload_A_1,
  output wire [8:0]    io_matAfterSlicers_2_payload_A_2,
  output wire [8:0]    io_matAfterSlicers_2_payload_A_3,
  output wire [8:0]    io_matAfterSlicers_2_payload_A_4,
  output wire [6:0]    io_matAfterSlicers_2_payload_B_0,
  output wire [6:0]    io_matAfterSlicers_2_payload_B_1,
  output wire [6:0]    io_matAfterSlicers_2_payload_B_2,
  output wire [6:0]    io_matAfterSlicers_2_payload_B_3,
  output wire [6:0]    io_matAfterSlicers_2_payload_B_4,
  output wire [1:0]    io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  output wire [5:0]    io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  output wire          io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  output wire [2:0]    io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction,
  output wire [5:0]    io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  output wire [18:0]   io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_UID,
  output wire [12:0]   io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  output wire [12:0]   io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt,
  output wire          io_matAfterSlicers_2_payload_Final,
  input  wire          clk,
  input  wire          reset
);
  localparam MatrixOperation_TypeDef_MatMul = 2'd0;
  localparam MatrixOperation_TypeDef_ElementAdd = 2'd1;
  localparam MatrixOperation_TypeDef_ElementMul = 2'd2;
  localparam MatrixOperation_TypeDef_ElementMax = 2'd3;
  localparam Activation_TypeDef_Exp = 3'd0;
  localparam Activation_TypeDef_Log = 3'd1;
  localparam Activation_TypeDef_Softplus = 3'd2;
  localparam Activation_TypeDef_Relu = 3'd3;
  localparam Activation_TypeDef_None = 3'd4;

  wire                streamDispatcher_1_io_input_ready;
  wire                streamDispatcher_1_io_outputs_0_valid;
  wire                streamDispatcher_1_io_outputs_0_payload_last;
  wire       [8:0]    streamDispatcher_1_io_outputs_0_payload_fragment_A_0;
  wire       [8:0]    streamDispatcher_1_io_outputs_0_payload_fragment_A_1;
  wire       [8:0]    streamDispatcher_1_io_outputs_0_payload_fragment_A_2;
  wire       [8:0]    streamDispatcher_1_io_outputs_0_payload_fragment_A_3;
  wire       [8:0]    streamDispatcher_1_io_outputs_0_payload_fragment_A_4;
  wire       [6:0]    streamDispatcher_1_io_outputs_0_payload_fragment_B_0;
  wire       [6:0]    streamDispatcher_1_io_outputs_0_payload_fragment_B_1;
  wire       [6:0]    streamDispatcher_1_io_outputs_0_payload_fragment_B_2;
  wire       [6:0]    streamDispatcher_1_io_outputs_0_payload_fragment_B_3;
  wire       [6:0]    streamDispatcher_1_io_outputs_0_payload_fragment_B_4;
  wire       [1:0]    streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  wire       [5:0]    streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  wire                streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  wire       [2:0]    streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  wire       [5:0]    streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  wire       [18:0]   streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  wire       [12:0]   streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  wire                streamDispatcher_1_io_outputs_1_valid;
  wire                streamDispatcher_1_io_outputs_1_payload_last;
  wire       [8:0]    streamDispatcher_1_io_outputs_1_payload_fragment_A_0;
  wire       [8:0]    streamDispatcher_1_io_outputs_1_payload_fragment_A_1;
  wire       [8:0]    streamDispatcher_1_io_outputs_1_payload_fragment_A_2;
  wire       [8:0]    streamDispatcher_1_io_outputs_1_payload_fragment_A_3;
  wire       [8:0]    streamDispatcher_1_io_outputs_1_payload_fragment_A_4;
  wire       [6:0]    streamDispatcher_1_io_outputs_1_payload_fragment_B_0;
  wire       [6:0]    streamDispatcher_1_io_outputs_1_payload_fragment_B_1;
  wire       [6:0]    streamDispatcher_1_io_outputs_1_payload_fragment_B_2;
  wire       [6:0]    streamDispatcher_1_io_outputs_1_payload_fragment_B_3;
  wire       [6:0]    streamDispatcher_1_io_outputs_1_payload_fragment_B_4;
  wire       [1:0]    streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  wire       [5:0]    streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  wire                streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  wire       [2:0]    streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  wire       [5:0]    streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  wire       [18:0]   streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  wire       [12:0]   streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  wire                streamDispatcher_1_io_outputs_2_valid;
  wire                streamDispatcher_1_io_outputs_2_payload_last;
  wire       [8:0]    streamDispatcher_1_io_outputs_2_payload_fragment_A_0;
  wire       [8:0]    streamDispatcher_1_io_outputs_2_payload_fragment_A_1;
  wire       [8:0]    streamDispatcher_1_io_outputs_2_payload_fragment_A_2;
  wire       [8:0]    streamDispatcher_1_io_outputs_2_payload_fragment_A_3;
  wire       [8:0]    streamDispatcher_1_io_outputs_2_payload_fragment_A_4;
  wire       [6:0]    streamDispatcher_1_io_outputs_2_payload_fragment_B_0;
  wire       [6:0]    streamDispatcher_1_io_outputs_2_payload_fragment_B_1;
  wire       [6:0]    streamDispatcher_1_io_outputs_2_payload_fragment_B_2;
  wire       [6:0]    streamDispatcher_1_io_outputs_2_payload_fragment_B_3;
  wire       [6:0]    streamDispatcher_1_io_outputs_2_payload_fragment_B_4;
  wire       [1:0]    streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  wire       [5:0]    streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  wire                streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  wire       [2:0]    streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  wire       [5:0]    streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  wire       [18:0]   streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  wire       [12:0]   streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  wire       [14:0]   _zz_matAColSliceCnt_willOverflowIfInc;
  wire       [14:0]   _zz_matAColSliceCnt_willOverflowIfInc_1;
  wire       [14:0]   _zz_matAColSliceCnt_valueNext;
  wire       [14:0]   _zz_matBColSliceCnt_willOverflowIfInc;
  wire       [14:0]   _zz_matBColSliceCnt_willOverflowIfInc_1;
  wire       [14:0]   _zz_matBColSliceCnt_valueNext;
  wire       [14:0]   _zz_matARowSliceCnt_willOverflowIfInc;
  wire       [14:0]   _zz_matARowSliceCnt_willOverflowIfInc_1;
  wire       [14:0]   _zz_matARowSliceCnt_valueNext;
  wire       [2:0]    _zz_matASubReadRowCnt_valueNext;
  wire       [16:0]   _zz_io_memoryReadPortA_Address;
  wire       [16:0]   _zz_io_memoryReadPortA_Address_1;
  wire       [32:0]   _zz_io_memoryReadPortA_Address_2;
  wire       [32:0]   _zz_io_memoryReadPortA_Address_3;
  wire       [17:0]   _zz_io_memoryReadPortA_Address_4;
  wire       [17:0]   _zz_io_memoryReadPortA_Address_5;
  wire       [17:0]   _zz_io_memoryReadPortA_Address_6;
  wire       [16:0]   _zz_io_memoryReadPortA_Address_7;
  wire       [14:0]   _zz_io_memoryReadPortA_Address_8;
  wire       [2:0]    _zz_matBSubReadRowCnt_valueNext;
  wire       [16:0]   _zz_io_memoryReadPortB_Address;
  wire       [16:0]   _zz_io_memoryReadPortB_Address_1;
  wire       [32:0]   _zz_io_memoryReadPortB_Address_2;
  wire       [32:0]   _zz_io_memoryReadPortB_Address_3;
  wire       [17:0]   _zz_io_memoryReadPortB_Address_4;
  wire       [17:0]   _zz_io_memoryReadPortB_Address_5;
  wire       [17:0]   _zz_io_memoryReadPortB_Address_6;
  wire       [16:0]   _zz_io_memoryReadPortB_Address_7;
  wire       [2:0]    _zz_matABSubSendRowCnt_valueNext;
  reg        [8:0]    _zz_matAfterSlicer_payload_A_0;
  reg        [8:0]    _zz_matAfterSlicer_payload_A_1;
  reg        [8:0]    _zz_matAfterSlicer_payload_A_2;
  reg        [8:0]    _zz_matAfterSlicer_payload_A_3;
  reg        [8:0]    _zz_matAfterSlicer_payload_A_4;
  wire       [6:0]    _zz_matAfterSlicer_payload_B_0;
  reg        [6:0]    _zz_matAfterSlicer_payload_B_0_1;
  wire       [6:0]    _zz_matAfterSlicer_payload_B_0_2;
  reg        [6:0]    _zz_matAfterSlicer_payload_B_0_3;
  wire       [6:0]    _zz_matAfterSlicer_payload_B_1;
  reg        [6:0]    _zz_matAfterSlicer_payload_B_1_1;
  wire       [6:0]    _zz_matAfterSlicer_payload_B_1_2;
  reg        [6:0]    _zz_matAfterSlicer_payload_B_1_3;
  wire       [6:0]    _zz_matAfterSlicer_payload_B_2;
  reg        [6:0]    _zz_matAfterSlicer_payload_B_2_1;
  wire       [6:0]    _zz_matAfterSlicer_payload_B_2_2;
  reg        [6:0]    _zz_matAfterSlicer_payload_B_2_3;
  wire       [6:0]    _zz_matAfterSlicer_payload_B_3;
  reg        [6:0]    _zz_matAfterSlicer_payload_B_3_1;
  wire       [6:0]    _zz_matAfterSlicer_payload_B_3_2;
  reg        [6:0]    _zz_matAfterSlicer_payload_B_3_3;
  wire       [6:0]    _zz_matAfterSlicer_payload_B_4;
  reg        [6:0]    _zz_matAfterSlicer_payload_B_4_1;
  wire       [6:0]    _zz_matAfterSlicer_payload_B_4_2;
  reg        [6:0]    _zz_matAfterSlicer_payload_B_4_3;
  reg        [18:0]   instReg_UID;
  reg        [1:0]    instReg_matrixOperation;
  reg        [5:0]    instReg_shiftLeft_AfterMatrixOperation;
  reg                 instReg_doTranspose;
  reg        [2:0]    instReg_activationFunction;
  reg        [5:0]    instReg_shiftLeft_AfterActivation;
  reg        [16:0]   instReg_input0Address;
  reg        [14:0]   instReg_input0Shape_0;
  reg        [14:0]   instReg_input0Shape_1;
  reg        [16:0]   instReg_input1Address;
  reg        [14:0]   instReg_input1Shape_0;
  reg        [14:0]   instReg_input1Shape_1;
  reg        [16:0]   instReg_outputAddress;
  wire                isMatMul;
  wire                instFinish;
  wire                io_inst_fire;
  reg        [14:0]   _zz_io_slicedInst_payload_outputShape_0;
  reg        [14:0]   _zz_io_slicedInst_payload_outputShape_1;
  wire                io_slicedInst_fire;
  wire                matABSubSendFinish;
  reg        [14:0]   matAColSliceCnt_cnt;
  wire                matAColSliceCnt_willOverflowIfInc;
  wire                matAColSliceCnt_willOverflow;
  wire       [14:0]   matAColSliceCnt_valueNext;
  reg        [14:0]   matBColSliceCnt_cnt;
  wire                matBColSliceCnt_willOverflowIfInc;
  wire                matBColSliceCnt_willOverflow;
  wire       [14:0]   matBColSliceCnt_valueNext;
  reg        [14:0]   matARowSliceCnt_cnt;
  wire                matARowSliceCnt_willOverflowIfInc;
  wire                matARowSliceCnt_willOverflow;
  wire       [14:0]   matARowSliceCnt_valueNext;
  reg        [2:0]    matASubReadRowCnt_cnt;
  wire                matASubReadRowCnt_willOverflowIfInc;
  wire                matASubReadRowCnt_willOverflow;
  wire       [2:0]    matASubReadRowCnt_valueNext;
  reg                 _zz_io_memoryReadPortA_Valid;
  reg        [8:0]    matASub_0_0;
  reg        [8:0]    matASub_0_1;
  reg        [8:0]    matASub_0_2;
  reg        [8:0]    matASub_0_3;
  reg        [8:0]    matASub_0_4;
  reg        [8:0]    matASub_1_0;
  reg        [8:0]    matASub_1_1;
  reg        [8:0]    matASub_1_2;
  reg        [8:0]    matASub_1_3;
  reg        [8:0]    matASub_1_4;
  reg        [8:0]    matASub_2_0;
  reg        [8:0]    matASub_2_1;
  reg        [8:0]    matASub_2_2;
  reg        [8:0]    matASub_2_3;
  reg        [8:0]    matASub_2_4;
  reg        [8:0]    matASub_3_0;
  reg        [8:0]    matASub_3_1;
  reg        [8:0]    matASub_3_2;
  reg        [8:0]    matASub_3_3;
  reg        [8:0]    matASub_3_4;
  reg        [8:0]    matASub_4_0;
  reg        [8:0]    matASub_4_1;
  reg        [8:0]    matASub_4_2;
  reg        [8:0]    matASub_4_3;
  reg        [8:0]    matASub_4_4;
  reg                 io_memoryReadPortA_Valid_regNext;
  reg        [2:0]    matASubReadRowCnt_cnt_regNext;
  wire       [7:0]    _zz_1;
  wire                _zz_2;
  wire                _zz_3;
  wire                _zz_4;
  wire                _zz_5;
  wire                _zz_6;
  wire       [8:0]    _zz_matASub_0_0;
  wire       [8:0]    _zz_matASub_0_1;
  wire       [8:0]    _zz_matASub_0_2;
  wire       [8:0]    _zz_matASub_0_3;
  wire       [8:0]    _zz_matASub_0_4;
  reg        [2:0]    matBSubReadRowCnt_cnt;
  wire                matBSubReadRowCnt_willOverflowIfInc;
  wire                matBSubReadRowCnt_willOverflow;
  wire       [2:0]    matBSubReadRowCnt_valueNext;
  reg                 _zz_io_memoryReadPortB_Valid;
  reg        [6:0]    matBSub_0_0;
  reg        [6:0]    matBSub_0_1;
  reg        [6:0]    matBSub_0_2;
  reg        [6:0]    matBSub_0_3;
  reg        [6:0]    matBSub_0_4;
  reg        [6:0]    matBSub_1_0;
  reg        [6:0]    matBSub_1_1;
  reg        [6:0]    matBSub_1_2;
  reg        [6:0]    matBSub_1_3;
  reg        [6:0]    matBSub_1_4;
  reg        [6:0]    matBSub_2_0;
  reg        [6:0]    matBSub_2_1;
  reg        [6:0]    matBSub_2_2;
  reg        [6:0]    matBSub_2_3;
  reg        [6:0]    matBSub_2_4;
  reg        [6:0]    matBSub_3_0;
  reg        [6:0]    matBSub_3_1;
  reg        [6:0]    matBSub_3_2;
  reg        [6:0]    matBSub_3_3;
  reg        [6:0]    matBSub_3_4;
  reg        [6:0]    matBSub_4_0;
  reg        [6:0]    matBSub_4_1;
  reg        [6:0]    matBSub_4_2;
  reg        [6:0]    matBSub_4_3;
  reg        [6:0]    matBSub_4_4;
  reg                 io_memoryReadPortB_Valid_regNext;
  reg        [2:0]    matASubReadRowCnt_cnt_regNext_1;
  wire       [7:0]    _zz_7;
  wire                _zz_8;
  wire                _zz_9;
  wire                _zz_10;
  wire                _zz_11;
  wire                _zz_12;
  wire       [6:0]    _zz_matBSub_0_0;
  wire       [6:0]    _zz_matBSub_0_1;
  wire       [6:0]    _zz_matBSub_0_2;
  wire       [6:0]    _zz_matBSub_0_3;
  wire       [6:0]    _zz_matBSub_0_4;
  reg                 matASubReadFinish;
  reg                 matBSubReadFinish;
  wire                matInSubReadFinish;
  reg                 matAfterSlicer_valid;
  wire                matAfterSlicer_ready;
  wire       [8:0]    matAfterSlicer_payload_A_0;
  wire       [8:0]    matAfterSlicer_payload_A_1;
  wire       [8:0]    matAfterSlicer_payload_A_2;
  wire       [8:0]    matAfterSlicer_payload_A_3;
  wire       [8:0]    matAfterSlicer_payload_A_4;
  wire       [6:0]    matAfterSlicer_payload_B_0;
  wire       [6:0]    matAfterSlicer_payload_B_1;
  wire       [6:0]    matAfterSlicer_payload_B_2;
  wire       [6:0]    matAfterSlicer_payload_B_3;
  wire       [6:0]    matAfterSlicer_payload_B_4;
  wire       [1:0]    matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  wire       [5:0]    matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  wire                matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  wire       [2:0]    matAfterSlicer_payload_CoreInstruction_Activation_Instruction_activationFunction;
  wire       [5:0]    matAfterSlicer_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  wire       [18:0]   matAfterSlicer_payload_CoreInstruction_Collector_Instruction_UID;
  wire       [12:0]   matAfterSlicer_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   matAfterSlicer_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  wire                matAfterSlicer_payload_Final;
  wire                matAfterSlicer_fire;
  reg        [2:0]    matABSubSendRowCnt_cnt;
  wire                matABSubSendRowCnt_willOverflowIfInc;
  wire                matABSubSendRowCnt_willOverflow;
  wire       [2:0]    matABSubSendRowCnt_valueNext;
  wire       [1:0]    _zz_io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  wire       [2:0]    _zz_io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  wire       [1:0]    _zz_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  wire       [2:0]    _zz_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction;
  wire       [1:0]    _zz_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  wire       [2:0]    _zz_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction;
  wire       [1:0]    _zz_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  wire       [2:0]    _zz_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction;
  `ifndef SYNTHESIS
  reg [79:0] io_inst_payload_matrixOperation_string;
  reg [63:0] io_inst_payload_activationFunction_string;
  reg [79:0] io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] instReg_matrixOperation_string;
  reg [63:0] instReg_activationFunction_string;
  reg [79:0] matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] matAfterSlicer_payload_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] _zz_io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] _zz_io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] _zz_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] _zz_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] _zz_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] _zz_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] _zz_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] _zz_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string;
  `endif


  assign _zz_matAColSliceCnt_willOverflowIfInc = ((isMatMul ? _zz_matAColSliceCnt_willOverflowIfInc_1 : 15'h0001) - 15'h0001);
  assign _zz_matAColSliceCnt_willOverflowIfInc_1 = (instReg_input0Shape_1 / 3'b101);
  assign _zz_matAColSliceCnt_valueNext = (matAColSliceCnt_cnt + 15'h0001);
  assign _zz_matBColSliceCnt_willOverflowIfInc = (_zz_matBColSliceCnt_willOverflowIfInc_1 - 15'h0001);
  assign _zz_matBColSliceCnt_willOverflowIfInc_1 = (instReg_input1Shape_1 / 3'b101);
  assign _zz_matBColSliceCnt_valueNext = (matBColSliceCnt_cnt + 15'h0001);
  assign _zz_matARowSliceCnt_willOverflowIfInc = (_zz_matARowSliceCnt_willOverflowIfInc_1 - 15'h0001);
  assign _zz_matARowSliceCnt_willOverflowIfInc_1 = (instReg_input0Shape_0 / 3'b101);
  assign _zz_matARowSliceCnt_valueNext = (matARowSliceCnt_cnt + 15'h0001);
  assign _zz_matASubReadRowCnt_valueNext = (matASubReadRowCnt_cnt + 3'b001);
  assign _zz_io_memoryReadPortA_Address = (instReg_input0Address + _zz_io_memoryReadPortA_Address_1);
  assign _zz_io_memoryReadPortA_Address_2 = (_zz_io_memoryReadPortA_Address_3 / 3'b101);
  assign _zz_io_memoryReadPortA_Address_1 = _zz_io_memoryReadPortA_Address_2[16:0];
  assign _zz_io_memoryReadPortA_Address_3 = (_zz_io_memoryReadPortA_Address_4 * instReg_input0Shape_1);
  assign _zz_io_memoryReadPortA_Address_4 = (_zz_io_memoryReadPortA_Address_5 + _zz_io_memoryReadPortA_Address_6);
  assign _zz_io_memoryReadPortA_Address_5 = (matARowSliceCnt_cnt * 3'b101);
  assign _zz_io_memoryReadPortA_Address_6 = {15'd0, matASubReadRowCnt_cnt};
  assign _zz_io_memoryReadPortA_Address_8 = (isMatMul ? matAColSliceCnt_cnt : matBColSliceCnt_cnt);
  assign _zz_io_memoryReadPortA_Address_7 = {2'd0, _zz_io_memoryReadPortA_Address_8};
  assign _zz_matBSubReadRowCnt_valueNext = (matBSubReadRowCnt_cnt + 3'b001);
  assign _zz_io_memoryReadPortB_Address = (instReg_input1Address + _zz_io_memoryReadPortB_Address_1);
  assign _zz_io_memoryReadPortB_Address_2 = (_zz_io_memoryReadPortB_Address_3 / 3'b101);
  assign _zz_io_memoryReadPortB_Address_1 = _zz_io_memoryReadPortB_Address_2[16:0];
  assign _zz_io_memoryReadPortB_Address_3 = (_zz_io_memoryReadPortB_Address_4 * instReg_input1Shape_1);
  assign _zz_io_memoryReadPortB_Address_4 = (_zz_io_memoryReadPortB_Address_5 + _zz_io_memoryReadPortB_Address_6);
  assign _zz_io_memoryReadPortB_Address_5 = ((isMatMul ? matAColSliceCnt_cnt : matARowSliceCnt_cnt) * 3'b101);
  assign _zz_io_memoryReadPortB_Address_6 = {15'd0, matBSubReadRowCnt_cnt};
  assign _zz_io_memoryReadPortB_Address_7 = {2'd0, matBColSliceCnt_cnt};
  assign _zz_matABSubSendRowCnt_valueNext = (matABSubSendRowCnt_cnt + 3'b001);
  assign _zz_matAfterSlicer_payload_B_0 = _zz_matAfterSlicer_payload_B_0_1;
  assign _zz_matAfterSlicer_payload_B_0_2 = _zz_matAfterSlicer_payload_B_0_3;
  assign _zz_matAfterSlicer_payload_B_1 = _zz_matAfterSlicer_payload_B_1_1;
  assign _zz_matAfterSlicer_payload_B_1_2 = _zz_matAfterSlicer_payload_B_1_3;
  assign _zz_matAfterSlicer_payload_B_2 = _zz_matAfterSlicer_payload_B_2_1;
  assign _zz_matAfterSlicer_payload_B_2_2 = _zz_matAfterSlicer_payload_B_2_3;
  assign _zz_matAfterSlicer_payload_B_3 = _zz_matAfterSlicer_payload_B_3_1;
  assign _zz_matAfterSlicer_payload_B_3_2 = _zz_matAfterSlicer_payload_B_3_3;
  assign _zz_matAfterSlicer_payload_B_4 = _zz_matAfterSlicer_payload_B_4_1;
  assign _zz_matAfterSlicer_payload_B_4_2 = _zz_matAfterSlicer_payload_B_4_3;
  StreamDispatcher streamDispatcher_1 (
    .io_input_valid                                                                                              (matAfterSlicer_valid                                                                                                               ), //i
    .io_input_ready                                                                                              (streamDispatcher_1_io_input_ready                                                                                                  ), //o
    .io_input_payload_last                                                                                       (matAfterSlicer_payload_Final                                                                                                       ), //i
    .io_input_payload_fragment_A_0                                                                               (matAfterSlicer_payload_A_0[8:0]                                                                                                    ), //i
    .io_input_payload_fragment_A_1                                                                               (matAfterSlicer_payload_A_1[8:0]                                                                                                    ), //i
    .io_input_payload_fragment_A_2                                                                               (matAfterSlicer_payload_A_2[8:0]                                                                                                    ), //i
    .io_input_payload_fragment_A_3                                                                               (matAfterSlicer_payload_A_3[8:0]                                                                                                    ), //i
    .io_input_payload_fragment_A_4                                                                               (matAfterSlicer_payload_A_4[8:0]                                                                                                    ), //i
    .io_input_payload_fragment_B_0                                                                               (matAfterSlicer_payload_B_0[6:0]                                                                                                    ), //i
    .io_input_payload_fragment_B_1                                                                               (matAfterSlicer_payload_B_1[6:0]                                                                                                    ), //i
    .io_input_payload_fragment_B_2                                                                               (matAfterSlicer_payload_B_2[6:0]                                                                                                    ), //i
    .io_input_payload_fragment_B_3                                                                               (matAfterSlicer_payload_B_3[6:0]                                                                                                    ), //i
    .io_input_payload_fragment_B_4                                                                               (matAfterSlicer_payload_B_4[6:0]                                                                                                    ), //i
    .io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation                    (_zz_io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation[1:0]                                  ), //i
    .io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation     (matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation[5:0]                          ), //i
    .io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                        (matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                                                  ), //i
    .io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction                         (_zz_io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction[2:0]                                       ), //i
    .io_input_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation                  (matAfterSlicer_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation[5:0]                                       ), //i
    .io_input_payload_fragment_CoreInstruction_Collector_Instruction_UID                                         (matAfterSlicer_payload_CoreInstruction_Collector_Instruction_UID[18:0]                                                             ), //i
    .io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt                          (matAfterSlicer_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt[12:0]                                              ), //i
    .io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt                          (matAfterSlicer_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt[12:0]                                              ), //i
    .io_outputs_0_valid                                                                                          (streamDispatcher_1_io_outputs_0_valid                                                                                              ), //o
    .io_outputs_0_ready                                                                                          (io_matAfterSlicers_0_ready                                                                                                         ), //i
    .io_outputs_0_payload_last                                                                                   (streamDispatcher_1_io_outputs_0_payload_last                                                                                       ), //o
    .io_outputs_0_payload_fragment_A_0                                                                           (streamDispatcher_1_io_outputs_0_payload_fragment_A_0[8:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_A_1                                                                           (streamDispatcher_1_io_outputs_0_payload_fragment_A_1[8:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_A_2                                                                           (streamDispatcher_1_io_outputs_0_payload_fragment_A_2[8:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_A_3                                                                           (streamDispatcher_1_io_outputs_0_payload_fragment_A_3[8:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_A_4                                                                           (streamDispatcher_1_io_outputs_0_payload_fragment_A_4[8:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_B_0                                                                           (streamDispatcher_1_io_outputs_0_payload_fragment_B_0[6:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_B_1                                                                           (streamDispatcher_1_io_outputs_0_payload_fragment_B_1[6:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_B_2                                                                           (streamDispatcher_1_io_outputs_0_payload_fragment_B_2[6:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_B_3                                                                           (streamDispatcher_1_io_outputs_0_payload_fragment_B_3[6:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_B_4                                                                           (streamDispatcher_1_io_outputs_0_payload_fragment_B_4[6:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation                (streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation[1:0]               ), //o
    .io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation (streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation[5:0]), //o
    .io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                    (streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                        ), //o
    .io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction                     (streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction[2:0]                    ), //o
    .io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation              (streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation[5:0]             ), //o
    .io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_UID                                     (streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_UID[18:0]                                   ), //o
    .io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt                      (streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt[12:0]                    ), //o
    .io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt                      (streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt[12:0]                    ), //o
    .io_outputs_1_valid                                                                                          (streamDispatcher_1_io_outputs_1_valid                                                                                              ), //o
    .io_outputs_1_ready                                                                                          (io_matAfterSlicers_1_ready                                                                                                         ), //i
    .io_outputs_1_payload_last                                                                                   (streamDispatcher_1_io_outputs_1_payload_last                                                                                       ), //o
    .io_outputs_1_payload_fragment_A_0                                                                           (streamDispatcher_1_io_outputs_1_payload_fragment_A_0[8:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_A_1                                                                           (streamDispatcher_1_io_outputs_1_payload_fragment_A_1[8:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_A_2                                                                           (streamDispatcher_1_io_outputs_1_payload_fragment_A_2[8:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_A_3                                                                           (streamDispatcher_1_io_outputs_1_payload_fragment_A_3[8:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_A_4                                                                           (streamDispatcher_1_io_outputs_1_payload_fragment_A_4[8:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_B_0                                                                           (streamDispatcher_1_io_outputs_1_payload_fragment_B_0[6:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_B_1                                                                           (streamDispatcher_1_io_outputs_1_payload_fragment_B_1[6:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_B_2                                                                           (streamDispatcher_1_io_outputs_1_payload_fragment_B_2[6:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_B_3                                                                           (streamDispatcher_1_io_outputs_1_payload_fragment_B_3[6:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_B_4                                                                           (streamDispatcher_1_io_outputs_1_payload_fragment_B_4[6:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation                (streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation[1:0]               ), //o
    .io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation (streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation[5:0]), //o
    .io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                    (streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                        ), //o
    .io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction                     (streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction[2:0]                    ), //o
    .io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation              (streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation[5:0]             ), //o
    .io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_UID                                     (streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_UID[18:0]                                   ), //o
    .io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt                      (streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt[12:0]                    ), //o
    .io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt                      (streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt[12:0]                    ), //o
    .io_outputs_2_valid                                                                                          (streamDispatcher_1_io_outputs_2_valid                                                                                              ), //o
    .io_outputs_2_ready                                                                                          (io_matAfterSlicers_2_ready                                                                                                         ), //i
    .io_outputs_2_payload_last                                                                                   (streamDispatcher_1_io_outputs_2_payload_last                                                                                       ), //o
    .io_outputs_2_payload_fragment_A_0                                                                           (streamDispatcher_1_io_outputs_2_payload_fragment_A_0[8:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_A_1                                                                           (streamDispatcher_1_io_outputs_2_payload_fragment_A_1[8:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_A_2                                                                           (streamDispatcher_1_io_outputs_2_payload_fragment_A_2[8:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_A_3                                                                           (streamDispatcher_1_io_outputs_2_payload_fragment_A_3[8:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_A_4                                                                           (streamDispatcher_1_io_outputs_2_payload_fragment_A_4[8:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_B_0                                                                           (streamDispatcher_1_io_outputs_2_payload_fragment_B_0[6:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_B_1                                                                           (streamDispatcher_1_io_outputs_2_payload_fragment_B_1[6:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_B_2                                                                           (streamDispatcher_1_io_outputs_2_payload_fragment_B_2[6:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_B_3                                                                           (streamDispatcher_1_io_outputs_2_payload_fragment_B_3[6:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_B_4                                                                           (streamDispatcher_1_io_outputs_2_payload_fragment_B_4[6:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation                (streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation[1:0]               ), //o
    .io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation (streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation[5:0]), //o
    .io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                    (streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                        ), //o
    .io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction                     (streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction[2:0]                    ), //o
    .io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation              (streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation[5:0]             ), //o
    .io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_UID                                     (streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_UID[18:0]                                   ), //o
    .io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt                      (streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt[12:0]                    ), //o
    .io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt                      (streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt[12:0]                    ), //o
    .clk                                                                                                         (clk                                                                                                                                ), //i
    .reset                                                                                                       (reset                                                                                                                              )  //i
  );
  always @(*) begin
    case(matABSubSendRowCnt_cnt)
      3'b000 : begin
        _zz_matAfterSlicer_payload_A_0 = matASub_0_0;
        _zz_matAfterSlicer_payload_A_1 = matASub_1_0;
        _zz_matAfterSlicer_payload_A_2 = matASub_2_0;
        _zz_matAfterSlicer_payload_A_3 = matASub_3_0;
        _zz_matAfterSlicer_payload_A_4 = matASub_4_0;
        _zz_matAfterSlicer_payload_B_0_1 = matBSub_0_0;
        _zz_matAfterSlicer_payload_B_0_3 = matBSub_4_0;
        _zz_matAfterSlicer_payload_B_1_1 = matBSub_0_1;
        _zz_matAfterSlicer_payload_B_1_3 = matBSub_3_0;
        _zz_matAfterSlicer_payload_B_2_1 = matBSub_0_2;
        _zz_matAfterSlicer_payload_B_2_3 = matBSub_2_0;
        _zz_matAfterSlicer_payload_B_3_1 = matBSub_0_3;
        _zz_matAfterSlicer_payload_B_3_3 = matBSub_1_0;
        _zz_matAfterSlicer_payload_B_4_1 = matBSub_0_4;
        _zz_matAfterSlicer_payload_B_4_3 = matBSub_0_0;
      end
      3'b001 : begin
        _zz_matAfterSlicer_payload_A_0 = matASub_0_1;
        _zz_matAfterSlicer_payload_A_1 = matASub_1_1;
        _zz_matAfterSlicer_payload_A_2 = matASub_2_1;
        _zz_matAfterSlicer_payload_A_3 = matASub_3_1;
        _zz_matAfterSlicer_payload_A_4 = matASub_4_1;
        _zz_matAfterSlicer_payload_B_0_1 = matBSub_1_0;
        _zz_matAfterSlicer_payload_B_0_3 = matBSub_4_1;
        _zz_matAfterSlicer_payload_B_1_1 = matBSub_1_1;
        _zz_matAfterSlicer_payload_B_1_3 = matBSub_3_1;
        _zz_matAfterSlicer_payload_B_2_1 = matBSub_1_2;
        _zz_matAfterSlicer_payload_B_2_3 = matBSub_2_1;
        _zz_matAfterSlicer_payload_B_3_1 = matBSub_1_3;
        _zz_matAfterSlicer_payload_B_3_3 = matBSub_1_1;
        _zz_matAfterSlicer_payload_B_4_1 = matBSub_1_4;
        _zz_matAfterSlicer_payload_B_4_3 = matBSub_0_1;
      end
      3'b010 : begin
        _zz_matAfterSlicer_payload_A_0 = matASub_0_2;
        _zz_matAfterSlicer_payload_A_1 = matASub_1_2;
        _zz_matAfterSlicer_payload_A_2 = matASub_2_2;
        _zz_matAfterSlicer_payload_A_3 = matASub_3_2;
        _zz_matAfterSlicer_payload_A_4 = matASub_4_2;
        _zz_matAfterSlicer_payload_B_0_1 = matBSub_2_0;
        _zz_matAfterSlicer_payload_B_0_3 = matBSub_4_2;
        _zz_matAfterSlicer_payload_B_1_1 = matBSub_2_1;
        _zz_matAfterSlicer_payload_B_1_3 = matBSub_3_2;
        _zz_matAfterSlicer_payload_B_2_1 = matBSub_2_2;
        _zz_matAfterSlicer_payload_B_2_3 = matBSub_2_2;
        _zz_matAfterSlicer_payload_B_3_1 = matBSub_2_3;
        _zz_matAfterSlicer_payload_B_3_3 = matBSub_1_2;
        _zz_matAfterSlicer_payload_B_4_1 = matBSub_2_4;
        _zz_matAfterSlicer_payload_B_4_3 = matBSub_0_2;
      end
      3'b011 : begin
        _zz_matAfterSlicer_payload_A_0 = matASub_0_3;
        _zz_matAfterSlicer_payload_A_1 = matASub_1_3;
        _zz_matAfterSlicer_payload_A_2 = matASub_2_3;
        _zz_matAfterSlicer_payload_A_3 = matASub_3_3;
        _zz_matAfterSlicer_payload_A_4 = matASub_4_3;
        _zz_matAfterSlicer_payload_B_0_1 = matBSub_3_0;
        _zz_matAfterSlicer_payload_B_0_3 = matBSub_4_3;
        _zz_matAfterSlicer_payload_B_1_1 = matBSub_3_1;
        _zz_matAfterSlicer_payload_B_1_3 = matBSub_3_3;
        _zz_matAfterSlicer_payload_B_2_1 = matBSub_3_2;
        _zz_matAfterSlicer_payload_B_2_3 = matBSub_2_3;
        _zz_matAfterSlicer_payload_B_3_1 = matBSub_3_3;
        _zz_matAfterSlicer_payload_B_3_3 = matBSub_1_3;
        _zz_matAfterSlicer_payload_B_4_1 = matBSub_3_4;
        _zz_matAfterSlicer_payload_B_4_3 = matBSub_0_3;
      end
      default : begin
        _zz_matAfterSlicer_payload_A_0 = matASub_0_4;
        _zz_matAfterSlicer_payload_A_1 = matASub_1_4;
        _zz_matAfterSlicer_payload_A_2 = matASub_2_4;
        _zz_matAfterSlicer_payload_A_3 = matASub_3_4;
        _zz_matAfterSlicer_payload_A_4 = matASub_4_4;
        _zz_matAfterSlicer_payload_B_0_1 = matBSub_4_0;
        _zz_matAfterSlicer_payload_B_0_3 = matBSub_4_4;
        _zz_matAfterSlicer_payload_B_1_1 = matBSub_4_1;
        _zz_matAfterSlicer_payload_B_1_3 = matBSub_3_4;
        _zz_matAfterSlicer_payload_B_2_1 = matBSub_4_2;
        _zz_matAfterSlicer_payload_B_2_3 = matBSub_2_4;
        _zz_matAfterSlicer_payload_B_3_1 = matBSub_4_3;
        _zz_matAfterSlicer_payload_B_3_3 = matBSub_1_4;
        _zz_matAfterSlicer_payload_B_4_1 = matBSub_4_4;
        _zz_matAfterSlicer_payload_B_4_3 = matBSub_0_4;
      end
    endcase
  end

  `ifndef SYNTHESIS
  always @(*) begin
    case(io_inst_payload_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_inst_payload_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_inst_payload_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_inst_payload_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_inst_payload_matrixOperation_string = "ElementMax";
      default : io_inst_payload_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_inst_payload_activationFunction)
      Activation_TypeDef_Exp : io_inst_payload_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_inst_payload_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_inst_payload_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_inst_payload_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_inst_payload_activationFunction_string = "None    ";
      default : io_inst_payload_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(instReg_matrixOperation)
      MatrixOperation_TypeDef_MatMul : instReg_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : instReg_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : instReg_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : instReg_matrixOperation_string = "ElementMax";
      default : instReg_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(instReg_activationFunction)
      Activation_TypeDef_Exp : instReg_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : instReg_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : instReg_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : instReg_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : instReg_activationFunction_string = "None    ";
      default : instReg_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(matAfterSlicer_payload_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : matAfterSlicer_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : matAfterSlicer_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : matAfterSlicer_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : matAfterSlicer_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : matAfterSlicer_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : matAfterSlicer_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(_zz_io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : _zz_io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : _zz_io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : _zz_io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : _zz_io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : _zz_io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(_zz_io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : _zz_io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : _zz_io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : _zz_io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : _zz_io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : _zz_io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : _zz_io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(_zz_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : _zz_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : _zz_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : _zz_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : _zz_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : _zz_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(_zz_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : _zz_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : _zz_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : _zz_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : _zz_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : _zz_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : _zz_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(_zz_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : _zz_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : _zz_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : _zz_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : _zz_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : _zz_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(_zz_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : _zz_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : _zz_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : _zz_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : _zz_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : _zz_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : _zz_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(_zz_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : _zz_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : _zz_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : _zz_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : _zz_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : _zz_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(_zz_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : _zz_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : _zz_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : _zz_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : _zz_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : _zz_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : _zz_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  `endif

  assign isMatMul = (instReg_matrixOperation == MatrixOperation_TypeDef_MatMul);
  assign io_inst_fire = (io_inst_valid && io_inst_ready);
  assign io_slicedInst_payload_UID = instReg_UID;
  assign io_slicedInst_payload_doTranspose = instReg_doTranspose;
  assign io_slicedInst_payload_outputAddress = instReg_outputAddress;
  always @(*) begin
    _zz_io_slicedInst_payload_outputShape_0 = 15'bxxxxxxxxxxxxxxx;
    case(instReg_matrixOperation)
      MatrixOperation_TypeDef_MatMul : begin
        if(instReg_doTranspose) begin
          _zz_io_slicedInst_payload_outputShape_0 = instReg_input1Shape_1;
        end else begin
          _zz_io_slicedInst_payload_outputShape_0 = instReg_input0Shape_0;
        end
      end
      default : begin
        if(instReg_doTranspose) begin
          _zz_io_slicedInst_payload_outputShape_0 = instReg_input0Shape_1;
        end else begin
          _zz_io_slicedInst_payload_outputShape_0 = instReg_input0Shape_0;
        end
      end
    endcase
  end

  always @(*) begin
    _zz_io_slicedInst_payload_outputShape_1 = 15'bxxxxxxxxxxxxxxx;
    case(instReg_matrixOperation)
      MatrixOperation_TypeDef_MatMul : begin
        if(instReg_doTranspose) begin
          _zz_io_slicedInst_payload_outputShape_1 = instReg_input0Shape_0;
        end else begin
          _zz_io_slicedInst_payload_outputShape_1 = instReg_input1Shape_1;
        end
      end
      default : begin
        if(instReg_doTranspose) begin
          _zz_io_slicedInst_payload_outputShape_1 = instReg_input0Shape_0;
        end else begin
          _zz_io_slicedInst_payload_outputShape_1 = instReg_input0Shape_1;
        end
      end
    endcase
  end

  assign io_slicedInst_payload_outputShape_0 = _zz_io_slicedInst_payload_outputShape_0;
  assign io_slicedInst_payload_outputShape_1 = _zz_io_slicedInst_payload_outputShape_1;
  assign io_slicedInst_fire = (io_slicedInst_valid && io_slicedInst_ready);
  assign matAColSliceCnt_willOverflowIfInc = (matAColSliceCnt_cnt == _zz_matAColSliceCnt_willOverflowIfInc);
  assign matAColSliceCnt_willOverflow = (matABSubSendFinish && matAColSliceCnt_willOverflowIfInc);
  assign matAColSliceCnt_valueNext = (matAColSliceCnt_willOverflowIfInc ? 15'h0 : _zz_matAColSliceCnt_valueNext);
  assign matBColSliceCnt_willOverflowIfInc = (matBColSliceCnt_cnt == _zz_matBColSliceCnt_willOverflowIfInc);
  assign matBColSliceCnt_willOverflow = (matAColSliceCnt_willOverflow && matBColSliceCnt_willOverflowIfInc);
  assign matBColSliceCnt_valueNext = (matBColSliceCnt_willOverflowIfInc ? 15'h0 : _zz_matBColSliceCnt_valueNext);
  assign matARowSliceCnt_willOverflowIfInc = (matARowSliceCnt_cnt == _zz_matARowSliceCnt_willOverflowIfInc);
  assign matARowSliceCnt_willOverflow = (matBColSliceCnt_willOverflow && matARowSliceCnt_willOverflowIfInc);
  assign matARowSliceCnt_valueNext = (matARowSliceCnt_willOverflowIfInc ? 15'h0 : _zz_matARowSliceCnt_valueNext);
  assign matASubReadRowCnt_willOverflowIfInc = (matASubReadRowCnt_cnt == 3'b100);
  assign matASubReadRowCnt_willOverflow = (io_memoryReadPortA_Valid && matASubReadRowCnt_willOverflowIfInc);
  assign matASubReadRowCnt_valueNext = (matASubReadRowCnt_willOverflowIfInc ? 3'b000 : _zz_matASubReadRowCnt_valueNext);
  assign io_memoryReadPortA_Valid = ((matASubReadRowCnt_cnt == 3'b000) ? (io_slicedInst_fire || _zz_io_memoryReadPortA_Valid) : 1'b1);
  assign io_memoryReadPortA_Address = (_zz_io_memoryReadPortA_Address + _zz_io_memoryReadPortA_Address_7);
  assign _zz_1 = ({7'd0,1'b1} <<< matASubReadRowCnt_cnt_regNext);
  assign _zz_2 = _zz_1[0];
  assign _zz_3 = _zz_1[1];
  assign _zz_4 = _zz_1[2];
  assign _zz_5 = _zz_1[3];
  assign _zz_6 = _zz_1[4];
  assign _zz_matASub_0_0 = io_memoryReadPortA_Data[8 : 0];
  assign _zz_matASub_0_1 = io_memoryReadPortA_Data[17 : 9];
  assign _zz_matASub_0_2 = io_memoryReadPortA_Data[26 : 18];
  assign _zz_matASub_0_3 = io_memoryReadPortA_Data[35 : 27];
  assign _zz_matASub_0_4 = io_memoryReadPortA_Data[44 : 36];
  assign matBSubReadRowCnt_willOverflowIfInc = (matBSubReadRowCnt_cnt == 3'b100);
  assign matBSubReadRowCnt_willOverflow = (io_memoryReadPortB_Valid && matBSubReadRowCnt_willOverflowIfInc);
  assign matBSubReadRowCnt_valueNext = (matBSubReadRowCnt_willOverflowIfInc ? 3'b000 : _zz_matBSubReadRowCnt_valueNext);
  assign io_memoryReadPortB_Valid = ((matBSubReadRowCnt_cnt == 3'b000) ? (io_slicedInst_fire || _zz_io_memoryReadPortB_Valid) : 1'b1);
  assign io_memoryReadPortB_Address = (_zz_io_memoryReadPortB_Address + _zz_io_memoryReadPortB_Address_7);
  assign _zz_7 = ({7'd0,1'b1} <<< matASubReadRowCnt_cnt_regNext_1);
  assign _zz_8 = _zz_7[0];
  assign _zz_9 = _zz_7[1];
  assign _zz_10 = _zz_7[2];
  assign _zz_11 = _zz_7[3];
  assign _zz_12 = _zz_7[4];
  assign _zz_matBSub_0_0 = io_memoryReadPortB_Data[6 : 0];
  assign _zz_matBSub_0_1 = io_memoryReadPortB_Data[13 : 7];
  assign _zz_matBSub_0_2 = io_memoryReadPortB_Data[20 : 14];
  assign _zz_matBSub_0_3 = io_memoryReadPortB_Data[27 : 21];
  assign _zz_matBSub_0_4 = io_memoryReadPortB_Data[34 : 28];
  assign matInSubReadFinish = (matASubReadFinish && matBSubReadFinish);
  assign matAfterSlicer_fire = (matAfterSlicer_valid && matAfterSlicer_ready);
  assign matABSubSendRowCnt_willOverflowIfInc = (matABSubSendRowCnt_cnt == 3'b100);
  assign matABSubSendRowCnt_willOverflow = (matAfterSlicer_fire && matABSubSendRowCnt_willOverflowIfInc);
  assign matABSubSendRowCnt_valueNext = (matABSubSendRowCnt_willOverflowIfInc ? 3'b000 : _zz_matABSubSendRowCnt_valueNext);
  assign matABSubSendFinish = matABSubSendRowCnt_willOverflow;
  assign instFinish = (((matABSubSendFinish && matAColSliceCnt_willOverflowIfInc) && matBColSliceCnt_willOverflowIfInc) && matARowSliceCnt_willOverflowIfInc);
  assign matAfterSlicer_payload_A_0 = _zz_matAfterSlicer_payload_A_0;
  assign matAfterSlicer_payload_A_1 = _zz_matAfterSlicer_payload_A_1;
  assign matAfterSlicer_payload_A_2 = _zz_matAfterSlicer_payload_A_2;
  assign matAfterSlicer_payload_A_3 = _zz_matAfterSlicer_payload_A_3;
  assign matAfterSlicer_payload_A_4 = _zz_matAfterSlicer_payload_A_4;
  assign matAfterSlicer_payload_B_0 = (isMatMul ? _zz_matAfterSlicer_payload_B_0 : _zz_matAfterSlicer_payload_B_0_2);
  assign matAfterSlicer_payload_B_1 = (isMatMul ? _zz_matAfterSlicer_payload_B_1 : _zz_matAfterSlicer_payload_B_1_2);
  assign matAfterSlicer_payload_B_2 = (isMatMul ? _zz_matAfterSlicer_payload_B_2 : _zz_matAfterSlicer_payload_B_2_2);
  assign matAfterSlicer_payload_B_3 = (isMatMul ? _zz_matAfterSlicer_payload_B_3 : _zz_matAfterSlicer_payload_B_3_2);
  assign matAfterSlicer_payload_B_4 = (isMatMul ? _zz_matAfterSlicer_payload_B_4 : _zz_matAfterSlicer_payload_B_4_2);
  assign matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = instReg_matrixOperation;
  assign matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation = instReg_shiftLeft_AfterMatrixOperation;
  assign matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose = instReg_doTranspose;
  assign matAfterSlicer_payload_CoreInstruction_Activation_Instruction_activationFunction = instReg_activationFunction;
  assign matAfterSlicer_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation = instReg_shiftLeft_AfterActivation;
  assign matAfterSlicer_payload_CoreInstruction_Collector_Instruction_UID = instReg_UID;
  assign matAfterSlicer_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt = matARowSliceCnt_cnt[12:0];
  assign matAfterSlicer_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt = matBColSliceCnt_cnt[12:0];
  assign matAfterSlicer_payload_Final = (matABSubSendRowCnt_willOverflowIfInc && matAColSliceCnt_willOverflowIfInc);
  assign _zz_io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = matAfterSlicer_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign _zz_io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction = matAfterSlicer_payload_CoreInstruction_Activation_Instruction_activationFunction;
  assign matAfterSlicer_ready = streamDispatcher_1_io_input_ready;
  assign _zz_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign _zz_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction = streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  assign _zz_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign _zz_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction = streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  assign _zz_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign _zz_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction = streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  assign io_matAfterSlicers_0_valid = streamDispatcher_1_io_outputs_0_valid;
  assign io_matAfterSlicers_0_payload_A_0 = streamDispatcher_1_io_outputs_0_payload_fragment_A_0;
  assign io_matAfterSlicers_0_payload_A_1 = streamDispatcher_1_io_outputs_0_payload_fragment_A_1;
  assign io_matAfterSlicers_0_payload_A_2 = streamDispatcher_1_io_outputs_0_payload_fragment_A_2;
  assign io_matAfterSlicers_0_payload_A_3 = streamDispatcher_1_io_outputs_0_payload_fragment_A_3;
  assign io_matAfterSlicers_0_payload_A_4 = streamDispatcher_1_io_outputs_0_payload_fragment_A_4;
  assign io_matAfterSlicers_0_payload_B_0 = streamDispatcher_1_io_outputs_0_payload_fragment_B_0;
  assign io_matAfterSlicers_0_payload_B_1 = streamDispatcher_1_io_outputs_0_payload_fragment_B_1;
  assign io_matAfterSlicers_0_payload_B_2 = streamDispatcher_1_io_outputs_0_payload_fragment_B_2;
  assign io_matAfterSlicers_0_payload_B_3 = streamDispatcher_1_io_outputs_0_payload_fragment_B_3;
  assign io_matAfterSlicers_0_payload_B_4 = streamDispatcher_1_io_outputs_0_payload_fragment_B_4;
  assign io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = _zz_io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation = streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  assign io_matAfterSlicers_0_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose = streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  assign io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction = _zz_io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_activationFunction;
  assign io_matAfterSlicers_0_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation = streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  assign io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_UID = streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  assign io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt = streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  assign io_matAfterSlicers_0_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt = streamDispatcher_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  assign io_matAfterSlicers_0_payload_Final = streamDispatcher_1_io_outputs_0_payload_last;
  assign io_matAfterSlicers_1_valid = streamDispatcher_1_io_outputs_1_valid;
  assign io_matAfterSlicers_1_payload_A_0 = streamDispatcher_1_io_outputs_1_payload_fragment_A_0;
  assign io_matAfterSlicers_1_payload_A_1 = streamDispatcher_1_io_outputs_1_payload_fragment_A_1;
  assign io_matAfterSlicers_1_payload_A_2 = streamDispatcher_1_io_outputs_1_payload_fragment_A_2;
  assign io_matAfterSlicers_1_payload_A_3 = streamDispatcher_1_io_outputs_1_payload_fragment_A_3;
  assign io_matAfterSlicers_1_payload_A_4 = streamDispatcher_1_io_outputs_1_payload_fragment_A_4;
  assign io_matAfterSlicers_1_payload_B_0 = streamDispatcher_1_io_outputs_1_payload_fragment_B_0;
  assign io_matAfterSlicers_1_payload_B_1 = streamDispatcher_1_io_outputs_1_payload_fragment_B_1;
  assign io_matAfterSlicers_1_payload_B_2 = streamDispatcher_1_io_outputs_1_payload_fragment_B_2;
  assign io_matAfterSlicers_1_payload_B_3 = streamDispatcher_1_io_outputs_1_payload_fragment_B_3;
  assign io_matAfterSlicers_1_payload_B_4 = streamDispatcher_1_io_outputs_1_payload_fragment_B_4;
  assign io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = _zz_io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation = streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  assign io_matAfterSlicers_1_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose = streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  assign io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction = _zz_io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_activationFunction;
  assign io_matAfterSlicers_1_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation = streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  assign io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_UID = streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  assign io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt = streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  assign io_matAfterSlicers_1_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt = streamDispatcher_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  assign io_matAfterSlicers_1_payload_Final = streamDispatcher_1_io_outputs_1_payload_last;
  assign io_matAfterSlicers_2_valid = streamDispatcher_1_io_outputs_2_valid;
  assign io_matAfterSlicers_2_payload_A_0 = streamDispatcher_1_io_outputs_2_payload_fragment_A_0;
  assign io_matAfterSlicers_2_payload_A_1 = streamDispatcher_1_io_outputs_2_payload_fragment_A_1;
  assign io_matAfterSlicers_2_payload_A_2 = streamDispatcher_1_io_outputs_2_payload_fragment_A_2;
  assign io_matAfterSlicers_2_payload_A_3 = streamDispatcher_1_io_outputs_2_payload_fragment_A_3;
  assign io_matAfterSlicers_2_payload_A_4 = streamDispatcher_1_io_outputs_2_payload_fragment_A_4;
  assign io_matAfterSlicers_2_payload_B_0 = streamDispatcher_1_io_outputs_2_payload_fragment_B_0;
  assign io_matAfterSlicers_2_payload_B_1 = streamDispatcher_1_io_outputs_2_payload_fragment_B_1;
  assign io_matAfterSlicers_2_payload_B_2 = streamDispatcher_1_io_outputs_2_payload_fragment_B_2;
  assign io_matAfterSlicers_2_payload_B_3 = streamDispatcher_1_io_outputs_2_payload_fragment_B_3;
  assign io_matAfterSlicers_2_payload_B_4 = streamDispatcher_1_io_outputs_2_payload_fragment_B_4;
  assign io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = _zz_io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation = streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  assign io_matAfterSlicers_2_payload_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose = streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  assign io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction = _zz_io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_activationFunction;
  assign io_matAfterSlicers_2_payload_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation = streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  assign io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_UID = streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  assign io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt = streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  assign io_matAfterSlicers_2_payload_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt = streamDispatcher_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  assign io_matAfterSlicers_2_payload_Final = streamDispatcher_1_io_outputs_2_payload_last;
  always @(posedge clk or posedge reset) begin
    if(reset) begin
      io_inst_ready <= 1'b1;
      io_slicedInst_valid <= 1'b0;
      _zz_io_memoryReadPortA_Valid <= 1'b0;
      io_memoryReadPortA_Valid_regNext <= 1'b0;
      _zz_io_memoryReadPortB_Valid <= 1'b0;
      io_memoryReadPortB_Valid_regNext <= 1'b0;
      matASubReadFinish <= 1'b0;
      matBSubReadFinish <= 1'b0;
      matAfterSlicer_valid <= 1'b0;
    end else begin
      if(io_inst_fire) begin
        io_inst_ready <= 1'b0;
      end else begin
        if(instFinish) begin
          io_inst_ready <= 1'b1;
        end
      end
      if(io_slicedInst_fire) begin
        io_slicedInst_valid <= 1'b0;
      end else begin
        if(io_inst_fire) begin
          io_slicedInst_valid <= 1'b1;
        end
      end
      _zz_io_memoryReadPortA_Valid <= (matABSubSendFinish && (! instFinish));
      io_memoryReadPortA_Valid_regNext <= io_memoryReadPortA_Valid;
      _zz_io_memoryReadPortB_Valid <= (matABSubSendFinish && (! instFinish));
      io_memoryReadPortB_Valid_regNext <= io_memoryReadPortB_Valid;
      if(matInSubReadFinish) begin
        matASubReadFinish <= 1'b0;
        matBSubReadFinish <= 1'b0;
      end else begin
        if(matASubReadRowCnt_willOverflow) begin
          matASubReadFinish <= 1'b1;
        end
        if(matBSubReadRowCnt_willOverflow) begin
          matBSubReadFinish <= 1'b1;
        end
      end
      if(matInSubReadFinish) begin
        matAfterSlicer_valid <= 1'b1;
      end else begin
        if(matABSubSendFinish) begin
          matAfterSlicer_valid <= 1'b0;
        end
      end
    end
  end

  always @(posedge clk) begin
    if(io_inst_fire) begin
      instReg_UID <= io_inst_payload_UID;
      instReg_matrixOperation <= io_inst_payload_matrixOperation;
      instReg_shiftLeft_AfterMatrixOperation <= io_inst_payload_shiftLeft_AfterMatrixOperation;
      instReg_doTranspose <= io_inst_payload_doTranspose;
      instReg_activationFunction <= io_inst_payload_activationFunction;
      instReg_shiftLeft_AfterActivation <= io_inst_payload_shiftLeft_AfterActivation;
      instReg_input0Address <= io_inst_payload_input0Address;
      instReg_input0Shape_0 <= io_inst_payload_input0Shape_0;
      instReg_input0Shape_1 <= io_inst_payload_input0Shape_1;
      instReg_input1Address <= io_inst_payload_input1Address;
      instReg_input1Shape_0 <= io_inst_payload_input1Shape_0;
      instReg_input1Shape_1 <= io_inst_payload_input1Shape_1;
      instReg_outputAddress <= io_inst_payload_outputAddress;
    end
    if(io_inst_fire) begin
      matAColSliceCnt_cnt <= 15'h0;
    end else begin
      if(matABSubSendFinish) begin
        matAColSliceCnt_cnt <= matAColSliceCnt_valueNext;
      end
    end
    if(io_inst_fire) begin
      matBColSliceCnt_cnt <= 15'h0;
    end else begin
      if(matAColSliceCnt_willOverflow) begin
        matBColSliceCnt_cnt <= matBColSliceCnt_valueNext;
      end
    end
    if(io_inst_fire) begin
      matARowSliceCnt_cnt <= 15'h0;
    end else begin
      if(matBColSliceCnt_willOverflow) begin
        matARowSliceCnt_cnt <= matARowSliceCnt_valueNext;
      end
    end
    if(io_inst_fire) begin
      matASubReadRowCnt_cnt <= 3'b000;
    end else begin
      if(io_memoryReadPortA_Valid) begin
        matASubReadRowCnt_cnt <= matASubReadRowCnt_valueNext;
      end
    end
    if(io_memoryReadPortA_Valid_regNext) begin
      if(_zz_2) begin
        matASub_0_0 <= _zz_matASub_0_0;
      end
      if(_zz_3) begin
        matASub_1_0 <= _zz_matASub_0_0;
      end
      if(_zz_4) begin
        matASub_2_0 <= _zz_matASub_0_0;
      end
      if(_zz_5) begin
        matASub_3_0 <= _zz_matASub_0_0;
      end
      if(_zz_6) begin
        matASub_4_0 <= _zz_matASub_0_0;
      end
      if(_zz_2) begin
        matASub_0_1 <= _zz_matASub_0_1;
      end
      if(_zz_3) begin
        matASub_1_1 <= _zz_matASub_0_1;
      end
      if(_zz_4) begin
        matASub_2_1 <= _zz_matASub_0_1;
      end
      if(_zz_5) begin
        matASub_3_1 <= _zz_matASub_0_1;
      end
      if(_zz_6) begin
        matASub_4_1 <= _zz_matASub_0_1;
      end
      if(_zz_2) begin
        matASub_0_2 <= _zz_matASub_0_2;
      end
      if(_zz_3) begin
        matASub_1_2 <= _zz_matASub_0_2;
      end
      if(_zz_4) begin
        matASub_2_2 <= _zz_matASub_0_2;
      end
      if(_zz_5) begin
        matASub_3_2 <= _zz_matASub_0_2;
      end
      if(_zz_6) begin
        matASub_4_2 <= _zz_matASub_0_2;
      end
      if(_zz_2) begin
        matASub_0_3 <= _zz_matASub_0_3;
      end
      if(_zz_3) begin
        matASub_1_3 <= _zz_matASub_0_3;
      end
      if(_zz_4) begin
        matASub_2_3 <= _zz_matASub_0_3;
      end
      if(_zz_5) begin
        matASub_3_3 <= _zz_matASub_0_3;
      end
      if(_zz_6) begin
        matASub_4_3 <= _zz_matASub_0_3;
      end
      if(_zz_2) begin
        matASub_0_4 <= _zz_matASub_0_4;
      end
      if(_zz_3) begin
        matASub_1_4 <= _zz_matASub_0_4;
      end
      if(_zz_4) begin
        matASub_2_4 <= _zz_matASub_0_4;
      end
      if(_zz_5) begin
        matASub_3_4 <= _zz_matASub_0_4;
      end
      if(_zz_6) begin
        matASub_4_4 <= _zz_matASub_0_4;
      end
    end
    if(io_inst_fire) begin
      matBSubReadRowCnt_cnt <= 3'b000;
    end else begin
      if(io_memoryReadPortB_Valid) begin
        matBSubReadRowCnt_cnt <= matBSubReadRowCnt_valueNext;
      end
    end
    if(io_memoryReadPortB_Valid_regNext) begin
      if(_zz_8) begin
        matBSub_0_0 <= _zz_matBSub_0_0;
      end
      if(_zz_9) begin
        matBSub_1_0 <= _zz_matBSub_0_0;
      end
      if(_zz_10) begin
        matBSub_2_0 <= _zz_matBSub_0_0;
      end
      if(_zz_11) begin
        matBSub_3_0 <= _zz_matBSub_0_0;
      end
      if(_zz_12) begin
        matBSub_4_0 <= _zz_matBSub_0_0;
      end
      if(_zz_8) begin
        matBSub_0_1 <= _zz_matBSub_0_1;
      end
      if(_zz_9) begin
        matBSub_1_1 <= _zz_matBSub_0_1;
      end
      if(_zz_10) begin
        matBSub_2_1 <= _zz_matBSub_0_1;
      end
      if(_zz_11) begin
        matBSub_3_1 <= _zz_matBSub_0_1;
      end
      if(_zz_12) begin
        matBSub_4_1 <= _zz_matBSub_0_1;
      end
      if(_zz_8) begin
        matBSub_0_2 <= _zz_matBSub_0_2;
      end
      if(_zz_9) begin
        matBSub_1_2 <= _zz_matBSub_0_2;
      end
      if(_zz_10) begin
        matBSub_2_2 <= _zz_matBSub_0_2;
      end
      if(_zz_11) begin
        matBSub_3_2 <= _zz_matBSub_0_2;
      end
      if(_zz_12) begin
        matBSub_4_2 <= _zz_matBSub_0_2;
      end
      if(_zz_8) begin
        matBSub_0_3 <= _zz_matBSub_0_3;
      end
      if(_zz_9) begin
        matBSub_1_3 <= _zz_matBSub_0_3;
      end
      if(_zz_10) begin
        matBSub_2_3 <= _zz_matBSub_0_3;
      end
      if(_zz_11) begin
        matBSub_3_3 <= _zz_matBSub_0_3;
      end
      if(_zz_12) begin
        matBSub_4_3 <= _zz_matBSub_0_3;
      end
      if(_zz_8) begin
        matBSub_0_4 <= _zz_matBSub_0_4;
      end
      if(_zz_9) begin
        matBSub_1_4 <= _zz_matBSub_0_4;
      end
      if(_zz_10) begin
        matBSub_2_4 <= _zz_matBSub_0_4;
      end
      if(_zz_11) begin
        matBSub_3_4 <= _zz_matBSub_0_4;
      end
      if(_zz_12) begin
        matBSub_4_4 <= _zz_matBSub_0_4;
      end
    end
    if(io_inst_fire) begin
      matABSubSendRowCnt_cnt <= 3'b000;
    end else begin
      if(matAfterSlicer_fire) begin
        matABSubSendRowCnt_cnt <= matABSubSendRowCnt_valueNext;
      end
    end
  end

  always @(posedge clk) begin
    matASubReadRowCnt_cnt_regNext <= matASubReadRowCnt_cnt;
  end

  always @(posedge clk) begin
    matASubReadRowCnt_cnt_regNext_1 <= matASubReadRowCnt_cnt;
  end


endmodule

module Sdpram_1 (
  input  wire          io_read_Valid,
  input  wire [16:0]   io_read_Address,
  output wire [34:0]   io_read_Data,
  input  wire          io_write_Valid,
  input  wire [16:0]   io_write_Address,
  input  wire [34:0]   io_write_Data,
  input  wire          clk,
  input  wire          reset
);

  reg        [34:0]   mem_spinal_port0;
  reg [34:0] mem [0:131071];

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

module Sdpram (
  input  wire          io_read_Valid,
  input  wire [16:0]   io_read_Address,
  output wire [44:0]   io_read_Data,
  input  wire          io_write_Valid,
  input  wire [16:0]   io_write_Address,
  input  wire [44:0]   io_write_Data,
  input  wire          clk,
  input  wire          reset
);

  reg        [44:0]   mem_spinal_port0;
  reg [44:0] mem [0:131071];

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

module StreamDispatcher (
  input  wire          io_input_valid,
  output wire          io_input_ready,
  input  wire          io_input_payload_last,
  input  wire [8:0]    io_input_payload_fragment_A_0,
  input  wire [8:0]    io_input_payload_fragment_A_1,
  input  wire [8:0]    io_input_payload_fragment_A_2,
  input  wire [8:0]    io_input_payload_fragment_A_3,
  input  wire [8:0]    io_input_payload_fragment_A_4,
  input  wire [6:0]    io_input_payload_fragment_B_0,
  input  wire [6:0]    io_input_payload_fragment_B_1,
  input  wire [6:0]    io_input_payload_fragment_B_2,
  input  wire [6:0]    io_input_payload_fragment_B_3,
  input  wire [6:0]    io_input_payload_fragment_B_4,
  input  wire [1:0]    io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  input  wire [5:0]    io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  input  wire          io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  input  wire [2:0]    io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction,
  input  wire [5:0]    io_input_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  input  wire [18:0]   io_input_payload_fragment_CoreInstruction_Collector_Instruction_UID,
  input  wire [12:0]   io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  input  wire [12:0]   io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt,
  output wire          io_outputs_0_valid,
  input  wire          io_outputs_0_ready,
  output wire          io_outputs_0_payload_last,
  output wire [8:0]    io_outputs_0_payload_fragment_A_0,
  output wire [8:0]    io_outputs_0_payload_fragment_A_1,
  output wire [8:0]    io_outputs_0_payload_fragment_A_2,
  output wire [8:0]    io_outputs_0_payload_fragment_A_3,
  output wire [8:0]    io_outputs_0_payload_fragment_A_4,
  output wire [6:0]    io_outputs_0_payload_fragment_B_0,
  output wire [6:0]    io_outputs_0_payload_fragment_B_1,
  output wire [6:0]    io_outputs_0_payload_fragment_B_2,
  output wire [6:0]    io_outputs_0_payload_fragment_B_3,
  output wire [6:0]    io_outputs_0_payload_fragment_B_4,
  output wire [1:0]    io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  output wire [5:0]    io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  output wire          io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  output wire [2:0]    io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction,
  output wire [5:0]    io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  output wire [18:0]   io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_UID,
  output wire [12:0]   io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  output wire [12:0]   io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt,
  output wire          io_outputs_1_valid,
  input  wire          io_outputs_1_ready,
  output wire          io_outputs_1_payload_last,
  output wire [8:0]    io_outputs_1_payload_fragment_A_0,
  output wire [8:0]    io_outputs_1_payload_fragment_A_1,
  output wire [8:0]    io_outputs_1_payload_fragment_A_2,
  output wire [8:0]    io_outputs_1_payload_fragment_A_3,
  output wire [8:0]    io_outputs_1_payload_fragment_A_4,
  output wire [6:0]    io_outputs_1_payload_fragment_B_0,
  output wire [6:0]    io_outputs_1_payload_fragment_B_1,
  output wire [6:0]    io_outputs_1_payload_fragment_B_2,
  output wire [6:0]    io_outputs_1_payload_fragment_B_3,
  output wire [6:0]    io_outputs_1_payload_fragment_B_4,
  output wire [1:0]    io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  output wire [5:0]    io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  output wire          io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  output wire [2:0]    io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction,
  output wire [5:0]    io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  output wire [18:0]   io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_UID,
  output wire [12:0]   io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  output wire [12:0]   io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt,
  output wire          io_outputs_2_valid,
  input  wire          io_outputs_2_ready,
  output wire          io_outputs_2_payload_last,
  output wire [8:0]    io_outputs_2_payload_fragment_A_0,
  output wire [8:0]    io_outputs_2_payload_fragment_A_1,
  output wire [8:0]    io_outputs_2_payload_fragment_A_2,
  output wire [8:0]    io_outputs_2_payload_fragment_A_3,
  output wire [8:0]    io_outputs_2_payload_fragment_A_4,
  output wire [6:0]    io_outputs_2_payload_fragment_B_0,
  output wire [6:0]    io_outputs_2_payload_fragment_B_1,
  output wire [6:0]    io_outputs_2_payload_fragment_B_2,
  output wire [6:0]    io_outputs_2_payload_fragment_B_3,
  output wire [6:0]    io_outputs_2_payload_fragment_B_4,
  output wire [1:0]    io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  output wire [5:0]    io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  output wire          io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  output wire [2:0]    io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction,
  output wire [5:0]    io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  output wire [18:0]   io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_UID,
  output wire [12:0]   io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  output wire [12:0]   io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt,
  input  wire          clk,
  input  wire          reset
);
  localparam MatrixOperation_TypeDef_MatMul = 2'd0;
  localparam MatrixOperation_TypeDef_ElementAdd = 2'd1;
  localparam MatrixOperation_TypeDef_ElementMul = 2'd2;
  localparam MatrixOperation_TypeDef_ElementMax = 2'd3;
  localparam Activation_TypeDef_Exp = 3'd0;
  localparam Activation_TypeDef_Log = 3'd1;
  localparam Activation_TypeDef_Softplus = 3'd2;
  localparam Activation_TypeDef_Relu = 3'd3;
  localparam Activation_TypeDef_None = 3'd4;

  wire                streamDemux_1_io_input_ready;
  wire                streamDemux_1_io_outputs_0_valid;
  wire                streamDemux_1_io_outputs_0_payload_last;
  wire       [8:0]    streamDemux_1_io_outputs_0_payload_fragment_A_0;
  wire       [8:0]    streamDemux_1_io_outputs_0_payload_fragment_A_1;
  wire       [8:0]    streamDemux_1_io_outputs_0_payload_fragment_A_2;
  wire       [8:0]    streamDemux_1_io_outputs_0_payload_fragment_A_3;
  wire       [8:0]    streamDemux_1_io_outputs_0_payload_fragment_A_4;
  wire       [6:0]    streamDemux_1_io_outputs_0_payload_fragment_B_0;
  wire       [6:0]    streamDemux_1_io_outputs_0_payload_fragment_B_1;
  wire       [6:0]    streamDemux_1_io_outputs_0_payload_fragment_B_2;
  wire       [6:0]    streamDemux_1_io_outputs_0_payload_fragment_B_3;
  wire       [6:0]    streamDemux_1_io_outputs_0_payload_fragment_B_4;
  wire       [1:0]    streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  wire       [5:0]    streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  wire                streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  wire       [2:0]    streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  wire       [5:0]    streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  wire       [18:0]   streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  wire       [12:0]   streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  wire                streamDemux_1_io_outputs_1_valid;
  wire                streamDemux_1_io_outputs_1_payload_last;
  wire       [8:0]    streamDemux_1_io_outputs_1_payload_fragment_A_0;
  wire       [8:0]    streamDemux_1_io_outputs_1_payload_fragment_A_1;
  wire       [8:0]    streamDemux_1_io_outputs_1_payload_fragment_A_2;
  wire       [8:0]    streamDemux_1_io_outputs_1_payload_fragment_A_3;
  wire       [8:0]    streamDemux_1_io_outputs_1_payload_fragment_A_4;
  wire       [6:0]    streamDemux_1_io_outputs_1_payload_fragment_B_0;
  wire       [6:0]    streamDemux_1_io_outputs_1_payload_fragment_B_1;
  wire       [6:0]    streamDemux_1_io_outputs_1_payload_fragment_B_2;
  wire       [6:0]    streamDemux_1_io_outputs_1_payload_fragment_B_3;
  wire       [6:0]    streamDemux_1_io_outputs_1_payload_fragment_B_4;
  wire       [1:0]    streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  wire       [5:0]    streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  wire                streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  wire       [2:0]    streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  wire       [5:0]    streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  wire       [18:0]   streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  wire       [12:0]   streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  wire                streamDemux_1_io_outputs_2_valid;
  wire                streamDemux_1_io_outputs_2_payload_last;
  wire       [8:0]    streamDemux_1_io_outputs_2_payload_fragment_A_0;
  wire       [8:0]    streamDemux_1_io_outputs_2_payload_fragment_A_1;
  wire       [8:0]    streamDemux_1_io_outputs_2_payload_fragment_A_2;
  wire       [8:0]    streamDemux_1_io_outputs_2_payload_fragment_A_3;
  wire       [8:0]    streamDemux_1_io_outputs_2_payload_fragment_A_4;
  wire       [6:0]    streamDemux_1_io_outputs_2_payload_fragment_B_0;
  wire       [6:0]    streamDemux_1_io_outputs_2_payload_fragment_B_1;
  wire       [6:0]    streamDemux_1_io_outputs_2_payload_fragment_B_2;
  wire       [6:0]    streamDemux_1_io_outputs_2_payload_fragment_B_3;
  wire       [6:0]    streamDemux_1_io_outputs_2_payload_fragment_B_4;
  wire       [1:0]    streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  wire       [5:0]    streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  wire                streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  wire       [2:0]    streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  wire       [5:0]    streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  wire       [18:0]   streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  wire       [12:0]   streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  wire       [12:0]   streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  wire       [5:0]    _zz__zz_grant_2;
  wire       [5:0]    _zz__zz_grant_2_1;
  wire       [2:0]    _zz__zz_grant_2_2;
  wire                io_input_fire;
  reg                 lock;
  wire       [2:0]    request;
  reg        [2:0]    priority_1;
  wire                when_StreamDispatcher_l23;
  wire       [2:0]    _zz_grant;
  wire       [5:0]    _zz_grant_1;
  wire       [5:0]    _zz_grant_2;
  wire       [2:0]    grant;
  wire                _zz_selectNoLock;
  wire                _zz_selectNoLock_1;
  wire       [1:0]    selectNoLock;
  wire                when_StreamDispatcher_l28;
  reg        [1:0]    selectNoLock_regNextWhen;
  wire       [1:0]    select_1;
  `ifndef SYNTHESIS
  reg [79:0] io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string;
  `endif


  assign _zz__zz_grant_2 = (_zz_grant_1 - _zz__zz_grant_2_1);
  assign _zz__zz_grant_2_2 = priority_1;
  assign _zz__zz_grant_2_1 = {3'd0, _zz__zz_grant_2_2};
  StreamDemux streamDemux_1 (
    .io_select                                                                                                   (select_1[1:0]                                                                                                                 ), //i
    .io_input_valid                                                                                              (io_input_valid                                                                                                                ), //i
    .io_input_ready                                                                                              (streamDemux_1_io_input_ready                                                                                                  ), //o
    .io_input_payload_last                                                                                       (io_input_payload_last                                                                                                         ), //i
    .io_input_payload_fragment_A_0                                                                               (io_input_payload_fragment_A_0[8:0]                                                                                            ), //i
    .io_input_payload_fragment_A_1                                                                               (io_input_payload_fragment_A_1[8:0]                                                                                            ), //i
    .io_input_payload_fragment_A_2                                                                               (io_input_payload_fragment_A_2[8:0]                                                                                            ), //i
    .io_input_payload_fragment_A_3                                                                               (io_input_payload_fragment_A_3[8:0]                                                                                            ), //i
    .io_input_payload_fragment_A_4                                                                               (io_input_payload_fragment_A_4[8:0]                                                                                            ), //i
    .io_input_payload_fragment_B_0                                                                               (io_input_payload_fragment_B_0[6:0]                                                                                            ), //i
    .io_input_payload_fragment_B_1                                                                               (io_input_payload_fragment_B_1[6:0]                                                                                            ), //i
    .io_input_payload_fragment_B_2                                                                               (io_input_payload_fragment_B_2[6:0]                                                                                            ), //i
    .io_input_payload_fragment_B_3                                                                               (io_input_payload_fragment_B_3[6:0]                                                                                            ), //i
    .io_input_payload_fragment_B_4                                                                               (io_input_payload_fragment_B_4[6:0]                                                                                            ), //i
    .io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation                    (io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation[1:0]                                 ), //i
    .io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation     (io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation[5:0]                  ), //i
    .io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                        (io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                                          ), //i
    .io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction                         (io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction[2:0]                                      ), //i
    .io_input_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation                  (io_input_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation[5:0]                               ), //i
    .io_input_payload_fragment_CoreInstruction_Collector_Instruction_UID                                         (io_input_payload_fragment_CoreInstruction_Collector_Instruction_UID[18:0]                                                     ), //i
    .io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt                          (io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt[12:0]                                      ), //i
    .io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt                          (io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt[12:0]                                      ), //i
    .io_outputs_0_valid                                                                                          (streamDemux_1_io_outputs_0_valid                                                                                              ), //o
    .io_outputs_0_ready                                                                                          (io_outputs_0_ready                                                                                                            ), //i
    .io_outputs_0_payload_last                                                                                   (streamDemux_1_io_outputs_0_payload_last                                                                                       ), //o
    .io_outputs_0_payload_fragment_A_0                                                                           (streamDemux_1_io_outputs_0_payload_fragment_A_0[8:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_A_1                                                                           (streamDemux_1_io_outputs_0_payload_fragment_A_1[8:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_A_2                                                                           (streamDemux_1_io_outputs_0_payload_fragment_A_2[8:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_A_3                                                                           (streamDemux_1_io_outputs_0_payload_fragment_A_3[8:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_A_4                                                                           (streamDemux_1_io_outputs_0_payload_fragment_A_4[8:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_B_0                                                                           (streamDemux_1_io_outputs_0_payload_fragment_B_0[6:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_B_1                                                                           (streamDemux_1_io_outputs_0_payload_fragment_B_1[6:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_B_2                                                                           (streamDemux_1_io_outputs_0_payload_fragment_B_2[6:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_B_3                                                                           (streamDemux_1_io_outputs_0_payload_fragment_B_3[6:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_B_4                                                                           (streamDemux_1_io_outputs_0_payload_fragment_B_4[6:0]                                                                          ), //o
    .io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation                (streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation[1:0]               ), //o
    .io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation (streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation[5:0]), //o
    .io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                    (streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                        ), //o
    .io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction                     (streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction[2:0]                    ), //o
    .io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation              (streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation[5:0]             ), //o
    .io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_UID                                     (streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_UID[18:0]                                   ), //o
    .io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt                      (streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt[12:0]                    ), //o
    .io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt                      (streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt[12:0]                    ), //o
    .io_outputs_1_valid                                                                                          (streamDemux_1_io_outputs_1_valid                                                                                              ), //o
    .io_outputs_1_ready                                                                                          (io_outputs_1_ready                                                                                                            ), //i
    .io_outputs_1_payload_last                                                                                   (streamDemux_1_io_outputs_1_payload_last                                                                                       ), //o
    .io_outputs_1_payload_fragment_A_0                                                                           (streamDemux_1_io_outputs_1_payload_fragment_A_0[8:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_A_1                                                                           (streamDemux_1_io_outputs_1_payload_fragment_A_1[8:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_A_2                                                                           (streamDemux_1_io_outputs_1_payload_fragment_A_2[8:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_A_3                                                                           (streamDemux_1_io_outputs_1_payload_fragment_A_3[8:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_A_4                                                                           (streamDemux_1_io_outputs_1_payload_fragment_A_4[8:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_B_0                                                                           (streamDemux_1_io_outputs_1_payload_fragment_B_0[6:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_B_1                                                                           (streamDemux_1_io_outputs_1_payload_fragment_B_1[6:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_B_2                                                                           (streamDemux_1_io_outputs_1_payload_fragment_B_2[6:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_B_3                                                                           (streamDemux_1_io_outputs_1_payload_fragment_B_3[6:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_B_4                                                                           (streamDemux_1_io_outputs_1_payload_fragment_B_4[6:0]                                                                          ), //o
    .io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation                (streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation[1:0]               ), //o
    .io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation (streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation[5:0]), //o
    .io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                    (streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                        ), //o
    .io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction                     (streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction[2:0]                    ), //o
    .io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation              (streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation[5:0]             ), //o
    .io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_UID                                     (streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_UID[18:0]                                   ), //o
    .io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt                      (streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt[12:0]                    ), //o
    .io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt                      (streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt[12:0]                    ), //o
    .io_outputs_2_valid                                                                                          (streamDemux_1_io_outputs_2_valid                                                                                              ), //o
    .io_outputs_2_ready                                                                                          (io_outputs_2_ready                                                                                                            ), //i
    .io_outputs_2_payload_last                                                                                   (streamDemux_1_io_outputs_2_payload_last                                                                                       ), //o
    .io_outputs_2_payload_fragment_A_0                                                                           (streamDemux_1_io_outputs_2_payload_fragment_A_0[8:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_A_1                                                                           (streamDemux_1_io_outputs_2_payload_fragment_A_1[8:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_A_2                                                                           (streamDemux_1_io_outputs_2_payload_fragment_A_2[8:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_A_3                                                                           (streamDemux_1_io_outputs_2_payload_fragment_A_3[8:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_A_4                                                                           (streamDemux_1_io_outputs_2_payload_fragment_A_4[8:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_B_0                                                                           (streamDemux_1_io_outputs_2_payload_fragment_B_0[6:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_B_1                                                                           (streamDemux_1_io_outputs_2_payload_fragment_B_1[6:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_B_2                                                                           (streamDemux_1_io_outputs_2_payload_fragment_B_2[6:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_B_3                                                                           (streamDemux_1_io_outputs_2_payload_fragment_B_3[6:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_B_4                                                                           (streamDemux_1_io_outputs_2_payload_fragment_B_4[6:0]                                                                          ), //o
    .io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation                (streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation[1:0]               ), //o
    .io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation (streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation[5:0]), //o
    .io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                    (streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose                        ), //o
    .io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction                     (streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction[2:0]                    ), //o
    .io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation              (streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation[5:0]             ), //o
    .io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_UID                                     (streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_UID[18:0]                                   ), //o
    .io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt                      (streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt[12:0]                    ), //o
    .io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt                      (streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt[12:0]                    )  //o
  );
  `ifndef SYNTHESIS
  always @(*) begin
    case(io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  `endif

  assign io_input_fire = (io_input_valid && io_input_ready);
  assign request = {io_outputs_2_ready,{io_outputs_1_ready,io_outputs_0_ready}};
  assign when_StreamDispatcher_l23 = ((! lock) && io_input_valid);
  assign _zz_grant = request;
  assign _zz_grant_1 = {_zz_grant,_zz_grant};
  assign _zz_grant_2 = (_zz_grant_1 & (~ _zz__zz_grant_2));
  assign grant = (_zz_grant_2[5 : 3] | _zz_grant_2[2 : 0]);
  assign _zz_selectNoLock = grant[1];
  assign _zz_selectNoLock_1 = grant[2];
  assign selectNoLock = {_zz_selectNoLock_1,_zz_selectNoLock};
  assign when_StreamDispatcher_l28 = (! lock);
  assign select_1 = (lock ? selectNoLock_regNextWhen : selectNoLock);
  assign io_input_ready = streamDemux_1_io_input_ready;
  assign io_outputs_0_valid = streamDemux_1_io_outputs_0_valid;
  assign io_outputs_0_payload_last = streamDemux_1_io_outputs_0_payload_last;
  assign io_outputs_0_payload_fragment_A_0 = streamDemux_1_io_outputs_0_payload_fragment_A_0;
  assign io_outputs_0_payload_fragment_A_1 = streamDemux_1_io_outputs_0_payload_fragment_A_1;
  assign io_outputs_0_payload_fragment_A_2 = streamDemux_1_io_outputs_0_payload_fragment_A_2;
  assign io_outputs_0_payload_fragment_A_3 = streamDemux_1_io_outputs_0_payload_fragment_A_3;
  assign io_outputs_0_payload_fragment_A_4 = streamDemux_1_io_outputs_0_payload_fragment_A_4;
  assign io_outputs_0_payload_fragment_B_0 = streamDemux_1_io_outputs_0_payload_fragment_B_0;
  assign io_outputs_0_payload_fragment_B_1 = streamDemux_1_io_outputs_0_payload_fragment_B_1;
  assign io_outputs_0_payload_fragment_B_2 = streamDemux_1_io_outputs_0_payload_fragment_B_2;
  assign io_outputs_0_payload_fragment_B_3 = streamDemux_1_io_outputs_0_payload_fragment_B_3;
  assign io_outputs_0_payload_fragment_B_4 = streamDemux_1_io_outputs_0_payload_fragment_B_4;
  assign io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation = streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  assign io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose = streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  assign io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction = streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  assign io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation = streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  assign io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_UID = streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  assign io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt = streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  assign io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt = streamDemux_1_io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  assign io_outputs_1_valid = streamDemux_1_io_outputs_1_valid;
  assign io_outputs_1_payload_last = streamDemux_1_io_outputs_1_payload_last;
  assign io_outputs_1_payload_fragment_A_0 = streamDemux_1_io_outputs_1_payload_fragment_A_0;
  assign io_outputs_1_payload_fragment_A_1 = streamDemux_1_io_outputs_1_payload_fragment_A_1;
  assign io_outputs_1_payload_fragment_A_2 = streamDemux_1_io_outputs_1_payload_fragment_A_2;
  assign io_outputs_1_payload_fragment_A_3 = streamDemux_1_io_outputs_1_payload_fragment_A_3;
  assign io_outputs_1_payload_fragment_A_4 = streamDemux_1_io_outputs_1_payload_fragment_A_4;
  assign io_outputs_1_payload_fragment_B_0 = streamDemux_1_io_outputs_1_payload_fragment_B_0;
  assign io_outputs_1_payload_fragment_B_1 = streamDemux_1_io_outputs_1_payload_fragment_B_1;
  assign io_outputs_1_payload_fragment_B_2 = streamDemux_1_io_outputs_1_payload_fragment_B_2;
  assign io_outputs_1_payload_fragment_B_3 = streamDemux_1_io_outputs_1_payload_fragment_B_3;
  assign io_outputs_1_payload_fragment_B_4 = streamDemux_1_io_outputs_1_payload_fragment_B_4;
  assign io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation = streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  assign io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose = streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  assign io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction = streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  assign io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation = streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  assign io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_UID = streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  assign io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt = streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  assign io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt = streamDemux_1_io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  assign io_outputs_2_valid = streamDemux_1_io_outputs_2_valid;
  assign io_outputs_2_payload_last = streamDemux_1_io_outputs_2_payload_last;
  assign io_outputs_2_payload_fragment_A_0 = streamDemux_1_io_outputs_2_payload_fragment_A_0;
  assign io_outputs_2_payload_fragment_A_1 = streamDemux_1_io_outputs_2_payload_fragment_A_1;
  assign io_outputs_2_payload_fragment_A_2 = streamDemux_1_io_outputs_2_payload_fragment_A_2;
  assign io_outputs_2_payload_fragment_A_3 = streamDemux_1_io_outputs_2_payload_fragment_A_3;
  assign io_outputs_2_payload_fragment_A_4 = streamDemux_1_io_outputs_2_payload_fragment_A_4;
  assign io_outputs_2_payload_fragment_B_0 = streamDemux_1_io_outputs_2_payload_fragment_B_0;
  assign io_outputs_2_payload_fragment_B_1 = streamDemux_1_io_outputs_2_payload_fragment_B_1;
  assign io_outputs_2_payload_fragment_B_2 = streamDemux_1_io_outputs_2_payload_fragment_B_2;
  assign io_outputs_2_payload_fragment_B_3 = streamDemux_1_io_outputs_2_payload_fragment_B_3;
  assign io_outputs_2_payload_fragment_B_4 = streamDemux_1_io_outputs_2_payload_fragment_B_4;
  assign io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation = streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  assign io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose = streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  assign io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction = streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  assign io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation = streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  assign io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_UID = streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  assign io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt = streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  assign io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt = streamDemux_1_io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  always @(posedge clk or posedge reset) begin
    if(reset) begin
      lock <= 1'b0;
      priority_1 <= 3'b001;
      selectNoLock_regNextWhen <= 2'b00;
    end else begin
      if(io_input_fire) begin
        lock <= (! io_input_payload_last);
      end
      if(when_StreamDispatcher_l23) begin
        priority_1 <= {priority_1[1 : 0],priority_1[2 : 2]};
      end
      if(when_StreamDispatcher_l28) begin
        selectNoLock_regNextWhen <= selectNoLock;
      end
    end
  end


endmodule

module StreamDemux (
  input  wire [1:0]    io_select,
  input  wire          io_input_valid,
  output reg           io_input_ready,
  input  wire          io_input_payload_last,
  input  wire [8:0]    io_input_payload_fragment_A_0,
  input  wire [8:0]    io_input_payload_fragment_A_1,
  input  wire [8:0]    io_input_payload_fragment_A_2,
  input  wire [8:0]    io_input_payload_fragment_A_3,
  input  wire [8:0]    io_input_payload_fragment_A_4,
  input  wire [6:0]    io_input_payload_fragment_B_0,
  input  wire [6:0]    io_input_payload_fragment_B_1,
  input  wire [6:0]    io_input_payload_fragment_B_2,
  input  wire [6:0]    io_input_payload_fragment_B_3,
  input  wire [6:0]    io_input_payload_fragment_B_4,
  input  wire [1:0]    io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  input  wire [5:0]    io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  input  wire          io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  input  wire [2:0]    io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction,
  input  wire [5:0]    io_input_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  input  wire [18:0]   io_input_payload_fragment_CoreInstruction_Collector_Instruction_UID,
  input  wire [12:0]   io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  input  wire [12:0]   io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt,
  output reg           io_outputs_0_valid,
  input  wire          io_outputs_0_ready,
  output wire          io_outputs_0_payload_last,
  output wire [8:0]    io_outputs_0_payload_fragment_A_0,
  output wire [8:0]    io_outputs_0_payload_fragment_A_1,
  output wire [8:0]    io_outputs_0_payload_fragment_A_2,
  output wire [8:0]    io_outputs_0_payload_fragment_A_3,
  output wire [8:0]    io_outputs_0_payload_fragment_A_4,
  output wire [6:0]    io_outputs_0_payload_fragment_B_0,
  output wire [6:0]    io_outputs_0_payload_fragment_B_1,
  output wire [6:0]    io_outputs_0_payload_fragment_B_2,
  output wire [6:0]    io_outputs_0_payload_fragment_B_3,
  output wire [6:0]    io_outputs_0_payload_fragment_B_4,
  output wire [1:0]    io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  output wire [5:0]    io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  output wire          io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  output wire [2:0]    io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction,
  output wire [5:0]    io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  output wire [18:0]   io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_UID,
  output wire [12:0]   io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  output wire [12:0]   io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt,
  output reg           io_outputs_1_valid,
  input  wire          io_outputs_1_ready,
  output wire          io_outputs_1_payload_last,
  output wire [8:0]    io_outputs_1_payload_fragment_A_0,
  output wire [8:0]    io_outputs_1_payload_fragment_A_1,
  output wire [8:0]    io_outputs_1_payload_fragment_A_2,
  output wire [8:0]    io_outputs_1_payload_fragment_A_3,
  output wire [8:0]    io_outputs_1_payload_fragment_A_4,
  output wire [6:0]    io_outputs_1_payload_fragment_B_0,
  output wire [6:0]    io_outputs_1_payload_fragment_B_1,
  output wire [6:0]    io_outputs_1_payload_fragment_B_2,
  output wire [6:0]    io_outputs_1_payload_fragment_B_3,
  output wire [6:0]    io_outputs_1_payload_fragment_B_4,
  output wire [1:0]    io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  output wire [5:0]    io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  output wire          io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  output wire [2:0]    io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction,
  output wire [5:0]    io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  output wire [18:0]   io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_UID,
  output wire [12:0]   io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  output wire [12:0]   io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt,
  output reg           io_outputs_2_valid,
  input  wire          io_outputs_2_ready,
  output wire          io_outputs_2_payload_last,
  output wire [8:0]    io_outputs_2_payload_fragment_A_0,
  output wire [8:0]    io_outputs_2_payload_fragment_A_1,
  output wire [8:0]    io_outputs_2_payload_fragment_A_2,
  output wire [8:0]    io_outputs_2_payload_fragment_A_3,
  output wire [8:0]    io_outputs_2_payload_fragment_A_4,
  output wire [6:0]    io_outputs_2_payload_fragment_B_0,
  output wire [6:0]    io_outputs_2_payload_fragment_B_1,
  output wire [6:0]    io_outputs_2_payload_fragment_B_2,
  output wire [6:0]    io_outputs_2_payload_fragment_B_3,
  output wire [6:0]    io_outputs_2_payload_fragment_B_4,
  output wire [1:0]    io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  output wire [5:0]    io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  output wire          io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  output wire [2:0]    io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction,
  output wire [5:0]    io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  output wire [18:0]   io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_UID,
  output wire [12:0]   io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  output wire [12:0]   io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt
);
  localparam MatrixOperation_TypeDef_MatMul = 2'd0;
  localparam MatrixOperation_TypeDef_ElementAdd = 2'd1;
  localparam MatrixOperation_TypeDef_ElementMul = 2'd2;
  localparam MatrixOperation_TypeDef_ElementMax = 2'd3;
  localparam Activation_TypeDef_Exp = 3'd0;
  localparam Activation_TypeDef_Log = 3'd1;
  localparam Activation_TypeDef_Softplus = 3'd2;
  localparam Activation_TypeDef_Relu = 3'd3;
  localparam Activation_TypeDef_None = 3'd4;

  wire                when_Stream_l1003;
  wire                when_Stream_l1003_1;
  wire                when_Stream_l1003_2;
  `ifndef SYNTHESIS
  reg [79:0] io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string;
  `endif


  `ifndef SYNTHESIS
  always @(*) begin
    case(io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "ElementMax";
      default : io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction)
      Activation_TypeDef_Exp : io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Exp     ";
      Activation_TypeDef_Log : io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Log     ";
      Activation_TypeDef_Softplus : io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Softplus";
      Activation_TypeDef_Relu : io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "Relu    ";
      Activation_TypeDef_None : io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "None    ";
      default : io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string = "????????";
    endcase
  end
  `endif

  always @(*) begin
    io_input_ready = 1'b0;
    if(!when_Stream_l1003) begin
      io_input_ready = io_outputs_0_ready;
    end
    if(!when_Stream_l1003_1) begin
      io_input_ready = io_outputs_1_ready;
    end
    if(!when_Stream_l1003_2) begin
      io_input_ready = io_outputs_2_ready;
    end
  end

  assign io_outputs_0_payload_last = io_input_payload_last;
  assign io_outputs_0_payload_fragment_A_0 = io_input_payload_fragment_A_0;
  assign io_outputs_0_payload_fragment_A_1 = io_input_payload_fragment_A_1;
  assign io_outputs_0_payload_fragment_A_2 = io_input_payload_fragment_A_2;
  assign io_outputs_0_payload_fragment_A_3 = io_input_payload_fragment_A_3;
  assign io_outputs_0_payload_fragment_A_4 = io_input_payload_fragment_A_4;
  assign io_outputs_0_payload_fragment_B_0 = io_input_payload_fragment_B_0;
  assign io_outputs_0_payload_fragment_B_1 = io_input_payload_fragment_B_1;
  assign io_outputs_0_payload_fragment_B_2 = io_input_payload_fragment_B_2;
  assign io_outputs_0_payload_fragment_B_3 = io_input_payload_fragment_B_3;
  assign io_outputs_0_payload_fragment_B_4 = io_input_payload_fragment_B_4;
  assign io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation = io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  assign io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose = io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  assign io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction = io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  assign io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation = io_input_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  assign io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_UID = io_input_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  assign io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt = io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  assign io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt = io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  assign when_Stream_l1003 = (2'b00 != io_select);
  always @(*) begin
    if(when_Stream_l1003) begin
      io_outputs_0_valid = 1'b0;
    end else begin
      io_outputs_0_valid = io_input_valid;
    end
  end

  assign io_outputs_1_payload_last = io_input_payload_last;
  assign io_outputs_1_payload_fragment_A_0 = io_input_payload_fragment_A_0;
  assign io_outputs_1_payload_fragment_A_1 = io_input_payload_fragment_A_1;
  assign io_outputs_1_payload_fragment_A_2 = io_input_payload_fragment_A_2;
  assign io_outputs_1_payload_fragment_A_3 = io_input_payload_fragment_A_3;
  assign io_outputs_1_payload_fragment_A_4 = io_input_payload_fragment_A_4;
  assign io_outputs_1_payload_fragment_B_0 = io_input_payload_fragment_B_0;
  assign io_outputs_1_payload_fragment_B_1 = io_input_payload_fragment_B_1;
  assign io_outputs_1_payload_fragment_B_2 = io_input_payload_fragment_B_2;
  assign io_outputs_1_payload_fragment_B_3 = io_input_payload_fragment_B_3;
  assign io_outputs_1_payload_fragment_B_4 = io_input_payload_fragment_B_4;
  assign io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation = io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  assign io_outputs_1_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose = io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  assign io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction = io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  assign io_outputs_1_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation = io_input_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  assign io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_UID = io_input_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  assign io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt = io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  assign io_outputs_1_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt = io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  assign when_Stream_l1003_1 = (2'b01 != io_select);
  always @(*) begin
    if(when_Stream_l1003_1) begin
      io_outputs_1_valid = 1'b0;
    end else begin
      io_outputs_1_valid = io_input_valid;
    end
  end

  assign io_outputs_2_payload_last = io_input_payload_last;
  assign io_outputs_2_payload_fragment_A_0 = io_input_payload_fragment_A_0;
  assign io_outputs_2_payload_fragment_A_1 = io_input_payload_fragment_A_1;
  assign io_outputs_2_payload_fragment_A_2 = io_input_payload_fragment_A_2;
  assign io_outputs_2_payload_fragment_A_3 = io_input_payload_fragment_A_3;
  assign io_outputs_2_payload_fragment_A_4 = io_input_payload_fragment_A_4;
  assign io_outputs_2_payload_fragment_B_0 = io_input_payload_fragment_B_0;
  assign io_outputs_2_payload_fragment_B_1 = io_input_payload_fragment_B_1;
  assign io_outputs_2_payload_fragment_B_2 = io_input_payload_fragment_B_2;
  assign io_outputs_2_payload_fragment_B_3 = io_input_payload_fragment_B_3;
  assign io_outputs_2_payload_fragment_B_4 = io_input_payload_fragment_B_4;
  assign io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation = io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  assign io_outputs_2_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose = io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  assign io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction = io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  assign io_outputs_2_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation = io_input_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  assign io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_UID = io_input_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  assign io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt = io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  assign io_outputs_2_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt = io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  assign when_Stream_l1003_2 = (2'b10 != io_select);
  always @(*) begin
    if(when_Stream_l1003_2) begin
      io_outputs_2_valid = 1'b0;
    end else begin
      io_outputs_2_valid = io_input_valid;
    end
  end


endmodule

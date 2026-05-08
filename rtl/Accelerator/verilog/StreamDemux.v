// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : StreamDemux
// Git hash  : e7984c6fd05794f7617e004bdecebf29e04e3020

`timescale 1ns/1ps 
module StreamDemux (
  input  wire          io_input_valid,
  output reg           io_input_ready,
  input  wire          io_input_payload_last,
  input  wire [23:0]   io_input_payload_fragment_A_0,
  input  wire [23:0]   io_input_payload_fragment_A_1,
  input  wire [23:0]   io_input_payload_fragment_A_2,
  input  wire [23:0]   io_input_payload_fragment_A_3,
  input  wire [23:0]   io_input_payload_fragment_A_4,
  input  wire [23:0]   io_input_payload_fragment_A_5,
  input  wire [23:0]   io_input_payload_fragment_A_6,
  input  wire [23:0]   io_input_payload_fragment_A_7,
  input  wire [23:0]   io_input_payload_fragment_A_8,
  input  wire [23:0]   io_input_payload_fragment_A_9,
  input  wire [23:0]   io_input_payload_fragment_A_10,
  input  wire [23:0]   io_input_payload_fragment_A_11,
  input  wire [23:0]   io_input_payload_fragment_A_12,
  input  wire [23:0]   io_input_payload_fragment_A_13,
  input  wire [23:0]   io_input_payload_fragment_A_14,
  input  wire [23:0]   io_input_payload_fragment_A_15,
  input  wire [23:0]   io_input_payload_fragment_A_16,
  input  wire [23:0]   io_input_payload_fragment_A_17,
  input  wire [23:0]   io_input_payload_fragment_A_18,
  input  wire [23:0]   io_input_payload_fragment_A_19,
  input  wire [23:0]   io_input_payload_fragment_A_20,
  input  wire [23:0]   io_input_payload_fragment_A_21,
  input  wire [23:0]   io_input_payload_fragment_A_22,
  input  wire [23:0]   io_input_payload_fragment_A_23,
  input  wire [23:0]   io_input_payload_fragment_A_24,
  input  wire [23:0]   io_input_payload_fragment_A_25,
  input  wire [23:0]   io_input_payload_fragment_A_26,
  input  wire [23:0]   io_input_payload_fragment_A_27,
  input  wire [23:0]   io_input_payload_fragment_A_28,
  input  wire [23:0]   io_input_payload_fragment_A_29,
  input  wire [23:0]   io_input_payload_fragment_A_30,
  input  wire [23:0]   io_input_payload_fragment_A_31,
  input  wire [23:0]   io_input_payload_fragment_B_0,
  input  wire [23:0]   io_input_payload_fragment_B_1,
  input  wire [23:0]   io_input_payload_fragment_B_2,
  input  wire [23:0]   io_input_payload_fragment_B_3,
  input  wire [23:0]   io_input_payload_fragment_B_4,
  input  wire [23:0]   io_input_payload_fragment_B_5,
  input  wire [23:0]   io_input_payload_fragment_B_6,
  input  wire [23:0]   io_input_payload_fragment_B_7,
  input  wire [23:0]   io_input_payload_fragment_B_8,
  input  wire [23:0]   io_input_payload_fragment_B_9,
  input  wire [23:0]   io_input_payload_fragment_B_10,
  input  wire [23:0]   io_input_payload_fragment_B_11,
  input  wire [23:0]   io_input_payload_fragment_B_12,
  input  wire [23:0]   io_input_payload_fragment_B_13,
  input  wire [23:0]   io_input_payload_fragment_B_14,
  input  wire [23:0]   io_input_payload_fragment_B_15,
  input  wire [23:0]   io_input_payload_fragment_B_16,
  input  wire [23:0]   io_input_payload_fragment_B_17,
  input  wire [23:0]   io_input_payload_fragment_B_18,
  input  wire [23:0]   io_input_payload_fragment_B_19,
  input  wire [23:0]   io_input_payload_fragment_B_20,
  input  wire [23:0]   io_input_payload_fragment_B_21,
  input  wire [23:0]   io_input_payload_fragment_B_22,
  input  wire [23:0]   io_input_payload_fragment_B_23,
  input  wire [23:0]   io_input_payload_fragment_B_24,
  input  wire [23:0]   io_input_payload_fragment_B_25,
  input  wire [23:0]   io_input_payload_fragment_B_26,
  input  wire [23:0]   io_input_payload_fragment_B_27,
  input  wire [23:0]   io_input_payload_fragment_B_28,
  input  wire [23:0]   io_input_payload_fragment_B_29,
  input  wire [23:0]   io_input_payload_fragment_B_30,
  input  wire [23:0]   io_input_payload_fragment_B_31,
  input  wire [1:0]    io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  input  wire [5:0]    io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  input  wire          io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  input  wire [2:0]    io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction,
  input  wire [5:0]    io_input_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  input  wire [18:0]   io_input_payload_fragment_CoreInstruction_Collector_Instruction_UID,
  input  wire [10:0]   io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  input  wire [10:0]   io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt,
  output reg           io_outputs_0_valid,
  input  wire          io_outputs_0_ready,
  output wire          io_outputs_0_payload_last,
  output wire [23:0]   io_outputs_0_payload_fragment_A_0,
  output wire [23:0]   io_outputs_0_payload_fragment_A_1,
  output wire [23:0]   io_outputs_0_payload_fragment_A_2,
  output wire [23:0]   io_outputs_0_payload_fragment_A_3,
  output wire [23:0]   io_outputs_0_payload_fragment_A_4,
  output wire [23:0]   io_outputs_0_payload_fragment_A_5,
  output wire [23:0]   io_outputs_0_payload_fragment_A_6,
  output wire [23:0]   io_outputs_0_payload_fragment_A_7,
  output wire [23:0]   io_outputs_0_payload_fragment_A_8,
  output wire [23:0]   io_outputs_0_payload_fragment_A_9,
  output wire [23:0]   io_outputs_0_payload_fragment_A_10,
  output wire [23:0]   io_outputs_0_payload_fragment_A_11,
  output wire [23:0]   io_outputs_0_payload_fragment_A_12,
  output wire [23:0]   io_outputs_0_payload_fragment_A_13,
  output wire [23:0]   io_outputs_0_payload_fragment_A_14,
  output wire [23:0]   io_outputs_0_payload_fragment_A_15,
  output wire [23:0]   io_outputs_0_payload_fragment_A_16,
  output wire [23:0]   io_outputs_0_payload_fragment_A_17,
  output wire [23:0]   io_outputs_0_payload_fragment_A_18,
  output wire [23:0]   io_outputs_0_payload_fragment_A_19,
  output wire [23:0]   io_outputs_0_payload_fragment_A_20,
  output wire [23:0]   io_outputs_0_payload_fragment_A_21,
  output wire [23:0]   io_outputs_0_payload_fragment_A_22,
  output wire [23:0]   io_outputs_0_payload_fragment_A_23,
  output wire [23:0]   io_outputs_0_payload_fragment_A_24,
  output wire [23:0]   io_outputs_0_payload_fragment_A_25,
  output wire [23:0]   io_outputs_0_payload_fragment_A_26,
  output wire [23:0]   io_outputs_0_payload_fragment_A_27,
  output wire [23:0]   io_outputs_0_payload_fragment_A_28,
  output wire [23:0]   io_outputs_0_payload_fragment_A_29,
  output wire [23:0]   io_outputs_0_payload_fragment_A_30,
  output wire [23:0]   io_outputs_0_payload_fragment_A_31,
  output wire [23:0]   io_outputs_0_payload_fragment_B_0,
  output wire [23:0]   io_outputs_0_payload_fragment_B_1,
  output wire [23:0]   io_outputs_0_payload_fragment_B_2,
  output wire [23:0]   io_outputs_0_payload_fragment_B_3,
  output wire [23:0]   io_outputs_0_payload_fragment_B_4,
  output wire [23:0]   io_outputs_0_payload_fragment_B_5,
  output wire [23:0]   io_outputs_0_payload_fragment_B_6,
  output wire [23:0]   io_outputs_0_payload_fragment_B_7,
  output wire [23:0]   io_outputs_0_payload_fragment_B_8,
  output wire [23:0]   io_outputs_0_payload_fragment_B_9,
  output wire [23:0]   io_outputs_0_payload_fragment_B_10,
  output wire [23:0]   io_outputs_0_payload_fragment_B_11,
  output wire [23:0]   io_outputs_0_payload_fragment_B_12,
  output wire [23:0]   io_outputs_0_payload_fragment_B_13,
  output wire [23:0]   io_outputs_0_payload_fragment_B_14,
  output wire [23:0]   io_outputs_0_payload_fragment_B_15,
  output wire [23:0]   io_outputs_0_payload_fragment_B_16,
  output wire [23:0]   io_outputs_0_payload_fragment_B_17,
  output wire [23:0]   io_outputs_0_payload_fragment_B_18,
  output wire [23:0]   io_outputs_0_payload_fragment_B_19,
  output wire [23:0]   io_outputs_0_payload_fragment_B_20,
  output wire [23:0]   io_outputs_0_payload_fragment_B_21,
  output wire [23:0]   io_outputs_0_payload_fragment_B_22,
  output wire [23:0]   io_outputs_0_payload_fragment_B_23,
  output wire [23:0]   io_outputs_0_payload_fragment_B_24,
  output wire [23:0]   io_outputs_0_payload_fragment_B_25,
  output wire [23:0]   io_outputs_0_payload_fragment_B_26,
  output wire [23:0]   io_outputs_0_payload_fragment_B_27,
  output wire [23:0]   io_outputs_0_payload_fragment_B_28,
  output wire [23:0]   io_outputs_0_payload_fragment_B_29,
  output wire [23:0]   io_outputs_0_payload_fragment_B_30,
  output wire [23:0]   io_outputs_0_payload_fragment_B_31,
  output wire [1:0]    io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation,
  output wire [5:0]    io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation,
  output wire          io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose,
  output wire [2:0]    io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction,
  output wire [5:0]    io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation,
  output wire [18:0]   io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_UID,
  output wire [10:0]   io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt,
  output wire [10:0]   io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt
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
  `ifndef SYNTHESIS
  reg [79:0] io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string;
  reg [79:0] io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation_string;
  reg [63:0] io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction_string;
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
  `endif

  always @(*) begin
    io_input_ready = 1'b0;
    if(!when_Stream_l1003) begin
      io_input_ready = io_outputs_0_ready;
    end
  end

  assign io_outputs_0_payload_last = io_input_payload_last;
  assign io_outputs_0_payload_fragment_A_0 = io_input_payload_fragment_A_0;
  assign io_outputs_0_payload_fragment_A_1 = io_input_payload_fragment_A_1;
  assign io_outputs_0_payload_fragment_A_2 = io_input_payload_fragment_A_2;
  assign io_outputs_0_payload_fragment_A_3 = io_input_payload_fragment_A_3;
  assign io_outputs_0_payload_fragment_A_4 = io_input_payload_fragment_A_4;
  assign io_outputs_0_payload_fragment_A_5 = io_input_payload_fragment_A_5;
  assign io_outputs_0_payload_fragment_A_6 = io_input_payload_fragment_A_6;
  assign io_outputs_0_payload_fragment_A_7 = io_input_payload_fragment_A_7;
  assign io_outputs_0_payload_fragment_A_8 = io_input_payload_fragment_A_8;
  assign io_outputs_0_payload_fragment_A_9 = io_input_payload_fragment_A_9;
  assign io_outputs_0_payload_fragment_A_10 = io_input_payload_fragment_A_10;
  assign io_outputs_0_payload_fragment_A_11 = io_input_payload_fragment_A_11;
  assign io_outputs_0_payload_fragment_A_12 = io_input_payload_fragment_A_12;
  assign io_outputs_0_payload_fragment_A_13 = io_input_payload_fragment_A_13;
  assign io_outputs_0_payload_fragment_A_14 = io_input_payload_fragment_A_14;
  assign io_outputs_0_payload_fragment_A_15 = io_input_payload_fragment_A_15;
  assign io_outputs_0_payload_fragment_A_16 = io_input_payload_fragment_A_16;
  assign io_outputs_0_payload_fragment_A_17 = io_input_payload_fragment_A_17;
  assign io_outputs_0_payload_fragment_A_18 = io_input_payload_fragment_A_18;
  assign io_outputs_0_payload_fragment_A_19 = io_input_payload_fragment_A_19;
  assign io_outputs_0_payload_fragment_A_20 = io_input_payload_fragment_A_20;
  assign io_outputs_0_payload_fragment_A_21 = io_input_payload_fragment_A_21;
  assign io_outputs_0_payload_fragment_A_22 = io_input_payload_fragment_A_22;
  assign io_outputs_0_payload_fragment_A_23 = io_input_payload_fragment_A_23;
  assign io_outputs_0_payload_fragment_A_24 = io_input_payload_fragment_A_24;
  assign io_outputs_0_payload_fragment_A_25 = io_input_payload_fragment_A_25;
  assign io_outputs_0_payload_fragment_A_26 = io_input_payload_fragment_A_26;
  assign io_outputs_0_payload_fragment_A_27 = io_input_payload_fragment_A_27;
  assign io_outputs_0_payload_fragment_A_28 = io_input_payload_fragment_A_28;
  assign io_outputs_0_payload_fragment_A_29 = io_input_payload_fragment_A_29;
  assign io_outputs_0_payload_fragment_A_30 = io_input_payload_fragment_A_30;
  assign io_outputs_0_payload_fragment_A_31 = io_input_payload_fragment_A_31;
  assign io_outputs_0_payload_fragment_B_0 = io_input_payload_fragment_B_0;
  assign io_outputs_0_payload_fragment_B_1 = io_input_payload_fragment_B_1;
  assign io_outputs_0_payload_fragment_B_2 = io_input_payload_fragment_B_2;
  assign io_outputs_0_payload_fragment_B_3 = io_input_payload_fragment_B_3;
  assign io_outputs_0_payload_fragment_B_4 = io_input_payload_fragment_B_4;
  assign io_outputs_0_payload_fragment_B_5 = io_input_payload_fragment_B_5;
  assign io_outputs_0_payload_fragment_B_6 = io_input_payload_fragment_B_6;
  assign io_outputs_0_payload_fragment_B_7 = io_input_payload_fragment_B_7;
  assign io_outputs_0_payload_fragment_B_8 = io_input_payload_fragment_B_8;
  assign io_outputs_0_payload_fragment_B_9 = io_input_payload_fragment_B_9;
  assign io_outputs_0_payload_fragment_B_10 = io_input_payload_fragment_B_10;
  assign io_outputs_0_payload_fragment_B_11 = io_input_payload_fragment_B_11;
  assign io_outputs_0_payload_fragment_B_12 = io_input_payload_fragment_B_12;
  assign io_outputs_0_payload_fragment_B_13 = io_input_payload_fragment_B_13;
  assign io_outputs_0_payload_fragment_B_14 = io_input_payload_fragment_B_14;
  assign io_outputs_0_payload_fragment_B_15 = io_input_payload_fragment_B_15;
  assign io_outputs_0_payload_fragment_B_16 = io_input_payload_fragment_B_16;
  assign io_outputs_0_payload_fragment_B_17 = io_input_payload_fragment_B_17;
  assign io_outputs_0_payload_fragment_B_18 = io_input_payload_fragment_B_18;
  assign io_outputs_0_payload_fragment_B_19 = io_input_payload_fragment_B_19;
  assign io_outputs_0_payload_fragment_B_20 = io_input_payload_fragment_B_20;
  assign io_outputs_0_payload_fragment_B_21 = io_input_payload_fragment_B_21;
  assign io_outputs_0_payload_fragment_B_22 = io_input_payload_fragment_B_22;
  assign io_outputs_0_payload_fragment_B_23 = io_input_payload_fragment_B_23;
  assign io_outputs_0_payload_fragment_B_24 = io_input_payload_fragment_B_24;
  assign io_outputs_0_payload_fragment_B_25 = io_input_payload_fragment_B_25;
  assign io_outputs_0_payload_fragment_B_26 = io_input_payload_fragment_B_26;
  assign io_outputs_0_payload_fragment_B_27 = io_input_payload_fragment_B_27;
  assign io_outputs_0_payload_fragment_B_28 = io_input_payload_fragment_B_28;
  assign io_outputs_0_payload_fragment_B_29 = io_input_payload_fragment_B_29;
  assign io_outputs_0_payload_fragment_B_30 = io_input_payload_fragment_B_30;
  assign io_outputs_0_payload_fragment_B_31 = io_input_payload_fragment_B_31;
  assign io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation = io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_matrixOperation;
  assign io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation = io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_shiftLeft_AfterMatrixOperation;
  assign io_outputs_0_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose = io_input_payload_fragment_CoreInstruction_SystolicArray2D_CC_Instruction_doTranspose;
  assign io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction = io_input_payload_fragment_CoreInstruction_Activation_Instruction_activationFunction;
  assign io_outputs_0_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation = io_input_payload_fragment_CoreInstruction_Activation_Instruction_shiftLeft_AfterActivation;
  assign io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_UID = io_input_payload_fragment_CoreInstruction_Collector_Instruction_UID;
  assign io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt = io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatA_row_slice_cnt;
  assign io_outputs_0_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt = io_input_payload_fragment_CoreInstruction_Collector_Instruction_MatB_col_slice_cnt;
  assign when_Stream_l1003 = 1'b0;
  always @(*) begin
    if(when_Stream_l1003) begin
      io_outputs_0_valid = 1'b0;
    end else begin
      io_outputs_0_valid = io_input_valid;
    end
  end


endmodule

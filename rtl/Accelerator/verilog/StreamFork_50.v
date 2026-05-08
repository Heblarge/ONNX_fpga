// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : StreamFork_50
// Git hash  : e7984c6fd05794f7617e004bdecebf29e04e3020

`timescale 1ns/1ps 
module StreamFork_50 (
  input  wire          io_input_valid,
  output reg           io_input_ready,
  input  wire [23:0]   io_input_payload_A_19_data,
  input  wire          io_input_payload_A_19_Final,
  input  wire [23:0]   io_input_payload_A_20_data,
  input  wire          io_input_payload_A_20_Final,
  input  wire [23:0]   io_input_payload_A_21_data,
  input  wire          io_input_payload_A_21_Final,
  input  wire [23:0]   io_input_payload_A_22_data,
  input  wire          io_input_payload_A_22_Final,
  input  wire [23:0]   io_input_payload_A_23_data,
  input  wire          io_input_payload_A_23_Final,
  input  wire [23:0]   io_input_payload_A_24_data,
  input  wire          io_input_payload_A_24_Final,
  input  wire [23:0]   io_input_payload_A_25_data,
  input  wire          io_input_payload_A_25_Final,
  input  wire [23:0]   io_input_payload_A_26_data,
  input  wire          io_input_payload_A_26_Final,
  input  wire [23:0]   io_input_payload_A_27_data,
  input  wire          io_input_payload_A_27_Final,
  input  wire [23:0]   io_input_payload_A_28_data,
  input  wire          io_input_payload_A_28_Final,
  input  wire [23:0]   io_input_payload_A_29_data,
  input  wire          io_input_payload_A_29_Final,
  input  wire [23:0]   io_input_payload_A_30_data,
  input  wire          io_input_payload_A_30_Final,
  input  wire [23:0]   io_input_payload_A_31_data,
  input  wire          io_input_payload_A_31_Final,
  input  wire [23:0]   io_input_payload_B_19_data,
  input  wire          io_input_payload_B_19_Final,
  input  wire [23:0]   io_input_payload_B_20_data,
  input  wire          io_input_payload_B_20_Final,
  input  wire [23:0]   io_input_payload_B_21_data,
  input  wire          io_input_payload_B_21_Final,
  input  wire [23:0]   io_input_payload_B_22_data,
  input  wire          io_input_payload_B_22_Final,
  input  wire [23:0]   io_input_payload_B_23_data,
  input  wire          io_input_payload_B_23_Final,
  input  wire [23:0]   io_input_payload_B_24_data,
  input  wire          io_input_payload_B_24_Final,
  input  wire [23:0]   io_input_payload_B_25_data,
  input  wire          io_input_payload_B_25_Final,
  input  wire [23:0]   io_input_payload_B_26_data,
  input  wire          io_input_payload_B_26_Final,
  input  wire [23:0]   io_input_payload_B_27_data,
  input  wire          io_input_payload_B_27_Final,
  input  wire [23:0]   io_input_payload_B_28_data,
  input  wire          io_input_payload_B_28_Final,
  input  wire [23:0]   io_input_payload_B_29_data,
  input  wire          io_input_payload_B_29_Final,
  input  wire [23:0]   io_input_payload_B_30_data,
  input  wire          io_input_payload_B_30_Final,
  input  wire [23:0]   io_input_payload_B_31_data,
  input  wire          io_input_payload_B_31_Final,
  input  wire [5:0]    io_input_payload_OpMode_post_Shift,
  input  wire          io_input_payload_OpMode_do_PostTranspose,
  input  wire [1:0]    io_input_payload_OpMode_MatrixOperation,
  input  wire [3:0]    io_input_payload_ID,
  output wire          io_outputs_0_valid,
  input  wire          io_outputs_0_ready,
  output wire [23:0]   io_outputs_0_payload_A_19_data,
  output wire          io_outputs_0_payload_A_19_Final,
  output wire [23:0]   io_outputs_0_payload_B_31_data,
  output wire          io_outputs_0_payload_B_31_Final,
  output wire [5:0]    io_outputs_0_payload_OpMode_post_Shift,
  output wire          io_outputs_0_payload_OpMode_do_PostTranspose,
  output wire [1:0]    io_outputs_0_payload_OpMode_MatrixOperation,
  output wire [3:0]    io_outputs_0_payload_ID,
  output wire          io_outputs_1_valid,
  input  wire          io_outputs_1_ready,
  output wire [23:0]   io_outputs_1_payload_A_20_data,
  output wire          io_outputs_1_payload_A_20_Final,
  output wire [23:0]   io_outputs_1_payload_B_30_data,
  output wire          io_outputs_1_payload_B_30_Final,
  output wire [5:0]    io_outputs_1_payload_OpMode_post_Shift,
  output wire          io_outputs_1_payload_OpMode_do_PostTranspose,
  output wire [1:0]    io_outputs_1_payload_OpMode_MatrixOperation,
  output wire [3:0]    io_outputs_1_payload_ID,
  output wire          io_outputs_2_valid,
  input  wire          io_outputs_2_ready,
  output wire [23:0]   io_outputs_2_payload_A_21_data,
  output wire          io_outputs_2_payload_A_21_Final,
  output wire [23:0]   io_outputs_2_payload_B_29_data,
  output wire          io_outputs_2_payload_B_29_Final,
  output wire [5:0]    io_outputs_2_payload_OpMode_post_Shift,
  output wire          io_outputs_2_payload_OpMode_do_PostTranspose,
  output wire [1:0]    io_outputs_2_payload_OpMode_MatrixOperation,
  output wire [3:0]    io_outputs_2_payload_ID,
  output wire          io_outputs_3_valid,
  input  wire          io_outputs_3_ready,
  output wire [23:0]   io_outputs_3_payload_A_22_data,
  output wire          io_outputs_3_payload_A_22_Final,
  output wire [23:0]   io_outputs_3_payload_B_28_data,
  output wire          io_outputs_3_payload_B_28_Final,
  output wire [5:0]    io_outputs_3_payload_OpMode_post_Shift,
  output wire          io_outputs_3_payload_OpMode_do_PostTranspose,
  output wire [1:0]    io_outputs_3_payload_OpMode_MatrixOperation,
  output wire [3:0]    io_outputs_3_payload_ID,
  output wire          io_outputs_4_valid,
  input  wire          io_outputs_4_ready,
  output wire [23:0]   io_outputs_4_payload_A_23_data,
  output wire          io_outputs_4_payload_A_23_Final,
  output wire [23:0]   io_outputs_4_payload_B_27_data,
  output wire          io_outputs_4_payload_B_27_Final,
  output wire [5:0]    io_outputs_4_payload_OpMode_post_Shift,
  output wire          io_outputs_4_payload_OpMode_do_PostTranspose,
  output wire [1:0]    io_outputs_4_payload_OpMode_MatrixOperation,
  output wire [3:0]    io_outputs_4_payload_ID,
  output wire          io_outputs_5_valid,
  input  wire          io_outputs_5_ready,
  output wire [23:0]   io_outputs_5_payload_A_24_data,
  output wire          io_outputs_5_payload_A_24_Final,
  output wire [23:0]   io_outputs_5_payload_B_26_data,
  output wire          io_outputs_5_payload_B_26_Final,
  output wire [5:0]    io_outputs_5_payload_OpMode_post_Shift,
  output wire          io_outputs_5_payload_OpMode_do_PostTranspose,
  output wire [1:0]    io_outputs_5_payload_OpMode_MatrixOperation,
  output wire [3:0]    io_outputs_5_payload_ID,
  output wire          io_outputs_6_valid,
  input  wire          io_outputs_6_ready,
  output wire [23:0]   io_outputs_6_payload_A_25_data,
  output wire          io_outputs_6_payload_A_25_Final,
  output wire [23:0]   io_outputs_6_payload_B_25_data,
  output wire          io_outputs_6_payload_B_25_Final,
  output wire [5:0]    io_outputs_6_payload_OpMode_post_Shift,
  output wire          io_outputs_6_payload_OpMode_do_PostTranspose,
  output wire [1:0]    io_outputs_6_payload_OpMode_MatrixOperation,
  output wire [3:0]    io_outputs_6_payload_ID,
  output wire          io_outputs_7_valid,
  input  wire          io_outputs_7_ready,
  output wire [23:0]   io_outputs_7_payload_A_26_data,
  output wire          io_outputs_7_payload_A_26_Final,
  output wire [23:0]   io_outputs_7_payload_B_24_data,
  output wire          io_outputs_7_payload_B_24_Final,
  output wire [5:0]    io_outputs_7_payload_OpMode_post_Shift,
  output wire          io_outputs_7_payload_OpMode_do_PostTranspose,
  output wire [1:0]    io_outputs_7_payload_OpMode_MatrixOperation,
  output wire [3:0]    io_outputs_7_payload_ID,
  output wire          io_outputs_8_valid,
  input  wire          io_outputs_8_ready,
  output wire [23:0]   io_outputs_8_payload_A_27_data,
  output wire          io_outputs_8_payload_A_27_Final,
  output wire [23:0]   io_outputs_8_payload_B_23_data,
  output wire          io_outputs_8_payload_B_23_Final,
  output wire [5:0]    io_outputs_8_payload_OpMode_post_Shift,
  output wire          io_outputs_8_payload_OpMode_do_PostTranspose,
  output wire [1:0]    io_outputs_8_payload_OpMode_MatrixOperation,
  output wire [3:0]    io_outputs_8_payload_ID,
  output wire          io_outputs_9_valid,
  input  wire          io_outputs_9_ready,
  output wire [23:0]   io_outputs_9_payload_A_28_data,
  output wire          io_outputs_9_payload_A_28_Final,
  output wire [23:0]   io_outputs_9_payload_B_22_data,
  output wire          io_outputs_9_payload_B_22_Final,
  output wire [5:0]    io_outputs_9_payload_OpMode_post_Shift,
  output wire          io_outputs_9_payload_OpMode_do_PostTranspose,
  output wire [1:0]    io_outputs_9_payload_OpMode_MatrixOperation,
  output wire [3:0]    io_outputs_9_payload_ID,
  output wire          io_outputs_10_valid,
  input  wire          io_outputs_10_ready,
  output wire [23:0]   io_outputs_10_payload_A_29_data,
  output wire          io_outputs_10_payload_A_29_Final,
  output wire [23:0]   io_outputs_10_payload_B_21_data,
  output wire          io_outputs_10_payload_B_21_Final,
  output wire [5:0]    io_outputs_10_payload_OpMode_post_Shift,
  output wire          io_outputs_10_payload_OpMode_do_PostTranspose,
  output wire [1:0]    io_outputs_10_payload_OpMode_MatrixOperation,
  output wire [3:0]    io_outputs_10_payload_ID,
  output wire          io_outputs_11_valid,
  input  wire          io_outputs_11_ready,
  output wire [23:0]   io_outputs_11_payload_A_30_data,
  output wire          io_outputs_11_payload_A_30_Final,
  output wire [23:0]   io_outputs_11_payload_B_20_data,
  output wire          io_outputs_11_payload_B_20_Final,
  output wire [5:0]    io_outputs_11_payload_OpMode_post_Shift,
  output wire          io_outputs_11_payload_OpMode_do_PostTranspose,
  output wire [1:0]    io_outputs_11_payload_OpMode_MatrixOperation,
  output wire [3:0]    io_outputs_11_payload_ID,
  output wire          io_outputs_12_valid,
  input  wire          io_outputs_12_ready,
  output wire [23:0]   io_outputs_12_payload_A_31_data,
  output wire          io_outputs_12_payload_A_31_Final,
  output wire [23:0]   io_outputs_12_payload_B_19_data,
  output wire          io_outputs_12_payload_B_19_Final,
  output wire [5:0]    io_outputs_12_payload_OpMode_post_Shift,
  output wire          io_outputs_12_payload_OpMode_do_PostTranspose,
  output wire [1:0]    io_outputs_12_payload_OpMode_MatrixOperation,
  output wire [3:0]    io_outputs_12_payload_ID,
  output wire          io_outputs_13_valid,
  input  wire          io_outputs_13_ready,
  output wire          io_outputs_14_valid,
  input  wire          io_outputs_14_ready,
  output wire          io_outputs_15_valid,
  input  wire          io_outputs_15_ready,
  output wire          io_outputs_16_valid,
  input  wire          io_outputs_16_ready,
  output wire          io_outputs_17_valid,
  input  wire          io_outputs_17_ready,
  output wire          io_outputs_18_valid,
  input  wire          io_outputs_18_ready,
  output wire          io_outputs_19_valid,
  input  wire          io_outputs_19_ready,
  output wire          io_outputs_20_valid,
  input  wire          io_outputs_20_ready,
  output wire          io_outputs_21_valid,
  input  wire          io_outputs_21_ready,
  output wire          io_outputs_22_valid,
  input  wire          io_outputs_22_ready,
  output wire          io_outputs_23_valid,
  input  wire          io_outputs_23_ready,
  output wire          io_outputs_24_valid,
  input  wire          io_outputs_24_ready,
  output wire          io_outputs_25_valid,
  input  wire          io_outputs_25_ready,
  output wire          io_outputs_26_valid,
  input  wire          io_outputs_26_ready,
  output wire          io_outputs_27_valid,
  input  wire          io_outputs_27_ready,
  output wire          io_outputs_28_valid,
  input  wire          io_outputs_28_ready,
  output wire          io_outputs_29_valid,
  input  wire          io_outputs_29_ready,
  output wire          io_outputs_30_valid,
  input  wire          io_outputs_30_ready,
  output wire          io_outputs_31_valid,
  input  wire          io_outputs_31_ready,
  output wire          io_outputs_32_valid,
  input  wire          io_outputs_32_ready,
  output wire          io_outputs_33_valid,
  input  wire          io_outputs_33_ready,
  output wire          io_outputs_34_valid,
  input  wire          io_outputs_34_ready,
  output wire          io_outputs_35_valid,
  input  wire          io_outputs_35_ready,
  output wire          io_outputs_36_valid,
  input  wire          io_outputs_36_ready,
  output wire          io_outputs_37_valid,
  input  wire          io_outputs_37_ready,
  output wire          io_outputs_38_valid,
  input  wire          io_outputs_38_ready,
  output wire          io_outputs_39_valid,
  input  wire          io_outputs_39_ready,
  output wire          io_outputs_40_valid,
  input  wire          io_outputs_40_ready,
  output wire          io_outputs_41_valid,
  input  wire          io_outputs_41_ready,
  output wire          io_outputs_42_valid,
  input  wire          io_outputs_42_ready,
  output wire          io_outputs_43_valid,
  input  wire          io_outputs_43_ready,
  output wire          io_outputs_44_valid,
  input  wire          io_outputs_44_ready,
  output wire          io_outputs_45_valid,
  input  wire          io_outputs_45_ready,
  output wire          io_outputs_46_valid,
  input  wire          io_outputs_46_ready,
  output wire          io_outputs_47_valid,
  input  wire          io_outputs_47_ready,
  output wire          io_outputs_48_valid,
  input  wire          io_outputs_48_ready,
  output wire          io_outputs_49_valid,
  input  wire          io_outputs_49_ready,
  output wire          io_outputs_50_valid,
  input  wire          io_outputs_50_ready,
  input  wire          SystolicArray2D_CC_core_clk,
  input  wire          SystolicArray2D_CC_core_reset
);
  localparam MatrixOperation_TypeDef_MatMul = 2'd0;
  localparam MatrixOperation_TypeDef_ElementAdd = 2'd1;
  localparam MatrixOperation_TypeDef_ElementMul = 2'd2;
  localparam MatrixOperation_TypeDef_ElementMax = 2'd3;

  reg                 logic_linkEnable_0;
  reg                 logic_linkEnable_1;
  reg                 logic_linkEnable_2;
  reg                 logic_linkEnable_3;
  reg                 logic_linkEnable_4;
  reg                 logic_linkEnable_5;
  reg                 logic_linkEnable_6;
  reg                 logic_linkEnable_7;
  reg                 logic_linkEnable_8;
  reg                 logic_linkEnable_9;
  reg                 logic_linkEnable_10;
  reg                 logic_linkEnable_11;
  reg                 logic_linkEnable_12;
  reg                 logic_linkEnable_13;
  reg                 logic_linkEnable_14;
  reg                 logic_linkEnable_15;
  reg                 logic_linkEnable_16;
  reg                 logic_linkEnable_17;
  reg                 logic_linkEnable_18;
  reg                 logic_linkEnable_19;
  reg                 logic_linkEnable_20;
  reg                 logic_linkEnable_21;
  reg                 logic_linkEnable_22;
  reg                 logic_linkEnable_23;
  reg                 logic_linkEnable_24;
  reg                 logic_linkEnable_25;
  reg                 logic_linkEnable_26;
  reg                 logic_linkEnable_27;
  reg                 logic_linkEnable_28;
  reg                 logic_linkEnable_29;
  reg                 logic_linkEnable_30;
  reg                 logic_linkEnable_31;
  reg                 logic_linkEnable_32;
  reg                 logic_linkEnable_33;
  reg                 logic_linkEnable_34;
  reg                 logic_linkEnable_35;
  reg                 logic_linkEnable_36;
  reg                 logic_linkEnable_37;
  reg                 logic_linkEnable_38;
  reg                 logic_linkEnable_39;
  reg                 logic_linkEnable_40;
  reg                 logic_linkEnable_41;
  reg                 logic_linkEnable_42;
  reg                 logic_linkEnable_43;
  reg                 logic_linkEnable_44;
  reg                 logic_linkEnable_45;
  reg                 logic_linkEnable_46;
  reg                 logic_linkEnable_47;
  reg                 logic_linkEnable_48;
  reg                 logic_linkEnable_49;
  reg                 logic_linkEnable_50;
  wire                when_Stream_l1088;
  wire                when_Stream_l1088_1;
  wire                when_Stream_l1088_2;
  wire                when_Stream_l1088_3;
  wire                when_Stream_l1088_4;
  wire                when_Stream_l1088_5;
  wire                when_Stream_l1088_6;
  wire                when_Stream_l1088_7;
  wire                when_Stream_l1088_8;
  wire                when_Stream_l1088_9;
  wire                when_Stream_l1088_10;
  wire                when_Stream_l1088_11;
  wire                when_Stream_l1088_12;
  wire                when_Stream_l1088_13;
  wire                when_Stream_l1088_14;
  wire                when_Stream_l1088_15;
  wire                when_Stream_l1088_16;
  wire                when_Stream_l1088_17;
  wire                when_Stream_l1088_18;
  wire                when_Stream_l1088_19;
  wire                when_Stream_l1088_20;
  wire                when_Stream_l1088_21;
  wire                when_Stream_l1088_22;
  wire                when_Stream_l1088_23;
  wire                when_Stream_l1088_24;
  wire                when_Stream_l1088_25;
  wire                when_Stream_l1088_26;
  wire                when_Stream_l1088_27;
  wire                when_Stream_l1088_28;
  wire                when_Stream_l1088_29;
  wire                when_Stream_l1088_30;
  wire                when_Stream_l1088_31;
  wire                when_Stream_l1088_32;
  wire                when_Stream_l1088_33;
  wire                when_Stream_l1088_34;
  wire                when_Stream_l1088_35;
  wire                when_Stream_l1088_36;
  wire                when_Stream_l1088_37;
  wire                when_Stream_l1088_38;
  wire                when_Stream_l1088_39;
  wire                when_Stream_l1088_40;
  wire                when_Stream_l1088_41;
  wire                when_Stream_l1088_42;
  wire                when_Stream_l1088_43;
  wire                when_Stream_l1088_44;
  wire                when_Stream_l1088_45;
  wire                when_Stream_l1088_46;
  wire                when_Stream_l1088_47;
  wire                when_Stream_l1088_48;
  wire                when_Stream_l1088_49;
  wire                when_Stream_l1088_50;
  wire                io_outputs_0_fire;
  wire                io_outputs_1_fire;
  wire                io_outputs_2_fire;
  wire                io_outputs_3_fire;
  wire                io_outputs_4_fire;
  wire                io_outputs_5_fire;
  wire                io_outputs_6_fire;
  wire                io_outputs_7_fire;
  wire                io_outputs_8_fire;
  wire                io_outputs_9_fire;
  wire                io_outputs_10_fire;
  wire                io_outputs_11_fire;
  wire                io_outputs_12_fire;
  wire                io_outputs_13_fire;
  wire                io_outputs_14_fire;
  wire                io_outputs_15_fire;
  wire                io_outputs_16_fire;
  wire                io_outputs_17_fire;
  wire                io_outputs_18_fire;
  wire                io_outputs_19_fire;
  wire                io_outputs_20_fire;
  wire                io_outputs_21_fire;
  wire                io_outputs_22_fire;
  wire                io_outputs_23_fire;
  wire                io_outputs_24_fire;
  wire                io_outputs_25_fire;
  wire                io_outputs_26_fire;
  wire                io_outputs_27_fire;
  wire                io_outputs_28_fire;
  wire                io_outputs_29_fire;
  wire                io_outputs_30_fire;
  wire                io_outputs_31_fire;
  wire                io_outputs_32_fire;
  wire                io_outputs_33_fire;
  wire                io_outputs_34_fire;
  wire                io_outputs_35_fire;
  wire                io_outputs_36_fire;
  wire                io_outputs_37_fire;
  wire                io_outputs_38_fire;
  wire                io_outputs_39_fire;
  wire                io_outputs_40_fire;
  wire                io_outputs_41_fire;
  wire                io_outputs_42_fire;
  wire                io_outputs_43_fire;
  wire                io_outputs_44_fire;
  wire                io_outputs_45_fire;
  wire                io_outputs_46_fire;
  wire                io_outputs_47_fire;
  wire                io_outputs_48_fire;
  wire                io_outputs_49_fire;
  wire                io_outputs_50_fire;
  `ifndef SYNTHESIS
  reg [79:0] io_input_payload_OpMode_MatrixOperation_string;
  reg [79:0] io_outputs_0_payload_OpMode_MatrixOperation_string;
  reg [79:0] io_outputs_1_payload_OpMode_MatrixOperation_string;
  reg [79:0] io_outputs_2_payload_OpMode_MatrixOperation_string;
  reg [79:0] io_outputs_3_payload_OpMode_MatrixOperation_string;
  reg [79:0] io_outputs_4_payload_OpMode_MatrixOperation_string;
  reg [79:0] io_outputs_5_payload_OpMode_MatrixOperation_string;
  reg [79:0] io_outputs_6_payload_OpMode_MatrixOperation_string;
  reg [79:0] io_outputs_7_payload_OpMode_MatrixOperation_string;
  reg [79:0] io_outputs_8_payload_OpMode_MatrixOperation_string;
  reg [79:0] io_outputs_9_payload_OpMode_MatrixOperation_string;
  reg [79:0] io_outputs_10_payload_OpMode_MatrixOperation_string;
  reg [79:0] io_outputs_11_payload_OpMode_MatrixOperation_string;
  reg [79:0] io_outputs_12_payload_OpMode_MatrixOperation_string;
  `endif


  `ifndef SYNTHESIS
  always @(*) begin
    case(io_input_payload_OpMode_MatrixOperation)
      MatrixOperation_TypeDef_MatMul : io_input_payload_OpMode_MatrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_input_payload_OpMode_MatrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_input_payload_OpMode_MatrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_input_payload_OpMode_MatrixOperation_string = "ElementMax";
      default : io_input_payload_OpMode_MatrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_0_payload_OpMode_MatrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_0_payload_OpMode_MatrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_0_payload_OpMode_MatrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_0_payload_OpMode_MatrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_0_payload_OpMode_MatrixOperation_string = "ElementMax";
      default : io_outputs_0_payload_OpMode_MatrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_1_payload_OpMode_MatrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_1_payload_OpMode_MatrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_1_payload_OpMode_MatrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_1_payload_OpMode_MatrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_1_payload_OpMode_MatrixOperation_string = "ElementMax";
      default : io_outputs_1_payload_OpMode_MatrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_2_payload_OpMode_MatrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_2_payload_OpMode_MatrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_2_payload_OpMode_MatrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_2_payload_OpMode_MatrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_2_payload_OpMode_MatrixOperation_string = "ElementMax";
      default : io_outputs_2_payload_OpMode_MatrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_3_payload_OpMode_MatrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_3_payload_OpMode_MatrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_3_payload_OpMode_MatrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_3_payload_OpMode_MatrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_3_payload_OpMode_MatrixOperation_string = "ElementMax";
      default : io_outputs_3_payload_OpMode_MatrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_4_payload_OpMode_MatrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_4_payload_OpMode_MatrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_4_payload_OpMode_MatrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_4_payload_OpMode_MatrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_4_payload_OpMode_MatrixOperation_string = "ElementMax";
      default : io_outputs_4_payload_OpMode_MatrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_5_payload_OpMode_MatrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_5_payload_OpMode_MatrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_5_payload_OpMode_MatrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_5_payload_OpMode_MatrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_5_payload_OpMode_MatrixOperation_string = "ElementMax";
      default : io_outputs_5_payload_OpMode_MatrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_6_payload_OpMode_MatrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_6_payload_OpMode_MatrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_6_payload_OpMode_MatrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_6_payload_OpMode_MatrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_6_payload_OpMode_MatrixOperation_string = "ElementMax";
      default : io_outputs_6_payload_OpMode_MatrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_7_payload_OpMode_MatrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_7_payload_OpMode_MatrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_7_payload_OpMode_MatrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_7_payload_OpMode_MatrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_7_payload_OpMode_MatrixOperation_string = "ElementMax";
      default : io_outputs_7_payload_OpMode_MatrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_8_payload_OpMode_MatrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_8_payload_OpMode_MatrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_8_payload_OpMode_MatrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_8_payload_OpMode_MatrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_8_payload_OpMode_MatrixOperation_string = "ElementMax";
      default : io_outputs_8_payload_OpMode_MatrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_9_payload_OpMode_MatrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_9_payload_OpMode_MatrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_9_payload_OpMode_MatrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_9_payload_OpMode_MatrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_9_payload_OpMode_MatrixOperation_string = "ElementMax";
      default : io_outputs_9_payload_OpMode_MatrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_10_payload_OpMode_MatrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_10_payload_OpMode_MatrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_10_payload_OpMode_MatrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_10_payload_OpMode_MatrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_10_payload_OpMode_MatrixOperation_string = "ElementMax";
      default : io_outputs_10_payload_OpMode_MatrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_11_payload_OpMode_MatrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_11_payload_OpMode_MatrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_11_payload_OpMode_MatrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_11_payload_OpMode_MatrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_11_payload_OpMode_MatrixOperation_string = "ElementMax";
      default : io_outputs_11_payload_OpMode_MatrixOperation_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_outputs_12_payload_OpMode_MatrixOperation)
      MatrixOperation_TypeDef_MatMul : io_outputs_12_payload_OpMode_MatrixOperation_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_outputs_12_payload_OpMode_MatrixOperation_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_outputs_12_payload_OpMode_MatrixOperation_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_outputs_12_payload_OpMode_MatrixOperation_string = "ElementMax";
      default : io_outputs_12_payload_OpMode_MatrixOperation_string = "??????????";
    endcase
  end
  `endif

  always @(*) begin
    io_input_ready = 1'b1;
    if(when_Stream_l1088) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_1) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_2) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_3) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_4) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_5) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_6) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_7) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_8) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_9) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_10) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_11) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_12) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_13) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_14) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_15) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_16) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_17) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_18) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_19) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_20) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_21) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_22) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_23) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_24) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_25) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_26) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_27) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_28) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_29) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_30) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_31) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_32) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_33) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_34) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_35) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_36) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_37) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_38) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_39) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_40) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_41) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_42) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_43) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_44) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_45) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_46) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_47) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_48) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_49) begin
      io_input_ready = 1'b0;
    end
    if(when_Stream_l1088_50) begin
      io_input_ready = 1'b0;
    end
  end

  assign when_Stream_l1088 = ((! io_outputs_0_ready) && logic_linkEnable_0);
  assign when_Stream_l1088_1 = ((! io_outputs_1_ready) && logic_linkEnable_1);
  assign when_Stream_l1088_2 = ((! io_outputs_2_ready) && logic_linkEnable_2);
  assign when_Stream_l1088_3 = ((! io_outputs_3_ready) && logic_linkEnable_3);
  assign when_Stream_l1088_4 = ((! io_outputs_4_ready) && logic_linkEnable_4);
  assign when_Stream_l1088_5 = ((! io_outputs_5_ready) && logic_linkEnable_5);
  assign when_Stream_l1088_6 = ((! io_outputs_6_ready) && logic_linkEnable_6);
  assign when_Stream_l1088_7 = ((! io_outputs_7_ready) && logic_linkEnable_7);
  assign when_Stream_l1088_8 = ((! io_outputs_8_ready) && logic_linkEnable_8);
  assign when_Stream_l1088_9 = ((! io_outputs_9_ready) && logic_linkEnable_9);
  assign when_Stream_l1088_10 = ((! io_outputs_10_ready) && logic_linkEnable_10);
  assign when_Stream_l1088_11 = ((! io_outputs_11_ready) && logic_linkEnable_11);
  assign when_Stream_l1088_12 = ((! io_outputs_12_ready) && logic_linkEnable_12);
  assign when_Stream_l1088_13 = ((! io_outputs_13_ready) && logic_linkEnable_13);
  assign when_Stream_l1088_14 = ((! io_outputs_14_ready) && logic_linkEnable_14);
  assign when_Stream_l1088_15 = ((! io_outputs_15_ready) && logic_linkEnable_15);
  assign when_Stream_l1088_16 = ((! io_outputs_16_ready) && logic_linkEnable_16);
  assign when_Stream_l1088_17 = ((! io_outputs_17_ready) && logic_linkEnable_17);
  assign when_Stream_l1088_18 = ((! io_outputs_18_ready) && logic_linkEnable_18);
  assign when_Stream_l1088_19 = ((! io_outputs_19_ready) && logic_linkEnable_19);
  assign when_Stream_l1088_20 = ((! io_outputs_20_ready) && logic_linkEnable_20);
  assign when_Stream_l1088_21 = ((! io_outputs_21_ready) && logic_linkEnable_21);
  assign when_Stream_l1088_22 = ((! io_outputs_22_ready) && logic_linkEnable_22);
  assign when_Stream_l1088_23 = ((! io_outputs_23_ready) && logic_linkEnable_23);
  assign when_Stream_l1088_24 = ((! io_outputs_24_ready) && logic_linkEnable_24);
  assign when_Stream_l1088_25 = ((! io_outputs_25_ready) && logic_linkEnable_25);
  assign when_Stream_l1088_26 = ((! io_outputs_26_ready) && logic_linkEnable_26);
  assign when_Stream_l1088_27 = ((! io_outputs_27_ready) && logic_linkEnable_27);
  assign when_Stream_l1088_28 = ((! io_outputs_28_ready) && logic_linkEnable_28);
  assign when_Stream_l1088_29 = ((! io_outputs_29_ready) && logic_linkEnable_29);
  assign when_Stream_l1088_30 = ((! io_outputs_30_ready) && logic_linkEnable_30);
  assign when_Stream_l1088_31 = ((! io_outputs_31_ready) && logic_linkEnable_31);
  assign when_Stream_l1088_32 = ((! io_outputs_32_ready) && logic_linkEnable_32);
  assign when_Stream_l1088_33 = ((! io_outputs_33_ready) && logic_linkEnable_33);
  assign when_Stream_l1088_34 = ((! io_outputs_34_ready) && logic_linkEnable_34);
  assign when_Stream_l1088_35 = ((! io_outputs_35_ready) && logic_linkEnable_35);
  assign when_Stream_l1088_36 = ((! io_outputs_36_ready) && logic_linkEnable_36);
  assign when_Stream_l1088_37 = ((! io_outputs_37_ready) && logic_linkEnable_37);
  assign when_Stream_l1088_38 = ((! io_outputs_38_ready) && logic_linkEnable_38);
  assign when_Stream_l1088_39 = ((! io_outputs_39_ready) && logic_linkEnable_39);
  assign when_Stream_l1088_40 = ((! io_outputs_40_ready) && logic_linkEnable_40);
  assign when_Stream_l1088_41 = ((! io_outputs_41_ready) && logic_linkEnable_41);
  assign when_Stream_l1088_42 = ((! io_outputs_42_ready) && logic_linkEnable_42);
  assign when_Stream_l1088_43 = ((! io_outputs_43_ready) && logic_linkEnable_43);
  assign when_Stream_l1088_44 = ((! io_outputs_44_ready) && logic_linkEnable_44);
  assign when_Stream_l1088_45 = ((! io_outputs_45_ready) && logic_linkEnable_45);
  assign when_Stream_l1088_46 = ((! io_outputs_46_ready) && logic_linkEnable_46);
  assign when_Stream_l1088_47 = ((! io_outputs_47_ready) && logic_linkEnable_47);
  assign when_Stream_l1088_48 = ((! io_outputs_48_ready) && logic_linkEnable_48);
  assign when_Stream_l1088_49 = ((! io_outputs_49_ready) && logic_linkEnable_49);
  assign when_Stream_l1088_50 = ((! io_outputs_50_ready) && logic_linkEnable_50);
  assign io_outputs_0_valid = (io_input_valid && logic_linkEnable_0);
  assign io_outputs_0_payload_A_19_data = io_input_payload_A_19_data;
  assign io_outputs_0_payload_A_19_Final = io_input_payload_A_19_Final;
  assign io_outputs_0_payload_B_31_data = io_input_payload_B_31_data;
  assign io_outputs_0_payload_B_31_Final = io_input_payload_B_31_Final;
  assign io_outputs_0_payload_OpMode_post_Shift = io_input_payload_OpMode_post_Shift;
  assign io_outputs_0_payload_OpMode_do_PostTranspose = io_input_payload_OpMode_do_PostTranspose;
  assign io_outputs_0_payload_OpMode_MatrixOperation = io_input_payload_OpMode_MatrixOperation;
  assign io_outputs_0_payload_ID = io_input_payload_ID;
  assign io_outputs_0_fire = (io_outputs_0_valid && io_outputs_0_ready);
  assign io_outputs_1_valid = (io_input_valid && logic_linkEnable_1);
  assign io_outputs_1_payload_A_20_data = io_input_payload_A_20_data;
  assign io_outputs_1_payload_A_20_Final = io_input_payload_A_20_Final;
  assign io_outputs_1_payload_B_30_data = io_input_payload_B_30_data;
  assign io_outputs_1_payload_B_30_Final = io_input_payload_B_30_Final;
  assign io_outputs_1_payload_OpMode_post_Shift = io_input_payload_OpMode_post_Shift;
  assign io_outputs_1_payload_OpMode_do_PostTranspose = io_input_payload_OpMode_do_PostTranspose;
  assign io_outputs_1_payload_OpMode_MatrixOperation = io_input_payload_OpMode_MatrixOperation;
  assign io_outputs_1_payload_ID = io_input_payload_ID;
  assign io_outputs_1_fire = (io_outputs_1_valid && io_outputs_1_ready);
  assign io_outputs_2_valid = (io_input_valid && logic_linkEnable_2);
  assign io_outputs_2_payload_A_21_data = io_input_payload_A_21_data;
  assign io_outputs_2_payload_A_21_Final = io_input_payload_A_21_Final;
  assign io_outputs_2_payload_B_29_data = io_input_payload_B_29_data;
  assign io_outputs_2_payload_B_29_Final = io_input_payload_B_29_Final;
  assign io_outputs_2_payload_OpMode_post_Shift = io_input_payload_OpMode_post_Shift;
  assign io_outputs_2_payload_OpMode_do_PostTranspose = io_input_payload_OpMode_do_PostTranspose;
  assign io_outputs_2_payload_OpMode_MatrixOperation = io_input_payload_OpMode_MatrixOperation;
  assign io_outputs_2_payload_ID = io_input_payload_ID;
  assign io_outputs_2_fire = (io_outputs_2_valid && io_outputs_2_ready);
  assign io_outputs_3_valid = (io_input_valid && logic_linkEnable_3);
  assign io_outputs_3_payload_A_22_data = io_input_payload_A_22_data;
  assign io_outputs_3_payload_A_22_Final = io_input_payload_A_22_Final;
  assign io_outputs_3_payload_B_28_data = io_input_payload_B_28_data;
  assign io_outputs_3_payload_B_28_Final = io_input_payload_B_28_Final;
  assign io_outputs_3_payload_OpMode_post_Shift = io_input_payload_OpMode_post_Shift;
  assign io_outputs_3_payload_OpMode_do_PostTranspose = io_input_payload_OpMode_do_PostTranspose;
  assign io_outputs_3_payload_OpMode_MatrixOperation = io_input_payload_OpMode_MatrixOperation;
  assign io_outputs_3_payload_ID = io_input_payload_ID;
  assign io_outputs_3_fire = (io_outputs_3_valid && io_outputs_3_ready);
  assign io_outputs_4_valid = (io_input_valid && logic_linkEnable_4);
  assign io_outputs_4_payload_A_23_data = io_input_payload_A_23_data;
  assign io_outputs_4_payload_A_23_Final = io_input_payload_A_23_Final;
  assign io_outputs_4_payload_B_27_data = io_input_payload_B_27_data;
  assign io_outputs_4_payload_B_27_Final = io_input_payload_B_27_Final;
  assign io_outputs_4_payload_OpMode_post_Shift = io_input_payload_OpMode_post_Shift;
  assign io_outputs_4_payload_OpMode_do_PostTranspose = io_input_payload_OpMode_do_PostTranspose;
  assign io_outputs_4_payload_OpMode_MatrixOperation = io_input_payload_OpMode_MatrixOperation;
  assign io_outputs_4_payload_ID = io_input_payload_ID;
  assign io_outputs_4_fire = (io_outputs_4_valid && io_outputs_4_ready);
  assign io_outputs_5_valid = (io_input_valid && logic_linkEnable_5);
  assign io_outputs_5_payload_A_24_data = io_input_payload_A_24_data;
  assign io_outputs_5_payload_A_24_Final = io_input_payload_A_24_Final;
  assign io_outputs_5_payload_B_26_data = io_input_payload_B_26_data;
  assign io_outputs_5_payload_B_26_Final = io_input_payload_B_26_Final;
  assign io_outputs_5_payload_OpMode_post_Shift = io_input_payload_OpMode_post_Shift;
  assign io_outputs_5_payload_OpMode_do_PostTranspose = io_input_payload_OpMode_do_PostTranspose;
  assign io_outputs_5_payload_OpMode_MatrixOperation = io_input_payload_OpMode_MatrixOperation;
  assign io_outputs_5_payload_ID = io_input_payload_ID;
  assign io_outputs_5_fire = (io_outputs_5_valid && io_outputs_5_ready);
  assign io_outputs_6_valid = (io_input_valid && logic_linkEnable_6);
  assign io_outputs_6_payload_A_25_data = io_input_payload_A_25_data;
  assign io_outputs_6_payload_A_25_Final = io_input_payload_A_25_Final;
  assign io_outputs_6_payload_B_25_data = io_input_payload_B_25_data;
  assign io_outputs_6_payload_B_25_Final = io_input_payload_B_25_Final;
  assign io_outputs_6_payload_OpMode_post_Shift = io_input_payload_OpMode_post_Shift;
  assign io_outputs_6_payload_OpMode_do_PostTranspose = io_input_payload_OpMode_do_PostTranspose;
  assign io_outputs_6_payload_OpMode_MatrixOperation = io_input_payload_OpMode_MatrixOperation;
  assign io_outputs_6_payload_ID = io_input_payload_ID;
  assign io_outputs_6_fire = (io_outputs_6_valid && io_outputs_6_ready);
  assign io_outputs_7_valid = (io_input_valid && logic_linkEnable_7);
  assign io_outputs_7_payload_A_26_data = io_input_payload_A_26_data;
  assign io_outputs_7_payload_A_26_Final = io_input_payload_A_26_Final;
  assign io_outputs_7_payload_B_24_data = io_input_payload_B_24_data;
  assign io_outputs_7_payload_B_24_Final = io_input_payload_B_24_Final;
  assign io_outputs_7_payload_OpMode_post_Shift = io_input_payload_OpMode_post_Shift;
  assign io_outputs_7_payload_OpMode_do_PostTranspose = io_input_payload_OpMode_do_PostTranspose;
  assign io_outputs_7_payload_OpMode_MatrixOperation = io_input_payload_OpMode_MatrixOperation;
  assign io_outputs_7_payload_ID = io_input_payload_ID;
  assign io_outputs_7_fire = (io_outputs_7_valid && io_outputs_7_ready);
  assign io_outputs_8_valid = (io_input_valid && logic_linkEnable_8);
  assign io_outputs_8_payload_A_27_data = io_input_payload_A_27_data;
  assign io_outputs_8_payload_A_27_Final = io_input_payload_A_27_Final;
  assign io_outputs_8_payload_B_23_data = io_input_payload_B_23_data;
  assign io_outputs_8_payload_B_23_Final = io_input_payload_B_23_Final;
  assign io_outputs_8_payload_OpMode_post_Shift = io_input_payload_OpMode_post_Shift;
  assign io_outputs_8_payload_OpMode_do_PostTranspose = io_input_payload_OpMode_do_PostTranspose;
  assign io_outputs_8_payload_OpMode_MatrixOperation = io_input_payload_OpMode_MatrixOperation;
  assign io_outputs_8_payload_ID = io_input_payload_ID;
  assign io_outputs_8_fire = (io_outputs_8_valid && io_outputs_8_ready);
  assign io_outputs_9_valid = (io_input_valid && logic_linkEnable_9);
  assign io_outputs_9_payload_A_28_data = io_input_payload_A_28_data;
  assign io_outputs_9_payload_A_28_Final = io_input_payload_A_28_Final;
  assign io_outputs_9_payload_B_22_data = io_input_payload_B_22_data;
  assign io_outputs_9_payload_B_22_Final = io_input_payload_B_22_Final;
  assign io_outputs_9_payload_OpMode_post_Shift = io_input_payload_OpMode_post_Shift;
  assign io_outputs_9_payload_OpMode_do_PostTranspose = io_input_payload_OpMode_do_PostTranspose;
  assign io_outputs_9_payload_OpMode_MatrixOperation = io_input_payload_OpMode_MatrixOperation;
  assign io_outputs_9_payload_ID = io_input_payload_ID;
  assign io_outputs_9_fire = (io_outputs_9_valid && io_outputs_9_ready);
  assign io_outputs_10_valid = (io_input_valid && logic_linkEnable_10);
  assign io_outputs_10_payload_A_29_data = io_input_payload_A_29_data;
  assign io_outputs_10_payload_A_29_Final = io_input_payload_A_29_Final;
  assign io_outputs_10_payload_B_21_data = io_input_payload_B_21_data;
  assign io_outputs_10_payload_B_21_Final = io_input_payload_B_21_Final;
  assign io_outputs_10_payload_OpMode_post_Shift = io_input_payload_OpMode_post_Shift;
  assign io_outputs_10_payload_OpMode_do_PostTranspose = io_input_payload_OpMode_do_PostTranspose;
  assign io_outputs_10_payload_OpMode_MatrixOperation = io_input_payload_OpMode_MatrixOperation;
  assign io_outputs_10_payload_ID = io_input_payload_ID;
  assign io_outputs_10_fire = (io_outputs_10_valid && io_outputs_10_ready);
  assign io_outputs_11_valid = (io_input_valid && logic_linkEnable_11);
  assign io_outputs_11_payload_A_30_data = io_input_payload_A_30_data;
  assign io_outputs_11_payload_A_30_Final = io_input_payload_A_30_Final;
  assign io_outputs_11_payload_B_20_data = io_input_payload_B_20_data;
  assign io_outputs_11_payload_B_20_Final = io_input_payload_B_20_Final;
  assign io_outputs_11_payload_OpMode_post_Shift = io_input_payload_OpMode_post_Shift;
  assign io_outputs_11_payload_OpMode_do_PostTranspose = io_input_payload_OpMode_do_PostTranspose;
  assign io_outputs_11_payload_OpMode_MatrixOperation = io_input_payload_OpMode_MatrixOperation;
  assign io_outputs_11_payload_ID = io_input_payload_ID;
  assign io_outputs_11_fire = (io_outputs_11_valid && io_outputs_11_ready);
  assign io_outputs_12_valid = (io_input_valid && logic_linkEnable_12);
  assign io_outputs_12_payload_A_31_data = io_input_payload_A_31_data;
  assign io_outputs_12_payload_A_31_Final = io_input_payload_A_31_Final;
  assign io_outputs_12_payload_B_19_data = io_input_payload_B_19_data;
  assign io_outputs_12_payload_B_19_Final = io_input_payload_B_19_Final;
  assign io_outputs_12_payload_OpMode_post_Shift = io_input_payload_OpMode_post_Shift;
  assign io_outputs_12_payload_OpMode_do_PostTranspose = io_input_payload_OpMode_do_PostTranspose;
  assign io_outputs_12_payload_OpMode_MatrixOperation = io_input_payload_OpMode_MatrixOperation;
  assign io_outputs_12_payload_ID = io_input_payload_ID;
  assign io_outputs_12_fire = (io_outputs_12_valid && io_outputs_12_ready);
  assign io_outputs_13_valid = (io_input_valid && logic_linkEnable_13);
  assign io_outputs_13_fire = (io_outputs_13_valid && io_outputs_13_ready);
  assign io_outputs_14_valid = (io_input_valid && logic_linkEnable_14);
  assign io_outputs_14_fire = (io_outputs_14_valid && io_outputs_14_ready);
  assign io_outputs_15_valid = (io_input_valid && logic_linkEnable_15);
  assign io_outputs_15_fire = (io_outputs_15_valid && io_outputs_15_ready);
  assign io_outputs_16_valid = (io_input_valid && logic_linkEnable_16);
  assign io_outputs_16_fire = (io_outputs_16_valid && io_outputs_16_ready);
  assign io_outputs_17_valid = (io_input_valid && logic_linkEnable_17);
  assign io_outputs_17_fire = (io_outputs_17_valid && io_outputs_17_ready);
  assign io_outputs_18_valid = (io_input_valid && logic_linkEnable_18);
  assign io_outputs_18_fire = (io_outputs_18_valid && io_outputs_18_ready);
  assign io_outputs_19_valid = (io_input_valid && logic_linkEnable_19);
  assign io_outputs_19_fire = (io_outputs_19_valid && io_outputs_19_ready);
  assign io_outputs_20_valid = (io_input_valid && logic_linkEnable_20);
  assign io_outputs_20_fire = (io_outputs_20_valid && io_outputs_20_ready);
  assign io_outputs_21_valid = (io_input_valid && logic_linkEnable_21);
  assign io_outputs_21_fire = (io_outputs_21_valid && io_outputs_21_ready);
  assign io_outputs_22_valid = (io_input_valid && logic_linkEnable_22);
  assign io_outputs_22_fire = (io_outputs_22_valid && io_outputs_22_ready);
  assign io_outputs_23_valid = (io_input_valid && logic_linkEnable_23);
  assign io_outputs_23_fire = (io_outputs_23_valid && io_outputs_23_ready);
  assign io_outputs_24_valid = (io_input_valid && logic_linkEnable_24);
  assign io_outputs_24_fire = (io_outputs_24_valid && io_outputs_24_ready);
  assign io_outputs_25_valid = (io_input_valid && logic_linkEnable_25);
  assign io_outputs_25_fire = (io_outputs_25_valid && io_outputs_25_ready);
  assign io_outputs_26_valid = (io_input_valid && logic_linkEnable_26);
  assign io_outputs_26_fire = (io_outputs_26_valid && io_outputs_26_ready);
  assign io_outputs_27_valid = (io_input_valid && logic_linkEnable_27);
  assign io_outputs_27_fire = (io_outputs_27_valid && io_outputs_27_ready);
  assign io_outputs_28_valid = (io_input_valid && logic_linkEnable_28);
  assign io_outputs_28_fire = (io_outputs_28_valid && io_outputs_28_ready);
  assign io_outputs_29_valid = (io_input_valid && logic_linkEnable_29);
  assign io_outputs_29_fire = (io_outputs_29_valid && io_outputs_29_ready);
  assign io_outputs_30_valid = (io_input_valid && logic_linkEnable_30);
  assign io_outputs_30_fire = (io_outputs_30_valid && io_outputs_30_ready);
  assign io_outputs_31_valid = (io_input_valid && logic_linkEnable_31);
  assign io_outputs_31_fire = (io_outputs_31_valid && io_outputs_31_ready);
  assign io_outputs_32_valid = (io_input_valid && logic_linkEnable_32);
  assign io_outputs_32_fire = (io_outputs_32_valid && io_outputs_32_ready);
  assign io_outputs_33_valid = (io_input_valid && logic_linkEnable_33);
  assign io_outputs_33_fire = (io_outputs_33_valid && io_outputs_33_ready);
  assign io_outputs_34_valid = (io_input_valid && logic_linkEnable_34);
  assign io_outputs_34_fire = (io_outputs_34_valid && io_outputs_34_ready);
  assign io_outputs_35_valid = (io_input_valid && logic_linkEnable_35);
  assign io_outputs_35_fire = (io_outputs_35_valid && io_outputs_35_ready);
  assign io_outputs_36_valid = (io_input_valid && logic_linkEnable_36);
  assign io_outputs_36_fire = (io_outputs_36_valid && io_outputs_36_ready);
  assign io_outputs_37_valid = (io_input_valid && logic_linkEnable_37);
  assign io_outputs_37_fire = (io_outputs_37_valid && io_outputs_37_ready);
  assign io_outputs_38_valid = (io_input_valid && logic_linkEnable_38);
  assign io_outputs_38_fire = (io_outputs_38_valid && io_outputs_38_ready);
  assign io_outputs_39_valid = (io_input_valid && logic_linkEnable_39);
  assign io_outputs_39_fire = (io_outputs_39_valid && io_outputs_39_ready);
  assign io_outputs_40_valid = (io_input_valid && logic_linkEnable_40);
  assign io_outputs_40_fire = (io_outputs_40_valid && io_outputs_40_ready);
  assign io_outputs_41_valid = (io_input_valid && logic_linkEnable_41);
  assign io_outputs_41_fire = (io_outputs_41_valid && io_outputs_41_ready);
  assign io_outputs_42_valid = (io_input_valid && logic_linkEnable_42);
  assign io_outputs_42_fire = (io_outputs_42_valid && io_outputs_42_ready);
  assign io_outputs_43_valid = (io_input_valid && logic_linkEnable_43);
  assign io_outputs_43_fire = (io_outputs_43_valid && io_outputs_43_ready);
  assign io_outputs_44_valid = (io_input_valid && logic_linkEnable_44);
  assign io_outputs_44_fire = (io_outputs_44_valid && io_outputs_44_ready);
  assign io_outputs_45_valid = (io_input_valid && logic_linkEnable_45);
  assign io_outputs_45_fire = (io_outputs_45_valid && io_outputs_45_ready);
  assign io_outputs_46_valid = (io_input_valid && logic_linkEnable_46);
  assign io_outputs_46_fire = (io_outputs_46_valid && io_outputs_46_ready);
  assign io_outputs_47_valid = (io_input_valid && logic_linkEnable_47);
  assign io_outputs_47_fire = (io_outputs_47_valid && io_outputs_47_ready);
  assign io_outputs_48_valid = (io_input_valid && logic_linkEnable_48);
  assign io_outputs_48_fire = (io_outputs_48_valid && io_outputs_48_ready);
  assign io_outputs_49_valid = (io_input_valid && logic_linkEnable_49);
  assign io_outputs_49_fire = (io_outputs_49_valid && io_outputs_49_ready);
  assign io_outputs_50_valid = (io_input_valid && logic_linkEnable_50);
  assign io_outputs_50_fire = (io_outputs_50_valid && io_outputs_50_ready);
  always @(posedge SystolicArray2D_CC_core_clk or posedge SystolicArray2D_CC_core_reset) begin
    if(SystolicArray2D_CC_core_reset) begin
      logic_linkEnable_0 <= 1'b1;
      logic_linkEnable_1 <= 1'b1;
      logic_linkEnable_2 <= 1'b1;
      logic_linkEnable_3 <= 1'b1;
      logic_linkEnable_4 <= 1'b1;
      logic_linkEnable_5 <= 1'b1;
      logic_linkEnable_6 <= 1'b1;
      logic_linkEnable_7 <= 1'b1;
      logic_linkEnable_8 <= 1'b1;
      logic_linkEnable_9 <= 1'b1;
      logic_linkEnable_10 <= 1'b1;
      logic_linkEnable_11 <= 1'b1;
      logic_linkEnable_12 <= 1'b1;
      logic_linkEnable_13 <= 1'b1;
      logic_linkEnable_14 <= 1'b1;
      logic_linkEnable_15 <= 1'b1;
      logic_linkEnable_16 <= 1'b1;
      logic_linkEnable_17 <= 1'b1;
      logic_linkEnable_18 <= 1'b1;
      logic_linkEnable_19 <= 1'b1;
      logic_linkEnable_20 <= 1'b1;
      logic_linkEnable_21 <= 1'b1;
      logic_linkEnable_22 <= 1'b1;
      logic_linkEnable_23 <= 1'b1;
      logic_linkEnable_24 <= 1'b1;
      logic_linkEnable_25 <= 1'b1;
      logic_linkEnable_26 <= 1'b1;
      logic_linkEnable_27 <= 1'b1;
      logic_linkEnable_28 <= 1'b1;
      logic_linkEnable_29 <= 1'b1;
      logic_linkEnable_30 <= 1'b1;
      logic_linkEnable_31 <= 1'b1;
      logic_linkEnable_32 <= 1'b1;
      logic_linkEnable_33 <= 1'b1;
      logic_linkEnable_34 <= 1'b1;
      logic_linkEnable_35 <= 1'b1;
      logic_linkEnable_36 <= 1'b1;
      logic_linkEnable_37 <= 1'b1;
      logic_linkEnable_38 <= 1'b1;
      logic_linkEnable_39 <= 1'b1;
      logic_linkEnable_40 <= 1'b1;
      logic_linkEnable_41 <= 1'b1;
      logic_linkEnable_42 <= 1'b1;
      logic_linkEnable_43 <= 1'b1;
      logic_linkEnable_44 <= 1'b1;
      logic_linkEnable_45 <= 1'b1;
      logic_linkEnable_46 <= 1'b1;
      logic_linkEnable_47 <= 1'b1;
      logic_linkEnable_48 <= 1'b1;
      logic_linkEnable_49 <= 1'b1;
      logic_linkEnable_50 <= 1'b1;
    end else begin
      if(io_outputs_0_fire) begin
        logic_linkEnable_0 <= 1'b0;
      end
      if(io_outputs_1_fire) begin
        logic_linkEnable_1 <= 1'b0;
      end
      if(io_outputs_2_fire) begin
        logic_linkEnable_2 <= 1'b0;
      end
      if(io_outputs_3_fire) begin
        logic_linkEnable_3 <= 1'b0;
      end
      if(io_outputs_4_fire) begin
        logic_linkEnable_4 <= 1'b0;
      end
      if(io_outputs_5_fire) begin
        logic_linkEnable_5 <= 1'b0;
      end
      if(io_outputs_6_fire) begin
        logic_linkEnable_6 <= 1'b0;
      end
      if(io_outputs_7_fire) begin
        logic_linkEnable_7 <= 1'b0;
      end
      if(io_outputs_8_fire) begin
        logic_linkEnable_8 <= 1'b0;
      end
      if(io_outputs_9_fire) begin
        logic_linkEnable_9 <= 1'b0;
      end
      if(io_outputs_10_fire) begin
        logic_linkEnable_10 <= 1'b0;
      end
      if(io_outputs_11_fire) begin
        logic_linkEnable_11 <= 1'b0;
      end
      if(io_outputs_12_fire) begin
        logic_linkEnable_12 <= 1'b0;
      end
      if(io_outputs_13_fire) begin
        logic_linkEnable_13 <= 1'b0;
      end
      if(io_outputs_14_fire) begin
        logic_linkEnable_14 <= 1'b0;
      end
      if(io_outputs_15_fire) begin
        logic_linkEnable_15 <= 1'b0;
      end
      if(io_outputs_16_fire) begin
        logic_linkEnable_16 <= 1'b0;
      end
      if(io_outputs_17_fire) begin
        logic_linkEnable_17 <= 1'b0;
      end
      if(io_outputs_18_fire) begin
        logic_linkEnable_18 <= 1'b0;
      end
      if(io_outputs_19_fire) begin
        logic_linkEnable_19 <= 1'b0;
      end
      if(io_outputs_20_fire) begin
        logic_linkEnable_20 <= 1'b0;
      end
      if(io_outputs_21_fire) begin
        logic_linkEnable_21 <= 1'b0;
      end
      if(io_outputs_22_fire) begin
        logic_linkEnable_22 <= 1'b0;
      end
      if(io_outputs_23_fire) begin
        logic_linkEnable_23 <= 1'b0;
      end
      if(io_outputs_24_fire) begin
        logic_linkEnable_24 <= 1'b0;
      end
      if(io_outputs_25_fire) begin
        logic_linkEnable_25 <= 1'b0;
      end
      if(io_outputs_26_fire) begin
        logic_linkEnable_26 <= 1'b0;
      end
      if(io_outputs_27_fire) begin
        logic_linkEnable_27 <= 1'b0;
      end
      if(io_outputs_28_fire) begin
        logic_linkEnable_28 <= 1'b0;
      end
      if(io_outputs_29_fire) begin
        logic_linkEnable_29 <= 1'b0;
      end
      if(io_outputs_30_fire) begin
        logic_linkEnable_30 <= 1'b0;
      end
      if(io_outputs_31_fire) begin
        logic_linkEnable_31 <= 1'b0;
      end
      if(io_outputs_32_fire) begin
        logic_linkEnable_32 <= 1'b0;
      end
      if(io_outputs_33_fire) begin
        logic_linkEnable_33 <= 1'b0;
      end
      if(io_outputs_34_fire) begin
        logic_linkEnable_34 <= 1'b0;
      end
      if(io_outputs_35_fire) begin
        logic_linkEnable_35 <= 1'b0;
      end
      if(io_outputs_36_fire) begin
        logic_linkEnable_36 <= 1'b0;
      end
      if(io_outputs_37_fire) begin
        logic_linkEnable_37 <= 1'b0;
      end
      if(io_outputs_38_fire) begin
        logic_linkEnable_38 <= 1'b0;
      end
      if(io_outputs_39_fire) begin
        logic_linkEnable_39 <= 1'b0;
      end
      if(io_outputs_40_fire) begin
        logic_linkEnable_40 <= 1'b0;
      end
      if(io_outputs_41_fire) begin
        logic_linkEnable_41 <= 1'b0;
      end
      if(io_outputs_42_fire) begin
        logic_linkEnable_42 <= 1'b0;
      end
      if(io_outputs_43_fire) begin
        logic_linkEnable_43 <= 1'b0;
      end
      if(io_outputs_44_fire) begin
        logic_linkEnable_44 <= 1'b0;
      end
      if(io_outputs_45_fire) begin
        logic_linkEnable_45 <= 1'b0;
      end
      if(io_outputs_46_fire) begin
        logic_linkEnable_46 <= 1'b0;
      end
      if(io_outputs_47_fire) begin
        logic_linkEnable_47 <= 1'b0;
      end
      if(io_outputs_48_fire) begin
        logic_linkEnable_48 <= 1'b0;
      end
      if(io_outputs_49_fire) begin
        logic_linkEnable_49 <= 1'b0;
      end
      if(io_outputs_50_fire) begin
        logic_linkEnable_50 <= 1'b0;
      end
      if(io_input_ready) begin
        logic_linkEnable_0 <= 1'b1;
        logic_linkEnable_1 <= 1'b1;
        logic_linkEnable_2 <= 1'b1;
        logic_linkEnable_3 <= 1'b1;
        logic_linkEnable_4 <= 1'b1;
        logic_linkEnable_5 <= 1'b1;
        logic_linkEnable_6 <= 1'b1;
        logic_linkEnable_7 <= 1'b1;
        logic_linkEnable_8 <= 1'b1;
        logic_linkEnable_9 <= 1'b1;
        logic_linkEnable_10 <= 1'b1;
        logic_linkEnable_11 <= 1'b1;
        logic_linkEnable_12 <= 1'b1;
        logic_linkEnable_13 <= 1'b1;
        logic_linkEnable_14 <= 1'b1;
        logic_linkEnable_15 <= 1'b1;
        logic_linkEnable_16 <= 1'b1;
        logic_linkEnable_17 <= 1'b1;
        logic_linkEnable_18 <= 1'b1;
        logic_linkEnable_19 <= 1'b1;
        logic_linkEnable_20 <= 1'b1;
        logic_linkEnable_21 <= 1'b1;
        logic_linkEnable_22 <= 1'b1;
        logic_linkEnable_23 <= 1'b1;
        logic_linkEnable_24 <= 1'b1;
        logic_linkEnable_25 <= 1'b1;
        logic_linkEnable_26 <= 1'b1;
        logic_linkEnable_27 <= 1'b1;
        logic_linkEnable_28 <= 1'b1;
        logic_linkEnable_29 <= 1'b1;
        logic_linkEnable_30 <= 1'b1;
        logic_linkEnable_31 <= 1'b1;
        logic_linkEnable_32 <= 1'b1;
        logic_linkEnable_33 <= 1'b1;
        logic_linkEnable_34 <= 1'b1;
        logic_linkEnable_35 <= 1'b1;
        logic_linkEnable_36 <= 1'b1;
        logic_linkEnable_37 <= 1'b1;
        logic_linkEnable_38 <= 1'b1;
        logic_linkEnable_39 <= 1'b1;
        logic_linkEnable_40 <= 1'b1;
        logic_linkEnable_41 <= 1'b1;
        logic_linkEnable_42 <= 1'b1;
        logic_linkEnable_43 <= 1'b1;
        logic_linkEnable_44 <= 1'b1;
        logic_linkEnable_45 <= 1'b1;
        logic_linkEnable_46 <= 1'b1;
        logic_linkEnable_47 <= 1'b1;
        logic_linkEnable_48 <= 1'b1;
        logic_linkEnable_49 <= 1'b1;
        logic_linkEnable_50 <= 1'b1;
      end
    end
  end


endmodule

// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : SystolicArray2DUnitSpecial_1
// Git hash  : e7984c6fd05794f7617e004bdecebf29e04e3020

`timescale 1ns/1ps 
module SystolicArray2DUnitSpecial_1 (
  input  wire          io_upStream_valid,
  output reg           io_upStream_ready,
  input  wire          io_upStream_payload_A_last,
  input  wire [23:0]   io_upStream_payload_A_fragment,
  input  wire          io_upStream_payload_B_last,
  input  wire [23:0]   io_upStream_payload_B_fragment,
  input  wire [1:0]    io_upStream_payload_Ctrl_Mode,
  input  wire          io_upStream_payload_Ctrl_Transpose,
  input  wire [5:0]    io_upStream_payload_Ctrl_Shift,
  input  wire [3:0]    io_upStream_payload_ID,
  output wire          io_downStream_valid,
  input  wire          io_downStream_ready,
  output wire          io_downStream_payload_A_last,
  output wire [23:0]   io_downStream_payload_A_fragment,
  output wire          io_downStream_payload_B_last,
  output wire [23:0]   io_downStream_payload_B_fragment,
  output wire          io_result_valid,
  input  wire          io_result_ready,
  output wire [23:0]   io_result_payload_Z_fragment,
  output wire [1:0]    io_result_payload_Ctrl_Mode,
  output wire          io_result_payload_Ctrl_Transpose,
  output wire [3:0]    io_result_payload_ID,
  input  wire          SystolicArray2D_CC_core_clk,
  input  wire          SystolicArray2D_CC_core_reset
);
  localparam MatrixOperation_TypeDef_MatMul = 2'd0;
  localparam MatrixOperation_TypeDef_ElementAdd = 2'd1;
  localparam MatrixOperation_TypeDef_ElementMul = 2'd2;
  localparam MatrixOperation_TypeDef_ElementMax = 2'd3;

  reg        [52:0]   Shift_Node_logic_instSIntShifter_io_input;
  wire       [23:0]   Shift_Node_logic_instSIntShifter_io_output;
  wire       [23:0]   _zz_MulMax_Node_PAYLOAD_greater;
  wire       [52:0]   _zz_Add_Node_PAYLOAD_sum_fragment;
  wire                Add_Node_isReady;
  wire                Filter_Node_isValid;
  wire       [52:0]   Filter_Node_PAYLOAD_sum_fragment;
  wire       [47:0]   Filter_Node_PAYLOAD_greater;
  wire       [47:0]   Filter_Node_PAYLOAD_product_fragment;
  wire       [3:0]    Filter_Node_PAYLOAD_ID;
  wire       [1:0]    Filter_Node_PAYLOAD_Ctrl_Mode;
  wire                Filter_Node_PAYLOAD_Ctrl_Transpose;
  wire       [5:0]    Filter_Node_PAYLOAD_Ctrl_Shift;
  wire                Filter_Node_isReady;
  wire                Add_Node_isValid;
  wire                MulMax_Node_isValid;
  reg        [47:0]   Add_Node_PAYLOAD_greater;
  reg        [3:0]    Add_Node_PAYLOAD_ID;
  reg                 Shift_Node_valid;
  reg                 Filter_Node_valid;
  reg                 Add_Node_valid;
  reg                 MulMax_Node_ready;
  wire                Add_Node_ready;
  reg                 Filter_Node_ready;
  reg        [3:0]    Shift_Node_PAYLOAD_ID;
  wire                Shift_Node_ready;
  wire                Shift_Node_isValid;
  wire       [23:0]   Shift_Node_PAYLOAD_result;
  reg        [47:0]   Shift_Node_PAYLOAD_product_fragment;
  reg        [47:0]   Shift_Node_PAYLOAD_greater;
  reg        [52:0]   Shift_Node_PAYLOAD_sum_fragment;
  reg        [1:0]    Shift_Node_PAYLOAD_Ctrl_Mode;
  reg                 Shift_Node_PAYLOAD_Ctrl_Transpose;
  reg        [5:0]    Shift_Node_PAYLOAD_Ctrl_Shift;
  reg        [23:0]   Add_Node_PAYLOAD_B_fragment;
  reg        [23:0]   Add_Node_PAYLOAD_A_fragment;
  reg                 _zz_CL_Add2Filter_terminateRequest_SystolicArray2DUnitSpecial_l132;
  wire                Add_Node_isFiring;
  reg                 Add_Node_PAYLOAD_product_last;
  reg        [47:0]   Add_Node_PAYLOAD_product_fragment;
  reg        [1:0]    Add_Node_PAYLOAD_Ctrl_Mode;
  reg                 Add_Node_PAYLOAD_Ctrl_Transpose;
  reg        [5:0]    Add_Node_PAYLOAD_Ctrl_Shift;
  reg        [47:0]   Add_Node_PAYLOAD_addend_2_fragment;
  reg        [52:0]   Add_Node_PAYLOAD_addend_1_fragment;
  wire                Add_Node_PAYLOAD_sum_last;
  wire       [52:0]   Add_Node_PAYLOAD_sum_fragment;
  reg        [23:0]   MulMax_Node_PAYLOAD_compare_2;
  reg        [23:0]   MulMax_Node_PAYLOAD_compare_1;
  wire       [47:0]   MulMax_Node_PAYLOAD_greater;
  reg                 MulMax_Node_PAYLOAD_factor_2_last;
  reg        [23:0]   MulMax_Node_PAYLOAD_factor_2_fragment;
  reg                 MulMax_Node_PAYLOAD_factor_1_last;
  reg        [23:0]   MulMax_Node_PAYLOAD_factor_1_fragment;
  wire                MulMax_Node_PAYLOAD_product_last;
  wire       [47:0]   MulMax_Node_PAYLOAD_product_fragment;
  wire       [3:0]    MulMax_Node_PAYLOAD_ID;
  wire       [1:0]    MulMax_Node_PAYLOAD_Ctrl_Mode;
  wire                MulMax_Node_PAYLOAD_Ctrl_Transpose;
  wire       [5:0]    MulMax_Node_PAYLOAD_Ctrl_Shift;
  wire                MulMax_Node_PAYLOAD_B_last;
  wire       [23:0]   MulMax_Node_PAYLOAD_B_fragment;
  wire                MulMax_Node_PAYLOAD_A_last;
  wire       [23:0]   MulMax_Node_PAYLOAD_A_fragment;
  wire                MulMax_Node_isCancel;
  wire                MulMax_Node_isReady;
  wire                MulMax_Node_valid;
  wire                upStream_for_downStream_valid;
  wire                upStream_for_downStream_ready;
  wire                upStream_for_downStream_payload_A_last;
  wire       [23:0]   upStream_for_downStream_payload_A_fragment;
  wire                upStream_for_downStream_payload_B_last;
  wire       [23:0]   upStream_for_downStream_payload_B_fragment;
  wire                upStream_for_result_valid;
  wire                upStream_for_result_ready;
  wire                upStream_for_result_payload_A_last;
  wire       [23:0]   upStream_for_result_payload_A_fragment;
  wire                upStream_for_result_payload_B_last;
  wire       [23:0]   upStream_for_result_payload_B_fragment;
  wire       [1:0]    upStream_for_result_payload_Ctrl_Mode;
  wire                upStream_for_result_payload_Ctrl_Transpose;
  wire       [5:0]    upStream_for_result_payload_Ctrl_Shift;
  wire       [3:0]    upStream_for_result_payload_ID;
  reg                 io_upStream_fork2_logic_linkEnable_0;
  reg                 io_upStream_fork2_logic_linkEnable_1;
  wire                when_Stream_l1088;
  wire                when_Stream_l1088_1;
  wire                upStream_for_downStream_fire;
  wire                upStream_for_result_fire;
  wire                upStream_for_downStream_s2mPipe_valid;
  reg                 upStream_for_downStream_s2mPipe_ready;
  wire                upStream_for_downStream_s2mPipe_payload_A_last;
  wire       [23:0]   upStream_for_downStream_s2mPipe_payload_A_fragment;
  wire                upStream_for_downStream_s2mPipe_payload_B_last;
  wire       [23:0]   upStream_for_downStream_s2mPipe_payload_B_fragment;
  reg                 upStream_for_downStream_rValidN;
  reg                 upStream_for_downStream_rData_A_last;
  reg        [23:0]   upStream_for_downStream_rData_A_fragment;
  reg                 upStream_for_downStream_rData_B_last;
  reg        [23:0]   upStream_for_downStream_rData_B_fragment;
  wire                upStream_for_downStream_s2mPipe_m2sPipe_valid;
  wire                upStream_for_downStream_s2mPipe_m2sPipe_ready;
  wire                upStream_for_downStream_s2mPipe_m2sPipe_payload_A_last;
  wire       [23:0]   upStream_for_downStream_s2mPipe_m2sPipe_payload_A_fragment;
  wire                upStream_for_downStream_s2mPipe_m2sPipe_payload_B_last;
  wire       [23:0]   upStream_for_downStream_s2mPipe_m2sPipe_payload_B_fragment;
  reg                 upStream_for_downStream_s2mPipe_rValid;
  reg                 upStream_for_downStream_s2mPipe_rData_A_last;
  reg        [23:0]   upStream_for_downStream_s2mPipe_rData_A_fragment;
  reg                 upStream_for_downStream_s2mPipe_rData_B_last;
  reg        [23:0]   upStream_for_downStream_s2mPipe_rData_B_fragment;
  wire                when_Stream_l393;
  wire                upStream_for_result_s2mPipe_valid;
  reg                 upStream_for_result_s2mPipe_ready;
  wire                upStream_for_result_s2mPipe_payload_A_last;
  wire       [23:0]   upStream_for_result_s2mPipe_payload_A_fragment;
  wire                upStream_for_result_s2mPipe_payload_B_last;
  wire       [23:0]   upStream_for_result_s2mPipe_payload_B_fragment;
  wire       [1:0]    upStream_for_result_s2mPipe_payload_Ctrl_Mode;
  wire                upStream_for_result_s2mPipe_payload_Ctrl_Transpose;
  wire       [5:0]    upStream_for_result_s2mPipe_payload_Ctrl_Shift;
  wire       [3:0]    upStream_for_result_s2mPipe_payload_ID;
  reg                 upStream_for_result_rValidN;
  reg                 upStream_for_result_rData_A_last;
  reg        [23:0]   upStream_for_result_rData_A_fragment;
  reg                 upStream_for_result_rData_B_last;
  reg        [23:0]   upStream_for_result_rData_B_fragment;
  reg        [1:0]    upStream_for_result_rData_Ctrl_Mode;
  reg                 upStream_for_result_rData_Ctrl_Transpose;
  reg        [5:0]    upStream_for_result_rData_Ctrl_Shift;
  reg        [3:0]    upStream_for_result_rData_ID;
  wire       [1:0]    _zz_upStream_for_result_s2mPipe_payload_Ctrl_Mode;
  wire                upStream_for_result_s2mPipe_m2sPipe_valid;
  wire                upStream_for_result_s2mPipe_m2sPipe_ready;
  wire                upStream_for_result_s2mPipe_m2sPipe_payload_A_last;
  wire       [23:0]   upStream_for_result_s2mPipe_m2sPipe_payload_A_fragment;
  wire                upStream_for_result_s2mPipe_m2sPipe_payload_B_last;
  wire       [23:0]   upStream_for_result_s2mPipe_m2sPipe_payload_B_fragment;
  wire       [1:0]    upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Mode;
  wire                upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Transpose;
  wire       [5:0]    upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Shift;
  wire       [3:0]    upStream_for_result_s2mPipe_m2sPipe_payload_ID;
  reg                 upStream_for_result_s2mPipe_rValid;
  reg                 upStream_for_result_s2mPipe_rData_A_last;
  reg        [23:0]   upStream_for_result_s2mPipe_rData_A_fragment;
  reg                 upStream_for_result_s2mPipe_rData_B_last;
  reg        [23:0]   upStream_for_result_s2mPipe_rData_B_fragment;
  reg        [1:0]    upStream_for_result_s2mPipe_rData_Ctrl_Mode;
  reg                 upStream_for_result_s2mPipe_rData_Ctrl_Transpose;
  reg        [5:0]    upStream_for_result_s2mPipe_rData_Ctrl_Shift;
  reg        [3:0]    upStream_for_result_s2mPipe_rData_ID;
  wire                when_Stream_l393_1;
  wire                when_SystolicArray2DUnitSpecial_l83;
  wire                when_SystolicArray2DUnitSpecial_l88;
  wire                when_SystolicArray2DUnitSpecial_l95;
  reg        [52:0]   reg_ProductSum;
  wire                when_SystolicArray2DUnitSpecial_l121;
  wire                when_SystolicArray2DUnitSpecial_l126;
  wire                CL_Add2Filter_terminateRequest_SystolicArray2DUnitSpecial_l132;
  wire                when_SystolicArray2DUnitSpecial_l135;
  wire                when_SystolicArray2DUnitSpecial_l154;
  wire                when_SystolicArray2DUnitSpecial_l156;
  wire                when_SystolicArray2DUnitSpecial_l158;
  wire                when_SystolicArray2DUnitSpecial_l160;
  wire                when_StageLink_l67;
  wire                when_CtrlLink_l157;
  wire                when_StageLink_l67_1;
  `ifndef SYNTHESIS
  reg [79:0] Filter_Node_PAYLOAD_Ctrl_Mode_string;
  reg [79:0] Shift_Node_PAYLOAD_Ctrl_Mode_string;
  reg [79:0] Add_Node_PAYLOAD_Ctrl_Mode_string;
  reg [79:0] MulMax_Node_PAYLOAD_Ctrl_Mode_string;
  reg [79:0] io_upStream_payload_Ctrl_Mode_string;
  reg [79:0] io_result_payload_Ctrl_Mode_string;
  reg [79:0] upStream_for_result_payload_Ctrl_Mode_string;
  reg [79:0] upStream_for_result_s2mPipe_payload_Ctrl_Mode_string;
  reg [79:0] upStream_for_result_rData_Ctrl_Mode_string;
  reg [79:0] _zz_upStream_for_result_s2mPipe_payload_Ctrl_Mode_string;
  reg [79:0] upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Mode_string;
  reg [79:0] upStream_for_result_s2mPipe_rData_Ctrl_Mode_string;
  `endif


  assign _zz_MulMax_Node_PAYLOAD_greater = (($signed(MulMax_Node_PAYLOAD_compare_1) < $signed(MulMax_Node_PAYLOAD_compare_2)) ? MulMax_Node_PAYLOAD_compare_2 : MulMax_Node_PAYLOAD_compare_1);
  assign _zz_Add_Node_PAYLOAD_sum_fragment = {{5{Add_Node_PAYLOAD_addend_2_fragment[47]}}, Add_Node_PAYLOAD_addend_2_fragment};
  SIntShifter Shift_Node_logic_instSIntShifter (
    .io_input       (Shift_Node_logic_instSIntShifter_io_input[52:0] ), //i
    .io_shiftAmount (Shift_Node_PAYLOAD_Ctrl_Shift[5:0]              ), //i
    .io_output      (Shift_Node_logic_instSIntShifter_io_output[23:0])  //o
  );
  `ifndef SYNTHESIS
  always @(*) begin
    case(Filter_Node_PAYLOAD_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : Filter_Node_PAYLOAD_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : Filter_Node_PAYLOAD_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : Filter_Node_PAYLOAD_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : Filter_Node_PAYLOAD_Ctrl_Mode_string = "ElementMax";
      default : Filter_Node_PAYLOAD_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(Shift_Node_PAYLOAD_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : Shift_Node_PAYLOAD_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : Shift_Node_PAYLOAD_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : Shift_Node_PAYLOAD_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : Shift_Node_PAYLOAD_Ctrl_Mode_string = "ElementMax";
      default : Shift_Node_PAYLOAD_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(Add_Node_PAYLOAD_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : Add_Node_PAYLOAD_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : Add_Node_PAYLOAD_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : Add_Node_PAYLOAD_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : Add_Node_PAYLOAD_Ctrl_Mode_string = "ElementMax";
      default : Add_Node_PAYLOAD_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(MulMax_Node_PAYLOAD_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : MulMax_Node_PAYLOAD_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : MulMax_Node_PAYLOAD_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : MulMax_Node_PAYLOAD_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : MulMax_Node_PAYLOAD_Ctrl_Mode_string = "ElementMax";
      default : MulMax_Node_PAYLOAD_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_upStream_payload_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : io_upStream_payload_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_upStream_payload_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_upStream_payload_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_upStream_payload_Ctrl_Mode_string = "ElementMax";
      default : io_upStream_payload_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(io_result_payload_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : io_result_payload_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : io_result_payload_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : io_result_payload_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : io_result_payload_Ctrl_Mode_string = "ElementMax";
      default : io_result_payload_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(upStream_for_result_payload_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : upStream_for_result_payload_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : upStream_for_result_payload_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : upStream_for_result_payload_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : upStream_for_result_payload_Ctrl_Mode_string = "ElementMax";
      default : upStream_for_result_payload_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(upStream_for_result_s2mPipe_payload_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : upStream_for_result_s2mPipe_payload_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : upStream_for_result_s2mPipe_payload_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : upStream_for_result_s2mPipe_payload_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : upStream_for_result_s2mPipe_payload_Ctrl_Mode_string = "ElementMax";
      default : upStream_for_result_s2mPipe_payload_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(upStream_for_result_rData_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : upStream_for_result_rData_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : upStream_for_result_rData_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : upStream_for_result_rData_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : upStream_for_result_rData_Ctrl_Mode_string = "ElementMax";
      default : upStream_for_result_rData_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(_zz_upStream_for_result_s2mPipe_payload_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : _zz_upStream_for_result_s2mPipe_payload_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : _zz_upStream_for_result_s2mPipe_payload_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : _zz_upStream_for_result_s2mPipe_payload_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : _zz_upStream_for_result_s2mPipe_payload_Ctrl_Mode_string = "ElementMax";
      default : _zz_upStream_for_result_s2mPipe_payload_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Mode_string = "ElementMax";
      default : upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(upStream_for_result_s2mPipe_rData_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : upStream_for_result_s2mPipe_rData_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : upStream_for_result_s2mPipe_rData_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : upStream_for_result_s2mPipe_rData_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : upStream_for_result_s2mPipe_rData_Ctrl_Mode_string = "ElementMax";
      default : upStream_for_result_s2mPipe_rData_Ctrl_Mode_string = "??????????";
    endcase
  end
  `endif

  always @(*) begin
    _zz_CL_Add2Filter_terminateRequest_SystolicArray2DUnitSpecial_l132 = 1'b0;
    if(when_SystolicArray2DUnitSpecial_l121) begin
      if(Add_Node_isFiring) begin
        if(!when_SystolicArray2DUnitSpecial_l126) begin
          _zz_CL_Add2Filter_terminateRequest_SystolicArray2DUnitSpecial_l132 = 1'b1;
        end
      end
    end
  end

  always @(*) begin
    io_upStream_ready = 1'b1;
    if(when_Stream_l1088) begin
      io_upStream_ready = 1'b0;
    end
    if(when_Stream_l1088_1) begin
      io_upStream_ready = 1'b0;
    end
  end

  assign when_Stream_l1088 = ((! upStream_for_downStream_ready) && io_upStream_fork2_logic_linkEnable_0);
  assign when_Stream_l1088_1 = ((! upStream_for_result_ready) && io_upStream_fork2_logic_linkEnable_1);
  assign upStream_for_downStream_valid = (io_upStream_valid && io_upStream_fork2_logic_linkEnable_0);
  assign upStream_for_downStream_payload_A_last = io_upStream_payload_A_last;
  assign upStream_for_downStream_payload_A_fragment = io_upStream_payload_A_fragment;
  assign upStream_for_downStream_payload_B_last = io_upStream_payload_B_last;
  assign upStream_for_downStream_payload_B_fragment = io_upStream_payload_B_fragment;
  assign upStream_for_downStream_fire = (upStream_for_downStream_valid && upStream_for_downStream_ready);
  assign upStream_for_result_valid = (io_upStream_valid && io_upStream_fork2_logic_linkEnable_1);
  assign upStream_for_result_payload_A_last = io_upStream_payload_A_last;
  assign upStream_for_result_payload_A_fragment = io_upStream_payload_A_fragment;
  assign upStream_for_result_payload_B_last = io_upStream_payload_B_last;
  assign upStream_for_result_payload_B_fragment = io_upStream_payload_B_fragment;
  assign upStream_for_result_payload_Ctrl_Mode = io_upStream_payload_Ctrl_Mode;
  assign upStream_for_result_payload_Ctrl_Transpose = io_upStream_payload_Ctrl_Transpose;
  assign upStream_for_result_payload_Ctrl_Shift = io_upStream_payload_Ctrl_Shift;
  assign upStream_for_result_payload_ID = io_upStream_payload_ID;
  assign upStream_for_result_fire = (upStream_for_result_valid && upStream_for_result_ready);
  assign upStream_for_downStream_ready = upStream_for_downStream_rValidN;
  assign upStream_for_downStream_s2mPipe_valid = (upStream_for_downStream_valid || (! upStream_for_downStream_rValidN));
  assign upStream_for_downStream_s2mPipe_payload_A_last = (upStream_for_downStream_rValidN ? upStream_for_downStream_payload_A_last : upStream_for_downStream_rData_A_last);
  assign upStream_for_downStream_s2mPipe_payload_A_fragment = (upStream_for_downStream_rValidN ? upStream_for_downStream_payload_A_fragment : upStream_for_downStream_rData_A_fragment);
  assign upStream_for_downStream_s2mPipe_payload_B_last = (upStream_for_downStream_rValidN ? upStream_for_downStream_payload_B_last : upStream_for_downStream_rData_B_last);
  assign upStream_for_downStream_s2mPipe_payload_B_fragment = (upStream_for_downStream_rValidN ? upStream_for_downStream_payload_B_fragment : upStream_for_downStream_rData_B_fragment);
  always @(*) begin
    upStream_for_downStream_s2mPipe_ready = upStream_for_downStream_s2mPipe_m2sPipe_ready;
    if(when_Stream_l393) begin
      upStream_for_downStream_s2mPipe_ready = 1'b1;
    end
  end

  assign when_Stream_l393 = (! upStream_for_downStream_s2mPipe_m2sPipe_valid);
  assign upStream_for_downStream_s2mPipe_m2sPipe_valid = upStream_for_downStream_s2mPipe_rValid;
  assign upStream_for_downStream_s2mPipe_m2sPipe_payload_A_last = upStream_for_downStream_s2mPipe_rData_A_last;
  assign upStream_for_downStream_s2mPipe_m2sPipe_payload_A_fragment = upStream_for_downStream_s2mPipe_rData_A_fragment;
  assign upStream_for_downStream_s2mPipe_m2sPipe_payload_B_last = upStream_for_downStream_s2mPipe_rData_B_last;
  assign upStream_for_downStream_s2mPipe_m2sPipe_payload_B_fragment = upStream_for_downStream_s2mPipe_rData_B_fragment;
  assign io_downStream_valid = upStream_for_downStream_s2mPipe_m2sPipe_valid;
  assign upStream_for_downStream_s2mPipe_m2sPipe_ready = io_downStream_ready;
  assign io_downStream_payload_A_last = upStream_for_downStream_s2mPipe_m2sPipe_payload_A_last;
  assign io_downStream_payload_A_fragment = upStream_for_downStream_s2mPipe_m2sPipe_payload_A_fragment;
  assign io_downStream_payload_B_last = upStream_for_downStream_s2mPipe_m2sPipe_payload_B_last;
  assign io_downStream_payload_B_fragment = upStream_for_downStream_s2mPipe_m2sPipe_payload_B_fragment;
  assign upStream_for_result_ready = upStream_for_result_rValidN;
  assign upStream_for_result_s2mPipe_valid = (upStream_for_result_valid || (! upStream_for_result_rValidN));
  assign _zz_upStream_for_result_s2mPipe_payload_Ctrl_Mode = (upStream_for_result_rValidN ? upStream_for_result_payload_Ctrl_Mode : upStream_for_result_rData_Ctrl_Mode);
  assign upStream_for_result_s2mPipe_payload_A_last = (upStream_for_result_rValidN ? upStream_for_result_payload_A_last : upStream_for_result_rData_A_last);
  assign upStream_for_result_s2mPipe_payload_A_fragment = (upStream_for_result_rValidN ? upStream_for_result_payload_A_fragment : upStream_for_result_rData_A_fragment);
  assign upStream_for_result_s2mPipe_payload_B_last = (upStream_for_result_rValidN ? upStream_for_result_payload_B_last : upStream_for_result_rData_B_last);
  assign upStream_for_result_s2mPipe_payload_B_fragment = (upStream_for_result_rValidN ? upStream_for_result_payload_B_fragment : upStream_for_result_rData_B_fragment);
  assign upStream_for_result_s2mPipe_payload_Ctrl_Mode = _zz_upStream_for_result_s2mPipe_payload_Ctrl_Mode;
  assign upStream_for_result_s2mPipe_payload_Ctrl_Transpose = (upStream_for_result_rValidN ? upStream_for_result_payload_Ctrl_Transpose : upStream_for_result_rData_Ctrl_Transpose);
  assign upStream_for_result_s2mPipe_payload_Ctrl_Shift = (upStream_for_result_rValidN ? upStream_for_result_payload_Ctrl_Shift : upStream_for_result_rData_Ctrl_Shift);
  assign upStream_for_result_s2mPipe_payload_ID = (upStream_for_result_rValidN ? upStream_for_result_payload_ID : upStream_for_result_rData_ID);
  always @(*) begin
    upStream_for_result_s2mPipe_ready = upStream_for_result_s2mPipe_m2sPipe_ready;
    if(when_Stream_l393_1) begin
      upStream_for_result_s2mPipe_ready = 1'b1;
    end
  end

  assign when_Stream_l393_1 = (! upStream_for_result_s2mPipe_m2sPipe_valid);
  assign upStream_for_result_s2mPipe_m2sPipe_valid = upStream_for_result_s2mPipe_rValid;
  assign upStream_for_result_s2mPipe_m2sPipe_payload_A_last = upStream_for_result_s2mPipe_rData_A_last;
  assign upStream_for_result_s2mPipe_m2sPipe_payload_A_fragment = upStream_for_result_s2mPipe_rData_A_fragment;
  assign upStream_for_result_s2mPipe_m2sPipe_payload_B_last = upStream_for_result_s2mPipe_rData_B_last;
  assign upStream_for_result_s2mPipe_m2sPipe_payload_B_fragment = upStream_for_result_s2mPipe_rData_B_fragment;
  assign upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Mode = upStream_for_result_s2mPipe_rData_Ctrl_Mode;
  assign upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Transpose = upStream_for_result_s2mPipe_rData_Ctrl_Transpose;
  assign upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Shift = upStream_for_result_s2mPipe_rData_Ctrl_Shift;
  assign upStream_for_result_s2mPipe_m2sPipe_payload_ID = upStream_for_result_s2mPipe_rData_ID;
  assign MulMax_Node_valid = upStream_for_result_s2mPipe_m2sPipe_valid;
  assign upStream_for_result_s2mPipe_m2sPipe_ready = (MulMax_Node_isReady || MulMax_Node_isCancel);
  assign MulMax_Node_PAYLOAD_A_last = upStream_for_result_s2mPipe_m2sPipe_payload_A_last;
  assign MulMax_Node_PAYLOAD_A_fragment = upStream_for_result_s2mPipe_m2sPipe_payload_A_fragment;
  assign MulMax_Node_PAYLOAD_B_last = upStream_for_result_s2mPipe_m2sPipe_payload_B_last;
  assign MulMax_Node_PAYLOAD_B_fragment = upStream_for_result_s2mPipe_m2sPipe_payload_B_fragment;
  assign MulMax_Node_PAYLOAD_Ctrl_Mode = upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Mode;
  assign MulMax_Node_PAYLOAD_Ctrl_Transpose = upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Transpose;
  assign MulMax_Node_PAYLOAD_Ctrl_Shift = upStream_for_result_s2mPipe_m2sPipe_payload_Ctrl_Shift;
  assign MulMax_Node_PAYLOAD_ID = upStream_for_result_s2mPipe_m2sPipe_payload_ID;
  assign MulMax_Node_PAYLOAD_product_fragment = ($signed(MulMax_Node_PAYLOAD_factor_1_fragment) * $signed(MulMax_Node_PAYLOAD_factor_2_fragment));
  assign MulMax_Node_PAYLOAD_product_last = (MulMax_Node_PAYLOAD_factor_1_last && MulMax_Node_PAYLOAD_factor_2_last);
  assign MulMax_Node_PAYLOAD_greater = {{24{_zz_MulMax_Node_PAYLOAD_greater[23]}}, _zz_MulMax_Node_PAYLOAD_greater};
  assign when_SystolicArray2DUnitSpecial_l83 = (MulMax_Node_PAYLOAD_Ctrl_Mode == MatrixOperation_TypeDef_MatMul);
  always @(*) begin
    if(when_SystolicArray2DUnitSpecial_l83) begin
      MulMax_Node_PAYLOAD_factor_1_last = MulMax_Node_PAYLOAD_A_last;
    end else begin
      if(when_SystolicArray2DUnitSpecial_l88) begin
        MulMax_Node_PAYLOAD_factor_1_last = MulMax_Node_PAYLOAD_A_last;
      end else begin
        if(when_SystolicArray2DUnitSpecial_l95) begin
          MulMax_Node_PAYLOAD_factor_1_last = MulMax_Node_PAYLOAD_A_last;
        end else begin
          MulMax_Node_PAYLOAD_factor_1_last = MulMax_Node_PAYLOAD_A_last;
        end
      end
    end
  end

  always @(*) begin
    if(when_SystolicArray2DUnitSpecial_l83) begin
      MulMax_Node_PAYLOAD_factor_1_fragment = MulMax_Node_PAYLOAD_A_fragment;
    end else begin
      if(when_SystolicArray2DUnitSpecial_l88) begin
        MulMax_Node_PAYLOAD_factor_1_fragment = 24'h0;
      end else begin
        if(when_SystolicArray2DUnitSpecial_l95) begin
          MulMax_Node_PAYLOAD_factor_1_fragment = MulMax_Node_PAYLOAD_A_fragment;
        end else begin
          MulMax_Node_PAYLOAD_factor_1_fragment = 24'h0;
        end
      end
    end
  end

  always @(*) begin
    if(when_SystolicArray2DUnitSpecial_l83) begin
      MulMax_Node_PAYLOAD_factor_2_last = MulMax_Node_PAYLOAD_B_last;
    end else begin
      if(when_SystolicArray2DUnitSpecial_l88) begin
        MulMax_Node_PAYLOAD_factor_2_last = MulMax_Node_PAYLOAD_B_last;
      end else begin
        if(when_SystolicArray2DUnitSpecial_l95) begin
          MulMax_Node_PAYLOAD_factor_2_last = MulMax_Node_PAYLOAD_B_last;
        end else begin
          MulMax_Node_PAYLOAD_factor_2_last = MulMax_Node_PAYLOAD_B_last;
        end
      end
    end
  end

  always @(*) begin
    if(when_SystolicArray2DUnitSpecial_l83) begin
      MulMax_Node_PAYLOAD_factor_2_fragment = MulMax_Node_PAYLOAD_B_fragment;
    end else begin
      if(when_SystolicArray2DUnitSpecial_l88) begin
        MulMax_Node_PAYLOAD_factor_2_fragment = 24'h0;
      end else begin
        if(when_SystolicArray2DUnitSpecial_l95) begin
          MulMax_Node_PAYLOAD_factor_2_fragment = MulMax_Node_PAYLOAD_B_fragment;
        end else begin
          MulMax_Node_PAYLOAD_factor_2_fragment = 24'h0;
        end
      end
    end
  end

  always @(*) begin
    if(when_SystolicArray2DUnitSpecial_l83) begin
      MulMax_Node_PAYLOAD_compare_1 = 24'h0;
    end else begin
      if(when_SystolicArray2DUnitSpecial_l88) begin
        MulMax_Node_PAYLOAD_compare_1 = MulMax_Node_PAYLOAD_A_fragment;
      end else begin
        if(when_SystolicArray2DUnitSpecial_l95) begin
          MulMax_Node_PAYLOAD_compare_1 = 24'h0;
        end else begin
          MulMax_Node_PAYLOAD_compare_1 = 24'h0;
        end
      end
    end
  end

  always @(*) begin
    if(when_SystolicArray2DUnitSpecial_l83) begin
      MulMax_Node_PAYLOAD_compare_2 = 24'h0;
    end else begin
      if(when_SystolicArray2DUnitSpecial_l88) begin
        MulMax_Node_PAYLOAD_compare_2 = MulMax_Node_PAYLOAD_B_fragment;
      end else begin
        if(when_SystolicArray2DUnitSpecial_l95) begin
          MulMax_Node_PAYLOAD_compare_2 = 24'h0;
        end else begin
          MulMax_Node_PAYLOAD_compare_2 = 24'h0;
        end
      end
    end
  end

  assign when_SystolicArray2DUnitSpecial_l88 = (MulMax_Node_PAYLOAD_Ctrl_Mode == MatrixOperation_TypeDef_ElementMax);
  assign when_SystolicArray2DUnitSpecial_l95 = (MulMax_Node_PAYLOAD_Ctrl_Mode == MatrixOperation_TypeDef_ElementMul);
  assign Add_Node_PAYLOAD_sum_fragment = ($signed(Add_Node_PAYLOAD_addend_1_fragment) + $signed(_zz_Add_Node_PAYLOAD_sum_fragment));
  assign Add_Node_PAYLOAD_sum_last = Add_Node_PAYLOAD_product_last;
  assign when_SystolicArray2DUnitSpecial_l121 = (Add_Node_PAYLOAD_Ctrl_Mode == MatrixOperation_TypeDef_MatMul);
  always @(*) begin
    if(when_SystolicArray2DUnitSpecial_l121) begin
      Add_Node_PAYLOAD_addend_1_fragment = reg_ProductSum;
    end else begin
      if(when_SystolicArray2DUnitSpecial_l135) begin
        Add_Node_PAYLOAD_addend_1_fragment = {{29{Add_Node_PAYLOAD_A_fragment[23]}}, Add_Node_PAYLOAD_A_fragment};
      end else begin
        Add_Node_PAYLOAD_addend_1_fragment = 53'h0;
      end
    end
  end

  always @(*) begin
    if(when_SystolicArray2DUnitSpecial_l121) begin
      Add_Node_PAYLOAD_addend_2_fragment = Add_Node_PAYLOAD_product_fragment;
    end else begin
      if(when_SystolicArray2DUnitSpecial_l135) begin
        Add_Node_PAYLOAD_addend_2_fragment = {{24{Add_Node_PAYLOAD_B_fragment[23]}}, Add_Node_PAYLOAD_B_fragment};
      end else begin
        Add_Node_PAYLOAD_addend_2_fragment = 48'h0;
      end
    end
  end

  assign when_SystolicArray2DUnitSpecial_l126 = (Add_Node_PAYLOAD_sum_last == 1'b1);
  assign CL_Add2Filter_terminateRequest_SystolicArray2DUnitSpecial_l132 = _zz_CL_Add2Filter_terminateRequest_SystolicArray2DUnitSpecial_l132;
  assign when_SystolicArray2DUnitSpecial_l135 = (Add_Node_PAYLOAD_Ctrl_Mode == MatrixOperation_TypeDef_ElementAdd);
  assign when_SystolicArray2DUnitSpecial_l154 = (Shift_Node_PAYLOAD_Ctrl_Mode == MatrixOperation_TypeDef_MatMul);
  always @(*) begin
    if(when_SystolicArray2DUnitSpecial_l154) begin
      Shift_Node_logic_instSIntShifter_io_input = Shift_Node_PAYLOAD_sum_fragment;
    end else begin
      if(when_SystolicArray2DUnitSpecial_l156) begin
        Shift_Node_logic_instSIntShifter_io_input = {{5{Shift_Node_PAYLOAD_greater[47]}}, Shift_Node_PAYLOAD_greater};
      end else begin
        if(when_SystolicArray2DUnitSpecial_l158) begin
          Shift_Node_logic_instSIntShifter_io_input = Shift_Node_PAYLOAD_sum_fragment;
        end else begin
          if(when_SystolicArray2DUnitSpecial_l160) begin
            Shift_Node_logic_instSIntShifter_io_input = {{5{Shift_Node_PAYLOAD_product_fragment[47]}}, Shift_Node_PAYLOAD_product_fragment};
          end else begin
            Shift_Node_logic_instSIntShifter_io_input = 53'h0;
          end
        end
      end
    end
  end

  assign when_SystolicArray2DUnitSpecial_l156 = (Shift_Node_PAYLOAD_Ctrl_Mode == MatrixOperation_TypeDef_ElementMax);
  assign when_SystolicArray2DUnitSpecial_l158 = (Shift_Node_PAYLOAD_Ctrl_Mode == MatrixOperation_TypeDef_ElementAdd);
  assign when_SystolicArray2DUnitSpecial_l160 = (Shift_Node_PAYLOAD_Ctrl_Mode == MatrixOperation_TypeDef_ElementMul);
  assign Shift_Node_PAYLOAD_result = Shift_Node_logic_instSIntShifter_io_output;
  assign io_result_valid = Shift_Node_isValid;
  assign Shift_Node_ready = io_result_ready;
  assign io_result_payload_Z_fragment = Shift_Node_PAYLOAD_result;
  assign io_result_payload_Ctrl_Mode = Shift_Node_PAYLOAD_Ctrl_Mode;
  assign io_result_payload_Ctrl_Transpose = Shift_Node_PAYLOAD_Ctrl_Transpose;
  assign io_result_payload_ID = Shift_Node_PAYLOAD_ID;
  always @(*) begin
    MulMax_Node_ready = Add_Node_ready;
    if(when_StageLink_l67) begin
      MulMax_Node_ready = 1'b1;
    end
  end

  assign when_StageLink_l67 = (! Add_Node_isValid);
  always @(*) begin
    Filter_Node_valid = Add_Node_valid;
    if(when_CtrlLink_l157) begin
      Filter_Node_valid = 1'b0;
    end
  end

  assign Add_Node_ready = Filter_Node_isReady;
  assign when_CtrlLink_l157 = (|CL_Add2Filter_terminateRequest_SystolicArray2DUnitSpecial_l132);
  assign Filter_Node_PAYLOAD_Ctrl_Mode = Add_Node_PAYLOAD_Ctrl_Mode;
  assign Filter_Node_PAYLOAD_Ctrl_Transpose = Add_Node_PAYLOAD_Ctrl_Transpose;
  assign Filter_Node_PAYLOAD_Ctrl_Shift = Add_Node_PAYLOAD_Ctrl_Shift;
  assign Filter_Node_PAYLOAD_ID = Add_Node_PAYLOAD_ID;
  assign Filter_Node_PAYLOAD_product_fragment = Add_Node_PAYLOAD_product_fragment;
  assign Filter_Node_PAYLOAD_greater = Add_Node_PAYLOAD_greater;
  assign Filter_Node_PAYLOAD_sum_fragment = Add_Node_PAYLOAD_sum_fragment;
  always @(*) begin
    Filter_Node_ready = Shift_Node_ready;
    if(when_StageLink_l67_1) begin
      Filter_Node_ready = 1'b1;
    end
  end

  assign when_StageLink_l67_1 = (! Shift_Node_isValid);
  assign MulMax_Node_isValid = MulMax_Node_valid;
  assign MulMax_Node_isReady = MulMax_Node_ready;
  assign MulMax_Node_isCancel = 1'b0;
  assign Add_Node_isFiring = (Add_Node_isValid && Add_Node_isReady);
  assign Add_Node_isValid = Add_Node_valid;
  assign Add_Node_isReady = Add_Node_ready;
  assign Filter_Node_isValid = Filter_Node_valid;
  assign Filter_Node_isReady = Filter_Node_ready;
  assign Shift_Node_isValid = Shift_Node_valid;
  always @(posedge SystolicArray2D_CC_core_clk or posedge SystolicArray2D_CC_core_reset) begin
    if(SystolicArray2D_CC_core_reset) begin
      io_upStream_fork2_logic_linkEnable_0 <= 1'b1;
      io_upStream_fork2_logic_linkEnable_1 <= 1'b1;
      upStream_for_downStream_rValidN <= 1'b1;
      upStream_for_downStream_s2mPipe_rValid <= 1'b0;
      upStream_for_result_rValidN <= 1'b1;
      upStream_for_result_s2mPipe_rValid <= 1'b0;
      reg_ProductSum <= 53'h0;
      Add_Node_valid <= 1'b0;
      Shift_Node_valid <= 1'b0;
    end else begin
      if(upStream_for_downStream_fire) begin
        io_upStream_fork2_logic_linkEnable_0 <= 1'b0;
      end
      if(upStream_for_result_fire) begin
        io_upStream_fork2_logic_linkEnable_1 <= 1'b0;
      end
      if(io_upStream_ready) begin
        io_upStream_fork2_logic_linkEnable_0 <= 1'b1;
        io_upStream_fork2_logic_linkEnable_1 <= 1'b1;
      end
      if(upStream_for_downStream_valid) begin
        upStream_for_downStream_rValidN <= 1'b0;
      end
      if(upStream_for_downStream_s2mPipe_ready) begin
        upStream_for_downStream_rValidN <= 1'b1;
      end
      if(upStream_for_downStream_s2mPipe_ready) begin
        upStream_for_downStream_s2mPipe_rValid <= upStream_for_downStream_s2mPipe_valid;
      end
      if(upStream_for_result_valid) begin
        upStream_for_result_rValidN <= 1'b0;
      end
      if(upStream_for_result_s2mPipe_ready) begin
        upStream_for_result_rValidN <= 1'b1;
      end
      if(upStream_for_result_s2mPipe_ready) begin
        upStream_for_result_s2mPipe_rValid <= upStream_for_result_s2mPipe_valid;
      end
      if(when_SystolicArray2DUnitSpecial_l121) begin
        if(Add_Node_isFiring) begin
          if(when_SystolicArray2DUnitSpecial_l126) begin
            reg_ProductSum <= 53'h0;
          end else begin
            reg_ProductSum <= Add_Node_PAYLOAD_sum_fragment;
          end
        end
      end
      if(MulMax_Node_isReady) begin
        Add_Node_valid <= MulMax_Node_isValid;
      end
      if(Filter_Node_isReady) begin
        Shift_Node_valid <= Filter_Node_isValid;
      end
    end
  end

  always @(posedge SystolicArray2D_CC_core_clk) begin
    if(upStream_for_downStream_ready) begin
      upStream_for_downStream_rData_A_last <= upStream_for_downStream_payload_A_last;
      upStream_for_downStream_rData_A_fragment <= upStream_for_downStream_payload_A_fragment;
      upStream_for_downStream_rData_B_last <= upStream_for_downStream_payload_B_last;
      upStream_for_downStream_rData_B_fragment <= upStream_for_downStream_payload_B_fragment;
    end
    if(upStream_for_downStream_s2mPipe_ready) begin
      upStream_for_downStream_s2mPipe_rData_A_last <= upStream_for_downStream_s2mPipe_payload_A_last;
      upStream_for_downStream_s2mPipe_rData_A_fragment <= upStream_for_downStream_s2mPipe_payload_A_fragment;
      upStream_for_downStream_s2mPipe_rData_B_last <= upStream_for_downStream_s2mPipe_payload_B_last;
      upStream_for_downStream_s2mPipe_rData_B_fragment <= upStream_for_downStream_s2mPipe_payload_B_fragment;
    end
    if(upStream_for_result_ready) begin
      upStream_for_result_rData_A_last <= upStream_for_result_payload_A_last;
      upStream_for_result_rData_A_fragment <= upStream_for_result_payload_A_fragment;
      upStream_for_result_rData_B_last <= upStream_for_result_payload_B_last;
      upStream_for_result_rData_B_fragment <= upStream_for_result_payload_B_fragment;
      upStream_for_result_rData_Ctrl_Mode <= upStream_for_result_payload_Ctrl_Mode;
      upStream_for_result_rData_Ctrl_Transpose <= upStream_for_result_payload_Ctrl_Transpose;
      upStream_for_result_rData_Ctrl_Shift <= upStream_for_result_payload_Ctrl_Shift;
      upStream_for_result_rData_ID <= upStream_for_result_payload_ID;
    end
    if(upStream_for_result_s2mPipe_ready) begin
      upStream_for_result_s2mPipe_rData_A_last <= upStream_for_result_s2mPipe_payload_A_last;
      upStream_for_result_s2mPipe_rData_A_fragment <= upStream_for_result_s2mPipe_payload_A_fragment;
      upStream_for_result_s2mPipe_rData_B_last <= upStream_for_result_s2mPipe_payload_B_last;
      upStream_for_result_s2mPipe_rData_B_fragment <= upStream_for_result_s2mPipe_payload_B_fragment;
      upStream_for_result_s2mPipe_rData_Ctrl_Mode <= upStream_for_result_s2mPipe_payload_Ctrl_Mode;
      upStream_for_result_s2mPipe_rData_Ctrl_Transpose <= upStream_for_result_s2mPipe_payload_Ctrl_Transpose;
      upStream_for_result_s2mPipe_rData_Ctrl_Shift <= upStream_for_result_s2mPipe_payload_Ctrl_Shift;
      upStream_for_result_s2mPipe_rData_ID <= upStream_for_result_s2mPipe_payload_ID;
    end
    Add_Node_PAYLOAD_Ctrl_Mode <= Add_Node_PAYLOAD_Ctrl_Mode;
    Add_Node_PAYLOAD_Ctrl_Transpose <= Add_Node_PAYLOAD_Ctrl_Transpose;
    Add_Node_PAYLOAD_Ctrl_Shift <= Add_Node_PAYLOAD_Ctrl_Shift;
    if(MulMax_Node_isReady) begin
      Add_Node_PAYLOAD_A_fragment <= MulMax_Node_PAYLOAD_A_fragment;
      Add_Node_PAYLOAD_B_fragment <= MulMax_Node_PAYLOAD_B_fragment;
      Add_Node_PAYLOAD_Ctrl_Mode <= MulMax_Node_PAYLOAD_Ctrl_Mode;
      Add_Node_PAYLOAD_Ctrl_Transpose <= MulMax_Node_PAYLOAD_Ctrl_Transpose;
      Add_Node_PAYLOAD_Ctrl_Shift <= MulMax_Node_PAYLOAD_Ctrl_Shift;
      Add_Node_PAYLOAD_ID <= MulMax_Node_PAYLOAD_ID;
      Add_Node_PAYLOAD_product_last <= MulMax_Node_PAYLOAD_product_last;
      Add_Node_PAYLOAD_product_fragment <= MulMax_Node_PAYLOAD_product_fragment;
      Add_Node_PAYLOAD_greater <= MulMax_Node_PAYLOAD_greater;
    end
    if(Filter_Node_isReady) begin
      Shift_Node_PAYLOAD_Ctrl_Mode <= Filter_Node_PAYLOAD_Ctrl_Mode;
      Shift_Node_PAYLOAD_Ctrl_Transpose <= Filter_Node_PAYLOAD_Ctrl_Transpose;
      Shift_Node_PAYLOAD_Ctrl_Shift <= Filter_Node_PAYLOAD_Ctrl_Shift;
      Shift_Node_PAYLOAD_ID <= Filter_Node_PAYLOAD_ID;
      Shift_Node_PAYLOAD_product_fragment <= Filter_Node_PAYLOAD_product_fragment;
      Shift_Node_PAYLOAD_greater <= Filter_Node_PAYLOAD_greater;
      Shift_Node_PAYLOAD_sum_fragment <= Filter_Node_PAYLOAD_sum_fragment;
    end
  end


endmodule

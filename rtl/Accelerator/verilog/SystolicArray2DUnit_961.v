// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : SystolicArray2DUnit_961
// Git hash  : e7984c6fd05794f7617e004bdecebf29e04e3020

`timescale 1ns/1ps 
module SystolicArray2DUnit_961 (
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

  wire       [23:0]   Shift_Node_logic_instSIntShifter_io_output;
  wire       [52:0]   _zz_Add_Node_PAYLOAD_sum_fragment;
  wire                Add_Node_isReady;
  wire                Filter_Node_isValid;
  wire       [52:0]   Filter_Node_PAYLOAD_sum_fragment;
  wire       [3:0]    Filter_Node_PAYLOAD_ID;
  wire       [1:0]    Filter_Node_PAYLOAD_Ctrl_Mode;
  wire                Filter_Node_PAYLOAD_Ctrl_Transpose;
  wire       [5:0]    Filter_Node_PAYLOAD_Ctrl_Shift;
  wire                Filter_Node_isReady;
  wire                Add_Node_isValid;
  wire                Mul_Node_isValid;
  reg        [3:0]    Add_Node_PAYLOAD_ID;
  reg                 Shift_Node_valid;
  reg                 Filter_Node_valid;
  reg                 Add_Node_valid;
  reg                 Mul_Node_ready;
  wire                Add_Node_ready;
  reg                 Filter_Node_ready;
  reg        [3:0]    Shift_Node_PAYLOAD_ID;
  wire                Shift_Node_ready;
  wire                Shift_Node_isValid;
  wire       [23:0]   Shift_Node_PAYLOAD_result;
  reg        [52:0]   Shift_Node_PAYLOAD_sum_fragment;
  reg        [1:0]    Shift_Node_PAYLOAD_Ctrl_Mode;
  reg                 Shift_Node_PAYLOAD_Ctrl_Transpose;
  reg        [5:0]    Shift_Node_PAYLOAD_Ctrl_Shift;
  reg                 _zz_CL_Add2Filter_terminateRequest_SystolicArray2DUnit_l222;
  wire                Add_Node_isFiring;
  reg                 Add_Node_PAYLOAD_product_last;
  reg        [47:0]   Add_Node_PAYLOAD_product_fragment;
  reg        [1:0]    Add_Node_PAYLOAD_Ctrl_Mode;
  reg                 Add_Node_PAYLOAD_Ctrl_Transpose;
  reg        [5:0]    Add_Node_PAYLOAD_Ctrl_Shift;
  wire       [47:0]   Add_Node_PAYLOAD_addend_2_fragment;
  wire       [52:0]   Add_Node_PAYLOAD_addend_1_fragment;
  wire                Add_Node_PAYLOAD_sum_last;
  wire       [52:0]   Add_Node_PAYLOAD_sum_fragment;
  wire                Mul_Node_PAYLOAD_factor_2_last;
  wire       [23:0]   Mul_Node_PAYLOAD_factor_2_fragment;
  wire                Mul_Node_PAYLOAD_factor_1_last;
  wire       [23:0]   Mul_Node_PAYLOAD_factor_1_fragment;
  wire                Mul_Node_PAYLOAD_product_last;
  wire       [47:0]   Mul_Node_PAYLOAD_product_fragment;
  wire       [3:0]    Mul_Node_PAYLOAD_ID;
  wire       [1:0]    Mul_Node_PAYLOAD_Ctrl_Mode;
  wire                Mul_Node_PAYLOAD_Ctrl_Transpose;
  wire       [5:0]    Mul_Node_PAYLOAD_Ctrl_Shift;
  wire                Mul_Node_PAYLOAD_B_last;
  wire       [23:0]   Mul_Node_PAYLOAD_B_fragment;
  wire                Mul_Node_PAYLOAD_A_last;
  wire       [23:0]   Mul_Node_PAYLOAD_A_fragment;
  wire                Mul_Node_isCancel;
  wire                Mul_Node_isReady;
  wire                Mul_Node_valid;
  wire                upStream_for_downStream_valid;
  wire                upStream_for_downStream_ready;
  wire                upStream_for_downStream_payload_A_last;
  wire       [23:0]   upStream_for_downStream_payload_A_fragment;
  wire                upStream_for_result_valid;
  reg                 upStream_for_result_ready;
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
  reg                 upStream_for_downStream_rValidN;
  reg                 upStream_for_downStream_rData_A_last;
  reg        [23:0]   upStream_for_downStream_rData_A_fragment;
  wire                upStream_for_downStream_s2mPipe_m2sPipe_valid;
  wire                upStream_for_downStream_s2mPipe_m2sPipe_ready;
  wire                upStream_for_downStream_s2mPipe_m2sPipe_payload_A_last;
  wire       [23:0]   upStream_for_downStream_s2mPipe_m2sPipe_payload_A_fragment;
  reg                 upStream_for_downStream_s2mPipe_rValid;
  reg                 upStream_for_downStream_s2mPipe_rData_A_last;
  reg        [23:0]   upStream_for_downStream_s2mPipe_rData_A_fragment;
  wire                when_Stream_l393;
  wire                when_Stream_l466;
  reg                 upStream_for_result_throwWhen_valid;
  wire                upStream_for_result_throwWhen_ready;
  wire                upStream_for_result_throwWhen_payload_A_last;
  wire       [23:0]   upStream_for_result_throwWhen_payload_A_fragment;
  wire                upStream_for_result_throwWhen_payload_B_last;
  wire       [23:0]   upStream_for_result_throwWhen_payload_B_fragment;
  wire       [1:0]    upStream_for_result_throwWhen_payload_Ctrl_Mode;
  wire                upStream_for_result_throwWhen_payload_Ctrl_Transpose;
  wire       [5:0]    upStream_for_result_throwWhen_payload_Ctrl_Shift;
  wire       [3:0]    upStream_for_result_throwWhen_payload_ID;
  wire                upStream_for_result_throwWhen_s2mPipe_valid;
  reg                 upStream_for_result_throwWhen_s2mPipe_ready;
  wire                upStream_for_result_throwWhen_s2mPipe_payload_A_last;
  wire       [23:0]   upStream_for_result_throwWhen_s2mPipe_payload_A_fragment;
  wire                upStream_for_result_throwWhen_s2mPipe_payload_B_last;
  wire       [23:0]   upStream_for_result_throwWhen_s2mPipe_payload_B_fragment;
  wire       [1:0]    upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode;
  wire                upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Transpose;
  wire       [5:0]    upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Shift;
  wire       [3:0]    upStream_for_result_throwWhen_s2mPipe_payload_ID;
  reg                 upStream_for_result_throwWhen_rValidN;
  reg                 upStream_for_result_throwWhen_rData_A_last;
  reg        [23:0]   upStream_for_result_throwWhen_rData_A_fragment;
  reg                 upStream_for_result_throwWhen_rData_B_last;
  reg        [23:0]   upStream_for_result_throwWhen_rData_B_fragment;
  reg        [1:0]    upStream_for_result_throwWhen_rData_Ctrl_Mode;
  reg                 upStream_for_result_throwWhen_rData_Ctrl_Transpose;
  reg        [5:0]    upStream_for_result_throwWhen_rData_Ctrl_Shift;
  reg        [3:0]    upStream_for_result_throwWhen_rData_ID;
  wire       [1:0]    _zz_upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode;
  wire                upStream_for_result_throwWhen_s2mPipe_m2sPipe_valid;
  wire                upStream_for_result_throwWhen_s2mPipe_m2sPipe_ready;
  wire                upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_A_last;
  wire       [23:0]   upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_A_fragment;
  wire                upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_B_last;
  wire       [23:0]   upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_B_fragment;
  wire       [1:0]    upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Mode;
  wire                upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Transpose;
  wire       [5:0]    upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Shift;
  wire       [3:0]    upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_ID;
  reg                 upStream_for_result_throwWhen_s2mPipe_rValid;
  reg                 upStream_for_result_throwWhen_s2mPipe_rData_A_last;
  reg        [23:0]   upStream_for_result_throwWhen_s2mPipe_rData_A_fragment;
  reg                 upStream_for_result_throwWhen_s2mPipe_rData_B_last;
  reg        [23:0]   upStream_for_result_throwWhen_s2mPipe_rData_B_fragment;
  reg        [1:0]    upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Mode;
  reg                 upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Transpose;
  reg        [5:0]    upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Shift;
  reg        [3:0]    upStream_for_result_throwWhen_s2mPipe_rData_ID;
  wire                when_Stream_l393_1;
  reg        [52:0]   reg_ProductSum;
  wire                when_SystolicArray2DUnit_l216;
  wire                CL_Add2Filter_terminateRequest_SystolicArray2DUnit_l222;
  wire                when_StageLink_l67;
  wire                when_CtrlLink_l157;
  wire                when_StageLink_l67_1;
  `ifndef SYNTHESIS
  reg [79:0] Filter_Node_PAYLOAD_Ctrl_Mode_string;
  reg [79:0] Shift_Node_PAYLOAD_Ctrl_Mode_string;
  reg [79:0] Add_Node_PAYLOAD_Ctrl_Mode_string;
  reg [79:0] Mul_Node_PAYLOAD_Ctrl_Mode_string;
  reg [79:0] io_upStream_payload_Ctrl_Mode_string;
  reg [79:0] io_result_payload_Ctrl_Mode_string;
  reg [79:0] upStream_for_result_payload_Ctrl_Mode_string;
  reg [79:0] upStream_for_result_throwWhen_payload_Ctrl_Mode_string;
  reg [79:0] upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode_string;
  reg [79:0] upStream_for_result_throwWhen_rData_Ctrl_Mode_string;
  reg [79:0] _zz_upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode_string;
  reg [79:0] upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Mode_string;
  reg [79:0] upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Mode_string;
  `endif


  assign _zz_Add_Node_PAYLOAD_sum_fragment = {{5{Add_Node_PAYLOAD_addend_2_fragment[47]}}, Add_Node_PAYLOAD_addend_2_fragment};
  SIntShifter Shift_Node_logic_instSIntShifter (
    .io_input       (Shift_Node_PAYLOAD_sum_fragment[52:0]           ), //i
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
    case(Mul_Node_PAYLOAD_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : Mul_Node_PAYLOAD_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : Mul_Node_PAYLOAD_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : Mul_Node_PAYLOAD_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : Mul_Node_PAYLOAD_Ctrl_Mode_string = "ElementMax";
      default : Mul_Node_PAYLOAD_Ctrl_Mode_string = "??????????";
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
    case(upStream_for_result_throwWhen_payload_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : upStream_for_result_throwWhen_payload_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : upStream_for_result_throwWhen_payload_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : upStream_for_result_throwWhen_payload_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : upStream_for_result_throwWhen_payload_Ctrl_Mode_string = "ElementMax";
      default : upStream_for_result_throwWhen_payload_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode_string = "ElementMax";
      default : upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(upStream_for_result_throwWhen_rData_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : upStream_for_result_throwWhen_rData_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : upStream_for_result_throwWhen_rData_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : upStream_for_result_throwWhen_rData_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : upStream_for_result_throwWhen_rData_Ctrl_Mode_string = "ElementMax";
      default : upStream_for_result_throwWhen_rData_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(_zz_upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : _zz_upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : _zz_upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : _zz_upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : _zz_upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode_string = "ElementMax";
      default : _zz_upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Mode_string = "ElementMax";
      default : upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Mode_string = "??????????";
    endcase
  end
  always @(*) begin
    case(upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Mode)
      MatrixOperation_TypeDef_MatMul : upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Mode_string = "MatMul    ";
      MatrixOperation_TypeDef_ElementAdd : upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Mode_string = "ElementAdd";
      MatrixOperation_TypeDef_ElementMul : upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Mode_string = "ElementMul";
      MatrixOperation_TypeDef_ElementMax : upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Mode_string = "ElementMax";
      default : upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Mode_string = "??????????";
    endcase
  end
  `endif

  always @(*) begin
    _zz_CL_Add2Filter_terminateRequest_SystolicArray2DUnit_l222 = 1'b0;
    if(Add_Node_isFiring) begin
      if(!when_SystolicArray2DUnit_l216) begin
        _zz_CL_Add2Filter_terminateRequest_SystolicArray2DUnit_l222 = 1'b1;
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
  assign io_downStream_valid = upStream_for_downStream_s2mPipe_m2sPipe_valid;
  assign upStream_for_downStream_s2mPipe_m2sPipe_ready = io_downStream_ready;
  assign io_downStream_payload_A_last = upStream_for_downStream_s2mPipe_m2sPipe_payload_A_last;
  assign io_downStream_payload_A_fragment = upStream_for_downStream_s2mPipe_m2sPipe_payload_A_fragment;
  assign when_Stream_l466 = (upStream_for_result_payload_Ctrl_Mode != MatrixOperation_TypeDef_MatMul);
  always @(*) begin
    upStream_for_result_throwWhen_valid = upStream_for_result_valid;
    if(when_Stream_l466) begin
      upStream_for_result_throwWhen_valid = 1'b0;
    end
  end

  always @(*) begin
    upStream_for_result_ready = upStream_for_result_throwWhen_ready;
    if(when_Stream_l466) begin
      upStream_for_result_ready = 1'b1;
    end
  end

  assign upStream_for_result_throwWhen_payload_A_last = upStream_for_result_payload_A_last;
  assign upStream_for_result_throwWhen_payload_A_fragment = upStream_for_result_payload_A_fragment;
  assign upStream_for_result_throwWhen_payload_B_last = upStream_for_result_payload_B_last;
  assign upStream_for_result_throwWhen_payload_B_fragment = upStream_for_result_payload_B_fragment;
  assign upStream_for_result_throwWhen_payload_Ctrl_Mode = upStream_for_result_payload_Ctrl_Mode;
  assign upStream_for_result_throwWhen_payload_Ctrl_Transpose = upStream_for_result_payload_Ctrl_Transpose;
  assign upStream_for_result_throwWhen_payload_Ctrl_Shift = upStream_for_result_payload_Ctrl_Shift;
  assign upStream_for_result_throwWhen_payload_ID = upStream_for_result_payload_ID;
  assign upStream_for_result_throwWhen_ready = upStream_for_result_throwWhen_rValidN;
  assign upStream_for_result_throwWhen_s2mPipe_valid = (upStream_for_result_throwWhen_valid || (! upStream_for_result_throwWhen_rValidN));
  assign _zz_upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode = (upStream_for_result_throwWhen_rValidN ? upStream_for_result_throwWhen_payload_Ctrl_Mode : upStream_for_result_throwWhen_rData_Ctrl_Mode);
  assign upStream_for_result_throwWhen_s2mPipe_payload_A_last = (upStream_for_result_throwWhen_rValidN ? upStream_for_result_throwWhen_payload_A_last : upStream_for_result_throwWhen_rData_A_last);
  assign upStream_for_result_throwWhen_s2mPipe_payload_A_fragment = (upStream_for_result_throwWhen_rValidN ? upStream_for_result_throwWhen_payload_A_fragment : upStream_for_result_throwWhen_rData_A_fragment);
  assign upStream_for_result_throwWhen_s2mPipe_payload_B_last = (upStream_for_result_throwWhen_rValidN ? upStream_for_result_throwWhen_payload_B_last : upStream_for_result_throwWhen_rData_B_last);
  assign upStream_for_result_throwWhen_s2mPipe_payload_B_fragment = (upStream_for_result_throwWhen_rValidN ? upStream_for_result_throwWhen_payload_B_fragment : upStream_for_result_throwWhen_rData_B_fragment);
  assign upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode = _zz_upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode;
  assign upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Transpose = (upStream_for_result_throwWhen_rValidN ? upStream_for_result_throwWhen_payload_Ctrl_Transpose : upStream_for_result_throwWhen_rData_Ctrl_Transpose);
  assign upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Shift = (upStream_for_result_throwWhen_rValidN ? upStream_for_result_throwWhen_payload_Ctrl_Shift : upStream_for_result_throwWhen_rData_Ctrl_Shift);
  assign upStream_for_result_throwWhen_s2mPipe_payload_ID = (upStream_for_result_throwWhen_rValidN ? upStream_for_result_throwWhen_payload_ID : upStream_for_result_throwWhen_rData_ID);
  always @(*) begin
    upStream_for_result_throwWhen_s2mPipe_ready = upStream_for_result_throwWhen_s2mPipe_m2sPipe_ready;
    if(when_Stream_l393_1) begin
      upStream_for_result_throwWhen_s2mPipe_ready = 1'b1;
    end
  end

  assign when_Stream_l393_1 = (! upStream_for_result_throwWhen_s2mPipe_m2sPipe_valid);
  assign upStream_for_result_throwWhen_s2mPipe_m2sPipe_valid = upStream_for_result_throwWhen_s2mPipe_rValid;
  assign upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_A_last = upStream_for_result_throwWhen_s2mPipe_rData_A_last;
  assign upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_A_fragment = upStream_for_result_throwWhen_s2mPipe_rData_A_fragment;
  assign upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_B_last = upStream_for_result_throwWhen_s2mPipe_rData_B_last;
  assign upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_B_fragment = upStream_for_result_throwWhen_s2mPipe_rData_B_fragment;
  assign upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Mode = upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Mode;
  assign upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Transpose = upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Transpose;
  assign upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Shift = upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Shift;
  assign upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_ID = upStream_for_result_throwWhen_s2mPipe_rData_ID;
  assign Mul_Node_valid = upStream_for_result_throwWhen_s2mPipe_m2sPipe_valid;
  assign upStream_for_result_throwWhen_s2mPipe_m2sPipe_ready = (Mul_Node_isReady || Mul_Node_isCancel);
  assign Mul_Node_PAYLOAD_A_last = upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_A_last;
  assign Mul_Node_PAYLOAD_A_fragment = upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_A_fragment;
  assign Mul_Node_PAYLOAD_B_last = upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_B_last;
  assign Mul_Node_PAYLOAD_B_fragment = upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_B_fragment;
  assign Mul_Node_PAYLOAD_Ctrl_Mode = upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Mode;
  assign Mul_Node_PAYLOAD_Ctrl_Transpose = upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Transpose;
  assign Mul_Node_PAYLOAD_Ctrl_Shift = upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_Ctrl_Shift;
  assign Mul_Node_PAYLOAD_ID = upStream_for_result_throwWhen_s2mPipe_m2sPipe_payload_ID;
  assign Mul_Node_PAYLOAD_product_fragment = ($signed(Mul_Node_PAYLOAD_factor_1_fragment) * $signed(Mul_Node_PAYLOAD_factor_2_fragment));
  assign Mul_Node_PAYLOAD_product_last = (Mul_Node_PAYLOAD_factor_1_last && Mul_Node_PAYLOAD_factor_2_last);
  assign Mul_Node_PAYLOAD_factor_1_last = Mul_Node_PAYLOAD_A_last;
  assign Mul_Node_PAYLOAD_factor_1_fragment = Mul_Node_PAYLOAD_A_fragment;
  assign Mul_Node_PAYLOAD_factor_2_last = Mul_Node_PAYLOAD_B_last;
  assign Mul_Node_PAYLOAD_factor_2_fragment = Mul_Node_PAYLOAD_B_fragment;
  assign Add_Node_PAYLOAD_sum_fragment = ($signed(Add_Node_PAYLOAD_addend_1_fragment) + $signed(_zz_Add_Node_PAYLOAD_sum_fragment));
  assign Add_Node_PAYLOAD_sum_last = Add_Node_PAYLOAD_product_last;
  assign Add_Node_PAYLOAD_addend_1_fragment = reg_ProductSum;
  assign Add_Node_PAYLOAD_addend_2_fragment = Add_Node_PAYLOAD_product_fragment;
  assign when_SystolicArray2DUnit_l216 = (Add_Node_PAYLOAD_sum_last == 1'b1);
  assign CL_Add2Filter_terminateRequest_SystolicArray2DUnit_l222 = _zz_CL_Add2Filter_terminateRequest_SystolicArray2DUnit_l222;
  assign Shift_Node_PAYLOAD_result = Shift_Node_logic_instSIntShifter_io_output;
  assign io_result_valid = Shift_Node_isValid;
  assign Shift_Node_ready = io_result_ready;
  assign io_result_payload_Z_fragment = Shift_Node_PAYLOAD_result;
  assign io_result_payload_Ctrl_Mode = Shift_Node_PAYLOAD_Ctrl_Mode;
  assign io_result_payload_Ctrl_Transpose = Shift_Node_PAYLOAD_Ctrl_Transpose;
  assign io_result_payload_ID = Shift_Node_PAYLOAD_ID;
  always @(*) begin
    Mul_Node_ready = Add_Node_ready;
    if(when_StageLink_l67) begin
      Mul_Node_ready = 1'b1;
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
  assign when_CtrlLink_l157 = (|CL_Add2Filter_terminateRequest_SystolicArray2DUnit_l222);
  assign Filter_Node_PAYLOAD_Ctrl_Mode = Add_Node_PAYLOAD_Ctrl_Mode;
  assign Filter_Node_PAYLOAD_Ctrl_Transpose = Add_Node_PAYLOAD_Ctrl_Transpose;
  assign Filter_Node_PAYLOAD_Ctrl_Shift = Add_Node_PAYLOAD_Ctrl_Shift;
  assign Filter_Node_PAYLOAD_ID = Add_Node_PAYLOAD_ID;
  assign Filter_Node_PAYLOAD_sum_fragment = Add_Node_PAYLOAD_sum_fragment;
  always @(*) begin
    Filter_Node_ready = Shift_Node_ready;
    if(when_StageLink_l67_1) begin
      Filter_Node_ready = 1'b1;
    end
  end

  assign when_StageLink_l67_1 = (! Shift_Node_isValid);
  assign Mul_Node_isValid = Mul_Node_valid;
  assign Mul_Node_isReady = Mul_Node_ready;
  assign Mul_Node_isCancel = 1'b0;
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
      upStream_for_result_throwWhen_rValidN <= 1'b1;
      upStream_for_result_throwWhen_s2mPipe_rValid <= 1'b0;
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
      if(upStream_for_result_throwWhen_valid) begin
        upStream_for_result_throwWhen_rValidN <= 1'b0;
      end
      if(upStream_for_result_throwWhen_s2mPipe_ready) begin
        upStream_for_result_throwWhen_rValidN <= 1'b1;
      end
      if(upStream_for_result_throwWhen_s2mPipe_ready) begin
        upStream_for_result_throwWhen_s2mPipe_rValid <= upStream_for_result_throwWhen_s2mPipe_valid;
      end
      if(Add_Node_isFiring) begin
        if(when_SystolicArray2DUnit_l216) begin
          reg_ProductSum <= 53'h0;
        end else begin
          reg_ProductSum <= Add_Node_PAYLOAD_sum_fragment;
        end
      end
      if(Mul_Node_isReady) begin
        Add_Node_valid <= Mul_Node_isValid;
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
    end
    if(upStream_for_downStream_s2mPipe_ready) begin
      upStream_for_downStream_s2mPipe_rData_A_last <= upStream_for_downStream_s2mPipe_payload_A_last;
      upStream_for_downStream_s2mPipe_rData_A_fragment <= upStream_for_downStream_s2mPipe_payload_A_fragment;
    end
    if(upStream_for_result_throwWhen_ready) begin
      upStream_for_result_throwWhen_rData_A_last <= upStream_for_result_throwWhen_payload_A_last;
      upStream_for_result_throwWhen_rData_A_fragment <= upStream_for_result_throwWhen_payload_A_fragment;
      upStream_for_result_throwWhen_rData_B_last <= upStream_for_result_throwWhen_payload_B_last;
      upStream_for_result_throwWhen_rData_B_fragment <= upStream_for_result_throwWhen_payload_B_fragment;
      upStream_for_result_throwWhen_rData_Ctrl_Mode <= upStream_for_result_throwWhen_payload_Ctrl_Mode;
      upStream_for_result_throwWhen_rData_Ctrl_Transpose <= upStream_for_result_throwWhen_payload_Ctrl_Transpose;
      upStream_for_result_throwWhen_rData_Ctrl_Shift <= upStream_for_result_throwWhen_payload_Ctrl_Shift;
      upStream_for_result_throwWhen_rData_ID <= upStream_for_result_throwWhen_payload_ID;
    end
    if(upStream_for_result_throwWhen_s2mPipe_ready) begin
      upStream_for_result_throwWhen_s2mPipe_rData_A_last <= upStream_for_result_throwWhen_s2mPipe_payload_A_last;
      upStream_for_result_throwWhen_s2mPipe_rData_A_fragment <= upStream_for_result_throwWhen_s2mPipe_payload_A_fragment;
      upStream_for_result_throwWhen_s2mPipe_rData_B_last <= upStream_for_result_throwWhen_s2mPipe_payload_B_last;
      upStream_for_result_throwWhen_s2mPipe_rData_B_fragment <= upStream_for_result_throwWhen_s2mPipe_payload_B_fragment;
      upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Mode <= upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Mode;
      upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Transpose <= upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Transpose;
      upStream_for_result_throwWhen_s2mPipe_rData_Ctrl_Shift <= upStream_for_result_throwWhen_s2mPipe_payload_Ctrl_Shift;
      upStream_for_result_throwWhen_s2mPipe_rData_ID <= upStream_for_result_throwWhen_s2mPipe_payload_ID;
    end
    Add_Node_PAYLOAD_Ctrl_Mode <= Add_Node_PAYLOAD_Ctrl_Mode;
    Add_Node_PAYLOAD_Ctrl_Transpose <= Add_Node_PAYLOAD_Ctrl_Transpose;
    Add_Node_PAYLOAD_Ctrl_Shift <= Add_Node_PAYLOAD_Ctrl_Shift;
    if(Mul_Node_isReady) begin
      Add_Node_PAYLOAD_Ctrl_Mode <= Mul_Node_PAYLOAD_Ctrl_Mode;
      Add_Node_PAYLOAD_Ctrl_Transpose <= Mul_Node_PAYLOAD_Ctrl_Transpose;
      Add_Node_PAYLOAD_Ctrl_Shift <= Mul_Node_PAYLOAD_Ctrl_Shift;
      Add_Node_PAYLOAD_ID <= Mul_Node_PAYLOAD_ID;
      Add_Node_PAYLOAD_product_last <= Mul_Node_PAYLOAD_product_last;
      Add_Node_PAYLOAD_product_fragment <= Mul_Node_PAYLOAD_product_fragment;
    end
    if(Filter_Node_isReady) begin
      Shift_Node_PAYLOAD_Ctrl_Mode <= Filter_Node_PAYLOAD_Ctrl_Mode;
      Shift_Node_PAYLOAD_Ctrl_Transpose <= Filter_Node_PAYLOAD_Ctrl_Transpose;
      Shift_Node_PAYLOAD_Ctrl_Shift <= Filter_Node_PAYLOAD_Ctrl_Shift;
      Shift_Node_PAYLOAD_ID <= Filter_Node_PAYLOAD_ID;
      Shift_Node_PAYLOAD_sum_fragment <= Filter_Node_PAYLOAD_sum_fragment;
    end
  end


endmodule

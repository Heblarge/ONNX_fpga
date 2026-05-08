// Generator : SpinalHDL v1.11.0    git head : 63852c61e498798f4e293594ce53fcb02c45eb6b
// Component : SIntShifter
// Git hash  : e7984c6fd05794f7617e004bdecebf29e04e3020

`timescale 1ns/1ps 
module SIntShifter (
  input  wire [52:0]   io_input,
  input  wire [5:0]    io_shiftAmount,
  output wire [23:0]   io_output
);

  wire       [5:0]    _zz_effShift;
  wire       [139:0]  _zz_rawResult;
  wire       [76:0]   _zz_rawResult_1;
  wire       [139:0]  _zz_rawResult_2;
  wire       [5:0]    _zz_rawResult_3;
  wire       [139:0]  _zz_satResult;
  wire       [139:0]  _zz_satResult_1;
  wire       [139:0]  _zz_satResult_2;
  wire       [139:0]  _zz_satResult_3;
  wire       [139:0]  _zz_satResult_4;
  wire       [5:0]    effShift;
  wire       [76:0]   extInput;
  wire       [139:0]  rawResult;
  wire       [76:0]   maxS;
  wire       [76:0]   minS;
  wire       [139:0]  satResult;

  assign _zz_effShift = (($signed(io_shiftAmount) < $signed(6'h28)) ? 6'h28 : io_shiftAmount);
  assign _zz_rawResult_1 = ($signed(extInput) >>> effShift);
  assign _zz_rawResult = {{63{_zz_rawResult_1[76]}}, _zz_rawResult_1};
  assign _zz_rawResult_2 = ({{63{extInput[76]}},extInput} <<< _zz_rawResult_3);
  assign _zz_rawResult_3 = (- effShift);
  assign _zz_satResult = {{63{maxS[76]}}, maxS};
  assign _zz_satResult_1 = {{63{maxS[76]}}, maxS};
  assign _zz_satResult_2 = (($signed(rawResult) < $signed(_zz_satResult_3)) ? _zz_satResult_4 : rawResult);
  assign _zz_satResult_3 = {{63{minS[76]}}, minS};
  assign _zz_satResult_4 = {{63{minS[76]}}, minS};
  assign effShift = (($signed(6'h18) < $signed(io_shiftAmount)) ? 6'h18 : _zz_effShift);
  assign extInput = {{24{io_input[52]}}, io_input};
  assign rawResult = (($signed(6'h0) < $signed(effShift)) ? _zz_rawResult : _zz_rawResult_2);
  assign maxS = 77'h000000000000007fffff;
  assign minS = 77'h1fffffffffffff800000;
  assign satResult = (($signed(_zz_satResult) < $signed(rawResult)) ? _zz_satResult_1 : _zz_satResult_2);
  assign io_output = satResult[23:0];

endmodule

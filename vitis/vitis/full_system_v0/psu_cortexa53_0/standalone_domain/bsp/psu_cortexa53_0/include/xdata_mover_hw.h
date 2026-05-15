// ==============================================================
// Vitis HLS - High-Level Synthesis from C, C++ and OpenCL v2024.2 (64-bit)
// Tool Version Limit: 2024.11
// Copyright 1986-2022 Xilinx, Inc. All Rights Reserved.
// Copyright 2022-2024 Advanced Micro Devices, Inc. All Rights Reserved.
// 
// ==============================================================
// control_r
// 0x00 : reserved
// 0x04 : reserved
// 0x08 : reserved
// 0x0c : reserved
// 0x10 : Data signal of mem_src
//        bit 31~0 - mem_src[31:0] (Read/Write)
// 0x14 : Data signal of mem_src
//        bit 31~0 - mem_src[63:32] (Read/Write)
// 0x18 : reserved
// 0x1c : Data signal of mem_dst
//        bit 31~0 - mem_dst[31:0] (Read/Write)
// 0x20 : Data signal of mem_dst
//        bit 31~0 - mem_dst[63:32] (Read/Write)
// 0x24 : reserved
// (SC = Self Clear, COR = Clear on Read, TOW = Toggle on Write, COH = Clear on Handshake)

#define XDATA_MOVER_CONTROL_R_ADDR_MEM_SRC_DATA 0x10
#define XDATA_MOVER_CONTROL_R_BITS_MEM_SRC_DATA 64
#define XDATA_MOVER_CONTROL_R_ADDR_MEM_DST_DATA 0x1c
#define XDATA_MOVER_CONTROL_R_BITS_MEM_DST_DATA 64

// control
// 0x00 : Control signals
//        bit 0  - ap_start (Read/Write/COH)
//        bit 1  - ap_done (Read/COR)
//        bit 2  - ap_idle (Read)
//        bit 3  - ap_ready (Read/COR)
//        bit 7  - auto_restart (Read/Write)
//        bit 9  - interrupt (Read)
//        others - reserved
// 0x04 : Global Interrupt Enable Register
//        bit 0  - Global Interrupt Enable (Read/Write)
//        others - reserved
// 0x08 : IP Interrupt Enable Register (Read/Write)
//        bit 0 - enable ap_done interrupt (Read/Write)
//        bit 1 - enable ap_ready interrupt (Read/Write)
//        others - reserved
// 0x0c : IP Interrupt Status Register (Read/TOW)
//        bit 0 - ap_done (Read/TOW)
//        bit 1 - ap_ready (Read/TOW)
//        others - reserved
// 0x10 : Data signal of rows
//        bit 31~0 - rows[31:0] (Read/Write)
// 0x14 : reserved
// 0x18 : Data signal of row_len_bytes
//        bit 31~0 - row_len_bytes[31:0] (Read/Write)
// 0x1c : reserved
// 0x20 : Data signal of src_stride_bytes
//        bit 31~0 - src_stride_bytes[31:0] (Read/Write)
// 0x24 : reserved
// 0x28 : Data signal of dst_stride_bytes
//        bit 31~0 - dst_stride_bytes[31:0] (Read/Write)
// 0x2c : reserved
// 0x30 : Data signal of src_offset_bytes
//        bit 31~0 - src_offset_bytes[31:0] (Read/Write)
// 0x34 : reserved
// 0x38 : Data signal of dst_offset_bytes
//        bit 31~0 - dst_offset_bytes[31:0] (Read/Write)
// 0x3c : reserved
// (SC = Self Clear, COR = Clear on Read, TOW = Toggle on Write, COH = Clear on Handshake)

#define XDATA_MOVER_CONTROL_ADDR_AP_CTRL               0x00
#define XDATA_MOVER_CONTROL_ADDR_GIE                   0x04
#define XDATA_MOVER_CONTROL_ADDR_IER                   0x08
#define XDATA_MOVER_CONTROL_ADDR_ISR                   0x0c
#define XDATA_MOVER_CONTROL_ADDR_ROWS_DATA             0x10
#define XDATA_MOVER_CONTROL_BITS_ROWS_DATA             32
#define XDATA_MOVER_CONTROL_ADDR_ROW_LEN_BYTES_DATA    0x18
#define XDATA_MOVER_CONTROL_BITS_ROW_LEN_BYTES_DATA    32
#define XDATA_MOVER_CONTROL_ADDR_SRC_STRIDE_BYTES_DATA 0x20
#define XDATA_MOVER_CONTROL_BITS_SRC_STRIDE_BYTES_DATA 32
#define XDATA_MOVER_CONTROL_ADDR_DST_STRIDE_BYTES_DATA 0x28
#define XDATA_MOVER_CONTROL_BITS_DST_STRIDE_BYTES_DATA 32
#define XDATA_MOVER_CONTROL_ADDR_SRC_OFFSET_BYTES_DATA 0x30
#define XDATA_MOVER_CONTROL_BITS_SRC_OFFSET_BYTES_DATA 32
#define XDATA_MOVER_CONTROL_ADDR_DST_OFFSET_BYTES_DATA 0x38
#define XDATA_MOVER_CONTROL_BITS_DST_OFFSET_BYTES_DATA 32


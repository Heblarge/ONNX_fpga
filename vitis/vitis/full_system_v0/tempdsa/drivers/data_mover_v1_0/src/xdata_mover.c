// ==============================================================
// Vitis HLS - High-Level Synthesis from C, C++ and OpenCL v2024.2 (64-bit)
// Tool Version Limit: 2024.11
// Copyright 1986-2022 Xilinx, Inc. All Rights Reserved.
// Copyright 2022-2024 Advanced Micro Devices, Inc. All Rights Reserved.
// 
// ==============================================================
/***************************** Include Files *********************************/
#include "xdata_mover.h"

/************************** Function Implementation *************************/
#ifndef __linux__
int XData_mover_CfgInitialize(XData_mover *InstancePtr, XData_mover_Config *ConfigPtr) {
    Xil_AssertNonvoid(InstancePtr != NULL);
    Xil_AssertNonvoid(ConfigPtr != NULL);

    InstancePtr->Control_r_BaseAddress = ConfigPtr->Control_r_BaseAddress;
    InstancePtr->Control_BaseAddress = ConfigPtr->Control_BaseAddress;
    InstancePtr->IsReady = XIL_COMPONENT_IS_READY;

    return XST_SUCCESS;
}
#endif

void XData_mover_Start(XData_mover *InstancePtr) {
    u32 Data;

    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    Data = XData_mover_ReadReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_AP_CTRL) & 0x80;
    XData_mover_WriteReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_AP_CTRL, Data | 0x01);
}

u32 XData_mover_IsDone(XData_mover *InstancePtr) {
    u32 Data;

    Xil_AssertNonvoid(InstancePtr != NULL);
    Xil_AssertNonvoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    Data = XData_mover_ReadReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_AP_CTRL);
    return (Data >> 1) & 0x1;
}

u32 XData_mover_IsIdle(XData_mover *InstancePtr) {
    u32 Data;

    Xil_AssertNonvoid(InstancePtr != NULL);
    Xil_AssertNonvoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    Data = XData_mover_ReadReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_AP_CTRL);
    return (Data >> 2) & 0x1;
}

u32 XData_mover_IsReady(XData_mover *InstancePtr) {
    u32 Data;

    Xil_AssertNonvoid(InstancePtr != NULL);
    Xil_AssertNonvoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    Data = XData_mover_ReadReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_AP_CTRL);
    // check ap_start to see if the pcore is ready for next input
    return !(Data & 0x1);
}

void XData_mover_EnableAutoRestart(XData_mover *InstancePtr) {
    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    XData_mover_WriteReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_AP_CTRL, 0x80);
}

void XData_mover_DisableAutoRestart(XData_mover *InstancePtr) {
    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    XData_mover_WriteReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_AP_CTRL, 0);
}

void XData_mover_Set_mem_src(XData_mover *InstancePtr, u64 Data) {
    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    XData_mover_WriteReg(InstancePtr->Control_r_BaseAddress, XDATA_MOVER_CONTROL_R_ADDR_MEM_SRC_DATA, (u32)(Data));
    XData_mover_WriteReg(InstancePtr->Control_r_BaseAddress, XDATA_MOVER_CONTROL_R_ADDR_MEM_SRC_DATA + 4, (u32)(Data >> 32));
}

u64 XData_mover_Get_mem_src(XData_mover *InstancePtr) {
    u64 Data;

    Xil_AssertNonvoid(InstancePtr != NULL);
    Xil_AssertNonvoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    Data = XData_mover_ReadReg(InstancePtr->Control_r_BaseAddress, XDATA_MOVER_CONTROL_R_ADDR_MEM_SRC_DATA);
    Data += (u64)XData_mover_ReadReg(InstancePtr->Control_r_BaseAddress, XDATA_MOVER_CONTROL_R_ADDR_MEM_SRC_DATA + 4) << 32;
    return Data;
}

void XData_mover_Set_mem_dst(XData_mover *InstancePtr, u64 Data) {
    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    XData_mover_WriteReg(InstancePtr->Control_r_BaseAddress, XDATA_MOVER_CONTROL_R_ADDR_MEM_DST_DATA, (u32)(Data));
    XData_mover_WriteReg(InstancePtr->Control_r_BaseAddress, XDATA_MOVER_CONTROL_R_ADDR_MEM_DST_DATA + 4, (u32)(Data >> 32));
}

u64 XData_mover_Get_mem_dst(XData_mover *InstancePtr) {
    u64 Data;

    Xil_AssertNonvoid(InstancePtr != NULL);
    Xil_AssertNonvoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    Data = XData_mover_ReadReg(InstancePtr->Control_r_BaseAddress, XDATA_MOVER_CONTROL_R_ADDR_MEM_DST_DATA);
    Data += (u64)XData_mover_ReadReg(InstancePtr->Control_r_BaseAddress, XDATA_MOVER_CONTROL_R_ADDR_MEM_DST_DATA + 4) << 32;
    return Data;
}

void XData_mover_Set_rows(XData_mover *InstancePtr, u32 Data) {
    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    XData_mover_WriteReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_ROWS_DATA, Data);
}

u32 XData_mover_Get_rows(XData_mover *InstancePtr) {
    u32 Data;

    Xil_AssertNonvoid(InstancePtr != NULL);
    Xil_AssertNonvoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    Data = XData_mover_ReadReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_ROWS_DATA);
    return Data;
}

void XData_mover_Set_row_len_bytes(XData_mover *InstancePtr, u32 Data) {
    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    XData_mover_WriteReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_ROW_LEN_BYTES_DATA, Data);
}

u32 XData_mover_Get_row_len_bytes(XData_mover *InstancePtr) {
    u32 Data;

    Xil_AssertNonvoid(InstancePtr != NULL);
    Xil_AssertNonvoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    Data = XData_mover_ReadReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_ROW_LEN_BYTES_DATA);
    return Data;
}

void XData_mover_Set_src_stride_bytes(XData_mover *InstancePtr, u32 Data) {
    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    XData_mover_WriteReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_SRC_STRIDE_BYTES_DATA, Data);
}

u32 XData_mover_Get_src_stride_bytes(XData_mover *InstancePtr) {
    u32 Data;

    Xil_AssertNonvoid(InstancePtr != NULL);
    Xil_AssertNonvoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    Data = XData_mover_ReadReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_SRC_STRIDE_BYTES_DATA);
    return Data;
}

void XData_mover_Set_dst_stride_bytes(XData_mover *InstancePtr, u32 Data) {
    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    XData_mover_WriteReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_DST_STRIDE_BYTES_DATA, Data);
}

u32 XData_mover_Get_dst_stride_bytes(XData_mover *InstancePtr) {
    u32 Data;

    Xil_AssertNonvoid(InstancePtr != NULL);
    Xil_AssertNonvoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    Data = XData_mover_ReadReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_DST_STRIDE_BYTES_DATA);
    return Data;
}

void XData_mover_Set_src_offset_bytes(XData_mover *InstancePtr, u32 Data) {
    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    XData_mover_WriteReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_SRC_OFFSET_BYTES_DATA, Data);
}

u32 XData_mover_Get_src_offset_bytes(XData_mover *InstancePtr) {
    u32 Data;

    Xil_AssertNonvoid(InstancePtr != NULL);
    Xil_AssertNonvoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    Data = XData_mover_ReadReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_SRC_OFFSET_BYTES_DATA);
    return Data;
}

void XData_mover_Set_dst_offset_bytes(XData_mover *InstancePtr, u32 Data) {
    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    XData_mover_WriteReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_DST_OFFSET_BYTES_DATA, Data);
}

u32 XData_mover_Get_dst_offset_bytes(XData_mover *InstancePtr) {
    u32 Data;

    Xil_AssertNonvoid(InstancePtr != NULL);
    Xil_AssertNonvoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    Data = XData_mover_ReadReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_DST_OFFSET_BYTES_DATA);
    return Data;
}

void XData_mover_InterruptGlobalEnable(XData_mover *InstancePtr) {
    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    XData_mover_WriteReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_GIE, 1);
}

void XData_mover_InterruptGlobalDisable(XData_mover *InstancePtr) {
    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    XData_mover_WriteReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_GIE, 0);
}

void XData_mover_InterruptEnable(XData_mover *InstancePtr, u32 Mask) {
    u32 Register;

    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    Register =  XData_mover_ReadReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_IER);
    XData_mover_WriteReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_IER, Register | Mask);
}

void XData_mover_InterruptDisable(XData_mover *InstancePtr, u32 Mask) {
    u32 Register;

    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    Register =  XData_mover_ReadReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_IER);
    XData_mover_WriteReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_IER, Register & (~Mask));
}

void XData_mover_InterruptClear(XData_mover *InstancePtr, u32 Mask) {
    Xil_AssertVoid(InstancePtr != NULL);
    Xil_AssertVoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    XData_mover_WriteReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_ISR, Mask);
}

u32 XData_mover_InterruptGetEnabled(XData_mover *InstancePtr) {
    Xil_AssertNonvoid(InstancePtr != NULL);
    Xil_AssertNonvoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    return XData_mover_ReadReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_IER);
}

u32 XData_mover_InterruptGetStatus(XData_mover *InstancePtr) {
    Xil_AssertNonvoid(InstancePtr != NULL);
    Xil_AssertNonvoid(InstancePtr->IsReady == XIL_COMPONENT_IS_READY);

    return XData_mover_ReadReg(InstancePtr->Control_BaseAddress, XDATA_MOVER_CONTROL_ADDR_ISR);
}


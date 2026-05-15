// ==============================================================
// Vitis HLS - High-Level Synthesis from C, C++ and OpenCL v2024.2 (64-bit)
// Tool Version Limit: 2024.11
// Copyright 1986-2022 Xilinx, Inc. All Rights Reserved.
// Copyright 2022-2024 Advanced Micro Devices, Inc. All Rights Reserved.
// 
// ==============================================================
#ifndef __linux__

#include "xstatus.h"
#ifdef SDT
#include "xparameters.h"
#endif
#include "xdata_mover.h"

extern XData_mover_Config XData_mover_ConfigTable[];

#ifdef SDT
XData_mover_Config *XData_mover_LookupConfig(UINTPTR BaseAddress) {
	XData_mover_Config *ConfigPtr = NULL;

	int Index;

	for (Index = (u32)0x0; XData_mover_ConfigTable[Index].Name != NULL; Index++) {
		if (!BaseAddress || XData_mover_ConfigTable[Index].Control_r_BaseAddress == BaseAddress) {
			ConfigPtr = &XData_mover_ConfigTable[Index];
			break;
		}
	}

	return ConfigPtr;
}

int XData_mover_Initialize(XData_mover *InstancePtr, UINTPTR BaseAddress) {
	XData_mover_Config *ConfigPtr;

	Xil_AssertNonvoid(InstancePtr != NULL);

	ConfigPtr = XData_mover_LookupConfig(BaseAddress);
	if (ConfigPtr == NULL) {
		InstancePtr->IsReady = 0;
		return (XST_DEVICE_NOT_FOUND);
	}

	return XData_mover_CfgInitialize(InstancePtr, ConfigPtr);
}
#else
XData_mover_Config *XData_mover_LookupConfig(u16 DeviceId) {
	XData_mover_Config *ConfigPtr = NULL;

	int Index;

	for (Index = 0; Index < XPAR_XDATA_MOVER_NUM_INSTANCES; Index++) {
		if (XData_mover_ConfigTable[Index].DeviceId == DeviceId) {
			ConfigPtr = &XData_mover_ConfigTable[Index];
			break;
		}
	}

	return ConfigPtr;
}

int XData_mover_Initialize(XData_mover *InstancePtr, u16 DeviceId) {
	XData_mover_Config *ConfigPtr;

	Xil_AssertNonvoid(InstancePtr != NULL);

	ConfigPtr = XData_mover_LookupConfig(DeviceId);
	if (ConfigPtr == NULL) {
		InstancePtr->IsReady = 0;
		return (XST_DEVICE_NOT_FOUND);
	}

	return XData_mover_CfgInitialize(InstancePtr, ConfigPtr);
}
#endif

#endif


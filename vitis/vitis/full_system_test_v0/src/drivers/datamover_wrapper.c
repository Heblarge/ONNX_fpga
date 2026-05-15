#include "datamover_wrapper.h"
#include "xil_printf.h"

// 定义全局变量
XData_mover DataMover0, DataMover1, DataMover2;
volatile int TransferDone0 = 0;
volatile int TransferDone1 = 0;
volatile int TransferDone2 = 0;

int InitDataMover(XData_mover *InstancePtr, u16 DeviceId, const char *name) {
    int Status;
    XData_mover_Config *Config;
    xil_printf("[DM] Initializing %s...\r\n", name);
    Config = XData_mover_LookupConfig(DeviceId);
    if (NULL == Config) {
        xil_printf("[DM] Lookup config failed for %s\r\n", name);
        return XST_FAILURE;
    }

    Status = XData_mover_CfgInitialize(InstancePtr, Config);
    if (Status != XST_SUCCESS) {
        xil_printf("[DM] CfgInitialize failed for %s\r\n", name);
        return XST_FAILURE;
    }

    XData_mover_DisableAutoRestart(InstancePtr);
    XData_mover_InterruptGlobalEnable(InstancePtr);
    XData_mover_InterruptEnable(InstancePtr, 0x3);
    xil_printf("[DM] %s initialized successfully\r\n", name);
    return XST_SUCCESS;
}

int TransferData(XData_mover *InstancePtr, volatile int *done_flag,
                 u64 src_addr, u64 dst_addr, u32 rows, u32 row_len, const char *name) {
    u32 timeout = TIMEOUT_COUNT;
    xil_printf("[XFER] %s: 0x%llx -> 0x%llx (%dx%d bytes)\r\n", name, src_addr, dst_addr, rows, row_len);

    while (!XData_mover_IsReady(InstancePtr) && timeout--) {
        if (timeout == 0) {
            xil_printf("[XFER] ERROR: %s not ready\r\n", name);
            return XST_FAILURE;
        }
    }
    XData_mover_Set_mem_src(InstancePtr, src_addr);
    XData_mover_Set_mem_dst(InstancePtr, dst_addr);
    XData_mover_Set_rows(InstancePtr, rows);
    XData_mover_Set_row_len_bytes(InstancePtr, row_len);
    XData_mover_Set_src_stride_bytes(InstancePtr, row_len);
    XData_mover_Set_dst_stride_bytes(InstancePtr, row_len);
    XData_mover_Set_src_offset_bytes(InstancePtr, 0);
    XData_mover_Set_dst_offset_bytes(InstancePtr, 0);

    *done_flag = 0;
    XData_mover_Start(InstancePtr);
    xil_printf("[XFER] %s started\r\n", name);
    return XST_SUCCESS;
}

int WaitForTransfer(volatile int *done_flag, const char *name) {
    u32 timeout = TIMEOUT_COUNT;
    xil_printf("[WAIT] Waiting for %s to complete...\r\n", name);
    while (!(*done_flag) && timeout--) {
        if (timeout % 10000000 == 0) xil_printf("[WAIT] %s still in progress...\r\n", name);
    }
    if (!(*done_flag)) {
        xil_printf("[WAIT] ERROR: %s timeout\r\n", name);
        return XST_FAILURE;
    }
    xil_printf("[WAIT] %s completed\r\n", name);
    return XST_SUCCESS;
}

void DataMover0IntrHandler(void *CallbackRef) {
    XData_mover *InstancePtr = (XData_mover *)CallbackRef;
    u32 IrqStatus = XData_mover_InterruptGetStatus(InstancePtr);
    // xil_printf("[ISR0] DataMover0 interrupt: 0x%08x\r\n", IrqStatus);
    XData_mover_InterruptClear(InstancePtr, IrqStatus);
    if (IrqStatus & 0x1) TransferDone0 = 1;
}

void DataMover1IntrHandler(void *CallbackRef) {
    XData_mover *InstancePtr = (XData_mover *)CallbackRef;
    u32 IrqStatus = XData_mover_InterruptGetStatus(InstancePtr);
    // xil_printf("[ISR1] DataMover1 interrupt: 0x%08x\r\n", IrqStatus);
    XData_mover_InterruptClear(InstancePtr, IrqStatus);
    if (IrqStatus & 0x1) TransferDone1 = 1;
}

void DataMover2IntrHandler(void *CallbackRef) {
    XData_mover *InstancePtr = (XData_mover *)CallbackRef;
    u32 IrqStatus = XData_mover_InterruptGetStatus(InstancePtr);
    // xil_printf("[ISR2] DataMover2 interrupt: 0x%08x\r\n", IrqStatus);
    XData_mover_InterruptClear(InstancePtr, IrqStatus);
    if (IrqStatus & 0x1) TransferDone2 = 1;
}

#include "sys_intr.h"
#include "../config.h"
#include "datamover_wrapper.h" // 需要访问 DataMover 实例和 Handler
#include "xil_printf.h"

XScuGic Intc; // 定义全局 GIC 实例

int SetupIntrSystem(void) {
    int Status;
    XScuGic_Config *IntcConfig;

    xil_printf("[GIC] Initializing interrupt controller...\r\n");
    IntcConfig = XScuGic_LookupConfig(INTC_DEV_ID);
    if (NULL == IntcConfig) {
        xil_printf("[GIC] Lookup config failed\r\n");
        return XST_FAILURE;
    }

    Status = XScuGic_CfgInitialize(&Intc, IntcConfig, IntcConfig->CpuBaseAddress);
    if (Status != XST_SUCCESS) {
        xil_printf("[GIC] CfgInitialize failed\r\n");
        return XST_FAILURE;
    }

    Xil_ExceptionInit();
    Xil_ExceptionRegisterHandler(XIL_EXCEPTION_ID_INT,
                                 (Xil_ExceptionHandler)XScuGic_InterruptHandler,
                                 &Intc);

    // 连接 DataMover0
    Status = XScuGic_Connect(&Intc, DATA_MOVER_0_INTR_ID,
                            (Xil_InterruptHandler)DataMover0IntrHandler, &DataMover0);
    if (Status != XST_SUCCESS) return XST_FAILURE;

    // 连接 DataMover1
    Status = XScuGic_Connect(&Intc, DATA_MOVER_1_INTR_ID,
                            (Xil_InterruptHandler)DataMover1IntrHandler, &DataMover1);
    if (Status != XST_SUCCESS) return XST_FAILURE;

    // 连接 DataMover2
    Status = XScuGic_Connect(&Intc, DATA_MOVER_2_INTR_ID,
                            (Xil_InterruptHandler)DataMover2IntrHandler, &DataMover2);
    if (Status != XST_SUCCESS) return XST_FAILURE;

    // 使能中断
    XScuGic_Enable(&Intc, DATA_MOVER_0_INTR_ID);
    XScuGic_Enable(&Intc, DATA_MOVER_1_INTR_ID);
    XScuGic_Enable(&Intc, DATA_MOVER_2_INTR_ID);

    Xil_ExceptionEnable();
    xil_printf("[GIC] Interrupt system initialized successfully\r\n");
    return XST_SUCCESS;
}

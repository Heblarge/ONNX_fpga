/*
 * datamover_driver.c
 *
 * Data Mover Driver Implementation
 * 从 r5_bm_validation/drivers/datamover_wrapper.c 迁移
 */

#include "datamover_driver.h"
#include "xil_printf.h"
#include "xscugic.h"
#include <stdlib.h>

// 全局驱动实例 (用于中断处理回调)
static datamover_driver_t* g_driver = NULL;

// ==================== 中断处理函数 ====================

void DataMover0IntrHandler(void *CallbackRef) {
    XData_mover *InstancePtr = (XData_mover *)CallbackRef;
    u32 IrqStatus = XData_mover_InterruptGetStatus(InstancePtr);
    XData_mover_InterruptClear(InstancePtr, IrqStatus);

    if (g_driver && g_driver->done0) {
        if (IrqStatus & 0x1) g_driver->done0 = 1;
    }
}

void DataMover1IntrHandler(void *CallbackRef) {
    XData_mover *InstancePtr = (XData_mover *)CallbackRef;
    u32 IrqStatus = XData_mover_InterruptGetStatus(InstancePtr);
    XData_mover_InterruptClear(InstancePtr, IrqStatus);

    if (g_driver && g_driver->done1) {
        if (IrqStatus & 0x1) g_driver->done1 = 1;
    }
}

void DataMover2IntrHandler(void *CallbackRef) {
    XData_mover *InstancePtr = (XData_mover *)CallbackRef;
    u32 IrqStatus = XData_mover_InterruptGetStatus(InstancePtr);
    XData_mover_InterruptClear(InstancePtr, IrqStatus);

    if (g_driver && g_driver->done2) {
        if (IrqStatus & 0x1) g_driver->done2 = 1;
    }
}

// ==================== Data Mover 驱动函数 ====================

/**
 * 初始化单个 Data Mover
 */
static int init_one_datamover(XData_mover *InstancePtr, u16 DeviceId, const char *name) {
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

/**
 * 初始化 Data Mover 驱动
 */
int dmdrv_init(datamover_driver_t* driver) {
    if (driver == NULL) {
        return -1;
    }

    memset(driver, 0, sizeof(datamover_driver_t));
    g_driver = driver;

    // 初始化 DataMover0
    driver->handles[0].instance = &driver->dm0;
    driver->handles[0].done_flag = &driver->done0;
    driver->handles[0].name = "DM0";
    if (init_one_datamover(&driver->dm0, DATA_MOVER_0_DEV_ID, "DataMover0") != XST_SUCCESS) {
        return -1;
    }

    // 初始化 DataMover1
    driver->handles[1].instance = &driver->dm1;
    driver->handles[1].done_flag = &driver->done1;
    driver->handles[1].name = "DM1";
    if (init_one_datamover(&driver->dm1, DATA_MOVER_1_DEV_ID, "DataMover1") != XST_SUCCESS) {
        return -1;
    }

    // 初始化 DataMover2
    driver->handles[2].instance = &driver->dm2;
    driver->handles[2].done_flag = &driver->done2;
    driver->handles[2].name = "DM2";
    if (init_one_datamover(&driver->dm2, DATA_MOVER_2_DEV_ID, "DataMover2") != XST_SUCCESS) {
        return -1;
    }

    driver->initialized = true;
    xil_printf("[DM] All Data Movers initialized\n");
    return 0;
}

/**
 * 关闭 Data Mover 驱动
 */
void dmdrv_cleanup(datamover_driver_t* driver) {
    if (driver == NULL) {
        return;
    }

    // 禁用所有 Data Mover 的自动重启和中断
    for (int i = 0; i < 3; i++) {
        XData_mover_DisableAutoRestart(driver->handles[i].instance);
        XData_mover_InterruptGlobalDisable(driver->handles[i].instance);
    }

    driver->initialized = false;
    g_driver = NULL;
    xil_printf("[DM] Data Mover driver cleaned up\n");
}

/**
 * 传输数据
 */
int dmdrv_transfer(datamover_driver_t* driver, int index,
                   uint64_t src_addr, uint64_t dst_addr,
                   uint32_t rows, uint32_t row_len) {
    if (driver == NULL || !driver->initialized) {
        xil_printf("[DM] ERROR: Driver not initialized\n");
        return -1;
    }

    if (index < 0 || index >= 3) {
        xil_printf("[DM] ERROR: Invalid index %d\n", index);
        return -1;
    }

    XData_mover* instance = driver->handles[index].instance;
    volatile int* done_flag = driver->handles[index].done_flag;
    const char* name = driver->handles[index].name;

    // 等待 Data Mover 就绪
    u32 timeout = TIMEOUT_COUNT;
    while (!XData_mover_IsReady(instance) && timeout--) {
        if (timeout == 0) {
            xil_printf("[DM] ERROR: %s not ready\r\n", name);
            return XST_FAILURE;
        }
    }

    // 配置传输参数
    XData_mover_Set_mem_src(instance, src_addr);
    XData_mover_Set_mem_dst(instance, dst_addr);
    XData_mover_Set_rows(instance, rows);
    XData_mover_Set_row_len_bytes(instance, row_len);
    XData_mover_Set_src_stride_bytes(instance, row_len);
    XData_mover_Set_dst_stride_bytes(instance, row_len);
    XData_mover_Set_src_offset_bytes(instance, 0);
    XData_mover_Set_dst_offset_bytes(instance, 0);

    // 清除完成标志
    *done_flag = 0;

    // 启动传输
    XData_mover_Start(instance);

    xil_printf("[DM] %s: 0x%llx -> 0x%llx (%dx%d bytes)\r\n",
               name, src_addr, dst_addr, rows, row_len);

    return 0;
}

/**
 * 等待传输完成
 */
int dmdrv_wait_complete(datamover_driver_t* driver, int index) {
    if (driver == NULL || !driver->initialized) {
        return -1;
    }

    if (index < 0 || index >= 3) {
        return -1;
    }

    volatile int* done_flag = driver->handles[index].done_flag;
    const char* name = driver->handles[index].name;

    u32 timeout = TIMEOUT_COUNT;
    while (!(*done_flag) && timeout--) {
        if (timeout % 10000000 == 0) {
            xil_printf("[DM] %s still in progress...\r\n", name);
        }
    }

    if (!(*done_flag)) {
        xil_printf("[DM] ERROR: %s timeout\r\n", name);
        return XST_FAILURE;
    }

    xil_printf("[DM] %s completed\r\n", name);
    return 0;
}

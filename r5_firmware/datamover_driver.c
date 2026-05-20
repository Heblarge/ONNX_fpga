/*
 * datamover_driver.c
 *
 * Data Mover Driver Implementation
 * 从 r5_bm_validation/drivers/datamover_wrapper.c 迁移
 */

#include "datamover_driver.h"
#include "xil_printf.h"
#include "xscugic.h"
#include <metal/log.h>
#include <metal/sys.h>
#include <stdlib.h>
#include "xil_mpu.h"
// 日志宏 - 使用 libmetal，通过 RPMsg 发送到 A53
#define DM_LOG(fmt, ...) metal_info(fmt, ##__VA_ARGS__)
#define DM_ERR(fmt, ...) metal_err(fmt, ##__VA_ARGS__)

// 全局驱动实例 (用于中断处理回调)
static datamover_driver_t* g_driver = NULL;

// ==================== 中断处理函数 ====================

void DataMover0IntrHandler(void *CallbackRef) {
    XData_mover *InstancePtr = (XData_mover *)CallbackRef;
    u32 IrqStatus = XData_mover_InterruptGetStatus(InstancePtr);
    XData_mover_InterruptClear(InstancePtr, IrqStatus);

    if (g_driver) {
        if (IrqStatus & 0x1) g_driver->done0 = 1;
    }
}

void DataMover1IntrHandler(void *CallbackRef) {
    XData_mover *InstancePtr = (XData_mover *)CallbackRef;
    u32 IrqStatus = XData_mover_InterruptGetStatus(InstancePtr);
    XData_mover_InterruptClear(InstancePtr, IrqStatus);

    if (g_driver) {
        if (IrqStatus & 0x1) g_driver->done1 = 1;
    }
}

void DataMover2IntrHandler(void *CallbackRef) {
    XData_mover *InstancePtr = (XData_mover *)CallbackRef;
    u32 IrqStatus = XData_mover_InterruptGetStatus(InstancePtr);
    XData_mover_InterruptClear(InstancePtr, IrqStatus);

    if (g_driver) {
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

    DM_LOG("[DM] Initializing %s...\r\n", name);
    DM_LOG("[DM] Looking up config for DeviceId=%u\r\n", DeviceId);

    Config = XData_mover_LookupConfig(DeviceId);
    if (NULL == Config) {
        DM_ERR("[DM] Lookup config failed for %s (DeviceId=%u)\r\n", name, DeviceId);
        return XST_FAILURE;
    }

    DM_LOG("[DM] Config found: Ctrl_r=0x%llx, Ctrl=0x%llx\r\n",
           Config->Control_r_BaseAddress, Config->Control_BaseAddress);

    DM_LOG("[DM] Calling CfgInitialize...\r\n");
    Status = XData_mover_CfgInitialize(InstancePtr, Config);
    DM_LOG("[DM] CfgInitialize returned: %d\r\n", Status);

    if (Status != XST_SUCCESS) {
        DM_ERR("[DM] CfgInitialize failed for %s (status=%d)\r\n", name, Status);
        return XST_FAILURE;
    }

    DM_LOG("[DM] === STEP 1: Before DisableAutoRestart ===\r\n");
    DM_LOG("[DM] Calling DisableAutoRestart...\r\n");
    XData_mover_DisableAutoRestart(InstancePtr);
    DM_LOG("[DM] DisableAutoRestart DONE\r\n");

    DM_LOG("[DM] === STEP 2: Before InterruptGlobalEnable ===\r\n");
    DM_LOG("[DM] Calling InterruptGlobalEnable...\r\n");
    XData_mover_InterruptGlobalEnable(InstancePtr);
    DM_LOG("[DM] InterruptGlobalEnable DONE\r\n");

    DM_LOG("[DM] === STEP 3: Before InterruptEnable ===\r\n");
    DM_LOG("[DM] Calling InterruptEnable...\r\n");
    XData_mover_InterruptEnable(InstancePtr, 0x3);
    DM_LOG("[DM] InterruptEnable DONE\r\n");

    DM_LOG("[DM] === DataMover0 INIT COMPLETE ===\r\n");
    return XST_SUCCESS;
}

/**
 * 初始化 Data Mover 驱动（不连接中断，参考裸机代码）
 * 中断连接应该在 dmdrv_setup_interrupts() 中单独完成
 */
int dmdrv_init(datamover_driver_t* driver) {
    if (driver == NULL) {
        return -1;
    }

    memset(driver, 0, sizeof(datamover_driver_t));
    g_driver = driver;

    DM_LOG("[DM] Initializing DataMover instances...\r\n");

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

    // 注意：中断连接移到 dmdrv_setup_interrupts() 中
    // 这样可以确保 DataMover 完全初始化后再设置中断

    driver->initialized = true;
    DM_LOG("[DM] All Data Movers initialized (interrupts not yet connected)\n");
    return 0;
}

/**
 * 设置 DataMover 中断（在 dmdrv_init() 之后调用）
 * 参考裸机代码 full_system_test_v0/src/drivers/sys_intr.c
 */
int dmdrv_setup_interrupts(datamover_driver_t* driver) {
    if (driver == NULL || !driver->initialized) {
        DM_ERR("[DM] ERROR: Driver not initialized\n");
        return -1;
    }

    DM_LOG("[DM] Setting up interrupts...\r\n");

    // 检查 GIC 是否已初始化
    // IsReady 被设置为 XIL_COMPONENT_IS_READY (通常是 0x11111111，不是 1)
    if (xInterruptController.IsReady == 0) {
        DM_ERR("[DM] ERROR: GIC not initialized!\r\n");
        return -1;
    }

    // 连接 DataMover0 中断
    DM_LOG("[DM] Connecting DM0 interrupt (ID=%u)...\r\n", DATA_MOVER_0_INTR_ID);
    if (XScuGic_Connect(&xInterruptController, DATA_MOVER_0_INTR_ID,
                        (Xil_ExceptionHandler)DataMover0IntrHandler, &driver->dm0) != XST_SUCCESS) {
        DM_ERR("[DM] Failed to connect DM0 interrupt\r\n");
        return -1;
    }

    // 连接 DataMover1 中断
    DM_LOG("[DM] Connecting DM1 interrupt (ID=%u)...\r\n", DATA_MOVER_1_INTR_ID);
    if (XScuGic_Connect(&xInterruptController, DATA_MOVER_1_INTR_ID,
                        (Xil_ExceptionHandler)DataMover1IntrHandler, &driver->dm1) != XST_SUCCESS) {
        DM_ERR("[DM] Failed to connect DM1 interrupt\r\n");
        return -1;
    }

    // 连接 DataMover2 中断
    DM_LOG("[DM] Connecting DM2 interrupt (ID=%u)...\r\n", DATA_MOVER_2_INTR_ID);
    if (XScuGic_Connect(&xInterruptController, DATA_MOVER_2_INTR_ID,
                        (Xil_ExceptionHandler)DataMover2IntrHandler, &driver->dm2) != XST_SUCCESS) {
        DM_ERR("[DM] Failed to connect DM2 interrupt\r\n");
        return -1;
    }

    // 使能 DataMover 中断
    XScuGic_Enable(&xInterruptController, DATA_MOVER_0_INTR_ID);
    XScuGic_Enable(&xInterruptController, DATA_MOVER_1_INTR_ID);
    XScuGic_Enable(&xInterruptController, DATA_MOVER_2_INTR_ID);

    DM_LOG("[DM] Interrupts registered: DM0=%d, DM1=%d, DM2=%d\r\n",
            DATA_MOVER_0_INTR_ID, DATA_MOVER_1_INTR_ID, DATA_MOVER_2_INTR_ID);

    return 0;
}

/**
 * 关闭 Data Mover 驱动
 */
void dmdrv_cleanup(datamover_driver_t* driver) {
    if (driver == NULL) {
        return;
    }

    // 禁用 DataMover 中断
    XScuGic_Disable(&xInterruptController, DATA_MOVER_0_INTR_ID);
    XScuGic_Disable(&xInterruptController, DATA_MOVER_1_INTR_ID);
    XScuGic_Disable(&xInterruptController, DATA_MOVER_2_INTR_ID);

    // 断开 DataMover 中断
    XScuGic_Disconnect(&xInterruptController, DATA_MOVER_0_INTR_ID);
    XScuGic_Disconnect(&xInterruptController, DATA_MOVER_1_INTR_ID);
    XScuGic_Disconnect(&xInterruptController, DATA_MOVER_2_INTR_ID);

    // 禁用所有 Data Mover 的自动重启和中断
    for (int i = 0; i < 3; i++) {
        XData_mover_DisableAutoRestart(driver->handles[i].instance);
        XData_mover_InterruptGlobalDisable(driver->handles[i].instance);
    }

    driver->initialized = false;
    g_driver = NULL;
    DM_LOG("[DM] Data Mover driver cleaned up\n");
}

/**
 * 传输数据
 */
int dmdrv_transfer(datamover_driver_t* driver, int index,
                   uint64_t src_addr, uint64_t dst_addr,
                   uint32_t rows, uint32_t row_len) {
    if (driver == NULL || !driver->initialized) {
        DM_ERR("[DM] ERROR: Driver not initialized\n");
        return -1;
    }

    if (index < 0 || index >= 3) {
        DM_ERR("[DM] ERROR: Invalid index %d\n", index);
        return -1;
    }

    XData_mover* instance = driver->handles[index].instance;
    volatile int* done_flag = driver->handles[index].done_flag;
    const char* name = driver->handles[index].name;

    // 等待 Data Mover 就绪
    u32 timeout = TIMEOUT_COUNT;
    while (!XData_mover_IsReady(instance) && timeout--) {
        if (timeout == 0) {
            DM_ERR("[DM] ERROR: %s not ready\r\n", name);
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

    DM_LOG("[DM] %s: 0x%llx -> 0x%llx (%dx%d bytes)\r\n",
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
            DM_LOG("[DM] %s still in progress...\r\n", name);
        }
    }

    if (!(*done_flag)) {
        DM_ERR("[DM] ERROR: %s timeout\r\n", name);
        return XST_FAILURE;
    }

    DM_LOG("[DM] %s completed\r\n", name);
    return 0;
}

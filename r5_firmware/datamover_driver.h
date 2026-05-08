/*
 * datamover_driver.h
 *
 * Data Mover Driver for R5 Firmware
 * 从 r5_bm_validation/drivers/datamover_wrapper.h 迁移
 *
 * 用于在 PS DDR 和 PL BRAM 之间传输矩阵数据
 */

#ifndef DATAMOVER_DRIVER_H
#define DATAMOVER_DRIVER_H

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#include "xdata_mover.h"

// Data Mover 设备 ID
#define DATA_MOVER_0_DEV_ID  XPAR_XDATA_MOVER_0_DEVICE_ID
#define DATA_MOVER_1_DEV_ID  XPAR_XDATA_MOVER_1_DEVICE_ID
#define DATA_MOVER_2_DEV_ID  XPAR_XDATA_MOVER_2_DEVICE_ID

// 超时计数
#define TIMEOUT_COUNT       100000000

// 矩阵参数
#define MATRIX_ROWS         32
#define MATRIX_COLS         32
#define MATRIX_SIZE         (MATRIX_ROWS * MATRIX_COLS * sizeof(int32_t))

// Data Mover 句柄
typedef struct {
    XData_mover* instance;
    volatile int* done_flag;
    const char* name;
} datamover_handle_t;

// Data Mover 驱动上下文
typedef struct {
    XData_mover dm0;
    XData_mover dm1;
    XData_mover dm2;
    volatile int done0;
    volatile int done1;
    volatile int done2;
    datamover_handle_t handles[3];
    bool initialized;
} datamover_driver_t;

/**
 * 初始化 Data Mover 驱动
 * @param driver 驱动句柄
 * @return 0=成功, <0=失败
 */
int dmdrv_init(datamover_driver_t* driver);

/**
 * 关闭 Data Mover 驱动
 * @param driver 驱动句柄
 */
void dmdrv_cleanup(datamover_driver_t* driver);

/**
 * 传输数据 (PS DDR -> BRAM 或 BRAM -> PS DDR)
 * @param driver 驱动句柄
 * @param index Data Mover 索引 (0, 1, 2)
 * @param src_addr 源地址
 * @param dst_addr 目标地址
 * @param rows 行数
 * @param row_len 每行字节数
 * @return 0=成功, <0=失败
 */
int dmdrv_transfer(datamover_driver_t* driver, int index,
                   uint64_t src_addr, uint64_t dst_addr,
                   uint32_t rows, uint32_t row_len);

/**
 * 等待传输完成
 * @param driver 驱动句柄
 * @param index Data Mover 索引
 * @return 0=成功, <0=超时
 */
int dmdrv_wait_complete(datamover_driver_t* driver, int index);

/**
 * 中断处理函数 (需要注册到 GIC)
 */
void DataMover0IntrHandler(void *CallbackRef);
void DataMover1IntrHandler(void *CallbackRef);
void DataMover2IntrHandler(void *CallbackRef);

#ifdef __cplusplus
}
#endif

#endif // DATAMOVER_DRIVER_H

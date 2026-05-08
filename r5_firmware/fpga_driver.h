/*
 * fpga_driver.h
 *
 * FPGA Accelerator Driver for R5 Firmware
 * Provides AXI4-Lite interface to configure the FPGA accelerator
 */

#ifndef FPGA_DRIVER_H
#define FPGA_DRIVER_H

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

// FPGA 寄存器地址定义
// 这些地址需要根据实际硬件设计修改
#define FPGA_ACCELERATOR_BASE  0x80000000  // AXI4-Lite 基地址

// 指令寄存器偏移
#define FPGA_REG_UID                      0x00
#define FPGA_REG_MATRIX_OP                0x04
#define FPGA_REG_SHIFT_AFTER_MATMUL       0x08
#define FPGA_REG_DO_TRANSPOSE             0x0C
#define FPGA_REG_ACTIVATION               0x10
#define FPGA_REG_SHIFT_AFTER_ACTIVATION   0x14
#define FPGA_REG_INPUT0_SHAPE0            0x18
#define FPGA_REG_INPUT0_SHAPE1            0x1C
#define FPGA_REG_INPUT1_SHAPE0            0x20  // 新增
#define FPGA_REG_INPUT1_SHAPE1            0x24
#define FPGA_REG_SHIFT_A                  0x28
#define FPGA_REG_SHIFT_B                  0x2C
#define FPGA_REG_CONTROL                  0x30  // 控制寄存器
#define FPGA_REG_STATUS                   0x34  // 状态寄存器

// 控制寄存器位定义
#define FPGA_CTRL_START       (1 << 0)  // 启动计算
#define FPGA_CTRL_RESET       (1 << 1)  // 复位

// 状态寄存器位定义
#define FPGA_STATUS_BUSY       (1 << 0)  // 忙标志
#define FPGA_STATUS_DONE       (1 << 1)  // 完成标志
#define FPGA_STATUS_ERROR      (1 << 2)  // 错误标志

// 矩阵操作类型
typedef enum {
    FPGA_OP_MATMUL      = 0,
    FPGA_OP_ELEMENT_ADD = 1,
    FPGA_OP_ELEMENT_MUL = 2,
    FPGA_OP_ELEMENT_MAX = 3
} fpga_matrix_op_t;

// 激活函数类型
typedef enum {
    FPGA_ACT_EXP      = 0,
    FPGA_ACT_LOG      = 1,
    FPGA_ACT_SOFTPLUS = 2,
    FPGA_ACT_RELU     = 3,
    FPGA_ACT_NONE     = 4
} fpga_activation_t;

// FPGA 指令结构 (对应 ComputeInstruction_Simplified_TypeDef)
typedef struct {
    uint32_t UID;
    uint32_t matrixOperation;
    int32_t  shiftLeft_AfterMatrixOperation;
    uint8_t  doTranspose;
    uint32_t activationFunction;
    int32_t  shiftLeft_AfterActivation;
    uint32_t input0Shape0;
    uint32_t input0Shape1;
    uint32_t input1Shape0;  // 新增！
    uint32_t input1Shape1;
    int32_t  shiftLeft_A;
    int32_t  shiftLeft_B;
} fpga_instruction_t;

// FPGA 驱动句柄
typedef struct {
    volatile uint32_t* base;     // 寄存器基址
    bool initialized;
} fpga_driver_t;

/**
 * 初始化 FPGA 驱动
 * @param driver 驱动句柄
 * @return 0=成功, <0=失败
 */
int fpga_init(fpga_driver_t* driver);

/**
 * 关闭 FPGA 驱动
 * @param driver 驱动句柄
 */
void fpga_cleanup(fpga_driver_t* driver);

/**
 * 发送指令到 FPGA
 * @param driver 驱动句柄
 * @param inst 指令
 * @return 0=成功, <0=失败
 */
int fpga_send_instruction(fpga_driver_t* driver, const fpga_instruction_t* inst);

/**
 * 等待 FPGA 完成
 * @param driver 驱动句柄
 * @param timeout_ms 超时时间(毫秒)
 * @return 0=成功, <0=超时或错误
 */
int fpga_wait_completion(fpga_driver_t* driver, int timeout_ms);

/**
 * 读取 FPGA 状态
 * @param driver 驱动句柄
 * @return 状态寄存器值
 */
uint32_t fpga_read_status(fpga_driver_t* driver);

/**
 * 复位 FPGA
 * @param driver 驱动句柄
 */
void fpga_reset(fpga_driver_t* driver);

#ifdef __cplusplus
}
#endif

#endif // FPGA_DRIVER_H

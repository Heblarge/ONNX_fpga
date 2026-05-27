/*
 * fpga_driver.h
 *
 * FPGA Accelerator Driver for R5 Firmware
 * Provides AXI4-Lite interface to configure the FPGA accelerator
 *
 * 指令格式与 Accelerator/InstJavaTODO.java 完全对齐
 */

#ifndef FPGA_DRIVER_H
#define FPGA_DRIVER_H

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

// ==================== FPGA 寄存器地址定义 ====================
// 从 r5_bm_validation/config.h 迁移而来
// 这些地址需要根据实际硬件设计修改

// 加速器指令寄存器基址
#define INSTR_BASEADDR       0x80000000  // XPAR_WRAPFORFPGA_0_BASEADDR
#define INSTR_FIRE_OFFSET    0x10        // 触发指令执行

// Cache 控制器
#define CACHE_CTRL           0x80010000  // XPAR_DUALCACHE_CTRL_0_BASEADDR
#define CACHE_A_LIFECYCLE_OFFSET 0x00    // Cache A 生命周期配置
#define CACHE_B_LIFECYCLE_OFFSET 0x04    // Cache B 生命周期配置

// BRAM 地址空间 (用于 Data Mover 数据传输)
#define BRAM0_BASE           0xA0000000  // XPAR_AXI_BRAM_CTRL_0_S_AXI_BASEADDR
#define BRAM1_BASE           0xA0010000  // XPAR_AXI_BRAM_CTRL_1_S_AXI_BASEADDR
#define BRAM2_BASE           0xA0020000  // XPAR_AXI_BRAM_CTRL_2_S_AXI_BASEADDR
#define BRAM_SIZE            0x100000    // 1MB 每个

// DDR 地址空间 (PS DDR)
#define DDR_BASE             0x00000000  // XPAR_PSU_DDR_0_S_AXI_BASEADDR

// ==================== 指令格式定义 ====================
// 与 Accelerator/InstJavaTODO.java 完全对齐

// 矩阵操作类型
#define MATRIX_OP_MATMUL      0
#define MATRIX_OP_ELEMENT_ADD 1
#define MATRIX_OP_ELEMENT_MUL 2
#define MATRIX_OP_ELEMENT_MAX 3

// 激活函数类型
#define ACTIVATION_EXP       0
#define ACTIVATION_LOG       1
#define ACTIVATION_SOFTPLUS  2
#define ACTIVATION_RELU      3
#define ACTIVATION_NONE      4

// ==================== 缓冲区状态定义 ====================
// 与 JNI 层 accelerator_jni.cpp 保持一致
#define BUFFER_STATUS_FREE     0x00
#define BUFFER_STATUS_READY    0x01  // A53已写入数据，等待R5处理
#define BUFFER_STATUS_BUSY     0x02  // R5正在处理
#define BUFFER_STATUS_DONE     0x03  // R5处理完成
#define BUFFER_STATUS_ERROR    0xFF

// ==================== 指令结构 (对应 InstJavaTODO.java) ====================
// 简化设计：直接使用4个block，消除64个buffer的复杂地址转换
// Java: InstJavaTODO(int UID, String matrixOperation, int shiftLeft_AfterMatrixOperation,
//                    boolean doTranspose, String activationFunction, int shiftLeft_AfterActivation,
//                    int blockIdA, int blockIdB, int blockIdZ,
//                    int input0Shape0, int input0Shape1, int input1Shape1,
//                    int shiftLeft_A, int shiftLeft_B)

typedef struct {
    int32_t  UID;                           // 唯一标识符
    uint8_t  matrixOperation;               // 0=MatMul, 1=ElementAdd, 2=ElementMul, 3=ElementMax
    int8_t   shiftLeft_AfterMatrixOperation;
    uint8_t  doTranspose;                   // 0=false, 1=true
    uint8_t  activationFunction;            // 0=Exp, 1=Log, 2=Softplus, 3=Relu, 4=None
    int8_t   shiftLeft_AfterActivation;
    int32_t  blockIdA;                      // 输入A的block索引 (0-3)
    int32_t  blockIdB;                      // 输入B的block索引 (0-3)
    int32_t  blockIdZ;                      // 输出Z的block索引 (0-3)
    int32_t  input0Shape0;                  // input0Shape[0] (行数)
    int32_t  input0Shape1;                  // input0Shape[1] (列数)
    int32_t  input1Shape0;                  // input1Shape[0] (行数，ElementWise时等于input0Shape0)
    int32_t  input1Shape1;                  // input1Shape[1] (列数)
    int8_t   shiftLeft_A;                   // 输入A左移位数
    int8_t   shiftLeft_B;                   // 输入B左移位数
    // 总共: 4+1+1+1+1+1+4+4+4+4+4+4+4+1+1 = 40字节
} __attribute__((packed)) instruction_msg_t;

// FPGA 128-bit 紧凑指令格式 (用于硬件)
// 从 r5_bm_validation/drivers/accelerator.c 迁移
typedef struct {
    uint32_t UID;                           // 位 [18:0] (19位)
    uint32_t matrixOperation;               // 位 [20:19] (2位)
    int32_t  shiftLeft_AfterMatrixOperation;// 位 [26:21] (6位)
    uint8_t  doTranspose;                   // 位 [27] (1位)
    uint32_t activationFunction;            // 位 [30:28] (3位)
    int32_t  shiftLeft_AfterActivation;     // 位 [36:31] (6位)
    uint32_t input0Shape0;                  // 位 [52:37] (16位)
    uint32_t input0Shape1;                  // 位 [68:53] (16位)
    uint32_t input1Shape0;                  // 位 [84:69] (16位)
    uint32_t input1Shape1;                  // 位 [100:85] (16位)
    int32_t  shiftLeft_A;                   // 位 [106:101] (6位)
    int32_t  shiftLeft_B;                   // 位 [112:107] (6位)
    // 总共 113 位，填充到 128 位 (16 字节)
    uint8_t  _padding[1];
} __attribute__((packed)) fpga_instruction_t;

// ==================== FPGA 驱动句柄 ====================
typedef struct {
    volatile uint32_t* instr_base;   // 指令寄存器基址
    volatile uint32_t* cache_base;   // Cache 控制器基址
    bool initialized;
} fpga_driver_t;

// ==================== 函数声明 ====================

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
 * 组装 128-bit 指令数据到缓冲区
 * 从 r5_bm_validation/drivers/accelerator.c 迁移
 */
void fpga_make_instruction(
    uint8_t *cmd_buffer,
    uint32_t uid,
    uint8_t matrixOp,
    int8_t shiftAfterMatrix,
    uint8_t doTranspose,
    uint8_t activationFunc,
    int8_t shiftAfterActiv,
    uint32_t input0Shape_0,
    uint32_t input0Shape_1,
    uint32_t input1Shape_0,
    uint32_t input1Shape_1,
    int8_t shiftLeft_A,
    int8_t shiftLeft_B
);

/**
 * 将指令写入硬件寄存器并触发
 * 从 r5_bm_validation/drivers/accelerator.c 迁移
 */
void fpga_write_instruction(volatile uint32_t *base, uint8_t *cmd);

/**
 * 配置 Cache 生命周期
 */
void fpga_configure_cache(fpga_driver_t* driver, uint16_t cycle_a, uint16_t cycle_b);

/**
 * 从消息格式转换为硬件指令格式
 */
void fpga_convert_instruction(const instruction_msg_t* msg, fpga_instruction_t* inst);

/**
 * 发送指令到 FPGA
 */
int fpga_send_instruction(fpga_driver_t* driver, const fpga_instruction_t* inst);

/**
 * 等待 FPGA 完成 (轮询方式)
 * @param timeout_us 超时时间(微秒)
 */
int fpga_wait_completion(fpga_driver_t* driver, int timeout_us);

/**
 * 复位 FPGA
 */
void fpga_reset(fpga_driver_t* driver);

#ifdef __cplusplus
}
#endif

#endif // FPGA_DRIVER_H

/*
 * fpga_driver.c
 *
 * FPGA Accelerator Driver Implementation
 * Merged from r5_bm_validation/drivers/accelerator.c logic
 */

#include "fpga_driver.h"
#include "xil_io.h"
#include "xil_printf.h"
#include <string.h>
#include <unistd.h>

#define usleep_range(us_min, us_max) usleep((us_min + us_max) / 2)

// ==================== 内部辅助函数 ====================

/**
 * 设置位域 (从 r5_bm_validation/drivers/accelerator.c)
 */
static void set_bits(uint8_t *buffer, int start_bit, int length, uint64_t value) {
    uint64_t v = value;
    for (int i = 0; i < length; i++) {
        int bit_pos = start_bit + i;
        int byte_idx = bit_pos / 8;
        int bit_idx = bit_pos % 8;
        uint8_t bit = (v >> i) & 1;
        if (bit)
            buffer[byte_idx] |= (1 << bit_idx);
        else
            buffer[byte_idx] &= ~(1 << bit_idx);
    }
}

// ==================== FPGA 驱动函数 ====================

/**
 * 初始化 FPGA 驱动
 */
int fpga_init(fpga_driver_t* driver) {
    if (driver == NULL) {
        return -1;
    }

    // 设置寄存器基址
    driver->instr_base = (volatile uint32_t*)INSTR_BASEADDR;
    driver->cache_base = (volatile uint32_t*)CACHE_CTRL;

    if (driver->instr_base == NULL) {
        xil_printf("[FPGA] ERROR: Failed to map instruction registers\n");
        return -1;
    }

    if (driver->cache_base == NULL) {
        xil_printf("[FPGA] ERROR: Failed to map cache controller\n");
        return -1;
    }

    driver->initialized = true;
    xil_printf("[FPGA] Driver initialized: instr=0x%08x, cache=0x%08x\n",
               INSTR_BASEADDR, CACHE_CTRL);
    return 0;
}

/**
 * 关闭 FPGA 驱动
 */
void fpga_cleanup(fpga_driver_t* driver) {
    if (driver == NULL) {
        return;
    }

    if (driver->initialized) {
        fpga_reset(driver);
        driver->initialized = false;
    }
    xil_printf("[FPGA] Driver cleaned up\n");
}

/**
 * 组装 128-bit 指令数据到缓冲区
 * 从 r5_bm_validation/drivers/accelerator.c 迁移
 *
 * 指令格式 (113 位有效数据):
 * - [18:0]    UID (19位)
 * - [20:19]   matrixOperation (2位)
 * - [26:21]   shiftLeft_AfterMatrixOperation (6位)
 * - [27]      doTranspose (1位)
 * - [30:28]   activationFunction (3位)
 * - [36:31]   shiftLeft_AfterActivation (6位)
 * - [52:37]   input0Shape0 (16位)
 * - [68:53]   input0Shape1 (16位)
 * - [84:69]   input1Shape0 (16位)
 * - [100:85]  input1Shape1 (16位)
 * - [106:101] shiftLeft_A (6位)
 * - [112:107] shiftLeft_B (6位)
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
) {
    memset(cmd_buffer, 0, 16);  // 128-bit = 16 字节

    set_bits(cmd_buffer, 0, 19, uid & 0x7FFFF);
    set_bits(cmd_buffer, 19, 2, matrixOp & 0x3);
    set_bits(cmd_buffer, 21, 6, (uint64_t)(shiftAfterMatrix & 0x3F));
    set_bits(cmd_buffer, 27, 1, doTranspose & 0x1);
    set_bits(cmd_buffer, 28, 3, activationFunc & 0x7);
    set_bits(cmd_buffer, 31, 6, (uint64_t)(shiftAfterActiv & 0x3F));
    set_bits(cmd_buffer, 37, 16, input0Shape_0 & 0xFFFF);
    set_bits(cmd_buffer, 53, 16, input0Shape_1 & 0xFFFF);
    set_bits(cmd_buffer, 69, 16, input1Shape_0 & 0xFFFF);
    set_bits(cmd_buffer, 85, 16, input1Shape_1 & 0xFFFF);
    set_bits(cmd_buffer, 101, 6, (uint64_t)(shiftLeft_A & 0x3F));
    set_bits(cmd_buffer, 107, 6, (uint64_t)(shiftLeft_B & 0x3F));
}

/**
 * 将指令写入硬件寄存器并触发
 * 从 r5_bm_validation/drivers/accelerator.c 迁移
 */
void fpga_write_instruction(volatile uint32_t *base, uint8_t *cmd) {
    uint32_t dword_data[4] = {0};

    // 将字节转换为 32-bit 字
    for (int i = 0; i < 4; i++) {
        for (int b = 0; b < 4; b++) {
            int byte_index = i * 4 + b;
            if (byte_index < 16) {
                dword_data[i] |= ((uint32_t)cmd[byte_index]) << (b * 8);
            }
        }
    }

    // 写入 4 个寄存器
    base[0] = dword_data[0];
    base[1] = dword_data[1];
    base[2] = dword_data[2];
    base[3] = dword_data[3];

    // 触发指令执行
    Xil_Out32((uintptr_t)base + INSTR_FIRE_OFFSET, 0x1);

    xil_printf("[FPGA] Instruction written and fired\n");
}

/**
 * 配置 Cache 生命周期
 */
void fpga_configure_cache(fpga_driver_t* driver, uint16_t cycle_a, uint16_t cycle_b) {
    if (driver == NULL || !driver->initialized) {
        return;
    }

    Xil_Out32((uintptr_t)driver->cache_base + CACHE_A_LIFECYCLE_OFFSET, cycle_a);
    Xil_Out32((uintptr_t)driver->cache_base + CACHE_B_LIFECYCLE_OFFSET, cycle_b);

    xil_printf("[FPGA] Cache configured: A=%u cycles, B=%u cycles\n", cycle_a, cycle_b);
}

/**
 * 从消息格式转换为硬件指令格式
 */
void fpga_convert_instruction(const instruction_msg_t* msg, fpga_instruction_t* inst) {
    inst->UID = msg->UID;
    inst->matrixOperation = msg->matrixOperation;
    inst->shiftLeft_AfterMatrixOperation = msg->shiftLeft_AfterMatrixOperation;
    inst->doTranspose = msg->doTranspose;
    inst->activationFunction = msg->activationFunction;
    inst->shiftLeft_AfterActivation = msg->shiftLeft_AfterActivation;
    inst->input0Shape0 = msg->input0Shape0;
    inst->input0Shape1 = msg->input0Shape1;
    inst->input1Shape0 = msg->input1Shape0;
    inst->input1Shape1 = msg->input1Shape1;
    inst->shiftLeft_A = msg->shiftLeft_A;
    inst->shiftLeft_B = msg->shiftLeft_B;
}

/**
 * 发送指令到 FPGA
 */
int fpga_send_instruction(fpga_driver_t* driver, const fpga_instruction_t* inst) {
    if (driver == NULL || !driver->initialized || inst == NULL) {
        xil_printf("[FPGA] ERROR: Invalid driver or instruction\n");
        return -1;
    }

    // 组装 128-bit 指令
    uint8_t cmd_buffer[16];
    fpga_make_instruction(
        cmd_buffer,
        inst->UID,
        inst->matrixOperation,
        inst->shiftLeft_AfterMatrixOperation,
        inst->doTranspose,
        inst->activationFunction,
        inst->shiftLeft_AfterActivation,
        inst->input0Shape0,
        inst->input0Shape1,
        inst->input1Shape0,
        inst->input1Shape1,
        inst->shiftLeft_A,
        inst->shiftLeft_B
    );

    // 写入硬件并触发
    fpga_write_instruction(driver->instr_base, cmd_buffer);

    return 0;
}

/**
 * 等待 FPGA 完成
 * 注意: 当前硬件可能没有状态寄存器，这里使用固定延迟
 */
int fpga_wait_completion(fpga_driver_t* driver, int timeout_us) {
    if (driver == NULL || !driver->initialized) {
        return -1;
    }

    // 简单延迟等待 (根据实际计算时间调整)
    // TODO: 如果硬件有状态寄存器，应该轮询状态
    usleep_range(100, 200);  // 假设每次计算约 100-200us

    return 0;
}

/**
 * 复位 FPGA
 */
void fpga_reset(fpga_driver_t* driver) {
    if (driver == NULL || !driver->initialized) {
        return;
    }

    xil_printf("[FPGA] Resetting...\n");

    // 触发复位 (如果有复位寄存器)
    // Xil_Out32((uintptr_t)driver->instr_base + INSTR_FIRE_OFFSET, 0x0);

    usleep_range(100, 200);
}

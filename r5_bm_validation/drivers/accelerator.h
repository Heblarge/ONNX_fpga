#ifndef ACCELERATOR_H
#define ACCELERATOR_H

#include <stdint.h>

// 组装 128-bit 指令数据到缓冲区
void make_command_data(
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

// 将指令写入硬件寄存器并触发
void write_instruction(uint8_t *cmd);

// 配置 Cache 生命周期
void ConfigureCacheLifecycle(uint16_t cycle_a, uint16_t cycle_b);

#endif

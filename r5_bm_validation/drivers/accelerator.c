#include "accelerator.h"
#include "../config.h"
#include "xil_io.h"
#include <string.h>

void set_bits(uint8_t *buffer, int start_bit, int length, uint64_t value) {
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

void make_command_data(
    uint8_t *cmd_buffer,
    uint32_t uid,
    uint8_t matrixOp,
    int8_t shiftAfterMatrix,//矩阵移位
    uint8_t doTranspose,//转置
    uint8_t activationFunc,//定义域限制
    int8_t shiftAfterActiv,//activation之后移位
    uint32_t input0Shape_0,
    uint32_t input0Shape_1,
    uint32_t input1Shape_0,
    uint32_t input1Shape_1,
    int8_t shiftLeft_A,
    int8_t shiftLeft_B
) {
    memset(cmd_buffer, 0, 15);
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

void write_instruction(uint8_t *cmd) {
    volatile u32 *target_ptr = (volatile u32 *)INSTR_BASEADDR;
    uint32_t dword_data[4] = {0};
    for (int i = 0; i < 4; i++) {
        for (int b = 0; b < 4; b++) {
            int byte_index = i * 4 + b;
            if (byte_index < 16) {
                dword_data[i] |= ((uint32_t)cmd[byte_index]) << (b * 8);
            }
        }
    }
    target_ptr[0] = dword_data[0];
    target_ptr[1] = dword_data[1];
    target_ptr[2] = dword_data[2];
    target_ptr[3] = dword_data[3];
    Xil_Out32(INSTR_BASEADDR + INSTR_FIRE_OFFSET, 0x1);
}

void ConfigureCacheLifecycle(uint16_t cycle_a, uint16_t cycle_b) {
    Xil_Out32(CACHE_CTRL + CACHE_A_LIFECYCLE_OFFSET, (uint32_t)cycle_a);
    Xil_Out32(CACHE_CTRL + CACHE_B_LIFECYCLE_OFFSET, (uint32_t)cycle_b);
}

/*
 * fpga_driver.c
 *
 * FPGA Accelerator Driver Implementation
 * Provides AXI4-Lite interface to configure the FPGA accelerator
 */

#include "fpga_driver.h"
#include "xil_printf.h"
#include <stdlib.h>
#include <unistd.h>

#define usleep_range(us_min, us_max) usleep((us_min + us_max) / 2)

/**
 * 初始化 FPGA 驱动
 */
int fpga_init(fpga_driver_t* driver) {
    if (driver == NULL) {
        return -1;
    }

    // 设置寄存器基址 (需要根据实际硬件配置)
    // 这里使用示例地址，实际应从设备树获取
    driver->base = (volatile uint32_t*)FPGA_ACCELERATOR_BASE;

    if (driver->base == NULL) {
        xil_printf("ERROR: Failed to map FPGA registers\n");
        return -1;
    }

    // 复位 FPGA
    fpga_reset(driver);

    driver->initialized = true;
    xil_printf("FPGA driver initialized: base=0x%08x\n", FPGA_ACCELERATOR_BASE);
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
}

/**
 * 写入寄存器
 */
static inline void write_reg(volatile uint32_t* base, uint32_t offset, uint32_t value) {
    *(volatile uint32_t*)((uintptr_t)base + offset) = value;
}

/**
 * 读取寄存器
 */
static inline uint32_t read_reg(volatile uint32_t* base, uint32_t offset) {
    return *(volatile uint32_t*)((uintptr_t)base + offset);
}

/**
 * 发送指令到 FPGA
 */
int fpga_send_instruction(fpga_driver_t* driver, const fpga_instruction_t* inst) {
    if (driver == NULL || !driver->initialized || inst == NULL) {
        return -1;
    }

    volatile uint32_t* base = driver->base;

    // 等待 FPGA 空闲
    uint32_t status = read_reg(base, FPGA_REG_STATUS);
    int timeout = 1000;
    while ((status & FPGA_STATUS_BUSY) && timeout > 0) {
        usleep_range(10, 50);
        status = read_reg(base, FPGA_REG_STATUS);
        timeout--;
    }

    if (status & FPGA_STATUS_BUSY) {
        xil_printf("ERROR: FPGA busy timeout\n");
        return -1;
    }

    // 写入指令参数
    write_reg(base, FPGA_REG_UID, inst->UID);
    write_reg(base, FPGA_REG_MATRIX_OP, inst->matrixOperation);
    write_reg(base, FPGA_REG_SHIFT_AFTER_MATMUL, inst->shiftLeft_AfterMatrixOperation);
    write_reg(base, FPGA_REG_DO_TRANSPOSE, inst->doTranspose ? 1 : 0);
    write_reg(base, FPGA_REG_ACTIVATION, inst->activationFunction);
    write_reg(base, FPGA_REG_SHIFT_AFTER_ACTIVATION, inst->shiftLeft_AfterActivation);
    write_reg(base, FPGA_REG_INPUT0_SHAPE0, inst->input0Shape0);
    write_reg(base, FPGA_REG_INPUT0_SHAPE1, inst->input0Shape1);
    write_reg(base, FPGA_REG_INPUT1_SHAPE0, inst->input1Shape0);  // 新增
    write_reg(base, FPGA_REG_INPUT1_SHAPE1, inst->input1Shape1);
    write_reg(base, FPGA_REG_SHIFT_A, inst->shiftLeft_A);
    write_reg(base, FPGA_REG_SHIFT_B, inst->shiftLeft_B);

    // 启动计算
    write_reg(base, FPGA_REG_CONTROL, FPGA_CTRL_START);

    return 0;
}

/**
 * 等待 FPGA 完成
 */
int fpga_wait_completion(fpga_driver_t* driver, int timeout_ms) {
    if (driver == NULL || !driver->initialized) {
        return -1;
    }

    volatile uint32_t* base = driver->base;
    int elapsed = 0;
    const int poll_interval = 100;  // 100us

    while (elapsed < timeout_ms * 10) {
        uint32_t status = read_reg(base, FPGA_REG_STATUS);

        if (status & FPGA_STATUS_ERROR) {
            xil_printf("ERROR: FPGA error detected, status=0x%x\n", status);
            return -1;
        }

        if (status & FPGA_STATUS_DONE) {
            return 0;  // 成功
        }

        usleep_range(poll_interval, poll_interval + 50);
        elapsed += 1;
    }

    xil_printf("ERROR: FPGA timeout after %d ms\n", timeout_ms);
    return -1;
}

/**
 * 读取 FPGA 状态
 */
uint32_t fpga_read_status(fpga_driver_t* driver) {
    if (driver == NULL || !driver->initialized) {
        return 0;
    }
    return read_reg(driver->base, FPGA_REG_STATUS);
}

/**
 * 复位 FPGA
 */
void fpga_reset(fpga_driver_t* driver) {
    if (driver == NULL || !driver->initialized) {
        return;
    }

    volatile uint32_t* base = driver->base;

    // 触发复位
    write_reg(base, FPGA_REG_CONTROL, FPGA_CTRL_RESET);

    // 等待复位完成
    usleep_range(100, 200);

    // 清除复位
    write_reg(base, FPGA_REG_CONTROL, 0);

    // 等待 FPGA 就绪
    usleep_range(100, 200);
}

/*
 * rpmsg-accelerator.c
 *
 * R5 Firmware for FPGA Accelerator Control
 * Receives instructions from A53 via RPMsg,
 * then configures the FPGA accelerator.
 *
 * Merged with r5_bm_validation logic
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "xil_printf.h"
#include <openamp/open_amp.h>
#include <metal/alloc.h>
#include <metal/log.h>
#include "platform_info.h"
#include "fpga_driver.h"

#define RPMSG_SERVICE_NAME "rpmsg-openamp-demo-channel"
#define SHUTDOWN_MSG 0xEF56A55A

// ==================== 消息定义 ====================

// RPMsg 消息类型
#define RPMSG_CMD_INSTRUCTIONS_DATA 0x02   // 直接发送指令数据
#define RPMSG_CMD_COMPLETION          0x03  // 计算完成
#define RPMSG_CMD_ERROR               0x04  // 错误通知

// RPMsg 消息头
typedef struct {
    uint32_t cmd;       // 命令类型
    uint32_t count;     // 指令数量
} __attribute__((packed)) rpmsg_header_t;

// 指令数组最大长度
#define MAX_INSTRUCTIONS 256

// ==================== 全局变量 ====================

static struct rpmsg_endpoint g_lept;
static fpga_driver_t g_fpga;

#define LPRINTF(fmt, ...) xil_printf("[R5] " fmt, ##__VA_ARGS__)
#define LPERROR(fmt, ...) LPRINTF("ERROR: " fmt, ##__VA_ARGS__)

// ==================== 指令处理 ====================

/**
 * 执行单条指令
 */
static int execute_single_instruction(const instruction_msg_t* msg_inst) {
    fpga_instruction_t fpga_inst;

    // 转换指令格式
    fpga_convert_instruction(msg_inst, &fpga_inst);

    LPRINTF("Exec: UID=%u, op=%u, shapes=(%u,%u)x(%u,%u), shifts=(%d,%d)\n",
            fpga_inst.UID,
            fpga_inst.matrixOperation,
            fpga_inst.input0Shape0, fpga_inst.input0Shape1,
            fpga_inst.input1Shape0, fpga_inst.input1Shape1,
            fpga_inst.shiftLeft_A, fpga_inst.shiftLeft_B);

    // 发送到 FPGA
    if (fpga_send_instruction(&g_fpga, &fpga_inst) != 0) {
        LPERROR("Failed to send instruction to FPGA\n");
        return -1;
    }

    // 等待完成
    if (fpga_wait_completion(&g_fpga, 1000) != 0) {
        LPERROR("Instruction timeout\n");
        return -1;
    }

    return 0;
}

/**
 * 处理指令数组消息
 */
static int handle_instructions_data(void *data, size_t len) {
    rpmsg_header_t* header = (rpmsg_header_t*)data;

    uint32_t count = header->count;
    if (count > MAX_INSTRUCTIONS) {
        LPERROR("Too many instructions: %u\n", count);
        return -1;
    }

    LPRINTF("Received %u instructions\n", count);

    // 检查消息长度
    size_t expected_size = sizeof(rpmsg_header_t) + count * sizeof(instruction_msg_t);
    if (len < expected_size) {
        LPERROR("Invalid message length: %zu < %zu\n", len, expected_size);
        return -1;
    }

    // 获取指令数组起始位置
    instruction_msg_t* instructions = (instruction_msg_t*)((uint8_t*)data + sizeof(rpmsg_header_t));

    // 初始化 FPGA 驱动 (如果尚未初始化)
    if (!g_fpga.initialized) {
        if (fpga_init(&g_fpga) != 0) {
            LPERROR("Failed to initialize FPGA driver\n");
            return -1;
        }
        // 配置默认 Cache 生命周期
        fpga_configure_cache(&g_fpga, 100, 100);
    }

    // 执行每条指令
    for (uint32_t i = 0; i < count; i++) {
        if (execute_single_instruction(&instructions[i]) != 0) {
            LPERROR("Failed to execute instruction %u\n", i);
            // 继续执行后续指令
        }
    }

    LPRINTF("All instructions completed\n");
    return 0;
}

// ==================== RPMsg 回调 ====================

static int rpmsg_endpoint_cb(struct rpmsg_endpoint *ept, void *data, size_t len,
                             uint32_t src, void *priv) {
    (void)priv;
    (void)src;

    /* 检查关闭消息 */
    if (*(uint32_t *)data == SHUTDOWN_MSG) {
        LPRINTF("Shutdown message received\n");
        return RPMSG_SUCCESS;
    }

    /* 解析消息头 */
    if (len < sizeof(rpmsg_header_t)) {
        LPERROR("Message too short: %zu\n", len);
        return RPMSG_SUCCESS;
    }

    rpmsg_header_t* header = (rpmsg_header_t*)data;

    switch (header->cmd) {
        case RPMSG_CMD_INSTRUCTIONS_DATA:
            // 处理指令数据
            if (handle_instructions_data(data, len) == 0) {
                // 发送完成通知
                uint32_t response = RPMSG_CMD_COMPLETION;
                rpmsg_send(ept, &response, sizeof(response));
                LPRINTF("Sent completion notification\n");
            } else {
                // 发送错误通知
                uint32_t response = RPMSG_CMD_ERROR;
                rpmsg_send(ept, &response, sizeof(response));
            }
            break;

        default:
            LPRINTF("Unknown command: 0x%x\n", header->cmd);
            break;
    }

    return RPMSG_SUCCESS;
}

static void rpmsg_service_unbind(struct rpmsg_endpoint *ept) {
    (void)ept;
    LPRINTF("Remote endpoint destroyed\n");
}

// ==================== 应用入口 ====================

int32_t app(struct rpmsg_device *rdev, void *priv) {
    int32_t ret;
    struct rproc_plat_info arg;
    char ept_name[32] = RPMSG_SERVICE_NAME;

    arg.rpdev = rdev;
    arg.rproc = priv;

    LPRINTF("Creating rpmsg endpoint\n");

    // 创建 RPMsg 端点
    ret = rpmsg_create_ept(&g_lept, rdev, ept_name,
                           RPMSG_ADDR_ANY, RPMSG_ADDR_ANY,
                           &rpmsg_endpoint_cb,
                           &rpmsg_service_unbind);
    if (ret != 0) {
        LPERROR("Failed to create endpoint\n");
        return -1;
    }

    LPRINTF("RPMsg accelerator endpoint created successfully\n");

    // 等待远程处理器重置
    ret = platform_poll_on_vdev_reset(&arg);

    return ret;
}

int main(int argc, char *argv[]) {
    void *platform = NULL;
    struct rpmsg_device *rpdev;
    int32_t ret;

    LPRINTF("Starting FPGA Accelerator R5 Firmware...\n");

    /* Initialize platform */
    ret = platform_init(argc, argv, &platform);
    if (ret != 0) {
        LPERROR("Failed to initialize platform\n");
        platform_cleanup(platform);
        while (1) ;
    }

    /*
     * 主循环：处理 RPMsg 通信
     */
    while (1) {
        rpdev = platform_create_rpmsg_vdev(platform, 0,
                                           VIRTIO_DEV_DEVICE,
                                           NULL, NULL);
        if (!rpdev) {
            LPERROR("Failed to create rpmsg virtio device\n");
            platform_cleanup(platform);
            while (1) ;
        }

        app(rpdev, platform);
        platform_release_rpmsg_vdev(rpdev, platform);
    }

    /* Never reach here. */
    return ret;
}

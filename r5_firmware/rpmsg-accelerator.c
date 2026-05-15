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
#include <stdarg.h>
#include "xil_cache.h"
#include <openamp/open_amp.h>
#include <metal/alloc.h>
#include <metal/log.h>
#include "platform_info.h"
#include "fpga_driver.h"
#include "datamover_driver.h"

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

// ==================== Buffer池定义 ====================
// 预分配buffer池，每个buffer对应共享内存中的一个固定大小区域
// 共享内存布局（需与rsc_table.c和Java保持一致）:
// - Block 0: 0x3F100000, 10MB
// - Block 1: 0x3FB00000, 10MB
// - Block 2: 0x40500000, 10MB
// - Block 3: 0x40F00000, 10MB
//
// 每个块划分为16个buffer，每个buffer最大640KB (可容纳512x512的int32矩阵)
#define BUFFERS_PER_BLOCK  16
#define BUFFER_MAX_SIZE    (640 * 1024)  // 640KB per buffer
#define BUFFER_MAX_SIZE_HEX 0xA0000      // 640KB = 0xA0000
#define TOTAL_BUFFERS      (4 * BUFFERS_PER_BLOCK)  // 64个buffer

typedef struct {
    uint64_t shm_phys_addr;   // 共享内存物理地址
    uint32_t size;            // buffer大小
    uint32_t reserved;        // 对齐
} buffer_info_t;

// Buffer池映射表
// 每个buffer相隔640KB (0xA0000)，与Java侧SharedMemoryPool对齐
// 布局：每个Block (10MB) 包含 16 个 buffer (每个 640KB)
static const buffer_info_t g_buffer_pool[TOTAL_BUFFERS] = {
    // Block 0: 0x3F100000
    {0x3F100000UL, BUFFER_MAX_SIZE, 0}, {0x3F1A0000UL, BUFFER_MAX_SIZE, 0},
    {0x3F240000UL, BUFFER_MAX_SIZE, 0}, {0x3F2E0000UL, BUFFER_MAX_SIZE, 0},
    {0x3F380000UL, BUFFER_MAX_SIZE, 0}, {0x3F420000UL, BUFFER_MAX_SIZE, 0},
    {0x3F4C0000UL, BUFFER_MAX_SIZE, 0}, {0x3F560000UL, BUFFER_MAX_SIZE, 0},
    {0x3F600000UL, BUFFER_MAX_SIZE, 0}, {0x3F6A0000UL, BUFFER_MAX_SIZE, 0},
    {0x3F740000UL, BUFFER_MAX_SIZE, 0}, {0x3F7E0000UL, BUFFER_MAX_SIZE, 0},
    {0x3F880000UL, BUFFER_MAX_SIZE, 0}, {0x3F920000UL, BUFFER_MAX_SIZE, 0},
    {0x3F9C0000UL, BUFFER_MAX_SIZE, 0}, {0x3FA60000UL, BUFFER_MAX_SIZE, 0},
    // Block 1: 0x3FB00000
    {0x3FB00000UL, BUFFER_MAX_SIZE, 0}, {0x3FBA0000UL, BUFFER_MAX_SIZE, 0},
    {0x3FC40000UL, BUFFER_MAX_SIZE, 0}, {0x3FCE0000UL, BUFFER_MAX_SIZE, 0},
    {0x3FD80000UL, BUFFER_MAX_SIZE, 0}, {0x3FE20000UL, BUFFER_MAX_SIZE, 0},
    {0x3FEC0000UL, BUFFER_MAX_SIZE, 0}, {0x3FF60000UL, BUFFER_MAX_SIZE, 0},
    {0x40000000UL, BUFFER_MAX_SIZE, 0}, {0x400A0000UL, BUFFER_MAX_SIZE, 0},
    {0x40140000UL, BUFFER_MAX_SIZE, 0}, {0x401E0000UL, BUFFER_MAX_SIZE, 0},
    {0x40280000UL, BUFFER_MAX_SIZE, 0}, {0x40320000UL, BUFFER_MAX_SIZE, 0},
    {0x403C0000UL, BUFFER_MAX_SIZE, 0}, {0x40460000UL, BUFFER_MAX_SIZE, 0},
    // Block 2: 0x40500000
    {0x40500000UL, BUFFER_MAX_SIZE, 0}, {0x405A0000UL, BUFFER_MAX_SIZE, 0},
    {0x40640000UL, BUFFER_MAX_SIZE, 0}, {0x406E0000UL, BUFFER_MAX_SIZE, 0},
    {0x40780000UL, BUFFER_MAX_SIZE, 0}, {0x40820000UL, BUFFER_MAX_SIZE, 0},
    {0x408C0000UL, BUFFER_MAX_SIZE, 0}, {0x40960000UL, BUFFER_MAX_SIZE, 0},
    {0x40A00000UL, BUFFER_MAX_SIZE, 0}, {0x40AA0000UL, BUFFER_MAX_SIZE, 0},
    {0x40B40000UL, BUFFER_MAX_SIZE, 0}, {0x40BE0000UL, BUFFER_MAX_SIZE, 0},
    {0x40C80000UL, BUFFER_MAX_SIZE, 0}, {0x40D20000UL, BUFFER_MAX_SIZE, 0},
    {0x40DC0000UL, BUFFER_MAX_SIZE, 0}, {0x40E60000UL, BUFFER_MAX_SIZE, 0},
    // Block 3: 0x40F00000
    {0x40F00000UL, BUFFER_MAX_SIZE, 0}, {0x40FA0000UL, BUFFER_MAX_SIZE, 0},
    {0x41040000UL, BUFFER_MAX_SIZE, 0}, {0x410E0000UL, BUFFER_MAX_SIZE, 0},
    {0x41180000UL, BUFFER_MAX_SIZE, 0}, {0x41220000UL, BUFFER_MAX_SIZE, 0},
    {0x412C0000UL, BUFFER_MAX_SIZE, 0}, {0x41360000UL, BUFFER_MAX_SIZE, 0},
    {0x41400000UL, BUFFER_MAX_SIZE, 0}, {0x414A0000UL, BUFFER_MAX_SIZE, 0},
    {0x41540000UL, BUFFER_MAX_SIZE, 0}, {0x415E0000UL, BUFFER_MAX_SIZE, 0},
    {0x41680000UL, BUFFER_MAX_SIZE, 0}, {0x41720000UL, BUFFER_MAX_SIZE, 0},
    {0x417C0000UL, BUFFER_MAX_SIZE, 0}, {0x41860000UL, BUFFER_MAX_SIZE, 0},
};

// ==================== 全局变量 ====================

static struct rpmsg_endpoint g_lept;        // 主通信端点
static struct rpmsg_endpoint g_log_ept;     // 日志端点
static fpga_driver_t g_fpga;
datamover_driver_t g_datamover;
static bool g_datamover_initialized = false;

// 共享内存 metal I/O region（由 platform_info.c 注册）
// 用于正确访问 A53-R5 共享内存中的 buffer 状态标志
extern struct metal_device *get_shared_mem_device(void);

// ==================== 日志通道配置 ====================
#define LOG_CHANNEL_NAME "rpmsg-log-channel"

// RPMsg 日志输出函数
static void rpmsg_log_print(const char *fmt, ...) {
    char buffer[256];
    va_list args;

    if (!g_log_ept.rdev) return;  // 日志端点未创建，跳过

    va_start(args, fmt);
    int len = vsnprintf(buffer, sizeof(buffer), fmt, args);
    va_end(args);

    if (len > 0) {
        rpmsg_send(&g_log_ept, buffer, len);
    }
}

// 使用 RPMsg 日志的 LPRINTF
#define LPRINTF(fmt, ...) rpmsg_log_print("[R5] " fmt, ##__VA_ARGS__)
#define LPERROR(fmt, ...) LPRINTF("ERROR: " fmt, ##__VA_ARGS__)

// ==================== 前向声明 ====================
static const buffer_info_t* get_buffer_info(int bufferId);

// ==================== 缓冲区状态管理 ====================

/**
 * 获取缓冲区状态标志位的物理地址
 * 标志位位于每个buffer的起始位置（前4字节）
 */
static uint32_t get_buffer_status_phys_addr(int bufferId) {
    const buffer_info_t* info = get_buffer_info(bufferId);
    if (info == NULL) {
        return 0;
    }
    // 状态标志位于buffer起始位置
    return (uint32_t)info->shm_phys_addr;
}

/**
 * 通过libmetal获取缓冲区状态的虚拟地址
 * 将共享内存物理地址映射为虚拟地址
 */
static volatile uint32_t* get_buffer_status_virt_addr(int bufferId) {
    uint32_t phys_addr = get_buffer_status_phys_addr(bufferId);
    if (phys_addr == 0) {
        return NULL;
    }

    // 对于 ZynqMP R5 (32位)，物理地址可以直接用作指针
    // R5 是裸机环境，没有 MMU 虚拟地址转换
    return (volatile uint32_t*)phys_addr;
}

/**
 * 读取缓冲区状态
 */
static uint32_t read_buffer_status(int bufferId) {
    volatile uint32_t* status_addr = get_buffer_status_virt_addr(bufferId);
    if (status_addr == NULL) {
        return BUFFER_STATUS_ERROR;
    }

    // 确保读取最新数据
    Xil_DCacheInvalidateRange((uint32_t)status_addr, sizeof(uint32_t));
    return *status_addr;
}

/**
 * 设置缓冲区状态
 */
static void write_buffer_status(int bufferId, uint32_t status) {
    volatile uint32_t* status_addr = get_buffer_status_virt_addr(bufferId);
    if (status_addr == NULL) {
        return;
    }

    *status_addr = status;
    // 确保A53能看到状态更新
    Xil_DCacheFlushRange((uint32_t)status_addr, sizeof(uint32_t));
}

// ==================== 指令处理 ====================

/**
 * 根据bufferId获取buffer信息
 */
static const buffer_info_t* get_buffer_info(int bufferId) {
    if (bufferId < 0 || bufferId >= TOTAL_BUFFERS) {
        LPERROR("Invalid bufferId: %d\n", bufferId);
        return NULL;
    }
    return &g_buffer_pool[bufferId];
}

/**
 * 执行单条指令
 * 与 Accelerator/InstJavaTODO.java 对齐
 *
 * 流程：
 * 1. 验证bufferId并获取共享内存地址
 * 2. DataMover: 共享内存 → FPGA片上SRAM (sdpramA/B从地址0开始)
 * 3. 发送指令到FPGA
 * 4. 等待计算完成
 * 5. DataMover: FPGA片上SRAM → 共享内存
 */
static int execute_single_instruction(const instruction_msg_t* msg_inst) {
    fpga_instruction_t fpga_inst;

    // 打印接收到的指令信息
    LPRINTF("Exec: UID=%d, op=%d, transpose=%d, act=%d\n",
            msg_inst->UID, msg_inst->matrixOperation,
            msg_inst->doTranspose, msg_inst->activationFunction);
    LPRINTF("     shapes: A=(%d,%d), B=(%d,%d), shifts=(%d,%d)\n",
            msg_inst->input0Shape0, msg_inst->input0Shape1,
            msg_inst->input1Shape0, msg_inst->input1Shape1,
            msg_inst->shiftLeft_A, msg_inst->shiftLeft_B);

    // 0. 检查输入缓冲区状态
    uint32_t statusA = read_buffer_status(msg_inst->bufferIdA);
    uint32_t statusB = read_buffer_status(msg_inst->bufferIdB);
    if (statusA != BUFFER_STATUS_READY) {
        LPERROR("Buffer A not ready: id=%d, status=%d\n", msg_inst->bufferIdA, statusA);
        return -1;
    }
    if (statusB != BUFFER_STATUS_READY) {
        LPERROR("Buffer B not ready: id=%d, status=%d\n", msg_inst->bufferIdB, statusB);
        return -1;
    }

    // 标记输入缓冲区为BUSY，输出缓冲区为BUSY
    write_buffer_status(msg_inst->bufferIdA, BUFFER_STATUS_BUSY);
    write_buffer_status(msg_inst->bufferIdB, BUFFER_STATUS_BUSY);
    write_buffer_status(msg_inst->bufferIdZ, BUFFER_STATUS_BUSY);

    // 1. 获取buffer信息
    const buffer_info_t* bufA = get_buffer_info(msg_inst->bufferIdA);
    const buffer_info_t* bufB = get_buffer_info(msg_inst->bufferIdB);
    const buffer_info_t* bufZ = get_buffer_info(msg_inst->bufferIdZ);

    if (bufA == NULL || bufB == NULL || bufZ == NULL) {
        LPERROR("Invalid buffer IDs: A=%d, B=%d, Z=%d\n",
                msg_inst->bufferIdA, msg_inst->bufferIdB, msg_inst->bufferIdZ);
        return -1;
    }

    LPRINTF("     buffers: A[id=%d,PA=0x%lX], B[id=%d,PA=0x%lX], Z[id=%d,PA=0x%lX]\n",
            msg_inst->bufferIdA, bufA->shm_phys_addr,
            msg_inst->bufferIdB, bufB->shm_phys_addr,
            msg_inst->bufferIdZ, bufZ->shm_phys_addr);

    // 2. 计算数据大小
    uint32_t sizeA = msg_inst->input0Shape0 * msg_inst->input0Shape1 * 4;  // int32 = 4字节
    uint32_t sizeB = msg_inst->input1Shape0 * msg_inst->input1Shape1 * 4;
    uint32_t sizeZ = msg_inst->input0Shape0 * msg_inst->input1Shape1 * 4;
    uint32_t rowLenA = msg_inst->input0Shape1 * 4;
    uint32_t rowLenB = msg_inst->input1Shape1 * 4;
    uint32_t rowLenZ = msg_inst->input1Shape1 * 4;

    // 3. DataMover: 共享内存 → FPGA SRAM
    // FPGA片上SRAM地址 (从r5_bm_validation迁移)
    const uint64_t sdpramA_base = 0xA0000000UL;  // sdpramA
    const uint64_t sdpramB_base = 0xA0010000UL;  // sdpramB
    const uint64_t sdpramZ_base = 0xA0020000UL;  // sdpramZ (输出)

    // Cache同步: Invalidate R5的cache，确保DataMover读取的是A53写入的最新数据
    Xil_DCacheInvalidateRange(bufA->shm_phys_addr, sizeA);
    Xil_DCacheInvalidateRange(bufB->shm_phys_addr, sizeB);

    LPRINTF("     DataMover: shm→FPGA SRAM...\n");

    // 搬运tileA: 共享内存 → sdpramA (从地址0开始)
    // 注意：源地址需要 +64 跳过状态标志区域
    if (dmdrv_transfer(&g_datamover, 0,  // DataMover 0
                       bufA->shm_phys_addr + 64, sdpramA_base,
                       msg_inst->input0Shape0, rowLenA) != 0) {
        LPERROR("Failed to transfer tileA\n");
        return -1;
    }
    if (dmdrv_wait_complete(&g_datamover, 0) != 0) {
        LPERROR("tileA transfer timeout\n");
        return -1;
    }

    // 搬运tileB: 共享内存 → sdpramB (从地址0开始)
    // 注意：源地址需要 +64 跳过状态标志区域
    if (dmdrv_transfer(&g_datamover, 1,  // DataMover 1
                       bufB->shm_phys_addr + 64, sdpramB_base,
                       msg_inst->input1Shape0, rowLenB) != 0) {
        LPERROR("Failed to transfer tileB\n");
        return -1;
    }
    if (dmdrv_wait_complete(&g_datamover, 1) != 0) {
        LPERROR("tileB transfer timeout\n");
        return -1;
    }

    LPRINTF("     DataMover: complete, now triggering FPGA...\n");

    // 4. 动态配置 Cache 生命周期
    // 根据矩阵大小计算所需的Cache生命周期
    // Cache A 存储 input0 (M x K)，每行需要参与计算 N 个输出列
    // Cache B 存储 input1 (K x N)，每列需要参与计算 M 个输出行
    // systolicArraySideNum = 32，数据按32x32分块处理
    //
    // 估算公式（保守计算）：
    // - cycle_a: Cache A 需要支持 (N / 32) 次重用，加上安全裕量
    // - cycle_b: Cache B 需要支持 (M / 32) 次重用，加上安全裕量
    //
    uint32_t tile_count_n = (msg_inst->input1Shape1 + 31) / 32;  // 向上取整
    uint32_t tile_count_m = (msg_inst->input0Shape0 + 31) / 32;  // 向上取整

    // 基本周期数 + 安全裕量(50%) + 最小基准值
    uint16_t cycle_a = (uint16_t)(tile_count_n * 3 / 2 + 10);
    uint16_t cycle_b = (uint16_t)(tile_count_m * 3 / 2 + 10);

    // 限制在合理范围内 (最小10，最大1000)
    if (cycle_a < 10) cycle_a = 10;
    if (cycle_a > 1000) cycle_a = 1000;
    if (cycle_b < 10) cycle_b = 10;
    if (cycle_b > 1000) cycle_b = 1000;

    fpga_configure_cache(&g_fpga, cycle_a, cycle_b);

    // 5. 转换指令格式并发送到FPGA
    fpga_convert_instruction(msg_inst, &fpga_inst);

    if (fpga_send_instruction(&g_fpga, &fpga_inst) != 0) {
        LPERROR("Failed to send instruction to FPGA\n");
        return -1;
    }

    // 6. 等待FPGA完成
    // 根据矩阵大小动态计算等待时间
    // 32x32 约 500us, 512x512 约 128ms (256倍)，使用安全裕量
    uint32_t fpga_wait_us = 500 * (msg_inst->input0Shape0 / 32) * (msg_inst->input1Shape1 / 32);
    if (fpga_wait_us < 500) fpga_wait_us = 500;       // 最小 500us
    if (fpga_wait_us > 500000) fpga_wait_us = 500000; // 最大 500ms

    LPRINTF("     Waiting for FPGA (wait_us=%u)...\n", fpga_wait_us);

    if (fpga_wait_completion(&g_fpga, fpga_wait_us) != 0) {
        LPERROR("Instruction timeout after %u us\n", fpga_wait_us);
        return -1;
    }

    LPRINTF("     FPGA computation complete\n");

    // 7. DataMover: FPGA SRAM → 共享内存
    LPRINTF("     DataMover: FPGA SRAM→shm...\n");

    // 注意：目标地址需要 +64 跳过状态标志区域
    if (dmdrv_transfer(&g_datamover, 2,  // DataMover 2
                       sdpramZ_base, bufZ->shm_phys_addr + 64,
                       msg_inst->input0Shape0, rowLenZ) != 0) {
        LPERROR("Failed to transfer result\n");
        return -1;
    }
    if (dmdrv_wait_complete(&g_datamover, 2) != 0) {
        LPERROR("result transfer timeout\n");
        return -1;
    }

    // Cache同步: Flush R5的cache，确保A53读取时能看到DataMover写入的最新结果
    Xil_DCacheFlushRange(bufZ->shm_phys_addr, sizeZ);

    // 8. 更新缓冲区状态
    write_buffer_status(msg_inst->bufferIdA, BUFFER_STATUS_FREE);  // 输入缓冲区可重用
    write_buffer_status(msg_inst->bufferIdB, BUFFER_STATUS_FREE);
    write_buffer_status(msg_inst->bufferIdZ, BUFFER_STATUS_DONE);   // 输出缓冲区完成

    LPRINTF("     Instruction fully complete, bufferZ marked as DONE\n");

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

    LPRINTF("Received %u instructions (len=%zu)\n", count, len);

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
        // Cache 生命周期将在执行每条指令时动态配置
    }

    // 初始化 DataMover (如果尚未初始化)
    if (!g_datamover_initialized) {
        if (dmdrv_init(&g_datamover) != 0) {
            LPERROR("Failed to initialize DataMover driver\n");
            return -1;
        }
        g_datamover_initialized = true;
        LPRINTF("DataMover driver initialized\n");
    }

    // 执行每条指令
    int failed_count = 0;
    for (uint32_t i = 0; i < count; i++) {
        if (execute_single_instruction(&instructions[i]) != 0) {
            LPERROR("Failed to execute instruction %u\n", i);
            failed_count++;
            // 继续执行后续指令
        }
    }

    LPRINTF("All instructions completed (failed=%d)\n", failed_count);

    // 如果有任何指令失败，返回错误
    if (failed_count > 0) {
        LPERROR("Returning error due to %d failed instructions\n", failed_count);
        return -1;
    }

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

    LPRINTF("RPMsg callback: len=%zu, src=%u\n", len, src);

    /* 解析消息头 */
    if (len < sizeof(rpmsg_header_t)) {
        LPERROR("Message too short: %zu\n", len);
        return RPMSG_SUCCESS;
    }

    rpmsg_header_t* header = (rpmsg_header_t*)data;
    LPRINTF("RPMsg cmd=0x%x, count=%u\n", header->cmd, header->count);

    switch (header->cmd) {
        case RPMSG_CMD_INSTRUCTIONS_DATA:
            // 处理指令数据
            if (handle_instructions_data(data, len) == 0) {
                // 发送完成通知
                uint32_t response = RPMSG_CMD_COMPLETION;
                int ret = rpmsg_send(ept, &response, sizeof(response));
                LPRINTF("Sent completion notification (ret=%d)\n", ret);
            } else {
                // 发送错误通知
                uint32_t response = RPMSG_CMD_ERROR;
                rpmsg_send(ept, &response, sizeof(response));
                LPRINTF("Sent error notification\n");
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

    LPRINTF("RPMsg accelerator endpoint created successfully (addr=%d)\n", g_lept.addr);

    // 创建日志端点 (用于将R5日志发送到A53)
    LPRINTF("Creating log endpoint...\n");
    ret = rpmsg_create_ept(&g_log_ept, rdev, LOG_CHANNEL_NAME,
                           0x401, RPMSG_ADDR_ANY, NULL, NULL);
    if (ret != 0) {
        LPRINTF("Warning: Failed to create log endpoint, logs may not be visible\n");
    } else {
        LPRINTF("Log endpoint created successfully (addr=%d)\n", g_log_ept.addr);
    }

    // 等待远程处理器重置
    LPRINTF("Waiting for vdev reset...\n");
    ret = platform_poll_on_vdev_reset(&arg);
    LPRINTF("Vdev reset complete (ret=%d)\n", ret);

    return ret;
}

int main(int argc, char *argv[]) {
    void *platform = NULL;
    struct rpmsg_device *rpdev;
    int32_t ret;

    // 初始化日志（需要在platform_init之后才能工作）
    /* Initialize platform */
    ret = platform_init(argc, argv, &platform);
    if (ret != 0) {
        LPERROR("Failed to initialize platform\n");
        platform_cleanup(platform);
        while (1) ;
    }

    LPRINTF("Platform initialized successfully\n");

    /*
     * 主循环：处理 RPMsg 通信
     */
    while (1) {
        LPRINTF("Creating RPMsg virtio device...\n");
        rpdev = platform_create_rpmsg_vdev(platform, 0,
                                           VIRTIO_DEV_DEVICE,
                                           NULL, NULL);
        if (!rpdev) {
            LPERROR("Failed to create rpmsg virtio device\n");
            platform_cleanup(platform);
            while (1) ;
        }

        LPRINTF("RPMsg virtio device created\n");

        ret = app(rpdev, platform);
        LPRINTF("App returned with ret=%d\n", ret);

        platform_release_rpmsg_vdev(rpdev, platform);
        LPRINTF("RPMsg virtio device released\n");
    }

    /* Never reach here. */
    return ret;
}

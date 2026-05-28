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
#include "xil_printf.h"
#include <openamp/open_amp.h>
#include <metal/alloc.h>
#include <metal/log.h>
#include "platform_info.h"
#include "fpga_driver.h"
#include "datamover_driver.h"
#include "xparameters.h"

#define RPMSG_SERVICE_NAME "rpmsg-openamp-demo-channel"
#define SHUTDOWN_MSG 0xEF56A55A

// ==================== 消息定义 ====================

// RPMsg 消息类型
#define RPMSG_CMD_INSTRUCTIONS_DATA 0x02   // 直接发送指令数据
#define RPMSG_CMD_COMPLETION          0x03  // 计算完成
#define RPMSG_CMD_ERROR               0x04  // 错误通知
#define RPMSG_CMD_LOG                 0x01  // 日志消息
extern char *get_rsc_trace_info(uint32_t *len);

/* Global trace buffer offset - shared by all trace_print calls */
static uint32_t trace_offset = 0;

void trace_print(const char *msg) {
    uint32_t len;
    char *buf = get_rsc_trace_info(&len);
    if (!buf) return;
    uint32_t start = trace_offset;
    while (*msg && trace_offset < len - 1) {
        buf[trace_offset++] = *msg++;
    }
    buf[trace_offset] = '\0';
    Xil_DCacheFlushRange((UINTPTR)(buf + start), trace_offset - start + 1);
}
// RPMsg 消息头
typedef struct {
    uint32_t cmd;       // 命令类型
    uint32_t count;     // 指令数量
} __attribute__((packed)) rpmsg_header_t;

// 日志消息结构（带前缀标识）
typedef struct {
    uint32_t cmd;       // RPMSG_CMD_LOG
    char log_text[240]; // 日志文本（限制在240字节以内，保证总长度<=256）
} __attribute__((packed)) rpmsg_log_message_t;

// 指令数组最大长度
#define MAX_INSTRUCTIONS 256

// ==================== Block池定义 ====================
// 简化设计：直接使用4个block，每个block作为完整的数据区
// 共享内存布局（需与rsc_table.c和Java保持一致）:
// - Block 0: 0x3F100000, 10MB
// - Block 1: 0x3FB00000, 10MB
// - Block 2: 0x40500000, 10MB
// - Block 3: 0x40F00000, 10MB

#define TOTAL_BLOCKS       4
#define BLOCK_MAX_SIZE     (10 * 1024 * 1024)  // 10MB per block

typedef struct {
    uint64_t shm_phys_addr;   // 共享内存物理地址
    uint32_t size;            // block大小
    uint32_t reserved;        // 对齐
} block_info_t;

// Block池映射表
// 与A核侧的SHM_CONFIG和Java侧SharedMemoryPool完全对齐
static const block_info_t g_block_pool[TOTAL_BLOCKS] = {
    {0x3F100000UL, BLOCK_MAX_SIZE, 0},  // Block 0
    {0x3FB00000UL, BLOCK_MAX_SIZE, 0},  // Block 1
    {0x40500000UL, BLOCK_MAX_SIZE, 0},  // Block 2
    {0x40F00000UL, BLOCK_MAX_SIZE, 0},  // Block 3
};

// ==================== 全局变量 ====================

static struct rpmsg_endpoint g_lept;        // 主通信端点
static fpga_driver_t g_fpga;
datamover_driver_t g_datamover;
static bool g_datamover_initialized = false;

// 共享内存 metal I/O region（由 platform_info.c 注册）
// 用于正确访问 A53-R5 共享内存中的 buffer 状态标志
extern struct metal_device *get_shared_mem_device(void);

// ==================== Metal Log Handler ====================
/**
 * 自定义 libmetal log handler，通过主 RPMsg 通道发送日志到 A53
 */
static void metal_rpmsg_log_handler(enum metal_log_level level,
                                    const char *format, ...) {
    rpmsg_log_message_t log_msg;
    va_list args;
    static int call_count = 0;
    static int send_count = 0;

    call_count++;

    // 检查端点是否已创建
    if (!g_lept.rdev) {
        return;  // 端点未就绪，丢弃日志
    }

    log_msg.cmd = RPMSG_CMD_LOG;

    // 格式化日志文本
    va_start(args, format);
    int len = vsnprintf(log_msg.log_text, sizeof(log_msg.log_text), format, args);
    va_end(args);

    if (len > 0 && len < (int)sizeof(log_msg.log_text)) {
        // 发送日志消息
        int ret = rpmsg_send(&g_lept, &log_msg, sizeof(uint32_t) + len + 1);
        if (ret == 0) {
            send_count++;
        }
    }
}

// 日志宏定义 - 使用 libmetal 的 log 宏
// 这些宏会调用 metal_rpmsg_log_handler（如果已注册）
#define LPRINTF(fmt, ...) metal_info(fmt, ##__VA_ARGS__)
#define LPERROR(fmt, ...) metal_err(fmt, ##__VA_ARGS__)

// ==================== 前向声明 ====================
static const block_info_t* get_block_info(int blockId);

// ==================== Block状态管理 ====================

/**
 * 获取block状态标志位的物理地址
 * 标志位位于每个block的起始位置（前4字节）
 */
static uint32_t get_block_status_phys_addr(int blockId) {
    const block_info_t* info = get_block_info(blockId);
    if (info == NULL) {
        return 0;
    }
    // 状态标志位于block起始位置
    return (uint32_t)info->shm_phys_addr;
}

/**
 * 通过libmetal获取block状态的虚拟地址
 * 将共享内存物理地址映射为虚拟地址
 */
static volatile uint32_t* get_block_status_virt_addr(int blockId) {
    uint32_t phys_addr = get_block_status_phys_addr(blockId);
    if (phys_addr == 0) {
        return NULL;
    }

    // 对于 ZynqMP R5 (32位)，物理地址可以直接用作指针
    // R5 是裸机环境，没有 MMU 虚拟地址转换
    return (volatile uint32_t*)phys_addr;
}

/**
 * 读取block状态
 */
static uint32_t read_block_status(int blockId) {
    volatile uint32_t* status_addr = get_block_status_virt_addr(blockId);
    if (status_addr == NULL) {
        return BUFFER_STATUS_ERROR;
    }

    // 确保读取最新数据
    Xil_DCacheInvalidateRange((uint32_t)status_addr, sizeof(uint32_t));
    return *status_addr;
}

/**
 * 设置block状态
 */
static void write_block_status(int blockId, uint32_t status) {
    volatile uint32_t* status_addr = get_block_status_virt_addr(blockId);
    if (status_addr == NULL) {
        return;
    }

    *status_addr = status;
    // 确保A53能看到状态更新
    Xil_DCacheFlushRange((uint32_t)status_addr, sizeof(uint32_t));
}

// ==================== 指令处理 ====================

/**
 * 根据blockId获取block信息
 */
static const block_info_t* get_block_info(int blockId) {
    if (blockId < 0 || blockId >= TOTAL_BLOCKS) {
        LPERROR("Invalid blockId: %d\n", blockId);
        return NULL;
    }
    return &g_block_pool[blockId];
}

/**
 * 执行单条指令
 * 与 Accelerator/InstJavaTODO.java 对齐
 *
 * 流程：
 * 1. 验证blockId并获取共享内存地址
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

    // 0. 检查输入block状态
    uint32_t statusA = read_block_status(msg_inst->blockIdA);
    uint32_t statusB = read_block_status(msg_inst->blockIdB);
    if (statusA != BUFFER_STATUS_READY) {
        LPERROR("Block A not ready: id=%d, status=%d\n", msg_inst->blockIdA, statusA);
        return -1;
    }
    if (statusB != BUFFER_STATUS_READY) {
        LPERROR("Block B not ready: id=%d, status=%d\n", msg_inst->blockIdB, statusB);
        return -1;
    }

    // 标记输入block为BUSY，输出block为BUSY
    write_block_status(msg_inst->blockIdA, BUFFER_STATUS_BUSY);
    write_block_status(msg_inst->blockIdB, BUFFER_STATUS_BUSY);
    write_block_status(msg_inst->blockIdZ, BUFFER_STATUS_BUSY);

    // 1. 获取block信息
    const block_info_t* blkA = get_block_info(msg_inst->blockIdA);
    const block_info_t* blkB = get_block_info(msg_inst->blockIdB);
    const block_info_t* blkZ = get_block_info(msg_inst->blockIdZ);

    if (blkA == NULL || blkB == NULL || blkZ == NULL) {
        LPERROR("Invalid block IDs: A=%d, B=%d, Z=%d\n",
                msg_inst->blockIdA, msg_inst->blockIdB, msg_inst->blockIdZ);
        return -1;
    }

    LPRINTF("     blocks: A[id=%d,PA=0x%llX], B[id=%d,PA=0x%llX], Z[id=%d,PA=0x%llX]\n",
            msg_inst->blockIdA, (unsigned long long)blkA->shm_phys_addr,
            msg_inst->blockIdB, (unsigned long long)blkB->shm_phys_addr,
            msg_inst->blockIdZ, (unsigned long long)blkZ->shm_phys_addr);

    // 2. 计算数据大小
    uint32_t sizeA = msg_inst->input0Shape0 * msg_inst->input0Shape1 * 4;  // int32 = 4字节
    uint32_t sizeB = msg_inst->input1Shape0 * msg_inst->input1Shape1 * 4;
    uint32_t sizeZ = msg_inst->input0Shape0 * msg_inst->input1Shape1 * 4;
    uint32_t rowLenA = msg_inst->input0Shape1 * 4;
    uint32_t rowLenB = msg_inst->input1Shape1 * 4;
    uint32_t rowLenZ = msg_inst->input1Shape1 * 4;

    // 3. DataMover: 共享内存 → FPGA SRAM
    // FPGA片上SRAM地址（从 xparameters.h 获取正确的 BRAM 地址）
    const uint64_t sdpramA_base = XPAR_AXI_BRAM_CTRL_0_S_AXI_BASEADDR;  // sdpramA: 0xA0000000
    const uint64_t sdpramB_base = XPAR_AXI_BRAM_CTRL_1_S_AXI_BASEADDR;  // sdpramB: 0xA2000000
    const uint64_t sdpramZ_base = XPAR_AXI_BRAM_CTRL_2_S_AXI_BASEADDR;  // sdpramZ: 0xA4000000

    // Cache同步: Invalidate R5的cache，确保DataMover读取的是A53写入的最新数据
    Xil_DCacheInvalidateRange((UINTPTR)blkA->shm_phys_addr, sizeA);
    Xil_DCacheInvalidateRange((UINTPTR)blkB->shm_phys_addr, sizeB);

    LPRINTF("     DataMover: shm→FPGA SRAM...\n");

    // 调试：确认 invalidate 后 CPU 看到的共享内存数据是否正确
    volatile uint32_t* blockA_header_ptr = (volatile uint32_t*)(uintptr_t)(blkA->shm_phys_addr + 0);
    volatile uint32_t* blockA_data_ptr = (volatile uint32_t*)(uintptr_t)(blkA->shm_phys_addr + 64);
    LPRINTF("     blockA header+0: %08X %08X %08X %08X\n",
            blockA_header_ptr[0], blockA_header_ptr[1], blockA_header_ptr[2], blockA_header_ptr[3]);
    LPRINTF("     blockA data+64:   %08X %08X %08X %08X\n",
            blockA_data_ptr[0], blockA_data_ptr[1], blockA_data_ptr[2], blockA_data_ptr[3]);

    // 对照测试：CPU 直接从 DDR 搬到 BRAM，绕过 DataMover
    // 用于确认是否是 SMMU 权限问题导致 DataMover 读 DDR 失败
    volatile uint32_t* cpu_test_src = (volatile uint32_t*)(uintptr_t)(blkA->shm_phys_addr + 64);
    volatile uint32_t* cpu_test_dst = (volatile uint32_t*)(uintptr_t)sdpramA_base;
    for (int i = 0; i < 10; i++) {
        cpu_test_dst[i] = cpu_test_src[i];
    }
    // 确保写入完成后再读取
    Xil_DCacheFlushRange((UINTPTR)sdpramA_base, 10 * sizeof(uint32_t));
    LPRINTF("CPU copy test - sdpramA: %d %d %d %d\n",
            (int)cpu_test_dst[0], (int)cpu_test_dst[1], (int)cpu_test_dst[2], (int)cpu_test_dst[3]);
    LPRINTF("CPU copy test - src    : %d %d %d %d\n",
            (int)cpu_test_src[0], (int)cpu_test_src[1], (int)cpu_test_src[2], (int)cpu_test_src[3]);

    // 搬运tileA: 共享内存 → sdpramA (从地址0开始)
    // 注意：源地址需要 +64 跳过状态标志区域
    if (dmdrv_transfer(&g_datamover, 0,  // DataMover 0
                       blkA->shm_phys_addr + 64, sdpramA_base,
                       msg_inst->input0Shape0, rowLenA) != 0) {
        LPERROR("Failed to transfer tileA\n");
        return -1;
    }
    if (dmdrv_wait_complete(&g_datamover, 0) != 0) {
        LPERROR("tileA transfer timeout\n");
        return -1;
    }

    // 调试：打印 DataMover 搬运后 sdpramA 中的数据
    // DataMover 直接写内存，CPU 读取前需要 invalidate cache
    Xil_DCacheInvalidateRange((UINTPTR)sdpramA_base, msg_inst->input0Shape0 * msg_inst->input0Shape1 * 4);
    volatile uint32_t* sdpramA_ptr = (volatile uint32_t*)(uintptr_t)sdpramA_base;
    LPRINTF("     sdpramA first 10: ");
    for (int i = 0; i < 10; i++) {
        LPRINTF("%d ", sdpramA_ptr[i]);
    }
    LPRINTF("\n");

    // 调试：打印共享内存中 blockA 前10个元素（64字节偏移量后是数据区）
    volatile uint32_t* blockA_ptr = (volatile uint32_t*)(uintptr_t)(blkA->shm_phys_addr + 64);
    LPRINTF("     blockA first 10: ");
    for (int i = 0; i < 10; i++) {
        LPRINTF("%d ", blockA_ptr[i]);
    }
    LPRINTF("\n");

    // 搬运tileB: 共享内存 → sdpramB (从地址0开始)
    // 注意：源地址需要 +64 跳过状态标志区域
    if (dmdrv_transfer(&g_datamover, 1,  // DataMover 1
                       blkB->shm_phys_addr + 64, sdpramB_base,
                       msg_inst->input1Shape0, rowLenB) != 0) {
        LPERROR("Failed to transfer tileB\n");
        return -1;
    }
    if (dmdrv_wait_complete(&g_datamover, 1) != 0) {
        LPERROR("tileB transfer timeout\n");
        return -1;
    }

    // 调试：打印 DataMover 搬运后 sdpramB 中的数据
    // DataMover 直接写内存，CPU 读取前需要 invalidate cache
    Xil_DCacheInvalidateRange((UINTPTR)sdpramB_base, msg_inst->input1Shape0 * msg_inst->input1Shape1 * 4);
    volatile uint32_t* sdpramB_ptr = (volatile uint32_t*)(uintptr_t)sdpramB_base;
    LPRINTF("     sdpramB first 10: ");
    for (int i = 0; i < 10; i++) {
        LPRINTF("%d ", sdpramB_ptr[i]);
    }
    LPRINTF("\n");

    // 调试：打印共享内存中 blockB 前10个元素（64字节偏移量后是数据区）
    volatile uint32_t* blockB_ptr = (volatile uint32_t*)(uintptr_t)(blkB->shm_phys_addr + 64);
    LPRINTF("     blockB first 10: ");
    for (int i = 0; i < 10; i++) {
        LPRINTF("%d ", blockB_ptr[i]);
    }
    LPRINTF("\n");

    LPRINTF("     DataMover: complete, now triggering FPGA...\n");

    // 4. 配置 Cache 生命周期
    // 由于 BRAM 能装下所有数据，Cache 只需要单次访问即可
    // 设置为 1 表示数据只使用一次，不需要重用
    uint16_t cycle_a = 1;
    uint16_t cycle_b = 1;

    LPRINTF("     Before fpga_configure_cache (cycle_a=%u, cycle_b=%u)\n", cycle_a, cycle_b);
    fpga_configure_cache(&g_fpga, cycle_a, cycle_b);
    LPRINTF("     After fpga_configure_cache\n");

    // 5. 转换指令格式并发送到FPGA
    LPRINTF("     Before fpga_convert_instruction\n");
    fpga_convert_instruction(msg_inst, &fpga_inst);
    LPRINTF("     After fpga_convert_instruction\n");

    LPRINTF("     Before fpga_send_instruction\n");
    if (fpga_send_instruction(&g_fpga, &fpga_inst) != 0) {
        LPERROR("Failed to send instruction to FPGA\n");
        return -1;
    }
    LPRINTF("     After fpga_send_instruction\n");

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

    // 调试：打印 sdpramZ 中的计算结果（FPGA 直接写入的）
    // FPGA 直接写内存，CPU 读取前需要 invalidate cache
    Xil_DCacheInvalidateRange((UINTPTR)sdpramZ_base, msg_inst->input0Shape0 * msg_inst->input1Shape1 * 4);
    volatile uint32_t* sdpramZ_ptr = (volatile uint32_t*)(uintptr_t)sdpramZ_base;
    LPRINTF("     sdpramZ first 10: ");
    for (int i = 0; i < 10; i++) {
        LPRINTF("%d ", sdpramZ_ptr[i]);
    }
    LPRINTF("\n");

    // 7. DataMover: FPGA SRAM → 共享内存
    LPRINTF("     DataMover: FPGA SRAM→shm...\n");

    // 注意：目标地址需要 +64 跳过状态标志区域
    if (dmdrv_transfer(&g_datamover, 2,  // DataMover 2
                       sdpramZ_base, blkZ->shm_phys_addr + 64,
                       msg_inst->input0Shape0, rowLenZ) != 0) {
        LPERROR("Failed to transfer result\n");
        return -1;
    }
    if (dmdrv_wait_complete(&g_datamover, 2) != 0) {
        LPERROR("result transfer timeout\n");
        return -1;
    }

    // Cache同步: Flush R5的cache，确保A53读取时能看到DataMover写入的最新结果
    Xil_DCacheFlushRange((UINTPTR)blkZ->shm_phys_addr, sizeZ);

    // 调试：打印传回共享内存后的数据
    volatile uint32_t* blockZ_ptr = (volatile uint32_t*)(uintptr_t)(blkZ->shm_phys_addr + 64);
    LPRINTF("     blockZ first 10 (after DM2): ");
    for (int i = 0; i < 10; i++) {
        LPRINTF("%d ", blockZ_ptr[i]);
    }
    LPRINTF("\n");

    // 8. 更新block状态
    write_block_status(msg_inst->blockIdA, BUFFER_STATUS_FREE);  // 输入block可重用
    write_block_status(msg_inst->blockIdB, BUFFER_STATUS_FREE);
    write_block_status(msg_inst->blockIdZ, BUFFER_STATUS_DONE);   // 输出block完成

    LPRINTF("     Instruction fully complete, blockZ marked as DONE\n");

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
        LPRINTF("About to call fpga_init...\n");
        if (fpga_init(&g_fpga) != 0) {
            LPERROR("Failed to initialize FPGA driver\n");
            return -1;
        }
        LPRINTF("fpga_init returned successfully\n");
        // Cache 生命周期将在执行每条指令时动态配置
    }

    // 初始化 DataMover (如果尚未初始化)
    if (!g_datamover_initialized) {
        LPRINTF("Initializing DataMover driver...\n");
        if (dmdrv_init(&g_datamover) != 0) {
            LPERROR("Failed to initialize DataMover driver\n");
            return -1;
        }
        LPRINTF("Setting up DataMover interrupts...\n");
        if (dmdrv_setup_interrupts(&g_datamover) != 0) {
            LPERROR("Failed to setup DataMover interrupts\n");
            return -1;
        }
        g_datamover_initialized = true;
        LPRINTF("DataMover driver fully initialized\n");
    }

    // 执行每条指令
    int failed_count = 0;
    for (uint32_t i = 0; i < count; i++) {
        // 调试：直接打印关键字段
        LPRINTF("Instruction %u: blockIdA=%d, blockIdB=%d, blockIdZ=%d\n",
                i, instructions[i].blockIdA, instructions[i].blockIdB, instructions[i].blockIdZ);

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

    // 测试消息：收到 0xFFFFFFFF 时发送日志响应
    if (len == sizeof(uint32_t) && *(uint32_t *)data == 0xFFFFFFFF) {
        rpmsg_log_message_t test_log;
        test_log.cmd = RPMSG_CMD_LOG;
        snprintf(test_log.log_text, sizeof(test_log.log_text),
                 "[R5] PONG - RPMsg communication is working!\n");
        rpmsg_send(ept, &test_log, sizeof(uint32_t) + strlen(test_log.log_text) + 1);
        return RPMSG_SUCCESS;
    }

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

    xil_printf("[R5] Creating rpmsg endpoint\n");

    // 创建 RPMsg 端点
    ret = rpmsg_create_ept(&g_lept, rdev, ept_name,
                           RPMSG_ADDR_ANY, RPMSG_ADDR_ANY,
                           &rpmsg_endpoint_cb,
                           &rpmsg_service_unbind);
    if (ret != 0) {
        xil_printf("[R5] ERROR: Failed to create endpoint\n");
        return -1;
    }

    xil_printf("[R5] RPMsg accelerator endpoint created successfully (addr=%d)\n", g_lept.addr);

    // 注册自定义 metal log handler，将日志通过 RPMsg 发送到 A53
    metal_set_log_handler(metal_rpmsg_log_handler);
    metal_set_log_level(METAL_LOG_DEBUG);

    xil_printf("[R5] Metal log handler registered\n");

    // 等待远程处理器重置
    LPRINTF("Waiting for vdev reset...\n");
    ret = platform_poll_on_vdev_reset(&arg);
    LPRINTF("Vdev reset complete (ret=%d)\n", ret);
    /* ===== 出口清理 ===== */
    // 1. 先恢复默认日志handler，避免后续log调用往已销毁的endpoint发送
    metal_set_log_handler(metal_default_log_handler);

    // 2. 注销endpoint
    rpmsg_destroy_ept(&g_lept);

    // 3. 清零g_lept，防止残留指针被误用
    memset(&g_lept, 0, sizeof(g_lept));
    return ret;
}

int main(int argc, char *argv[]) {
    /* FIRST LINE: Direct physical address write, no dependencies */


    void *platform = NULL;
    struct rpmsg_device *rpdev;
    int32_t ret;
    uint32_t tlen;
    char *tb = get_rsc_trace_info(&tlen);
    if (tb) {
        tb[0] = 'M';
        tb[1] = '\0';
        Xil_DCacheFlushRange((UINTPTR)tb, 2);
    }
    /* Initialize platform */
    ret = platform_init(argc, argv, &platform);
    if (ret != 0) {
        xil_printf("[R5] ERROR: Failed to initialize platform\n");
        platform_cleanup(platform);
        while (1) ;
    }

    xil_printf("[R5] Platform initialized successfully\n");

    /*
     * 主循环：处理 RPMsg 通信
     */
    while (1) {
        // 重置trace buffer，防止第二次启动时offset溢出
        trace_offset = 0;
        tb = get_rsc_trace_info(&tlen);
        if (tb) memset(tb, 0, tlen);

    	trace_print(">> before create_virtio\n");
        rpdev = platform_create_rpmsg_vdev(platform, 0,
                                           VIRTIO_DEV_DEVICE,
                                           NULL, NULL);
        trace_print(">> after create_virtio\n");
        if (!rpdev) {
        	LPRINTF("[R5] ERROR: Failed to create rpmsg virtio device\n");
            platform_cleanup(platform);
            while (1) ;
        }

        xil_printf("[R5] RPMsg virtio device created\n");
        ret = app(rpdev, platform);
        xil_printf("[R5] App returned with ret=%d\n", ret);

        platform_release_rpmsg_vdev(rpdev, platform);
        xil_printf("[R5] RPMsg virtio device released\n");

        /* ===== 驱动反初始化，防止第二次启动时驱动卡死 ===== */
        if (g_datamover_initialized) {
            dmdrv_cleanup(&g_datamover);
            g_datamover_initialized = false;
        }
        if (g_fpga.initialized) {
            fpga_cleanup(&g_fpga);
        }

        xil_printf("[R5] Drivers cleaned up, restarting loop...\n");
    }

    /* Never reach here. */
    return ret;
}

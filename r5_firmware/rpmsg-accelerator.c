/*
 * rpmsg-accelerator.c
 *
 * R5 Firmware for FPGA Accelerator Control
 * Receives instructions from A53 via shared memory and RPMsg,
 * then configures the FPGA accelerator.
 *
 * This replaces the echo test with actual accelerator control logic.
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

// 消息命令定义
#define CMD_NEW_INSTRUCTIONS 0x01
#define CMD_COMPLETION       0x02
#define CMD_ERROR            0x03
#define CMD_PING             0x04
#define CMD_PONG             0x05

// 共享内存定义 (使用 vdev0buffer 区域)
// vdev0buffer: 0x3ed48000, 1MB (从设备树获取)
#define SHM_BASE_VDEV0BUFFER  0x3ed48000
#define SHM_SIZE (1 * 1024 * 1024)  // 1MB (vdev0buffer 大小)

#define OFFSET_INSTRUCTION_COUNT  0x00
#define OFFSET_STATUS_FLAG        0x04
#define OFFSET_INSTRUCTION_ARRAY  0x08
#define MAX_INSTRUCTIONS 4096     // 1MB 可容纳约 16K 条指令 (64B/条)

// 状态标志
#define STATUS_IDLE        0
#define STATUS_READY       1
#define STATUS_PROCESSING  2
#define STATUS_COMPLETED   3

// 指令结构 (对应硬件 ComputeInstruction_Simplified_TypeDef)
typedef struct {
    uint32_t UID;
    uint32_t matrixOperation;
    int32_t  shiftLeft_AfterMatrixOperation;
    uint8_t  doTranspose;
    uint32_t activationFunction;
    int32_t  shiftLeft_AfterActivation;
    // 地址字段在 Simplified_TypeDef 中固定为 0，不需要传输
    uint32_t input0Shape0;
    uint32_t input0Shape1;
    uint32_t input1Shape0;  // 新增！
    uint32_t input1Shape1;
    int32_t  shiftLeft_A;
    int32_t  shiftLeft_B;
    uint8_t  _padding[3];
} __attribute__((packed)) InstructionStruct;

// 共享内存指针 (通过 libmetal 访问)
static struct {
    struct metal_io_region* io;
    void* base;
    uint32_t size;
} g_shm = {
    .io = NULL,
    .base = NULL,
    .size = 0
};

static struct rpmsg_endpoint g_lept;
static fpga_driver_t g_fpga;

#define LPRINTF(fmt, ...) xil_printf("%s():%u " fmt, __func__, __LINE__, ##__VA_ARGS__)
#define LPERROR(fmt, ...) LPRINTF("ERROR: " fmt, ##__VA_ARGS__)

// ========== 共享内存操作 ==========

/**
 * 初始化共享内存访问
 * 使用 libmetal 访问共享内存区域
 */
static int init_shared_memory(struct rpmsg_device *rdev, struct metal_init_params* metal_params) {
    // 使用 libmetal 获取 vdev0buffer 共享内存区域
    struct metal_device *device;
    struct metal_io_region *io;
    unsigned int irq_info;

    // 获取共享内存设备
    int ret = metal_device_open("vdev0buffer", NULL, &device);
    if (ret != 0) {
        // 如果 libmetal 自动映射失败，使用手动映射
        // vdev0buffer 地址从设备树获取
        g_shm.base = (void*)SHM_BASE_VDEV0BUFFER;
        g_shm.size = SHM_SIZE;
        g_shm.io = NULL;
    } else {
        io = metal_device_io_region(device, 0);
        if (io == NULL) {
            LPRINTF("Failed to get vdev0buffer IO region\n");
            return -1;
        }
        g_shm.io = io;
        g_shm.base = metal_io_virt(io, 0);
        g_shm.size = metal_io_region_size(io);
    }

    // 清零共享内存
    memset(g_shm.base, 0, g_shm.size);

    LPRINTF("Shared memory initialized: base=0x%08x, size=%u\n",
            (uint32_t)g_shm.base, g_shm.size);

    return 0;
}

/**
 * 读取指令计数
 */
static inline uint32_t read_instruction_count(void) {
    if (g_shm.base == NULL) return 0;
    volatile uint32_t* count_ptr = (volatile uint32_t*)((uintptr_t)g_shm.base + OFFSET_INSTRUCTION_COUNT);
    return *count_ptr;
}

/**
 * 读取状态标志
 */
static inline uint32_t read_status(void) {
    if (g_shm.base == NULL) return 0;
    volatile uint32_t* status_ptr = (volatile uint32_t*)((uintptr_t)g_shm.base + OFFSET_STATUS_FLAG);
    return *status_ptr;
}

/**
 * 写入状态标志
 */
static inline void write_status(uint32_t status) {
    if (g_shm.base == NULL) return;
    volatile uint32_t* status_ptr = (volatile uint32_t*)((uintptr_t)g_shm.base + OFFSET_STATUS_FLAG);
    *status_ptr = status;
}

/**
 * 读取指令
 */
static inline bool read_instruction(uint32_t index, InstructionStruct* inst) {
    if (g_shm.base == NULL || index >= MAX_INSTRUCTIONS) return false;

    InstructionStruct* inst_array = (InstructionStruct*)((uintptr_t)g_shm.base + OFFSET_INSTRUCTION_ARRAY);
    *inst = inst_array[index];
    return true;
}

// ========== FPGA 控制 ==========

/**
 * 将 InstructionStruct 转换为 FPGA 指令格式
 */
static void convert_to_fpga_instruction(const InstructionStruct* src, fpga_instruction_t* dst) {
    dst->UID = src->UID;
    dst->matrixOperation = src->matrixOperation;
    dst->shiftLeft_AfterMatrixOperation = src->shiftLeft_AfterMatrixOperation;
    dst->doTranspose = src->doTranspose;
    dst->activationFunction = src->activationFunction;
    dst->shiftLeft_AfterActivation = src->shiftLeft_AfterActivation;
    // 地址字段在 Simplified_TypeDef 中固定为 0
    dst->input0Address = 0;
    dst->input1Address = 0;
    dst->outputAddress = 0;
    dst->input0Shape0 = src->input0Shape0;
    dst->input0Shape1 = src->input0Shape1;
    dst->input1Shape0 = src->input1Shape0;  // 新增
    dst->input1Shape1 = src->input1Shape1;
    dst->shiftLeft_A = src->shiftLeft_A;
    dst->shiftLeft_B = src->shiftLeft_B;
}

/**
 * 执行指令序列
 */
static int execute_instructions(void) {
    uint32_t count = read_instruction_count();
    if (count == 0 || count > MAX_INSTRUCTIONS) {
        LPERROR("Invalid instruction count: %u\n", count);
        return -1;
    }

    LPRINTF("Executing %u instructions...\n", count);

    // 更新状态为处理中
    write_status(STATUS_PROCESSING);

    // 初始化 FPGA 驱动
    if (fpga_init(&g_fpga) != 0) {
        LPERROR("Failed to initialize FPGA driver\n");
        write_status(STATUS_IDLE);
        return -1;
    }

    // 执行每条指令
    for (uint32_t i = 0; i < count; i++) {
        InstructionStruct inst;
        if (!read_instruction(i, &inst)) {
            LPERROR("Failed to read instruction %u\n", i);
            continue;
        }

        fpga_instruction_t fpga_inst;
        convert_to_fpga_instruction(&inst, &fpga_inst);

        LPRINTF("Exec inst[%u]: UID=%u, op=%u, shape0=(%u,%u) shape1=(%u,%u)\n",
                i, inst.UID, inst.matrixOperation,
                inst.input0Shape0, inst.input0Shape1,
                inst.input1Shape0, inst.input1Shape1);

        // 发送指令到 FPGA
        if (fpga_send_instruction(&g_fpga, &fpga_inst) != 0) {
            LPERROR("Failed to send instruction %u to FPGA\n", i);
            continue;
        }

        // 等待 FPGA 完成
        if (fpga_wait_completion(&g_fpga, 1000) != 0) {
            LPERROR("Instruction %u timeout\n", i);
            continue;
        }
    }

    // 关闭 FPGA 驱动
    fpga_cleanup(&g_fpga);

    // 更新状态为完成
    write_status(STATUS_COMPLETED);

    LPRINTF("All instructions executed successfully\n");
    return 0;
}

// ========== RPMsg 回调 ==========

static int rpmsg_endpoint_cb(struct rpmsg_endpoint *ept, void *data, size_t len,
                             uint32_t src, void *priv) {
    (void)priv;
    (void)src;

    /* 检查关闭消息 */
    if (*(uint32_t *)data == SHUTDOWN_MSG) {
        LPRINTF("Shutdown message received\n");
        return RPMSG_SUCCESS;
    }

    /* 处理命令消息 */
    uint32_t* cmd = (uint32_t*)data;
    switch (*cmd) {
        case CMD_NEW_INSTRUCTIONS:
            LPRINTF("New instructions notification received\n");
            // 启动指令执行
            execute_instructions();
            // 发送完成通知
            uint32_t response = CMD_COMPLETION;
            rpmsg_send(ept, &response, sizeof(response));
            break;

        case CMD_PING:
            LPRINTF("Ping received, sending pong\n");
            uint32_t pong = CMD_PONG;
            rpmsg_send(ept, &pong, sizeof(pong));
            break;

        default:
            LPRINTF("Unknown command: 0x%x\n", *cmd);
            break;
    }

    return RPMSG_SUCCESS;
}

static void rpmsg_service_unbind(struct rpmsg_endpoint *ept) {
    (void)ept;
    LPRINTF("Unexpected Remote endpoint destroy\n");
}

// ========== 应用入口 ==========

int32_t app(struct rpmsg_device *rdev, void *priv) {
    int32_t ret;
    struct rproc_plat_info arg;
    char ept_name[32] = RPMSG_SERVICE_NAME;

    arg.rpdev = rdev;
    arg.rproc = priv;

    // 初始化共享内存访问
    ret = init_shared_memory(rdev, NULL);
    if (ret != 0) {
        LPERROR("Failed to initialize shared memory\n");
        return ret;
    }

    // 创建 RPMsg 端点
    LPRINTF("Creating rpmsg endpoint\n");
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
        while (1)
            ;
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
            while (1)
                ;
        }

        app(rpdev, platform);
        platform_release_rpmsg_vdev(rpdev, platform);
    }

    /* Never reach here. */
    return ret;
}

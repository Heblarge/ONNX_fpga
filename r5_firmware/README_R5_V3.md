# R5 Firmware - 融合版本

## 概述

此版本的 R5 firmware 融合了 `r5_bm_validation` 验证程序中的硬件寄存器地址和指令格式。

## 文件结构

```
r5_firmware/
├── fpga_driver.c/h         # FPGA 加速器驱动 (融合了 r5_bm_validation 的逻辑)
├── datamover_driver.c/h    # Data Mover 驱动 (从 r5_bm_validation 迁移)
├── rpmsg-accelerator.c     # RPMsg 通信主程序
├── platform_info.c/h       # OpenAMP 平台信息
└── ...
```

## 主要改动

### 1. 寄存器地址定义 (fpga_driver.h)

从 `r5_bm_validation/config.h` 迁移的地址定义：

```c
#define INSTR_BASEADDR       0x80000000  // 加速器指令寄存器
#define INSTR_FIRE_OFFSET    0x10        // 触发指令执行
#define CACHE_CTRL           0x80001000  // Cache 控制器
#define BRAM0_BASE           0xA0000000  // BRAM0 (矩阵 A)
#define BRAM1_BASE           0xA0010000  // BRAM1 (矩阵 B)
#define BRAM2_BASE           0xA0020000  // BRAM2 (结果)
```

### 2. 指令格式 (fpga_driver.c)

使用 128-bit 紧凑指令格式，与 `r5_bm_validation/drivers/accelerator.c` 一致：

```c
void fpga_make_instruction(
    uint8_t *cmd_buffer,
    uint32_t uid,
    uint8_t matrixOp,      // 0=matmul, 1=add, 2=mul, 3=max
    int8_t shiftAfterMatrix,
    uint8_t doTranspose,
    uint8_t activationFunc, // 0=exp, 1=log, 2=softplus, 3=relu
    int8_t shiftAfterActiv,
    uint32_t input0Shape_0,
    uint32_t input0Shape_1,
    uint32_t input1Shape_0,
    uint32_t input1Shape_1,
    int8_t shiftLeft_A,
    int8_t shiftLeft_B
);
```

### 3. RPMsg 通信格式

消息格式：

```c
typedef struct {
    uint32_t cmd;   // RPMSG_CMD_INSTRUCTIONS_DATA = 0x02
    uint32_t count; // 指令数量
} rpmsg_header_t;

// 紧跟 instruction_msg_t 数组
typedef struct {
    uint32_t UID;
    uint32_t matrixOperation;
    int32_t  shiftLeft_AfterMatrixOperation;
    uint8_t  doTranspose;
    uint32_t activationFunction;
    int32_t  shiftLeft_AfterActivation;
    uint32_t input0Shape0;
    uint32_t input0Shape1;
    uint32_t input1Shape0;
    uint32_t input1Shape1;
    int32_t  shiftLeft_A;
    int32_t  shiftLeft_B;
} __attribute__((packed)) instruction_msg_t;
```

## 使用示例

### A53 端发送指令：

```c
// JNI 层已经实现了这个格式
// Java: HWAcceleratorJNI.executeInstructions(instructions)
```

### R5 端处理流程：

1. 接收 RPMsg 消息
2. 解析指令数组
3. 初始化 FPGA 驱动
4. 配置 Cache 生命周期
5. 逐条执行指令：
   - 组装 128-bit 指令
   - 写入硬件寄存器
   - 触发执行
   - 等待完成
6. 发送完成通知

## Data Mover 驱动 (可选)

如果需要使用 Data Mover 在 DDR 和 BRAM 之间传输数据：

```c
#include "datamover_driver.h"

datamover_driver_t dmdrv;
dmdrv_init(&dmdrv);

// 传输矩阵 A: DDR -> BRAM0
dmdrv_transfer(&dmdrv, 0, ddr_addr_a, BRAM0_BASE, 32, 128);
dmdrv_wait_complete(&dmdrv, 0);

// 传输矩阵 B: DDR -> BRAM1
dmdrv_transfer(&dmdrv, 1, ddr_addr_b, BRAM1_BASE, 32, 128);
dmdrv_wait_complete(&dmdrv, 1);

// ... 执行指令 ...

// 读取结果: BRAM2 -> DDR
dmdrv_transfer(&dmdrv, 2, BRAM2_BASE, ddr_addr_c, 32, 128);
dmdrv_wait_complete(&dmdrv, 2);

dmdrv_cleanup(&dmdrv);
```

## 编译

确保 Makefile 包含以下源文件：

```
r5_firmware/src/
├── rpmsg-accelerator.c
├── fpga_driver.c
├── datamover_driver.c      # 可选
├── platform_info.c
└── rsc_table.c
```

## 注意事项

1. **寄存器地址需要根据实际硬件配置修改** - 当前使用的是示例地址
2. **Cache 生命周期配置** - 默认值为 100，可能需要根据实际性能调整
3. **中断处理** - Data Mover 需要配置 GIC 中断
4. **超时时间** - 当前使用简单延迟，实际应该轮询状态寄存器

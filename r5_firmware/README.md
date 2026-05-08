# R5 FPGA Accelerator Firmware

## 目录结构

```
r5_firmware/
├── rpmsg-echo.c           # 原 RPMsg 回环固件
├── rpmsg-echo.h
├── rpmsg-accelerator.c    # FPGA 加速器控制固件 (新增)
├── fpga_driver.c          # FPGA 驱动实现 (新增)
├── fpga_driver.h          # FPGA 驱动头文件 (新增)
├── zynqmp_r5_a53_rproc.c
└── README.md              # 本文件
```

## 功能说明

`rpmsg-accelerator.c` 替代原有的回环测试，实现以下功能：

1. 接收来自 A53 的 RPMsg 通知
2. 从共享内存读取计算指令
3. 配置 FPGA 加速器执行计算
4. 返回完成通知

## 共享内存布局

```
从设备树获取的内存布局 (RPU0):
┌─────────────────────────────────────────────────────────┐
│ 0x3ed00000 - 0x3ed3ffff: rproc_0_reserved (256KB)       │  R5 私有内存
├─────────────────────────────────────────────────────────┤
│ 0x3ed40000 - 0x3ed43fff: vring0 (16KB)                  │  RPMsg vring
├─────────────────────────────────────────────────────────┤
│ 0x3ed44000 - 0x3ed47fff: vring1 (16KB)                  │  RPMsg vring
├─────────────────────────────────────────────────────────┤
│ 0x3ed48000 - 0x3ee47fff: vdev0buffer (1MB)              │  共享数据区域 ★
│   ┌──────────────────────────────────────────────────┐  │
│   │ +0x00: instruction_count (4B)                    │  │
│   │ +0x04: status_flag (4B)                          │  │
│   │ +0x08: instructions[] (64B each, up to ~16K)    │  │
│   └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘

状态标志定义:
- 0 = IDLE (空闲)
- 1 = READY (指令就绪)
- 2 = PROCESSING (处理中)
- 3 = COMPLETED (完成)
```

## FPGA 寄存器地址

```
Base Address: 0x80000000 (需根据实际硬件修改)

Offset  Register                Description
------ ------------------------ ----------------------------
0x00   REG_UID                 指令唯一标识符
0x04   REG_MATRIX_OP           矩阵操作类型
0x08   REG_SHIFT_AFTER_MATMUL  矩阵操作后移位量
0x0C   REG_DO_TRANSPOSE        是否转置
0x10   REG_ACTIVATION          激活函数类型
0x14   REG_SHIFT_AFTER_ACT     激活后移位量
0x18   REG_INPUT0_SHAPE0       输入0形状[0]
0x1C   REG_INPUT0_SHAPE1       输入0形状[1]
0x20   REG_INPUT1_SHAPE1       输入1形状[1]
0x24   REG_SHIFT_A             A矩阵移位量
0x28   REG_SHIFT_B             B矩阵移位量
0x30   REG_CONTROL             控制寄存器 (BIT0=START, BIT1=RESET)
0x34   REG_STATUS              状态寄存器 (BIT0=BUSY, BIT1=DONE, BIT2=ERROR)
```

## 编译

### 交叉编译工具链

```bash
export PATH=$PATH:/path/to/xilinx-gnu-aarch64/toolchain/bin
export CC=aarch64-none-elf-gcc
export AR=aarch64-none-elf-ar
```

### 编译命令

```bash
cd r5_firmware
aarch64-none-elf-gcc -c -mcpu=cortex-r5 -mfpu=vfpv3-d16 \
    -mfloat-abi=hard -Wall -O2 -g \
    -I/path/to/openamp/include \
    -I/path/to/libmetal/include \
    -I/path/to/xilinx_embeddedsw \
    fpga_driver.c rpmsg-accelerator.c

aarch64-none-elf-gcc -o rpmsg-accelerator.elf \
    fpga_driver.o rpmsg-accelerator.o \
    -L/path/to/openamp/lib -L/path/to/libmetal/lib \
    -lopenamp -lmetal -lm
```

## 部署到 ZynqMP

### 1. 将固件复制到目标系统

```bash
scp rpmsg-accelerator.elf root@target:/lib/firmware/
```

### 2. 加载 R5 固件

在 A53 Linux 端执行：

```bash
# 停止现有 R5 固件
echo stop > /sys/class/remoteproc/remoteproc0/state

# 设置新固件
echo rpmsg-accelerator.elf > /sys/class/remoteproc/remoteproc0/firmware

# 启动 R5
echo start > /sys/class/remoteproc/remoteproc0/state
```

### 3. 验证 RPMsg 通信

```bash
# 检查 RPMsg 设备
ls -la /dev/rpmsg*

# 查看内核日志
dmesg | grep -i rpmsg
```

## 配置修改

### 修改共享内存地址

共享内存地址从设备树 `vdev0buffer` 自动获取：
- RPU0: 0x3ed48000 (1MB)
- RPU1: 0x3ef48000 (1MB)

如果需要修改，编辑 `rpmsg-accelerator.c`:

```c
#define SHM_BASE_VDEV0BUFFER  0x3ed48000  // 对应设备树中的 vdev0buffer
#define SHM_SIZE (1 * 1024 * 1024)        // 1MB
```

A53 侧 JNI (`accelerator_jni.cpp`) 也需要同步修改：

```c
#define SHM_PHYS_BASE 0x3ed48000  // vdev0buffer 物理地址
#define SHM_SIZE (1 * 1024 * 1024)  // 1MB
```

### 修改 FPGA 基地址

编辑 `fpga_driver.h`:

```c
#define FPGA_ACCELERATOR_BASE 0x80000000  // 修改为实际地址
```

## 调试

### R5 固件日志

R5 固件通过 `xil_printf` 输出日志，可以通过以下方式查看：

1. 串口输出 (UART)
2. 共享内存日志缓冲区
3. RPMsg 消息发送到 A53

### 常见问题

**Q: R5 固件无法启动**
- 检查固件路径是否正确
- 检查设备树中的内存区域配置
- 查看 dmesg 日志

**Q: RPMsg 端点创建失败**
- 确保 openamp 库版本匹配
- 检查 virtio 设备配置

**Q: FPGA 配置失败**
- 检查 FPGA 基地址是否正确
- 检查 AXI4-Lite 接口是否连接
- 使用示波器/逻辑分析仪验证信号

## 性能优化

1. **批量发送指令**: 一次发送多条指令减少 RPMsg 开销
2. **DMA 数据传输**: 使用 DMA 在共享内存和 FPGA DDR 间传输大块数据
3. **指令流水线**: FPGA 支持指令流水线时可以持续发送指令

## 参考资料

- Xilinx OpenAMP: https://github.com/Xilinx/embeddedsw
- Libmetal: https://github.com/Xilinx/libmetal
- Linux RPMsg: Documentation/rpmsg.txt

# R5 FPGA Accelerator Firmware (v2)

## 设计原则

使用标准 OpenAMP 框架，通过 RPMsg 接收指令。

## 代码结构

```
r5_firmware/
├── rpmsg-accelerator.c         # R5 固件主程序
├── fpga_driver.c               # FPGA 驱动 (AXI4-Lite)
├── fpga_driver.h
└── README_R5_V2.md             # 本文档
```

## OpenAMP/RPMsg 通信流程

```
A53 Linux                      R5 Baremetal
────────                      ─────────────
1. Java 生成指令
2. JNI 通过 RPMsg 发送 →───────────→ 3. rpmsg_endpoint_cb 收到
4. 等待...                            ↓
                                  5. 解析指令
                                     ↓
                                  6. 配置 FPGA
                                     ↓
7. ←──────────────────────── 8. RPMsg 发送完成通知
9. 读取结果
```

## 指令格式

```c
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
} InstructionStruct;  // 44 字节
```

## RPMsg 消息协议

### 消息类型

```c
#define RPMSG_CMD_INSTRUCTIONS_DATA 0x02  // 直接发送指令数据
#define RPMSG_CMD_COMPLETION       0x03  // 计算完成
#define RPMSG_CMD_ERROR            0x04  // 错误通知
```

### 消息格式

```
+----------------+----------------+
| cmd (4B)       | 消息类型      |
+----------------+----------------+
| count (4B)     | 指令数量      |
+----------------+----------------+
| instructions[] | 指令数组      |
+----------------+----------------+
```

## 编译

### 环境变量

```bash
export CC=aarch64-none-elf-gcc
export AR=aarch64-none-elf-ar
export CROSS_COMPILE=aarch64-none-elf-
```

### 编译命令

```bash
cd r5_firmware

# 编译
$CC -c -mcpu=cortex-r5 -mfpu=vfpv3-d16 -mfloat-abi=hard \
    -Wall -O2 -g \
    -I$(OPENAMP)/include \
    -I$(LIBMETAL)/include \
    -I$(XILINX_EMBEDDEDSW)/libsw_services/openamp \
    fpga_driver.c rpmsg-accelerator.c

# 链接
$CC -o rpmsg-accelerator.elf \
    fpga_driver.o rpmsg-accelerator.o \
    -L$(OPENAMP)/lib -lopenamp \
    -L$(LIBMETAL)/lib -lmetal \
    -lm
```

## 部署

### 1. 复制固件到目标系统

```bash
scp rpmsg-accelerator.elf root@target:/lib/firmware/
```

### 2. 加载 R5 固件

```bash
# 停止现有 R5 固件
echo stop > /sys/class/remoteproc/remoteproc0/state

# 设置新固件
echo rpmsg-accelerator.elf > /sys/class/remoteproc/remoteproc0/firmware

# 启动 R5
echo start > /sys/class/remoteproc/remoteproc0/state

# 查看日志
dmesg | tail -20
```

## 调试

### R5 固件日志

R5 固件通过 `xil_printf` 输出日志，可通过：

1. **串口输出** (UART) - 最直接
2. **共享内存日志** - libmetal 支持的方式
3. **RPMsg 消息** - 发送调试信息到 A53

### 检查 RPMsg 状态

```bash
# A53 端
cat /sys/class/remoteproc/remoteproc0/state
cat /sys/class/remoteproc/remoteproc0/firmware

# 查看 RPMsg 端点
ls -la /dev/rpmsg*

# 查看内核日志
dmesg | grep -i rpmsg
```

## 常见问题

**Q: R5 固件无法启动**
```
remote processor 51400000.r5f: Boot failed: -22
```
A: 检查固件路径是否正确，查看设备树配置

**Q: RPMsg 端点创建失败**
```
Failed to create rpmsg endpoint
```
A: 确保 openamp 库版本匹配，检查 virtio 设备配置

**Q: FPGA 无响应**
```
FPGA timeout after 5000 ms
```
A: 检查 FPGA 基地址配置，验证 AXI4-Lite 连接

## 性能优化

1. **批量发送指令**: 一次 RPMsg 发送多条指令，减少中断开销
2. **共享内存**: 大数据块通过共享内存传输，RPMsg 仅发送通知
3. **零拷贝**: 使用 libmetal 的共享内存区域，避免数据复制

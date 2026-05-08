# FPGA Accelerator JNI Interface (v2)

## 设计原则

使用标准 OpenAMP/RPMsg 框架，**不需要手动映射物理地址**。

## OpenAMP/RPMsg 通信方式

```
┌──────────────────────────────────────────────────────────────┐
│                    A53 (Linux)                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Java Application                                      │ │
│  │    ↓                                                  │ │
│  │  HWAcceleratorJNI (libaccelerator_jni.so)            │ │
│  │    ↓                                                  │ │
│  │  RPMsg Char Device (/dev/rpmsgX)                      │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
                           ↕ RPMsg + IPI (通过 virtio)
┌──────────────────────────────────────────────────────────────┐
│                    R5 (Baremetal)                            │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  OpenAMP Library (libmetal + libopenamp)              │ │
│  │    ↓                                                  │ │
│  │  rpmsg-accelerator firmware                           │ │
│  │    ↓                                                  │ │
│  │  FPGA Driver (AXI4-Lite)                             │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

## 代码结构

```
linux_echo_test/
├── accelerator_jni_v2.cpp       # JNI 实现 (使用 RPMsg)
├── libaccelerator_jni_v2.so     # 编译输出
└── README_JNI_V2.md             # 本文档

r5_firmware/
├── rpmsg-accelerator-v2.c       # R5 固件 (使用 OpenAMP)
├── fpga_driver.c                 # FPGA 驱动
├── fpga_driver.h
└── README_R5_V2.md              # 固件说明
```

## 关键特性

### 1. 不需要 /dev/mem 映射
- RPMsg 自动管理共享内存
- 使用标准 `write()/read()` 系统调用

### 2. 自动 IPI 中断
- `write()` 到 `/dev/rpmsgX` 自动触发 IPI
- 无需手动操作中断控制器

### 3. 两种数据传输方式

**方式 A: 小批量 - 直接 RPMsg 发送**
```c
// 适合单次发送 < 10 条指令
InstructionStruct inst[4];
write(ept_fd, inst, sizeof(inst));  // 自动触发 IPI
```

**方式 B: 大批量 - 共享内存 + RPMsg 通知**
```c
// 适合大批量指令或大块数据
memcpy(shm_base, instructions, size);
uint32_t notify = 1;
write(ept_fd, &notify, sizeof(notify));  // 通知 R5 读取共享内存
```

## 编译

```bash
cd linux_echo_test
gcc -shared -fPIC -O2 -Wall \
    -I${JAVA_HOME}/include \
    -I${JAVA_HOME}/include/linux \
    accelerator_jni_v2.cpp \
    -o libaccelerator_jni_v2.so
```

## 运行

```bash
# 1. 确保 RPMsg 设备可用
ls -la /dev/rpmsg*

# 2. 加载 JNI 库
export LD_LIBRARY_PATH=$(pwd):$LD_LIBRARY_PATH

# 3. 运行 Java 应用
java -cp ... YourClass
```

## 依赖

- Linux Kernel with RPMsg support
- rpmsg_char kernel module (`modprobe rpmsg_char`)
- Java JDK 11+

## 权限

普通用户即可访问 `/dev/rpmsgX`，不需要 root 权限。

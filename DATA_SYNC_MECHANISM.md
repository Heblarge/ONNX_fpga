# A53-R5-FPGA 数据同步机制

## 架构概述

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           A53 (Linux - 用户空间)                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Java 应用                                                             │    │
│  │  ┌──────────────┐    ┌──────────────┐    ┌──────────────────────┐   │    │
│  │  │ SharedMemory │    │ InstJavaTODO │    │ HWAcceleratorJNI     │   │    │
│  │  │ Pool         │◄──►│ (指令格式)   │◄──►│ (JNI桥接)            │   │    │
│  │  └──────────────┘    └──────────────┘    └──────────┬───────────┘   │    │
│  │                                                      │               │    │
│  │                              ┌───────────────────────▼──────────────┐│    │
│  │                              │ accelerator_jni.cpp                  ││    │
│  │                              │  - /dev/mem mmap (物理内存映射)      ││    │
│  │                              │  - ARM dcache_flush/invalidate       ││    │
│  │                              │  - 缓冲区状态标志读写                ││    │
│  │                              └───────────────────────┬──────────────┘│    │
│  └──────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                  │
                                  │ RPMsg (指令) + IPI
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          R5 (OpenAMP/libmetal)                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ rpmsg-accelerator.c                                                  │    │
│  │  ┌────────────────────────────────────────────────────────────────┐ │    │
│  │  │ 缓冲区状态管理                                                  │ │    │
│  │  │  - read_buffer_status()   [CHECK: READY]                       │ │    │
│  │  │  - write_buffer_status()  [SET: BUSY → DONE]                   │ │    │
│  │  └────────────────────────────────────────────────────────────────┘ │    │
│  │                          │                                           │    │
│  │                          ▼                                           │    │
│  │  ┌────────────────────────────────────────────────────────────────┐ │    │
│  │  │ DataMover DMA 配置                                             │ │    │
│  │  │  - 直接使用物理地址进行 DMA 传输                                │ │    │
│  │  │  - 共享内存 → FPGA SRAM                                        │ │    │
│  │  └────────────────────────────────────────────────────────────────┘ │    │
│  └──────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                  │
                                  │ DMA
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              FPGA                                            │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────────────────┐  │
│  │  sdpramA     │      │  sdpramB     │      │  MatrixMul Array         │  │
│  │  (输入 A)    │ ──→  │  (输入 B)    │  ──→ │  (systolic: 32x32)       │  │
│  └──────────────┘      └──────────────┘      │  → Cache Controller       │  │
│                                                  → sdpramZ (输出)         │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 缓冲区状态机

```
┌─────────┐  1. A53写入数据    ┌─────────┐
│  FREE   │ ──────────────────► │  READY  │
└─────────┘    + dcache_flush   └─────────┘
    ▲                              │
    │                              │ 2. RPMsg指令 (IPI)
    │                              ▼
    │                         ┌─────────┐
    │                         │  BUSY   │
    │                         └─────────┘
    │                              │
    │                              │ 3. R5处理完成
    │                              ▼
    │                         ┌─────────┐
    │                         │  DONE   │
    │                         └─────────┘
    │                              │
    │                              │ 4. A53读取结果
    │                              │    + dcache_invalidate
    └──────────────────────────────┘
```

## 缓冲区布局

每个 buffer (640KB) 的布局：
```
+-------------------+
| Status (4 bytes)  │ ← 状态标志位 (FREE/READY/BUSY/DONE)
+-------------------+
| Reserved (60B)    │ ← 对齐到 64 字节边界
+-------------------+
|                   │
|  Data Payload     │ ← 实际数据 (矩阵张量)
|  (640KB - 64B)    │
|                   │
+-------------------+
```

## ARM Cache 同步机制

### A53 侧 (JNI)
```cpp
// 数据写入后刷新到内存
static void dcache_flush(void* addr, size_t size) {
    // 按 64 字节 cache line 对齐
    for (each cache line) {
        __asm__ volatile("dc cvac, %0" :: "r"(line) : "memory");
    }
    __asm__ volatile("dmb sy" ::: "memory");  // 数据同步屏障
}

// 读取数据前失效缓存
static void dcache_invalidate(void* addr, size_t size) {
    for (each cache line) {
        __asm__ volatile("dc ivac, %0" :: "r"(line) : "memory");
    }
    __asm__ volatile("dmb sy" ::: "memory");
}
```

### R5 侧 (Xil_ API)
```c
// DMA 读取前失效缓存 (确保读取 A53 写入的最新数据)
Xil_DCacheInvalidateRange(phys_addr, size);

// DMA 写入后刷新缓存 (确保 A53 能看到 R5 写入的数据)
Xil_DCacheFlushRange(phys_addr, size);
```

## 通信流程

### 完整执行流程
```
1. A53 Java: 分配 buffer → 写入数据到共享内存
2. A53 JNI: dcache_flush() + set_buffer_status(READY)
3. A53 JNI: 通过 RPMsg 发送指令 (包含 bufferId)
4. 内核: RPMsg write() → 触发 IPI → R5
5. R5: 收到 RPMsg → read_buffer_status() 检查 READY
6. R5: write_buffer_status(BUSY)
7. R5: 配置 DataMover DMA → 从共享内存读取数据
8. R5: 触发 FPGA 计算
9. R5: DataMover DMA → 写回结果到共享内存
10. R5: Xil_DCacheFlushRange() + write_buffer_status(DONE)
11. A53 JNI: wait_for_buffer_status() 检测 DONE
12. A53 JNI: dcache_invalidate() → 读取结果
13. A53 Java: 处理结果
```

## 关键改进

### 1. Cache 同步修复
**之前**: `__builtin___clear_cache()` (仅用于指令缓存)
**现在**: ARM 数据缓存操作 `dc cvac` / `dc ivac`

### 2. 状态标志同步
**之前**: 无明确同步点，依赖 RPMsg 完成通知
**现在**: 每个缓冲区有独立状态标志，可直接检查

### 3. 等待机制
**之前**: `poll()` 轮询 RPMsg (10ms 间隔)
**现在**: `wait_for_buffer_status()` 直接检查共享内存状态 (1ms 间隔)

## 配置参数

| 参数 | 值 | 说明 |
|------|-----|------|
| 共享内存块数 | 4 | 每个 Block 10MB |
| Buffer数/Block | 16 | 每个 Buffer 640KB |
| 总Buffer数 | 64 | 40MB 总共享内存 |
| Cache Line大小 | 64 字节 | ARMv8 标准 |
| RPMsg 缓冲区 | 512B | 指令传输 |
| 状态检查间隔 | 1ms | JNI 侧轮询 |
| 默认超时 | 5000ms | 5秒 |

## 编译和部署

### JNI 库编译
```bash
cd linux_echo_test
g++ -shared -fPIC -o libaccelerator_jni.so \
    -I${JAVA_HOME}/include \
    -I${JAVA_HOME}/include/linux \
    accelerator_jni.cpp rpmsg_comm.c -lpthread
```

### R5 固件编译
```bash
cd r5_firmware
# 使用 Vitis/Vivado SDK 编译
# 需要链接 libmetal 和 libopen_amp
```

## 注意事项

1. **物理地址直接访问**: R5 使用物理地址配置 DataMover，确保地址正确
2. **Cache 一致性**: 每次 DMA 传输前后都要进行 cache 操作
3. **状态标志对齐**: 状态标志位于 buffer 起始位置，占用前 4 字节
4. **数据偏移**: 实际数据从 buffer 偏移 64 字节开始（避开状态标志）

# StrideDma 用户手册

## 目录

1. [概述](#1-概述)
2. [架构与接口](#2-架构与接口)
3. [寄存器映射](#3-寄存器映射)
4. [工作原理](#4-工作原理)
5. [FPGA 硬件集成指南](#5-fpga-硬件集成指南)
6. [Vitis/Bare-metal 软件编程指南](#6-vitisbare-metal-软件编程指南)
7. [配置开销优化](#7-配置开销优化)
8. [边缘 Tile 自动裁剪](#8-边缘-tile-自动裁剪)
9. [AXI 突发约束与 4KB 边界](#9-axi-突发约束与-4kb-边界)
10. [设计参数](#10-设计参数)
---

## 1. 概述

**StrideDma** 是一个专为矩阵加速器设计的 DMA 引擎，具有以下核心特性：

- **矩阵分块**：软件配置大矩阵的形状、分块尺寸和当前 tile 的位置，硬件自动计算 DDR 地址、stride 和实际传输尺寸
- **自动边缘裁剪**：当矩阵维度不能被 tile 尺寸整除时，硬件自动收缩边缘 tile 的实际传输尺寸
- **双向传输**：支持 DDR → Local（提取 tile）和 Local → DDR（写回/拼合 tile）
- **4KB 突发分割**：自动处理 AXI 4KB 地址边界限制，将大行分割为多个子突发
- **降低配置开销**：寄存器持久化，重复传输同矩阵时每 tile 仅需 3 次 AXI-Lite 写

```
┌──────────────────────────────────────────────────────────┐
│                      StrideDma                           │
│                                                          │
│  AXI4-Lite Slave ◄── Software (Mat Shape/Tile Location)  │
│       (ctrl)                                             │
│                     ┌───────────────┐                    │
│  AXI4 Master ◄──────┤    FSM +      ├──────► AXI4 Master │
│    (mDdr)           │address compute│          (mLocal)  │
│   to DDR            │edge cut       │          to BRAM   │
│                     │burst spilit   │                    │
│                     └───────────────┘                    │
│                             └──────────────► INTR        │
└──────────────────────────────────────────────────────────┘
```

---

## 2. 架构与接口

### 2.1 端口列表

| 端口名 | 方向 | 协议 | 说明 |
|---------|------|------|------|
| `mDdr` | Master | AXI4 | 连接 DDR 存储控制器（HBM/DDR4/AXI Interconnect） |
| `mLocal` | Master | AXI4 | 连接本地 SDPRAM / BRAM（通过 AxiBramCtrl） |
| `ctrl` | Slave | AXI4-Lite | 软件配置和状态读取 |
| `intr` | Output | 电平 | 传输完成中断（写 INTR_CLR 清除） |

### 2.2 典型连接拓扑

```
 DDR/HBM Controller     Local SDPRAM/BRAM
       ▲                        ▲
       │ AXI4                   │ AXI4
  ┌────┴────────────────────────┴─────┐
  │              StrideDma            │
  │  (mDdr)                 (mLocal)  │
  └────┬────────────────────────┬─────┘
       │ AXI4-Lite (ctrl)       │ intr
  ┌────┴────────────────────────┴────┐
  │        PS / AXI Interconnect     │
  └──────────────────────────────────┘
```

---

## 3. 寄存器映射

### 3.1 寄存器总表

地址宽度：8 位（256 字节寄存器空间），数据宽度：32 位。

寄存器偏移由 `StrideDmaConfig.REG_*` 常量定义，支持 32/64 位 DDR 地址自动适配。
当 `ddrAddrWidth > 32` 时，`MAT_BASE` 在 32 位 AXI-Lite 总线上自动跨越多个 word，
后续寄存器偏移相应后移。

#### ddrAddrWidth = 32（默认，向后兼容）

| 偏移 | 名称 | 读/写 | 默认值 | 说明 |
|------|------|-------|--------|------|
| 0x00 | CTRL | R/W | 0x0 | 控制寄存器 |
| 0x04 | STATUS | R | 0x0 | 状态寄存器 |
| 0x08 | INTR_CLR | W | - | 中断清除 |
| 0x0C | MAT_BASE | R/W | 0x0 | 大矩阵 DDR 基地址（32 位，占 1 word） |
| 0x10 | LOCAL_BASE | R/W | 0x0 | BRAM 本地基地址 |
| 0x14 | MAT_COLS | R/W | 0x0 | 矩阵总列数（列 = beat 数） |
| 0x18 | MAT_ROWS | R/W | 0x0 | 矩阵总行数 |
| 0x1C | TILE_COLS | R/W | 0x0 | 分块标称宽度（beat 数） |
| 0x20 | TILE_ROWS | R/W | 0x0 | 分块标称高度 |
| 0x24 | TILE_COL_POS | R/W | 0x0 | 当前 tile 列起始位置（beat 数） |
| 0x28 | TILE_ROW_POS | R/W | 0x0 | 当前 tile 行起始位置 |
| 0x2C | ACTUAL_COLS | R | 0x0 | 实际传输宽度（硬件计算，只读） |
| 0x30 | ACTUAL_ROWS | R | 0x0 | 实际传输高度（硬件计算，只读） |

#### ddrAddrWidth = 64（64 位地址扩展）

| 偏移 | 名称 | 读/写 | 默认值 | 说明 |
|------|------|-------|--------|------|
| 0x00 | CTRL | R/W | 0x0 | 控制寄存器 |
| 0x04 | STATUS | R | 0x0 | 状态寄存器 |
| 0x08 | INTR_CLR | W | - | 中断清除 |
| 0x0C | MAT_BASE_LO | R/W | 0x0 | DDR 基地址低 32 位 |
| 0x10 | MAT_BASE_HI | R/W | 0x0 | DDR 基地址高 32 位 |
| 0x14 | LOCAL_BASE | R/W | 0x0 | BRAM 本地基地址 |
| 0x18 | MAT_COLS | R/W | 0x0 | 矩阵总列数（列 = beat 数） |
| 0x1C | MAT_ROWS | R/W | 0x0 | 矩阵总行数 |
| 0x20 | TILE_COLS | R/W | 0x0 | 分块标称宽度（beat 数） |
| 0x24 | TILE_ROWS | R/W | 0x0 | 分块标称高度 |
| 0x28 | TILE_COL_POS | R/W | 0x0 | 当前 tile 列起始位置（beat 数） |
| 0x2C | TILE_ROW_POS | R/W | 0x0 | 当前 tile 行起始位置 |
| 0x30 | ACTUAL_COLS | R | 0x0 | 实际传输宽度（硬件计算，只读） |
| 0x34 | ACTUAL_ROWS | R | 0x0 | 实际传输高度（硬件计算，只读） |

> **注意**：在 Scala/SpinalHDL 代码中，寄存器偏移统一使用 `StrideDmaConfig.REG_*` 常量获取，
> 无需关心具体数值。硬件生成时自动根据 `ddrAddrWidth` 选择正确的偏移。

### 3.2 CTRL 寄存器 (0x00) 位域

| Bit | 名称 | 类型 | 说明 |
|-----|------|------|------|
| [0] | START | W1S | 写 1 触发传输启动（自动清零），busy 时写入无效 |
| [1] | DIRECTION | R/W | 0 = DDR → Local（提取 tile），1 = Local → DDR（写回 tile） |
| [31:2] | - | - | 保留 |

### 3.3 STATUS 寄存器 (0x04) 位域

| Bit | 名称 | 类型 | 说明 |
|-----|------|------|------|
| [0] | BUSY | R | 1 = DMA 正在传输 |
| [1] | DONE/INTR | R | 1 = 传输完成（中断挂起），通过 INTR_CLR 清除 |
| [31:2] | - | - | 保留 |

### 3.4 尺寸单位说明

**关键概念**：所有"列"相关的寄存器（MAT_COLS、TILE_COLS、TILE_COL_POS、ACTUAL_COLS）的单位是 **beat**（一个数据总线宽度），不是字节。

- 默认配置：`dataWidth = 256 bits = 32 bytes/beat`
- 例如：一个 32768 字节宽的矩阵行 = 32768 / 32 = **1024 beats**
- 行相关的寄存器（MAT_ROWS、TILE_ROWS、TILE_ROW_POS、ACTUAL_ROWS）单位是 **行数**

---

## 4. 工作原理

### 4.1 硬件自动计算

当 START 被触发时，硬件在 **COMPUTE 状态**（1 个时钟周期）内自动计算：

```
actualCols = min(TILE_COLS, MAT_COLS - TILE_COL_POS)    // 边缘裁剪
actualRows = min(TILE_ROWS, MAT_ROWS - TILE_ROW_POS)    // 边缘裁剪

ddrAddr    = MAT_BASE + (TILE_ROW_POS × MAT_COLS + TILE_COL_POS) × bytesPerBeat
ddrStride  = MAT_COLS × bytesPerBeat
```

软件不需要自行计算地址和 stride。

### 4.2 传输状态机流程

```
IDLE ──start──► COMPUTE ──► ROW_START ──► (DDR_RD/WR 子状态) ──► ROW_DONE ──┐
                                ▲                                          │
                                └──────────────────────────────────────────┘
                                (rowIdx < actualRows)

                            ROW_START ──(rowIdx >= actualRows)──► DONE ──► IDLE
```

### 4.3 DDR 内存布局示例

```
    col 0       col T     col 2T    col 3T   col N-1
    ↓           ↓         ↓         ↓         ↓
    ┌───────────┬─────────┬─────────┬────────┐  row 0
    │ tile(0,0) │tile(0,1)│tile(0,2)│t(0,3)  │
    │  T × T    │  T × T  │  T × T  │ edge×T │
    ├───────────┼─────────┼─────────┼────────┤  row T
    │ tile(1,0) │tile(1,1)│tile(1,2)│t(1,3)  │
    │  T × T    │  T × T  │  T × T  │ edge×T │
    ├───────────┼─────────┼─────────┼────────┤  row 2T
    │ tile(2,0) │tile(2,1)│tile(2,2)│t(2,3)  │
    │  T×edge   │ T×edge  │ T×edge  │edge²   │
    └───────────┴─────────┴─────────┴────────┘  row N-1

    当 N 不能被 T 整除时，右侧和底部的 tile 会被自动裁剪：
    edge = N - (tilesPerDim-1) × T
```

### 4.4 BRAM 中的 Tile 布局（紧凑存储）

DMA 在 BRAM 中使用**紧凑行主序**布局：

```
BRAM 地址 (words):
  行 0: [0, 1, 2, ..., actualCols-1]
  行 1: [actualCols, actualCols+1, ..., 2*actualCols-1]
  ...
  行 r: [r*actualCols + 0, r*actualCols + 1, ..., r*actualCols + actualCols-1]
```

注意：边缘 tile 在 BRAM 中的布局是紧凑的（没有间隙），actual 尺寸小于标称尺寸。

---

## 5. FPGA 硬件集成指南

### 5.1 SpinalHDL 例化

```scala
import DMA._

// 32 位地址（默认，支持 4 GB）
val dmaCfg32 = StrideDmaConfig(
  ddrAddrWidth   = 32,       // DDR 地址宽度
  localAddrWidth = 20,       // BRAM 地址宽度 (1 MB)
  dataWidth      = 256,      // 数据总线宽度
  maxBurstLen    = 256       // AXI4 最大 burst
)

// 64 位地址（支持 >4 GB，如 HBM/大容量 DDR）
val dmaCfg64 = StrideDmaConfig(
  ddrAddrWidth   = 64,       // 64 位地址
  localAddrWidth = 20,
  dataWidth      = 256,
  maxBurstLen    = 256
)

// 获取寄存器偏移（自动适配地址位宽）
println(s"REG_MAT_BASE   = 0x${dmaCfg64.REG_MAT_BASE.toHexString}")
println(s"REG_LOCAL_BASE = 0x${dmaCfg64.REG_LOCAL_BASE.toHexString}")

val dma = StrideDma(dmaCfg64)  // 或 dmaCfg32
```

### 5.2 系统级连接（SpinalHDL 示例）

```scala
// 1. DDR 侧：连接 AXI Interconnect 或直接连接 DDR 控制器
axiInterconnect.io.slaves(0) <> dma.io.mDdr

// 2. Local 侧：连接 AXI BRAM Controller + SDPRAM
val bramCtrl = AxiBramCtrl(AxiBramCtrlConfig(
  axiConfig    = dma.localAxiCfg,
  memAddrWidth = dmaCfg.localAddrWidth
))
bramCtrl.io.axi <> dma.io.mLocal

val sdpram = Sdpram(addrWidth = dmaCfg.localAddrWidth, dataWidth = 256)
sdpram.io.read  <> bramCtrl.io.memRead
sdpram.io.write <> bramCtrl.io.memWrite

// 加速器可以使用 SDPRAM 的另一个端口直接访问 tile 数据
accelerator.io.tileData <> sdpram.io.port_b

// 3. 控制侧：连接处理器的 AXI-Lite 端口
processor.io.axiLite <> dma.io.ctrl

// 4. 中断
processor.io.irq(0) := dma.io.intr
```

### 5.3 Vivado Block Design 集成

如果将 StrideDma 生成为 Verilog IP 并加入 Vivado Block Design：

1. **生成 Verilog**：
   ```scala
   SpinalVerilog(StrideDma(StrideDmaConfig()))
   ```

2. **在 Block Design 中连接**：
   - `mDdr` → AXI Interconnect → DDR4/HBM Controller
   - `mLocal` → AXI BRAM Controller → Block RAM Generator (True Dual Port)
   - `ctrl` → AXI Interconnect (Lite) → Zynq PS AXI GP 端口
   - `intr` → Zynq PS 或 AXI Interrupt Controller

3. **地址映射**（在 Vivado Address Editor 中）：
   - 为 `ctrl` 分配基地址（例如 0x40000000），大小 256 字节
   - 为 `mDdr` 分配 DDR 地址范围（例如 0x00000000 - 0xFFFFFFFF）
   - `mLocal` 连接到本地 BRAM，为其分配地址范围

### 5.4 时钟与复位

- 所有端口共享同一时钟域
- 支持同步复位（SpinalHDL 默认）
- 推荐时钟频率：100-300 MHz（取决于 DDR 控制器）

---

## 6. Vitis/Bare-metal 软件编程指南

### 6.1 寄存器地址定义 (C/C++)

#### 32 位地址模式（默认）

```c
#include <stdint.h>

// 假设 DMA 基地址为 0x40000000（由 Vivado Address Editor 分配）
#define DMA_BASE        0x40000000

#define DMA_CTRL        (DMA_BASE + 0x00)
#define DMA_STATUS      (DMA_BASE + 0x04)
#define DMA_INTR_CLR    (DMA_BASE + 0x08)
#define DMA_MAT_BASE    (DMA_BASE + 0x0C)
#define DMA_LOCAL_BASE  (DMA_BASE + 0x10)
#define DMA_MAT_COLS    (DMA_BASE + 0x14)
#define DMA_MAT_ROWS    (DMA_BASE + 0x18)
#define DMA_TILE_COLS   (DMA_BASE + 0x1C)
#define DMA_TILE_ROWS   (DMA_BASE + 0x20)
#define DMA_TILE_COL_POS (DMA_BASE + 0x24)
#define DMA_TILE_ROW_POS (DMA_BASE + 0x28)
#define DMA_ACTUAL_COLS (DMA_BASE + 0x2C)   // 只读
#define DMA_ACTUAL_ROWS (DMA_BASE + 0x30)   // 只读

// CTRL 位域
#define DMA_CTRL_START      (1 << 0)
#define DMA_CTRL_DIR_MASK   (1 << 1)
#define DMA_DIR_DDR2LOCAL   0           // DDR → Local (提取)
#define DMA_DIR_LOCAL2DDR   (1 << 1)    // Local → DDR (写回)

// STATUS 位域
#define DMA_STATUS_BUSY     (1 << 0)
#define DMA_STATUS_DONE     (1 << 1)

// 寄存器读写宏
#define REG_WR(addr, val)  (*(volatile uint32_t *)(addr) = (val))
#define REG_RD(addr)       (*(volatile uint32_t *)(addr))
```

#### 64 位地址模式（ddrAddrWidth=64）

```c
// 64 位模式下 MAT_BASE 占 2 个 word，后续寄存器偏移 +4
#define DMA_BASE           0x40000000

#define DMA_CTRL           (DMA_BASE + 0x00)
#define DMA_STATUS         (DMA_BASE + 0x04)
#define DMA_INTR_CLR       (DMA_BASE + 0x08)
#define DMA_MAT_BASE_LO    (DMA_BASE + 0x0C)  // 基地址低 32 位
#define DMA_MAT_BASE_HI    (DMA_BASE + 0x10)  // 基地址高 32 位
#define DMA_LOCAL_BASE     (DMA_BASE + 0x14)
#define DMA_MAT_COLS       (DMA_BASE + 0x18)
#define DMA_MAT_ROWS       (DMA_BASE + 0x1C)
#define DMA_TILE_COLS      (DMA_BASE + 0x20)
#define DMA_TILE_ROWS      (DMA_BASE + 0x24)
#define DMA_TILE_COL_POS   (DMA_BASE + 0x28)
#define DMA_TILE_ROW_POS   (DMA_BASE + 0x2C)
#define DMA_ACTUAL_COLS    (DMA_BASE + 0x30)   // 只读
#define DMA_ACTUAL_ROWS    (DMA_BASE + 0x34)   // 只读

// 设置 64 位矩阵基地址
void dma_set_mat_base_64(uint64_t addr) {
    REG_WR(DMA_MAT_BASE_LO, (uint32_t)(addr & 0xFFFFFFFF));
    REG_WR(DMA_MAT_BASE_HI, (uint32_t)(addr >> 32));
}
```

### 6.2 基本 API 函数

```c
// 一次性设置矩阵参数（切换矩阵时调用）
// 32 位地址版本
void dma_setup_matrix(uint32_t mat_base, uint32_t local_base,
                      uint16_t mat_cols, uint16_t mat_rows,
                      uint16_t tile_cols, uint16_t tile_rows) {
    REG_WR(DMA_MAT_BASE,   mat_base);
    REG_WR(DMA_LOCAL_BASE,  local_base);
    REG_WR(DMA_MAT_COLS,   mat_cols);
    REG_WR(DMA_MAT_ROWS,   mat_rows);
    REG_WR(DMA_TILE_COLS,  tile_cols);
    REG_WR(DMA_TILE_ROWS,  tile_rows);
}

// 64 位地址版本
void dma_setup_matrix_64(uint64_t mat_base, uint32_t local_base,
                         uint16_t mat_cols, uint16_t mat_rows,
                         uint16_t tile_cols, uint16_t tile_rows) {
    dma_set_mat_base_64(mat_base);
    REG_WR(DMA_LOCAL_BASE,  local_base);
    REG_WR(DMA_MAT_COLS,   mat_cols);
    REG_WR(DMA_MAT_ROWS,   mat_rows);
    REG_WR(DMA_TILE_COLS,  tile_cols);
    REG_WR(DMA_TILE_ROWS,  tile_rows);
}

// 启动 tile 传输（每 tile 仅 3 次寄存器写）
void dma_start_tile(uint16_t tile_col_pos, uint16_t tile_row_pos,
                    uint32_t direction) {
    REG_WR(DMA_TILE_COL_POS, tile_col_pos);
    REG_WR(DMA_TILE_ROW_POS, tile_row_pos);
    REG_WR(DMA_CTRL, DMA_CTRL_START | direction);
}

// 轮询等待 DMA 完成
void dma_wait_done(void) {
    while (!(REG_RD(DMA_STATUS) & DMA_STATUS_DONE))
        ;
    REG_WR(DMA_INTR_CLR, 1);  // 清除中断
}

// 读取硬件计算的实际 tile 尺寸（可选，用于调试）
void dma_get_actual_size(uint16_t *actual_cols, uint16_t *actual_rows) {
    *actual_cols = (uint16_t)REG_RD(DMA_ACTUAL_COLS);
    *actual_rows = (uint16_t)REG_RD(DMA_ACTUAL_ROWS);
}
```

### 6.3 完整使用示例：矩阵分块搬运

```c
// =====================================================================
// 示例：将 1000×1000 矩阵从 DDR 分块提取到 BRAM 进行计算，再写回
// 矩阵不能被 256 整除 → 边缘 tile 由硬件自动处理
// =====================================================================

#define MAT_N     1000    // 矩阵维度 (beats)
#define TILE_T    256     // 分块尺寸 (beats)
#define SRC_DDR   0x00000000
#define DST_DDR   0x10000000
#define BRAM_BASE 0x00000000   // BRAM 本地地址

void process_matrix(void) {
    int tiles_per_dim = (MAT_N + TILE_T - 1) / TILE_T;  // = 4

    // ★ 一次性配置矩阵参数（6 次寄存器写）
    dma_setup_matrix(SRC_DDR, BRAM_BASE, MAT_N, MAT_N, TILE_T, TILE_T);

    for (int tr = 0; tr < tiles_per_dim; tr++) {
        for (int tc = 0; tc < tiles_per_dim; tc++) {
            int col_pos = tc * TILE_T;
            int row_pos = tr * TILE_T;

            // Step 1: DDR → BRAM（★ 每 tile 仅 3 次寄存器写）
            dma_start_tile(col_pos, row_pos, DMA_DIR_DDR2LOCAL);
            dma_wait_done();

            // 可选：查看硬件裁剪的实际尺寸
            uint16_t a_cols, a_rows;
            dma_get_actual_size(&a_cols, &a_rows);
            // a_cols 和 a_rows 会自动反映边缘裁剪

            // Step 2: 加速器在 BRAM 中处理 tile 数据
            accelerator_process_tile(a_cols, a_rows);

            // Step 3: BRAM → DDR dst
            //   只需更新 matBase（切换到目标矩阵），其他参数不变
            REG_WR(DMA_MAT_BASE, DST_DDR);
            dma_start_tile(col_pos, row_pos, DMA_DIR_LOCAL2DDR);
            dma_wait_done();

            // 切回源矩阵（为下一个 tile 做准备）
            REG_WR(DMA_MAT_BASE, SRC_DDR);
        }
    }
}
```

### 6.4 中断驱动模式（替代轮询）

```c
// 中断服务函数
void dma_isr(void) {
    REG_WR(DMA_INTR_CLR, 1);
    dma_done_flag = 1;
}

// 使用中断模式
void dma_start_tile_irq(uint16_t col_pos, uint16_t row_pos,
                         uint32_t direction) {
    dma_done_flag = 0;
    dma_start_tile(col_pos, row_pos, direction);
    // CPU 可以在等待 DMA 完成时做其他工作
    while (!dma_done_flag) {
        // 可以做其他计算，或者 WFI 进入低功耗等待
        __asm__("wfi");
    }
}
```

### 6.5 Vitis HLS 用户集成提示

如果你的加速器使用 Vitis HLS 实现，DMA 可以与 HLS IP 协同工作：

1. StrideDma 负责 DDR ↔ BRAM 的数据搬运
2. HLS IP 通过 BRAM 的另一个端口（或 AXI4 接口）访问 tile 数据
3. 控制流由 Zynq PS上的软件（bare-metal 或 Linux 驱动）编排

---

## 7. 配置开销优化

### 7.1 寄存器持久性

所有寄存器都是**持久的**（硬件 `Reg`），写入后一直保持，直到被新值覆盖或复位。因此：

- **不需要每次传输都重配所有寄存器**
- 只需要写入**发生变化的寄存器**

### 7.2 配置开销分析

| 场景 | 需要写的寄存器 | AXI-Lite 写次数 |
|------|---------------|-----------------|
| 首次配置（全部） | MAT_BASE + LOCAL_BASE + MAT_COLS/ROWS + TILE_COLS/ROWS + POS + CTRL | **9 次** |
| 同矩阵，换 tile（最常见） | TILE_COL_POS + TILE_ROW_POS + CTRL | **3 次** |
| 同矩阵，换方向 | CTRL（方向+启动同时写） | **3 次**（含位置） |
| 切换矩阵基地址 | MAT_BASE + POS + CTRL | **4 次** |
| 切换到完全不同的矩阵 | 全部重配 | **9 次** |

### 7.3 每次 AXI-Lite 写的周期开销

一次 AXI-Lite 写操作需要约 **3-5 个时钟周期**（AW + W 握手 + B 响应）。

因此：
- 全配（9 次）≈ 27-45 周期
- 快速模式（3 次）≈ 9-15 周期

对比 DMA 传输本身（256×256 tile ≈ 数万周期），**配置开销可忽略不计**（< 0.1%）。

### 7.4 推荐使用模式

```
┌────────────────────────────────────────────────────────
│ dma_setup_matrix(...)          // 6 次写，仅需 1 次    
│                                                        
│ for each tile:                                         
│   dma_start_tile(col, row, dir)  // 3 次写 ← 最小开销  
│   dma_wait_done()                                      
│   ... 处理 tile ...                                    
│                                                        
│ // 切换到另一个矩阵时才需要调用 dma_setup_matrix       
└────────────────────────────────────────────────────────
```

---

## 8. 边缘 Tile 自动裁剪

### 8.1 问题描述

当矩阵维度 `N` 不能被 tile 尺寸 `T` 整除时，最后一行/列的 tile 实际尺寸小于标称尺寸。

例如 `N=500, T=256`：

```
tilesPerDim = ceil(500/256) = 2

tile(0,0): 256×256  (完整)
tile(0,1): 244×256  (右边缘，列裁剪)
tile(1,0): 256×244  (底边缘，行裁剪)
tile(1,1): 244×244  (角落，双向裁剪)
```

### 8.2 硬件自动处理

软件**不需要手动计算边缘 tile 的实际尺寸**。只需设置：
- `TILE_COLS = T`（标称宽度）
- `TILE_ROWS = T`（标称高度）
- `TILE_COL_POS = tc × T`
- `TILE_ROW_POS = tr × T`

硬件会自动 clamp：
```
ACTUAL_COLS = min(TILE_COLS, MAT_COLS - TILE_COL_POS) = min(256, 500-256) = 244
ACTUAL_ROWS = min(TILE_ROWS, MAT_ROWS - TILE_ROW_POS) = min(256, 500-256) = 244
```

### 8.3 软件读取 ACTUAL 尺寸

传输完成后，软件可以读取 `ACTUAL_COLS` (0x2C) 和 `ACTUAL_ROWS` (0x30) 来获知实际传输了多大的 tile：

```c
dma_start_tile(256, 256, DMA_DIR_DDR2LOCAL);
dma_wait_done();

uint16_t actual_cols = REG_RD(DMA_ACTUAL_COLS);  // = 244
uint16_t actual_rows = REG_RD(DMA_ACTUAL_ROWS);  // = 244

// 加速器根据实际尺寸处理数据
process_tile(actual_cols, actual_rows);
```

---

## 9. AXI 突发约束与 4KB 边界

### 9.1 4KB 边界问题

AXI4 协议规定单次突发传输**不得跨越 4KB 地址边界**。对于 256-bit (32 bytes/beat) 数据总线：

```
maxBeatsPerBurst = min(maxBurstLen, 4096 / 32) = min(256, 128) = 128 beats
```

### 9.2 自动子突发分割

当单行数据超过 128 beats 时，DMA 自动将其分为多个子突发：

```
例如行宽 = 500 beats:
  子突发 1: 128 beats
  子突发 2: 128 beats
  子突发 3: 128 beats
  子突发 4: 116 beats (剩余)
```

这对软件完全透明，无需额外处理。

### 9.3 DataFifo 深度

内部 DataFifo 深度 = `maxBeatsPerBurst`（128），用于缓存一个完整子突发的数据。

---

## 10. 设计参数

### 10.1 StrideDmaConfig 参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `ddrAddrWidth` | 32 | DDR 地址宽度（位），32 位支持 4 GB，设为 64 支持 >4 GB |
| `localAddrWidth` | 20 | BRAM 地址宽度（位），20 位 = 1 MB |
| `dataWidth` | 256 | 数据总线宽度（位），与 DDR 控制器匹配 |
| `maxBurstLen` | 256 | AXI4 最大突发长度 |

### 10.2 派生参数

| 参数 | 计算公式 | 256-bit 示例 |
|------|----------|-------------|
| `bytesPerBeat` | dataWidth / 8 | 32 |
| `maxBeatsPerBurst` | min(maxBurstLen, 4096 / bytesPerBeat) | 128 |

### 10.3 资源占用估计

- 寄存器：约 256 位（10 个 16/32 位寄存器）
- FIFO：128 × 256 位 = 4 KB（可用 BRAM 或 LUT RAM 实现）
- 逻辑：FSM（13 状态） + 地址计算（1 个 16×16 乘法器 + 移位器）
- 总计：约 500-1000 LUT + 1 BRAM18K
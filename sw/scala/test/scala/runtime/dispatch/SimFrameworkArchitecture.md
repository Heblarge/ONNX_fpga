# LiveHPS 仿真框架完整架构文档

## 目录

1. [总体架构概览](#1-总体架构概览)
2. [Ramulator2 原理与集成](#2-ramulator2-原理与集成)
3. [SystemWrapper 硬件架构](#3-systemwrapper-硬件架构)
4. [DDR 数据初始化与读取](#4-ddr-数据初始化与读取)
5. [DMA 引擎与分块传输](#5-dma-引擎与分块传输)
6. [缓存控制器 (MatrixCacheController)](#6-缓存控制器-matrixcachecontroller)
7. [加速器核心数据通路](#7-加速器核心数据通路)
8. [完整数据流——五阶段生命周期](#8-完整数据流五阶段生命周期)
9. [波形可见性分析](#9-波形可见性分析)
10. [软件驱动层 (HwDrivers)](#10-软件驱动层-hwdrivers)
11. [SimContext——仿真线程架构](#11-simcontext仿真线程架构)
12. [DDR-Native vs Fallback 执行路径](#12-ddr-native-vs-fallback-执行路径)
13. [与 VerilatorDispatcher 的区别](#13-与-verilatordispatcher-的区别)
14. [关键源文件索引](#14-关键源文件索引)

---

## 1. 总体架构概览

整个仿真系统由三层组成：**Scala 软件驱动层**、**Verilator RTL 仿真层**、**Ramulator2 DDR 仿真层**。

```
+======================================================================+
|                        Scala SW Driver Layer                         |
|   SystemDispatcher / HybridDispatcher / SingleOpReplayTestSuite      |
|                                                                      |
|   +------------+  +----------+ +-----------+ +----------------+      |
|   | DdrDataDrv |  | DmaDriver| |CacheDriver| |InstructionDrv  |      |
|   +------+-----+  +----+-----+ +-----+-----+ +-------+--------+      |
|          |             |             |               |               |
|   +------+-------------+-------------+---------------+----------+    |
|   |                       SimContext                            |    |
|   |    (BlockingQueue cmd/result, background Verilator thread)  |    |
|   +----------------------------+--------------------------------+    |
+======================================================================+
                                 |
                                 | AXI4 / AXI4-Lite signal driving
                                 v
+======================================================================+
|                   Verilator RTL Simulation Layer                     |
|                                                                      |
|  +----------------------- SystemWrapper --------------------------+  |
|  |                                                                |  |
|  |   [DDR Access Path]                                            |  |
|  |                                                                |  |
|  |   hostAxi ---+                                                 |  |
|  |   dmaA.mDdr -+     +---------------+    +----------+           |  |
|  |   dmaB.mDdr -+---->| Axi4 Crossbar |--->| DRAMSim  |--->[DPI]  |  |
|  |   dmaZ.mDdr -+     |  (4M -> 1S)   |    |(AXI burst|           |  |
|  |                    +---------------+    | to beats)|           |  |
|  |                                         +----------+           |  |
|  |                                                                |  |
|  |   [Compute Datapath]                                           |  |
|  |                                                                |  |
|  |                 +-----------+     +--------+                   |  |
|  |                 | CacheCtrl |     |  Core  |                   |  |
|  |   +-------+     | +-------+ |     |        |                   |  |
|  |   | DMA_A |---->+-|BramA  |-+---->| Slicer |                   |  |
|  |   +-------+     | +-------+ |     |   |    |                   |  |
|  |   | DMA_B |---->+-|BramB  |-+---->| SysArr |                   |  |
|  |   +-------+     | +-------+ |     |   |    |                   |  |
|  |   | DMA_Z |<----+-|BramC  | |     | Activ  |                   |  |
|  |   +-------+     | +-------+ |     |   |    |                   |  |
|  |       |         +-----+-----+     |Collect.+--> write to BramC |  |
|  |       |           ^   |   ^       +---+----+                   |  |
|  |       |           |   |   |           |                        |  |
|  |       +-----> dmaDone | switch <------+                        |  |
|  |       |       (A,B,Z) | (A,B,C)                                |  |
|  |       |               v                                        |  |
|  |       |               +---globalIntr--+                        |  |
|  |       +-------------------------------+---> Micro Controller   |  |
|  |                                                                |  |
|  +----------------------------------------------------------------+  |
|                                                                      |
+======================================================================+
                                 |
                                 | DPI-C function calls
                                 v
+======================================================================+
|                 Ramulator2 DDR Simulation Layer (C++)                |
|                                                                      |
|   +--------------------------------------------------------------+   |
|   | DRAMSimDPIDriverRamulator2.cpp                               |   |
|   |                                                              |   |
|   |  memContent: std::map<addr, vector<svBitVecVal>>             |   |
|   |  (SW memory storage, NOT real DRAM chip)                     |   |
|   |                                                              |   |
|   |      +----------------+        +---------------------+       |   |
|   |      | GEM5 Frontend  |<------>| GenericDRAM         |       |   |
|   |      | (req dispatch) |        |  DDR4_8Gb_x8        |       |   |
|   |      +----------------+        |  DDR4_2400R timing  |       |   |
|   |                                |  FRFCFS Scheduler   |       |   |
|   |                                +---------------------+       |   |
|   +--------------------------------------------------------------+   |
|                                                                      |
+======================================================================+
```

**核心设计思想**：在 PC 上用 Verilator 编译 SpinalHDL 生成的 Verilog（完整 SoC），通过
DPI-C 桥接调用 ramulator2 C++ 库来精确模拟 DDR4 的时序行为。软件通过 `hostAxi` 端口
像真实 SoC 上的 CPU 一样读写 DDR，所有操作都是**周期精确**的。

---

## 2. Ramulator2 原理与集成

### 2.1 什么是 Ramulator2？

Ramulator2 是一个学术界广泛使用的 **DRAM 时序精确模拟器**（来自 ETH Zurich）。它不是一个
真实的 DDR 芯片，而是一个 C++ 库，**精确模拟 DDR 控制器 + DRAM 芯片的时序行为**：

- **DRAM 芯片时序**：tRCD、tRAS、tRP、tCL 等所有 DDR4 timing 参数
- **DDR 控制器调度**：FRFCFS（First-Ready First-Come-First-Served）调度策略
- **行策略**：OpenRowPolicy（行保持打开直到必须关闭）
- **刷新管理**：AllBank 刷新
- **地址映射**：RoBaRaCoCh（Row→Bank→Rank→Column→Channel）

**关键理解**：Ramulator2 模拟的是**时序**，不是真正的存储。真正的数据存储在 C++ 侧的
`std::map<addr_t, vector<svBitVecVal>> memContent` 中（一个普通的内存 map）。
Ramulator2 只负责回答"这个读/写请求需要多少个周期才能完成"。

### 2.2 配置文件 (ramulator_config.yaml)

```yaml
Frontend:
  impl: GEM5                    # request frontend type

MemorySystem:
  impl: GenericDRAM
  clock_ratio: 1                # DDR clk : sys clk ratio (1:1)

  DRAM:
    impl: DDR4
    org:
      preset: DDR4_8Gb_x8       # 8Gb DDR4 chip, x8 data width
      channel_width: 64          # channel width 64 bit
    timing:
      preset: DDR4_2400R         # DDR4-2400 timing params

  Controller:
    impl: Generic
    Scheduler:
      impl: FRFCFS               # First-Ready First-Come-First-Served
    RowPolicy:
      impl: OpenRowPolicy        # keep row open
    RefreshManager:
      impl: AllBank              # all-bank refresh
    plugins:
      - ControllerPlugin:
          impl: TraceRecorder
          path: ./trace/dram.trace  # DRAM access trace output

  AddrMapper:
    impl: RoBaRaCoCh             # address mapping policy
```

### 2.3 三层集成架构

```
Layer 1: SystemVerilog DPI BlackBox (DRAMSimDPIDriver.sv)
    | DPI-C function calls
Layer 2: C++ bridge (DRAMSimDPIDriverRamulator2.cpp)
    | ramulator2 API
Layer 3: Ramulator2 library (Frontend + MemorySystem)
```

#### 层级 1: DRAMSimDPIDriver.sv

这是一个 SystemVerilog 模块（100 行），作为 RTL 世界与 C++ 世界的桥梁：

```
Params: addressWidth=32, dataWidth=256, seed=0

Ports:
  dram_req  (Stream): valid/ready/addr/data/is_write  -> request channel
  dram_resp (Stream): valid/ready/addr/data/is_write  -> response channel

DPI-C functions:
  DRAMSimDPIDriverCreate(dataWidth, seed, obj)        -> init ramulator2 instance
  DRAMSimDPIDriverPushRequest(obj, addr, data, write) -> submit read/write request
  DRAMSimDPIDriverPopResponse(obj, addr, data, write) -> get completed response
  DRAMSimDPIDriverTick(obj)                           -> advance ramulator2 per cycle
```

**每个时钟上升沿**：
1. 尝试将 pending 请求推送给 ramulator2（如果满了就暂存）
2. 尝试从 ramulator2 弹出已完成的响应
3. 调用 `Tick()` 推进 ramulator2 一个周期

#### 层级 2: DRAMSimDPIDriverRamulator2.cpp

C++ 桥接层（~160 行），核心职责：

```cpp
class DRAMSimDPIDriver {
    // SW memory -- actual data storage
    std::map<addr_t, vector<svBitVecVal>> memContent;

    // ramulator2 components -- timing only
    Ramulator::IFrontEnd *frontend;
    Ramulator::IMemorySystem *memorysystem;

    // ordering: FRFCFS may complete OOO, reorder by seq num
    size_t nextReqSeqNum = 0;
    size_t nextRespSeqNum = 0;
    std::map<size_t, RequestContext> completedReqsMap;

    // max concurrent requests
    static constexpr size_t MAX_PENDING_REQS = 16;
};
```

**请求处理流程**：
```
pushRequest(addr, data, is_write)
  -> frontend->receive_external_requests(0, addr, 0, callback)
    -> ramulator2 internal queuing, scheduling, timing simulation
    -> on completion, callback fires:
        if (is_write) writeMemory(addr, data)  // store to memContent
        else          data = readMemory(addr)   // read from memContent
        completedReqsMap[seqNum] = response
```

**关键细节**：
- 未初始化的地址读取时，会用 `mt19937(seed)` 填充随机数据
- 最多 16 个并发请求（`MAX_PENDING_REQS`）
- 使用 `ordered_map` + 序列号保证响应按提交顺序返回

#### 层级 3: SpinalHDL 包装 (DRAMSim.scala)

将 ramulator2 的简单逐拍接口适配为标准 AXI4 协议：

```
AXI4 burst transaction
    | DRAMSim.fromAxi4()
    v
Per-beat DRAMSim requests

Breakdown:
  Write: AW(addr,len) + W(beat x N) -> one DRAMSim write req per W beat (addr auto-incr)
  Read:  AR(addr,len)               -> generate len+1 DRAMSim read reqs
                                    -> collect responses, assemble R channel (with last)

Arbitration: write-first (lowerFirst)
Outstanding read: arInfoFifo(depth=8) tracks totalBeats and id per AR burst
```

---

## 3. SystemWrapper 硬件架构

### 3.1 层次结构

```
SystemWrapper (cfg: SystemWrapperConfig)
|
+-- WrapForFPGA (core)              -- accelerator core
|   +-- Inst128_Wrapper             -- AXI-Lite -> 128-bit Stream
|   +-- Slicer                      -- input slicing + shift align
|   +-- SystolicArray2D_Wrapper     -- 8x8 systolic array
|   +-- Activation                  -- Relu/Exp/Log/Softplus
|   +-- Collector                   -- output collection
|
+-- MatrixCacheController           -- dual-bank ping-pong cache mgmt
|   +-- InputMatrixCacheController (A) -- CacheA ctrl
|   +-- InputMatrixCacheController (B) -- CacheB ctrl
|   +-- OutputMatrixCacheController(C) -- CacheC ctrl
|
+-- Sdpram x 3 (A, B, C)           -- physical BRAM
|
+-- StrideDma x 3 (A, B, Z)        -- matrix-tiling DMA engines
|
+-- AxiBramCtrl x 3                 -- AXI4 -> MemoryPort bridge
|
+-- DRAMSimDPIDriver                -- ramulator2 DPI BlackBox
|
+-- DRAMSim                         -- AXI4 -> per-beat DRAMSim adapter
|
+-- Axi4CrossbarFactory             -- 4 master -> 1 DDR slave arbiter
```

### 3.2 关键参数

| 参数 | 典型值 | 说明 |
|------|--------|------|
| `systolicArraySideNum` | 8 | 脉动阵列边长 → 8×8 |
| `elementWidth` | 24/32 | 数据元素位宽 |
| `intWidth` | 12 | 整数部分位宽 |
| `fracWidth` | 12 | 小数部分位宽 |
| `memDataWidth` | 256 | 8 × 32 bit = 256 bit 总线宽度 |
| `bytesPerBeat` | 32 | 每个 AXI beat 32 字节 |
| `cacheAddrWidth` | 14(可调) | 每 bank 深度 = 2^14 = 16K 行 |
| `ddrAddrWidth` | 32 | DDR 地址空间 4GB |
| `dmaMaxBurstLen` | 256 | DMA 最大突发长度 |

### 3.3 IO 端口

| 端口 | 类型 | 方向 | 用途 |
|------|------|------|------|
| `sAxi4LiteInst` | AXI4-Lite | slave | 128-bit 指令输入 |
| `sAxi4LiteCache` | AXI4-Lite | slave | 缓存 lifecycle/status/中断配置 |
| `ctrlDmaA/B/Z` | AXI4-Lite | slave | 3 个 DMA 控制寄存器 |
| `hostAxi` | AXI4 (full) | slave | **宿主 DDR 读写**（仿真专用） |
| `intrDmaA/B/Z` | Bool | output | 3 个 DMA 完成中断 |
| `globalIntr` | Bool | output | 计算完成中断 |

### 3.4 AXI Crossbar 连接

```
dmaA.mDdr --+
dmaB.mDdr --+     4 AXI4 master ports
dmaZ.mDdr --+--> Axi4CrossbarFactory --> ddrAxi --> DRAMSim --> DRAMSimDPIDriver
hostAxi ----+     (lowLatency=true)       (single slave port)
```

所有 4 个主端口共享同一个 DDR 地址空间 `[0, 2^32)`。Crossbar 自动管理
冲突仲裁和 ID 扩展（idWidth + 2 bits 用于区分主端口来源）。

---

## 4. DDR 数据初始化与读取

### 4.1 软件写入 DDR 的完整路径

当 Scala 软件调用 `ddrDriver.writeMatrix(ddrBase, matrix, rows, cols)` 时：

```
Step 1: Scala main thread
  ddrDriver.writeMatrix(addr, data)
    -> SimContext.submit(WriteMatrixToDdr(addr, matrix, rows, cols))
    -> cmdQueue.put(cmd)  // blocks until sim thread picks up

Step 2: Verilator sim thread (SimContext background thread)
  loop { cmd = cmdQueue.poll() }
    -> received WriteMatrixToDdr
    -> for (row <- 0 until rows; bc <- 0 until beatsPerRow):
        addr = ddrBase + (row * beatsPerRow + bc) * bytesPerBeat
        data = packMatrixBeat(matrix, row, bc)   // 8 x 32-bit elems -> 256-bit
        hostWriteBeat(dut, addr, data)

Step 3: hostWriteBeat AXI4 protocol driving
  set aw.valid=1, aw.addr=addr, aw.len=0 (single beat burst)
  set w.valid=1, w.data=256bit, w.last=1
  -> Verilator advances clock
  -> wait for aw.ready && w.ready sampled
  -> wait for b.valid (write response)

Step 4: RTL internal dataflow
  hostAxi.aw/w
    -> Axi4CrossbarFactory (arbitration pass)
    -> ddrAxi
    -> DRAMSim.fromAxi4():
        AW captures addr/len, generates one DRAMSim write req per W beat
    -> DRAMSimDPIDriver.sv:
        dram_req_valid -> DPI-C DRAMSimDPIDriverPushRequest()

Step 5: C++ ramulator2 layer
  pushRequest(addr, data, is_write=1)
    -> frontend->receive_external_requests(addr, callback)
    -> ramulator2 scheduling (FRFCFS) -> simulates DDR4 timing
    -> on completion, callback fires:
        writeMemory(addr, data)  // store to memContent map
    -> completedReqsMap[seqNum] = response

Step 6: Response return
  ramulator2 tick() then popResponse() gets completion ack
    -> DRAMSimDPIDriver.sv dram_resp_valid
    -> DRAMSim.fromAxi4() discards write resp (only B channel ack needed)
    -> Crossbar -> hostAxi.b.valid -> Scala gets write-done
```

### 4.2 软件从 DDR 读取的完整路径

```
Step 1: ddrDriver.readMatrix(addr, rows, cols)
  -> SimContext.submit(ReadMatrixFromDdr(...))

Step 2: hostReadBeat(dut, addr)
  set ar.valid=1, ar.addr=addr, ar.len=0
  -> wait for ar.ready
  -> wait for r.valid
  -> read r.data (256-bit BigInt)
  -> unpack to 8 x 32-bit signed integers

Step 3-6: symmetric to write path
  hostAxi.ar -> Crossbar -> DRAMSim -> DRAMSimDPIDriver -> DPI-C
  -> ramulator2 schedules read req -> readMemory(addr) from memContent
  -> response returns -> DRAMSim assembles R channel -> Crossbar -> hostAxi.r.data
```

### 4.3 数据在哪里？

**常见误解澄清**：

| 问题 | 答案 |
|------|------|
| 数据存在真实的 DDR 芯片里吗？ | **否**。存在 C++ 的 `std::map<addr, data>` 里 |
| Ramulator2 存储数据吗？ | **否**。Ramulator2 **只模拟时序**，真正的存储是 `memContent` map |
| 软件能直接访问 DDR 数据吗？ | **不能直接访问**。必须通过 hostAxi → Crossbar → DRAMSim → DPI-C 这条**完整的 AXI4 事务路径** |
| 每次读写都是周期精确的吗？ | **是**。即使数据实际在 C++ map 里，但每次访问都要经过 ramulator2 的时序仲裁，消耗真实的仿真周期 |
| 未初始化地址读取会怎样？ | 返回伪随机数据（由 `seed` 参数控制 `mt19937` 随机数生成器） |

---

## 5. DMA 引擎与分块传输

### 5.1 StrideDma 设计 (~500 行)

StrideDma 是专为矩阵加速器设计的 DMA 引擎，提供**矩阵分块语义**：

```
Matrix layout in DDR (row-major, contiguous):
+-------------------- matCols (beats) -----+
| row0: [beat0][beat1][beat2]...[beatN-1]  |
| row1: [beat0][beat1][beat2]...[beatN-1]  | matRows
| ...                                      |
| rowM: [beat0][beat1][beat2]...[beatN-1]  |
+------------------------------------------+

DMA transfers a tile sub-block:
            tileColPos
                |
                v
              +-- tileCols --+
  tileRowPos->| +==========+ |
              | |tile data | | 
              | |          | tileRows
              | +==========+ |
              +--------------+
```

### 5.2 寄存器映射

| 寄存器 | 功能 |
|--------|------|
| `REG_CTRL` | [0]=start, [1]=direction (0=DDR→Local, 1=Local→DDR) |
| `REG_MAT_BASE` | DDR 矩阵基地址（支持 64-bit） |
| `REG_MAT_COLS/ROWS` | 矩阵总尺寸（beat 为单位） |
| `REG_TILE_COLS/ROWS` | 分块标称尺寸 |
| `REG_TILE_COL/ROW_POS` | 分块起始位置 |
| `REG_ACTUAL_COLS/ROWS` | 只读，硬件自动计算的实际传输尺寸 |

### 5.3 硬件自动计算

```
ddrAddr = matBase + (tileRowPos * matCols + tileColPos) * bytesPerBeat
stride  = matCols * bytesPerBeat
actualCols = min(tileCols, matCols - tileColPos)   // right-edge clipping
actualRows = min(tileRows, matRows - tileRowPos)   // bottom-edge clipping
```

### 5.4 状态机 (13 个状态)

```
DDR -> Local (load):
  IDLE -> COMPUTE -> ROW_START -> DDR_RD_CMD -> DDR_RD_DATA -> LOC_WR_CMD -> LOC_WR_DATA
                                    -> ROW_DONE -> ROW_START(next row) -> ... -> DONE

Local -> DDR (store):
  IDLE -> COMPUTE -> ROW_START -> LOC_RD_CMD -> LOC_RD_DATA -> DDR_WR_CMD -> DDR_WR_DATA
                                    -> ROW_DONE -> ROW_START(next row) -> ... -> DONE
```

- 内置 4KB AXI4 burst 边界自动分割
- 使用内部 `dataFifo` 缓存一个 sub-burst 的数据

---

## 6. 缓存控制器 (MatrixCacheController)

### 6.1 双 Bank Ping-Pong 机制

```
Each cache (A/B/C) has 2 banks:

  Phase 1:                         Phase 2 (after switch):
  +--------+  +--------+          +--------+  +--------+
  | Bank 0 |  | Bank 1 |          | Bank 0 |  | Bank 1 |
  |  DMA   |  |  Core  |   --->   |  Core  |  |  DMA   |
  | writing|  | read/  |  switch  | read/  |  | writing|
  |  new   |  | write  |  signal  | write  |  |  next  |
  |  data  |  | prev   |          | data   |  |  data  |
  +--------+  +--------+          +--------+  +--------+

DMA transfer and core compute can overlap in pipeline.
```

### 6.2 AXI4-Lite 寄存器映射

| 地址 | 名称 | 功能 |
|------|------|------|
| 0x00 | LIFE_CFG_A | CacheA 数据可被核心重复读取的次数 |
| 0x04 | LIFE_CFG_B | CacheB 数据可被核心重复读取的次数 |
| 0x08 | STATUS | [0]A就绪, [1]B就绪, [2]C有效, [3]IRQ, [4]C满 |
| 0x0C | INTR_CLR | Write-1-to-Clear 清除输出中断 |

### 6.3 Life Cycle 机制

`lifeCfgA/B` 控制 CacheA/B 的数据能被核心读取多少次才需要重新加载：

- **MatMul 场景**：分块乘法 `C[m][n] += A[m][k] × B[k][n]`
  - CacheA 的每个 tile 需要被复用 `numTilesN` 次（对 N 维所有 tile 共享同一 A tile）
  - CacheB 的每个 tile 只使用 1 次（`lifeCfg=1`）
- **ElementWise 场景**：lifeCfgA=1, lifeCfgB=1

### 6.4 信号连接

```
DMA done signals:
  dmaA.intr -> cacheCtrl.dmaDoneA   (bank ready)
  dmaB.intr -> cacheCtrl.dmaDoneB
  dmaZ.intr -> cacheCtrl.dmaDoneC

Core switch signals:
  core.readSwitch  -> cacheCtrl.switchA, switchB  (core read done)
  core.writeSwitch -> cacheCtrl.switchC            (core write done)

Interrupt output:
  cacheCtrl.globalIntr -> io.globalIntr  (CacheC full / compute done)
```

---

## 7. 加速器核心数据通路

```
Instruction input:
  sAxi4LiteInst -> Inst128_Wrapper -> 128-bit Stream

Datapath:
  memPortA (read CacheA) --> Slicer --> shiftLeft_A align
  memPortB (read CacheB) --> Slicer --> shiftLeft_B align
                                  |
                                  v
                        SystolicArray2D_Wrapper
                           (8x8 Systolic Array)
                                  |
                                  v
                             Activation
                        (Relu/Exp/Log/Softplus)
                                  |
                                  v
                             Collector
                                  |
                                  v
                        memPortZ (write to CacheC)

Done signals:
  Collector -> instFinish -> readSwitch/writeSwitch -> CacheCtrl
```

### 7.1 指令格式 (128 bit)

指令由 `InstSim` 结构定义，包含：
- 操作类型（MatMul/ElementWise/Activation）
- 矩阵维度（M/K/N）
- 移位参数（shiftLeft_A, shiftLeft_B, shiftAfterOp）
- 缓存地址偏移
- Activation 选择

指令通过 4 次 32-bit AXI-Lite 写入，然后触发 trigger 寄存器开始执行。

---

## 8. 完整数据流——五阶段生命周期

以一次 DDR-Native MatMul 操作为例：

### 阶段 1: 软件将矩阵写入 DDR

```
Scala: ddrDriver.writeMatrix(addrA, matrixA)
  -> hostAxi.aw/w -> Crossbar -> DRAMSim -> DPI-C -> memContent[addr] = data
  -> each beat: 8 x 32-bit fixed-point -> 256-bit
  -> row-major contiguous layout
```

### 阶段 2: DMA 从 DDR 加载 tile 到 Cache

```
Scala: dmaDriver.transfer(DmaChannel.A, loadParams)

SW sets DMA registers:
  matBase, matCols, matRows, tileCols, tileRows, tileColPos, tileRowPos
  direction=0 (DDR->Local), start=1

HW state machine:
  COMPUTE: calc ddrAddr = matBase + offset, actualCols/Rows
  ROW_START -> DDR_RD_CMD:
    DMA.mDdr.ar(addr, len=actualCols-1) -> Crossbar -> DRAMSim -> ramulator2
  DDR_RD_DATA:
    DMA.mDdr.r.data -> dataFifo
  LOC_WR_CMD -> LOC_WR_DATA:
    dataFifo -> DMA.mLocal.aw/w -> AxiBramCtrl -> CacheCtrl.writeA -> SdpramA
  ROW_DONE -> next row -> ... -> DONE -> intrDmaA=1

SW waits:
  dmaDriver.waitComplete(DmaChannel.A)  // poll intrDmaA
  dmaDriver.clearInterrupt(DmaChannel.A)
```

### 阶段 3: 核心计算

```
Scala:
  cacheDriver.setLifeCfgA(numTilesN)
  cacheDriver.setLifeCfgB(1)
  instDriver.sendInstruction(inst)  // 128-bit instruction

HW execution:
  Slicer: reads slices from CacheA/B, applies shiftLeft_A/B alignment
  SystolicArray: 8x8 systolic array performs matmul/element-wise ops
  Activation:   optional activation function (LUT-based)
  Collector:    collects results -> writes to CacheC

Done:
  instFinish -> switchA/B/C -> globalIntr=1

SW waits:
  instDriver.waitComputeDone()  // poll globalIntr
```

### 阶段 4: DMA 从 Cache 存回 DDR

```
Scala: dmaDriver.transfer(DmaChannel.Z, storeParams)

HW state machine (reverse direction):
  LOC_RD_CMD -> LOC_RD_DATA:
    DMA_Z.mLocal.ar -> AxiBramCtrl_Z -> CacheCtrl.readC -> SdpramC
    DMA_Z.mLocal.r -> dataFifo
  DDR_WR_CMD -> DDR_WR_DATA:
    dataFifo -> DMA_Z.mDdr.aw/w -> Crossbar -> DRAMSim -> ramulator2
  -> DONE -> intrDmaZ=1

Scala:
  dmaDriver.waitComplete(DmaChannel.Z)
  cacheDriver.clearGlobalIntr()  // prepare for next compute
```

### 阶段 5: 软件从 DDR 读回结果

```
Scala: ddrDriver.readMatrix(addrZ, rows, cols)
  -> hostAxi.ar -> Crossbar -> DRAMSim -> DPI-C -> memContent[addr] -> data
  -> hostAxi.r.data -> unpack to Int matrix
  -> unpad to original dimensions
  -> pushFromLong2D() returns tensor ID
```

---

## 9. 波形可见性分析

### 9.1 能看到什么？

由于整个 SystemWrapper 都是 RTL 仿真（Verilator 编译），**所有 RTL 信号都可以在波形中观察**：

| 信号层级 | 可见性 | 示例 |
|---------|--------|------|
| hostAxi 事务 | ✅ 完全可见 | `hostAxi_aw_valid`, `hostAxi_w_data` |
| DMA 控制信号 | ✅ 完全可见 | `dmaA_io_ctrl_*`, `dmaA_io_mDdr_ar_*` |
| Crossbar 仲裁 | ✅ 完全可见 | 仲裁器内部状态 |
| DRAMSim AXI4 接口 | ✅ 完全可见 | `ddrAxi_ar_*`, `ddrAxi_r_*` |
| DRAMSim → DPI 请求/响应 | ✅ 完全可见 | `dram_req_valid`, `dram_resp_payload_data` |
| Cache BRAM 读写 | ✅ 完全可见 | `sdpramA_io_write_Data` |
| 核心计算 | ✅ 完全可见 | Slicer/SystolicArray/Activation 内部信号 |

### 9.2 不能看到什么？

| 信号 | 可见性 | 原因 |
|------|--------|------|
| DDR 芯片内部信号 | ❌ 不可见 | Ramulator2 是 C++ 库，不生成 RTL 信号 |
| DDR 行激活/预充电 | ❌ 不可见 | 在 ramulator2 内部完成，不暴露 |
| DDR 刷新操作 | ❌ 不可见 | 同上 |
| DDR 控制器调度状态 | ❌ 不可见 | 在 C++ 侧的 FRFCFS 调度器中 |
| memContent 内存内容 | ❌ 波形不可见 | 是 C++ `std::map`，波形只能看到 DPI 接口信号 |

### 9.3 替代观察方法

虽然 DDR 芯片内部不可见，但有替代方式：

1. **DRAM Trace 文件**：`ramulator_config.yaml` 中配置了 `TraceRecorder` →
   `./trace/dram.trace`，记录所有 DDR 访问的地址、时间、类型
2. **DPI 接口波形**：通过观察 `dram_req_*` 和 `dram_resp_*` 信号，可以看到每个
   DDR 请求的**提交时间**和**完成时间**，从而推算 DDR 延迟
3. **AXI Crossbar 波形**：可以分析 DMA 和 hostAxi 之间的仲裁竞争

### 9.4 波形观察方式

仿真使用 Verilator 后端，波形生成取决于 SimConfig 配置：
- `withFstWave` 或 `withVcdWave`：生成 FST/VCD 波形文件
- 波形文件保存在 `simWorkspace/SystemDispatcher/`
- 可用 GTKWave（VCD/FST）或 Verdi（fsdb）打开

---

## 10. 软件驱动层 (HwDrivers)

### 10.1 四个驱动类

```
Defined in HwDrivers.scala:

DmaDriver         -- DMA transfer control
CacheDriver       -- cache lifecycle/interrupt mgmt
InstructionDriver -- instruction encoding & sending
DdrDataDriver     -- DDR data read/write
```

### 10.2 DmaDriver

```scala
// generate load params (DDR -> Cache)
def makeLoadParams(ddrBase, matTotalCols, matTotalRows,
                   tileCols, tileRows, tileColPos, tileRowPos): DmaTransferParams
  // note: matCols and tileCols auto-divided by sideNum to beat units

// generate store params (Cache -> DDR)
def makeStoreParams(...): DmaTransferParams

// execute transfer (= startTransfer + waitComplete + clearInterrupt)
def transfer(channel: DmaChannel, params: DmaTransferParams): Unit
```

### 10.3 CacheDriver

```scala
def setLifeCfgA(n: Int)    // set CacheA lifecycle
def setLifeCfgB(n: Int)    // set CacheB lifecycle
def clearGlobalIntr()      // clear CacheC interrupt
```

### 10.4 InstructionDriver

```scala
def sendInstruction(inst: InstSim)   // 128-bit inst packing -> 4x32-bit AXI-Lite writes
def waitComputeDone()                // poll globalIntr
def sendMatMul(m, k, n, shifts)      // convenience method
def sendElementOp(op, size, shifts)  // convenience method
```

### 10.5 DdrDataDriver

```scala
def writeMatrix(addr: Long, data: Array[Array[Int]], rows: Int, cols: Int)
def readMatrix(addr: Long, rows: Int, cols: Int): Array[Array[Int]]
def matrixSizeBytes(rows: Int, cols: Int): Long
  // = rows * ceil(cols / sideNum) * bytesPerBeat
```

---

## 11. SimContext——仿真线程架构

### 11.1 编译与启动

```scala
SimConfig
  .workspacePath("simWorkspace/SystemDispatcher")
  .addRtl("hw/third_party/dpi/DRAMSimDPIDriver.sv")            // DPI SV module
  .addSimulatorFlag("-LDFLAGS -l:DRAMSimDPIDriverRamulator2.so") // C++ DPI impl
  .addSimulatorFlag("-LDFLAGS -lramulator")                      // ramulator2 lib
  .compile { SystemWrapper(sysCfg) }                             // SpinalHDL -> Verilog -> Verilator
```

Verilator 将 SpinalHDL 生成的 Verilog + DRAMSimDPIDriver.sv 编译为 C++ 仿真器，
链接 ramulator2 动态库。

### 11.2 命令/响应队列

```
Main Thread (Scala)                Sim Thread (Verilator)
     |                                   |
     |  SimCommand                       |
     +------ cmdQueue.put() ---------->  |  cmd = cmdQueue.poll()
     |       (blocking, cap=1)           |  -> drive AXI signals
     |                                   |  -> advance Verilator clk
     |                                   |  -> SimResult
     |  <----- resQueue.put() -----------+
     |  result = resQueue.take()         |
     |  (blocking)                       |
```

### 11.3 支持的命令类型

| SimCommand | 功能 |
|------------|------|
| `WriteMatrixToDdr` | 逐 beat 通过 hostAxi 写入 DDR |
| `ReadMatrixFromDdr` | 逐 beat 通过 hostAxi 从 DDR 读取 |
| `StartDmaCmd` | 设置 DMA 寄存器并启动传输 |
| `WaitDmaIntrCmd` | 轮询 DMA 中断直到完成 |
| `ClearDmaIntrCmd` | 清除 DMA 中断 |
| `SendInstructionCmd` | 128-bit 指令写入 |
| `WaitGlobalIntrCmd` | 轮询全局中断 |
| `SetLifeCfgCmd` | 设置缓存生命周期 |
| `ClearGlobalIntrCmd` | 清除全局中断 |

---

## 12. DDR-Native vs Fallback 执行路径

### 12.1 DDR-Native 路径

适用算子：MatMul, Gemm(无bias), Add, Max, Relu, Exp, Log, Softplus

```
Data stays in DDR throughout:
  ensureInDdr(A) -> ensureInDdr(B) -> memPool.allocate(Z)
  -> for each tile(m, n):
      DMA_A: DDR->Cache (lifeCfg=numTilesN or 1)
      DMA_B: DDR->Cache (lifeCfg=1)
      sendInstruction(shiftA, shiftB, shiftAfterOp)
      waitComputeDone()
      DMA_Z: Cache->DDR
      clearGlobalIntr()
  -> materializeToHost()  // final readback
```

特点：
- 数据不经过 Python/Scala 堆内存
- 移位由硬件 Slicer 的 `shiftLeft_A/B` 完成
- 最高效的路径

### 12.2 Fallback (FB) 路径

适用算子：Sub, Neg, Concat, GemmRelu, 广播 Add/Max, FusedOps

```
Pull data from Python:
  pullData() -> acquireDdr(pyTensorId) -> writeMatrix(ddrBase, data)
  -> HW compute (same as DDR-Native)
  -> readMatrix(addrZ) -> pushToHost() -> returns Python tensor ID
```

特点：
- 需要额外的 hostAxi 数据搬运
- 支持更复杂的操作组合

---

## 13. 与 VerilatorDispatcher 的区别

| 特性 | VerilatorDispatcher | SystemDispatcher |
|------|---------------------|------------------|
| 仿真范围 | 仅加速器核心 (WrapForFPGA) | 完整 SoC (SystemWrapper) |
| DDR 模拟 | 无（零周期 BRAM 直接访问） | ramulator2 DDR4 时序精确 |
| DMA | 无 | 3 个 StrideDma |
| 缓存管理 | 软件直接写 BRAM | MatrixCacheController 双 bank ping-pong |
| AXI Crossbar | 无 | 4 主→1 从仲裁 |
| 仿真速度 | 快 (~10-50x) | 慢（电路规模大 + DDR 时序开销） |
| 波形 | 仅核心信号 | 完整 SoC 信号 |
| 用途 | 快速功能验证 | 系统级性能评估、时序精确分析 |

**为什么 SystemDispatcher 慢？**
1. 电路规模：3 个 DMA 状态机 + AXI Crossbar + CacheCtrl ≈ 10-50x 更多的 RTL 逻辑
2. DDR 延迟：每个 DDR 访问需要经过 ramulator2 的时序仿真（tRCD + tCL + ...）
3. DMA 状态机：13 个状态的 FSM 需要多个周期完成一次传输
4. hostAxi 单 beat 写入：预加载大矩阵时逐 beat 写入，每个都是完整 AXI4 事务

---

## 14. 关键源文件索引

| 文件 | 路径 | 职责 |
|------|------|------|
| DRAMSimDPIDriver.sv | `hw/third_party/dpi/` | SV DPI BlackBox，RTL↔C++ 桥梁 |
| DRAMSimDPIDriverRamulator2.cpp | `hw/third_party/dpi/` | C++ DPI 实现，ramulator2 包装 |
| DRAMSim.scala | `hw/spinal/DMA/` | AXI4 burst → 逐拍 DRAMSim 适配 |
| SystemWrapper.scala | `hw/spinal/WrapForFPGA/` | 完整 SoC 顶层 |
| WrapForFPGA.scala | `hw/spinal/WrapForFPGA/` | 加速器核心 + FPGACfg |
| StrideDma.scala | `hw/spinal/DMA/` | 矩阵分块 DMA 引擎 |
| MatrixCache.scala | `hw/spinal/MatrixCacheInterface/` | 缓存控制器 |
| SimContext.scala | `sw/scala/main/scala/runtime/dispatch/system/` | Verilator 仿真线程 |
| HwDrivers.scala | `sw/scala/main/scala/runtime/dispatch/system/` | SW 驱动（DMA/Cache/Inst/DDR）|
| SystemDispatcher.scala | `sw/scala/main/scala/runtime/dispatch/system/` | DDR-Native/FB 执行逻辑 |
| ramulator_config.yaml | 项目根目录 | DDR4 时序参数配置 |

---

## 附录: 数据格式

### 内存中的数据排列

每个 AXI beat = 256 bit = 8 × 32-bit 元素：

```
beat address alignment: addr % 32 == 0

beat layout (256 bit):
  [elem7(32b)][elem6(32b)]...[elem1(32b)][elem0(32b)]
  MSB <--------------------------------------------- LSB

row storage:
  cols -> beatsPerRow = ceil(cols / sideNum)
  cols < sideNum are zero-padded

DDR address calculation:
  matBase + (row * beatsPerRow + beatCol) * bytesPerBeat
```

### 定点数格式

```
32-bit fixed-point: [sign(1)][int(intWidth-1)][frac(fracWidth)]
  total width: elementWidth = intWidth + fracWidth

typical configs:
  elementWidth=24, intWidth=12, fracWidth=12
  or
  elementWidth=32, intWidth=10, fracWidth=22
```

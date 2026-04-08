# SystemDispatcher 全图系统级仿真 — 完整技术文档

## 1. 概述

SystemDispatcher 是 LiveHPS 项目中 **最高保真度** 的仿真后端，它精确模拟了 FPGA 部署时的完整数据通路：

```
Host(CPU) ──hostAxi──► DDR(DRAM)
                         │
              ┌──────────┼──────────┐
              ▼          ▼          ▼
           DMA_A      DMA_B      DMA_Z
              │          │          ▲
              ▼          ▼          │
           CacheA     CacheB    CacheC
              │          │          ▲
              ▼          ▼          │
           Slicer    Slicer    Collector
         (shiftA)   (shiftB)       ▲
              │          │          │
              └────►SystolicArray──┘
                    (8×8 脉动阵列)
```

与其他后端的对比：

| 后端 | 日志标记 | 计算引擎 | DDR | DMA | Cache | 移位方式 | 速度 |
|------|---------|---------|-----|-----|-------|---------|------|
| **SW** | (无) | 软件参考模型 | ✗ | ✗ | ✗ | SW `roundShiftRight` | ~4 分钟 |
| **Verilator** | (无) | Verilator RTL | ✗ | ✗ | ✗ | SW `roundShiftRight` | ~10 分钟 |
| **System [FB]** | `[FB]` | Verilator RTL | ✓ | ✓ | ✓ | SW `roundShiftRight` | ~数小时 |
| **System [DDR]** | `[DDR]` | Verilator RTL | ✓ | ✓ | ✓ | **HW `shiftLeft_A/B`** | ~数小时 |

---

## 2. 术语解释

### [DDR] — DDR-Native 模式（DDR 原生模式）

**含义：** 数据 **全程驻留 DDR**，无 hostAxi 数据搬运。移位对齐通过硬件指令字段 `shiftLeft_A/B` 在 Slicer 桶形移位器中完成。

**数据流：**
```
               推理期间无 hostAxi 交互
                        ↕
DDR ──DMA_A──► CacheA ──Slicer(shiftA)──► SystolicArray ──► Collector ──► CacheC ──DMA_Z──► DDR
DDR ──DMA_B──► CacheB ──Slicer(shiftB)──►       ↑
                                          中间结果驻留 DDR
                                          供后续算子直接读取
```

**特点：**
- ✅ 完全模拟真实 FPGA 部署行为
- ✅ 推理阶段 **零 hostAxi 搬运**（纯 DMA + Compute 指令）
- ✅ 硬件移位——通过 Slicer 桶形移位器执行定点对齐
- ✅ 中间结果在 DDR 中直接复用，无需往返 Host
- ✅ 周期计数精确反映真实推理延迟

### [FB] — Fallback 模式（回退模式）

**含义：** 通过 **算子注册表**（OpRegistry）执行，走标准的算子实现路径。软件侧完成数据拉取、移位对齐、padding，然后写入 DDR，发起 DMA + Compute 指令。

**数据流：**
```
Python ──pullToLong2D──► SW(移位+padding) ──hostAxi──► DDR ──DMA──► Cache ──► Core ──► DDR ──hostAxi──► SW ──pushFromLong2D──► Python
```

**特点：**
- 算子内部仍有 hostAxi 数据搬运（pull 输入 → 写 DDR → 读回结果 → push 输出）
- 移位在软件侧通过 `roundShiftRight()` 完成（非硬件指令）
- 通过 DDR 指纹缓存（fingerprint cache）避免重复写入相同数据
- 适用于硬件不直接支持的算子（如 Neg、Concat、Sub、Fused 组合算子）

### 两种模式的选择逻辑

```
execute(node) {
  if (canDdrNative(node)):     // → [DDR] 模式
    数据已在 DDR → 纯 DMA+Compute → 结果写回 DDR → 回读给 Python(仅验证用)
  else:                        // → [FB] 模式  
    调用 super.execute() → 算子注册表 → pull→shift→pad→tiledHWOp→unpad→push
}
```

**DDR 原生支持的算子：** `MatMul`, `Gemm`（无 bias 无转置）, `Add`, `Max`, `Relu`, `Exp`, `Log`, `Softplus`（需满足形状条件）

**回退到 FB 的算子/情况：**
- `Neg` — 纯软件取反，无硬件指令
- `Concat` — 纯软件拼接 + 多输入 shift 对齐
- `Sub` — 需要软件侧对 B 取反后作为 Add 执行
- `Gemm`（有 bias 或有转置时）/ `GemmRelu` — 需要软件侧 Add / Transpose / Relu
- `FusedOp` (AddExp, AddLog, SubExp, NegSoftplus) — 通过 `dispatchToOp` 串联基础算子
- 3D×3D MatMul — 需逐 batch 切片，太复杂
- 需要 broadcast 的 Add/Max — 形状不匹配时回退

---

## 3. 三阶段执行模型

### Phase 1: 预加载（Preload）

**目的：** 模拟真实 FPGA 部署前，Host 将所有 ONNX 常量（权重、偏置等）一次性写入 DDR。

```
preloadInitializers(graph):
  for (name, tensorId) in graph.initializers:
    if shape.product < sideNum:
      SKIP  // 过小的张量（量化参数等）保留在软件侧
    else:
      data = pullToLong2D(tensorId)           // 从 Python 读取
      padded = zeroPad(data, sideNum倍数)      // 填充到硬件对齐尺寸
      addr = memPool.allocate(sizeBytes)       // 分配 DDR 空间
      ddrDriver.writeLongMatrix(addr, padded)  // 通过 hostAxi 写入 DDR
      _tensorDdrMap[name] = DdrTensorInfo(...)  // 注册到 DDR 张量注册表
      _ddrWriteCache[fingerprint] = entry       // 建立指纹缓存（回退路径用）
  
  allocateZeroMatrix()  // 预分配全零矩阵（供 Activation 使用）
```

**过滤逻辑：** `shape.product < sideNum`（如 `shape=[1]`、`shape=[8]`）的张量被跳过——这些通常是量化的缩放因子 (scale) 或偏移 (zero_point)，仅在软件侧合成指令时使用（作为移位参数），不需要加载到 DDR 中作为矩阵操作数。

**日志示例：**
```
[PRELOAD] #42  'encoder.weight'  shape=[256,64]  → padded(256×64) DDR=0x100080000  64 KB
[PRELOAD] SKIP 'quant_scale'     shape=[1] (1 elems < sideNum=16, param not tensor)
```

### Phase 2: 推理（Inference）

**目的：** 按拓扑序逐算子执行 ONNX 图中的所有 HW 节点。DDR-native 算子全程数据驻留 DDR；回退算子通过算子注册表。

**DDR 原生路径（[DDR]）的逻辑：**
```
executeDdrMatMul(node):
  infoA = ensureInDdr(inputA)   // 查 _tensorDdrMap，未命中则从 Python 拉取写入
  infoB = ensureInDdr(inputB)
  
  // 移位参数——合成到硬件指令中，由 Slicer 桶形移位器执行
  shiftA = tisA - ssA           // 正=左移，负=右移
  shiftB = tisB - ssB
  shiftAfterOp = tisA + tisB - tos
  
  addrZ = memPool.allocate(...)  // 分配输出 DDR 空间
  
  tiledHWOpDdr(addrA, addrB, addrZ,
    shiftA, shiftB, shiftAfterOp)  // 纯 DMA + Compute 指令序列
  
  _tensorDdrMap[outName] = DdrTensorInfo(addrZ, ...)  // 注册输出（供后续算子直接读取）
  
  materializeToHost(addrZ, ...)  // 回读到 Python（仅验证用，不计入推理周期）
```

**回退路径（[FB]）的逻辑：**
```
super.execute(node):
  → AcceleratorDispatcher.execute()
    → opRegistry[node.opType].execute()
      → MatMulOp / ElementWiseOp / ActivationOp / ...
        →  pullToLong2D()       // 从 Python 拉取
        →  roundShiftRight()    // 软件移位
        →  zeroPad()            // 对齐填充
        →  tiledHWOp()          // 调用 SystemDispatcher.tiledHWOp
            → acquireDdr()      // 指纹缓存 → 写 DDR
            → DMA + Compute     // 硬件执行
            → readLongMatrix()  // 从 DDR 读回
        →  unpad → push        // 去填充 → 返回 Python
```

### Phase 3: 回读（Readback）

**目的：** 将 DDR-native 算子的计算结果回读到 Python 侧，用于：
1. 在线 ORT 交叉验证（`onNodeComplete` 回调）
2. 供后续非 DDR-native 算子作为输入使用
3. 最终输出结果提取

```
materializeToHost(addrZ, origRows, origCols, padRows, padCols, shape):
  padded = ddrDriver.readLongMatrix(addrZ, padRows, padCols)  // 从 DDR 读取
  unpadded = trim(padded, origRows, origCols)                  // 去除填充
  rawId = pushFromLong2D(unpadded)                             // 推送到 Python
  if (shape.length > 2): Tensor.op("Reshape", shape)           // 恢复高维形状
```

**回读周期不计入推理：** `_currentComputeEndCycle` 在 DMA+Compute 完成后立即记录，`materializeToHost` 产生的额外 DDR 读取开销独立统计。

---

## 4. 每个算子的实现细节

### 4.1 MatMul / Gemm（矩阵乘法）

| 属性 | 值 |
|------|------|
| ONNX opType | `MatMul`、`Gemm`（无 bias 无转置时等效） |
| 执行模式 | **[DDR]**（2D×2D, 3D×2D）/ **[FB]**（3D×3D） |
| HW 操作 | `matmul` |
| 激活函数 | `none` |

**移位公式：**
```
输入:
  tisA = fpga_in_shift[0]      // 目标输入精度 A
  tisB = fpga_in_shift[1]      // 目标输入精度 B
  ssA  = producer_out_shift(A)  // A 的实际精度（来自上游算子）
  ssB  = producer_out_shift(B)  // B 的实际精度
  tos  = fpga_out_shift[0]      // 目标输出精度

[DDR] 模式:
  shiftLeft_A = tisA - ssA      // Slicer 对 A 的左移量（负=右移）
  shiftLeft_B = tisB - ssB      // Slicer 对 B 的左移量
  shiftLeft_AfterMatrixOperation = tisA + tisB - tos  // 乘后对齐输出精度

[FB] 模式:
  rsA = ssA - tisA   →  roundShiftRight(dataA, rsA)  // 软件右移对齐
  rsB = ssB - tisB   →  roundShiftRight(dataB, rsB)
  shiftAfterOp = tisA + tisB - tos
```

**[DDR] 与 [FB] 的等价性：**
- [FB] 在软件侧执行 `roundShiftRight(data, ssA - tisA)`（向右移 `ssA - tisA` 位，带四舍五入）
- [DDR] 在硬件 Slicer 中执行 `data << (tisA - ssA)`（左移 `tisA - ssA` 位）
- 当 `tisA - ssA < 0` 时，左移负数等效于右移正数
- 唯一差异：软件用 `roundShiftRight`（四舍五入），硬件用算术移位（截断），误差 ≤ 1 LSB

**3D 矩阵乘法支持：**
- **3D×2D**：`[batch, M, K] × [K, N]` → 展平为 `[batch*M, K] × [K, N]` → 计算 → reshape 为 `[batch, M, N]`
- **3D×3D**：回退到 [FB]，逐 batch 切片调用 `matMul2D`

**Tiling（分块）：**
```
                totalRows (M)
           ┌──────────────────┐
           │  tile(tM×K)      │  ← DMA_A 加载（lifeCfg=numTilesN，跨 N 方向复用）
           ├──────────────────┤
           │  tile(tM×K)      │
           └──────────────────┘
                               K×totalCols (N)
                         ┌─────────┬─────────┐
                         │tile(K,tN)│tile(K,tN)│ ← DMA_B 加载（lifeCfg=numTilesM，跨 M 方向复用）
                         └─────────┴─────────┘
```

- CacheA lifeCfg = numTilesN（A 块在 N 方向上被 B 的多个列块复用）
- CacheB lifeCfg = numTilesM（B 块在 M 方向上被 A 的多个行块复用）

### 4.2 Add（逐元素加法）

| 属性 | 值 |
|------|------|
| ONNX opType | `Add` |
| 执行模式 | **[DDR]**（形状完全相同）/ **[FB]**（需 broadcast） |
| HW 操作 | `elementadd` |
| 激活函数 | `none` |

**移位公式（与 ElementWiseOp 一致）：**
```
comparisonShift = max(tisA, tisB)  // 两路必须对齐到相同精度再加
shiftLeft_A = comparisonShift - ssA
shiftLeft_B = comparisonShift - ssB
shiftAfterOp = comparisonShift - tos
```

**Broadcast 处理：** 当 A 和 B 形状不同时（如 `[1, 32, 64]` + `[64]`），`canDdrNative` 返回 false，回退到 [FB] 模式。[FB] 中 `ElementWiseOp` 调用 `TorchBridge.call("broadcast_tensors")` 在 Python 侧完成广播。

### 4.3 Sub（逐元素减法）

| 属性 | 值 |
|------|------|
| ONNX opType | `Sub` |
| 执行模式 | **[FB]**（始终回退） |
| HW 操作 | `elementadd`（B 取反后加） |
| 激活函数 | `none` |

**为什么不支持 [DDR]：** 硬件没有减法指令。`ElementWiseOp` 实现中将 `Sub` 转换为 **B 取反 + elementadd**：
```scala
if (isSub) bData(i)(j) = -bData(i)(j)  // 逐元素取反
// 然后执行 elementadd
```
取反需要在软件侧完成，因此 Sub 必须走 [FB] 路径。

### 4.4 Max（逐元素取大）

| 属性 | 值 |
|------|------|
| ONNX opType | `Max` |
| 执行模式 | **[DDR]**（形状完全相同）/ **[FB]**（需 broadcast） |
| HW 操作 | `elementmax` |
| 激活函数 | `none` |

移位公式与 Add 相同（`comparisonShift = max(tisA, tisB)`），因为比较操作同样要求两路在同一精度下进行。

### 4.5 Relu / Exp / Log / Softplus（激活函数）

| 属性 | 值 |
|------|------|
| ONNX opType | `Relu`, `Exp`, `Log`, `Softplus` |
| 执行模式 | **[DDR]**（始终） |
| HW 操作 | `elementadd`（与零矩阵相加 → 恒等变换） |
| 激活函数 | 对应的查表函数 (`relu`/`exp`/`log`/`softplus`) |

**原理：** 激活函数通过硬件查表（LUT）实现。指令中指定 `activationFunction = Relu/Exp/Log/Softplus`，硬件在逐元素加法之后自动对结果应用激活函数。由于 B = 全零矩阵，`A + 0 = A`，实际效果等同于直接对 A 应用激活函数。

**移位公式：**
```
sHW = hwFracWidth            // 硬件查表的定点精度（固定值）  
shiftLeft_A = sHW - ssX      // 将输入对齐到 HW 查表精度
shiftLeft_B = 0              // 零矩阵不需要移位
shiftAfterOp = 0             // elementadd 后不移位（激活函数内部处理）
shiftAfterAct = sHW - tos    // 激活函数输出对齐到目标精度
```

**[DDR] 模式零矩阵处理：**
- 预加载阶段预分配一块全零的 DDR 区域 `_zeroDdrAddr`（显式写入零）
- 每个 tile 的 DMA_B 始终从 `_zeroDdrAddr + offset=0` 加载（`zeroBMode=true`）
- 避免按 tileRow offset 读取超出零矩阵分配范围的脏数据

**[FB] 模式：** `ActivationOp` 在软件侧构造全零数组 `padZ`，调用 `tiledElementOp(padX, padZ, ..., "elementadd", 0, actFn, postShift)`。

### 4.6 Neg（取反）

| 属性 | 值 |
|------|------|
| ONNX opType | `Neg` |
| 执行模式 | **[FB]**（纯软件，不调用硬件） |
| HW 操作 | 无 |

**实现：** 纯软件逐元素取反 + 移位：
```scala
for (i, j) xData(i)(j) = roundShiftRight(-xData(i)(j), ssX - tos)
```
最简单的算子，不需要任何硬件参与。但仍通过 `AcceleratorDispatcher.execute()` 管理（计入 HW 算子编号）。

### 4.7 Concat（拼接）

| 属性 | 值 |
|------|------|
| ONNX opType | `Concat` |
| 执行模式 | **[FB]**（纯软件，不调用硬件） |
| HW 操作 | 无 |

**实现：** 对每个输入独立做 shift 对齐（`rescaleAmount = ss - tos`），然后调用 Python 的 `Tensor.op("Concat", inputs, axis)` 完成拼接。

### 4.8 Gemm / GemmRelu（通用矩阵乘 + 偏置 + 可选 Relu）

| 属性 | 值 |
|------|------|
| ONNX opType | `Gemm`, `GemmRelu` |
| 执行模式 | **[DDR]**（无 bias、无转置时，退化为 MatMul）/ **[FB]**（有 bias、有转置或 GemmRelu） |
| HW 操作 | [DDR] 时 `matmul`；[FB] 时通过 `dispatchToOp("MatMul")` |

**DDR 原生条件（`canDdrNative`）：**
```
Gemm 走 [DDR] 当且仅当：
  1. inputIds.size == 2（无 bias C）
  2. transA == 0 && transB == 0（无转置）
  3. 满足 MatMul 相同的形状约束（2D×2D 或 3D×2D）
```

当前 FPGA 模型中，量化导出管线已通过 `fuse_gemm_bias_into_weight()` 将 bias 融合进权重矩阵，
所有 39 个 Gemm 节点均为无 bias、`transA=0, transB=0` 的形式，**全部走 [DDR] 路径**，
等效于 MatMul 直接调用 `executeDdrMatMul()`。

**[FB] 回退实现（有 bias 或 GemmRelu 时）：**
```
Gemm(A, B, C) = MatMul(A, B) + C
  1. 处理 transA/transB 转置属性
  2. dispatchToOp("MatMul", ...) → 调用 MatMulOp → tiledHWOp [FB]
  3. 若有 bias C：Tensor.op("Add", result, scaled_bias)（Python 侧）
  4. GemmRelu：额外调用 Tensor.op("Relu", result)（Python 侧）
```

### 4.9 FusedOp（融合算子）

| 属性 | 值 |
|------|------|
| ONNX opType | `NegSoftplus`, `AddExp`, `AddLog`, `SubExp` |
| 执行模式 | **[FB]**（组合算子） |
| HW 操作 | 通过 `dispatchToOp` 串联基础算子 |

**实现示例（AddExp）：**
```
AddExp(A, B) = Exp(Add(A, B))
  1. dispatchToOp("Add", node, inputs)  → 调用 ElementWiseOp → [FB]
  2. dispatchToOp("Exp", fakeNode, [addResult])  → 调用 ActivationOp → [FB]
```

**NegSoftplus 特殊：** `Softplus → Neg`，其中 Neg 步骤用 `Tensor.op("Neg")` 在 Python 侧完成（不走 HW）。

---

## 5. 核心数据结构

### DdrTensorInfo — DDR 张量描述符

```scala
case class DdrTensorInfo(
  name: String,            // ONNX 张量名（如 "encoder.weight"）
  ddrAddr: Long,           // DDR 中的基地址（如 0x100080000）
  origRows: Int,           // 原始行数（未填充，如 256）
  origCols: Int,           // 原始列数（未填充，如 64）
  padRows: Int,            // 填充后行数（sideNum 的倍数，如 256）
  padCols: Int,            // 填充后列数（sideNum 的倍数，如 64）  
  originalShape: Array[Long],  // 原始 N 维形状（如 [4, 64, 64]）
  isPreloaded: Boolean     // 是否为 Phase 1 预加载常量
)
```

### 两套索引

| 索引 | 键 | 用途 |
|------|-----|------|
| `_tensorDdrMap` | ONNX 张量名（如 `"PPQ_Variable_465"`) | DDR-native 算子通过 ONNX 名查找输入 |
| `_tidToDdr` | Python 张量 ID（如 `"tensor_abc123"`) | 回退算子产生的中间结果的 DDR 位置 |
| `_ddrWriteCache` | 数据指纹（64-bit hash） | 回退路径避免重复写入相同数据到 DDR |

### 零矩阵

```
_zeroDdrAddr: Long       // 预分配的全零 DDR 区域基地址
_zeroDdrMaxElems: Int    // 零矩阵最大元素数 (side × side)
```
在预加载阶段后调用 `allocateZeroMatrix()` 分配并显式写入零。供所有 [DDR] 模式 Activation 算子共用。

---

## 6. Tiling 引擎

### tiledHWOpDdr（[DDR] 矩阵乘法 Tiling）

```
输入：addrA(DDR), rowsA, colsA(=K), addrB(DDR), rowsB(=K), colsB, addrZ(DDR)
      shiftA, shiftB, shiftAfterOp

tile 划分：
  effectiveTileCapM = min(hwTileCapM, hwTileCapMForOutput)  // M 方向每块最大行数
  numTilesM = ceil(rowsA / effectiveTileCapM)
  numTilesN = ceil(colsB / hwTileCapN)

对每个 tile (mIdx, nIdx):
  if nIdx == 0:  DMA_A.load(addrA + mOff, tM×K);  lifeCfgA = numTilesN  // A 行块跨 N 复用
  if mIdx == 0:  DMA_B.load(addrB + nOff, K×tN);   lifeCfgB = numTilesM  // B 列块跨 M 复用
  
  发送指令: InstSim(matmul, shiftA, shiftB, shiftAfterOp, tM, K, tN)
  waitComputeDone()
  
  DMA_Z.store(addrZ + (mOff, nOff), tM×tN)
  clearGlobalIntr()
```

**关键：** `tiledHWOpDdr` 中 `shiftLeft_A` 和 `shiftLeft_B` 被设置在指令中，由 Slicer 硬件执行。`tiledHWOp`（回退路径）中 `shiftLeft_A = 0, shiftLeft_B = 0`，因为移位已在软件侧完成。

### tiledElementOpDdr（[DDR] 逐元素 Tiling）

```
输入：addrA(DDR), totalRows, totalCols, addrB(DDR), addrZ(DDR)
      shiftA, shiftB, shiftAfterOp, actFn, shiftAfterAct
      zeroBMode: Boolean

tile 划分：
  hwTileCap = floor(MAX_HW_ELEMENTS / cols / sideNum) * sideNum
  numTilesR = ceil(rows / hwTileCap)

对每个 tile rIdx:
  DMA_A.load(addrA + rOff, tR×cols);  lifeCfgA = 1
  
  if zeroBMode:
    DMA_B.load(addrB, tR×cols, offset=0)  // 始终从零矩阵开头加载
  else:
    DMA_B.load(addrB + rOff, tR×cols)     // 按 tile offset 加载
  lifeCfgB = 1
  
  发送指令: InstSim(elementadd/max, shiftA, shiftB, shiftAfterOp, actFn, shiftAfterAct, tR, cols, cols)
  waitComputeDone()
  
  DMA_Z.store(addrZ + rOff, tR×cols)
  clearGlobalIntr()
```

---

## 7. 移位机制对比：软件 vs 硬件

### 软件侧移位（[FB] 路径）

```scala
// MatMulOp.scala
val rsA = (ssA - tisA).toInt
val alignedA = aData.map(_.map(v => roundShiftRight(v, rsA)))
// rsA > 0: 右移（低精度对齐到高精度）
// rsA < 0: 左移  

def roundShiftRight(x: Long, n: Int): Long = {
  if (n <= 0) x << -n
  else (x + (1L << (n - 1))) >> n  // 带四舍五入的右移
}
```

### 硬件侧移位（[DDR] 路径）

```scala
// SystemDispatcher.scala
val shiftA = (tisA - ssA).toInt  // = -rsA
// 传入 InstSim.shiftLeft_A

// Slicer.scala (RTL)
shiftersA(i).io.shiftAmount := -instReg.shiftLeft_A  // 实际移动方向取反
// 当 shiftLeft_A > 0 → shiftAmount < 0 → 右移（因为 shiftAmount 是移位器的移动量）
// 当 shiftLeft_A < 0 → shiftAmount > 0 → 左移
```

**等价性：** `shiftLeft_A = tisA - ssA = -(ssA - tisA) = -rsA`

| 场景 | rsA | shiftLeft_A | 软件行为 | 硬件行为 |
|------|-----|------------|---------|---------|
| ssA > tisA（需降精度） | +3 | -3 | 右移3位(带round) | 右移3位(truncate) |
| ssA < tisA（需升精度） | -2 | +2 | 左移2位 | 左移2位 |
| ssA = tisA（已对齐） | 0 | 0 | 不变 | 不变 |

**精度差异：** 软件 `roundShiftRight` 有四舍五入（±0.5 LSB），硬件是纯算术移位（截断）。最大误差 1 LSB/元素，在 24-bit 定点精度下通常可忽略。

---

## 8. 性能统计

### 周期计数分离

```
_preloadCycles          // Phase 1: hostAxi 写全部常量的仿真周期
_currentComputeEndCycle // Phase 2: 纯 DMA + Compute 的周期（真实推理延迟）
_readbackOverheadCycles // Phase 3: 回读到 Python 的额外开销（不计入推理）

// 真实推理延迟 = Σ (computeEnd - startCycle) for all ops
// 总系统延迟  = preloadCycles + 推理延迟 + readbackOverhead
```

### 日志解读

```
┌─ HW#3 Relu 'PPQ_Operation_50'  [DDR]        ← 算子编号、类型、名称、模式
  [DDR-IN] WRITE 'x' (8192×64) → padded(8192×64) DDR=0x1042CAC00  ← 输入从 Python 写入 DDR
[ONLINE] HW#3 Relu [OK] meanErr=0.000001 maxErr=0.000008 ratio=1.0000  ← 在线 ORT 验证
└─ HW#3 done: compute=1576040 cycles, 2 tiles  [DDR]  act:relu(8192×64) DDR-native
     ↑ 纯计算周期          ↑ tile 数       ↑ 模式      ↑ 形状描述
```

```
┌─ HW#2 Gemm 'PPQ_Operation_59'  [FB]         ← 回退模式
[HW#2] Gemm 'PPQ_Operation_59' shifts_in=List(22, 20) shifts_out=List(20)
  [DDR-A] WRITE (8192×16) → DDR 0x1042CAC00  [matmul 8192×16×64]    ← 新数据写入 DDR
  [DDR-B] CACHE HIT @ 0x103AF9800  [matmul 8192×16×64]              ← 指纹缓存命中
└─ HW#2 done: compute=2870152 cycles, 2 tiles  [FB]  matmul(8192×16×64) tiles=2×1 [FB]
```

### printPerfSummary 输出

```
================================================================================
  System-Level Performance Summary (DDR-Resident Full-Graph)
================================================================================

  [Phase 1] DDR Constant Preloading:
    Tensors loaded:     987
    Data transferred:   245760 KB
    Preload cycles:     12345678

  [Phase 2] Inference (Pure DMA+Compute):
    Total HW ops:       156  (DDR-native: 89, fallback: 67)
    Pure compute cycles: 98765432  ← 真实推理周期数
    Total (preload+inf): 111111110

  [Phase 3] Readback Overhead (不计入推理):
    Readback cycles:    5432100  (仅用于结果验证/兼容)

  Per-OpType Breakdown (pure compute):
  ────────────────────────────────────────────────────────────────────────────────
  OpType          Count    ComputeCyc          Avg    Pct     Mode
  ────────────────────────────────────────────────────────────────────────────────
  Gemm               32       65432100      2044753  66.3%       FB
  MatMul             24       20000000       833333  20.3%      DDR
  Relu               45        8000000       177778   8.1%      DDR
  Add                20        3000000       150000   3.0%      MIX
  ...
================================================================================
```

---

## 9. 运行方式

### 切换后端

在 `HWBackendTestSuite.scala` 中修改：
```scala
private val HW_BACKEND = "system"   // "sw" / "verilator" / "vcs" / "system"
```

或通过 JVM 属性：
```bash
sbt -Dhw.backend=system 'testOnly runtime.dispatch.HWBackendTestSuite'
```

### 配置参数

```scala
val d = new SystemDispatcher(sysCfg, "int32")
d.dmaLogLevel = 1          // 0=静默, 1=算子摘要, 2=每次DMA
d.crossCheck = true        // 启用 SW 交叉校验
d.autoPreload = true       // 首次 execute 时自动预加载
d.collectPerfStats = true  // 收集性能统计
```

### 独立测试

```bash
sbt 'Test/runMain runtime.dispatch.system.SystemDispatcherTestSuite'
# 12 个 standalone 测试，验证 tiledHWOp / tiledElementOp 回退路径
```

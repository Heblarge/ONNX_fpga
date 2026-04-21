# Attention Operators In `FloatingPoint`

## 1. 文件范围

本文档对应当前实现（`package FloatingPoint`）：

- `AttentionOps.scala`
- `FpxxOnlineSoftmaxTester.scala`
- `FpxxQKVTester.scala`
- `FpxxDivStreams.scala`
- `FpxxDivEvenStreams.scala`

说明：`AttentionOps` 已包含 softmax 归一化除法与 QKV 累加归一化输出，不再使用“转定点后除法”的路径。

---

## 2. `AttentionOps` 整体功能

`AttentionOps.scala` 实现了 FlashAttention 前向 tile 更新核心链路：

1. `Q · K_i` 打分（dot product）
2. online softmax 状态更新（`newMax/newSum/prevScale`）
3. softmax 归一化输出（`normScores = expScores / newSum`）
4. 分子累加更新（`newAcc`）
5. 分子归一化输出（`newAccNorm = newAcc / newSum`）

它是“块级算子集合”，不是完整控制器/调度器。

---

## 3. 子算子与依赖关系

### 3.1 Attention 顶层相关

- `FpxxOnlineSoftmax`
- `FpxxQKV`

### 3.2 AttentionOps 内部子算子

- `AttentionExpConfig`
- `AttentionOps`（工具函数）
- `FpxxCompare`
- `FpxxSubCompatible`
- `FpxxDivCompatible`
- `FpxxExpNegLut`
- `FpxxAddChain`
- `FpxxDotProduct`

### 3.3 递归依赖到的 Fpxx 基础算子

- `FpxxAddCompatible`
- `FpxxMulCompatible`
- `FpxxConverter`
- `FpxxDivStreams`
- `FpxxDivEvenStreams`

---

## 4. 关键配置与工具模块

### 4.1 `AttentionExpConfig`

接口：

```scala
case class AttentionExpConfig(
  expLutFracBits: Int = 4,
  expLutMin: Double = -8.0
)
```

作用：

- 定义 `exp` LUT 量化精度与覆盖下限。
- 派生：
  - `expLutStepsPerUnit = 1 << expLutFracBits`
  - `expLutMaxIndex = -expLutMin * expLutStepsPerUnit`

### 4.2 `AttentionOps` 工具函数

主要接口：

- `mulConfigFor(c)`：为输入格式选择乘法内部格式（例如 float16/fp8 会映射到 *_mul 配置）。
- `delayWhenValid` / `delayBoolWhenValid`：按 `valid` 流对齐延迟。
- `fpxxHostFromDouble` / `fpxxConst`：构造浮点常量（支持 IEEE-like 与小位宽非 IEEE 配置）。
- `formatName`：格式标识字符串。

---

## 5. 算子接口与行为

### 5.1 `FpxxCompare`

接口：

- `lt(a,b)`
- `lte(a,b)`
- `maxOf(values)`

作用：浮点比较和 max 归约（用于 block max / new max 选择）。

### 5.2 `FpxxSubCompatible`

接口：

```scala
io.op: Flow { a: Fpxx, b: Fpxx }
io.result: Flow[Fpxx]
```

作用：通过翻转 `b.sign` 把减法映射到 `FpxxAddCompatible`。

### 5.3 `FpxxDivCompatible`

接口：

```scala
io.input: Flow { num: Fpxx, den: Fpxx }
io.result: Flow[Fpxx]
```

作用：统一的浮点除法包装器。

- `mant_size` 偶数：调用 `FpxxDivEvenStreams`
- `mant_size` 奇数：调用 `FpxxDivStreams`

用于 softmax 与 QKV 的归一化除法。

### 5.4 `FpxxExpNegLut`

接口：

```scala
io.op: Flow[Fpxx]
io.result: Flow[Fpxx]
```

作用：softmax 专用负区间 `exp(x)` LUT 近似。

### 5.5 `FpxxAddChain`

接口：

```scala
class FpxxAddChain(values: Seq[Flow[Fpxx]], c: FpxxConfig) {
  result: Flow[Fpxx]
}
```

作用：多路 `Flow[Fpxx]` 流水加法归约。

### 5.6 `FpxxDotProduct`

接口：

```scala
io.input.payload.a: Vec[Fpxx]
io.input.payload.b: Vec[Fpxx]
io.result: Flow[Fpxx(mulCfg)]
```

作用：逐元素乘法 + 加法链归约。

### 5.7 `FpxxOnlineSoftmax`

接口：

```scala
class FpxxOnlineSoftmax(tileSize: Int, c: FpxxConfig, cfg: AttentionExpConfig)
```

输入：

- `scores: Vec[Fpxx(c)]`
- `prevMax: Fpxx(c)`
- `prevSum: Fpxx(c)`
- `init: Bool`

输出：

- `expScores: Vec[Fpxx(c)]`
- `normScores: Vec[Fpxx(c)]`
- `newMax: Fpxx(c)`
- `prevScale: Fpxx(c)`
- `newSum: Fpxx(c)`

行为（在线递推）：

```text
blockMax  = max(scores)
newMax    = init ? blockMax : max(prevMax, blockMax)
exp_i     = exp(score_i - newMax)
prevScale = init ? 0 : exp(prevMax - newMax)
newSum    = init ? sum(exp_i) : prevSum * prevScale + sum(exp_i)
norm_i    = exp_i / newSum
```

### 5.8 `FpxxQKV`

接口：

```scala
class FpxxQKV(tileSize: Int, headDim: Int, c: FpxxConfig, cfg: AttentionExpConfig)
```

输入：

- `q: Vec[Fpxx(c)]`
- `k: Vec[tileSize][headDim][Fpxx(c)]`
- `v: Vec[tileSize][headDim][Fpxx(c)]`
- `prevMax: Fpxx(scoreCfg)`
- `prevSum: Fpxx(scoreCfg)`
- `prevAcc: Vec[Fpxx(scoreCfg)]`
- `init: Bool`

输出：

- `scores: Vec[Fpxx(scoreCfg)]`
- `expScores: Vec[Fpxx(scoreCfg)]`
- `normScores: Vec[Fpxx(scoreCfg)]`
- `newMax: Fpxx(scoreCfg)`
- `newSum: Fpxx(scoreCfg)`
- `newAcc: Vec[Fpxx(scoreCfg)]`
- `newAccNorm: Vec[Fpxx(scoreCfg)]`

行为：

```text
score_i     = dot(q, k_i)
(newMax, newSum, exp_i, norm_i, prevScale) <- online softmax
newAcc[d]   = prevAcc[d] * prevScale + Σ_i exp_i * v_i[d]
newAccNorm[d] = newAcc[d] / newSum
```

其中 `v` 会先通过 `FpxxConverter(c -> scoreCfg)` 对齐到内部计算格式。

---

## 6. 除法器实现说明

### 6.1 `FpxxDivStreams`

- Stream 接口、支持反压。
- 奇偶 mantissa 都可用：偶数 mantissa 时内部扩展到 `mant+1` 精度计算，再舍入回目标 mantissa。
- 特殊值输出通过 `set_nan/set_inf/set_zero` 与 `FpxxConfig` 保持一致。

### 6.2 `FpxxDivEvenStreams`

- 针对偶数 mantissa 的 Stream 除法器实现。
- 同样用 `set_nan/set_inf/set_zero` 处理特殊值。

### 6.3 Attention 中的使用

- `FpxxOnlineSoftmax`：`normScores = expScores / newSum`
- `FpxxQKV`：`newAccNorm = newAcc / newSum`

均通过 `FpxxDivCompatible` 自动选择具体除法实现。

---

## 7. Testbench 行为

### 7.1 `FpxxOnlineSoftmaxTester`

DUT：

```scala
new FpxxOnlineSoftmax(tileSize = 4, c = FpxxConfig.float32(), cfg = AttentionExpConfig())
```

测试内容：

- `init=true/false` 两种路径
- `newMax`
- `prevScale`
- `newSum`
- `expScores`
- `normScores`

golden 要点：

- `exp` 使用与 RTL 同步的 LUT 量化策略（`quantizedExp`）
- 先算 `expScores/newSum`，再算 `normScores`
- 逐项与硬件输出做容差比较

### 7.2 `FpxxQKVTester`

DUT：

```scala
new FpxxQKV(tileSize = 2, headDim = 4, c = FpxxConfig.float32(), cfg = AttentionExpConfig())
```

测试内容：

- `scores = Q·K`
- online softmax 更新
- `expScores`
- `normScores`
- `newMax/newSum`
- `newAcc`
- `newAccNorm`

golden 要点：

- 先算 `scores`，再按 online softmax 递推算 `expScores/newSum/normScores`
- 再算 `newAcc` 与 `newAccNorm`
- 所有输出与硬件逐项做容差比较

---

## 8. 运行方式

在项目根目录执行（`/home/wuhw2024/livehps_1`）：

```bash
sbt "runMain FloatingPoint.FpxxOnlineSoftmaxTester"
sbt "runMain FloatingPoint.FpxxQKVTester"
```

若用 VCS，建议确保：

- `VCS_TARGET_ARCH=linux64`
- `VCS_HOME`、license 环境已正确配置

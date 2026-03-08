# SquareSystolicArray Floating-Point Format Change Guide

本文档用于记录每次切换 `SquareSystolicArray` 浮点格式时，需要同步修改的文件和关键点。

## 1. 先确定目标格式映射

先在 `FpxxConfig` 中确认输入格式与乘法结果格式的对应关系：

- `float16()` -> `float16_mul()`
- `float8_e5m2fnuz()` -> `float8_e5m2mul()`
- `float8_e4m3fnuz()` -> `float8_e4m3mul()`

参考定义位置：`hw/spinal/FloatingPoint/Fpxx.scala` 第 20-59 行。

如果你要引入新的输入格式，必须先在 `FpxxConfig` 里新增对应的输入格式配置以及与该浮点格式对应的`xxx_mul()` 配置，再进行下面各文件修改。

## 2. 必改文件清单

## 2.1 `hw/spinal/FloatingPoint/FpAccumPE.scala`

关键位置：第 39、40、57 行。

每次切格式都要同步改 3 处：

1. `FpxxMul.Options.cIn`
2. `FpxxMul.Options.cOut`
3. `Fpxx2AFixCompatible.c`

示例（切到 `float8_e4m3fnuz`）：

```scala
cIn  = FpxxConfig.float8_e4m3fnuz(),
cOut = Some(FpxxConfig.float8_e4m3mul()),
...
c    = FpxxConfig.float8_e4m3mul(),
```

注意：这 3 处必须保持一致的“输入格式/乘积格式”配对，否则会出现位宽或数值语义不匹配。

## 2.2 `hw/spinal/FloatingPoint/FpAccumPETester.scala`

关键位置：第 78 行。

必须改：

1. `val fpxxCfg = ...` 改成与 `FpAccumPE` 中 `cIn` 一致的输入格式。

建议同步检查：

1. 第 131 行 `range`（格式越低精度，建议适当缩小输入范围）。
2. 输出对比容忍策略是否满足当前格式精度预期。

## 2.3 `hw/spinal/FloatingPoint/SquareSystolicArray.scala`

关键位置：第 12 行（配置默认值）和第 231 行（`SquareSystolicArrayApp` 示例配置）。

根据你的使用方式选择：

1. 如果工程里大量依赖默认参数，改第 12 行默认 `fpConfig`。
2. 如果只改当前生成入口，至少改第 231 行 `SquareSystolicArray_Config(... fpConfig = ...)`。

说明：模块内部 PE 输入和输出转换都使用 `cfg.fpConfig`（第 135-142、205-209 行），因此这里的配置会传递到整个阵列。

## 2.4 `hw/spinal/FloatingPoint/tb_SquareSystolicArray.scala`

关键位置：第 261 行。

必须改：

1. `cfg` 里的 `fpConfig = ...`，与 `SquareSystolicArray` 测试目标格式一致。

建议同步检查：

1. 第 333 行 `tolerance` 是否需要按新格式调整。
2. 该 testbench 的 `FloatAlgo`、打包/解包逻辑均基于 `cfg.fpConfig`，只要这里一致，编码路径会自动匹配。

## 2.5 `hw/spinal/Benchmark/CompareSquareVsSystolic2DPerf.scala`

关键位置：第 47、69、71、183 行。

必须改：

1. 第 47 行 `fpCfg` 中的 `fpConfig = ...`。

强烈建议同步改（避免目录名和日志误导）：

1. 第 69 行 `workspacePath` 中的 `fp16` 字样。
2. 第 71 行 `targetDirectory` 中的 `fp16` 字样。
3. 第 183 行 `PerfResult("SquareSystolicArray(fp16)", ...)` 的显示名。

## 3. 推荐修改顺序

1. 先改 `FpAccumPE.scala`（核心格式逻辑）。
2. 再改 `FpAccumPETester.scala`（单 PE 验证）。
3. 再改 `SquareSystolicArray.scala`（阵列配置入口）。
4. 再改 `tb_SquareSystolicArray.scala`（阵列级验证）。
5. 最后改 `CompareSquareVsSystolic2DPerf.scala`（性能对比配置与标签）。

## 4. 最短回归检查

1. `sbt compile`
2. `sbt "runMain FloatingPoint.FpxxPETest"`
3. `sbt "runMain FloatingPoint.SquareSystolicArray_Sim"`
4. `sbt "runMain Benchmark.CompareSquareVsSystolic2DPerf"`

如果第 2 步和第 3 步格式不一致，优先检查本清单第 2.1 和第 2.2 节。

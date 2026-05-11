# HWAccelerated + DL4J 混合执行器（Hybrid Fallback Mechanism）

## 问题描述
- HWAccelerated 后端只实现了 ~25-30 个 ONNX 算子（MatMul、Add、ReLU 等）
- 完整的 ONNX 标准包含数百个算子
- 某些模型可能包含不被 FPGA 支持的算子（Conv、LSTM、LayerNorm 等）
- R5 核心无法运行 Java/DL4J，所以所有 CPU 计算必须在 A53 侧进行

## 解决方案架构

```
┌────────────────────────────────────────────────────────┐
│          ONNX 模型输入（计算图）                        │
└───────────────────┬────────────────────────────────────┘
                    │
                    ▼
        ┌───────────────────────────┐
        │  HybridSequentialExecutor │
        │  (混合执行器)              │
        └───────────┬───────────────┘
                    │
          ┌─────────┴──────────┐
          │                    │
          ▼                    ▼
    ┌──────────────┐      ┌──────────────┐
    │  HWAccelerated│      │   DL4J       │
    │  OperatorSets│      │ OperatorSets │
    │  (优先)      │      │ (回退)       │
    └──────┬───────┘      └──────┬───────┘
           │                     │
           │ 支持的算子          │ 不支持的算子
           │ (MatMul, Add等)    │ (Conv, LSTM等)
           │                    │
           ▼                    ▼
    ┌──────────────────────────────────┐
    │     JNI → R5 → FPGA 加速计算      │  或  │  A53 CPU 计算 (DL4J)  │
    │     (通过 RPMsg)                  │      │                        │
    └──────────────────────────────────┘      └────────────────────────┘
           │                                         │
           └─────────────────┬──────────────────────┘
                             │
                             ▼
                      ┌─────────────────┐
                      │  中间结果缓存    │
                      │  (Session)      │
                      └────────┬────────┘
                               │
                               ▼
                      ┌─────────────────┐
                      │   最终输出结果   │
                      └─────────────────┘
```

## 执行流程

### 1. 初始化阶段
```
创建 Model
    ↓
创建 HWAcceleratedBackend（主后端）
    ↓
创建 DL4JBackend（备用后端）
    ↓
获取 HWAccelerated OperatorSets
    ↓
获取 DL4J OperatorSets
    ↓
创建 HybridSequentialExecutor，注入 DL4J OperatorSets
```

### 2. 执行阶段
对于计算图中的每个节点：

```
for each Node in TopologicalOrder:
    
    获取节点输入
        ↓
    【尝试 HWAccelerated 后端】
        ↓
    Catch UnsupportedOperationException:
        │
        ├─ 记录该算子为不支持
        │
        └─ 【切换到 DL4J 后端】
               ↓
           Catch Exception:
               └─ 抛出异常，两个后端都不支持
    
    保存输出到 Session 缓存
        ↓
    继续执行下一个节点
```

### 3. 具体代码流程

**HybridSequentialExecutor.handle() 方法：**

```java
private void handle(Session<T_BK_TS> session, OperatorSets opsets, Node node) {
    // 1. 获取节点的输入
    Inputs inputs = new Inputs();
    for (String inputName : node.getInputNames()) {
        Input input = Input.wrap(inputName, node, session.getIntermediateOutput(inputName));
        inputs.append(input);
    }

    try {
        // 2. 首先尝试 HWAccelerated 后端（FPGA 加速）
        outputs = super.handle(session, opsets, node, inputs);
        
    } catch (UnsupportedOperationException e) {
        // 3. 如果 HWAccelerated 不支持，切换到 DL4J（CPU 计算）
        System.out.printf("HWAccelerated 不支持 %s，回退到 DL4J\n", node.getOpType());
        unsupportedOps.add(node.getOpType());
        
        try {
            // 4. 使用 DL4J OperatorSets 重新执行
            outputs = super.handle(session, fallbackOpsets, node, inputs);
            
        } catch (Exception dlException) {
            // 5. DL4J 也不支持，则执行失败
            throw new RuntimeException("两个后端都不支持: " + node.getOpType(), dlException);
        }
    }

    // 6. 保存输出结果供后续节点使用
    for (Outputs.Output output : outputs.get()) {
        session.putIntermediateOutput(output.getName(), (T_BK_TS) output.getTensor());
    }
}
```

## 支持的算子对应关系

### HWAccelerated 支持的算子（~25-30 个）
- 矩阵运算: MatMul, Gemm, GemmRelu
- 算术运算: Add, Sub, Mul (via Gemm), Max, Greater, Neg
- 激活函数: ReLU, Exp, Log, Softplus, NegSoftplus, AddExp, AddLog, SubExp
- 张量操作: Reshape, Transpose, Concat, Slice, Tile, Expand, Where
- 量化: QuantizeLinear, DequantizeLinear
- 其他: Cast, ReduceMax

### DL4J 支持的算子（完整 ONNX 算子集）
- Conv, ConvTranspose
- LSTM, GRU, RNN
- LayerNormalization, BatchNormalization
- Sigmoid, Tanh, LeakyReLU
- 以及数百个其他标准 ONNX 算子

### 回退优先级
```
HWAccelerated（FPGA 加速）
    ↓
    如果不支持
    ↓
DL4J（A53 CPU）
    ↓
    如果不支持
    ↓
执行失败
```

## 性能影响

### 性能对比
| 执行位置 | 速度 | 延迟 | 能耗 |
|--------|------|------|------|
| FPGA (HWAccelerated) | ★★★★★ | ★★★★★ | ★★★★★ |
| A53 CPU (DL4J) | ★★☆☆☆ | ★★★☆☆ | ★★☆☆☆ |

### 优化建议
1. **分析模型**：确认模型中哪些算子需要回退
2. **分流计算**：将不支持的算子聚合在一起，减少切换次数
3. **预处理**：在模型加载时识别不支持的算子，提前通知用户
4. **批量优化**：如果有多个不支持的算子，可能需要专门优化 CPU 路径

## 内存管理

### 内存布局
```
┌─────────────────────────────────────────┐
│  A53 DDR 内存 (1GB+)                    │
├─────────────────────────────────────────┤
│  ND4J 张量 (DL4J)                       │
├─────────────────────────────────────────┤
│  共享内存 (1MB) ← RPMsg <→ R5            │
├─────────────────────────────────────────┤
│  HWAccelerated 中间结果                  │
├─────────────────────────────────────────┤
│  计算图执行状态 (Session)                │
└─────────────────────────────────────────┘
```

### 数据流
1. 输入数据进入 Session
2. HWAccelerated 算子：数据 → FPGA → 结果
3. DL4J 算子：数据 → ND4J 张量 → CPU 计算 → 结果
4. 结果存储在 Session 中供下一个节点使用

## 调试和监控

### 启用日志
```java
// HybridSequentialExecutor 会自动打印：
// - 每个节点的执行情况
// - 哪些算子被回退到 DL4J
// - 最终统计信息
```

### 获取统计信息
```java
Set<String> unsupportedOps = executor.getUnsupportedOps();
// 返回所有被回退的算子类型
```

### 典型输出
```
[HybridExecutor] 执行节点: input_node (OpType: Identity)
[HybridExecutor] 执行节点: dense_1 (OpType: MatMul)
[HybridExecutor] 执行节点: bias_add (OpType: Add)
[HybridExecutor] 执行节点: activation (OpType: Sigmoid)
[HybridExecutor] HWAccelerated 不支持 Sigmoid，回退到 DL4J 后端
[HybridExecutor] 执行节点: output (OpType: Identity)

[HybridExecutor] 以下算子已回退到 DL4J 后端：
  - Sigmoid
```

## 潜在问题与解决方案

### 问题 1: 数据类型不匹配
**症状**: 回退到 DL4J 后数据类型转换失败

**原因**: HWAccelerated 使用固定点整数（INT24/INT8），DL4J 可能期望浮点数

**解决方案**: 在 HybridSequentialExecutor 中添加类型转换层
```java
// 在回退前进行类型转换
INDArray convertedInput = convertDataType(input, DL4J_DTYPE);
```

### 问题 2: 形状兼容性
**症状**: 后端在处理张量形状时失败

**原因**: 某些算子在 HWAccelerated 中形状限制不同

**解决方案**: 在 handle 方法中验证形状
```java
validateShapeCompatibility(node, inputs, fallbackOpsets);
```

### 问题 3: 性能下降
**症状**: 引入回退机制后性能明显下降

**原因**: 频繁在 HWAccelerated 和 DL4J 之间切换

**解决方案**: 
- 分析模型，识别瓶颈算子
- 考虑为常用的不支持算子在 FPGA 中专门实现
- 或使用模型量化/裁剪减少需要的算子类型

## 后续优化方向

### 1. 算子专项实现
对于频繁回退的算子（如 Conv、Sigmoid），考虑在 FPGA 中专门实现

### 2. 智能调度
基于性能数据动态选择执行路径：
```
如果 (算子_计算量 < 阈值) {
    使用 DL4J（避免 JNI 开销）
} else {
    使用 FPGA（如果支持）
}
```

### 3. 预处理优化
在模型加载时进行分析：
```java
// 识别不支持的算子
List<String> unsupportedOps = analyzeModel(model);
if (unsupportedOps.size() > threshold) {
    warn("模型包含过多不支持的算子，建议使用纯 DL4J 后端");
}
```

## 总结

HybridSequentialExecutor 提供了一个灵活的解决方案，使 HWAccelerated 后端可以自动处理任何 ONNX 模型，即使它包含 FPGA 不支持的算子。这种方法结合了 FPGA 加速的性能优势和 CPU 计算的通用性。

# Scala Runtime 架构手册

## 概览

Scala Runtime 是一个纯 Scala 实现的 ONNX 推理框架，通过 Python/PyTorch 桥接器执行浮点算子，同时可将量化定点算子分发到 FPGA 加速器（SW参考模型 / VCS / Verilator）。

## 包结构

```
sw/scala/
├── main/scala/runtime/
│   ├── bridge/                     # Python 桥接层
│   │   └── TorchBridge.scala       # JSON-line 协议与 PyTorch 子进程通信
│   ├── engine/                     # 核心运行时引擎
│   │   ├── OnnxGraph.scala         # 图数据模型（GraphNode, OnnxGraph, IOInfo, AttrValue）
│   │   ├── OnnxLoader.scala        # ONNX 模型加载器
│   │   └── Session.scala           # 推理会话 + HWDispatcher trait
│   ├── dispatch/                   # HW 后端分发器
│   │   ├── AcceleratorDispatcher.scala  # 分发框架 + DispatchContext 实现
│   │   ├── VCSDispatcher.scala          # VCS RTL 仿真后端
│   │   ├── VerilatorDispatcher.scala    # Verilator RTL 仿真后端
│   │   └── ops/                         # 可插拔算子库
│   │       ├── HWOp.scala               # HWOp trait + DispatchContext trait
│   │       ├── MatMulOp.scala           # 矩阵乘法（2D/3D/Batch）
│   │       ├── ElementWiseOp.scala      # 逐元素运算（Add, Sub, Max）
│   │       ├── ActivationOp.scala       # 激活函数（Relu, Exp, Log, Softplus）
│   │       ├── NegOp.scala              # 取反（纯软件）
│   │       ├── ConcatOp.scala           # 拼接（带移位对齐）
│   │       ├── GemmOp.scala             # GEMM = MatMul + Bias（可选 Relu）
│   │       └── FusedOp.scala            # 融合算子（NegSoftplus, AddExp 等）
│   └── app/                        # 应用入口
│       └── RuntimeDemo.scala       # 命令行演示程序
└── test/scala/runtime/
    ├── engine/
    │   └── RuntimeTestSuite.scala       # MNIST 纯 PyTorch 推理测试
    └── dispatch/
        └── HWBackendTestSuite.scala     # FPGA HW 后端验证测试
```

## 核心模块说明

### `runtime.engine` — 核心引擎

| 文件 | 职责 |
|------|------|
| **OnnxGraph.scala** | 定义图的数据模型。`GraphNode` 表示一个 ONNX 算子节点（包含 name, opType, inputs, outputs, attrs）。`OnnxGraph` 封装完整计算图（nodes, initializers, inputs, outputs），提供拓扑排序 `topologicalOrder` 和生产者映射 `producerMap`。`AttrValue` 是密封特质层次结构，支持 Int/Float/String/Tensor 等属性类型。 |
| **OnnxLoader.scala** | 通过 Python 桥接器解析 ONNX 文件，返回 `OnnxGraph`。Python 端使用 `onnx` 库读取 protobuf，通过 JSON 传回节点/初始化器/IO 信息。 |
| **Session.scala** | 推理会话。`feed()` 喂入输入张量，`forward()` 按拓扑序逐节点执行。支持可选的 `HWDispatcher`：若节点满足 HW 分发条件则走加速器，否则走 PyTorch。`HWDispatcher` trait 定义两个方法：`shouldDispatch(node)` 和 `execute(node, inputIds, graph)`。 |

### `runtime.dispatch` — HW 后端

| 文件 | 职责 |
|------|------|
| **AcceleratorDispatcher.scala** | HW 分发框架。实现 `HWDispatcher` trait 和 `DispatchContext` trait。负责：分发策略（`shouldDispatch` 基于 `fpga_in_shift`/`fpga_out_shift`）、数据搬运（`pullToLong2D`/`pushFromLong2D` Base64 编码传输）、移位对齐、分块逻辑（`tiledHWOp`/`tiledElementOp`）。通过算子注册表将具体算子执行委托给 `ops/` 中的 `HWOp` 实现。 |
| **VCSDispatcher.scala** | VCS RTL 仿真后端。继承 `AcceleratorDispatcher`，重写 `runOneInst` 调用 Synopsys VCS 仿真器。支持在线交叉检查（每个 tile 与 SW 参考对比）。 |
| **VerilatorDispatcher.scala** | Verilator RTL 仿真后端。与 VCS 类似但使用 Verilator（速度更快）。 |

### `runtime.dispatch.ops` — 可插拔算子库

每个算子独立文件，实现 `HWOp` trait，通过注册表机制自动集成到分发流程。

| 文件 | 算子类型 | 说明 |
|------|---------|------|
| **HWOp.scala** | — | 定义 `HWOp` trait（`opTypes` + `execute()`）和 `DispatchContext` trait（算子访问共享基础设施的接口）|
| **MatMulOp.scala** | MatMul | 矩阵乘法，支持 2D×2D、3D×2D（展平 batch）、3D×3D（逐 batch 切片）。含分块、填充、移位对齐。 |
| **ElementWiseOp.scala** | Add, Sub, Max | 逐元素运算，含广播、移位对齐、分块。映射到 HW 的 elementadd/elementsub/elementmax 指令。 |
| **ActivationOp.scala** | Relu, Exp, Log, Softplus | 激活函数。输入预移位到 `hwFracWidth`，输出后移位到目标精度。支持浮点旁路模式（`useFloatActivations`）用于诊断。 |
| **NegOp.scala** | Neg | 取反，纯软件实现（不调用 HW）。 |
| **ConcatOp.scala** | Concat | 拼接。将每个输入重新缩放到输出移位后，在 PyTorch 中执行 concat。 |
| **GemmOp.scala** | Gemm, GemmRelu | GEMM = 可选转置 + MatMul + 偏置加法。GemmRelu = Gemm + Relu。通过 `dispatchToOp` 调用其他算子。 |
| **FusedOp.scala** | NegSoftplus, AddExp, AddLog, SubExp | 融合算子。组合已有算子实现：如 AddExp = Add + Exp。通过 `dispatchToOp` 链式调用。 |

### `runtime.bridge` — Python 桥接

| 文件 | 职责 |
|------|------|
| **TorchBridge.scala** | 管理 Python 子进程（`torch_bridge.py`），通过 JSON-line 协议双向通信。提供 `call(method, params)` RPC 接口。`Tensor` 类封装远程 Python 张量引用，支持 `toFloatArray()`、`op()`、`rand()` 等操作。 |

## 使用方式

### 运行 Demo

```bash
sbt "runMain runtime.app.RuntimeDemo path/to/model.onnx [--debug] [--hw]"
```

### 运行测试

```bash
# 纯 PyTorch 推理测试（MNIST）
sbt "testOnly runtime.engine.RuntimeTestSuite"

# HW 后端验证测试（FPGA 模型）
sbt "testOnly runtime.dispatch.HWBackendTestSuite"

# 使用 Verilator RTL 仿真（通过 JVM 参数）
sbt 'set Test/javaOptions += "-Dhw.backend=verilator"' \
    "testOnly runtime.dispatch.HWBackendTestSuite"
```

### 切换 HW 后端

在 `HWBackendTestSuite.scala` 中修改 `HW_BACKEND` 常量：

```scala
private val HW_BACKEND = "sw"        // SW 参考模型（快速）
private val HW_BACKEND = "verilator" // Verilator RTL 仿真
private val HW_BACKEND = "vcs"       // Synopsys VCS RTL 仿真
```

或通过 JVM 系统属性 `-Dhw.backend=...` 覆盖。

## 数据流

```
ONNX 文件 → OnnxLoader.load() → OnnxGraph
                                    ↓
                              Session(graph)
                                    ↓
                        session.feed(name, tensor)
                                    ↓
                 session.forward(Some(hwDispatcher))
                        ↓                    ↓
              HWDispatcher.execute()    TorchBridge.call("run_node")
              (定点整数运算)              (浮点 PyTorch 运算)
                        ↓                    ↓
                   session.getOutputs → Map[name, Tensor]
```

## 扩展指南

### 添加新 HW 算子

在 `runtime.dispatch.ops` 包中创建新文件：

```scala
package runtime.dispatch.ops
import runtime.engine._

class MyNewOp extends HWOp {
  override val opTypes: Set[String] = Set("MyOpType")

  override def execute(opType: String, node: GraphNode, inputIds: Seq[String],
                       graph: OnnxGraph, ctx: DispatchContext): Seq[String] = {
    // 通过 ctx 访问基础设施:
    //   ctx.pullToLong2D(id, rows, cols) — 获取定点数据
    //   ctx.pushFromLong2D(data, rows, cols, dtype) — 推送结果
    //   ctx.tiledHWOp(...) / ctx.tiledElementOp(...) — 分块执行
    //   ctx.dispatchToOp("OtherOp", node, inputs, graph) — 调用其他算子
    //   ctx.getProducerOutputShift(graph, name, default) — 查询移位
    //   ctx.HW_DIM_MULTIPLE, ctx.hwFracWidth — 常量
    ???
  }
}
```

然后在 `AcceleratorDispatcher` 中注册（构造函数已自动注册默认算子）：

```scala
val dispatcher = new AcceleratorDispatcher(bridge)
dispatcher.registerOp(new MyNewOp())  // 注册新算子
dispatcher.unregisterOp("OldOp")     // 移除旧算子（可选）
println(dispatcher.registeredOpTypes) // 查看所有已注册算子
```

### 添加新 RTL 后端

1. 在 `runtime.dispatch` 包中创建新文件，继承 `AcceleratorDispatcher`
2. 重写 `runOneInst()` 方法，调用你的仿真接口
3. 可选：添加在线交叉检查逻辑

### 添加新的调试特性

`AcceleratorDispatcher` 提供多个调试开关：
- `_debugLimit` / `_debugNodeNames` — 打印指定节点的详细信息
- `_digestMode` — 摘要模式，打印每个 HW 节点的统计概要
- `_validateRoundTrip` — 验证 Scala↔Python 数据传输的往返一致性
- `_useFloatActivations` — 使用浮点激活函数替代 HW 实现（诊断用）
- `onNodeComplete` — 节点完成回调，用于在线对比（如 FPGA vs ORT 交叉验证）

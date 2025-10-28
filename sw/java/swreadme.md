# 软件文档（以 modelTest 测试流程为核心）

本项目的软件部分基于 **Forwarder Framework**，用于统一加载 ONNX 模型、执行推理计算、保存中间结果，并验证算子正确性。  
系统通过抽象层、算子层、后端层、执行器层分层设计，并通过一个完整的 **`modelTest()` 测试流程**，覆盖了整个软件栈的所有核心模块。

---

## 🧩 软件总体结构

软件由以下五层构成（按调用和逻辑顺序排列）：

| 层级 | 说明 |
|------|------|
| **测试层 (Testing Layer)** | 覆盖整个框架，用于验证模型在不同后端的一致性。包含 `FWTestCase` 和 `ModelTest`，负责加载输入、执行推理、保存中间结果和验证输出。 |
| **抽象层 (Core Abstraction Layer)** | 提供统一的模型和张量管理接口，如 `Forwarder`、`Model`、`Tensor`。解耦上层逻辑与底层硬件实现。 |
| **执行器层 (Executor Layer)** | 负责调度算子执行，例如 `SequentialExecutor` 管理算子执行顺序和依赖关系。 |
| **后端层 (Backend Layer)** | 执行实际计算并管理推理会话，如 CPU/GPU/FPGA 后端，实现输入绑定、前向推理和中间结果保存。 |
| **算子层 (Operator Layer)** | 实现具体算子逻辑（Add、MatMul、Conv 等），描述输入输出依赖和计算规则。 |

---

## 🚀 `modelTest()` 测试流程（核心部分）

`modelTest()` 是整个软件系统的中枢函数，  
负责从模型加载到输出验证的完整流程。

### 流程概览

```plaintext
+---------------------------+
| 1. 加载模型 (ONNX)         |
+-------------+-------------+
              |
              v
+---------------------------+
| 2. 初始化 Forwarder        |
| (抽象层)                  |
+-------------+-------------+
              |
              v
+---------------------------+
| 3. 输入绑定与预处理       |
| (测试层加载输入 Tensor)    |
+-------------+-------------+
              |
              v
+---------------------------+
| 4. Executor.run()         |
| (执行器层)                |
| 管理算子调度和依赖        |
+-------------+-------------+
              |
              v
+---------------------------+
| 5. Backend.forward()      |
| (后端层)                  |
| 执行实际计算并生成中间输出 |
+-------------+-------------+
              |
              v
+---------------------------+
| 6. 保存中间输出结果       |
| (binary / pb 格式)        |
+-------------+-------------+
              |
              v
+---------------------------+
| 7. 输出验证与结果比对     |
| (测试层与 golden 输出对比)|
+---------------------------+

```
# 抽象层文档

## 🧩 概述

抽象层（Core Abstraction Layer）是 Forwarder 框架的基础层，负责提供统一的模型管理和张量操作接口。  
它的核心目标是将上层逻辑与具体后端实现解耦，使得不同后端（如 DL4J、TensorFlow 等）能够透明地进行模型推理计算。  
主要功能包括：

- 模型加载和管理
- 输入张量绑定与验证
- 输出张量获取与管理
- 中间结果缓存与资源管理
- 会话（Session）生命周期管理

抽象层的核心类包括：

- `Forwarder`：全局模型管理器，负责模型加载与注册
- `Model`：单个模型实例，管理算子、后端和执行器
- `Session`：推理会话，管理一次完整的推理过程（输入→前向计算→输出）
- `Tensor`：统一张量表示，用于存储数据
- `Config`：配置类，定义张量分配、字节序、执行器等参数
- `Backend`：抽象后端接口，定义张量转换、会话创建等方法

---
## 🚀 核心流程

抽象层的完整流程如下：

1. **加载模型**
    - 使用 `Forwarder.load(onnxModelPath, config)` 加载 ONNX 模型。
    - 返回 `Model` 实例，并注册到全局 `modelSet` 中。
    - 核心动作：
        - 解析 ONNX 模型结构
        - 初始化模型的默认执行器
        - 准备算子集合（Opsets）
```java
Model loadedModel = forwarder.load(absoluteModelPath,cfg).executor(SequentialExecutor.class); //FWTestCase.java
```
```java
public static Model load(String onnxModelPath, Config config) {
String modelId = UUID.randomUUID().toString();
Model model = new Model(modelId, onnxModelPath, config);
modelSet.putIfAbsent(modelId, model);
return model;//Forwarder.java
}
```
2. **准备输入**
    - 在 `Session` 中使用 `feed()` 方法绑定输入张量。
    - 输入张量验证：
        - 名称必须在模型计算图中定义
        - 数据类型和形状必须匹配
    - 输入张量会被转换为后端原生张量，并存入中间结果缓存
```java
session.feed(input, false);  //FWTestCase.java
```
```java
public Session<T_BK_TS> feed(String name, Tensor tensor, boolean autoAttach) {
    //获取计算图的输入
    GraphInput graphInput = this.backend.getModel().getGraph().getInputs(name);
    if (graphInput == null) {
        throw new IllegalArgumentException(String.format("Input named \"%s\" had not be defined in graph", name));
    } else {
        //检查输入的tensor的dataType和shape是否和网络定义的一致
        if (!tensor.equals(graphInput.getValueInfo())) {
            throw new IllegalArgumentException(
                    String.format("Shape or DataType is not equals to the input tensor named \"%s\" ", name));
        }
    }
    //转换为后端原生数据类型T_BK_TS
    T_BK_TS backendTensor = this.backend.toBackendTensor(this.intermediateTensorManager, tensor);
    //将输入作为中间结果存入一个map中，用name作为key
    this.intermediateOutputs.put(name, backendTensor);
    //默认会把输入的Tensor类型也存起来
    if (autoAttach) {
        this.exchangeTensorManager.attach(name, tensor);
    }
    return this;
}//Session.java
```

3. **执行推理**
    - 调用 `Session.forward()`：
        - 将常量张量加载到会话
        - 通过执行器递归执行算子
        - 逐步更新中间结果
    - 计算完成后，输出张量会被转换回前端 `Tensor` 类型，封装到 `Outputs` 中
```java
session.forward(); //FWTestCase.java
```
```java
public Session<T_BK_TS> forward() {
    this.intermediateOutputs.putAll(this.backend.getTensorManager().get());

    //从后端获取执行器
    Executor<T_BK_TS> executor = this.backend.getModel().getExecutor();
    //递归执行推理
    executor.execute(this, this.backend.getOpsets());
    // 处理输出：将后端张量转换回前端Tensor并封装为输出结果
    for (GraphOutput graphOutput : this.backend.getModel().getGraph().getOutputs()) {
        T_BK_TS backendTensor = this.intermediateOutputs.get(graphOutput.getName());//从中间结果获取所有名字和网络需要的输出一致的张量
        Tensor tensor = this.backend.toNativeTensor(this.exchangeTensorManager, graphOutput.getName(),
                backendTensor);//转换回Tensor类型
        Output output = Output.wrap(graphOutput.getName(), tensor);//封装为输出对象并存入结果集
        outputs.append(graphOutput.getName(), output);
    }
    return this;
}//Session.java
```


4. **获取输出**
    - 使用 `Session.getOutput(name)` 获取最终输出张量
    - 可获取单个输出或所有输出
    - 支持获取中间计算结果：
        - `getIntermediateOutput(name)` 返回后端类型
        - `getIntermediateOutputTensor(name)` 返回前端 `Tensor`

5. **资源释放**
    - 会话实现 `AutoCloseable`，使用 `close()` 方法释放：
        - 中间张量管理器
        - 交换张量管理器
        - 会话线程本地绑定
    - 模型关闭会自动释放相关后端资源

---

## 🧩 核心类说明

### 1. Model
- **作用**：表示一个加载的 ONNX 模型，负责管理执行器和后端实例。
- **核心方法**：
    - `executor(Class<? extends Executor> classOfExecutor)`：初始化执行器。
    - `backend(String name)`：获取或创建指定后端实例。
    - `getExecutor()` / `getConfig()` / `getId()`：获取执行器、配置、模型 ID。
    - `close()`：释放所有后端资源并清理模型引用。

**调用关系**：`Forwarder.load()` → `new Model(...)` → `executor()` → `backend()`

---

### 2. Session<T_BK_TS>
- **作用**：一次完整的模型推理会话，负责输入绑定、前向推理、输出处理和中间结果管理。
- **核心流程**：
    1. **绑定会话**：
        - 每个线程使用 `ThreadLocal<Session<?>>` 绑定独立会话，确保线程安全。
    2. **输入准备**：
        - `feed(Tensor tensor)` 或 `feed(String name, Tensor tensor, boolean autoAttach)`
            - 检查输入名称是否存在于 `GraphInput`
            - 检查数据类型和 shape 是否匹配
            - 转换为后端张量 `T_BK_TS` 并存入 `intermediateOutputs`
            - 可选择将前端 `Tensor` 存入 `exchangeTensorManager`
    3. **前向推理**：
        - 调用 `forward()`
            - 将后端常量张量加入中间结果集合
            - 获取 `Executor` 并执行 `executor.execute(this, backend.getOpsets())`
            - 将后端张量转换为前端 `Tensor` 并封装到 `Outputs`
    4. **获取输出**：
        - `getOutput(String name)`：前端 Tensor 输出
        - `getIntermediateOutput(String name)`：后端张量
        - `getIntermediateOutputTensor(String name)`：后端张量转换为前端 Tensor
    5. **资源释放**：
        - `close()`：释放中间张量管理器、交换张量管理器，并解除线程绑定

---

### 3. Tensor
- **作用**：前端统一张量类型，封装数据缓冲区和类型信息。
- **核心功能**：
    - 支持 `DIRECT` 或 `HEAP` 分配模式
    - 提供 shape、data type、元素总数查询
    - 提供 `getData()` 获取只读 ByteBuffer
    - `close()` 释放内存，防止泄漏

---

### 4. Config
- **作用**：模型和会话的配置管理，使用 Builder 模式初始化。
- **配置项**：
    - 执行器类型 `executor`（默认 `RayExecutor`）
    - 张量分配模式 `AllocationMode`
    - 内存字节序 `ByteOrder`
    - 调试开关 `isDebug`
- **方法**：
    - `getTensorOptions()`：生成 Tensor 初始化选项
    - `getExecutor()` / `isDebug()` / `getMemoryAllocationMode()` / `getMemoryByteOrder()`

---

### 5. Backend<T_TS>
- **作用**：负责实际计算执行和后端资源管理。
- **核心功能**：
    - 通过 `toBackendTensor()` 将前端 Tensor 转为后端张量
    - 通过 `toNativeTensor()` 将后端张量转换回前端 Tensor
    - 管理中间张量 `TensorManager<T_TS>`，负责资源释放
    - 初始化常量张量
    - 创建会话 `newSession()`
    - 获取 Opsets（算子集合）
- **重要方法**：
    - `disposeBackendTensor(T_TS tensor)`：释放后端张量资源
    - `getName()`：后端名称，例如 TensorFlow、DL4J

---

## 🔗 抽象层调用链

```text
Forwarder.load()
    └─> Model
            ├─> executor() → 初始化执行器
            ├─> backend() → 创建或获取后端
            │       └─> Backend → 管理张量、Opsets、常量
            └─> Session.newSession()
                    ├─> feed() → 输入绑定
                    ├─> forward() → 前向推理
                    └─> getOutput()/getIntermediateOutputTensor()
```
---
# 执行器层文档
参考代码路径：/home/user/Workspace/livehps_1/sw/java/main/java/org/forwarder/executor
## 一、层级定位与总体职责

执行器层（Executor Layer）位于系统的第三层，处于抽象层（Session / Model 管理）和算子实现层（Operator Backend）之间。

- **上层接口：** 由 `Session` 调用，用于控制模型执行。
- **核心职责：**
  - 负责解析 ONNX 模型的图结构；
  - 确定节点（Node）的执行顺序；
  - 调用相应的算子实现执行每个节点；
  - 维护中间结果的传递；
  - 将最终结果返回给 Session。

执行器层的关键点是**如何遍历并调度 ONNX 图中的节点**。  
为此，框架中提供了三种不同的执行策略：

| 类名 | 执行策略 | 特点 |
|------|------------|------|
| `RecursionExecutor` | 递归执行 | 自顶向下递归遍历依赖图 |
| `RayExecutor` | 拓扑排序 + 反向回溯 | 从输出节点反向遍历前驱节点 |
| `SequentialExecutor` | 拓扑排序 + 正向执行 | 预先排序所有节点后依次执行（推荐） |

---

## 二、核心类结构

执行器层的基础类是：

```java
package org.forwarder.executor;
public abstract class Executor<T_BK_TS> {
    protected Model model;

    public Executor(Model model) {
        this.model = model;
    }
    public abstract void execute(Session<T_BK_TS> session, OperatorSets opsets);
}
```
所有具体执行器（如 SequentialExecutor、RayExecutor 等）都继承该类，并实现 execute 方法。
## 三、ExecutorFactory 

ExecutorFactory 负责根据模型创建指定类型的执行器实例。
```java
package org.forwarder.executor;
import java.lang.reflect.Constructor;
import org.onnx4j.Model;
public class ExecutorFactory {

    public static Executor<?> createInstance(Model model, Class<? extends Executor> classOfExecutor)
            throws Exception {
        Constructor<? extends Executor> c = classOfExecutor.getConstructor(Model.class);
        return c.newInstance(model);
    }
}
```
## 四、RayExecutor
RayExecutor 使用拓扑结构保证节点按依赖顺序执行。
它从输出节点反向追踪所有依赖（前驱节点），形成一个有序的执行序列。
```java
@Override
public void execute(Session<T_BK_TS> session, OperatorSets opsets) {
    for (Node node : this.orderedSequenceNodes) {
        this.handle(session, opsets, node);
    }
}
```
执行流程：

1.调用 toOrderedSequenceNodes() 获取拓扑排序后的节点序列；

2.依次执行每个节点；

3.将每个节点的输出存入 session 的中间结果中；

4.后续节点从 session 获取输入张量。
```java
private Collection<Node> toOrderedSequenceNodes(Graph graph) {
    Collection<Node> nodes = new LinkedList<Node>();
    for (GraphOutput graphOutput : graph.getOutputs()) {
        this.predecessors(nodes, graph, graphOutput.getNode());
        this.addOrderedSequenceNode(nodes, graphOutput.getNode());
    }
    return nodes;
}
```
predecessors() 方法递归追踪前驱节点并存储访问顺序，从而生成拓扑序列。

## 五、SequentialExecutor 

SequentialExecutor 是当前框架中最稳定、可控的执行方式。

它首先对模型图进行拓扑排序（防止循环依赖），然后按正向顺序依次执行节点。
```java
@Override
public void execute(Session<T_BK_TS> session, OperatorSets opsets) {
    for (Node node : this.orderedSequenceNodes) {
        this.handle(session, opsets, node);
    }
}
```
在执行过程中：

每个节点执行前，会从 session 中取出输入；

每个节点执行后，输出张量会重新写入 session；

执行顺序严格遵守依赖拓扑。

## 拓扑排序算法实现
```java
private void topologicalSortUtil(Node node, Graph graph, LinkedList<Node> orderedNodes,
                                 Set<Node> visited, Set<Node> recursionStack) {
    visited.add(node);
    recursionStack.add(node);
    Collection<Node> predecessors = graph.predecessors(node);
    if (predecessors != null) {
        for (Node predecessor : predecessors) {
            if (recursionStack.contains(predecessor)) {
                throw new IllegalStateException("Graph has a cycle, topological sort not possible.");
            }
            if (!visited.contains(predecessor)) {
                topologicalSortUtil(predecessor, graph, orderedNodes, visited, recursionStack);
            }
        }
    }
    recursionStack.remove(node);
    orderedNodes.add(node);
}
```
## 六、执行器层总体执行流程
```plaintext
            +----------------+
            |   Session      |
            +--------+-------+
                     |
                     | 调用 ExecutorFactory 创建执行器
                     v
            +----------------+
            |   Executor     |
            |  (抽象类)      |
            +--------+-------+
                     |
          +----------+----------+
          |                     |
  +---------------+   +----------------+
  | RayExecutor   |   | SequentialExec |
  +---------------+   +----------------+
          |                     |
          +----------+----------+
                     |
           执行节点 handle(node)
                     |
           +-------------------+
           | OperatorSets 调用 |
           +-------------------+
                     |
             +---------------+
             | Tensor Output |
             +---------------+
```
---
# 后端层（Backend Layer）文档
参考代码路径：/home/user/Workspace/livehps_1/sw/java/main/java/org/forwarder/backend
## 一、模块概述

**后端层（Backend Layer）** 是 Forwarder 框架的底层执行支撑部分，负责模型推理的具体实现与硬件资源调度。

主要的工作是：

- **执行模型计算**：把节点的计算操作交给底层硬件（CPU/GPU/加速器）完成；
- **管理内存**：保证张量在运行过程中有足够的内存，并在完成后释放；
- **支持多后端**：可以根据硬件类型加载不同后端，例如 ND4J 或自定义加速器；
- **张量转换**：在 ONNX 张量和后端张量之间互相转换；
- **可调试和可扩展**：支持调试模式和新的后端接入。
- 
主要组成：
```text
org.forwarder.backend
├─ BackendRegistry      # 注册和管理已安装的 Backend
├─ BackendLoader        # 扫描服务，初始化可用 Backend
├─ BackendFactory       # 根据名称动态创建 Backend 实例
├─ impls.HWAccelerated  # 实现示例：HWAcceleratedBackend + HWAcceleratedSession + DataTypeHelper
```

---

## 二、核心组件

### 2.1 BackendRegistry

- 单例枚举，用于存储和管理已注册的 Backend 类。
- 功能：
    - 注册 Backend 实例；
    - 根据名称获取 Backend 类；
    - 获取所有已注册 Backend。

```java
public enum BackendRegistry {
    Instance;

    private Map<String, Class<? extends Backend>> backends = new HashMap<>();

    public Class<? extends Backend> get(String backendName) {
        return this.backends.get(backendName);
    }

    public void register(Backend<?> backend) {
        this.backends.put(backend.getName(), backend.getClass());
        logger.info("Backend named \"{}\" has installed", backend.getName());
    }

    public Map<String, Class<? extends Backend>> get() {
        return this.backends;
    }
}
```
---
### 2.2 BackendLoader

负责扫描和初始化所有可用 Backend（基于 ServiceLoader）。

功能：

自动扫描实现了 Backend 接口的服务；

将发现的 Backend 注册到 BackendRegistry；

保证初始化只执行一次。

---
### 2.3 BackendFactory

根据 Backend 名称创建具体 Backend 实例。

功能：

从 BackendRegistry 获取对应 Backend 类；


允许动态加载不同硬件后端。

---
### 2.4 HWAcceleratedBackend

继承自 Backend<INDArray>，提供 ND4J 后端实现。

功能：

把模型里的计算节点交给 ND4J 处理；

提供 Session 来管理内存；

负责 ONNX 张量和 ND4J 张量互转。

---
### 2.5 HWAcceleratedSession

管理 ND4J Workspace 并绑定到 Backend。

功能：

分配和管理内存 Workspace；

绑定当前线程 Session；

关闭时释放资源。

---
### 2.6 HWAcceleratedDataTypeHelper（数据类型转换）

作用：ONNX 张量数据类型 ↔ ND4J 后端张量数据类型的映射。

它的作用是 在 ONNX 张量数据类型和后端（ND4J/HWAccelerated）张量数据类型之间进行映射。

这种数据类型映射是 后端特有的实现细节，与具体算子（Operator/Executable）无关。算子层只关心输入输出张量的类型，但不负责类型转换。

在执行器层（Executor）或算子层调用时，需要先通过后端将 ONNX Tensor 转换为后端张量，或者将后端张量转换回 ONNX Tensor，
HWAcceleratedDataTypeHelper 就是在这个转换过程中使用的工具类。

## 三、后端层总体执行流程
```text
A[用户调用 execute()] --> B[Executor 层按顺序遍历节点]
B --> C{每个节点 Node}
C --> D[获取输入张量 Input]
D --> E{Session 中是否已有中间结果?}
E -- 是 --> F[直接使用中间结果]
E -- 否 --> G[递归或顺序调用前驱节点计算]
F --> H[调用 Backend 处理节点计算]
G --> H
H --> I[Backend 根据类型转 ONNX Tensor ↔ 后端张量]
I --> J[执行实际计算，生成输出张量 Output]
J --> K[将输出存入 Session 中的中间结果]
K --> L[下一个节点计算]
L --> M[所有节点计算完成，收集最终输出]
M --> N[返回给用户或进一步处理]
```

# 算子层文档
## 获取 ONNX 算子的实际行为
可参考 ONNX Python 库文档 https://onnx.ai/onnx/intro/python.html

## 流程
```text
ONNX Node ----> 算子层 (Operator)
Input: 后端张量
      |
执行 forward() -> 计算输出
      |
Output: 后端张量
```

## 已实现的算子
项目支持广泛的ONNX算子，包括但不限于：

| 算子类别 | 支持的算子 |
|---------|-----------|
| 基础数学运算 | Add, Sub, Mul, Div, Exp, Log, Neg |
| 线性代数 | MatMul, GeMM |
| 激活函数 | Relu, Softplus |
| 张量操作 | Reshape, Concat, Slice, Transpose, Expand, Tile |
| 规约操作 | ReduceMax, Max, Sum |
| 比较操作 | Greater, Where |
| 其他 | Cast, Constant, Identity, Shape, Squeeze, Unsqueeze |

## 算子版本支持
项目支持多个ONNX算子集版本（V1-V13），具体支持情况请参考代码中的算子实现。

## 添加onnx标准中已有的新算子的指南

### 步骤概述
1. **添加算子接口**: 在相应的算子集目录中创建接口定义
2. **实现算子逻辑**: 在后端实现中编写具体逻辑
3. **注册算子**: 在算子集初始化器中注册新算子
4. **验证实现**: 运行测试确保算子正确工作
5. **编写测试**: 创建完整的测试用例

### 详细流程
参考项目中的现有算子实现，如Add算子：
- 接口位置: `sw/java/main/java/org/onnx4j/opsets/domain/aiOnnx/v6/ops/AddV6.java`
- 实现位置: `sw/java/main/java/org/forwarder/backend/impls/dl4j/opsets/aiOnnx/v6/ops/DL4JAddV6.java`
- 注册位置: 相应的算子集初始化器文件

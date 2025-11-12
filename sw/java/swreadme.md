# 软件文档（以 modelTest 测试流程为核心）

本项目的软件部分基于 **Forwarder Framework**，用于统一加载 ONNX 模型、执行推理计算、保存中间结果，并验证算子正确性。  
系统通过抽象层、算子层、后端层、执行器层分层设计，并通过一个完整的 **`modelTest()` 测试流程**，覆盖了整个软件栈的所有核心模块。

## 软件总体结构

软件由以下五层构成（按调用和逻辑顺序排列）：

| 层级 | 说明 |
|------|------|
| **测试层 (Testing Layer)** | 覆盖整个框架，用于验证模型在不同后端的一致性。包含 `FWTestCase` 和 `ModelTest`，负责加载输入、执行推理、保存中间结果和验证输出。 |
| **抽象层 (Core Abstraction Layer)** | 提供统一的模型和张量管理接口，如 `Forwarder`、`Model`、`Tensor`。解耦上层逻辑与底层硬件实现。 |
| **执行器层 (Executor Layer)** | 负责调度算子执行，例如 `SequentialExecutor` 管理算子执行顺序和依赖关系。 |
| **后端层 (Backend Layer)** | 执行实际计算并管理推理会话，如 CPU/GPU/FPGA 后端，实现输入绑定、前向推理和中间结果保存。 |
| **算子层 (Operator Layer)** | 实现具体算子逻辑（Add、MatMul、Conv 等），描述输入输出依赖和计算规则。 |


## `modelTest()` 测试流程（核心部分）

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
---
<!-- forwarder（抽象层）好像不太准确 希望图表更新 -->
---

# 抽象层文档

## 概述

抽象层（Core Abstraction Layer）是 Forwarder 框架的基础层，负责提供统一的模型管理和张量操作接口。  
它的核心目标是将上层逻辑与具体后端实现解耦，使得不同后端（如 DL4J、TensorFlow 等）能够透明地进行模型推理计算。  
主要功能包括：

- 模型加载和管理
- 输入张量绑定与验证
- 输出张量获取与管理
- 中间结果缓存与资源管理
- 会话（Session）生命周期管理
---
<!-- 抽象层文档的概述写的好抽象，想看到人话版本的说明 -->
---
抽象层的核心类包括：

- `Forwarder`：全局模型管理器，负责模型加载与注册
- `Model`：单个模型实例，管理算子、后端和执行器
- `Session`：推理会话，管理一次完整的推理过程（输入→前向计算→输出）
- `Tensor`：统一张量表示，用于存储数据
- `Config`：配置类，定义张量分配、字节序、执行器等参数
- `Backend`：抽象后端接口，定义张量转换、会话创建等方法

---
## 核心流程

抽象层的完整流程如下：

1. **加载模型**
    - 使用 `Forwarder.load(onnxModelPath, config)` 加载 ONNX 模型。
    - 返回 `Model` 实例，并注册到全局 `modelSet` 中。
    - 核心动作：
        - 解析 ONNX 模型结构
        - 初始化模型的默认执行器
        - 准备算子集合（Opsets）

2. **准备输入**
    - 在 `Session` 中使用 `feed()` 方法绑定输入张量。
    - 输入张量验证：
        - 名称必须在模型计算图中定义
        - 数据类型和形状必须匹配
    - 输入张量会被转换为后端原生张量，并存入中间结果缓存

3. **执行推理**
    - 调用 `Session.forward()`：
        - 将常量张量加载到会话
        - 通过执行器递归执行算子
        - 逐步更新中间结果
    - 计算完成后，输出张量会被转换回前端 `Tensor` 类型，封装到 `Outputs` 中

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

## 核心类说明

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

## 抽象层调用链

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

---
<!-- 配套的图片我都看了，具体内容是可以看懂的，但是感觉东一榔头西一棒槌，有点缺乏整体逻辑性，所以看了很久才看懂，一张是总架构，一张变成了用户视角，一张是核心部分如何工作，一张是抽象层，而且感觉抽象层的图有点抽象
文档从抽象层突然转到核心类，太突然了-->
---

# Onnx_SpinalHDL_interface 项目详细结构

## 项目概述

本项目是一个结合 ONNX 模型推理和 SpinalHDL 硬件加速的混合架构系统。项目采用软件-硬件协同设计，使用 Java 进行 ONNX 模型推理，使用 SpinalHDL 进行硬件加速器设计，支持深度学习模型的端到端推理和硬件加速。

## 详细项目结构

### 软件部分 (sw/)

#### 1. ONNX 推理框架 (sw/java/main/java/org/onnx4j/)
- **核心模型解析**
  - `Model.java`: ONNX 模型解析核心类，负责加载和解析 ONNX 模型文件
  - `Tensor.java`: 张量表示类，处理多维数组数据和内存管理
  - `TensorManager.java`: 张量管理器，负责 Tensor 对象的生命周期管理
  - `Inputs.java` / `Outputs.java`: 输入输出张量管理

- **操作符集系统 (opsets/)**
  - 支持多版本 ONNX 算子实现 (v1-v13)
  - 包含各种数学运算、神经网络操作符
  - 提供算子接口定义和实现框架

#### 2. 推理后端 (sw/java/main/java/org/forwarder/)
- **核心推理引擎**
  - `Forwarder.java`: 推理器主类，协调模型执行
  - `Session.java`: 推理会话管理
  - `Model.java`: 模型实例管理
  - `Config.java`: 配置管理

- **后端实现 (backend/impls/)**
  - `dl4j/`: 基于 DeepLearning4J 的高性能数值计算后端
  - `tensorflow/`: TensorFlow 后端实现（支持有限）

#### 3. 应用示例 (sw/java/main/java/org/)
- `OnnxRuntime_run_MNIST.java`: MNIST 手写数字识别推理示例
  - 支持 PyTorch CNN 和 scikit-learn 模型
  - 包含数据预处理、推理执行、精度评估
  - 输出混淆矩阵和推理统计信息

#### 4. 测试框架 (sw/java/test/)
- **单元测试**
  - `ExpSimJavaTest.java`: 指数函数硬件模拟测试
  - `LNSimJavaTest.java`: 对数函数硬件模拟测试
  - `SoftplusSimJavaTest.java`: Softplus 函数硬件模拟测试
  - `MyTopLevelSimJavaTest.java`: 顶层硬件模拟测试

- **测试资源**
  - `mnist/`: MNIST 数据集和模型文件
  - `squeezenet/`: SqueezeNet 图像分类模型
  - `tiny_yolov2/`: Tiny YOLO v2 对象检测模型
  - `simple/`: 基础操作测试模型

### 硬件部分 (hw/)

#### 1. 硬件加速器核心 (hw/spinal/Accelerator/)
- **主加速器模块**
  - `Accelerator.scala`: 硬件加速器顶层设计
    - 集成切片器、数据泵、脉动阵列、激活函数等模块
    - 支持多核并行计算
    - 可配置的数据位宽和阵列尺寸
  - `AcceleratorSimInterface.scala`: 加速器仿真接口
  - `tb_Accelerator.scala`: 加速器测试平台
  - `performanceTest.scala`: 性能测试模块

#### 2. 计算核心模块

##### 2.1 脉动阵列系统 (hw/spinal/MatrixComputeUnit/)
- **二维脉动阵列 (SystolicArray2D/)**
  - `SystolicArray2D.scala`: 主脉动阵列实现
    - 支持矩阵乘法、元素级运算
    - 可配置的转置功能
    - 多级流水线设计
  - `SystolicArray2DUnit.scala`: 基本计算单元
  - `SystolicArray2DUnitSpecial.scala`: 特殊计算单元（支持元素级运算）
  - `SystolicArray2D_Wrapper.scala`: 阵列包装器，集成指令和时钟管理

- **加法器树 (AdderTree/)**
  - `adderTreeOri.scala`: 原始加法器树实现

- **元素级运算 (ElementWise/)**
  - `ElementWise.scala`: 元素级运算模块
  - `tb_ElementWise.scala`: 元素级运算测试

##### 2.2 激活函数模块 (hw/spinal/Activation/)
- `Activation.scala`: 统一激活函数模块
  - 集成指数、对数、ReLU、Softplus 函数
  - 可配置的位宽和精度
  - 支持函数选择和参数配置

##### 2.3 数学函数模块

- **指数函数 (ExponentialFunction/)**
  - `EXP_function.scala`: 硬件指数函数实现
  - `tb_EXP_function.scala`: 指数函数测试
  - LUT 生成和精度分析工具

- **对数函数 (LogarithmFunction/)**
  - `LN_function.scala`: 硬件对数函数实现
  - `tb_LN_function.scala`: 对数函数测试
  - CORDIC 算法实现

- **ReLU 函数 (ReLUFunction/)**
  - `ReLU_function.scala`: ReLU 激活函数
  - `ReLUFunctionTest.scala`: ReLU 函数测试

- **Softplus 函数 (SoftplusFunction/)**
  - `SoftplusFunction.scala`: Softplus 激活函数
  - `tb_Softplus_function.scala`: Softplus 函数测试

##### 2.4 CORDIC 算法 (hw/spinal/Cordic/)
- `cordic.scala`: 基本 CORDIC 算法实现
- `cordicDoubleRate.scala`: 双倍速率 CORDIC
- `tb_cordic.scala`: CORDIC 算法测试
- `cordicUtil.ipynb`: CORDIC 工具和算法分析

#### 3. 数据流管理模块

##### 3.1 矩阵切块和重组模块 (hw/spinal/Tiling/)
- `Slicer.scala`: 数据切片器
- `Collector.scala`: 结果收集器
- `Sdpram.scala`: 简单双端口 RAM
- `Tiling_sim.scala`: 切片和收集仿真模块

##### 3.2 数据泵系统 (hw/spinal/DataPump/)
- `DataPump_mm2s.scala`: 内存到流数据泵
- `DataPump_s2mm.scala`: 流到内存数据泵
- `DataPump_mm2mm.scala`: 内存到内存数据泵
- `MemoryPort.scala`: 内存端口定义

##### 3.3 接口系统 (hw/spinal/Interface/)
- `Interface.scala`: 通用接口定义
- `StreamDispatcher.scala`: 流分发器
- `Util.scala`: 工具函数和类型定义

#### 4. 内存管理系统 (hw/spinal/MemBlackBoxer/)
- **内存管理器 (MemManager/)**
  - `MemConfig.scala`: 内存配置
  - `MemPorts.scala`: 内存端口定义
  - `MemVendor.scala`: 厂商特定内存配置
  - `MemWrapper.scala`: 内存包装器

- **内存黑盒化 (PhaseMemBlackBoxer/)**
  - `PhaseSramConverter.scala`: SRAM 转换器
  - `Utils.scala`: 工具函数

#### 5. XPM 内存黑盒 (hw/spinal/XPM_BlackBox/)
- Xilinx 参数化宏内存实现
- 支持 SDPRAM、SPRAM、TDPRAM 等内存类型
- 提供 Verilog 包装和测试平台

#### 6. 浮点运算 (hw/spinal/FloatingPoint/)
- `fp_adder_multiplier_one_period.scala`: 单周期浮点加法和乘法
- 浮点运算单元设计和验证

#### 7. 开发测试环境 (hw/spinal/playGround/)
- 各种 SpinalHDL 功能演示和测试
- 时钟域、内存、流处理等示例代码

### 数据资源部分

#### 1. 数据集 (MNIST/)
- `data/`: MNIST 原始数据文件
- `models/`: 预训练 ONNX 模型
  - CNN、逻辑回归等模型
  - 不同 opset 版本的模型文件
- `raw/`: 原始图像和标签数据

#### 2. 推理输出 (java_outputs/)
- `java_dl4j_each_layer_outputs/`: DL4J 后端各层输出
- `java_hw_each_layer_outputs/`: 硬件模拟各层输出

#### 3. ONNX 调试工具 (onnx_debug_py/)
- Python 脚本用于调试和比较推理结果
- 张量比较、执行顺序分析等工具

### 构建和配置

#### 1. 构建配置
- `build.sbt` / `build.sc`: Scala 构建配置
- `project/`: SBT 项目配置
- `.scalafmt.conf`: 代码格式化配置

#### 2. 性能分析
- `performance_results_analyze.py`: 性能结果分析
- `performance_results.csv`: 性能数据
- 各种误差分析和比较图表

# Onnx_SpinalHDL_interface

一个ONNX模型推理与SpinalHDL硬件加速协同验证系统，实现软件-硬件协同设计架构。

## 项目概述

本项目探索了读取ONNX文件、执行模型推理、提取各层结果，并使用SpinalHDL编写的硬件模块进行验证的完整流程。系统集成了Java的[onnx4j](https://github.com/onnx4j/)库和Scala的硬件描述语言[SpinalHDL](https://github.com/SpinalHDL/SpinalHDL)，提供了一个完整的软硬件协同验证平台。

基于SpinalHDL的[sbt基础项目](https://github.com/SpinalHDL/SpinalTemplateSbt)，硬件加速器RTL设计和相应的测试程序目前在/hw目录下。

## 项目结构

```
├── 📂 MNIST/                         # MNIST数据集和预训练模型
├── 📂 hw/                            # 硬件部分 - SpinalHDL设计
│   ├── 📂 spinal/                    # SpinalHDL源代码
│   │   ├── 📂 Accelerator/           # 硬件加速器顶层设计
│   │   ├── 📂 Activation/            # 激活函数模块
│   │   ├── 📂 Conv/                  # 卷积运算模块
│   │   ├── 📂 Cordic/                # CORDIC算法实现
│   │   ├── 📂 DataPump/              # 数据流管理
│   │   ├── 📂 ExponentialFunction/   # 指数函数硬件实现
│   │   ├── 📂 FloatingPoint/         # 浮点运算模块
│   │   ├── 📂 GeMM/                  # 通用矩阵乘法
│   │   ├── 📂 Interface/             # 接口模块
│   │   ├── 📂 LogarithmFunction/     # 对数函数硬件实现
│   │   ├── 📂 LogCumsumExp/          # LogCumsumExp函数
│   │   ├── 📂 MemBlackBoxer/         # 内存黑盒集成
│   │   ├── 📂 playGround/            # 测试和演示代码
│   │   ├── 📂 projectname/           # 项目配置
│   │   ├── 📂 ReLUFunction/          # ReLU激活函数
│   │   ├── 📂 Sigmoid/               # Sigmoid函数
│   │   ├── 📂 simulation_tool/       # 仿真工具
│   │   ├── 📂 SoftplusFunction/      # Softplus函数
│   │   └── 📂 XPM_BlackBox/          # Xilinx IP集成
│   ├── 📂 gen/                       # 生成的硬件文件
│   ├── 📂 verilog/                   # Verilog输出
│   └── 📂 vhdl/                      # VHDL输出
├── 📂 onnx_debug_py/                 # ONNX调试和验证工具
├── 📂 performance_results_analyze/   # 性能分析结果
├── 📂 project/                       # SBT构建配置
├── 📂 rtl/                           # 生成的RTL代码
├── 📂 sw/                            # 软件部分 - ONNX推理框架
│   └── 📂 java/
│       ├── 📂 main/                  # 主代码目录
│       └── 📂 test/                  # 测试代码和测试数据
├── 📄 .gitignore                     # Git忽略规则
├── 📄 .mill-version                  # Mill构建工具版本
├── ⚙️ .scalafmt.conf                 # Scala代码格式化配置
├── 📄 .vlogansetup.args              # Verilog分析设置
├── 📄 absnet.onnx                    # ONNX模型文件
├── 🐍 autotreedoc.py                 # 项目结构文档生成工具
├── 📄 build.sbt                      # SBT构建配置
├── 📄 build.sc                       # Mill构建配置
├── 📝 DeepResearch.md                # 深度研究文档
├── 📝 PROJECT_STRUCTURE.md           # 项目结构文档
├── 📝 PROJECT_STRUCTURE_DETAILED.md  # 详细项目结构文档
└── 📄 worksheet.sc                   # Scala工作表文件
```

完整详细的项目结构请查看 [PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md)。

## 核心模块功能

### 软件部分 (sw/)
- **ONNX4J框架**: 完整的ONNX模型解析和执行框架
- **推理后端**: 支持DL4J和TensorFlow后端
- **测试框架**: 全面的算子验证和模型测试
- **资源管理**: 模型文件和数据集的配置管理

### 硬件部分 (hw/)
- **加速器核心**: 集成切片器、数据泵、脉动阵列的完整加速器设计
- **计算模块**: 各种数学函数和激活函数的硬件实现
- **数据流管理**: 高效的数据传输和内存管理
- **接口系统**: 软件-硬件通信接口
- **仿真工具**: 硬件模块的测试和验证框架

### 关键硬件模块

#### 加速器核心 (hw/spinal/Accelerator/)
- **Accelerator.scala**: 硬件加速器顶层设计，集成所有计算模块
- **AcceleratorSimInterface.scala**: 仿真接口定义
- **performanceTest.scala**: 性能测试框架

#### 计算模块
- **Activation.scala**: 统一激活函数模块，支持多种非线性函数
- **SystolicArray2D.scala**: 二维脉动阵列，支持矩阵运算
- **EXP_function.scala**: 指数函数硬件实现
- **LN_function.scala**: 自然对数函数硬件实现
- **ReLU_function.scala**: ReLU激活函数硬件实现
- **SoftplusFunction.scala**: Softplus函数硬件实现

#### 数据流管理
- **DataPump_mm2s.scala**: 内存到流数据传输
- **DataPump_s2mm.scala**: 流到内存数据传输
- **Slicer.scala**: 数据切片和重组模块
- **StreamDispatcher.scala**: 流数据分发

### 调试和分析工具
- **onnx_debug_py/**: Python脚本用于ONNX模型调试和结果验证
- **performance_results_analyze/**: 性能分析工具和结果可视化
- **autotreedoc.py**: 自动生成项目结构文档

## 算子支持状态

### 已实现的算子
项目支持广泛的ONNX算子，包括但不限于：

| 算子类别 | 支持的算子 |
|---------|-----------|
| 基础数学运算 | Add, Sub, Mul, Div, Exp, Log, Neg |
| 线性代数 | MatMul, GeMM, Conv |
| 激活函数 | Relu, Sigmoid, Softplus, LeakyRelu |
| 张量操作 | Reshape, Concat, Slice, Transpose, Expand, Tile |
| 规约操作 | ReduceMax, Max, Sum |
| 比较操作 | Greater, Where |
| 其他 | Cast, Constant, Identity, Shape, Squeeze, Unsqueeze |

### 算子版本支持
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

## 验证和测试

### ONNX算子行为验证
使用ONNXRuntime获取标准行为作为参考：
1. 定义输入张量和参数
2. 构造ONNX模型图
3. 使用ONNXRuntime推理获取输出
4. 对比Java端实现结果

参考示例：`sw/java/test/java/org/forwarder/backend/impls/dl4j/opsets/aiOnnx/v13/ops/CompareSliceV13.py`

### 硬件模块测试
每个硬件模块都包含相应的测试文件：
- 功能测试: `tb_*.scala` 文件
- 性能测试: `performanceTest.scala`
- 仿真验证: 使用生成的RTL进行仿真

## 运行

### 软件
在当前目录下运行以下指令执行最基础的单元测试
```bash
sbt "testOnly org.forwarder.demo.SimpleTest"
```
顺利的话，输出应该如下：
```bash
[info] welcome to sbt 1.6.0 (Red Hat, Inc. Java 17.0.16)
...
[forwarder-session-9aaceaee-d0cb-4e8d-b644-55360fc82102] device_0, current cycle: 16; max cycle: 0
[pool-1-thread-1] INFO org.forwarder.demo.SimpleTest - Total time: 33s  Avg time: 33.0ms
[info] Passed: Total 1, Failed 0, Errors 0, Passed 1
[success] Total time: 6 s, completed Oct 14, 2025, 10:09:22 AM
```

### 硬件
运行以下指令运行加速器的测试类
```bash
sbt "runMain Accelerator.AcceleratorTb"
```
顺利的话应该会输出： 
```bash
...
test 43 pass
test 44 pass
test 45 pass
test 46 pass
test 47 pass
test 48 pass
test 49 pass
TEST PASS
Total cycles: 74348, Cycles/test: 1486.96
Total operations: 8102912, FLOPS/cycle: 108.98628073384624
Unexpected termination of the simulation
           V C S   S i m u l a t i o n   R e p o r t 
Time: 743650 ps
CPU Time:     11.940 seconds;       Data structure size: 215.6Mb
Tue Oct 14 10:20:23 2025
[Done] Simulation done in 9930.138 ms
```

### 项目结构文档生成
```bash
python autotreedoc.py  # 生成最新的项目结构文档
```


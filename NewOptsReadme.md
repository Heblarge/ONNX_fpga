# 新算子的注册流程
流程中以GemmRelu为例

一个onnx官方不提供的新算子能正常在模型中工作需要两个部分
- 注册进抽象层的Model，Graph和Node，使得在加载模型的时候新算子能作为一个节点被识别为一个合法的算子
- 在注册之后有相对应的实现部分

## 1.注册新算子的Proto，使得被识别
本身其实只需要在onnx的Proto文件加入新算子的proto就可以完成，但onnx本身已经打包好了自己支持算子的proto和operatorsPrototype文件，并且没有公布这些这文件。
所以原创的算子不能丝滑被抽象层的Graph和Node识别为合法。

所以我们采取了这样的方式，设计了一个Newopsets.java,在这里去加入我们新算子的名字和Proto。并在Model中去执行getNewOpset()
```java
registerCustomOps(Newopsets.getNewOpset());
```
在加载模型之后去模型中看，是不是有我们需要的新算子，有的话也识别为合法算子，这工作相当于和onnx本身这部分的工作的流程是一样的，只不过区别就是我们的
新算子不在官方的Prototype文件中，在我们自己的Newopsets中。

所以，在注册新算子时，只需要在 livehps_1/sw/java/main/java/org/onnx4j/prototypes/Newopsets.java 中注册所需新算子和其Proto。
例如：
```java
        // 1️⃣ 创建 GemmRelu 算子
        OperatorProto gemmRelu = OperatorProto.newBuilder()
                .setOpType("GemmRelu")
                .setSinceVersion(1)
                .setStatus(OperatorStatus.STABLE)
                .setDocString("Fused operator performing Gemm (A*B + C) followed by ReLU activation.")
                .build();

        // 2️⃣ 创建 OperatorSetProto，并加入自定义算子
        OperatorSetProto NewOpset = OperatorSetProto.newBuilder()
                .setMagic("ONNXOPSET")
                .setIrVersion(9)         // 对应你项目使用的 IR_VERSION
                .setDomain("")           // 空域表示标准域
                .setOpsetVersion(13)      // 自定义 opset 版本
                .addOperator(gemmRelu)
                .build();

        return NewOpset;
```
如果现在我需要加一个算子叫AddSub，需要这样加入：
```java
        // 1️⃣ 创建 GemmRelu 算子
        OperatorProto gemmRelu = OperatorProto.newBuilder()
                .setOpType("GemmRelu")
                .setSinceVersion(1)
                .setStatus(OperatorStatus.STABLE)
                .setDocString("Fused operator performing Gemm (A*B + C) followed by ReLU activation.")
                .build();

        // 2️⃣ 创建 AddSub 算子
        OperatorProto addsub = OperatorProto.newBuilder()
                .setOpType("AddSub")
                .setSinceVersion(1)
                .setStatus(OperatorStatus.STABLE)
                .setDocString("Fused operator performing Add followed by Sub.")
                .build();

        // 3️⃣ 创建 OperatorSetProto，并加入自定义算子
        OperatorSetProto NewOpset = OperatorSetProto.newBuilder()
                .setMagic("ONNXOPSET")
                .setIrVersion(9)         // 对应你项目使用的 IR_VERSION
                .setDomain("")           // 空域表示标准域
                .setOpsetVersion(13)      // 自定义 opset 版本
                .addOperator(gemmRelu)
                .addOperator(addsub)
                .build();

        return NewOpset;
```

## 2. 加入算子原型和实现
### 首先在抽象层中完成对算子原型的定义
livehps_1/sw/java/main/java/org/onnx4j/opsets/domain/aiOnnx/

如：livehps_1/sw/java/main/java/org/onnx4j/opsets/domain/aiOnnx/v13/ops/GemmReluV13.java

### 然后在Forward层的后端中完成新算子的具体实现，如：
- livehps_1/sw/java/main/java/org/forwarder/backend/impls/HWAccelerated/opsets/v13/ops/HWAcceleratedGemmReluV13.java
- livehps_1/sw/java/main/java/org/forwarder/backend/impls/dl4j/opsets/aiOnnx/v13/ops/DL4JGemmReluV13.java

### 最后完成具体实现和算子原型之间的映射（以V13为例）
在AiOnnxOpsetInitializerV13.java中:
```java
    public abstract GemmReluV13 getGemmReluV13();
```
```java
    operators.put(GemmReluV13.OP_TYPE, this.getGemmReluV13());
```
在DL4JAiOnnxOperatorSetV13.java或HWAcceleratedOperatorSetV13.java中完成：
```java
        public GemmReluV13   getGemmReluV13(){return new    HWAcceleratedGemmReluV13();}
```
或
```java
    public GemmReluV13  getGemmReluV13(){return new    DL4JGemmReluV13();}
```
为了能跑，需要把TensorFlow处理一下：
```java
@Override
public GemmReluV13 getGemmReluV13() {
// TODO Auto-generated method stub
return null;
}
```
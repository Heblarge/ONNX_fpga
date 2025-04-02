# Onnx_SpinalHDL_interface

这个分支用来探索读取onnx文件，推理，并读取每一层结果，并测试由SpinalHDL编写的硬件模块的流程

其实主要就是一个对java的[onnx4j](https://github.com/onnx4j/)库和scala的硬件描述语言[SpinalHDL](https://https://github.com/SpinalHDL/SpinalHDL)测试接口进行混编的探索。


本分支基于SpinalHDL的[sbt基础项目](https://github.com/SpinalHDL/SpinalTemplateSbt)，但暂时不在这个分支里面开发硬件。


## 关于如何添加新的算子

目前onnx4j库有两个后端,分别基于dl4j库和tensorflow库实现,其中TF库的支持很简陋,因此我们这里有限选择DL4J作为后端,实现的标准请参考[所有算子目录](https://onnx.ai/onnx/operators/index.html)

这里用v6的Add算子为例子,主要参考[Add - 6](https://onnx.ai/onnx/operators/onnx__Add.html#l-onnx-op-add-6)以及[Add - 1 vs 6](https://onnx.ai/onnx/operators/text_diff_Add_1_6.html)

###
- 1.添加算子接口

  sw/java/main/java/org/onnx4j/opsets/domain/aiOnnx/v6/ops/AddV6.java

- 2.实现算子逻辑

  sw/java/main/java/org/forwarder/backend/impls/dl4j/opsets/aiOnnx/v6/ops/DL4JAddV6.java

- 3.将添加的算子注册到

  onnx4j
  - sw/java/main/java/org/onnx4j/opsets/domain/aiOnnx/v6/AiOnnxOpsetInitializerV6.java

  dl4j backend
  - sw/java/main/java/org/forwarder/backend/impls/dl4j/opsets/aiOnnx/v6/DL4JAiOnnxOperatorSetV6.java

  tensorflow backend
  - sw/java/main/java/org/forwarder/backend/impls/tensorflow/opsets/aiOnnx/v6/TFAiOnnxOperatorSetV6.java

- 4.验证是否添加成功

  运行测试

  sw/java/test/java/org/forwarder/backend/impls/dl4j/opsets/OpsetTest.java

  观察支持算子表是否更新了你添加的算子。

- 5.编写测试
  创建
  sw/java/test/java/org/forwarder/backend/impls/dl4j/opsets/aiOnnx/v6/ops/DL4JAddV6Test.java



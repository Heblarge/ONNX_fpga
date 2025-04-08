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

## 关于已经有的算子
| Operator\Opset     | V1 | V2 | V3 | V4 | V5 | V6 | V7 | V8 | V9 | V10 | V11 | V12 |
|:-------------------|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:---:|:---:|:---:|
| Abs                | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  |  1  |  1  | 
| Add*               | 1  | 1  | 1  | 1  | 1  | 6  | 6  | 6  | 6  |  6  |  6  |  6  |
| ArgMax             | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  |  1  |  1  |
| AveragePool        | 1  | 1  | 1  | 1  | 1  | 1  | 7  | 7  | 7  |  7  |  7  |  7  |
| BatchNormalization | 1  | 1  | 1  | 1  | 1  | 6  | 7  | 7  | 7  |  7  |  7  |  7  |
| Cast               | 1  | 1  | 1  | 1  | 1  | 6  | 6  | 6  | 9  |  9  |  9  |  9  |
| Concat             | 1  | 1  | 1  | 4  | 4  | 4  | 4  | 4  | 4  |  4  |  4  |  4  |
| Constant           | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  |  1  |  1  |
| Conv               | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  |  1  |  1  |
| Div*               | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  |  1  |  1  |
| Dropout            | 1  | 1  | 1  | 1  | 1  | 6  | 7  | 7  | 7  |  7  |  7  |  7  |
| Gather             | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  |  1  |  1  |
| Identity           | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  |  1  |  1  |
| ImageScaler        | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  |  1  |  1  |
| LeakyRelu          | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  |  1  |  1  |
| MatMul             | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  |  1  |  1  |
| MaxPool            | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  |  1  |  1  |
| Mul*               | 1  | 1  | 1  | 1  | 1  | 6  | 6  | 6  | 6  |  6  |  6  |  6  |
| ReduceMax          | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  | 11  | 12  |
| Relu               | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  |  1  |  1  |
| Reshape            | 1  | 1  | 1  | 1  | 5  | 5  | 5  | 5  | 5  |  5  |  5  |  5  |
| Shape              | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  |  1  |  1  |
| Sigmoid            | 1  | 1  | 1  | 1  | 1  | 6  | 6  | 6  | 6  |  6  |  6  |  6  |
| Softmax            | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  | 11  | 11  |
| Squeeze            | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  | 11  | 11  |
| Sub*               | 1  | 1  | 1  | 1  | 1  | 6  | 7  | 7  | 7  |  7  |  7  |  7  |
| Sum*               | 1  | 1  | 1  | 1  | 1  | 6  | 6  | 8  | 8  |  8  |  8  |  8  |
| Transpose          | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  |  1  |  1  |
| Unsqueeze          | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  |  1  | 11  | 11  |

另外需要注意BroadCast在V7之前的版本中实现的都有问题,因此不推荐使用上表中带*的算子V7之前的版本:

## 需要完成的算子
| Operator\Opset   | V1 | V2 | V3 | V4 | V5 | V6 | V7 | V8 | V9 | V10 | V11 | V12 | V13 | Done |  Resp.  |
|------------------|----|----|----|----|----|----|----|----|----|-----|-----|-----|-----|------|---------|
| Reshape          | 1  | 1  | 1  | 1  | 5  | 5  | 5  | 5  | 5  | 5   | 5   | 5   | 13  | OK   | zhoujsh |
| Concat           | 1  | 1  | 1  | 4  | 4  | 4  | 4  | 4  | 4  | 4   | 11  | 11  | 13  | OK   | zhoujsh |
| Slice            | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 10  | 11  | 11  | 13  | OK   | zhoujsh |
| Expand           | X  | X  | X  | X  | X  | X  | X  | 8  | 8  | 8   | 8   | 8   | 13  |      | zhoujsh |
| Tile             | 1  | 1  | 1  | 1  | 1  | 6  | 6  | 6  | 6  | 6   | 6   | 6   | 13  |      |         |
| Cast             | 1  | 1  | 1  | 1  | 1  | 6  | 6  | 6  | 9  | 9   | 9   | 9   | 13  |      |         |
| Transpose        | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1   | 1   | 1   | 13  |      |         |
| ReduceMax        | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 11  | 12  | 11  | 13  |      |         |
| Max              | 1  | 1  | 1  | 1  | 1  | 6  | 6  | 8  | 8  | 8   | 12  | 12  | 13  |      |         |
| Greater          | 1  | 1  | 1  | 1  | 1  | 1  | 7  | 7  | 9  | 9   | 9   | 9   | 13  |      |         |
| Where            | X  | X  | X  | X  | X  | X  | X  | X  | 9  | 9   | 9   | 9   | 9   |      |         |
| Neg              | 1  | 1  | 1  | 1  | 1  | 6  | 6  | 6  | 6  | 6   | 6   | 6   | 13  |      |         |
| ConstantOfShape  | X  | X  | X  | X  | X  | X  | X  | X  | 9  | 9   | 9   | 9   | 9   |      |         |
| MatMul           | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 9  | 9   | 9   | 9   | 13  |      |         |
| GeMM             | 1  | 1  | 1  | 1  | 1  | 6  | 7  | 7  | 9  | 9   | 11  | 11  | 13  |      |         |
| Conv             | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1   | 11  | 11  | 11  |      |         |
| Add              | 1  | 1  | 1  | 1  | 1  | 6  | 7  | 7  | 7  | 7   | 7   | 7   | 13  |      |         |
| Sub              | 1  | 1  | 1  | 1  | 1  | 6  | 7  | 7  | 7  | 7   | 7   | 7   | 13  |      |         |
| Exp              | 1  | 1  | 1  | 1  | 1  | 6  | 6  | 6  | 6  | 6   | 6   | 6   | 13  |      |         |
| Log              | 1  | 1  | 1  | 1  | 1  | 6  | 6  | 6  | 6  | 6   | 6   | 6   | 13  |      |         |
| Relu             | 1  | 1  | 1  | 1  | 1  | 6  | 6  | 6  | 6  | 6   | 6   | 6   | 13  |      |         |
| Softplus         | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1  | 1   | 11  | 11  | 13  |      |         |
| QuantizeLinear   | X  | X  | X  | X  | X  | X  | X  | X  | X  | 10  | 10  | 10  | 13  |      |         |
| DequantizeLinear | X  | X  | X  | X  | X  | X  | X  | X  | X  | 10  | 10  | 10  | 13  |      |         |

## 获取 ONNX 算子的实际行为
为了获取某版本 ONNX 的标准行为，使用 ONNXRuntime 执行单独导出的 ONNX 算子得到标准输出，并对比 Java 端结果。
可参考 ONNX Python 库文档 https://onnx.ai/onnx/intro/python.html

下面以Slice为例子：(/sw/java/test/java/org/forwarder/backend/impls/dl4j/opsets/aiOnnx/v13/ops/CompareSliceV13.py)

#### 1：定义输入张量（`make_tensor_value_info`）与切片参数（`make_tensor`）

```python
from onnx import TensorProto, helper
# 输入数据：类型为 FLOAT，形状为 (3, 4)
inputs = [helper.make_tensor_value_info("data", TensorProto.FLOAT, [3, 4])]
# 切片参数：
initializers = [
  const("starts", starts),
  const("ends", ends)
]
# 可选参数：
if axes is not None:
  initializers.append(const("axes", axes))
if steps is not None:
  initializers.append(const("steps", steps))
```

#### 2：构造节点
```python
from onnx.helper import make_node
slice_node = make_node(
    "Slice",
    inputs=["data", "starts", "ends", "axes"],  # 参数顺序必须一致
    outputs=["sliced"]
)
```

#### 3：构造图与模型并导出文件
```python
from onnx.helper import make_graph, make_model
graph = make_graph(
    [slice_node],
    "slice-graph",
    inputs=[data],
    outputs=[make_tensor_value_info("sliced", TensorProto.DOUBLE, None)],
    initializer=[starts_tensor, ends_tensor, axes_tensor]
)
model = make_model(graph, opset_imports=[onnx.helper.make_opsetid("", 13)]) # 此处可以指定算子集版本为13
check_model(model) # 检查而合法性
with open("slice_test.onnx", "wb") as f:
  f.write(model.SerializeToString()) # 导出模型到 slice_test.onnx 文件
```

#### 4：使用 ONNXRuntime 推理模型并获取输出结构
```python
import onnxruntime as ort
import numpy as np

sess = ort.InferenceSession("slice_test.onnx")
data_array = np.array([
    [1, 2, 3, 4],
    [5, 6, 7, 8],
    [9,10,11,12]
], dtype=np.float64)

outputs = sess.run(None, {"data": data_array})
result = outputs[0]
print(result)
```



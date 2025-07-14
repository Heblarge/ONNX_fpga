import numpy as np
import onnx
import onnxruntime as ort
from onnx import TensorProto, helper, numpy_helper
from onnx.checker import check_model

def describe_array(arr: np.ndarray) -> str:
    rank = arr.ndim
    shape = list(arr.shape)
    stride = list(arr.strides)
    dtype_size = arr.dtype.itemsize
    stride = [s // dtype_size for s in stride]
    print(arr)
    return f"Actual: Rank: {rank}, DataType: {arr.dtype.name.upper()}, Offset: 0, Order: c, Shape: {shape},  Stride: {stride}"

def const(name, value, dtype=np.int64):
    array = np.asarray(value, dtype=dtype)
    return helper.make_tensor(name=name, data_type=TensorProto.INT64, dims=array.shape, vals=array.flatten())

def make_slice_model(starts, ends, axes=None, steps=None):
    inputs = [helper.make_tensor_value_info("data", TensorProto.FLOAT, [3, 4])]
    outputs = [
        helper.make_tensor_value_info("output", TensorProto.FLOAT, [None, None])
    ]

    initializers = [
        const("starts", starts),
        const("ends", ends),
    ]
    input_names = ["data", "starts", "ends"]

    if axes is not None:
        initializers.append(const("axes", axes))
        input_names.append("axes")
    if steps is not None:
        initializers.append(const("steps", steps))
        input_names.append("steps")

    slice_node = helper.make_node("Slice", input_names, ["output"])
    graph = helper.make_graph([slice_node], "slice_test", inputs, outputs, initializer=initializers)
    model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", 13)])
    check_model(model)
    return model

data = np.array([
    [1, 2, 3, 4],
    [5, 6, 7, 8],
    [9,10,11,12]
], dtype=np.float32)

tests = [
    dict(desc="test1", starts=[0,1], ends=[2,4], axes=[0,1]),
    dict(desc="test2", starts=[0,0], ends=[3,2], axes=None),
    dict(desc="test3", starts=[2,3], ends=[0,0], axes=[0,1], steps=[1,1]),
    dict(desc="test4", starts=[-2,1], ends=[3,4], axes=[0,1], steps=[1,1]),
    dict(desc="test5", starts=[0], ends=[3], axes=[0], steps=[2]),
]

for test in tests:
    model = make_slice_model(test["starts"], test["ends"], test.get("axes"), test.get("steps"))

    with open("slice_test.onnx", "wb") as f:
        f.write(model.SerializeToString())

    sess = ort.InferenceSession("slice_test.onnx")
    input_name = sess.get_inputs()[0].name
    output = sess.run(None, {input_name: data})[0]

    print(f"{test['desc']} OK:")
    print(describe_array(output))

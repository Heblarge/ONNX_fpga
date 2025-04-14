import torch
import torch.nn as nn
import numpy as np
import onnx
import onnxruntime as ort

def export_and_run(model_class, input_tensor, opset=13):
    model = model_class()
    torch.onnx.export(model, input_tensor, "model.onnx", opset_version=opset, input_names=["x"])
    sess = ort.InferenceSession("model.onnx")
    input_name = sess.get_inputs()[0].name
    return sess.run(None, {input_name: input_tensor.numpy()})[0]

# 1. test1: reshape [2,3] -> [3,2]
class Model1(nn.Module):
    def forward(self, x):
        return x.reshape(3, 2)

input1 = torch.randn(2, 3)
out1 = export_and_run(Model1, input1)
print("test1 OK:", out1.shape == (3, 2))

# 2. test2: reshape [2,3] -> [-1]
class Model2(nn.Module):
    def forward(self, x):
        return x.reshape(-1)

input2 = torch.randn(2, 3)
out2 = export_and_run(Model2, input2)
print("test2 OK:", out2.shape == (6,))

# 3. test3: reshape [2,3] -> [0, -1]
class Model3(nn.Module):
    def forward(self, x):
        bsz = x.shape[0]
        return x.reshape(bsz, -1)

input3 = torch.randn(2, 3)
out3 = export_and_run(Model3, input3)
print("test3 OK:", out3.shape == (2, 3))

# 4. test4: reshape [2,3] -> [4,2] -> 报错
class Model4(nn.Module):
    def forward(self, x):
        return x.reshape(4, 2)

input4 = torch.randn(2, 3)
try:
    out4 = export_and_run(Model4, input4)
    print("test4 FAILED: did not throw")
except Exception as e:
    print("test4 OK: raised as expected →", str(e).split("\n")[0])

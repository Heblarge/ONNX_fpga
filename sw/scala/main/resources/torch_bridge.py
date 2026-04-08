#!/usr/bin/env python3
"""
PyTorch Bridge Server — communicates with Scala runtime via stdin/stdout.
Protocol: JSON-line based. Each request is a single JSON line on stdin,
          each response is a single JSON line on stdout.
Tensors are transferred as base64-encoded raw bytes.

Commands:
  load_model     — Load an ONNX model, return graph structure
  load_test_data — Load protobuf test tensors from a directory
  create_tensor  — Create a tensor from base64 data
  op             — Execute a tensor operation
  get_tensor     — Retrieve tensor data as base64
  get_info       — Get tensor shape/dtype info
  delete_tensor  — Free a tensor
  shutdown       — Exit the server
"""
import sys
import json
import base64
import struct
import os
import traceback
import numpy as np

import torch
import onnx
from onnx import numpy_helper, TensorProto

# Disable torch gradient computation for inference
torch.set_grad_enabled(False)

# Tensor store: id -> torch.Tensor
_tensors = {}
_next_id = [0]

def _new_id():
    _next_id[0] += 1
    return str(_next_id[0])

def _store(t):
    tid = _new_id()
    _tensors[tid] = t
    return tid

def _get(tid):
    return _tensors[tid]

def _del(tid):
    if tid in _tensors:
        del _tensors[tid]

def _dtype_str(dt):
    return str(dt).replace("torch.", "")

def _to_torch_dtype(s):
    mapping = {
        "float32": torch.float32, "float": torch.float32,
        "float64": torch.float64, "double": torch.float64,
        "int32": torch.int32, "int": torch.int32,
        "int64": torch.int64, "long": torch.int64,
        "int16": torch.int16, "short": torch.int16,
        "int8": torch.int8, "byte": torch.int8,
        "uint8": torch.uint8, "bool": torch.bool,
        "float16": torch.float16, "half": torch.float16,
        "bfloat16": torch.bfloat16,
    }
    return mapping.get(s, torch.float32)

def _tensor_info(tid):
    t = _get(tid)
    return {"id": tid, "shape": list(t.shape), "dtype": _dtype_str(t.dtype)}

# ============ ONNX Model Loading ============

def cmd_load_model(args):
    path = args["path"]
    model = onnx.load(path)
    graph = model.graph

    # Parse nodes
    nodes = []
    for n in graph.node:
        attrs = {}
        for a in n.attribute:
            if a.type == onnx.AttributeProto.INT:
                attrs[a.name] = {"type": "int", "value": a.i}
            elif a.type == onnx.AttributeProto.INTS:
                attrs[a.name] = {"type": "ints", "value": list(a.ints)}
            elif a.type == onnx.AttributeProto.FLOAT:
                attrs[a.name] = {"type": "float", "value": a.f}
            elif a.type == onnx.AttributeProto.FLOATS:
                attrs[a.name] = {"type": "floats", "value": list(a.floats)}
            elif a.type == onnx.AttributeProto.STRING:
                attrs[a.name] = {"type": "string", "value": a.s.decode("utf-8")}
            elif a.type == onnx.AttributeProto.STRINGS:
                attrs[a.name] = {"type": "strings", "value": [s.decode("utf-8") for s in a.strings]}
            elif a.type == onnx.AttributeProto.TENSOR:
                arr = numpy_helper.to_array(a.t)
                tid = _store(torch.from_numpy(arr.copy()))
                attrs[a.name] = {"type": "tensor", "value": tid}
            else:
                # fallback
                if a.ints:
                    attrs[a.name] = {"type": "ints", "value": list(a.ints)}
                elif a.floats:
                    attrs[a.name] = {"type": "floats", "value": list(a.floats)}
                else:
                    attrs[a.name] = {"type": "int", "value": a.i}

        nodes.append({
            "name": n.name if n.name else ",".join(n.output),
            "opType": n.op_type,
            "domain": n.domain,
            "inputs": list(n.input),
            "outputs": list(n.output),
            "attrs": attrs,
        })

    # Parse initializers - store as tensors
    initializers = {}
    for init in graph.initializer:
        arr = numpy_helper.to_array(init)
        tid = _store(torch.from_numpy(arr.copy()))
        initializers[init.name] = tid

    # Parse inputs
    def parse_value_info(vi):
        name = vi.name
        if vi.type.HasField("tensor_type"):
            tt = vi.type.tensor_type
            dtype = tt.elem_type
            shape = []
            if tt.HasField("shape"):
                shape = [d.dim_value if d.dim_value > 0 else -1 for d in tt.shape.dim]
            return {"name": name, "dtype": dtype, "shape": shape}
        return {"name": name, "dtype": 1, "shape": []}

    inputs = [parse_value_info(i) for i in graph.input]
    outputs = [parse_value_info(o) for o in graph.output]

    opset = model.opset_import[0].version if model.opset_import else 1

    return {
        "nodes": nodes,
        "initializers": initializers,
        "inputs": inputs,
        "outputs": outputs,
        "opsetVersion": opset,
        "irVersion": model.ir_version,
    }

def cmd_load_test_data(args):
    """Load protobuf test tensors from a data directory."""
    data_dir = args["dir"]
    result = {}
    for f in sorted(os.listdir(data_dir)):
        if not f.endswith(".pb"):
            continue
        fp = os.path.join(data_dir, f)
        tensor_proto = TensorProto()
        with open(fp, "rb") as fh:
            tensor_proto.ParseFromString(fh.read())
        arr = numpy_helper.to_array(tensor_proto)
        tid = _store(torch.from_numpy(arr.copy()))
        name = tensor_proto.name if tensor_proto.name else f.replace(".pb", "")
        result[f] = {"name": name, "id": tid, "shape": list(arr.shape), "dtype": str(arr.dtype)}
    return result

# ============ Tensor Operations ============

def cmd_create_tensor(args):
    shape = args["shape"]
    dtype = _to_torch_dtype(args.get("dtype", "float32"))
    if "data_b64" in args:
        raw = base64.b64decode(args["data_b64"])
        np_dtype = {
            torch.float32: np.float32, torch.float64: np.float64,
            torch.int32: np.int32, torch.int64: np.int64,
            torch.int16: np.int16, torch.int8: np.int8,
            torch.uint8: np.uint8, torch.bool: np.bool_,
            torch.float16: np.float16, torch.bfloat16: np.float32,
        }.get(dtype, np.float32)
        arr = np.frombuffer(raw, dtype=np_dtype).reshape(shape)
        t = torch.from_numpy(arr.copy())
        if dtype == torch.bfloat16:
            t = t.to(torch.bfloat16)
    elif "fill" in args:
        t = torch.full(shape, args["fill"], dtype=dtype)
    elif "rand" in args:
        t = torch.rand(shape, dtype=dtype if dtype.is_floating_point else torch.float32)
        if args["rand"] == "normal":
            t = torch.randn(shape, dtype=dtype if dtype.is_floating_point else torch.float32)
        t = t * 2 - 1  # range [-1, 1]
        if not dtype.is_floating_point:
            t = t.to(dtype)
    else:
        t = torch.zeros(shape, dtype=dtype)
    return _tensor_info(_store(t))

def cmd_get_tensor(args):
    tid = args["id"]
    t = _get(tid).contiguous().cpu()
    arr = t.numpy()
    raw = arr.tobytes()
    return {
        "id": tid,
        "shape": list(t.shape),
        "dtype": _dtype_str(t.dtype),
        "data_b64": base64.b64encode(raw).decode("ascii"),
    }

def cmd_get_info(args):
    return _tensor_info(args["id"])

def cmd_get_values(args):
    t = _get(args["id"])
    n = int(args.get("n", 5))
    flat = t.flatten()[:n]
    return {"values": flat.tolist(), "dtype": str(t.dtype)}

def cmd_delete_tensor(args):
    _del(args["id"])
    return {"ok": True}

# ============ Operator Execution ============

def _broadcast(a, b):
    """Broadcast two tensors to compatible shapes."""
    target = torch.broadcast_shapes(a.shape, b.shape)
    return a.expand(target), b.expand(target)

def cmd_op(args):
    op_name = args["op"]
    inputs = [_get(tid) if tid else None for tid in args.get("inputs", [])]
    attrs = args.get("attrs", {})

    result = _exec_op(op_name, inputs, attrs)

    if isinstance(result, (list, tuple)):
        return [_tensor_info(_store(r)) for r in result]
    else:
        return [_tensor_info(_store(result))]

def _exec_op(op_name, inputs, attrs):
    if op_name == "MatMul":
        return torch.matmul(inputs[0].float(), inputs[1].float())

    elif op_name == "Add":
        a, b = _broadcast(inputs[0].float(), inputs[1].float())
        return a + b

    elif op_name == "Sub":
        a, b = _broadcast(inputs[0].float(), inputs[1].float())
        return a - b

    elif op_name == "Mul":
        a, b = _broadcast(inputs[0].float(), inputs[1].float())
        return a * b

    elif op_name == "Div":
        a, b = _broadcast(inputs[0].float(), inputs[1].float())
        return a / b

    elif op_name == "Relu":
        return torch.relu(inputs[0].float())

    elif op_name == "LeakyRelu":
        alpha = attrs.get("alpha", 0.01)
        return torch.nn.functional.leaky_relu(inputs[0].float(), alpha)

    elif op_name == "Sigmoid":
        return torch.sigmoid(inputs[0].float())

    elif op_name == "Softmax":
        axis = attrs.get("axis", -1)
        return torch.softmax(inputs[0].float(), dim=axis)

    elif op_name == "Exp":
        return torch.exp(inputs[0].float())

    elif op_name == "Log":
        return torch.log(inputs[0].float())

    elif op_name == "Neg":
        return -inputs[0].float()

    elif op_name == "Abs":
        return torch.abs(inputs[0].float())

    elif op_name == "Reshape":
        data = inputs[0]
        shape = inputs[1] if len(inputs) > 1 and inputs[1] is not None else None
        if shape is not None:
            target = shape.long().tolist()
        else:
            target = attrs.get("shape", [-1])
        # resolve 0 dims
        current = list(data.shape)
        for i in range(len(target)):
            if target[i] == 0 and i < len(current):
                target[i] = current[i]
        return data.reshape(target)

    elif op_name == "Transpose":
        perm = attrs.get("perm", None)
        if perm:
            return inputs[0].permute(perm)
        else:
            return inputs[0].T

    elif op_name == "Concat":
        axis = attrs.get("axis", 0)
        tensors = [t for t in inputs if t is not None]
        return torch.cat(tensors, dim=axis)

    elif op_name == "Squeeze":
        data = inputs[0]
        if len(inputs) > 1 and inputs[1] is not None:
            axes = inputs[1].long().tolist()
        else:
            axes = attrs.get("axes", [])
        if axes:
            for ax in sorted(axes, reverse=True):
                data = data.squeeze(ax)
        else:
            data = data.squeeze()
        return data

    elif op_name == "Unsqueeze":
        data = inputs[0]
        if len(inputs) > 1 and inputs[1] is not None:
            axes = sorted(inputs[1].long().tolist())
        else:
            axes = sorted(attrs.get("axes", []))
        for ax in axes:
            data = data.unsqueeze(ax)
        return data

    elif op_name == "Shape":
        return torch.tensor(list(inputs[0].shape), dtype=torch.int64)

    elif op_name == "Gather":
        axis = attrs.get("axis", 0)
        data = inputs[0]
        indices = inputs[1].long()
        return torch.index_select(data, axis, indices.flatten()).reshape(
            list(data.shape[:axis]) + list(indices.shape) + list(data.shape[axis+1:])
        )

    elif op_name == "Slice":
        data = inputs[0]
        starts = inputs[1].long().tolist() if len(inputs) > 1 and inputs[1] is not None else attrs.get("starts", [0])
        ends = inputs[2].long().tolist() if len(inputs) > 2 and inputs[2] is not None else attrs.get("ends", [])
        axes = inputs[3].long().tolist() if len(inputs) > 3 and inputs[3] is not None else attrs.get("axes", list(range(len(starts))))
        steps = inputs[4].long().tolist() if len(inputs) > 4 and inputs[4] is not None else [1] * len(starts)

        # PyTorch doesn't support negative step in slicing; handle via flip
        flip_dims = []
        adj_starts = list(starts)
        adj_ends = list(ends)
        adj_steps = list(steps)
        for i, ax in enumerate(axes):
            st = steps[i] if i < len(steps) else 1
            if st < 0:
                flip_dims.append(int(ax))
                # After flip, reverse the start/end
                dim_size = data.shape[ax]
                s = starts[i]
                e = ends[i]
                # Convert negative indices
                if s < 0: s += dim_size
                if e < 0: e += dim_size
                if e < -dim_size: e = -1
                # After flip: new_start = dim_size - 1 - s, new_end = dim_size - 1 - e
                # But simpler: flip first, then slice with positive step
                new_s = dim_size - 1 - s if s < dim_size else 0
                new_e = dim_size - 1 - e if e >= 0 else dim_size
                if new_e < -dim_size:
                    new_e = dim_size
                adj_starts[i] = max(0, new_s)
                adj_ends[i] = new_e
                adj_steps[i] = -st
            elif st == 0:
                adj_steps[i] = 1

        if flip_dims:
            data = torch.flip(data, flip_dims)

        slices = [slice(None)] * data.dim()
        for i, ax in enumerate(axes):
            s = adj_starts[i]
            e = adj_ends[i]
            st = adj_steps[i]
            if e > 2**60:
                e = data.shape[ax]
            if e < -2**60:
                e = -data.shape[ax] - 1
            slices[ax] = slice(int(s), int(e), int(st))
        return data[tuple(slices)].contiguous()

    elif op_name == "Cast":
        to = attrs.get("to", 1)
        dtype_map = {
            1: torch.float32, 11: torch.float64,
            6: torch.int32, 7: torch.int64,
            5: torch.int16, 3: torch.int8,
            2: torch.uint8, 9: torch.bool,
            10: torch.float16, 16: torch.bfloat16,
        }
        return inputs[0].to(dtype_map.get(to, torch.float32))

    elif op_name == "Identity":
        return inputs[0]

    elif op_name == "Constant":
        # Constant value should have been stored in attrs as tensor id
        if "value" in attrs and isinstance(attrs["value"], str):
            return _get(attrs["value"])
        elif "value_float" in attrs:
            return torch.tensor(attrs["value_float"], dtype=torch.float32)
        elif "value_int" in attrs:
            return torch.tensor(attrs["value_int"], dtype=torch.int64)
        return torch.tensor(0.0)

    elif op_name == "Gemm":
        a = inputs[0].float()
        b = inputs[1].float()
        alpha = attrs.get("alpha", 1.0)
        beta = attrs.get("beta", 1.0)
        if attrs.get("transA", 0):
            a = a.T
        if attrs.get("transB", 0):
            b = b.T
        y = alpha * torch.matmul(a, b)
        if len(inputs) > 2 and inputs[2] is not None:
            y = y + beta * inputs[2].float()
        return y

    elif op_name == "Conv":
        x = inputs[0].float()
        w = inputs[1].float()
        bias = inputs[2].float() if len(inputs) > 2 and inputs[2] is not None else None
        pads = attrs.get("pads", [0,0,0,0])
        strides = attrs.get("strides", [1,1])
        dilations = attrs.get("dilations", [1,1])
        auto_pad = attrs.get("auto_pad", "NOTSET")

        if auto_pad.startswith("SAME"):
            # compute same padding
            kh, kw = w.shape[2], w.shape[3]
            pad_h = (kh - 1) // 2
            pad_w = (kw - 1) // 2
            padding = (pad_h, pad_w)
        else:
            if len(pads) == 4:
                padding = (pads[0], pads[1])
            else:
                padding = (0, 0)

        return torch.nn.functional.conv2d(x, w, bias, stride=tuple(strides),
                                          padding=padding, dilation=tuple(dilations))

    elif op_name == "BatchNormalization":
        x = inputs[0].float()
        scale = inputs[1].float()
        bias = inputs[2].float()
        mean = inputs[3].float()
        var_ = inputs[4].float()
        eps = attrs.get("epsilon", 1e-5)
        return torch.nn.functional.batch_norm(x, mean, var_, scale, bias, training=False, eps=eps)

    elif op_name == "MaxPool":
        x = inputs[0].float()
        ks = attrs.get("kernel_shape", [2,2])
        strides = attrs.get("strides", ks)
        pads = attrs.get("pads", [0,0,0,0])
        padding = (pads[0], pads[1]) if len(pads) >= 2 else (0, 0)
        return torch.nn.functional.max_pool2d(x, kernel_size=tuple(ks),
                                               stride=tuple(strides), padding=padding)

    elif op_name == "AveragePool":
        x = inputs[0].float()
        ks = attrs.get("kernel_shape", [2,2])
        strides = attrs.get("strides", ks)
        pads = attrs.get("pads", [0,0,0,0])
        padding = (pads[0], pads[1]) if len(pads) >= 2 else (0, 0)
        return torch.nn.functional.avg_pool2d(x, kernel_size=tuple(ks),
                                               stride=tuple(strides), padding=padding)

    elif op_name == "Dropout":
        return inputs[0]

    elif op_name == "ReduceMax":
        data = inputs[0]
        if len(inputs) > 1 and inputs[1] is not None:
            axes = inputs[1].long().tolist()
        else:
            axes = attrs.get("axes", [])
        keepdims = attrs.get("keepdims", 1) != 0
        for ax in sorted(axes, reverse=True):
            data = data.max(dim=ax, keepdim=keepdims).values
        return data

    elif op_name == "Sum":
        result = inputs[0].float()
        for t in inputs[1:]:
            if t is not None:
                result = result + t.float()
        return result

    elif op_name == "Tile":
        data = inputs[0]
        reps = inputs[1].long().tolist()
        return data.repeat(reps)

    elif op_name == "Expand":
        data = inputs[0]
        shape = inputs[1].long().tolist()
        return data.expand(shape)

    elif op_name == "Where":
        cond = inputs[0].bool()
        return torch.where(cond, inputs[1], inputs[2])

    elif op_name == "Greater":
        # Compare in original dtype; float cast only needed if dtypes differ
        a, b = inputs[0], inputs[1]
        if a.dtype != b.dtype:
            a, b = a.float(), b.float()
        a, b = _broadcast(a, b)
        return a > b

    elif op_name == "Max":
        result = inputs[0].float()
        for t in inputs[1:]:
            if t is not None:
                result = torch.max(result, t.float())
        return result

    elif op_name == "Softplus":
        return torch.nn.functional.softplus(inputs[0].float())

    elif op_name == "ArgMax":
        axis = attrs.get("axis", 0)
        return torch.argmax(inputs[0].float(), dim=axis)

    elif op_name == "QuantizeLinear":
        x = inputs[0].float()
        y_scale = inputs[1].float()
        y_zp = inputs[2].float() if len(inputs) > 2 and inputs[2] is not None else None
        scaled = torch.round(x / y_scale)
        if y_zp is not None:
            scaled = scaled + y_zp
        return scaled

    elif op_name == "DequantizeLinear":
        x = inputs[0].float()
        x_scale = inputs[1].float()
        x_zp = inputs[2].float() if len(inputs) > 2 and inputs[2] is not None else None
        dq = x - x_zp if x_zp is not None else x
        return dq * x_scale

    # Fused ops
    elif op_name == "GemmRelu":
        g = _exec_op("Gemm", inputs, attrs)
        return torch.relu(g)

    elif op_name == "AddExp":
        a, b = _broadcast(inputs[0].float(), inputs[1].float())
        return torch.exp(a + b)

    elif op_name == "AddLog":
        a, b = _broadcast(inputs[0].float(), inputs[1].float())
        return torch.log(a + b)

    elif op_name == "SubExp":
        a, b = _broadcast(inputs[0].float(), inputs[1].float())
        return torch.exp(a - b)

    elif op_name == "NegSoftplus":
        return -torch.nn.functional.softplus(inputs[0].float())

    elif op_name == "ImageScaler":
        x = inputs[0].float()
        scale = attrs.get("scale", 1.0)
        bias_vals = attrs.get("bias", [])
        result = x * scale
        if bias_vals:
            bias_t = torch.tensor(bias_vals, dtype=torch.float32).reshape(1, -1, 1, 1)
            result = result + bias_t
        return result

    else:
        raise ValueError(f"Unknown op: {op_name}")

# ============ Transfer helpers for HW backend ============

def cmd_get_stats(args):
    """Get tensor statistics: sum, max abs, min, max."""
    tid = args["id"]
    t = _get(tid).cpu().float()
    flat = t.reshape(-1)
    return {
        "sum": float(flat.sum()),
        "abs_sum": float(flat.abs().sum()),
        "max_abs": float(flat.abs().max()),
        "min": float(flat.min()),
        "max": float(flat.max()),
        "numel": int(flat.numel())
    }

def cmd_to_long_array_2d(args):
    """Convert tensor to 2D list of longs for HW interface."""
    tid = args["id"]
    t = _get(tid).cpu()
    rows = args.get("rows", t.shape[0] if t.dim() >= 1 else 1)
    cols = args.get("cols", t.shape[-1] if t.dim() >= 1 else 1)
    flat = t.reshape(-1).long()
    # Encode as base64 int64 array
    raw = flat.numpy().astype(np.int64).tobytes()
    return {"data_b64": base64.b64encode(raw).decode("ascii"), "rows": rows, "cols": cols}

def cmd_from_long_array_2d(args):
    """Create tensor from 2D long array (base64 int64)."""
    raw = base64.b64decode(args["data_b64"])
    rows = args["rows"]
    cols = args["cols"]
    arr = np.frombuffer(raw, dtype=np.int64).reshape(rows, cols)
    t = torch.from_numpy(arr.copy())
    dtype = _to_torch_dtype(args.get("dtype", "int64"))
    t = t.to(dtype)
    return _tensor_info(_store(t))

def cmd_broadcast_tensors(args):
    """Broadcast multiple tensors to the same shape."""
    ids = args["ids"]
    tensors = [_tensors[str(tid)] for tid in ids]
    broadcasted = torch.broadcast_tensors(*tensors)
    return [_tensor_info(_store(b.contiguous())) for b in broadcasted]

# ============ Main Loop ============

def cmd_save_tensor_pb(args):
    """Save a tensor as an ONNX TensorProto .pb file."""
    tid = str(args["id"])
    path = args["path"]
    name = args.get("name", "")
    t = _get(tid)
    arr = t.cpu().numpy()
    tp = numpy_helper.from_array(arr, name=name)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(tp.SerializeToString())
    return {"path": path, "dtype": str(arr.dtype), "shape": list(arr.shape)}

COMMANDS = {
    "load_model": cmd_load_model,
    "load_test_data": cmd_load_test_data,
    "create_tensor": cmd_create_tensor,
    "get_tensor": cmd_get_tensor,
    "get_info": cmd_get_info,
    "get_values": cmd_get_values,
    "delete_tensor": cmd_delete_tensor,
    "op": cmd_op,
    "get_stats": cmd_get_stats,
    "to_long_array_2d": cmd_to_long_array_2d,
    "from_long_array_2d": cmd_from_long_array_2d,
    "broadcast_tensors": cmd_broadcast_tensors,
    "save_tensor_pb": cmd_save_tensor_pb,
}

def main():
    # Signal ready
    sys.stdout.write(json.dumps({"status": "ready", "torch": torch.__version__, "cuda": torch.cuda.is_available()}) + "\n")
    sys.stdout.flush()

    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            req = json.loads(line)
            cmd = req.get("cmd")
            if cmd == "shutdown":
                sys.stdout.write(json.dumps({"status": "shutdown"}) + "\n")
                sys.stdout.flush()
                break
            handler = COMMANDS.get(cmd)
            if handler is None:
                resp = {"error": f"Unknown command: {cmd}"}
            else:
                result = handler(req.get("args", {}))
                resp = {"result": result}
        except Exception as e:
            resp = {"error": str(e), "traceback": traceback.format_exc()}

        sys.stdout.write(json.dumps(resp) + "\n")
        sys.stdout.flush()

if __name__ == "__main__":
    main()

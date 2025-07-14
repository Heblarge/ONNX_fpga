package org.forwarder.backend.impls.HWAccelerated.opsets.v8.ops;

import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v8.ops.ExpandV8;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class HWAcceleratedExpandV8 extends HWAcceleratedOperator implements ExpandV8 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        ExpandInputsV8<INDArray> castedInputs = new ExpandInputsV8<>(node, inputs);
        INDArray input = castedInputs.getInput();
        INDArray shapeTensor = castedInputs.getShape();

        INDArray expanded = expand(input, shapeTensor);

        return new ExpandOutputV8<>(expanded);
    }

    /**
     * 实现 Expand 算子逻辑：将输入张量 broadcast 到目标形状
     *
     * @param input        输入张量
     * @param shapeTensor  目标 shape，ONNX 中为 1D int64 张量
     * @return broadcast 结果
     */
    protected INDArray expand(INDArray input, INDArray shapeTensor) {
        long[] targetShape = shapeTensor.toLongVector();
        return input.broadcast(targetShape);
    }
}


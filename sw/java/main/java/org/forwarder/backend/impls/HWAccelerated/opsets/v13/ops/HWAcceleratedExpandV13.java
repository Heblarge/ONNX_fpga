package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.opsets.v8.ops.HWAcceleratedExpandV8;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ExpandV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class HWAcceleratedExpandV13 extends HWAcceleratedExpandV8 implements ExpandV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        ExpandInputsV13<INDArray> castedInputs = new ExpandInputsV13<>(node, inputs);
        INDArray input = castedInputs.getInput();
        INDArray shapeTensor = castedInputs.getShape();
        INDArray expanded = expand(input, shapeTensor);
        return new ExpandOutputV8<>(expanded);
    }

    @Override
    protected INDArray expand(INDArray input, INDArray shapeTensor) {
        if (shapeTensor.rank() != 1) {
            throw new IllegalArgumentException("Shape tensor must be 1-D");
        }
        long[] targetShape = shapeTensor.toLongVector();
        if (targetShape.length < input.rank()) {
            long[] padded = new long[input.rank()];
            int pad = input.rank() - targetShape.length;
            // 左侧补1
            for (int i = 0; i < pad; i++) padded[i] = 1;
            System.arraycopy(targetShape, 0, padded, pad, targetShape.length);
            targetShape = padded;
        }
        for (long d : targetShape) if (d < 0)
            throw new IllegalArgumentException("Negative dimension: " + d);
        return input.broadcast(targetShape);
    }
}
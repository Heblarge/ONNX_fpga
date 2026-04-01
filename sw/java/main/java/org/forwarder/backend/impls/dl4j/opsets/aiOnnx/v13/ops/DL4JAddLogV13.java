package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.AddLogV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class DL4JAddLogV13 extends DL4JAiOnnxOperator implements AddLogV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        AddLogV13.AddLogInputsV13<INDArray> castedInputs = new AddLogV13.AddLogInputsV13<>(node, inputs);
        return new AddLogV13.AddLogOutputV13<>(addLog(castedInputs.getA(), castedInputs.getB()));
    }

    /**
     * 对 a + b 的结果取自然对数，确保输入全为正数
     */
    public INDArray addLog(INDArray a, INDArray b) {
        INDArray sum = a.add(b);
        return log(sum);
    }

    /**
     * 对数运算，确保输入全为正数
     */
    public INDArray log(INDArray x) {
        if (x.minNumber().floatValue() <= 0) {
            throw new IllegalArgumentException("AddLog input must be positive");
        }
        return Transforms.log(x);
    }
}

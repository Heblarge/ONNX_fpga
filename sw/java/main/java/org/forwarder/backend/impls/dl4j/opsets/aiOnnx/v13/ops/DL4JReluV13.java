package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ReluV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class DL4JReluV13 extends DL4JAiOnnxOperator implements ReluV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        ReluV13.ReluInputsV13<INDArray> castedInputs = new ReluV13.ReluInputsV13<>(node, inputs);
        INDArray x = castedInputs.getInput();
        return new ReluV13.ReluOutputV13<>(this.relu(x));
    }


    public INDArray relu(INDArray x) {
        // 直接调用一个返回新结果的函数
        return Transforms.relu(x);
    }
}
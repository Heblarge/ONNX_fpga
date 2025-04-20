package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ExpV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class DL4JExpV13 extends DL4JAiOnnxOperator implements ExpV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        ExpV13.LogInputsV13<INDArray> castedInputs = new ExpV13.LogInputsV13<>(node, inputs);
        INDArray input = castedInputs.getInput();
        return new ExpV13.LogOutputV13<>(this.exp(input));
    }

    public INDArray exp(INDArray x) {
        return Nd4j.getExecutioner().exec(new org.nd4j.linalg.api.ops.impl.transforms.strict.Exp(x));
    }
}
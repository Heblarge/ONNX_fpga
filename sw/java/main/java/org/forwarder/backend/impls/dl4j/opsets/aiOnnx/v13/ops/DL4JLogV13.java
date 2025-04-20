package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.LogV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class DL4JLogV13 extends DL4JAiOnnxOperator implements LogV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        LogV13.LogInputsV13<INDArray> castedInputs = new LogV13.LogInputsV13<>(node, inputs);
        INDArray input = castedInputs.getInput();
        return new LogV13.LogOutputV13<>(this.log(input));
    }

    public INDArray log(INDArray x) {
        if (x.minNumber().doubleValue() <= 0) {
            throw new IllegalArgumentException("Log input must be positive");
        }
        //return Nd4j.math().log(x);
        return Nd4j.getExecutioner().exec(new org.nd4j.linalg.api.ops.impl.transforms.strict.Log(x));
    }
}
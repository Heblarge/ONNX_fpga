package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.SoftplusV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class DL4JSoftplusV13 extends DL4JAiOnnxOperator implements SoftplusV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        SoftplusV13.SoftplusInputsV13<INDArray> castedInputs = new SoftplusV13.SoftplusInputsV13<>(node, inputs);
        INDArray x = castedInputs.getX();
        return new SoftplusV13.SoftplusOutputV13<>(this.softplus(x));
    }


    public INDArray softplus(INDArray x) {
        return Nd4j.nn.softplus(x);
    }
}
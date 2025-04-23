package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import java.util.List;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.MaxV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class DL4JMaxV13 extends DL4JAiOnnxOperator implements MaxV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        MaxInputsV13<INDArray> castedOperatorInputs = new MaxInputsV13<>(node, inputs);
        List<INDArray> inputTensors = castedOperatorInputs.getInputTensors();
        INDArray result = elementwiseMax(inputTensors);
        return new MaxOutputV13<>(result);
    }

    /**
     * Performs element-wise max over a list of input tensors (supports broadcasting)
     */
    protected INDArray elementwiseMax(List<INDArray> inputTensors) {
        if (inputTensors == null || inputTensors.isEmpty()) {
            throw new IllegalArgumentException("Max operator requires at least one input tensor.");
        }

        INDArray result = inputTensors.get(0);
        for (int i = 1; i < inputTensors.size(); i++) {
            INDArray other = inputTensors.get(i);
            // Broadcasting (manual shape alignment if needed)
            result = Transforms.max(result.broadcast(result.shape()), other.broadcast(result.shape()));
        }

        return result;
    }
}







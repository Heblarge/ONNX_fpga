package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import java.util.List;
import java.util.stream.Collectors; // 引入 Collectors

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

        List<INDArray> inputTensors = castedOperatorInputs.getInputFields().stream()
                .map(field -> field.getData())
                .collect(Collectors.toList());

        INDArray result = elementwiseMax(inputTensors);
        return new MaxOutputV13<>(result);
    }

    /**
     * 对输入的张量列表进行逐元素的 "max" 操作
     */
    protected INDArray elementwiseMax(List<INDArray> inputTensors) {
        if (inputTensors == null || inputTensors.isEmpty()) {
            throw new IllegalArgumentException("Max operator requires at least one input tensor.");
        }

        INDArray result = inputTensors.get(0);
        for (int i = 1; i < inputTensors.size(); i++) {
            INDArray other = inputTensors.get(i);
            // ▼▼▼ 关键修改 2：简化广播逻辑 ▼▼▼
            // ND4J 的 Transforms.max 操作已经内置了广播功能，无需手动 broadcast
            result = Transforms.max(result, other);
        }

        return result;
    }
}
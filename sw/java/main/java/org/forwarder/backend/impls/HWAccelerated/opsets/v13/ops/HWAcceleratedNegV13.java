package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.NegV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class HWAcceleratedNegV13 extends HWAcceleratedOperator implements NegV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        NegInputsV13<INDArray> castedInputs = new NegInputsV13<>(node, inputs);
        INDArray x = castedInputs.getX();
        return new NegOutputV13<>(this.neg(x));
    }

    public INDArray neg(INDArray x) {
        // 使用ND4J的neg方法计算负值
        return x.neg();
    }
}
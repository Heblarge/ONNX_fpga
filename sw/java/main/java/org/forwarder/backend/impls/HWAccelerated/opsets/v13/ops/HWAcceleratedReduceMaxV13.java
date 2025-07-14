package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import com.google.common.primitives.Ints;
import org.forwarder.backend.impls.HWAccelerated.opsets.v1.ops.HWAcceleratedReduceMaxV1;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ReduceMaxV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import java.util.List;

public class HWAcceleratedReduceMaxV13 extends HWAcceleratedReduceMaxV1 implements ReduceMaxV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        ReduceMaxInputsV13<INDArray> castedOperatorInputs = new ReduceMaxInputsV13<INDArray>(node, inputs);
        INDArray data = castedOperatorInputs.getData();
        List<Long> axes = castedOperatorInputs.getAxes();
        Long keepdims = castedOperatorInputs.getKeepdims();
        return new ReduceMaxOutputV13<INDArray>(this.reduceMax(data, axes, keepdims));
    }

    protected INDArray reduceMax(INDArray data, List<Long> axes, Long keepdims) {
        return data.max(keepdims == 1L ? true : false, Ints.toArray(axes));
    }
}

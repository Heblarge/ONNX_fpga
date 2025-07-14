package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.opsets.v4.ops.HWAcceleratedConcatV4;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ConcatV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import java.util.List;

public class HWAcceleratedConcatV13 extends HWAcceleratedConcatV4 implements ConcatV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        ConcatInputsV13<INDArray> castedInputs = new ConcatInputsV13<>(node, inputs);
        List<INDArray> inputList = castedInputs.getInputs();
        long axis = normalizeAxis(castedInputs.getAxis(), inputList.get(0).rank());
        return new ConcatOutputV13<>(concat(inputList, axis));
    }

    /**
     * 添加对新增的负 axis 输入的支持
     */
    private int normalizeAxis(long axis, int rank) {
        if (axis < 0) {
            axis += rank;
        }
        if (axis < 0 || axis >= rank) {
            throw new IllegalArgumentException(String.format(
                    "axis=%d illegal，dimension of the input tensor should be %d", axis, rank));
        }
        return (int) axis;
    }

    @Override
    protected INDArray concat(List<INDArray> inputs, Long axis) {
        return Nd4j.concat(axis.intValue(), inputs.toArray(new INDArray[0]));
    }
}

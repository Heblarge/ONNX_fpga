package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import java.util.List;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v4.ops.DL4JConcatV4;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.operator.OperatorOutputs;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ConcatV13;

public class DL4JConcatV13 extends DL4JConcatV4 implements ConcatV13 {

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
    protected INDArray concat(List<INDArray> inputs, Long axisLong) {
        int rank = inputs.get(0).rank();
        int axis = normalizeAxis(axisLong, rank);

        long[] shapeRef = inputs.get(0).shape();

        for (int i = 1; i < inputs.size(); i++) {
            long[] shape = inputs.get(i).shape();
            if (shape.length != shapeRef.length) {
                throw new IllegalArgumentException(String.format(
                        "Input tensor %d rank %d != reference rank %d", i, shape.length, shapeRef.length));
            }
            for (int dim = 0; dim < shapeRef.length; dim++) {
                if (dim == axis) continue;
                if (shape[dim] != shapeRef[dim]) {
                    throw new IllegalArgumentException(String.format(
                            "Input tensor %d shape mismatch at dim %d: expected %d, got %d",
                            i, dim, shapeRef[dim], shape[dim]));
                }
            }
        }

        return Nd4j.concat(axis, inputs.toArray(new INDArray[0]));
    }
}

package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.DequantizeLinearV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import java.util.Arrays;

public class HWAcceleratedDequantizeLinearV13 extends HWAcceleratedOperator implements DequantizeLinearV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        DequantizeLinearInputsV13<INDArray> castedInputs = new DequantizeLinearInputsV13<>(node, inputs);
        INDArray x = castedInputs.getX();
        INDArray xScale = castedInputs.getXScale();
        INDArray xZeroPoint = castedInputs.hasXZeroPoint() ? castedInputs.getXZeroPoint() : Nd4j.scalar(0).castTo(x.dataType()); //null;

        int rank = x.rank();
        long axis = castedInputs.getAxis();
        int normalizedAxis = normalizeAxis(axis, rank);
        INDArray result = dequantizeLinear(x, xScale, xZeroPoint, normalizedAxis);
        return new DequantizeLinearOutputV13<>(result);
    }

    private int normalizeAxis(long axis, int rank) {
        if (axis < 0) { axis += rank; }
        if (axis < 0 || axis >= rank) {
            throw new IllegalArgumentException(
                    String.format("axis=%d is invalid for input tensor of rank %d", axis, rank)
            );
        }
        return (int) axis;
    }

    private INDArray dequantizeLinear(INDArray x, INDArray xScale, INDArray xZeroPoint, int axis) {
        if (xScale.rank() == 1) {
            xScale = reshapeForBroadcast(xScale, x.rank(), axis);
        }
        if (xZeroPoint.rank() == 1) {
            xZeroPoint = reshapeForBroadcast(xZeroPoint, x.rank(), axis);
        }

        INDArray xf  = x.castTo(DataType.FLOAT);
        INDArray zpf = xZeroPoint.castTo(DataType.FLOAT);

        return xf.sub(zpf).mul(xScale);
    }

    private INDArray reshapeForBroadcast(INDArray tensor, int targetRank, int axis) {
        long[] newShape = new long[targetRank];
        Arrays.fill(newShape, 1L);
        newShape[axis] = tensor.length();
        return tensor.reshape(newShape);
    }
}
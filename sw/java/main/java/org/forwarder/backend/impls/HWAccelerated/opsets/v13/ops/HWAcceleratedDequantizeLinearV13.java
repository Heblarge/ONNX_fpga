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
        INDArray xZeroPoint = castedInputs.hasXZeroPoint() ? castedInputs.getXZeroPoint() : Nd4j.scalar(0).castTo(x.dataType());

        if (xScale.isScalar() && (xZeroPoint == null || xZeroPoint.isScalar())) {
            INDArray result = dequantizeLinear(x, xScale, xZeroPoint, 0);
            return new DequantizeLinearOutputV13<>(result);
        } else {
            int rank = x.rank();
            long axis = castedInputs.getAxis();
            int normalizedAxis = normalizeAxis(axis, rank);
            INDArray result = dequantizeLinear(x, xScale, xZeroPoint, normalizedAxis);
            return new DequantizeLinearOutputV13<>(result);
        }
    }

    private int normalizeAxis(long axis, int rank) {
        if (axis < 0) {
            axis += rank;
        }
        if (rank > 0 && (axis < 0 || axis >= rank)) {
            throw new IllegalArgumentException(
                    String.format("axis=%d is invalid for input tensor of rank %d", axis, rank)
            );
        }
        return (int) axis;
    }

    private INDArray dequantizeLinear(INDArray x, INDArray xScale, INDArray xZeroPoint, int axis) {
        // Reshape scale and zero_point if they are vectors for per-axis dequantization
        if (xScale.isVector() && xScale.length() > 1) {
            xScale = reshapeForBroadcast(xScale, x.rank(), axis);
        }
        if (xZeroPoint != null && xZeroPoint.isVector() && xZeroPoint.length() > 1) {
            xZeroPoint = reshapeForBroadcast(xZeroPoint, x.rank(), axis);
        }

        // The formula is y = (x - x_zero_point) * x_scale
        // Cast quantized inputs to FLOAT for the calculation
        INDArray x_f = x.castTo(DataType.FLOAT);
        INDArray zp_f = xZeroPoint.castTo(DataType.FLOAT);

        return x_f.sub(zp_f).mul(xScale);
    }

    private INDArray reshapeForBroadcast(INDArray tensor, int targetRank, int axis) {
        long[] newShape = new long[targetRank];
        java.util.Arrays.fill(newShape, 1L);
        newShape[axis] = tensor.length();
        return tensor.reshape(newShape);
    }
}
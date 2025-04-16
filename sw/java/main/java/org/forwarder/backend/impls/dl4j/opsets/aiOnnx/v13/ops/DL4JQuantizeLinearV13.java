package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.ops.impl.transforms.clip.ClipByValue;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.QuantizeLinearV13;
import org.onnx4j.opsets.operator.OperatorOutputs;
import java.util.Arrays;

public class DL4JQuantizeLinearV13 extends DL4JAiOnnxOperator implements QuantizeLinearV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        QuantizeLinearInputsV13<INDArray> castedInputs = new QuantizeLinearInputsV13<>(node, inputs);
        INDArray x = castedInputs.getX();
        INDArray yScale = castedInputs.getYScale();
        INDArray yZeroPoint = castedInputs.hasYZeroPoint() ? castedInputs.getYZeroPoint() : null;

        int rank = x.rank();
        long axis = castedInputs.getAxis();
        int normalizedAxis = normalizeAxis(axis, rank);
        INDArray result = quantizeLinear(x, yScale, yZeroPoint, normalizedAxis);
        return new QuantizeLinearOutputV13<>(result);
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

    private INDArray quantizeLinear(INDArray x, INDArray yScale, INDArray yZeroPoint, int axis) {
        if (yScale.rank() == 1) {
            yScale = reshapeForBroadcast(yScale, x.rank(), axis);
        }
        if (yZeroPoint != null && yZeroPoint.rank() == 1) {
            yZeroPoint = reshapeForBroadcast(yZeroPoint, x.rank(), axis);
        }

        INDArray scaled = x.div(yScale);
        INDArray rounded = Transforms.round(scaled, false);

        if (yZeroPoint == null) {
            DataType outputType = inferOutputType(yZeroPoint);
            yZeroPoint = Nd4j.scalar(0).castTo(outputType);
        }

        INDArray quantized = rounded.add(yZeroPoint);

        quantized = applySaturation(quantized, yZeroPoint.dataType());
        return quantized;
    }

    private INDArray reshapeForBroadcast(INDArray tensor, int targetRank, int axis) {
        int[] newShape = new int[targetRank];
        Arrays.fill(newShape, 1);
        newShape[axis] = (int) tensor.length();
        return tensor.reshape(newShape);
    }

    private INDArray applySaturation(INDArray quantized, DataType dataType) {
        switch (dataType) {
            case UBYTE:
                return Nd4j.getExecutioner().exec(new ClipByValue(quantized, 0, 255))[0];
            case BYTE:
                return Nd4j.getExecutioner().exec(new ClipByValue(quantized, -128, 127))[0];
            default:
                throw new UnsupportedOperationException("Unsupported data type: " + dataType);
        }
    }

    private DataType inferOutputType(INDArray yZeroPoint) {
        return (yZeroPoint != null) ? yZeroPoint.dataType() : DataType.UBYTE;
    }
}
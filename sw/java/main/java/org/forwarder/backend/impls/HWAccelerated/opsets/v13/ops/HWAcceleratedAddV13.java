package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.AddV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

/**
 * Implements the Add operation using a hardware accelerator.
 * This version automatically pads inputs to be multiples of 32 for hardware compatibility.
 * It handles tensors of any rank >= 2 and supports Numpy-style broadcasting.
 */
public class HWAcceleratedAddV13 extends HWAcceleratedOperator implements AddV13 {

    private final int HW_DIM_MULTIPLE = 32;

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        AddInputsV13<INDArray> castedInputs = new AddInputsV13<>(node, inputs);
        INDArray matrixA = castedInputs.getA();
        INDArray matrixB = castedInputs.getB();
        INDArray outputTensor = this.add(matrixA, matrixB);
        return new AddOutputV13<>(outputTensor);
    }

    /**
     * Public dispatcher for the Add operation.
     * It handles broadcasting and reshapes tensors for efficient hardware execution.
     */
    public INDArray add(INDArray a, INDArray b) {
        if (!java.util.Arrays.equals(a.shape(), b.shape())) {
            long[] broadcastShape = getBroadcastShape(a.shape(), b.shape());
            a = a.broadcast(broadcastShape);
            b = b.broadcast(broadcastShape);
        }

        if (a.rank() == 2) {
            return add2D(a, b);
        } else if (a.rank() > 2) {
            long[] finalShape = a.shape();
            long numCols = finalShape[finalShape.length - 1];
            long numRows = a.length() / numCols;
            INDArray reshapedA = a.reshape('c', numRows, numCols);
            INDArray reshapedB = b.reshape('c', numRows, numCols);
            INDArray result2D = add2D(reshapedA, reshapedB);
            return result2D.reshape('c', finalShape);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported tensor rank for Add: " + a.rank() + ". Only ranks >= 2 are supported."
            );
        }
    }

    /**
     * Calculates the resulting shape of a broadcasting operation between two shapes.
     * Follows Numpy-style broadcasting rules.
     */
    private long[] getBroadcastShape(long[] shapeA, long[] shapeB) {
        int rankA = shapeA.length;
        int rankB = shapeB.length;
        int maxRank = Math.max(rankA, rankB);
        long[] resultShape = new long[maxRank];

        for (int i = 1; i <= maxRank; i++) {
            long dimA = (rankA - i >= 0) ? shapeA[rankA - i] : 1;
            long dimB = (rankB - i >= 0) ? shapeB[rankB - i] : 1;

            if (dimA != dimB && dimA != 1 && dimB != 1) {
                throw new IllegalArgumentException("Shapes " + java.util.Arrays.toString(shapeA) + " and "
                        + java.util.Arrays.toString(shapeB) + " are not broadcastable.");
            }
            resultShape[maxRank - i] = Math.max(dimA, dimB);
        }
        return resultShape;
    }

    /**
     * Calculates the ceiling of a value to the nearest multiple.
     */
    private int ceilToMultiple(int value, int multiple) {
        if (multiple == 0) return value;
        return ((value + multiple - 1) / multiple) * multiple;
    }

    /**
     * Performs 2D element-wise addition with padding and slicing.
     */
    private INDArray add2D(INDArray a, INDArray b) {
        if (!java.util.Arrays.equals(a.shape(), b.shape())) {
            throw new IllegalArgumentException("Input shapes must be identical for hardware acceleration.");
        }

        int originalRows = (int) a.rows();
        int originalCols = (int) a.columns();

        int paddedRows = ceilToMultiple(originalRows, HW_DIM_MULTIPLE);
        int paddedCols = ceilToMultiple(originalCols, HW_DIM_MULTIPLE);

        INDArray paddedA = Nd4j.zeros(paddedRows, paddedCols);
        paddedA.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols)}, a);

        INDArray paddedB = Nd4j.zeros(paddedRows, paddedCols);
        paddedB.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols)}, b);

        INDArray paddedResult = addOnAccelerator(paddedA, paddedB, paddedRows, paddedCols);

        return paddedResult.get(NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols));
    }

    /**
     * Private helper to run 2D element-wise addition on the hardware simulator.
     */
    private INDArray addOnAccelerator(INDArray a, INDArray b, int rows, int cols) {
        // Added fixed-point conversion logic to match Sub operator
        int fracWidth = 9; // Should be determined from onnx graph for optimal value
        double scaleFactor = Math.pow(2, fracWidth);

        int[][] fixedPointA = new int[rows][cols];
        int[][] fixedPointB = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                fixedPointA[i][j] = (int)Math.round(a.getFloat(i, j) * scaleFactor);
                fixedPointB[i][j] = (int)Math.round(b.getFloat(i, j) * scaleFactor);
            }
        }

        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "elementadd",
                0,
                false,
                "none",
                0,
                0,
                0,
                0,
                rows,
                cols,
                cols
        );

        int[][] fixedPointOutput = AcceleratorSimInterface.runRefOneInst(fixedPointA, fixedPointB, instruction);

        float[] output = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                output[i * cols + j] = (float)(((double)(fixedPointOutput[i][j])) / scaleFactor);
            }
        }

        return Nd4j.create(output).reshape(rows, cols);
    }
}
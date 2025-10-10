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

import java.util.List;

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
        List<Float> fpgaInScales = castedInputs.getFpgaInScales();
        List<Long> fpgaInShift = castedInputs.getFpgaInShift();
        List<Float> fpgaOutScale = castedInputs.getFpgaOutScale();
        List<Long> fpgaOutShift = castedInputs.getFpgaOutShift();
        INDArray outputTensor = this.add(matrixA, matrixB, fpgaInShift, fpgaOutShift);
        return new AddOutputV13<>(outputTensor);
    }

    /**
     * Calculates the ceiling of a value to the nearest multiple.
     */
    private int ceilToMultiple(int value, int multiple) {
        if (multiple == 0) return value;
        return ((value + multiple - 1) / multiple) * multiple;
    }

    /**
     * Public dispatcher for the Add operation.
     * It handles broadcasting and reshapes tensors for efficient hardware execution.
     */
    public INDArray add(INDArray a, INDArray b, List<Long> fpgaInShift, List<Long> fpgaOutShift) {
        if (!java.util.Arrays.equals(a.shape(), b.shape())) {
            long[] broadcastShape = getBroadcastShape(a.shape(), b.shape());
            a = a.broadcast(broadcastShape);
            b = b.broadcast(broadcastShape);
        }

        if (a.rank() == 2) {
            return add2D(a, b, fpgaInShift, fpgaOutShift);
        } else if (a.rank() > 2) {
            long[] finalShape = a.shape();
            long numCols = finalShape[finalShape.length - 1];
            long numRows = a.length() / numCols;
            INDArray reshapedA = a.reshape('c', numRows, numCols);
            INDArray reshapedB = b.reshape('c', numRows, numCols);
            INDArray result2D = add2D(reshapedA, reshapedB, fpgaInShift, fpgaOutShift);
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
     * Performs 2D element-wise addition with padding and slicing.
     */
    private INDArray add2D(INDArray a, INDArray b, List<Long> fpgaInShift, List<Long> fpgaOutShift) {
        if (!java.util.Arrays.equals(a.shape(), b.shape())) {
            throw new IllegalArgumentException("Input shapes must be identical for hardware acceleration.");
        }

        int originalRows = (int) a.rows();
        int originalCols = (int) a.columns();

        int paddedRows = ceilToMultiple(originalRows, HW_DIM_MULTIPLE);
        int paddedCols = ceilToMultiple(originalCols, HW_DIM_MULTIPLE);

        INDArray paddedA = Nd4j.zeros(a.dataType(), paddedRows, paddedCols);
        paddedA.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols)}, a);

        INDArray paddedB = Nd4j.zeros(b.dataType(), paddedRows, paddedCols);
        paddedB.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols)}, b);

        INDArray paddedResult = addOnAccelerator(paddedA, paddedB, paddedRows, paddedCols, fpgaInShift, fpgaOutShift);

        return paddedResult.get(NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols));
    }

    /**
     * Private helper to run 2D element-wise addition on the hardware simulator.
     */
    private INDArray addOnAccelerator(INDArray a, INDArray b, int rows, int cols, List<Long> fpgaInShift, List<Long> fpgaOutShift) {
        if (!fpgaInShift.get(0).equals(fpgaInShift.get(1))) {
            throw new IllegalArgumentException("For element-wise Add, input shifts (scales) must be identical.");
        }

        long[][] fixedPointA = new long[rows][cols];
        long[][] fixedPointB = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                fixedPointA[i][j] = a.getLong(i, j);
                fixedPointB[i][j] = b.getLong(i, j);
            }
        }
        int shiftAmount = (int) (fpgaInShift.get(0) - fpgaOutShift.get(0));

        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "elementadd",
                shiftAmount,
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

        long[][] hardwareResult = AcceleratorSimInterface.runRefOneInst(fixedPointA, fixedPointB, instruction);

        long[] output = new long[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                output[i * cols + j] =  hardwareResult[i][j];
            }
        }

        return Nd4j.create(output, new long[]{rows, cols}, a.dataType());
    }
}
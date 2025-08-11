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
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.SubV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

/**
 * Implements the Sub operation using a hardware accelerator,
 * with support for both 2D and 3D (batched) tensors.
 * This version automatically pads inputs to be multiples of 32 for hardware compatibility.
 */
public class HWAcceleratedSubV13 extends HWAcceleratedOperator implements SubV13 {

    private final int HW_DIM_MULTIPLE = 32;

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        SubInputsV13<INDArray> castedInputs = new SubInputsV13<>(node, inputs);
        INDArray matrixA = castedInputs.getA();
        INDArray matrixB = castedInputs.getB();
        INDArray outputTensor = this.sub(matrixA,matrixB);
        return new SubOutputV13<>(outputTensor);
    }

    /**
     * Public dispatcher for the Sub operation. It checks the tensor rank
     * and calls the appropriate implementation.
     */
    public INDArray sub(INDArray a, INDArray b) {
        if (a.rank() == 2 && b.rank() == 2) {
            return sub2D(a, b);
        } else if (a.rank() == 3 && b.rank() == 3) {
            return sub3D(a, b);
        } else {
            throw new IllegalArgumentException("Unsupported or mismatched tensor ranks for Sub: A=" + a.rank() + ", B=" + b.rank());
        }
    }

    /**
     * Calculates the ceiling of a value to the nearest multiple.
     */
    private int ceilToMultiple(int value, int multiple) {
        if (multiple == 0) return value;
        return ((value + multiple - 1) / multiple) * multiple;
    }

    /**
     * Performs 2D element-wise subtraction with padding and slicing.
     */
    private INDArray sub2D(INDArray a, INDArray b) {
        if (!java.util.Arrays.equals(a.shape(), b.shape())) {
            throw new IllegalArgumentException("Input shapes must be identical for hardware acceleration.");
        }

        int originalRows = (int) a.rows();
        int originalCols = (int) a.columns();

        // 1. Calculate padded dimensions
        int paddedRows = ceilToMultiple(originalRows, HW_DIM_MULTIPLE);
        int paddedCols = ceilToMultiple(originalCols, HW_DIM_MULTIPLE);

        // 2. Create padded INDArrays
        INDArray paddedA = Nd4j.zeros(paddedRows, paddedCols);
        paddedA.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols)}, a);

        INDArray paddedB = Nd4j.zeros(paddedRows, paddedCols);
        paddedB.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols)}, b);

        // 3. Call the hardware accelerator with the padded data
        INDArray paddedResult = subOnAccelerator(paddedA, paddedB, paddedRows, paddedCols);

        // 4. Slice the result back to the original output shape
        return paddedResult.get(NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols));
    }

    /**
     * Performs 3D (batched) element-wise subtraction.
     */
    private INDArray sub3D(INDArray a, INDArray b) {
        if (!java.util.Arrays.equals(a.shape(), b.shape())) {
            throw new IllegalArgumentException("Input shapes must be identical for 3D hardware subtraction.");
        }

        long batch = a.shape()[0];
        long rows = a.shape()[1];
        long cols = a.shape()[2];

        INDArray result = Nd4j.createUninitialized(new long[]{batch, rows, cols}, 'c');

        for (int i = 0; i < (int) batch; i++) {
            INDArray sliceA = a.slice(i);
            INDArray sliceB = b.slice(i);
            INDArray diffSlice = sub2D(sliceA, sliceB); // sub2D now handles padding
            result.putSlice(i, diffSlice);
        }

        return result;
    }

    /**
     * Private helper to run element-wise subtraction on the hardware simulator.
     * This method's logic is preserved exactly as requested.
     */
    private INDArray subOnAccelerator(INDArray a, INDArray b, int rows, int cols) {
        int[][] fixedPointA = new int[rows][cols];
        int[][] fixedPointB = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                fixedPointA[i][j] = Math.round(a.getFloat(i, j));
                fixedPointB[i][j] = Math.round(b.getFloat(i, j) * -1);
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

        int[][] fixedPointOutput = AcceleratorSimInterface.runSimOneInst(fixedPointA, fixedPointB, instruction);

        float[] output = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                output[i * cols + j] = (float) (fixedPointOutput[i][j]);
            }
        }

        return Nd4j.create(output).reshape(rows, cols);
    }
}

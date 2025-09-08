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
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ExpV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

/**
 * Implements the Exp operation using a hardware accelerator,
 * with support for both 2D and 3D (batched) tensors.
 * This version automatically pads inputs to be multiples of 32 for hardware compatibility.
 */
public class HWAcceleratedExpV13 extends HWAcceleratedOperator implements ExpV13 {

    private final int HW_DIM_MULTIPLE = 32;

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        ExpInputsV13<INDArray> castedInputs = new ExpInputsV13<>(node, inputs);
        INDArray inputTensor = castedInputs.getInput();
        INDArray outputTensor = this.exp(inputTensor);
        return new ExpOutputV13<>(outputTensor);
    }

    /**
     * Public dispatcher for the Exp operation. It checks the tensor rank
     * and calls the appropriate implementation.
     *
     * @param x The input tensor.
     * @return The result of the Exp operation.
     */
    public INDArray exp(INDArray x) {
        if (x.rank() == 2) {
            return exp2D(x);
        } else if (x.rank() == 3) {
            return exp3D(x);
        } else {
            throw new IllegalArgumentException("Unsupported tensor rank for Exp: " + x.rank());
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
     * Performs 2D Exp operation with padding and slicing.
     *
     * @param x The 2D input tensor.
     * @return The 2D result tensor.
     */
    private INDArray exp2D(INDArray x) {
        long[] shape = x.shape();
        int originalRows = (int) shape[0];
        int originalCols = (int) shape[1];

        // 1. Calculate padded dimensions
        int paddedRows = ceilToMultiple(originalRows, HW_DIM_MULTIPLE);
        int paddedCols = ceilToMultiple(originalCols, HW_DIM_MULTIPLE);

        // 2. Create a padded INDArray
        INDArray paddedX = Nd4j.zeros(paddedRows, paddedCols);
        paddedX.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols)}, x);

        // 3. Call the hardware accelerator with the padded data
        INDArray paddedResult = expOnAccelerator(paddedX, paddedRows, paddedCols);

        // 4. Slice the result back to the original output shape
        return paddedResult.get(NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols));
    }

    /**
     * Performs 3D (batched) Exp operation.
     *
     * @param x The 3D input tensor.
     * @return The 3D result tensor.
     */
    private INDArray exp3D(INDArray x) {
        long[] shape = x.shape();
        long batch = shape[0];
        long rows = shape[1];
        long cols = shape[2];

        INDArray result = Nd4j.createUninitialized(shape, 'c');

        for (int i = 0; i < (int) batch; i++) {
            INDArray slice = x.slice(i);
            INDArray expSlice = exp2D(slice); // exp2D now handles padding
            result.putSlice(i, expSlice);
        }

        return result;
    }

    /**
     * Private helper to run the Exp operation on the hardware simulator.
     * This method's logic is preserved exactly as requested.
     */
    private INDArray expOnAccelerator(INDArray x, int rows, int cols) {
        int fracWidth = AcceleratorSimInterface.acceleratorCfg().fracWidth();
        int[][] fixedPointInput = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                fixedPointInput[i][j] = Math.round(x.getFloat(i, j));
            }
        }

        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "elementadd",
                -fracWidth,
                false,
                "exp",
                fracWidth,
                0,
                0,
                0,
                rows,
                cols,
                cols
        );

        int[][] matrixB_zero = new int[rows][cols];
        int[][] fixedPointOutput = AcceleratorSimInterface.runRefOneInst(fixedPointInput, matrixB_zero, instruction);

        float[] output = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                output[i * cols + j] = (float) (fixedPointOutput[i][j]);
            }
        }

        return Nd4j.create(output).reshape(rows, cols);
    }
}

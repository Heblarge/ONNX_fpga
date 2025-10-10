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
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ReluV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import java.util.List;

/**
 * Implements the Relu operation using a hardware accelerator,
 * with support for both 2D and 3D (batched) tensors.
 * This version automatically pads inputs to be multiples of 32 for hardware compatibility.
 */
public class HWAcceleratedReluV13 extends HWAcceleratedOperator implements ReluV13 {

    private final int HW_DIM_MULTIPLE = 32;

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        ReluInputsV13<INDArray> castedInputs = new ReluInputsV13<>(node, inputs);
        INDArray inputTensor = castedInputs.getInput();
        List<Float> fpgaInScales = castedInputs.getFpgaInScales();
        List<Long> fpgaInShift = castedInputs.getFpgaInShift();
        List<Float> fpgaOutScale = castedInputs.getFpgaOutScale();
        List<Long> fpgaOutShift = castedInputs.getFpgaOutShift();
        INDArray outputTensor = this.relu(inputTensor, fpgaInShift, fpgaOutShift);
        return new ReluOutputV13<>(outputTensor);
    }

    /**
     * Public dispatcher for the Relu operation. It checks the tensor rank
     * and calls the appropriate implementation.
     *
     * @param x The input tensor.
     * @return The result of the Relu operation.
     */
    public INDArray relu(INDArray x, List<Long> fpgaInShift, List<Long> fpgaOutShift) {
        if (x.rank() == 2) {
            return relu2D(x, fpgaInShift, fpgaOutShift);
        } else if (x.rank() == 3) {
            return relu3D(x, fpgaInShift, fpgaOutShift);
        } else {
            throw new IllegalArgumentException("Unsupported tensor rank for Relu: " + x.rank());
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
     * Performs 2D Relu operation with padding and slicing.
     *
     * @param x The 2D input tensor.
     * @return The 2D result tensor.
     */
    private INDArray relu2D(INDArray x, List<Long> fpgaInShift, List<Long> fpgaOutShift) {
        long[] shape = x.shape();
        int originalRows = (int) shape[0];
        int originalCols = (int) shape[1];

        int paddedRows = ceilToMultiple(originalRows, HW_DIM_MULTIPLE);
        int paddedCols = ceilToMultiple(originalCols, HW_DIM_MULTIPLE);

        INDArray paddedX = Nd4j.zeros(x.dataType(), paddedRows, paddedCols);
        paddedX.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols)}, x);

        INDArray paddedResult = reluOnAccelerator(paddedX, paddedRows, paddedCols, fpgaInShift, fpgaOutShift);

        return paddedResult.get(NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols));
    }

    /**
     * Performs 3D (batched) Relu operation.
     *
     * @param x The 3D input tensor.
     * @return The 3D result tensor.
     */
    private INDArray relu3D(INDArray x, List<Long> fpgaInShift, List<Long> fpgaOutShift) {
        long[] shape = x.shape();
        long batch = shape[0];
        long rows = shape[1];
        long cols = shape[2];

        INDArray result = Nd4j.createUninitialized(x.dataType(), shape, 'c');

        for (int i = 0; i < (int) batch; i++) {
            INDArray slice = x.slice(i);
            INDArray reluSlice = relu2D(slice, fpgaInShift, fpgaOutShift);
            result.putSlice(i, reluSlice);
        }

        return result;
    }

    /**
     * Private helper to run the Relu operation on the hardware simulator.
     * This method's logic is preserved exactly as requested.
     */
    private INDArray reluOnAccelerator(INDArray x, int rows, int cols, List<Long> fpgaInShift, List<Long> fpgaOutShift) {

        long s_in = fpgaInShift.get(0);
        long s_hw = AcceleratorSimInterface.acceleratorCfg().fracWidth();
        long s_out = fpgaOutShift.get(0);

        long[][] fixedPointInput = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                fixedPointInput[i][j] = x.getLong(i, j);
            }
        }
        int preShiftAmount = (int) (s_in - s_hw);
        int postShiftAmount = (int) (s_hw - s_out);
        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "elementadd",
                preShiftAmount,
                false,
                "relu",
                postShiftAmount,
                0,
                0,
                0,
                rows,
                cols,
                cols
        );

        long[][] matrixB_zero = new long[rows][cols];
        long[][] hardwareResult = AcceleratorSimInterface.runRefOneInst(fixedPointInput, matrixB_zero, instruction);

        long[] output = new long[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                output[i * cols + j] = hardwareResult[i][j];
            }
        }

        INDArray fianlOutput =  Nd4j.create(output, new long[]{rows, cols}, x.dataType());

        return fianlOutput;
    }
}

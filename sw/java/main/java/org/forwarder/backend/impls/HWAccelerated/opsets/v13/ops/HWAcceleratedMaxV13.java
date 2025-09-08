package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.MaxV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import java.util.List;

public class HWAcceleratedMaxV13 extends HWAcceleratedOperator implements MaxV13 {

    private final int HW_DIM_MULTIPLE = 32;

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        MaxInputsV13<INDArray> castedInputs = new MaxInputsV13<>(node, inputs);
        List<INDArray> inputTensors = castedInputs.getInputTensors();
        INDArray outputTensor = this.max(inputTensors);
        return new MaxOutputV13<>(outputTensor);
    }

    public INDArray max(List<INDArray> inputTensors) {
        if (inputTensors == null || inputTensors.isEmpty()) {
            throw new IllegalArgumentException("Max operator requires at least one input tensor.");
        }

        INDArray currentMax = inputTensors.get(0);
        for (int i = 1; i < inputTensors.size(); i++) {
            currentMax = elementwiseMax(currentMax, inputTensors.get(i));
        }
        return currentMax;
    }

    private INDArray elementwiseMax(INDArray a, INDArray b) {
        if (a.rank() == 2 && b.rank() == 2) {
            return max2D(a, b);
        } else if (a.rank() == 3 && b.rank() == 3) {
            return max3D(a, b);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported or mismatched tensor ranks for Max: A=" + a.rank() + ", B=" + b.rank()
            );
        }
    }

    private int ceilToMultiple(int value, int multiple) {
        if (multiple == 0) return value;
        return ((value + multiple - 1) / multiple) * multiple;
    }

    private INDArray max2D(INDArray a, INDArray b) {
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
        INDArray paddedResult = maxOnAccelerator(paddedA, paddedB, paddedRows, paddedCols);

        // 4. Slice the result back to the original output shape
        return paddedResult.get(NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols));
    }


    private INDArray max3D(INDArray a, INDArray b) {
        if (!java.util.Arrays.equals(a.shape(), b.shape())) {
            throw new IllegalArgumentException("Input shapes must be identical for 3D hardware acceleration.");
        }
        long batch = a.shape()[0];
        long rows = a.shape()[1];
        long cols = a.shape()[2];
        INDArray result = Nd4j.createUninitialized(new long[]{batch, rows, cols}, 'c');

        for (int i = 0; i < (int) batch; i++) {
            INDArray sliceA = a.slice(i);
            INDArray sliceB = b.slice(i);
            INDArray maxSlice = max2D(sliceA, sliceB);
            result.putSlice(i, maxSlice);
        }
        return result;
    }


    private INDArray maxOnAccelerator(INDArray a, INDArray b, int rows, int cols) {
        int[][] fixedPointA = new int[rows][cols];
        int[][] fixedPointB = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                fixedPointA[i][j] = Math.round(a.getFloat(i, j));
                fixedPointB[i][j] = Math.round(b.getFloat(i, j));
            }
        }

        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "elementmax",
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
                output[i * cols + j] = (float) (fixedPointOutput[i][j]);
            }
        }

        return Nd4j.create(output).reshape(rows, cols);
    }
}

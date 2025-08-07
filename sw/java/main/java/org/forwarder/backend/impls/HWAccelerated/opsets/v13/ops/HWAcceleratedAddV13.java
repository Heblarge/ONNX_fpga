package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.AddV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class HWAcceleratedAddV13 extends HWAcceleratedOperator implements AddV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        AddInputsV13<INDArray> castedInputs = new AddInputsV13<>(node, inputs);
        INDArray matrixA = castedInputs.getA();
        INDArray matrixB = castedInputs.getB();
        INDArray outputTensor = this.add(matrixA, matrixB);
        return new AddOutputV13<>(outputTensor);
    }

    public INDArray add(INDArray a, INDArray b) {
        if (a.rank() == 2 && b.rank() == 2) {
            return add2D(a, b);
        } else if (a.rank() == 3 || b.rank() == 3) {
            return add3D(a, b);
        } else {
            throw new IllegalArgumentException("Unsupported tensor rank for Add: A=" + a.rank() + ", B=" + b.rank());
        }
    }

    private INDArray add2D(INDArray a, INDArray b) {
        long[] shapeA = a.shape();
        long[] shapeB = b.shape();

        if (!java.util.Arrays.equals(shapeA, shapeB)) {
            throw new IllegalArgumentException("Unsupported operation: Input shapes must be identical for 3D hardware addition.");
        }

        int rows = (int) shapeA[0];
        int cols = (int) shapeA[1];

        return addOnAccelerator(a, b, rows, cols);
    }

    private INDArray add3D(INDArray a, INDArray b) {
        long[] shapeA = a.shape();
        long[] shapeB = b.shape();

        if (!java.util.Arrays.equals(shapeA, shapeB)) {
            throw new IllegalArgumentException("Unsupported operation: Input shapes must be identical for 3D hardware addition.");
        }
        long batch = shapeA[0];
        long rows = shapeA[1];
        long cols = shapeA[2];

        INDArray result = Nd4j.createUninitialized(new long[]{batch, rows, cols}, 'c');

        for (int i = 0; i < (int) batch; i++) {
            INDArray sliceA = a.slice(i);
            INDArray sliceB = b.slice(i);

            INDArray sum = addOnAccelerator(sliceA, sliceB, (int)rows, (int)cols);

            result.putSlice(i, sum);
        }

        return result;
    }


    private INDArray addOnAccelerator(INDArray a, INDArray b, int rows, int cols) {
        // Convert float matrices to fixed-point for the accelerator
        int[][] fixedPointA = new int[rows][cols];
        int[][] fixedPointB = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                fixedPointA[i][j] = (int) (a.getFloat(i, j));
                fixedPointB[i][j] = (int) (b.getFloat(i, j));
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

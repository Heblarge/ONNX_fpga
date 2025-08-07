package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.MatMulV13;
import org.onnx4j.opsets.operator.OperatorOutputs;


public class HWAcceleratedMatMulV13 extends HWAcceleratedOperator implements MatMulV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        MatMulInputsV1<INDArray> castedInputs = new MatMulInputsV1<>(node, inputs);
        INDArray matrixA = castedInputs.getA();
        INDArray matrixB = castedInputs.getB();
        INDArray outputTensor = this.matmul(matrixA, matrixB);
        return new MatMulOutputV1<>(outputTensor);
    }

    public INDArray matmul(INDArray a, INDArray b) {
        if (a.rank() == 2 && b.rank() == 2) {
            return matmul2D(a, b);
        } else if (a.rank() == 3 || b.rank() == 3) {
            return matmul3D(a, b);
        } else {
            throw new IllegalArgumentException("Unsupported tensor rank for MatMul: A=" + a.rank() + ", B=" + b.rank());
        }
    }

    private INDArray matmul2D(INDArray a, INDArray b) {
        long[] shapeA = a.shape();
        long[] shapeB = b.shape();

        int rowsA = (int) shapeA[0]; // m
        int colsA = (int) shapeA[1]; // k
        int colsB = (int) shapeB[1]; // n

        if (colsA != shapeB[0]) {
            throw new IllegalArgumentException(
                    String.format("Matrix shape mismatch for 2D MatMul: A's columns (%d) must equal B's rows (%d).", colsA, shapeB[0])
            );
        }

        return matMulOnAccelerator(a, b, rowsA, colsA, colsB);
    }

    private INDArray matmul3D(INDArray a, INDArray b) {
        // Handle broadcasting for mixed 2D/3D inputs
        INDArray inputA = a.rank() == 2 ? a.reshape(1, a.rows(), a.columns()) : a;
        INDArray inputB = b.rank() == 2 ? b.reshape(1, b.rows(), b.columns()) : b;

        long batchA = inputA.shape()[0];
        long batchB = inputB.shape()[0];

        if (batchA != batchB && batchA != 1 && batchB != 1) {
            throw new IllegalArgumentException(
                    String.format("Batch dimensions must be compatible for 3D MatMul: A=%d, B=%d", batchA, batchB)
            );
        }

        long batch = Math.max(batchA, batchB);
        long m = inputA.size(1);
        long k_a = inputA.size(2);
        long k_b = inputB.size(1);
        long n = inputB.size(2);

        if (k_a != k_b) {
            throw new IllegalArgumentException(
                    String.format("Matrix shape mismatch for 3D MatMul: A's columns (%d) must equal B's rows (%d).", k_a, k_b)
            );
        }

        INDArray result = Nd4j.createUninitialized(new long[]{batch, m, n}, 'c'); //

        for (int i = 0; i < (int) batch; i++) {
            INDArray sliceA = (batchA == 1) ? inputA.slice(0) : inputA.slice(i);
            INDArray sliceB = (batchB == 1) ? inputB.slice(0) : inputB.slice(i);
            INDArray product = matmul2D(sliceA, sliceB);
            result.putSlice(i, product);
        }

        return result;
    }


    private INDArray matMulOnAccelerator(INDArray a, INDArray b, int rowsA, int colsA, int colsB){

        int[][] fixedPointA = new int[rowsA][colsA];
        int[][] fixedPointB = new int[colsA][colsB];
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsA; j++) {
                // 将浮点数转换为定点数
                fixedPointA[i][j] = (int) (a.getFloat(i, j));

            }
        }
        for (int i = 0; i < colsA; i++) {
            for (int j = 0; j < colsB; j++) {
                // 将浮点数转换为定点数
                fixedPointB[i][j] = (int) (b.getFloat(i, j));
            }
        }

        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "matmul",
                0,
                false,
                "none",
                0,
                0,
                0,
                0,
                rowsA,
                colsA,
                colsB
        );
        int[][] fixedPointOutput = AcceleratorSimInterface.runSimOneInst(fixedPointA, fixedPointB, instruction);

        float[] Output = new float[rowsA * colsB];
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                // 将定点数转换回浮点数
                Output[i * colsB + j] = (float) (fixedPointOutput[i][j]);
            }
        }

        return Nd4j.create(Output).reshape(rowsA, colsB);
    }

}

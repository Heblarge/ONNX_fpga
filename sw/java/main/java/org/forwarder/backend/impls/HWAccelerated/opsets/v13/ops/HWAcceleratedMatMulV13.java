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
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.MatMulV13;
import org.onnx4j.opsets.operator.OperatorOutputs;


public class HWAcceleratedMatMulV13 extends HWAcceleratedOperator implements MatMulV13 {

    private final int HW_DIM_MULTIPLE = 32;

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

    private int ceilToMultiple(int value, int multiple) {
        if (multiple == 0) return value;
        return ((value + multiple - 1) / multiple) * multiple;
    }

    private INDArray matmul2D(INDArray a, INDArray b) {
        long[] shapeA = a.shape();
        long[] shapeB = b.shape();

        int originalRowsA = (int) shapeA[0];
        int originalColsA = (int) shapeA[1];
        int originalRowsB = (int) shapeB[0];
        int originalColsB = (int) shapeB[1];

        if (originalColsA != originalRowsB) {
            throw new IllegalArgumentException(
                    String.format("Matrix shape mismatch for 2D MatMul: A's columns (%d) must equal B's rows (%d).", originalColsA, originalRowsB)
            );
        }

        // 1. 计算填充后的尺寸 (向上取整到32的倍数)
        int paddedRowsA = ceilToMultiple(originalRowsA, HW_DIM_MULTIPLE);
        int paddedColsA = ceilToMultiple(originalColsA, HW_DIM_MULTIPLE);
        int paddedColsB = ceilToMultiple(originalColsB, HW_DIM_MULTIPLE);

        // 2. 创建一个全零的、更大尺寸的矩阵，用于填充
        INDArray paddedA = Nd4j.zeros(paddedRowsA, paddedColsA);
        // 将原始矩阵的数据复制到新矩阵的左上角
        paddedA.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRowsA), NDArrayIndex.interval(0, originalColsA)}, a);

        INDArray paddedB = Nd4j.zeros(paddedColsA, paddedColsB);
        paddedB.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRowsB), NDArrayIndex.interval(0, originalColsB)}, b);

        // 3. 将填充后的矩阵送入硬件进行计算
        INDArray paddedResult = matMulOnAccelerator(paddedA, paddedB, paddedRowsA, paddedColsA, paddedColsB);

        // 4. 从硬件返回的结果中，切片出我们需要的原始尺寸部分
        return paddedResult.get(NDArrayIndex.interval(0, originalRowsA), NDArrayIndex.interval(0, originalColsB));
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

        int fracWidth = 8; // Should be determined from onnx graph for optimal value
        double scaleFactor = Math.pow(2, fracWidth);

        int[][] fixedPointA = new int[rowsA][colsA];
        int[][] fixedPointB = new int[colsA][colsB];
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsA; j++) {
                // 将浮点数转换为定点数
                fixedPointA[i][j] = (int)Math.round(a.getFloat(i, j) * scaleFactor);

            }
        }
        for (int i = 0; i < colsA; i++) {
            for (int j = 0; j < colsB; j++) {
                // 将浮点数转换为定点数
                fixedPointB[i][j] = (int)Math.round(b.getFloat(i, j) * scaleFactor);
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
        int[][] fixedPointOutput = AcceleratorSimInterface.runRefOneInst(fixedPointA, fixedPointB, instruction);

        float[] Output = new float[rowsA * colsB];
        double finalScaleFactor = scaleFactor * scaleFactor;
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                // 将定点数转换回浮点数
                Output[i * colsB + j] = (float)(((double)(fixedPointOutput[i][j])) / finalScaleFactor);
            }
        }

        return Nd4j.create(Output).reshape(rowsA, colsB);
    }

}

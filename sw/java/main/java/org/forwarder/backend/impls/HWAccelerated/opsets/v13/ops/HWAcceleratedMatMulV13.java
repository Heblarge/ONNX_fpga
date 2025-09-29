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
import scala.tools.nsc.doc.html.HtmlTags;


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
        } else if (a.rank() == 3 && b.rank() == 2) {
            return matmul3D2D(a, b);
        } else if (a.rank() == 3 && b.rank() == 3) {
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

    private INDArray matmul3D2D(INDArray a, INDArray b) {
        long batchSize = a.size(0);
        long M = a.size(1);
        long K = a.size(2);
        long N = b.size(1);

        INDArray a2D = a.reshape('c', batchSize * M, K);
        INDArray result2D = matmul2D(a2D, b);

        long[] outputShape = {batchSize, M, N};
        return result2D.reshape('c', outputShape);
    }

    private INDArray matmul3D(INDArray a, INDArray b) {
        long batchA = a.size(0);
        long batchB = b.size(0);
        long batch = Math.max(batchA, batchB);
        long m = a.size(1);
        long n = b.size(2);

        INDArray result = Nd4j.createUninitialized(new long[]{batch, m, n}, 'c');

        for (int i = 0; i < (int) batch; i++) {
            INDArray sliceA = (batchA == 1) ? a.slice(0) : a.slice(i);
            INDArray sliceB = (batchB == 1) ? b.slice(0) : b.slice(i);
            INDArray product = matmul2D(sliceA, sliceB);
            result.putSlice(i, product);
        }
        return result;
    }


    private INDArray matMulOnAccelerator(INDArray a, INDArray b, int rowsA, int colsA, int colsB){
        // 获取硬件参数
        int elementWidth = AcceleratorSimInterface.acceleratorCfg().elementWidth();
        int intWidth = AcceleratorSimInterface.acceleratorCfg().intWidth();
        long ELEMENT_INT_MAX = (1L << (elementWidth - 1)) - 1;
        long ELEMENT_INT_MIN = -(1L << (elementWidth - 1));
        double REAL_VALUE_MAX_RANGE = Math.pow(2, intWidth - 1);

        // 使用固定的 fracWidth
        int fracWidth = 10;
        int fracWidth2 = 22;
        double scaleFactor = Math.pow(2, fracWidth);

        // 输入转换溢出检查 (检查放大后的整数是否超出 elementWidth)
        int[][] fixedPointA = new int[rowsA][colsA];
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsA; j++) {
                double scaledValue = a.getFloat(i, j) * scaleFactor;
                if (scaledValue > ELEMENT_INT_MAX || scaledValue < ELEMENT_INT_MIN) {
                    throw new ArithmeticException("Input Overflow during scaling! Value exceeds " + elementWidth + "-bit range.");
                }
                fixedPointA[i][j] = (int)Math.round(scaledValue);
            }
        }

        int[][] fixedPointB = new int[colsA][colsB];
        for (int i = 0; i < colsA; i++) {
            for (int j = 0; j < colsB; j++) {
                double scaledValue = b.getFloat(i, j) * scaleFactor;
                if (scaledValue > ELEMENT_INT_MAX || scaledValue < ELEMENT_INT_MIN) {
                    throw new ArithmeticException("Input Overflow during scaling! Value exceeds " + elementWidth + "-bit range.");
                }
                fixedPointB[i][j] = (int)Math.round(scaledValue);
            }
        }

        // 调用硬件仿真
        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "matmul",
                fracWidth * 2,
                false,
                "none",
                0,
                0,
                0,
                0,
                rowsA,
                colsA,
                colsB);
        int[][] hardwareResult = AcceleratorSimInterface.runRefOneInst(fixedPointA, fixedPointB, instruction);

        // 将累加结果转换回浮点数
        float[] output = new float[rowsA * colsB];
        double finalScaleFactor = 14;
        double scaleFactor2 = Math.pow(2, fracWidth2);
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                output[i * colsB + j] = (float) (hardwareResult[i][j]);
            }
        }



        return Nd4j.create(output).reshape(rowsA, colsB);
    }

}

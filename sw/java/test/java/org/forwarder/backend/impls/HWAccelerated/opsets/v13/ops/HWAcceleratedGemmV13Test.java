package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test class provides comprehensive validation for the HWAcceleratedGemmV13 operator,
 * using a precise fixed-point simulation for accurate result verification.
 */
public class HWAcceleratedGemmV13Test extends HWAcceleratedTestCase {

    // Using smaller, more reasonable dimensions for unit testing
    private final int rowsA = 32;
    private final int colsA = 32;
    private final int colsB = 1024;
    private final float minValue = -5f;
    private final float maxValue = 5f;

    /**
     * Simulates the exact fixed-point arithmetic of the core MatMul hardware operator.
     */
    private INDArray calculateSimulatedFixedPointMatMul(INDArray a, INDArray b) {
        int fracWidth = 9; // Must match the value in the operator
        double scaleFactor = Math.pow(2, fracWidth);
        int m = (int) a.size(0);
        int k = (int) a.size(1);
        int n = (int) b.size(1);

        int[][] fixedPointA = new int[m][k];
        int[][] fixedPointB = new int[k][n];
        for (int i = 0; i < m; i++) for (int j = 0; j < k; j++) fixedPointA[i][j] = (int)Math.round(a.getFloat(i, j) * scaleFactor);
        for (int i = 0; i < k; i++) for (int j = 0; j < n; j++) fixedPointB[i][j] = (int)Math.round(b.getFloat(i, j) * scaleFactor);

        int[][] fixedPointOutput = new int[m][n];
        long[][] fixedPointOutputLong = new long[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                for (int l = 0; l < k; l++) {
                    // 使用 long 进行乘法以避免 int 溢出
                    fixedPointOutputLong[i][j] += (long)fixedPointA[i][l] * fixedPointB[l][j];
                }
            }
        }

        float[] output = new float[m * n];
        double finalScaleFactor = scaleFactor * scaleFactor;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                // 从 long 类型的累加器中读取结果
                output[i * n + j] = (float) (fixedPointOutputLong[i][j] / finalScaleFactor);
            }
        }
        return Nd4j.create(output).reshape(m, n);
    }

    private INDArray calculateSimulatedFixedPointAdd(INDArray a, INDArray b) {
        int fracWidth = 9; // Must match the value in the Add operator
        double scaleFactor = Math.pow(2, fracWidth);
        int rows = (int) a.rows();
        int cols = (int) a.columns();

        int[][] fixedPointA = new int[rows][cols];
        int[][] fixedPointB = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                fixedPointA[i][j] = (int)Math.round(a.getFloat(i, j) * scaleFactor);
                fixedPointB[i][j] = (int)Math.round(b.getFloat(i, j) * scaleFactor);
            }
        }

        int[][] fixedPointOutput = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                fixedPointOutput[i][j] = fixedPointA[i][j] + fixedPointB[i][j];
            }
        }

        float[] output = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                output[i * cols + j] = (float)(((double)(fixedPointOutput[i][j])) / scaleFactor);
            }
        }
        return Nd4j.create(output).reshape(rows, cols);
    }

    /**
     * Simulates the entire Gemm operation, now including the fixed-point Add simulation for C.
     */
    private INDArray calculateSimulatedFixedPointGemm(INDArray A, INDArray B, INDArray C, float alpha, float beta, long transA, long transB) {
        if (transA != 0L) { A = A.transpose(); }
        if (transB != 0L) { B = B.transpose(); }

        INDArray Y = calculateSimulatedFixedPointMatMul(A, B);
        Y.muli(alpha);

        if (C != null) {
            long[] yShape = Y.shape();
            if (!java.util.Arrays.equals(C.shape(), yShape)) {
                C = C.broadcast(yShape);
            }
            INDArray betaC = C.mul(beta);

            // MODIFIED: Use the fixed-point add simulation
            Y = calculateSimulatedFixedPointAdd(Y, betaC);
        }
        return Y;
    }


    /**
     * Simulates the entire Gemm operation (alpha * A'B' + beta * C) using the fixed-point MatMul simulation.
     */

    @Test
    public void testGemmWithAllParameters() throws Exception {
        System.out.println("\n--- Testing Gemm with all parameters (Y = alpha * A'* B' + beta * C) ---");
        float alpha = 0.5f;
        float beta = 2.0f;
        INDArray matrixA = Nd4j.create( HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsA, rowsA, minValue, maxValue));
        INDArray matrixB = Nd4j.create( HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsB, colsA, minValue, maxValue));
        INDArray matrixC = Nd4j.create( HWAcceleratedTestModel.generateRandom2DFloatMatrix(rowsA, colsB, minValue, maxValue));

        INDArray theoreticalExpected = matrixA.transpose().mmul(matrixB.transpose()).mul(alpha).add(matrixC.mul(beta));
        INDArray simulatedExpected = calculateSimulatedFixedPointGemm(matrixA, matrixB, matrixC, alpha, beta, 1L, 1L);

        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
        INDArray actualOutput = operator.gemm(matrixA, matrixB, matrixC, alpha, beta, 1L, 1L);

        double hardwareLogicTolerance = 1e-3;
        HWAcceleratedTestModel.validate("Gemm", theoreticalExpected, simulatedExpected, actualOutput, hardwareLogicTolerance);
    }


}
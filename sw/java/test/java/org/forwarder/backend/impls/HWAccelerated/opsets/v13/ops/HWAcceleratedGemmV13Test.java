package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
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
    private final int rowsA = 64;
    private final int colsA = 33;
    private final int colsB = 60;
    private final float minValue = -3.0f;
    private final float maxValue = 3.0f;

    /**
     * Simulates the exact fixed-point arithmetic of the core MatMul hardware operator.
     */
    private INDArray calculateSimulatedFixedPointMatMul(INDArray a, INDArray b) {
        int fracWidth = 8; // Must match the value in the operator
        double scaleFactor = Math.pow(2, fracWidth);
        int m = (int) a.size(0);
        int k = (int) a.size(1);
        int n = (int) b.size(1);

        int[][] fixedPointA = new int[m][k];
        int[][] fixedPointB = new int[k][n];
        for (int i = 0; i < m; i++) for (int j = 0; j < k; j++) fixedPointA[i][j] = (int)Math.round(a.getFloat(i, j) * scaleFactor);
        for (int i = 0; i < k; i++) for (int j = 0; j < n; j++) fixedPointB[i][j] = (int)Math.round(b.getFloat(i, j) * scaleFactor);

        int[][] fixedPointOutput = new int[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                for (int l = 0; l < k; l++) {
                    fixedPointOutput[i][j] += fixedPointA[i][l] * fixedPointB[l][j];
                }
            }
        }

        float[] output = new float[m * n];
        double finalScaleFactor = scaleFactor * scaleFactor;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                output[i * n + j] = (float) (fixedPointOutput[i][j] / finalScaleFactor);
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
    public void testMatMul() throws Exception {
        System.out.println("\n--- Testing MatMul (Y = A * B) ---");
        INDArray matrixA = Nd4j.create(generateRandomFloatMatrix(rowsA, colsA, minValue, maxValue));
        INDArray matrixB = Nd4j.create(generateRandomFloatMatrix(colsA, colsB, minValue, maxValue));

        INDArray theoreticalExpected = matrixA.mmul(matrixB);
        INDArray simulatedExpected = calculateSimulatedFixedPointGemm(matrixA, matrixB, null, 1.0f, 0.0f, 0L, 0L);

        validateGemm(theoreticalExpected, simulatedExpected, matrixA, matrixB, null, 1.0f, 0.0f, 0L, 0L);
    }

    @Test
    public void testGemmWithAllParameters() throws Exception {
        System.out.println("\n--- Testing Gemm with all parameters (Y = alpha*A'*B' + beta*C) ---");
        float alpha = 0.5f;
        float beta = 2.0f;
        INDArray matrixA = Nd4j.create(generateRandomFloatMatrix(colsA, rowsA, minValue, maxValue)); // Shape for transpose
        INDArray matrixB = Nd4j.create(generateRandomFloatMatrix(colsB, colsA, minValue, maxValue)); // Shape for transpose
        INDArray matrixC = Nd4j.create(generateRandomFloatMatrix(rowsA, colsB, minValue, maxValue));

        INDArray theoreticalExpected = matrixA.transpose().mmul(matrixB.transpose()).mul(alpha).add(matrixC.mul(beta));
        INDArray simulatedExpected = calculateSimulatedFixedPointGemm(matrixA, matrixB, matrixC, alpha, beta, 1L, 1L);

        validateGemm(theoreticalExpected, simulatedExpected, matrixA, matrixB, matrixC, alpha, beta, 1L, 1L);
    }

    private void validateGemm(INDArray theoreticalExpected, INDArray simulatedExpected, INDArray inputA, INDArray inputB, INDArray inputC, float alpha, float beta, long transA, long transB) throws Exception {
        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
        INDArray actualOutput = operator.gemm(inputA, inputB, inputC, alpha, beta, transA, transB);

        assertArrayEquals("The output shape must match the expected shape.", simulatedExpected.shape(), actualOutput.shape());

        float[] theoreticalVector = theoreticalExpected.dup('c').data().asFloat();
        float[] simulatedVector = simulatedExpected.dup('c').data().asFloat();
        float[] actualVector = actualOutput.dup('c').data().asFloat();

        String horizontalLine = new String(new char[154]).replace('\0', '-');
        String headerFormat = "%-8s | %-10s | %-12s | %-9s | %-15s | %-5s | %-15s | %-5s | %-7s%n";
        String dataFormat   = "%-8d | %-15.6f | %-15.6f | %-14f | %-15.6f | %-12s | %-15.6f | %-12s | %-7s%n";

        System.out.println("\n\n" + horizontalLine);
        System.out.printf(headerFormat, "Index", "理论值", "模拟值", "实际值", "总误差(Abs)", "总误差(Rel)", "逻辑误差(Abs)", "逻辑误差(Rel)", "Check");
        System.out.println(horizontalLine);

        int errorCount = 0;
        double hardwareLogicTolerance = 1e-3; // Tolerance is slightly higher for MatMul due to accumulated errors

        for (int i = 0; i < actualVector.length; i++) {
            double theoreticalVal = theoreticalVector[i];
            double simulatedVal = simulatedVector[i];
            double actualVal = actualVector[i];

            double totalAbsError = Math.abs(actualVal - theoreticalVal);
            double totalRelError = (Math.abs(theoreticalVal) > 1e-9) ? (totalAbsError / Math.abs(theoreticalVal)) : 0.0;
            double logicAbsError = Math.abs(actualVal - simulatedVal);
            double logicRelError = (Math.abs(simulatedVal) > 1e-9) ? (logicAbsError / Math.abs(simulatedVal)) : 0.0;
            boolean pass = logicAbsError <= hardwareLogicTolerance;

            if (!pass) {
                errorCount++;
            }

            String totalRelErrorStr = String.format("%.2f%%", totalRelError * 100);
            String logicRelErrorStr = String.format("%.2f%%", logicRelError * 100);
            System.out.printf(dataFormat, i, theoreticalVal, simulatedVal, actualVal, totalAbsError, totalRelErrorStr, logicAbsError, logicRelErrorStr, pass ? "Pass" : "Fail");

        }
        System.out.println(horizontalLine);

        assertTrue("硬件实际值与模拟值不符，逻辑错误! " + errorCount + " errors found.", errorCount == 0);
        System.out.println("\nCongratulations! This test case passed with precise fixed-point validation!");
    }

    private float[][] generateRandomFloatMatrix(int rows, int cols, float min, float max) {
        Random random = new Random();
        float[][] matrix = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = min + random.nextFloat() * (max - min);
            }
        }
        return matrix;
    }
}
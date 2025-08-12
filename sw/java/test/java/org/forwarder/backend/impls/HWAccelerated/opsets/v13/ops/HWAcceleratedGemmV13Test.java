package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test class provides comprehensive validation for the HWAcceleratedGemmV13 operator.
 * It includes multiple test cases to verify each parameter of the Gemm operation
 * (A, B, C, alpha, beta, transA, transB) and prints detailed matrix information.
 */
public class HWAcceleratedGemmV13Test extends HWAcceleratedTestCase {

    private final int rowsA = 8192;
    private final int colsA = 129;
    private final int colsB = 1024;
    private final int minValue = -10;
    private final int maxValue = 10;

    /**
     * Tests the basic MatMul case: Y = A * B.
     */
    @Test
    public void testMatMul() throws Exception {
        System.out.println("\n--- Testing MatMul (Y = A * B) ---");
        INDArray matrixA = Nd4j.create(generateRandomIntegerMatrix(rowsA, colsA, minValue, maxValue));
        INDArray matrixB = Nd4j.create(generateRandomIntegerMatrix(colsA, colsB, minValue, maxValue));

        INDArray expectedMatrix = matrixA.mmul(matrixB);

        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
        INDArray actualOutput = operator.gemm(matrixA, matrixB, null, 1.0f, 0.0f, 0L, 0L);

        printAndAssert(expectedMatrix, actualOutput, matrixA, matrixB, null);
    }

    /**
     * Tests Gemm with the C matrix: Y = A * B + C.
     */
    @Test
    public void testGemmWithC() throws Exception {
        System.out.println("\n--- Testing Gemm with C (Y = A * B + C) ---");
        INDArray matrixA = Nd4j.create(generateRandomIntegerMatrix(rowsA, colsA, minValue, maxValue));
        INDArray matrixB = Nd4j.create(generateRandomIntegerMatrix(colsA, colsB, minValue, maxValue));
        INDArray matrixC = Nd4j.create(generateRandomIntegerMatrix(rowsA, colsB, minValue, maxValue));

        INDArray expectedMatrix = matrixA.mmul(matrixB).add(matrixC);

        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
        INDArray actualOutput = operator.gemm(matrixA, matrixB, matrixC, 1.0f, 1.0f, 0L, 0L);

        printAndAssert(expectedMatrix, actualOutput, matrixA, matrixB, matrixC);
    }

    /**
     * Tests Gemm with alpha and beta scaling: Y = alpha * A * B + beta * C.
     */
    @Test
    public void testGemmWithAlphaBeta() throws Exception {
        System.out.println("\n--- Testing Gemm with alpha & beta (Y = alpha*A*B + beta*C) ---");
        float alpha = 2.5f;
        float beta = -1.5f;
        INDArray matrixA = Nd4j.create(generateRandomIntegerMatrix(rowsA, colsA, minValue, maxValue));
        INDArray matrixB = Nd4j.create(generateRandomIntegerMatrix(colsA, colsB, minValue, maxValue));
        INDArray matrixC = Nd4j.create(generateRandomIntegerMatrix(rowsA, colsB, minValue, maxValue));

        INDArray expectedMatrix = matrixA.mmul(matrixB).mul(alpha).add(matrixC.mul(beta));

        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
        INDArray actualOutput = operator.gemm(matrixA, matrixB, matrixC, alpha, beta, 0L, 0L);

        printAndAssert(expectedMatrix, actualOutput, matrixA, matrixB, matrixC);
    }

    /**
     * Tests Gemm with transposition of A: Y = A' * B.
     */
    @Test
    public void testGemmWithTransposeA() throws Exception {
        System.out.println("\n--- Testing Gemm with transpose A (Y = A'*B) ---");
        INDArray matrixA = Nd4j.create(generateRandomIntegerMatrix(colsA, rowsA, minValue, maxValue));
        INDArray matrixB = Nd4j.create(generateRandomIntegerMatrix(colsA, colsB, minValue, maxValue));

        INDArray expectedMatrix = matrixA.transpose().mmul(matrixB);

        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
        INDArray actualOutput = operator.gemm(matrixA, matrixB, null, 1.0f, 0.0f, 1L, 0L);

        printAndAssert(expectedMatrix, actualOutput, matrixA, matrixB, null);
    }

    /**
     * Tests Gemm with all parameters active: Y = alpha * A' * B' + beta * C.
     */
    @Test
    public void testGemmWithAllParameters() throws Exception {
        System.out.println("\n--- Testing Gemm with all parameters (Y = alpha*A'*B' + beta*C) ---");
        float alpha = 0.5f;
        float beta = 2.0f;
        INDArray matrixA = Nd4j.create(generateRandomIntegerMatrix(colsA, rowsA, minValue, maxValue));
        INDArray matrixB = Nd4j.create(generateRandomIntegerMatrix(colsB, colsA, minValue, maxValue));
        INDArray matrixC = Nd4j.create(generateRandomIntegerMatrix(rowsA, colsB, minValue, maxValue));

        INDArray expectedMatrix = matrixA.transpose().mmul(matrixB.transpose()).mul(alpha).add(matrixC.mul(beta));

        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
        INDArray actualOutput = operator.gemm(matrixA, matrixB, matrixC, alpha, beta, 1L, 1L);

        printAndAssert(expectedMatrix, actualOutput, matrixA, matrixB, matrixC);
    }

    /**
     * Helper method to print matrices, comparison details, and assert equality.
     */
    private void printAndAssert(INDArray expected, INDArray actualOutput, INDArray inputA, INDArray inputB, INDArray inputC) {
        System.out.println("\ninputA (" + java.util.Arrays.toString(inputA.shape()) + "):");
        System.out.print(inputA);
        System.out.println("\ninputB (" + java.util.Arrays.toString(inputB.shape()) + "):");
        System.out.print(inputB);
        if (inputC != null) {
            System.out.println("\ninputC (" + java.util.Arrays.toString(inputC.shape()) + "):");
            System.out.print(inputC);
        }
        System.out.println("\nexpectedMatrix (" + java.util.Arrays.toString(expected.shape()) + "):");
        System.out.print(expected);
        System.out.println("\nactualOutput (" + java.util.Arrays.toString(actualOutput.shape()) + "):");
        System.out.print(actualOutput);

        assertArrayEquals("The output shape should match the expected shape.", expected.shape(), actualOutput.shape());

        float[] expectedVector = expected.dup('c').data().asFloat();
        float[] actualVector = actualOutput.data().asFloat();

        int errorCount = 0;
        double relativeErrorTolerance = 0.02; // Allow 2% relative error

        System.out.println("\n" + new String(new char[110]).replace('\0', '-'));
        System.out.printf(
                "%-10s | %-20s | %-20s | %-20s | %-20s | %-7s%n",
                "index", "Expected", "Actual", "Abs Error", "Rel Error %", "check"
        );
        System.out.println(new String(new char[110]).replace('\0', '-'));

        for (int i = 0; i < expectedVector.length; i++) {
            double expectedVal = expectedVector[i];
            double actualVal = actualVector[i];
            double absoluteError = actualVal - expectedVal;
            double relativeError = (Math.abs(expectedVal) > 1e-6) ? (absoluteError / expectedVal) : 0.0;
            boolean pass = (Math.abs(relativeError) < relativeErrorTolerance) || (Math.abs(absoluteError) < 1e-3);

            System.out.printf(
                    "%-10d | %-20.6f | %-20.6f | %-20.6f | %-20.2f%% | %-7s%n",
                    i, expectedVal, actualVal, absoluteError, relativeError * 100, pass ? "Pass" : "Fail"
            );

            if (!pass) {
                errorCount++;
            }
        }
        System.out.println(new String(new char[110]).replace('\0', '-'));

        assertTrue(
                "The calculation result exceeds the allowable error range. " + errorCount + " errors found.",
                errorCount == 0
        );

        System.out.println("\nCongratulations! This test case passed!");
    }

    /**
     * Generates a matrix of floats with random integer values within a specified range.
     */
    private float[][] generateRandomIntegerMatrix(int rows, int cols, int min, int max) {
        Random random = new Random();
        float[][] matrix = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = random.nextInt(max - min + 1) + min;
            }
        }
        return matrix;
    }
}

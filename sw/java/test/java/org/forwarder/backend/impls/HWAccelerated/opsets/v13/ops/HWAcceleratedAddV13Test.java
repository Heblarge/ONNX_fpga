package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test class validates the HWAcceleratedAddV13 operator for
 * both 2D and 3D (batched) element-wise addition.
 */
public class HWAcceleratedAddV13Test extends HWAcceleratedTestCase {

    /**
     * Tests standard 2D element-wise addition.
     */
    @Test
    public void testAdd2D() throws Exception {
        System.out.println("\n--- Testing 2D Add ---");
        int rows = 4;
        int cols = 4;
        int minValue = -10;
        int maxValue = 10;

        INDArray matrixA = Nd4j.create(generateRandomIntegerMatrix(rows, cols, minValue, maxValue));
        INDArray matrixB = Nd4j.create(generateRandomIntegerMatrix(rows, cols, minValue, maxValue));

        INDArray expectedMatrix = matrixA.add(matrixB);

        this.validateAdd(expectedMatrix, matrixA, matrixB);
    }

    /**
     * Tests 3D (batched) element-wise addition.
     */
    @Test
    public void testAdd3D() throws Exception {
        System.out.println("\n--- Testing 3D (Batched) Add ---");
        int batchSize = 2;
        int rows = 4;
        int cols = 4;
        int minValue = -10;
        int maxValue = 10;

        INDArray matrixA = Nd4j.create(batchSize, rows, cols);
        INDArray matrixB = Nd4j.create(batchSize, rows, cols);
        for (int i = 0; i < batchSize; i++) {
            matrixA.putSlice(i, Nd4j.create(generateRandomIntegerMatrix(rows, cols, minValue, maxValue)));
            matrixB.putSlice(i, Nd4j.create(generateRandomIntegerMatrix(rows, cols, minValue, maxValue)));
        }

        INDArray expectedMatrix = matrixA.add(matrixB);

        this.validateAdd(expectedMatrix, matrixA, matrixB);
    }

    /**
     * Helper method to run the operator, print matrices, and assert correctness.
     */
    private void validateAdd(INDArray expected, INDArray inputA, INDArray inputB) throws Exception {
        HWAcceleratedAddV13 operator = new HWAcceleratedAddV13();
        INDArray actualOutput = operator.add(inputA, inputB);

        System.out.println("\ninputA shape: " + java.util.Arrays.toString(inputA.shape()));
        System.out.print(inputA);
        System.out.println("\n\ninputB shape: " + java.util.Arrays.toString(inputB.shape()));
        System.out.print(inputB);
        System.out.println("\n\nexpectedMatrix shape: " + java.util.Arrays.toString(expected.shape()));
        System.out.print(expected);
        System.out.println("\n\nactualOutput shape: " + java.util.Arrays.toString(actualOutput.shape()));
        System.out.print(actualOutput);

        assertArrayEquals("The output shape must match the expected shape.", expected.shape(), actualOutput.shape());

        float[] expectedVector = expected.dup('c').data().asFloat();
        float[] actualVector = actualOutput.dup('c').data().asFloat();

        int errorCount = 0;
        double relativeErrorTolerance = 0.02; // Allow 2% relative error

        System.out.println("\n\n" + new String(new char[110]).replace('\0', '-'));
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

            if (i < 100) { // Limit detailed output
                System.out.printf(
                        "%-10d | %-20.6f | %-20.6f | %-20.6f | %-20.2f%% | %-7s%n",
                        i, expectedVal, actualVal, absoluteError, relativeError * 100, pass ? "Pass" : "Fail"
                );
            }

            if (!pass) {
                errorCount++;
            }
        }
        if (expectedVector.length > 100) {
            System.out.println("... (comparison output truncated for brevity)");
        }
        System.out.println(new String(new char[110]).replace('\0', '-'));

        assertTrue(
                "The calculation result exceeds the allowable error range. " + errorCount + " errors found.",
                errorCount == 0
        );

        System.out.println("\nCongratulations! This test case passed!");
    }

    /**
     * Generates a 2D matrix of floats with random integer values.
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

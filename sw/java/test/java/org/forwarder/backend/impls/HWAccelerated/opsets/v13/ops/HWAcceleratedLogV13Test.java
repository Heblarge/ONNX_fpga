package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test class validates the HWAcceleratedLogV13 operator for
 * both 2D and 3D (batched) tensors using floating-point inputs.
 */
public class HWAcceleratedLogV13Test extends HWAcceleratedTestCase {

    /**
     * Tests standard 2D Log operation with random floats.
     */
    @Test
    public void testLog2D() throws Exception {
        System.out.println("\n--- Testing 2D Log ---");
        int rows = 32;
        int cols = 32;
        // Log is only defined for positive numbers
        int minValue = 1;
        int maxValue = 20;

        INDArray input = Nd4j.create(generateRandomFloatMatrix(rows, cols, minValue, maxValue));
        INDArray expected = Transforms.log(input.dup(), true); // Base e

        this.testLog(expected, input);
    }

    /**
     * Tests 3D (batched) Log operation with random floats.
     */
    @Test
    public void testLog3D() throws Exception {
        System.out.println("\n--- Testing 3D (Batched) Log ---");
        int batchSize = 2;
        int rows = 28;
        int cols = 512;
        int minValue = 1;
        int maxValue = 10;

        INDArray input = createRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue);
        INDArray expected = Transforms.log(input.dup(), true); // Base e

        this.testLog(expected, input);
    }

    /**
     * Helper method to run the operator, print matrices, and assert correctness.
     */
    private void testLog(INDArray expected, INDArray input) throws Exception {
        HWAcceleratedLogV13 operator = new HWAcceleratedLogV13();
        INDArray actualOutput = operator.log(input);

        System.out.println("\ninputA shape: " + java.util.Arrays.toString(input.shape()));
        System.out.print(input);
        System.out.println("\n\nexpectedMatrix shape: " + java.util.Arrays.toString(expected.shape()));
        System.out.print(expected);
        System.out.println("\n\nactualOutput shape: " + java.util.Arrays.toString(actualOutput.shape()));
        System.out.print(actualOutput);

        assertArrayEquals("The output shape must match the expected shape.", expected.shape(), actualOutput.shape());

        float[] expectedVector = expected.dup('c').data().asFloat();
        float[] actualVector = actualOutput.dup('c').data().asFloat();

        int errorCount = 0;
        double relativeErrorTolerance = 0.02; // Allow 2% relative error
        double absoluteErrorTolerance = 1e-3;  // Absolute error for small values

        System.out.println("\n");
        System.out.println(new String(new char[110]).replace('\0', '-'));
        System.out.printf(
                "%-10s | %-20s | %-20s | %-20s | %-20s | %-7s%n",
                "index", "Expected", "Actual", "Abs Error", "Rel Error %", "check"
        );
        System.out.println(new String(new char[110]).replace('\0', '-'));

        for (int i = 0; i < expectedVector.length; i++) {
            double expectedVal = expectedVector[i];
            double actualVal = actualVector[i];
            double absoluteError = Math.abs(actualVal - expectedVal);
            double relativeError = (Math.abs(expectedVal) > 1e-6) ? (absoluteError / expectedVal) : 0.0;

            boolean pass = (relativeError < relativeErrorTolerance) || (absoluteError < absoluteErrorTolerance);

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

        System.out.println("\nCongratulations! All tests pass!");
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
    /**
     * Generates a 3D matrix of floats with random integer values.
     */
    private INDArray createRandom3DIntegerMatrix(int batch, int rows, int cols, int min, int max) {
        INDArray matrix = Nd4j.create(batch, rows, cols);
        for (int i = 0; i < batch; i++) {
            matrix.putSlice(i, Nd4j.create(generateRandomIntegerMatrix(rows, cols, min, max)));
        }
        return matrix;
    }
    /**
     * Generates a 2D matrix of floats with random float values.
     */
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
    /**
     * Generates a 3D matrix of floats with random float values.
     */
    private INDArray createRandom3DFloatMatrix(int batch, int rows, int cols, int min, int max) {
        INDArray matrix = Nd4j.create(batch, rows, cols);
        for (int i = 0; i < batch; i++) {
            matrix.putSlice(i, Nd4j.create(generateRandomFloatMatrix(rows, cols, min, max)));
        }
        return matrix;
    }
}

package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test class validates the HWAcceleratedMaxV13 operator.
 * The validation logic is aligned with the Sub operator's test, providing
 * a 3-way comparison between theoretical, simulated, and actual hardware values.
 */
public class HWAcceleratedMaxV13Test extends HWAcceleratedTestCase {

    /**
     * Helper method that simulates the exact fixed-point arithmetic of the Max hardware operator.
     */
    private INDArray calculateSimulatedFixedPointMax(INDArray a, INDArray b) {
        // Match the fixed-point parameters from the Max operator
        int fracWidth = 8;
        double scaleFactor = Math.pow(2, fracWidth);

        int rows = (int) a.rows();
        int cols = (int) a.columns();

        // 1. Scale up and round to integer
        int[][] fixedPointA = new int[rows][cols];
        int[][] fixedPointB = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                fixedPointA[i][j] = (int)Math.round(a.getFloat(i, j) * scaleFactor);
                fixedPointB[i][j] = (int)Math.round(b.getFloat(i, j) * scaleFactor);
            }
        }

        // 2. Perform the max operation in the integer domain
        int[][] fixedPointOutput = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                fixedPointOutput[i][j] = Math.max(fixedPointA[i][j], fixedPointB[i][j]);
            }
        }

        // 3. Scale back down to float
        float[] output = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                output[i * cols + j] = (float)(((double)(fixedPointOutput[i][j])) / scaleFactor);
            }
        }

        return Nd4j.create(output).reshape(rows, cols);
    }

    @Test
    public void testMax2D() throws Exception {
        System.out.println("\n--- Testing 2D Max ---");
        int rows = 30;
        int cols = 32;
        float minValue = -10.0f;
        float maxValue = 10.0f;

        INDArray matrixA = Nd4j.create(generateRandomFloatMatrix(rows, cols, minValue, maxValue));
        INDArray matrixB = Nd4j.create(generateRandomFloatMatrix(rows, cols, minValue, maxValue));

        INDArray theoreticalExpected = Transforms.max(matrixA, matrixB);
        INDArray simulatedExpected = calculateSimulatedFixedPointMax(matrixA, matrixB);

        this.validateMax(theoreticalExpected, simulatedExpected, matrixA, matrixB);
    }

    @Test
    public void testMax3D() throws Exception {
        System.out.println("\n--- Testing 3D (Batched) Max ---");
        int batchSize = 2;
        int rows = 30;
        int cols = 32;
        float minValue = -10.0f;
        float maxValue = 10.0f;

        INDArray matrixA = createRandom3DMatrix(batchSize, rows, cols, minValue, maxValue);
        INDArray matrixB = createRandom3DMatrix(batchSize, rows, cols, minValue, maxValue);

        INDArray theoreticalExpected = Transforms.max(matrixA, matrixB);
        INDArray simulatedExpected = Nd4j.create(matrixA.shape());
        for (int i = 0; i < batchSize; i++) {
            INDArray sliceA = matrixA.slice(i);
            INDArray sliceB = matrixB.slice(i);
            INDArray expectedSlice = calculateSimulatedFixedPointMax(sliceA, sliceB);
            simulatedExpected.putSlice(i, expectedSlice);
        }

        this.validateMax(theoreticalExpected, simulatedExpected, matrixA, matrixB);
    }

    /**
     * Validation helper with a 3-way comparison, matching the Sub test format.
     */
    private void validateMax(INDArray theoreticalExpected, INDArray simulatedExpected, INDArray inputA, INDArray inputB) throws Exception {
        HWAcceleratedMaxV13 operator = new HWAcceleratedMaxV13();
        List<INDArray> inputs = Arrays.asList(inputA, inputB);
        INDArray actualOutput = operator.max(inputs);

        assertArrayEquals("The output shape must match the expected shape.", simulatedExpected.shape(), actualOutput.shape());

        float[] theoreticalVector = theoreticalExpected.dup('c').data().asFloat();
        float[] simulatedVector = simulatedExpected.dup('c').data().asFloat();
        float[] actualVector = actualOutput.dup('c').data().asFloat();

        String horizontalLine = new String(new char[154]).replace('\0', '-');
        String headerFormat = "%-8s | %-10s | %-12s | %-9s | %-15s | %-5s | %-15s | %-5s | %-7s%n";
        String dataFormat   = "%-8d | %-15.6f | %-15.6f | %-15.6f | %-15.6f | %-15s | %-15.6f | %-15s | %-7s%n";

        System.out.println("\n\n" + horizontalLine);
        System.out.printf(headerFormat, "Index", "理论值", "模拟值", "实际值", "总误差(Abs)", "总误差(Rel)", "逻辑误差(Abs)", "逻辑误差(Rel)", "Check");
        System.out.println(horizontalLine);

        int errorCount = 0;
        double hardwareLogicTolerance = 1e-6;

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

    private INDArray createRandom3DMatrix(int batch, int rows, int cols, float min, float max) {
        INDArray matrix = Nd4j.create(batch, rows, cols);
        for (int i = 0; i < batch; i++) {
            matrix.putSlice(i, Nd4j.create(generateRandomFloatMatrix(rows, cols, min, max)));
        }
        return matrix;
    }
}
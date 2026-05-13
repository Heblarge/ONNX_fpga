package org.forwarder.backend.impls.HWAccelerated.utils;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.Random;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import org.nd4j.linalg.api.buffer.DataType;

public final class HWAcceleratedTestModel {

    private HWAcceleratedTestModel() {}

    /**
     * A generic, reusable method to validate operator outputs with a 3-way comparison.
     * It uses the user-specified printf format for detailed reporting.
     * This version prints details for every single element.
     *
     * @param opName The name of the operator being tested.
     * @param theoreticalExpected The high-precision floating-point result.
     * @param simulatedExpected The result from a software simulation of the fixed-point hardware logic.
     * @param actualOutput The actual result from the hardware-accelerated operator.
     * @param hardwareLogicTolerance The tolerance for comparing the simulated vs. actual results.
     */
    public static void validate(String opName, INDArray theoreticalExpected, INDArray simulatedExpected, INDArray actualOutput, double hardwareLogicTolerance) {
        assertArrayEquals("The output shape must match the expected shape.", simulatedExpected.shape(), actualOutput.shape());

        float[] theoreticalVector = theoreticalExpected.dup('c').data().asFloat();
        float[] simulatedVector = simulatedExpected.dup('c').data().asFloat();
        float[] actualVector = actualOutput.dup('c').data().asFloat();

        String horizontalLine = new String(new char[201]).replace('\0', '-');

        String headerFormat = "%-8s | %-10s | %-12s | %-9s | %-15s | %-5s | %-15s | %-5s | %-7s%n";
        String dataFormat   = "%-8d | %-15.6f | %-15.6f | %-14f | %-15.6f | %-12s | %-15.6f | %-12s | %-7s%n";

        System.out.println("\n\n" + horizontalLine);
        System.out.printf(headerFormat,
                "Index", "理论值", "模拟值(理论模拟硬件值)", "实际值", "总误差(理论与实际绝对误差)", "总相对误差",
                "逻辑误差(模拟与实际绝对误差)", "逻辑相对误差", "Check"
        );
        System.out.println(horizontalLine);

        int errorCount = 0;

        for (int i = 0; i < actualVector.length; i++) {
            double theoreticalVal = theoreticalVector[i];
            double simulatedVal = simulatedVector[i];
            double actualVal = actualVector[i];

            double totalAbsError = Math.abs(actualVal - theoreticalVal);
            double totalRelError = (Math.abs(theoreticalVal) > 1e-7) ? (totalAbsError / Math.abs(theoreticalVal)) : 0.0;
            double logicAbsError = Math.abs(actualVal - simulatedVal);
            double logicRelError = (Math.abs(simulatedVal) > 1e-7) ? (logicAbsError / Math.abs(simulatedVal)) : 0.0;
            boolean pass = logicAbsError <= hardwareLogicTolerance;

            if (!pass) {
                errorCount++;
            }

            String totalRelErrorStr = String.format("%.2f%%", totalRelError * 100);
            String logicRelErrorStr = String.format("%.2f%%", logicRelError * 100);

            System.out.printf(dataFormat, i, theoreticalVal, simulatedVal, actualVal,
                    totalAbsError, totalRelErrorStr, logicAbsError, logicRelErrorStr,
                    pass ? "Pass" : "Fail");
        }
        System.out.println(horizontalLine);

        assertTrue(String.format("[%s] 硬件实际值与模拟值不符，逻辑错误! 共 %d 个错误。", opName, errorCount), errorCount == 0);
        System.out.println(String.format("\nCongratulations!\n [%s] test case passed with fixed-point validation!", opName));
    }


    /**
     * NEW: Validation for NON-LINEAR activation functions (Exp, Log, etc.).
     * This method compares the actual hardware output directly against the theoretical high-precision result.
     *
     * @param opName The name of the activation function being tested.
     * @param theoreticalExpected The high-precision floating-point result.
     * @param actualOutput The actual result from the hardware-accelerated operator.
     * @param tolerance The acceptable relative or absolute error.
     */
    public static void validateActivationFunction(String opName, INDArray theoreticalExpected, INDArray actualOutput, double tolerance) {
        assertArrayEquals("The output shape must match the expected shape.", theoreticalExpected.shape(), actualOutput.shape());

        float[] theoreticalVector = theoreticalExpected.dup('c').data().asFloat();
        float[] actualVector = actualOutput.dup('c').data().asFloat();

        String horizontalLine = new String(new char[100]).replace('\0', '-');
        String headerFormat = "%-8s | %-20s | %-20s | %-20s | %-20s | %-7s%n";
        String dataFormat = "%-8d | %-20.6f | %-20.6f | %-20.6f | %-20s | %-7s%n";

        System.out.println("\n\n" + horizontalLine);
        System.out.printf(headerFormat, "Index", "理论值 (Expected)", "实际硬件值 (Actual)", "绝对误差", "相对误差", "Check");
        System.out.println(horizontalLine);

        int errorCount = 0;

        for (int i = 0; i < actualVector.length; i++) {
            double theoreticalVal = theoreticalVector[i];
            double actualVal = actualVector[i];

            double absError = Math.abs(actualVal - theoreticalVal);
            double relError = (Math.abs(theoreticalVal) > 1e-9) ? (absError / Math.abs(theoreticalVal)) : 0.0;
           boolean pass = (relError < 2e-1) || (absError < tolerance);

            if (!pass) {
                errorCount++;
            }

            String relErrorStr = String.format("%.2f%%", relError * 100);
            System.out.printf(dataFormat, i, theoreticalVal, actualVal, absError, relErrorStr, pass ? "Pass" : "Fail");
        }
        System.out.println(horizontalLine);
        assertTrue(String.format("[%s] The calculation result exceeds the allowable error range. %d errors found.", opName, errorCount), errorCount == 0);
        System.out.println(String.format("\nCongratulations! [%s] test case passed!", opName));
    }

    /**
     * Generates a 2D matrix with random float values.
     */
    public static float[][] generateRandom2DFloatMatrix(int rows, int cols, float min, float max, long scale) {
        Random random = new Random();
        float[][] matrix = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = (float) ((min + random.nextFloat() * (max - min)) * Math.pow(2,scale));
            }
        }
        return matrix;
    }

    /**
     * Generates a 3D matrix with random float values.
     */
    public static INDArray generateRandom3DFloatMatrix(int batch, int rows, int cols, float min, float max, long scale) {
        INDArray matrix = Nd4j.create(batch, rows, cols);
        for (int i = 0; i < batch; i++) {
            matrix.putSlice(i, Nd4j.create(generateRandom2DFloatMatrix(rows, cols, min, max, scale)));
        }
        return matrix;
    }

    public static int[][] generateRandom2DIntMatrix(int rows, int cols, float min, float max, long scale) {
        Random random = new Random();
        int[][] matrix = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double randomFloat = (min + random.nextDouble() * (max - min)) * Math.pow(2, scale);
                matrix[i][j] = (int) Math.round(randomFloat);
            }
        }
        return matrix;
    }

    /**
     * Generates a 3D INDArray of DataType.INT32 with random integer values.
     */
    public static INDArray generateRandom3DIntMatrix(int batch, int rows, int cols, int min, int max, long scale) {
        INDArray matrix = Nd4j.create(batch, rows, cols);
        for (int i = 0; i < batch; i++) {
            int[][] sliceData = generateRandom2DIntMatrix(rows, cols, min, max, scale);
            INDArray slice = Nd4j.createFromArray(sliceData);
            matrix.putSlice(i, slice);
        }
        return matrix.castTo(DataType.LONG);
    }
}


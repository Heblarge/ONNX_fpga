package org.forwarder.backend.impls.HWAccelerated.utils;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.Random;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

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
     * Generates a 2D matrix with random float values.
     */
    public static float[][] generateRandom2DFloatMatrix(int rows, int cols, float min, float max) {
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
     * Generates a 3D matrix with random float values.
     */
    public static INDArray generateRandom3DFloatMatrix(int batch, int rows, int cols, float min, float max) {
        INDArray matrix = Nd4j.create(batch, rows, cols);
        for (int i = 0; i < batch; i++) {
            matrix.putSlice(i, Nd4j.create(generateRandom2DFloatMatrix(rows, cols, min, max)));
        }
        return matrix;
    }
}


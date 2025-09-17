package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class HWAcceleratedMatMulV13Test extends HWAcceleratedTestCase {

    /**
     * Simulates the exact fixed-point arithmetic of the MatMul hardware operator.
     */
    private INDArray calculateSimulatedFixedPointMatMul(INDArray a, INDArray b) {
        // These parameters must EXACTLY match the ones in HWAcceleratedMatMulV13
        int fracWidth = 8;
        double scaleFactor = Math.pow(2, fracWidth);

        int rowsA = (int) a.size(0);
        int colsA = (int) a.size(1); // This is also rowsB
        int colsB = (int) b.size(1);

        // 1. Convert inputs to fixed-point integers
        int[][] fixedPointA = new int[rowsA][colsA];
        int[][] fixedPointB = new int[colsA][colsB];
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsA; j++) {
                fixedPointA[i][j] = (int)Math.round(a.getFloat(i, j) * scaleFactor);
            }
        }
        for (int i = 0; i < colsA; i++) {
            for (int j = 0; j < colsB; j++) {
                fixedPointB[i][j] = (int)Math.round(b.getFloat(i, j) * scaleFactor);
            }
        }

        // 2. Perform matrix multiplication in the integer domain
        int[][] fixedPointOutput = new int[rowsA][colsB];
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                for (int k = 0; k < colsA; k++) {
                    fixedPointOutput[i][j] += fixedPointA[i][k] * fixedPointB[k][j];
                }
            }
        }

        // 3. Convert the result back to float, accounting for double scaling
        float[] output = new float[rowsA * colsB];
        double finalScaleFactor = scaleFactor * scaleFactor;
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                output[i * colsB + j] = (float) (fixedPointOutput[i][j] / finalScaleFactor);
            }
        }

        return Nd4j.create(output).reshape(rowsA, colsB);
    }

    @Test
    public void testMatMul2D() throws Exception {
        System.out.println("\n--- Testing 2D MatMul ---");
        int rowsA = 32;
        int colsA = 3;
        int colsB = 33;
        int minValue = -3;
        int maxValue = 3;

        INDArray matrixA = Nd4j.create(generateRandomFloatMatrix(rowsA, colsA, minValue, maxValue));
        INDArray matrixB = Nd4j.create(generateRandomFloatMatrix(colsA, colsB, minValue, maxValue));

        INDArray theoreticalExpected = matrixA.mmul(matrixB);
        INDArray simulatedExpected = calculateSimulatedFixedPointMatMul(matrixA, matrixB);

        this.validateMatMul(theoreticalExpected, simulatedExpected, matrixA, matrixB);
    }

    @Test
    public void testMatMul3D() throws Exception {
        System.out.println("\n--- Testing 3D (Batched) MatMul ---");
        int batchSize = 32;
        int rowsA = 64; // Using smaller dimensions to keep test fast
        int colsA = 32;
        int colsB = 32;
        int minValue = -2;
        int maxValue = 2;

        INDArray matrixA = createRandom3DMatrix(batchSize, rowsA, colsA, minValue, maxValue);
        INDArray matrixB = createRandom3DMatrix(batchSize, colsA, colsB, minValue, maxValue);

        INDArray theoreticalExpected = Nd4j.create(batchSize, rowsA, colsB);
        for (int i = 0; i < batchSize; i++) {
            INDArray productSlice = matrixA.slice(i).mmul(matrixB.slice(i));
            theoreticalExpected.putSlice(i, productSlice);
        }
        INDArray simulatedExpected = Nd4j.create(batchSize, rowsA, colsB);
        for (int i = 0; i < batchSize; i++) {
            simulatedExpected.putSlice(i, calculateSimulatedFixedPointMatMul(matrixA.slice(i), matrixB.slice(i)));
        }

        this.validateMatMul(theoreticalExpected, simulatedExpected, matrixA, matrixB);
    }

    private void validateMatMul(INDArray theoreticalExpected, INDArray simulatedExpected, INDArray inputA, INDArray inputB) throws Exception {
        HWAcceleratedMatMulV13 operator = new HWAcceleratedMatMulV13();
        INDArray actualOutput = operator.matmul(inputA, inputB);

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

    /**
     * Generates a 3D matrix with random float values.
     */
    private INDArray createRandom3DMatrix(int batch, int rows, int cols, float min, float max) {
        INDArray matrix = Nd4j.create(batch, rows, cols);
        for (int i = 0; i < batch; i++) {
            matrix.putSlice(i, Nd4j.create(generateRandomFloatMatrix(rows, cols, min, max)));
        }
        return matrix;
    }
}
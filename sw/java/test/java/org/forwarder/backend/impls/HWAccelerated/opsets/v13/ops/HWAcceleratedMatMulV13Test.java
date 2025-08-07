package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class HWAcceleratedMatMulV13Test extends HWAcceleratedTestCase {

    @Test
    public void testMatMul2D() throws Exception {
        System.out.println("\n--- Testing 2D MatMul ---");
        int rowsA = 32;
        int colsA = 32;
        int colsB = 32;
        int minValue = -10;
        int maxValue = 10;

        float[][] randomMatrixA = generateRandomIntegerMatrix(rowsA, colsA, minValue, maxValue);
        float[][] randomMatrixB = generateRandomIntegerMatrix(colsA, colsB, minValue, maxValue);
        INDArray matrixA = Nd4j.create(randomMatrixA);
        INDArray matrixB = Nd4j.create(randomMatrixB);

        INDArray expectedMatrix = matrixA.mmul(matrixB);

        this.testMatMul(expectedMatrix, matrixA, matrixB);
    }

    /**
     * Tests 3D (batched) matrix multiplication.
     */
    @Test
    public void testMatMul3D() throws Exception {
        System.out.println("\n--- Testing 3D (Batched) MatMul ---");
        int batchSize = 2;
        int rowsA = 4;
        int colsA = 4;
        int colsB = 4;
        int minValue = -10;
        int maxValue = 10;

        // Create random 3D tensors with integer values for batched multiplication
        INDArray matrixA = Nd4j.create(batchSize, rowsA, colsA);
        INDArray matrixB = Nd4j.create(batchSize, colsA, colsB);
        for (int i = 0; i < batchSize; i++) {
            matrixA.putSlice(i, Nd4j.create(generateRandomIntegerMatrix(rowsA, colsA, minValue, maxValue)));
            matrixB.putSlice(i, Nd4j.create(generateRandomIntegerMatrix(colsA, colsB, minValue, maxValue)));
        }

        // Calculate the expected result by multiplying each slice individually
        INDArray expectedMatrix = Nd4j.create(batchSize, rowsA, colsB);
        for (int i = 0; i < batchSize; i++) {
            expectedMatrix.putSlice(i, matrixA.slice(i).mmul(matrixB.slice(i)));
        }

        this.testMatMul(expectedMatrix, matrixA, matrixB);
    }


    private void testMatMul(INDArray expected, INDArray inputA, INDArray inputB) throws Exception {
        HWAcceleratedMatMulV13 operator = new HWAcceleratedMatMulV13();
        INDArray actualOutput = operator.matmul(inputA, inputB);

        System.out.println("\ninputA:");
        System.out.print(inputA);
        System.out.println("\ninputB:");
        System.out.print(inputB);
        System.out.println("\nexpectedMatrix:");
        System.out.print(expected);
        System.out.println("\nactualOutput:");
        System.out.print(actualOutput);

        assertArrayEquals("The number of outputs should match the number of inputs.", expected.shape(), actualOutput.shape());

        float[] expectedVector = expected.dup('c').data().asFloat();
        float[] actualVector = actualOutput.data().asFloat();

        int errorCount = 0;
        double relativeErrorTolerance = 0.02; // 允许 2% 的相对误差

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

            double absoluteError = actualVal - expectedVal;
            double relativeError = (Math.abs(expectedVal) > 1e-6) ? (absoluteError / expectedVal) : 0.0;

            boolean pass = (Math.abs(relativeError) < relativeErrorTolerance)|| (Math.abs(absoluteError) < 1e-3);

            System.out.printf(
                        "%-10d | %-20.6f | %-20.6f | %-20.6f | %-20.2f%% | %-7s%n",
                        i,
                        expectedVal,
                        actualVal,
                        absoluteError,
                        relativeError * 100,
                        pass ? "Pass" : "Fail"
            );

            if (!pass) {
                errorCount++;
            }
        }
        System.out.println(new String(new char[110]).replace('\0', '-'));

        assertTrue(
                "计算结果超出允许的误差范围。共发现 " + errorCount + " 个错误。",
                errorCount == 0
        );

        System.out.println("\nCongratulations! All tests pass!");
}


    private float[][] generateRandomIntegerMatrix(int rows, int cols, int min, int max) {
        Random random = new Random();
        float[][] matrix = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // 生成一个在 [min, max) 区间内的随机整数
                matrix[i][j] = random.nextInt(max - min + 1) + min;
            }
        }
        return matrix;
    }
}
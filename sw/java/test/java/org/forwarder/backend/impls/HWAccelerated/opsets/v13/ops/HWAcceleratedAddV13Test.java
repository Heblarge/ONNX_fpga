package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class HWAcceleratedAddV13Test extends HWAcceleratedTestCase {

    @Test
    public void testWithRandomFloatMatrix() throws Exception {
        int matrixSize = 32;
        int rows = 16;
        int cols = 8;
        int minValue = -10;
        int maxValue = 10;

        float[][] randomMatrixA = generateRandomIntegerMatrix(rows, cols, minValue, maxValue);
        float[][] randomMatrixB = generateRandomIntegerMatrix(rows, cols, minValue, maxValue);
        INDArray MatrixA = Nd4j.create(randomMatrixA);
        INDArray MatrixB = Nd4j.create(randomMatrixB);

        INDArray expectedMatrix = MatrixA.add(MatrixB);

        this.testAdd(expectedMatrix, MatrixA, MatrixB);
    }


    private void testAdd(INDArray expected, INDArray inputA, INDArray inputB) throws Exception {
        HWAcceleratedAddV13 operator = new HWAcceleratedAddV13();

        INDArray actualOutput = operator.add(inputA, inputB);

        System.out.println("\ninputA:");
        System.out.print(inputA);
        System.out.println("\ninputB:");
        System.out.print(inputB);
        System.out.println("\nexpectedMatrix:");
        System.out.print(expected);
        System.out.println("\nactualOutput:");
        System.out.print(actualOutput);

        assertArrayEquals("The number of outputs should match the number of inputs.", expected.shape(), actualOutput.shape());

        float[] expectedVector = expected.data().asFloat();
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

            boolean pass = (relativeError < relativeErrorTolerance) || (Math.abs(absoluteError) < 1e-3);

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
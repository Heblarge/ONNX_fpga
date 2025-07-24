package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class HWAcceleratedLogV13Test extends HWAcceleratedTestCase {


    @Test
    public void testWithRandomFloatMatrix() throws Exception {
        int matrixSize = 32;
        int rows = 4;
        int cols = 8;
        float minValue = 0.0f;
        float maxValue = 16.0f;

        float[][] randomMatrix = generateRandomFloatMatrix(rows, cols, minValue, maxValue);
        INDArray input = Nd4j.create(randomMatrix);

        float[][] expectedMatrix = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                expectedMatrix[i][j] = (float) Math.log(randomMatrix[i][j]);
            }
        }
        INDArray expected = Nd4j.create(expectedMatrix);

        this.testLog(expected, input);
    }


    private void testLog(INDArray expected, INDArray input) throws Exception {
        HWAcceleratedLogV13 operator = new HWAcceleratedLogV13();

        int fracWidth = AcceleratorSimInterface.acceleratorCfg().fracWidth();
        double factor = Math.pow(2, fracWidth);
        INDArray inputForHardware = input.mul(factor);

        INDArray rawActualOutput = operator.log(inputForHardware);
        INDArray actualOutput = rawActualOutput.div(factor);

        System.out.println("\ninput:");
        System.out.print(input);
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

            boolean pass = (Math.abs(relativeError) < relativeErrorTolerance) || (Math.abs(absoluteError) < 2 * 1e-2);

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


    private float[][] generateRandomFloatMatrix(int rows, int cols, float min, float max) {
        Random random = new Random();
        float[][] matrix = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // 生成一个在 [min, max) 区间内的随机浮点数
                matrix[i][j] = min + random.nextFloat() * (max - min);
            }
        }
        return matrix;
    }
}
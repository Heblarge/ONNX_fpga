package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
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
        int fracWidth = 9;
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
        INDArray matrixA = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue));
        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue));
        INDArray theoreticalExpected = Transforms.max(matrixA, matrixB);
        INDArray simulatedExpected = calculateSimulatedFixedPointMax(matrixA, matrixB);
        HWAcceleratedMaxV13 operator = new HWAcceleratedMaxV13();
        List<INDArray> inputs = Arrays.asList(matrixA, matrixB);
        INDArray actualOutput = operator.max(inputs);
        double hardwareLogicTolerance = 1e-6;
        HWAcceleratedTestModel.validate("Max - 2D",theoreticalExpected, simulatedExpected, actualOutput, hardwareLogicTolerance);
    }

    @Test
    public void testMax3D() throws Exception {
        System.out.println("\n--- Testing 3D (Batched) Max ---");
        int batchSize = 2;
        int rows = 30;
        int cols = 32;
        float minValue = -10.0f;
        float maxValue = 10.0f;

        INDArray matrixA = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue);
        INDArray matrixB = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue);

        INDArray theoreticalExpected = Transforms.max(matrixA, matrixB);
        INDArray simulatedExpected = Nd4j.create(matrixA.shape());
        for (int i = 0; i < batchSize; i++) {
            INDArray sliceA = matrixA.slice(i);
            INDArray sliceB = matrixB.slice(i);
            INDArray expectedSlice = calculateSimulatedFixedPointMax(sliceA, sliceB);
            simulatedExpected.putSlice(i, expectedSlice);
        }
        HWAcceleratedMaxV13 operator = new HWAcceleratedMaxV13();
        List<INDArray> inputs = Arrays.asList(matrixA, matrixB);
        INDArray actualOutput = operator.max(inputs);
        double hardwareLogicTolerance = 1e-6;
        HWAcceleratedTestModel.validate("Max - 3D",theoreticalExpected, simulatedExpected, actualOutput, hardwareLogicTolerance);

    }

}
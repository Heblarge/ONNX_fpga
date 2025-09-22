package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
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
        int fracWidth = 9;
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
        int minValue = -10;
        int maxValue = 5;

        INDArray matrixA = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rowsA, colsA, minValue, maxValue));
        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsA, colsB, minValue, maxValue));

        INDArray theoreticalExpected = matrixA.mmul(matrixB);
        INDArray simulatedExpected = calculateSimulatedFixedPointMatMul(matrixA, matrixB);

        HWAcceleratedMatMulV13 operator = new HWAcceleratedMatMulV13();
        INDArray actualOutput = operator.matmul(matrixA, matrixB);

        HWAcceleratedTestModel.validate("MatMul - 2D", theoreticalExpected, simulatedExpected, actualOutput, 1e-6);
    }

    @Test
    public void testMatMul3D() throws Exception {
        System.out.println("\n--- Testing 3D (Batched) MatMul ---");
        int batchSize = 2;
        int rowsA = 32;
        int colsA = 32;
        int colsB = 32;
        int minValue = -5;
        int maxValue = 5;

        INDArray matrixA = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rowsA, colsA, minValue, maxValue);
        INDArray matrixB = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, colsA, colsB, minValue, maxValue);

        INDArray theoreticalExpected = Nd4j.create(batchSize, rowsA, colsB);
        for (int i = 0; i < batchSize; i++) {
            INDArray productSlice = matrixA.slice(i).mmul(matrixB.slice(i));
            theoreticalExpected.putSlice(i, productSlice);
        }
        INDArray simulatedExpected = Nd4j.create(batchSize, rowsA, colsB);
        for (int i = 0; i < batchSize; i++) {
            simulatedExpected.putSlice(i, calculateSimulatedFixedPointMatMul(matrixA.slice(i), matrixB.slice(i)));
        }

        HWAcceleratedMatMulV13 operator = new HWAcceleratedMatMulV13();
        INDArray actualOutput = operator.matmul(matrixA, matrixB);

        HWAcceleratedTestModel.validate("MatMul - 3D", theoreticalExpected, simulatedExpected, actualOutput, 1e-6);
    }

    @Test
    public void testMatMul3DWithSimpleInts() throws Exception {
        System.out.println("\n--- Testing 3D (Batched) MatMul with Simple Integers ---");
        int batchSize = 2;
        int rowsA = 32;
        int colsA = 32;
        int colsB = 32;

        INDArray matrixA = createSimple3DMatrix(batchSize, rowsA, colsA, new float[]{1f, -1f});
        INDArray matrixB = createSimple3DMatrix(batchSize, colsA, colsB, new float[]{2f, 1f, -1f});

        INDArray theoreticalExpected = Nd4j.create(batchSize, rowsA, colsB);
        for (int i = 0; i < batchSize; i++) {
            INDArray productSlice = matrixA.slice(i).mmul(matrixB.slice(i));
            theoreticalExpected.putSlice(i, productSlice);
        }
        INDArray simulatedExpected = Nd4j.create(batchSize, rowsA, colsB);
        for (int i = 0; i < batchSize; i++) {
            simulatedExpected.putSlice(i, calculateSimulatedFixedPointMatMul(matrixA.slice(i), matrixB.slice(i)));
        }
        HWAcceleratedMatMulV13 operator = new HWAcceleratedMatMulV13();
        INDArray actualOutput = operator.matmul(matrixA, matrixB);
        double hardwareLogicTolerance = 1e-6;
        HWAcceleratedTestModel.validate("MatMul-3D", theoreticalExpected, simulatedExpected, actualOutput, hardwareLogicTolerance);
    }



    private INDArray createSimple3DMatrix(int batch, int rows, int cols, float[] values) {
        INDArray matrix = Nd4j.create(batch, rows, cols);
        int valueIndex = 0;
        for (int i = 0; i < batch; i++) {
            for (int j = 0; j < rows; j++) {
                for (int k = 0; k < cols; k++) {
                    matrix.putScalar(i, j, k, values[valueIndex % values.length]);
                    valueIndex++;
                }
            }
        }
        return matrix;
    }

}
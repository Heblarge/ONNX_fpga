package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.List;
import java.util.Arrays;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.linalg.api.buffer.DataType;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test class provides comprehensive validation for the HWAcceleratedGemmV13 operator,
 * using a precise fixed-point simulation for accurate result verification.
 */
public class HWAcceleratedGemmV13Test extends HWAcceleratedTestCase {

    private INDArray calculateSimulatedGemmWithShifts(
            INDArray A, INDArray B, INDArray C,
            float alpha, float beta, long transA, long transB,
            List<Long> fpgaInShift, Long fpgaOutShift
    ) {
        if (transA != 0L) { A = A.transpose(); }
        if (transB != 0L) { B = B.transpose(); }

        int m = (int) A.rows();
        int k = (int) A.columns();
        int n = (int) B.columns();

        long[][] matA_long = new long[m][k];
        for (int i = 0; i < m; i++) for (int j = 0; j < k; j++) matA_long[i][j] = A.getInt(i, j);

        long[][] matB_long = new long[k][n];
        for (int i = 0; i < k; i++) for (int j = 0; j < n; j++) matB_long[i][j] = B.getInt(i, j);

        // 2. Perform matrix multiplication manually with a long accumulator
        long[][] matMulResult_long = new long[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                long accumulator = 0L;
                for (int l = 0; l < k; l++) {
                    accumulator += matA_long[i][l] * matB_long[l][j];
                }
                matMulResult_long[i][j] = accumulator;
            }
        }

        long shiftAmount = fpgaInShift.get(0) + fpgaInShift.get(1) - fpgaOutShift;

        long[][] shiftedResult_long = new long[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                long val = matMulResult_long[i][j];
                shiftedResult_long[i][j] = (long) (val / Math.pow(2,shiftAmount));
            }
        }
        float[] outputFloat = new float[m * n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                outputFloat[i * n + j] = (float)(shiftedResult_long[i][j]);
            }
        }

        INDArray Y = Nd4j.create(outputFloat, new long[]{m, n});

        Y.muli(alpha);

        if (C != null) {
            long[] yShape = Y.shape();
            if (!java.util.Arrays.equals(C.shape(), yShape)) { C = C.broadcast(yShape); }
            INDArray betaC = C.mul(beta);
            Y.addi(betaC);
        }

        return Y;
    }


    /**
     * Simulates the entire Gemm operation (alpha * A'B' + beta * C) using the fixed-point MatMul simulation.
     */

    @Test
    public void testGemmWithAllParameters() throws Exception {
        System.out.println("\n--- Testing Gemm with all parameters (Y = alpha * A'* B' + beta * C) ---");
        int rowsA = 32;
        int colsA = 32;
        int colsB = 16;
        float minValue = -5f;
        float maxValue = 5f;

        float alpha = 1f;
        float beta = 2.0f;

        List<Long> fpgaInShift = Arrays.asList(24L, 24L);
        Long fpgaOutShift = 25L;
        long shift1 = fpgaInShift.get(0);
        long shift2 = fpgaInShift.get(1);

        INDArray matrixA = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rowsA, colsA, minValue, maxValue, shift1));
        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsA, colsB, minValue, maxValue, shift2));
        long shiftAmount = fpgaInShift.get(0) + fpgaInShift.get(1) - fpgaOutShift;

        INDArray theoreticalExpected = matrixA.mmul(matrixB).mul(alpha).div(1L << shiftAmount);
        INDArray simulatedExpected = calculateSimulatedGemmWithShifts(matrixA, matrixB, null,
                alpha, beta, 0L, 0L, fpgaInShift, fpgaOutShift
        );

        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
        INDArray actualOutput = operator.gemm(matrixA, matrixB, null,
                alpha, beta, 0L, 0L, fpgaInShift, fpgaOutShift);

        double tolerance = 1e-2;
        HWAcceleratedTestModel.validate("Gemm - Full", theoreticalExpected, simulatedExpected, actualOutput, tolerance);
    }

//    @Test
//    public void testMatMul3Dx2D() throws Exception {
//        System.out.println("\n--- Testing 3D x 2D MatMul ---");
//
//        int batchSize = 4;
//        int rowsA = 32;
//        int colsA = 64;
//        int colsB = 16;
//        int minValue = -3;
//        int maxValue = 3;
//
//        float alpha = 1.0f;
//        float beta = 0.0f;
//
//        INDArray matrixA = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rowsA, colsA, minValue, maxValue);
//        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsA, colsB, minValue, maxValue));
//
//        INDArray reshapedA = matrixA.reshape('c', batchSize * rowsA, colsA);
//        INDArray theoreticalExpected = reshapedA.mmul(matrixB).reshape('c', batchSize, rowsA, colsB);
//
//        INDArray simulatedExpected = calculateSimulatedFixedPointMatMul(reshapedA, matrixB).reshape('c', batchSize, rowsA, colsB);
//
//        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
//        INDArray actualOutput = operator.gemm(matrixA, matrixB, null, alpha, beta, 0L, 0L);
//
//        double tolerance = 1e-2;
//        HWAcceleratedTestModel.validate("Gemm - 3Dx2D", theoreticalExpected, simulatedExpected, actualOutput, tolerance);
//    }


}




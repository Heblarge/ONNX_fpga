package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Test;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.List;
import java.util.Arrays;


public class HWAcceleratedGemmV13Test extends HWAcceleratedTestCase {

    private INDArray calculateSimulatedMatmulWithAlpha(
            INDArray A, INDArray B, float alpha,
            List<Long> fpgaInShift, List<Long> fpgaOutShift
    ) {
        int rankA = A.rank();
        int rankB = B.rank();

        if (rankA > 2 && rankB == 2) { // 3D x 2D case
            long M = A.size(rankA - 2);
            long K = A.size(rankA - 1);
            long numBatches = A.length() / (M * K);
            INDArray a2D = A.reshape('c', numBatches * M, K);
            INDArray result2D = calculateSimulatedMatmulWithAlpha(a2D, B, alpha, fpgaInShift, fpgaOutShift);

            long[] outputShape = Arrays.copyOf(A.shape(), (int)rankA);
            outputShape[(int)rankA - 1] = B.size(1);
            return result2D.reshape('c', outputShape);

        } else if (rankA == 3 && rankB == 3) {
            long batchSize = A.size(0);
            INDArray result = Nd4j.create(DataType.LONG, batchSize, A.size(1), B.size(2));
            for (int i = 0; i < batchSize; i++) {
                INDArray sliceResult = calculateSimulatedMatmulWithAlpha(
                        A.slice(i, 0), B.slice(i, 0), alpha, fpgaInShift, fpgaOutShift);
                result.putSlice(i, sliceResult);
            }
            return result;
        }

        int m = (int) A.rows();
        int k = (int) A.columns();
        int n = (int) B.columns();

        long[][] matA_long = new long[m][k];
        for (int i = 0; i < m; i++) for (int j = 0; j < k; j++) matA_long[i][j] = A.getInt(i, j);

        long[][] matB_long = new long[k][n];
        for (int i = 0; i < k; i++) for (int j = 0; j < n; j++) matB_long[i][j] = B.getInt(i, j);

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

        long shiftAmount = fpgaInShift.get(0) + fpgaInShift.get(1) - fpgaOutShift.get(0);
        long[][] shiftedResult_long = new long[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                shiftedResult_long[i][j] = (shiftAmount >= 0)
                        ? (matMulResult_long[i][j] >> shiftAmount)
                        : (matMulResult_long[i][j] << -shiftAmount);
            }
        }

        float[] outputFloat = new float[m * n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                outputFloat[i * n + j] = (float) shiftedResult_long[i][j];
            }
        }
        INDArray Y = Nd4j.create(outputFloat, new long[]{m, n});

        Y.muli(alpha);

        return Y;
    }

    @Test
    public void testGemmSimple() throws Exception {
        System.out.println("\n--- Testing Gemm simple case (Y = alpha * A * B) ---");
        int rowsA = 32;
        int colsA = 32;
        int colsB = 16;
        float minValue = -5f;
        float maxValue = 5f;

        float alpha = 2.0f;

        List<Long> fpgaInShift = Arrays.asList(20L, 20L);
        List<Long> fpgaOutShift =  Arrays.asList(20L);

        INDArray matrixA = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rowsA, colsA, minValue, maxValue, fpgaInShift.get(0)));
        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsA, colsB, minValue, maxValue, fpgaInShift.get(1)));

        long shiftAmount = fpgaInShift.get(0) + fpgaInShift.get(1) - fpgaOutShift.get(0);
        INDArray integerMatMul = matrixA.mmul(matrixB);
        INDArray theoreticalExpected = (shiftAmount >= 0)
                ? integerMatMul.div(1L << shiftAmount)
                : integerMatMul.mul(1L << -shiftAmount);
        theoreticalExpected.muli(alpha);

        INDArray simulatedExpected = calculateSimulatedMatmulWithAlpha(matrixA, matrixB, alpha, fpgaInShift, fpgaOutShift);

        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
        INDArray actualOutput = operator.gemm(matrixA, matrixB, null, alpha, 1.0f, 0L, 0L, fpgaInShift, fpgaOutShift);

        HWAcceleratedTestModel.validate("Gemm - Simple", theoreticalExpected, simulatedExpected, actualOutput, 0.0);
    }

    @Test
    public void testGemm3D() throws Exception {
        System.out.println("\n--- Testing Gemm 3D x 3D (Batched MatMul) ---");
        int batchSize = 4;
        int rowsA = 32;   // M
        int colsA = 64;   // K
        int colsB = 16;   // N
        float minValue = -5f;
        float maxValue = 5f;

        float alpha = 1.0f;

        List<Long> fpgaInShift = Arrays.asList(20L, 20L);
        List<Long> fpgaOutShift = Arrays.asList(25L);

        INDArray matrixA_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rowsA, colsA, minValue, maxValue, fpgaInShift.get(0));
        INDArray matrixB_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, colsA, colsB, minValue, maxValue, fpgaInShift.get(1));

        long shiftAmount = fpgaInShift.get(0) + fpgaInShift.get(1) - fpgaOutShift.get(0);
        INDArray theoreticalExpected = Nd4j.create(batchSize, rowsA, colsB);
        for (int i = 0; i < batchSize; i++) {
            INDArray sliceA = matrixA_int.slice(i, 0);
            INDArray sliceB = matrixB_int.slice(i, 0);
            INDArray integerMatMulSlice = sliceA.mmul(sliceB);
            INDArray theoreticalSlice = (shiftAmount >= 0)
                    ? integerMatMulSlice.div(1L << shiftAmount)
                    : integerMatMulSlice.mul(1L << -shiftAmount);
            theoreticalSlice.muli(alpha);
            theoreticalExpected.putSlice(i, theoreticalSlice);
        }

        INDArray simulatedExpected = calculateSimulatedMatmulWithAlpha(matrixA_int, matrixB_int, alpha, fpgaInShift, fpgaOutShift);

        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
        INDArray actualOutput = operator.gemm(matrixA_int, matrixB_int, null, alpha, 1.0f, 0L, 0L, fpgaInShift, fpgaOutShift);

        double tolerance = 1.0 / Math.pow(2, fpgaOutShift.get(0));
        HWAcceleratedTestModel.validate("Gemm - 3D", theoreticalExpected, simulatedExpected, actualOutput, tolerance);
    }
}
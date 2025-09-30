package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.List;
import java.util.Arrays;
import org.nd4j.linalg.api.buffer.DataType;

/**
 * This test class provides validation for the HWAcceleratedMatMulV13 operator,
 * assuming pre-quantized int32 inputs and using a precise fixed-point simulation.
 */
public class HWAcceleratedMatMulV13Test extends HWAcceleratedTestCase {

    private INDArray calculateSimulatedFixedPointMatMul(
            INDArray A_int, INDArray B_int,
            List<Long> fpgaInShift, Long fpgaOutShift
    ) {
        int rankA = A_int.rank();
        int rankB = B_int.rank();

        if (rankA > 2 && rankB == 2) { // 3D x 2D case
            long M = A_int.size(rankA - 2);
            long K = A_int.size(rankA - 1);
            long numBatches = A_int.length() / (M * K);
            INDArray a2D = A_int.reshape('c', numBatches * M, K);
            INDArray result2D = calculateSimulatedFixedPointMatMul(a2D, B_int, fpgaInShift, fpgaOutShift);

            long[] outputShape = Arrays.copyOf(A_int.shape(), (int)rankA);
            outputShape[(int)rankA - 1] = B_int.size(1);
            return result2D.reshape('c', outputShape);

        } else if (rankA == 3 && rankB == 3) {
            long batchSize = A_int.size(0);
            INDArray result = Nd4j.create(DataType.LONG, batchSize, A_int.size(1), B_int.size(2));
            for (int i = 0; i < batchSize; i++) {
                INDArray sliceResult = calculateSimulatedFixedPointMatMul(
                        A_int.slice(i, 0), B_int.slice(i, 0), fpgaInShift, fpgaOutShift);
                result.putSlice(i, sliceResult);
            }
            return result;
        }

        int m = (int) A_int.rows();
        int k = (int) A_int.columns();
        int n = (int) B_int.columns();

        long[][] matA_long = new long[m][k];
        for (int i = 0; i < m; i++) for (int j = 0; j < k; j++) matA_long[i][j] = A_int.getInt(i, j);

        long[][] matB_long = new long[k][n];
        for (int i = 0; i < k; i++) for (int j = 0; j < n; j++) matB_long[i][j] = B_int.getInt(i, j);

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

        float[] flatResult = new float[m * n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                flatResult[i * n + j] = shiftedResult_long[i][j];
            }
        }
        return Nd4j.create(flatResult, new long[]{m, n});
    }

    @Test
    public void testMatMul2DWithQuantization() throws Exception {
        System.out.println("\n--- Testing 2D MatMul with Quantization Params ---");

        int rowsA = 32;
        int colsA = 32;
        int colsB = 32;
        float minValue = -5f;
        float maxValue = 5f;

        List<Long> fpgaInShift = Arrays.asList(10L, 10L); // Input fractional bits
        Long fpgaOutShift = 20L;                          // Target output fractional bits
        long shift1 = fpgaInShift.get(0);
        long shift2 = fpgaInShift.get(1);

        INDArray matrixA = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rowsA, colsA, minValue, maxValue, shift1));
        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsA, colsB, minValue, maxValue, shift2));

        long shiftAmount = fpgaInShift.get(0) + fpgaInShift.get(1) - fpgaOutShift;

        INDArray theoreticalExpected = matrixA.mmul(matrixB).div(1L << shiftAmount);
        INDArray simulatedExpected = calculateSimulatedFixedPointMatMul(matrixA, matrixB, fpgaInShift, fpgaOutShift);

        HWAcceleratedMatMulV13 operator = new HWAcceleratedMatMulV13();
        INDArray actualOutput = operator.matmul(matrixA, matrixB, fpgaInShift, fpgaOutShift);

        HWAcceleratedTestModel.validate("MatMul - 2D Quantized", theoreticalExpected, simulatedExpected, actualOutput, 0.0);
    }

    @Test
    public void testMatMul3Dx2D() throws Exception {
        System.out.println("\n--- Testing 3D x 2D MatMul with Quantization Params ---");

        int batchSize = 4;
        int rowsA = 32; // M dimension
        int colsA = 64; // K dimension
        int colsB = 16; // N dimension
        float minValue = -5f;
        float maxValue = 5f;

        List<Long> fpgaInShift = Arrays.asList(10L, 10L);
        Long fpgaOutShift = 20L;
        long shift1 = fpgaInShift.get(0);
        long shift2 = fpgaInShift.get(1);

        INDArray matrixA = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rowsA, colsA, minValue, maxValue, shift1);
        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsA, colsB, minValue, maxValue, shift2));
        long shiftAmount = fpgaInShift.get(0) + fpgaInShift.get(1) - fpgaOutShift;
        // INDArray theoreticalExpected = matrixA.mmul(matrixB).div(1L << shiftAmount);
        INDArray expectedOutput = calculateSimulatedFixedPointMatMul(matrixA, matrixB, fpgaInShift, fpgaOutShift);

        HWAcceleratedMatMulV13 operator = new HWAcceleratedMatMulV13();
        INDArray actualOutput = operator.matmul(matrixA, matrixB, fpgaInShift, fpgaOutShift);

        HWAcceleratedTestModel.validate("MatMul - 3Dx2D Quantized", expectedOutput, expectedOutput, actualOutput, 0.0);
    }

    @Test
    public void testMatMul3Dx3D() throws Exception {
        System.out.println("\n--- Testing 3D x 3D MatMul with Quantization Params ---");

        int batchSize = 4;
        int rowsA = 32;
        int colsA = 64;
        int colsB = 16;
        float minValue = -5f;
        float maxValue = 5f;

        List<Long> fpgaInShift = Arrays.asList(10L, 13L);
        Long fpgaOutShift = 20L;
        long shift1 = fpgaInShift.get(0);
        long shift2 = fpgaInShift.get(1);

        INDArray matrixA = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rowsA, colsA, minValue, maxValue, shift1);
        INDArray matrixB = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, colsA, colsB, minValue, maxValue, shift2);
        long shiftAmount = fpgaInShift.get(0) + fpgaInShift.get(1) - fpgaOutShift;
        INDArray theoreticalExpected = Nd4j.create(DataType.FLOAT, batchSize, rowsA, colsB);

        for (int i = 0; i < batchSize; i++) {
            INDArray sliceA_float = matrixA.slice(i, 0);
            INDArray sliceB_float = matrixB.slice(i, 0);

            INDArray productSlice_float = sliceA_float.mmul(sliceB_float).div(Math.pow(2, shiftAmount));

            theoreticalExpected.putSlice(i, productSlice_float);
        }
        INDArray expectedOutput = calculateSimulatedFixedPointMatMul(matrixA, matrixB, fpgaInShift, fpgaOutShift);

        HWAcceleratedMatMulV13 operator = new HWAcceleratedMatMulV13();
        INDArray actualOutput = operator.matmul(matrixA, matrixB, fpgaInShift, fpgaOutShift);

        HWAcceleratedTestModel.validate("MatMul - 3Dx3D Quantized", theoreticalExpected, expectedOutput, actualOutput, 0.0);
    }

}
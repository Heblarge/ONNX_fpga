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
 * MODIFIED: Now reflects the quantization logic with source/target shifts.
 */
public class HWAcceleratedMatMulV13Test extends HWAcceleratedTestCase {

    private INDArray calculateSimulatedFixedPointMatMul(
            INDArray A_int, INDArray B_int,
            long sourceShiftA, long sourceShiftB,
            long targetInputShiftA, long targetInputShiftB,
            long targetOutputShift
    ) {
        int rankA = A_int.rank();
        int rankB = B_int.rank();

        if (rankA > 2 && rankB == 2) {
            long M = A_int.size(rankA - 2);
            long K = A_int.size(rankA - 1);
            long numBatches = A_int.length() / (M * K);
            INDArray a2D = A_int.reshape('c', numBatches * M, K);
            INDArray result2D = calculateSimulatedFixedPointMatMul(
                    a2D, B_int,
                    sourceShiftA, sourceShiftB,
                    targetInputShiftA, targetInputShiftB,
                    targetOutputShift
            );

            long[] outputShape = Arrays.copyOf(A_int.shape(), rankA);
            outputShape[rankA - 1] = B_int.size(1);
            return result2D.reshape('c', outputShape);

        } else if (rankA == 3 && rankB == 3) {
            long batchSize = A_int.size(0);
            INDArray result = Nd4j.create(DataType.LONG, batchSize, A_int.size(1), B_int.size(2));
            for (int i = 0; i < batchSize; i++) {
                INDArray sliceResult = calculateSimulatedFixedPointMatMul(
                        A_int.slice(i), B_int.slice(i),
                        sourceShiftA, sourceShiftB,
                        targetInputShiftA, targetInputShiftB,
                        targetOutputShift
                );
                result.putSlice(i, sliceResult);
            }
            return result;
        } else if (rankA != 2 || rankB != 2) {
            throw new IllegalArgumentException("Unsupported ranks for simulation: A=" + rankA + ", B=" + rankB);
        }

        int m = (int) A_int.rows();
        int k = (int) A_int.columns();
        int n = (int) B_int.columns();

        int rescaleShiftA = (int) (sourceShiftA - targetInputShiftA);
        int rescaleShiftB = (int) (sourceShiftB - targetInputShiftB);
        int hardwareShiftAmount = (int) (targetInputShiftA + targetInputShiftB - targetOutputShift);

        long[][] fixedPointA = new long[m][k];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < k; j++) {
                long valA = A_int.getLong(i, j);
                fixedPointA[i][j] = (rescaleShiftA < 0) ? (valA << -rescaleShiftA) : (valA >> rescaleShiftA);
            }
        }

        long[][] fixedPointB = new long[k][n];
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < n; j++) {
                long valB = B_int.getLong(i, j);
                fixedPointB[i][j] = (rescaleShiftB < 0) ? (valB << -rescaleShiftB) : (valB >> rescaleShiftB);
            }
        }

        long[][] matMulResult_long = new long[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                long accumulator = 0L;
                for (int l = 0; l < k; l++) {
                    accumulator += fixedPointA[i][l] * fixedPointB[l][j];
                }
                matMulResult_long[i][j] = accumulator;
            }
        }

        long[][] shiftedResult_long = new long[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                long val = matMulResult_long[i][j];
                shiftedResult_long[i][j] = (hardwareShiftAmount < 0) ? (val << -hardwareShiftAmount) : (val >> hardwareShiftAmount);
            }
        }

        long[] flatResultLong = new long[m * n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                flatResultLong[i * n + j] = shiftedResult_long[i][j];
            }
        }
        return Nd4j.create(flatResultLong, new long[]{m, n}, A_int.dataType());
    }


    @Test
    public void testMatMul2DWithQuantization() throws Exception {
        System.out.println("\n--- Testing 2D MatMul with Quantization Params ---");

        int rowsA = 256;
        int colsA = 1024;
        int colsB = 560;
        float minValue = -5f;
        float maxValue = 5f;

        long sourceShiftA = 25L;
        long sourceShiftB = 25L;
        long targetInputShiftA = 25L;
        long targetInputShiftB = 25L;
        long targetOutputShift = 23L;

        INDArray matrixA = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rowsA, colsA, minValue, maxValue, sourceShiftA));
        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsA, colsB, minValue, maxValue, sourceShiftB));

        long theoreticalShiftAmount = sourceShiftA + sourceShiftB - targetOutputShift;
        INDArray theoreticalExpected = matrixA.mmul(matrixB);
        theoreticalExpected = (theoreticalShiftAmount < 0)
                ? theoreticalExpected.mul(1L << -theoreticalShiftAmount)
                : theoreticalExpected.div(1L << theoreticalShiftAmount);

        INDArray simulatedExpected = calculateSimulatedFixedPointMatMul(
                matrixA, matrixB,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift
        );

        HWAcceleratedMatMulV13 operator = new HWAcceleratedMatMulV13();

        INDArray actualOutput = operator.matmul(
                matrixA, matrixB,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift,
                "testMatMul2DWithQuantization"
        );

        HWAcceleratedTestModel.validate("MatMul - 2D Quantized", theoreticalExpected, simulatedExpected, actualOutput, 0.0);
    }

    @Test
    public void testMatMul3Dx2D() throws Exception {
        System.out.println("\n--- Testing 3D x 2D MatMul with Quantization Params ---");

        int batchSize = 4;
        int rowsA = 32;
        int colsA = 64;
        int colsB = 16;
        float minValue = -5f;
        float maxValue = 5f;

        long sourceShiftA = 10L;
        long sourceShiftB = 10L;
        long targetInputShiftA = 12L;
        long targetInputShiftB = 12L;
        long targetOutputShift = 20L;

        INDArray matrixA = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rowsA, colsA, minValue, maxValue, sourceShiftA);
        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsA, colsB, minValue, maxValue, sourceShiftB));

        INDArray simulatedExpected = calculateSimulatedFixedPointMatMul(
                matrixA, matrixB,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift
        );

        HWAcceleratedMatMulV13 operator = new HWAcceleratedMatMulV13();

        INDArray actualOutput = operator.matmul(
                matrixA, matrixB,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift,
                "testMatMul3Dx2D"
        );

        HWAcceleratedTestModel.validate("MatMul - 3Dx2D Quantized", simulatedExpected, simulatedExpected, actualOutput, 0.0);
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

        long sourceShiftA = 10L;
        long sourceShiftB = 13L;
        long targetInputShiftA = 10L;
        long targetInputShiftB = 10L;
        long targetOutputShift = 20L;

        INDArray matrixA = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rowsA, colsA, minValue, maxValue, sourceShiftA);
        INDArray matrixB = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, colsA, colsB, minValue, maxValue, sourceShiftB);

        INDArray simulatedExpected = calculateSimulatedFixedPointMatMul(
                matrixA, matrixB,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift
        );

        HWAcceleratedMatMulV13 operator = new HWAcceleratedMatMulV13();

        INDArray actualOutput = operator.matmul(
                matrixA, matrixB,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift,
                "testMatMul3Dx3D"
        );

        HWAcceleratedTestModel.validate("MatMul - 3Dx3D Quantized", simulatedExpected, simulatedExpected, actualOutput, 0.0);
    }
}
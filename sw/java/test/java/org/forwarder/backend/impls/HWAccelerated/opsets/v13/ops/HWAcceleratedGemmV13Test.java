//package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;
//
//import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
//import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
//import org.junit.Test;
//import org.nd4j.linalg.api.buffer.DataType;
//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.factory.Nd4j;
//import java.util.List;
//import java.util.Arrays;
//
//
//public class HWAcceleratedGemmV13Test extends HWAcceleratedTestCase {
//
//    private INDArray calculateSimulatedFixedPointGemmAsMatMul(
//            INDArray A_int, INDArray B_int,
//            long sourceShiftA, long sourceShiftB,
//            long targetInputShiftA, long targetInputShiftB,
//            long targetOutputShift
//    ) {
//        int rankA = A_int.rank();
//        int rankB = B_int.rank();
//
//        if (rankA > 2 && rankB == 2) {
//            long M = A_int.size(rankA - 2);
//            long K = A_int.size(rankA - 1);
//            long numBatches = A_int.length() / (M * K);
//            INDArray a2D = A_int.reshape('c', numBatches * M, K);
//            INDArray result2D = calculateSimulatedFixedPointGemmAsMatMul(
//                    a2D, B_int, sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift);
//            long[] outputShape = Arrays.copyOf(A_int.shape(), rankA);
//            outputShape[rankA - 1] = B_int.size(1);
//            return result2D.reshape('c', outputShape);
//        } else if (rankA == 3 && rankB == 3) {
//            long batchSize = A_int.size(0);
//            INDArray result = Nd4j.create(DataType.LONG, batchSize, A_int.size(1), B_int.size(2));
//            for (int i = 0; i < batchSize; i++) {
//                INDArray sliceResult = calculateSimulatedFixedPointGemmAsMatMul(
//                        A_int.slice(i), B_int.slice(i),
//                        sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift);
//                result.putSlice(i, sliceResult);
//            }
//            return result;
//        } else if (rankA != 2 || rankB != 2) {
//            throw new IllegalArgumentException("Unsupported ranks for simulation: A=" + rankA + ", B=" + rankB);
//        }
//
//        int m = (int) A_int.rows();
//        int k = (int) A_int.columns();
//        int n = (int) B_int.columns();
//
//        int rescaleShiftA = (int) (sourceShiftA - targetInputShiftA);
//        int rescaleShiftB = (int) (sourceShiftB - targetInputShiftB);
//        int hardwareShiftAmount = (int) (targetInputShiftA + targetInputShiftB - targetOutputShift);
//
//        long[][] fixedPointA = new long[m][k];
//        for (int i = 0; i < m; i++) for (int j = 0; j < k; j++) {
//            long valA = A_int.getLong(i, j);
//            fixedPointA[i][j] = (rescaleShiftA < 0) ? (valA << -rescaleShiftA) : (valA >> rescaleShiftA);
//        }
//        long[][] fixedPointB = new long[k][n];
//        for (int i = 0; i < k; i++) for (int j = 0; j < n; j++) {
//            long valB = B_int.getLong(i, j);
//            fixedPointB[i][j] = (rescaleShiftB < 0) ? (valB << -rescaleShiftB) : (valB >> rescaleShiftB);
//        }
//
//        long[][] matMulResult_long = new long[m][n];
//        for (int i = 0; i < m; i++) for (int j = 0; j < n; j++) {
//            long accumulator = 0L;
//            for (int l = 0; l < k; l++) accumulator += fixedPointA[i][l] * fixedPointB[l][j];
//            matMulResult_long[i][j] = accumulator;
//        }
//
//        long[][] shiftedResult_long = new long[m][n];
//        for (int i = 0; i < m; i++) for (int j = 0; j < n; j++) {
//            long val = matMulResult_long[i][j];
//            shiftedResult_long[i][j] = (hardwareShiftAmount < 0) ? (val << -hardwareShiftAmount) : (val >> hardwareShiftAmount);
//        }
//
//        long[] flatResultLong = new long[m * n];
//        for (int i = 0; i < m; i++) for (int j = 0; j < n; j++) flatResultLong[i * n + j] = shiftedResult_long[i][j];
//        return Nd4j.create(flatResultLong, new long[]{m, n}, A_int.dataType());
//    }
//
//    @Test
//    public void testGemmSimple() throws Exception {
//        System.out.println("\n--- Testing Gemm simple case (as MatMul) ---"); // MODIFIED
//        int rowsA = 32;
//        int colsA = 32;
//        int colsB = 16;
//        float minValue = -5f;
//        float maxValue = 5f;
//
//        long sourceShiftA = 20L;
//        long sourceShiftB = 20L;
//        long targetInputShiftA = 20L;
//        long targetInputShiftB = 20L;
//        long targetOutputShift = 20L;
//
//        INDArray matrixA = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rowsA, colsA, minValue, maxValue, sourceShiftA));
//        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsA, colsB, minValue, maxValue, sourceShiftB));
//
//        long theoreticalShiftAmount = sourceShiftA + sourceShiftB - targetOutputShift;
//        INDArray integerMatMul = matrixA.mmul(matrixB);
//        INDArray theoreticalExpected = (theoreticalShiftAmount >= 0)
//                ? integerMatMul.div(1L << theoreticalShiftAmount)
//                : integerMatMul.mul(1L << -theoreticalShiftAmount);
//
//        INDArray simulatedExpected = calculateSimulatedFixedPointGemmAsMatMul(
//                matrixA, matrixB,
//                sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift
//        );
//
//        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
//
//        INDArray actualOutput = operator.gemm(
//                matrixA, matrixB, null, 1.0f, 1.0f, 0L, 0L,
//                sourceShiftA, sourceShiftB,
//                targetInputShiftA, targetInputShiftB,
//                targetOutputShift
//        );
//
//        HWAcceleratedTestModel.validate("Gemm - Simple (as MatMul)", theoreticalExpected, simulatedExpected, actualOutput, 0.0); // MODIFIED: Compare sim vs actual
//    }
//
//    @Test
//    public void testGemm3D() throws Exception {
//        System.out.println("\n--- Testing Gemm 3D x 3D (as Batched MatMul) ---"); // MODIFIED
//        int batchSize = 4;
//        int rowsA = 32;   // M
//        int colsA = 64;   // K
//        int colsB = 16;   // N
//        float minValue = -5f;
//        float maxValue = 5f;
//
//
//        long sourceShiftA = 20L;
//        long sourceShiftB = 20L;
//        long targetInputShiftA = 20L;
//        long targetInputShiftB = 20L;
//        long targetOutputShift = 25L;
//
//        INDArray matrixA_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rowsA, colsA, minValue, maxValue, sourceShiftA);
//        INDArray matrixB_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, colsA, colsB, minValue, maxValue, sourceShiftB);
//
//        INDArray simulatedExpected = calculateSimulatedFixedPointGemmAsMatMul(
//                matrixA_int, matrixB_int,
//                sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift
//        );
//
//        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
//
//        INDArray actualOutput = operator.gemm(
//                matrixA_int, matrixB_int, null, 1.0f, 1.0f, 0L, 0L, // C=null, alpha=1, beta=1, trans=0
//                sourceShiftA, sourceShiftB,
//                targetInputShiftA, targetInputShiftB,
//                targetOutputShift
//        );
//
//        HWAcceleratedTestModel.validate("Gemm - 3D (as MatMul)", simulatedExpected, simulatedExpected, actualOutput, 0.0); // MODIFIED
//    }
//}
package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test class provides comprehensive validation for the HWAcceleratedGemmV13 operator,
 * using a precise fixed-point simulation for accurate result verification.
 */
public class HWAcceleratedGemmV13Test extends HWAcceleratedTestCase {

    // Using smaller, more reasonable dimensions for unit testing
    private final int rowsA = 32;
    private final int colsA = 32;
    private final int colsB = 1024;
    private final float minValue = -5f;
    private final float maxValue = 5f;

    /**
     * Simulates the exact fixed-point arithmetic of the core MatMul hardware operator.
     */
    private INDArray calculateSimulatedFixedPointMatMul(INDArray a, INDArray b) {
        int fracWidth = 9; // Must match the value in the operator
        double scaleFactor = Math.pow(2, fracWidth);
        int m = (int) a.size(0);
        int k = (int) a.size(1);
        int n = (int) b.size(1);

        int[][] fixedPointA = new int[m][k];
        int[][] fixedPointB = new int[k][n];
        for (int i = 0; i < m; i++) for (int j = 0; j < k; j++) fixedPointA[i][j] = (int)Math.round(a.getFloat(i, j) * scaleFactor);
        for (int i = 0; i < k; i++) for (int j = 0; j < n; j++) fixedPointB[i][j] = (int)Math.round(b.getFloat(i, j) * scaleFactor);

        int[][] fixedPointOutput = new int[m][n];
        long[][] fixedPointOutputLong = new long[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                for (int l = 0; l < k; l++) {
                    // 使用 long 进行乘法以避免 int 溢出
                    fixedPointOutputLong[i][j] += (long)fixedPointA[i][l] * fixedPointB[l][j];
                }
            }
        }

        float[] output = new float[m * n];
        double finalScaleFactor = scaleFactor * scaleFactor;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                // 从 long 类型的累加器中读取结果
                output[i * n + j] = (float) (fixedPointOutputLong[i][j] / finalScaleFactor);
            }
        }
        return Nd4j.create(output).reshape(m, n);
    }

    private INDArray calculateSimulatedFixedPointAdd(INDArray a, INDArray b) {
        int fracWidth = 9; // Must match the value in the Add operator
        double scaleFactor = Math.pow(2, fracWidth);
        int rows = (int) a.rows();
        int cols = (int) a.columns();

        int[][] fixedPointA = new int[rows][cols];
        int[][] fixedPointB = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                fixedPointA[i][j] = (int)Math.round(a.getFloat(i, j) * scaleFactor);
                fixedPointB[i][j] = (int)Math.round(b.getFloat(i, j) * scaleFactor);
            }
        }

        int[][] fixedPointOutput = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                fixedPointOutput[i][j] = fixedPointA[i][j] + fixedPointB[i][j];
            }
        }

        float[] output = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                output[i * cols + j] = (float)(((double)(fixedPointOutput[i][j])) / scaleFactor);
            }
        }
        return Nd4j.create(output).reshape(rows, cols);
    }

    /**
     * Simulates the entire Gemm operation, now including the fixed-point Add simulation for C.
     */
    private INDArray calculateSimulatedFixedPointGemm(INDArray A, INDArray B, INDArray C, float alpha, float beta, long transA, long transB) {
        if (transA != 0L) { A = A.transpose(); }
        if (transB != 0L) { B = B.transpose(); }

        INDArray Y = calculateSimulatedFixedPointMatMul(A, B);
        Y.muli(alpha);

        if (C != null) {
            long[] yShape = Y.shape();
            if (!java.util.Arrays.equals(C.shape(), yShape)) {
                C = C.broadcast(yShape);
            }
            INDArray betaC = C.mul(beta);

            // MODIFIED: Use the fixed-point add simulation
            Y = calculateSimulatedFixedPointAdd(Y, betaC);
        }
        return Y;
    }


    /**
     * Simulates the entire Gemm operation (alpha * A'B' + beta * C) using the fixed-point MatMul simulation.
     */

    @Test
    public void testGemmWithAllParameters() throws Exception {
        System.out.println("\n--- Testing Gemm with all parameters (Y = alpha * A'* B' + beta * C) ---");
        float alpha = 0.5f;
        float beta = 2.0f;
        INDArray matrixA = Nd4j.create( HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsA, rowsA, minValue, maxValue));
        INDArray matrixB = Nd4j.create( HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsB, colsA, minValue, maxValue));
        INDArray matrixC = Nd4j.create( HWAcceleratedTestModel.generateRandom2DFloatMatrix(rowsA, colsB, minValue, maxValue));

        INDArray theoreticalExpected = matrixA.transpose().mmul(matrixB.transpose()).mul(alpha).add(matrixC.mul(beta));
        INDArray simulatedExpected = calculateSimulatedFixedPointGemm(matrixA, matrixB, matrixC, alpha, beta, 1L, 1L);

        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
        INDArray actualOutput = operator.gemm(matrixA, matrixB, matrixC, alpha, beta, 1L, 1L);

        double hardwareLogicTolerance = 1e-3;
        HWAcceleratedTestModel.validate("Gemm", theoreticalExpected, simulatedExpected, actualOutput, hardwareLogicTolerance);
    }


}


//import org.nd4j.linalg.ops.transforms.Transforms;
//
//
//public class HWAcceleratedGemmV13Test extends HWAcceleratedTestCase {
//
//    private final int rowsA = 32;
//    private final int colsA = 32;
//    private final int colsB = 1024;
//    private final float minValue = -5f;
//    private final float maxValue = 5f;
//
//
//    private INDArray calculateSimulatedFixedPointMatMul(INDArray a, INDArray b, int fracWidthA, int fracWidthB, int maxWidth) {
//
//        int m = a.rows();
//        int k = a.columns();
//        int n = b.columns();
//
//        long scaleFactorA = 1L << fracWidthA;
//        long scaleFactorB = 1L << fracWidthB;
//
//        // 最大绝对值
//        double maxAbsA = Transforms.abs(a, true).maxNumber().doubleValue();
//        double maxAbsB = Transforms.abs(b, true).maxNumber().doubleValue();
//
//        // 计算动态 extraShift
//        int extraShiftA = Math.max(0,
//                maxWidth - 1 - (int) Math.ceil(Math.log(Math.max(1.0, maxAbsA * scaleFactorA)) / Math.log(2)));
//        int extraShiftB = Math.max(0,
//                maxWidth - 1 - (int) Math.ceil(Math.log(Math.max(1.0, maxAbsB * scaleFactorB)) / Math.log(2)));
//
//        long[][] fixedA = new long[m][k];
//        long[][] fixedB = new long[k][n];
//
//        for (int i = 0; i < m; i++)
//            for (int j = 0; j < k; j++)
//                fixedA[i][j] = Math.round(a.getDouble(i, j) * scaleFactorA) << extraShiftA;
//
//        for (int i = 0; i < k; i++)
//            for (int j = 0; j < n; j++)
//                fixedB[i][j] = Math.round(b.getDouble(i, j) * scaleFactorB) << extraShiftB;
//
//        // GEMM
//        double[][] output = new double[m][n];
//        for (int i = 0; i < m; i++) {
//            for (int j = 0; j < n; j++) {
//                long acc = 0;
//                for (int p = 0; p < k; p++) {
//                    acc += fixedA[i][p] * fixedB[p][j];
//                }
//                double finalScale = (double) (scaleFactorA * scaleFactorB) * (1L << (extraShiftA + extraShiftB));
//                output[i][j] = acc / finalScale;
//            }
//        }
//
//        return Nd4j.create(output);
//    }
//
//    /**
//     * 模拟定点加法
//     */
//    private INDArray calculateSimulatedFixedPointAdd(INDArray a, INDArray b, int fracWidth) {
//        double scaleFactor = Math.pow(2, fracWidth);
//        int rows = a.rows();
//        int cols = a.columns();
//
//        int[][] fixedA = new int[rows][cols];
//        int[][] fixedB = new int[rows][cols];
//
//        for (int i = 0; i < rows; i++)
//            for (int j = 0; j < cols; j++) {
//                fixedA[i][j] = (int) Math.round(a.getDouble(i, j) * scaleFactor);
//                fixedB[i][j] = (int) Math.round(b.getDouble(i, j) * scaleFactor);
//            }
//
//        int[][] fixedOut = new int[rows][cols];
//        for (int i = 0; i < rows; i++)
//            for (int j = 0; j < cols; j++)
//                fixedOut[i][j] = fixedA[i][j] + fixedB[i][j];
//
//        double[] output = new double[rows * cols];
//        for (int i = 0; i < rows; i++)
//            for (int j = 0; j < cols; j++)
//                output[i * cols + j] = fixedOut[i][j] / scaleFactor;
//
//        return Nd4j.create(output).reshape(rows, cols);
//    }
//
//    /**
//     * 模拟 GEMM = αAB + βC
//     */
//    private INDArray calculateSimulatedFixedPointGemm(INDArray A, INDArray B, INDArray C, float alpha, float beta, long transA, long transB) {
//        if (transA != 0L) A = A.transpose();
//        if (transB != 0L) B = B.transpose();
//
//        INDArray Y = calculateSimulatedFixedPointMatMul(A, B, 22, 20, 27);
//        Y.muli(alpha);
//
//        if (C != null) {
//            if (!java.util.Arrays.equals(C.shape(), Y.shape())) {
//                C = C.broadcast(Y.shape());
//            }
//            INDArray betaC = C.mul(beta);
//            Y = calculateSimulatedFixedPointAdd(Y, betaC, 9);
//        }
//
//        return Y;
//    }
//
//    /**
//     * 误差评估
//     */
//    private void evaluateError(INDArray ref, INDArray test) {
//        INDArray diff = ref.sub(test);
//        double mse = diff.mul(diff).meanNumber().doubleValue();
//        double rmse = Math.sqrt(mse);
//        double norm = ref.mul(ref).meanNumber().doubleValue();
//        double nrmse = rmse / Math.sqrt(norm);
//        double maxAbsDiff = Transforms.abs(diff, true).maxNumber().doubleValue();
//
//        System.out.printf("[ErrorEval] RMSE=%.6e, NRMSE=%.6e, MaxAbsDiff=%.6e%n",
//                rmse, nrmse, maxAbsDiff);
//
//        assertTrue("NRMSE too large: " + nrmse, nrmse < 1e-2);
//    }
//    private void evaluateAndPrint(INDArray ref, INDArray test, double tolerance) {
//        int rows = (int) ref.rows();
//        int cols = (int) ref.columns();
//
//        System.out.println("Index | Expected | Actual | Diff | Pass/Fail");
//
//        int passCount = 0;
//        int total = rows * cols;
//
//        for (int i = 0; i < rows; i++) {
//            for (int j = 0; j < cols; j++) {
//                double expected = ref.getDouble(i, j);
//                double actual = test.getDouble(i, j);
//                double diff = actual - expected;
//                boolean pass = Math.abs(diff) <= tolerance;
//                if (pass) passCount++;
//
//                System.out.printf("%d,%d | %.6f | %.6f | %.6f | %s%n",
//                        i, j, expected, actual, diff, pass ? "Pass" : "Fail");
//            }
//        }
//
//        System.out.printf("Summary: Pass %d / %d, Fail %d / %d%n%n",
//                passCount, total, total - passCount, total);
//    }
//
//
//    @Test
//    public void testGemmWithAllParameters() throws Exception {
//        System.out.println("\n--- Testing Gemm with all parameters (Y = αAB + βC) ---");
//
//        float alpha = 0.5f;
//        float beta = 2.0f;
//
//        INDArray matrixA = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rowsA, colsA, minValue, maxValue));
//        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsB, colsA, minValue, maxValue));
//        INDArray matrixC = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rowsA, colsB, minValue, maxValue));
//
//        // 浮点参考结果
//        INDArray theoreticalExpected = matrixA.transpose().mmul(matrixB.transpose()).mul(alpha).add(matrixC.mul(beta));
//
//        // 固定点模拟
//        INDArray simulatedExpected = calculateSimulatedFixedPointGemm(matrixA, matrixB, matrixC, alpha, beta, 1L, 1L);
//
//        // 调用真实硬件算子
//        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
//        INDArray actualOutput = operator.gemm(matrixA, matrixB, matrixC, alpha, beta, 1L, 1L);
//
//        // 误差评估
//        double tolerance = 0.03;
//
//
//        System.out.println("--- Fixed-Point Simulation vs Theoretical ---");
//        evaluateAndPrint(simulatedExpected, theoreticalExpected, tolerance);
//
//        System.out.println("--- Hardware Output vs Theoretical ---");
//        evaluateAndPrint(actualOutput, theoreticalExpected, tolerance);
//        evaluateError(theoreticalExpected, simulatedExpected);
//        evaluateError(theoreticalExpected, actualOutput);
//    }
//}


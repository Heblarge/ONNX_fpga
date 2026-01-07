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

    private INDArray calculateSimulatedFixedPointGemmAsMatMul(
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
            INDArray result2D = calculateSimulatedFixedPointGemmAsMatMul(
                    a2D, B_int, sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift);
            long[] outputShape = Arrays.copyOf(A_int.shape(), rankA);
            outputShape[rankA - 1] = B_int.size(1);
            return result2D.reshape('c', outputShape);
        } else if (rankA == 3 && rankB == 3) {
            long batchSize = A_int.size(0);
            INDArray result = Nd4j.create(DataType.LONG, batchSize, A_int.size(1), B_int.size(2));
            for (int i = 0; i < batchSize; i++) {
                INDArray sliceResult = calculateSimulatedFixedPointGemmAsMatMul(
                        A_int.slice(i), B_int.slice(i),
                        sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift);
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
        for (int i = 0; i < m; i++) for (int j = 0; j < k; j++) {
            long valA = A_int.getLong(i, j);
            fixedPointA[i][j] = (rescaleShiftA < 0) ? (valA << -rescaleShiftA) : (valA >> rescaleShiftA);
        }
        long[][] fixedPointB = new long[k][n];
        for (int i = 0; i < k; i++) for (int j = 0; j < n; j++) {
            long valB = B_int.getLong(i, j);
            fixedPointB[i][j] = (rescaleShiftB < 0) ? (valB << -rescaleShiftB) : (valB >> rescaleShiftB);
        }

        long[][] matMulResult_long = new long[m][n];
        for (int i = 0; i < m; i++) for (int j = 0; j < n; j++) {
            long accumulator = 0L;
            for (int l = 0; l < k; l++) accumulator += fixedPointA[i][l] * fixedPointB[l][j];
            matMulResult_long[i][j] = accumulator;
        }

        long[][] shiftedResult_long = new long[m][n];
        for (int i = 0; i < m; i++) for (int j = 0; j < n; j++) {
            long val = matMulResult_long[i][j];
            shiftedResult_long[i][j] = (hardwareShiftAmount < 0) ? (val << -hardwareShiftAmount) : (val >> hardwareShiftAmount);
        }

        long[] flatResultLong = new long[m * n];
        for (int i = 0; i < m; i++) for (int j = 0; j < n; j++) flatResultLong[i * n + j] = shiftedResult_long[i][j];
        return Nd4j.create(flatResultLong, new long[]{m, n}, A_int.dataType());
    }

    @Test
    public void testGemmSimple() throws Exception {
        System.out.println("\n--- Testing Gemm simple case (as MatMul) ---"); // MODIFIED
        int rowsA = 32;
        int colsA = 32;
        int colsB = 16;
        float minValue = -5f;
        float maxValue = 5f;

        long sourceShiftA = 20L;
        long sourceShiftB = 20L;
        long targetInputShiftA = 20L;
        long targetInputShiftB = 20L;
        long targetOutputShift = 20L;

        INDArray matrixA = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rowsA, colsA, minValue, maxValue, sourceShiftA));
        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsA, colsB, minValue, maxValue, sourceShiftB));

        long theoreticalShiftAmount = sourceShiftA + sourceShiftB - targetOutputShift;
        INDArray integerMatMul = matrixA.mmul(matrixB);
        INDArray theoreticalExpected = (theoreticalShiftAmount >= 0)
                ? integerMatMul.div(1L << theoreticalShiftAmount)
                : integerMatMul.mul(1L << -theoreticalShiftAmount);

        INDArray simulatedExpected = calculateSimulatedFixedPointGemmAsMatMul(
                matrixA, matrixB,
                sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift
        );

        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();

        INDArray actualOutput = operator.gemm(
                matrixA, matrixB, null, 1.0f, 1.0f, 0L, 0L,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift,
                "testGemmSimple"
        );

        HWAcceleratedTestModel.validate("Gemm - Simple (as MatMul)", theoreticalExpected, simulatedExpected, actualOutput, 0.0); // MODIFIED: Compare sim vs actual
    }

    @Test
    public void testGemm3D() throws Exception {
        System.out.println("\n--- Testing Gemm 3D x 3D (as Batched MatMul) ---"); // MODIFIED
        int batchSize = 4;
        int rowsA = 1024;   // M
        int colsA = 512;   // K
        int colsB = 16;   // N
        float minValue = -5f;
        float maxValue = 5f;


        long sourceShiftA = 20L;
        long sourceShiftB = 20L;
        long targetInputShiftA = 20L;
        long targetInputShiftB = 20L;
        long targetOutputShift = 25L;

        INDArray matrixA_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rowsA, colsA, minValue, maxValue, sourceShiftA);
        INDArray matrixB_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, colsA, colsB, minValue, maxValue, sourceShiftB);

        INDArray simulatedExpected = calculateSimulatedFixedPointGemmAsMatMul(
                matrixA_int, matrixB_int,
                sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift
        );

        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();

        INDArray actualOutput = operator.gemm(
                matrixA_int, matrixB_int, null, 1.0f, 1.0f, 0L, 0L, // C=null, alpha=1, beta=1, trans=0
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift,
                "testGemm3D"
        );

        HWAcceleratedTestModel.validate("Gemm - 3D (as MatMul)", simulatedExpected, simulatedExpected, actualOutput, 0.0); // MODIFIED
    }

    @Test
    public void testGemm1x16x16x32() throws Exception {
        System.out.println("\n--- Testing Gemm 1x16 * 16x32 ---");

        int rowsA = 1;   // M
        int colsA = 16;  // K
        int colsB = 32;  // N
        float minValue = -5f;
        float maxValue = 5f;

        long sourceShiftA = 20L;
        long sourceShiftB = 20L;
        long targetInputShiftA = 20L;
        long targetInputShiftB = 20L;
        long targetOutputShift = 20L;

        // 生成随机输入矩阵
        INDArray matrixA = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rowsA, colsA, minValue, maxValue, sourceShiftA));
        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(colsA, colsB, minValue, maxValue, sourceShiftB));

        // 计算理论值（整数 mmul + shift）
        long theoreticalShiftAmount = sourceShiftA + sourceShiftB - targetOutputShift;
        INDArray integerMatMul = matrixA.mmul(matrixB);
        INDArray theoreticalExpected = (theoreticalShiftAmount >= 0)
                ? integerMatMul.div(1L << theoreticalShiftAmount)
                : integerMatMul.mul(1L << -theoreticalShiftAmount);

        // 模拟固定点 gemm
        INDArray simulatedExpected = calculateSimulatedFixedPointGemmAsMatMul(
                matrixA, matrixB,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift
        );

        // 调用你的 HWAccelerated Gemm 算子
        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();
        INDArray actualOutput = operator.gemm(
                matrixA, matrixB, null, 1.0f, 1.0f, 0L, 0L,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift,
                "testGemm1x16x16x32"
        );

        // 验证
        HWAcceleratedTestModel.validate(
                "Gemm 1x16 * 16x32",
                theoreticalExpected,
                simulatedExpected,
                actualOutput,
                0.0
        );
    }

    @Test
    public void testGemmFixedMatrix1x16x16x32() throws Exception {
        System.out.println("\n--- Testing Gemm with fixed 1x16 * 16x32 ---");
        long sourceShiftA = 0L;
        long sourceShiftB = 26L;
        long targetOutputShift = 26L;

        long targetInputShiftA = 0L;
        long targetInputShiftB = 26L;

        INDArray matrixA = Nd4j.create(new double[][]{
                {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0}
        }).mul(1L << sourceShiftA);



        INDArray matrixB = Nd4j.create(new double[][]{
                {0.0220, 0.1594, 0.0677, -0.2402, 0.3839, 0.3231, -0.3331, 0.3041, 0.1114, 0.0501, -0.1604, 0.0097, -0.2248, 0.2981, -0.3817, 0.4595, -0.4293, -0.1666, 0.2109, -0.0278, -0.2666, 0.0596, -0.3149, -0.1720, -0.0448, 0.1694, 0.3136, -0.2353, 0.0120, -0.2075, 0.3568, 0.2258},
                {-0.0365, 0.1792, 0.1929, 0.2242, -0.0207, -0.1017, 0.1839, -0.4153, 0.0897, -0.2213, -0.3096, 0.1113, -0.3170, 0.2612, -0.4800, -0.1618, -0.1415, -0.1821, -0.2995, 0.3181, 0.4749, -0.1718, -0.3423, -0.2440, 0.0534, 0.1601, 0.1332, 0.1403, 0.0933, -0.1728, -0.1344, 0.0735},
                {0.4145, -0.0748, -0.4486, 0.1900, -0.1077, -0.0840, 0.0468, -0.0319, -0.4690, 0.1492, 0.1677, -0.4238, -0.1089, -0.4477, 0.4394, 0.0878, 0.0959, -0.2074, 0.2811, 0.2374, 0.3340, 0.3992, -0.3324, 0.2737, -0.0927, -0.0094, -0.0853, 0.3945, 0.3608, 0.4624, -0.3914, 0.4116},
                {-0.2708, -0.1006, -0.0579, -0.3810, 0.3630, -0.4055, -0.3326, -0.2721, -0.3118, -0.0426, -0.2884, 0.2733, -0.4806, -0.0673, 0.3524, -0.1630, 0.0262, 0.2025, -0.3236, -0.0502, 0.1625, -0.4854, -0.2367, -0.2346, 0.1309, -0.4632, 0.2336, -0.3476, -0.3910, 0.2075, 0.1416, 0.3382},
                {0.3496, 0.1711, 0.3062, 0.0525, -0.4033, -0.2676, 0.4599, -0.3433, 0.1879, 0.2528, 0.3350, -0.2619, 0.4713, -0.2038, -0.1901, -0.3423, -0.1917, 0.3040, -0.4756, 0.3413, 0.4081, 0.0859, -0.4237, 0.2253, 0.2153, -0.4479, -0.3163, -0.2077, -0.4857, -0.0792, 0.2290, -0.2911},
                {-0.2326, -0.1374, 0.0936, -0.0658, -0.1000, 0.1034, 0.1262, -0.0900, -0.4632, -0.3247, -0.0381, -0.2182, 0.0844, 0.0111, -0.2629, 0.0818, -0.3516, 0.1341, 0.3627, -0.2045, -0.0215, 0.2982, -0.1563, 0.0830, -0.4584, -0.3913, 0.3625, 0.1876, 0.4183, -0.1696, -0.3134, -0.1018},
                {-0.2638, 0.4011, -0.4202, -0.1919, 0.1623, 0.3565, 0.4929, 0.0461, -0.2152, -0.3250, 0.1382, 0.2311, 0.3815, -0.4095, -0.2064, -0.3414, 0.1437, 0.2112, -0.4582, -0.4591, -0.0504, -0.0517, 0.2802, -0.1342, 0.3017, 0.3243, -0.0014, -0.3413, 0.0461, 0.1158, 0.3217, 0.3663},
                {-0.1369, 0.3889, 0.1226, -0.2653, -0.2062, -0.3393, 0.1666, -0.4977, 0.0429, -0.2297, 0.2147, -0.4233, 0.1822, 0.3110, -0.4293, -0.0082, -0.2447, 0.0860, -0.1819, 0.2021, 0.3616, -0.0197, -0.4052, -0.0698, 0.4640, -0.0212, -0.3027, 0.4295, -0.2386, 0.3765, 0.4015, 0.0410},
                {0.3009, -0.0906, -0.4686, 0.1596, -0.3878, -0.4140, -0.1005, 0.1857, -0.4636, 0.3612, 0.0038, -0.4063, -0.3059, -0.2953, 0.0804, -0.3801, -0.4019, -0.1464, 0.3945, 0.0931, 0.4003, 0.4047, 0.4356, 0.2395, -0.4734, -0.3133, -0.4259, -0.3162, 0.0647, -0.3954, -0.1313, 0.0421},
                {-0.2681, -0.0294, -0.4215, -0.3637, 0.4887, 0.3553, 0.2744, -0.3872, 0.2809, -0.3973, -0.2381, -0.4521, -0.4362, 0.2416, -0.2940, -0.2625, 0.0268, -0.4008, 0.4103, 0.3910, 0.4646, -0.2226, -0.0151, 0.2898, 0.0715, -0.1446, -0.1204, 0.0834, -0.2548, 0.3282, 0.1111, 0.4971},
                {0.0358, 0.2738, -0.2373, 0.1603, -0.4245, -0.2186, 0.2820, -0.0591, 0.3744, 0.0798, 0.0761, 0.4497, 0.2062, 0.0652, 0.2870, 0.0636, -0.3757, 0.2181, 0.2626, 0.2277, 0.4909, -0.4148, 0.2741, 0.0619, 0.0868, -0.1063, -0.0931, 0.3139, 0.2512, -0.4307, -0.4491, -0.4552},
                {0.4181, -0.0294, -0.3653, 0.1573, -0.1741, -0.4931, -0.2389, -0.1868, 0.4898, 0.1710, -0.0828, 0.1350, 0.4555, -0.2925, -0.1030, 0.2505, -0.1837, -0.1731, -0.4706, -0.2682, 0.4007, 0.0014, 0.2418, -0.0643, -0.2248, 0.1979, 0.3180, -0.3470, 0.0654, -0.3222, 0.2686, -0.3197},
                {0.2437, -0.3925, -0.4624, -0.3294, 0.4253, -0.3403, 0.0447, -0.0534, 0.3394, 0.1170, -0.3739, -0.2792, 0.4075, 0.0839, -0.0886, 0.1533, 0.3298, -0.2260, -0.4827, 0.3201, 0.2973, -0.1898, -0.3433, -0.0146, 0.0994, -0.2164, -0.1344, 0.3301, -0.1215, -0.1692, 0.3828, -0.4117},
                {-0.1654, 0.2816, 0.4363, 0.0114, -0.4724, -0.0356, -0.0468, 0.1142, -0.4066, -0.4338, 0.1584, 0.3191, -0.3976, -0.2293, 0.2837, -0.2986, 0.1051, -0.4318, 0.3528, 0.2274, -0.2496, -0.1249, -0.2009, 0.1454, -0.4546, 0.3100, -0.2667, -0.3227, -0.1613, 0.4066, -0.3827, -0.0279},
                {-0.2690, 0.1850, -0.1134, -0.4103, 0.3431, -0.0792, -0.1579, 0.2067, -0.3839, 0.0108, 0.4762, -0.4556, -0.1988, 0.1926, -0.1836, -0.1656, 0.0649, -0.4344, -0.2427, -0.0405, 0.2673, -0.3701, 0.2741, -0.4828, 0.2178, 0.0207, -0.2499, 0.3474, -0.1105, -0.3017, 0.2813, -0.2584},
                {-0.3602, 0.3352, -0.1447, -0.3834, -0.2145, -0.0921, -0.1566, 0.4606, -0.3982, -0.2470, -0.2703, 0.3797, 0.3902, 0.2457, 0.4952, -0.4042, -0.1763, 0.3273, 0.0617, 0.2119, -0.4751, -0.1078, -0.2959, 0.2717, -0.4055, 0.4607, -0.2347, 0.4588, 0.3770, 0.4853, -0.1773, -0.3606}
        }).mul(1L << sourceShiftB);;




        long theoreticalShiftAmount = sourceShiftA + sourceShiftB - targetOutputShift;

        INDArray integerMatMul = matrixA.mmul(matrixB);

        INDArray theoreticalExpected = (theoreticalShiftAmount >= 0)
                ? integerMatMul.div(1L << theoreticalShiftAmount)
                : integerMatMul.mul(1L << -theoreticalShiftAmount);


        INDArray simulatedExpected = calculateSimulatedFixedPointGemmAsMatMul(
                matrixA, matrixB,
                sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift
        );

        HWAcceleratedGemmV13 operator = new HWAcceleratedGemmV13();

        INDArray actualOutput = operator.gemm(
                matrixA, matrixB, null, 1.0f, 1.0f, 0L, 0L,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift,
                "testGemmFixedMatrix1x16x16x32"
        );

        HWAcceleratedTestModel.validate("Gemm - Fixed 1x16 * 16x32", theoreticalExpected, simulatedExpected, actualOutput, 0.0);
    }


}


package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class DL4JGemmV13Test extends DL4JTestCase {

    private INDArray invokeGemm(DL4JGemmV13 op,
                                INDArray A,
                                INDArray B,
                                INDArray C,
                                float alpha,
                                float beta,
                                long transA,
                                long transB) {
        try {
            Method m = DL4JGemmV13.class.getDeclaredMethod(
                    "gemm",
                    INDArray.class, INDArray.class, INDArray.class,
                    float.class, float.class, long.class, long.class
            );
            m.setAccessible(true);
            return (INDArray) m.invoke(op, A, B, C, alpha, beta, transA, transB);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        } catch (Exception e) {
            throw new RuntimeException("GeMM invocation failed", e);
        }
    }


    private void testGemm(INDArray expected,
                          INDArray A,
                          INDArray B,
                          INDArray C,
                          float alpha,
                          float beta,
                          long transA,
                          long transB) {

        DL4JGemmV13 op = new DL4JGemmV13();
        INDArray actual = invokeGemm(op, A, B, C, alpha, beta, transA, transB);

        System.out.println("=== GEMM Test Debug ===");
        System.out.println("A shape: " + A.shapeInfoToString());
        System.out.println("B shape: " + B.shapeInfoToString());
        if (C != null) System.out.println("C shape: " + C.shapeInfoToString());
        System.out.println("alpha: " + alpha + ", beta: " + beta);
        System.out.println("transA: " + transA + ", transB: " + transB);
        System.out.println("Expected:\n" + expected);
        System.out.println("Actual:\n" + actual);

        assertArrayEquals("Shape mismatch", expected.shape(), actual.shape());
        assertTrue("Value mismatch (tolerance=1e-5)", expected.equalsWithEps(actual, 1e-5f));

    }

    private INDArray matrix(float[][] data) {
        return Nd4j.create(data);
    }
    private INDArray matrix(float[][][] data) {
        return Nd4j.create(data);
    }


    @Test
    public void testWithProvidedData() {
        // A 的第一行数据
        float[][] aData = {
                {-0.18809962f, -0.26599026f, -0.1974665f, 1.0f}
        };

        // B 的完整数据，已被重塑为 4x64
        float[][] bData = {
                {1.8480625f, -0.0823307f, -0.67522144f, -1.3676157f, -1.8022804f, -0.58966255f, 0.028306f, 0.7909708f, -0.616971f, -1.462122f, -1.5852823f, 0.34417534f, 1.6640244f, -0.3256445f, 3.0065231f, 1.0799294f, 1.0846977f, 0.07532406f, -1.3114395f, -1.3759089f, -1.6710205f, 0.20634747f, -2.3717928f, -0.3616209f, 0.80085087f, -1.0288038f, 0.19532394f, -1.0883799f, 1.986599f, 0.2254467f, 1.3551655f, 0.39880562f, 3.1637745f, -1.9517632f, -0.27506733f, -1.6714945f, -2.8662128f, 1.8617096f, -1.5272408f, -1.60215f, -2.3934402f, -0.05008221f, -1.2764091f, 2.091961f, -2.1543713f, -1.339941f, 0.83726215f, -0.37668419f, 1.3086348f, -1.2532701f, 1.0532837f, -1.1367922f, 0.58939266f, -0.7547331f, 2.5484638f, 0.96098804f, -0.22272205f, -1.3303337f, -0.21096134f, 2.7231512f, -1.4039488f, 1.6446066f, -1.604743f, -0.08120823f},
                {-0.24816322f, 1.1547318f, 1.8931341f, 1.3714542f, -0.23769283f, 1.4486914f, 0.22850037f, -0.21639347f, 2.1516542f, -1.7245598f, -0.29477692f, -0.9329748f, 0.2572031f, 0.6723232f, -3.1256733f, 1.5100698f, 0.59845257f, -2.0623598f, -0.77836037f, -1.0439339f, -0.661026f, 0.61411667f, -1.4565172f, -3.7473879f, 0.2310152f, -1.1587772f, -2.5554323f, -2.7230997f, -1.3561382f, 1.7418127f, -0.21700764f, 0.61004925f, -0.14417744f, -0.84269238f, 1.668951f, 0.44991112f, 0.1377306f, 1.6222658f, 1.1108923f, 0.63318825f, 0.96734047f, -0.027874947f, -1.3857021f, 1.9521465f, 0.43170643f, 1.4815254f, 1.2088795f, -1.7798252f, -1.8764591f, 0.03484249f, -1.6474342f, 0.8876238f, 0.98209953f, -2.0483713f, -0.73182297f, 0.8330326f, -0.74048996f, -2.1213922f, -0.03906727f, -0.32260227f, -0.59158325f, 0.37301922f, 0.160923f, -0.04942417f},
                {-2.1935673f, 3.2359219f, 0.08753681f, -0.727684f, 0.29286957f, -0.040023804f, -3.4317064f, 1.7438965f, 1.6566772f, -2.939167f, 1.4625111f, 3.3282366f, -1.5350571f, -0.8548956f, -0.62475586f, 1.8262119f, 1.3118954f, -2.3217525f, 2.2373371f, 1.7245512f, 0.6060991f, -3.3180656f, -0.9562435f, -0.46806908f, 1.4707508f, 0.8671875f, 1.1022644f, -1.6424627f, 1.4833746f, -1.0701771f, 2.3887482f, -1.6400146f, 0.1461916f, -1.8709345f, -0.61038208f, -0.97260094f, -0.46255112f, 0.2300005f, 0.69215393f, -2.6923323f, 1.5098419f, 4.7093506f, 0.30624008f, -1.6843042f, -0.8042631f, -0.37176895f, -2.1062498f, 1.888588f, -1.8756046f, -3.1116972f, 1.5319633f, 1.4391756f, 2.366662f, 1.4581604f, -1.6072054f, -1.3778296f, -3.4519444f, 0.27983665f, 3.3692455f, 1.3186588f, -0.22806835f, -1.2532482f, 2.0296059f, 4.375639f},
                {-0.30069542f, 0.09172058f, 0.074653625f, 0.07515621f, -0.0396595f, 0.057468414f, -0.14281845f, -0.17039776f, 0.02617073f, -0.022462845f, 0.042565346f, -0.039751053f, 0.046037674f, -0.013184547f, 0.008283615f, -0.052614212f, -0.08409977f, -0.12702751f, -0.15585423f, 0.08913231f, -0.08060074f, 0.033977509f, 0.158494f, 0.13831711f, -0.15721703f, -0.012212753f, 0.052732468f, 0.09253788f, 0.03581524f, 0.20034504f, 0.3420906f, 0.011835098f, 0.16081142f, 0.06320667f, 0.1940031f, -0.008777618f, 0.03491497f, 0.0825634f, 0.09621906f, -0.24177837f, 0.11133575f, 0.45181084f, 0.0295f, 0.051898956f, 0.04371643f, 0.022371292f, -0.06127739f, 0.2056284f, 0.004183769f, -0.11277962f, -0.021965027f, -0.020837784f, 0.13669205f, -0.038166046f, 0.05175972f, 0.061017036f, -0.017291069f, 0.09414196f, 0.2952795f, 0.01735878f, 0.07096195f, -0.28056145f, -0.052739143f, 0.4466467f}
        };

        // 我们计算出的“标准答案” Y 的第一行
        float[][] expectedData = {
                {
                        -0.14915025f, -0.83892655f, -0.31917828f,  0.11130394f,  0.30474085f,
                        -0.20905071f,  0.46872538f, -0.60598165f, -0.7572346f,   1.2916648f,
                        0.13036722f, -0.51354325f, -0.03225587f,  0.03805086f,  0.3975247f,
                        -1.018028f,   -0.7063689f,   0.86584f,    -0.14393578f,  0.28507543f,
                        0.2898598f,    0.48702136f,  1.1808728f,    1.2955345f,  -0.6597286f,
                        0.31828782f,  0.47805193f,  1.345911f,   -0.27006048f, -0.09404251f,
                        -0.32679135f,  0.09840069f, -0.42481154f,  1.0239275f,  -0.07765153f,
                        0.37801397f,  0.6287519f,  -0.7445477f,  -0.04867126f,  0.42280895f,
                        0.00609464f, -0.46129322f,  0.57770324f, -0.52825636f,  0.4929382f,
                        -0.04624571f, -0.12440249f,  0.37696588f,  0.62751895f,  0.7281482f,
                        -0.08439726f, -0.32729584f, -0.70273787f,  0.36070794f,  0.08442162f,
                        -0.06924784f,  0.9032093f,    0.85338855f, -0.31996036f, -0.6694468f,
                        0.5374354f,  -0.44165623f, -0.1944707f,  -0.38897383f
                }
        };

        // C 未提供，所以是 null
        testGemm(
                matrix(expectedData),
                matrix(aData),
                matrix(bData),
                null,
                1.0f,
                1.0f, // beta=0.0因为C是null，这等效于我们之前的计算
                0L,
                0L
        );
    }

    @Test
    public void testBasicMultiplication() {
        testGemm(
                matrix(new float[][]{{19, 22}, {43, 50}}),
                matrix(new float[][]{{1, 2}, {3, 4}}),
                matrix(new float[][]{{5, 6}, {7, 8}}),
                null,
                1.0f,
                0.0f,
                0L,
                0L
        );
    }

    @Test
    public void testWithScalingAndBias() {
        testGemm(
                matrix(new float[][]{{21.25f, 24.25f}, {45.25f, 52.25f}}),
                matrix(new float[][]{{1, 2}, {3, 4}}),
                matrix(new float[][]{{5, 6}, {7, 8}}),
                matrix(new float[][]{{1.5f, 1.5f}, {1.5f, 1.5f}}),
                1.0f,
                1.5f,
                0L,
                0L
        );
    }

    @Test
    public void testTransposeOperations() {
        testGemm(
                matrix(new float[][]{{19, 22}, {43, 50}}),
                matrix(new float[][]{{1, 3}, {2, 4}}), // A^T
                matrix(new float[][]{{5, 7}, {6, 8}}), // B^T
                null,
                1.0f,
                0.0f,
                1L,
                1L
        );
    }

    @Test(expected = RuntimeException.class)
    public void testDimensionMismatch() {
        testGemm(
                null,
                matrix(new float[][]{{1, 2, 3}}), // 1x3
                matrix(new float[][]{{4}, {5}}),   // 2x1
                null,
                1.0f,
                0.0f,
                0L,
                0L
        );
    }

    @Test
    public void testAlphaScaling() {
        testGemm(
                matrix(new float[][]{{38, 44}, {86, 100}}),
                matrix(new float[][]{{1, 2}, {3, 4}}),
                matrix(new float[][]{{5, 6}, {7, 8}}),
                null,
                2.0f,
                0.0f,
                0L,
                0L
        );
    }

    @Test
    public void testBetaScaling() {
        testGemm(
                matrix(new float[][]{{21, 26}, {49, 58}}),
                matrix(new float[][]{{1, 2}, {3, 4}}),
                matrix(new float[][]{{5, 6}, {7, 8}}),
                matrix(new float[][]{{1, 2}, {3, 4}}),
                1.0f,
                2.0f,
                0L,
                0L
        );
    }

    @Test
    public void testBroadcastC() {
        testGemm(
                matrix(new float[][]{{2, 3}, {4, 5}}),
                matrix(new float[][]{{1, 0}, {0, 1}}),
                matrix(new float[][]{{1, 2}, {3, 4}}),
                Nd4j.create(new float[]{1}),
                1.0f,
                1.0f,
                0L,
                0L
        );
    }

    @Test
    public void testTransposeAOnly() {
        testGemm(
                matrix(new float[][]{{19, 22}, {43, 50}}),
                matrix(new float[][]{{1, 3}, {2, 4}}),
                matrix(new float[][]{{5, 6}, {7, 8}}),
                null,
                1.0f,
                0.0f,
                1L,
                0L
        );
    }

    @Test
    public void testTransposeBOnly() {
        testGemm(
                matrix(new float[][]{{21, 24, 27}, {47, 54, 61}}),
                matrix(new float[][]{{1,3}, {2,4}}),
                matrix(new float[][]{{5,6,7}, {8,9,10}}),
                null,
                1.0f,
                0.0f,
                1L,
                0L
        );
    }

    @Test
    public void testNonSquareTranspose() {
        testGemm(
                matrix(new float[][]{{39, 49, 59}, {54, 68, 82}, {69, 87, 105}}),
                matrix(new float[][]{{1,2,3}, {4,5,6}}),       // 2x3 → 转置后3x2
                matrix(new float[][]{{7,8}, {9,10}, {11,12}}), // 3x2 → 转置后2x3
                null,
                1.0f,
                0.0f,
                1L,
                1L
        );
    }

    @Test
    public void testVectorMultiplication() {
        testGemm(
                matrix(new float[][]{{10, 12, 14}, {15, 18, 21}, {20, 24, 28}}), // 行向量 * 列向量
                matrix(new float[][]{{2, 3, 4}}),  // 1x3 → 转置后3x1
                matrix(new float[][]{{5}, {6}, {7}}), // 3x1 → 转置后1x3
                null,
                1.0f,
                0.0f,
                1L,
                1L
        );
    }

    @Test
    public void testBatchedMultiplication3D() {
        INDArray a = matrix(new float[][][]{
                {{1, 2, 3}, {4, 5, 6}},      // 第1个矩阵 (2x3)
                {{7, 8, 9}, {10, 11, 12}}    // 第2个矩阵 (2x3)
        });

        INDArray b = matrix(new float[][][]{
                {{10, 11}, {20, 21}, {30, 31}}, // 第1个矩阵 (3x2)
                {{1, 2}, {3, 4}, {5, 6}}        // 第2个矩阵 (3x2)
        });

        INDArray expected = matrix(new float[][][]{
                {{140, 146}, {320, 335}},     // A[0] @ B[0] 的结果
                {{76, 100}, {103, 136}}       // A[1] @ B[1] 的结果
        });

        testGemm(expected, a, b, null, 1.0f, 0.0f, 0L, 0L);
    }

    @Test
    public void testBatchedMultiplication3D2D() {
        INDArray a = matrix(new float[][][]{
                {{1, 2, 3}, {4, 5, 6}},      // 第1个批次
                {{7, 8, 9}, {10, 11, 12}}    // 第2个批次
        }); // Shape: [2, 2, 3]

        INDArray b = matrix(new float[][]{
                {10, 11},
                {20, 21},
                {30, 31}
        }); // Shape: [3, 2]

        INDArray expected = matrix(new float[][][]{
                {{140, 146}, {320, 335}},     // A[0] @ B 的结果
                {{500, 524}, {680, 713}}       // A[1] @ B 的结果
        }); // Shape: [2, 2, 2]

        testGemm(expected, a, b, null, 1.0f, 0.0f, 0L, 0L);
    }


}
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




}
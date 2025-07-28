package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class DL4JMatMulV13Test {

    private final DL4JMatMulV13 op = new DL4JMatMulV13();

    @Test
    public void testFloatMatMul() {
        INDArray a = Nd4j.create(new float[][] {
                {1, 2, 3},
                {4, 5, 5}
        });
        INDArray b = Nd4j.create(new float[][] {
                {1, 2,3,4},
                {5,6,7,8},
                {9,10,11,12}
        });
        INDArray expected = Nd4j.create(new float[][] {
                {38, 44, 50, 56},
                {83, 98, 113, 128}
        });
        checkEqual("Float", expected, op.matmul(a, b));
    }

    @Test
    public void testDoubleMatMul() {
        INDArray a = Nd4j.create(new double[][] {
                {1, 2},
                {3, 4}
        });
        INDArray b = Nd4j.create(new double[][] {
                {5, 6},
                {7, 8}
        });
        INDArray expected = Nd4j.create(new double[][] {
                {19, 22},
                {43, 50}
        });
        checkEqual("Double", expected, op.matmul(a, b));
    }

    @Test
    public void testIntMatMul() {
        INDArray a = Nd4j.createFromArray(new int[][] {
                {1, 2},
                {3, 4}
        });

        INDArray b = Nd4j.createFromArray(new int[][] {
                {5, 6},
                {7, 8}
        });

        INDArray expected = Nd4j.createFromArray(new int[][] {
                {19, 22},
                {43, 50}
        });

        checkEqual("Int", expected, op.matmul(a, b));
    }

    @Test
    public void testLongMatMul() {
        INDArray a = Nd4j.createFromArray(new long[][] {
                {1, 2},
                {3, 4}
        });

        INDArray b = Nd4j.createFromArray(new long[][] {
                {5, 6},
                {7, 8}
        });

        INDArray expected = Nd4j.createFromArray(new long[][] {
                {19, 22},
                {43, 50}
        });

        checkEqual("Long", expected, op.matmul(a, b));
    }

    // 统一比较与打印输出
    private void checkEqual(String type, INDArray expected, INDArray actual) {
        System.out.println("[" + type + "] Expected shape: " + Arrays.toString(expected.shape()));
        System.out.println("[" + type + "] Actual shape:   " + Arrays.toString(actual.shape()));
        System.out.println("[" + type + "] Expected:\n" + expected);
        System.out.println("[" + type + "] Actual:\n" + actual);

        assertTrue("[" + type + "] MatMul mismatch!", expected.equals(actual));
    }
}

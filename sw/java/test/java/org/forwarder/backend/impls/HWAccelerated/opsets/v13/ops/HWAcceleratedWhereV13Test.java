package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;


import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class HWAcceleratedWhereV13Test {

    private final HWAcceleratedWhereV13 op = new HWAcceleratedWhereV13();

    //测试 1：广播 condition + x，基本例子
    @Test
    public void testBroadcastedWhere() {
        INDArray condition = Nd4j.create(new boolean[][] {
                {true},
                {false},
                {true}
        });
        INDArray x = Nd4j.create(new float[][] {
                {10f, 20f, 30f, 40f}
        }).broadcast(3, 4);
        INDArray y = Nd4j.create(new float[][] {
                {1f, 2f, 3f, 4f},
                {5f, 6f, 7f, 8f},
                {9f,10f,11f,12f}
        });
        INDArray expected = Nd4j.create(new float[][] {
                {10f, 20f, 30f, 40f},
                {5f, 6f, 7f, 8f},
                {10f, 20f, 30f, 40f}
        });

        checkWhere(expected, condition, x, y);
    }

    //测试 2：完全相同 shape
    @Test
    public void testSameShapeWhere() {
        INDArray condition = Nd4j.create(new boolean[][] {
                {true, false},
                {false, true}
        });
        INDArray x = Nd4j.create(new float[][] {
                {1f, 2f},
                {3f, 4f}
        });
        INDArray y = Nd4j.create(new float[][] {
                {5f, 6f},
                {7f, 8f}
        });
        INDArray expected = Nd4j.create(new float[][] {
                {1f, 6f},
                {7f, 4f}
        });

        checkWhere(expected, condition, x, y);
    }

    //测试 3：1D 向量广播
    @Test
    public void test1DWhereBroadcast() {
        INDArray condition = Nd4j.create(new boolean[] {true, false, true});
        INDArray x = Nd4j.create(new float[] {1f, 2f, 3f});
        INDArray y = Nd4j.create(new float[] {4f, 5f, 6f});
        INDArray expected = Nd4j.create(new float[] {1f, 5f, 3f});

        checkWhere(expected, condition, x, y);
    }

    //测试 4：scalar condition 广播
    @Test
    public void testScalarCondition() {
        INDArray condition = Nd4j.scalar(true); // scalar broadcast to all
        INDArray x = Nd4j.create(new float[] {1f, 2f, 3f});
        INDArray y = Nd4j.create(new float[] {4f, 5f, 6f});
        INDArray expected = Nd4j.create(new float[] {1f, 2f, 3f});

        checkWhere(expected, condition.broadcast(3), x, y);  // 手动广播 condition
    }

    //测试 5：复杂混合广播
    @Test
    public void testMixedWhere() {
        INDArray condition = Nd4j.create(new boolean[][] {
                {true, false, true},
                {false, true, false}
        });
        INDArray x = Nd4j.create(new float[] {9f}).broadcast(2, 3);  // scalar → [2,3]
        INDArray y = Nd4j.create(new float[][] {
                {1f, 2f, 3f},
                {4f, 5f, 6f}
        });
        INDArray expected = Nd4j.create(new float[][] {
                {9f, 2f, 9f},
                {4f, 9f, 6f}
        });

        checkWhere(expected, condition, x, y);
    }

    //统一断言函数
    protected void checkWhere(INDArray expected, INDArray condition, INDArray x, INDArray y) {
        INDArray actual = op.where(condition, x, y);

        System.out.println("Expected shape: " + Arrays.toString(expected.shape()));
        System.out.println("Actual shape:   " + Arrays.toString(actual.shape()));
        System.out.println("Expected values:\n" + expected);
        System.out.println("Actual values:\n" + actual);

        assertTrue("Mismatch between expected and actual", expected.equals(actual));
    }
}

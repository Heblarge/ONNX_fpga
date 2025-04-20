package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import org.forwarder.backend.impls.dl4j.DL4JSession;
import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class DL4JExpV13Test extends DL4JTestCase {

    /**
     * 正数输入测试：验证正值指数计算
     */
    @Test
    public void testPositiveValues() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{1.0f, 2.0f, 3.0f});
                INDArray expected = Nd4j.create(new float[]{
                        (float) Math.exp(1.0),
                        (float) Math.exp(2.0),
                        (float) Math.exp(3.0)
                })
        ) {
            this.testExp(expected, input);
        }
    }

    /**
     * 负数输入测试：验证负值指数计算
     */
    @Test
    public void testNegativeValues() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{-1.0f, -2.0f, -3.0f});
                INDArray expected = Nd4j.create(new float[]{
                        (float) Math.exp(-1.0),
                        (float) Math.exp(-2.0),
                        (float) Math.exp(-3.0)
                })
        ) {
            this.testExp(expected, input);
        }
    }

    /**
     * 零值测试：验证exp(0)=1
     */
    @Test
    public void testZeroValue() throws Exception {
        try (
                INDArray input = Nd4j.zeros(1);
                INDArray expected = Nd4j.ones(1)
        ) {
            this.testExp(expected, input);
        }
    }

    /**
     * 混合数值测试：包含正数、负数、零值
     */
    @Test
    public void testMixedValues() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{0.0f, -1.5f, 2.5f});
                INDArray expected = Nd4j.create(new float[]{
                        1.0f,
                        (float) Math.exp(-1.5),
                        (float) Math.exp(2.5)
                })
        ) {
            this.testExp(expected, input);
        }
    }

    /**
     * 高维数据测试：3D张量
     */
    @Test
    public void test3DInput() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[][][]{
                        {{1.0f, -2.0f}, {0.0f, 3.0f}},
                        {{-1.5f, 2.5f}, {0.5f, -0.5f}}
                });
                INDArray expected = Nd4j.create(new float[][][]{
                        {{(float) Math.exp(1), (float) Math.exp(-2)},
                                {(float) Math.exp(0), (float) Math.exp(3)}},
                        {{(float) Math.exp(-1.5), (float) Math.exp(2.5)},
                                {(float) Math.exp(0.5), (float) Math.exp(-0.5)}}
                })
        ) {
            this.testExp(expected, input);
        }
    }

    /**
     * 边界值测试：极大值和极小值
     */
    @Test
    public void testBoundaryValues() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{
                        Float.MAX_VALUE,
                        Float.MIN_VALUE,
                        -Float.MAX_VALUE
                });
                INDArray expected = Nd4j.create(new float[]{
                        Float.POSITIVE_INFINITY, // exp(极大正数)趋向无穷大
                        (float) Math.exp(Float.MIN_VALUE), // 接近1
                        0.0f // exp(极大负数)趋向0
                })
        ) {
            this.testExp(expected, input);
        }
    }

    private void testExp(INDArray expected, INDArray input) throws Exception {
        try (DL4JSession session = new DL4JSession(null)) {
            DL4JExpV13 operator = new DL4JExpV13();
            INDArray output = operator.exp(input);

            if (expected != null) {
                System.out.println(String.format(
                        "{Expected shape: %s} - {Actual shape: %s}",
                        expected.shapeInfoToString(),
                        output.shapeInfoToString()
                ));
                System.out.println(String.format(
                        "{Expected values: %s} - {Actual values: %s}",
                        expected.toString(),
                        output.toString()
                ));

                // 验证形状一致性
                assertArrayEquals("Shape mismatch", expected.shape(), output.shape());
                // 允许存在浮点误差
                assertTrue("Content mismatch", expected.equalsWithEps(output, 1e-6f));
            }
        }
    }
}
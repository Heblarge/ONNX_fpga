package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import org.forwarder.backend.impls.dl4j.DL4JSession;
import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class DL4JLogV13Test extends DL4JTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * All positive values: input (3) → output natural log values
     */
    @Test
    public void test1_PositiveValues() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{1.0f, (float) Math.E, 10.0f});
                INDArray expected = Nd4j.create(new float[]{0.0f, 1.0f, (float) Math.log(10)})
        ) {
            this.testLog(expected, input);
        }
    }

    /**
     * Zero value input → should throw IllegalArgumentException
     */
    @Test
    public void test2_ZeroValue() throws Exception {
        try (
                INDArray input = Nd4j.zeros(1);
        ) {
            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage("must be positive");
            this.testLog(null, input);
        }
    }

    /**
     * Negative values: input (2,2) → should throw exception
     */
    @Test
    public void test3_NegativeValues() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[][]{{-1.0f, -2.0f}, {-3.0f, -4.0f}});
        ) {
            thrown.expect(IllegalArgumentException.class);
            this.testLog(null, input);
        }
    }

    /**
     * Mixed positive/non-positive: input (1,3) → should throw exception
     */
    @Test
    public void test4_MixedValues() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{2.0f, 0.0f, 4.0f}).reshape(1, 3);
        ) {
            thrown.expect(IllegalArgumentException.class);
            this.testLog(null, input);
        }
    }

    /**
     * High-dimensional input: 3D tensor with valid values
     */
    @Test
    public void test5_HighDimension() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[][][]{
                        {{1.0f, 2.0f}, {3.0f, 4.0f}},
                        {{5.0f, 6.0f}, {7.0f, 8.0f}}
                });
                INDArray expected = Nd4j.create(new float[][][]{
                        {{0.0f, (float) Math.log(2)}, {(float) Math.log(3), (float) Math.log(4)}},
                        {{(float) Math.log(5), (float) Math.log(6)}, {(float) Math.log(7), (float) Math.log(8)}}
                })
        ) {
            this.testLog(expected, input);
        }
    }

    /**
     * Edge case: minimal positive value
     */
    @Test
    public void test6_MinimalPositive() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{Float.MIN_VALUE});
                INDArray expected = Nd4j.create(new float[]{(float) Math.log(Float.MIN_VALUE)})
        ) {
            this.testLog(expected, input);
        }
    }

    private void testLog(INDArray expected, INDArray input) throws Exception {
        try (DL4JSession session = new DL4JSession(null)) {
            DL4JLogV13 operator = new DL4JLogV13();
            INDArray output = operator.log(input);

            // 仅在非异常测试时验证输出
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
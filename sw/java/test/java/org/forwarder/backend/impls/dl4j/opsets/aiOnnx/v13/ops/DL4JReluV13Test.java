package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import org.forwarder.backend.impls.dl4j.DL4JSession;
import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class DL4JReluV13Test extends DL4JTestCase {

    /**
     * All positive values: input (3) → output (3) unchanged
     */
    @Test
    public void test1_PositiveValues() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{1.0f, 2.0f, 3.0f});
                INDArray expected = Nd4j.create(new float[]{1.0f, 2.0f, 3.0f})
        ) {
            this.testRelu(expected, input);
        }
    }

    /**
     * All negative values: input (2,2) → output (2,2) zeros
     */
    @Test
    public void test2_NegativeValues() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[][]{{-1.0f, -2.0f}, {-3.0f, -4.0f}});
                INDArray expected = Nd4j.zeros(2, 2)
        ) {
            this.testRelu(expected, input);
        }
    }

    /**
     * Mixed values: input (1,4) → output (0,2,0,4)
     */
    @Test
    public void test3_MixedValues() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{-1.0f, 2.0f, -3.0f, 4.0f});
                INDArray expected = Nd4j.create(new float[]{0.0f, 2.0f, 0.0f, 4.0f})
        ) {
            this.testRelu(expected, input);
        }
    }

    /**
     * Zero input: scalar → zero output
     */
    @Test
    public void test4_ZeroValue() throws Exception {
        try (
                INDArray input = Nd4j.zeros(1);
                INDArray expected = Nd4j.zeros(1)
        ) {
            this.testRelu(expected, input);
        }
    }

    /**
     * 3D input: (2,2,2) → output with correct activation
     */
    @Test
    public void test5_HighDimension() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[][][]{
                        {{-1.0f, 2.0f}, {3.0f, -4.0f}},
                        {{5.0f, -6.0f}, {-7.0f, 8.0f}}
                });
                INDArray expected = Nd4j.create(new float[][][]{
                        {{0.0f, 2.0f}, {3.0f, 0.0f}},
                        {{5.0f, 0.0f}, {0.0f, 8.0f}}
                })
        ) {
            this.testRelu(expected, input);
        }
    }

    private void testRelu(INDArray expected, INDArray input) throws Exception {
        try (DL4JSession session = new DL4JSession(null)) {
            DL4JReluV13 operator = new DL4JReluV13();
            INDArray output = operator.relu(input);

            System.out.println(String.format(
                    "{Expected shape: %s} - {Actual shape: %s}",
                    expected.shapeInfoToString(),
                    output.shapeInfoToString()
            ));

            // 验证形状一致性
            assertArrayEquals("Shape mismatch", expected.shape(), output.shape());
            // 验证内容一致性
            assertTrue("Content mismatch", expected.equals(output));
        }
    }
}
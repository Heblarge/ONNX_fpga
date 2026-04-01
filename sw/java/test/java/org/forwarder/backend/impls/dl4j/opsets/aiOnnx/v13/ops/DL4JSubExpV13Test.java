package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import static org.junit.Assert.*;

import org.forwarder.backend.impls.dl4j.DL4JSession;
import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class DL4JSubExpV13Test extends DL4JTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * 基本数值测试: Exp(a - b)
     */
    @Test
    public void testBasicValues() throws Exception {
        try (
                INDArray a = Nd4j.create(new float[]{10f, 20f, 30f});
                INDArray b = Nd4j.create(new float[]{4f, 5f, 6f});
                INDArray expected = Nd4j.create(new float[]{
                        (float)Math.exp(10 - 4),
                        (float)Math.exp(20 - 5),
                        (float)Math.exp(30 - 6)
                })
        ) {
            this.testSubExp(expected, a, b);
        }
    }

    /**
     * 广播测试
     * A: (2x3)
     * B: (3)
     */
    @Test
    public void testBroadcast() throws Exception {
        try (
                INDArray a = Nd4j.ones(2, 3).mul(10f); // 全为10
                INDArray b = Nd4j.create(new float[]{1f, 2f, 3f});
                INDArray expected = Nd4j.create(new float[][]{
                        {
                                (float)Math.exp(10 - 1),
                                (float)Math.exp(10 - 2),
                                (float)Math.exp(10 - 3)
                        },
                        {
                                (float)Math.exp(10 - 1),
                                (float)Math.exp(10 - 2),
                                (float)Math.exp(10 - 3)
                        }
                })
        ) {
            this.testSubExp(expected, a, b);
        }
    }

    /**
     * 混合正负值测试
     */
    @Test
    public void testMixedValues() throws Exception {
        try (
                INDArray a = Nd4j.create(new float[]{-1f, 0f, 1f});
                INDArray b = Nd4j.create(new float[]{1f, -1f, 2f});
                INDArray expected = Nd4j.create(new float[]{
                        (float)Math.exp(-1 - 1),   // exp(-2)
                        (float)Math.exp(0 - (-1)), // exp(1)
                        (float)Math.exp(1 - 2)     // exp(-1)
                })
        ) {
            this.testSubExp(expected, a, b);
        }
    }

    /**
     * 形状不匹配异常测试
     */
    @Test
    public void testShapeMismatch() throws Exception {
        try (
                INDArray a = Nd4j.create(3);
                INDArray b = Nd4j.create(4)
        ) {
            thrown.expect(IllegalStateException.class);
            this.testSubExp(null, a, b);
        }
    }

    private void testSubExp(INDArray expected, INDArray a, INDArray b) throws Exception {
        try (DL4JSession session = new DL4JSession(null)) {

            DL4JSubExpV13 operator = new DL4JSubExpV13();
            INDArray output = operator.subExp(a, b);

            if (expected != null) {

                System.out.println(String.format(
                        "{Expected shape: %s} - {Actual shape: %s}",
                        expected.shapeInfoToString(),
                        output.shapeInfoToString()
                ));

                assertArrayEquals(expected.shape(), output.shape());

                assertTrue("Content mismatch at SubExp",
                        expected.equalsWithEps(output, 1e-6f));
            }
        }
    }
}

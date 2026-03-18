package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import static org.junit.Assert.*;

import org.forwarder.backend.impls.dl4j.DL4JSession;
import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class DL4JAddExpV13Test extends DL4JTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testBasicValues() throws Exception {
        try (
                INDArray a = Nd4j.create(new float[]{1f, 2f, 3f});
                INDArray b = Nd4j.create(new float[]{4f, 5f, 6f});
                INDArray expected = Nd4j.create(new float[]{
                        (float)Math.exp(5),
                        (float)Math.exp(7),
                        (float)Math.exp(9)
                })
        ) {
            this.testAddExp(expected, a, b);
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
                INDArray a = Nd4j.ones(2,3);
                INDArray b = Nd4j.create(new float[]{1f,2f,3f});
                INDArray expected = Nd4j.create(new float[][]{
                        {
                                (float)Math.exp(2),
                                (float)Math.exp(3),
                                (float)Math.exp(4)
                        },
                        {
                                (float)Math.exp(2),
                                (float)Math.exp(3),
                                (float)Math.exp(4)
                        }
                })
        ) {
            this.testAddExp(expected, a, b);
        }
    }

    /**
     * 混合正负值
     */
    @Test
    public void testMixedValues() throws Exception {
        try (
                INDArray a = Nd4j.create(new float[]{-1f, 0f, 1f});
                INDArray b = Nd4j.create(new float[]{1f, -1f, 2f});
                INDArray expected = Nd4j.create(new float[]{
                        (float)Math.exp(0),
                        (float)Math.exp(-1),
                        (float)Math.exp(3)
                })
        ) {
            this.testAddExp(expected, a, b);
        }
    }


    @Test
    public void testShapeMismatch() throws Exception {
        try (
                INDArray a = Nd4j.create(3);
                INDArray b = Nd4j.create(4)
        ) {
            thrown.expect(IllegalStateException.class);
            this.testAddExp(null, a, b);
        }
    }

    private void testAddExp(INDArray expected, INDArray a, INDArray b) throws Exception {
        try (DL4JSession session = new DL4JSession(null)) {

            DL4JAddExpV13 operator = new DL4JAddExpV13();
            INDArray output = operator.addExp(a, b);

            if (expected != null) {

                System.out.println(String.format(
                        "{Expected shape: %s} - {Actual shape: %s}",
                        expected.shapeInfoToString(),
                        output.shapeInfoToString()
                ));

                assertArrayEquals(expected.shape(), output.shape());

                assertTrue("Content mismatch",
                        expected.equalsWithEps(output, 1e-6f));
            }
        }
    }
}

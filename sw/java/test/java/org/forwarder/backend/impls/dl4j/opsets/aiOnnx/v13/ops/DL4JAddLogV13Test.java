package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.DL4JSession;
import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

public class DL4JAddLogV13Test extends DL4JTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /** ================================
     * 正常广播测试
     * ================================ */
    @Test
    public void testPositiveBroadcast() throws Exception {
        try (INDArray a = Nd4j.create(new float[]{1.0f, 2.0f, 3.0f});
             INDArray b = Nd4j.create(new float[]{4.0f, 5.0f, 6.0f});
             INDArray expected = Nd4j.create(new float[]{
                     (float)Math.log(5.0), (float)Math.log(7.0), (float)Math.log(9.0)
             })) {
            this.testAddLog(expected, a, b);
        }
    }

    /** ================================
     * 高维广播测试
     * ================================ */
    @Test
    public void testHighDimension() throws Exception {
        try (INDArray a = Nd4j.ones(2, 2, 2);
             INDArray b = Nd4j.ones(2, 1, 2);
             INDArray expected = Nd4j.create(new float[][][]{
                     {{(float)Math.log(2), (float)Math.log(2)},
                             {(float)Math.log(2), (float)Math.log(2)}},
                     {{(float)Math.log(2), (float)Math.log(2)},
                             {(float)Math.log(2), (float)Math.log(2)}}
             })) {
            this.testAddLog(expected, a, b);
        }
    }

    /** ================================
     * 异常测试：0输入
     * ================================ */
    @Test
    public void testZeroInput() throws Exception {
        try (INDArray a = Nd4j.create(new float[]{0.0f, 1.0f});
             INDArray b = Nd4j.create(new float[]{0.0f, 2.0f})) {
            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage("AddLog input must be positive");
            this.testAddLog(null, a, b);
        }
    }

    /** ================================
     * 异常测试：负数输入
     * ================================ */
    @Test
    public void testNegativeInput() throws Exception {
        try (INDArray a = Nd4j.create(new float[]{1.0f, 2.0f});
             INDArray b = Nd4j.create(new float[]{-2.0f, 3.0f})) {
            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage("AddLog input must be positive");
            this.testAddLog(null, a, b);
        }
    }

    /** ================================
     * 核心测试方法
     * ================================ */
    private void testAddLog(INDArray expected, INDArray a, INDArray b) throws Exception {
        try (DL4JSession session = new DL4JSession(null)) {
            DL4JAddLogV13 operator = new DL4JAddLogV13();
            INDArray output = operator.addLog(a, b);

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
                assertArrayEquals("Shape mismatch", expected.shape(), output.shape());
                assertTrue("Content mismatch", expected.equalsWithEps(output, 1e-6f));
            }
        }
    }
}

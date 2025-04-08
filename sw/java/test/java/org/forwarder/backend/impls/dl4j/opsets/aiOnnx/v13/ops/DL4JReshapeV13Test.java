package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import static org.junit.Assert.assertTrue;

import org.forwarder.backend.impls.dl4j.DL4JSession;
import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class DL4JReshapeV13Test extends DL4JTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * <pre>
     * A: [2 x 3]
     * shape: [3, 2]
     * result: [3 x 2]
     * </pre>
     */
    @Test
    public void test1() throws Exception {
        try (
                INDArray expected = Nd4j.create(3, 2);
                INDArray input = Nd4j.create(2, 3);
                INDArray shape = Nd4j.createFromArray(3L, 2L);
        ) {
            this.testReshape(expected, input, shape);
        }
    }

    /**
     * <pre>
     * A: [2 x 3]
     * shape: [-1]
     * result: [6]
     * </pre>
     */
    @Test
    public void test2() throws Exception {
        try (
                INDArray expected = Nd4j.create(6);
                INDArray input = Nd4j.create(2, 3);
                INDArray shape = Nd4j.createFromArray(-1L);
        ) {
            this.testReshape(expected, input, shape);
        }
    }

    /**
     * <pre>
     * A: [2 x 3]
     * shape: [0, -1]
     * result: [2 x 3]
     * </pre>
     */
    @Test
    public void test3() throws Exception {
        try (
                INDArray expected = Nd4j.create(2, 3);
                INDArray input = Nd4j.create(2, 3);
                INDArray shape = Nd4j.createFromArray(0L, -1L);
        ) {
            this.testReshape(expected, input, shape);
        }
    }

    /**
     * <pre>
     * A: [2 x 3]
     * shape: [4, 2] → 不合法，总元素数为 8 ≠ 6
     * 应抛出异常
     * </pre>
     */
    @Test
    public void test4() throws Exception {
        try (
                INDArray input = Nd4j.create(2, 3);
                INDArray shape = Nd4j.createFromArray(4L, 2L);
        ) {
            thrown.expect(IllegalArgumentException.class);
            this.testReshape(null, input, shape);
        }
    }

    private void testReshape(INDArray expected, INDArray input, INDArray shape) throws Exception {
        try (DL4JSession session = new DL4JSession(null)) {
            DL4JReshapeV13 operator = new DL4JReshapeV13();
            INDArray output = operator.reshape(input, shape);
            System.out.println(String.format("{Expected: %s} - {Actual: %s}",
                    expected.shapeInfoToString(), output.shapeInfoToString()));
            assertTrue(output.equalShapes(expected));
        }
    }
}
package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedSession;
import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * Strict ONNX Expand v13 testcases, pay attention to shape tensor construction
 */
public class HWAcceleratedExpandV13Test extends HWAcceleratedTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Case 1: Basic expansion (3,1) → (3,4)
     */
    @Test
    public void testExpandBasic() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{1,2,3}, new int[]{3,1});
                INDArray shape = Nd4j.createFromArray(3,4);
                INDArray expected = Nd4j.create(new float[][]{
                        {1,1,1,1},
                        {2,2,2,2},
                        {3,3,3,3}
                })
        ) {
            this.testExpand(expected, input, shape);
        }
    }

    /**
     * Case 2: Right-align, shape shorter than input, (2,1,6) -> (2,3,6)
     */
    @Test
    public void testExpandRightAlign() throws Exception {
        try (
                INDArray input = Nd4j.linspace(1,12,12).reshape(2,1,6);
                INDArray shape = Nd4j.createFromArray(2,3,6);
                INDArray expected = input.broadcast(2,3,6)
        ) {
            this.testExpand(expected, input, shape);
        }
    }

    /**
     * Case 3: shape with non-1D (should fail)
     */
    @Test
    public void testExpandShapeNot1D() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{1,2,3}, new int[]{3,1});
                INDArray illegalShape = Nd4j.create(new long[][]{{3,4}}) // This is 2D!
        ) {
            thrown.expect(org.nd4j.linalg.exception.ND4JIllegalStateException.class);
            this.testExpand(null, input, illegalShape);
        }
    }

    /**
     * Case 4: shape with negative dimension (should fail)
     */
    @Test
    public void testExpandShapeNegative() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{1,2,3}, new int[]{3,1});
                INDArray illegalShape = Nd4j.createFromArray(-1, 4)
        ) {
            thrown.expect(IllegalArgumentException.class);
            this.testExpand(null, input, illegalShape);
        }
    }

    /**
     * Case 5: Input shape matches target shape (no expand needed)
     */
    @Test
    public void testExpandNoOp() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[][]{{1,2,3,4}, {5,6,7,8}});
                INDArray shape = Nd4j.createFromArray(2,4);
                INDArray expected = input.dup()
        ) {
            this.testExpand(expected, input, shape);
        }
    }

    /**
     * Case 6: Broadcasting (1,1,3) → (2,4,3), multi-dim
     */
    @Test
    public void testExpandBroadcastMultiDim() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{1,2,3}).reshape(1,1,3);
                INDArray shape = Nd4j.createFromArray(2,4,3);
                INDArray expected = input.broadcast(2,4,3)
        ) {
            this.testExpand(expected, input, shape);
        }
    }


    /**
     * Helper: test expand and check result shape
     */
    private void testExpand(INDArray expected, INDArray input, INDArray shape) throws Exception {
        try (HWAcceleratedSession session = new HWAcceleratedSession(null)) {
            HWAcceleratedExpandV13 operator = new HWAcceleratedExpandV13();
            INDArray y = operator.expand(input, shape);
            if (expected != null) {
                assertArrayEquals(
                        "Shape mismatch", expected.shape(), y.shape());
                assertTrue("Content mismatch", expected.equalsWithEps(y, 1e-5));
            }
        }
    }
}
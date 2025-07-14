package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedSession;
import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.junit.Assert.assertArrayEquals;

public class HWAcceleratedConcatV13Test extends HWAcceleratedTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Concat along axis 0: (2, 3) + (2, 3) → (4, 3)
     */
    @Test
    public void test1() throws Exception {
        try (
                INDArray expected = Nd4j.create(4, 3);
                INDArray a = Nd4j.create(2, 3);
                INDArray b = Nd4j.create(2, 3)
        ) {
            this.testConcat(expected, a, b, 0);
        }
    }

    /**
     * Concat along axis 1: (2, 3) + (2, 3) → (2, 6)
     */
    @Test
    public void test2() throws Exception {
        try (
                INDArray expected = Nd4j.create(2, 6);
                INDArray a = Nd4j.create(2, 3);
                INDArray b = Nd4j.create(2, 3)
        ) {
            this.testConcat(expected, a, b, 1);
        }
    }

    /**
     * Concat with negative axis -1 → last dimension: (2, 3) + (2, 3) → (2, 6)
     */
    @Test
    public void test3() throws Exception {
        try (
                INDArray expected = Nd4j.create(2, 6);
                INDArray a = Nd4j.create(2, 3);
                INDArray b = Nd4j.create(2, 3)
        ) {
            this.testConcat(expected, a, b, -1);
        }
    }

    /**
     * Incompatible shapes: (2, 3) + (2, 4) → error
     */
    @Test
    public void test4() throws Exception {
        try (
                INDArray a = Nd4j.create(2, 3);
                INDArray b = Nd4j.create(2, 4)
        ) {
            thrown.expect(IllegalArgumentException.class);
            this.testConcat(null, a, b, 1);
        }
    }

    /**
     * Concat 1D: (3) + (2) → (5)
     */
    @Test
    public void test5() throws Exception {
        try (
                INDArray expected = Nd4j.create(5);
                INDArray a = Nd4j.create(3);
                INDArray b = Nd4j.create(2)
        ) {
            this.testConcat(expected, a, b, 0);
        }
    }

    /**
     * Concat 3D: (2, 3, 5) + (2, 6, 5) → (2, 9, 5)
     */
    @Test
    public void test6() throws Exception {
        try (
                INDArray expected = Nd4j.create(2, 9, 5);
                INDArray a = Nd4j.create(2, 3, 5);
                INDArray b = Nd4j.create(2, 6, 5)
        ) {
            this.testConcat(expected, a, b, -2);
        }
    }


    private void testConcat(INDArray expected, INDArray a, INDArray b, long axis) throws Exception {
        try (HWAcceleratedSession session = new HWAcceleratedSession(null)) {
            HWAcceleratedConcatV13 operator = new HWAcceleratedConcatV13();
            INDArray y = operator.concat(java.util.List.of(a, b), axis);
            System.out.println(String.format("{Expected: %s} - {Actual: %s}",
                    expected.shapeInfoToString(), y.shapeInfoToString()));
            assertArrayEquals(expected.shape(), y.shape());
        }
    }
}


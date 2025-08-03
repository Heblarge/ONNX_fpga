package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.exception.ND4JIllegalStateException;

public class DL4JReshapeV13Test extends DL4JTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testBasicReshape() {
        INDArray input = Nd4j.create(new float[]{1, 2, 3, 4, 5, 6}, new long[]{2, 3});
        INDArray shape = Nd4j.createFromArray(3L, 2L);
        INDArray expected = input.dup().reshape(3, 2);
        this.testReshape(expected, input, shape);
    }

    @Test
    public void testInferDimension() {
        INDArray input = Nd4j.create(2, 3);
        INDArray shape = Nd4j.createFromArray(-1L);
        INDArray expected = input.dup().reshape(6);
        this.testReshape(expected, input, shape);
    }

    @Test
    public void testCopyDimension() {
        INDArray input = Nd4j.create(2, 3);
        INDArray shape = Nd4j.createFromArray(0L, -1L);
        INDArray expected = input.dup().reshape(2, 3);
        this.testReshape(expected, input, shape);
    }

    @Test
    public void testInvalidShapeThrowsException() {
        INDArray input = Nd4j.create(2, 3);
        INDArray shape = Nd4j.createFromArray(4L, 2L);
        thrown.expect(ND4JIllegalStateException.class);
        this.testReshape(null, input, shape);
    }

    @Test
    public void testSliceAndReshape_64x64_to_32x2x64() {
        int length = 8192 * 1024;
        float[] inputDataFlat = new float[length];
        for (int i = 0; i < length; i++) {
            inputDataFlat[i] = i;
        }
        INDArray inputTensor = Nd4j.create(inputDataFlat, new long[]{8192, 1024});
        long[] newShape = new long[]{32, 256, 1024};
        INDArray shapeTensor = Nd4j.createFromArray(newShape);
        INDArray expectedTensor = Nd4j.create(inputDataFlat, newShape);
        this.testReshape(expectedTensor, inputTensor, shapeTensor);
    }


    private void testReshape(INDArray expected, INDArray input, INDArray shape) {
        DL4JReshapeV13 operator = new DL4JReshapeV13();
        INDArray actual = operator.reshape(input, shape);

        if (expected != null) {
            System.out.println(String.format("\n--- Testing Reshape ---\n{Expected shape: %s} - {Actual shape: %s}",
                    Arrays.toString(expected.shape()), Arrays.toString(actual.shape())));

            System.out.println("Expected values:\n" + expected);
            System.out.println("Actual values:\n" + actual);

            assertArrayEquals("Shape mismatch", expected.shape(), actual.shape());
            assertTrue("Value mismatch", expected.equals(actual));
        }
    }
}
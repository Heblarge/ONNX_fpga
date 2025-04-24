package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.junit.Assert.assertArrayEquals;

public class DL4JNegV13Test extends DL4JTestCase {

    private void testNeg(INDArray expected, INDArray input) throws Exception {
        DL4JNegV13 op = new DL4JNegV13();
        INDArray result = op.neg(input);

        System.out.printf("Expected: %s, Actual: %s%n", expected.shapeInfoToString(), result.shapeInfoToString());
        assertArrayEquals(expected.shape(), result.shape());
        assertArrayEquals(expected.toDoubleVector(), result.toDoubleVector(), 1e-6);
    }

    @Test
    public void testScalarInput() throws Exception {
        INDArray input = Nd4j.scalar(5.0);
        INDArray expected = Nd4j.scalar(-5.0);

        testNeg(expected, input);
    }

    @Test
    public void testZeroInput() throws Exception {
        INDArray input = Nd4j.scalar(0.0);
        INDArray expected = Nd4j.scalar(0.0); // -0 = 0

        testNeg(expected, input);
    }

    @Test
    public void testVectorInput() throws Exception {
        INDArray input = Nd4j.create(new double[]{-1.0, 0.0, 1.0, 2.5});
        INDArray expected = Nd4j.create(new double[]{1.0, 0.0, -1.0, -2.5});

        testNeg(expected, input);
    }

    @Test
    public void testMatrixInput() throws Exception {
        INDArray input = Nd4j.create(new double[][]{{-10.0, 0.0}, {10.0, 100.0}});
        INDArray expected = Nd4j.create(new double[][]{{10.0, 0.0}, {-10.0, -100.0}});

        testNeg(expected, input);
    }

    @Test
    public void testHighDimInput() throws Exception {
        INDArray input = Nd4j.create(2, 3, 4).assign(0.5);
        INDArray expected = Nd4j.create(2, 3, 4).assign(-0.5);

        testNeg(expected, input);
    }

    @Test
    public void testLargeValues() throws Exception {
        // 测试大数值输入
        INDArray largePos = Nd4j.scalar(Double.MAX_VALUE);
        INDArray largeNeg = Nd4j.scalar(-Double.MAX_VALUE);

        INDArray expectedLargePos = Nd4j.scalar(-Double.MAX_VALUE);
        INDArray expectedLargeNeg = Nd4j.scalar(Double.MAX_VALUE);

        testNeg(expectedLargePos, largePos);
        testNeg(expectedLargeNeg, largeNeg);
    }

    @Test
    public void testSpecialValues() throws Exception {
        // 测试特殊值
        INDArray nan = Nd4j.scalar(Double.NaN);
        INDArray posInf = Nd4j.scalar(Double.POSITIVE_INFINITY);
        INDArray negInf = Nd4j.scalar(Double.NEGATIVE_INFINITY);

        INDArray expectedNan = Nd4j.scalar(Double.NaN);
        INDArray expectedPosInf = Nd4j.scalar(Double.NEGATIVE_INFINITY);
        INDArray expectedNegInf = Nd4j.scalar(Double.POSITIVE_INFINITY);

        testNeg(expectedNan, nan);
        testNeg(expectedPosInf, posInf);
        testNeg(expectedNegInf, negInf);
    }
}
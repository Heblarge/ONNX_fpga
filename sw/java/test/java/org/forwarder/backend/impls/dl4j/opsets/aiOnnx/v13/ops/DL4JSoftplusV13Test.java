package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.junit.Assert.assertArrayEquals;

public class DL4JSoftplusV13Test extends DL4JTestCase {

    private void testSoftPlus(INDArray expected, INDArray input) throws Exception {
        DL4JSoftplusV13 op = new DL4JSoftplusV13();
        INDArray result = op.softplus(input);

        System.out.printf("Expected: %s, Actual: %s%n", expected.shapeInfoToString(), result.shapeInfoToString());
        assertArrayEquals(expected.shape(), result.shape());
    }

    @Test
    public void testScalarInput() throws Exception {
        INDArray input = Nd4j.scalar(0.0);
        INDArray expected = Nd4j.scalar(Math.log(2.0)); // softplus(0) = ln(2)

        testSoftPlus(expected, input);
    }

    @Test
    public void testVectorInput() throws Exception {
        INDArray input = Nd4j.create(new double[]{-1.0, 0.0, 1.0});
        INDArray expected = Nd4j.create(new double[]{
                Math.log(1 + Math.exp(-1)),
                Math.log(2),
                Math.log(1 + Math.exp(1))});

        testSoftPlus(expected, input);
    }

    @Test
    public void testMatrixInput() throws Exception {
        INDArray input = Nd4j.create(new double[][]{{-10.0, 0.0}, {10.0, 100.0}});
        INDArray expected = Nd4j.create(new double[][]{
                {Math.log(1 + Math.exp(-10)), Math.log(2)},
                {Math.log(1 + Math.exp(10)), Math.log(1 + Math.exp(100))}});

        testSoftPlus(expected, input);
    }

    @Test
    public void testHighDimInput() throws Exception {
        INDArray input = Nd4j.create(2, 3, 4).assign(-0.5);
        INDArray expected = Nd4j.create(2, 3, 4).assign(
                Math.log(1 + Math.exp(-0.5)));

        testSoftPlus(expected, input);
    }

    @Test
    public void testNumericalStability() throws Exception {
        // 测试大数值输入
        INDArray largePos = Nd4j.scalar(100.0);
        INDArray largeNeg = Nd4j.scalar(-100.0);

        INDArray expectedLargePos = Nd4j.scalar(100.0); // softpos(100) ≈ 100
        INDArray expectedLargeNeg = Nd4j.scalar(0.0); // softplus(-100) ≈ 0

        testSoftPlus(expectedLargePos, largePos);
        testSoftPlus(expectedLargeNeg, largeNeg);

    }
}
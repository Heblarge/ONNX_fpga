package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.Arrays;
import org.nd4j.linalg.ops.transforms.Transforms;

public class DL4JNegSoftplusV13Test extends DL4JTestCase {

    /**
     * 统一的辅助测试方法
     */
    private void testNegSoftplus(INDArray expected, INDArray input) throws Exception {
        DL4JNegSoftplusV13 op = new DL4JNegSoftplusV13();
        INDArray result = op.negSoftplus(input);

        System.out.println("\n--- Testing NegSoftplus ---");
        System.out.printf("Input Shape: %s, Result Shape: %s%n",
                Arrays.toString(input.shape()), Arrays.toString(result.shape()));

        // 断言：形状必须一致
        assertArrayEquals("Shape mismatch", expected.shape(), result.shape());

        // 断言：数值在误差范围内相等 (由于涉及Exp/Log，容差设为 1e-4)
        assertTrue("Value mismatch exceeds tolerance 1e-4", expected.equalsWithEps(result, 1e-4f));
    }

    @Test
    public void testScalarZero() throws Exception {
        // Softplus(0) = ln(2), NegSoftplus(0) = -ln(2)
        INDArray input = Nd4j.scalar(0.0);
        INDArray expected = Nd4j.scalar(-Math.log(2.0));
        testNegSoftplus(expected, input);
    }

    @Test
    public void testScalarPositive() throws Exception {
        INDArray input = Nd4j.scalar(1.0);
        INDArray expected = Nd4j.scalar(-Math.log(1 + Math.exp(1.0)));
        testNegSoftplus(expected, input);
    }

    @Test
    public void testVectorInput() throws Exception {
        // 测试正数、负数和零的组合
        INDArray input = Nd4j.create(new double[]{-1.0, 0.0, 2.0});
        INDArray expected = Nd4j.create(new double[]{
                -Math.log(1 + Math.exp(-1.0)),
                -Math.log(2.0),
                -Math.log(1 + Math.exp(2.0))
        });
        testNegSoftplus(expected, input);
    }

    @Test
    public void testMatrixInput() throws Exception {
        INDArray input = Nd4j.create(new double[][]{{-2.0, 0.5}, {3.0, -0.5}});
        INDArray expected = Nd4j.create(new double[][]{
                {-Math.log(1 + Math.exp(-2.0)), -Math.log(1 + Math.exp(0.5))},
                {-Math.log(1 + Math.exp(3.0)), -Math.log(1 + Math.exp(-0.5)) }
        });
        testNegSoftplus(expected, input);
    }

    @Test
    public void testNumericalStabilityLargePositive() throws Exception {
        // 对于大正数 x, Softplus(x) ≈ x, 所以 NegSoftplus(x) ≈ -x
        // 如果实现不稳，exp(100) 会溢出导致结果变成 -Infinity 或 NaN
        INDArray input = Nd4j.scalar(100.0);
        INDArray expected = Nd4j.scalar(-100.0);

        System.out.println("\n--- Running Numerical Stability Test (Large Positive) ---");
        testNegSoftplus(expected, input);
    }

    @Test
    public void testNumericalStabilityLargeNegative() throws Exception {
        // 对于大负数 x, Softplus(x) ≈ 0, 所以 NegSoftplus(x) ≈ 0
        INDArray input = Nd4j.scalar(-100.0);
        INDArray expected = Nd4j.scalar(0.0);

        System.out.println("\n--- Running Numerical Stability Test (Large Negative) ---");
        testNegSoftplus(expected, input);
    }

    @Test
    public void testHighDimTensor() throws Exception {
        // 测试 1x32x512 的典型深度学习中间层形状
        long[] shape = new long[]{1, 32, 512};
        INDArray input = Nd4j.rand(shape).sub(0.5); // 随机分布在 [-0.5, 0.5]

        // 使用原生数学逻辑计算期望值
        // 这里需要注意，ND4J 的基础操作是逐元素的
        // expected = -ln(1 + exp(input))
        INDArray softplus = Transforms.log(Transforms.exp(input.castTo(org.nd4j.linalg.api.buffer.DataType.DOUBLE)).add(1.0));
        INDArray expected = softplus.neg().castTo(org.nd4j.linalg.api.buffer.DataType.FLOAT);

        System.out.println("\n--- Running Large 3D Tensor Test (1x32x512) ---");
        testNegSoftplus(expected, input);
    }
}
package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

public class DL4JNegV13Test extends DL4JTestCase {

    /**
     * 统一的辅助测试方法
     * @param expected 期望的结果张量
     * @param input    输入的张量
     * @throws Exception
     */
    private void testNeg(INDArray expected, INDArray input) throws Exception {
        DL4JNegV13 op = new DL4JNegV13();
        INDArray result = op.neg(input);

        System.out.printf("Testing Neg - Expected Shape: %s, Actual Shape: %s%n",
                expected.shapeInfoToString(), result.shapeInfoToString());

        // 断言：形状必须一致
        assertArrayEquals(expected.shape(), result.shape());

        // 断言：数值在误差范围内相等 (适用于任意维度)
        assertTrue(expected.equalsWithEps(result, 1e-6));
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
        INDArray expected = Nd4j.scalar(0.0); // -0 等于 0
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
    public void testLargeVolumeInput() throws Exception {
        // 创建一个包含100万个随机数的大矩阵 (1000x1000)
        INDArray input = Nd4j.rand(1000, 1000);

        // 期望的结果就是输入矩阵逐元素乘以 -1
        INDArray expected = input.mul(-1);

        System.out.println("\n--- Running Large Volume Test (1,000,000 elements) ---");
        testNeg(expected, input);
    }



    @Test
    public void testLarge3DTensor() throws Exception {
        long[] shape = new long[]{1, 32, 512};
        // 创建一个包含 1 * 32 * 512 = 16384 个随机数的张量
        INDArray input = Nd4j.rand(shape);

        // 期望的结果就是输入张量逐元素乘以 -1
        INDArray expected = input.mul(-1);

        System.out.println("\n--- Running Large 3D Tensor Test (1x32x512) ---");
        testNeg(expected, input);
    }
}
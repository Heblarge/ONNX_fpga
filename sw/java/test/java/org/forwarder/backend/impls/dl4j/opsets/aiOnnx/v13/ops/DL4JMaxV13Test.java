package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.forwarder.backend.impls.dl4j.DL4JSession;
import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class DL4JMaxV13Test extends DL4JTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testMaxBroadcastableInputs() throws Exception {
        // 测试广播 + 多输入张量最大值
        this.testMax(
                Nd4j.create(new float[] {2.0f, 5.0f, 4.0f}),
                Arrays.asList(
                        Nd4j.create(new float[] {1.0f, 5.0f, 2.0f}),
                        Nd4j.create(new float[] {2.0f, 3.0f, 4.0f})
                )
        );
    }

    @Test
    public void testMaxIdenticalShapes() throws Exception {
        // 测试同形状张量
        this.testMax(
                Nd4j.create(new float[][] {
                        {3.0f, 5.0f},
                        {4.5f, 6.0f}
                }),
                Arrays.asList(
                        Nd4j.create(new float[][] {
                                {1.0f, 5.0f},
                                {4.5f, 2.0f}
                        }),
                        Nd4j.create(new float[][] {
                                {3.0f, 1.2f},
                                {2.2f, 6.0f}
                        })
                )
        );
    }

    @Test
    public void testMaxSingleInput() throws Exception {
        // 单个输入张量，输出应与输入一致
        INDArray input = Nd4j.create(new float[] {1.1f, 2.2f, 3.3f});
        this.testMax(input, Arrays.asList(input));
    }

    @Test
    public void testMaxMultipleInputs() throws Exception {
        // 构造 10 个形状相同的张量，每个元素值逐渐增大
        List<INDArray> inputs = Arrays.asList(
                Nd4j.create(new float[] {1, 2, 3}),
                Nd4j.create(new float[] {2, 3, 4}),
                Nd4j.create(new float[] {3, 4, 5}),
                Nd4j.create(new float[] {4, 5, 6}),
                Nd4j.create(new float[] {5, 6, 7}),
                Nd4j.create(new float[] {6, 7, 8}),
                Nd4j.create(new float[] {7, 8, 9}),
                Nd4j.create(new float[] {8, 9, 10}),
                Nd4j.create(new float[] {9, 11, 11}),
                Nd4j.create(new float[] {10, 10, 12})  // 最大值
        );

        INDArray expected = Nd4j.create(new float[] {10, 11, 12});
        this.testMax(expected, inputs);
    }

    protected void testMax(INDArray expected, List<INDArray> inputTensors) throws Exception {
        try (DL4JSession session = new DL4JSession(null)) {
            INDArray actual = this.executeOperator(inputTensors);
            System.out.println(String.format("{Expected: %s} - {Actual: %s}", expected, actual));
            assertTrue("Expected: " + expected + " but got: " + actual, actual.equals(expected));
        }
    }

    protected INDArray executeOperator(List<INDArray> inputTensors) {
        DL4JMaxV13 operator = new DL4JMaxV13();
        return operator.elementwiseMax(inputTensors);
    }
}

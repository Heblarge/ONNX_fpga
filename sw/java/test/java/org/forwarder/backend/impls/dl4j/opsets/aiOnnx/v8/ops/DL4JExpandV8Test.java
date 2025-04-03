package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v8.ops;

import static org.junit.Assert.assertTrue;

import org.forwarder.backend.impls.dl4j.DL4JSession;
import org.forwarder.backend.impls.dl4j.DL4JTestCase;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.opsets.operator.OperatorOutputs;

/**
 * Unit test for ONNX ExpandV8 operator using ND4J backend.
 */
public class DL4JExpandV8Test extends DL4JTestCase {

    @Test
    public void testExpandBasic() throws Exception {
        // 输入：3x1，目标 shape：3x4
        long[] targetShape = new long[]{3, 4};

        INDArray input = Nd4j.arange(1, 4).reshape(3, 1); // 不 try-with-resources
        INDArray shape = Nd4j.createFromArray(targetShape);

        // 期望输出：形状应该是 3x4，值应该是行重复
        INDArray expected = Nd4j.create(new float[][]{
                {1, 1, 1, 1},
                {2, 2, 2, 2},
                {3, 3, 3, 3}
        });

        INDArray actual = this.testExpand(input, shape);

        System.out.println(String.format(
                "[Expand] Expected shape: %s, Actual shape: %s",
                expected.shapeInfoToString(),
                actual.shapeInfoToString()
        ));

        assertArrayEquals(expected.shape(), actual.shape());
        assertTrue(actual.equals(expected));
    }

    private INDArray testExpand(INDArray input, INDArray shapeTensor) throws Exception {
        try (DL4JSession session = new DL4JSession(null)) {
            DL4JExpandV8 operator = new DL4JExpandV8();

            // 构造 ONNX-style 输入封装
            Inputs.Input input0 = Inputs.Input.wrap("input", null, input);
            Inputs.Input input1 = Inputs.Input.wrap("shape", null, shapeTensor);
            Inputs inputs = Inputs.wrap(new Inputs.Input[]{input0, input1});

            //return operator.forward(null, inputs).get(0);

//            OperatorOutputs<INDArray> ans = operator.forward(null, inputs);
//            INDArray a = ans.get(0);
            return operator.forward(null, inputs).get(0).detach();
        }
    }
}

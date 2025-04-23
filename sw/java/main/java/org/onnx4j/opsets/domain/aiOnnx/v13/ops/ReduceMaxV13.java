package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.ReduceMaxV1;
import org.onnx4j.opsets.domain.aiOnnx.v11.ops.ReduceMaxV11.ReduceMaxInputsV11;
import org.onnx4j.opsets.domain.aiOnnx.v11.ops.ReduceMaxV11.ReduceMaxOutputV11;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;

/**
 * ReduceMax Operator v12
 *
 * <p>
 * Computes the max of the input tensor's element along the provided axes. The
 * resulted tensor has the same rank as the input if keepdims equal 1. If
 * keepdims equal 0, then the resulted tensor have the reduced dimension pruned.
 *
 * <p>
 * The above behavior is similar to numpy, with the exception that numpy default
 * keepdims to False instead of True.
 *
 * @author HarryLee {@literal <formaten@qq.com>}
 * @version 12
 * @since Version 1 of the default ONNX operator set
 * @see <a href=
 *      "https://github.com/onnx/onnx/blob/master/docs/Changelog.md#ReduceMax-12">
 *      ONNX.Changelog.md</a>
 * @see <a href=
 *      "https://github.com/onnx/onnx/blob/master/docs/Operators.md#ReduceMax">
 *      ONNX.Operators.md</a>
 */
public interface ReduceMaxV13 extends ReduceMaxV1, AiOnnxOperatorV13 {

    class ReduceMaxInputsV13<T_TENSOR> extends ReduceMaxInputsV11<T_TENSOR> {

        public ReduceMaxInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
        }

    }

    /**
     * Outputs for operator execution (forward & backward) s
     *
     * @param <T_TENSOR>
     *            The backend tensor object.
     */
    class ReduceMaxOutputV13<T_TENSOR> extends ReduceMaxOutputV11<T_TENSOR> {

        public ReduceMaxOutputV13(T_TENSOR output) {
            super(output);
        }

    }

}
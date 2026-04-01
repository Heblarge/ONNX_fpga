package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.SubExpV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

/**
 * DL4J 实现的 SubExp 融合算子: Exp(A - B)
 */
public class DL4JSubExpV13 extends DL4JAiOnnxOperator implements SubExpV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        // 使用我们之前定义的 SubExpInputsV13 接口解析输入
        SubExpInputsV13<INDArray> castedInputs = new SubExpInputsV13<>(node, inputs);

        INDArray a = castedInputs.getA();
        INDArray b = castedInputs.getB();

        // 执行融合逻辑：先减后指数
        return new SubExpOutputV13<>(this.subExp(a, b));
    }

    /**
     * 计算 Exp(a - b)
     * 这里利用 ND4J 的 inplace sub 减少内存分配，然后进行变换
     */
    protected INDArray subExp(INDArray a, INDArray b) {
        // 使用 a.sub(b) 会返回新数组，a.subi(b) 会修改原数组。
        // 为了安全起见，通常在 forward 中使用 a.sub(b)
        return Transforms.exp(a.sub(b));
    }

}
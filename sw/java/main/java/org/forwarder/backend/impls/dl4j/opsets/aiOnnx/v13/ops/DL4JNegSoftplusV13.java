package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.strict.SoftPlus;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.NegSoftplusV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class DL4JNegSoftplusV13 extends DL4JAiOnnxOperator implements NegSoftplusV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        NegSoftplusInputsV13<INDArray> castedInputs = new NegSoftplusInputsV13<>(node, inputs);
        INDArray x = castedInputs.getX();
        return new NegSoftplusOutputV13<>(this.negSoftplus(x));
    }

    public INDArray negSoftplus(INDArray x) {
        // 1. 转换类型保证精度
        INDArray xDouble = x.castTo(DataType.DOUBLE);

        // 2. 创建结果数组
        // 在 beta6 中，如果 createUninitialized 报错，直接用 Nd4j.create
        // 或者是 Nd4j.create(xDouble.shape(), xDouble.stride(), 0, xDouble.ordering(), DataType.DOUBLE)
        // 最简单可靠的写法：
        INDArray softplusResult = Nd4j.create(DataType.DOUBLE, xDouble.shape());

        // 3. 执行 SoftPlus 算子
        // 注意：某些 beta6 版本 SoftPlus 构造函数为 SoftPlus(input, output)
        Nd4j.getExecutioner().exec(new SoftPlus(xDouble, softplusResult));

        // 4. 取负并转回原始类型
        return softplusResult.neg().castTo(x.dataType());
    }
}
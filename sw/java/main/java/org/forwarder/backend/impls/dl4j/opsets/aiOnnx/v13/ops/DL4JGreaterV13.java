package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.custom.GreaterThan;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.GreaterV13;
import org.onnx4j.opsets.operator.OperatorOutputs;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.buffer.DataType;

public class DL4JGreaterV13 extends DL4JAiOnnxOperator implements GreaterV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        GreaterInputsV13<INDArray> castedInputs = new GreaterInputsV13<>(node, inputs);
        INDArray result = this.greater(castedInputs.getA(), castedInputs.getB());
        return new GreaterOutputV13<>(result);
    }

    /**
     * 执行 A > B 的比较运算，返回布尔张量
     */
    protected INDArray greater(INDArray a, INDArray b) {
        // 获取广播后形状
        long[] shapeA = a.shape();
        long[] shapeB = b.shape();
        long[] broadcastShape = Shape.broadcastOutputShape(shapeA, shapeB);

        // 创建布尔类型输出张量
        INDArray output = Nd4j.create(broadcastShape).castTo(DataType.BOOL);

        // 构建 op 并执行
        DynamicCustomOp op = DynamicCustomOp.builder("greater")
                .addInputs(a, b)
                .addOutputs(output)
                .build();

        Nd4j.getExecutioner().exec(op);

        return output;
    }
}


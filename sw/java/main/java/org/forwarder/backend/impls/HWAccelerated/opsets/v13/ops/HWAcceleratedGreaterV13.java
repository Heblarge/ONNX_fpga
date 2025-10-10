package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.GreaterV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class HWAcceleratedGreaterV13 extends HWAcceleratedOperator implements GreaterV13 {

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
        INDArray aProc = a;
        INDArray bProc = b;

        // 检查两个输入的数据类型是否一致
        if (a.dataType() != b.dataType()) {
            System.out.printf(
                    "WARNING: Greater operator received mismatched data types (%s vs %s). Casting second input to match first.%n",
                    a.dataType(), b.dataType()
            );
            // 如果不一致，将第二个张量(b)的类型转换为与第一个(a)一致
            bProc = b.castTo(a.dataType()).dup();
        }

        // 使用处理后、类型一致的张量 aProc 和 bProc
        long[] shapeA = aProc.shape();
        long[] shapeB = bProc.shape();
        long[] broadcastShape = Shape.broadcastOutputShape(shapeA, shapeB);

        INDArray output = Nd4j.create(DataType.BOOL, broadcastShape);

        DynamicCustomOp op = DynamicCustomOp.builder("greater")
                .addInputs(aProc, bProc)
                .addOutputs(output)
                .build();

        Nd4j.getExecutioner().exec(op);

        return output;
    }
}


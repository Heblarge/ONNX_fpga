package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v9.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v9.ops.WhereV9;
import org.onnx4j.opsets.operator.OperatorOutputs;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.api.ops.DynamicCustomOp;


public class DL4JWhereV9 extends DL4JAiOnnxOperator implements WhereV9 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        WhereInputsV9<INDArray> parsed = new WhereInputsV9<>(node, inputs);
        INDArray result = where(parsed.getCondition(), parsed.getX(), parsed.getY());
        return new WhereOutputV9<>(result);
    }

    protected INDArray where(INDArray condition, INDArray x, INDArray y) {
        // 计算广播后形状（两两进行）
        long[] shapeCond = condition.shape();
        long[] shapeX = x.shape();
        long[] shapeY = y.shape();

        long[] shapeCX = Shape.broadcastOutputShape(shapeCond, shapeX);
        long[] finalShape = Shape.broadcastOutputShape(shapeCX, shapeY);

        // 创建输出张量（用 x 的数据类型）
        INDArray output = Nd4j.createUninitialized(x.dataType(), finalShape);

        DynamicCustomOp op = DynamicCustomOp.builder("select")
                .addInputs(condition, x, y)
                .addOutputs(output)
                .build();

        Nd4j.getExecutioner().exec(op);
        return output;
    }
}

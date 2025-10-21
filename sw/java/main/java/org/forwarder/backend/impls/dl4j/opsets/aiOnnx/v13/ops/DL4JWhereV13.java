package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;


import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.WhereV13;
import org.onnx4j.opsets.operator.OperatorOutputs;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.tensor.DataType;

import java.util.Arrays;

/**
 * HWAcceleratedWhereV13
 *
 * v13 版本的 Where Operator，纯软件实现，支持广播
 * x、y 类型与 condition 相同
 */
public class DL4JWhereV13 extends DL4JAiOnnxOperator implements WhereV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        WhereInputsV13<INDArray> parsed = new WhereInputsV13<>(node, inputs);
        INDArray result = where(parsed.getCondition(), parsed.getX(), parsed.getY());
        return new WhereOutputV13<>(result);
    }

    /**
     * v13 Where 操作，支持广播
     */
    protected INDArray where(INDArray condition, INDArray x, INDArray y) {
        // 1. 计算最终广播 shape
        long[] shapeCondX = broadcastShapes(condition.shape(), x.shape());
        long[] finalShape = broadcastShapes(shapeCondX, y.shape());

        // 2. 广播所有输入
        INDArray condB = condition.broadcast(finalShape);
        INDArray xB = x.broadcast(finalShape);
        INDArray yB = y.broadcast(finalShape);

        // 3. 将布尔 condition 转为浮点 0/1
        INDArray mask = condB.castTo(xB.dataType());

        // 4. 输出 = mask * x + (1 - mask) * y
        return mask.mul(xB).add(mask.rsub(1).mul(yB));
    }

    /**
     * 自实现广播 shape 函数，不依赖 ND4J Shape 类
     */
    private long[] broadcastShapes(long[] a, long[] b) {
        int maxRank = Math.max(a.length, b.length);
        long[] result = new long[maxRank];
        for (int i = 1; i <= maxRank; i++) {
            long dimA = (i <= a.length) ? a[a.length - i] : 1;
            long dimB = (i <= b.length) ? b[b.length - i] : 1;
            if (dimA != dimB && dimA != 1 && dimB != 1) {
                throw new IllegalArgumentException(
                        "Shapes " + Arrays.toString(a) + " and " + Arrays.toString(b) + " are not broadcastable."
                );
            }
            result[maxRank - i] = Math.max(dimA, dimB);
        }
        return result;
    }
}

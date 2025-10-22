package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedQuantizedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.WhereV13;
import org.onnx4j.opsets.operator.OperatorOutputs;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.tensor.DataType;

import java.util.Arrays;
import java.util.List;

public class HWAcceleratedWhereV13 extends HWAcceleratedQuantizedOperator implements WhereV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        WhereInputsV13<INDArray> castedInputs = new WhereInputsV13<>(node, inputs);

        INDArray condition = castedInputs.getCondition();
        INDArray matrixX = castedInputs.getX();
        INDArray matrixY = castedInputs.getY();

        Graph graph = node.getGraph();
        List<Long> targetInputShifts = castedInputs.getFpgaInShift();

        long targetInputShiftX = targetInputShifts.get(1);
        long targetInputShiftY = targetInputShifts.get(2);

        long targetOutputShift = castedInputs.getFpgaOutShift().get(0);

        String inputXName = node.getInputNames()[1];
        long sourceShiftX = this.getProducerOutputShift(graph, inputXName, targetInputShiftX);
        String inputYName = node.getInputNames()[2];
        long sourceShiftY = this.getProducerOutputShift(graph, inputYName, targetInputShiftY);
        INDArray result = where(
                condition,
                matrixX,
                matrixY,
                sourceShiftX,
                sourceShiftY,
                targetOutputShift
        );

        return new WhereOutputV13<>(result);
    }


    protected INDArray where(INDArray condition, INDArray x, INDArray y,
                             long sourceShiftX, long sourceShiftY,
                             long commonShift) {
        int rescaleAmountX = (int) (sourceShiftX - commonShift);
        INDArray rescaledX = (rescaleAmountX == 0)
                ? x
                : (rescaleAmountX < 0
                ? x.dup().mul(1L << -rescaleAmountX)
                : x.dup().div(1L << rescaleAmountX)
        );

        int rescaleAmountY = (int) (sourceShiftY - commonShift);
        INDArray rescaledY = (rescaleAmountY == 0)
                ? y
                : (rescaleAmountY < 0
                ? y.dup().mul(1L << -rescaleAmountY)
                : y.dup().div(1L << rescaleAmountY)
        );

        // 1. 计算最终广播 shape
        long[] shapeCondX = broadcastShapes(condition.shape(), rescaledX.shape());
        long[] finalShape = broadcastShapes(shapeCondX, rescaledY.shape());

        // 2. 广播所有输入
        INDArray condB = condition.broadcast(finalShape);
        INDArray xB = rescaledX.broadcast(finalShape);
        INDArray yB = rescaledY.broadcast(finalShape);

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

package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedQuantizedOperator;
import org.forwarder.backend.impls.HWAccelerated.opsets.v4.ops.HWAcceleratedConcatV4;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ConcatV13;
import org.onnx4j.opsets.operator.OperatorOutputs;
import java.util.List;

public class HWAcceleratedConcatV13 extends HWAcceleratedQuantizedOperator implements ConcatV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        ConcatInputsV13<INDArray> castedInputs = new ConcatInputsV13<>(node, inputs);
        List<INDArray> inputList = castedInputs.getInputs();
        long axis = normalizeAxis(castedInputs.getAxis(), inputList.get(0).rank());
        Graph graph = node.getGraph();
        List<Long> targetInputShifts = castedInputs.getFpgaInShift();
        long targetInputShiftA = targetInputShifts.get(0);
        long targetInputShiftB = targetInputShifts.get(1);

        String inputAName = node.getInputNames()[0];
        long sourceShiftA = this.getProducerOutputShift(graph, inputAName, targetInputShiftA);
        String inputBName = node.getInputNames()[1];
        long sourceShiftB = this.getProducerOutputShift(graph, inputBName, targetInputShiftB);

        long targetOutputShift = castedInputs.getFpgaOutShift().get(0);

        INDArray concatenatedOutput = this.concat(
                inputList,
                axis,
                sourceShiftA,
                sourceShiftB,
                targetOutputShift
        );

        return new ConcatOutputV13<>(concatenatedOutput);
    }

    /**
     * 添加对新增的负 axis 输入的支持
     */
    private int normalizeAxis(long axis, int rank) {
        if (axis < 0) {
            axis += rank;
        }
        if (axis < 0 || axis >= rank) {
            throw new IllegalArgumentException(String.format(
                    "axis=%d illegal，dimension of the input tensor should be %d", axis, rank));
        }
        return (int) axis;
    }


    protected INDArray concat(List<INDArray> inputs, Long axisLong, long sourceShiftA, long sourceShiftB, long targetOutputShift) {
        int rank = inputs.get(0).rank();
        int axis = normalizeAxis(axisLong, rank);
        INDArray matrixA = inputs.get(0);
        INDArray matrixB = inputs.get(1);

        long[] shapeA = matrixA.shape();
        long[] shapeB = matrixB.shape();
        if (shapeA.length != shapeB.length) {
            throw new IllegalArgumentException(String.format(
                    "Input tensor ranks must match: %d != %d", shapeA.length, shapeB.length));
        }
        for (int dim = 0; dim < shapeA.length; dim++) {
            if (dim == axis) continue;
            if (shapeA[dim] != shapeB[dim]) {
                throw new IllegalArgumentException(String.format(
                        "Input tensor shape mismatch at dim %d: expected %d, got %d",
                        dim, shapeA[dim], shapeB[dim]));
            }
        }

        int rescaleAmountA = (int) (sourceShiftA - targetOutputShift);
        INDArray rescaledA = (rescaleAmountA == 0)
                ? matrixA
                : (rescaleAmountA < 0
                ? matrixA.mul(1L << -rescaleAmountA)
                : matrixA.div(1L << rescaleAmountA)
        );

        int rescaleAmountB = (int) (sourceShiftB - targetOutputShift);
        INDArray rescaledB = (rescaleAmountB == 0)
                ? matrixB
                : (rescaleAmountB < 0
                ? matrixB.mul(1L << -rescaleAmountB)
                : matrixB.div(1L << rescaleAmountB)
        );

        return Nd4j.concat(axis, rescaledA, rescaledB);
    }

}

package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.onnx4j.Inputs;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.NegV13;
import org.onnx4j.opsets.operator.OperatorOutputs;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedQuantizedOperator;


public class HWAcceleratedNegV13 extends HWAcceleratedQuantizedOperator implements NegV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        NegInputsV13<INDArray> castedInputs = new NegInputsV13<>(node, inputs);
        INDArray x = castedInputs.getX();
        Graph graph = node.getGraph();

        long targetInputShift = castedInputs.getFpgaInShift().get(0);
        String inputName = node.getInputNames()[0];
        long sourceShift = this.getProducerOutputShift(graph, inputName, targetInputShift);

        long targetOutputShift = castedInputs.getFpgaOutShift().get(0);
        return new NegOutputV13<>(this.neg(x, sourceShift, targetOutputShift));
    }

    public INDArray neg(INDArray x, long targetInputShift, long targetOutputShift) {
        int rescaleAmount = (int) (targetInputShift - targetOutputShift);
        INDArray rescaledX = (rescaleAmount == 0)
                ? x
                : (rescaleAmount < 0
                ? x.mul(1L << -rescaleAmount)
                : x.div(1L << rescaleAmount)
        );

        return rescaledX.neg();
    }
}
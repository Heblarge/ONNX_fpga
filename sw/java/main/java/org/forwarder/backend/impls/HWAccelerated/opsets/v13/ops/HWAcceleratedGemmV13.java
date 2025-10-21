package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedQuantizedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.onnx4j.Inputs;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.GemmV13;
import org.onnx4j.opsets.operator.OperatorOutputs;
import java.util.List;

/**
 * Implements the Gemm operation using a hardware accelerator.
 * This version uses standard Java arrays for padding and slicing and relies on
 * hardware instruction shifts for fixed-point scaling.
 */
public class HWAcceleratedGemmV13 extends HWAcceleratedQuantizedOperator implements GemmV13 {

    private final int HW_DIM_MULTIPLE = 32;

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        GeMMInputsV13<INDArray> castedInputs = new GeMMInputsV13<>(node, inputs);
        INDArray a = castedInputs.getA();
        INDArray b = castedInputs.getB();
        INDArray c = castedInputs.hasC() ? castedInputs.getC() : null;
        float alpha = castedInputs.getAlpha();
        float beta = castedInputs.getBeta();
        long transA = castedInputs.getTransA();
        long transB = castedInputs.getTransB();

        Graph graph = node.getGraph();
        List<Long> targetInputShifts = castedInputs.getFpgaInShift();
        long targetInputShiftA = targetInputShifts.get(0);
        long targetInputShiftB = targetInputShifts.get(1);
        String inputAName = node.getInputNames()[0];
        long sourceShiftA = this.getProducerOutputShift(graph, inputAName, targetInputShiftA);
        String inputBName = node.getInputNames()[1];
        long sourceShiftB = this.getProducerOutputShift(graph, inputBName, targetInputShiftB);
        long targetOutputShift = castedInputs.getFpgaOutShift().get(0);

        INDArray result = gemm(
                a, b, c, alpha, beta, transA, transB,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift
        );

        return new GeMMOutputV13<>(result);
    }

    protected INDArray gemm(INDArray A, INDArray B, INDArray C, float alpha, float beta, long transA, long transB, long sourceShiftA, long sourceShiftB, long targetInputShiftA, long targetInputShiftB, long targetOutputShift) {
        if (transA != 0L) { A = A.transpose(); }
        if (transB != 0L) { B = B.transpose(); }

        HWAcceleratedMatMulV13 matmulOp = new HWAcceleratedMatMulV13();
        INDArray matmulResult = matmulOp.matmul(A, B, sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift);
        INDArray Y = matmulResult.mul((int)alpha);

        if (C != null) {
            long[] yShape = Y.shape();
            if (!java.util.Arrays.equals(C.shape(), yShape)) {
                C = C.broadcast(yShape);
            }
            INDArray betaC = C.mul(beta);
            Y = Y.add(betaC);
        }

        return Y;
    }

}

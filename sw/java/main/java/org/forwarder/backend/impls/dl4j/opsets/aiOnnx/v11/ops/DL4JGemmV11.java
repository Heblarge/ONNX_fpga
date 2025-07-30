package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v11.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v11.ops.GemmV11;
import org.onnx4j.opsets.operator.OperatorOutputs;


public class DL4JGemmV11 extends DL4JAiOnnxOperator implements GemmV11 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        GeMMInputsV11<INDArray> castedInputs = new GeMMInputsV11<>(node, inputs);
        INDArray A      = castedInputs.getA();
        INDArray B      = castedInputs.getB();
        INDArray C      = castedInputs.hasC() ? castedInputs.getC() : null;
        float   alpha   = castedInputs.getAlpha();
        float   beta    = castedInputs.getBeta();
        long    transA  = castedInputs.getTransA();
        long    transB  = castedInputs.getTransB();

        INDArray result = gemm(A,B,C,alpha,beta,transA,transB);

        return new GeMMOutputV11<>(result);
    }

    protected INDArray gemm(INDArray A, INDArray B, INDArray C, float alpha, float beta, long transA, long transB) {
        if (transA != 0L) { A = A.transpose(); }
        if (transB != 0L) { B = B.transpose(); }

        INDArray Y = A.mmul(B).muli(alpha);

        if (C != null) {
            long[] yShape = Y.shape();
            if (!java.util.Arrays.equals(C.shape(), yShape)) {
                C = C.broadcast(yShape);
            }
            Y.addi(C.mul(beta));
        }

        return Y;
    }
}

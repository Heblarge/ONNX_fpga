package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.GemmV13;
import org.onnx4j.opsets.operator.OperatorOutputs;


public class DL4JGemmV13 extends DL4JAiOnnxOperator implements GemmV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        GeMMInputsV13<INDArray> castedInputs = new GeMMInputsV13<>(node, inputs);
        INDArray A      = castedInputs.getA();
        INDArray B      = castedInputs.getB();
        INDArray C      = castedInputs.hasC() ? castedInputs.getC() : null;
        float   alpha   = castedInputs.getAlpha();
        float   beta    = castedInputs.getBeta();
        long    transA  = castedInputs.getTransA();
        long    transB  = castedInputs.getTransB();

        INDArray result = gemm(A, B, C, alpha, beta, transA, transB);

        return new GeMMOutputV13<>(result);
    }

    protected INDArray gemm(INDArray A, INDArray B, INDArray C, float alpha, float beta, long transA, long transB) {

        if (transA != 0L) { A = A.transpose(); }
        if (transB != 0L) { B = B.transpose(); }

        INDArray matmulResult;

        if (A.rank() == 2 && B.rank() == 2) {
            matmulResult = A.mmul(B);
        } else if (A.rank() == 3 && B.rank() == 2) {
            long batchSize = A.size(0);
            long M = A.size(1);
            long K_A = A.size(2);
            long K_B = B.size(0);
            long N = B.size(1);

            if (K_A != K_B) {
                throw new IllegalArgumentException("Matrix shape mismatch for 3D x 2D MatMul: A's inner dim (" + K_A + ") must equal B's rows (" + K_B + ").");
            }
            INDArray a2D = A.reshape('c', batchSize * M, K_A);
            INDArray result2D = a2D.mmul(B);
            matmulResult = result2D.reshape('c', batchSize, M, N);
        } else if (A.rank() == 3 && B.rank() == 3) {
            long batchA = A.size(0);
            long batchB = B.size(0);
            if (batchA != batchB && batchA != 1 && batchB != 1) {
                throw new IllegalArgumentException("Batch dimensions are not compatible for 3D x 3D MatMul: A batch=" + batchA + ", B batch=" + batchB);
            }
            long finalBatch = Math.max(batchA, batchB);
            long M = A.size(1);
            long N = B.size(2);

            matmulResult = Nd4j.create(A.dataType(), new long[]{finalBatch, M, N}, 'c');

            for (int i = 0; i < finalBatch; i++) {
                INDArray sliceA = (batchA == 1) ? A.slice(0) : A.slice(i);
                INDArray sliceB = (batchB == 1) ? B.slice(0) : B.slice(i);

                INDArray product = sliceA.dup().mmul(sliceB.dup());

                matmulResult.putSlice(i, product);
            }
        }
        else {
            throw new IllegalArgumentException(
                    String.format("Unsupported GEMM operand ranks: A rank = %d, B rank = %d", A.rank(), B.rank())
            );
        }

        INDArray Y = matmulResult.mul(alpha);

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

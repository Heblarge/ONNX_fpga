package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.GemmV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

/**
 * Implements the Gemm operation using a hardware accelerator.
 * This version uses standard Java arrays for padding and slicing and relies on
 * hardware instruction shifts for fixed-point scaling.
 */
public class HWAcceleratedGemmV13 extends HWAcceleratedOperator implements GemmV13 {

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

        INDArray result = gemm(a, b, c, alpha, beta, transA, transB);

        return new GeMMOutputV13<>(result);
    }

    private int ceilToMultiple(int value, int multiple) {
        if (multiple == 0) return value;
        return ((value + multiple - 1) / multiple) * multiple;
    }

    protected INDArray gemm(INDArray A, INDArray B, INDArray C, float alpha, float beta, long transA, long transB) {
        if (transA != 0L) { A = A.transpose(); }
        if (transB != 0L) { B = B.transpose(); }

        INDArray Y = matmul2D(A, B).mul(alpha);

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

    private INDArray matmul2D(INDArray a, INDArray b) {
        long[] shapeA = a.shape();
        long[] shapeB = b.shape();

        int originalRowsA = (int) shapeA[0];
        int originalColsA = (int) shapeA[1];
        int originalRowsB = (int) shapeB[0];
        int originalColsB = (int) shapeB[1];

        if (originalColsA != originalRowsB) {
            throw new IllegalArgumentException(
                    String.format("Matrix shape mismatch for 2D MatMul: A's columns (%d) must equal B's rows (%d).", originalColsA, originalRowsB)
            );
        }

        int paddedRowsA = ceilToMultiple(originalRowsA, HW_DIM_MULTIPLE);
        int paddedColsA = ceilToMultiple(originalColsA, HW_DIM_MULTIPLE);
        int paddedColsB = ceilToMultiple(originalColsB, HW_DIM_MULTIPLE);

        int[][] paddedA = new int[paddedRowsA][paddedColsA];
        for(int i = 0; i < originalRowsA; i++) {
            for(int j = 0; j < originalColsA; j++) {
                paddedA[i][j] = Math.round(a.getFloat(i, j));
            }
        }

        int[][] paddedB = new int[paddedColsA][paddedColsB];
        for(int i = 0; i < originalRowsB; i++) {
            for(int j = 0; j < originalColsB; j++) {
                paddedB[i][j] = Math.round(b.getFloat(i, j));
            }
        }

        int[][] paddedResult = matMulOnAccelerator(paddedA, paddedB, paddedRowsA, paddedColsA, paddedColsB);

        float[][] finalResult = new float[originalRowsA][originalColsB];
        for(int i = 0; i < originalRowsA; i++) {
            for(int j = 0; j < originalColsB; j++) {
                finalResult[i][j] = paddedResult[i][j];
            }
        }

        return Nd4j.create(finalResult);
    }


    private int[][] matMulOnAccelerator(int[][] a, int[][] b, int rowsA, int colsA, int colsB){
        int fracWidth = AcceleratorSimInterface.acceleratorCfg().fracWidth();

        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "matmul",
                0,
                false,
                "none",
                0,
                0, 0, 0,
                rowsA,
                colsA,
                colsB
        );

        return AcceleratorSimInterface.runRefOneInst(a, b, instruction);
    }
}

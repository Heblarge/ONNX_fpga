package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
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
            HWAcceleratedAddV13 addOperator = new HWAcceleratedAddV13();
            Y = addOperator.add(Y, betaC);
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

        INDArray paddedA = Nd4j.zeros(paddedRowsA, paddedColsA);
        paddedA.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRowsA), NDArrayIndex.interval(0, originalColsA)}, a);

        INDArray paddedB = Nd4j.zeros(paddedColsA, paddedColsB);
        paddedB.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRowsB), NDArrayIndex.interval(0, originalColsB)}, b);

        INDArray paddedResult = matMulOnAccelerator(paddedA, paddedB, paddedRowsA, paddedColsA, paddedColsB);

        return paddedResult.get(NDArrayIndex.interval(0, originalRowsA), NDArrayIndex.interval(0, originalColsB));

    }


    private INDArray matMulOnAccelerator(INDArray a, INDArray b, int rowsA, int colsA, int colsB){

        int elementWidth = AcceleratorSimInterface.acceleratorCfg().elementWidth();
        int intWidth = AcceleratorSimInterface.acceleratorCfg().intWidth();
        long ELEMENT_INT_MAX = (1L << (elementWidth - 1)) - 1;
        long ELEMENT_INT_MIN = -(1L << (elementWidth - 1));
        double REAL_VALUE_MAX_RANGE = Math.pow(2, intWidth - 1);

        float maxAbsA = a.amaxNumber().floatValue();
        float maxAbsB = b.amaxNumber().floatValue();
        double maxPossibleOutput = (double)colsA * maxAbsA * maxAbsB;
//        if (maxPossibleOutput >= REAL_VALUE_MAX_RANGE) {
//            throw new ArithmeticException(String.format(
//                    "Potential Computation Overflow! Estimated max output value %.2f exceeds hardware range of +/-%.2f defined by intWidth=%d.",
//                    maxPossibleOutput, REAL_VALUE_MAX_RANGE, intWidth
//            ));
//        }

        int fracWidth = 9;
        double scaleFactor = Math.pow(2, fracWidth);

        int[][] fixedPointA = new int[rowsA][colsA];
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsA; j++) {
                double scaledValue = a.getFloat(i, j) * scaleFactor;
                if (scaledValue > ELEMENT_INT_MAX || scaledValue < ELEMENT_INT_MIN) {
                    throw new ArithmeticException("Input Overflow during scaling!");
                }
                fixedPointA[i][j] = (int)Math.round(scaledValue);
            }
        }

        int[][] fixedPointB = new int[colsA][colsB];
        for (int i = 0; i < colsA; i++) {
            for (int j = 0; j < colsB; j++) {
                double scaledValue = b.getFloat(i, j) * scaleFactor;
                if (scaledValue > ELEMENT_INT_MAX || scaledValue < ELEMENT_INT_MIN) {
                    throw new ArithmeticException("Input Overflow during scaling!");
                }
                fixedPointB[i][j] = (int)Math.round(scaledValue);
            }
        }

        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "matmul",
                0,
                false,
                "none",
                0,
                0,
                0,
                0,
                rowsA,
                colsA,
                colsB
        );
        int[][] hardwareResult = AcceleratorSimInterface.runRefOneInst(fixedPointA, fixedPointB, instruction);

        float[] output = new float[rowsA * colsB];
        double finalScaleFactor = scaleFactor * scaleFactor;
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                output[i * colsB + j] = (float) (hardwareResult[i][j] / finalScaleFactor);
            }
        }

        return Nd4j.create(output).reshape(rowsA, colsB);
    }
}

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
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.MatMulV13;
import org.onnx4j.opsets.operator.OperatorOutputs;
import scala.tools.nsc.doc.html.HtmlTags;

import java.util.List;


public class HWAcceleratedMatMulV13 extends HWAcceleratedOperator implements MatMulV13 {

    private final int HW_DIM_MULTIPLE = 32;

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        MatMulInputsV13<INDArray> castedInputs = new MatMulInputsV13<>(node, inputs);
        INDArray matrixA = castedInputs.getA();
        INDArray matrixB = castedInputs.getB();
        List<Float> fpgaInScales = castedInputs.getFpgaInScales();
        List<Long> fpgaInShift = castedInputs.getFpgaInShift();
        Float fpgaOutScale = castedInputs.getFpgaOutScale();
        Long fpgaOutShift = castedInputs.getFpgaOutShift();
        INDArray outputTensor = this.matmul(matrixA, matrixB, fpgaInShift, fpgaOutShift);
        return new MatMulOutputV13<>(outputTensor);
    }

    private int ceilToMultiple(int value, int multiple) {
        if (multiple == 0) return value;
        return ((value + multiple - 1) / multiple) * multiple;
    }

    public INDArray matmul(INDArray a, INDArray b, List<Long> fpgaInShift, Long fpgaOutShift) {
        if (a.rank() == 2 && b.rank() == 2) {
            return matmul2D(a, b, fpgaInShift, fpgaOutShift);
        } else if (a.rank() == 3 && b.rank() == 2) {
            return matmul3D2D(a, b, fpgaInShift, fpgaOutShift);
        } else if (a.rank() == 3 && b.rank() == 3) {
            return matmul3D(a, b, fpgaInShift, fpgaOutShift);
        } else {
            throw new IllegalArgumentException("Unsupported tensor rank for MatMul: A=" + a.rank() + ", B=" + b.rank());
        }
    }



    private INDArray matmul2D(INDArray a, INDArray b, List<Long> fpgaInShift, Long fpgaOutShift) {
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

        // 1. 计算填充后的尺寸 (向上取整到32的倍数)
        int paddedRowsA = ceilToMultiple(originalRowsA, HW_DIM_MULTIPLE);
        int paddedColsA = ceilToMultiple(originalColsA, HW_DIM_MULTIPLE);
        int paddedColsB = ceilToMultiple(originalColsB, HW_DIM_MULTIPLE);


        INDArray paddedA = Nd4j.zeros(paddedRowsA, paddedColsA);
        paddedA.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRowsA), NDArrayIndex.interval(0, originalColsA)}, a);

        INDArray paddedB = Nd4j.zeros(paddedColsA, paddedColsB);
        paddedB.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRowsB), NDArrayIndex.interval(0, originalColsB)}, b);

        INDArray paddedResult = matMulOnAccelerator(paddedA, paddedB, paddedRowsA, paddedColsA, paddedColsB, fpgaInShift, fpgaOutShift);

        return paddedResult.get(NDArrayIndex.interval(0, originalRowsA), NDArrayIndex.interval(0, originalColsB));
    }

    private INDArray matmul3D2D(INDArray a, INDArray b, List<Long> fpgaInShift, Long fpgaOutShift) {
        long batchSize = a.size(0);
        long M = a.size(1);
        long K = a.size(2);
        long N = b.size(1);

        INDArray a2D = a.reshape('c', batchSize * M, K);
        INDArray result2D = matmul2D(a2D, b, fpgaInShift, fpgaOutShift);

        long[] outputShape = {batchSize, M, N};
        return result2D.reshape('c', outputShape);
    }

    private INDArray matmul3D(INDArray a, INDArray b, List<Long> fpgaInShift, Long fpgaOutShift) {
        long batchA = a.size(0);
        long batchB = b.size(0);
        long batch = Math.max(batchA, batchB);
        long m = a.size(1);
        long n = b.size(2);

        INDArray result = Nd4j.createUninitialized(new long[]{batch, m, n}, 'c');

        for (int i = 0; i < (int) batch; i++) {
            INDArray sliceA = (batchA == 1) ? a.slice(0) : a.slice(i);
            INDArray sliceB = (batchB == 1) ? b.slice(0) : b.slice(i);
            INDArray product = matmul2D(sliceA, sliceB, fpgaInShift, fpgaOutShift);
            result.putSlice(i, product);
        }
        return result;
    }


    private INDArray matMulOnAccelerator(INDArray a, INDArray b, int rowsA, int colsA, int colsB, List<Long> fpgaInShift, Long fpgaOutShift){

        long[][] fixedPointA = new long[rowsA][colsA];
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsA; j++) {
                fixedPointA[i][j] = a.getLong(i, j);
            }
        }

        long[][] fixedPointB = new long[colsA][colsB];
        for (int i = 0; i < colsA; i++) {
            for (int j = 0; j < colsB; j++) {
                fixedPointB[i][j] = b.getLong(i, j);
            }
        }

        int shiftAmount = (int) (fpgaInShift.get(0) + fpgaInShift.get(1) - fpgaOutShift);
        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "matmul",
                shiftAmount,
                false,
                "none",
                0,
                0,
                0,
                0,
                rowsA,
                colsA,
                colsB);
        long[][] hardwareResult = AcceleratorSimInterface.runRefOneInst(fixedPointA, fixedPointB, instruction);

        // 将累加结果转换回浮点数
        float[] output = new float[rowsA * colsB];

        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                output[i * colsB + j] = (float) (hardwareResult[i][j]);
            }
        }

        return Nd4j.create(output).reshape(rowsA, colsB);
    }

}

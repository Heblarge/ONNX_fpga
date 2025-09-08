package org.forwarder.backend.impls.HWAccelerated.opsets.v11.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v11.ops.GemmV11;
import org.onnx4j.opsets.operator.OperatorOutputs;


public class HWAcceleratedGemmV11 extends HWAcceleratedOperator implements GemmV11 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        GeMMInputsV11<INDArray> castedInputs = new GeMMInputsV11<>(node, inputs);
        INDArray matrixa = castedInputs.getA();
        INDArray matrixb = castedInputs.getB();
        INDArray outputTensor = this.gemm(matrixa,matrixb);
        return new GeMMOutputV11<>(outputTensor);
    }

    public INDArray gemm(INDArray a, INDArray b) {
        long[] shapeA = a.shape();
        long[] shapeB = b.shape();

        int rowsA = (int) shapeA[0]; // m
        int colsA = (int) shapeA[1]; // k
        int colsB = (int) shapeB[1]; // n

        if (shapeA[1] != shapeB[0]) {
            throw new IllegalArgumentException(
                    String.format("Matrix shape mismatch for MatMul: A's columns (%d) must equal B's rows (%d).", shapeA[1], shapeB[0])
            );
        }

        int[][] fixedPointA = new int[rowsA][colsA];
        int[][] fixedPointB = new int[colsA][colsB];
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsA; j++) {
                // 将浮点数转换为定点数
                fixedPointA[i][j] = (int) (a.getFloat(i, j));

            }
        }
        for (int i = 0; i < colsA; i++) {
            for (int j = 0; j < colsB; j++) {
                // 将浮点数转换为定点数
                fixedPointB[i][j] = (int) (b.getFloat(i, j));
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
        int[][] fixedPointOutput = AcceleratorSimInterface.runRefOneInst(fixedPointA, fixedPointB, instruction);

        float[] Output = new float[rowsA * colsB];
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                // 将定点数转换回浮点数
                Output[i * colsB + j] = (float) (fixedPointOutput[i][j]);
            }
        }

        return Nd4j.create(Output).reshape(rowsA, colsB);
    }

}

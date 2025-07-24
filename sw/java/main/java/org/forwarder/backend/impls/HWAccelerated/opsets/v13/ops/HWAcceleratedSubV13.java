package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.SubV13;
import org.onnx4j.opsets.operator.OperatorOutputs;


public class HWAcceleratedSubV13 extends HWAcceleratedOperator implements SubV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        SubInputsV13<INDArray> castedInputs = new SubInputsV13<>(node, inputs);
        INDArray matrixa = castedInputs.getA();
        INDArray matrixb = castedInputs.getB();
        INDArray outputTensor = this.sub(matrixa,matrixb);
        return new SubOutputV13<>(outputTensor);
    }

    public INDArray sub(INDArray a, INDArray b) {
        long[] shapeA = a.shape();
        long[] shapeB = b.shape();

        int rowsA = (int) shapeA[0];
        int colsA = (int) shapeA[1];
        int rowsB = (int) shapeB[0];
        int colsB = (int) shapeB[1];

        if(!java.util.Arrays.equals(shapeA, shapeB)) {
            throw new IllegalArgumentException("Matrix A and B must be equal!");
        }

        int[][] fixedPointA = new int[rowsA][colsA];
        int[][] fixedPointB = new int[rowsB][colsB];
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsA; j++) {
                // 将浮点数转换为定点数
                fixedPointA[i][j] = (int) (a.getFloat(i, j));

            }
        }
        for (int i = 0; i < rowsB; i++) {
            for (int j = 0; j < colsB; j++) {
                // 将浮点数转换为定点数
                fixedPointB[i][j] = (int) (b.getFloat(i, j) * -1);
            }
        }

        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "elementadd",
                0,
                false,
                "none",
                0,
                0,
                0,
                0,
                rowsA,
                colsA,
                colsA
        );
        int[][] fixedPointOutput = AcceleratorSimInterface.runSimOneInst(fixedPointA, fixedPointB, instruction);

        float[] Output = new float[rowsA * colsA];
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsA; j++) {
                // 将定点数转换回浮点数
                Output[i * colsA + j] = (float) (fixedPointOutput[i][j]);
            }
        }

        return Nd4j.create(Output).reshape(shapeA);
    }

}

package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.MatMulV13;
import org.onnx4j.opsets.operator.OperatorOutputs;


public class HWAcceleratedMatMulV13 extends HWAcceleratedOperator implements MatMulV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        MatMulInputsV1<INDArray> castedInputs = new MatMulInputsV1<>(node, inputs);
        INDArray matrixa = castedInputs.getA();
        INDArray matrixb = castedInputs.getB();
        INDArray outputTensor = this.matmul(matrixa,matrixb);
        return new MatMulOutputV1<>(outputTensor);
    }

    public INDArray matmul(INDArray a, INDArray b) {
        long[] shapeA = a.shape();
        long[] shapeB = b.shape();

        int rows = (int) shapeA[0];
        int cols = (int) shapeA[1];
        if(rows != cols) {
            throw new IllegalArgumentException("rows and cols must be equal!");
        }
        if((shapeA[0] != shapeB[0]) || (shapeA[1] != shapeB[1])){
            throw new IllegalArgumentException("A B rows must have same size!");
        }

        int[][] fixedPointA = new int[rows][cols];
        int[][] fixedPointB = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // 将浮点数转换为定点数
                fixedPointA[i][j] = (int) (a.getFloat(i, j));
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
                rows,
                cols,
                cols
        );
        int[][] fixedPointOutput = AcceleratorSimInterface.runSimOneInst(fixedPointA, fixedPointB, instruction);

        float[] Output = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // 将定点数转换回浮点数
                Output[i * cols + j] = (float) (fixedPointOutput[i][j]);
            }
        }

        return Nd4j.create(Output).reshape(shapeA);
    }

}

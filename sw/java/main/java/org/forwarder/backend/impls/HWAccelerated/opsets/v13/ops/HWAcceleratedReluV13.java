package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ReluV13;
import org.onnx4j.opsets.operator.OperatorOutputs;


public class HWAcceleratedReluV13 extends HWAcceleratedOperator implements ReluV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        ReluInputsV13<INDArray> castedInputs = new ReluInputsV13<>(node, inputs);
        INDArray inputTensor = castedInputs.getInput();
        INDArray outputTensor = this.relu(inputTensor);
        return new ReluOutputV13<>(outputTensor);
    }

    public INDArray relu(INDArray x) {
        long[] shape = x.shape();
        int rows = (int) shape[0];
        int cols = (int) shape[1];
        if(rows != cols) {
            throw new IllegalArgumentException("rows and cols must be equal!");
        }
        int[][] fixedPointInput = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // 将浮点数转换为定点数
                fixedPointInput[i][j] = (int) (x.getFloat(i, j));
            }
        }
        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "elementadd",
                0,
                false,
                "relu",
                0,
                0,
                0,
                0,
                rows,
                cols,
                cols
        );
        int[][] matrixB_zero = new int[rows][cols];
        int[][] fixedPointOutput = AcceleratorSimInterface.runSimOneInst(fixedPointInput, matrixB_zero, instruction);

        float[] Output = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // 将定点数转换回浮点数
                Output[i * cols + j] = (float) (fixedPointOutput[i][j]);
            }
        }

        return Nd4j.create(Output).reshape(shape);
    }

}

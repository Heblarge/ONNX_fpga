package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ExpV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import Accelerator.InstJavaTODO;
import Accelerator.AcceleratorSimInterface;


public class HWAcceleratedExpV13 extends HWAcceleratedOperator implements ExpV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        ExpInputsV13<INDArray> castedInputs = new ExpInputsV13<>(node, inputs);
        INDArray inputTensor = castedInputs.getInput();
        INDArray outputTensor = this.exp(inputTensor);
        return new ExpOutputV13<>(outputTensor);
    }

    public INDArray exp(INDArray x) {
        long[] shape = x.shape();
        int rows = (int) shape[0];
        int cols = (int) shape[1];
        if(rows != cols) {
            throw new IllegalArgumentException("rows and cols must be equal!");
        }
        int fracWidth = AcceleratorSimInterface.acceleratorCfg().fracWidth();
        double factor = Math.pow(2, fracWidth);
        int[][] fixedPointInput = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // 将浮点数转换为定点数
                fixedPointInput[i][j] = (int) Math.round(x.getFloat(i, j) * factor);
            }
        }
        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "elementadd",
                0,
                false,
                "exp",
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
                Output[i * cols + j] = (float) (fixedPointOutput[i][j] / factor);
            }
        }

        return Nd4j.create(Output).reshape(shape);
    }

}

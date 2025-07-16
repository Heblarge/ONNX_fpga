package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.MaxV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import java.util.List;


public class HWAcceleratedMaxV13 extends HWAcceleratedOperator implements MaxV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        MaxInputsV13<INDArray> castedInputs = new MaxInputsV13<>(node, inputs);
        List<INDArray> inputTensor = castedInputs.getInputTensors();
        INDArray outputTensor = this.max(inputTensor);
        return new MaxOutputV13<>(outputTensor);
    }

    public INDArray max(List<INDArray> inputTensors) {
        if (inputTensors == null || inputTensors.isEmpty()) {
            throw new IllegalArgumentException("Max operator requires at least one input tensor.");
        }

        INDArray matrixA = inputTensors.get(0);
        INDArray matrixB = inputTensors.get(1);

        long[] shape = matrixA.shape();
        int rows = (int) shape[0];
        int cols = (int) shape[1];
        if(rows != cols) {
            throw new IllegalArgumentException("rows and cols must be equal!");
        }

        int[][] fixedPointInputA = new int[rows][cols];
        int[][] fixedPointInputB = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // 将浮点数转换为定点数
                fixedPointInputA[i][j] = (int) (matrixA.getFloat(i, j));
                fixedPointInputB[i][j] = (int) (matrixB.getFloat(i, j));
            }
        }
        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "elementmax",
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
        int[][] fixedPointOutput = AcceleratorSimInterface.runSimOneInst(fixedPointInputA, fixedPointInputB, instruction);

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

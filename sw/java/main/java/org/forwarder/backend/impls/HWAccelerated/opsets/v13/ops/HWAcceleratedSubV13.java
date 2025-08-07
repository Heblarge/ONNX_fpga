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
        INDArray matrixA = castedInputs.getA();
        INDArray matrixB = castedInputs.getB();
        INDArray outputTensor = this.sub(matrixA,matrixB);
        return new SubOutputV13<>(outputTensor);
    }


    public INDArray sub(INDArray a, INDArray b) {
        if (a.rank() == 2 && b.rank() == 2) {
            return sub2D(a, b);
        } else if (a.rank() == 3 || b.rank() == 3) {
            return sub3D(a, b);
        } else {
            throw new IllegalArgumentException("Unsupported tensor rank for Sub: A=" + a.rank() + ", B=" + b.rank());
        }
    }

    private INDArray sub2D(INDArray a, INDArray b) {
        long[] shapeA = a.shape();
        long[] shapeB = b.shape();

        if (!java.util.Arrays.equals(shapeA, shapeB)) {
            throw new IllegalArgumentException("Unsupported operation: Input shapes must be identical for hardware acceleration.");
        }

        int rows = (int) shapeA[0];
        int cols = (int) shapeA[1];

        return subOnAccelerator(a, b, rows, cols);
    }

    private INDArray sub3D(INDArray a, INDArray b) {
        long[] shapeA = a.shape();
        long[] shapeB = b.shape();

        if (!java.util.Arrays.equals(shapeA, shapeB)) {
            throw new IllegalArgumentException("Unsupported operation: Input shapes must be identical for 3D hardware addition.");
        }

        long batch = shapeA[0];
        long rows = shapeA[1];
        long cols = shapeA[2];

        INDArray result = Nd4j.createUninitialized(new long[]{batch, rows, cols}, 'c');

        for (int i = 0; i < (int) batch; i++) {
            INDArray sliceA = a.slice(i);
            INDArray sliceB = b.slice(i);

            INDArray sum = subOnAccelerator(sliceA, sliceB, (int)rows, (int)cols);

            result.putSlice(i, sum);
        }

        return result;
    }

    private INDArray subOnAccelerator(INDArray a, INDArray b, int rows, int cols) {

        int[][] fixedPointA = new int[rows][cols];
        int[][] fixedPointB = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                fixedPointA[i][j] = (int) (a.getFloat(i, j));
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

        return Nd4j.create(Output).reshape(rows, cols);
    }

}

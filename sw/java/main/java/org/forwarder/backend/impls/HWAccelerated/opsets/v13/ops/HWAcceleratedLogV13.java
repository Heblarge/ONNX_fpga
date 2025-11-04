package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
// import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator; // (Unused)
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedQuantizedOperator;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedCollector;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.onnx4j.Inputs;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.LogV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

// import java.util.List; // (Unused)

public class HWAcceleratedLogV13 extends HWAcceleratedQuantizedOperator implements LogV13 {

    private final int HW_DIM_MULTIPLE = 32;

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        LogInputsV13<INDArray> castedInputs = new LogInputsV13<>(node, inputs);
        INDArray inputTensor = castedInputs.getInput();
        Graph graph = node.getGraph();
        long targetInputShift = castedInputs.getFpgaInShift().get(0);
        long targetOutputShift = castedInputs.getFpgaOutShift().get(0);
        String inputName = node.getInputNames()[0];
        long sourceShift = this.getProducerOutputShift(graph, inputName, targetInputShift);

        String nodeName = node.getName();

        INDArray outputTensor = this.log(
                inputTensor,
                sourceShift,
                targetInputShift,
                targetOutputShift,
                nodeName
        );
        return new LogOutputV13<>(outputTensor);
    }

    public INDArray log(INDArray x, long sourceShift, long targetInputShift, long targetOutputShift, String nodeName) {
        if (x.rank() == 2) {
            return log2D(x, sourceShift, targetInputShift, targetOutputShift, nodeName);
        } else if (x.rank() == 3) {
            return log3D(x, sourceShift, targetInputShift, targetOutputShift, nodeName);
        } else {
            throw new IllegalArgumentException("Unsupported tensor rank for Log: " + x.rank());
        }
    }

    private int ceilToMultiple(int value, int multiple) {
        if (multiple == 0) return value;
        return ((value + multiple - 1) / multiple) * multiple;
    }

    private INDArray log2D(INDArray x, long sourceShift, long targetInputShift, long targetOutputShift, String nodeName) {
        long[] shape = x.shape();
        int originalRows = (int) shape[0];
        int originalCols = (int) shape[1];

        int paddedRows = ceilToMultiple(originalRows, HW_DIM_MULTIPLE);
        int paddedCols = ceilToMultiple(originalCols, HW_DIM_MULTIPLE);

        INDArray paddedX = Nd4j.zeros(x.dataType(),paddedRows, paddedCols);
        paddedX.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols)}, x);

        INDArray paddedResult = logOnAccelerator(paddedX, paddedRows, paddedCols, sourceShift, targetInputShift, targetOutputShift, nodeName);

        return paddedResult.get(NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols));
    }

    private INDArray log3D(INDArray x, long sourceShift, long targetInputShift, long targetOutputShift, String nodeName) {
        long[] shape = x.shape();
        long batch = shape[0];
        // long rows = shape[1]; // (Unused)
        // long cols = shape[2]; // (Unused)

        INDArray result = Nd4j.createUninitialized(x.dataType(), shape, 'c');

        for (int i = 0; i < (int) batch; i++) {
            INDArray slice = x.slice(i);
            INDArray logSlice = log2D(slice, sourceShift, targetInputShift, targetOutputShift, nodeName);
            result.putSlice(i, logSlice);
        }

        return result;
    }

    private INDArray logOnAccelerator(INDArray x, int rows, int cols, long sourceShift, long targetInputShift, long targetOutputShift, String nodeName) {

        long s_in = sourceShift;
        long s_hw = AcceleratorSimInterface.acceleratorCfg().fracWidth();
        long s_out = targetOutputShift;

        int preShiftAmount = (int) (s_in - s_hw);

        long[][] fixedPointInput = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long valX = x.getLong(i, j);
                fixedPointInput[i][j] = (preShiftAmount < 0) ? (valX << -preShiftAmount) : (valX >> preShiftAmount);

            }
        }

        int postShiftAmount = (int) (s_hw - s_out);
        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "elementadd",
                0,
                false,
                "log",
                postShiftAmount,
                0,
                0,
                0,
                rows,
                cols,
                cols
        );

        long[][] matrixB_zero = new long[rows][cols];
        long[][] hardwareResult = AcceleratorSimInterface.runRefOneInst(fixedPointInput, matrixB_zero, instruction);

        HWAcceleratedCollector.getInstance().recordLayerUsage(nodeName, fixedPointInput, matrixB_zero, hardwareResult);

        long[] output = new long[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                output[i * cols + j] = hardwareResult[i][j];
            }
        }

        INDArray fianlOutput =  Nd4j.create(output, new long[]{rows, cols}, x.dataType());

        return fianlOutput;
    }
}

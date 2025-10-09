package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.Arrays;
import java.util.List;

public class HWAcceleratedReluV13Test extends HWAcceleratedTestCase {

    private INDArray calculateSimulatedFixedPointRelu(
            INDArray x, List<Long> fpgaInShift, List<Long> fpgaOutShift
    ) {

        if (x.rank() > 2) {
            long[] finalShape = x.shape();
            long numCols = finalShape[finalShape.length - 1];
            long numRows = x.length() / numCols;
            INDArray reshapedX = x.reshape('c', numRows, numCols);
            INDArray result2D = calculateSimulatedFixedPointRelu(reshapedX, fpgaInShift, fpgaOutShift);
            return result2D.reshape('c', finalShape);
        }

        long s_in = fpgaInShift.get(0);
        long s_hw = AcceleratorSimInterface.acceleratorCfg().fracWidth();
        long s_out = fpgaOutShift.get(0);

        long preShiftAmount = s_in - s_hw;
        int rows = (int) x.rows();
        int cols = (int) x.columns();
        long[][] preShifted_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long val = x.getLong(i, j);
                preShifted_long[i][j] = (preShiftAmount >= 0)
                        ? (val >> preShiftAmount)
                        : (val << -preShiftAmount);
            }
        }

        long[][] reluResult_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                reluResult_long[i][j] = Math.max(0, preShifted_long[i][j]);
            }
        }

        long postShiftAmount = s_hw - s_out;
        long[][] postShifted_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                postShifted_long[i][j] = (postShiftAmount >= 0)
                        ? (reluResult_long[i][j] >> postShiftAmount)
                        : (reluResult_long[i][j] << -postShiftAmount);
            }
        }

        float[] flatResult = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                flatResult[i * cols + j] = postShifted_long[i][j];
            }
        }
        return Nd4j.create(flatResult, new long[]{rows, cols});
    }

    @Test
    public void testReluWithShifts() throws Exception {
        System.out.println("\n--- Testing Relu with Two-Stage Quantization Shifts ---");
        int rows = 32;
        int cols = 32;
        float minValue = -10.0f;
        float maxValue = 10.0f;

        List<Long> fpgaInShift = Arrays.asList(22L);
        long s_hw = AcceleratorSimInterface.acceleratorCfg().intWidth();
        List<Long> fpgaOutShift = Arrays.asList(15L);

        INDArray matrix_int = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue, fpgaInShift.get(0)));

        long preShiftAmount = fpgaInShift.get(0) - s_hw;
        INDArray preShifted_int = (preShiftAmount >= 0)
                ? matrix_int.div(1L << preShiftAmount)
                : matrix_int.mul(1L << -preShiftAmount);

        INDArray reluResult_int = Transforms.relu(preShifted_int);

        long postShiftAmount = s_hw - fpgaOutShift.get(0);
        INDArray theoreticalExpected = (postShiftAmount >= 0)
                ? reluResult_int.div(1L << postShiftAmount)
                : reluResult_int.mul(1L << -postShiftAmount);

        INDArray simulatedExpected = calculateSimulatedFixedPointRelu(matrix_int, fpgaInShift, fpgaOutShift);

        HWAcceleratedReluV13 operator = new HWAcceleratedReluV13();
        INDArray actualOutput = operator.relu(matrix_int, fpgaInShift, fpgaOutShift);

        // 5. Validate all three results
        double tolerance = 50;
        HWAcceleratedTestModel.validate("ReLU - 2D Quantized", theoreticalExpected, simulatedExpected, actualOutput, tolerance);
    }


}
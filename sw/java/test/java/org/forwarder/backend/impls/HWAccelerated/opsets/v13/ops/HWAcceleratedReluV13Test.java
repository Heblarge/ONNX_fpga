package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Test;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.Arrays;
import java.util.List;

public class HWAcceleratedReluV13Test extends HWAcceleratedTestCase {

    private INDArray calculateSimulatedFixedPointRelu(
            INDArray x, long sourceShift, /* long targetInputShift, // Not used */ long targetOutputShift
    ) {
        if (x.rank() > 2) {
            long[] finalShape = x.shape();
            long numCols = finalShape[finalShape.length - 1];
            long numRows = x.length() / numCols;
            INDArray reshapedX = x.reshape('c', numRows, numCols);

            INDArray result2D = calculateSimulatedFixedPointRelu(reshapedX, sourceShift, targetOutputShift);
            return result2D.reshape('c', finalShape);
        }

        int rows = (int) x.rows();
        int cols = (int) x.columns();

        long s_in = sourceShift;
        long s_hw = AcceleratorSimInterface.acceleratorCfg().fracWidth();
        long s_out = targetOutputShift;

        int preShiftAmount = (int) (s_in - s_hw);
        int postShiftAmount = (int) (s_hw - s_out);

        long[][] preShifted_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long val = x.getLong(i, j);
                preShifted_long[i][j] = (preShiftAmount < 0) ? (val << -preShiftAmount) : (val >> preShiftAmount);
            }
        }

        long[][] reluResult_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                reluResult_long[i][j] = Math.max(0, preShifted_long[i][j]);
            }
        }

        long[][] postShifted_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long val = reluResult_long[i][j];
                postShifted_long[i][j] = (postShiftAmount < 0) ? (val << -postShiftAmount) : (val >> postShiftAmount);
            }
        }

        long[] flatResultLong = new long[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                flatResultLong[i * cols + j] = postShifted_long[i][j];
            }
        }
        return Nd4j.create(flatResultLong, new long[]{rows, cols}, x.dataType());
    }

    @Test
    public void testReluWithShifts() throws Exception {
        System.out.println("\n--- Testing Relu with Two-Stage Quantization Shifts ---");
        int rows = 40;
        int cols = 20;
        float minValue = -10.0f;
        float maxValue = 10.0f;

        long sourceShift = 10L;
        long targetInputShift = 22L;
        long targetOutputShift = 8L;
        long s_hw = AcceleratorSimInterface.acceleratorCfg().fracWidth();

        INDArray matrix_int = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue, sourceShift));

        int preShiftAmount = (int) (sourceShift - s_hw);
        INDArray preShifted_int = Nd4j.create(DataType.LONG, matrix_int.shape());
        for(int i=0; i<rows; i++) for(int j=0; j<cols; j++){
            long val = matrix_int.getLong(i, j);
            preShifted_int.putScalar(i, j, (preShiftAmount < 0) ? (val << -preShiftAmount) : (val >> preShiftAmount));
        }
        INDArray reluResult_int = Transforms.relu(preShifted_int);
        int postShiftAmount = (int) (s_hw - targetOutputShift);
        INDArray theoreticalExpected = Nd4j.create(DataType.LONG, matrix_int.shape());
        for(int i=0; i<rows; i++) for(int j=0; j<cols; j++){
            long val = reluResult_int.getLong(i, j);
            theoreticalExpected.putScalar(i, j, (postShiftAmount < 0) ? (val << -postShiftAmount) : (val >> postShiftAmount));
        }

        INDArray simulatedExpected = calculateSimulatedFixedPointRelu(
                matrix_int, sourceShift, targetOutputShift
        );

        HWAcceleratedReluV13 operator = new HWAcceleratedReluV13();
        INDArray actualOutput = operator.relu(
                matrix_int,
                sourceShift,
                targetInputShift,
                targetOutputShift,
                "ReluTest"
        );

        double tolerance = 0.0;
        HWAcceleratedTestModel.validate("ReLU - 2D Quantized", theoreticalExpected, simulatedExpected, actualOutput, tolerance); // Compare sim vs actual
    }
}
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

public class HWAcceleratedLogV13Test extends HWAcceleratedTestCase {

    private static class LogCordicSimulator {
        private final int bit_int;
        private final int bit_frac;
        private final int rotate;
        private final boolean using_compensation_iters = true;
        private final int[] atanh_vals_fix;
        private final int log2_fix;

        private static class NormalizeResult { int x_norm; int k; }

        public LogCordicSimulator(int bit_int_magnitude, int bit_frac) {
            this.bit_int = bit_int_magnitude;
            this.bit_frac = bit_frac;
            this.rotate = bit_frac;
            this.atanh_vals_fix = generateAtanhTable();
            this.log2_fix = (int)Math.round(Math.log(2.0) * (1L << bit_frac));
        }

        private int[] generateAtanhTable() {
            int[] table = new int[rotate + 1];
            double scale_factor = (1L << bit_frac);
            for (int j = 0; j <= rotate; j++) {
                if (j == 0) table[j] = 0;
                else {
                    double v = 0.5 * Math.log((1.0 + Math.pow(2.0, -j)) / (1.0 - Math.pow(2.0, -j)));
                    table[j] = (int)Math.round(v * scale_factor);
                }
            }
            return table;
        }

        private int countLeadingZeros(int x, int width) {
            if (x == 0) return width;
            int totalBits = 32;
            int lz = Integer.numberOfLeadingZeros(x);
            int effectiveLz = lz - (totalBits - width);
            return Math.max(0, effectiveLz);
        }

        private NormalizeResult normalize(int x) {
            NormalizeResult res = new NormalizeResult();
            int width = bit_int + bit_frac;
            if (x <= 0) { res.x_norm = 1 << bit_frac; res.k = -width; return res; }
            int msbPosition = width - 1 - countLeadingZeros(x, width) - bit_frac;
            res.k = msbPosition;
            int shiftAmount = bit_frac - (width - 1 - countLeadingZeros(x, width));
            if (shiftAmount > 0) {
                int mask = (width >= 32) ? -1 : ((1 << width) - 1);
                res.x_norm = (x << shiftAmount) & mask;
            } else {
                res.x_norm = x >> (-shiftAmount);
            }
            return res;
        }

        public long compute(int x_fixed) {
            if (x_fixed <= 0) return Long.MIN_VALUE;
            NormalizeResult norm = normalize(x_fixed);
            int x_norm = norm.x_norm;
            int k = norm.k;
            long scale_factor = 1L << bit_frac;
            long x_n = (long)x_norm + scale_factor;
            long y_n = (long)x_norm - scale_factor;
            long z_n = 0;
            for (int j = 1; j <= rotate; j++) {
                long sign_y = (y_n >= 0L) ? -1L : 1L;
                long x_temp = x_n;
                long y_temp = y_n;
                x_n = x_temp + (sign_y * (y_temp >> j));
                y_n = y_temp + (sign_y * (x_temp >> j));
                z_n = z_n - (sign_y * (long)atanh_vals_fix[j]);
                if (using_compensation_iters && (j == 4 || j == 13)) {
                    sign_y = (y_n >= 0L) ? -1L : 1L;
                    x_temp = x_n;
                    y_temp = y_n;
                    x_n = x_temp + (sign_y * (y_temp >> j));
                    y_n = y_temp + (sign_y * (x_temp >> j));
                    z_n = z_n - (sign_y * (long)atanh_vals_fix[j]);
                }
            }
            return 2L * z_n + (long)k * (long)log2_fix;
        }
    }

    private INDArray calculateSimulatedFixedPointLog(
            INDArray x,
            long sourceShift,
            long targetInputShift,
            long targetOutputShift
    ) {

        if (x.rank() > 2) {
            long[] finalShape = x.shape();
            long numCols = finalShape[finalShape.length - 1];
            long numRows = x.length() / numCols;
            INDArray reshapedX = x.reshape('c', numRows, numCols);
            INDArray result2D = calculateSimulatedFixedPointLog(reshapedX, sourceShift, targetInputShift, targetOutputShift);
            return result2D.reshape('c', finalShape);
        }

        int rows = (int) x.rows();
        int cols = (int) x.columns();

        long s_in = sourceShift;
        long s_hw_int = AcceleratorSimInterface.acceleratorCfg().intWidth();
        long s_hw_frac = AcceleratorSimInterface.acceleratorCfg().fracWidth();
        long s_out = targetOutputShift;

        LogCordicSimulator simulator = new LogCordicSimulator((int)s_hw_int - 1, (int)s_hw_frac);

        int preShiftAmount = (int) (s_in - s_hw_frac);
        int postShiftAmount = (int) (s_hw_frac - s_out);

        long[][] preShifted_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long val = x.getLong(i, j);
                preShifted_long[i][j] = (preShiftAmount < 0) ? safeLeftShift(val, -preShiftAmount) : (val >> preShiftAmount);
            }
        }

        long[][] logResult_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                logResult_long[i][j] = simulator.compute((int)preShifted_long[i][j]);
            }
        }

        long[][] postShifted_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long val = logResult_long[i][j];
                postShifted_long[i][j] = (postShiftAmount < 0) ? safeLeftShift(val, -postShiftAmount) : (val >> postShiftAmount);
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


    private long safeLeftShift(long value, int amount) {
        if (amount <= 0) return (amount == 0) ? value : (value >> -amount);
        if (value == 0) return 0;
        long overflowCheckMask = (-1L) << (64 - amount);
        boolean potentialOverflow;
        if (value > 0) {
            potentialOverflow = (value & overflowCheckMask) != 0;
            if (potentialOverflow) return Long.MAX_VALUE;
        } else { // value < 0
            potentialOverflow = (~value & overflowCheckMask) != 0;
            if (potentialOverflow) return Long.MIN_VALUE;
        }
        return value << amount;
    }



    @Test
    public void testLogWithShifts() throws Exception {
        System.out.println("\n--- Testing Log with Two-Stage Quantization Shifts ---");
        int rows = 32;
        int cols = 32;
        float minValue = 0.001f;
        float maxValue = 10.0f;

        long sourceShift = 15L;
        long targetInputShift = 10L;
        long targetOutputShift = 20L;

        INDArray matrix_int = Nd4j.createFromArray(
                HWAcceleratedTestModel.generateRandom2DIntMatrix(rows, cols, minValue, maxValue, (int)sourceShift)
        );

        INDArray matrix_float = matrix_int.castTo(DataType.DOUBLE).div(Math.pow(2, sourceShift));
        INDArray theoreticalExpected_float = Transforms.log(matrix_float, true);
        INDArray theoreticalExpected = Transforms.round(
                theoreticalExpected_float.mul(Math.pow(2, targetOutputShift))
        ).castTo(matrix_int.dataType());

        INDArray simulatedExpected = calculateSimulatedFixedPointLog(
                matrix_int, sourceShift, targetInputShift, targetOutputShift
        );

        HWAcceleratedLogV13 operator = new HWAcceleratedLogV13();

        INDArray actualOutput = operator.log(
                matrix_int,
                sourceShift,
                targetInputShift,
                targetOutputShift
        );

        double tolerance = 10.0;
        HWAcceleratedTestModel.validate("Log - 2D Quantized", theoreticalExpected, simulatedExpected, actualOutput, tolerance);
    }

    @Test
    public void testLog3DWithShifts() throws Exception {
        System.out.println("\n--- Testing Log 3D (Batched) with Two-Stage Quantization Shifts ---");
        int batch = 2;
        int rows = 30;
        int cols = 34;
        int minValue = 1;
        int maxValue = 50;

        long sourceShift = 18L;
        long targetInputShift = 0L;
        long targetOutputShift = 25L;

        INDArray matrix_int = HWAcceleratedTestModel.generateRandom3DIntMatrix(batch, rows, cols, minValue, maxValue, (int)sourceShift);

        INDArray matrix_float = matrix_int.castTo(DataType.DOUBLE).div(Math.pow(2, sourceShift));
        INDArray theoreticalExpected_float = Transforms.log(matrix_float, true);
        INDArray theoreticalExpected = Transforms.round(
                theoreticalExpected_float.mul(Math.pow(2, targetOutputShift))
        ).castTo(matrix_int.dataType());

        INDArray simulatedExpected = calculateSimulatedFixedPointLog(
                matrix_int, sourceShift, targetInputShift, targetOutputShift
        );

        HWAcceleratedLogV13 operator = new HWAcceleratedLogV13();

        INDArray actualOutput = operator.log(
                matrix_int,
                sourceShift,
                targetInputShift,
                targetOutputShift
        );

        double tolerance = 150.0;
        HWAcceleratedTestModel.validate("Log - 3D Quantized", theoreticalExpected, simulatedExpected, actualOutput, tolerance);
    }

}
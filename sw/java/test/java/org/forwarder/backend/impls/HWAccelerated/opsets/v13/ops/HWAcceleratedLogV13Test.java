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

public class HWAcceleratedLogV13Test extends HWAcceleratedTestCase {

private static class LogCordicSimulator {
    private final int bit_int;
    private final int bit_frac;
    private final int rotate;
    private final boolean using_compensation_iters = true;

    private final int[] atanh_vals_fix;
    private final int log2_fix;

    private static class NormalizeResult {
        int x_norm;
        int k;
    }

    public LogCordicSimulator(int bit_int_magnitude, int bit_frac) {
        this.bit_int = bit_int_magnitude;
        this.bit_frac = bit_frac;
        this.rotate = bit_frac;
        this.atanh_vals_fix = generateAtanhTable();
        this.log2_fix = (int)Math.round(Math.log(2.0) * (1 << bit_frac));
    }

    private int[] generateAtanhTable() {
        int[] table = new int[rotate + 1];
        double scale_factor = (1 << bit_frac);
        for (int j = 0; j <= rotate; j++) {
            if (j == 0) {
                table[j] = 0;
            } else {
                double v = 0.5 * Math.log((1 + Math.pow(2, -j)) / (1 - Math.pow(2, -j)));
                table[j] = (int)(v * scale_factor);
            }
        }
        return table;
    }

    private int countLeadingZeros(int x, int width) {
        int mask = (width >= 32) ? -1 : ((1 << width) - 1);
        int v = x & mask;
        if (v == 0) return width;
        int bitLength = 32 - Integer.numberOfLeadingZeros(v);
        return width - bitLength;
    }

    private NormalizeResult normalize(int x) {
        NormalizeResult res = new NormalizeResult();
        int width = bit_int + bit_frac;
        int threshold = bit_int - 1;
        int lz = countLeadingZeros(x, width);

        if (lz < threshold) {
            int sh = (threshold - lz);
            res.x_norm = x >>> sh;
            res.k = sh;
        } else if (lz > threshold) {
            int sh = (lz - threshold);
            int mask = (width >= 32) ? -1 : ((1 << width) - 1);
            res.x_norm = (x << sh) & mask;
            res.k = -sh;
        } else {
            res.x_norm = x;
            res.k = 0;
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
            long sign_y = (y_n >= 0L) ? 1L : -1L;
            long atanh_val = (long)atanh_vals_fix[j];

            long x_temp = x_n;
            x_n = x_n - (sign_y * (y_n >> j));
            y_n = y_n - (sign_y * (x_temp >> j));
            z_n = z_n + sign_y * atanh_val;

            if (using_compensation_iters && (j == 4 || j == 13)) {
                sign_y = (y_n >= 0L) ? 1L : -1L;
                x_temp = x_n;
                x_n = x_n - (sign_y * (y_n >> j));
                y_n = y_n - (sign_y * (x_temp >> j));
                z_n = z_n + sign_y * atanh_val;
            }
        }

        return 2L * z_n + (long)k * (long)log2_fix;
    }
}

    private INDArray calculateSimulatedFixedPointLog(
            INDArray x, List<Long> fpgaInShift, Long fpgaOutShift
    ) {
        if (x.rank() > 2) {
            long[] finalShape = x.shape();
            long numCols = finalShape[finalShape.length - 1];
            long numRows = x.length() / numCols;
            INDArray reshapedX = x.reshape('c', numRows, numCols);
            INDArray result2D = calculateSimulatedFixedPointLog(reshapedX, fpgaInShift, fpgaOutShift);
            return result2D.reshape('c', finalShape);
        }

        long s_in = fpgaInShift.get(0);
        long s_hw_int = AcceleratorSimInterface.acceleratorCfg().intWidth();
        long s_hw_frac = AcceleratorSimInterface.acceleratorCfg().fracWidth();
        long s_out = fpgaOutShift;

        LogCordicSimulator simulator = new LogCordicSimulator((int)s_hw_int - 1, (int)s_hw_frac);

        long preShiftAmount = s_in - s_hw_frac;
        int rows = (int) x.rows();
        int cols = (int) x.columns();
        long[][] preShifted_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long val = x.getLong(i, j);
                preShifted_long[i][j] = (preShiftAmount >= 0) ? (val >> preShiftAmount) : (val << -preShiftAmount);
            }
        }

        long[][] logResult_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                logResult_long[i][j] = simulator.compute((int)preShifted_long[i][j]);
            }
        }

        long postShiftAmount = s_hw_frac - s_out;
        long[][] postShifted_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                postShifted_long[i][j] = (postShiftAmount >= 0) ? (logResult_long[i][j] >> postShiftAmount) : (logResult_long[i][j] << -postShiftAmount);
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
    public void testLogWithShifts() throws Exception {
        System.out.println("\n--- Testing Log with Two-Stage Quantization Shifts ---");
        int rows = 32;
        int cols = 32;
        float minValue = 0.00001f;
        float maxValue = 10.0f;

        List<Long> fpgaInShift = Arrays.asList(15L);
        Long fpgaOutShift = 30L;

        INDArray matrix_int = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue, fpgaInShift.get(0)));

        INDArray matrix_float = matrix_int.div(Math.pow(2, fpgaInShift.get(0)));
        INDArray theoreticalExpected_float = Transforms.log(matrix_float, true);
        INDArray theoreticalExpected = Transforms.round(theoreticalExpected_float.mul(Math.pow(2, fpgaOutShift)));

        INDArray simulatedExpected = calculateSimulatedFixedPointLog(matrix_int, fpgaInShift, fpgaOutShift);

        HWAcceleratedLogV13 operator = new HWAcceleratedLogV13();
        INDArray actualOutput = operator.log(matrix_int, fpgaInShift, fpgaOutShift);

        double tolerance = 5.0;
        HWAcceleratedTestModel.validate("Log - 2D Quantized", theoreticalExpected, simulatedExpected, actualOutput, tolerance);
    }
}
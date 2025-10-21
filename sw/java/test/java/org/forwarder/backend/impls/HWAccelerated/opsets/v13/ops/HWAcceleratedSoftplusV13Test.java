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

public class HWAcceleratedSoftplusV13Test extends HWAcceleratedTestCase {

    private static class SoftplusLutSimulator {
        private final int K1 = 7, K2 = 6, K3 = 6;
        private final int total_bits = K1 + K2 + K3;
        private final int bit_frac;
        private final double t_min = -16.0, t_max = 16.0;
        private final int[] P_table;
        private final int[] N_table;

        public SoftplusLutSimulator(int hardwareFracBits) {
            this.bit_frac = hardwareFracBits;
            this.P_table = generatePTable();
            this.N_table = generateNTable();
        }

        private double alphamid(int xh, int xm, int xl) {
            int idx = (xh << (K2 + K3)) | (xm << K3) | xl;
            double fullIdxMax = (1 << total_bits) - 1;
            double t_mid = t_min + (double)idx / fullIdxMax * (t_max - t_min);
            return Math.log1p(Math.exp(t_mid));
        }

        private int[] generatePTable() {
            int pLen = 1 << (K1 + K2);
            int[] pTable = new int[pLen];
            for (int idx = 0; idx < pLen; idx++) {
                int xh = idx >> K2;
                int xm = idx & ((1 << K2) - 1);
                double spread = alphamid(xh, xm, 0) - alphamid(xh, xm, (1 << K3) - 1);
                double first = alphamid(xh, 0, 0) - alphamid(xh, 0, (1 << K3) - 1);
                double last = alphamid(xh, (1 << K2) - 1, 0) - alphamid(xh, (1 << K2) - 1, (1 << K3) - 1);
                double avg_spread = (first + last) / 2.0;
                double adjust = (avg_spread - spread) / 2.0;
                double value = (alphamid(xh, xm, 0) + adjust) * (1 << bit_frac);
                pTable[idx] = (int) Math.round(value);
            }
            return pTable;
        }

        private int[] generateNTable() {
            int nLen = 1 << (K1 + K3);
            int[] nTable = new int[nLen];
            for (int idx = 0; idx < nLen; idx++) {
                int xh = idx >> K3;
                int xl = idx & ((1 << K3) - 1);
                double diff0 = alphamid(xh, 0, xl) - alphamid(xh, 0, 0);
                double diff1 = alphamid(xh, (1 << K2) - 1, xl) - alphamid(xh, (1 << K2) - 1, 0);
                double avgDiff = (diff0 + diff1) / 2.0;
                double value = avgDiff * (1 << bit_frac);
                nTable[idx] = (int) Math.round(value);
            }
            return nTable;
        }

        public int compute(long payloadInt) {
            long scaleInvLong = (((1L << total_bits) - 1L) * (1L << bit_frac)) / ((long)Math.round((t_max - t_min) * (1L << bit_frac)));
            long tMinFixed = (long) Math.round(t_min * (1L << bit_frac));
            long numerator = (payloadInt - tMinFixed) * scaleInvLong;
            long idxLong = numerator >> bit_frac;

            int idx = (int) idxLong;
            int maxIdx = (1 << total_bits) - 1;
            if (idx < 0) idx = 0;
            else if (idx > maxIdx) idx = maxIdx;

            int xh = idx >> (K2 + K3);
            int rem = idx & ((1 << (K2 + K3)) - 1);
            int xm = rem >> K3;
            int xl = rem & ((1 << K3) - 1);

            int pIdx = (xh << K2) | xm;
            int nIdx = (xh << K3) | xl;

            pIdx = Math.max(0, Math.min(pIdx, P_table.length - 1));
            nIdx = Math.max(0, Math.min(nIdx, N_table.length - 1));

            return P_table[pIdx] + N_table[nIdx];
        }
    }

    private INDArray calculateSimulatedFixedPointSoftplus(
            INDArray x, long sourceShift, long targetOutputShift
    ) {
        if (x.rank() > 2) {
            long[] finalShape = x.shape();
            long numCols = finalShape[finalShape.length - 1];
            long numRows = x.length() / numCols;
            INDArray reshapedX = x.reshape('c', numRows, numCols);
            INDArray result2D = calculateSimulatedFixedPointSoftplus(reshapedX, sourceShift, targetOutputShift);
            return result2D.reshape('c', finalShape);
        }

        int rows = (int) x.rows();
        int cols = (int) x.columns();

        long s_in = sourceShift;
        long s_hw = AcceleratorSimInterface.acceleratorCfg().fracWidth();
        long s_out = targetOutputShift;

        SoftplusLutSimulator simulator = new SoftplusLutSimulator((int)s_hw);

        int preShiftAmount = (int) (s_in - s_hw);
        int postShiftAmount = (int) (s_hw - s_out);

        long[][] preShifted_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long val = x.getLong(i, j);
                preShifted_long[i][j] = (preShiftAmount < 0) ? safeLeftShift(val, -preShiftAmount) : (val >> preShiftAmount);
            }
        }

        long[][] softplusResult_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                softplusResult_long[i][j] = simulator.compute(preShifted_long[i][j]);
            }
        }

        long[][] postShifted_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long val = softplusResult_long[i][j];
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
        } else {
            potentialOverflow = (~value & overflowCheckMask) != 0;
            if (potentialOverflow) return Long.MIN_VALUE;
        }
        return value << amount;
    }

    @Test
    public void testSoftplusWithShifts() throws Exception {
        System.out.println("\n--- Testing Softplus with Two-Stage Quantization Shifts ---");
        int rows = 32;
        int cols = 32;
        float minValue = -10.0f;
        float maxValue = 10.0f;

        long sourceShift = 18L;
        long targetInputShift = 0L;
        long targetOutputShift = 25L;

        INDArray matrix_int = Nd4j.createFromArray(
                HWAcceleratedTestModel.generateRandom2DIntMatrix(rows, cols, minValue, maxValue, (int)sourceShift)
        );

        INDArray matrix_float = matrix_int.castTo(DataType.DOUBLE).div(Math.pow(2, sourceShift));
        INDArray theoreticalExpected_float = Transforms.log(Transforms.exp(matrix_float).add(1), true);
        INDArray theoreticalExpected = Transforms.round(theoreticalExpected_float.mul(Math.pow(2, targetOutputShift)));
        theoreticalExpected = theoreticalExpected.castTo(matrix_int.dataType());


        INDArray simulatedExpected = calculateSimulatedFixedPointSoftplus(
                matrix_int, sourceShift, targetOutputShift
        );

        HWAcceleratedSoftplusV13 operator = new HWAcceleratedSoftplusV13();

        INDArray actualOutput = operator.softplus(
                matrix_int,
                sourceShift,
                targetInputShift,
                targetOutputShift
        );

        double tolerance = 50.0;
        HWAcceleratedTestModel.validate("Softplus - 2D Quantized Corrected", theoreticalExpected, simulatedExpected, actualOutput, tolerance);
    }

    @Test
    public void testSoftplus3DWithShifts() throws Exception {
        System.out.println("\n--- Testing Softplus 3D (Batched) with Two-Stage Quantization Shifts ---");
        int batchSize = 2;
        int rows = 32;
        int cols = 64;
        int minValue = -10;
        int maxValue = 10;

        long sourceShift = 22L;
        long targetInputShift = 0L;
        long targetOutputShift = 30L;

        INDArray matrix_int = HWAcceleratedTestModel.generateRandom3DIntMatrix(batchSize, rows, cols, minValue, maxValue, (int)sourceShift);

        INDArray matrix_float = matrix_int.castTo(DataType.DOUBLE).div(Math.pow(2, sourceShift));
        INDArray theoreticalExpected_float = Transforms.log(Transforms.exp(matrix_float).add(1), true);
        INDArray theoreticalExpected = Transforms.round(theoreticalExpected_float.mul(Math.pow(2, targetOutputShift)));
        theoreticalExpected = theoreticalExpected.castTo(matrix_int.dataType());

        INDArray simulatedExpected = calculateSimulatedFixedPointSoftplus(
                matrix_int, sourceShift, targetOutputShift
        );

        HWAcceleratedSoftplusV13 operator = new HWAcceleratedSoftplusV13();

        INDArray actualOutput = operator.softplus(
                matrix_int,
                sourceShift,
                targetInputShift,
                targetOutputShift
        );

        double tolerance = 1050.0;
        HWAcceleratedTestModel.validate("Softplus - 3D Quantized Corrected", theoreticalExpected, simulatedExpected, actualOutput, tolerance);
    }
}
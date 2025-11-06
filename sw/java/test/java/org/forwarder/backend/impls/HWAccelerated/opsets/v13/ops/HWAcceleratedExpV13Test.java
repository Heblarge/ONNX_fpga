package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Test;
import org.nd4j.linalg.api.buffer.DataType; // Added import
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.Arrays;
import java.util.List;

public class HWAcceleratedExpV13Test extends HWAcceleratedTestCase {

    private static class ExpCordicSimulator {
        private final int bit_int;
        private final int bit_frac;
        private final int x_max;
        private final int rotate;
        private final int expx_int_bit;

        private final long[] exp_frac_table;
        private final long[] exp_int_pos_table;
        private final long[] exp_int_neg_table;
        private final long[] poweroftwo_table;
        public ExpCordicSimulator(int bit_int_magnitude, int bit_frac, int x_max) {
            this.bit_int = bit_int_magnitude;
            this.bit_frac = bit_frac;
            this.x_max = x_max;
            this.rotate = this.bit_int;
            this.expx_int_bit = (int)Math.ceil(Math.log(Math.ceil(Math.exp(x_max))) / Math.log(2)); // Bits for exp(x_max) integer part

            this.exp_frac_table = generateExpFracTable();
            this.exp_int_pos_table = generateExpIntPosTable();
            this.exp_int_neg_table = generateExpIntNegTable();
            this.poweroftwo_table = generatePowerOfTwoTable();
        }

        private int log2Up(int x) {
            if (x <= 0) return 0;
            return 32 - Integer.numberOfLeadingZeros(x - 1);
        }

        private long[] generateExpFracTable() {
            long[] table = new long[rotate];
            for (int i = 0; i < rotate; i++) {
                table[i] = Math.round(Math.exp(1.0 / Math.pow(2, i + 1)) * Math.pow(2, bit_frac));
            }
            return table;
        }

        private long[] generateExpIntPosTable() {
            int len = log2Up(x_max); // Only need table up to log2(x_max)
            long[] table = new long[len];
            for (int i = 0; i < len; i++) {
                table[i] = Math.round(Math.exp(Math.pow(2, i)) * Math.pow(2, bit_frac));
            }
            return table;
        }

        private long[] generateExpIntNegTable() {
            long[] table = new long[bit_int]; // Need table for all integer bits
            for (int i = 0; i < bit_int; i++) {
                table[i] = Math.round(Math.exp(-Math.pow(2, i)) * Math.pow(2, bit_frac));
            }
            return table;
        }

        private long[] generatePowerOfTwoTable() {
            long[] table = new long[rotate];
            for (int i = 0; i < rotate; i++) {
                // Use multiplication for precision
                table[i] = Math.round((1.0 / Math.pow(2, i + 1)) * Math.pow(2, bit_frac));
            }
            return table;
        }

        private boolean getBit(long n, int k) { return (n & (1L << k)) != 0; }
        private long floor(long value) { return value >> bit_frac; }
        private long sat(long value) {
            long max_val = (1L << (expx_int_bit + bit_frac)) - 1;

            return Math.min(value, max_val);
        }

        public long compute(int x_fixed) {
            int int_x = x_fixed >> bit_frac;
            long abs_int_x = Math.abs(int_x);
            boolean neg = int_x < 0;
            long frac_x = (long)x_fixed & ((1L << bit_frac) - 1);

            long expx_frac_reg = 1L << bit_frac;
            long current_frac_x = frac_x;

            for (int i = 0; i < rotate; i++) {
                if (current_frac_x >= poweroftwo_table[i]) {
                    current_frac_x -= poweroftwo_table[i];
                    expx_frac_reg = sat(floor(expx_frac_reg * exp_frac_table[i]));
                }
            }

            long expx_int_reg = 1L << bit_frac;

            for (int i = 0; i < bit_int; i++) {
                if (getBit(abs_int_x, i)) {
                    if (neg) {
                        expx_int_reg = sat(floor(expx_int_reg * exp_int_neg_table[i]));
                    } else {
                        if (i < exp_int_pos_table.length) {
                            expx_int_reg = sat(floor(expx_int_reg * exp_int_pos_table[i]));
                        } else {
                            expx_int_reg = sat(Long.MAX_VALUE);
                        }
                    }
                }
            }

            return sat(floor(expx_frac_reg * expx_int_reg));
        }
    }

    private INDArray calculateSimulatedFixedPointExp(
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
            INDArray result2D = calculateSimulatedFixedPointExp(reshapedX, sourceShift, targetInputShift, targetOutputShift);
            return result2D.reshape('c', finalShape);
        }

        int rows = (int) x.rows();
        int cols = (int) x.columns();

        long s_in = sourceShift;
        long s_hw_frac = AcceleratorSimInterface.acceleratorCfg().fracWidth();
        long s_out = targetOutputShift;

        int bit_int_magnitude = AcceleratorSimInterface.acceleratorCfg().intWidth() - 1;

        ExpCordicSimulator simulator = new ExpCordicSimulator(bit_int_magnitude, (int)s_hw_frac, 9);

        int preShiftAmount = (int) (s_in - s_hw_frac);
        int postShiftAmount = (int) (s_hw_frac - s_out);

        long[][] preShifted_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long val = x.getLong(i, j);
                preShifted_long[i][j] = (preShiftAmount < 0) ? (val << -preShiftAmount) : (val >> preShiftAmount);
            }
        }

      long[][] expResult_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                expResult_long[i][j] = simulator.compute((int)preShifted_long[i][j]);
            }
        }


        long[][] postShifted_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long val = expResult_long[i][j];
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
    public void testExpWithShifts() throws Exception {
        System.out.println("\n--- Testing Exp 2D with Two-Stage Quantization Shifts ---");
        int rows = 32;
        int cols = 32;
        float minValue = -8.0f;
        float maxValue = 6.0f;

        long sourceShift = 23L;
        long targetInputShift = 20L;
        long targetOutputShift = 25L;

        INDArray matrix_int = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue, sourceShift));

        INDArray matrix_float = matrix_int.div(Math.pow(2, sourceShift));
        INDArray theoreticalExpected_float = Transforms.exp(matrix_float, true);
        INDArray theoreticalExpected = Transforms.round(theoreticalExpected_float.mul(Math.pow(2, targetOutputShift)));

        INDArray simulatedExpected = calculateSimulatedFixedPointExp(
                matrix_int, sourceShift, targetInputShift, targetOutputShift // Pass dummy targetInputShift
        );

        HWAcceleratedExpV13 operator = new HWAcceleratedExpV13();

        INDArray actualOutput = operator.exp(
                matrix_int,
                sourceShift,
                targetInputShift,
                targetOutputShift,
                "Exp2D"
        );

        double tolerance = 10.0;
        HWAcceleratedTestModel.validate("Exp - 2D Quantized", theoreticalExpected, simulatedExpected, actualOutput, tolerance);
    }

    @Test
    public void testExp3DWithShifts() throws Exception {
        System.out.println("\n--- Testing Exp 3D (Batched) with Two-Stage Quantization Shifts ---");
        int batchSize = 2;
        int rows = 50;
        int cols = 68;
        float minValue = -8.0f;
        float maxValue = 6.0f;

        long sourceShift = 22L;
        long targetInputShift = 20L;
        long targetOutputShift = 20L;

        INDArray matrix_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue, sourceShift);

        INDArray matrix_float = matrix_int.div(Math.pow(2, sourceShift));
        INDArray theoreticalExpected_float = Transforms.exp(matrix_float, true);
        INDArray theoreticalExpected = Transforms.round(theoreticalExpected_float.mul(Math.pow(2, targetOutputShift)));

        INDArray simulatedExpected = calculateSimulatedFixedPointExp(
                matrix_int, sourceShift, targetInputShift, targetOutputShift
        );

        HWAcceleratedExpV13 operator = new HWAcceleratedExpV13();

        INDArray actualOutput = operator.exp(
                matrix_int,
                sourceShift,
                targetInputShift,
                targetOutputShift,
                "Exp3D"
        );

        double tolerance = 5.0;
        HWAcceleratedTestModel.validate("Exp - 3D Quantized", theoreticalExpected, simulatedExpected, actualOutput, tolerance);
    }

}
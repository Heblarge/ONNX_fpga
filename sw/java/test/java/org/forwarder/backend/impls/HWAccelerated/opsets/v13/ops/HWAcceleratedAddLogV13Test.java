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

public class HWAcceleratedAddLogV13Test extends HWAcceleratedTestCase {

    private final long T_OUT = 18L;
    private final long S_DATA = 16L;

    @Test
    public void testAddLog2D() throws Exception {
        System.out.println("\n--- Testing 2D AddLog (Original Format) ---");
        int rows = 64, cols = 128;
        runFormattedTest(new long[]{rows, cols}, "Test2D_AddLog");
    }

    @Test
    public void testAddLog3D() throws Exception {
        System.out.println("\n--- Testing 3D AddLog (Original Format) ---");
        int batch = 2, rows = 32, cols = 32;
        runFormattedTest(new long[]{batch, rows, cols}, "Test3D_AddLog");
    }

    private void runFormattedTest(long[] shape, String nodeName) throws Exception {
        // 1. 生成测试数据 (保持和你原来 generateRandom2D 类似的定点化逻辑)
        INDArray mA = Transforms.round(Nd4j.rand(shape, 1.0f, 15.0f, Nd4j.getRandom()).mul(Math.pow(2, S_DATA)));
        INDArray mB = Transforms.round(Nd4j.rand(shape, 1.0f, 15.0f, Nd4j.getRandom()).mul(Math.pow(2, S_DATA)));

        // 2. 理论值计算
        INDArray floatSum = mA.castTo(DataType.DOUBLE).div(Math.pow(2, S_DATA))
                .add(mB.castTo(DataType.DOUBLE).div(Math.pow(2, S_DATA)));
        INDArray theoretical = Transforms.round(Transforms.log(floatSum).mul(Math.pow(2, T_OUT)));

        // 3. 模拟器计算 (包含 +6 补偿修正)
        INDArray simulated = calculateSimulatedFixedPointAddLog(mA, mB, S_DATA, S_DATA, S_DATA, S_DATA, T_OUT);

        // 4. 算子计算
        HWAcceleratedAddLogV13 operator = new HWAcceleratedAddLogV13();
        INDArray actual = operator.addLog(mA, mB, S_DATA, S_DATA, S_DATA, S_DATA, T_OUT, nodeName);

        // 5. 调用你原来的 validate 格式化输出方法
        // 如果你的 validate 接收 (String, INDArray, INDArray, INDArray, double)
        HWAcceleratedTestModel.validate(nodeName, theoretical, simulated, actual, 100.0);
    }

    private INDArray calculateSimulatedFixedPointAddLog(INDArray a, INDArray b, long sA, long sB, long tInA, long tInB, long tOut) {
        if (!Arrays.equals(a.shape(), b.shape())) {
            long[] bs = getBroadcastShape(a.shape(), b.shape());
            a = a.broadcast(bs); b = b.broadcast(bs);
        }

        long[] originalShape = a.shape();
        if (a.rank() > 2) {
            long lastDim = a.size(-1);
            long otherDims = a.length() / lastDim;
            a = a.reshape('c', otherDims, lastDim);
            b = b.reshape('c', otherDims, lastDim);
        }

        int rows = (int) a.rows(), cols = (int) a.columns();
        long s_hw_frac = AcceleratorSimInterface.acceleratorCfg().fracWidth();

        int resA = (int)(sA - tInA), resB = (int)(sB - tInB);
        long[][] addRes = new long[rows][cols];
        long maxAbs = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long vA = (resA < 0) ? (a.getLong(i, j) << -resA) : (a.getLong(i, j) >> resA);
                long vB = (resB < 0) ? (b.getLong(i, j) << -resB) : (b.getLong(i, j) >> resB);
                addRes[i][j] = Math.max(vA + vB, 1L);
                maxAbs = Math.max(maxAbs, Math.abs(addRes[i][j]));
            }
        }

        int logScale = (int)(tInA - s_hw_frac);
        long hwMaxFix = (long)(5.80 * (1L << s_hw_frac));
        int dynShift = 0;
        if (maxAbs > 0) {
            long tV = (logScale > 0) ? (maxAbs >> logScale) : (maxAbs << -logScale);
            while ((tV >> dynShift) > hwMaxFix) { dynShift++; }
        }
        int totalPreShift = logScale + dynShift;

        // 对齐算子里的 +6 补偿
        double ln2 = Math.log(2.0);
        long compensation = Math.round((totalPreShift + 6) * ln2 * Math.pow(2, tOut));

        LogCordicSimulator sim = new LogCordicSimulator((int)AcceleratorSimInterface.acceleratorCfg().intWidth()-1, (int)s_hw_frac);
        long[] flat = new long[rows * cols];
        int postShift = (int)(s_hw_frac - tOut);

        long hwMinFix = (long)Math.ceil(0.21 * (1L << s_hw_frac));
        long safeMin = (totalPreShift > 0) ? (hwMinFix << totalPreShift) : (hwMinFix >> -totalPreShift);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long val = Math.max(addRes[i][j], safeMin);
                int x_hw = (int)((totalPreShift > 0) ? (val >> totalPreShift) : (val << -totalPreShift));
                long rawLog = sim.compute(x_hw);
                long base = (postShift < 0) ? (rawLog << -postShift) : (rawLog >> postShift);
                flat[i * cols + j] = base + compensation;
            }
        }
        return Nd4j.create(flat, new long[]{rows, cols}, DataType.LONG).reshape('c', originalShape);
    }

    private long[] getBroadcastShape(long[] s1, long[] s2) {
        int max = Math.max(s1.length, s2.length);
        long[] res = new long[max];
        for (int i = 1; i <= max; i++) {
            long d1 = (s1.length - i >= 0) ? s1[s1.length - i] : 1;
            long d2 = (s2.length - i >= 0) ? s2[s2.length - i] : 1;
            res[max - i] = Math.max(d1, d2);
        }
        return res;
    }

    private static class LogCordicSimulator {
        private final int bit_frac;
        private final int[] atanh_table;
        private final int log2_fix;
        public LogCordicSimulator(int bit_int, int bit_frac) {
            this.bit_frac = bit_frac;
            this.atanh_table = new int[bit_frac + 1];
            for (int j = 1; j <= bit_frac; j++) {
                double v = 0.5 * Math.log((1.0 + Math.pow(2.0, -j)) / (1.0 - Math.pow(2.0, -j)));
                atanh_table[j] = (int) Math.round(v * (1L << bit_frac));
            }
            this.log2_fix = (int) Math.round(Math.log(2.0) * (1L << bit_frac));
        }
        public long compute(int x_fixed) {
            if (x_fixed <= 0) return Long.MIN_VALUE;
            int msb = 31 - Integer.numberOfLeadingZeros(x_fixed);
            int k = msb - bit_frac;
            int x_norm = (bit_frac - msb >= 0) ? (x_fixed << (bit_frac - msb)) : (x_fixed >> (msb - bit_frac));
            long x_n = (long) x_norm + (1L << bit_frac), y_n = (long) x_norm - (1L << bit_frac), z_n = 0;
            for (int j = 1; j <= bit_frac; j++) {
                long sy = (y_n >= 0L) ? -1L : 1L;
                long tx = x_n, ty = y_n;
                x_n = tx + (sy * (ty >> j)); y_n = ty + (sy * (tx >> j)); z_n = z_n - (sy * atanh_table[j]);
                if (j == 4 || j == 13) {
                    sy = (y_n >= 0L) ? -1L : 1L; tx = x_n; ty = y_n;
                    x_n = tx + (sy * (ty >> j)); y_n = ty + (sy * (tx >> j)); z_n = z_n - (sy * atanh_table[j]);
                }
            }
            return 2L * z_n + (long) k * log2_fix;
        }
    }
}
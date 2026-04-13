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

    private final long T_OUT = 19L;
    private final long S_DATA = 19L; // 对应 fpga_in_shift

    @Test
    public void testAddLog2D() throws Exception {
        System.out.println("\n--- Testing 2D AddLog (Static Shift Bit-Match) ---");
        int rows = 64, cols = 128;
        runFormattedTest(new long[]{rows, cols}, "Test2D_AddLog");
    }

    @Test
    public void testAddLog3D() throws Exception {
        System.out.println("\n--- Testing 3D AddLog (Static Shift Bit-Match) ---");
        int batch = 2, rows = 32, cols = 32;
        runFormattedTest(new long[]{batch, rows, cols}, "Test3D_AddLog");
    }

    private void runFormattedTest(long[] shape, String nodeName) throws Exception {
        // 1. 获取硬件参数
        long s_hw_frac = AcceleratorSimInterface.acceleratorCfg().fracWidth(); // intWidth=12 时这里是 12
        long internalAddShift = 20L; // 对应 fpga_after_mat_shift
        long fpgaBeforeActShift = 20L; // 对应 fpga_before_act_shift

        int staticTPS = (int) (fpgaBeforeActShift - s_hw_frac);
        // 2. 生成原始随机数据 (mA, mB)
        // 使用 double 显式转换，并传入随机数生成器（通常使用 Nd4j.getRandom()）
        INDArray mA = Transforms.round(Nd4j.rand(shape, 1.0, 15.0, Nd4j.getRandom()).mul(Math.pow(2, S_DATA)));
        INDArray mB = Transforms.round(Nd4j.rand(shape, 1.0, 15.0, Nd4j.getRandom()).mul(Math.pow(2, S_DATA)));

        // 3. 模拟算子内部求和 (基于 internalAddShift)
        // 强制对齐到 20 位中间层
        double scaleToInternal = Math.pow(2, internalAddShift - S_DATA);
        // --- 修改 HWAcceleratedAddLogV13Test.java 中的理论值计算部分 ---

        // 1. 获取硬件加法后的原始和 (20位平面)
        INDArray sumArr = mA.add(mB).mul(scaleToInternal);

        // 2. 【关键】模拟硬件指令的截断行为 (addShiftAmount = 8)
        // 硬件实际上是拿这个 shiftedSum 在算对数
        INDArray shiftedSum = Transforms.floor(sumArr.div(Math.pow(2, staticTPS)));

        // 3. 计算基于硬件入口的“真值”
        // 硬件的 fracWidth 是 12，所以除以 2^12 才是硬件认定的真值
        INDArray hwRealValue = shiftedSum.div(Math.pow(2, s_hw_frac));

        // 4. 对这个“截断后的真值”取对数，并放大回输出位宽
        INDArray theoretical = Transforms.round(
                Transforms.log(hwRealValue).mul(Math.pow(2, T_OUT))
        );


        // 6. 模拟器计算
        INDArray simulated = calculateSimulatedFixedPointAddLog(
                mA, mB, S_DATA, S_DATA, internalAddShift, fpgaBeforeActShift, T_OUT, staticTPS
        );

        // 7. 算子计算
        HWAcceleratedAddLogV13 operator = new HWAcceleratedAddLogV13();
        INDArray actual = operator.addLog(
                mA, mB, S_DATA, S_DATA, internalAddShift, fpgaBeforeActShift, T_OUT, nodeName
        );

        // 8. 验证
        // 重点：Logic Error (模拟与实际绝对误差) 必须为 0
        HWAcceleratedTestModel.validate(nodeName, theoretical, simulated, actual, 1000.0);
    }

    private INDArray calculateSimulatedFixedPointAddLog(INDArray a, INDArray b, long sA, long sB,
                                                        long internalAddShift, long fpgaBeforeActShift,
                                                        long tOut, int staticTPS) {
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

        int rows = (int) a.rows();
        int cols = (int) a.columns();
        long s_hw_frac = AcceleratorSimInterface.acceleratorCfg().fracWidth();

        // 模拟算子内部：Rescale -> Add
        int resA = (int)(sA - internalAddShift);
        int resB = (int)(sB - internalAddShift);
        long[][] addRes = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long vA = (resA < 0) ? (a.getLong(i, j) << -resA) : (a.getLong(i, j) >> resA);
                long vB = (resB < 0) ? (b.getLong(i, j) << -resB) : (b.getLong(i, j) >> resB);
                addRes[i][j] = Math.max(vA + vB, 1L);
            }
        }

        // 使用静态 TPS (对应指令里的 addShiftAmount)
        int totalPreShift = staticTPS;

        LogCordicSimulator sim = new LogCordicSimulator(13, (int)s_hw_frac);
        long[] flat = new long[rows * cols];
        int postShift = (int)(s_hw_frac - tOut); // 12 - 19 = -7

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long val = addRes[i][j];
                // 执行硬件预位移
                int x_hw = (int)((totalPreShift > 0) ? (val >> totalPreShift) : (val << -totalPreShift));
                long rawLog = sim.compute(x_hw);

                // 执行硬件后位移 (左移 7 位)
                long base = (postShift < 0) ? (rawLog << -postShift) : (rawLog >> postShift);

                // 最终结果 (无动态补偿)
                flat[i * cols + j] = (long)((int)base);
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
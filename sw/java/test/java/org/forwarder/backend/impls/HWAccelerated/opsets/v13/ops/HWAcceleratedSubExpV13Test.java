package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

public class HWAcceleratedSubExpV13Test extends HWAcceleratedTestCase {

    /* ============================================================
       固定点 Sub 仿真（支持 >2D，模拟硬件 A - B 的定点行为）
       ============================================================ */
    private INDArray simulateSub(
            INDArray a,
            INDArray b,
            long sourceShiftA,
            long sourceShiftB,
            long targetInputShiftA,
            long targetInputShiftB,
            long targetOutputShift
    ) {
        // 处理高维情况
        if (a.rank() > 2) {
            long[] shape = a.shape();
            long cols = shape[shape.length - 1];
            long rows = a.length() / cols;

            INDArray a2D = a.reshape('c', rows, cols);
            INDArray b2D = b.reshape('c', rows, cols);

            INDArray result2D = simulateSub(
                    a2D, b2D,
                    sourceShiftA, sourceShiftB,
                    targetInputShiftA, targetInputShiftB,
                    targetOutputShift
            );

            return result2D.reshape('c', shape);
        }

        int rows = (int) a.rows();
        int cols = (int) a.columns();

        // 计算 Rescale 位移
        int rescaleA = (int) (sourceShiftA - targetInputShiftA);
        int rescaleB = (int) (sourceShiftB - targetInputShiftB);
        // 注意：subExp 内部硬件执行后可能还涉及从输入层级到输出层级的最终 Shift
        int finalShift = (int) (targetInputShiftA - targetOutputShift);

        long[] result = new long[rows * cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long valA = a.getLong(i, j);
                long valB = b.getLong(i, j);

                // 对齐到硬件输入 Shift
                long fixedA = (rescaleA < 0) ? (valA << -rescaleA) : (valA >> rescaleA);
                long fixedB = (rescaleB < 0) ? (valB << -rescaleB) : (valB >> rescaleB);

                long diff = fixedA - fixedB;

                // 模拟输出位移
                long shifted = (finalShift < 0)
                        ? (diff << -finalShift)
                        : (diff >> finalShift);

                result[i * cols + j] = shifted;
            }
        }

        return Nd4j.create(result, new long[]{rows, cols}, a.dataType());
    }

    /* ============================================================
       固定点 Exp 理论参考
       ============================================================ */
    private INDArray simulateExp(
            INDArray x,
            long sourceShift,
            long targetOutputShift
    ) {
        // 还原为浮点数进行标准 Exp 运算
        INDArray floatX = x.div(Math.pow(2, sourceShift));
        INDArray floatExp = Transforms.exp(floatX, true);
        // 再量化回目标位移
        return Transforms.round(
                floatExp.mul(Math.pow(2, targetOutputShift))
        );
    }

    /* ============================================================
       2D 测试
       ============================================================ */
    @Test
    public void testSubExp2D() throws Exception {
        int rows = 64;
        int cols = 64;

        long sourceShift = 18L;
        long targetInA = 18L;
        long targetInB = 18L;
        long targetOut = 20L;

        float min = -0.5f;
        float max = 0.5f;

        INDArray A = Nd4j.create(
                HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, min, max, sourceShift)
        );

        INDArray B = Nd4j.create(
                HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, min, max, sourceShift)
        );

        /* ---------- theoretical (纯浮点模拟后的量化) ---------- */
        INDArray sub = A.sub(B);
        INDArray theoretical = simulateExp(sub, targetInA, targetOut);

        /* ---------- simulated (模拟硬件定点逻辑) ---------- */
        // 模拟 subExp 内部先做 A-B 的过程，由于 Exp 紧跟其后，第一步输出 Shift 通常等于输入 Shift
        INDArray simulatedSub = simulateSub(A, B, sourceShift, sourceShift, targetInA, targetInB, targetInA);
        INDArray simulated = simulateExp(simulatedSub, targetInA, targetOut);

        /* ---------- actual (运行你的算子) ---------- */
        HWAcceleratedSubExpV13 op = new HWAcceleratedSubExpV13();
        INDArray actual = op.subExp(
                A, B,
                sourceShift, sourceShift,
                targetInA, targetInB,
                targetOut,
                "SubExp2D"
        );

        /* ---------- validate ---------- */
        HWAcceleratedTestModel.validate(
                "SubExp 2D",
                theoretical,
                simulated,
                actual,
                10000.0 // 容忍误差
        );
    }

    /* ============================================================
       3D 测试 (验证 Rank Handling 和 Reshape 逻辑)
       ============================================================ */
    @Test
    public void testSubExp3D() throws Exception {
        int batch = 2;
        int rows = 32;
        int cols = 32;

        long sourceShift = 16L;
        long targetInA = 16L;
        long targetInB = 16L;
        long targetOut = 18L;

        INDArray A = Transforms.round(
                Nd4j.rand(batch, rows, cols).mul(1.0).sub(0.5).mul(1L << sourceShift), false
        );
        INDArray B = Transforms.round(
                Nd4j.rand(batch, rows, cols).mul(1.0).sub(0.5).mul(1L << sourceShift), false
        );

        INDArray sub = A.sub(B);
        INDArray theoretical = simulateExp(sub, targetInA, targetOut);

        INDArray simulatedSub = simulateSub(A, B, sourceShift, sourceShift, targetInA, targetInB, targetInA);
        INDArray simulated = simulateExp(simulatedSub, targetInA, targetOut);

        HWAcceleratedSubExpV13 op = new HWAcceleratedSubExpV13();
        INDArray actual = op.subExp(A, B, sourceShift, sourceShift, targetInA, targetInB, targetOut, "SubExp3D");

        HWAcceleratedTestModel.validate("SubExp 3D", theoretical, simulated, actual, 10000.0);
    }
}
package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

public class HWAcceleratedAddExpV13Test extends HWAcceleratedTestCase {

    /* ============================================================
       固定点 Add 仿真（支持 >2D）
       ============================================================ */
    private INDArray simulateAdd(
            INDArray a,
            INDArray b,
            long sourceShiftA,
            long sourceShiftB,
            long targetInputShift,
            long targetOutputShift
    ) {

        if (a.rank() > 2) {
            long[] shape = a.shape();
            long cols = shape[shape.length - 1];
            long rows = a.length() / cols;

            INDArray a2D = a.reshape('c', rows, cols);
            INDArray b2D = b.reshape('c', rows, cols);

            INDArray result2D = simulateAdd(
                    a2D, b2D,
                    sourceShiftA, sourceShiftB,
                    targetInputShift,
                    targetOutputShift
            );

            return result2D.reshape('c', shape);
        }

        int rows = (int) a.rows();
        int cols = (int) a.columns();

        int rescaleA = (int) (sourceShiftA - targetInputShift);
        int rescaleB = (int) (sourceShiftB - targetInputShift);
        int finalShift = (int) (targetInputShift - targetOutputShift);

        long[] result = new long[rows * cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {

                long valA = a.getLong(i, j);
                long valB = b.getLong(i, j);

                long fixedA = (rescaleA < 0) ? (valA << -rescaleA) : (valA >> rescaleA);
                long fixedB = (rescaleB < 0) ? (valB << -rescaleB) : (valB >> rescaleB);

                long sum = fixedA + fixedB;

                long shifted = (finalShift < 0)
                        ? (sum << -finalShift)
                        : (sum >> finalShift);

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
        INDArray floatX = x.div(Math.pow(2, sourceShift));
        INDArray floatExp = Transforms.exp(floatX, true);
        return Transforms.round(
                floatExp.mul(Math.pow(2, targetOutputShift))
        );
    }

    /* ============================================================
       2D 测试
       ============================================================ */
    @Test
    public void testAddExp2D() throws Exception {

        int rows = 64;
        int cols = 64;

        long sourceShift = 18L;
        long targetInputShift = 18L;
        long targetOutputShift = 20L;

        float min = -1f;
        float max = 1f;

        INDArray A = Nd4j.create(
                HWAcceleratedTestModel.generateRandom2DFloatMatrix(
                        rows, cols, min, max, sourceShift
                )
        );

        INDArray B = Nd4j.create(
                HWAcceleratedTestModel.generateRandom2DFloatMatrix(
                        rows, cols, min, max, sourceShift
                )
        );

        /* ---------- theoretical ---------- */
        INDArray add = A.add(B);
        INDArray theoretical =
                simulateExp(add, targetInputShift, targetOutputShift);

        /* ---------- simulated ---------- */
        INDArray simulatedAdd =
                simulateAdd(A, B,
                        sourceShift, sourceShift,
                        targetInputShift,
                        targetInputShift);

        INDArray simulated =
                simulateExp(simulatedAdd,
                        targetInputShift,
                        targetOutputShift);

        /* ---------- actual ---------- */
        HWAcceleratedAddExpV13 op = new HWAcceleratedAddExpV13();

        INDArray actual = op.addExp(
                A, B,
                sourceShift, sourceShift,
                targetInputShift,
                targetInputShift,
                targetOutputShift,
                "AddExp2D"
        );

        HWAcceleratedTestModel.validate(
                "AddExp 2D",
                theoretical,
                simulated,
                actual,
                10000.0
        );
    }

    /* ============================================================
       3D 测试
       ============================================================ */
    @Test
    public void testAddExp3D() throws Exception {

        int batch = 4;
        int rows = 32;
        int cols = 32;

        long sourceShift = 18L;
        long targetInputShift = 18L;
        long targetOutputShift = 20L;

        float min = -1f;
        float max = 1f;

        INDArray A = Transforms.round(
                Nd4j.rand(batch, rows, cols)
                        .mul(max - min)
                        .add(min)
                        .mul(1L << sourceShift),
                false
        );


        INDArray B = Transforms.round(
                Nd4j.rand(batch, rows, cols)
                        .mul(max - min)
                        .add(min)
                        .mul(1L << sourceShift),
                false
        );


        /* ---------- theoretical ---------- */
        INDArray add = A.add(B);
        INDArray theoretical =
                simulateExp(add,
                        targetInputShift,
                        targetOutputShift);

        /* ---------- simulated ---------- */
        INDArray simulatedAdd =
                simulateAdd(A, B,
                        sourceShift, sourceShift,
                        targetInputShift,
                        targetInputShift);

        INDArray simulated =
                simulateExp(simulatedAdd,
                        targetInputShift,
                        targetOutputShift);

        /* ---------- actual ---------- */
        HWAcceleratedAddExpV13 op = new HWAcceleratedAddExpV13();

        INDArray actual = op.addExp(
                A, B,
                sourceShift, sourceShift,
                targetInputShift,
                targetInputShift,
                targetOutputShift,
                "AddExp3D"
        );

        HWAcceleratedTestModel.validate(
                "AddExp 3D",
                theoretical,
                simulated,
                actual,
                10000.0
        );
    }
}

package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.List;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedSession;
import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Test;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class HWAcceleratedWhereV13Test extends HWAcceleratedTestCase {

    private final HWAcceleratedWhereV13 op = new HWAcceleratedWhereV13();

    private INDArray calculateSimulatedFixedPointWhere(
            INDArray condition, INDArray x, INDArray y,
            long sourceShiftX, long sourceShiftY,
            long commonShift
    ) {
        int rescaleAmountX = (int) (sourceShiftX - commonShift);
        INDArray rescaledX = (rescaleAmountX == 0)
                ? x
                : (rescaleAmountX < 0
                ? x.dup().mul(1L << -rescaleAmountX)
                : x.dup().div(1L << rescaleAmountX)
        );
        rescaledX = rescaledX.castTo(x.dataType());

        int rescaleAmountY = (int) (sourceShiftY - commonShift);
        INDArray rescaledY = (rescaleAmountY == 0)
                ? y
                : (rescaleAmountY < 0
                ? y.dup().mul(1L << -rescaleAmountY)
                : y.dup().div(1L << rescaleAmountY)
        );
        rescaledY = rescaledY.castTo(y.dataType());

        long[] shapeCondX = broadcastShapes(condition.shape(), rescaledX.shape());
        long[] finalShape = broadcastShapes(shapeCondX, rescaledY.shape());

        INDArray condB = condition.broadcast(finalShape);
        INDArray xB = rescaledX.broadcast(finalShape);
        INDArray yB = rescaledY.broadcast(finalShape);

        INDArray mask = condB.castTo(xB.dataType());
        return mask.mul(xB).add(mask.rsub(1).mul(yB));
    }

    private long[] broadcastShapes(long[] a, long[] b) {
        int maxRank = Math.max(a.length, b.length);
        long[] result = new long[maxRank];
        for (int i = 1; i <= maxRank; i++) {
            long dimA = (i <= a.length) ? a[a.length - i] : 1;
            long dimB = (i <= b.length) ? b[b.length - i] : 1;
            if (dimA != dimB && dimA != 1 && dimB != 1) {
                throw new IllegalArgumentException(
                        "Shapes " + Arrays.toString(a) + " and " + Arrays.toString(b) + " are not broadcastable."
                );
            }
            result[maxRank - i] = Math.max(dimA, dimB);
        }
        return result;
    }

    @Test
    public void testBroadcastedWhere() {
        INDArray condition = Nd4j.create(new boolean[][] {
                {true},
                {false},
                {true}
        });
        INDArray x = Nd4j.create(new float[][] {
                {10f, 20f, 30f, 40f}
        }).broadcast(3, 4);
        INDArray y = Nd4j.create(new float[][] {
                {1f, 2f, 3f, 4f},
                {5f, 6f, 7f, 8f},
                {9f,10f,11f,12f}
        });
        INDArray expected = Nd4j.create(new float[][] {
                {10f, 20f, 30f, 40f},
                {5f, 6f, 7f, 8f},
                {10f, 20f, 30f, 40f}
        });

        checkWhere(expected, condition, x, y, 0L, 0L, 0L); // MODIFIED: 调用带 shift 的 check
    }

    //测试 2：完全相同 shape
    @Test
    public void testSameShapeWhere() {
        INDArray condition = Nd4j.create(new boolean[][] {
                {true, false},
                {false, true}
        });
        INDArray x = Nd4j.create(new float[][] {
                {1f, 2f},
                {3f, 4f}
        });
        INDArray y = Nd4j.create(new float[][] {
                {5f, 6f},
                {7f, 8f}
        });
        INDArray expected = Nd4j.create(new float[][] {
                {1f, 6f},
                {7f, 4f}
        });

        checkWhere(expected, condition, x, y, 0L, 0L, 0L); // MODIFIED: 调用带 shift 的 check
    }

    //测试 3：1D 向量广播
    @Test
    public void test1DWhereBroadcast() {
        INDArray condition = Nd4j.create(new boolean[] {true, false, true});
        INDArray x = Nd4j.create(new float[] {1f, 2f, 3f});
        INDArray y = Nd4j.create(new float[] {4f, 5f, 6f});
        INDArray expected = Nd4j.create(new float[] {1f, 5f, 3f});

        checkWhere(expected, condition, x, y, 0L, 0L, 0L); // MODIFIED: 调用带 shift 的 check
    }

    //测试 4：scalar condition 广播
    @Test
    public void testScalarCondition() {
        INDArray condition = Nd4j.scalar(true);
        INDArray x = Nd4j.create(new float[] {1f, 2f, 3f});
        INDArray y = Nd4j.create(new float[] {4f, 5f, 6f});
        INDArray expected = Nd4j.create(new float[] {1f, 2f, 3f});

        checkWhere(expected, condition, x, y, 0L, 0L, 0L); // MODIFIED: 调用带 shift 的 check

    }

    @Test
    public void testMixedWhere() {
        INDArray condition = Nd4j.create(new boolean[][] {
                {true, false, true},
                {false, true, false}
        });
        INDArray x = Nd4j.create(new float[] {9f}).broadcast(2, 3);
        INDArray y = Nd4j.create(new float[][] {
                {1f, 2f, 3f},
                {4f, 5f, 6f}
        });
        INDArray expected = Nd4j.create(new float[][] {
                {9f, 2f, 9f},
                {4f, 9f, 6f}
        });

        checkWhere(expected, condition, x, y, 0L, 0L, 0L);
    }

    @Test
    public void testMixedWhereWithShifts() throws Exception {
        System.out.println("\n--- Testing Mixed Broadcast Where with Quantization Shifts ---");

        long sourceShiftX = 10L;
        long sourceShiftY = 12L;
        long commonShift = 14L;

        INDArray condition = Nd4j.createFromArray(new int[][] {
                {1, 0, 1},
                {0, 1, 0}
        }).castTo(DataType.INT);

        int valX = 9216;
        INDArray matrixX = Nd4j.valueArrayOf(new long[]{2, 3}, valX).castTo(DataType.INT);

        INDArray matrixY = Nd4j.createFromArray(new int[][] {
                {4096, 8192, 12288},  // [1, 2, 3] * (1 << 12)
                {16384, 20480, 24576} // [4, 5, 6] * (1 << 12)
        }).castTo(DataType.INT);

        INDArray simulatedExpected = calculateSimulatedFixedPointWhere(
                condition, matrixX, matrixY,
                sourceShiftX, sourceShiftY, commonShift
        );


        INDArray manualExpected = Nd4j.createFromArray(new int[][] {
                {147456, 32768, 147456}, // X << 4, Y << 2, X << 4
                {65536, 147456, 98304}   // Y << 2, X << 4, Y << 2
        }).castTo(DataType.INT);

        HWAcceleratedWhereV13 operator = new HWAcceleratedWhereV13();
        INDArray actualOutput = operator.where(
                condition, matrixX, matrixY,
                sourceShiftX, sourceShiftY, commonShift
        );

        HWAcceleratedTestModel.validate("Where - Mixed Broadcast Quantized", manualExpected, simulatedExpected, actualOutput, 0.0);
    }

    @Test
    public void testWhereWithShifts() throws Exception {
        System.out.println("\n--- Testing Where with Quantization Shifts ---");

        long sourceShiftX = 20L;
        long sourceShiftY = 22L;
        long commonShift = 15L;

        INDArray condition = Nd4j.create(new long[][] {
                {1L, 0L, 1L},
                {0L, 0L, 1L}
        });

        INDArray matrixX = Nd4j.valueArrayOf(new long[]{2, 3}, 128L);
        INDArray matrixY = Nd4j.valueArrayOf(new long[]{2, 3}, 256L);

        INDArray simulatedExpected = calculateSimulatedFixedPointWhere(
                condition, matrixX, matrixY,
                sourceShiftX, sourceShiftY, commonShift
        );

        // 手动验证模拟结果：
        INDArray manualExpected = Nd4j.create(new long[][] {
                {4L, 2L, 4L},
                {2L, 2L, 4L}
        });


        HWAcceleratedWhereV13 operator = new HWAcceleratedWhereV13();
        INDArray actualOutput = operator.where(
                condition, matrixX, matrixY,
                sourceShiftX, sourceShiftY, commonShift
        );

        System.out.println("Expected (Simulated) values:\n" + simulatedExpected);
        System.out.println("Actual values:\n" + actualOutput);

        HWAcceleratedTestModel.validate("Where - Quantized", manualExpected, simulatedExpected, actualOutput, 0.0);
    }

    protected void checkWhere(INDArray expected, INDArray condition, INDArray x, INDArray y,
                              long sourceShiftX, long sourceShiftY, long commonShift) {

        INDArray simulatedExpected = calculateSimulatedFixedPointWhere(
                condition, x, y, sourceShiftX, sourceShiftY, commonShift
        );

        INDArray actual = op.where(condition, x, y, sourceShiftX, sourceShiftY, commonShift);

        System.out.println("Expected (Simulated) shape: " + Arrays.toString(simulatedExpected.shape()));
        System.out.println("Actual shape:               " + Arrays.toString(actual.shape()));
        System.out.println("Expected (Simulated) values:\n" + simulatedExpected);
        System.out.println("Actual values:\n" + actual);

        assertTrue("Mismatch between simulated expected and actual", simulatedExpected.equals(actual));

        if(sourceShiftX == 0 && sourceShiftY == 0 && commonShift == 0) {
            assertTrue("Mismatch between theoretical expected and actual", expected.equals(actual));
        }
    }

}
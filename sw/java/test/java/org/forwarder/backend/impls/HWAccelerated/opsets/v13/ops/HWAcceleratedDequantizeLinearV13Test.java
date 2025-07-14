package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class HWAcceleratedDequantizeLinearV13Test extends HWAcceleratedTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Helper to invoke dequantizeLinear via reflection, normalize axis, and unwrap exceptions.
     */
    @SuppressWarnings("unchecked")
    private INDArray invokeDequantize(HWAcceleratedDequantizeLinearV13 op,
                                      INDArray x,
                                      INDArray xScale,
                                      INDArray xZeroPoint,
                                      int axisParam) {

        int rank = x.rank();
        int axis = axisParam < 0 ? axisParam + rank : axisParam;
        if (axis < 0 || axis >= rank) {
            throw new IllegalArgumentException(
                    String.format("axis=%d is invalid for input tensor of rank %d", axisParam, rank)
            );
        }
        try {
            Method m = HWAcceleratedDequantizeLinearV13.class
                    .getDeclaredMethod("dequantizeLinear",
                            INDArray.class, INDArray.class, INDArray.class, int.class);
            m.setAccessible(true);
            return (INDArray) m.invoke(op, x, xScale, xZeroPoint, axis);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void testDequantize(INDArray expected,
                                INDArray x,
                                INDArray xScale,
                                INDArray xZeroPoint,
                                int axis) {
        HWAcceleratedDequantizeLinearV13 op = new HWAcceleratedDequantizeLinearV13();
        INDArray result = invokeDequantize(op, x, xScale, xZeroPoint, axis);
        System.out.printf("Expected: %s, Actual: %s%n",
                expected.shapeInfoToString(), result.shapeInfoToString());
        assertArrayEquals("Shape mismatch",
                expected.shape(), result.shape());
        assertTrue("Values differ: expected " + expected + " but got " + result,
                expected.equalsWithEps(result, 1e-6));
    }


    @Test
    public void test1_PerTensorWithExplicitZeroPoint() {
        // y = (x - 128) * 2
        testDequantize(
                Nd4j.createFromArray(new float[]{-256f, -250f, 0f, 254f}),
                Nd4j.createFromArray(new float[]{0f, 3f, 128f, 255f}),
                Nd4j.scalar(2f),
                Nd4j.scalar(128f),
                0
        );
    }

    @Test
    public void test2_DefaultZeroPointPerTensor() {
        // y = x * 0.5
        testDequantize(
                Nd4j.createFromArray(new float[]{0.5f, 1.0f, 1.5f}),
                Nd4j.createFromArray(new float[]{1f, 2f, 3f}),
                Nd4j.scalar(0.5f),
                Nd4j.scalar(0f),
                0
        );
    }

    @Test
    public void test3_PerAxisBroadcast() {
        // x = [[10,20],[30,40]], scale=[1,10], zp=0
        testDequantize(
                Nd4j.createFromArray(new float[][]{{10f,200f},{30f,400f}}),
                Nd4j.createFromArray(new float[][]{{10f,20f},{30f,40f}}),
                Nd4j.createFromArray(new float[]{1f,10f}),
                Nd4j.scalar(0f),
                1
        );
    }

    @Test
    public void test4_NegativeAxisIndexing() {
        // x = [[5,6,7]], scale=[1,2,3], zp=0, axis=-1
        testDequantize(
                Nd4j.createFromArray(new float[][]{{5f,12f,21f}}),
                Nd4j.createFromArray(new float[][]{{5f,6f,7f}}),
                Nd4j.createFromArray(new float[]{1f,2f,3f}),
                Nd4j.scalar(0f),
                -1
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test5_AxisOutOfRange() {
        invokeDequantize(
                new HWAcceleratedDequantizeLinearV13(),
                Nd4j.createFromArray(new float[]{1f,2f,3f}),
                Nd4j.scalar(1f),
                Nd4j.scalar(0f),
                5
        );
    }

    @Test
    public void test6_IntTypeDefaultZeroPoint() {
        // int tensor [-10,0,10], scale=2, zp omitted=0
        testDequantize(
                Nd4j.createFromArray(new float[]{-20f,0f,20f}),
                Nd4j.createFromArray(new int[]{-10,0,10})
                        .castTo(DataType.INT),
                Nd4j.scalar(2f),
                Nd4j.scalar(0).castTo(DataType.INT),
                0
        );
    }
}

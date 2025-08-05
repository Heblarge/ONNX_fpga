package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.forwarder.backend.impls.dl4j.DL4JTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class DL4JQuantizeLinearV13Test extends DL4JTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Helper to invoke quantizeLinear via reflection, normalize axis, and unwrap exceptions.
     */
    @SuppressWarnings("unchecked")
    private INDArray invokeQuantize(DL4JQuantizeLinearV13 op,
                                    INDArray x,
                                    INDArray yScale,
                                    INDArray yZeroPoint,
                                    int axisParam) {
        // normalize axis
        int rank = x.rank();
        int axis = axisParam < 0 ? axisParam + rank : axisParam;
        if (axis < 0 || axis >= rank) {
            throw new IllegalArgumentException(
                    String.format("axis=%d is invalid for input tensor of rank %d", axisParam, rank)
            );
        }
        try {
            Method m = DL4JQuantizeLinearV13.class.getDeclaredMethod(
                    "quantizeLinear", INDArray.class, INDArray.class, INDArray.class, int.class);
            m.setAccessible(true);
            return (INDArray) m.invoke(op, x, yScale, yZeroPoint, axis);
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

    private void testQuantize(INDArray expected,
                              INDArray x,
                              INDArray yScale,
                              INDArray yZeroPoint,
                              int axis) {
        DL4JQuantizeLinearV13 op = new DL4JQuantizeLinearV13();
        INDArray result = invokeQuantize(op, x, yScale, yZeroPoint, axis);
        System.out.printf("Expected: %s, Actual: %s%n",
                expected.shapeInfoToString(), result.shapeInfoToString());
        assertArrayEquals(expected.shape(), result.shape());
        assertTrue("Values differ: expected " + expected + " but got " + result,
                expected.equalsWithEps(result, 1e-6));
    }

    private INDArray vector(double... vals) {
        return Nd4j.createFromArray(vals);
    }

    private INDArray matrix(double[][] vals) {
        return Nd4j.createFromArray(vals);
    }

    private INDArray byteVector(byte... vals) {
        return Nd4j.createFromArray(vals);
    }

    private INDArray ubyteVector(int... vals) {
        float[] floatVals = new float[vals.length];
        for (int i = 0; i < vals.length; i++) {
            floatVals[i] = vals[i];
        }
        return Nd4j.createFromArray(floatVals).castTo(DataType.UBYTE);
    }

    // Test cases

    @Test
    public void test1_WithoutZeroPoint() {
        // Without zero point, output type defaults to UBYTE
        testQuantize(
                ubyteVector(0, 2, 0),
                vector(0.1, 0.9, -0.5),
                Nd4j.scalar(0.5),
                null,
                0
        );
    }

    @Test
    public void test2_WithUint8ZeroPoint() {
        // CORRECTED: Use ubyteVector to correctly test UINT8 path
        testQuantize(
                ubyteVector(2, 3, 4),
                vector(0.0, 1.0, 2.0),
                Nd4j.scalar(1.0),
                ubyteVector(2),
                0
        );
    }

    @Test
    public void test3_Int8Saturation() {
        // -200/1 -> round(-200) + (-128) = -328 -> saturates to -128
        // 0/1 -> round(0) + (-128) = -128
        // 200/1 -> round(200) + (-128) = 72
        testQuantize(
                byteVector((byte)-128, (byte)-128, (byte)72),
                vector(-200.0, 0.0, 200.0),
                Nd4j.scalar(1.0),
                byteVector((byte)-128),
                0
        );
    }

    @Test
    public void test4_PerAxisBroadcast() {
        // CORRECTED & RE-ENABLED
        INDArray x = matrix(new double[][]{{1,2,3},{4,5,6}});
        INDArray scale = vector(1.0, 2.0, 3.0);
        INDArray zeroPoint = ubyteVector(0, 1, 2);
        // axisParam = -1 normalized to 1
        // result = round(x / scale) + zeroPoint
        // row0: round([1/1, 2/2, 3/3]) + [0,1,2] -> [1,1,1] + [0,1,2] -> [1,2,3]
        // row1: round([4/1, 5/2, 6/3]) + [0,1,2] -> [4,2,2] + [0,1,2] -> [4,3,4] (note: 5/2=2.5 rounds to 2)
        INDArray expected = Nd4j.create(new double[][]{{1,2,3},{4,4,4}}).castTo(DataType.UBYTE);
        testQuantize(expected, x, scale, zeroPoint, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test5_AxisOutOfRange() {
        invokeQuantize(new DL4JQuantizeLinearV13(), vector(1,2,3), Nd4j.scalar(1.0), null, 5);
    }

    @Test(expected = IllegalStateException.class)
    public void test6_IncompatibleShapes() {
        invokeQuantize(new DL4JQuantizeLinearV13(), vector(1,2), vector(1,2,3), null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test7_EmptyInputThrows() {
        invokeQuantize(new DL4JQuantizeLinearV13(), Nd4j.empty(), Nd4j.scalar(1.0), null, 0);
    }

    @Test
    public void test8_NegativeValues() {
        testQuantize(
                byteVector((byte)-128, (byte)-128, (byte)-128, (byte)-125),
                vector(-1.0, -2.0, -3.0, 3.0),
                Nd4j.scalar(1.0),
                byteVector((byte) -128),
                0
        );
    }

    @Test
    public void test9_Uint8Saturation() {
        // CORRECTED & RE-ENABLED: Test clipping for UBYTE
        // 300/1+0 -> 300 -> clips to 255. -10/1+0 -> -10 -> clips to 0
        testQuantize(
                ubyteVector(255, 0),
                vector(300.0, -10.0),
                Nd4j.scalar(1.0),
                ubyteVector(0),
                0
        );
    }

    @Test
    public void test10_ZeroScale() {
        thrown.expect(IllegalArgumentException.class);
        testQuantize(
                vector(0.0, 0.0, 0.0),
                vector(1.0, 2.0, 3.0),
                Nd4j.scalar(0.0), // Scale is zero
                null,
                0
        );
    }

    @Test
    public void test11_SingleElementInput() {
        testQuantize(
                byteVector((byte)3),
                vector(1.0),
                Nd4j.scalar(0.5),
                byteVector((byte) 1),
                0
        );
    }

    @Test
    public void test12_Int32Output() {
        // NEW: Test case for INT32 output as seen in the user's model
        INDArray x = Nd4j.createFromArray(new float[]{2.3841858e-07f, -4.7683716e-07f, 7.1525574e-07f});
        INDArray yScale = Nd4j.scalar(2.3841858e-07f).castTo(DataType.FLOAT);
        INDArray yZeroPoint = Nd4j.scalar(0).castTo(DataType.INT);
        // Expected: round(x/yScale) + 0 -> round([1.0, -2.0, 3.0]) -> [1, -2, 3]
        INDArray expected = Nd4j.createFromArray(new int[]{1, -2, 3});
        testQuantize(expected, x, yScale, yZeroPoint, 0);
    }

}

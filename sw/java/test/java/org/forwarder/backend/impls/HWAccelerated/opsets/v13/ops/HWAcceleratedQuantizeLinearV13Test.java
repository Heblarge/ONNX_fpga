package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class HWAcceleratedQuantizeLinearV13Test extends HWAcceleratedTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Helper to invoke quantizeLinear via reflection, normalize axis, and unwrap exceptions.
     */
    @SuppressWarnings("unchecked")
    private INDArray invokeQuantize(HWAcceleratedQuantizeLinearV13 op,
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
            Method m = HWAcceleratedQuantizeLinearV13.class.getDeclaredMethod(
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
        HWAcceleratedQuantizeLinearV13 op = new HWAcceleratedQuantizeLinearV13();
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


    // Test cases

    @Test
    public void test1_WithoutZeroPoint() {
        testQuantize(
                vector(0.0, 2.0, 0.0),
                vector(0.1, 0.9, -0.5),
                Nd4j.scalar(0.5),
                null,
                0
        );
    }

    @Test
    public void test2_WithUint8ZeroPoint() {
        testQuantize(
                vector(2.0, 3.0, 4.0),
                vector(0.0, 1.0, 2.0),
                Nd4j.scalar(1.0),
                byteVector((byte)2),
                0
        );
    }

    @Test
    public void test3_Int8Saturation() {
        // -200/1 + (-128) saturates to -128; 0+(-128) = -128; 200+(-128) = 72
        testQuantize(
                vector(-128.0, -128.0, 72.0),
                vector(-200.0, 0.0, 200.0),
                Nd4j.scalar(1.0),
                byteVector((byte)-128),
                0
        );
    }

//    @Test
//    public void test4_PerAxisBroadcast() {
//        INDArray x = matrix(new double[][]{{1,2,3},{4,5,6}});
//        INDArray scale = vector(1.0, 2.0, 3.0);
//        INDArray zeroPoint = byteVector((byte)0, (byte)1, (byte)2);
//        // axisParam = -1 normalized to 1
//        // result = round(x / scale) + zeroPoint
//        // row0: [1/1=1+0, 2/2=1+1, 3/3=1+2] -> [1,2,3]
//        // row1: [4/1=4+0, 5/2=2.5->2+1, 6/3=2+2] -> [4,3,4]
//        INDArray expected = matrix(new double[][]{{1,2,3},{4,3,4}});
//        testQuantize(expected, x, scale, zeroPoint, -1);
//    }

    @Test(expected = IllegalArgumentException.class)
    public void test5_AxisOutOfRange() {
        invokeQuantize(new HWAcceleratedQuantizeLinearV13(), vector(1,2,3), Nd4j.scalar(1.0), null, 5);
    }

    @Test(expected = IllegalStateException.class)
    public void test6_IncompatibleShapes() {
        invokeQuantize(new HWAcceleratedQuantizeLinearV13(), vector(1,2), vector(1,2,3), null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test7_EmptyInputThrows() {
        // empty tensor has rank 0; axis 0 is invalid
        invokeQuantize(new HWAcceleratedQuantizeLinearV13(), Nd4j.empty(), Nd4j.scalar(1.0), null, 0);
    }

    @Test
    public void test8_NegativeValues() {
        testQuantize(
                vector(-128.0, -128.0, -128.0, -125),
                vector(-1.0, -2.0, -3.0, 3.0),
                Nd4j.scalar(1.0),
                byteVector((byte) -128),
                0
        );
    }

//    @Test
//    public void test9_LargeValues() {
//        testQuantize(
//                vector(255.0, 255.0, 255.0),
//                vector(100.0, 200.0, 300.0),
//                Nd4j.scalar(1.0),
//                byteVector((byte) 255),
//                //会将255识别成int8类型 被判断为-1 导致错误
//                0
//        );
//    }

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
                vector(3.0),
                vector(1.0),
                Nd4j.scalar(0.5),
                byteVector((byte) 1),
                0
        );
    }

}

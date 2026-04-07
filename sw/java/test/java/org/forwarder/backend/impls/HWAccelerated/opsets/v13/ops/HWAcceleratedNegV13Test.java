//package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;
//
//import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
//import org.junit.Test;
//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.factory.Nd4j;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertArrayEquals;
//import static org.junit.Assert.assertTrue;
//import static org.junit.Assert.fail;
//
//
//
//import static org.junit.Assert.assertArrayEquals;
//
//public class HWAcceleratedNegV13Test extends HWAcceleratedTestCase {
//
//    private void assertINDArrayEqualsWithNaN(INDArray expected, INDArray actual, double eps) {
//        assertArrayEquals(expected.shape(), actual.shape());
//        long length = expected.length();
//        for (int i = 0; i < length; i++) {
//            double e = expected.getDouble(i);
//            double a = actual.getDouble(i);
//            if (Double.isNaN(e)) {
//                if (!Double.isNaN(a)) {
//                    fail("Expected NaN at index " + i + " but got " + a);
//                }
//            } else if (Double.isInfinite(e)) {
//                if (e != a) {
//                    fail("Expected " + e + " at index " + i + " but got " + a);
//                }
//            } else {
//                if (Math.abs(e - a) > eps) {
//                    fail("Expected " + e + " at index " + i + " but got " + a);
//                }
//            }
//        }
//    }
//
//    private void testNeg(INDArray expected, INDArray input) {
//        HWAcceleratedNegV13 op = new HWAcceleratedNegV13();
//        INDArray result = op.neg(input);
//        assertINDArrayEqualsWithNaN(expected, result, 1e-6);
//    }
//
//
//
//    @Test
//    public void testScalarInput() throws Exception {
//        INDArray input = Nd4j.scalar(5.0);
//        INDArray expected = Nd4j.scalar(-5.0);
//
//        testNeg(expected, input);
//    }
//
//    @Test
//    public void testZeroInput() throws Exception {
//        INDArray input = Nd4j.scalar(0.0);
//        INDArray expected = Nd4j.scalar(0.0); // -0 = 0
//
//        testNeg(expected, input);
//    }
//
//    @Test
//    public void testVectorInput() throws Exception {
//        INDArray input = Nd4j.create(new double[]{-1.0, 0.0, 1.0, 2.5});
//        INDArray expected = Nd4j.create(new double[]{1.0, 0.0, -1.0, -2.5});
//
//        testNeg(expected, input);
//    }
//
//    @Test
//    public void testMatrixInput() throws Exception {
//        INDArray input = Nd4j.create(new double[][]{{-10.0, 0.0}, {10.0, 100.0}});
//        INDArray expected = Nd4j.create(new double[][]{{10.0, 0.0}, {-10.0, -100.0}});
//
//        testNeg(expected, input);
//    }
//
//    @Test
//    public void testHighDimInput() throws Exception {
//        INDArray input = Nd4j.create(2, 3, 4).assign(0.5);
//        INDArray expected = Nd4j.create(2, 3, 4).assign(-0.5);
//
//        testNeg(expected, input);
//    }
//
//    @Test
//    public void testLargeValues() throws Exception {
//        // 测试大数值输入
//        INDArray largePos = Nd4j.scalar(Double.MAX_VALUE);
//        INDArray largeNeg = Nd4j.scalar(-Double.MAX_VALUE);
//
//        INDArray expectedLargePos = Nd4j.scalar(-Double.MAX_VALUE);
//        INDArray expectedLargeNeg = Nd4j.scalar(Double.MAX_VALUE);
//
//        testNeg(expectedLargePos, largePos);
//        testNeg(expectedLargeNeg, largeNeg);
//    }
//
//    @Test
//    public void testSpecialValues() throws Exception {
//        // 测试特殊值
//        INDArray nan = Nd4j.scalar(Double.NaN);
//        INDArray posInf = Nd4j.scalar(Double.POSITIVE_INFINITY);
//        INDArray negInf = Nd4j.scalar(Double.NEGATIVE_INFINITY);
//
//        INDArray expectedNan = Nd4j.scalar(Double.NaN);
//        INDArray expectedPosInf = Nd4j.scalar(Double.NEGATIVE_INFINITY);
//        INDArray expectedNegInf = Nd4j.scalar(Double.POSITIVE_INFINITY);
//
//        testNeg(expectedNan, nan);
//        testNeg(expectedPosInf, posInf);
//        testNeg(expectedNegInf, negInf);
//    }
//}
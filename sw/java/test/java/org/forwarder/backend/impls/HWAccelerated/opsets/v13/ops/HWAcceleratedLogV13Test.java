//package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;
//
//import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
//import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
//import org.junit.Test;
//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.factory.Nd4j;
//import org.nd4j.linalg.ops.transforms.Transforms;
//
//import java.util.Random;
//
//import static org.junit.Assert.assertArrayEquals;
//import static org.junit.Assert.assertTrue;
//
///**
// * This test class validates the HWAcceleratedLogV13 operator for
// * both 2D and 3D (batched) tensors using floating-point inputs.
// */
//public class HWAcceleratedLogV13Test extends HWAcceleratedTestCase {
//
//    @Test
//    public void testLog2D() throws Exception {
//        System.out.println("\n--- Testing 2D Log ---");
//        int rows = 32;
//        int cols = 32;
//        // Log is only defined for positive numbers
//        int minValue = 1;
//        int maxValue = 50;
//
//        INDArray input = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue));
//        INDArray theoreticalExpected = Transforms.log(input.dup(), true); // Base e
//
//        HWAcceleratedLogV13 operator = new HWAcceleratedLogV13();
//        INDArray actualOutput = operator.log(input.dup());
//        double hardwareLogicTolerance = 1e-4;
//        HWAcceleratedTestModel.validateActivationFunction("Log - 2D", theoreticalExpected, actualOutput, hardwareLogicTolerance);
//    }
//
//    /**
//     * Tests 3D (batched) Log operation with random floats.
//     */
//    @Test
//    public void testLog3D() throws Exception {
//        System.out.println("\n--- Testing 3D (Batched) Log ---");
//        int batchSize = 2;
//        int rows = 28;
//        int cols = 512;
//        int minValue = 1;
//        int maxValue = 10;
//
//        INDArray input = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue);
//        INDArray theoreticalExpected = Transforms.log(input.dup(), true); // Base e
//
//        HWAcceleratedLogV13 operator = new HWAcceleratedLogV13();
//        INDArray actualOutput = operator.log(input.dup());
//        double hardwareLogicTolerance = 1e-4;
//        HWAcceleratedTestModel.validateActivationFunction("Log - 3D", theoreticalExpected, actualOutput, hardwareLogicTolerance);
//    }
//}

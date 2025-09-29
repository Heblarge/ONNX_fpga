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
// * This test class validates the HWAcceleratedReluV13 operator for
// * both 2D and 3D (batched) tensors using floating-point inputs.
// */
//public class HWAcceleratedReluV13Test extends HWAcceleratedTestCase {
//
//    @Test
//    public void testRelu2D() throws Exception {
//        System.out.println("\n--- Testing 2D Relu ---");
//        int rows = 32;
//        int cols = 32;
//        float minValue = -50.0f;
//        float maxValue = 50.0f;
//
//        INDArray input = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue));
//        INDArray theoreticalExpected = Transforms.relu(input.dup()); // Use dup to avoid modifying the input
//
//        HWAcceleratedReluV13 operator = new HWAcceleratedReluV13();
//        INDArray actualOutput = operator.relu(input.dup());
//        double hardwareLogicTolerance = 1e-5;
//        HWAcceleratedTestModel.validateActivationFunction("ReLU - 2D", theoreticalExpected, actualOutput, hardwareLogicTolerance);
//    }
//
//    /**
//     * Tests 3D (batched) Relu operation with random floats.
//     */
//    @Test
//    public void testRelu3D() throws Exception {
//        System.out.println("\n--- Testing 3D (Batched) Relu ---");
//        int batchSize = 2;
//        int rows = 8192;
//        int cols = 65;
//        float minValue = -10.0f;
//        float maxValue = 10.0f;
//
//        INDArray input = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue);
//        INDArray theoreticalExpected = Transforms.relu(input.dup()); // Use dup to avoid modifying the input
//
//        HWAcceleratedReluV13 operator = new HWAcceleratedReluV13();
//        INDArray actualOutput = operator.relu(input.dup());
//        double hardwareLogicTolerance = 1e-5;
//        HWAcceleratedTestModel.validateActivationFunction("ReLU - 3D", theoreticalExpected, actualOutput, hardwareLogicTolerance);
//    }
//}

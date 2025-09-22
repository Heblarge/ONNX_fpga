package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test class validates the HWAcceleratedSoftplusV13 operator for
 * both 2D and 3D (batched) tensors using floating-point inputs.
 */
public class HWAcceleratedSoftplusV13Test extends HWAcceleratedTestCase {

    @Test
    public void testSoftplus2D() throws Exception {
        System.out.println("\n--- Testing 2D Softplus ---");
        int rows = 32;
        int cols = 32;
        float minValue = -10.0f;
        float maxValue = 16.0f;

        INDArray input = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue));
        INDArray theoreticalExpected = Transforms.log(Transforms.exp(input.dup()).add(1), true);

        HWAcceleratedSoftplusV13 operator = new HWAcceleratedSoftplusV13();
        INDArray actualOutput = operator.softplus(input.dup());
        double hardwareLogicTolerance = 1e-3;
        HWAcceleratedTestModel.validateActivationFunction("Softplus - 2D", theoreticalExpected, actualOutput, hardwareLogicTolerance);
    }

    /**
     * Tests 3D (batched) Softplus operation with random floats.
     */
    @Test
    public void testSoftplus3D() throws Exception {
        System.out.println("\n--- Testing 3D (Batched) Softplus ---");
        int batchSize = 2;
        int rows = 28;
        int cols = 511;
        float minValue = -10.0f;
        float maxValue = 10.0f;

        INDArray input = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue);
        INDArray theoreticalExpected = Transforms.log(Transforms.exp(input.dup()).add(1), true);

        HWAcceleratedSoftplusV13 operator = new HWAcceleratedSoftplusV13();
        INDArray actualOutput = operator.softplus(input.dup());
        double hardwareLogicTolerance = 1e-3;
        HWAcceleratedTestModel.validateActivationFunction("Softplus - 3D", theoreticalExpected, actualOutput, hardwareLogicTolerance);
    }
}

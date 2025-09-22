package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
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
 * This test class validates the HWAcceleratedExpV13 operator for
 * both 2D and 3D (batched) tensors.
 */
public class HWAcceleratedExpV13Test extends HWAcceleratedTestCase {

    @Test
    public void testExp2D() throws Exception {
        System.out.println("\n--- Testing 2D Exp ---");
        int rows = 32;
        int cols = 32;
        int minValue = -3;
        int maxValue = 3;

        INDArray input = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue));
        INDArray theoreticalExpected = Transforms.exp(input.dup());

        HWAcceleratedExpV13 operator = new HWAcceleratedExpV13();
        INDArray actualOutput = operator.exp(input.dup());
        double hardwareLogicTolerance = 1e-4;
        HWAcceleratedTestModel.validateActivationFunction("Exp - 2D", theoreticalExpected, actualOutput, hardwareLogicTolerance);
    }

    /**
     * Tests 3D (batched) Exp operation.
     */
    @Test
    public void testExp3D() throws Exception {
        System.out.println("\n--- Testing 3D (Batched) Exp ---");
        int batchSize = 1;
        int rows = 28;
        int cols = 512;
        int minValue = -3;
        int maxValue = 4;

        INDArray input = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue);
        INDArray theoreticalExpected = Transforms.exp(input.dup());

        HWAcceleratedExpV13 operator = new HWAcceleratedExpV13();
        INDArray actualOutput = operator.exp(input.dup());
        double hardwareLogicTolerance = 1e-4;
        HWAcceleratedTestModel.validateActivationFunction("Exp - 3D", theoreticalExpected, actualOutput, hardwareLogicTolerance);
    }
}

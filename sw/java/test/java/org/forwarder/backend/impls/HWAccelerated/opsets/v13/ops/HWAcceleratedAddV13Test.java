//package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;
//
//import Accelerator.AcceleratorSimInterface;
//import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
//import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
//import org.junit.Test;
//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.factory.Nd4j;
//
//import java.util.Random;
//
//import static org.junit.Assert.assertArrayEquals;
//import static org.junit.Assert.assertTrue;
//
///**
// * This test class validates the HWAcceleratedAddV13 operator.
// */
//public class HWAcceleratedAddV13Test extends HWAcceleratedTestCase {
//
//    private INDArray calculateSimulatedFixedPointAdd(INDArray a, INDArray b) {
//        int fracWidth = 9;
//        double scaleFactor = Math.pow(2, fracWidth);
//        int rows = (int) a.rows();
//        int cols = (int) a.columns();
//        int[][] fixedPointA = new int[rows][cols];
//        int[][] fixedPointB = new int[rows][cols];
//        for (int i = 0; i < rows; i++) {
//            for (int j = 0; j < cols; j++) {
//                fixedPointA[i][j] = (int)Math.round(a.getFloat(i, j) * scaleFactor);
//                fixedPointB[i][j] = (int)Math.round(b.getFloat(i, j) * scaleFactor);
//            }
//        }
//        int[][] fixedPointOutput = new int[rows][cols];
//        for (int i = 0; i < rows; i++) {
//            for (int j = 0; j < cols; j++) {
//                fixedPointOutput[i][j] = fixedPointA[i][j] + fixedPointB[i][j];
//            }
//        }
//        float[] output = new float[rows * cols];
//        for (int i = 0; i < rows; i++) {
//            for (int j = 0; j < cols; j++) {
//                output[i * cols + j] = (float)(((double)(fixedPointOutput[i][j])) / scaleFactor);
//            }
//        }
//        return Nd4j.create(output).reshape(rows, cols);
//    }
//
//    @Test
//    public void testAdd2D() throws Exception {
//        System.out.println("\n--- Testing 2D Add ---");
//        int rows = 32;
//        int cols = 32;
//        int minValue = -5;
//        int maxValue = 5;
//
//        INDArray matrixA = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue));
//        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue));
//        INDArray theoreticalExpected = matrixA.add(matrixB);
//        INDArray simulatedExpected = calculateSimulatedFixedPointAdd(matrixA, matrixB);
//        HWAcceleratedAddV13 operator = new HWAcceleratedAddV13();
//        INDArray actualOutput = operator.add(matrixA, matrixB);
//        double hardwareLogicTolerance = 1e-6;
//        HWAcceleratedTestModel.validate("Add - 2D",theoreticalExpected, simulatedExpected, actualOutput, hardwareLogicTolerance);
//    }
//
//    @Test
//    public void testAdd3D() throws Exception {
//        System.out.println("\n--- Testing 3D (Batched) Add ---");
//        int batchSize = 2;
//        int rows = 28;
//        int cols = 32;
//        int minValue = -5;
//        int maxValue = 5;
//
//        INDArray matrixA = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue);
//        INDArray matrixB = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue);
//        INDArray theoreticalExpected = matrixA.add(matrixB);
//        INDArray simulatedExpected = Nd4j.create(matrixA.shape());
//        for (int i = 0; i < batchSize; i++) {
//            INDArray sliceA = matrixA.slice(i);
//            INDArray sliceB = matrixB.slice(i);
//            INDArray expectedSlice = calculateSimulatedFixedPointAdd(sliceA, sliceB);
//            simulatedExpected.putSlice(i, expectedSlice);
//        }
//        HWAcceleratedAddV13 operator = new HWAcceleratedAddV13();
//        INDArray actualOutput = operator.add(matrixA, matrixB);
//        double hardwareLogicTolerance = 1e-6;
//        HWAcceleratedTestModel.validate("Add - 3D",theoreticalExpected, simulatedExpected, actualOutput, hardwareLogicTolerance);
//    }
//
//
//}
package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.Arrays;
import java.util.List;

public class HWAcceleratedMaxV13Test extends HWAcceleratedTestCase {

    private INDArray calculateSimulatedFixedPointMax(INDArray a, INDArray b) {
        if (!java.util.Arrays.equals(a.shape(), b.shape())) {
            long[] broadcastShape = getBroadcastShape(a.shape(), b.shape());
            a = a.broadcast(broadcastShape);
            b = b.broadcast(broadcastShape);
        }

        if (a.rank() > 2) {
            long[] finalShape = a.shape();
            long numCols = finalShape[finalShape.length - 1];
            long numRows = a.length() / numCols;
            INDArray reshapedA = a.reshape('c', numRows, numCols);
            INDArray reshapedB = b.reshape('c', numRows, numCols);
            INDArray result2D = calculateSimulatedFixedPointMax(reshapedA, reshapedB);
            return result2D.reshape('c', finalShape);
        }

        int rows = (int) a.rows();
        int cols = (int) a.columns();

        long[][] matA_long = new long[rows][cols];
        long[][] matB_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matA_long[i][j] = a.getLong(i, j);
                matB_long[i][j] = b.getLong(i, j);
            }
        }

        long[][] maxResult_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                maxResult_long[i][j] = Math.max(matA_long[i][j], matB_long[i][j]);
            }
        }

        float[] flatResult = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                flatResult[i * cols + j] = maxResult_long[i][j];
            }
        }
        return Nd4j.create(flatResult, new long[]{rows, cols});
    }

    private long[] getBroadcastShape(long[] shapeA, long[] shapeB) {
        int rankA = shapeA.length;
        int rankB = shapeB.length;
        int maxRank = Math.max(rankA, rankB);
        long[] resultShape = new long[maxRank];
        for (int i = 1; i <= maxRank; i++) {
            long dimA = (rankA - i >= 0) ? shapeA[rankA - i] : 1;
            long dimB = (rankB - i >= 0) ? shapeB[rankB - i] : 1;
            if (dimA != dimB && dimA != 1 && dimB != 1) throw new IllegalArgumentException("Shapes are not broadcastable.");
            resultShape[maxRank - i] = Math.max(dimA, dimB);
        }
        return resultShape;
    }

    @Test
    public void testMax2D() throws Exception {
        System.out.println("\n--- Testing 2D Max (Integer Domain) ---");
        int rows = 30;
        int cols = 32;
        float minValue = -10f;
        float maxValue = 10f;

        INDArray matrixA_int = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue, 10));
        INDArray matrixB_int = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue, 10));

        INDArray theoreticalExpected = Transforms.max(matrixA_int, matrixB_int);

        INDArray simulatedExpected = calculateSimulatedFixedPointMax(matrixA_int, matrixB_int);

        HWAcceleratedMaxV13 operator = new HWAcceleratedMaxV13();
        List<INDArray> inputs = Arrays.asList(matrixA_int, matrixB_int);
        INDArray actualOutput = operator.max(inputs, null, null); // Pass null for shifts

        // All three results should be identical
        HWAcceleratedTestModel.validate("Max - 2D", theoreticalExpected, simulatedExpected, actualOutput, 0.0);
    }

    @Test
    public void testMax3D() throws Exception {
        System.out.println("\n--- Testing 3D (Batched) Max (Integer Domain) ---");
        int batchSize = 2;
        int rows = 30;
        int cols = 32;
        float minValue = -10f;
        float maxValue = 10f;

        INDArray matrixA_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue, 20);
        INDArray matrixB_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue, 20);

        INDArray theoreticalExpected = Transforms.max(matrixA_int, matrixB_int);
        INDArray simulatedExpected = calculateSimulatedFixedPointMax(matrixA_int, matrixB_int);

        HWAcceleratedMaxV13 operator = new HWAcceleratedMaxV13();
        List<INDArray> inputs = Arrays.asList(matrixA_int, matrixB_int);
        INDArray actualOutput = operator.max(inputs, null, null);

        HWAcceleratedTestModel.validate("Max - 3D", theoreticalExpected, simulatedExpected, actualOutput, 0.0);
    }

    @Test
    public void testMaxBroadcast_3D_with_1D_Scalar() throws Exception {
        System.out.println("\n--- Testing Max Broadcast: (1,32,512) + [1] ---");
        float minValue = -3f;
        float maxValue = -5f;

        INDArray matrixA_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(1, 32, 32, minValue, maxValue,20);
        INDArray matrixB_int = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(1, 1, minValue, maxValue, 20)).reshape(1);

        INDArray theoreticalExpected = Transforms.max(matrixA_int, matrixB_int);
        INDArray simulatedExpected = calculateSimulatedFixedPointMax(matrixA_int, matrixB_int);

        HWAcceleratedMaxV13 operator = new HWAcceleratedMaxV13();
        List<INDArray> inputs = Arrays.asList(matrixA_int, matrixB_int);
        INDArray actualOutput = operator.max(inputs, null, null);

        HWAcceleratedTestModel.validate("Max - Broadcast [1]", theoreticalExpected, simulatedExpected, actualOutput, 0.0);
    }
}
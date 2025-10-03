package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Test;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;
import java.util.List;


public class HWAcceleratedAddV13Test extends HWAcceleratedTestCase {

    private INDArray calculateSimulatedFixedPointAdd(
            INDArray a, INDArray b, List<Long> fpgaInShift, Long fpgaOutShift
    ) {
        if (!fpgaInShift.get(0).equals(fpgaInShift.get(1))) {
            throw new IllegalArgumentException("Input shifts must be identical for simulated add.");
        }

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
            INDArray result2D = calculateSimulatedFixedPointAdd(reshapedA, reshapedB, fpgaInShift, fpgaOutShift);
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

        long[][] addResult_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                addResult_long[i][j] = matA_long[i][j] + matB_long[i][j];
            }
        }

        long shiftAmount = fpgaInShift.get(0) - fpgaOutShift;

        long[][] shiftedResult_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                shiftedResult_long[i][j] = (shiftAmount >= 0) ? (addResult_long[i][j] >> shiftAmount) : (addResult_long[i][j] << -shiftAmount);
            }
        }

        float[] flatResult = new float[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                flatResult[i * cols + j] = shiftedResult_long[i][j];
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
    public void testAdd2D() throws Exception {
        System.out.println("\n--- Testing 2D Add with Quantization Params ---");
        int rows = 32;
        int cols = 32;
        float minValue = -10f;
        float maxValue = 10f;

        List<Long> fpgaInShift = Arrays.asList(25L, 25L);
        Long fpgaOutShift = 30L;

        INDArray matrixA = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue, fpgaInShift.get(0)));
        INDArray matrixB = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue, fpgaInShift.get(1)));

        long shiftAmount = fpgaInShift.get(0) - fpgaOutShift;
        INDArray integerSum = matrixA.add(matrixB);
        INDArray theoreticalExpected = (shiftAmount >= 0)
                ? integerSum.div(1L << shiftAmount)
                : integerSum.mul(1L << -shiftAmount);
        INDArray simulatedExpected = calculateSimulatedFixedPointAdd(matrixA, matrixB, fpgaInShift, fpgaOutShift);
        HWAcceleratedAddV13 operator = new HWAcceleratedAddV13();
        INDArray actualOutput = operator.add(matrixA, matrixB, fpgaInShift, fpgaOutShift);

        HWAcceleratedTestModel.validate("Add - 2D Quantized", theoreticalExpected, simulatedExpected, actualOutput, 0.0);
    }

    @Test
    public void testAdd3D() throws Exception {
        System.out.println("\n--- Testing 3D (Batched) Add with Quantization Params ---");
        int batchSize = 2;
        int rows = 28;
        int cols = 32;
        float minValue = -10f;
        float maxValue = 10f;

        List<Long> fpgaInShift = Arrays.asList(14L, 14L);
        Long fpgaOutShift = 12L;

        INDArray matrixA = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue, fpgaInShift.get(0));
        INDArray matrixB = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue, fpgaInShift.get(1));

        long shiftAmount = fpgaInShift.get(0) - fpgaOutShift;
        INDArray integerSum = matrixA.add(matrixB);
        INDArray theoreticalExpected = (shiftAmount >= 0)
                ? integerSum.div(1L << shiftAmount)
                : integerSum.mul(1L << -shiftAmount);
        INDArray simulatedExpected = calculateSimulatedFixedPointAdd(matrixA, matrixB, fpgaInShift, fpgaOutShift);
        HWAcceleratedAddV13 operator = new HWAcceleratedAddV13();
        INDArray actualOutput = operator.add(matrixA, matrixB, fpgaInShift, fpgaOutShift);

        HWAcceleratedTestModel.validate("Add - 3D Quantized", theoreticalExpected, simulatedExpected, actualOutput, 0.0);
    }

    @Test
    public void testAddBroadcast() throws Exception {
        System.out.println("\n--- Testing Add Broadcast: (1,32,512) + (1,1) ---");
        float minValue = -5f;
        float maxValue = 5f;

        List<Long> fpgaInShift = Arrays.asList(15L, 15L);
        Long fpgaOutShift = 25L;

        INDArray matrixA_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(1, 32, 32, minValue, maxValue, fpgaInShift.get(0));
        INDArray matrixB_int = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(1, 1, minValue, maxValue, fpgaInShift.get(1)));

        long shiftAmount = fpgaInShift.get(0) - fpgaOutShift;
        INDArray integerSum = matrixA_int.add(matrixB_int);
        INDArray theoreticalExpected = (shiftAmount >= 0)
                ? integerSum.div(1L << shiftAmount)
                : integerSum.mul(1L << -shiftAmount);

        INDArray simulatedExpected = calculateSimulatedFixedPointAdd(matrixA_int, matrixB_int, fpgaInShift, fpgaOutShift);

        HWAcceleratedAddV13 operator = new HWAcceleratedAddV13();
        INDArray actualOutput = operator.add(matrixA_int, matrixB_int, fpgaInShift, fpgaOutShift);

        HWAcceleratedTestModel.validate("Add - Broadcast (1,1)", theoreticalExpected, simulatedExpected, actualOutput, 0.0);
    }

    @Test
    public void testAddBroadcast1D_Scalar() throws Exception {
        System.out.println("\n--- Testing Add Broadcast: (1,32,512) + [1] ---");
        float minValue = -5f;
        float maxValue = 5f;

        List<Long> fpgaInShift = Arrays.asList(18L, 18L);
        Long fpgaOutShift = 20L;

        INDArray matrixA_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(1, 32, 512, minValue, maxValue, fpgaInShift.get(0));
        INDArray matrixB_int = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(1, 1, minValue, maxValue, fpgaInShift.get(1))).reshape(1);

        long shiftAmount = fpgaInShift.get(0) - fpgaOutShift;
        INDArray integerSum = matrixA_int.add(matrixB_int);
        INDArray theoreticalExpected = (shiftAmount >= 0)
                ? integerSum.div(1L << shiftAmount)
                : integerSum.mul(1L << -shiftAmount);

        INDArray simulatedExpected = calculateSimulatedFixedPointAdd(matrixA_int, matrixB_int, fpgaInShift, fpgaOutShift);

        HWAcceleratedAddV13 operator = new HWAcceleratedAddV13();
        INDArray actualOutput = operator.add(matrixA_int, matrixB_int, fpgaInShift, fpgaOutShift);

        double tolerance = 0.0;
        HWAcceleratedTestModel.validate("Add - Broadcast [1]", theoreticalExpected, simulatedExpected, actualOutput, tolerance);
    }
}
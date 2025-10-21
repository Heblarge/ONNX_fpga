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
            INDArray a, INDArray b,
            long sourceShiftA, long sourceShiftB,
            long targetInputShiftA, long targetInputShiftB,
            long targetOutputShift
    ) {
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
            INDArray result2D = calculateSimulatedFixedPointAdd(
                    reshapedA, reshapedB,
                    sourceShiftA, sourceShiftB,
                    targetInputShiftA, targetInputShiftB,
                    targetOutputShift
            );
            return result2D.reshape('c', finalShape);
        }

        int rows = (int) a.rows();
        int cols = (int) a.columns();

        if (targetInputShiftA != targetInputShiftB) {
            throw new IllegalArgumentException("Simulated Add requires targetInputShifts to be equal.");
        }

        int rescaleShiftA = (int) (sourceShiftA - targetInputShiftA);
        int rescaleShiftB = (int) (sourceShiftB - targetInputShiftB);

        int finalShiftAmount = (int) (targetInputShiftA - targetOutputShift);

        long[][] fixedPointA = new long[rows][cols];
        long[][] fixedPointB = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long valA = a.getLong(i, j);
                fixedPointA[i][j] = (rescaleShiftA < 0) ? (valA << -rescaleShiftA) : (valA >> rescaleShiftA);
                long valB = b.getLong(i, j);
                fixedPointB[i][j] = (rescaleShiftB < 0) ? (valB << -rescaleShiftB) : (valB >> rescaleShiftB);
            }
        }

        long[][] addResult_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                addResult_long[i][j] = fixedPointA[i][j] + fixedPointB[i][j];
            }
        }

        long[][] shiftedResult_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long val = addResult_long[i][j];
                shiftedResult_long[i][j] = (finalShiftAmount < 0) ? (val << -finalShiftAmount) : (val >> finalShiftAmount);
            }
        }

        long[] flatResultLong = new long[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                flatResultLong[i * cols + j] = shiftedResult_long[i][j];
            }
        }

        return Nd4j.create(flatResultLong, new long[]{rows, cols}, a.dataType());
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
        float minValue = -10;
        float maxValue = 10;

        long sourceShiftA = 15L;
        long sourceShiftB = 19L;
        long targetInputShiftA = 18L;
        long targetInputShiftB = 18L;
        long targetOutputShift = 20L;

        int[][] matrixDataA = HWAcceleratedTestModel.generateRandom2DIntMatrix(rows, cols, minValue, maxValue, sourceShiftA);
        int[][] matrixDataB = HWAcceleratedTestModel.generateRandom2DIntMatrix(rows, cols, minValue, maxValue, sourceShiftB);

        INDArray matrixA = Nd4j.createFromArray(matrixDataA);
        INDArray matrixB = Nd4j.createFromArray(matrixDataB);
        long theoreticalShiftAmount = targetInputShiftA - targetOutputShift;
        INDArray integerSum = matrixA.add(matrixB);
        INDArray theoreticalExpected = (theoreticalShiftAmount >= 0)
                ? integerSum.div(1L << theoreticalShiftAmount)
                : integerSum.mul(1L << -theoreticalShiftAmount);

        INDArray simulatedExpected = calculateSimulatedFixedPointAdd(
                matrixA, matrixB,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift
        );

        HWAcceleratedAddV13 operator = new HWAcceleratedAddV13();

        INDArray actualOutput = operator.add(
                matrixA, matrixB,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift
        );

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

        long sourceShiftA = 14L;
        long sourceShiftB = 14L;
        long targetInputShiftA = 14L;
        long targetInputShiftB = 14L;
        long targetOutputShift = 12L;

        INDArray matrixA = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue, sourceShiftA);
        INDArray matrixB = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue, sourceShiftB);

        long theoreticalShiftAmount = targetInputShiftA - targetOutputShift;
        INDArray integerSum = matrixA.add(matrixB);
        INDArray theoreticalExpected = (theoreticalShiftAmount >= 0)
                ? integerSum.div(1L << theoreticalShiftAmount)
                : integerSum.mul(1L << -theoreticalShiftAmount);

        INDArray simulatedExpected = calculateSimulatedFixedPointAdd(
                matrixA, matrixB,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift
        );
        HWAcceleratedAddV13 operator = new HWAcceleratedAddV13();

        INDArray actualOutput = operator.add(
                matrixA, matrixB,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift
        );

        HWAcceleratedTestModel.validate("Add - 3D Quantized", theoreticalExpected, simulatedExpected, actualOutput, 0.0);
    }

    @Test
    public void testAddBroadcast() throws Exception {
        System.out.println("\n--- Testing Add Broadcast: (1,32,32) + (1,1) ---");
        float minValue = -5f;
        float maxValue = 5f;

        long sourceShiftA = 15L;
        long sourceShiftB = 15L;
        long targetInputShiftA = 15L;
        long targetInputShiftB = 15L;
        long targetOutputShift = 25L;

        INDArray matrixA_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(1, 32, 32, minValue, maxValue, sourceShiftA);
        INDArray matrixB_int = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(1, 1, minValue, maxValue, sourceShiftB));

        long theoreticalShiftAmount = targetInputShiftA - targetOutputShift;
        INDArray integerSum = matrixA_int.add(matrixB_int);
        INDArray theoreticalExpected = (theoreticalShiftAmount >= 0)
                ? integerSum.div(1L << theoreticalShiftAmount)
                : integerSum.mul(1L << -theoreticalShiftAmount);

        INDArray simulatedExpected = calculateSimulatedFixedPointAdd(
                matrixA_int, matrixB_int,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift
        );

        HWAcceleratedAddV13 operator = new HWAcceleratedAddV13();

        INDArray actualOutput = operator.add(
                matrixA_int, matrixB_int,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift);

        HWAcceleratedTestModel.validate("Add - Broadcast (1,1)", theoreticalExpected, simulatedExpected, actualOutput, 0.0);
    }

    @Test
    public void testAddBroadcast1D_Scalar() throws Exception {
        System.out.println("\n--- Testing Add Broadcast: (1,32,512) + [1] ---");
        float minValue = -5f;
        float maxValue = 5f;

        long sourceShiftA = 18L;
        long sourceShiftB = 18L;
        long targetInputShiftA = 18L;
        long targetInputShiftB = 18L;
        long targetOutputShift = 25L;

        INDArray matrixA_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(1, 32, 512, minValue, maxValue, sourceShiftA);
        INDArray matrixB_int = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(1, 1, minValue, maxValue, sourceShiftB)).reshape(1);

        long theoreticalShiftAmount = targetInputShiftA - targetOutputShift;
        INDArray integerSum = matrixA_int.add(matrixB_int);
        INDArray theoreticalExpected = (theoreticalShiftAmount >= 0)
                ? integerSum.div(1L << theoreticalShiftAmount)
                : integerSum.mul(1L << -theoreticalShiftAmount);

        INDArray simulatedExpected = calculateSimulatedFixedPointAdd(
                matrixA_int, matrixB_int,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift
        );

        HWAcceleratedAddV13 operator = new HWAcceleratedAddV13();

        INDArray actualOutput = operator.add(
                matrixA_int, matrixB_int,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift
        );

        double tolerance = 0.0;
        HWAcceleratedTestModel.validate("Add - Broadcast [1]", theoreticalExpected, simulatedExpected, actualOutput, tolerance);
    }
}
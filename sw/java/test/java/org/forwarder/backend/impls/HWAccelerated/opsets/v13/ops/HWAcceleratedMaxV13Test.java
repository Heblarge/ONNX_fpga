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

    private INDArray calculateSimulatedFixedPointMax(
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
            INDArray result2D = calculateSimulatedFixedPointMax(
                    reshapedA, reshapedB,
                    sourceShiftA, sourceShiftB,
                    targetInputShiftA, targetInputShiftB,
                    targetOutputShift
            );
            return result2D.reshape('c', finalShape);
        }

        int rows = (int) a.rows();
        int cols = (int) a.columns();

        long comparisonShift = Math.max(targetInputShiftA, targetInputShiftB);
        int preRescaleShiftA = (int) (sourceShiftA - comparisonShift);
        int preRescaleShiftB = (int) (sourceShiftB - comparisonShift);
        int hardwareShiftAmount = (int) (comparisonShift - targetOutputShift);

        long[][] fixedPointA = new long[rows][cols];
        long[][] fixedPointB = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long valA = a.getLong(i, j);
                fixedPointA[i][j] = (preRescaleShiftA < 0) ? (valA << -preRescaleShiftA) : (valA >> preRescaleShiftA);
                long valB = b.getLong(i, j);
                fixedPointB[i][j] = (preRescaleShiftB < 0) ? (valB << -preRescaleShiftB) : (valB >> preRescaleShiftB);
            }
        }

        long[][] maxResult_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                maxResult_long[i][j] = Math.max(fixedPointA[i][j], fixedPointB[i][j]);
            }
        }

        long[][] shiftedResult_long = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long val = maxResult_long[i][j];
                shiftedResult_long[i][j] = (hardwareShiftAmount < 0) ? (val << -hardwareShiftAmount) : (val >> hardwareShiftAmount);
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
    public void testMax2D() throws Exception {
        System.out.println("\n--- Testing 2D Max with Quantization Params ---");
        int rows = 30;
        int cols = 32;
        float minValue = -10f;
        float maxValue = 10f;

        long sourceShiftA = 11L;
        long sourceShiftB = 11L;
        long targetInputShiftA = 10L;
        long targetInputShiftB = 9L;
        long targetOutputShift = 8L;

        INDArray matrixA_int = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue, sourceShiftA));
        INDArray matrixB_int = Nd4j.create(HWAcceleratedTestModel.generateRandom2DFloatMatrix(rows, cols, minValue, maxValue, sourceShiftB));

        INDArray simulatedExpected = calculateSimulatedFixedPointMax(
                matrixA_int, matrixB_int,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift
        );

        HWAcceleratedMaxV13 operator = new HWAcceleratedMaxV13();

        List<INDArray> inputsList = Arrays.asList(matrixA_int, matrixB_int); // 创建 List
        INDArray actualOutput = operator.max(
                inputsList, // 传递 List
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift,
                "Max2D"
        );

        HWAcceleratedTestModel.validate("Max - 2D Quantized", simulatedExpected, simulatedExpected, actualOutput, 0.0);
    }

    @Test
    public void testMax3D() throws Exception {
        System.out.println("\n--- Testing 3D (Batched) Max with Quantization Params ---");
        int batchSize = 2;
        int rows = 30;
        int cols = 32;
        float minValue = -10f;
        float maxValue = 10f;

        long sourceShiftA = 22L;
        long sourceShiftB = 22L;
        long targetInputShiftA = 20L;
        long targetInputShiftB = 21L;
        long targetOutputShift = 18L;

        INDArray matrixA_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue, sourceShiftA);
        INDArray matrixB_int = HWAcceleratedTestModel.generateRandom3DFloatMatrix(batchSize, rows, cols, minValue, maxValue, sourceShiftB);

        INDArray simulatedExpected = calculateSimulatedFixedPointMax(
                matrixA_int, matrixB_int,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift
        );

        HWAcceleratedMaxV13 operator = new HWAcceleratedMaxV13();

        List<INDArray> inputsList = Arrays.asList(matrixA_int, matrixB_int);
        INDArray actualOutput = operator.max(
                inputsList, // 传递 List
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift,
                "Max3D"
        );

        HWAcceleratedTestModel.validate("Max - 3D Quantized", simulatedExpected, simulatedExpected, actualOutput, 0.0);
    }

}
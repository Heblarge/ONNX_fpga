package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedSession;
import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTestModel;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class HWAcceleratedConcatV13Test extends HWAcceleratedTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private INDArray calculateSimulatedFixedPointConcat(
            List<INDArray> inputs,
            Long axisLong,
            long sourceShiftA, long sourceShiftB,
            long targetOutputShift
    ) {

        INDArray matrixA = inputs.get(0);
        INDArray matrixB = inputs.get(1);

        int axis = normalizeAxis(axisLong, matrixA.rank());

        long[] shapeA = matrixA.shape();
        long[] shapeB = matrixB.shape();

        long commonShift = targetOutputShift; // 直接使用 targetOutputShift
        int rescaleAmountA = (int) (sourceShiftA - commonShift);
        INDArray rescaledA = (rescaleAmountA == 0)
                ? matrixA
                : (rescaleAmountA < 0
                ? matrixA.dup().mul(1L << -rescaleAmountA)
                : matrixA.dup().div(1L << rescaleAmountA)
        );
        rescaledA = rescaledA.castTo(matrixA.dataType());

        int rescaleAmountB = (int) (sourceShiftB - commonShift);
        INDArray rescaledB = (rescaleAmountB == 0)
                ? matrixB
                : (rescaleAmountB < 0
                ? matrixB.dup().mul(1L << -rescaleAmountB)
                : matrixB.dup().div(1L << rescaleAmountB)
        );
        rescaledB = rescaledB.castTo(matrixB.dataType());

        return Nd4j.concat(axis, rescaledA, rescaledB);
    }

    private int normalizeAxis(long axis, int rank) {
        if (axis < 0) {
            axis += rank;
        }
        if (axis < 0 || axis >= rank) {
            throw new IllegalArgumentException(String.format(
                    "axis=%d illegal，dimension of the input tensor should be %d", axis, rank));
        }
        return (int) axis;
    }


    @Test
    public void testConcatWithShifts() throws Exception {
        System.out.println("\n--- Testing Concat with Quantization Shifts ---");
        int rows = 32;
        int colsA = 20;
        int colsB = 10;
        long axisRaw = 1;
        float minValue = -5f;
        float maxValue = 5f;

        long sourceShiftA = 16L;
        long sourceShiftB = 16L;
        long commonShift = 16L;

        INDArray matrixA = Nd4j.createFromArray(HWAcceleratedTestModel.generateRandom2DIntMatrix(rows, colsA, minValue, maxValue, (int)sourceShiftA));
        INDArray matrixB = Nd4j.createFromArray(HWAcceleratedTestModel.generateRandom2DIntMatrix(rows, colsB, minValue, maxValue, (int)sourceShiftB));

        INDArray simulatedExpected = calculateSimulatedFixedPointConcat(
                Arrays.asList(matrixA, matrixB),
                axisRaw,
                sourceShiftA, sourceShiftB, commonShift
        );

        INDArray expectedShapeOnly = Nd4j.create(matrixA.dataType(), rows, colsA + colsB);

        this.testConcat(expectedShapeOnly, matrixA, matrixB, axisRaw, sourceShiftA, sourceShiftB, commonShift);

        HWAcceleratedConcatV13 operatorDirect = new HWAcceleratedConcatV13();
        List<INDArray> inputs = Arrays.asList(matrixA, matrixB);
        INDArray actualOutput= operatorDirect.concat(inputs, axisRaw, sourceShiftA, sourceShiftB, commonShift);
        HWAcceleratedTestModel.validate("Concat - 2D Quantized Direct Compare", simulatedExpected, simulatedExpected, actualOutput, 0.0);

    }

    @Test
    public void testConcatWithScalarBroadcast() throws Exception {
        System.out.println("\n--- Testing Concat with Scalar Broadcast and Shifts ---");
        long[] shapeA = {1, 32, 1024};
        long[] shapeB_scalar = {1};
        long[] shapeB_broadcast = {1, 32, 1};
        long axisRaw = -1L;
        int minValue = -5;
        int maxValue = 5;

        long sourceShiftA = 18L;
        long sourceShiftB = 19L;
        long commonShift = 19L;

        INDArray matrixA = HWAcceleratedTestModel.generateRandom3DIntMatrix((int)shapeA[0], (int)shapeA[1], (int)shapeA[2], minValue, maxValue, (int)sourceShiftA);

        INDArray matrixB_scalar = Nd4j.scalar(524288L);
        INDArray matrixB_broadcasted = matrixB_scalar.broadcast(shapeB_broadcast);

        INDArray simulatedExpected = calculateSimulatedFixedPointConcat(
                Arrays.asList(matrixA, matrixB_broadcasted),
                axisRaw,
                sourceShiftA, sourceShiftB, commonShift
        );

        long[] expectedOutputShape = {1, 32, 1025};

        this.testConcat(Nd4j.create(matrixA.dataType(), expectedOutputShape),
                matrixA, matrixB_broadcasted,
                axisRaw, sourceShiftA, sourceShiftB, commonShift);

        HWAcceleratedConcatV13 operatorDirect = new HWAcceleratedConcatV13();
        List<INDArray> inputsDirect = Arrays.asList(matrixA, matrixB_broadcasted);
        INDArray actualOutputDirect = operatorDirect.concat(inputsDirect, axisRaw, sourceShiftA, sourceShiftB, commonShift);
        HWAcceleratedTestModel.validate("Concat - Scalar Broadcast Quantized Direct Compare", simulatedExpected, simulatedExpected, actualOutputDirect, 0.0);
    }

    @Test
    public void testConcatSpecificValuesWithShift() throws Exception {
        System.out.println("\n--- Testing Concat Specific Values with Quantization Shifts ---");

        long[] shapeA = {3, 3, 3};
        long[] shapeB = {3, 5, 3};
        long axisRaw = 1L;

        long valA = 256L;
        long valB = 1L;

        long sourceShiftA = 6L;
        long sourceShiftB = 4L;
        long commonShift = 5L;

        INDArray matrixA = Nd4j.valueArrayOf(shapeA, valA);
        INDArray matrixB = Nd4j.valueArrayOf(shapeB, valB);

        INDArray simulatedExpected = calculateSimulatedFixedPointConcat(
                Arrays.asList(matrixA, matrixB),
                axisRaw,
                sourceShiftA, sourceShiftB, commonShift
        );
        HWAcceleratedConcatV13 operator = new HWAcceleratedConcatV13();
        List<INDArray> inputs = Arrays.asList(matrixA, matrixB);
        INDArray actualOutput = operator.concat(inputs, axisRaw, sourceShiftA, sourceShiftB, commonShift);

        long[] expectedShape = {3, 8, 3}; // 3 + 5 = 8 along axis 1
        assertArrayEquals("Output shape mismatch", expectedShape, actualOutput.shape());

        HWAcceleratedTestModel.validate("Concat - Specific Values Quantized", simulatedExpected, simulatedExpected, actualOutput, 0.0);
    }


    private void testConcat(INDArray expected, INDArray a, INDArray b, long axisRaw,
                            long sourceShiftA, long sourceShiftB, long commonShift) throws Exception {
        try (HWAcceleratedSession session = new HWAcceleratedSession(null)) {
            HWAcceleratedConcatV13 operator = new HWAcceleratedConcatV13();
            List<INDArray> inputs = Arrays.asList(a, b);
            INDArray y = operator.concat(inputs, axisRaw, sourceShiftA, sourceShiftB, commonShift);

            if (expected != null) {
                System.out.println(String.format("{Expected Shape: %s} - {Actual Shape: %s}",
                        Arrays.toString(expected.shape()), Arrays.toString(y.shape())));
                assertArrayEquals(expected.shape(), y.shape());
            }
        }
    }



}


package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class HWAcceleratedTileV13Test extends HWAcceleratedTestCase {

    private void testTile(INDArray expected, INDArray input, INDArray repeats) throws Exception {
        HWAcceleratedTileV13 op = new HWAcceleratedTileV13();
        INDArray result = op.tile(input, repeats);

        System.out.printf("Expected: %s, Actual: %s%n", expected.shapeInfoToString(), result.shapeInfoToString());
        assertArrayEquals(expected.shape(), result.shape());
        assertEquals(expected, result);
    }

    @Test
    public void testScalarInput() throws Exception {
        INDArray input = Nd4j.scalar(5.0);
        INDArray repeats = Nd4j.createFromArray(2L);
        INDArray expected = Nd4j.create(new double[]{5.0, 5.0});

        testTile(expected, input, repeats);
    }

    @Test
    public void testVectorInput() throws Exception {
        INDArray input = Nd4j.create(new double[]{1.0, 2.0, 3.0});
        INDArray repeats = Nd4j.createFromArray(2L);
        INDArray expected = Nd4j.create(new double[]{1.0, 2.0, 3.0, 1.0, 2.0, 3.0});

        testTile(expected, input, repeats);
    }

    @Test
    public void testMatrixInput() throws Exception {
        INDArray input = Nd4j.create(new double[][]{{1.0, 2.0}, {3.0, 4.0}});
        INDArray repeats = Nd4j.createFromArray(2L, 2L);
        INDArray expected = Nd4j.create(new double[][]{
                {1.0, 2.0, 1.0, 2.0},
                {3.0, 4.0, 3.0, 4.0},
                {1.0, 2.0, 1.0, 2.0},
                {3.0, 4.0, 3.0, 4.0}
        });

        testTile(expected, input, repeats);
    }

    @Test
    public void test3DInput() throws Exception {
        INDArray input = Nd4j.create(new double[][][]{{{1.0, 2.0}, {3.0, 4.0}}});
        INDArray repeats = Nd4j.createFromArray(2L, 1L, 2L);
        INDArray expected = Nd4j.create(new double[][][]{
                {{1.0, 2.0, 1.0, 2.0}, {3.0, 4.0, 3.0, 4.0}},
                {{1.0, 2.0, 1.0, 2.0}, {3.0, 4.0, 3.0, 4.0}}
        });

        testTile(expected, input, repeats);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRankMismatch() throws Exception {
        INDArray input = Nd4j.create(new double[]{1.0, 2.0});
        INDArray repeats = Nd4j.createFromArray(2L, 2L);

        HWAcceleratedTileV13 op = new HWAcceleratedTileV13();
        op.tile(input, repeats);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeRepeats() throws Exception {
        INDArray input = Nd4j.create(new double[]{1.0, 2.0});
        INDArray repeats = Nd4j.createFromArray(-2L);

        HWAcceleratedTileV13 op = new HWAcceleratedTileV13();
        op.tile(input, repeats);
    }

    @Test
    public void testTileWithOneRepeat() throws Exception {
        INDArray input = Nd4j.create(new double[]{1.0, 2.0, 3.0});
        INDArray repeats = Nd4j.createFromArray(1L);
        INDArray expected = Nd4j.create(new double[]{1.0, 2.0, 3.0});

        testTile(expected, input, repeats);
    }

    @Test
    public void testEmptyInput() throws Exception {
        INDArray input = Nd4j.create(new double[]{});
        INDArray repeats = Nd4j.createFromArray(2L);
        INDArray expected = Nd4j.create(new double[]{});

        testTile(expected, input, repeats);
    }
}
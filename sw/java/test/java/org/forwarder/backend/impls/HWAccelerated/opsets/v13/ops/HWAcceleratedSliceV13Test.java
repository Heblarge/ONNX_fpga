package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.junit.Assert.assertArrayEquals;

public class HWAcceleratedSliceV13Test extends HWAcceleratedTestCase {

    private void testSlice(INDArray expected, INDArray data, INDArray starts, INDArray ends,
                           INDArray axes, INDArray steps) throws Exception {
        HWAcceleratedSliceV13 op = new HWAcceleratedSliceV13();
        INDArray result = op.slice(data,
                longList(starts), longList(ends),
                axes != null ? longList(axes) : op.defaultAxes((int) starts.length()),
                steps != null ? longList(steps) : op.defaultSteps((int) starts.length()));

        System.out.printf("Expected: %s, Actual: %s%n", expected.shapeInfoToString(), result.shapeInfoToString());
        assertArrayEquals(expected.shape(), result.shape());
    }

    private INDArray dataMatrix() {
        return Nd4j.create(new double[][]{
                {1, 2, 3, 4},
                {5, 6, 7, 8},
                {9, 10, 11, 12}
        });
    }

    private INDArray tensor(long... vals) {
        return Nd4j.createFromArray(vals);
    }

    private java.util.List<Long> longList(INDArray arr) {
        return java.util.Arrays.stream(arr.toLongVector()).boxed().toList();
    }

    @Test
    public void test1() throws Exception {
        INDArray expected = Nd4j.create(new double[][]{
                {2, 3, 4},
                {6, 7, 8}
        });
        testSlice(expected, dataMatrix(), tensor(0, 1), tensor(2, 4), tensor(0, 1), null);
    }

    @Test
    public void test2() throws Exception {
        INDArray expected = Nd4j.create(new double[][]{
                {1, 2},
                {5, 6},
                {9, 10}
        });
        testSlice(expected, dataMatrix(), tensor(0, 0), tensor(3, 2), null, null);
    }

    @Test
    public void test3() throws Exception {
        INDArray expected = Nd4j.create(new double[][]{
                {12, 11, 10},
                {8, 7 , 6}
        });
        testSlice(expected, dataMatrix(), tensor(2, 3), tensor(0, 0), tensor(0, 1), tensor(-1, -1));
        // 注意！ 目前不支持 Step 0，不支持负 Step 时 start > end，上述两种情况应当返回空张量
    }

    @Test
    public void test4() throws Exception {
        INDArray expected = Nd4j.create(new double[][]{
                { 6, 7, 8},
                {10,11,12}
        });
        testSlice(expected, dataMatrix(), tensor(-2, 1), tensor(3, 4), tensor(0, 1), tensor(1, 1));
    }

    @Test
    public void test5() throws Exception {
        INDArray expected = Nd4j.create(new double[][]{
                {1, 2, 3, 4},
                {9,10,11,12}
        });
        testSlice(expected, dataMatrix(), tensor(0), tensor(3), tensor(0), tensor(2));
    }
}

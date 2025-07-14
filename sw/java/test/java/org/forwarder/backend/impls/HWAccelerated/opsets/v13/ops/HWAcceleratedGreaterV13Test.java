package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.junit.Assert.assertTrue;

public class HWAcceleratedGreaterV13Test {

    //测试同形状张量逐元素 > 比较
    @Test
    public void testGreaterWithSameShape() throws Exception {
        INDArray a = Nd4j.create(new float[] {1f, 3f, 5f});
        INDArray b = Nd4j.create(new float[] {2f, 3f, 4f});
        INDArray expected = Nd4j.create(new boolean[] {false, false, true});

        testGreater(expected, a, b);
    }
    //2D
    @Test
    public void testGreaterWithBroadcast() throws Exception {
        INDArray a = Nd4j.create(new float[][] {
                {1f, 4f, 5f},
                {6f, 7f, 3f}
        });

        INDArray b = Nd4j.create(new float[] {3f, 4f, 2f});  // broadcast to (2,3)
        INDArray expected = Nd4j.create(new boolean[][] {
                {false, false, true},
                {true, true, true}
        });

        testGreater(expected, a, b);
    }

    protected void testGreater(INDArray expected, INDArray a, INDArray b) throws Exception {
        HWAcceleratedGreaterV13 op = new HWAcceleratedGreaterV13();
        INDArray actual = op.greater(a, b);

        System.out.println(String.format("Expected:\n%s\nActual:\n%s", expected, actual));
        assertTrue("Mismatch in comparison result", actual.equals(expected));
    }
}

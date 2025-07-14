package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class HWAcceleratedTransposeV13Test {

    private final HWAcceleratedTransposeV13 operator = new HWAcceleratedTransposeV13();

    // helper: 将 long[] 转为 List<Long>
    private List<Long> longArrayToList(long[] arr) {
        List<Long> list = new ArrayList<>(arr.length);
        for (long v : arr) list.add(v);
        return list;
    }



    @Test
    public void testTranspose_EmptyPerm_DefaultReverse() {
        // perm==[] -> 也默认逆序
        INDArray input = Nd4j.arange(30).reshape(2, 3, 5);
        INDArray expect = input.permute(2, 1, 0);
        INDArray output = operator.transpose(input, Collections.emptyList());
        assertArrayEquals(expect.shape(), output.shape());
        assertTrue(expect.equalsWithEps(output, 1e-6));
    }

    @Test
    public void testTranspose_SpecifiedPerm() {
        // perm=[1,0,2]
        INDArray input = Nd4j.arange(24).reshape(2, 3, 4);
        INDArray expect = input.permute(1, 0, 2);
        List<Long> perm = longArrayToList(new long[]{1, 0, 2});
        INDArray output = operator.transpose(input, perm);
        assertArrayEquals(expect.shape(), output.shape());
        assertTrue(expect.equalsWithEps(output, 1e-6));
    }

    @Test
    public void testTranspose_PermWithNegAxis() {
        // perm=(0, -1, 1) 应等价于 perm=(0,2,1)
        INDArray input = Nd4j.arange(60).reshape(3, 4, 5);
        INDArray expect = input.permute(0, 2, 1);
        List<Long> perm = longArrayToList(new long[]{0, -1, 1});
        INDArray output = operator.transpose(input, perm);
        assertArrayEquals(expect.shape(), output.shape());
        assertTrue(expect.equalsWithEps(output, 1e-6));
    }



    @Test
    public void testTranspose_IdentityPerm() {
        // perm=[0,1,2,3]，效果等价不变
        INDArray input = Nd4j.arange(24).reshape(2, 3, 2, 2);
        INDArray expect = input.dup();
        List<Long> perm = longArrayToList(new long[]{0, 1, 2, 3});
        INDArray out = operator.transpose(input, perm);
        assertArrayEquals(expect.shape(), out.shape());
        assertTrue(expect.equalsWithEps(out, 1e-6));
    }



    @Test(expected = IllegalArgumentException.class)
    public void testTranspose_PermWrongLengthThrows() {
        INDArray input = Nd4j.create(new float[6]).reshape(2, 3);
        // perm长度!=rank，应抛异常
        List<Long> perm = longArrayToList(new long[]{1L});
        operator.transpose(input, perm);
    }
}
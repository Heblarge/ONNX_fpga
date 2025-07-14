package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedSession;
import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.tensor.DataType;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class HWAcceleratedCastV13Test extends HWAcceleratedTestCase {

    // ------------------------- 合法转换测试 -------------------------
    @Test
    public void testFloatToInt() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{1.8f, -2.3f, 3.0f});
                INDArray expected = Nd4j.create(new int[]{1, -2, 3}, new long[]{3}, org.nd4j.linalg.api.buffer.DataType.INT)
        ) {
            testCast(input, expected, DataType.FLOAT, DataType.INT32);
        }
    }

    @Test
    public void testInt64ToFloat() throws Exception {
        try (
                INDArray input = Nd4j.create(new long[]{100L, -200L, 300L}, new long[]{3}, org.nd4j.linalg.api.buffer.DataType.LONG);
                INDArray expected = Nd4j.create(new float[]{100f, -200f, 300f})
        ) {
            testCast(input, expected, DataType.INT64, DataType.FLOAT);
        }
    }

    @Test
    public void testFloatToBool() throws Exception {
        try (
                INDArray input = Nd4j.create(new float[]{0f, 1.5f, -0.1f});
                INDArray expected = Nd4j.create(new boolean[]{false, true, true})
        ) {
            testCast(input, expected, DataType.FLOAT, DataType.BOOL);
        }
    }

    // ------------------------- 边界值测试 -------------------------
    @Test
    public void testInt32ToInt8Overflow() throws Exception {
        try (
                INDArray input = Nd4j.create(new int[]{128, -129, 255}, new long[]{3}, org.nd4j.linalg.api.buffer.DataType.INT);
                INDArray expected = Nd4j.create(new byte[]{-128, 127, -1}, new long[]{3}, org.nd4j.linalg.api.buffer.DataType.BYTE)
        ) {
            testCast(input, expected, DataType.INT32, DataType.INT8);
        }
    }

    // ------------------------- 测试工具方法 -------------------------
    private void testCast(INDArray input, INDArray expected, DataType inputType, DataType targetType) throws Exception {
        try (HWAcceleratedSession session = new HWAcceleratedSession(null)) {
            HWAcceleratedCastV13 operator = new HWAcceleratedCastV13();
            INDArray output = operator.cast(input, targetType); // 直接传DataType

            assertArrayEquals(
                    String.format("[%s -> %s] Shape mismatch", inputType, targetType),
                    expected.shape(), output.shape());
            assertEquals(
                    String.format("[%s -> %s] DataType mismatch", inputType, targetType),
                    expected.dataType(), output.dataType());

            if (!targetType.equals(DataType.FLOAT16)) {
                assertEquals(
                        String.format("[%s -> %s] Data content mismatch", inputType, targetType),
                        expected.toString(), output.toString());
            }
            System.out.printf("Test [%s -> %s] PASSED%n", inputType, targetType);
        } catch (Throwable t) {
            System.err.printf("Test [%s -> %s] FAILED: %s%n", inputType, targetType, t.getMessage());
            throw t;  // 重新抛出异常，保证JUnit能识别测试失败
        }
    }
}
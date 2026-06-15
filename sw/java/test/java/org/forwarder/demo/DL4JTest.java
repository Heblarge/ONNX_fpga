package org.forwarder.demo;

import org.forwarder.Config;
import org.forwarder.Forwarder;
import org.forwarder.Model;
import org.forwarder.Backend;
import org.forwarder.Session;

import org.onnx4j.Tensor;
import org.onnx4j.tensor.TensorBuilder;
import org.onnx4j.prototypes.OnnxProto3.TensorProto;
import com.google.protobuf.ByteString;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * HWAccelerated 后端测试 - 测试非融合版 gemm_relu_ir7_add_exp.onnx
 * 模型有两个输入：Add_8_output_0 和 Concat_9_output_0
 * 输出：Exp_10_output_0
 */
public class DL4JTest {

    public static void main(String[] args) throws Exception {
        String modelPath = "sw/java/test/resources/gemm_relu_ir7_add_exp.onnx";

        // 初始化共享内存池
        System.out.println("========================================");
        System.out.println("HWAccelerated Backend Test");
        System.out.println("========================================");
        System.out.println("Model: " + modelPath);
        System.out.println("Initializing shared memory pool...");

        if (!Session.initializeSharedMemoryPool()) {
            System.err.println("Failed to initialize shared memory pool!");
            System.exit(1);
        }
        System.out.println("Shared memory pool initialized OK.");

        try {
            runTest(modelPath);
        } finally {
            Session.shutdownSharedMemoryPool();
            System.out.println("Shared memory pool shutdown.");
        }
    }

    private static void runTest(String modelPath) throws Exception {
        Config cfg = Config.builder()
                .setDebug(true)
                .setMemoryByteOrder(ByteOrder.LITTLE_ENDIAN)
                .build();

        Forwarder forwarder = new Forwarder();
        Model model = forwarder.load(modelPath, cfg);
        Backend<?> backend = model.backend("HWAccelerated");

        System.out.println("Backend: " + backend.getName());
        System.out.println("Graph inputs: " + Arrays.toString(Arrays.stream(model.getGraph().getInputs()).map(i -> i.getName()).toArray()));
        System.out.println("Graph outputs: " + Arrays.toString(Arrays.stream(model.getGraph().getOutputs()).map(o -> o.getName()).toArray()));

        try (Session<?> session = backend.newSession()) {
            // 为所有输入创建测试数据
            System.out.println("\n===== Creating test inputs =====");
            int inputIndex = 0;
            for (var input : model.getGraph().getInputs()) {
                long[] inputShape = input.getValueInfo().getShape().toArray();
                int totalElements = 1;
                for (long d : inputShape) totalElements *= (int) d;

                System.out.println("Input name: " + input.getName());
                System.out.println("Input shape: " + Arrays.toString(inputShape));
                System.out.println("Total elements: " + totalElements);

                // 生成符合模型量化格式的测试数据
                // 根据模型参数：
                // - Add_8算子输入: Q18.13格式 (shift=13)
                // - Concat_9输出: Q18.13格式 (shift=13)

                System.out.println("Generating quantized test data...");

                // 创建原始浮点数数据（用户想要测试的实际值）
                float[] floatData = new float[totalElements];
                for (int i = 0; i < totalElements; i++) {
                    if (inputIndex == 0) {
                        // 第一个输入：小范围浮点数 (-0.5 到 0.5)
                        // 量化后不会溢出 Q18.13 范围
                        float[] pattern = {-0.5f, -0.25f, 0.0f, 0.25f, 0.5f};
                        floatData[i] = pattern[i % pattern.length];
                    } else {
                        // 第二个输入：正浮点数 (0.1 到 0.8)
                        floatData[i] = 0.1f; // 0.1, 0.2, ..., 0.8
                    }
                }

                System.out.println("Original float data (first 5): " +
                    Arrays.toString(Arrays.copyOf(floatData, Math.min(5, floatData.length))));

                // 使用 Q18.13 格式量化 (shift=13，对应Add算子的输入格式)
                // 量化后值范围：约 -4096 到 4096 (对于 -0.5 到 0.5 的浮点数)
                int shift = 13;
                Tensor inputTensor = buildQuantizedIntTensor(model, input.getName(), floatData, inputShape, shift);
                session.feed(inputTensor, false);
                inputIndex++;
            }

            // 执行推理
            System.out.println("\n===== Running inference... =====");
            session.forward();

            // 获取输出
            String outputName = model.getGraph().getOutputs()[0].getName();
            Tensor outputTensor = session.getOutput(outputName);

            long[] outputShape = outputTensor.getShape();
            int outputSize = 1;
            for (long d : outputShape) outputSize *= (int) d;

            System.out.println("\n===== Output =====");
            System.out.println("Output name: " + outputName);
            System.out.println("Output shape: " + Arrays.toString(outputShape));
            System.out.println("Output size: " + outputSize);

            // 读取输出数据
            ByteBuffer dataBuffer = outputTensor.getData();
            dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
            dataBuffer.rewind();

            // 打印前20个输出值（包含反量化）
            // 根据模型，Exp算子输出: Q12.19格式 (shift=19)
            int printCount = Math.min(outputSize, 20);
            int outputShift = 19;  // Exp输出格式
            double outputScale = Math.pow(2.0, outputShift);

            System.out.println("Output data (first " + printCount + "):");
            System.out.printf("  Output format: Q12.%d (shift=%d)%n", outputShift, outputShift);
            System.out.println("  Showing quantized and de-quantized values:");
            for (int i = 0; i < printCount; i++) {
                int quantized = dataBuffer.getInt();
                // 反量化：float_value = quantized / 2^shift
                float dequantized = (float)(quantized / outputScale);
                System.out.printf("  out[%d] = %d (0x%08X) → %.6f%n", i, quantized, quantized, dequantized);
            }

            System.out.println("\n===== Test completed successfully! =====");
        } finally {
            model.close();
        }
    }

    /**
     * 构建 FLOAT32 Tensor（不依赖 ND4J）
     */
    private static Tensor buildFloatTensor(Model model, String name, float[] data, long[] shape) {
        TensorProto.Builder builder = TensorProto.newBuilder();

        for (long dim : shape) {
            builder.addDims(dim);
        }

        builder.setDataType(TensorProto.DataType.FLOAT.getNumber());

        ByteBuffer buffer = ByteBuffer.allocate(data.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.asFloatBuffer().put(data);
        buffer.rewind();  // Reset position before copying

        builder.setRawData(ByteString.copyFrom(buffer));

        return TensorBuilder.builder(builder.build(), model.getConfig().getTensorOptions())
                .manager(model.getTensorManager())
                .name(name)
                .build();
    }

    /**
     * 构建量化后的 INT32 Tensor（符合模型Q格式要求）
     *
     * 量化参数：
     * - Add算子输入: Q18.13 (shift=13, scale=1/8192)
     * - Exp算子输入: Q11.20 (shift=20, scale=1/1048576)
     *
     * 量化公式: quantized = round(float_value × 2^shift)
     * 反量化: float_value = quantized / 2^shift
     */
    private static Tensor buildQuantizedIntTensor(Model model, String name, float[] floatData, long[] shape, int shift) {
        TensorProto.Builder builder = TensorProto.newBuilder();

        for (long dim : shape) {
            builder.addDims(dim);
        }

        builder.setDataType(TensorProto.DataType.INT32.getNumber());

        ByteBuffer buffer = ByteBuffer.allocate(floatData.length * 4).order(ByteOrder.LITTLE_ENDIAN);

        // 量化：float → 定点数
        // quantized = round(float_value × 2^shift)
        double scaleFactor = Math.pow(2.0, shift);
        System.out.printf("  Quantizing with shift=%d (scale=%.6e):%n", shift, 1.0/scaleFactor);

        for (int i = 0; i < floatData.length; i++) {
            // 量化到定点数
            long quantized = Math.round(floatData[i] * scaleFactor);

            // 限制在32位有符号整数范围内
            if (quantized > Integer.MAX_VALUE) quantized = Integer.MAX_VALUE;
            if (quantized < Integer.MIN_VALUE) quantized = Integer.MIN_VALUE;

            buffer.putInt((int) quantized);

            // 打印前几个值的转换过程
            if (i < 5) {
                System.out.printf("    [%d] %.4f → %d (0x%08X)%n",
                    i, floatData[i], (int)quantized, (int)quantized);
            }
        }

        buffer.rewind();  // Reset position before copying
        builder.setRawData(ByteString.copyFrom(buffer));

        return TensorBuilder.builder(builder.build(), model.getConfig().getTensorOptions())
                .manager(model.getTensorManager())
                .name(name)
                .build();
    }

    /**
     * 构建 INT32 Tensor（不依赖 ND4J）
     */
    private static Tensor buildIntTensor(Model model, String name, int[] data, long[] shape) {
        TensorProto.Builder builder = TensorProto.newBuilder();

        for (long dim : shape) {
            builder.addDims(dim);
        }

        builder.setDataType(TensorProto.DataType.INT32.getNumber());

        ByteBuffer buffer = ByteBuffer.allocate(data.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.asIntBuffer().put(data);
        buffer.rewind();  // Reset position before copying

        builder.setRawData(ByteString.copyFrom(buffer));

        return TensorBuilder.builder(builder.build(), model.getConfig().getTensorOptions())
                .manager(model.getTensorManager())
                .name(name)
                .build();
    }
}

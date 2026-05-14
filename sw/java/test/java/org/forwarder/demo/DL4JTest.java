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

        // 获取第一个输入的形状
        var firstInput = model.getGraph().getInputs()[0];
        long[] inputShape = firstInput.getValueInfo().getShape().toArray();

        int totalElements = 1;
        for (long d : inputShape) totalElements *= (int) d;

        System.out.println("\n===== Creating test input =====");
        System.out.println("Input name: " + firstInput.getName());
        System.out.println("Input shape: " + Arrays.toString(inputShape));
        System.out.println("Total elements: " + totalElements);

        // 创建 INT32 类型输入数据（固定点量化）
        int[] inputData = new int[totalElements];
        for (int i = 0; i < totalElements; i++) {
            // 简单的递增序列，便于调试
            inputData[i] = 10000 + i * 1000;
        }

        System.out.println("Input data (first 10): " + Arrays.toString(Arrays.copyOf(inputData, Math.min(10, inputData.length))));

        try (Session<?> session = backend.newSession()) {
            // Feed 输入
            Tensor inputTensor = buildInt32Tensor(model, firstInput.getName(), inputData, inputShape);
            session.feed(inputTensor, false);

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

            // 打印前20个输出值
            int printCount = Math.min(outputSize, 20);
            System.out.println("Output data (first " + printCount + "):");
            for (int i = 0; i < printCount; i++) {
                int val = dataBuffer.getInt();
                System.out.printf("  out[%d] = %d (0x%08X)%n", i, val, val);
            }

            System.out.println("\n===== Test completed successfully! =====");
        } finally {
            model.close();
        }
    }

    /**
     * 构建 INT32 Tensor（不依赖 ND4J）
     */
    private static Tensor buildInt32Tensor(Model model, String name, int[] data, long[] shape) {
        TensorProto.Builder builder = TensorProto.newBuilder();

        for (long dim : shape) {
            builder.addDims(dim);
        }

        builder.setDataType(TensorProto.DataType.INT32.getNumber());

        ByteBuffer buffer = ByteBuffer.allocate(data.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.asIntBuffer().put(data);

        builder.setRawData(ByteString.copyFrom(buffer));

        return TensorBuilder.builder(builder.build(), model.getConfig().getTensorOptions())
                .manager(model.getTensorManager())
                .name(name)
                .build();
    }
}

package org.forwarder.demo;

import org.forwarder.Config;
import org.forwarder.Forwarder;
import org.forwarder.Model;
import org.forwarder.Backend;
import org.forwarder.Session;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.buffer.DataType;
import org.onnx4j.Tensor;
import org.onnx4j.tensor.TensorBuilder;
import org.onnx4j.prototypes.OnnxProto3.TensorProto;
import com.google.protobuf.ByteString;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

public class HWTest {

    public static void main(String[] args) throws Exception {
        String modelPathA = "/home/user/Workspace/livehps_1/sw/java/test/resources/gemm_relu_ir7_addlog.onnx";
        String modelPathB = "/home/user/Workspace/livehps_1/sw/java/test/resources/gemm_relu_ir7_add_log.onnx";

        Config cfg = Config.builder()
                .setDebug(true)
                .setMemoryByteOrder(ByteOrder.LITTLE_ENDIAN)
                .build();

        // =========================================================
        // 1️⃣ 构造阶梯输入：[1, 32, 512]
        // =========================================================
        long[] expectedShape = {1, 32, 512};
        int totalElements = 1 * 32 * 512;
        int[] rawData = new int[totalElements];

        for (int i = 0; i < totalElements; i++) {
            // 产生阶梯数据
            rawData[i] = (i / 512) % 32;
        }

        INDArray inputNd = Nd4j.create(rawData, expectedShape, DataType.INT);

        System.out.println("===== SHARED STEP INPUT DATA (INT32) =====");
        System.out.println("Shape: " + Arrays.toString(inputNd.shape()));

        // =========================================================
        // 2️⃣ 运行推理
        // =========================================================
        float[] outA = runOneModel("MODEL-A", modelPathA, cfg, inputNd);
        float[] outB = runOneModel("MODEL-B", modelPathB, cfg, inputNd);

        // =========================================================
        // 3️⃣ 结果深度比对
        // =========================================================
        System.out.println("\n===== DATA INSPECTION (DECIMAL VIEW) =====");
        System.out.printf("%-6s | %-12s | %-12s | %-12s%n", "Idx", "A (Raw)", "B (Raw)", "B (Decoded)");
        System.out.println("------------------------------------------------------------");

        double qScale = Math.pow(2, 22); // 你的 frac_bits
        for (int i = 0; i < outA.length && i < 10 * 512; i += 512) {
            int intA = Float.floatToRawIntBits(outA[i]);
            int intB = Float.floatToRawIntBits(outB[i]);

            // 重点看 B，因为 B 动起来了
            double bReal = (double)intB / qScale;

            System.out.printf("%-6d | %-12d | %-12d | %-12.6f%n",
                    i, intA, intB, bReal);
        }

        int count = 0;
        // 每隔 512 个取一个点观察梯度变化
        for (int i = 0; i < outA.length && count < 20; i += 512) {
            int intA = Float.floatToRawIntBits(outA[i]);
            int intB = Float.floatToRawIntBits(outB[i]);

            System.out.printf("%-6d | %-12d | %-12d | %-12e | %-12e%n",
                    i, intA, intB, outA[i], outA[i] - outB[i]);
            count++;
        }

        double sumDiff = 0;
        for (int i = 0; i < outA.length; i++) sumDiff += Math.abs(outA[i] - outB[i]);
        System.out.printf("%nTotal Average Absolute Error: %e%n", sumDiff / outA.length);
    }

    private static float[] runOneModel(String tag, String modelPath, Config cfg, INDArray inputNd) throws Exception {
        Forwarder forwarder = new Forwarder();
        Model model = forwarder.load(modelPath, cfg);
        Backend<?> backend = model.backend("HWAccelerated");
        float[] outputData;

        try (Session<?> session = backend.newSession()) {
            // 1. 获取模型官方定义的 Input 列表
            var graphInputs = model.getGraph().getInputs();
            java.util.HashSet<String> fedNames = new java.util.HashSet<>();

            for (var gi : graphInputs) {
                String name = gi.getName();
                session.feed(buildInt32Tensor(model, name, inputNd), false);
                fedNames.add(name);
                System.out.println("  [" + tag + "] Fed Input: " + name);
            }

            // 2. 【关键补丁】针对融合模型 A 隐藏的 Initializer 变量
            // 既然 Model A 的内容显示它包含 PPQ_Variable_825，我们强制盲喂
            String specialVar = "PPQ_Variable_825";
            if (!fedNames.contains(specialVar)) {
                try {
                    // 即使它不在 Inputs 里，只要它在 Initializer 里，feed 也会成功
                    session.feed(buildInt32Tensor(model, specialVar, inputNd), false);
                    System.out.println("  [" + tag + "] Forced Fed Initializer: " + specialVar);
                } catch (Exception e) {
                    // 如果 Model B 报错说没这个变量，说明 B 确实已经处理过了，忽略即可
                }
            }

            System.out.println("[" + tag + "] Forwarding...");
            session.forward();

            // 3. 获取第一个输出（无论是 AddLog 输出还是拆分后的 Log 输出）
            String outputName = model.getGraph().getOutputs()[0].getName();
            Tensor outputTensor = session.getOutput(outputName);

            ByteBuffer dataBuffer = outputTensor.getData();
            outputData = new float[dataBuffer.capacity() / 4];
            dataBuffer.rewind();
            dataBuffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(outputData);

            System.out.println("[" + tag + "] Done.");
        } finally {
            model.close();
        }
        return outputData;
    }

    private static Tensor buildInt32Tensor(Model model, String name, INDArray array) {
        TensorProto.Builder builder = TensorProto.newBuilder();
        for (long dim : array.shape()) builder.addDims(dim);
        builder.setDataType(TensorProto.DataType.INT32.getNumber());

        ByteBuffer buffer = ByteBuffer.allocate((int) array.length() * 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.asIntBuffer().put(array.dup('c').data().asInt());
        builder.setRawData(ByteString.copyFrom(buffer));

        return TensorBuilder.builder(builder.build(), model.getConfig().getTensorOptions())
                .manager(model.getTensorManager())
                .name(name)
                .build();
    }
}
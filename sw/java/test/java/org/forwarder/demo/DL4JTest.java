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

public class DL4JTest {

    public static void main(String[] args) throws Exception {
        // 模型 A: 融合算子 AddExp
        String modelPathA = "/home/user/Workspace/livehps_1/sw/java/test/resources/gemm_relu_ir7_addexp.onnx";
        // 模型 B: 拆分算子 Add + Exp
        String modelPathB = "/home/user/Workspace/livehps_1/sw/java/test/resources/gemm_relu_ir7_add_exp.onnx";

        // ===== 1. 构造双输入 (1, 8) =====
        // 注意：Exp(x) 在 x 较大时会溢出，这里建议使用较小的数值进行测试
        INDArray inputA = Nd4j.rand(DataType.FLOAT, 1, 8).muli(2).subi(1); // -1.0 ~ 1.0
        INDArray inputB = Nd4j.rand(DataType.FLOAT, 1, 8).muli(2).subi(1); // -1.0 ~ 1.0

        System.out.println("===== INPUT A =====");
        System.out.println(inputA);
        System.out.println("===== INPUT B =====");
        System.out.println(inputB);

        // ===== 2. 跑模型 A (融合) =====
        float[] outA = runOnce(modelPathA, "DL4J", inputA, inputB);

        // ===== 3. 跑模型 B (拆分) =====
        float[] outB = runOnce(modelPathB, "DL4J", inputA, inputB);

        // ===== 4. 对比结果 =====
        compareResults(outA, outB, 1e-5f);
    }

    private static float[] runOnce(
            String modelPath,
            String backendName,
            INDArray inA,
            INDArray inB
    ) throws Exception {

        Config cfg = Config.builder()
                .setDebug(false)
                .setMemoryByteOrder(ByteOrder.LITTLE_ENDIAN)
                .build();

        Forwarder forwarder = new Forwarder();
        Model model = forwarder.load(modelPath, cfg);
        Backend<?> backend = model.backend(backendName);

        try (Session<?> session = backend.newSession()) {
            // 注意：这里的 "InputA" 和 "InputB" 需要对应你 ONNX 模型里的实际节点名称
            // 这里的 "Output" 也需要对应模型输出节点的名称
            session.feed(buildFloatTensor(model, "InputA", inA), false);
            session.feed(buildFloatTensor(model, "InputB", inB), false);

            session.forward();

            Tensor out = session.getOutput("Output");
            var fb = out.getData().asFloatBuffer();
            fb.rewind();

            float[] result = new float[fb.remaining()];
            fb.get(result);
            return result;
        } finally {
            model.close();
        }
    }

    private static void compareResults(float[] a, float[] b, float threshold) {
        System.out.println("\n===== COMPARISON (Fused vs Split) =====");
        boolean passed = true;
        for (int i = 0; i < a.length; i++) {
            float diff = Math.abs(a[i] - b[i]);
            if (diff > threshold) {
                passed = false;
            }
            System.out.printf("[%02d] Fused: %.6f | Split: %.6f | Diff: %.6e %s%n",
                    i, a[i], b[i], diff, (diff > threshold ? "[FAIL]" : ""));
        }
        System.out.println("\nFinal Result: " + (passed ? "PASS ✅" : "FAIL ❌"));
    }

    private static Tensor buildFloatTensor(Model model, String name, INDArray array) {
        TensorProto.Builder builder = TensorProto.newBuilder();
        for (long d : array.shape()) builder.addDims(d);
        builder.setDataType(TensorProto.DataType.FLOAT.getNumber());

        ByteBuffer buffer = ByteBuffer.allocate((int) array.length() * 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.asFloatBuffer().put(array.dup('c').data().asFloat());
        builder.setRawData(ByteString.copyFrom(buffer));

        return TensorBuilder.builder(builder.build(), model.getConfig().getTensorOptions())
                .manager(model.getTensorManager())
                .name(name)
                .build();
    }
}

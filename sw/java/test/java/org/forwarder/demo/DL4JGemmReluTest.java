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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class DL4JGemmReluTest {

    public static void main(String[] args) throws Exception {

        String modelPathA =
                "/home/user/Workspace/livehps_1/sw/java/test/resources/gemm_relu_ir7.onnx";

        String modelPathB =
                "/home/user/Workspace/livehps_1/sw/java/test/resources/gemm_relu_ir7_1.onnx";

        // ===== 1. 构造输入 =====
        INDArray inputNd = Nd4j.linspace(1, 16, 16)
                .reshape(1, 16)
                .castTo(DataType.FLOAT);

        System.out.println("===== INPUT =====");
        System.out.println(inputNd);

        // ===== 2. 跑模型 A =====
        float[] outA = runOnce(
                modelPathA,
                "DL4J",
                "Input",
                "Output",
                inputNd
        );

        // ===== 3. 跑模型 B =====
        float[] outB = runOnce(
                modelPathB,
                "DL4J",
                "Input",
                "Output",
                inputNd
        );

        // ===== 4. 打印结果 =====
        System.out.println("\n===== OUTPUT A =====");
        System.out.println(Arrays.toString(outA));

        System.out.println("\n===== OUTPUT B =====");
        System.out.println(Arrays.toString(outB));

        // ===== 5. 对比 diff =====
        System.out.println("\n===== DIFF (A - B) =====");
        for (int i = 0; i < outA.length; i++) {
            System.out.printf(
                    "[%02d]  A=%.6f  B=%.6f  diff=%.6e%n",
                    i, outA[i], outB[i], outA[i] - outB[i]
            );
        }
    }

    /**
     * 跑一次模型，返回 float[] 输出
     */
    private static float[] runOnce(
            String modelPath,
            String backendName,
            String inputName,
            String outputName,
            INDArray inputNd
    ) throws Exception {

        Config cfg = Config.builder()
                .setDebug(false)
                .setMemoryByteOrder(ByteOrder.LITTLE_ENDIAN)
                .build();

        Forwarder forwarder = new Forwarder();
        Model model = forwarder.load(modelPath, cfg);
        Backend<?> backend = model.backend(backendName);

        try (Session<?> session = backend.newSession()) {

            Tensor inputTensor = buildFloatTensor(
                    model,
                    inputName,
                    inputNd
            );

            session.feed(inputTensor, false);
            session.forward();

            Tensor out = session.getOutput(outputName);
            var fb = out.getData().asFloatBuffer();
            fb.rewind();

            float[] result = new float[fb.remaining()];
            fb.get(result);

            return result;
        } finally {
            model.close();
        }
    }

    /**
     * INDArray -> ONNX Tensor
     */
    private static Tensor buildFloatTensor(
            Model model,
            String name,
            INDArray array
    ) {

        TensorProto.Builder builder = TensorProto.newBuilder();

        for (long d : array.shape()) {
            builder.addDims(d);
        }

        builder.setDataType(TensorProto.DataType.FLOAT.getNumber());

        ByteBuffer buffer = ByteBuffer
                .allocate((int) array.length() * 4)
                .order(ByteOrder.LITTLE_ENDIAN);

        buffer.asFloatBuffer()
                .put(array.dup('c').data().asFloat());

        builder.setRawData(
                com.google.protobuf.ByteString.copyFrom(buffer)
        );

        return TensorBuilder.builder(
                        builder.build(),
                        model.getConfig().getTensorOptions()
                )
                .manager(model.getTensorManager())
                .name(name)
                .build();
    }
}

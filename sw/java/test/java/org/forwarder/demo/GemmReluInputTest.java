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

public class GemmReluInputTest {

    public static void main(String[] args) throws Exception {

        // ===== 1. 模型路径 =====
        String modelPath =
                "/home/user/Workspace/livehps_1/sw/java/test/resources/gemm_relu_ir7.onnx";

        // ===== 2. Config / Forwarder =====
        Config cfg = Config.builder()
                .setDebug(true)
                .setMemoryByteOrder(ByteOrder.LITTLE_ENDIAN)
                .build();

        Forwarder forwarder = new Forwarder();
        Model model = forwarder.load(modelPath, cfg);

        // ===== 3. DL4J backend =====
        Backend<?> backend = model.backend("DL4J");

        try (Session<?> session = backend.newSession()) {

            // ===== 4. 构造输入 INDArray =====
            float[] inputData = new float[16];
            for (int i = 0; i < inputData.length; i++) {
                inputData[i] = i + 1;
            }

            INDArray inputNd = Nd4j.create(inputData, new long[]{1, 16})
                    .castTo(DataType.FLOAT);

            System.out.println("===== INPUT (INDArray) =====");
            System.out.println(inputNd);

            // ===== 5. INDArray -> ONNX Tensor =====
            Tensor inputTensor = buildFloatTensor(
                    model,
                    "Input",            // ⚠️ 必须和 onnx 输入名一致
                    inputNd
            );

            // ===== 6. feed =====
            session.feed(inputTensor, false);

            // ===== 7. forward =====
            session.forward();

            // ===== 8. 取输出 =====
            Tensor outputTensor = session.getOutput("Output");

            // 一定要 rewind
            var fb = outputTensor.getData().asFloatBuffer();
            fb.rewind();

            float[] outputData = new float[fb.remaining()];
            fb.get(outputData);

            System.out.println("===== OUTPUT =====");
            System.out.println(Arrays.toString(outputData));
        }

        model.close();
    }

    /**
     * 用 INDArray 构造 onnx4j Tensor（完全对齐 FWTestCase 风格）
     */
    private static Tensor buildFloatTensor(
            Model model,
            String name,
            INDArray array
    ) {

        TensorProto.Builder builder = TensorProto.newBuilder();

        // shape
        for (long dim : array.shape()) {
            builder.addDims(dim);
        }

        builder.setDataType(TensorProto.DataType.FLOAT.getNumber());

        // raw data
        ByteBuffer buffer = ByteBuffer
                .allocate((int) array.length() * 4)
                .order(ByteOrder.LITTLE_ENDIAN);

        buffer.asFloatBuffer().put(array.dup('c').data().asFloat());

        builder.setRawData(
                com.google.protobuf.ByteString.copyFrom(buffer)
        );

        return TensorBuilder.builder(builder.build(), model.getConfig().getTensorOptions())
                .manager(model.getTensorManager())
                .name(name)
                .build();
    }
}

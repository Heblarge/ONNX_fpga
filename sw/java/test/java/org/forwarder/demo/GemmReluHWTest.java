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

public class GemmReluHWTest {

    public static void main(String[] args) throws Exception {

        String modelPathA =
                "/home/user/Workspace/livehps_1/sw/java/test/resources/gemm_relu_ir7_3.onnx";

        String modelPathB =
                "/home/user/Workspace/livehps_1/sw/java/test/resources/gemm_relu_ir7_2.onnx";

        Config cfg = Config.builder()
                .setDebug(true)
                .setMemoryByteOrder(ByteOrder.LITTLE_ENDIAN)
                .build();

        // ===============================
        // 1️⃣ 构造同一份输入
        // ===============================
        float[] inputData = new float[16];
        for (int i = 0; i < inputData.length; i++) {
            inputData[i] = i + 2000000000;
        }

        INDArray inputNd = Nd4j.create(inputData, new int[]{1, 16})
                .castTo(DataType.INT);

        System.out.println("===== INPUT =====");
        System.out.println("shape = " + Arrays.toString(inputNd.shape()));
        System.out.println(inputNd);

        // ===============================
        // 2️⃣ 分别跑两个模型
        // ===============================
        float[] outA = runOneModel("MODEL-A", modelPathA, cfg, inputNd);
        float[] outB = runOneModel("MODEL-B", modelPathB, cfg, inputNd);

        // ===============================
        // 3️⃣ 对比结果
        // ===============================
        System.out.println("\n===== COMPARE OUTPUT =====");
        for (int i = 0; i < Math.min(outA.length, outB.length); i++) {
            float diff = outA[i] - outB[i];
            System.out.printf(
                    "idx=%2d  A=%e  B=%e  diff=%e%n",
                    i, outA[i], outB[i], diff
            );
        }
    }

    /**
     * 跑一次模型，返回 output float[]
     */
    private static float[] runOneModel(
            String tag,
            String modelPath,
            Config cfg,
            INDArray inputNd
    ) throws Exception {

        Forwarder forwarder = new Forwarder();
        Model model = forwarder.load(modelPath, cfg);
        Backend<?> backend = model.backend("HWAccelerated");

        float[] outputData;

        try (Session<?> session = backend.newSession()) {

            Tensor inputTensor = buildIntTensor(
                    model,
                    "Input",
                    inputNd
            );

            session.feed(inputTensor, false);
            session.forward();

            Tensor outputTensor = session.getOutput("Output");

            long[] shape = outputTensor.getShape();
            int logicalSize = 1;
            for (long d : shape) logicalSize *= (int) d;

            var fb = outputTensor.getData().asFloatBuffer();
            fb.rewind();

            outputData = new float[logicalSize];
            fb.get(outputData);

            System.out.println("\n===== " + tag + " =====");
            System.out.println("model = " + modelPath);
            System.out.println("shape = " + Arrays.toString(shape));
            System.out.println("output = " + Arrays.toString(outputData));
        }

        model.close();
        return outputData;
    }

    /**
     * INDArray -> ONNX INT32 Tensor
     */
    private static Tensor buildIntTensor(
            Model model,
            String name,
            INDArray array
    ) {
        TensorProto.Builder builder = TensorProto.newBuilder();

        for (long dim : array.shape()) {
            builder.addDims(dim);
        }

        builder.setDataType(TensorProto.DataType.INT32.getNumber());

        ByteBuffer buffer = ByteBuffer
                .allocate((int) array.length() * 4)
                .order(ByteOrder.LITTLE_ENDIAN);

        buffer.asIntBuffer().put(array.dup('c').data().asInt());

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

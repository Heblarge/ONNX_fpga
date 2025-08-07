package onnx_debug_java;

import org.onnx4j.prototypes.OnnxProto3.TensorProto;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.util.Arrays;

public class compare_output_rot {

    /**
     * 数据容器，用于同时存储张量的形状和数据。
     */
    static class TensorData {
        final long[] shape;
        final float[] data;

        TensorData(long[] shape, float[] data) {
            this.shape = shape;
            this.data = data;
        }
    }

    /**
     * 从 .pb 文件加载 ONNX TensorProto 并提取形状和浮点数据。
     */
    public static TensorData loadPbFile(File path) throws IOException {
        if (!path.exists()) {
            System.err.println("错误：找不到 PB 文件 " + path);
            return null;
        }
        byte[] fileBytes = Files.readAllBytes(path.toPath());
        TensorProto tensorProto = TensorProto.parseFrom(fileBytes);
        long[] shape = tensorProto.getDimsList().stream().mapToLong(Long::longValue).toArray();
        ByteBuffer byteBuffer = tensorProto.getRawData().asReadOnlyByteBuffer();
        FloatBuffer floatBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        float[] data = new float[floatBuffer.capacity()];
        floatBuffer.get(data);
        return new TensorData(shape, data);
    }

    /**
     * 从自定义 .bin 文件中加载数据。
     */
    public static TensorData loadBinFile(File path) throws IOException {
        if (!path.exists()) {
            System.err.println("错误：找不到 BIN 文件 " + path);
            return null;
        }
        try (DataInputStream dis = new DataInputStream(new FileInputStream(path))) {
            int rank = dis.readInt();
            long[] shape = new long[rank];
            long totalElements = 1;
            for (int i = 0; i < rank; i++) {
                shape[i] = dis.readLong();
                totalElements *= shape[i];
            }
            float[] data = new float[(int) totalElements];
            for (int i = 0; i < data.length; i++) {
                data[i] = dis.readFloat();
            }
            return new TensorData(shape, data);
        }
    }

    /**
     * 将一个 float 数组以矩阵形式保存到文件。
     */
    private static void saveMatrixToFile(String filePath, String title, long[] shape, float[] data) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("--- " + title + " ---");
            writer.println("Shape: " + Arrays.toString(shape));
            writer.println();

            if (shape.length == 3) {
                long dim1 = shape[0], dim2 = shape[1], dim3 = shape[2];
                int linearIndex = 0;
                for (int i = 0; i < dim1; i++) {
                    writer.printf("--- Slice (Dimension 1, Index %d) ---%n", i);
                    for (int j = 0; j < dim2; j++) {
                        for (int k = 0; k < dim3; k++) {
                            writer.printf("%15.10f  ", data[linearIndex]);
                            linearIndex++;
                        }
                        writer.println();
                    }
                    writer.println();
                }
            } else {
                writer.println(Arrays.toString(data));
            }
            System.out.printf("成功将 [%s] 保存到: %s%n", title, new File(filePath).getAbsolutePath());
        }
    }

    /**
     * 将一个 double 数组（用于误差）以矩阵形式保存到文件。
     */
    private static void saveMatrixToFile(String filePath, String title, long[] shape, double[] data) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("--- " + title + " ---");
            writer.println("Shape: " + Arrays.toString(shape));
            writer.println();

            if (shape.length == 3) {
                long dim1 = shape[0], dim2 = shape[1], dim3 = shape[2];
                int linearIndex = 0;
                for (int i = 0; i < dim1; i++) {
                    writer.printf("--- Slice (Dimension 1, Index %d) ---%n", i);
                    for (int j = 0; j < dim2; j++) {
                        for (int k = 0; k < dim3; k++) {
                            writer.printf("%15.10f  ", data[linearIndex]);
                            linearIndex++;
                        }
                        writer.println();
                    }
                    writer.println();
                }
            } else {
                writer.println(Arrays.toString(data));
            }
            System.out.printf("成功将 [%s] 保存到: %s%n", title, new File(filePath).getAbsolutePath());
        }
    }

    public static void main(String[] args) throws IOException {
        File pbFile = new File("/home/user/Workspace/livehps_1/sw/java/test/java/onnx_debug_java/pb_output/output_rot.pb");
        File binFile = new File("/home/user/Workspace/livehps_1/sw/java/test/java/onnx_debug_java/onnx_output/rot.bin");
        float tolerance = 1e-3f;

        String outputDirectory = "/home/user/Workspace/livehps_1/sw/java/test/java/onnx_debug_java/";

        File pbValuesFile = new File(outputDirectory, "pb_rot_values.txt");
        File binValuesFile = new File(outputDirectory, "bin_rot_values.txt");
        File errorDetailsFile = new File(outputDirectory, "error_rot_details.txt");


        System.out.println("--- 开始误差分析 ---");
        System.out.println("标准答案 (.pb): " + pbFile.getAbsolutePath());
        System.out.println("Java 输出 (.bin): " + binFile.getAbsolutePath());
        System.out.println();

        TensorData pbTensor = loadPbFile(pbFile);
        TensorData binTensor = loadBinFile(binFile);

        if (pbTensor == null || binTensor == null) {
            System.err.println("文件加载失败，无法继续。");
            return;
        }

        if (!Arrays.equals(pbTensor.shape, binTensor.shape) || pbTensor.data.length != binTensor.data.length) {
            System.err.printf("错误: 张量尺寸或元素数量不匹配!%n  PB: shape=%s, length=%d%n  BIN: shape=%s, length=%d%n",
                    Arrays.toString(pbTensor.shape), pbTensor.data.length,
                    Arrays.toString(binTensor.shape), binTensor.data.length);
            return;
        }

        saveMatrixToFile(pbValuesFile.getAbsolutePath(), ".pb 文件内容", pbTensor.shape, pbTensor.data);
        saveMatrixToFile(binValuesFile.getAbsolutePath(), ".bin 文件内容", binTensor.shape, binTensor.data);
        System.out.println();

        double[] absDiff = new double[pbTensor.data.length];
        double maxError = 0.0, minError = Double.MAX_VALUE, sumError = 0.0;
        int mismatchCount = 0;

        for (int i = 0; i < pbTensor.data.length; i++) {
            absDiff[i] = Math.abs(pbTensor.data[i] - binTensor.data[i]);
            if (absDiff[i] > maxError) maxError = absDiff[i];
            if (absDiff[i] < minError) minError = absDiff[i];
            sumError += absDiff[i];
            if (absDiff[i] > tolerance) mismatchCount++;
        }
        double meanError = sumError / pbTensor.data.length;

        System.out.println("--- 误差分析摘要 ---");
        System.out.printf("最大误差 (Max Error):    %.10f%n", maxError);
        System.out.printf("最小误差 (Min Error):    %.10f%n", minError);
        System.out.printf("平均误差 (Mean Error):   %.10f%n", meanError);
        System.out.printf("总元素数量:              %d%n", pbTensor.data.length);
        System.out.printf("超过容忍度 (%.5f) 的元素数量: %d%n", tolerance, mismatchCount);

        if (mismatchCount == 0) {
            System.out.println("\n结论: 所有元素的误差均在容忍度范围内。");
        } else {
            System.out.printf("%n结论: 存在 %d 个元素的误差超过了容忍度。%n", mismatchCount);
        }

        System.out.println(); // 增加一个换行
        // 保存误差矩阵
        saveMatrixToFile(errorDetailsFile.getAbsolutePath(), "元素绝对误差", pbTensor.shape, absDiff);
    }
}
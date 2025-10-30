/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.forwarder.demo;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import javax.naming.OperationNotSupportedException;

import com.google.protobuf.ByteString;
import org.apache.commons.io.FileUtils;
import org.forwarder.Backend;
import org.forwarder.Model;
import org.forwarder.Config;
import org.forwarder.Forwarder;
import org.forwarder.Session;
import org.forwarder.executor.impls.RayExecutor;
import org.forwarder.executor.impls.SequentialExecutor;
import org.nd4j.linalg.api.buffer.DataType;
import org.onnx4j.Tensor;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.prototypes.OnnxProto3.TensorProto;
import org.onnx4j.tensor.TensorBuilder;
import java.util.Arrays;
import org.nd4j.linalg.api.ndarray.INDArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import junit.framework.TestCase;

public abstract class FWTestCase extends TestCase {

    private static final int TENSOR_MAX_OUTPUT_LEN = 1000;

    private static Logger logger = LoggerFactory.getLogger(FWTestCase.class);

    // 保存模式
    public enum SaveMode {
        NONE,             // 不保存任何结果
        FINAL_ONLY,       // 只保存最终结果
        ALL_INTERMEDIATE  // 保存所有中间结果
    }

    // 输出数据模式
    public enum OutputMode {
        Normal,      // 不对输出做任何处理
        Dequantize   // 缩小输出倍数
        // rot       22
        // trj       25
        // pre_trans 24
    }

    /**
         * Create the test case
         *
         * @param testName
         *            name of the test case
         * @throws IOException
         * @throws FileNotFoundException
         */
    public FWTestCase(String testName) {
        super(testName);
    }

    protected void testModel(
            Map<List<String>, List<String>> tensorPairPaths,
            String modelPath,
            List<String> inputNames,
            List<String> outputNames,
            String[] backendNames,
            float tolerance,
            Map<String, String> backendOutputPaths,
            SaveMode saveMode,
            OutputMode outputMode

    ) throws FileNotFoundException, IOException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, OperationNotSupportedException {
        // 从Resource获取模型文件地址
        String absoluteModelPath = URLDecoder.decode(FWTestCase.class.getResource(modelPath).getFile(), "utf-8");
        assertNotNull(absoluteModelPath);//判断非空

        try {
            //尝试构建Forwarder，加载model
            Config cfg=Config.builder()
                    .setDebug(true)
                    .setMemoryByteOrder(ByteOrder.LITTLE_ENDIAN)
                    .setExecutor(SequentialExecutor.class)
                    .build();
            Forwarder forwarder = new Forwarder();
            Model loadedModel=forwarder.load(absoluteModelPath,cfg).executor(SequentialExecutor.class);

//            SequentialExecutor<?> executor = (SequentialExecutor<?>) loadedModel.getExecutor();
//            executor.printExecutionSequence();

            assert forwarder != null;
            assert loadedModel!=null;

            // 使用并行流处理多个数据对，实现多核并行
            tensorPairPaths.entrySet().parallelStream().forEach(tensorPairPath -> {
                try {
                    List<Tensor> inputTensors = new ArrayList<>();
                    List<Tensor> expectedOutputTensors = new ArrayList<>();
                    String dataSubDirName = new File(tensorPairPath.getKey().get(0)).getParentFile().getName();

                    // 加载所有输入 tensor
                    for (int i = 0; i < inputNames.size(); i++) {
                        inputTensors.add(this.loadTensor(loadedModel, inputNames.get(i), tensorPairPath.getKey().get(i)));
                    }

                    // 加载所有预期输出 tensor
                    for (int i = 0; i < outputNames.size(); i++) {
                        expectedOutputTensors.add(this.loadTensor(loadedModel, outputNames.get(i), tensorPairPath.getValue().get(i)));
                    }

                    // 遍历后端
                    for (String backendName : backendNames) {
                        Backend<?> backend = loadedModel.backend(backendName);
                        try (Session<?> session = backend.newSession()) {

                            // 输入全部 feed
                            for (Tensor input : inputTensors) {
                                session.feed(input, false);
                            }
                            // 执行推理
                            session.forward();

                            if (saveMode != SaveMode.NONE) {
                                String basePath = backendOutputPaths.get(backendName);
                                if (basePath != null && !basePath.isEmpty()) {
                                    File outputDir = new File(basePath, dataSubDirName);
                                    if(saveMode == SaveMode.ALL_INTERMEDIATE) {
                                        saveAllTensorsAsBin(session, outputDir);
                                    } else if (saveMode == SaveMode.FINAL_ONLY) {
                                        saveFinalTensorsAsPb(session, outputNames, outputDir,outputMode);
                                    }
                                }
                            }

                            // 获取每个输出并比较
                            for (int i = 0; i < outputNames.size(); i++) {
                                Tensor actual = session.getOutput(outputNames.get(i));
                                Tensor expected = expectedOutputTensors.get(i);
                                logger.info("======================= Comparing output tensor: {} =======================", outputNames.get(i));
                                logger.info("  EXPECTED: {}", dumpTensor(expected));
                                logger.info("    ACTUAL: {}", dumpTensor(actual));
                                //this.assertSimilarity(actual, expected, tolerance);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing tensor pair: {}", tensorPairPath, e);
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            logger.error("Failed to close forwarder instance", e);
        }


        logger.info("Finished");
    }

    private void saveAllTensorsAsBin(Session<?> session, File outputDir) throws IOException {
        setupDirectory(outputDir);
        Graph graph = session.getBackend().getModel().getGraph();
        Map<String, Node> outputProducingNodeMap = new HashMap<>();
        for (Node node : graph.getNodes()) {
            for (String outputName : node.getOutputNames()) {
                outputProducingNodeMap.put(outputName, node);
            }
        }
        Map<String, ?> intermediateOutputs = session.getIntermediateOutputs();
        for(Entry<String, ?> entry : intermediateOutputs.entrySet()) {
            String name = entry.getKey();
            Node producingNode = outputProducingNodeMap.get(name);
            if (producingNode != null && !"Constant".equals(producingNode.getOpType())) {
                saveTensorAsBinary(name, (INDArray) entry.getValue(), outputDir);
            }
        }
    }

    private void saveFinalTensorsAsPb(Session<?> session, List<String> finalOutputNames, File outputDir, OutputMode outputMode) throws IOException {
        setupDirectory(outputDir);
        for(String name : finalOutputNames) {
            INDArray tensorData = (INDArray) session.getIntermediateOutput(name);
            String saveName = name;
            if (name.equals("pre_trans_fp")) {
                saveName = "pre_trans";
            }
            if (outputMode == OutputMode.Dequantize) {
                if (tensorData.dataType() != DataType.FLOAT) {
                    tensorData = tensorData.castTo(DataType.FLOAT);
                }
                if (saveName.equals("pre_trans")) {
                    tensorData = tensorData.div(Math.pow(2, 24)); // 除以 2^24
                } else if (saveName.equals("rot")) {
                    tensorData = tensorData.div(Math.pow(2, 22)); // 除以 2^22
                } else if (saveName.equals("trj")) {
                    tensorData = tensorData.div(Math.pow(2, 25)); // 除以 2^25
                }
            }
            saveTensorAsPb(saveName, tensorData, outputDir);
        }
    }

    private void setupDirectory(File dir) {
        if (dir.exists()) {
            for (File file : dir.listFiles()) file.delete();
        }
        dir.mkdirs();
        // System.out.println("输出将被保存到: " + dir.getAbsolutePath());
    }

    private void saveTensorAsBinary(String name, INDArray tensorData, File outputDir) {
        if (tensorData != null) {
            String sanitizedName = name.replace('/', '_').replace(':', '_');
            String fileName = sanitizedName + ".bin";
            File outputFile = new File(outputDir, fileName);

            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(outputFile))) {
                dos.writeInt(tensorData.rank());
                for (long dim : tensorData.shape()) {
                    dos.writeLong(dim);
                }
                dos.writeInt(tensorData.dataType().ordinal());
                INDArray cOrderTensor = tensorData.dup('c');

                switch (cOrderTensor.dataType()) {
                    case INT:
                        int[] intData = cOrderTensor.data().asInt();
                        for (int val : intData) {
                            dos.writeInt(val);
                        }
                        break;
                    case LONG:
                        long[] longData = cOrderTensor.data().asLong();
                        for (long val : longData) {
                            dos.writeLong(val);
                        }
                        break;
                    case FLOAT:
                        float[] floatData = cOrderTensor.data().asFloat();
                        for (float val : floatData) {
                            dos.writeFloat(val);
                        }
                        break;
                    case DOUBLE:
                        double[] doubleData = cOrderTensor.data().asDouble();
                        for (double val : doubleData) {
                            dos.writeDouble(val);
                        }
                        break;
                    case BOOL:
                        byte[] boolDataAsBytes = cOrderTensor.data().asBytes();
                        dos.write(boolDataAsBytes);
                        break;
                    default:
                        throw new IOException("Unsupported data type for binary serialization: " + cOrderTensor.dataType());
                }
            } catch (IOException e) {
                System.err.println("Failed to save tensor as .bin: " + name);
                e.printStackTrace();
            }
        }
    }


    private void saveTensorAsPb(String name, INDArray tensor, File outputDir) throws IOException {
        if (tensor == null) return;
        String sanitizedName = name.replace('/', '_').replace(':', '_');
        String fileNameWithPrefix = "output_" + sanitizedName + ".pb";
        File file = new File(outputDir, fileNameWithPrefix);

        TensorProto.Builder builder = TensorProto.newBuilder();
        for (long dim : tensor.shape()) {
            builder.addDims(dim);
        }
        builder.setDataType(mapDl4jDataTypeToOnnx(tensor.dataType()).getNumber());

        ByteBuffer byteBuffer = ByteBuffer.allocate((int) (tensor.length() * 4)).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.asFloatBuffer().put(tensor.dup('c').data().asNioFloat());
        builder.setRawData(ByteString.copyFrom(byteBuffer));

        try (FileOutputStream fos = new FileOutputStream(file)) {
            builder.build().writeTo(fos);
        }
    }

    private TensorProto.DataType mapDl4jDataTypeToOnnx(DataType dl4jType) {
        switch (dl4jType) {
            case FLOAT: return TensorProto.DataType.FLOAT;
            case DOUBLE: return TensorProto.DataType.DOUBLE;
            case INT: return TensorProto.DataType.INT32;
            case LONG: return TensorProto.DataType.INT64;
            default: return TensorProto.DataType.UNDEFINED;
        }
    }


    /**
     * Determines whether two compared tensors, data types and shapes are equal and numerically similar (within the specified tolerance range).
     *
     * @param actual
     * @param expected
     * @param tolerance
     */
    protected void assertSimilarity(Tensor actual, Tensor expected, float tolerance) {
        assertEquals(expected.getValueInfo(), actual.getValueInfo());
        assertEquals(expected.getData().capacity(), actual.getData().capacity());

        FloatBuffer actualFloat = actual.getData().asFloatBuffer();
        FloatBuffer expectedFloat = expected.getData().asFloatBuffer();

        for (int n = 0; n < actualFloat.capacity(); n++) {
            Float nActual = actualFloat.get();
            Float nExpected = expectedFloat.get();
            assertTrue(Math.abs((nActual) - (nExpected)) <= tolerance);
        }
    }
//    protected void assertSimilarity(Tensor actual, Tensor expected, float tolerance) {
//        assertEquals(expected.getValueInfo(), actual.getValueInfo());
//        long[] shape = actual.getShape();
//
//        FloatBuffer actualBuffer = actual.getData().asFloatBuffer();
//        FloatBuffer expectedBuffer = expected.getData().asFloatBuffer();
//        actualBuffer.rewind();
//        expectedBuffer.rewind();
//
//        // 步骤 1: 尝试直接比较 (假设两者都是行优先)
//        boolean directMatch = true;
//        for (int i = 0; i < expectedBuffer.capacity(); i++) {
//            if (Math.abs(expectedBuffer.get(i) - actualBuffer.get(i)) > tolerance) {
//                directMatch = false;
//                break;
//            }
//        }
//
//        if (directMatch) {
//            logger.info("--> Tensors match with direct (Row-Major) comparison. No conversion needed.");
//            assertTrue(true); // 断言成功
//            return;
//        }
//
//        // 步骤 2: 如果直接比较失败，则执行列优先到行优先的转换
//        logger.info("--> Direct comparison failed. Assuming Column-Major layout and attempting conversion...");
//
//        if (shape.length != 3) {
//            fail("此方法仅为3D Tensor设计，当前Tensor维度为: " + shape.length);
//            return;
//        }
//        long dim1 = shape[0], dim2 = shape[1], dim3 = shape[2];
//
//        float[] actualRowMajorData = new float[actualBuffer.capacity()];
//        for (int i = 0; i < dim1; i++) {
//            for (int j = 0; j < dim2; j++) {
//                for (int k = 0; k < dim3; k++) {
//                    int sourceIndex = (int) (i + j * dim1 + k * dim1 * dim2);
//                    int destinationIndex = (int) (i * dim2 * dim3 + j * dim3 + k);
//                    actualRowMajorData[destinationIndex] = actualBuffer.get(sourceIndex);
//                }
//            }
//        }
//
//        // 步骤 3: 比较转换后的数据
//        logger.info("  CONVERTED ACTUAL (Row-Major): " + Arrays.toString(actualRowMajorData));
//        for (int i = 0; i < expectedBuffer.capacity(); i++) {
//            float nExpected = expectedBuffer.get(i);
//            float nActual = actualRowMajorData[i];
//            assertTrue(
//                    String.format("转换后，在行优先索引 %d 处不匹配: 期望值是 <%f>, 实际值是 <%f>", i, nExpected, nActual),
//                    Math.abs(nActual - nExpected) <= tolerance
//            );
//        }
//        logger.info("--> Tensors match after Column-to-Row conversion.");
//    }

    protected Tensor loadTensor(Model model, String inputName, String tensorProtoName)
            throws InvalidProtocolBufferException, IOException, NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        String tensorProtoPath = URLDecoder.decode(this.getClass().getResource(tensorProtoName).getFile(), "utf-8");
        assertNotNull(tensorProtoPath);
        TensorProto tensorProto = TensorProto.parseFrom(FileUtils.readFileToByteArray(new File(tensorProtoPath)));
        model.getConfig().getTensorOptions();
        return TensorBuilder.builder(tensorProto, model.getConfig().getTensorOptions())
                .manager(model.getTensorManager()).name(inputName).build();
    }

    protected String dumpTensor(Tensor tensor) {
        String tensorString = tensor.toString().replaceAll("[\n\t]", "");
        return tensorString.length() > TENSOR_MAX_OUTPUT_LEN
                ? tensorString.subSequence(0, TENSOR_MAX_OUTPUT_LEN) + " ..." : tensorString;
    }

    /**
     * 比较两个后端之间的中间张量误差。
     * @param tensorPairPaths 输入和输出张量路径的映射
     * @param modelPath 模型路径
     * @param inputNames 输入张量名称列表
     * @param outputNames 输出张量名称列表
     * @param backendNames 后端名称数组（必须包含两个后端名称）
     * @param tolerance 误差容忍度
     */
    protected void compareIntermediateTensors(
            Map<List<String>, List<String>> tensorPairPaths,
            String modelPath,
            List<String> inputNames,
            List<String> outputNames,
            String[] backendNames,
            float tolerance
    ) throws Exception {
        if (backendNames.length != 2) {
            throw new IllegalArgumentException("必须传入两个后端名称进行比较。");
        }

        // 从资源加载模型
        String absoluteModelPath = URLDecoder.decode(FWTestCase.class.getResource(modelPath).getFile(), "utf-8");
        assertNotNull(absoluteModelPath);

        Config cfg = Config.builder()
                .setDebug(true)
                .setMemoryByteOrder(ByteOrder.LITTLE_ENDIAN)
                .setExecutor(SequentialExecutor.class)
                .build();
        Forwarder forwarder = new Forwarder();
        Model loadedModel = forwarder.load(absoluteModelPath, cfg).executor(SequentialExecutor.class);
        assert forwarder != null;
        assert loadedModel != null;

        // 遍历待测试的所有输入
        for (Map.Entry<List<String>, List<String>> tensorPairPath : tensorPairPaths.entrySet()) {
            List<Tensor> inputTensors = new ArrayList<>();

            // 加载所有输入 tensor
            for (int i = 0; i < inputNames.size(); i++) {
                inputTensors.add(this.loadTensor(loadedModel, inputNames.get(i), tensorPairPath.getKey().get(i)));
            }

            Map<String, Map<String, Tensor>> backendTensors = new HashMap<>();

            for (String backendName : backendNames) {
                Backend<?> backend = loadedModel.backend(backendName);
                try (Session<?> session = backend.newSession()) {

                    // 输入全部 feed
                    for (Tensor input : inputTensors) {
                        session.feed(input, false);
                    }

                    // 执行推理
                    session.forward();

                    // 保存中间张量到内存
                    Map<String, Tensor> tensors = new HashMap<>();
                    for (String outputName : outputNames) {
                        Tensor tensor = session.getIntermediateOutputTensor(outputName);
                        tensors.put(outputName, tensor);
                    }
                    backendTensors.put(backendName, tensors);
                }
            }

            // 比较两个后端的张量
            String backend1 = backendNames[0];
            String backend2 = backendNames[1];
            for (String tensorName : outputNames) {
                Tensor tensor1 = backendTensors.get(backend1).get(tensorName);
                Tensor tensor2 = backendTensors.get(backend2).get(tensorName);

                // 比较张量误差
                assertSimilarity(tensor1, tensor2, tolerance);
                System.out.println("张量 " + tensorName + " 在 " + backend1 + " 和 " + backend2 + " 后端之间一致。");
            }
        }

    }

}

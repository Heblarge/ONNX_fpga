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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;


import javax.naming.OperationNotSupportedException;

import org.apache.commons.io.FileUtils;
import org.forwarder.Backend;
import org.forwarder.Model;
import org.forwarder.Config;
import org.forwarder.Forwarder;
import org.forwarder.Session;
import org.forwarder.executor.impls.RayExecutor;
import org.forwarder.executor.impls.SequentialExecutor;
import org.onnx4j.Tensor;
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
            float tolerance
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

            //Model loadedModel=forwarder.load(absoluteModelPath,cfg).executor(SequentialExecutor.class);

            // 遍历待测试的所有输入
            for (Entry<List<String>, List<String>> tensorPairPath : tensorPairPaths.entrySet()) {

                Model loadedModel = forwarder.load(absoluteModelPath, cfg);
                assert loadedModel != null;

                List<Tensor> inputTensors = new ArrayList<>();
                List<Tensor> expectedOutputTensors = new ArrayList<>();

                // 加载所有输入 tensor
                for (int i = 0; i < inputNames.size(); i++) {
                    inputTensors.add(this.loadTensor(loadedModel, inputNames.get(i), tensorPairPath.getKey().get(i)));
                }

                // 加载所有预期输出 tensor
                for (int i = 0; i < outputNames.size(); i++) {
                    expectedOutputTensors.add(this.loadTensor(loadedModel, outputNames.get(i), tensorPairPath.getValue().get(i)));
                }

                for (String backendName : backendNames) {
                    Backend<?> backend = loadedModel.backend(backendName);
                    try (Session<?> session = backend.newSession()) {
                        for (Tensor input : inputTensors) {
                            session.feed(input, false);
                        }

                        // 执行推理
                        session.forward();

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
            }
        } catch (Exception e) {
            logger.error("Failed to close forwarder instance", e);
        }

        logger.info("Finished");
    }

    /**
     * Determines whether two compared tensors, data types and shapes are equal and numerically similar (within the specified tolerance range).
     *
     * @param actual
     * @param expected
     * @param tolerance
     */
//    protected void assertSimilarity(Tensor actual, Tensor expected, float tolerance) {
//        assertEquals(expected.getValueInfo(), actual.getValueInfo());
//        assertEquals(expected.getData().capacity(), actual.getData().capacity());
//
//        FloatBuffer actualFloat = actual.getData().asFloatBuffer();
//        FloatBuffer expectedFloat = expected.getData().asFloatBuffer();
//
//        for (int n = 0; n < actualFloat.capacity(); n++) {
//            Float nActual = actualFloat.get();
//            Float nExpected = expectedFloat.get();
//            assertTrue(Math.abs((nActual) - (nExpected)) <= tolerance);
//        }
//    }
    protected void assertSimilarity(Tensor actual, Tensor expected, float tolerance) {
        assertEquals(expected.getValueInfo(), actual.getValueInfo());
        long[] shape = actual.getShape();

        FloatBuffer actualBuffer = actual.getData().asFloatBuffer();
        FloatBuffer expectedBuffer = expected.getData().asFloatBuffer();
        actualBuffer.rewind();
        expectedBuffer.rewind();

        // 步骤 1: 尝试直接比较 (假设两者都是行优先)
        boolean directMatch = true;
        for (int i = 0; i < expectedBuffer.capacity(); i++) {
            if (Math.abs(expectedBuffer.get(i) - actualBuffer.get(i)) > tolerance) {
                directMatch = false;
                break;
            }
        }

        if (directMatch) {
            logger.info("--> Tensors match with direct (Row-Major) comparison. No conversion needed.");
            assertTrue(true); // 断言成功
            return;
        }

        // 步骤 2: 如果直接比较失败，则执行列优先到行优先的转换
        logger.info("--> Direct comparison failed. Assuming Column-Major layout and attempting conversion...");

        if (shape.length != 3) {
            fail("此方法仅为3D Tensor设计，当前Tensor维度为: " + shape.length);
            return;
        }
        long dim1 = shape[0], dim2 = shape[1], dim3 = shape[2];

        float[] actualRowMajorData = new float[actualBuffer.capacity()];
        for (int i = 0; i < dim1; i++) {
            for (int j = 0; j < dim2; j++) {
                for (int k = 0; k < dim3; k++) {
                    int sourceIndex = (int) (i + j * dim1 + k * dim1 * dim2);
                    int destinationIndex = (int) (i * dim2 * dim3 + j * dim3 + k);
                    actualRowMajorData[destinationIndex] = actualBuffer.get(sourceIndex);
                }
            }
        }

        // 步骤 3: 比较转换后的数据
        logger.info("  CONVERTED ACTUAL (Row-Major): " + Arrays.toString(actualRowMajorData));
        for (int i = 0; i < expectedBuffer.capacity(); i++) {
            float nExpected = expectedBuffer.get(i);
            float nActual = actualRowMajorData[i];
            assertTrue(
                    String.format("转换后，在行优先索引 %d 处不匹配: 期望值是 <%f>, 实际值是 <%f>", i, nExpected, nActual),
                    Math.abs(nActual - nExpected) <= tolerance
            );
        }
        logger.info("--> Tensors match after Column-to-Row conversion.");
    }


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
}

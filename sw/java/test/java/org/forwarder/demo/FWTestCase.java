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
import org.onnx4j.Tensor;
import org.onnx4j.prototypes.OnnxProto3.TensorProto;
import org.onnx4j.tensor.TensorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import junit.framework.TestCase;

public abstract class FWTestCase extends TestCase {

    private static final int TENSOR_MAX_OUTPUT_LEN = 1000;

    private static Logger logger = LoggerFactory.getLogger(ForwarderTestCase.class);

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
        String absoluteModelPath = URLDecoder.decode(ForwarderTestCase.class.getResource(modelPath).getFile(), "utf-8");
        assertNotNull(absoluteModelPath);//判断非空

        try {
            //尝试构建Forwarder，加载model
            Config cfg=Config.builder()
                    .setDebug(true)
                    .setMemoryByteOrder(ByteOrder.LITTLE_ENDIAN)
                    .setExecutor(RayExecutor.class)
                    .build();
            Forwarder forwarder = new Forwarder();
            Model loadedModel=forwarder.load(absoluteModelPath,cfg).executor(RayExecutor.class);

            RayExecutor<?> executor = (RayExecutor<?>) loadedModel.getExecutor();
            executor.printExecutionSequence();

            assert forwarder != null;
            assert loadedModel!=null;
            //遍历待测试的所有输入
            for (Entry<List<String>, List<String>> tensorPairPath : tensorPairPaths.entrySet()) {
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

                        // 获取每个输出并比较
                        for (int i = 0; i < outputNames.size(); i++) {
                            Tensor actual = session.getOutput(outputNames.get(i));
                            Tensor expected = expectedOutputTensors.get(i);
                            this.assertSimilarity(actual, expected, tolerance);
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
     * @param excepted
     * @param tolerance
     */
    protected void assertSimilarity(Tensor actual, Tensor excepted, float tolerance) {
        assertEquals(excepted.getValueInfo(), actual.getValueInfo());
        assertEquals(excepted.getData().capacity(), actual.getData().capacity());

        FloatBuffer actualFloat = actual.getData().asFloatBuffer();
        FloatBuffer exceptedFloat = excepted.getData().asFloatBuffer();

        for (int n = 0; n < actualFloat.capacity(); n++) {
            Float nActual = actualFloat.get();
            Float nExcepted = exceptedFloat.get();
            assertTrue(Math.abs((nActual) - (nExcepted)) <= tolerance);
        }
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

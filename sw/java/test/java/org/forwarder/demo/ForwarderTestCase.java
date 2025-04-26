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

public abstract class ForwarderTestCase extends TestCase {

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
	public ForwarderTestCase(String testName) {
		super(testName);
	}

	protected void testModel(Map<String, String> tensorPairPaths, String modelPath, String inputName, String outputName,
			String[] backendNames, float tolerance) throws FileNotFoundException, IOException, NoSuchMethodException,
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
			for (Entry<String, String> tensorPairPath : tensorPairPaths.entrySet()) {
				Tensor inputTensor = this.loadTensor(loadedModel, inputName, tensorPairPath.getKey());
				logger.info("Loaded input tensor proto named \"{}\"", tensorPairPath.getKey());

				Tensor exceptedOutputTensor = this.loadTensor(loadedModel, inputName, tensorPairPath.getValue());
				logger.info("Loaded input tensor proto named \"{}\"", tensorPairPath.getValue());

				logger.info("Input Tensor: {}", this.dumpTensor(inputTensor));
				logger.info("Excepted Tensor: {}", this.dumpTensor(exceptedOutputTensor));
				//遍历待测试的所有后端
				for (String backendName : backendNames) {
					for (int n = 0; n < 50; n++) {
					Backend<?> backend = loadedModel.backend(backendName);
					//通过backend启动一个session
					try (Session<?> session = backend.newSession()) {
						//推理并获取输出
						Tensor y0 = session.feed(inputTensor, false).forward().getOutput(outputName);

						logger.info("Actual: {}", this.dumpTensor(y0));
						logger.info("Excepted: {}", this.dumpTensor(exceptedOutputTensor));
						//判断结果是否误差太大
						this.assertSimilarity(y0, exceptedOutputTensor, tolerance);
						}
					}
				}

				//inputTensor.close();
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

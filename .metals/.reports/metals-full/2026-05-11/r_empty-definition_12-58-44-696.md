error id: file://<WORKSPACE>/sw/java/main/java/org/forwarder/Session.java:_empty_/SharedMemoryPool#
file://<WORKSPACE>/sw/java/main/java/org/forwarder/Session.java
empty definition using pc, found symbol in pc: _empty_/SharedMemoryPool#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 2600
uri: file://<WORKSPACE>/sw/java/main/java/org/forwarder/Session.java
text:
```scala
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
package org.forwarder;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.forwarder.util.TensorUtils;
import org.forwarder.executor.Executor;
import org.onnx4j.Outputs;
import org.onnx4j.Outputs.Output;
import org.onnx4j.Tensor;
import org.onnx4j.TensorManager;
import org.onnx4j.SharedMemoryPool;
import org.onnx4j.model.graph.exchanges.GraphInput;
import org.onnx4j.model.graph.exchanges.GraphOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * ONNX模型推理会话抽象基类
 * 负责管理一次完整的模型推理过程：输入准备、前向计算、输出获取和资源清理
 * 实现了AutoCloseable接口，确保资源正确释放
 *
 * @param <T_BK_TS> 后端框架特有的张量类型（如ONNX Runtime的OrtTensor）
 */
public abstract class Session<T_BK_TS> implements AutoCloseable {
	// 线程本地变量，确保每个线程有自己独立的会话实例
	protected static final ThreadLocal<Session<?>> TL_SESSION = new ThreadLocal<Session<?>>();
	// 日志记录器示例
	private static Logger logger = LoggerFactory.getLogger(Session.class);

	/* 共享内存池配置 */
	private static SharedMemoryPool globalSharedMemoryPool = null;
	private static boolean sharedMemoryEnabled = false;
	private static final Object poolLock = new Object();

	protected Backend<T_BK_TS> backend;// 后端计算引擎
	protected Outputs outputs;// 模型输出结果集合
	protected Map<String, T_BK_TS> intermediateOutputs;// 中间计算结果缓存（名称->后端张量）
	protected TensorManager<T_BK_TS> intermediateTensorManager;// 中间张量管理器
	protected TensorManager<Tensor> exchangeTensorManager;// 交换张量管理器（前端Tensor）

	/**
	 * 初始化全局共享内存池
	 * 应在创建任何Session之前调用
	 */
	public static boolean initializeSharedMemoryPool() {
		synchronized (poolLock) {
			if (globalSharedMemoryPool != null) {
				return true;
			}
			globalSharedMemoryPool = SharedMemoryPool@@.getInstance();
			sharedMemoryEnabled = globalSharedMemoryPool.initialize();
			if (sharedMemoryEnabled) {
				logger.info("Shared memory pool initialized successfully");
			} else {
				logger.warn("Failed to initialize shared memory pool, falling back to regular allocation");
			}
			return sharedMemoryEnabled;
		}
	}

	/**
	 * 关闭全局共享内存池
	 */
	public static void shutdownSharedMemoryPool() {
		synchronized (poolLock) {
			if (globalSharedMemoryPool != null) {
				globalSharedMemoryPool.close();
				globalSharedMemoryPool = null;
				sharedMemoryEnabled = false;
				logger.info("Shared memory pool shutdown");
			}
		}
	}

	/**
	 * 获取共享内存池实例
	 */
	public static SharedMemoryPool getSharedMemoryPool() {
		return globalSharedMemoryPool;
	}

	/**
	 * 是否启用共享内存
	 */
	public static boolean isSharedMemoryEnabled() {
		return sharedMemoryEnabled && globalSharedMemoryPool != null && globalSharedMemoryPool.isInitialized();
	}

	/**
	 * 构造函数，初始化会话
	 *
	 * @param backend 后端计算引擎实例
	 */
	public Session(Backend<T_BK_TS> backend) {
		// 检查当前线程是否已存在会话，确保线程安全
		if (Session.TL_SESSION.get() != null)
			throw new RuntimeException("Session in this thread has been inited");

		// 将当前会话绑定到线程本地变量
		Session.TL_SESSION.set(this);

		this.backend = backend;
		this.intermediateOutputs = new HashMap<String, T_BK_TS>();
		this.outputs = new Outputs();

		// 初始化中间张量管理器，使用后端特定的释放方法
		this.intermediateTensorManager = new TensorManager<T_BK_TS>() {

			@Override
			protected void dispose(T_BK_TS tensor) {
				backend.disposeBackendTensor(tensor);
			}

		};

		// 初始化交换张量管理器，使用Tensor自身的close方法
		this.exchangeTensorManager = new TensorManager<Tensor>() {

			@Override
			protected void dispose(Tensor tensor) {
				tensor.close(); // 调用Tensor自身的close方法
			}

		};
		//初始化完成输出日志
		logger.debug("Session binded in thread \"{}\"", Thread.currentThread().getName());
	}

	//输入数据，从输入的tensor获取张量名，并autoattach
	public Session<T_BK_TS> feed(Tensor tensor) {
		return this.feed(tensor, true);
	}

	//输入数据，从输入的tensor获取张量名，根据输入决定是否autoAttach
	public Session<T_BK_TS> feed(Tensor tensor, boolean autoAttach) {
		if (tensor.getName() != null)
			return this.feed(tensor.getName(), tensor, autoAttach);
		else
			throw new IllegalArgumentException("The name of tensor can not be null.");
	}

	//输入数据，使用输入的张量名，并autoattach
	public Session<T_BK_TS> feed(String name, Tensor tensor) {
		return this.feed(name, tensor, true);
	}

	//输入数据，使用输入的张量名，根据输入决定是否autoattach，以上三个函数最后都是调用的这个函数
	public Session<T_BK_TS> feed(String name, Tensor tensor, boolean autoAttach) {
		//获取计算图的输入
		GraphInput graphInput = this.backend.getModel().getGraph().getInputs(name);
		if (graphInput == null) {
			throw new IllegalArgumentException(String.format("Input named \"%s\" had not be defined in graph", name));
		} else {
			//检查输入的tensor的dataType和shape是否和网络定义的一致
			if (!tensor.equals(graphInput.getValueInfo())) {
				throw new IllegalArgumentException(
						String.format("Shape or DataType is not equals to the input tensor named \"%s\" ", name));
			}
		}

		// 如果启用了共享内存且当前tensor不是共享内存模式，则转换
		Tensor feedTensor = tensor;
		if (isSharedMemoryEnabled() && !tensor.isSharedMemory()) {
			feedTensor = copyToSharedMemory(tensor);
			logger.debug("Copied input tensor '{}' to shared memory", name);
		}

		//转换为后端原生数据类型T_BK_TS
		T_BK_TS backendTensor = this.backend.toBackendTensor(this.intermediateTensorManager, feedTensor);
		//将输入作为中间结果存入一个map中，用name作为key
		this.intermediateOutputs.put(name, backendTensor);
		//默认会把输入的Tensor类型也存起来
		if (autoAttach) {
			this.exchangeTensorManager.attach(name, feedTensor);
		}

		return this;
	}

	/**
	 * 执行模型前向计算（推理）
	 *
	 * @return 当前会话实例
	 */
	public Session<T_BK_TS> forward() {
		//
		// Put all constant resources to session
		// 将后端常量张量全部存入当前会话的中间结果集
		//
		this.intermediateOutputs.putAll(this.backend.getTensorManager().get());

		//从后端获取执行器
		Executor<T_BK_TS> executor = this.backend.getModel().getExecutor();
		//递归执行推理
		executor.execute(this, this.backend.getOpsets());
	// 处理输出：将后端张量转换回前端Tensor并封装为输出结果
		for (GraphOutput graphOutput : this.backend.getModel().getGraph().getOutputs()) {
			T_BK_TS backendTensor = this.intermediateOutputs.get(graphOutput.getName());//从中间结果获取所有名字和网络需要的输出一致的张量
			Tensor tensor = this.backend.toNativeTensor(this.exchangeTensorManager, graphOutput.getName(),
					backendTensor);//转换回Tensor类型
			Output output = Output.wrap(graphOutput.getName(), tensor);//封装为输出对象并存入结果集
			outputs.append(graphOutput.getName(), output);
		}

		// 打印共享内存统计
		if (isSharedMemoryEnabled()) {
			globalSharedMemoryPool.printStatistics();
		}

		return this;
	}

	/**
	 * 将tensor复制到共享内存
	 */
	private Tensor copyToSharedMemory(Tensor source) {
		long sizeBytes = source.getMemoryBytes();
		SharedMemoryPool.Allocation alloc = globalSharedMemoryPool.allocate(sizeBytes);
		if (alloc == null) {
			logger.warn("Failed to allocate shared memory, using original tensor");
			return source;
		}

		// 创建共享内存tensor
		ByteBuffer sourceBuffer = source.getData();
		ByteBuffer targetBuffer = globalSharedMemoryPool.mapBuffer(alloc.blockId, alloc.offset, (int)sizeBytes);
		targetBuffer.put(sourceBuffer);
		targetBuffer.flip();

		return new Tensor(
			source.getName(),
			source.getDocString(),
			source.getDataType(),
			org.onnx4j.tensor.Shape.fromArray(source.getShape()),
			alloc.physicalAddress,
			alloc.blockId,
			globalSharedMemoryPool,
			alloc.offset
		);
	}

	//获取中间张量管理器
	public TensorManager<T_BK_TS> getTensorManager() {
		return intermediateTensorManager;
	}
	//获取后端计算引擎
	public Backend<T_BK_TS> getBackend() {
		return backend;
	}
	//按名称获取输出张量
	public Tensor getOutput(String name) {
		return this.outputs.getTensor(name);
	}
	//存储中间计算结果
	public void putIntermediateOutput(String name, T_BK_TS backendTensor) {
		this.intermediateTensorManager.attach(name, backendTensor);
		this.intermediateOutputs.put(name, backendTensor);
	}
	//按名称获取中间计算结果(T_BK_TS) 获取单个
	public T_BK_TS getIntermediateOutput(String name) {
		return this.intermediateOutputs.get(name);
	}

	// 获取所有张量
	public Map<String, T_BK_TS> getIntermediateOutputs() {
		return this.intermediateOutputs;
	}

	// 获取所有中间层输出
	public Map<String, Tensor> getAllIntermediateOutputTensors() {
		return this.intermediateOutputs.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> this.backend.toNativeTensor(this.exchangeTensorManager, entry.getKey(), entry.getValue())
				));
	}

	/**
     * 将中间张量从后端类型转换为前端 Tensor 类型。
     * @param name 中间张量的名称
     * @return 转换后的前端 Tensor
     */

	// 获取输出 用于最终计算结果算子
    public Tensor getIntermediateOutputTensor(String name) {
        // 从 intermediateOutputs 获取后端张量
        T_BK_TS backendTensor = this.intermediateOutputs.get(name);
        if (backendTensor == null) {
            throw new IllegalArgumentException("中间张量名称不存在: " + name);
        }

        // 使用 backend 将后端张量转换为前端 Tensor
        return this.backend.toNativeTensor(this.exchangeTensorManager, name, backendTensor);
    }

	//实现close接口，以满足AutoClosable类
	@Override
	public void close() throws Exception {
		this.intermediateTensorManager.close();
		this.exchangeTensorManager.close();

		Session.TL_SESSION.remove();
		logger.debug("Session closed");
	}

}

```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/SharedMemoryPool#
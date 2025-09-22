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
package org.onnx4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**

张量管理器抽象基类，用于管理所有张量类型的内存申请和释放

实现了AutoCloseable接口，支持资源自动释放

@param <T_TS> 管理的张量类型
**/

public abstract class TensorManager<T_TS> implements AutoCloseable {
//成员
	// 日志记录器
	private static Logger logger = LoggerFactory.getLogger(TensorManager.class);
	// 标记位，标记管理器是否已关闭
	private boolean hasClosed = false;
	// 存储张量的映射表，键为张量名称，值为张量对象
	private Map<String, T_TS> tensors = new HashMap<String, T_TS>();

//接口
	/**

	抽象方法：释放/处置单个张量资源

	需要子类实现具体的资源释放逻辑

	@param tensor 需要释放的张量对象
	*/
	protected abstract void dispose(T_TS tensor);

	
	
//函数
//增
	public void attach(String name, T_TS tensor) {
		this.tensors.put(name, tensor);
	}
//删
	public void detach(String name) {
		this.tensors.remove(name);
	}
//根据键值name获取哈希表里面的实体对象T_TS
	public T_TS get(String name) {
		return this.tensors.get(name);
	}
//获取一个无法被修改的哈希表副本
	public Map<String, T_TS> get() {
		return Collections.unmodifiableMap(this.tensors);
	}
//实现AutoCloseable接口的close方法
	@Override
	public void close() throws Exception {
		// 检查是否已经关闭
		if (this.hasClosed)
			throw new IllegalStateException("The TensorManager has closed.");
		// 遍历所有张量并逐个释放
		for (Entry<String, T_TS> entry : this.tensors.entrySet()) {
			try {
				this.dispose(entry.getValue());
				logger.info("Tensor[{}:{}] has been released.", entry.getValue().getClass().getName(),
						entry.getKey());
			} catch (Exception e) {
				logger.error("Tensor[{}:{}] can not be released.", entry.getValue().getClass().getName(),
						entry.getKey());
			}
		}
		// 清理资源
		this.tensors.clear();
		this.tensors = null;
		this.hasClosed = true;
	}
}

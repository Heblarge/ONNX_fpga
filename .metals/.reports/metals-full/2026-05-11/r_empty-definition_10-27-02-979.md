error id: file://<WORKSPACE>/sw/java/main/java/org/onnx4j/Tensor.java:java/nio/ByteOrder#nativeOrder().
file://<WORKSPACE>/sw/java/main/java/org/onnx4j/Tensor.java
empty definition using pc, found symbol in pc: java/nio/ByteOrder#nativeOrder().
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 1477
uri: file://<WORKSPACE>/sw/java/main/java/org/onnx4j/Tensor.java
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
package org.onnx4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.onnx4j.tensor.DataType;
import org.onnx4j.tensor.Shape;
import org.onnx4j.tensor.TensorDump;
import org.onnx4j.tensor.ValueInfo;
import org.onnx4j.utils.DirectBufferDealloc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tensor extends NamedOnnxObject implements AutoCloseable {

	private static Logger logger = LoggerFactory.getLogger(Tensor.class);

	public enum AllocationMode {

		DIRECT, HEAP, SHARED_MEMORY

	}

	public static class Options {

		private AllocationMode allocationMode = AllocationMode.DIRECT;

		private ByteOrder byteOrder = ByteOrder.nati@@veOrder();

		/* 共享内存相关参数 */
		private long sharedMemoryAddress = 0;      /* 共享内存物理地址 */
		private int sharedMemoryBlockId = -1;     /* 共享内存块ID (0-3) */
		private SharedMemoryPool pool = null;     /* 所属的内存池 */

		private Options() {
		}

		public AllocationMode getAllocationMode() {
			return allocationMode;
		}

		public Options setAllocationMode(AllocationMode allocationMode) {
			this.allocationMode = allocationMode;
			return this;
		}

		public ByteOrder getByteOrder() {
			return byteOrder;
		}

		public Options setByteOrder(ByteOrder byteOrder) {
			this.byteOrder = byteOrder;
			return this;
		}

		public long getSharedMemoryAddress() {
			return sharedMemoryAddress;
		}

		public Options setSharedMemoryAddress(long address) {
			this.sharedMemoryAddress = address;
			this.allocationMode = AllocationMode.SHARED_MEMORY;
			return this;
		}

		public int getSharedMemoryBlockId() {
			return sharedMemoryBlockId;
		}

		public Options setSharedMemoryBlockId(int blockId) {
			this.sharedMemoryBlockId = blockId;
			return this;
		}

		public SharedMemoryPool getPool() {
			return pool;
		}

		public Options setPool(SharedMemoryPool pool) {
			this.pool = pool;
			return this;
		}

	}

	public static Options options() {
		return new Options();
	}

	private ValueInfo valueInfo;
	private ByteBuffer dataBuffer;
	private AllocationMode allocationMode;
	private long sharedMemoryAddress;      /* 共享内存物理地址 */
	private int sharedMemoryBlockId;       /* 共享内存块ID */
	private SharedMemoryPool pool;         /* 所属的内存池 */
	private int sharedMemoryOffset;        /* 在共享内存块内的偏移 */

	/* 原有构造函数 */
	public Tensor(String name, String docString, DataType dataType, Shape shape, ByteBuffer dataBuffer) {
		super(name, docString);

		if (dataBuffer == null || dataBuffer.capacity() <= 0)
			throw new IllegalArgumentException("Databuffer is null or empty");

		this.name = name;
		this.valueInfo = new ValueInfo(dataType, shape);
		this.dataBuffer = dataBuffer;
		this.allocationMode = (dataBuffer.isDirect()) ? AllocationMode.DIRECT : AllocationMode.HEAP;
		this.sharedMemoryAddress = 0;
		this.sharedMemoryBlockId = -1;
		this.pool = null;
		this.sharedMemoryOffset = 0;
	}

	/* 共享内存模式构造函数 */
	public Tensor(String name, String docString, DataType dataType, Shape shape,
	             long sharedMemoryAddr, int blockId, SharedMemoryPool pool, int offset) {
		super(name, docString);

		this.name = name;
		this.valueInfo = new ValueInfo(dataType, shape);
		this.allocationMode = AllocationMode.SHARED_MEMORY;
		this.sharedMemoryAddress = sharedMemoryAddr;
		this.sharedMemoryBlockId = blockId;
		this.pool = pool;
		this.sharedMemoryOffset = offset;

		/* 通过JNI映射共享内存到ByteBuffer */
		this.dataBuffer = pool.mapBuffer(blockId, offset, (int)shape.getTotalSize() * dataType.getUnitSize());
		if (this.dataBuffer == null) {
			throw new IllegalArgumentException("Failed to map shared memory buffer");
		}
	}

	/* 使用Options创建Tensor */
	public Tensor(String name, String docString, DataType dataType, Shape shape, Options options) {
		super(name, docString);

		this.name = name;
		this.valueInfo = new ValueInfo(dataType, shape);
		this.allocationMode = options.getAllocationMode();
		this.sharedMemoryAddress = options.getSharedMemoryAddress();
		this.sharedMemoryBlockId = options.getSharedMemoryBlockId();
		this.pool = options.getPool();

		long requiredBytes = (long)shape.getTotalSize() * dataType.getUnitSize();

		if (this.allocationMode == AllocationMode.SHARED_MEMORY) {
			if (this.pool == null) {
				throw new IllegalArgumentException("Pool must be specified for SHARED_MEMORY mode");
			}
			/* 从内存池分配空间 */
			SharedMemoryPool.Allocation alloc = this.pool.allocate(requiredBytes);
			this.sharedMemoryBlockId = alloc.blockId;
			this.sharedMemoryOffset = alloc.offset;
			this.sharedMemoryAddress = alloc.physicalAddress;
			/* 映射到ByteBuffer */
			this.dataBuffer = this.pool.mapBuffer(alloc.blockId, alloc.offset, (int)requiredBytes);
			if (this.dataBuffer == null) {
				throw new IllegalArgumentException("Failed to map shared memory buffer");
			}
		} else {
			/* DIRECT或HEAP模式，创建新的ByteBuffer */
			this.sharedMemoryBlockId = -1;
			this.sharedMemoryOffset = 0;
			if (this.allocationMode == AllocationMode.DIRECT) {
				this.dataBuffer = ByteBuffer.allocateDirect((int)requiredBytes);
			} else {
				this.dataBuffer = ByteBuffer.allocate((int)requiredBytes);
			}
		}
		this.dataBuffer.order(options.getByteOrder());
	}

	public String getName() {
		return name;
	}

	/**
	 * 获取Tensor数据部分(ByteBuffer)的内存占用量
	 *
	 * @return 占用字节数
	 */
	public long getMemoryBytes() {
		if (this.dataBuffer != null)
			return this.dataBuffer.capacity();

		return -1L;
	}

	public ValueInfo getValueInfo() {
		return valueInfo;
	}

	/**
	 * 获取元素总数量
	 *
	 * @return 元素总数量
	 */
	public long getElementSize() {
		return this.getMemoryBytes() / this.valueInfo.getDataType().getUnitSize();
	}

	public DataType getDataType() {
		return this.valueInfo.getDataType();
	}

	public int getRanks() {
		return this.valueInfo.getRank();
	}

	/**
	 * 返回当前数据缓存的只读引用
	 *
	 * @return
	 */
	public ByteBuffer getData() {
		return this.dataBuffer.slice().asReadOnlyBuffer().order(this.dataBuffer.order());
	}

	public long[] getShape() {
		return this.valueInfo.getShape().toArray();
	}

	public boolean equalsIn(DataType[] constrainTypes) {
		for (DataType dataType : constrainTypes) {
			if (dataType.equals(this.valueInfo.getDataType()))
				return true;
		}
		return false;
	}

	public boolean equals(ValueInfo valueInfo) {
		return this.valueInfo.equals(valueInfo);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj.getClass().isInstance(Tensor.class) == false)
			return false;

		if (this.equals(((Tensor) obj).valueInfo) == false)
			return false;

		return this.dataBuffer.equals(((Tensor) obj).dataBuffer);
	}

	/* ========== 共享内存相关方法 ========== */

	/**
	 * 是否为共享内存模式
	 */
	public boolean isSharedMemory() {
		return this.allocationMode == AllocationMode.SHARED_MEMORY;
	}

	/**
	 * 获取分配模式
	 */
	public AllocationMode getAllocationMode() {
		return this.allocationMode;
	}

	/**
	 * 获取共享内存物理地址
	 */
	public long getSharedMemoryAddress() {
		return this.sharedMemoryAddress;
	}

	/**
	 * 获取共享内存块ID
	 */
	public int getSharedMemoryBlockId() {
		return this.sharedMemoryBlockId;
	}

	/**
	 * 获取在共享内存块内的偏移
	 */
	public int getSharedMemoryOffset() {
		return this.sharedMemoryOffset;
	}

	/**
	 * 获取物理地址信息，用于传递给FPGA
	 * 返回格式: [blockId, offset, length]
	 */
	public long[] getPhysicalAddressInfo() {
		if (!isSharedMemory()) {
			return null;
		}
		return new long[] {
			this.sharedMemoryBlockId,
			this.sharedMemoryOffset,
			this.getMemoryBytes()
		};
	}

	@Override
	public void close() {
		if (this.dataBuffer != null) {
			if (this.allocationMode == AllocationMode.SHARED_MEMORY) {
				/* 共享内存模式：归还到内存池 */
				if (this.pool != null) {
					this.pool.free(this.sharedMemoryBlockId, this.sharedMemoryOffset);
				}
				/* 取消JNI映射 */
				this.pool.unmapBuffer(this.sharedMemoryBlockId, this.dataBuffer);
			} else if (this.allocationMode == AllocationMode.DIRECT) {
				/* DIRECT模式：释放direct buffer */
				if (!DirectBufferDealloc.deallocateDirectBuffer(this.dataBuffer))
					throw new RuntimeException(String.format("[Tensor:%s] can not be released.", this.name));
			}
			/* HEAP模式由GC自动管理 */

			this.dataBuffer = null;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Tensor[name=").append(name);
		sb.append(", mode=").append(allocationMode);
		if (isSharedMemory()) {
			sb.append(", block=").append(sharedMemoryBlockId);
			sb.append(", offset=0x").append(Integer.toHexString(sharedMemoryOffset));
		}
		sb.append(", shape=").append(valueInfo.getShape());
		sb.append(", dataType=").append(valueInfo.getDataType());
		sb.append("]");
		return sb.toString();
	}

	/**
	 * 获取完整的dump信息
	 */
	public String toDetailedString() {
		return TensorDump.dump(this);
	}
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: java/nio/ByteOrder#nativeOrder().
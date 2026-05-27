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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicLong;
import java.util.HashMap;
import java.util.Map;

/**
 * 共享内存池管理器
 *
 * 管理多个预分配的共享内存块，用于A53-R5-FPGA之间的高效数据传递
 *
 * 内存布局 (需与rsc_table.c和设备树保持一致):
 * - Block 0: 0x3F100000, 10MB
 * - Block 1: 0x3FB00000, 10MB
 * - Block 2: 0x40500000, 10MB
 * - Block 3: 0x40F00000, 10MB
 */
public class SharedMemoryPool implements AutoCloseable {

	private static Logger logger = LoggerFactory.getLogger(SharedMemoryPool.class);

	/**
	 * 单个内存块的信息
	 */
	public static class MemoryBlock {
		public final int blockId;
		public final long physicalAddress;  /* 物理地址 */
		public final int size;              /* 块大小 */
		public final ByteBuffer mappedBuffer; /* 映射的ByteBuffer */
		public final AtomicLong usedBytes;  /* 已使用字节数 */

		public MemoryBlock(int blockId, long physicalAddress, int size, ByteBuffer mappedBuffer) {
			this.blockId = blockId;
			this.physicalAddress = physicalAddress;
			this.size = size;
			this.mappedBuffer = mappedBuffer;
			this.usedBytes = new AtomicLong(0);
		}

		public int getFreeBytes() {
			return size - (int)usedBytes.get();
		}

		public double getUsageRatio() {
			return (double)usedBytes.get() / size;
		}
	}

	/**
	 * 内存分配结果
	 */
	public static class Allocation {
		public final int blockId;
		public final int offset;
		public final long physicalAddress;
		public final int size;

		public Allocation(int blockId, int offset, long physicalAddress, int size) {
			this.blockId = blockId;
			this.offset = offset;
			this.physicalAddress = physicalAddress;
			this.size = size;
		}
	}

	/* ========== JNI 方法声明 ========== */
	static {
		try {
			System.loadLibrary("accelerator_jni");
		} catch (UnsatisfiedLinkError e) {
			logger.warn("Cannot load accelerator_jni library: {}", e.getMessage());
		}
	}

	/**
	 * 初始化共享内存池映射
	 * @param addresses 物理地址数组
	 * @param sizes 每个块的大小数组
	 * @return 成功返回true
	 */
	private native boolean nativeInitialize(long[] addresses, int[] sizes);

	/**
	 * 映射单个内存块到ByteBuffer
	 * @param blockId 块ID
	 * @param size 映射大小
	 * @return 映射的ByteBuffer的native地址
	 */
	private native long nativeMapBuffer(int blockId, int size);

	/**
	 * 取消映射
	 * @param blockId 块ID
	 * @param bufferAddress Buffer地址
	 */
	private native void nativeUnmapBuffer(int blockId, long bufferAddress);

	/**
	 * 关闭共享内存池
	 */
	private native void nativeShutdown();

	/* ========== Java 层实现 ========== */

	private static SharedMemoryPool instance;
	private final MemoryBlock[] blocks;
	private final Map<Integer, ByteBuffer> sliceBuffers = new HashMap<>();
	private boolean initialized = false;

	/* Block 池状态管理 - 简化设计：直接使用4个block */
	private final boolean[] blockUsed = new boolean[4];  /* 跟踪每个block的使用状态 */
	private final Object blockLock = new Object();  /* block分配同步锁 */

	/* 默认配置 - 需与rsc_table.c保持一致 */
	private static final long[] DEFAULT_ADDRESSES = {
		0x3F100000L,  /* Block 0 */
		0x3FB00000L,  /* Block 1 */
		0x40500000L,  /* Block 2 */
		0x40F00000L   /* Block 3 */
	};

	private static final int[] DEFAULT_SIZES = {
		0x00A00000,   /* 10MB */
		0x00A00000,   /* 10MB */
		0x00A00000,   /* 10MB */
		0x00A00000    /* 10MB */
	};

	private SharedMemoryPool() {
		this.blocks = new MemoryBlock[DEFAULT_ADDRESSES.length];
	}

	/**
	 * 获取单例实例
	 */
	public static SharedMemoryPool getInstance() {
		if (instance == null) {
			synchronized (SharedMemoryPool.class) {
				if (instance == null) {
					instance = new SharedMemoryPool();
				}
			}
		}
		return instance;
	}

	/**
	 * 初始化共享内存池
	 * 复用 HWAcceleratorJNI 的共享内存映射
	 */
	public synchronized boolean initialize() {
		if (initialized) {
			return true;
		}

		/* 使用 HWAcceleratorJNI 的共享内存映射 */
		org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratorJNI jni =
			org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratorJNI.getInstance();

		if (!jni.initialize()) {
			logger.error("Failed to initialize HWAcceleratorJNI");
			return false;
		}

		if (!jni.initializeSharedMemory()) {
			logger.error("Failed to initialize shared memory via HWAcceleratorJNI");
			return false;
		}

		/* 获取每个块的ByteBuffer映射 */
		for (int i = 0; i < DEFAULT_ADDRESSES.length; i++) {
			long nativeAddr = jni.getSharedMemoryMmapAddress(i);
			if (nativeAddr == 0) {
				logger.error("Failed to get mmap address for block {}", i);
				return false;
			}

			/* 创建Direct ByteBuffer包装native地址 */
			ByteBuffer buffer = createByteBufferFromNative(nativeAddr, DEFAULT_SIZES[i]);
			buffer.order(ByteOrder.nativeOrder());

			blocks[i] = new MemoryBlock(i, DEFAULT_ADDRESSES[i], DEFAULT_SIZES[i], buffer);
		}

		initialized = true;
		logger.info("Shared memory pool initialized with {} blocks (using HWAcceleratorJNI)", blocks.length);
		for (MemoryBlock block : blocks) {
			logger.info("  Block {}: PA=0x{:X}, Size={}MB",
				block.blockId, block.physicalAddress, block.size / (1024 * 1024));
		}

		return true;
	}

	/**
	 * 分配内存 - 简单首次适应算法
	 */
	public Allocation allocate(long size) {
		if (!initialized) {
			throw new IllegalStateException("Shared memory pool not initialized");
		}

		/* 对齐到64字节边界 */
		int alignedSize = (int)((size + 63) & ~63);

		for (MemoryBlock block : blocks) {
			int currentUsed = (int)block.usedBytes.get();
			if (currentUsed + alignedSize <= block.size) {
				/* 原子分配 */
				int newUsed = (int)block.usedBytes.addAndGet(alignedSize);
				if (newUsed <= block.size) {
					int offset = newUsed - alignedSize;
					long physicalAddr = block.physicalAddress + offset;
					logger.debug("Allocated {} bytes in block {} at offset 0x{:X}",
						alignedSize, block.blockId, offset);
					return new Allocation(block.blockId, offset, physicalAddr, alignedSize);
				} else {
					/* 分配失败，回滚 */
					block.usedBytes.addAndGet(-alignedSize);
				}
			}
		}

		logger.error("Failed to allocate {} bytes from shared memory pool", size);
		printStatistics();
		return null;
	}

	/**
	 * 释放内存 - 简单实现：仅标记，不立即合并
	 * TODO: 实现真正的空闲链表管理
	 */
	public void free(int blockId, int offset) {
		if (!initialized) {
			return;
		}

		logger.warn("Free called for block {} offset 0x{:X} - current implementation does not track individual allocations",
			blockId, offset);
		/* 当前简化实现：仅在allocate时累加，不支持单独释放 */
		/* 完整实现需要维护空闲块链表 */
	}

	/* ========== Block 池管理方法 ========== */

	/**
	 * Block信息：直接使用block作为完整的数据区
	 */
	public static class BlockInfo {
		public final int blockId;
		public final long physicalAddress;
		public final int size;

		public BlockInfo(int blockId, long physicalAddress, int size) {
			this.blockId = blockId;
			this.physicalAddress = physicalAddress;
			this.size = size;
		}
	}

	/**
	 * 获取指定 blockId 的信息
	 */
	public BlockInfo getBlockInfo(int blockId) {
		if (!initialized) {
			throw new IllegalStateException("Shared memory pool not initialized");
		}
		if (blockId < 0 || blockId >= 4) {
			throw new IllegalArgumentException("Invalid blockId: " + blockId + ", must be 0-3");
		}

		return new BlockInfo(blockId, DEFAULT_ADDRESSES[blockId], DEFAULT_SIZES[blockId]);
	}

	/**
	 * 按blockId分配block
	 * @param blockId block ID (0-3)
	 * @return BlockInfo 对象，包含分配信息
	 * @throws IllegalStateException 如果 block 已被占用
	 */
	public BlockInfo allocateBlock(int blockId) {
		if (!initialized) {
			throw new IllegalStateException("Shared memory pool not initialized");
		}
		if (blockId < 0 || blockId >= 4) {
			throw new IllegalArgumentException("Invalid blockId: " + blockId);
		}

		synchronized (blockLock) {
			if (blockUsed[blockId]) {
				throw new IllegalStateException("Block " + blockId + " is already in use");
			}
			blockUsed[blockId] = true;
		}

		BlockInfo info = getBlockInfo(blockId);
		logger.debug("Allocated block {}: PA=0x{:X}, Size={}MB",
			blockId, info.physicalAddress, info.size / (1024 * 1024));
		return info;
	}

	/**
	 * 释放block
	 * @param blockId block ID (0-3)
	 */
	public void freeBlock(int blockId) {
		if (blockId < 0 || blockId >= 4) {
			throw new IllegalArgumentException("Invalid blockId: " + blockId);
		}

		synchronized (blockLock) {
			if (!blockUsed[blockId]) {
				logger.warn("Block {} was not allocated", blockId);
				return;
			}
			blockUsed[blockId] = false;
		}

		logger.debug("Freed block {}", blockId);
	}

	/**
	 * 映射指定 buffer 到 ByteBuffer
	 * @param bufferId buffer ID (0-63)
	 * @param size 映射大小
	 * @return 映射的 ByteBuffer
	 */
	public ByteBuffer mapBuffer(int blockId, int size) {
		BufferInfo info = getBufferInfo(bufferId);
		return mapBuffer(info.blockId, info.offsetInBlock, size);
	}

	/**
	 * 检查 buffer 是否可用
	 */
	public boolean isBufferFree(int blockId) {
		if (bufferId < 0 || bufferId >= 4) {
			return false;
		}
		synchronized (bufferLock) {
			return !bufferUsed[bufferId];
		}
	}

	/**
	 * 获取下一个可用的 buffer ID
	 * @param startId 开始搜索的 buffer ID
	 * @return 可用的 buffer ID，如果没有则返回 -1
	 */
	public int findNextFreeBuffer(int startId) {
		for (int i = 0; i < 4; i++) {
			int blockId = (startId + i) % 4;
			if (isBufferFree(bufferId)) {
				return bufferId;
			}
		}
		return -1;  /* 所有 buffer 都被占用 */
	}

	/**
	 * 映射buffer切片
	 */
	public ByteBuffer mapBuffer(int blockId, int offset, int size) {
		if (!initialized) {
			throw new IllegalStateException("Shared memory pool not initialized");
		}

		if (blockId < 0 || blockId >= blocks.length) {
			throw new IllegalArgumentException("Invalid block ID: " + blockId);
		}

		MemoryBlock block = blocks[blockId];
		if (offset + size > block.size) {
			throw new IllegalArgumentException("Buffer slice out of bounds");
		}

		/* 创建原buffer的切片视图 */
		synchronized (block.mappedBuffer) {
			block.mappedBuffer.position(offset);
			ByteBuffer slice = block.mappedBuffer.slice();
			slice.limit(size);
			slice.order(block.mappedBuffer.order());
			return slice;
		}
	}

	/**
	 * 取消映射
	 */
	public void unmapBuffer(int blockId, ByteBuffer buffer) {
		/* 切片视图不需要显式取消映射，原buffer会统一管理 */
		synchronized (sliceBuffers) {
			sliceBuffers.remove(blockId);
		}
	}

	/**
	 * 获取物理地址
	 */
	public long getPhysicalAddress(int blockId, int offset) {
		if (!initialized || blockId < 0 || blockId >= blocks.length) {
			return 0;
		}
		return blocks[blockId].physicalAddress + offset;
	}

	/**
	 * 打印统计信息
	 */
	public void printStatistics() {
		if (!initialized) {
			logger.info("Shared memory pool not initialized");
			return;
		}

		logger.info("=== Shared Memory Pool Statistics ===");
		long totalUsed = 0;
		long totalFree = 0;

		for (MemoryBlock block : blocks) {
			long used = block.usedBytes.get();
			long free = block.getFreeBytes();
			totalUsed += used;
			totalFree += free;

			logger.info("Block {}: PA=0x{:X}, Used={}KB ({}%), Free={}KB",
				block.blockId, block.physicalAddress,
				used / 1024, (int)(block.getUsageRatio() * 100),
				free / 1024);
		}

		logger.info("Total: Used={}MB, Free={}MB, Usage={}%",
			totalUsed / (1024 * 1024), totalFree / (1024 * 1024),
			(int)((double)totalUsed / (totalUsed + totalFree) * 100));
	}

	/**
	 * 重置内存池（清空所有分配）
	 */
	public synchronized void reset() {
		if (!initialized) {
			return;
		}

		for (MemoryBlock block : blocks) {
			block.usedBytes.set(0);
		}
		logger.info("Shared memory pool reset");
	}

	@Override
	public synchronized void close() {
		if (initialized) {
			/* HWAcceleratorJNI 管理共享内存生命周期，这里不需要单独关闭 */
			for (int i = 0; i < blocks.length; i++) {
				blocks[i] = null;
			}
			initialized = false;
			logger.info("Shared memory pool closed");
		}
	}

	/* ========== 辅助方法 ========== */

	/**
	 * 从native地址创建DirectByteBuffer
	 * 使用反射访问私有构造函数
	 */
	private ByteBuffer createByteBufferFromNative(long address, int size) {
		try {
			Class<?> cls = Class.forName("java.nio.DirectByteBuffer");
			java.lang.reflect.Constructor<?> ctor = cls.getDeclaredConstructor(
				long.class, int.class);
			ctor.setAccessible(true);
			return (ByteBuffer)ctor.newInstance(address, size);
		} catch (Exception e) {
			logger.error("Failed to create DirectByteBuffer from native address", e);
			return null;
		}
	}

	public boolean isInitialized() {
		return initialized;
	}

	public int getBlockCount() {
		return blocks.length;
	}

	public MemoryBlock getBlock(int blockId) {
		if (blockId >= 0 && blockId < blocks.length) {
			return blocks[blockId];
		}
		return null;
	}
}

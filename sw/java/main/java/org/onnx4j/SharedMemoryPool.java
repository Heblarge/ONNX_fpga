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

	/* Buffer 池状态管理 */
	private final boolean[] bufferUsed = new boolean[TOTAL_BUFFERS];  /* 跟踪每个buffer的使用状态 */
	private final Object bufferLock = new Object();  /* buffer分配同步锁 */

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

	/* 固定 Buffer 池配置 - 需与 rpmsg-accelerator.c 保持一致 */
	private static final int BUFFERS_PER_BLOCK = 16;
	private static final int BUFFER_MAX_SIZE = 640 * 1024;  /* 640KB per buffer */
	private static final int TOTAL_BUFFERS = 4 * BUFFERS_PER_BLOCK;  /* 64个buffer */

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

	/* ========== 固定 Buffer 池管理方法 ========== */

	/**
	 * 根据 bufferId 计算其在固定池中的物理地址和偏移
	 * 与 rpmsg-accelerator.c 的 g_buffer_pool 保持一致
	 */
	public static class BufferInfo {
		public final int bufferId;
		public final int blockId;
		public final int offsetInBlock;
		public final long physicalAddress;
		public final int size;

		public BufferInfo(int bufferId, int blockId, int offsetInBlock, long physicalAddress, int size) {
			this.bufferId = bufferId;
			this.blockId = blockId;
			this.offsetInBlock = offsetInBlock;
			this.physicalAddress = physicalAddress;
			this.size = size;
		}
	}

	/**
	 * 获取指定 bufferId 的固定地址信息
	 * 与 rpmsg-accelerator.c 的 g_buffer_pool 完全对齐
	 */
	public BufferInfo getBufferInfo(int bufferId) {
		if (!initialized) {
			throw new IllegalStateException("Shared memory pool not initialized");
		}
		if (bufferId < 0 || bufferId >= TOTAL_BUFFERS) {
			throw new IllegalArgumentException("Invalid bufferId: " + bufferId + ", must be 0-" + (TOTAL_BUFFERS - 1));
		}

		int blockId = bufferId / BUFFERS_PER_BLOCK;
		int bufferInBlock = bufferId % BUFFERS_PER_BLOCK;
		int offsetInBlock = bufferInBlock * BUFFER_MAX_SIZE;
		long physicalAddr = DEFAULT_ADDRESSES[blockId] + offsetInBlock;

		return new BufferInfo(bufferId, blockId, offsetInBlock, physicalAddr, BUFFER_MAX_SIZE);
	}

	/**
	 * 按固定 bufferId 分配 buffer（与 R5 侧 buffer 池对齐）
	 * @param bufferId buffer ID (0-63)
	 * @return BufferInfo 对象，包含分配信息
	 * @throws IllegalStateException 如果 buffer 已被占用
	 */
	public BufferInfo allocateBuffer(int bufferId) {
		if (!initialized) {
			throw new IllegalStateException("Shared memory pool not initialized");
		}
		if (bufferId < 0 || bufferId >= TOTAL_BUFFERS) {
			throw new IllegalArgumentException("Invalid bufferId: " + bufferId);
		}

		synchronized (bufferLock) {
			if (bufferUsed[bufferId]) {
				throw new IllegalStateException("Buffer " + bufferId + " is already in use");
			}
			bufferUsed[bufferId] = true;
		}

		BufferInfo info = getBufferInfo(bufferId);
		logger.debug("Allocated buffer {}: block={}, offset=0x{:X}, PA=0x{:X}",
			bufferId, info.blockId, info.offsetInBlock, info.physicalAddress);
		return info;
	}

	/**
	 * 释放固定 buffer
	 * @param bufferId buffer ID (0-63)
	 */
	public void freeBuffer(int bufferId) {
		if (bufferId < 0 || bufferId >= TOTAL_BUFFERS) {
			throw new IllegalArgumentException("Invalid bufferId: " + bufferId);
		}

		synchronized (bufferLock) {
			if (!bufferUsed[bufferId]) {
				logger.warn("Buffer {} was not allocated", bufferId);
				return;
			}
			bufferUsed[bufferId] = false;
		}

		logger.debug("Freed buffer {}", bufferId);
	}

	/**
	 * 映射指定 buffer 到 ByteBuffer
	 * @param bufferId buffer ID (0-63)
	 * @param size 映射大小
	 * @return 映射的 ByteBuffer
	 */
	public ByteBuffer mapBuffer(int bufferId, int size) {
		BufferInfo info = getBufferInfo(bufferId);
		return mapBuffer(info.blockId, info.offsetInBlock, size);
	}

	/**
	 * 检查 buffer 是否可用
	 */
	public boolean isBufferFree(int bufferId) {
		if (bufferId < 0 || bufferId >= TOTAL_BUFFERS) {
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
		for (int i = 0; i < TOTAL_BUFFERS; i++) {
			int bufferId = (startId + i) % TOTAL_BUFFERS;
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

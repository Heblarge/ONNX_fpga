package org.forwarder.backend.impls.HWAccelerated.utils;

import Accelerator.InstJavaTODO;

/**
 * JNI 接口 - 用于与 R5 核和 FPGA 加速器通信
 *
 * 使用 OpenAMP RPMsg 进行通信：
 * - rpmsg_send() 直接发送指令，内核自动处理 vring 更新和 IPI 触发
 * - 共享内存池用于高效的数据传递，避免数据拷贝
 */
public class HWAcceleratorJNI {

	// RPMsg 设备名 - 可通过环境变量或系统属性覆盖
	private static final String RPMSG_DEVICE = System.getProperty("hwaccelerator.rpmsg.device",
			System.getenv().getOrDefault("RPMSG_DEVICE", "virtio0.rpmsg-openamp-demo-channel.-1.1024"));

	private static final int DEFAULT_TIMEOUT_MS = 5000;

	private static HWAcceleratorJNI instance;
	private boolean initialized = false;

	/* 共享内存池配置 - 需与rsc_table.c保持一致 */
	private static final long[] SHARED_MEM_ADDRESSES = {
		0x3F100000L,  /* Block 0: 10MB */
		0x3FB00000L,  /* Block 1: 10MB */
		0x40500000L,  /* Block 2: 10MB */
		0x40F00000L   /* Block 3: 10MB */
	};

	private static final int[] SHARED_MEM_SIZES = {
		0x00A00000,   /* 10MB */
		0x00A00000,   /* 10MB */
		0x00A00000,   /* 10MB */
		0x00A00000    /* 10MB */
	};

	/* 共享内存相关状态 */
	private long[] sharedMemMmapAddresses;  /* mmap后的虚拟地址 */

	static {
		try {
			System.loadLibrary("accelerator_jni");
		} catch (UnsatisfiedLinkError e) {
			System.err.println("Warning: Cannot load accelerator_jni library: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private HWAcceleratorJNI() {
		sharedMemMmapAddresses = new long[SHARED_MEM_ADDRESSES.length];
	}

	public static HWAcceleratorJNI getInstance() {
		if (instance == null) {
			synchronized (HWAcceleratorJNI.class) {
				if (instance == null) {
					instance = new HWAcceleratorJNI();
				}
			}
		}
		return instance;
	}

	// ========== JNI 方法声明 ==========

	/**
	 * 初始化 JNI 层 (打开 RPMsg 设备)
	 */
	private native boolean nativeInitialize(String rpmsgDevice);

	/**
	 * 初始化共享内存池映射
	 * @param physAddresses 物理地址数组
	 * @param sizes 每个块的大小数组
	 * @return 成功返回true
	 */
	private native boolean nativeInitializeSharedMemory(long[] physAddresses, int[] sizes);

	/**
	 * 映射单个共享内存块到ByteBuffer
	 * @param blockId 块ID (0-3)
	 * @param size 映射大小
	 * @return 映射后的虚拟地址，失败返回0
	 */
	private native long nativeMapSharedMemoryBlock(int blockId, int size);

	/**
	 * 取消映射共享内存块
	 * @param blockId 块ID
	 * @param mmapAddr mmap返回的虚拟地址
	 */
	private native void nativeUnmapSharedMemoryBlock(int blockId, long mmapAddr);

	/**
	 * 同步共享内存到设备 (确保CPU写入对设备可见)
	 * @param blockId 块ID
	 * @param offset 偏移
	 * @param size 大小
	 */
	private native void nativeSyncSharedMemoryToDevice(int blockId, int offset, int size);

	/**
	 * 同步设备到共享内存 (确保设备写入对CPU可见)
	 * @param blockId 块ID
	 * @param offset 偏移
	 * @param size 大小
	 */
	private native void nativeSyncSharedMemoryFromDevice(int blockId, int offset, int size);

	/**
	 * 关闭 JNI 层
	 */
	private native void nativeShutdown();

	/**
	 * 关闭共享内存映射
	 */
	private native void nativeShutdownSharedMemory();

	/**
	 * 通过 RPMsg 发送指令到 R5
	 */
	private native boolean nativeSendInstructions(InstJavaTODO[] instructions, int count);

	/**
	 * 等待 R5/FPGA 完成计算
	 * @deprecated 使用 waitForBufferCompletion 代替，直接检查缓冲区状态
	 */
	@Deprecated
	private native boolean nativeWaitForCompletion(int timeoutMs);

	/**
	 * 等待指定缓冲区完成处理（基于共享内存状态标志）
	 * 直接检查缓冲区状态，无需轮询 RPMsg
	 * @param bufferId 缓冲区ID (0-63)
	 * @param timeoutMs 超时时间（毫秒）
	 * @return 成功返回true
	 */
	private native boolean nativeWaitForBufferCompletion(int bufferId, int timeoutMs);

	/**
	 * 读取计算结果
	 */
	private native byte[] nativeReadResult(int offset, int length);

	/**
	 * 获取当前状态
	 */
	private native int nativeGetStatus();

	/**
	 * 直接写入 DDR 地址 (用于初始化数据)
	 */
	private native boolean nativeWriteDDR(long address, byte[] data, int length);

	/**
	 * 直接读取 DDR 地址
	 */
	private native byte[] nativeReadDDR(long address, int length);

	// ========== Java 包装方法 ==========

	/**
	 * 初始化 JNI 接口
	 */
	public boolean initialize() {
		if (initialized) {
			return true;
		}

		System.out.println("[JNI] Initializing RPMsg: " + RPMSG_DEVICE);
		boolean success = nativeInitialize(RPMSG_DEVICE);
		if (success) {
			initialized = true;
			System.out.println("[JNI] HWAcceleratorJNI initialized successfully");
		} else {
			System.err.println("[JNI] Failed to initialize HWAcceleratorJNI");
		}
		return success;
	}

	/**
	 * 初始化共享内存池
	 */
	public boolean initializeSharedMemory() {
		if (!initialized) {
			System.err.println("HWAcceleratorJNI not initialized, call initialize() first");
			return false;
		}

		boolean success = nativeInitializeSharedMemory(SHARED_MEM_ADDRESSES, SHARED_MEM_SIZES);
		if (success) {
			System.out.println("Shared memory pool initialized successfully");
			for (int i = 0; i < SHARED_MEM_ADDRESSES.length; i++) {
				long addr = nativeMapSharedMemoryBlock(i, SHARED_MEM_SIZES[i]);
				if (addr == 0) {
					System.err.println("Failed to map shared memory block " + i);
					nativeShutdownSharedMemory();
					return false;
				}
				sharedMemMmapAddresses[i] = addr;
				System.out.println("  Block " + i + ": PA=0x" + Long.toHexString(SHARED_MEM_ADDRESSES[i]) +
					", VA=0x" + Long.toHexString(addr) + ", Size=" + (SHARED_MEM_SIZES[i] / 1024 / 1024) + "MB");
			}
		} else {
			System.err.println("Failed to initialize shared memory pool");
		}
		return success;
	}

	/**
	 * 获取共享内存块的mmap地址
	 */
	public long getSharedMemoryMmapAddress(int blockId) {
		if (blockId >= 0 && blockId < sharedMemMmapAddresses.length) {
			return sharedMemMmapAddresses[blockId];
		}
		return 0;
	}

	/**
	 * 同步共享内存到设备
	 */
	public void syncToDevice(int blockId, int offset, int size) {
		nativeSyncSharedMemoryToDevice(blockId, offset, size);
	}

	/**
	 * 同步设备到共享内存
	 */
	public void syncFromDevice(int blockId, int offset, int size) {
		nativeSyncSharedMemoryFromDevice(blockId, offset, size);
	}

	/**
	 * 关闭 JNI 接口
	 */
	public void shutdown() {
		if (initialized) {
			nativeShutdownSharedMemory();
			nativeShutdown();
			initialized = false;
			for (int i = 0; i < sharedMemMmapAddresses.length; i++) {
				sharedMemMmapAddresses[i] = 0;
			}
			System.out.println("HWAcceleratorJNI shutdown");
		}
	}

	/**
	 * 发送指令并等待完成
	 */
	public boolean executeInstructions(InstJavaTODO[] instructions) {
		return executeInstructions(instructions, DEFAULT_TIMEOUT_MS);
	}

	/**
	 * 发送指令并等待完成 (带超时)
	 */
	public boolean executeInstructions(InstJavaTODO[] instructions, int timeoutMs) {
		if (!initialized) {
			throw new IllegalStateException("HWAcceleratorJNI not initialized");
		}

		if (instructions == null || instructions.length == 0) {
			System.err.println("No instructions to execute");
			return false;
		}

		System.out.println("Sending " + instructions.length + " instructions to FPGA...");

		// 发送指令
		if (!nativeSendInstructions(instructions, instructions.length)) {
			System.err.println("Failed to send instructions");
			return false;
		}

		// 等待完成
		if (!nativeWaitForCompletion(timeoutMs)) {
			System.err.println("Timeout waiting for FPGA completion");
			return false;
		}

		System.out.println("FPGA execution completed successfully");
		return true;
	}

	/**
	 * 发送指令并等待指定输出缓冲区完成
	 * 使用缓冲区状态标志同步，比轮询 RPMsg 更高效
	 */
	public boolean executeInstructionsWithBufferSync(InstJavaTODO[] instructions, int timeoutMs) {
		if (!initialized) {
			throw new IllegalStateException("HWAcceleratorJNI not initialized");
		}

		if (instructions == null || instructions.length == 0) {
			System.err.println("No instructions to execute");
			return false;
		}

		System.out.println("Sending " + instructions.length + " instructions with buffer sync...");

		// 发送指令
		if (!nativeSendInstructions(instructions, instructions.length)) {
			System.err.println("Failed to send instructions");
			return false;
		}

		// 等待最后一个输出缓冲区完成
		InstJavaTODO lastInst = instructions[instructions.length - 1];
		int outputBufferId = getOutputBufferId(lastInst);
		if (!nativeWaitForBufferCompletion(outputBufferId, timeoutMs)) {
			System.err.println("Timeout waiting for buffer " + outputBufferId + " completion");
			return false;
		}

		System.out.println("FPGA execution completed (buffer " + outputBufferId + " ready)");
		return true;
	}

	/**
	 * 等待指定缓冲区完成
	 */
	public boolean waitForBufferCompletion(int bufferId, int timeoutMs) {
		if (!initialized) {
			throw new IllegalStateException("HWAcceleratorJNI not initialized");
		}
		return nativeWaitForBufferCompletion(bufferId, timeoutMs);
	}

	/**
	 * 获取指令的输出缓冲区ID
	 */
	private int getOutputBufferId(InstJavaTODO inst) {
		try {
			java.lang.reflect.Field field = inst.getClass().getDeclaredField("bufferIdZ");
			field.setAccessible(true);
			return field.getInt(inst);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get bufferIdZ", e);
		}
	}

	/**
	 * 写入数据到 DDR
	 */
	public boolean writeDDR(long address, byte[] data) {
		if (!initialized) {
			throw new IllegalStateException("HWAcceleratorJNI not initialized");
		}
		return nativeWriteDDR(address, data, data.length);
	}

	/**
	 * 从 DDR 读取数据
	 */
	public byte[] readDDR(long address, int length) {
		if (!initialized) {
			throw new IllegalStateException("HWAcceleratorJNI not initialized");
		}
		return nativeReadDDR(address, length);
	}

	/**
	 * 获取当前状态
	 */
	public int getStatus() {
		if (!initialized) {
			return -1;
		}
		return nativeGetStatus();
	}

	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * 获取共享内存配置
	 */
	public static long[] getSharedMemoryAddresses() {
		return SHARED_MEM_ADDRESSES.clone();
	}

	public static int[] getSharedMemorySizes() {
		return SHARED_MEM_SIZES.clone();
	}

	public static int getBlockCount() {
		return SHARED_MEM_ADDRESSES.length;
	}
}

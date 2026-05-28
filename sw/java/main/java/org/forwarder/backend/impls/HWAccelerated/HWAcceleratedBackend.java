package org.forwarder.backend.impls.HWAccelerated;

import org.forwarder.Backend;
import org.forwarder.Model;
import org.forwarder.Session;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratorJNI;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.SharedMemoryPool;
import org.onnx4j.Tensor;
import org.onnx4j.TensorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 硬件加速器后端 - 共享内存方案
 *
 * 对外使用INDArray接口，内部数据存储在共享内存中
 * 现有算子代码无需任何修改
 */
public class HWAcceleratedBackend extends Backend<INDArray> {

	private static Logger logger = LoggerFactory.getLogger(HWAcceleratedBackend.class);

	public static final String BACKEND_NAME = "HWAccelerated";

	private static boolean jniInitialized = false;
	private static final Object JNI_LOCK = new Object();
	private static SharedMemoryPool sharedMemoryPool;

	public HWAcceleratedBackend() {
		super();
	}

	public HWAcceleratedBackend(Model model) {
		super(model);
		initializeJNI();
	}

	private static void initializeJNI() {
		synchronized (JNI_LOCK) {
			if (!jniInitialized) {
				HWAcceleratorJNI jni = HWAcceleratorJNI.getInstance();
				if (jni.initialize()) {
					if (jni.initializeSharedMemory()) {
						sharedMemoryPool = SharedMemoryPool.getInstance();
						jniInitialized = true;
						logger.info("HWAccelerated JNI and shared memory initialized");
					}
				}
			}
		}
	}

	@Override
	public String getName() {
		return BACKEND_NAME;
	}

	@Override
	public void disposeBackendTensor(INDArray backendTensor) {
		if (backendTensor != null && backendTensor.closeable()) {
			backendTensor.close();
		}
	}

	@Override
	public INDArray toBackendTensor(TensorManager<INDArray> tensorManager, Tensor onnx4jTensor) {
		if (sharedMemoryPool == null || !sharedMemoryPool.isInitialized()) {
			throw new IllegalStateException("Shared memory pool not initialized. Call Session.initializeSharedMemoryPool() first.");
		}

		long requiredBytes = onnx4jTensor.getMemoryBytes();
		SharedMemoryPool.Allocation alloc = sharedMemoryPool.allocate(requiredBytes);

		if (alloc == null) {
			throw new RuntimeException("Failed to allocate shared memory for tensor: " + onnx4jTensor.getName());
		}

		// 复制数据到共享内存
		java.nio.ByteBuffer srcBuffer = onnx4jTensor.getData();

		// DEBUG: 检查srcBuffer状态
		System.out.printf("[DEBUG toBackendTensor] tensor=%s, buf position=%d limit=%d capacity=%d%n",
			onnx4jTensor.getName(), srcBuffer.position(), srcBuffer.limit(), srcBuffer.capacity());
		// 用绝对索引读取前4个int，不受position影响
		if (srcBuffer.capacity() >= 16) {
			System.out.printf("[DEBUG toBackendTensor] first 4 ints (absolute): %d %d %d %d%n",
				srcBuffer.getInt(0), srcBuffer.getInt(4), srcBuffer.getInt(8), srcBuffer.getInt(12));
		}

		java.nio.ByteBuffer dstBuffer = sharedMemoryPool.mapBuffer(alloc.blockId, alloc.offset, (int) requiredBytes);
		dstBuffer.put(srcBuffer);
		dstBuffer.flip();

		// 同步到设备
		HWAcceleratorJNI.getInstance().syncToDevice(alloc.blockId, alloc.offset, (int) requiredBytes);

		// 创建包装共享内存的INDArray
		DataType nd4jDataType = convertDataType(onnx4jTensor.getDataType());
		DataBuffer dataBuffer = Nd4j.createBuffer(dstBuffer, nd4jDataType, (int) onnx4jTensor.getElementSize());

		int[] shapeArray = new int[onnx4jTensor.getShape().length];
		for (int i = 0; i < shapeArray.length; i++) {
			shapeArray[i] = (int) onnx4jTensor.getShape()[i];
		}

		INDArray indArray = Nd4j.create(dataBuffer, shapeArray);
		indArray = attachTrackingInfo(indArray, alloc.blockId, alloc.offset, alloc.physicalAddress);

		tensorManager.attach(onnx4jTensor.getName(), indArray);

		logger.debug("Converted tensor {} to shared memory INDArray: block={}, offset=0x{:X}",
			onnx4jTensor.getName(), alloc.blockId, alloc.offset);

		return indArray;
	}

	@Override
	public Tensor toNativeTensor(TensorManager<Tensor> tensorManager, String name, INDArray backendTensor) {
		// 算子执行时已经把数据从 shared memory 复制到 INDArray
		// 所以这里直接从 INDArray 创建 Tensor，而不是指向 shared memory
		org.onnx4j.tensor.DataType onnx4jDataType = convertToONNXDataType(backendTensor.data().dataType());

		// DEBUG: 检查INDArray数据
		long totalElements = backendTensor.length();
		System.out.printf("[DEBUG toNativeTensor] tensor=%s, dtype=%s, length=%d%n",
			name, backendTensor.data().dataType(), totalElements);
		if (totalElements >= 4) {
			System.out.printf("[DEBUG toNativeTensor] first 4 values from INDArray: %d %d %d %d%n",
				backendTensor.getLong(0), backendTensor.getLong(1), backendTensor.getLong(2), backendTensor.getLong(3));
		}

		// 从 INDArray 数据创建 Tensor（与 DL4J 后端类似）
		java.nio.ByteBuffer dataBuffer = backendTensor.data().asNio();
		// DEBUG: 检查asNio()返回的buffer
		System.out.printf("[DEBUG toNativeTensor] asNio buffer: position=%d limit=%d capacity=%d%n",
			dataBuffer.position(), dataBuffer.limit(), dataBuffer.capacity());
		if (dataBuffer.capacity() >= 16) {
			System.out.printf("[DEBUG toNativeTensor] first 4 ints from asNio (absolute): %d %d %d %d%n",
				dataBuffer.getInt(0), dataBuffer.getInt(4), dataBuffer.getInt(8), dataBuffer.getInt(12));
		}

		Tensor tensor = new Tensor(
			name,
			"",
			onnx4jDataType,
			org.onnx4j.tensor.Shape.create(backendTensor.shape()),
			dataBuffer  // 使用 INDArray 的数据
		);

		return tensor;
	}

	@Override
	public Session<INDArray> newSession() {
		return new HWAcceleratedSession(this);
	}

	/**
	 * 为INDArray附加共享内存跟踪信息（上板环境简化实现）
	 */
	private INDArray attachTrackingInfo(INDArray array, int blockId, int offset, long physicalAddress) {
		// 上板环境不需要 attach，直接返回原数组
		return array;
	}

	/**
	 * 从INDArray获取共享内存信息（上板环境简化实现）
	 */
	public static SharedMemoryInfo getSharedMemoryInfo(INDArray array) {
		// 上板环境返回默认值
		return new SharedMemoryInfo(0, 0, (int)array.length() * 4, 0);
	}

	/**
	 * 转换ONNX数据类型到ND4J数据类型
	 */
	private DataType convertDataType(org.onnx4j.tensor.DataType onnxType) {
		if (onnxType == org.onnx4j.tensor.DataType.FLOAT) return DataType.FLOAT;
		if (onnxType == org.onnx4j.tensor.DataType.DOUBLE) return DataType.DOUBLE;
		if (onnxType == org.onnx4j.tensor.DataType.INT32) return DataType.INT;
		if (onnxType == org.onnx4j.tensor.DataType.INT64) return DataType.LONG;
		if (onnxType == org.onnx4j.tensor.DataType.BOOL) return DataType.BOOL;
		// 其他类型默认返回 FLOAT
		return DataType.FLOAT;
	}

	/**
	 * 转换ND4J数据类型到ONNX数据类型
	 */
	private org.onnx4j.tensor.DataType convertToONNXDataType(DataType nd4jType) {
		if (nd4jType == DataType.FLOAT) return org.onnx4j.tensor.DataType.FLOAT;
		if (nd4jType == DataType.DOUBLE) return org.onnx4j.tensor.DataType.DOUBLE;
		if (nd4jType == DataType.INT) return org.onnx4j.tensor.DataType.INT32;
		if (nd4jType == DataType.LONG) return org.onnx4j.tensor.DataType.INT64;
		if (nd4jType == DataType.BOOL) return org.onnx4j.tensor.DataType.BOOL;
		// 其他类型默认返回 FLOAT
		return org.onnx4j.tensor.DataType.FLOAT;
	}

	public static SharedMemoryPool getSharedMemoryPool() {
		return sharedMemoryPool;
	}

	public static boolean isInitialized() {
		return jniInitialized;
	}

	/**
	 * 共享内存信息（附加到INDArray）
	 */
	public static class SharedMemoryInfo {
		public final int blockId;
		public final int offset;
		public final int size;
		public final long physicalAddress;

		public SharedMemoryInfo(int blockId, int offset, int size, long physicalAddress) {
			this.blockId = blockId;
			this.offset = offset;
			this.size = size;
			this.physicalAddress = physicalAddress;
		}

		public int getWordAddress() {
			return offset / 4;
		}

		@Override
		public String toString() {
			return String.format("SharedMemoryInfo[block=%d, offset=0x%X, PA=0x%X]", blockId, offset, physicalAddress);
		}
	}
}

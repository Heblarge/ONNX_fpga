package org.forwarder.backend.impls.HWAccelerated.utils;

import Accelerator.InstJavaTODO;

/**
 * JNI 接口 - 用于与 R5 核和 FPGA 加速器通信
 *
 * 使用 OpenAMP RPMsg 进行通信：
 * - rpmsg_send() 直接发送指令，内核自动处理 vring 更新和 IPI 触发
 * - 无需应用层管理共享内存映射
 */
public class HWAcceleratorJNI {

    private static final String RPMSG_DEVICE = "virtio0.rpmsg-openamp-demo-channel.-1.1024";
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private static HWAcceleratorJNI instance;
    private boolean initialized = false;

    static {
        try {
            System.loadLibrary("accelerator_jni");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Warning: Cannot load accelerator_jni library: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private HWAcceleratorJNI() {
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
     * 关闭 JNI 层
     */
    private native void nativeShutdown();

    /**
     * 通过 RPMsg 发送指令到 R5
     */
    private native boolean nativeSendInstructions(InstJavaTODO[] instructions, int count);

    /**
     * 等待 R5/FPGA 完成计算
     */
    private native boolean nativeWaitForCompletion(int timeoutMs);

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

        boolean success = nativeInitialize(RPMSG_DEVICE);
        if (success) {
            initialized = true;
            System.out.println("HWAcceleratorJNI initialized successfully");
        } else {
            System.err.println("Failed to initialize HWAcceleratorJNI");
        }
        return success;
    }

    /**
     * 关闭 JNI 接口
     */
    public void shutdown() {
        if (initialized) {
            nativeShutdown();
            initialized = false;
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
}

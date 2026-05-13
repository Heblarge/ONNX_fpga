package org.forwarder.backend.impls.HWAccelerated.utils;

/**
 * 空的收集器类 - 用于上板环境
 */
public class HWAcceleratedCollector {
    private static HWAcceleratedCollector instance = new HWAcceleratedCollector();

    public static HWAcceleratedCollector getInstance() {
        return instance;
    }

    public void recordLayerUsage(String name, long[][] a, long[][] b, long[][] c) {
        // 空实现
    }

    public void reset() {
        // 空实现
    }
}

package org.forwarder.backend.impls.HWAccelerated.utils;

import Accelerator.InstJavaTODO;

/**
 * 空的跟踪器类 - 用于上板环境
 */
public class HWAcceleratedTracer {
    private static HWAcceleratedTracer instance = new HWAcceleratedTracer();

    public static HWAcceleratedTracer getInstance() {
        return instance;
    }

    public void startNode(Object node, Object inputs, Object attrs) {
        // 空实现
    }

    public void endNode(Object output) {
        // 空实现
    }

    public void addTile(String index, InstJavaTODO inst) {
        // 空实现
    }

    public TilingInfo getActiveTilingInfo() {
        return new TilingInfo();
    }

    public void pushBatchContext(String ctx) {
        // 空实现
    }

    public void popBatchContext() {
        // 空实现
    }

    public void reset() {
        // 空实现
    }

    public static class TilingInfo {
        public String strategy;
        public DimInfo mDim = new DimInfo();
        public DimInfo nDim = new DimInfo();
        public DimInfo kDim = new DimInfo();
        public String status;

        public static class DimInfo {
            public int totalPaddedSize;
            public boolean tiled;
        }
    }
}

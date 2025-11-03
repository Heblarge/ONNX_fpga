package org.forwarder.backend.impls.HWAccelerated.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HWAcceleratedCollector {

    private static final HWAcceleratedCollector INSTANCE = new HWAcceleratedCollector();

    private static final long BYTES_PER_ELEMENT = 8; // long is 8 bytes in Java

    public static class ThreadStats {
        private long totalInputA_Bytes = 0;
        private long totalInputB_Bytes = 0;
        private long totalOutputY_Bytes = 0;

        // [修改 1] 添加 element 计数字段
        private long totalInputA_Elements = 0;
        private long totalInputB_Elements = 0;
        private long totalOutputY_Elements = 0;

        private long peakSingleLayerIO_Bytes = 0;
        private String peakLayerName = "";
        private final List<LayerStatus> layerHistory = new ArrayList<>();

        public void clear() {
            totalInputA_Bytes = 0;
            totalInputB_Bytes = 0;
            totalOutputY_Bytes = 0;

            // [修改 2] 重置 element 计数字段
            totalInputA_Elements = 0;
            totalInputB_Elements = 0;
            totalOutputY_Elements = 0;

            peakSingleLayerIO_Bytes = 0;
            peakLayerName = "";
            layerHistory.clear();
        }
    }

    public static class LayerStatus {
        public final String layerName;
        public final long bytesA;
        public final long bytesB;
        public final long bytesY;
        public final long totalBytes;

        // [修改 3] 添加 element 计数字段
        public final long elementsA;
        public final long elementsB;
        public final long elementsY;

        // [修改 4] 更新构造函数以接收 element 计数
        public LayerStatus(String layerName, long bytesA, long bytesB, long bytesY,
                           long elementsA, long elementsB, long elementsY) {
            this.layerName = layerName;
            this.bytesA = bytesA;
            this.bytesB = bytesB;
            this.bytesY = bytesY;
            this.totalBytes = bytesA + bytesB + bytesY;

            // 分配新字段
            this.elementsA = elementsA;
            this.elementsB = elementsB;
            this.elementsY = elementsY;
        }

        @Override
        public String toString() {
            String nodeSeparator = "------------------------------------------------------------------";
            String layerNameStr = (layerName.length() > 60) ? layerName.substring(0, 57) + "..." : layerName;

            // [修改 5] 更新格式化字符串
            return String.format(Locale.US,
                    "%s%n 节点 (Node): %s%n" +
                            "  - 输入 A:   %10.4f MB (%d bytes, %d elements)%n" + // 添加了 elements
                            "  - 输入 B:   %10.4f MB (%d bytes, %d elements)%n" + // 添加了 elements
                            "  - 输出 Y:   %10.4f MB (%d bytes, %d elements)%n" + // 添加了 elements
                            "  - 本层总计: %10.4f MB (%d bytes)",
                    nodeSeparator,
                    layerNameStr,
                    bytesA / (1024.0 * 1024.0), bytesA, elementsA, // 传入新值
                    bytesB / (1024.0 * 1024.0), bytesB, elementsB, // 传入新值
                    bytesY / (1024.0 * 1024.0), bytesY, elementsY, // 传入新值
                    totalBytes / (1024.0 * 1024.0), totalBytes
            );
        }
    }


    private final ThreadLocal<ThreadStats> threadStats = ThreadLocal.withInitial(ThreadStats::new);

    private HWAcceleratedCollector() {}
    public static HWAcceleratedCollector getInstance() { return INSTANCE; }

    // (将 calculateBytes 替换为 calculateElements)
    private long calculateElements(long[][] tensor) {
        if (tensor == null) return 0;
        int rows = tensor.length;
        if (rows == 0) return 0;
        int cols = tensor[0].length;
        return (long) rows * cols;
    }

    // [修改 6] 核心逻辑修改
    public void recordLayerUsage(String layerName, long[][] A, long[][] B, long[][] Y) {
        ThreadStats stats = threadStats.get();

        // 1. 首先计算 Elements (rows * cols)
        long currentA_Elements = calculateElements(A);
        long currentB_Elements = calculateElements(B);
        long currentY_Elements = calculateElements(Y);

        // 2. 然后派生出 Bytes
        long currentA_Bytes = currentA_Elements * BYTES_PER_ELEMENT;
        long currentB_Bytes = currentB_Elements * BYTES_PER_ELEMENT;
        long currentY_Bytes = currentY_Elements * BYTES_PER_ELEMENT;
        long currentTotal_Bytes = currentA_Bytes + currentB_Bytes + currentY_Bytes;

        // 3. 更新总计 (Bytes)
        stats.totalInputA_Bytes += currentA_Bytes;
        stats.totalInputB_Bytes += currentB_Bytes;
        stats.totalOutputY_Bytes += currentY_Bytes;

        // 4. 更新总计 (Elements) - 新增
        stats.totalInputA_Elements += currentA_Elements;
        stats.totalInputB_Elements += currentB_Elements;
        stats.totalOutputY_Elements += currentY_Elements;

        if (currentTotal_Bytes > stats.peakSingleLayerIO_Bytes) {
            stats.peakSingleLayerIO_Bytes = currentTotal_Bytes;
            stats.peakLayerName = layerName;
        }

        // 5. 将 Bytes 和 Elements 都传入 LayerStatus
        stats.layerHistory.add(new LayerStatus(layerName,
                currentA_Bytes, currentB_Bytes, currentY_Bytes,
                currentA_Elements, currentB_Elements, currentY_Elements));
    }

    public void reset() {
        threadStats.get().clear();
    }


    public ThreadStats getCurrentThreadStats() {
        return threadStats.get();
    }

    // [修改 7] 更新摘要报告
    public String getReport() {
        ThreadStats stats = getCurrentThreadStats();
        StringBuilder report = new StringBuilder();

        // 1. 摘要部分
        report.append("==================================================================\n");
        report.append("           HWAccelerated - I/O Report\n");
        report.append("==================================================================\n\n");

        double totalA_MB = stats.totalInputA_Bytes / (1024.0 * 1024.0);
        double totalB_MB = stats.totalInputB_Bytes / (1024.0 * 1024.0);
        double totalY_MB = stats.totalOutputY_Bytes / (1024.0 * 1024.0);
        double totalIO_MB = totalA_MB + totalB_MB + totalY_MB;
        double peakIO_MB = stats.peakSingleLayerIO_Bytes / (1024.0 * 1024.0);

        report.append(String.format(Locale.US, "--- Summary ---%n"));
        // 更新这里的格式化字符串
        report.append(String.format(Locale.US, "总计 - 输入 A: %12.4f MB (%d bytes, %d elements)%n", totalA_MB, stats.totalInputA_Bytes, stats.totalInputA_Elements));
        report.append(String.format(Locale.US, "总计 - 输入 B: %12.4f MB (%d bytes, %d elements)%n", totalB_MB, stats.totalInputB_Bytes, stats.totalInputB_Elements));
        report.append(String.format(Locale.US, "总计 - 输出 Y: %12.4f MB (%d bytes, %d elements)%n", totalY_MB, stats.totalOutputY_Bytes, stats.totalOutputY_Elements));
        report.append(String.format(Locale.US, "---%n"));
        report.append(String.format(Locale.US, "模型总 I/O:   %12.4f MB%n", totalIO_MB));
        report.append(String.format(Locale.US, "---%n"));
        report.append(String.format(Locale.US, "单层 I/O 峰值: %10.4f MB%n", peakIO_MB));
        report.append(String.format(Locale.US, "峰值所在层: %s%n", stats.peakLayerName));

        // 2. 详细日志部分
        report.append("\n\n--- Layer-by-Layer Log ---%n");
        if (stats.layerHistory.isEmpty()) {
            report.append("未记录任何层。\n");
        } else {
            for (LayerStatus layerStatus : stats.layerHistory) {
                report.append(layerStatus.toString()).append("\n");
            }
        }

        return report.toString();
    }

}


package org.forwarder.backend.impls.HWAccelerated.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HWAcceleratedCollector {

    private static final HWAcceleratedCollector INSTANCE = new HWAcceleratedCollector();

    private static final long BYTES_PER_ELEMENT = 8;

    public static class ThreadStats {
        private long peakInputA_Bytes = 0;
        private long peakInputB_Bytes = 0;
        private long peakOutputY_Bytes = 0;

        private long peakInputA_Elements = 0;
        private long peakInputB_Elements = 0;
        private long peakOutputY_Elements = 0;

        private String peakInputA_LayerName = "";
        private String peakInputB_LayerName = "";
        private String peakOutputY_LayerName = "";

        public void clear() {
            peakInputA_Bytes = 0;
            peakInputB_Bytes = 0;
            peakOutputY_Bytes = 0;
            peakInputA_Elements = 0;
            peakInputB_Elements = 0;
            peakOutputY_Elements = 0;

            peakInputA_LayerName = "";
            peakInputB_LayerName = "";
            peakOutputY_LayerName = "";
        }
    }

    private final ThreadLocal<ThreadStats> threadStats = ThreadLocal.withInitial(ThreadStats::new);

    private HWAcceleratedCollector() {}
    public static HWAcceleratedCollector getInstance() { return INSTANCE; }

    private long calculateElements(long[][] tensor) {
        if (tensor == null) return 0;
        int rows = tensor.length;
        if (rows == 0) return 0;
        int cols = tensor[0].length;
        return (long) rows * cols;
    }

    public void recordLayerUsage(String layerName, long[][] A, long[][] B, long[][] Y) {
        ThreadStats stats = threadStats.get();

        long currentA_Elements = calculateElements(A);
        long currentB_Elements = calculateElements(B);
        long currentY_Elements = calculateElements(Y);

        long currentA_Bytes = currentA_Elements * BYTES_PER_ELEMENT;
        long currentB_Bytes = currentB_Elements * BYTES_PER_ELEMENT;
        long currentY_Bytes = currentY_Elements * BYTES_PER_ELEMENT;

        if (currentA_Bytes > stats.peakInputA_Bytes) {
            stats.peakInputA_Bytes = currentA_Bytes;
            stats.peakInputA_Elements = currentA_Elements;
            stats.peakInputA_LayerName = layerName;
        }

        if (currentB_Bytes > stats.peakInputB_Bytes) {
            stats.peakInputB_Bytes = currentB_Bytes;
            stats.peakInputB_Elements = currentB_Elements;
            stats.peakInputB_LayerName = layerName;
        }

        if (currentY_Bytes > stats.peakOutputY_Bytes) {
            stats.peakOutputY_Bytes = currentY_Bytes;
            stats.peakOutputY_Elements = currentY_Elements;
            stats.peakOutputY_LayerName = layerName;
        }
    }

    public void reset() {
        threadStats.get().clear();
    }

    public ThreadStats getCurrentThreadStats() {
        return threadStats.get();
    }

    public String getReport() {
        ThreadStats stats = getCurrentThreadStats();
        StringBuilder report = new StringBuilder();

        report.append("==================================================================\n");
        report.append("                HWAccelerated - I/O Report\n");
        report.append("==================================================================\n\n");

        double peakA_MB = stats.peakInputA_Bytes / (1024.0 * 1024.0);
        double peakB_MB = stats.peakInputB_Bytes / (1024.0 * 1024.0);
        double peakY_MB = stats.peakOutputY_Bytes / (1024.0 * 1024.0);

        report.append(String.format(Locale.US, "--- 1. 峰值 - 输入 A (位于: %s) ---%n", stats.peakInputA_LayerName.isEmpty() ? "N/A" : stats.peakInputA_LayerName));
        report.append(String.format(Locale.US, "峰值 A: %12.4f MB (%d bytes, %d elements)%n", peakA_MB, stats.peakInputA_Bytes, stats.peakInputA_Elements));
        report.append(String.format(Locale.US, "%n%n"));

        report.append(String.format(Locale.US, "--- 2. 峰值 - 输入 B (位于: %s) ---%n", stats.peakInputB_LayerName.isEmpty() ? "N/A" : stats.peakInputB_LayerName));
        report.append(String.format(Locale.US, "峰值 B: %12.4f MB (%d bytes, %d elements)%n", peakB_MB, stats.peakInputB_Bytes, stats.peakInputB_Elements));
        report.append(String.format(Locale.US, "%n%n"));

        report.append(String.format(Locale.US, "--- 3. 峰值 - 输出 Y (位于: %s) ---%n", stats.peakOutputY_LayerName.isEmpty() ? "N/A" : stats.peakOutputY_LayerName));
        report.append(String.format(Locale.US, "峰值 Y: %12.4f MB (%d bytes, %d elements)%n", peakY_MB, stats.peakOutputY_Bytes, stats.peakOutputY_Elements));

        return report.toString();
    }

}


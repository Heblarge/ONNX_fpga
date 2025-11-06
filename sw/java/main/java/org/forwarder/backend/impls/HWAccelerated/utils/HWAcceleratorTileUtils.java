package org.forwarder.backend.impls.HWAccelerated.utils;

import java.util.Arrays;

public final class HWAcceleratorTileUtils {

    private HWAcceleratorTileUtils() {}

    public static void clearTile(long[][] tile, int rows, int cols) {
        for(int i = 0; i < rows; i++) {
            Arrays.fill(tile[i], 0, cols, 0L);
        }
    }

    public static void copyTileFromSource(long[][] tile, long[][] source, int rowOffset, int colOffset, int tileRows, int tileCols) {
        for (int i = 0; i < tileRows; i++) {
            System.arraycopy(source[rowOffset + i], colOffset, tile[i], 0, tileCols);
        }
    }

    public static void accumulateTile(long[][] acc, long[][] partial, int tileRows, int tileCols) {
        for (int i = 0; i < tileRows; i++) {
            for (int j = 0; j < tileCols; j++) {
                acc[i][j] += partial[i][j];
            }
        }
    }

    public static void copyTileToResult(long[][] result, long[][] tile, int rowOffset, int colOffset, int tileRows, int tileCols) {
        for (int i = 0; i < tileRows; i++) {
            System.arraycopy(tile[i], 0, result[rowOffset + i], colOffset, tileCols);
        }
    }

}

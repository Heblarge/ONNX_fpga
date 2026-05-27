package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedQuantizedOperator;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedCollector;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.onnx4j.Inputs;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ExpV13;
import org.onnx4j.opsets.operator.OperatorOutputs;
import org.onnx4j.SharedMemoryPool;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratorJNI;

import static org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratorTileUtils.*;

public class HWAcceleratedExpV13 extends HWAcceleratedQuantizedOperator implements ExpV13 {

    private static final int HW_DIM_MULTIPLE = 16;
    private static final int MAX_HW_ELEMENTS = 512 * 512;

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        ExpInputsV13<INDArray> castedInputs = new ExpInputsV13<>(node, inputs);
        INDArray inputTensor = castedInputs.getInput();

        Graph graph = node.getGraph();
        long targetInputShift = castedInputs.getFpgaInShift().get(0);
        long targetOutputShift = castedInputs.getFpgaOutShift().get(0);
        String inputName = node.getInputNames()[0];
        long sourceShift = this.getProducerOutputShift(graph, inputName, targetInputShift);

        String nodeName = node.getName();

        INDArray outputTensor = this.exp(
                inputTensor,
                sourceShift,
                targetInputShift,
                targetOutputShift,
                nodeName
        );
        return new ExpOutputV13<>(outputTensor);
    }

    public INDArray exp(INDArray x, long sourceShift, long targetInputShift, long targetOutputShift, String nodeName) {
        if (x.rank() == 2) {
            return exp2D(x, sourceShift, targetInputShift, targetOutputShift, nodeName);
        } else if (x.rank() == 3) {
            return exp3D(x, sourceShift, targetInputShift, targetOutputShift, nodeName);
        } else {
            throw new IllegalArgumentException("Unsupported tensor rank for Exp: " + x.rank());
        }
    }

    private int ceilToMultiple(int value, int multiple) {
        if (multiple == 0) return value;
        return ((value + multiple - 1) / multiple) * multiple;
    }

    private INDArray exp2D(INDArray x, long sourceShift, long targetInputShift, long targetOutputShift, String nodeName) {
        long[] shape = x.shape();
        int originalRows = (int) shape[0];
        int originalCols = (int) shape[1];

        int paddedRows = ceilToMultiple(originalRows, HW_DIM_MULTIPLE);
        int paddedCols = ceilToMultiple(originalCols, HW_DIM_MULTIPLE);

        INDArray paddedX = Nd4j.zeros(x.dataType(), paddedRows, paddedCols);
        paddedX.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols)}, x);

        INDArray paddedResult = expOnAccelerator(paddedX, paddedRows, paddedCols, sourceShift, targetInputShift, targetOutputShift, nodeName);

        return paddedResult.get(NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols));
    }

    private INDArray exp3D(INDArray x, long sourceShift, long targetInputShift, long targetOutputShift, String nodeName) {
        long[] shape = x.shape();
        long batch = shape[0];

        INDArray result = Nd4j.createUninitialized(x.dataType(), shape, 'c');

        for (int i = 0; i < (int) batch; i++) {
            INDArray slice = x.slice(i);
            INDArray expSlice = exp2D(slice, sourceShift, targetInputShift, targetOutputShift, nodeName);
            result.putSlice(i, expSlice);
        }

        return result;
    }

    /**
     * 通过JNI将数据写入共享内存，发送指令到R5/FPGA执行
     * 使用固定block方案：A=block 0, B=block 1, Z=block 2
     */
    private long[][] executeOnHardware(long[][] tileA, long[][] tileB, InstJavaTODO instruction,
                                       int rows, int cols) {

        // 1. 获取共享内存池
        SharedMemoryPool pool = SharedMemoryPool.getInstance();
        if (!pool.isInitialized()) {
            throw new IllegalStateException("SharedMemoryPool not initialized");
        }

        // 2. 固定使用block 0, 1, 2（A=0, B=1, Z=2）
        final int blockIdA = 0;
        final int blockIdB = 1;
        final int blockIdZ = 2;

        // 3. 分配block（设置blockUsed[]状态，防止并发冲突）
        pool.allocateBlock(blockIdA);
        pool.allocateBlock(blockIdB);
        pool.allocateBlock(blockIdZ);

        try {
            int sizeA = rows * cols * 4;
            int sizeB = rows * cols * 4;
            int sizeZ = rows * cols * 4;

            // 4. 写入数据A到block 0
            java.nio.ByteBuffer bufA = pool.mapBlock(blockIdA, sizeA);
            bufA.order(java.nio.ByteOrder.nativeOrder());
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++)
                    bufA.putInt((int)tileA[i][j]);

            // 5. 写入数据B到block 1
            java.nio.ByteBuffer bufB = pool.mapBlock(blockIdB, sizeB);
            bufB.order(java.nio.ByteOrder.nativeOrder());
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++)
                    bufB.putInt((int)tileB[i][j]);

            // 6. 设置指令的blockId
            instruction.blockIdA = blockIdA;
            instruction.blockIdB = blockIdB;
            instruction.blockIdZ = blockIdZ;

            // 7. 执行
            HWAcceleratorJNI jni = HWAcceleratorJNI.getInstance();

            jni.syncToDevice(blockIdA, 64, sizeA);
            jni.syncToDevice(blockIdB, 64, sizeB);

            if (!jni.executeInstructions(new InstJavaTODO[]{instruction})) {
                throw new RuntimeException("FPGA execution failed");
            }

            jni.syncFromDevice(blockIdZ, 64, sizeZ);

            // 8. 读取结果从block 2
            java.nio.ByteBuffer bufZ = pool.mapBlock(blockIdZ, sizeZ);
            long[][] result = new long[rows][cols];
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++)
                    result[i][j] = bufZ.getInt() & 0xFFFFFFFFL;

            return result;
        } finally {
            // 9. 释放block（无论成功或失败）
            pool.freeBlock(blockIdA);
            pool.freeBlock(blockIdB);
            pool.freeBlock(blockIdZ);
        }
    }

    private INDArray expOnAccelerator(INDArray x, int rows, int cols, long sourceShift, long targetInputShift, long targetOutputShift, String nodeName) {

        long s_in = sourceShift;
        long s_hw = AcceleratorSimInterface.acceleratorCfg().fracWidth();
        long s_out = targetOutputShift;

        int preShiftAmount = (int) (s_in - s_hw);

        long[][] fixedPointInput = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long valX = x.getLong(i, j);
                fixedPointInput[i][j] = (preShiftAmount < 0) ? (valX << -preShiftAmount) : (valX >> preShiftAmount);
            }
        }

        int postShiftAmount = (int) (s_hw - s_out);
        long[][] matrixB_zero = new long[rows][cols];
        long[][] hardwareResult = new long[rows][cols];

        int maxRowsPerTile = MAX_HW_ELEMENTS / cols;
        if (maxRowsPerTile < HW_DIM_MULTIPLE) {
            throw new IllegalArgumentException(
                    String.format("Node %s: Cannot tile. Matrix column dimension (%d) is too large. " +
                                    "Hardware can only support %d rows with this width, but operator requires multiples of %d.",
                            nodeName, cols, maxRowsPerTile, HW_DIM_MULTIPLE)
            );
        }

        int hwTileCap = (maxRowsPerTile / HW_DIM_MULTIPLE) * HW_DIM_MULTIPLE;

        for (int rowOffset = 0; rowOffset < rows; ) {
            int rowsRemaining = rows - rowOffset;
            int TILE_ROWS = Math.min(rowsRemaining, hwTileCap);

            if (TILE_ROWS <= 0) break;

            long[][] tileA = new long[TILE_ROWS][cols];
            long[][] tileB = new long[TILE_ROWS][cols];

            InstJavaTODO instruction = new InstJavaTODO(
                    0,
                    "none",
                    0,
                    false,
                    "exp",
                    postShiftAmount,
                    0,
                    0,
                    0,
                    TILE_ROWS,
                    cols,
                    cols,
                    0,
                    0
            );

            copyTileFromSource(tileA, fixedPointInput, rowOffset, 0, TILE_ROWS, cols);

            // 使用硬件执行替代仿真
            long[][] tileResult = executeOnHardware(tileA, tileB, instruction, TILE_ROWS, cols);

            copyTileToResult(hardwareResult, tileResult, rowOffset, 0, TILE_ROWS, cols);

            rowOffset += TILE_ROWS;
        }
        HWAcceleratedCollector.getInstance().recordLayerUsage(nodeName, fixedPointInput, matrixB_zero, hardwareResult);

        long[] output = new long[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                output[i * cols + j] = hardwareResult[i][j];
            }
        }

        INDArray fianlOutput = Nd4j.create(output, new long[]{rows, cols}, x.dataType());

        return fianlOutput;
    }
}

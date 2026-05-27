package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedQuantizedOperator;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedCollector;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.onnx4j.Inputs;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.SubV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import static org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratorTileUtils.*;

import java.util.List;

/**
 * Implements the Sub operation using a hardware accelerator.
 * This version automatically pads inputs to be multiples of 32 for hardware compatibility.
 * It handles tensors of any rank >= 2 and supports Numpy-style broadcasting.
 */
public class HWAcceleratedSubV13 extends HWAcceleratedQuantizedOperator implements SubV13 {

    private static final int HW_DIM_MULTIPLE = 16;
    private static final int MAX_HW_ELEMENTS = 512 * 512;

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        SubInputsV13<INDArray> castedInputs = new SubInputsV13<>(node, inputs);
        INDArray matrixA = castedInputs.getA();
        INDArray matrixB = castedInputs.getB();

        Graph graph = node.getGraph();
        List<Long> targetInputShifts = castedInputs.getFpgaInShift();
        long targetInputShiftA = targetInputShifts.get(0);
        long targetInputShiftB = targetInputShifts.get(1);
        String inputAName = node.getInputNames()[0];
        long sourceShiftA = this.getProducerOutputShift(graph, inputAName, targetInputShiftA);
        String inputBName = node.getInputNames()[1];
        long sourceShiftB = this.getProducerOutputShift(graph, inputBName, targetInputShiftB);
        long targetOutputShift = castedInputs.getFpgaOutShift().get(0);

        String nodeName = node.getName();
        INDArray outputTensor = this.sub(
                matrixA,
                matrixB,
                sourceShiftA,
                sourceShiftB,
                targetInputShiftA,
                targetInputShiftB,
                targetOutputShift,
                nodeName
        );
        return new SubOutputV13<>(outputTensor);
    }

    /**
     * Public dispatcher for the Sub operation.
     * It handles broadcasting and reshapes tensors for efficient hardware execution.
     */
    public INDArray sub(INDArray a, INDArray b, long sourceShiftA, long sourceShiftB, long targetInputShiftA, long targetInputShiftB, long targetOutputShift, String nodeName) {
        // 如果两个输入的形状不完全相同，则进行广播处理
        if (!java.util.Arrays.equals(a.shape(), b.shape())) {
            long[] broadcastShape = getBroadcastShape(a.shape(), b.shape());
            // 将 a 和 b 广播到目标形状
            a = a.broadcast(broadcastShape);
            b = b.broadcast(broadcastShape);
        }

        if (a.rank() == 2) {
            return sub2D(a, b, sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift, nodeName);
        } else if (a.rank() > 2) {
            long[] finalShape = a.shape();
            long numCols = finalShape[finalShape.length - 1];
            long numRows = a.length() / numCols;
            INDArray reshapedA = a.reshape('c', numRows, numCols);
            INDArray reshapedB = b.reshape('c', numRows, numCols);
            INDArray result2D = sub2D(reshapedA, reshapedB, sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift, nodeName);
            return result2D.reshape('c', finalShape);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported tensor rank for Sub: " + a.rank() + ". Only ranks >= 2 are supported."
            );
        }
    }

    /**
     * Calculates the resulting shape of a broadcasting operation between two shapes.
     * Follows Numpy-style broadcasting rules.
     */
    private long[] getBroadcastShape(long[] shapeA, long[] shapeB) {
        int rankA = shapeA.length;
        int rankB = shapeB.length;
        int maxRank = Math.max(rankA, rankB);
        long[] resultShape = new long[maxRank];

        for (int i = 1; i <= maxRank; i++) {
            long dimA = (rankA - i >= 0) ? shapeA[rankA - i] : 1;
            long dimB = (rankB - i >= 0) ? shapeB[rankB - i] : 1;

            if (dimA != dimB && dimA != 1 && dimB != 1) {
                throw new IllegalArgumentException("Shapes " + java.util.Arrays.toString(shapeA) + " and "
                        + java.util.Arrays.toString(shapeB) + " are not broadcastable.");
            }
            resultShape[maxRank - i] = Math.max(dimA, dimB);
        }
        return resultShape;
    }

    /**
     * Calculates the ceiling of a value to the nearest multiple.
     */
    private int ceilToMultiple(int value, int multiple) {
        if (multiple == 0) return value;
        return ((value + multiple - 1) / multiple) * multiple;
    }

    /**
     * Performs 2D element-wise subtraction with padding and slicing.
     */
    private INDArray sub2D(INDArray a, INDArray b, long sourceShiftA, long sourceShiftB, long targetInputShiftA, long targetInputShiftB, long targetOutputShift, String nodeName) {
        if (!java.util.Arrays.equals(a.shape(), b.shape())) {
            throw new IllegalArgumentException("Input shapes must be identical for hardware acceleration.");
        }

        int originalRows = (int) a.rows();
        int originalCols = (int) a.columns();

        int paddedRows = ceilToMultiple(originalRows, HW_DIM_MULTIPLE);
        int paddedCols = ceilToMultiple(originalCols, HW_DIM_MULTIPLE);

        INDArray paddedA = Nd4j.zeros(a.dataType(), paddedRows, paddedCols);
        paddedA.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols)}, a);

        INDArray paddedB = Nd4j.zeros(b.dataType(), paddedRows, paddedCols);
        paddedB.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols)}, b);

        INDArray paddedResult = subOnAccelerator(
                paddedA, paddedB, paddedRows, paddedCols,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift,
                nodeName);

        return paddedResult.get(NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols));
    }

    /**
     * Private helper to run element-wise subtraction on the hardware simulator.
     * Note: It simulates A - B by computing A + (-B) on the hardware, which only supports addition.
     */
    private INDArray subOnAccelerator(INDArray a, INDArray b, int rows, int cols, long sourceShiftA, long sourceShiftB, long targetInputShiftA, long targetInputShiftB, long targetOutputShift, String nodeName) {
        if (targetInputShiftA != targetInputShiftB) {
            throw new IllegalArgumentException(
                    "Inputs to Add operation have different target input shifts ("
                            + targetInputShiftA + " vs " + targetInputShiftB + "), which is unsupported."
            );
        }

        int rescaleShiftA = (int) (sourceShiftA - targetInputShiftA);
        int rescaleShiftB = (int) (sourceShiftB - targetInputShiftB);

        long[][] fixedPointA = new long[rows][cols];
        long[][] fixedPointB = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long valA = a.getLong(i, j);
                fixedPointA[i][j] = (rescaleShiftA < 0) ? (valA << -rescaleShiftA) : (valA >> rescaleShiftA);
                long valB = b.getLong(i, j) * -1;
                fixedPointB[i][j] = (rescaleShiftB < 0) ? (valB << -rescaleShiftB) : (valB >> rescaleShiftB);

            }
        }

        int shiftAmount = (int) (targetInputShiftA - targetOutputShift);

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
                    "elementadd",
                    shiftAmount,
                    false,
                    "none",
                    0,
                    0,  // blockIdA
                    0,  // blockIdB
                    0,  // blockIdZ
                    TILE_ROWS,
                    cols,
                    cols,
                    0,
                    0
            );

            copyTileFromSource(tileA, fixedPointA, rowOffset, 0, TILE_ROWS, cols);
            copyTileFromSource(tileB, fixedPointB, rowOffset, 0, TILE_ROWS, cols);

            long[][] tileResult = executeOnHardware(tileA, tileB, instruction, TILE_ROWS, cols);

            copyTileToResult(hardwareResult, tileResult, rowOffset, 0, TILE_ROWS, cols);

            rowOffset += TILE_ROWS;
        }
        HWAcceleratedCollector.getInstance().recordLayerUsage(nodeName, fixedPointA, fixedPointB, hardwareResult);

        long[] output = new long[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                output[i * cols + j] = hardwareResult[i][j];
            }
        }

        return Nd4j.create(output, new long[]{rows, cols}, a.dataType());
    }

    // ==================== 真实硬件执行方法 ====================
    /**
     * 通过JNI将数据写入共享内存，发送指令到R5/FPGA执行
     * 使用固定block方案：A=block 0, B=block 1, Z=block 2
     */
    private long[][] executeOnHardware(long[][] tileA, long[][] tileB, InstJavaTODO instruction,
                                       int rows, int cols) {
        System.out.println("[JNI-HW] Sub: " + rows + "x" + cols);
        org.onnx4j.SharedMemoryPool pool = org.onnx4j.SharedMemoryPool.getInstance();
        if (!pool.isInitialized()) {
            throw new IllegalStateException("SharedMemoryPool not initialized");
        }

        // 固定使用block 0, 1, 2（A=0, B=1, Z=2）
        final int blockIdA = 0;
        final int blockIdB = 1;
        final int blockIdZ = 2;

        // 分配block（设置blockUsed[]状态，防止并发冲突）
        pool.allocateBlock(blockIdA);
        pool.allocateBlock(blockIdB);
        pool.allocateBlock(blockIdZ);

        try {
            int size = rows * cols * 4;

            // 写入数据A到block 0
            java.nio.ByteBuffer bufA = pool.mapBlock(blockIdA, size);
            bufA.order(java.nio.ByteOrder.nativeOrder());
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++)
                    bufA.putInt((int)tileA[i][j]);

            // 写入数据B到block 1
            java.nio.ByteBuffer bufB = pool.mapBlock(blockIdB, size);
            bufB.order(java.nio.ByteOrder.nativeOrder());
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++)
                    bufB.putInt((int)tileB[i][j]);

            instruction.blockIdA = blockIdA;
            instruction.blockIdB = blockIdB;
            instruction.blockIdZ = blockIdZ;

            org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratorJNI jni =
                org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratorJNI.getInstance();

            jni.syncToDevice(blockIdA, 64, size);
            jni.syncToDevice(blockIdB, 64, size);

            if (!jni.executeInstructions(new InstJavaTODO[]{instruction})) {
                throw new RuntimeException("FPGA execution failed");
            }

            jni.syncFromDevice(blockIdZ, 64, size);

            // 读取结果从block 2
            java.nio.ByteBuffer bufZ = pool.mapBlock(blockIdZ, size);
            long[][] result = new long[rows][cols];
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++)
                    result[i][j] = bufZ.getInt() & 0xFFFFFFFFL;

            return result;
        } finally {
            // 释放block（无论成功或失败）
            pool.freeBlock(blockIdA);
            pool.freeBlock(blockIdB);
            pool.freeBlock(blockIdZ);
        }
    }
}
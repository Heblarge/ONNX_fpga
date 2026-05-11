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
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.Graph;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.AddV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import static org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratorTileUtils.*;

import java.util.List;

/**
 * Implements the Add operation using a hardware accelerator.
 * This version automatically pads inputs to be multiples of 32 for hardware compatibility.
 * It handles tensors of any rank >= 2 and supports Numpy-style broadcasting.
 */
public class HWAcceleratedAddV13 extends HWAcceleratedQuantizedOperator implements AddV13 {

    private static final int HW_DIM_MULTIPLE = 16;
    private static final int MAX_HW_ELEMENTS = 512 * 512;

    // Buffer分配计数器
    private static int nextBufferId = 0;

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        AddInputsV13<INDArray> castedInputs = new AddInputsV13<>(node, inputs);
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
        INDArray outputTensor = this.add(
                matrixA,
                matrixB,
                sourceShiftA,
                sourceShiftB,
                targetInputShiftA,
                targetInputShiftB,
                targetOutputShift,
                nodeName);
        return new AddOutputV13<>(outputTensor);
    }

    /**
     * Calculates the ceiling of a value to the nearest multiple.
     */
    private int ceilToMultiple(int value, int multiple) {
        if (multiple == 0) return value;
        return ((value + multiple - 1) / multiple) * multiple;
    }

    /**
     * Public dispatcher for the Add operation.
     * It handles broadcasting and reshapes tensors for efficient hardware execution.
     */
    public INDArray add(INDArray a, INDArray b, long sourceShiftA, long sourceShiftB,
                        long targetInputShiftA, long targetInputShiftB, long targetOutputShift,
                        String nodeName) {
        if (!java.util.Arrays.equals(a.shape(), b.shape())) {
            long[] broadcastShape = getBroadcastShape(a.shape(), b.shape());
            a = a.broadcast(broadcastShape);
            b = b.broadcast(broadcastShape);
        }

        if (a.rank() == 2) {
            return add2D(a, b, sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift, nodeName);
        } else if (a.rank() > 2) {
            long[] finalShape = a.shape();
            long numCols = finalShape[finalShape.length - 1];
            long numRows = a.length() / numCols;
            INDArray reshapedA = a.reshape('c', numRows, numCols);
            INDArray reshapedB = b.reshape('c', numRows, numCols);
            INDArray result2D = add2D(reshapedA, reshapedB, sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift, nodeName);
            return result2D.reshape('c', finalShape);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported tensor rank for Add: " + a.rank() + ". Only ranks >= 2 are supported."
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
     * Performs 2D element-wise addition with padding and slicing.
     */
    private INDArray add2D(INDArray a, INDArray b, long sourceShiftA, long sourceShiftB, long targetInputShiftA, long targetInputShiftB, long targetOutputShift, String nodeName) {
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

        INDArray paddedResult = addOnAccelerator(
                paddedA, paddedB, paddedRows, paddedCols,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift,
                nodeName);

        return paddedResult.get(NDArrayIndex.interval(0, originalRows), NDArrayIndex.interval(0, originalCols));
    }

    /**
     * Private helper to run 2D element-wise addition on the hardware simulator.
     */
    private INDArray addOnAccelerator(INDArray a, INDArray b, int rows, int cols, long sourceShiftA, long sourceShiftB, long targetInputShiftA, long targetInputShiftB, long targetOutputShift, String nodeName) {
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
                long valB = b.getLong(i, j);
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
                    0,  // bufferIdA
                    0,  // bufferIdB
                    0,  // bufferIdZ
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
                output[i * cols + j] =  hardwareResult[i][j];
            }
        }

        return Nd4j.create(output, new long[]{rows, cols}, a.dataType());
    }

    // ==================== 真实硬件执行方法 ====================
    /**
     * 通过JNI将数据写入共享内存，发送指令到R5/FPGA执行
     * 使用预分配buffer池方案（与 R5 侧 g_buffer_pool 对齐）
     */
    private long[][] executeOnHardware(long[][] tileA, long[][] tileB, InstJavaTODO instruction,
                                       int rows, int cols) {

        System.out.println("[JNI-HW] Add: " + rows + "x" + cols);

        // 1. 获取共享内存池
        org.onnx4j.SharedMemoryPool pool = org.onnx4j.SharedMemoryPool.getInstance();
        if (!pool.isInitialized()) {
            throw new IllegalStateException("SharedMemoryPool not initialized");
        }

        // 2. 计算大小并分配固定 buffer（与 R5 侧对齐）
        int sizeA = rows * cols * 4;
        int sizeB = rows * cols * 4;
        int sizeZ = rows * cols * 4;

        int bufferIdA = pool.findNextFreeBuffer(nextBufferId);
        if (bufferIdA < 0) {
            throw new RuntimeException("No free buffer available for A");
        }
        org.onnx4j.SharedMemoryPool.BufferInfo infoA = pool.allocateBuffer(bufferIdA);

        int bufferIdB = pool.findNextFreeBuffer(bufferIdA + 1);
        if (bufferIdB < 0) {
            pool.freeBuffer(bufferIdA);
            throw new RuntimeException("No free buffer available for B");
        }
        org.onnx4j.SharedMemoryPool.BufferInfo infoB = pool.allocateBuffer(bufferIdB);

        int bufferIdZ = pool.findNextFreeBuffer(bufferIdB + 1);
        if (bufferIdZ < 0) {
            pool.freeBuffer(bufferIdA);
            pool.freeBuffer(bufferIdB);
            throw new RuntimeException("No free buffer available for Z");
        }
        org.onnx4j.SharedMemoryPool.BufferInfo infoZ = pool.allocateBuffer(bufferIdZ);

        nextBufferId = (bufferIdZ + 1) % 64;

        try {
            // 3. 写入数据（使用固定地址）
            java.nio.ByteBuffer buffer = pool.mapBuffer(bufferIdA, sizeA);
            buffer.order(java.nio.ByteOrder.nativeOrder());
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++)
                    buffer.putInt((int)tileA[i][j]);

            buffer = pool.mapBuffer(bufferIdB, sizeB);
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++)
                    buffer.putInt((int)tileB[i][j]);

            // 4. 设置bufferId
            instruction.bufferIdA = bufferIdA;
            instruction.bufferIdB = bufferIdB;
            instruction.bufferIdZ = bufferIdZ;

            // 5. 执行
            org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratorJNI jni =
                org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratorJNI.getInstance();

            jni.syncToDevice(infoA.blockId, infoA.offsetInBlock, sizeA);
            jni.syncToDevice(infoB.blockId, infoB.offsetInBlock, sizeB);

            if (!jni.executeInstructions(new InstJavaTODO[]{instruction})) {
                throw new RuntimeException("FPGA execution failed");
            }

            jni.syncFromDevice(infoZ.blockId, infoZ.offsetInBlock, sizeZ);

            // 6. 读取结果
            buffer = pool.mapBuffer(bufferIdZ, sizeZ);
            long[][] result = new long[rows][cols];
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++)
                    result[i][j] = buffer.getInt() & 0xFFFFFFFFL;

            return result;
        } finally {
            // 7. 释放 buffer
            pool.freeBuffer(bufferIdA);
            pool.freeBuffer(bufferIdB);
            pool.freeBuffer(bufferIdZ);
        }
    }
}
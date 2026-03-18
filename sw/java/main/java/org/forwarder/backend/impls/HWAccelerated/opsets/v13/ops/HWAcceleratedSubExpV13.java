package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedQuantizedOperator;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedCollector;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.SubExpV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import static org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratorTileUtils.*;

import java.util.List;

/**
 * HWAcceleratedSubExpV13 实现：计算 Exp(A - B)
 * 逻辑：在 Java 层计算 A - B，然后调用硬件的 elementadd 指令（加 0）并开启 exp 激活函数。
 */
public class HWAcceleratedSubExpV13 extends HWAcceleratedQuantizedOperator implements SubExpV13 {

    private static final int HW_DIM_MULTIPLE = 16;
    private static final int MAX_HW_ELEMENTS = 512 * 512;

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        SubExpInputsV13<INDArray> castedInputs = new SubExpInputsV13<>(node, inputs);
        INDArray A = castedInputs.getA();
        INDArray B = castedInputs.getB();

        Graph graph = node.getGraph();
        List<Long> targetInputShifts = castedInputs.getFpgaInShift();
        long targetInShiftA = targetInputShifts.get(0);
        long targetInShiftB = targetInputShifts.get(1);

        String inputAName = node.getInputNames()[0];
        String inputBName = node.getInputNames()[1];

        // 获取输入算子的输出 Shift
        long srcShiftA = this.getProducerOutputShift(graph, inputAName, targetInShiftA);
        long srcShiftB = this.getProducerOutputShift(graph, inputBName, targetInShiftB);

        long targetOutShift = castedInputs.getFpgaOutShift().get(0);

        INDArray output = subExp(A, B, srcShiftA, srcShiftB, targetInShiftA, targetInShiftB, targetOutShift, node.getName());
        return new SubExpOutputV13<>(output);
    }

    public INDArray subExp(INDArray a, INDArray b, long srcA, long srcB, long tgtInA, long tgtInB, long tgtOut, String nodeName) {
        // 1. 广播处理 (Broadcasting)
        if (!java.util.Arrays.equals(a.shape(), b.shape())) {
            long[] shape = getBroadcastShape(a.shape(), b.shape());
            a = a.broadcast(shape);
            b = b.broadcast(shape);
        }

        // 2. 维度变换 (转换为 2D 处理)
        long[] finalShape = a.shape();
        long numCols = finalShape[finalShape.length - 1];
        long numRows = a.length() / numCols;

        INDArray reshapedA = a.reshape('c', numRows, numCols);
        INDArray reshapedB = b.reshape('c', numRows, numCols);

        int originalRows = (int) numRows;
        int originalCols = (int) numCols;

        // 3. 计算填充 (Padding) 和 Rescale
        int paddedRows = ceilToMultiple(originalRows, HW_DIM_MULTIPLE);
        int paddedCols = ceilToMultiple(originalCols, HW_DIM_MULTIPLE);

        long s_hw = AcceleratorSimInterface.acceleratorCfg().fracWidth();
        int rescaleA = (int)(srcA - tgtInA);
        int rescaleB = (int)(srcB - tgtInB);

        // 在 Java 层预计算减法结果
        long[][] subResult = new long[paddedRows][paddedCols];
        long maxAbs = 0;

        for (int i = 0; i < paddedRows; i++) {
            for (int j = 0; j < paddedCols; j++) {
                if (i < originalRows && j < originalCols) {
                    long fA = (rescaleA < 0) ? (reshapedA.getLong(i, j) << -rescaleA) : (reshapedA.getLong(i, j) >> rescaleA);
                    long fB = (rescaleB < 0) ? (reshapedB.getLong(i, j) << -rescaleB) : (reshapedB.getLong(i, j) >> rescaleB);
                    long diff = fA - fB;
                    subResult[i][j] = diff;
                    maxAbs = Math.max(maxAbs, Math.abs(diff));
                } else {
                    subResult[i][j] = 0;
                }
            }
        }

        // 4. Exp 动态缩放逻辑 (针对硬件定点 Exp 的输入范围限制)
        int expInShift = (int)(tgtInA - s_hw);
        long expUpper = (2L << s_hw);
        int dynamicShift = 0;
        while (maxAbs > 0) {
            long test = (expInShift > 0) ? (maxAbs >> expInShift) : (maxAbs << -expInShift);
            if ((test >> dynamicShift) <= expUpper) break;
            dynamicShift++;
        }

        int subShiftAmount = expInShift + dynamicShift;
        int postShift = (int)(s_hw - tgtOut);

        // 5. 分块执行硬件指令
        long[][] hardwareResult = new long[paddedRows][paddedCols];
        int hwTileCap = (MAX_HW_ELEMENTS / paddedCols / HW_DIM_MULTIPLE) * HW_DIM_MULTIPLE;

        for (int rowOffset = 0; rowOffset < paddedRows; ) {
            int TILE_ROWS = Math.min(paddedRows - rowOffset, hwTileCap);
            if (TILE_ROWS <= 0) break;

            long[][] tileA = new long[TILE_ROWS][paddedCols];
            long[][] tileB = new long[TILE_ROWS][paddedCols]; // 减法已完成，tileB 设为全 0

            // 将 A-B 的差值填入 tileA
            copyTileFromSource(tileA, subResult, rowOffset, 0, TILE_ROWS, paddedCols);

            // 使用 elementadd 指令，激活函数设为 exp
            InstJavaTODO inst = new InstJavaTODO(
                    0,
                    "elementadd",    // 修复：使用硬件支持的加法名
                    subShiftAmount,
                    false,
                    "exp",           // 开启 exp 激活
                    postShift,
                    0, 0, 0,
                    TILE_ROWS, paddedCols, paddedCols, 0, 0
            );

            long[][] res = AcceleratorSimInterface.runRefOneInst(tileA, tileB, inst);
            copyTileToResult(hardwareResult, res, rowOffset, 0, TILE_ROWS, paddedCols);
            rowOffset += TILE_ROWS;
        }

        // 记录层使用情况用于调试
        HWAcceleratedCollector.getInstance().recordLayerUsage(nodeName, subResult, new long[paddedRows][paddedCols], hardwareResult);

        // 6. 还原数据形状并去除 Padding
        long[] flat = new long[originalRows * originalCols];
        for (int i = 0; i < originalRows; i++) {
            for (int j = 0; j < originalCols; j++) {
                flat[i * originalCols + j] = hardwareResult[i][j];
            }
        }

        return Nd4j.create(flat, new long[]{originalRows, originalCols}, a.dataType()).reshape('c', finalShape);
    }

    private int ceilToMultiple(int value, int multiple) {
        return (multiple == 0) ? value : ((value + multiple - 1) / multiple) * multiple;
    }

    private long[] getBroadcastShape(long[] s1, long[] s2) {
        int max = Math.max(s1.length, s2.length);
        long[] res = new long[max];
        for (int i = 1; i <= max; i++) {
            long d1 = (s1.length - i >= 0) ? s1[s1.length - i] : 1;
            long d2 = (s2.length - i >= 0) ? s2[s2.length - i] : 1;
            res[max - i] = Math.max(d1, d2);
        }
        return res;
    }
}
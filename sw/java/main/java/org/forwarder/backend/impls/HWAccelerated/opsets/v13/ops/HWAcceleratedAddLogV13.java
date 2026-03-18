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
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.AddLogV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import java.util.List;

public class HWAcceleratedAddLogV13 extends HWAcceleratedQuantizedOperator implements AddLogV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        AddLogInputsV13<INDArray> castedInputs = new AddLogInputsV13<>(node, inputs);

        // 1. 获取输入名列表，这是判断输入数量最可靠的依据
        String[] inputNames = node.getInputNames();
        int inputCount = inputNames.length;

        // 2. 获取输入 A (始终存在)
        INDArray A = castedInputs.getA();

        // 3. 安全获取输入 B
        INDArray B;
        if (inputCount > 1) {
            // 只有逻辑上确实有两个输入名时，才去尝试获取 B
            B = castedInputs.getB();
        } else {
            // 如果模型里这个算子只连了一个输入，我们给它补个 1（ln(A+1)）或 0（ln(A+0)）
            // 根据 AddLog 的定义通常是 A+B，这里补 0 是为了让 A + B = A
            B = Nd4j.zeros(A.dataType(), A.shape());
            System.out.println("  [WARN] Node " + node.getName() + " has only 1 input. Padding B with zeros.");
        }

        Graph graph = node.getGraph();

        // 4. 获取位移参数，增加列表长度保护
        List<Long> targetInShifts = castedInputs.getFpgaInShift();
        long tInA = targetInShifts.get(0);
        // 如果配置里只有一个位移值，B 借用 A 的位移值
        long tInB = (targetInShifts.size() > 1) ? targetInShifts.get(1) : tInA;
        long tOut = castedInputs.getFpgaOutShift().get(0);

        // 5. 获取生产者的位移
        String inAName = inputNames[0];
        long sA = this.getProducerOutputShift(graph, inAName, tInA);

        long sB = 0;
        if (inputCount > 1) {
            String inBName = inputNames[1];
            sB = this.getProducerOutputShift(graph, inBName, tInB);
        } else {
            sB = tInB; // 单输入时设为一致，避免 rescale 产生大幅抖动
        }

        INDArray output = addLog(A, B, sA, sB, tInA, tInB, tOut, node.getName());
        return new AddLogOutputV13<>(output);
    }

    /* ============================================================
       以下计算逻辑保持不变，已包含你之前的补偿修复
       ============================================================ */

    public INDArray addLog(INDArray a, INDArray b, long sA, long sB, long tInA, long tInB, long tOut, String nodeName) {
        if (!java.util.Arrays.equals(a.shape(), b.shape())) {
            long[] bs = getBroadcastShape(a.shape(), b.shape());
            a = a.broadcast(bs); b = b.broadcast(bs);
        }

        long[] originalShape = a.shape();
        if (a.rank() > 2) {
            long lastDim = a.size(-1);
            long otherDims = a.length() / lastDim;
            a = a.reshape('c', otherDims, lastDim);
            b = b.reshape('c', otherDims, lastDim);
        }

        INDArray result2D = addLog2D(a, b, sA, sB, tInA, tInB, tOut, nodeName);
        return result2D.reshape('c', originalShape);
    }

    private INDArray addLog2D(INDArray a, INDArray b, long sA, long sB, long tInA, long tInB, long tOut, String nodeName) {
        int rows = (int) a.rows();
        int cols = (int) a.columns();
        long s_hw_frac = AcceleratorSimInterface.acceleratorCfg().fracWidth();

        // 1. 对齐输入
        int rescaleA = (int)(sA - tInA);
        int rescaleB = (int)(sB - tInB);
        long[][] addResult = new long[rows][cols];
        long maxAbs = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long fA = (rescaleA < 0) ? (a.getLong(i, j) << -rescaleA) : (a.getLong(i, j) >> rescaleA);
                long fB = (rescaleB < 0) ? (b.getLong(i, j) << -rescaleB) : (b.getLong(i, j) >> rescaleB);
                addResult[i][j] = Math.max(fA + fB, 1L);
                maxAbs = Math.max(maxAbs, Math.abs(addResult[i][j]));
            }
        }

        // 2. 动态位移逻辑
        int logScaleShift = (int)(tInA - s_hw_frac);
        double hwMax = 5.80;
        long hwMaxFix = (long)Math.floor(hwMax * (1L << s_hw_frac));
        int dynamicShift = 0;
        if (maxAbs > 0) {
            long testVal = (logScaleShift > 0) ? (maxAbs >> logScaleShift) : (maxAbs << -logScaleShift);
            while ((testVal >> dynamicShift) > hwMaxFix) { dynamicShift++; }
        }
        int totalPreShift = logScaleShift + dynamicShift;

        // 3. 范围钳位
        double hwMinSafe = 0.21;
        long hwMinFix = (long)Math.ceil(hwMinSafe * (1L << s_hw_frac));
        long safeMinRaw = (totalPreShift > 0) ? (hwMinFix << totalPreShift) : (hwMinFix >> -totalPreShift);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (addResult[i][j] < safeMinRaw) addResult[i][j] = safeMinRaw;
            }
        }

        // 4. 调用硬件
        int postShift = (int)(s_hw_frac - tOut);
        InstJavaTODO instruction = new InstJavaTODO(0, "elementadd", totalPreShift, false, "log", postShift, 0, 0, 0, rows, cols, cols, 0, 0);
        long[][] tileResult = AcceleratorSimInterface.runRefOneInst(addResult, new long[rows][cols], instruction);

        // 5. 补偿逻辑 (ln(2) 修正)
        double ln2 = Math.log(2.0);
        long compensation = Math.round((totalPreShift + 6) * ln2 * Math.pow(2, tOut));

        long[] flat = new long[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                flat[i * cols + j] = tileResult[i][j] + compensation;
            }
        }

        HWAcceleratedCollector.getInstance().recordLayerUsage(nodeName, addResult, new long[rows][cols], tileResult);
        return Nd4j.create(flat, new long[]{rows, cols}, a.dataType());
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
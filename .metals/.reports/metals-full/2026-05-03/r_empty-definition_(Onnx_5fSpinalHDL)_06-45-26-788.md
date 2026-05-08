error id: file://<WORKSPACE>/sw/java/main/java/org/forwarder/backend/impls/HWAccelerated/opsets/v13/ops/HWAcceleratedAddLogV13.java:Accelerator/AcceleratorSimInterface#
file://<WORKSPACE>/sw/java/main/java/org/forwarder/backend/impls/HWAccelerated/opsets/v13/ops/HWAcceleratedAddLogV13.java
empty definition using pc, found symbol in pc: Accelerator/AcceleratorSimInterface#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 86
uri: file://<WORKSPACE>/sw/java/main/java/org/forwarder/backend/impls/HWAccelerated/opsets/v13/ops/HWAcceleratedAddLogV13.java
text:
```scala
package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.@@AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedQuantizedOperator;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedCollector;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.Constant;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.AddLogV13;
import org.onnx4j.opsets.operator.OperatorOutputs;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.onnx4j.model.graph.node.attributes.IntAttribute;
import org.onnx4j.Tensor;
import org.onnx4j.model.graph.node.Attributes;
import org.onnx4j.model.graph.node.attributes.IntAttribute;
import org.onnx4j.model.graph.node.attributes.IntsAttribute;

import java.util.List;

public class HWAcceleratedAddLogV13 extends HWAcceleratedQuantizedOperator implements AddLogV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        String[] inputNames = node.getInputNames();
        Graph graph = node.getGraph();

        // 1. 获取输入 A (Relu 的输出)
        INDArray A = (INDArray) inputs.get()[0].getTensor();

        // 2. 获取加数 B (Constant 或 Tensor)
        INDArray B = null;
        if (inputNames.length > 1) {
            String bName = inputNames[1];
            Constant[] graphConstants = graph.getConstants();
            if (graphConstants != null) {
                for (Constant c : graphConstants) {
                    if (c.getName().equals(bName)) {
                        B = safeExtract(c);
                        break;
                    }
                }
            }
            if (B == null && inputs.get().length > 1) {
                B = (INDArray) inputs.get()[1].getTensor();
            }
        }

        // 形状补齐逻辑 (保持不变)
        if (B == null) {
            B = Nd4j.zeros(A.dataType(), A.shape());
        } else if (!java.util.Arrays.equals(A.shape(), B.shape())) {
            B = B.broadcast(A.shape());
        }

        // --- 3. 【核心修改：精准读取中间位宽属性】 ---

        long fpgaInShift = 16L;
        org.onnx4j.model.graph.node.Attribute inAttrObj = node.getAttrs().getAttr("fpga_in_shift");
        if (inAttrObj instanceof org.onnx4j.model.graph.node.attributes.IntAttribute) {
            fpgaInShift = ((org.onnx4j.model.graph.node.attributes.IntAttribute) inAttrObj).getValue();
        } else if (inAttrObj instanceof org.onnx4j.model.graph.node.attributes.IntsAttribute) {
            List<Long> vals = ((org.onnx4j.model.graph.node.attributes.IntsAttribute) inAttrObj).getValue();
            if (vals != null && !vals.isEmpty()) fpgaInShift = vals.get(0);
        }

        // B. 读取 fpga_after_mat_shift (20位中间层)
        long internalAddShift;
        org.onnx4j.model.graph.node.Attribute matAttrObj = node.getAttrs().getAttr("fpga_after_mat_shift");
        if (matAttrObj instanceof org.onnx4j.model.graph.node.attributes.IntsAttribute) {
            List<Long> matShifts = ((org.onnx4j.model.graph.node.attributes.IntsAttribute) matAttrObj).getValue();
            internalAddShift = (matShifts != null && !matShifts.isEmpty()) ? matShifts.get(0) : fpgaInShift + 1;
        } else if (matAttrObj instanceof org.onnx4j.model.graph.node.attributes.IntAttribute) {
            internalAddShift = ((org.onnx4j.model.graph.node.attributes.IntAttribute) matAttrObj).getValue();
        } else {
            internalAddShift = fpgaInShift + 1;
        }

        // C. 读取 fpga_before_act_shift
        long fpgaBeforeActShift;
        org.onnx4j.model.graph.node.Attribute actAttrObj = node.getAttrs().getAttr("fpga_before_act_shift");
        if (actAttrObj instanceof org.onnx4j.model.graph.node.attributes.IntsAttribute) {
            List<Long> actShifts = ((org.onnx4j.model.graph.node.attributes.IntsAttribute) actAttrObj).getValue();
            fpgaBeforeActShift = (actShifts != null && !actShifts.isEmpty()) ? actShifts.get(0) : internalAddShift;
        } else {
            fpgaBeforeActShift = internalAddShift;
        }

        // D. 读取 fpga_out_shift
        long tOut = 18L;
        org.onnx4j.model.graph.node.Attribute outAttrObj = node.getAttrs().getAttr("fpga_out_shift");
        if (outAttrObj instanceof org.onnx4j.model.graph.node.attributes.IntAttribute) {
            tOut = ((org.onnx4j.model.graph.node.attributes.IntAttribute) outAttrObj).getValue();
        } else if (outAttrObj instanceof org.onnx4j.model.graph.node.attributes.IntsAttribute) {
            List<Long> vals = ((org.onnx4j.model.graph.node.attributes.IntsAttribute) outAttrObj).getValue();
            if (vals != null && !vals.isEmpty()) tOut = vals.get(0);
        }

        // 打印日志，确认读取成功
        System.out.println(String.format("  [AddLog-Fixed] In: %d, Internal: %d, Out: %d", fpgaInShift, internalAddShift, tOut));

        // 获取上游生产者的 Shift，用于 rescale 对齐到 internalAddShift
        long sA = this.getProducerOutputShift(graph, inputNames[0], fpgaInShift);
        long sB = (inputNames.length > 1) ? this.getProducerOutputShift(graph, inputNames[1], fpgaInShift) : fpgaInShift;

        // --- 4. 执行核心逻辑 ---
        // 这里我们将真实的 internalAddShift (20) 传入计算核心
        INDArray output = addLog(A, B, sA, sB, internalAddShift, fpgaBeforeActShift, tOut, node.getName());
        return new AddLogOutputV13<>(output);
    }


    public INDArray addLog(INDArray a, INDArray b, long sA, long sB,
                           long internalAddShift, long fpgaBeforeActShift,
                           long tOut, String nodeName) {
        long[] originalShape = a.shape();
        if (a.rank() > 2) {
            long lastDim = a.size(-1);
            long otherDims = a.length() / lastDim;
            a = a.reshape('c', otherDims, lastDim);
            b = b.reshape('c', otherDims, lastDim);
        }

        // 重点：这里的参数必须跟下面 addLog2D 的定义完全一致
        INDArray result2D = addLog2D(a, b, sA, sB, internalAddShift, fpgaBeforeActShift, tOut, nodeName);

        return result2D.reshape('c', originalShape);
    }

    private INDArray addLog2D(INDArray a, INDArray b, long sA, long sB,
                              long internalAddShift, long fpgaBeforeActShift,
                              long tOut, String nodeName) {
        int rows = (int) a.rows();
        int cols = (int) a.columns();
        long s_hw = AcceleratorSimInterface.acceleratorCfg().fracWidth(); // 12


        int addShiftAmount = (int) (fpgaBeforeActShift - s_hw); //  20 - 12 = 8

        long[][] addResult = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // A, B 缩放到 internalAddShift (20位)
                long fA = (sA > internalAddShift) ? a.getLong(i, j) >> (sA - internalAddShift) : a.getLong(i, j) << (internalAddShift - sA);
                long fB = (sB > internalAddShift) ? b.getLong(i, j) >> (sB - internalAddShift) : b.getLong(i, j) << (internalAddShift - sB);

                // 硬件加法结果，保持在 internalAddShift (20位)
                addResult[i][j] = Math.max(fA + fB, 1L);
            }
        }

        // 2计算输出位移
        int postShiftAmount = (int) (s_hw - tOut); // 12 - 19 = -7

        // 3️构造指令
        InstJavaTODO instruction = new InstJavaTODO(
                0,
                "elementadd",
                addShiftAmount,
                false,
                "log",
                postShiftAmount,
                0, 0, 0,
                rows, cols, cols,
                0, 0
        );

        // 运行硬件 (不计算任何动态补偿 factor)
        long[][] tileResult = AcceleratorSimInterface.runRefOneInst(addResult, new long[rows][cols], instruction);


        long[] flat = new long[rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                flat[i * cols + j] = tileResult[i][j];
            }
        }
        return Nd4j.create(flat, new long[]{rows, cols}, a.dataType());
    }

    private long[][] toLongArray(INDArray arr) {
        int r = (int)arr.rows(), c = (int)arr.columns();
        long[][] res = new long[r][c];
        for(int i=0; i<r; i++) {
            for(int j=0; j<c; j++) res[i][j] = arr.getLong(i,j);
        }
        return res;
    }

    private INDArray safeExtract(Constant c) {
        Tensor tensor = c.getTensor();
        Object data = tensor.getData();
        if (data instanceof INDArray) return (INDArray) data;

        if (data instanceof ByteBuffer) {
            ByteBuffer b = (ByteBuffer) data;
            b.rewind();


            int length = (int) tensor.getElementSize();

            int[] v = new int[length];
            // 确保以小端序读取 INT32 (对应模型中的定点值)
            b.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(v);

            return Nd4j.createFromArray(v).reshape(tensor.getShape());
        }
        return null;
    }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: Accelerator/AcceleratorSimInterface#
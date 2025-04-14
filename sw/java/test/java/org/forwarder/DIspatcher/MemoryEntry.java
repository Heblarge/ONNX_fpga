package org.forwarder.DIspatcher;

import java.util.List;


public class MemoryEntry {
    public final String tensorName;
    public final String dataType; // 如 float32
    public final List<Integer> shape;
    public final Object data; // 可为 float[] / INDArray 等
    public final String producerNode; // 哪个节点生成的（如 Convolution28）
    public final boolean isConstant; // 是否是模型内置常量，如 Parameter5

    public MemoryEntry(String tensorName, Object data, String dataType, List<Integer> shape, String producerNode, boolean isConstant) {
        this.tensorName = tensorName;
        this.data = data;
        this.dataType = dataType;
        this.shape = shape;
        this.producerNode = producerNode;
        this.isConstant = isConstant;
    }
}

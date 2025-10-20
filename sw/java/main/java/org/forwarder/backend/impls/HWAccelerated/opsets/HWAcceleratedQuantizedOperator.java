package org.forwarder.backend.impls.HWAccelerated.opsets;

import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.node.Attribute;
import org.onnx4j.model.graph.node.Attributes;
import org.onnx4j.model.graph.node.attributes.IntsAttribute;

import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedQuantizedConstants;

import java.util.List;
import javax.annotation.Nullable;

public abstract class HWAcceleratedQuantizedOperator extends HWAcceleratedOperator implements HWAcceleratedQuantizedConstants {

    @Nullable
    protected Node findProducer(Graph graph, String tensorName) {
        // 1. 调用 Graph.java 中的 getNodes()
        for (Node node : graph.getNodes()) {
            // 2. 调用 Node.java 中的 getOutputNames()
            String[] outputNames = node.getOutputNames();
            for (String outputName : outputNames) {
                if (outputName.equals(tensorName)) {
                    return node;
                }
            }
        }
        return null;
    }

    protected long getProducerOutputShift(Graph graph, String tensorName, long defaultShift) {
        Node producerNode = this.findProducer(graph, tensorName);

        if (producerNode == null) {
            return defaultShift;
        }

        // 检查当前生产者节点是否定义了 'fpga_out_shift'
        Attributes attrs = producerNode.getAttrs();
        List<Long> shifts = null;
        try {
            // 尝试获取属性。我们使用 getAttrValue 并传入 null 作为默认值，
            // 这样如果属性不存在，它会返回 null，而不会抛出异常。
            shifts = attrs.getAttrValue(ATTR_FPGA_OUT_SHIFT, IntsAttribute.class, null);

        } catch (Exception e) {
            // 捕获可能的类型转换等异常，当作“未找到”处理
            shifts = null;
        }

        if (shifts != null && !shifts.isEmpty()) {
            // 找到这个节点 (如 Add, MatMul) 是一个“量化感知”节点。
            // 返回它定义的 shift 值。
            return shifts.get(0);
        } else {
            return defaultShift;
        }
    }
}
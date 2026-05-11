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
		for (Node node : graph.getNodes()) {
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
			shifts = attrs.getAttrValue(ATTR_FPGA_OUT_SHIFT, IntsAttribute.class, null);
		} catch (Exception e) {
			shifts = null;
		}

		if (shifts != null && !shifts.isEmpty()) {
			return shifts.get(0);
		} else {
			return defaultShift;
		}
	}
}

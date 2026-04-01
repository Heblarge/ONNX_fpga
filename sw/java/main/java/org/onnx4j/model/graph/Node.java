/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onnx4j.model.graph;

import java.util.Arrays;

import org.onnx4j.Model;
import org.onnx4j.NamedOnnxObject;
import org.onnx4j.Tensor;
import org.onnx4j.model.graph.node.Attributes;
import org.onnx4j.prototypes.OnnxProto3.NodeProto;
import org.onnx4j.model.Graph;
import org.onnx4j.prototypes.OnnxOperatorsProto3.OperatorProto;

public final class Node extends NamedOnnxObject {

	protected Model model;
	protected String domain;
	protected String opType;
	protected String[] inputNames;
	protected String[] outputNames;
	protected Attributes attributes;
	protected OperatorProto operatorProto;

	public Node(Model model, NodeProto nodeProto, Tensor.Options tensorOptions) {
		super(nodeProto.getName(), nodeProto.getDocString());

		this.inputNames = nodeProto.getInputList().toArray(new String[nodeProto.getInputList().size()]);
		this.outputNames = nodeProto.getOutputList().toArray(new String[nodeProto.getOutputList().size()]);

		this.domain = nodeProto.getDomain();
		this.opType = nodeProto.getOpType();
		this.attributes = new Attributes(model, nodeProto.getAttributeList());

		this.model = model;
		if (this.model != null && this.model.getGraph() != null) {
			this.operatorProto = this.model.getGraph().getCustomOp(this.opType);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		org.onnx4j.model.graph.Node other = (org.onnx4j.model.graph.Node) o;

		// 1. 优先使用 Node Name 比较（针对有名字的标准节点）
		if (this.getName() != null && !this.getName().isEmpty() &&
				other.getName() != null && !other.getName().isEmpty()) {
			return java.util.Objects.equals(this.getName(), other.getName());
		}

		// 2. 如果 Node Name 为空（融合算子、大模型抠出的子图节点），则对比完整的输出变量列表
		// 这是 ONNX 拓扑结构中最为严谨的判定方式，能完美处理多输出算子
		return java.util.Arrays.equals(this.getOutputNames(), other.getOutputNames());
	}

	@Override
	public int hashCode() {
		if (this.getName() != null && !this.getName().isEmpty()) {
			return java.util.Objects.hash(this.getName());
		}
		// 必须和 equals 保持一致
		return java.util.Arrays.hashCode(this.getOutputNames());
	}

	public String[] getInputNames() {
		return inputNames;
	}

	public String[] getOutputNames() {
		return outputNames;
	}

	public Attributes getAttrs() {
		return attributes;
	}

	public String getOpType() {
		return opType;
	}

	public String getDomain() {
		return domain;
	}

	public OperatorProto getOperatorProto() {
		return this.operatorProto;
	}


	@Override
	public String toString() {
		return "Node [domain=" + domain + ", opType=" + opType + ", inputNames=" + Arrays.toString(inputNames)
				+ ", outputNames=" + Arrays.toString(outputNames) + ", attributes=" + attributes + "]";
	}

	public Graph getGraph() {
		return this.model.getGraph();
	}


}
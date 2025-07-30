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
package org.forwarder.executor.impls;

import java.util.Collection;
import java.util.LinkedList;

import org.forwarder.Session;
import org.forwarder.executor.Executor;
import org.onnx4j.Inputs;
import org.onnx4j.Inputs.Input;
import org.onnx4j.Model;
import org.onnx4j.Outputs;
import org.onnx4j.Outputs.Output;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.exchanges.GraphOutput;
import org.onnx4j.opsets.OperatorSets;
import java.util.Set;
import java.util.HashSet;


public class RayExecutor<T_BK_TS> extends Executor<T_BK_TS> {

	private Collection<Node> orderedSequenceNodes;

	public RayExecutor(Model model) {
		super(model);
		this.orderedSequenceNodes = this.toOrderedSequenceNodes(model.getGraph());
	}

	@Override
	public void execute(Session<T_BK_TS> session, OperatorSets opsets) {
		for (Node node : this.orderedSequenceNodes) {
			//提取图中的每一个节点
			this.handle(session, opsets, node);
		}
	}

	private void handle(Session<T_BK_TS> session, OperatorSets opsets, Node node) {
		Inputs inputs = new Inputs();
		// 遍历当前节点的所有输入名称，并为每个输入创建Input对象并添加到inputs集合中
		for (String inputName : node.getInputNames()) {
			// 为当前节点的每个输入名称创建Input对象，使用wrap方法包装inputName、node和从session获取的中间输出结果
			Input input = Input.wrap(inputName, node, session.getIntermediateOutput(inputName));
			inputs.append(input);
		}
		// 调用父类中的handle方法，传入session、opsets、node和inputs，获取outputs集合
		Outputs outputs = super.handle(session, opsets, node, inputs);
		// 遍历outputs集合中的每个输出，将其名称和对应的张量存储回session的中间输出结果中
		for (Output output : outputs.get()) {
			// session.intermediateOutputs 包含节点内静态参数，推理的中间结果在此追加
			session.putIntermediateOutput(output.getName(), output.getTensor());
		}
	}
	// 从图的输出节点（终点）开始反向遍历
	private Collection<Node> toOrderedSequenceNodes(Graph graph) {
		Collection<Node> nodes = new LinkedList<Node>();
		for (GraphOutput graphOutput : graph.getOutputs()) {
			this.predecessors(nodes, graph, graphOutput.getNode());
			this.addOrderedSequenceNode(nodes, graphOutput.getNode());
		}
		return nodes;
	}

	public void printExecutionSequence() {
		System.out.println("==== Execution Sequence of Nodes (Topological Order) ====");
		int idx = 0;
		for (Node node : this.orderedSequenceNodes) {
			String opType = node.getOpType();
			String name = node.getName();
			System.out.printf("[%02d] Node Name: %-30s OpType: %s\n", idx++, name, opType);
		}
		System.out.println("===========================================================");
	}


//	private void predecessors(Collection<Node> nodes, Graph graph, Node node) {
//		Collection<Node> set = graph.predecessors(node);
//		// 递归处理所有前驱节点
//		for (Node predecessorNode : set) {
//			this.predecessors(nodes, graph, predecessorNode);
//		}
//		// 将前驱节点加入列表
//		for (Node predecessor : set) {
//			this.addOrderedSequenceNode(nodes, predecessor);
//		}
//	}

	public void predecessors(Collection<Node> nodes, Graph graph, Node node) {
		Set<Node> visited = new HashSet<>();
		predecessors(nodes, graph, node, visited);
	}


	private void predecessors(Collection<Node> nodes, Graph graph, Node node, Set<Node> visited) {
		if (visited.contains(node)) {
			return;
		}
		visited.add(node);

		Collection<Node> set = graph.predecessors(node);
		for (Node predecessorNode : set) {
			this.predecessors(nodes, graph, predecessorNode, visited);
		}
		for (Node predecessor : set) {
			this.addOrderedSequenceNode(nodes, predecessor);
		}
	}



	private void addOrderedSequenceNode(Collection<Node> nodes, Node node) {
		if (this.contains(nodes, node) == false)
			nodes.add(node);
	}

	private boolean contains(Collection<Node> nodes, Node node) {
		for (Node existingNode : nodes) {
			if (node.equals(existingNode))
				return true;
		}
		return false;
	}
}
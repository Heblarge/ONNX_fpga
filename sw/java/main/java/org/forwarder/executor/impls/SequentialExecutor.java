/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.forwarder.executor.impls;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

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

public class SequentialExecutor<T_BK_TS> extends Executor<T_BK_TS> {

    private final Collection<Node> orderedSequenceNodes;

    public SequentialExecutor(Model model) {
        super(model);
        this.orderedSequenceNodes = this.toOrderedSequenceNodes(model.getGraph());
    }

    @Override
    public void execute(Session<T_BK_TS> session, OperatorSets opsets) {
        for (Node node : this.orderedSequenceNodes) {
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
        //在这里应该可以看到每个算子的输出
        // 遍历outputs集合中的每个输出，将其名称和对应的张量存储回session的中间输出结果中
        for (Output output : outputs.get()) {
            // session.intermediateOutputs 包含节点内静态参数，推理的中间结果在此追加
            session.putIntermediateOutput(output.getName(), output.getTensor());
        }
    }

    private Collection<Node> toOrderedSequenceNodes(Graph graph) {
        // Using LinkedList for efficient additions. The result will be a topologically sorted list of nodes.
        LinkedList<Node> orderedNodes = new LinkedList<>();
        // orderedNodes 最终排好序的节点

        // Set to track all visited nodes to avoid redundant processing.
        Set<Node> visited = new HashSet<>();
        // visited 用来记录所有已经访问过的节点的集合

        // Set to track nodes currently in the recursion stack to detect cycles in the graph.
        Set<Node> recursionStack = new HashSet<>();
        // 只记录在当前这一次深度搜索路径上的节点

        // Start the DFS traversal from all output nodes of the graph.
        for (GraphOutput graphOutput : graph.getOutputs()) {
            Node outputNode = graphOutput.getNode();
            if (!visited.contains(outputNode)) {
                topologicalSortUtil(outputNode, graph, orderedNodes, visited, recursionStack);
            }
        }

        return orderedNodes;
    }


    private void topologicalSortUtil(Node node, Graph graph, LinkedList<Node> orderedNodes, Set<Node> visited, Set<Node> recursionStack) {

        visited.add(node);
        recursionStack.add(node);
        // 标记当前节点

        // 递归访问所有前驱节点
        Collection<Node> predecessors = graph.predecessors(node);
        if (predecessors != null) {
            for (Node predecessor : predecessors) {
                // 如果前驱节点在当前路径上，说明有环
                if (recursionStack.contains(predecessor)) {
                    throw new IllegalStateException("Graph has a cycle, topological sort not possible. Cycle detected at node: " + predecessor.getName());
                }
                // 如果前驱节点还未被访问过，则对它进行递归
                if (!visited.contains(predecessor)) {
                    topologicalSortUtil(predecessor, graph, orderedNodes, visited, recursionStack);
                }
            }
        }
        recursionStack.remove(node);
        orderedNodes.add(node);
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
}
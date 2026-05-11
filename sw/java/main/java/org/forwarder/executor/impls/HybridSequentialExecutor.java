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
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.exchanges.GraphOutput;
import org.onnx4j.opsets.OperatorSets;

/**
 * 混合执行器：支持 HWAccelerated 和 DL4J 后端的自动回退
 * 当 HWAccelerated 不支持某个算子时，自动切换到 DL4J 后端计算
 */
public class HybridSequentialExecutor<T_BK_TS> extends Executor<T_BK_TS> {

    private final Collection<Node> orderedSequenceNodes;
    private final OperatorSets fallbackOpsets;  // 回退用的 DL4J OperatorSets
    private Set<String> unsupportedOps;          // 记录不支持的算子

    public HybridSequentialExecutor(Model model, OperatorSets fallbackOpsets) {
        super(model);
        this.orderedSequenceNodes = this.toOrderedSequenceNodes(model.getGraph());
        this.fallbackOpsets = fallbackOpsets;    // 注入 DL4J OperatorSets 用于回退
        this.unsupportedOps = new HashSet<>();
    }

    @Override
    public void execute(Session<T_BK_TS> session, OperatorSets opsets) {
        for (Node node : this.orderedSequenceNodes) {
            this.handle(session, opsets, node);
        }
        
        // 打印统计信息
        if (!unsupportedOps.isEmpty()) {
            System.out.println("\n[HybridExecutor] 以下算子已回退到 DL4J 后端：");
            for (String op : unsupportedOps) {
                System.out.println("  - " + op);
            }
        }
    }

    private void handle(Session<T_BK_TS> session, OperatorSets opsets, Node node) {
        Inputs inputs = new Inputs();
        for (String inputName : node.getInputNames()) {
            Input input = Input.wrap(inputName, node, session.getIntermediateOutput(inputName));
            inputs.append(input);
        }

        System.out.printf("[HybridExecutor] 执行节点: %-30s (OpType: %s)\n", node.getName(), node.getOpType());
        
        Outputs outputs;
        try {
            // 首先尝试使用 HWAccelerated 后端
            outputs = super.handle(session, opsets, node, inputs);
        } catch (UnsupportedOperationException e) {
            // 如果 HWAccelerated 不支持，回退到 DL4J
            System.out.printf("[HybridExecutor] HWAccelerated 不支持 %s，回退到 DL4J 后端\n", node.getOpType());
            unsupportedOps.add(node.getOpType());
            
            try {
                outputs = super.handle(session, fallbackOpsets, node, inputs);
            } catch (Exception dlException) {
                System.err.printf("[HybridExecutor] DL4J 后端也不支持 %s，执行失败\n", node.getOpType());
                throw new RuntimeException("Both HWAccelerated and DL4J backends failed for operator: " + node.getOpType(), dlException);
            }
        }

        // 保存输出结果
        for (Outputs.Output output : outputs.get()) {
            session.putIntermediateOutput(output.getName(), (T_BK_TS) output.getTensor());
        }
    }

    private Collection<Node> toOrderedSequenceNodes(Graph graph) {
        LinkedList<Node> orderedNodes = new LinkedList<>();
        Set<Node> visited = new HashSet<>();
        Set<Node> recursionStack = new HashSet<>();
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
        Collection<Node> predecessors = graph.predecessors(node);
        if (predecessors != null) {
            for (Node predecessor : predecessors) {
                if (recursionStack.contains(predecessor)) {
                    throw new IllegalStateException("Graph has a cycle, topological sort not possible.");
                }
                if (!visited.contains(predecessor)) {
                    topologicalSortUtil(predecessor, graph, orderedNodes, visited, recursionStack);
                }
            }
        }
        recursionStack.remove(node);
        orderedNodes.add(node);
    }

    /**
     * 获取统计信息：哪些算子被回退到 DL4J
     */
    public Set<String> getUnsupportedOps() {
        return new HashSet<>(unsupportedOps);
    }

}

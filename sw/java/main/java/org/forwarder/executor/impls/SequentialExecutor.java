package org.forwarder.executor.impls;

import com.google.protobuf.ByteString;
import org.forwarder.Session;
import org.forwarder.executor.Executor;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.onnx4j.Inputs;
import org.onnx4j.Inputs.Input;
import org.onnx4j.Model;
import org.onnx4j.Outputs;
import org.onnx4j.Outputs.Output;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.exchanges.GraphOutput;
import org.onnx4j.opsets.OperatorSets;
import org.onnx4j.prototypes.OnnxProto3.TensorProto;
import java.io.DataOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

// 这个版本是只保存最终的输出结果到data_n文件夹

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
        for (String inputName : node.getInputNames()) {
            Input input = Input.wrap(inputName, node, session.getIntermediateOutput(inputName));
            inputs.append(input);
        }

        System.out.printf("Executing Node: %-30s (OpType: %s)\n", node.getName(), node.getOpType());
        Outputs outputs = super.handle(session, opsets, node, inputs);

        for (Output output : outputs.get()) {
            // 将节点的输出存回会话中，供后续节点使用
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

}
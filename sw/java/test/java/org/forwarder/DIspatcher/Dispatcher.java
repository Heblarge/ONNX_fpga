package org.forwarder.DIspatcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import junit.framework.Assert;
import org.forwarder.Forwarder;
import org.forwarder.Model;
import org.forwarder.Config;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.exchanges.GraphOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;
import java.nio.ByteOrder;

public class Dispatcher {
    private static Logger logger = LoggerFactory.getLogger(Dispatcher.class);

    private final String modelPath;
    private final List<Instruction> instructions;

    public Dispatcher(String modelPath) {
        this.modelPath = modelPath;
        this.instructions = new ArrayList<>();
    }
    public List<Instruction> generateInstructions() throws FileNotFoundException, IOException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, OperationNotSupportedException {
        String absoluteModelPath = URLDecoder.decode(Dispatcher.class.getResource(modelPath).getFile(), "utf-8");
        assertNotNull(absoluteModelPath);
        logger.info("Loading model from: {}", absoluteModelPath);

        // 加载模型（类似 ForwarderTestCase 中）
        Config cfg = Config.builder()
                .setDebug(true)
                .setMemoryByteOrder(ByteOrder.LITTLE_ENDIAN)
                .build();

        Forwarder forwarder = new Forwarder();
        Model model = forwarder.load(absoluteModelPath, cfg);

        Graph graph = model.getGraph();
        Collection<Node> sortedNodes = this.topologicalSort(graph);

        // 为每个 Node 生成指令
        for (Node node : sortedNodes) {
            Instruction instr = new Instruction(
                    node.getOpType(),
                    node.getName(),  // nodeName
                    new ArrayList<>(Arrays.asList(node.getInputNames())),
                    new ArrayList<>(Arrays.asList(node.getOutputNames())),
                    new LinkedHashMap<>()  // 空属性表，后续可从 node.getAttrs() 提取
            );
            this.instructions.add(instr);
        }

        logger.info("Generated {} instructions", instructions.size());
        return this.instructions;
    }

    private Collection<Node> topologicalSort(Graph graph) {
        LinkedList<Node> nodes = new LinkedList<>();
        for (GraphOutput graphOutput : graph.getOutputs()) {
            Node outputNode = graphOutput.getNode();
            this.predecessors(nodes, graph, outputNode);
            this.addOrderedSequenceNode(nodes, outputNode); // 显式加入输出节点
        }
        return nodes;
    }

    private void predecessors(Collection<Node> nodes, Graph graph, Node node) {
        Collection<Node> preds = graph.predecessors(node);
        for (Node pred : preds) {
            this.predecessors(nodes, graph, pred);
        }
        for (Node pred : preds) {
            this.addOrderedSequenceNode(nodes, pred);
        }
    }

    private void addOrderedSequenceNode(Collection<Node> nodes, Node node) {
        if (!this.contains(nodes, node))
            nodes.add(node);
    }

    private boolean contains(Collection<Node> nodes, Node target) {
        for (Node n : nodes) {
            if (n.equals(target))
                return true;
        }
        return false;
    }




//    private Collection<Node> topologicalSort(Graph graph) {
//        Set<Node> sorted = new LinkedHashSet<>();
//        for (GraphOutput graphOutput : graph.getOutputs()) {
//            dfs(graphOutput.getNode(), graph, sorted);
//        }
//        return sorted;
//    }
//
//    private void dfs(Node node, Graph graph, Set<Node> sorted) {
//        for (Node pred : graph.predecessors(node)) {
//            dfs(pred, graph, sorted);
//        }
//        sorted.add(node);
//    }


    public static void assertNotNull(Object object) {
        Assert.assertNotNull(object);
    }
}

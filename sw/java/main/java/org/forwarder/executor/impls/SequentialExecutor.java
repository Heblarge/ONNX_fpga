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

import org.forwarder.Session;
import org.forwarder.executor.Executor;
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

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class SequentialExecutor<T_BK_TS> extends Executor<T_BK_TS> {

    private final Collection<Node> orderedSequenceNodes;
    private final File JAVA_OUTPUTS_DIR = new File("java_outputs");
    private static final String OUTPUT_LOG_FILE = "operator_outputs.log"; // 使用相对路径，更灵活

    public SequentialExecutor(Model model) {
        super(model);
        this.orderedSequenceNodes = this.toOrderedSequenceNodes(model.getGraph());
    }

    @Override
    public void execute(Session<T_BK_TS> session, OperatorSets opsets) {
        // 自动清理旧的输出目录和日志文件
        if (JAVA_OUTPUTS_DIR.exists()) {
            for (File file : JAVA_OUTPUTS_DIR.listFiles()) {
                file.delete();
            }
        }
        JAVA_OUTPUTS_DIR.mkdirs();

        try {
            // 清空日志文件，准备本次运行的写入
            new FileWriter(OUTPUT_LOG_FILE, false).close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Node node : this.orderedSequenceNodes) {
            this.handle(session, opsets, node);
        }
    }

    private void handle(Session<T_BK_TS> session, OperatorSets opsets, Node node) {
        // 1. 准备输入
        Inputs inputs = new Inputs();
        for (String inputName : node.getInputNames()) {
            Input input = Input.wrap(inputName, node, session.getIntermediateOutput(inputName));
            inputs.append(input);
        }

        // 2. 执行计算
        Outputs outputs = super.handle(session, opsets, node, inputs);

        // 3. 记录日志并更新Session状态
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_LOG_FILE, true))) {
            writer.write("---- Executed Node: " + node.getName() + " (OpType: " + node.getOpType() + ") ----\n");

            for (Output output : outputs.get()) {
                // 必须先将输出存入session，供后续节点使用
                session.putIntermediateOutput(output.getName(), (T_BK_TS) output.getTensor());

                String tensorName = output.getName();
                INDArray tensorData = (INDArray) output.getTensor();

                // 立刻将正确的二进制数据保存到文件
                String safeFileName = tensorName.replace('/', '_').replace(':', '_') + ".bin";
                try {
                    // 确保在调用 toString() 之前保存
                    saveTensorAsBinary(tensorData, new File(JAVA_OUTPUTS_DIR, safeFileName));
                } catch (IOException e) {
                    System.err.println("Failed to save intermediate tensor as binary: " + tensorName);
                    e.printStackTrace();
                }

                writer.write("Output Name: " + tensorName + "\n");
                writer.write("Output Tensor: \n" + tensorData.toString() + "\n");

            }

            writer.write("-----------------------------------------------------------\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveTensorAsBinary(INDArray tensor, File file) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            // Write shape info
            dos.writeInt(tensor.rank());
            for (long dim : tensor.shape()) {
                dos.writeLong(dim);
            }

            // Manually extract data in C-order to bypass potential ND4J bugs
            long[] shape = tensor.shape();
            long[] strides = tensor.stride();
            long length = tensor.length();

            for (int i = 0; i < length; i++) {
                long[] coords = new long[tensor.rank()];
                long temp = i;
                // Calculate coordinates from the linear index (C-order logic)
                for (int d = tensor.rank() - 1; d >= 0; d--) {
                    coords[d] = temp % shape[d];
                    temp /= shape[d];
                }

                dos.writeFloat(tensor.getFloat(coords));
            }
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

    public void printExecutionSequence() {
        // ... (此方法保持不变，但为了完整性，我们把它也放进来) ...
        System.out.println("==== Execution Sequence of Nodes (Topological Order) ====");
        String fileName = "execution_order.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (Node node : this.orderedSequenceNodes) {
                for (String outputName : node.getOutputNames()) {
                    writer.write(outputName);
                    writer.newLine();
                }
            }
            System.out.println("Successfully saved execution order to: " + new File(fileName).getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save execution_order.txt");
            e.printStackTrace();
        }
        int idx = 0;
        for (Node node : this.orderedSequenceNodes) {
            String opType = node.getOpType();
            String name = node.getName();
            System.out.printf("[%02d] Node Name: %-30s OpType: %s\n", idx++, name, opType);
        }
        System.out.println("===========================================================");
    }
}
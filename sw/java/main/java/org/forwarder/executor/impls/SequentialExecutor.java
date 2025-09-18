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
    private final File JAVA_OUTPUTS_DIR = new File("/home/user/Workspace/livehps_1/onnx_debug_py/java_hw_each_layer_outputs");

    // 新增一个执行计数器，用于创建 data1, data2, ... 目录
    private int executionCount = 0;

    public SequentialExecutor(Model model) {
        super(model);
        this.orderedSequenceNodes = this.toOrderedSequenceNodes(model.getGraph());
    }

    @Override
    public void execute(Session<T_BK_TS> session, OperatorSets opsets) {
        // 每次执行时，计数器加一
        this.executionCount++;

        // 根据当前执行次数创建独立的子目录，例如 "java_outputs/data1"
        File currentOutputDataDir = new File(JAVA_OUTPUTS_DIR, "data" + this.executionCount);
        if (currentOutputDataDir.exists()) {
            for (File file : currentOutputDataDir.listFiles()) {
                file.delete();
            }
        }
        currentOutputDataDir.mkdirs();
        System.out.println("输出将被保存到: " + currentOutputDataDir.getAbsolutePath());

        // 1. 依次执行图中的所有节点
        for (Node node : this.orderedSequenceNodes) {
            this.handle(session, opsets, node, currentOutputDataDir);
        }

//        // 2. 所有节点执行完毕后，保存模型的最终输出到当前执行的子目录
//        System.out.println("--- 推理完成，正在保存最终输出... ---");
//        for (GraphOutput graphOutput : this.model.getGraph().getOutputs()) {
//            String outputName = graphOutput.getName();
//            INDArray tensorData = (INDArray) session.getIntermediateOutput(outputName);
//
//            if (tensorData != null) {
//                String sanitizedName = outputName.replace('/', '_').replace(':', '_');
//                String finalFileName = "output_" + sanitizedName + ".pb";
//
//                // 将文件保存到新建的 dataN 子目录中
//                File outputFile = new File(currentOutputDataDir, finalFileName);
//                try {
//                    saveTensorAsPb(tensorData, outputFile);
//                    System.out.println("  - 已保存: " + outputFile.getName());
//                } catch (IOException e) {
//                    System.err.println("保存最终输出张量失败: " + outputName);
//                    e.printStackTrace();
//                }
//            } else {
//                System.err.println("警告：在会话中找不到最终输出张量: " + outputName);
//            }
//        }
//        System.out.println("--- 所有最终输出保存完毕 ---");
        System.out.println("--- 所有节点执行和保存完毕 ---");
    }

    private void handle(Session<T_BK_TS> session, OperatorSets opsets, Node node, File outputDir) {
        Inputs inputs = new Inputs();
        for (String inputName : node.getInputNames()) {
            Input input = Input.wrap(inputName, node, session.getIntermediateOutput(inputName));
            inputs.append(input);
        }

        System.out.printf("正在执行节点: %-30s (OpType: %s)\n", node.getName(), node.getOpType());
        Outputs outputs = super.handle(session, opsets, node, inputs);

        for (Output output : outputs.get()) {
            // 将节点的输出存回会话中，供后续节点使用
            session.putIntermediateOutput(output.getName(), (T_BK_TS) output.getTensor());

            // 同时，将这个输出张量保存到文件中
            String outputName = output.getName();
            INDArray tensorData = (INDArray) output.getTensor();

            if (tensorData != null) {
                String sanitizedName = outputName.replace('/', '_').replace(':', '_');
                String fileName = sanitizedName + ".bin";
                File outputFile = new File(outputDir, fileName);

                try {
                    //saveTensorAsPb(tensorData, outputFile);
                    saveTensorAsBinary(tensorData, outputFile);
                    System.out.println("  - 已保存中间层输出: " + outputFile.getName());
                } catch (IOException e) {
                    System.err.println("保存中间层输出张量失败: " + outputName);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 将 INDArray 转换为 ONNX TensorProto 并保存为 .pb 文件。
     */
//    private void saveTensorAsPb(INDArray tensor, File file) throws IOException {
//        TensorProto.Builder builder = TensorProto.newBuilder();
//        for (long dim : tensor.shape()) {
//            builder.addDims(dim);
//        }
//        builder.setDataType(mapDl4jDataTypeToOnnx(tensor.dataType()).getNumber());
//        ByteBuffer byteBuffer = ByteBuffer.allocate((int) (tensor.length() * tensor.dataType().width()))
//                .order(ByteOrder.LITTLE_ENDIAN);
//        switch(tensor.dataType()) {
//            case FLOAT:
//                byteBuffer.asFloatBuffer().put(tensor.data().asNioFloat().rewind());
//                break;
//            default:
//                byteBuffer.asFloatBuffer().put(tensor.data().asNioFloat().rewind());
//                break;
//        }
//        builder.setRawData(ByteString.copyFrom(byteBuffer));
//        TensorProto tensorProto = builder.build();
//        try (FileOutputStream fos = new FileOutputStream(file)) {
//            tensorProto.writeTo(fos);
//        }
//    }

    private void saveTensorAsBinary(INDArray tensor, File file) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            // Write shape info
            dos.writeInt(tensor.rank());
            for (long dim : tensor.shape()) {
                dos.writeLong(dim);
            }

            // Manually extract data in C-order
            long[] shape = tensor.shape();
            long length = tensor.length();
            int rank = tensor.rank();

            for (int i = 0; i < length; i++) {
                long[] coords = new long[rank];
                long temp = i;
                // Calculate coordinates from the linear index (C-order logic)
                for (int d = rank - 1; d >= 0; d--) {
                    if (shape[d] > 0) {
                        coords[d] = temp % shape[d];
                        temp /= shape[d];
                    } else {
                        coords[d] = 0;
                    }
                }
                dos.writeFloat(tensor.getFloat(coords));
            }
        }
    }


    private void saveTensorAsPb(INDArray tensor, File file) throws IOException {
        TensorProto.Builder builder = TensorProto.newBuilder();
        for (long dim : tensor.shape()) {
            builder.addDims(dim);
        }
        builder.setDataType(mapDl4jDataTypeToOnnx(tensor.dataType()).getNumber());

        // 按行优先顺序逐个读取元素
        int length = (int) tensor.length();
        long[] shape = tensor.shape();
        int rank = tensor.rank();

        // 创建一个保证小端序（ONNX raw_data 标准）的 ByteBuffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(length * 4).order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();

        // 按照标准的行优先（C-order）顺序遍历所有元素
        for (int i = 0; i < length; i++) {
            // 从行优先的线性索引 i，反向计算出逻辑坐标 coords
            long[] coords = new long[rank];
            long temp = i;
            for (int d = rank - 1; d >= 0; d--) {
                if (shape[d] > 0) {
                    coords[d] = temp % shape[d];
                    temp /= shape[d];
                } else {
                    coords[d] = 0;
                }
            }

            // 使用 getFloat(coords) 方法，该方法可以智能地处理C序或F序的内部布局
            float value = tensor.getFloat(coords);
            // 将正确顺序的元素放入新的 buffer
            floatBuffer.put(value);
        }

        builder.setRawData(ByteString.copyFrom(byteBuffer));
        TensorProto tensorProto = builder.build();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            tensorProto.writeTo(fos);
        }
    }


    private TensorProto.DataType mapDl4jDataTypeToOnnx(DataType dl4jType) {
        switch (dl4jType) {
            case FLOAT: return TensorProto.DataType.FLOAT;
            case DOUBLE: return TensorProto.DataType.DOUBLE;
            case INT: return TensorProto.DataType.INT32;
            case LONG: return TensorProto.DataType.INT64;
            default: return TensorProto.DataType.UNDEFINED;
        }
    }

    // ... toOrderedSequenceNodes, topologicalSortUtil, printExecutionSequence 方法保持不变 ...
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
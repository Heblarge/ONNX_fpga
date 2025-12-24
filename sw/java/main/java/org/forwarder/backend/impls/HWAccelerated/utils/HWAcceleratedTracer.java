package org.forwarder.backend.impls.HWAccelerated.utils;

import Accelerator.InstJavaTODO;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.node.Attributes;
import org.onnx4j.model.graph.node.Attribute;
import org.onnx4j.Tensor;
import org.onnx4j.Inputs;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.ArrayDeque;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class HWAcceleratedTracer {

    private static final HWAcceleratedTracer INSTANCE = new HWAcceleratedTracer();
    private final AtomicInteger executionOrderCounter = new AtomicInteger(0);
    private final List<NodeTrace> completedTraces = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Long, NodeTrace> activeTraces = new ConcurrentHashMap<>();
    private final ThreadLocal<Deque<String>> batchContext = ThreadLocal.withInitial(ArrayDeque::new);

    private HWAcceleratedTracer() {}

    public static HWAcceleratedTracer getInstance() {
        return INSTANCE;
    }

    public void reset() {
        executionOrderCounter.set(0);
        completedTraces.clear();
        activeTraces.clear();
        batchContext.get().clear();
    }

    public static class DimInfo {
        public int totalPaddedSize = 0;
        public int tileCapUsed = 0;
        public boolean tiled = false;
    }

    public static class TilingInfo {
        public String status = "NOT_APPLICABLE";
        public String strategy = "NONE";
        public int totalTilesSentToHw = 0;
        public final DimInfo mDim = new DimInfo();
        public final DimInfo nDim = new DimInfo();
        public final DimInfo kDim = new DimInfo();
    }

    private static class TileTrace {
        public final String tileIndex;

        public TileTrace(String tileIndex) {
            this.tileIndex = tileIndex;
        }
    }

    private static class NodeTrace {
        public final String nodeName;
        public final String nodeType;
        public final int executionOrder;
        public final long startTime;
        public Map<String, Object> attributes;
        public List<String> inputShapes = new ArrayList<>();
        public String outputShape = "[Unknown]";
        public long durationMs = 0;
        public final TilingInfo tilingInfo = new TilingInfo();
        public final List<TileTrace> tiles = Collections.synchronizedList(new ArrayList<>());

        public InstJavaTODO commonInstruction = null;

        public NodeTrace(Node node) {
            this.nodeName = node.getName();
            this.nodeType = node.getOpType();
            this.executionOrder = HWAcceleratedTracer.getInstance().executionOrderCounter.incrementAndGet();
            this.startTime = System.currentTimeMillis();
        }

        public String getReportAsJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("  {\n");
            sb.append(String.format("    \"node_name\": \"%s\",\n", escapeJson(nodeName)));
            sb.append(String.format("    \"node_type\": \"%s\",\n", escapeJson(nodeType)));
            sb.append(String.format("    \"execution_order\": %d,\n", executionOrder));
            sb.append("    \"attributes\": {\n");
            if (attributes != null && !attributes.isEmpty()) {
                int attrCount = 0;
                List<String> sortedKeys = new ArrayList<>(attributes.keySet());
                Collections.sort(sortedKeys);
                for (String key : sortedKeys) {
                    if (attrCount++ > 0) sb.append(",\n");
                    sb.append(String.format("      \"%s\": %s", escapeJson(key), formatJsonValue(attributes.get(key))));
                }
                sb.append("\n");
            }
            sb.append("    },\n");
            sb.append("    \"input_shapes\": [\n");
            for (int i = 0; i < inputShapes.size(); i++) {
                sb.append(String.format("      \"%s\"", escapeJson(inputShapes.get(i))));
                if (i < inputShapes.size() - 1) sb.append(",\n");
            }
            sb.append("\n    ],\n");
            sb.append(String.format("    \"output_shape\": \"%s\",\n", escapeJson(outputShape)));
            // sb.append(String.format("    \"duration_ms\": %d,\n", durationMs));
            sb.append("    \"tiling_info\": {\n");
            sb.append(String.format("      \"status\": \"%s\",\n", escapeJson(tilingInfo.status)));
            sb.append(String.format("      \"strategy\": \"%s\",\n", escapeJson(tilingInfo.strategy)));
            sb.append(String.format("      \"total_tiles_sent_to_hw\": %d,\n", tilingInfo.totalTilesSentToHw));
            sb.append(serializeDimInfo("m_dim", tilingInfo.mDim)).append(",\n");
            sb.append(serializeDimInfo("n_dim", tilingInfo.nDim)).append(",\n");
            sb.append(serializeDimInfo("k_dim", tilingInfo.kDim)).append("\n");
            sb.append("    },\n");

            if (commonInstruction != null) {
                sb.append("    \"instruction\": {\n");
                sb.append(serializeInstruction(commonInstruction));
                sb.append("\n    }\n");
            } else {
                sb.append("    \"instruction\": null,\n");
            }

            // sb.append("    \"tiles_sent_to_hw\": [\n");
//            synchronized (tiles) {
//                for (int i = 0; i < tiles.size(); i++) {
//                    TileTrace tile = tiles.get(i);
//                    sb.append("      {\n");
//                    sb.append(String.format("        \"tile_index\": \"%s\"\n", escapeJson(tile.tileIndex)));
//                    sb.append("      }");
//                    if (i < tiles.size() - 1) sb.append(",\n");
//                }
//            }
            // sb.append("\n    ]\n");
            sb.append("  }");
            return sb.toString();
        }

        private String serializeInstruction(InstJavaTODO inst) {
            if (inst == null) return "";
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("      \"UID\": %d,\n", inst.UID));
            sb.append(String.format("      \"matrixOperation\": \"%s\",\n", escapeJson(inst.matrixOperation)));
            sb.append(String.format("      \"shiftLeft_AfterMatrixOperation\": %d,\n", inst.shiftLeft_AfterMatrixOperation));
            sb.append(String.format("      \"doTranspose\": %b,\n", inst.doTranspose));
            sb.append(String.format("      \"activationFunction\": \"%s\",\n", escapeJson(inst.activationFunction)));
            sb.append(String.format("      \"shiftLeft_AfterActivation\": %d,\n", inst.shiftLeft_AfterActivation));
            sb.append(String.format("      \"input0Address\": %d,\n", inst.input0Address));
            sb.append(String.format("      \"input1Address\": %d,\n", inst.input1Address));
            sb.append(String.format("      \"outputAddress\": %d,\n", inst.outputAddress));
            sb.append(String.format("      \"input0Shape0\": %d,\n", inst.input0Shape0));
            sb.append(String.format("      \"input0Shape1\": %d,\n", inst.input0Shape1));
            sb.append(String.format("      \"input1Shape1\": %d", inst.input1Shape1));

            return sb.toString();
        }

        private String serializeDimInfo(String name, DimInfo dim) {
            return String.format(
                    "      \"%s\": {\n" +
                            "        \"total_padded_size\": %d,\n" +
                            "        \"tile_cap_used\": %d,\n" +
                            "        \"tiled\": %b\n" +
                            "      }",
                    name, dim.totalPaddedSize, dim.tileCapUsed, dim.tiled
            );
        }

        private String formatJsonValue(Object value) {
            if (value == null) {
                return "null";
            }
            if (value instanceof String) {
                return "\"" + escapeJson(value.toString()) + "\"";
            }
            if (value instanceof Number || value instanceof Boolean) {
                return value.toString();
            }
            if (value instanceof List) {
                StringBuilder arrayBuilder = new StringBuilder();
                arrayBuilder.append("[");
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    arrayBuilder.append(formatJsonValue(list.get(i)));
                    if (i < list.size() - 1) {
                        arrayBuilder.append(", ");
                    }
                }
                arrayBuilder.append("]");
                return arrayBuilder.toString();
            }
            return "\"" + escapeJson(value.toString()) + "\"";
        }

        private String escapeJson(String s) {
            if (s == null) return "null";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    public void startNode(Node node, Inputs inputs, Attributes attributes) {
        NodeTrace trace = new NodeTrace(node);
        trace.attributes = formatAttributes(attributes);
        trace.inputShapes = formatInputs(inputs);
        activeTraces.put(Thread.currentThread().getId(), trace);
    }

    private List<String> formatInputs(Inputs inputs) {
        List<String> shapes = new ArrayList<>();
        if (inputs == null) {
            return shapes;
        }
        try {
            Inputs.Input[] inputObjects = inputs.get();
            if (inputObjects != null) {
                for (Inputs.Input input : inputObjects) {
                    if (input == null) continue;
                    Object tensorObj = input.getTensor();
                    if (tensorObj == null) continue;

                    if (tensorObj instanceof INDArray) {
                        shapes.add(Arrays.toString(((INDArray) tensorObj).shape()));
                    } else if (tensorObj instanceof Tensor) {
                        Tensor t = (Tensor) tensorObj;
                        if (t.getValueInfo() != null) {
                            shapes.add(t.getValueInfo().getShape().toString());
                        } else {
                            shapes.add("[Shape Unknown]");
                        }
                    } else {
                        shapes.add("[Unknown Tensor Type: " + tensorObj.getClass().getName() + "]");
                    }
                }
            }
        } catch (Exception e) {
            shapes.add("[Error reading inputs: " + e.getMessage() + "]");
        }
        return shapes;
    }

    private Map<String, Object> formatAttributes(Attributes attributes) {
        Map<String, Object> publicMap = new HashMap<>();
        if (attributes == null) {
            return publicMap;
        }
        try {
            Field attrsField = Attributes.class.getDeclaredField("attrs");
            attrsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, Attribute<?>> privateMap = (Map<String, Attribute<?>>) attrsField.get(attributes);

            if (privateMap != null) {
                for (Map.Entry<String, Attribute<?>> entry : privateMap.entrySet()) {
                    if (entry.getValue() != null) {
                        Method getValueMethod = entry.getValue().getClass().getMethod("getValue");
                        publicMap.put(entry.getKey(), getValueMethod.invoke(entry.getValue()));
                    } else {
                        publicMap.put(entry.getKey(), null);
                    }
                }
            }
        } catch (Exception e) {
            publicMap.put("TRACER_ERROR", "Failed to access attributes via reflection: " + e.getMessage());
        }
        return publicMap;
    }

    public TilingInfo getActiveTilingInfo() {
        NodeTrace trace = activeTraces.get(Thread.currentThread().getId());
        if (trace != null) {
            return trace.tilingInfo;
        }
        return new TilingInfo();
    }

    public void pushBatchContext(String context) {
        this.batchContext.get().push(context);
    }

    public void popBatchContext() {
        Deque<String> stack = this.batchContext.get();
        if (stack != null && !stack.isEmpty()) {
            stack.pop();
        }
    }

    public void addTile(String tileIndex, InstJavaTODO instruction) {
        NodeTrace trace = activeTraces.get(Thread.currentThread().getId());
        if (trace != null) {
            String contextPrefix = String.join(" -> ", this.batchContext.get());
            String finalTileIndex = contextPrefix.isEmpty() ? tileIndex : (contextPrefix + " -> " + tileIndex);

            if (trace.commonInstruction == null) {
                trace.commonInstruction = instruction;
            }

            trace.tiles.add(new TileTrace(finalTileIndex));
            trace.tilingInfo.totalTilesSentToHw++;
        }
    }

    public void endNode(Object output) {
        long threadId = Thread.currentThread().getId();
        NodeTrace trace = activeTraces.remove(threadId);
        if (trace != null) {
            trace.durationMs = System.currentTimeMillis() - trace.startTime;
            try {
                if (output instanceof INDArray) {
                    trace.outputShape = Arrays.toString(((INDArray) output).shape());
                } else if (output instanceof Tensor) {
                    trace.outputShape = ((Tensor) output).getValueInfo().getShape().toString();
                } else if (output != null) {
                    trace.outputShape = "[Output is not INDArray or Tensor: " + output.getClass().getName() + "]";
                } else {
                    trace.outputShape = "[Output is Null]";
                }
            } catch (Exception e) {
                trace.outputShape = "[Error reading output shape]";
            }
            completedTraces.add(trace);
        }
    }

    public String getReportAsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        List<NodeTrace> sortedTraces = new ArrayList<>(completedTraces);
        Collections.sort(sortedTraces, Comparator.comparingInt(t -> t.executionOrder));
        for (int i = 0; i < sortedTraces.size(); i++) {
            sb.append(sortedTraces.get(i).getReportAsJson());
            if (i < sortedTraces.size() - 1) {
                sb.append(",\n");
            }
        }
        sb.append("\n]\n");
        return sb.toString();
    }
}
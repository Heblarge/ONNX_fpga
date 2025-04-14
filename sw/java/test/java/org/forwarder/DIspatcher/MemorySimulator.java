package org.forwarder.DIspatcher;


import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class MemorySimulator {
    private final Map<String, MemoryEntry> memoryMap = new LinkedHashMap<>();

    public void allocate(String tensorName, Object data, String dataType, List<Integer> shape, String producerNode, boolean isConstant) {
        memoryMap.put(tensorName, new MemoryEntry(tensorName, data, dataType, shape, producerNode, isConstant));
    }

    public MemoryEntry get(String tensorName) {
        return memoryMap.get(tensorName);
    }

    public List<MemoryEntry> getByProducer(String nodeName) {
        return memoryMap.values().stream()
                .filter(entry -> entry.producerNode.equals(nodeName))
                .collect(Collectors.toList());
    }

    public void dumpMemoryStatus() {
        for (var entry : memoryMap.values()) {
            System.out.printf("Tensor: %s, shape: %s, from: %s, constant: %s\n",
                    entry.tensorName, entry.shape, entry.producerNode, entry.isConstant);
        }
    }
}


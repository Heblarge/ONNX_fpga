package org.forwarder.DIspatcher;

import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Instruction implements Serializable{

    private static final long serialVersionUID = 1L;

    /** 算子类型，如 Conv、Relu、Reshape */
    @Getter
    private final String opType;

    /** 输入键名列表 */
    @Getter
    private final List<String> inputs;

    /** 输出键名列表 */
    @Getter
    private final List<String> outputs;

    /** 算子属性（可选），如 axis、transB、keepdims 等 */
    private final Map<String, Object> attributes;

    /** 可选：节点名称（ONNX 中的 name 字段） */
    @Getter
    private final String nodeName;

    // 更多...

    public Instruction(String opType, List<String> inputs, List<String> outputs) {
        this(opType, null, inputs, outputs, null);
    }

    public Instruction(String opType, String nodeName, List<String> inputs, List<String> outputs,
                       Map<String, Object> attributes) {
        this.opType = opType;
        this.nodeName = nodeName;
        this.inputs = inputs;
        this.outputs = outputs;
        this.attributes = attributes != null ? attributes : new LinkedHashMap<>();
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public String toString() {
        return String.format("Instruction{node=%s, opType=%s, inputs=%s, outputs=%s, attrs=%s}",
                nodeName != null ? nodeName : "<anonymous>",
                opType, inputs, outputs, attributes.keySet());
    }
}

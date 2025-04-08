package org.onnx4j.opsets.domain.aiOnnx.v13;

import org.onnx4j.opsets.domain.aiOnnx.v12.AiOnnxOperatorV12;

public interface AiOnnxOperatorV13 extends AiOnnxOperatorV12 {
    public default long getVersion() {
        return 13L;
    }

}

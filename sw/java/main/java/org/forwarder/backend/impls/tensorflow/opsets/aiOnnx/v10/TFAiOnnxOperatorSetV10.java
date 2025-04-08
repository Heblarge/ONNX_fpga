package org.forwarder.backend.impls.tensorflow.opsets.aiOnnx.v10;

import org.forwarder.backend.impls.tensorflow.opsets.aiOnnx.v9.TFAiOnnxOperatorSetV9;
import org.onnx4j.opsets.domain.aiOnnx.v10.AiOnnxOpsetInitializerV10;

public class TFAiOnnxOperatorSetV10 extends TFAiOnnxOperatorSetV9 implements AiOnnxOpsetInitializerV10 {
    public TFAiOnnxOperatorSetV10() {
        super(1, "", "", 10L, "ONNX OPSET-V10 USING TENSORFLOW BACKEND");
    }
    public TFAiOnnxOperatorSetV10(int irVersion, String irVersionPrerelease, String irBuildMetadata, long opsetVersion,
                                 String docString) {
        super(irVersion, irVersionPrerelease, irBuildMetadata, opsetVersion, docString);
    }
}

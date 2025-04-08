package org.forwarder.backend.impls.tensorflow.opsets.aiOnnx.v12;

import org.forwarder.backend.impls.tensorflow.opsets.aiOnnx.v11.TFAiOnnxOperatorSetV11;
import org.onnx4j.opsets.domain.aiOnnx.v11.ops.SoftmaxV11;
import org.onnx4j.opsets.domain.aiOnnx.v12.AiOnnxOpsetInitializerV12;
import org.onnx4j.opsets.domain.aiOnnx.v12.ops.ReduceMaxV12;


public class TFAiOnnxOperatorSetV12 extends TFAiOnnxOperatorSetV11 implements AiOnnxOpsetInitializerV12 {

    public TFAiOnnxOperatorSetV12() {
        super(1, "", "", 12L, "ONNX OPSET-V12 USING TENSORFLOW BACKEND");
    }
    public TFAiOnnxOperatorSetV12(int irVersion, String irVersionPrerelease, String irBuildMetadata, long opsetVersion,
                                  String docString) {
        super(irVersion, irVersionPrerelease, irBuildMetadata, opsetVersion, docString);
    }

    @Override
    public ReduceMaxV12 getReduceMaxV12() {
        // TODO Auto-generated method stub
        return null;
    }

}

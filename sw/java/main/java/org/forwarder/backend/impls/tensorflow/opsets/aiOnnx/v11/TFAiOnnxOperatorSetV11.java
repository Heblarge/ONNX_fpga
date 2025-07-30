package org.forwarder.backend.impls.tensorflow.opsets.aiOnnx.v11;

import org.forwarder.backend.impls.tensorflow.opsets.aiOnnx.v10.TFAiOnnxOperatorSetV10;
import org.onnx4j.opsets.domain.aiOnnx.v11.AiOnnxOpsetInitializerV11;
import org.onnx4j.opsets.domain.aiOnnx.v11.ops.ReduceMaxV11;
import org.onnx4j.opsets.domain.aiOnnx.v11.ops.SoftmaxV11;
import org.onnx4j.opsets.domain.aiOnnx.v11.ops.SqueezeV11;
import org.onnx4j.opsets.domain.aiOnnx.v11.ops.*;


public class TFAiOnnxOperatorSetV11 extends TFAiOnnxOperatorSetV10 implements AiOnnxOpsetInitializerV11 {

    public TFAiOnnxOperatorSetV11() {
        super(1, "", "", 11L, "ONNX OPSET-V11 USING TENSORFLOW BACKEND");
    }
    public TFAiOnnxOperatorSetV11(int irVersion, String irVersionPrerelease, String irBuildMetadata, long opsetVersion,
                                  String docString) {
        super(irVersion, irVersionPrerelease, irBuildMetadata, opsetVersion, docString);
    }

    @Override
    public SoftmaxV11 getSoftmaxV11() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public SqueezeV11 getSqueezeV11() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public UnsqueezeV11 getUnsqueezeV11() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ReduceMaxV11 getReduceMaxV11() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public GemmV11 getGemmV11() {
        // TODO Auto-generated method stub
        return null;
    }
}

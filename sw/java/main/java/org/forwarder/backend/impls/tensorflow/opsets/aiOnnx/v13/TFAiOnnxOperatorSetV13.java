package org.forwarder.backend.impls.tensorflow.opsets.aiOnnx.v13;

import org.forwarder.backend.impls.tensorflow.opsets.aiOnnx.v12.TFAiOnnxOperatorSetV12;
import org.onnx4j.opsets.domain.aiOnnx.v12.ops.ReduceMaxV12;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOpsetInitializerV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.*;

public class TFAiOnnxOperatorSetV13 extends TFAiOnnxOperatorSetV12 implements AiOnnxOpsetInitializerV13 {
    public TFAiOnnxOperatorSetV13() {
        super(1, "", "", 13L, "ONNX OPSET-V13 USING TENSORFLOW BACKEND");
    }
    public TFAiOnnxOperatorSetV13(int irVersion, String irVersionPrerelease, String irBuildMetadata, long opsetVersion,
                                  String docString) {
        super(irVersion, irVersionPrerelease, irBuildMetadata, opsetVersion, docString);
    }

    @Override
    public ReshapeV13 getReshapeV13() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ConcatV13 getConcatV13() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SliceV13 getSliceV13() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AddV13 getAddV13() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SubV13 getSubV13() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExpV13 getExpV13() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LogV13 getLogV13() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ReluV13 getReluV13() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SoftplusV13 getSoftplusV13() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public QuantizeLinearV13 getQuantizeLinearV13() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DequantizeLinearV13 getDequantizeLinearV13() {
        // TODO Auto-generated method stub
        return null;
    }


}

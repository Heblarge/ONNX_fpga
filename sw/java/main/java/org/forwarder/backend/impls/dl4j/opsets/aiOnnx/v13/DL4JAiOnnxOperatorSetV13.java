package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v12.DL4JAiOnnxOperatorSetV12;
import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops.DL4JConcatV13;
import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops.DL4JReshapeV13;
import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops.DL4JSliceV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOpsetInitializerV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ConcatV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ReshapeV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.SliceV13;

public class DL4JAiOnnxOperatorSetV13 extends DL4JAiOnnxOperatorSetV12 implements AiOnnxOpsetInitializerV13 {
    @Override
    public ReshapeV13 getReshapeV13() {
        return new DL4JReshapeV13();
    }

    public ConcatV13 getConcatV13(){
        return new DL4JConcatV13();
    }
    public SliceV13 getSliceV13(){
        return new DL4JSliceV13();
    }

    public DL4JAiOnnxOperatorSetV13() {
        this(1, "", "", 13L, "ONNX OPSET-V13 USING DL4J BACKEND");
    }

    public DL4JAiOnnxOperatorSetV13(int irVersion, String irVersionPrerelease, String irBuildMetadata, long opsetVersion,
                                   String docString) {
        super(irVersion, irVersionPrerelease, irBuildMetadata, opsetVersion, docString);
    }
}

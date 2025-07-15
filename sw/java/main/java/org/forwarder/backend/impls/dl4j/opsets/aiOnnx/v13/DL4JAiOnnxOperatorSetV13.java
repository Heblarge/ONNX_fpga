package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v12.DL4JAiOnnxOperatorSetV12;
import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops.*;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOpsetInitializerV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.*;

public class DL4JAiOnnxOperatorSetV13 extends DL4JAiOnnxOperatorSetV12 implements AiOnnxOpsetInitializerV13 {
    @Override
    public ReshapeV13          getReshapeV13() {
        return new          DL4JReshapeV13();
    }
    public ConcatV13           getConcatV13(){
        return new           DL4JConcatV13();
    }
    public SliceV13            getSliceV13(){
        return new            DL4JSliceV13();
    }
    public CastV13            getCastV13(){return new                DL4JCastV13();}
    public ExpandV13            getExpandV13(){return new            DL4JExpandV13();}
    public TransposeV13            getTransposeV13(){return new            DL4JTransposeV13();}
    public NegV13            getNegV13(){return new            DL4JNegV13();}
    public TileV13            getTileV13(){return new            DL4JTileV13();}
    public ReduceMaxV13        getReduceMaxV13(){return new          DL4JReduceMaxV13();}
    public MaxV13             getMaxV13(){return new                DL4JMaxV13();}
    public GreaterV13          getGreaterV13(){return new            DL4JGreaterV13();}
    public MatMulV13           getMatMulV13(){return new             DL4JMatMulV13();}
    public GeMMV13             getGeMMV13(){
        return new             DL4JGeMMV13();
    }
    public AddV13              getAddV13(){
        return new              DL4JAddV13();
    }
    public SubV13              getSubV13(){
        return new              DL4JSubV13();
    }
    public ExpV13              getExpV13(){
        return new              DL4JExpV13();
    }
    public LogV13              getLogV13(){
        return new              DL4JLogV13();
    }
    public ReluV13             getReluV13(){
        return new             DL4JReluV13();
    }
    public SoftplusV13         getSoftplusV13(){
        return new         DL4JSoftplusV13();
    }
    public QuantizeLinearV13   getQuantizeLinearV13(){
        return new   DL4JQuantizeLinearV13();
    }
    public DequantizeLinearV13 getDequantizeLinearV13(){
        return new DL4JDequantizeLinearV13();
    }

    public DL4JAiOnnxOperatorSetV13() {
        this(1, "", "", 13L, "ONNX OPSET-V13 USING DL4J BACKEND");
    }

    public DL4JAiOnnxOperatorSetV13(int irVersion, String irVersionPrerelease, String irBuildMetadata, long opsetVersion,
                                   String docString) {
        super(irVersion, irVersionPrerelease, irBuildMetadata, opsetVersion, docString);
    }
}

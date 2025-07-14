package org.forwarder.backend.impls.HWAccelerated.opsets.v13;

import org.forwarder.backend.impls.HWAccelerated.opsets.v12.HWAcceleratedOperatorSetV12;
import org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops.*;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOpsetInitializerV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.*;

public class HWAcceleratedOperatorSetV13 extends HWAcceleratedOperatorSetV12 implements AiOnnxOpsetInitializerV13 {
    @Override
    public ReshapeV13          getReshapeV13() {
        return new          HWAcceleratedReshapeV13();
    }
    public ConcatV13           getConcatV13(){
        return new           HWAcceleratedConcatV13();
    }
    public SliceV13            getSliceV13(){
        return new            HWAcceleratedSliceV13();
    }
    public CastV13            getCastV13(){return new                HWAcceleratedCastV13();}
    public ExpandV13            getExpandV13(){return new            HWAcceleratedExpandV13();}
    public TransposeV13            getTransposeV13(){return new            HWAcceleratedTransposeV13();}
    public NegV13            getNegV13(){return new                  HWAcceleratedNegV13();}
    public TileV13            getTileV13(){return new                HWAcceleratedTileV13();}
    public ReduceMaxV13        getReduceMaxV13(){return new          HWAcceleratedReduceMaxV13();}
    public MaxV13              getMaxV13(){return new                HWAcceleratedMaxV13();}
    public GreaterV13          getGreaterV13(){return new            HWAcceleratedGreaterV13();}
    public MatMulV13           getMatMulV13(){return new             HWAcceleratedMatMulV13();}
    public GeMMV13             getGeMMV13(){
        return new             HWAcceleratedGeMMV13();
    }
    public AddV13              getAddV13(){
        return new              HWAcceleratedAddV13();
    }
    public SubV13              getSubV13(){
        return new              HWAcceleratedSubV13();
    }
    public ExpV13              getExpV13(){
        return new              HWAcceleratedExpV13();
    }
    public LogV13              getLogV13(){
        return new              HWAcceleratedLogV13();
    }
    public ReluV13             getReluV13(){
        return new             HWAcceleratedReluV13();
    }
    public SoftplusV13         getSoftplusV13(){
        return new         HWAcceleratedSoftplusV13();
    }
    public QuantizeLinearV13   getQuantizeLinearV13(){
        return new   HWAcceleratedQuantizeLinearV13();
    }
    public DequantizeLinearV13 getDequantizeLinearV13(){
        return new HWAcceleratedDequantizeLinearV13();
    }

    public HWAcceleratedOperatorSetV13() {
        this(1, "", "", 13L, "ONNX OPSET-V13 USING DL4J BACKEND");
    }

    public HWAcceleratedOperatorSetV13(int irVersion, String irVersionPrerelease, String irBuildMetadata, long opsetVersion,
                                    String docString) {
        super(irVersion, irVersionPrerelease, irBuildMetadata, opsetVersion, docString);
    }
}

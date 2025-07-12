package org.forwarder.backend.impls.HWAccelerated.opsets.v1;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedBackend;
import org.forwarder.backend.impls.HWAccelerated.opsets.v1.ops.*;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperatorSet;
import org.forwarder.opset.annotations.Opset;
import org.onnx4j.opsets.domain.aiOnnx.v1.AiOnnxOpsetInitializerV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.AbsV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.AddV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.ArgMaxV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.AveragePoolV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.BatchNormalizationV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.CastV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.ConcatV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.ConstantV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.ConvV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.DivV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.DropoutV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.GatherV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.IdentityV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.ImageScalerV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.LeakyReluV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.MatMulV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.MaxPoolV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.MulV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.ReduceMaxV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.ReluV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.ReshapeV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.ShapeV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.SigmoidV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.SoftmaxV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.SqueezeV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.SubV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.SumV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.TransposeV1;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.UnsqueezeV1;

@Opset(backendName = HWAcceleratedBackend.BACKEND_NAME)
public class HWAcceleratedOperatorSetV1 extends HWAcceleratedOperatorSet implements AiOnnxOpsetInitializerV1 {

    @Override
    public AbsV1 getAbsV1() { return new HWAcceleratedAbsV1(); }

    @Override
    public MatMulV1 getMatMulV1() { return new HWAcceleratedMatMulV1(); }

    @Override
    public IdentityV1 getIdentityV1() { return new HWAcceleratedIdentityV1(); }

    @Override
    public ArgMaxV1 getArgMaxV1() { return new HWAcceleratedArgMaxV1(); }

    @Override
    public DivV1 getDivV1() { return new HWAcceleratedDivV1(); }

    @Override
    public ReshapeV1 getReshapeV1() { return new HWAcceleratedReshapeV1(); }

    @Override
    public AddV1 getAddV1() { return new HWAcceleratedAddV1(); }

    @Override
    public MaxPoolV1 getMaxPoolV1() { return new HWAcceleratedMaxPoolV1(); }

    @Override
    public ReluV1 getReluV1() { return new HWAcceleratedReluV1(); }

    @Override
    public ConvV1 getConvV1() { return new HWAcceleratedConvV1(); }

    @Override
    public ConstantV1 getConstantV1() { return new HWAcceleratedConstantV1(); }

    @Override
    public ImageScalerV1 getImageScalerV1() { return new HWAcceleratedImageScalerV1(); }

    @Override
    public BatchNormalizationV1 getBatchNormalizationV1() { return new HWAcceleratedBatchNormalizationV1(); }

    @Override
    public LeakyReluV1 getLeakyReluV1() { return new HWAcceleratedLeakyReluV1(); }

    @Override
    public ConcatV1 getConcatV1() { return new HWAcceleratedConcatV1(); }

    @Override
    public MulV1 getMulV1() { return new HWAcceleratedMulV1(); }

    @Override
    public DropoutV1 getDropoutV1() { return new HWAcceleratedDropoutV1(); }

    @Override
    public AveragePoolV1 getAveragePoolV1() { return new HWAcceleratedAveragePoolV1(); }

    @Override
    public CastV1 getCastV1() { return new HWAcceleratedCastV1(); }

    // TODO:
    @Override
    public GatherV1 getGatherV1() { return null; }

    @Override
    public SubV1 getSubV1() { return new HWAcceleratedSubV1(); }

    @Override
    public SumV1 getSumV1() { return new HWAcceleratedSumV1(); }

    @Override
    public SigmoidV1 getSigmoidV1() { return new HWAcceleratedSigmoidV1(); }

    @Override
    public SoftmaxV1 getSoftmaxV1() { return new HWAcceleratedSoftmaxV1(); }

    @Override
    public SqueezeV1 getSqueezeV1() { return new HWAcceleratedSqueezeV1(); }

    @Override
    public UnsqueezeV1 getUnsqueezeV1() { return new HWAcceleratedUnsqueezeV1(); }

    @Override
    public ReduceMaxV1 getReduceMaxV1() { return new HWAcceleratedReduceMaxV1(); }

    @Override
    public TransposeV1 getTransposeV1() { return new HWAcceleratedTransposeV1(); }

    @Override
    public ShapeV1 getShapeV1() { return new HWAcceleratedShapeV1(); }

    public HWAcceleratedOperatorSetV1() {
        this(1, "", "", 1L, "ONNX OPSET-V1 USING HWAccelerated BACKEND");
    }

    public HWAcceleratedOperatorSetV1(int irVersion, String irVersionPrerelease, String irBuildMetadata,
                                   long opsetVersion, String docString) {
        super(irVersion, irVersionPrerelease, irBuildMetadata, opsetVersion, docString);
    }

}
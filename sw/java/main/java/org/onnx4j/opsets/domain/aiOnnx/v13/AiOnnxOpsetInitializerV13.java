package org.onnx4j.opsets.domain.aiOnnx.v13;

import org.onnx4j.opsets.Operator;
import org.onnx4j.opsets.domain.aiOnnx.v12.AiOnnxOpsetInitializerV12;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ConcatV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ReshapeV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.SliceV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.AddV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.SubV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ExpV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.LogV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ReluV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.SoftplusV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.QuantizeLinearV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.DequantizeLinearV13;

import java.util.Map;

public interface AiOnnxOpsetInitializerV13 extends AiOnnxOpsetInitializerV12 {
    public abstract ReshapeV13 getReshapeV13();

    public abstract ConcatV13 getConcatV13();

    public abstract SliceV13 getSliceV13();

    public abstract AddV13 getAddV13();

    public abstract SubV13 getSubV13();

    public abstract ExpV13 getExpV13();

    public abstract LogV13 getLogV13();

    public abstract ReluV13 getReluV13();

    public abstract SoftplusV13 getSoftplusV13();

    public abstract QuantizeLinearV13 getQuantizeLinearV13();

    public abstract DequantizeLinearV13 getDequantizeLinearV13();

    public default Map<String, Operator> initializeOperators() {
        Map<String, Operator> operators = AiOnnxOpsetInitializerV12.super.initializeOperators();

        operators.put(ReshapeV13.OP_TYPE, this.getReshapeV13());

        operators.put(ConcatV13.OP_TYPE, this.getConcatV13());

        operators.put(SliceV13.OP_TYPE, this.getSliceV13());

        operators.put(AddV13.OP_TYPE, this.getAddV13());

        operators.put(SubV13.OP_TYPE, this.getSubV13());

        operators.put(ExpV13.OP_TYPE, this.getExpV13());

        operators.put(LogV13.OP_TYPE, this.getLogV13());

        operators.put(ReluV13.OP_TYPE, this.getReluV13());

        operators.put(SoftplusV13.OP_TYPE, this.getSoftplusV13());

        operators.put(QuantizeLinearV13.OP_TYPE, this.getQuantizeLinearV13());

        operators.put(DequantizeLinearV13.OP_TYPE, this.getDequantizeLinearV13());

        return operators;
    }
}

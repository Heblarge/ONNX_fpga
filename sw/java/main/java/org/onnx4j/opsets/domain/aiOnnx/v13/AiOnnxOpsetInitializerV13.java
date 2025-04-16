package org.onnx4j.opsets.domain.aiOnnx.v13;

import org.onnx4j.opsets.Operator;
import org.onnx4j.opsets.domain.aiOnnx.v12.AiOnnxOpsetInitializerV12;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ConcatV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ReshapeV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.SliceV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.SoftplusV13;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.QuantizeLinearV13;

import java.util.Map;

public interface AiOnnxOpsetInitializerV13 extends AiOnnxOpsetInitializerV12 {
    public abstract ReshapeV13 getReshapeV13();

    public abstract ConcatV13 getConcatV13();

    public abstract SliceV13 getSliceV13();

    public abstract SoftplusV13 getSoftplusV13();

    public abstract QuantizeLinearV13 getQuantizeLinearV13();

    public default Map<String, Operator> initializeOperators() {
        Map<String, Operator> operators = AiOnnxOpsetInitializerV12.super.initializeOperators();

        operators.put(ReshapeV13.OP_TYPE, this.getReshapeV13());

        operators.put(ConcatV13.OP_TYPE, this.getConcatV13());

        operators.put(SliceV13.OP_TYPE, this.getSliceV13());

        operators.put(SoftplusV13.OP_TYPE, this.getSoftplusV13());

        operators.put(QuantizeLinearV13.OP_TYPE, this.getQuantizeLinearV13());

        return operators;
    }
}

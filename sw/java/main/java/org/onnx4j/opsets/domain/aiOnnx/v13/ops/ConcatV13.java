package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v4.ops.ConcatV4;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.tensor.DataType;

public interface ConcatV13 extends ConcatV4, AiOnnxOperatorV13 {
    public static final Field.TypeConstraint TPYE_CONSTRAINT_T = new Field.TypeConstraint(DataType.allTypes());
    class ConcatInputsV13<T_TENSOR> extends ConcatInputsV4<T_TENSOR> {

        public ConcatInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
        }

        @Override
        public Field.TypeConstraint getInputFieldsTypeConstraint() {
            return TPYE_CONSTRAINT_T;
        }

    }
    class ConcatOutputV13<T_TENSOR> extends ConcatOutputV4<T_TENSOR> {

        public ConcatOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public Field.TypeConstraint getTypeConstraint() {
            return TPYE_CONSTRAINT_T;
        }

    }
}

package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v6.ops.CastV6;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.tensor.DataType;

public interface CastV13 extends CastV6, AiOnnxOperatorV13 {

    // V13: 支持 "allTypes" 作为输入输出类型约束
    public static final Field.TypeConstraint TYPE_CONSTRAINT_T = new Field.TypeConstraint(DataType.allTypes());

    class CastInputsV13<T_TENSOR> extends CastInputV6<T_TENSOR>  {


        public static final String ATTR_TO = "to";

        public CastInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
        }


        public Field.TypeConstraint getInputFieldsTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }

        /**
         * 返回目标数据类型（DataType枚举）
         */

    }

    class CastOutputV13<T_TENSOR> extends CastOutputV6<T_TENSOR> {
        public CastOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public Field.TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }
    }
}
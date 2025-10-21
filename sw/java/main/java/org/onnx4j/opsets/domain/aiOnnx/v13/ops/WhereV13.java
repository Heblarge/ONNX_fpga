package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v9.ops.WhereV9;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.tensor.DataType;

/**
 * Where Operator v13
 *
 * 与 v9 相比，v13 版本在结构上与其他 v13 算子一致，
 * 不涉及硬件加速或量化属性，仅支持纯软件逻辑。
 */
public interface WhereV13 extends WhereV9, AiOnnxOperatorV13 {

    /** 支持所有张量类型（与 WhereV9 一致） */
    public static final TypeConstraint TYPE_CONSTRAINT_T = new TypeConstraint(DataType.allTypes());

    class WhereInputsV13<T_TENSOR> extends WhereV9.WhereInputsV9<T_TENSOR> {

        private final InputField<T_TENSOR> conditionField;
        private final InputField<T_TENSOR> xField;
        private final InputField<T_TENSOR> yField;

        public WhereInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.conditionField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[0]);
            this.xField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[1]);
            this.yField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[2]);
        }

        public T_TENSOR getCondition() { return conditionField.getData(); }
        public T_TENSOR getX() { return xField.getData(); }
        public T_TENSOR getY() { return yField.getData(); }
    }

    class WhereOutputV13<T_TENSOR> extends WhereV9.WhereOutputV9<T_TENSOR> {
        public WhereOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }
    }
}

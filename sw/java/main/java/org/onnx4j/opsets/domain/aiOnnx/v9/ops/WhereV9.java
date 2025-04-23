package org.onnx4j.opsets.domain.aiOnnx.v9.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v9.AiOnnxOperatorV9;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.tensor.DataType;

/**
 * Where Operator v9
 * Implements ONNX where(condition, X, Y)
 */
public interface WhereV9 extends AiOnnxOperatorV9 {

    String OP_TYPE = "Where";

    TypeConstraint TYPE_CONSTRAINT_B = new TypeConstraint(DataType.BOOL);
    TypeConstraint TYPE_CONSTRAINT_T = new TypeConstraint(
            DataType.BOOL, DataType.FLOAT, DataType.DOUBLE, DataType.FLOAT16, DataType.INT8,
            DataType.INT16, DataType.INT32, DataType.INT64, DataType.UINT8, DataType.UINT16,
            DataType.UINT32, DataType.UINT64, DataType.STRING, DataType.COMPLEX64, DataType.COMPLEX128
    );

    @Override
    default String getOpType() {
        return OP_TYPE;
    }

    @Override
    default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    class WhereInputsV9<T_TENSOR> extends OperatorInputs<T_TENSOR> {

        private final InputField<T_TENSOR> conditionField;
        private final InputField<T_TENSOR> xField;
        private final InputField<T_TENSOR> yField;

        public WhereInputsV9(Node node, Inputs inputs) {
            super(node, inputs);
            this.conditionField = new InputField<>(this, TYPE_CONSTRAINT_B, inputArray[0]);
            this.xField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[1]);
            this.yField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[2]);
        }

        public T_TENSOR getCondition() { return conditionField.getData(); }

        public T_TENSOR getX() { return xField.getData(); }

        public T_TENSOR getY() { return yField.getData(); }
    }

    class WhereOutputV9<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {

        public WhereOutputV9(T_TENSOR output) {
            super(output);
        }

        @Override
        public TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }
    }

    class TypeConstraint extends org.onnx4j.opsets.operator.Field.TypeConstraint {
        public TypeConstraint(DataType... types) {
            super(types);
        }
    }
}

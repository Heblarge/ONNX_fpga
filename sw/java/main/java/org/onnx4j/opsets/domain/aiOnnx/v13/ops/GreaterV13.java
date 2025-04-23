package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.tensor.DataType;

/**
 * Greater Operator v13
 *
 * Performs element-wise 'greater than' comparison of input tensors A > B,
 * returns a boolean tensor. Supports Numpy-style broadcasting.
 */
public interface GreaterV13 extends AiOnnxOperatorV13 {

    String OP_TYPE = "Greater";

    // 输入类型约束：支持所有数值张量
    TypeConstraint TYPE_CONSTRAINT_T = new TypeConstraint(
            DataType.FLOAT, DataType.DOUBLE, DataType.FLOAT16, DataType.BFLOAT16,
            DataType.INT8, DataType.INT16, DataType.INT32, DataType.INT64,
            DataType.UINT8, DataType.UINT16, DataType.UINT32, DataType.UINT64
    );

    // 输出类型约束：布尔类型
    TypeConstraint TYPE_CONSTRAINT_BOOL = new TypeConstraint(DataType.BOOL);

    @Override
    default String getOpType() {
        return OP_TYPE;
    }

    @Override
    default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    /**
     * Inputs for Greater operator (A, B)
     */
    class GreaterInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {

        private final InputField<T_TENSOR> aField;
        private final InputField<T_TENSOR> bField;

        public GreaterInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.aField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[0]);
            this.bField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[1]);
        }

        public T_TENSOR getA() {
            return aField.getData();
        }

        public T_TENSOR getB() {
            return bField.getData();
        }
    }

    /**
     * Output for Greater operator
     */
    class GreaterOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {

        public GreaterOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_BOOL;
        }
    }

    /**
     * Wrapper for type constraint
     */
    class TypeConstraint extends org.onnx4j.opsets.operator.Field.TypeConstraint {
        public TypeConstraint(DataType... types) {
            super(types);
        }
    }
}

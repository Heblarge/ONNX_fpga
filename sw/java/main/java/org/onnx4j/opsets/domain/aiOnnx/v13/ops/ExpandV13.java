package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v8.ops.ExpandV8;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.fields.OutputField;
import org.onnx4j.tensor.DataType;

/**
 * Expand Operator v13
 *
 * 与 v8 相比，v13 版本新增支持 float16 数据类型
 */
public interface ExpandV13 extends ExpandV8, AiOnnxOperatorV13 {

    /** 类型约束：支持所有数值张量类型（包括新增的float16） */
    public static final TypeConstraint TYPE_CONSTRAINT_T = new TypeConstraint(
            DataType.BFLOAT16,
            DataType.BOOL,
            DataType.COMPLEX128,
            DataType.COMPLEX64,
            DataType.DOUBLE,
            DataType.FLOAT,
            DataType.FLOAT16,
            DataType.INT16,
            DataType.INT32,
            DataType.INT64,
            DataType.INT8,
            DataType.STRING,
            DataType.UINT16,
            DataType.UINT32,
            DataType.UINT64,
            DataType.UINT8
    );

    /** shape输入的类型约束（应为INT64） */
    public static final TypeConstraint TYPE_CONSTRAINT_INT64 = new TypeConstraint(DataType.INT64);

    class ExpandInputsV13<T_TENSOR> extends ExpandInputsV8<T_TENSOR> {

        private final InputField<T_TENSOR> inputField;
        private final InputField<T_TENSOR> shapeField;

        public ExpandInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.inputField = new InputField<>(this, TYPE_CONSTRAINT_T, super.inputArray[0]);
            this.shapeField = new InputField<>(this, TYPE_CONSTRAINT_INT64, super.inputArray[1]);
        }

        @Override
        public T_TENSOR getInput() {
            return this.inputField.getData();
        }

        @Override
        public T_TENSOR getShape() {
            return this.shapeField.getData();
        }
    }

}
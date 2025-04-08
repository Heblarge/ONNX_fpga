package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import java.util.List;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.ReshapeV1;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.domain.aiOnnx.v5.ops.ReshapeV5;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.tensor.DataType;

/**
 * Reshape Operator v13
 *
 * 与 v5 相比，v13 版本将 shape 由 attribute 改为 input，同时放宽输入类型约束。
 */
public interface ReshapeV13 extends ReshapeV1, AiOnnxOperatorV13 {

    /** 类型约束：支持所有张量类型（包括浮点、整数等） */
    public static final TypeConstraint TYPE_CONSTRAINT_T = new TypeConstraint(DataType.allTypes());

    public static final TypeConstraint TPYE_CONSTRAINT_INT64 = new TypeConstraint(DataType.INT64);

    class ReshapeInputsV13<T_TENSOR> extends ReshapeInputsV1<T_TENSOR> {

        private final Field<T_TENSOR> shapeTensorField;

        public ReshapeInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.shapeTensorField = new InputField<T_TENSOR>(this, TPYE_CONSTRAINT_INT64, super.inputArray[1]);
        }

        public T_TENSOR getShapeTensor() {
            return shapeTensorField.getData();
        }


        @Override
        public List<Long> getShape() {
            throw new UnsupportedOperationException(String.format("Attribute named \"%s\" has deprecated", ATTR_SHAPE));
        }

        @Override
        public List<Long> getConsumedInputs() {
            throw new UnsupportedOperationException(
                    String.format("Attribute named \"%s\" has deprecated", ATTR_CONSUMED_INPUTS));
        }
    }

    class ReshapeOutputV13<T_TENSOR> extends ReshapeOutputV1<T_TENSOR> {

        public ReshapeOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }
    }
}

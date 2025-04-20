package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;

import org.onnx4j.model.graph.node.attributes.IntAttribute;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import org.onnx4j.opsets.operator.fields.AttributeField;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.tensor.DataType;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.InputField;

public interface DequantizeLinearV13 extends AiOnnxOperatorV13 {
    public static final String OP_TYPE = "DequantizeLinear";
    public static final TypeConstraint TYPE_CONSTRAINT_T = new Field.TypeConstraint(DataType.INT32, DataType.INT8, DataType.UINT8);

    @Override
    default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    @Override
    default String getOpType() {
        return OP_TYPE;
    }

    class DequantizeLinearInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {
        public static final String ATTR_AXIS = "axis";
        protected InputField<T_TENSOR> xField;
        protected InputField<T_TENSOR> xScaleField;
        protected InputField<T_TENSOR> xZeroPointField;
        protected Field<Long> axisField;

        public DequantizeLinearInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.xField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[0]);
            this.xScaleField = new InputField<>(this, new TypeConstraint(DataType.FLOAT), inputArray[1]);
            this.xZeroPointField = inputArray.length > 2
                    ? new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[2])
                    : null;
            axisField = new AttributeField<Long>(super.attrs, ATTR_AXIS, IntAttribute.class, 1L, true);
        }

        public T_TENSOR getX() {
            return xField.getData();
        }

        public T_TENSOR getXScale() {
            return xScaleField.getData();
        }

        public boolean hasXZeroPoint() {
            return xZeroPointField != null;
        }

        public T_TENSOR getXZeroPoint() {
            if (!hasXZeroPoint()) {
                throw new IllegalStateException("QuantizeLinear operation requires x_zero_point input");
            }
            return xZeroPointField.getData();
        }

        public Long getAxis() {
            return axisField.getData();
        }
    }


    class DequantizeLinearOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {
        public DequantizeLinearOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public TypeConstraint getTypeConstraint() {
            return new TypeConstraint(DataType.FLOAT);
        }
    }
}

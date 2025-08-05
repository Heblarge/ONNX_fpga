package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

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

public interface QuantizeLinearV13 extends AiOnnxOperatorV13 {

    public static final String OP_TYPE = "QuantizeLinear";
    public static final TypeConstraint TYPE_CONSTRAINT_T1 = new Field.TypeConstraint(DataType.FLOAT, DataType.INT32);
    public static final TypeConstraint TYPE_CONSTRAINT_T2 = new Field.TypeConstraint(DataType.INT8, DataType.UINT8, DataType.INT32);

    @Override
    default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    @Override
    default String getOpType() {
        return OP_TYPE;
    }

    class QuantizeLinearInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {
        public static final String ATTR_AXIS = "axis";
        protected InputField<T_TENSOR> xField;
        protected InputField<T_TENSOR> yScaleField;
        protected InputField<T_TENSOR> yZeroPointField;
        protected Field<Long> axisField;
        //protected final int axis;

        public QuantizeLinearInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
//            this.axis = node.getAttribute("axis") != null
//                    ? node.getAttribute("axis").getIntValue(1)
//                    : 1;
            this.xField = new InputField<>(this, TYPE_CONSTRAINT_T1, inputArray[0]);
            this.yScaleField = new InputField<>(this, new TypeConstraint(DataType.FLOAT), inputArray[1]);
            this.yZeroPointField = inputArray.length > 2
                    ? new InputField<>(this, new TypeConstraint(DataType.INT8, DataType.UINT8), inputArray[2])
                    : null;
            axisField = new AttributeField<Long>(super.attrs, ATTR_AXIS, IntAttribute.class, 1L, true);
        }


        public T_TENSOR getX() {
            return xField.getData();
        }

        public T_TENSOR getYScale() {
            return yScaleField.getData();
        }

        public boolean hasYZeroPoint() {
            return yZeroPointField != null;
        }

        public T_TENSOR getYZeroPoint() {
            if (!hasYZeroPoint()) {
                throw new IllegalStateException("QuantizeLinear operation requires y_zero_point input");
            }
            return yZeroPointField.getData();
        }

        public Long getAxis() {
            return axisField.getData();
        }
    }

    class QuantizeLinearOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {
        public QuantizeLinearOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T2;
        }
    }
}

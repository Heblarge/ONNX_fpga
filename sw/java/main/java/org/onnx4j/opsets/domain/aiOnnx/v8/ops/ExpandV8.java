package org.onnx4j.opsets.domain.aiOnnx.v8.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v8.AiOnnxOperatorV8;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.OutputField;
import org.onnx4j.opsets.operator.output.MultiOperatorOutputs;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import org.onnx4j.tensor.DataType;

/**
 * Expand Operator v8
 *
 * Broadcast the input tensor following the given shape.
 *
 * @see https://github.com/onnx/onnx/blob/main/docs/Operators.md#Expand
 */
public interface ExpandV8 extends AiOnnxOperatorV8 {

    public static final String OP_TYPE = "Expand";

    /**
     * Constrain input and output types to all numeric tensors.
     */
    public static final TypeConstraint TYPE_CONSTRAINT_T = new TypeConstraint(DataType.numericTypes());

    @Override
    public default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    @Override
    public default String getOpType() {
        return OP_TYPE;
    }

    class ExpandInputsV8<T_TENSOR> extends OperatorInputs<T_TENSOR> {

        protected Field<T_TENSOR> inputField;
        protected Field<T_TENSOR> shapeField;

        public ExpandInputsV8(Node node, Inputs inputs) {
            super(node, inputs);
            this.inputField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[0]);
            this.shapeField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[1]);
        }

        public T_TENSOR getInput() {
            return inputField.getData();
        }

        public T_TENSOR getShape() {
            return shapeField.getData();
        }
    }

    class ExpandOutputV8<T_TENSOR> extends MultiOperatorOutputs<T_TENSOR> {

//        public ExpandOutputV8(T_TENSOR output) {
//            super(output);
//        }
        public ExpandOutputV8(T_TENSOR output) {
            this.addOutputField(new OutputField<>(this, TYPE_CONSTRAINT_T, output, false));
        }


//        @Override
//        public TypeConstraint getTypeConstraint() {
//            return TYPE_CONSTRAINT_T;
//        }
    }

}

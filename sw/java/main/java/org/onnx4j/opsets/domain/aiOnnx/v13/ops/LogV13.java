package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.tensor.DataType;

public interface LogV13 extends AiOnnxOperatorV13 {

    public static final String OP_TYPE = "Log";
    public static final Field.TypeConstraint TYPE_CONSTRAINT_T = new Field.TypeConstraint(
            DataType.BFLOAT16,
            DataType.DOUBLE,
            DataType.FLOAT,
            DataType.FLOAT16
    );

    @Override
    default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    @Override
    default String getOpType() {
        return OP_TYPE;
    }

    class LogInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {

        protected InputField<T_TENSOR> inputField;

        public LogInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.inputField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[0]);
        }

        public T_TENSOR getInput() {
            return inputField.getData();
        }
    }

    class LogOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {

        public LogOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public Field.TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }
    }
}
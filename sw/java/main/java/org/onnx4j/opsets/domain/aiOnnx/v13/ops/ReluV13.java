package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.tensor.DataType;

public interface ReluV13 extends AiOnnxOperatorV13 {

    public static final String OP_TYPE = "Relu";
    public static final Field.TypeConstraint TYPE_CONSTRAINT_T = new Field.TypeConstraint(
            DataType.BFLOAT16,
            DataType.FLOAT16,
            DataType.FLOAT,
            DataType.DOUBLE
    );

    @Override
    default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    @Override
    default String getOpType() {
        return OP_TYPE;
    }

    class ReluInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {

        protected InputField<T_TENSOR> xField;

        public ReluInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.xField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[0]);
        }
        
        public T_TENSOR getX() {
            return xField.getData();
        }
    }

    class ReluOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {

        public ReluOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public Field.TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }
    }
}

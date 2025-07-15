package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.tensor.DataType;

public interface SoftplusV13 extends AiOnnxOperatorV13 {

    public static final String OP_TYPE = "Softplus";
    public static final TypeConstraint TPYE_CONSTRAINT_T = new Field.TypeConstraint(DataType.numericTypes());

    @Override
    default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    @Override
    default String getOpType() {
        return OP_TYPE;
    }

    class SoftplusInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {
        protected InputField<T_TENSOR> xField;

        public SoftplusInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.xField = new InputField<>(this, TPYE_CONSTRAINT_T, inputArray[0]);
        }

        public T_TENSOR getInput() {
            return xField.getData();
        }
    }

    class SoftplusOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {
        public SoftplusOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public TypeConstraint getTypeConstraint() {
            return TPYE_CONSTRAINT_T;
        }
    }
}
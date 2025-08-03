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

public interface NegV13 extends AiOnnxOperatorV13 {

    public static final String OP_TYPE = "Neg";
    public static final TypeConstraint TPYE_CONSTRAINT_T = new Field.TypeConstraint(DataType.FLOAT,
            DataType.INT32,
            DataType.INT8,
            DataType.INT16,
            DataType.INT64,
            DataType.FLOAT16,
            DataType.DOUBLE,
            DataType.BFLOAT16);

    @Override
    default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    @Override
    default String getOpType() {
        return OP_TYPE;
    }

    class NegInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {
        protected InputField<T_TENSOR> xField;

        public NegInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.xField = new InputField<>(this, TPYE_CONSTRAINT_T, inputArray[0]);
        }

        public T_TENSOR getX() {
            return xField.getData();
        }
    }

    class NegOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {
        public NegOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public TypeConstraint getTypeConstraint() {
            return TPYE_CONSTRAINT_T;
        }
    }
}
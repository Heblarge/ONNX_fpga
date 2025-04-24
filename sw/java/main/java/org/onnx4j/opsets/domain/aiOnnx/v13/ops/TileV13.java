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

public interface TileV13 extends AiOnnxOperatorV13 {

    public static final String OP_TYPE = "Tile";
    public static final TypeConstraint TPYE_CONSTRAINT_T = new Field.TypeConstraint(DataType.numericTypes());
    public static final TypeConstraint TPYE_CONSTRAINT_T1 = new Field.TypeConstraint(DataType.INT64);

    @Override
    default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    @Override
    default String getOpType() {
        return OP_TYPE;
    }

    class TileInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {
        protected InputField<T_TENSOR> inputField;
        protected InputField<T_TENSOR> repeatsField;

        public TileInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.inputField = new InputField<>(this, TPYE_CONSTRAINT_T, inputArray[0]);
            this.repeatsField = new InputField<>(this, TPYE_CONSTRAINT_T1, inputArray[1]);
        }

        public T_TENSOR getInput() {
            return inputField.getData();
        }

        public T_TENSOR getRepeats() {
            return repeatsField.getData();
        }
    }

    class TileOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {
        public TileOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public TypeConstraint getTypeConstraint() {
            return TPYE_CONSTRAINT_T;
        }
    }
}
package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import java.util.Arrays;
import java.util.ArrayList; // 引入 ArrayList
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.tensor.DataType;


public interface MaxV13 extends AiOnnxOperatorV13 {

    public static final String OP_TYPE = "Max";

    public static final TypeConstraint TYPE_CONSTRAINT_T = new TypeConstraint(
            DataType.FLOAT, DataType.DOUBLE, DataType.FLOAT16, DataType.BFLOAT16,
            DataType.INT8, DataType.INT16, DataType.INT32, DataType.INT64,
            DataType.UINT8, DataType.UINT16, DataType.UINT32, DataType.UINT64
    );

    @Override
    public default String getOpType() {
        return OP_TYPE;
    }

    @Override
    public default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    /**
     * Inputs for Max operator (variadic input tensors)
     */
    class MaxInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {

        private final List<InputField<T_TENSOR>> inputField; // 应为 List<InputField> 类型

        public MaxInputsV13(Node node, Inputs inputs) {
            super(node, inputs);

            this.inputField = new ArrayList<>();
            for (org.onnx4j.Inputs.Input rawInput : super.inputArray) {
                this.inputField.add(new InputField<>(this, TYPE_CONSTRAINT_T, rawInput));
            }
        }


        public List<T_TENSOR> getInputTensors() {
            return this.inputFields.stream()
                    .map(InputField::getData)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Output for Max operator
     */
    class MaxOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {

        public MaxOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }
    }

    /**
     * Type constraint interface
     */
    class TypeConstraint extends org.onnx4j.opsets.operator.Field.TypeConstraint {
        public TypeConstraint(DataType... allowedTypes) {
            super(allowedTypes);
        }
    }
}





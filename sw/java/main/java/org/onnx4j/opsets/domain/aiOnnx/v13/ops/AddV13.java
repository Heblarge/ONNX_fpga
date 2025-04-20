package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v7.ops.AddV7;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import org.onnx4j.tensor.DataType;

public interface AddV13 extends AddV7, AiOnnxOperatorV13 {

    public static final TypeConstraint TYPE_CONSTRAINT_T = new TypeConstraint(DataType.highPrecisionNumeric());

    class AddInputsV13<T_TENSOR> extends AddV7.AddInputsV7<T_TENSOR> {

        public AddInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
        }

    }

    class AddOutputV13<T_TENSOR> extends AddV7.AddOutputV7<T_TENSOR> {

        public AddOutputV13(T_TENSOR output) {
            super(output);
        }

    }


}
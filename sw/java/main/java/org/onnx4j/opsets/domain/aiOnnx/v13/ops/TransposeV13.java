
package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.TransposeV1;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.tensor.DataType;


public interface TransposeV13 extends TransposeV1, AiOnnxOperatorV13 {

    public static final Field.TypeConstraint TPYE_CONSTRAINT_T = new Field.TypeConstraint(
            DataType.BFLOAT16,    // tensor(bfloat16)
            DataType.BOOL,        // tensor(bool)
            DataType.COMPLEX128,  // tensor(complex128)
            DataType.COMPLEX64,   // tensor(complex64)
            DataType.DOUBLE,      // tensor(double)
            DataType.FLOAT,      // tensor(float)
            DataType.FLOAT16,     // tensor(float16)
            DataType.INT16,      // tensor(int16)
            DataType.INT32,       // tensor(int32)
            DataType.INT64,      // tensor(int64)
            DataType.INT8,       // tensor(int8)
            DataType.STRING,     // tensor(string)
            DataType.UINT16,     // tensor(uint16)
            DataType.UINT32,     // tensor(uint32)
            DataType.UINT64,     // tensor(uint64)
            DataType.UINT8       // tensor(uint8)
    );

    // Inputs with version-specific type constraint
    class TransposeInputsV13<T_TENSOR> extends TransposeInputsV1<T_TENSOR> {
        public TransposeInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
        }

        public Field.TypeConstraint getInputFieldsTypeConstraint() {
            return TPYE_CONSTRAINT_T;
        }
    }

    // Output with version-specific type constraint
    class TransposeOutputV13<T_TENSOR> extends TransposeOutputV1<T_TENSOR> {
        public TransposeOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public Field.TypeConstraint getTypeConstraint() {
            return TPYE_CONSTRAINT_T;
        }
    }
}
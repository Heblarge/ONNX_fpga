package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.model.graph.Node;
import org.onnx4j.Inputs;
import org.onnx4j.tensor.DataType;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.MatMulV1;
import org.onnx4j.opsets.operator.Field;
/**
 * MatMul Operator v13 - extends v1
 * Same logic, with expanded data type support
 */
public interface MatMulV13 extends MatMulV1, AiOnnxOperatorV13 {

    @Override
    default String getOpType() {
        return "MatMul";
    }

    @Override
    default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    /**
     * Override type constraint: MatMul-13 supports more types than V1
     */

    class TypeConstraint extends Field.TypeConstraint {
        public TypeConstraint(DataType... types) {
            super(types);
        }
    }
    TypeConstraint TYPE_CONSTRAINT_T = new TypeConstraint(
            DataType.FLOAT, DataType.FLOAT16, DataType.DOUBLE, DataType.BFLOAT16,
            DataType.INT32, DataType.INT64, DataType.UINT32, DataType.UINT64
    );
}


package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;

import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.tensor.DataType;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.InputField;

public interface SliceV13 extends AiOnnxOperatorV13{
    public static final String OP_TYPE = "Slice";
    public static final TypeConstraint TPYE_CONSTRAINT_T = new Field.TypeConstraint(DataType.numericTypes());

    @Override
    public default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    @Override
    public default String getOpType() {
        return OP_TYPE;
    }

    class SliceInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {
        protected Field<T_TENSOR> dataField;
        protected Field<T_TENSOR> startsField;
        protected Field<T_TENSOR> endsField;
        protected Field<T_TENSOR> axesField;       //optional
        protected Field<T_TENSOR> stepsField;      //optional

        public SliceInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.dataField = new InputField<>(this, TPYE_CONSTRAINT_T, inputArray[0]);
            this.startsField = new InputField<>(this, TPYE_CONSTRAINT_T, inputArray[1]);
            this.endsField = new InputField<>(this, TPYE_CONSTRAINT_T, inputArray[2]);

            this.axesField = inputArray.length > 3
                    ? new InputField<>(this, TPYE_CONSTRAINT_T, inputArray[3])
                    : null;
            this.stepsField = inputArray.length > 4
                    ? new InputField<>(this, TPYE_CONSTRAINT_T, inputArray[4])
                    : null;
        }

        public T_TENSOR getData() {
            return dataField.getData();
        }

        public T_TENSOR getStartsTensor() {
            return startsField.getData();
        }

        public T_TENSOR getEndsTensor() {
            return endsField.getData();
        }

        public boolean hasAxes() {
            return axesField != null;
        }

        public T_TENSOR getAxesTensor() {
            if (axesField == null) {
                throw new IllegalStateException("Slice 操作未提供 axes 输入");
            }
            return axesField.getData();
        }

        public boolean hasSteps() {
            return stepsField != null;
        }

        public T_TENSOR getStepsTensor() {
            if (stepsField == null) {
                throw new IllegalStateException("Slice 操作未提供 steps 输入");
            }
            return stepsField.getData();
        }

        // 可选提供：将张量转为 List<Long>（假设使用 ND4J 或类似 API）
        public List<Long> toLongList(T_TENSOR tensor) {
            // 示例实现（若使用 ND4J）：
            long[] raw = ((org.nd4j.linalg.api.ndarray.INDArray) tensor).toLongVector();
            return Arrays.stream(raw).boxed().collect(Collectors.toList());
        }
    }

    class SliceOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {
        public SliceOutputV13(T_TENSOR output) {
            super(output);
        }
        @Override
        public TypeConstraint getTypeConstraint() {
            return TPYE_CONSTRAINT_T;
        }
    }
}

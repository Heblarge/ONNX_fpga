package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.tensor.DataType;
import org.onnx4j.opsets.operator.OperatorInputs;

import java.util.List;

public interface AddLogV13 extends AiOnnxOperatorV13 {

    String OP_TYPE = "AddLog";

    Field.TypeConstraint TYPE_CONSTRAINT_T = new Field.TypeConstraint(
            DataType.BFLOAT16,
            DataType.DOUBLE,
            DataType.FLOAT,
            DataType.FLOAT16,
            DataType.INT32
    );

    @Override
    default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    @Override
    default String getOpType() {
        return OP_TYPE;
    }


    class AddLogInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {

        protected InputField<T_TENSOR> aField;
        protected InputField<T_TENSOR> bField;


        protected List<Float> fpgaInScales;
        protected List<Long> fpgaInShift;
        protected List<Float> fpgaOutScales;
        protected List<Long> fpgaOutShift;

        public AddLogInputsV13(Node node, Inputs inputs) {
            super(node, inputs);

            this.aField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[0]);
            this.bField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[1]);

            // 初始化默认值
            this.fpgaInScales = List.of(0.0f);
            this.fpgaInShift  = List.of(0L);
            this.fpgaOutScales = List.of(0.0f);
            this.fpgaOutShift = List.of(0L);
        }

        public T_TENSOR getA() { return aField.getData(); }
        public T_TENSOR getB() { return bField.getData(); }
        public List<Float> getFpgaInScales() { return fpgaInScales; }
        public List<Long> getFpgaInShift() { return fpgaInShift; }
        public List<Float> getFpgaOutScales() { return fpgaOutScales; }
        public List<Long> getFpgaOutShift() { return fpgaOutShift; }
    }

    class AddLogOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {

        public AddLogOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public Field.TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }
    }
}

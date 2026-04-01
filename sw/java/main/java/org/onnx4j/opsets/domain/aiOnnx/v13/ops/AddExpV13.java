package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.node.attributes.FloatsAttribute;
import org.onnx4j.model.graph.node.attributes.IntsAttribute;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.AttributeField;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.tensor.DataType;

import java.util.List;

public interface AddExpV13 extends AiOnnxOperatorV13 {

    String OP_TYPE = "AddExp";

    Field.TypeConstraint TYPE_CONSTRAINT_T =
            new Field.TypeConstraint(DataType.highPrecisionNumeric());

    @Override
    default String getOpType() {
        return OP_TYPE;
    }

    @Override
    default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }


    class AddExpInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {

        public static final String ATTR_FPGA_IN_SCALES = "fpga_in_scales";
        public static final String ATTR_FPGA_IN_SHIFT = "fpga_in_shift";
        public static final String ATTR_FPGA_OUT_SCALES = "fpga_out_scales";
        public static final String ATTR_FPGA_OUT_SHIFT = "fpga_out_shift";

        protected InputField<T_TENSOR> aField;
        protected InputField<T_TENSOR> bField;

        protected Field<List<Float>> fpgaInScalesField;
        protected Field<List<Long>> fpgaInShiftField;
        protected Field<List<Float>> fpgaOutScalesField;
        protected Field<List<Long>> fpgaOutShiftField;

        public AddExpInputsV13(Node node, Inputs inputs) {
            super(node, inputs);

            // 两个输入
            this.aField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[0]);
            this.bField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[1]);

            // FPGA 量化属性（保持和 Add / Exp 一致）
            this.fpgaInScalesField =
                    new AttributeField<>(attrs, ATTR_FPGA_IN_SCALES, FloatsAttribute.class, List.of(0.0f), true);

            this.fpgaInShiftField =
                    new AttributeField<>(attrs, ATTR_FPGA_IN_SHIFT, IntsAttribute.class, List.of(0L), true);

            this.fpgaOutScalesField =
                    new AttributeField<>(attrs, ATTR_FPGA_OUT_SCALES, FloatsAttribute.class, List.of(0.0f), true);

            this.fpgaOutShiftField =
                    new AttributeField<>(attrs, ATTR_FPGA_OUT_SHIFT, IntsAttribute.class, List.of(0L), true);
        }

        public T_TENSOR getA() {
            return aField.getData();
        }

        public T_TENSOR getB() {
            return bField.getData();
        }

        public List<Float> getFpgaInScales() {
            return fpgaInScalesField.getData();
        }

        public List<Long> getFpgaInShift() {
            return fpgaInShiftField.getData();
        }

        public List<Float> getFpgaOutScale() {
            return fpgaOutScalesField.getData();
        }

        public List<Long> getFpgaOutShift() {
            return fpgaOutShiftField.getData();
        }
    }


    class AddExpOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {

        public AddExpOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public Field.TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }
    }
}

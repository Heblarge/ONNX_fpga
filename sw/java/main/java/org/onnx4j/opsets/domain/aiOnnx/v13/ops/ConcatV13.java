package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.node.attributes.FloatAttribute;
import org.onnx4j.model.graph.node.attributes.FloatsAttribute;
import org.onnx4j.model.graph.node.attributes.IntAttribute;
import org.onnx4j.model.graph.node.attributes.IntsAttribute;
import org.onnx4j.opsets.domain.aiOnnx.v4.ops.ConcatV4;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.fields.AttributeField;
import org.onnx4j.tensor.DataType;
import java.util.List;

public interface ConcatV13 extends ConcatV4, AiOnnxOperatorV13 {
    public static final Field.TypeConstraint TPYE_CONSTRAINT_T = new Field.TypeConstraint(DataType.allTypes());
    class ConcatInputsV13<T_TENSOR> extends ConcatInputsV4<T_TENSOR> {

        public static final String ATTR_FPGA_IN_SCALES = "fpga_in_scales";
        public static final String ATTR_FPGA_IN_SHIFT = "fpga_in_shift";
        public static final String ATTR_FPGA_OUT_SCALES = "fpga_out_scales";
        public static final String ATTR_FPGA_OUT_SHIFT = "fpga_out_shift";

        protected Field<List<Float>> fpgaInScalesField;
        protected Field<List<Long>> fpgaInShiftField;
        protected Field<List<Float>> fpgaOutScalesField;
        protected Field<List<Long>> fpgaOutShiftField;

        public ConcatInputsV13(Node node, Inputs inputs) {
            super(node, inputs);

            this.fpgaInScalesField = new AttributeField<>(super.attrs, ATTR_FPGA_IN_SCALES, FloatsAttribute.class,  List.of(0.0f), true);
            this.fpgaInShiftField = new AttributeField<>(super.attrs, ATTR_FPGA_IN_SHIFT, IntsAttribute.class,  List.of(0L), true);
            this.fpgaOutScalesField = new AttributeField<>(super.attrs, ATTR_FPGA_OUT_SCALES, FloatsAttribute.class,  List.of(0.0f), true);
            this.fpgaOutShiftField = new AttributeField<>(super.attrs, ATTR_FPGA_OUT_SHIFT, IntsAttribute.class,  List.of(0L), true);

        }

        @Override
        public Field.TypeConstraint getInputFieldsTypeConstraint() {
            return TPYE_CONSTRAINT_T;
        }

        public List<Float> getFpgaInScales() { return fpgaInScalesField.getData(); }
        public List<Long> getFpgaInShift() { return fpgaInShiftField.getData(); }
        public List<Float> getFpgaOutScale() { return fpgaOutScalesField.getData(); }
        public List<Long> getFpgaOutShift() { return fpgaOutShiftField.getData(); }

    }
    class ConcatOutputV13<T_TENSOR> extends ConcatOutputV4<T_TENSOR> {

        public ConcatOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public Field.TypeConstraint getTypeConstraint() {
            return TPYE_CONSTRAINT_T;
        }

    }
}

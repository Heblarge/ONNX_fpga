package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.node.attributes.FloatsAttribute;
import org.onnx4j.model.graph.node.attributes.IntsAttribute;
import org.onnx4j.opsets.domain.aiOnnx.v9.ops.WhereV9;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import org.onnx4j.opsets.operator.fields.AttributeField;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.tensor.DataType;
import java.util.List;


public interface WhereV13 extends WhereV9, AiOnnxOperatorV13 {

    public static final TypeConstraint TYPE_CONSTRAINT_T = new TypeConstraint(DataType.allTypes());

    class WhereInputsV13<T_TENSOR> extends WhereV9.WhereInputsV9<T_TENSOR> {

        public static final String ATTR_FPGA_IN_SCALES = "fpga_in_scales";
        public static final String ATTR_FPGA_IN_SHIFT = "fpga_in_shift";
        public static final String ATTR_FPGA_OUT_SCALES = "fpga_out_scales";
        public static final String ATTR_FPGA_OUT_SHIFT = "fpga_out_shift";

        protected Field<List<Float>> fpgaInScalesField;
        protected Field<List<Long>> fpgaInShiftField;
        protected Field<List<Float>> fpgaOutScalesField;
        protected Field<List<Long>> fpgaOutShiftField;

        private final InputField<T_TENSOR> conditionField;
        private final InputField<T_TENSOR> xField;
        private final InputField<T_TENSOR> yField;

        public WhereInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.conditionField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[0]);
            this.xField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[1]);
            this.yField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[2]);

            this.fpgaInScalesField = new AttributeField<>(super.attrs, ATTR_FPGA_IN_SCALES, FloatsAttribute.class, List.of(0.0f), true);
            this.fpgaInShiftField = new AttributeField<>(super.attrs, ATTR_FPGA_IN_SHIFT, IntsAttribute.class, List.of(0L), true);
            this.fpgaOutScalesField = new AttributeField<>(super.attrs, ATTR_FPGA_OUT_SCALES, FloatsAttribute.class, List.of(0.0f), true);
            this.fpgaOutShiftField = new AttributeField<>(super.attrs, ATTR_FPGA_OUT_SHIFT, IntsAttribute.class, List.of(0L), true);

        }

        public T_TENSOR getCondition() { return conditionField.getData(); }
        public T_TENSOR getX() { return xField.getData(); }
        public T_TENSOR getY() { return yField.getData(); }
        public List<Float> getFpgaInScales() { return fpgaInScalesField.getData(); }
        public List<Long> getFpgaInShift() { return fpgaInShiftField.getData(); }
        public List<Float> getFpgaOutScale() { return fpgaOutScalesField.getData(); }
        public List<Long> getFpgaOutShift() { return fpgaOutShiftField.getData(); }
    }

    class WhereOutputV13<T_TENSOR> extends WhereV9.WhereOutputV9<T_TENSOR> {
        public WhereOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }
    }
}

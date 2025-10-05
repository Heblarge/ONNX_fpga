package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.node.attributes.FloatAttribute;
import org.onnx4j.model.graph.node.attributes.FloatsAttribute;
import org.onnx4j.model.graph.node.attributes.IntAttribute;
import org.onnx4j.model.graph.node.attributes.IntsAttribute;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.AttributeField;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.tensor.DataType;
import java.util.List;

public interface ReluV13 extends AiOnnxOperatorV13 {

    public static final String OP_TYPE = "Relu";
    public static final Field.TypeConstraint TYPE_CONSTRAINT_T = new Field.TypeConstraint(
            DataType.BFLOAT16,
            DataType.FLOAT16,
            DataType.FLOAT,
            DataType.DOUBLE,
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

    class ReluInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {

        public static final String ATTR_FPGA_IN_SCALES = "fpga_in_scales";
        public static final String ATTR_FPGA_IN_SHIFT = "fpga_in_shift";
        public static final String ATTR_FPGA_OUT_SCALES = "fpga_out_scales";
        public static final String ATTR_FPGA_OUT_SHIFT = "fpga_out_shift";

        protected InputField<T_TENSOR> xField;
        protected Field<List<Float>> fpgaInScalesField;
        protected Field<List<Long>> fpgaInShiftField;
        protected Field<Float> fpgaOutScalesField;
        protected Field<Long> fpgaOutShiftField;

        public ReluInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.xField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[0]);
            this.fpgaInScalesField = new AttributeField<>(super.attrs, ATTR_FPGA_IN_SCALES, FloatsAttribute.class, null, true);
            this.fpgaInShiftField = new AttributeField<>(super.attrs, ATTR_FPGA_IN_SHIFT, IntsAttribute.class, null, true);
            this.fpgaOutScalesField = new AttributeField<>(super.attrs, ATTR_FPGA_OUT_SCALES, FloatAttribute.class, null, true);
            this.fpgaOutShiftField = new AttributeField<>(super.attrs, ATTR_FPGA_OUT_SHIFT, IntAttribute.class, null, true);

        }
        
        public T_TENSOR getInput() {
            return xField.getData();
        }
        public List<Float> getFpgaInScales() { return fpgaInScalesField.getData(); }
        public List<Long> getFpgaInShift() { return fpgaInShiftField.getData(); }
        public Float getFpgaOutScale() { return fpgaOutScalesField.getData(); }
        public Long getFpgaOutShift() { return fpgaOutShiftField.getData(); }

    }

    class ReluOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {

        public ReluOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public Field.TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }
    }
}

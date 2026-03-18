package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.node.attributes.FloatsAttribute;
import org.onnx4j.model.graph.node.attributes.IntsAttribute;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.AttributeField;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.tensor.DataType;

import java.util.List;

/**
 * Fused operator performing Softplus activation followed by Negation: f(x) = -ln(1 + e^x).
 */
public interface NegSoftplusV13 extends AiOnnxOperatorV13 {

    public static final String OP_TYPE = "NegSoftplus";

    public static final TypeConstraint TYPE_CONSTRAINT_T = new Field.TypeConstraint(
            DataType.FLOAT, DataType.DOUBLE, DataType.FLOAT16, DataType.BFLOAT16,
            DataType.INT8, DataType.INT16, DataType.INT32, DataType.INT64,
            DataType.UINT8, DataType.UINT16, DataType.UINT32, DataType.UINT64
    );

    @Override
    default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    @Override
    default String getOpType() {
        return OP_TYPE;
    }

    class NegSoftplusInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {

        public static final String ATTR_FPGA_IN_SCALES = "fpga_in_scales";
        public static final String ATTR_FPGA_IN_SHIFT = "fpga_in_shift";
        public static final String ATTR_FPGA_OUT_SCALES = "fpga_out_scales";
        public static final String ATTR_FPGA_OUT_SHIFT = "fpga_out_shift";

        protected InputField<T_TENSOR> xField;
        protected Field<List<Float>> fpgaInScalesField;
        protected Field<List<Long>> fpgaInShiftField;
        protected Field<List<Float>> fpgaOutScalesField;
        protected Field<List<Long>> fpgaOutShiftField;

        public NegSoftplusInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.xField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[0]);

            // 继承 Softplus 的量化属性
            this.fpgaInScalesField = new AttributeField<>(super.attrs, ATTR_FPGA_IN_SCALES, FloatsAttribute.class, List.of(0.0f), true);
            this.fpgaInShiftField = new AttributeField<>(super.attrs, ATTR_FPGA_IN_SHIFT, IntsAttribute.class, List.of(0L), true);
            this.fpgaOutScalesField = new AttributeField<>(super.attrs, ATTR_FPGA_OUT_SCALES, FloatsAttribute.class, List.of(0.0f), true);
            this.fpgaOutShiftField = new AttributeField<>(super.attrs, ATTR_FPGA_OUT_SHIFT, IntsAttribute.class, List.of(0L), true);
        }

        public T_TENSOR getX() {
            return xField.getData();
        }

        public List<Float> getFpgaInScales() {
            return fpgaInScalesField.getData();
        }

        public List<Long> getFpgaInShift() {
            return fpgaInShiftField.getData();
        }

        public List<Float> getFpgaOutScales() {
            return fpgaOutScalesField.getData();
        }

        public List<Long> getFpgaOutShift() {
            return fpgaOutShiftField.getData();
        }
    }

    class NegSoftplusOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {
        public NegSoftplusOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }
    }
}

package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.model.graph.Node;
import org.onnx4j.Inputs;
import org.onnx4j.tensor.DataType;
import org.onnx4j.model.graph.node.attributes.FloatAttribute;
import org.onnx4j.model.graph.node.attributes.FloatsAttribute;
import org.onnx4j.model.graph.node.attributes.IntAttribute;
import org.onnx4j.model.graph.node.attributes.IntsAttribute;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.MatMulV1;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.AttributeField;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import java.util.List;
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

    class MatMulInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {

        public static final Field.TypeConstraint TYPE_CONSTRAINT_T = new Field.TypeConstraint(
                DataType.FLOAT, DataType.FLOAT16, DataType.DOUBLE, DataType.BFLOAT16,
                DataType.INT32, DataType.INT64, DataType.UINT32, DataType.UINT64
        );

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

        public MatMulInputsV13(Node node, Inputs inputs) {
            super(node, inputs);

            this.aField = new InputField<T_TENSOR>(this, TYPE_CONSTRAINT_T, inputArray[0]);
            this.bField = new InputField<T_TENSOR>(this, TYPE_CONSTRAINT_T, inputArray[1]);

            this.fpgaInScalesField = new AttributeField<>(super.attrs, ATTR_FPGA_IN_SCALES, FloatsAttribute.class, null, true);
            this.fpgaInShiftField = new AttributeField<>(super.attrs, ATTR_FPGA_IN_SHIFT, IntsAttribute.class, null, true);
            this.fpgaOutScalesField = new AttributeField<>(super.attrs, ATTR_FPGA_OUT_SCALES, FloatsAttribute.class, null, true);
            this.fpgaOutShiftField = new AttributeField<>(super.attrs, ATTR_FPGA_OUT_SHIFT, IntsAttribute.class, null, true);
        }

        public T_TENSOR getA() { return aField.getData(); }
        public T_TENSOR getB() { return bField.getData(); }
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

    class MatMulOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {

        public MatMulOutputV13(T_TENSOR output) {
            super(output);
        }
        @Override
        public TypeConstraint getTypeConstraint() {
            return MatMulInputsV13.TYPE_CONSTRAINT_T;
        }

    }
}


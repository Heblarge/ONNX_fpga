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

/**
 * Custom Op: Gemm + Relu (fused)
 * Structure follows GemmV13 and ReluV13 layout
 */
public interface GemmReluV13 extends AiOnnxOperatorV13 {

    String OP_TYPE = "GemmRelu";

    // Tensor type constraints same as Gemm
    Field.TypeConstraint TYPE_CONSTRAINT_T = new Field.TypeConstraint(
            DataType.BFLOAT16,
            DataType.FLOAT16,
            DataType.FLOAT,
            DataType.DOUBLE,
            DataType.INT32,
            DataType.INT64,
            DataType.UINT32,
            DataType.UINT64
    );

    @Override
    default String getOpType() {
        return OP_TYPE;
    }

    @Override
    default OperatorStatus getStatus() {
        return OperatorStatus.EXPERIMENTAL;     // <—— 新算子建议 EXPERIMENTAL
    }

    /*==================== Inputs ====================*/
    class GemmReluInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {

        public static final String ATTR_ALPHA = "alpha";
        public static final String ATTR_BETA  = "beta";
        public static final String ATTR_TRANSA = "transA";
        public static final String ATTR_TRANSB = "transB";
        public static final String ATTR_FPGA_IN_SCALES = "fpga_in_scales";
        public static final String ATTR_FPGA_IN_SHIFT = "fpga_in_shift";
        public static final String ATTR_FPGA_OUT_SCALES = "fpga_out_scales";
        public static final String ATTR_FPGA_OUT_SHIFT = "fpga_out_shift";

        protected InputField<T_TENSOR> aField;
        protected InputField<T_TENSOR> bField;
        protected InputField<T_TENSOR> cField;

        protected Field<Float> alphaField;
        protected Field<Float> betaField;
        protected Field<Long> transAField;
        protected Field<Long> transBField;
        protected Field<List<Float>> fpgaInScalesField;
        protected Field<List<Long>> fpgaInShiftField;
        protected Field<List<Float>> fpgaOutScalesField;
        protected Field<List<Long>> fpgaOutShiftField;

        public GemmReluInputsV13(Node node, Inputs inputs) {
            super(node, inputs);

            this.aField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[0]);
            this.bField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[1]);
            this.cField = inputArray.length > 2
                    ? new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[2])
                    : null;

            // attributes follow Gemm defaults
            this.alphaField = new AttributeField<>(attrs, ATTR_ALPHA, FloatAttribute.class, 1.0f, false);
            this.betaField  = new AttributeField<>(attrs, ATTR_BETA, FloatAttribute.class, 1.0f, false);
            this.transAField = new AttributeField<>(attrs, ATTR_TRANSA, IntAttribute.class, 0L, false);
            this.transBField = new AttributeField<>(attrs, ATTR_TRANSB, IntAttribute.class, 0L, false);
            this.fpgaInScalesField = new AttributeField<>(super.attrs, ATTR_FPGA_IN_SCALES, FloatsAttribute.class,  List.of(0.0f), true);
            this.fpgaInShiftField = new AttributeField<>(super.attrs, ATTR_FPGA_IN_SHIFT, IntsAttribute.class,  List.of(0L,0L), true);
            this.fpgaOutScalesField = new AttributeField<>(super.attrs, ATTR_FPGA_OUT_SCALES, FloatsAttribute.class,  List.of(0.0f), true);
            this.fpgaOutShiftField = new AttributeField<>(super.attrs, ATTR_FPGA_OUT_SHIFT, IntsAttribute.class,  List.of(0L), true);

        }

        public T_TENSOR getA() { return aField.getData(); }
        public T_TENSOR getB() { return bField.getData(); }
        public boolean hasC() { return cField != null; }
        public T_TENSOR getC() { return cField.getData(); }

        public float getAlpha() { return alphaField.getData(); }
        public float getBeta()  { return betaField.getData(); }
        public long getTransA() { return transAField.getData(); }
        public long getTransB() { return transBField.getData(); }
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

    /*==================== Output ====================*/
    class GemmReluOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {

        public GemmReluOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public Field.TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }
    }
}

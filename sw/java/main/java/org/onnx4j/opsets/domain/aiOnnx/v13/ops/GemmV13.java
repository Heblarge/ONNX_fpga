package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.node.attributes.FloatAttribute;
import org.onnx4j.model.graph.node.attributes.IntAttribute;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.AttributeField;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.tensor.DataType;

public interface GemmV13 extends AiOnnxOperatorV13 {

    String OP_TYPE = "GeMM";
    TypeConstraint TYPE_CONSTRAINT_T = new Field.TypeConstraint(
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
    default OperatorStatus getStatus() {
        return OperatorStatus.STABLE;
    }

    @Override
    default String getOpType() {
        return OP_TYPE;
    }

    class GeMMInputsV13<T_TENSOR> extends OperatorInputs<T_TENSOR> {
        public static final String ATTR_ALPHA = "alpha";
        public static final String ATTR_BETA = "beta";
        public static final String ATTR_TRANSA = "transA";
        public static final String ATTR_TRANSB = "transB";

        protected InputField<T_TENSOR> aField;
        protected InputField<T_TENSOR> bField;
        protected InputField<T_TENSOR> cField;
        protected Field<Float> alphaField;
        protected Field<Float> betaField;
        protected Field<Long> transAField;
        protected Field<Long> transBField;

        public GeMMInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
            this.aField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[0]);
            this.bField = new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[1]);
            this.cField = inputArray.length > 2
                    ? new InputField<>(this, TYPE_CONSTRAINT_T, inputArray[2])
                    : null;
            this.alphaField = new AttributeField<>(super.attrs, ATTR_ALPHA, FloatAttribute.class, 1.0f, false);
            this.betaField  = new AttributeField<>(super.attrs, ATTR_BETA, FloatAttribute.class, 1.0f, false);
            this.transAField = new AttributeField<>(super.attrs, ATTR_TRANSA, IntAttribute.class, 0L, false);
            this.transBField = new AttributeField<>(super.attrs, ATTR_TRANSB, IntAttribute.class, 0L, false);
        }

        public T_TENSOR getA() {
            return aField.getData();
        }

        public T_TENSOR getB() {
            return bField.getData();
        }

        public boolean hasC() {
            return cField != null;
        }

        public T_TENSOR getC() {
            if (!hasC()) {
                throw new IllegalStateException("Gemm operation missing optional input C");
            }
            return cField.getData();
        }

        public float getAlpha() {
            return alphaField.getData();
        }

        public float getBeta() {
            return betaField.getData();
        }

        public long getTransA() {
            return transAField.getData();
        }

        public long getTransB() {
            return transBField.getData();
        }
    }

    class GeMMOutputV13<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {
        public GeMMOutputV13(T_TENSOR output) {
            super(output);
        }

        @Override
        public TypeConstraint getTypeConstraint() {
            return TYPE_CONSTRAINT_T;
        }
    }
}

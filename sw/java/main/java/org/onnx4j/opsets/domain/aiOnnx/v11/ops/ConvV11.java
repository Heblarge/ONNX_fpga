package org.onnx4j.opsets.domain.aiOnnx.v11.ops;

import java.util.List;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.model.graph.node.attributes.IntAttribute;
import org.onnx4j.model.graph.node.attributes.IntsAttribute;
import org.onnx4j.model.graph.node.attributes.StringAttribute;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.ConvV1;
import org.onnx4j.opsets.domain.aiOnnx.v11.AiOnnxOperatorV11;
import org.onnx4j.opsets.operator.Field;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import org.onnx4j.opsets.operator.fields.AttributeField;
import org.onnx4j.opsets.operator.output.SingleOperatorOutputs;
import org.onnx4j.tensor.DataType;


public interface ConvV11 extends ConvV1, AiOnnxOperatorV11 {

    String OP_TYPE = "Slicer";
    TypeConstraint TYPE_CONSTRAINT_T = new Field.TypeConstraint(DataType.FLOAT16, DataType.FLOAT, DataType.DOUBLE);

    @Override default String getOpType() { return OP_TYPE; }
    @Override default OperatorStatus getStatus() { return OperatorStatus.STABLE; }


    class ConvInputsV11<T_TENSOR> extends ConvV1.ConvInputsV1<T_TENSOR> {
        private final AttributeField<String>     autoPadField;
        private final AttributeField<List<Long>> dilationsField;
        private final AttributeField<Long>       groupField;
        private final AttributeField<List<Long>> kernelShapeField;
        private final AttributeField<List<Long>> padsField;
        private final AttributeField<List<Long>> stridesField;
        public ConvInputsV11(Node node, Inputs inputs) {
            super(node, inputs);
            this.autoPadField     = new AttributeField<>(super.attrs, ATTR_AUTO_PAD,     StringAttribute.class, "NOTSET", false);
            this.dilationsField   = new AttributeField<>(super.attrs, ATTR_DILATIONS,    IntsAttribute.class, null, true);
            this.groupField       = new AttributeField<>(super.attrs, ATTR_GROUP,        IntAttribute.class, 1L, true);
            this.kernelShapeField = new AttributeField<>(super.attrs, ATTR_KERNEL_SHAPE, IntsAttribute.class, null, false);
            this.padsField        = new AttributeField<>(super.attrs, ATTR_PADS,         IntsAttribute.class, null, false);
            this.stridesField     = new AttributeField<>(super.attrs, ATTR_STRIDES,      IntsAttribute.class, null, false);
        }
        public String     getAutoPad()     { return autoPadField.getData(); }
        public List<Long> getDilations()   { return dilationsField.getData(); }
        public Long       getGroup()       { return groupField.getData(); }
        public List<Long> getKernelShape() { return kernelShapeField.getData(); }
        public List<Long> getPads()        { return padsField.getData(); }
        public List<Long> getStrides()     { return stridesField.getData(); }
    }

    class ConvOutputV11<T_TENSOR> extends SingleOperatorOutputs<T_TENSOR> {
        public ConvOutputV11(T_TENSOR output) { super(output); }
        @Override public TypeConstraint getTypeConstraint() { return TYPE_CONSTRAINT_T; }
    }
}

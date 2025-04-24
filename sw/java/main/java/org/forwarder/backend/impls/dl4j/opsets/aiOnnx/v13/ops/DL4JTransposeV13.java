

package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import java.util.List;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v1.ops.DL4JTransposeV1;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.TransposeV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import com.google.common.primitives.Ints;


public class DL4JTransposeV13 extends DL4JTransposeV1 implements TransposeV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        return super.forward(node, inputs);
    }



    @Override
    protected INDArray transpose(INDArray data, List<Long> perm) {
        return super.transpose(data, perm);
    }
}
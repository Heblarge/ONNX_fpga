

package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import org.forwarder.backend.impls.HWAccelerated.opsets.v1.ops.HWAcceleratedTransposeV1;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.TransposeV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import java.util.List;


public class HWAcceleratedTransposeV13 extends HWAcceleratedTransposeV1 implements TransposeV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        return super.forward(node, inputs);
    }



    @Override
    protected INDArray transpose(INDArray data, List<Long> perm) {
        return super.transpose(data, perm);
    }
}
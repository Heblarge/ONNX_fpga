package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.GemmReluV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class DL4JGemmReluV13 extends DL4JAiOnnxOperator implements GemmReluV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {

        // === 1. 解析 Inputs（完全由 proto 定义） ===
        GemmReluInputsV13<INDArray> castedInputs =
                new GemmReluInputsV13<>(node, inputs);

        INDArray A = castedInputs.getA();
        INDArray B = castedInputs.getB();
        INDArray C = castedInputs.hasC() ? castedInputs.getC() : null;

        float alpha = castedInputs.getAlpha();
        float beta  = castedInputs.getBeta();
        long transA = castedInputs.getTransA();
        long transB = castedInputs.getTransB();

        if (transA != 0L) A = A.transpose();
        if (transB != 0L) B = B.transpose();

        INDArray Y = A.mmul(B).mul(alpha);

        if (C != null) {
            if (!java.util.Arrays.equals(Y.shape(), C.shape())) {
                C = C.broadcast(Y.shape());
            }
            Y = Y.add(C.mul(beta));
        }

        // === 3. ReLU ===
        INDArray out = Transforms.relu(Y);

        // === 4. 返回 proto 中定义的 Output ===
        return new GemmReluOutputV13<>(out);
    }
}

package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.AddExpV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class DL4JAddExpV13 extends DL4JAiOnnxOperator implements AddExpV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {

        AddExpInputsV13<INDArray> castedInputs =
                new AddExpInputsV13<>(node, inputs);

        INDArray a = castedInputs.getA();
        INDArray b = castedInputs.getB();

        return new AddExpOutputV13<>(this.addExp(a, b));
    }


    protected INDArray addExp(INDArray a, INDArray b) {

        //  Add (支持 broadcast)
        INDArray sum = a.add(b);

        //  Exp
        return Transforms.exp(sum);
    }
}


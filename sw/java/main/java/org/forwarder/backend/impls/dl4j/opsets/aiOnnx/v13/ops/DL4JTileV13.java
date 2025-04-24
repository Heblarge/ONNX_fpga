package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.TileV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class DL4JTileV13 extends DL4JAiOnnxOperator implements TileV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        TileV13.TileInputsV13<INDArray> castedInputs = new TileV13.TileInputsV13<>(node, inputs);
        INDArray input = castedInputs.getInput();
        INDArray repeats = castedInputs.getRepeats();
        return new TileV13.TileOutputV13<>(this.tile(input, repeats));
    }

    public INDArray tile(INDArray input, INDArray repeats) {
        if (input.isEmpty()) {
            return input;
        }


        if (!repeats.dataType().equals(DataType.LONG)) {
            throw new IllegalArgumentException("Repeats tensor must be of type long");
        }


        if (input.rank() != repeats.length()) {
            throw new IllegalArgumentException(
                    String.format("Input rank (%d) and repeats length (%d) must match",
                            input.rank(), repeats.length()));
        }


        int[] repeatDims = new int[(int)repeats.length()];
        for (int i = 0; i < repeats.length(); i++) {
            long repeat = repeats.getLong(i);


            if (repeat < 0) {
                throw new IllegalArgumentException(
                        String.format("Repeat value %d at dimension %d cannot be negative",
                                repeat, i));
            }


            if (repeat > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        String.format("Repeat value %d at dimension %d exceeds Integer.MAX_VALUE",
                                repeat, i));
            }

            repeatDims[i] = (int)repeat;
        }


        return Nd4j.tile(input, repeatDims);
    }
}
package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.operator.OperatorOutputs;
import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v6.ops.DL4JCastV6;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.CastV13;
import org.onnx4j.tensor.DataType;

public class DL4JCastV13 extends DL4JCastV6 implements CastV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        CastInputsV13<INDArray> castedInputs = new CastInputsV13<>(node, inputs);
        INDArray input = castedInputs.getInput();
        DataType targetType = DataType.valueOf(castedInputs.getTo()); // 获取目标数据类型

        // 执行类型转换
        INDArray output = cast(input, targetType);

        return new CastOutputV13<>(output);
    }

    /**
     * 核心转换逻辑：将输入 INDArray 转换为目标数据类型
     */
    protected INDArray cast(INDArray input, DataType targetType) {
        // ND4J 的转换方法（根据 ONNX 数据类型映射到 ND4J 类型）
        switch (targetType) {
            case FLOAT:    // float32
                return input.castTo(Nd4j.defaultFloatingPointType());
            case UINT8:    // uint8
                return input.castTo(org.nd4j.linalg.api.buffer.DataType.UBYTE);
            case INT8:    // int8
                return input.castTo(org.nd4j.linalg.api.buffer.DataType.BYTE);
            case INT16:   // int16
                return input.castTo(org.nd4j.linalg.api.buffer.DataType.SHORT);
            case INT32:   // int32
                return input.castTo(org.nd4j.linalg.api.buffer.DataType.INT);
            case INT64:   // int64
                return input.castTo(org.nd4j.linalg.api.buffer.DataType.LONG);
            case BOOL:    // bool
                return input.castTo(org.nd4j.linalg.api.buffer.DataType.BOOL);
            case FLOAT16:  // float16
                return input.castTo(org.nd4j.linalg.api.buffer.DataType.HALF);
            case DOUBLE:   // float64
                return input.castTo(org.nd4j.linalg.api.buffer.DataType.DOUBLE);
            default:
                throw new UnsupportedOperationException(
                        String.format("Unsupported target data type: %s", targetType));
        }
    }

    // 输入类（继承自 CastInputsV13）
    public static class CastInputsV13<T_TENSOR> extends CastV13.CastInputsV13<T_TENSOR> {
        public CastInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
        }
    }

    // 输出类（继承自 CastOutputV13）
    public static class CastOutputV13<T_TENSOR> extends CastV13.CastOutputV13<T_TENSOR> {
        public CastOutputV13(T_TENSOR output) {
            super(output);
        }
    }
}
package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.MatMulV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class DL4JMatMulV13 extends DL4JAiOnnxOperator implements MatMulV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        MatMulInputsV1<INDArray> castedOperatorInputs = new MatMulInputsV1<>(node, inputs);
        INDArray a = castedOperatorInputs.getA();
        INDArray b = castedOperatorInputs.getB();

        INDArray result = this.matmul(a, b);
        return new MatMulOutputV1<>(result);
    }

    /**
     * 支持 int、long、float、double，确保输出类型与输入一致
     */
    protected INDArray matmul(INDArray a, INDArray b) {
        DataType typeA = a.dataType();
        DataType typeB = b.dataType();

        // 决定输出类型（相同类型优先，浮点优先）
        DataType targetType = resolveCommonType(typeA, typeB);

        // 转为可计算类型（float 或 double）
        INDArray aCalc = convertToComputableType(a);
        INDArray bCalc = convertToComputableType(b);

        // 执行乘法（mmul 只支持 float/double）
        INDArray result = aCalc.mmul(bCalc);

        // 转回目标类型（如 INT 或 LONG）
        if (!result.dataType().equals(targetType)) {
            result = result.castTo(targetType);
        }

        return result;
    }

    /**
     * 判断目标输出类型（类型优先级：double > float > long > int）
     */
    private DataType resolveCommonType(DataType a, DataType b) {
        if (a == b) return a;
        if (isDoubleLike(a) || isDoubleLike(b)) return DataType.DOUBLE;
        if (isFloatLike(a) || isFloatLike(b)) return DataType.FLOAT;
        if (isLongLike(a) || isLongLike(b)) return DataType.LONG;
        return DataType.INT; // fallback
    }

    private INDArray convertToComputableType(INDArray x) {
        DataType type = x.dataType();
        if (isFloatLike(type)) return x;
        if (isDoubleLike(type)) return x;
        if (isIntLike(type)) return x.castTo(DataType.FLOAT);
        if (isLongLike(type)) return x.castTo(DataType.DOUBLE);
        return x.castTo(DataType.FLOAT); // default fallback
    }

    // 类型判断辅助
    private boolean isFloatLike(DataType t) {
        return t == DataType.FLOAT;
    }

    private boolean isDoubleLike(DataType t) {
        return t == DataType.DOUBLE;
    }

    private boolean isIntLike(DataType t) {
        return t == DataType.INT;
    }

    private boolean isLongLike(DataType t) {
        return t == DataType.LONG;
    }
}



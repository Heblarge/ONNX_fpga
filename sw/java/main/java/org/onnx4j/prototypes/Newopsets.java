package org.onnx4j.prototypes;

import org.onnx4j.prototypes.OnnxOperatorsProto3.OperatorProto;
import org.onnx4j.prototypes.OnnxOperatorsProto3.OperatorSetProto;
import org.onnx4j.prototypes.OnnxOperatorsProto3.OperatorStatus;

public class Newopsets {

    public static OperatorSetProto getNewOpset() {
        // 1️⃣ 创建 GemmRelu 算子
        OperatorProto gemmRelu = OperatorProto.newBuilder()
                .setOpType("GemmRelu")
                .setSinceVersion(1)
                .setStatus(OperatorStatus.STABLE)
                .setDocString("Fused operator performing Gemm (A*B + C) followed by ReLU activation.")
                .build();

        // 2️⃣ 创建 OperatorSetProto，并加入自定义算子
        OperatorSetProto NewOpset = OperatorSetProto.newBuilder()
                .setMagic("ONNXOPSET")
                .setIrVersion(9)         // 对应你项目使用的 IR_VERSION
                .setDomain("")           // 空域表示标准域
                .setOpsetVersion(13)      // 自定义 opset 版本
                .addOperator(gemmRelu)
                .build();

        return NewOpset;
    }

    // 测试打印
    public static void main(String[] args) {
        OperatorSetProto opset = getNewOpset();
        System.out.println(opset);
    }
}

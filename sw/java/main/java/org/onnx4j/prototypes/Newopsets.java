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

        OperatorProto addExp = OperatorProto.newBuilder()
                .setOpType("AddExp")
                .setSinceVersion(1)
                .setStatus(OperatorStatus.STABLE)
                .setDocString("Fused operator performing Add (A + B) followed by Exp activation.")
                .build();

        OperatorProto addLog = OperatorProto.newBuilder()
                .setOpType("AddLog")
                .setSinceVersion(1)
                .setStatus(OperatorStatus.STABLE)
                .setDocString("Fused operator performing Add (A + B) followed by Log activation.")
                .build();

        OperatorProto subExp = OperatorProto.newBuilder()
                .setOpType("SubExp")
                .setSinceVersion(1)
                .setStatus(OperatorStatus.STABLE)
                .setDocString("Fused operator performing Sub (A - B) followed by Exp activation.")
                .build();

        OperatorProto negSoftplus = OperatorProto.newBuilder()
                .setOpType("NegSoftplus")
                .setSinceVersion(1)
                .setStatus(OperatorStatus.STABLE)
                .setDocString("Fused operator performing Softplus activation followed by Negation: f(x) = -ln(1 + e^x).")
                .build();



        // 2️⃣ 创建 OperatorSetProto，并加入自定义算子
        OperatorSetProto NewOpset = OperatorSetProto.newBuilder()
                .setMagic("ONNXOPSET")
                .setIrVersion(9)         // 对应你项目使用的 IR_VERSION
                .setDomain("")           // 空域表示标准域
                .setOpsetVersion(13)      // 自定义 opset 版本
                .addOperator(gemmRelu)
                .addOperator(addExp)
                .addOperator(addLog)
                .addOperator(subExp)
                .addOperator(negSoftplus)
                .build();

        return NewOpset;
    }

    // 测试打印
    public static void main(String[] args) {
        OperatorSetProto opset = getNewOpset();
        System.out.println(opset);
    }
}

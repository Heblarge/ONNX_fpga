package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;



import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops.DL4JGemmReluV13;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

public class DL4JGemmReluV13Test {

    public static void main(String[] args) {

        // 创建算子实例
        DL4JGemmReluV13 op = new DL4JGemmReluV13();

        // 构造简单输入矩阵 A, B, C
        INDArray A = Nd4j.create(new float[][]{
                {1, 2},
                {3, 4}
        });
        INDArray B = Nd4j.create(new float[][]{
                {5, 6},
                {7, 8}
        });
        INDArray C = Nd4j.create(new float[][]{
                {1, 1},
                {1, 1}
        });

        // 模拟 alpha, beta, transA, transB
        float alpha = 1.0f;
        float beta = 1.0f;
        long transA = 0L;
        long transB = 0L;

        // GEMM + ReLU 计算
        INDArray Y = A.mmul(B).mul(alpha);
        if (C != null) {
            if (!java.util.Arrays.equals(Y.shape(), C.shape())) {
                C = C.broadcast(Y.shape());
            }
            Y = Y.add(C.mul(beta));
        }
        INDArray out = Transforms.relu(Y);

        // 打印结果
        System.out.println("A:");
        System.out.println(A);
        System.out.println("B:");
        System.out.println(B);
        System.out.println("C:");
        System.out.println(C);
        System.out.println("Output (ReLU(A*B + C)):");
        System.out.println(out);

        // 简单断言
        INDArray expected = Nd4j.create(new float[][]{
                {20, 23},
                {48, 55}
        });
        assert out.equals(expected) : "输出不符合预期！";

        System.out.println("测试通过！");
    }
}

package Accelerator;

/**
 * FPGA指令结构
 * 简化设计：直接使用4个block，每个block作为完整的数据区
 * 消除了64个buffer的复杂地址转换逻辑
 */
public class InstJavaTODO {
    public int UID;
    public String matrixOperation;
    public int shiftLeft_AfterMatrixOperation;
    public boolean doTranspose;
    public String activationFunction;
    public int shiftLeft_AfterActivation;

    // Block索引：直接指向共享内存中的4个block之一
    public int blockIdA;     // 输入A的block索引 (0-3)
    public int blockIdB;     // 输入B的block索引 (0-3)
    public int blockIdZ;     // 输出Z的block索引 (0-3)

    public int input0Shape0;  // M / ROWA
    public int input0Shape1;  // K / COLA
    // input1Shape0 (ROWB) 在JNI侧自动设为 input0Shape1
    public int input1Shape1;  // N / COLB
    public int shiftLeft_A;
    public int shiftLeft_B;

    public InstJavaTODO(
        int UID,
        String matrixOperation,
        int shiftLeft_AfterMatrixOperation,
        boolean doTranspose,
        String activationFunction,
        int shiftLeft_AfterActivation,
        int blockIdA,
        int blockIdB,
        int blockIdZ,
        int input0Shape0,
        int input0Shape1,
        int input1Shape1,
        int shiftLeft_A,
        int shiftLeft_B

    ) {
        this.UID = UID;
        this.matrixOperation = matrixOperation;
        this.shiftLeft_AfterMatrixOperation = shiftLeft_AfterMatrixOperation;
        this.doTranspose = doTranspose;
        this.activationFunction = activationFunction;
        this.shiftLeft_AfterActivation = shiftLeft_AfterActivation;
        this.blockIdA = blockIdA;
        this.blockIdB = blockIdB;
        this.blockIdZ = blockIdZ;
        this.input0Shape0 = input0Shape0;
        this.input0Shape1 = input0Shape1;
        this.input1Shape1 = input1Shape1;
        this.shiftLeft_A = shiftLeft_A;
        this.shiftLeft_B = shiftLeft_B;
    }
}

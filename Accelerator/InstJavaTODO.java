package Accelerator;

import spinal.core.U;

/**
 * FPGA指令结构
 * 使用预分配buffer池方案：R5根据bufferId从池中获取物理地址
 */
public class InstJavaTODO {
    public int UID;
    public String matrixOperation;
    public int shiftLeft_AfterMatrixOperation;
    public boolean doTranspose;
    public String activationFunction;
    public int shiftLeft_AfterActivation;

    // Buffer索引：R5根据索引从预分配池获取物理地址
    public int bufferIdA;     // 输入A的buffer索引 (0-15)
    public int bufferIdB;     // 输入B的buffer索引 (0-15)
    public int bufferIdZ;     // 输出Z的buffer索引 (0-15)

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
        int bufferIdA,
        int bufferIdB,
        int bufferIdZ,
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
        this.bufferIdA = bufferIdA;
        this.bufferIdB = bufferIdB;
        this.bufferIdZ = bufferIdZ;
        this.input0Shape0 = input0Shape0;
        this.input0Shape1 = input0Shape1;
        this.input1Shape1 = input1Shape1;
        this.shiftLeft_A = shiftLeft_A;
        this.shiftLeft_B = shiftLeft_B;
    }
}

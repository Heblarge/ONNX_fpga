package Accelerator;

import spinal.core.U;

public class InstJavaTODO {
    public int UID;
    public String matrixOperation;
    public int shiftLeft_AfterMatrixOperation;
    public boolean doTranspose;
    public String activationFunction;
    public int shiftLeft_AfterActivation;
    public int input0Address;
    public int input1Address;
    public int outputAddress;
    public int input0Shape0;
    public int input0Shape1;
    public int input1Shape1;
    public int shiftLeft_A;
    public int shiftLeft_B;

    public InstJavaTODO(
        int UID,
        String matrixOperation,
        int shiftLeft_AfterMatrixOperation,
        boolean doTranspose,
        String activationFunction,
        int shiftLeft_AfterActivation,
        int input0Address,
        int input1Address,
        int outputAddress,
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
        this.input0Address = input0Address;
        this.input1Address = input1Address;
        this.outputAddress = outputAddress;
        this.input0Shape0 = input0Shape0;
        this.input0Shape1 = input0Shape1;
        this.input1Shape1 = input1Shape1;
        this.shiftLeft_A = shiftLeft_A;
        this.shiftLeft_B = shiftLeft_B;
    }
}

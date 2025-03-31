package OnnxSpinalHDLInterface;
import org.forwarder.demo.opset.OpsetTest;
import org.forwarder.backend.impls.dl4j.opsets.MemoryTest;

import org.onnx4j.Tensor;
import org.onnx4j.tensor.TensorBuilder;
import org.onnx4j.TensorManager;
import org.onnx4j.tensor.DataType;
import org.onnx4j.tensor.Shape;

public class onnx4j_run {
    
    public static void main(String[] args){

        OpsetTest opsettest = new OpsetTest();
        System.out.println("run testRegisterNewOpset()");
        opsettest.testRegisterNewOpset();
        System.out.println("run testRegisterNewOpset()");
        opsettest.testRegisterNewOpset();
        MemoryTest memorytest = new MemoryTest();
        System.out.println("run testMemory()");
        memorytest.testMemory();
        //System.out.println("run  testLinearRegression()");
        //memorytest.testLinearRegression();
        
	}
    
}
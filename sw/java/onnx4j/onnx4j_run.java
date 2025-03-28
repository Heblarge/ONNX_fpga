package OnnxSpinalHDLInterface;
import org.forwarder.demo.opset.OpsetTest;
import org.forwarder.backend.impls.dl4j.opsets.MemoryTest;

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
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
        TensorManager<Tensor> tsMgr = new TensorManager<Tensor>() {

			@Override
			protected void dispose(Tensor tensor) {
				tensor.close();
			}

		};
        TensorBuilder builder = TensorBuilder.builder(DataType.FLOAT, Shape.create(2L, 3L, 3L), Tensor.options()).manager(tsMgr);
        for (int n = 0; n < 2 * 3 * 3; n++) {
			builder.putFloat(Float.valueOf(n));
		}
        try (Tensor ts = builder.build()) {
			String expectedOutput = "Tensor[2, 3, 3] = [[[0.0,1.0,2.0],[3.0,4.0,5.0],[6.0,7.0,8.0],],[[9.0,10.0,11.0],[12.0,13.0,14.0],[15.0,16.0,17.0],],]";
			String actualOutput = ts.toString().replaceAll("[\t\n]", "");
			//assertEquals(expectedOutput, actualOutput);
            if(expectedOutput==actualOutput){System.out.println(actualOutput);}else{System.out.println("Expected: " + expectedOutput + ", but got: " + actualOutput);}
			
		}
	}
    
}
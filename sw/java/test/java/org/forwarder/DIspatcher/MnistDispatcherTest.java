package org.forwarder.DIspatcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.naming.OperationNotSupportedException;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.forwarder.demo.ForwarderTestCase;

public class MnistDispatcherTest extends ForwarderTestCase {
    public MnistDispatcherTest(String testName) {
        super(testName);
    }
    public static Test suite() throws FileNotFoundException, IOException {
        return new TestSuite(MnistDispatcherTest.class);
    }
    public void testDispatcherForOpsetV8() throws FileNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, OperationNotSupportedException, IOException {
        String modelPath = "/mnist/opset_v8/model.onnx";
        Dispatcher dispatcher = new Dispatcher(modelPath);
        List<Instruction> instructions = dispatcher.generateInstructions();
        for (Instruction instr : instructions) {
            System.out.println(instr);
        }
    }
}

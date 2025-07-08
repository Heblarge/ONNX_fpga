import org.junit.Test;
import projectname.ExpSim;
import scala.Tuple2;
import scala.collection.JavaConverters;
import scala.collection.Seq;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExpSimJavaTest {
    @Test
    public void testRunSim() {
        // 构造输入序列
        float[] testInputs = new float[]{0.0f, 1.0f, -1.0f, 2.5f, -2.5f, 3.45f, 5.0f, 8.5f};

        double[] expectedOutputs = new double[testInputs.length];
        for (int i = 0; i < testInputs.length; i++) {
            expectedOutputs[i] = Math.exp(testInputs[i]);
        }
        System.out.println("Starting simulation with inputs: " + Arrays.toString(testInputs));

        float[] actualOutputs = ExpSim.runSim(testInputs);
        System.out.println("Received results from simulation: " + Arrays.toString(actualOutputs));

        assertEquals("The number of outputs should match the number of inputs.", testInputs.length, actualOutputs.length);


        double delta = 0.02;

        System.out.printf("%-10s | %-20s | %-20s | %-15s%n", "Input (x)", "Expected (e^x)", "Actual (e^x)", "Relative Error");
        System.out.println(new String(new char[75]).replace('\0', '-'));

        for (int i = 0; i < actualOutputs.length; i++) {
            double expected = expectedOutputs[i];
            double actual = actualOutputs[i];
            double relativeError = Math.abs((actual - expected) / expected);

            System.out.printf("%-10.3f | %-20.4f | %-20.4f | %-15.2f%%%n", testInputs[i], expected, actual, relativeError * 100);

            // Assert that the actual result is close to the expected result within the defined tolerance.
            assertEquals("Mismatch for input " + testInputs[i], expected, actual, expected * delta);
        }
        System.out.println(new String(new char[75]).replace('\0', '-'));
        System.out.println("Test passed: All outputs are within the acceptable error margin.");
    }
}

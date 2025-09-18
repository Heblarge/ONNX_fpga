/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.forwarder.demo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import javax.naming.OperationNotSupportedException;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Unit test for MNIST model.
 */
public class ModelTest extends FWTestCase {

    //private static Logger logger = LoggerFactory.getLogger(MnistModelTest.class);

    /**
     * Create the test case
     *
     * @param testName
     *            name of the test case
     * @throws IOException
     * @throws FileNotFoundException
     */
    public ModelTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static Test suite() throws FileNotFoundException, IOException {
        return new TestSuite(ModelTest.class);
    }


    public void testModelWithOpsetV13() throws FileNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, OperationNotSupportedException, IOException {
        Map<List<String>, List<String>> tensorPairPaths = new LinkedHashMap<>();
        for (int n = 1; n < 2; n++) {
            List<String> inputs = List.of(
                    "/mnist/Quantized/data" + n + "/input_seq_pc.pb",
                    "/mnist/Quantized/data" + n + "/input_seq_pos.pb"
            );
            List<String> outputs = List.of(
                    "/mnist/Quantized/data" + n + "/output_pre_tran.pb",
                    "/mnist/Quantized/data" + n + "/output_rot.pb",
                    "/mnist/Quantized/data" + n + "/output_trj.pb"
            );
            tensorPairPaths.put(inputs, outputs);
        }

        super.testModel(
                tensorPairPaths,
                "/mnist/onnx_graph/NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_OnnxRuntime_ir_version5.onnx",
                List.of("seq_pc", "seq_pos"), // 假设两个输入名
                List.of("pre_trans", "rot", "trj"), // 假设三个输出名
                new String[] { "HWAccelerated" },
                0.0001f
        );
    }

}

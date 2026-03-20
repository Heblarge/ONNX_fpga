/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at

 *     http://www.apache.org/licenses/LICENSE-2.0

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
//import org.junit.Test;

/**
 * Unit test for model.
 */
public class ModelTest extends FWTestCase {

    //private static Logger logger = LoggerFactory.getLogger(MnistModelTest.class);

//    /**
//     * Create the test case
//     *
//     * @param testName
//     *            name of the test case
//     * @throws IOException
//     * @throws FileNotFoundException
////     */
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

//    public void testModelWithOpsetV13_hw() throws FileNotFoundException, NoSuchMethodException,
//            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
//            InvocationTargetException, OperationNotSupportedException, IOException {
//        Map<List<String>, List<String>> tensorPairPaths = new LinkedHashMap<>();
//        Map<String, String> backendPaths = new HashMap<>();
//        String currentDir = System.getProperty("user.dir");
//        backendPaths.put(
//                "HWAccelerated",
//                // 输出文件夹的位置
//                currentDir + "/java_hw_final_layer_outputs_2");
//        for (int n = 10; n < 11; n++) {
//            List<String> inputs = List.of(
//                    "/mnist/int32_refpb/int32/data" + n + "/input_seq_pc.pb",
//                    "/mnist/int32_refpb/int32/data" + n + "/input_seq_pos.pb"
//            );
//            List<String> outputs = List.of(
//                    "/mnist/int32_refpb/int32/data" + n + "/output_pre_tran.pb",
//                    "/mnist/int32_refpb/int32/data" + n + "/output_rot.pb",
//                    "/mnist/int32_refpb/int32/data" + n + "/output_trj.pb"
//            );
//            tensorPairPaths.put(inputs, outputs);
//        }
//        super.testModel(
//                tensorPairPaths,
//                "/mnist/onnx_graph/NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_FPGA.onnx",
//                List.of("seq_pc", "seq_pos"),       // 假设两个输入名
//                List.of("pre_trans", "rot", "trj"), // 假设三个输出名
//                new String[] {
//                        //"DL4J"
//                        //,
//                        "HWAccelerated"
//                },
//                0.0001f,
//                backendPaths,
//                SaveMode.FINAL_ONLY,
//                OutputMode.Dequantize
//        );
//    }
//
//    public void testModelWithOpsetV13_dl4j_quantized() throws FileNotFoundException, NoSuchMethodException,
//            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
//            InvocationTargetException, OperationNotSupportedException, IOException {
//        Map<List<String>, List<String>> tensorPairPaths = new LinkedHashMap<>();
//        Map<String, String> backendPaths = new HashMap<>();
//        String currentDir = System.getProperty("user.dir");
//        backendPaths.put(
//                "DL4J",
//                // 输出文件夹的位置
//                currentDir+ "/java_dl4j_quantized_all_layer_outputs");
//        for (int n = 10; n < 11 ; n++) {
//            List<String> inputs = List.of(
//                    "/mnist/Quantized/data" + n + "/input_seq_pc.pb",
//                    "/mnist/Quantized/data" + n + "/input_seq_pos.pb"
//            );
//            List<String> outputs = List.of(
//                    "/mnist/Quantized/data" + n + "/output_pre_tran.pb",
//                    "/mnist/Quantized/data" + n + "/output_rot.pb",
//                    "/mnist/Quantized/data" + n + "/output_trj.pb"
//            );
//            tensorPairPaths.put(inputs, outputs);
//        }
//        super.testModel(
//                tensorPairPaths,
//                "/mnist/onnx_graph/NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_OnnxRuntime_0306.onnx",
//                List.of("seq_pc", "seq_pos_fp"),       // 假设两个输入名
//                List.of("pre_trans_fp", "rot", "trj"), // 假设三个输出名
//                new String[] {
//                        "DL4J"
//                        //,
//                        //"HWAccelerated"
//                },
//                0.0001f,
//                backendPaths,
//                SaveMode.ALL_INTERMEDIATE,
//                OutputMode.Normal
//        );
//    }
//

    // 四组数据集：
    public void testModelWithOpsetV13_hw_FM() throws FileNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, OperationNotSupportedException, IOException {
        Map<List<String>, List<String>> tensorPairPaths = new LinkedHashMap<>();
        Map<String, String> backendPaths = new HashMap<>();
        String currentDir = System.getProperty("user.dir");
        backendPaths.put(
                "HWAccelerated",
                // 输出文件夹的位置
                currentDir + "/datasets_results/FPGA_FM_final_layer");
        //[12, 14, 27, 45, 60, 88, 99, 111,
        // 128, 175, 189, 218, 234, 256, 299, 305,
        // 333, 342, 389, 401, 444, 476, 490, 503,
        // 512, 555, 581, 612, 633, 654, 672, 680]
        for (int n = 680; n < 681; n++) {
            List<String> inputs = List.of(
                    "/mnist/PickedDatasets_int/FreeMotion/data" + n + "_0/input_seq_pc.pb",
                    "/mnist/PickedDatasets_int/FreeMotion/data" + n + "_0/input_seq_pos.pb"
            );
            List<String> outputs = List.of(
                    "/mnist/PickedDatasets_int/FreeMotion/data" + n + "_0/output_pre_trans.pb",
                    "/mnist/PickedDatasets_int/FreeMotion/data" + n + "_0/output_rot.pb",
                    "/mnist/PickedDatasets_int/FreeMotion/data" + n + "_0/output_trj.pb"
            );
            tensorPairPaths.put(inputs, outputs);
        }
        super.testModel(
                tensorPairPaths,
                "/mnist/onnx_graph/NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_FPGA.onnx",
                List.of("seq_pc", "seq_pos"),       // 假设两个输入名
                List.of("pre_trans", "rot", "trj"), // 假设三个输出名
                new String[] {
                    //"DL4J"
                    //,
                    "HWAccelerated"
                     },
                0.0001f,
                backendPaths,
                SaveMode.FINAL_ONLY,
                OutputMode.Dequantize
        );
    }

    public void testModelWithOpsetV13_hw_FM_OBJ() throws FileNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, OperationNotSupportedException, IOException {
        Map<List<String>, List<String>> tensorPairPaths = new LinkedHashMap<>();
        Map<String, String> backendPaths = new HashMap<>();
        String currentDir = System.getProperty("user.dir");
        backendPaths.put(
                "HWAccelerated",
                // 输出文件夹的位置
                currentDir + "/datasets_results/FPGA_FM_OBJ_final_layer");

        // [4, 7, 12, 19, 21, 28, 33, 34,
        // 42, 50, 51, 56, 60, 64, 71, 75,
        // 78, 82, 89, 93, 96, 101, 105, 108,
        // 114, 117, 120, 122, 127, 129, 131, 133]
        for (int n = 133; n < 134 ; n++) {
            List<String> inputs = List.of(
                    "/mnist/PickedDatasets_int/FreeMotion-OBJ/data" + n + "_0/input_seq_pc.pb",
                    "/mnist/PickedDatasets_int/FreeMotion-OBJ/data" + n + "_0/input_seq_pos.pb"
            );
            List<String> outputs = List.of(
                    "/mnist/PickedDatasets_int/FreeMotion-OBJ/data" + n + "_0/output_pre_trans.pb",
                    "/mnist/PickedDatasets_int/FreeMotion-OBJ/data" + n + "_0/output_rot.pb",
                    "/mnist/PickedDatasets_int/FreeMotion-OBJ/data" + n + "_0/output_trj.pb"
            );
            tensorPairPaths.put(inputs, outputs);
        }
        super.testModel(
                tensorPairPaths,
                "/mnist/onnx_graph/NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_FPGA.onnx",
                List.of("seq_pc", "seq_pos"),       // 假设两个输入名
                List.of("pre_trans", "rot", "trj"), // 假设三个输出名
                new String[] {
                        //"DL4J"
                        //,
                        "HWAccelerated"
                },
                0.0001f,
                backendPaths,
                SaveMode.FINAL_ONLY,
                OutputMode.Dequantize
        );
    }

    public void testModelWithOpsetV13_hw_NM() throws FileNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, OperationNotSupportedException, IOException {
        Map<List<String>, List<String>> tensorPairPaths = new LinkedHashMap<>();
        Map<String, String> backendPaths = new HashMap<>();
        String currentDir = System.getProperty("user.dir");
        backendPaths.put(
                "HWAccelerated",
                // 输出文件夹的位置
                currentDir + "/datasets_results/FPGA_NM_final_layer");

        // [14, 256, 388, 512, 604, 789, 901, 1024,
        // 1150, 1280, 1405, 1555, 1780, 1999, 2048, 2222,
        // 2500, 2750, 2999, 3141, 3333, 3500, 3780, 4004,
        // 4096, 4250, 4500, 4678, 4800, 4950, 5010, 5042]
        for (int n = 5042; n < 5043 ; n++) {
            List<String> inputs = List.of(
                    "/mnist/PickedDatasets_int/NoiseMotion/data" + n + "_0/input_seq_pc.pb",
                    "/mnist/PickedDatasets_int/NoiseMotion/data" + n + "_0/input_seq_pos.pb"
            );
            List<String> outputs = List.of(
                    "/mnist/PickedDatasets_int/NoiseMotion/data" + n + "_0/output_pre_trans.pb",
                    "/mnist/PickedDatasets_int/NoiseMotion/data" + n + "_0/output_rot.pb",
                    "/mnist/PickedDatasets_int/NoiseMotion/data" + n + "_0/output_trj.pb"
            );
            tensorPairPaths.put(inputs, outputs);
        }
        super.testModel(
                tensorPairPaths,
                "/mnist/onnx_graph/NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_FPGA.onnx",
                List.of("seq_pc", "seq_pos"),       // 假设两个输入名
                List.of("pre_trans", "rot", "trj"), // 假设三个输出名
                new String[] {
                        //"DL4J"
                        //,
                        "HWAccelerated"
                },
                0.0001f,
                backendPaths,
                SaveMode.FINAL_ONLY,
                OutputMode.Dequantize
        );
    }

    public void testModelWithOpsetV13_hw_S4D() throws FileNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, OperationNotSupportedException, IOException {
        Map<List<String>, List<String>> tensorPairPaths = new LinkedHashMap<>();
        Map<String, String> backendPaths = new HashMap<>();
        String currentDir = System.getProperty("user.dir");
        backendPaths.put(
                "HWAccelerated",
                // 输出文件夹的位置
                currentDir + "/datasets_results_all/FPGA_Sloper4D");

        // 随机取48个数
        int[] selectedIndices = {
                1, 4, 7, 8, 12, 19, 23, 25, 29, 31, 34, 38, 41, 44, 47, 50,
                53, 58, 62, 65, 69, 72, 75, 78, 81, 85, 88, 92, 95, 99, 103,
                106, 110, 114, 117, 121, 124, 127, 129, 131, 134, 136, 139,
                141, 143, 145, 146, 147
        };
        for (int n : selectedIndices) {
            List<String> inputs = List.of(
                    currentDir + "/sw/java/test/resources/mnist/PickedDatasets_int/Sloper4D/data" + n + "_0/input_seq_pc.pb",
                    currentDir + "/sw/java/test/resources/mnist/PickedDatasets_int/Sloper4D/data" + n + "_0/input_seq_pos.pb"
            );
            List<String> outputs = List.of(
                    currentDir + "/sw/java/test/resources/mnist/PickedDatasets_int/Sloper4D/data" + n + "_0/output_pre_trans.pb",
                    currentDir + "/sw/java/test/resources/mnist/PickedDatasets_int/Sloper4D/data" + n + "_0/output_rot.pb",
                    currentDir + "/sw/java/test/resources/mnist/PickedDatasets_int/Sloper4D/data" + n + "_0/output_trj.pb"
            );
            tensorPairPaths.put(inputs, outputs);
        }

        super.testModel(
                tensorPairPaths,
                currentDir + "/sw/java/test/resources/mnist/onnx_graph/NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_FPGA_Sloper4D.onnx",
                List.of("seq_pc", "seq_pos"),       // 假设两个输入名
                List.of("pre_trans", "rot", "trj"), // 假设三个输出名
                new String[] {
                        //"DL4J"
                        //,
                        "HWAccelerated"
                },
                0.0001f,
                backendPaths,
                SaveMode.FINAL_ONLY,
                OutputMode.Dequantize
        );
    }


    public void testModelWithOpsetV13_dl4j_quantized_FM() throws FileNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, OperationNotSupportedException, IOException {
        Map<List<String>, List<String>> tensorPairPaths = new LinkedHashMap<>();
        Map<String, String> backendPaths = new HashMap<>();
        String currentDir = System.getProperty("user.dir");
        backendPaths.put(
                "DL4J",
                // 输出文件夹的位置
                currentDir + "/datasets_results_all/Quantized_FM_all");
        for (int n = 0; n < 693 ; n++) {
            List<String> inputs = List.of(
                    "/home/user/Workspace/livehps_1/sw/java/test/resources/mnist/datasets/FreeMotion/data" + n + "_0/input_seq_pc.pb",
                    "/home/user/Workspace/livehps_1/sw/java/test/resources/mnist/datasets/FreeMotion/data" + n + "_0/input_seq_pos.pb"
            );
            List<String> outputs = List.of(
                    currentDir + "/sw/java/test/resources/mnist/datasets/FreeMotion/data" + n + "_0/output_pre_trans.pb",
                    currentDir + "/sw/java/test/resources/mnist/datasets/FreeMotion/data" + n + "_0/output_rot.pb",
                    currentDir + "/sw/java/test/resources/mnist/datasets/FreeMotion/data" + n + "_0/output_trj.pb"
            );
            tensorPairPaths.put(inputs, outputs);
        }

        super.testModel(
                tensorPairPaths,
                currentDir + "/sw/java/test/resources/mnist/onnx_graph/NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_OnnxRuntime_0306.onnx",
                List.of("seq_pc", "seq_pos_fp"),       // 假设两个输入名
                List.of("pre_trans_fp", "rot", "trj"), // 假设三个输出名
                new String[] {
                        "DL4J"
                        //,
                        //"HWAccelerated"
                },
                0.0001f,
                backendPaths,
                SaveMode.FINAL_ONLY,
                OutputMode.Normal
        );
    }

    public void testModelWithOpsetV13_dl4j_quantized_FM_OBJ() throws FileNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, OperationNotSupportedException, IOException {
        Map<List<String>, List<String>> tensorPairPaths = new LinkedHashMap<>();
        Map<String, String> backendPaths = new HashMap<>();
        String currentDir = System.getProperty("user.dir");
        backendPaths.put(
                "DL4J",
                // 输出文件夹的位置
                currentDir+ "/datasets_results/Quantized_FM_OBJ_final_layer");
        // [4, 7, 12, 19, 21, 28, 33, 34,
        // 42, 50, 51, 56, 60, 64, 71, 75,
        // 78, 82, 89, 93, 96, 101, 105, 108,
        // 114, 117, 120, 122, 127, 129, 131, 133]
        for (int n = 133; n < 134 ; n++) {
            List<String> inputs = List.of(
                    "/mnist/datasets/FreeMotion-OBJ/data" + n + "_0/input_seq_pc.pb",
                    "/mnist/datasets/FreeMotion-OBJ/data" + n + "_0/input_seq_pos.pb"
            );
            List<String> outputs = List.of(
                    "/mnist/datasets/FreeMotion-OBJ/data" + n + "_0/output_pre_trans.pb",
                    "/mnist/datasets/FreeMotion-OBJ/data" + n + "_0/output_rot.pb",
                    "/mnist/datasets/FreeMotion-OBJ/data" + n + "_0/output_trj.pb"
            );
            tensorPairPaths.put(inputs, outputs);
        }
        super.testModel(
                tensorPairPaths,
                "/mnist/onnx_graph/NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_OnnxRuntime_0306.onnx",
                List.of("seq_pc", "seq_pos_fp"),       // 假设两个输入名
                List.of("pre_trans_fp", "rot", "trj"), // 假设三个输出名
                new String[] {
                        "DL4J"
                        //,
                        //"HWAccelerated"
                },
                0.0001f,
                backendPaths,
                SaveMode.FINAL_ONLY,
                OutputMode.Normal
        );
    }

    public void testModelWithOpsetV13_dl4j_quantized_NM() throws FileNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, OperationNotSupportedException, IOException {
        Map<List<String>, List<String>> tensorPairPaths = new LinkedHashMap<>();
        Map<String, String> backendPaths = new HashMap<>();
        String currentDir = System.getProperty("user.dir");
        backendPaths.put(
                "DL4J",
                // 输出文件夹的位置
                currentDir+ "/datasets_results/Quantized_NM_final_layer");
        // [14, 256, 388, 512, 604, 789, 901, 1024,
        // 1150, 1280, 1405, 1555, 1780, 1999, 2048, 2222,
        // 2500, 2750, 2999, 3141, 3333, 3500, 3780, 4004,
        // 4096, 4250, 4500, 4678, 4800, 4950, 5010, 5042]
        for (int n = 5042; n < 5043 ; n++) {
            List<String> inputs = List.of(
                    "/mnist/datasets/NoiseMotion/data" + n + "_0/input_seq_pc.pb",
                    "/mnist/datasets/NoiseMotion/data" + n + "_0/input_seq_pos.pb"
            );
            List<String> outputs = List.of(
                    "/mnist/datasets/NoiseMotion/data" + n + "_0/output_pre_trans.pb",
                    "/mnist/datasets/NoiseMotion/data" + n + "_0/output_rot.pb",
                    "/mnist/datasets/NoiseMotion/data" + n + "_0/output_trj.pb"
            );
            tensorPairPaths.put(inputs, outputs);
        }
        super.testModel(
                tensorPairPaths,
                "/mnist/onnx_graph/NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_OnnxRuntime_0306.onnx",
                List.of("seq_pc", "seq_pos_fp"),       // 假设两个输入名
                List.of("pre_trans_fp", "rot", "trj"), // 假设三个输出名
                new String[] {
                        "DL4J"
                        //,
                        //"HWAccelerated"
                },
                0.0001f,
                backendPaths,
                SaveMode.FINAL_ONLY,
                OutputMode.Normal
        );
    }

    public void testModelWithOpsetV13_dl4j_quantized_S4D() throws FileNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, OperationNotSupportedException, IOException {
        Map<List<String>, List<String>> tensorPairPaths = new LinkedHashMap<>();
        Map<String, String> backendPaths = new HashMap<>();
        String currentDir = System.getProperty("user.dir");
        backendPaths.put(
                "DL4J",
                // 输出文件夹的位置
                currentDir+ "/datasets_results_all/Quantized_Sloper4D");

        int[] selectedIndices = {
                1, 4, 7, 8, 12, 19, 23, 25, 29, 31, 34, 38, 41, 44, 47, 50,
                53, 58, 62, 65, 69, 72, 75, 78, 81, 85, 88, 92, 95, 99, 103,
                106, 110, 114, 117, 121, 124, 127, 129, 131, 134, 136, 139,
                141, 143, 145, 146, 147
        };
        for (int n : selectedIndices) {
            List<String> inputs = List.of(
                    "/mnist/datasets/Sloper4D/data" + n + "_0/input_seq_pc.pb",
                    "/mnist/datasets/Sloper4D/data" + n + "_0/input_seq_pos.pb"
            );
            List<String> outputs = List.of(
                    "/mnist/datasets/Sloper4D/data" + n + "_0/output_pre_trans.pb",
                    "/mnist/datasets/Sloper4D/data" + n + "_0/output_rot.pb",
                    "/mnist/datasets/Sloper4D/data" + n + "_0/output_trj.pb"
            );
            tensorPairPaths.put(inputs, outputs);
        }
        super.testModel(
                tensorPairPaths,
                "/mnist/onnx_graph/NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_OnnxRuntime_Sloper4D.onnx",
                List.of("seq_pc", "seq_pos_fp"),       // 假设两个输入名
                List.of("pre_trans_fp", "rot", "trj"), // 假设三个输出名
                new String[] {
                        "DL4J"
                        //,
                        //"HWAccelerated"
                },
                0.0001f,
                backendPaths,
                SaveMode.FINAL_ONLY,
                OutputMode.Normal
        );
    }

//    public void testCompareIntermediateTensors() throws Exception {
//    Map<List<String>, List<String>> tensorPairPaths = new LinkedHashMap<>();
//    for (int n = 1; n < 2; n++) {
//        List<String> inputs = List.of(
//                "/mnist/Quantized/data" + n + "/input_seq_pc.pb",
//                "/mnist/Quantized/data" + n + "/input_seq_pos.pb"
//        );
//        List<String> outputs = List.of(
//                "/mnist/Quantized/data" + n + "/output_pre_tran.pb",
//                "/mnist/Quantized/data" + n + "/output_rot.pb",
//                "/mnist/Quantized/data" + n + "/output_trj.pb"
//        );
//        tensorPairPaths.put(inputs, outputs);
//    }
//
//    String modelPath = "/mnist/onnx_graph/NEW_deploy_module2_Stacked_MinGRU_DB_conv_FPGA_INT24_FPGA.onnx";
//    List<String> inputNames = List.of("seq_pc", "seq_pos");
//    List<String> outputNames = List.of("pre_trans", "rot", "trj");
//    String[] backendNames = {"DL4J", "HWAccelerated"};
//    float tolerance = 0.0001f;
//
//    compareIntermediateTensors(tensorPairPaths, modelPath, inputNames, outputNames, backendNames, tolerance);
//}

    public static void main(String[] args) {
        try {
            System.out.println("========== Start ==========");
            ModelTest runner = new ModelTest("ManualRunner");

            System.out.println(">> HW Sloper4D ");
            runner.testModelWithOpsetV13_hw_S4D();
            // 需要跑哪个直接加上就行

//            System.out.println(">> DL4J Sloper4D ");
//            runner.testModelWithOpsetV13_dl4j_quantized_S4D();
            System.out.println("========== Finished! ==========");

        } catch (Exception e) {
            System.err.println("运行过程中发生错误：");
            e.printStackTrace();
        }
    }
}

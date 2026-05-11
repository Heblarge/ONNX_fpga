/**
 * HybridExecutor 使用示例
 * 
 * 演示如何使用 HWAccelerated + DL4J 混合执行器处理不支持的算子
 */

package org.forwarder.example;

import org.forwarder.Model;
import org.forwarder.Session;
import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedBackend;
import org.forwarder.backend.impls.dl4j.DL4JBackend;
import org.forwarder.executor.impls.HybridSequentialExecutor;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.onnx4j.opsets.OperatorSets;
import org.onnx4j.opsets.OperatorSetRegistry;

public class HybridExecutorExample {

    /**
     * 运行 ONNX 模型，支持 HWAccelerated + DL4J 混合执行
     */
    public static void runModelWithHybridExecutor(String modelPath, String inputName, INDArray inputData) {
        try {
            // 1. 加载模型
            Model model = Model.load(modelPath);
            
            // 2. 创建后端
            HWAcceleratedBackend hwAccelBackend = new HWAcceleratedBackend(model);
            DL4JBackend dl4jBackend = new DL4JBackend(model);
            
            // 3. 获取两个后端的 OperatorSets
            OperatorSets hwAccelOpsets = OperatorSetRegistry.getInstance().getOpsets(hwAccelBackend.getName());
            OperatorSets dl4jOpsets = OperatorSetRegistry.getInstance().getOpsets(dl4jBackend.getName());
            
            // 4. 创建混合执行器（注入 DL4J OperatorSets 作为回退）
            HybridSequentialExecutor<INDArray> executor = new HybridSequentialExecutor<>(model, dl4jOpsets);
            
            // 5. 创建会话
            Session<INDArray> session = hwAccelBackend.newSession();
            
            // 6. 设置输入
            session.feed(inputName, inputData);
            
            // 7. 执行（混合执行，HWAccelerated 优先，失败时回退到 DL4J）
            executor.execute(session, hwAccelOpsets);
            
            // 8. 获取输出
            INDArray result = session.fetch("output");
            System.out.println("计算完成，结果形状: " + result.shapeInfoToString());
            
            // 9. 查看回退统计
            System.out.println("\n回退统计:");
            if (executor.getUnsupportedOps().isEmpty()) {
                System.out.println("所有算子都由 HWAccelerated 处理");
            } else {
                System.out.println("以下算子被回退到 DL4J:");
                for (String op : executor.getUnsupportedOps()) {
                    System.out.println("  - " + op);
                }
            }
            
            // 10. 清理资源
            session.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

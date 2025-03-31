package org.forwarder.backend.impls.dl4j.opsets;

import java.util.Random;

import org.bytedeco.javacpp.Pointer;
import org.junit.Test;
import org.nd4j.linalg.api.memory.conf.WorkspaceConfiguration;
import org.nd4j.linalg.api.memory.enums.AllocationPolicy;
import org.nd4j.linalg.api.memory.enums.LearningPolicy;
import org.nd4j.linalg.api.memory.enums.MemoryKind;
import org.nd4j.linalg.api.memory.enums.MirroringPolicy;
import org.nd4j.linalg.api.memory.enums.SpillPolicy;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.memory.abstracts.Nd4jWorkspace;

public class MemoryTest {

	@Test
	public void testMemory() {
		System.out.println("totalPhysicalBytes: " + Pointer.maxBytes() / 1024 / 1024 / 1024 + "G");

		WorkspaceConfiguration wsCfg = WorkspaceConfiguration.builder().initialSize(10 * 1024 * 1024)
				.maxSize(10 * 1024 * 1024).overallocationLimit(0.1).policyAllocation(AllocationPolicy.STRICT)
				.policyLearning(LearningPolicy.FIRST_LOOP).policyMirroring(MirroringPolicy.FULL)
				.policySpill(SpillPolicy.EXTERNAL).build();

		int len = 4;
		int iter =1;

		for (int n = 0; n < iter; n++) {
			// try (Nd4jWorkspace ws = (Nd4jWorkspace)
			// Nd4j.getWorkspaceManager().getAndActivateWorkspace(wsCfg,
			// "testws")) {
			// ws.enableDebug(true);
			//System.out.println("MMUL" + Nd4j.linspace(1, len, len).reshape(1, len)
			//		.mmul(Nd4j.linspace(1, len, len).reshape(1, len).transpose()));
			// }
			/*Nd4j.linspace(1, len, len).reshape(1, len)
			.mmul(Nd4j.linspace(1, len, len).reshape(1, len).transpose());
			System.gc();*/
			//测试一些INDArray的常规计算操作
			INDArray a = Nd4j.linspace(1, len, len);//生成线性递增的数组，范围从1到len，长度为len
			System.out.println("a:"+a);
			INDArray b = a.reshape(1, len);//reshape为1行len列的数组
			System.out.println("b:"+b);
			INDArray c = Nd4j.linspace(1, len, len);//生成线性递增的数组，范围从1到len，长度为len
			System.out.println(c);
			INDArray d = a.reshape(1, len);//reshape为1行len列的数组
			System.out.println(d);
			INDArray e = d.transpose();//转置数组

			System.out.println(e);
			INDArray f = b.mmul(e);//矩阵乘法
			System.out.println(f);//输出结果
			System.out.println("iter:"+n);

			//f.close();
			//e.close();
			//d.close();
			c.close();
			//b.close();
			a.close();
		}
	}

	@Test
	public void testLinearRegression() {
		int len = 1000; // 定义数组长度为1000
		int iters = 10000; // 定义迭代次数为1000
		int exampleCount = 1000; // 定义样本数量为1000
		double learningRate = 0.01; // 定义学习率为0.01
		Random random = new Random(); // 初始化随机数生成器
		double[] data = new double[exampleCount * 3]; // 初始化数据数组，长度为样本数量的3倍
		double[] param = new double[exampleCount * 3]; // 初始化参数数组，长度为样本数量的3倍

		//随机生成data
		for (int i = 0; i < exampleCount * 3; i++) {
			data[i] = random.nextDouble();
		}
		//生成param，有一个固定的patten
		for (int i = 0; i < exampleCount * 3; i++) {
			param[i] = 3;
			param[++i] = 4;
			param[++i] = 5;
		}
		
		INDArray features = Nd4j.create(data, new int[] { exampleCount, 3 });//从data创建features矩阵[exampleCount,3]
		INDArray params = Nd4j.create(param, new int[] { exampleCount, 3 });//从param创建params矩阵[exampleCount,3]


		// 计算标签值
		INDArray label = features.mul(params).sum(1).add(10);//首先执行features和params的矩阵按元素相乘，然后对结果按行求和，最后加上10得到标签值//相当于是一个偏置为10 的线性层

		// 初始化参数向量
		double[] parameter = new double[] { 1.0, 1.0, 1.0, 1.0 };

		// 记录开始时间
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < iters; i++) {
			BGD(features, label, learningRate, parameter);
			System.out.println("MMUL" + Nd4j.linspace(1, len, len).reshape(1, len)
					.mmul(Nd4j.linspace(1, len, len).reshape(1, len).transpose()));
			System.gc();
		}
		System.out.println("testLinearRegression take time: " + (System.currentTimeMillis() - startTime)+"ms");
	}

	private static void BGD(INDArray features, INDArray label, double learningRate, double[] parameter) {
		// 计算误差向量temp
		INDArray temp = features.getColumn(0).mul(parameter[0]).add(features.getColumn(1).mul(parameter[1]))
				.add(features.getColumn(2).mul(parameter[2])).add(parameter[3]).sub(label);
				

		// 更新参数
		parameter[0] = parameter[0] - 2 * learningRate * temp.mul(features.getColumn(0)).sum(0).getDouble(0) / features.size(0);
		parameter[1] = parameter[1] - 2 * learningRate * temp.mul(features.getColumn(1)).sum(0).getDouble(0) / features.size(0);
		parameter[2] = parameter[2] - 2 * learningRate * temp.mul(features.getColumn(2)).sum(0).getDouble(0) / features.size(0);
		parameter[3] = parameter[3] - 2 * learningRate * temp.sum(0).getDouble(0) / features.size(0);

		// 计算函数结果和总损失
		INDArray functionResult = features.getColumn(0).mul(parameter[0]).add(features.getColumn(1).mul(parameter[1]))
				.add(features.getColumn(2).mul(parameter[2])).add(parameter[3]).sub(label);
		double totalLoss = functionResult.mul(functionResult).sum(0).getDouble(0);
		System.out.println("totalLoss:" + totalLoss);
		System.out.println(parameter[0] + " " + parameter[1] + " " + parameter[2] + " " + parameter[3]);
	}

}

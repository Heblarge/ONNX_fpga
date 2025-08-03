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
package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;

import com.google.common.collect.Lists;
import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v5.ops.DL4JReshapeV5;
import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.ReshapeV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DL4JReshapeV13 extends DL4JAiOnnxOperator  implements ReshapeV13 {

	@Override
	public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
		ReshapeInputsV13<INDArray> castedOperatorInputs = new ReshapeInputsV13<INDArray>(node, inputs);
		INDArray data = castedOperatorInputs.getData();
		INDArray shape = castedOperatorInputs.getShapeTensor();
		return new ReshapeOutputV13<INDArray>(this.reshape(data, shape));
	}

	protected INDArray reshape(INDArray data, INDArray shapeTensor) {
		// 1. 从 shape 张量中获取目标形状
		long[] newShape = shapeTensor.toLongVector();
		long[] originalShape = data.shape();

		// 2. 处理 ONNX 规范中 shape dimension 为 0 的特殊情况
		for (int i = 0; i < newShape.length; i++) {
			if (newShape[i] == 0) {
				if (i < originalShape.length) {
					newShape[i] = originalShape[i];
				} else {
					throw new IllegalArgumentException("Invalid shape provided for reshape: Dimension " + i + " is 0 but input has only " + originalShape.length + " dimensions.");
				}
			}
		}

		// 3. 直接调用 ND4J 自己的 reshape 方法，它会自动处理 -1 的情况
		return data.reshape(newShape);

	}

}
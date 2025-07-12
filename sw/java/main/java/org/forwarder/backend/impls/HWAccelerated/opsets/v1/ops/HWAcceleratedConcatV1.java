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
package org.forwarder.backend.impls.HWAccelerated.opsets.v1.ops;

import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v1.ops.ConcatV1;
import org.onnx4j.opsets.operator.OperatorOutputs;

import java.util.List;

public class HWAcceleratedConcatV1 extends HWAcceleratedOperator implements ConcatV1 {

	@Override
	public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
		ConcatInputsV1<INDArray> castedOperatorInputs = new ConcatInputsV1<INDArray>(node, inputs);
		List<INDArray> inputList = castedOperatorInputs.getInputs();
		Long axis = castedOperatorInputs.getAxis();
		return new ConcatOutputV1<INDArray>(this.concat(inputList, axis));
	}

	protected INDArray concat(List<INDArray> inputs, Long axis) {
		return Nd4j.concat(axis.intValue(), inputs.toArray(new INDArray[inputs.size()]));
	}
}
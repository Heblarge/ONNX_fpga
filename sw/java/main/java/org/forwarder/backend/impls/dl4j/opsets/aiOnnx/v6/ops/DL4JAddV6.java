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
package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v6.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v1.ops.DL4JAddV1;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v6.ops.AddV6;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class DL4JAddV6 extends DL4JAddV1 implements AddV6 {

	@Override
	public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
		AddInputsV6<INDArray> castedOperatorInputs = new AddInputsV6<INDArray>(node, inputs);
		INDArray a = castedOperatorInputs.getA();
		INDArray b = castedOperatorInputs.getB();
		Long axis = castedOperatorInputs.getAxis();
		Long broadcast = castedOperatorInputs.getBroadcast();
    if(broadcast==1){
      return new AddOutputV6<INDArray>(this.add(a, b, axis, 1L));
    }else{
      return new AddOutputV6<INDArray>(this.add(a, b, axis, 0L));
    }
		
	}

	protected INDArray add(INDArray a, INDArray b, Long axis, Long broadcast) {
		return super.add(a, b, axis, broadcast, null);
	}

}
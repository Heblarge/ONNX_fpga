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
package org.forwarder.backend.impls.HWAccelerated.opsets.v7;

import org.forwarder.backend.impls.HWAccelerated.opsets.v7.ops.*;
import org.forwarder.backend.impls.HWAccelerated.opsets.v6.HWAcceleratedOperatorSetV6;
import org.onnx4j.opsets.domain.aiOnnx.v7.AiOnnxOperatorSetInitializerV7;
import org.onnx4j.opsets.domain.aiOnnx.v7.ops.*;

public class HWAcceleratedOperatorSetV7 extends HWAcceleratedOperatorSetV6 implements AiOnnxOperatorSetInitializerV7 {

	//@Override
	//public AcosV7 getAcosV7() { return new DL4JAcosV7(); }

	@Override
	public BatchNormalizationV7 getBatchNormalizationV7() { return new HWAcceleratedBatchNormalizationV7(); }

	@Override
	public DropoutV7 getDropoutV7() { return new HWAcceleratedDropoutV7(); }

	@Override
	public AveragePoolV7 getAveragePoolV7() { return new HWAcceleratedAveragePoolV7(); }

	@Override
	public SubV7 getSubV7() { return new HWAcceleratedSubV7(); }

	@Override
	public AddV7 getAddV7() { return new HWAcceleratedAddV7(); }

	public HWAcceleratedOperatorSetV7() {
		super(1, "", "", 7L, "ONNX OPSET-V7 USING DL4J BACKEND");
	}

	public HWAcceleratedOperatorSetV7(int irVersion, String irVersionPrerelease, String irBuildMetadata,
			long opsetVersion, String docString) {
		super(irVersion, irVersionPrerelease, irBuildMetadata, opsetVersion, docString);
	}

}
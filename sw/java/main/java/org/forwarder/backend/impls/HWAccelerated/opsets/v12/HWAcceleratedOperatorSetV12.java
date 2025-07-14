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
package org.forwarder.backend.impls.HWAccelerated.opsets.v12;

import org.forwarder.backend.impls.HWAccelerated.opsets.v12.ops.HWAcceleratedReduceMaxV12;
import org.forwarder.backend.impls.HWAccelerated.opsets.v11.HWAcceleratedOperatorSetV11;
import org.onnx4j.opsets.domain.aiOnnx.v12.AiOnnxOpsetInitializerV12;
import org.onnx4j.opsets.domain.aiOnnx.v12.ops.ReduceMaxV12;

public class HWAcceleratedOperatorSetV12 extends HWAcceleratedOperatorSetV11 implements AiOnnxOpsetInitializerV12 {

	@Override
	public ReduceMaxV12 getReduceMaxV12() { return new HWAcceleratedReduceMaxV12(); }

	public HWAcceleratedOperatorSetV12() {
		super(1, "", "", 12L, "ONNX OPSET-V11 USING HWAccelerated BACKEND");
	}

	public HWAcceleratedOperatorSetV12(int irVersion, String irVersionPrerelease, String irBuildMetadata,
			long opsetVersion, String docString) {
		super(irVersion, irVersionPrerelease, irBuildMetadata, opsetVersion, docString);
	}

}
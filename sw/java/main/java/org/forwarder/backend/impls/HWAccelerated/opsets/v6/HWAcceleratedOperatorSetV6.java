package org.forwarder.backend.impls.HWAccelerated.opsets.v6;

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

import org.forwarder.backend.impls.HWAccelerated.opsets.v5.HWAcceleratedOperatorSetV5;
import org.forwarder.backend.impls.HWAccelerated.opsets.v6.ops.* ;

import org.onnx4j.opsets.domain.aiOnnx.v6.AiOnnxOpsetInitializerV6;
import org.onnx4j.opsets.domain.aiOnnx.v6.ops.BatchNormalizationV6;
import org.onnx4j.opsets.domain.aiOnnx.v6.ops.CastV6;
import org.onnx4j.opsets.domain.aiOnnx.v6.ops.DropoutV6;
import org.onnx4j.opsets.domain.aiOnnx.v6.ops.MulV6;
import org.onnx4j.opsets.domain.aiOnnx.v6.ops.SigmoidV6;
import org.onnx4j.opsets.domain.aiOnnx.v6.ops.SubV6;
import org.onnx4j.opsets.domain.aiOnnx.v6.ops.AddV6;
import org.onnx4j.opsets.domain.aiOnnx.v6.ops.SumV6;

public class HWAcceleratedOperatorSetV6 extends HWAcceleratedOperatorSetV5 implements AiOnnxOpsetInitializerV6 {

    @Override
    public AddV6 getAddV6() { return new HWAcceleratedAddV6(); }

    @Override
    public MulV6 getMulV6() { return new HWAcceleratedMulV6(); }

    @Override
    public DropoutV6 getDropoutV6() { return new HWAcceleratedDropoutV6(); }

    @Override
    public CastV6 getCastV6() { return new HWAcceleratedCastV6(); }

    @Override
    public SubV6 getSubV6() { return new HWAcceleratedSubV6(); }

    @Override
    public SumV6 getSumV6() { return new HWAcceleratedSumV6(); }

    @Override
    public SigmoidV6 getSigmoidV6() { return new HWAcceleratedSigmoidV6(); }

    @Override
    public BatchNormalizationV6 getBatchNormalizationV6() { return new HWAcceleratedBatchNormalizationV6(); }

    public HWAcceleratedOperatorSetV6() {
        this(1, "", "", 6L, "ONNX OPSET-V6 USING DL4J BACKEND");
    }

    public HWAcceleratedOperatorSetV6(int irVersion, String irVersionPrerelease, String irBuildMetadata,
                                   long opsetVersion, String docString) {
        super(irVersion, irVersionPrerelease, irBuildMetadata, opsetVersion, docString);
    }

}
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
package org.onnx4j.opsets.domain.aiOnnx.v13.ops;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v7.ops.SubV7;
import org.onnx4j.opsets.domain.aiOnnx.v13.AiOnnxOperatorV13;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import org.onnx4j.tensor.DataType;


public interface SubV13 extends SubV7, AiOnnxOperatorV13 {

    public static final TypeConstraint TYPE_CONSTRAINT_T = new TypeConstraint(DataType.highPrecisionNumeric());

    class SubInputsV13<T_TENSOR> extends SubInputsV7<T_TENSOR> {

        public SubInputsV13(Node node, Inputs inputs) {
            super(node, inputs);
        }

    }

    class SubOutputV13<T_TENSOR> extends SubV7.SubOutputV7<T_TENSOR> {

        public SubOutputV13(T_TENSOR output) {
            super(output);
        }

    }

}
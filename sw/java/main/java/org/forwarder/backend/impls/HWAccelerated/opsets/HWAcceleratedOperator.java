package org.forwarder.backend.impls.HWAccelerated.opsets;

import org.forwarder.opset.operator.Executable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.onnx4j.opsets.operator.Field.TypeConstraint;
import org.onnx4j.opsets.operator.OperatorInputs;
import org.onnx4j.opsets.operator.fields.InputField;
import org.onnx4j.tensor.DataType;

public abstract class HWAcceleratedOperator implements Executable<INDArray> {

    public void preconditions(OperatorInputs<INDArray> operatorInputs) {
        for (InputField<INDArray> inputField : operatorInputs.getInputFields()) {
            TypeConstraint constraints = inputField.getConstraints();
			/*Arrays.asList(constraints.getDataTypes()).contains(arg0)
			for (DataType availableType : constraints.getDataTypes()) {
				Arrays.
			}*/
        }
    }

}